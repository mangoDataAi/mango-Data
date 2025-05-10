import request from '@/utils/request'

// 分页查询脱敏规则
export function getMaskRulePage(params) {
  return request({
    url: '/api/datasecurity/mask/page',
    method: 'get',
    params
  })
}

// 获取脱敏规则详情
export function getMaskRuleDetail(id) {
  return request({
    url: `/api/datasecurity/mask/${id}`,
    method: 'get'
  })
}

// 新增脱敏规则
export function addMaskRule(data) {
  return request({
    url: '/api/datasecurity/mask/add',
    method: 'post',
    data
  })
}

// 更新脱敏规则
export function updateMaskRule(data) {
  return request({
    url: '/api/datasecurity/mask/update',
    method: 'post',
    data
  })
}

// 删除脱敏规则
export function deleteMaskRule(id) {
  return request({
    url: `/api/datasecurity/mask/${id}`,
    method: 'delete'
  })
}

// 批量删除脱敏规则
export function batchDeleteMaskRule(ids) {
  return request({
    url: '/api/datasecurity/mask/batch-delete',
    method: 'delete',
    data: { ids }
  })
}

// 更新脱敏规则状态
export function updateMaskRuleStatus(data) {
  return request({
    url: '/api/datasecurity/mask/status',
    method: 'put',
    data
  })
}

// 批量更新脱敏规则状态
export function batchUpdateMaskRuleStatus(data) {
  return request({
    url: '/api/datasecurity/mask/batch-status',
    method: 'put',
    data
  })
}

// 预览脱敏规则效果
export function previewMaskRule(id) {
  return request({
    url: '/api/datasecurity/mask/preview',
    method: 'get',
    params: { id }
  })
}

// 测试脱敏规则
export function testMaskRule(data) {
  return request({
    url: '/api/datasecurity/mask/test',
    method: 'post',
    data
  })
}

// 分页查询加密规则
export function getEncryptRulePage(params) {
  return request({
    url: '/api/datasecurity/encrypt/page',
    method: 'get',
    params
  })
}

// 获取加密规则详情
export function getEncryptRuleDetail(id) {
  return request({
    url: `/api/datasecurity/encrypt/${id}`,
    method: 'get'
  })
}

// 新增加密规则
export function addEncryptRule(data) {
  return request({
    url: '/api/datasecurity/encrypt/add',
    method: 'post',
    data
  })
}

// 更新加密规则
export function updateEncryptRule(data) {
  return request({
    url: '/api/datasecurity/encrypt/update',
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
    url: '/api/datasecurity/encrypt/status',
    method: 'put',
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