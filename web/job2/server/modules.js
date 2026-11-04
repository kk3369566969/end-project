import { query } from "./db.js";

const mapListing = (row) => ({
  id: row.id,
  title: row.title,
  author: row.author,
  condition: row.condition_label,
  price: Number(row.price),
  description: row.description || "",
  cover: row.cover_data || row.cover || "",
  coverData: row.cover_data || "",
  sellerId: row.seller_id,
  seller: row.seller_name || "书友",
  status: row.status,
  createdAt: row.created_at,
  buyerId: row.buyer_id,
});

export async function listSecondhand({ status = "active", userId } = {}) {
  const values = [];
  const filters = [];
  if (status !== "all") {
    filters.push("l.status = ?");
    values.push(status);
  }
  if (userId) {
    filters.push("l.seller_id = ?");
    values.push(userId);
  }
  const [rows] = await query(`SELECT l.*, u.name AS seller_name
    FROM secondhand_listings l LEFT JOIN users u ON u.id = l.seller_id
    ${filters.length ? `WHERE ${filters.join(" AND ")}` : ""}
    ORDER BY l.created_at DESC`, values);
  return rows.map(mapListing);
}

export async function createSecondhand(userId, payload) {
  const [result] = await query(`INSERT INTO secondhand_listings
    (seller_id, title, author, condition_label, price, description, cover, cover_data, status)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'active')`, [
    userId,
    String(payload.title || "").trim(),
    String(payload.author || "").trim(),
    String(payload.condition || "九成新").trim(),
    Number(payload.price || 0),
    String(payload.description || "").trim(),
    String(payload.cover || "").trim(),
    String(payload.coverData || ""),
  ]);
  const [rows] = await query(`SELECT l.*, u.name AS seller_name FROM secondhand_listings l
    LEFT JOIN users u ON u.id = l.seller_id WHERE l.id = ?`, [result.insertId]);
  return mapListing(rows[0]);
}

export async function buySecondhand(userId, listingId) {
  const [result] = await query("UPDATE secondhand_listings SET status = 'sold', buyer_id = ? WHERE id = ? AND status = 'active' AND seller_id <> ?", [userId, listingId, userId]);
  if (!result.affectedRows) throw new Error("这本书已被购买或不能购买自己的书");
  const [rows] = await query(`SELECT l.*, u.name AS seller_name FROM secondhand_listings l
    LEFT JOIN users u ON u.id = l.seller_id WHERE l.id = ?`, [listingId]);
  return mapListing(rows[0]);
}

const mapClub = (row) => ({
  id: row.id,
  title: row.title,
  description: row.description || "",
  bookId: row.book_id,
  bookTitle: row.book_title || "自由选书",
  hostId: row.host_id,
  host: row.host_name || "书友",
  eventAt: row.event_at,
  location: row.location || "线上",
  capacity: Number(row.capacity || 0),
  image: row.image_data || "",
  memberCount: Number(row.member_count || 0),
  joined: Boolean(row.joined),
  status: row.status,
});

export async function listClubs(userId) {
  const [rows] = await query(`SELECT c.*, b.title AS book_title, u.name AS host_name,
    (SELECT COUNT(*) FROM club_members cm WHERE cm.club_id = c.id AND cm.status = 'joined') AS member_count,
    EXISTS(SELECT 1 FROM club_members me WHERE me.club_id = c.id AND me.user_id = ?) AS joined
    FROM book_clubs c LEFT JOIN books b ON b.id = c.book_id LEFT JOIN users u ON u.id = c.host_id
    WHERE c.status = 'published' ORDER BY c.event_at ASC`, [userId || 0]);
  return rows.map(mapClub);
}

export async function createClub(userId, payload) {
  const [result] = await query(`INSERT INTO book_clubs
    (title, description, book_id, host_id, event_at, location, capacity, image_data, status)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'published')`, [
    String(payload.title || "").trim(),
    String(payload.description || "").trim(),
    payload.bookId || null,
    userId,
    payload.eventAt,
    String(payload.location || "线上").trim(),
    Math.max(1, Number(payload.capacity || 20)),
    String(payload.coverData || ""),
  ]);
  const clubs = await listClubs(userId);
  return clubs.find((item) => item.id === result.insertId);
}

export async function joinClub(userId, clubId) {
  const [existing] = await query("SELECT status FROM club_members WHERE club_id = ? AND user_id = ?", [clubId, userId]);
  if (existing.length) {
    await query("UPDATE club_members SET status = IF(status = 'joined', 'left', 'joined') WHERE club_id = ? AND user_id = ?", [clubId, userId]);
  } else {
    await query("INSERT INTO club_members (club_id, user_id, status) VALUES (?, ?, 'joined')", [clubId, userId]);
  }
  const clubs = await listClubs(userId);
  return clubs.find((item) => item.id === Number(clubId));
}

