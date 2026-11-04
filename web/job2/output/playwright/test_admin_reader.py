import json
import os
from pathlib import Path

from playwright.sync_api import sync_playwright, expect


BASE_URL = "http://localhost:5174"
OUT_DIR = Path(__file__).parent
CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"


def api_json(request, path, headers=None):
    response = request.get(f"{BASE_URL}{path}", headers=headers or {})
    assert response.ok, f"GET {path} failed: {response.status} {response.text()}"
    return response.json()


def login(page, email, password):
    page.goto(BASE_URL)
    page.wait_for_load_state("networkidle")
    page.get_by_label("邮箱", exact=True).fill(email)
    page.get_by_label("密码", exact=True).fill(password)
    page.get_by_role("button", name="登录纸灯", exact=True).click()
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(800)


with sync_playwright() as playwright:
    browser = playwright.chromium.launch(headless=True, executable_path=CHROME)
    results = {"reader": {}, "admin": {}, "database": {}, "console_errors": []}

    reader_context = browser.new_context(viewport={"width": 1280, "height": 720})
    reader_page = reader_context.new_page()
    console_errors = []
    reader_page.on("console", lambda message: console_errors.append(message.text) if message.type == "error" else None)
    login(reader_page, "reader@paperlantern.book", "123456")

    reader_body = reader_page.locator("body").inner_text()
    assert "逛书架" in reader_body
    assert "书友论坛" in reader_body
    assert "我的纸灯" in reader_body
    assert reader_page.get_by_role("button", name="后台管理", exact=True).count() == 0
    results["reader"]["role_navigation"] = "passed"

    health = api_json(reader_context.request, "/api/health")
    books = api_json(reader_context.request, "/api/books")
    assert health["mode"] == "mysql", health
    assert len(books) >= 8
    assert any(book["title"] in reader_body for book in books)
    results["database"] = {"health": health, "book_count": len(books), "sample_title": books[0]["title"]}

    reader_page.screenshot(path=str(OUT_DIR / "reader-home.png"), full_page=True)
    reader_page.get_by_role("button", name="查看详情", exact=True).click()
    expect(reader_page.get_by_text("ISBN", exact=True)).to_be_visible()
    expect(reader_page.get_by_text("出版社", exact=True)).to_be_visible()
    reader_page.get_by_role("button", name="目录", exact=True).click()
    expect(reader_page.get_by_text("起飞", exact=True)).to_be_visible()
    reader_page.get_by_role("button", name="内容试读", exact=True).click()
    expect(reader_page.get_by_text("试读内容来自出版社授权节选", exact=True)).to_be_visible()
    expect(reader_page.get_by_role("button", name="购买后评价", exact=True)).to_be_visible()
    reader_page.get_by_role("button", name="加入想读", exact=True).click()
    reader_page.screenshot(path=str(OUT_DIR / "reader-book-detail.png"), full_page=True)
    reader_page.get_by_role("button", name="关闭详情", exact=True).click()
    results["reader"]["book_detail_enhancements"] = "passed"

    reader_page.get_by_role("button", name="书友论坛", exact=True).click()
    reader_page.wait_for_load_state("networkidle")
    expect(reader_page.get_by_text("有没有读完之后，真的改变生活的书？", exact=True)).to_be_visible()
    expect(reader_page.get_by_text("# 年度书单", exact=True).last).to_be_visible()
    reader_page.get_by_role("button", name="关注", exact=True).first.click()
    reader_page.once("dialog", lambda dialog: dialog.dismiss())
    reader_page.get_by_role("button", name="举报", exact=True).first.click()
    reader_page.get_by_role("button", name="林间有风", exact=True).click()
    expect(reader_page.get_by_text("书友主页", exact=True)).to_be_visible()
    reader_page.get_by_role("button", name="关闭", exact=True).last.click()
    results["reader"]["forum"] = "passed"
    reader_page.screenshot(path=str(OUT_DIR / "reader-forum.png"), full_page=True)

    reader_page.get_by_role("button", name="我的纸灯", exact=True).click()
    reader_page.wait_for_load_state("networkidle")
    expect(reader_page.get_by_text("收藏夹", exact=True)).to_be_visible()
    expect(reader_page.get_by_text("收货地址", exact=True)).to_be_visible()
    reader_page.get_by_role("button", name="阅读书架", exact=True).click()
    expect(reader_page.get_by_text("我的阅读书架", exact=True)).to_be_visible()
    expect(reader_page.get_by_text("夜航西飞", exact=True).first).to_be_visible()
    expect(reader_page.get_by_text("最近浏览", exact=True)).to_be_visible()
    reader_page.screenshot(path=str(OUT_DIR / "reader-reading-shelf.png"), full_page=True)
    results["reader"]["reading_shelf"] = "passed"
    results["reader"]["profile"] = "passed"
    reader_page.screenshot(path=str(OUT_DIR / "reader-profile.png"), full_page=True)

    reader_token = reader_page.evaluate("localStorage.getItem('paper-lantern-token')")
    profile_api = api_json(
        reader_context.request,
        "/api/profile",
        headers={"Authorization": f"Bearer {reader_token}"},
    )
    forum_api = api_json(reader_context.request, "/api/forum/posts")
    assert profile_api["email"] == "reader@paperlantern.book"
    assert "addresses" in profile_api and "orders" in profile_api
    assert len(forum_api) >= 3
    assert any(post["title"] == "有没有读完之后，真的改变生活的书？" for post in forum_api)
    results["database"]["profile_email"] = profile_api["email"]
    results["database"]["profile_address_count"] = len(profile_api["addresses"])
    results["database"]["forum_post_count"] = len(forum_api)
    unauthorized = reader_context.request.get(f"{BASE_URL}/api/admin/overview", headers={"Authorization": f"Bearer {reader_token}"})
    assert unauthorized.status == 403
    results["reader"]["admin_api_forbidden"] = "passed"
    reader_context.close()

    admin_context = browser.new_context(viewport={"width": 1280, "height": 720})
    admin_page = admin_context.new_page()
    admin_page.on("console", lambda message: console_errors.append(message.text) if message.type == "error" else None)
    login(admin_page, "admin@paperlantern.book", "admin123")

    admin_body = admin_page.locator("body").inner_text()
    assert "管理员工作台" in admin_body
    assert "后台管理" not in admin_body
    assert "逛书架" not in admin_body
    assert "数据总览" in admin_body
    assert "图书管理" in admin_body
    assert "用户管理" in admin_body
    results["admin"]["role_navigation"] = "passed"

    overview = api_json(admin_context.request, "/api/admin/overview", headers={"Authorization": f"Bearer {admin_page.evaluate("localStorage.getItem('paper-lantern-token')")}"})
    assert str(overview["books"]) in admin_body
    results["admin"]["overview"] = overview
    admin_page.screenshot(path=str(OUT_DIR / "admin-overview.png"), full_page=True)

    admin_page.get_by_role("button", name="图书管理", exact=True).first.click()
    admin_page.wait_for_load_state("networkidle")
    expect(admin_page.get_by_role("button", name="新增图书", exact=True)).to_be_visible()
    expect(admin_page.get_by_text("夜航西飞", exact=True)).to_be_visible()
    results["admin"]["book_list"] = "passed"

    admin_page.get_by_role("button", name="新增图书", exact=True).click()
    admin_page.get_by_label("书名", exact=True).fill("Playwright 验证书")
    admin_page.get_by_label("作者", exact=True).fill("自动化测试")
    admin_page.get_by_label("分类名称", exact=True).fill("测试分类")
    admin_page.get_by_label("售价", exact=True).fill("18")
    admin_page.get_by_label("原价", exact=True).fill("22")
    admin_page.get_by_label("库存", exact=True).fill("6")
    admin_page.get_by_label("简介", exact=True).fill("用于验证管理员图书 CRUD 的临时记录")
    admin_page.get_by_role("button", name="保存图书", exact=True).click()
    row = admin_page.locator("tr").filter(has_text="Playwright 验证书")
    expect(row).to_be_visible()
    results["admin"]["book_create"] = "passed"

    admin_page.on("dialog", lambda dialog: dialog.accept())
    row.get_by_role("button").nth(1).click()
    expect(row).not_to_be_visible()
    assert not any(item["title"] == "Playwright 验证书" for item in api_json(admin_context.request, "/api/books"))
    results["admin"]["book_delete"] = "passed"
    admin_page.screenshot(path=str(OUT_DIR / "admin-books.png"), full_page=True)

    admin_page.get_by_role("button", name="用户管理", exact=True).first.click()
    admin_page.wait_for_load_state("networkidle")
    expect(admin_page.get_by_text("reader@paperlantern.book", exact=True)).to_be_visible()
    expect(admin_page.get_by_text("admin@paperlantern.book", exact=True)).to_be_visible()
    results["admin"]["users"] = "passed"

    admin_page.get_by_role("button", name="订单管理", exact=True).first.click()
    admin_page.wait_for_load_state("networkidle")
    expect(admin_page.get_by_role("heading", name="订单管理", exact=True)).to_be_visible()
    results["admin"]["orders"] = "passed"

    admin_page.get_by_role("button", name="社区审核", exact=True).first.click()
    admin_page.wait_for_load_state("networkidle")
    expect(admin_page.get_by_text("讨论帖子", exact=True)).to_be_visible()
    expect(admin_page.get_by_text("书籍评论", exact=True)).to_be_visible()
    pin_button = admin_page.get_by_role("button", name="置顶", exact=True).first
    expect(pin_button).to_be_visible()
    pin_button.click()
    expect(admin_page.get_by_role("button", name="取消置顶", exact=True).first).to_be_visible()
    admin_page.get_by_role("button", name="取消置顶", exact=True).first.click()
    results["admin"]["community_review"] = "passed"
    admin_page.screenshot(path=str(OUT_DIR / "admin-community.png"), full_page=True)

    results["console_errors"] = [error for error in console_errors if "favicon" not in error.lower()]
    assert not results["console_errors"], results["console_errors"]
    admin_context.close()
    browser.close()
    print(json.dumps(results, ensure_ascii=False, indent=2, default=str))
