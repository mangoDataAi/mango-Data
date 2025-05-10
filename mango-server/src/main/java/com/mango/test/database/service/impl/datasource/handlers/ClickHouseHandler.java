package com.mango.test.database.service.impl.datasource.handlers;

import com.mango.test.database.entity.ColumnDefinition;
import com.mango.test.database.entity.DataSource;
import com.mango.test.database.entity.IndexDefinition;
import com.mango.test.database.entity.TableDefinition;
import com.mango.test.database.service.impl.datasource.AbstractDatabaseHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ClickHouseHandler extends AbstractDatabaseHandler {

    private static final String DRIVER_CLASS = "com.clickhouse.jdbc.ClickHouseDriver";
    private static final String DEFAULT_PORT = "8123";
    private static final String URL_TEMPLATE = "jdbc:clickhouse://%s:%s/%s";

    private static final int DEFAULT_VARCHAR_LENGTH = 255;
    private static final int MAX_VARCHAR_LENGTH = Integer.MAX_VALUE;

    private static final Map<String, String> TYPE_MAPPING = new HashMap<>();
    private static final Map<String, Map<String, String>> COMMON_TYPE_MAPPING = new HashMap<>();

    static {
        // 基本类型映射
        TYPE_MAPPING.put("string", "String");
        TYPE_MAPPING.put("text", "String");
        TYPE_MAPPING.put("mediumtext", "String");
        TYPE_MAPPING.put("longtext", "String");
        TYPE_MAPPING.put("char", "FixedString");
        
        // 数值类型
        TYPE_MAPPING.put("tinyint", "Int8");
        TYPE_MAPPING.put("smallint", "Int16");
        TYPE_MAPPING.put("int", "Int32");
        TYPE_MAPPING.put("integer", "Int32");
        TYPE_MAPPING.put("bigint", "Int64");
        TYPE_MAPPING.put("float", "Float32");
        TYPE_MAPPING.put("double", "Float64");
        TYPE_MAPPING.put("decimal", "Decimal");
        
        // 日期时间类型
        TYPE_MAPPING.put("date", "Date");
        TYPE_MAPPING.put("datetime", "DateTime");
        TYPE_MAPPING.put("timestamp", "DateTime");
        
        // 其他类型
        TYPE_MAPPING.put("boolean", "UInt8");
        TYPE_MAPPING.put("binary", "String");
        TYPE_MAPPING.put("blob", "String");
        
        // 从其他数据库类型的映射
        // MySQL类型映射
        Map<String, String> mysqlMapping = new HashMap<>();
        mysqlMapping.put("VARCHAR", "String");
        mysqlMapping.put("CHAR", "FixedString");
        mysqlMapping.put("TEXT", "String");
        mysqlMapping.put("TINYINT", "Int8");
        mysqlMapping.put("SMALLINT", "Int16");
        mysqlMapping.put("INT", "Int32");
        mysqlMapping.put("BIGINT", "Int64");
        mysqlMapping.put("FLOAT", "Float32");
        mysqlMapping.put("DOUBLE", "Float64");
        mysqlMapping.put("DECIMAL", "Decimal");
        mysqlMapping.put("DATE", "Date");
        mysqlMapping.put("DATETIME", "DateTime");
        mysqlMapping.put("TIMESTAMP", "DateTime");
        mysqlMapping.put("BOOLEAN", "UInt8");
        mysqlMapping.put("BLOB", "String");
        COMMON_TYPE_MAPPING.put("mysql", mysqlMapping);
        
        // Oracle类型映射
        Map<String, String> oracleMapping = new HashMap<>();
        oracleMapping.put("VARCHAR2", "String");
        oracleMapping.put("NVARCHAR2", "String");
        oracleMapping.put("CHAR", "FixedString");
        oracleMapping.put("NUMBER", "Decimal");
        oracleMapping.put("DATE", "DateTime");
        oracleMapping.put("TIMESTAMP", "DateTime");
        oracleMapping.put("CLOB", "String");
        oracleMapping.put("BLOB", "String");
        COMMON_TYPE_MAPPING.put("oracle", oracleMapping);
        
        // PostgreSQL类型映射
        Map<String, String> postgresMapping = new HashMap<>();
        postgresMapping.put("VARCHAR", "String");
        postgresMapping.put("TEXT", "String");
        postgresMapping.put("INTEGER", "Int32");
        postgresMapping.put("BIGINT", "Int64");
        postgresMapping.put("NUMERIC", "Decimal");
        postgresMapping.put("TIMESTAMP", "DateTime");
        postgresMapping.put("BYTEA", "String");
        COMMON_TYPE_MAPPING.put("postgresql", postgresMapping);
    }

    public ClickHouseHandler(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public String getDriverClassName() {
        return DRIVER_CLASS;
    }

    @Override
    public String getDefaultPort() {
        return DEFAULT_PORT;
    }

    @Override
    public String getJdbcUrl() {
        return String.format(URL_TEMPLATE,
            dataSource.getHost(),
            dataSource.getPort() != null ? dataSource.getPort() : getDefaultPort(),
            dataSource.getDbName()
        );
    }

    @Override
    public String getDatabaseProductName() {
        return "ClickHouse";
    }

    @Override
    public String getValidationQuery() {
        return "SELECT 1";
    }

    @Override
    public String getQuoteString() {
        return "`";
    }

    @Override
    public List<String> getSystemDatabases() {
        return Arrays.asList("system", "information_schema", "default");
    }

    @Override
    public String generatePageSql(String sql, int offset, int limit) {
        // ClickHouse 支持两种分页语法：
        // 1. LIMIT offset, limit (MySQL风格)
        // 2. LIMIT limit OFFSET offset (SQL标准)
        // 推荐使用第二种，更清晰且兼容性更好
        return sql + " LIMIT " + limit + " OFFSET " + offset;
    }

    /**
     * 生成ClickHouse的LIMIT子句
     *
     * @param offset 起始位置
     * @param limit  返回记录的最大数量
     * @return 格式化后的LIMIT子句
     */
    @Override
    public String getLimitClause(long offset, int limit) {
        // ClickHouse 支持两种分页语法，这里使用SQL标准语法
        return "LIMIT " + limit + " OFFSET " + offset;
    }

    @Override
    public String generateCountSql(String sql) {
        return "SELECT count() FROM (" + sql + ") as t";
    }

    @Override
    public boolean supportsBatchUpdates() {
        return true;
    }

    @Override
    public boolean supportsGetGeneratedKeys() {
        return false;
    }

    @Override
    public boolean supportsTransactions() {
        return false;
    }

    @Override
    public String getAddIndexSql(String tableName, String indexName, List<String> columns, boolean unique) {
        StringBuilder columnStr = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) columnStr.append(", ");
            columnStr.append(escapeIdentifier(columns.get(i)));
        }
        
        // ClickHouse 索引语法，改用 minmax 或 set 类型根据需求
        // GRANULARITY 值可以调整以优化性能
        String indexType = "minmax";
        int granularity = 8; // 默认值通常为8
        
        return "ALTER TABLE " + escapeIdentifier(tableName) + 
               " ADD INDEX " + escapeIdentifier(indexName) + 
               " (" + columnStr.toString() + ") TYPE " + indexType + 
               " GRANULARITY " + granularity;
    }

    @Override
    public String getDropIndexSql(String tableName, String indexName) {
        // ClickHouse 删除索引语法
        return "ALTER TABLE " + escapeIdentifier(tableName) + 
               " DROP INDEX " + escapeIdentifier(indexName);
    }

    @Override
    public String getCreateIndexSql(String tableName, String indexName, String[] columns) {
        // ClickHouse 创建索引语法
        StringBuilder columnStr = new StringBuilder();
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) columnStr.append(", ");
            columnStr.append(escapeIdentifier(columns[i]));
        }
        
        return "ALTER TABLE " + escapeIdentifier(tableName) + 
               " ADD INDEX " + escapeIdentifier(indexName) + " (" + columnStr.toString() + ") TYPE minmax GRANULARITY 1";
    }

    public String escapeIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
        return getQuoteString() + identifier + getQuoteString();
    }

    @Override
    public String getShowIndexesSql(String tableName) {
        // ClickHouse的查看索引语法
        return String.format("SELECT name as index_name, expr as column_name, type " +
                           "FROM system.data_skipping_indices " +
                           "WHERE table = '%s' AND database = currentDatabase()",
            tableName
        );
    }

    @Override
    public String getSchema() {
        return dataSource.getDbName();
    }

    @Override
    public void setSchema(Connection conn, String schema) throws SQLException {
        if (schema != null && !schema.isEmpty()) {
            conn.setSchema(schema);
        }
    }

    @Override
    public String getDefaultSchema() {
        return "default";
    }

    @Override
    public String wrapValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? "1" : "0";
        }
        if (value instanceof java.sql.Date) {
            // ClickHouse 日期函数调用方式更正
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return "toDate('" + sdf.format((java.sql.Date)value) + "')";
        }
        if (value instanceof java.sql.Timestamp || value instanceof Date) {
            // ClickHouse 日期时间函数调用方式更正
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return "toDateTime('" + sdf.format(value) + "')";
        }
        return "'" + value.toString().replace("'", "''") + "'";
    }

    @Override
    public String wrapIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
        return getQuoteString() + identifier.replace(getQuoteString(), getQuoteString() + getQuoteString()) + getQuoteString();
    }

    @Override
    public String getTableExistsSql(String tableName) {
        return String.format(
            "SELECT count() FROM system.tables " +
            "WHERE database = currentDatabase() AND name = '%s'",
            tableName
        );
    }

    @Override
    public String getDropTableSql(String tableName) {
        return "DROP TABLE IF EXISTS " + wrapIdentifier(tableName);
    }

    @Override
    public String getTruncateTableSql(String tableName) {
        return "TRUNCATE TABLE " + wrapIdentifier(tableName);
    }

    @Override
    public String getRenameTableSql(String oldTableName, String newTableName) {
        return "RENAME TABLE " + wrapIdentifier(oldTableName) + " TO " + wrapIdentifier(newTableName);
    }

    @Override
    public String getShowCreateTableSql(String tableName) {
        return "SHOW CREATE TABLE " + escapeIdentifier(tableName);
    }

    @Override
    public String getShowTablesSql() {
        return "SHOW TABLES";
    }

    @Override
    public String getShowColumnsSql(String tableName) {
        return "DESCRIBE " + wrapIdentifier(tableName);
    }

    @Override
    public String getAlterTableEngineSql(String tableName, String engine) {
        return "ALTER TABLE " + wrapIdentifier(tableName) + " MODIFY ENGINE = " + engine;
    }

    @Override
    public String getAlterTableCharsetSql(String tableName, String charset, String collate) {
        // ClickHouse 不支持修改表字符集
        return "";
    }

    @Override
    public String getAddTableCommentSql(String tableName, String comment) {
        // ClickHouse 不支持单独的 COMMENT ON TABLE 语法
        // 需要在 ALTER TABLE 时通过 MODIFY COMMENT 来实现
        return "-- ClickHouse 不支持单独的 COMMENT ON TABLE 语法，需要重建表或通过其他方式处理";
    }

    @Override
    public String getModifyTableCommentSql(String tableName, String comment) {
        // ClickHouse 不支持单独修改表注释
        return "-- ClickHouse 不支持单独的 COMMENT ON TABLE 语法，需要重建表或通过其他方式处理";
    }

    @Override
    public String getAddColumnCommentSql(String tableName, String columnName, String comment) {
        // ClickHouse 需要通过 ALTER TABLE MODIFY COLUMN 来添加注释
        return "ALTER TABLE " + escapeIdentifier(tableName) + 
               " MODIFY COLUMN " + escapeIdentifier(columnName) + 
               " COMMENT " + wrapValue(comment);
    }

    @Override
    public String getModifyColumnCommentSql(String tableName, String columnName, String comment) {
        // 在 ClickHouse 中，需要完整指定列定义才能修改注释
        // 这里只给出框架，实际实现需要先获取列的当前类型等信息
        return "ALTER TABLE " + escapeIdentifier(tableName) + 
               " MODIFY COLUMN " + escapeIdentifier(columnName) + 
               " COMMENT " + wrapValue(comment);
    }

    @Override
    public String getDefaultTextType() {
        return "String";
    }

    @Override
    public String getDefaultIntegerType() {
        return "Int32";
    }

    @Override
    public String getDefaultDecimalType() {
        return "Decimal(10,2)";
    }

    @Override
    public String getDefaultDateType() {
        return "Date";
    }

    @Override
    public String getDefaultTimeType() {
        return "DateTime";
    }

    @Override
    public String getDefaultDateTimeType() {
        return "DateTime";
    }

    @Override
    public String getDefaultBooleanType() {
        return "UInt8";
    }

    @Override
    public String getDefaultBlobType() {
        return "String";
    }

    @Override
    public int getDefaultVarcharLength() {
        return DEFAULT_VARCHAR_LENGTH;
    }

    @Override
    public int getMaxVarcharLength() {
        return MAX_VARCHAR_LENGTH;
    }

    @Override
    public String getCreateTableSql(String tableName, List<ColumnDefinition> columns, 
                                  String tableComment, String engine, String charset, String collate) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(wrapIdentifier(tableName)).append(" (\n");
        
        // 添加列定义
        for (int i = 0; i < columns.size(); i++) {
            ColumnDefinition column = columns.get(i);
            if (i > 0) {
                sql.append(",\n");
            }
            
            sql.append("    ").append(wrapIdentifier(column.getName())).append(" ");
            
            // 处理类型定义
            String type = column.getType();
            StringBuilder typeDefinition = new StringBuilder();
            
            if (column.getLength() != null && isValidFieldLength(type, column.getLength())) {
                if (column.getScale() != null) {
                    // Decimal 类型
                    typeDefinition.append(type).append("(")
                       .append(column.getPrecision() != null ? column.getPrecision() : column.getLength())
                       .append(",").append(column.getScale()).append(")");
                } else if (!type.equalsIgnoreCase("String")) {
                    // 带长度的类型（非 String 类型）
                    typeDefinition.append(type).append("(").append(column.getLength()).append(")");
                } else {
                    // String 类型不需要长度
                    typeDefinition.append(type);
                }
            } else {
                typeDefinition.append(type);
            }
            
            // 可空性 - ClickHouse 中使用 Nullable 作为包装类型
            if (column.isNullable()) {
                sql.append("Nullable(").append(typeDefinition).append(")");
            } else {
                sql.append(typeDefinition);
            }
            
            // 默认值
            if (column.getDefaultValue() != null && !column.getDefaultValue().isEmpty()) {
                sql.append(" DEFAULT ").append(column.getDefaultValue());
            }
            
            // 列注释
            if (column.getComment() != null && !column.getComment().isEmpty()) {
                sql.append(" COMMENT ").append(wrapValue(column.getComment()));
            }
        }
        
        // 主键定义
        List<String> primaryKeys = new ArrayList<>();
        for (ColumnDefinition column : columns) {
            if (column.isPrimaryKey()) {
                primaryKeys.add(column.getName());
            }
        }
        
        if (!primaryKeys.isEmpty()) {
            sql.append(",\n    PRIMARY KEY (");
            for (int i = 0; i < primaryKeys.size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append(wrapIdentifier(primaryKeys.get(i)));
            }
            sql.append(")");
        }
        
        sql.append("\n)");
        
        // 引擎
        if (engine == null || engine.isEmpty()) {
            // 如果有主键，使用 MergeTree
            if (!primaryKeys.isEmpty()) {
                engine = "MergeTree()";
            } else {
                // 无主键时，根据需求选择合适的引擎
                // TinyLog 轻量，适合小表
                // Log 稍重，有更多特性
                // StripeLog 存储结构不同
                engine = "MergeTree()"; // 即使没有主键，通常也推荐使用 MergeTree
            }
        }
        sql.append(" ENGINE = ").append(engine);
        
        // 排序键 (如果未在引擎中指定)
        if (!primaryKeys.isEmpty() && !engine.contains("ORDER BY")) {
            // ClickHouse ORDER BY 不需要括号包围单列，但多列时需要
            if (primaryKeys.size() == 1) {
                sql.append(" ORDER BY ").append(wrapIdentifier(primaryKeys.get(0)));
            } else {
                sql.append(" ORDER BY (");
                for (int i = 0; i < primaryKeys.size(); i++) {
                    if (i > 0) {
                        sql.append(", ");
                    }
                    sql.append(wrapIdentifier(primaryKeys.get(i)));
                }
                sql.append(")");
            }
        }
        
        // 表注释
        if (tableComment != null && !tableComment.isEmpty()) {
            sql.append(" COMMENT ").append(wrapValue(tableComment));
        }
        
        return sql.toString();
    }

    @Override
    public Map<String, String> getTypeMapping() {
        return TYPE_MAPPING;
    }

    @Override
    public Map<String, Map<String, String>> getCommonTypeMapping() {
        return COMMON_TYPE_MAPPING;
    }

    @Override
    public String convertFromOtherDbType(String sourceType, String sourceDbType) {
        // 如果源类型为空，返回默认类型
        if (sourceType == null || sourceType.trim().isEmpty()) {
            return "String";
        }

        // 统一转换为大写进行匹配
        sourceType = sourceType.toUpperCase().trim();
        sourceDbType = sourceDbType.toLowerCase();

        // 获取对应数据库的类型映射
        Map<String, String> typeMapping = COMMON_TYPE_MAPPING.get(sourceDbType);
        if (typeMapping != null) {
            // 处理带有长度的类型，如 VARCHAR(255)
            String baseType = sourceType.contains("(") ?
                    sourceType.substring(0, sourceType.indexOf("(")) :
                    sourceType;

            String mappedType = typeMapping.get(baseType);
            if (mappedType != null) {
                // 如果原类型包含长度信息，且目标类型需要长度
                if (sourceType.contains("(") && !mappedType.equals("String")) {
                    String lengthPart = sourceType.substring(sourceType.indexOf("("));
                    // 对于某些特殊类型，可能需要调整长度
                    if (mappedType.equals("FixedString")) {
                        return mappedType + lengthPart;
                    }
                    // Decimal类型保留精度信息
                    if (mappedType.equals("Decimal") && lengthPart.contains(",")) {
                        return mappedType + lengthPart;
                    }
                }
                return mappedType;
            }
        }

        // 特殊类型处理
        if (sourceType.contains("CHAR")) {
            return "String";
        }
        if (sourceType.contains("TEXT")) {
            return "String";
        }
        if (sourceType.contains("INT")) {
            if (sourceType.startsWith("TINY")) return "Int8";
            if (sourceType.startsWith("SMALL")) return "Int16";
            if (sourceType.startsWith("BIG")) return "Int64";
            return "Int32";
        }
        if (sourceType.contains("FLOAT") || sourceType.contains("REAL")) {
            return "Float32";
        }
        if (sourceType.contains("DOUBLE")) {
            return "Float64";
        }
        if (sourceType.contains("DECIMAL") || sourceType.contains("NUMERIC")) {
            return "Decimal(10,2)";
        }
        if (sourceType.contains("BOOL")) {
            return "UInt8";
        }
        if (sourceType.contains("DATE")) {
            return "Date";
        }
        if (sourceType.contains("TIME")) {
            return "DateTime";
        }
        if (sourceType.contains("BLOB") || sourceType.contains("BINARY")) {
            return "String";
        }

        // 如果没有找到映射，返回默认类型
        return "String";
    }

    @Override
    public String formatFieldDefinition(String fieldName, String fieldType, 
            Integer length, Integer precision, Integer scale,
            boolean nullable, String defaultValue, String comment) {
            
        StringBuilder definition = new StringBuilder();
        definition.append(wrapIdentifier(fieldName)).append(" ");
        
        // 处理字段类型
        String type = fieldType.toUpperCase();
        
        // 构建基本类型字符串
        StringBuilder typeStr = new StringBuilder(type);
        
        // 添加长度/精度信息
        if (length != null) {
            if (scale != null) {
                // 对于 Decimal 类型
                typeStr.append("(")
                     .append(precision != null ? precision : length)
                     .append(",")
                     .append(scale)
                     .append(")");
            } else if (!type.equals("STRING") && !type.contains("INT") && 
                      !type.equals("FLOAT32") && !type.equals("FLOAT64") && 
                      !type.equals("DATE") && !type.equals("DATETIME")) {
                // 只有某些类型需要长度
                typeStr.append("(").append(length).append(")");
            }
        }
        
        // 生成完整的类型定义
        String finalType = typeStr.toString();
        
        // 处理可空性 - ClickHouse 中 Nullable 是一个包装类型
        if (nullable) {
            definition.append("Nullable(").append(finalType).append(")");
        } else {
            definition.append(finalType);
        }
        
        // 添加默认值 - 确保正确处理字符串和数值
        if (defaultValue != null && !defaultValue.isEmpty()) {
            if (defaultValue.equalsIgnoreCase("NULL")) {
                // NULL 默认值只能用于 Nullable 类型
                if (nullable) {
                    definition.append(" DEFAULT NULL");
                }
            } else if (defaultValue.toLowerCase().startsWith("current_timestamp") || 
                      defaultValue.toLowerCase().startsWith("now()")) {
                // 时间戳函数处理
                definition.append(" DEFAULT now()");
            } else {
                // 其他默认值处理
                definition.append(" DEFAULT ");
                
                // 数值类型不需要引号
                if (type.contains("INT") || type.equals("FLOAT32") || type.equals("FLOAT64") || 
                    type.equals("DECIMAL")) {
                    // 尝试解析数值，确保格式正确
                    try {
                        Double.parseDouble(defaultValue);
                        definition.append(defaultValue);
                    } catch (NumberFormatException e) {
                        // 如果不是有效数字，则添加引号
                        definition.append(wrapValue(defaultValue));
                    }
                } else {
                    // 字符串和其他类型
                    definition.append(wrapValue(defaultValue));
                }
            }
        }
        
        // 添加注释
        if (comment != null && !comment.isEmpty()) {
            definition.append(" COMMENT ").append(wrapValue(comment));
        }
        
        return definition.toString();
    }

    @Override
    public boolean isValidFieldLength(String type, int length) {
        if (type == null) {
            return false;
        }
        
        type = type.toUpperCase();
        
        // ClickHouse 的特殊处理
        if (type.equals("STRING")) {
            // ClickHouse 的 String 类型不需要长度限制
            return true;
        }
        
        if (type.equals("FIXEDSTRING")) {
            // FixedString 类型的长度限制
            return length > 0 && length <= 1000000;
        }
        
        if (type.contains("INT") || type.startsWith("UINT")) {
            // 整数类型不需要长度
            return true;
        }
        
        if (type.equals("DECIMAL")) {
            // Decimal 类型的精度限制
            return length > 0 && length <= 76;
        }
        
        if (type.equals("FLOAT32") || type.equals("FLOAT64")) {
            // 浮点类型不需要长度
            return true;
        }
        
        if (type.equals("DATE") || type.equals("DATETIME") || type.equals("DATETIME64")) {
            // 日期时间类型不需要长度
            return true;
        }
        
        if (type.startsWith("ARRAY") || type.startsWith("MAP") || type.startsWith("TUPLE")) {
            // 复合类型不需要长度
            return true;
        }
        
        // 其他类型默认验证
        return length > 0 && length <= MAX_VARCHAR_LENGTH;
    }

    @Override
    public String getAddColumnSql(String tableName, String columnDefinition) {
        return "ALTER TABLE " + wrapIdentifier(tableName) + " ADD COLUMN " + columnDefinition;
    }

    @Override
    public String getAddColumnSql(String tableName, ColumnDefinition column, String afterColumn) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(escapeIdentifier(tableName))
           .append(" ADD COLUMN ").append(escapeIdentifier(column.getName()))
           .append(" ");
        
        String type = column.getType();
        StringBuilder typeStr = new StringBuilder(type);
        
        // 处理类型及长度
        if (column.getLength() != null && isValidFieldLength(type, column.getLength())) {
            if (column.getScale() != null) {
                // Decimal 类型
                typeStr.append("(")
                      .append(column.getPrecision() != null ? column.getPrecision() : column.getLength())
                      .append(", ")
                      .append(column.getScale())
                      .append(")");
            } else if (!type.equalsIgnoreCase("String") && 
                     !type.equalsIgnoreCase("Date") && 
                     !type.equalsIgnoreCase("DateTime") && 
                     !type.contains("Int") && 
                     !type.contains("Float")) {
                // 其他需要长度的类型
                typeStr.append("(").append(column.getLength()).append(")");
            }
        }
        
        // ClickHouse 中 Nullable 是包装类型
        if (column.isNullable()) {
            sql.append("Nullable(").append(typeStr).append(")");
        } else {
            sql.append(typeStr);
        }
        
        // 默认值
        if (column.getDefaultValue() != null && !column.getDefaultValue().isEmpty()) {
            sql.append(" DEFAULT ").append(column.getDefaultValue());
        }
        
        // 注释
        if (column.getComment() != null && !column.getComment().isEmpty()) {
            sql.append(" COMMENT ").append(wrapValue(column.getComment()));
        }
        
        // AFTER 子句
        if (afterColumn != null && !afterColumn.isEmpty()) {
            sql.append(" AFTER ").append(escapeIdentifier(afterColumn));
        }
        
        return sql.toString();
    }

    @Override
    public String getModifyColumnSql(String tableName, String columnName, String newDefinition) {
        // ClickHouse 使用 ALTER TABLE ... MODIFY COLUMN 语法
        return "ALTER TABLE " + escapeIdentifier(tableName) + 
               " MODIFY COLUMN " + newDefinition;
    }

    @Override
    public String getDropColumnSql(String tableName, String columnName) {
        return "ALTER TABLE " + wrapIdentifier(tableName) + " DROP COLUMN " + wrapIdentifier(columnName);
    }

    @Override
    public String mapJavaTypeToDbType(Class<?> javaType) {
        if (String.class.equals(javaType)) {
            return "String";
        }
        if (Integer.class.equals(javaType) || int.class.equals(javaType)) {
            return "Int32";
        }
        if (Long.class.equals(javaType) || long.class.equals(javaType)) {
            return "Int64";
        }
        if (Double.class.equals(javaType) || double.class.equals(javaType)) {
            return "Float64";
        }
        if (Float.class.equals(javaType) || float.class.equals(javaType)) {
            return "Float32";
        }
        if (java.math.BigDecimal.class.equals(javaType)) {
            return "Decimal(20,4)";
        }
        if (Boolean.class.equals(javaType) || boolean.class.equals(javaType)) {
            return "UInt8";
        }
        if (Date.class.equals(javaType)) {
            return "DateTime";
        }
        if (byte[].class.equals(javaType)) {
            return "String";
        }
        
        if (javaType.isArray()) {
            // 为 Java 数组类型映射 ClickHouse 数组
            Class<?> componentType = javaType.getComponentType();
            String baseType = mapJavaTypeToDbType(componentType);
            return "Array(" + baseType + ")";
        }
        
        return "String";
    }

    @Override
    public Class<?> mapDbTypeToJavaType(String dbType) {
        dbType = dbType.toUpperCase();
        if (dbType.startsWith("STRING") || dbType.startsWith("FIXEDSTRING")) {
            return String.class;
        }
        if (dbType.startsWith("INT8") || dbType.startsWith("INT16") || dbType.startsWith("INT32") || 
            dbType.startsWith("UINT8") || dbType.startsWith("UINT16")) {
            return Integer.class;
        }
        if (dbType.startsWith("INT64") || dbType.startsWith("UINT32") || dbType.startsWith("UINT64")) {
            return Long.class;
        }
        if (dbType.startsWith("FLOAT32")) {
            return Float.class;
        }
        if (dbType.startsWith("FLOAT64")) {
            return Double.class;
        }
        if (dbType.startsWith("DECIMAL")) {
            return java.math.BigDecimal.class;
        }
        if (dbType.equals("DATE")) {
            return java.sql.Date.class;
        }
        if (dbType.startsWith("DATETIME")) {
            return java.sql.Timestamp.class;
        }
        if (dbType.startsWith("ARRAY")) {
            return Object[].class;
        }
        return String.class;
    }

    @Override
    public Map<String, Integer> getDefaultLengthMapping() {
        Map<String, Integer> lengthMapping = new HashMap<>();
        lengthMapping.put("String", DEFAULT_VARCHAR_LENGTH);
        lengthMapping.put("FixedString", 100);
        lengthMapping.put("Decimal", 10);
        return lengthMapping;
    }

    @Override
    public String convertCreateTableSql(String sourceSql, String sourceDbType) {
        try {
            // 解析源SQL
            TableDefinition tableDefinition = parseCreateTableSql(sourceSql);
            
            // 转换字段类型
            for (ColumnDefinition column : tableDefinition.getColumns()) {
                String sourceType = column.getType();
                String targetType = convertDataType(sourceType, sourceDbType, getDatabaseProductName());
                column.setType(targetType);
                
                adjustClickHouseColumnType(column);
            }
            
            // 移除不支持的属性
            tableDefinition.setCharset(null);
            tableDefinition.setCollate(null);
            
            // 生成ClickHouse建表语句
            return generateCreateTableSql(tableDefinition);
        } catch (Exception e) {
            log.error("Failed to convert CREATE TABLE SQL from {} to ClickHouse: {}", sourceDbType, e.getMessage());
            throw new RuntimeException("Failed to convert CREATE TABLE SQL", e);
        }
    }

    @Override
    public TableDefinition parseCreateTableSql(String createTableSql) {
        TableDefinition tableDefinition = new TableDefinition();
        List<ColumnDefinition> columns = new ArrayList<>();
        List<IndexDefinition> indexes = new ArrayList<>();

        try {
            // 提取表名
            Pattern tablePattern = Pattern.compile("CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([\\w\\.`]+)\\s*\\(", 
                Pattern.CASE_INSENSITIVE);
            Matcher tableMatcher = tablePattern.matcher(createTableSql);
            if (tableMatcher.find()) {
                String tableName = tableMatcher.group(1).replace("`", "");
                tableDefinition.setTableName(tableName);
            }

            // 提取列定义部分
            int startIndex = createTableSql.indexOf('(');
            int endIndex = createTableSql.lastIndexOf(')');
            if (startIndex == -1 || endIndex == -1) {
                throw new RuntimeException("Invalid CREATE TABLE SQL statement");
            }

            String columnsPart = createTableSql.substring(startIndex + 1, endIndex);
            String[] definitions = splitDefinitions(columnsPart);

            // 解析每个定义（列或约束）
            for (String definition : definitions) {
                definition = definition.trim();
                
                // 跳过空定义
                if (definition.isEmpty()) {
                    continue;
                }

                // 处理主键约束
                if (definition.toUpperCase().startsWith("PRIMARY KEY")) {
                    Pattern pkPattern = Pattern.compile("PRIMARY\\s+KEY\\s*\\(([^)]+)\\)", 
                        Pattern.CASE_INSENSITIVE);
                    Matcher pkMatcher = pkPattern.matcher(definition);
                    if (pkMatcher.find()) {
                        String[] pkColumns = pkMatcher.group(1).split(",");
                        for (String pkColumn : pkColumns) {
                            String columnName = pkColumn.trim().replace("`", "");
                            for (ColumnDefinition column : columns) {
                                if (column.getName().equalsIgnoreCase(columnName)) {
                                    column.setPrimaryKey(true);
                                    break;
                                }
                            }
                        }
                    }
                    continue;
                }

                // 处理普通列定义
                ColumnDefinition column = parseColumnDefinition(definition);
                if (column != null) {
                    columns.add(column);
                }
            }

            tableDefinition.setColumns(columns);
            tableDefinition.setIndexes(indexes);

            // 解析引擎
            Pattern enginePattern = Pattern.compile("ENGINE\\s*=\\s*(\\w+)", Pattern.CASE_INSENSITIVE);
            Matcher engineMatcher = enginePattern.matcher(createTableSql);
            if (engineMatcher.find()) {
                tableDefinition.setEngine(engineMatcher.group(1));
            }

            // 解析ORDER BY
            Pattern orderByPattern = Pattern.compile("ORDER\\s+BY\\s*\\(([^)]+)\\)", 
                Pattern.CASE_INSENSITIVE);
            Matcher orderByMatcher = orderByPattern.matcher(createTableSql);
            if (orderByMatcher.find()) {
                Map<String, String> extraProps = tableDefinition.getExtraProperties();
                if (extraProps == null) {
                    extraProps = new HashMap<>();
                    tableDefinition.setExtraProperties(extraProps);
                }
                extraProps.put("ORDER_BY", orderByMatcher.group(1));
            }

            // 解析PARTITION BY
            Pattern partitionByPattern = Pattern.compile("PARTITION\\s+BY\\s+([^\\s]+)", 
                Pattern.CASE_INSENSITIVE);
            Matcher partitionByMatcher = partitionByPattern.matcher(createTableSql);
            if (partitionByMatcher.find()) {
                Map<String, String> extraProps = tableDefinition.getExtraProperties();
                if (extraProps == null) {
                    extraProps = new HashMap<>();
                    tableDefinition.setExtraProperties(extraProps);
                }
                extraProps.put("PARTITION_BY", partitionByMatcher.group(1));
            }

            return tableDefinition;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CREATE TABLE SQL: " + e.getMessage(), e);
        }
    }

    private String[] splitDefinitions(String columnsPart) {
        List<String> definitions = new ArrayList<>();
        StringBuilder currentDefinition = new StringBuilder();
        int parenthesesCount = 0;
        boolean inQuotes = false;
        char[] chars = columnsPart.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            
            if (c == '(' && !inQuotes) {
                parenthesesCount++;
            } else if (c == ')' && !inQuotes) {
                parenthesesCount--;
            } else if (c == '\'' && (i == 0 || chars[i-1] != '\\')) {
                inQuotes = !inQuotes;
            }

            if (c == ',' && parenthesesCount == 0 && !inQuotes) {
                definitions.add(currentDefinition.toString().trim());
                currentDefinition = new StringBuilder();
            } else {
                currentDefinition.append(c);
            }
        }

        if (currentDefinition.length() > 0) {
            definitions.add(currentDefinition.toString().trim());
        }

        return definitions.toArray(new String[0]);
    }

    private ColumnDefinition parseColumnDefinition(String definition) {
        // 基本列信息的正则表达式
        Pattern columnPattern = Pattern.compile(
            "`?([\\w]+)`?\\s+" +                      // 列名
            "([\\w\\(\\),]+)" +                       // 数据类型
            "(?:\\s+DEFAULT\\s+([^\\s,]+))?" +       // 默认值（可选）
            "(?:\\s+ALIAS\\s+([^\\s,]+))?" +         // 别名（可选）
            "(?:\\s+MATERIALIZED\\s+([^\\s,]+))?" +  // 物化表达式（可选）
            "(?:\\s+NOT\\s+NULL)?",                  // NOT NULL（可选）
            Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = columnPattern.matcher(definition);
        if (!matcher.find()) {
            return null;
        }

        ColumnDefinition column = new ColumnDefinition();
        column.setName(matcher.group(1));

        // 解析类型和长度/精度
        String fullType = matcher.group(2);
        Pattern typePattern = Pattern.compile("([\\w]+)(?:\\((\\d+)(?:,(\\d+))?\\))?");
        Matcher typeMatcher = typePattern.matcher(fullType);
        if (typeMatcher.find()) {
            column.setType(typeMatcher.group(1));
            if (typeMatcher.group(2) != null) {
                if (typeMatcher.group(3) != null) {
                    // 有精度和小数位
                    column.setPrecision(Integer.parseInt(typeMatcher.group(2)));
                    column.setScale(Integer.parseInt(typeMatcher.group(3)));
                } else {
                    // 只有长度
                    column.setLength(Integer.parseInt(typeMatcher.group(2)));
                }
            }
        }

        // 设置默认值
        if (matcher.group(3) != null) {
            column.setDefaultValue(matcher.group(3));
        }

        // 设置是否可空
        column.setNullable(!definition.toUpperCase().contains("NOT NULL"));

        // 设置额外属性
        Map<String, String> extraProps = new HashMap<>();
        if (matcher.group(4) != null) {
            extraProps.put("ALIAS", matcher.group(4));
        }
        if (matcher.group(5) != null) {
            extraProps.put("MATERIALIZED", matcher.group(5));
        }
        if (!extraProps.isEmpty()) {
            column.setExtraProperties(extraProps);
        }

        return column;
    }

    @Override
    public String generateCreateTableSql(TableDefinition tableDefinition) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ");
        sql.append(wrapIdentifier(tableDefinition.getTableName()));
        sql.append(" (\n");

        List<String> columnDefinitions = new ArrayList<>();
        List<String> primaryKeys = new ArrayList<>();

        // 生成列定义
        for (ColumnDefinition column : tableDefinition.getColumns()) {
            StringBuilder columnSql = new StringBuilder();
            columnSql.append(wrapIdentifier(column.getName())).append(" ");
            
            String type = column.getType().toUpperCase();
            // ClickHouse特定类型映射
            switch (type) {
                case "VARCHAR":
                case "CHAR":
                    type = "String";
                    break;
                case "TEXT":
                    type = "String";
                    break;
                case "INTEGER":
                    type = "Int32";
                    break;
                case "BIGINT":
                    type = "Int64";
                    break;
                case "FLOAT":
                    type = "Float32";
                    break;
                case "DOUBLE":
                    type = "Float64";
                    break;
                case "DECIMAL":
                    if (column.getPrecision() != null && column.getScale() != null) {
                        type = String.format("Decimal(%d,%d)", 
                            column.getPrecision(), column.getScale());
                    } else {
                        type = "Decimal(18,2)";
                    }
                    break;
                case "BOOLEAN":
                    type = "UInt8";
                    break;
                case "DATE":
                    type = "Date";
                    break;
                case "TIMESTAMP":
                    type = "DateTime";
                    break;
            }
            columnSql.append(type);

            // 添加Nullable修饰符
            if (column.isNullable()) {
                columnSql.append(" Nullable(").append(type).append(")");
            }

            // 添加默认值
            if (column.getDefaultValue() != null) {
                columnSql.append(" DEFAULT ").append(column.getDefaultValue());
            }

            // 添加ALIAS或MATERIALIZED表达式
            if (column.getExtraProperties() != null) {
                if (column.getExtraProperties().containsKey("ALIAS")) {
                    columnSql.append(" ALIAS ").append(column.getExtraProperties().get("ALIAS"));
                }
                if (column.getExtraProperties().containsKey("MATERIALIZED")) {
                    columnSql.append(" MATERIALIZED ").append(column.getExtraProperties().get("MATERIALIZED"));
                }
            }

            // 收集主键列
            if (column.isPrimaryKey()) {
                primaryKeys.add(wrapIdentifier(column.getName()));
            }

            columnDefinitions.add(columnSql.toString());
        }

        sql.append(String.join(",\n", columnDefinitions));

        // 添加主键约束
        if (!primaryKeys.isEmpty()) {
            sql.append(",\nPRIMARY KEY (")
               .append(String.join(", ", primaryKeys))
               .append(")");
        }

        sql.append("\n)");

        // 添加引擎
        String engine = tableDefinition.getEngine();
        if (engine == null || engine.isEmpty()) {
            engine = "MergeTree()";
        }
        sql.append("\nENGINE = ").append(engine);

        // 添加ORDER BY
        Map<String, String> extraProps = tableDefinition.getExtraProperties();
        if (extraProps != null) {
            if (extraProps.containsKey("ORDER_BY")) {
                sql.append("\nORDER BY (")
                   .append(extraProps.get("ORDER_BY"))
                   .append(")");
            }

            // 添加PARTITION BY
            if (extraProps.containsKey("PARTITION_BY")) {
                sql.append("\nPARTITION BY ")
                   .append(extraProps.get("PARTITION_BY"));
            }
        }

        // 添加表注释
        if (tableDefinition.getTableComment() != null && !tableDefinition.getTableComment().isEmpty()) {
            sql.append("\nCOMMENT '").append(tableDefinition.getTableComment().replace("'", "''")).append("'");
        }

        return sql.toString();
    }

    private void adjustClickHouseColumnType(ColumnDefinition column) {
        String type = column.getType().toUpperCase();
        
        // ClickHouse不需要指定字符串长度
        if (type.contains("STRING") || type.contains("FIXED_STRING")) {
            column.setLength(null);
        }
        
        // 调整Decimal精度和小数位
        if (type.contains("DECIMAL")) {
            if (column.getPrecision() == null) {
                column.setPrecision(18);
            }
            if (column.getScale() == null) {
                column.setScale(2);
            }
            column.setPrecision(Math.min(column.getPrecision(), 38));
            column.setScale(Math.min(column.getScale(), column.getPrecision()));
        }
        
        // 处理日期时间类型
        if (type.contains("DATETIME64")) {
            if (column.getPrecision() == null) {
                column.setPrecision(3);
            }
            column.setPrecision(Math.min(column.getPrecision(), 9));
        }
    }

    @Override
    public List<TableDefinition> getAllTables(String database) throws Exception {
        List<TableDefinition> tables = new ArrayList<>();
        try (Connection conn = getConnection()) {
            String sql = String.format(
                "SELECT name, engine, create_table_query, total_rows, total_bytes " +
                    "FROM system.tables " +
                "WHERE database = '%s' " +
                "ORDER BY name",
                database
            );
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    TableDefinition table = TableDefinition.builder()
                        .tableName(rs.getString("name"))
                        .engine(rs.getString("engine"))
                        .build();
                    
                    // 存储创建表的SQL以及其他属性
                    Map<String, String> extraProperties = new HashMap<>();
                    extraProperties.put("createSql", rs.getString("create_table_query"));
                    extraProperties.put("rowCount", String.valueOf(rs.getLong("total_rows")));
                    extraProperties.put("sizeBytes", String.valueOf(rs.getLong("total_bytes")));
                    table.setExtraProperties(extraProperties);
                    
                    tables.add(table);
                }
            }
        } catch (Exception e) {
            log.error("获取表列表失败: database={}, error={}", database, e.getMessage(), e);
            throw e;
        }
        return tables;
    }

    @Override
    public List<ColumnDefinition> getTableColumns(String database, String tableName) throws Exception {
        List<ColumnDefinition> columns = new ArrayList<>();
        try (Connection conn = getConnection()) {
            // 首先检查这是否是一个系统表
            boolean isSystemTable = false;
            if (tableName.contains(".")) {
                String[] parts = tableName.split("\\.");
                if (parts.length > 1 && "system".equalsIgnoreCase(parts[0])) {
                    isSystemTable = true;
                    tableName = parts[1]; // 获取实际表名
                    database = "system";  // 设置数据库为system
                }
            }
            
            // 确保数据库名不为空
            if (database == null || database.trim().isEmpty()) {
                // 尝试从连接中获取当前数据库
                database = getCurrentDatabase(conn);
                log.info("数据库名为空，已自动设置为当前数据库: {}", database);
            }
            
            // 构建SQL语句
            String sql = String.format(
                "SELECT name, type, default_expression, comment, is_in_primary_key " +
                "FROM system.columns " +
                "WHERE database = '%s' AND table = '%s' " +
                "ORDER BY position",
                database, tableName
            );
            
            log.info("执行ClickHouse查询获取表结构: {}", sql);
            
            try (Statement stmt = conn.createStatement()) {
                log.info("开始执行查询...");
                ResultSet rs = stmt.executeQuery(sql);
                log.info("查询执行完成，获取ResultSet");
                
                // 检查ResultSet状态
                boolean hasResults = false;
                int rowCount = 0;
                
                try {
                    // 尝试提前检查是否有数据
                    hasResults = rs.isBeforeFirst();
                    log.info("ResultSet.isBeforeFirst(): {}", hasResults);
                } catch (Exception e) {
                    log.warn("无法检查ResultSet.isBeforeFirst(): {}", e.getMessage());
                }
                
                log.info("开始遍历ResultSet...");
                while (rs.next()) {
                    rowCount++;
                    log.info("处理第{}行数据", rowCount);
                    
                    String columnName = rs.getString("name");
                    String dataType = rs.getString("type");
                    String defaultValue = rs.getString("default_expression");
                    String comment = rs.getString("comment");
                    boolean isPrimaryKey = rs.getBoolean("is_in_primary_key");
                    
                    log.info("列信息: name={}, type={}, isPrimaryKey={}", columnName, dataType, isPrimaryKey);
                    
                    // 清理和简化数据类型
                    String cleanType = dataType;
                    String displayType = dataType;
                    boolean isNested = false;
                    boolean isArray = false;
                    
                    // 检查是否嵌套结构字段
                    if (columnName.contains(".")) {
                        isNested = true;
                        // 这可能是嵌套结构的一部分，如items.product_id
                        // 为UI显示优化类型描述
                        if (cleanType.contains("Array")) {
                            isArray = true;
                            // 提取数组内的元素类型
                            if (cleanType.startsWith("Array(") && cleanType.endsWith(")")) {
                                String elementType = cleanType.substring(6, cleanType.length() - 1);
                                if (elementType.startsWith("Nullable(")) {
                                    elementType = elementType.substring(9, elementType.length() - 1);
                                }
                                // 设置一个更友好的显示类型
                                displayType = "Array<" + elementType + ">";
                                
                                // 对UI层更友好的描述
                                Map<String, String> extraProps = new HashMap<>();
                                extraProps.put("clickhouse_type", dataType);
                                extraProps.put("display_type", displayType);
                                extraProps.put("is_nested", "true");
                                extraProps.put("is_array", "true");
                                extraProps.put("element_type", elementType);
                                
                                // 分离父字段和子字段名
                                String[] nameParts = columnName.split("\\.");
                                if (nameParts.length > 1) {
                                    extraProps.put("parent_field", nameParts[0]);
                                    extraProps.put("child_field", nameParts[1]);
                                }
                                
                                // 创建更友好的列定义
                                ColumnDefinition.ColumnDefinitionBuilder builder = ColumnDefinition.builder()
                                    .name(columnName)
                                    .type("Array") // 简化类型名称增强可读性
                                    .defaultValue(defaultValue)
                                    .comment(comment)
                                    .primaryKey(isPrimaryKey)
                                    .nullable(dataType.contains("Nullable"))
                                    .extraProperties(extraProps);
                                
                                columns.add(builder.build());
                                continue; // 跳过后续处理
                            }
                        }
                    }
                    
                    if (cleanType.startsWith("Nullable(")) {
                        cleanType = cleanType.substring(9, cleanType.length() - 1);
                    }
                    
                    // 检查是否为嵌套结构本身
                    if (cleanType.startsWith("Nested(")) {
                        // 这是一个Nested类型，需要特殊处理
                        isNested = true;
                        displayType = "Nested";
                        cleanType = "Nested";
                    }
                    
                    // 检查是否为数组类型
                    if (cleanType.startsWith("Array(")) {
                        isArray = true;
                        displayType = cleanType; // 保留完整类型信息
                        cleanType = "Array";     // 简化后的基本类型
                    }
                    
                    // 创建基本列定义
                    ColumnDefinition.ColumnDefinitionBuilder builder = ColumnDefinition.builder()
                        .name(columnName)
                        .type(cleanType)
                        .defaultValue(defaultValue)
                        .comment(comment)
                        .primaryKey(isPrimaryKey)
                        .nullable(dataType.contains("Nullable"));
                    
                    // 准备额外属性Map
                    Map<String, String> extraProps = new HashMap<>();
                    extraProps.put("clickhouse_type", dataType);
                    
                    if (isNested) {
                        extraProps.put("is_nested", "true");
                    }
                    
                    if (isArray) {
                        extraProps.put("is_array", "true");
                        
                        // 提取数组元素类型
                        if (displayType.startsWith("Array(") && displayType.endsWith(")")) {
                            String elementType = displayType.substring(6, displayType.length() - 1);
                            extraProps.put("element_type", elementType);
                        }
                    }
                    
                    // 解析类型信息（长度、精度、小数位数）
                    if (cleanType.contains("(")) {
                        int startPos = cleanType.indexOf("(");
                        int endPos = cleanType.indexOf(")");
                        if (startPos > 0 && endPos > startPos) {
                            // 提取基本类型（不包括括号内容）
                            String baseType = cleanType.substring(0, startPos);
                            builder.type(baseType);
                            
                            // 提取括号内的参数
                            String lengthPart = cleanType.substring(startPos + 1, endPos);
                            if (lengthPart.contains(",")) {
                                // 处理Decimal(p,s)格式
                                String[] parts = lengthPart.split(",");
                                try {
                                    builder.precision(Integer.parseInt(parts[0].trim()));
                                    builder.scale(Integer.parseInt(parts[1].trim()));
                                } catch (NumberFormatException e) {
                                    log.warn("解析Decimal精度和小数位失败: {}", lengthPart);
                                }
                            } else {
                                // 处理String(n)、FixedString(n)等格式
                                try {
                                    builder.length(Integer.parseInt(lengthPart.trim()));
                                } catch (NumberFormatException e) {
                                    log.warn("解析字段长度失败: {}", lengthPart);
                                }
                            }
                        }
                    }
                    
                    // 存储原始ClickHouse类型
                    extraProps.put("clickhouse_type", dataType);
                    
                    // 根据ClickHouse类型推断Java类型
                    String javaType = "";
                    if (cleanType.startsWith("Int") || cleanType.startsWith("UInt")) {
                        javaType = Integer.class.getName();
                    } else if (cleanType.startsWith("Float") || cleanType.startsWith("Double")) {
                        javaType = Double.class.getName();
                    } else if (cleanType.startsWith("String") || cleanType.startsWith("FixedString")) {
                        javaType = String.class.getName();
                    } else if (cleanType.startsWith("Date")) {
                        javaType = java.sql.Date.class.getName();
                    } else if (cleanType.startsWith("DateTime")) {
                        javaType = java.sql.Timestamp.class.getName();
                    } else if (cleanType.startsWith("Decimal")) {
                        javaType = java.math.BigDecimal.class.getName();
                    } else if (cleanType.equals("Bool") || cleanType.equals("Boolean")) {
                        javaType = Boolean.class.getName();
                    } else {
                        javaType = String.class.getName(); // 默认作为字符串处理
                    }
                    
                    // 将Java类型添加到额外属性中
                    extraProps.put("java_type", javaType);
                    
                    // 设置额外属性
                    builder.extraProperties(extraProps);
                    
                    columns.add(builder.build());
                }
                
                log.info("ResultSet遍历结束，处理了{}行数据", rowCount);
                
                // 如果没有行，尝试直接检查system.tables表确认表是否存在
                if (rowCount == 0) {
                    log.warn("没有找到表结构信息，尝试检查表 {}.{} 是否存在", database, tableName);
                    
                    try (Statement checkStmt = conn.createStatement();
                         ResultSet checkRs = checkStmt.executeQuery(
                             "SELECT count() FROM system.tables WHERE database = '" + database + 
                             "' AND name = '" + tableName + "'")) {
                        if (checkRs.next()) {
                            long tableCount = checkRs.getLong(1);
                            log.info("表 {}.{} 存在检查: {}", database, tableName, tableCount > 0 ? "存在" : "不存在");
                            
                            // 如果表确实存在，则尝试使用完整的表名格式
                            if (tableCount > 0) {
                                log.info("表存在但无法获取列信息，尝试使用完整表名");
                                String fullTableName = database + "." + tableName;
                                
                                try (Statement descStmt = conn.createStatement();
                                     ResultSet descRs = descStmt.executeQuery("DESCRIBE TABLE " + fullTableName)) {
                                    
                                    int descRows = 0;
                                    while (descRs.next()) {
                                        descRows++;
                                        String colName = descRs.getString("name");
                                        String colType = descRs.getString("type");
                                        log.info("DESCRIBE结果: 列名={}, 类型={}", colName, colType);
                                        
                                        // 创建列定义
                                        ColumnDefinition col = ColumnDefinition.builder()
                                            .name(colName)
                                            .type(colType)
                                            .nullable(colType.contains("Nullable"))
                                            .build();
                                        
                                        columns.add(col);
                                    }
                                    
                                    log.info("DESCRIBE命令返回了{}行结果", descRows);
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("检查表是否存在时出错: {}", e.getMessage());
                    }
                    
                    // 如果仍然没有获取到列信息，尝试使用DESCRIBE命令
                    if (columns.isEmpty()) {
                        log.info("尝试使用DESCRIBE命令获取表结构");
                        
                        // 确保使用正确的表名格式
                        String describeTable = database + "." + tableName;
                        
                        try (Statement descStmt = conn.createStatement();
                             ResultSet descRs = descStmt.executeQuery("DESCRIBE TABLE " + describeTable)) {
                            
                            int descRows = 0;
                            while (descRs.next()) {
                                descRows++;
                                String colName = descRs.getString("name");
                                String colType = descRs.getString("type");
                                log.info("DESCRIBE结果: 列名={}, 类型={}", colName, colType);
                                
                                // 创建列定义
                                ColumnDefinition col = ColumnDefinition.builder()
                                    .name(colName)
                                    .type(colType)
                                    .nullable(colType.contains("Nullable"))
                                    .build();
                                
                                columns.add(col);
                            }
                            
                            log.info("DESCRIBE命令返回了{}行结果", descRows);
                        } catch (Exception e) {
                            log.warn("DESCRIBE命令执行失败: {}", e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取列信息失败: database={}, table={}, error={}", database, tableName, e.getMessage(), e);
            throw new Exception("获取表结构失败: " + e.getMessage(), e);
        }
        
        log.info("最终获取到{}个列定义", columns.size());
        
        if (columns.isEmpty()) {
            log.warn("表 {}.{} 没有列信息或表不存在", database, tableName);
        }
        
        return columns;
    }
    
    /**
     * 获取当前连接的数据库名
     * @param conn 数据库连接
     * @return 当前数据库名
     */
    private String getCurrentDatabase(Connection conn) {
        // 首先尝试从dataSource获取
        if (dataSource.getDbName() != null && !dataSource.getDbName().trim().isEmpty()) {
            return dataSource.getDbName();
        }
        
        // 然后尝试从URL中提取
        String url = dataSource.getUrl();
        if (url != null && url.contains("/")) {
            int lastSlashIndex = url.lastIndexOf('/');
            if (lastSlashIndex < url.length() - 1) {
                String dbFromUrl = url.substring(lastSlashIndex + 1);
                // 去除可能的参数部分
                if (dbFromUrl.contains("?")) {
                    dbFromUrl = dbFromUrl.substring(0, dbFromUrl.indexOf("?"));
                }
                if (!dbFromUrl.trim().isEmpty()) {
                    return dbFromUrl;
                }
            }
        }
        
        // 尝试从JDBC连接中查询
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT currentDatabase()")) {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (Exception e) {
            log.warn("无法从连接中获取当前数据库: {}", e.getMessage());
        }
        
        // 如果都失败了，返回默认值
        return "default";
    }

    @Override
    public String getDatabaseType() {
        return "clickhouse";
    }

    @Override
    public String getDatabaseName() {
        return dataSource.getDbName();
    }

    @Override
    public List<String> listTables() throws Exception {
        List<String> tables = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW TABLES")) {
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        } catch (Exception e) {
            log.error("获取表列表失败: error={}", e.getMessage(), e);
            throw e;
        }
        return tables;
    }

    @Override
    public Map<String, Object> getTableInfo(String tableName) throws Exception {
        Map<String, Object> tableInfo = new HashMap<>();
        try (Connection conn = getConnection()) {
            // 获取表基本信息
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT engine, create_table_query FROM system.tables WHERE database = currentDatabase() AND name = '" + tableName + "'")) {
                if (rs.next()) {
                    tableInfo.put("tableName", tableName);
                    tableInfo.put("engine", rs.getString("engine"));
                    tableInfo.put("createTableQuery", rs.getString("create_table_query"));
                }
            }
            
            // 获取表行数
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT count() FROM " + wrapIdentifier(tableName))) {
                if (rs.next()) {
                    tableInfo.put("rowCount", rs.getLong(1));
                }
            } catch (Exception e) {
                log.warn("获取表行数失败: table={}, error={}", tableName, e.getMessage());
                tableInfo.put("rowCount", -1L);
            }
            
            // 获取表列数
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT count() FROM system.columns WHERE database = currentDatabase() AND table = '" + tableName + "'")) {
                if (rs.next()) {
                    tableInfo.put("columnCount", rs.getInt(1));
                }
            }
            
            // 获取表大小
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT sum(bytes) FROM system.parts WHERE database = currentDatabase() AND table = '" + tableName + "'")) {
                if (rs.next()) {
                    tableInfo.put("tableSize", rs.getLong(1));
                }
            } catch (Exception e) {
                log.warn("获取表大小失败: table={}, error={}", tableName, e.getMessage());
                tableInfo.put("tableSize", -1L);
            }
            
        } catch (Exception e) {
            log.error("获取表信息失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return tableInfo;
    }

    @Override
    public List<Map<String, Object>> listColumns(String tableName) throws Exception {
        List<Map<String, Object>> columns = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name, type, position, default_expression, comment, is_in_primary_key " +
                                             "FROM system.columns " +
                                             "WHERE database = currentDatabase() AND table = '" + tableName + "' " +
                                             "ORDER BY position")) {
            while (rs.next()) {
                Map<String, Object> column = new HashMap<>();
                column.put("columnName", rs.getString("name"));
                column.put("dataType", rs.getString("type"));
                column.put("position", rs.getInt("position"));
                column.put("defaultValue", rs.getString("default_expression"));
                column.put("comment", rs.getString("comment"));
                column.put("isPrimaryKey", rs.getBoolean("is_in_primary_key"));
                column.put("nullable", rs.getString("type").contains("Nullable"));
                columns.add(column);
            }
        } catch (Exception e) {
            log.error("获取列信息失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return columns;
    }

    @Override
    public Map<String, Object> getColumnInfo(String tableName, String columnName) throws Exception {
        Map<String, Object> columnInfo = null;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name, type, position, default_expression, comment, is_in_primary_key, " +
                                             "compression_codec, is_in_sorting_key " +
                                             "FROM system.columns " +
                                             "WHERE database = currentDatabase() AND table = '" + tableName + "' AND name = '" + columnName + "'")) {
            if (rs.next()) {
                columnInfo = new HashMap<>();
                columnInfo.put("columnName", rs.getString("name"));
                columnInfo.put("dataType", rs.getString("type"));
                columnInfo.put("position", rs.getInt("position"));
                columnInfo.put("defaultValue", rs.getString("default_expression"));
                columnInfo.put("comment", rs.getString("comment"));
                columnInfo.put("isPrimaryKey", rs.getBoolean("is_in_primary_key"));
                columnInfo.put("compressionCodec", rs.getString("compression_codec"));
                columnInfo.put("isSortingKey", rs.getBoolean("is_in_sorting_key"));
                columnInfo.put("nullable", rs.getString("type").contains("Nullable"));
                
                // 解析数据类型以获取更多信息
                String dataType = rs.getString("type");
                if (dataType != null) {
                    if (dataType.contains("(")) {
                        String typeBase = dataType.substring(0, dataType.indexOf("("));
                        String params = dataType.substring(dataType.indexOf("(") + 1, dataType.indexOf(")"));
                        columnInfo.put("typeBase", typeBase);
                        
                        if (params.contains(",")) {
                            String[] parts = params.split(",");
                            columnInfo.put("precision", Integer.parseInt(parts[0].trim()));
                            columnInfo.put("scale", Integer.parseInt(parts[1].trim()));
                        } else {
                            columnInfo.put("length", Integer.parseInt(params.trim()));
                        }
                    } else {
                        columnInfo.put("typeBase", dataType);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取列详细信息失败: table={}, column={}, error={}", tableName, columnName, e.getMessage(), e);
            throw e;
        }
        
        if (columnInfo == null) {
            throw new Exception("Column not found: " + columnName);
        }
        
        return columnInfo;
    }

    @Override
    public String getTableEngine(String tableName) throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT engine FROM system.tables WHERE database = currentDatabase() AND name = '" + tableName + "'")) {
            if (rs.next()) {
                return rs.getString("engine");
            }
        } catch (Exception e) {
            log.error("获取表引擎失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return null;
    }

    @Override
    public String getTableCharset(String tableName) throws Exception {
        // ClickHouse不直接支持表级别的字符集设置，返回默认值
        return "UTF-8";
    }

    @Override
    public String getTableCollation(String tableName) throws Exception {
        // ClickHouse不直接支持表级别的排序规则设置，返回默认值
        return "binary";
    }

    @Override
    public Long getTableSize(String tableName) throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT sum(bytes) FROM system.parts WHERE database = currentDatabase() AND table = '" + tableName + "'")) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (Exception e) {
            log.error("获取表大小失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return -1L;
    }

    @Override
    public Long getTableRowCount(String tableName) throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT count() FROM " + wrapIdentifier(tableName))) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (Exception e) {
            log.error("获取表行数失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return -1L;
    }

    @Override
    public String getTableSpace(String tableName) throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT path FROM system.parts WHERE database = currentDatabase() AND table = '" + tableName + "' LIMIT 1")) {
            if (rs.next()) {
                return rs.getString("path");
            }
        } catch (Exception e) {
            log.error("获取表空间信息失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return null;
    }

    @Override
    public Integer getCharacterLength(String tableName, String columnName) throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT type FROM system.columns " +
                                             "WHERE database = currentDatabase() AND table = '" + tableName + "' " +
                                             "AND name = '" + columnName + "' " +
                                             "AND type LIKE '%String%' OR type LIKE '%FixedString%'")) {
            if (rs.next()) {
                String dataType = rs.getString("type");
                if (dataType != null) {
                    if (dataType.contains("FixedString")) {
                        int startIndex = dataType.indexOf("(");
                        int endIndex = dataType.indexOf(")");
                        if (startIndex > 0 && endIndex > startIndex) {
                            String lengthStr = dataType.substring(startIndex + 1, endIndex);
                            try {
                                return Integer.parseInt(lengthStr);
                            } catch (NumberFormatException e) {
                                log.warn("解析字符长度失败: table={}, column={}, type={}", tableName, columnName, dataType);
                            }
                        }
                    } else if (dataType.contains("String")) {
                        // ClickHouse的String类型没有固定长度限制
                        return Integer.MAX_VALUE;
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取字符长度失败: table={}, column={}, error={}", tableName, columnName, e.getMessage(), e);
            throw e;
        }
        return null;
    }

    @Override
    public Integer getNumericPrecision(String tableName, String columnName) throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT type FROM system.columns " +
                                             "WHERE database = currentDatabase() AND table = '" + tableName + "' " +
                                             "AND name = '" + columnName + "'")) {
            if (rs.next()) {
                String dataType = rs.getString("type");
                if (dataType != null) {
                    if (dataType.contains("Decimal")) {
                        int startIndex = dataType.indexOf("(");
                        int endIndex = dataType.indexOf(",");
                        if (startIndex > 0 && endIndex > startIndex) {
                            String precisionStr = dataType.substring(startIndex + 1, endIndex);
                            try {
                                return Integer.parseInt(precisionStr.trim());
                            } catch (NumberFormatException e) {
                                log.warn("解析数值精度失败: table={}, column={}, type={}", tableName, columnName, dataType);
                            }
                        }
                    } else if (dataType.contains("Int8")) {
                        return 3;
                    } else if (dataType.contains("Int16") || dataType.contains("UInt8")) {
                        return 5;
                    } else if (dataType.contains("Int32") || dataType.contains("UInt16")) {
                        return 10;
                    } else if (dataType.contains("Int64") || dataType.contains("UInt32")) {
                        return 19;
                    } else if (dataType.contains("UInt64")) {
                        return 20;
                    } else if (dataType.contains("Float32")) {
                        return 7;
                    } else if (dataType.contains("Float64")) {
                        return 15;
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取数值精度失败: table={}, column={}, error={}", tableName, columnName, e.getMessage(), e);
            throw e;
        }
        return null;
    }

    @Override
    public Integer getNumericScale(String tableName, String columnName) throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT type FROM system.columns " +
                                             "WHERE database = currentDatabase() AND table = '" + tableName + "' " +
                                             "AND name = '" + columnName + "'")) {
            if (rs.next()) {
                String dataType = rs.getString("type");
                if (dataType != null) {
                    if (dataType.contains("Decimal")) {
                        int commaIndex = dataType.indexOf(",");
                        int endIndex = dataType.indexOf(")");
                        if (commaIndex > 0 && endIndex > commaIndex) {
                            String scaleStr = dataType.substring(commaIndex + 1, endIndex);
                            try {
                                return Integer.parseInt(scaleStr.trim());
                            } catch (NumberFormatException e) {
                                log.warn("解析小数位数失败: table={}, column={}, type={}", tableName, columnName, dataType);
                            }
                        }
                    } else if (dataType.contains("Float32")) {
                        return 7;
                    } else if (dataType.contains("Float64")) {
                        return 15;
                    } else if (dataType.matches(".*Int.*")) {
                        return 0;
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取小数位数失败: table={}, column={}, error={}", tableName, columnName, e.getMessage(), e);
            throw e;
        }
        return null;
    }

    @Override
    public String getColumnDefault(String tableName, String columnName) throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT default_expression FROM system.columns " +
                                             "WHERE database = currentDatabase() AND table = '" + tableName + "' " +
                                             "AND name = '" + columnName + "'")) {
            if (rs.next()) {
                return rs.getString("default_expression");
            }
        } catch (Exception e) {
            log.error("获取默认值失败: table={}, column={}, error={}", tableName, columnName, e.getMessage(), e);
            throw e;
        }
        return null;
    }

    @Override
    public String getColumnExtra(String tableName, String columnName) throws Exception {
        StringBuilder extra = new StringBuilder();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT is_in_primary_key, is_in_sorting_key, compression_codec " +
                                             "FROM system.columns " +
                                             "WHERE database = currentDatabase() AND table = '" + tableName + "' " +
                                             "AND name = '" + columnName + "'")) {
            if (rs.next()) {
                boolean isPrimaryKey = rs.getBoolean("is_in_primary_key");
                boolean isSortingKey = rs.getBoolean("is_in_sorting_key");
                String compressionCodec = rs.getString("compression_codec");
                
                if (isPrimaryKey) {
                    extra.append("PRIMARY KEY ");
                }
                
                if (isSortingKey) {
                    extra.append("SORTING KEY ");
                }
                
                if (compressionCodec != null && !compressionCodec.isEmpty()) {
                    extra.append("CODEC(").append(compressionCodec).append(") ");
                }
            }
        } catch (Exception e) {
            log.error("获取额外属性失败: table={}, column={}, error={}", tableName, columnName, e.getMessage(), e);
            throw e;
        }
        return extra.length() > 0 ? extra.toString().trim() : null;
    }

    @Override
    public Integer getColumnPosition(String tableName, String columnName) throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT position FROM system.columns " +
                                             "WHERE database = currentDatabase() AND table = '" + tableName + "' " +
                                             "AND name = '" + columnName + "'")) {
            if (rs.next()) {
                return rs.getInt("position");
            }
        } catch (Exception e) {
            log.error("获取列位置失败: table={}, column={}, error={}", tableName, columnName, e.getMessage(), e);
            throw e;
        }
        throw new Exception("Column not found: " + columnName);
    }

    @Override
    public Date getTableCreateTime(String tableName) throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT min(modification_time) as create_time FROM system.parts " +
                                             "WHERE database = currentDatabase() AND table = '" + tableName + "'")) {
            if (rs.next()) {
                long timestamp = rs.getLong("create_time");
                if (timestamp > 0) {
                    return new Date(timestamp * 1000); // ClickHouse时间戳是秒级的
                }
            }
        } catch (Exception e) {
            log.error("获取表创建时间失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return null;
    }

    @Override
    public Date getTableUpdateTime(String tableName) throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT max(modification_time) as update_time FROM system.parts " +
                                             "WHERE database = currentDatabase() AND table = '" + tableName + "'")) {
            if (rs.next()) {
                long timestamp = rs.getLong("update_time");
                if (timestamp > 0) {
                    return new Date(timestamp * 1000); // ClickHouse时间戳是秒级的
                }
            }
        } catch (Exception e) {
            log.error("获取表更新时间失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return null;
    }

    @Override
    public List<String> getPrimaryKeys(String tableName) throws Exception {
        List<String> primaryKeys = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM system.columns " +
                                             "WHERE database = currentDatabase() AND table = '" + tableName + "' " +
                                             "AND is_in_primary_key = 1 ORDER BY position")) {
            while (rs.next()) {
                primaryKeys.add(rs.getString("name"));
            }
        } catch (Exception e) {
            log.error("获取主键信息失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return primaryKeys;
    }

    @Override
    public List<Map<String, Object>> getForeignKeys(String tableName) throws Exception {
        // ClickHouse不支持外键约束
        return new ArrayList<>();
    }

    @Override
    public List<Map<String, Object>> getIndexes(String tableName) throws Exception {
        List<Map<String, Object>> indexes = new ArrayList<>();
        
        // 获取主键索引
        Map<String, Object> primaryKeyIndex = new HashMap<>();
        List<String> primaryKeyColumns = new ArrayList<>();
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM system.columns " +
                                             "WHERE database = currentDatabase() AND table = '" + tableName + "' " +
                                             "AND is_in_primary_key = 1 ORDER BY position")) {
            while (rs.next()) {
                primaryKeyColumns.add(rs.getString("name"));
            }
        }
        
        if (!primaryKeyColumns.isEmpty()) {
            primaryKeyIndex.put("indexName", "PRIMARY");
            primaryKeyIndex.put("columns", primaryKeyColumns);
            primaryKeyIndex.put("unique", true);
            primaryKeyIndex.put("type", "PRIMARY");
            indexes.add(primaryKeyIndex);
        }
        
        // 获取排序键索引
        Map<String, Object> sortingKeyIndex = new HashMap<>();
        List<String> sortingKeyColumns = new ArrayList<>();
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM system.columns " +
                                             "WHERE database = currentDatabase() AND table = '" + tableName + "' " +
                                             "AND is_in_sorting_key = 1 ORDER BY position")) {
            while (rs.next()) {
                sortingKeyColumns.add(rs.getString("name"));
            }
        }
        
        if (!sortingKeyColumns.isEmpty() && !sortingKeyColumns.equals(primaryKeyColumns)) {
            sortingKeyIndex.put("indexName", "SORTING");
            sortingKeyIndex.put("columns", sortingKeyColumns);
            sortingKeyIndex.put("unique", false);
            sortingKeyIndex.put("type", "SORTING");
            indexes.add(sortingKeyIndex);
        }
        
        // 获取跳数索引（ClickHouse 19.11+）
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name, expression, type FROM system.data_skipping_indices " +
                                             "WHERE database = currentDatabase() AND table = '" + tableName + "'")) {
            while (rs.next()) {
                Map<String, Object> index = new HashMap<>();
                index.put("indexName", rs.getString("name"));
                index.put("expression", rs.getString("expression"));
                index.put("type", rs.getString("type"));
                index.put("unique", false);
                indexes.add(index);
            }
        } catch (Exception e) {
            // 可能是旧版本ClickHouse，不支持system.data_skipping_indices表
            log.warn("获取跳数索引失败，可能是不支持的ClickHouse版本: table={}, error={}", tableName, e.getMessage());
        }
        
        return indexes;
    }

    @Override
    public Map<String, Object> getTableCompleteness(String tableName) throws Exception {
        Map<String, Object> completeness = new HashMap<>();
        try (Connection conn = getConnection()) {
            // 获取表的总行数
            long totalRows = 0;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT count() FROM " + wrapIdentifier(tableName))) {
                if (rs.next()) {
                    totalRows = rs.getLong(1);
                }
            }
            
            if (totalRows == 0) {
                completeness.put("completenessRatio", 1.0);
                completeness.put("nullCount", 0);
                completeness.put("nonNullCount", 0);
                completeness.put("totalCount", 0);
                return completeness;
            }
            
            // 获取表的列信息
            List<String> columns = new ArrayList<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT name FROM system.columns " +
                                                 "WHERE database = currentDatabase() AND table = '" + tableName + "' " +
                                                 "AND type LIKE '%Nullable%'")) {
                while (rs.next()) {
                    columns.add(rs.getString("name"));
                }
            }
            
            // 计算每列的非空值数量
            long totalNonNullCount = 0;
            long totalNullableColumns = columns.size();
            
            for (String column : columns) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT count() - countIf(isNull(" + wrapIdentifier(column) + ")) AS non_null_count FROM " + wrapIdentifier(tableName))) {
                    if (rs.next()) {
                        long nonNullCount = rs.getLong("non_null_count");
                        totalNonNullCount += nonNullCount;
                    }
                }
            }
            
            // 计算完整性比率
            double completenessRatio = totalNullableColumns > 0 ? (double) totalNonNullCount / (totalRows * totalNullableColumns) : 1.0;
            
            completeness.put("completenessRatio", completenessRatio);
            completeness.put("nullCount", (totalRows * totalNullableColumns) - totalNonNullCount);
            completeness.put("nonNullCount", totalNonNullCount);
            completeness.put("totalCount", totalRows * totalNullableColumns);
            
        } catch (Exception e) {
            log.error("计算表完整性失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return completeness;
    }

    @Override
    public Map<String, Object> getTableAccuracy(String tableName) throws Exception {
        Map<String, Object> accuracy = new HashMap<>();
        try (Connection conn = getConnection()) {
            // 获取表的总行数
            long totalRows = 0;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT count() FROM " + wrapIdentifier(tableName))) {
                if (rs.next()) {
                    totalRows = rs.getLong(1);
                }
            }
            
            if (totalRows == 0) {
                accuracy.put("accuracyRatio", 1.0);
                accuracy.put("validCount", 0);
                accuracy.put("invalidCount", 0);
                accuracy.put("totalCount", 0);
                return accuracy;
            }
            
            // 获取表的列信息
            List<Map<String, String>> columns = new ArrayList<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT name, type FROM system.columns " +
                                                 "WHERE database = currentDatabase() AND table = '" + tableName + "'")) {
                while (rs.next()) {
                    Map<String, String> column = new HashMap<>();
                    column.put("name", rs.getString("name"));
                    column.put("type", rs.getString("type"));
                    columns.add(column);
                }
            }
            
            // 计算每列的有效值数量
            long totalValidCount = 0;
            long totalCheckedCount = 0;
            
            for (Map<String, String> column : columns) {
                String columnName = column.get("name");
                String dataType = column.get("type");
                
                String validationSql = getValidationSqlForDataType(tableName, columnName, dataType);
                if (validationSql != null) {
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(validationSql)) {
                        if (rs.next()) {
                            long validCount = rs.getLong(1);
                            totalValidCount += validCount;
                            totalCheckedCount += totalRows;
                        }
                    }
                }
            }
            
            // 计算准确性比率
            double accuracyRatio = totalCheckedCount > 0 ? (double) totalValidCount / totalCheckedCount : 1.0;
            
            accuracy.put("accuracyRatio", accuracyRatio);
            accuracy.put("validCount", totalValidCount);
            accuracy.put("invalidCount", totalCheckedCount - totalValidCount);
            accuracy.put("totalCount", totalCheckedCount);
            
        } catch (Exception e) {
            log.error("计算表准确性失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return accuracy;
    }

    @Override
    public Map<String, Object> getTableConsistency(String tableName) throws Exception {
        // ClickHouse不支持外键约束，所以无法检查外键一致性
        Map<String, Object> consistency = new HashMap<>();
        consistency.put("consistencyRatio", 1.0);
        consistency.put("validCount", 0);
        consistency.put("invalidCount", 0);
        consistency.put("totalCount", 0);
        return consistency;
    }

    @Override
    public Map<String, Object> getTableUniqueness(String tableName) throws Exception {
        Map<String, Object> uniqueness = new HashMap<>();
        try (Connection conn = getConnection()) {
            // 获取表的主键信息
            List<String> primaryKeys = getPrimaryKeys(tableName);
            
            if (primaryKeys.isEmpty()) {
                uniqueness.put("uniquenessRatio", 0.0);
                uniqueness.put("duplicateCount", 0);
                uniqueness.put("uniqueCount", 0);
                uniqueness.put("totalCount", 0);
                return uniqueness;
            }
            
            // 获取表的总行数
            long totalRows = 0;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT count() FROM " + wrapIdentifier(tableName))) {
                if (rs.next()) {
                    totalRows = rs.getLong(1);
                }
            }
            
            if (totalRows == 0) {
                uniqueness.put("uniquenessRatio", 1.0);
                uniqueness.put("duplicateCount", 0);
                uniqueness.put("uniqueCount", 0);
                uniqueness.put("totalCount", 0);
                return uniqueness;
            }
            
            // 检查主键列的唯一性
            long totalUniqueCount = 0;
            
            for (String column : primaryKeys) {
                String countSql = String.format(
                    "SELECT count(DISTINCT %s) FROM %s",
                    wrapIdentifier(column),
                    wrapIdentifier(tableName)
                );
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(countSql)) {
                    if (rs.next()) {
                        long uniqueCount = rs.getLong(1);
                        totalUniqueCount += uniqueCount;
                    }
                }
            }
            
            // 计算唯一性比率
            double uniquenessRatio = primaryKeys.size() > 0 ? (double) totalUniqueCount / (totalRows * primaryKeys.size()) : 0.0;
            
            uniqueness.put("uniquenessRatio", uniquenessRatio);
            uniqueness.put("duplicateCount", (totalRows * primaryKeys.size()) - totalUniqueCount);
            uniqueness.put("uniqueCount", totalUniqueCount);
            uniqueness.put("totalCount", totalRows * primaryKeys.size());
            
        } catch (Exception e) {
            log.error("计算表唯一性失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return uniqueness;
    }

    @Override
    public Map<String, Object> getTableValidity(String tableName) throws Exception {
        Map<String, Object> validity = new HashMap<>();
        
        try (Connection conn = getConnection()) {
            List<Map<String, Object>> columns = listColumns(tableName);
            Map<String, Object> columnValidity = new HashMap<>();
            
            // 检查日期字段的有效性
            for (Map<String, Object> column : columns) {
                String columnName = (String) column.get("name");
                String dataType = ((String) column.get("type")).toLowerCase();
                
                if (dataType.contains("date") || dataType.contains("time")) {
                    // 检查日期字段是否有无效值
                    String sql = String.format(
                        "SELECT COUNT(*) AS total, " +
                        "SUM(CASE WHEN %s IS NULL THEN 0 ELSE 1 END) AS non_null, " +
                        "SUM(CASE WHEN %s IS NULL THEN 0 " +
                        "     WHEN isValidDate(%s) THEN 1 " +
                        "     ELSE 0 END) AS valid_format " +
                        "FROM %s",
                        wrapIdentifier(columnName),
                        wrapIdentifier(columnName),
                        wrapIdentifier(columnName),
                        wrapIdentifier(tableName)
                    );
                    
                    try (PreparedStatement stmt = conn.prepareStatement(sql);
                         ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            long total = rs.getLong("total");
                            long nonNull = rs.getLong("non_null");
                            long validFormat = rs.getLong("valid_format");
                            
                            double validRate = total > 0 ? (double) validFormat / total * 100 : 100.0;
                            Map<String, Object> validityMap = new HashMap<>();
                            validityMap.put("VALID_RATE", validRate);
                            validityMap.put("TOTAL", total);
                            validityMap.put("NON_NULL", nonNull);
                            validityMap.put("VALID_FORMAT", validFormat);
                            columnValidity.put(columnName, validityMap);
                        }
                    }
                }
            }
            
            validity.put("DATE_FIELD_VALIDITY", columnValidity);
    } catch (Exception e) {
            log.error("获取表有效性失败: table={}, error={}", tableName, e.getMessage(), e);
        throw e;
    }
        
    return validity;
}

