import {
  ArrowRight,
  ArrowLeftRight,
  Bell,
  BadgePercent,
  BookOpen,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  CalendarDays,
  Flame,
  Heart,
  LayoutDashboard,
  Library,
  LogOut,
  MessageCircle,
  Package,
  Plus,
  Search,
  ShoppingBag,
  SlidersHorizontal,
  Sparkles,
  Star,
  ShieldCheck,
  Target,
  Trophy,
  BarChart3,
  UserCircle,
  X,
} from "lucide-react";
import { useEffect, useState } from "react";

export function StoreRail({
  activeView = "home",
  section = "overview",
  setSection,
  category = "all",
  setCategory,
  categoryList,
  onView,
  authUser,
  onLogout,
  cartCount,
  onOpenBag,
  onSearch,
  adminSection = "overview",
  onAdminSection,
}) {
  const [categoryOpen, setCategoryOpen] = useState(activeView === "home" && section === "shelf");
  if (authUser.role === "admin") {
    return (
      <AdminRail
        activeSection={adminSection}
        onSectionChange={onAdminSection}
        authUser={authUser}
        onLogout={onLogout}
      />
    );
  }
  const openHome = () => onView("home");
  const chooseCategory = (id) => {
    setCategory?.(id);
    setSection?.("shelf");
    setCategoryOpen(true);
    openHome();
  };
  const toggleAllShelf = () => {
    if (activeView === "home" && section === "shelf" && category === "all")
      setCategoryOpen((open) => !open);
    else {
      setCategory?.("all");
      setSection?.("shelf");
      setCategoryOpen(true);
      openHome();
    }
  };
  return (
    <aside className="store-rail">
      <button
        className="rail-brand brand-button"
        onClick={() => {
          setSection?.("overview");
          openHome();
        }}
        aria-label="纸灯书店首页"
      >
        <span className="brand-mark">
          <BookOpen size={18} />
        </span>
        <span>
          纸灯<small>BOOKS</small>
        </span>
      </button>
      <div className="rail-banner">
        <strong>纸灯书店</strong>
        <span>满 99 元包邮</span>
        <small>48 小时内发货</small>
      </div>
      <div className="rail-main-nav">
        <button
          className={activeView === "home" ? "rail-button is-active" : "rail-button"}
          onClick={openHome}
        >
          <BookOpen size={16} />
          <span>逛书架</span>
        </button>
        <button
          className={activeView === "forum" ? "rail-button is-active" : "rail-button"}
          onClick={() => onView("forum")}
        >
          <MessageCircle size={16} />
          <span>书友论坛</span>
        </button>
        <button
          className={activeView === "profile" ? "rail-button is-active" : "rail-button"}
          onClick={() => onView("profile")}
        >
          <UserCircle size={16} />
          <span>我的纸灯</span>
        </button>
        <button className={activeView === "secondhand" ? "rail-button is-active" : "rail-button"} onClick={() => onView("secondhand")}>
          <ArrowLeftRight size={16} />
          <span>二手书市</span>
        </button>
        <button className={activeView === "clubs" ? "rail-button is-active" : "rail-button"} onClick={() => onView("clubs")}>
          <CalendarDays size={16} />
          <span>读书会</span>
        </button>
        <button className={activeView === "challenges" ? "rail-button is-active" : "rail-button"} onClick={() => onView("challenges")}>
          <Trophy size={16} />
          <span>阅读挑战</span>
        </button>
        <button className={activeView === "analytics" ? "rail-button is-active" : "rail-button"} onClick={() => onView("analytics")}>
          <BarChart3 size={16} />
          <span>我的数据</span>
        </button>
        <button className={activeView === "messages" ? "rail-button is-active" : "rail-button"} onClick={() => onView("messages")}>
          <Bell size={16} />
          <span>消息中心</span>
        </button>
        <button className={activeView === "leaderboard" ? "rail-button is-active" : "rail-button"} onClick={() => onView("leaderboard")}>
          <Trophy size={16} />
          <span>阅读榜单</span>
        </button>
        {authUser.role === "admin" && (
          <button
            className={activeView === "admin" ? "rail-button is-active" : "rail-button"}
            onClick={() => onView("admin")}
          >
            <ShieldCheck size={16} />
            <span>后台管理</span>
          </button>
        )}
      </div>
      <div className="rail-tools">
        <button onClick={onSearch} aria-label="搜索书籍">
          <Search size={15} />
          <span>搜索</span>
        </button>
        <button onClick={onOpenBag} aria-label={`打开书袋，${cartCount}件商品`}>
          <ShoppingBag size={15} />
          <span>书袋 {cartCount ? `(${cartCount})` : ""}</span>
        </button>
      </div>
      <div className="rail-heading">
        <span>书架</span>
        <small>CATALOGUE</small>
      </div>
      <div className="rail-group">
        <button
          className={
            activeView === "home" && section === "overview"
              ? "rail-button is-active"
              : "rail-button"
          }
          onClick={() => {
            setSection?.("overview");
            openHome();
          }}
        >
          <Library size={17} />
          <span>首页概览</span>
        </button>
        <button
          className={
            activeView === "home" && section === "editor" ? "rail-button is-active" : "rail-button"
          }
          onClick={() => {
            setSection?.("editor");
            openHome();
          }}
        >
          <Sparkles size={17} />
          <span>编辑选书</span>
          <b>本周</b>
        </button>
        <button
          className={
            activeView === "home" && section === "shelf" && category === "all"
              ? "rail-button is-active"
              : "rail-button"
          }
          onClick={toggleAllShelf}
          aria-expanded={categoryOpen}
        >
          <BookOpen size={17} />
          <span>全部书架</span>
          <b>{categoryList[0].count}</b>
          <ChevronDown className={`rail-caret ${categoryOpen ? "is-open" : ""}`} size={14} />
        </button>
        {categoryOpen && (
          <div className="rail-subgroup" aria-label="书架分类">
            {categoryList.slice(1).map((item) => (
              <button
                key={item.id}
                className={
                  activeView === "home" && section === "shelf" && category === item.id
                    ? "rail-button is-active"
                    : "rail-button"
                }
                onClick={() => chooseCategory(item.id)}
              >
                <span className="rail-dot"></span>
                <span>{item.label}</span>
                <b>{item.count}</b>
              </button>
            ))}
          </div>
        )}
      </div>
      <div className="rail-bottom">
        <div className="rail-user">
          <UserCircle size={17} />
          <span>{authUser.name}</span>
          <small>{authUser.roleLabel}</small>
        </div>
        <button className="rail-button rail-logout" onClick={onLogout}>
          <LogOut size={16} />
          <span>退出登录</span>
        </button>
      </div>
    </aside>
  );
}

