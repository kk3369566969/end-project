# -*- coding: utf-8 -*-
"""把 HTML 转换产物中的 [[FIG:相对路径::图注]] 占位段落替换为真实图片与图注。

editor_sdk 的 HTML 导入不抓取外链图片，因此图片在转 docx 之后由本脚本补入：
占位段落清空 → 插入居中图片（按页面宽度与页高双向限制）→ 其后插入居中图注。

用法：python tools/insert_figures.py <docx> <图片基准目录>
"""
import os
import re
import sys

from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor
from docx.text.paragraph import Paragraph
from PIL import Image

PAT = re.compile(r"\[\[FIG:(.+?)::(.*?)\]\]")
MAX_W = 15.8          # 版心宽度 16cm，留 0.2cm 余量
MAX_H = 20.5          # A4 正文高度约 24cm，给图注与上下文留空间


def clear_runs(para):
    for run in list(para.runs):
        run._element.getparent().remove(run._element)


def add_caption(after_para, text):
    new_p = OxmlElement("w:p")
    after_para._element.addnext(new_p)
    cap = Paragraph(new_p, after_para._parent)
    cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    cap.paragraph_format.space_before = Pt(4)
    cap.paragraph_format.space_after = Pt(10)
    run = cap.add_run(text)
    run.font.size = Pt(10)
    run.font.color.rgb = RGBColor(0x33, 0x33, 0x33)
    run.font.name = "宋体"
    rpr = run._element.get_or_add_rPr()
    rfonts = rpr.find(qn("w:rFonts"))
    if rfonts is None:
        rfonts = OxmlElement("w:rFonts")
        rpr.append(rfonts)
    rfonts.set(qn("w:eastAsia"), "宋体")


def main(docx_path, base_dir):
    doc = Document(docx_path)
    ok = miss = 0
    for para in list(doc.paragraphs):
        match = PAT.search(para.text)
        if not match:
            continue
        rel, caption = match.group(1).strip(), match.group(2).strip()
        image_path = os.path.join(base_dir, rel.replace("/", os.sep))
        if not os.path.isfile(image_path):
            print("[warn] 图片缺失：", image_path)
            miss += 1
            continue
        with Image.open(image_path) as im:
            w, h = im.size
        width_cm = MAX_W
        if width_cm * h / w > MAX_H:
            width_cm = MAX_H * w / h

        clear_runs(para)
        para.alignment = WD_ALIGN_PARAGRAPH.CENTER
        para.paragraph_format.space_before = Pt(8)
        para.paragraph_format.space_after = Pt(2)
        para.add_run().add_picture(image_path, width=Cm(width_cm))
        if caption:
            add_caption(para, caption)
        ok += 1

    doc.save(docx_path)
    print(f"inserted={ok} missing={miss} -> {docx_path}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else "docs")
