// 订单详情页
const { request } = require('../../utils/request')

const STATUS_TEXT = {
  1: '待支付',
  2: '待拣货',
  3: '已接单',
  4: '配送中',
  5: '已完成',
  6: '已取消'
}

Page({
  data: {
    order: null,
    items: []
  },

  onLoad(query) {
    this.id = query.id
    this.load()
  },

  async load() {
    try {
      const order = await request({ url: `/user/order/orderItem/${this.id}` })
      this.setData({
        order: {
          ...order,
          statusText: STATUS_TEXT[order.status] || '未知',
          amountText: Number(order.amount).toFixed(2)
        },
        items: (order.orderItemList || []).map((i) => ({
          ...i,
          amountText: Number(i.amount).toFixed(2)
        }))
      })
    } catch (e) {}
  },

  async onRepeat() {
    try {
      await request({ url: `/user/order/repetition/${this.id}`, method: 'POST' })
      wx.showToast({ title: '已加入购物车', icon: 'none' })
      setTimeout(() => wx.switchTab({ url: '/pages/cart/cart' }), 600)
    } catch (e) {}
  },

  async onReminder() {
    try {
      await request({ url: `/user/order/reminder/${this.id}` })
      wx.showToast({ title: '已催单', icon: 'none' })
    } catch (e) {}
  }
})
