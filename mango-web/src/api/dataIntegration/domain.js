import request from '@/utils/request'

// 获取主题域列表
export function getDomainList(params) {
  return request({
    url: '/api/domain/list',
    method: 'get',
    params
  })
}

// 获取主题域详情
export function getDomainById(id) {
  return request({
    url: `/api/domain/${id}`,
    method: 'get'
  })
}

// 创建主题域
export function createDomain(data) {
  return request({
    url: '/api/domain',
    method: 'post',
    data
  })
}

// 更新主题域
export function updateDomain(id, data) {
  return request({
    url: `/api/domain/${id}`,
    method: 'put',
    data
  })
}

// 删除主题域
export function deleteDomain(id) {
  return request({
    url: `/api/domain/${id}`,
    method: 'delete'
  })
}

// 获取主题域属性
export function getDomainAttributes(id) {
  return request({
    url: `/api/domain/${id}/attributes`,
    method: 'get'
  })
}

// 添加主题域属性
export function addDomainAttribute(id, data) {
  return request({
    url: `/api/domain/${id}/attributes`,
    method: 'post',
    data
  })
}

// 更新主题域属性
export function updateDomainAttribute(attributeId, data) {
  return request({
    url: `/api/domain/attributes/${attributeId}`,
    method: 'put',
    data
  })
}

// 删除主题域属性
export function deleteDomainAttribute(attributeId) {
  return request({
    url: `/api/domain/attributes/${attributeId}`,
    method: 'delete'
  })
}

// 获取主题域关联的标准
export function getDomainStandards(id) {
  return request({
    url: `/api/domain/${id}/standards`,
    method: 'get'
  })
}

// 关联标准
export function linkDomainStandard(id, data) {
  return request({
    url: `/api/domain/${id}/standards`,
    method: 'post',
    data
  })
}

// 解除标准关联
export function unlinkDomainStandard(id, standardId) {
  return request({
    url: `/api/domain/${id}/standards/${standardId}`,
    method: 'delete'
  })
}

// 获取主题域树形结构
export function getDomainTree() {
  return request({
    url: '/api/domain/tree',
    method: 'get'
  })
}

// 获取主题域和维度域的组合树结构
export function getDomainDimensionTree() {
  return request({
    url: '/api/domain/dimension-tree',
    method: 'get'
  })
} 