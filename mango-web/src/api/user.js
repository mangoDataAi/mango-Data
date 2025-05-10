import request from '@/utils/request'

// 登录方法
export function login(data) {
  return request({
    url: '/api/sso/login',
    method: 'post',
    params: {
      username: data.username,
      password: data.password
    }
  })
}

// 获取用户信息
export function getUserInfo() {
  return request({
    url: '/api/sso/info',
    method: 'get'
  })
}

// 退出方法
export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}
