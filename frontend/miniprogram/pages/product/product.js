// 商品详情页
const { request } = require('../../utils/request')

Page({
  data: {
    product: null,
    specGroups: [],
    selected: {},
    priceText: '0.00',
    stock: 0,
    soldOut: false,
    loading: true
  },

  onLoad(query) {
    this.id = query.id
    this.load()
  },

  async load() {
    try {
      const product = await request({ url: `/user/product/${this.id}` })
      //解析规格可选值
      const specGroups = (product.specs || []).map((s) => {
        let options = []
        try {
          options = JSON.parse(s.value)
        } catch (e) {
          options = String(s.value || '').split(',')
        }
        return { name: s.name, options }
      })
      const selected = {}
      specGroups.forEach((g) => {
        selected[g.name] = g.options[0] || ''
      })
      this.setData({
        product,
        specGroups,
        selected,
        priceText: Number(product.price).toFixed(2),
        stock: product.stock || 0,
        soldOut: (product.stock || 0) <= 0,
        loading: false
      })
      wx.setNavigationBarTitle({ title: product.name })
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  onPickSpec(e) {
    const { group, value } = e.currentTarget.dataset
    this.setData({ [`selected.${group}`]: value })
  },

  async onAdd() {
    const { product, specGroups, selected } = this.data
    if (!product) return
    const specText = specGroups
      .map((g) => selected[g.name])
      .filter(Boolean)
      .join(',')
    try {
      await request({
        url: '/user/cart/add',
        method: 'POST',
        data: { productId: product.id, specText }
      })
      wx.showToast({ title: '已加入购物车', icon: 'none' })
    } catch (e) {}
  },

  goCart() {
    wx.switchTab({ url: '/pages/cart/cart' })
  }
})
