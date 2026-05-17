//应用外壳，登录、布局与路由
import { api, store, esc, toast, ICONS, h } from './core.js';

//路由表

const ROUTES = [
  { path: 'workbench', title: '工作台', icon: 'dashboard', group: '概览', load: () => import('./views/workbench.js') },
  { path: 'orders', title: '订单管理', icon: 'orders', group: '经营', badge: true, load: () => import('./views/orders.js') },
  { path: 'products', title: '商品管理', icon: 'products', group: '经营', load: () => import('./views/products.js') },
  { path: 'bundles', title: '组合商品', icon: 'bundles', group: '经营', load: () => import('./views/bundles.js') },
  { path: 'categories', title: '分类管理', icon: 'categories', group: '经营', load: () => import('./views/categories.js') },
  { path: 'employees', title: '员工管理', icon: 'employees', group: '门店', load: () => import('./views/employees.js') },
  { path: 'stats', title: '数据统计', icon: 'stats', group: '门店', load: () => import('./views/stats.js') },
  { path: 'settings', title: '设置', icon: 'settings', group: '门店', load: () => import('./views/settings.js') },
];

const app = document.getElementById('app');
let unreadTimer = null;
let routing = false;

function currentPath() {
  return (location.hash || '').replace(/^#\/?/, '') || 'workbench';
}

function boot() {
  if (!store.token) return renderLogin();
  renderShell();
  window.addEventListener('hashchange', onRoute);
  onRoute();
}

//登录

function renderLogin() {
  clearInterval(unreadTimer);
  app.innerHTML = `
    <div class="login-page">
      <div class="login-brand">
        <div class="brand-top">鲜达超市 · FRESHMART</div>
        <div class="brand-main">
          <h1>把每天的鲜，<br>准时送到家门口</h1>
          <p>线上生鲜超市的门店运营端：商品与组合装维护、订单接单派送、库存与并发控制、销售数据统计，一个后台跑通。</p>
        </div>
        <div class="brand-foot">
          <span>7 大品类 · 21 款商品</span>
          <span>平均 30 分钟送达</span>
          <span>库存防超卖</span>
        </div>
      </div>

      <div class="login-form-wrap">
        <div class="login-card">
          <img class="login-logo" src="/img/logo-lockup.png" alt="鲜达超市" onerror="this.style.display='none'">
          <h2>门店运营端</h2>
          <p class="sub">登录后管理商品、订单与门店数据</p>
          <form id="login-form">
            <label class="field">
              <span>账号</span>
              <input class="input" id="login-user" autocomplete="username" placeholder="登录账号" value="admin">
            </label>
            <label class="field">
              <span>密码</span>
              <input class="input" id="login-pass" type="password" autocomplete="current-password" placeholder="登录密码" value="123456">
            </label>
            <button class="btn btn-primary btn-block login-btn" type="submit" id="login-btn">登 录</button>
            <p class="login-error" id="login-error"></p>
          </form>
          <p class="login-foot">演示账号 <code>admin</code> / <code>123456</code></p>
        </div>
      </div>
    </div>`;

  const form = app.querySelector('#login-form');
  const errBox = app.querySelector('#login-error');
  const btn = app.querySelector('#login-btn');

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = form.querySelector('#login-user').value.trim();
    const password = form.querySelector('#login-pass').value;
    errBox.textContent = '';
    if (!username || !password) { errBox.textContent = '请输入账号和密码'; return; }
    btn.disabled = true;
    btn.textContent = '登录中…';
    try {
      const data = await api.login(username, password);
      store.token = data.token;
      store.user = { id: data.id, name: data.name, userName: data.userName };
      location.hash = '#/workbench';
      boot();
    } catch (err) {
      errBox.textContent = err.message;
      btn.disabled = false;
      btn.textContent = '登 录';
    }
  });
  setTimeout(() => form.querySelector('#login-pass').focus(), 60);
}

//布局

