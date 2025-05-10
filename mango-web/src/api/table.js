import request from '@/utils/request'

// 获取表列表(分页)
export function getTableList(groupId, params = {}) {
  // 确保参数存在且类型正确
  const queryParams = {
    pageNum: Number(params.pageNum) || 1,
    pageSize: Number(params.pageSize) || 10,
    name: params.name || undefined,
    displayName: params.displayName || undefined,
    createTimeStart: params.createTimeStart || undefined,
    createTimeEnd: params.createTimeEnd || undefined,
    minFieldCount: params.minFieldCount || undefined,
    maxFieldCount: params.maxFieldCount || undefined
  }

  // 移除所有 undefined 的参数
  Object.keys(queryParams).forEach(key => 
    queryParams[key] === undefined && delete queryParams[key]
  )

  return request({
    url: `/api/table/list/${groupId}`,
    method: 'get',
    params: queryParams
  })
}

// 获取表详情
export function getTableDetail(id) {
  return request({
    url: `/api/table/${id}`,
    method: 'get'
  })
}

// 创建表
export function createTable(groupId, data) {
  return request({
    url: `/api/table/${groupId}`,
    method: 'post',
    data
  })
}

// 更新表
export function updateTable(id, data) {
  return request({
    url: `/api/table/${id}`,
    method: 'put',
    data
  })
}

// 删除表
export function deleteTable(id) {
  return request({
    url: `/api/table/${id}`,
    method: 'delete'
  })
}

// 通过 SQL 创建表
export function createTableBySQL(groupId, data) {
  return request({
    url: `/api/table/${groupId}/sql`,
    method: 'post',
    data
  })
}

// 引用已有表
export function referenceExistingTables(groupId, sourceId, tables) {
  return request({
    url: `/api/table/${groupId}/reference/${sourceId}`,
    method: 'post',
    data: tables
  })
}

// 物化表
export function materializeTable(data) {
  return request({
    url: '/api/table/materialize',
    method: 'post',
    data
  })
}

// 下载表模板
export function downloadTableTemplate(tableId, format) {
  return request({
    url: `/api/table/${tableId}/template/${format}`,
    method: 'get',
    responseType: 'blob',
    headers: {
      'Accept': 'application/octet-stream'
    }
  })
}

// 下载空模板
export function downloadEmptyTemplate(format) {
  return request({
    url: `/api/table/template/empty/${format}`,
    method: 'get',
    responseType: 'blob',
    headers: {
      'Accept': 'application/octet-stream'
    }
  })
}
