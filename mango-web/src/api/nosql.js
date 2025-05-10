import request from '@/utils/request'

/**
 * 获取指定数据源的所有集合
 * @param {string} datasourceId 数据源ID
 * @returns {Promise} 返回包含集合列表的Promise
 */
export function getCollections(datasourceId) {
  return request({
    url: `/api/nosql/${datasourceId}/collections`,
    method: 'get'
  })
}

/**
 * 获取指定集合的详细信息
 * @param {string} datasourceId 数据源ID
 * @param {string} collection 集合名称
 * @returns {Promise} 返回包含集合详细信息的Promise
 */
export function getCollectionInfo(datasourceId, collection) {
  return request({
    url: `/api/nosql/${datasourceId}/collection/${collection}/info`,
    method: 'get'
  })
}

/**
 * 获取指定集合的文档数据
 * @param {string} datasourceId 数据源ID
 * @param {string} collection 集合名称
 * @param {Object} params 查询参数
 * @param {number} params.page 页码
 * @param {number} params.pageSize 每页大小
 * @param {Object} params.filter 过滤条件
 * @returns {Promise} 返回包含文档列表的Promise
 */
export function getDocuments(datasourceId, collection, params) {
  return request({
    url: `/api/nosql/${datasourceId}/collection/${collection}/query`,
    method: 'post',
    data: params
  })
}

/**
 * 创建新集合
 * @param {string} datasourceId 数据源ID
 * @param {Object} collectionConfig 集合配置
 * @param {string} collectionConfig.name 集合名称
 * @param {boolean} collectionConfig.capped 是否固定大小集合
 * @param {number} collectionConfig.size 集合大小限制（字节）
 * @param {number} collectionConfig.maxDocuments 文档数量限制
 * @returns {Promise} 返回创建结果的Promise
 */
export function createCollection(datasourceId, collectionConfig) {
  return request({
    url: `/api/nosql/${datasourceId}/collection`,
    method: 'post',
    data: collectionConfig
  })
}

/**
 * 删除集合
 * @param {string} datasourceId 数据源ID
 * @param {string} collection 集合名称
 * @returns {Promise} 返回删除结果的Promise
 */
export function deleteCollection(datasourceId, collection) {
  return request({
    url: `/api/nosql/${datasourceId}/collection/${collection}`,
    method: 'delete'
  })
}

/**
 * 在集合中创建文档
 * @param {string} datasourceId 数据源ID
 * @param {string} collection 集合名称
 * @param {Object} document 文档数据
 * @returns {Promise} 返回创建结果的Promise
 */
export function createDocument(datasourceId, collection, document) {
  return request({
    url: `/api/nosql/${datasourceId}/collection/${collection}/document`,
    method: 'post',
    data: document
  })
}

/**
 * 更新文档
 * @param {string} datasourceId 数据源ID
 * @param {string} collection 集合名称
 * @param {string} documentId 文档ID
 * @param {Object} document 更新的文档数据
 * @returns {Promise} 返回更新结果的Promise
 */
export function updateDocument(datasourceId, collection, documentId, document) {
  // 确保documentId是字符串并进行URL编码
  const encodedDocumentId = encodeURIComponent(String(documentId));
  console.log('更新文档 - 编码前ID:', documentId, '编码后ID:', encodedDocumentId);
  
  // 处理文档对象，确保移除特殊的_id格式，避免服务器端冲突
  const safeDocument = { ...document };
  if (safeDocument._id) {
    // 如果_id是对象且包含timestamp和date，移除它以避免服务器端错误
    if (typeof safeDocument._id === 'object' && 
        (safeDocument._id.timestamp || safeDocument._id.$oid)) {
      console.log('移除复杂_id对象，将由服务器使用URL中的ID', safeDocument._id);
      delete safeDocument._id;
    }
  }
  
  return request({
    url: `/api/nosql/${datasourceId}/collection/${collection}/document/${encodedDocumentId}`,
    method: 'put',
    data: safeDocument
  })
}

/**
 * 删除文档
 * @param {string} datasourceId 数据源ID
 * @param {string} collection 集合名称
 * @param {string} documentId 文档ID
 * @returns {Promise} 返回删除结果的Promise
 */
export function deleteDocument(datasourceId, collection, documentId) {
  // 确保documentId是字符串并进行URL编码
  const encodedDocumentId = encodeURIComponent(String(documentId));
  
  return request({
    url: `/api/nosql/${datasourceId}/collection/${collection}/document/${encodedDocumentId}`,
    method: 'delete'
  })
}

/**
 * 获取集合Schema
 * @param {string} datasourceId 数据源ID
 * @param {string} collection 集合名称
 * @returns {Promise} 返回Schema信息的Promise
 */
