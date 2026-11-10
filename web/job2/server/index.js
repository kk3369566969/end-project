import express from "express";
import cors from "cors";
import dotenv from "dotenv";
import { categories } from "./data.js";
import { initDatabase, listBooks, createOrder, isFallback } from "./db.js";
import {
  addAddress,
  addPostComment,
  addReview,
  addReviewComment,
  canReviewBook,
  createForumPost,
  getReviewComments,
  getNotifications,
  getUserProfile,
  getBookReviews,
  getForumPosts,
  getPostComments,
  getProfile,
  removeAddress,
  reportPost,
  markNotificationsRead,
  markNotificationRead,
  toggleFollow,
  toggleCommentLike,
  togglePostLike,
  toggleReviewLike,
  toggleFavorite,
  updateProfile,
} from "./community.js";
import { getUserFromRequest, login, logout, register } from "./auth.js";
import {
  createBook,
  deleteBook,
  deletePost,
  deleteReview,
  getOverview,
  listAdminBooks,
  listAdminOrders,
  listAdminPosts,
  listAdminReviews,
  listAdminUsers,
  updateBook,
  updateOrderStatus,
  setPostPinned,
  listInventory,
  updateInventory,
} from "./admin.js";
import { getReadingData, recordReadingHistory, updateReading } from "./reading.js";
import {
  buySecondhand,
  createClub,
  createSecondhand,
  getAdminAnalytics,
  getReaderAnalytics,
  getReadingLeaderboard,
  joinChallenge,
  joinClub,
  listChallenges,
  listClubs,
  listSecondhand,
  seedNewModules,
  updateChallengeProgress,
} from "./modules.js";

dotenv.config();
const app = express();
const port = Number(process.env.PORT || 3001);
app.use(cors());
// 帖子支持将小型图片以 base64 保存到数据库，默认 100KB 会导致带图讨论直接返回 413。
app.use(express.json({ limit: "5mb" }));

async function requireUser(req, res, next) {
  try {
    const user = await getUserFromRequest(req);
    if (!user) return res.status(401).json({ message: "请先登录" });
    req.user = user;
    next();
  } catch (error) {
    res.status(503).json({ message: "数据库暂不可用", detail: error.message });
  }
}

async function requireAdmin(req, res, next) {
  await requireUser(req, res, () => {
    if (req.user.role !== "admin")
      return res.status(403).json({ message: "只有管理员可以执行此操作" });
    next();
  });
}

app.get("/api/health", (_req, res) =>
  res.json({ ok: !isFallback(), mode: isFallback() ? "unavailable" : "mysql" }),
);

app.post("/api/auth/login", async (req, res) => {
  try {
    const { email, password } = req.body || {};
    const result = await login(email, password);
    result ? res.json(result) : res.status(401).json({ message: "邮箱或密码不正确，请检查后重试" });
  } catch (error) {
    res.status(503).json({ message: "数据库暂不可用", detail: error.message });
  }
});

app.post("/api/auth/register", async (req, res) => {
  const { name, email, password } = req.body || {};
  if (!name?.trim() || !email?.trim() || !password || password.length < 6)
    return res.status(400).json({ message: "请填写完整信息，密码至少 6 位" });
  try {
    const result = await register({ name, email, password });
    result?.error ? res.status(409).json({ message: result.error }) : res.status(201).json(result);
  } catch (error) {
    res.status(503).json({ message: "数据库暂不可用", detail: error.message });
  }
});

app.get("/api/auth/me", async (req, res) => {
  try {
    const user = await getUserFromRequest(req);
    user ? res.json({ user }) : res.status(401).json({ message: "未登录" });
  } catch (error) {
    res.status(503).json({ message: "数据库暂不可用", detail: error.message });
  }
});

app.post("/api/auth/logout", async (req, res) => {
  try {
    await logout(req.headers.authorization?.replace(/^Bearer\s+/i, ""));
    res.json({ ok: true });
  } catch (error) {
    res.status(503).json({ message: "数据库暂不可用", detail: error.message });
  }
});

