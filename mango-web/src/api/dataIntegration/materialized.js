import request from '@/utils/request'

// 获取物化视图列表
export function getMaterializedViews(params) {
  return request({
    url: '/api/v1/materialized-views',
    method: 'get',
    params
  })
}

// 获取物化视图详情
export function getMaterializedView(id) {
  return request({
    url: `/api/v1/materialized-views/${id}`,
    method: 'get'
  })
}

// 创建物化视图
export function createMaterializedView(data) {
  return request({
    url: '/api/v1/materialized-views',
    method: 'post',
    data
  })
}

// 更新物化视图
export function updateMaterializedView(id, data) {
  return request({
    url: `/api/v1/materialized-views/${id}`,
    method: 'put',
    data
  })
}

// 删除物化视图
export function deleteMaterializedView(id) {
  return request({
    url: `/api/v1/materialized-views/${id}`,
    method: 'delete'
  })
}

// 刷新物化视图
export function refreshMaterializedView(id) {
  return request({
    url: `/api/v1/materialized-views/${id}/refresh`,
    method: 'post'
  })
}

// 批量刷新物化视图
export function batchRefreshMaterializedViews(ids) {
  return request({
    url: '/api/v1/materialized-views/batch-refresh',
    method: 'post',
    data: { ids }
  })
}

// 获取物化视图数据预览
export function previewMaterializedView(id, params) {
  return request({
    url: `/api/v1/materialized-views/${id}/preview`,
    method: 'get',
    params
  })
}

// 导出物化视图数据
export function exportMaterializedView(id, data) {
  return request({
    url: `/api/v1/materialized-views/${id}/export`,
    method: 'post',
    data,
    responseType: 'blob'
  })
}

// 获取物化视图依赖关系
export function getMaterializedViewDependencies(id) {
  return request({
    url: `/api/v1/materialized-views/${id}/dependencies`,
    method: 'get'
  })
}

// 获取物化视图性能指标
export function getMaterializedViewMetrics(id, params) {
  return request({
    url: `/api/v1/materialized-views/${id}/metrics`,
    method: 'get',
    params
  })
}

// 获取物化视图刷新日志
export function getMaterializedViewLogs(id, params) {
  return request({
    url: `/api/v1/materialized-views/${id}/logs`,
    method: 'get',
    params
  })
}

// 获取物化视图列信息
export function getMaterializedViewColumns(id) {
  return request({
    url: `/api/v1/materialized-views/${id}/columns`,
    method: 'get'
  })
}

// 测试物化视图SQL
export function testMaterializedViewSql(data) {
  return request({
    url: '/api/v1/materialized-views/test',
    method: 'post',
    data
  })
}

// 获取物化视图统计信息
export function getMaterializedViewStats() {
  return request({
    url: '/api/v1/materialized-views/stats',
    method: 'get'
  })
} 