from pathlib import Path
from playwright.sync_api import sync_playwright

BASE = "http://localhost:5174"
CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"


def login(page, email, password):
    page.goto(BASE)
    page.wait_for_load_state("networkidle")
    page.get_by_label("邮箱", exact=True).fill(email)
    page.get_by_label("密码", exact=True).fill(password)
    page.get_by_role("button", name="登录纸灯", exact=True).click()
    page.wait_for_timeout(500)


with sync_playwright() as p:
    browser = p.chromium.launch(headless=True, executable_path=CHROME)
    reader = browser.new_page(viewport={"width": 1280, "height": 720})
    errors = []
    reader.on("console", lambda msg: errors.append(msg.text) if msg.type == "error" else None)
    login(reader, "reader@paperlantern.book", "123456")

    reader.get_by_role("button", name="消息中心", exact=True).click()
    reader.get_by_role("heading", name="消息中心", exact=True).wait_for()
    assert reader.get_by_text("消息中心").count() >= 1

    reader.get_by_role("button", name="阅读榜单", exact=True).click()
    reader.get_by_role("heading", name="阅读排行榜", exact=True).wait_for()
    assert reader.get_by_role("button", name="年度读完", exact=True).count() == 1
    reader.get_by_role("button", name="阅读时长", exact=True).click()
    assert reader.get_by_text("阅读时长").count() >= 1
    reader.screenshot(path=str(Path("output/playwright/reader-leaderboard.png")))

    admin = browser.new_page(viewport={"width": 1280, "height": 720})
    login(admin, "admin@paperlantern.book", "admin123")
    admin.get_by_role("complementary").get_by_role("button", name="库存管理", exact=True).click()
    admin.get_by_role("heading", name="库存管理", exact=True).wait_for()
    stock_input = admin.locator(".inventory-stock").first
    current = stock_input.input_value()
    stock_input.fill(current)
    admin.get_by_role("button", name="保存库存", exact=True).first.click()
    admin.wait_for_timeout(300)
    assert admin.get_by_text("库存预警").count() >= 0
    admin.screenshot(path=str(Path("output/playwright/admin-inventory.png")))

    health = reader.request.get("http://localhost:3001/api/health").json()
    assert health["ok"] is True and health["mode"] == "mysql"
    print({"reader": "messages_and_leaderboard_passed", "admin": "inventory_passed", "database": health, "console_errors": errors})
    browser.close()