app.get("/api/categories", (_req, res) => res.json(categories));
app.get("/api/books", async (req, res) => {
  try {
    res.json(await listBooks(req.query));
  } catch (error) {
    res.status(500).json({ message: "书目加载失败", detail: error.message });
  }
});
app.get("/api/books/:id", async (req, res) => {
  try {
    const result = await listBooks({ search: "" });
    const book = result.find((item) => String(item.id) === req.params.id);
    book ? res.json(book) : res.status(404).json({ message: "找不到这本书" });
  } catch (error) {
    res.status(500).json({ message: "书目加载失败", detail: error.message });
  }
});

app.post("/api/orders", requireUser, async (req, res) => {
  const { customer, items, total } = req.body || {};
  if (
    !customer?.name ||
    !customer?.phone ||
    !customer?.address ||
    !Array.isArray(items) ||
    !items.length
  )
    return res.status(400).json({ message: "请完整填写收货信息并确认购物车" });
  try {
    res.status(201).json(await createOrder({ userId: req.user.id, customer, items, total }));
  } catch (error) {
    res.status(500).json({ message: "订单创建失败", detail: error.message });
  }
});

app.get("/api/books/:id/reviews", async (req, res) => {
  try {
    res.json(await getBookReviews(req.params.id, req.query.sort));
  } catch (error) {
    res.status(500).json({ message: "评论加载失败", detail: error.message });
  }
});
app.post("/api/books/:id/reviews", requireUser, async (req, res) => {
  const { rating, content } = req.body || {};
  if (!rating || !content?.trim()) return res.status(400).json({ message: "请填写评分和评论内容" });
  try {
    res.status(201).json(await addReview(req.user.id, req.params.id, { rating, content }));
  } catch (error) {
    res.status(500).json({ message: "评论发布失败", detail: error.message });
  }
});
app.get("/api/books/:id/review-permission", requireUser, async (req, res) => {
  try {
    res.json({ canReview: await canReviewBook(req.user.id, req.params.id) });
  } catch (error) {
    res.status(500).json({ message: "评价资格检查失败", detail: error.message });
  }
});
app.post("/api/reviews/:id/like", requireUser, async (req, res) => {
  try {
    res.json(await toggleReviewLike(req.user.id, req.params.id));
  } catch (error) {
    res.status(500).json({ message: "评论点赞失败", detail: error.message });
  }
});
app.get("/api/reviews/:id/comments", async (req, res) => {
  try {
    res.json(await getReviewComments(req.params.id));
  } catch (error) {
    res.status(500).json({ message: "评论回复加载失败", detail: error.message });
  }
});
app.post("/api/reviews/:id/comments", requireUser, async (req, res) => {
  if (!req.body?.content?.trim()) return res.status(400).json({ message: "回复内容不能为空" });
  try {
    res.status(201).json(await addReviewComment(req.user.id, req.params.id, req.body.content));
  } catch (error) {
    res.status(500).json({ message: "评论回复失败", detail: error.message });
  }
});

app.get("/api/forum/posts", async (req, res) => {
  try {
    res.json(await getForumPosts(req.query));
  } catch (error) {
    res.status(500).json({ message: "论坛加载失败", detail: error.message });
  }
});
app.post("/api/forum/posts", requireUser, async (req, res) => {
  const { title, excerpt, bodyMarkdown, imageUrl, imageData, category, tags } = req.body || {};
  const content = String(excerpt || bodyMarkdown || "").trim();
  if (!title?.trim() || !content) return res.status(400).json({ message: "请填写帖子标题和正文" });
  try {
    res.status(201).json(
      await createForumPost(req.user.id, {
        title,
        excerpt: content,
        bodyMarkdown,
        imageUrl,
        imageData,
        category,
        tags,
      }),
    );
  } catch (error) {
    res.status(500).json({ message: "帖子发布失败", detail: error.message });
  }
});
app.post("/api/forum/posts/:id/like", requireUser, async (req, res) => {
  try {
    res.json(await togglePostLike(req.user.id, req.params.id));
  } catch (error) {
    res.status(500).json({ message: "帖子点赞失败", detail: error.message });
  }
});
app.post("/api/forum/posts/:id/report", requireUser, async (req, res) => {
  try {
    res.json(await reportPost(req.user.id, req.params.id, req.body?.reason));
  } catch (error) {
    res.status(500).json({ message: "举报提交失败", detail: error.message });
  }
});
app.get("/api/forum/posts/:id/comments", async (req, res) => {
  try {
    res.json(await getPostComments(req.params.id));
  } catch (error) {
    res.status(500).json({ message: "回复加载失败", detail: error.message });
  }
});
app.post("/api/forum/posts/:id/comments", requireUser, async (req, res) => {
  const { content } = req.body || {};
  if (!content?.trim()) return res.status(400).json({ message: "回复内容不能为空" });
  try {
    res.status(201).json(await addPostComment(req.user.id, req.params.id, content));
  } catch (error) {
    res.status(500).json({ message: "回复发布失败", detail: error.message });
  }
});
app.post("/api/forum/comments/:id/like", requireUser, async (req, res) => {
  try {
    res.json(await toggleCommentLike(req.user.id, req.params.id));
  } catch (error) {
    res.status(500).json({ message: "回复点赞失败", detail: error.message });
  }
});
app.post("/api/users/:id/follow", requireUser, async (req, res) => {
  try {
    res.json(await toggleFollow(req.user.id, req.params.id));
  } catch (error) {
    res.status(400).json({ message: error.message || "关注操作失败" });
  }
});
app.get("/api/users/:id/profile", async (req, res) => {
  try {
    const viewer = await getUserFromRequest(req);
    res.json(await getUserProfile(req.params.id, viewer?.id));
  } catch (error) {
    res.status(500).json({ message: "用户主页加载失败", detail: error.message });
  }
});

