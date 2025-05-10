import request from '@/utils/request'

/**
 * 获取标准包树结构
 * @returns {Promise}
 */
export function getPackageTree() {
  return request({
    url: '/api/dataasset/standard/package/tree',
    method: 'get'
  })
}

/**
 * 获取对象列表
 * @param {Object} params 查询参数
 * @returns {Promise}
 */
export function getObjectList(params) {
  return request({
    url: '/api/dataasset/standard/object/page',
    method: 'get',
    params
  })
}

/**
 * 获取规则列表
 * @param {Object} params 查询参数
 * @returns {Promise}
 */
export function getRuleList(params) {
  return request({
    url: '/api/dataasset/standard/rule/page',
    method: 'get',
    params
  })
}

/**
 * 保存或更新规则
 * @param {Object} data 规则数据
 * @returns {Promise}
 */
export function saveRule(data) {
  return request({
    url: '/api/dataasset/standard/rule/save',
    method: 'post',
    data
  })
}

/**
 * 删除规则
 * @param {string} id 规则ID
 * @returns {Promise}
 */
export function deleteRule(id) {
  return request({
    url: `/api/dataasset/standard/rule/${id}`,
    method: 'delete'
  })
}

/**
 * 保存或更新对象（表）
 * @param {Object} data 对象数据
 * @returns {Promise}
 */
export function saveObject(data) {
  return request({
    url: '/api/dataasset/standard/object/save',
    method: 'post',
    data
  })
}

/**
 * 删除对象（表）
 * @param {string} id 对象ID
 * @returns {Promise}
 */
export function deleteObject(id) {
  return request({
    url: `/api/dataasset/standard/object/${id}`,
    method: 'delete'
  })
}

/**
 * 获取规则详情
 * @param {string} id 规则ID
 * @returns {Promise}
 */
export function getRuleDetail(id) {
  return request({
    url: '/api/dataasset/standard/rule/page',
    method: 'get',
    params: {
      id: id,
      current: 1,
      size: 1
    }
  }).then(response => {
    // 处理响应，提取第一条记录作为详情
    if (response.code === 0 && response.data && response.data.records && response.data.records.length > 0) {
      // 返回与原API相同格式的响应
      return {
        code: 0,
        msg: 'success',
        data: response.data.records[0]
      };
    } else {
      // 没有找到记录
      return {
        code: 404,
        msg: '未找到规则详情',
        data: null
      };
    }
  });
}

/**
 * 获取对象详情
 * @param {string} id 对象ID
 * @returns {Promise}
 */
export function getObjectDetail(id) {
  return request({
    url: `/api/dataasset/standard/object/detail/${id}`,
    method: 'get'
  })
}

/**
 * 获取规则模板列表
 * @returns {Promise}
 */
export function getRuleTemplates() {
  return request({
    url: '/api/dataasset/standard/template/list',
    method: 'get'
  })
}

/**
 * 应用规则
 * @param {Object} data 应用规则的数据
 * @returns {Promise}
 */
export function applyRule(data) {
  return request({
    url: '/api/dataasset/standard/rule/apply',
    method: 'post',
    data
  })
}

/**
 * 验证规则
 * @param {Object} data 验证规则的数据
 * @returns {Promise}
 */
export function validateRule(data) {
  return request({
    url: '/api/dataasset/standard/rule/validate',
    method: 'post',
    data
  })
}

/**
 * 获取规则验证结果
 * @param {string} taskId 任务ID
 * @returns {Promise}
 */
export function getRuleValidationResult(taskId) {
  return request({
    url: `/api/dataasset/standard/rule/validation/result/${taskId}`,
    method: 'get'
  })
}

/**
 * 导出规则
 * @param {Object} params 导出参数
 * @returns {Promise}
 */
export function exportRules(params) {
  return request({
    url: '/api/dataasset/standard/rule/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

/**
 * 导入规则
 * @param {FormData} formData 包含文件的表单数据
 * @returns {Promise}
 */
export function importRules(formData) {
  return request({
    url: '/api/dataasset/standard/rule/import',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 获取命名标准列表
 * @param {Object} params 查询参数
 * @returns {Promise}
 */
export function getNamingStandardList(params) {
  return request({
    url: '/api/dataasset/standard/naming/list',
    method: 'get',
    params
  })
}

/**
 * 获取命名标准详情
 * @param {string} id 命名标准ID
 * @returns {Promise}
 */
export function getNamingStandardDetail(id) {
  return request({
    url: `/api/dataasset/standard/naming/detail/${id}`,
    method: 'get'
  })
} 