import { getTableFields2 } from './tableFieldsHelper';
import { getTableFields } from '@/api/database';

// 保存当前的上下文，以便获取字段时使用
const fieldContext = {
  sourceTable: null,
  targetTable: null,
  // 缓存字段信息以便重用
  fieldCache: new Map()
};

/**
 * 设置源表信息
 * @param {Object} tableInfo 包含id和dataSourceId的表信息
 */
export const setSourceTable = (tableInfo) => {
  if (tableInfo) {
    fieldContext.sourceTable = tableInfo;
    console.log('设置源表信息:', tableInfo);
  }
};

/**
 * 设置目标表信息并立即加载字段
 * @param {Object} tableInfo 包含id和dataSourceId的表信息
 * @param {Function} callback 加载完成后的回调函数，接收字段数组作为参数
 * @returns {Promise<Array>} 表字段数组的Promise
 */
export const setTargetTable = (tableInfo, callback) => {
  if (tableInfo) {
    fieldContext.targetTable = tableInfo;
    console.log('设置目标表信息:', tableInfo);
    
    // 立即加载字段信息
    if (tableInfo.dataSourceId && (tableInfo.name || tableInfo.tableName)) {
      return getTableFieldsSync(tableInfo).then(fields => {
        if (callback && typeof callback === 'function') {
          callback(fields);
        }
        return fields;
      });
    }
  }
  return Promise.resolve([]);
};

/**
 * 同步获取表字段（返回Promise但会尽快解析）
 * @param {Object} tableInfo 表信息对象
 * @returns {Promise<Array>} 表字段数组的Promise
 */
export const getTableFieldsSync = async (tableInfo) => {
  if (!tableInfo) {
    console.warn('getTableFieldsSync: 未提供表信息');
    return [];
  }
  
  const tableName = tableInfo.name || tableInfo.tableName;
  const dataSourceId = tableInfo.dataSourceId;
  
  if (!dataSourceId || !tableName) {
    console.warn('getTableFieldsSync: 缺少必要参数');
    return [];
  }
  
  // 生成缓存键
  const cacheKey = `${dataSourceId}:${tableName}`;
  
  // 如果缓存中有数据，直接返回
  if (fieldContext.fieldCache.has(cacheKey)) {
    console.log(`使用缓存的表字段数据: ${tableName}`);
    return fieldContext.fieldCache.get(cacheKey);
  }
  
  console.log(`直接调用API获取表字段: ${tableName}, 数据源ID: ${dataSourceId}`);
  try {
    // 直接调用API获取字段
    const response = await getTableFields({
      dataSourceId,
      tableName
    });
    
    if (response && response.code === 0 && response.data) {
      // 格式化字段数据
      const fields = response.data.map(field => ({
        name: field.name || field.columnName || field.field_name,
        type: field.type || field.dataType || field.field_type,
        isPrimary: field.isPrimary || field.primaryKey || field.is_primary || false,
        description: field.comment || field.description || field.remarks || '',
        nullable: field.nullable !== false && field.is_nullable !== false
      }));
      
      // 缓存结果
      fieldContext.fieldCache.set(cacheKey, fields);
      console.log(`成功获取并缓存表 ${tableName} 的 ${fields.length} 个字段`);
      return fields;
    }
  } catch (error) {
    console.error(`获取表 ${tableName} 字段失败:`, error);
  }
  
  // 失败后使用getTableFields2作为备选方案
  const mockedFields = getMockTableFieldsLocal(tableName);
  if (mockedFields && mockedFields.length > 0) {
    // 缓存模拟数据
    fieldContext.fieldCache.set(cacheKey, mockedFields);
    return mockedFields;
  }
  
  return getTableFields2(tableInfo, fieldContext);
};

/**
 * 获取模拟表字段数据（本地实现）
 * @private
 * @param {string} tableId 表ID或表名
 * @returns {Array} 表字段数组
 */
