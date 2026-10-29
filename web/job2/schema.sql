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
