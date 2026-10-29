import crypto from 'node:crypto'

const users = [
  { id: 1, name: '周末读者', email: 'reader@paperlantern.book', password: '123456', role: 'reader', roleLabel: '普通读者', bio: '在杭州生活，喜欢散文、城市漫游和有一点点难度的书。' },
  { id: 2, name: '纸灯编辑部', email: 'editor@paperlantern.book', password: '123456', role: 'editor', roleLabel: '书店编辑', bio: '负责本周书单，也在寻找下一本值得推荐的书。' },
  { id: 3, name: '纸灯管理员', email: 'admin@paperlantern.book', password: 'admin123', role: 'admin', roleLabel: '系统管理员', bio: '维护纸灯书店与书友社区。' }
]
const sessions = new Map()
const publicUser = user => ({ id: user.id, name: user.name, email: user.email, role: user.role, roleLabel: user.roleLabel, bio: user.bio })

export function login(email, password) {
  const user = users.find(item => item.email.toLowerCase() === String(email).toLowerCase() && item.password === password)
  if (!user) return null
  const token = crypto.randomBytes(24).toString('hex')
  sessions.set(token, user.id)
  return { token, user: publicUser(user) }
}

export function register({ name, email, password }) {
  if (users.some(user => user.email.toLowerCase() === String(email).toLowerCase())) return { error: '这个邮箱已经注册过了' }
  const user = { id: users.length + 1, name: name.trim(), email: email.trim(), password, role: 'reader', roleLabel: '普通读者', bio: '刚来到纸灯，正在寻找第一本想读完的书。' }
  users.push(user)
  return login(email, password)
}

export function getUserByToken(token) {
  const userId = sessions.get(token)
  return users.find(user => user.id === userId) || null
}

export function getUserFromRequest(request) {
  const token = request.headers.authorization?.replace(/^Bearer\s+/i, '')
  return token ? getUserByToken(token) : null
}

export function logout(token) { sessions.delete(token) }
