import request from '@/utils/request'

// API基础路径
const baseURL = '/api/system/menus'

/**
 * 获取菜单树形结构
 */
export function getMenuTree() {
  return request({
    url: `${baseURL}/tree`,
    method: 'get'
  })
}

/**
 * 获取菜单列表
 */
export function getMenuList(params) {
  return request({
    url: `${baseURL}/list`,
    method: 'get',
    params
  })
}

/**
 * 获取菜单详情
 * @param {String} id 菜单ID
 */
export function getMenuInfo(id) {
  return request({
    url: `${baseURL}/${id}`,
    method: 'get'
  })
}

/**
 * 新增菜单
 * @param {Object} data 菜单信息
 */
export function addMenu(data) {
  return request({
    url: baseURL,
    method: 'post',
    data
  })
}

/**
 * 修改菜单
 * @param {Object} data 菜单信息
 */
export function updateMenu(data) {
  return request({
    url: baseURL,
    method: 'put',
    data
  })
}

/**
 * 删除菜单
 * @param {String} id 菜单ID
 */
export function deleteMenu(id) {
  return request({
    url: `${baseURL}/${id}`,
    method: 'delete'
  })
}

/**
 * 获取路由菜单数据
 */
export function getRoutes() {
  return request({
    url: `${baseURL}/routes`,
    method: 'get'
  })
} 