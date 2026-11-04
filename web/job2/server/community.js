import { query } from "./db.js";

const toTags = (value) => {
  try {
    return value ? JSON.parse(value) : [];
  } catch {
    return [];
  }
};

const mapPost = (row) => ({
  id: row.id,
  title: row.title,
  excerpt: row.excerpt,
  category: row.category,
  author: row.author_name,
  authorId: row.user_id ? Number(row.user_id) : null,
  avatar: row.author_name?.slice(0, 1) || "友",
  likes: Number(row.likes || 0),
  comments: Number(row.comments || 0),
  createdAt: row.created_at,
  tags: toTags(row.tags),
  bodyMarkdown: row.body_markdown || row.excerpt,
  imageUrl: row.image_data || row.image_url || "",
  pinned: Boolean(row.pinned),
});

const mapReview = (row) => ({
  id: row.id,
  user: row.user_name,
  rating: Number(row.rating),
  content: row.content,
  helpful: Number(row.helpful_count || 0),
  liked: Boolean(row.liked),
  createdAt: row.created_at,
});

export async function getForumPosts({ category, search } = {}) {
  const filters = [];
  const values = [];
  if (category && category !== "全部") {
    filters.push("category = ?");
    values.push(category);
  }
  if (search) {
    filters.push("(title LIKE ? OR excerpt LIKE ? OR tags LIKE ?)");
    values.push(`%${search}%`, `%${search}%`, `%${search}%`);
  }
  const [rows] = await query(
    `SELECT id, user_id, title, excerpt, body_markdown, image_url, image_data, category, author_name, likes, comments, created_at, tags, pinned FROM forum_posts ${filters.length ? `WHERE ${filters.join(" AND ")}` : ""} ORDER BY pinned DESC, created_at DESC`,
    values,
  );
  return rows.map(mapPost);
}

export async function createForumPost(
  userId,
  { title, excerpt, bodyMarkdown, imageUrl, imageData, category = "阅读分享", tags = [] },
) {
  const [result] = await query(
    "INSERT INTO forum_posts (user_id, title, excerpt, body_markdown, image_url, image_data, category, author_name, likes, comments, tags) SELECT ?, ?, ?, ?, ?, ?, ?, name, 0, 0, ? FROM users WHERE id = ?",
    [userId, title.trim(), excerpt.trim(), bodyMarkdown?.trim() || excerpt.trim(), imageUrl?.trim() || null, imageData || null, category, JSON.stringify(tags), userId],
  );
  const [rows] = await query(
    "SELECT id, user_id, title, excerpt, body_markdown, image_url, image_data, category, author_name, likes, comments, created_at, tags, pinned FROM forum_posts WHERE id = ?",
    [result.insertId],
  );
  return mapPost(rows[0]);
}

export async function getPostComments(postId) {
  const [rows] = await query(
    "SELECT id, user_name, content, likes, created_at FROM forum_comments WHERE post_id = ? ORDER BY created_at ASC",
    [postId],
  );
  return rows.map((row) => ({
    id: row.id,
    user: row.user_name,
    content: row.content,
    likes: Number(row.likes || 0),
    createdAt: row.created_at,
  }));
}

export async function toggleCommentLike(userId, commentId) {
  const [existing] = await query("SELECT comment_id FROM forum_comment_likes WHERE user_id = ? AND comment_id = ?", [userId, commentId]);
  if (existing.length) {
    await query("DELETE FROM forum_comment_likes WHERE user_id = ? AND comment_id = ?", [userId, commentId]);
    await query("UPDATE forum_comments SET likes = GREATEST(likes - 1, 0) WHERE id = ?", [commentId]);
  } else {
    await query("INSERT INTO forum_comment_likes (user_id, comment_id) VALUES (?, ?)", [userId, commentId]);
    await query("UPDATE forum_comments SET likes = likes + 1 WHERE id = ?", [commentId]);
  }
  const [rows] = await query("SELECT likes FROM forum_comments WHERE id = ?", [commentId]);
  return { liked: !existing.length, likes: Number(rows[0]?.likes || 0) };
}

