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

public class HiveHandler extends AbstractDatabaseHandler {

    private static final String DRIVER_CLASS = "org.apache.hive.jdbc.HiveDriver";
    private static final String DEFAULT_PORT = "10000";
    private static final String URL_TEMPLATE = "jdbc:hive2://%s:%s/%s";

    private static final int DEFAULT_VARCHAR_LENGTH = 255;
    private static final int MAX_VARCHAR_LENGTH = 65535;

    private static final Map<String, String> TYPE_MAPPING = new HashMap<>();
    private static final Map<String, Map<String, String>> COMMON_TYPE_MAPPING = new HashMap<>();

    static {
        // 基本类型映射
        TYPE_MAPPING.put("string", "STRING");
        TYPE_MAPPING.put("text", "STRING");
        TYPE_MAPPING.put("int", "INT");
        TYPE_MAPPING.put("bigint", "BIGINT");
        TYPE_MAPPING.put("float", "FLOAT");
        TYPE_MAPPING.put("double", "DOUBLE");
        TYPE_MAPPING.put("decimal", "DECIMAL");
        TYPE_MAPPING.put("boolean", "BOOLEAN");
        TYPE_MAPPING.put("date", "DATE");
        TYPE_MAPPING.put("timestamp", "TIMESTAMP");
        TYPE_MAPPING.put("binary", "BINARY");

        // MySQL 类型映射
        Map<String, String> mysqlMapping = new HashMap<>();
        mysqlMapping.put("VARCHAR", "STRING");
        mysqlMapping.put("CHAR", "STRING");
        mysqlMapping.put("TEXT", "STRING");
        mysqlMapping.put("INT", "INT");
        mysqlMapping.put("BIGINT", "BIGINT");
        mysqlMapping.put("FLOAT", "FLOAT");
        mysqlMapping.put("DOUBLE", "DOUBLE");
        mysqlMapping.put("DECIMAL", "DECIMAL");
        mysqlMapping.put("BOOLEAN", "BOOLEAN");
        mysqlMapping.put("DATE", "DATE");
        mysqlMapping.put("TIMESTAMP", "TIMESTAMP");
        mysqlMapping.put("BLOB", "BINARY");
        COMMON_TYPE_MAPPING.put("mysql", mysqlMapping);

        // Oracle 类型映射
        Map<String, String> oracleMapping = new HashMap<>();
        oracleMapping.put("VARCHAR2", "STRING");
        oracleMapping.put("CHAR", "STRING");
        oracleMapping.put("NUMBER", "DECIMAL");
        oracleMapping.put("DATE", "DATE");
        oracleMapping.put("TIMESTAMP", "TIMESTAMP");
        oracleMapping.put("BLOB", "BINARY");
        COMMON_TYPE_MAPPING.put("oracle", oracleMapping);

        // PostgreSQL 类型映射
        Map<String, String> postgresMapping = new HashMap<>();
        postgresMapping.put("VARCHAR", "STRING");
        postgresMapping.put("CHAR", "STRING");
        postgresMapping.put("TEXT", "STRING");
        postgresMapping.put("INTEGER", "INT");
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

        // 补充 Hive 特有的类型
        TYPE_MAPPING.put("UNIONTYPE", "UNIONTYPE");
        TYPE_MAPPING.put("INTERVAL_YEAR_MONTH", "INTERVAL_YEAR_MONTH");
        TYPE_MAPPING.put("INTERVAL_DAY_TIME", "INTERVAL_DAY_TIME");
    }

    public HiveHandler(DataSource dataSource) {
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
        return "Hive";
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
        return Arrays.asList("default", "information_schema");
    }

    @Override
    public String generatePageSql(String sql, int offset, int limit) {
        // Hive 使用 LIMIT 子句，不支持 OFFSET
        // 为了支持 OFFSET，需要使用窗口函数或子查询
        if (offset <= 0) {
            return sql + " LIMIT " + limit;
        }
        
        // 创建带行号的子查询
        StringBuilder pageSql = new StringBuilder();
        pageSql.append("SELECT * FROM (");
        pageSql.append("SELECT t.*, ROW_NUMBER() OVER() AS rn FROM (");
        pageSql.append(sql);
        pageSql.append(") t) tmp WHERE rn > ").append(offset);
        pageSql.append(" AND rn <= ").append(offset + limit);
        
        return pageSql.toString();
    }

    /**
     * 生成Hive的LIMIT子句
     *
     * @param offset 起始位置
     * @param limit  返回记录的最大数量
     * @return 格式化后的LIMIT子句
     */
    @Override
    public String getLimitClause(long offset, int limit) {
        // Hive 使用 LIMIT 子句，不支持 OFFSET
        if (offset <= 0) {
            return "LIMIT " + limit;
        } else {
            // 对于有偏移量的情况，在完整SQL中需要使用窗口函数
            // 这里只返回 LIMIT 部分，完整实现在 generatePageSql 中
            return "LIMIT " + limit;
        }
    }

    @Override
    public String generateCountSql(String sql) {
        return "SELECT COUNT(*) FROM (" + sql + ") tmp";
    }

    @Override
    public boolean supportsBatchUpdates() {
        return false; // Hive不支持批量更新
    }

    @Override
    public boolean supportsGetGeneratedKeys() {
        return false; // Hive不支持自增主键
    }

    @Override
    public boolean supportsTransactions() {
        return false; // Hive不支持事务
    }

    @Override
    public String getShowTablesSql() {
        return "SHOW TABLES";
    }

    @Override
    public String getShowColumnsSql(String tableName) {
        // 使用 DESCRIBE 命令获取基本的列信息
        return "DESCRIBE " + wrapIdentifier(tableName);
    }

    // 添加获取扩展列信息的方法
    public String getShowExtendedColumnsSql(String tableName) {
        // 使用 DESCRIBE EXTENDED 获取更详细的列信息
        return "DESCRIBE EXTENDED " + wrapIdentifier(tableName);
    }

    // 添加获取格式化列信息的方法
    public String getShowFormattedColumnsSql(String tableName) {
        // 使用 DESCRIBE FORMATTED 获取格式化的列信息
        return "DESCRIBE FORMATTED " + wrapIdentifier(tableName);
    }

    @Override
    public String getAddTableCommentSql(String tableName, String comment) {
        return String.format(
            "ALTER TABLE %s SET TBLPROPERTIES ('comment' = %s)",
            wrapIdentifier(tableName),
            wrapValue(comment)
        );
    }

    @Override
    public String getModifyTableCommentSql(String tableName, String comment) {
        return getAddTableCommentSql(tableName, comment);
    }

    @Override
    public String getAddColumnCommentSql(String tableName, String columnName, String comment) {
        return String.format(
            "ALTER TABLE %s CHANGE COLUMN %s %s COMMENT %s",
            wrapIdentifier(tableName),
            wrapIdentifier(columnName),
            wrapIdentifier(columnName),
            wrapValue(comment)
        );
    }

    @Override
    public String getModifyColumnCommentSql(String tableName, String columnName, String comment) {
        return getAddColumnCommentSql(tableName, columnName, comment);
    }

    @Override
    public String getShowIndexesSql(String tableName) {
        // Hive 3.0 之后已移除索引功能
        // 为了兼容，可以返回一个查询不返回任何结果的 SQL
        return "SELECT NULL AS index_name, NULL AS column_name WHERE 1=0";
    }

    @Override
    public String getAddIndexSql(String tableName, String indexName, List<String> columns, boolean unique) {
        // Hive 3.0 之后已移除索引功能
        // 返回注释说明这一点
        return "-- Hive 3.0 之后已移除索引功能，此操作不适用";
    }

    @Override
    public String getDropIndexSql(String tableName, String indexName) {
        // Hive 3.0 之后已移除索引功能
        // 返回注释说明这一点
        return "-- Hive 3.0 之后已移除索引功能，此操作不适用";
    }

    @Override
    public String getAlterTableEngineSql(String tableName, String engine) {
        // 修改 Hive 表的存储格式
        return String.format(
            "ALTER TABLE %s SET FILEFORMAT %s",
            wrapIdentifier(tableName),
            engine
        );
    }

    @Override
    public String getAlterTableCharsetSql(String tableName, String charset, String collate) {
        // Hive 不支持修改表字符集
        return "";
    }

    @Override
    public boolean isValidFieldLength(String type, int length) {
        type = type.toUpperCase();
        
        // Hive 的特殊处理
        if (type.contains("VARCHAR") || type.contains("CHAR")) {
            return length > 0 && length <= 65535;
        }
        
        if (type.equals("DECIMAL")) {
            return length > 0 && length <= 38;
        }
        
        // Hive 中的 VARCHAR 和 CHAR 有长度限制
        if (type.equalsIgnoreCase("VARCHAR")) {
            return length > 0 && length <= MAX_VARCHAR_LENGTH;
        } else if (type.equalsIgnoreCase("CHAR")) {
            return length > 0 && length <= 255; // Hive 的 CHAR 类型最大长度为 255
        }
        
        // Hive 中的这些类型不需要指定长度
        if (type.equalsIgnoreCase("STRING") || 
            type.equalsIgnoreCase("BOOLEAN") ||
            type.equalsIgnoreCase("INT") ||
            type.equalsIgnoreCase("SMALLINT") ||
            type.equalsIgnoreCase("BIGINT") ||
            type.equalsIgnoreCase("FLOAT") ||
            type.equalsIgnoreCase("DOUBLE") ||
            type.equalsIgnoreCase("DECIMAL") ||
            type.equalsIgnoreCase("DATE") ||
            type.equalsIgnoreCase("TIMESTAMP") ||
            type.equalsIgnoreCase("BINARY") ||
            type.equalsIgnoreCase("ARRAY") ||
            type.equalsIgnoreCase("MAP") ||
            type.equalsIgnoreCase("STRUCT")) {
            return true;
        }
        
        // 其他类型不需要长度
        return true;
    }

    @Override
    public String getSchema() {
        return dataSource.getDbName();
    }

    @Override
    public String getDefaultSchema() {
        return "default";  // Hive 默认数据库是 default
    }

    @Override
    public void setSchema(Connection conn, String schema) throws SQLException {
        if (schema != null && !schema.isEmpty()) {
            conn.setSchema(schema);
        }
    }

    @Override
    public String getTableExistsSql(String tableName) {
        // Hive 使用 DESCRIBE 来检查表是否存在
        return "DESCRIBE " + wrapIdentifier(tableName);
    }

    @Override
    public String getDropTableSql(String tableName) {
        return "DROP TABLE IF EXISTS " + wrapIdentifier(tableName);
    }

    @Override
    public String getTruncateTableSql(String tableName) {
        // Hive 使用 TRUNCATE TABLE 语法
        return "TRUNCATE TABLE " + wrapIdentifier(tableName);
    }

    @Override
    public String getRenameTableSql(String oldTableName, String newTableName) {
        // Hive 使用 ALTER TABLE RENAME TO 语法
        return "ALTER TABLE " + wrapIdentifier(oldTableName) + " RENAME TO " + wrapIdentifier(newTableName);
    }

    @Override
    public String getShowCreateTableSql(String tableName) {
        return "SHOW CREATE TABLE " + wrapIdentifier(tableName);
    }

    @Override
    public String formatFieldDefinition(String name, String type, Integer length, Integer precision, Integer scale, boolean nullable, String defaultValue, String comment) {
        StringBuilder sb = new StringBuilder();
        sb.append(wrapIdentifier(name)).append(" ");
        
        // 处理数据类型
        if (type.equalsIgnoreCase("VARCHAR") && length != null) {
            sb.append("VARCHAR(").append(length).append(")");
        } else if (type.equalsIgnoreCase("CHAR") && length != null) {
            sb.append("CHAR(").append(length).append(")");
        } else if (type.equalsIgnoreCase("DECIMAL") && precision != null) {
            sb.append("DECIMAL");
            if (precision != null) {
                sb.append("(").append(precision);
                if (scale != null) {
                    sb.append(",").append(scale);
                }
                sb.append(")");
            }
        } else {
            sb.append(type.toUpperCase());
        }
        
        // Hive 不支持 NOT NULL 约束
        // Hive 不支持默认值设置
        
        // 添加注释
        if (comment != null && !comment.isEmpty()) {
            sb.append(" COMMENT ").append(wrapValue(comment));
        }

        return sb.toString();
    }

    @Override
    public String getCreateTableSql(String tableName, List<ColumnDefinition> columns, 
                                  String tableComment, String engine, String charset, String collate) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ");
        sql.append(wrapIdentifier(tableName));
        sql.append(" (\n");
        
        List<String> columnDefs = new ArrayList<>();
        for (ColumnDefinition column : columns) {
            columnDefs.add("  " + formatFieldDefinition(
                column.getName(), column.getType(), column.getLength(),
                column.getPrecision(), column.getScale(), column.isNullable(),
                column.getDefaultValue(), column.getComment()
            ));
        }
        
        sql.append(String.join(",\n", columnDefs));
        sql.append("\n)");
        
