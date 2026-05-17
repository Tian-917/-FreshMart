//商品管理
import {
  api, fmt, esc, STATUS, pill, toast, delegate, renderPager, emptyState,
  skeletonRows, openModal, confirmDialog, ICONS, h,
} from '../core.js';

const state = { page: 1, pageSize: 10, name: '', categoryId: '', status: '', total: 0, records: [], categories: [] };

export async function render(root) {
  root.innerHTML = `
    <div class="page-head">
      <h1>商品管理</h1>
      <div class="desc">在售商品的资料、价格与库存</div>
      <div class="spacer"></div>
      <button class="btn btn-primary" id="btn-new">${ICONS.plus}<span>新增商品</span></button>
    </div>

    <section class="card">
      <div class="card-body" style="padding-bottom:0">
        <div class="toolbar" style="margin-bottom:0;padding-bottom:14px">
          <input class="input search" id="f-name" placeholder="商品名称" value="${esc(state.name)}">
          <select class="select" id="f-cat"><option value="">全部分类</option></select>
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
          <thead>
            <tr><th>商品</th><th>分类</th><th>价格</th><th>库存</th><th>规格</th><th>状态</th><th>更新时间</th><th>操作</th></tr>
          </thead>
          <tbody id="rows">${skeletonRows(6, 8)}</tbody>
        </table>
      </div>
      <div id="pager"></div>
    </section>`;

  state.categories = await api.categoryList(1).catch(() => []);
  const catSel = root.querySelector('#f-cat');
  catSel.innerHTML = '<option value="">全部分类</option>' +
    state.categories.map((c) => `<option value="${c.id}"${String(state.categoryId) === String(c.id) ? ' selected' : ''}>${esc(c.name)}</option>`).join('');

  root.querySelector('#btn-search').onclick = () => {
    state.name = root.querySelector('#f-name').value.trim();
    state.categoryId = catSel.value;
    state.status = root.querySelector('#f-status').value;
    state.page = 1;
    load(root);
  };
  root.querySelector('#btn-reset').onclick = () => {
    state.name = ''; state.categoryId = ''; state.status = ''; state.page = 1;
    root.querySelector('#f-name').value = '';
    catSel.value = '';
    root.querySelector('#f-status').value = '';
    load(root);
  };
  root.querySelector('#btn-new').onclick = () => editProduct(null, root);

  delegate(root, '[data-id]', 'click', async (e, el) => {
    const id = el.dataset.id;
    const act = el.dataset.act;
    if (act === 'edit') return editProduct(id, root);
    if (act === 'toggle') {
      const status = el.dataset.status === '1' ? 0 : 1;
      try {
        await api.productStatus(id, status);
        toast(status === 1 ? '已上架' : '已下架');
        load(root);
      } catch (err) { toast(err.message, 'err'); }
    }
    if (act === 'del') {
      const ok = await confirmDialog({ title: '删除商品', message: '删除后商品将从列表与小程序中消失，确定删除？', okText: '删除', danger: true });
      if (!ok) return;
      try {
        await api.productDelete([id]);
        toast('已删除');
        load(root);
      } catch (err) { toast(err.message, 'err'); }
    }
  });

  await load(root);
}

async function load(root) {
  const rows = root.querySelector('#rows');
  rows.innerHTML = skeletonRows(6, 8);
  try {
    const res = await api.productPage({
      page: state.page,
      pageSize: state.pageSize,
      name: state.name || undefined,
      categoryId: state.categoryId || undefined,
      status: state.status || undefined,
    });
    state.records = res.records || [];
    state.total = res.total || 0;

    rows.innerHTML = state.records.length ? state.records.map((p) => `
      <tr>
        <td>
          <div style="display:flex;align-items:center;gap:10px">
            <img class="thumb" src="${esc(p.image || '')}" alt="" onerror="this.style.visibility='hidden'">
            <div><div class="strong">${esc(p.name)}</div>
              <div style="font-size:12px;color:var(--muted-2)">#${p.id}</div></div>
          </div>
        </td>
        <td>${esc(p.categoryName || '—')}</td>
        <td class="num strong">${fmt.money(p.price)}</td>
        <td class="num${p.stock <= 5 ? ' strong' : ''}" style="${p.stock <= 5 ? 'color:var(--red)' : ''}">${fmt.num(p.stock)}</td>
        <td>${(p.specs || []).length ? `<span class="pill plain green">${(p.specs || []).length} 个</span>` : '<span class="hint">—</span>'}</td>
        <td>${pill(STATUS.shelf, p.status)}</td>
        <td class="num nowrap">${fmt.dt(p.updateTime)}</td>
        <td class="nowrap"><div class="row-actions">
          <button class="link-btn" data-act="edit" data-id="${p.id}">编辑</button>
          <button class="link-btn" data-act="toggle" data-id="${p.id}" data-status="${p.status}">${p.status === 1 ? '下架' : '上架'}</button>
          <button class="link-btn danger" data-act="del" data-id="${p.id}">删除</button>
        </div></td>
      </tr>`).join('') : `<tr><td colspan="8">${emptyState('没有找到商品', '试试新增一个商品')}</td></tr>`;

    const pagerBox = root.querySelector('#pager');
    pagerBox.innerHTML = '';
    pagerBox.append(renderPager({
      total: state.total, page: state.page, pageSize: state.pageSize,
      onChange: (p) => { state.page = p; load(root); },
    }));
  } catch (e) {
    rows.innerHTML = `<tr><td colspan="8">${emptyState('加载失败', e.message)}</td></tr>`;
  }
}

