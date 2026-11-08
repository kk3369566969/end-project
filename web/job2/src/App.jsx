import { useEffect, useMemo, useState } from "react";
import {
  ArrowRight,
  ArrowLeftRight,
  BarChart3,
  BookMarked,
  BookOpen,
  BookOpenCheck,
  Bookmark,
  Check,
  CheckCircle2,
  CalendarDays,
  ChevronDown,
  Clock3,
  Eye,
  Flag,
  Heart,
  Image,
  LayoutDashboard,
  Library,
  List,
  MapPin,
  Medal,
  Menu,
  MessageCircle,
  Minus,
  Package,
  PackageCheck,
  Pencil,
  Plus,
  Search,
  Send,
  ShoppingBag,
  Sparkles,
  Star,
  ThumbsUp,
  Target,
  Trophy,
  Trash2,
  Truck,
  UserCircle,
  UserPlus,
  Users,
  X,
} from "lucide-react";
import StoreHome, { StoreRail } from "./StoreHome.jsx";

const fallbackBooks = [
  {
    id: 1,
    title: "夜航西飞",
    author: "柏瑞尔·马卡姆",
    category: "literature",
    categoryLabel: "文学小说",
    price: 42,
    originalPrice: 56,
    rating: 4.9,
    stock: 24,
    badge: "编辑推荐",
    description: "一部写给夜行者的非洲航空回忆录，关于风、土地与自由。",
    cover:
      "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?auto=format&fit=crop&w=620&q=85",
    tone: "ochre",
  },
  {
    id: 2,
    title: "山茶文具店",
    author: "小川糸",
    category: "life",
    categoryLabel: "生活方式",
    price: 39,
    originalPrice: 48,
    rating: 4.8,
    stock: 18,
    badge: "治愈书单",
    description: "用一封封手写信，重新练习好好告别与认真生活。",
    cover:
      "https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=620&q=85",
    tone: "sage",
  },
  {
    id: 3,
    title: "设计中的设计",
    author: "原研哉",
    category: "design",
    categoryLabel: "设计与艺术",
    price: 76,
    originalPrice: 98,
    rating: 4.9,
    stock: 9,
    badge: "设计必读",
    description: "从日常物件出发，重新理解“无意识设计”的力量。",
    cover:
      "https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&w=620&q=85",
    tone: "ink",
  },
  {
    id: 4,
    title: "人间草木",
    author: "汪曾祺",
    category: "literature",
    categoryLabel: "文学小说",
    price: 32,
    originalPrice: 42,
    rating: 4.8,
    stock: 31,
    badge: "常读常新",
    description: "在草木、食物与旧事之间，读一个温厚而有趣的人间。",
    cover:
      "https://images.unsplash.com/photo-1518373714866-3f1478910cc0?auto=format&fit=crop&w=620&q=85",
    tone: "clay",
  },
  {
    id: 5,
    title: "东京八平米",
    author: "吉井忍",
    category: "life",
    categoryLabel: "生活方式",
    price: 45,
    originalPrice: 59,
    rating: 4.7,
    stock: 15,
    badge: "城市漫游",
    description: "一间八平米的房间，收纳着一个年轻人的城市生活哲学。",
    cover:
      "https://images.unsplash.com/photo-1495446815901-a7297e633e8d?auto=format&fit=crop&w=620&q=85",
    tone: "blue",
  },
  {
    id: 6,
    title: "观看之道",
    author: "约翰·伯格",
    category: "design",
    categoryLabel: "设计与艺术",
    price: 53,
    originalPrice: 68,
    rating: 4.9,
    stock: 12,
    badge: "视觉启蒙",
    description: "图像如何被看见，也如何反过来塑造我们看世界的方式。",
    cover:
      "https://images.unsplash.com/photo-1511108690759-009324a90311?auto=format&fit=crop&w=620&q=85",
    tone: "rose",
  },
  {
    id: 7,
    title: "置身事内",
    author: "兰小欢",
    category: "social",
    categoryLabel: "社会观察",
    price: 49,
    originalPrice: 65,
    rating: 4.8,
    stock: 20,
    badge: "年度社科",
    description: "理解中国经济运行的一扇窗口，写给每个身处其中的人。",
    cover:
      "https://images.unsplash.com/photo-1532012197267-da84d127e765?auto=format&fit=crop&w=620&q=85",
    tone: "violet",
  },
  {
    id: 8,
    title: "月亮与六便士",
    author: "毛姆",
    category: "literature",
    categoryLabel: "文学小说",
    price: 36,
    originalPrice: 46,
    rating: 4.8,
    stock: 27,
    badge: "经典新译",
    description: "当一个人决定追随内心，世俗的秩序会如何回应。",
    cover:
      "https://images.unsplash.com/photo-1476275466078-4007374efbbe?auto=format&fit=crop&w=620&q=85",
    tone: "green",
  },
];
const categoryList = [
  { id: "all", label: "全部书目", count: 8 },
  { id: "literature", label: "文学小说", count: 3 },
  { id: "life", label: "生活方式", count: 2 },
  { id: "design", label: "设计与艺术", count: 2 },
  { id: "social", label: "社会观察", count: 1 },
];
const forumCategories = ["全部", "阅读分享", "书目讨论", "同城活动", "阅读方法"];
const fallbackPosts = [
  {
    id: 1,
    title: "有没有读完之后，真的改变生活的书？",
    excerpt:
      "最近在整理年度书单，想听听大家有没有那种读完之后，生活方式悄悄发生变化的书。文学、社科、工具书都可以。",
    author: "林间有风",
    avatar: "林",
    category: "阅读分享",
    likes: 38,
    comments: 12,
    createdAt: "2025-06-18T09:30:00.000Z",
    tags: ["年度书单", "生活方式"],
  },
  {
    id: 2,
    title: "《观看之道》读书会：我们如何被图像影响？",
    excerpt:
      "准备在周六晚上做一场线上共读，想从广告、电影和社交媒体里的图像聊起，欢迎带着自己的例子来。",
    author: "阿禾",
    avatar: "禾",
    category: "同城活动",
    likes: 24,
    comments: 8,
    createdAt: "2025-06-17T14:10:00.000Z",
    tags: ["设计", "共读"],
  },
  {
    id: 3,
    title: "给刚开始读毛姆的朋友：先从哪一本开始？",
    excerpt:
      "最近收到很多关于毛姆的提问。把我读过的几本简单排了顺序，也想看看大家心中的入坑作是哪本。",
    author: "纸灯编辑部",
    avatar: "纸",
    category: "书目讨论",
    likes: 56,
    comments: 19,
    createdAt: "2025-06-15T11:00:00.000Z",
    tags: ["毛姆", "入门"],
  },
];
const fallbackProfile = {
  name: "周末读者",
  email: "reader@paperlantern.book",
  bio: "在杭州生活，喜欢散文、城市漫游和有一点点难度的书。",
  points: 368,
  favorites: [2, 6],
  addresses: [
    {
      id: 1,
      label: "家",
      receiver: "周末读者",
      phone: "138****0000",
      detail: "浙江省杭州市西湖区纸灯路 1 号",
      isDefault: true,
    },
  ],
  orders: [
    {
      id: "PL-240618",
      status: "已送达",
      total: 81,
      createdAt: "2025-06-18",
      items: [
        { title: "人间草木", quantity: 1 },
        { title: "月亮与六便士", quantity: 1 },
      ],
    },
  ],
};
const dateText = (value) =>
  new Date(value).toLocaleDateString("zh-CN", { month: "long", day: "numeric" });
const SESSION_TOKEN_KEY = "paper-lantern-token";
const getSessionToken = () => window.sessionStorage.getItem(SESSION_TOKEN_KEY) || "";

