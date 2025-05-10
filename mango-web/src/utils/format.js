/**
 * 格式化字节大小
 * @param {number} bytes 字节数
 * @param {number} decimals 小数位数
 * @returns {string} 格式化后的字符串
 */
export function formatBytes(bytes, decimals = 2) {
  if (bytes === 0) return '0 B'

  const k = 1024
  const dm = decimals < 0 ? 0 : decimals
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB']

  const i = Math.floor(Math.log(bytes) / Math.log(k))

  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`
}

/**
 * 获取规则类型的中文名称
 * @param {string} type 规则类型代码
 * @returns {string} 规则类型的中文名称
 */
export function getRuleTypeName(type) {
  const typeMap = {
    'NAMING': '命名规范',
    'STRUCTURE': '结构规范',
    'DATA': '数据规范',
    'QUALITY': '质量规范',
    'DOMAIN': '主题域规范',
    'RESOURCE': '资源规范',
    'SECURITY': '安全规范',
    'PERFORMANCE': '性能规范',
    'COMPATIBILITY': '兼容性规范'
  };
  return typeMap[type] || '未知规范';
}

/**
 * 获取规则子类型的中文名称
 * @param {string} subType 规则子类型代码
 * @returns {string} 规则子类型的中文名称
 */
export function getRuleSubTypeName(subType) {
  if (!subType) return '未知';
  
  // 解析子类型代码，格式可能是 "TYPE_SUBTYPE" 或 "SUBTYPE"
  const parts = subType.split('_');
  const mainType = parts.length > 1 ? parts[0] : '';
  
  const subTypeMap = {
    // 通用命名规范子类型
    'TABLE': '表命名规范',
    'COLUMN': '字段命名规范',
    'PRIMARY_KEY': '主键命名规范',
    'FOREIGN_KEY': '外键命名规范',
    'INDEX': '索引命名规范',
    'CONSTRAINT': '约束命名规范',
    'SURROGATE_KEY': '代理键命名规范',
    'BUSINESS_KEY': '业务键命名规范',
    'MEASURE': '度量字段命名规范',
    'ITEM': '配置项命名规范',
    'KEY': '键名命名规范',
    'VIEW': '视图命名规范',
    'ALIAS': '别名命名规范',
    
    // 结构规范子类型
    'PRIMARY_KEY_STRUCTURE': '主键设计规范',
    'FOREIGN_KEY_STRUCTURE': '外键设计规范',
    'COLUMN_STRUCTURE': '字段设计规范',
    'INDEX_STRUCTURE': '索引设计规范',
    'CONSTRAINT_STRUCTURE': '约束设计规范',
    'SURROGATE_KEY_STRUCTURE': '代理键设计规范',
    'BUSINESS_KEY_STRUCTURE': '业务键设计规范',
    'ATTRIBUTE': '维度属性规范',
    'HIERARCHY': '层次结构规范',
    'KEY_STRUCTURE': '事实表键设计规范',
    'MEASURE_STRUCTURE': '度量字段设计规范',
    'PARTITION': '分区设计规范',
    'TABLE_STRUCTURE': '表结构规范',
    'ITEM_STRUCTURE': '配置项设计规范',
    'DOMAIN': '值域设计规范',
    
    // 数据规范子类型
    'DATA_TYPE': '数据类型规范',
    'LENGTH': '数据长度规范',
    'PRECISION': '数据精度规范',
    'DEFAULT': '默认值规范',
    'NULL': '空值规范',
    
    // 质量规范子类型
    'COMPLETENESS': '完整性规范',
    'ACCURACY': '准确性规范',
    'CONSISTENCY': '一致性规范',
    'VALIDITY': '有效性规范',
    'TIMELINESS': '及时性规范',
    
    // 主题域规范子类型
    'DIVISION': '主题划分规范',
    'DEFINITION': '主题定义规范',
    'RELATION': '主题关系规范',
    
    // 变化规范子类型
    'TYPE': 'SCD类型选择规范',
    'HISTORY': '历史变更规范',
    'EFFECTIVE_TIME': '生效时间规范',
    'EXPIRY_TIME': '失效时间规范',
    
    // 关联规范子类型
    'DIMENSION': '维度关系规范',
    'FACT': '事实表关联规范',
    'HIERARCHY_RELATION': '层次关联规范',
    
    // 度量规范子类型
    'DEFINITION_MEASURE': '度量定义规范',
    'CALCULATION': '计算逻辑规范',
    'AGGREGATION': '聚合规则规范',
    'PRECISION_MEASURE': '精度要求规范',
    
    // 性能规范子类型
    'PARTITION_PERFORMANCE': '分区策略规范',
    'INDEX_PERFORMANCE': '索引设计规范',
    'COMPRESSION': '压缩策略规范',
    'QUERY': '查询优化规范',
    'MATERIALIZED': '物化视图规范',
    'REFRESH': '刷新策略规范',
    
    // 生命周期规范子类型
    'RETENTION': '保留期限规范',
    'CLEANUP': '清理策略规范',
    'ALERT': '告警机制规范',
    
    // 资源规范子类型
    'STORAGE': '存储空间规范',
    'CONCURRENCY': '并发控制规范',
    
    // 安全规范子类型
    'ACCESS': '访问控制规范',
    'MODIFY': '修改权限规范',
    'ENCRYPTION': '加密存储规范',
    
    // 版本规范子类型
    'CONTROL': '版本控制规范',
    'CHANGE_LOG': '变更记录规范',
    'ROLLBACK': '回滚机制规范',
    
    // 设计规范子类型
    'LOGIC': '视图逻辑规范',
    'MAPPING': '字段映射规范',
    'JOIN': '连接设计规范',
    
    // 依赖规范子类型
    'BASE_TABLE': '基表依赖规范',
    'VIEW_DEPENDENCY': '视图依赖规范',
    'REFRESH_ORDER': '刷新顺序规范'
  };
  
  // 处理特殊情况，如 TABLE_NAMING 应该返回 "表命名规范"
  if (subType.includes('_')) {
    const specificMapping = {
      'TABLE_NAMING': '表命名规范',
      'COLUMN_NAMING': '字段命名规范',
      'PRIMARY_KEY_NAMING': '主键命名规范',
      'FOREIGN_KEY_NAMING': '外键命名规范',
      'INDEX_NAMING': '索引命名规范',
      'CONSTRAINT_NAMING': '约束命名规范',
      'VIEW_NAMING': '视图命名规范',
      'SURROGATE_KEY_NAMING': '代理键命名规范',
      'BUSINESS_KEY_NAMING': '业务键命名规范',
      'MEASURE_NAMING': '度量字段命名规范',
      'ITEM_NAMING': '配置项命名规范',
      'KEY_NAMING': '键名命名规范',
      'ALIAS_NAMING': '别名命名规范'
    };
    
    if (specificMapping[subType]) {
      return specificMapping[subType];
    }
  }
  
  // 如果是复合类型（如 TABLE_NAMING），尝试获取第二部分的映射
  if (parts.length > 1) {
    const secondPart = parts.slice(1).join('_');
    if (subTypeMap[secondPart]) {
      return subTypeMap[secondPart];
    }
  }
  
  // 尝试直接获取完整子类型的映射
  if (subType === 'TABLE_NAMING') return '表命名规范';
  if (subType === 'COLUMN_NAMING') return '字段命名规范';
  return subTypeMap[subType] || subType;
} 