@Override
public Map<String, Integer> getQualityIssueCount(String tableName) throws Exception {
    Map<String, Integer> issueCount = new HashMap<>();
        
    try (Connection conn = getConnection()) {
            // 统计空值问题
            int nullValueCount = 0;
            List<Map<String, Object>> columns = listColumns(tableName);
            
            for (Map<String, Object> column : columns) {
                String columnName = (String) column.get("name");
                String sql = String.format(
                    "SELECT COUNT(*) AS null_count " +
                    "FROM %s " +
                    "WHERE %s IS NULL",
                    wrapIdentifier(tableName),
                    wrapIdentifier(columnName)
                );
                
                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        nullValueCount += rs.getInt("null_count");
                    }
                }
            }
            
            issueCount.put("NULL_VALUES", nullValueCount);
            
            // 统计重复值问题
            int duplicateValueCount = 0;
            
            // 获取主键或唯一索引列
            List<String> uniqueColumns = new ArrayList<>();
            List<String> primaryKeys = getPrimaryKeys(tableName);
            if (!primaryKeys.isEmpty()) {
                uniqueColumns.addAll(primaryKeys);
            } else {
                // 如果没有主键，尝试使用所有列
                for (Map<String, Object> column : columns) {
                    uniqueColumns.add((String) column.get("name"));
                }
            }
            
            if (!uniqueColumns.isEmpty()) {
                String columnList = uniqueColumns.stream()
                    .map(this::wrapIdentifier)
                    .collect(Collectors.joining(", "));
                
                String sql = String.format(
                    "SELECT COUNT(*) - COUNT(DISTINCT(%s)) AS dup_count " +
                    "FROM %s",
                    columnList,
                    wrapIdentifier(tableName)
                );
                
                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        duplicateValueCount = rs.getInt("dup_count");
                    }
                }
            }
            
            issueCount.put("DUPLICATE_VALUES", duplicateValueCount);
            
    } catch (Exception e) {
        log.error("获取质量问题统计失败: table={}, error={}", tableName, e.getMessage(), e);
        throw e;
    }
        
    return issueCount;
}

