//订单管理
import {
  api, fmt, esc, STATUS, pill, toast, delegate, renderPager, emptyState,
  skeletonRows, openDrawer, confirmDialog, ICONS,
} from '../core.js';

const state = {
  page: 1,
  pageSize: 10,
  status: '',
  number: '',
  phone: '',
  beginTime: '',
  endTime: '',
  records: [],
  total: 0,
  stats: { toBeConfirmed: 0, confirmed: 0, deliveryInProgress: 0 },
};

const TABS = [
  { key: '', label: '全部订单' },
  { key: '2', label: '待接单' },
  { key: '3', label: '已接单' },
  { key: '4', label: '派送中' },
  { key: '5', label: '已完成' },
  { key: '6', label: '已取消' },
];

function tabCount(key) {
  const s = state.stats;
  if (key === '2') return s.toBeConfirmed || 0;
  if (key === '3') return s.confirmed || 0;
  if (key === '4') return s.deliveryInProgress || 0;
  return null;
}

function actionsFor(o) {
  const btns = [];
  if (o.status === 2) {
    btns.push(`<button class="link-btn" data-act="confirm" data-id="${o.id}">接单</button>`);
    btns.push(`<button class="link-btn danger" data-act="reject" data-id="${o.id}">拒单</button>`);
  }
  if (o.status === 3) {
    btns.push(`<button class="link-btn" data-act="deliver" data-id="${o.id}">开始派送</button>`);
    btns.push(`<button class="link-btn danger" data-act="cancel" data-id="${o.id}">取消</button>`);
  }
  if (o.status === 4) btns.push(`<button class="link-btn" data-act="complete" data-id="${o.id}">完成订单</button>`);
  btns.push(`<button class="link-btn muted" data-act="detail" data-id="${o.id}">详情</button>`);
  return btns.join('');
}

export async function render(root) {
  root.innerHTML = `
    <div class="page-head">
      <h1>订单管理</h1>
      <div class="desc">接单、派送、完成与售后处理</div>
    </div>

    <div class="toolbar">
      <div class="tabs" id="tabs"></div>
    </div>

    <section class="card">
      <div class="card-body" style="padding-bottom:0">
        <div class="toolbar" style="margin-bottom:0;padding-bottom:14px">
          <input class="input search" id="f-number" placeholder="订单号" value="${esc(state.number)}">
          <input class="input" id="f-phone" style="min-width:140px" placeholder="手机号" value="${esc(state.phone)}">
          <input class="input" id="f-begin" type="date" style="min-width:140px" value="${state.beginTime}">
          <span style="color:var(--muted-2)">~</span>
          <input class="input" id="f-end" type="date" style="min-width:140px" value="${state.endTime}">
          <button class="btn btn-primary" id="btn-search">${ICONS.search}查询</button>
          <button class="btn" id="btn-reset">重置</button>
        </div>
      </div>
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>订单号</th><th>订单商品</th><th>收货人</th><th>金额</th>
              <th>下单时间</th><th>状态</th><th>操作</th>
            </tr>
          </thead>
          <tbody id="rows">${skeletonRows(6, 7)}</tbody>
        </table>
      </div>
      <div id="pager"></div>
    </section>`;

  const tabsEl = root.querySelector('#tabs');
  tabsEl.style.cssText = 'display:flex;gap:6px;flex-wrap:wrap';
  tabsEl.innerHTML = TABS.map((t) => {
    const n = tabCount(t.key);
    const active = String(state.status) === t.key;
    return `<button class="tab${active ? ' active' : ''}" data-tab="${t.key}">
      ${t.label}${n !== null ? `<b>${n}</b>` : ''}
    </button>`;
  }).join('');

  root.querySelector('#btn-search').onclick = () => {
    state.number = root.querySelector('#f-number').value.trim();
    state.phone = root.querySelector('#f-phone').value.trim();
    state.beginTime = root.querySelector('#f-begin').value;
    state.endTime = root.querySelector('#f-end').value;
    state.page = 1;
    load(root);
  };
  root.querySelector('#btn-reset').onclick = () => {
    state.number = ''; state.phone = ''; state.beginTime = ''; state.endTime = ''; state.page = 1;
    root.querySelector('#f-number').value = '';
    root.querySelector('#f-phone').value = '';
    root.querySelector('#f-begin').value = '';
    root.querySelector('#f-end').value = '';
    load(root);
  };
  tabsEl.addEventListener('click', (e) => {
    const btn = e.target.closest('[data-tab]');
    if (!btn) return;
    state.status = btn.dataset.tab;
    state.page = 1;
    render(root);
  });

  delegate(root, 'tr', 'click', () => {});
  await load(root);
}

