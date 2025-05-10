import request from '@/utils/request'

/**
 * 分页查询目录列表
 * @param {Object} params 查询参数
 * @returns {Promise}
 */
export function getCatalogsByPage(params) {
  const safeData = JSON.parse(JSON.stringify(params))
  return request({
    url: '/api/metadata/catalog/page',
    method: 'post',
    data: safeData
  })
}

/**
 * 高级搜索目录
 * @param {Object} params 搜索参数
 * @returns {Promise}
 */
export function searchCatalogs(params) {
  const safeData = JSON.parse(JSON.stringify(params))
  return request({
    url: '/api/metadata/catalog/search/advanced',
    method: 'post',
    data: safeData
  })
}

/**
 * 获取所有目录类型
 * @returns {Promise}
 */
export function getAllCatalogTypes() {
  return request({
    url: '/api/metadata/catalog/types/all',
    method: 'get'
  })
}

/**
 * 获取所有目录标签
 * @returns {Promise}
 */
export function getAllCatalogTags() {
  return request({
    url: '/api/metadata/catalog/tags/all',
    method: 'get'
  })
}

/**
 * 获取所有目录状态
 * @returns {Promise}
 */
export function getAllCatalogStatus() {
  return request({
    url: '/api/metadata/catalog/status/all',
    method: 'get'
  })
}

/**
 * 添加目录
 * @param {Object} data 目录数据
 * @returns {Promise}
 */
export function addCatalog(data) {
  const safeData = JSON.parse(JSON.stringify(data))
  return request({
    url: '/api/metadata/catalog',
    method: 'post',
    data: safeData
  })
}

/**
 * 更新目录
 * @param {Object} data 目录数据
 * @returns {Promise}
 */
export function updateCatalog(data) {
  const safeData = JSON.parse(JSON.stringify(data))
  return request({
    url: '/api/metadata/catalog',
    method: 'put',
    data: safeData
  })
}

/**
 * 删除目录
 * @param {String} id 目录ID
 * @returns {Promise}
 */
export function deleteCatalog(id) {
  return request({
    url: `/api/metadata/catalog/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除目录
 * @param {Array} ids 目录ID数组
 * @returns {Promise}
 */
export function batchDeleteCatalogs(ids) {
  const safeData = JSON.parse(JSON.stringify(ids))
  return request({
    url: '/api/metadata/catalog/batch',
    method: 'delete',
    data: safeData
  })
}

/**
 * 获取目录详情
 * @param {String} id 目录ID
 * @returns {Promise}
 */
export function getCatalogDetail(id) {
  return request({
    url: `/api/metadata/catalog/${id}`,
    method: 'get'
  })
}

/**
 * 获取目录树
 * @returns {Promise}
 */
export function getCatalogTree() {
  return request({
    url: '/api/metadata/catalog/tree',
    method: 'get'
  })
}

/**
 * 搜索目录
 * @param {String} keyword 关键词
 * @returns {Promise}
 */
export function searchCatalog(keyword) {
  return request({
    url: '/api/metadata/catalog/search',
    method: 'get',
    params: { keyword }
  })
}

/**
 * 分页目录
 * @param {Object} data 目录数据
 * @returns {Promise}
 */
export function pageCatalogs(data) {
  const safeData = JSON.parse(JSON.stringify(data))
  return request({
    url: '/api/metadata/catalog/page',
    method: 'post',
    data: safeData
  })
} 