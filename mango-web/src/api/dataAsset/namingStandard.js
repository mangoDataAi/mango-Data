import request from '@/utils/request'

// 命名标准相关接口
export default {
  // 获取统计数据
  getStatistics() {
    return request({
      url: '/api/dataasset/standard/naming/statistics',
      method: 'get'
    })
  },

  // 获取命名标准列表
  getList(params) {
    return request({
      url: '/api/dataasset/standard/naming/list',
      method: 'get',
      params
    })
  },

  // 获取主题域选项
  getDomainOptions() {
    return request({
      url: '/api/dataasset/standard/naming/domain/list',
      method: 'get'
    })
  },

  // 获取命名标准详情
  getDetail(id) {
    return request({
      url: `/api/dataasset/standard/naming/${id}`,
      method: 'get'
    })
  },

  // 创建命名标准
  create(data) {
    return request({
      url: '/api/dataasset/standard/naming/create',
      method: 'post',
      data
    })
  },

  // 更新命名标准
  update(id, data) {
    return request({
      url: `/api/dataasset/standard/naming/${id}`,
      method: 'put',
      data
    })
  },

  // 删除命名标准
  delete(id) {
    return request({
      url: `/api/dataasset/standard/naming/${id}`,
      method: 'delete'
    })
  },

  // 验证名称
  validate(data) {
    return request({
      url: '/api/dataasset/standard/naming/validate',
      method: 'post',
      data
    })
  },


  // 分页查询命名标准
  page(params) {
    return request({
      url: '/api/dataasset/standard/naming/page',
      method: 'get',
      params: {
        pageNum: params.pageNum,
        pageSize: params.pageSize,
        name: params.name,
        target: params.target,
        domainId: params.domainId,
        dimensionTypes: params.dimensionTypes ? params.dimensionTypes.join(',') : undefined
      }
    })
  },

  // 执行规则检查
  check(data) {
    return request({
      url: '/api/dataasset/standard/naming/check',
      method: 'post',
      data
    })
  },

  exportCheckResult(data, config = {}) {
    return request({
      url: '/api/dataasset/standard/naming/check/export',
      method: 'post',
      data,
      ...config
    })
  }
} 