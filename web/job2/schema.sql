CREATE DATABASE IF NOT EXISTS bookstore DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bookstore;

CREATE TABLE IF NOT EXISTS books (
  id INT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(120) NOT NULL,
  author VARCHAR(80) NOT NULL,
  category VARCHAR(40) NOT NULL,
  category_label VARCHAR(40) NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  original_price DECIMAL(10,2) NOT NULL,
  rating DECIMAL(2,1) DEFAULT 0,
  stock INT DEFAULT 0,
  badge VARCHAR(40),
  description TEXT,
  cover VARCHAR(500),
  cover_data MEDIUMTEXT,
  inventory_status VARCHAR(20) NOT NULL DEFAULT 'active',
  tone VARCHAR(30),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_name VARCHAR(80) NOT NULL,
  customer_phone VARCHAR(30) NOT NULL,
  customer_address VARCHAR(255) NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT '待发货',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  book_id INT NOT NULL,
  quantity INT NOT NULL,
  unit_price DECIMAL(10,2) NOT NULL,
  CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_order_items_book FOREIGN KEY (book_id) REFERENCES books(id)
);

INSERT INTO books (title, author, category, category_label, price, original_price, rating, stock, badge, description, cover, tone)
SELECT '夜航西飞', '柏瑞尔·马卡姆', 'literature', '文学小说', 42, 56, 4.9, 24, '编辑推荐', '一部写给夜行者的非洲航空回忆录，关于风、土地与自由。', 'https://images.unsplash.com/photo-1543002588-bfa74002ed7e?auto=format&fit=crop&w=620&q=85', 'ochre'
WHERE NOT EXISTS (SELECT 1 FROM books WHERE title = '夜航西飞');
INSERT INTO books (title, author, category, category_label, price, original_price, rating, stock, badge, description, cover, tone)
SELECT '山茶文具店', '小川糸', 'life', '生活方式', 39, 48, 4.8, 18, '治愈书单', '用一封封手写信，重新练习好好告别与认真生活。', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=620&q=85', 'sage'
WHERE NOT EXISTS (SELECT 1 FROM books WHERE title = '山茶文具店');
INSERT INTO books (title, author, category, category_label, price, original_price, rating, stock, badge, description, cover, tone)
SELECT '设计中的设计', '原研哉', 'design', '设计与艺术', 76, 98, 4.9, 9, '设计必读', '从日常物件出发，重新理解“无意识设计”的力量。', 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&w=620&q=85', 'ink'
WHERE NOT EXISTS (SELECT 1 FROM books WHERE title = '设计中的设计');
INSERT INTO books (title, author, category, category_label, price, original_price, rating, stock, badge, description, cover, tone)
SELECT '人间草木', '汪曾祺', 'literature', '文学小说', 32, 42, 4.8, 31, '常读常新', '在草木、食物与旧事之间，读一个温厚而有趣的人间。', 'https://images.unsplash.com/photo-1518373714866-3f1478910cc0?auto=format&fit=crop&w=620&q=85', 'clay'
WHERE NOT EXISTS (SELECT 1 FROM books WHERE title = '人间草木');
INSERT INTO books (title, author, category, category_label, price, original_price, rating, stock, badge, description, cover, tone)
SELECT '东京八平米', '吉井忍', 'life', '生活方式', 45, 59, 4.7, 15, '城市漫游', '一间八平米的房间，收纳着一个年轻人的城市生活哲学。', 'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?auto=format&fit=crop&w=620&q=85', 'blue'
WHERE NOT EXISTS (SELECT 1 FROM books WHERE title = '东京八平米');
INSERT INTO books (title, author, category, category_label, price, original_price, rating, stock, badge, description, cover, tone)
SELECT '观看之道', '约翰·伯格', 'design', '设计与艺术', 53, 68, 4.9, 12, '视觉启蒙', '图像如何被看见，也如何反过来塑造我们看世界的方式。', 'https://images.unsplash.com/photo-1511108690759-009324a90311?auto=format&fit=crop&w=620&q=85', 'rose'
WHERE NOT EXISTS (SELECT 1 FROM books WHERE title = '观看之道');
INSERT INTO books (title, author, category, category_label, price, original_price, rating, stock, badge, description, cover, tone)
SELECT '置身事内', '兰小欢', 'social', '社会观察', 49, 65, 4.8, 20, '年度社科', '理解中国经济运行的一扇窗口，写给每个身处其中的人。', 'https://images.unsplash.com/photo-1532012197267-da84d127e765?auto=format&fit=crop&w=620&q=85', 'violet'
WHERE NOT EXISTS (SELECT 1 FROM books WHERE title = '置身事内');
INSERT INTO books (title, author, category, category_label, price, original_price, rating, stock, badge, description, cover, tone)
SELECT '月亮与六便士', '毛姆', 'literature', '文学小说', 36, 46, 4.8, 27, '经典新译', '当一个人决定追随内心，世俗的秩序会如何回应。', 'https://images.unsplash.com/photo-1476275466078-4007374efbbe?auto=format&fit=crop&w=620&q=85', 'green'
WHERE NOT EXISTS (SELECT 1 FROM books WHERE title = '月亮与六便士');

