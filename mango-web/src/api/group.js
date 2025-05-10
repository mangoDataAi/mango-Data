import request from '@/utils/request'

// 获取分组列表
export function getGroupList(params) {
  return request({
    url: '/api/group/list',
    method: 'get',
    params
  })
}

// 创建分组
export function createGroup(data) {
  return request({
    url: '/api/group',
    method: 'post',
    data
  })
}

// 更新分组
export function updateGroup(id, data) {
  return request({
    url: `/api/group/${id}`,
    method: 'put',
    data
  })
}

// 删除分组
export function deleteGroup(id) {
  return request({
    url: `/api/group/${id}`,
    method: 'delete'
  })
}

// 获取分组详情
export function getGroupDetail(id) {
  return request({
    url: `/api/group/${id}`,
    method: 'get'
  })
}

// 获取分组信息
export function getGroupInfo(groupId) {
  return request({
    url: `/api/group/${groupId}`,
    method: 'get'
  })
} 