@Override
public Map<String, Double> getQualityIssueDistribution(String tableName) throws Exception {
    Map<String, Double> distribution = new HashMap<>();
        Map<String, Integer> issueCount = getQualityIssueCount(tableName);
        
        int totalIssues = issueCount.values().stream().mapToInt(Integer::intValue).sum();
        
        if (totalIssues > 0) {
            for (Map.Entry<String, Integer> entry : issueCount.entrySet()) {
                double percentage = (double) entry.getValue() / totalIssues * 100;
                distribution.put(entry.getKey(), percentage);
            }
        }
        
    return distribution;
}

private String getValidationSqlForDataType(String tableName, String columnName, String dataType) {
    if (dataType == null) {
        return null;
    }
    
        String sql = "SELECT COUNT(*) FROM " + wrapIdentifier(tableName) + " WHERE ";
    
        if (dataType.contains("int") || dataType.contains("float") || dataType.contains("decimal") || dataType.contains("double")) {
            // 数值类型验证
        return sql + wrapIdentifier(columnName) + " IS NOT NULL";
        } else if (dataType.contains("string") || dataType.contains("fixedstring")) {
        // 字符串类型验证
        return sql + wrapIdentifier(columnName) + " IS NOT NULL";
        } else if (dataType.contains("date") || dataType.contains("time")) {
        // 日期类型验证
        return sql + wrapIdentifier(columnName) + " IS NOT NULL";
    }
    
    return null;
}

