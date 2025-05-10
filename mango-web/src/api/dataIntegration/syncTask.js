import request from '@/utils/request'


// 创建同步任务
export function createTask(data) {
  return request({
    url: '/api/v1/data-integration/sync-tasks',
    method: 'post',
    data
  })
}

// 更新同步任务
export function updateTask(id, data) {
  return request({
    url: `/api/v1/data-integration/sync-tasks/${id}`,
    method: 'post',
    data
  })
}


// 删除同步任务
export function deleteTask(id) {
  return request({
    url: `/api/v1/data-integration/sync-tasks/${id}`,
    method: 'delete'
  })
}

// 批量删除同步任务
export function batchDeleteSyncTasks(ids) {
  return request({
    url: '/api/v1/data-integration/sync-tasks/batch',
    method: 'delete',
    data: ids
  })
}

// 获取同步任务详情
export function getTaskDetail(id) {
  return request({
    url: `/api/v1/data-integration/sync-tasks/${id}`,
    method: 'get'
  })
}

// 分页查询同步任务
export function pageTasks(params) {
  return request({
    url: '/api/v1/data-integration/sync-tasks',
    method: 'get',
    params
  })
}

// 执行同步任务
export function executeTask(id) {
  return request({
    url: `/api/v1/data-integration/sync-tasks/${id}/execute`,
    method: 'post'
  })
}

// 批量执行同步任务
export function batchExecuteSyncTasks(ids) {
  return request({
    url: '/api/v1/data-integration/sync-tasks/batch-execute',
    method: 'post',
    data: ids
  })
}

// 停止同步任务
export function stopTask(id) {
  return request({
    url: `/api/v1/data-integration/sync-tasks/${id}/stop`,
    method: 'post'
  })
}

// 获取任务执行日志
export function getTaskExecutionLog(id) {
  return request({
    url: `/api/v1/data-integration/sync-tasks/${id}/logs`,
    method: 'get'
  })
}

// 获取任务状态统计
export function getTaskStatusStats() {
  return request({
    url: '/api/v1/data-integration/sync-tasks/statistics',
    method: 'get'
  })
}

// 获取任务统计数据
export function getTaskStatistics() {
  return request({
    url: '/api/v1/data-integration/sync-tasks/statistics',
    method: 'get'
  })
}

// 获取表映射关系
export function getTableMappings(taskId) {
  return request({
    url: `/api/v1/data-integration/sync-tasks/${taskId}/mappings`,
    method: 'get'
  })
}

// 更新表映射关系
export function updateTableMappings(taskId, data) {
  return request({
    url: `/api/v1/data-integration/sync-tasks/${taskId}/mappings`,
    method: 'put',
    data
  })
}

// 获取字段映射关系
export function getFieldMappings(taskId, mappingId) {
  return request({
    url: `/api/v1/data-integration/sync-tasks/${taskId}/mappings/${mappingId}/fields`,
    method: 'get'
  })
}

// 更新字段映射关系
export function updateFieldMappings(taskId, mappingId, data) {
  return request({
    url: `/api/v1/data-integration/sync-tasks/${taskId}/mappings/${mappingId}/fields`,
    method: 'put',
    data
  })
}

// 更新数据验证设置
export function updateValidationSettings(taskId, mappingId, data) {
  return request({
    url: `/api/v1/data-integration/sync-tasks/${taskId}/mappings/${mappingId}/validation`,
    method: 'put',
    data
  })
}

// 更新数据过滤条件
export function updateFilterCondition(taskId, mappingId, data) {
  return request({
    url: `/api/v1/data-integration/sync-tasks/${taskId}/mappings/${mappingId}/filter`,
    method: 'put',
    data
  })
}

// 根据模板创建任务
export function createTaskFromTemplate(templateId, taskName) {
  return request({
    url: `/api/v1/data-integration/sync-tasks/template/${templateId}`,
    method: 'post',
    params: { taskName }
  })
}

// 保存任务为模板
export function saveTaskAsTemplate(taskId, templateName, description) {
  return request({
    url: `/api/v1/data-integration/sync-tasks/${taskId}/save-as-template`,
    method: 'post',
    params: { templateName, description }
  })
} 