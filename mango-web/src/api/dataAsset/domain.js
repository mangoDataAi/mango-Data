import request from '@/utils/request'

// 获取主题域树
export function getDomainTree() {
  return request({
    url: '/api/database/domain/tree',
    method: 'get'
  })
}

// 获取主题域列表
export function getDomainList(params) {
  return request({
    url: '/api/database/domain/list',
    method: 'get',
    params
  })
}

// 获取主题域详情
export function getDomainDetail(id) {
  return request({
    url: `/api/database/domain/${id}`,
    method: 'get'
  })
}

// 创建主题域
export function createDomain(data) {
  return request({
    url: '/api/database/domain',
    method: 'post',
    data
  })
}

// 更新主题域
export function updateDomain(data) {
  return request({
    url: '/api/database/domain',
    method: 'put',
    data
  })
}

// 删除主题域
export function deleteDomain(id) {
  return request({
    url: `/api/database/domain/${id}`,
    method: 'delete'
  })
} 