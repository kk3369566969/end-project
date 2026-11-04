from pathlib import Path
from playwright.sync_api import sync_playwright

BASE = "http://localhost:5174"
CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"


def login(page, email, password):
    page.goto(BASE)
    page.wait_for_load_state("networkidle")
    page.get_by_label("邮箱", exact=True).fill(email)
    page.get_by_label("密码", exact=True).fill(password)
    page.get_by_role("button", name="登录纸灯").click()
    page.wait_for_timeout(700)


with sync_playwright() as p:
    print("starting", flush=True)
    browser = p.chromium.launch(headless=True, executable_path=CHROME)
    reader = browser.new_page()
    console_errors = []
    reader.on("console", lambda msg: console_errors.append(msg.text) if msg.type == "error" else None)
    login(reader, "reader@paperlantern.book", "123456")
    print("reader logged", flush=True)

    reader.get_by_role("button", name="书友论坛").click()
    reader.get_by_role("heading", name="读完一本书").wait_for()
    reader.get_by_role("button", name="发起讨论").last.click()
    modal = reader.locator(".form-modal").last
    modal.get_by_role("heading", name="发起一场讨论").wait_for()
    scroll_state = modal.evaluate("node => ({ scrollHeight: node.scrollHeight, clientHeight: node.clientHeight })")
    assert scroll_state["scrollHeight"] > scroll_state["clientHeight"]
    assert modal.evaluate("node => { node.scrollTop = node.scrollHeight; return node.scrollTop; }") > 0
    modal.get_by_label("标题").fill("浏览器发布测试")
    modal.get_by_label("Markdown 正文").fill("只填写 Markdown 正文也可以发布。")
    modal.get_by_role("button", name="发布讨论").click()
    reader.get_by_text("浏览器发布测试").wait_for()

    reader.get_by_role("button", name="二手书市").click()
    reader.get_by_role("heading", name="二手书市").wait_for()
    assert reader.get_by_text("人间草木").count() >= 1
    reader.get_by_role("button", name="发布闲置书").click()
    reader.get_by_role("heading", name="发布闲置书").wait_for()
    reader.get_by_label("书名").fill("测试二手书")
    reader.get_by_label("作者").fill("测试作者")
    reader.get_by_label("价格").fill("12")
    reader.get_by_role("button", name="发布二手书").click()
    reader.wait_for_timeout(600)
    assert reader.get_by_text("测试二手书").count() >= 1

    reader.get_by_role("button", name="读书会").click()
    reader.get_by_role("heading", name="读书会活动").wait_for()
    reader.wait_for_timeout(500)
    assert reader.get_by_text("观看之道").count() >= 1

    reader.get_by_role("button", name="阅读挑战").click()
    reader.get_by_role("heading", name="阅读挑战").wait_for()
    if reader.get_by_role("button", name="加入挑战").count():
        reader.get_by_role("button", name="加入挑战").click()
    reader.get_by_role("button", name="保存进度").wait_for()

    reader.get_by_role("button", name="我的数据").click()
    reader.get_by_role("heading", name="我的数据").wait_for()
    assert reader.get_by_text("阅读偏好").count() == 1
    reader.screenshot(path=str(Path("output/playwright/new-modules-reader.png")))
    print("reader passed", flush=True)

    admin = browser.new_page()
    login(admin, "admin@paperlantern.book", "admin123")
    print("admin logged", flush=True)
    admin.get_by_role("complementary").get_by_role("button", name="经营分析").click()
    admin.get_by_role("heading", name="经营分析").wait_for()
    assert admin.get_by_text("来自 MySQL 实时聚合").count() == 1
    admin.screenshot(path=str(Path("output/playwright/new-modules-admin.png")))

    health = reader.request.get("http://localhost:3001/api/health").json()
    assert health["ok"] is True and health["mode"] == "mysql"
    print({"reader": "passed", "admin": "passed", "database": health, "console_errors": console_errors})
    browser.close()
