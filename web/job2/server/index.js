import express from 'express'
import cors from 'cors'
import dotenv from 'dotenv'
import { categories, books } from './data.js'
import { initDatabase, listBooks, createOrder, isFallback } from './db.js'
import { addAddress, addPostComment, addReview, createForumPost, getBookReviews, getForumPosts, getPostComments, getProfile, removeAddress, toggleFavorite, updateProfile } from './community.js'
import { getUserFromRequest, login, logout, register } from './auth.js'

dotenv.config()
const app = express()
const port = Number(process.env.PORT || 3001)
app.use(cors())
app.use(express.json())

app.get('/api/health', (_req, res) => res.json({ ok: true, mode: isFallback() ? 'demo' : 'mysql' }))
app.post('/api/auth/login', (req, res) => {
  const { email, password } = req.body || {}
  const result = login(email, password)
  result ? res.json(result) : res.status(401).json({ message: '邮箱或密码不正确，请检查后重试' })
})
app.post('/api/auth/register', (req, res) => {
  const { name, email, password } = req.body || {}
  if (!name?.trim() || !email?.trim() || !password || password.length < 6) return res.status(400).json({ message: '请填写完整信息，密码至少 6 位' })
  const result = register({ name, email, password })
  result?.error ? res.status(409).json({ message: result.error }) : res.status(201).json(result)
})
app.get('/api/auth/me', (req, res) => {
  const user = getUserFromRequest(req)
  user ? res.json({ user: { id: user.id, name: user.name, email: user.email, role: user.role, roleLabel: user.roleLabel, bio: user.bio } }) : res.status(401).json({ message: '未登录' })
})
app.post('/api/auth/logout', (req, res) => { logout(req.headers.authorization?.replace(/^Bearer\s+/i, '')); res.json({ ok: true }) })
app.get('/api/categories', (_req, res) => res.json(categories))
app.get('/api/books', async (req, res) => {
  try { res.json(await listBooks(req.query)) }
  catch (error) { res.status(500).json({ message: '书目加载失败', detail: error.message }) }
})
app.get('/api/books/:id', async (req, res) => {
  try {
    const result = await listBooks({ search: '' })
    const book = result.find(item => String(item.id) === req.params.id) || books.find(item => String(item.id) === req.params.id)
    book ? res.json(book) : res.status(404).json({ message: '找不到这本书' })
  } catch (error) { res.status(500).json({ message: '书目加载失败' }) }
})
app.post('/api/orders', async (req, res) => {
  const { customer, items, total } = req.body || {}
  if (!customer?.name || !customer?.phone || !customer?.address || !Array.isArray(items) || !items.length) return res.status(400).json({ message: '请完整填写收货信息并确认购物车' })
  try { res.status(201).json(await createOrder({ customer, items, total })) }
  catch (error) { res.status(500).json({ message: '订单创建失败', detail: error.message }) }
})
app.get('/api/books/:id/reviews', (req, res) => res.json(getBookReviews(req.params.id)))
app.post('/api/books/:id/reviews', (req, res) => {
  const { rating, content } = req.body || {}
  if (!rating || !content?.trim()) return res.status(400).json({ message: '请填写评分和评论内容' })
  res.status(201).json(addReview(req.params.id, { rating, content }))
})
app.get('/api/forum/posts', (req, res) => res.json(getForumPosts(req.query)))
app.post('/api/forum/posts', (req, res) => {
  const { title, excerpt, category, tags } = req.body || {}
  if (!title?.trim() || !excerpt?.trim()) return res.status(400).json({ message: '请填写帖子标题和正文' })
  res.status(201).json(createForumPost({ title, excerpt, category, tags }))
})
app.get('/api/forum/posts/:id/comments', (req, res) => res.json(getPostComments(req.params.id)))
app.post('/api/forum/posts/:id/comments', (req, res) => {
  const { content } = req.body || {}
  if (!content?.trim()) return res.status(400).json({ message: '回复内容不能为空' })
  res.status(201).json(addPostComment(req.params.id, content))
})
app.get('/api/profile', (_req, res) => res.json(getProfile()))
app.patch('/api/profile', (req, res) => res.json(updateProfile(req.body || {})))
app.post('/api/profile/favorites/:bookId', (req, res) => res.json({ favorites: toggleFavorite(req.params.bookId) }))
app.get('/api/profile/reviews/:bookId', (req, res) => res.json(getBookReviews(req.params.bookId)))
app.post('/api/profile/addresses', (req, res) => {
  const { label, receiver, phone, detail } = req.body || {}
  if (!receiver?.trim() || !phone?.trim() || !detail?.trim()) return res.status(400).json({ message: '请完整填写地址信息' })
  res.status(201).json(addAddress({ label: label?.trim() || '常用地址', receiver, phone, detail }))
})
app.delete('/api/profile/addresses/:id', (req, res) => res.json(removeAddress(req.params.id)))

initDatabase().finally(() => app.listen(port, () => console.log(`Paper Lantern API running at http://localhost:${port}`)))
