from __future__ import annotations

import json
import re
from pathlib import Path
from statistics import mean

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_CELL_VERTICAL_ALIGNMENT, WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK, WD_LINE_SPACING
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Inches, Pt, RGBColor


ROOT = Path(__file__).resolve().parent
OUT = ROOT / "网络爬虫实验报告.docx"
DATA_DIR = ROOT / "data"


def set_cell_shading(cell, fill: str) -> None:
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = tc_pr.find(qn("w:shd"))
    if shd is None:
        shd = OxmlElement("w:shd")
        tc_pr.append(shd)
    shd.set(qn("w:fill"), fill)


def set_cell_margins(cell, top=100, start=120, bottom=100, end=120) -> None:
    tc = cell._tc
    tc_pr = tc.get_or_add_tcPr()
    tc_mar = tc_pr.first_child_found_in("w:tcMar")
    if tc_mar is None:
        tc_mar = OxmlElement("w:tcMar")
        tc_pr.append(tc_mar)
    for margin, value in (("top", top), ("start", start), ("bottom", bottom), ("end", end)):
        node = tc_mar.find(qn(f"w:{margin}"))
        if node is None:
            node = OxmlElement(f"w:{margin}")
            tc_mar.append(node)
        node.set(qn("w:w"), str(value))
        node.set(qn("w:type"), "dxa")


def set_table_borders(table, color="B7C4D6", size="6") -> None:
    tbl = table._tbl
    tbl_pr = tbl.tblPr
    borders = tbl_pr.first_child_found_in("w:tblBorders")
    if borders is None:
        borders = OxmlElement("w:tblBorders")
        tbl_pr.append(borders)
    for edge in ("top", "left", "bottom", "right", "insideH", "insideV"):
        tag = f"w:{edge}"
        element = borders.find(qn(tag))
        if element is None:
            element = OxmlElement(tag)
            borders.append(element)
        element.set(qn("w:val"), "single")
        element.set(qn("w:sz"), size)
        element.set(qn("w:space"), "0")
        element.set(qn("w:color"), color)


def set_repeat_table_header(row) -> None:
    tr_pr = row._tr.get_or_add_trPr()
    tbl_header = OxmlElement("w:tblHeader")
    tbl_header.set(qn("w:val"), "true")
    tr_pr.append(tbl_header)


def set_cell_text(cell, text: str, bold=False, color="000000", size=10.5, align=None) -> None:
    cell.text = ""
    paragraph = cell.paragraphs[0]
    if align is not None:
        paragraph.alignment = align
    paragraph.paragraph_format.space_after = Pt(0)
    paragraph.paragraph_format.line_spacing = 1.05
    run = paragraph.add_run(str(text))
    run.bold = bold
    run.font.name = "Microsoft YaHei"
    run._element.rPr.rFonts.set(qn("w:eastAsia"), "Microsoft YaHei")
    run.font.size = Pt(size)
    run.font.color.rgb = RGBColor.from_string(color)
    cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
    set_cell_margins(cell)


def add_table(doc, headers, rows, widths=None, header_fill="1F4E79"):
    table = doc.add_table(rows=1, cols=len(headers))
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    set_table_borders(table)
    header = table.rows[0]
    set_repeat_table_header(header)
    for i, title in enumerate(headers):
        set_cell_text(header.cells[i], title, bold=True, color="FFFFFF", size=10)
        set_cell_shading(header.cells[i], header_fill)
        if widths:
            header.cells[i].width = Cm(widths[i])
    for row_index, row_data in enumerate(rows):
        row = table.add_row()
        for i, value in enumerate(row_data):
            set_cell_text(row.cells[i], value, size=9.5)
            if widths:
                row.cells[i].width = Cm(widths[i])
            if row_index % 2 == 1:
                set_cell_shading(row.cells[i], "F3F6FA")
    doc.add_paragraph().paragraph_format.space_after = Pt(2)
    return table


def set_run_font(run, name="SimSun", size=12, bold=False, color="000000"):
    run.font.name = name
    run._element.rPr.rFonts.set(qn("w:eastAsia"), name)
    run.font.size = Pt(size)
    run.bold = bold
    run.font.color.rgb = RGBColor.from_string(color)


def add_para(doc, text="", style=None, first_line=True, align=None, space_after=6, line=1.35):
    p = doc.add_paragraph(style=style)
    if align is not None:
        p.alignment = align
    p.paragraph_format.space_after = Pt(space_after)
    p.paragraph_format.line_spacing = line
    if first_line and text:
        p.paragraph_format.first_line_indent = Cm(0.74)
    if text:
        set_run_font(p.add_run(text))
    return p


def add_bullet(doc, text, level=0):
    p = doc.add_paragraph(style="List Bullet" if level == 0 else "List Bullet 2")
    p.paragraph_format.space_after = Pt(4)
    p.paragraph_format.line_spacing = 1.25
    set_run_font(p.add_run(text), size=11.5)
    return p


def add_heading(doc, text, level=1):
    p = doc.add_paragraph(style=f"Heading {level}")
    if getattr(doc, "_pending_page_break", False):
        p.paragraph_format.page_break_before = True
        doc._pending_page_break = False
    p.paragraph_format.keep_with_next = True
    p.paragraph_format.space_before = Pt(10 if level == 1 else 6)
    p.paragraph_format.space_after = Pt(5)
    run = p.add_run(text)
    set_run_font(run, name="Microsoft YaHei", size=16 if level == 1 else 13, bold=True)
    return p


def add_code(doc, text):
    for line in text.strip("\n").splitlines():
        p = doc.add_paragraph()
        p.paragraph_format.left_indent = Cm(0.5)
        p.paragraph_format.right_indent = Cm(0.5)
        p.paragraph_format.space_after = Pt(1)
        p.paragraph_format.line_spacing = 1.05
        run = p.add_run(line)
        set_run_font(run, name="Consolas", size=8.7, color="1F2937")
        pPr = p._p.get_or_add_pPr()
        shd = OxmlElement("w:shd")
        shd.set(qn("w:fill"), "F3F4F6")
        pPr.append(shd)


def add_page_break(doc):
    # 封面后保留一次分页，正文其余部分自然流动，避免表格占满页面时产生空白页。
    count = getattr(doc, "_page_break_count", 0)
    if count == 0:
        doc.add_page_break()
    doc._page_break_count = count + 1


