import request from '@/utils/request'

// API接口
export function getApiList(data) {
  return request({
    url: '/api/dataservice/page',
    method: 'post',
    data
  })
}

// 获取HTTP服务列表
export function getHttpServiceList(params) {
  return request({
    url: '/api/ds/api/list',
    method: 'get',
    params
  })
}

export function getApiStatistics() {
  return request({
    url: '/api/dataservice/statistics',
    method: 'get'
  })
}

export function getApiDetail(id) {
  return request({
    url: `/api/dataservice/${id}`,
    method: 'get'
  })
}

export function saveOrUpdateApi(data) {
  return request({
    url: '/api/dataservice/save',
    method: 'post',
    data
  })
}

export function publishApi(id) {
  return request({
    url: `/api/dataservice/publish/${id}`,
    method: 'post'
  })
}

export function offlineApi(id) {
  return request({
    url: `/api/dataservice/offline/${id}`,
    method: 'post'
  })
}

export function deleteApi(id) {
  return request({
    url: `/api/dataservice/delete/${id}`,
    method: 'post'
  })
}

export function batchPublishApi(ids) {
  return request({
    url: '/api/dataservice/batch/publish',
    method: 'post',
    data: ids
  })
}

export function batchOfflineApi(ids) {
  return request({
    url: '/api/dataservice/batch/offline',
    method: 'post',
    data: ids
  })
}

export function batchDeleteApi(ids) {
  return request({
    url: '/api/dataservice/batch/delete',
    method: 'post',
    data: ids
  })
} 