export async function addPostComment(userId, postId, content) {
  const [result] = await query(
    "INSERT INTO forum_comments (post_id, user_id, user_name, content) SELECT ?, id, name, ? FROM users WHERE id = ?",
    [postId, content.trim(), userId],
  );
  await query("UPDATE forum_posts SET comments = comments + 1 WHERE id = ?", [postId]);
  const [rows] = await query(
    "SELECT id, user_name, content, likes, created_at FROM forum_comments WHERE id = ?",
    [result.insertId],
  );
  const row = rows[0];
  return { id: row.id, user: row.user_name, content: row.content, likes: Number(row.likes || 0), createdAt: row.created_at };
}

export async function addReview(userId, bookId, { rating, content }) {
  const [purchased] = await query(
    "SELECT oi.id FROM orders o JOIN order_items oi ON oi.order_id = o.id WHERE o.user_id = ? AND oi.book_id = ? LIMIT 1",
    [userId, bookId],
  );
  if (!purchased.length) throw new Error("购买该书后才能评价");
  const [result] = await query(
    "INSERT INTO book_reviews (book_id, user_id, user_name, rating, content, helpful_count) SELECT ?, id, name, ?, ?, 0 FROM users WHERE id = ?",
    [bookId, Number(rating), content.trim(), userId],
  );
  const [rows] = await query(
    "SELECT id, user_name, rating, content, helpful_count, created_at FROM book_reviews WHERE id = ?",
    [result.insertId],
  );
  return mapReview(rows[0]);
}

export async function getBookReviews(bookId, sort = "recent") {
  const order = sort === "helpful" ? "helpful_count DESC, created_at DESC" : sort === "rating" ? "rating DESC, created_at DESC" : "created_at DESC";
  const [rows] = await query(
    `SELECT id, user_name, rating, content, helpful_count, created_at FROM book_reviews WHERE book_id = ? ORDER BY ${order}`,
    [bookId],
  );
  return rows.map(mapReview);
}

export async function canReviewBook(userId, bookId) {
  const [rows] = await query(
    "SELECT oi.id FROM orders o JOIN order_items oi ON oi.order_id = o.id WHERE o.user_id = ? AND oi.book_id = ? LIMIT 1",
    [userId, bookId],
  );
  return Boolean(rows.length);
}

export async function togglePostLike(userId, postId) {
  const [existing] = await query("SELECT post_id FROM forum_post_likes WHERE user_id = ? AND post_id = ?", [userId, postId]);
  if (existing.length) {
    await query("DELETE FROM forum_post_likes WHERE user_id = ? AND post_id = ?", [userId, postId]);
    await query("UPDATE forum_posts SET likes = GREATEST(likes - 1, 0) WHERE id = ?", [postId]);
  } else {
    await query("INSERT INTO forum_post_likes (user_id, post_id) VALUES (?, ?)", [userId, postId]);
    await query("UPDATE forum_posts SET likes = likes + 1 WHERE id = ?", [postId]);
    const [owners] = await query("SELECT user_id, title FROM forum_posts WHERE id = ?", [postId]);
    if (owners[0]?.user_id && Number(owners[0].user_id) !== Number(userId))
      await query("INSERT INTO notifications (user_id, type, content) VALUES (?, 'post_like', ?)", [owners[0].user_id, `你的帖子《${owners[0].title}》收到新的赞`]);
  }
  const [rows] = await query("SELECT likes FROM forum_posts WHERE id = ?", [postId]);
  return { liked: !existing.length, likes: Number(rows[0]?.likes || 0) };
}