app.get("/api/profile", requireUser, async (req, res) => {
  try {
    res.json(await getProfile(req.user.id));
  } catch (error) {
    res.status(500).json({ message: "个人资料加载失败", detail: error.message });
  }
});
app.get("/api/notifications", requireUser, async (req, res) => {
  try {
    res.json(await getNotifications(req.user.id));
  } catch (error) {
    res.status(500).json({ message: "通知加载失败", detail: error.message });
  }
});
app.post("/api/notifications/read", requireUser, async (req, res) => {
  try {
    res.json(await markNotificationsRead(req.user.id));
  } catch (error) {
    res.status(500).json({ message: "通知更新失败", detail: error.message });
  }
});
app.post("/api/notifications/:id/read", requireUser, async (req, res) => {
  try {
    res.json(await markNotificationRead(req.user.id, req.params.id));
  } catch (error) {
    res.status(500).json({ message: "通知更新失败", detail: error.message });
  }
});
app.get("/api/reading", requireUser, async (req, res) => {
  try {
    res.json(await getReadingData(req.user.id));
  } catch (error) {
    res.status(500).json({ message: "阅读书架加载失败", detail: error.message });
  }
});

app.get("/api/secondhand", requireUser, async (req, res) => {
  try {
    res.json(await listSecondhand({ status: req.query.status || "active" }));
  } catch (error) {
    res.status(500).json({ message: "二手书加载失败", detail: error.message });
  }
});
app.post("/api/secondhand", requireUser, async (req, res) => {
  if (!req.body?.title?.trim() || !req.body?.author?.trim())
    return res.status(400).json({ message: "请填写书名和作者" });
  try {
    res.status(201).json(await createSecondhand(req.user.id, req.body));
  } catch (error) {
    res.status(400).json({ message: "二手书发布失败", detail: error.message });
  }
});
app.post("/api/secondhand/:id/buy", requireUser, async (req, res) => {
  try {
    res.json(await buySecondhand(req.user.id, req.params.id));
  } catch (error) {
    res.status(400).json({ message: error.message || "购买失败" });
  }
});
app.get("/api/clubs", requireUser, async (req, res) => {
  try {
    res.json(await listClubs(req.user.id));
  } catch (error) {
    res.status(500).json({ message: "读书会加载失败", detail: error.message });
  }
});
app.post("/api/clubs", requireUser, async (req, res) => {
  if (!req.body?.title?.trim() || !req.body?.eventAt)
    return res.status(400).json({ message: "请填写活动标题和时间" });
  try {
    res.status(201).json(await createClub(req.user.id, req.body));
  } catch (error) {
    res.status(400).json({ message: "活动创建失败", detail: error.message });
  }
});
app.post("/api/clubs/:id/join", requireUser, async (req, res) => {
  try {
    res.json(await joinClub(req.user.id, req.params.id));
  } catch (error) {
    res.status(400).json({ message: "报名失败", detail: error.message });
  }
});
app.get("/api/challenges", requireUser, async (req, res) => {
  try {
    res.json(await listChallenges(req.user.id));
  } catch (error) {
    res.status(500).json({ message: "阅读挑战加载失败", detail: error.message });
  }
});
app.post("/api/challenges/:id/join", requireUser, async (req, res) => {
  try {
    res.json(await joinChallenge(req.user.id, req.params.id));
  } catch (error) {
    res.status(400).json({ message: "加入挑战失败", detail: error.message });
  }
});
app.patch("/api/challenges/:id/progress", requireUser, async (req, res) => {
  try {
    res.json(await updateChallengeProgress(req.user.id, req.params.id, req.body || {}));
  } catch (error) {
    res.status(400).json({ message: "进度保存失败", detail: error.message });
  }
});
app.get("/api/analytics/me", requireUser, async (req, res) => {
  try {
    res.json(await getReaderAnalytics(req.user.id));
  } catch (error) {
    res.status(500).json({ message: "阅读数据加载失败", detail: error.message });
  }
});
app.get("/api/reading/leaderboard", requireUser, async (_req, res) => {
  try {
    res.set("Cache-Control", "no-store");
    res.json(await getReadingLeaderboard());
  } catch (error) {
    res.status(500).json({ message: "阅读排行榜加载失败", detail: error.message });
  }
});
app.patch("/api/reading/:bookId", requireUser, async (req, res) => {
  try {
    res.json(await updateReading(req.user.id, req.params.bookId, req.body || {}));
  } catch (error) {
    res.status(400).json({ message: "阅读状态更新失败", detail: error.message });
  }
});
app.post("/api/reading/:bookId/history", requireUser, async (req, res) => {
  try {
    res.json(await recordReadingHistory(req.user.id, req.params.bookId));
  } catch (error) {
    res.status(400).json({ message: "阅读记录保存失败", detail: error.message });
  }
});
app.patch("/api/profile", requireUser, async (req, res) => {
  try {
    res.json(await updateProfile(req.user.id, req.body || {}));
  } catch (error) {
    res.status(500).json({ message: "资料更新失败", detail: error.message });
  }
});
app.post("/api/profile/favorites/:bookId", requireUser, async (req, res) => {
  try {
    res.json({
      favorites: await toggleFavorite(req.user.id, req.params.bookId),
    });
  } catch (error) {
    res.status(500).json({ message: "收藏更新失败", detail: error.message });
  }
});
app.get("/api/profile/reviews/:bookId", requireUser, async (req, res) => {
  try {
    res.json(await getBookReviews(req.params.bookId));
  } catch (error) {
    res.status(500).json({ message: "评论加载失败", detail: error.message });
  }
});
app.post("/api/profile/addresses", requireUser, async (req, res) => {
  const { label, receiver, phone, detail } = req.body || {};
  if (!receiver?.trim() || !phone?.trim() || !detail?.trim())
    return res.status(400).json({ message: "请完整填写地址信息" });
  try {
    res.status(201).json(
      await addAddress(req.user.id, {
        label: label?.trim() || "常用地址",
        receiver,
        phone,
        detail,
      }),
    );
  } catch (error) {
    res.status(500).json({ message: "地址保存失败", detail: error.message });
  }
});
app.delete("/api/profile/addresses/:id", requireUser, async (req, res) => {
  try {
    res.json(await removeAddress(req.user.id, req.params.id));
  } catch (error) {
    res.status(500).json({ message: "地址删除失败", detail: error.message });
  }
});