CREATE TABLE IF NOT EXISTS book_reviews (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  book_id INT NOT NULL,
  user_name VARCHAR(80) NOT NULL,
  rating TINYINT NOT NULL,
  content TEXT NOT NULL,
  helpful_count INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_reviews_book FOREIGN KEY (book_id) REFERENCES books(id)
);

CREATE TABLE IF NOT EXISTS forum_posts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(180) NOT NULL,
  excerpt TEXT NOT NULL,
  category VARCHAR(40) NOT NULL,
  author_name VARCHAR(80) NOT NULL,
  likes INT DEFAULT 0,
  comments INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS forum_comments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  post_id BIGINT NOT NULL,
  user_name VARCHAR(80) NOT NULL,
  content TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_forum_comments_post FOREIGN KEY (post_id) REFERENCES forum_posts(id)
);

CREATE TABLE IF NOT EXISTS user_addresses (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL DEFAULT 1,
  label VARCHAR(30) NOT NULL,
  receiver VARCHAR(80) NOT NULL,
  phone VARCHAR(30) NOT NULL,
  detail VARCHAR(255) NOT NULL,
  is_default BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS user_favorites (
  user_id BIGINT NOT NULL DEFAULT 1,
  book_id INT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, book_id),
  CONSTRAINT fk_favorites_book FOREIGN KEY (book_id) REFERENCES books(id)
);

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(80) NOT NULL,
  email VARCHAR(160) NOT NULL UNIQUE,
  password_hash VARCHAR(220) NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'reader',
  bio VARCHAR(255) DEFAULT '',
  points INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_sessions (
  token VARCHAR(128) PRIMARY KEY,
  user_id BIGINT NOT NULL,
  expires_at DATETIME NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

SET @alter_sql = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'user_id') = 0, 'ALTER TABLE orders ADD COLUMN user_id BIGINT NULL', 'SELECT 1');
PREPARE alter_stmt FROM @alter_sql;
EXECUTE alter_stmt;
DEALLOCATE PREPARE alter_stmt;
SET @alter_sql = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'book_reviews' AND COLUMN_NAME = 'user_id') = 0, 'ALTER TABLE book_reviews ADD COLUMN user_id BIGINT NULL', 'SELECT 1');
PREPARE alter_stmt FROM @alter_sql;
EXECUTE alter_stmt;
DEALLOCATE PREPARE alter_stmt;
SET @alter_sql = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'forum_posts' AND COLUMN_NAME = 'user_id') = 0, 'ALTER TABLE forum_posts ADD COLUMN user_id BIGINT NULL', 'SELECT 1');
PREPARE alter_stmt FROM @alter_sql;
EXECUTE alter_stmt;
DEALLOCATE PREPARE alter_stmt;
SET @alter_sql = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'forum_posts' AND COLUMN_NAME = 'tags') = 0, 'ALTER TABLE forum_posts ADD COLUMN tags TEXT', 'SELECT 1');
PREPARE alter_stmt FROM @alter_sql;
EXECUTE alter_stmt;
DEALLOCATE PREPARE alter_stmt;
SET @alter_sql = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'forum_comments' AND COLUMN_NAME = 'user_id') = 0, 'ALTER TABLE forum_comments ADD COLUMN user_id BIGINT NULL', 'SELECT 1');
PREPARE alter_stmt FROM @alter_sql;
EXECUTE alter_stmt;
DEALLOCATE PREPARE alter_stmt;