@Override
public List<Map<String, Object>> getTableForeignKeyRelations(String tableName) throws Exception {
    // ClickHouse不支持外键约束
    return new ArrayList<>();
}

@Override
public List<Map<String, Object>> getReferencedByRelations(String tableName) throws Exception {
    // ClickHouse不支持外键约束
    return new ArrayList<>();
}

@Override
public Map<String, Object> getTableFieldStatistics(String tableName) throws Exception {
    Map<String, Object> statistics = new HashMap<>();
        
    try (Connection conn = getConnection()) {
            List<Map<String, Object>> columns = listColumns(tableName);
            
            int totalColumns = columns.size();
            int numericColumns = 0;
            int stringColumns = 0;
            int dateColumns = 0;
            int booleanColumns = 0;
            int otherColumns = 0;
            
            for (Map<String, Object> column : columns) {
                String dataType = ((String) column.get("type")).toLowerCase();
                
                if (dataType.contains("int") || dataType.contains("float") || 
                    dataType.contains("decimal") || dataType.contains("double")) {
                    numericColumns++;
                } else if (dataType.contains("string") || dataType.contains("fixedstring")) {
                    stringColumns++;
                } else if (dataType.contains("date") || dataType.contains("time")) {
                    dateColumns++;
                } else if (dataType.equals("uint8") && dataType.contains("bool")) {
                    booleanColumns++;
                } else {
                    otherColumns++;
                }
            }
            
            statistics.put("TOTAL_COLUMNS", totalColumns);
            statistics.put("NUMERIC_COLUMNS", numericColumns);
            statistics.put("STRING_COLUMNS", stringColumns);
            statistics.put("DATE_COLUMNS", dateColumns);
            statistics.put("BOOLEAN_COLUMNS", booleanColumns);
            statistics.put("OTHER_COLUMNS", otherColumns);
            
            // 计算百分比
            if (totalColumns > 0) {
                statistics.put("NUMERIC_PERCENTAGE", (double) numericColumns / totalColumns * 100);
                statistics.put("STRING_PERCENTAGE", (double) stringColumns / totalColumns * 100);
                statistics.put("DATE_PERCENTAGE", (double) dateColumns / totalColumns * 100);
                statistics.put("BOOLEAN_PERCENTAGE", (double) booleanColumns / totalColumns * 100);
                statistics.put("OTHER_PERCENTAGE", (double) otherColumns / totalColumns * 100);
            }
    } catch (Exception e) {
        log.error("获取表字段统计信息失败: table={}, error={}", tableName, e.getMessage(), e);
        throw e;
    }
        
    return statistics;
}

