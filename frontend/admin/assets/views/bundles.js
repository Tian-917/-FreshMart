//组合商品
import {
  api, fmt, esc, STATUS, pill, toast, delegate, renderPager, emptyState,
  skeletonRows, openModal, confirmDialog, ICONS, h,
} from '../core.js';

const state = { page: 1, pageSize: 10, name: '', status: '', total: 0, records: [], categories: [], products: [] };

export async function render(root) {
  root.innerHTML = `
    <div class="page-head">
      <h1>组合商品</h1>
      <div class="desc">把多个商品打包售卖，下单时按明细扣减单品库存</div>
      <div class="spacer"></div>
      <button class="btn btn-primary" id="btn-new">${ICONS.plus}<span>新增组合</span></button>
    </div>

    <section class="card">
      <div class="card-body" style="padding-bottom:0">
        <div class="toolbar" style="margin-bottom:0;padding-bottom:14px">
          <input class="input search" id="f-name" placeholder="组合名称" value="${esc(state.name)}">
          <select class="select" id="f-status">
            <option value="">全部状态</option>
            <option value="1"${state.status === '1' ? ' selected' : ''}>在售</option>
            <option value="0"${state.status === '0' ? ' selected' : ''}>已下架</option>
          </select>
          <button class="btn btn-primary" id="btn-search">${ICONS.search}查询</button>
          <button class="btn" id="btn-reset">重置</button>
        </div>
      </div>
      <div class="table-wrap">
        <table class="table">
          <thead><tr><th>组合商品</th><th>包含单品</th><th>价格</th><th>状态</th><th>更新时间</th><th>操作</th></tr></thead>
          <tbody id="rows">${skeletonRows(5, 6)}</tbody>
        </table>
      </div>
      <div id="pager"></div>
    </section>`;

  const [cats, prods] = await Promise.all([
    api.categoryList(2).catch(() => []),
    api.productList({ status: 1 }).catch(() => []),
  ]);
  state.categories = cats;
  state.products = prods;

  root.querySelector('#btn-search').onclick = () => {
    state.name = root.querySelector('#f-name').value.trim();
    state.status = root.querySelector('#f-status').value;
    state.page = 1;
    load(root);
  };
  root.querySelector('#btn-reset').onclick = () => {
    state.name = ''; state.status = ''; state.page = 1;
    root.querySelector('#f-name').value = '';
    root.querySelector('#f-status').value = '';
    load(root);
  };
  root.querySelector('#btn-new').onclick = () => editBundle(null, root);

  delegate(root, '[data-id]', 'click', async (e, el) => {
    const id = el.dataset.id;
    const act = el.dataset.act;
    if (act === 'edit') return editBundle(id, root);
    if (act === 'toggle') {
      const status = el.dataset.status === '1' ? 0 : 1;
      try {
        await api.bundleStatus(id, status);
        toast(status === 1 ? '已上架' : '已下架');
        load(root);
      } catch (err) { toast(err.message, 'err'); }
    }
    if (act === 'del') {
      const ok = await confirmDialog({ title: '删除组合商品', message: '删除后顾客将无法再购买该组合，确定删除？', okText: '删除', danger: true });
      if (!ok) return;
      try {
        await api.bundleDelete([id]);
        toast('已删除');
        load(root);
      } catch (err) { toast(err.message, 'err'); }
    }
  });

  await load(root);
}