        // Hive 不支持 charset 和 collate 参数，但支持 COMMENT
        if (tableComment != null && !tableComment.isEmpty()) {
            sql.append(" COMMENT ").append(wrapValue(tableComment));
        }
        
        // 在 Hive 中，engine 参数用于指定存储格式
        if (engine != null && !engine.isEmpty()) {
            sql.append(" STORED AS ").append(engine);
        } else {
            // 默认使用 TEXTFILE 格式
            sql.append(" STORED AS TEXTFILE");
        }
        
        return sql.toString();
    }

    @Override
    public Map<String, String> getTypeMapping() {
        Map<String, String> typeMapping = new HashMap<>();
        
        // 基本类型映射
        typeMapping.put("VARCHAR", "VARCHAR");
        typeMapping.put("CHAR", "CHAR");
        typeMapping.put("TEXT", "STRING");
        typeMapping.put("LONGTEXT", "STRING");
        typeMapping.put("MEDIUMTEXT", "STRING");
        typeMapping.put("TINYTEXT", "STRING");
        typeMapping.put("INT", "INT");
        typeMapping.put("INTEGER", "INT");
        typeMapping.put("SMALLINT", "SMALLINT");
        typeMapping.put("TINYINT", "TINYINT");
        typeMapping.put("MEDIUMINT", "INT");
        typeMapping.put("BIGINT", "BIGINT");
        typeMapping.put("DECIMAL", "DECIMAL");
        typeMapping.put("NUMERIC", "DECIMAL");
        typeMapping.put("FLOAT", "FLOAT");
        typeMapping.put("DOUBLE", "DOUBLE");
        typeMapping.put("REAL", "DOUBLE");
        typeMapping.put("DATE", "DATE");
        typeMapping.put("TIME", "STRING");
        typeMapping.put("DATETIME", "TIMESTAMP");
        typeMapping.put("TIMESTAMP", "TIMESTAMP");
        typeMapping.put("YEAR", "INT");
        typeMapping.put("BOOLEAN", "BOOLEAN");
        typeMapping.put("BIT", "BOOLEAN");
        typeMapping.put("BLOB", "BINARY");
        typeMapping.put("LONGBLOB", "BINARY");
        typeMapping.put("MEDIUMBLOB", "BINARY");
        typeMapping.put("TINYBLOB", "BINARY");
        typeMapping.put("BINARY", "BINARY");
        typeMapping.put("VARBINARY", "BINARY");
        
        // Hive 不支持的类型，转为适当的替代类型
        typeMapping.put("ENUM", "STRING");
        typeMapping.put("SET", "STRING");
        typeMapping.put("JSON", "STRING");
        typeMapping.put("GEOMETRY", "STRING");
        typeMapping.put("POINT", "STRING");
        typeMapping.put("LINESTRING", "STRING");
        typeMapping.put("POLYGON", "STRING");
        typeMapping.put("MULTIPOINT", "STRING");
        typeMapping.put("MULTILINESTRING", "STRING");
        typeMapping.put("MULTIPOLYGON", "STRING");
        typeMapping.put("GEOMETRYCOLLECTION", "STRING");
        
        // Oracle 特有类型
        typeMapping.put("NUMBER", "DECIMAL");
        typeMapping.put("RAW", "BINARY");
        typeMapping.put("LONG RAW", "BINARY");
        typeMapping.put("CLOB", "STRING");
        typeMapping.put("NCLOB", "STRING");
        typeMapping.put("NVARCHAR2", "VARCHAR");
        typeMapping.put("NCHAR", "CHAR");
        
        // SQL Server 特有类型
        typeMapping.put("UNIQUEIDENTIFIER", "STRING");
        typeMapping.put("NTEXT", "STRING");
        typeMapping.put("IMAGE", "BINARY");
        typeMapping.put("MONEY", "DECIMAL");
        typeMapping.put("SMALLMONEY", "DECIMAL");
        typeMapping.put("DATETIME2", "TIMESTAMP");
        typeMapping.put("DATETIMEOFFSET", "STRING");
        typeMapping.put("SMALLDATETIME", "TIMESTAMP");
        typeMapping.put("XML", "STRING");
        
        return typeMapping;
    }
    

    @Override
    public Map<String, Map<String, String>> getCommonTypeMapping() {
        return COMMON_TYPE_MAPPING;
    }

    @Override
    public String wrapValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        
        if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof java.sql.Date) {
            // Hive日期格式：yyyy-MM-dd
            return "'" + value + "'";
        } else if (value instanceof Time) {
            // Hive不直接支持TIME类型，使用字符串
            return "'" + value + "'";
        } else if (value instanceof Timestamp) {
            // Hive时间戳格式：yyyy-MM-dd HH:mm:ss.SSS
            return "'" + value + "'";
        } else if (value instanceof byte[]) {
            // 二进制数据转为十六进制字符串
            byte[] bytes = (byte[]) value;
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02X", b));
            }
            return "'" + hex.toString() + "'";
        } else {
            // 字符串需要转义单引号
            String stringValue = value.toString().replace("'", "\\'");
            return "'" + stringValue + "'";
        }
    }

    @Override
    public String wrapIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
        return "`" + identifier.replace("`", "``") + "`";
    }

    @Override
    public String getDefaultTextType() {
        return "STRING";
    }

    @Override
    public String getDefaultIntegerType() {
        return "INT";
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
        return "TIMESTAMP";
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
    public String mapJavaTypeToDbType(Class<?> javaType) {
        if (String.class.equals(javaType)) {
            return "STRING";
        }
        if (Integer.class.equals(javaType) || int.class.equals(javaType)) {
            return "INT";
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
            return "DECIMAL(20,4)";
        }
        if (Boolean.class.equals(javaType) || boolean.class.equals(javaType)) {
            return "BOOLEAN";
        }
        if (Date.class.equals(javaType)) {
            return "TIMESTAMP";
        }
        if (byte[].class.equals(javaType)) {
            return "BINARY";
        }
        return "STRING";
    }

    @Override
    public Class<?> mapDbTypeToJavaType(String dbType) {
        dbType = dbType.toUpperCase();
        if (dbType.equals("STRING")) {
            return String.class;
        }
        if (dbType.equals("INT")) {
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
        if (dbType.equals("TIMESTAMP")) {
            return Timestamp.class;
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
    public String getAddColumnSql(String tableName, String columnDefinition) {
        return "ALTER TABLE " + wrapIdentifier(tableName) + " ADD COLUMNS (" + columnDefinition + ")";
    }

    @Override
    public String getAddColumnSql(String tableName, ColumnDefinition column, String afterColumn) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ");
        sql.append(wrapIdentifier(tableName));
        sql.append(" ADD COLUMNS (");
        sql.append(formatFieldDefinition(
            column.getName(), column.getType(), column.getLength(),
            column.getPrecision(), column.getScale(), column.isNullable(),
            column.getDefaultValue(), column.getComment()
        ));
        sql.append(")");
        
        // Hive 不支持 AFTER 子句来指定列的位置
        
        return sql.toString();
    }

    @Override
    public String getModifyColumnSql(String tableName, String columnName, String newDefinition) {
        // Hive 使用 CHANGE 而不是 MODIFY
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ");
        sql.append(wrapIdentifier(tableName));
        sql.append(" CHANGE COLUMN ");
        sql.append(wrapIdentifier(columnName));
        sql.append(" ");
        sql.append(newDefinition);
        
        return sql.toString();
    }

    @Override
    public String getDropColumnSql(String tableName, String columnName) {
        // 当前实现有问题，Hive 无法通过 ALTER TABLE REPLACE COLUMNS 直接删除特定列
        // 正确的方法是获取当前所有列，然后重建表结构，跳过要删除的列
        return "-- Hive 不支持直接删除列。需要使用 ALTER TABLE " + wrapIdentifier(tableName) +
               " REPLACE COLUMNS 命令重新指定所有需要保留的列";
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
    public String convertFromOtherDbType(String sourceType, String sourceDbType) {
        sourceType = sourceType.toUpperCase().trim();
        sourceDbType = sourceDbType.toUpperCase().trim();

        // MySQL类型转换
        if ("MYSQL".equals(sourceDbType)) {
            switch (sourceType) {
                case "TINYINT":
                    return "TINYINT";
                case "SMALLINT":
                    return "SMALLINT";
                case "MEDIUMINT":
                case "INT":
                    return "INT";
                case "BIGINT":
                    return "BIGINT";
                case "FLOAT":
                    return "FLOAT";
                case "DOUBLE":
                    return "DOUBLE";
                case "DECIMAL":
                case "NUMERIC":
                    return "DECIMAL";
                case "CHAR":
                case "VARCHAR":
                case "TINYTEXT":
                case "TEXT":
                case "MEDIUMTEXT":
                case "LONGTEXT":
                    return "STRING";
                case "TINYBLOB":
                case "BLOB":
                case "MEDIUMBLOB":
                case "LONGBLOB":
                    return "BINARY";
                case "DATE":
                    return "DATE";
                case "TIME":
                case "TIMESTAMP":
                case "DATETIME":
                    return "TIMESTAMP";
                case "BOOLEAN":
                case "BOOL":
                    return "BOOLEAN";
                default:
                    return "STRING";
            }
        }
        
        // Oracle类型转换
        if ("ORACLE".equals(sourceDbType)) {
            switch (sourceType) {
                case "NUMBER":
                    return "DECIMAL";
                case "VARCHAR2":
                case "NVARCHAR2":
                case "CHAR":
                case "NCHAR":
                case "CLOB":
                case "NCLOB":
                    return "STRING";
                case "BLOB":
                case "RAW":
                case "LONG RAW":
                    return "BINARY";
                case "DATE":
                    return "DATE";
                default:
                    return "STRING";
            }
        }

        // SQL Server类型转换
        if ("SQLSERVER".equals(sourceDbType)) {
            switch (sourceType) {
                case "TINYINT":
                    return "TINYINT";
                case "SMALLINT":
                    return "SMALLINT";
                case "INT":
                    return "INT";
                case "BIGINT":
                    return "BIGINT";
                case "REAL":
                    return "FLOAT";
                case "FLOAT":
                    return "DOUBLE";
                case "DECIMAL":
                case "NUMERIC":
                case "MONEY":
                case "SMALLMONEY":
                    return "DECIMAL";
                case "CHAR":
                case "VARCHAR":
                case "NCHAR":
                case "NVARCHAR":
                case "TEXT":
                case "NTEXT":
                    return "STRING";
                case "IMAGE":
                case "BINARY":
                case "VARBINARY":
                    return "BINARY";
                case "DATE":
                    return "DATE";
                case "TIME":
                case "DATETIME":
                case "DATETIME2":
                case "SMALLDATETIME":
                    return "TIMESTAMP";
                case "BIT":
                    return "BOOLEAN";
                default:
                    return "STRING";
            }
        }

        // 默认返回STRING
        return "STRING";
    }

    @Override
    public Map<String, Integer> getDefaultLengthMapping() {
        Map<String, Integer> lengthMapping = new HashMap<>();
        lengthMapping.put("VARCHAR", DEFAULT_VARCHAR_LENGTH);
        lengthMapping.put("CHAR", 1);
        lengthMapping.put("DECIMAL", 10);
        lengthMapping.put("STRING", DEFAULT_VARCHAR_LENGTH);
        lengthMapping.put("INT", 11);
        lengthMapping.put("BIGINT", 20);
        return lengthMapping;
    }

    @Override
    public String generateCreateTableSql(TableDefinition tableDefinition) {
        StringBuilder sql = new StringBuilder();
        
        // 获取基本的创建表 SQL
        sql.append(getCreateTableSql(
            tableDefinition.getTableName(),
            tableDefinition.getColumns(),
            tableDefinition.getTableComment(),
            tableDefinition.getEngine(),
            tableDefinition.getCharset(),
            tableDefinition.getCollate()
        ));
        
        // 处理分区信息
        Map<String, String> properties = tableDefinition.getExtraProperties();
        if (properties != null && properties.containsKey("PARTITIONED_BY")) {
            String partitionedBy = properties.get("PARTITIONED_BY");
            sql.append("\nPARTITIONED BY (").append(partitionedBy).append(")");
        }
        
        // 处理分桶信息
        if (properties != null && properties.containsKey("CLUSTERED_BY") 
                && properties.containsKey("NUM_BUCKETS")) {
            String clusteredBy = properties.get("CLUSTERED_BY");
            String numBuckets = properties.get("NUM_BUCKETS");
            sql.append("\nCLUSTERED BY (").append(clusteredBy).append(")");
            
            // 处理排序列
            if (properties.containsKey("SORTED_BY")) {
                String sortedBy = properties.get("SORTED_BY");
                sql.append(" SORTED BY (").append(sortedBy).append(")");
            }
            
                    sql.append(" INTO ").append(numBuckets).append(" BUCKETS");
            }

            // 处理行格式
        if (properties != null && properties.containsKey("ROW_FORMAT")) {
            String rowFormat = properties.get("ROW_FORMAT");
                sql.append("\nROW FORMAT ").append(rowFormat);
            }

            // 处理存储格式
        if (properties != null && properties.containsKey("STORED_AS")) {
            String storedAs = properties.get("STORED_AS");
                sql.append("\nSTORED AS ").append(storedAs);
        } else if (tableDefinition.getEngine() != null && !tableDefinition.getEngine().isEmpty()) {
            sql.append("\nSTORED AS ").append(tableDefinition.getEngine());
        } else {
            sql.append("\nSTORED AS TEXTFILE");
            }

            // 处理表属性
        if (properties != null && properties.containsKey("TBLPROPERTIES")) {
            String tblProperties = properties.get("TBLPROPERTIES");
            sql.append("\nTBLPROPERTIES (").append(tblProperties).append(")");
        }

        return sql.toString();
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
                
                // 调整Hive特定的列类型
                adjustHiveColumnType(column);
            }
            
            // 生成Hive建表语句
            return generateCreateTableSql(tableDefinition);
        } catch (Exception e) {
            log.error("Failed to convert CREATE TABLE SQL from {} to Hive: {}", sourceDbType, e.getMessage());
            throw new RuntimeException("Failed to convert CREATE TABLE SQL", e);
        }
    }

    private void adjustHiveColumnType(ColumnDefinition column) {
        String type = column.getType().toUpperCase();
        
        // 处理 NVARCHAR/NCHAR，Hive 不支持这些类型
        if (type.equals("NVARCHAR")) {
            column.setType("VARCHAR");
        } else if (type.equals("NCHAR")) {
            column.setType("CHAR");
        }
        
        // Hive 的 VARCHAR 最大长度为 65535
        if (type.equals("VARCHAR") && column.getLength() != null && column.getLength() > MAX_VARCHAR_LENGTH) {
            column.setLength(MAX_VARCHAR_LENGTH);
        }
        
        // Hive 的 CHAR 最大长度为 255
        if (type.equals("CHAR") && column.getLength() != null && column.getLength() > 255) {
            column.setLength(255);
        }
        
        // 以下类型在 Hive 中不需要长度
        if (type.equals("STRING") || type.equals("INT") || type.equals("BIGINT") || 
            type.equals("FLOAT") || type.equals("DOUBLE") || type.equals("BOOLEAN") ||
            type.equals("DATE") || type.equals("TIMESTAMP") || type.equals("BINARY")) {
                column.setLength(null);
        }
        
        // Hive 不支持自增列，移除自增属性
        if (column.getDefaultValue() != null && 
            column.getDefaultValue().toUpperCase().contains("AUTO_INCREMENT")) {
            column.setDefaultValue(null);
        }
        
        // Hive 不支持默认值约束
        column.setDefaultValue(null);
    }

    @Override
    public TableDefinition parseCreateTableSql(String createTableSql) {
        TableDefinition tableDefinition = new TableDefinition();
        List<ColumnDefinition> columns = new ArrayList<>();

        try {
            // 提取表名
            Pattern tablePattern = Pattern.compile("CREATE\\s+(?:EXTERNAL\\s+)?TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([\\w\\.`]+)\\s*\\(", 
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
            String[] columnDefs = columnsPart.split(",(?=(?:[^']*'[^']*')*[^']*$)");

            // 解析每个列定义
            for (String columnDef : columnDefs) {
                columnDef = columnDef.trim();
                if (columnDef.isEmpty()) continue;

                // 解析列定义
                Pattern columnPattern = Pattern.compile(
                    "`?([\\w]+)`?\\s+" +                // 列名
                    "([\\w]+)" +                        // 数据类型
                    "(?:\\s*\\([^)]+\\))?" +           // 可选的长度/精度
                    "(?:\\s+COMMENT\\s+'([^']*)')?"    // 可选的注释
                );

                Matcher matcher = columnPattern.matcher(columnDef);
                if (matcher.find()) {
                    ColumnDefinition column = new ColumnDefinition();
                    column.setName(matcher.group(1));
                    column.setType(matcher.group(2).toUpperCase());
                    
                    // 设置注释
                    if (matcher.group(3) != null) {
                        column.setComment(matcher.group(3));
                    }

                    // Hive列总是可空的
                    column.setNullable(true);
                    
                    columns.add(column);
                }
            }

            tableDefinition.setColumns(columns);

            // 解析表属性
            Map<String, String> extraProps = new HashMap<>();
            
            // 解析是否为外部表
            if (createTableSql.toUpperCase().contains("EXTERNAL TABLE")) {
                extraProps.put("EXTERNAL", "true");
            }
            
            // 解析存储格式
            Pattern storagePattern = Pattern.compile("STORED\\s+AS\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
            Matcher storageMatcher = storagePattern.matcher(createTableSql);
            if (storageMatcher.find()) {
                extraProps.put("STORED_AS", storageMatcher.group(1));
            }
            
            // 解析位置
            Pattern locationPattern = Pattern.compile("LOCATION\\s+'([^']+)'", Pattern.CASE_INSENSITIVE);
            Matcher locationMatcher = locationPattern.matcher(createTableSql);
            if (locationMatcher.find()) {
                extraProps.put("LOCATION", locationMatcher.group(1));
            }
            
            // 解析表注释
            Pattern commentPattern = Pattern.compile("COMMENT\\s+'([^']+)'", Pattern.CASE_INSENSITIVE);
            Matcher commentMatcher = commentPattern.matcher(createTableSql);
            if (commentMatcher.find()) {
                tableDefinition.setTableComment(commentMatcher.group(1));
            }
            
            tableDefinition.setExtraProperties(extraProps);

            return tableDefinition;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CREATE TABLE SQL: " + e.getMessage(), e);
        }
    }

    @Override
    public List<TableDefinition> getAllTables(String database) throws Exception {
        List<TableDefinition> tables = new ArrayList<>();
        String sql = "SHOW TABLES";
        
        if (database != null && !database.isEmpty()) {
            sql += " IN " + wrapIdentifier(database);
        }

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String tableName = rs.getString(1);
                TableDefinition table = TableDefinition.builder()
                    .tableName(tableName)
                    .build();
                
                // Get table comment if available
                try (PreparedStatement descStmt = conn.prepareStatement("DESCRIBE FORMATTED " + wrapIdentifier(tableName))) {
                    ResultSet descRs = descStmt.executeQuery();
                    while (descRs.next()) {
                        String colName = descRs.getString(1);
                        if (colName != null && colName.trim().equalsIgnoreCase("Comment:")) {
                            String comment = descRs.getString(2);
                            if (comment != null) {
                                table.setTableComment(comment.trim());
                            }
                            break;
                        }
                    }
                } catch (Exception e) {
                    // Ignore errors when getting table comment
                }
                
                tables.add(table);
            }
        }
        
        return tables;
    }

    @Override
    public List<ColumnDefinition> getTableColumns(String database, String tableName) throws Exception {
        List<ColumnDefinition> columns = new ArrayList<>();
        String sql = "DESCRIBE FORMATTED " + (database != null ? wrapIdentifier(database) + "." : "") + wrapIdentifier(tableName);

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            boolean inDetailedView = false;
            while (rs.next()) {
                String colName = rs.getString(1);
                if (colName == null) continue;
                
                colName = colName.trim();
                
                // Skip header rows and empty rows
                if (colName.isEmpty() || colName.startsWith("#")) {
                    continue;
                }
                
                // Check if we're in the detailed column section
                if (colName.equals("# Detailed Table Information")) {
                    inDetailedView = true;
                    continue;
                }
                
                // Process only if we haven't reached the detailed view section
                if (!inDetailedView && !colName.startsWith("#")) {
                    // Get column name
                    String columnName = colName;
                    
                    // Get column type and parse information
                    String dataType = rs.getString(2);
                    String type = dataType;
                    Integer length = null;
                    Integer precision = null;
                    Integer scale = null;
                    
                    if (dataType != null) {
                        dataType = dataType.trim();
                        // Extract type and length/precision if available
                        if (dataType.contains("(")) {
                            String[] parts = dataType.split("[()]");
                            type = parts[0];
                            if (parts.length > 1) {
                                if (parts[1].contains(",")) {
                                    String[] precisionScale = parts[1].split(",");
                                    precision = Integer.parseInt(precisionScale[0].trim());
                                    scale = Integer.parseInt(precisionScale[1].trim());
                                } else {
                                    length = Integer.parseInt(parts[1].trim());
                                }
                            }
                        }
                    }
                    
                    // Get column comment
                    String comment = rs.getString(3);
                    if (comment == null || comment.trim().isEmpty()) {
                        comment = null;
                    } else {
                        comment = comment.trim();
                    }
                    
                    // Create column definition
                    ColumnDefinition column = ColumnDefinition.builder()
                        .name(columnName)
                        .type(type)
                        .length(length)
                        .precision(precision)
                        .scale(scale)
                        .nullable(true) // Hive columns are nullable by default
                        .comment(comment)
                        .build();
                    
                    columns.add(column);
                }
            }
        }
        
        return columns;
    }

    @Override
    public Map<String, Object> getColumnDistribution(String tableName, String columnName) throws Exception {
        Map<String, Object> distribution = new HashMap<>();
        try (Connection conn = getConnection()) {
            String dataType = null;
            
            // 获取字段类型
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                    "DESCRIBE " + wrapIdentifier(tableName) + " " + wrapIdentifier(columnName))) {
                if (rs.next()) {
                    dataType = rs.getString("data_type").toUpperCase();
                }
            }
            
            if (dataType == null) {
                throw new Exception("Column not found: " + columnName);
            }

            StringBuilder sql = new StringBuilder();
            sql.append("SELECT COUNT(*) as total_count, ");
            sql.append("COUNT(DISTINCT ").append(wrapIdentifier(columnName)).append(") as unique_count, ");
            sql.append("COUNT(").append(wrapIdentifier(columnName)).append(") as non_null_count, ");
            
            // 数值类型的统计
            if (dataType.matches("TINYINT|SMALLINT|INT|BIGINT|FLOAT|DOUBLE|DECIMAL")) {
                sql.append("MIN(").append(wrapIdentifier(columnName)).append(") as min_value, ");
                sql.append("MAX(").append(wrapIdentifier(columnName)).append(") as max_value, ");
                sql.append("AVG(").append(wrapIdentifier(columnName)).append(") as avg_value, ");
                sql.append("STDDEV_POP(").append(wrapIdentifier(columnName)).append(") as std_dev, ");
                sql.append("percentile(").append(wrapIdentifier(columnName)).append(", 0.5) as median ");
            }
            // 字符串类型的统计
            else if (dataType.matches("STRING|VARCHAR|CHAR")) {
                sql.append("MIN(LENGTH(").append(wrapIdentifier(columnName)).append(")) as min_length, ");
                sql.append("MAX(LENGTH(").append(wrapIdentifier(columnName)).append(")) as max_length, ");
                sql.append("AVG(LENGTH(").append(wrapIdentifier(columnName)).append(")) as avg_length ");
            }
            // 日期时间类型的统计
            else if (dataType.matches("TIMESTAMP|DATE")) {
                sql.append("MIN(").append(wrapIdentifier(columnName)).append(") as earliest_date, ");
                sql.append("MAX(").append(wrapIdentifier(columnName)).append(") as latest_date ");
            }
            
            sql.append("FROM ").append(wrapIdentifier(tableName));
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql.toString())) {
                if (rs.next()) {
                    distribution.put("totalCount", rs.getLong("total_count"));
                    distribution.put("uniqueCount", rs.getLong("unique_count"));
                    distribution.put("nonNullCount", rs.getLong("non_null_count"));
                    distribution.put("nullCount", rs.getLong("total_count") - rs.getLong("non_null_count"));
                    distribution.put("uniqueRatio", (double)rs.getLong("unique_count") / rs.getLong("total_count"));
                    distribution.put("nullRatio", (double)(rs.getLong("total_count") - rs.getLong("non_null_count")) / rs.getLong("total_count"));
                    
                    if (dataType.matches("TINYINT|SMALLINT|INT|BIGINT|FLOAT|DOUBLE|DECIMAL")) {
                        distribution.put("minValue", rs.getObject("min_value"));
                        distribution.put("maxValue", rs.getObject("max_value"));
                        distribution.put("avgValue", rs.getDouble("avg_value"));
                        distribution.put("stdDev", rs.getDouble("std_dev"));
                        distribution.put("median", rs.getDouble("median"));
                    }
                    else if (dataType.matches("STRING|VARCHAR|CHAR")) {
                        distribution.put("minLength", rs.getInt("min_length"));
                        distribution.put("maxLength", rs.getInt("max_length"));
                        distribution.put("avgLength", rs.getDouble("avg_length"));
                    }
                    else if (dataType.matches("TIMESTAMP|DATE")) {
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
            log.error("获取字段分布信息失败: table={}, column={}, error={}", 
                     tableName, columnName, e.getMessage(), e);
            throw e;
        }
        return distribution;
    }

    @Override
    public String getDatabaseType() {
        return "Hive";
    }

    @Override
    public String getDatabaseName() {
        try (Connection conn = getConnection()) {
            return conn.getCatalog();
        } catch (Exception e) {
            log.error("获取数据库名称失败: error={}", e.getMessage(), e);
            return null;
        }
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
                 ResultSet rs = stmt.executeQuery("DESCRIBE FORMATTED " + wrapIdentifier(tableName))) {
                boolean inDetailedInfo = false;
                while (rs.next()) {
                    String col1 = rs.getString(1);
                    if (col1 != null) {
                        col1 = col1.trim();
                        if (col1.equals("# Detailed Table Information")) {
                            inDetailedInfo = true;
                            continue;
                        }
                        
                        if (inDetailedInfo && col1.length() > 0 && !col1.startsWith("#")) {
                            String key = col1;
                            String value = rs.getString(2);
                            if (value != null) {
                                value = value.trim();
                                tableInfo.put(key, value);
                            }
                        }
                    }
                }
            }
            
            // 获取表行数
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(1) FROM " + wrapIdentifier(tableName))) {
                if (rs.next()) {
                    tableInfo.put("rowCount", rs.getLong(1));
                }
            } catch (Exception e) {
                log.warn("获取表行数失败: table={}, error={}", tableName, e.getMessage());
                tableInfo.put("rowCount", -1L);
            }
            
            // 获取表列数
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("DESCRIBE " + wrapIdentifier(tableName))) {
                int columnCount = 0;
                while (rs.next()) {
                    String colName = rs.getString(1);
                    if (colName != null && !colName.trim().isEmpty() && !colName.trim().startsWith("#")) {
                        columnCount++;
                    }
                }
                tableInfo.put("columnCount", columnCount);
            }
            
            tableInfo.put("tableName", tableName);
            
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
             ResultSet rs = stmt.executeQuery("DESCRIBE " + wrapIdentifier(tableName))) {
            int position = 1;
            while (rs.next()) {
                String colName = rs.getString(1);
                if (colName != null && !colName.trim().isEmpty() && !colName.trim().startsWith("#")) {
                    Map<String, Object> column = new HashMap<>();
                    column.put("columnName", colName.trim());
                    column.put("dataType", rs.getString(2));
                    column.put("comment", rs.getString(3));
                    column.put("position", position++);
                    column.put("nullable", true); // Hive默认允许NULL
                    columns.add(column);
                }
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
             ResultSet rs = stmt.executeQuery("DESCRIBE " + wrapIdentifier(tableName) + " " + wrapIdentifier(columnName))) {
            if (rs.next()) {
                columnInfo = new HashMap<>();
                columnInfo.put("columnName", columnName);
                columnInfo.put("dataType", rs.getString(2));
                columnInfo.put("comment", rs.getString(3));
                columnInfo.put("nullable", true); // Hive默认允许NULL
                
                // 解析数据类型以获取长度、精度等信息
                String dataType = rs.getString(2);
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
             ResultSet rs = stmt.executeQuery("DESCRIBE FORMATTED " + wrapIdentifier(tableName))) {
            while (rs.next()) {
                String col1 = rs.getString(1);
                if (col1 != null && col1.trim().equals("InputFormat:")) {
                    String inputFormat = rs.getString(2);
                    if (inputFormat != null) {
                        if (inputFormat.contains("TextInputFormat")) {
                            return "TEXTFILE";
                        } else if (inputFormat.contains("SequenceFileInputFormat")) {
                            return "SEQUENCEFILE";
                        } else if (inputFormat.contains("RCFileInputFormat")) {
                            return "RCFILE";
                        } else if (inputFormat.contains("OrcInputFormat")) {
                            return "ORC";
                        } else if (inputFormat.contains("ParquetInputFormat")) {
                            return "PARQUET";
                        } else if (inputFormat.contains("AvroInputFormat")) {
                            return "AVRO";
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取表引擎失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return "UNKNOWN";
    }

    @Override
    public String getTableCharset(String tableName) throws Exception {
        // Hive不直接支持字符集设置，返回默认值
        return "UTF-8";
    }

    @Override
    public String getTableCollation(String tableName) throws Exception {
        // Hive不直接支持排序规则设置，返回默认值
        return "binary";
    }

    @Override
    public Long getTableSize(String tableName) throws Exception {
        // Hive 需要通过 DESCRIBE FORMATTED 命令获取表大小
        String sql = "DESCRIBE FORMATTED " + wrapIdentifier(tableName);
        Long size = null;
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
        
            while (rs.next()) {
                String col1 = rs.getString(1);
                if (col1 != null && col1.trim().equals("totalSize")) {
                    String sizeStr = rs.getString(2);
                    if (sizeStr != null && !sizeStr.trim().isEmpty()) {
                        try {
                            // 通常 totalSize 会带单位，例如 "1.2 GB"
                            // 需要解析字符串获取实际大小
                            String[] parts = sizeStr.trim().split("\\s+");
                            if (parts.length >= 1) {
                                double value = Double.parseDouble(parts[0]);
                                long multiplier = 1;
                                
                                if (parts.length > 1) {
                                    String unit = parts[1].toUpperCase();
                                    if (unit.equals("KB")) {
                                        multiplier = 1024L;
                                    } else if (unit.equals("MB")) {
                                        multiplier = 1024L * 1024L;
                                    } else if (unit.equals("GB")) {
                                        multiplier = 1024L * 1024L * 1024L;
                                    } else if (unit.equals("TB")) {
                                        multiplier = 1024L * 1024L * 1024L * 1024L;
                                    }
                                }
                                
                                size = (long) (value * multiplier);
                            }
                        } catch (NumberFormatException e) {
                            log.warn("无法解析表大小: {}", sizeStr);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取表大小失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        
        return size;
    }

    @Override
    public Long getTableRowCount(String tableName) throws Exception {
        // Hive 可以通过 DESCRIBE FORMATTED 命令获取行数，但不总是准确
        // 需要先执行 ANALYZE TABLE 命令收集统计信息
        String analyzeSql = "ANALYZE TABLE " + wrapIdentifier(tableName) + " COMPUTE STATISTICS";
        String countSql = "DESCRIBE FORMATTED " + wrapIdentifier(tableName);
        Long rowCount = null;
        
        try (Connection conn = getConnection()) {
            // 收集统计信息
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(analyzeSql);
            } catch (SQLException e) {
                log.warn("收集表统计信息失败: table={}, error={}", tableName, e.getMessage());
                // 忽略错误，继续尝试获取行数
            }
            
            // 获取行数
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(countSql)) {
                
                while (rs.next()) {
                    String col1 = rs.getString(1);
                    if (col1 != null && col1.trim().equals("numRows")) {
                        String rowCountStr = rs.getString(2);
                        if (rowCountStr != null && !rowCountStr.trim().isEmpty()) {
                            try {
                                rowCount = Long.parseLong(rowCountStr.trim());
                            } catch (NumberFormatException e) {
                                log.warn("无法解析表行数: {}", rowCountStr);
                            }
                        }
                    }
                }
            }
            
            // 如果无法通过 DESCRIBE FORMATTED 获取行数，使用 COUNT(*) 查询
            if (rowCount == null) {
                String sqlCount = "SELECT COUNT(*) FROM " + wrapIdentifier(tableName);
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sqlCount)) {
                    
            if (rs.next()) {
                        rowCount = rs.getLong(1);
                    }
                } catch (SQLException e) {
                    log.warn("通过 COUNT(*) 获取表行数失败: table={}, error={}", tableName, e.getMessage());
                    // 无法获取行数，返回 null
                }
            }
        } catch (Exception e) {
            log.error("获取表行数失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        
        return rowCount;
    }

    @Override
    public String getTableSpace(String tableName) throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("DESCRIBE FORMATTED " + wrapIdentifier(tableName))) {
            while (rs.next()) {
                String col1 = rs.getString(1);
                if (col1 != null && col1.trim().equals("Location:")) {
                    return rs.getString(2);
                }
            }
        } catch (Exception e) {
            log.error("获取表空间信息失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return null;
    }

    @Override
    public Integer getCharacterLength(String tableName, String columnName) throws Exception {
        // 获取列的数据类型
        String dataType = null;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("DESCRIBE " + wrapIdentifier(tableName) + " " + wrapIdentifier(columnName))) {
        
            if (rs.next()) {
                dataType = rs.getString(2);
            }
        }
        
        if (dataType == null) {
            throw new SQLException("Column not found: " + columnName);
        }
        
        // 解析类型定义中的长度
        // 例如：VARCHAR(255) -> 255
        if (dataType.toUpperCase().startsWith("VARCHAR") || dataType.toUpperCase().startsWith("CHAR")) {
            Pattern pattern = Pattern.compile("\\((\\d+)\\)");
            Matcher matcher = pattern.matcher(dataType);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        }
        
        // 对于 STRING 类型，Hive 不限定长度，返回默认值
        if (dataType.toUpperCase().equals("STRING")) {
            return DEFAULT_VARCHAR_LENGTH;
        }
        
        return null; // 非字符类型列返回 null
    }

    @Override
    public Integer getNumericPrecision(String tableName, String columnName) throws Exception {
        // 获取列的数据类型
        String dataType = null;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("DESCRIBE " + wrapIdentifier(tableName) + " " + wrapIdentifier(columnName))) {
        
            if (rs.next()) {
                dataType = rs.getString(2);
            }
        }
        
        if (dataType == null) {
            throw new SQLException("Column not found: " + columnName);
        }
        
        // 解析 DECIMAL 类型的精度
        // 例如：DECIMAL(10,2) -> 10
        if (dataType.toUpperCase().startsWith("DECIMAL")) {
            Pattern pattern = Pattern.compile("DECIMAL\\((\\d+)(?:,(\\d+))?\\)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(dataType);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        }
        
        // 对于其他数值类型，返回固定精度
        if (dataType.toUpperCase().equals("TINYINT")) {
            return 3; // -128 to 127
        } else if (dataType.toUpperCase().equals("SMALLINT")) {
            return 5; // -32768 to 32767
        } else if (dataType.toUpperCase().equals("INT")) {
            return 10; // -2147483648 to 2147483647
        } else if (dataType.toUpperCase().equals("BIGINT")) {
            return 19; // -9223372036854775808 to 9223372036854775807
        } else if (dataType.toUpperCase().equals("FLOAT")) {
            return 7; // 32-bit precision
        } else if (dataType.toUpperCase().equals("DOUBLE")) {
            return 15; // 64-bit precision
        }
        
        return null; // 非数值类型列返回 null
    }

    @Override
    public Integer getNumericScale(String tableName, String columnName) throws Exception {
        // 获取列的数据类型
        String dataType = null;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("DESCRIBE " + wrapIdentifier(tableName) + " " + wrapIdentifier(columnName))) {
        
            if (rs.next()) {
                dataType = rs.getString(2);
            }
        }
        
        if (dataType == null) {
            throw new SQLException("Column not found: " + columnName);
        }
        
        // 解析 DECIMAL 类型的小数位数
        // 例如：DECIMAL(10,2) -> 2
        if (dataType.toUpperCase().startsWith("DECIMAL")) {
            Pattern pattern = Pattern.compile("DECIMAL\\((\\d+),(\\d+)\\)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(dataType);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(2));
            }
        }
        
        // 对于整数类型，小数位数为 0
        if (dataType.toUpperCase().equals("TINYINT") ||
            dataType.toUpperCase().equals("SMALLINT") ||
            dataType.toUpperCase().equals("INT") ||
            dataType.toUpperCase().equals("BIGINT")) {
            return 0;
        }
        
        // 对于浮点类型，返回默认小数位数
        if (dataType.toUpperCase().equals("FLOAT")) {
            return 7;
        } else if (dataType.toUpperCase().equals("DOUBLE")) {
            return 15;
        }
        
        return null; // 非数值类型列返回 null
    }

    @Override
    public String getColumnDefault(String tableName, String columnName) throws Exception {
        // Hive不支持列默认值
        return null;
    }

    @Override
    public String getColumnExtra(String tableName, String columnName) throws Exception {
        // Hive不支持额外属性如自增等
        return null;
    }

    @Override
    public Integer getColumnPosition(String tableName, String columnName) throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("DESCRIBE " + wrapIdentifier(tableName))) {
            int position = 1;
            while (rs.next()) {
                String colName = rs.getString(1);
                if (colName != null && !colName.trim().isEmpty() && !colName.trim().startsWith("#")) {
                    if (colName.trim().equalsIgnoreCase(columnName)) {
                        return position;
                    }
                    position++;
                }
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
             ResultSet rs = stmt.executeQuery("DESCRIBE FORMATTED " + wrapIdentifier(tableName))) {
            while (rs.next()) {
                String col1 = rs.getString(1);
                if (col1 != null && col1.trim().equals("CreateTime:")) {
                    String timeStr = rs.getString(2);
                    if (timeStr != null && !timeStr.trim().isEmpty()) {
                        try {
                            return new java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US).parse(timeStr.trim());
                        } catch (Exception e) {
                            log.warn("解析表创建时间失败: table={}, time={}", tableName, timeStr);
                        }
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
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("DESCRIBE FORMATTED " + wrapIdentifier(tableName))) {
            while (rs.next()) {
                String col1 = rs.getString(1);
                if (col1 != null && col1.trim().equals("LastAccessTime:")) {
                    String timeStr = rs.getString(2);
                    if (timeStr != null && !timeStr.trim().isEmpty()) {
                        try {
                            return new java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US).parse(timeStr.trim());
                        } catch (Exception e) {
                            log.warn("解析表更新时间失败: table={}, time={}", tableName, timeStr);
                        }
                    }
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
             ResultSet rs = stmt.executeQuery("DESCRIBE FORMATTED " + wrapIdentifier(tableName))) {
            boolean inConstraintSection = false;
            while (rs.next()) {
                String col1 = rs.getString(1);
                if (col1 != null) {
                    col1 = col1.trim();
                    if (col1.equals("# Constraints")) {
                        inConstraintSection = true;
                        continue;
                    }
                    
                    if (inConstraintSection && col1.contains("Primary key")) {
                        String constraintInfo = rs.getString(2);
                        if (constraintInfo != null && constraintInfo.contains("(")) {
                            int startIndex = constraintInfo.indexOf("(");
                            int endIndex = constraintInfo.indexOf(")");
                            if (startIndex > 0 && endIndex > startIndex) {
                                String columnsStr = constraintInfo.substring(startIndex + 1, endIndex);
                                String[] columns = columnsStr.split(",");
                                for (String column : columns) {
                                    primaryKeys.add(column.trim());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取主键信息失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return primaryKeys;
    }

    @Override
    public List<Map<String, Object>> getForeignKeys(String tableName) throws Exception {
        // Hive 不支持外键约束，返回空列表
        return new ArrayList<>();
    }

    @Override
    public List<Map<String, Object>> getIndexes(String tableName) throws Exception {
        // Hive 3.0 已移除索引功能，直接返回空列表
        return new ArrayList<>();
    }

    @Override
    public Map<String, Object> getTableCompleteness(String tableName) throws Exception {
        Map<String, Object> completeness = new HashMap<>();
        
        try (Connection conn = getConnection()) {
            // 获取表的总行数
            long totalRows = 0;
            String countSql = "SELECT COUNT(*) FROM " + wrapIdentifier(tableName);
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(countSql)) {
                if (rs.next()) {
                    totalRows = rs.getLong(1);
                }
            }
            
            if (totalRows == 0) {
                completeness.put("totalRows", 0);
                completeness.put("completenessRatio", 0.0);
                completeness.put("columnsCompleteness", new HashMap<>());
                return completeness;
            }
            
            completeness.put("totalRows", totalRows);
            
            // 获取表的列信息
            List<Map<String, Object>> columns = listColumns(tableName);
            Map<String, Double> columnsCompleteness = new HashMap<>();
            double totalCompletenessRatio = 0.0;
            
            for (Map<String, Object> column : columns) {
                String columnName = (String) column.get("COLUMN_NAME");
                
                // 计算每列的非空值比例
                String nullCountSql = "SELECT COUNT(*) - COUNT(" + wrapIdentifier(columnName) + ") FROM " + wrapIdentifier(tableName);
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(nullCountSql)) {
                    if (rs.next()) {
                        long nullCount = rs.getLong(1);
                        double completenessRatio = 1.0 - ((double) nullCount / totalRows);
                        columnsCompleteness.put(columnName, completenessRatio);
                        totalCompletenessRatio += completenessRatio;
                    }
                }
            }
            
            // 计算总体完整性比例（所有列的平均值）
            if (!columns.isEmpty()) {
                completeness.put("completenessRatio", totalCompletenessRatio / columns.size());
            } else {
                completeness.put("completenessRatio", 0.0);
            }
            
            completeness.put("columnsCompleteness", columnsCompleteness);
            
        } catch (Exception e) {
            log.error("获取表完整性失败: table={}, error={}", tableName, e.getMessage(), e);
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
            String countSql = "SELECT COUNT(*) FROM " + wrapIdentifier(tableName);
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(countSql)) {
                if (rs.next()) {
                    totalRows = rs.getLong(1);
                }
            }
            
            if (totalRows == 0) {
                accuracy.put("totalRows", 0);
                accuracy.put("accuracyRatio", 0.0);
                accuracy.put("columnsAccuracy", new HashMap<>());
                return accuracy;
            }
            
            accuracy.put("totalRows", totalRows);
            
            // 获取表的列信息
            List<Map<String, Object>> columns = listColumns(tableName);
            Map<String, Double> columnsAccuracy = new HashMap<>();
            double totalAccuracyRatio = 0.0;
            int validatedColumns = 0;
            
            for (Map<String, Object> column : columns) {
                String columnName = (String) column.get("COLUMN_NAME");
                String dataType = (String) column.get("TYPE_NAME");
                
                if (dataType == null) continue;
                
                // 使用适当的方法验证数据准确性
                String validationSql = getValidationSqlForDataType(tableName, columnName, dataType);
                
                if (validationSql != null && !validationSql.equals("SELECT 0")) {
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(validationSql)) {
                        if (rs.next()) {
                            long invalidCount = rs.getLong(1);
                            double accuracyRatio = 1.0 - ((double) invalidCount / totalRows);
                            columnsAccuracy.put(columnName, accuracyRatio);
                            totalAccuracyRatio += accuracyRatio;
                            validatedColumns++;
                        }
                    }
                }
            }
            
            // 计算总体准确性比例（已验证列的平均值）
            if (validatedColumns > 0) {
                accuracy.put("accuracyRatio", totalAccuracyRatio / validatedColumns);
            } else {
                accuracy.put("accuracyRatio", 1.0); // 如果没有验证任何列，假设准确性为 100%
            }
            
            accuracy.put("columnsAccuracy", columnsAccuracy);
            
        } catch (Exception e) {
            log.error("获取表准确性失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        
        return accuracy;
    }

    @Override
    public Map<String, Object> getTableConsistency(String tableName) throws Exception {
        // Hive 不支持外键约束，返回默认一致性指标
        Map<String, Object> consistency = new HashMap<>();
        consistency.put("consistencyRatio", 1.0); // 由于无外键，默认全部一致
                consistency.put("validCount", 0);
                consistency.put("invalidCount", 0);
                consistency.put("totalCount", 0);
        consistency.put("message", "Hive 不支持外键约束，无法检查引用完整性");
        return consistency;
    }

    @Override
    public Map<String, Object> getTableUniqueness(String tableName) throws Exception {
        Map<String, Object> uniqueness = new HashMap<>();
        try (Connection conn = getConnection()) {
            // 获取表的主键信息
            List<String> primaryKeys = getPrimaryKeys(tableName);
            
            // 获取表的唯一索引信息
            List<Map<String, Object>> indexes = getIndexes(tableName);
            List<String> uniqueColumns = new ArrayList<>();
            
            // 添加主键列
            uniqueColumns.addAll(primaryKeys);
            
            // 添加唯一索引列
            for (Map<String, Object> index : indexes) {
                String indexType = (String) index.get("indexType");
                if (indexType != null && indexType.toUpperCase().contains("UNIQUE")) {
                    String columnName = (String) index.get("columnName");
                    if (columnName != null && !uniqueColumns.contains(columnName)) {
                        uniqueColumns.add(columnName);
                    }
                }
            }
            
            if (uniqueColumns.isEmpty()) {
                uniqueness.put("uniquenessRatio", 0.0);
                uniqueness.put("duplicateCount", 0);
                uniqueness.put("uniqueCount", 0);
                uniqueness.put("totalCount", 0);
                return uniqueness;
            }
            
            // 获取表的总行数
            long totalRows = 0;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(1) FROM " + wrapIdentifier(tableName))) {
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
            
            // 检查每个唯一列的唯一性
            long totalUniqueCount = 0;
            
            for (String column : uniqueColumns) {
                String countSql = String.format(
                    "SELECT COUNT(DISTINCT %s) FROM %s",
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
            double uniquenessRatio = uniqueColumns.size() > 0 ? (double) totalUniqueCount / (totalRows * uniqueColumns.size()) : 0.0;
            
            uniqueness.put("uniquenessRatio", uniquenessRatio);
            uniqueness.put("duplicateCount", (totalRows * uniqueColumns.size()) - totalUniqueCount);
            uniqueness.put("uniqueCount", totalUniqueCount);
            uniqueness.put("totalCount", totalRows * uniqueColumns.size());
            
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
            // 获取表的总行数
            long totalRows = 0;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(1) FROM " + wrapIdentifier(tableName))) {
                if (rs.next()) {
                    totalRows = rs.getLong(1);
                }
            }
            
            if (totalRows == 0) {
                validity.put("validityRatio", 1.0);
                validity.put("validCount", 0);
                validity.put("invalidCount", 0);
                validity.put("totalCount", 0);
                return validity;
            }
            
            // 获取表的列信息
            List<Map<String, String>> columns = new ArrayList<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("DESCRIBE " + wrapIdentifier(tableName))) {
                while (rs.next()) {
                    String colName = rs.getString(1);
                    if (colName != null && !colName.trim().isEmpty() && !colName.trim().startsWith("#")) {
                        Map<String, String> column = new HashMap<>();
                        column.put("name", colName.trim());
                        column.put("type", rs.getString(2));
                        columns.add(column);
                    }
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
            
            // 计算有效性比率
            double validityRatio = totalCheckedCount > 0 ? (double) totalValidCount / totalCheckedCount : 1.0;
            
            validity.put("validityRatio", validityRatio);
            validity.put("validCount", totalValidCount);
            validity.put("invalidCount", totalCheckedCount - totalValidCount);
            validity.put("totalCount", totalCheckedCount);
            
        } catch (Exception e) {
            log.error("计算表有效性失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return validity;
    }

    @Override
    public Map<String, Integer> getQualityIssueCount(String tableName) throws Exception {
        Map<String, Integer> issueCount = new HashMap<>();
        try (Connection conn = getConnection()) {
            // 获取表的列信息
            List<Map<String, String>> columns = new ArrayList<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("DESCRIBE " + wrapIdentifier(tableName))) {
                while (rs.next()) {
                    String colName = rs.getString(1);
                    if (colName != null && !colName.trim().isEmpty() && !colName.trim().startsWith("#")) {
                        Map<String, String> column = new HashMap<>();
                        column.put("name", colName.trim());
                        column.put("type", rs.getString(2));
                        columns.add(column);
                    }
                }
            }
            
            // 计算每列的问题数量
            for (Map<String, String> column : columns) {
                String columnName = column.get("name");
                String dataType = column.get("type");
                
                // 计算空值数量
                String nullCountSql = String.format(
                    "SELECT COUNT(1) FROM %s WHERE %s IS NULL",
                    wrapIdentifier(tableName),
                    wrapIdentifier(columnName)
                );
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(nullCountSql)) {
                    if (rs.next()) {
                        int nullCount = rs.getInt(1);
                        if (nullCount > 0) {
                            issueCount.put(columnName + "_null", nullCount);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("获取质量问题统计失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return issueCount;
    }

    @Override
    public Map<String, Double> getQualityIssueDistribution(String tableName) throws Exception {
        Map<String, Double> distribution = new HashMap<>();
        try (Connection conn = getConnection()) {
            // 获取表的总行数
            long totalRows = 0;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(1) FROM " + wrapIdentifier(tableName))) {
                if (rs.next()) {
                    totalRows = rs.getLong(1);
                }
            }
            
            if (totalRows == 0) {
                return distribution;
            }
            
            // 获取表的列信息
            List<Map<String, String>> columns = new ArrayList<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("DESCRIBE " + wrapIdentifier(tableName))) {
                while (rs.next()) {
                    String colName = rs.getString(1);
                    if (colName != null && !colName.trim().isEmpty() && !colName.trim().startsWith("#")) {
                        Map<String, String> column = new HashMap<>();
                        column.put("name", colName.trim());
                        column.put("type", rs.getString(2));
                        columns.add(column);
                    }
                }
            }
            
            // 计算每列的问题分布
            for (Map<String, String> column : columns) {
                String columnName = column.get("name");
                String dataType = column.get("type");
                
                // 计算空值比例
                String nullCountSql = String.format(
                    "SELECT COUNT(1) FROM %s WHERE %s IS NULL",
                    wrapIdentifier(tableName),
                    wrapIdentifier(columnName)
                );
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(nullCountSql)) {
                    if (rs.next()) {
                        int nullCount = rs.getInt(1);
                        double nullRatio = (double) nullCount / totalRows;
                        distribution.put(columnName + "_null_ratio", nullRatio);
                    }
                }
                
                // 计算格式不正确的比例
                String validationSql = getValidationSqlForDataType(tableName, columnName, dataType);
                if (validationSql != null) {
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(validationSql.replace("COUNT(1)", "COUNT(1) - COUNT(" + wrapIdentifier(columnName) + ")"))) {
                        if (rs.next()) {
                            int invalidCount = rs.getInt(1);
                            double invalidRatio = (double) invalidCount / totalRows;
                            distribution.put(columnName + "_invalid_ratio", invalidRatio);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("获取质量问题分布失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return distribution;
    }

    private String getValidationSqlForDataType(String tableName, String columnName, String dataType) {
        // 针对 Hive 数据类型优化验证 SQL
        String column = wrapIdentifier(columnName);
        String table = wrapIdentifier(tableName);
        
        dataType = dataType.toUpperCase();
        if (dataType.startsWith("STRING") || dataType.startsWith("VARCHAR") || dataType.startsWith("CHAR")) {
            // 对字符串类型不做格式验证，但可以检查长度约束
            if (dataType.startsWith("VARCHAR") || dataType.startsWith("CHAR")) {
                // 提取类型中的长度参数
                Pattern pattern = Pattern.compile("\\((\\d+)\\)");
                Matcher matcher = pattern.matcher(dataType);
                if (matcher.find()) {
                    int maxLength = Integer.parseInt(matcher.group(1));
                    return "SELECT COUNT(*) FROM " + table + 
                           " WHERE " + column + " IS NOT NULL AND LENGTH(" + column + ") > " + maxLength;
                }
            }
            return "SELECT 0"; // 无长度限制的字符串类型无法验证
        }
        
        // 其他数据类型验证保持不变
        if (dataType.startsWith("INT") || dataType.startsWith("SMALLINT") || dataType.startsWith("BIGINT") || 
            dataType.startsWith("TINYINT")) {
            return "SELECT COUNT(*) FROM " + table + 
                   " WHERE " + column + " IS NOT NULL AND CAST(" + column + " AS STRING) REGEXP '^-?[0-9]+$' = FALSE";
        } else if (dataType.startsWith("FLOAT") || dataType.startsWith("DOUBLE") || dataType.startsWith("DECIMAL")) {
            return "SELECT COUNT(*) FROM " + table + 
                   " WHERE " + column + " IS NOT NULL AND CAST(" + column + " AS STRING) REGEXP '^-?[0-9]+(\\.[0-9]+)?$' = FALSE";
        } else if (dataType.startsWith("DATE")) {
            return "SELECT COUNT(*) FROM " + table + 
                   " WHERE " + column + " IS NOT NULL AND CAST(" + column + " AS DATE) IS NULL";
        } else if (dataType.startsWith("TIMESTAMP")) {
            return "SELECT COUNT(*) FROM " + table + 
                   " WHERE " + column + " IS NOT NULL AND CAST(" + column + " AS TIMESTAMP) IS NULL";
        } else if (dataType.startsWith("BOOLEAN")) {
            return "SELECT COUNT(*) FROM " + table + 
                   " WHERE " + column + " IS NOT NULL AND " + column + " NOT IN (TRUE, FALSE)";
        }
        
        // 对于复杂类型（ARRAY、MAP、STRUCT）无法简单验证
        return "SELECT 0";
    }

    @Override
    public List<Map<String, Object>> getTableForeignKeyRelations(String tableName) throws Exception {
        List<Map<String, Object>> relations = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("DESCRIBE FORMATTED " + wrapIdentifier(tableName))) {
            boolean inConstraintSection = false;
            while (rs.next()) {
                String col1 = rs.getString(1);
                if (col1 != null) {
                    col1 = col1.trim();
                    if (col1.equals("# Constraints")) {
                        inConstraintSection = true;
                        continue;
                    }
                    
                    if (inConstraintSection && col1.contains("Foreign key")) {
                        Map<String, Object> relation = new HashMap<>();
                        String constraintInfo = rs.getString(2);
                        if (constraintInfo != null) {
                            // 解析外键信息，格式通常为：parent: <parent_table>
                            String[] parts = constraintInfo.split(":");
                            if (parts.length > 1) {
                                relation.put("constraintName", col1.replace("Foreign key", "").trim());
                                relation.put("tableName", tableName);
                                relation.put("referencedTableName", parts[1].trim());
                                relations.add(relation);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取表外键关系失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return relations;
    }

    @Override
    public List<Map<String, Object>> getReferencedByRelations(String tableName) throws Exception {
        // Hive 不支持外键约束，返回空列表
        return new ArrayList<>();
    }

    @Override
    public Map<String, Object> getTableFieldStatistics(String tableName) throws Exception {
        Map<String, Object> statistics = new HashMap<>();
        try (Connection conn = getConnection()) {
            // 获取表的列信息
            List<Map<String, String>> columns = new ArrayList<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("DESCRIBE " + wrapIdentifier(tableName))) {
                while (rs.next()) {
                    String colName = rs.getString(1);
                    if (colName != null && !colName.trim().isEmpty() && !colName.trim().startsWith("#")) {
                        Map<String, String> column = new HashMap<>();
                        column.put("name", colName.trim());
                        column.put("type", rs.getString(2));
                        columns.add(column);
                    }
                }
            }
            
            // 统计各类型字段数量
            int numericCount = 0;
            int stringCount = 0;
            int dateCount = 0;
            int booleanCount = 0;
            int otherCount = 0;
            
            for (Map<String, String> column : columns) {
                String dataType = column.get("type");
                if (dataType != null) {
                    if (dataType.startsWith("int") || dataType.startsWith("bigint") || dataType.startsWith("smallint") || 
                        dataType.startsWith("tinyint") || dataType.startsWith("float") || dataType.startsWith("double") || 
                        dataType.startsWith("decimal")) {
                        numericCount++;
                    } else if (dataType.startsWith("string") || dataType.startsWith("varchar") || dataType.startsWith("char")) {
                        stringCount++;
                    } else if (dataType.startsWith("date") || dataType.startsWith("timestamp")) {
                        dateCount++;
                    } else if (dataType.startsWith("boolean")) {
                        booleanCount++;
                    } else {
                        otherCount++;
                    }
                }
            }
            
            statistics.put("totalFields", columns.size());
            statistics.put("numericFields", numericCount);
            statistics.put("stringFields", stringCount);
            statistics.put("dateFields", dateCount);
            statistics.put("booleanFields", booleanCount);
            statistics.put("otherFields", otherCount);
            
            // 获取表的总行数
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(1) FROM " + wrapIdentifier(tableName))) {
                if (rs.next()) {
                    statistics.put("rowCount", rs.getLong(1));
                }
            }
            
            // 获取表的主键信息
            List<String> primaryKeys = getPrimaryKeys(tableName);
            statistics.put("primaryKeyCount", primaryKeys.size());
            
            // 获取表的索引信息
            List<Map<String, Object>> indexes = getIndexes(tableName);
            statistics.put("indexCount", indexes.size());
            
        } catch (Exception e) {
            log.error("获取表字段统计信息失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return statistics;
    }

    public Date getDate() throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT CURRENT_TIMESTAMP")) {
            if (rs.next()) {
                return rs.getTimestamp(1);
            }
        } catch (Exception e) {
            log.error("获取当前日期失败: error={}", e.getMessage(), e);
            throw e;
        }
        return new Date();
    }

    @Override
    public List<Map<String, Object>> getTableDependencies(String tableName) throws Exception {
        // 使用Hive的DESCRIBE EXTENDED命令获取表信息，包括依赖关系
        String sql = "DESCRIBE EXTENDED " + wrapIdentifier(tableName);
        List<Map<String, Object>> dependencies = new ArrayList<>();
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
        
            boolean foundDependencies = false;
            StringBuilder dependencyInfo = new StringBuilder();
            
            while (rs.next()) {
                String colName = rs.getString(1);
                if (colName != null && (colName.contains("viewOriginalText") || colName.contains("viewExpandedText"))) {
                    String viewText = rs.getString(2);
                    if (viewText != null) {
                        // 从视图定义中提取表名
                        extractTableNamesFromSql(viewText, dependencies);
                    }
                    foundDependencies = true;
                }
            }
            
            if (!foundDependencies) {
                // 如果不是视图，尝试查询Hive元数据获取依赖关系
                // 注意：这需要对Hive元数据表的访问权限
                try {
                    sql = "SELECT d.NAME as dependent_table " +
                          "FROM TBLS t JOIN DBS d ON t.DB_ID = d.DB_ID " +
                          "WHERE t.TBL_NAME = ?";
                    
                    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        pstmt.setString(1, tableName);
                        try (ResultSet depsRs = pstmt.executeQuery()) {
                            while (depsRs.next()) {
                                Map<String, Object> dep = new HashMap<>();
                                dep.put("table_name", depsRs.getString("dependent_table"));
                                dependencies.add(dep);
                            }
                        }
                    }
        } catch (Exception e) {
                    // 元数据查询可能会失败，忽略错误
        }
            }
        }
        
        return dependencies;
    }

    private void extractTableNamesFromSql(String sql, List<Map<String, Object>> tables) {
        // 简单实现：使用正则表达式从SQL中提取表名
        // 实际应用中需要更复杂的SQL解析器
        Pattern pattern = Pattern.compile("\\bFROM\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sql);
        
        while (matcher.find()) {
            String tableName = matcher.group(1);
            Map<String, Object> table = new HashMap<>();
            table.put("table_name", tableName);
            tables.add(table);
        }
        
        // 查找JOIN子句中的表
        pattern = Pattern.compile("\\bJOIN\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(sql);
        
        while (matcher.find()) {
            String tableName = matcher.group(1);
            Map<String, Object> table = new HashMap<>();
            table.put("table_name", tableName);
            tables.add(table);
        }
    }

    public List<Map<String, Object>> getTableChangeHistory(String tableName) throws Exception {
        List<Map<String, Object>> history = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("DESCRIBE FORMATTED " + wrapIdentifier(tableName))) {
            while (rs.next()) {
                String col1 = rs.getString(1);
                if (col1 != null) {
                    col1 = col1.trim();
                    if (col1.equals("CreateTime:") || col1.equals("LastAccessTime:") || col1.equals("transient_lastDdlTime:")) {
                        String timeStr = rs.getString(2);
                        if (timeStr != null && !timeStr.trim().isEmpty()) {
                            try {
                                Map<String, Object> change = new HashMap<>();
                                Date changeTime = new java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US).parse(timeStr.trim());
                                change.put("changeTime", changeTime);
                                change.put("user", "unknown"); // Hive不提供操作用户信息
                                
                                if (col1.equals("CreateTime:")) {
                                    change.put("operationType", "CREATE");
                                } else if (col1.equals("LastAccessTime:")) {
                                    change.put("operationType", "ACCESS");
                                } else {
                                    change.put("operationType", "ALTER");
                                }
                                
                                change.put("sql", ""); // Hive不提供历史SQL信息
                                history.add(change);
                            } catch (Exception e) {
                                log.warn("解析表变更时间失败: table={}, time={}", tableName, timeStr);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取表变更历史失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return history;
    }

    @Override
    public Map<String, Object> getTableUpdateFrequency(String tableName) throws Exception {
        Map<String, Object> frequency = new HashMap<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("DESCRIBE FORMATTED " + wrapIdentifier(tableName))) {
            Date createTime = null;
            Date lastAccessTime = null;
            
            while (rs.next()) {
                String col1 = rs.getString(1);
                if (col1 != null) {
                    col1 = col1.trim();
                    if (col1.equals("CreateTime:")) {
                        String timeStr = rs.getString(2);
                        if (timeStr != null && !timeStr.trim().isEmpty()) {
                            try {
                                createTime = new java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US).parse(timeStr.trim());
                            } catch (Exception e) {
                                log.warn("解析表创建时间失败: table={}, time={}", tableName, timeStr);
                            }
                        }
                    } else if (col1.equals("LastAccessTime:")) {
                        String timeStr = rs.getString(2);
                        if (timeStr != null && !timeStr.trim().isEmpty()) {
                            try {
                                lastAccessTime = new java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US).parse(timeStr.trim());
                            } catch (Exception e) {
                                log.warn("解析表最后访问时间失败: table={}, time={}", tableName, timeStr);
                            }
                        }
                    }
                }
            }
            
            frequency.put("createTime", createTime);
            frequency.put("lastUpdateTime", lastAccessTime);
            
            if (createTime != null && lastAccessTime != null) {
                long diffInMillis = lastAccessTime.getTime() - createTime.getTime();
                long diffInDays = diffInMillis / (24 * 60 * 60 * 1000);
                
                if (diffInDays > 0) {
                    frequency.put("averageUpdateInterval", diffInDays);
                    frequency.put("updateFrequency", 1.0 / diffInDays);
                } else {
                    frequency.put("averageUpdateInterval", 0);
                    frequency.put("updateFrequency", 0);
                }
            }
            
        } catch (Exception e) {
            log.error("获取表更新频率失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return frequency;
    }

    @Override
    public List<Map<String, Object>> getTableGrowthTrend(String tableName, int days) throws Exception {
        // Hive不提供历史数据增长趋势的直接查询方式
        // 这里返回一个空列表，实际应用中可能需要通过其他方式收集这些数据
        return new ArrayList<>();
    }

    @Override
    public List<Map<String, Object>> getTableDataSample(String tableName, int sampleSize) throws Exception {
        // Hive 的 TABLESAMPLE 可以按百分比或按字节数采样
        // 这里使用按百分比采样，确保有足够数据
        // 注意：BERNOULLI 采样比 BUCKET 采样更均匀，但可能性能较低
        String sql = "SELECT * FROM " + wrapIdentifier(tableName) + 
                     " TABLESAMPLE(10 PERCENT) " +
                     " LIMIT " + sampleSize;
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
        
            return resultSetToList(rs);
        }
    }

    @Override
    public Map<String, Object> getColumnValueRange(String tableName, String columnName) throws Exception {
        Map<String, Object> range = new HashMap<>();
        try (Connection conn = getConnection()) {
            // 首先确定列的数据类型
            String dataType = null;
            try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("DESCRIBE " + wrapIdentifier(tableName) + " " + wrapIdentifier(columnName))) {
            if (rs.next()) {
                    dataType = rs.getString(2);
                }
            }
            
            if (dataType == null) {
                throw new SQLException("Column not found: " + columnName);
            }
            
            // 根据数据类型构建不同的查询
            String rangeSql;
            if (dataType.startsWith("int") || dataType.startsWith("bigint") || 
                dataType.startsWith("smallint") || dataType.startsWith("tinyint") || 
                dataType.startsWith("float") || dataType.startsWith("double") || 
                        dataType.startsWith("decimal")) {
                        
                // 数值类型的范围查询
                rangeSql = "SELECT " +
                          "MIN(" + wrapIdentifier(columnName) + ") as min_value, " +
                          "MAX(" + wrapIdentifier(columnName) + ") as max_value, " +
                          "AVG(" + wrapIdentifier(columnName) + ") as avg_value " +
                          "FROM " + wrapIdentifier(tableName);
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(rangeSql)) {
                    if (rs.next()) {
                        range.put("minValue", rs.getObject(1));
                        range.put("maxValue", rs.getObject(2));
                        range.put("avgValue", rs.getObject(3));
                    }
                }
                
            } else if (dataType.startsWith("string") || dataType.startsWith("varchar") || 
                      dataType.startsWith("char")) {
                
                // 字符串类型的长度范围
                rangeSql = "SELECT " +
                          "MIN(LENGTH(" + wrapIdentifier(columnName) + ")) as min_length, " +
                          "MAX(LENGTH(" + wrapIdentifier(columnName) + ")) as max_length, " +
                          "AVG(LENGTH(" + wrapIdentifier(columnName) + ")) as avg_length " +
                          "FROM " + wrapIdentifier(tableName);
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(rangeSql)) {
                    if (rs.next()) {
                        range.put("minLength", rs.getInt(1));
                        range.put("maxLength", rs.getInt(2));
                        range.put("avgLength", rs.getDouble(3));
                    }
                }
                
                // 对于字符串，还可以获取字典序的最小和最大值
                rangeSql = "SELECT " +
                          "MIN(" + wrapIdentifier(columnName) + ") as lex_min, " +
                          "MAX(" + wrapIdentifier(columnName) + ") as lex_max " +
                          "FROM " + wrapIdentifier(tableName);
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(rangeSql)) {
                    if (rs.next()) {
                        range.put("lexMinValue", rs.getString(1));
                        range.put("lexMaxValue", rs.getString(2));
                            }
                        }
                        
                    } else if (dataType.startsWith("date") || dataType.startsWith("timestamp")) {
                        
                // 日期类型的范围
                rangeSql = "SELECT " +
                          "MIN(" + wrapIdentifier(columnName) + ") as min_date, " +
                          "MAX(" + wrapIdentifier(columnName) + ") as max_date " +
                          "FROM " + wrapIdentifier(tableName);
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(rangeSql)) {
                    if (rs.next()) {
                        // 需要使用适当的方法获取日期/时间戳
                        if (dataType.startsWith("date")) {
                            range.put("minDate", rs.getDate(1));
                            range.put("maxDate", rs.getDate(2));
                        } else {
                            range.put("minTimestamp", rs.getTimestamp(1));
                            range.put("maxTimestamp", rs.getTimestamp(2));
                        }
                    }
                }
            }
            
            // 对所有类型都计算空值率
            String nullCountSql = "SELECT " +
                                "COUNT(*) as total_count, " +
                                "SUM(CASE WHEN " + wrapIdentifier(columnName) + " IS NULL THEN 1 ELSE 0 END) as null_count " +
                                "FROM " + wrapIdentifier(tableName);
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(nullCountSql)) {
                if (rs.next()) {
                    long totalCount = rs.getLong(1);
                    long nullCount = rs.getLong(2);
                    range.put("totalCount", totalCount);
                        range.put("nullCount", nullCount);
                        range.put("nullRatio", totalCount > 0 ? (double) nullCount / totalCount : 0);
                    }
                }
        } catch (Exception e) {
            log.error("获取列值范围失败: table={}, column={}, error={}", tableName, columnName, e.getMessage(), e);
            throw e;
        }
        
        return range;
    }

    @Override
    public List<Map<String, Object>> getColumnValueDistribution(String tableName, String columnName, int topN) throws Exception {
        String column = wrapIdentifier(columnName);
        String table = wrapIdentifier(tableName);
        
        // 使用Hive兼容的语法
        String sql = "SELECT " + column + " as value, COUNT(*) as count " +
                     "FROM " + table + " " +
                     "GROUP BY " + column + " " +
                     "ORDER BY count DESC " +
                     "LIMIT " + topN;
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
        
            return resultSetToList(rs);
        }
    }

    @Override
    public Map<String, Object> calculateQualityMetric(String tableName, String metricType) throws Exception {
        Map<String, Object> result = new HashMap<>();
        String table = wrapIdentifier(tableName);
        
        // 根据不同的指标类型构建SQL
        String sql;
        switch (metricType.toUpperCase()) {
            case "COMPLETENESS":
                // 计算非空率
                List<Map<String, Object>> columns = listColumns(tableName);
                Map<String, Double> completenessRates = new HashMap<>();
                
                for (Map<String, Object> column : columns) {
                    String columnName = (String) column.get("COLUMN_NAME");
                    if (columnName != null) {
                        sql = "SELECT " +
                              "COUNT(*) AS total_count, " +
                              "COUNT(" + wrapIdentifier(columnName) + ") AS non_null_count " +
                              "FROM " + table;
                        
                        try (Connection conn = getConnection();
                             Statement stmt = conn.createStatement();
                             ResultSet rs = stmt.executeQuery(sql)) {
                        
                            if (rs.next()) {
                                long totalCount = rs.getLong("total_count");
                                long nonNullCount = rs.getLong("non_null_count");
                                double completenessRate = totalCount > 0 ? (double) nonNullCount / totalCount : 1.0;
                                completenessRates.put(columnName, completenessRate);
                            }
                        }
                    }
                }
                
                result.put("column_completeness_rates", completenessRates);
                break;
                
            case "ACCURACY":
                // 数据准确性检查
                // Hive中需要使用正则表达式或自定义UDF进行数据类型验证
                // 这里提供一个简化实现
                columns = listColumns(tableName);
                Map<String, Long> inaccurateDataCounts = new HashMap<>();
                
                for (Map<String, Object> column : columns) {
                    String columnName = (String) column.get("COLUMN_NAME");
                    String dataType = (String) column.get("TYPE_NAME");
                    
                    if (columnName != null && dataType != null) {
                        sql = getValidationSqlForDataType(tableName, columnName, dataType);
                        
                        try (Connection conn = getConnection();
                             Statement stmt = conn.createStatement();
                             ResultSet rs = stmt.executeQuery(sql)) {
                        
                            if (rs.next()) {
                                long inaccurateCount = rs.getLong(1);
                                inaccurateDataCounts.put(columnName, inaccurateCount);
                            }
                        }
                    }
                }
                
                result.put("inaccurate_data_counts", inaccurateDataCounts);
                    break;
                
            // 可以添加其他指标类型...
                
                default:
                throw new IllegalArgumentException("Unsupported metric type: " + metricType);
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getQualityIssues(String tableName) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        try {
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
    public Map<String, String> getStoredProcedureDefinitions(String schemaName) throws Exception {
        // Hive 不支持存储过程，返回空映射
        return new HashMap<>();
    }

    // 处理复杂类型的方法
    public String formatComplexTypeDefinition(String name, String baseType, Map<String, Object> typeParams) {
        StringBuilder sb = new StringBuilder();
        sb.append(wrapIdentifier(name)).append(" ");
        
        if ("ARRAY".equalsIgnoreCase(baseType)) {
            // 数组类型: ARRAY<elementType>
            String elementType = (String) typeParams.get("elementType");
            sb.append("ARRAY<").append(elementType).append(">");
        } else if ("MAP".equalsIgnoreCase(baseType)) {
            // 映射类型: MAP<keyType, valueType>
            String keyType = (String) typeParams.get("keyType");
            String valueType = (String) typeParams.get("valueType");
            sb.append("MAP<").append(keyType).append(", ").append(valueType).append(">");
        } else if ("STRUCT".equalsIgnoreCase(baseType)) {
            // 结构类型: STRUCT<field1:type1, field2:type2, ...>
            @SuppressWarnings("unchecked")
            List<Map<String, String>> fields = (List<Map<String, String>>) typeParams.get("fields");
            sb.append("STRUCT<");
            
            List<String> fieldDefs = new ArrayList<>();
            for (Map<String, String> field : fields) {
                String fieldName = field.get("name");
                String fieldType = field.get("type");
                fieldDefs.add(fieldName + ":" + fieldType);
            }
            
            sb.append(String.join(", ", fieldDefs));
            sb.append(">");
        }
        
        // 添加注释（如果有）
        String comment = (String) typeParams.get("comment");
        if (comment != null && !comment.isEmpty()) {
            sb.append(" COMMENT ").append(wrapValue(comment));
        }
        
        return sb.toString();
    }

    // 解析复杂类型的方法
    public Map<String, Object> parseComplexType(String typeDefinition) {
        Map<String, Object> result = new HashMap<>();
        
        // 解析数组类型: ARRAY<elementType>
        if (typeDefinition.toUpperCase().startsWith("ARRAY<")) {
            result.put("baseType", "ARRAY");
            String elementType = typeDefinition.substring(6, typeDefinition.length() - 1);
            result.put("elementType", elementType);
        } 
        // 解析映射类型: MAP<keyType, valueType>
        else if (typeDefinition.toUpperCase().startsWith("MAP<")) {
            result.put("baseType", "MAP");
            String content = typeDefinition.substring(4, typeDefinition.length() - 1);
            int commaPos = content.indexOf(',');
            if (commaPos > 0) {
                String keyType = content.substring(0, commaPos).trim();
                String valueType = content.substring(commaPos + 1).trim();
                result.put("keyType", keyType);
                result.put("valueType", valueType);
            }
        } 
        // 解析结构类型: STRUCT<field1:type1, field2:type2, ...>
        else if (typeDefinition.toUpperCase().startsWith("STRUCT<")) {
            result.put("baseType", "STRUCT");
            String content = typeDefinition.substring(7, typeDefinition.length() - 1);
            List<Map<String, String>> fields = new ArrayList<>();
            
            // 分割字段定义
            String[] fieldDefs = content.split(",(?=(?:[^<]*<[^>]*>)*[^<]*$)");
            for (String fieldDef : fieldDefs) {
                int colonPos = fieldDef.indexOf(':');
                if (colonPos > 0) {
                    String fieldName = fieldDef.substring(0, colonPos).trim();
                    String fieldType = fieldDef.substring(colonPos + 1).trim();
                    
                    Map<String, String> field = new HashMap<>();
                    field.put("name", fieldName);
                    field.put("type", fieldType);
                    fields.add(field);
                }
            }
            
            result.put("fields", fields);
        }
        
        return result;
    }

    // 获取分析表的SQL
    public String getAnalyzeTableSql(String tableName) {
        return "ANALYZE TABLE " + wrapIdentifier(tableName) + " COMPUTE STATISTICS";
    }

    // 获取分析表列的SQL
    public String getAnalyzeTableColumnsSql(String tableName, List<String> columns) {
        StringBuilder sb = new StringBuilder();
        sb.append("ANALYZE TABLE ").append(wrapIdentifier(tableName));
        sb.append(" COMPUTE STATISTICS FOR COLUMNS ");
        
        List<String> wrappedColumns = columns.stream()
                .map(this::wrapIdentifier)
                .collect(Collectors.toList());
        
        sb.append(String.join(", ", wrappedColumns));
        
        return sb.toString();
    }

    // 获取将查询结果写入表的SQL
    public String getInsertOverwriteTableSql(String tableName, String query) {
        return "INSERT OVERWRITE TABLE " + wrapIdentifier(tableName) + " " + query;
    }

    // 获取将查询结果导出到HDFS的SQL
    public String getInsertOverwriteDirectorySql(String hdfsPath, String query) {
        return "INSERT OVERWRITE DIRECTORY '" + hdfsPath + "' " + query;
    }

    // 获取将查询结果导出到本地文件系统的SQL
    public String getInsertOverwriteLocalDirectorySql(String localPath, String query) {
        return "INSERT OVERWRITE LOCAL DIRECTORY '" + localPath + "' " + query;
    }

    // 获取通过查询创建表的SQL
    public String getCreateTableAsSelectSql(String tableName, String query, Map<String, String> tableProperties) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(wrapIdentifier(tableName));
        
        // 添加表属性
        if (tableProperties != null && !tableProperties.isEmpty()) {
            sb.append("\nTBLPROPERTIES (");
            List<String> props = new ArrayList<>();
            for (Map.Entry<String, String> entry : tableProperties.entrySet()) {
                props.add(wrapValue(entry.getKey()) + "=" + wrapValue(entry.getValue()));
            }
            sb.append(String.join(", ", props));
            sb.append(")");
        }
        
        sb.append("\nAS ").append(query);
        
        return sb.toString();
    }

    // 获取创建视图的SQL
    public String getCreateViewSql(String viewName, String query, String comment) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE VIEW ").append(wrapIdentifier(viewName));
        
        // 添加视图注释
        if (comment != null && !comment.isEmpty()) {
            sb.append(" COMMENT ").append(wrapValue(comment));
        }
        
        sb.append(" AS ").append(query);
        
        return sb.toString();
    }

    // 获取修改视图的SQL
    public String getAlterViewSql(String viewName, String query) {
        return "ALTER VIEW " + wrapIdentifier(viewName) + " AS " + query;
    }

    // 获取删除视图的SQL
    public String getDropViewSql(String viewName) {
        return "DROP VIEW IF EXISTS " + wrapIdentifier(viewName);
    }

    // 获取设置Hive参数的SQL
    public String getSetParameterSql(String paramName, String paramValue) {
        return "SET " + paramName + "=" + paramValue;
    }

    // 常用的Hive参数设置
    public String getEnableMapJoinSql() {
        return "SET hive.auto.convert.join=true";
    }

    public String getSetMapJoinSizeSql(int sizeInMB) {
        return "SET hive.auto.convert.join.noconditionaltask.size=" + sizeInMB;
    }

    public String getEnableDynamicPartitionSql() {
        return "SET hive.exec.dynamic.partition.mode=nonstrict";
    }

    public String getSetReducersSql(int numReducers) {
        return "SET mapred.reduce.tasks=" + numReducers;
    }

    // 获取MERGE操作的SQL (Hive 3.0+)
    public String getMergeSql(String targetTable, String sourceTable, String joinCondition, 
                             String matchedAction, String notMatchedAction) {
        // 添加版本检查提示
        StringBuilder sb = new StringBuilder();
        sb.append("-- 注意：MERGE 语法仅在 Hive 3.0+ 且启用事务时可用\n");
        sb.append("MERGE INTO ").append(wrapIdentifier(targetTable)).append(" AS target");
        sb.append(" USING ").append(wrapIdentifier(sourceTable)).append(" AS source");
        sb.append(" ON ").append(joinCondition);
        
        if (matchedAction != null && !matchedAction.isEmpty()) {
            sb.append(" WHEN MATCHED THEN ").append(matchedAction);
        }
        
        if (notMatchedAction != null && !notMatchedAction.isEmpty()) {
            sb.append(" WHEN NOT MATCHED THEN ").append(notMatchedAction);
        }
        
        return sb.toString();
    }

    // 获取从本地文件加载数据的SQL
    public String getLoadDataLocalSql(String tableName, String filePath, String delimiter, String overwrite) {
        StringBuilder sb = new StringBuilder();
        sb.append("LOAD DATA LOCAL INPATH ");
        sb.append(wrapValue(filePath));
        
        if ("true".equalsIgnoreCase(overwrite)) {
            sb.append(" OVERWRITE");
        } else {
            sb.append(" INTO");
        }
        
        sb.append(" TABLE ");
        sb.append(wrapIdentifier(tableName));
        
        if (delimiter != null && !delimiter.isEmpty()) {
            sb.append(" FIELDS TERMINATED BY ");
            sb.append(wrapValue(delimiter));
        }
        
        return sb.toString();
    }

    // 获取从HDFS加载数据的SQL
    public String getLoadDataSql(String tableName, String hdfsPath, String delimiter, String overwrite) {
        StringBuilder sb = new StringBuilder();
        sb.append("LOAD DATA INPATH ");
        sb.append(wrapValue(hdfsPath));
        
        if ("true".equalsIgnoreCase(overwrite)) {
            sb.append(" OVERWRITE");
        } else {
            sb.append(" INTO");
        }
        
        sb.append(" TABLE ");
        sb.append(wrapIdentifier(tableName));
        
        if (delimiter != null && !delimiter.isEmpty()) {
            sb.append(" FIELDS TERMINATED BY ");
            sb.append(wrapValue(delimiter));
        }
        
        return sb.toString();
    }

    // 获取创建临时表的SQL
    public String getCreateTemporaryTableSql(String tableName, List<ColumnDefinition> columns) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TEMPORARY TABLE ");
        sql.append(wrapIdentifier(tableName));
        sql.append(" (\n");
        
        List<String> columnDefs = new ArrayList<>();
        for (ColumnDefinition column : columns) {
            columnDefs.add("  " + formatFieldDefinition(
                column.getName(), column.getType(), column.getLength(),
                column.getPrecision(), column.getScale(), column.isNullable(),
                column.getDefaultValue(), column.getComment()
            ));
        }
        
        sql.append(String.join(",\n", columnDefs));
        sql.append("\n)");
        
        return sql.toString();
    }

    // 获取修改表属性的SQL
    public String getAlterTablePropertiesSql(String tableName, Map<String, String> properties) {
        StringBuilder sb = new StringBuilder();
        sb.append("ALTER TABLE ");
        sb.append(wrapIdentifier(tableName));
        sb.append(" SET TBLPROPERTIES (");
        
        List<String> propList = new ArrayList<>();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            propList.add(wrapValue(entry.getKey()) + "=" + wrapValue(entry.getValue()));
        }
        
        sb.append(String.join(", ", propList));
        sb.append(")");
        
        return sb.toString();
    }

    // 获取修改表存储格式的SQL
    public String getAlterTableStorageFormatSql(String tableName, String format) {
        return "ALTER TABLE " + wrapIdentifier(tableName) + " SET FILEFORMAT " + format;
    }

    // 获取修改表分区的SQL
    public String getAlterTableAddPartitionSql(String tableName, Map<String, String> partitionSpec) {
        StringBuilder sb = new StringBuilder();
        sb.append("ALTER TABLE ");
        sb.append(wrapIdentifier(tableName));
        sb.append(" ADD PARTITION (");
        
        List<String> partitions = new ArrayList<>();
        for (Map.Entry<String, String> entry : partitionSpec.entrySet()) {
            partitions.add(wrapIdentifier(entry.getKey()) + "=" + wrapValue(entry.getValue()));
        }
        
        sb.append(String.join(", ", partitions));
        sb.append(")");
        
        return sb.toString();
    }

    // 获取创建临时函数的SQL
    public String getCreateTemporaryFunctionSql(String functionName, String className) {
        return "CREATE TEMPORARY FUNCTION " + wrapIdentifier(functionName) + 
               " AS " + wrapValue(className);
    }

    // 获取删除函数的SQL
    public String getDropFunctionSql(String functionName) {
        return "DROP TEMPORARY FUNCTION IF EXISTS " + wrapIdentifier(functionName);
    }

    // 添加用于将 ResultSet 转换为 List<Map<String, Object>> 的工具方法
    private List<Map<String, Object>> resultSetToList(ResultSet rs) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object value = rs.getObject(i);
                row.put(columnName, value);
            }
            list.add(row);
        }
        
        return list;
    }
    
    @Override
    public List<String> getTablePartitions(String tableName, String partitionField) throws Exception {
        List<String> partitions = new ArrayList<>();
        
        // In Hive, we can get partition information from DESCRIBE FORMATTED
        String partitionQuery = "SHOW PARTITIONS " + wrapIdentifier(tableName);
        
        boolean hasPartitions = false;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(partitionQuery)) {
            
            while (rs.next()) {
                hasPartitions = true;
                String partitionSpec = rs.getString(1); // Format: partcol1=val1/partcol2=val2/...
                
                // Parse the partition specification to extract the value of the requested field
                Map<String, String> partMap = parsePartitionSpec(partitionSpec);
                if (partMap.containsKey(partitionField)) {
                    partitions.add(partMap.get(partitionField));
                }
            }
        } catch (Exception e) {
            log.warn("Could not get partition information using SHOW PARTITIONS: {}", e.getMessage());
        }
        
        // If no partitions found or couldn't extract the desired field, fall back to distinct values
        if (!hasPartitions) {
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
                log.error("Failed to get distinct partition values: {}", e.getMessage());
            }
        }
        
        return partitions;
    }
    
    // Helper method to parse Hive partition specification
    private Map<String, String> parsePartitionSpec(String partitionSpec) {
        Map<String, String> partMap = new HashMap<>();
        String[] parts = partitionSpec.split("/");
        
        for (String part : parts) {
            String[] keyValue = part.split("=");
            if (keyValue.length == 2) {
                partMap.put(keyValue[0], keyValue[1]);
            }
        }
        
        return partMap;
    }

    @Override
    public String getCreateTempTableSql(String tempTableName, String sourceTableName, boolean preserveRows) throws Exception {
        // 获取源表的列信息
        List<Map<String, Object>> columnsData = listColumns(sourceTableName);
        
        if (columnsData == null || columnsData.isEmpty()) {
            throw new IllegalArgumentException("Cannot create temp table - no columns found in source table: " + sourceTableName);
        }
        
        // 将 Map 列表转换为 ColumnDefinition 列表
        List<ColumnDefinition> columns = new ArrayList<>();
        for (Map<String, Object> columnData : columnsData) {
            ColumnDefinition column = new ColumnDefinition();
            column.setName((String) columnData.get("name"));
            column.setType((String) columnData.get("type"));
            
            if (columnData.get("maxLength") != null) {
                column.setLength(((Number) columnData.get("maxLength")).intValue());
            }
            
            if (columnData.get("precision") != null) {
                column.setPrecision(((Number) columnData.get("precision")).intValue());
            }
            
            if (columnData.get("scale") != null) {
                column.setScale(((Number) columnData.get("scale")).intValue());
            }
            
            column.setNullable(columnData.containsKey("nullable") ? (Boolean) columnData.get("nullable") : true);
            column.setDefaultValue(columnData.get("defaultValue") != null ? columnData.get("defaultValue").toString() : null);
            column.setPrimaryKey(columnData.containsKey("primary_key") ? (Boolean) columnData.get("primary_key") : false);
            
            columns.add(column);
        }
        
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("CREATE ");
        
        // Hive中临时表总是会话级别的，不考虑preserveRows参数
        sqlBuilder.append("TEMPORARY TABLE ")
                .append(wrapIdentifier(tempTableName))
                .append(" (");
        
        for (int i = 0; i < columns.size(); i++) {
            ColumnDefinition column = columns.get(i);
            
            if (i > 0) {
                sqlBuilder.append(", ");
            }
            
            sqlBuilder.append(wrapIdentifier(column.getName()))
                    .append(" ")
                    .append(column.getType());
            
            // 添加精度/长度信息
            String typeName = column.getType().toUpperCase();
            if ((typeName.equals("DECIMAL") || typeName.equals("NUMERIC")) && column.getPrecision() != null) {
                int precision = column.getPrecision() > 0 ? column.getPrecision() : 38; // 默认最大精度
                int scale = column.getScale() != null && column.getScale() >= 0 ? column.getScale() : 0;
                sqlBuilder.append("(").append(precision).append(",").append(scale).append(")");
            } else if (column.getLength() != null && column.getLength() > 0 
                    && (typeName.equals("CHAR") 
                    || typeName.equals("VARCHAR")
                    || typeName.equals("BINARY")
                    || typeName.equals("VARBINARY"))) {
                sqlBuilder.append("(").append(column.getLength()).append(")");
            }
            
            // Hive临时表不支持NULL约束和DEFAULT值
        }
        
        // Hive临时表不支持主键
        sqlBuilder.append(")");
        
        return sqlBuilder.toString();
    }
    
    // 获取表的存储格式
    private String getTableFormat(String tableName) {
        String format = "";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            String sql = "DESCRIBE FORMATTED " + wrapIdentifier(tableName);
            try (ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    String colName = rs.getString(1);
                    if (colName != null && colName.trim().contains("Storage Format")) {
                        format = rs.getString(2);
                        if (format != null) {
                            format = format.trim();
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Unable to determine table format: {}", e.getMessage());
        }
        return format;
    }
} 