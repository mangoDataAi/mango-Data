import request from '@/utils/request'

// 分页查询加密规则
export function getEncryptRulePage(params) {
  return request({
    url: '/api/datasecurity/encrypt/page',
    method: 'post',
    data: params
  })
}

// 获取加密规则详情
export function getEncryptRuleDetail(id) {
  return request({
    url: `/api/datasecurity/encrypt/${id}`,
    method: 'get'
  })
}

// 保存或更新加密规则
export function addEncryptRule(data) {
  return request({
    url: '/api/datasecurity/encrypt/save',
    method: 'post',
    data
  })
}

// 更新加密规则
export function updateEncryptRule(data) {
  return request({
    url: '/api/datasecurity/encrypt/save',
    method: 'post',
    data
  })
}

// 删除加密规则
export function deleteEncryptRule(id) {
  return request({
    url: `/api/datasecurity/encrypt/${id}`,
    method: 'delete'
  })
}

// 批量删除加密规则
export function batchDeleteEncryptRule(ids) {
  return request({
    url: '/api/datasecurity/encrypt/batch-delete',
    method: 'delete',
    data: { ids }
  })
}

// 更新加密规则状态
export function updateEncryptRuleStatus(data) {
  return request({
    url: '/api/datasecurity/encrypt/updateStatus',
    method: 'post',
    data
  })
}

// 批量更新加密规则状态
export function batchUpdateEncryptRuleStatus(data) {
  return request({
    url: '/api/datasecurity/encrypt/batch-status',
    method: 'put',
    data
  })
}

// 预览加密规则效果
export function previewEncryptRule(id) {
  return request({
    url: '/api/datasecurity/encrypt/preview',
    method: 'get',
    params: { id }
  })
}

// 测试加密规则
export function testEncryptRule(data) {
  return request({
    url: '/api/datasecurity/encrypt/test',
    method: 'post',
    data
  })
} 