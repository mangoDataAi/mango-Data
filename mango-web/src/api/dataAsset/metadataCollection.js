import request from '@/utils/request'

/**
 * 获取元数据采集执行历史
 * @param {string} collectionId 采集ID
 * @param {number} limit 限制数量
 * @returns {Promise} 响应对象
 */
export function getCollectionExecutionHistory(collectionId, limit = 10) {
  return request({
    url: '/api/metadata/version/collection/execution/history',
    method: 'get',
    params: { collectionId, limit }
  })
}

/**
 * 获取元数据采集版本的执行详情
 * @param {string} versionId 版本ID
 * @returns {Promise} 响应对象
 */
export function getCollectionVersionExecution(versionId) {
  return request({
    url: '/api/metadata/version/collection/execution',
    method: 'get',
    params: { versionId }
  })
}

/**
 * 比较元数据采集两个版本
 * @param {string} versionId1 版本1 ID
 * @param {string} versionId2 版本2 ID
 * @returns {Promise} 响应对象
 */
export function compareCollectionVersions(versionId1, versionId2) {
  return request({
    url: '/api/metadata/version/collection/compare',
    method: 'get',
    params: { versionId1, versionId2 }
  })
}

/**
 * 获取元数据采集最新版本
 * @param {string} collectionId 采集ID
 * @param {string} dataType 数据类型
 * @returns {Promise} 响应对象
 */
export function getLatestCollectionVersion(collectionId, dataType) {
  return request({
    url: '/api/metadata/version/collection/latest',
    method: 'get',
    params: { collectionId, dataType }
  })
}

/**
 * 获取数据库字段类型分布
 * @param {String} databaseId 数据库ID
 * @param {Boolean} versioned 是否查询定版数据
 * @returns {Promise} 请求结果
 */
export function getFieldTypeDistribution(databaseId, versioned = false) {
  return request({
    url: `/api/metadata/version/field-type-distribution/${databaseId}`,
    method: 'get',
    params: { versioned }
  });
}

/**
 * 获取数据库索引分析数据
 * @param {String} databaseId 数据库ID
 * @param {Boolean} versioned 是否查询定版数据
 * @returns {Promise} 请求结果，包含索引类型分布和表索引覆盖数据
 */
export function getIndexAnalysis(databaseId, versioned = false) {
  return request({
    url: `/api/metadata/version/index-analysis/${databaseId}`,
    method: 'get',
    params: { versioned }
  });
} 