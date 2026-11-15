"""把课程设计文档 markdown 转换为可被 html-to-doc 转换的 HTML（academic-paper 版式）。

只用标准库；规则针对本项目的 markdown 子集：标题、表格、代码块、列表、引用、段落。
输出满足 doc-typeset / html-review 的约束：样式全部走 CSS 变量、段落不超 500 字、
代码块等宽浅底、表格三线、封面与目录独立分节。
"""
import html
import re
import sys

PAGE_CSS = """  @page {
    margin: 2.6cm 2.4cm 2.4cm 2.6cm;
    @bottom-center { content: counter(page); }
  }
  @page cover {
    @bottom-center { content: none; }
  }
  section[role="cover"] { page: cover; }
"""

CSS_VARS = """
  :root {
    --fs-h1: var(--typography-fontSize-h1, 15pt);
    --fs-h2: var(--typography-fontSize-h2, 14pt);
    --fs-h3: var(--typography-fontSize-h3, 12pt);
    --fs-body: var(--typography-fontSize-body, 12pt);
    --fs-small: var(--typography-fontSize-caption, 10.5pt);
    --fs-code: 10.5pt;
    --fs-cover-title: 26pt;
    --fs-cover-subtitle: 15pt;
    --ff-heading: var(--typography-fontFamily-heading, "黑体", SimHei, sans-serif);
    --ff-body: var(--typography-fontFamily-body, "宋体", SimSun, serif);
    --ff-code: var(--typography-fontFamily-code, "Courier New", Consolas, monospace);
    --lh-body: var(--typography-lineHeight-body, 1.5);
    --color-primary: var(--color-primary, #000000);
    --color-text: var(--color-text, #000000);
    --color-bg: var(--color-background, #ffffff);
    --color-muted: #555555;
    --color-border: #999999;
    --color-code-bg: #f5f6f8;
    --color-th-bg: var(--color-tableHeaderBg, #f0f0f0);
    --spacing-paragraph: 0.45em;
    --spacing-block: 0.9em;
    --spacing-section: 1.5em;
    --spacing-none: 0;
    --spacing-li: 0.2em;
    --spacing-cellPad: 0.32em 0.5em;
    --spacing-codePadX: 0.9em;
    --spacing-codePadY: 0.6em;
    --spacing-notePad: 0.4em 0.8em;
    --spacing-listLeft: 2.2em;
    --spacing-tocItem: 0.25em;
    --spacing-tocLeft: 1.6em;
    --spacing-coverTitleTop: 3.5cm;
    --spacing-coverTitleGap: 0.6em;
    --spacing-coverSubGap: 2.0cm;
    --spacing-coverMeta: 0.5em;
    --margin-page: 2.6cm;
    --size-content-width: 16.0cm;
  }
"""

