// 结算页
const { request } = require('../../utils/request')

function pad(n) {
  return n < 10 ? '0' + n : '' + n
}

function formatTime(date) {
  return (
    date.getFullYear() +
    '-' + pad(date.getMonth() + 1) +
    '-' + pad(date.getDate()) +
    ' ' + pad(date.getHours()) +
    ':' + pad(date.getMinutes()) +
    ':' + pad(date.getSeconds())
  )
}

Page({
  data: {
    carts: [],
    total: '0.00',
    address: null,
    remark: '',
    submitting: false
  },

  onLoad() {
    //幂等令牌，重复提交时后端返回同一笔订单
    this.idempotentToken = 'mp-' + Date.now() + '-' + Math.random().toString(36).slice(2, 10)
    this.loadCart()
    this.loadAddress()
  },

  onShow() {
    const picked = wx.getStorageSync('pickedAddress')
    if (picked && picked.id) {
      this.setData({ address: picked })
      wx.removeStorageSync('pickedAddress')
    }
  },

  async loadCart() {
    try {
      const carts = await request({ url: '/user/cart/list' })
      const list = (carts || []).map((c) => ({
        ...c,
        amountText: Number(c.amount).toFixed(2),
        subtotal: (Number(c.amount) * c.number).toFixed(2)
      }))
      const total = list.reduce((n, c) => n + Number(c.amount) * c.number, 0)
      this.setData({ carts: list, total: total.toFixed(2) })
    } catch (e) {}
  },

  async loadAddress() {
    try {
      const address = await request({ url: '/user/addressBook/default' })
      if (address && address.id) this.setData({ address })
    } catch (e) {}
  },

  chooseAddress() {
    wx.navigateTo({ url: '/pages/address/address?select=1' })
  },

  onRemark(e) {
    this.setData({ remark: e.detail.value })
  },

  async onSubmit() {
    if (this.data.submitting) return
    const { address, carts, total } = this.data
    if (!address) {
      wx.showToast({ title: '请先选择收货地址', icon: 'none' })
      return
    }
    if (!carts.length) {
      wx.showToast({ title: '购物车是空的', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    try {
      const submit = await request({
        url: '/user/order/submit',
        method: 'POST',
        data: {
          addressBookId: address.id,
          payMethod: 1,
          remark: this.data.remark,
          estimatedDeliveryTime: formatTime(new Date(Date.now() + 30 * 60 * 1000)),
          deliveryStatus: 1,
          packAmount: 0,
          tablewareNumber: 0,
          tablewareStatus: 1,
          amount: Number(total),
          idempotentToken: this.idempotentToken
        }
      })
      //模拟支付，直接调后端置为已支付
      await request({
        url: '/user/order/payment',
        method: 'PUT',
        data: { orderNumber: submit.orderNumber, payMethod: 1 }
      })
      wx.showToast({ title: '下单成功', icon: 'success' })
      setTimeout(() => wx.switchTab({ url: '/pages/orders/orders' }), 800)
    } catch (e) {
    } finally {
      this.setData({ submitting: false })
    }
  }
})
