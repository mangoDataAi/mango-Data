import { defineStore } from 'pinia'
import { login as loginApi } from '@/api/user'
import request from '@/utils/request'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: null
  }),

  getters: {
    getToken: (state) => state.token,
    getUserInfo: (state) => state.userInfo
  },

  actions: {
    setToken(token) {
      this.token = token
      localStorage.setItem('token', token)
      document.cookie = `TOKEN=${token}`
    },
    setUserInfo(userInfo) {
      this.userInfo = userInfo
    },
    async login(userInfo) {
      try {
        const res = await loginApi(userInfo)
        if (res.code === 0) {  // 登录成功
          // 从返回数据的 body 中获取 token 和 user 信息
          const { token, user } = res.body
          this.setToken(token)
          this.setUserInfo(user)
          return res
        }
        return Promise.reject(res)
      } catch (error) {
        return Promise.reject(error)
      }
    },
    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
      document.cookie = 'TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT'
    },
    async refreshToken() {
      try {
        const res = await request({
          url: '/api/auth/refresh',
          method: 'post',
          data: {
            token: this.token || localStorage.getItem('token')
          }
        })
        if (res.code === 0) {
          this.token = res.data.token
          localStorage.setItem('token', res.data.token)
          return res.data.token
        }
        throw new Error('刷新token失败')
      } catch (error) {
        console.error('刷新token失败:', error)
        throw error
      }
    }
  }
})
