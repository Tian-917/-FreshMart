//分类管理
import {
  api, fmt, esc, STATUS, pill, toast, delegate, renderPager, emptyState,
  skeletonRows, openModal, confirmDialog, ICONS, h,
} from '../core.js';

const state = { page: 1, pageSize: 10, type: 1, name: '', total: 0, records: [] };

const TYPES = [
  { key: 1, label: '商品分类' },
  { key: 2, label: '组合分类' },
];

export async function render(root) {
  root.innerHTML = `
    <div class="page-head">
      <h1>分类管理</h1>
      <div class="desc">顾客端首页的分类导航，同时决定商品归组</div>
      <div class="spacer"></div>
      <button class="btn btn-primary" id="btn-new">${ICONS.plus}<span>新增分类</span></button>
    </div>

    <section class="card">
      <div class="card-body" style="padding-bottom:0">
        <div class="toolbar" style="margin-bottom:0;padding-bottom:14px">
          <div id="type-tabs" style="display:flex;gap:6px"></div>
          <input class="input search" id="f-name" placeholder="分类名称" value="${esc(state.name)}">
          <button class="btn btn-primary" id="btn-search">${ICONS.search}查询</button>
          <button class="btn" id="btn-reset">重置</button>
        </div>
      </div>
      <div class="table-wrap">
        <table class="table">
          <thead><tr><th>分类名称</th><th>类型</th><th>排序</th><th>状态</th><th>更新时间</th><th>操作</th></tr></thead>
          <tbody id="rows">${skeletonRows(5, 6)}</tbody>
        </table>
      </div>
      <div id="pager"></div>
    </section>`;

  const tabs = root.querySelector('#type-tabs');
  tabs.innerHTML = TYPES.map((t) => `<button class="seg${state.type === t.key ? ' active' : ''}" data-type="${t.key}">${t.label}</button>`).join('');
  tabs.addEventListener('click', (e) => {
    const b = e.target.closest('[data-type]');
    if (!b) return;
    state.type = Number(b.dataset.type);
    state.page = 1;
    render(root);
  });

  root.querySelector('#btn-search').onclick = () => {
    state.name = root.querySelector('#f-name').value.trim();
    state.page = 1;
    load(root);
  };
  root.querySelector('#btn-reset').onclick = () => {
    state.name = ''; state.page = 1;
    root.querySelector('#f-name').value = '';
    load(root);
  };
  root.querySelector('#btn-new').onclick = () => editCategory(null, root);

  delegate(root, '[data-id]', 'click', async (e, el) => {
    const id = el.dataset.id;
    const act = el.dataset.act;
    if (act === 'edit') return editCategory(id, root);
    if (act === 'toggle') {
      const status = el.dataset.status === '1' ? 0 : 1;
      try {
        await api.categoryStatus(id, status);
        toast(status === 1 ? '已启用' : '已停用');
        load(root);
      } catch (err) { toast(err.message, 'err'); }
    }
    if (act === 'del') {
      const ok = await confirmDialog({ title: '删除分类', message: '分类下若还有商品将无法删除，确定继续？', okText: '删除', danger: true });
      if (!ok) return;
      try {
        await api.categoryDelete(id);
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
    const res = await api.categoryPage({
      page: state.page, pageSize: state.pageSize, type: state.type, name: state.name || undefined,
    });
    state.records = res.records || [];
    state.total = res.total || 0;

    rows.innerHTML = state.records.length ? state.records.map((c) => `
      <tr>
        <td class="strong">${esc(c.name)}</td>
        <td>${c.type === 2 ? '<span class="pill plain blue">组合</span>' : '<span class="pill plain green">商品</span>'}</td>
        <td class="num">${fmt.num(c.sort)}</td>
        <td>${pill(STATUS.shelf, c.status)}</td>
        <td class="num nowrap">${fmt.dt(c.updateTime)}</td>
        <td class="nowrap"><div class="row-actions">
          <button class="link-btn" data-act="edit" data-id="${c.id}">编辑</button>
          <button class="link-btn" data-act="toggle" data-id="${c.id}" data-status="${c.status}">${c.status === 1 ? '停用' : '启用'}</button>
          <button class="link-btn danger" data-act="del" data-id="${c.id}">删除</button>
        </div></td>
      </tr>`).join('') : `<tr><td colspan="6">${emptyState('还没有分类', '先建几个分类，商品才有归属')}</td></tr>`;

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

async function editCategory(id, root) {
  const rec = state.records.find((x) => String(x.id) === String(id));
  const data = id && rec ? rec : { name: '', type: state.type, sort: 0 };

  const form = document.createElement('div');
  form.innerHTML = `
    <div class="form-grid">
      <div class="field full">
        <label>分类名称</label>
        <input class="input" id="c-name" value="${esc(data.name)}" placeholder="例如：时令蔬菜">
      </div>
      <div class="field">
        <label>类型</label>
        <select class="select" id="c-type">
          <option value="1"${data.type === 1 ? ' selected' : ''}>商品分类</option>
          <option value="2"${data.type === 2 ? ' selected' : ''}>组合分类</option>
        </select>
      </div>
      <div class="field">
        <label>排序（数字越小越靠前）</label>
        <input class="input" id="c-sort" type="number" value="${data.sort ?? 0}">
      </div>
    </div>`;

  openModal(form, {
    title: id ? '编辑分类' : '新增分类',
    width: 520,
    foot: (m) => [
      h('button', { class: 'btn', onclick: () => m.close() }, '取消'),
      h('button', {
        class: 'btn btn-primary',
        onclick: async (e) => {
          const btn = e.currentTarget;
          const payload = {
            id: id || undefined,
            name: form.querySelector('#c-name').value.trim(),
            type: Number(form.querySelector('#c-type').value),
            sort: Number(form.querySelector('#c-sort').value || 0),
          };
          if (!payload.name) return toast('请填写分类名称', 'err');
          btn.disabled = true;
          try {
            if (id) await api.categoryUpdate(payload);
            else await api.categorySave(payload);
            toast(id ? '分类已更新' : '分类已创建');
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
  setTimeout(() => form.querySelector('#c-name').focus(), 40);
}
