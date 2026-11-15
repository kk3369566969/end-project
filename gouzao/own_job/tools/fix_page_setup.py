# -*- coding: utf-8 -*-
"""统一 docx 的页面设置：全部节改为 A4（21×29.7cm），页边距与 HTML 的 @page 对齐。

editor_sdk 导入 HTML 时建出的「正文节」缺少 w:pgSz，Word 会按默认 Letter 渲染，
导致同一文档里封面是 A4、正文是 Letter。此脚本补齐 pgSz 并统一页边距。

用法：python tools/fix_page_setup.py <docx> [<docx> ...]
"""
import sys

from docx import Document
from docx.shared import Cm
from docx.oxml.ns import qn

A4_W = 11906   # twips
A4_H = 16838


def fix(path):
    doc = Document(path)
    changed = 0
    for section in doc.sections:
        sect = section._sectPr
        pg_sz = sect.find(qn("w:pgSz"))
        if pg_sz is None:
            pg_sz = sect.makeelement(qn("w:pgSz"), {})
            mar = sect.find(qn("w:pgMar"))
            if mar is not None:
                mar.addprevious(pg_sz)
            else:
                sect.append(pg_sz)
        pg_sz.set(qn("w:w"), str(A4_W))
        pg_sz.set(qn("w:h"), str(A4_H))
        pg_sz.set(qn("w:orient"), "portrait")
        section.top_margin = Cm(2.6)
        section.bottom_margin = Cm(2.4)
        section.left_margin = Cm(2.6)
        section.right_margin = Cm(2.4)
        changed += 1
    doc.save(path)
    print(f"fixed sections={changed} -> {path}")


if __name__ == "__main__":
    for arg in sys.argv[1:]:
        fix(arg)