def add_page_number(paragraph):
    paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = paragraph.add_run("第 ")
    set_run_font(run, name="Microsoft YaHei", size=9, color="666666")
    fld_char1 = OxmlElement("w:fldChar")
    fld_char1.set(qn("w:fldCharType"), "begin")
    instr_text = OxmlElement("w:instrText")
    instr_text.set(qn("xml:space"), "preserve")
    instr_text.text = "PAGE"
    fld_char2 = OxmlElement("w:fldChar")
    fld_char2.set(qn("w:fldCharType"), "end")
    run._r.append(fld_char1)
    run._r.append(instr_text)
    run._r.append(fld_char2)
    run2 = paragraph.add_run(" 页")
    set_run_font(run2, name="Microsoft YaHei", size=9, color="666666")


def load_records(filename):
    path = DATA_DIR / filename
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines() if line.strip()]


def setup_document():
    doc = Document()
    section = doc.sections[0]
    section.page_width = Cm(21)
    section.page_height = Cm(29.7)
    section.top_margin = Cm(2.35)
    section.bottom_margin = Cm(2.2)
    section.left_margin = Cm(2.55)
    section.right_margin = Cm(2.35)
    section.header_distance = Cm(1.1)
    section.footer_distance = Cm(1.1)
    styles = doc.styles
    normal = styles["Normal"]
    normal.font.name = "SimSun"
    normal._element.rPr.rFonts.set(qn("w:eastAsia"), "SimSun")
    normal.font.size = Pt(12)
    normal.paragraph_format.line_spacing = 1.35
    normal.paragraph_format.space_after = Pt(6)
    for style_name, size in (("Heading 1", 16), ("Heading 2", 13)):
        style = styles[style_name]
        style.font.name = "Microsoft YaHei"
        style._element.rPr.rFonts.set(qn("w:eastAsia"), "Microsoft YaHei")
        style.font.size = Pt(size)
        style.font.bold = True
        style.font.color.rgb = RGBColor(0, 0, 0)
    styles["Title"].font.name = "Microsoft YaHei"
    styles["Title"]._element.rPr.rFonts.set(qn("w:eastAsia"), "Microsoft YaHei")
    styles["Title"].font.size = Pt(24)
    styles["Title"].font.bold = True
    styles["Title"].font.color.rgb = RGBColor(0, 0, 0)
    header = section.header.paragraphs[0]
    header.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    set_run_font(header.add_run("网络爬虫实验报告"), name="Microsoft YaHei", size=9, color="666666")
    add_page_number(section.footer.paragraphs[0])
    return doc


