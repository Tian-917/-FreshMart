//工作台
import { api, fmt, h, esc, pill, STATUS, toast, delegate, emptyState, ICONS } from '../core.js';
import { lineChart, donutChart } from '../charts.js';

function ymd(d) {
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}

function lastNDays(n) {
  const out = [];
  const now = new Date();
  for (let i = n - 1; i >= 0; i--) {
    const d = new Date(now);
    d.setDate(now.getDate() - i);
    out.push(ymd(d));
  }
  return out;
}

export async function render(root) {
  root.innerHTML = `
    <div class="page-head">
      <h1>门店工作台</h1>
      <div class="desc">${new Date().toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' })} · 数据实时统计</div>
      <div class="spacer"></div>
      <button class="btn" id="reload">${ICONS.stats}<span>刷新数据</span></button>
    </div>
    <div id="wb-body">
      <div class="kpi-grid">
        ${Array.from({ length: 5 }).map(() => '<div class="kpi"><div class="skeleton" style="height:12px;width:52px"></div><div class="skeleton" style="height:24px;width:90px;margin-top:14px"></div></div>').join('')}
      </div>
    </div>`;

  let today;
  try {
    today = await api.businessData();
  } catch (e) {
    toast(e.message, 'err');
  }

  const days = lastNDays(7);

  const [orderOv, productOv, bundleOv, gmv, waiting, productList] = await Promise.all([
    api.orderOverview().catch(() => ({})),
    api.productOverview().catch(() => ({})),
    api.bundleOverview().catch(() => ({})),
    api.gmvReport(days[0], days[6]).catch(() => ({ dateList: '', gmvList: '' })),
    api.orderPage({ page: 1, pageSize: 5, status: 2 }).catch(() => ({ total: 0, records: [] })),
    api.productPage({ page: 1, pageSize: 200 }).catch(() => ({ records: [] })),
  ]);

  const lowStock = (productList.records || [])
    .filter((p) => p.status === 1 && p.stock != null && p.stock <= 10)
    .sort((a, b) => a.stock - b.stock);

  const biz = today || { gmv: 0, validOrderCount: 0, orderCompletionRate: 0, unitPrice: 0, newUsers: 0 };
  const gmvLabels = (gmv.dateList || '').split(',').filter(Boolean);
  const gmvValues = (gmv.gmvList || '').split(',').map((v) => Number(v) || 0);

  const body = `
    <div class="kpi-grid">
      <div class="kpi accent">
        <div class="label">今日销售额</div>
        <div class="value">${fmt.money(biz.gmv)}</div>
        <div class="foot">含已完成订单实收金额</div>
      </div>
      <div class="kpi">
        <div class="label">有效订单</div>
        <div class="value">${fmt.num(biz.validOrderCount)}<small>单</small></div>
        <div class="foot">已完成并计入口径</div>
      </div>
      <div class="kpi">
        <div class="label">订单完成率</div>
        <div class="value">${fmt.pct(biz.orderCompletionRate)}</div>
        <div class="foot">有效订单 / 全部订单</div>
      </div>
      <div class="kpi">
        <div class="label">客单价</div>
        <div class="value">${fmt.money(biz.unitPrice)}</div>
        <div class="foot">销售额 / 有效订单</div>
      </div>
      <div class="kpi">
        <div class="label">新增用户</div>
        <div class="value">${fmt.num(biz.newUsers)}<small>人</small></div>
        <div class="foot">今日新注册会员</div>
      </div>
    </div>

    <div class="grid-2">
      <section class="card">
        <div class="card-head">
          <h3>订单概览</h3><span class="sub">今日 ${fmt.num(orderOv.allOrders)} 单</span>
          <div class="spacer"></div>
          <a class="link-btn" href="#/orders">全部订单 →</a>
        </div>
        <div class="card-body">
          <div class="stat-row" style="grid-template-columns:repeat(5,1fr)">
            <div class="stat"><div class="l">待接单</div><div class="v ${orderOv.waitingOrders ? 'warn' : ''}">${fmt.num(orderOv.waitingOrders)}</div></div>
            <div class="stat"><div class="l">派送中</div><div class="v">${fmt.num(orderOv.deliveredOrders)}</div></div>
            <div class="stat"><div class="l">已完成</div><div class="v ok">${fmt.num(orderOv.completedOrders)}</div></div>
            <div class="stat"><div class="l">已取消</div><div class="v">${fmt.num(orderOv.cancelledOrders)}</div></div>
            <div class="stat"><div class="l">全部订单</div><div class="v">${fmt.num(orderOv.allOrders)}</div></div>
          </div>
          <div class="ov-donut">
            ${(() => {
              //总览只给了四个状态，剩下的（待付款、已接单）统一归到"其他"
              const known = (orderOv.waitingOrders || 0) + (orderOv.deliveredOrders || 0)
                + (orderOv.completedOrders || 0) + (orderOv.cancelledOrders || 0);
              const rest = Math.max(0, (orderOv.allOrders || 0) - known);
              return donutChart({
                size: 132,
                thickness: 15,
                items: [
                  { name: '待接单', value: orderOv.waitingOrders || 0, color: '#e8a33d' },
                  { name: '派送中', value: orderOv.deliveredOrders || 0, color: '#3d8e6a' },
                  { name: '已完成', value: orderOv.completedOrders || 0, color: '#2f9e44' },
                  { name: '已取消', value: orderOv.cancelledOrders || 0, color: '#c9d3cc' },
                  { name: '其他', value: rest, color: '#dfe6e1' },
                ],
              });
            })()}
          </div>
        </div>
      </section>

      <section class="card">
        <div class="card-head"><h3>商品概况</h3><div class="spacer"></div></div>
        <div class="card-body">
          <div class="overview-grid">
            <a class="overview-item" href="#/products">
              <div class="ico">${ICONS.products}</div>
              <div><div class="k">在售商品</div><div class="n">${fmt.num(productOv.sold)}</div></div>
            </a>
            <a class="overview-item" href="#/products">
              <div class="ico" style="background:#f3f5f3;color:#8a998f">${ICONS.products}</div>
              <div><div class="k">下架商品</div><div class="n">${fmt.num(productOv.discontinued)}</div></div>
            </a>
            <a class="overview-item" href="#/bundles">
              <div class="ico">${ICONS.bundles}</div>
              <div><div class="k">在售组合装</div><div class="n">${fmt.num(bundleOv.sold)}</div></div>
            </a>
            <a class="overview-item" href="#/bundles">
              <div class="ico" style="background:#f3f5f3;color:#8a998f">${ICONS.bundles}</div>
              <div><div class="k">下架组合装</div><div class="n">${fmt.num(bundleOv.discontinued)}</div></div>
            </a>
          </div>
          <div class="stock-warn">
            <div class="sw-head">
              <span>库存预警</span>
              <span class="sw-count">${fmt.num(lowStock.length)} 款</span>
              <div class="spacer"></div>
              <a class="link-btn" href="#/products">去补货 →</a>
            </div>
            ${lowStock.length
              ? `<div class="sw-list">${lowStock.slice(0, 3).map((p) => `
                  <span class="sw-item"><b>${esc(p.name)}</b><em>${fmt.num(p.stock)}</em></span>`).join('')}</div>`
              : '<p class="sw-empty">暂无低于 10 件的在售商品</p>'}
          </div>
        </div>
      </section>
    </div>

    <div class="grid-2">
      <section class="card">
        <div class="card-head">
          <h3>近 7 日销售额</h3>
          <div class="spacer"></div>
          <a class="link-btn" href="#/stats">数据统计 →</a>
        </div>
        <div class="card-body">
          ${gmvLabels.length
            ? lineChart({ labels: gmvLabels, series: [{ name: '销售额', color: '#2f9e44', values: gmvValues }] })
            : emptyState('近 7 日没有销售数据')}
          <div class="legend"><span><i style="background:#2f9e44"></i>销售额（元）</span></div>
        </div>
      </section>

      <section class="card">
        <div class="card-head">
          <h3>待接单</h3>
          <span class="sub">全部待处理 ${fmt.num(waiting.total)} 单</span>
          <div class="spacer"></div>
          <a class="link-btn" href="#/orders">去处理 →</a>
        </div>
        <div class="card-body tight">
          ${(waiting.records || []).length ? `
            <div class="table-wrap">
              <table class="table">
                <thead><tr><th>订单号</th><th>金额</th><th>下单时间</th><th></th></tr></thead>
                <tbody>
                  ${waiting.records.map((o) => `
                    <tr>
                      <td class="num strong">${esc(o.number)}</td>
                      <td class="num">${fmt.money(o.amount)}</td>
                      <td class="num nowrap">${fmt.time(o.orderTime)}</td>
                      <td class="nowrap"><button class="link-btn" data-accept="${o.id}">接单</button></td>
                    </tr>`).join('')}
                </tbody>
              </table>
            </div>` : emptyState('暂无待接单', '新订单会自动出现在这里')}
        </div>
      </section>
    </div>`;

  root.innerHTML = `
    <div class="page-head">
      <h1>门店工作台</h1>
      <div class="desc">${new Date().toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' })} · 数据实时统计</div>
      <div class="spacer"></div>
      <button class="btn" id="reload">${ICONS.stats}<span>刷新数据</span></button>
    </div>` + body;

  document.getElementById('reload')?.addEventListener('click', () => render(root));

  delegate(root, '[data-accept]', 'click', async (e, el) => {
    const id = el.dataset.accept;
    el.disabled = true;
    try {
      await api.orderConfirm(id);
      toast('已接单，请尽快拣货');
      render(root);
    } catch (err) {
      toast(err.message, 'err');
      el.disabled = false;
    }
  });
}
