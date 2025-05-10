import request from '@/utils/request';

/**
 * 发布新版本
 * @param {Object} data 版本数据
 * @returns {Promise} 请求Promise
 */
export function publishVersion(data) {
  return request({
    url: '/api/metadata/version/publish',
    method: 'post',
    data
  });
}

/**
 * 获取版本历史记录
 * @param {String} metadataId 元数据ID
 * @param {String} metadataType 元数据类型
 * @returns {Promise} 请求Promise
 */
export function getVersionHistory(metadataId, metadataType) {
  return request({
    url: '/api/metadata/version/history',
    method: 'get',
    params: {
      metadataId,
      metadataType
    }
  });
}

/**
 * 获取版本目录树
 * @param {Object} params 查询参数，包含 versionId
 * @returns {Promise} 请求Promise
 */
export function getMetadataVersionCatalogTree(params) {
  return request({
    url: '/api/metadata/version/catalog/tree',
    method: 'get',
    params
  });
}

/**
 * 获取版本数据库列表
 * @param {Object} params 查询参数，包含 versionId, catalogId 等
 * @returns {Promise} 请求Promise
 */
export function getMetadataVersionDatabaseList(params) {
  return request({
    url: '/api/metadata/version/database/list',
    method: 'get',
    params
  });
}

/**
 * 获取版本列表
 * @returns {Promise} 请求Promise
 */
export function getMetadataVersionList() {
  return request({
    url: '/api/metadata/version/list',
    method: 'get'
  });
}



/**
 * 获取版本详情
 * @param {String} versionId 版本ID
 * @param activeView
 * @returns {Promise} 请求Promise
 */
export function getVersionDetail(versionId,activeView) {
  return request({
    url: `/api/metadata/version/collection/detail`,
    method: 'get',
    params: {
      versionId,
      activeView
    }
  });
}

/**
 * 比较两个版本
 * @param {String} versionId1 版本1 ID
 * @param {String} versionId2 版本2 ID
 * @returns {Promise} 请求Promise
 */
export function compareVersions(versionId1, versionId2) {
  return request({
    url: '/api/metadata/version/compare',
    method: 'get',
    params: {
      versionId1,
      versionId2
    }
  });
}

/**
 * 回滚到指定版本
 * @param {String} versionId 版本ID
 * @param {String} comment 回滚说明
 * @param activeView
 * @returns {Promise} 请求Promise
 */
export function rollbackToVersion(versionId, comment, activeView) {
  return request({
    url: `/api/metadata/version/rollback/${versionId}`,
    method: 'post',
    params: {
      comment,
      activeView
    }
  });
}

/**
 * 审批版本
 * @param {String} versionId 版本ID
 * @param {Boolean} approved 是否批准
 * @param {String} comment 审批意见
 * @returns {Promise} 请求Promise
 */
export function approveVersion(versionId, approved, comment) {
  return request({
    url: `/api/metadata/version/approve/${versionId}`,
    method: 'post',
    params: {
      approved,
      comment
    }
  });
}

/**
 * 获取版本变更日志
 * @param {String} versionId 版本ID
 * @returns {Promise} 请求Promise
 */
export function getVersionChangeLogs(versionId) {
  return request({
    url: `/api/metadata/version/changelog/${versionId}`,
    method: 'get'
  });
}

/**
 * 获取当前版本
 * @param {String} metadataId 元数据ID
 * @param {String} metadataType 元数据类型
 * @returns {Promise} 请求Promise
 */
export function getCurrentVersion(metadataId, metadataType) {
  return request({
    url: '/api/metadata/version/current',
    method: 'get',
    params: {
      metadataId,
      metadataType
    }
  });
}

/**
 * 获取版本表列表
 * @param {Object} params 查询参数，包含 versionId, databaseId, current, size 等
 * @returns {Promise} 请求Promise
 */
export function getMetadataVersionTableList(params) {
  return request({
    url: '/api/metadata/version/table/list',
    method: 'get',
    params
  });
}

/**
 * 获取版本字段列表
 * @param {Object} params 查询参数，包含 versionId, tableId, current, size 等
 * @returns {Promise} 请求Promise
 */
export function getMetadataVersionFieldList(params) {
  return request({
    url: '/api/metadata/version/field/list',
    method: 'get',
    params
  });
}

/**
 * 获取版本元数据详情
 * @param {Object} params 查询参数，包含 versionId, metadataId, metadataType 等
 * @returns {Promise} 请求Promise
 */
export function getMetadataVersionDetail(params) {
  return request({
    url: '/api/metadata/version/detail',
    method: 'get',
    params
  });
} 