INSERT INTO users (id, name, email, password_hash, role, bio, points)
SELECT 1, '周末读者', 'reader@paperlantern.book', 'scrypt:40e0d8ada845a8af36f5d34d198532c7:c24e5791f9f11d2a9571106c3cd171421fef5e68f7105c834f0e6907fbbb6d37757bf0d8342feec8b6db0c4c9772fc32659e6260c884691900872392cd0101bd', 'reader', '在杭州生活，喜欢散文、城市漫游和有一点点难度的书。', 368
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'reader@paperlantern.book');
INSERT INTO users (id, name, email, password_hash, role, bio, points)
SELECT 2, '纸灯管理员', 'admin@paperlantern.book', 'scrypt:0d2eaeecb8e01287bb7993884e845023:c199cfd67c10afd5e5c3acb7c61682700e050bdac2baaaf36078eace415edcdd6d707dadf0e6823b70ec646aea1b58a13dec3236ae42b31fe225398d2d15696c', 'admin', '维护纸灯书店与书友社区。', 0
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@paperlantern.book');

INSERT INTO user_addresses (user_id, label, receiver, phone, detail, is_default)
SELECT 1, '家', '周末读者', '138****0000', '浙江省杭州市西湖区纸灯路 1 号', 1
WHERE NOT EXISTS (SELECT 1 FROM user_addresses WHERE user_id = 1);
INSERT INTO user_favorites (user_id, book_id)
SELECT 1, 2 WHERE NOT EXISTS (SELECT 1 FROM user_favorites WHERE user_id = 1 AND book_id = 2);
INSERT INTO user_favorites (user_id, book_id)
SELECT 1, 6 WHERE NOT EXISTS (SELECT 1 FROM user_favorites WHERE user_id = 1 AND book_id = 6);

INSERT INTO forum_posts (user_id, title, excerpt, category, author_name, likes, comments, tags)
SELECT 1, '有没有读完之后，真的改变生活的书？', '最近在整理年度书单，想听听大家有没有那种读完之后，生活方式悄悄发生变化的书。文学、社科、工具书都可以。', '阅读分享', '林间有风', 38, 12, '["年度书单","生活方式"]'
WHERE NOT EXISTS (SELECT 1 FROM forum_posts WHERE title = '有没有读完之后，真的改变生活的书？');
INSERT INTO forum_posts (user_id, title, excerpt, category, author_name, likes, comments, tags)
SELECT 2, '《观看之道》读书会：我们如何被图像影响？', '准备在周六晚上做一场线上共读，想从广告、电影和社交媒体里的图像聊起，欢迎带着自己的例子来。', '同城活动', '纸灯管理员', 24, 8, '["设计","共读"]'
WHERE NOT EXISTS (SELECT 1 FROM forum_posts WHERE title = '《观看之道》读书会：我们如何被图像影响？');
INSERT INTO forum_posts (user_id, title, excerpt, category, author_name, likes, comments, tags)
SELECT 1, '给刚开始读毛姆的朋友：先从哪一本开始？', '最近收到很多关于毛姆的提问。把我读过的几本简单排了顺序，也想看看大家心中的入坑作是哪本。', '书目讨论', '周末读者', 56, 19, '["毛姆","入门"]'
WHERE NOT EXISTS (SELECT 1 FROM forum_posts WHERE title = '给刚开始读毛姆的朋友：先从哪一本开始？');