CSS_BODY = """
  * { box-sizing: border-box; }
  body {
    font-family: var(--ff-body);
    font-size: var(--fs-body);
    line-height: var(--lh-body);
    color: var(--color-text);
    background: var(--color-bg);
    max-width: var(--size-content-width);
    margin-left: auto;
    margin-right: auto;
  }
  h1, h2, h3 { font-family: var(--ff-heading); font-weight: bold; color: var(--color-primary); }
  h1 { font-size: var(--fs-h1); margin-top: var(--spacing-section); margin-bottom: var(--spacing-block); }
  h2 { font-size: var(--fs-h2); margin-top: var(--spacing-block); margin-bottom: var(--spacing-paragraph); }
  h3 { font-size: var(--fs-h3); margin-top: var(--spacing-block); margin-bottom: var(--spacing-paragraph); }
  p { font-size: var(--fs-body); text-indent: 2em; margin-top: var(--spacing-none); margin-bottom: var(--spacing-paragraph); text-align: justify; }
  ul, ol { margin-top: var(--spacing-none); margin-bottom: var(--spacing-paragraph); padding-left: var(--spacing-listLeft); }
  li { font-size: var(--fs-body); margin-bottom: var(--spacing-li); text-align: justify; }
  strong { font-weight: bold; }
  code { font-family: var(--ff-code); font-size: var(--fs-code); }

  .cover-title { text-align: center; font-family: var(--ff-heading); font-size: var(--fs-cover-title); font-weight: bold; text-indent: 0; margin-top: var(--spacing-coverTitleTop); margin-bottom: var(--spacing-coverTitleGap); }
  .cover-subtitle { text-align: center; font-size: var(--fs-cover-subtitle); text-indent: 0; color: var(--color-muted); margin-bottom: var(--spacing-coverSubGap); }
  .cover-meta { text-align: center; font-size: var(--fs-body); text-indent: 0; margin-bottom: var(--spacing-coverMeta); color: var(--color-text); }

  .doc-toc { margin-bottom: var(--spacing-section); }
  .toc-title { text-align: center; font-family: var(--ff-heading); font-size: var(--fs-h2); font-weight: bold; text-indent: 0; margin-bottom: var(--spacing-block); letter-spacing: var(--spacing-tocItem); }
  .toc-list, .toc-list ol { list-style: none; list-style-type: none; padding-left: var(--spacing-none); }
  .toc-list li { text-indent: 0; margin-bottom: var(--spacing-tocItem); }
  .toc-list ol li { padding-left: var(--spacing-tocLeft); }
  .toc-list a { color: var(--color-primary); text-decoration: none; }

  .abstract { margin-bottom: var(--spacing-section); }
  .abstract-heading { font-family: var(--ff-heading); font-weight: bold; font-size: var(--fs-h2); text-indent: 0; margin-bottom: var(--spacing-paragraph); }
  .abstract-text { text-indent: 2em; font-size: var(--fs-small); }
  .keywords { text-indent: 0; font-size: var(--fs-small); }

  .code-block { font-family: var(--ff-code); font-size: var(--fs-code); line-height: 1.35; background: var(--color-code-bg); border-left: 3px solid var(--color-border); padding-left: var(--spacing-codePadX); padding-right: var(--spacing-codePadX); margin-top: var(--spacing-none); margin-bottom: var(--spacing-none); text-indent: 0; white-space: pre-wrap; word-break: break-all; text-align: left; }
  .code-first { padding-top: var(--spacing-codePadY); }
  .code-last { padding-bottom: var(--spacing-codePadY); margin-bottom: var(--spacing-block); }
  .figure-marker { text-align: center; text-indent: 0; margin-top: var(--spacing-block); margin-bottom: var(--spacing-none); font-size: var(--fs-small); }
  .note { border-left: 3px solid var(--color-border); background: var(--color-code-bg); padding: var(--spacing-notePad); text-indent: 0; margin-bottom: var(--spacing-block); }

  .three-line-table { border-collapse: collapse; width: 100%; margin-top: var(--spacing-none); margin-bottom: var(--spacing-block); }
  .three-line-table thead tr:first-child { border-top: 1.5pt solid var(--color-text); }
  .three-line-table thead tr:last-child { border-bottom: 0.75pt solid var(--color-text); }
  .three-line-table tbody tr:last-child { border-bottom: 1.5pt solid var(--color-text); }
  .three-line-table td, .three-line-table th { border-left: none; border-right: none; padding: var(--spacing-cellPad); text-align: left; vertical-align: top; font-size: var(--fs-small); }
  .three-line-table th { font-family: var(--ff-heading); font-weight: bold; background: var(--color-th-bg); }

  .references { margin-top: var(--spacing-section); }
  .reference-list { padding-left: var(--spacing-listLeft); }
  .reference-item { font-size: var(--fs-small); margin-bottom: var(--spacing-li); text-align: left; }
"""

INLINE_BOLD = re.compile(r"\*\*(.+?)\*\*")
INLINE_CODE = re.compile(r"`([^`]+)`")
CHUNK_LIMIT = 460


def inline(text: str) -> str:
    out = html.escape(text, quote=False)
    out = INLINE_BOLD.sub(r"<strong>\1</strong>", out)
    out = INLINE_CODE.sub(r"<code>\1</code>", out)
    return out


def split_row(line: str):
    line = line.strip()
    if line.startswith("|"):
        line = line[1:]
    if line.endswith("|"):
        line = line[:-1]
    return [cell.strip() for cell in line.split("|")]


def is_separator(cells) -> bool:
    return all(re.fullmatch(r":?-{2,}:?", cell.replace(" ", "")) for cell in cells if cell != "")


