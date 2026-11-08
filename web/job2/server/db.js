import mysql from "mysql2/promise";
import dotenv from "dotenv";

dotenv.config();

let pool;
let usingFallback = true;

export async function initDatabase() {
  try {
    pool = mysql.createPool({
      host: process.env.DB_HOST || "localhost",
      port: Number(process.env.DB_PORT || 3306),
      user: process.env.DB_USER || "root",
      password: process.env.DB_PASSWORD || "1234",
      database: process.env.DB_NAME || "bookstore",
      waitForConnections: true,
      connectionLimit: 8,
      connectTimeout: 1200,
    });
    await pool.query("SELECT 1");
    await ensureFeatureSchema();
    usingFallback = false;
    console.log("MySQL connected");
  } catch (error) {
    usingFallback = true;
    console.warn("MySQL unavailable, using demo data. Run schema.sql to enable persistence.");
  }
}

async function addColumnIfMissing(table, column, definition) {
  const [rows] = await pool.query(
    "SELECT COUNT(*) AS count FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
    [table, column],
  );
  if (!Number(rows[0]?.count))
    await pool.query(`ALTER TABLE \`${table}\` ADD COLUMN \`${column}\` ${definition}`);
}

async function ensureFeatureSchema() {
  const bookColumns = [
    ["cover_data", "MEDIUMTEXT NULL"],
    ["inventory_status", "VARCHAR(20) NOT NULL DEFAULT 'active'"],
    ["isbn", "VARCHAR(32) NULL"],
    ["publisher", "VARCHAR(120) NULL"],
    ["published_at", "DATE NULL"],
    ["author_bio", "TEXT NULL"],
    ["toc", "TEXT NULL"],
    ["excerpt", "TEXT NULL"],
  ];
  for (const [column, definition] of bookColumns) await addColumnIfMissing("books", column, definition);
  for (const [column, definition] of [
    ["body_markdown", "TEXT NULL"],
    ["image_url", "VARCHAR(500) NULL"],
    ["image_data", "MEDIUMTEXT NULL"],
    ["pinned", "BOOLEAN NOT NULL DEFAULT FALSE"],
    ["report_count", "INT NOT NULL DEFAULT 0"],
  ]) await addColumnIfMissing("forum_posts", column, definition);
  await addColumnIfMissing("forum_comments", "likes", "INT NOT NULL DEFAULT 0");
  await pool.query(`CREATE TABLE IF NOT EXISTS reading_shelves (
    user_id BIGINT NOT NULL, book_id INT NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'want',
    progress INT NOT NULL DEFAULT 0, minutes INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, book_id), CONSTRAINT fk_reading_shelf_book FOREIGN KEY (book_id) REFERENCES books(id)
  )`);
  await pool.query(`CREATE TABLE IF NOT EXISTS reading_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, book_id INT NOT NULL,
    viewed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, INDEX idx_reading_history_user (user_id, viewed_at),
    CONSTRAINT fk_reading_history_book FOREIGN KEY (book_id) REFERENCES books(id)
  )`);
  await pool.query(`CREATE TABLE IF NOT EXISTS forum_post_likes (
    user_id BIGINT NOT NULL, post_id BIGINT NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, post_id), CONSTRAINT fk_post_like_post FOREIGN KEY (post_id) REFERENCES forum_posts(id) ON DELETE CASCADE
  )`);
  await pool.query(`CREATE TABLE IF NOT EXISTS review_likes (
    user_id BIGINT NOT NULL, review_id BIGINT NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, review_id), CONSTRAINT fk_review_like_review FOREIGN KEY (review_id) REFERENCES book_reviews(id) ON DELETE CASCADE
  )`);
  await pool.query(`CREATE TABLE IF NOT EXISTS forum_comment_likes (
    user_id BIGINT NOT NULL, comment_id BIGINT NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, comment_id), CONSTRAINT fk_comment_like_comment FOREIGN KEY (comment_id) REFERENCES forum_comments(id) ON DELETE CASCADE
  )`);
  await pool.query(`CREATE TABLE IF NOT EXISTS user_follows (
    follower_id BIGINT NOT NULL, following_id BIGINT NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (follower_id, following_id)
  )`);
  await pool.query(`CREATE TABLE IF NOT EXISTS forum_reports (
    id BIGINT PRIMARY KEY AUTO_INCREMENT, post_id BIGINT NOT NULL, user_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_forum_report (post_id, user_id), CONSTRAINT fk_report_post FOREIGN KEY (post_id) REFERENCES forum_posts(id) ON DELETE CASCADE
  )`);
  await pool.query(`CREATE TABLE IF NOT EXISTS review_comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT, review_id BIGINT NOT NULL, user_id BIGINT NOT NULL,
    content TEXT NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_comment_review FOREIGN KEY (review_id) REFERENCES book_reviews(id) ON DELETE CASCADE
  )`);
  await pool.query(`CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, type VARCHAR(40) NOT NULL,
    content VARCHAR(255) NOT NULL, is_read BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notifications_user (user_id, created_at)
  )`);
  await pool.query(`CREATE TABLE IF NOT EXISTS inventory_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT, book_id INT NOT NULL, admin_id BIGINT NOT NULL,
    delta INT NOT NULL, stock_after INT NOT NULL, reason VARCHAR(180) NOT NULL DEFAULT '库存调整',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_inventory_book (book_id, created_at),
    CONSTRAINT fk_inventory_book FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_admin FOREIGN KEY (admin_id) REFERENCES users(id)
  )`);
  await pool.query(`CREATE TABLE IF NOT EXISTS secondhand_listings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT, seller_id BIGINT NOT NULL, buyer_id BIGINT NULL,
    title VARCHAR(160) NOT NULL, author VARCHAR(120) NOT NULL, condition_label VARCHAR(30) NOT NULL,
    price DECIMAL(10,2) NOT NULL DEFAULT 0, description TEXT, cover VARCHAR(500), cover_data MEDIUMTEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'active', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_secondhand_status (status), CONSTRAINT fk_secondhand_seller FOREIGN KEY (seller_id) REFERENCES users(id)
  )`);
  await addColumnIfMissing("secondhand_listings", "cover_data", "MEDIUMTEXT NULL");
  await pool.query(`CREATE TABLE IF NOT EXISTS book_clubs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT, title VARCHAR(180) NOT NULL, description TEXT,
    book_id INT NULL, host_id BIGINT NOT NULL, event_at DATETIME NOT NULL, location VARCHAR(160) DEFAULT '线上',
    capacity INT NOT NULL DEFAULT 20, image_data MEDIUMTEXT, status VARCHAR(20) NOT NULL DEFAULT 'published', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_club_book FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE SET NULL,
    CONSTRAINT fk_club_host FOREIGN KEY (host_id) REFERENCES users(id)
  )`);
  await addColumnIfMissing("book_clubs", "image_data", "MEDIUMTEXT NULL");
  await pool.query(`CREATE TABLE IF NOT EXISTS club_members (
    club_id BIGINT NOT NULL, user_id BIGINT NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'joined',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (club_id, user_id),
    CONSTRAINT fk_club_member_club FOREIGN KEY (club_id) REFERENCES book_clubs(id) ON DELETE CASCADE,
    CONSTRAINT fk_club_member_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
  )`);
  await pool.query(`CREATE TABLE IF NOT EXISTS reading_challenges (
    id BIGINT PRIMARY KEY AUTO_INCREMENT, title VARCHAR(160) NOT NULL, description TEXT,
    target_books INT NOT NULL DEFAULT 0, target_minutes INT NOT NULL DEFAULT 0,
    start_date DATE NOT NULL, end_date DATE NOT NULL, image_data MEDIUMTEXT, status VARCHAR(20) NOT NULL DEFAULT 'published',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  )`);
  await addColumnIfMissing("reading_challenges", "image_data", "MEDIUMTEXT NULL");
  await pool.query(`CREATE TABLE IF NOT EXISTS challenge_progress (
    challenge_id BIGINT NOT NULL, user_id BIGINT NOT NULL, books_completed INT NOT NULL DEFAULT 0,
    minutes INT NOT NULL DEFAULT 0, joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (challenge_id, user_id), CONSTRAINT fk_challenge_progress_challenge FOREIGN KEY (challenge_id) REFERENCES reading_challenges(id) ON DELETE CASCADE,
    CONSTRAINT fk_challenge_progress_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
  )`);
  await pool.query(`UPDATE books SET
    isbn = CASE title
      WHEN '夜航西飞' THEN '978-7532792842' WHEN '山茶文具店' THEN '978-7532783124'
      WHEN '设计中的设计' THEN '978-7532750636' WHEN '人间草木' THEN '978-7530218346'
      WHEN '东京八平米' THEN '978-7532176903' WHEN '观看之道' THEN '978-7532759660'
      WHEN '置身事内' THEN '978-7542673802' WHEN '月亮与六便士' THEN '978-7532788818'
      ELSE CONCAT('978-7', LPAD(id, 9, '0')) END,
    publisher = CASE title
      WHEN '夜航西飞' THEN '上海人民出版社' WHEN '山茶文具店' THEN '湖南文艺出版社'
      WHEN '设计中的设计' THEN '山东人民出版社' WHEN '人间草木' THEN '北京十月文艺出版社'
      WHEN '东京八平米' THEN '上海译文出版社' WHEN '观看之道' THEN '广西师范大学出版社'
      WHEN '置身事内' THEN '上海三联书店' WHEN '月亮与六便士' THEN '浙江文艺出版社'
      ELSE '纸灯书局' END,
    published_at = CASE title
      WHEN '夜航西飞' THEN '2022-04-01' WHEN '山茶文具店' THEN '2018-12-01'
      WHEN '设计中的设计' THEN '2018-07-01' WHEN '人间草木' THEN '2017-06-01'
      WHEN '东京八平米' THEN '2020-03-01' WHEN '观看之道' THEN '2015-11-01'
      WHEN '置身事内' THEN '2021-10-01' WHEN '月亮与六便士' THEN '2018-01-01'
      ELSE CURRENT_DATE END,
    author_bio = CASE title
      WHEN '夜航西飞' THEN '柏瑞尔·马卡姆是飞行员、作家与探险者，以细腻而勇敢的文字记录非洲天空。'
      WHEN '山茶文具店' THEN '小川糸擅长从日常生活中写出温柔的关系与细微的告别。'
      WHEN '设计中的设计' THEN '原研哉是日本设计师，长期关注日常物品与感知经验。'
      WHEN '人间草木' THEN '汪曾祺以平实温厚的笔触写食物、草木与人情。'
      WHEN '东京八平米' THEN '吉井忍长期观察城市生活，关注普通人的居住与迁徙。'
      WHEN '观看之道' THEN '约翰·伯格是英国艺术评论家，善于拆解图像背后的权力与观看方式。'
      WHEN '置身事内' THEN '兰小欢以清晰的经济学视角解释公共部门与市场的运行逻辑。'
      WHEN '月亮与六便士' THEN '毛姆是英国小说家，作品常在世俗秩序与精神追求之间寻找张力。'
      ELSE '一位值得被更多读者发现的作者。' END,
    toc = CASE title
      WHEN '夜航西飞' THEN '起飞｜非洲的风｜穿越夜色｜抵达远方'
      WHEN '山茶文具店' THEN '代笔｜告别｜重逢｜一封写给自己的信'
      WHEN '设计中的设计' THEN '无意识设计｜白｜觉知｜设计的未来'
      WHEN '人间草木' THEN '草木｜四时｜食味｜人间滋味'
      WHEN '东京八平米' THEN '一间房｜城市生活｜邻里｜搬家'
      WHEN '观看之道' THEN '观看与被观看｜油画｜广告｜视觉经验'
      WHEN '置身事内' THEN '地方政府｜土地财政｜产业升级｜理解现实'
      WHEN '月亮与六便士' THEN '离开伦敦｜追寻艺术｜塔希提｜代价'
      ELSE '序章｜第一章｜第二章｜尾声' END,
    excerpt = COALESCE(excerpt, description)
    WHERE isbn IS NULL OR isbn = ''`);
}

