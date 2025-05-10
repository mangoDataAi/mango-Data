import request from '@/utils/request'

/**
 * 获取文件系统内容
 * @param {string} datasourceId 数据源ID
 * @param {string} path 路径
 * @param {boolean} recursive 是否递归获取子目录
 * @returns {Promise} 响应Promise
 */
export function getFileSystemContents(datasourceId, path, recursive = false) {
  return request({
    url: `/api/filesystem/${datasourceId}/list`,
    method: 'get',
    params: {
      path,
      filter: recursive ? '.*' : null
    }
  })
}

/**
 * 获取文件和目录列表
 * @param {string} datasourceId 数据源ID
 * @param {string} path 路径，默认为根目录
 * @returns {Promise} 响应Promise
 */
export function listFiles(datasourceId, path = '/') {
  return request({
    url: `/api/filesystem/${datasourceId}/list`,
    method: 'get',
    params: {
      path
    }
  })
}

/**
 * 获取文件预览内容
 * @param {string} datasourceId 数据源ID
 * @param {string} path 文件路径
 * @returns {Promise} 响应Promise
 */
export function getFilePreview(datasourceId, path) {
  return request({
    url: `/api/filesystem/${datasourceId}/preview`,
    method: 'get',
    params: {
      path
    }
  })
}

/**
 * 创建目录
 * @param {string} datasourceId 数据源ID
 * @param {string} path 目录路径
 * @returns {Promise} 响应Promise
 */
export function createDirectory(datasourceId, path) {
  return request({
    url: `/api/filesystem/${datasourceId}/mkdir`,
    method: 'post',
    data: {
      path
    }
  })
}

/**
 * 删除文件或目录
 * @param {string} datasourceId 数据源ID
 * @param {string} path 文件或目录路径
 * @returns {Promise} 响应Promise
 */
export function deleteFileOrDirectory(datasourceId, path) {
  return request({
    url: `/api/filesystem/${datasourceId}/delete`,
    method: 'delete',
    params: {
      path
    }
  })
}

/**
 * 上传文件
 * @param {string} datasourceId 数据源ID
 * @param {string} path 目标路径
 * @param {FormData} formData 文件表单数据
 * @returns {Promise} 响应Promise
 */
export function uploadFile(datasourceId, path, formData) {
  return request({
    url: `/api/filesystem/${datasourceId}/upload`,
    method: 'post',
    params: {
      path
    },
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 复制文件或目录
 * @param {string} datasourceId 数据源ID
 * @param {string} sourcePath 源路径
 * @param {string} targetPath 目标路径
 * @returns {Promise} 响应Promise
 */
export function copyFileOrDirectory(datasourceId, sourcePath, targetPath) {
  return request({
    url: `/api/filesystem/${datasourceId}/copy`,
    method: 'post',
    data: {
      sourcePath,
      targetPath
    }
  })
}

/**
 * 移动文件或目录
 * @param {string} datasourceId 数据源ID
 * @param {string} sourcePath 源路径
 * @param {string} targetPath 目标路径
 * @returns {Promise} 响应Promise
 */
export function moveFileOrDirectory(datasourceId, sourcePath, targetPath) {
  return request({
    url: `/api/filesystem/${datasourceId}/move`,
    method: 'post',
    data: {
      sourcePath,
      targetPath
    }
  })
}

/**
 * 重命名文件或目录
 * @param {string} datasourceId 数据源ID
 * @param {string} path 路径
 * @param {string} newName 新名称
 * @returns {Promise} 响应Promise
 */
export function renameFileOrDirectory(datasourceId, path, newName) {
  return request({
    url: `/api/filesystem/${datasourceId}/rename`,
    method: 'post',
    data: {
      path,
      newName
    }
  })
}

/**
 * 获取文件系统信息
 * @param {string} datasourceId 数据源ID
 * @returns {Promise} 响应Promise
 */
export function getFileSystemInfo(datasourceId) {
  return request({
    url: `/api/filesystem/${datasourceId}/info`,
    method: 'get'
  })
}

/**
 * 获取文件系统统计信息
 * @param {string} datasourceId 数据源ID
 * @returns {Promise} 响应Promise
 */
export function getFileSystemStats(datasourceId) {
  return request({
    url: `/api/filesystem/${datasourceId}/stats`,
    method: 'get'
  })
}

/**
 * 获取文件内容
 * @param {string} datasourceId 数据源ID
 * @param {string} path 文件路径
 * @param {boolean} binary 是否为二进制文件
 * @returns {Promise} 响应Promise
 */
export function getFileContents(datasourceId, path, binary = false) {
  return request({
    url: `/api/filesystem/${datasourceId}/content`,
    method: 'get',
    params: {
      path,
      binary
    },
    responseType: binary ? 'blob' : 'text'
  })
} 