app.get("/api/admin/overview", requireAdmin, async (_req, res) => {
  try {
    res.json(await getOverview());
  } catch (error) {
    res.status(500).json({ message: "后台数据加载失败", detail: error.message });
  }
});
app.get("/api/admin/analytics", requireAdmin, async (_req, res) => {
  try {
    res.json(await getAdminAnalytics());
  } catch (error) {
    res.status(500).json({ message: "经营数据加载失败", detail: error.message });
  }
});
app.get("/api/admin/books", requireAdmin, async (_req, res) => {
  try {
    res.json(await listAdminBooks());
  } catch (error) {
    res.status(500).json({ message: "图书管理数据加载失败", detail: error.message });
  }
});
app.get("/api/admin/inventory", requireAdmin, async (_req, res) => {
  try {
    res.json(await listInventory());
  } catch (error) {
    res.status(500).json({ message: "库存数据加载失败", detail: error.message });
  }
});
app.patch("/api/admin/inventory/:id", requireAdmin, async (req, res) => {
  if (!Number.isFinite(Number(req.body?.stock)) || Number(req.body.stock) < 0)
    return res.status(400).json({ message: "库存数量必须是非负数字" });
  try {
    const item = await updateInventory(req.params.id, req.user.id, req.body || {});
    item ? res.json(item) : res.status(404).json({ message: "图书不存在" });
  } catch (error) {
    res.status(400).json({ message: "库存更新失败", detail: error.message });
  }
});
app.post("/api/admin/books", requireAdmin, async (req, res) => {
  try {
    res.status(201).json(await createBook(req.body || {}));
  } catch (error) {
    res.status(400).json({ message: "图书保存失败", detail: error.message });
  }
});
app.patch("/api/admin/books/:id", requireAdmin, async (req, res) => {
  try {
    const book = await updateBook(req.params.id, req.body || {});
    book ? res.json(book) : res.status(404).json({ message: "图书不存在" });
  } catch (error) {
    res.status(400).json({ message: "图书更新失败", detail: error.message });
  }
});
app.delete("/api/admin/books/:id", requireAdmin, async (req, res) => {
  try {
    await deleteBook(req.params.id);
    res.json({ ok: true });
  } catch (error) {
    res.status(400).json({
      message: "图书删除失败，请先处理关联订单或评论",
      detail: error.message,
    });
  }
});
app.get("/api/admin/orders", requireAdmin, async (_req, res) => {
  try {
    res.json(await listAdminOrders());
  } catch (error) {
    res.status(500).json({ message: "订单管理数据加载失败", detail: error.message });
  }
});
app.patch("/api/admin/orders/:id/status", requireAdmin, async (req, res) => {
  const allowed = ["待发货", "配送中", "已完成", "已取消"];
  if (!allowed.includes(req.body?.status))
    return res.status(400).json({ message: "不支持的订单状态" });
  try {
    res.json(await updateOrderStatus(req.params.id, req.body.status));
  } catch (error) {
    res.status(400).json({ message: "订单状态更新失败", detail: error.message });
  }
});
app.get("/api/admin/users", requireAdmin, async (_req, res) => {
  try {
    res.json(await listAdminUsers());
  } catch (error) {
    res.status(500).json({ message: "用户管理数据加载失败", detail: error.message });
  }
});
app.get("/api/admin/posts", requireAdmin, async (_req, res) => {
  try {
    res.json(await listAdminPosts());
  } catch (error) {
    res.status(500).json({ message: "帖子管理数据加载失败", detail: error.message });
  }
});
app.delete("/api/admin/posts/:id", requireAdmin, async (req, res) => {
  try {
    await deletePost(req.params.id);
    res.json({ ok: true });
  } catch (error) {
    res.status(400).json({ message: "帖子删除失败", detail: error.message });
  }
});
app.patch("/api/admin/posts/:id/pin", requireAdmin, async (req, res) => {
  try {
    res.json(await setPostPinned(req.params.id, Boolean(req.body?.pinned)));
  } catch (error) {
    res.status(400).json({ message: "帖子置顶更新失败", detail: error.message });
  }
});
app.get("/api/admin/reviews", requireAdmin, async (_req, res) => {
  try {
    res.json(await listAdminReviews());
  } catch (error) {
    res.status(500).json({ message: "评论管理数据加载失败", detail: error.message });
  }
});
app.delete("/api/admin/reviews/:id", requireAdmin, async (req, res) => {
  try {
    await deleteReview(req.params.id);
    res.json({ ok: true });
  } catch (error) {
    res.status(400).json({ message: "评论删除失败", detail: error.message });
  }
});

initDatabase()
  .then(() => (isFallback() ? null : seedNewModules()))
  .finally(() =>
    app.listen(port, () => console.log(`Paper Lantern API running at http://localhost:${port}`)),
  );
