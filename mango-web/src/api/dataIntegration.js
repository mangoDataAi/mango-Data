import request from '@/utils/request'

/**
 * 创建同步任务
 * @param {Object} data 任务数据
 * @returns {Promise}
 */
export function createSyncTask(data) {
  return request({
    url: '/api/sync/tasks',
    method: 'post',
    data
  })
}

/**
 * 更新同步任务
 * @param {String|Number} id 任务ID
 * @param {Object} data 任务数据
 * @returns {Promise}
 */
export function updateSyncTask(id, data) {
  return request({
    url: `/api/sync/tasks/${id}`,
    method: 'put',
    data
  })
}

/**
 * 获取同步任务详情
 * @param {String|Number} id 任务ID
 * @returns {Promise}
 */
export function getSyncTaskDetail(id) {
  return request({
    url: `/api/sync/tasks/${id}`,
    method: 'get'
  })
}

/**
 * 获取表映射
 * @param {String|Number} taskId 任务ID
 * @returns {Promise}
 */
export function getTableMappings(taskId) {
  return request({
    url: `/api/sync/tasks/${taskId}/mappings`,
    method: 'get'
  })
}

/**
 * 更新表映射
 * @param {String|Number} taskId 任务ID
 * @param {Array} mappings 表映射数据
 * @returns {Promise}
 */
export function updateTableMappings(taskId, mappings) {
  return request({
    url: `/api/sync/tasks/${taskId}/mappings`,
    method: 'put',
    data: mappings
  })
}

/**
 * 获取字段映射
 * @param {String|Number} taskId 任务ID
 * @param {String|Number} mappingId 表映射ID
 * @returns {Promise}
 */
export function getFieldMappings(taskId, mappingId) {
  return request({
    url: `/api/sync/tasks/${taskId}/mappings/${mappingId}/fields`,
    method: 'get'
  })
}

/**
 * 更新字段映射
 * @param {String|Number} taskId 任务ID
 * @param {String|Number} mappingId 表映射ID
 * @param {Array} mappings 字段映射数据
 * @returns {Promise}
 */
export function updateFieldMappings(taskId, mappingId, mappings) {
  return request({
    url: `/api/sync/tasks/${taskId}/mappings/${mappingId}/fields`,
    method: 'put',
    data: mappings
  })
}

/**
 * 更新验证设置
 * @param {String|Number} taskId 任务ID
 * @param {String|Number} mappingId 表映射ID
 * @param {Object} settings 验证设置
 * @returns {Promise}
 */
export function updateValidationSettings(taskId, mappingId, settings) {
  return request({
    url: `/api/sync/tasks/${taskId}/mappings/${mappingId}/validation`,
    method: 'put',
    data: settings
  })
}

/**
 * 更新过滤条件
 * @param {String|Number} taskId 任务ID
 * @param {String|Number} mappingId 表映射ID
 * @param {Object} conditions 过滤条件
 * @returns {Promise}
 */
export function updateFilterCondition(taskId, mappingId, conditions) {
  return request({
    url: `/api/sync/tasks/${taskId}/mappings/${mappingId}/filter`,
    method: 'put',
    data: conditions
  })
}

/**
 * 更新同步模式
 * @param {String|Number} taskId 任务ID
 * @param {Object} mode 同步模式设置
 * @returns {Promise}
 */
export function updateSyncMode(taskId, mode) {
  return request({
    url: `/api/sync/tasks/${taskId}/mode`,
    method: 'put',
    data: mode
  })
}

/**
 * 更新调度设置
 * @param {String|Number} taskId 任务ID
 * @param {Object} settings 调度设置
 * @returns {Promise}
 */
export function updateScheduleSettings(taskId, settings) {
  return request({
    url: `/api/sync/tasks/${taskId}/schedule`,
    method: 'put',
    data: settings
  })
}

/**
 * 更新依赖设置
 * @param {String|Number} taskId 任务ID
 * @param {Object} settings 依赖设置
 * @returns {Promise}
 */
export function updateDependencySettings(taskId, settings) {
  return request({
    url: `/api/sync/tasks/${taskId}/dependencies`,
    method: 'put',
    data: settings
  })
}

/**
 * 更新重试策略
 * @param {String|Number} taskId 任务ID
 * @param {Object} strategy 重试策略
 * @returns {Promise}
 */
export function updateRetryStrategy(taskId, strategy) {
  return request({
    url: `/api/sync/tasks/${taskId}/retry`,
    method: 'put',
    data: strategy
  })
}

/**
 * 更新性能设置
 * @param {String|Number} taskId 任务ID
 * @param {Object} settings 性能设置
 * @returns {Promise}
 */
export function updatePerformanceSettings(taskId, settings) {
  return request({
    url: `/api/sync/tasks/${taskId}/performance`,
    method: 'put',
    data: settings
  })
}

/**
 * 执行同步任务
 * @param {String|Number} taskId 任务ID
 * @returns {Promise}
 */
export function executeSyncTask(taskId) {
  return request({
    url: `/api/sync/tasks/${taskId}/execute`,
    method: 'post'
  })
}

/**
 * 停止同步任务
 * @param {String|Number} taskId 任务ID
 * @returns {Promise}
 */
export function stopSyncTask(taskId) {
  return request({
    url: `/api/sync/tasks/${taskId}/stop`,
    method: 'post'
  })
}

/**
 * 获取任务执行日志
 * @param {String|Number} taskId 任务ID
 * @param {Object} params 查询参数
 * @returns {Promise}
 */
