package com.mango.test.database.service.impl.datasource.handlers;

import com.mango.test.database.entity.ColumnDefinition;
import com.mango.test.database.entity.DataSource;
import com.mango.test.database.entity.TableDefinition;
import com.mango.test.database.service.impl.datasource.AbstractDatabaseHandler;

import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HBaseHandler extends AbstractDatabaseHandler {

    private static final String DRIVER_CLASS = "org.apache.phoenix.jdbc.PhoenixDriver";
    private static final String DEFAULT_PORT = "2181";
    private static final String URL_TEMPLATE = "jdbc:phoenix:%s:%s";

    private static final int DEFAULT_VARCHAR_LENGTH = 255;
    private static final int MAX_VARCHAR_LENGTH = Integer.MAX_VALUE;

    private static final Map<String, String> TYPE_MAPPING = new HashMap<>();
    private static final Map<String, Map<String, String>> COMMON_TYPE_MAPPING = new HashMap<>();

    static {
        // 基本类型映射
        TYPE_MAPPING.put("string", "VARCHAR");
        TYPE_MAPPING.put("text", "VARCHAR");
        TYPE_MAPPING.put("char", "CHAR");
        TYPE_MAPPING.put("int", "INTEGER");
        TYPE_MAPPING.put("bigint", "BIGINT");
        TYPE_MAPPING.put("float", "FLOAT");
        TYPE_MAPPING.put("double", "DOUBLE");
        TYPE_MAPPING.put("decimal", "DECIMAL");
        TYPE_MAPPING.put("boolean", "BOOLEAN");
        TYPE_MAPPING.put("date", "DATE");
        TYPE_MAPPING.put("time", "TIME");
        TYPE_MAPPING.put("timestamp", "TIMESTAMP");
        TYPE_MAPPING.put("binary", "BINARY");

        // MySQL 类型映射
        Map<String, String> mysqlMapping = new HashMap<>();
        mysqlMapping.put("VARCHAR", "VARCHAR");
        mysqlMapping.put("CHAR", "CHAR");
        mysqlMapping.put("TEXT", "VARCHAR");
        mysqlMapping.put("INT", "INTEGER");
        mysqlMapping.put("BIGINT", "BIGINT");
        mysqlMapping.put("FLOAT", "FLOAT");
        mysqlMapping.put("DOUBLE", "DOUBLE");
        mysqlMapping.put("DECIMAL", "DECIMAL");
        mysqlMapping.put("BOOLEAN", "BOOLEAN");
        mysqlMapping.put("DATE", "DATE");
        mysqlMapping.put("TIME", "TIME");
        mysqlMapping.put("TIMESTAMP", "TIMESTAMP");
        mysqlMapping.put("BLOB", "BINARY");
        COMMON_TYPE_MAPPING.put("mysql", mysqlMapping);

        // Oracle 类型映射
        Map<String, String> oracleMapping = new HashMap<>();
        oracleMapping.put("VARCHAR2", "VARCHAR");
        oracleMapping.put("CHAR", "CHAR");
        oracleMapping.put("NUMBER", "DECIMAL");
        oracleMapping.put("DATE", "DATE");
        oracleMapping.put("TIMESTAMP", "TIMESTAMP");
        oracleMapping.put("BLOB", "BINARY");
        COMMON_TYPE_MAPPING.put("oracle", oracleMapping);

        // PostgreSQL 类型映射
        Map<String, String> postgresMapping = new HashMap<>();
        postgresMapping.put("VARCHAR", "VARCHAR");
        postgresMapping.put("CHAR", "CHAR");
        postgresMapping.put("TEXT", "VARCHAR");
        postgresMapping.put("INTEGER", "INTEGER");
        postgresMapping.put("BIGINT", "BIGINT");
        postgresMapping.put("REAL", "FLOAT");
        postgresMapping.put("DOUBLE PRECISION", "DOUBLE");
        postgresMapping.put("NUMERIC", "DECIMAL");
        postgresMapping.put("BOOLEAN", "BOOLEAN");
        postgresMapping.put("DATE", "DATE");
        postgresMapping.put("TIME", "TIME");
        postgresMapping.put("TIMESTAMP", "TIMESTAMP");
        postgresMapping.put("BYTEA", "BINARY");
        COMMON_TYPE_MAPPING.put("postgresql", postgresMapping);
    }

    public HBaseHandler(DataSource dataSource) {
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
            dataSource.getPort() != null ? dataSource.getPort() : getDefaultPort()
        );
    }

    @Override
    public String getDatabaseProductName() {
        return "HBase";
    }

    @Override
    public String getValidationQuery() {
        return "SELECT 1";
    }


    @Override
    public String generatePageSql(String sql, int offset, int limit) {
        // HBase 不支持标准的分页语法，需要在应用层处理
        // 这里返回原始SQL，分页逻辑需要在应用层实现
        return sql;
    }

    /**
     * 生成HBase的LIMIT子句
     *
     * @param offset 起始位置
     * @param limit  返回记录的最大数量
     * @return 格式化后的LIMIT子句
     */
    @Override
    public String getLimitClause(long offset, int limit) {
        // HBase 不支持标准的分页语法
        // 返回空字符串，分页逻辑需要在应用层实现
        return "";
    }

    @Override
    public String generateCountSql(String sql) {
        // 添加 Phoenix 特有的优化提示
        return "SELECT /*+ NO_CACHE */ COUNT(1) FROM (" + sql + ")";
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
        // Phoenix 4.8+ 开始支持事务
        // 但默认为 false，需要特殊配置
        return false;
    }

    @Override
    public String getShowIndexesSql(String tableName) {
        return String.format(
            "SELECT INDEX_NAME, COLUMN_NAME, INDEX_TYPE " +
            "FROM SYSTEM.CATALOG " +
            "WHERE TABLE_SCHEM = CURRENT_SCHEMA() " +
            "AND TABLE_NAME = '%s' " +
            "AND INDEX_TYPE IS NOT NULL",
            tableName.toUpperCase()
        );
    }

    @Override
    public String getShowTablesSql() {
        // 从Phoenix系统表中获取所有表
        return "SELECT DISTINCT TABLE_NAME FROM SYSTEM.CATALOG WHERE TABLE_TYPE = 'u'";
    }

    @Override
    public String getShowColumnsSql(String tableName) {
        // 从Phoenix系统表中获取表的列信息
        return "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_SIZE, DECIMAL_DIGITS, IS_NULLABLE " +
               "FROM SYSTEM.CATALOG WHERE TABLE_NAME = '" + tableName.toUpperCase() + "' " +
               "AND COLUMN_NAME IS NOT NULL ORDER BY ORDINAL_POSITION";
    }

    @Override
    public int getDefaultVarcharLength() {
        return 0;
    }

    @Override
    public int getMaxVarcharLength() {
        return 0;
    }

    @Override
    public String getAddTableCommentSql(String tableName, String comment) {
        // Phoenix 不支持表注释
        return "";
    }

    @Override
    public String getModifyTableCommentSql(String tableName, String comment) {
        // Phoenix 不支持表注释
        return "";
    }

    @Override
    public String getAddColumnCommentSql(String tableName, String columnName, String comment) {
        // Phoenix 不支持列注释
        return "";
    }

    @Override
    public String getModifyColumnCommentSql(String tableName, String columnName, String comment) {
        // Phoenix 不支持列注释
        return "";
    }

    @Override
    public String getAddIndexSql(String tableName, String indexName, List<String> columns, boolean unique) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE ");
        
        // Phoenix 索引可以是本地索引或全局索引
        if (unique) {
            sql.append("UNIQUE ");
        } 
        
        sql.append("INDEX ");
        sql.append(wrapIdentifier(indexName));
        sql.append(" ON ");
        sql.append(wrapIdentifier(tableName));
        sql.append(" (");
        
        sql.append(columns.stream()
                  .map(this::wrapIdentifier)
                  .collect(Collectors.joining(", ")));
        
        sql.append(")");
        
        // 索引选项
        boolean isLocalIndex = indexName.toLowerCase().contains("local");
        if (isLocalIndex) {
            sql.append(" LOCAL");
        }
        
        // 添加索引属性
        sql.append(" ASYNC");  // 异步创建索引以提高性能
        
        return sql.toString();
    }

    @Override
    public String getDropIndexSql(String tableName, String indexName) {
        return "DROP INDEX IF EXISTS " + wrapIdentifier(indexName) + 
               " ON " + wrapIdentifier(tableName);
    }

    @Override
    public String getAlterTableEngineSql(String tableName, String engine) {
        // Phoenix 不支持修改存储引擎
        return "";
    }

    @Override
    public String getAlterTableCharsetSql(String tableName, String charset, String collate) {
        // Phoenix 不支持修改字符集
        return "";
    }

    @Override
    public String formatFieldDefinition(String fieldName, String fieldType, 
            Integer length, Integer precision, Integer scale,
            boolean nullable, String defaultValue, String comment) {
        StringBuilder sb = new StringBuilder();
        sb.append(wrapIdentifier(fieldName)).append(" ");
        
        fieldType = fieldType.toUpperCase();
        sb.append(fieldType);
        
        // 添加长度/精度
        if (fieldType.equals("VARCHAR") || fieldType.equals("CHAR") || 
            fieldType.equals("BINARY") || fieldType.equals("VARBINARY")) {
            if (length != null) {
                sb.append("(").append(length).append(")");
            } else {
                // Phoenix 要求这些类型必须有长度
                sb.append("(").append(DEFAULT_VARCHAR_LENGTH).append(")");
            }
        } else if (fieldType.equals("DECIMAL")) {
            if (precision != null) {
                sb.append("(").append(precision);
                if (scale != null) {
                    sb.append(",").append(scale);
                } else {
                    sb.append(",0");
                }
                sb.append(")");
            }
        }
        
        // Phoenix 对于标记为 NOT NULL 的列有严格限制
        if (!nullable) {
            // Phoenix 不能将 NULL 作为默认值
            if (defaultValue != null && defaultValue.equalsIgnoreCase("NULL")) {
                // 移除冲突的默认值
                defaultValue = null;
            }
            sb.append(" NOT NULL");
        }
        
        // Phoenix 的默认值支持非常有限
        if (defaultValue != null) {
            // 检查默认值是否是Phoenix支持的
            if (isValidPhoenixDefaultValue(fieldType, defaultValue)) {
                sb.append(" DEFAULT ").append(defaultValue);
            }
        }
        
        // Phoenix 不支持列注释
        return sb.toString();
    }

    private boolean isValidPhoenixDefaultValue(String fieldType, String defaultValue) {
        fieldType = fieldType.toUpperCase();
        // Phoenix 只支持以下几种类型的默认值
        if ((fieldType.equals("INTEGER") || fieldType.equals("BIGINT") || 
             fieldType.equals("TINYINT") || fieldType.equals("SMALLINT") ||
             fieldType.equals("FLOAT") || fieldType.equals("DOUBLE") || 
             fieldType.equals("DECIMAL")) && defaultValue.matches("-?\\d+(\\.\\d+)?")) {
            return true;
        }
        if (fieldType.equals("BOOLEAN") && 
            (defaultValue.equalsIgnoreCase("true") || defaultValue.equalsIgnoreCase("false"))) {
            return true;
        }
        if ((fieldType.equals("VARCHAR") || fieldType.equals("CHAR")) && 
            (defaultValue.startsWith("'") && defaultValue.endsWith("'"))) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isValidFieldLength(String type, int length) {
        type = type.toUpperCase();
        
        switch (type) {
            case "VARCHAR":
            case "CHAR":
            case "VARBINARY":
            case "BINARY":
                // Phoenix这些类型的最大长度为1,000,000
                return length > 0 && length <= 1000000;
            case "DECIMAL":
            case "NUMERIC":
                // Phoenix DECIMAL的最大精度为38
                return length > 0 && length <= 38;
            default:
                // 其他类型不需要长度
                return true;
        }
    }

    @Override
    public String getCreateTableSql(String tableName, List<ColumnDefinition> columns, 
                                  String tableComment, String engine, String charset, String collate) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ");
        
        // Phoenix 不支持 IF NOT EXISTS 语法
        // sql.append("IF NOT EXISTS ");
        
        sql.append(wrapIdentifier(tableName));
        sql.append(" (\n");

        List<String> columnDefinitions = new ArrayList<>();
        List<String> primaryKeys = new ArrayList<>();

        for (ColumnDefinition column : columns) {
            StringBuilder columnSql = new StringBuilder();
            columnSql.append("  ").append(wrapIdentifier(column.getName())).append(" ");
            
            String type = column.getType().toUpperCase();
            columnSql.append(type);
            
            // 添加长度/精度
            if (type.equals("VARCHAR") || type.equals("CHAR") || 
                type.equals("BINARY") || type.equals("VARBINARY")) {
                if (column.getLength() != null) {
                    columnSql.append("(").append(column.getLength()).append(")");
                }
            } else if (type.equals("DECIMAL")) {
                if (column.getPrecision() != null) {
                    columnSql.append("(")
                           .append(column.getPrecision())
                           .append(",")
                           .append(column.getScale() != null ? column.getScale() : 0)
                           .append(")");
                }
            }
            
            // 添加NOT NULL约束
            if (!column.isNullable()) {
                columnSql.append(" NOT NULL");
            }
            
            // Phoenix 的默认值语法有特殊限制
            if (column.getDefaultValue() != null) {
                // Phoenix 支持少量默认值类型
                String defaultValue = column.getDefaultValue();
                if (defaultValue.equalsIgnoreCase("CURRENT_DATE") || 
                    defaultValue.equalsIgnoreCase("CURRENT_TIME") || 
                    defaultValue.equalsIgnoreCase("CURRENT_TIMESTAMP")) {
                    // 不支持这些默认值，删除它们
                } else if (defaultValue.equals("NULL") && !column.isNullable()) {
                    // 对于非空列，不能默认为NULL
                } else if (defaultValue.equals("''") && (type.equals("VARCHAR") || type.equals("CHAR"))) {
                    columnSql.append(" DEFAULT ''");
                } else if (defaultValue.matches("-?\\d+") && 
                          (type.equals("INTEGER") || type.equals("BIGINT") || type.equals("TINYINT") || 
                           type.equals("SMALLINT") || type.equals("FLOAT") || type.equals("DOUBLE") || 
                           type.equals("DECIMAL"))) {
                    columnSql.append(" DEFAULT ").append(defaultValue);
                } else if ((defaultValue.equals("true") || defaultValue.equals("false")) && 
                          type.equals("BOOLEAN")) {
                    columnSql.append(" DEFAULT ").append(defaultValue);
                }
                // 其他类型不支持默认值
            }
            
            // Phoenix 不支持列注释
            
            // 收集主键列
            if (column.isPrimaryKey()) {
                primaryKeys.add(wrapIdentifier(column.getName()));
            }
            
            columnDefinitions.add(columnSql.toString());
        }
        
        // 添加主键约束 - Phoenix 需要主键定义
        if (!primaryKeys.isEmpty()) {
            columnDefinitions.add("  CONSTRAINT PK PRIMARY KEY (" + String.join(", ", primaryKeys) + ")");
        } else {
            // Phoenix 要求表必须有主键
            throw new IllegalArgumentException("Phoenix requires a primary key for table creation");
        }
        
        sql.append(String.join(",\n", columnDefinitions));
        sql.append("\n)");
        
        // Phoenix 特有的表属性
        List<String> tableOptions = new ArrayList<>();
        
        // SALT_BUCKETS 选项 - 用于预分区
        if (engine != null && engine.toUpperCase().startsWith("SALT_BUCKETS=")) {
            tableOptions.add(engine);
        } else {
            // 默认添加 SALT_BUCKETS=10 以提高性能
            tableOptions.add("SALT_BUCKETS=10");
        }
        
        // DATA_BLOCK_ENCODING 选项
        tableOptions.add("DATA_BLOCK_ENCODING='FAST_DIFF'");
        
        // COMPRESSION 选项
        tableOptions.add("COMPRESSION='GZ'");
        
        // TTL 选项 - 如果指定
        if (charset != null && charset.toUpperCase().startsWith("TTL=")) {
            tableOptions.add(charset);
        }
        
        // 添加表选项
        if (!tableOptions.isEmpty()) {
            sql.append(" ").append(String.join(", ", tableOptions));
        }
        
        return sql.toString();
    }

    @Override
    public Map<String, Integer> getDefaultLengthMapping() {
        Map<String, Integer> lengthMapping = new HashMap<>();
        lengthMapping.put("VARCHAR", DEFAULT_VARCHAR_LENGTH);
        lengthMapping.put("CHAR", 1);
        lengthMapping.put("DECIMAL", 10);
        return lengthMapping;
    }

    @Override
    public String convertFromOtherDbType(String sourceType, String sourceDbType) {
        sourceType = sourceType.toUpperCase();
        
        // Phoenix特定类型映射
        switch (sourceType) {
            case "VARCHAR":
            case "VARCHAR2":
            case "NVARCHAR":
            case "NVARCHAR2":
                return "VARCHAR";
            case "CHAR":
            case "NCHAR":
                return "CHAR";
            case "TEXT":
            case "CLOB":
            case "NCLOB":
            case "LONGTEXT":
            case "MEDIUMTEXT":
                return "VARCHAR";
            case "INTEGER":
            case "INT":
            case "INT4":
                return "INTEGER";
            case "BIGINT":
            case "INT8":
                return "BIGINT";
            case "SMALLINT":
            case "INT2":
                return "SMALLINT";
            case "TINYINT":
            case "INT1":
                return "TINYINT";
            case "FLOAT":
            case "REAL":
                return "FLOAT";
            case "DOUBLE":
            case "DOUBLE PRECISION":
                return "DOUBLE";
            case "DECIMAL":
            case "NUMERIC":
                return "DECIMAL";
            case "BOOLEAN":
            case "BIT":
                return "BOOLEAN";
            case "DATE":
                return "DATE";
            case "TIME":
                return "TIME";
            case "TIMESTAMP":
            case "DATETIME":
                return "TIMESTAMP";
            case "BLOB":
            case "BINARY":
            case "VARBINARY":
            case "LONGBLOB":
            case "MEDIUMBLOB":
                return "VARBINARY";
            default:
                // 对于不支持的类型，使用 VARCHAR
                return "VARCHAR";
        }
    }

    @Override
    public String getSchema() {
        // Phoenix 没有真正的 schema 概念
        // 但可以使用表名前缀作为命名空间
        return dataSource.getDbName() != null ? dataSource.getDbName() : "";
    }

    @Override
    public String getDefaultSchema() {
        return "";  // Phoenix 不使用 schema
    }

    @Override
    public void setSchema(Connection conn, String schema) throws SQLException {
        // Phoenix 不支持 SET SCHEMA 命令
        // 不进行任何操作
    }

    @Override
    public String getTableExistsSql(String tableName) {
        // Phoenix系统表中检查表是否存在
        return "SELECT COUNT(*) FROM SYSTEM.CATALOG WHERE TABLE_NAME = '" + 
               tableName.toUpperCase() + "' AND COLUMN_NAME IS NULL";
    }

    @Override
    public String getDropTableSql(String tableName) {
        // Phoenix 支持标准的 DROP TABLE 语法
        return "DROP TABLE " + wrapIdentifier(tableName);
    }

    @Override
    public String getTruncateTableSql(String tableName) {
        // Phoenix 不支持 TRUNCATE TABLE 命令
        // 使用 DELETE FROM 替代
        return "DELETE FROM " + wrapIdentifier(tableName);
    }

    @Override
    public String getRenameTableSql(String oldTableName, String newTableName) {
        // Phoenix不支持重命名表
        throw new UnsupportedOperationException("Phoenix does not support renaming tables.");
    }

    @Override
    public String getShowCreateTableSql(String tableName) {
        // Phoenix 没有直接的 SHOW CREATE TABLE 语法
        return "!describe " + wrapIdentifier(tableName);
    }

    @Override
    public String getAddColumnSql(String tableName, String columnDefinition) {
        return "ALTER TABLE " + wrapIdentifier(tableName) + " ADD " + columnDefinition;
    }

    @Override
    public String getAddColumnSql(String tableName, ColumnDefinition column, String afterColumn) {
        // Phoenix 不支持指定列顺序的 AFTER 子句
        if (afterColumn != null) {
            log.warn("Phoenix does not support AFTER clause in ADD COLUMN. Ignoring afterColumn parameter.");
        }
        
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(wrapIdentifier(tableName))
           .append(" ADD ").append(wrapIdentifier(column.getName()))
           .append(" ").append(column.getType());
        
        // 添加长度/精度信息
        if (column.getLength() != null) {
            if (column.getType().toUpperCase().equals("VARCHAR") || 
                column.getType().toUpperCase().equals("CHAR") ||
                column.getType().toUpperCase().equals("BINARY") || 
                column.getType().toUpperCase().equals("VARBINARY")) {
                sql.append("(").append(column.getLength()).append(")");
            }
        } else if (column.getPrecision() != null && 
                  (column.getType().toUpperCase().equals("DECIMAL") || 
                   column.getType().toUpperCase().equals("NUMERIC"))) {
            sql.append("(").append(column.getPrecision());
            if (column.getScale() != null) {
                sql.append(",").append(column.getScale());
            }
            sql.append(")");
        }
        
        // 添加非空约束 - Phoenix仅支持非空列
        if (!column.isNullable()) {
            sql.append(" NOT NULL");
        }
        
        // Phoenix列添加不支持默认值
        // 且不支持列注释
        
        return sql.toString();
    }

    @Override
    public String getModifyColumnSql(String tableName, String columnName, String newDefinition) {
        // Phoenix 不支持更改列类型，只支持设置为 NOT NULL
        if (newDefinition.toUpperCase().contains("NOT NULL")) {
            return "ALTER TABLE " + wrapIdentifier(tableName) + 
                   " ALTER COLUMN " + wrapIdentifier(columnName) + " SET NOT NULL";
        }
        
        // 明确提示不支持
        throw new UnsupportedOperationException(
            "Phoenix only supports modifying columns to add NOT NULL constraint. " +
            "Column type modifications are not supported."
        );
    }

    @Override
    public String getDropColumnSql(String tableName, String columnName) {
        return "ALTER TABLE " + wrapIdentifier(tableName) + " DROP COLUMN " + wrapIdentifier(columnName);
    }

    @Override
    public String getCreateIndexSql(String tableName, String indexName, String[] columns) {
        return String.format("CREATE INDEX %s ON %s (%s)",
            wrapIdentifier(indexName),
            wrapIdentifier(tableName),
            String.join(", ", Arrays.stream(columns)
                .map(this::wrapIdentifier)
                .collect(Collectors.toList()))
        );
    }

    @Override
    public String getDefaultTextType() {
        return "VARCHAR";  // Phoenix 使用 VARCHAR 作为文本类型
    }

    @Override
    public String getDefaultIntegerType() {
        return "INTEGER";
    }

    @Override
    public String getDefaultDecimalType() {
        return "DECIMAL(10,2)";
    }

    @Override
    public String getDefaultDateType() {
        return "DATE";
    }

    @Override
    public String getDefaultTimeType() {
        return "TIME";
    }

    @Override
    public String getDefaultDateTimeType() {
        return "TIMESTAMP";
    }

    @Override
    public String getDefaultBooleanType() {
        return "BOOLEAN";
    }

    @Override
    public String getDefaultBlobType() {
        return "BINARY";
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
    public String mapJavaTypeToDbType(Class<?> javaType) {
        if (String.class.equals(javaType)) {
            return "VARCHAR";
        }
        if (Integer.class.equals(javaType) || int.class.equals(javaType)) {
            return "INTEGER";
        }
        if (Long.class.equals(javaType) || long.class.equals(javaType)) {
            return "BIGINT";
        }
        if (Double.class.equals(javaType) || double.class.equals(javaType)) {
            return "DOUBLE";
        }
        if (Float.class.equals(javaType) || float.class.equals(javaType)) {
            return "FLOAT";
        }
        if (java.math.BigDecimal.class.equals(javaType)) {
            return "DECIMAL";
        }
        if (Boolean.class.equals(javaType) || boolean.class.equals(javaType)) {
            return "BOOLEAN";
        }
        if (java.sql.Date.class.equals(javaType)) {
            return "DATE";
        }
        if (java.sql.Time.class.equals(javaType)) {
            return "TIME";
        }
        if (java.sql.Timestamp.class.equals(javaType) || Date.class.equals(javaType)) {
            return "TIMESTAMP";
        }
        if (byte[].class.equals(javaType)) {
            return "VARBINARY";
        }
        // Phoenix 不支持复杂类型
        return "VARCHAR";
    }

    @Override
    public Class<?> mapDbTypeToJavaType(String dbType) {
        dbType = dbType.toUpperCase();
        if (dbType.contains("VARCHAR") || dbType.contains("CHAR")) {
            return String.class;
        }
        if (dbType.equals("INTEGER")) {
            return Integer.class;
        }
        if (dbType.equals("BIGINT")) {
            return Long.class;
        }
        if (dbType.equals("FLOAT")) {
            return Float.class;
        }
        if (dbType.equals("DOUBLE")) {
            return Double.class;
        }
        if (dbType.startsWith("DECIMAL")) {
            return java.math.BigDecimal.class;
        }
        if (dbType.equals("DATE")) {
            return java.sql.Date.class;
        }
        if (dbType.equals("TIME")) {
            return java.sql.Time.class;
        }
        if (dbType.equals("TIMESTAMP")) {
            return java.sql.Timestamp.class;
        }
        if (dbType.equals("BINARY")) {
            return byte[].class;
        }
        if (dbType.equals("BOOLEAN")) {
            return Boolean.class;
        }
        return String.class;
    }

    @Override
    public String getQuoteString() {
        return "\"";  // Phoenix 使用双引号作为标识符引用符
    }

    @Override
    public List<String> getSystemDatabases() {
        return Arrays.asList("SYSTEM");  // Phoenix 的系统数据库
    }

    @Override
    public String wrapValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        
        if (value instanceof Number) {
            // Phoenix 要求数值类型保持原样，不需要引号
            return value.toString();
        }
        
        if (value instanceof Boolean) {
            // Phoenix 布尔值使用真实的布尔值而非字符串
            return ((Boolean) value) ? "true" : "false";
        }
        
        if (value instanceof java.sql.Date) {
            // Phoenix 日期格式
            return "TO_DATE('" + value.toString() + "', 'yyyy-MM-dd')";
        }
        
        if (value instanceof java.sql.Time) {
            // Phoenix 时间格式
            return "TO_TIME('" + value.toString() + "', 'HH:mm:ss')";
        }
        
        if (value instanceof java.sql.Timestamp) {
            // Phoenix 时间戳格式，没有时区信息
            return "TO_TIMESTAMP('" + value.toString() + "', 'yyyy-MM-dd HH:mm:ss.SSS')";
        }
        
        // 添加对字节数组的特殊处理
        if (value instanceof byte[]) {
            byte[] bytes = (byte[]) value;
            StringBuilder hex = new StringBuilder("X'");
            for (byte b : bytes) {
                hex.append(String.format("%02X", b));
            }
            hex.append("'");
            return hex.toString();
        }
        
        // 默认情况，Phoenix 对字符串的处理：必须使用单引号，并需要转义
        return "'" + value.toString().replace("'", "''") + "'";
    }

    @Override
    public String wrapIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
        
        // Phoenix表名和列名默认是大写的
        // 只有双引号包裹的标识符才保持大小写敏感
        
        // 如果包含特殊字符或不是全大写，则需要引号
        if (containsSpecialChar(identifier) || !identifier.equals(identifier.toUpperCase())) {
            return "\"" + identifier.replace("\"", "\"\"") + "\"";
        }
        
        return identifier;
    }

    private boolean containsSpecialChar(String identifier) {
        // Phoenix保留关键字和特殊字符检查
        String[] phoenixKeywords = { "SELECT", "FROM", "WHERE", "UPSERT", "DELETE", "ALTER", 
                                    "CREATE", "DROP", "INDEX", "TABLE", "VIEW", "PRIMARY", "KEY" };
        
        for (String keyword : phoenixKeywords) {
            if (identifier.toUpperCase().equals(keyword)) {
                return true;
            }
        }
        
        // 检查特殊字符
        return identifier.contains(" ") || identifier.contains("-") || 
               identifier.contains(".") || identifier.contains("(") || 
               identifier.contains("\"") || identifier.contains("'");
    }

    @Override
    public String generateCreateTableSql(TableDefinition tableDefinition) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ");
        
        // Phoenix不支持IF NOT EXISTS语法
        
        // 表名处理
        sql.append(wrapIdentifier(tableDefinition.getTableName()));
        sql.append(" (\n");

        List<String> columnDefinitions = new ArrayList<>();
        List<String> primaryKeys = new ArrayList<>();

        // 列定义处理
        for (ColumnDefinition column : tableDefinition.getColumns()) {
            StringBuilder columnSql = new StringBuilder();
            columnSql.append("  ").append(wrapIdentifier(column.getName())).append(" ");
            
            // 调整列类型
            adjustHBaseColumnType(column);
            String type = column.getType().toUpperCase();
            columnSql.append(type);
            
            // 添加长度/精度
            if ((type.equals("VARCHAR") || type.equals("CHAR") || 
                type.equals("BINARY") || type.equals("VARBINARY")) && 
                column.getLength() != null) {
                columnSql.append("(").append(column.getLength()).append(")");
            } else if (type.equals("DECIMAL") && column.getPrecision() != null) {
                columnSql.append("(").append(column.getPrecision());
                if (column.getScale() != null) {
                    columnSql.append(",").append(column.getScale());
                }
                columnSql.append(")");
            }
            
            // 添加非空约束
            if (!column.isNullable()) {
                columnSql.append(" NOT NULL");
            }
            
            // Phoenix的默认值支持非常有限
            if (column.getDefaultValue() != null) {
                String defaultValue = column.getDefaultValue();
                // 只有在确认是Phoenix支持的默认值时才添加
                if (isValidPhoenixDefaultValue(type, defaultValue)) {
                    columnSql.append(" DEFAULT ").append(defaultValue);
                }
            }
            
            // Phoenix不支持列注释
            
            // 收集主键列
            if (column.isPrimaryKey()) {
                primaryKeys.add(wrapIdentifier(column.getName()));
            }
            
            columnDefinitions.add(columnSql.toString());
        }
        
        // Phoenix必须有主键定义
        if (primaryKeys.isEmpty()) {
            throw new IllegalArgumentException("Phoenix tables must have a primary key defined");
        }
        
        // 添加主键约束
        columnDefinitions.add("  CONSTRAINT PK PRIMARY KEY (" + 
                            String.join(", ", primaryKeys) + ")");
        
        sql.append(String.join(",\n", columnDefinitions));
        sql.append("\n)");
        
        // 处理表属性
        Map<String, String> extraProps = tableDefinition.getExtraProperties();
        List<String> tableOptions = new ArrayList<>();
        
        // 处理盐溶表配置 (SALT_BUCKETS)
        if (extraProps != null && extraProps.containsKey("SALT_BUCKETS")) {
            tableOptions.add("SALT_BUCKETS=" + extraProps.get("SALT_BUCKETS"));
        } else {
            // 默认添加10个盐溶桶
            tableOptions.add("SALT_BUCKETS=10");
        }
        
        // 处理压缩选项
        if (extraProps != null && extraProps.containsKey("COMPRESSION")) {
            tableOptions.add("COMPRESSION='" + extraProps.get("COMPRESSION") + "'");
        } else {
            // 默认使用GZ压缩
            tableOptions.add("COMPRESSION='GZ'");
        }
        
        // 处理数据块编码
        if (extraProps != null && extraProps.containsKey("DATA_BLOCK_ENCODING")) {
            tableOptions.add("DATA_BLOCK_ENCODING='" + extraProps.get("DATA_BLOCK_ENCODING") + "'");
        } else {
            // 默认使用FAST_DIFF编码
            tableOptions.add("DATA_BLOCK_ENCODING='FAST_DIFF'");
        }
        
        // 处理TTL (Time-To-Live)选项
        if (extraProps != null && extraProps.containsKey("TTL")) {
            tableOptions.add("TTL=" + extraProps.get("TTL"));
        }
        
        // 添加所有表选项
        if (!tableOptions.isEmpty()) {
            sql.append(" ").append(String.join(", ", tableOptions));
        }
        
        return sql.toString();
    }

    @Override
    public TableDefinition parseCreateTableSql(String createTableSql) {
        TableDefinition tableDefinition = new TableDefinition();
        List<ColumnDefinition> columns = new ArrayList<>();

        try {
            // Parse table name
            Pattern tablePattern = Pattern.compile("create\\s+'([^']+)'", Pattern.CASE_INSENSITIVE);
            Matcher tableMatcher = tablePattern.matcher(createTableSql);
            if (tableMatcher.find()) {
                tableDefinition.setTableName(tableMatcher.group(1));
            }

            // Parse column family definitions
            Pattern cfPattern = Pattern.compile("COLUMN_FAMILY\\s*=\\s*'([^']+)'", Pattern.CASE_INSENSITIVE);
            Matcher cfMatcher = cfPattern.matcher(createTableSql);
            
            while (cfMatcher.find()) {
                String columnFamily = cfMatcher.group(1);
                
                // Create a column definition for each column family
                ColumnDefinition column = new ColumnDefinition();
                column.setName(columnFamily);
                column.setType("VARCHAR"); // Default type for HBase columns
                column.setNullable(true);
                columns.add(column);
            }

            tableDefinition.setColumns(columns);
            return tableDefinition;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse HBase CREATE TABLE SQL: " + e.getMessage(), e);
        }
    }

    @Override
    public String convertCreateTableSql(String sourceSql, String sourceDbType) {
        try {
            TableDefinition tableDefinition = parseCreateTableSql(sourceSql);
            
            // 转换列类型
            for (ColumnDefinition column : tableDefinition.getColumns()) {
                String sourceType = column.getType();
                String targetType = convertFromOtherDbType(sourceType, sourceDbType);
                column.setType(targetType);
                
                // 调整HBase特定的列类型
                adjustHBaseColumnType(column);
            }
            
            // 生成HBase建表语句
            return generateCreateTableSql(tableDefinition);
        } catch (Exception e) {
            log.error("Failed to convert CREATE TABLE SQL from {} to HBase: {}", sourceDbType, e.getMessage());
            throw new RuntimeException("Failed to convert CREATE TABLE SQL", e);
        }
    }

    private void adjustHBaseColumnType(ColumnDefinition column) {
        String type = column.getType().toUpperCase();
        
        // VARCHAR 类型处理 - Phoenix 中 VARCHAR 需要长度限制
        if (type.equals("VARCHAR")) {
            if (column.getLength() == null) {
                column.setLength(DEFAULT_VARCHAR_LENGTH);
            }
            // Phoenix VARCHAR 最大长度为 1,000,000
            if (column.getLength() > 1000000) {
                column.setLength(1000000);
            }
        }
        // CHAR 类型处理
        else if (type.equals("CHAR")) {
            if (column.getLength() == null) {
                column.setLength(1);
            }
            // Phoenix CHAR 最大长度为 1,000,000
            if (column.getLength() > 1000000) {
                column.setLength(1000000);
            }
        }
        // BINARY 和 VARBINARY 类型处理
        else if (type.equals("BINARY") || type.equals("VARBINARY")) {
            if (column.getLength() == null) {
                column.setLength(1);
            }
            // Phoenix BINARY/VARBINARY 最大长度为 1,000,000
            if (column.getLength() > 1000000) {
                column.setLength(1000000);
            }
        }
        // DECIMAL 类型处理
        else if (type.equals("DECIMAL")) {
            if (column.getPrecision() == null) {
                column.setPrecision(10);
            }
            if (column.getScale() == null) {
                column.setScale(0);
            }
            // Phoenix DECIMAL 最大精度为 38
            column.setPrecision(Math.min(column.getPrecision(), 38));
            column.setScale(Math.min(column.getScale(), column.getPrecision()));
        }
        // Phoenix 不支持 TEXT 类型，转换为 VARCHAR
        else if (type.equals("TEXT") || type.equals("CLOB") || type.equals("LONGTEXT")) {
            column.setType("VARCHAR");
            if (column.getLength() == null || column.getLength() > 1000000) {
                column.setLength(1000000);
            }
        }
        // Phoenix 不支持 BLOB 类型，转换为 VARBINARY
        else if (type.equals("BLOB") || type.equals("LONGBLOB")) {
            column.setType("VARBINARY");
            if (column.getLength() == null || column.getLength() > 1000000) {
                column.setLength(1000000);
            }
        }
        // Phoenix 使用特殊的 INTEGER 自增列
        else if (type.equals("IDENTITY") || type.equals("SERIAL")) {
            column.setType("INTEGER");
            column.setAutoIncrement(true);
        }
        // Phoenix 支持 TIMESTAMP，但需确保不带时区信息
        else if (type.equals("TIMESTAMP") && column.getScale() != null) {
            // Phoenix 不支持 TIMESTAMP 精度设置
            column.setScale(null);
        }
    }

    @Override
    public List<TableDefinition> getAllTables(String database) throws Exception {
        String sql = "SELECT TABLE_NAME, " +
                    "TABLE_TYPE, " +
                    "REMARKS as TABLE_COMMENT " +
                    "FROM SYSTEM.CATALOG " +
                    "WHERE TABLE_SCHEM = ? " +
                    "AND TABLE_TYPE = 'TABLE' " +
                    "GROUP BY TABLE_NAME, TABLE_TYPE, REMARKS";
        
        try (Connection conn = getConnection()) {
            List<TableDefinition> tables = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, database);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        TableDefinition table = TableDefinition.builder()
                            .tableName(rs.getString("TABLE_NAME"))
                            .tableComment(rs.getString("TABLE_COMMENT"))
                            .build();
                        tables.add(table);
                    }
                }
            }
            return tables;
        }
    }

    @Override
    public List<ColumnDefinition> getTableColumns(String database, String tableName) throws Exception {
        String sql = "SELECT COLUMN_NAME, " +
                    "DATA_TYPE as COLUMN_TYPE, " +
                    "COLUMN_SIZE as LENGTH, " +
                    "DECIMAL_DIGITS as SCALE, " +
                    "IS_NULLABLE, " +
                    "COLUMN_DEF as COLUMN_DEFAULT, " +
                    "REMARKS as COLUMN_COMMENT, " +
                    "KEY_SEQ " +
                    "FROM SYSTEM.CATALOG " +
                    "WHERE TABLE_SCHEM = ? " +
                    "AND TABLE_NAME = ? " +
                    "ORDER BY ORDINAL_POSITION";
        
        try (Connection conn = getConnection()) {
            List<ColumnDefinition> columns = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, database);
                stmt.setString(2, tableName);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String columnName = rs.getString("COLUMN_NAME");
                        
                        // 获取列类型信息
                        String type = rs.getString("COLUMN_TYPE");
                        Integer length = rs.getInt("LENGTH");
                        if (rs.wasNull()) {
                            length = null;
                        }
                        
                        Integer scale = rs.getInt("SCALE");
                        if (rs.wasNull()) {
                            scale = null;
                        }
                        
                        // 构建格式化的类型字符串（这里只是为了后续展示使用，不影响实际的类型存储）
                        String formattedType = type;
                        if (type.contains("CHAR") && length != null && length > 0) {
                            formattedType = type + "(" + length + ")";
                        } else if (type.equals("DECIMAL")) {
                            if (scale != null && scale > 0) {
                                formattedType = type + "(" + length + "," + scale + ")";
                            } else if (length != null) {
                                formattedType = type + "(" + length + ")";
                            }
                        }
                        
                        // 是否可为空
                        boolean isNullable = "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE"));
                        
                        // 是否为主键
                        boolean isPrimaryKey = rs.getInt("KEY_SEQ") > 0;
                        
                        // 默认值
                        String defaultValue = rs.getString("COLUMN_DEFAULT");
                        
                        // 注释
                        String comment = rs.getString("COLUMN_COMMENT");
                        
                        ColumnDefinition column = ColumnDefinition.builder()
                            .name(columnName)
                            .type(type)
                            .length(length)
                            .scale(scale)
                            .nullable(isNullable)
                            .defaultValue(defaultValue)
                            .comment(comment)
                            .primaryKey(isPrimaryKey)
                            .build();
                            
                        columns.add(column);
                    }
                }
            }
            return columns;
        }
    }

    @Override
    public List<Map<String, Object>> getTableDependencies(String tableName) throws Exception {
        List<Map<String, Object>> dependencies = new ArrayList<>();
        
        try (Connection conn = getConnection()) {
            // 在HBase/Phoenix中，表之间的依赖主要通过以下方式体现：
            // 1. 外键关系（如果定义了）
            // 2. 索引表依赖于主表
            // 3. 视图依赖于基表
            
            // 1. 获取外键依赖
            try {
                List<Map<String, Object>> foreignKeys = getForeignKeys(tableName);
                for (Map<String, Object> fk : foreignKeys) {
                    Map<String, Object> dependency = new HashMap<>();
                    dependency.put("dependencyType", "FOREIGN_KEY");
                    dependency.put("objectName", fk.get("referencedTable"));
                    dependency.put("objectType", "TABLE");
                    dependency.put("columnName", fk.get("columnName"));
                    dependency.put("referencedColumn", fk.get("referencedColumn"));
                    dependency.put("constraintName", fk.get("constraintName"));
                    dependencies.add(dependency);
                }
            } catch (Exception e) {
                log.warn("获取外键依赖失败: {}", e.getMessage());
                // 继续执行，不中断流程
            }
            
            // 2. 获取索引依赖
            try {
                // 查询依赖于此表的索引
                String indexSql = 
                    "SELECT INDEX_NAME, TABLE_NAME, INDEX_TYPE " +
                    "FROM SYSTEM.CATALOG " +
                    "WHERE DATA_TABLE_NAME = ? " +
                    "AND INDEX_TYPE IS NOT NULL " +
                    "GROUP BY INDEX_NAME, TABLE_NAME, INDEX_TYPE";
                
                try (PreparedStatement stmt = conn.prepareStatement(indexSql)) {
                    stmt.setString(1, tableName.toUpperCase());
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            Map<String, Object> dependency = new HashMap<>();
                            dependency.put("dependencyType", "INDEX");
                            dependency.put("objectName", rs.getString("INDEX_NAME"));
                            dependency.put("objectType", "INDEX");
                            dependency.put("indexType", rs.getString("INDEX_TYPE"));
                            dependencies.add(dependency);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("获取索引依赖失败: {}", e.getMessage());
                // 继续执行，不中断流程
            }
            
            // 3. 获取视图依赖
            try {
                // 查询依赖于此表的视图
                String viewSql = 
                    "SELECT TABLE_NAME, TABLE_TYPE " +
                    "FROM SYSTEM.CATALOG " +
                    "WHERE BASE_TABLE_NAME = ? " +
                    "AND TABLE_TYPE = 'v' " +
                    "GROUP BY TABLE_NAME, TABLE_TYPE";
                
                try (PreparedStatement stmt = conn.prepareStatement(viewSql)) {
                    stmt.setString(1, tableName.toUpperCase());
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            Map<String, Object> dependency = new HashMap<>();
                            dependency.put("dependencyType", "VIEW");
                            dependency.put("objectName", rs.getString("TABLE_NAME"));
                            dependency.put("objectType", "VIEW");
                            dependencies.add(dependency);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("获取视图依赖失败: {}", e.getMessage());
                // 继续执行，不中断流程
            }
            
            // 4. 获取表的列族信息（HBase特有）
            try {
                // 查询表的列族信息
                String columnFamilySql = 
                    "SELECT DISTINCT COLUMN_FAMILY " +
                    "FROM SYSTEM.CATALOG " +
                    "WHERE TABLE_NAME = ? " +
                    "AND COLUMN_FAMILY IS NOT NULL";
                
                try (PreparedStatement stmt = conn.prepareStatement(columnFamilySql)) {
                    stmt.setString(1, tableName.toUpperCase());
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            String columnFamily = rs.getString("COLUMN_FAMILY");
                            if (columnFamily != null && !columnFamily.isEmpty()) {
                                Map<String, Object> dependency = new HashMap<>();
                                dependency.put("dependencyType", "COLUMN_FAMILY");
                                dependency.put("objectName", columnFamily);
                                dependency.put("objectType", "COLUMN_FAMILY");
                                dependencies.add(dependency);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("获取列族信息失败: {}", e.getMessage());
                // 继续执行，不中断流程
            }
            
            // 5. 获取协处理器依赖（HBase特有）
            try {
                // 在Phoenix中，协处理器信息不直接存储在系统表中
                // 这里我们可以通过HBase Admin API获取，但这需要直接使用HBase API
                // 由于当前实现基于JDBC，我们提供一个简化的实现
                
                // 注意：这是一个模拟实现，实际应用中可能需要使用HBase Admin API
                String coprocessorSql = 
                    "SELECT TABLE_NAME " +
                    "FROM SYSTEM.CATALOG " +
                    "WHERE TABLE_NAME = ?";
                
                try (PreparedStatement stmt = conn.prepareStatement(coprocessorSql)) {
                    stmt.setString(1, tableName.toUpperCase());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            // 这里我们假设表可能有默认的协处理器
                            // 实际情况需要通过HBase Admin API查询
                            Map<String, Object> dependency = new HashMap<>();
                            dependency.put("dependencyType", "COPROCESSOR");
                            dependency.put("objectName", "DefaultCoprocessor");
                            dependency.put("objectType", "COPROCESSOR");
                            dependency.put("note", "This is a placeholder. Actual coprocessors should be queried using HBase Admin API.");
                            dependencies.add(dependency);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("获取协处理器信息失败: {}", e.getMessage());
                // 继续执行，不中断流程
            }
            
        } catch (Exception e) {
            log.error("获取表依赖失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        
        return dependencies;
    }

    @Override
    public String getDatabaseType() {
        return "hbase";
    }

    @Override
    public String getDatabaseName() {
        // HBase/Phoenix不使用传统的数据库名称概念
        // 返回连接字符串中的ZooKeeper地址作为标识
        return dataSource.getHost() + ":" + 
               (dataSource.getPort() != null ? dataSource.getPort() : getDefaultPort());
    }

    @Override
    public List<String> listTables() throws Exception {
        List<String> tables = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT TABLE_NAME FROM SYSTEM.CATALOG " +
                "WHERE TABLE_TYPE = 'u' " +  // 'u' 表示用户表
                "GROUP BY TABLE_NAME")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
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
            // 获取表的基本信息
            String sql = "SELECT TABLE_NAME, TABLE_TYPE, REMARKS, " +
                        "LAST_DDL_TIMESTAMP " +
                        "FROM SYSTEM.CATALOG " +
                        "WHERE TABLE_NAME = ? " +
                        "AND COLUMN_NAME IS NULL " +
                        "LIMIT 1";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        tableInfo.put("tableName", rs.getString("TABLE_NAME"));
                        tableInfo.put("tableType", rs.getString("TABLE_TYPE"));
                        tableInfo.put("comment", rs.getString("REMARKS"));
                        tableInfo.put("createTime", rs.getTimestamp("LAST_DDL_TIMESTAMP"));
                    } else {
                        throw new Exception("Table not found: " + tableName);
                    }
                }
            }
            
            // 获取表的列数
            String columnCountSql = "SELECT COUNT(*) AS COLUMN_COUNT " +
                                   "FROM SYSTEM.CATALOG " +
                                   "WHERE TABLE_NAME = ? " +
                                   "AND COLUMN_NAME IS NOT NULL";
            
            try (PreparedStatement stmt = conn.prepareStatement(columnCountSql)) {
                stmt.setString(1, tableName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        tableInfo.put("columnCount", rs.getInt("COLUMN_COUNT"));
                    }
                }
            }
            
            // 获取表的行数（HBase不直接支持COUNT(*)，这里使用近似值）
            try {
                Long rowCount = getTableRowCount(tableName);
                tableInfo.put("rowCount", rowCount);
            } catch (Exception e) {
                log.warn("获取表行数失败: {}", e.getMessage());
                tableInfo.put("rowCount", -1L);
            }
            
            // 获取表的大小（HBase不直接支持，这里使用近似值）
            try {
                Long tableSize = getTableSize(tableName);
                tableInfo.put("tableSize", tableSize);
            } catch (Exception e) {
                log.warn("获取表大小失败: {}", e.getMessage());
                tableInfo.put("tableSize", -1L);
            }
            
            // 获取表的索引信息
            List<Map<String, Object>> indexes = getIndexes(tableName);
            tableInfo.put("indexCount", indexes.size());
            
            // 获取表的列族信息（HBase特有）
            Set<String> columnFamilies = new HashSet<>();
            String cfSql = "SELECT DISTINCT COLUMN_FAMILY " +
                          "FROM SYSTEM.CATALOG " +
                          "WHERE TABLE_NAME = ? " +
                          "AND COLUMN_FAMILY IS NOT NULL";
            
            try (PreparedStatement stmt = conn.prepareStatement(cfSql)) {
                stmt.setString(1, tableName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String cf = rs.getString("COLUMN_FAMILY");
                        if (cf != null && !cf.isEmpty()) {
                            columnFamilies.add(cf);
                        }
                    }
                }
            }
            tableInfo.put("columnFamilies", columnFamilies);
            tableInfo.put("columnFamilyCount", columnFamilies.size());
            
            // 获取表的盐桶数（HBase特有）
            String saltBucketsSql = "SELECT SALT_BUCKETS " +
                                   "FROM SYSTEM.CATALOG " +
                                   "WHERE TABLE_NAME = ? " +
                                   "AND COLUMN_NAME IS NULL " +
                                   "LIMIT 1";
            
            try (PreparedStatement stmt = conn.prepareStatement(saltBucketsSql)) {
                stmt.setString(1, tableName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int saltBuckets = rs.getInt("SALT_BUCKETS");
                        if (!rs.wasNull()) {
                            tableInfo.put("saltBuckets", saltBuckets);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("获取盐桶数失败: {}", e.getMessage());
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
        try (Connection conn = getConnection()) {
            String sql = "SELECT COLUMN_NAME, DATA_TYPE, NULLABLE, " +
                        "COLUMN_FAMILY, ORDINAL_POSITION, " +
                        "ARRAY_SIZE, DATA_TABLE_NAME, " +
                        "KEY_SEQ " +
                        "FROM SYSTEM.CATALOG " +
                        "WHERE TABLE_NAME = ? " +
                        "AND COLUMN_NAME IS NOT NULL " +
                        "ORDER BY ORDINAL_POSITION";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> column = new HashMap<>();
                        column.put("columnName", rs.getString("COLUMN_NAME"));
                        column.put("dataType", rs.getString("DATA_TYPE"));
                        column.put("nullable", "Y".equals(rs.getString("NULLABLE")));
                        column.put("columnFamily", rs.getString("COLUMN_FAMILY"));
                        column.put("position", rs.getInt("ORDINAL_POSITION"));
                        
                        // 检查是否为数组类型
                        int arraySize = rs.getInt("ARRAY_SIZE");
                        if (!rs.wasNull() && arraySize > 0) {
                            column.put("isArray", true);
                            column.put("arraySize", arraySize);
                        } else {
                            column.put("isArray", false);
                        }
                        
                        // 检查是否为主键
                        int keySeq = rs.getInt("KEY_SEQ");
                        if (!rs.wasNull() && keySeq > 0) {
                            column.put("isPrimaryKey", true);
                            column.put("keySequence", keySeq);
                        } else {
                            column.put("isPrimaryKey", false);
                        }
                        
                        columns.add(column);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取列列表失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return columns;
    }

    @Override
    public Map<String, Object> getColumnInfo(String tableName, String columnName) throws Exception {
        Map<String, Object> columnInfo = null;
        try (Connection conn = getConnection()) {
            String sql = "SELECT COLUMN_NAME, DATA_TYPE, NULLABLE, " +
                        "COLUMN_FAMILY, ORDINAL_POSITION, " +
                        "ARRAY_SIZE, DATA_TABLE_NAME, " +
                        "KEY_SEQ, COLUMN_SIZE, DECIMAL_DIGITS, " +
                        "COLUMN_DEF, REMARKS " +
                        "FROM SYSTEM.CATALOG " +
                        "WHERE TABLE_NAME = ? " +
                        "AND COLUMN_NAME = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName.toUpperCase());
                  stmt.setString(2, columnName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        columnInfo = new HashMap<>();
                        columnInfo.put("columnName", rs.getString("COLUMN_NAME"));
                        columnInfo.put("dataType", rs.getString("DATA_TYPE"));
                        columnInfo.put("nullable", "Y".equals(rs.getString("NULLABLE")));
                        columnInfo.put("columnFamily", rs.getString("COLUMN_FAMILY"));
                        columnInfo.put("position", rs.getInt("ORDINAL_POSITION"));
                        
                        // 检查是否为数组类型
                        int arraySize = rs.getInt("ARRAY_SIZE");
                        if (!rs.wasNull() && arraySize > 0) {
                            columnInfo.put("isArray", true);
                            columnInfo.put("arraySize", arraySize);
                        } else {
                            columnInfo.put("isArray", false);
                        }
                        
                        // 检查是否为主键
                        int keySeq = rs.getInt("KEY_SEQ");
                        if (!rs.wasNull() && keySeq > 0) {
                            columnInfo.put("isPrimaryKey", true);
                            columnInfo.put("keySequence", keySeq);
                        } else {
                            columnInfo.put("isPrimaryKey", false);
                        }
                        
                        // 获取长度和精度
                        int columnSize = rs.getInt("COLUMN_SIZE");
                        if (!rs.wasNull()) {
                            columnInfo.put("length", columnSize);
                        }
                        
                        int decimalDigits = rs.getInt("DECIMAL_DIGITS");
                        if (!rs.wasNull()) {
                            columnInfo.put("scale", decimalDigits);
                        }
                        
                        // 获取默认值和注释
                        columnInfo.put("defaultValue", rs.getString("COLUMN_DEF"));
                        columnInfo.put("comment", rs.getString("REMARKS"));
                    }
                }
            }
            
            // 如果找到了列信息，获取更多统计信息
            if (columnInfo != null) {
                // 获取非空值数量
                String nonNullSql = String.format(
                    "SELECT COUNT(*) AS non_null_count FROM %s WHERE %s IS NOT NULL",
                    wrapIdentifier(tableName), wrapIdentifier(columnName)
                );
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(nonNullSql)) {
                    if (rs.next()) {
                        columnInfo.put("nonNullCount", rs.getLong("non_null_count"));
                    }
                } catch (Exception e) {
                    log.warn("获取非空值数量失败: {}", e.getMessage());
                }
                
                // 获取唯一值数量
                String distinctSql = String.format(
                    "SELECT COUNT(DISTINCT %s) AS distinct_count FROM %s",
                    wrapIdentifier(columnName), wrapIdentifier(tableName)
                );
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(distinctSql)) {
                    if (rs.next()) {
                        columnInfo.put("distinctCount", rs.getLong("distinct_count"));
                    }
                } catch (Exception e) {
                    log.warn("获取唯一值数量失败: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("获取列信息失败: table={}, column={}, error={}", 
                     tableName, columnName, e.getMessage(), e);
            throw e;
        }
        
        if (columnInfo == null) {
            throw new Exception("Column not found: " + columnName);
        }
        
        return columnInfo;
    }

    @Override
    public List<String> getPrimaryKeys(String tableName) throws Exception {
        List<String> primaryKeys = new ArrayList<>();
        try (Connection conn = getConnection()) {
            String sql = "SELECT COLUMN_NAME " +
                        "FROM SYSTEM.CATALOG " +
                        "WHERE TABLE_NAME = ? " +
                        "AND KEY_SEQ IS NOT NULL " +
                        "ORDER BY KEY_SEQ";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        primaryKeys.add(rs.getString("COLUMN_NAME"));
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取主键失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return primaryKeys;
    }

    @Override
    public List<Map<String, Object>> getForeignKeys(String tableName) throws Exception {
        // HBase/Phoenix不直接支持外键约束
        // 这里返回一个空列表，或者可以通过自定义元数据表来实现
        List<Map<String, Object>> foreignKeys = new ArrayList<>();
        
        try (Connection conn = getConnection()) {
            // 尝试从自定义元数据表中获取外键信息
            // 注意：这是一个示例实现，实际应用中可能需要创建这样的元数据表
            String sql = "SELECT FK_NAME, FK_COLUMN_NAME, " +
                        "REF_TABLE_NAME, REF_COLUMN_NAME " +
                        "FROM SYSTEM.FOREIGN_KEYS " +  // 假设存在这样的系统表
                        "WHERE TABLE_NAME = ?";
            
            try {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, tableName.toUpperCase());
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            Map<String, Object> fk = new HashMap<>();
                            fk.put("constraintName", rs.getString("FK_NAME"));
                            fk.put("columnName", rs.getString("FK_COLUMN_NAME"));
                            fk.put("referencedTable", rs.getString("REF_TABLE_NAME"));
                            fk.put("referencedColumn", rs.getString("REF_COLUMN_NAME"));
                            foreignKeys.add(fk);
                        }
                    }
                }
            } catch (SQLException e) {
                // 如果表不存在，忽略错误
                log.debug("外键元数据表不存在: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("获取外键失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        
        return foreignKeys;
    }

    @Override
    public List<Map<String, Object>> getIndexes(String tableName) throws Exception {
        List<Map<String, Object>> indexes = new ArrayList<>();
        try (Connection conn = getConnection()) {
            String sql = "SELECT IDX.INDEX_NAME, IDX.DATA_TABLE_NAME, " +
                        "IDX.INDEX_TYPE, IDX.IS_UNIQUE, " +
                        "COL.COLUMN_NAME, COL.ORDINAL_POSITION " +
                        "FROM SYSTEM.CATALOG IDX " +
                        "JOIN SYSTEM.CATALOG COL ON IDX.INDEX_NAME = COL.TABLE_NAME " +
                        "WHERE IDX.DATA_TABLE_NAME = ? " +
                        "AND IDX.COLUMN_NAME IS NULL " +
                        "AND COL.COLUMN_NAME IS NOT NULL " +
                        "ORDER BY IDX.INDEX_NAME, COL.ORDINAL_POSITION";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> index = new HashMap<>();
                        index.put("indexName", rs.getString("INDEX_NAME"));
                        index.put("tableName", rs.getString("DATA_TABLE_NAME"));
                        index.put("indexType", rs.getString("INDEX_TYPE"));
                        index.put("isUnique", "1".equals(rs.getString("IS_UNIQUE")));
                        index.put("columnName", rs.getString("COLUMN_NAME"));
                        index.put("position", rs.getInt("ORDINAL_POSITION"));
                        indexes.add(index);
                    }
                }
            } catch (SQLException e) {
                // 如果查询失败，尝试使用简化的查询
                log.warn("获取索引详细信息失败，尝试简化查询: {}", e.getMessage());
                
                String simpleSql = "SELECT INDEX_NAME, INDEX_TYPE, IS_UNIQUE " +
                                  "FROM SYSTEM.CATALOG " +
                                  "WHERE DATA_TABLE_NAME = ? " +
                                  "AND COLUMN_NAME IS NULL " +
                                  "AND INDEX_TYPE IS NOT NULL";
                
                try (PreparedStatement simpleStmt = conn.prepareStatement(simpleSql)) {
                    simpleStmt.setString(1, tableName.toUpperCase());
                    try (ResultSet rs = simpleStmt.executeQuery()) {
                        while (rs.next()) {
                            Map<String, Object> index = new HashMap<>();
                            index.put("indexName", rs.getString("INDEX_NAME"));
                            index.put("tableName", tableName);
                            index.put("indexType", rs.getString("INDEX_TYPE"));
                            index.put("isUnique", "1".equals(rs.getString("IS_UNIQUE")));
                            indexes.add(index);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取索引失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return indexes;
    }

    @Override
    public Long getTableSize(String tableName) throws Exception {
        // HBase/Phoenix不直接提供表大小的查询方式
        // 这里提供一个近似实现，实际应用中可能需要使用HBase Admin API
        try (Connection conn = getConnection()) {
            // 尝试从系统表中获取表大小信息
            String sql = "SELECT SUM(GUIDE_POSTS_WIDTH) AS TABLE_SIZE " +
                        "FROM SYSTEM.STATS " +
                        "WHERE PHYSICAL_NAME = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Long size = rs.getLong("TABLE_SIZE");
                        if (!rs.wasNull()) {
                            return size;
                        }
                    }
                }
            } catch (SQLException e) {
                log.warn("从SYSTEM.STATS获取表大小失败: {}", e.getMessage());
            }
            
            // 如果上述方法失败，尝试估算表大小
            // 获取表的行数和列数
            Long rowCount = getTableRowCount(tableName);
            int columnCount = 0;
            
            String columnCountSql = "SELECT COUNT(*) AS COLUMN_COUNT " +
                                   "FROM SYSTEM.CATALOG " +
                                   "WHERE TABLE_NAME = ? " +
                                   "AND COLUMN_NAME IS NOT NULL";
            
            try (PreparedStatement stmt = conn.prepareStatement(columnCountSql)) {
                stmt.setString(1, tableName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        columnCount = rs.getInt("COLUMN_COUNT");
                    }
                }
            }
            
            // 假设每个单元格平均大小为100字节
            if (rowCount != null && columnCount > 0) {
                return rowCount * columnCount * 100L;
            }
            
            // 如果无法估算，返回默认值
            return -1L;
        } catch (Exception e) {
            log.error("获取表大小失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Long getTableRowCount(String tableName) throws Exception {
        // Phoenix 可以使用系统表的统计信息获取行数估计
        String optimizedSql = "SELECT SUM(GUIDE_POSTS_ROW_COUNT) FROM SYSTEM.STATS " +
                             "WHERE PHYSICAL_NAME = '" + tableName.toUpperCase() + "'";
                             
        // 尝试从统计信息获取 (更快但不太准确)
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(optimizedSql)) {
            if (rs.next() && rs.getObject(1) != null) {
                return rs.getLong(1);
            }
        } catch (Exception e) {
            // 忽略错误
        }
        
        // 回退到标准计数 (准确但较慢)
        String countSql = "SELECT /*+ NO_CACHE */ COUNT(*) FROM " + wrapIdentifier(tableName);
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        
        return 0L;
    }

    @Override
    public Date getTableCreateTime(String tableName) throws Exception {
        try (Connection conn = getConnection()) {
            String sql = "SELECT LAST_DDL_TIMESTAMP " +
                        "FROM SYSTEM.CATALOG " +
                        "WHERE TABLE_NAME = ? " +
                        "AND COLUMN_NAME IS NULL " +
                        "LIMIT 1";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getTimestamp("LAST_DDL_TIMESTAMP");
                    }
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
        // HBase/Phoenix不直接存储表的更新时间
        // 这里返回与创建时间相同的值，或者可以通过自定义元数据表来实现
        return getTableCreateTime(tableName);
    }

    @Override
    public String getTableSpace(String tableName) throws Exception {
        // Phoenix 没有传统的表空间概念
        // 返回 HBase 的命名空间信息作为表空间替代
        try (Connection conn = getConnection()) {
            String schema = getSchema();
            if (schema != null && !schema.isEmpty()) {
                return schema;
            }
            // 从表名提取可能的命名空间
            if (tableName.contains(".")) {
                return tableName.substring(0, tableName.indexOf('.'));
            }
        } catch (Exception e) {
            log.warn("Failed to get table namespace: {}", e.getMessage());
        }
        return "default";
    }

    @Override
    public Integer getCharacterLength(String tableName, String columnName) throws Exception {
        try (Connection conn = getConnection()) {
            String sql = "SELECT COLUMN_SIZE " +
                        "FROM SYSTEM.CATALOG " +
                        "WHERE TABLE_NAME = ? " +
                        "AND COLUMN_NAME = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName.toUpperCase());
                  stmt.setString(2, columnName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int length = rs.getInt("COLUMN_SIZE");
                        if (!rs.wasNull()) {
                            return length;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取字符长度失败: table={}, column={}, error={}", 
                     tableName, columnName, e.getMessage(), e);
            throw e;
        }
        return null;
    }

    @Override
    public Integer getNumericPrecision(String tableName, String columnName) throws Exception {
        try (Connection conn = getConnection()) {
            String sql = "SELECT COLUMN_SIZE " +
                        "FROM SYSTEM.CATALOG " +
                        "WHERE TABLE_NAME = ? " +
                        "AND COLUMN_NAME = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName.toUpperCase());
                  stmt.setString(2, columnName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int precision = rs.getInt("COLUMN_SIZE");
                        if (!rs.wasNull()) {
                            return precision;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取数值精度失败: table={}, column={}, error={}", 
                     tableName, columnName, e.getMessage(), e);
            throw e;
        }
        return null;
    }

    @Override
    public Integer getNumericScale(String tableName, String columnName) throws Exception {
        try (Connection conn = getConnection()) {
            String sql = "SELECT DECIMAL_DIGITS " +
                        "FROM SYSTEM.CATALOG " +
                        "WHERE TABLE_NAME = ? " +
                        "AND COLUMN_NAME = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName.toUpperCase());
                  stmt.setString(2, columnName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int scale = rs.getInt("DECIMAL_DIGITS");
                        if (!rs.wasNull()) {
                            return scale;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取小数位数失败: table={}, column={}, error={}", 
                     tableName, columnName, e.getMessage(), e);
            throw e;
        }
        return null;
    }

    @Override
    public String getColumnDefault(String tableName, String columnName) throws Exception {
        try (Connection conn = getConnection()) {
            String sql = "SELECT COLUMN_DEF " +
                        "FROM SYSTEM.CATALOG " +
                        "WHERE TABLE_NAME = ? " +
                        "AND COLUMN_NAME = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName.toUpperCase());
                  stmt.setString(2, columnName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("COLUMN_DEF");
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取列默认值失败: table={}, column={}, error={}", 
                     tableName, columnName, e.getMessage(), e);
            throw e;
        }
        return null;
    }

    @Override
    public String getColumnExtra(String tableName, String columnName) throws Exception {
        StringBuilder extra = new StringBuilder();
        try (Connection conn = getConnection()) {
            // 获取列的额外属性
            String sql = "SELECT KEY_SEQ, COLUMN_FAMILY, ARRAY_SIZE " +
                        "FROM SYSTEM.CATALOG " +
                        "WHERE TABLE_NAME = ? " +
                        "AND COLUMN_NAME = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName.toUpperCase());
                  stmt.setString(2, columnName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        // 检查是否为主键
                        int keySeq = rs.getInt("KEY_SEQ");
                        if (!rs.wasNull() && keySeq > 0) {
                            extra.append("PRIMARY KEY");
                        }
                        
                        // 检查列族
                        String columnFamily = rs.getString("COLUMN_FAMILY");
                        if (columnFamily != null && !columnFamily.isEmpty()) {
                            if (extra.length() > 0) extra.append(", ");
                            extra.append("COLUMN_FAMILY='").append(columnFamily).append("'");
                        }
                        
                        // 检查是否为数组
                        int arraySize = rs.getInt("ARRAY_SIZE");
                        if (!rs.wasNull() && arraySize > 0) {
                            if (extra.length() > 0) extra.append(", ");
                            extra.append("ARRAY[").append(arraySize).append("]");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取列额外属性失败: table={}, column={}, error={}", 
                     tableName, columnName, e.getMessage(), e);
            throw e;
        }
        return extra.toString();
    }

    @Override
    public Integer getColumnPosition(String tableName, String columnName) throws Exception {
        try (Connection conn = getConnection()) {
            String sql = "SELECT ORDINAL_POSITION " +
                        "FROM SYSTEM.CATALOG " +
                        "WHERE TABLE_NAME = ? " +
                        "AND COLUMN_NAME = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName.toUpperCase());
                  stmt.setString(2, columnName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("ORDINAL_POSITION");
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取列位置失败: table={}, column={}, error={}", 
                     tableName, columnName, e.getMessage(), e);
            throw e;
        }
        return null;
    }

    @Override
    public List<Map<String, Object>> getReferencedByRelations(String tableName) throws Exception {
        // HBase/Phoenix不直接支持外键约束
        // 这里返回一个空列表，或者可以通过自定义元数据表来实现
        List<Map<String, Object>> referencedBy = new ArrayList<>();
        
        try (Connection conn = getConnection()) {
            // 尝试从自定义元数据表中获取引用信息
            // 注意：这是一个示例实现，实际应用中可能需要创建这样的元数据表
            String sql = "SELECT FK_TABLE_NAME, FK_COLUMN_NAME, " +
                        "FK_NAME, REF_COLUMN_NAME " +
                        "FROM SYSTEM.FOREIGN_KEYS " +  // 假设存在这样的系统表
                        "WHERE REF_TABLE_NAME = ?";
            
            try {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, tableName.toUpperCase());
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            Map<String, Object> ref = new HashMap<>();
                            ref.put("tableName", rs.getString("FK_TABLE_NAME"));
                            ref.put("columnName", rs.getString("FK_COLUMN_NAME"));
                            ref.put("constraintName", rs.getString("FK_NAME"));
                            ref.put("referencedColumn", rs.getString("REF_COLUMN_NAME"));
                            referencedBy.add(ref);
                        }
                    }
                }
            } catch (SQLException e) {
                // 如果表不存在，忽略错误
                log.debug("外键元数据表不存在: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("获取引用关系失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        
        return referencedBy;
    }

    @Override
    public List<Map<String, Object>> getTableForeignKeyRelations(String tableName) throws Exception {
        // 这个方法与getForeignKeys类似，但返回格式可能略有不同
        // 为了保持一致性，这里直接调用getForeignKeys方法
        return getForeignKeys(tableName);
    }

    @Override
    public Map<String, Object> getTableFieldStatistics(String tableName) throws Exception {
        Map<String, Object> statistics = new HashMap<>();
        try (Connection conn = getConnection()) {
            // 获取表的列信息
            List<Map<String, Object>> columns = listColumns(tableName);
            
            // 统计不同类型的列数量
            int totalColumns = columns.size();
            int numericColumns = 0;
            int stringColumns = 0;
            int dateColumns = 0;
            int booleanColumns = 0;
            int binaryColumns = 0;
            int arrayColumns = 0;
            int primaryKeyColumns = 0;
            
            Map<String, Integer> columnFamilyCount = new HashMap<>();
            
            for (Map<String, Object> column : columns) {
                String dataType = (String) column.get("dataType");
                if (dataType == null) continue;
                
                dataType = dataType.toUpperCase();
                
                // 统计数据类型
                if (dataType.contains("INT") || dataType.contains("FLOAT") || 
                    dataType.contains("DOUBLE") || dataType.contains("DECIMAL") || 
                    dataType.contains("NUMERIC")) {
                    numericColumns++;
                } else if (dataType.contains("CHAR") || dataType.contains("VARCHAR") || 
                           dataType.contains("TEXT") || dataType.contains("CLOB")) {
                    stringColumns++;
                } else if (dataType.contains("DATE") || dataType.contains("TIME") || 
                           dataType.contains("TIMESTAMP")) {
                    dateColumns++;
                } else if (dataType.equals("BOOLEAN")) {
                    booleanColumns++;
                } else if (dataType.contains("BINARY") || dataType.contains("BLOB")) {
                    binaryColumns++;
                }
                
                // 统计数组列
                Boolean isArray = (Boolean) column.get("isArray");
                if (isArray != null && isArray) {
                    arrayColumns++;
                }
                
                // 统计主键列
                Boolean isPrimaryKey = (Boolean) column.get("isPrimaryKey");
                if (isPrimaryKey != null && isPrimaryKey) {
                    primaryKeyColumns++;
                }
                
                // 统计列族
                String columnFamily = (String) column.get("columnFamily");
                if (columnFamily != null && !columnFamily.isEmpty()) {
                    columnFamilyCount.put(columnFamily, 
                                         columnFamilyCount.getOrDefault(columnFamily, 0) + 1);
                }
            }
            
            // 添加统计结果
            statistics.put("totalColumns", totalColumns);
            statistics.put("numericColumns", numericColumns);
            statistics.put("stringColumns", stringColumns);
            statistics.put("dateColumns", dateColumns);
            statistics.put("booleanColumns", booleanColumns);
            statistics.put("binaryColumns", binaryColumns);
            statistics.put("arrayColumns", arrayColumns);
            statistics.put("primaryKeyColumns", primaryKeyColumns);
            statistics.put("columnFamilies", columnFamilyCount);
            
            // 获取索引数量
            List<Map<String, Object>> indexes = getIndexes(tableName);
            statistics.put("indexCount", indexes.size());
            
            // 获取表的行数
            try {
                Long rowCount = getTableRowCount(tableName);
                statistics.put("rowCount", rowCount);
            } catch (Exception e) {
                log.warn("获取表行数失败: {}", e.getMessage());
                statistics.put("rowCount", -1L);
            }
            
        } catch (Exception e) {
            log.error("获取表字段统计信息失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return statistics;
    }

    @Override
    public Map<String, Object> getColumnDistribution(String tableName, String columnName) throws Exception {
        Map<String, Object> distribution = new HashMap<>();
        try (Connection conn = getConnection()) {
            // 获取列的数据类型
            String dataType = null;
            try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT DATA_TYPE FROM SYSTEM.CATALOG " +
                "WHERE TABLE_NAME = ? AND COLUMN_NAME = ?")) {
                stmt.setString(1, tableName.toUpperCase());
                  stmt.setString(2, columnName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        dataType = rs.getString("DATA_TYPE");
                        distribution.put("dataType", dataType);
                    }
                }
            }
            
            if (dataType == null) {
                throw new Exception("Column not found: " + columnName);
            }
            
            // 获取非空值数量和总行数
            long totalRows = getTableRowCount(tableName);
            long nonNullCount = 0;
            String nonNullSql = String.format(
                "SELECT COUNT(*) AS count FROM %s WHERE %s IS NOT NULL",
                wrapIdentifier(tableName), wrapIdentifier(columnName)
            );
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(nonNullSql)) {
                if (rs.next()) {
                    nonNullCount = rs.getLong("count");
                }
            }
            
            long nullCount = totalRows - nonNullCount;
            distribution.put("totalCount", totalRows);
            distribution.put("nullCount", nullCount);
            distribution.put("nonNullCount", nonNullCount);
            
            if (totalRows > 0) {
                distribution.put("nullRate", (double) nullCount / totalRows);
                distribution.put("nonNullRate", (double) nonNullCount / totalRows);
            }
            
            // 获取唯一值数量
            String distinctSql = String.format(
                "SELECT COUNT(DISTINCT %s) AS count FROM %s WHERE %s IS NOT NULL",
                wrapIdentifier(columnName), wrapIdentifier(tableName), wrapIdentifier(columnName)
            );
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(distinctSql)) {
                if (rs.next()) {
                    long distinctCount = rs.getLong("count");
                    distribution.put("distinctCount", distinctCount);
                    
                    if (nonNullCount > 0) {
                        distribution.put("distinctRate", (double) distinctCount / nonNullCount);
                    }
                }
            }
            
            // 根据数据类型获取不同的统计信息
            if (dataType.toUpperCase().contains("INT") || 
                dataType.toUpperCase().contains("FLOAT") || 
                dataType.toUpperCase().contains("DOUBLE") || 
                dataType.toUpperCase().contains("DECIMAL") || 
                dataType.toUpperCase().contains("NUMERIC")) {
                
                // 数值类型，获取最小值、最大值、平均值等
                String statsSql = String.format(
                    "SELECT " +
                    "MIN(%s) AS min_value, " +
                    "MAX(%s) AS max_value, " +
                    "AVG(%s) AS avg_value " +
                    "FROM %s WHERE %s IS NOT NULL",
                    wrapIdentifier(columnName), wrapIdentifier(columnName),
                    wrapIdentifier(columnName), wrapIdentifier(tableName),
                    wrapIdentifier(columnName)
                );
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(statsSql)) {
                    if (rs.next()) {
                        distribution.put("minValue", rs.getObject("min_value"));
                        distribution.put("maxValue", rs.getObject("max_value"));
                        distribution.put("avgValue", rs.getDouble("avg_value"));
                    }
                }
                
                // Phoenix不直接支持标准差和中位数计算
                // 这里可以添加自定义实现，但可能会很慢
                
            } else if (dataType.toUpperCase().contains("CHAR") || 
                       dataType.toUpperCase().contains("VARCHAR") || 
                       dataType.toUpperCase().contains("TEXT")) {
                
                // 字符串类型，获取长度统计
                String lengthStatsSql = String.format(
                    "SELECT " +
                    "MIN(LENGTH(%s)) AS min_length, " +
                    "MAX(LENGTH(%s)) AS max_length, " +
                    "AVG(LENGTH(%s)) AS avg_length " +
                    "FROM %s WHERE %s IS NOT NULL",
                    wrapIdentifier(columnName), wrapIdentifier(columnName),
                    wrapIdentifier(columnName), wrapIdentifier(tableName),
                    wrapIdentifier(columnName)
                );
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(lengthStatsSql)) {
                    if (rs.next()) {
                        distribution.put("minLength", rs.getInt("min_length"));
                        distribution.put("maxLength", rs.getInt("max_length"));
                        distribution.put("avgLength", rs.getDouble("avg_length"));
                    }
                }
                
                // 获取空字符串数量
                String emptyStringSql = String.format(
                    "SELECT COUNT(*) AS count FROM %s WHERE %s = ''",
                    wrapIdentifier(tableName), wrapIdentifier(columnName)
                );
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(emptyStringSql)) {
                    if (rs.next()) {
                        long emptyCount = rs.getLong("count");
                        distribution.put("emptyStringCount", emptyCount);
                        
                        if (nonNullCount > 0) {
                            distribution.put("emptyStringRate", (double) emptyCount / nonNullCount);
                        }
                    }
                }
                
            } else if (dataType.toUpperCase().contains("DATE") || 
                       dataType.toUpperCase().contains("TIME")) {
                
                // 日期时间类型，获取最早和最晚日期
                String dateStatsSql = String.format(
                    "SELECT " +
                    "MIN(%s) AS earliest_date, " +
                    "MAX(%s) AS latest_date " +
                    "FROM %s WHERE %s IS NOT NULL",
                    wrapIdentifier(columnName), wrapIdentifier(columnName),
                    wrapIdentifier(tableName), wrapIdentifier(columnName)
                );
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(dateStatsSql)) {
                    if (rs.next()) {
                        distribution.put("earliestDate", rs.getTimestamp("earliest_date"));
                        distribution.put("latestDate", rs.getTimestamp("latest_date"));
                    }
                }
            }
            
            // 获取频率最高的值（TOP 10）
            String topValuesSql = String.format(
                "SELECT %s as value, COUNT(*) as frequency " +
                "FROM %s " +
                "WHERE %s IS NOT NULL " +
                "GROUP BY %s " +
                "ORDER BY frequency DESC " +
                "LIMIT 10",
                wrapIdentifier(columnName),
                wrapIdentifier(tableName),
                wrapIdentifier(columnName),
                wrapIdentifier(columnName)
            );
            
            List<Map<String, Object>> topValues = new ArrayList<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(topValuesSql)) {
                while (rs.next()) {
                    Map<String, Object> valueFreq = new HashMap<>();
                    valueFreq.put("value", rs.getObject("value"));
                    valueFreq.put("frequency", rs.getLong("frequency"));
                    topValues.add(valueFreq);
                }
            }
            distribution.put("topValues", topValues);
            
        } catch (Exception e) {
            log.error("获取列分布信息失败: table={}, column={}, error={}", 
                     tableName, columnName, e.getMessage(), e);
            throw e;
        }
        return distribution;
    }

    @Override
    public List<Map<String, Object>> getColumnValueDistribution(String tableName, String columnName, int topN) throws Exception {
        List<Map<String, Object>> distribution = new ArrayList<>();
        try (Connection conn = getConnection()) {
            // 获取列的数据类型
            String dataType = null;
            try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT DATA_TYPE FROM SYSTEM.CATALOG " +
                "WHERE TABLE_NAME = ? AND COLUMN_NAME = ?")) {
                stmt.setString(1, tableName.toUpperCase());
                  stmt.setString(2, columnName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        dataType = rs.getString("DATA_TYPE");
                    }
                }
            }
            
            if (dataType == null) {
                throw new Exception("Column not found: " + columnName);
            }
            
            // 获取总行数，用于计算百分比
            long totalRows = getTableRowCount(tableName);
            
            // 根据数据类型执行不同的分析
            if (dataType.toUpperCase().contains("CHAR") || 
                dataType.toUpperCase().contains("VARCHAR") || 
                dataType.toUpperCase().contains("TEXT")) {
                
                // 字符串类型，直接统计频率
                String sql = String.format(
                    "SELECT %s AS value, COUNT(*) AS count FROM %s WHERE %s IS NOT NULL " +
                    "GROUP BY %s ORDER BY count DESC LIMIT %d",
                    wrapIdentifier(columnName), wrapIdentifier(tableName), 
                    wrapIdentifier(columnName), wrapIdentifier(columnName), topN
                );
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        Map<String, Object> item = new HashMap<>();
                        String value = rs.getString("value");
                        long count = rs.getLong("count");
                        
                        item.put("value", value);
                        item.put("count", count);
                        
                        if (totalRows > 0) {
                            double percentage = (double) count / totalRows * 100;
                            item.put("percentage", percentage);
                        }
                        
                        distribution.add(item);
                    }
                }
                
            } else if (dataType.toUpperCase().contains("DATE") || 
                       dataType.toUpperCase().contains("TIME")) {
                
                // 日期类型，按年月分组
                // Phoenix不直接支持日期提取函数，使用TO_CHAR函数
                String sql = String.format(
                    "SELECT TO_CHAR(%s, 'yyyy-MM') AS date_group, COUNT(*) AS count FROM %s " +
                    "WHERE %s IS NOT NULL GROUP BY date_group ORDER BY count DESC LIMIT %d",
                    wrapIdentifier(columnName), wrapIdentifier(tableName), 
                    wrapIdentifier(columnName), topN
                );
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        Map<String, Object> item = new HashMap<>();
                        String dateGroup = rs.getString("date_group");
                        long count = rs.getLong("count");
                        
                        item.put("value", dateGroup);
                        item.put("count", count);
                        
                        if (totalRows > 0) {
                            double percentage = (double) count / totalRows * 100;
                            item.put("percentage", percentage);
                        }
                        
                        distribution.add(item);
                    }
                } catch (SQLException e) {
                    // 如果TO_CHAR函数不可用，尝试使用字符串转换
                    log.warn("日期格式化失败，尝试使用字符串转换: {}", e.getMessage());
                    
                    String altSql = String.format(
                        "SELECT %s AS date_value, COUNT(*) AS count FROM %s " +
                        "WHERE %s IS NOT NULL GROUP BY date_value ORDER BY count DESC LIMIT %d",
                        wrapIdentifier(columnName), wrapIdentifier(tableName), 
                        wrapIdentifier(columnName), topN
                    );
                    
                    try (Statement altStmt = conn.createStatement();
                         ResultSet altRs = altStmt.executeQuery(altSql)) {
                        while (altRs.next()) {
                            Map<String, Object> item = new HashMap<>();
                            Object dateValue = altRs.getObject("date_value");
                            long count = altRs.getLong("count");
                            
                            item.put("value", dateValue);
                            item.put("count", count);
                            
                            if (totalRows > 0) {
                                double percentage = (double) count / totalRows * 100;
                                item.put("percentage", percentage);
                            }
                            
                            distribution.add(item);
                        }
                    }
                }
                
            } else if (dataType.toUpperCase().contains("INT") || 
                       dataType.toUpperCase().contains("FLOAT") || 
                       dataType.toUpperCase().contains("DOUBLE") || 
                       dataType.toUpperCase().contains("DECIMAL") || 
                       dataType.toUpperCase().contains("NUMERIC")) {
                
                // 数值类型，计算范围并分组
                Double min = null;
                Double max = null;
                
                // 获取最大最小值
                String minMaxSql = String.format(
                    "SELECT MIN(%s) AS min_val, MAX(%s) AS max_val FROM %s WHERE %s IS NOT NULL",
                    wrapIdentifier(columnName), wrapIdentifier(columnName), 
                    wrapIdentifier(tableName), wrapIdentifier(columnName)
                );
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(minMaxSql)) {
                    if (rs.next()) {
                        min = rs.getDouble("min_val");
                        max = rs.getDouble("max_val");
                    }
                }
                
                if (min != null && max != null) {
                    // 如果最大值和最小值相同，直接返回单个值的分布
                    if (Math.abs(max - min) < 0.000001) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("value", min);
                        item.put("count", totalRows);
                        item.put("percentage", 100.0);
                        distribution.add(item);
                    } else {
                        // 计算区间
                        int numIntervals = Math.min(10, topN);
                        double interval = (max - min) / numIntervals;
                        
                        // 对于整数类型，如果范围较小，直接按值分组
                        if (dataType.toUpperCase().contains("INT") && (max - min) <= 100) {
                            String sql = String.format(
                                "SELECT %s AS value, COUNT(*) AS count FROM %s " +
                                "WHERE %s IS NOT NULL GROUP BY value ORDER BY count DESC LIMIT %d",
                                wrapIdentifier(columnName), wrapIdentifier(tableName), 
                                wrapIdentifier(columnName), topN
                            );
                            
                            try (Statement stmt = conn.createStatement();
                                 ResultSet rs = stmt.executeQuery(sql)) {
                                while (rs.next()) {
                                    Map<String, Object> item = new HashMap<>();
                                    Object value = rs.getObject("value");
                                    long count = rs.getLong("count");
                                    
                                    item.put("value", value);
                                    item.put("count", count);
                                    
                                    if (totalRows > 0) {
                                        double percentage = (double) count / totalRows * 100;
                                        item.put("percentage", percentage);
                                    }
                                    
                                    distribution.add(item);
                                }
                            }
                        } else {
                            // 按区间分组
                            for (int i = 0; i < numIntervals; i++) {
                                double lowerBound = min + (i * interval);
                                double upperBound = min + ((i + 1) * interval);
                                
                                // 最后一个区间包含最大值
                                if (i == numIntervals - 1) {
                                    upperBound = max;
                                }
                                
                                String sql = String.format(
                                    "SELECT COUNT(*) AS count FROM %s WHERE %s >= ? AND %s < ?",
                                    wrapIdentifier(tableName), wrapIdentifier(columnName), 
                                    wrapIdentifier(columnName)
                                );
                                
                                // 对于最后一个区间，包含最大值
                                if (i == numIntervals - 1) {
                                    sql = String.format(
                                        "SELECT COUNT(*) AS count FROM %s WHERE %s >= ? AND %s <= ?",
                                        wrapIdentifier(tableName), wrapIdentifier(columnName), 
                                        wrapIdentifier(columnName)
                                    );
                                }
                                
                                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                                    stmt.setDouble(1, lowerBound);
                                    stmt.setDouble(2, upperBound);
                                    try (ResultSet rs = stmt.executeQuery()) {
                                        if (rs.next()) {
                                            long count = rs.getLong("count");
                                            if (count > 0) {
                                                Map<String, Object> item = new HashMap<>();
                                                String rangeLabel = String.format("[%.2f, %.2f]", lowerBound, upperBound);
                                                
                                                item.put("value", rangeLabel);
                                                item.put("lowerBound", lowerBound);
                                                item.put("upperBound", upperBound);
                                                item.put("count", count);
                                                
                                                if (totalRows > 0) {
                                                    double percentage = (double) count / totalRows * 100;
                                                    item.put("percentage", percentage);
                                                }
                                                
                                                distribution.add(item);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
            } else if (dataType.toUpperCase().equals("BOOLEAN")) {
                
                // 布尔类型，直接统计true和false的数量
                String sql = String.format(
                    "SELECT %s AS value, COUNT(*) AS count FROM %s " +
                    "WHERE %s IS NOT NULL GROUP BY value",
                    wrapIdentifier(columnName), wrapIdentifier(tableName), 
                    wrapIdentifier(columnName)
                );
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        Map<String, Object> item = new HashMap<>();
                        boolean value = rs.getBoolean("value");
                        long count = rs.getLong("count");
                        
                        item.put("value", value);
                        item.put("count", count);
                        
                        if (totalRows > 0) {
                            double percentage = (double) count / totalRows * 100;
                            item.put("percentage", percentage);
                        }
                        
                        distribution.add(item);
                    }
                }
                
            } else {
                
                // 其他类型，按值分组
                String sql = String.format(
                    "SELECT %s AS value, COUNT(*) AS count FROM %s " +
                    "WHERE %s IS NOT NULL GROUP BY value ORDER BY count DESC LIMIT %d",
                    wrapIdentifier(columnName), wrapIdentifier(tableName), 
                    wrapIdentifier(columnName), topN
                );
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        Map<String, Object> item = new HashMap<>();
                        Object value = rs.getObject("value");
                        long count = rs.getLong("count");
                        
                        item.put("value", value);
                        item.put("count", count);
                        
                        if (totalRows > 0) {
                            double percentage = (double) count / totalRows * 100;
                            item.put("percentage", percentage);
                        }
                        
                        distribution.add(item);
                    }
                }
            }
            
            // 添加NULL值的统计
            String nullSql = String.format(
                "SELECT COUNT(*) AS null_count FROM %s WHERE %s IS NULL",
                wrapIdentifier(tableName), wrapIdentifier(columnName)
            );
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(nullSql)) {
                if (rs.next()) {
                    long nullCount = rs.getLong("null_count");
                    if (nullCount > 0) {
                        Map<String, Object> nullItem = new HashMap<>();
                        nullItem.put("value", "NULL");
                        nullItem.put("count", nullCount);
                        
                        if (totalRows > 0) {
                            double percentage = (double) nullCount / totalRows * 100;
                            nullItem.put("percentage", percentage);
                        }
                        
                        distribution.add(nullItem);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("获取列值分布失败: table={}, column={}, topN={}, error={}", 
                     tableName, columnName, topN, e.getMessage(), e);
            throw e;
        }
        return distribution;
    }

    @Override
    public Map<String, Object> getColumnValueRange(String tableName, String columnName) throws Exception {
        Map<String, Object> range = new HashMap<>();
        try (Connection conn = getConnection()) {
            // 获取列的数据类型
            String dataType = null;
            try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT DATA_TYPE FROM SYSTEM.CATALOG " +
                "WHERE TABLE_NAME = ? AND COLUMN_NAME = ?")) {
                stmt.setString(1, tableName.toUpperCase());
                  stmt.setString(2, columnName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        dataType = rs.getString("DATA_TYPE");
                        range.put("dataType", dataType);
                    }
                }
            }
            
            if (dataType == null) {
                throw new Exception("Column not found: " + columnName);
            }
            
            // 获取非空值数量和总行数
            long totalRows = getTableRowCount(tableName);
            long nonNullCount = 0;
            String nonNullSql = String.format(
                "SELECT COUNT(*) AS count FROM %s WHERE %s IS NOT NULL",
                wrapIdentifier(tableName), wrapIdentifier(columnName)
            );
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(nonNullSql)) {
                if (rs.next()) {
                    nonNullCount = rs.getLong("count");
                }
            }
            
            long nullCount = totalRows - nonNullCount;
            range.put("totalCount", totalRows);
            range.put("nullCount", nullCount);
            range.put("nonNullCount", nonNullCount);
            
            if (totalRows > 0) {
                range.put("nullRate", (double) nullCount / totalRows);
                range.put("nonNullRate", (double) nonNullCount / totalRows);
            }
            
            // 获取唯一值数量
            String distinctSql = String.format(
                "SELECT COUNT(DISTINCT %s) AS count FROM %s WHERE %s IS NOT NULL",
                wrapIdentifier(columnName), wrapIdentifier(tableName), wrapIdentifier(columnName)
            );
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(distinctSql)) {
                if (rs.next()) {
                    long distinctCount = rs.getLong("count");
                    range.put("distinctCount", distinctCount);
                    
                    if (nonNullCount > 0) {
                        range.put("distinctRate", (double) distinctCount / nonNullCount);
                    }
                }
            }
            
            // 根据数据类型获取不同的统计信息
            if (dataType.toUpperCase().contains("INT") || 
                dataType.toUpperCase().contains("FLOAT") || 
                dataType.toUpperCase().contains("DOUBLE") || 
                dataType.toUpperCase().contains("DECIMAL") || 
                dataType.toUpperCase().contains("NUMERIC")) {
                
                // 数值类型，获取最小值、最大值、平均值等
                String statsSql = String.format(
                    "SELECT " +
                    "MIN(%s) AS min_val, " +
                    "MAX(%s) AS max_val, " +
                    "AVG(%s) AS avg_val " +
                    "FROM %s WHERE %s IS NOT NULL",
                    wrapIdentifier(columnName), wrapIdentifier(columnName),
                    wrapIdentifier(columnName), wrapIdentifier(tableName),
                    wrapIdentifier(columnName)
                );
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(statsSql)) {
                    if (rs.next()) {
                        range.put("min", rs.getObject("min_val"));
                        range.put("max", rs.getObject("max_val"));
                        range.put("avg", rs.getDouble("avg_val"));
                    }
                }
                
                // Phoenix不直接支持标准差和分位数计算
                // 这里可以添加自定义实现，但可能会很慢
                
            } else if (dataType.toUpperCase().contains("CHAR") || 
                       dataType.toUpperCase().contains("VARCHAR") || 
                       dataType.toUpperCase().contains("TEXT")) {
                
                // 字符串类型，获取长度统计
                String lengthStatsSql = String.format(
                    "SELECT " +
                    "MIN(LENGTH(%s)) AS min_length, " +
                    "MAX(LENGTH(%s)) AS max_length, " +
                    "AVG(LENGTH(%s)) AS avg_length " +
                    "FROM %s WHERE %s IS NOT NULL",
                    wrapIdentifier(columnName), wrapIdentifier(columnName),
                    wrapIdentifier(columnName), wrapIdentifier(tableName),
                    wrapIdentifier(columnName)
                );
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(lengthStatsSql)) {
                    if (rs.next()) {
                        range.put("minLength", rs.getInt("min_length"));
                        range.put("maxLength", rs.getInt("max_length"));
                        range.put("avgLength", rs.getDouble("avg_length"));
                    }
                }
                
                // 获取空字符串数量
                String emptyStringSql = String.format(
                    "SELECT COUNT(*) AS count FROM %s WHERE %s = ''",
                    wrapIdentifier(tableName), wrapIdentifier(columnName)
                );
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(emptyStringSql)) {
                    if (rs.next()) {
                        long emptyCount = rs.getLong("count");
                        range.put("emptyStringCount", emptyCount);
                        
                        if (nonNullCount > 0) {
                            range.put("emptyStringRate", (double) emptyCount / nonNullCount);
                        }
                    }
                }
                
            } else if (dataType.toUpperCase().contains("DATE") || 
                       dataType.toUpperCase().contains("TIME")) {
                
                // 日期时间类型，获取最早和最晚日期
                String dateStatsSql = String.format(
                    "SELECT " +
                    "MIN(%s) AS min_date, " +
                    "MAX(%s) AS max_date " +
                    "FROM %s WHERE %s IS NOT NULL",
                    wrapIdentifier(columnName), wrapIdentifier(columnName),
                    wrapIdentifier(tableName), wrapIdentifier(columnName)
                );
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(dateStatsSql)) {
                    if (rs.next()) {
                        range.put("minDate", rs.getTimestamp("min_date"));
                        range.put("maxDate", rs.getTimestamp("max_date"));
                        
                        // 计算日期范围（天数）
                        java.sql.Timestamp minDate = rs.getTimestamp("min_date");
                        java.sql.Timestamp maxDate = rs.getTimestamp("max_date");
                        
                        if (minDate != null && maxDate != null) {
                            long diffMillis = maxDate.getTime() - minDate.getTime();
                            long diffDays = diffMillis / (1000 * 60 * 60 * 24);
                            range.put("dateRangeDays", diffDays);
                        }
                    }
                }
                
            } else if (dataType.toUpperCase().equals("BOOLEAN")) {
                
                // 布尔类型，获取true和false的数量
                String booleanStatsSql = String.format(
                    "SELECT %s AS value, COUNT(*) AS count FROM %s " +
                    "WHERE %s IS NOT NULL GROUP BY %s",
                    wrapIdentifier(columnName), wrapIdentifier(tableName),
                    wrapIdentifier(columnName), wrapIdentifier(columnName)
                );
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(booleanStatsSql)) {
                    long trueCount = 0;
                    long falseCount = 0;
                    
                    while (rs.next()) {
                        boolean value = rs.getBoolean("value");
                        long count = rs.getLong("count");
                        
                        if (value) {
                            trueCount = count;
                        } else {
                            falseCount = count;
                        }
                    }
                    
                    range.put("trueCount", trueCount);
                    range.put("falseCount", falseCount);
                    
                    if (nonNullCount > 0) {
                        range.put("trueRate", (double) trueCount / nonNullCount);
                        range.put("falseRate", (double) falseCount / nonNullCount);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("获取列值域范围失败: table={}, column={}, error={}", 
                     tableName, columnName, e.getMessage(), e);
            throw e;
        }
        return range;
    }

    @Override
    public Map<String, Object> getTableUpdateFrequency(String tableName) throws Exception {
        Map<String, Object> result = new HashMap<>();
        try (Connection conn = getConnection()) {
            // 获取表的创建时间和最后更新时间
            Date createTime = getTableCreateTime(tableName);
            Date updateTime = getTableUpdateTime(tableName);
            
            result.put("createTime", createTime);
            result.put("lastUpdateTime", updateTime);
            
            // 计算表存在的天数
            long existenceDays = 1; // 默认至少1天，避免除零错误
            if (createTime != null) {
                long now = System.currentTimeMillis();
                long createTimeMs = createTime.getTime();
                existenceDays = Math.max(1, (now - createTimeMs) / (1000 * 60 * 60 * 24));
                result.put("existenceDays", existenceDays);
            }
            
            // 获取表的行数
            Long rowCount = getTableRowCount(tableName);
            result.put("rowCount", rowCount);
            
            // 计算平均每日增长
            if (rowCount != null && existenceDays > 0) {
                double avgDailyGrowth = (double) rowCount / existenceDays;
                result.put("avgDailyGrowth", avgDailyGrowth);
            }
            
            // HBase/Phoenix不直接支持表操作统计
            // 这里提供一些估计值，实际应用中可能需要自定义监控
            result.put("estimatedInserts", rowCount);
            result.put("estimatedUpdates", 0L);
            result.put("estimatedDeletes", 0L);
            
            // 计算每日平均操作数（基于估计值）
            if (existenceDays > 0) {
                result.put("avgDailyInserts", (double) rowCount / existenceDays);
                result.put("avgDailyUpdates", 0.0);
                result.put("avgDailyDeletes", 0.0);
                result.put("avgDailyOperations", (double) rowCount / existenceDays);
            }
            
            // 添加更新频率描述
            if (updateTime != null && createTime != null) {
                long daysSinceLastUpdate = (System.currentTimeMillis() - updateTime.getTime()) / (1000 * 60 * 60 * 24);
                if (daysSinceLastUpdate == 0) {
                    result.put("updateFrequencyDesc", "今日有更新");
                } else if (daysSinceLastUpdate < 7) {
                    result.put("updateFrequencyDesc", "一周内有更新");
                } else if (daysSinceLastUpdate < 30) {
                    result.put("updateFrequencyDesc", "一月内有更新");
                } else {
                    result.put("updateFrequencyDesc", "长期未更新");
                }
            } else {
                result.put("updateFrequencyDesc", "未知");
            }
            
            // 尝试从自定义监控表获取更多信息
            try {
                String monitorSql = "SELECT UPDATE_TIME, OPERATION_TYPE, OPERATION_COUNT " +
                                   "FROM TABLE_OPERATIONS_HISTORY " +  // 假设存在这样的表
                                   "WHERE TABLE_NAME = ? " +
                                   "ORDER BY UPDATE_TIME DESC LIMIT 30";
                
                try (PreparedStatement stmt = conn.prepareStatement(monitorSql)) {
                    stmt.setString(1, tableName.toUpperCase());
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<Map<String, Object>> history = new ArrayList<>();
                        while (rs.next()) {
                            Map<String, Object> entry = new HashMap<>();
                            entry.put("updateTime", rs.getTimestamp("UPDATE_TIME"));
                            entry.put("operationType", rs.getString("OPERATION_TYPE"));
                            entry.put("operationCount", rs.getLong("OPERATION_COUNT"));
                            history.add(entry);
                        }
                        if (!history.isEmpty()) {
                            result.put("operationHistory", history);
                        }
                    }
                }
            } catch (SQLException e) {
                // 如果表不存在，忽略错误
                log.debug("操作历史表不存在: {}", e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("获取表更新频率失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getTableGrowthTrend(String tableName, int days) throws Exception {
        List<Map<String, Object>> trendData = new ArrayList<>();
        try (Connection conn = getConnection()) {
            // 获取当前行数
            Long currentRowCount = getTableRowCount(tableName);
            
            // 获取表的创建时间
            Date createTime = getTableCreateTime(tableName);
            
            // 获取表的最后更新时间
            Date updateTime = getTableUpdateTime(tableName);
            
            if (currentRowCount == null || currentRowCount == 0) {
                // 表为空，返回空趋势
                return trendData;
            }
            
            // 计算表存在的天数
            long existenceDays = 1; // 默认至少1天，避免除零错误
            if (createTime != null) {
                long now = System.currentTimeMillis();
                long createTimeMs = createTime.getTime();
                existenceDays = Math.max(1, (now - createTimeMs) / (1000 * 60 * 60 * 24));
            }
            
            // 计算平均每日增长
            double avgDailyGrowth = (double) currentRowCount / existenceDays;
            
            // 尝试从自定义监控表获取历史数据
            try {
                String historySql = "SELECT RECORD_DATE, ROW_COUNT " +
                                   "FROM TABLE_GROWTH_HISTORY " +  // 假设存在这样的表
                                   "WHERE TABLE_NAME = ? " +
                                   "AND RECORD_DATE >= ? " +
                                   "ORDER BY RECORD_DATE";
                
                try (PreparedStatement stmt = conn.prepareStatement(historySql)) {
                    stmt.setString(1, tableName.toUpperCase());
                    
                    // 计算days天前的日期
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DAY_OF_MONTH, -days);
                    Date startDate = cal.getTime();
                    stmt.setDate(2, new java.sql.Date(startDate.getTime()));
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            Map<String, Object> dataPoint = new HashMap<>();
                            dataPoint.put("date", rs.getDate("RECORD_DATE"));
                            dataPoint.put("rowCount", rs.getLong("ROW_COUNT"));
                            trendData.add(dataPoint);
                        }
                    }
                }
            } catch (SQLException e) {
                // 如果表不存在，忽略错误
                log.debug("增长历史表不存在: {}", e.getMessage());
            }
            
            // 如果没有历史数据或历史数据不足，使用估算值
            if (trendData.isEmpty()) {
                // 生成过去days天的估计增长数据
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                
                // 如果表创建时间晚于请求的天数，调整天数
                int actualDays = days;
                if (createTime != null) {
                    Calendar createCal = Calendar.getInstance();
                    createCal.setTime(createTime);
                    createCal.set(Calendar.HOUR_OF_DAY, 0);
                    createCal.set(Calendar.MINUTE, 0);
                    createCal.set(Calendar.SECOND, 0);
                    createCal.set(Calendar.MILLISECOND, 0);
                    
                    long daysBetween = (cal.getTimeInMillis() - createCal.getTimeInMillis()) / (1000 * 60 * 60 * 24);
                    actualDays = (int) Math.min(days, daysBetween + 1);
                }
                
                // 生成每天的估计数据
                for (int i = 0; i < actualDays; i++) {
                    Map<String, Object> dataPoint = new HashMap<>();
                    Date date = cal.getTime();
                    dataPoint.put("date", date);
                    
                    // 估计该日期的行数
                    // 假设线性增长模型
                    long estimatedRows = Math.round(currentRowCount - (avgDailyGrowth * i));
                    // 确保行数不为负
                    estimatedRows = Math.max(0, estimatedRows);
                    dataPoint.put("rowCount", estimatedRows);
                    
                    // 添加到趋势数据
                    trendData.add(dataPoint);
                    
                    // 前一天
                    cal.add(Calendar.DAY_OF_MONTH, -1);
                }
                
                // 反转列表，使其按日期升序排列
                Collections.reverse(trendData);
            }
            
        } catch (Exception e) {
            log.error("获取表增长趋势失败: table={}, days={}, error={}", 
                     tableName, days, e.getMessage(), e);
            throw e;
        }
        return trendData;
    }

    @Override
    public List<Map<String, Object>> getTableDataSample(String tableName, int sampleSize) throws Exception {
        List<Map<String, Object>> samples = new ArrayList<>();
        try (Connection conn = getConnection()) {
            // 获取表的所有列
            List<Map<String, Object>> columns = listColumns(tableName);
            if (columns.isEmpty()) {
                return samples;
            }
            
            // 构建列名列表
            List<String> columnNames = new ArrayList<>();
            for (Map<String, Object> column : columns) {
                columnNames.add((String) column.get("columnName"));
            }
            
            // 构建查询SQL
            String columnList = columnNames.stream()
                .map(this::wrapIdentifier)
                .collect(Collectors.joining(", "));
            
            // Phoenix支持LIMIT子句
            String sql = String.format("SELECT %s FROM %s LIMIT %d", 
                                      columnList, wrapIdentifier(tableName), sampleSize);
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String colName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        row.put(colName, value);
                    }
                    samples.add(row);
                }
            }
            
            // 如果没有获取到样本，尝试使用随机排序
            if (samples.isEmpty() && getTableRowCount(tableName) > 0) {
                // Phoenix不直接支持RAND()函数，使用其他方式获取随机样本
                // 这里使用ROWNUM作为替代
                String altSql = String.format("SELECT %s FROM %s WHERE ROWNUM <= %d", 
                                            columnList, wrapIdentifier(tableName), sampleSize);
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(altSql)) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            String colName = metaData.getColumnName(i);
                            Object value = rs.getObject(i);
                            row.put(colName, value);
                        }
                        samples.add(row);
                    }
                } catch (SQLException e) {
                    // 如果ROWNUM不可用，尝试使用简单的LIMIT
                    log.warn("使用ROWNUM获取样本失败，使用简单LIMIT: {}", e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("获取表数据样本失败: table={}, sampleSize={}, error={}", 
                     tableName, sampleSize, e.getMessage(), e);
            throw e;
        }
        return samples;
    }

    @Override
    public Map<String, Object> calculateQualityMetric(String tableName, String metricType) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        try {
            switch (metricType.toLowerCase()) {
                case "completeness":
                    result = getTableCompleteness(tableName);
                    break;
                case "accuracy":
                    result = getTableAccuracy(tableName);
                    break;
                case "consistency":
                    result = getTableConsistency(tableName);
                    break;
                case "uniqueness":
                    result = getTableUniqueness(tableName);
                    break;
                case "validity":
                    result = getTableValidity(tableName);
                    break;
                default:
                    log.warn("Unsupported metric type: {}", metricType);
                    result.put("error", "Unsupported metric type: " + metricType);
            }
        } catch (Exception e) {
            log.error("Error calculating quality metric for table {}: {}", tableName, e.getMessage(), e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getQualityIssues(String tableName) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        try (Connection conn = getConnection()) {
            // Get completeness issues
            Map<String, Object> completeness = getTableCompleteness(tableName);
            result.put("completeness", completeness);
            
            // Get accuracy issues
            Map<String, Object> accuracy = getTableAccuracy(tableName);
            result.put("accuracy", accuracy);
            
            // Get consistency issues
            Map<String, Object> consistency = getTableConsistency(tableName);
            result.put("consistency", consistency);
            
            // Get uniqueness issues
            Map<String, Object> uniqueness = getTableUniqueness(tableName);
            result.put("uniqueness", uniqueness);
            
            // Get validity issues
            Map<String, Object> validity = getTableValidity(tableName);
            result.put("validity", validity);
            
            // Get issue counts
            Map<String, Integer> issueCounts = getQualityIssueCount(tableName);
            result.put("issueCounts", issueCounts);
            
            // Get issue distribution
            Map<String, Double> issueDistribution = getQualityIssueDistribution(tableName);
            result.put("issueDistribution", issueDistribution);
        } catch (Exception e) {
            log.error("Error getting quality issues for table {}: {}", tableName, e.getMessage(), e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @Override
    public String getTableEngine(String tableName) throws Exception {
        // HBase doesn't have the concept of storage engines like MySQL
        // Return HBase's storage mechanism
        return "HBase/Phoenix";
    }

    @Override
    public String getTableCharset(String tableName) throws Exception {
        // HBase/Phoenix doesn't have the concept of character sets at the table level
        // It generally uses UTF-8 for string storage
        return "UTF-8";
    }

    @Override
    public String getTableCollation(String tableName) throws Exception {
        // HBase/Phoenix doesn't have the concept of collations at the table level
        // Return a default value
        return "N/A";
    }

    @Override
    public Map<String, Object> getTableCompleteness(String tableName) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        try (Connection conn = getConnection()) {
            // Get column information
            List<Map<String, Object>> columns = listColumns(tableName);
            
            // Get row count
            Long rowCount = getTableRowCount(tableName);
            result.put("rowCount", rowCount);
            
            // Calculate null counts and rates for each column
            List<Map<String, Object>> columnStats = new ArrayList<>();
            
            for (Map<String, Object> column : columns) {
                String columnName = (String) column.get("COLUMN_NAME");
                
                // Skip system columns
                if (columnName.startsWith("_")) {
                    continue;
                }
                
                Map<String, Object> columnStat = new HashMap<>();
                columnStat.put("columnName", columnName);
                
                // Count null values
                String sql = String.format(
                    "SELECT COUNT(*) AS total_count, COUNT(\"%s\") AS non_null_count FROM \"%s\"",
                    columnName, tableName
                );
                
                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    
                    if (rs.next()) {
                        long totalCount = rs.getLong("total_count");
                        long nonNullCount = rs.getLong("non_null_count");
                        long nullCount = totalCount - nonNullCount;
                        double nullRate = totalCount > 0 ? (double) nullCount / totalCount : 0.0;
                        double completenessRate = totalCount > 0 ? (double) nonNullCount / totalCount : 1.0;
                        
                        columnStat.put("totalCount", totalCount);
                        columnStat.put("nonNullCount", nonNullCount);
                        columnStat.put("nullCount", nullCount);
                        columnStat.put("nullRate", nullRate);
                        columnStat.put("completenessRate", completenessRate);
                    }
                } catch (SQLException e) {
                    log.warn("Error calculating completeness for column {}.{}: {}", 
                             tableName, columnName, e.getMessage());
                    columnStat.put("error", e.getMessage());
                }
                
                columnStats.add(columnStat);
            }
            
            result.put("columnStats", columnStats);
            
            // Calculate overall completeness
            double overallCompleteness = 0.0;
            int validColumns = 0;
            
            for (Map<String, Object> stat : columnStats) {
                if (stat.containsKey("completenessRate")) {
                    overallCompleteness += (double) stat.get("completenessRate");
                    validColumns++;
                }
            }
            
            if (validColumns > 0) {
                overallCompleteness /= validColumns;
            }
            
            result.put("overallCompleteness", overallCompleteness);
            
        } catch (Exception e) {
            log.error("Error calculating completeness for table {}: {}", tableName, e.getMessage(), e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getTableAccuracy(String tableName) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        try (Connection conn = getConnection()) {
            // Get column information
            List<Map<String, Object>> columns = listColumns(tableName);
            
            // Calculate accuracy metrics for each column
            List<Map<String, Object>> columnStats = new ArrayList<>();
            
            for (Map<String, Object> column : columns) {
                String columnName = (String) column.get("COLUMN_NAME");
                String dataType = (String) column.get("DATA_TYPE");
                
                // Skip system columns
                if (columnName.startsWith("_")) {
                    continue;
                }
                
                Map<String, Object> columnStat = new HashMap<>();
                columnStat.put("columnName", columnName);
                columnStat.put("dataType", dataType);
                
                // For numeric columns, check for values within expected range
                if (dataType.contains("INT") || dataType.contains("FLOAT") || 
                    dataType.contains("DOUBLE") || dataType.contains("DECIMAL")) {
                    
                    try {
                        Map<String, Object> range = getColumnValueRange(tableName, columnName);
                        columnStat.putAll(range);
                        
                        // Add accuracy metrics based on range
                        if (range.containsKey("min") && range.containsKey("max")) {
                            columnStat.put("hasRangeInfo", true);
                        } else {
                            columnStat.put("hasRangeInfo", false);
                        }
                    } catch (Exception e) {
                        log.warn("Error getting range for column {}.{}: {}", 
                                 tableName, columnName, e.getMessage());
                        columnStat.put("rangeError", e.getMessage());
                    }
                }
                
                // For date/time columns, check for values within valid range
                else if (dataType.contains("DATE") || dataType.contains("TIME")) {
                    try {
                        String sql = String.format(
                            "SELECT COUNT(*) AS total_count, " +
                            "COUNT(CASE WHEN \"%s\" > TO_DATE('1900-01-01') AND \"%s\" < TO_DATE('2100-01-01') THEN 1 END) AS valid_count " +
                            "FROM \"%s\"",
                            columnName, columnName, tableName
                        );
                        
                        try (PreparedStatement stmt = conn.prepareStatement(sql);
                             ResultSet rs = stmt.executeQuery()) {
                            
                            if (rs.next()) {
                                long totalCount = rs.getLong("total_count");
                                long validCount = rs.getLong("valid_count");
                                double validRate = totalCount > 0 ? (double) validCount / totalCount : 1.0;
                                
                                columnStat.put("totalCount", totalCount);
                                columnStat.put("validCount", validCount);
                                columnStat.put("validRate", validRate);
                            }
                        }
                    } catch (SQLException e) {
                        log.warn("Error calculating date accuracy for column {}.{}: {}", 
                                 tableName, columnName, e.getMessage());
                        columnStat.put("dateError", e.getMessage());
                    }
                }
                
                // For string columns, check for pattern compliance if applicable
                else if (dataType.contains("CHAR") || dataType.contains("VARCHAR")) {
                    // This would require domain-specific knowledge about expected patterns
                    // For now, just report the length distribution
                    try {
                        String sql = String.format(
                            "SELECT " +
                            "COUNT(*) AS total_count, " +
                            "AVG(LENGTH(\"%s\")) AS avg_length, " +
                            "MIN(LENGTH(\"%s\")) AS min_length, " +
                            "MAX(LENGTH(\"%s\")) AS max_length " +
                            "FROM \"%s\" WHERE \"%s\" IS NOT NULL",
                            columnName, columnName, columnName, tableName, columnName
                        );
                        
                        try (PreparedStatement stmt = conn.prepareStatement(sql);
                             ResultSet rs = stmt.executeQuery()) {
                            
                            if (rs.next()) {
                                long totalCount = rs.getLong("total_count");
                                double avgLength = rs.getDouble("avg_length");
                                int minLength = rs.getInt("min_length");
                                int maxLength = rs.getInt("max_length");
                                
                                columnStat.put("totalCount", totalCount);
                                columnStat.put("avgLength", avgLength);
                                columnStat.put("minLength", minLength);
                                columnStat.put("maxLength", maxLength);
                            }
                        }
                    } catch (SQLException e) {
                        log.warn("Error calculating string accuracy for column {}.{}: {}", 
                                 tableName, columnName, e.getMessage());
                        columnStat.put("stringError", e.getMessage());
                    }
                }
                
                columnStats.add(columnStat);
            }
            
            result.put("columnStats", columnStats);
            
            // Calculate overall accuracy (simplified)
            double overallAccuracy = 0.95; // Default assumption
            result.put("overallAccuracy", overallAccuracy);
            result.put("note", "Detailed accuracy calculation requires domain-specific rules");
            
        } catch (Exception e) {
            log.error("Error calculating accuracy for table {}: {}", tableName, e.getMessage(), e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getTableConsistency(String tableName) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Check for primary key consistency
            List<String> primaryKeys = getPrimaryKeys(tableName);
            result.put("hasPrimaryKey", !primaryKeys.isEmpty());
            result.put("primaryKeys", primaryKeys);
            
            // Check for duplicate records based on primary key
            if (!primaryKeys.isEmpty()) {
                try (Connection conn = getConnection()) {
                    String pkColumns = primaryKeys.stream()
                        .map(pk -> "\"" + pk + "\"")
                        .collect(Collectors.joining(", "));
                    
                    String sql = String.format(
                        "SELECT %s, COUNT(*) as count FROM \"%s\" GROUP BY %s HAVING COUNT(*) > 1",
                        pkColumns, tableName, pkColumns
                    );
                    
                    try (PreparedStatement stmt = conn.prepareStatement(sql);
                         ResultSet rs = stmt.executeQuery()) {
                        
                        List<Map<String, Object>> duplicates = new ArrayList<>();
                        while (rs.next()) {
                            Map<String, Object> duplicate = new HashMap<>();
                            for (String pk : primaryKeys) {
                                duplicate.put(pk, rs.getObject(pk));
                            }
                            duplicate.put("count", rs.getLong("count"));
                            duplicates.add(duplicate);
                        }
                        
                        result.put("hasDuplicatePrimaryKeys", !duplicates.isEmpty());
                        result.put("duplicatePrimaryKeys", duplicates);
                    }
                } catch (SQLException e) {
                    log.warn("Error checking primary key consistency for table {}: {}", 
                             tableName, e.getMessage());
                    result.put("primaryKeyConsistencyError", e.getMessage());
                }
            }
            
            // Check for foreign key consistency
            List<Map<String, Object>> foreignKeys = getForeignKeys(tableName);
            result.put("hasForeignKeys", !foreignKeys.isEmpty());
            result.put("foreignKeys", foreignKeys);
            
            // Note: HBase/Phoenix doesn't enforce foreign key constraints by default
            result.put("note", "HBase/Phoenix doesn't enforce foreign key constraints by default");
            
            // Overall consistency score (simplified)
            double consistencyScore = result.containsKey("hasDuplicatePrimaryKeys") && 
                                     !(boolean)result.get("hasDuplicatePrimaryKeys") ? 1.0 : 0.5;
            result.put("consistencyScore", consistencyScore);
            
        } catch (Exception e) {
            log.error("Error calculating consistency for table {}: {}", tableName, e.getMessage(), e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getTableUniqueness(String tableName) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        try (Connection conn = getConnection()) {
            // Get column information
            List<Map<String, Object>> columns = listColumns(tableName);
            
            // Get row count
            Long rowCount = getTableRowCount(tableName);
            result.put("rowCount", rowCount);
            
            // Check uniqueness for each column
            List<Map<String, Object>> columnStats = new ArrayList<>();
            
            for (Map<String, Object> column : columns) {
                String columnName = (String) column.get("COLUMN_NAME");
                
                // Skip system columns
                if (columnName.startsWith("_")) {
                    continue;
                }
                
                Map<String, Object> columnStat = new HashMap<>();
                columnStat.put("columnName", columnName);
                
                // Count distinct values
                String sql = String.format(
                    "SELECT COUNT(*) AS total_count, COUNT(DISTINCT \"%s\") AS distinct_count " +
                    "FROM \"%s\" WHERE \"%s\" IS NOT NULL",
                    columnName, tableName, columnName
                );
                
                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    
                    if (rs.next()) {
                        long totalCount = rs.getLong("total_count");
                        long distinctCount = rs.getLong("distinct_count");
                        double uniquenessRate = totalCount > 0 ? (double) distinctCount / totalCount : 1.0;
                        
                        columnStat.put("totalCount", totalCount);
                        columnStat.put("distinctCount", distinctCount);
                        columnStat.put("duplicateCount", totalCount - distinctCount);
                        columnStat.put("uniquenessRate", uniquenessRate);
                        
                        // Check if column is completely unique
                        columnStat.put("isUnique", distinctCount == totalCount);
                    }
                } catch (SQLException e) {
                    log.warn("Error calculating uniqueness for column {}.{}: {}", 
                             tableName, columnName, e.getMessage());
                    columnStat.put("error", e.getMessage());
                }
                
                // Get top duplicate values if there are duplicates
                if (columnStat.containsKey("duplicateCount") && (long)columnStat.get("duplicateCount") > 0) {
                    String dupSql = String.format(
                        "SELECT \"%s\" AS value, COUNT(*) AS count " +
                        "FROM \"%s\" " +
                        "WHERE \"%s\" IS NOT NULL " +
                        "GROUP BY \"%s\" " +
                        "HAVING COUNT(*) > 1 " +
                        "ORDER BY COUNT(*) DESC " +
                        "LIMIT 10",
                        columnName, tableName, columnName, columnName
                    );
                    
                    try (PreparedStatement stmt = conn.prepareStatement(dupSql);
                         ResultSet rs = stmt.executeQuery()) {
                        
                        List<Map<String, Object>> topDuplicates = new ArrayList<>();
                        while (rs.next()) {
                            Map<String, Object> dup = new HashMap<>();
                            dup.put("value", rs.getObject("value"));
                            dup.put("count", rs.getLong("count"));
                            topDuplicates.add(dup);
                        }
                        
                        columnStat.put("topDuplicates", topDuplicates);
                    } catch (SQLException e) {
                        log.warn("Error getting top duplicates for column {}.{}: {}", 
                                 tableName, columnName, e.getMessage());
                        columnStat.put("topDuplicatesError", e.getMessage());
                    }
                }
                
                columnStats.add(columnStat);
            }
            
            result.put("columnStats", columnStats);
            
            // Calculate overall uniqueness
            double overallUniqueness = 0.0;
            int validColumns = 0;
            
            for (Map<String, Object> stat : columnStats) {
                if (stat.containsKey("uniquenessRate")) {
                    overallUniqueness += (double) stat.get("uniquenessRate");
                    validColumns++;
                }
            }
            
            if (validColumns > 0) {
                overallUniqueness /= validColumns;
            }
            
            result.put("overallUniqueness", overallUniqueness);
            
        } catch (Exception e) {
            log.error("Error calculating uniqueness for table {}: {}", tableName, e.getMessage(), e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getTableValidity(String tableName) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        try (Connection conn = getConnection()) {
            // Get column information
            List<Map<String, Object>> columns = listColumns(tableName);
            
            // Calculate validity metrics for each column
            List<Map<String, Object>> columnStats = new ArrayList<>();
            
            for (Map<String, Object> column : columns) {
                String columnName = (String) column.get("COLUMN_NAME");
                String dataType = (String) column.get("DATA_TYPE");
                
                // Skip system columns
                if (columnName.startsWith("_")) {
                    continue;
                }
                
                Map<String, Object> columnStat = new HashMap<>();
                columnStat.put("columnName", columnName);
                columnStat.put("dataType", dataType);
                
                // For numeric columns, check for values within valid range
                if (dataType.contains("INT") || dataType.contains("FLOAT") || 
                    dataType.contains("DOUBLE") || dataType.contains("DECIMAL")) {
                    
                    // Define reasonable bounds based on data type
                    String minBound = "0";
                    String maxBound = "9999999999";
                    
                    if (dataType.contains("TINYINT")) {
                        minBound = "-128";
                        maxBound = "127";
                    } else if (dataType.contains("SMALLINT")) {
                        minBound = "-32768";
                        maxBound = "32767";
                    } else if (dataType.contains("INTEGER")) {
                        minBound = "-2147483648";
                        maxBound = "2147483647";
                    } else if (dataType.contains("BIGINT")) {
                        minBound = "-9223372036854775808";
                        maxBound = "9223372036854775807";
                    }
                    
                    try {
                        String sql = String.format(
                            "SELECT COUNT(*) AS total_count, " +
                            "COUNT(CASE WHEN \"%s\" >= %s AND \"%s\" <= %s THEN 1 END) AS valid_count " +
                            "FROM \"%s\" WHERE \"%s\" IS NOT NULL",
                            columnName, minBound, columnName, maxBound, tableName, columnName
                        );
                        
                        try (PreparedStatement stmt = conn.prepareStatement(sql);
                             ResultSet rs = stmt.executeQuery()) {
                            
                            if (rs.next()) {
                                long totalCount = rs.getLong("total_count");
                                long validCount = rs.getLong("valid_count");
                                double validRate = totalCount > 0 ? (double) validCount / totalCount : 1.0;
                                
                                columnStat.put("totalCount", totalCount);
                                columnStat.put("validCount", validCount);
                                columnStat.put("invalidCount", totalCount - validCount);
                                columnStat.put("validRate", validRate);
                            }
                        }
                    } catch (SQLException e) {
                        log.warn("Error calculating numeric validity for column {}.{}: {}", 
                                 tableName, columnName, e.getMessage());
                        columnStat.put("numericError", e.getMessage());
                    }
                }
                
                // For date/time columns, check for values within valid range
                else if (dataType.contains("DATE") || dataType.contains("TIME")) {
                    try {
                        String sql = String.format(
                            "SELECT COUNT(*) AS total_count, " +
                            "COUNT(CASE WHEN \"%s\" > TO_DATE('1900-01-01') AND \"%s\" < TO_DATE('2100-01-01') THEN 1 END) AS valid_count " +
                            "FROM \"%s\" WHERE \"%s\" IS NOT NULL",
                            columnName, columnName, tableName, columnName
                        );
                        
                        try (PreparedStatement stmt = conn.prepareStatement(sql);
                             ResultSet rs = stmt.executeQuery()) {
                            
                            if (rs.next()) {
                                long totalCount = rs.getLong("total_count");
                                long validCount = rs.getLong("valid_count");
                                double validRate = totalCount > 0 ? (double) validCount / totalCount : 1.0;
                                
                                columnStat.put("totalCount", totalCount);
                                columnStat.put("validCount", validCount);
                                columnStat.put("invalidCount", totalCount - validCount);
                                columnStat.put("validRate", validRate);
                            }
                        }
                    } catch (SQLException e) {
                        log.warn("Error calculating date validity for column {}.{}: {}", 
                                 tableName, columnName, e.getMessage());
                        columnStat.put("dateError", e.getMessage());
                    }
                }
                
                // For string columns, validity would depend on domain-specific rules
                else if (dataType.contains("CHAR") || dataType.contains("VARCHAR")) {
                    // For now, just report non-null values as valid
                    try {
                        String sql = String.format(
                            "SELECT COUNT(*) AS total_count " +
                            "FROM \"%s\" WHERE \"%s\" IS NOT NULL",
                            tableName, columnName
                        );
                        
                        try (PreparedStatement stmt = conn.prepareStatement(sql);
                             ResultSet rs = stmt.executeQuery()) {
                            
                            if (rs.next()) {
                                long totalCount = rs.getLong("total_count");
                                
                                columnStat.put("totalCount", totalCount);
                                columnStat.put("validCount", totalCount); // Assuming all non-null values are valid
                                columnStat.put("invalidCount", 0);
                                columnStat.put("validRate", 1.0);
                                columnStat.put("note", "Domain-specific validation rules not applied");
                            }
                        }
                    } catch (SQLException e) {
                        log.warn("Error calculating string validity for column {}.{}: {}", 
                                 tableName, columnName, e.getMessage());
                        columnStat.put("stringError", e.getMessage());
                    }
                }
                
                columnStats.add(columnStat);
            }
            
            result.put("columnStats", columnStats);
            
            // Calculate overall validity
            double overallValidity = 0.0;
            int validColumns = 0;
            
            for (Map<String, Object> stat : columnStats) {
                if (stat.containsKey("validRate")) {
                    overallValidity += (double) stat.get("validRate");
                    validColumns++;
                }
            }
            
            if (validColumns > 0) {
                overallValidity /= validColumns;
            }
            
            result.put("overallValidity", overallValidity);
            
        } catch (Exception e) {
            log.error("Error calculating validity for table {}: {}", tableName, e.getMessage(), e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Integer> getQualityIssueCount(String tableName) throws Exception {
        Map<String, Integer> result = new HashMap<>();
        
        try {
            // Get completeness issues
            Map<String, Object> completeness = getTableCompleteness(tableName);
            int completenessIssues = 0;
            
            if (completeness.containsKey("columnStats")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> columnStats = (List<Map<String, Object>>) completeness.get("columnStats");
                
                for (Map<String, Object> stat : columnStats) {
                    if (stat.containsKey("nullCount")) {
                        completenessIssues += ((Number) stat.get("nullCount")).intValue();
                    }
                }
            }
            
            result.put("completenessIssues", completenessIssues);
            
            // Get accuracy issues
            Map<String, Object> accuracy = getTableAccuracy(tableName);
            int accuracyIssues = 0;
            
            // Accuracy issues are more domain-specific and would require custom logic
            // For now, provide a placeholder
            result.put("accuracyIssues", accuracyIssues);
            
            // Get consistency issues
            Map<String, Object> consistency = getTableConsistency(tableName);
            int consistencyIssues = 0;
            
            if (consistency.containsKey("duplicatePrimaryKeys")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> duplicates = (List<Map<String, Object>>) consistency.get("duplicatePrimaryKeys");
                consistencyIssues = duplicates.size();
            }
            
            result.put("consistencyIssues", consistencyIssues);
            
            // Get uniqueness issues
            Map<String, Object> uniqueness = getTableUniqueness(tableName);
            int uniquenessIssues = 0;
            
            if (uniqueness.containsKey("columnStats")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> columnStats = (List<Map<String, Object>>) uniqueness.get("columnStats");
                
                for (Map<String, Object> stat : columnStats) {
                    if (stat.containsKey("duplicateCount")) {
                        uniquenessIssues += ((Number) stat.get("duplicateCount")).intValue();
                    }
                }
            }
            
            result.put("uniquenessIssues", uniquenessIssues);
            
            // Get validity issues
            Map<String, Object> validity = getTableValidity(tableName);
            int validityIssues = 0;
            
            if (validity.containsKey("columnStats")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> columnStats = (List<Map<String, Object>>) validity.get("columnStats");
                
                for (Map<String, Object> stat : columnStats) {
                    if (stat.containsKey("invalidCount")) {
                        validityIssues += ((Number) stat.get("invalidCount")).intValue();
                    }
                }
            }
            
            result.put("validityIssues", validityIssues);
            
            // Calculate total issues
            int totalIssues = completenessIssues + accuracyIssues + consistencyIssues + 
                             uniquenessIssues + validityIssues;
            result.put("totalIssues", totalIssues);
            
        } catch (Exception e) {
            log.error("Error calculating quality issue counts for table {}: {}", tableName, e.getMessage(), e);
            result.put("error", -1);
        }
        
        return result;
    }

    @Override
    public Map<String, Double> getQualityIssueDistribution(String tableName) throws Exception {
        Map<String, Double> result = new HashMap<>();
        
        try {
            // Get issue counts
            Map<String, Integer> issueCounts = getQualityIssueCount(tableName);
            
            // Calculate total issues
            int totalIssues = issueCounts.getOrDefault("totalIssues", 0);
            
            if (totalIssues > 0) {
                // Calculate distribution percentages
                for (Map.Entry<String, Integer> entry : issueCounts.entrySet()) {
                    if (!entry.getKey().equals("totalIssues") && !entry.getKey().equals("error")) {
                        double percentage = (double) entry.getValue() / totalIssues;
                        result.put(entry.getKey() + "Percentage", percentage);
                    }
                }
            } else {
                // If no issues, set all percentages to 0
                result.put("completenessIssuesPercentage", 0.0);
                result.put("accuracyIssuesPercentage", 0.0);
                result.put("consistencyIssuesPercentage", 0.0);
                result.put("uniquenessIssuesPercentage", 0.0);
                result.put("validityIssuesPercentage", 0.0);
            }
            
            // Add total issues count for reference
            result.put("totalIssues", (double) totalIssues);
            
        } catch (Exception e) {
            log.error("Error calculating quality issue distribution for table {}: {}", tableName, e.getMessage(), e);
            result.put("error", -1.0);
        }
        
        return result;
    }

    @Override
    public Map<String, String> getStoredProcedureDefinitions(String tableName) throws Exception {
        // HBase不支持存储过程，返回空Map
        log.info("HBase不支持存储过程，返回空结果: table={}", tableName);
        return new HashMap<>();
    }

    // 获取表的区域信息
    public String getRegionInfoSql(String tableName) {
        return "SELECT REGION_NAME, START_KEY, END_KEY, GUIDE_POSTS, GUIDE_POST_WIDTH " +
               "FROM SYSTEM.STATS WHERE PHYSICAL_NAME LIKE '" + 
               tableName.toUpperCase() + "%'";
    }

    // 设置表分割点
    public String getPreSplitRegionSql(String tableName, List<String> splitPoints) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ");
        sql.append(wrapIdentifier(tableName));
        sql.append(" (...) SPLIT ON ");
        List<String> formattedSplitPoints = new ArrayList<>();
        for (String splitPoint : splitPoints) {
            formattedSplitPoints.add("'" + splitPoint + "'");
        }
        sql.append("(").append(String.join(", ", formattedSplitPoints)).append(")");
        return sql.toString();
    }

    // 创建命名空间
    public String getCreateNamespaceSql(String namespace) {
        return "CREATE SCHEMA IF NOT EXISTS " + namespace;
    }

    // 设置全局二级索引
    public String getCreateGlobalIndexSql(String tableName, String indexName, List<String> columns) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE INDEX ");
        sql.append(wrapIdentifier(indexName));
        sql.append(" ON ");
        sql.append(wrapIdentifier(tableName));
        sql.append(" (");
        sql.append(columns.stream()
                 .map(this::wrapIdentifier)
                 .collect(Collectors.joining(", ")));
        sql.append(")");
        return sql.toString();
    }

    // 批量UPSERT（更新插入）
    public String generateBatchUpsertSql(String tableName, List<String> columns) {
        StringBuilder sql = new StringBuilder();
        sql.append("UPSERT INTO ");
        sql.append(wrapIdentifier(tableName));
        sql.append(" (");
        sql.append(columns.stream()
                 .map(this::wrapIdentifier)
                 .collect(Collectors.joining(", ")));
        sql.append(") VALUES (");
        sql.append(columns.stream()
                 .map(col -> "?")
                 .collect(Collectors.joining(", ")));
        sql.append(")");
        return sql.toString();
    }

    // 添加收集表统计信息的方法
    public String getUpdateStatisticsSql(String tableName) {
        return "UPDATE STATISTICS " + wrapIdentifier(tableName);
    }

    // 添加生成高效 WHERE 子句的辅助方法
    public String generateOptimizedWhereSql(String tableName, Map<String, Object> conditions) {
        // 检查条件中是否包含主键列
        List<String> pkColumns = new ArrayList<>();
        try {
            pkColumns = getPrimaryKeys(tableName);
        } catch (Exception e) {
            // 忽略异常
        }
        
        StringBuilder whereSql = new StringBuilder();
        
        // 先添加主键条件（效率更高）
        for (String pkColumn : pkColumns) {
            if (conditions.containsKey(pkColumn)) {
                if (whereSql.length() > 0) {
                    whereSql.append(" AND ");
                }
                whereSql.append(wrapIdentifier(pkColumn))
                        .append(" = ")
                        .append(wrapValue(conditions.get(pkColumn)));
            }
        }
        
        // 再添加非主键条件
        for (Map.Entry<String, Object> entry : conditions.entrySet()) {
            String column = entry.getKey();
            if (!pkColumns.contains(column)) {
                if (whereSql.length() > 0) {
                    whereSql.append(" AND ");
                }
                whereSql.append(wrapIdentifier(column))
                        .append(" = ")
                        .append(wrapValue(entry.getValue()));
            }
        }
        
        return whereSql.length() > 0 ? " WHERE " + whereSql.toString() : "";
    }
    
    @Override
    public String getCreateTempTableSql(String tempTableName, String sourceTableName, boolean preserveRows) throws Exception {
        // HBase/Phoenix不直接支持临时表，所以我们创建一个普通表
        // 注意：客户端需要手动删除这个表
        List<Map<String, Object>> columnsData = listColumns(sourceTableName);
        
        if (columnsData == null || columnsData.isEmpty()) {
            throw new IllegalArgumentException("Cannot create temp table - no columns found in source table: " + sourceTableName);
        }
        
        // 将 Map 列表转换为 ColumnDefinition 列表
        List<ColumnDefinition> columns = new ArrayList<>();
        for (Map<String, Object> columnData : columnsData) {
            ColumnDefinition column = new ColumnDefinition();
            column.setName((String) columnData.get("COLUMN_NAME"));
            column.setType((String) columnData.get("DATA_TYPE"));
            
            if (columnData.get("ARRAY_SIZE") != null) {
                column.setLength(((Number) columnData.get("ARRAY_SIZE")).intValue());
            }
            
            // HBase/Phoenix的主键信息
            if (columnData.get("KEY_SEQ") != null && ((Number) columnData.get("KEY_SEQ")).intValue() > 0) {
                column.setPrimaryKey(true);
            }
            
            // 可空性
            column.setNullable(columnData.containsKey("NULLABLE") ? 
                "YES".equalsIgnoreCase((String) columnData.get("NULLABLE")) : true);
            
            columns.add(column);
        }
        
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("CREATE TABLE ")
                 .append(wrapIdentifier(tempTableName))
                 .append(" (");
        
        // 获取主键
        List<String> primaryKeys = getPrimaryKeys(sourceTableName);
        boolean hasPrimaryKey = primaryKeys != null && !primaryKeys.isEmpty();
        
        // 处理所有列
        for (int i = 0; i < columns.size(); i++) {
            ColumnDefinition column = columns.get(i);
            
            if (i > 0) {
                sqlBuilder.append(", ");
            }
            
            sqlBuilder.append(wrapIdentifier(column.getName()))
                    .append(" ")
                    .append(column.getType());
            
            // 添加数组大小信息
            if (column.getLength() != null && column.getLength() > 0 
                    && column.getType().toUpperCase().contains("ARRAY")) {
                sqlBuilder.append("[").append(column.getLength()).append("]");
            }
            
            // 添加NULL约束
            if (!column.isNullable()) {
                sqlBuilder.append(" NOT NULL");
            }
            
            // 添加主键约束
            if (column.isPrimaryKey() || (hasPrimaryKey && primaryKeys.contains(column.getName()))) {
                if (i == 0) {
                    sqlBuilder.append(" PRIMARY KEY");
                }
            }
        }
        
        sqlBuilder.append(")");
        
        // Phoenix特有的表属性
        sqlBuilder.append(" COLUMN_ENCODED_BYTES=0");
        
        // 如果不保留临时表数据，添加TRANSACTIONAL=true以便稍后清理
        if (!preserveRows) {
            sqlBuilder.append(", TRANSACTIONAL=true");
        }
        
        return sqlBuilder.toString();
    }
    
    @Override
    public List<String> getTablePartitions(String tableName, String partitionField) throws Exception {
        List<String> partitions = new ArrayList<>();
        
        // HBase/Phoenix doesn't have built-in partitioning like traditional RDBMS
        // However, we can still get distinct values for a column
        String sql = String.format("SELECT DISTINCT %s FROM %s ORDER BY %s", 
                wrapIdentifier(partitionField), 
                wrapIdentifier(tableName),
                wrapIdentifier(partitionField));
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Object value = rs.getObject(1);
                if (value != null) {
                    partitions.add(value.toString());
                }
            }
        } catch (Exception e) {
            log.error("Failed to get partition values from HBase: {}", e.getMessage());
        }
        
        return partitions;
    }
} 