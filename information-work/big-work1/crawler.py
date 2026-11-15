"""A small, real web crawler with HTML extraction and crawl metrics.

The implementation intentionally uses Python's standard library so it can be
run on a clean lab machine.  It follows robots.txt by default and limits the
crawl to the seed host unless --allow-external is supplied.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import logging
import os
import re
import sys
import ssl
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
import urllib.robotparser
from collections import deque
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import asdict, dataclass, field
from datetime import datetime, timezone
from html.parser import HTMLParser
from pathlib import Path
from typing import Iterable


LOG = logging.getLogger("crawler")
TRACKING_QUERY_KEYS = {"fbclid", "gclid", "mc_cid", "mc_eid"}

try:
    import certifi
except ImportError:  # pragma: no cover - optional dependency in minimal Python installs
    certifi = None


def normalize_url(raw_url: str, base_url: str | None = None) -> str | None:
    """Resolve and normalize a URL, returning None for unsupported schemes."""
    try:
        url = urllib.parse.urljoin(base_url or "", raw_url.strip())
        parts = urllib.parse.urlsplit(url)
    except ValueError:
        return None
    if parts.scheme.lower() not in {"http", "https"} or not parts.netloc:
        return None
    host = parts.hostname.lower() if parts.hostname else ""
    if not host:
        return None
    try:
        port = parts.port
    except ValueError:
        return None
    if (parts.scheme.lower(), port) in {("http", 80), ("https", 443)}:
        netloc = host
    else:
        netloc = host if port is None else f"{host}:{port}"
    path = parts.path or "/"
    if not path.startswith("/"):
        path = "/" + path
    query = urllib.parse.parse_qsl(parts.query, keep_blank_values=True)
    query = [(key, value) for key, value in query if key.lower() not in TRACKING_QUERY_KEYS]
    query_text = urllib.parse.urlencode(query, doseq=True)
    return urllib.parse.urlunsplit((parts.scheme.lower(), netloc, path, query_text, ""))


class HTMLDocumentParser(HTMLParser):
    """Extract title, visible text, description, and links from HTML."""

    SKIP_TAGS = {"script", "style", "noscript", "template", "svg"}

    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.title_parts: list[str] = []
        self.text_parts: list[str] = []
        self.links: list[str] = []
        self.description = ""
        self._skip_depth = 0
        self._in_title = False

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        tag = tag.lower()
        if tag in self.SKIP_TAGS:
            self._skip_depth += 1
        if tag == "title":
            self._in_title = True
        attrs_dict = {key.lower(): value for key, value in attrs}
        if tag == "meta" and attrs_dict.get("name", "").lower() == "description":
            self.description = (attrs_dict.get("content") or "").strip()
        if tag == "a" and attrs_dict.get("href"):
            self.links.append(attrs_dict["href"])

    def handle_startendtag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        self.handle_starttag(tag, attrs)
        self.handle_endtag(tag)

    def handle_endtag(self, tag: str) -> None:
        tag = tag.lower()
        if tag == "title":
            self._in_title = False
        if tag in self.SKIP_TAGS and self._skip_depth:
            self._skip_depth -= 1

    def handle_data(self, data: str) -> None:
        if self._in_title:
            self.title_parts.append(data)
            return
        if not self._skip_depth:
            text = re.sub(r"\s+", " ", data).strip()
            if text:
                self.text_parts.append(text)

    @property
    def title(self) -> str:
        return re.sub(r"\s+", " ", " ".join(self.title_parts)).strip()

    @property
    def text(self) -> str:
        return re.sub(r"\s+", " ", " ".join(self.text_parts)).strip()


@dataclass(slots=True)
class CrawlConfig:
    seeds: list[str]
    max_pages: int = 20
    max_depth: int = 1
    workers: int = 4
    timeout: float = 15.0
    delay: float = 0.0
    max_bytes: int = 5 * 1024 * 1024
    same_domain: bool = True
    respect_robots: bool = True
    user_agent: str = "LabCrawler/1.0 (+https://example.com/bot-info)"
    raw_dir: Path | None = None


@dataclass(slots=True)
class CrawlRecord:
    url: str
    final_url: str = ""
    depth: int = 0
    status: int | None = None
    content_type: str = ""
    title: str = ""
    description: str = ""
    text: str = ""
    links: list[str] = field(default_factory=list)
    bytes: int = 0
    elapsed_ms: float = 0.0
    fetched_at: str = ""
    error: str = ""
    raw_file: str = ""


@dataclass(slots=True)
class _FetchResult:
    record: CrawlRecord
    discovered: list[str]


class WebCrawler:
    def __init__(self, config: CrawlConfig) -> None:
        if not config.seeds:
            raise ValueError("at least one seed URL is required")
        if config.max_pages < 1 or config.max_depth < 0 or config.workers < 1:
            raise ValueError("max-pages, max-depth, and workers must be positive")
        self.config = config
        self.seeds = [self._require_url(seed) for seed in config.seeds]
        self.allowed_hosts = {urllib.parse.urlsplit(seed).hostname for seed in self.seeds}
        self._robots: dict[str, urllib.robotparser.RobotFileParser | None] = {}
        self._robots_lock = threading.Lock()
        self._host_last_request: dict[str, float] = {}
        self._delay_lock = threading.Lock()
        self._ssl_context = self._make_ssl_context()

    @staticmethod
    def _make_ssl_context() -> ssl.SSLContext:
        if certifi is not None:
            try:
                return ssl.create_default_context(cafile=certifi.where())
            except (OSError, ssl.SSLError):
                pass
        return ssl.create_default_context()

    @staticmethod
    def _require_url(url: str) -> str:
        normalized = normalize_url(url)
        if normalized is None:
            raise ValueError(f"invalid seed URL: {url}")
        return normalized

    def _is_allowed(self, url: str) -> bool:
        if not self.config.same_domain:
            return True
        hostname = urllib.parse.urlsplit(url).hostname
        return hostname in self.allowed_hosts

    def _robots_for(self, url: str) -> urllib.robotparser.RobotFileParser | None:
        parts = urllib.parse.urlsplit(url)
        origin = f"{parts.scheme}://{parts.netloc}"
        with self._robots_lock:
            if origin in self._robots:
                return self._robots[origin]
        parser: urllib.robotparser.RobotFileParser | None = urllib.robotparser.RobotFileParser()
        robots_url = origin + "/robots.txt"
        parser.set_url(robots_url)
        try:
            request = urllib.request.Request(
                robots_url,
                headers={"User-Agent": self.config.user_agent},
            )
            with urllib.request.urlopen(request, timeout=self.config.timeout, context=self._ssl_context) as response:
                parser.parse(response.read(1024 * 1024).decode("utf-8", "replace").splitlines())
        except (OSError, urllib.error.URLError, TimeoutError) as exc:
            LOG.debug("Could not read robots.txt from %s: %s", origin, exc)
            parser = None
        with self._robots_lock:
            self._robots[origin] = parser
        return parser

    def _can_fetch(self, url: str) -> bool:
        if not self.config.respect_robots:
            return True
        parser = self._robots_for(url)
        return parser is None or parser.can_fetch(self.config.user_agent, url)

    def _wait_for_host(self, url: str) -> None:
        if self.config.delay <= 0:
            return
        host = urllib.parse.urlsplit(url).netloc
        with self._delay_lock:
            now = time.monotonic()
            wait = self.config.delay - (now - self._host_last_request.get(host, 0.0))
            if wait > 0:
                time.sleep(wait)
            self._host_last_request[host] = time.monotonic()

    def _fetch(self, url: str, depth: int) -> _FetchResult:
        started = time.perf_counter()
        record = CrawlRecord(
            url=url,
            depth=depth,
            fetched_at=datetime.now(timezone.utc).isoformat(),
        )
        if not self._is_allowed(url):
            record.error = "outside allowed seed domain"
            record.elapsed_ms = round((time.perf_counter() - started) * 1000, 2)
            return _FetchResult(record, [])
        if not self._can_fetch(url):
            record.error = "blocked by robots.txt"
            record.elapsed_ms = round((time.perf_counter() - started) * 1000, 2)
            return _FetchResult(record, [])
        self._wait_for_host(url)
        request = urllib.request.Request(
            url,
            headers={
                "User-Agent": self.config.user_agent,
                "Accept": "text/html,application/xhtml+xml;q=0.9,*/*;q=0.1",
                "Accept-Encoding": "identity",
            },
        )
        try:
            with urllib.request.urlopen(request, timeout=self.config.timeout, context=self._ssl_context) as response:
                record.status = getattr(response, "status", response.getcode())
                record.final_url = normalize_url(response.geturl()) or response.geturl()
                record.content_type = response.headers.get_content_type().lower()
                payload = response.read(self.config.max_bytes + 1)
                if len(payload) > self.config.max_bytes:
                    raise ValueError(f"response exceeds {self.config.max_bytes} byte limit")
                record.bytes = len(payload)
                if record.content_type not in {"text/html", "application/xhtml+xml"}:
                    record.error = "unsupported content type"
                else:
                    html = self._decode(payload, response.headers.get_content_charset())
                    parser = HTMLDocumentParser()
                    parser.feed(html)
                    parser.close()
                    record.title = parser.title
                    record.description = parser.description
                    record.text = parser.text
                    discovered: list[str] = []
                    for href in parser.links:
                        link = normalize_url(href, record.final_url or url)
                        if link and self._is_allowed(link):
                            discovered.append(link)
                    record.links = sorted(set(discovered))
                    self._save_raw(url, payload, record)
                    record.elapsed_ms = round((time.perf_counter() - started) * 1000, 2)
                    return _FetchResult(record, record.links)
        except urllib.error.HTTPError as exc:
            record.status = exc.code
            record.final_url = url
            record.error = f"HTTP {exc.code}: {exc.reason}"
        except (urllib.error.URLError, TimeoutError, ValueError, OSError) as exc:
            record.error = str(exc)
        record.elapsed_ms = round((time.perf_counter() - started) * 1000, 2)
        return _FetchResult(record, [])

    @staticmethod
    def _decode(payload: bytes, charset: str | None) -> str:
        candidates = [charset] if charset else []
        head = payload[:4096].decode("ascii", "ignore")
        match = re.search(r"charset\s*=\s*[\"']?([\w.-]+)", head, flags=re.I)
        if match:
            candidates.append(match.group(1))
        candidates.extend(["utf-8", "gb18030", "latin-1"])
        for encoding in candidates:
            try:
                return payload.decode(encoding or "utf-8")
            except (LookupError, UnicodeDecodeError):
                continue
        return payload.decode("utf-8", "replace")

    def _save_raw(self, url: str, payload: bytes, record: CrawlRecord) -> None:
        if self.config.raw_dir is None:
            return
        self.config.raw_dir.mkdir(parents=True, exist_ok=True)
        digest = hashlib.sha256(url.encode("utf-8")).hexdigest()[:16]
        path = self.config.raw_dir / f"{digest}.html"
        path.write_bytes(payload)
        record.raw_file = str(path)

    def crawl(self) -> list[CrawlRecord]:
        queue: deque[tuple[str, int]] = deque((seed, 0) for seed in self.seeds)
        scheduled: set[str] = set(self.seeds)
        seen: set[str] = set()
        records: list[CrawlRecord] = []
        with ThreadPoolExecutor(max_workers=self.config.workers) as executor:
            while queue and len(records) < self.config.max_pages:
                batch: list[tuple[str, int]] = []
                while queue and len(batch) < self.config.workers and len(records) + len(batch) < self.config.max_pages:
                    url, depth = queue.popleft()
                    if url in seen:
                        continue
                    seen.add(url)
                    batch.append((url, depth))
                futures = {executor.submit(self._fetch, url, depth): (url, depth) for url, depth in batch}
                for future in as_completed(futures):
                    result = future.result()
                    records.append(result.record)
                    if result.record.error or result.record.depth >= self.config.max_depth:
                        continue
                    for link in result.discovered:
                        if link not in seen and link not in scheduled:
                            scheduled.add(link)
                            queue.append((link, result.record.depth + 1))
        records.sort(key=lambda item: (item.depth, item.url))
        return records[: self.config.max_pages]


CSV_FIELDS = [field.name for field in CrawlRecord.__dataclass_fields__.values()]


def write_records(records: Iterable[CrawlRecord], path: Path, output_format: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    rows = [asdict(record) for record in records]
    if output_format == "csv":
        with path.open("w", encoding="utf-8-sig", newline="") as handle:
            writer = csv.DictWriter(handle, fieldnames=CSV_FIELDS)
            writer.writeheader()
            for row in rows:
                row["links"] = " ".join(row["links"])
                writer.writerow(row)
    else:
        with path.open("w", encoding="utf-8") as handle:
            for row in rows:
                handle.write(json.dumps(row, ensure_ascii=False) + "\n")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Crawl real web pages and extract HTML content.")
    parser.add_argument("urls", nargs="*", help="seed URLs")
    parser.add_argument("--seed-file", type=Path, help="UTF-8 file containing one seed URL per line")
    parser.add_argument("--max-pages", type=int, default=20, help="maximum number of URLs to fetch (default: 20)")
    parser.add_argument("--max-depth", type=int, default=1, help="maximum link depth from seeds (default: 1)")
    parser.add_argument("--workers", type=int, default=4, help="parallel fetch workers (default: 4)")
    parser.add_argument("--timeout", type=float, default=15.0, help="request timeout in seconds")
    parser.add_argument("--delay", type=float, default=0.0, help="minimum delay per host in seconds")
    parser.add_argument("--max-bytes", type=int, default=5 * 1024 * 1024, help="maximum response size")
    parser.add_argument("--output", type=Path, default=Path("data/crawl/results.jsonl"), help="result file")
    parser.add_argument("--format", choices=("jsonl", "csv"), help="output format; inferred from extension when omitted")
    parser.add_argument("--raw-dir", type=Path, default=Path("data/crawl/raw"), help="directory for fetched HTML")
    parser.add_argument("--user-agent", default="LabCrawler/1.0 (+https://example.com/bot-info)")
    parser.add_argument("--allow-external", action="store_true", help="follow links outside the seed hosts")
    parser.add_argument("--no-robots", action="store_true", help="do not check robots.txt")
    parser.add_argument("--verbose", action="store_true")
    return parser


def load_seeds(args: argparse.Namespace) -> list[str]:
    seeds = list(args.urls)
    if args.seed_file:
        seeds.extend(
            line.strip()
            for line in args.seed_file.read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.lstrip().startswith("#")
        )
    if not seeds:
        raise ValueError("provide at least one URL or --seed-file")
    return seeds


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    logging.basicConfig(level=logging.DEBUG if args.verbose else logging.WARNING, format="%(levelname)s %(message)s")
    try:
        seeds = load_seeds(args)
        output_format = args.format or ("csv" if args.output.suffix.lower() == ".csv" else "jsonl")
        config = CrawlConfig(
            seeds=seeds,
            max_pages=args.max_pages,
            max_depth=args.max_depth,
            workers=args.workers,
            timeout=args.timeout,
            delay=args.delay,
            max_bytes=args.max_bytes,
            same_domain=not args.allow_external,
            respect_robots=not args.no_robots,
            user_agent=args.user_agent,
            raw_dir=args.raw_dir,
        )
        started = time.perf_counter()
        records = WebCrawler(config).crawl()
        write_records(records, args.output, output_format)
    except (OSError, ValueError) as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 2
    success = sum(1 for record in records if record.status and not record.error and record.text)
    failed = len(records) - success
    total_bytes = sum(record.bytes for record in records)
    elapsed = time.perf_counter() - started
    print(json.dumps({
        "fetched": len(records),
        "successful_html": success,
        "failed_or_skipped": failed,
        "bytes": total_bytes,
        "elapsed_seconds": round(elapsed, 3),
        "pages_per_second": round(len(records) / elapsed, 2) if elapsed else 0,
        "output": str(args.output),
    }, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
