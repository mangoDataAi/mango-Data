import { getTableFields } from '@/api/database'

/**
 * 从真实API获取表字段信息
 * @param {string} dataSourceId 数据源ID
 * @param {string} tableName 表名
 * @returns {Promise<Array|null>} 表字段数组或null（出错时）
 */
export const fetchRealTableFields = async (dataSourceId, tableName) => {
  if (!dataSourceId || !tableName) {
    console.warn('fetchRealTableFields: 缺少必要参数:', { dataSourceId, tableName });
    return null;
  }

  try {
    console.log(`调用API获取表 ${tableName} 的字段信息，数据源ID: ${dataSourceId}`);
    const response = await getTableFields({
      dataSourceId,
      tableName
    });

    if (response && response.code === 0 && response.data) {
      console.log(`从API成功获取到表 ${tableName} 的字段信息:`, response.data);
      
      // 格式化API返回的字段数据为统一格式
      return response.data.map(field => ({
        name: field.name || field.columnName || field.field_name,
        type: field.type || field.dataType || field.field_type,
        isPrimary: field.isPrimary || field.primaryKey || field.is_primary || false,
        description: field.comment || field.description || field.remarks || '',
        nullable: field.nullable !== false && field.is_nullable !== false
      }));
    } else {
      console.warn(`API返回错误: ${response?.message || '未知错误'}`, response);
      return null;
    }
  } catch (error) {
    console.error(`调用API获取表 ${tableName} 的字段信息失败:`, error);
    return null;
  }
};

/**
 * 获取模拟表字段数据
 * @param {string} tableName 表名
 * @returns {Array} 表字段数组
 */
export const getMockFieldsByTableName = (tableName) => {
  // 标准化表名以用于匹配模拟数据
  const normalizedTableName = tableName ? tableName.toLowerCase() : '';
  
  // 用户基础信息表
  if (normalizedTableName.includes('user') || normalizedTableName.includes('用户')) {
    return [
      { name: 'id', type: 'bigint', isPrimary: true },
      { name: 'name', type: 'varchar(64)', description: '用户名' },
      { name: 'email', type: 'varchar(128)', description: '邮箱' },
      { name: 'phone', type: 'varchar(20)', description: '电话' },
      { name: 'status', type: 'tinyint', description: '状态' },
      { name: 'created_at', type: 'timestamp', description: '创建时间' },
      { name: 'updated_at', type: 'timestamp', description: '更新时间' }
    ]
  }
  
  // 任务日志表
  if (normalizedTableName.includes('task_log') || normalizedTableName.includes('日志')) {
    return [
      { name: 'log_id', type: 'bigint', isPrimary: true },
      { name: 'task_id', type: 'bigint', description: '任务ID' },
      { name: 'type', type: 'varchar(32)', description: '日志类型' },
      { name: 'content', type: 'text', description: '日志内容' },
      { name: 'status', type: 'tinyint', description: '状态' },
      { name: 'create_time', type: 'timestamp', description: '创建时间' }
    ]
  }
  
  // 订单表
  if (normalizedTableName.includes('order') || normalizedTableName.includes('订单')) {
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
    ]
  }
  
  // 产品表
  if (normalizedTableName.includes('product') || normalizedTableName.includes('商品')) {
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
    ]
  }
  
  // 系统配置表
  if (normalizedTableName.includes('config') || normalizedTableName.includes('配置')) {
    return [
      { name: 'id', type: 'int', isPrimary: true },
      { name: 'key', type: 'varchar(64)', description: '配置键' },
      { name: 'value', type: 'varchar(512)', description: '配置值' },
      { name: 'description', type: 'varchar(128)', description: '配置描述' },
      { name: 'update_time', type: 'timestamp', description: '更新时间' }
    ]
  }
  
  // 默认表结构
  console.warn(`未找到表 ${tableName} 的字段定义，返回通用字段结构`);
  return [
    { name: 'id', type: 'bigint', isPrimary: true },
    { name: 'name', type: 'varchar(128)', description: '名称' },
    { name: 'description', type: 'text', description: '描述' },
    { name: 'status', type: 'tinyint', description: '状态' },
    { name: 'created_by', type: 'bigint', description: '创建人' },
    { name: 'created_at', type: 'timestamp', description: '创建时间' },
    { name: 'updated_at', type: 'timestamp', description: '更新时间' }
  ]
};

/**
 * 提取表信息
 * @param {Object|string} tableId 表ID（对象或字符串）
 * @returns {Object} 包含表名和数据源ID的对象
 */
export const extractTableInfo = (tableId) => {
  let result = {
    tableName: null,
    dataSourceId: null,
    schemaName: null
  };

  if (typeof tableId === 'object') {
    // 从对象中提取信息
    result.tableName = tableId.name || tableId.tableName;
    result.dataSourceId = tableId.dataSourceId;
    result.schemaName = tableId.schema;
  } else if (typeof tableId === 'string') {
    // 处理字符串格式
    if (tableId.includes('.')) {
      const parts = tableId.split('.');
      result.schemaName = parts[0];
      result.tableName = parts[1];
    } else {
      result.tableName = tableId;
    }
  }

  return result;
};

/**
 * 获取表字段信息（先尝试API，然后降级到模拟数据）
 * @param {Object|string} tableId 表ID或表对象
 * @param {Object} context 上下文对象，包含source和target表信息
 * @returns {Promise<Array>} 表字段数组
 */
export const getTableFields2 = async (tableId, context = {}) => {
  if (!tableId) {
    console.warn('getTableFields2: 未提供tableId');
    return [];
  }

  // 提取表信息
  const tableInfo = extractTableInfo(tableId);
  let { tableName, dataSourceId } = tableInfo;

  // 如果没有从tableId中获取到dataSourceId，从context中尝试获取
  if (!dataSourceId && context.sourceTable && tableId === context.sourceTable.id) {
    dataSourceId = context.sourceTable.dataSourceId;
  } else if (!dataSourceId && context.targetTable && tableId === context.targetTable.id) {
    dataSourceId = context.targetTable.dataSourceId;
  }

  console.log(`获取表 ${tableName} 的字段信息, 数据源ID: ${dataSourceId || '未知'}`);
  
  // 如果有数据源ID和表名，尝试使用真实API获取
  if (dataSourceId && tableName) {
    try {
      const apiFields = await fetchRealTableFields(dataSourceId, tableName);
      if (apiFields && apiFields.length > 0) {
        return apiFields;
      }
    } catch (apiError) {
      console.error('通过API获取字段失败，将使用模拟数据:', apiError);
    }
  }
  
  // 使用模拟数据作为后备
  console.log(`使用模拟数据获取表 ${tableName} 的字段信息`);
  return getMockFieldsByTableName(tableName);
}; 