function App() {
  const [authUser, setAuthUser] = useState(null);
  const [authLoading, setAuthLoading] = useState(true);
  const [view, setView] = useState("home");
  const [homeSection, setHomeSection] = useState("overview");
  const [adminSection, setAdminSection] = useState("overview");
  const [books, setBooks] = useState(fallbackBooks);
  const [category, setCategory] = useState("all");
  const [search, setSearch] = useState("");
  const [sort, setSort] = useState("featured");
  const [cart, setCart] = useState([]);
  const [cartOpen, setCartOpen] = useState(false);
  const [checkoutOpen, setCheckoutOpen] = useState(false);
  const [order, setOrder] = useState(null);
  const [toast, setToast] = useState("");
  const [mobileMenu, setMobileMenu] = useState(false);
  const [activeBook, setActiveBook] = useState(null);
  const [bookReviews, setBookReviews] = useState([]);
  const [bookCanReview, setBookCanReview] = useState(false);
  const [reviewSort, setReviewSort] = useState("recent");
  const [reviewComments, setReviewComments] = useState({});
  const [reading, setReading] = useState({ shelves: { want: [], reading: [], read: [] }, history: [], stats: {} });
  const [posts, setPosts] = useState(fallbackPosts);
  const [forumCategory, setForumCategory] = useState("全部");
  const [forumSearch, setForumSearch] = useState("");
  const [postOpen, setPostOpen] = useState(false);
  const [activePost, setActivePost] = useState(null);
  const [postComments, setPostComments] = useState([]);
  const [profile, setProfile] = useState(fallbackProfile);
  const [profileTab, setProfileTab] = useState("orders");
  const [profileEditOpen, setProfileEditOpen] = useState(false);
  const [addressOpen, setAddressOpen] = useState(false);
  const [activeUserProfile, setActiveUserProfile] = useState(null);
  const [secondhand, setSecondhand] = useState([]);
  const [clubs, setClubs] = useState([]);
  const [challenges, setChallenges] = useState([]);
  const [analytics, setAnalytics] = useState(null);
  const [leaderboard, setLeaderboard] = useState(null);
  const authHeaders = () => ({
    Authorization: `Bearer ${getSessionToken()}`,
  });
  useEffect(() => {
    // Migrate a legacy shared token once, then keep this tab isolated.
    const legacyToken = window.localStorage.getItem(SESSION_TOKEN_KEY);
    const token = getSessionToken() || legacyToken;
    if (legacyToken && !getSessionToken()) {
      window.sessionStorage.setItem(SESSION_TOKEN_KEY, legacyToken);
      window.localStorage.removeItem(SESSION_TOKEN_KEY);
    }
    if (!token) {
      setAuthLoading(false);
      return;
    }
    fetch("/api/auth/me", { headers: { Authorization: `Bearer ${token}` } })
      .then((res) => (res.ok ? res.json() : Promise.reject()))
      .then((data) => {
        setAuthUser(data.user);
        if (data.user.role === "admin") setView("admin");
      })
      .catch(() => window.sessionStorage.removeItem(SESSION_TOKEN_KEY))
      .finally(() => setAuthLoading(false));
  }, []);
  useEffect(() => {
    const params = new URLSearchParams();
    if (category !== "all") params.set("category", category);
    if (search.trim()) params.set("search", search.trim());
    if (sort !== "featured") params.set("sort", sort);
    fetch(`/api/books?${params}`)
      .then((res) => (res.ok ? res.json() : Promise.reject()))
      .then(setBooks)
      .catch(() => setBooks(fallbackBooks));
  }, [category, search, sort]);
  useEffect(() => {
    if (view === "forum")
      fetch(
        `/api/forum/posts?category=${encodeURIComponent(forumCategory)}&search=${encodeURIComponent(forumSearch)}`,
      )
        .then((res) => (res.ok ? res.json() : Promise.reject()))
        .then(setPosts)
        .catch(() => setPosts(fallbackPosts));
    if (view === "profile")
      Promise.all([
        fetch("/api/profile", { headers: authHeaders() }).then((res) => (res.ok ? res.json() : Promise.reject())),
        fetch("/api/reading", { headers: authHeaders() }).then((res) => (res.ok ? res.json() : Promise.reject())),
      ])
        .then(([nextProfile, nextReading]) => {
          setProfile(nextProfile);
          setReading(nextReading);
        })
        .catch(() => {
          setProfile(fallbackProfile);
          setReading({ shelves: { want: [], reading: [], read: [] }, history: [], stats: {} });
        });
    if (view === "secondhand")
      fetch("/api/secondhand", { headers: authHeaders() })
        .then((res) => (res.ok ? res.json() : Promise.reject()))
        .then(setSecondhand)
        .catch(() => setSecondhand([]));
    if (view === "clubs")
      fetch("/api/clubs", { headers: authHeaders() })
        .then((res) => (res.ok ? res.json() : Promise.reject()))
        .then(setClubs)
        .catch(() => setClubs([]));
    if (view === "analytics")
      fetch("/api/analytics/me", { headers: authHeaders() })
        .then((res) => (res.ok ? res.json() : Promise.reject()))
        .then(setAnalytics)
        .catch(() => setAnalytics(null));
    if (view === "growth")
      Promise.all([
        fetch("/api/challenges", { headers: authHeaders() }).then((res) => (res.ok ? res.json() : Promise.reject())),
        fetch("/api/reading/leaderboard", { headers: authHeaders() }).then((res) => (res.ok ? res.json() : Promise.reject())),
      ])
        .then(([nextChallenges, nextLeaderboard]) => {
          setChallenges(nextChallenges);
          setLeaderboard(nextLeaderboard);
        })
        .catch(() => {
          setChallenges([]);
          setLeaderboard(null);
        });
  }, [view, forumCategory, forumSearch]);
  const cartCount = cart.reduce((sum, item) => sum + item.quantity, 0);
  const total = cart.reduce((sum, item) => sum + item.price * item.quantity, 0);
  const featured = useMemo(() => books.find((book) => book.id === 1) || fallbackBooks[0], [books]);
  function notify(message) {
    setToast(message);
    window.setTimeout(() => setToast(""), 2200);
  }
  function goView(next) {
    if (authUser?.role === "admin" && next !== "admin") return;
    setView(next);
    setMobileMenu(false);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }
  async function publishSecondhand(payload) {
    const response = await fetch("/api/secondhand", { method: "POST", headers: { "Content-Type": "application/json", ...authHeaders() }, body: JSON.stringify(payload) });
    if (response.ok) { const created = await response.json(); setSecondhand((current) => [created, ...current]); notify("二手书已发布"); }
    else notify((await response.json().catch(() => ({}))).message || "发布失败");
  }
  async function buySecondhandListing(listing) {
    const response = await fetch(`/api/secondhand/${listing.id}/buy`, { method: "POST", headers: authHeaders() });
    if (response.ok) { const next = await response.json(); setSecondhand((current) => current.map((item) => item.id === next.id ? next : item)); notify("已锁定这本二手书，请联系卖家完成交接"); }
    else notify((await response.json().catch(() => ({}))).message || "购买失败");
  }
  async function publishClub(payload) {
    const response = await fetch("/api/clubs", { method: "POST", headers: { "Content-Type": "application/json", ...authHeaders() }, body: JSON.stringify(payload) });
    if (response.ok) { const created = await response.json(); setClubs((current) => [...current, created].sort((a, b) => new Date(a.eventAt) - new Date(b.eventAt))); notify("读书会已创建"); }
    else notify((await response.json().catch(() => ({}))).message || "创建失败");
  }
  async function toggleClub(club) {
    const response = await fetch(`/api/clubs/${club.id}/join`, { method: "POST", headers: authHeaders() });
    if (response.ok) { const next = await response.json(); setClubs((current) => current.map((item) => item.id === next.id ? next : item)); notify(next.joined ? "已报名读书会" : "已取消报名"); }
  }
  async function toggleChallenge(challenge) {
    const response = await fetch(`/api/challenges/${challenge.id}/join`, { method: "POST", headers: authHeaders() });
    if (response.ok) { const next = await response.json(); setChallenges((current) => current.map((item) => item.id === next.id ? next : item)); notify("已加入阅读挑战"); }
  }
  async function saveChallengeProgress(challenge, payload) {
    const response = await fetch(`/api/challenges/${challenge.id}/progress`, { method: "PATCH", headers: { "Content-Type": "application/json", ...authHeaders() }, body: JSON.stringify(payload) });
    if (response.ok) { const next = await response.json(); setChallenges((current) => current.map((item) => item.id === next.id ? next : item)); notify("挑战进度已保存"); }
  }
  function addToCart(book) {
    setCart((current) => {
      const existing = current.find((item) => item.id === book.id);
      return existing
        ? current.map((item) =>
            item.id === book.id ? { ...item, quantity: item.quantity + 1 } : item,
          )
        : [...current, { ...book, quantity: 1 }];
    });
    notify(`《${book.title}》已放入书袋`);
  }
  function updateQuantity(id, delta) {
    setCart((current) =>
      current.map((item) =>
        item.id === id ? { ...item, quantity: Math.max(1, item.quantity + delta) } : item,
      ),
    );
  }
  function removeItem(id) {
    setCart((current) => current.filter((item) => item.id !== id));
  }
  async function openBook(book, nextReviewSort = reviewSort) {
    setActiveBook(book);
    try {
      const [reviewsResponse, permissionResponse, readingResponse] = await Promise.all([
        fetch(`/api/books/${book.id}/reviews?sort=${nextReviewSort}`),
        fetch(`/api/books/${book.id}/review-permission`, { headers: authHeaders() }),
        fetch(`/api/reading/${book.id}/history`, { method: "POST", headers: authHeaders() }),
      ]);
      setBookReviews(reviewsResponse.ok ? await reviewsResponse.json() : []);
      setBookCanReview(permissionResponse.ok && (await permissionResponse.json()).canReview);
      if (readingResponse.ok) setReading(await readingResponse.json());
    } catch {
      setBookReviews([]);
      setBookCanReview(false);
    }
  }
  async function sortBookReviews(nextSort) {
    setReviewSort(nextSort);
    if (activeBook) {
      const response = await fetch(`/api/books/${activeBook.id}/reviews?sort=${nextSort}`);
      if (response.ok) setBookReviews(await response.json());
    }
  }
  async function toggleFavorite(book) {
    const response = await fetch(`/api/profile/favorites/${book.id}`, {
      method: "POST",
      headers: authHeaders(),
    });
    if (response.ok) {
      const data = await response.json();
      setProfile((current) => ({ ...current, favorites: data.favorites }));
      notify(data.favorites.includes(book.id) ? "已加入收藏夹" : "已取消收藏");
    }
  }
  async function submitReview(book, review) {
    const response = await fetch(`/api/books/${book.id}/reviews`, {
      method: "POST",
      headers: { "Content-Type": "application/json", ...authHeaders() },
      body: JSON.stringify(review),
    });
    if (response.ok) {
      const item = await response.json();
      setBookReviews((current) => [item, ...current]);
      setBookCanReview(false);
      notify("评论已发布，感谢你的分享");
    } else {
      const data = await response.json().catch(() => ({}));
      notify(data.message || "评价提交失败");
    }
  }
  async function toggleReviewLike(reviewId) {
    const response = await fetch(`/api/reviews/${reviewId}/like`, { method: "POST", headers: authHeaders() });
    if (!response.ok) return;
    const data = await response.json();
    setBookReviews((current) => current.map((review) => review.id === reviewId ? { ...review, helpful: data.helpful, liked: data.liked } : review));
  }
  async function loadReviewComments(reviewId) {
    const response = await fetch(`/api/reviews/${reviewId}/comments`);
    if (response.ok) {
      const comments = await response.json();
      setReviewComments((current) => ({ ...current, [reviewId]: comments }));
    }
  }
  async function submitReviewComment(reviewId, content) {
    const response = await fetch(`/api/reviews/${reviewId}/comments`, { method: "POST", headers: { "Content-Type": "application/json", ...authHeaders() }, body: JSON.stringify({ content }) });
    if (response.ok) {
      const comment = await response.json();
      setReviewComments((current) => ({ ...current, [reviewId]: [...(current[reviewId] || []), comment] }));
    }
  }
  async function submitPost(data) {
    const response = await fetch("/api/forum/posts", {
      method: "POST",
      headers: { "Content-Type": "application/json", ...authHeaders() },
      body: JSON.stringify(data),
    });
    if (response.ok) {
      const post = await response.json();
      setPosts((current) => [post, ...current]);
      setPostOpen(false);
      notify("帖子已发布，等待读者回应");
    } else {
      const data = await response.json().catch(() => ({}));
      notify(data.message || "帖子发布失败，请检查标题和正文");
    }
  }
  async function togglePostLike(post) {
    const response = await fetch(`/api/forum/posts/${post.id}/like`, { method: "POST", headers: authHeaders() });
    if (!response.ok) return;
    const data = await response.json();
    setPosts((current) => current.map((item) => item.id === post.id ? { ...item, likes: data.likes } : item));
  }
  async function reportPost(post) {
    const reason = window.prompt("请填写举报原因", "内容与书友交流主题无关");
    if (!reason) return;
    const response = await fetch(`/api/forum/posts/${post.id}/report`, { method: "POST", headers: { "Content-Type": "application/json", ...authHeaders() }, body: JSON.stringify({ reason }) });
    if (response.ok) notify("举报已提交，管理员会尽快处理");
  }
  async function followAuthor(authorId) {
    if (!authorId) return;
    const response = await fetch(`/api/users/${authorId}/follow`, { method: "POST", headers: authHeaders() });
    if (response.ok) notify((await response.json()).following ? "已关注作者" : "已取消关注");
  }
  async function openUserProfile(userId) {
    const response = await fetch(`/api/users/${userId}/profile`, { headers: authHeaders() });
    if (response.ok) setActiveUserProfile(await response.json());
  }
  async function updateReading(bookId, payload) {
    const response = await fetch(`/api/reading/${bookId}`, { method: "PATCH", headers: { "Content-Type": "application/json", ...authHeaders() }, body: JSON.stringify(payload) });
    if (response.ok) setReading(await response.json());
  }
  async function openPost(post) {
    setActivePost(post);
    try {
      const response = await fetch(`/api/forum/posts/${post.id}/comments`);
      setPostComments(response.ok ? await response.json() : []);
    } catch {
      setPostComments([]);
    }
  }
  async function submitPostComment(content) {
    const response = await fetch(`/api/forum/posts/${activePost.id}/comments`, {
      method: "POST",
      headers: { "Content-Type": "application/json", ...authHeaders() },
      body: JSON.stringify({ content }),
    });
    if (response.ok) {
      const comment = await response.json();
      setPostComments((current) => [...current, comment]);
      setPosts((current) =>
        current.map((post) =>
          post.id === activePost.id ? { ...post, comments: post.comments + 1 } : post,
        ),
      );
      notify("回复已发布");
    }
  }
  async function toggleCommentLike(commentId) {
    const response = await fetch(`/api/forum/comments/${commentId}/like`, { method: "POST", headers: authHeaders() });
    if (!response.ok) return;
    const data = await response.json();
    setPostComments((current) => current.map((comment) => comment.id === commentId ? { ...comment, likes: data.likes, liked: data.liked } : comment));
  }
  function handleLogin(result) {
    window.sessionStorage.setItem(SESSION_TOKEN_KEY, result.token);
    setAuthUser(result.user);
    setProfile((current) => ({
      ...current,
      name: result.user.name,
      email: result.user.email,
      bio: result.user.bio,
    }));
    setView(result.user.role === "admin" ? "admin" : "home");
  }
  async function handleLogout() {
    const token = getSessionToken();
    await fetch("/api/auth/logout", {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
    }).catch(() => {});
    window.sessionStorage.removeItem(SESSION_TOKEN_KEY);
    setAuthUser(null);
  }
  if (authLoading)
    return (
      <div className="auth-loading">
        <div className="brand">
          <span className="brand-mark">
            <BookOpen size={20} />
          </span>
          <span>
            纸灯<small>BOOKS</small>
          </span>
        </div>
        <span>正在点亮你的阅读空间…</span>
      </div>
    );
  if (!authUser) return <LoginPage onLogin={handleLogin} />;
  async function removeAddress(id) {
    const res = await fetch(`/api/profile/addresses/${id}`, {
      method: "DELETE",
      headers: authHeaders(),
    });
    if (res.ok) {
      const addresses = await res.json();
      setProfile((current) => ({ ...current, addresses }));
    }
  }
  async function saveProfile(data) {
    const res = await fetch("/api/profile", {
      method: "PATCH",
      headers: { "Content-Type": "application/json", ...authHeaders() },
      body: JSON.stringify(data),
    });
    if (res.ok) {
      setProfile(await res.json());
      setProfileEditOpen(false);
      notify("个人资料已更新");
    }
  }
  async function saveAddress(data) {
    const res = await fetch("/api/profile/addresses", {
      method: "POST",
      headers: { "Content-Type": "application/json", ...authHeaders() },
      body: JSON.stringify(data),
    });
    if (res.ok) {
      const address = await res.json();
      setProfile((current) => ({ ...current, addresses: [...current.addresses, address] }));
      setAddressOpen(false);
      notify("收货地址已保存");
    }
  }
  async function openCheckout() {
    setCartOpen(false);
    try {
      const response = await fetch("/api/profile", { headers: authHeaders() });
      if (response.ok) setProfile(await response.json());
    } catch {
      // The checkout form remains usable for manual entry if the profile request fails.
    }
    setCheckoutOpen(true);
  }
  const workspaceRailProps = {
    activeView: view,
    section: homeSection,
    setSection: setHomeSection,
    category,
    setCategory,
    categoryList,
    onView: goView,
    authUser,
    onLogout: handleLogout,
    cartCount,
    onOpenBag: () => setCartOpen(true),
    adminSection,
    onAdminSection: setAdminSection,
    onSearch: () => {
      goView("home");
      setHomeSection("shelf");
      window.setTimeout(() => document.getElementById("book-search")?.focus(), 100);
    },
  };
  return (
    <div className="app-shell">
      <header className="site-header home-header-hidden">
        <div className="container header-inner">
          <button
            className="icon-button mobile-only"
            aria-label="打开菜单"
            onClick={() => setMobileMenu(!mobileMenu)}
          >
            <Menu size={21} />
          </button>
          <button
            className="brand brand-button"
            onClick={() => goView("home")}
            aria-label="纸灯书店首页"
          >
            <span className="brand-mark">
              <BookOpen size={20} />
            </span>
            <span>
              纸灯<small>BOOKS</small>
            </span>
          </button>
          <nav className={`main-nav ${mobileMenu ? "is-open" : ""}`}>
            <button className={view === "home" ? "nav-active" : ""} onClick={() => goView("home")}>
              逛书架
            </button>
            <button
              className={view === "forum" ? "nav-active" : ""}
              onClick={() => goView("forum")}
            >
              书友论坛
            </button>
            <button
              className={view === "profile" ? "nav-active" : ""}
              onClick={() => goView("profile")}
            >
              我的纸灯
            </button>
          </nav>
          <div className="header-actions">
            <button
              className="icon-button"
              aria-label="搜索书籍"
              onClick={() => {
                goView("home");
                window.setTimeout(() => document.getElementById("book-search")?.focus(), 100);
              }}
            >
              <Search size={19} />
            </button>
            <button
              className="bag-button"
              onClick={() => setCartOpen(true)}
              aria-label={`打开书袋，${cartCount}件商品`}
            >
              <ShoppingBag size={19} />
              <span>书袋</span>
              {cartCount > 0 && <b>{cartCount}</b>}
            </button>
            <button
              className="profile-chip"
              onClick={() => goView("profile")}
              aria-label="打开个人中心"
            >
              <UserCircle size={20} />
            </button>
            <span className="current-user" title={authUser.email}>
              {authUser.name} · {authUser.roleLabel}
            </span>
            <button className="logout-button" onClick={handleLogout}>
              退出
            </button>
          </div>
        </div>
      </header>
      {view === "home" && (
        <StoreHome
          books={books}
          featured={featured}
          category={category}
          setCategory={setCategory}
          search={search}
          setSearch={setSearch}
          sort={sort}
          setSort={setSort}
          categoryList={categoryList}
          onAdd={addToCart}
          onOpen={openBook}
          favorites={profile.favorites}
          onFavorite={toggleFavorite}
          goView={goView}
          authUser={authUser}
          onLogout={handleLogout}
          cartCount={cartCount}
          onOpenBag={() => setCartOpen(true)}
          section={homeSection}
          setSection={setHomeSection}
        />
      )}
      {view === "admin" && authUser.role === "admin" && (
        <main className="store-workspace workspace-page">
          <StoreRail {...workspaceRailProps} />
          <section className="store-content workspace-content">
            <AdminView authUser={authUser} tab={adminSection} onTabChange={setAdminSection} />
          </section>
        </main>
      )}
      {view === "forum" && (
        <main className="store-workspace workspace-page">
          <StoreRail {...workspaceRailProps} />
          <section className="store-content workspace-content">
            <ForumView
              posts={posts}
              category={forumCategory}
              setCategory={setForumCategory}
              search={forumSearch}
              setSearch={setForumSearch}
              onNew={() => setPostOpen(true)}
              onOpen={openPost}
              onLike={togglePostLike}
              onReport={reportPost}
              onFollowAuthor={followAuthor}
              onOpenAuthor={openUserProfile}
              viewerId={authUser.id}
            />
          </section>
        </main>
      )}
      {view === "profile" && (
        <main className="store-workspace workspace-page">
          <StoreRail {...workspaceRailProps} />
          <section className="store-content workspace-content">
            <ProfileView
              profile={{ ...profile, name: authUser.name, email: authUser.email }}
              books={books}
              tab={profileTab}
              setTab={setProfileTab}
              onEdit={() => setProfileEditOpen(true)}
              onAddress={() => setAddressOpen(true)}
              onRemoveAddress={removeAddress}
              onOpenBook={openBook}
              reading={reading}
              onUpdateReading={updateReading}
            />
          </section>
        </main>
      )}
      {view === "secondhand" && (
        <main className="store-workspace workspace-page">
          <StoreRail {...workspaceRailProps} />
          <section className="store-content workspace-content"><SecondhandView listings={secondhand} viewerId={authUser.id} onPublish={publishSecondhand} onBuy={buySecondhandListing} /></section>
        </main>
      )}
      {view === "clubs" && (
        <main className="store-workspace workspace-page">
          <StoreRail {...workspaceRailProps} />
          <section className="store-content workspace-content"><ClubsView clubs={clubs} books={books} onCreate={publishClub} onToggle={toggleClub} /></section>
        </main>
      )}
      {view === "growth" && (
        <main className="store-workspace workspace-page">
          <StoreRail {...workspaceRailProps} />
          <section className="store-content workspace-content"><ReadingGrowthView challenges={challenges} leaderboard={leaderboard} onJoin={toggleChallenge} onSaveProgress={saveChallengeProgress} /></section>
        </main>
      )}
      {view === "analytics" && (
        <main className="store-workspace workspace-page">
          <StoreRail {...workspaceRailProps} />
          <section className="store-content workspace-content"><ReaderAnalyticsView analytics={analytics} /></section>
        </main>
      )}
      <footer>
        <div className="container footer-inner">
          <button className="brand footer-brand brand-button" onClick={() => goView("home")}>
            <span className="brand-mark">
              <BookOpen size={20} />
            </span>
            <span>
              纸灯<small>BOOKS</small>
            </span>
          </button>
          <span>© 2025 纸灯书店 · 杭州</span>
          <span>周一至周日 10:00—21:00</span>
        </div>
      </footer>
      {cartOpen && (
        <CartDrawer
          cart={cart}
          count={cartCount}
          total={total}
          onClose={() => setCartOpen(false)}
          onUpdate={updateQuantity}
          onRemove={removeItem}
          onCheckout={openCheckout}
        />
      )}
      {checkoutOpen && (
        <CheckoutModal
          total={total}
          cart={cart}
          addresses={profile.addresses || []}
          onClose={() => setCheckoutOpen(false)}
          onSuccess={async (created) => {
            setOrder(created);
            setCart([]);
            setCheckoutOpen(false);
          }}
        />
      )}
      {order && (
        <div className="overlay">
          <div className="success-modal">
            <div className="success-icon">
              <Check size={25} />
            </div>
            <span className="section-kicker">订单已确认</span>
            <h2>谢谢你，把书带回家。</h2>
            <p>
              订单 <strong>#{order.id}</strong> 已进入备货。我们会在 48 小时内寄出。
            </p>
            <div className="success-detail">
              <span>订单金额</span>
              <strong>¥{order.total}</strong>
            </div>
            <button className="button button-dark button-wide" onClick={() => setOrder(null)}>
              继续逛书架
            </button>
          </div>
        </div>
      )}
      {activeBook && (
        <BookDetailModal
          book={activeBook}
          reviews={bookReviews}
          isFavorite={profile.favorites.includes(activeBook.id)}
          onFavorite={() => toggleFavorite(activeBook)}
          onAdd={() => addToCart(activeBook)}
          onClose={() => setActiveBook(null)}
          onReview={(review) => submitReview(activeBook, review)}
          canReview={bookCanReview}
          reviewSort={reviewSort}
          onReviewSort={sortBookReviews}
          onLikeReview={toggleReviewLike}
          reviewComments={reviewComments}
          onLoadReviewComments={loadReviewComments}
          onSubmitReviewComment={submitReviewComment}
          books={books}
          onOpenRelated={openBook}
          onUpdateReading={updateReading}
        />
      )}
      {activePost && (
        <ForumPostModal
          post={activePost}
          comments={postComments}
          onClose={() => setActivePost(null)}
          onSubmit={submitPostComment}
          onLikeComment={toggleCommentLike}
        />
      )}
      {postOpen && <NewPostModal onClose={() => setPostOpen(false)} onSubmit={submitPost} />}
      {activeUserProfile && (
        <UserProfileModal profile={activeUserProfile} onClose={() => setActiveUserProfile(null)} onFollow={() => followAuthor(activeUserProfile.id)} onOpenPost={openPost} />
      )}
      {profileEditOpen && (
        <ProfileEditModal
          profile={profile}
          onClose={() => setProfileEditOpen(false)}
          onSave={saveProfile}
        />
      )}
      {addressOpen && <AddressModal onClose={() => setAddressOpen(false)} onSave={saveAddress} />}
      {toast && (
        <div className="toast">
          <Check size={16} /> {toast}
        </div>
      )}
    </div>
  );
}

