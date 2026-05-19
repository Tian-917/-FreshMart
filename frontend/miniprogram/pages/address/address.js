// 收货地址页
const { request } = require('../../utils/request')

Page({
  data: {
    list: [],
    selectMode: false,
    showForm: false,
    form: {
      id: null,
      consignee: '',
      sex: '1',
      phone: '',
      cityName: '',
      districtName: '',
      detail: '',
      label: '家',
      isDefault: 1
    }
  },

  onLoad(query) {
    this.setData({ selectMode: query.select === '1' })
    this.load()
  },

  onShow() {
    if (this.loaded) this.load()
    this.loaded = true
  },

  async load() {
    try {
      const list = await request({ url: '/user/addressBook/list' })
      this.setData({ list: list || [] })
    } catch (e) {}
  },

  onPick(e) {
    if (!this.data.selectMode) return
    const item = this.data.list[e.currentTarget.dataset.index]
    wx.setStorageSync('pickedAddress', item)
    wx.navigateBack()
  },

  openAdd() {
    this.setData({
      showForm: true,
      form: {
        id: null,
        consignee: '',
        sex: '1',
        phone: '',
        cityName: '北京市',
        districtName: '朝阳区',
        detail: '',
        label: '家',
        isDefault: 0
      }
    })
  },

  openEdit(e) {
    const item = this.data.list[e.currentTarget.dataset.index]
    this.setData({ showForm: true, form: { ...item } })
  },

  closeForm() {
    this.setData({ showForm: false })
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: e.detail.value })
  },

  async onSave() {
    const f = this.data.form
    if (!f.consignee || !f.phone || !f.detail) {
      wx.showToast({ title: '收货人、电话、详细地址必填', icon: 'none' })
      return
    }
    const payload = {
      consignee: f.consignee,
      sex: f.sex,
      phone: f.phone,
      provinceName: '北京市',
      cityName: f.cityName || '北京市',
      districtName: f.districtName || '朝阳区',
      detail: f.detail,
      label: f.label || '家',
      isDefault: f.isDefault ? 1 : 0
    }
    try {
      if (f.id) {
        await request({ url: '/user/addressBook', method: 'PUT', data: { ...payload, id: f.id } })
      } else {
        await request({ url: '/user/addressBook', method: 'POST', data: payload })
      }
      wx.showToast({ title: '已保存', icon: 'none' })
      this.setData({ showForm: false })
      this.load()
    } catch (e) {}
  },

  async onDefault(e) {
    const item = this.data.list[e.currentTarget.dataset.index]
    try {
      await request({ url: '/user/addressBook/default', method: 'PUT', data: { id: item.id } })
      this.load()
    } catch (e) {}
  },

  onDelete(e) {
    const item = this.data.list[e.currentTarget.dataset.index]
    wx.showModal({
      title: '删除地址',
      content: '确定删除这条地址吗？',
      success: async (r) => {
        if (!r.confirm) return
        try {
          await request({ url: `/user/addressBook?id=${item.id}`, method: 'DELETE' })
          this.load()
        } catch (e) {}
      }
    })
  }
})
