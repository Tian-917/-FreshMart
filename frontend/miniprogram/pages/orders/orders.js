// 订单列表页
const { request } = require('../../utils/request')

const STATUS_TEXT = {
  1: '待支付',
  2: '待拣货',
  3: '已接单',
  4: '配送中',
  5: '已完成',
  6: '已取消'
}

const STATUS_TAG = {
  1: 'tag-orange',
  2: 'tag-green',
  3: 'tag-green',
  4: 'tag-green',
  5: 'tag-gray',
  6: 'tag-gray'
}

Page({
  data: {
    tabs: [
      { label: '全部', value: '' },
      { label: '待支付', value: 1 },
      { label: '待拣货', value: 2 },
      { label: '配送中', value: 4 },
      { label: '已完成', value: 5 },
      { label: '已取消', value: 6 }
    ],
    activeTab: '',
    orders: [],
    loading: true
  },

  onShow() {
    this.load()
  },

  async load() {
    this.setData({ loading: true })
    try {
      const status = this.data.activeTab
      const url = `/user/order/historyTradeOrder?page=1&pageSize=20${status === '' ? '' : '&status=' + status}`
      const page = await request({ url })
      const orders = (page.records || []).map((o) => ({
        ...o,
        statusText: STATUS_TEXT[o.status] || '未知',
        statusTag: STATUS_TAG[o.status] || 'tag-gray',
        amountText: Number(o.amount).toFixed(2),
        items: (o.orderItemList || []).map((i) => ({
          ...i,
          amountText: Number(i.amount).toFixed(2)
        }))
      }))
      this.setData({ orders, loading: false })
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  onTab(e) {
    this.setData({ activeTab: e.currentTarget.dataset.value })
    this.load()
  },

  goDetail(e) {
    wx.navigateTo({ url: `/pages/orderDetail/orderDetail?id=${e.currentTarget.dataset.id}` })
  },

  async onCancel(e) {
    const id = e.currentTarget.dataset.id
    const ok = await new Promise((resolve) =>
      wx.showModal({
        title: '取消订单',
        content: '确定取消这一单吗？',
        success: (r) => resolve(r.confirm)
      })
    )
    if (!ok) return
    try {
      await request({ url: `/user/order/cancel/${id}`, method: 'PUT' })
      wx.showToast({ title: '已取消', icon: 'none' })
      this.load()
    } catch (e) {}
  },

  async onReminder(e) {
    const id = e.currentTarget.dataset.id
    try {
      await request({ url: `/user/order/reminder/${id}` })
      wx.showToast({ title: '已催单，门店会尽快处理', icon: 'none' })
    } catch (e) {}
  },

  async onRepeat(e) {
    const id = e.currentTarget.dataset.id
    try {
      await request({ url: `/user/order/repetition/${id}`, method: 'POST' })
      wx.showToast({ title: '已加入购物车', icon: 'none' })
      setTimeout(() => wx.switchTab({ url: '/pages/cart/cart' }), 600)
    } catch (e) {}
  },

  async onPay(e) {
    const item = this.data.orders[e.currentTarget.dataset.index]
    try {
      await request({
        url: '/user/order/payment',
        method: 'PUT',
        data: { orderNumber: item.number, payMethod: 1 }
      })
      wx.showToast({ title: '支付成功', icon: 'success' })
      this.load()
    } catch (e) {}
  },

  goIndex() {
    wx.switchTab({ url: '/pages/index/index' })
  }
})
