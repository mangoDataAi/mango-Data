import request from '@/utils/request'

/**
 * 根据数据库ID获取表列表
 * @param {string} databaseId 数据库ID
 * @returns {Promise} 请求Promise
 */
export function getTablesByDatabaseId(databaseId) {
  return request({
    url: '/api/metadata/tables',
    method: 'get',
    params: { databaseId }
  })
}

/**
 * 获取表详情
 * @param {string} tableId 表ID
 * @returns {Promise} 请求Promise
 */
export function getTableDetail(tableId) {
  return request({
    url: `/api/metadata/tables/${tableId}`,
    method: 'get'
  })
}

/**
 * 添加表
 * @param {Object} table 表信息
 * @returns {Promise} 请求Promise
 */
export function addTable(table) {
  return request({
    url: '/api/metadata/tables',
    method: 'post',
    data: table
  })
}

/**
 * 更新表
 * @param {Object} table 表信息
 * @returns {Promise} 请求Promise
 */
export function updateTable(table) {
  return request({
    url: '/api/metadata/tables',
    method: 'put',
    data: table
  })
}

/**
 * 删除表
 * @param {string} tableId 表ID
 * @returns {Promise} 请求Promise
 */
export function deleteTable(tableId) {
  return request({
    url: `/api/metadata/tables/${tableId}`,
    method: 'delete'
  })
}

/**
 * 搜索表
 * @param {string} keyword 关键字
 * @param {string} databaseId 数据库ID（可选）
 * @returns {Promise} 请求Promise
 */
export function searchTables(keyword, databaseId) {
  return request({
    url: '/api/metadata/tables/search',
    method: 'get',
    params: { keyword, databaseId }
  })
}

/**
 * 获取表数量
 * @param {string} databaseId 数据库ID
 * @returns {Promise} 请求Promise
 */
export function getTableCount(databaseId) {
  return request({
    url: '/api/metadata/tables/count',
    method: 'get',
    params: { databaseId }
  })
}

/**
 * 同步表元数据
 * @param {string} tableId 表ID
 * @returns {Promise} 请求Promise
 */
export function syncTableMetadata(tableId) {
  return request({
    url: `/api/metadata/tables/${tableId}/sync`,
    method: 'post'
  })
}

/**
 * 分析表统计信息
 * @param {string} tableId 表ID
 * @returns {Promise} 请求Promise
 */
export function analyzeTableStats(tableId) {
  return request({
    url: `/api/metadata/tables/${tableId}/analyze`,
    method: 'post'
  })
}

/**
 * 导出表定义
 * @param {string} tableId 表ID
 * @returns {Promise} 请求Promise
 */
export function exportTableDefinition(tableId) {
  return request({
    url: `/api/metadata/tables/${tableId}/export`,
    method: 'get',
    responseType: 'blob'
  })
} 