import request from '@/utils/request'

// 基础路径
const baseUrl = '/api/dataasset/collection'

// 执行SQL查询
export function executeQuery(data) {
  return request({
    url: `${baseUrl}/execute`,
    method: 'post',
    data
  })
}


// 获取元数据集合列表（分页）
export function getCollectionList(data) {
  return request({
    url: `${baseUrl}/page`,
    method: 'post',
    data
  })
}

// 获取元数据集合详情
export function getCollectionDetail(id) {
  return request({
    url: `${baseUrl}/${id}`,
    method: 'get'
  })
}

// 创建元数据集合
export function createCollection(data) {
  return request({
    url: baseUrl,
    method: 'post',
    data
  })
}

// 更新元数据集合
export function updateCollection(data) {
  return request({
    url: baseUrl,
    method: 'put',
    data
  })
}

// 删除元数据集合
export function deleteCollection(id) {
  return request({
    url: `${baseUrl}/${id}`,
    method: 'delete'
  })
}

// 获取元数据集合统计信息
export function getCollectionStatistics(id) {
  return request({
    url: `${baseUrl}/${id}/statistics`,
    method: 'get'
  })
}

// 获取元数据集合血缘关系图数据
export function getLineageGraphData(id) {
  return request({
    url: `${baseUrl}/${id}/lineage`,
    method: 'get'
  })
}

// 分析元数据集合血缘关系
export function analyzeLineage(id) {
  return request({
    url: `${baseUrl}/${id}/analyze-lineage`,
    method: 'post'
  })
}

// 导入元数据集合
export function importCollection(data) {
  return request({
    url: `${baseUrl}/import`,
    method: 'post',
    data
  })
}

// 导出元数据集合
export function exportCollection(id) {
  return request({
    url: `${baseUrl}/${id}/export`,
    method: 'get'
  })
}

// 获取所有集合类型
export function getAllCollectionTypes() {
  return request({
    url: `${baseUrl}/types`,
    method: 'get'
  })
}

// 获取所有标签
export function getAllTags() {
  return request({
    url: `${baseUrl}/tags`,
    method: 'get'
  })
}

// 获取采集统计数据
export function getCollectionStats() {
  return request({
    url: `${baseUrl}/stats`,
    method: 'get'
  })
}

// 启动采集任务
export function startCollection(id) {
  return request({
    url: `${baseUrl}/${id}/start`,
    method: 'post'
  })
}

// 停止采集任务
export function stopCollection(id) {
  return request({
    url: `${baseUrl}/${id}/stop`,
    method: 'post'
  })
}

// 批量启动采集任务
export function batchStartCollections(ids) {
  return request({
    url: `${baseUrl}/batch-start`,
    method: 'post',
    data: ids
  })
}

// 批量停止采集任务
export function batchStopCollections(ids) {
  return request({
    url: `${baseUrl}/batch-stop`,
    method: 'post',
    data: ids
  })
}

// 执行采集任务
export function executeCollection(id) {
  return request({
    url: `${baseUrl}/${id}/execute`,
    method: 'post'
  })
}

// 获取采集任务状态
export function getExecutionStatus(executionId) {
  return request({
    url: `${baseUrl}/execution/${executionId}/status`,
    method: 'get'
  })
}

// 获取采集任务日志
export function getExecutionLogs(executionId, offset, limit) {
  return request({
    url: `${baseUrl}/execution/${executionId}/logs`,
    method: 'get',
    params: { offset, limit }
  })
}

// 新增 - 获取采集任务执行状态详情
export function getCollectionExecutionStatus(id) {
  return request({
    url: `${baseUrl}/${id}/execution-status`,
    method: 'get'
  })
}

// 新增 - 获取采集任务执行日志
export function getCollectionExecutionLogs(id, offset, limit) {
  return request({
    url: `${baseUrl}/${id}/execution-logs`,
    method: 'get',
    params: { offset, limit }
  })
}

// 新增 - 获取数据预览
export function getDataPreview(id, tableName, limit) {
  return request({
    url: `${baseUrl}/${id}/data-preview`,
    method: 'get',
    params: { tableName, limit }
  })
}

// 新增 - 获取采集分析结果
export function getCollectionAnalysis(id) {
  return request({
    url: `${baseUrl}/${id}/analysis`,
    method: 'get'
  })
} 