function AdminRail({ activeSection, onSectionChange, authUser, onLogout }) {
  const items = [
    ["overview", "数据总览", LayoutDashboard],
    ["books", "图书管理", Library],
    ["orders", "订单管理", Package],
    ["users", "用户管理", UserCircle],
    ["community", "社区审核", MessageCircle],
    ["analytics", "经营分析", BarChart3],
    ["inventory", "库存管理", Package],
  ];
  return (
    <aside className="store-rail admin-rail">
      <div className="rail-brand brand-button" aria-label="纸灯书店管理端">
        <span className="brand-mark">
          <BookOpen size={18} />
        </span>
        <span>
          纸灯<small>ADMIN</small>
        </span>
      </div>
      <div className="admin-rail-title">
        <ShieldCheck size={16} />
        <div>
          <strong>管理端</strong>
          <small>ADMIN WORKSPACE</small>
        </div>
      </div>
      <nav className="admin-rail-nav">
        {items.map(([id, label, Icon]) => (
          <button
            key={id}
            className={activeSection === id ? "rail-button is-active" : "rail-button"}
            onClick={() => onSectionChange?.(id)}
          >
            <Icon size={16} />
            <span>{label}</span>
          </button>
        ))}
      </nav>
      <div className="rail-bottom">
        <div className="rail-user">
          <UserCircle size={17} />
          <span>{authUser.name}</span>
          <small>管理员</small>
        </div>
        <button className="rail-button rail-logout" onClick={onLogout}>
          <LogOut size={16} />
          <span>退出登录</span>
        </button>
      </div>
    </aside>
  );
}

