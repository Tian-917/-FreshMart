//设置
import { api, store, esc, toast, h } from '../core.js';

export async function render(root) {
  const user = store.user || {};
  const status = await api.storeStatus().catch(() => 1);

  root.innerHTML = `
    <div class="page-head">
      <h1>设置</h1>
      <div class="desc">门店状态、账号安全与系统信息</div>
    </div>

    <div class="settings-grid">
      <section class="card">
        <div class="card-head"><h3>门店营业状态</h3></div>
        <div class="card-body">
          <div class="setting-row">
            <div>
              <div class="strong">${status === 1 ? '营业中' : '已打烊'}</div>
              <p class="hint">打烊后顾客端将无法下单，可随时切回营业中。</p>
            </div>
            <label class="switch">
              <input type="checkbox" id="store-switch" ${status === 1 ? 'checked' : ''}>
              <span></span>
            </label>
          </div>
        </div>
      </section>

      <section class="card">
        <div class="card-head"><h3>我的账号</h3></div>
        <div class="card-body">
          <dl class="kv">
            <dt>登录账号</dt><dd class="num">${esc(user.userName || user.username || '—')}</dd>
            <dt>姓名</dt><dd>${esc(user.name || '—')}</dd>
            <dt>角色</dt><dd>门店管理员</dd>
          </dl>
        </div>
      </section>

      <section class="card span2">
        <div class="card-head"><h3>修改密码</h3></div>
        <div class="card-body">
          <form id="pwd-form" class="form-grid" style="max-width:520px">
            <div class="field full">
              <label>当前密码</label>
              <input class="input" type="password" id="pw-old" autocomplete="current-password" placeholder="请输入当前密码">
            </div>
            <div class="field">
              <label>新密码</label>
              <input class="input" type="password" id="pw-new" autocomplete="new-password" placeholder="至少 6 位">
            </div>
            <div class="field">
              <label>确认新密码</label>
              <input class="input" type="password" id="pw-confirm" autocomplete="new-password" placeholder="再输入一次">
            </div>
            <div class="field full">
              <button class="btn btn-primary" type="submit">保存新密码</button>
            </div>
          </form>
        </div>
      </section>

      <section class="card span2">
        <div class="card-head"><h3>关于</h3></div>
        <div class="card-body">
          <p class="hint" style="line-height:1.9">
            鲜达超市门店运营端 · FreshMart Admin<br>
            前端：原生 ES Module + 自绘 SVG 图表，无构建、无第三方依赖<br>
            接口：Spring Boot 3 + MyBatis + MySQL + Redis，经 nginx <code>/api/</code> 反向代理到 <code>:8080/admin/</code>
          </p>
        </div>
      </section>
    </div>`;

  const sw = root.querySelector('#store-switch');
  sw.addEventListener('change', async () => {
    const next = sw.checked ? 1 : 0;
    try {
      await api.setStoreStatus(next);
      toast(next === 1 ? '门店已开始营业' : '门店已打烊');
      const label = root.querySelector('.setting-row .strong');
      if (label) label.textContent = next === 1 ? '营业中' : '已打烊';
      const badge = document.querySelector('#store-badge');
      if (badge) {
        badge.textContent = next === 1 ? '营业中' : '已打烊';
        badge.classList.toggle('off', next !== 1);
      }
    } catch (e) {
      sw.checked = !sw.checked;
      toast(e.message, 'err');
    }
  });

  const form = root.querySelector('#pwd-form');
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const oldPassword = form.querySelector('#pw-old').value;
    const newPassword = form.querySelector('#pw-new').value;
    const confirm = form.querySelector('#pw-confirm').value;
    if (!oldPassword) return toast('请输入当前密码', 'err');
    if (newPassword.length < 6) return toast('新密码至少 6 位', 'err');
    if (newPassword !== confirm) return toast('两次输入的新密码不一致', 'err');
    try {
      await api.employeePassword({ empId: user.id, oldPassword, newPassword });
      toast('密码已修改，请用新密码重新登录');
      store.clear();
      setTimeout(() => { location.hash = '#/login'; location.reload(); }, 900);
    } catch (err) {
      toast(err.message, 'err');
    }
  });
}
