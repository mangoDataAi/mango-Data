package com.mango.test.database.adapter;

import com.mango.test.database.entity.Table;
import com.mango.test.database.entity.TableField;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 表信息适配器
 * 用于处理表对象的各种操作，提供字段获取、主键获取等方法
 */
public class TableAdapter {
    
    // 缓存表名，避免重复获取
    private static final Map<String, String> TABLE_NAME_CACHE = new ConcurrentHashMap<>();
    
    // 缓存表的字段列表
    private static final Map<String, List<TableField>> TABLE_FIELDS_CACHE = new ConcurrentHashMap<>();
    
    // 缓存表的主键字段
    private static final Map<String, List<TableField>> PRIMARY_KEYS_CACHE = new ConcurrentHashMap<>();
    
    /**
     * 获取表名
     * @param table 表对象
     * @return 表名
     */
    public static String getTableName(Table table) {
        if (table == null) {
            return null;
        }
        
        String tableId = table.getId();
        if (tableId != null && TABLE_NAME_CACHE.containsKey(tableId)) {
            return TABLE_NAME_CACHE.get(tableId);
        }
        
        String tableName = table.getName();
        if (tableId != null && tableName != null) {
            TABLE_NAME_CACHE.put(tableId, tableName);
        }
        
        return tableName;
    }
    
    /**
     * 设置表名缓存
     * @param tableId 表ID
     * @param tableName 表名
     */
    public static void setTableName(String tableId, String tableName) {
        if (tableId != null && tableName != null) {
            TABLE_NAME_CACHE.put(tableId, tableName);
        }
    }
    
    /**
     * 获取表的所有字段
     * @param table 表对象
     * @return 字段列表
     */
    public static List<TableField> getFields(Table table) {
        if (table == null) {
            return new ArrayList<>();
        }
        
        String tableId = table.getId();
        if (tableId != null && TABLE_FIELDS_CACHE.containsKey(tableId)) {
            return TABLE_FIELDS_CACHE.get(tableId);
        }
        
        // 这里需要从外部获取字段，但由于没有实际存储，返回空列表
        // 实际使用时，应该从数据库或其他数据源获取字段列表
        List<TableField> fields = new ArrayList<>();
        
        if (tableId != null) {
            TABLE_FIELDS_CACHE.put(tableId, fields);
        }
        
        return fields;
    }
    
    /**
     * 设置表的字段缓存
     * @param tableId 表ID
     * @param fields 字段列表
     */
    public static void setFields(String tableId, List<TableField> fields) {
        if (tableId != null) {
            TABLE_FIELDS_CACHE.put(tableId, fields);
        }
    }
    
    /**
     * 获取表的主键字段
     * @param table 表对象
     * @return 主键字段列表
     */
    public static List<TableField> getPrimaryKeys(Table table) {
        if (table == null) {
            return new ArrayList<>();
        }
        
        String tableId = table.getId();
        if (tableId != null && PRIMARY_KEYS_CACHE.containsKey(tableId)) {
            return PRIMARY_KEYS_CACHE.get(tableId);
        }
        
        // 从字段列表中筛选主键字段
        List<TableField> allFields = getFields(table);
        List<TableField> primaryKeys = allFields.stream()
                .filter(field -> field.getIsPrimary() != null && field.getIsPrimary() == 1)
                .collect(Collectors.toList());
        
        if (tableId != null) {
            PRIMARY_KEYS_CACHE.put(tableId, primaryKeys);
        }
        
        return primaryKeys;
    }
    
    /**
     * 设置表的主键字段缓存
     * @param tableId 表ID
     * @param primaryKeys 主键字段列表
     */
    public static void setPrimaryKeys(String tableId, List<TableField> primaryKeys) {
        if (tableId != null) {
            PRIMARY_KEYS_CACHE.put(tableId, primaryKeys);
        }
    }
    
    /**
     * 清除表的所有缓存
     * @param tableId 表ID
     */
    public static void clearCache(String tableId) {
        if (tableId != null) {
            TABLE_NAME_CACHE.remove(tableId);
            TABLE_FIELDS_CACHE.remove(tableId);
            PRIMARY_KEYS_CACHE.remove(tableId);
        }
    }
    
    /**
     * 获取表schema
     * @param table 表对象
     * @return schema名称，如果表或数据源为null则返回空字符串
     */
    public static String getSchema(Table table) {
        if (table == null) {
            return "";
        }
        
        if (table.getDatasource() != null && table.getDatasource().getSchema() != null) {
            return table.getDatasource().getSchema();
        }
        
        return "";
    }

} 