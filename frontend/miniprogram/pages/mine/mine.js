// 我的页
const app = getApp()

Page({
  data: {
    member: null,
    stats: { orders: 0, cart: 0 }
  },

  onShow() {
    const member = wx.getStorageSync('member') || app.globalData.member
    this.setData({ member })
  },

  goAddress() {
    wx.navigateTo({ url: '/pages/address/address' })
  },

  goOrders() {
    wx.switchTab({ url: '/pages/orders/orders' })
  },

  goIndex() {
    wx.switchTab({ url: '/pages/index/index' })
  },

  onRelogin() {
    app.logout()
    wx.showLoading({ title: '登录中' })
    app
      .login()
      .then(() => {
        wx.hideLoading()
        wx.showToast({ title: '已重新登录', icon: 'none' })
        this.setData({ member: wx.getStorageSync('member') })
      })
      .catch(() => wx.hideLoading())
  },

  onAbout() {
    wx.showModal({
      title: '关于鲜达超市',
      content: '会员端 Demo：扫码下单、门店拣货、30 分钟达。后端 Spring Boot + MyBatis-Plus + MySQL + Redis。',
      showCancel: false
    })
  }
})
