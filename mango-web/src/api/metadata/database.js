import request from '@/utils/request'

// 根据目录ID获取数据库列表
export function getDatabasesByCatalogId(catalogId) {
  return request({
    url: `/api/metadata/catalog/${catalogId}/databases`,
    method: 'get'
  })
}

// 获取数据库详情
export function getDatabaseDetail(databaseId) {
  return request({
    url: `/api/metadata/database/${databaseId}`,
    method: 'get'
  })
}

// 添加数据库
export function addDatabase(data) {
  return request({
    url: '/api/metadata/database',
    method: 'post',
    data
  })
}

// 更新数据库
export function updateDatabase(data) {
  return request({
    url: '/api/metadata/database',
    method: 'put',
    data
  })
}

// 删除数据库
export function deleteDatabase(databaseId) {
  return request({
    url: `/api/metadata/database/${databaseId}`,
    method: 'delete'
  })
}

// 搜索数据库
export function searchDatabases(keyword) {
  return request({
    url: '/api/metadata/database/search',
    method: 'get',
    params: { keyword }
  })
}

// 获取数据库数量
export function getDatabaseCount(catalogId) {
  return request({
    url: `/api/metadata/database/count`,
    method: 'get',
    params: { catalogId }
  })
} 