INSERT INTO book_reviews (book_id, user_id, user_name, rating, content, helpful_count)
SELECT 1, 1, '周末读者', 5, '像坐在一架小飞机里，往下看见时间和土地。读完很想去远方。', 16
WHERE NOT EXISTS (SELECT 1 FROM book_reviews WHERE book_id = 1);
INSERT INTO book_reviews (book_id, user_id, user_name, rating, content, helpful_count)
SELECT 3, 1, '周末读者', 5, '设计师的案头书，重新看见生活里那些被忽略的细节。', 22
WHERE NOT EXISTS (SELECT 1 FROM book_reviews WHERE book_id = 3);

-- 书籍详情、阅读书架与社区互动扩展
SET @alter_sql = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'books' AND COLUMN_NAME = 'isbn') = 0, 'ALTER TABLE books ADD COLUMN isbn VARCHAR(32) NULL, ADD COLUMN publisher VARCHAR(120) NULL, ADD COLUMN published_at DATE NULL, ADD COLUMN author_bio TEXT NULL, ADD COLUMN toc TEXT NULL, ADD COLUMN excerpt TEXT NULL', 'SELECT 1');
PREPARE alter_stmt FROM @alter_sql;
EXECUTE alter_stmt;
DEALLOCATE PREPARE alter_stmt;
SET @alter_sql = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'forum_posts' AND COLUMN_NAME = 'body_markdown') = 0, 'ALTER TABLE forum_posts ADD COLUMN body_markdown TEXT NULL, ADD COLUMN image_url VARCHAR(500) NULL, ADD COLUMN image_data MEDIUMTEXT NULL, ADD COLUMN pinned BOOLEAN NOT NULL DEFAULT FALSE, ADD COLUMN report_count INT NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE alter_stmt FROM @alter_sql;
EXECUTE alter_stmt;
DEALLOCATE PREPARE alter_stmt;
SET @alter_sql = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'forum_posts' AND COLUMN_NAME = 'image_data') = 0, 'ALTER TABLE forum_posts ADD COLUMN image_data MEDIUMTEXT NULL', 'SELECT 1');
PREPARE alter_stmt FROM @alter_sql;
EXECUTE alter_stmt;
DEALLOCATE PREPARE alter_stmt;
SET @alter_sql = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'forum_comments' AND COLUMN_NAME = 'likes') = 0, 'ALTER TABLE forum_comments ADD COLUMN likes INT NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE alter_stmt FROM @alter_sql;
EXECUTE alter_stmt;
DEALLOCATE PREPARE alter_stmt;

CREATE TABLE IF NOT EXISTS reading_shelves (
  user_id BIGINT NOT NULL, book_id INT NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'want', progress INT NOT NULL DEFAULT 0, minutes INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, book_id), CONSTRAINT fk_reading_shelf_book FOREIGN KEY (book_id) REFERENCES books(id)
);
CREATE TABLE IF NOT EXISTS reading_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, book_id INT NOT NULL, viewed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_reading_history_user (user_id, viewed_at), CONSTRAINT fk_reading_history_book FOREIGN KEY (book_id) REFERENCES books(id)
);
CREATE TABLE IF NOT EXISTS forum_post_likes (user_id BIGINT NOT NULL, post_id BIGINT NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (user_id, post_id), CONSTRAINT fk_post_like_post FOREIGN KEY (post_id) REFERENCES forum_posts(id) ON DELETE CASCADE);
CREATE TABLE IF NOT EXISTS review_likes (user_id BIGINT NOT NULL, review_id BIGINT NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (user_id, review_id), CONSTRAINT fk_review_like_review FOREIGN KEY (review_id) REFERENCES book_reviews(id) ON DELETE CASCADE);
CREATE TABLE IF NOT EXISTS forum_comment_likes (user_id BIGINT NOT NULL, comment_id BIGINT NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (user_id, comment_id), CONSTRAINT fk_comment_like_comment FOREIGN KEY (comment_id) REFERENCES forum_comments(id) ON DELETE CASCADE);
CREATE TABLE IF NOT EXISTS user_follows (follower_id BIGINT NOT NULL, following_id BIGINT NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (follower_id, following_id));
CREATE TABLE IF NOT EXISTS forum_reports (id BIGINT PRIMARY KEY AUTO_INCREMENT, post_id BIGINT NOT NULL, user_id BIGINT NOT NULL, reason VARCHAR(255) NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, UNIQUE KEY uq_forum_report (post_id, user_id), CONSTRAINT fk_report_post FOREIGN KEY (post_id) REFERENCES forum_posts(id) ON DELETE CASCADE);
CREATE TABLE IF NOT EXISTS review_comments (id BIGINT PRIMARY KEY AUTO_INCREMENT, review_id BIGINT NOT NULL, user_id BIGINT NOT NULL, content TEXT NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, CONSTRAINT fk_review_comment_review FOREIGN KEY (review_id) REFERENCES book_reviews(id) ON DELETE CASCADE);
CREATE TABLE IF NOT EXISTS notifications (id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, type VARCHAR(40) NOT NULL, content VARCHAR(255) NOT NULL, is_read BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, INDEX idx_notifications_user (user_id, created_at));

