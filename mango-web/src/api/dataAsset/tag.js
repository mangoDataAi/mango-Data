import axios from 'axios';

// 标签分类相关API
export function getCategoryTree() {
  return axios.get('/api/dataasset/tag/category/tree');
}

export function addCategory(data) {
  return axios.post('/api/dataasset/tag/category', data);
}

export function updateCategory(data) {
  return axios.put('/api/dataasset/tag/category', data);
}

export function deleteCategory(id) {
  return axios.delete(`/api/dataasset/tag/category/${id}`);
}

export function listCategories() {
  return axios.get('/api/dataasset/tag/category/list');
}

// 标签集相关API
export function listTagSetsByCategory(categoryId, keyword) {
  return axios.get('/api/dataasset/tag/tagset/list', {
    params: { categoryId, keyword }
  });
}

export function addTagSet(data) {
  console.log('添加标签集，提交数据:', JSON.stringify(data));
  return axios.post('/api/dataasset/tag/tagset', data)
    .catch(error => {
      console.error('添加标签集API错误:', error.response?.data || error.message);
      throw error;
    });
}

export function updateTagSet(data) {
  console.log('更新标签集，提交数据:', JSON.stringify(data));
  return axios.put('/api/dataasset/tag/tagset', data)
    .catch(error => {
      console.error('更新标签集API错误:', error.response?.data || error.message);
      throw error;
    });
}

export function deleteTagSet(id) {
  return axios.delete(`/api/dataasset/tag/tagset/${id}`);
}

export function getTagSetDetail(id) {
  return axios.get(`/api/dataasset/tag/tagset/${id}`);
}

export function listAllTagSets() {
  return axios.get('/api/dataasset/tag/tagset/all');
}

// 标签相关API
export function pageTagList(params) {
  return axios.get('/api/dataasset/tag/list', { params });
}

export function addTag(data) {
  return axios.post('/api/dataasset/tag', data);
}

export function updateTag(data) {
  return axios.put('/api/dataasset/tag', data);
}

export function deleteTag(id) {
  return axios.delete(`/api/dataasset/tag/${id}`);
}

export function batchDeleteTags(ids) {
  return axios.delete('/api/dataasset/tag/batch', {
    data: ids
  });
}

export function getTagDetail(id) {
  return axios.get(`/api/dataasset/tag/${id}`);
}

export function getTagStatistics(tagSetId) {
  return axios.get('/api/dataasset/tag/statistics', {
    params: { tagSetId }
  });
}

// 标签树相关API
export function getTagTree() {
  return axios.get('/api/dataasset/tag/tree').then(response => {
    console.log('标签树API原始响应:', response);
    return response;
  });
} 