//表单

async function editProduct(id, root) {
  let data = { name: '', categoryId: state.categories[0]?.id || '', price: '', stock: 100, image: '', description: '', status: 1, specs: [] };
  if (id) {
    try {
      data = await api.productGet(id);
    } catch (e) { toast(e.message, 'err'); return; }
  }

  const form = document.createElement('div');
  form.innerHTML = `
    <div class="form-grid">
      <div class="field full">
        <label>商品名称</label>
        <input class="input" id="p-name" value="${esc(data.name)}" placeholder="例如：有机西兰花 300g">
      </div>
      <div class="field">
        <label>分类</label>
        <select class="select" id="p-cat">
          ${state.categories.map((c) => `<option value="${c.id}"${String(data.categoryId) === String(c.id) ? ' selected' : ''}>${esc(c.name)}</option>`).join('')}
        </select>
      </div>
      <div class="field">
        <label>售价（元）</label>
        <input class="input" id="p-price" type="number" step="0.01" value="${data.price ?? ''}" placeholder="9.90">
      </div>
      <div class="field">
        <label>可售库存（件）</label>
        <input class="input" id="p-stock" type="number" value="${data.stock ?? 100}">
      </div>
      <div class="field">
        <label>状态</label>
        <select class="select" id="p-status">
          <option value="1"${data.status === 1 ? ' selected' : ''}>上架在售</option>
          <option value="0"${data.status === 0 ? ' selected' : ''}>下架</option>
        </select>
      </div>
      <div class="field full">
        <label>商品图片</label>
        <div class="img-row">
          <input class="input" id="p-image" value="${esc(data.image || '')}" placeholder="/static/goods/broccoli.png">
          <input type="file" id="p-file" accept="image/*" hidden>
          <button class="btn" id="p-upload" type="button">选择图片</button>
          <img class="img-preview" id="p-preview" src="${esc(data.image || '')}" alt="" onerror="this.style.visibility='hidden'">
        </div>
      </div>
      <div class="field full">
        <label>商品描述</label>
        <textarea class="textarea" id="p-desc" placeholder="产地、储存方式、口感等">${esc(data.description || '')}</textarea>
      </div>
      <div class="field full">
        <label style="display:flex;align-items:center;justify-content:space-between">
          <span>规格（如 500g / 净重）</span>
          <button class="link-btn" id="p-add-spec" type="button">+ 添加规格</button>
        </label>
        <div class="spec-list" id="p-specs"></div>
      </div>
    </div>`;

  const specsBox = form.querySelector('#p-specs');
  const specRow = (s = { name: '', value: '' }) => {
    const row = document.createElement('div');
    row.className = 'spec-row';
    row.innerHTML = `<input class="input" data-k="name" placeholder="规格名，如 重量" value="${esc(s.name || '')}">
      <input class="input" data-k="value" placeholder="规格值，如 500g" value="${esc(s.value || '')}">
      <button class="icon-btn" type="button" data-del>✕</button>`;
    row.querySelector('[data-del]').onclick = () => row.remove();
    return row;
  };
  (data.specs || []).forEach((s) => specsBox.append(specRow(s)));
  form.querySelector('#p-add-spec').onclick = () => specsBox.append(specRow());
  form.querySelector('#p-image').addEventListener('input', (e) => {
    const img = form.querySelector('#p-preview');
    img.src = e.target.value;
    img.style.visibility = e.target.value ? 'visible' : 'hidden';
  });

  const fileInput = form.querySelector('#p-file');
  const uploadBtn = form.querySelector('#p-upload');
  uploadBtn.onclick = () => fileInput.click();
  fileInput.onchange = async () => {
    const file = fileInput.files[0];
    if (!file) return;
    uploadBtn.disabled = true;
    uploadBtn.textContent = '上传中...';
    try {
      const url = await api.upload(file);
      form.querySelector('#p-image').value = url;
      const img = form.querySelector('#p-preview');
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

  const modal = openModal(form, {
    title: id ? '编辑商品' : '新增商品',
    width: 660,
    foot: (m) => [
      h('button', { class: 'btn', onclick: () => m.close() }, '取消'),
      h('button', {
        class: 'btn btn-primary',
        onclick: async (e) => {
          const btn = e.currentTarget;
          const payload = {
            id: id || undefined,
            name: form.querySelector('#p-name').value.trim(),
            categoryId: Number(form.querySelector('#p-cat').value) || undefined,
            price: Number(form.querySelector('#p-price').value),
            stock: Number(form.querySelector('#p-stock').value),
            image: form.querySelector('#p-image').value.trim(),
            description: form.querySelector('#p-desc').value.trim(),
            status: Number(form.querySelector('#p-status').value),
            specs: [...specsBox.querySelectorAll('.spec-row')].map((r, i) => ({
              name: r.querySelector('[data-k="name"]').value.trim(),
              value: r.querySelector('[data-k="value"]').value.trim(),
              sort: i,
            })).filter((s) => s.name || s.value),
          };
          if (!payload.name) return toast('请填写商品名称', 'err');
          if (!(payload.price >= 0)) return toast('请填写正确的价格', 'err');
          btn.disabled = true;
          try {
            if (id) await api.productUpdate(payload);
            else await api.productSave(payload);
            toast(id ? '商品已更新' : '商品已创建');
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
  setTimeout(() => form.querySelector('#p-name').focus(), 40);
}