export default function StoreHome({
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
  authUser,
  onLogout,
  cartCount,
  onOpenBag,
  section,
  setSection,
}) {
  const [shelfMode, setShelfMode] = useState("all");
  const categoryLabel = categoryList.find((item) => item.id === category)?.label || "全部书目";
  return (
    <main className="store-workspace" id="top">
      <StoreRail
        activeView="home"
        section={section}
        setSection={(next) => {
          setSection(next);
          if (next !== "shelf") setShelfMode("all");
        }}
        category={category}
        setCategory={(id) => {
          setCategory(id);
          setShelfMode("all");
        }}
        categoryList={categoryList}
        onView={goView}
        authUser={authUser}
        onLogout={onLogout}
        cartCount={cartCount}
        onOpenBag={onOpenBag}
        onSearch={() => document.getElementById("book-search")?.focus()}
      />
      <section className="store-content">
        {section === "overview" && (
          <StoreOverview
            books={books}
            featured={featured}
            onOpen={onOpen}
            onAdd={onAdd}
            onShelf={() => setSection("shelf")}
          />
        )}
        {section === "editor" && <StoreEditor books={books} onOpen={onOpen} onAdd={onAdd} />}
        {section === "shelf" && (
          <StoreShelf
            books={books}
            categoryLabel={categoryLabel}
            category={category}
            onCategoryChange={(id) => {
              setCategory(id);
              setCategoryOpen(true);
            }}
            search={search}
            setSearch={setSearch}
            sort={sort}
            setSort={setSort}
            categoryList={categoryList}
            shelfMode={shelfMode}
            setShelfMode={setShelfMode}
            onAdd={onAdd}
            onOpen={onOpen}
            favorites={favorites}
            onFavorite={onFavorite}
          />
        )}
      </section>
    </main>
  );
}

