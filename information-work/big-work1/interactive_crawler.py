"""只输入一个网址即可启动的中文网页爬虫入口。"""

from __future__ import annotations

import json
import logging
import sys
import time
from pathlib import Path

from crawler import CrawlConfig, WebCrawler, normalize_url, write_records


# 交互模式使用稳定的实验默认值，用户不需要重复填写参数。
DEFAULT_MAX_PAGES = 20
DEFAULT_MAX_DEPTH = 1
DEFAULT_WORKERS = 4
DEFAULT_TIMEOUT = 20.0
DEFAULT_DELAY = 0.2
DEFAULT_MAX_BYTES = 5 * 1024 * 1024
DEFAULT_USER_AGENT = "LabCrawler/1.0 (+https://example.com/bot-info)"
DEFAULT_OUTPUT = Path("data/crawl/results.jsonl")
DEFAULT_RAW_DIR = Path("data/crawl/raw")


def configure_console_encoding() -> None:
    """让 Windows 终端尽量使用 UTF-8 显示中文提示。"""
    for stream in (sys.stdout, sys.stderr):
        reconfigure = getattr(stream, "reconfigure", None)
        if reconfigure is not None:
            reconfigure(encoding="utf-8", errors="replace")


def read_url() -> str:
    """读取一个网址，并允许用户省略 HTTP(S) 协议头。"""
    while True:
        raw = input("请输入要抓取的网页地址：").strip()
        if not raw:
            print("网页地址不能为空，请重新输入。")
            continue
        candidate = raw if "://" in raw else f"https://{raw}"
        url = normalize_url(candidate)
        if url is None:
            print("地址格式不正确，例如：https://example.com/。")
            continue
        return url


def main() -> int:
    configure_console_encoding()
    print("=" * 52)
    print("              真实网页爬虫")
    print("=" * 52)
    print("只需输入一个网页地址，程序将使用默认配置自动抓取。")
    print(f"默认配置：最多 {DEFAULT_MAX_PAGES} 页，深度 {DEFAULT_MAX_DEPTH}，并发 {DEFAULT_WORKERS}，同域限速 {DEFAULT_DELAY} 秒。")
    try:
        seed = read_url()
        config = CrawlConfig(
            seeds=[seed],
            max_pages=DEFAULT_MAX_PAGES,
            max_depth=DEFAULT_MAX_DEPTH,
            workers=DEFAULT_WORKERS,
            timeout=DEFAULT_TIMEOUT,
            delay=DEFAULT_DELAY,
            max_bytes=DEFAULT_MAX_BYTES,
            same_domain=True,
            respect_robots=True,
            user_agent=DEFAULT_USER_AGENT,
            raw_dir=DEFAULT_RAW_DIR,
        )
        logging.basicConfig(level=logging.WARNING, format="%(levelname)s %(message)s")
        print(f"正在抓取：{seed}")
        started = time.perf_counter()
        records = WebCrawler(config).crawl()
        write_records(records, DEFAULT_OUTPUT, "jsonl")
        elapsed = time.perf_counter() - started
        success = sum(1 for record in records if record.status and not record.error and record.text)
        summary = {
            "抓取页数": len(records),
            "成功网页": success,
            "失败或跳过": len(records) - success,
            "总字节数": sum(record.bytes for record in records),
            "耗时秒数": round(elapsed, 3),
            "结果文件": str(DEFAULT_OUTPUT),
        }
        print("\n--- 抓取结果 ---")
        print(json.dumps(summary, ensure_ascii=False, indent=2))
        for record in records:
            if record.error:
                print(f"失败：{record.url} -> {record.error}")
        if success == 0:
            print("没有抓到可解析的 HTML 页面，请检查网址、网络连接或目标站点是否拒绝访问。")
            return 1
        return 0
    except KeyboardInterrupt:
        print("\n用户中断抓取。")
        return 130
    except (OSError, ValueError) as exc:
        print(f"\n运行失败：{exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
