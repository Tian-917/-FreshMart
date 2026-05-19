// 购物车页
const { request } = require('../../utils/request')

Page({
  data: {
    carts: [],
    total: '0.00',
    count: 0
  },

  onShow() {
    this.load()
  },

  async load() {
    try {
      const carts = await request({ url: '/user/cart/list' })
      const list = (carts || []).map((c) => ({
        ...c,
        amountText: Number(c.amount).toFixed(2),
        subtotal: (Number(c.amount) * c.number).toFixed(2)
      }))
      const total = list.reduce((n, c) => n + Number(c.amount) * c.number, 0)
      const count = list.reduce((n, c) => n + c.number, 0)
      this.setData({ carts: list, total: total.toFixed(2), count })
    } catch (e) {}
  },

  payload(item) {
    const data = {}
    if (item.productId) data.productId = item.productId
    if (item.bundleId) data.bundleId = item.bundleId
    if (item.specText) data.specText = item.specText
    return data
  },

  async onPlus(e) {
    const index = e.currentTarget.dataset.index
    const item = this.data.carts[index]
    try {
      await request({ url: '/user/cart/add', method: 'POST', data: this.payload(item) })
      this.load()
    } catch (e) {}
  },

  async onMinus(e) {
    const index = e.currentTarget.dataset.index
    const item = this.data.carts[index]
    try {
      await request({ url: '/user/cart/sub', method: 'POST', data: this.payload(item) })
      this.load()
    } catch (e) {}
  },

  onClean() {
    wx.showModal({
      title: '清空购物车',
      content: '确定要清空吗？',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await request({ url: '/user/cart/clean', method: 'DELETE' })
          this.load()
        } catch (e) {}
      }
    })
  },

  goCheckout() {
    if (!this.data.carts.length) {
      wx.showToast({ title: '购物车是空的', icon: 'none' })
      return
    }
    wx.navigateTo({ url: '/pages/checkout/checkout' })
  },

  goIndex() {
    wx.switchTab({ url: '/pages/index/index' })
  }
})