export async function toggleReviewLike(userId, reviewId) {
  const [existing] = await query("SELECT review_id FROM review_likes WHERE user_id = ? AND review_id = ?", [userId, reviewId]);
  if (existing.length) {
    await query("DELETE FROM review_likes WHERE user_id = ? AND review_id = ?", [userId, reviewId]);
    await query("UPDATE book_reviews SET helpful_count = GREATEST(helpful_count - 1, 0) WHERE id = ?", [reviewId]);
  } else {
    await query("INSERT INTO review_likes (user_id, review_id) VALUES (?, ?)", [userId, reviewId]);
    await query("UPDATE book_reviews SET helpful_count = helpful_count + 1 WHERE id = ?", [reviewId]);
  }
  const [rows] = await query("SELECT helpful_count FROM book_reviews WHERE id = ?", [reviewId]);
  return { liked: !existing.length, helpful: Number(rows[0]?.helpful_count || 0) };
}

export async function addReviewComment(userId, reviewId, content) {
  const [result] = await query("INSERT INTO review_comments (review_id, user_id, content) VALUES (?, ?, ?)", [reviewId, userId, content.trim()]);
  const [rows] = await query("SELECT c.id, c.content, c.created_at, u.name FROM review_comments c JOIN users u ON u.id = c.user_id WHERE c.id = ?", [result.insertId]);
  const row = rows[0];
  return { id: row.id, user: row.name, content: row.content, createdAt: row.created_at };
}

export async function getReviewComments(reviewId) {
  const [rows] = await query("SELECT c.id, c.content, c.created_at, u.name FROM review_comments c JOIN users u ON u.id = c.user_id WHERE c.review_id = ? ORDER BY c.created_at ASC", [reviewId]);
  return rows.map((row) => ({ id: row.id, user: row.name, content: row.content, createdAt: row.created_at }));
}

export async function reportPost(userId, postId, reason) {
  await query("INSERT INTO forum_reports (post_id, user_id, reason) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE reason = VALUES(reason)", [postId, userId, reason?.trim() || "内容需要核实"]);
  await query("UPDATE forum_posts SET report_count = report_count + 1 WHERE id = ?", [postId]);
  return { ok: true };
}

export async function toggleFollow(userId, targetId) {
  if (Number(userId) === Number(targetId)) throw new Error("不能关注自己");
  const [existing] = await query("SELECT follower_id FROM user_follows WHERE follower_id = ? AND following_id = ?", [userId, targetId]);
  if (existing.length) await query("DELETE FROM user_follows WHERE follower_id = ? AND following_id = ?", [userId, targetId]);
  else {
    await query("INSERT INTO user_follows (follower_id, following_id) VALUES (?, ?)", [userId, targetId]);
    const [followers] = await query("SELECT name FROM users WHERE id = ?", [userId]);
    if (followers[0]) await query("INSERT INTO notifications (user_id, type, content) VALUES (?, 'follow', ?)", [targetId, `${followers[0].name} 关注了你的主页`]);
  }
  return { following: !existing.length };
}

export async function getNotifications(userId) {
  const [rows] = await query("SELECT id, type, content, is_read, created_at FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 30", [userId]);
  return rows.map((row) => ({ id: row.id, type: row.type, content: row.content, read: Boolean(row.is_read), createdAt: row.created_at }));
}

export async function markNotificationsRead(userId) {
  await query("UPDATE notifications SET is_read = 1 WHERE user_id = ?", [userId]);
  return { ok: true };
}

export async function markNotificationRead(userId, notificationId) {
  await query("UPDATE notifications SET is_read = 1 WHERE id = ? AND user_id = ?", [notificationId, userId]);
  return { ok: true };
}

export async function getUserProfile(userId, viewerId = null) {
  const [[userRows], [postRows], [followRows]] = await Promise.all([
    query("SELECT id, name, bio, points, created_at FROM users WHERE id = ?", [userId]),
    query("SELECT id, title, excerpt, category, author_name, likes, comments, created_at, tags, pinned FROM forum_posts WHERE user_id = ? ORDER BY created_at DESC LIMIT 12", [userId]),
    viewerId ? query("SELECT follower_id FROM user_follows WHERE follower_id = ? AND following_id = ?", [viewerId, userId]) : Promise.resolve([[]]),
  ]);
  const user = userRows[0];
  return { id: user.id, name: user.name, bio: user.bio || "", points: Number(user.points || 0), following: Boolean(followRows.length), posts: postRows.map(mapPost) };
}

