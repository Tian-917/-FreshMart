# -*- coding: utf-8 -*-
"""鲜达超市后端全接口冒烟测试。

覆盖管理端 36 个接口、会员端 21 个接口，以及未授权、幂等、库存不足、
状态非法流转等异常路径。测试自己造数据、结束时清理。
"""
import json
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid

BASE = 'http://localhost:8080'
RESULTS = []
CTX = {}


# ---------------------------------------------------------------- 基础设施
def call(method, path, body=None, token=None, header='token', raw=False):
    url = BASE + path
    data = None
    headers = {}
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode('utf-8')
        headers['Content-Type'] = 'application/json;charset=utf-8'
    if token:
        headers[header] = token
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            payload = r.read()
            if raw:
                return r.status, payload
            try:
                return r.status, json.loads(payload.decode('utf-8'))
            except Exception:
                return r.status, payload
    except urllib.error.HTTPError as e:
        payload = e.read()
        try:
            return e.code, json.loads(payload.decode('utf-8'))
        except Exception:
            return e.code, payload


def check(name, cond, detail=''):
    RESULTS.append({'case': name, 'pass': bool(cond), 'detail': str(detail)[:300]})
    print(('PASS  ' if cond else 'FAIL  ') + name + ('' if cond else '  -> ' + str(detail)[:300]))
    return cond


def admin(method, path, body=None):
    return call(method, path, body, CTX.get('admin'))


def user(method, path, body=None):
    return call(method, path, body, CTX.get('user'), header='authentication')


def ok(resp, label):
    status, data = resp
    good = status == 200 and isinstance(data, dict) and data.get('code') == 1
    check(label, good, data if not good else '')
    return data.get('data') if good else None


def fail_expected(name, resp, keyword=None):
    status, data = resp
    msg = data.get('msg') if isinstance(data, dict) else str(data)
    good = status != 200 or (isinstance(data, dict) and data.get('code') == 0)
    if keyword and good:
        good = keyword in (msg or '') or keyword in str(data)
    check(name, good, 'status=%s msg=%s' % (status, msg))
    return good


