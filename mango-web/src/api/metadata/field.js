import request from '@/utils/request'

/**
 * 根据表ID获取字段列表
 * @param {string} tableId 表ID
 * @returns {Promise} 请求Promise
 */
export function getFieldsByTableId(tableId) {
  return request({
    url: '/api/metadata/fields',
    method: 'get',
    params: { tableId }
  })
}

/**
 * 获取字段详情
 * @param {string} fieldId 字段ID
 * @returns {Promise} 请求Promise
 */
export function getFieldDetail(fieldId) {
  return request({
    url: `/api/metadata/fields/${fieldId}`,
    method: 'get'
  })
}

/**
 * 添加字段
 * @param {Object} field 字段信息
 * @returns {Promise} 请求Promise
 */
export function addField(field) {
  return request({
    url: '/api/metadata/fields',
    method: 'post',
    data: field
  })
}

/**
 * 更新字段
 * @param {Object} field 字段信息
 * @returns {Promise} 请求Promise
 */
export function updateField(field) {
  return request({
    url: '/api/metadata/fields',
    method: 'put',
    data: field
  })
}

/**
 * 删除字段
 * @param {string} fieldId 字段ID
 * @returns {Promise} 请求Promise
 */
export function deleteField(fieldId) {
  return request({
    url: `/api/metadata/fields/${fieldId}`,
    method: 'delete'
  })
}

/**
 * 搜索字段
 * @param {string} keyword 关键字
 * @param {string} tableId 表ID（可选）
 * @returns {Promise} 请求Promise
 */
export function searchFields(keyword, tableId) {
  return request({
    url: '/api/metadata/fields/search',
    method: 'get',
    params: { keyword, tableId }
  })
}

/**
 * 获取字段数量
 * @param {string} tableId 表ID
 * @returns {Promise} 请求Promise
 */
export function getFieldCount(tableId) {
  return request({
    url: '/api/metadata/fields/count',
    method: 'get',
    params: { tableId }
  })
}

/**
 * 添加业务映射
 * @param {string} tableId 表ID
 * @param {string} fieldId 字段ID
 * @param {Object} mapping 映射信息
 * @returns {Promise} 请求Promise
 */
export function addBusinessMapping(tableId, fieldId, mapping) {
  return request({
    url: `/api/metadata/fields/${fieldId}/mappings`,
    method: 'post',
    params: { tableId },
    data: mapping
  })
}

/**
 * 获取业务映射列表
 * @param {string} tableId 表ID
 * @param {string} fieldId 字段ID
 * @returns {Promise} 请求Promise
 */
export function getBusinessMappings(tableId, fieldId) {
  return request({
    url: `/api/metadata/fields/${fieldId}/mappings`,
    method: 'get',
    params: { tableId }
  })
}

/**
 * 更新业务映射
 * @param {string} mappingId 映射ID
 * @param {Object} mapping 映射信息
 * @returns {Promise} 请求Promise
 */
export function updateBusinessMapping(mappingId, mapping) {
  return request({
    url: `/api/metadata/mappings/${mappingId}`,
    method: 'put',
    data: mapping
  })
}

/**
 * 删除业务映射
 * @param {string} mappingId 映射ID
 * @returns {Promise} 请求Promise
 */
export function deleteBusinessMapping(mappingId) {
  return request({
    url: `/api/metadata/mappings/${mappingId}`,
    method: 'delete'
  })
} 