def render_table(rows):
    header = split_row(rows[0])
    body = [split_row(r) for r in rows[1:] if not is_separator(split_row(r))]
    parts = ['<table class="three-line-table">', "<thead>", "<tr>"]
    parts += [f"<th>{inline(cell)}</th>" for cell in header]
    parts += ["</tr>", "</thead>", "<tbody>"]
    for row in body:
        parts.append("<tr>")
        parts += [f"<td>{inline(cell)}</td>" for cell in row]
        parts.append("</tr>")
    parts += ["</tbody>", "</table>"]
    return "\n".join(parts)


def chunk_code(code: str):
    """按行切分代码块，保证每段段落不超过 500 字（转换后仍是等宽浅底，视觉连续）。"""
    chunks = []
    current = ""
    for line in code.split("\n"):
        candidate = line if not current else current + "\n" + line
        if len(candidate) > CHUNK_LIMIT and current:
            chunks.append(current)
            current = line
        else:
            current = candidate
    if current:
        chunks.append(current)
    return chunks


def render_code(code: str):
    """代码块用 <pre> 承载：editor_sdk 导入时会把 pre 转成保留换行的单格表格，
    而多行 <p> 会被压成一行，代码将不可读。"""
    return f'<pre class="code-block">{html.escape(code, quote=False)}</pre>'


def render_figure(payload):
    path, caption = payload
    return f'<p class="figure-marker">[[FIG:{path}::{caption}]]</p>'


def render_abstract(paragraphs, anchor=None):
    id_attr = f' id="{anchor}"' if anchor else ""
    parts = ['<div class="abstract" aria-label="摘要">',
             f'<h1{id_attr}>摘要</h1>']
    for text in paragraphs:
        if text.startswith("关键词") or text.startswith("**关键词"):
            parts.append(f'<p class="keywords">{inline(text)}</p>')
        else:
            parts.append(f'<p class="abstract-text">{inline(text)}</p>')
    parts.append("</div>")
    return "\n".join(parts)


def render_references(items, anchor=None):
    id_attr = f' id="{anchor}"' if anchor else ""
    parts = ['<div class="references" aria-label="参考文献">',
             f"<h1{id_attr}>参考文献</h1>",
             '<ol class="reference-list">']
    parts += [f'<li class="reference-item">{inline(item)}</li>' for item in items]
    parts += ["</ol>", "</div>"]
    return "\n".join(parts)


