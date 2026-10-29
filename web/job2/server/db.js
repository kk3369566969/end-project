import mysql from 'mysql2/promise'
import dotenv from 'dotenv'
import { books as fallbackBooks } from './data.js'

dotenv.config()

let pool
let usingFallback = true

export async function initDatabase() {
  try {
    pool = mysql.createPool({
      host: process.env.DB_HOST || 'localhost',
      port: Number(process.env.DB_PORT || 3306),
      user: process.env.DB_USER || 'root',
      password: process.env.DB_PASSWORD || '1234',
      database: process.env.DB_NAME || 'bookstore',
      waitForConnections: true,
      connectionLimit: 8,
      connectTimeout: 1200
    })
    await pool.query('SELECT 1')
    usingFallback = false
    console.log('MySQL connected')
  } catch (error) {
    usingFallback = true
    console.warn('MySQL unavailable, using demo data. Run schema.sql to enable persistence.')
  }
}

export function isFallback() { return usingFallback }

export async function listBooks({ category, search, sort }) {
  if (usingFallback) {
    let result = [...fallbackBooks]
    if (category && category !== 'all') result = result.filter(book => book.category === category)
    if (search) {
      const keyword = search.toLowerCase()
      result = result.filter(book => `${book.title}${book.author}${book.description}`.toLowerCase().includes(keyword))
    }
    if (sort === 'price-asc') result.sort((a, b) => a.price - b.price)
    if (sort === 'price-desc') result.sort((a, b) => b.price - a.price)
    if (sort === 'rating') result.sort((a, b) => b.rating - a.rating)
    return result
  }
  const filters = []
  const values = []
  if (category && category !== 'all') { filters.push('category = ?'); values.push(category) }
  if (search) { filters.push('(title LIKE ? OR author LIKE ? OR description LIKE ?)'); values.push(`%${search}%`, `%${search}%`, `%${search}%`) }
  const order = sort === 'price-asc' ? 'price ASC' : sort === 'price-desc' ? 'price DESC' : sort === 'rating' ? 'rating DESC' : 'created_at DESC'
  const [rows] = await pool.query(`SELECT id, title, author, category, category_label AS categoryLabel, price, original_price AS originalPrice, rating, stock, badge, description, cover, tone FROM books ${filters.length ? `WHERE ${filters.join(' AND ')}` : ''} ORDER BY ${order}`, values)
  return rows
}

export async function createOrder({ customer, items, total }) {
  if (usingFallback) return { id: `PL-${Date.now().toString().slice(-6)}`, status: '待发货', total, createdAt: new Date().toISOString() }
  const connection = await pool.getConnection()
  try {
    await connection.beginTransaction()
    const [order] = await connection.query('INSERT INTO orders (customer_name, customer_phone, customer_address, total_amount, status) VALUES (?, ?, ?, ?, ?)', [customer.name, customer.phone, customer.address, total, '待发货'])
    for (const item of items) await connection.query('INSERT INTO order_items (order_id, book_id, quantity, unit_price) VALUES (?, ?, ?, ?)', [order.insertId, item.id, item.quantity, item.price])
    await connection.commit()
    return { id: order.insertId, status: '待发货', total, createdAt: new Date().toISOString() }
  } catch (error) {
    await connection.rollback()
    throw error
  } finally { connection.release() }
}
