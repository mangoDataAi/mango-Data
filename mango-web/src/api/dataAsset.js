import request from '@/utils/request'

const baseUrl = '/api/dataasset/catalog'

/**
 * 获取数据资产统计数据
 */
export function getDataAssetStatistics() {
  return request({
    url: `${baseUrl}/statistics`,
    method: 'get'
  })
}

/**
 * 分页查询数据资产
 * @param {Object} params 查询参数
 */
export function pageDataAssets(params) {
  return request({
    url: `${baseUrl}/assets/page`,
    method: 'post',
    data: params
  })
}

/**
 * 获取数据资产详情
 * @param {String} id 资产ID
 */
export function getDataAssetDetail(id) {
  return request({
    url: `${baseUrl}/asset/${id}`,
    method: 'get'
  })
}

/**
 * 创建数据资产
 * @param {Object} data 资产数据
 */
export function createDataAsset(data) {
  return request({
    url: `${baseUrl}/create`,
    method: 'post',
    data
  })
}

/**
 * 更新数据资产
 * @param {Object} data 资产数据
 */
export function updateDataAsset(data) {
  return request({
    url: `${baseUrl}/update`,
    method: 'post',
    data
  })
}

/**
 * 删除数据资产
 * @param {String} id 资产ID
 */
export function deleteDataAsset(id) {
  return request({
    url: `${baseUrl}/asset/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除数据资产
 * @param {Array} ids 资产ID列表
 */
export function batchDeleteDataAssets(ids) {
  return request({
    url: `${baseUrl}/assets/batch`,
    method: 'delete',
    data: ids
  })
}

/**
 * 变更数据资产状态
 * @param {String} id 资产ID
 * @param {String} status 状态
 */
export function changeDataAssetStatus(id, status) {
  return request({
    url: `${baseUrl}/asset/${id}/status/${status}`,
    method: 'put'
  })
}

/**
 * 高级查询数据资产
 * @param {Object} params 查询参数
 */
export function advancedQueryDataAssets(params) {
  return request({
    url: `${baseUrl}/assets/advanced-query`,
    method: 'post',
    data: params
  })
} 