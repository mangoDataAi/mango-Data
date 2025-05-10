import { getTablesByDatabaseId } from './table'
import { getFieldsByTableId } from './field'

/**
 * 根据数据源ID获取表列表
 * @param {Object} params 参数对象，包含datasourceId
 * @returns {Promise} 请求Promise
 */
export function getTables(params) {
  return getTablesByDatabaseId(params.datasourceId)
}

/**
 * 获取表字段列表
 * @param {Object} params 参数对象，包含datasourceId和tableName
 * @returns {Promise} 请求Promise
 */
export function getTableColumns(params) {
  return getFieldsByTableId(params.tableName)
}

// 导出其他原有内容
export { 
  getTablesByDatabaseId, 
  getFieldsByTableId
} 