async function load(root) {
  const rows = root.querySelector('#rows');
  rows.innerHTML = skeletonRows(6, 7);

  api.orderStatistics().then((s) => {
    state.stats = s || state.stats;
    const tabsEl = root.querySelector('#tabs');
    if (tabsEl) {
      tabsEl.innerHTML = TABS.map((t) => {
        const n = tabCount(t.key);
        const active = String(state.status) === t.key;
        return `<button class="tab${active ? ' active' : ''}" data-tab="${t.key}">${t.label}${n !== null ? `<b>${n}</b>` : ''}</button>`;
      }).join('');
    }
  }).catch(() => {});

  try {
    const res = await api.orderPage({
      page: state.page,
      pageSize: state.pageSize,
      status: state.status || undefined,
      number: state.number || undefined,
      phone: state.phone || undefined,
      beginTime: state.beginTime ? `${state.beginTime} 00:00:00` : undefined,
      endTime: state.endTime ? `${state.endTime} 23:59:59` : undefined,
    });
    state.records = res.records || [];
    state.total = res.total || 0;

    rows.innerHTML = state.records.length
      ? state.records.map((o) => `
        <tr>
          <td class="num strong">${esc(o.number)}</td>
          <td style="max-width:280px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${esc(o.productSummary || '')}">${esc(o.productSummary || '—')}</td>
          <td>
            <div class="strong">${esc(o.consignee || '—')}</div>
            <div class="num" style="font-size:12px;color:var(--muted)">${esc(o.phone || '')}</div>
          </td>
          <td class="num strong">${fmt.money(o.amount)}</td>
          <td class="num nowrap">${fmt.dt(o.orderTime)}</td>
          <td class="nowrap">${pill(STATUS.order, o.status)}</td>
          <td class="nowrap"><div class="row-actions">${actionsFor(o)}</div></td>
        </tr>`).join('')
      : `<tr><td colspan="7">${emptyState('没有符合条件的订单', '换个筛选条件试试')}</td></tr>`;

    const pagerBox = root.querySelector('#pager');
    pagerBox.innerHTML = '';
    pagerBox.append(renderPager({
      total: state.total,
      page: state.page,
      pageSize: state.pageSize,
      onChange: (p) => { state.page = p; load(root); },
    }));
  } catch (e) {
    rows.innerHTML = `<tr><td colspan="7">${emptyState('加载失败', e.message)}</td></tr>`;
  }
}

//操作

document.addEventListener('click', async (e) => {
  const btn = e.target.closest('[data-act]');
  if (!btn) return;
  const id = btn.dataset.id;
  const act = btn.dataset.act;
  if (act === 'detail') return showDetail(id);

  if (act === 'reject') {
    const reason = await promptReason('拒单原因', '例如：商品缺货，建议改单');
    if (!reason) return;
    return run(btn, () => api.orderReject(id, reason), '已拒单');
  }
  if (act === 'cancel') {
    const reason = await promptReason('取消原因', '例如：顾客电话要求取消');
    if (!reason) return;
    return run(btn, () => api.orderCancel(id, reason), '订单已取消');
  }
  if (act === 'confirm') {
    const ok = await confirmDialog({ title: '确认接单', message: '接单后请尽快拣货，顾客端会看到「商家已接单」。', okText: '接单' });
    if (!ok) return;
    return run(btn, () => api.orderConfirm(id), '已接单');
  }
  if (act === 'deliver') return run(btn, () => api.orderDeliver(id), '已开始派送');
  if (act === 'complete') {
    const ok = await confirmDialog({ title: '完成订单', message: '确认顾客已收到商品？完成后订单将计入销售额。', okText: '确认完成' });
    if (!ok) return;
    return run(btn, () => api.orderComplete(id), '订单已完成');
  }
});

async function run(btn, fn, message) {
  btn.disabled = true;
  try {
    await fn();
    toast(message);
    const root = document.getElementById('view-root');
    if (root) await load(root);
  } catch (err) {
    toast(err.message, 'err');
  } finally {
    btn.disabled = false;
  }
}

