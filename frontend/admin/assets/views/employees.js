//员工管理
import {
  api, fmt, esc, STATUS, pill, toast, delegate, renderPager, emptyState,
  skeletonRows, openModal, confirmDialog, ICONS, h,
} from '../core.js';

const state = { page: 1, pageSize: 10, name: '', total: 0, records: [] };

export async function render(root) {
  root.innerHTML = `
    <div class="page-head">
      <h1>员工管理</h1>
      <div class="desc">门店员工账号，新建账号初始密码为 123456</div>
      <div class="spacer"></div>
      <button class="btn btn-primary" id="btn-new">${ICONS.plus}<span>新增员工</span></button>
    </div>

    <section class="card">
      <div class="card-body" style="padding-bottom:0">
        <div class="toolbar" style="margin-bottom:0;padding-bottom:14px">
          <input class="input search" id="f-name" placeholder="姓名" value="${esc(state.name)}">
          <button class="btn btn-primary" id="btn-search">${ICONS.search}查询</button>
          <button class="btn" id="btn-reset">重置</button>
        </div>
      </div>
      <div class="table-wrap">
        <table class="table">
          <thead><tr><th>姓名</th><th>账号</th><th>手机号</th><th>性别</th><th>状态</th><th>更新时间</th><th>操作</th></tr></thead>
          <tbody id="rows">${skeletonRows(5, 7)}</tbody>
        </table>
      </div>
      <div id="pager"></div>
    </section>`;

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
  root.querySelector('#btn-new').onclick = () => editEmployee(null, root);

  delegate(root, '[data-id]', 'click', async (e, el) => {
    const id = el.dataset.id;
    const act = el.dataset.act;
    if (act === 'edit') return editEmployee(id, root);
    if (act === 'toggle') {
      const status = el.dataset.status === '1' ? 0 : 1;
      const ok = await confirmDialog({
        title: status === 0 ? '禁用账号' : '启用账号',
        message: status === 0 ? '禁用后该员工将无法登录管理端。' : '启用后该员工可以重新登录。',
        okText: status === 0 ? '禁用' : '启用',
        danger: status === 0,
      });
      if (!ok) return;
      try {
        await api.employeeStatus(id, status);
        toast(status === 0 ? '已禁用' : '已启用');
        load(root);
      } catch (err) { toast(err.message, 'err'); }
    }
  });

  await load(root);
}

async function load(root) {
  const rows = root.querySelector('#rows');
  rows.innerHTML = skeletonRows(5, 7);
  try {
    const res = await api.employeePage({ page: state.page, pageSize: state.pageSize, name: state.name || undefined });
    state.records = res.records || [];
    state.total = res.total || 0;

    rows.innerHTML = state.records.length ? state.records.map((e) => `
      <tr>
        <td class="strong">${esc(e.name)}</td>
        <td class="num">${esc(e.username)}</td>
        <td class="num">${esc(e.phone || '—')}</td>
        <td>${e.sex === '1' ? '男' : e.sex === '0' ? '女' : '—'}</td>
        <td>${pill(STATUS.employee, e.status)}</td>
        <td class="num nowrap">${fmt.dt(e.updateTime)}</td>
        <td class="nowrap"><div class="row-actions">
          <button class="link-btn" data-act="edit" data-id="${e.id}">编辑</button>
          <button class="link-btn ${e.status === 1 ? 'danger' : ''}" data-act="toggle" data-id="${e.id}" data-status="${e.status}">${e.status === 1 ? '禁用' : '启用'}</button>
        </div></td>
      </tr>`).join('') : `<tr><td colspan="7">${emptyState('没有找到员工', '新增一个账号试试')}</td></tr>`;

    const pagerBox = root.querySelector('#pager');
    pagerBox.innerHTML = '';
    pagerBox.append(renderPager({
      total: state.total, page: state.page, pageSize: state.pageSize,
      onChange: (p) => { state.page = p; load(root); },
    }));
  } catch (e) {
    rows.innerHTML = `<tr><td colspan="7">${emptyState('加载失败', e.message)}</td></tr>`;
  }
}

async function editEmployee(id, root) {
  let data = { username: '', name: '', phone: '', sex: '1', idNumber: '' };
  if (id) {
    try { data = await api.employeeGet(id); } catch (e) { toast(e.message, 'err'); return; }
  }

  const form = document.createElement('div');
  form.innerHTML = `
    <div class="form-grid">
      <div class="field">
        <label>账号${id ? '（不可修改）' : ''}</label>
        <input class="input" id="e-username" value="${esc(data.username)}" ${id ? 'disabled' : ''} placeholder="登录用账号，如 zhangsan">
      </div>
      <div class="field">
        <label>姓名</label>
        <input class="input" id="e-name" value="${esc(data.name)}" placeholder="张三">
      </div>
      <div class="field">
        <label>手机号</label>
        <input class="input" id="e-phone" value="${esc(data.phone || '')}" placeholder="13800000000">
      </div>
      <div class="field">
        <label>性别</label>
        <select class="select" id="e-sex">
          <option value="1"${data.sex === '1' ? ' selected' : ''}>男</option>
          <option value="0"${data.sex === '0' ? ' selected' : ''}>女</option>
        </select>
      </div>
      <div class="field full">
        <label>身份证号</label>
        <input class="input" id="e-idNumber" value="${esc(data.idNumber || '')}" placeholder="110101199001011234">
      </div>
      ${id ? '' : '<p class="hint">新建账号的初始密码为 <b>123456</b>，员工首次登录后可在「设置」里修改。</p>'}
    </div>`;

  openModal(form, {
    title: id ? '编辑员工' : '新增员工',
    width: 560,
    foot: (m) => [
      h('button', { class: 'btn', onclick: () => m.close() }, '取消'),
      h('button', {
        class: 'btn btn-primary',
        onclick: async (e) => {
          const btn = e.currentTarget;
          const payload = {
            id: id || undefined,
            username: form.querySelector('#e-username').value.trim(),
            name: form.querySelector('#e-name').value.trim(),
            phone: form.querySelector('#e-phone').value.trim(),
            sex: form.querySelector('#e-sex').value,
            idNumber: form.querySelector('#e-idNumber').value.trim(),
          };
          if (!payload.username && !id) return toast('请填写账号', 'err');
          if (!payload.name) return toast('请填写姓名', 'err');
          btn.disabled = true;
          try {
            if (id) await api.employeeUpdate(payload);
            else await api.employeeSave(payload);
            toast(id ? '员工信息已更新' : '员工已创建，初始密码 123456');
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
  setTimeout(() => form.querySelector(id ? '#e-name' : '#e-username').focus(), 40);
}
