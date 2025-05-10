import request from '@/utils/request'

const baseUrl = '/api/dataasset/catalog'

/**
 * 获取分类统计数据
 */
export function getStatistics() {
  return request({
    url: `${baseUrl}/statistics`,
    method: 'get'
  })
}

/**
 * 获取分类树
 */
export function getCategoryTree() {
  return request({
    url: `${baseUrl}/tree`,
    method: 'get'
  })
}

/**
 * 添加分类
 * @param {Object} data 分类数据
 */
export function addCategory(data) {
  return request({
    url: `${baseUrl}/category`,
    method: 'post',
    data
  })
}

/**
 * 更新分类
 * @param {Object} data 分类数据
 */
export function updateCategory(data) {
  return request({
    url: `${baseUrl}/category`,
    method: 'put',
    data
  })
}

/**
 * 删除分类
 * @param {String|Number} id 分类ID
 */
export function deleteCategory(id) {
  return request({
    url: `${baseUrl}/category/${id}`,
    method: 'delete'
  })
}

/**
 * 获取分类详情
 * @param {String|Number} id 分类ID
 */
export function getCategoryDetail(id) {
  return request({
    url: `${baseUrl}/category/${id}`,
    method: 'get'
  })
}

/**
 * 根据分类ID获取编目项
 * @param {String|Number} categoryId 分类ID
 * @param {Object} params 查询参数
 */
export function getItemsByCategory(categoryId, params) {
  return request({
    url: `${baseUrl}/category/${categoryId}/items`,
    method: 'get',
    params
  })
}

/**
 * 移动分类
 * @param {String|Number} id 分类ID
 * @param {String|Number} parentId 父分类ID
 */
export function moveCategory(id, parentId) {
  return request({
    url: `${baseUrl}/category/move`,
    method: 'post',
    data: { id, parentId }
  })
}

/**
 * 获取分类统计
 * @param {String|Number} id 分类ID
 */
export function getCategoryStats(id) {
  return request({
    url: `${baseUrl}/category/${id}/stats`,
    method: 'get'
  })
}

// 更新分类排序
export function updateCategorySort(categoryId, parentId, sort) {
  return request({
    url: `${baseUrl}/category/sort`,
    method: 'put',
    params: { categoryId, parentId, sort }
  })
}

// 分页查询数据项
export function pageItems(data) {
  return request({
    url: `${baseUrl}/items/page`,
    method: 'post',
    data
  })
}

// 添加数据项
export function addItem(data) {
  return request({
    url: `${baseUrl}/item`,
    method: 'post',
    data
  })
}

// 更新数据项
export function updateItem(data) {
  return request({
    url: `${baseUrl}/item`,
    method: 'put',
    data
  })
}

// 删除数据项
export function deleteItem(id) {
  return request({
    url: `${baseUrl}/item/${id}`,
    method: 'delete'
  })
}

// 批量删除数据项
export function batchDeleteItems(ids) {
  return request({
    url: `${baseUrl}/item/batch`,
    method: 'delete',
    data: ids
  })
}

// 获取数据项详情
export function getItemDetail(id) {
  return request({
    url: `${baseUrl}/item/${id}`,
    method: 'get'
  })
}

// 更新数据项标签
export function updateItemTags(data) {
  return request({
    url: `${baseUrl}/item/tags`,
    method: 'put',
    data
  })
}

// 分页查询可关联资产
export function pageAssets(data) {
  return request({
    url: `${baseUrl}/assets/page`,
    method: 'post',
    data
  })
}

// 关联资产到分类
export function linkAssets(data) {
  return request({
    url: `${baseUrl}/assets/link`,
    method: 'post',
    data
  })
} 