//请求、格式化、DOM 工具与通用组件
const TOKEN_KEY = 'fm.token';
const USER_KEY = 'fm.user';

export const store = {
  get token() { return localStorage.getItem(TOKEN_KEY) || ''; },
  set token(v) { v ? localStorage.setItem(TOKEN_KEY, v) : localStorage.removeItem(TOKEN_KEY); },
  get user() {
    try { return JSON.parse(localStorage.getItem(USER_KEY) || 'null'); } catch { return null; }
  },
  set user(v) { v ? localStorage.setItem(USER_KEY, JSON.stringify(v)) : localStorage.removeItem(USER_KEY); },
  clear() { localStorage.removeItem(TOKEN_KEY); localStorage.removeItem(USER_KEY); },
};

//HTTP

export class ApiError extends Error {
  constructor(message, code, status) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.status = status;
  }
}

function qs(params) {
  if (!params) return '';
  const usable = Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== '');
  if (!usable.length) return '';
  return '?' + usable.map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`).join('&');
}

export async function request(path, { method = 'GET', body, params, form, blob = false } = {}) {
  const headers = {};
  if (store.token) headers.token = store.token;
  let payload;
  if (form) {
    payload = form;
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    payload = JSON.stringify(body);
  }

  let res;
  try {
    res = await fetch('/api' + path + qs(params), { method, headers, body: payload });
  } catch (e) {
    throw new ApiError('网络异常，请检查后台服务是否已启动', -1, 0);
  }

  if (res.status === 401) {
    store.clear();
    if (!location.hash.startsWith('#/login')) location.hash = '#/login';
    throw new ApiError('登录已过期，请重新登录', 401, 401);
  }
  if (blob) {
    if (!res.ok) throw new ApiError('导出失败（' + res.status + '）', -1, res.status);
    return res.blob();
  }
  let json;
  try {
    json = await res.json();
  } catch {
    throw new ApiError('服务返回了无法解析的内容（' + res.status + '）', -1, res.status);
  }
  if (json.code !== 1) throw new ApiError(json.msg || '操作失败', json.code, res.status);
  return json.data;
}

export const api = {
  login: (username, password) => request('/employee/login', { method: 'POST', body: { username, password } }),
  logout: () => request('/employee/logout', { method: 'POST' }),
  unread: () => request('/messages/countUnread'),

  storeStatus: () => request('/store/status'),
  setStoreStatus: (status) => request(`/store/${status}`, { method: 'PUT' }),

  businessData: () => request('/workspace/businessData'),
  orderOverview: () => request('/workspace/overviewTradeOrder'),
  productOverview: () => request('/workspace/overviewProductes'),
  bundleOverview: () => request('/workspace/overviewBundles'),

  orderPage: (params) => request('/order/conditionSearch', { params }),
  orderStatistics: () => request('/order/statistics'),
  orderDetail: (id) => request(`/order/details/${id}`),
  orderConfirm: (id) => request('/order/confirm', { method: 'PUT', body: { id, status: 3 } }),
  orderReject: (id, rejectionReason) => request('/order/rejection', { method: 'PUT', body: { id, rejectionReason } }),
  orderDeliver: (id) => request(`/order/delivery/${id}`, { method: 'PUT' }),
  orderComplete: (id) => request(`/order/complete/${id}`, { method: 'PUT' }),
  orderCancel: (id, cancelReason) => request('/order/cancel', { method: 'PUT', body: { id, cancelReason } }),

  productPage: (params) => request('/product/page', { params }),
  productGet: (id) => request(`/product/${id}`),
  productList: (params) => request('/product/list', { params }),
  productSave: (body) => request('/product', { method: 'POST', body }),
  productUpdate: (body) => request('/product', { method: 'PUT', body }),
  productDelete: (ids) => request('/product', { method: 'DELETE', params: { ids: ids.join(',') } }),
  productStatus: (id, status) => request(`/product/status/${status}`, { method: 'POST', params: { id } }),

  bundlePage: (params) => request('/bundle/page', { params }),
  bundleGet: (id) => request(`/bundle/${id}`),
  bundleSave: (body) => request('/bundle', { method: 'POST', body }),
  bundleUpdate: (body) => request('/bundle', { method: 'PUT', body }),
  bundleDelete: (ids) => request('/bundle', { method: 'DELETE', params: { ids: ids.join(',') } }),
  bundleStatus: (id, status) => request(`/bundle/status/${status}`, { method: 'POST', params: { id } }),

  categoryPage: (params) => request('/category/page', { params }),
  categoryList: (type) => request('/category/list', { params: { type } }),
  categorySave: (body) => request('/category', { method: 'POST', body }),
  categoryUpdate: (body) => request('/category', { method: 'PUT', body }),
  categoryDelete: (id) => request('/category', { method: 'DELETE', params: { id } }),
  categoryStatus: (id, status) => request(`/category/status/${status}`, { method: 'POST', params: { id } }),

  employeePage: (params) => request('/employee/page', { params }),
  employeeGet: (id) => request(`/employee/${id}`),
  employeeSave: (body) => request('/employee', { method: 'POST', body }),
  employeeUpdate: (body) => request('/employee', { method: 'PUT', body }),
  employeeStatus: (id, status) => request(`/employee/status/${status}`, { method: 'POST', params: { id } }),
  employeePassword: (body) => request('/employee/editPassword', { method: 'PUT', body }),

  gmvReport: (begin, end) => request('/report/gmvStatistics', { params: { begin, end } }),
  orderReport: (begin, end) => request('/report/tradeOrderStatistics', { params: { begin, end } }),
  userReport: (begin, end) => request('/report/userStatistics', { params: { begin, end } }),
  top10: (begin, end) => request('/report/top10', { params: { begin, end } }),
  exportReport: (begin, end) => request('/report/export', { params: { begin, end }, blob: true }),

  upload: (file) => {
    const form = new FormData();
    form.append('file', file);
    return request('/common/upload', { method: 'POST', form });
  },
};

//格式化

export const fmt = {
  money(v) {
    const n = Number(v || 0);
    return '¥' + n.toFixed(2);
  },
  num(v) {
    return Number(v || 0).toLocaleString('zh-CN');
  },
  pct(v) {
    return (Number(v || 0) * 100).toFixed(1) + '%';
  },
  date(v) {
    if (!v) return '—';
    return String(v).slice(0, 10);
  },
  dt(v) {
    if (!v) return '—';
    return String(v).replace('T', ' ').slice(0, 16);
  },
  time(v) {
    if (!v) return '—';
    return String(v).replace('T', ' ').slice(11, 16);
  },
};

export function esc(s) {
  return String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

export const STATUS = {
  order: {
    1: { text: '待付款', cls: 'amber' },
    2: { text: '待接单', cls: 'amber' },
    3: { text: '已接单', cls: 'blue' },
    4: { text: '派送中', cls: 'blue' },
    5: { text: '已完成', cls: 'green' },
    6: { text: '已取消', cls: 'gray' },
  },
  pay: { 0: { text: '未支付', cls: 'gray' }, 1: { text: '已支付', cls: 'green' } },
  shelf: { 0: { text: '已下架', cls: 'gray' }, 1: { text: '在售', cls: 'green' } },
  employee: { 0: { text: '已禁用', cls: 'gray' }, 1: { text: '正常', cls: 'green' } },
};

export function pill(map, value) {
  const s = map[value] || { text: '未知', cls: 'gray' };
  return `<span class="pill ${s.cls}">${s.text}</span>`;
}

//DOM

export function h(tag, props = {}, ...children) {
  const el = document.createElement(tag);
  for (const [k, v] of Object.entries(props || {})) {
    if (v === null || v === undefined || v === false) continue;
    if (k === 'class') el.className = v;
    else if (k === 'html') el.innerHTML = v;
    else if (k === 'text') el.textContent = v;
    else if (k.startsWith('on') && typeof v === 'function') el.addEventListener(k.slice(2), v);
    else if (k === 'dataset') Object.assign(el.dataset, v);
    else el.setAttribute(k, v === true ? '' : v);
  }
  for (const c of children.flat()) {
    if (c === null || c === undefined || c === false) continue;
    el.append(c instanceof Node ? c : document.createTextNode(String(c)));
  }
  return el;
}

export const $ = (sel, root = document) => root.querySelector(sel);
export const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];

export function delegate(root, selector, type, handler) {
  root.addEventListener(type, (e) => {
    const target = e.target.closest(selector);
    if (target && root.contains(target)) handler(e, target);
  });
}

//提示

export function toast(message, kind = 'ok') {
  const root = document.getElementById('toast-root');
  const el = h('div', { class: `toast ${kind}` }, message);
  root.append(el);
  setTimeout(() => {
    el.style.transition = 'opacity .2s, transform .2s';
    el.style.opacity = '0';
    el.style.transform = 'translateY(-6px)';
    setTimeout(() => el.remove(), 220);
  }, kind === 'err' ? 3200 : 2000);
}

export function confirmDialog({ title = '确认操作', message, okText = '确定', danger = false }) {
  return new Promise((resolve) => {
    const mask = h('div', { class: 'modal-mask' });
    const close = (v) => { mask.remove(); resolve(v); };
    mask.append(
      h('div', { class: 'modal', style: 'width:420px' },
        h('div', { class: 'modal-head' },
          h('h3', { text: title }),
          h('div', { class: 'spacer' }),
          h('button', { class: 'icon-btn', onclick: () => close(false) }, '✕'),
        ),
        h('div', { class: 'modal-body' }, h('p', { class: 'hint', style: 'font-size:13px;color:var(--ink-2);line-height:1.8' }, message)),
        h('div', { class: 'modal-foot' },
          h('button', { class: 'btn', onclick: () => close(false) }, '取消'),
          h('button', {
            class: `btn ${danger ? 'btn-danger' : 'btn-primary'}`,
            style: danger ? 'border-color:#f3c8c6' : '',
            onclick: () => close(true),
          }, okText),
        ),
      ),
    );
    mask.addEventListener('click', (e) => { if (e.target === mask) close(false); });
    document.getElementById('layer-root').append(mask);
  });
}

export function openModal(content, { title, width = 560, foot } = {}) {
  const mask = h('div', { class: 'modal-mask' });
  const modal = h('div', { class: `modal${width > 600 ? ' wide' : ''}`, style: `width:${width}px` });
  const api = {
    el: modal,
    close: () => mask.remove(),
  };
  modal.append(
    h('div', { class: 'modal-head' },
      h('h3', { text: title || '' }),
      h('div', { class: 'spacer' }),
      h('button', { class: 'icon-btn', onclick: () => api.close() }, '✕'),
    ),
    h('div', { class: 'modal-body' }, content),
    foot ? h('div', { class: 'modal-foot' }, foot(api)) : null,
  );
  mask.append(modal);
  mask.addEventListener('click', (e) => { if (e.target === mask) api.close(); });
  document.getElementById('layer-root').append(mask);
  return api;
}

export function openDrawer(content, { title, foot } = {}) {
  const mask = h('div', { class: 'drawer-mask' });
  const drawer = h('aside', { class: 'drawer' });
  const api = {
    el: drawer,
    close: () => {
      mask.classList.remove('open');
      drawer.classList.remove('open');
      setTimeout(() => { mask.remove(); drawer.remove(); }, 240);
    },
  };
  drawer.append(
    h('div', { class: 'drawer-head' },
      h('h3', { text: title || '' }),
      h('div', { class: 'spacer' }),
      h('button', { class: 'icon-btn', onclick: () => api.close() }, '✕'),
    ),
    h('div', { class: 'drawer-body' }, content),
    foot ? h('div', { class: 'drawer-foot' }, foot(api)) : null,
  );
  document.getElementById('layer-root').append(mask, drawer);
  requestAnimationFrame(() => {
    mask.classList.add('open');
    drawer.classList.add('open');
  });
  mask.addEventListener('click', () => api.close());
  return api;
}

//分页

export function renderPager({ total, page, pageSize, onChange }) {
  const pages = Math.max(1, Math.ceil(total / pageSize));
  const wrap = h('div', { class: 'pager' });
  wrap.append(h('span', { text: `共 ${fmt.num(total)} 条 · 第 ${page}/${pages} 页` }));
  wrap.append(h('div', { class: 'spacer' }));

  const pagesBox = h('div', { class: 'pages' });
  const btn = (label, target, opts = {}) => h('button', {
    class: `pg${opts.active ? ' active' : ''}`,
    disabled: opts.disabled ? true : null,
    onclick: () => onChange(target),
    text: label,
  });
  pagesBox.append(btn('‹', page - 1, { disabled: page <= 1 }));

  const windowSize = 5;
  let start = Math.max(1, page - Math.floor(windowSize / 2));
  let end = Math.min(pages, start + windowSize - 1);
  start = Math.max(1, end - windowSize + 1);
  for (let i = start; i <= end; i++) pagesBox.append(btn(String(i), i, { active: i === page }));

  pagesBox.append(btn('›', page + 1, { disabled: page >= pages }));
  wrap.append(pagesBox);
  return wrap;
}

//空态

export function emptyState(text = '暂无数据', sub = '') {
  return `<div class="empty">
    <div class="icon">${ICONS.inbox}</div>
    <p>${esc(text)}</p>${sub ? `<p style="margin-top:6px;font-size:12px">${esc(sub)}</p>` : ''}
  </div>`;
}

export function skeletonRows(rows = 5, cols = 4) {
  return Array.from({ length: rows }).map(() =>
    `<tr>${Array.from({ length: cols }).map(() => '<td><div class="skeleton" style="height:14px"></div></td>').join('')}</tr>`,
  ).join('');
}

//图标

export const ICONS = {
  dashboard: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="9" rx="2"/><rect x="14" y="3" width="7" height="5" rx="2"/><rect x="14" y="12" width="7" height="9" rx="2"/><rect x="3" y="16" width="7" height="5" rx="2"/></svg>',
  orders: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M6 3h9l4 4v14H6z"/><path d="M9 9h7M9 13h7M9 17h4"/></svg>',
  products: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M4 8l8-4 8 4v8l-8 4-8-4z"/><path d="M4 8l8 4 8-4M12 12v8"/></svg>',
  bundles: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M3 10h18l-2 10H5z"/><path d="M8 10V7a4 4 0 018 0v3"/></svg>',
  categories: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M4 5h7v7H4zM13 5h7v7h-7zM4 14h7v5H4zM13 14h7v5h-7z"/></svg>',
  employees: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="8" r="3.2"/><path d="M3.5 20a5.5 5.5 0 0111 0"/><path d="M16 11a3 3 0 100-6"/><path d="M17.5 20a5 5 0 00-2-4"/></svg>',
  stats: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M4 20V6M4 20h16"/><path d="M8 20v-6M12 20V9M16 20v-8M20 20V5"/></svg>',
  settings: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M12 3v3M12 18v3M3 12h3M18 12h3M5.6 5.6l2.1 2.1M16.3 16.3l2.1 2.1M18.4 5.6l-2.1 2.1M7.7 16.3l-2.1 2.1"/></svg>',
  bell: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M18 15V10a6 6 0 10-12 0v5l-2 2h16z"/><path d="M10 20a2 2 0 004 0"/></svg>',
  menu: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"><path d="M4 7h16M4 12h16M4 17h16"/></svg>',
  search: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"><circle cx="11" cy="11" r="6.5"/><path d="M16 16l4 4"/></svg>',
  plus: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M12 5v14M5 12h14"/></svg>',
  download: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M12 4v10M8 11l4 4 4-4"/><path d="M5 19h14"/></svg>',
  inbox: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M4 13l2-7h12l2 7v5H4z"/><path d="M4 13h4l1 2h6l1-2h4"/></svg>',
};