export function isFallback() {
  return usingFallback;
}

export async function query(sql, values = []) {
  if (usingFallback || !pool) throw new Error("MySQL database is unavailable");
  return pool.query(sql, values);
}

export async function listBooks({ category, search, sort }) {
  if (usingFallback) throw new Error("MySQL database is unavailable");
  const filters = [];
  const values = [];
  if (category && category !== "all") {
    filters.push("category = ?");
    values.push(category);
  }
  if (search) {
    filters.push("(title LIKE ? OR author LIKE ? OR description LIKE ?)");
    values.push(`%${search}%`, `%${search}%`, `%${search}%`);
  }
  const order =
    sort === "price-asc"
      ? "price ASC"
      : sort === "price-desc"
        ? "price DESC"
        : sort === "rating"
          ? "rating DESC"
          : "created_at DESC";
  const [rows] = await pool.query(
    `SELECT id, title, author, category, category_label AS categoryLabel, price, original_price AS originalPrice, rating, stock, badge, description, COALESCE(NULLIF(cover_data, ''), cover) AS cover, cover_data AS coverData, tone, isbn, publisher, published_at AS publishedAt, author_bio AS authorBio, toc, excerpt FROM books ${filters.length ? `WHERE ${filters.join(" AND ")}` : ""} ORDER BY ${order}`,
    values,
  );
  return rows;
}

export async function createOrder({ userId, customer, items, total }) {
  if (usingFallback || !pool) throw new Error("MySQL database is unavailable");
  const connection = await pool.getConnection();
  try {
    await connection.beginTransaction();
    const [order] = await connection.query(
      "INSERT INTO orders (user_id, customer_name, customer_phone, customer_address, total_amount, status) VALUES (?, ?, ?, ?, ?, ?)",
      [userId, customer.name, customer.phone, customer.address, total, "待发货"],
    );
    for (const item of items)
      await connection.query(
        "INSERT INTO order_items (order_id, book_id, quantity, unit_price) VALUES (?, ?, ?, ?)",
        [order.insertId, item.id, item.quantity, item.price],
      );
    await connection.query(
      "INSERT INTO notifications (user_id, type, content) VALUES (?, 'order', ?)",
      [userId, `订单 #${order.insertId} 已提交，当前状态：待发货`],
    );
    await connection.commit();
    return { id: order.insertId, status: "待发货", total, createdAt: new Date().toISOString() };
  } catch (error) {
    await connection.rollback();
    throw error;
  } finally {
    connection.release();
  }
}