@Override
public Map<String, Object> getTableUpdateFrequency(String tableName) throws Exception {
        // ClickHouse不直接存储表的更新时间，返回基本信息
    Map<String, Object> frequency = new HashMap<>();
        frequency.put("TABLE_NAME", tableName);
        frequency.put("LAST_UPDATE_TIME", null);
        frequency.put("UPDATE_FREQUENCY", "Unknown");
    return frequency;
}

@Override
public List<Map<String, Object>> getTableGrowthTrend(String tableName, int days) throws Exception {
        // ClickHouse不直接存储表的增长趋势，返回空列表
        return new ArrayList<>();
}

@Override
public List<Map<String, Object>> getTableDataSample(String tableName, int sampleSize) throws Exception {
    List<Map<String, Object>> samples = new ArrayList<>();
        
        try (Connection conn = getConnection()) {
            // 获取表的所有列
            List<Map<String, Object>> columns = listColumns(tableName);
            List<String> columnNames = columns.stream()
                .map(col -> (String) col.get("name"))
                .collect(Collectors.toList());
            
            // 构建查询语句
            String columnList = columnNames.stream()
                .map(this::wrapIdentifier)
                .collect(Collectors.joining(", "));
            
            String sql = String.format(
                "SELECT %s FROM %s LIMIT %d",
                columnList,
                wrapIdentifier(tableName),
                sampleSize
            );
            
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
                    for (String columnName : columnNames) {
                        row.put(columnName, rs.getObject(columnName));
            }
            samples.add(row);
        }
            }
    } catch (Exception e) {
        log.error("获取表数据样本失败: table={}, error={}", tableName, e.getMessage(), e);
        throw e;
    }
        
    return samples;
}