function promptReason(title, placeholder) {
  return new Promise((resolve) => {
    const mask = document.createElement('div');
    mask.className = 'modal-mask';
    mask.innerHTML = `
      <div class="modal" style="width:440px">
        <div class="modal-head"><h3>${esc(title)}</h3><div class="spacer"></div></div>
        <div class="modal-body">
          <div class="field"><label>请填写原因（会展示给顾客）</label>
            <textarea class="textarea" id="reason-input" placeholder="${esc(placeholder)}"></textarea>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn" data-cancel>取消</button>
          <button class="btn btn-primary" data-ok>确定</button>
        </div>
      </div>`;
    mask.querySelector('[data-cancel]').onclick = () => { mask.remove(); resolve(''); };
    mask.querySelector('[data-ok]').onclick = () => {
      const v = mask.querySelector('#reason-input').value.trim();
      if (!v) { mask.querySelector('#reason-input').focus(); return; }
      mask.remove();
      resolve(v);
    };
    mask.addEventListener('click', (e) => { if (e.target === mask) { mask.remove(); resolve(''); } });
    document.getElementById('layer-root').append(mask);
    setTimeout(() => mask.querySelector('#reason-input').focus(), 30);
  });
}

//详情

async function showDetail(id) {
  const body = document.createElement('div');
  body.innerHTML = '<div class="skeleton" style="height:16px;width:40%"></div><div class="skeleton" style="height:80px;margin-top:16px"></div>';
  const drawer = openDrawer(body, { title: '订单详情' });
  try {
    const o = await api.orderDetail(id);
    const items = o.orderItemList || [];
    const steps = [
      { at: o.orderTime, label: '提交订单', done: true },
      { at: o.checkoutTime, label: '支付完成', done: !!o.payStatus },
      { at: o.deliveryTime, label: '开始派送', done: [4, 5].includes(o.status) },
      { at: o.status === 5 ? o.deliveryTime : null, label: '订单完成', done: o.status === 5 },
    ];
    body.innerHTML = `
      <div class="detail-section">
        <div style="display:flex;align-items:center;gap:10px;margin-bottom:14px">
          <span class="num" style="font-family:var(--mono);font-size:15px;font-weight:600">${esc(o.number)}</span>
          ${pill(STATUS.order, o.status)}
          ${pill(STATUS.pay, o.payStatus)}
        </div>
        <dl class="kv">
          <dt>收货人</dt><dd>${esc(o.consignee || '—')} · ${esc(o.phone || '')}</dd>
          <dt>收货地址</dt><dd>${esc(o.address || '—')}</dd>
          <dt>下单时间</dt><dd class="num">${fmt.dt(o.orderTime)}</dd>
          <dt>预计送达</dt><dd class="num">${fmt.dt(o.estimatedDeliveryTime)}</dd>
          <dt>订单备注</dt><dd>${esc(o.remark || '无')}</dd>
          ${o.cancelReason ? `<dt>取消原因</dt><dd>${esc(o.cancelReason)}</dd>` : ''}
          ${o.rejectionReason ? `<dt>拒单原因</dt><dd>${esc(o.rejectionReason)}</dd>` : ''}
        </dl>
      </div>

      <div class="detail-section">
        <h4>商品明细</h4>
        <div class="order-items">
          ${items.length ? items.map((it) => `
            <div class="order-item">
              <img src="${esc(it.image || '/img/placeholder.png')}" alt="" onerror="this.src='/img/placeholder.png'">
              <div class="meta">
                <div class="n">${esc(it.name)}${it.specText ? `<span style="color:var(--muted-2)"> · ${esc(it.specText)}</span>` : ''}</div>
                <div class="s">数量 × ${it.number}</div>
              </div>
              <div class="amt">${fmt.money(it.amount)}</div>
            </div>`).join('') : '<p class="hint">无商品明细</p>'}
        </div>
      </div>

      <div class="detail-section">
        <h4>金额</h4>
        <dl class="kv">
          <dt>商品金额</dt><dd class="num">${fmt.money(o.amount)}</dd>
          <dt>打包费</dt><dd class="num">${fmt.money(o.packAmount || 0)}</dd>
          <dt style="color:var(--ink);font-weight:600">实收金额</dt>
          <dd class="num" style="font-weight:600;color:var(--green-700);font-size:15px">${fmt.money(Number(o.amount || 0) + Number(o.packAmount || 0))}</dd>
        </dl>
      </div>

      <div class="detail-section">
        <h4>订单进度</h4>
        <div class="timeline">
          ${steps.map((s) => `<div class="step${s.done ? ' done' : ''}"><i></i><div><b>${s.label}</b>${s.at ? fmt.dt(s.at) : '待处理'}</div></div>`).join('')}
        </div>
      </div>`;
  } catch (e) {
    body.innerHTML = `<p class="hint">${esc(e.message)}</p>`;
  }
}
