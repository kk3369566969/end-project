import { books } from './data.js'

const now = () => new Date().toISOString()

let posts = [
  { id: 1, title: '有没有读完之后，真的改变生活的书？', excerpt: '最近在整理年度书单，想听听大家有没有那种读完之后，生活方式悄悄发生变化的书。文学、社科、工具书都可以。', author: '林间有风', avatar: '林', category: '阅读分享', likes: 38, comments: 12, createdAt: '2025-06-18T09:30:00.000Z', tags: ['年度书单', '生活方式'] },
  { id: 2, title: '《观看之道》读书会：我们如何被图像影响？', excerpt: '准备在周六晚上做一场线上共读，想从广告、电影和社交媒体里的图像聊起，欢迎带着自己的例子来。', author: '阿禾', avatar: '禾', category: '同城活动', likes: 24, comments: 8, createdAt: '2025-06-17T14:10:00.000Z', tags: ['设计', '共读'] },
  { id: 3, title: '给刚开始读毛姆的朋友：先从哪一本开始？', excerpt: '最近收到很多关于毛姆的提问。把我读过的几本简单排了顺序，也想看看大家心中的入坑作是哪本。', author: '纸灯编辑部', avatar: '纸', category: '书目讨论', likes: 56, comments: 19, createdAt: '2025-06-15T11:00:00.000Z', tags: ['毛姆', '入门'] }
]

let reviews = {
  1: [
    { id: 1, user: '北方来信', rating: 5, content: '像坐在一架小飞机里，往下看见时间和土地。读完很想去远方。', helpful: 16, createdAt: '2025-06-12T10:00:00.000Z' },
    { id: 2, user: 'Mia', rating: 4, content: '译笔很有画面感，适合晚上关掉手机慢慢读。', helpful: 8, createdAt: '2025-06-09T12:30:00.000Z' }
  ],
  3: [{ id: 3, user: '小满', rating: 5, content: '设计师的案头书，重新看见生活里那些被忽略的细节。', helpful: 22, createdAt: '2025-06-01T09:00:00.000Z' }]
}
let postComments = {
  1: [{ id: 11, user: '阿禾', content: '我会选《山茶文具店》，它让我开始认真写信。', createdAt: '2025-06-18T15:20:00.000Z' }],
  2: [{ id: 12, user: 'Mia', content: '很想参加，周六晚上见。', createdAt: '2025-06-17T18:30:00.000Z' }]
}

let profile = {
  id: 1,
  name: '周末读者',
  email: 'reader@paperlantern.book',
  bio: '在杭州生活，喜欢散文、城市漫游和有一点点难度的书。',
  points: 368,
  joinedAt: '2024-09-12',
  favorites: [2, 6],
  addresses: [
    { id: 1, label: '家', receiver: '周末读者', phone: '138****0000', detail: '浙江省杭州市西湖区纸灯路 1 号', isDefault: true },
    { id: 2, label: '公司', receiver: '周末读者', phone: '138****0000', detail: '浙江省杭州市拱墅区运河商务区 18 号', isDefault: false }
  ],
  orders: [
    { id: 'PL-240618', status: '已送达', total: 81, createdAt: '2025-06-18', items: [{ title: '人间草木', quantity: 1 }, { title: '月亮与六便士', quantity: 1 }] },
    { id: 'PL-240529', status: '已送达', total: 76, createdAt: '2025-05-29', items: [{ title: '设计中的设计', quantity: 1 }] }
  ]
}

export function getForumPosts({ category, search } = {}) {
  let result = [...posts]
  if (category && category !== '全部') result = result.filter(post => post.category === category)
  if (search) result = result.filter(post => `${post.title}${post.excerpt}${post.tags.join('')}`.toLowerCase().includes(search.toLowerCase()))
  return result
}

export function createForumPost({ title, excerpt, category = '阅读分享', tags = [] }) {
  const post = { id: Date.now(), title, excerpt, category, tags, author: profile.name, avatar: profile.name.slice(0, 1), likes: 0, comments: 0, createdAt: now() }
  posts = [post, ...posts]
  return post
}

export function getPostComments(postId) { return postComments[String(postId)] || [] }
export function addPostComment(postId, content) {
  const id = String(postId)
  const comment = { id: Date.now(), user: profile.name, content: content.trim(), createdAt: now() }
  postComments[id] = [...(postComments[id] || []), comment]
  posts = posts.map(post => String(post.id) === id ? { ...post, comments: (post.comments || 0) + 1 } : post)
  return comment
}

export function addReview(bookId, { rating, content }) {
  const key = String(bookId)
  const review = { id: Date.now(), user: profile.name, rating: Number(rating), content, helpful: 0, createdAt: now() }
  reviews[key] = [review, ...(reviews[key] || [])]
  return review
}

export function getBookReviews(bookId) { return reviews[String(bookId)] || [] }
export function getProfile() { return profile }

export function updateProfile(payload) {
  profile = { ...profile, name: payload.name?.trim() || profile.name, bio: payload.bio?.trim() || '' }
  return profile
}

export function toggleFavorite(bookId) {
  const id = Number(bookId)
  profile.favorites = profile.favorites.includes(id) ? profile.favorites.filter(item => item !== id) : [...profile.favorites, id]
  return profile.favorites
}

export function addAddress(address) {
  const next = { ...address, id: Date.now(), isDefault: profile.addresses.length === 0 }
  profile.addresses = [...profile.addresses, next]
  return next
}

export function removeAddress(addressId) {
  profile.addresses = profile.addresses.filter(address => address.id !== Number(addressId))
  if (profile.addresses.length && !profile.addresses.some(address => address.isDefault)) profile.addresses[0].isDefault = true
  return profile.addresses
}

export function getBookById(bookId) { return books.find(book => book.id === Number(bookId)) }
