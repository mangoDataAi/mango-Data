import request from '@/utils/request'

// 获取模型列表
export function getModelList(query) {
  return request({
    url: '/api/database/model/list',
    method: 'get',
    params: query
  })
}

// 获取模型树
export function getModelTree() {
  return request({
    url: '/api/database/model/tree',
    method: 'get'
  })
}

// 获取模型详情
export function getModel(id) {
  return request({
    url: `/api/database/model/design/${id}`,
    method: 'get'
  })
}

// 新增模型
export function addModel(data) {
  return request({
    url: '/api/database/model',
    method: 'post',
    data: data
  })
}

// 修改模型
export function updateModel(data) {
  return request({
    url: '/api/database/model',
    method: 'put',
    data: data
  })
}

// 删除模型
export function deleteModel(id) {
  return request({
    url: `/api/database/model/${id}`,
    method: 'delete'
  })
}

// 发布模型
export function publishModel(id) {
  return request({
    url: `/api/database/model/publish/${id}`,
    method: 'post'
  })
}

// 运行模型
export function runModel(id) {
  return request({
    url: `/api/database/model/run/${id}`,
    method: 'post'
  })
}

// 获取模型设计详情
export function getModelDesign(modelId) {
  return request({
    url: `/api/database/model/design/${modelId}`,
    method: 'get'
  })
}

// 保存模型设计
export function saveModelDesign(data) {
  return request({
    url: '/api/database/model/design/save',
    method: 'post',
    data: data
  })
}

// 导入模型
export function importModel(file) {
  const formData = new FormData()
  formData.append('file', file)
  
  return request({
    url: '/api/database/model/import',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 导出模型
export function exportModel(id) {
  return request({
    url: `/api/database/model/export/${id}`,
    method: 'get',
    responseType: 'blob'
  })
} 