async function load(root) {
  const rows = root.querySelector('#rows');
  rows.innerHTML = skeletonRows(5, 6);
  try {
    const res = await api.bundlePage({
      page: state.page, pageSize: state.pageSize,
      name: state.name || undefined, status: state.status || undefined,
    });
    state.records = res.records || [];
    state.total = res.total || 0;

    rows.innerHTML = state.records.length ? state.records.map((b) => {
      const items = b.items || [];
      return `<tr>
        <td>
          <div style="display:flex;align-items:center;gap:10px">
            <img class="thumb" src="${esc(b.image || '')}" alt="" onerror="this.style.visibility='hidden'">
            <div><div class="strong">${esc(b.name)}</div>
              <div style="font-size:12px;color:var(--muted-2)">#${b.id} · ${esc(b.categoryName || '')}</div></div>
          </div>
        </td>
        <td style="max-width:320px">
          ${items.length ? items.map((it) => `<span class="tag">${esc(it.name)} ×${it.copies}</span>`).join('') : '<span class="hint">无明细</span>'}
        </td>
        <td class="num strong">${fmt.money(b.price)}</td>
        <td>${pill(STATUS.shelf, b.status)}</td>
        <td class="num nowrap">${fmt.dt(b.updateTime)}</td>
        <td class="nowrap"><div class="row-actions">
          <button class="link-btn" data-act="edit" data-id="${b.id}">编辑</button>
          <button class="link-btn" data-act="toggle" data-id="${b.id}" data-status="${b.status}">${b.status === 1 ? '下架' : '上架'}</button>
          <button class="link-btn danger" data-act="del" data-id="${b.id}">删除</button>
        </div></td>
      </tr>`;
    }).join('') : `<tr><td colspan="6">${emptyState('还没有组合商品', '把季节性商品打包售卖试试')}</td></tr>`;

    const pagerBox = root.querySelector('#pager');
    pagerBox.innerHTML = '';
    pagerBox.append(renderPager({
      total: state.total, page: state.page, pageSize: state.pageSize,
      onChange: (p) => { state.page = p; load(root); },
    }));
  } catch (e) {
    rows.innerHTML = `<tr><td colspan="6">${emptyState('加载失败', e.message)}</td></tr>`;
  }
}

//表单

