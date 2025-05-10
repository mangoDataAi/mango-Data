import request from '@/utils/request'

// 获取主题域列表
export function getDomainList(params) {
  return request({
    url: '/api/domain/list',
    method: 'get',
    params
  })
}

// 获取主题域树
export function getDomainTree() {
  return request({
    url: '/api/domain/tree',
    method: 'get'
  })
}

// 获取主题域树 - 新API
export function getDomainTreeList() {
  return request({
    url: '/api/domain/tree',
    method: 'get'
  })
}

// 获取主题域详情
export function getDomainDetail(id) {
  return request({
    url: `/api/domain/${id}`,
    method: 'get'
  })
}

// 获取模型已应用的标准
export function getAppliedStandards(modelId) {
  return request({
    url: '/api/domain/applied-standards',
    method: 'get',
    params: { modelId }
  })
}

// 获取可应用的标准
export function getAvailableStandards(modelId, domainIds) {
  return request({
    url: '/api/domain/available-standards',
    method: 'get',
    params: { 
      modelId,
      domainIds: Array.isArray(domainIds) ? domainIds.join(',') : domainIds
    }
  })
}

// 应用标准到模型
export function applyModelStandards(data) {
  return request({
    url: '/api/domain/apply-standards',
    method: 'post',
    data
  }).then(response => {
    // 确保返回有效的响应对象
    return response || { code: -1, message: '未收到服务器响应' }
  }).catch(error => {
    console.error('应用标准API错误:', error)
    // 返回一个带有错误信息的对象，而不是抛出异常
    return { code: -1, message: error.message || '应用标准失败' }
  })
}

// 移除主题域的所有标准
export function removeDomainStandards(modelId, domainId) {
  return request({
    url: '/api/domain/remove-domain-standards',
    method: 'post',
    data: { modelId, domainId }
  })
}

// 移除单个标准
export function removeStandard(modelId, standardId) {
  return request({
    url: '/api/domain/remove-standard',
    method: 'post',
    data: { modelId, standardId }
  })
}


// 验证模型已应用的标准
export function validateModelStandards(modelId) {
  console.log('调用 validateModelStandards API，参数:', { modelId });
  return request({
    url: '/api/domain/validate-standards',
    method: 'get',
    params: { modelId }
  }).then(response => {
    // 确保返回有效的响应对象
    return response || { code: -1, message: '未收到服务器响应' };
  }).catch(error => {
    console.error('验证标准API错误:', error);
    // 返回一个带有错误信息的对象，而不是抛出异常
    return { code: -1, message: error.message || '验证标准失败' };
  });
}

// 获取所有标准规则（不与主题域关联）
export function getRuleObjectsWithoutObjectId() {
  console.log('调用 getRuleObjectsWithoutObjectId API，获取所有标准规则');
  return request({
    url: '/api/dataasset/standard/rule/object/all',
    method: 'get'
  });
} 