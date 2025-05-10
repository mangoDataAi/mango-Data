import request from '@/utils/request'

/**
 * 获取时间序列数据库中的所有指标
 * @param {string} datasourceId 数据源ID
 * @returns {Promise} 返回包含指标列表的Promise
 */
export function getMetrics(datasourceId) {
  return request({
    url: `/api/timeseries/${datasourceId}/metrics`,
    method: 'get'
  })
}

/**
 * 获取指定指标的详细信息
 * @param {string} datasourceId 数据源ID
 * @param {string} metricName 指标名称
 * @returns {Promise} 返回包含指标详细信息的Promise
 */
export function getMetricInfo(datasourceId, metricName) {
  return request({
    url: `/api/timeseries/${datasourceId}/metrics/${encodeURIComponent(metricName)}/info`,
    method: 'get'
  })
}

/**
 * 获取时间序列数据
 * @param {string} datasourceId 数据源ID
 * @param {Object} params 查询参数
 * @param {string} params.metric 指标名称
 * @param {number} params.start 开始时间戳（毫秒）
 * @param {number} params.end 结束时间戳（毫秒）
 * @param {string} params.aggregation 聚合函数（avg, max, min, sum, count）
 * @param {string} params.resolution 时间分辨率
 * @param {Object} params.tags 标签过滤条件
 * @returns {Promise} 返回包含时间序列数据的Promise
 */
export function getTimeSeriesData(datasourceId, params) {
  return request({
    url: `/api/timeseries/${datasourceId}/query`,
    method: 'post',
    data: params
  })
}

/**
 * 获取指标的唯一标签值
 * @param {string} datasourceId 数据源ID
 * @param {string} metricName 指标名称
 * @param {string} tagKey 标签键
 * @returns {Promise} 返回包含标签值的Promise
 */
export function getTagValues(datasourceId, metricName, tagKey) {
  return request({
    url: `/api/timeseries/${datasourceId}/metrics/${encodeURIComponent(metricName)}/tags/${encodeURIComponent(tagKey)}`,
    method: 'get'
  })
}

/**
 * 获取指标可用的聚合函数
 * @param {string} datasourceId 数据源ID
 * @param {string} metricName 指标名称
 * @returns {Promise} 返回可用聚合函数的Promise
 */
export function getAvailableAggregations(datasourceId, metricName) {
  return request({
    url: `/api/timeseries/${datasourceId}/metrics/${encodeURIComponent(metricName)}/aggregations`,
    method: 'get'
  })
}

/**
 * 批量获取多个指标的时间序列数据
 * @param {string} datasourceId 数据源ID
 * @param {Object} params 查询参数
 * @param {Array<string>} params.metrics 指标名称数组
 * @param {number} params.start 开始时间戳（毫秒）
 * @param {number} params.end 结束时间戳（毫秒）
 * @param {string} params.aggregation 聚合函数
 * @param {string} params.resolution 时间分辨率
 * @param {Object} params.tags 标签过滤条件
 * @returns {Promise} 返回包含多个指标的时间序列数据的Promise
 */
export function getBatchTimeSeriesData(datasourceId, params) {
  return request({
    url: `/api/timeseries/${datasourceId}/batch-query`,
    method: 'post',
    data: params
  })
}

/**
 * 根据查询表达式获取时间序列数据
 * @param {string} datasourceId 数据源ID
 * @param {Object} params 查询参数
 * @param {string} params.query 查询表达式
 * @param {number} params.start 开始时间戳（毫秒）
 * @param {number} params.end 结束时间戳（毫秒）
 * @param {string} params.resolution 时间分辨率
 * @returns {Promise} 返回查询结果的Promise
 */
export function queryByExpression(datasourceId, params) {
  return request({
    url: `/api/timeseries/${datasourceId}/expression-query`,
    method: 'post',
    data: params
  })
}

/**
 * 获取时间序列数据库的统计信息
 * @param {string} datasourceId 数据源ID
 * @returns {Promise} 返回统计信息的Promise
 */
export function getDatabaseStats(datasourceId) {
  return request({
    url: `/api/timeseries/${datasourceId}/stats`,
    method: 'get'
  })
}

/**
 * 导出时间序列数据
 * @param {string} datasourceId 数据源ID
 * @param {Object} params 导出参数
 * @param {string} params.metric 指标名称
 * @param {number} params.start 开始时间戳（毫秒）
 * @param {number} params.end 结束时间戳（毫秒）
 * @param {string} params.format 导出格式（csv, json）
 * @param {Object} params.tags 标签过滤条件
 * @returns {Promise} 返回导出结果的Promise
 */
export function exportTimeSeriesData(datasourceId, params) {
  return request({
    url: `/api/timeseries/${datasourceId}/export`,
    method: 'post',
    responseType: 'blob',
    data: params
  })
} 