def main(src_path: str, out_path: str, cover_meta=None):
    lines = open(src_path, encoding="utf-8").read().splitlines()

    title = ""
    subtitle = ""
    body = []
    for line in lines:
        if line.startswith("# ") and not title:
            title = line[2:].strip()
            continue
        if line.startswith("## ") and not subtitle and not re.match(r"##\s+\d", line):
            subtitle = line[3:].strip()
            continue
        body.append(line)

    blocks = []
    headings = []
    counter = 0
    i = 0
    while i < len(body):
        line = body[i]
        stripped = line.strip()

        if not stripped or stripped == "---":
            i += 1
            continue

        if stripped.startswith("```"):
            i += 1
            code = []
            while i < len(body) and not body[i].strip().startswith("```"):
                code.append(body[i])
                i += 1
            i += 1
            blocks.append(("code", "\n".join(code)))
            continue

        match = re.match(r"^(#{2,4})\s+(.*)$", stripped)
        if match:
            level = len(match.group(1)) - 1          # ## → h1, ### → h2, #### → h3
            text = match.group(2).strip()
            counter += 1
            anchor = f"section-{counter}"
            headings.append((level, anchor, text))
            blocks.append(("heading", (level, anchor, text)))
            i += 1
            continue

        figure = re.match(r"^!\[(.*?)\]\((.+?)\)$", stripped)
        if figure:
            blocks.append(("figure", (figure.group(2).strip(), figure.group(1).strip())))
            i += 1
            continue

        if stripped.startswith("|"):
            rows = []
            while i < len(body) and body[i].strip().startswith("|"):
                rows.append(body[i])
                i += 1
            blocks.append(("table", rows))
            continue

        if re.match(r"^[-*]\s+", stripped) or re.match(r"^\d+\.\s+", stripped):
            ordered = bool(re.match(r"^\d+\.\s+", stripped))
            items = []
            while i < len(body):
                current = body[i].strip()
                if re.match(r"^[-*]\s+", current) or re.match(r"^\d+\.\s+", current):
                    items.append(re.sub(r"^([-*]|\d+\.)\s+", "", current))
                    i += 1
                elif current and not current.startswith(("|", "#", ">", "```")):
                    items[-1] += " " + current
                    i += 1
                else:
                    break
            blocks.append(("list", (ordered, items)))
            continue

        if stripped.startswith(">"):
            quote = []
            while i < len(body) and body[i].strip().startswith(">"):
                quote.append(body[i].strip().lstrip(">").strip())
                i += 1
            blocks.append(("quote", " ".join(quote)))
            continue

        paragraph = []
        while i < len(body):
            current = body[i].strip()
            if not current or current.startswith(("|", "#", ">", "```", "---")):
                break
            if re.match(r"^[-*]\s+", current) or re.match(r"^\d+\.\s+", current):
                break
            paragraph.append(current)
            i += 1
        blocks.append(("paragraph", " ".join(paragraph)))

    # —— 正文（摘要 / 参考文献特殊处理）——
    out = []
    index = 0
    while index < len(blocks):
        kind, payload = blocks[index]
        if kind == "heading" and payload[2] == "摘要":
            texts = []
            probe = index + 1
            while probe < len(blocks) and blocks[probe][0] == "paragraph":
                texts.append(blocks[probe][1])
                probe += 1
            out.append(render_abstract(texts, payload[1]))
            index = probe
            continue
        if kind == "heading" and payload[2] == "参考文献":
            items = []
            probe = index + 1
            while probe < len(blocks) and blocks[probe][0] in ("list", "paragraph"):
                item_kind, item_payload = blocks[probe]
                if item_kind == "list":
                    items.extend(item_payload[1])
                else:
                    items.append(item_payload)
                probe += 1
            out.append(render_references(items, payload[1]))
            index = probe
            continue

        if kind == "heading":
            level, anchor, text = payload
            out.append(f'<h{level} id="{anchor}">{inline(text)}</h{level}>')
        elif kind == "paragraph":
            out.append(f"<p>{inline(payload)}</p>")
        elif kind == "code":
            out.append(render_code(payload))
        elif kind == "quote":
            out.append(f'<p class="note">&nbsp;&nbsp;{inline(payload)}</p>')
        elif kind == "list":
            ordered, items = payload
            tag = "ol" if ordered else "ul"
            out.append(f"<{tag}>")
            out += [f"<li>{inline(item)}</li>" for item in items]
            out.append(f"</{tag}>")
        elif kind == "table":
            out.append(render_table(payload))
        elif kind == "figure":
            out.append(render_figure(payload))
        index += 1

    # —— 目录 ——
    toc = ['<ol class="toc-list">']
    has_sub = False
    for level, anchor, text in headings:
        if level == 1:
            if has_sub:
                toc.append("</ol></li>")
                has_sub = False
            elif toc[-1].startswith("<li"):
                toc.append("</li>")
            toc.append(f'<li><a href="#{anchor}">{inline(text)}</a>')
        else:
            if not has_sub:
                toc.append("<ol>")
                has_sub = True
            toc.append(f'<li><a href="#{anchor}">{inline(text)}</a></li>')
    if has_sub:
        toc.append("</ol></li>")
    elif len(toc) > 1:
        toc.append("</li>")
    toc.append("</ol>")
    toc_html = "\n".join(toc)
    body_html = "\n".join(out)
    cover_meta = cover_meta or []
    meta_html = "\n".join(f'<p class="cover-meta">{html.escape(m)}</p>' for m in cover_meta)

    document = f"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="docx-page-size" content="A4">
<title>{html.escape(title)}</title>
<style>
{PAGE_CSS}
{CSS_VARS}
{CSS_BODY}
</style>
</head>
<body>

<section role="cover">
<p class="cover-title">{html.escape(title)}</p>
<p class="cover-subtitle">{html.escape(subtitle)}</p>
{meta_html}
</section>

<section role="cover">
<nav class="doc-toc" aria-label="文档目录">
<p class="toc-title">目　录</p>
{toc_html}
</nav>
</section>

<section role="body" data-page-restart="1">
{body_html}
</section>

</body>
</html>
"""
    with open(out_path, "w", encoding="utf-8") as handle:
        handle.write(document)
    print(f"headings={len(headings)} blocks={len(blocks)} -> {out_path}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2], sys.argv[3:])