export async function getProfile(userId) {
  const [[userRows], [favoriteRows], [addressRows], [orderRows]] = await Promise.all([
    query("SELECT id, name, email, bio, points, created_at FROM users WHERE id = ?", [userId]),
    query("SELECT book_id FROM user_favorites WHERE user_id = ?", [userId]),
    query(
      "SELECT id, label, receiver, phone, detail, is_default FROM user_addresses WHERE user_id = ? ORDER BY is_default DESC, id DESC",
      [userId],
    ),
    query(
      "SELECT o.id, o.status, o.total_amount, o.created_at, oi.quantity, b.title FROM orders o LEFT JOIN order_items oi ON oi.order_id = o.id LEFT JOIN books b ON b.id = oi.book_id WHERE o.user_id = ? ORDER BY o.created_at DESC",
      [userId],
    ),
  ]);
  const user = userRows[0];
  const orders = [];
  for (const row of orderRows) {
    let order = orders.find((item) => String(item.id) === String(row.id));
    if (!order) {
      order = {
        id: row.id,
        status: row.status,
        total: Number(row.total_amount),
        createdAt: row.created_at,
        items: [],
      };
      orders.push(order);
    }
    if (row.title) order.items.push({ title: row.title, quantity: row.quantity });
  }
  return {
    id: user.id,
    name: user.name,
    email: user.email,
    bio: user.bio || "",
    points: Number(user.points || 0),
    joinedAt: user.created_at,
    favorites: favoriteRows.map((row) => Number(row.book_id)),
    addresses: addressRows.map((row) => ({
      id: row.id,
      label: row.label,
      receiver: row.receiver,
      phone: row.phone,
      detail: row.detail,
      isDefault: Boolean(row.is_default),
    })),
    orders,
  };
}

export async function updateProfile(userId, payload) {
  await query("UPDATE users SET name = ?, bio = ? WHERE id = ?", [
    payload.name?.trim() || "纸灯读者",
    payload.bio?.trim() || "",
    userId,
  ]);
  return getProfile(userId);
}

export async function toggleFavorite(userId, bookId) {
  const [rows] = await query(
    "SELECT book_id FROM user_favorites WHERE user_id = ? AND book_id = ?",
    [userId, bookId],
  );
  if (rows.length)
    await query("DELETE FROM user_favorites WHERE user_id = ? AND book_id = ?", [userId, bookId]);
  else await query("INSERT INTO user_favorites (user_id, book_id) VALUES (?, ?)", [userId, bookId]);
  const [favorites] = await query("SELECT book_id FROM user_favorites WHERE user_id = ?", [userId]);
  return favorites.map((row) => Number(row.book_id));
}

export async function addAddress(userId, address) {
  const [result] = await query(
    "INSERT INTO user_addresses (user_id, label, receiver, phone, detail, is_default) VALUES (?, ?, ?, ?, ?, ?)",
    [
      userId,
      address.label,
      address.receiver.trim(),
      address.phone.trim(),
      address.detail.trim(),
      0,
    ],
  );
  const [rows] = await query(
    "SELECT id, label, receiver, phone, detail, is_default FROM user_addresses WHERE id = ?",
    [result.insertId],
  );
  const row = rows[0];
  return {
    id: row.id,
    label: row.label,
    receiver: row.receiver,
    phone: row.phone,
    detail: row.detail,
    isDefault: Boolean(row.is_default),
  };
}

export async function removeAddress(userId, addressId) {
  await query("DELETE FROM user_addresses WHERE id = ? AND user_id = ?", [addressId, userId]);
  const [remaining] = await query(
    "SELECT id, label, receiver, phone, detail, is_default FROM user_addresses WHERE user_id = ? ORDER BY is_default DESC, id DESC",
    [userId],
  );
  if (remaining.length && !remaining.some((row) => row.is_default))
    await query("UPDATE user_addresses SET is_default = 1 WHERE id = ?", [remaining[0].id]);
  return getProfile(userId).then((profile) => profile.addresses);
}
