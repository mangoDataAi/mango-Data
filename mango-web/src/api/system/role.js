import request from '@/utils/request'

// API基础路径
const baseURL = '/api/system/roles'

/**
 * 获取角色概览数据
 */
export function getRoleOverview() {
  return request({
    url: `${baseURL}/overview`,
    method: 'get'
  })
}

/**
 * 分页查询角色列表
 * @param {Object} params 查询参数
 */
export function getRoleList(params) {
  return request({
    url: `${baseURL}/page`,
    method: 'get',
    params
  })
}

/**
 * 获取角色详情
 * @param {String} id 角色ID
 */
export function getRoleInfo(id) {
  console.log('获取角色详情, ID:', id);
  return request({
    url: `${baseURL}/${id}`,
    method: 'get'
  }).then(res => {
    console.log('角色详情响应:', res);
    return res;
  }).catch(error => {
    console.error('获取角色详情失败:', error);
    throw error;
  });
}

/**
 * 新增角色
 * @param {Object} data 角色信息
 */
export function addRole(data) {
  console.log('添加角色请求数据:', data);
  return request({
    url: baseURL,
    method: 'post',
    data: {
      name: data.name,
      code: data.code,
      color: data.color,
      sort: data.sort || 0,
      status: data.status || 1,
      remark: data.remark || '',
      dataScope: data.dataScope || 'ALL',
      menuIds: data.menuIds || [],
      deptIds: data.deptIds || []
    }
  })
}

/**
 * 修改角色
 * @param {Object} data 角色信息
 */
export function updateRole(data) {
  console.log('修改角色请求数据:', data);
  
  // 确保前端发送有效数据结构
  const requestData = {
    id: data.id,
    name: data.name,
    code: data.code,
    color: data.color || '#1890FF',
    sort: data.sort || 0,
    status: data.status !== undefined ? data.status : 1,
    dataScope: data.dataScope || 'ALL',
    remark: data.remark || '',
    menuIds: data.menuIds || [],
    deptIds: data.deptIds || []
  };
  
  return request({
    url: baseURL,
    method: 'put',
    data: requestData
  }).then(res => {
    console.log('修改角色响应:', res);
    return res;
  }).catch(error => {
    console.error('修改角色失败:', error);
    throw error;
  });
}

/**
 * 删除角色
 * @param {String} ids 角色ID字符串，多个以逗号分隔
 */
export function deleteRole(ids) {
  return request({
    url: `${baseURL}/${ids}`,
    method: 'delete'
  })
}

/**
 * 更新角色状态
 * @param {String} id 角色ID
 * @param {Number} status 状态(1正常 0停用)
 */
export function updateRoleStatus(id, status) {
  return request({
    url: `${baseURL}/status/${id}/${status}`,
    method: 'put'
  })
}

/**
 * 角色权限分配
 * @param {String} roleId 角色ID
 * @param {Array} menuIds 菜单ID数组
 */
export function assignRoleMenus(roleId, menuIds) {
  return request({
    url: `${baseURL}/auth/${roleId}`,
    method: 'put',
    data: menuIds
  })
}

/**
 * 角色数据权限分配
 * @param {String} roleId 角色ID
 * @param {Object} data 数据权限信息
 */
export function assignRoleDataScope(roleId, data) {
  return request({
    url: `${baseURL}/dataScope/${roleId}`,
    method: 'put',
    data
  })
}

/**
 * 获取角色选项列表
 */
export function getRoleOptions() {
  return request({
    url: `${baseURL}/options`,
    method: 'get'
  })
}

/**
 * 获取用户角色
 * @param {String} userId 用户ID
 */
export function getUserRoles(userId) {
  return request({
    url: `${baseURL}/user/${userId}`,
    method: 'get'
  })
}

/**
 * 分配用户角色
 * @param {String} userId 用户ID
 * @param {Array} roleIds 角色ID数组
 */
export function assignUserRoles(userId, roleIds) {
  return request({
    url: `${baseURL}/user/${userId}`,
    method: 'put',
    data: roleIds
  })
}

/**
 * 导出角色
 * @param {Object} params 查询参数
 */
export function exportRole(params) {
  return request({
    url: `${baseURL}/export`,
    method: 'get',
    params,
    responseType: 'blob'
  })
}

/**
 * 导入角色数据
 * @param {Array} data 角色数据
 */
export function importRole(data) {
  return request({
    url: `${baseURL}/import`,
    method: 'post',
    data
  })
}

/**
 * 下载导入模板
 */
export function downloadTemplate() {
  return request({
    url: `${baseURL}/template`,
    method: 'get',
    responseType: 'blob'
  })
}

/**
 * 预览导入数据
 * @param {FormData} formData 上传的文件数据
 */
export function previewImport(formData) {
  return request({
    url: `${baseURL}/preview`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
} 