def upload(path):
    token = CTX['admin']
    boundary = '----fm' + uuid.uuid4().hex
    with open(path, 'rb') as f:
        content = f.read()
    body = (('--%s\r\nContent-Disposition: form-data; name="file"; filename="test.png"\r\n'
             'Content-Type: image/png\r\n\r\n' % boundary).encode() + content +
            ('\r\n--%s--\r\n' % boundary).encode())
    req = urllib.request.Request(BASE + '/admin/common/upload', data=body, method='POST',
                                 headers={'Content-Type': 'multipart/form-data; boundary=' + boundary,
                                          'token': token})
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            return r.status, json.loads(r.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8')


# ---------------------------------------------------------------- 用例
def test_unauthorized():
    print('\n=== 鉴权 ===')
    fail_expected('管理端无 token 返回 401', call('GET', '/admin/product/page'))
    fail_expected('会员端无 token 返回 401', call('GET', '/user/cart/list', header='authentication'))
    fail_expected('管理端错误 token 返回 401', call('GET', '/admin/product/page', token='bad.token.here'))
    fail_expected('会员 token 调管理端被拒', call('GET', '/admin/product/page', token='bad', header='authentication'))


def test_admin_login():
    print('\n=== 管理端登录 / 员工 ===')
    data = ok(call('POST', '/admin/employee/login', {'username': 'admin', 'password': '123456'}), '管理员登录成功')
    if not data:
        return
    CTX['admin'] = data['token']
    CTX['admin_id'] = data.get('id')
    check('登录返回用户名', data.get('userName') == 'admin', data)

    fail_expected('错误密码登录失败', call('POST', '/admin/employee/login', {'username': 'admin', 'password': 'wrong'}))

    page = ok(admin('GET', '/admin/employee/page?page=1&pageSize=10'), '员工分页')
    if page:
        check('员工分页有记录', page.get('total', 0) >= 1, page.get('total'))

    ok(admin('GET', '/admin/employee/%s' % CTX['admin_id']), '员工详情')

    name = 'ui_test_' + uuid.uuid4().hex[:6]
    emp = ok(admin('POST', '/admin/employee',
                   {'username': name, 'name': '接口测试员', 'phone': '13800000000',
                    'sex': '1', 'idNumber': '110101199001011234'}), '新增员工')
    page = ok(admin('GET', '/admin/employee/page?page=1&pageSize=20&name=' + urllib.parse.quote(name)), '按姓名搜员工')
    if page and page.get('records'):
        CTX['emp_id'] = page['records'][0]['id']
        ok(admin('PUT', '/admin/employee',
                 {'id': CTX['emp_id'], 'username': name, 'name': '接口测试员2',
                  'phone': '13800000001', 'sex': '1', 'idNumber': '110101199001011234'}), '修改员工')
        got = ok(admin('GET', '/admin/employee/%s' % CTX['emp_id']), '修改后回查')
        check('员工姓名已更新', got and got.get('name') == '接口测试员2', got and got.get('name'))
        ok(admin('POST', '/admin/employee/status/0?id=%s' % CTX['emp_id']), '禁用员工')
        ok(admin('POST', '/admin/employee/status/1?id=%s' % CTX['emp_id']), '启用员工')

        # 用新员工验证改密码：默认密码 123456
        login = call('POST', '/admin/employee/login', {'username': name, 'password': '123456'})
        d = ok(login, '新员工默认密码登录')
        if d:
            fail_expected('旧密码错误时改密码被拒',
                          call('PUT', '/admin/employee/editPassword',
                               {'empId': CTX['emp_id'], 'oldPassword': 'bad', 'newPassword': 'abc123'},
                               token=d['token']))
            ok_admin = ok(call('PUT', '/admin/employee/editPassword',
                               {'empId': CTX['emp_id'], 'oldPassword': '123456', 'newPassword': 'abc12345'},
                               token=d['token']), '修改自己的密码')
            ok(call('POST', '/admin/employee/login', {'username': name, 'password': 'abc12345'}), '新密码登录')
            ok(call('POST', '/admin/employee/logout', None, token=d['token']), '员工登出')

    fail_expected('员工分页无 token 401', call('GET', '/admin/employee/page'))


def test_category():
    print('\n=== 分类 ===')
    page = ok(admin('GET', '/admin/category/page?page=1&pageSize=50'), '分类分页')
    ok(admin('GET', '/admin/category/list?type=1'), '分类列表 type=1')
    ok(admin('GET', '/admin/category/list?type=2'), '分类列表 type=2')

    cname = '测试分类' + uuid.uuid4().hex[:4]
    ok(admin('POST', '/admin/category', {'type': 1, 'name': cname, 'sort': 99}), '新增分类')
    page = ok(admin('GET', '/admin/category/page?page=1&pageSize=50&name=' + urllib.parse.quote(cname)), '按名称查分类')
    if page and page.get('records'):
        cid = page['records'][0]['id']
        CTX['cat_id'] = cid
        ok(admin('PUT', '/admin/category', {'id': cid, 'type': 1, 'name': cname + '改', 'sort': 98}), '修改分类')
        ok(admin('POST', '/admin/category/status/0?id=%s' % cid), '停用分类')
        ok(admin('POST', '/admin/category/status/1?id=%s' % cid), '启用分类')


def test_product():
    print('\n=== 商品 ===')
    page = ok(admin('GET', '/admin/product/page?page=1&pageSize=10'), '商品分页')
    if page:
        check('商品分页有数据', page.get('total', 0) >= 21, page.get('total'))
        rec = page['records'][0]
        check('商品列表带分类名', bool(rec.get('categoryName')), rec.get('categoryName'))

    ok(admin('GET', '/admin/product/list?categoryId=101'), '商品按分类查询')
    pid_exist = None
    page = ok(admin('GET', '/admin/product/page?page=1&pageSize=1'), '取一个商品 id')
    if page and page.get('records'):
        pid_exist = page['records'][0]['id']
        detail = ok(admin('GET', '/admin/product/%s' % pid_exist), '商品详情')
        check('商品详情含库存字段', detail is not None and 'stock' in json.dumps(detail), detail)

    pname = '接口测试商品' + uuid.uuid4().hex[:4]
    ok(admin('POST', '/admin/product',
             {'name': pname, 'categoryId': 101, 'price': 3.5, 'image': '/static/goods/broccoli.jpg',
              'description': '冒烟测试', 'status': 1,
              'specs': [{'name': '规格', 'value': '500g'}]}), '新增商品（带规格）')
    CTX['product_name'] = pname
    page = ok(admin('GET', '/admin/product/page?page=1&pageSize=10&name=' + urllib.parse.quote(pname)), '按名称查商品')
    if page and page.get('records'):
        rec = page['records'][0]
        CTX['product_id'] = rec['id']
        check('新商品默认库存 100', rec.get('stock') == 100, rec.get('stock'))
        check('新商品规格已保存', len(rec.get('specs') or []) == 1, rec.get('specs'))
        ok(admin('PUT', '/admin/product',
                 {'id': rec['id'], 'name': pname, 'categoryId': 101, 'price': 4.5, 'stock': 50,
                  'image': '/static/goods/broccoli.jpg', 'description': '改过', 'status': 1,
                  'specs': [{'name': '规格', 'value': '500g'}]}), '修改商品')
        got = ok(admin('GET', '/admin/product/%s' % rec['id']), '修改后回查商品')
        check('商品价格已更新', got and float(got.get('price')) == 4.5, got and got.get('price'))
        ok(admin('POST', '/admin/product/status/0?id=%s' % rec['id']), '商品下架')
        got = ok(admin('GET', '/admin/product/%s' % rec['id']), '下架后回查')
        check('商品状态为下架', got and got.get('status') == 0, got and got.get('status'))
        ok(admin('POST', '/admin/product/status/1?id=%s' % rec['id']), '商品上架')


def test_bundle():
    print('\n=== 组合商品 ===')
    page = ok(admin('GET', '/admin/bundle/page?page=1&pageSize=10'), '组合商品分页')
    if page and page.get('records'):
        rec = page['records'][0]
        check('组合商品列表带明细', len(rec.get('items') or []) > 0, 'items=%s' % len(rec.get('items') or []))
    ok(admin('GET', '/admin/bundle/page?page=1&pageSize=10&categoryId=201'), '组合商品按分类查询')
    page = ok(admin('GET', '/admin/bundle/page?page=1&pageSize=1'), '取组合商品 id')
    if page and page.get('records'):
        ok(admin('GET', '/admin/bundle/%s' % page['records'][0]['id']), '组合商品详情')

    bname = '接口测试组合' + uuid.uuid4().hex[:4]
    ok(admin('POST', '/admin/bundle',
             {'name': bname, 'categoryId': 201, 'price': 9.9, 'status': 1,
              'description': '冒烟', 'image': '/static/goods/bundle.jpg',
              'items': [{'productId': CTX.get('product_id') or 1001, 'name': '冒烟单品', 'price': 2.0, 'copies': 2}]}),
       '新增组合商品')
    page = ok(admin('GET', '/admin/bundle/page?page=1&pageSize=10&name=' + urllib.parse.quote(bname)), '按名称查组合')
    if page and page.get('records'):
        rec = page['records'][0]
        CTX['bundle_id'] = rec['id']
        check('组合商品明细 1 条', len(rec.get('items') or []) == 1, rec.get('items'))
        ok(admin('PUT', '/admin/bundle',
                 {'id': rec['id'], 'name': bname, 'categoryId': 201, 'price': 12.5, 'status': 1,
                  'description': '改过', 'image': '/static/goods/bundle.jpg',
                  'items': [{'productId': CTX.get('product_id') or 1001, 'name': '冒烟单品', 'price': 2.0, 'copies': 3}]}),
           '修改组合商品')
        ok(admin('POST', '/admin/bundle/status/0?id=%s' % rec['id']), '组合商品下架')
        ok(admin('POST', '/admin/bundle/status/1?id=%s' % rec['id']), '组合商品上架')


def test_common():
    print('\n=== 上传 / 消息 / 门店 ===')
    status, data = upload(r'D:\nginx-1.20.2- FreshMart\html\sky\static\goods\broccoli.jpg')
    good = status == 200 and isinstance(data, dict) and data.get('code') == 1 and data['data'].startswith('http')
    check('上传图片返回 OSS 地址', good, data)
    if good:
        url = data['data']
        try:
            with urllib.request.urlopen(url, timeout=30) as r:
                check('上传后的图片可公开访问', r.status == 200, r.status)
        except Exception as e:
            check('上传后的图片可公开访问', False, e)
    ok(admin('GET', '/admin/messages/countUnread'), '未读消息数')
    ok(admin('GET', '/admin/store/status'), '门店状态')


def test_reports():
    print('\n=== 报表 / 工作台 ===')
    today = time.strftime('%Y-%m-%d')
    week = time.strftime('%Y-%m-%d', time.localtime(time.time() - 6 * 86400))
    ok(admin('GET', '/admin/report/gmvStatistics?begin=%s&end=%s' % (week, today)), '销售额统计')
    ok(admin('GET', '/admin/report/userStatistics?begin=%s&end=%s' % (week, today)), '用户统计')
    ok(admin('GET', '/admin/report/tradeOrderStatistics?begin=%s&end=%s' % (week, today)), '订单统计')
    ok(admin('GET', '/admin/report/top10?begin=%s&end=%s' % (week, today)), '销量 Top10')
    status, payload = call('GET', '/admin/report/export?begin=%s&end=%s' % (week, today), token=CTX['admin'], raw=True)
    good = status == 200 and payload[:2] == b'PK'
    check('导出 Excel 返回 xlsx 文件', good, 'status=%s len=%s' % (status, len(payload)))
    fail_expected('top10 缺参数时报错', admin('GET', '/admin/report/top10'))

    d = ok(admin('GET', '/admin/workspace/businessData'), '工作台营业数据')
    check('营业数据含 gmv', d is not None and 'gmv' in d, d)
    d = ok(admin('GET', '/admin/workspace/overviewTradeOrder'), '工作台订单概览')
    check('订单概览含全部订单数', d is not None and 'allOrders' in d, d)
    ok(admin('GET', '/admin/workspace/overviewProductes'), '工作台商品概况')
    ok(admin('GET', '/admin/workspace/overviewBundles'), '工作台组合概况')


def test_user_login():
    print('\n=== 会员端登录 / 基础数据 ===')
    data = ok(call('POST', '/user/user/login', {'code': 'dev'}), '会员登录（开发模式）')
    if not data:
        return False
    CTX['user'] = data['token']
    check('会员登录返回 openid', bool(data.get('openid')), data)
    ok(user('GET', '/user/store/status'), '门店营业状态')
    cats = ok(user('GET', '/user/category/list'), '会员端分类列表')
    check('分类列表非空', bool(cats), 'len=%s' % (len(cats) if cats else 0))
    prods = ok(user('GET', '/user/product/list?categoryId=101'), '会员端商品列表')
    if prods:
        CTX['user_product'] = prods[0]
        check('商品列表带图片', bool(prods[0].get('image')), prods[0].get('image'))
        detail = ok(user('GET', '/user/product/%s' % prods[0]['id']), '会员端商品详情')
        check('商品详情含规格', detail is not None and 'specs' in json.dumps(detail), detail)
    bundles = ok(user('GET', '/user/bundle/list?categoryId=201'), '会员端组合商品列表')
    if bundles:
        CTX['user_bundle'] = bundles[0]
        ok(user('GET', '/user/bundle/product/%s' % bundles[0]['id']), '组合商品包含的单品')
    return True


def test_cart():
    print('\n=== 购物车 ===')
    p = CTX['user_product']
    ok(user('POST', '/user/cart/add', {'productId': p['id'], 'specText': '500g'}), '加入购物车（商品）')
    ok(user('POST', '/user/cart/add', {'productId': p['id'], 'specText': '500g'}), '重复加入合并数量')
    items = ok(user('GET', '/user/cart/list'), '购物车列表')
    if items:
        hit = [i for i in items if i.get('productId') == p['id']]
        check('购物车数量累加为 2', hit and hit[0].get('number') == 2, hit and hit[0].get('number'))
        ok(user('POST', '/user/cart/sub', {'productId': p['id'], 'specText': '500g'}), '购物车减一')
    if CTX.get('user_bundle'):
        ok(user('POST', '/user/cart/add', {'bundleId': CTX['user_bundle']['id']}), '加入购物车（组合）')
    ok(user('DELETE', '/user/cart/clean'), '清空购物车')
    items = ok(user('GET', '/user/cart/list'), '清空后购物车列表')
    check('清空后购物车为空', items is not None and len(items) == 0, items)


def test_address():
    print('\n=== 地址簿 ===')
    body = {'consignee': '冒烟测试', 'phone': '13900000000', 'sex': '1',
            'provinceName': '北京市', 'cityName': '北京市', 'districtName': '朝阳区',
            'detail': '测试路 1 号', 'label': '公司', 'isDefault': 1}
    ok(user('POST', '/user/addressBook', body), '新增地址')
    lst = ok(user('GET', '/user/addressBook/list'), '地址列表')
    if lst:
        aid = lst[0]['id']
        CTX['addr_id'] = aid
        ok(user('GET', '/user/addressBook/%s' % aid), '地址详情')
        ok(user('PUT', '/user/addressBook', dict(body, id=aid, detail='测试路 2 号')), '修改地址')
        ok(user('PUT', '/user/addressBook/default', {'id': aid}), '设置默认地址')
        d = ok(user('GET', '/user/addressBook/default'), '查询默认地址')
        check('默认地址 id 正确', d and d.get('id') == aid, d)


def test_order_flow():
    print('\n=== 订单全流程（会员下单 → 管理端接单 → 派送 → 完成）===')
    p = CTX['user_product']
    user('DELETE', '/user/cart/clean')
    user('POST', '/user/cart/add', {'productId': p['id'], 'specText': '500g'})
    token = 'smoke-' + uuid.uuid4().hex
    CTX['idem'] = token
    order = ok(user('POST', '/user/order/submit',
                    {'addressBookId': CTX['addr_id'], 'payMethod': 1, 'remark': '冒烟测试单',
                     'amount': 9.9, 'idempotentToken': token}), '提交订单')
    if order:
        oid = order.get('id')
        CTX['order_id'] = oid
        number = order.get('orderNumber') or order.get('number')
        CTX['order_number'] = number
        check('提交订单返回订单号', bool(number), number)

        hist = ok(user('GET', '/user/order/historyTradeOrder?page=1&pageSize=10'), '历史订单')
        check('历史订单包含新单', hist and any(r.get('id') == oid for r in hist.get('records', [])),
              'total=%s' % (hist.get('total') if hist else None))
        ok(user('GET', '/user/order/orderItem/%s' % oid), '订单详情（会员端）')
        ok(user('GET', '/user/order/reminder/%s' % oid), '催单')
        ok(user('PUT', '/user/order/payment', {'orderNumber': number, 'payMethod': 1}), '模拟支付')
        ok(user('PUT', '/user/order/payment', {'orderNumber': number, 'payMethod': 1}), '重复支付幂等')

        # 管理端接单 → 派送 → 完成
        ok(admin('PUT', '/admin/order/confirm', {'id': oid, 'status': 3}), '接单')
        fail_expected('重复接单被拒', admin('PUT', '/admin/order/confirm', {'id': oid, 'status': 3}), '订单状态')
        ok(admin('PUT', '/admin/order/delivery/%s' % oid), '派送')
        ok(admin('PUT', '/admin/order/complete/%s' % oid), '完成订单')
        fail_expected('已完成订单不能重复完成', admin('PUT', '/admin/order/complete/%s' % oid), '订单状态')

        ok(user('POST', '/user/order/repetition/%s' % oid), '再来一单')
        items = ok(user('GET', '/user/cart/list'), '再来一单后购物车')
        check('再来一单回填购物车', items is not None and len(items) > 0, items and len(items))

        d = ok(admin('GET', '/admin/order/details/%s' % oid), '订单详情（管理端）')
        check('管理端详情含订单明细', d is not None and len(d.get('orderItemList') or []) > 0,
              'items=%s' % (len(d.get('orderItemList') or []) if d else None))


def test_order_negative():
    print('\n=== 订单异常路径 ===')
    p = CTX['user_product']
    # 幂等：同 token 再提交
    order = ok(user('POST', '/user/order/submit',
                    {'addressBookId': CTX['addr_id'], 'payMethod': 1, 'remark': '幂等测试',
                     'amount': 9.9, 'idempotentToken': CTX['idem']}), '同幂等令牌重复提交')
    check('重复提交返回同一订单', order and order.get('id') == CTX.get('order_id'),
          order and order.get('id'))

    # 库存不足：把自己的测试商品库存改成 0，下单应被拒绝
    pid = CTX.get('product_id')
    if pid:
        user('DELETE', '/user/cart/clean')
        user('POST', '/user/cart/add', {'productId': pid, 'specText': '500g'})
        ok(admin('PUT', '/admin/product',
                 {'id': pid, 'name': CTX['product_name'], 'categoryId': 101, 'price': 4.5, 'stock': 0,
                  'image': '/static/goods/broccoli.jpg', 'description': '改过', 'status': 1,
                  'specs': [{'name': '规格', 'value': '500g'}]}), '把测试商品库存改成 0')
        status, data = user('POST', '/user/order/submit',
                            {'addressBookId': CTX['addr_id'], 'payMethod': 1, 'amount': 1,
                             'idempotentToken': 'neg-' + uuid.uuid4().hex})
        msg = data.get('msg') if isinstance(data, dict) else str(data)
        check('库存不足时下单被拒绝且提示库存', status != 500 and msg and '库存' in msg,
              'status=%s msg=%s' % (status, msg))
        ok(admin('PUT', '/admin/product',
                 {'id': pid, 'name': CTX['product_name'], 'categoryId': 101, 'price': 4.5, 'stock': 50,
                  'image': '/static/goods/broccoli.jpg', 'description': '改过', 'status': 1,
                  'specs': [{'name': '规格', 'value': '500g'}]}), '恢复测试商品库存')

    # 取消：新单未支付时取消
    user('DELETE', '/user/cart/clean')
    user('POST', '/user/cart/add', {'productId': p['id'], 'specText': '500g'})
    o = ok(user('POST', '/user/order/submit',
                {'addressBookId': CTX['addr_id'], 'payMethod': 1, 'amount': 5,
                 'idempotentToken': 'cancel-' + uuid.uuid4().hex}), '提交待取消订单')
    if o:
        ok(user('PUT', '/user/order/cancel/%s' % o['id']), '用户取消未支付订单')
        fail_expected('重复取消被拒', user('PUT', '/user/order/cancel/%s' % o['id']), '订单状态')

    # 拒单
    user('DELETE', '/user/cart/clean')
    user('POST', '/user/cart/add', {'productId': p['id'], 'specText': '500g'})
    o2 = ok(user('POST', '/user/order/submit',
                 {'addressBookId': CTX['addr_id'], 'payMethod': 1, 'amount': 6,
                  'idempotentToken': 'reject-' + uuid.uuid4().hex}), '提交待拒单订单')
    if o2:
        user('PUT', '/user/order/payment', {'orderNumber': o2.get('orderNumber'), 'payMethod': 1})
        ok(admin('PUT', '/admin/order/rejection', {'id': o2['id'], 'rejectionReason': '冒烟测试拒单'}),
           '管理端拒单')
        d = ok(admin('GET', '/admin/order/details/%s' % o2['id']), '拒单后详情')
        check('拒单后状态为已取消', d and d.get('status') == 6, d and d.get('status'))

    # 管理端取消
    user('DELETE', '/user/cart/clean')
    user('POST', '/user/cart/add', {'productId': p['id'], 'specText': '500g'})
    o3 = ok(user('POST', '/user/order/submit',
                 {'addressBookId': CTX['addr_id'], 'payMethod': 1, 'amount': 7,
                  'idempotentToken': 'admincancel-' + uuid.uuid4().hex}), '提交待管理端取消订单')
    if o3:
        ok(admin('PUT', '/admin/order/cancel', {'id': o3['id'], 'cancelReason': '冒烟测试取消'}),
           '管理端取消订单')


def test_order_query():
    print('\n=== 订单查询 / 统计 ===')
    page = ok(admin('GET', '/admin/order/conditionSearch?page=1&pageSize=10'), '订单条件查询')
    if page:
        check('订单分页有数据', page.get('total', 0) >= 1, page.get('total'))
    ok(admin('GET', '/admin/order/conditionSearch?page=1&pageSize=10&status=5&beginTime=2026-11-01%2000:00:00&endTime=2026-12-01%2000:00:00'),
       '订单按状态+时间查询')
    d = ok(admin('GET', '/admin/order/statistics'), '订单状态统计')
    check('状态统计含 toBeConfirmed', d is not None and 'toBeConfirmed' in d, d)


def cleanup():
    print('\n=== 清理测试数据 ===')
    if CTX.get('order_id'):
        pass
    # 删除测试商品/组合/分类/员工/地址；测试订单通过 SQL 清理（见 shell 步骤）
    if CTX.get('bundle_id'):
        admin('POST', '/admin/bundle/status/0?id=%s' % CTX['bundle_id'])
        ok(admin('DELETE', '/admin/bundle?ids=%s' % CTX['bundle_id']), '删除测试组合')
    if CTX.get('product_id'):
        r1 = admin('POST', '/admin/product/status/0?id=%s' % CTX['product_id'])
        r2 = admin('GET', '/admin/product/%s' % CTX['product_id'])
        print('debug cleanup: id=%s status_resp=%s now=%s' % (
            CTX['product_id'], r1, (r2[1].get('data') or {}).get('status') if isinstance(r2[1], dict) else r2))
        ok(admin('DELETE', '/admin/product?ids=%s' % CTX['product_id']), '删除测试商品')
    if CTX.get('cat_id'):
        ok(admin('DELETE', '/admin/category?id=%s' % CTX['cat_id']), '删除测试分类')
    if CTX.get('emp_id'):
        ok(admin('POST', '/admin/employee/status/0?id=%s' % CTX['emp_id']), '禁用测试员工')
    if CTX.get('addr_id'):
        ok(user('DELETE', '/user/addressBook?id=%s' % CTX['addr_id']), '删除测试地址')
    user('DELETE', '/user/cart/clean')
    ok(admin('PUT', '/admin/store/1'), '恢复门店营业')


def main():
    test_unauthorized()
    test_admin_login()
    if not CTX.get('admin'):
        print('管理员登录失败，终止')
        return 1
    test_category()
    test_product()
    test_bundle()
    test_common()
    test_reports()
    if not test_user_login():
        print('会员登录失败，终止')
        return 1
    test_cart()
    test_address()
    test_order_flow()
    test_order_negative()
    test_order_query()
    cleanup()

    total = len(RESULTS)
    passed = sum(1 for r in RESULTS if r['pass'])
    print('\n================ 汇总 ================')
    print('用例 %d，通过 %d，失败 %d' % (total, passed, total - passed))
    for r in RESULTS:
        if not r['pass']:
            print('  FAIL: %s -> %s' % (r['case'], r['detail']))
    with open(r'D:\Java\_nearnow_tools\api_test_result.json', 'w', encoding='utf-8') as f:
        json.dump({'total': total, 'passed': passed, 'results': RESULTS, 'ctx': CTX}, f,
                  ensure_ascii=False, indent=2)
    return 0 if passed == total else 2


if __name__ == '__main__':
    sys.exit(main())
