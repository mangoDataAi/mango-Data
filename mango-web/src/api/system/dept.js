import request from '@/utils/request'

// API基础路径
const baseURL = '/api/system/organizations'

/**
 * 获取部门树形结构
 */
export function getDeptTree() {
  return request({
    url: `${baseURL}/tree`,
    method: 'get'
  })
}

/**
 * 获取部门列表
 */
export function getDeptList(params) {
  return request({
    url: baseURL,
    method: 'get',
    params
  })
}

/**
 * 根据ID集合获取部门
 * @param {Array} ids 部门ID数组
 */
export function getDeptsByIds(ids) {
  return request({
    url: `${baseURL}/getByIds`,
    method: 'post',
    data: ids
  })
}

/**
 * 获取部门详情
 * @param {String} id 部门ID
 */
export function getDeptInfo(id) {
  return request({
    url: `${baseURL}/${id}`,
    method: 'get'
  })
}

/**
 * 新增部门
 * @param {Object} data 部门信息
 */
export function addDept(data) {
  return request({
    url: baseURL,
    method: 'post',
    data
  })
}

/**
 * 修改部门
 * @param {Object} data 部门信息
 */
export function updateDept(data) {
  return request({
    url: baseURL,
    method: 'put',
    data
  })
}

/**
 * 删除部门
 * @param {String} id 部门ID
 */
export function deleteDept(id) {
  return request({
    url: `${baseURL}/${id}`,
    method: 'delete'
  })
} 