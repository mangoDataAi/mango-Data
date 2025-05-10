import request from '@/utils/request'

/**
 * 获取对象规则列表
 * @param {string} objectId - 对象ID
 * @returns {Promise} - 返回规则列表
 */
export function getObjectRules(objectId) {
  return request({
    url: `/api/dataasset/standard/rule/object/${objectId}`,
    method: 'get'
  })
}

/**
 * 获取规则列表（带筛选条件）
 * @param {Object} params - 筛选参数
 * @param {string} params.objectId - 对象ID
 * @param {string} [params.type] - 规则类型
 * @param {string} [params.subType] - 规则子类型
 * @param {string|number} [params.status] - 规则状态
 * @param {number} [params.page] - 页码
 * @param {number} [params.size] - 每页条数
 * @returns {Promise} - 返回规则列表
 */
export function getRuleList(params) {
  return request({
    url: '/api/dataasset/standard/rule/list',
    method: 'get',
    params
  })
}

/**
 * 添加规则
 * @param {Object} data - 规则数据
 * @returns {Promise} - 返回添加结果
 */
export function addRule(data) {
  return request({
    url: '/api/dataasset/standard/rule',
    method: 'post',
    data
  })
}

/**
 * 更新规则
 * @param {Object} data - 规则数据
 * @returns {Promise} - 返回更新结果
 */
export function updateRule(data) {
  return request({
    url: '/api/dataasset/standard/rule',
    method: 'put',
    data
  })
}

/**
 * 删除规则
 * @param {string} id - 规则ID
 * @returns {Promise} - 返回删除结果
 */
export function deleteRule(id) {
  return request({
    url: `/api/dataasset/standard/rule/${id}`,
    method: 'delete'
  })
}

/**
 * 获取规则详情
 * @param {string} id - 规则ID
 * @returns {Promise} - 返回规则详情
 */
export function getRuleDetail(id) {
  return request({
    url: `/api/dataasset/standard/rule/${id}`,
    method: 'get'
  })
}

/**
 * 启用规则
 * @param {string} id - 规则ID
 * @returns {Promise} - 返回启用结果
 */
export function enableRule(id) {
  return request({
    url: `/api/dataasset/standard/rule/enable/${id}`,
    method: 'put'
  })
}

/**
 * 禁用规则
 * @param {string} id - 规则ID
 * @returns {Promise} - 返回禁用结果
 */
export function disableRule(id) {
  return request({
    url: `/api/dataasset/standard/rule/disable/${id}`,
    method: 'put'
  })
}

/**
 * 批量操作规则
 * @param {string} action - 操作类型：enable, disable, delete
 * @param {Array<string>} ids - 规则ID数组
 * @returns {Promise} - 返回操作结果
 */
export function batchOperateRules(action, ids) {
  return request({
    url: `/api/dataasset/standard/rule/batch/${action}`,
    method: 'post',
    data: { ids }
  })
} 