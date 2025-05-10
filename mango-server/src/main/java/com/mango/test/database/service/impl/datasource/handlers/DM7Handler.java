package com.mango.test.database.service.impl.datasource.handlers;

import com.mango.test.database.entity.ColumnDefinition;
import com.mango.test.database.entity.DataSource;
import com.mango.test.database.entity.IndexDefinition;
import com.mango.test.database.entity.TableDefinition;
import com.mango.test.database.service.impl.datasource.AbstractDatabaseHandler;

import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;

public class DM7Handler extends AbstractDatabaseHandler {

    private static final String DRIVER_CLASS = "dm.jdbc.driver.DmDriver";
    private static final String DEFAULT_PORT = "5236";
    private static final String URL_TEMPLATE = "jdbc:dm://%s:%s/%s";

    private static final int DEFAULT_VARCHAR_LENGTH = 255;
    private static final int MAX_VARCHAR_LENGTH = 32767;

    private static final Map<String, String> TYPE_MAPPING = new HashMap<>();
    private static final Map<String, Integer> DEFAULT_LENGTH_MAPPING = new HashMap<>();
    private static final Map<String, Map<String, String>> COMMON_TYPE_MAPPING = new HashMap<>();

    static {
        // 字符串类型
        TYPE_MAPPING.put("string", "VARCHAR");
        TYPE_MAPPING.put("char", "CHAR");
        TYPE_MAPPING.put("text", "TEXT");
        TYPE_MAPPING.put("nchar", "NCHAR");
        TYPE_MAPPING.put("nvarchar", "NVARCHAR");
        TYPE_MAPPING.put("ntext", "NTEXT");
        TYPE_MAPPING.put("clob", "CLOB");
        TYPE_MAPPING.put("nclob", "NCLOB");

        // 数值类型
        TYPE_MAPPING.put("tinyint", "TINYINT");
        TYPE_MAPPING.put("smallint", "SMALLINT");
        TYPE_MAPPING.put("int", "INT");
        TYPE_MAPPING.put("integer", "INTEGER");
        TYPE_MAPPING.put("bigint", "BIGINT");
        TYPE_MAPPING.put("float", "FLOAT");
        TYPE_MAPPING.put("double", "DOUBLE");
        TYPE_MAPPING.put("decimal", "DECIMAL");
        TYPE_MAPPING.put("number", "NUMBER");
        TYPE_MAPPING.put("numeric", "NUMERIC");

        // 日期时间类型
        TYPE_MAPPING.put("date", "DATE");
        TYPE_MAPPING.put("time", "TIME");
        TYPE_MAPPING.put("timestamp", "TIMESTAMP");
        TYPE_MAPPING.put("datetime", "TIMESTAMP");

        // 二进制类型
        TYPE_MAPPING.put("binary", "BINARY");
        TYPE_MAPPING.put("varbinary", "VARBINARY");
        TYPE_MAPPING.put("blob", "BLOB");
        TYPE_MAPPING.put("image", "BLOB");
        TYPE_MAPPING.put("longvarbinary", "BLOB");

        // 布尔类型
        TYPE_MAPPING.put("boolean", "BIT");
        TYPE_MAPPING.put("bit", "BIT");

        // 默认长度映射
        DEFAULT_LENGTH_MAPPING.put("VARCHAR", 255);
        DEFAULT_LENGTH_MAPPING.put("CHAR", 1);
        DEFAULT_LENGTH_MAPPING.put("NVARCHAR", 255);
        DEFAULT_LENGTH_MAPPING.put("NCHAR", 1);
        DEFAULT_LENGTH_MAPPING.put("BINARY", 1);
        DEFAULT_LENGTH_MAPPING.put("VARBINARY", 255);
        DEFAULT_LENGTH_MAPPING.put("DECIMAL", 18);
        DEFAULT_LENGTH_MAPPING.put("NUMBER", 18);
        DEFAULT_LENGTH_MAPPING.put("NUMERIC", 18);

        // MySQL类型映射
        Map<String, String> mysqlMapping = new HashMap<>();
        mysqlMapping.put("VARCHAR", "VARCHAR");
        mysqlMapping.put("CHAR", "CHAR");
        mysqlMapping.put("TEXT", "TEXT");
        mysqlMapping.put("TINYINT", "TINYINT");
        mysqlMapping.put("SMALLINT", "SMALLINT");
        mysqlMapping.put("INT", "INTEGER");
        mysqlMapping.put("BIGINT", "BIGINT");
        mysqlMapping.put("FLOAT", "FLOAT");
        mysqlMapping.put("DOUBLE", "DOUBLE");
        mysqlMapping.put("DECIMAL", "DECIMAL");
        mysqlMapping.put("DATE", "DATE");
        mysqlMapping.put("TIME", "TIME");
        mysqlMapping.put("DATETIME", "TIMESTAMP");
        mysqlMapping.put("TIMESTAMP", "TIMESTAMP");
        mysqlMapping.put("BLOB", "BLOB");
        mysqlMapping.put("LONGBLOB", "BLOB");
        COMMON_TYPE_MAPPING.put("mysql", mysqlMapping);

        // Oracle类型映射
        Map<String, String> oracleMapping = new HashMap<>();
        oracleMapping.put("VARCHAR2", "VARCHAR");
        oracleMapping.put("NVARCHAR2", "NVARCHAR");
        oracleMapping.put("CHAR", "CHAR");
        oracleMapping.put("NCHAR", "NCHAR");
        oracleMapping.put("NUMBER", "NUMBER");
        oracleMapping.put("FLOAT", "FLOAT");
        oracleMapping.put("DATE", "DATE");
        oracleMapping.put("TIMESTAMP", "TIMESTAMP");
        oracleMapping.put("CLOB", "CLOB");
        oracleMapping.put("NCLOB", "NCLOB");
        oracleMapping.put("BLOB", "BLOB");
        oracleMapping.put("RAW", "BINARY");
        oracleMapping.put("LONG RAW", "BLOB");
        COMMON_TYPE_MAPPING.put("oracle", oracleMapping);

        // PostgreSQL类型映射
        Map<String, String> postgresMapping = new HashMap<>();
        postgresMapping.put("VARCHAR", "VARCHAR");
        postgresMapping.put("CHAR", "CHAR");
        postgresMapping.put("TEXT", "TEXT");
        postgresMapping.put("SMALLINT", "SMALLINT");
        postgresMapping.put("INTEGER", "INTEGER");
        postgresMapping.put("BIGINT", "BIGINT");
        postgresMapping.put("REAL", "FLOAT");
        postgresMapping.put("DOUBLE PRECISION", "DOUBLE");
        postgresMapping.put("NUMERIC", "NUMBER");
        postgresMapping.put("BOOLEAN", "BIT");
        postgresMapping.put("TIMESTAMP", "TIMESTAMP");
        postgresMapping.put("DATE", "DATE");
        postgresMapping.put("TIME", "TIME");
        postgresMapping.put("BYTEA", "BLOB");
        COMMON_TYPE_MAPPING.put("postgresql", postgresMapping);

        // SQL Server类型映射
        Map<String, String> sqlserverMapping = new HashMap<>();
        sqlserverMapping.put("VARCHAR", "VARCHAR");
        sqlserverMapping.put("CHAR", "CHAR");
        sqlserverMapping.put("TEXT", "TEXT");
        sqlserverMapping.put("NTEXT", "NTEXT");
        sqlserverMapping.put("NVARCHAR", "NVARCHAR");
        sqlserverMapping.put("NCHAR", "NCHAR");
        sqlserverMapping.put("SMALLINT", "SMALLINT");
        sqlserverMapping.put("INT", "INTEGER");
        sqlserverMapping.put("BIGINT", "BIGINT");
        sqlserverMapping.put("REAL", "FLOAT");
        sqlserverMapping.put("FLOAT", "DOUBLE");
        sqlserverMapping.put("DECIMAL", "DECIMAL");
        sqlserverMapping.put("DATETIME", "TIMESTAMP");
        sqlserverMapping.put("DATETIME2", "TIMESTAMP");
        sqlserverMapping.put("DATE", "DATE");
        sqlserverMapping.put("TIME", "TIME");
        sqlserverMapping.put("BINARY", "BINARY");
        sqlserverMapping.put("VARBINARY", "VARBINARY");
        sqlserverMapping.put("IMAGE", "BLOB");
        sqlserverMapping.put("BIT", "BIT");
        COMMON_TYPE_MAPPING.put("sqlserver", sqlserverMapping);

        // DB2类型映射
        Map<String, String> db2Mapping = new HashMap<>();
        db2Mapping.put("VARCHAR", "VARCHAR");
        db2Mapping.put("CHAR", "CHAR");
        db2Mapping.put("CLOB", "CLOB");
        db2Mapping.put("SMALLINT", "SMALLINT");
        db2Mapping.put("INTEGER", "INTEGER");
        db2Mapping.put("BIGINT", "BIGINT");
        db2Mapping.put("REAL", "FLOAT");
        db2Mapping.put("DOUBLE", "DOUBLE");
        db2Mapping.put("DECIMAL", "DECIMAL");
        db2Mapping.put("NUMERIC", "NUMBER");
        db2Mapping.put("DATE", "DATE");
        db2Mapping.put("TIME", "TIME");
        db2Mapping.put("TIMESTAMP", "TIMESTAMP");
        db2Mapping.put("BLOB", "BLOB");
        db2Mapping.put("CHAR FOR BIT DATA", "BINARY");
        db2Mapping.put("VARCHAR FOR BIT DATA", "VARBINARY");
        COMMON_TYPE_MAPPING.put("db2", db2Mapping);

        // MariaDB类型映射
        Map<String, String> mariadbMapping = new HashMap<>();
        mariadbMapping.putAll(mysqlMapping); // MariaDB与MySQL类型基本相同
        COMMON_TYPE_MAPPING.put("mariadb", mariadbMapping);

        // H2类型映射
        Map<String, String> h2Mapping = new HashMap<>();
        h2Mapping.put("VARCHAR", "VARCHAR");
        h2Mapping.put("CHAR", "CHAR");
        h2Mapping.put("CLOB", "TEXT");
        h2Mapping.put("TINYINT", "TINYINT");
        h2Mapping.put("SMALLINT", "SMALLINT");
        h2Mapping.put("INT", "INTEGER");
        h2Mapping.put("BIGINT", "BIGINT");
        h2Mapping.put("REAL", "FLOAT");
        h2Mapping.put("DOUBLE", "DOUBLE");
        h2Mapping.put("DECIMAL", "DECIMAL");
        h2Mapping.put("BOOLEAN", "BIT");
        h2Mapping.put("DATE", "DATE");
        h2Mapping.put("TIME", "TIME");
        h2Mapping.put("TIMESTAMP", "TIMESTAMP");
        h2Mapping.put("BLOB", "BLOB");
        COMMON_TYPE_MAPPING.put("h2", h2Mapping);

        // DM8类型映射
        Map<String, String> dm8Mapping = new HashMap<>();
        dm8Mapping.putAll(TYPE_MAPPING); // DM8与DM7类型基本相同
        COMMON_TYPE_MAPPING.put("dm8", dm8Mapping);

        // GaussDB/OpenGauss类型映射
        Map<String, String> gaussMapping = new HashMap<>();
        gaussMapping.putAll(postgresMapping); // 基本兼容PostgreSQL
        COMMON_TYPE_MAPPING.put("gaussdb", gaussMapping);
        COMMON_TYPE_MAPPING.put("opengauss", gaussMapping);

        // 人大金仓类型映射
        Map<String, String> kingbaseMapping = new HashMap<>();
        kingbaseMapping.putAll(postgresMapping); // 基本兼容PostgreSQL
        COMMON_TYPE_MAPPING.put("kingbase", kingbaseMapping);

        // 神通数据库类型映射
        Map<String, String> shentongMapping = new HashMap<>();
        shentongMapping.putAll(postgresMapping);
        COMMON_TYPE_MAPPING.put("shentong", shentongMapping);

        // 瀚高数据库类型映射
        Map<String, String> highgoMapping = new HashMap<>();
        highgoMapping.putAll(postgresMapping);
        COMMON_TYPE_MAPPING.put("highgo", highgoMapping);
    }