function LoginPage({ onLogin }) {
  const [mode, setMode] = useState("login");
  const [role, setRole] = useState("reader");
  const [form, setForm] = useState({
    name: "",
    email: "reader@paperlantern.book",
    password: "123456",
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const demos = {
    reader: ["reader@paperlantern.book", "123456"],
    admin: ["admin@paperlantern.book", "admin123"],
  };
  function chooseRole(next) {
    setRole(next);
    if (mode === "login")
      setForm((current) => ({ ...current, email: demos[next][0], password: demos[next][1] }));
  }
  async function submit(event) {
    event.preventDefault();
    setLoading(true);
    setError("");
    const endpoint = mode === "login" ? "/api/auth/login" : "/api/auth/register";
    try {
      const response = await fetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(form),
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.message);
      onLogin(data);
    } catch (err) {
      setError(err.message || "操作失败，请稍后重试");
    } finally {
      setLoading(false);
    }
  }
  return (
    <main className="auth-page">
      <div className="auth-visual">
        <div className="auth-brand brand">
          <span className="brand-mark">
            <BookOpen size={20} />
          </span>
          <span>
            纸灯<small>BOOKS</small>
          </span>
        </div>
        <div className="auth-quote">
          <span>READ SLOWLY</span>
          <h1>
            每一次翻页，
            <br />
            <em>都是一次抵达。</em>
          </h1>
          <p>
            登录纸灯，保存你的阅读轨迹，
            <br />
            和同频的人分享一本书。
          </p>
        </div>
        <div className="auth-visual-foot">
          <span>杭州 · 独立书店</span>
          <span>EST. 2025</span>
        </div>
      </div>
      <section className="auth-panel">
        <div className="auth-panel-top">
          <span>纸灯书店</span>
          <span>阅读者入口</span>
        </div>
        <div className="auth-form-wrap">
          <div className="auth-tabs">
            <button
              className={mode === "login" ? "is-active" : ""}
              onClick={() => setMode("login")}
            >
              登录
            </button>
            <button
              className={mode === "register" ? "is-active" : ""}
              onClick={() => setMode("register")}
            >
              注册
            </button>
          </div>
          {mode === "login" && (
            <div className="role-switch">
              <span>选择身份</span>
              <div>
                {[
                  ["reader", "普通读者"],
                  ["admin", "管理员"],
                ].map(([id, label]) => (
                  <button
                    key={id}
                    className={role === id ? "is-active" : ""}
                    onClick={() => chooseRole(id)}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </div>
          )}
          <form className="auth-form" onSubmit={submit}>
            {mode === "register" && (
              <label>
                昵称
                <input
                  required
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  placeholder="怎么称呼你"
                />
              </label>
            )}
            <label>
              邮箱
              <input
                required
                type="email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                placeholder="name@example.com"
              />
            </label>
            <label>
              密码
              <input
                required
                minLength="6"
                type="password"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
                placeholder="至少 6 位字符"
              />
            </label>
            {error && <p className="form-error">{error}</p>}
            <button className="button button-dark button-wide" disabled={loading}>
              {loading ? "正在进入…" : mode === "login" ? "登录纸灯" : "创建账号"}{" "}
              <ArrowRight size={17} />
            </button>
          </form>
          {mode === "login" && (
            <div className="demo-hint">
              <strong>演示账号</strong>
              <span>读者 reader@paperlantern.book / 123456</span>
              <span>管理员 admin@paperlantern.book / admin123</span>
            </div>
          )}
          <p className="auth-terms">
            登录即表示你同意纸灯书店的使用规则。不同身份拥有相应的工作台权限。
          </p>
        </div>
      </section>
    </main>
  );
}

function HomeView({
  books,
  featured,
  category,
  setCategory,
  search,
  setSearch,
  sort,
  setSort,
  categoryList,
  onAdd,
  onOpen,
  favorites,
  onFavorite,
  goView,
}) {
  return (
    <main id="top">
      <section className="hero container">
        <div className="hero-copy">
          <p className="eyebrow">
            <Sparkles size={15} /> 本周主题书单 / 07
          </p>
          <h1>
            在灯下，找到
            <br />
            <em>下一本。</em>
          </h1>
          <p className="hero-lede">
            我们为你挑出那些读完之后，
            <br />
            会想把生活重新过一遍的书。
          </p>
          <div className="hero-actions">
            <button
              className="button button-dark"
              onClick={() =>
                document.getElementById("books")?.scrollIntoView({ behavior: "smooth" })
              }
            >
              开始选书 <ArrowRight size={17} />
            </button>
            <button className="text-link" onClick={() => goView("forum")}>
              去书友论坛 <MessageCircle size={15} />
            </button>
          </div>
          <div className="hero-meta">
            <span>
              <strong>8</strong> 本精选上架
            </span>
            <span>
              <strong>4.8</strong> 平均读者评分
            </span>
          </div>
        </div>
        <div className="hero-art">
          <div className="sun-disc"></div>
          <div className="hero-book hero-book-back"></div>
          <div className="hero-book hero-book-front">
            <span>
              把日子
              <br />
              <i>过成诗</i>
            </span>
          </div>
          <div className="hero-caption">
            THE
            <br />
            NIGHT
            <br />
            READING
          </div>
        </div>
      </section>
      <section className="editor-strip">
        <div className="container editor-inner">
          <div>
            <span className="section-kicker">本周编辑选书</span>
            <h2>
              不追热闹，
              <br />
              只留值得。
            </h2>
          </div>
          <div className="editor-feature">
            <img src={featured.cover} alt={featured.title} />
            <div>
              <span className="mini-label">编辑手记</span>
              <h3>{featured.title}</h3>
              <p>{featured.description}</p>
              <button className="text-link" onClick={() => onOpen(featured)}>
                查看书籍详情 <ArrowRight size={15} />
              </button>
            </div>
          </div>
          <div className="editor-stat">
            <span>
              本周已被读者
              <br />
              带走
            </span>
            <strong>1,286</strong>
            <small>本</small>
          </div>
        </div>
      </section>
      <section className="catalogue container" id="books">
        <div className="catalogue-heading">
          <div>
            <span className="section-kicker">书架 / 发现你的下一本</span>
            <h2>
              慢慢挑，<em>总会遇见。</em>
            </h2>
          </div>
          <div className="search-wrap">
            <Search size={18} />
            <input
              id="book-search"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="搜书名、作者或关键词"
            />
            <kbd>⌘ K</kbd>
          </div>
        </div>
        <div className="catalogue-layout">
          <aside className="filters">
            <p className="filter-title">按主题浏览</p>
            {categoryList.map((item) => (
              <button
                key={item.id}
                className={category === item.id ? "filter-active" : ""}
                onClick={() => setCategory(item.id)}
              >
                <span>{item.label}</span>
                <small>{item.count}</small>
              </button>
            ))}
            <div className="filter-note">
              <Heart size={17} />
              <span>
                每一本书，都应该
                <br />
                有被认真读完的机会。
              </span>
            </div>
          </aside>
          <div className="book-area">
            <div className="book-toolbar">
              <span>{books.length} 本书可选</span>
              <label>
                排序{" "}
                <select value={sort} onChange={(e) => setSort(e.target.value)}>
                  <option value="featured">编辑精选</option>
                  <option value="rating">评分最高</option>
                  <option value="price-asc">价格从低到高</option>
                  <option value="price-desc">价格从高到低</option>
                </select>
                <ChevronDown size={15} />
              </label>
            </div>
            <div className="book-grid">
              {books.map((book) => (
                <BookCard
                  key={book.id}
                  book={book}
                  onAdd={onAdd}
                  onOpen={onOpen}
                  isFavorite={favorites.includes(book.id)}
                  onFavorite={() => onFavorite(book)}
                />
              ))}
            </div>
            {books.length === 0 && (
              <div className="empty-state">
                <Library size={28} />
                <h3>暂时没有找到这本书</h3>
                <p>换一个关键词试试，或者回到全部书目。</p>
                <button
                  className="button button-outline"
                  onClick={() => {
                    setSearch("");
                    setCategory("all");
                  }}
                >
                  查看全部
                </button>
              </div>
            )}
          </div>
        </div>
      </section>
      <section className="about-band">
        <div className="container about-inner">
          <div className="about-symbol">✳</div>
          <div>
            <span className="section-kicker">关于纸灯</span>
            <h2>
              书店不是仓库，
              <br />
              是你和世界相遇的地方。
            </h2>
          </div>
          <p>
            纸灯是一间小小的独立书店。我们相信，买书不是囤积知识，而是为某个还没发生的念头，留一盏灯。
          </p>
          <button className="text-link" onClick={() => goView("forum")}>
            加入书友讨论 <ArrowRight size={15} />
          </button>
        </div>
      </section>
    </main>
  );
}
function BookCard({ book, onAdd, onOpen, isFavorite, onFavorite }) {
  return (
    <article className="book-card">
      <button
        className="book-cover-button"
        onClick={() => onOpen(book)}
        aria-label={`查看《${book.title}》详情`}
      >
        <div className={`book-cover tone-${book.tone}`}>
          <img src={book.cover} alt={`${book.title}封面`} loading="lazy" />
          <span className="book-badge">{book.badge}</span>
        </div>
      </button>
      <div className="book-info">
        <div>
          <span className="book-category">{book.categoryLabel}</span>
          <h3>{book.title}</h3>
          <p>{book.author}</p>
        </div>
        <div className="book-rating">
          <Star size={14} fill="currentColor" /> {book.rating}
        </div>
      </div>
      <div className="book-bottom">
        <div>
          <strong>¥{book.price}</strong>
          <del>¥{book.originalPrice}</del>
        </div>
        <div className="book-card-actions">
          <button
            className={`heart-button ${isFavorite ? "is-favorite" : ""}`}
            onClick={onFavorite}
            aria-label={isFavorite ? `取消收藏《${book.title}》` : `收藏《${book.title}》`}
          >
            <Heart size={16} fill={isFavorite ? "currentColor" : "none"} />
          </button>
          <button
            className="add-button"
            onClick={() => onAdd(book)}
            aria-label={`将《${book.title}》加入书袋`}
          >
            <Plus size={18} />
          </button>
        </div>
      </div>
    </article>
  );
}
const emptyAdminBook = {
  title: "",
  author: "",
  category: "literature",
  categoryLabel: "文学小说",
  price: "",
  originalPrice: "",
  rating: "5",
  stock: "0",
  badge: "",
  description: "",
  cover: "",
  coverData: "",
  tone: "ink",
};

function AdminView({ authUser, tab, onTabChange }) {
  const [overview, setOverview] = useState(null);
  const [books, setBooks] = useState([]);
  const [orders, setOrders] = useState([]);
  const [users, setUsers] = useState([]);
  const [posts, setPosts] = useState([]);
  const [reviews, setReviews] = useState([]);
  const [adminAnalytics, setAdminAnalytics] = useState(null);
  const [inventory, setInventory] = useState([]);
  const [bookForm, setBookForm] = useState(null);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const token = getSessionToken;
  const adminFetch = async (url, options = {}) => {
    const response = await fetch(url, {
      ...options,
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token()}`,
        ...(options.headers || {}),
      },
    });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(data.message || "请求失败");
    return data;
  };
  const loadTab = async (nextTab = tab) => {
    try {
      setError("");
      if (nextTab === "overview") setOverview(await adminFetch("/api/admin/overview"));
      if (nextTab === "books") setBooks(await adminFetch("/api/admin/books"));
      if (nextTab === "orders") setOrders(await adminFetch("/api/admin/orders"));
      if (nextTab === "users") setUsers(await adminFetch("/api/admin/users"));
      if (nextTab === "community") {
        const [nextPosts, nextReviews] = await Promise.all([
          adminFetch("/api/admin/posts"),
          adminFetch("/api/admin/reviews"),
        ]);
        setPosts(nextPosts);
        setReviews(nextReviews);
      }
      if (nextTab === "analytics") setAdminAnalytics(await adminFetch("/api/admin/analytics"));
      if (nextTab === "inventory") setInventory(await adminFetch("/api/admin/inventory"));
    } catch (err) {
      setError(err.message || "后台数据加载失败");
    }
  };
  // The selected admin tab is controlled by App (and also by the admin rail).
  // Load data whenever that shared selection changes so both navigation bars
  // behave consistently. Previously the rail changed `tab` without invoking
  // `loadTab`, leaving users/orders/community panels empty.
  useEffect(() => {
    loadTab(tab);
  }, [tab]);
  const changeTab = (nextTab) => {
    onTabChange(nextTab);
  };
  const saveBook = async (event) => {
    event.preventDefault();
    setSaving(true);
    try {
      const data = bookForm.id
        ? await adminFetch(`/api/admin/books/${bookForm.id}`, {
            method: "PATCH",
            body: JSON.stringify(bookForm),
          })
        : await adminFetch("/api/admin/books", { method: "POST", body: JSON.stringify(bookForm) });
      setBooks((current) =>
        bookForm.id
          ? current.map((book) => (book.id === data.id ? data : book))
          : [data, ...current],
      );
      setBookForm(null);
    } catch (err) {
      setError(err.message || "图书保存失败");
    } finally {
      setSaving(false);
    }
  };
  const removeBook = async (book) => {
    if (!window.confirm(`确定删除《${book.title}》吗？`)) return;
    try {
      await adminFetch(`/api/admin/books/${book.id}`, { method: "DELETE" });
      setBooks((current) => current.filter((item) => item.id !== book.id));
    } catch (err) {
      setError(err.message || "图书删除失败");
    }
  };
  const changeOrderStatus = async (orderId, status) => {
    try {
      const next = await adminFetch(`/api/admin/orders/${orderId}/status`, {
        method: "PATCH",
        body: JSON.stringify({ status }),
      });
      setOrders((current) =>
        current.map((order) => (order.id === next.id ? { ...order, ...next } : order)),
      );
    } catch (err) {
      setError(err.message || "订单状态更新失败");
    }
  };
  const removePost = async (post) => {
    if (!window.confirm(`确定删除这条讨论吗？`)) return;
    try {
      await adminFetch(`/api/admin/posts/${post.id}`, { method: "DELETE" });
      setPosts((current) => current.filter((item) => item.id !== post.id));
    } catch (err) {
      setError(err.message || "帖子删除失败");
    }
  };
  const togglePostPin = async (post) => {
    try {
      const data = await adminFetch(`/api/admin/posts/${post.id}/pin`, { method: "PATCH", body: JSON.stringify({ pinned: !post.pinned }) });
      setPosts((current) => current.map((item) => item.id === post.id ? { ...item, pinned: data.pinned } : item));
    } catch (err) { setError(err.message || "帖子置顶更新失败"); }
  };
  const removeReview = async (review) => {
    if (!window.confirm("确定删除这条评论吗？")) return;
    try {
      await adminFetch(`/api/admin/reviews/${review.id}`, { method: "DELETE" });
      setReviews((current) => current.filter((item) => item.id !== review.id));
    } catch (err) {
      setError(err.message || "评论删除失败");
    }
  };
  const saveInventory = async (item, payload) => {
    try {
      const next = await adminFetch(`/api/admin/inventory/${item.id}`, { method: "PATCH", body: JSON.stringify(payload) });
      setInventory((current) => current.map((entry) => entry.id === next.id ? next : entry));
    } catch (err) {
      setError(err.message || "库存更新失败");
    }
  };
  const tabs = [
    ["overview", "总览", LayoutDashboard],
    ["books", "图书管理", Library],
    ["orders", "订单管理", Package],
    ["users", "用户管理", Users],
    ["community", "社区审核", MessageCircle],
    ["analytics", "经营分析", BarChart3],
    ["inventory", "库存管理", PackageCheck],
  ];
  return (
    <main className="admin-page container">
      <div className="admin-head">
        <div>
          <span className="section-kicker">ADMIN WORKSPACE</span>
          <h1>管理员工作台</h1>
          <p>管理书目、订单、用户与书友社区内容。</p>
        </div>
        <div className="admin-head-actions">
          <span>{authUser.name}</span>
          <span className="admin-role admin-role-admin">管理员端</span>
        </div>
      </div>
      <nav className="admin-tabs">
        {tabs.map(([id, label, Icon]) => (
          <button key={id} className={tab === id ? "is-active" : ""} onClick={() => changeTab(id)}>
            <Icon size={16} /> {label}
          </button>
        ))}
      </nav>
      {error && <div className="admin-error">{error}</div>}
      {tab === "overview" && overview && (
        <div className="admin-overview">
          <div className="admin-stat-grid">
            {[
              ["书籍总数", overview.books, Library],
              ["注册用户", overview.users, Users],
              ["订单总数", overview.orders, Package],
              ["讨论帖子", overview.posts, MessageCircle],
              ["累计销售额", `¥${overview.revenue.toFixed(2)}`, ShoppingBag],
            ].map(([label, value, Icon]) => (
              <div className="admin-stat-card" key={label}>
                <Icon size={17} />
                <span>{label}</span>
                <strong>{value}</strong>
              </div>
            ))}
          </div>
          <div className="admin-overview-note">
            <span className="section-kicker">数据来源</span>
            <h2>MySQL 已连接</h2>
            <p>当前后台统计、图书、用户、订单和社区内容均从 bookstore 数据库读取。</p>
          </div>
        </div>
      )}
      {tab === "books" && (
        <section className="admin-section">
          <div className="admin-section-head">
            <div>
              <span className="section-kicker">BOOK CATALOGUE</span>
              <h2>图书管理</h2>
            </div>
            <button
              className="button button-dark"
              onClick={() => setBookForm({ ...emptyAdminBook })}
            >
              <Plus size={15} /> 新增图书
            </button>
          </div>
          {bookForm && (
            <form className="admin-book-form" onSubmit={saveBook}>
              <div className="admin-form-title">
                <strong>{bookForm.id ? "编辑图书" : "新增图书"}</strong>
                <button type="button" onClick={() => setBookForm(null)}>
                  取消
                </button>
              </div>
              <div className="admin-form-grid">
                {[
                  ["title", "书名"],
                  ["author", "作者"],
                  ["categoryLabel", "分类名称"],
                  ["badge", "标签"],
                  ["price", "售价"],
                  ["originalPrice", "原价"],
                  ["rating", "评分"],
                  ["stock", "库存"],
                  ["cover", "封面地址"],
                  ["tone", "色调"],
                ].map(([field, label]) => (
                  <label key={field}>
                    {label}
                    <input
                      required={!["badge", "cover", "tone"].includes(field)}
                      type={
                        ["price", "originalPrice", "rating", "stock"].includes(field)
                          ? "number"
                          : "text"
                      }
                      step={field === "rating" ? "0.1" : "1"}
                      value={bookForm[field]}
                      onChange={(event) =>
                        setBookForm({ ...bookForm, [field]: event.target.value })
                      }
                    />
                  </label>
                ))}
                <label>
                  分类代码
                  <select
                    value={bookForm.category}
                    onChange={(event) => setBookForm({ ...bookForm, category: event.target.value })}
                  >
                    <option value="literature">literature</option>
                    <option value="life">life</option>
                    <option value="design">design</option>
                    <option value="social">social</option>
                  </select>
                </label>
                <label>
                  上传封面图片
                  <input
                    type="file"
                    accept="image/*"
                    onChange={(event) => {
                      const file = event.target.files?.[0];
                      if (!file) return;
                      if (file.size > 3 * 1024 * 1024) {
                        setError("封面图片不能超过 3MB");
                        return;
                      }
                      const reader = new FileReader();
                      reader.onload = () => setBookForm((current) => ({ ...current, coverData: String(reader.result || "") }));
                      reader.readAsDataURL(file);
                    }}
                  />
                  <small className="field-hint">支持 JPG、PNG，图片会保存到 MySQL</small>
                </label>
                <label className="admin-form-wide">
                  简介
                  <textarea
                    required
                    value={bookForm.description}
                    onChange={(event) =>
                      setBookForm({ ...bookForm, description: event.target.value })
                    }
                    rows="3"
                  />
                </label>
              </div>
              <button className="button button-dark" disabled={saving}>
                {saving ? "保存中…" : "保存图书"}
              </button>
            </form>
          )}
          <div className="admin-table-wrap">
            <table className="admin-table">
              <thead>
                <tr>
                  <th>书名</th>
                  <th>分类</th>
                  <th>售价</th>
                  <th>库存</th>
                  <th>评分</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {books.map((book) => (
                  <tr key={book.id}>
                    <td>
                      <strong>{book.title}</strong>
                      <small>{book.author}</small>
                    </td>
                    <td>{book.categoryLabel}</td>
                    <td>¥{book.price}</td>
                    <td>{book.stock}</td>
                    <td>{book.rating}</td>
                    <td>
                      <button
                        className="admin-icon-button"
                        onClick={() => setBookForm({ ...book })}
                      >
                        <Pencil size={15} />
                      </button>
                      <button className="admin-icon-button danger" onClick={() => removeBook(book)}>
                        <Trash2 size={15} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
      {tab === "orders" && <AdminOrders orders={orders} onStatusChange={changeOrderStatus} />}
      {tab === "users" && <AdminUsers users={users} />}
      {tab === "community" && (
        <AdminCommunity
          posts={posts}
          reviews={reviews}
          onRemovePost={removePost}
          onTogglePin={togglePostPin}
          onRemoveReview={removeReview}
        />
      )}
      {tab === "analytics" && adminAnalytics && <AdminAnalytics analytics={adminAnalytics} />}
      {tab === "inventory" && <AdminInventory inventory={inventory} onSave={saveInventory} />}
    </main>
  );
}

function AdminOrders({ orders, onStatusChange }) {
  return (
    <section className="admin-section">
      <div className="admin-section-head">
        <div>
          <span className="section-kicker">ORDER CENTER</span>
          <h2>订单管理</h2>
        </div>
        <span className="muted-label">共 {orders.length} 笔</span>
      </div>
      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>订单</th>
              <th>客户</th>
              <th>商品</th>
              <th>金额</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((order) => (
              <tr key={order.id}>
                <td>
                  <strong>#{order.id}</strong>
                  <small>{new Date(order.createdAt).toLocaleString("zh-CN")}</small>
                </td>
                <td>
                  {order.customerName}
                  <small>{order.customerPhone}</small>
                </td>
                <td>{order.items || "无明细"}</td>
                <td>¥{order.total.toFixed(2)}</td>
                <td>
                  <select
                    value={order.status}
                    onChange={(event) => onStatusChange(order.id, event.target.value)}
                  >
                    <option>待发货</option>
                    <option>配送中</option>
                    <option>已完成</option>
                    <option>已取消</option>
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!orders.length && <div className="admin-empty">还没有订单</div>}
      </div>
    </section>
  );
}

function AdminUsers({ users }) {
  return (
    <section className="admin-section">
      <div className="admin-section-head">
        <div>
          <span className="section-kicker">USER DIRECTORY</span>
          <h2>用户管理</h2>
        </div>
        <span className="muted-label">共 {users.length} 人</span>
      </div>
      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>用户</th>
              <th>邮箱</th>
              <th>角色</th>
              <th>积分</th>
              <th>注册时间</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id}>
                <td>
                  <strong>{user.name}</strong>
                </td>
                <td>{user.email}</td>
                <td>
                  <span className={`admin-role admin-role-${user.role}`}>{user.roleLabel}</span>
                </td>
                <td>{user.points}</td>
                <td>{new Date(user.createdAt).toLocaleDateString("zh-CN")}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function AdminCommunity({ posts, reviews, onRemovePost, onRemoveReview, onTogglePin }) {
  return (
    <section className="admin-section">
      <div className="admin-section-head">
        <div>
          <span className="section-kicker">COMMUNITY REVIEW</span>
          <h2>社区审核</h2>
        </div>
      </div>
      <div className="admin-community-grid">
        <div>
          <h3>讨论帖子</h3>
          {posts.map((post) => (
            <article className="admin-content-row" key={post.id}>
              <div>
                <strong>{post.title}</strong>
                <small>
                  {post.author} · {post.category} · {post.likes} 赞 · {post.comments} 回复
                </small>
              </div>
              <div className="admin-content-actions"><button className="admin-text-button" onClick={() => onTogglePin(post)}>{post.pinned ? "取消置顶" : "置顶"}</button><button className="admin-icon-button danger" onClick={() => onRemovePost(post)}><Trash2 size={15} /></button></div>
            </article>
          ))}
        </div>
        <div>
          <h3>书籍评论</h3>
          {reviews.map((review) => (
            <article className="admin-content-row" key={review.id}>
              <div>
                <strong>
                  《{review.bookTitle}》 · {review.rating} 分
                </strong>
                <small>
                  {review.user}：{review.content}
                </small>
              </div>
              <button className="admin-icon-button danger" onClick={() => onRemoveReview(review)}>
                <Trash2 size={15} />
              </button>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}

function FeatureHeader({ eyebrow, title, description, action }) {
  return <div className="feature-head"><div><span className="section-kicker">{eyebrow}</span><h1>{title}</h1><p>{description}</p></div>{action}</div>;
}

function SecondhandView({ listings, viewerId, onPublish, onBuy }) {
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({ title: "", author: "", condition: "九成新", price: "", description: "", cover: "", coverData: "" });
  const submit = (event) => { event.preventDefault(); onPublish(form); setForm({ title: "", author: "", condition: "九成新", price: "", description: "", cover: "", coverData: "" }); setOpen(false); };
  return <main className="feature-page container">
    <FeatureHeader eyebrow="SECONDHAND BOOK MARKET" title="二手书市" description="让读完的书继续流动，和书友交换下一段阅读。" action={<button className="button button-dark" onClick={() => setOpen(true)}><Plus size={15} /> 发布闲置书</button>} />
    <div className="feature-note"><ArrowLeftRight size={18} /><span>真实交易状态来自 MySQL；购买后会锁定商品，双方可通过站内联系方式完成交接。</span></div>
    <div className="feature-grid secondhand-grid">{listings.map((item) => <article className="feature-card secondhand-card" key={item.id}>
      {item.cover ? <img src={item.cover} alt="" /> : <div className="feature-cover"><BookOpen size={24} /></div>}
      <div className="feature-card-body"><div className="feature-card-top"><span className="status-pill">{item.condition}</span><span className={`status-pill ${item.status === "active" ? "status-active" : "status-muted"}`}>{item.status === "active" ? "在售" : "已售出"}</span></div><h3>{item.title}</h3><p className="muted-label">{item.author} · 卖家 {item.seller}</p><p>{item.description || "书友留下的阅读痕迹，等待下一位读者。"}</p><div className="feature-card-bottom"><strong>¥{item.price.toFixed(2)}</strong>{item.status === "active" && item.sellerId !== viewerId ? <button className="button button-outline" onClick={() => onBuy(item)}>联系购买</button> : <span className="muted-label">{item.sellerId === viewerId ? "我的发布" : "交易完成"}</span>}</div></div>
    </article>)}{!listings.length && <div className="empty-state"><ArrowLeftRight size={28} /><h3>还没有二手书</h3><p>发布第一本闲置书，让它遇到下一位读者。</p></div>}</div>
    {open && <div className="overlay" onClick={() => setOpen(false)}><div className="form-modal" onClick={(event) => event.stopPropagation()}><div className="drawer-head"><div><span className="section-kicker">SECONDHAND</span><h2>发布闲置书</h2></div><button className="icon-button" onClick={() => setOpen(false)}><X size={20} /></button></div><form className="modal-form" onSubmit={submit}><label>书名<input required value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></label><label>作者<input required value={form.author} onChange={(e) => setForm({ ...form, author: e.target.value })} /></label><label>成色<select value={form.condition} onChange={(e) => setForm({ ...form, condition: e.target.value })}><option>全新</option><option>九成新</option><option>八成新</option><option>有明显阅读痕迹</option></select></label><label>价格<input required type="number" min="0" step="0.01" value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} /></label><label>封面地址<input value={form.cover} onChange={(e) => setForm({ ...form, cover: e.target.value })} placeholder="可选" /></label><label>上传封面图片<input type="file" accept="image/*" onChange={(e) => { const file = e.target.files?.[0]; if (!file || file.size > 3 * 1024 * 1024) return; const reader = new FileReader(); reader.onload = () => setForm((current) => ({ ...current, coverData: String(reader.result || "") })); reader.readAsDataURL(file); }} /><small className="field-hint">支持 JPG、PNG，图片会保存到 MySQL</small></label><label>说明<textarea rows="3" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} placeholder="购买渠道、批注情况等" /></label><button className="button button-dark button-wide">发布二手书</button></form></div></div>}
  </main>;
}

function ClubsView({ clubs, books, onCreate, onToggle }) {
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({ title: "", description: "", bookId: "", eventAt: "", location: "线上", capacity: 20, coverData: "" });
  const submit = (event) => { event.preventDefault(); onCreate(form); setForm({ title: "", description: "", bookId: "", eventAt: "", location: "线上", capacity: 20, coverData: "" }); setOpen(false); };
  return <main className="feature-page container"><FeatureHeader eyebrow="BOOK CLUB EVENTS" title="读书会活动" description="线上共读、线下见面，把书里的问题带到真实对话里。" action={<button className="button button-dark" onClick={() => setOpen(true)}><Plus size={15} /> 发起读书会</button>} /><div className="feature-grid">{clubs.map((club) => <article className="feature-card club-card" key={club.id}>{club.image && <img className="club-cover" src={club.image} alt="活动封面" />}<div className="club-date"><CalendarDays size={20} /><strong>{new Date(club.eventAt).toLocaleDateString("zh-CN", { month: "short", day: "numeric" })}</strong><span>{new Date(club.eventAt).toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" })}</span></div><div className="feature-card-body"><span className="section-kicker">{club.location}</span><h3>{club.title}</h3><p>{club.description}</p><div className="club-meta"><span><BookMarked size={14} /> {club.bookTitle}</span><span><Users size={14} /> {club.memberCount}/{club.capacity}</span><span>发起人 {club.host}</span></div><button className={`button ${club.joined ? "button-outline" : "button-dark"}`} onClick={() => onToggle(club)}>{club.joined ? <><CheckCircle2 size={15} /> 已报名</> : "报名参加"}</button></div></article>)}{!clubs.length && <div className="empty-state"><CalendarDays size={28} /><h3>暂时没有活动</h3><p>发起一场共读，邀请书友一起开始。</p></div>}</div>{open && <div className="overlay" onClick={() => setOpen(false)}><div className="form-modal" onClick={(event) => event.stopPropagation()}><div className="drawer-head"><div><span className="section-kicker">BOOK CLUB</span><h2>发起读书会</h2></div><button className="icon-button" onClick={() => setOpen(false)}><X size={20} /></button></div><form className="modal-form" onSubmit={submit}><label>活动标题<input required value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></label><label>共读书籍<select value={form.bookId} onChange={(e) => setForm({ ...form, bookId: e.target.value })}><option value="">自由选书</option>{books.map((book) => <option key={book.id} value={book.id}>{book.title}</option>)}</select></label><label>活动时间<input required type="datetime-local" value={form.eventAt} onChange={(e) => setForm({ ...form, eventAt: e.target.value })} /></label><label>地点或会议链接<input value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} /></label><label>人数上限<input type="number" min="1" value={form.capacity} onChange={(e) => setForm({ ...form, capacity: e.target.value })} /></label><label>上传活动图片<input type="file" accept="image/*" onChange={(e) => { const file = e.target.files?.[0]; if (!file || file.size > 3 * 1024 * 1024) return; const reader = new FileReader(); reader.onload = () => setForm((current) => ({ ...current, coverData: String(reader.result || "") })); reader.readAsDataURL(file); }} /><small className="field-hint">支持 JPG、PNG，最大 3MB</small></label><label>活动介绍<textarea required rows="3" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></label><button className="button button-dark button-wide">发布活动</button></form></div></div>}</main>;
}

function ReadingGrowthView({ challenges, leaderboard, onJoin, onSaveProgress }) {
  const [tab, setTab] = useState("challenge");
  return <main className="feature-page container"><FeatureHeader eyebrow="READING GROWTH" title="阅读成长" description="用挑战建立阅读节奏，再用榜单看看自己和书友走了多远。" /><div className="growth-tabs"><button className={tab === "challenge" ? "is-active" : ""} onClick={() => setTab("challenge")}><Target size={15} /> 挑战进度</button><button className={tab === "leaderboard" ? "is-active" : ""} onClick={() => setTab("leaderboard")}><Trophy size={15} /> 阅读榜单</button></div>{tab === "challenge" ? <ChallengesPanel challenges={challenges} onJoin={onJoin} onSaveProgress={onSaveProgress} /> : <LeaderboardPanel leaderboard={leaderboard} />}</main>;
}

function ChallengesPanel({ challenges, onJoin, onSaveProgress }) {
  return <section className="growth-panel"><div className="feature-grid challenge-grid">{challenges.map((challenge) => { const bookRate = challenge.targetBooks ? Math.min(100, challenge.booksCompleted / challenge.targetBooks * 100) : 0; const minuteRate = challenge.targetMinutes ? Math.min(100, challenge.minutes / challenge.targetMinutes * 100) : 0; return <article className="feature-card challenge-card" key={challenge.id}><div className="challenge-icon"><Trophy size={23} /></div><div className="feature-card-body"><span className="section-kicker">{challenge.startDate} — {challenge.endDate}</span><h3>{challenge.title}</h3><p>{challenge.description}</p><div className="challenge-progress"><div><span>完成书籍 {challenge.booksCompleted}/{challenge.targetBooks}</span><b>{Math.round(bookRate)}%</b></div><i><b style={{ width: `${bookRate}%` }} /></i><div><span>阅读时长 {challenge.minutes}/{challenge.targetMinutes} 分钟</span><b>{Math.round(minuteRate)}%</b></div><i><b style={{ width: `${minuteRate}%` }} /></i></div><div className="challenge-actions">{!challenge.joined ? <button className="button button-dark" onClick={() => onJoin(challenge)}><Target size={15} /> 加入挑战</button> : <ChallengeProgressForm challenge={challenge} onSave={onSaveProgress} />}</div></div></article>; })}{!challenges.length && <div className="empty-state"><Trophy size={28} /><h3>暂无进行中的挑战</h3><p>管理员发布挑战后，会在这里显示。</p></div>}</div></section>;
}

function ChallengeProgressForm({ challenge, onSave }) {
  const [books, setBooks] = useState(challenge.booksCompleted); const [minutes, setMinutes] = useState(challenge.minutes);
  return <form className="challenge-form" onSubmit={(e) => { e.preventDefault(); onSave(challenge, { booksCompleted: books, minutes }); }}><label>已完成书籍<input type="number" min="0" value={books} onChange={(e) => setBooks(e.target.value)} /></label><label>阅读分钟<input type="number" min="0" value={minutes} onChange={(e) => setMinutes(e.target.value)} /></label><button className="button button-outline"><Check size={14} /> 保存进度</button></form>;
}

function ReaderAnalyticsView({ analytics }) {
  if (!analytics) return <main className="feature-page container"><div className="empty-state"><BarChart3 size={28} /><h3>暂无阅读数据</h3><p>浏览和阅读几本书后，这里会生成你的数据。</p></div></main>;
  const maxCategory = Math.max(...analytics.categories.map((item) => item.count), 1);
  return <main className="feature-page container"><FeatureHeader eyebrow="MY READING DATA" title="我的数据" description="把阅读时间、书目偏好和年度目标放在一张清晰的图表里。" /><div className="analytics-stat-grid"><div><Clock3 size={18} /><span>阅读分钟</span><strong>{analytics.summary.minutes}</strong></div><div><BookOpenCheck size={18} /><span>读完书籍</span><strong>{analytics.summary.finished}</strong></div><div><Library size={18} /><span>收录书籍</span><strong>{analytics.summary.books}</strong></div><div><ShoppingBag size={18} /><span>购书消费</span><strong>¥{analytics.summary.spending.toFixed(2)}</strong></div></div><div className="analytics-grid"><section className="feature-card"><div className="content-heading"><div><span className="section-kicker">阅读偏好</span><h2>我在读什么</h2></div></div>{analytics.categories.length ? analytics.categories.map((item) => <div className="bar-row" key={item.label}><span>{item.label}</span><i><b style={{ width: `${item.count / maxCategory * 100}%` }} /></i><strong>{item.count}</strong></div>) : <p className="muted-label">还没有足够的书架数据。</p>}</section><section className="feature-card"><div className="content-heading"><div><span className="section-kicker">最近六个月</span><h2>浏览趋势</h2></div></div><div className="month-bars">{analytics.monthly.length ? analytics.monthly.slice().reverse().map((item) => <div key={item.month}><b style={{ height: `${Math.max(12, item.count * 18)}px` }} /><span>{item.month.slice(5)}</span></div>) : <p className="muted-label">还没有浏览记录。</p>}</div></section></div></main>;
}

function LeaderboardPanel({ leaderboard }) {
  const [tab, setTab] = useState("finished");
  const tabs = [["finished", "年度读完", "本"], ["minutes", "阅读时长", "分钟"], ["active", "最近活跃", "次浏览"]];
  const rows = leaderboard?.[tab] || [];
  return <section className="growth-panel"><div className="leaderboard-tabs">{tabs.map(([id, label]) => <button key={id} className={tab === id ? "is-active" : ""} onClick={() => setTab(id)}>{label}</button>)}</div><div className="leaderboard-list">{rows.map((item) => <article className={`leaderboard-row rank-${item.rank}`} key={item.id}><strong className="leaderboard-rank">{item.rank <= 3 ? <Medal size={19} /> : item.rank}</strong><div className="leaderboard-avatar">{item.name.slice(0, 1)}</div><div className="leaderboard-user"><strong>{item.name}</strong><span>{tab === "finished" ? `阅读时长 ${item.minutes} 分钟` : tab === "minutes" ? `读完 ${item.finished} 本` : "保持阅读记录"}</span></div><b>{item.score}<small>{tabs.find(([id]) => id === tab)?.[2]}</small></b></article>)}{!rows.length && <div className="empty-state"><Trophy size={28} /><h3>还没有排行榜数据</h3><p>开始记录阅读后，就会出现在这里。</p></div>}</div></section>;
}

function AdminAnalytics({ analytics }) {
  const totalRevenue = analytics.sales.reduce((sum, item) => sum + item.revenue, 0);
  return <section className="admin-section"><div className="admin-section-head"><div><span className="section-kicker">BUSINESS ANALYTICS</span><h2>经营分析</h2></div><span className="muted-label">来自 MySQL 实时聚合</span></div><div className="analytics-stat-grid"><div><ShoppingBag size={18} /><span>近六个月销售额</span><strong>¥{totalRevenue.toFixed(2)}</strong></div><div><Package size={18} /><span>读书会活动</span><strong>{analytics.clubs}</strong></div><div><ArrowLeftRight size={18} /><span>二手书在售</span><strong>{analytics.secondhand.find((item) => item.status === "active")?.count || 0}</strong></div></div><div className="analytics-grid"><section className="feature-card"><div className="content-heading"><div><span className="section-kicker">热销图书</span><h2>销售排行</h2></div></div>{analytics.topBooks.length ? analytics.topBooks.map((item, index) => <div className="bar-row" key={item.title}><span>{index + 1}. {item.title}</span><i><b style={{ width: `${Math.min(100, item.quantity * 15)}%` }} /></i><strong>{item.quantity}</strong></div>) : <p className="muted-label">暂无订单数据。</p>}</section><section className="feature-card"><div className="content-heading"><div><span className="section-kicker">阅读活跃度</span><h2>浏览趋势</h2></div></div>{analytics.activity.map((item) => <div className="bar-row" key={item.month}><span>{item.month}</span><i><b style={{ width: `${Math.min(100, item.views * 10)}%` }} /></i><strong>{item.views}</strong></div>)}</section></div></section>;
}

function AdminInventory({ inventory, onSave }) {
  const [drafts, setDrafts] = useState({});
  const draft = (item) => drafts[item.id] || { stock: item.stock, status: item.status, reason: "库存调整" };
  return <section className="admin-section"><div className="admin-section-head"><div><span className="section-kicker">INVENTORY CONTROL</span><h2>库存管理</h2></div><span className="muted-label">低于 5 本自动标记预警</span></div><div className="inventory-summary"><strong>{inventory.filter((item) => item.lowStock).length}</strong><span>本书需要补货</span><strong>{inventory.filter((item) => item.status === "paused").length}</strong><span>本书已暂停销售</span></div><div className="admin-table-wrap"><table className="admin-table inventory-table"><thead><tr><th>图书</th><th>当前库存</th><th>销售状态</th><th>调整原因</th><th>操作</th></tr></thead><tbody>{inventory.map((item) => { const value = draft(item); return <tr key={item.id}><td><strong>{item.title}</strong><small>{item.author}</small></td><td><input className={item.lowStock ? "inventory-stock low" : "inventory-stock"} type="number" min="0" value={value.stock} onChange={(e) => setDrafts((current) => ({ ...current, [item.id]: { ...value, stock: e.target.value } }))} /><small>{item.lowStock ? "库存预警" : `已调整 ${item.changes} 次`}</small></td><td><select value={value.status} onChange={(e) => setDrafts((current) => ({ ...current, [item.id]: { ...value, status: e.target.value } }))}><option value="active">正常销售</option><option value="paused">暂停销售</option><option value="sold_out">售罄</option></select></td><td><input value={value.reason} onChange={(e) => setDrafts((current) => ({ ...current, [item.id]: { ...value, reason: e.target.value } }))} /></td><td><button className="button button-outline inventory-save" onClick={() => onSave(item, value)}>保存库存</button></td></tr>; })}</tbody></table>{!inventory.length && <div className="admin-empty">暂无库存数据</div>}</div></section>;
}

function ForumView({ posts, category, setCategory, search, setSearch, onNew, onOpen, onLike, onReport, onFollowAuthor, onOpenAuthor, viewerId }) {
  const [feed, setFeed] = useState("推荐");
  const [likedPosts, setLikedPosts] = useState([]);
  const [savedPosts, setSavedPosts] = useState([]);
  const [followedTopics, setFollowedTopics] = useState(["阅读分享", "书目讨论"]);
  const feedTabs = ["推荐", "最新", "热榜", "关注"];
  const topics = [
    { label: "年度书单", count: 128 },
    { label: "通勤阅读", count: 86 },
    { label: "设计与生活", count: 64 },
    { label: "同城共读", count: 42 },
  ];
  const toggleTopic = (topic) =>
    setFollowedTopics((current) =>
      current.includes(topic) ? current.filter((item) => item !== topic) : [...current, topic],
    );
  const visiblePosts = [...posts]
    .filter(
      (post) =>
        feed !== "关注" ||
        followedTopics.some((topic) => post.category === topic || post.tags?.includes(topic)),
    )
    .sort((a, b) => {
      if (feed === "最新") return new Date(b.createdAt) - new Date(a.createdAt);
      if (feed === "热榜") return b.likes + b.comments * 2 - (a.likes + a.comments * 2);
      return 0;
    });
  const emptyText =
    feed === "关注"
      ? "先关注几个话题，首页会出现对应讨论。"
      : "换个关键词，或者成为第一个发起讨论的人。";
  return (
    <main className="forum-page container">
      <div className="forum-hero">
        <div>
          <span className="section-kicker">
            <Users size={15} /> 纸灯书友会
          </span>
          <h1>
            读完一本书，
            <br />
            <em>再和人聊聊。</em>
          </h1>
          <p>
            找到同频书友，收藏有启发的回答，
            <br />
            也可以发起一场你想参加的读书会。
          </p>
        </div>
        <div className="forum-hero-stats">
          <strong>2,418</strong>
          <span>位书友正在阅读</span>
          <strong>86</strong>
          <span>场进行中的讨论</span>
        </div>
      </div>
      <div className="forum-community-strip">
        <div className="forum-community-metrics">
          <div>
            <span>今日新帖</span>
            <strong>36</strong>
          </div>
          <div>
            <span>活跃书友</span>
            <strong>128</strong>
          </div>
          <div>
            <span>本周共读</span>
            <strong>12</strong>
          </div>
        </div>
        <div className="forum-hot-topics">
          <span>正在讨论</span>
          {topics.slice(0, 3).map((topic) => (
            <button key={topic.label} onClick={() => setSearch(topic.label)}>
              # {topic.label}
            </button>
          ))}
        </div>
      </div>
      <div className="forum-compose">
        <div className="post-avatar">周</div>
        <button className="forum-compose-input" onClick={onNew}>
          分享一本刚读完的书，或者提出一个问题…
        </button>
        <button className="button button-dark" onClick={onNew}>
          <Plus size={16} /> 发起讨论
        </button>
      </div>
      <div className="forum-toolbar">
        <div className="forum-feed-tabs">
          {feedTabs.map((item) => (
            <button
              key={item}
              className={feed === item ? "is-active" : ""}
              onClick={() => setFeed(item)}
            >
              {item}
              {item === "关注" && <small>{followedTopics.length}</small>}
            </button>
          ))}
        </div>
        <div className="forum-actions">
          <div className="search-wrap">
            <Search size={17} />
            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="搜索讨论、书名或话题"
            />
          </div>
          <button className="button button-dark" onClick={onNew}>
            <Plus size={16} /> 发起讨论
          </button>
        </div>
      </div>
      <div className="forum-category-bar">
        <span>话题分类</span>
        <div className="forum-tabs">
          {forumCategories.map((item) => (
            <button
              key={item}
              className={category === item ? "is-active" : ""}
              onClick={() => setCategory(item)}
            >
              {item}
            </button>
          ))}
        </div>
      </div>
      <div className="forum-layout">
        <div className="post-list">
          {visiblePosts.map((post) => (
            <article className="post-card" key={post.id}>
              <div className="post-avatar">{post.avatar}</div>
              <div className="post-main">
                <div className="post-meta">
                  <span>{post.category}</span>
                  <time>
                    <Clock3 size={13} /> {dateText(post.createdAt)}
                  </time>
                </div>
                <button className="post-title-button" onClick={() => onOpen(post)}>
                  {post.title}
                </button>
                <p>{post.excerpt}</p>
                <div className="post-footer">
                  <div className="post-author">
                    {post.authorId ? (
                      <button className="author-link" onClick={() => onOpenAuthor(post.authorId)}>{post.author}</button>
                    ) : post.author}
                    {post.authorId && post.authorId !== viewerId && (
                      <button className="author-follow" onClick={() => onFollowAuthor(post.authorId)}>
                        <UserPlus size={12} /> 关注
                      </button>
                    )}
                    {post.tags.map((tag) => (
                      <b key={tag}>{tag}</b>
                    ))}
                  </div>
                  <div className="post-actions">
                    <button
                      className={
                        likedPosts.includes(post.id) ? "post-action is-active" : "post-action"
                      }
                      onClick={() => {
                        setLikedPosts((current) => current.includes(post.id) ? current.filter((id) => id !== post.id) : [...current, post.id]);
                        onLike(post);
                      }}
                    >
                      <ThumbsUp size={14} /> {post.likes + (likedPosts.includes(post.id) ? 1 : 0)}
                    </button>
                    <button className="post-action" onClick={() => onOpen(post)}>
                      <MessageCircle size={14} /> {post.comments}
                    </button>
                    <button
                      className={
                        savedPosts.includes(post.id) ? "post-action is-active" : "post-action"
                      }
                      onClick={() =>
                        setSavedPosts((current) =>
                          current.includes(post.id)
                            ? current.filter((id) => id !== post.id)
                            : [...current, post.id],
                        )
                      }
                    >
                      <Bookmark size={14} /> {savedPosts.includes(post.id) ? "已收藏" : "收藏"}
                    </button>
                    <button className="post-action" onClick={() => onReport(post)}>
                      <Flag size={14} /> 举报
                    </button>
                  </div>
                </div>
              </div>
            </article>
          ))}
          {!visiblePosts.length && (
            <div className="empty-state">
              <MessageCircle size={28} />
              <h3>还没有相关讨论</h3>
              <p>{emptyText}</p>
              <button className="button button-outline" onClick={onNew}>
                发起讨论
              </button>
            </div>
          )}
        </div>
        <aside className="forum-aside">
          <div className="aside-box forum-hot-list">
            <div className="forum-aside-heading">
              <span className="section-kicker">热榜</span>
              <small>实时更新</small>
            </div>
            <h3>大家正在聊</h3>
            {[...posts]
              .sort((a, b) => b.likes - a.likes)
              .slice(0, 3)
              .map((post, index) => (
                <button className="hot-post" key={post.id} onClick={() => onOpen(post)}>
                  <b>0{index + 1}</b>
                  <span>
                    {post.title}
                    <small>
                      {post.likes} 人赞同 · {post.comments} 条回应
                    </small>
                  </span>
                </button>
              ))}
          </div>
          <div className="aside-box forum-topic-box">
            <div className="forum-aside-heading">
              <span className="section-kicker">关注话题</span>
              <small>{followedTopics.length} 个</small>
            </div>
            {topics.map((topic) => (
              <div className="topic-row" key={topic.label}>
                <span>
                  <strong># {topic.label}</strong>
                  <small>{topic.count} 条讨论</small>
                </span>
                <button
                  className={
                    followedTopics.includes(topic.label)
                      ? "topic-follow is-followed"
                      : "topic-follow"
                  }
                  onClick={() => toggleTopic(topic.label)}
                >
                  {followedTopics.includes(topic.label) ? "已关注" : "关注"}
                </button>
              </div>
            ))}
          </div>
          <div className="aside-box aside-green">
            <Bookmark size={20} />
            <h3>社区公约</h3>
            <p>尊重每一种阅读经验，给具体的观点留下空间。</p>
          </div>
        </aside>
      </div>
    </main>
  );
}
function ProfileView({
  profile,
  books,
  tab,
  setTab,
  onEdit,
  onAddress,
  onRemoveAddress,
  onOpenBook,
  reading,
  onUpdateReading,
}) {
  const favoriteBooks = books.filter((book) => profile.favorites?.includes(book.id));
  return (
    <main className="profile-page container">
      <div className="profile-head">
        <div className="profile-avatar">
          <UserCircle size={42} />
        </div>
        <div className="profile-intro">
          <span className="section-kicker">我的纸灯</span>
          <h1>{profile.name}</h1>
          <p>{profile.bio}</p>
          <small>{profile.email} · 加入纸灯书店以来，已读过很多个夜晚</small>
        </div>
        <button className="button button-outline" onClick={onEdit}>
          <Pencil size={15} /> 编辑资料
        </button>
      </div>
      <div className="profile-stats">
        <div>
          <strong>{profile.orders?.length || 0}</strong>
          <span>已购订单</span>
        </div>
        <div>
          <strong>{favoriteBooks.length}</strong>
          <span>收藏书籍</span>
        </div>
        <div>
          <strong>{profile.points || 0}</strong>
          <span>纸灯积分</span>
        </div>
        <div>
          <strong>12</strong>
          <span>发表讨论</span>
        </div>
      </div>
      <div className="profile-layout">
        <aside className="profile-nav">
          {[
            ["orders", Package, "我的订单"],
            ["favorites", Heart, "收藏夹"],
            ["addresses", MapPin, "收货地址"],
            ["reading", BookMarked, "阅读书架"],
          ].map(([id, Icon, label]) => (
            <button key={id} className={tab === id ? "is-active" : ""} onClick={() => setTab(id)}>
              <Icon size={17} /> {label}
            </button>
          ))}
        </aside>
        <section className="profile-content">
          {tab === "orders" && (
            <div>
              <div className="content-heading">
                <div>
                  <span className="section-kicker">购买记录</span>
                  <h2>最近的阅读补给</h2>
                </div>
                <span className="muted-label">共 {profile.orders?.length || 0} 笔</span>
              </div>
              <div className="order-list">
                {profile.orders?.map((order) => (
                  <article className="order-card" key={order.id}>
                    <div className="order-head">
                      <span>订单号 {order.id}</span>
                      <b>{order.status}</b>
                    </div>
                    <div className="order-body">
                      <div className="order-books">
                        {order.items.map((item) => (
                          <span key={item.title}>
                            {item.title} × {item.quantity}
                          </span>
                        ))}
                      </div>
                      <div className="order-price">
                        <small>{order.createdAt}</small>
                        <strong>¥{order.total}</strong>
                      </div>
                    </div>
                    <button className="text-link">
                      再次购买 <ArrowRight size={14} />
                    </button>
                  </article>
                ))}
              </div>
            </div>
          )}
          {tab === "favorites" && (
            <div>
              <div className="content-heading">
                <div>
                  <span className="section-kicker">收藏夹</span>
                  <h2>想读的，先留在这里</h2>
                </div>
                <span className="muted-label">{favoriteBooks.length} 本书</span>
              </div>
              <div className="favorite-grid">
                {favoriteBooks.map((book) => (
                  <button className="favorite-card" key={book.id} onClick={() => onOpenBook(book)}>
                    <img src={book.cover} alt={book.title} />
                    <div>
                      <span>{book.categoryLabel}</span>
                      <h3>{book.title}</h3>
                      <p>{book.author}</p>
                    </div>
                  </button>
                ))}
                {!favoriteBooks.length && (
                  <div className="empty-state">
                    <Heart size={28} />
                    <h3>收藏夹还是空的</h3>
                    <p>在书架里点击心形，保存想读的书。</p>
                  </div>
                )}
              </div>
            </div>
          )}
          {tab === "addresses" && (
            <div>
              <div className="content-heading">
                <div>
                  <span className="section-kicker">收货地址</span>
                  <h2>把书寄到你身边</h2>
                </div>
                <button className="button button-dark" onClick={onAddress}>
                  <Plus size={16} /> 新增地址
                </button>
              </div>
              <div className="address-list">
                {profile.addresses?.map((address) => (
                  <article className="address-card" key={address.id}>
                    <div className="address-label">
                      {address.label}
                      {address.isDefault && <b>默认</b>}
                    </div>
                    <h3>
                      {address.receiver} <span>{address.phone}</span>
                    </h3>
                    <p>{address.detail}</p>
                    <button
                      className="text-link danger-link"
                      onClick={() => onRemoveAddress(address.id)}
                    >
                      <X size={14} /> 删除
                    </button>
                  </article>
                ))}
              </div>
            </div>
          )}
          {tab === "reading" && (
            <ReadingShelfPanel reading={reading} books={books} onUpdate={onUpdateReading} onOpenBook={onOpenBook} />
          )}
        </section>
      </div>
    </main>
  );
}
function ReadingShelfPanel({ reading, onUpdate, onOpenBook }) {
  const sections = [["want", "想读", Bookmark], ["reading", "在读", BookOpenCheck], ["read", "已读", Check]];
  const stats = reading?.stats || {};
  return (
    <div className="reading-panel">
      <div className="content-heading"><div><span className="section-kicker">PERSONAL READING</span><h2>我的阅读书架</h2></div><span className="muted-label">记录每一次翻页</span></div>
      <div className="reading-stat-grid"><div><BookOpenCheck size={18} /><strong>{stats.finished || 0}</strong><span>本年度读完</span></div><div><Clock3 size={18} /><strong>{stats.minutes || 0}</strong><span>阅读分钟</span></div><div><BarChart3 size={18} /><strong>{stats.books || 0}</strong><span>书架藏书</span></div></div>
      {sections.map(([status, label, Icon]) => <section className="reading-shelf-section" key={status}><div className="reading-section-head"><h3><Icon size={16} /> {label}</h3><span>{reading?.shelves?.[status]?.length || 0} 本</span></div><div className="reading-book-grid">{(reading?.shelves?.[status] || []).map((book) => <article key={book.id} className="reading-book-card"><button onClick={() => onOpenBook(book)}><img src={book.cover} alt="" /><span>{book.title}</span><small>{book.author}</small></button>{status === "reading" && <><div className="reading-progress"><b style={{ width: `${book.progress}%` }} /></div><small>{book.progress}% · {book.minutes} 分钟</small></>}{status !== "read" && <select value={status} onChange={(e) => onUpdate(book.id, { status: e.target.value, progress: e.target.value === "read" ? 100 : book.progress })}><option value="want">移到想读</option><option value="reading">移到在读</option><option value="read">标记已读</option></select>}</article>)}</div>{!(reading?.shelves?.[status] || []).length && <p className="muted-label reading-empty">从书籍详情页加入“想读”，开始建立自己的阅读轨迹。</p>}</section>)}
      <section className="reading-history"><div className="reading-section-head"><h3><Eye size={16} /> 最近浏览</h3><span>最多显示 12 本</span></div><div className="history-list">{(reading?.history || []).map((book) => <button key={book.id} onClick={() => onOpenBook(book)}><img src={book.cover} alt="" /><span>{book.title}</span><small>{dateText(book.viewedAt)}</small></button>)}</div></section>
      <div className="annual-reading-card"><div><span className="section-kicker">YEAR IN BOOKS</span><h3>{stats.year || new Date().getFullYear()} 年度书单</h3><p>已读 {stats.finished || 0} 本 · 共投入 {stats.minutes || 0} 分钟</p></div><button className="button button-dark" onClick={() => window.print()}><BarChart3 size={15} /> 生成年度书单</button></div>
    </div>
  );
}
function BookDetailModal({ book, reviews, isFavorite, onFavorite, onAdd, onClose, onReview, canReview, reviewSort, onReviewSort, onLikeReview, reviewComments, onLoadReviewComments, onSubmitReviewComment, books, onOpenRelated, onUpdateReading }) {
  const [reviewOpen, setReviewOpen] = useState(false);
  const [detailTab, setDetailTab] = useState("intro");
  const [replyOpen, setReplyOpen] = useState(null);
  const [replyText, setReplyText] = useState("");
  const [rating, setRating] = useState(5);
  const [content, setContent] = useState("");
  const submit = (e) => {
    e.preventDefault();
    if (content.trim()) {
      onReview({ rating, content });
      setContent("");
      setReviewOpen(false);
    }
  };
  return (
    <div className="overlay" onClick={onClose}>
      <div className="book-detail-modal" onClick={(e) => e.stopPropagation()}>
        <button className="detail-close icon-button" onClick={onClose} aria-label="关闭详情">
          <X size={20} />
        </button>
        <div className="detail-top">
          <div className="detail-cover">
            <img src={book.cover} alt={`${book.title}封面`} />
          </div>
          <div className="detail-copy">
            <span className="book-category">{book.categoryLabel}</span>
            <h2>{book.title}</h2>
            <p className="detail-author">{book.author}</p>
            <div className="detail-rating">
              <Star size={16} fill="currentColor" /> {book.rating}{" "}
              <span>· {reviews.length + 18} 条读者评论</span>
            </div>
            <p className="detail-description">{book.description}</p>
            <div className="book-metadata-grid">
              <span><b>ISBN</b>{book.isbn || "待补充"}</span>
              <span><b>出版社</b>{book.publisher || "纸灯书局"}</span>
              <span><b>出版时间</b>{book.publishedAt ? new Date(book.publishedAt).toLocaleDateString("zh-CN") : "待补充"}</span>
            </div>
            <div className="detail-price">
              <strong>¥{book.price}</strong>
              <del>¥{book.originalPrice}</del>
              <span>库存 {book.stock} 本</span>
            </div>
            <div className="detail-actions">
              <button className="button button-dark" onClick={onAdd}>
                加入书袋 <ShoppingBag size={16} />
              </button>
              <button
                className={`button button-outline favorite-action ${isFavorite ? "is-favorite" : ""}`}
                onClick={onFavorite}
              >
                <Heart size={16} fill={isFavorite ? "currentColor" : "none"} />{" "}
                {isFavorite ? "已收藏" : "收藏"}
              </button>
            </div>
            <div className="reading-actions"><button onClick={() => onUpdateReading(book.id, { status: "want" })}><Bookmark size={14} /> 加入想读</button><button onClick={() => onUpdateReading(book.id, { status: "reading", progress: 1 })}><BookOpenCheck size={14} /> 开始阅读</button></div>
          </div>
        </div>
        <div className="detail-tabs">
          {[['intro', '内容简介'], ['toc', '目录'], ['excerpt', '内容试读'], ['author', '作者简介']].map(([id, label]) => (
            <button key={id} className={detailTab === id ? "is-active" : ""} onClick={() => setDetailTab(id)}>{label}</button>
          ))}
        </div>
        <div className="detail-tab-panel">
          {detailTab === "intro" && <p>{book.description}</p>}
          {detailTab === "toc" && <ol>{String(book.toc || "序章｜第一章｜第二章｜尾声").split("｜").map((item) => <li key={item}>{item}</li>)}</ol>}
          {detailTab === "excerpt" && <blockquote>{book.excerpt || book.description}<br /><small>试读内容来自出版社授权节选</small></blockquote>}
          {detailTab === "author" && <p>{book.authorBio || `${book.author}是一位持续观察生活、并把经验写进作品的作者。`}</p>}
        </div>
        <div className="similar-books">
          <div className="content-heading"><div><span className="section-kicker">继续发现</span><h3>相似书籍</h3></div></div>
          <div className="similar-book-list">{books.filter((item) => item.id !== book.id && item.category === book.category).slice(0, 3).map((item) => <button key={item.id} onClick={() => onOpenRelated(item)}><img src={item.cover} alt="" /><span>{item.title}</span><small>{item.author}</small></button>)}</div>
        </div>
        <div className="detail-review-head">
          <div>
            <span className="section-kicker">读者短评</span>
            <h3>看看别人怎么读</h3>
          </div>
          <div className="review-controls"><select value={reviewSort} onChange={(e) => onReviewSort(e.target.value)}><option value="recent">最新</option><option value="helpful">最有帮助</option><option value="rating">评分最高</option></select><button className="text-link" disabled={!canReview} onClick={() => setReviewOpen(!reviewOpen)}>{reviewOpen ? "收起评论框" : canReview ? "写一条评论" : "购买后评价"} <Pencil size={14} /></button></div>
        </div>
        <div className="rating-distribution">{[5, 4, 3, 2, 1].map((score) => { const count = reviews.filter((review) => review.rating === score).length; return <div key={score}><span>{score} 星</span><i><b style={{ width: `${reviews.length ? (count / reviews.length) * 100 : 0}%` }} /></i><small>{count}</small></div>; })}</div>
        {reviewOpen && (
          <form className="review-form" onSubmit={submit}>
            <div className="star-input">
              <span>给这本书打分</span>
              {[1, 2, 3, 4, 5].map((item) => (
                <button
                  type="button"
                  key={item}
                  onClick={() => setRating(item)}
                  aria-label={`${item}星`}
                >
                  <Star size={18} fill={item <= rating ? "currentColor" : "none"} />
                </button>
              ))}
            </div>
            <textarea
              required
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="写下你的阅读感受…"
              rows="3"
            />
            <button className="button button-dark" type="submit">
              发布评论 <Send size={15} />
            </button>
          </form>
        )}
        <div className="review-list">
          {reviews.map((review) => (
            <article className="review-item" key={review.id}>
              <div className="review-avatar">{review.user.slice(0, 1)}</div>
              <div>
                <div className="review-meta">
                  <strong>{review.user}</strong>
                  <span>
                    {[1, 2, 3, 4, 5].map((item) => (
                      <Star
                        key={item}
                        size={12}
                        fill={item <= review.rating ? "currentColor" : "none"}
                      />
                    ))}
                  </span>
                  <time>{dateText(review.createdAt)}</time>
                </div>
                <p>{review.content}</p>
                <div className="review-actions"><button onClick={() => onLikeReview(review.id)} className={review.liked ? "is-active" : ""}><ThumbsUp size={12} /> {review.helpful} 有帮助</button><button onClick={() => { const next = replyOpen === review.id ? null : review.id; setReplyOpen(next); if (next) onLoadReviewComments(review.id); }}><MessageCircle size={12} /> 回复</button></div>
                {replyOpen === review.id && <div className="review-replies">{(reviewComments[review.id] || []).map((comment) => <p key={comment.id}><strong>{comment.user}</strong>：{comment.content}</p>)}<form onSubmit={(e) => { e.preventDefault(); if (replyText.trim()) { onSubmitReviewComment(review.id, replyText); setReplyText(""); } }}><input value={replyText} onChange={(e) => setReplyText(e.target.value)} placeholder="回复这条评论" /><button>发送</button></form></div>}
              </div>
            </article>
          ))}
          {!reviews.length && <p className="muted-label">还没有评论，来写第一条吧。</p>}
        </div>
      </div>
    </div>
  );
}
function CartDrawer({ cart, count, total, onClose, onUpdate, onRemove, onCheckout }) {
  return (
    <div className="overlay" onClick={onClose}>
      <aside className="cart-drawer" onClick={(e) => e.stopPropagation()}>
        <div className="drawer-head">
          <div>
            <span className="section-kicker">你的阅读计划</span>
            <h2>
              书袋 <small>{count} 本</small>
            </h2>
          </div>
          <button className="icon-button" aria-label="关闭书袋" onClick={onClose}>
            <X size={20} />
          </button>
        </div>
        {cart.length === 0 ? (
          <div className="cart-empty">
            <ShoppingBag size={32} />
            <h3>书袋还是空的</h3>
            <p>把想读的书放进来，慢慢决定。</p>
            <button className="button button-dark" onClick={onClose}>
              回到书架
            </button>
          </div>
        ) : (
          <>
            <div className="cart-items">
              {cart.map((item) => (
                <div className="cart-item" key={item.id}>
                  <img src={item.cover} alt="" />
                  <div className="cart-item-info">
                    <h3>{item.title}</h3>
                    <p>{item.author}</p>
                    <div className="quantity">
                      <button onClick={() => onUpdate(item.id, -1)} aria-label="减少数量">
                        <Minus size={13} />
                      </button>
                      <span>{item.quantity}</span>
                      <button onClick={() => onUpdate(item.id, 1)} aria-label="增加数量">
                        <Plus size={13} />
                      </button>
                    </div>
                  </div>
                  <div className="cart-item-price">
                    <strong>¥{item.price * item.quantity}</strong>
                    <button onClick={() => onRemove(item.id)}>移除</button>
                  </div>
                </div>
              ))}
            </div>
            <div className="cart-summary">
              <div>
                <span>商品小计</span>
                <strong>¥{total}</strong>
              </div>
              <small>运费将在结算时根据收货地址计算</small>
              <button className="button button-dark button-wide" onClick={onCheckout}>
                去结算 <ArrowRight size={17} />
              </button>
            </div>
          </>
        )}
      </aside>
    </div>
  );
}
function NewPostModal({ onClose, onSubmit }) {
  const [data, setData] = useState({ title: "", excerpt: "", bodyMarkdown: "", imageUrl: "", imageData: "", tags: [], category: "阅读分享" });
  const [tagText, setTagText] = useState("");
  return (
    <div className="overlay" onClick={onClose}>
      <div className="form-modal" onClick={(e) => e.stopPropagation()}>
        <div className="drawer-head">
          <div>
            <span className="section-kicker">书友会</span>
            <h2>发起一场讨论</h2>
          </div>
          <button className="icon-button" onClick={onClose} aria-label="关闭">
            <X size={20} />
          </button>
        </div>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            const content = data.excerpt.trim() || data.bodyMarkdown.trim();
            if (!content) return;
            onSubmit({ ...data, excerpt: content, bodyMarkdown: data.bodyMarkdown.trim() || content, tags: tagText.split(/[，,\s]+/).filter(Boolean) });
          }}
          className="modal-form"
        >
          <label>
            标题
            <input
              required
              value={data.title}
              onChange={(e) => setData({ ...data, title: e.target.value })}
              placeholder="比如：最近有什么让你停不下来的书？"
            />
          </label>
          <label>
            Markdown 正文
            <textarea
              value={data.bodyMarkdown}
              onChange={(e) => setData({ ...data, bodyMarkdown: e.target.value })}
              rows="4"
              placeholder="支持 **加粗**、`代码` 与换行，给书友更清晰的阅读体验"
            />
          </label>
          <label>
            图片地址（可选）
            <input
              value={data.imageUrl}
              onChange={(e) => setData({ ...data, imageUrl: e.target.value })}
              placeholder="粘贴图片地址，发布后展示在帖子中"
            />
          </label>
          <label>
            或上传图片
            <input type="file" accept="image/*" onChange={(e) => { const file = e.target.files?.[0]; if (!file) return; if (file.size > 3 * 1024 * 1024) { window.alert("图片不能超过 3MB"); e.target.value = ""; return; } const reader = new FileReader(); reader.onload = () => setData((current) => ({ ...current, imageData: String(reader.result || "") })); reader.readAsDataURL(file); }} />
            <small className="field-hint">图片会随帖子保存到数据库，支持 JPG、PNG，最大 3MB</small>
          </label>
          <label>
            帖子标签
            <input value={tagText} onChange={(e) => setTagText(e.target.value)} placeholder="例如：毛姆,年度书单" />
          </label>
          <label>
            正文
            <textarea
              value={data.excerpt}
              onChange={(e) => setData({ ...data, excerpt: e.target.value })}
              rows="5"
              placeholder="说说你的想法，给书友一些具体的线索…"
            />
          </label>
          <label>
            话题分类
            <select
              value={data.category}
              onChange={(e) => setData({ ...data, category: e.target.value })}
            >
              {forumCategories.slice(1).map((item) => (
                <option key={item}>{item}</option>
              ))}
            </select>
          </label>
          <button className="button button-dark button-wide" type="submit">
            发布讨论 <Send size={15} />
          </button>
        </form>
      </div>
    </div>
  );
}
function ProfileEditModal({ profile, onClose, onSave }) {
  const [data, setData] = useState({ name: profile.name, bio: profile.bio });
  return (
    <div className="overlay" onClick={onClose}>
      <div className="form-modal" onClick={(e) => e.stopPropagation()}>
        <div className="drawer-head">
          <div>
            <span className="section-kicker">我的纸灯</span>
            <h2>编辑个人资料</h2>
          </div>
          <button className="icon-button" onClick={onClose} aria-label="关闭">
            <X size={20} />
          </button>
        </div>
        <form
          className="modal-form"
          onSubmit={(e) => {
            e.preventDefault();
            onSave(data);
          }}
        >
          <label>
            昵称
            <input
              required
              value={data.name}
              onChange={(e) => setData({ ...data, name: e.target.value })}
            />
          </label>
          <label>
            个人简介
            <textarea
              value={data.bio}
              onChange={(e) => setData({ ...data, bio: e.target.value })}
              rows="4"
            />
          </label>
          <button className="button button-dark button-wide">
            保存资料 <Check size={15} />
          </button>
        </form>
      </div>
    </div>
  );
}
function AddressModal({ onClose, onSave }) {
  const [data, setData] = useState({ label: "家", receiver: "", phone: "", detail: "" });
  return (
    <div className="overlay" onClick={onClose}>
      <div className="form-modal" onClick={(e) => e.stopPropagation()}>
        <div className="drawer-head">
          <div>
            <span className="section-kicker">收货地址</span>
            <h2>新增收货地址</h2>
          </div>
          <button className="icon-button" onClick={onClose} aria-label="关闭">
            <X size={20} />
          </button>
        </div>
        <form
          className="modal-form"
          onSubmit={(e) => {
            e.preventDefault();
            onSave(data);
          }}
        >
          <label>
            地址标签
            <input
              value={data.label}
              onChange={(e) => setData({ ...data, label: e.target.value })}
              placeholder="家 / 公司"
            />
          </label>
          <label>
            收货人
            <input
              required
              value={data.receiver}
              onChange={(e) => setData({ ...data, receiver: e.target.value })}
            />
          </label>
          <label>
            联系电话
            <input
              required
              value={data.phone}
              onChange={(e) => setData({ ...data, phone: e.target.value })}
            />
          </label>
          <label>
            详细地址
            <textarea
              required
              value={data.detail}
              onChange={(e) => setData({ ...data, detail: e.target.value })}
              rows="3"
            />
          </label>
          <button className="button button-dark button-wide">
            保存地址 <MapPin size={15} />
          </button>
        </form>
      </div>
    </div>
  );
}
function ForumPostModal({ post, comments, onClose, onSubmit, onLikeComment }) {
  const [content, setContent] = useState("");
  return (
    <div className="overlay" onClick={onClose}>
      <div className="post-detail-modal" onClick={(e) => e.stopPropagation()}>
        <div className="drawer-head">
          <div>
            <span className="section-kicker">
              {post.category} · {dateText(post.createdAt)}
            </span>
            <h2>{post.title}</h2>
          </div>
          <button className="icon-button" onClick={onClose} aria-label="关闭讨论">
            <X size={20} />
          </button>
        </div>
        <div className="post-detail-body">
          <div className="post-avatar">{post.avatar}</div>
          <div>
            <strong>{post.author}</strong>
            <p>{renderMarkdown(post.bodyMarkdown || post.excerpt)}</p>
            {post.imageUrl && <img className="post-detail-image" src={post.imageUrl} alt="帖子配图" />}
            <div className="post-detail-tags">{post.tags?.map((tag) => <span key={tag}>#{tag}</span>)}</div>
          </div>
        </div>
        <div className="comment-heading">
          <span>全部回复 · {comments.length}</span>
        </div>
        <div className="comment-list">
          {comments.map((comment) => (
            <article key={comment.id}>
              <div className="review-avatar">{comment.user.slice(0, 1)}</div>
              <div>
                <strong>{comment.user}</strong>
                <time>{dateText(comment.createdAt)}</time>
                <p>{comment.content}</p>
                <button className={comment.liked ? "comment-like is-active" : "comment-like"} onClick={() => onLikeComment(comment.id)}><ThumbsUp size={12} /> {comment.likes || 0}</button>
              </div>
            </article>
          ))}
          {!comments.length && <p className="muted-label">还没有回复，来留下第一句话吧。</p>}
        </div>
        <form
          className="comment-form"
          onSubmit={(e) => {
            e.preventDefault();
            if (content.trim()) {
              onSubmit(content);
              setContent("");
            }
          }}
        >
          <textarea
            required
            value={content}
            onChange={(e) => setContent(e.target.value)}
            rows="3"
            placeholder="写下你的回应…"
          />
          <button className="button button-dark" type="submit">
            回复 <Send size={15} />
          </button>
        </form>
      </div>
    </div>
  );
}
function renderMarkdown(text) {
  const parts = String(text || "").split(/(\*\*[^*]+\*\*|`[^`]+`)/g);
  return parts.map((part, index) => {
    if (part.startsWith("**") && part.endsWith("**")) return <strong key={index}>{part.slice(2, -2)}</strong>;
    if (part.startsWith("`") && part.endsWith("`")) return <code key={index}>{part.slice(1, -1)}</code>;
    return <span key={index}>{part}</span>;
  });
}
function UserProfileModal({ profile, onClose, onFollow, onOpenPost }) {
  return (
    <div className="overlay" onClick={onClose}>
      <div className="form-modal user-profile-modal" onClick={(e) => e.stopPropagation()}>
        <div className="drawer-head">
          <div><span className="section-kicker">书友主页</span><h2>{profile.name}</h2></div>
          <button className="icon-button" onClick={onClose} aria-label="关闭"><X size={20} /></button>
        </div>
        <p className="user-profile-bio">{profile.bio || "这位书友还没有写下简介。"}</p>
        <div className="user-profile-stats"><span>{profile.posts.length} 篇帖子</span><span>{profile.points} 纸灯积分</span></div>
        <button className="button button-outline" onClick={onFollow}><UserPlus size={15} /> 关注 / 取消关注</button>
        <div className="user-profile-posts">
          {profile.posts.map((post) => <button key={post.id} onClick={() => { onClose(); onOpenPost(post); }}><strong>{post.title}</strong><small>{post.likes} 赞 · {post.comments} 回复</small></button>)}
        </div>
      </div>
    </div>
  );
}
function CheckoutModal({ total, cart, addresses = [], onClose, onSuccess }) {
  const [customer, setCustomer] = useState({ name: "", phone: "", address: "" });
  const [selectedAddressId, setSelectedAddressId] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  useEffect(() => {
    const preferred = addresses.find((address) => address.isDefault) || addresses[0];
    if (!preferred) {
      setSelectedAddressId("");
      return;
    }
    setSelectedAddressId(String(preferred.id));
    setCustomer({ name: preferred.receiver, phone: preferred.phone, address: preferred.detail });
  }, [addresses]);
  function selectAddress(value) {
    setSelectedAddressId(value);
    const address = addresses.find((item) => String(item.id) === value);
    if (address) setCustomer({ name: address.receiver, phone: address.phone, address: address.detail });
    else setCustomer({ name: "", phone: "", address: "" });
  }
  async function submit(e) {
    e.preventDefault();
    setLoading(true);
    setError("");
    try {
      const res = await fetch("/api/orders", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${getSessionToken()}`,
        },
        body: JSON.stringify({ customer, items: cart, total }),
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.message);
      onSuccess(data);
    } catch (err) {
      setError(err.message || "订单创建失败，请稍后重试");
    } finally {
      setLoading(false);
    }
  }
  return (
    <div className="overlay" onClick={onClose}>
      <div className="checkout-modal" onClick={(e) => e.stopPropagation()}>
        <div className="drawer-head">
          <div>
            <span className="section-kicker">最后一步</span>
            <h2>确认收货信息</h2>
          </div>
          <button className="icon-button" onClick={onClose} aria-label="关闭结算">
            <X size={20} />
          </button>
        </div>
        <form onSubmit={submit}>
          <label>
            使用保存的地址
            <select value={selectedAddressId} onChange={(e) => selectAddress(e.target.value)}>
              <option value="">手动填写新地址</option>
              {addresses.map((address) => (
                <option key={address.id} value={address.id}>
                  {address.label} · {address.receiver} · {address.detail}
                </option>
              ))}
            </select>
            {addresses.length > 0 && <small className="field-hint">已自动填入个人中心的默认地址，也可以在这里切换。</small>}
          </label>
          <label>
            收货人
            <input
              required
              value={customer.name}
              onChange={(e) => setCustomer({ ...customer, name: e.target.value })}
              placeholder="怎么称呼你"
            />
          </label>
          <label>
            联系电话
            <input
              required
              pattern="[0-9*+ -]{6,}"
              value={customer.phone}
              onChange={(e) => setCustomer({ ...customer, phone: e.target.value })}
              placeholder="用于物流联系"
            />
          </label>
          <label>
            收货地址
            <textarea
              required
              value={customer.address}
              onChange={(e) => setCustomer({ ...customer, address: e.target.value })}
              placeholder="省 / 市 / 区 · 街道 · 门牌号"
              rows="3"
            />
          </label>
          {error && <p className="form-error">{error}</p>}
          <div className="checkout-total">
            <span>应付金额</span>
            <strong>¥{total}</strong>
          </div>
          <button className="button button-dark button-wide" disabled={loading}>
            {loading ? "正在确认…" : "确认下单"} <ArrowRight size={17} />
          </button>
        </form>
      </div>
    </div>
  );
}
export default App;
