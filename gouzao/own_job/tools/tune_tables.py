# -*- coding: utf-8 -*-
"""按内容需要重新分配 docx 表格的列宽。

editor_sdk 导入 HTML 表格时不识别列宽，6 列的宽表会被压成「每格 2~3 字」的窄条，
一页只能放两三行。本脚本按「每列最长单元格的显示宽度（中文算 2、西文算 1）」
为列宽分配版心宽度，并把表格设为固定布局，使长行在单元格内正常折行。

用法：python tools/tune_tables.py <docx> [<docx> ...]
"""
import sys

from docx import Document
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm

CONTENT_CM = 16.0     # A4 21cm - 左右页边距 2.6/2.4
MIN_CM = 2.2
CODE_MIN_CM = 15.6


def disp_width(text):
    width = 0
    for ch in text:
        width += 2 if ord(ch) > 0x2E80 else 1
    return width


def longest_line_width(text):
    return max((disp_width(line) for line in text.split("\n")), default=0)


def set_widths(table, widths):
    table.autofit = False
    tbl_pr = table._tbl.tblPr
    tbl_w = tbl_pr.find(qn("w:tblW"))
    if tbl_w is None:
        tbl_w = OxmlElement("w:tblW")
        tbl_pr.append(tbl_w)
    tbl_w.set(qn("w:type"), "dxa")
    tbl_w.set(qn("w:w"), str(int(sum(widths) * 567)))
    for i, cm in enumerate(widths):
        if i < len(table.columns):
            table.columns[i].width = Cm(cm)
    for row in table.rows:
        for i, cell in enumerate(row.cells):
            if i < len(widths):
                cell.width = Cm(widths[i])


def tune(path):
    doc = Document(path)
    stat = {"wide": 0, "code": 0}
    for table in doc.tables:
        ncol = len(table.columns)
        nrow = len(table.rows)
        if ncol == 0:
            continue
        if ncol == 1 and nrow == 1:
            # 代码块（HTML 的 <pre> 被导入成 1×1 表格）：撑满版心，避免长行溢出
            set_widths(table, [CODE_MIN_CM])
            stat["code"] += 1
            continue
        need = [0] * ncol
        for row in table.rows:
            for i, cell in enumerate(row.cells[:ncol]):
                need[i] = max(need[i], longest_line_width(cell.text))
        total = sum(need) or 1
        widths = [max(MIN_CM, CONTENT_CM * need[i] / total) for i in range(ncol)]
        scale = CONTENT_CM / sum(widths)
        widths = [w * scale for w in widths]
        set_widths(table, widths)
        stat["wide"] += 1
    doc.save(path)
    print(f"tuned tables={stat['wide']} code_blocks={stat['code']} -> {path}")


if __name__ == "__main__":
    for arg in sys.argv[1:]:
        tune(arg)
