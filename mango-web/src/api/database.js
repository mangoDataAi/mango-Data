import request from '@/utils/request'

// 获取数据源列表
export function getDataSourceList() {
  return request({
    url: '/api/ds/db/list',
    method: 'get'
  })
}

// 获取指定类型的数据源列表
export function getDataSourceListByTypes(types) {
  return request({
    url: '/api/ds/db/list/byTypes',
    method: 'get',
    params: { types }
  })
}

// 获取数据表列表
export function getTableList(dataSourceId) {
  return request({
    url: `/api/ds/${dataSourceId}/tables`,
    method: 'get'
  })
}

// 获取数据表列表 建模使用
export function getTableListModeling(groupId,params) {
  return request({
    url: `/api/table/list/${groupId}`,
    method: 'get',
    params
  })
}


// 获取表字段信息
export function getTableFields(params) {
  return request({
    url: `/api/ds/${params.datasourceId}/table/${params.tableName}/columns`,
    method: 'get'
  })
}

// 测试数据源连接
export function testConnection(data) {
  return request({
    url: '/database/datasource/test',
    method: 'post',
    data
  })
}

// 保存数据源
export function saveDataSource(data) {
  return request({
    url: '/database/datasource/save',
    method: 'post',
    data
  })
}

// 删除数据源
export function deleteDataSource(id) {
  return request({
    url: `/database/datasource/delete/${id}`,
    method: 'delete'
  })
}

// 获取数据源详情
export function getDataSourceDetail(id) {
  return request({
    url: `/database/datasource/detail/${id}`,
    method: 'get'
  })
}

// 更新数据源
export function updateDataSource(data) {
  return request({
    url: '/database/datasource/update',
    method: 'put',
    data
  })
}

// 执行SQL
export function executeSql(data) {
  return request({
    url: '/api/database/execute',
    method: 'post',
    data
  })
}

// 获取表列表
export function getTableSzList(groupId) {
  return request({
    url: `/api/ds/${groupId}/tables`,
    method: 'get'
  })
}

// 执行SQL创建表
export function createTableBySQL(data) {
  return request({
    url: '/api/ds/db/execute',
    method: 'post',
    data
  })
}

// 引用已有表
export function referenceExistingTables(data) {
  return request({
    url: '/api/ds/db/reference',
    method: 'post',
    data
  })
}

// 解析SQL
export function parseSQL(sql) {
  return request({
    url: '/api/database/parse',
    method: 'post',
    data: { sql }
  })
}

// 格式化SQL
export function formatSQL(sql) {
  return request({
    url: '/api/database/format',
    method: 'post',
    data: { sql }
  })
}

// 执行查询
export function executeQuery(dataSourceId, data) {
  return request({
    url: `/api/ds/${dataSourceId}/query`,
    method: 'post',
    data
  })
}

// 获取数据库表 - 同getTableList，提供别名兼容
export function getDatabaseTables(dataSourceId, params) {
  return request({
    url: `/api/ds/${dataSourceId}/tables`,
    method: 'get',
    params
  })
}

// 获取表结构
export function getTableStructure(dataSourceId, tableName) {
  return request({
    url: `/api/ds/${dataSourceId}/table/${tableName}/structure`,
    method: 'get'
  })
}

// 获取表数据
export function getTableData(dataSourceId, tableName, params) {
  return request({
    url: `/api/ds/${dataSourceId}/table/${tableName}/data`,
    method: 'get',
    params
  })
}
