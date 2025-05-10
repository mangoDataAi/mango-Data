import request from '@/utils/request'

// 获取文件数据源列表
export function getFileSourceList(params) {
  return request({
    url: '/api/file-source/list',
    method: 'get',
    params
  })
}

// 获取文件数据源统计信息
export function getFileSourceStats() {
  return request({
    url: '/api/file-source/stats',
    method: 'get'
  })
}

// 获取文件数据源详情
export function getFileSourceDetail(id) {
  return request({
    url: `/api/file-source/${id}`,
    method: 'get'
  })
}

// 上传文件
export function uploadFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/api/file-source/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 删除文件数据源
export function deleteFileSource(id) {
  return request({
    url: `/api/file-source/${id}`,
    method: 'delete'
  })
}

// 批量删除文件数据源
export function deleteBatchFileSource(ids) {
  return request({
    url: '/api/file-source/batch',
    method: 'delete',
    data: ids
  })
}

// 获取文件数据
export function getFileSourceData(id, params) {
  return request({
    url: `/api/file-source/${id}/data`,
    method: 'get',
    params: {
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || 10
    }
  })
}

// 更新文件数据
export function updateFileSourceData(id, data) {
  return request({
    url: `/api/file-source/${id}/data`,
    method: 'put',
    data
  })
}

// 下载模板
export function downloadTemplate(fileType) {
  return request({
    url: '/api/file-source/template',
    method: 'get',
    params: { fileType },
    responseType: 'blob'
  }).then(response => {
    // 获取正确的内容类型
    const contentType = getContentType(fileType)
    
    // 创建blob对象，确保CSV使用正确编码
    const blob = new Blob([response], {
      type: contentType
    })
    
    // 创建下载链接
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    
    // 设置文件名
    link.setAttribute('download', `template.${fileType.toLowerCase()}`)
    document.body.appendChild(link)
    link.click()

    // 清理
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  })
}

// 获取文件的MIME类型
function getContentType(fileType) {
  const type = fileType.toLowerCase()
  if (type === 'csv' || type === 'txt' || type === 'dmp' || type === 'sql') {
    return 'text/plain'
  } else if (type === 'xlsx') {
    return 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  } else if (type === 'json') {
    return 'application/json'
  } else if (type === 'xml') {
    return 'application/xml'
  } else {
    return 'application/octet-stream'
  }
}

// 获取文件数据
export function getFileData(id, params) {
  return request({
    url: `/api/file-source/${id}/data`,
    method: 'get',
    params
  })
}

// 获取表字段信息
export function getTableFields(id) {
  return request({
    url: `/api/file-source/${id}/fields`,
    method: 'get'
  })
}

// 获取表数据
export function getTableData(id, params) {
  return request({
    url: `/api/file-source/${id}/data`,
    method: 'get',
    params
  })
}
