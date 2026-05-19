// 小程序入口，登录与全局状态
const { request } = require('./utils/request')

App({
  globalData: {
    member: null,
    token: ''
  },

  onLaunch() {
    this.login()
  },

  //会员登录，有 token 直接用，没有就 wx.login 换 code 调后端
  login() {
    const cached = wx.getStorageSync('token')
    if (cached) {
      this.globalData.token = cached
      return Promise.resolve(cached)
    }
    return new Promise((resolve, reject) => {
      wx.login({
        success: (res) => {
          request({
            url: '/user/user/login',
            method: 'POST',
            data: { code: res.code || 'dev' },
            noAuth: true
          })
            .then((data) => {
              wx.setStorageSync('token', data.token)
              wx.setStorageSync('member', data)
              this.globalData.token = data.token
              this.globalData.member = data
              resolve(data.token)
            })
            .catch(reject)
        },
        fail: reject
      })
    })
  },

  logout() {
    wx.removeStorageSync('token')
    wx.removeStorageSync('member')
    this.globalData.token = ''
    this.globalData.member = null
  }
})