export function getTaskExecutionLogs(taskId, params) {
  return request({
    url: `/api/sync/tasks/${taskId}/logs`,
    method: 'get',
    params
  })
}

/**
 * 从模板创建任务
 * @param {String|Number} templateId 模板ID
 * @param {String} taskName 任务名称
 * @returns {Promise}
 */
export function createTaskFromTemplate(templateId, taskName) {
  return request({
    url: `/api/sync/templates/${templateId}/create`,
    method: 'post',
    data: { name: taskName }
  })
}

/**
 * 保存任务为模板
 * @param {String|Number} taskId 任务ID
 * @param {String} name 模板名称
 * @param {String} description 模板描述
 * @returns {Promise}
 */
export function saveTaskAsTemplate(taskId, name, description) {
  return request({
    url: `/api/sync/tasks/${taskId}/saveAsTemplate`,
    method: 'post',
    data: {
      name,
      description
    }
  })
}

/**
 * 获取任务模板列表
 * @param {Object} params 查询参数
 * @returns {Promise}
 */
export function getSyncTaskTemplates(params) {
  return request({
    url: '/api/sync/templates',
    method: 'get',
    params
  })
}

/**
 * 批量执行任务
 * @param {Array} taskIds 任务ID列表
 * @returns {Promise}
 */
export function batchExecuteTasks(taskIds) {
  return request({
    url: '/api/sync/tasks/batch-execute',
    method: 'post',
    data: { taskIds }
  })
}

/**
 * 批量删除任务
 * @param {Array} taskIds 任务ID列表
 * @returns {Promise}
 */
export function batchDeleteSyncTasks(taskIds) {
  return request({
    url: '/api/sync/tasks/batch-delete',
    method: 'post',
    data: { taskIds }
  })
}

/**
 * 获取任务状态统计
 * @returns {Promise}
 */
export function getSyncTaskStatusStats() {
  return request({
    url: '/api/sync/stats/status',
    method: 'get'
  })
}

/**
 * 获取任务统计信息
 * @returns {Promise}
 */
export function getTaskStats() {
  return request({
    url: '/api/sync/stats',
    method: 'get'
  })
}

// 实时同步任务接口
export function getRealtimeSyncTasks(params) {
  return request({
    url: '/api/v1/data-integration/realtime-sync-tasks',
    method: 'get',
    params
  })
}

export function getTaskDetail(id) {
  return request({
    url: `/api/v1/data-integration/realtime-sync-tasks/${id}`,
    method: 'get'
  })
}

export function createTask(data) {
  return request({
    url: '/api/v1/data-integration/realtime-sync-tasks',
    method: 'post',
    data
  })
}

export function updateTask(id, data) {
  return request({
    url: `/api/v1/data-integration/realtime-sync-tasks/${id}`,
    method: 'put',
    data
  })
}

export function deleteTask(id) {
  return request({
    url: `/api/v1/data-integration/realtime-sync-tasks/${id}`,
    method: 'delete'
  })
}

export function batchDeleteTasks(ids) {
  return request({
    url: '/api/v1/data-integration/realtime-sync-tasks/batch',
    method: 'delete',
    data: ids
  })
}

export function startTask(id, additionalConfig) {
  return request({
    url: `/api/v1/data-integration/realtime-sync-tasks/${id}/start`,
    method: 'post',
    data: additionalConfig || {}
  })
}

export function stopTask(id) {
  return request({
    url: `/api/v1/data-integration/realtime-sync-tasks/${id}/stop`,
    method: 'post'
  })
}

export function resetTask(id) {
  return request({
    url: `/api/v1/data-integration/realtime-sync-tasks/${id}/reset`,
    method: 'post'
  })
}

export function duplicateTask(id, newName) {
  return request({
    url: `/api/v1/data-integration/realtime-sync-tasks/${id}/duplicate`,
    method: 'post',
    params: { newName }
  })
}

export function getTaskStatusStats() {
  return request({
    url: '/api/v1/data-integration/realtime-sync-tasks/status-stats',
    method: 'get'
  })
}

export function getSyncModeStats() {
  return request({
    url: '/api/v1/data-integration/realtime-sync-tasks/sync-mode-stats',
    method: 'get'
  })
}

export function getTaskStatistics() {
  return request({
    url: '/api/v1/data-integration/realtime-sync-tasks/statistics',
    method: 'get'
  })
}

export function getTaskLogs(id, limit) {
  return request({
    url: `/api/v1/data-integration/realtime-sync-tasks/${id}/logs`,
    method: 'get',
    params: { limit }
  })
}

export function getTaskPerformance(id, timeRange, metrics) {
  return request({
    url: `/api/v1/data-integration/realtime-sync-tasks/${id}/performance`,
    method: 'get',
    params: { timeRange, metrics }
  })
}

export function previewFlinkSQL(data) {
  return request({
    url: '/api/v1/data-integration/realtime-sync-tasks/preview-sql',
    method: 'post',
    data
  })
}

export function getAvailableDataSources(params) {
  return request({
    url: '/api/v1/data-integration/realtime-sync-tasks/available-datasources',
    method: 'get',
    params
  })
}

export function getDataSourceCompatibility(sourceType, targetType, useCdc) {
  return request({
    url: '/api/v1/data-integration/realtime-sync-tasks/datasource-compatibility',
    method: 'get',
    params: { sourceType, targetType, useCdc }
  })
}

export function checkNameAvailability(name, excludeId) {
  return request({
    url: '/api/v1/data-integration/realtime-sync-tasks/check-name',
    method: 'get',
    params: { name, excludeId }
  })
} 