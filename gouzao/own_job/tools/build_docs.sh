#!/usr/bin/env bash
# 一键生成三份交付文档（md → HTML → DOCX → 插图 → 页面设置 → 列宽）
# 依赖：python + python-docx + pillow + editor_sdk(html-to-doc)
set -euo pipefail
cd "$(dirname "$0")/.."

CONVERT="C:/Users/33695/.workbuddy/plugins/cache/workbuddy-builtin/workbuddy-docx/5.9.3-wb.42398134.gd4692421.h480e8d1fee0f/skills/html-to-doc/scripts/convert_doc.py"

build() {
  local md="$1"; shift
  local name="$1"; shift
  echo "=== $name"
  python tools/md2html.py "docs/$md" "docs/$name.html" "$@"
  python "$CONVERT" --html "docs/$name.html" --output "docs/$name.docx" > /tmp/convert.json 2>&1 || true
  python tools/insert_figures.py "docs/$name.docx" docs
  python tools/fix_page_setup.py "docs/$name.docx"
  python tools/tune_tables.py "docs/$name.docx"
}

build 课程设计报告.md 课程设计报告 \
  "课程名称：软件构造" \
  "项目名称：AI 辅助课堂同伴互评工具（Java 桌面版）" \
  "技术栈：Java 17 / Swing + FlatLaf / JDBC / MySQL 8（内置 H2）" \
  "构建与测试：Maven 3.9 / JUnit 5 / JaCoCo" \
  "交付日期：2026 年 11 月"

build JUnit单元测试说明.md JUnit单元测试说明 \
  "课程名称：软件构造" \
  "项目名称：AI 辅助课堂同伴互评工具（Java 桌面版）" \
  "测试框架：JUnit 5.10.2 + JaCoCo 0.8.12" \
  "用例总数：50（7 个测试类）" \
  "交付日期：2026 年 11 月"

build 用户操作手册.md 用户操作手册 \
  "课程名称：软件构造" \
  "项目名称：AI 辅助课堂同伴互评工具（Java 桌面版）" \
  "适用角色：教师 / 学生 / 管理员" \
  "对应版本：v1.0.0" \
  "编写日期：2026 年 11 月"

echo "全部完成"