function renderShell() {
  const user = store.user || {};
  const groups = [];
  for (const r of ROUTES) {
    const g = groups.find((x) => x.name === r.group);
    if (g) g.items.push(r);
    else groups.push({ name: r.group, items: [r] });
  }

  app.innerHTML = `
    <div class="shell">
      <aside class="sidebar" id="sidebar">
        <div class="brand">
          <img src="/img/logo-on-dark.png" alt="鲜达超市" onerror="this.style.display='none'">
        </div>
        <nav class="nav" id="nav">
          ${groups.map((g) => `
            <div class="group-label">${g.name}</div>
            ${g.items.map((r) => `
              <a href="#/${r.path}" data-path="${r.path}">
                ${ICONS[r.icon] || ''}
                <span class="txt">${r.title}</span>
                ${r.badge ? '<span class="badge" id="nav-badge" hidden></span>' : ''}
              </a>`).join('')}`).join('')}
        </nav>
        <div class="sidebar-foot">
          <span class="avatar">${esc((user.name || 'A').slice(0, 1))}</span>
          <span class="who">${esc(user.name || '管理员')}</span>
        </div>
      </aside>

      <div class="main">
        <header class="topbar">
          <button class="icon-btn only-mobile" id="btn-menu">${ICONS.menu}</button>
          <button class="store-pill" id="store-toggle" title="点击切换营业状态">
            <i></i><span id="store-badge">营业中</span>
          </button>
          <div class="spacer"></div>
          <button class="icon-btn bell" id="btn-bell" title="待接单提醒">
            ${ICONS.bell}
            <span class="dot" id="bell-dot" hidden></span>
          </button>
          <div class="who-box" id="user-menu">
            <span class="avatar">${esc((user.name || 'A').slice(0, 1))}</span>
            <span class="name">${esc(user.name || '管理员')}</span>
            <span class="caret" style="color:var(--muted-2);font-size:11px">▾</span>
            <div class="menu-pop" id="user-dropdown">
              <button data-nav="#/settings">修改密码</button>
              <button id="btn-logout">退出登录</button>
            </div>
          </div>
        </header>
        <main class="content" id="view-root"></main>
      </div>
    </div>`;

  app.querySelector('#btn-logout').onclick = async () => {
    try { await api.logout(); } catch { /* 忽略 */ }
    store.clear();
    renderLogin();
    location.hash = '#/login';
  };
  app.querySelector('#user-menu').addEventListener('click', (e) => {
    if (e.target.closest('.menu-pop')) {
      const nav = e.target.closest('[data-nav]');
      if (nav) { location.hash = nav.dataset.nav; app.querySelector('#user-dropdown').classList.remove('open'); }
      return;
    }
    app.querySelector('#user-dropdown').classList.toggle('open');
  });
  document.addEventListener('click', (e) => {
    const dd = document.getElementById('user-dropdown');
    if (dd && !e.target.closest('#user-menu')) dd.classList.remove('open');
  });
  app.querySelector('#btn-bell').onclick = () => { location.hash = '#/orders'; };
  app.querySelector('#btn-menu').onclick = () => app.querySelector('#sidebar').classList.toggle('open');
  app.querySelector('#store-toggle').onclick = async () => {
    const pill = app.querySelector('#store-toggle');
    const badge = app.querySelector('#store-badge');
    const next = pill.classList.contains('closed') ? 1 : 0;
    try {
      await api.setStoreStatus(next);
      badge.textContent = next === 1 ? '营业中' : '已打烊';
      pill.classList.toggle('closed', next !== 1);
      toast(next === 1 ? '门店已开始营业' : '门店已打烊');
    } catch (e) { toast(e.message, 'err'); }
  };

  refreshStoreState();
  refreshUnread();
  clearInterval(unreadTimer);
  unreadTimer = setInterval(refreshUnread, 30000);
}

async function refreshStoreState() {
  try {
    const status = await api.storeStatus();
    const pill = document.getElementById('store-toggle');
    const badge = document.getElementById('store-badge');
    if (!badge) return;
    badge.textContent = status === 1 ? '营业中' : '已打烊';
    if (pill) pill.classList.toggle('closed', status !== 1);
  } catch { /* 忽略 */ }
}

async function refreshUnread() {
  try {
    const n = await api.unread();
    const dot = document.getElementById('bell-dot');
    const navBadge = document.getElementById('nav-badge');
    const has = Number(n) > 0;
    if (dot) dot.hidden = !has;
    if (navBadge) {
      navBadge.hidden = !has;
      navBadge.textContent = has ? n : '';
      navBadge.style.display = has ? 'inline-block' : 'none';
    }
  } catch { /* 忽略 */ }
}

//路由

async function onRoute() {
  if (!store.token) return renderLogin();
  const path = currentPath();
  if (path === 'login') { store.clear(); return renderLogin(); }

  const route = ROUTES.find((r) => r.path === path) || ROUTES[0];
  document.title = `${route.title} · 鲜达超市`;
  [...document.querySelectorAll('.nav a')].forEach((a) => {
    a.classList.toggle('active', a.dataset.path === route.path);
  });
  document.getElementById('sidebar')?.classList.remove('open');

  const root = document.getElementById('view-root');
  if (!root || routing) return;
  routing = true;
  try {
    root.innerHTML = `<div class="page-skeleton">
      <div class="skeleton" style="height:26px;width:180px"></div>
      <div class="skeleton" style="height:14px;width:280px;margin-top:12px"></div>
      <div class="skeleton" style="height:88px;margin-top:24px"></div>
      <div class="skeleton" style="height:320px;margin-top:18px"></div>
    </div>`;
    const mod = await route.load();
    await mod.render(root);
  } catch (e) {
    root.innerHTML = `<div class="card"><div class="card-body">
      <h3 style="margin:0 0 8px">页面加载失败</h3>
      <p class="hint">${esc(e.message)}</p>
      <button class="btn" style="margin-top:14px" onclick="location.reload()">重新加载</button>
    </div></div>`;
  } finally {
    routing = false;
  }
  if (location.hash !== `#/${route.path}`) location.hash = `#/${route.path}`;
}

boot();