function StoreOverview({ books, featured, onOpen, onAdd, onShelf }) {
  const shortlist = books.filter((book) => book.id !== featured.id).slice(0, 3);
  const bannerBooks = [featured, ...books.filter((book) => book.id !== featured.id)].slice(0, 4);
  const [bannerIndex, setBannerIndex] = useState(0);
  const [bannerPaused, setBannerPaused] = useState(false);
  const bannerBook = bannerBooks[bannerIndex] || featured;
  useEffect(() => {
    if (bannerPaused || bannerBooks.length < 2) return undefined;
    const timer = window.setInterval(
      () => setBannerIndex((index) => (index + 1) % bannerBooks.length),
      4500,
    );
    return () => window.clearInterval(timer);
  }, [bannerPaused, bannerBooks.length]);
  useEffect(() => {
    setBannerIndex(0);
  }, [books.length]);
  const moveBanner = (step) =>
    setBannerIndex((index) => (index + step + bannerBooks.length) % bannerBooks.length);
  return (
    <div className="store-panel overview-panel">
      <div className="panel-topline">
        <span className="panel-label">STORE / OVERVIEW</span>
        <span className="panel-date">本周更新 · 2025.06.18</span>
      </div>
      <div className="overview-grid">
        <div className="overview-copy">
          <h1>
            今天想读
            <br />
            <em>哪一本？</em>
          </h1>
          <p>搜索书名、作者，或直接从编辑选书开始。</p>
          <div className="overview-actions">
            <button className="button button-dark" onClick={onShelf}>
              打开全部书架 <ArrowRight size={16} />
            </button>
            <button className="text-link" onClick={() => onOpen(bannerBook)}>
              查看本周主推 <ArrowRight size={14} />
            </button>
          </div>
        </div>
        <div
          className="overview-feature overview-banner"
          onMouseEnter={() => setBannerPaused(true)}
          onMouseLeave={() => setBannerPaused(false)}
        >
          <div className="overview-banner-top">
            <span className="mini-label">编辑主推 / 0{bannerIndex + 1}</span>
            <div className="overview-banner-nav">
              <button onClick={() => moveBanner(-1)} aria-label="上一本推荐">
                <ChevronLeft size={15} />
              </button>
              <button onClick={() => moveBanner(1)} aria-label="下一本推荐">
                <ChevronRight size={15} />
              </button>
            </div>
          </div>
          <div className="overview-banner-body">
            <img src={bannerBook.cover} alt={bannerBook.title} />
            <div>
              <button className="overview-banner-title" onClick={() => onOpen(bannerBook)}>
                {bannerBook.title}
              </button>
              <p>{bannerBook.author}</p>
              <strong>¥{bannerBook.price}</strong>
              <div className="overview-banner-actions">
                <button
                  className="icon-add"
                  onClick={() => onAdd(bannerBook)}
                  aria-label={`将《${bannerBook.title}》加入书袋`}
                >
                  <Plus size={18} />
                </button>
                <button className="overview-detail-link" onClick={() => onOpen(bannerBook)}>
                  查看详情 <ArrowRight size={13} />
                </button>
              </div>
            </div>
          </div>
          <div className="overview-banner-bottom">
            <div className="overview-banner-dots">
              {bannerBooks.map((book, index) => (
                <button
                  key={book.id}
                  className={index === bannerIndex ? "is-active" : ""}
                  onClick={() => setBannerIndex(index)}
                  aria-label={`查看第 ${index + 1} 本推荐`}
                />
              ))}
            </div>
            <span>{bannerPaused ? "已暂停" : "自动轮播"}</span>
          </div>
        </div>
      </div>
      <div className="overview-metrics">
        <div>
          <span>可选书目</span>
          <strong>08</strong>
        </div>
        <div>
          <span>平均评分</span>
          <strong>4.8</strong>
        </div>
        <div>
          <span>本周新上架</span>
          <strong>03</strong>
        </div>
        <div>
          <span>满额包邮</span>
          <strong>¥99</strong>
        </div>
      </div>
      <div className="quick-row">
        <span className="panel-label">快速入口</span>
        <button onClick={onShelf}>
          <BookOpen size={16} /> 全部书架 <ArrowRight size={14} />
        </button>
        <button onClick={() => onOpen(bannerBook)}>
          <Star size={16} /> 查看主推 <ArrowRight size={14} />
        </button>
        <button onClick={() => onAdd(bannerBook)}>
          <ShoppingBag size={16} /> 加入书袋 <ArrowRight size={14} />
        </button>
      </div>
      <div className="overview-lower">
        <section className="overview-shortlist">
          <div className="overview-lower-head">
            <div>
              <span className="section-kicker">今日书单</span>
              <h2>再挑三本</h2>
            </div>
            <button className="text-link" onClick={onShelf}>
              查看全部 <ArrowRight size={14} />
            </button>
          </div>
          <div className="overview-mini-books">
            {shortlist.map((book) => (
              <div className="overview-mini-book" key={book.id}>
                <button className="overview-mini-main" onClick={() => onOpen(book)}>
                  <img src={book.cover} alt={book.title} />
                  <span>
                    <strong>{book.title}</strong>
                    <small>{book.author}</small>
                    <em>
                      ¥{book.price} · {book.rating} 分
                    </em>
                  </span>
                </button>
                <button
                  className="overview-mini-add"
                  onClick={() => onAdd(book)}
                  aria-label={`将《${book.title}》加入书袋`}
                >
                  <Plus size={14} />
                </button>
              </div>
            ))}
          </div>
        </section>
        <aside className="overview-note">
          <span className="section-kicker">选书提示</span>
          <h3>先按兴趣，再看评分。</h3>
          <p>从分类或快捷筛选开始，找到更适合今天的那一本。</p>
          <button className="text-link" onClick={onShelf}>
            开始筛选 <ArrowRight size={14} />
          </button>
        </aside>
      </div>
    </div>
  );
}

function StoreEditor({ books, onOpen, onAdd }) {
  return (
    <div className="store-panel editor-panel">
      <div className="panel-topline">
        <span className="panel-label">EDITOR / PICKS</span>
        <span className="panel-date">每周三更新一批</span>
      </div>
      <div className="editor-header">
        <div>
          <span className="section-kicker">本周编辑选书</span>
          <h1>
            把时间留给
            <br />
            <em>值得读的书。</em>
          </h1>
        </div>
        <p>从新书、经典和读者反馈中，挑出本周最适合带回家的四本。</p>
      </div>
      <div className="editor-grid">
        {books.slice(0, 4).map((book, index) => (
          <button className="editor-book" key={book.id} onClick={() => onOpen(book)}>
            <span className="editor-number">0{index + 1}</span>
            <img src={book.cover} alt={book.title} />
            <span className="editor-book-meta">
              <b>{book.title}</b>
              <small>{book.author}</small>
              <i>¥{book.price}</i>
            </span>
            <span
              className="editor-book-action"
              onClick={(event) => {
                event.stopPropagation();
                onAdd(book);
              }}
            >
              <Plus size={16} />
            </span>
          </button>
        ))}
      </div>
    </div>
  );
}