CREATE TABLE IF NOT EXISTS inventory_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  book_id INT NOT NULL,
  admin_id BIGINT NOT NULL,
  delta INT NOT NULL,
  stock_after INT NOT NULL,
  reason VARCHAR(180) NOT NULL DEFAULT '库存调整',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_inventory_book (book_id, created_at),
  CONSTRAINT fk_inventory_book FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
  CONSTRAINT fk_inventory_admin FOREIGN KEY (admin_id) REFERENCES users(id)
);

-- 横向业务模块：二手书、读书会、阅读挑战
CREATE TABLE IF NOT EXISTS secondhand_listings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  seller_id BIGINT NOT NULL,
  buyer_id BIGINT NULL,
  title VARCHAR(160) NOT NULL,
  author VARCHAR(120) NOT NULL,
  condition_label VARCHAR(30) NOT NULL,
  price DECIMAL(10,2) NOT NULL DEFAULT 0,
  description TEXT,
  cover VARCHAR(500),
  cover_data MEDIUMTEXT,
  status VARCHAR(20) NOT NULL DEFAULT 'active',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_secondhand_status (status),
  CONSTRAINT fk_secondhand_seller FOREIGN KEY (seller_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS book_clubs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(180) NOT NULL,
  description TEXT,
  book_id INT NULL,
  host_id BIGINT NOT NULL,
  event_at DATETIME NOT NULL,
  location VARCHAR(160) DEFAULT '线上',
  capacity INT NOT NULL DEFAULT 20,
  image_data MEDIUMTEXT,
  status VARCHAR(20) NOT NULL DEFAULT 'published',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_club_book FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE SET NULL,
  CONSTRAINT fk_club_host FOREIGN KEY (host_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS club_members (
  club_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'joined',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (club_id, user_id),
  CONSTRAINT fk_club_member_club FOREIGN KEY (club_id) REFERENCES book_clubs(id) ON DELETE CASCADE,
  CONSTRAINT fk_club_member_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS reading_challenges (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(160) NOT NULL,
  description TEXT,
  target_books INT NOT NULL DEFAULT 0,
  target_minutes INT NOT NULL DEFAULT 0,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  image_data MEDIUMTEXT,
  status VARCHAR(20) NOT NULL DEFAULT 'published',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS challenge_progress (
  challenge_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  books_completed INT NOT NULL DEFAULT 0,
  minutes INT NOT NULL DEFAULT 0,
  joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (challenge_id, user_id),
  CONSTRAINT fk_challenge_progress_challenge FOREIGN KEY (challenge_id) REFERENCES reading_challenges(id) ON DELETE CASCADE,
  CONSTRAINT fk_challenge_progress_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