const mapChallenge = (row) => ({
  id: row.id,
  title: row.title,
  description: row.description || "",
  targetBooks: Number(row.target_books || 0),
  targetMinutes: Number(row.target_minutes || 0),
  startDate: row.start_date,
  endDate: row.end_date,
  image: row.image_data || "",
  joined: Boolean(row.joined),
  booksCompleted: Number(row.books_completed || 0),
  minutes: Number(row.minutes || 0),
  status: row.status,
});

export async function listChallenges(userId) {
  const [rows] = await query(`SELECT c.*,
    EXISTS(SELECT 1 FROM challenge_progress cp WHERE cp.challenge_id = c.id AND cp.user_id = ?) AS joined,
    COALESCE((SELECT cp.books_completed FROM challenge_progress cp WHERE cp.challenge_id = c.id AND cp.user_id = ?), 0) AS books_completed,
    COALESCE((SELECT cp.minutes FROM challenge_progress cp WHERE cp.challenge_id = c.id AND cp.user_id = ?), 0) AS minutes
    FROM reading_challenges c WHERE c.status = 'published' ORDER BY c.end_date ASC`, [userId || 0, userId || 0, userId || 0]);
  return rows.map(mapChallenge);
}

export async function joinChallenge(userId, challengeId) {
  await query("INSERT IGNORE INTO challenge_progress (challenge_id, user_id) VALUES (?, ?)", [challengeId, userId]);
  const challenges = await listChallenges(userId);
  return challenges.find((item) => item.id === Number(challengeId));
}

export async function updateChallengeProgress(userId, challengeId, payload) {
  await query(`INSERT INTO challenge_progress (challenge_id, user_id, books_completed, minutes)
    VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE books_completed = VALUES(books_completed), minutes = VALUES(minutes)`, [
    challengeId,
    userId,
    Math.max(0, Number(payload.booksCompleted || 0)),
    Math.max(0, Number(payload.minutes || 0)),
  ]);
  const challenges = await listChallenges(userId);
  return challenges.find((item) => item.id === Number(challengeId));
}

export async function getReaderAnalytics(userId) {
  const [[summaryRows], [categoryRows], [monthlyRows]] = await Promise.all([
    query(`SELECT COALESCE(SUM(s.minutes), 0) AS minutes,
      COUNT(DISTINCT CASE WHEN s.status = 'read' THEN s.book_id END) AS finished,
      COUNT(DISTINCT s.book_id) AS books,
      COALESCE((SELECT SUM(o.total_amount) FROM orders o WHERE o.user_id = ? AND o.status <> '已取消'), 0) AS spending
      FROM reading_shelves s WHERE s.user_id = ?`, [userId, userId]),
    query(`SELECT b.category_label AS label, COUNT(*) AS count FROM reading_shelves s
      JOIN books b ON b.id = s.book_id WHERE s.user_id = ? GROUP BY b.category_label ORDER BY count DESC`, [userId]),
    query(`SELECT DATE_FORMAT(viewed_at, '%Y-%m') AS month, COUNT(*) AS count
      FROM reading_history WHERE user_id = ? GROUP BY month ORDER BY month DESC LIMIT 6`, [userId]),
  ]);
  return {
    summary: {
      minutes: Number(summaryRows[0]?.minutes || 0),
      finished: Number(summaryRows[0]?.finished || 0),
      books: Number(summaryRows[0]?.books || 0),
      spending: Number(summaryRows[0]?.spending || 0),
    },
    categories: categoryRows.map((row) => ({ label: row.label, count: Number(row.count) })),
    monthly: monthlyRows.map((row) => ({ month: row.month, count: Number(row.count) })),
  };
}