    public DM7Handler(DataSource dataSource) {
        super(dataSource);
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
    public String getSchema() {
        return dataSource.getDbName();
    }

    @Override
    public void setSchema(Connection conn, String schema) throws SQLException {
        if (schema != null && !schema.isEmpty()) {
            conn.setSchema(schema.toUpperCase());
        }
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
    public String getDefaultSchema() {
        return "PUBLIC";
    }

    @Override
    public String getValidationQuery() {
        return "SELECT 1 FROM DUAL";
    }

    @Override
    public String getQuoteString() {
        return "\"";
    }

    @Override
    public List<String> getSystemDatabases() {
        return Arrays.asList("SYSDBA", "SYSAUDITOR", "SYSSSO");
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
            // 达梦日期格式
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return "TO_DATE('" + sdf.format((java.sql.Date)value) + "', 'YYYY-MM-DD')";
        }
        if (value instanceof Timestamp || value instanceof Date) {
            // 达梦时间戳格式
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return "TO_DATE('" + sdf.format(value) + "', 'YYYY-MM-DD HH24:MI:SS')";
        }
        // 字符串值需要单引号转义
        return "'" + value.toString().replace("'", "''") + "'";
    }

    @Override
    public String wrapIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public String generatePageSql(String sql, int offset, int limit) {
        // 达梦数据库使用 LIMIT OFFSET 语法
        return sql + " LIMIT " + limit + " OFFSET " + offset;
    }

    /**
     * 生成达梦数据库的LIMIT子句
     *
     * @param offset 起始位置
     * @param limit  返回记录的最大数量
     * @return 格式化后的LIMIT子句
     */
    @Override
    public String getLimitClause(long offset, int limit) {
        // 达梦数据库使用 LIMIT OFFSET 语法
        return "LIMIT " + limit + " OFFSET " + offset;
    }

    @Override
    public String generateCountSql(String sql) {
        return "SELECT COUNT(*) FROM (" + sql + ")";
    }

    @Override
    public boolean supportsBatchUpdates() {
        return true;
    }

    @Override
    public boolean supportsGetGeneratedKeys() {
        return true;
    }

    @Override
    public boolean supportsTransactions() {
        return true;
    }

    @Override
    public String getDatabaseProductName() {
        return "DM7";
    }

    @Override
    public String getTableExistsSql(String tableName) {
        // 达梦数据库通过系统表检查表是否存在
        return String.format(
            "SELECT COUNT(*) FROM ALL_TABLES WHERE TABLE_NAME = '%s' AND OWNER = USER",
                tableName.toUpperCase()
        );
    }

    @Override
    public String getCreateTableSql(String tableName, List<ColumnDefinition> columns,
                                    String tableComment, String engine, String charset, String collate) {
        StringBuilder sb = new StringBuilder();

        // 1. 创建表
        sb.append("CREATE TABLE ").append(escapeIdentifier(tableName)).append(" (\n");

        // 添加列定义
        List<String> columnDefs = new ArrayList<>();
        List<String> pkColumns = new ArrayList<>();
        
        for (ColumnDefinition column : columns) {
            // 调整列类型以适应DM7数据库
            adjustDM7ColumnType(column);
            
            // 生成字段定义 (注意：这里不包含注释，因为DM数据库不支持在CREATE TABLE中添加列注释)
            String fieldDef = formatFieldDefinition(
                    column.getName(),
                    column.getType(),
                    column.getLength(),
                    column.getPrecision(),
                    column.getScale(),
                    column.isNullable(),
                    column.getDefaultValue(),
                null // 注释在后续单独添加
            );
            
            columnDefs.add(fieldDef);
            
            // 收集主键列
            if (column.isPrimaryKey()) {
                pkColumns.add(escapeIdentifier(column.getName()));
            }
        }
        
        // 添加主键约束
        if (!pkColumns.isEmpty()) {
            columnDefs.add("PRIMARY KEY (" + String.join(", ", pkColumns) + ")");
        }
        
        sb.append(String.join(",\n", columnDefs)).append("\n)");
        
        return sb.toString();
    }

    /**
     * 调整列类型以适应DM7数据库
     */
    public void adjustDM7ColumnType(ColumnDefinition column) {
        String type = column.getType().toUpperCase();
        
        // 转换常见类型为DM7支持的类型
        switch (type) {
            case "INT":
            case "INTEGER":
                column.setType("INTEGER");
                column.setLength(null); // 整数类型不需要长度
                break;
            case "BIGINT":
                column.setType("BIGINT");
                column.setLength(null);
                break;
            case "SMALLINT":
                column.setType("SMALLINT");
                column.setLength(null);
                break;
            case "TINYINT":
                column.setType("SMALLINT"); // DM7没有TINYINT，转为SMALLINT
                column.setLength(null);
                break;
            case "DOUBLE":
            case "FLOAT":
                column.setType(type);
                column.setLength(null);
                break;
            case "TIMESTAMP":
                column.setType("TIMESTAMP");
                column.setLength(null);
                break;
            case "DATE":
                column.setType("DATE");
                column.setLength(null);
                break;
            case "TEXT":
            case "LONGTEXT":
            case "MEDIUMTEXT":
                column.setType("CLOB");
                column.setLength(null);
                break;
            case "BLOB":
            case "LONGBLOB":
            case "MEDIUMBLOB":
                column.setType("BLOB");
                column.setLength(null);
                break;
            case "VARCHAR":
            case "VARCHAR2":
                // 确保VARCHAR类型有合理的长度
                if (column.getLength() == null || column.getLength() <= 0) {
                    column.setLength(getDefaultVarcharLength());
                } else if (column.getLength() > getMaxVarcharLength()) {
                    column.setLength(getMaxVarcharLength());
                }
                break;
            case "CHAR":
                // 确保CHAR类型有合理的长度
                if (column.getLength() == null || column.getLength() <= 0) {
                    column.setLength(1);
                } else if (column.getLength() > 2000) {
                    column.setLength(2000);
                }
                break;
            case "DECIMAL":
            case "NUMERIC":
            case "NUMBER":
                // 确保数值类型有合理的精度和标度
                if (column.getPrecision() == null || column.getPrecision() <= 0) {
                    column.setPrecision(10);
                }
                if (column.getScale() == null) {
                    column.setScale(0);
                }
                break;
            case "BOOLEAN":
            case "BOOL":
                column.setType("NUMBER");
                column.setPrecision(1);
                column.setScale(0);
                break;
        }
        
        // 处理自增列的特殊情况
        if (column.isAutoIncrement()) {
            // 达梦数据库使用序列和触发器实现自增，这里只转换类型
            if (type.equals("INT") || type.equals("INTEGER")) {
                column.setType("INTEGER");
                column.setLength(null);
            } else if (type.equals("BIGINT")) {
                column.setType("BIGINT");
                column.setLength(null);
            } else {
                column.setType("INTEGER");
                column.setLength(null);
            }
            // 自增功能需要在创建表后通过序列和触发器实现
        }
    }

    @Override
    public String getDropTableSql(String tableName) {
        return "DROP TABLE " + wrapIdentifier(tableName);
    }

    @Override
    public String getTruncateTableSql(String tableName) {
        return "TRUNCATE TABLE " + wrapIdentifier(tableName);
    }

    @Override
    public String getAddColumnSql(String tableName, String columnDefinition) {
        return "ALTER TABLE " + wrapIdentifier(tableName) + " ADD " + columnDefinition;
    }

    @Override
    public String getAddColumnSql(String tableName, ColumnDefinition column, String afterColumn) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ")
           .append(wrapIdentifier(tableName))
           .append(" ADD (")
           .append(wrapIdentifier(column.getName()))
           .append(" ");
        
        // 处理类型定义
        if (column.getLength() != null && isValidFieldLength(column.getType(), column.getLength())) {
            if (column.getScale() != null) {
                // Decimal 类型
                sql.append(column.getType())
                   .append("(")
                        .append(column.getPrecision() != null ? column.getPrecision() : column.getLength())
                        .append(",")
                        .append(column.getScale())
                        .append(")");
            } else {
                // 带长度的类型
                sql.append(column.getType())
                   .append("(")
                   .append(column.getLength())
                   .append(")");
            }
        } else {
            sql.append(column.getType());
        }
        
        // 处理可空性
        if (!column.isNullable()) {
            sql.append(" NOT NULL");
        }

        // 处理默认值
        if (column.getDefaultValue() != null && !column.getDefaultValue().isEmpty()) {
            sql.append(" DEFAULT ").append(column.getDefaultValue());
        }
        
        sql.append(")");
        
        // 达梦数据库不支持 AFTER 子句
        // 忽略 afterColumn 参数

        return sql.toString();
    }

    @Override
    public String getModifyColumnSql(String tableName, String columnName, String newDefinition) {
        // 达梦数据库的修改列语法
        return String.format(
            "ALTER TABLE %s MODIFY (%s %s)",
            wrapIdentifier(tableName),
            wrapIdentifier(columnName),
            newDefinition
        );
    }

    @Override
    public String getDropColumnSql(String tableName, String columnName) {
        // 达梦数据库删除列的语法
        return String.format(
            "ALTER TABLE %s DROP COLUMN %s",
            wrapIdentifier(tableName),
            wrapIdentifier(columnName)
        );
    }

    @Override
    public String getRenameTableSql(String oldTableName, String newTableName) {
        return "RENAME " + wrapIdentifier(oldTableName) + " TO " + wrapIdentifier(newTableName);
    }

    @Override
    public String getShowCreateTableSql(String tableName) {
        // 达梦数据库通过查询系统表获取表结构信息
        return String.format(
            "SELECT COLUMN_NAME, DATA_TYPE, DATA_LENGTH, DATA_PRECISION, DATA_SCALE, NULLABLE, DATA_DEFAULT, COMMENTS " +
            "FROM USER_TAB_COLUMNS WHERE TABLE_NAME = '%s' ORDER BY COLUMN_ID",
            tableName.toUpperCase()
        );
    }

    @Override
    public String getShowTablesSql() {
        // 达梦数据库查询当前用户的所有表
        return "SELECT TABLE_NAME FROM USER_TABLES ORDER BY TABLE_NAME";
    }

    @Override
    public String getShowColumnsSql(String tableName) {
        // 达梦数据库查询表的列信息
        return String.format(
            "SELECT COLUMN_NAME AS \"COLUMN_NAME\", " +
            "DATA_TYPE AS \"TYPE_NAME\", " +
            "DATA_LENGTH AS \"COLUMN_SIZE\", " +
            "DATA_PRECISION AS \"PRECISION\", " +
            "DATA_SCALE AS \"SCALE\", " +
            "DECODE(NULLABLE, 'Y', 1, 0) AS \"NULLABLE\", " +
            "DATA_DEFAULT AS \"COLUMN_DEF\", " +
            "COMMENTS AS \"REMARKS\" " +
            "FROM USER_TAB_COLUMNS WHERE TABLE_NAME = '%s' ORDER BY COLUMN_ID",
            tableName.toUpperCase()
        );
    }

    @Override
    public String getAddTableCommentSql(String tableName, String comment) {
        // 达梦数据库添加表注释
        return String.format(
            "COMMENT ON TABLE %s IS %s",
            wrapIdentifier(tableName),
            wrapValue(comment)
        );
    }

    @Override
    public String getModifyTableCommentSql(String tableName, String comment) {
        // 达梦数据库修改表注释（与添加语法相同）
        return getAddTableCommentSql(tableName, comment);
    }

    @Override
    public String getAddColumnCommentSql(String tableName, String columnName, String comment) {
        // 达梦数据库添加列注释
        return String.format(
            "COMMENT ON COLUMN %s.%s IS %s",
            wrapIdentifier(tableName),
            wrapIdentifier(columnName),
            wrapValue(comment)
        );
    }

    @Override
    public String getModifyColumnCommentSql(String tableName, String columnName, String comment) {
        // 达梦数据库修改列注释（与添加语法相同）
        return getAddColumnCommentSql(tableName, columnName, comment);
    }

    @Override
    public String getShowIndexesSql(String tableName) {
        // 达梦数据库查询索引信息
        return String.format(
            "SELECT UI.INDEX_NAME, " +
            "DECODE(UI.UNIQUENESS, 'UNIQUE', 'UNIQUE', 'NON-UNIQUE') AS TYPE, " +
            "UIC.COLUMN_NAME, UIC.COLUMN_POSITION " +
            "FROM USER_INDEXES UI " +
            "JOIN USER_IND_COLUMNS UIC ON UI.INDEX_NAME = UIC.INDEX_NAME " +
            "WHERE UI.TABLE_NAME = '%s' " +
            "ORDER BY UI.INDEX_NAME, UIC.COLUMN_POSITION",
            tableName.toUpperCase()
        );
    }

