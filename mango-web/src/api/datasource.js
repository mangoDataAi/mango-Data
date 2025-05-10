import request from '@/utils/request'

// 获取数据源列表
export function getDataSourceList() {
  return request({
    url: '/api/ds/db/list',
    method: 'get'
  })
}

// 分页获取数据源列表
export function getDataSourcePage(params) {
  return request({
    url: '/api/ds/db/page',
    method: 'get',
    params
  })
}

// 测试数据源连接
export function testDataSource(data) {
  return request({
    url: '/api/ds/db/test',
    method: 'post',
    data
  })
}

// 创建数据源
export function createDataSource(data) {
  return request({
    url: '/api/ds/create',
    method: 'post',
    data
  })
}

// 更新数据源
export function updateDataSource(data) {
  return request({
    url: '/api/ds/update',
    method: 'put',
    data
  })
}

// 删除数据源
export function deleteDataSource(id) {
  return request({
    url: `/api/ds/${id}`,
    method: 'delete'
  })
}

// 批量删除数据源
export function batchDeleteDataSource(ids) {
  return request({
    url: '/api/ds/db/batch',
    method: 'delete',
    data: ids
  })
}

// 批量删除数据源
export function batchDeleteDbDs(ids) {
  return request({
    url: '/api/ds/db/batch',
    method: 'delete',
    data: ids
  })
}

// 创建数据源
export function createDs(data) {
  return request({
    url: '/api/ds/create',
    method: 'post',
    data
  })
}

// 更新数据源
export function updateDs(data) {
  return request({
    url: '/api/ds/update',
    method: 'put',
    data
  })
}

// 删除数据源
export function deleteDs(id) {
  return request({
    url: `/api/ds/${id}`,
    method: 'delete'
  })
}

// 分页获取数据源列表
export function getDbDsPage(params) {
  return request({
    url: '/api/ds/db/page',
    method: 'get',
    params
  })
}

// 获取数据源详情
export function getDsDetail(id) {
  return request({
    url: `/api/ds/${id}`,
    method: 'get'
  })
}

// 测试数据源配置
export function testDs(data) {
  return request({
    url: '/api/ds/test',
    method: 'post',
    data
  })
}

// 获取数据库表列表
export function getDbTables(dataSourceId, params) {
  return request({
    url: `/api/ds/${dataSourceId}/tables`,
    method: 'get',
    params: {
      types: params.types,
      searchText: params.searchText
    }
  })
}

// 获取表结构
export function getSourceTableStructure(sourceId, tableName) {
  return request({
    url: `/api/datasource/${sourceId}/table/${tableName}/structure`,
    method: 'get'
  })
}

// 获取表结构(数据库连接池使用)
export function getTableStructure(data) {
  return request({
    url: `/api/ds/table/structure`,
    method: 'post',
    data
  })
}

// 执行SQL查询
export function executeQuery(data) {
  return request({
    url: '/api/ds/execute',
    method: 'post',
    data
  })
}

// 创建接口数据源
export function createApiDs(data) {
  return request({
    url: '/api/ds/api/create',
    method: 'post',
    data
  })
}

// 测试接口数据源
export function testApiDs(data) {
  return request({
    url: '/api/ds/api/test',
    method: 'post',
    data
  })
}

// 获取接口数据源列表
export function getApiDsList() {
  return request({
    url: '/api/ds/api/list',
    method: 'get'
  })
}

// 更新接口数据源
export function updateApiDs(data) {
  return request({
    url: '/api/ds/api/update',
    method: 'put',
    data
  })
}

// 删除接口数据源
export function deleteApiDs(id) {
  return request({
    url: `/api/ds/api/${id}`,
    method: 'delete'
  })
}

// 预览接口数据
export function previewApiData(id) {
  return request({
    url: `/api/ds/api/${id}/preview`,
    method: 'get'
  })
}

// 获取数据库数据源列表
export function getDatabaseSources() {
  return request({
    url: '/api/ds/db/list',
    method: 'get'
  })
}

// 获取数据源列表 (为了兼容现有代码)
export function listDatasource() {
  return request({
    url: '/api/ds/db/list',
    method: 'get'
  })
}

