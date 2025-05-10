import request from '@/utils/request'

/**
 * 获取图数据库的模型结构（节点类型和关系类型）
 * @param {string} datasourceId 数据源ID
 * @returns {Promise} 返回包含图数据库模型的Promise
 */
export function getGraphSchema(datasourceId) {
  return request({
    url: `/api/graph/${datasourceId}/schema`,
    method: 'get'
  })
}

/**
 * 执行Cypher查询
 * @param {string} datasourceId 数据源ID
 * @param {Object} queryConfig 查询配置
 * @param {string} queryConfig.query Cypher查询语句
 * @param {Object} queryConfig.params 查询参数（可选）
 * @returns {Promise} 返回查询结果的Promise
 */
export function executeQuery(datasourceId, queryConfig) {
  return request({
    url: `/api/graph/${datasourceId}/query`,
    method: 'post',
    data: queryConfig
  })
}

/**
 * 创建节点
 * @param {string} datasourceId 数据源ID
 * @param {Object} nodeConfig 节点配置
 * @param {Array<string>} nodeConfig.labels 节点标签数组
 * @param {Object} nodeConfig.properties 节点属性对象
 * @returns {Promise} 返回创建结果的Promise
 */
export function createNode(datasourceId, nodeConfig) {
  return request({
    url: `/api/graph/${datasourceId}/node`,
    method: 'post',
    data: nodeConfig
  })
}

/**
 * 更新节点
 * @param {string} datasourceId 数据源ID
 * @param {string} nodeId 节点ID
 * @param {Object} nodeConfig 节点配置
 * @param {Array<string>} nodeConfig.labels 节点标签数组（可选）
 * @param {Object} nodeConfig.properties 节点属性对象
 * @returns {Promise} 返回更新结果的Promise
 */
export function updateNode(datasourceId, nodeId, nodeConfig) {
  return request({
    url: `/api/graph/${datasourceId}/node/${nodeId}`,
    method: 'put',
    data: nodeConfig
  })
}

/**
 * 删除节点
 * @param {string} datasourceId 数据源ID
 * @param {string} nodeId 节点ID
 * @param {boolean} detachDelete 是否同时删除关联的关系
 * @returns {Promise} 返回删除结果的Promise
 */
export function deleteNode(datasourceId, nodeId, detachDelete = true) {
  return request({
    url: `/api/graph/${datasourceId}/node/${nodeId}`,
    method: 'delete',
    params: { detachDelete }
  })
}

/**
 * 创建关系
 * @param {string} datasourceId 数据源ID
 * @param {Object} relationshipConfig 关系配置
 * @param {string} relationshipConfig.fromNode 起始节点ID
 * @param {string} relationshipConfig.type 关系类型
 * @param {string} relationshipConfig.toNode 目标节点ID
 * @param {Object} relationshipConfig.properties 关系属性对象
 * @returns {Promise} 返回创建结果的Promise
 */
export function createRelationship(datasourceId, relationshipConfig) {
  return request({
    url: `/api/graph/${datasourceId}/relationship`,
    method: 'post',
    data: relationshipConfig
  })
}

/**
 * 更新关系
 * @param {string} datasourceId 数据源ID
 * @param {string} relationshipId 关系ID
 * @param {Object} relationshipConfig 关系配置
 * @param {Object} relationshipConfig.properties 关系属性对象
 * @returns {Promise} 返回更新结果的Promise
 */
export function updateRelationship(datasourceId, relationshipId, relationshipConfig) {
  return request({
    url: `/api/graph/${datasourceId}/relationship/${relationshipId}`,
    method: 'put',
    data: relationshipConfig
  })
}

/**
 * 删除关系
 * @param {string} datasourceId 数据源ID
 * @param {string} relationshipId 关系ID
 * @returns {Promise} 返回删除结果的Promise
 */
export function deleteRelationship(datasourceId, relationshipId) {
  return request({
    url: `/api/graph/${datasourceId}/relationship/${relationshipId}`,
    method: 'delete'
  })
}

/**
 * 获取节点属性
 * @param {string} datasourceId 数据源ID
 * @param {string} nodeId 节点ID
 * @returns {Promise} 返回节点属性的Promise
 */
export function getNodeProperties(datasourceId, nodeId) {
  return request({
    url: `/api/graph/${datasourceId}/node/${nodeId}/properties`,
    method: 'get'
  })
}

/**
 * 获取关系属性
 * @param {string} datasourceId 数据源ID
 * @param {string} relationshipId 关系ID
 * @returns {Promise} 返回关系属性的Promise
 */
export function getRelationshipProperties(datasourceId, relationshipId) {
  return request({
    url: `/api/graph/${datasourceId}/relationship/${relationshipId}/properties`,
    method: 'get'
  })
}

/**
 * 获取节点的相邻节点
 * @param {string} datasourceId 数据源ID
 * @param {Object} params 查询参数
 * @param {string} params.nodeType 节点类型
 * @param {string} params.propertyFilter 属性过滤条件
 * @param {number} params.depth 查询深度
 * @param {number} params.limit 返回结果限制
 * @returns {Promise} 返回相邻节点的Promise
 */
export function getNeighbors(datasourceId, params) {
  return request({
    url: `/api/graph/${datasourceId}/neighbors`,
    method: 'get',
    params
  })
}

/**
 * 获取最短路径
 * @param {string} datasourceId 数据源ID
 * @param {Object} pathConfig 路径配置
 * @param {string} pathConfig.sourceId 起始节点ID
 * @param {string} pathConfig.targetId 目标节点ID
 * @param {number} pathConfig.maxDepth 最大深度
 * @param {Array<string>} pathConfig.relationshipTypes 关系类型限制（可选）
 * @returns {Promise} 返回最短路径的Promise
 */
export function getShortestPath(datasourceId, pathConfig) {
  return request({
    url: `/api/graph/${datasourceId}/shortestPath`,
    method: 'post',
    data: pathConfig
  })
}

/**
 * 获取图数据库统计信息
 * @param {string} datasourceId 数据源ID
 * @returns {Promise} 返回统计信息的Promise
 */
export function getDatabaseStats(datasourceId) {
  return request({
    url: `/api/graph/${datasourceId}/stats`,
    method: 'get'
  })
} 