const getMockTableFieldsLocal = (tableId) => {
  if (!tableId) {
    console.warn('getMockTableFieldsLocal: 未提供tableId');
    return [];
  }

  // 表名字符串，支持多种tableId格式
  const processTableId = (id) => {
    if (typeof id !== 'string') {
      return String(id).toLowerCase();
    }
    
    // 如果是schema.name格式，提取name部分
    if (id.includes('.')) {
      return id.split('.').pop().toLowerCase();
    }
    
    return id.toLowerCase();
  };
  
  const tableIdStr = processTableId(tableId);
  console.log(`获取模拟表字段: ${tableIdStr}`);
  
  // 用户基础信息表
  if (tableIdStr.includes('user') || tableIdStr.includes('用户')) {
    return [
      { name: 'id', type: 'bigint', isPrimary: true },
      { name: 'name', type: 'varchar(64)', description: '用户名' },
      { name: 'email', type: 'varchar(128)', description: '邮箱' },
      { name: 'phone', type: 'varchar(20)', description: '电话' },
      { name: 'status', type: 'tinyint', description: '状态' },
      { name: 'created_at', type: 'timestamp', description: '创建时间' },
      { name: 'updated_at', type: 'timestamp', description: '更新时间' }
    ];
  }
  
  // 订单表
  if (tableIdStr.includes('order') || tableIdStr.includes('订单')) {
    return [
      { name: 'order_id', type: 'bigint', isPrimary: true },
      { name: 'user_id', type: 'bigint', description: '用户ID' },
      { name: 'order_no', type: 'varchar(64)', description: '订单号' },
      { name: 'amount', type: 'decimal(10,2)', description: '订单金额' },
      { name: 'status', type: 'tinyint', description: '订单状态' },
      { name: 'payment_method', type: 'varchar(32)', description: '支付方式' },
      { name: 'address_id', type: 'bigint', description: '地址ID' },
      { name: 'created_at', type: 'timestamp', description: '创建时间' },
      { name: 'pay_time', type: 'timestamp', description: '支付时间' }
    ];
  }
  
  // 产品表
  if (tableIdStr.includes('product') || tableIdStr.includes('商品')) {
    return [
      { name: 'product_id', type: 'bigint', isPrimary: true },
      { name: 'name', type: 'varchar(128)', description: '产品名称' },
      { name: 'price', type: 'decimal(10,2)', description: '价格' },
      { name: 'stock', type: 'int', description: '库存' },
      { name: 'category_id', type: 'int', description: '类别ID' },
      { name: 'description', type: 'text', description: '描述' },
      { name: 'status', type: 'tinyint', description: '状态' },
      { name: 'created_at', type: 'timestamp', description: '创建时间' },
      { name: 'updated_at', type: 'timestamp', description: '更新时间' }
    ];
  }
  
  // 任务日志表
  if (tableIdStr.includes('task_log') || tableIdStr.includes('日志')) {
    return [
      { name: 'log_id', type: 'bigint', isPrimary: true },
      { name: 'task_id', type: 'bigint', description: '任务ID' },
      { name: 'type', type: 'varchar(32)', description: '日志类型' },
      { name: 'content', type: 'text', description: '日志内容' },
      { name: 'status', type: 'tinyint', description: '状态' },
      { name: 'create_time', type: 'timestamp', description: '创建时间' }
    ];
  }
  
  // 系统配置表
  if (tableIdStr.includes('config') || tableIdStr.includes('配置')) {
    return [
      { name: 'id', type: 'int', isPrimary: true },
      { name: 'key', type: 'varchar(64)', description: '配置键' },
      { name: 'value', type: 'varchar(512)', description: '配置值' },
      { name: 'description', type: 'varchar(128)', description: '配置描述' },
      { name: 'update_time', type: 'timestamp', description: '更新时间' }
    ];
  }
  
  // 默认表结构
  console.warn(`未找到表 ${tableId} 的字段定义，返回通用字段结构`);
  return [
    { name: 'id', type: 'bigint', isPrimary: true },
    { name: 'name', type: 'varchar(128)', description: '名称' },
    { name: 'description', type: 'text', description: '描述' },
    { name: 'status', type: 'tinyint', description: '状态' },
    { name: 'created_by', type: 'bigint', description: '创建人' },
    { name: 'created_at', type: 'timestamp', description: '创建时间' },
    { name: 'updated_at', type: 'timestamp', description: '更新时间' }
  ];
};

/**
 * 获取表字段的包装函数，集成真实API和模拟数据
 * @param {Object|string} tableId 表ID或表对象
 * @param {boolean} isTarget 是否是目标表
 * @returns {Array} 表字段数组（同步模式下初始返回[]，异步加载后更新）
 */