// 测试连接
export function testConnection(data) {
  return request({
    url: '/api/ds/test',
    method: 'post',
    data
  })
}

/**
 * 复制查询结果到新表
 * @param {Object} data 包含源表、目标表和数据的对象
 * @returns {Promise}
 */
export function copyQueryResult(data) {
  return request({
    url: '/api/ds/copy-query',
    method: 'post',
    data
  })
}

/**
 * 删除数据库表
 * @param {number} dataSourceId - 数据源ID
 * @param {string} tableName - 表名
 */
export function dropTable(dataSourceId, tableName) {
  return request({
    url: `/api/ds/${dataSourceId}/table/${tableName}`,
    method: 'delete'
  })
}

/**
 * 批量删除数据库表
 * @param {number} dataSourceId - 数据源ID
 * @param {Array<string>} tableNames - 表名列表
 */
export function dropTables(dataSourceId, tableNames) {
  return request({
    url: `/api/ds/${dataSourceId}/tables/batch`,
    method: 'delete',
    data: { tableNames }
  })
}

// 基础数据源API
export function getDataSources() {
  return request({
    url: '/api/datasource/list',
    method: 'get'
  })
}

export function getDataSourceById(id) {
  return request({
    url: `/api/datasource/${id}`,
    method: 'get'
  })
}

export function testDataSourceConnection(data) {
  return request({
    url: '/api/datasource/test',
    method: 'post',
    data
  })
}

export function createNewDataSource(data) {
  return request({
    url: '/api/datasource/create',
    method: 'post',
    data
  })
}

export function updateDataSourceById(id, data) {
  return request({
    url: `/api/datasource/${id}`,
    method: 'put',
    data
  })
}

export function deleteDataSourceById(id) {
  return request({
    url: `/api/datasource/${id}`,
    method: 'delete'
  })
}

// 图数据库API
export function getGraphSchema(dataSourceId) {
  return request({
    url: `/api/graph/${dataSourceId}/schema`,
    method: 'get'
  })
}

export function executeGraphQuery(dataSourceId, data) {
  return request({
    url: `/api/graph/${dataSourceId}/query`,
    method: 'post',
    data
  })
}

// 消息队列API
export function getTopics(dataSourceId) {
  return request({
    url: `/api/messagequeue/${dataSourceId}/topics`,
    method: 'get'
  })
}

export function getTopicDetails(dataSourceId, topic) {
  return request({
    url: `/api/messagequeue/${dataSourceId}/topics/${topic}`,
    method: 'get'
  })
}

export function sendMessage(dataSourceId, topic, data) {
  return request({
    url: `/api/messagequeue/${dataSourceId}/topics/${topic}/messages`,
    method: 'post',
    data
  })
}

// 时序数据库API
export function getMetrics(dataSourceId) {
  return request({
    url: `/api/timeseries/${dataSourceId}/metrics`,
    method: 'get'
  })
}

export function getTimeSeriesData(dataSourceId, params) {
  return request({
    url: `/api/timeseries/${dataSourceId}/data`,
    method: 'get',
    params
  })
}

// 文件系统API
export function listDirectory(dataSourceId, path) {
  return request({
    url: `/api/filesystem/${dataSourceId}/list`,
    method: 'get',
    params: { path }
  })
}

export function getFileContent(dataSourceId, path) {
  return request({
    url: `/api/filesystem/${dataSourceId}/content`,
    method: 'get',
    params: { path }
  })
}

// NoSQL通用API
export function getCollections(dataSourceId) {
  return request({
    url: `/api/nosql/${dataSourceId}/collections`,
    method: 'get'
  })
}

export function getDocuments(dataSourceId, collection, query = {}, limit = 20, skip = 0) {
  return request({
    url: `/api/nosql/${dataSourceId}/collections/${collection}/documents`,
    method: 'get',
    params: { query, limit, skip }
  })
}

/**
 * 获取数据源统计信息
 * @returns {Promise<Object>} 返回统计信息
 */
export function getDataSourceStatistics() {
  return request({
    url: '/api/ds/statistics',
    method: 'get'
  })
}

// 获取API数据源统计信息
export function getApiSourceStats() {
  return request({
    url: '/api/ds/api/stats',
    method: 'get'
  })
}