@Override
public Map<String, Object> getColumnValueRange(String tableName, String columnName) throws Exception {
    Map<String, Object> range = new HashMap<>();
        
        try (Connection conn = getConnection()) {
            Map<String, Object> columnInfo = getColumnInfo(tableName, columnName);
            String dataType = ((String) columnInfo.get("type")).toLowerCase();
            
            if (dataType.contains("int") || dataType.contains("float") || 
                dataType.contains("decimal") || dataType.contains("double")) {
                    
                    // 数值类型
                String sql = String.format(
                    "SELECT MIN(%s) AS min_value, " +
                    "MAX(%s) AS max_value, " +
                    "AVG(%s) AS avg_value " +
                    "FROM %s " +
                    "WHERE %s IS NOT NULL",
                        wrapIdentifier(columnName),
                        wrapIdentifier(columnName),
                        wrapIdentifier(columnName),
                    wrapIdentifier(tableName),
                    wrapIdentifier(columnName)
                );
                
                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        range.put("MIN_VALUE", rs.getObject("min_value"));
                        range.put("MAX_VALUE", rs.getObject("max_value"));
                        range.put("AVG_VALUE", rs.getObject("avg_value"));
                    }
                }
            } else if (dataType.contains("date") || dataType.contains("time")) {
                    // 日期类型
                String sql = String.format(
                    "SELECT MIN(%s) AS min_value, " +
                    "MAX(%s) AS max_value " +
                    "FROM %s " +
                    "WHERE %s IS NOT NULL",
                        wrapIdentifier(columnName),
                        wrapIdentifier(columnName),
                    wrapIdentifier(tableName),
                    wrapIdentifier(columnName)
                );
                
                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        range.put("MIN_VALUE", rs.getObject("min_value"));
                        range.put("MAX_VALUE", rs.getObject("max_value"));
                    }
                }
            } else if (dataType.contains("string") || dataType.contains("fixedstring")) {
                // 字符类型
                String sql = String.format(
                    "SELECT MIN(length(%s)) AS min_length, " +
                    "MAX(length(%s)) AS max_length, " +
                    "AVG(length(%s)) AS avg_length " +
                    "FROM %s " +
                    "WHERE %s IS NOT NULL",
                wrapIdentifier(columnName),
                    wrapIdentifier(columnName),
                    wrapIdentifier(columnName),
                    wrapIdentifier(tableName),
                    wrapIdentifier(columnName)
                );
                
                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        range.put("MIN_LENGTH", rs.getObject("min_length"));
                        range.put("MAX_LENGTH", rs.getObject("max_length"));
                        range.put("AVG_LENGTH", rs.getObject("avg_length"));
                    }
                }
            }
    } catch (Exception e) {
            log.error("获取字段值域范围失败: table={}, column={}, error={}", tableName, columnName, e.getMessage(), e);
        throw e;
    }
        
    return range;
}

