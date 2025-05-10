import request from '@/utils/request'

// 获取维度列表
export function getDimensionList(params) {
  return request({
    url: '/api/dimension/list',
    method: 'get',
    params
  })
}

// 获取维度详情
export function getDimensionById(id) {
  return request({
    url: `/api/dimension/${id}`,
    method: 'get'
  })
}

// 创建维度
export function createDimension(data) {
  return request({
    url: '/api/dimension',
    method: 'post',
    data
  })
}

// 更新维度
export function updateDimension(id, data) {
  return request({
    url: `/api/dimension/${id}`,
    method: 'put',
    data
  })
}

// 删除维度
export function deleteDimension(id) {
  return request({
    url: `/api/dimension/${id}`,
    method: 'delete'
  })
}

// 获取维度属性列表
export function getDimensionAttributes(id) {
  return request({
    url: `/api/dimension/${id}`,
    method: 'get'
  })
}

// 添加维度属性
export function addDimensionAttribute(id, data) {
  return request({
    url: `/api/dimension/${id}/attributes`,
    method: 'post',
    data
  })
}

// 更新维度属性
export function updateDimensionAttribute(attributeId, data) {
  return request({
    url: `/api/dimension/attributes/${attributeId}`,
    method: 'put',
    data
  })
}

// 删除维度属性
export function deleteDimensionAttribute(attributeId) {
  return request({
    url: `/api/dimension/attributes/${attributeId}`,
    method: 'delete'
  })
}

// 获取维度关联的标准
export function getDimensionStandards(id) {
  return request({
    url: `/api/dimension/${id}`,
    method: 'get'
  })
}

// 关联标准
export function linkDimensionStandard(id, data) {
  return request({
    url: `/api/dimension/${id}/standards`,
    method: 'post',
    data
  })
}

// 解除标准关联
export function unlinkDimensionStandard(id, standardId) {
  return request({
    url: `/api/dimension/${id}/standards/${standardId}`,
    method: 'delete'
  })
}

/**
 * 获取维度树结构
 * @param {Object} params - 查询参数
 * @param {string} [params.domainId] - 主题域ID，可选
 * @returns {Promise} 返回维度树数据
 */
export function getDimensionTree(params) {
  return request({
    url: '/api/dimension/tree',
    method: 'get',
    params
  })
}

// 获取主题域和维度域的组合树结构
export function getDomainDimensionTree() {
  return request({
    url: '/api/domain/dimension-tree',
    method: 'get'
  })
} 