    @Override
    public String getAddIndexSql(String tableName, String indexName, List<String> columns, boolean unique) {
        StringBuilder columnStr = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                columnStr.append(", ");
            }
            columnStr.append(wrapIdentifier(columns.get(i)));
        }
        
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE ");
        if (unique) {
            sql.append("UNIQUE ");
        }
        sql.append("INDEX ")
           .append(wrapIdentifier(indexName))
           .append(" ON ")
           .append(wrapIdentifier(tableName))
           .append(" (")
           .append(columnStr)
           .append(")");
        
        return sql.toString();
    }

    @Override
    public String getDropIndexSql(String tableName, String indexName) {
        return "DROP INDEX " + wrapIdentifier(indexName);
    }

    @Override
    public String getAlterTableEngineSql(String tableName, String engine) {
        // DM7 不支持修改存储引擎
        return "";
    }

    @Override
    public String getAlterTableCharsetSql(String tableName, String charset, String collate) {
        // DM7 不支持修改表字符集
        return "";
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
    public String getDefaultTextType() {
        return "TEXT";
    }

    @Override
    public String getDefaultIntegerType() {
        return "INTEGER";
    }

    @Override
    public String getDefaultDecimalType() {
        return "NUMBER(10,2)";
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
        return "NUMBER(1)";
    }

    @Override
    public String getDefaultBlobType() {
        return "BLOB";
    }

    @Override
    public String mapJavaTypeToDbType(Class<?> javaType) {
        if (String.class.equals(javaType)) {
            return "VARCHAR2(255)";
        }
        if (Integer.class.equals(javaType) || int.class.equals(javaType)) {
            return "INTEGER";
        }
        if (Long.class.equals(javaType) || long.class.equals(javaType)) {
            return "NUMBER(19)";
        }
        if (Double.class.equals(javaType) || double.class.equals(javaType)) {
            return "NUMBER(19,4)";
        }
        if (Float.class.equals(javaType) || float.class.equals(javaType)) {
            return "NUMBER(10,2)";
        }
        if (BigDecimal.class.equals(javaType)) {
            return "NUMBER(19,4)";
        }
        if (Boolean.class.equals(javaType) || boolean.class.equals(javaType)) {
            return "NUMBER(1)";
        }
        if (java.sql.Date.class.equals(javaType)) {
            return "DATE";
        }
        if (Time.class.equals(javaType)) {
            return "DATE";
        }
        if (Timestamp.class.equals(javaType) || Date.class.equals(javaType)) {
            return "TIMESTAMP";
        }
        if (byte[].class.equals(javaType)) {
            return "BLOB";
        }
        return "VARCHAR2(255)";
    }

    @Override
    public Class<?> mapDbTypeToJavaType(String dbType) {
        if (dbType == null) {
            return String.class;
        }
        
        dbType = dbType.toUpperCase();
        
        if (dbType.contains("CHAR") || dbType.contains("TEXT") || dbType.equals("CLOB")) {
            return String.class;
        }
        if (dbType.equals("INTEGER") || dbType.equals("INT")) {
            return Integer.class;
        }
        if (dbType.equals("BIGINT")) {
            return Long.class;
        }
        if (dbType.equals("NUMBER")) {
            // 对于 NUMBER 类型，可能需要根据精度和小数位数判断对应的 Java 类型
            return BigDecimal.class;
        }
        if (dbType.equals("FLOAT") || dbType.equals("REAL")) {
            return Float.class;
        }
        if (dbType.equals("DOUBLE") || dbType.equals("DOUBLE PRECISION")) {
            return Double.class;
        }
        if (dbType.equals("DATE")) {
            return java.sql.Date.class;
        }
        if (dbType.equals("TIME")) {
            return Time.class;
        }
        if (dbType.equals("TIMESTAMP")) {
            return Timestamp.class;
        }
        if (dbType.equals("BLOB") || dbType.equals("IMAGE")) {
            return byte[].class;
        }
        if (dbType.equals("BIT") || dbType.equals("BOOLEAN")) {
            return Boolean.class;
        }
        
        return String.class;
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
    public String getCreateIndexSql(String tableName, String indexName, String[] columns) {
        StringBuilder columnStr = new StringBuilder();
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                columnStr.append(", ");
            }
            columnStr.append(wrapIdentifier(columns[i]));
        }
        
        return String.format(
            "CREATE INDEX %s ON %s (%s)",
                wrapIdentifier(indexName),
                wrapIdentifier(tableName),
            columnStr.toString()
        );
    }

    @Override
    public Map<String, Integer> getDefaultLengthMapping() {
        Map<String, Integer> lengthMapping = new HashMap<>();
        lengthMapping.put("VARCHAR", DEFAULT_VARCHAR_LENGTH);
        lengthMapping.put("CHAR", 1);
        lengthMapping.put("NUMBER", 10);
        lengthMapping.put("DECIMAL", 10);
        return lengthMapping;
    }

    @Override
    public boolean isValidFieldLength(String type, int length) {
        // 规范化类型名称为大写
        String upperType = type.toUpperCase();
        
        // 以下类型不需要指定长度
        if (upperType.equals("INTEGER") || upperType.equals("INT") || 
            upperType.equals("BIGINT") || upperType.equals("SMALLINT") ||
            upperType.equals("DOUBLE") || upperType.equals("FLOAT") ||
            upperType.equals("DATE") || upperType.equals("TIMESTAMP") ||
            upperType.equals("BOOLEAN") || upperType.equals("CLOB") ||
            upperType.equals("BLOB") || upperType.equals("LONGVARCHAR") ||
            upperType.equals("REAL") || upperType.equals("TIME")) {
            return false;
        }
        
        // VARCHAR类型有长度限制
        if (upperType.equals("VARCHAR") || upperType.equals("VARCHAR2")) {
            return length > 0 && length <= getMaxVarcharLength();
        }
        
        // CHAR类型有长度限制
        if (upperType.equals("CHAR")) {
            return length > 0 && length <= 2000;
        }
        
        // 数值类型的精度和标度检查
        if (upperType.equals("NUMBER") || upperType.equals("DECIMAL") || upperType.equals("NUMERIC")) {
            return length >= 1 && length <= 38;
        }
        
        // 对于其他类型，只要长度是正数就接受
        return length > 0;
    }

    @Override
    public String convertFromOtherDbType(String sourceType, String sourceDbType) {
        // 如果源类型为空，返回默认类型
        if (sourceType == null || sourceType.trim().isEmpty()) {
            return "VARCHAR";
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
                if (sourceType.contains("(") && !mappedType.equals("CLOB") && !mappedType.equals("BLOB")) {
                    String lengthPart = sourceType.substring(sourceType.indexOf("("));
                    return mappedType + lengthPart;
                }
                return mappedType;
            }
        }

        // 特殊类型处理
        if (sourceType.contains("CHAR")) {
            return sourceType.contains("VAR") ? "VARCHAR" : "CHAR";
        }
        if (sourceType.contains("TEXT")) {
            return "CLOB";
        }
        if (sourceType.contains("INT")) {
            if (sourceType.startsWith("TINY") || sourceType.startsWith("SMALL")) return "SMALLINT";
            if (sourceType.startsWith("BIG")) return "NUMBER(19)";
            return "INTEGER";
        }
        if (sourceType.contains("FLOAT") || sourceType.contains("REAL")) {
            return "FLOAT";
        }
        if (sourceType.contains("DOUBLE")) {
            return "DOUBLE";
        }
        if (sourceType.contains("DECIMAL") || sourceType.contains("NUMERIC")) {
            return "NUMBER(10,2)";
        }
        if (sourceType.contains("BOOL")) {
            return "NUMBER(1)";
        }
        if (sourceType.contains("DATE")) {
            return "DATE";
        }
        if (sourceType.contains("TIME")) {
            if (sourceType.contains("TIMESTAMP")) return "TIMESTAMP";
            return "TIME";
        }
        if (sourceType.contains("BLOB") || sourceType.contains("BINARY")) {
            return "BLOB";
        }

        // 如果没有找到映射，返回默认类型
        return "VARCHAR";
    }

    @Override
    public String formatFieldDefinition(String fieldName, String fieldType,
                                        Integer length, Integer precision, Integer scale,
                                        boolean nullable, String defaultValue, String comment) {
        StringBuilder sb = new StringBuilder();
        
        // 字段名
        sb.append(escapeIdentifier(fieldName)).append(" ");
        
        // 规范化类型名称为大写
        String upperType = fieldType.toUpperCase();
        
        // 根据类型处理长度、精度和标度
        if (upperType.equals("NUMBER") || upperType.equals("DECIMAL") || upperType.equals("NUMERIC")) {
            sb.append(upperType);
            if (precision != null && precision > 0) {
                sb.append("(").append(precision);
                if (scale != null && scale >= 0) {
                    sb.append(",").append(scale);
                }
                sb.append(")");
            }
        } else if (upperType.equals("VARCHAR") || upperType.equals("VARCHAR2") || upperType.equals("CHAR")) {
            sb.append(upperType);
            // 字符类型需要长度
            int actualLength = (length != null && length > 0) ? length : 
                              (upperType.equals("CHAR") ? 1 : getDefaultVarcharLength());
            if (upperType.equals("VARCHAR") || upperType.equals("VARCHAR2")) {
                actualLength = Math.min(actualLength, getMaxVarcharLength());
            } else if (upperType.equals("CHAR")) {
                actualLength = Math.min(actualLength, 2000);
            }
            sb.append("(").append(actualLength).append(")");
        } else if (!isValidFieldLength(upperType, 1)) {
            // 这些类型不需要长度规范
            sb.append(upperType);
        } else {
            // 其他可能需要长度的类型，如果提供了有效长度则使用它
            sb.append(upperType);
            if (length != null && length > 0) {
                sb.append("(").append(length).append(")");
            }
        }
        
        // 非空约束
        if (!nullable) {
            sb.append(" NOT NULL");
        }
        
        // 默认值
        if (defaultValue != null && !defaultValue.isEmpty()) {
                // 处理日期时间类型的默认值
            if ((upperType.contains("DATE") || upperType.contains("TIMESTAMP") || upperType.contains("TIME")) && 
                !defaultValue.toUpperCase().equals("SYSDATE") && 
                !defaultValue.toUpperCase().contains("TO_DATE")) {
                
                if (defaultValue.toUpperCase().equals("CURRENT_TIMESTAMP") || 
                    defaultValue.toUpperCase().contains("NOW()")) {
                    sb.append(" DEFAULT SYSDATE");
                } else if (defaultValue.startsWith("'") && defaultValue.endsWith("'")) {
                    sb.append(" DEFAULT TO_DATE(").append(defaultValue).append(", 'YYYY-MM-DD HH24:MI:SS')");
                } else {
                    sb.append(" DEFAULT ").append(defaultValue);
                }
                } else {
                sb.append(" DEFAULT ").append(defaultValue);
            }
        }
        
        // 注意：不在这里添加注释，DM数据库要求通过单独的COMMENT ON COLUMN语句添加注释
        
        return sb.toString();
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

                adjustDM7ColumnType(column);
            }

            // 生成DM7建表语句
            return generateCreateTableSql(tableDefinition);
        } catch (Exception e) {
            log.error("Failed to convert CREATE TABLE SQL from {} to DM7: {}", sourceDbType, e.getMessage());
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
            Pattern tablePattern = Pattern.compile("CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([\\w\\.\"]+)\\s*\\(",
                    Pattern.CASE_INSENSITIVE);
            Matcher tableMatcher = tablePattern.matcher(createTableSql);
            if (tableMatcher.find()) {
                String tableName = tableMatcher.group(1).replace("\"", "");
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
                            String columnName = pkColumn.trim().replace("\"", "");
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

                // 处理唯一约束
                if (definition.toUpperCase().startsWith("UNIQUE")) {
                    Pattern uniquePattern = Pattern.compile("UNIQUE\\s*\\(([^)]+)\\)",
                            Pattern.CASE_INSENSITIVE);
                    Matcher uniqueMatcher = uniquePattern.matcher(definition);
                    if (uniqueMatcher.find()) {
                        String[] uniqueColumns = uniqueMatcher.group(1).split(",");
                        IndexDefinition index = new IndexDefinition();
                        index.setUnique(true);
                        index.setColumns(Arrays.asList(uniqueColumns));
                        indexes.add(index);
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

            // 解析表空间
            Pattern tablespacePattern = Pattern.compile("TABLESPACE\\s+(\\w+)",
                    Pattern.CASE_INSENSITIVE);
            Matcher tablespaceMatcher = tablespacePattern.matcher(createTableSql);
            if (tablespaceMatcher.find()) {
                Map<String, String> extraProps = new HashMap<>();
                extraProps.put("TABLESPACE", tablespaceMatcher.group(1));
                tableDefinition.setExtraProperties(extraProps);
            }

            // 解析存储参数
            Pattern storagePattern = Pattern.compile("STORAGE\\s*\\(([^)]+)\\)",
                    Pattern.CASE_INSENSITIVE);
            Matcher storageMatcher = storagePattern.matcher(createTableSql);
            if (storageMatcher.find()) {
                String storageParams = storageMatcher.group(1);
                Map<String, String> extraProps = tableDefinition.getExtraProperties();
                if (extraProps == null) {
                    extraProps = new HashMap<>();
                    tableDefinition.setExtraProperties(extraProps);
                }

                Pattern paramPattern = Pattern.compile("(\\w+)\\s+(\\w+)");
                Matcher paramMatcher = paramPattern.matcher(storageParams);
                while (paramMatcher.find()) {
                    extraProps.put(paramMatcher.group(1), paramMatcher.group(2));
                }
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
            } else if (c == '\'' && (i == 0 || chars[i - 1] != '\\')) {
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
                "\"?([\\w]+)\"?\\s+" +                    // 列名
                        "([\\w\\(\\),]+)" +                       // 数据类型
                        "(?:\\s+DEFAULT\\s+([^\\s,]+))?" +       // 默认值（可选）
                        "(?:\\s+NOT\\s+NULL)?" +                 // NOT NULL（可选）
                        "(?:\\s+IDENTITY(?:\\s*\\([^)]+\\))?)?" + // 自增（可选）
                        "(?:\\s+PRIMARY\\s+KEY)?",               // 主键（可选）
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

        // 设置是否自增
        column.setAutoIncrement(definition.toUpperCase().contains("IDENTITY"));

        // 设置是否主键
        column.setPrimaryKey(definition.toUpperCase().contains("PRIMARY KEY"));

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
            // DM7特定类型映射
            switch (type) {
                case "VARCHAR":
                    type = "VARCHAR2";
                    break;
                case "TEXT":
                    type = "CLOB";
                    break;
                case "BLOB":
                    type = "BLOB";
                    break;
                case "BOOLEAN":
                    type = "NUMBER(1)";
                    break;
                case "TIMESTAMP":
                    type = "TIMESTAMP(6)";
                    break;
            }
            columnSql.append(type);

            // 添加长度/精度
            if (column.getLength() != null) {
                if (type.equals("NUMBER")) {
                    if (column.getScale() != null) {
                        columnSql.append("(").append(column.getPrecision())
                                .append(",").append(column.getScale()).append(")");
                    } else {
                        columnSql.append("(").append(column.getPrecision()).append(")");
                    }
                } else if (!type.equals("CLOB") && !type.equals("BLOB") &&
                        !type.equals("DATE") && !type.startsWith("TIMESTAMP")) {
                    columnSql.append("(").append(column.getLength()).append(")");
                }
            }

            // 添加NOT NULL约束
            if (!column.isNullable()) {
                columnSql.append(" NOT NULL");
            }

            // 添加默认值
            if (column.getDefaultValue() != null) {
                columnSql.append(" DEFAULT ").append(column.getDefaultValue());
            }

            // 处理自增列
            if (column.isAutoIncrement()) {
                columnSql.append(" IDENTITY");
            }

            // 收集主键列
            if (column.isPrimaryKey()) {
                primaryKeys.add(wrapIdentifier(column.getName()));
            }

            columnDefinitions.add(columnSql.toString());
        }

        // 添加主键约束
        if (!primaryKeys.isEmpty()) {
            columnDefinitions.add("PRIMARY KEY (" + String.join(", ", primaryKeys) + ")");
        }

        sql.append(String.join(",\n", columnDefinitions));
        sql.append("\n)");

        // 添加表空间
        Map<String, String> extraProps = tableDefinition.getExtraProperties();
        if (extraProps != null) {
            if (extraProps.containsKey("TABLESPACE")) {
                sql.append("\nTABLESPACE ").append(extraProps.get("TABLESPACE"));
            }

            // 添加存储参数
            List<String> storageParams = new ArrayList<>();
            if (extraProps.containsKey("INITIAL")) {
                storageParams.add("INITIAL " + extraProps.get("INITIAL"));
            }
            if (extraProps.containsKey("NEXT")) {
                storageParams.add("NEXT " + extraProps.get("NEXT"));
            }
            if (extraProps.containsKey("MINEXTENTS")) {
                storageParams.add("MINEXTENTS " + extraProps.get("MINEXTENTS"));
            }
            if (extraProps.containsKey("MAXEXTENTS")) {
                storageParams.add("MAXEXTENTS " + extraProps.get("MAXEXTENTS"));
            }
            if (!storageParams.isEmpty()) {
                sql.append("\nSTORAGE (")
                        .append(String.join(" ", storageParams))
                        .append(")");
            }
        }

        return sql.toString();
    }

    @Override
    public List<TableDefinition> getAllTables(String database) throws Exception {
        String sql = "SELECT TABLE_NAME, " +
                "'TABLE' as TABLE_TYPE, " +
                "COMMENTS as TABLE_COMMENT " +
                "FROM USER_TAB_COMMENTS " +
                "WHERE TABLE_TYPE = 'TABLE'";

        try (Connection conn = getConnection()) {
            List<TableDefinition> tables = new ArrayList<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    TableDefinition table = TableDefinition.builder()
                        .tableName(rs.getString("TABLE_NAME"))
                        .tableType(rs.getString("TABLE_TYPE"))
                        .tableComment(rs.getString("TABLE_COMMENT"))
                        .build();

                    // 存储额外信息
                    Map<String, String> extraProperties = new HashMap<>();
                    extraProperties.put("tableType", rs.getString("TABLE_TYPE"));
                    table.setExtraProperties(extraProperties);
                    
                    tables.add(table);
                }
            }
            return tables;
        }
    }

    @Override
    public List<ColumnDefinition> getTableColumns(String database, String tableName) throws Exception {
        String sql = "SELECT A.COLUMN_NAME, " +
                "A.DATA_TYPE as COLUMN_TYPE, " +
                "A.DATA_LENGTH as LENGTH, " +
                "A.DATA_PRECISION, " +
                "A.DATA_SCALE, " +
                "A.NULLABLE as IS_NULLABLE, " +
                "A.DATA_DEFAULT as COLUMN_DEFAULT, " +
                "B.COMMENTS as COLUMN_COMMENT, " +
                "DECODE(C.COLUMN_NAME, NULL, 0, 1) as IS_PRIMARY_KEY " +
                "FROM USER_TAB_COLUMNS A " +
                "LEFT JOIN USER_COL_COMMENTS B ON A.TABLE_NAME = B.TABLE_NAME " +
                "AND A.COLUMN_NAME = B.COLUMN_NAME " +
                "LEFT JOIN USER_CONS_COLUMNS C ON A.TABLE_NAME = C.TABLE_NAME " +
                "AND A.COLUMN_NAME = C.COLUMN_NAME " +
                "AND C.CONSTRAINT_NAME IN (SELECT CONSTRAINT_NAME FROM USER_CONSTRAINTS " +
                "WHERE CONSTRAINT_TYPE = 'P' AND TABLE_NAME = ?) " +
                "WHERE A.TABLE_NAME = ? " +
                "ORDER BY A.COLUMN_ID";

        try (Connection conn = getConnection()) {
            List<ColumnDefinition> columns = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName.toUpperCase());
                stmt.setString(2, tableName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        // 获取基本字段信息
                        String type = rs.getString("COLUMN_TYPE");
                        int length = rs.getInt("LENGTH");
                        int precision = rs.getInt("DATA_PRECISION");
                        int scale = rs.getInt("DATA_SCALE");
                        String defaultValue = rs.getString("COLUMN_DEFAULT");
                        String comment = rs.getString("COLUMN_COMMENT");
                        boolean isNullable = "Y".equals(rs.getString("IS_NULLABLE"));
                        boolean isPrimaryKey = rs.getInt("IS_PRIMARY_KEY") == 1;

                        // 创建 ColumnDefinition 对象
                        ColumnDefinition column = ColumnDefinition.builder()
                            .name(rs.getString("COLUMN_NAME"))
                            .type(type)
                            .length(length > 0 ? length : null)
                            .precision(precision > 0 ? precision : null)
                            .scale(scale > 0 ? scale : null)
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
    public String getDatabaseType() {
        return "DM7";
    }

    @Override
    public String getDatabaseName() {
        return dataSource.getDbName();
    }

    @Override
    public List<String> listTables() throws Exception {
        List<String> tables = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT TABLE_NAME FROM USER_TABLES")) {
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
        Connection conn = null;
        
        try {
            conn = getConnection();
            
            // 基本表信息
            String basicInfoSql = "SELECT " +
                    "TABLE_NAME, " +
                    "OWNER, " +
                    "TABLESPACE_NAME, " +
                    "NUM_ROWS, " +
                    "BLOCKS, " +
                    "EMPTY_BLOCKS, " +
                    "AVG_SPACE, " +
                    "CHAIN_CNT, " +
                    "AVG_ROW_LEN, " +
                    "LAST_ANALYZED, " +
                    "COMPRESSION, " +
                    "COMPRESS_FOR " +
                    "FROM ALL_TABLES " +
                    "WHERE TABLE_NAME = ?";
            
            try (PreparedStatement pstmt = conn.prepareStatement(basicInfoSql)) {
                pstmt.setString(1, tableName.toUpperCase());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        tableInfo.put("name", rs.getString("TABLE_NAME"));
                        tableInfo.put("owner", rs.getString("OWNER"));
                        tableInfo.put("tablespace", rs.getString("TABLESPACE_NAME"));
                        tableInfo.put("blocks", rs.getLong("BLOCKS"));
                        tableInfo.put("emptyBlocks", rs.getLong("EMPTY_BLOCKS"));
                        tableInfo.put("avgSpace", rs.getLong("AVG_SPACE"));
                        tableInfo.put("chainCount", rs.getLong("CHAIN_CNT"));
                        tableInfo.put("avgRowLength", rs.getLong("AVG_ROW_LEN"));
                        tableInfo.put("lastAnalyzed", rs.getTimestamp("LAST_ANALYZED"));
                        tableInfo.put("compression", rs.getString("COMPRESSION"));
                        tableInfo.put("compressFor", rs.getString("COMPRESS_FOR"));
                    }
                }
            }
            
            // 表注释信息
            String commentSql = "SELECT COMMENTS FROM ALL_TAB_COMMENTS WHERE TABLE_NAME = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(commentSql)) {
                pstmt.setString(1, tableName.toUpperCase());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        tableInfo.put("comment", rs.getString("COMMENTS"));
                    }
                }
            }
            
            // 表的列数
            String columnCountSql = "SELECT COUNT(*) AS COLUMN_COUNT FROM ALL_TAB_COLUMNS WHERE TABLE_NAME = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(columnCountSql)) {
                pstmt.setString(1, tableName.toUpperCase());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        tableInfo.put("columnCount", rs.getInt("COLUMN_COUNT"));
                    }
                }
            }
            
            // 表的索引数
            String indexCountSql = "SELECT COUNT(*) AS INDEX_COUNT FROM ALL_INDEXES WHERE TABLE_NAME = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(indexCountSql)) {
                pstmt.setString(1, tableName.toUpperCase());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        tableInfo.put("indexCount", rs.getInt("INDEX_COUNT"));
                    }
                }
            }
            
            // 表的约束数
            String constraintCountSql = "SELECT COUNT(*) AS CONSTRAINT_COUNT FROM ALL_CONSTRAINTS WHERE TABLE_NAME = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(constraintCountSql)) {
                pstmt.setString(1, tableName.toUpperCase());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        tableInfo.put("constraintCount", rs.getInt("CONSTRAINT_COUNT"));
                    }
                }
            }
            
            // 表的触发器数
            String triggerCountSql = "SELECT COUNT(*) AS TRIGGER_COUNT FROM ALL_TRIGGERS WHERE TABLE_NAME = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(triggerCountSql)) {
                pstmt.setString(1, tableName.toUpperCase());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        tableInfo.put("triggerCount", rs.getInt("TRIGGER_COUNT"));
                    }
                }
            }
            
            // 表的分区信息
            String partitionCountSql = "SELECT COUNT(*) AS PARTITION_COUNT FROM ALL_TAB_PARTITIONS WHERE TABLE_NAME = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(partitionCountSql)) {
                pstmt.setString(1, tableName.toUpperCase());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        tableInfo.put("partitionCount", rs.getInt("PARTITION_COUNT"));
                    }
                }
            }
            
            // 表的存储信息
            String storageSql = "SELECT " +
                    "INITIAL_EXTENT, " +
                    "NEXT_EXTENT, " +
                    "MIN_EXTENTS, " +
                    "MAX_EXTENTS, " +
                    "PCT_INCREASE, " +
                    "FREELISTS, " +
                    "FREELIST_GROUPS, " +
                    "BUFFER_POOL " +
                    "FROM ALL_TABLES " +
                    "WHERE TABLE_NAME = ?";
            
            try (PreparedStatement pstmt = conn.prepareStatement(storageSql)) {
                pstmt.setString(1, tableName.toUpperCase());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        Map<String, Object> storageInfo = new HashMap<>();
                        storageInfo.put("initialExtent", rs.getLong("INITIAL_EXTENT"));
                        storageInfo.put("nextExtent", rs.getLong("NEXT_EXTENT"));
                        storageInfo.put("minExtents", rs.getLong("MIN_EXTENTS"));
                        storageInfo.put("maxExtents", rs.getLong("MAX_EXTENTS"));
                        storageInfo.put("pctIncrease", rs.getInt("PCT_INCREASE"));
                        storageInfo.put("freelists", rs.getInt("FREELISTS"));
                        storageInfo.put("freelistGroups", rs.getInt("FREELIST_GROUPS"));
                        storageInfo.put("bufferPool", rs.getString("BUFFER_POOL"));
                        tableInfo.put("storage", storageInfo);
                    }
                }
            }
            
            // 表的统计信息
            String statsSql = "SELECT " +
                    "SAMPLE_SIZE, " +
                    "DEGREE, " +
                    "INSTANCES, " +
                    "TEMPORARY, " +
                    "PARTITIONED, " +
                    "IOT_TYPE, " +
                    "TABLE_LOCK, " +
                    "DURATION " +
                    "FROM ALL_TABLES " +
                    "WHERE TABLE_NAME = ?";
            
            try (PreparedStatement pstmt = conn.prepareStatement(statsSql)) {
                pstmt.setString(1, tableName.toUpperCase());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        tableInfo.put("sampleSize", rs.getLong("SAMPLE_SIZE"));
                        tableInfo.put("degree", rs.getString("DEGREE"));
                        tableInfo.put("instances", rs.getString("INSTANCES"));
                        tableInfo.put("temporary", rs.getString("TEMPORARY"));
                        tableInfo.put("partitioned", rs.getString("PARTITIONED"));
                        tableInfo.put("iotType", rs.getString("IOT_TYPE"));
                        tableInfo.put("tableLock", rs.getString("TABLE_LOCK"));
                        tableInfo.put("duration", rs.getString("DURATION"));
                    }
                }
            }
            
            // 表的DDL
            String ddlSql = "SELECT DBMS_METADATA.GET_DDL('TABLE', ?) AS DDL FROM DUAL";
            try (PreparedStatement pstmt = conn.prepareStatement(ddlSql)) {
                pstmt.setString(1, tableName.toUpperCase());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        tableInfo.put("ddl", rs.getString("DDL"));
                    }
                }
            } catch (SQLException e) {
                // 忽略DDL获取错误，因为某些用户可能没有权限
                tableInfo.put("ddl", "无法获取DDL，可能缺少权限");
            }
            
            // 表的依赖对象
            String dependencySql = "SELECT " +
                    "REFERENCED_OWNER, " +
                    "REFERENCED_NAME, " +
                    "REFERENCED_TYPE " +
                    "FROM ALL_DEPENDENCIES " +
                    "WHERE NAME = ? AND TYPE = 'TABLE'";
            
            try (PreparedStatement pstmt = conn.prepareStatement(dependencySql)) {
                pstmt.setString(1, tableName.toUpperCase());
                try (ResultSet rs = pstmt.executeQuery()) {
                    List<Map<String, Object>> dependencies = new ArrayList<>();
                    while (rs.next()) {
                        Map<String, Object> dependency = new HashMap<>();
                        dependency.put("owner", rs.getString("REFERENCED_OWNER"));
                        dependency.put("name", rs.getString("REFERENCED_NAME"));
                        dependency.put("type", rs.getString("REFERENCED_TYPE"));
                        dependencies.add(dependency);
                    }
                    tableInfo.put("dependencies", dependencies);
                }
            }
            
            // 表的大小估算
            String sizeSql = "SELECT " +
                    "BLOCKS * (SELECT BLOCK_SIZE FROM V$PARAMETER WHERE NAME = 'db_block_size') AS TABLE_SIZE " +
                    "FROM ALL_TABLES " +
                    "WHERE TABLE_NAME = ?";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sizeSql)) {
                pstmt.setString(1, tableName.toUpperCase());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        tableInfo.put("sizeBytes", rs.getLong("TABLE_SIZE"));
                        // 转换为MB
                        double sizeMB = rs.getLong("TABLE_SIZE") / (1024.0 * 1024.0);
                        tableInfo.put("sizeMB", sizeMB);
                    }
                }
            } catch (SQLException e) {
                // 忽略大小估算错误
                tableInfo.put("sizeBytes", -1L);
                tableInfo.put("sizeMB", -1.0);
            }
            
            // 表的创建时间和最后修改时间
            String timeSql = "SELECT " +
                    "CREATED, " +
                    "LAST_DDL_TIME " +
                    "FROM ALL_OBJECTS " +
                    "WHERE OBJECT_NAME = ? AND OBJECT_TYPE = 'TABLE'";
            
            try (PreparedStatement pstmt = conn.prepareStatement(timeSql)) {
                pstmt.setString(1, tableName.toUpperCase());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        tableInfo.put("createTime", rs.getTimestamp("CREATED"));
                        tableInfo.put("lastModified", rs.getTimestamp("LAST_DDL_TIME"));
                    }
                }
            }

            
            return tableInfo;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // 忽略关闭连接错误
                }
            }
        }
    }

    @Override
    public List<Map<String, Object>> listColumns(String tableName) throws Exception {
        List<Map<String, Object>> columns = new ArrayList<>();
        try (Connection conn = getConnection()) {
            String sql = "SELECT c.COLUMN_NAME, c.DATA_TYPE, c.DATA_LENGTH, " +
                    "c.DATA_PRECISION, c.DATA_SCALE, c.NULLABLE, " +
                    "c.DATA_DEFAULT, c.COLUMN_ID, cc.COMMENTS " +
                    "FROM USER_TAB_COLUMNS c " +
                    "LEFT JOIN USER_COL_COMMENTS cc " +
                    "ON c.TABLE_NAME = cc.TABLE_NAME AND c.COLUMN_NAME = cc.COLUMN_NAME " +
                    "WHERE c.TABLE_NAME = ? " +
                    "ORDER BY c.COLUMN_ID";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName.toUpperCase());

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> column = new HashMap<>();
                        column.put("name", rs.getString("COLUMN_NAME"));
                        column.put("type", rs.getString("DATA_TYPE"));
                        column.put("length", rs.getInt("DATA_LENGTH"));
                        column.put("precision", rs.getObject("DATA_PRECISION"));
                        column.put("scale", rs.getObject("DATA_SCALE"));
                        column.put("nullable", "Y".equalsIgnoreCase(rs.getString("NULLABLE")));
                        column.put("defaultValue", rs.getString("DATA_DEFAULT"));
                        column.put("comment", rs.getString("COMMENTS"));
                        column.put("position", rs.getInt("COLUMN_ID"));
                        columns.add(column);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取字段列表失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return columns;
    }

    @Override
    public Map<String, Object> getColumnInfo(String tableName, String columnName) throws Exception {
        Map<String, Object> info = new HashMap<>();
        try (Connection conn = getConnection()) {
            String sql = "SELECT c.COLUMN_NAME, c.DATA_TYPE, c.DATA_LENGTH, " +
                    "c.DATA_PRECISION, c.DATA_SCALE, c.NULLABLE, " +
                    "c.DATA_DEFAULT, c.COLUMN_ID, cc.COMMENTS " +
                    "FROM USER_TAB_COLUMNS c " +
                    "LEFT JOIN USER_COL_COMMENTS cc " +
                    "ON c.TABLE_NAME = cc.TABLE_NAME AND c.COLUMN_NAME = cc.COLUMN_NAME " +
                    "WHERE c.TABLE_NAME = ? AND c.COLUMN_NAME = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName.toUpperCase());
                stmt.setString(2, columnName.toUpperCase());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        info.put("name", rs.getString("COLUMN_NAME"));
                        info.put("type", rs.getString("DATA_TYPE"));
                        info.put("length", rs.getInt("DATA_LENGTH"));
                        info.put("precision", rs.getObject("DATA_PRECISION"));
                        info.put("scale", rs.getObject("DATA_SCALE"));
                        info.put("nullable", "Y".equalsIgnoreCase(rs.getString("NULLABLE")));
                        info.put("defaultValue", rs.getString("DATA_DEFAULT"));
                        info.put("comment", rs.getString("COMMENTS"));
                        info.put("position", rs.getInt("COLUMN_ID"));

                        // 检查是否为主键
                        String pkSql = "SELECT COUNT(*) as is_pk " +
                                "FROM USER_CONSTRAINTS c " +
                                "JOIN USER_CONS_COLUMNS cc " +
                                "ON c.CONSTRAINT_NAME = cc.CONSTRAINT_NAME " +
                                "WHERE c.CONSTRAINT_TYPE = 'P' " +
                                "AND c.TABLE_NAME = ? " +
                                "AND cc.COLUMN_NAME = ?";

                        try (PreparedStatement pkStmt = conn.prepareStatement(pkSql)) {
                            pkStmt.setString(1, tableName.toUpperCase());
                            if (columnName != null && !columnName.isEmpty()) {
                                pkStmt.setString(2, columnName.toUpperCase());
                            }

                            try (ResultSet pkRs = pkStmt.executeQuery()) {
                                if (pkRs.next()) {
                                    info.put("isPrimaryKey", pkRs.getInt("is_pk") > 0);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取字段信息失败: table={}, column={}, error={}",
                    tableName, columnName, e.getMessage(), e);
            throw e;
        }
        return info;
    }

    @Override
    public Map<String, Object> calculateQualityMetric(String tableName, String metricType) throws Exception {
        Map<String, Object> result = new HashMap<>();
        String sql = null;

        switch (metricType.toLowerCase()) {
            case "completeness":
                sql = String.format("SELECT COUNT(*) as total_count, " +
                                "SUM(CASE WHEN * IS NULL THEN 1 ELSE 0 END) as null_count " +
                                "FROM %s",
                        wrapIdentifier(tableName));
                break;

            case "uniqueness":
                sql = String.format("SELECT COUNT(*) as total_count, " +
                                "COUNT(DISTINCT *) as unique_count " +
                                "FROM %s",
                        wrapIdentifier(tableName));
                break;

            case "validity":
                // 这里需要根据表结构生成验证SQL
                sql = String.format("SELECT COUNT(*) as total_count, " +
                                "0 as invalid_count " +
                                "FROM %s",
                        wrapIdentifier(tableName));
                break;
        }

        if (sql != null) {
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    result.put("metricType", metricType);
                    result.put("totalCount", rs.getLong("total_count"));

                    switch (metricType.toLowerCase()) {
                        case "completeness":
                            long nullCount = rs.getLong("null_count");
                            result.put("nullCount", nullCount);
                            result.put("completeness", 1 - (double) nullCount / rs.getLong("total_count"));
                            break;

                        case "uniqueness":
                            long uniqueCount = rs.getLong("unique_count");
                            result.put("uniqueCount", uniqueCount);
                            result.put("uniqueness", (double) uniqueCount / rs.getLong("total_count"));
                            break;

                        case "validity":
                            long invalidCount = rs.getLong("invalid_count");
                            result.put("invalidCount", invalidCount);
                            result.put("validity", 1 - (double) invalidCount / rs.getLong("total_count"));
                            break;
                    }
                }
            } catch (Exception e) {
                log.error("计算质量指标失败: table={}, metric={}, error={}",
                        tableName, metricType, e.getMessage(), e);
                throw e;
            }
        }

        return result;
    }

    @Override
    public Map<String, Object> getQualityIssues(String tableName) throws Exception {
        Map<String, Object> issues = new HashMap<>();
        try (Connection conn = getConnection()) {
            // 获取表的所有列
            List<Map<String, Object>> columns = listColumns(tableName);

            // 对每一列进行质量检查
            List<Map<String, Object>> columnIssues = new ArrayList<>();
            for (Map<String, Object> column : columns) {
                String columnName = (String) column.get("name");
                String dataType = ((String) column.get("type")).toUpperCase();

                // 根据数据类型检查质量问题
                String sql = null;
                switch (dataType) {
                    case "NUMBER":
                    case "INT":
                    case "INTEGER":
                    case "BIGINT":
                    case "SMALLINT":
                        sql = String.format(
                                "SELECT COUNT(*) as issue_count " +
                                        "FROM %s " +
                                        "WHERE %s IS NOT NULL AND REGEXP_LIKE(TO_CHAR(%s), '^-?[0-9]+$') = 0",
                                wrapIdentifier(tableName),
                                wrapIdentifier(columnName),
                                wrapIdentifier(columnName));
                        break;

                    case "DECIMAL":
                    case "NUMERIC":
                    case "FLOAT":
                    case "DOUBLE":
                        sql = String.format(
                                "SELECT COUNT(*) as issue_count " +
                                        "FROM %s " +
                                        "WHERE %s IS NOT NULL AND REGEXP_LIKE(TO_CHAR(%s), '^-?[0-9]*.?[0-9]+$') = 0",
                                wrapIdentifier(tableName),
                                wrapIdentifier(columnName),
                                wrapIdentifier(columnName));
                        break;

                    case "DATE":
                        sql = String.format(
                                "SELECT COUNT(*) as issue_count " +
                                        "FROM %s " +
                                        "WHERE %s IS NOT NULL AND TO_CHAR(%s, 'YYYY-MM-DD') IS NULL",
                                wrapIdentifier(tableName),
                                wrapIdentifier(columnName),
                                wrapIdentifier(columnName));
                        break;

                    case "TIMESTAMP":
                        sql = String.format(
                                "SELECT COUNT(*) as issue_count " +
                                        "FROM %s " +
                                        "WHERE %s IS NOT NULL AND TO_CHAR(%s, 'YYYY-MM-DD HH24:MI:SS') IS NULL",
                                wrapIdentifier(tableName),
                                wrapIdentifier(columnName),
                                wrapIdentifier(columnName));
                        break;
                }

                if (sql != null) {
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(sql)) {
                        if (rs.next()) {
                            long issueCount = rs.getLong("issue_count");
                            if (issueCount > 0) {
                                Map<String, Object> issue = new HashMap<>();
                                issue.put("columnName", columnName);
                                issue.put("dataType", dataType);
                                issue.put("issueCount", issueCount);
                                issue.put("issueType", "Invalid Format");
                                issue.put("description", "Values do not match expected format for " + dataType);
                                columnIssues.add(issue);
                            }
                        }
                    }
                }
            }

            issues.put("tableName", tableName);
            issues.put("totalColumns", columns.size());
            issues.put("columnsWithIssues", columnIssues.size());
            issues.put("issues", columnIssues);
        } catch (Exception e) {
            log.error("获取质量问题失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return issues;
    }

    @Override
    public String getTableEngine(String tableName) throws Exception {
        // 达梦数据库不使用存储引擎概念
        // 可以返回表空间或其他相关信息
        return getTableSpace(tableName);
    }

    @Override
    public String getTableCharset(String tableName) throws Exception {
        // 原代码可能类似于:
        /*
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT CHARACTER_SET_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = '" + tableName.toUpperCase() + "'")) {
            if (rs.next()) {
                return rs.getString("CHARACTER_SET_NAME");
            }
            return null;
        } catch (Exception e) {
            log.error("获取表字符集失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        */
        
        // 修改为使用DM7特定的函数来获取字符集
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT SF_GET_UNICODE_FLAG() AS CHARSET_FLAG FROM DUAL")) {
        
            if (rs.next()) {
                int charsetFlag = rs.getInt("CHARSET_FLAG");
                // 根据返回值映射到具体的字符集名称
                switch (charsetFlag) {
                    case 0:
                        return "GB18030";
                    case 1:
                        return "UTF-8";
                    case 2:
                        return "EUC-KR";
                    default:
                        return "UNKNOWN(" + charsetFlag + ")";
                }
            }
            return "UNKNOWN";
        } catch (Exception e) {
            log.error("获取表字符集失败: table={}, error={}", tableName, e.getMessage(), e);
            
            // 尝试备用方法
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT UNICODE() AS CHARSET_FLAG FROM DUAL")) {
            
                if (rs.next()) {
                    int charsetFlag = rs.getInt("CHARSET_FLAG");
                    // 根据返回值映射到具体的字符集名称
                    switch (charsetFlag) {
                        case 0:
                            return "GB18030";
                        case 1:
                            return "UTF-8";
                        case 2:
                            return "EUC-KR";
                        default:
                            return "UNKNOWN(" + charsetFlag + ")";
                    }
                }
                return "UNKNOWN";
            } catch (Exception e2) {
                log.error("备用方法获取字符集也失败: error={}", e2.getMessage(), e2);
                // 如果两种方法都失败，返回默认值或抛出异常
                return "UTF-8"; // 默认假设为UTF-8
            }
        }
    }

    @Override
    public String getTableCollation(String tableName) throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
                
                // 尝试获取数据库级别的排序规则设置
                String sql = "SELECT VALUE FROM V$NLS_PARAMETERS WHERE PARAMETER = 'NLS_SORT'";
            try (ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            } catch (SQLException e) {
                log.warn("获取排序规则失败: {}", e.getMessage());
            }
            
            // 返回默认值
            return "BINARY";
        }
    }

    @Override
    public Long getTableSize(String tableName) throws Exception {
        String sql = String.format(
            "SELECT SUM(BYTES) AS TABLE_SIZE " +
            "FROM USER_SEGMENTS " +
            "WHERE SEGMENT_NAME = '%s' AND SEGMENT_TYPE = 'TABLE'",
            tableName.toUpperCase()
        );
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                return rs.getLong("TABLE_SIZE");
                }
            } catch (SQLException e) {
            // 如果上述查询失败，尝试另一种方法
            log.warn("获取表大小失败，尝试替代方法: {}", e.getMessage());
            
            // 替代方法：估算大小
            try {
                Long rowCount = getTableRowCount(tableName);
                // 估算每行大约 100 字节
                return rowCount * 100;
            } catch (Exception ex) {
                log.error("估算表大小失败: {}", ex.getMessage());
            }
        }
        
        return 0L;
    }

    @Override
    public Long getTableRowCount(String tableName) throws Exception {
        // 达梦数据库查询表行数
        String sql = String.format("SELECT COUNT(*) AS ROW_COUNT FROM %s", wrapIdentifier(tableName));
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
                    if (rs.next()) {
                return rs.getLong("ROW_COUNT");
            }
        }
        return 0L;
    }

    @Override
    public String getTableSpace(String tableName) throws Exception {
        // 达梦数据库查询表空间
        String sql = String.format(
            "SELECT TABLESPACE_NAME FROM USER_TABLES WHERE TABLE_NAME = '%s'",
            tableName.toUpperCase()
        );
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
                    if (rs.next()) {
                return rs.getString("TABLESPACE_NAME");
            }
        }
        return null;
    }

    @Override
    public Integer getCharacterLength(String tableName, String columnName) throws Exception {
        Integer length = null;
        try (Connection conn = getConnection()) {
            String sql = "SELECT DATA_LENGTH " +
                    "FROM USER_TAB_COLUMNS " +
                    "WHERE TABLE_NAME = ? AND COLUMN_NAME = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName.toUpperCase());
                stmt.setString(2, columnName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        length = rs.getInt("DATA_LENGTH");
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取字段字符长度失败: table={}, column={}, error={}",
                    tableName, columnName, e.getMessage(), e);
            throw e;
        }
        return length;
    }

    @Override
    public Integer getNumericPrecision(String tableName, String columnName) throws Exception {
        // 添加参数验证
        if (tableName == null || columnName == null) {
            log.warn("获取字段数值精度失败: 表名或列名为空, table={}, column={}", tableName, columnName);
            return null;
        }
        
        // 修改列名 NUMERIC_PRECISION 为 DATA_PRECISION
        String sql = "SELECT DATA_PRECISION FROM USER_TAB_COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tableName.toUpperCase());
            stmt.setString(2, columnName.toUpperCase());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // 处理可能为 null 的情况
                    Object precision = rs.getObject("DATA_PRECISION");
                    return precision != null ? rs.getInt("DATA_PRECISION") : null;
                }
                return null;
            }
        } catch (Exception e) {
            log.error("获取字段数值精度失败: table={}, column={}, error={}", tableName, columnName, e.getMessage());
            throw e;
        }
    }

    @Override
    public Integer getNumericScale(String tableName, String columnName) throws Exception {
        Integer scale = null;
        try (Connection conn = getConnection()) {
            String sql = "SELECT DATA_SCALE " +
                    "FROM USER_TAB_COLUMNS " +
                    "WHERE TABLE_NAME = ? AND COLUMN_NAME = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName.toUpperCase());
                stmt.setString(2, columnName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        scale = rs.getObject("DATA_SCALE") != null ?
                                rs.getInt("DATA_SCALE") : null;
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取字段小数位数失败: table={}, column={}, error={}",
                    tableName, columnName, e.getMessage(), e);
            throw e;
        }
        return scale;
    }

    @Override
    public String getColumnDefault(String tableName, String columnName) throws Exception {
        // 达梦数据库查询列默认值
        String sql = String.format(
            "SELECT DATA_DEFAULT FROM USER_TAB_COLUMNS " +
            "WHERE TABLE_NAME = '%s' AND COLUMN_NAME = '%s'",
            tableName.toUpperCase(),
            columnName.toUpperCase()
        );
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
                    if (rs.next()) {
                String defaultValue = rs.getString("DATA_DEFAULT");
                // 处理默认值字符串，去除引号等
                if (defaultValue != null) {
                    defaultValue = defaultValue.trim();
                    if (defaultValue.startsWith("'") && defaultValue.endsWith("'")) {
                        defaultValue = defaultValue.substring(1, defaultValue.length() - 1);
                    }
                }
                return defaultValue;
            }
        }
        return null;
    }

    @Override
    public String getColumnExtra(String tableName, String columnName) throws Exception {
        String extra = null;
        try (Connection conn = getConnection()) {
            // 检查是否为自增列
            String sql = "SELECT IDENTITY_COLUMN " +
                    "FROM USER_TAB_COLUMNS " +
                    "WHERE TABLE_NAME = ? AND COLUMN_NAME = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName.toUpperCase());
                stmt.setString(2, columnName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getString("IDENTITY_COLUMN") != null) {
                        extra = "IDENTITY";
                    }
                }
            } catch (SQLException e) {
                // DM7可能不支持IDENTITY_COLUMN列，忽略错误
            }
        } catch (Exception e) {
            log.error("获取字段额外属性失败: table={}, column={}, error={}",
                    tableName, columnName, e.getMessage(), e);
            throw e;
        }
        return extra;
    }

    @Override
    public Integer getColumnPosition(String tableName, String columnName) throws Exception {
        Integer position = null;
        try (Connection conn = getConnection()) {
            String sql = "SELECT COLUMN_ID " +
                    "FROM USER_TAB_COLUMNS " +
                    "WHERE TABLE_NAME = ? AND COLUMN_NAME = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName.toUpperCase());
                stmt.setString(2, columnName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        position = rs.getInt("COLUMN_ID");
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取字段位置失败: table={}, column={}, error={}",
                    tableName, columnName, e.getMessage(), e);
            throw e;
        }
        return position;
    }

    @Override
    public Date getTableCreateTime(String tableName) throws Exception {
        // 达梦数据库不直接提供表创建时间
        // 可以通过查询系统表尝试获取
        // 这里返回当前时间作为替代方案
        return new Date();
    }

    @Override
    public Date getTableUpdateTime(String tableName) throws Exception {
        // 达梦数据库不直接提供表更新时间
        // 可以通过查询系统表或监控信息尝试获取
        // 这里返回当前时间作为替代方案
        return new Date();
    }

    @Override
    public Map<String, Object> getTableCompleteness(String tableName) throws Exception {
        Map<String, Object> result = new HashMap<>();
        try (Connection conn = getConnection()) {
            // 获取所有列
            List<Map<String, Object>> columns = listColumns(tableName);

            // 计算每列的非空率
            Map<String, Double> columnCompleteness = new HashMap<>();
            long totalRows = getTableRowCount(tableName);
            result.put("totalRows", totalRows);

            if (totalRows > 0) {
                for (Map<String, Object> column : columns) {
                    String columnName = (String) column.get("name");
                    String sql = String.format(
                            "SELECT COUNT(*) as null_count " +
                                    "FROM %s " +
                                    "WHERE %s IS NULL",
                            wrapIdentifier(tableName),
                            wrapIdentifier(columnName));

                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(sql)) {
                        if (rs.next()) {
                            long nullCount = rs.getLong("null_count");
                            double completeness = 1.0 - (double) nullCount / totalRows;
                            columnCompleteness.put(columnName, completeness);
                        }
                    }
                }
            }

            result.put("columnCompleteness", columnCompleteness);

            // 计算整体完整性
            if (!columnCompleteness.isEmpty()) {
                double avgCompleteness = columnCompleteness.values().stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);
                result.put("overallCompleteness", avgCompleteness);
            }
        } catch (Exception e) {
            log.error("获取表完整性失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return result;
    }

    @Override
    public Map<String, Object> getTableAccuracy(String tableName) throws Exception {
        Map<String, Object> result = new HashMap<>();
        try (Connection conn = getConnection()) {
            // 获取所有列
            List<Map<String, Object>> columns = listColumns(tableName);

            // 计算每列的准确性
            Map<String, Double> columnAccuracy = new HashMap<>();
            long totalRows = getTableRowCount(tableName);
            result.put("totalRows", totalRows);

            if (totalRows > 0) {
                for (Map<String, Object> column : columns) {
                    String columnName = (String) column.get("name");
                    String dataType = ((String) column.get("type")).toUpperCase();

                    String validationSql = getValidationSqlForDataType(tableName, columnName, dataType);
                    if (validationSql != null) {
                        try (Statement stmt = conn.createStatement();
                             ResultSet rs = stmt.executeQuery(validationSql)) {
                            if (rs.next()) {
                                long invalidCount = rs.getLong("invalid_count");
                                double accuracy = 1.0 - (double) invalidCount / totalRows;
                                columnAccuracy.put(columnName, accuracy);
                            }
                        }
                    }
                }
            }

            result.put("columnAccuracy", columnAccuracy);

            // 计算整体准确性
            if (!columnAccuracy.isEmpty()) {
                double avgAccuracy = columnAccuracy.values().stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);
                result.put("overallAccuracy", avgAccuracy);
            }
        } catch (Exception e) {
            log.error("获取表准确性失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return result;
    }

    private String getValidationSqlForDataType(String tableName, String columnName, String dataType) {
        dataType = dataType.toUpperCase();
        
        // 达梦数据库特定的数据验证 SQL
        if (dataType.contains("CHAR") || dataType.contains("TEXT") || dataType.contains("CLOB")) {
            // 字符串类型验证
                return String.format(
                "SELECT COUNT(*) AS invalid_count FROM %s WHERE %s IS NOT NULL AND LENGTH(TRIM(%s)) = 0",
                        wrapIdentifier(tableName),
                        wrapIdentifier(columnName),
                wrapIdentifier(columnName)
            );
        } else if (dataType.equals("NUMBER") || dataType.contains("INT") || dataType.contains("DECIMAL") || 
                   dataType.contains("FLOAT") || dataType.contains("DOUBLE")) {
            // 数值类型验证 - 达梦使用不同的模式匹配
                return String.format(
                "SELECT COUNT(*) AS invalid_count FROM %s WHERE %s IS NOT NULL AND " +
                "NOT REGEXP_LIKE(TO_CHAR(%s), '^[+-]?\\d*(\\.\\d+)?$')",
                        wrapIdentifier(tableName),
                        wrapIdentifier(columnName),
                wrapIdentifier(columnName)
            );
        } else if (dataType.contains("DATE") || dataType.equals("TIMESTAMP")) {
            // 日期类型验证
                return String.format(
                "SELECT COUNT(*) AS invalid_count FROM %s WHERE %s IS NOT NULL AND " +
                "(%s < TO_DATE('0001-01-01', 'YYYY-MM-DD') OR %s > TO_DATE('9999-12-31', 'YYYY-MM-DD'))",
                        wrapIdentifier(tableName),
                        wrapIdentifier(columnName),
                wrapIdentifier(columnName),
                wrapIdentifier(columnName)
            );
        }

        // 默认验证
                return String.format(
            "SELECT COUNT(*) AS invalid_count FROM %s WHERE %s IS NOT NULL",
                        wrapIdentifier(tableName),
            wrapIdentifier(columnName)
        );
    }

    @Override
    public List<String> getPrimaryKeys(String tableName) throws Exception {
        // 达梦数据库查询主键列
        String sql = String.format(
            "SELECT C.COLUMN_NAME " +
            "FROM USER_CONSTRAINTS A " +
            "JOIN USER_CONS_COLUMNS C ON A.CONSTRAINT_NAME = C.CONSTRAINT_NAME " +
            "WHERE A.TABLE_NAME = '%s' AND A.CONSTRAINT_TYPE = 'P' " +
            "ORDER BY C.POSITION",
            tableName.toUpperCase()
        );
        
        List<String> primaryKeys = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        primaryKeys.add(rs.getString("COLUMN_NAME"));
                    }
        }
        return primaryKeys;
    }

    @Override
    public List<Map<String, Object>> getForeignKeys(String tableName) throws Exception {
        // 达梦数据库查询外键
        String sql = String.format(
            "SELECT A.CONSTRAINT_NAME, A.TABLE_NAME, A.COLUMN_NAME, " +
            "C.TABLE_NAME AS REFERENCED_TABLE_NAME, C.COLUMN_NAME AS REFERENCED_COLUMN_NAME " +
            "FROM USER_CONS_COLUMNS A " +
            "JOIN USER_CONSTRAINTS B ON A.CONSTRAINT_NAME = B.CONSTRAINT_NAME " +
            "JOIN USER_CONS_COLUMNS C ON B.R_CONSTRAINT_NAME = C.CONSTRAINT_NAME " +
            "WHERE B.CONSTRAINT_TYPE = 'R' AND A.TABLE_NAME = '%s'",
            tableName.toUpperCase()
        );
        
        List<Map<String, Object>> foreignKeys = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                Map<String, Object> fk = new HashMap<>();
                fk.put("constraint_name", rs.getString("CONSTRAINT_NAME"));
                fk.put("table_name", rs.getString("TABLE_NAME"));
                fk.put("column_name", rs.getString("COLUMN_NAME"));
                fk.put("referenced_table_name", rs.getString("REFERENCED_TABLE_NAME"));
                fk.put("referenced_column_name", rs.getString("REFERENCED_COLUMN_NAME"));
                foreignKeys.add(fk);
            }
        }
        return foreignKeys;
    }

    @Override
    public List<Map<String, Object>> getIndexes(String tableName) throws Exception {
        List<Map<String, Object>> indexes = new ArrayList<>();
        try (Connection conn = getConnection()) {
            String sql = "SELECT " +
                    "i.INDEX_NAME, " +
                    "i.UNIQUENESS, " +
                    "ic.COLUMN_NAME, " +
                    "ic.COLUMN_POSITION " +
                    "FROM USER_INDEXES i " +
                    "JOIN USER_IND_COLUMNS ic " +
                    "ON i.INDEX_NAME = ic.INDEX_NAME " +
                    "WHERE i.TABLE_NAME = ? " +
                    "ORDER BY i.INDEX_NAME, ic.COLUMN_POSITION";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName.toUpperCase());

                Map<String, Map<String, Object>> indexMap = new HashMap<>();

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String indexName = rs.getString("INDEX_NAME");
                        String columnName = rs.getString("COLUMN_NAME");
                        boolean isUnique = "UNIQUE".equals(rs.getString("UNIQUENESS"));

                        Map<String, Object> indexInfo = indexMap.get(indexName);
                        if (indexInfo == null) {
                            indexInfo = new HashMap<>();
                            indexInfo.put("name", indexName);
                            indexInfo.put("unique", isUnique);

                            // 检查是否为主键索引
                            String pkSql = "SELECT COUNT(*) as is_pk " +
                                    "FROM USER_CONSTRAINTS " +
                                    "WHERE CONSTRAINT_TYPE = 'P' " +
                                    "AND CONSTRAINT_NAME = ?";

                            try (PreparedStatement pkStmt = conn.prepareStatement(pkSql)) {
                                pkStmt.setString(1, indexName);
                                try (ResultSet pkRs = pkStmt.executeQuery()) {
                                    if (pkRs.next()) {
                                        indexInfo.put("primary", pkRs.getInt("is_pk") > 0);
                                    }
                                }
                            }

                            indexInfo.put("columns", new ArrayList<String>());
                            indexMap.put(indexName, indexInfo);
                        }

                        @SuppressWarnings("unchecked")
                        List<String> columns = (List<String>) indexInfo.get("columns");
                        columns.add(columnName);
                    }
                }

                indexes.addAll(indexMap.values());
            }
        } catch (Exception e) {
            log.error("获取索引信息失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return indexes;
    }

    @Override
    public Map<String, Object> getTableConsistency(String tableName) throws Exception {
        Map<String, Object> result = new HashMap<>();
        try (Connection conn = getConnection()) {
            // 检查主键一致性
            List<String> primaryKeys = getPrimaryKeys(tableName);
            if (primaryKeys.isEmpty()) {
                result.put("primaryKeyConsistency", true);
            } else {
                String pkColumns = primaryKeys.stream()
                        .map(this::wrapIdentifier)
                        .collect(Collectors.joining(", "));

                String pkSql = String.format(
                        "SELECT COUNT(*) as total_count, " +
                                "COUNT(DISTINCT %s) as unique_count " +
                                "FROM %s",
                        pkColumns,
                        wrapIdentifier(tableName));

                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(pkSql)) {
                    if (rs.next()) {
                        long totalCount = rs.getLong("total_count");
                        long uniqueCount = rs.getLong("unique_count");
                        result.put("primaryKeyConsistency", totalCount == uniqueCount);
                    }
                }
            }

            // 检查外键一致性
            List<Map<String, Object>> foreignKeys = getForeignKeys(tableName);
            boolean fkConsistency = true;

            for (Map<String, Object> fk : foreignKeys) {
                String columnName = (String) fk.get("columnName");
                String refTable = (String) fk.get("referencedTable");
                String refColumn = (String) fk.get("referencedColumn");

                String fkSql = String.format(
                        "SELECT COUNT(*) as violation_count " +
                                "FROM %s t1 " +
                                "LEFT JOIN %s t2 ON t1.%s = t2.%s " +
                                "WHERE t1.%s IS NOT NULL AND t2.%s IS NULL",
                        wrapIdentifier(tableName),
                        wrapIdentifier(refTable),
                        wrapIdentifier(columnName),
                        wrapIdentifier(refColumn),
                        wrapIdentifier(columnName),
                        wrapIdentifier(refColumn));

                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(fkSql)) {
                    if (rs.next()) {
                        if (rs.getLong("violation_count") > 0) {
                            fkConsistency = false;
                            break;
                        }
                    }
                }
            }

            result.put("foreignKeyConsistency", fkConsistency);

        } catch (Exception e) {
            log.error("获取表一致性失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return result;
    }

    @Override
    public Map<String, Object> getTableUniqueness(String tableName) throws Exception {
        Map<String, Object> uniqueness = new HashMap<>();
        List<Map<String, Object>> uniquenessIssues = new ArrayList<>();
        
        // 获取表的所有列
        String sql = String.format(
            "SELECT COLUMN_NAME, DATA_TYPE FROM USER_TAB_COLUMNS " +
            "WHERE TABLE_NAME = '%s'",
            tableName.toUpperCase()
        );
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
        
                    while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                
                // 检查列唯一性（找到重复值）
                String duplicateSql = String.format(
                    "SELECT %s, COUNT(*) AS dup_count " +
                    "FROM %s " +
                    "WHERE %s IS NOT NULL " +
                    "GROUP BY %s " +
                    "HAVING COUNT(*) > 1 " +
                    "ORDER BY dup_count DESC " +
                    "FETCH FIRST 10 ROWS ONLY",
                    wrapIdentifier(columnName),
                    wrapIdentifier(tableName),
                    wrapIdentifier(columnName),
                    wrapIdentifier(columnName)
                );
                
                try (Statement dupStmt = conn.createStatement();
                     ResultSet dupRs = dupStmt.executeQuery(duplicateSql)) {
                    
                    boolean hasDuplicates = false;
                    long totalDuplicates = 0;
                    
                    while (dupRs.next()) {
                        hasDuplicates = true;
                        String value = dupRs.getString(1);
                        long count = dupRs.getLong("dup_count");
                        totalDuplicates += count;
                        
                        Map<String, Object> issue = new HashMap<>();
                        issue.put("column_name", columnName);
                        issue.put("value", value);
                        issue.put("duplicate_count", count);
                        uniquenessIssues.add(issue);
                    }
                    
                    if (hasDuplicates) {
                        // 计算总记录数
                        String countSql = String.format(
                            "SELECT COUNT(*) AS total FROM %s",
                            wrapIdentifier(tableName)
                        );
                        
                        try (Statement countStmt = conn.createStatement();
                             ResultSet countRs = countStmt.executeQuery(countSql)) {
                            
                            if (countRs.next()) {
                                long total = countRs.getLong("total");
                                // 添加列重复率
                                double duplicateRate = (double) totalDuplicates / total;
                                
                                Map<String, Object> columnInfo = new HashMap<>();
                                columnInfo.put("column_name", columnName);
                                columnInfo.put("duplicate_rate", duplicateRate);
                                columnInfo.put("total_duplicates", totalDuplicates);
                                uniquenessIssues.add(columnInfo);
                            }
                        }
                    }
                }
            }
        }
        
        uniqueness.put("issues", uniquenessIssues);
        uniqueness.put("has_issues", !uniquenessIssues.isEmpty());
        
        return uniqueness;
    }

    @Override
    public Map<String, Object> getTableValidity(String tableName) throws Exception {
        Map<String, Object> result = new HashMap<>();
        try (Connection conn = getConnection()) {
            // 获取所有列的数据类型
            List<Map<String, Object>> columns = listColumns(tableName);

            Map<String, Long> invalidCounts = new HashMap<>();
            long totalRows = getTableRowCount(tableName);

            for (Map<String, Object> column : columns) {
                String columnName = (String) column.get("name");
                String dataType = ((String) column.get("type")).toUpperCase();

                String validationSql = getValidationSqlForDataType(tableName, columnName, dataType);
                if (validationSql != null) {
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(validationSql)) {
                        if (rs.next()) {
                            invalidCounts.put(columnName, rs.getLong("invalid_count"));
                        }
                    }
                }
            }

            result.put("totalRows", totalRows);
            result.put("invalidCounts", invalidCounts);

            if (totalRows > 0 && !invalidCounts.isEmpty()) {
                double overallValidity = 1.0 - invalidCounts.values().stream()
                        .mapToLong(Long::longValue)
                        .sum() / (double) (totalRows * invalidCounts.size());
                result.put("overallValidity", overallValidity);
            }
        } catch (Exception e) {
            log.error("获取表有效性失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return result;
    }

    @Override
    public Map<String, Integer> getQualityIssueCount(String tableName) throws Exception {
        Map<String, Integer> counts = new HashMap<>();
        try (Connection conn = getConnection()) {
            // 获取所有列
            List<Map<String, Object>> columns = listColumns(tableName);

            for (Map<String, Object> column : columns) {
                String columnName = (String) column.get("name");
                String dataType = ((String) column.get("type")).toUpperCase();

                String validationSql = getValidationSqlForDataType(tableName, columnName, dataType);
                if (validationSql != null) {
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(validationSql)) {
                        if (rs.next()) {
                            counts.put(columnName, rs.getInt("invalid_count"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取质量问题数量失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return counts;
    }

    @Override
    public Map<String, Double> getQualityIssueDistribution(String tableName) throws Exception {
        Map<String, Double> distribution = new HashMap<>();
        try (Connection conn = getConnection()) {
            // 获取总行数
            long totalRows = getTableRowCount(tableName);
            if (totalRows == 0) {
                return distribution;
            }

            // 获取所有列
            List<Map<String, Object>> columns = listColumns(tableName);

            for (Map<String, Object> column : columns) {
                String columnName = (String) column.get("name");
                String dataType = ((String) column.get("type")).toUpperCase();

                String validationSql = getValidationSqlForDataType(tableName, columnName, dataType);
                if (validationSql != null) {
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(validationSql)) {
                        if (rs.next()) {
                            long invalidCount = rs.getLong("invalid_count");
                            double ratio = (double) invalidCount / totalRows;
                            distribution.put(columnName, ratio);
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

    @Override
    public List<Map<String, Object>> getTableForeignKeyRelations(String tableName) throws Exception {
        List<Map<String, Object>> relations = new ArrayList<>();
        try (Connection conn = getConnection()) {
            String sql = "SELECT " +
                    "c.CONSTRAINT_NAME, " +
                    "cc.COLUMN_NAME, " +
                    "r.TABLE_NAME as REFERENCED_TABLE_NAME, " +
                    "rc.COLUMN_NAME as REFERENCED_COLUMN_NAME, " +
                    "c.DELETE_RULE " +
                    "FROM USER_CONSTRAINTS c " +
                    "JOIN USER_CONS_COLUMNS cc ON c.CONSTRAINT_NAME = cc.CONSTRAINT_NAME " +
                    "JOIN USER_CONSTRAINTS r ON c.R_CONSTRAINT_NAME = r.CONSTRAINT_NAME " +
                    "JOIN USER_CONS_COLUMNS rc ON r.CONSTRAINT_NAME = rc.CONSTRAINT_NAME " +
                    "WHERE c.CONSTRAINT_TYPE = 'R' " +
                    "AND c.TABLE_NAME = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName.toUpperCase());

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> relation = new HashMap<>();
                        relation.put("constraintName", rs.getString("CONSTRAINT_NAME"));
                        relation.put("sourceColumn", rs.getString("COLUMN_NAME"));
                        relation.put("referencedTable", rs.getString("REFERENCED_TABLE_NAME"));
                        relation.put("referencedColumn", rs.getString("REFERENCED_COLUMN_NAME"));
                        relation.put("deleteRule", rs.getString("DELETE_RULE"));
                        relations.add(relation);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取外键关系失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return relations;
    }

    @Override
    public List<Map<String, Object>> getReferencedByRelations(String tableName) throws Exception {
        List<Map<String, Object>> relations = new ArrayList<>();
        try (Connection conn = getConnection()) {
            // 获取引用此表的外键关系
            String sql = "SELECT A.CONSTRAINT_NAME, A.TABLE_NAME AS REFERENCING_TABLE, " +
                    "A.COLUMN_NAME AS REFERENCING_COLUMN, " +
                    "C.COLUMN_NAME AS REFERENCED_COLUMN " +
                    "FROM ALL_CONS_COLUMNS A " +
                    "JOIN ALL_CONSTRAINTS B ON A.CONSTRAINT_NAME = B.CONSTRAINT_NAME " +
                    "JOIN ALL_CONS_COLUMNS C ON B.R_CONSTRAINT_NAME = C.CONSTRAINT_NAME " +
                    "WHERE B.CONSTRAINT_TYPE = 'R' " +
                    "AND C.TABLE_NAME = ? " +
                    "AND C.OWNER = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName.toUpperCase());
                stmt.setString(2, getSchema().toUpperCase());

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> relation = new HashMap<>();
                        relation.put("referencingTable", rs.getString("REFERENCING_TABLE"));
                        relation.put("referencingColumn", rs.getString("REFERENCING_COLUMN"));
                        relation.put("referencedColumn", rs.getString("REFERENCED_COLUMN"));
                        relation.put("constraintName", rs.getString("CONSTRAINT_NAME"));
                        relation.put("dependencyType", "Referenced by Foreign Key");
                        relations.add(relation);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取被引用关系失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return relations;
    }

    @Override
    public Map<String, Object> getTableFieldStatistics(String tableName) throws Exception {
        Map<String, Object> statistics = new HashMap<>();
        
        try (Connection conn = getConnection()) {
            // 使用 CAST 和 DECIMAL 来处理大数值
            String sql = "SELECT " +
                    "COUNT(*) as total_rows, " +
                    "COUNT(DISTINCT %s) as unique_count, " +
                    "CAST(COUNT(CASE WHEN %s IS NULL THEN 1 END) AS DECIMAL(38,2)) as null_count, " +
                    "CAST(COUNT(CASE WHEN %s IS NOT NULL THEN 1 END) AS DECIMAL(38,2)) as not_null_count, " +
                    "CAST(COUNT(CASE WHEN TRIM(%s) = '' AND %s IS NOT NULL THEN 1 END) AS DECIMAL(38,2)) as empty_count " +
                    "FROM %s";
            
            // 获取所有列信息
            List<Map<String, Object>> columns = listColumns(tableName);
            
            for (Map<String, Object> column : columns) {
                String columnName = (String) column.get("COLUMN_NAME");
                String formattedSql = String.format(sql, 
                    columnName, columnName, columnName, columnName, columnName, tableName);
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(formattedSql)) {
                    if (rs.next()) {
                        Map<String, Object> columnStats = new HashMap<>();
                        columnStats.put("totalRows", rs.getBigDecimal("total_rows"));
                        columnStats.put("uniqueCount", rs.getBigDecimal("unique_count"));
                        columnStats.put("nullCount", rs.getBigDecimal("null_count"));
                        columnStats.put("notNullCount", rs.getBigDecimal("not_null_count"));
                        columnStats.put("emptyCount", rs.getBigDecimal("empty_count"));
                        
                        // 计算比率时使用 BigDecimal 避免精度损失
                        BigDecimal totalRows = rs.getBigDecimal("total_rows");
                        if (totalRows != null && totalRows.compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal nullRate = rs.getBigDecimal("null_count")
                                .divide(totalRows, 4, RoundingMode.HALF_UP);
                            BigDecimal notNullRate = rs.getBigDecimal("not_null_count")
                                .divide(totalRows, 4, RoundingMode.HALF_UP);
                            BigDecimal emptyRate = rs.getBigDecimal("empty_count")
                                .divide(totalRows, 4, RoundingMode.HALF_UP);
                            
                            columnStats.put("nullRate", nullRate);
                            columnStats.put("notNullRate", notNullRate);
                            columnStats.put("emptyRate", emptyRate);
                        }
                        
                        statistics.put(columnName, columnStats);
                    }
                } catch (SQLException e) {
                    log.warn("获取列 {} 的统计信息失败: {}", columnName, e.getMessage());
                    // 继续处理其他列
                }
            }
        } catch (Exception e) {
            log.error("获取字段统计信息失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        
        return statistics;
    }

    public List<Map<String, Object>> getTableChangeHistory(String tableName) throws Exception {
        List<Map<String, Object>> history = new ArrayList<>();
        try (Connection conn = getConnection()) {
            String sql = "SELECT " +
                    "OPERATION_TIME as CHANGE_TIME, " +
                    "OPERATION_USER as USER_NAME, " +
                    "OPERATION_TYPE, " +
                    "SQL_TEXT " +
                    "FROM V$SQL_AUDIT " +
                    "WHERE TABLE_NAME = ? " +
                    "AND OPERATION_TYPE IN ('CREATE', 'ALTER', 'DROP', 'RENAME') " +
                    "ORDER BY OPERATION_TIME DESC";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName.toUpperCase());

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> change = new HashMap<>();
                        change.put("changeTime", rs.getTimestamp("CHANGE_TIME"));
                        change.put("user", rs.getString("USER_NAME"));
                        change.put("operationType", rs.getString("OPERATION_TYPE"));
                        change.put("sql", rs.getString("SQL_TEXT"));
                        history.add(change);
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
    public Map<String, Object> getColumnDistribution(String tableName, String columnName) throws Exception {
        Map<String, Object> distribution = new HashMap<>();
        try (Connection conn = getConnection()) {
            String dataType = null;

            // 添加参数验证
            if (tableName == null || columnName == null) {
                log.warn("获取字段分布信息失败: 表名或列名为空, table={}, column={}", tableName, columnName);
                return distribution;
            }

            // 获取字段类型
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT DATA_TYPE FROM USER_TAB_COLUMNS " +
                            "WHERE TABLE_NAME = ? AND COLUMN_NAME = ?")) {
                stmt.setString(1, tableName.toUpperCase());
                stmt.setString(2, columnName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        dataType = rs.getString("DATA_TYPE").toUpperCase();
                    }
                }
            }

            if (dataType == null) {
                log.warn("列不存在: table={}, column={}", tableName, columnName);
                return distribution;
            }

            // 基本统计信息
            String basicSql = "SELECT COUNT(*) as total_count, " +
                    "COUNT(DISTINCT " + wrapIdentifier(columnName) + ") as unique_count, " +
                    "COUNT(" + wrapIdentifier(columnName) + ") as non_null_count " +
                    "FROM " + wrapIdentifier(tableName);
                    
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(basicSql)) {
                if (rs.next()) {
                    distribution.put("totalCount", rs.getLong("total_count"));
                    distribution.put("uniqueCount", rs.getLong("unique_count"));
                    distribution.put("nonNullCount", rs.getLong("non_null_count"));
                    
                    // 计算空值率和唯一率
                    long totalCount = rs.getLong("total_count");
                    if (totalCount > 0) {
                        double nullRate = 1.0 - (double) rs.getLong("non_null_count") / totalCount;
                        double uniqueRate = (double) rs.getLong("unique_count") / totalCount;
                        distribution.put("nullRate", nullRate);
                        distribution.put("uniqueRate", uniqueRate);
                    }
                }
            }

            // 根据数据类型获取特定统计信息
            if (dataType.matches("NUMBER|DECIMAL|NUMERIC|FLOAT|DOUBLE")) {
                // 数值类型的统计
                String numericSql = "SELECT " +
                        "MIN(" + wrapIdentifier(columnName) + ") as min_value, " +
                        "MAX(" + wrapIdentifier(columnName) + ") as max_value, " +
                        "AVG(" + wrapIdentifier(columnName) + ") as avg_value, " +
                        "STDDEV(" + wrapIdentifier(columnName) + ") as std_dev " +
                        "FROM " + wrapIdentifier(tableName);
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(numericSql)) {
                    if (rs.next()) {
                        Map<String, Object> numericStats = new HashMap<>();
                        numericStats.put("min", rs.getObject("min_value"));
                        numericStats.put("max", rs.getObject("max_value"));
                        numericStats.put("avg", rs.getObject("avg_value"));
                        numericStats.put("stdDev", rs.getObject("std_dev"));
                        distribution.put("numericStats", numericStats);
                    }
                }
                
                // 尝试获取中位数（某些数据库可能不支持）
                try {
                    String medianSql = "SELECT MEDIAN(" + wrapIdentifier(columnName) + ") as median " +
                            "FROM " + wrapIdentifier(tableName);
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(medianSql)) {
                        if (rs.next()) {
                            Map<String, Object> numericStats = (Map<String, Object>) distribution.getOrDefault("numericStats", new HashMap<>());
                            numericStats.put("median", rs.getObject("median"));
                            distribution.put("numericStats", numericStats);
                        }
                    }
                } catch (SQLException e) {
                    log.warn("获取中位数失败: table={}, column={}, error={}", tableName, columnName, e.getMessage());
                }
            } 
            // 字符串类型的统计
            else if (dataType.matches("VARCHAR2|CHAR|CLOB|NVARCHAR2|NCHAR|NCLOB")) {
                String stringSql = "SELECT " +
                        "MIN(LENGTH(" + wrapIdentifier(columnName) + ")) as min_length, " +
                        "MAX(LENGTH(" + wrapIdentifier(columnName) + ")) as max_length, " +
                        "AVG(LENGTH(" + wrapIdentifier(columnName) + ")) as avg_length " +
                        "FROM " + wrapIdentifier(tableName);
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(stringSql)) {
                    if (rs.next()) {
                        Map<String, Object> stringStats = new HashMap<>();
                        stringStats.put("minLength", rs.getObject("min_length"));
                        stringStats.put("maxLength", rs.getObject("max_length"));
                        stringStats.put("avgLength", rs.getObject("avg_length"));
                        distribution.put("stringStats", stringStats);
                    }
                }
            } 
            // 日期时间类型的统计
            else if (dataType.matches("DATE|TIMESTAMP")) {
                String dateSql = "SELECT " +
                        "MIN(" + wrapIdentifier(columnName) + ") as earliest_date, " +
                        "MAX(" + wrapIdentifier(columnName) + ") as latest_date " +
                        "FROM " + wrapIdentifier(tableName);
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(dateSql)) {
                    if (rs.next()) {
                        Map<String, Object> dateStats = new HashMap<>();
                        dateStats.put("earliestDate", rs.getObject("earliest_date"));
                        dateStats.put("latestDate", rs.getObject("latest_date"));
                        distribution.put("dateStats", dateStats);
                    }
                }
            }

            return distribution;
        } catch (Exception e) {
            log.error("获取字段分布信息失败: table={}, column={}, error={}", 
                    tableName, columnName, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<Map<String, Object>> getTableDependencies(String tableName) throws Exception {
        List<Map<String, Object>> dependencies = new ArrayList<>();
        try (Connection conn = getConnection()) {
            // 1. 获取视图依赖
            String viewSql = "SELECT VIEW_NAME as dependent_object, " +
                    "'VIEW' as object_type, " +
                    "TEXT as dependency_definition " +
                    "FROM ALL_VIEWS " +
                    "WHERE OWNER = ? " +
                    "AND TEXT LIKE ?";

            try (PreparedStatement stmt = conn.prepareStatement(viewSql)) {
                stmt.setString(1, getSchema().toUpperCase());
                stmt.setString(2, "%" + tableName.toUpperCase() + "%");

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> dependency = new HashMap<>();
                        dependency.put("name", rs.getString("dependent_object"));
                        dependency.put("type", rs.getString("object_type"));
                        dependency.put("definition", rs.getString("dependency_definition"));
                        dependency.put("dependencyType", "Referenced by View");
                        dependencies.add(dependency);
                    }
                }
            } catch (SQLException e) {
                log.warn("获取视图依赖失败: table={}, error={}", tableName, e.getMessage());
            }

            // 2. 获取触发器依赖
            String triggerSql = "SELECT TRIGGER_NAME as dependent_object, " +
                    "'TRIGGER' as object_type, " +
                    "TRIGGER_BODY as trigger_definition " +
                    "FROM ALL_TRIGGERS " +
                    "WHERE OWNER = ? " +
                    "AND (TABLE_NAME = ? OR TRIGGER_BODY LIKE ?)";

            try (PreparedStatement stmt = conn.prepareStatement(triggerSql)) {
                stmt.setString(1, getSchema().toUpperCase());
                stmt.setString(2, tableName.toUpperCase());
                stmt.setString(3, "%" + tableName.toUpperCase() + "%");

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> dependency = new HashMap<>();
                        dependency.put("name", rs.getString("dependent_object"));
                        dependency.put("type", rs.getString("object_type"));
                        dependency.put("definition", rs.getString("trigger_definition"));
                        dependency.put("dependencyType", "Referenced by Trigger");
                        dependencies.add(dependency);
                    }
                }
            } catch (SQLException e) {
                log.warn("获取触发器依赖失败: table={}, error={}", tableName, e.getMessage());
            }

            // 3. 获取存储过程依赖
            String procSql = "SELECT OBJECT_NAME as dependent_object, " +
                    "OBJECT_TYPE as object_type " +
                    "FROM ALL_OBJECTS " +
                    "WHERE OWNER = ? " +
                    "AND OBJECT_TYPE IN ('PROCEDURE', 'FUNCTION', 'PACKAGE') " +
                    "AND OBJECT_ID IN (" +
                    "  SELECT DISTINCT REFERENCED_OBJECT_ID " +
                    "  FROM ALL_DEPENDENCIES " +
                    "  WHERE REFERENCED_NAME = ? " +
                    "  AND REFERENCED_OWNER = ? " +
                    "  AND REFERENCED_TYPE = 'TABLE'" +
                    ")";

            try (PreparedStatement stmt = conn.prepareStatement(procSql)) {
                stmt.setString(1, getSchema().toUpperCase());
                stmt.setString(2, tableName.toUpperCase());
                stmt.setString(3, getSchema().toUpperCase());

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> dependency = new HashMap<>();
                        dependency.put("name", rs.getString("dependent_object"));
                        dependency.put("type", rs.getString("object_type"));
                        dependency.put("dependencyType", "Referenced by " + rs.getString("object_type"));
                        dependencies.add(dependency);
                    }
                }
            } catch (SQLException e) {
                log.warn("获取存储过程依赖失败: table={}, error={}", tableName, e.getMessage());
            }

            // 4. 获取同义词依赖
            String synonymSql = "SELECT SYNONYM_NAME as dependent_object, " +
                    "'SYNONYM' as object_type " +
                    "FROM ALL_SYNONYMS " +
                    "WHERE OWNER = ? " +
                    "AND TABLE_NAME = ? " +
                    "AND TABLE_OWNER = ?";

            try (PreparedStatement stmt = conn.prepareStatement(synonymSql)) {
                stmt.setString(1, getSchema().toUpperCase());
                stmt.setString(2, tableName.toUpperCase());
                stmt.setString(3, getSchema().toUpperCase());

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> dependency = new HashMap<>();
                        dependency.put("name", rs.getString("dependent_object"));
                        dependency.put("type", rs.getString("object_type"));
                        dependency.put("dependencyType", "Referenced by Synonym");
                        dependencies.add(dependency);
                    }
                }
            } catch (SQLException e) {
                log.warn("获取同义词依赖失败: table={}, error={}", tableName, e.getMessage());
            }

            // 5. 获取表的外键依赖（引用其他表）
            List<Map<String, Object>> foreignKeys = getTableForeignKeyRelations(tableName);
            for (Map<String, Object> fk : foreignKeys) {
                Map<String, Object> dependency = new HashMap<>();
                dependency.put("name", fk.get("referencedTable"));
                dependency.put("type", "TABLE");
                dependency.put("sourceColumn", fk.get("sourceColumn"));
                dependency.put("referencedColumn", fk.get("referencedColumn"));
                dependency.put("constraintName", fk.get("constraintName"));
                dependency.put("dependencyType", "References Table");
                dependencies.add(dependency);
            }

            // 6. 获取被其他表引用的关系
            List<Map<String, Object>> referencedBy = getReferencedByRelations(tableName);
            for (Map<String, Object> ref : referencedBy) {
                Map<String, Object> dependency = new HashMap<>();
                dependency.put("name", ref.get("referencingTable"));
                dependency.put("type", "TABLE");
                dependency.put("sourceColumn", ref.get("referencingColumn"));
                dependency.put("referencedColumn", ref.get("referencedColumn"));
                dependency.put("constraintName", ref.get("constraintName"));
                dependency.put("dependencyType", "Referenced by Foreign Key");
                dependencies.add(dependency);
            }

            // 7. 获取物化视图依赖
            String mvSql = "SELECT MVIEW_NAME as dependent_object, " +
                    "'MATERIALIZED VIEW' as object_type, " +
                    "QUERY as dependency_definition " +
                    "FROM ALL_MVIEWS " +
                    "WHERE OWNER = ? " +
                    "AND QUERY LIKE ?";

            try (PreparedStatement stmt = conn.prepareStatement(mvSql)) {
                stmt.setString(1, getSchema().toUpperCase());
                stmt.setString(2, "%" + tableName.toUpperCase() + "%");

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> dependency = new HashMap<>();
                        dependency.put("name", rs.getString("dependent_object"));
                        dependency.put("type", rs.getString("object_type"));
                        dependency.put("definition", rs.getString("dependency_definition"));
                        dependency.put("dependencyType", "Referenced by Materialized View");
                        dependencies.add(dependency);
                    }
                }
            } catch (SQLException e) {
                log.warn("获取物化视图依赖失败: table={}, error={}", tableName, e.getMessage());
            }

        } catch (Exception e) {
            log.error("获取表依赖关系失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return dependencies;
    }

    @Override
    public List<Map<String, Object>> getColumnValueDistribution(String tableName, String columnName, int topN) throws Exception {
        // 检查列的数据类型
        String dataType = "";
        String sql = String.format(
                    "SELECT DATA_TYPE FROM USER_TAB_COLUMNS " +
            "WHERE TABLE_NAME = '%s' AND COLUMN_NAME = '%s'",
            tableName.toUpperCase(),
            columnName.toUpperCase()
        );
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
                    if (rs.next()) {
                        dataType = rs.getString("DATA_TYPE").toUpperCase();
            }
        }
        
        // 构造分布查询
        String distributionSql;
        if (dataType.contains("DATE") || dataType.contains("TIMESTAMP")) {
            // 日期类型使用 TO_CHAR 格式化
            distributionSql = String.format(
                "SELECT TO_CHAR(%s, 'YYYY-MM-DD') AS value, COUNT(*) AS count " +
                            "FROM %s " +
                            "WHERE %s IS NOT NULL " +
                "GROUP BY TO_CHAR(%s, 'YYYY-MM-DD') " +
                "ORDER BY count DESC " +
                            "FETCH FIRST %d ROWS ONLY",
                    wrapIdentifier(columnName),
                    wrapIdentifier(tableName),
                    wrapIdentifier(columnName),
                    wrapIdentifier(columnName),
                    topN
            );
        } else {
            // 其他类型
            distributionSql = String.format(
                "SELECT %s AS value, COUNT(*) AS count " +
                                    "FROM %s " +
                                    "WHERE %s IS NOT NULL " +
                "GROUP BY %s " +
                "ORDER BY count DESC " +
                "FETCH FIRST %d ROWS ONLY",
                            wrapIdentifier(columnName),
                            wrapIdentifier(tableName),
                            wrapIdentifier(columnName),
                            wrapIdentifier(columnName),
                topN
            );
        }
        
        List<Map<String, Object>> distribution = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(distributionSql)) {
            while (rs.next()) {
                            Map<String, Object> item = new HashMap<>();
                item.put("value", rs.getString("value"));
                item.put("count", rs.getLong("count"));
                            distribution.add(item);
            }
        }
        
        return distribution;
    }

    @Override
    public Map<String, Object> getTableUpdateFrequency(String tableName) throws Exception {
        // 达梦数据库不直接提供更新频率信息
        // 这里提供一个推算的方法，或者可以使用系统监控数据
        Map<String, Object> frequency = new HashMap<>();
        
        // 如果有审计或日志表记录了更新操作，可以查询
        // 这里仅返回一个示例结果
        frequency.put("last_update_time", getTableUpdateTime(tableName));
        frequency.put("updates_per_day", 0); // 无法确定，默认为0
        frequency.put("updates_per_week", 0);
        frequency.put("updates_per_month", 0);
        
        return frequency;
    }

    @Override
    public List<Map<String, Object>> getTableGrowthTrend(String tableName, int days) throws Exception {
        // 达梦数据库不支持直接查询历史数据大小
        // 这里返回一个基本的示例数据
        List<Map<String, Object>> trend = new ArrayList<>();
        
        // 获取当前表大小作为基准
        Long currentSize = getTableSize(tableName);
        
        // 生成过去几天的趋势数据（模拟）
            Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        
            for (int i = 0; i < days; i++) {
            Map<String, Object> point = new HashMap<>();
            // 假设每天减少 5% 的数据量（过去的数据量小于当前）
            double factor = 1.0 - (i * 0.05);
            if (factor < 0.5) factor = 0.5; // 设置下限
            
            long estimatedSize = (long) (currentSize * factor);
            
            point.put("date", sdf.format(calendar.getTime()));
            point.put("size", estimatedSize);
            
            trend.add(point);
            calendar.add(Calendar.DAY_OF_MONTH, -1); // 向前一天
            }
            
            // 反转列表，使其按日期升序排列
            Collections.reverse(trend);
            
            return trend;
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

            // 构建查询SQL
            String columnList = columnNames.stream()
                    .map(this::wrapIdentifier)
                    .collect(Collectors.joining(", "));

            String sql = String.format(
                    "SELECT %s FROM %s FETCH FIRST %d ROWS ONLY",
                    columnList,
                    wrapIdentifier(tableName),
                    sampleSize
            );

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

            // 如果需要随机样本而不是前N行，可以使用以下SQL
            if (samples.isEmpty()) {
                String randomSql = String.format(
                        "SELECT %s FROM %s TABLESAMPLE BERNOULLI(%d)",
                        columnList,
                        wrapIdentifier(tableName),
                        Math.min(sampleSize * 10, 100) // 转换为百分比，限制在100以内
                );

                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(randomSql)) {

                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    while (rs.next() && samples.size() < sampleSize) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            String colName = metaData.getColumnName(i);
                            Object value = rs.getObject(i);
                            row.put(colName, value);
                        }
                        samples.add(row);
                    }
                } catch (SQLException e) {
                    log.warn("获取随机样本失败: table={}, error={}", tableName, e.getMessage());
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
    public Map<String, Object> getColumnValueRange(String tableName, String columnName) throws Exception {
        Map<String, Object> range = new HashMap<>();
        
        // 获取列数据类型
        String dataType = "";
        String typeSql = String.format(
                    "SELECT DATA_TYPE FROM USER_TAB_COLUMNS " +
            "WHERE TABLE_NAME = '%s' AND COLUMN_NAME = '%s'",
            tableName.toUpperCase(),
            columnName.toUpperCase()
        );
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(typeSql)) {
                    if (rs.next()) {
                        dataType = rs.getString("DATA_TYPE").toUpperCase();
            }
        }
        
        // 根据数据类型构造范围查询
        String rangeSql;
        if (dataType.contains("DATE") || dataType.contains("TIMESTAMP")) {
            // 日期类型
            rangeSql = String.format(
                "SELECT MIN(%s) AS min_value, MAX(%s) AS max_value, " +
                "COUNT(DISTINCT %s) AS distinct_count " +
                                "FROM %s WHERE %s IS NOT NULL",
                        wrapIdentifier(columnName),
                        wrapIdentifier(columnName),
                        wrapIdentifier(columnName),
                        wrapIdentifier(tableName),
                        wrapIdentifier(columnName)
                );

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(rangeSql)) {
                    if (rs.next()) {
                    java.sql.Date minDate = rs.getDate("min_value");
                    java.sql.Date maxDate = rs.getDate("max_value");
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    
                    range.put("minValue", minDate != null ? sdf.format(minDate) : null);
                    range.put("maxValue", maxDate != null ? sdf.format(maxDate) : null);
                    range.put("distinctCount", rs.getLong("distinct_count"));
                }
            }
        } else if (dataType.contains("NUMBER") || dataType.contains("INT") || 
                  dataType.contains("FLOAT") || dataType.contains("DOUBLE") ||
                  dataType.contains("DECIMAL")) {
            // 数值类型
            rangeSql = String.format(
                "SELECT MIN(%s) AS min_value, MAX(%s) AS max_value, " +
                "COUNT(DISTINCT %s) AS distinct_count " +
                                    "FROM %s WHERE %s IS NOT NULL",
                            wrapIdentifier(columnName),
                            wrapIdentifier(columnName),
                            wrapIdentifier(columnName),
                            wrapIdentifier(tableName),
                            wrapIdentifier(columnName)
                    );

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(rangeSql)) {
                        if (rs.next()) {
                    range.put("minValue", rs.getObject("min_value"));
                    range.put("maxValue", rs.getObject("max_value"));
                    range.put("distinctCount", rs.getLong("distinct_count"));
                }
            }
        } else {
            // 字符串类型
            rangeSql = String.format(
                "SELECT MIN(%s) AS min_value, MAX(%s) AS max_value, " +
                "COUNT(DISTINCT %s) AS distinct_count " +
                                "FROM %s WHERE %s IS NOT NULL",
                        wrapIdentifier(columnName),
                        wrapIdentifier(columnName),
                        wrapIdentifier(columnName),
                        wrapIdentifier(tableName),
                        wrapIdentifier(columnName)
                );

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(rangeSql)) {
                    if (rs.next()) {
                        range.put("minValue", rs.getString("min_value"));
                        range.put("maxValue", rs.getString("max_value"));
                    range.put("distinctCount", rs.getLong("distinct_count"));
                }
            }
        }
        
        return range;
    }

    @Override
    public Map<String, String> getStoredProcedureDefinitions(String tableName) throws Exception {
        Map<String, String> procedures = new HashMap<>();

        try {
            // 达梦数据库存储过程查询语法
            String sql = "SELECT NAME, TEXT FROM ALL_SOURCE " +
                        "WHERE TYPE = 'PROCEDURE' AND OWNER = USER";
            
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                    while (rs.next()) {
                    String procName = rs.getString("NAME");
                    String procText = rs.getString("TEXT");
                    
                    // 检查是否与指定表相关
                    if (tableName != null && !tableName.isEmpty()) {
                        if (procText.toUpperCase().contains(tableName.toUpperCase())) {
                            procedures.put(procName, procText);
                        }
                    } else {
                        procedures.put(procName, procText);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取存储过程定义失败: table={}, error={}", tableName, e.getMessage(), e);
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
        
        // 达梦数据库分区查询，类似Oracle
        String partitionListSql = 
                "SELECT PARTITION_NAME, HIGH_VALUE FROM ALL_TAB_PARTITIONS " + 
                "WHERE TABLE_OWNER = ? AND TABLE_NAME = ? ORDER BY PARTITION_POSITION";
        
        try (Connection conn = getConnection()) {
            boolean hasPartitions = false;
            
            try {
                // 先检查表是否分区表
                String checkPartitionedSql = 
                    "SELECT COUNT(*) FROM ALL_TABLES WHERE OWNER = ? AND TABLE_NAME = ? AND PARTITIONED = 'YES'";
                
                try (PreparedStatement stmt = conn.prepareStatement(checkPartitionedSql)) {
                    stmt.setString(1, getSchema().toUpperCase());
                    stmt.setString(2, tableName.toUpperCase());
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            hasPartitions = true;
                        }
                    }
                }
                
                // 如果是分区表，查询分区信息
                if (hasPartitions) {
                    try (PreparedStatement stmt = conn.prepareStatement(partitionListSql)) {
                        stmt.setString(1, getSchema().toUpperCase());
                        stmt.setString(2, tableName.toUpperCase());
                        
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                String highValue = rs.getString("HIGH_VALUE");
                                // 解析高水位值，通常是分区键的上限
                                if (highValue != null && highValue.contains("'")) {
                                    int start = highValue.indexOf("'") + 1;
                                    int end = highValue.lastIndexOf("'");
                                    if (start < end) {
                                        partitions.add(highValue.substring(start, end));
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("查询达梦数据库分区信息失败: {}", e.getMessage());
                hasPartitions = false;
            }
            
            // 如果没有找到分区信息或不是分区表，使用通用查询
            if (!hasPartitions || partitions.isEmpty()) {
                // 使用SQL查询分区列表（基于指定的分区字段查询唯一值）
                String sql = "SELECT DISTINCT " + wrapIdentifier(partitionField) + 
                             " FROM " + wrapIdentifier(tableName) + 
                             " ORDER BY " + wrapIdentifier(partitionField);
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    
                    while (rs.next()) {
                        Object value = rs.getObject(1);
                        if (value != null) {
                            partitions.add(value.toString());
                        }
                    }
                } catch (Exception e) {
                    log.error("获取表的分区值失败: {}", e.getMessage());
                }
            }
        }
        
        return partitions;
    }
    
    /**
     * 获取创建临时表的SQL - 达梦数据库实现
     *
     * @param tempTableName 临时表名称
     * @param sourceTableName 源表名称（用于复制结构）
     * @param preserveRows 是否在会话结束后保留数据
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
        
        // 获取主键信息
        List<String> primaryKeys = getPrimaryKeys(sourceTableName);
        
        StringBuilder sql = new StringBuilder();
        // 达梦数据库临时表语法：使用GLOBAL TEMPORARY TABLE
        sql.append("CREATE GLOBAL TEMPORARY TABLE ").append(wrapIdentifier(tempTableName)).append(" (");
        
        // 添加列定义
        for (int i = 0; i < columns.size(); i++) {
            Map<String, Object> column = columns.get(i);
            if (i > 0) {
                sql.append(", ");
            }
            
            String columnName = column.get("name").toString();
            String dataType = column.get("type").toString();
            
            sql.append(wrapIdentifier(columnName)).append(" ").append(dataType);
            
            // 处理NULL约束
            if (column.containsKey("nullable")) {
                boolean isNullable = Boolean.parseBoolean(column.get("nullable").toString());
                if (!isNullable) {
                    sql.append(" NOT NULL");
                }
            }
            
            // 处理默认值
            if (column.containsKey("default") && column.get("default") != null) {
                String defaultValue = column.get("default").toString();
                sql.append(" DEFAULT ").append(defaultValue);
            }
        }
        
        // 添加主键
        if (primaryKeys != null && !primaryKeys.isEmpty()) {
            sql.append(", PRIMARY KEY (");
            for (int i = 0; i < primaryKeys.size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append(wrapIdentifier(primaryKeys.get(i)));
            }
            sql.append(")");
        }
        
        sql.append(")");
        
        // 达梦数据库临时表特有设置：ON COMMIT PRESERVE ROWS 或 ON COMMIT DELETE ROWS
        if (preserveRows) {
            sql.append(" ON COMMIT PRESERVE ROWS");
        } else {
            sql.append(" ON COMMIT DELETE ROWS");
        }
        
        return sql.toString();
    }

    /**
     * 执行更新操作
     * @param sql SQL语句
     * @return 更新的行数
     * @throws Exception 如果更新过程中发生错误
     */
    @Override
    public int executeUpdate(String sql) throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            return stmt.executeUpdate(sql);
        } catch (SQLException e) {
            log.error("执行更新操作失败: {}, 错误: {}", sql, e.getMessage());
            throw e;
        }
    }
} 