import request from '@/utils/request'

// 获取目录树
export function getCatalogTree() {
  console.log('调用获取目录树API')
  return request({
    url: '/api/metadata/catalog/tree',
    method: 'get'
  }).then(response => {
    console.log('目录树原始响应:', response)
    return response
  })
}

// 获取完整的元数据层级结构（目录-库-表-字段）
export function getMetadataHierarchy() {
  return request({
    url: '/api/metadata/catalog/hierarchy',
    method: 'get'
  })
}

// 根据目录ID获取数据库列表
export async function getDatabasesByCatalogId(params) {
  try {
    // 确保 params 是一个对象
    const validParams = params && typeof params === 'object' ? params : { catalogId: params };
    
    // 记录实际发送的参数，帮助调试
    console.log('发送获取数据库列表请求，参数:', validParams);
    
    const response = await request({
      url: '/api/metadata/catalog/databases',
      method: 'get',
      params: validParams
    });
    
    // 确保返回一个有效对象，同时记录响应用于调试
    console.log('获取数据库列表响应:', response);
    return response || { data: [], total: 0 };
  } catch (error) {
    console.error('获取数据库列表请求失败:', error);
    // 返回一个带有明确结构的空对象
    return { data: [], total: 0, error: error.message || '未知错误' };
  }
}

/**
 * 根据数据库ID获取表列表
 * @param {Object} params 查询参数
 * @returns {Promise} 返回表列表
 */
export function getTablesByDatabaseId(params) {
  return request({
    url: '/api/metadata/catalog/tables',
    method: 'get',
    params
  })
}

// 根据表ID获取字段列表
export function getFieldsByTableId(tableId) {
  return request({
    url: `/api/metadata/catalog/table/${tableId}/fields`,
    method: 'get'
  })
}

// 获取表详情
export function getTableDetail(tableId) {
  return request({
    url: `/api/metadata/catalog/table/${tableId}`,
    method: 'get'
  })
}

// 获取字段详情
export function getFieldDetail(fieldId) {
  return request({
    url: `/api/metadata/catalog/field/${fieldId}`,
    method: 'get'
  })
}

// 添加目录节点
export function addCatalogNode(data) {
  console.log('调用添加目录节点API, 数据:', data)
  return request({
    url: '/api/metadata/catalog',
    method: 'post',
    data
  }).then(response => {
    console.log('添加节点原始响应:', response)
    return response
  })
}

// 更新目录节点
export function updateCatalogNode(data) {
  console.log('调用更新目录节点API, 数据:', data)
  return request({
    url: '/api/metadata/catalog',
    method: 'put',
    data
  }).then(response => {
    console.log('更新节点原始响应:', response)
    return response
  })
}

// 删除目录节点
export function deleteCatalogNode(id) {
  console.log('调用删除目录节点API, ID:', id)
  return request({
    url: `/api/metadata/catalog/${id}`,
    method: 'delete'
  }).then(response => {
    console.log('删除节点原始响应:', response)
    return response
  })
}

// 搜索目录节点
export function searchCatalog(keyword) {
  console.log('调用搜索目录节点API, 关键词:', keyword)
  return request({
    url: '/api/metadata/catalog/search',
    method: 'get',
    params: { keyword }
  }).then(response => {
    console.log('搜索节点原始响应:', response)
    return response
  })
}

// 获取节点详情
export function getCatalogNodeDetail(id) {
  console.log('调用获取节点详情API, ID:', id)
  return request({
    url: `/api/metadata/catalog/${id}`,
    method: 'get'
  }).then(response => {
    console.log('获取节点详情原始响应:', response)
    return response
  })
}

// 获取数据源类型的目录树
export function getDataSourceCatalogTree() {
  console.log('调用获取数据源目录树API')
  return request({
    url: '/api/metadata/catalog/datasource/tree',
    method: 'get'
  }).then(response => {
    console.log('数据源目录树原始响应:', response)
    return response
  })
} 