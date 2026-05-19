// 首页
const { request } = require('../../utils/request')

Page({
  data: {
    storeStatus: 1,
    categories: [],
    activeId: null,
    products: [],
    cartCount: 0,
    cartTotal: '0.00',
    loading: true
  },

  onLoad() {
    this.init()
  },

  onShow() {
    this.loadCartSummary()
  },

  async init() {
    const app = getApp()
    try {
      await app.login()
      await this.loadStoreStatus()
      const categories = await request({ url: '/user/category/list?type=1' })
      if (categories && categories.length) {
        this.setData({ categories, activeId: categories[0].id })
        await this.loadProducts(categories[0].id)
      }
    } catch (e) {
      //错误提示统一在 request 里处理
    } finally {
      this.setData({ loading: false })
    }
  },

  async loadStoreStatus() {
    try {
      const status = await request({ url: '/user/store/status' })
      this.setData({ storeStatus: status === 1 ? 1 : 0 })
    } catch (e) {}
  },

  async loadProducts(categoryId) {
    const products = await request({ url: `/user/product/list?categoryId=${categoryId}` })
    this.setData({
      products: (products || []).map((p) => ({
        ...p,
        priceText: Number(p.price).toFixed(2),
        soldOut: (p.stock || 0) <= 0,
        lowStock: (p.stock || 0) > 0 && (p.stock || 0) <= 10
      }))
    })
  },

  async loadCartSummary() {
    try {
      const carts = await request({ url: '/user/cart/list' })
      const count = (carts || []).reduce((n, c) => n + c.number, 0)
      const total = (carts || []).reduce((n, c) => n + Number(c.amount) * c.number, 0)
      this.setData({ cartCount: count, cartTotal: total.toFixed(2) })
    } catch (e) {}
  },

  onSelectCategory(e) {
    const id = e.currentTarget.dataset.id
    this.setData({ activeId: id })
    this.loadProducts(id)
  },

  goProduct(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/product/product?id=${id}` })
  },

  async onAdd(e) {
    const id = e.currentTarget.dataset.id
    const soldOut = e.currentTarget.dataset.soldout
    if (soldOut) {
      wx.showToast({ title: '该商品已售罄', icon: 'none' })
      return
    }
    try {
      await request({ url: '/user/cart/add', method: 'POST', data: { productId: id } })
      wx.showToast({ title: '已加入购物车', icon: 'none' })
      this.loadCartSummary()
    } catch (e) {}
  },

  goCart() {
    wx.switchTab({ url: '/pages/cart/cart' })
  },

  goCheckout() {
    if (this.data.cartCount === 0) {
      wx.showToast({ title: '购物车是空的', icon: 'none' })
      return
    }
    wx.navigateTo({ url: '/pages/checkout/checkout' })
  },

  onPullDownRefresh() {
    this.init().then(() => wx.stopPullDownRefresh())
  }
})
