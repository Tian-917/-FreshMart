//数据统计
import { api, fmt, esc, toast, ICONS, h } from '../core.js';
import { lineChart, barChart, hbarChart } from '../charts.js';

const state = { begin: '', end: '', data: null };

function iso(d) {
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}
function daysAgo(n) {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return iso(d);
}
function nums(s) {
  return String(s || '').split(',').filter((x) => x !== '').map((x) => Number(x));
}
function strs(s) {
  return String(s || '').split(',').filter((x) => x !== '');
}

const PRESETS = [
  { key: 7, label: '近 7 天' },
  { key: 30, label: '近 30 天' },
  { key: 0, label: '本月' },
];

export async function render(root) {
  if (!state.begin || !state.end) {
    const end = new Date();
    const begin = new Date();
    begin.setDate(begin.getDate() - 6);
    state.begin = iso(begin);
    state.end = iso(end);
  }

  root.innerHTML = `
    <div class="page-head">
      <h1>数据统计</h1>
      <div class="desc">销售额、订单与用户增长趋势</div>
      <div class="spacer"></div>
      <button class="btn" id="btn-export">${ICONS.download}<span>导出报表</span></button>
    </div>

    <div class="toolbar">
      <div style="display:flex;gap:6px" id="presets">
        ${PRESETS.map((p) => `<button class="seg" data-days="${p.key}">${p.label}</button>`).join('')}
      </div>
      <input class="input" type="date" id="d-begin" value="${state.begin}" style="min-width:150px">
      <span style="color:var(--muted-2)">~</span>
      <input class="input" type="date" id="d-end" value="${state.end}" style="min-width:150px">
      <button class="btn btn-primary" id="btn-query">查询</button>
    </div>

    <div class="kpi-grid cols-4" id="kpis">${Array.from({ length: 4 }).map(() => '<div class="kpi"><div class="skeleton" style="height:12px;width:50%"></div><div class="skeleton" style="height:26px;margin-top:10px"></div></div>').join('')}</div>

    <div class="grid-2">
      <section class="card">
        <div class="card-head"><h3>销售额趋势</h3><span class="sub">单位：元</span></div>
        <div class="card-body" id="c-gmv"><div class="skeleton" style="height:210px"></div></div>
      </section>
      <section class="card">
        <div class="card-head"><h3>用户增长</h3><span class="sub">累计 / 新增</span></div>
        <div class="card-body" id="c-user"><div class="skeleton" style="height:210px"></div></div>
      </section>
    </div>

    <section class="card" style="margin-bottom:16px">
      <div class="card-head"><h3>订单量趋势</h3><span class="sub">全部订单 / 有效订单</span></div>
      <div class="card-body" id="c-order"><div class="skeleton" style="height:160px"></div></div>
    </section>

    <section class="card">
      <div class="card-head"><h3>销量 Top 10</h3><span class="sub">按售出件数排序</span></div>
      <div class="card-body" id="c-top10"><div class="skeleton" style="height:300px"></div></div>
    </section>`;

  root.querySelector('#presets').addEventListener('click', (e) => {
    const b = e.target.closest('[data-days]');
    if (!b) return;
    const days = Number(b.dataset.days);
    if (days === 0) {
      const d = new Date();
      state.begin = iso(new Date(d.getFullYear(), d.getMonth(), 1));
      state.end = iso(d);
    } else {
      state.begin = daysAgo(days - 1);
      state.end = iso(new Date());
    }
    root.querySelector('#d-begin').value = state.begin;
    root.querySelector('#d-end').value = state.end;
    load(root);
  });
  root.querySelector('#btn-query').onclick = () => {
    state.begin = root.querySelector('#d-begin').value;
    state.end = root.querySelector('#d-end').value;
    load(root);
  };
  root.querySelector('#btn-export').onclick = async (e) => {
    const btn = e.currentTarget;
    btn.disabled = true;
    try {
      const blob = await api.exportReport(state.begin, state.end);
      const url = URL.createObjectURL(blob);
      const a = h('a', { href: url, download: `销售报表-${state.begin}_${state.end}.xlsx` });
      document.body.append(a);
      a.click();
      a.remove();
      setTimeout(() => URL.revokeObjectURL(url), 3000);
      toast('报表已开始下载');
    } catch (err) {
      toast(err.message, 'err');
    } finally {
      btn.disabled = false;
    }
  };

  await load(root);
}

