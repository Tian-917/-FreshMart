// 请求封装，自动带 token
const BASE_URL = 'http://localhost:8080'

//统一请求封装，自动带 authentication 头，401 清 token
function request({ url, method = 'GET', data = {}, noAuth = false }) {
  return new Promise((resolve, reject) => {
    const header = { 'content-type': 'application/json' }
    if (!noAuth) {
      header.authentication = wx.getStorageSync('token') || ''
    }
    wx.request({
      url: BASE_URL + url,
      method,
      data,
      header,
      success(res) {
        if (res.statusCode === 401) {
          wx.removeStorageSync('token')
          wx.showToast({ title: '登录已过期，请重新进入', icon: 'none' })
          reject(res)
          return
        }
        const body = res.data
        if (body && body.code === 1) {
          resolve(body.data)
          return
        }
        wx.showToast({ title: (body && body.msg) || '操作失败', icon: 'none' })
        reject(body || res)
      },
      fail(err) {
        wx.showToast({ title: '连不上后端，确认 java -jar 已启动', icon: 'none' })
        reject(err)
      }
    })
  })
}

module.exports = { request, BASE_URL }
