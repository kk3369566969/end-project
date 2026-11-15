# -*- coding: utf-8 -*-
"""生成课程设计文档所需的 UML / ER 示意图。

只用 Pillow：Pen 封装文字测量、类框、实体框、箭头与虚线；各图函数按固定坐标排版，
坐标用上一元素的返回值推进，避免硬编码造成重叠。
运行：python tools/umlgen.py   → 输出 docs/img/*.png
"""
import math
import os

from PIL import Image, ImageDraw, ImageFont

FONT_PATH = r"C:\Windows\Fonts\simhei.ttf"
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "docs", "img")

INK = "#1b1b1b"
MUTED = "#5a5a5a"
LINE = "#333333"
HEAD = "#eef1f5"
BAND = "#f7f8fa"
ACCENT = "#2f5d8a"

_fonts = {}


def F(size):
    if size not in _fonts:
        _fonts[size] = ImageFont.truetype(FONT_PATH, size)
    return _fonts[size]


class Pen:
    def __init__(self, w, h, bg="white"):
        self.w, self.h = w, h
        self.img = Image.new("RGB", (w, h), bg)
        self.d = ImageDraw.Draw(self.img)

    # ---------- 基础 ----------
    def tw(self, s, size):
        return self.d.textlength(s, font=F(size))

    def text(self, x, y, s, size=24, fill=INK, bg=False):
        if bg:
            w = self.tw(s, size)
            self.d.rectangle([x - 3, y - 2, x + w + 3, y + size + 5], fill="white")
        self.d.text((x, y), s, font=F(size), fill=fill)

    def center(self, cx, y, s, size=24, fill=INK, bg=False):
        w = self.tw(s, size)
        if bg:
            self.d.rectangle([cx - w / 2 - 3, y - 2, cx + w / 2 + 3, y + size + 5], fill="white")
        self.d.text((cx, y), s, font=F(size), fill=fill, anchor="ma")

    def rect(self, x, y, w, h, fill="white", outline=LINE, width=2, radius=10):
        self.d.rounded_rectangle([x, y, x + w, y + h], radius=radius,
                                 fill=fill, outline=outline, width=width)

    def entity(self, x, y, title, rows, width=None, title_size=23, row_size=19):
        """表实体：标题条 + 若干行；宽高按内容自适应，返回 (x,y,w,h)。"""
        lh = row_size + 11
        tw_max = max([self.tw(title, title_size)] + [self.tw(r, row_size) for r in rows])
        w = width or int(tw_max + 34)
        h = (title_size + 20) + len(rows) * lh + 8
        self.d.rectangle([x, y, x + w, y + h], fill="white", outline=LINE, width=2)
        self.d.rectangle([x, y, x + w, y + title_size + 18], fill=HEAD, outline=LINE, width=2)
        self.center(x + w // 2, y + 6, title, title_size)
        cy = y + title_size + 24
        for row in rows:
            self.text(x + 12, cy, row, row_size)
            cy += lh
        return (x, y, w, h)

    # ---------- 线 ----------
    def line(self, p, q, width=2, dash=None, fill=LINE):
        if not dash:
            self.d.line([p[0], p[1], q[0], q[1]], fill=fill, width=width)
            return
        dx, dy = q[0] - p[0], q[1] - p[1]
        length = math.hypot(dx, dy)
        if length == 0:
            return
        ux, uy = dx / length, dy / length
        pos = 0.0
        while pos < length:
            end = min(pos + dash, length)
            self.d.line([p[0] + ux * pos, p[1] + uy * pos,
                         p[0] + ux * end, p[1] + uy * end], fill=fill, width=width)
            pos = end + dash * 0.9

    def tri(self, tip, angle, size=15, hollow=False):
        a1, a2 = angle + math.radians(150), angle - math.radians(150)
        pts = [tip,
               (tip[0] + size * math.cos(a1), tip[1] + size * math.sin(a1)),
               (tip[0] + size * math.cos(a2), tip[1] + size * math.sin(a2))]
        self.d.polygon(pts, fill="white" if hollow else LINE, outline=LINE)

    def diamond(self, tip, angle, size=17):
        bx = tip[0] - size * math.cos(angle)
        by = tip[1] - size * math.sin(angle)
        hx = tip[0] - size * 2 * math.cos(angle)
        hy = tip[1] - size * 2 * math.sin(angle)
        ox, oy = -math.sin(angle) * size * 0.5, math.cos(angle) * size * 0.5
        self.d.polygon([tip, (bx + ox, by + oy), (hx, hy), (bx - ox, by - oy)],
                       fill="white", outline=LINE)

    def arrow(self, p, q, kind="assoc", width=2, label=None, lsize=19,
              label_at=0.5, label_dx=0, label_dy=-26):
        """kind: assoc / dep / impl / extend / plain"""
        angle = math.atan2(q[1] - p[1], q[0] - p[0])
        gap = {"assoc": 14, "dep": 14, "impl": 18, "extend": 18}.get(kind, 0)
        end = (q[0] - gap * math.cos(angle), q[1] - gap * math.sin(angle))
        if kind in ("dep", "impl"):
            self.line(p, end, width=width, dash=9)
        else:
            self.line(p, end, width=width)
        if kind == "assoc":
            self.tri(q, angle)
        elif kind == "dep":
            self.d.line([q, (q[0] - 15 * math.cos(angle - 0.42),
                             q[1] - 15 * math.sin(angle - 0.42))], fill=LINE, width=width)
            self.d.line([q, (q[0] - 15 * math.cos(angle + 0.42),
                             q[1] - 15 * math.sin(angle + 0.42))], fill=LINE, width=width)
        elif kind in ("impl", "extend"):
            self.tri(q, angle, hollow=True)
        if label:
            lx = p[0] + (q[0] - p[0]) * label_at + label_dx
            ly = p[1] + (q[1] - p[1]) * label_at + label_dy
            self.center(lx, ly, label, lsize, fill=MUTED, bg=True)

    # ---------- UML 类框 ----------
    def uml(self, x, y, w, name, stereo=None, fields=(), methods=(), size=24,
            fill="white", outline=LINE):
        pad = 11
        lh = size + 9
        sub = int(size * 0.84)
        rows = []
        for t in fields:
            rows.append(("f", t))
        for t in methods:
            rows.append(("m", t))
        head = (lh if stereo else 0) + lh + pad
        body = sum(lh for _ in rows) + (pad if fields else 0) + (pad if methods else 0)
        h = head + body
        self.d.rounded_rectangle([x, y, x + w, y + h], radius=8, fill=fill,
                                 outline=outline, width=2)
        cy = y + 5
        if stereo:
            self.center(x + w // 2, cy, stereo, sub, MUTED)
            cy += lh
        self.center(x + w // 2, cy, name, size)
        cy += lh
        self.d.line([x, cy, x + w, cy], fill=LINE, width=2)
        cy += 5
        for idx, (kind, t) in enumerate(rows):
            if kind == "m" and fields and idx == len(fields):
                cy += pad - 5
                self.d.line([x, cy, x + w, cy], fill=LINE, width=2)
                cy += 5
            self.text(x + pad, cy, t, sub)
            cy += lh
        return (x, y, w, h)

    def band(self, x, y, w, h, title, sub=None):
        self.d.rounded_rectangle([x, y, x + w, y + h], radius=12, fill=BAND,
                                 outline="#c9ced6", width=2)
        self.text(x + 16, y + 10, title, 23, fill=ACCENT)
        if sub:
            self.text(x + 16, y + 42, sub, 18, fill=MUTED)

    def save(self, name):
        os.makedirs(OUT, exist_ok=True)
        path = os.path.join(OUT, name)
        self.img.save(path)
        print("saved", path, self.img.size)


STERE = "<<interface>>"


# ============================ 图 2-1 用例图 ============================
def fig_usecase():
    p = Pen(1800, 1120)
    p.text(40, 22, "图 2-1  系统用例图：教师与学生两类参与者", 26, fill=ACCENT)

    def actor(x, y, name):
        p.d.ellipse([x, y, x + 46, y + 46], outline=LINE, width=2, fill="white")
        p.d.line([x + 23, y + 46, x + 23, y + 100], fill=LINE, width=2)
        p.d.line([x - 12, y + 70, x + 58, y + 70], fill=LINE, width=2)
        p.d.line([x + 23, y + 100, x - 4, y + 138], fill=LINE, width=2)
        p.d.line([x + 23, y + 100, x + 50, y + 138], fill=LINE, width=2)
        p.center(x + 23, y + 152, name, 23)

    def uc(cx, cy, text, w=330):
        p.d.rounded_rectangle([cx - w // 2, cy - 27, cx + w // 2, cy + 27], radius=27,
                              fill="white", outline=LINE, width=2)
        p.center(cx, cy - 11, text, 21)

    # 系统边界：包住全部用例
    p.rect(300, 90, 1200, 760, fill="#fbfcfd", outline="#c9ced6", width=2)
    p.center(900, 104, "同伴互评工具", 24)

    actor(96, 400, "教师")
    actor(1636, 400, "学生")

    left = [("登录系统", 170), ("管理课程", 250), ("布置作业与评分细则", 330),
            ("发布 / 撤回 / 结束作业", 410), ("触发互评分配", 490),
            ("查看提交、统计与明细", 570), ("正则检索与导出 CSV", 650)]
    for text, y in left:
        uc(560, y, text)
        p.arrow((164, 470), (394, y), "plain", width=1)

    right = [("查看可提交作业", 250), ("提交 / 更新作业正文", 330),
             ("查看我的互评任务（盲评）", 410), ("逐维度打分并写评语", 490),
             ("生成 AI 参考意见", 570)]
    for text, y in right:
        uc(1230, y, text, w=340)
        p.arrow((1636, 470), (1402, y), "plain", width=1)

    p.d.rounded_rectangle([690, 740, 1110, 794], radius=27, fill="white", outline=LINE, width=2)
    p.center(900, 756, "注册账号（教师需邀请码）", 21)
    p.arrow((130, 560), (250, 780), "plain", width=1)
    p.arrow((1636, 560), (1560, 780), "plain", width=1)

    p.text(60, 900, "<<include>> 触发分配 → 前置校验「作业已发布」且「提交数 ≥ 2」", 20, fill=MUTED)
    p.text(60, 940, "<<include>> 提交互评 → 前置校验「任务待办」且各维度分落在区间内", 20, fill=MUTED)
    p.text(60, 980, "<<extend>>  AI 参考意见 → 仅在已配置密钥且开关打开时出现入口", 20, fill=MUTED)
    p.text(60, 1040, "说明：用例按「参与者能独立完成的一次业务目标」划分，未把 CRUD 逐个拆成用例；", 20, fill=MUTED)
    p.text(60, 1072, "      否则图上会退化成接口清单，看不出业务边界。", 20, fill=MUTED)
    p.save("fig-2-1-usecase.png")


# ============================ 图 4-1 核心类图 ============================
def fig_core_classes():
    p = Pen(1900, 1220)
    p.text(40, 22, "图 4-1  核心类图：启动装配、服务层、数据访问层与策略接口（省略 getter/setter 与纯展示构件）",
           24, fill=ACCENT)

    m = p.uml(830, 66, 180, "Main", methods=["+ main(String[])"], size=23)
    ac = p.uml(410, 220, 400, "AppContext", "组合根",
               fields=["- config : AppConfig"],
               methods=["+ static connect() : AppContext",
                        "+ auth() / review() / ai() ..."], size=23)
    cfg = p.uml(880, 236, 270, "AppConfig", "",
                methods=["+ load(Path)", "+ dbUrl() / aiKey()"], size=22)
    p.arrow((920, m[1] + m[3]), (720, 216), "assoc", label="创建", label_at=0.5, label_dy=-24)
    p.arrow((810, 276), (876, 276), "assoc", label="读取配置", label_at=0.5, label_dy=-26)

    # 业务层
    p.band(20, 440, 1860, 195, "业务层 service：7 个服务，构造器注入，服务之间互不 new")
    names = ["AuthService", "CourseService", "AssignmentService", "SubmissionService",
             "AllocationService", "ReviewService", "StatisticsService"]
    boxes = []
    x = 34
    for n in names:
        boxes.append(p.uml(x, 508, 246, n, "", methods=["校验与规则收口"], size=21))
        x += 264
    p.arrow((700, 430), (700, 502), "assoc", label="构造器注入", label_at=0.82, label_dx=86, label_dy=2)

    # DAO 接口
    p.band(20, 690, 1860, 175, "数据访问层 dao：只暴露接口，服务层不出现 SQL")
    daos = ["UserDao", "CourseDao", "AssignmentDao", "SubmissionDao", "ReviewTaskDao", "ReviewDao"]
    x = 132
    for n in daos:
        p.uml(x, 752, 250, n, STERE, methods=["+ find / insert / update"], size=21)
        x += 272
    p.arrow((700, 640), (700, 746), "dep", label="只依赖接口", label_at=0.5, label_dy=-2)

    # 策略接口
    p.band(20, 915, 900, 250, "domain 策略接口（算法可整体替换）",
           "计分：ScoreCalculators.of(mode) 取实例   分配：构造器注入")
    p.uml(60, 1005, 330, "ScoreCalculator", STERE, methods=["+ calculate(items, scores)"], size=22)
    p.uml(470, 1005, 330, "ReviewAllocator", STERE, methods=["+ allocate(list, n)"], size=22)
    p.text(64, 1096, "实现：NormalizedWeighted", 18, fill=MUTED)
    p.text(64, 1124, "            RawWeighted", 18, fill=MUTED)
    p.text(474, 1096, "实现：RandomRingAllocator", 18, fill=MUTED)

    # AI
    p.band(960, 915, 920, 250, "ai 策略接口（全项目唯一调用点：ReviewDialog）",
           "约束：ai 包不依赖 dao / db / service，删掉即退回纯手动互评")
    p.uml(1000, 1005, 430, "ReviewSuggestionProvider", STERE,
          methods=["+ isAvailable() / suggest(req)"], size=22)
    p.uml(1480, 1005, 370, "UnavailableSuggestion", "空对象",
          methods=["未启用时占位，界面免判空"], size=21)
    p.text(1004, 1096, "实现：DeepSeekSuggestionProvider", 18, fill=MUTED)
    p.text(1004, 1124, "      （HTTP + Jackson）", 18, fill=MUTED)

    p.text(40, 1180, "注：虚线为依赖（dep），实线箭头为关联；箭头方向即依赖方向，全部单向、无环。", 20, fill=MUTED)
    p.save("fig-4-1-core-classes.png")


# ============================ 图 4-2 策略族类图 ============================
def fig_strategies():
    p = Pen(1900, 1240)
    p.text(40, 22, "图 4-2  策略族类图：计分 / 分配 / AI 参考意见（三者形状一致，可整体替换）", 25, fill=ACCENT)

    # 计分
    p.band(30, 76, 600, 470, "计分策略族", "ReviewService 通过 ScoreCalculators.of(mode) 获取")
    p.uml(180, 190, 300, "ScoreCalculator", STERE,
          methods=["+ calculate(items, scores)", "+ mode() : ScoringMode"], size=22)
    a = p.uml(56, 400, 280, "NormalizedWeighted", "",
              methods=["区间归一化后加权", "权重不足 100 自动归一"], size=21)
    b = p.uml(356, 400, 250, "RawWeighted", "",
              methods=["Σ(得分 × 权重%)", "同量纲维度直接加权"], size=21)
    p.arrow((a[0] + a[2] // 2, a[1]), (250, 356), "impl")
    p.arrow((b[0] + b[2] // 2, b[1]), (400, 356), "impl")

    # 分配
    p.band(660, 76, 560, 470, "分配策略族", "AllocationService.withDefaultAllocator(...) 注入")
    p.uml(810, 190, 260, "ReviewAllocator", STERE,
          methods=["+ allocate(list, n)", "+ name() : String"], size=22)
    c = p.uml(750, 400, 380, "RandomRingAllocator", "",
              methods=["打乱成环，顺延取 N 人", "O(size×N)，随机源可注入"], size=21)
    p.arrow((c[0] + c[2] // 2, c[1]), (940, 356), "impl")

    # AI
    p.band(30, 590, 1840, 590, "AI 参考意见策略族", "SuggestionProviders.fromConfig(config) → AppContext.ai()")
    p.uml(120, 700, 470, "ReviewSuggestionProvider", STERE,
          methods=["+ displayName() : String",
                   "+ isAvailable() : boolean",
                   "+ unavailableReason() : String",
                   "+ suggest(request) : ReviewSuggestion"], size=22)
    d = p.uml(56, 960, 430, "DeepSeekSuggestionProvider", "",
              methods=["POST /chat/completions", "超时 / IO / 中断 → AppException"], size=21)
    e = p.uml(520, 960, 300, "UnavailableSuggestion", "空对象",
              methods=["isAvailable() = false", "承担「未启用」语义"], size=21)
    p.arrow((d[0] + d[2] // 2, d[1]), (290, 926), "impl")
    p.arrow((e[0] + e[2] // 2, e[1]), (450, 926), "impl")

    f = p.uml(1000, 700, 300, "SuggestionPrompt", "",
              methods=["+ systemPrompt()", "+ userMessage(request)"], size=21)
    g = p.uml(1350, 700, 300, "SuggestionParser", "",
              methods=["+ parse(json, request)", "维度校验 / 分数裁剪"], size=21)
    p.arrow((f[0] + 40, f[1]), (420, 900), "dep", label="uses", label_at=0.55, label_dy=-16)
    p.arrow((g[0] + 40, g[1]), (450, 900), "dep", label="uses", label_at=0.5, label_dy=-16)
    h = p.uml(1000, 960, 300, "SuggestionRequest", "输入 record",
              methods=["题面、要求、细则、正文", "刻意不含作者身份"], size=21)
    i = p.uml(1350, 960, 330, "ReviewSuggestion", "输出 record",
              methods=["hints / comment", "model / elapsed / tokens"], size=21)
    p.arrow((f[0] + 90, f[1] + f[3]), (h[0] + 90, h[1]), "dep", label="入参", label_dy=-24)
    p.arrow((g[0] + 90, g[1] + g[3]), (i[0] + 90, i[1]), "dep", label="产出", label_dy=-24)
    p.text(1000, 878, "约束：ai 包内不出现任何 Dao / Connection 引用（编译期即不依赖）", 19, fill=MUTED)
    p.save("fig-4-2-strategies.png")


# ============================ 图 4-3 数据访问层类图 ============================
def fig_dao_layer():
    p = Pen(1800, 830)
    p.text(40, 22, "图 4-3  数据访问层类图：接口 — 抽象模板 — 六个 JDBC 实现", 25, fill=ACCENT)
    base = p.uml(620, 100, 470, "AbstractJdbcDao", "抽象模板",
                 fields=["# provider : ConnectionProvider"],
                 methods=["# query(sql, mapper, params)",
                          "# queryOne(...)   # insert(sql, params)",
                          "# update(...)   # executeBatch(...)",
                          "# inTransaction(work)"], size=22)
    cp = p.uml(1250, 100, 380, "ConnectionProvider", STERE,
               methods=["+ getConnection() : Connection"], size=22)
    p.arrow((1246, 190), (1096, 210), "dep", label="取得连接", label_at=0.5, label_dy=-24)
    dm = p.uml(1250, 300, 390, "DriverManagerProvider", "",
               methods=["+ diagnose(SQLException)", "五类错误分诊为中文提示"], size=21)
    p.arrow((dm[0] + dm[2] // 2, dm[1]), (1440, 250), "impl")

    impls = ["JdbcUserDao", "JdbcCourseDao", "JdbcAssignmentDao",
             "JdbcSubmissionDao", "JdbcReviewTaskDao", "JdbcReviewDao"]
    xs = []
    x = 40
    for k, n in enumerate(impls):
        box = p.uml(x, 540, 250, n, "", methods=["SQL + RowMapper", "不再写 try-with-resources"], size=20)
        xs.append(box[0] + box[2] // 2)
        x += 290

    # 继承：六个子类汇到一条水平母线，母线再连父类，避免箭头在同一处堆叠
    bus_y = 430
    p.line((xs[0], bus_y), (xs[-1], bus_y), width=2)
    for cx in xs:
        p.arrow((cx, bus_y), (cx, 534), "plain", width=2)
    p.line((xs[3], bus_y), (xs[3], base[1] + base[3] + 18), width=2)
    p.tri((xs[3], base[1] + base[3]), -math.pi / 2, size=17, hollow=True)
    p.text(30, 392, "继承（空心三角指向父类）", 20, fill=MUTED)

    p.text(40, 700, "模板方法把「取连接 → 绑定参数 → 映射结果 → 关闭资源 → 包装异常」固定下来；", 20, fill=MUTED)
    p.text(40, 740, "子类无法遗漏资源关闭，也无法自行用另一种方式包装异常——异常体系因此保持单向。", 20, fill=MUTED)
    p.save("fig-4-3-dao-layer.png")


# ============================ 图 4-4 顺序图 ============================
def fig_sequence():
    p = Pen(1790, 1420)
    p.text(40, 22, "图 4-4  顺序图：学生提交一次互评（第 5~7 步为 AI 旁路）", 25, fill=ACCENT)
    lanes = [("学生", 130), ("ReviewDialog", 400), ("ReviewService", 720),
             ("ScoreCalculator", 990), ("ReviewDao", 1250), ("AiAssist", 1450)]
    xmap = dict(lanes)
    top = 90
    for name, x in lanes:
        w = 190 if len(name) > 8 else 150
        p.d.rectangle([x - w // 2, top, x + w // 2, top + 50], fill=HEAD, outline=LINE, width=2)
        p.center(x, top + 13, name, 20)
        p.line((x, top + 50), (x, 1330), width=2, dash=10)

    def msg(y, a, b, text, kind="solid"):
        x1, x2 = xmap[a], xmap[b]
        p.line((x1, y), (x2, y), width=2, dash=None if kind == "solid" else 9)
        p.tri((x2, y), 0 if x2 > x1 else math.pi, size=13)
        p.center((x1 + x2) / 2, y - 28, text, 18, bg=True)

    def back(y, a, b, text):
        x1, x2 = xmap[a], xmap[b]
        p.line((x1, y), (x2, y), width=2, dash=9)
        p.tri((x2, y), 0 if x2 > x1 else math.pi, size=13)
        p.center((x1 + x2) / 2, y - 28, text, 18, bg=True)

    def selfcall(y, who, text):
        x = xmap[who]
        p.line((x, y), (x + 100, y), width=2)
        p.line((x + 100, y), (x + 100, y + 44), width=2)
        p.line((x + 100, y + 44), (x, y + 44), width=2)
        p.tri((x, y + 44), math.pi, size=13)
        p.text(x + 112, y + 6, text, 18)

    msg(180, "学生", "ReviewDialog", "1: 打开待办任务")
    msg(240, "ReviewDialog", "ReviewService", "2: loadTask(taskId)")
    msg(300, "ReviewService", "ReviewDao", "3: 查任务与细则")
    back(360, "ReviewService", "ReviewDao", "4: TaskRow + 细则列表")

    msg(460, "ReviewDialog", "AiAssist", "5: 点击「AI 参考意见」（可选）", "dash")
    selfcall(520, "AiAssist", "6: 后台请求，不占 EDT")
    back(650, "ReviewDialog", "AiAssist", "7: AiAdoption（建议分 + 总评草稿）")

    msg(740, "学生", "ReviewDialog", "8: 核对 / 修改分数与评语")
    msg(800, "ReviewDialog", "ReviewService", "9: submit(taskId, scores, comment)")
    selfcall(860, "ReviewService", "10: 校验区间与评语长度")
    msg(960, "ReviewService", "ScoreCalculator", "11: calculate(items, scores)")
    back(1020, "ReviewService", "ScoreCalculator", "12: totalScore")
    msg(1100, "ReviewService", "ReviewDao", "13: 写 review + review_score")
    msg(1160, "ReviewService", "ReviewDao", "14: 标记任务完成")
    back(1220, "ReviewDialog", "ReviewService", "15: 完成")
    back(1280, "学生", "ReviewDialog", "16: 刷新列表与统计")

    p.text(60, 1350, "注：第 5~7 步是旁路——AI 只把建议写回界面控件，第 9 步的入口与参数与不使用 AI 时完全一致。",
           19, fill=MUTED)
    p.save("fig-4-4-sequence-review.png")


# ============================ 图 4-5 状态图 ============================
def fig_state():
    p = Pen(1700, 780)
    p.text(40, 22, "图 4-5  状态图：作业（Assignment）状态机", 25, fill=ACCENT)
    p.d.ellipse([60, 300, 100, 340], fill=LINE)
    nodes = [("草稿", "DRAFT", 190), ("开放提交", "PUBLISHED", 550),
             ("互评中", "ALLOCATED", 950), ("已结束", "CLOSED", 1350)]
    boxes = []
    for cn, en, x in nodes:
        p.d.rounded_rectangle([x, 270, x + 220, 380], radius=26, fill="white",
                              outline=LINE, width=2)
        p.center(x + 110, 292, cn, 24)
        p.center(x + 110, 332, en, 20, MUTED)
        boxes.append((x, 270, 220, 110))

    p.line((100, 320), (186, 320), width=2)
    p.tri((188, 320), 0, size=13)
    p.text(66, 272, "创建", 19, fill=MUTED)

    def fwd(i, j, label):
        x1, y1, w1, _ = boxes[i]
        x2, y2, _, _ = boxes[j]
        p.line((x1 + w1, y1 + 55), (x2, y2 + 55), width=2)
        p.tri((x2, y2 + 55), 0, size=13)
        p.center((x1 + w1 + x2) / 2, y1 + 16, label, 19, bg=True)

    fwd(0, 1, "发布")
    fwd(1, 2, "分配成功")
    fwd(2, 3, "结束互评")

    p.line((660, 380), (660, 440), width=2)
    p.line((660, 440), (300, 440), width=2)
    p.line((300, 440), (300, 384), width=2)
    p.tri((300, 382), -math.pi / 2, size=13)
    p.center(480, 448, "撤回", 19, bg=True)

    p.d.ellipse([1430, 420, 1480, 470], outline=LINE, width=2)
    p.d.ellipse([1442, 432, 1468, 458], fill=LINE)
    p.line((1460, 380), (1460, 418), width=2)
    p.tri((1460, 418), math.pi / 2, size=13)

    p.text(60, 470, "迁移表（AssignmentService.TRANSITIONS）", 22)
    p.d.rectangle([60, 510, 780, 690], fill="white", outline="#c9ced6", width=2)
    rows = [("DRAFT", "PUBLISHED"), ("PUBLISHED", "DRAFT, CLOSED"),
            ("ALLOCATED", "CLOSED"), ("CLOSED", "—（终态）")]
    p.text(84, 522, "当前状态", 20, fill=MUTED)
    p.text(360, 522, "允许迁移到", 20, fill=MUTED)
    p.line((60, 552), (780, 552), width=1, dash=6)
    for k, (a, b) in enumerate(rows):
        p.text(84, 566 + k * 34, a, 20)
        p.text(360, 566 + k * 34, b, 20)
    p.text(830, 510, "① 非法迁移一律拒绝，提示当前状态与允许动作；", 20, fill=MUTED)
    p.text(830, 550, "② 「发布 → 互评中」由分配服务在任务落库后触发，", 20, fill=MUTED)
    p.text(870, 590, "不暴露为可手工点击的迁移，避免出现「任务已生成", 20, fill=MUTED)
    p.text(870, 630, "但状态仍是开放提交」的中间态；", 20, fill=MUTED)
    p.text(830, 680, "③ 已结束为终态，需重开时新建作业，保证历史成绩不被改写。", 20, fill=MUTED)
    p.save("fig-4-5-state-assignment.png")


# ============================ 图 5-1 ER 图 ============================
def fig_er():
    p = Pen(2000, 1460)
    p.text(40, 22, "图 5-1  数据库 ER 图：8 张表及其关系（未建外键，由服务层显式级联）", 25, fill=ACCENT)

    user = p.entity(50, 100, "app_user", [
        "* id  BIGINT PK", "username  VARCHAR(64) UQ", "password_hash  VARCHAR(128)",
        "display_name  VARCHAR(64)", "role  VARCHAR(16)", "created_at  TIMESTAMP"], width=350)
    course = p.entity(50, 460, "course", [
        "* id  BIGINT PK", "code / name / semester", "teacher_id → app_user.id",
        "created_at  TIMESTAMP"], width=350)
    assign = p.entity(500, 460, "assignment", [
        "* id  BIGINT PK", "course_id → course.id", "title / description",
        "deadline  TIMESTAMP NULL", "anonymous  BOOLEAN", "reviewers_per_submission  INT",
        "scoring_mode / status", "created_at  TIMESTAMP"], width=360)
    rubric = p.entity(500, 830, "rubric_item", [
        "* id  BIGINT PK", "assignment_id → assignment.id", "name  VARCHAR(64)",
        "weight  INT", "min_score / max_score", "sort_no  INT"], width=360)
    sub = p.entity(960, 460, "submission", [
        "* id  BIGINT PK", "assignment_id → assignment.id", "student_id → app_user.id",
        "content  TEXT", "submitted_at / updated_at", "UQ(assignment_id, student_id)"], width=360)
    task = p.entity(960, 830, "review_task", [
        "* id  BIGINT PK", "assignment_id → assignment.id", "submission_id → submission.id",
        "reviewer_id → app_user.id", "status", "created_at / completed_at",
        "UQ(submission_id, reviewer_id)"], width=360)
    review = p.entity(1430, 830, "review", [
        "* id  BIGINT PK", "task_id → review_task.id  UQ", "submission_id / reviewer_id",
        "total_score  DECIMAL(6,2)", "comment  VARCHAR(2000)", "created_at  TIMESTAMP"], width=370)
    score = p.entity(1430, 1130, "review_score", [
        "* id  BIGINT PK", "review_id → review.id", "rubric_item_id → rubric_item.id",
        "item_name（冗余，保留历史维度名）", "score  DECIMAL(6,2)"], width=470)

    def right(b):   # 右中
        return (b[0] + b[2], b[1] + b[3] // 2)

    def left(b):
        return (b[0], b[1] + b[3] // 2)

    def bottom(b, dx=0):
        return (b[0] + b[2] // 2 + dx, b[1] + b[3])

    def top(b, dx=0):
        return (b[0] + b[2] // 2 + dx, b[1])

    p.arrow(bottom(user), top(course), "assoc", label="1 : N  授课", label_at=0.5, label_dy=-24)
    p.arrow(right(course), left(assign), "assoc", label="1 : N", label_dy=-24)
    p.arrow(bottom(assign), top(rubric), "assoc", label="1 : N", label_dy=-4, label_dx=-60)
    p.arrow(right(assign), left(sub), "assoc", label="1 : N", label_dy=-24)
    p.arrow(bottom(sub), top(task), "assoc", label="1 : N", label_dy=-4, label_dx=-60)
    p.arrow(right(task), left(review), "assoc", label="1 : 0..1", label_dy=-24)
    p.arrow(bottom(review), top(score), "assoc", label="1 : N", label_at=0.45, label_dy=-26)

    p.text(50, 700, "app_user 另被", 19, fill=MUTED)
    p.text(50, 732, "submission.student_id、", 19, fill=MUTED)
    p.text(50, 764, "review_task.reviewer_id", 19, fill=MUTED)
    p.text(50, 796, "引用，图面不重复连线。", 19, fill=MUTED)
    p.text(450, 1372, "① 唯一约束承担业务底线：防重复提交、防重复分配、防重复评审；", 19, fill=MUTED)
    p.text(450, 1406, "② review_score 冗余 item_name，保证细则改名后历史评分仍可读；", 19, fill=MUTED)
    p.save("fig-5-1-er.png")


if __name__ == "__main__":
    fig_usecase()
    fig_core_classes()
    fig_strategies()
    fig_dao_layer()
    fig_sequence()
    fig_state()
    fig_er()
