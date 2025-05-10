import request from '@/utils/request';

export default {
  /**
   * 获取图数据库Schema
   * @param {string} dataSourceId 数据源ID
   * @returns {Promise<Object>} 请求响应
   */
  getGraphSchema(dataSourceId) {
    return request({
      url: `/api/graph/${dataSourceId}/schema`,
      method: 'get'
    });
  },

  /**
   * 执行图数据库查询
   * @param {string} dataSourceId 数据源ID
   * @param {string} query 查询语句
   * @returns {Promise<Object>} 请求响应
   */
  executeQuery(dataSourceId, query) {
    return request({
      url: `/api/graph/${dataSourceId}/query`,
      method: 'post',
      data: { query }
    });
  },

  /**
   * 创建节点
   * @param {string} dataSourceId 数据源ID
   * @param {Object} node 节点数据
   * @returns {Promise<Object>} 请求响应
   */
  createNode(dataSourceId, node) {
    return request({
      url: `/api/graph/${dataSourceId}/node`,
      method: 'post',
      data: node
    });
  },

  /**
   * 删除节点
   * @param {string} dataSourceId 数据源ID
   * @param {string} nodeId 节点ID
   * @returns {Promise<Object>} 请求响应
   */
  deleteNode(dataSourceId, nodeId) {
    return request({
      url: `/api/graph/${dataSourceId}/node/${nodeId}`,
      method: 'delete'
    });
  },

  /**
   * 创建关系
   * @param {string} dataSourceId 数据源ID
   * @param {Object} relationship 关系数据
   * @returns {Promise<Object>} 请求响应
   */
  createRelationship(dataSourceId, relationship) {
    return request({
      url: `/api/graph/${dataSourceId}/relationship`,
      method: 'post',
      data: relationship
    });
  },

  /**
   * 删除关系
   * @param {string} dataSourceId 数据源ID
   * @param {string} relationshipId 关系ID
   * @returns {Promise<Object>} 请求响应
   */
  deleteRelationship(dataSourceId, relationshipId) {
    return request({
      url: `/api/graph/${dataSourceId}/relationship/${relationshipId}`,
      method: 'delete'
    });
  },

  /**
   * 获取节点邻居
   * @param {string} dataSourceId 数据源ID
   * @param {Object} params 请求参数
   * @returns {Promise<Object>} 请求响应
   */
  getNeighbors(dataSourceId, params) {
    return request({
      url: `/api/graph/${dataSourceId}/neighbors`,
      method: 'post',
      data: params
    });
  },

  /**
   * 获取所有节点
   * @param {string} dataSourceId 数据源ID
   * @param {number} limit 限制数量
   * @returns {Promise<Object>} 请求响应
   */
  getAllNodes(dataSourceId, limit = 100) {
    return request({
      url: `/api/graph/${dataSourceId}/nodes`,
      method: 'get',
      params: { limit }
    });
  }
}; 