export async function getReadingLeaderboard() {
  const [finishedRows, minutesRows, activeRows] = await Promise.all([
    query(`SELECT u.id, u.name,
      COALESCE(SUM(CASE WHEN s.status = 'read' THEN 1 ELSE 0 END), 0) AS score,
      COALESCE(SUM(s.minutes), 0) AS minutes
      FROM users u LEFT JOIN reading_shelves s ON s.user_id = u.id
      WHERE u.role = 'reader' GROUP BY u.id ORDER BY score DESC, minutes DESC, u.id ASC LIMIT 20`),
    query(`SELECT u.id, u.name, COALESCE(SUM(s.minutes), 0) AS score,
      COALESCE(SUM(CASE WHEN s.status = 'read' THEN 1 ELSE 0 END), 0) AS finished
      FROM users u LEFT JOIN reading_shelves s ON s.user_id = u.id
      WHERE u.role = 'reader' GROUP BY u.id ORDER BY score DESC, finished DESC, u.id ASC LIMIT 20`),
    query(`SELECT u.id, u.name, COUNT(h.id) AS score
      FROM users u LEFT JOIN reading_history h ON h.user_id = u.id
      WHERE u.role = 'reader' GROUP BY u.id ORDER BY score DESC, u.id ASC LIMIT 20`),
  ]);
  const map = (rows, extra = () => ({})) => rows.map((row, index) => ({
    rank: index + 1,
    id: row.id,
    name: row.name,
    score: Number(row.score || 0),
    ...extra(row),
  }));
  return {
    finished: map(finishedRows[0], (row) => ({ minutes: Number(row.minutes || 0) })),
    minutes: map(minutesRows[0], (row) => ({ finished: Number(row.finished || 0) })),
    active: map(activeRows[0]),
  };
}

export async function getAdminAnalytics() {
  const [[sales], [topBooks], [activity], [secondhand], [clubs]] = await Promise.all([
    query(`SELECT DATE_FORMAT(created_at, '%Y-%m') AS month, COUNT(*) AS orders,
      COALESCE(SUM(total_amount), 0) AS revenue FROM orders WHERE status <> '已取消'
      GROUP BY month ORDER BY month DESC LIMIT 6`),
    query(`SELECT b.title, SUM(oi.quantity) AS quantity FROM order_items oi JOIN books b ON b.id = oi.book_id
      JOIN orders o ON o.id = oi.order_id WHERE o.status <> '已取消' GROUP BY b.id ORDER BY quantity DESC LIMIT 5`),
    query(`SELECT DATE_FORMAT(viewed_at, '%Y-%m') AS month, COUNT(*) AS views FROM reading_history GROUP BY month ORDER BY month DESC LIMIT 6`),
    query("SELECT status, COUNT(*) AS count FROM secondhand_listings GROUP BY status"),
    query("SELECT COUNT(*) AS count FROM book_clubs WHERE status = 'published'")
  ]);
  return {
    sales: sales.map((row) => ({ month: row.month, orders: Number(row.orders), revenue: Number(row.revenue) })),
    topBooks: topBooks.map((row) => ({ title: row.title, quantity: Number(row.quantity) })),
    activity: activity.map((row) => ({ month: row.month, views: Number(row.views) })),
    secondhand: secondhand.map((row) => ({ status: row.status, count: Number(row.count) })),
    clubs: Number(clubs[0]?.count || 0),
  };
}

export async function seedNewModules() {
  const [listingCount] = await query("SELECT COUNT(*) AS count FROM secondhand_listings");
  if (Number(listingCount[0]?.count || 0) === 0) {
    await query(`INSERT INTO secondhand_listings (seller_id, title, author, condition_label, price, description, cover, status)
      VALUES (1, '人间草木', '汪曾祺', '九成新', 18, '少量铅笔划线，适合想从日常重新认识生活的读者。', 'https://images.unsplash.com/photo-1518373714866-3f1478910cc0?auto=format&fit=crop&w=620&q=85', 'active')`);
  }
  const [clubCount] = await query("SELECT COUNT(*) AS count FROM book_clubs");
  if (Number(clubCount[0]?.count || 0) === 0) {
    await query(`INSERT INTO book_clubs (title, description, book_id, host_id, event_at, location, capacity, status)
      VALUES ('《观看之道》周末共读', '从广告、电影和社交媒体里的图像聊起，带着一个例子来参加。', 6, 2, DATE_ADD(NOW(), INTERVAL 5 DAY), '线上会议室', 30, 'published')`);
  }
  const [challengeCount] = await query("SELECT COUNT(*) AS count FROM reading_challenges");
  if (Number(challengeCount[0]?.count || 0) === 0) {
    await query(`INSERT INTO reading_challenges (title, description, target_books, target_minutes, start_date, end_date, status)
      VALUES ('秋日三本书', '在一个月里读完三本书，给生活留出稳定的阅读时间。', 3, 600, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY), 'published')`);
  }
  const [notificationCount] = await query("SELECT COUNT(*) AS count FROM notifications WHERE user_id = 1");
  if (Number(notificationCount[0]?.count || 0) === 0)
    await query("INSERT INTO notifications (user_id, type, content) VALUES (1, 'system', '欢迎来到纸灯消息中心，活动报名、评论回复和订单状态都会在这里提醒你。')");
}