function StoreShelf({
  books,
  categoryLabel,
  category,
  onCategoryChange,
  search,
  setSearch,
  sort,
  setSort,
  categoryList,
  shelfMode,
  setShelfMode,
  onAdd,
  onOpen,
  favorites,
  onFavorite,
}) {
  const filters = [
    { id: "all", label: "全部", icon: <Library size={14} /> },
    { id: "rating", label: "高分推荐", icon: <Star size={14} /> },
    { id: "discount", label: "折扣专区", icon: <BadgePercent size={14} /> },
    { id: "stock", label: "库存紧张", icon: <Flame size={14} /> },
    { id: "favorites", label: "我的收藏", icon: <Heart size={14} /> },
  ];
  const visibleBooks = books.filter((book) => {
    if (shelfMode === "rating") return Number(book.rating) >= 4.8;
    if (shelfMode === "discount")
      return book.originalPrice && (book.originalPrice - book.price) / book.originalPrice >= 0.2;
    if (shelfMode === "stock") return Number(book.stock) <= 12;
    if (shelfMode === "favorites") return favorites.includes(book.id);
    return true;
  });
  const hasRefinements = search.trim() || shelfMode !== "all";
  const emptyText =
    shelfMode === "favorites"
      ? "还没有收藏书籍"
      : shelfMode === "stock"
        ? "暂时没有库存紧张的书"
        : "没有匹配结果";
  return (
    <div className="store-panel shelf-panel">
      <div className="panel-topline">
        <span className="panel-label">SHELF / {categoryLabel.toUpperCase()}</span>
        <span className="panel-date">{visibleBooks.length} 本结果</span>
      </div>
      <div className="shelf-toolbar">
        <div>
          <div className="shelf-title-row">
            <h1>{categoryLabel}</h1>
            <span className="shelf-result-count">{visibleBooks.length} 本</span>
          </div>
          <span>搜索、排序和快捷筛选</span>
        </div>
        <div className="shelf-controls">
          <div className="search-wrap">
            <Search size={17} />
            <input
              id="book-search"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="搜索书名或作者"
            />
            {search && (
              <button className="search-clear" onClick={() => setSearch("")} aria-label="清空搜索">
                <X size={13} />
              </button>
            )}
          </div>
          <label>
            <select value={sort} onChange={(e) => setSort(e.target.value)}>
              <option value="featured">推荐排序</option>
              <option value="rating">评分最高</option>
              <option value="price-asc">价格最低</option>
              <option value="price-desc">价格最高</option>
            </select>
            <ChevronDown size={14} />
          </label>
        </div>
      </div>
      <div className="shelf-quick-row">
        <span className="shelf-filter-label">
          <SlidersHorizontal size={14} /> 快捷筛选
        </span>
        {filters.map((filter) => (
          <button
            key={filter.id}
            className={shelfMode === filter.id ? "shelf-filter is-active" : "shelf-filter"}
            onClick={() => setShelfMode(filter.id)}
          >
            {filter.icon}
            <span>{filter.label}</span>
          </button>
        ))}
        {hasRefinements && (
          <button
            className="shelf-reset"
            onClick={() => {
              setSearch("");
              setShelfMode("all");
            }}
          >
            重置
          </button>
        )}
      </div>
      <div className="shelf-tabs">
        {categoryList.map((item) => (
          <button
            key={item.id}
            className={category === item.id ? "is-active" : ""}
            onClick={() => onCategoryChange(item.id)}
          >
            {item.label}
            <small>{item.count}</small>
          </button>
        ))}
      </div>
      <div className="shelf-books">
        {visibleBooks.map((book) => (
          <BookCard
            key={book.id}
            book={book}
            onAdd={onAdd}
            onOpen={onOpen}
            isFavorite={favorites.includes(book.id)}
            onFavorite={() => onFavorite(book)}
          />
        ))}
        {!visibleBooks.length && (
          <div className="empty-state">
            <Library size={26} />
            <h3>{emptyText}</h3>
            <p>换个关键词、分类或筛选条件。</p>
            <button
              className="button button-outline"
              onClick={() => {
                setSearch("");
                setShelfMode("all");
              }}
            >
              清除筛选
            </button>
          </div>
        )}
      </div>
    </div>
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