export const getMockTableFields = (tableId, isTarget = false) => {
  if (!tableId) {
    console.warn('getMockTableFields: 未提供tableId');
    return [];
  }

  // 立即返回空数组，并启动异步加载
  const result = [];
  
  // 处理tableId参数，确保能够获取到正确的表信息
  let tableInfo = tableId;
  let tableName;
  let dataSourceId;
  
  // 判断tableId的类型和内容
  if (typeof tableId === 'string') {
    // 如果是字符串ID，尝试从上下文获取完整信息
    tableInfo = isTarget ? fieldContext.targetTable : fieldContext.sourceTable;
    tableName = tableId;
    
    if (!tableInfo && (!dataSourceId || !tableName)) {
      console.log('无法从上下文获取表信息，使用模拟数据');
      // 使用本地模拟数据
      const mockedFields = getMockTableFieldsLocal(tableId);
      result.push(...mockedFields);
      return result;
    }
    
    if (tableInfo) {
      dataSourceId = tableInfo.dataSourceId;
      tableName = tableInfo.name || tableInfo.tableName || tableId;
    }
  } else if (tableId && typeof tableId === 'object') {
    // 如果是对象，直接获取属性
    dataSourceId = tableId.dataSourceId;
    tableName = tableId.name || tableId.tableName;
  }
  
  // 如果有dataSourceId，尝试调用真实API
  if (dataSourceId && tableName) {
    // 生成缓存键
    const cacheKey = `${dataSourceId}:${tableName}`;
    
    // 首先检查缓存
    if (fieldContext.fieldCache.has(cacheKey)) {
      console.log(`使用缓存中的字段数据: ${tableName}`);
      const cachedFields = fieldContext.fieldCache.get(cacheKey);
      result.push(...cachedFields);
      return result;
    }
    
    // 异步调用真实API获取字段
    console.log(`异步调用真实API获取表字段: ${tableName}, 数据源ID: ${dataSourceId}`);
    getTableFields({
      dataSourceId,
      tableName
    }).then(response => {
      if (response && response.code === 0 && response.data) {
        // 格式化字段数据
        const fields = response.data.map(field => ({
          name: field.name || field.columnName || field.field_name,
          type: field.type || field.dataType || field.field_type,
          isPrimary: field.isPrimary || field.primaryKey || field.is_primary || false,
          description: field.comment || field.description || field.remarks || '',
          nullable: field.nullable !== false && field.is_nullable !== false
        }));
        
        // 缓存结果
        fieldContext.fieldCache.set(cacheKey, fields);
        
        // 动态更新result数组，这样Vue的响应式系统可以捕获到变化
        result.length = 0;
        result.push(...fields);
        
        console.log(`成功获取到 ${tableName} 的 ${fields.length} 个字段`);
      } else {
        console.warn('API返回无效的字段数据，使用模拟数据');
        // 使用模拟数据
        const mockedFields = getMockTableFieldsLocal(tableName);
        // 缓存结果
        fieldContext.fieldCache.set(cacheKey, mockedFields);
        // 更新响应式数组
        result.length = 0;
        result.push(...mockedFields);
      }
    }).catch(error => {
      console.error(`API调用失败: ${error.message || '未知错误'}，使用模拟数据`);
      // 使用模拟数据
      const mockedFields = getMockTableFieldsLocal(tableName);
      // 缓存结果
      if (dataSourceId && tableName) {
        fieldContext.fieldCache.set(cacheKey, mockedFields);
      }
      // 更新响应式数组
      result.length = 0;
      result.push(...mockedFields);
    });
  } else {
    // 无法获取dataSourceId，直接使用模拟数据
    console.log('缺少dataSourceId或tableName，使用模拟数据');
    const mockedFields = getMockTableFieldsLocal(tableName || tableId);
    result.push(...mockedFields);
  }
  
  return result;
};

// 导出一个初始化函数，用于在组件挂载时调用
export const initTableFields = async (sourceTable, targetTable) => {
  if (sourceTable) {
    setSourceTable(sourceTable);
    // 预加载源表字段
    await getTableFieldsSync(sourceTable);
  }
  
  if (targetTable) {
    setTargetTable(targetTable);
    // 预加载目标表字段
    await getTableFieldsSync(targetTable);
  }
}; 