@Override
public List<Map<String, Object>> getColumnValueDistribution(String tableName, String columnName, int topN) throws Exception {
    List<Map<String, Object>> distribution = new ArrayList<>();
        
        try (Connection conn = getConnection()) {
            // 获取总行数
            long totalRows = 0;
            String countSql = String.format(
                "SELECT COUNT(*) AS total_rows " +
                "FROM %s " +
                "WHERE %s IS NOT NULL",
                        wrapIdentifier(tableName),
                wrapIdentifier(columnName)
            );
            
            try (PreparedStatement stmt = conn.prepareStatement(countSql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    totalRows = rs.getLong("total_rows");
                }
            }
            
            if (totalRows > 0) {
                // 获取前N个值的分布
                String sql = String.format(
                    "SELECT %s AS value, COUNT(*) AS frequency " +
                    "FROM %s " +
                    "WHERE %s IS NOT NULL " +
                    "GROUP BY %s " +
                    "ORDER BY frequency DESC " +
                    "LIMIT %d",
                        wrapIdentifier(columnName),
                        wrapIdentifier(tableName),
                    wrapIdentifier(columnName),
                        wrapIdentifier(columnName),
                        topN
                    );
                    
                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                            Map<String, Object> item = new HashMap<>();
                        item.put("VALUE", rs.getObject("value"));
                        long frequency = rs.getLong("frequency");
                        item.put("FREQUENCY", frequency);
                        item.put("PERCENTAGE", (double) frequency / totalRows * 100);
                            distribution.add(item);
                        }
                    }
                }
    } catch (Exception e) {
            log.error("获取字段值分布失败: table={}, column={}, error={}", tableName, columnName, e.getMessage(), e);
        throw e;
    }
        
    return distribution;
}

