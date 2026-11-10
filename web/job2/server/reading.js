import { query } from "./db.js";

const mapBook = (row) => ({
  id: row.id,
  title: row.title,
  author: row.author,
  categoryLabel: row.category_label,
  cover: row.cover,
  status: row.status,
  progress: Number(row.progress || 0),
  minutes: Number(row.minutes || 0),
  updatedAt: row.updated_at,
});

export async function getReadingData(userId) {
  const [[shelfRows], [historyRows], [statsRows]] = await Promise.all([
    query(
      `SELECT b.id, b.title, b.author, b.category_label, b.cover, s.status, s.progress, s.minutes, s.updated_at
      FROM reading_shelves s JOIN books b ON b.id = s.book_id WHERE s.user_id = ? ORDER BY s.updated_at DESC`,
      [userId],
    ),
    query(
      `SELECT h.id, h.viewed_at, b.id AS book_id, b.title, b.author, b.cover, b.category_label
      FROM reading_history h JOIN books b ON b.id = h.book_id WHERE h.user_id = ? ORDER BY h.viewed_at DESC LIMIT 12`,
      [userId],
    ),
    query(
      `SELECT COALESCE(SUM(minutes), 0) AS minutes, COUNT(DISTINCT book_id) AS books,
      SUM(CASE WHEN status = 'read' THEN 1 ELSE 0 END) AS finished FROM reading_shelves WHERE user_id = ?`,
      [userId],
    ),
  ]);
  const groups = { want: [], reading: [], read: [] };
  shelfRows.map(mapBook).forEach((book) => (groups[book.status] || groups.want).push(book));
  return {
    shelves: groups,
    history: historyRows.map((row) => ({
      id: row.id,
      bookId: row.book_id,
      title: row.title,
      author: row.author,
      cover: row.cover,
      categoryLabel: row.category_label,
      viewedAt: row.viewed_at,
    })),
    stats: {
      minutes: Number(statsRows[0]?.minutes || 0),
      books: Number(statsRows[0]?.books || 0),
      finished: Number(statsRows[0]?.finished || 0),
      year: new Date().getFullYear(),
    },
  };
}

export async function updateReading(userId, bookId, payload = {}) {
  const status = ["want", "reading", "read"].includes(payload.status) ? payload.status : "want";
  const progress = Math.max(
    0,
    Math.min(100, Number(payload.progress ?? (status === "read" ? 100 : 0))),
  );
  const minutes = Math.max(0, Number(payload.minutes || 0));
  await query(
    `INSERT INTO reading_shelves (user_id, book_id, status, progress, minutes) VALUES (?, ?, ?, ?, ?)
    ON DUPLICATE KEY UPDATE status = VALUES(status), progress = VALUES(progress), minutes = minutes + VALUES(minutes)`,
    [userId, bookId, status, progress, minutes],
  );
  return getReadingData(userId);
}

export async function recordReadingHistory(userId, bookId) {
  await query("INSERT INTO reading_history (user_id, book_id) VALUES (?, ?)", [userId, bookId]);
  await query(
    "DELETE h1 FROM reading_history h1 JOIN reading_history h2 ON h1.user_id = h2.user_id AND h1.book_id = h2.book_id AND h1.id < h2.id WHERE h1.user_id = ?",
    [userId],
  );
  return getReadingData(userId);
}
