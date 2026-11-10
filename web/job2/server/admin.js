import { query } from "./db.js";

const mapBook = (row) => ({
  id: row.id,
  title: row.title,
  author: row.author,
  category: row.category,
  categoryLabel: row.category_label,
  price: Number(row.price),
  originalPrice: Number(row.original_price),
  rating: Number(row.rating || 0),
  stock: Number(row.stock || 0),
  badge: row.badge,
  description: row.description,
  cover: row.cover_data || row.cover,
  coverData: row.cover_data || "",
  inventoryStatus: row.inventory_status || "active",
  tone: row.tone,
  isbn: row.isbn,
  publisher: row.publisher,
  publishedAt: row.published_at,
  authorBio: row.author_bio,
  toc: row.toc,
  excerpt: row.excerpt,
  createdAt: row.created_at,
});

export async function getOverview() {
  const [[bookRows], [userRows], [orderRows], [postRows], [revenueRows]] = await Promise.all([
    query("SELECT COUNT(*) AS count FROM books"),
    query("SELECT COUNT(*) AS count FROM users"),
    query("SELECT COUNT(*) AS count FROM orders"),
    query("SELECT COUNT(*) AS count FROM forum_posts"),
    query("SELECT COALESCE(SUM(total_amount), 0) AS total FROM orders WHERE status <> '已取消'"),
  ]);
  return {
    books: Number(bookRows[0].count),
    users: Number(userRows[0].count),
    orders: Number(orderRows[0].count),
    posts: Number(postRows[0].count),
    revenue: Number(revenueRows[0].total),
  };
}

export async function listAdminBooks() {
  const [rows] = await query(
    "SELECT id, title, author, category, category_label, price, original_price, rating, stock, badge, description, cover, cover_data, inventory_status, tone, isbn, publisher, published_at, author_bio, toc, excerpt, created_at FROM books ORDER BY created_at DESC, id DESC",
  );
  return rows.map(mapBook);
}

export async function createBook(payload) {
  const [result] = await query(
    "INSERT INTO books (title, author, category, category_label, price, original_price, rating, stock, badge, description, cover, cover_data, inventory_status, tone, isbn, publisher, published_at, author_bio, toc, excerpt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
    [
      payload.title.trim(),
      payload.author.trim(),
      payload.category,
      payload.categoryLabel.trim(),
      Number(payload.price),
      Number(payload.originalPrice),
      Number(payload.rating || 0),
      Number(payload.stock || 0),
      payload.badge?.trim() || "",
      payload.description?.trim() || "",
      payload.cover?.trim() || "",
      payload.coverData || "",
      payload.inventoryStatus || "active",
      payload.tone?.trim() || "ink",
      payload.isbn?.trim() || "",
      payload.publisher?.trim() || "纸灯书局",
      payload.publishedAt || null,
      payload.authorBio?.trim() || "",
      payload.toc?.trim() || "序章｜第一章｜第二章｜尾声",
      payload.excerpt?.trim() || payload.description?.trim() || "",
    ],
  );
  const [rows] = await query(
    "SELECT id, title, author, category, category_label, price, original_price, rating, stock, badge, description, cover, cover_data, inventory_status, tone, isbn, publisher, published_at, author_bio, toc, excerpt, created_at FROM books WHERE id = ?",
    [result.insertId],
  );
  return mapBook(rows[0]);
}

export async function updateBook(id, payload) {
  await query(
    "UPDATE books SET title = ?, author = ?, category = ?, category_label = ?, price = ?, original_price = ?, rating = ?, stock = ?, badge = ?, description = ?, cover = ?, cover_data = ?, inventory_status = ?, tone = ?, isbn = ?, publisher = ?, published_at = ?, author_bio = ?, toc = ?, excerpt = ? WHERE id = ?",
    [
      payload.title.trim(),
      payload.author.trim(),
      payload.category,
      payload.categoryLabel.trim(),
      Number(payload.price),
      Number(payload.originalPrice),
      Number(payload.rating || 0),
      Number(payload.stock || 0),
      payload.badge?.trim() || "",
      payload.description?.trim() || "",
      payload.cover?.trim() || "",
      payload.coverData || "",
      payload.inventoryStatus || "active",
      payload.tone?.trim() || "ink",
      payload.isbn?.trim() || "",
      payload.publisher?.trim() || "纸灯书局",
      payload.publishedAt || null,
      payload.authorBio?.trim() || "",
      payload.toc?.trim() || "序章｜第一章｜第二章｜尾声",
      payload.excerpt?.trim() || payload.description?.trim() || "",
      id,
    ],
  );
  const [rows] = await query(
    "SELECT id, title, author, category, category_label, price, original_price, rating, stock, badge, description, cover, cover_data, inventory_status, tone, isbn, publisher, published_at, author_bio, toc, excerpt, created_at FROM books WHERE id = ?",
    [id],
  );
  return rows[0] ? mapBook(rows[0]) : null;
}

export async function deleteBook(id) {
  await query("DELETE FROM books WHERE id = ?", [id]);
}