@Override
public Map<String, Object> getColumnDistribution(String tableName, String columnName) throws Exception {
    Map<String, Object> distribution = new HashMap<>();
        
    try (Connection conn = getConnection()) {
            Map<String, Object> columnInfo = getColumnInfo(tableName, columnName);
            String dataType = ((String) columnInfo.get("type")).toLowerCase();
            
            // 获取基本统计信息
            String sql = String.format(
                "SELECT COUNT(*) AS total_count, " +
                "COUNT(DISTINCT %s) AS distinct_count, " +
                "SUM(CASE WHEN %s IS NULL THEN 1 ELSE 0 END) AS null_count ",
                wrapIdentifier(columnName),
                wrapIdentifier(columnName)
            );
            
            // 根据数据类型添加不同的统计
            if (dataType.contains("int") || dataType.contains("float") || 
                dataType.contains("decimal") || dataType.contains("double")) {
                
                sql += String.format(
                    ", MIN(%s) AS min_value " +
                    ", MAX(%s) AS max_value " +
                    ", AVG(%s) AS avg_value ",
                    wrapIdentifier(columnName),
                    wrapIdentifier(columnName),
                    wrapIdentifier(columnName)
                );
            }
            
            sql += "FROM " + wrapIdentifier(tableName);
            
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long totalCount = rs.getLong("total_count");
                    long distinctCount = rs.getLong("distinct_count");
                    long nullCount = rs.getLong("null_count");
                    
                    distribution.put("TOTAL_COUNT", totalCount);
                    distribution.put("DISTINCT_COUNT", distinctCount);
                    distribution.put("NULL_COUNT", nullCount);
                    
                    if (totalCount > 0) {
                        distribution.put("DISTINCT_PERCENTAGE", (double) distinctCount / totalCount * 100);
                        distribution.put("NULL_PERCENTAGE", (double) nullCount / totalCount * 100);
                    }
                    
                    if (dataType.contains("int") || dataType.contains("float") || 
                        dataType.contains("decimal") || dataType.contains("double")) {
                        
                        distribution.put("MIN_VALUE", rs.getObject("min_value"));
                        distribution.put("MAX_VALUE", rs.getObject("max_value"));
                        distribution.put("AVG_VALUE", rs.getObject("avg_value"));
                    }
                    
                    // 获取频率分布（前10个值）
                    if (totalCount > 0 && distinctCount > 0 && distinctCount < 100) {
                        List<Map<String, Object>> valueDistribution = getColumnValueDistribution(tableName, columnName, 10);
                        distribution.put("FREQUENCY_DISTRIBUTION", valueDistribution);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取列分布信息失败: table={}, column={}, error={}", tableName, columnName, e.getMessage(), e);
            throw e;
        }
        
        return distribution;
    }

    @Override
    public List<Map<String, Object>> getTableDependencies(String tableName) throws Exception {
        // ClickHouse不支持外键和依赖关系
        return new ArrayList<>();
    }

    @Override
    public Map<String, Object> getQualityIssues(String tableName) throws Exception {
        Map<String, Object> issues = new HashMap<>();
        
        try (Connection conn = getConnection()) {
            List<Map<String, Object>> columns = listColumns(tableName);
            Map<String, Integer> nullCounts = new HashMap<>();
            
            // 获取空值统计
            for (Map<String, Object> column : columns) {
                String columnName = (String) column.get("name");
                String sql = String.format(
                    "SELECT COUNT(*) AS null_count " +
                    "FROM %s " +
                    "WHERE %s IS NULL",
                    wrapIdentifier(tableName),
                    wrapIdentifier(columnName)
                );
                
                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int nullCount = rs.getInt("null_count");
                        if (nullCount > 0) {
                            nullCounts.put(columnName, nullCount);
                        }
                    }
                }
            }
            
            if (!nullCounts.isEmpty()) {
                issues.put("null_values", nullCounts);
            }
            
            // 获取重复值统计
            Map<String, Integer> duplicateCounts = new HashMap<>();
            for (Map<String, Object> column : columns) {
                String columnName = (String) column.get("name");
                String sql = String.format(
                    "SELECT %s, COUNT(*) AS count " +
            "FROM %s " +
            "WHERE %s IS NOT NULL " +
            "GROUP BY %s " +
                    "HAVING COUNT(*) > 1",
            wrapIdentifier(columnName),
            wrapIdentifier(tableName),
            wrapIdentifier(columnName),
            wrapIdentifier(columnName)
        );
        
                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    int duplicateCount = 0;
            while (rs.next()) {
                        duplicateCount++;
                    }
                    if (duplicateCount > 0) {
                        duplicateCounts.put(columnName, duplicateCount);
                    }
                }
            }
            
            if (!duplicateCounts.isEmpty()) {
                issues.put("duplicate_values", duplicateCounts);
            }
    } catch (Exception e) {
            log.error("获取质量问题失败: table={}, error={}", tableName, e.getMessage(), e);
        throw e;
    }
        
        return issues;
    }

    @Override
    public Map<String, Object> calculateQualityMetric(String tableName, String metricType) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 根据指标类型计算不同的质量指标
            switch (metricType.toLowerCase()) {
                case "completeness":
                    return getTableCompleteness(tableName);
                case "accuracy":
                    return getTableAccuracy(tableName);
                case "consistency":
                    return getTableConsistency(tableName);
                case "uniqueness":
                    return getTableUniqueness(tableName);
                case "validity":
                    return getTableValidity(tableName);
                case "overall":
                default:
                    // 获取各维度的质量指标
                    Map<String, Object> completeness = getTableCompleteness(tableName);
                    Map<String, Object> accuracy = getTableAccuracy(tableName);
                    Map<String, Object> consistency = getTableConsistency(tableName);
                    Map<String, Object> uniqueness = getTableUniqueness(tableName);
                    
                    // 提取各维度的比率
                    double completenessRatio = (double) completeness.get("completenessRatio");
                    double accuracyRatio = (double) accuracy.get("accuracyRatio");
                    double consistencyRatio = (double) consistency.get("consistencyRatio");
                    double uniquenessRatio = (double) uniqueness.get("uniquenessRatio");
                    
                    // 计算综合质量分数 (0-100分)
                    double overallScore = (completenessRatio + accuracyRatio + consistencyRatio + uniquenessRatio) / 4 * 100;
                    
                    // 质量等级评定
                    String qualityLevel;
                    if (overallScore >= 90) {
                        qualityLevel = "优秀";
                    } else if (overallScore >= 80) {
                        qualityLevel = "良好";
                    } else if (overallScore >= 70) {
                        qualityLevel = "一般";
                    } else if (overallScore >= 60) {
                        qualityLevel = "及格";
                    } else {
                        qualityLevel = "较差";
                    }
                    
                    // 构建返回结果
                    result.put("tableName", tableName);
                    result.put("overallScore", overallScore);
                    result.put("qualityLevel", qualityLevel);
                    result.put("completenessScore", completenessRatio * 100);
                    result.put("accuracyScore", accuracyRatio * 100);
                    result.put("consistencyScore", consistencyRatio * 100);
                    result.put("uniquenessScore", uniquenessRatio * 100);
                    
                    // 添加各维度的详细信息
                    result.put("completenessDetails", completeness);
                    result.put("accuracyDetails", accuracy);
                    result.put("consistencyDetails", consistency);
                    result.put("uniquenessDetails", uniqueness);
                    
                    // 获取质量问题
                    Map<String, Object> issues = getQualityIssues(tableName);
                    result.put("issues", issues);
                    
                    // 计算问题总数
                    int totalIssueCount = 0;
                    if (issues.containsKey("null_values")) {
                        Map<String, Integer> nullCounts = (Map<String, Integer>) issues.get("null_values");
                        totalIssueCount += nullCounts.values().stream().mapToInt(Integer::intValue).sum();
                    }
                    if (issues.containsKey("duplicate_values")) {
                        Map<String, Integer> duplicateCounts = (Map<String, Integer>) issues.get("duplicate_values");
                        totalIssueCount += duplicateCounts.values().stream().mapToInt(Integer::intValue).sum();
                    }
                    result.put("totalIssueCount", totalIssueCount);
                    
                    // 添加时间戳
                    result.put("calculationTime", new Date());
                    
                    return result;
            }
        } catch (Exception e) {
            log.error("计算表质量指标失败: table={}, metricType={}, error={}", tableName, metricType, e.getMessage(), e);
            throw e;
        }
}

    @Override
    public Map<String, String> getStoredProcedureDefinitions(String tableName) throws Exception {
        // ClickHouse不支持传统意义上的存储过程，但支持用户定义函数(UDF)
        Map<String, String> procedures = new HashMap<>();
        
        try (Connection conn = getConnection()) {
            // 查询系统表中的函数定义
            String sql = "SELECT name AS function_name, create_query AS function_definition " +
                         "FROM system.functions " +
                         "WHERE create_query LIKE ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, "%" + tableName + "%");
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String funcName = rs.getString("function_name");
                        String funcDef = rs.getString("function_definition");
                        procedures.put(funcName, funcDef);
                    }
                }
            } catch (SQLException e) {
                // ClickHouse可能不支持此查询，忽略错误
                log.warn("ClickHouse查询函数定义失败: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("获取函数定义失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        
        return procedures;
    }

    /**
     * 获取表的分区列表
     *
     * @param tableName 表名
     * @param partitionField 分区字段名
     * @return 分区值列表
     */
    @Override
    public List<String> getTablePartitions(String tableName, String partitionField) throws Exception {
        List<String> partitions = new ArrayList<>();
        
        // ClickHouse支持分区表，首先尝试查询分区信息
        String partitionInfoSql = String.format(
                "SELECT partition FROM system.parts WHERE table = '%s' AND active = 1 GROUP BY partition ORDER BY partition",
                tableName);
                
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
             
            try (ResultSet rs = stmt.executeQuery(partitionInfoSql)) {
                boolean hasPartitions = false;
                while (rs.next()) {
                    hasPartitions = true;
                    String partition = rs.getString("partition");
                    if (partition != null) {
                        partitions.add(partition);
                    }
                }
                
                // 如果没有找到分区信息，使用通用查询
                if (!hasPartitions) {
                    // 基于字段直接查询
                    String sql = String.format("SELECT DISTINCT %s FROM %s ORDER BY %s", 
                            wrapIdentifier(partitionField), 
                            wrapIdentifier(tableName),
                            wrapIdentifier(partitionField));
                    
                    try (ResultSet genericRs = stmt.executeQuery(sql)) {
                        while (genericRs.next()) {
                            Object value = genericRs.getObject(1);
                            if (value != null) {
                                partitions.add(value.toString());
                            }
                        }
                    } catch (Exception e) {
                        log.warn("获取表的分区值失败: {}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.warn("查询ClickHouse分区信息失败: {}", e.getMessage());
                
                // 如果查询分区信息失败，回退到直接查询字段值
                String sql = String.format("SELECT DISTINCT %s FROM %s ORDER BY %s", 
                        wrapIdentifier(partitionField), 
                        wrapIdentifier(tableName),
                        wrapIdentifier(partitionField));
                
                try (ResultSet genericRs = stmt.executeQuery(sql)) {
                    while (genericRs.next()) {
                        Object value = genericRs.getObject(1);
                        if (value != null) {
                            partitions.add(value.toString());
                        }
                    }
                } catch (Exception ex) {
                    log.error("获取表的分区值失败: {}", ex.getMessage());
                }
            }
        }
        
        return partitions;
    }

    /**
     * 获取创建临时表的SQL
     *
     * @param tempTableName 临时表名称
     * @param sourceTableName 源表名称（用于复制结构）
     * @param preserveRows 是否在会话结束后保留数据（在ClickHouse中忽略）
     * @return 创建临时表的SQL
     * @throws Exception 如果出错
     */
    @Override
    public String getCreateTempTableSql(String tempTableName, String sourceTableName, boolean preserveRows) throws Exception {
        // 获取源表的列信息
        List<Map<String, Object>> columns = listColumns(sourceTableName);
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("无法获取源表 " + sourceTableName + " 的列信息");
        }
        
        StringBuilder sql = new StringBuilder();
        // ClickHouse不支持真正的临时表，而是使用TEMPORARY引擎的表
        sql.append("CREATE TABLE ").append(wrapIdentifier(tempTableName)).append(" (");
        
        // 添加列定义
        for (int i = 0; i < columns.size(); i++) {
            Map<String, Object> column = columns.get(i);
            if (i > 0) {
                sql.append(", ");
            }
            
            String columnName = column.get("name").toString();
            String dataType = column.get("type").toString();
            
            sql.append(wrapIdentifier(columnName)).append(" ").append(dataType);
            
            // 处理NULL约束 (ClickHouse列默认是Nullable的，除非指定)
            if (column.containsKey("nullable")) {
                boolean isNullable = Boolean.parseBoolean(column.get("nullable").toString());
                if (isNullable) {
                    if (!dataType.contains("Nullable")) {
                        sql.append(" Nullable");
                    }
                }
            }
        }
        
        sql.append(") ENGINE = Memory");
        
        return sql.toString();
    }
} 