export function getCollectionSchema(datasourceId, collection) {
  return request({
    url: `/api/nosql/${datasourceId}/collection/${collection}/schema`,
    method: 'get'
  })
}

/**
 * 执行数据库查询（适用于MongoDB等支持查询语言的NoSQL数据库）
 * @param {string} datasourceId 数据源ID
 * @param {Object} queryConfig 查询配置
 * @param {string} queryConfig.collection 集合名称
 * @param {Object} queryConfig.filter 过滤条件
 * @param {Object} queryConfig.projection 投影字段
 * @param {Object} queryConfig.sort 排序
 * @param {number} queryConfig.limit 限制返回数量
 * @param {number} queryConfig.skip 跳过文档数
 * @returns {Promise} 返回查询结果的Promise
 */
export function executeQuery(datasourceId, queryConfig) {
  return request({
    url: `/api/nosql/${datasourceId}/query`,
    method: 'post',
    data: queryConfig
  })
}

/**
 * 获取数据库统计信息
 * @param {string} datasourceId 数据源ID
 * @returns {Promise} 返回统计信息的Promise
 */
export function getDatabaseStats(datasourceId) {
  return request({
    url: `/api/nosql/${datasourceId}/stats`,
    method: 'get'
  })
}

/**
 * 执行数据库命令（高级操作）
 * @param {string} datasourceId 数据源ID
 * @param {Object} command 命令对象
 * @returns {Promise} 返回命令执行结果的Promise
 */
export function executeCommand(datasourceId, command) {
  return request({
    url: `/api/nosql/${datasourceId}/command`,
    method: 'post',
    data: command
  })
}

/**
 * 创建索引
 * @param {string} datasourceId 数据源ID
 * @param {string} collection 集合名称
 * @param {Object} indexConfig 索引配置
 * @returns {Promise} 返回创建结果的Promise
 */
export function createIndex(datasourceId, collection, indexConfig) {
  return request({
    url: `/api/nosql/${datasourceId}/collection/${collection}/index`,
    method: 'post',
    data: indexConfig
  })
}

/**
 * 删除索引
 * @param {string} datasourceId 数据源ID
 * @param {string} collection 集合名称
 * @param {string} indexName 索引名称
 * @returns {Promise} 返回删除结果的Promise
 */
export function deleteIndex(datasourceId, collection, indexName) {
  return request({
    url: `/api/nosql/${datasourceId}/collection/${collection}/index/${indexName}`,
    method: 'delete'
  })
}

/**
 * 获取集合的索引列表
 * @param {string} datasourceId 数据源ID
 * @param {string} collection 集合名称
 * @returns {Promise} 返回索引列表的Promise
 */
export function getIndexes(datasourceId, collection) {
  return request({
    url: `/api/nosql/${datasourceId}/collection/${collection}/index`,
    method: 'get'
  })
}

/**
 * 获取Redis特定键的值
 * @param {string} datasourceId 数据源ID
 * @param {string} key 键名
 * @returns {Promise} 返回键值的Promise
 */
export function getRedisKeyValue(datasourceId, key) {
  return request({
    url: `/api/nosql/${datasourceId}/redis/keys/${encodeURIComponent(key)}`,
    method: 'get'
  })
}

/**
 * 设置Redis键值
 * @param {string} datasourceId 数据源ID
 * @param {string} key 键名
 * @param {Object} valueConfig 值配置
 * @param {string} valueConfig.type 值类型 (string, list, hash, set, zset)
 * @param {any} valueConfig.value 键值
 * @param {number} valueConfig.ttl 过期时间（秒）
 * @returns {Promise} 返回设置结果的Promise
 */
export function setRedisKeyValue(datasourceId, key, valueConfig) {
  return request({
    url: `/api/nosql/${datasourceId}/redis/keys/${encodeURIComponent(key)}`,
    method: 'post',
    data: valueConfig
  })
}

/**
 * 删除Redis键
 * @param {string} datasourceId 数据源ID
 * @param {string} key 键名
 * @returns {Promise} 返回删除结果的Promise
 */
export function deleteRedisKey(datasourceId, key) {
  return request({
    url: `/api/nosql/${datasourceId}/redis/keys/${encodeURIComponent(key)}`,
    method: 'delete'
  })
}

/**
 * 获取Redis键列表
 * @param {string} datasourceId 数据源ID
 * @param {Object} params 查询参数
 * @param {string} params.pattern 键名模式
 * @param {string} params.type 键类型
 * @returns {Promise} 返回键列表的Promise
 */
export function getRedisKeys(datasourceId, params) {
  return request({
    url: `/api/nosql/${datasourceId}/redis/keys`,
    method: 'get',
    params
  })
} 