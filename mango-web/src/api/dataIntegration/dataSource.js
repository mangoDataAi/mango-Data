import request from '@/utils/request'

/**
 * 获取数据源列表
 */
export function getDataSourceList() {
  return request({
    url: '/api/ds/db/list',
    method: 'get'
  })
}

/**
 * 获取所有数据源及其表信息
 * @param {Object} params 请求参数
 * @param {string} params.types 表类型，默认为'table'
 * @param {string} params.searchText 搜索文本（可选）
 */
export function getAllDataSourcesAndTables(params = { types: 'table' }) {
  return request({
    url: '/api/ds/dbs/all-tables',
    method: 'get',
    params
  })
}

/**
 * 获取数据源的表列表
 * @param {string} dataSourceId 数据源ID
 * @param {Object} params 请求参数
 * @param {string} params.types 表类型，默认为'table'
 * @param {string} params.searchText 搜索文本（可选）
 */
export function getTableList(dataSourceId, params = {}) {
  return request({
    url: `/api/ds/${dataSourceId}/tables`,
    method: 'get',
    params
  })
}

/**
 * 获取表字段信息
 * @param {string} dataSourceId 数据源ID
 * @param {string} tableName 表名
 */
export function getTableColumns(dataSourceId, tableName) {
  return request({
    url: `/api/ds/${dataSourceId}/table/${tableName}/columns`,
    method: 'get'
  })
}

/**
 * 测试数据源连接
 * @param {Object} data 数据源配置信息
 */
export function testConnection(data) {
  return request({
    url: '/api/ds/db/test',
    method: 'post',
    data
  })
}

/**
 * 创建数据源
 * @param {Object} data 数据源信息
 */
export function createDataSource(data) {
  return request({
    url: '/api/ds/create',
    method: 'post',
    data
  })
}

/**
 * 更新数据源
 * @param {Object} data 数据源信息
 */
export function updateDataSource(data) {
  return request({
    url: '/api/ds/update',
    method: 'put',
    data
  })
}

/**
 * 删除数据源
 * @param {string} id 数据源ID
 */
export function deleteDataSource(id) {
  return request({
    url: `/api/ds/${id}`,
    method: 'delete'
  })
} 