def main():
    example = load_records("report-example.jsonl")
    iana = load_records("report-iana.jsonl")
    w3 = load_records("report-w3.jsonl")
    doc = setup_document()

    # 封面
    p = doc.add_paragraph(style="Title")
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(80)
    p.paragraph_format.space_after = Pt(22)
    set_run_font(p.add_run("网络爬虫实验报告"), name="Microsoft YaHei", size=26, bold=True)
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    set_run_font(p.add_run("真实网页抓取与网络蜘蛛定制"), name="Microsoft YaHei", size=16, color="1F4E79")
    p.paragraph_format.space_after = Pt(45)
    add_table(doc, ["项目", "填写内容"], [
        ("姓名", "________________"),
        ("学号", "________________"),
        ("专业与班级", "________________"),
        ("指导教师", "________________"),
        ("实验日期", "2026 年 11 月 15 日"),
    ], widths=[4.0, 10.0], header_fill="1F4E79")
    add_para(doc, "本报告围绕网络爬虫的基本原理、真实网页抓取、抓取性能分析和网络蜘蛛定制展开。实验程序能够从互联网获取网页内容，提取标题、正文和超链接，并将结果保存为 JSONL 与原始 HTML 文件。", first_line=False, align=WD_ALIGN_PARAGRAPH.CENTER, space_after=0)
    add_page_break(doc)

    # 摘要与目录
    add_heading(doc, "摘要", 1)
    add_para(doc, "本实验设计并实现了一个基于 Python 标准库的真实网页爬虫。程序从用户提供的种子网址开始，按照广度优先策略访问网页，完成 URL 规范化、robots.txt 检查、HTTP 请求、字符集识别、HTML 解析、正文提取、链接发现和结果持久化等步骤。实验以 example.com、iana.org 和 W3C 三类典型网站为对象，记录页面数量、状态码、响应大小、解析文本长度、访问耗时和被 robots.txt 拦截的情况。实测结果表明，爬虫能够稳定抓取静态 HTML 页面，并能将网站策略造成的不可抓取页面记录为明确的失败原因。")
    add_para(doc, "通过本实验可以理解网络蜘蛛的工作链路，并掌握抓取范围、并发数、请求超时、访问间隔和原始页面保存等参数对实验结果的影响。实验同时保留了完整的可运行代码和结果文件，为后续开展网页清洗、倒排索引或抓取性能对比提供了数据基础。")
    add_para(doc, "本报告不仅描述程序能够完成什么，还说明程序为什么这样设计、每个数据字段如何产生、实验数据如何计算以及异常结果如何解释。读者可以根据报告中的命令和默认参数复现实验，并把 JSONL 结果继续用于文本清洗、分词或搜索引擎索引。")
    add_para(doc, "关键词：网络爬虫；网页抓取；HTML 解析；robots.txt；性能分析；Python", first_line=False)
    add_heading(doc, "目录", 1)
    toc_rows = [
        ("1", "实验目标与内容", "3"), ("2", "实验前准备与环境", "3"),
        ("3", "网络爬虫原理与流程", "4"), ("4", "抓取工具原理分析", "6"),
        ("5", "系统设计与程序实现", "7"), ("6", "交互式使用流程", "8"),
        ("7", "实验设计与数据采集", "9"), ("8", "抓取性能分析结果", "10"),
        ("9", "性能差异讨论", "12"), ("10", "网络蜘蛛定制", "13"),
        ("11", "问题、改进与结论", "14"), ("附录", "关键代码与运行记录", "15"),
    ]
    add_table(doc, ["章节", "标题", "页码"], toc_rows, widths=[2.0, 10.0, 2.0])
    add_page_break(doc)

    # 第1页正文
    add_heading(doc, "1 实验目标与内容", 1)
    add_para(doc, "本实验的目标是理解和熟悉网络爬虫的基本原理，能够编译、运行和测试一个给定的网络爬虫程序，并能够对部分功能进行修改与定制。实验不以生成静态示例为目的，而是要求程序访问真实网页，得到可以复查的页面内容和运行数据。")
    add_heading(doc, "1.1 实验主要内容", 2)
    add_bullet(doc, "理解种子网址、URL 队列、网页下载、链接发现和去重之间的关系。")
    add_bullet(doc, "运行真实网页抓取程序，获取标题、正文、页面大小、状态码和超链接。")
    add_bullet(doc, "记录不同网站的抓取速度、页面数量、响应大小和失败情况。")
    add_bullet(doc, "修改爬虫关键参数，实现同域限制、robots.txt 遵守、请求限速和原始 HTML 保存。")
    add_bullet(doc, "分析实验结果，说明网站结构、网络延迟和访问策略对抓取结果的影响。")
    add_heading(doc, "1.2 实验要求对应关系", 2)
    add_table(doc, ["图片要求", "本报告对应内容"], [
        ("个人信息", "封面填写表格，姓名、学号、专业和班级可直接补充"),
        ("实验目标和内容简介", "第 1 章和摘要"),
        ("网络蜘蛛原理和流程简介", "第 3 章"),
        ("抓取工具原理分析", "第 4 章"),
        ("抓取性能分析", "第 7 至第 9 章，包含三类网站实测表格"),
        ("网络蜘蛛定制", "第 10 章，说明代码修改和运行参数"),
    ], widths=[5.0, 9.0])
    add_heading(doc, "1.3 预期成果与验收标准", 2)
    add_table(doc, ["成果", "验收标准", "本实验结果"], [
        ("可运行程序", "能够从命令行或交互入口启动", "crawler.py 和 interactive_crawler.py 均可启动"),
        ("真实抓取", "响应来自公开网站而非本地模拟数据", "三组样本均产生真实 HTTP/HTTPS 记录"),
        ("网页解析", "至少得到标题、正文和链接", "CrawlRecord 保存 title、text 和 links"),
        ("性能数据", "记录页面数、大小、耗时和速度", "JSONL 与汇总表均包含这些指标"),
        ("功能定制", "至少实现范围、限速或保存策略修改", "同域限制、robots、限速和原始 HTML 均已实现"),
    ], widths=[3.5, 5.2, 5.3])
    add_heading(doc, "1.4 报告阅读方法", 2)
    add_para(doc, "第 3 至第 6 章先解释程序的工作原理和实现方法，第 7 至第 9 章再给出实验设计、原始数据和性能结论，第 10 至第 11 章说明定制内容、问题处理和改进方向。附录保留关键代码、运行记录和文件清单，便于检查报告中的技术描述是否与程序一致。")
    add_page_break(doc)

    # 第2页
    add_heading(doc, "2 实验前准备与环境", 1)
    add_para(doc, "实验前需要阅读网络爬虫相关资料，理解 HTTP 请求、HTML 文档、超链接、robots.txt 和网页解析的基本概念。程序运行不依赖复杂的爬虫框架，便于在 VC++ 或其他开发平台中理解程序结构，也便于将抓取结果作为后续搜索引擎实验的输入。")
    add_heading(doc, "2.1 软件与硬件环境", 2)
    add_table(doc, ["项目", "配置"], [
        ("操作系统", "Windows 10/11，PowerShell"),
        ("编程语言", "Python 3.10 及以上版本"),
        ("主要模块", "urllib、urllib.robotparser、html.parser、concurrent.futures"),
        ("数据格式", "JSONL、CSV、HTML"),
        ("网络条件", "能够访问公开 HTTP/HTTPS 网页"),
        ("代码目录", "D:\\work\\end-project\\information-work\\big-work1"),
    ], widths=[4.0, 10.0])
    add_heading(doc, "2.2 运行准备", 2)
    add_para(doc, "将 crawler.py、interactive_crawler.py 和 README.md 放在同一目录。交互运行时执行 python interactive_crawler.py，输入一个网页地址即可开始抓取。命令行运行时可指定最大页数和最大深度，实验结果会写入 data/crawl 目录。")
    add_code(doc, "python .\\interactive_crawler.py\n\npython .\\crawler.py https://www.iana.org/domains/example --max-pages 5 --max-depth 1")
    add_heading(doc, "2.3 实验安全与合规约束", 2)
    add_para(doc, "实验程序默认遵守 robots.txt，只跟踪种子网址所在域名，并设置最大页数、最大响应大小和请求间隔。实验只选取公开网页作为样本，不绕过登录、验证码或访问控制，不对目标网站进行高频请求。")
    add_heading(doc, "2.4 安装与启动检查", 2)
    add_table(doc, ["检查项", "命令或现象", "处理方式"], [
        ("Python 版本", "python --version", "确保版本不低于 3.10"),
        ("模块导入", "运行 crawler.py 不出现 ImportError", "本程序只依赖标准库，certifi 为可选 CA 包"),
        ("网络连通", "能够访问 example.com", "检查代理、防火墙和 DNS"),
        ("证书校验", "HTTPS 请求出现证书错误", "程序优先读取 certifi 证书链"),
        ("文件写入", "data/crawl 下生成 JSONL 和 HTML", "检查当前目录的写权限"),
    ], widths=[3.5, 5.0, 5.5])
    add_heading(doc, "2.5 实验前数据清理", 2)
    add_para(doc, "每轮实验应使用独立的结果文件和原始 HTML 目录，避免旧结果与新结果混在一起。报告中的三组数据分别写入 report-example.jsonl、report-iana.jsonl 和 report-w3.jsonl，原始页面也分别保存在对应目录中。这样可以在实验结束后按网站单独统计，不会因为重复运行而覆盖或误合并数据。")
    add_page_break(doc)

    # 第3页
    add_heading(doc, "3 网络爬虫原理与流程", 1)
    add_para(doc, "网络爬虫是一类按照预定规则自动访问互联网资源的程序。它从一个或多个种子网址开始，先下载网页，再从网页中提取新的超链接，把未访问的链接加入待抓取队列，循环执行直到达到页面数、深度或其他停止条件。")
    add_heading(doc, "3.1 基本流程", 2)
    add_table(doc, ["步骤", "处理内容", "本程序实现"], [
        ("1", "输入种子", "读取并规范化 HTTP/HTTPS 地址"),
        ("2", "范围检查", "根据种子域名过滤外部链接"),
        ("3", "规则检查", "读取 robots.txt 并判断是否允许访问"),
        ("4", "发送请求", "使用 User-Agent 和超时参数请求页面"),
        ("5", "解析页面", "提取标题、摘要、可见正文和 a 标签链接"),
        ("6", "加入队列", "去重后按深度加入下一轮待抓取队列"),
        ("7", "保存结果", "写入 JSONL/CSV，并保存原始 HTML"),
    ], widths=[2.0, 5.8, 6.2])
    add_heading(doc, "3.2 广度优先抓取", 2)
    add_para(doc, "程序使用 deque 维护待抓取队列。种子网页的深度为 0，从种子网页中发现的链接深度为 1。每轮从队列中取出一批网址交给线程池执行，抓取结果返回后再把下一层链接加入队列。通过 scheduled 集合和 seen 集合避免同一 URL 被重复安排和重复访问。")
    add_heading(doc, "3.3 数据流表示", 2)
    add_para(doc, "种子网址 -> URL 规范化 -> 域名与 robots 检查 -> HTTP/HTTPS 请求 -> 字符集识别 -> HTML 解析 -> 正文与链接提取 -> URL 去重入队 -> JSONL、CSV 和原始 HTML 输出", first_line=False, align=WD_ALIGN_PARAGRAPH.CENTER, space_after=10)
    add_para(doc, "该流程体现了网络蜘蛛的核心闭环：下载页面产生新的链接，新的链接扩大抓取范围，抓取范围又受到深度、页数、域名和 robots 规则约束。")
    add_heading(doc, "3.4 抓取算法伪代码", 2)
    add_code(doc, "queue <- [(seed, 0)]\nscheduled <- {seed}\nwhile queue is not empty and records < max_pages:\n    batch <- take at most workers URLs from queue\n    results <- fetch batch concurrently\n    for result in results:\n        save result\n        if result is valid and depth < max_depth:\n            for link in result.links:\n                if link is allowed and link not in scheduled:\n                    scheduled.add(link)\n                    queue.append((link, depth + 1))")
    add_para(doc, "伪代码中的 queue、scheduled 和 records 分别对应待处理任务、已安排任务和已完成记录。将“已安排”和“已完成”分开，可以避免同一个链接在不同线程的发现结果返回时被重复加入队列。")
    add_heading(doc, "3.5 停止条件与异常路径", 2)
    add_table(doc, ["条件", "触发时机", "程序行为"], [
        ("页数达到上限", "records 数量达到 max_pages", "不再安排新的 URL"),
        ("深度达到上限", "当前页面 depth >= max_depth", "保存当前页但不扩展其链接"),
        ("robots 禁止", "can_fetch 返回 False", "记录错误，不发送页面请求"),
        ("HTTP 或网络异常", "请求抛出异常", "保存状态或错误文本，继续其他任务"),
        ("响应过大或非 HTML", "超过 max_bytes 或内容类型不支持", "记录错误，不进入正文解析"),
    ], widths=[4.0, 5.0, 5.0])
    add_page_break(doc)

    # 第4页
    add_heading(doc, "4 抓取工具原理分析", 1)
    add_heading(doc, "4.1 urllib.request 与 HTTP 请求", 2)
    add_para(doc, "urllib.request 负责建立 HTTP/HTTPS 请求。程序设置 User-Agent、Accept 和 Accept-Encoding 头，声明自己是实验爬虫并优先请求 HTML 内容。timeout 控制单次请求最长等待时间，响应读取上限防止异常大文件占用过多内存。")
    add_heading(doc, "4.2 urllib.robotparser 与 robots.txt", 2)
    add_para(doc, "robots.txt 是网站公开声明的爬虫访问规则。程序先访问网站根目录的 robots.txt，将其解析为 RobotFileParser，再调用 can_fetch 判断当前 User-Agent 是否可以访问目标 URL。若 robots.txt 不存在或暂时无法读取，程序会记录调试信息并继续请求页面；若明确禁止，则记录 blocked by robots.txt。")
    add_heading(doc, "4.3 HTMLParser 页面解析", 2)
    add_para(doc, "HTMLParser 是 Python 标准库中的流式 HTML 解析器。自定义 HTMLDocumentParser 只保留实验所需信息：title 标签中的标题、meta description 中的摘要、非 script/style/noscript/template/svg 区域的可见文本，以及 a 标签 href 属性中的链接。这样可以去除脚本和样式噪声，得到适合后续索引的正文。")
    add_heading(doc, "4.4 URL 规范化", 2)
    add_para(doc, "normalize_url 使用 urljoin 处理相对链接，将主机名转为小写，将空路径转换为 /，移除 URL 片段，并过滤 fbclid、gclid 等常见跟踪参数。规范化后的 URL 作为去重键，也作为原始 HTML 文件名的哈希输入。")
    add_heading(doc, "4.5 ThreadPoolExecutor 并发", 2)
    add_para(doc, "线程池用于并发等待多个网络请求，从而减少单个请求等待时间对总耗时的影响。程序仍然按域名执行最小请求间隔，既保留并发能力，又避免短时间内向同一个网站发送过多请求。")
    add_table(doc, ["工具或模块", "作用", "实验价值"], [
        ("urllib.request", "构造和发送网页请求", "观察 HTTP 状态、内容类型和响应大小"),
        ("urllib.parse", "URL 解析、拼接和规范化", "处理相对链接与重复地址"),
        ("urllib.robotparser", "解析 robots.txt", "验证网站抓取限制"),
        ("HTMLParser", "解析 HTML 标签和文本", "提取标题、正文和超链接"),
        ("ThreadPoolExecutor", "并行执行请求", "测量并发抓取效率"),
    ], widths=[4.0, 5.0, 5.0])
    add_heading(doc, "4.6 字符集与内容类型处理", 2)
    add_para(doc, "网页可能通过 HTTP Content-Type 或 HTML 头部声明字符集。程序首先读取响应头中的 charset，再检查页面前 4096 字节中的 charset 声明，按 utf-8、gb18030 和 latin-1 顺序尝试解码。latin-1 作为最后的兼容选择可以避免因少量异常字节让整个任务终止，但对这类页面应在后续清洗阶段再次检查乱码。")
    add_para(doc, "程序只解析 text/html 和 application/xhtml+xml。图片、PDF、压缩包等资源仍然可以被请求，但会被标记为 unsupported content type，不会当作正文写入索引。该策略可以减少无关二进制内容对网页正文统计的干扰。")
    add_heading(doc, "4.7 TLS 与证书校验", 2)
    add_para(doc, "HTTPS 请求使用 ssl.create_default_context 创建 TLS 上下文。若运行环境安装了 certifi，程序使用 certifi 提供的 CA 文件；否则回退到系统默认 CA。程序没有默认关闭证书校验，避免把中间人响应误当成真实网页内容。")
    add_page_break(doc)

    # 第5页
    add_heading(doc, "5 系统设计与程序实现", 1)
    add_heading(doc, "5.1 模块结构", 2)
    add_table(doc, ["模块", "主要对象或函数", "职责"], [
        ("URL 处理", "normalize_url", "检查协议、解析主机、清理参数"),
        ("HTML 解析", "HTMLDocumentParser", "提取标题、摘要、正文和链接"),
        ("配置模型", "CrawlConfig", "保存页数、深度、线程、超时和规则"),
        ("结果模型", "CrawlRecord", "保存每个页面的完整抓取记录"),
        ("抓取引擎", "WebCrawler", "调度队列、robots、请求和解析"),
        ("结果输出", "write_records", "写入 JSONL 或 CSV"),
        ("交互入口", "interactive_crawler.main", "只输入 URL 后使用默认配置抓取"),
    ], widths=[3.0, 5.0, 6.0])
    add_heading(doc, "5.2 结果数据结构", 2)
    add_para(doc, "每条 CrawlRecord 包含 url、final_url、depth、status、content_type、title、description、text、links、bytes、elapsed_ms、fetched_at、error 和 raw_file 字段。这种结构同时保留了可检索内容和性能指标，既可以直接用于实验分析，也可以作为搜索引擎索引的输入。")
    add_heading(doc, "5.3 核心抓取代码", 2)
    crawler_text = (ROOT / "crawler.py").read_text(encoding="utf-8")
    match = re.search(r"    def _fetch\(self, url: str, depth: int\).*?    @staticmethod\n    def _decode", crawler_text, re.S)
    snippet = match.group(0).replace("    @staticmethod\n    def _decode", "") if match else "response = urllib.request.urlopen(request, timeout=self.config.timeout, context=self._ssl_context)"
    add_code(doc, "def _fetch(self, url: str, depth: int) -> _FetchResult:\n    # 记录请求时间、状态码、大小、正文和链接\n    with urllib.request.urlopen(request, timeout=self.config.timeout, context=self._ssl_context) as response:\n        record.status = getattr(response, \"status\", response.getcode())\n        payload = response.read(self.config.max_bytes + 1)\n        parser = HTMLDocumentParser()\n        parser.feed(self._decode(payload, response.headers.get_content_charset()))\n        record.title = parser.title\n        record.text = parser.text\n        record.links = sorted(set(parser.links))")
    add_para(doc, "程序将异常转换为记录中的 error 字段，而不是让一个坏链接终止整个任务。这样可以在实验结束后统计失败页面，并区分 HTTP 错误、robots 拦截、超时、证书问题和内容类型不支持等情况。")
    add_heading(doc, "5.4 函数调用关系", 2)
    add_table(doc, ["调用阶段", "入口函数", "调用对象", "输出"], [
        ("参数读取", "main", "build_parser、load_seeds", "CrawlConfig"),
        ("任务启动", "WebCrawler.crawl", "ThreadPoolExecutor", "多个 _FetchResult"),
        ("规则判断", "_fetch", "_is_allowed、_can_fetch", "允许或错误记录"),
        ("网页处理", "_fetch", "_decode、HTMLDocumentParser", "标题、正文、链接"),
        ("结果持久化", "write_records", "csv/json.dumps", "JSONL 或 CSV 文件"),
    ], widths=[3.0, 4.0, 4.0, 3.0])
    add_heading(doc, "5.5 复杂度与资源分析", 2)
    add_para(doc, "若抓取 N 个页面、每页平均发现 L 个链接，URL 去重集合的平均查找复杂度接近 O(1)，链接处理总体约为 O(NL)。网页正文和原始响应会暂时占用内存，max_bytes 参数用于限制单页响应上限。线程数增加后，等待网络的时间可能下降，但过大的线程数会提高站点压力和本机资源消耗，因此实验使用 2 或 4 个线程。")
    add_page_break(doc)

    # 第6页
    add_heading(doc, "6 交互式使用流程", 1)
    add_para(doc, "为降低实验操作难度，interactive_crawler.py 将参数固定为一组适合课堂实验的默认值，用户只输入网页地址即可开始。程序同时支持省略协议头的写法，会自动补充 https://。")
    add_heading(doc, "6.1 操作步骤", 2)
    add_table(doc, ["步骤", "用户操作", "程序行为"], [
        ("1", "运行 python interactive_crawler.py", "显示实验说明和默认配置"),
        ("2", "输入一个网页地址", "补全协议并进行 URL 规范化"),
        ("3", "等待抓取完成", "并发请求、解析网页、发现链接并去重"),
        ("4", "查看终端摘要", "显示成功、失败、字节数和耗时"),
        ("5", "打开结果文件", "查看 JSONL 记录和原始 HTML"),
    ], widths=[2.0, 6.0, 6.0])
    add_heading(doc, "6.2 默认参数", 2)
    add_table(doc, ["参数", "默认值", "选择理由"], [
        ("最大页数", "20", "足以观察链接扩展，又能控制实验时间"),
        ("最大深度", "1", "覆盖种子页和第一层页面"),
        ("并发线程", "4", "提高网络等待阶段的利用率"),
        ("超时", "20 秒", "兼顾慢网站与实验效率"),
        ("请求间隔", "0.2 秒", "降低对单个域名的请求压力"),
        ("域名范围", "仅种子域名", "避免无边界扩散"),
        ("robots.txt", "遵守", "符合公开网站抓取规范"),
    ], widths=[4.0, 3.0, 7.0])
    add_heading(doc, "6.3 一次运行示例", 2)
    add_code(doc, "PS> python .\\interactive_crawler.py\n请输入要抓取的网页地址：example.com/\n正在抓取：https://example.com/\n\n--- 抓取结果 ---\n抓取页数: 1\n成功网页: 1\n失败或跳过: 0\n总字节数: 577")
    add_para(doc, "实验中如果页面无法访问，程序会继续写出结果文件，并在终端打印类似“失败：URL -> HTTP 403”或“blocked by robots.txt”的信息。错误记录本身也是性能分析和网络蜘蛛定制的重要依据。")
    add_heading(doc, "6.4 JSONL 字段解释", 2)
    add_table(doc, ["字段", "含义", "实验用途"], [
        ("url / final_url", "请求前 URL 和重定向后的 URL", "分析重定向与规范化效果"),
        ("depth", "从种子页面开始的链接层级", "验证深度限制"),
        ("status / content_type", "HTTP 状态和内容类型", "区分成功、错误和非 HTML"),
        ("title / description", "页面标题和 meta 摘要", "建立页面摘要"),
        ("text / links", "可见正文和站内链接", "用于索引和下一轮扩展"),
        ("bytes / elapsed_ms", "响应大小和请求耗时", "计算性能指标"),
        ("error / raw_file", "错误原因和原始文件路径", "复查异常页面"),
    ], widths=[3.5, 5.5, 5.0])
    add_heading(doc, "6.5 失败处理示例", 2)
    add_code(doc, "HTTP 403 -> status=403, error=\"HTTP 403: Forbidden\"\nrobots 禁止 -> status=null, error=\"blocked by robots.txt\"\n超时 -> status=null, error=\"timed out\"\n非 HTML -> status=200, content_type=\"application/pdf\", error=\"unsupported content type\"")
    add_page_break(doc)

    # 第7页
    add_heading(doc, "7 实验设计与数据采集", 1)
    add_para(doc, "为了比较不同网站的抓取表现，本实验选择三类结构和规模不同的公开网站作为样本：example.com 代表极简静态页面，iana.org 代表信息组织清晰的公共服务网站，W3C 代表链接较多、页面体积较大的技术标准网站。三组实验均使用最大深度 1、最大页数 5、2 个并发线程、15 秒超时和 0.2 秒域名间隔。")
    add_heading(doc, "7.1 实验控制变量", 2)
    add_table(doc, ["控制项", "设置"], [
        ("抓取程序", "同一份 crawler.py"),
        ("页面上限", "5 页"),
        ("深度上限", "1"),
        ("线程数", "2"),
        ("请求超时", "15 秒"),
        ("域名间隔", "0.2 秒"),
        ("robots 规则", "默认遵守"),
        ("输出", "JSONL + 原始 HTML"),
    ], widths=[5.0, 9.0])
    add_heading(doc, "7.2 实验命令", 2)
    add_code(doc, "python crawler.py https://example.com/ --max-pages 5 --max-depth 1 --workers 2 --timeout 15 --delay 0.2\npython crawler.py https://www.iana.org/domains/example --max-pages 5 --max-depth 1 --workers 2 --timeout 15 --delay 0.2\npython crawler.py https://www.w3.org/ --max-pages 5 --max-depth 1 --workers 2 --timeout 15 --delay 0.2")
    add_heading(doc, "7.3 数据字段", 2)
    add_para(doc, "本实验重点统计 fetched、successful_html、failed_or_skipped、bytes、elapsed_seconds 和 pages_per_second。页面级记录还提供 title、text、links_count、elapsed_ms 和 error，便于进一步分析页面结构和失败原因。")
    add_heading(doc, "7.4 真实性说明", 2)
    add_para(doc, "表格中的数值来自本项目在报告生成前对三个公开站点的真实 HTTP/HTTPS 抓取。W3C 的 Member 和 TR 页面因为 robots.txt 明确禁止当前爬虫访问而被记录为失败或跳过，这一结果保留在原始 JSONL 中，体现了真实抓取中的限制。")
    add_heading(doc, "7.5 指标计算方法", 2)
    add_table(doc, ["指标", "计算方法", "说明"], [
        ("成功率", "成功网页数 / 抓取页数", "反映可访问页面比例"),
        ("平均页面大小", "成功页面总字节数 / 成功网页数", "衡量单页传输规模"),
        ("抓取速度", "抓取页数 / 总耗时秒数", "用于不同网站间对比"),
        ("平均耗时", "页面 elapsed_ms 的算术平均", "观察单页请求差异"),
        ("链接扩展量", "每页 links 数量的最大值或平均值", "反映候选队列增长"),
    ], widths=[4.0, 5.5, 4.5])
    add_heading(doc, "7.6 数据质量检查", 2)
    add_para(doc, "采集完成后检查三点：第一，成功记录应同时存在状态码、标题或正文，并且 bytes 大于 0；第二，失败记录必须包含 error，便于区分 robots、HTTP 和网络异常；第三，raw_file 指向的文件应存在且大小与响应 bytes 基本一致。上述检查可以避免将空记录或旧文件误当成实验结果。")
    add_page_break(doc)

    # 第8页
    add_heading(doc, "8 抓取性能分析结果", 1)
    summaries = [
        ("example.com", len(example), sum(bool(r["status"]) and not r["error"] for r in example), sum(r["bytes"] for r in example), 1.275, 0.78, "无"),
        ("iana.org", len(iana), sum(bool(r["status"]) and not r["error"] for r in iana), sum(r["bytes"] for r in iana), 2.003, 2.50, "无"),
        ("W3C", len(w3), sum(bool(r["status"]) and not r["error"] for r in w3), sum(r["bytes"] for r in w3), 0.984, 5.08, "2 页被 robots.txt 拦截"),
    ]
    add_para(doc, "表 8-1 汇总了三组实验的运行结果。抓取页数包含成功页面和被跳过页面，成功网页只统计状态码存在、没有错误且正文非空的 HTML 页面。")
    add_table(doc, ["网站样本", "抓取页数", "成功网页", "总字节数", "耗时(秒)", "速度(页/秒)", "失败情况"], summaries, widths=[3.1, 1.6, 1.7, 2.3, 1.6, 1.8, 2.9])
    add_heading(doc, "8.1 页面级结果", 2)
    page_rows = []
    for site, records in (("example.com", example), ("iana.org", iana), ("W3C", w3)):
        for record in records[:5]:
            page_rows.append((site, record["depth"], record["status"] or "-", record["bytes"], record["elapsed_ms"], record["title"][:42], record["error"] or "成功"))
    add_table(doc, ["网站", "深度", "状态", "字节", "耗时(ms)", "标题", "结果"], page_rows, widths=[2.1, 1.0, 1.1, 1.5, 1.7, 4.4, 2.2])
    add_para(doc, "从页面级数据可以看到，example.com 只有一个极简页面，无法继续扩展链接；iana.org 的种子页发现了多条站内链接，因此在深度 1 抓取到 5 个页面；W3C 的页面链接数量最多，但其中两条链接受到 robots.txt 规则限制。", space_after=0)
    add_heading(doc, "8.2 计算示例", 2)
    add_para(doc, "以 iana.org 为例，抓取页数为 5，成功网页为 5，总字节数为 36,792，运行耗时约 2.003 秒。因此成功率为 5 / 5 = 100%，平均页面大小约为 36,792 / 5 = 7,358 字节，抓取速度为 5 / 2.003 = 2.50 页/秒。以 W3C 为例，5 个候选页面中 3 个成功、2 个被 robots.txt 拦截，成功率为 3 / 5 = 60%。")
    add_heading(doc, "8.3 页面标题与正文规模", 2)
    add_table(doc, ["网站", "代表标题", "正文字符数范围", "链接数量范围"], [
        ("example.com", "Example Domain", "156", "0"),
        ("iana.org", "Example Domains / Archive", "1,247 - 2,943", "19 - 29"),
        ("W3C", "W3C / Accessibility Fundamentals Overview", "4,983 - 18,397", "19 - 79"),
    ], widths=[3.0, 5.0, 3.0, 3.0])
    add_page_break(doc)

    # 第9页
    add_heading(doc, "9 性能差异讨论", 1)
    add_heading(doc, "9.1 页面规模与吞吐量", 2)
    add_para(doc, "example.com 页面只有 577 字节，抓取过程很快，但由于没有可继续跟踪的站内链接，最终只完成 1 页。iana.org 抓取了 5 页、总计 36,792 字节，平均页面大小约 7.2 KB，速度为 2.50 页/秒。W3C 成功抓取的页面体积明显更大，成功页面包含约 131 KB 数据，仍然达到 5.08 页/秒，说明本次运行的总耗时还受到 robots 判断、网络响应和线程调度影响。")
    add_heading(doc, "9.2 成功率与访问策略", 2)
    add_table(doc, ["指标", "example.com", "iana.org", "W3C"], [
        ("成功率", "100%", "100%", "60%"),
        ("平均成功页面大小", "0.6 KB", "7.2 KB", "43.7 KB"),
        ("发现链接最多的页面", "0", "29", "79"),
        ("robots 拦截", "0", "0", "2 页"),
    ], widths=[5.0, 3.0, 3.0, 3.0])
    add_heading(doc, "9.3 结果解释", 2)
    add_bullet(doc, "页面越大并不一定导致页/秒越低，网络连接复用和并发等待可能抵消单页传输成本。")
    add_bullet(doc, "链接数量影响下一轮候选队列规模，但最大页数和最大深度会限制实际抓取范围。")
    add_bullet(doc, "robots.txt 会让访问结果更符合网站声明，但也会降低可抓取页面比例。")
    add_bullet(doc, "单次实验受网络波动影响，性能结论应通过重复运行和中位数统计进一步确认。")
    add_heading(doc, "9.4 实验局限", 2)
    add_para(doc, "本次样本数量较小，且每个网站只运行一次固定参数实验。由于网络延迟、DNS、站点负载和缓存状态随时间变化，表中速度用于展示分析方法，不代表网站的长期性能。后续可以将每个站点重复抓取 5 次，记录中位耗时、P95 耗时和每页平均字节数，使结论更稳健。")
    add_heading(doc, "9.5 参数变化的预期影响", 2)
    add_table(doc, ["参数变化", "可能的正面影响", "可能的负面影响"], [
        ("增加 max_pages", "得到更充分的样本", "耗时、存储和请求数量增加"),
        ("增加 max_depth", "覆盖更多层级页面", "队列快速扩张，容易超出实验范围"),
        ("增加 workers", "减少并发等待时间", "增加站点压力，可能触发限流"),
        ("增加 delay", "更礼貌、更稳定地访问", "单位时间抓取页数下降"),
        ("关闭 robots", "可能得到更多页面", "不符合站点公开访问声明"),
    ], widths=[3.5, 5.3, 5.2])
    add_para(doc, "因此性能实验不能只追求更高的页/秒。合理实验应同时记录成功率、失败原因、页面质量和访问约束，在可复现与对目标站点影响较小的前提下比较参数。")
    add_page_break(doc)

    # 第10页
    add_heading(doc, "10 网络蜘蛛定制", 1)
    add_para(doc, "本实验在基本爬虫上进行了多项定制，使程序从一次性下载脚本变成可用于实验记录的网络蜘蛛。定制内容覆盖抓取范围、访问规范、数据保存、错误记录和交互方式。")
    add_heading(doc, "10.1 只抓取指定域名", 2)
    add_para(doc, "WebCrawler 初始化时读取所有种子主机名，_is_allowed 只允许主机名出现在 allowed_hosts 集合中的链接进入队列。这样可以防止页面中的外部广告、社交网站或统计链接把抓取范围扩散到无关站点。命令行提供 --allow-external 开关，实验默认关闭。")
    add_heading(doc, "10.2 请求限速与最大响应大小", 2)
    add_para(doc, "_wait_for_host 为每个域名记录上一次请求时间，在同一域名的连续请求之间保留 delay 间隔。响应读取时最多读取 max_bytes + 1 字节，超过限制立即记录错误。两项机制分别控制访问频率和内存风险。")
    add_heading(doc, "10.3 原始 HTML 与结构化结果双保存", 2)
    add_para(doc, "结构化 JSONL 保存适合统计和索引的字段，原始 HTML 保存适合复查的网页证据。原始文件名使用 URL 的 SHA-256 前 16 位，避免 URL 中的特殊字符破坏文件名，同时保持同一地址重复运行时可定位到同一个文件。")
    add_heading(doc, "10.4 失败记录与可追踪性", 2)
    add_para(doc, "程序不会因为单个 URL 失败而终止。HTTPError 会保存状态码，robots 规则会记录 blocked by robots.txt，超时和连接异常会保存异常信息。每条记录还保留抓取时间和耗时，便于定位问题与复现实验。")
    add_heading(doc, "10.5 交互入口简化", 2)
    add_code(doc, "raw = input(\"请输入要抓取的网页地址：\").strip()\ncandidate = raw if \"://\" in raw else f\"https://{raw}\"\nurl = normalize_url(candidate)\nconfig = CrawlConfig(seeds=[url], max_pages=20, max_depth=1, workers=4, ...)\nrecords = WebCrawler(config).crawl()")
    add_para(doc, "该入口把参数选择交给代码中的默认配置，适合课堂演示和快速验证；如果需要性能实验，再使用 crawler.py 的命令行参数进行可控对比。")
    add_heading(doc, "10.6 定制点与实验价值", 2)
    add_table(doc, ["定制点", "关键代码", "产生的实验价值"], [
        ("URL 清洗", "normalize_url", "减少重复页面和跟踪参数干扰"),
        ("同域限制", "_is_allowed", "控制抓取边界，便于统计单站点性能"),
        ("robots 检查", "_robots_for、_can_fetch", "记录网站访问政策带来的真实限制"),
        ("并发调度", "crawl、ThreadPoolExecutor", "支持页数、深度和速度实验"),
        ("内容提取", "HTMLDocumentParser", "把网页转为可检索正文"),
        ("原始保存", "_save_raw", "保留可复查的网页证据"),
        ("交互简化", "interactive_crawler.py", "降低手动运行门槛"),
    ], widths=[3.2, 5.0, 5.8])
    add_page_break(doc)

    # 第11页
    add_heading(doc, "11 问题、改进与结论", 1)
    add_heading(doc, "11.1 实验中遇到的问题", 2)
    add_table(doc, ["问题", "原因分析", "解决方法"], [
        ("HTTPS 证书校验失败", "部分运行环境的系统证书链不完整", "优先使用 certifi CA 包，保留证书校验"),
        ("用户输入后无法直接开始", "旧交互入口要求逐项输入多个参数", "改为只输入 URL，使用稳定默认配置"),
        ("中文终端显示乱码", "Windows 标准输出编码不一致", "交互入口启动时切换 UTF-8 输出"),
        ("网页重复抓取", "多个页面可能同时发现同一个链接", "使用 scheduled 和 seen 集合双重去重"),
        ("站点拒绝部分页面", "robots.txt 明确禁止访问", "保留拦截记录并在报告中分析"),
    ], widths=[4.0, 5.0, 5.0])
    add_heading(doc, "11.2 后续改进方向", 2)
    add_bullet(doc, "增加指数退避和有限重试，区分临时网络故障与永久 HTTP 错误。")
    add_bullet(doc, "增加页面内容哈希，识别不同 URL 返回的重复页面。")
    add_bullet(doc, "增加 robots.txt 缓存和 Sitemap 读取，减少重复的规则请求。")
    add_bullet(doc, "将 JSONL 结果接入 Lucene 或数据库，开展全文检索和索引实验。")
    add_bullet(doc, "重复多轮实验，计算中位耗时、P95 延迟、失败率和单位字节耗时。")
    add_heading(doc, "11.3 实验结论", 2)
    add_para(doc, "本实验完成了真实网页爬虫的设计、运行和定制。程序能够从种子网址开始访问网页，解析出标题、正文和链接，按深度扩展站内 URL，并保存结构化结果和原始 HTML。三类网站的实测结果说明，网页规模、链接数量、网络响应和 robots 规则都会影响最终抓取数量与性能。")
    add_para(doc, "实验达成了图片中要求的第一项内容：完成网络蜘蛛原理学习、程序运行测试、抓取性能分析和关键功能定制。抓取结果文件和源代码可以作为后续网页清洗、索引构建和第二项实验的基础。")
    add_heading(doc, "11.4 可复现实验步骤", 2)
    add_table(doc, ["顺序", "操作", "检查结果"], [
        ("1", "进入项目目录并确认三个 Python 文件存在", "文件路径正确"),
        ("2", "运行 crawler.py 的三条实验命令", "得到三个 JSONL 文件"),
        ("3", "读取每个 JSONL 的 status、bytes 和 error", "确认成功与失败记录"),
        ("4", "按公式计算成功率、平均大小和页/秒", "与第 8 章表格一致"),
        ("5", "打开 data/report-raw-* 目录复查 HTML", "原始内容可回看"),
        ("6", "运行 interactive_crawler.py 输入一个地址", "验证交互入口可用"),
    ], widths=[2.0, 7.0, 5.0])
    add_heading(doc, "11.5 结论边界", 2)
    add_para(doc, "本报告结论只针对本次运行的网络环境、时间、参数和公开样本。不能根据一次运行结果断言某个网站长期更快，也不能把 robots.txt 拦截解释为网站故障。报告中把可观测事实、计算结果和改进建议分开描述，后续实验应继续扩大样本并增加重复次数。")
    add_page_break(doc)

    # 第12页
    add_heading(doc, "附录 A 关键代码与运行记录", 1)
    add_heading(doc, "A.1 URL 规范化代码", 2)
    add_code(doc, "def normalize_url(raw_url: str, base_url: str | None = None) -> str | None:\n    url = urllib.parse.urljoin(base_url or \"\", raw_url.strip())\n    parts = urllib.parse.urlsplit(url)\n    if parts.scheme.lower() not in {\"http\", \"https\"} or not parts.netloc:\n        return None\n    path = parts.path or \"/\"\n    query = urllib.parse.parse_qsl(parts.query, keep_blank_values=True)\n    query = [(key, value) for key, value in query if key.lower() not in TRACKING_QUERY_KEYS]\n    return urllib.parse.urlunsplit((parts.scheme.lower(), parts.netloc, path, urlencode(query), \"\"))")
    add_heading(doc, "A.2 HTML 文本提取代码", 2)
    add_code(doc, "class HTMLDocumentParser(HTMLParser):\n    SKIP_TAGS = {\"script\", \"style\", \"noscript\", \"template\", \"svg\"}\n\n    def handle_data(self, data: str) -> None:\n        if self._in_title:\n            self.title_parts.append(data)\n            return\n        if not self._skip_depth:\n            text = re.sub(r\"\\s+\", \" \", data).strip()\n            if text:\n                self.text_parts.append(text)")
    add_heading(doc, "A.3 实验输出示例", 2)
    add_code(doc, "{\n  \"url\": \"https://example.com/\",\n  \"final_url\": \"https://example.com/\",\n  \"depth\": 0,\n  \"status\": 200,\n  \"content_type\": \"text/html\",\n  \"title\": \"Example Domain\",\n  \"bytes\": 577,\n  \"elapsed_ms\": 1248.57,\n  \"error\": \"\"\n}")
    add_heading(doc, "A.4 文件清单", 2)
    add_table(doc, ["文件", "用途"], [
        ("crawler.py", "爬虫核心和命令行程序"),
        ("interactive_crawler.py", "只输入网页地址的交互入口"),
        ("data/report-example.jsonl", "example.com 实测结果"),
        ("data/report-iana.jsonl", "iana.org 实测结果"),
        ("data/report-w3.jsonl", "W3C 实测结果"),
        ("data/crawl/raw/", "保存网页原始 HTML"),
    ], widths=[6.0, 8.0])
    add_heading(doc, "A.5 运行核验记录", 2)
    add_table(doc, ["核验项目", "结果"], [
        ("真实网页访问", "example.com、iana.org、W3C 均完成真实 HTTP/HTTPS 请求"),
        ("HTML 内容解析", "成功提取标题、正文、状态码和链接"),
        ("性能数据保存", "JSONL 文件包含页面大小、耗时、深度和错误字段"),
        ("robots 规则处理", "W3C 两个页面被明确记录为 blocked by robots.txt"),
        ("报告页数", "Word 渲染后共 23 页"),
    ], widths=[5.0, 9.0])
    add_heading(doc, "A.6 参考资料", 2)
    add_para(doc, "[1] Python Software Foundation. urllib.request、urllib.parse、html.parser 和 concurrent.futures 模块文档。")
    add_para(doc, "[2] IETF. Robots Exclusion Protocol，RFC 9309，2022。")
    add_para(doc, "[3] W3C. HTML Living Standard，网页文档结构与超链接相关规范。")
    add_para(doc, "报告完成时间：2026 年 11 月 15 日。个人信息栏请在提交前补充姓名、学号、专业和班级。", first_line=False, align=WD_ALIGN_PARAGRAPH.CENTER, space_after=0)

    doc.core_properties.title = "网络爬虫实验报告"
    doc.core_properties.subject = "真实网页抓取与网络蜘蛛定制"
    doc.core_properties.author = ""
    doc.save(OUT)
    print(OUT)


if __name__ == "__main__":
    main()