async function load(root) {
  const [gmv, orders, users, top10] = await Promise.all([
    api.gmvReport(state.begin, state.end).catch(() => null),
    api.orderReport(state.begin, state.end).catch(() => null),
    api.userReport(state.begin, state.end).catch(() => null),
    api.top10(state.begin, state.end).catch(() => null),
  ]);

  const labels = strs(gmv?.dateList || orders?.dateList || users?.dateList);
  const gmvValues = nums(gmv?.gmvList);
  const orderCounts = nums(orders?.orderCountList);
  const validCounts = nums(orders?.validOrderCountList);
  const totalUsers = nums(users?.totalUserList);
  const newUsers = nums(users?.newUserList);

  const totalGmv = gmvValues.reduce((a, b) => a + b, 0);
  const validOrderCount = orders?.validOrderCount ?? validCounts.reduce((a, b) => a + b, 0);
  const completionRate = orders?.orderCompletionRate ?? (validOrderCount && orderCounts.length
    ? validOrderCount / orderCounts.reduce((a, b) => a + b, 0) : 0);
  const newUserTotal = newUsers.reduce((a, b) => a + b, 0);

  root.querySelector('#kpis').innerHTML = `
    <div class="kpi"><div class="label">销售额</div><div class="value">${fmt.money(totalGmv)}</div><div class="foot">${state.begin} ~ ${state.end}</div></div>
    <div class="kpi"><div class="label">有效订单</div><div class="value">${fmt.num(validOrderCount)}<small>单</small></div><div class="foot">已支付并完成</div></div>
    <div class="kpi"><div class="label">订单完成率</div><div class="value">${fmt.pct(completionRate)}</div><div class="foot">有效订单 / 全部订单</div></div>
    <div class="kpi"><div class="label">新增用户</div><div class="value">${fmt.num(newUserTotal)}<small>人</small></div><div class="foot">期间首次下单用户</div></div>`;

  root.querySelector('#c-gmv').innerHTML = labels.length
    ? lineChart({ labels, series: [{ name: '销售额', color: '#2f9e44', values: gmvValues }] })
    : '<p class="hint">该时间段没有数据</p>';

  root.querySelector('#c-user').innerHTML = labels.length
    ? lineChart({
      labels,
      series: [
        { name: '累计用户', color: '#1f7a32', values: totalUsers, area: false },
        { name: '新增用户', color: '#6fce8b', values: newUsers },
      ],
      format: (v) => fmt.num(v),
      integer: true,
    }) + '<div class="legend"><span><i style="background:#1f7a32"></i>累计用户</span><span><i style="background:#6fce8b"></i>新增用户</span></div>'
    : '<p class="hint">该时间段没有数据</p>';

  root.querySelector('#c-order').innerHTML = labels.length
    ? barChart({
      labels,
      height: 160,
      series: [
        { name: '全部订单', color: '#2f9e44', values: orderCounts },
        { name: '有效订单', color: '#9fd8b0', values: validCounts },
      ],
      legend: '<span><i style="background:#2f9e44"></i>全部订单</span><span><i style="background:#9fd8b0"></i>有效订单</span>',
    })
    : '<p class="hint">该时间段没有数据</p>';

  const topLabels = strs(top10?.productNameList);
  const topValues = nums(top10?.soldCountList);
  root.querySelector('#c-top10').innerHTML = topLabels.length
    ? hbarChart({ labels: topLabels, values: topValues })
    : '<p class="hint">该时间段没有销量数据</p>';
}
