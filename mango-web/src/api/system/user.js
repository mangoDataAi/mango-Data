import request from '@/utils/request'

// API基础路径
const baseURL = '/api/system/users'

/**
 * 获取用户列表
 * @param {Object} params 查询参数
 */
export function getUserList(params) {
  return request({
    url: `${baseURL}/page`,
    method: 'post',
    data: params
  })
}

/**
 * 获取用户详情
 * @param {String} id 用户ID
 */
export function getUserInfo(id) {
  return request({
    url: `${baseURL}/${id}`,
    method: 'get'
  })
}

/**
 * 新增用户
 * @param {Object} data 用户信息
 */
export function addUser(data) {
  return request({
    url: baseURL,
    method: 'post',
    data
  })
}

/**
 * 修改用户
 * @param {Object} data 用户信息
 */
export function updateUser(data) {
  return request({
    url: baseURL,
    method: 'put',
    data
  })
}

/**
 * 删除用户
 * @param {String} ids 用户ID字符串，多个以逗号分隔
 */
export function deleteUser(ids) {
  return request({
    url: `${baseURL}/${ids}`,
    method: 'delete'
  })
}

/**
 * 重置用户密码
 * @param {String} id 用户ID
 * @param {String} password 新密码
 */
export function resetUserPassword(id, password) {
  return request({
    url: `${baseURL}/password/reset`,
    method: 'put',
    data: {
      userId: id,
      password
    }
  })
}


/**
 * 获取指定角色下的用户列表
 * @param {String} roleId 角色ID
 */
export function getUsersByRoleId(roleId) {
  return request({
    url: `${baseURL}/role/${roleId}`,
    method: 'get'
  })
}

/**
 * 导出用户
 * @param {Object} params 查询参数
 */
export function exportUser(params) {
  return request({
    url: `${baseURL}/export`,
    method: 'get',
    params,
    responseType: 'blob'
  })
}

/**
 * 导入用户
 * @param {Array} data 用户数据
 */
export function importUser(data) {
  return request({
    url: `${baseURL}/import`,
    method: 'post',
    data
  })
}

/**
 * 下载用户导入模板
 */
export function downloadTemplate() {
  return request({
    url: `${baseURL}/template`,
    method: 'get',
    responseType: 'blob'
  })
} 