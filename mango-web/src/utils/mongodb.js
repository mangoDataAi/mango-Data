/**
 * MongoDB ObjectId处理工具
 */

/**
 * 从MongoDB文档中提取ID字符串
 * 支持多种可能的ID格式
 * @param {Object} doc MongoDB文档对象
 * @returns {String|null} ID字符串，如果无法提取则返回null
 */
export function extractIdFromDocument(doc) {
  if (!doc) return null;
  
  // 如果直接传入了ID (对象或字符串)
  if (typeof doc === 'string') return doc;
  
  // 文档没有_id字段
  if (!doc._id) {
    // 尝试使用id字段
    if (doc.id) return String(doc.id);
    return null;
  }
  
  // _id是字符串
  if (typeof doc._id === 'string') {
    return doc._id;
  }
  
  // _id是数字
  if (typeof doc._id === 'number') {
    return String(doc._id);
  }
  
  // _id是对象
  if (typeof doc._id === 'object') {
    // MongoDB扩展JSON格式 { $oid: "xxx" }
    if (doc._id.$oid) {
      return doc._id.$oid;
    }
    
    // 处理 {timestamp: xxx, date: "xxx"} 格式
    if (doc._id.timestamp) {
      console.log('在extractIdFromDocument中处理timestamp格式:', doc._id);
      return String(doc._id.timestamp);
    }
    
    // 嵌套的id属性
    if (doc._id.id) {
      return String(doc._id.id);
    }
    
    // 使用toString方法
    if (typeof doc._id.toString === 'function') {
      const idStr = doc._id.toString();
      if (idStr && idStr !== '[object Object]') {
        return idStr;
      }
    }
    
    // 遍历对象属性查找可能的ID值
    for (const key in doc._id) {
      if (typeof doc._id[key] === 'string' || typeof doc._id[key] === 'number') {
        if (key !== 'toString' && key !== 'date') {
          return String(doc._id[key]);
        }
      }
    }
  }
  
  return null;
}

/**
 * 将ID转换为MongoDB扩展JSON格式
 * @param {String|Object} id 字符串ID或ObjectId对象
 * @returns {Object} 格式为 { $oid: "xxx" } 的对象
 */
export function formatObjectId(id) {
  if (!id) return null;
  
  if (typeof id === 'string') {
    return { $oid: id };
  }
  
  if (typeof id === 'object') {
    if (id.$oid) return id;
    
    if (typeof id.toString === 'function') {
      const idStr = id.toString();
      if (idStr && idStr !== '[object Object]') {
        return { $oid: idStr };
      }
    }
  }
  
  return null;
}

/**
 * 检查是否为有效的MongoDB ObjectId格式
 * @param {String} id 要检查的ID字符串
 * @returns {Boolean} 是否是有效的ObjectId
 */
export function isValidObjectId(id) {
  if (!id || typeof id !== 'string') return false;
  
  // MongoDB ObjectId是24位十六进制字符串
  return /^[0-9a-fA-F]{24}$/.test(id);
}

export default {
  extractIdFromDocument,
  formatObjectId,
  isValidObjectId
}; 