export async function listAdminOrders() {
  const [rows] = await query(
    "SELECT o.id, o.status, o.total_amount, o.customer_name, o.customer_phone, o.customer_address, o.created_at, u.name AS user_name, GROUP_CONCAT(CONCAT(b.title, ' × ' , oi.quantity) SEPARATOR '、') AS items FROM orders o LEFT JOIN users u ON u.id = o.user_id LEFT JOIN order_items oi ON oi.order_id = o.id LEFT JOIN books b ON b.id = oi.book_id GROUP BY o.id ORDER BY o.created_at DESC",
  );
  return rows.map((row) => ({
    id: row.id,
    status: row.status,
    total: Number(row.total_amount),
    customerName: row.customer_name,
    customerPhone: row.customer_phone,
    customerAddress: row.customer_address,
    userName: row.user_name || "访客",
    items: row.items || "",
    createdAt: row.created_at,
  }));
}

export async function updateOrderStatus(id, status) {
  const [currentRows] = await query("SELECT id, user_id, status FROM orders WHERE id = ?", [id]);
  const current = currentRows[0];
  if (!current) return null;
  await query("UPDATE orders SET status = ? WHERE id = ?", [status, id]);
  if (current.user_id && current.status !== status) {
    await query(
      "INSERT INTO notifications (user_id, type, content) VALUES (?, 'order_status', ?)",
      [current.user_id, `订单 #${id} 状态已更新为：${status}`],
    );
  }
  const [rows] = await query(
    "SELECT id, status, total_amount, customer_name, customer_phone, customer_address, created_at FROM orders WHERE id = ?",
    [id],
  );
  const row = rows[0];
  return row
    ? {
        id: row.id,
        status: row.status,
        total: Number(row.total_amount),
        customerName: row.customer_name,
        customerPhone: row.customer_phone,
        customerAddress: row.customer_address,
        createdAt: row.created_at,
      }
    : null;
}

export async function listAdminUsers() {
  const [rows] = await query(
    "SELECT id, name, email, role, points, created_at FROM users ORDER BY created_at DESC, id DESC",
  );
  return rows.map((row) => ({
    id: row.id,
    name: row.name,
    email: row.email,
    role: row.role,
    roleLabel: row.role === "admin" ? "管理员" : "普通读者",
    points: Number(row.points || 0),
    createdAt: row.created_at,
  }));
}

export async function listAdminPosts() {
  const [rows] = await query(
    "SELECT id, title, category, author_name, likes, comments, pinned, created_at FROM forum_posts ORDER BY pinned DESC, created_at DESC",
  );
  return rows.map((row) => ({
    id: row.id,
    title: row.title,
    category: row.category,
    author: row.author_name,
    likes: Number(row.likes || 0),
    comments: Number(row.comments || 0),
    pinned: Boolean(row.pinned),
    createdAt: row.created_at,
  }));
}

export async function setPostPinned(id, pinned) {
  await query("UPDATE forum_posts SET pinned = ? WHERE id = ?", [pinned ? 1 : 0, id]);
  return { id: Number(id), pinned: Boolean(pinned) };
}

export async function deletePost(id) {
  await query("DELETE FROM forum_comments WHERE post_id = ?", [id]);
  await query("DELETE FROM forum_posts WHERE id = ?", [id]);
}

export async function listAdminReviews() {
  const [rows] = await query(
    "SELECT r.id, r.rating, r.content, r.helpful_count, r.created_at, r.user_name, b.title AS book_title FROM book_reviews r LEFT JOIN books b ON b.id = r.book_id ORDER BY r.created_at DESC",
  );
  return rows.map((row) => ({
    id: row.id,
    bookTitle: row.book_title || "已删除书籍",
    user: row.user_name,
    rating: Number(row.rating),
    content: row.content,
    helpful: Number(row.helpful_count || 0),
    createdAt: row.created_at,
  }));
}

export async function deleteReview(id) {
  await query("DELETE FROM book_reviews WHERE id = ?", [id]);
}

export async function listInventory() {
  const [rows] = await query(`SELECT b.id, b.title, b.author, b.stock, b.inventory_status,
    b.cover, b.cover_data, COALESCE((SELECT COUNT(*) FROM inventory_logs l WHERE l.book_id = b.id), 0) AS changes
    FROM books b ORDER BY b.stock ASC, b.title ASC`);
  return rows.map((row) => ({
    id: row.id,
    title: row.title,
    author: row.author,
    stock: Number(row.stock || 0),
    status: row.inventory_status || "active",
    cover: row.cover_data || row.cover || "",
    changes: Number(row.changes || 0),
    lowStock: Number(row.stock || 0) <= 5,
  }));
}

export async function updateInventory(bookId, adminId, { stock, status, reason } = {}) {
  const nextStock = Math.max(0, Number(stock));
  const nextStatus = ["active", "paused", "sold_out"].includes(status) ? status : "active";
  const [rows] = await query("SELECT stock FROM books WHERE id = ?", [bookId]);
  if (!rows[0]) return null;
  const previous = Number(rows[0].stock || 0);
  await query("UPDATE books SET stock = ?, inventory_status = ? WHERE id = ?", [
    nextStock,
    nextStatus,
    bookId,
  ]);
  if (previous !== nextStock)
    await query(
      "INSERT INTO inventory_logs (book_id, admin_id, delta, stock_after, reason) VALUES (?, ?, ?, ?, ?)",
      [bookId, adminId, nextStock - previous, nextStock, String(reason || "库存调整").trim()],
    );
  const inventory = await listInventory();
  return inventory.find((item) => item.id === Number(bookId));
}