async function editBundle(id, root) {
  let data = { name: '', categoryId: state.categories[0]?.id || '', price: '', image: '', description: '', status: 1, items: [] };
  if (id) {
    try { data = await api.bundleGet(id); } catch (e) { toast(e.message, 'err'); return; }
  }

  const form = document.createElement('div');
  form.innerHTML = `
    <div class="form-grid">
      <div class="field full">
        <label>组合名称</label>
        <input class="input" id="b-name" value="${esc(data.name)}" placeholder="例如：一周轻食蔬菜包">
      </div>
      <div class="field">
        <label>分类</label>
        <select class="select" id="b-cat">
          ${state.categories.map((c) => `<option value="${c.id}"${String(data.categoryId) === String(c.id) ? ' selected' : ''}>${esc(c.name)}</option>`).join('')}
        </select>
      </div>
      <div class="field">
        <label>组合售价（元）</label>
        <input class="input" id="b-price" type="number" step="0.01" value="${data.price ?? ''}" placeholder="39.90">
      </div>
      <div class="field">
        <label>状态</label>
        <select class="select" id="b-status">
          <option value="1"${data.status === 1 ? ' selected' : ''}>上架在售</option>
          <option value="0"${data.status === 0 ? ' selected' : ''}>下架</option>
        </select>
      </div>
      <div class="field full">
        <label>组合图片</label>
        <div class="img-row">
          <input class="input" id="b-image" value="${esc(data.image || '')}" placeholder="/static/goods/bundle.png">
          <input type="file" id="b-file" accept="image/*" hidden>
          <button class="btn" id="b-upload" type="button">选择图片</button>
          <img class="img-preview" id="b-preview" src="${esc(data.image || '')}" alt="" onerror="this.style.visibility='hidden'">
        </div>
      </div>
      <div class="field full">
        <label>组合描述</label>
        <textarea class="textarea" id="b-desc">${esc(data.description || '')}</textarea>
      </div>
      <div class="field full">
        <label style="display:flex;align-items:center;justify-content:space-between">
          <span>包含单品（下单时按份数拆开扣库存）</span>
          <button class="link-btn" id="b-add-item" type="button">+ 添加单品</button>
        </label>
        <div class="item-list" id="b-items"></div>
        <p class="hint" id="b-total"></p>
      </div>
    </div>`;

  const itemsBox = form.querySelector('#b-items');
  const totalEl = form.querySelector('#b-total');

  const recalc = () => {
    const sum = [...itemsBox.querySelectorAll('.item-row')]
      .reduce((acc, r) => acc + Number(r.querySelector('[data-k="price"]').value || 0) * Number(r.querySelector('[data-k="copies"]').value || 1), 0);
    totalEl.textContent = `单品原价合计 ¥${sum.toFixed(2)}` + (data.price ? ` · 组合价 ¥${Number(data.price).toFixed(2)}` : '');
  };

  const itemRow = (it = {}) => {
    const row = document.createElement('div');
    row.className = 'item-row';
    row.innerHTML = `
      <select class="select" data-k="productId">
        <option value="">选择单品…</option>
        ${state.products.map((p) => `<option value="${p.id}"${String(it.productId) === String(p.id) ? ' selected' : ''}>${esc(p.name)}（${fmt.money(p.price)}）</option>`).join('')}
      </select>
      <input class="input" data-k="name" placeholder="名称" value="${esc(it.name || '')}">
      <input class="input" data-k="price" type="number" step="0.01" style="width:110px" placeholder="单价" value="${it.price ?? ''}">
      <input class="input" data-k="copies" type="number" style="width:80px" placeholder="份数" value="${it.copies ?? 1}">
      <button class="icon-btn" type="button" data-del>✕</button>`;
    row.querySelector('[data-k="productId"]').addEventListener('change', (e) => {
      const p = state.products.find((x) => String(x.id) === e.target.value);
      if (p) {
        row.querySelector('[data-k="name"]').value = p.name;
        row.querySelector('[data-k="price"]').value = p.price;
      }
      recalc();
    });
    row.querySelector('[data-k="price"]').addEventListener('input', recalc);
    row.querySelector('[data-k="copies"]').addEventListener('input', recalc);
    row.querySelector('[data-del]').onclick = () => { row.remove(); recalc(); };
    return row;
  };

  (data.items || []).forEach((it) => itemsBox.append(itemRow(it)));
  form.querySelector('#b-add-item').onclick = () => { itemsBox.append(itemRow()); recalc(); };
  form.querySelector('#b-image').addEventListener('input', (e) => {
    const img = form.querySelector('#b-preview');
    img.src = e.target.value;
    img.style.visibility = e.target.value ? 'visible' : 'hidden';
  });

  const fileInput = form.querySelector('#b-file');
  const uploadBtn = form.querySelector('#b-upload');
  uploadBtn.onclick = () => fileInput.click();
  fileInput.onchange = async () => {
    const file = fileInput.files[0];
    if (!file) return;
    uploadBtn.disabled = true;
    uploadBtn.textContent = '上传中...';
    try {
      const url = await api.upload(file);
      form.querySelector('#b-image').value = url;
      const img = form.querySelector('#b-preview');
      img.src = url;
      img.style.visibility = 'visible';
      toast('图片已上传');
    } catch (err) {
      toast(err.message, 'err');
    } finally {
      uploadBtn.disabled = false;
      uploadBtn.textContent = '选择图片';
      fileInput.value = '';
    }
  };
  if (!(data.items || []).length) itemsBox.append(itemRow());
  recalc();

  openModal(form, {
    title: id ? '编辑组合商品' : '新增组合商品',
    width: 780,
    foot: (m) => [
      h('button', { class: 'btn', onclick: () => m.close() }, '取消'),
      h('button', {
        class: 'btn btn-primary',
        onclick: async (e) => {
          const btn = e.currentTarget;
          const items = [...itemsBox.querySelectorAll('.item-row')].map((r) => {
            const pid = Number(r.querySelector('[data-k="productId"]').value) || null;
            return {
              productId: pid,
              name: r.querySelector('[data-k="name"]').value.trim(),
              price: Number(r.querySelector('[data-k="price"]').value || 0),
              copies: Number(r.querySelector('[data-k="copies"]').value || 1),
            };
          }).filter((x) => x.productId);
          const payload = {
            id: id || undefined,
            name: form.querySelector('#b-name').value.trim(),
            categoryId: Number(form.querySelector('#b-cat').value) || undefined,
            price: Number(form.querySelector('#b-price').value),
            status: Number(form.querySelector('#b-status').value),
            image: form.querySelector('#b-image').value.trim(),
            description: form.querySelector('#b-desc').value.trim(),
            items,
          };
          if (!payload.name) return toast('请填写组合名称', 'err');
          if (!items.length) return toast('至少添加一个单品', 'err');
          btn.disabled = true;
          try {
            if (id) await api.bundleUpdate(payload);
            else await api.bundleSave(payload);
            toast(id ? '组合商品已更新' : '组合商品已创建');
            m.close();
            load(root);
          } catch (err) {
            toast(err.message, 'err');
            btn.disabled = false;
          }
        },
      }, id ? '保存' : '创建'),
    ],
  });
  setTimeout(() => form.querySelector('#b-name').focus(), 40);
}
