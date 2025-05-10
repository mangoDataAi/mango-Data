package com.mango.test.database.service.impl.datasource.handlers;

import com.mango.test.database.entity.ColumnDefinition;
import com.mango.test.database.entity.DataSource;
import com.mango.test.database.entity.IndexDefinition;
import com.mango.test.database.entity.TableDefinition;
import com.mango.test.database.service.impl.datasource.AbstractDatabaseHandler;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.Reader;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OracleHandler extends AbstractDatabaseHandler {

    private static final String DRIVER_CLASS = "oracle.jdbc.OracleDriver";
    private static final String DEFAULT_PORT = "1521";
    private static final String URL_TEMPLATE = "jdbc:oracle:thin:@%s:%s:%s";

    private static final int DEFAULT_VARCHAR_LENGTH = 4000;
    private static final int MAX_VARCHAR_LENGTH = 4000;

    private static final Map<String, String> TYPE_MAPPING = new HashMap<>();
    private static final Map<String, Integer> DEFAULT_LENGTH_MAPPING = new HashMap<>();

    private static final Map<String, Map<String, String>> COMMON_TYPE_MAPPING = new HashMap<>();

    // 添加在类顶部的变量定义区域
    private static final Logger log = LoggerFactory.getLogger(OracleHandler.class);

    static {
        // 字符串类型
        TYPE_MAPPING.put("string", "VARCHAR2");
        TYPE_MAPPING.put("char", "CHAR");
        TYPE_MAPPING.put("text", "CLOB");
        TYPE_MAPPING.put("nchar", "NCHAR");
        TYPE_MAPPING.put("nvarchar", "NVARCHAR2");
        TYPE_MAPPING.put("ntext", "NCLOB");

        // 数值类型
        TYPE_MAPPING.put("tinyint", "NUMBER(3)");
        TYPE_MAPPING.put("smallint", "NUMBER(5)");
        TYPE_MAPPING.put("mediumint", "NUMBER(7)");
        TYPE_MAPPING.put("int", "NUMBER(10)");
        TYPE_MAPPING.put("integer", "NUMBER(10)");
        TYPE_MAPPING.put("bigint", "NUMBER(19)");
        TYPE_MAPPING.put("float", "BINARY_FLOAT");
        TYPE_MAPPING.put("double", "BINARY_DOUBLE");
        TYPE_MAPPING.put("decimal", "NUMBER");
        TYPE_MAPPING.put("number", "NUMBER");

        // 日期时间类型
        TYPE_MAPPING.put("date", "DATE");
        TYPE_MAPPING.put("time", "TIMESTAMP");
        TYPE_MAPPING.put("datetime", "TIMESTAMP");
        TYPE_MAPPING.put("timestamp", "TIMESTAMP");
        TYPE_MAPPING.put("year", "NUMBER(4)");

        // 二进制类型
        TYPE_MAPPING.put("binary", "RAW");
        TYPE_MAPPING.put("varbinary", "RAW");
        TYPE_MAPPING.put("blob", "BLOB");
        TYPE_MAPPING.put("longblob", "BLOB");
        TYPE_MAPPING.put("raw", "RAW");
        TYPE_MAPPING.put("longraw", "LONG RAW");

        // 其他类型
        TYPE_MAPPING.put("boolean", "NUMBER(1)");
        TYPE_MAPPING.put("clob", "CLOB");
        TYPE_MAPPING.put("nclob", "NCLOB");

        // 默认长度映射
        DEFAULT_LENGTH_MAPPING.put("VARCHAR2", 255);
        DEFAULT_LENGTH_MAPPING.put("NVARCHAR2", 255);
        DEFAULT_LENGTH_MAPPING.put("CHAR", 1);
        DEFAULT_LENGTH_MAPPING.put("NCHAR", 1);
        DEFAULT_LENGTH_MAPPING.put("RAW", 2000);
        DEFAULT_LENGTH_MAPPING.put("NUMBER", 10);

        // MySQL类型映射
        Map<String, String> mysqlMapping = new HashMap<>();
        mysqlMapping.put("VARCHAR", "VARCHAR2");
        mysqlMapping.put("TINYINT", "NUMBER(3)");
        mysqlMapping.put("SMALLINT", "NUMBER(5)");
        mysqlMapping.put("INT", "NUMBER(10)");
        mysqlMapping.put("BIGINT", "NUMBER(19)");
        mysqlMapping.put("FLOAT", "FLOAT");
        mysqlMapping.put("DOUBLE", "DOUBLE PRECISION");
        mysqlMapping.put("DECIMAL", "NUMBER");
        mysqlMapping.put("DATE", "DATE");
        mysqlMapping.put("DATETIME", "TIMESTAMP");
        mysqlMapping.put("TIMESTAMP", "TIMESTAMP");
        mysqlMapping.put("TEXT", "CLOB");
        mysqlMapping.put("BLOB", "BLOB");
        mysqlMapping.put("BOOLEAN", "NUMBER(1)");
        COMMON_TYPE_MAPPING.put("mysql", mysqlMapping);

        // PostgreSQL类型映射
        Map<String, String> postgresMapping = new HashMap<>();
        postgresMapping.put("VARCHAR", "VARCHAR2");
        postgresMapping.put("TEXT", "CLOB");
        postgresMapping.put("INTEGER", "NUMBER(10)");
        postgresMapping.put("BIGINT", "NUMBER(19)");
        postgresMapping.put("NUMERIC", "NUMBER");
        postgresMapping.put("TIMESTAMP", "TIMESTAMP");
        postgresMapping.put("BYTEA", "BLOB");
        COMMON_TYPE_MAPPING.put("postgresql", postgresMapping);

        // SQL Server类型映射
        Map<String, String> sqlserverMapping = new HashMap<>();
        sqlserverMapping.put("VARCHAR", "VARCHAR2");
        sqlserverMapping.put("NVARCHAR", "NVARCHAR2");
        sqlserverMapping.put("TEXT", "CLOB");
        sqlserverMapping.put("NTEXT", "NCLOB");
        sqlserverMapping.put("INT", "NUMBER(10)");
        sqlserverMapping.put("BIGINT", "NUMBER(19)");
        sqlserverMapping.put("DECIMAL", "NUMBER");
        sqlserverMapping.put("DATETIME", "TIMESTAMP");
        sqlserverMapping.put("DATETIME2", "TIMESTAMP");
        sqlserverMapping.put("IMAGE", "BLOB");
        COMMON_TYPE_MAPPING.put("sqlserver", sqlserverMapping);
    }

    public OracleHandler(DataSource dataSource) {
        super(dataSource);
    }


    @Override
    public String getJdbcUrl() {
        String host = getHost();
        String port = getPort() == null ? DEFAULT_PORT : getPort();
        String database = getDatabase();
        String properties = getDatabaseProperty("properties", "");
        
        // 检查是否是连接字符串格式（TNSNAMES格式）
        if (database != null && database.contains("=")) {
            return "jdbc:oracle:thin:@" + database;
        }
        
        // 检查是否使用SERVICE_NAME
        if (properties != null && properties.contains("SERVICE_NAME")) {
            return String.format(
                "jdbc:oracle:thin:@//%s:%s/%s",
                host, port, database
            );
        }
        
        // 默认使用SID
        return String.format(URL_TEMPLATE, host, port, database);
    }

    @Override
    public String getSchema() {
        return dataSource.getUsername().toUpperCase();
    }

    @Override
    public void setSchema(Connection conn, String schema) throws SQLException {
        // Oracle使用ALTER SESSION设置当前模式
        if (schema != null && !schema.isEmpty()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER SESSION SET CURRENT_SCHEMA = " + wrapIdentifier(schema));
            }
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
        return null;
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
        List<String> systemDatabases = new ArrayList<>();
        systemDatabases.add("SYS");
        systemDatabases.add("SYSTEM");
        systemDatabases.add("SYSAUX");
        systemDatabases.add("TEMP");
        systemDatabases.add("DBSNMP");
        systemDatabases.add("CTXSYS");
        systemDatabases.add("MDSYS");
        systemDatabases.add("ORDSYS");
        systemDatabases.add("OUTLN");
        systemDatabases.add("DMSYS");
        return systemDatabases;
    }

    @Override
    public String wrapValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        
        if (value instanceof String) {
            // 处理字符串值，注意Oracle中字符串用单引号
            return "'" + ((String) value).replace("'", "''") + "'";
        } else if (value instanceof Date) {
            // Oracle日期格式处理
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return "TO_TIMESTAMP('" + sdf.format((Date) value) + "', 'YYYY-MM-DD HH24:MI:SS')";
        } else if (value instanceof Boolean) {
            // Oracle没有布尔类型，使用数字1和0
            return (Boolean) value ? "1" : "0";
        } else if (value instanceof byte[]) {
            // 二进制数据用HEXTORAW转换
            return "HEXTORAW('" + bytesToHex((byte[]) value) + "')";
        } else if (value instanceof Reader) {
            // 对 CLOB 数据的处理
            return "EMPTY_CLOB()";  // 在实际插入时需要单独处理
        } else if (value instanceof InputStream) {
            // 对 BLOB 数据的处理
            return "EMPTY_BLOB()";  // 在实际插入时需要单独处理
        }
        
            return value.toString();
        }

    // 辅助方法，将二进制数据转换为十六进制字符串
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    @Override
    public String wrapIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
        
        // Oracle 标识符最大长度为30个字符（12c以前）或128个字符（12c及以后）
        if (identifier.length() > 30) {
            // 目前只是记录警告，但实际上有可能在某些版本上发生截断
            // 可以考虑提供一个配置选项，允许自动截断或生成一个哈希值来替代长标识符
            log.warn("Identifier '{}' exceeds Oracle's 30 character limit in pre-12c versions. It may be truncated.", identifier);
        }
        
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public String generatePageSql(String sql, int offset, int limit) {
        try {
            return getVersionAwarePageSql(sql, offset, limit);
        } catch (Exception e) {
            log.error("Error generating pagination SQL", e);
            
            // 回退到简单的分页实现
            try {
                Connection conn = getConnection();
                boolean isOracle12c = isOracle12cOrHigher(conn);
                closeConnection(conn);
                
                if (isOracle12c) {
                    // Oracle 12c 及以上版本支持 OFFSET FETCH 语法
                    return sql + String.format(
                        " OFFSET %d ROWS FETCH NEXT %d ROWS ONLY",
                        offset, limit
                    );
                } else {
                    // Oracle 11g 及以下版本使用子查询和 ROWNUM
        return String.format(
                        "SELECT * FROM (SELECT t.*, ROWNUM rn FROM (%s) t WHERE ROWNUM <= %d) WHERE rn > %d",
                sql, offset + limit, offset
        );
                }
            } catch (SQLException ex) {
                // 如果连接也失败，使用最简单的Oracle 11g风格作为最后的回退
                return String.format(
                    "SELECT * FROM (SELECT t.*, ROWNUM rn FROM (%s) t WHERE ROWNUM <= %d) WHERE rn > %d",
                    sql, offset + limit, offset
                );
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * 生成Oracle数据库的LIMIT子句
     * Oracle没有直接的LIMIT语法，需要根据版本使用不同的写法
     * 
     * @param offset 起始位置
     * @param limit  返回记录的最大数量
     * @return 格式化后的LIMIT子句
     */
    @Override
    public String getLimitClause(long offset, int limit) {
        try {
            Connection conn = getConnection();
            boolean isOracle12c = isOracle12cOrHigher(conn);
            closeConnection(conn);
            
            if (isOracle12c) {
                // Oracle 12c 及以上版本支持 OFFSET FETCH 语法
                return String.format("OFFSET %d ROWS FETCH NEXT %d ROWS ONLY", offset, limit);
            } else {
                // Oracle 11g 及以下版本使用ROWNUM
                // 注意：此处返回的是WHERE子句，需要在外部与其他WHERE条件组合
                return String.format("ROWNUM <= %d", offset + limit);
            }
        } catch (Exception e) {
            log.error("Error generating limit clause", e);
            // 默认使用Oracle 11g语法
            return String.format("ROWNUM <= %d", offset + limit);
        }
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
        return "Oracle";
    }

    @Override
    public String getTableExistsSql(String tableName) {
        return String.format(
                "SELECT COUNT(*) FROM ALL_TABLES WHERE OWNER = '%s' AND TABLE_NAME = '%s'",
                getSchema(),
                tableName.toUpperCase()
        );
    }

    @Override
    public String getCreateTableSql(String tableName, List<ColumnDefinition> columns,
                                    String tableComment, String engine, String charset, String collate) {

        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(wrapIdentifier(tableName)).append(" (\n");

        for (int i = 0; i < columns.size(); i++) {
            ColumnDefinition column = columns.get(i);
            
            // 确保列类型与Oracle兼容
            adjustOracleColumnType(column);
            
            sql.append("    ")
               .append(formatFieldDefinition(
                    column.getName(),
                    column.getType(),
                    column.getLength(),
                    column.getPrecision(),
                    column.getScale(),
                    column.isNullable(),
                    column.getDefaultValue(),
                    column.getComment()
               ));
            
            if (i < columns.size() - 1) {
                sql.append(",\n");
            }
        }
        
        sql.append("\n)");

        // Oracle 不支持直接在CREATE TABLE语句中设置ENGINE, CHARSET, COLLATE
        // 注释和其他属性需要通过单独的语句添加
        
        return sql.toString();
    }

    @Override
    public String getDropTableSql(String tableName) {
        return "DROP TABLE " + wrapIdentifier(tableName) + " PURGE";
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
        sql.append("ALTER TABLE ").append(wrapIdentifier(tableName));
        sql.append(" ADD ").append(wrapIdentifier(column.getName()));
        sql.append(" ").append(column.getType());

        // 长度/精度
        if (column.getLength() != null) {
            if (column.getScale() != null) {
                sql.append("(").append(column.getPrecision())
                        .append(",").append(column.getScale()).append(")");
            } else {
                sql.append("(").append(column.getLength()).append(")");
            }
        }

        // 是否可空
        sql.append(column.isNullable() ? " NULL" : " NOT NULL");

        // 默认值
        if (column.getDefaultValue() != null) {
            sql.append(" DEFAULT ").append(wrapValue(column.getDefaultValue()));
        }

        // Oracle不支持AFTER语法,忽略afterColumn参数

        return sql.toString();
    }

    @Override
    public String getModifyColumnSql(String tableName, String columnName, String newDefinition) {
        return String.format(
            "ALTER TABLE %s MODIFY (%s)",
            wrapIdentifier(tableName),
            newDefinition
        );
    }

    @Override
    public String getDropColumnSql(String tableName, String columnName) {
        return "ALTER TABLE " + wrapIdentifier(tableName) + " DROP COLUMN " + wrapIdentifier(columnName);
    }

    @Override
    public String getRenameTableSql(String oldTableName, String newTableName) {
        return String.format(
            "ALTER TABLE %s RENAME TO %s",
            wrapIdentifier(oldTableName),
            wrapIdentifier(newTableName)
        );
    }

    @Override
    public String getShowCreateTableSql(String tableName) {
        // Oracle没有类似MySQL的SHOW CREATE TABLE语句
        // 使用字典视图查询表结构
        return String.format(
            "SELECT DBMS_METADATA.GET_DDL('TABLE', '%s') AS CREATE_TABLE_SQL FROM DUAL",
            tableName.toUpperCase()
        );
    }

    @Override
    public String getShowTablesSql() {
        // 查询用户拥有的所有表
        return "SELECT TABLE_NAME FROM USER_TABLES ORDER BY TABLE_NAME";
    }

    @Override
    public String getShowColumnsSql(String tableName) {
        // 查询表的所有列信息
        return String.format(
            "SELECT COLUMN_NAME, DATA_TYPE, DATA_LENGTH, DATA_PRECISION, DATA_SCALE, " +
            "NULLABLE, DATA_DEFAULT, COLUMN_ID " +
            "FROM USER_TAB_COLUMNS WHERE TABLE_NAME = '%s' ORDER BY COLUMN_ID",
            tableName.toUpperCase()
        );
    }

    @Override
    public Class<?> mapDbTypeToJavaType(String dbType) {
        dbType = dbType.toUpperCase();
        switch (dbType) {
            case "NUMBER":
                return BigDecimal.class;
            case "VARCHAR2":
            case "NVARCHAR2":
            case "CHAR":
            case "NCHAR":
            case "CLOB":
            case "NCLOB":
                return String.class;
            case "DATE":
            case "TIMESTAMP":
                return Date.class;
            case "BLOB":
            case "RAW":
            case "LONG RAW":
                return byte[].class;
            default:
                return String.class;
        }
    }

    @Override
    public Map<String, String> getTypeMapping() {
        if (TYPE_MAPPING.isEmpty()) {
            // Oracle数据类型映射
            TYPE_MAPPING.put("varchar", "VARCHAR2");
            TYPE_MAPPING.put("char", "CHAR");
            TYPE_MAPPING.put("text", "CLOB");
            TYPE_MAPPING.put("longtext", "CLOB");
            TYPE_MAPPING.put("mediumtext", "CLOB");
            TYPE_MAPPING.put("tinytext", "VARCHAR2");
            TYPE_MAPPING.put("int", "NUMBER(10,0)");
            TYPE_MAPPING.put("integer", "NUMBER(10,0)");
            TYPE_MAPPING.put("tinyint", "NUMBER(3,0)");
            TYPE_MAPPING.put("smallint", "NUMBER(5,0)");
            TYPE_MAPPING.put("mediumint", "NUMBER(7,0)");
            TYPE_MAPPING.put("bigint", "NUMBER(19,0)");
            TYPE_MAPPING.put("float", "FLOAT");
            TYPE_MAPPING.put("double", "FLOAT(24)");
            TYPE_MAPPING.put("decimal", "NUMBER");
            TYPE_MAPPING.put("date", "DATE");
            TYPE_MAPPING.put("datetime", "TIMESTAMP");
            TYPE_MAPPING.put("timestamp", "TIMESTAMP");
            TYPE_MAPPING.put("time", "TIMESTAMP");
            TYPE_MAPPING.put("year", "NUMBER(4,0)");
            TYPE_MAPPING.put("blob", "BLOB");
            TYPE_MAPPING.put("longblob", "BLOB");
            TYPE_MAPPING.put("mediumblob", "BLOB");
            TYPE_MAPPING.put("tinyblob", "BLOB");
            TYPE_MAPPING.put("binary", "RAW");
            TYPE_MAPPING.put("varbinary", "RAW");
            TYPE_MAPPING.put("bit", "NUMBER(1,0)");
            TYPE_MAPPING.put("boolean", "NUMBER(1,0)");
            TYPE_MAPPING.put("enum", "VARCHAR2");
            TYPE_MAPPING.put("set", "VARCHAR2");
        }
        return TYPE_MAPPING;
    }

    @Override
    public Map<String, Integer> getDefaultLengthMapping() {
        return DEFAULT_LENGTH_MAPPING;
    }

    @Override
    public boolean isValidFieldLength(String type, int length) {
        type = type.toUpperCase();
        switch (type) {
            case "VARCHAR2":
            case "NVARCHAR2":
                return length > 0 && length <= 4000;
            case "CHAR":
            case "NCHAR":
                return length > 0 && length <= 2000;
            case "RAW":
                return length > 0 && length <= 2000;
            case "NUMBER":
                return length > 0 && length <= 38;
            default:
                return true;
        }
    }

    @Override
    public String formatFieldDefinition(String name, String type, Integer length,
                                        Integer precision, Integer scale, boolean nullable, String defaultValue, String comment) {
        StringBuilder definition = new StringBuilder();
        definition.append(wrapIdentifier(name)).append(" ");

        type = type.toUpperCase();
        
        // 根据不同数据类型格式化定义
        if (type.equals("NUMBER") && precision != null) {
            if (scale != null && scale > 0) {
                definition.append(type).append("(").append(precision).append(",").append(scale).append(")");
            } else {
                definition.append(type).append("(").append(precision).append(")");
            }
        } else if (type.equals("VARCHAR2") && length != null) {
            definition.append(type).append("(").append(length).append(" BYTE)");
        } else if (type.equals("CHAR") && length != null) {
            definition.append(type).append("(").append(length).append(" BYTE)");
        } else {
            // 其他类型如CLOB, BLOB等不需要长度
            definition.append(type);
        }
        
        // 非空约束
        if (!nullable) {
            definition.append(" NOT NULL");
        }
        
        // 默认值处理
        if (defaultValue != null && !defaultValue.isEmpty()) {
            // 为字符串类型添加引号
            if (type.contains("VARCHAR") || type.contains("CHAR") || type.equals("CLOB")) {
                if (!defaultValue.startsWith("'") && !defaultValue.endsWith("'")) {
                    defaultValue = "'" + defaultValue + "'";
                }
            }
            definition.append(" DEFAULT ").append(defaultValue);
        }
        
        // Oracle不支持在列定义中添加注释，需要通过单独的语句添加
        
        return definition.toString();
    }

    @Override
    public String convertFromOtherDbType(String sourceType, String sourceDbType) {
        sourceType = sourceType.toUpperCase();
        sourceDbType = sourceDbType.toLowerCase();

        Map<String, String> mappings = COMMON_TYPE_MAPPING.get(sourceDbType);
        if (mappings != null && mappings.containsKey(sourceType)) {
            return mappings.get(sourceType);
        }

        return TYPE_MAPPING.getOrDefault(sourceType.toLowerCase(), "VARCHAR2");
    }

    @Override
    public Map<String, Map<String, String>> getCommonTypeMapping() {
        return COMMON_TYPE_MAPPING;
    }

    @Override
    public String getAddTableCommentSql(String tableName, String comment) {
        if (comment == null || comment.isEmpty()) {
            return "";
        }
        
        return String.format(
            "COMMENT ON TABLE %s IS '%s'",
                wrapIdentifier(tableName),
            comment.replace("'", "''")
        );
    }

    @Override
    public String getModifyTableCommentSql(String tableName, String comment) {
        return getAddTableCommentSql(tableName, comment);
    }

    @Override
    public String getAddColumnCommentSql(String tableName, String columnName, String comment) {
        if (comment == null || comment.isEmpty()) {
            return "";
        }
        
        return String.format(
            "COMMENT ON COLUMN %s.%s IS '%s'",
                wrapIdentifier(tableName),
                wrapIdentifier(columnName),
            comment.replace("'", "''")
        );
    }

    @Override
    public String getModifyColumnCommentSql(String tableName, String columnName, String comment) {
        return getAddColumnCommentSql(tableName, columnName, comment);
    }

    @Override
    public String getAlterTableCharsetSql(String tableName, String charset, String collate) {
        // Oracle不支持直接修改表字符集，需要使用重建表的方式
        // 返回注释，提示用户Oracle中的处理方式
        return "-- Oracle does not support direct ALTER TABLE CHARSET. " +
               "-- To change character set, you need to recreate the table " +
               "-- or modify at database/tablespace level.";
    }

    @Override
    public String getAlterTableEngineSql(String tableName, String engine) {
        // Oracle不支持存储引擎概念
        return "-- Oracle does not support storage engines like MySQL. " +
               "-- This operation is not applicable in Oracle.";
    }

    @Override
    public String getShowIndexesSql(String tableName) {
        return String.format(
            "SELECT UI.INDEX_NAME, UI.UNIQUENESS, UIC.COLUMN_NAME, UIC.COLUMN_POSITION " +
            "FROM USER_INDEXES UI " +
            "JOIN USER_IND_COLUMNS UIC ON UI.INDEX_NAME = UIC.INDEX_NAME " +
            "WHERE UI.TABLE_NAME = '%s' " +
            "ORDER BY UI.INDEX_NAME, UIC.COLUMN_POSITION",
                tableName.toUpperCase()
        );
    }

    @Override
    public String getAddIndexSql(String tableName, String indexName, List<String> columns, boolean unique) {
        StringBuilder sb = new StringBuilder();
        
        sb.append(unique ? "CREATE UNIQUE INDEX " : "CREATE INDEX ");
        sb.append(wrapIdentifier(indexName)).append(" ON ");
        sb.append(wrapIdentifier(tableName)).append(" (");
        
        for (int i = 0; i < columns.size(); i++) {
            sb.append(wrapIdentifier(columns.get(i)));
            if (i < columns.size() - 1) {
                sb.append(", ");
            }
        }
        
        sb.append(")");
        
        return sb.toString();
    }

    @Override
    public String getDropIndexSql(String tableName, String indexName) {
        // Oracle中删除索引不需要表名
        return String.format("DROP INDEX %s", wrapIdentifier(indexName));
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
        return "CLOB";
    }

    @Override
    public String getDefaultIntegerType() {
        return "NUMBER(10,0)";
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
        return "TIMESTAMP";
    }

    @Override
    public String getDefaultDateTimeType() {
        return "TIMESTAMP";
    }

    @Override
    public String getDefaultBooleanType() {
        return "NUMBER(1,0)";
    }

    @Override
    public String getDefaultBlobType() {
        return "BLOB";
    }

    @Override
    public String mapJavaTypeToDbType(Class<?> javaType) {
        if (String.class.equals(javaType)) {
            return "VARCHAR2";
        }
        if (Integer.class.equals(javaType) || int.class.equals(javaType)) {
            return "NUMBER(10)";
        }
        if (Long.class.equals(javaType) || long.class.equals(javaType)) {
            return "NUMBER(19)";
        }
        if (Double.class.equals(javaType) || double.class.equals(javaType)) {
            return "NUMBER(20,4)";
        }
        if (Float.class.equals(javaType) || float.class.equals(javaType)) {
            return "NUMBER(10,2)";
        }
        if (BigDecimal.class.equals(javaType)) {
            return "NUMBER(20,4)";
        }
        if (Boolean.class.equals(javaType) || boolean.class.equals(javaType)) {
            return "NUMBER(1)";
        }
        if (Date.class.equals(javaType)) {
            return "TIMESTAMP";
        }
        if (byte[].class.equals(javaType)) {
            return "BLOB";
        }
        return "VARCHAR2";
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

                // 调整字段长度和精度
                adjustOracleColumnType(column);
            }

            // 移除不支持的属性
            tableDefinition.setEngine(null);
            tableDefinition.setCharset(null);
            tableDefinition.setCollate(null);

            // 设置Oracle特定属性
            if (tableDefinition.getExtraProperties() == null) {
                tableDefinition.setExtraProperties(new HashMap<>());
            }
            if (!tableDefinition.getExtraProperties().containsKey("TABLESPACE")) {
                tableDefinition.getExtraProperties().put("TABLESPACE", "USERS");
            }

            // 生成Oracle建表语句
            return generateCreateTableSql(tableDefinition);
        } catch (Exception e) {
            log.error("Failed to convert CREATE TABLE SQL from {} to Oracle: {}", sourceDbType, e.getMessage());
            throw new RuntimeException("Failed to convert CREATE TABLE SQL", e);
        }
    }

    /**
     * 调整Oracle列类型
     */
    private void adjustOracleColumnType(ColumnDefinition column) {
        String type = column.getType().toUpperCase();

        // VARCHAR在Oracle中是VARCHAR2，且长度限制为4000
        if ("VARCHAR".equalsIgnoreCase(type)) {
            column.setType("VARCHAR2");
            if (column.getLength() == null) {
                column.setLength(DEFAULT_VARCHAR_LENGTH);
            } else if (column.getLength() > MAX_VARCHAR_LENGTH) {
                column.setLength(MAX_VARCHAR_LENGTH);
            }
        }
        
        // TEXT类型转换为CLOB
        else if ("TEXT".equalsIgnoreCase(type) || "LONGTEXT".equalsIgnoreCase(type) || 
                 "MEDIUMTEXT".equalsIgnoreCase(type) || "TINYTEXT".equalsIgnoreCase(type)) {
            column.setType("CLOB");
            column.setLength(null); // CLOB不需要长度
        }
        
        // INT类型转换为NUMBER(10,0)
        else if ("INT".equalsIgnoreCase(type) || "INTEGER".equalsIgnoreCase(type)) {
            column.setType("NUMBER");
            column.setPrecision(10);
                column.setScale(0);
            column.setLength(null);
            }
        
        // SMALLINT转换为NUMBER(5,0)
        else if ("SMALLINT".equalsIgnoreCase(type)) {
            column.setType("NUMBER");
            column.setPrecision(5);
            column.setScale(0);
            column.setLength(null);
        }
        
        // BIGINT转换为NUMBER(19,0)
        else if ("BIGINT".equalsIgnoreCase(type)) {
            column.setType("NUMBER");
            column.setPrecision(19);
            column.setScale(0);
            column.setLength(null);
        }
        
        // FLOAT和DOUBLE处理
        else if ("FLOAT".equalsIgnoreCase(type)) {
            column.setType("FLOAT");
            column.setLength(null);
        }
        else if ("DOUBLE".equalsIgnoreCase(type)) {
            column.setType("FLOAT");
            column.setPrecision(24); // Oracle FLOAT(24)相当于DOUBLE PRECISION
            column.setLength(null);
        }

        // DECIMAL/NUMERIC处理
        else if ("DECIMAL".equalsIgnoreCase(type) || "NUMERIC".equalsIgnoreCase(type)) {
            column.setType("NUMBER");
            // 如果未指定精度和小数位
            if (column.getPrecision() == null) {
                column.setPrecision(10);
            }
            if (column.getScale() == null) {
                column.setScale(0);
            }
            column.setLength(null);
        }

        // DATETIME/TIMESTAMP处理
        else if ("DATETIME".equalsIgnoreCase(type) || "TIMESTAMP".equalsIgnoreCase(type)) {
            column.setType("TIMESTAMP");
            column.setLength(null);
        }
        
        // DATE处理
        else if ("DATE".equalsIgnoreCase(type)) {
            column.setType("DATE");
            column.setLength(null);
        }

        // BOOLEAN处理
        else if ("BOOLEAN".equalsIgnoreCase(type) || "BOOL".equalsIgnoreCase(type)) {
            column.setType("NUMBER");
            column.setPrecision(1);
            column.setScale(0);
            column.setLength(null);
        }
        
        // BLOB处理
        else if ("BLOB".equalsIgnoreCase(type) || "LONGBLOB".equalsIgnoreCase(type) || 
                 "MEDIUMBLOB".equalsIgnoreCase(type) || "TINYBLOB".equalsIgnoreCase(type)) {
            column.setType("BLOB");
            column.setLength(null);
        }
        
        // BINARY/VARBINARY处理
        else if ("BINARY".equalsIgnoreCase(type) || "VARBINARY".equalsIgnoreCase(type)) {
            column.setType("RAW");
            if (column.getLength() == null) {
                column.setLength(2000);
            } else if (column.getLength() > 2000) {
                column.setLength(2000); // RAW最大2000字节
            }
        }
        
        // 处理ENUM和SET类型（MySQL特有类型）
        else if ("ENUM".equalsIgnoreCase(type) || "SET".equalsIgnoreCase(type)) {
            column.setType("VARCHAR2");
            if (column.getLength() == null || column.getLength() > MAX_VARCHAR_LENGTH) {
                column.setLength(MAX_VARCHAR_LENGTH);
            }
        }
        
        // 处理JSON类型（PostgreSQL和MySQL 5.7+特有）
        else if ("JSON".equalsIgnoreCase(type) || "JSONB".equalsIgnoreCase(type)) {
            column.setType("CLOB");
            column.setLength(null);
        }
        
        // 处理INTERVAL类型（PostgreSQL特有）
        else if ("INTERVAL".equalsIgnoreCase(type)) {
            column.setType("VARCHAR2");
            column.setLength(100);
        }
        
        // 处理GEOMETRY类型（空间数据）
        else if ("GEOMETRY".equalsIgnoreCase(type) || "GEOGRAPHY".equalsIgnoreCase(type)) {
            try (Connection conn = getConnection()) {
                // 检测是否安装了 Oracle Spatial
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM all_objects WHERE object_name = 'SDO_GEOMETRY' AND owner = 'MDSYS'")) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        column.setType("MDSYS.SDO_GEOMETRY");
                    } else {
                        column.setType("BLOB");
                    }
                    column.setLength(null);
                } catch (Exception e) {
                    // 如果出错，默认使用 BLOB
                    column.setType("BLOB");
                    column.setLength(null);
                }
            } catch (Exception e) {
                // 如果获取连接失败，默认使用 BLOB
                column.setType("BLOB");
                column.setLength(null);
            }
        }
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
            // Oracle特定类型映射
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
                columnSql.append(" GENERATED BY DEFAULT AS IDENTITY");
            }

            // 收集主键列
            if (column.isPrimaryKey()) {
                primaryKeys.add(wrapIdentifier(column.getName()));
            }

            columnDefinitions.add(columnSql.toString());
        }

        // 添加主键约束
        if (!primaryKeys.isEmpty()) {
            columnDefinitions.add("CONSTRAINT " +
                    wrapIdentifier(tableDefinition.getTableName() + "_PK") +
                    " PRIMARY KEY (" + String.join(", ", primaryKeys) + ")");
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
            if (extraProps.containsKey("PCTINCREASE")) {
                storageParams.add("PCTINCREASE " + extraProps.get("PCTINCREASE"));
            }
            if (!storageParams.isEmpty()) {
                sql.append("\nSTORAGE (")
                        .append(String.join(" ", storageParams))
                        .append(")");
            }
        }

        // 添加表注释
        if (tableDefinition.getTableComment() != null && !tableDefinition.getTableComment().isEmpty()) {
            sql.append(";\n")
                    .append("COMMENT ON TABLE ")
                    .append(wrapIdentifier(tableDefinition.getTableName()))
                    .append(" IS ")
                    .append(wrapValue(tableDefinition.getTableComment()));
        }

        // 添加列注释
        for (ColumnDefinition column : tableDefinition.getColumns()) {
            if (column.getComment() != null && !column.getComment().isEmpty()) {
                sql.append(";\n")
                        .append("COMMENT ON COLUMN ")
                        .append(wrapIdentifier(tableDefinition.getTableName()))
                        .append(".")
                        .append(wrapIdentifier(column.getName()))
                        .append(" IS ")
                        .append(wrapValue(column.getComment()));
            }
        }

        return sql.toString();
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
                if (definition.isEmpty()) continue;

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
            Pattern tablespacePattern = Pattern.compile("TABLESPACE\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
            Matcher tablespaceMatcher = tablespacePattern.matcher(createTableSql);
            if (tablespaceMatcher.find()) {
                Map<String, String> extraProps = new HashMap<>();
                extraProps.put("TABLESPACE", tablespaceMatcher.group(1));
                tableDefinition.setExtraProperties(extraProps);
            }

            // 解析存储参数
            Pattern storagePattern = Pattern.compile("STORAGE\\s*\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);
            Matcher storageMatcher = storagePattern.matcher(createTableSql);
            if (storageMatcher.find()) {
                Map<String, String> extraProps = tableDefinition.getExtraProperties();
                if (extraProps == null) {
                    extraProps = new HashMap<>();
                    tableDefinition.setExtraProperties(extraProps);
                }
                extraProps.put("STORAGE_PARAMS", storageMatcher.group(1));
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
            } else if (c == '"' && (i == 0 || chars[i - 1] != '\\')) {
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
        Pattern columnPattern = Pattern.compile(
                "\"?([\\w]+)\"?\\s+" +                    // 列名
                        "([\\w\\(\\),]+)" +                       // 数据类型
                        "(?:\\s+DEFAULT\\s+([^\\s,]+))?" +       // 默认值（可选）
                        "(?:\\s+NOT\\s+NULL)?" +                 // NOT NULL（可选）
                        "(?:\\s+GENERATED\\s+(?:ALWAYS|BY\\s+DEFAULT)\\s+AS\\s+IDENTITY)?" + // 自增（可选）
                        "(?:\\s+CONSTRAINT\\s+[\\w\"]+)?" +      // 约束名（可选）
                        "(?:\\s+PRIMARY\\s+KEY)?",               // 主键（可选）
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = columnPattern.matcher(definition);
        if (!matcher.find()) {
            return null;
        }

        ColumnDefinition column = new ColumnDefinition();
        column.setName(matcher.group(1));

        String fullType = matcher.group(2);
        Pattern typePattern = Pattern.compile("([\\w]+)(?:\\((\\d+)(?:,(\\d+))?\\))?");
        Matcher typeMatcher = typePattern.matcher(fullType);
        if (typeMatcher.find()) {
            column.setType(typeMatcher.group(1));
            if (typeMatcher.group(2) != null) {
                if (typeMatcher.group(3) != null) {
                    column.setPrecision(Integer.parseInt(typeMatcher.group(2)));
                    column.setScale(Integer.parseInt(typeMatcher.group(3)));
                } else {
                    column.setLength(Integer.parseInt(typeMatcher.group(2)));
                }
            }
        }

        if (matcher.group(3) != null) {
            column.setDefaultValue(matcher.group(3));
        }

        column.setNullable(!definition.toUpperCase().contains("NOT NULL"));
        column.setAutoIncrement(definition.toUpperCase().contains("GENERATED"));
        column.setPrimaryKey(definition.toUpperCase().contains("PRIMARY KEY"));

        return column;
    }

    @Override
    public List<TableDefinition> getAllTables(String database) throws Exception {
        String sql = "SELECT TABLE_NAME, COMMENTS FROM USER_TAB_COMMENTS " +
                "WHERE TABLE_TYPE = 'TABLE' ORDER BY TABLE_NAME";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            List<TableDefinition> tables = new ArrayList<>();
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                String comment = rs.getString("COMMENTS");

                TableDefinition table = TableDefinition.builder()
                        .tableName(tableName)
                        .tableComment(comment)
                        .build();

                tables.add(table);
            }
            return tables;
        }
    }

    @Override
    public List<ColumnDefinition> getTableColumns(String database, String tableName) throws Exception {
        String sql = "SELECT c.COLUMN_NAME, c.DATA_TYPE, c.DATA_LENGTH, c.DATA_PRECISION, c.DATA_SCALE, " +
                "c.NULLABLE, c.DATA_DEFAULT, c.COLUMN_ID, " +
                "CASE WHEN p.CONSTRAINT_TYPE = 'P' THEN 'YES' ELSE 'NO' END AS IS_PRIMARY_KEY, " +
                "CASE WHEN cc.COMMENTS IS NOT NULL THEN cc.COMMENTS ELSE '' END AS COLUMN_COMMENT, " +
                "CASE WHEN ic.COLUMN_NAME IS NOT NULL THEN 'YES' ELSE 'NO' END AS IS_IDENTITY " +
                "FROM ALL_TAB_COLUMNS c " +
                "LEFT JOIN (" +
                "  SELECT col.COLUMN_NAME, con.CONSTRAINT_TYPE " +
                "  FROM ALL_CONSTRAINTS con " +
                "  JOIN ALL_CONS_COLUMNS col ON con.CONSTRAINT_NAME = col.CONSTRAINT_NAME " +
                "  WHERE con.TABLE_NAME = ? AND con.CONSTRAINT_TYPE = 'P' AND con.OWNER = USER " +
                ") p ON c.COLUMN_NAME = p.COLUMN_NAME " +
                "LEFT JOIN ALL_COL_COMMENTS cc ON c.TABLE_NAME = cc.TABLE_NAME AND c.COLUMN_NAME = cc.COLUMN_NAME AND cc.OWNER = USER " +
                "LEFT JOIN SYS.USER_IDENTITY_COLUMNS ic ON c.TABLE_NAME = ic.TABLE_NAME AND c.COLUMN_NAME = ic.COLUMN_NAME " +
                "WHERE c.TABLE_NAME = ? AND c.OWNER = USER " +
                "ORDER BY c.COLUMN_ID";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, tableName.toUpperCase());
            stmt.setString(2, tableName.toUpperCase());
            
            List<ColumnDefinition> columns = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME");
                    String dataType = rs.getString("DATA_TYPE");
                    
                    // 获取长度和精度信息
                    Integer dataLength = rs.getObject("DATA_LENGTH") != null ? rs.getInt("DATA_LENGTH") : null;
                    Integer dataPrecision = rs.getObject("DATA_PRECISION") != null ? rs.getInt("DATA_PRECISION") : null;
                    Integer dataScale = rs.getObject("DATA_SCALE") != null ? rs.getInt("DATA_SCALE") : null;
                    
                    // 处理 Oracle 特有的类型长度信息
                    Integer length = null;
                    if (dataType.contains("CHAR") && dataLength != null) {
                        length = dataLength;
                    } else if (dataType.equals("NUMBER") && dataPrecision != null) {
                        length = dataPrecision;
                    }
                    
                    // 是否可为空
                    boolean isNullable = "Y".equals(rs.getString("NULLABLE"));
                    
                    // 默认值
                    String defaultValue = rs.getString("DATA_DEFAULT");
                    if (defaultValue != null) {
                        defaultValue = defaultValue.trim();
                    }
                    
                    // 是否为主键
                    boolean isPrimaryKey = "YES".equals(rs.getString("IS_PRIMARY_KEY"));
                    
                    // 是否自增
                    boolean isAutoIncrement = "YES".equals(rs.getString("IS_IDENTITY"));
                    
                    // 注释
                    String comment = rs.getString("COLUMN_COMMENT");
                    
                    ColumnDefinition column = ColumnDefinition.builder()
                        .name(columnName)
                        .type(dataType)
                        .length(length)
                        .precision(dataPrecision)
                        .scale(dataScale)
                        .nullable(isNullable)
                        .defaultValue(defaultValue)
                        .comment(comment)
                        .primaryKey(isPrimaryKey)
                        .autoIncrement(isAutoIncrement)
                        .build();
                    
                    columns.add(column);
                }
            }
            
            return columns;
        } catch (SQLException e) {
            // 如果 IDENTITY 列查询失败（旧版 Oracle 不支持），尝试替代查询
            if (e.getMessage().contains("SYS.USER_IDENTITY_COLUMNS")) {
                log.info("Oracle版本不支持IDENTITY列查询，使用替代方法: {}", e.getMessage());
                return getTableColumnsWithoutIdentity(tableName);
            }
            throw e;
        }
    }

    // 不依赖 IDENTITY 列的备用查询方法
    private List<ColumnDefinition> getTableColumnsWithoutIdentity(String tableName) throws Exception {
        String sql = "SELECT c.COLUMN_NAME, c.DATA_TYPE, c.DATA_LENGTH, c.DATA_PRECISION, c.DATA_SCALE, " +
                "c.NULLABLE, c.DATA_DEFAULT, c.COLUMN_ID, " +
                "CASE WHEN p.CONSTRAINT_TYPE = 'P' THEN 'YES' ELSE 'NO' END AS IS_PRIMARY_KEY, " +
                "CASE WHEN cc.COMMENTS IS NOT NULL THEN cc.COMMENTS ELSE '' END AS COLUMN_COMMENT " +
                "FROM ALL_TAB_COLUMNS c " +
                "LEFT JOIN (" +
                "  SELECT col.COLUMN_NAME, con.CONSTRAINT_TYPE " +
                "  FROM ALL_CONSTRAINTS con " +
                "  JOIN ALL_CONS_COLUMNS col ON con.CONSTRAINT_NAME = col.CONSTRAINT_NAME " +
                "  WHERE con.TABLE_NAME = ? AND con.CONSTRAINT_TYPE = 'P' AND con.OWNER = USER " +
                ") p ON c.COLUMN_NAME = p.COLUMN_NAME " +
                "LEFT JOIN ALL_COL_COMMENTS cc ON c.TABLE_NAME = cc.TABLE_NAME AND c.COLUMN_NAME = cc.COLUMN_NAME AND cc.OWNER = USER " +
                "WHERE c.TABLE_NAME = ? AND c.OWNER = USER " +
                "ORDER BY c.COLUMN_ID";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, tableName.toUpperCase());
            stmt.setString(2, tableName.toUpperCase());
            
            List<ColumnDefinition> columns = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME");
                    String dataType = rs.getString("DATA_TYPE");
                    
                    // 获取长度和精度信息
                    Integer dataLength = rs.getObject("DATA_LENGTH") != null ? rs.getInt("DATA_LENGTH") : null;
                    Integer dataPrecision = rs.getObject("DATA_PRECISION") != null ? rs.getInt("DATA_PRECISION") : null;
                    Integer dataScale = rs.getObject("DATA_SCALE") != null ? rs.getInt("DATA_SCALE") : null;
                    
                    // 处理 Oracle 特有的类型长度信息
                    Integer length = null;
                    if (dataType.contains("CHAR") && dataLength != null) {
                        length = dataLength;
                    } else if (dataType.equals("NUMBER") && dataPrecision != null) {
                        length = dataPrecision;
                    }
                    
                    // 是否可为空
                    boolean isNullable = "Y".equals(rs.getString("NULLABLE"));
                    
                    // 默认值
                    String defaultValue = rs.getString("DATA_DEFAULT");
                    if (defaultValue != null) {
                        defaultValue = defaultValue.trim();
                    }
                    
                    // 是否为主键
                    boolean isPrimaryKey = "YES".equals(rs.getString("IS_PRIMARY_KEY"));
                    
                    // 注释
                    String comment = rs.getString("COLUMN_COMMENT");
                    
                    // 检查序列和触发器来判断是否自增
                    boolean isAutoIncrement = false;
                    
                    // 检查是否有关联的序列（自增）
                    try {
                        String sequenceSql = "SELECT TRIGGER_NAME FROM USER_TRIGGERS " +
                                            "WHERE TABLE_OWNER = USER AND TABLE_NAME = ? " +
                                            "AND TRIGGERING_EVENT LIKE '%INSERT%' " +
                                            "AND TRIGGER_BODY LIKE '%' || ? || '%' AND TRIGGER_BODY LIKE '%NEXTVAL%'";
                        
                        try (PreparedStatement seqStmt = conn.prepareStatement(sequenceSql)) {
                            seqStmt.setString(1, tableName.toUpperCase());
                            seqStmt.setString(2, columnName.toUpperCase());
                            
                            try (ResultSet seqRs = seqStmt.executeQuery()) {
                                isAutoIncrement = seqRs.next(); // 如果找到触发器，则认为是自增列
                            }
                        }
                    } catch (Exception e) {
                        // 忽略检查自增失败的异常
                        log.debug("检查自增列失败: table={}, column={}, error={}", 
                                 tableName, columnName, e.getMessage());
                    }
                    
                    ColumnDefinition column = ColumnDefinition.builder()
                        .name(columnName)
                        .type(dataType)
                        .length(length)
                        .precision(dataPrecision)
                        .scale(dataScale)
                        .nullable(isNullable)
                        .defaultValue(defaultValue)
                        .comment(comment)
                        .primaryKey(isPrimaryKey)
                        .autoIncrement(isAutoIncrement)
                        .build();
                    
                    columns.add(column);
                }
            }
            
            return columns;
        }
    }

    @Override
    public String getDatabaseType() {
        return "Oracle";
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
             ResultSet rs = stmt.executeQuery("SELECT TABLE_NAME FROM USER_TABLES ORDER BY TABLE_NAME")) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
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
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT t.TABLE_NAME, t.TABLESPACE_NAME, t.STATUS, t.NUM_ROWS, t.BLOCKS, " +
                            "t.LAST_ANALYZED, c.COMMENTS " +
                            "FROM USER_TABLES t " +
                            "LEFT JOIN USER_TAB_COMMENTS c ON t.TABLE_NAME = c.TABLE_NAME " +
                            "WHERE t.TABLE_NAME = ?")) {
                stmt.setString(1, tableName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        tableInfo.put("tableName", rs.getString("TABLE_NAME"));
                        tableInfo.put("tablespaceName", rs.getString("TABLESPACE_NAME"));
                        tableInfo.put("status", rs.getString("STATUS"));
                        tableInfo.put("rowCount", rs.getLong("NUM_ROWS"));
                        tableInfo.put("blocks", rs.getLong("BLOCKS"));
                        tableInfo.put("lastAnalyzed", rs.getTimestamp("LAST_ANALYZED"));
                        tableInfo.put("comments", rs.getString("COMMENTS"));
                    }
                }
            }

            // 获取表列数
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(*) AS COLUMN_COUNT FROM USER_TAB_COLUMNS WHERE TABLE_NAME = ?")) {
                stmt.setString(1, tableName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        tableInfo.put("columnCount", rs.getInt("COLUMN_COUNT"));
                    }
                }
            }

            // 获取表大小
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT SUM(BYTES) AS TABLE_SIZE FROM USER_SEGMENTS WHERE SEGMENT_NAME = ?")) {
                stmt.setString(1, tableName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        tableInfo.put("tableSize", rs.getLong("TABLE_SIZE"));
                    }
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
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT c.COLUMN_NAME, c.DATA_TYPE, c.DATA_LENGTH, c.DATA_PRECISION, c.DATA_SCALE, " +
                             "c.NULLABLE, c.COLUMN_ID, c.DEFAULT_LENGTH, c.DATA_DEFAULT, " +
                             "c.CHAR_LENGTH, c.CHAR_USED, cc.COMMENTS " +
                             "FROM USER_TAB_COLUMNS c " +
                             "LEFT JOIN USER_COL_COMMENTS cc ON c.TABLE_NAME = cc.TABLE_NAME AND c.COLUMN_NAME = cc.COLUMN_NAME " +
                             "WHERE c.TABLE_NAME = ? " +
                             "ORDER BY c.COLUMN_ID")) {
            stmt.setString(1, tableName.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> column = new HashMap<>();
                    column.put("columnName", rs.getString("COLUMN_NAME"));
                    column.put("dataType", rs.getString("DATA_TYPE"));
                    column.put("dataLength", rs.getInt("DATA_LENGTH"));
                    column.put("dataPrecision", rs.getInt("DATA_PRECISION"));
                    column.put("dataScale", rs.getInt("DATA_SCALE"));
                    column.put("nullable", "Y".equals(rs.getString("NULLABLE")));
                    column.put("columnId", rs.getInt("COLUMN_ID"));
                    column.put("defaultLength", rs.getInt("DEFAULT_LENGTH"));
                    column.put("defaultValue", rs.getString("DATA_DEFAULT"));
                    column.put("charLength", rs.getInt("CHAR_LENGTH"));
                    column.put("charUsed", rs.getString("CHAR_USED"));
                    column.put("comments", rs.getString("COMMENTS"));
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
        Map<String, Object> columnInfo = new HashMap<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT c.COLUMN_NAME, c.DATA_TYPE, c.DATA_LENGTH, c.DATA_PRECISION, c.DATA_SCALE, " +
                 "c.NULLABLE, c.DATA_DEFAULT, c.COLUMN_ID, " +
                 "CASE WHEN p.CONSTRAINT_TYPE = 'P' THEN 'YES' ELSE 'NO' END AS IS_PRIMARY_KEY, " +
                 "CASE WHEN cc.COMMENTS IS NOT NULL THEN cc.COMMENTS ELSE '' END AS COLUMN_COMMENT " +
                 "FROM ALL_TAB_COLUMNS c " +
                 "LEFT JOIN (" +
                 "  SELECT col.COLUMN_NAME, con.CONSTRAINT_TYPE " +
                 "  FROM ALL_CONSTRAINTS con " +
                 "  JOIN ALL_CONS_COLUMNS col ON con.CONSTRAINT_NAME = col.CONSTRAINT_NAME " +
                 "  WHERE con.TABLE_NAME = ? AND con.CONSTRAINT_TYPE = 'P' AND con.OWNER = USER " +
                 ") p ON c.COLUMN_NAME = p.COLUMN_NAME " +
                 "LEFT JOIN ALL_COL_COMMENTS cc ON c.TABLE_NAME = cc.TABLE_NAME AND c.COLUMN_NAME = cc.COLUMN_NAME AND cc.OWNER = USER " +
                 "WHERE c.TABLE_NAME = ? AND c.COLUMN_NAME = ? AND c.OWNER = USER")) {
            stmt.setString(1, tableName.toUpperCase());
            stmt.setString(2, tableName.toUpperCase());
            stmt.setString(3, columnName.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    columnInfo.put("COLUMN_NAME", rs.getString("COLUMN_NAME"));
                    columnInfo.put("DATA_TYPE", rs.getString("DATA_TYPE"));
                    columnInfo.put("DATA_LENGTH", rs.getInt("DATA_LENGTH"));
                    columnInfo.put("DATA_PRECISION", rs.getInt("DATA_PRECISION"));
                    columnInfo.put("DATA_SCALE", rs.getInt("DATA_SCALE"));
                    columnInfo.put("NULLABLE", "Y".equals(rs.getString("NULLABLE")));
                    columnInfo.put("DATA_DEFAULT", rs.getString("DATA_DEFAULT"));
                    columnInfo.put("COLUMN_ID", rs.getInt("COLUMN_ID"));
                    columnInfo.put("IS_PRIMARY_KEY", "YES".equals(rs.getString("IS_PRIMARY_KEY")));
                    columnInfo.put("COLUMN_COMMENT", rs.getString("COLUMN_COMMENT"));
                }
            }
        } catch (Exception e) {
            log.error("获取列信息失败: table={}, column={}, error={}", tableName, columnName, e.getMessage(), e);
            throw e;
        }
        
        if (columnInfo.isEmpty()) {
            throw new Exception("Column not found: " + columnName);
        }
        
        return columnInfo;
    }

    @Override
    public String getTableEngine(String tableName) throws Exception {
        // Oracle不支持存储引擎的概念，返回默认值
        return "Oracle";
    }

    @Override
    public String getTableCharset(String tableName) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT DEFAULT_CHARACTER_SET_NAME FROM ALL_TABLES WHERE OWNER = USER AND TABLE_NAME = ?")) {
            stmt.setString(1, tableName.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("DEFAULT_CHARACTER_SET_NAME");
                }
            }
        } catch (Exception e) {
            // 可能是Oracle版本不支持此查询
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT VALUE FROM NLS_DATABASE_PARAMETERS WHERE PARAMETER = 'NLS_CHARACTERSET'")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("VALUE");
                    }
                }
            } catch (Exception ex) {
                log.error("获取表字符集失败: table={}, error={}", tableName, ex.getMessage(), ex);
                throw ex;
            }
        }
        return "UTF8";
    }

    @Override
    public String getTableCollation(String tableName) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT VALUE FROM NLS_DATABASE_PARAMETERS WHERE PARAMETER = 'NLS_SORT'")) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("VALUE");
                }
            }
        } catch (Exception e) {
            log.error("获取表排序规则失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return "BINARY";
    }

    @Override
    public Long getTableSize(String tableName) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT SUM(BYTES) AS TABLE_SIZE FROM USER_SEGMENTS WHERE SEGMENT_NAME = ?")) {
            stmt.setString(1, tableName.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("TABLE_SIZE");
                }
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
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT NUM_ROWS FROM USER_TABLES WHERE TABLE_NAME = ?")) {
            stmt.setString(1, tableName.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("NUM_ROWS");
                }
            }
        } catch (Exception e) {
            log.error("获取表行数失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return -1L;
    }

    @Override
    public String getTableSpace(String tableName) throws Exception {
        String sql = "SELECT TABLESPACE_NAME FROM USER_TABLES WHERE TABLE_NAME = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tableName.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("TABLESPACE_NAME");
                }
                return null;
            }
        }
    }

    @Override
    public Integer getCharacterLength(String tableName, String columnName) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT CHAR_LENGTH FROM USER_TAB_COLUMNS " +
                             "WHERE TABLE_NAME = ? AND COLUMN_NAME = ? " +
                             "AND DATA_TYPE IN ('VARCHAR2', 'NVARCHAR2', 'CHAR', 'NCHAR')")) {
            stmt.setString(1, tableName.toUpperCase());
              stmt.setString(2, columnName.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("CHAR_LENGTH");
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
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT DATA_PRECISION FROM USER_TAB_COLUMNS " +
                             "WHERE TABLE_NAME = ? AND COLUMN_NAME = ? " +
                             "AND DATA_TYPE IN ('NUMBER', 'FLOAT', 'BINARY_FLOAT', 'BINARY_DOUBLE')")) {
            stmt.setString(1, tableName.toUpperCase());
              stmt.setString(2, columnName.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("DATA_PRECISION");
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
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT DATA_SCALE FROM USER_TAB_COLUMNS " +
                             "WHERE TABLE_NAME = ? AND COLUMN_NAME = ? " +
                             "AND DATA_TYPE IN ('NUMBER', 'FLOAT')")) {
            stmt.setString(1, tableName.toUpperCase());
              stmt.setString(2, columnName.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("DATA_SCALE");
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
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT DATA_DEFAULT FROM USER_TAB_COLUMNS " +
                             "WHERE TABLE_NAME = ? AND COLUMN_NAME = ?")) {
            stmt.setString(1, tableName.toUpperCase());
              stmt.setString(2, columnName.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("DATA_DEFAULT");
                }
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
        try (Connection conn = getConnection()) {
            // 检查是否是主键
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT 1 FROM USER_CONS_COLUMNS ucc " +
                            "JOIN USER_CONSTRAINTS uc ON ucc.CONSTRAINT_NAME = uc.CONSTRAINT_NAME " +
                            "WHERE uc.CONSTRAINT_TYPE = 'P' AND ucc.TABLE_NAME = ? AND ucc.COLUMN_NAME = ?")) {
                stmt.setString(1, tableName.toUpperCase());
                  stmt.setString(2, columnName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        extra.append("PRIMARY KEY ");
                    }
                }
            }

            // 检查是否是唯一键
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT 1 FROM USER_CONS_COLUMNS ucc " +
                            "JOIN USER_CONSTRAINTS uc ON ucc.CONSTRAINT_NAME = uc.CONSTRAINT_NAME " +
                            "WHERE uc.CONSTRAINT_TYPE = 'U' AND ucc.TABLE_NAME = ? AND ucc.COLUMN_NAME = ?")) {
                stmt.setString(1, tableName.toUpperCase());
                  stmt.setString(2, columnName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        extra.append("UNIQUE ");
                    }
                }
            }

            // 检查是否是自增列（使用序列和触发器实现）
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT 1 FROM USER_TRIGGERS t " +
                            "WHERE t.TABLE_NAME = ? AND t.TRIGGER_TYPE = 'BEFORE EACH ROW' " +
                            "AND REGEXP_LIKE(t.TRIGGER_BODY, ':" + columnName.toUpperCase() + "\\s*:=\\s*.*\\.NEXTVAL')")) {
                stmt.setString(1, tableName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        extra.append("AUTO_INCREMENT ");
                    }
                }
            } catch (Exception e) {
                // 忽略正则表达式错误
            }

            // 检查是否有检查约束
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT 1 FROM USER_CONS_COLUMNS ucc " +
                            "JOIN USER_CONSTRAINTS uc ON ucc.CONSTRAINT_NAME = uc.CONSTRAINT_NAME " +
                            "WHERE uc.CONSTRAINT_TYPE = 'C' AND ucc.TABLE_NAME = ? AND ucc.COLUMN_NAME = ? " +
                            "AND uc.SEARCH_CONDITION IS NOT NULL AND uc.SEARCH_CONDITION NOT LIKE '%IS NOT NULL%'")) {
                stmt.setString(1, tableName.toUpperCase());
                  stmt.setString(2, columnName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        extra.append("CHECK ");
                    }
                }
            }

            // 检查是否是虚拟列
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT VIRTUAL_COLUMN FROM USER_TAB_COLUMNS " +
                            "WHERE TABLE_NAME = ? AND COLUMN_NAME = ?")) {
                stmt.setString(1, tableName.toUpperCase());
                  stmt.setString(2, columnName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && "YES".equals(rs.getString("VIRTUAL_COLUMN"))) {
                        extra.append("VIRTUAL ");
                    }
                }
            } catch (Exception e) {
                // 忽略不支持VIRTUAL_COLUMN列的Oracle版本
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
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COLUMN_ID FROM USER_TAB_COLUMNS " +
                             "WHERE TABLE_NAME = ? AND COLUMN_NAME = ?")) {
            stmt.setString(1, tableName.toUpperCase());
              stmt.setString(2, columnName.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("COLUMN_ID");
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
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT CREATED FROM USER_OBJECTS " +
                             "WHERE OBJECT_TYPE = 'TABLE' AND OBJECT_NAME = ?")) {
            stmt.setString(1, tableName.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getTimestamp("CREATED");
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
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT LAST_DDL_TIME FROM USER_OBJECTS " +
                             "WHERE OBJECT_TYPE = 'TABLE' AND OBJECT_NAME = ?")) {
            stmt.setString(1, tableName.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getTimestamp("LAST_DDL_TIME");
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
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT ucc.COLUMN_NAME FROM USER_CONS_COLUMNS ucc " +
                             "JOIN USER_CONSTRAINTS uc ON ucc.CONSTRAINT_NAME = uc.CONSTRAINT_NAME " +
                             "WHERE uc.CONSTRAINT_TYPE = 'P' AND ucc.TABLE_NAME = ? " +
                             "ORDER BY ucc.POSITION")) {
            stmt.setString(1, tableName.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    primaryKeys.add(rs.getString("COLUMN_NAME"));
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
        String sql = String.format(
            "SELECT " +
            "a.constraint_name AS fk_name, " +
            "a.table_name AS fk_table_name, " +
            "b.column_name AS fk_column_name, " +
            "c.table_name AS referenced_table_name, " +
            "d.column_name AS referenced_column_name " +
            "FROM user_constraints a " +
            "JOIN user_cons_columns b ON a.constraint_name = b.constraint_name " +
            "JOIN user_constraints c ON a.r_constraint_name = c.constraint_name " +
            "JOIN user_cons_columns d ON c.constraint_name = d.constraint_name " +
            "WHERE a.constraint_type = 'R' " +
            "AND a.table_name = '%s' " +
            "ORDER BY a.constraint_name, b.position",
            tableName.toUpperCase()
        );
        
        // ... existing code for execution ...
        
        return new ArrayList<>();
    }

    @Override
    public List<Map<String, Object>> getIndexes(String tableName) throws Exception {
        List<Map<String, Object>> indexes = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT ui.INDEX_NAME, ui.INDEX_TYPE, ui.UNIQUENESS, uic.COLUMN_NAME, uic.COLUMN_POSITION " +
                             "FROM USER_INDEXES ui " +
                             "JOIN USER_IND_COLUMNS uic ON ui.INDEX_NAME = uic.INDEX_NAME " +
                             "WHERE ui.TABLE_NAME = ? " +
                             "ORDER BY ui.INDEX_NAME, uic.COLUMN_POSITION")) {
            stmt.setString(1, tableName.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                Map<String, Map<String, Object>> indexMap = new HashMap<>();

                while (rs.next()) {
                    String indexName = rs.getString("INDEX_NAME");
                    Map<String, Object> index = indexMap.get(indexName);

                    if (index == null) {
                        index = new HashMap<>();
                        index.put("indexName", indexName);
                        index.put("indexType", rs.getString("INDEX_TYPE"));
                        index.put("unique", "UNIQUE".equals(rs.getString("UNIQUENESS")));
                        index.put("columns", new ArrayList<String>());
                        indexMap.put(indexName, index);
                    }

                    ((List<String>) index.get("columns")).add(rs.getString("COLUMN_NAME"));
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
    public Map<String, Object> getTableCompleteness(String tableName) throws Exception {
        Map<String, Object> completeness = new HashMap<>();
        try (Connection conn = getConnection()) {
            List<Map<String, Object>> columns = listColumns(tableName);
            
            // 计算总行数
            String countSql = "SELECT COUNT(*) AS total_rows FROM " + wrapIdentifier(tableName);
            long totalRows = 0;
            try (PreparedStatement stmt = conn.prepareStatement(countSql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    totalRows = rs.getLong("total_rows");
                }
            }
            
            completeness.put("TOTAL_ROWS", totalRows);
            
            if (totalRows > 0) {
                Map<String, Object> columnCompleteness = new HashMap<>();
                
                // 计算每列的非空率
                for (Map<String, Object> column : columns) {
                    String columnName = (String) column.get("COLUMN_NAME");
                    String sql = String.format("SELECT COUNT(*) AS non_null_count " +
                                              "FROM %s " +
                                              "WHERE %s IS NOT NULL",
                                              wrapIdentifier(tableName),
                                              wrapIdentifier(columnName));
                    try (PreparedStatement stmt = conn.prepareStatement(sql);
                         ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            long nonNullCount = rs.getLong("non_null_count");
                            double nonNullRate = (double) nonNullCount / totalRows * 100;
                            columnCompleteness.put(columnName, nonNullRate);
                        }
                    }
                }
                
                completeness.put("COLUMN_COMPLETENESS", columnCompleteness);
                
                // 计算整体完整性
                double totalCompleteness = 0;
                for (Object rate : columnCompleteness.values()) {
                    totalCompleteness += (Double) rate;
                }
                completeness.put("OVERALL_COMPLETENESS", totalCompleteness / columnCompleteness.size());
            }
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
            List<Map<String, Object>> columns = listColumns(tableName);
            
            // 计算总行数
            String countSql = "SELECT COUNT(*) AS total_rows FROM " + wrapIdentifier(tableName);
            long totalRows = 0;
            try (PreparedStatement stmt = conn.prepareStatement(countSql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    totalRows = rs.getLong("total_rows");
                }
            }
            
            accuracy.put("TOTAL_ROWS", totalRows);
            
            if (totalRows > 0) {
                Map<String, Object> columnAccuracy = new HashMap<>();
                
                // 对于数值类型字段，检查是否在合理范围内
                for (Map<String, Object> column : columns) {
                    String columnName = (String) column.get("COLUMN_NAME");
                    String dataType = ((String) column.get("DATA_TYPE")).toUpperCase();
                    
                    if (dataType.contains("NUMBER") || dataType.contains("FLOAT") || 
                        dataType.contains("BINARY_FLOAT") || dataType.contains("BINARY_DOUBLE") || 
                        dataType.contains("DECIMAL")) {
                        
                        // 获取最大值和最小值
                        String sql = String.format("SELECT MIN(%s) AS min_val, MAX(%s) AS max_val " +
                                                  "FROM %s " +
                                                  "WHERE %s IS NOT NULL",
                                                  wrapIdentifier(columnName),
                                                  wrapIdentifier(columnName),
                                                  wrapIdentifier(tableName),
                                                  wrapIdentifier(columnName));
                        try (PreparedStatement stmt = conn.prepareStatement(sql);
                             ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                Map<String, Object> rangeMap = new HashMap<>();
                                rangeMap.put("MIN", rs.getObject("min_val"));
                                rangeMap.put("MAX", rs.getObject("max_val"));
                                columnAccuracy.put(columnName, rangeMap);
                            }
                        }
                    }
                }
                
                accuracy.put("COLUMN_RANGES", columnAccuracy);
            }
        } catch (Exception e) {
            log.error("获取表准确性失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return accuracy;
    }

    @Override
    public Map<String, Object> getTableConsistency(String tableName) throws Exception {
        Map<String, Object> consistency = new HashMap<>();
        
        // 检查外键一致性
        List<Map<String, Object>> foreignKeys = getForeignKeys(tableName);
        if (!foreignKeys.isEmpty()) {
            Map<String, Object> fkConsistency = new HashMap<>();
            
            try (Connection conn = getConnection()) {
                for (Map<String, Object> fk : foreignKeys) {
                    String fkName = (String) fk.get("FK_NAME");
                    String refTable = (String) fk.get("REFERENCED_TABLE_NAME");
                    
                    @SuppressWarnings("unchecked")
                    List<String> columns = (List<String>) fk.get("COLUMN_NAMES");
                    
                    @SuppressWarnings("unchecked")
                    List<String> refColumns = (List<String>) fk.get("REFERENCED_COLUMN_NAMES");
                    
                    if (columns.size() == 1 && refColumns.size() == 1) {
                        // 简单外键检查
                        String sql = String.format(
                            "SELECT COUNT(*) AS violation_count " +
                            "FROM %s t1 " +
                            "WHERE t1.%s IS NOT NULL " +
                            "AND NOT EXISTS (SELECT 1 FROM %s t2 WHERE t1.%s = t2.%s)",
                            wrapIdentifier(tableName),
                            wrapIdentifier(columns.get(0)),
                            wrapIdentifier(refTable),
                            wrapIdentifier(columns.get(0)),
                            wrapIdentifier(refColumns.get(0))
                        );
                        
                        try (PreparedStatement stmt = conn.prepareStatement(sql);
                             ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                long violationCount = rs.getLong("violation_count");
                                Map<String, Object> violationMap = new HashMap<>();
                                violationMap.put("VIOLATION_COUNT", violationCount);
                                violationMap.put("REF_TABLE", refTable);
                                violationMap.put("COLUMNS", String.join(",", columns));
                                violationMap.put("REF_COLUMNS", String.join(",", refColumns));
                                fkConsistency.put(fkName, violationMap);
                            }
                        }
                    }
                }
            }
            
            consistency.put("FOREIGN_KEY_CONSISTENCY", fkConsistency);
        }
        
        return consistency;
    }

    @Override
    public Map<String, Object> getTableUniqueness(String tableName) throws Exception {
        Map<String, Object> uniqueness = new HashMap<>();
        
        // 获取表的唯一索引
        List<Map<String, Object>> indexes = getIndexes(tableName);
        List<Map<String, Object>> uniqueIndexes = new ArrayList<>();
        for (Map<String, Object> idx : indexes) {
            if ((Boolean) idx.get("IS_UNIQUE")) {
                uniqueIndexes.add(idx);
            }
        }
        
        if (!uniqueIndexes.isEmpty()) {
            Map<String, Object> indexUniqueness = new HashMap<>();
            
            try (Connection conn = getConnection()) {
                for (Map<String, Object> idx : uniqueIndexes) {
                    String indexName = (String) idx.get("INDEX_NAME");
                    
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> columns = (List<Map<String, Object>>) idx.get("COLUMNS");
                    
                    if (!columns.isEmpty()) {
                        // 提取列名
                        List<String> columnNames = new ArrayList<>();
                        for (Map<String, Object> col : columns) {
                            columnNames.add((String) col.get("COLUMN_NAME"));
                        }
                        
                        // 检查是否有重复值
                        String columnList = columnNames.stream()
                            .map(this::wrapIdentifier)
                            .collect(Collectors.joining(", "));
                        
                        String sql = String.format(
                            "SELECT %s, COUNT(*) AS dup_count " +
                            "FROM %s " +
                            "GROUP BY %s " +
                            "HAVING COUNT(*) > 1",
                            columnList,
                            wrapIdentifier(tableName),
                            columnList
                        );
                        
                        long duplicateCount = 0;
                        try (PreparedStatement stmt = conn.prepareStatement(sql);
                             ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                duplicateCount++;
                            }
                        }
                        
                        Map<String, Object> duplicateMap = new HashMap<>();
                        duplicateMap.put("DUPLICATE_COUNT", duplicateCount);
                        duplicateMap.put("COLUMNS", String.join(",", columnNames));
                        indexUniqueness.put(indexName, duplicateMap);
                    }
                }
            }
            
            uniqueness.put("UNIQUE_INDEX_VIOLATIONS", indexUniqueness);
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
                String columnName = (String) column.get("COLUMN_NAME");
                String dataType = ((String) column.get("DATA_TYPE")).toUpperCase();
                
                if (dataType.contains("DATE") || dataType.contains("TIMESTAMP")) {
                    // 检查日期字段是否有无效值
                    String sql = String.format(
                        "SELECT COUNT(*) AS total, " +
                        "SUM(CASE WHEN %s IS NULL THEN 0 ELSE 1 END) AS non_null, " +
                        "SUM(CASE WHEN %s IS NULL THEN 0 " +
                        "     WHEN TO_CHAR(%s, 'YYYY-MM-DD') IS NOT NULL THEN 1 " +
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
            // 获取表的列信息
            List<Map<String, String>> columns = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COLUMN_NAME, DATA_TYPE FROM USER_TAB_COLUMNS " +
                            "WHERE TABLE_NAME = ?")) {
                stmt.setString(1, tableName.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, String> column = new HashMap<>();
                        column.put("name", rs.getString("COLUMN_NAME"));
                        column.put("type", rs.getString("DATA_TYPE"));
                        columns.add(column);
                    }
                }
            }

            // 计算每列的问题数量
            int nullValueCount = 0;
            for (Map<String, String> column : columns) {
                String columnName = column.get("name");
                String dataType = column.get("type");

                // 计算空值数量
                String nullCountSql = String.format(
                        "SELECT COUNT(*) FROM %s WHERE %s IS NULL",
                        wrapIdentifier(tableName),
                        wrapIdentifier(columnName)
                );

                try (PreparedStatement stmt = conn.prepareStatement(nullCountSql);
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        nullValueCount += rs.getInt(1);
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
                for (Map<String, String> column : columns) {
                    uniqueColumns.add(column.get("name"));
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
            
            // 统计外键一致性问题
            int fkViolationCount = 0;
            List<Map<String, Object>> foreignKeys = getForeignKeys(tableName);
            
            for (Map<String, Object> fk : foreignKeys) {
                @SuppressWarnings("unchecked")
                List<String> columns1 = (List<String>) fk.get("COLUMN_NAMES");
                
                @SuppressWarnings("unchecked")
                List<String> refColumns = (List<String>) fk.get("REFERENCED_COLUMN_NAMES");
                
                String refTable = (String) fk.get("REFERENCED_TABLE_NAME");
                
                if (columns1.size() == 1 && refColumns.size() == 1) {
                    String sql = String.format(
                        "SELECT COUNT(*) AS violation_count " +
                        "FROM %s t1 " +
                        "WHERE t1.%s IS NOT NULL " +
                        "AND NOT EXISTS (SELECT 1 FROM %s t2 WHERE t1.%s = t2.%s)",
                        wrapIdentifier(tableName),
                        wrapIdentifier(columns1.get(0)),
                        wrapIdentifier(refTable),
                        wrapIdentifier(columns1.get(0)),
                        wrapIdentifier(refColumns.get(0))
                    );
                    
                    try (PreparedStatement stmt = conn.prepareStatement(sql);
                         ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            fkViolationCount += rs.getInt("violation_count");
                        }
                    }
                }
            }
            
            issueCount.put("FK_VIOLATIONS", fkViolationCount);
            
            // 统计日期格式问题
            int dateFormatViolationCount = 0;
            for (Map<String, String> column : columns) {
                String columnName = column.get("name");
                String dataType = column.get("type").toUpperCase();
                
                if (dataType.contains("DATE") || dataType.contains("TIMESTAMP")) {
                    String sql = String.format(
                        "SELECT COUNT(*) AS invalid_count " +
                        "FROM %s " +
                        "WHERE %s IS NOT NULL " +
                        "AND TO_CHAR(%s, 'YYYY-MM-DD') IS NULL",
                        wrapIdentifier(tableName),
                        wrapIdentifier(columnName),
                        wrapIdentifier(columnName)
                    );
                    
                    try (PreparedStatement stmt = conn.prepareStatement(sql);
                         ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            dateFormatViolationCount += rs.getInt("invalid_count");
                        }
                    }
                }
            }
            
            issueCount.put("DATE_FORMAT_VIOLATIONS", dateFormatViolationCount);
            
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
        
        if (dataType.contains("NUMBER") || dataType.contains("FLOAT") || 
            dataType.contains("BINARY_FLOAT") || dataType.contains("BINARY_DOUBLE") || 
            dataType.contains("DECIMAL")) {
            // 数值类型验证
            return sql + wrapIdentifier(columnName) + " IS NOT NULL";
        } else if (dataType.contains("VARCHAR") || dataType.contains("CHAR") || 
                  dataType.contains("CLOB") || dataType.contains("NCLOB")) {
            // 字符串类型验证
            return sql + wrapIdentifier(columnName) + " IS NOT NULL";
        } else if (dataType.contains("DATE") || dataType.contains("TIMESTAMP")) {
            // 日期类型验证
            return sql + wrapIdentifier(columnName) + " IS NOT NULL";
        }
        
        return null;
    }

    @Override
    public Map<String, Object> getTableFieldStatistics(String tableName) throws Exception {
        Map<String, Object> statistics = new HashMap<>();
        
        String sql = "SELECT COUNT(*) as total_columns, " +
                     "SUM(CASE WHEN data_type IN ('NUMBER', 'FLOAT', 'BINARY_FLOAT', 'BINARY_DOUBLE') THEN 1 ELSE 0 END) as numeric_columns, " +
                     "SUM(CASE WHEN data_type IN ('VARCHAR2', 'CHAR', 'NVARCHAR2', 'NCHAR', 'CLOB', 'NCLOB') THEN 1 ELSE 0 END) as character_columns, " +
                     "SUM(CASE WHEN data_type IN ('DATE', 'TIMESTAMP') THEN 1 ELSE 0 END) as date_columns, " +
                     "SUM(CASE WHEN data_type IN ('BLOB', 'RAW', 'LONG RAW') THEN 1 ELSE 0 END) as binary_columns, " +
                     "SUM(CASE WHEN nullable = 'Y' THEN 1 ELSE 0 END) as nullable_columns " +
                     "FROM all_tab_columns " +
                     "WHERE table_name = ? " +
                     "AND owner = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tableName.toUpperCase());
            stmt.setString(2, getSchema().toUpperCase());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    statistics.put("total_columns", rs.getInt("total_columns"));
                    statistics.put("numeric_columns", rs.getInt("numeric_columns"));
                    statistics.put("character_columns", rs.getInt("character_columns"));
                    statistics.put("date_columns", rs.getInt("date_columns"));
                    statistics.put("binary_columns", rs.getInt("binary_columns"));
                    statistics.put("nullable_columns", rs.getInt("nullable_columns"));
                }
            }
        }
        
        return statistics;
    }

    @Override
    public Map<String, Object> getColumnDistribution(String tableName, String columnName) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        // 获取数据类型
        Map<String, Object> columnInfo = getColumnInfo(tableName, columnName);
        String dataType = (String) columnInfo.get("DATA_TYPE");
        
        String sql;
        if (dataType.contains("CHAR") || dataType.contains("CLOB")) {
            // 对于文本类型，统计长度分布
            sql = String.format(
                "SELECT " +
                "NVL(LENGTH(%s), 0) AS length, " +
                "COUNT(*) AS count " +
                "FROM %s " +
                "GROUP BY LENGTH(%s) " +
                "ORDER BY length",
                wrapIdentifier(columnName),
                wrapIdentifier(tableName),
                wrapIdentifier(columnName)
            );
        } else if (dataType.contains("NUMBER") || dataType.contains("FLOAT")) {
            // 数值类型，使用TRUNC函数进行分桶
            sql = String.format(
                "SELECT " +
                "TRUNC(%s, 1) AS bucket, " +
                "COUNT(*) AS count " +
                "FROM %s " +
                "GROUP BY TRUNC(%s, 1) " +
                "ORDER BY bucket",
                wrapIdentifier(columnName),
                wrapIdentifier(tableName),
                wrapIdentifier(columnName)
            );
            } else if (dataType.contains("DATE") || dataType.contains("TIMESTAMP")) {
            // 日期类型，按天分组
            sql = String.format(
                "SELECT " +
                "TRUNC(%s) AS day, " +
                "COUNT(*) AS count " +
                "FROM %s " +
                "GROUP BY TRUNC(%s) " +
                "ORDER BY day",
                wrapIdentifier(columnName),
                wrapIdentifier(tableName),
                wrapIdentifier(columnName)
            );
            } else {
            // 其他类型，直接按值分组
            sql = String.format(
                "SELECT " +
                "%s AS value, " +
                "COUNT(*) AS count " +
                "FROM %s " +
                "GROUP BY %s " +
                "ORDER BY value",
                wrapIdentifier(columnName),
                wrapIdentifier(tableName),
                wrapIdentifier(columnName)
            );
        }
        
        // ... existing code for execution ...
        
        return result;
    }

    @Override
    public Map<String, Object> getTableUpdateFrequency(String tableName) throws Exception {
        Map<String, Object> frequency = new HashMap<>();
        
        // 在Oracle中，可以通过查询DBA_TAB_MODIFICATIONS或ALL_TAB_MODIFICATIONS来获取表的修改信息
        // 但这需要特定的权限，且不是所有Oracle版本都支持
        // 这里提供一个基本实现，实际使用时可能需要根据具体环境调整
        
        String sql = "SELECT t.last_analyzed, " +
                     "s.timestamp as last_ddl_time " +
                     "FROM all_tables t " +
                     "JOIN all_objects s ON t.table_name = s.object_name AND t.owner = s.owner " +
                     "WHERE t.table_name = ? " +
                     "AND t.owner = ? " +
                     "AND s.object_type = 'TABLE'";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tableName.toUpperCase());
            stmt.setString(2, getSchema().toUpperCase());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    frequency.put("last_analyzed", rs.getTimestamp("last_analyzed"));
                    frequency.put("last_ddl_time", rs.getTimestamp("last_ddl_time"));
                    
                    // 注意：Oracle不直接提供表的DML操作时间
                    frequency.put("note", "Oracle does not directly provide DML operation times. " +
                                         "Consider using triggers or audit features to track update frequency.");
                }
            }
        }
        
        return frequency;
    }

    @Override
    public List<Map<String, Object>> getTableGrowthTrend(String tableName, int days) throws Exception {
        // Oracle中使用TRUNC函数进行日期截断
        String sql = String.format(
            "SELECT " +
            "TO_CHAR(d.day, 'YYYY-MM-DD') AS date, " +
            "NVL(t.count, 0) AS count " +
            "FROM (" +
            "  SELECT TRUNC(SYSDATE) - LEVEL + 1 AS day " +
            "  FROM DUAL " +
            "  CONNECT BY LEVEL <= %d" +
            ") d LEFT JOIN (" +
            "  SELECT TRUNC(created_date) AS day, COUNT(*) AS count " +
            "  FROM %s " +
            "  WHERE created_date >= TRUNC(SYSDATE) - %d " +
            "  GROUP BY TRUNC(created_date)" +
            ") t ON d.day = t.day " +
            "ORDER BY d.day",
            days,
            wrapIdentifier(tableName),
            days
        );
        
        // ... existing code for execution ...
        
        return new ArrayList<>();
    }

    @Override
    public List<Map<String, Object>> getTableDataSample(String tableName, int sampleSize) throws Exception {
        // Oracle 的 SAMPLE 语法使用百分比，而不是具体行数
        int samplePercent = Math.min(sampleSize * 10, 100); // 确保不超过100%
        
        // 优先使用 SAMPLE BLOCK 以获得更好的性能
        String sql = String.format(
            "SELECT * FROM %s SAMPLE BLOCK (%d)",
            wrapIdentifier(tableName),
            samplePercent
        );
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.setFetchSize(sampleSize); // 设置 JDBC 驱动一次获取的行数
            
            try (ResultSet rs = stmt.executeQuery(sql)) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                List<Map<String, Object>> samples = new ArrayList<>();
                int rowCount = 0;
                while (rs.next() && rowCount < sampleSize) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }
                    samples.add(row);
                    rowCount++;
                }
                return samples;
            }
        }
    }

    @Override
    public Map<String, Object> getColumnValueRange(String tableName, String columnName) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        // 获取列信息
        Map<String, Object> columnInfo = getColumnInfo(tableName, columnName);
        String dataType = (String) columnInfo.get("DATA_TYPE");
        
        String sql;
        if (dataType.contains("NUMBER") || dataType.contains("FLOAT")) {
            // 数值类型
            sql = String.format(
                "SELECT " +
                "MIN(%s) AS min_value, " +
                "MAX(%s) AS max_value, " +
                "AVG(%s) AS avg_value, " +
                "STDDEV(%s) AS std_dev " +
                "FROM %s",
                wrapIdentifier(columnName),
                wrapIdentifier(columnName),
                wrapIdentifier(columnName),
                wrapIdentifier(columnName),
                wrapIdentifier(tableName)
            );
            } else if (dataType.contains("DATE") || dataType.contains("TIMESTAMP")) {
            // 日期类型
            sql = String.format(
                "SELECT " +
                "TO_CHAR(MIN(%s), 'YYYY-MM-DD HH24:MI:SS') AS min_value, " +
                "TO_CHAR(MAX(%s), 'YYYY-MM-DD HH24:MI:SS') AS max_value, " +
                "COUNT(DISTINCT %s) AS distinct_count " +
                "FROM %s",
                wrapIdentifier(columnName),
                wrapIdentifier(columnName),
                wrapIdentifier(columnName),
                wrapIdentifier(tableName)
            );
            } else {
            // 字符类型或其他类型
            sql = String.format(
                "SELECT " +
                "MIN(LENGTH(%s)) AS min_length, " +
                "MAX(LENGTH(%s)) AS max_length, " +
                "AVG(LENGTH(%s)) AS avg_length, " +
                "COUNT(DISTINCT %s) AS distinct_count " +
                "FROM %s",
                wrapIdentifier(columnName),
                wrapIdentifier(columnName),
                wrapIdentifier(columnName),
                wrapIdentifier(columnName),
                wrapIdentifier(tableName)
            );
        }
        
        // ... existing code for execution ...
        
        return result;
    }

    @Override
    public List<Map<String, Object>> getColumnValueDistribution(String tableName, String columnName, int topN) throws Exception {
        if (tableName == null || columnName == null) {
            return new ArrayList<>();
        }
        
        String sql = String.format(
            "SELECT %s as value, COUNT(*) as count, " +
            "ROUND(100.0 * COUNT(*) / (SELECT COUNT(*) FROM %s), 2) as percentage " +
            "FROM %s " +
            "WHERE %s IS NOT NULL " +
            "GROUP BY %s " +
            "ORDER BY count DESC " +
            "FETCH FIRST %d ROWS ONLY",
            wrapIdentifier(columnName),
            wrapIdentifier(tableName),
            wrapIdentifier(tableName),
            wrapIdentifier(columnName),
            wrapIdentifier(columnName),
            topN
        );
        
        return executeQuery(sql);
    }

    @Override
    public Map<String, Object> calculateQualityMetric(String tableName, String metricType) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
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
                result.put("error", "Unsupported metric type: " + metricType);
                break;
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getQualityIssues(String tableName) throws Exception {
        Map<String, Object> issues = new HashMap<>();
        
        // 获取各类质量问题统计
        issues.put("issue_count", getQualityIssueCount(tableName));
        issues.put("issue_distribution", getQualityIssueDistribution(tableName));
        
        // 获取各类质量指标
        issues.put("completeness", getTableCompleteness(tableName));
        issues.put("accuracy", getTableAccuracy(tableName));
        issues.put("consistency", getTableConsistency(tableName));
        issues.put("uniqueness", getTableUniqueness(tableName));
        issues.put("validity", getTableValidity(tableName));
        
        return issues;
    }

    @Override
    public List<Map<String, Object>> getTableForeignKeyRelations(String tableName) throws Exception {
        List<Map<String, Object>> relations = new ArrayList<>();
        
        String sql = "SELECT a.constraint_name, a.table_name as source_table, " +
                     "b.column_name as source_column, " +
                     "c.table_name as target_table, " +
                     "d.column_name as target_column, " +
                     "a.delete_rule " +
                     "FROM all_constraints a " +
                     "JOIN all_cons_columns b ON a.constraint_name = b.constraint_name AND a.owner = b.owner " +
                     "JOIN all_constraints c ON a.r_constraint_name = c.constraint_name AND a.r_owner = c.owner " +
                     "JOIN all_cons_columns d ON c.constraint_name = d.constraint_name AND c.owner = d.owner " +
                     "WHERE a.constraint_type = 'R' " +
                     "AND a.table_name = ? " +
                     "AND a.owner = ? " +
                     "ORDER BY a.constraint_name, b.position";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tableName.toUpperCase());
            stmt.setString(2, getSchema().toUpperCase());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> relation = new HashMap<>();
                    relation.put("constraint_name", rs.getString("constraint_name"));
                    relation.put("source_table", rs.getString("source_table"));
                    relation.put("source_column", rs.getString("source_column"));
                    relation.put("target_table", rs.getString("target_table"));
                    relation.put("target_column", rs.getString("target_column"));
                    relation.put("delete_rule", rs.getString("delete_rule"));
                    relations.add(relation);
                }
            }
        }
        
        return relations;
    }

    @Override
    public List<Map<String, Object>> getReferencedByRelations(String tableName) throws Exception {
        // 查询哪些表引用了指定表（反向外键关系）
        String sql = String.format(
            "SELECT " +
            "a.constraint_name AS fk_name, " +
            "a.table_name AS fk_table_name, " +
            "b.column_name AS fk_column_name, " +
            "c.table_name AS referenced_table_name, " +
            "d.column_name AS referenced_column_name " +
            "FROM user_constraints a " +
            "JOIN user_cons_columns b ON a.constraint_name = b.constraint_name " +
            "JOIN user_constraints c ON a.r_constraint_name = c.constraint_name " +
            "JOIN user_cons_columns d ON c.constraint_name = d.constraint_name " +
                     "WHERE a.constraint_type = 'R' " +
            "AND c.table_name = '%s' " +
            "ORDER BY a.constraint_name, b.position",
            tableName.toUpperCase()
        );
        
        // ... existing code for execution ...
        
        return new ArrayList<>();
    }

    @Override
    public List<Map<String, Object>> getTableDependencies(String tableName) throws Exception {
        List<Map<String, Object>> dependencies = new ArrayList<>();
        
        String sql = "SELECT DISTINCT d.owner AS schema_name, d.name AS object_name, d.type AS object_type, " +
                     "d.referenced_owner AS referenced_schema, d.referenced_name AS referenced_object, " +
                     "d.referenced_type AS referenced_type " +
                     "FROM all_dependencies d " +
                     "WHERE d.referenced_name = ? " +
                     "ORDER BY d.type, d.name";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tableName.toUpperCase());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> dependency = new HashMap<>();
                    dependency.put("schema_name", rs.getString("schema_name"));
                    dependency.put("object_name", rs.getString("object_name"));
                    dependency.put("object_type", rs.getString("object_type"));
                    dependency.put("referenced_schema", rs.getString("referenced_schema"));
                    dependency.put("referenced_object", rs.getString("referenced_object"));
                    dependency.put("referenced_type", rs.getString("referenced_type"));
                    dependencies.add(dependency);
                }
            }
        }
        
        return dependencies;
    }



    @Override
    public Map<String, String> getStoredProcedureDefinitions(String tableName) throws Exception {
        // 当前实现只有查询SQL，未实现结果处理
        String sql = String.format(
            "SELECT " +
            "object_name, " +
            "DBMS_METADATA.GET_DDL(object_type, object_name) AS definition " +
            "FROM user_objects " +
            "WHERE object_type IN ('PROCEDURE', 'FUNCTION', 'PACKAGE', 'TRIGGER') " +
            "AND object_name LIKE '%%%s%%'",
            tableName.toUpperCase()
        );
        
        Map<String, String> procedures = new HashMap<>();
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String objectName = rs.getString("object_name");
                    String definition = rs.getString("definition");
                    procedures.put(objectName, definition);
                }
            }
        } catch (Exception e) {
            log.error("Failed to get stored procedure definitions: table={}, error={}", 
                     tableName, e.getMessage(), e);
        }
        
        return procedures;
    }

    // 向表添加主键约束
    public String getAddPrimaryKeySql(String tableName, List<String> columnNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("ALTER TABLE ")
          .append(wrapIdentifier(tableName))
          .append(" ADD CONSTRAINT ")
          .append(wrapIdentifier("PK_" + tableName))
          .append(" PRIMARY KEY (");
        
        for (int i = 0; i < columnNames.size(); i++) {
            sb.append(wrapIdentifier(columnNames.get(i)));
            if (i < columnNames.size() - 1) {
                sb.append(", ");
            }
        }
        
        sb.append(")");
        
        return sb.toString();
    }

    // 添加序列生成方法，用于模拟自增列功能
    public String getCreateSequenceSql(String tableName, String columnName) {
        String sequenceName = tableName + "_" + columnName + "_SEQ";
        return String.format(
            "CREATE SEQUENCE %s START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE",
            wrapIdentifier(sequenceName)
        );
    }

    // 添加触发器方法，用于实现自增功能
    public String getCreateAutoIncrementTriggerSql(String tableName, String columnName) {
        String triggerName = tableName + "_" + columnName + "_TRG";
        String sequenceName = tableName + "_" + columnName + "_SEQ";
        
        // 修改触发器实现，避免在高并发下可能的互斥问题
        return String.format(
            "CREATE OR REPLACE TRIGGER %s\n" +
            "BEFORE INSERT ON %s\n" +
            "FOR EACH ROW\n" +
            "BEGIN\n" +
            "  IF :NEW.%s IS NULL THEN\n" +    // 只在值为NULL时设置序列值
            "    SELECT %s.NEXTVAL INTO :NEW.%s FROM DUAL;\n" +
            "  END IF;\n" +
            "END;",
            wrapIdentifier(triggerName),
            wrapIdentifier(tableName),
            wrapIdentifier(columnName),
            wrapIdentifier(sequenceName),
            wrapIdentifier(columnName)
        );
    }

    // 获取表关联的序列
    public List<Map<String, Object>> getTableSequences(String tableName) throws Exception {
        // 查询与表关联的序列（通常用于自增ID）
        String sql = String.format(
            "SELECT " +
            "sequence_name, " +
            "last_number, " +
            "min_value, " +
            "max_value, " +
            "increment_by, " +
            "cycle_flag " +
            "FROM user_sequences " +
            "WHERE sequence_name LIKE '%s%%'",
            tableName.toUpperCase()
        );
        
        List<Map<String, Object>> sequences = new ArrayList<>();
        
        // ... existing code for execution ...
        
        return sequences;
    }

    // 更新序列的下一个值
    public void resetSequence(String sequenceName, long newValue) throws Exception {
        // Oracle重设序列值的方法
        try (Connection conn = getConnection()) {
            // 先获取序列的当前信息
            String infoSql = String.format(
                "SELECT " +
                "increment_by, min_value, max_value, cycle_flag " +
                "FROM user_sequences " +
                "WHERE sequence_name = '%s'",
                sequenceName.toUpperCase()
            );
            
            Map<String, Object> seqInfo = null;
            // ... 执行查询获取seqInfo ...
            
            if (seqInfo != null) {
                // 需要使用DROP和CREATE来重设序列值
                int incrementBy = ((Number) seqInfo.get("INCREMENT_BY")).intValue();
                long minValue = ((Number) seqInfo.get("MIN_VALUE")).longValue();
                long maxValue = ((Number) seqInfo.get("MAX_VALUE")).longValue();
                String cycleFlag = (String) seqInfo.get("CYCLE_FLAG");
                
                try (Statement stmt = conn.createStatement()) {
                    // 删除原序列
                    stmt.execute("DROP SEQUENCE " + wrapIdentifier(sequenceName));
                    
                    // 创建新序列，从指定值开始
                    String createSql = String.format(
                        "CREATE SEQUENCE %s " +
                        "START WITH %d " +
                        "INCREMENT BY %d " +
                        "MINVALUE %d " +
                        "MAXVALUE %d " +
                        "%s",
                        wrapIdentifier(sequenceName),
                        newValue,
                        incrementBy,
                        minValue,
                        maxValue,
                        "Y".equals(cycleFlag) ? "CYCLE" : "NOCYCLE"
                    );
                    
                    stmt.execute(createSql);
                }
            }
        }
    }

    // 添加一个辅助方法，处理Oracle中的字符串连接
    private String getConcatenationSql(String... parts) {
        // Oracle使用 || 运算符连接字符串
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                result.append(" || ");
            }
            result.append(parts[i]);
        }
        return result.toString();
    }

    // 示例使用:
    // 而不是使用 "column1 + ' ' + column2"
    // 应该使用 getConcatenationSql("column1", "' '", "column2")

    // 添加处理Oracle日期函数的辅助方法
    private String getDateFormatSql(String column, String format) {
        // Oracle使用TO_CHAR将日期转换为字符串
        return String.format("TO_CHAR(%s, '%s')", column, format);
    }

    // 改进Oracle日期间隔处理函数
    private String getDateAddSql(String date, int amount, String unit) {
        // Oracle的INTERVAL语法
        String unitStr = unit.toUpperCase();
        if ("DAY".equals(unitStr) || "DAYS".equals(unitStr)) {
            return amount >= 0 
                ? String.format("%s + INTERVAL '%d' DAY", date, amount)
                : String.format("%s - INTERVAL '%d' DAY", date, Math.abs(amount));
        } else if ("MONTH".equals(unitStr) || "MONTHS".equals(unitStr)) {
            return amount >= 0 
                ? String.format("%s + INTERVAL '%d' MONTH", date, amount)
                : String.format("%s - INTERVAL '%d' MONTH", date, Math.abs(amount));
        } else if ("YEAR".equals(unitStr) || "YEARS".equals(unitStr)) {
            return amount >= 0 
                ? String.format("%s + INTERVAL '%d' YEAR", date, amount)
                : String.format("%s - INTERVAL '%d' YEAR", date, Math.abs(amount));
        } else if ("HOUR".equals(unitStr) || "HOURS".equals(unitStr)) {
            return amount >= 0 
                ? String.format("%s + INTERVAL '%d' HOUR", date, amount)
                : String.format("%s - INTERVAL '%d' HOUR", date, Math.abs(amount));
        } else if ("MINUTE".equals(unitStr) || "MINUTES".equals(unitStr)) {
            return amount >= 0 
                ? String.format("%s + INTERVAL '%d' MINUTE", date, amount)
                : String.format("%s - INTERVAL '%d' MINUTE", date, Math.abs(amount));
        } else if ("SECOND".equals(unitStr) || "SECONDS".equals(unitStr)) {
            return amount >= 0 
                ? String.format("%s + INTERVAL '%d' SECOND", date, amount)
                : String.format("%s - INTERVAL '%d' SECOND", date, Math.abs(amount));
        }
        
        // 默认按天处理
        return amount >= 0 
            ? String.format("%s + %d", date, amount)
            : String.format("%s - %d", date, Math.abs(amount));
    }

    private String getDateDiffSql(String date1, String date2, String unit) {
        // Oracle计算日期差
        unit = unit.toUpperCase();
        if ("DAY".equals(unit)) {
            return String.format("(%s - %s)", date1, date2);
        } else if ("MONTH".equals(unit)) {
            return String.format("MONTHS_BETWEEN(%s, %s)", date1, date2);
        } else if ("YEAR".equals(unit)) {
            return String.format("EXTRACT(YEAR FROM %s) - EXTRACT(YEAR FROM %s)", date1, date2);
        }
        return String.format("(%s - %s)", date1, date2); // 默认按天
    }

    // 添加一个辅助方法，处理NULL值比较
    private String getNullCheckSql(String column, boolean isNull) {
        // Oracle中检查NULL值
        if (isNull) {
            return String.format("%s IS NULL", column);
        } else {
            return String.format("%s IS NOT NULL", column);
        }
    }

    // 添加一个辅助方法，处理空字符串检查
    // 注意：Oracle中NULL和空字符串是相同的
    private String getEmptyStringCheckSql(String column, boolean isEmpty) {
        // Oracle中检查空字符串
        if (isEmpty) {
            return String.format("(%s IS NULL OR %s = '')", column, column);
        } else {
            return String.format("%s IS NOT NULL AND %s <> ''", column, column);
        }
    }

    // 实现处理自增列的方法：创建完整的自增方案（序列+触发器）
    public void createAutoIncrementColumn(String tableName, String columnName) throws Exception {
        // 1. 创建序列
        String createSequenceSql = getCreateSequenceSql(tableName, columnName);
        
        // 2. 创建触发器
        String createTriggerSql = getCreateAutoIncrementTriggerSql(tableName, columnName);
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            // 执行创建序列
            stmt.execute(createSequenceSql);
            
            // 执行创建触发器
            stmt.execute(createTriggerSql);
        }
    }

    // 获取当前序列值
    public long getCurrentSequenceValue(String tableName, String columnName) throws Exception {
        String sequenceName = tableName + "_" + columnName + "_SEQ";
        String sql = String.format(
            "SELECT %s.CURRVAL FROM DUAL",
            wrapIdentifier(sequenceName)
        );
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            // 序列可能尚未被使用，会抛出ORA-08002错误
            // 在这种情况下，返回0或默认起始值
            if (e.getErrorCode() == 8002) {
                return 0;
            }
            throw e;
        }
        
        return 0;
    }

    // 添加Oracle分析函数的支持方法
    public String getRowNumberSql(String partitionColumns, String orderColumns) {
        StringBuilder sql = new StringBuilder("ROW_NUMBER() OVER(");
        
        if (partitionColumns != null && !partitionColumns.isEmpty()) {
            sql.append("PARTITION BY ").append(partitionColumns);
        }
        
        if (orderColumns != null && !orderColumns.isEmpty()) {
            if (partitionColumns != null && !partitionColumns.isEmpty()) {
                sql.append(" ");
            }
            sql.append("ORDER BY ").append(orderColumns);
        }
        
        sql.append(")");
        return sql.toString();
    }

    // 添加获取前N行分析的方法
    public String getTopNRowsSql(String tableName, String orderColumn, boolean ascending, int n) {
        String direction = ascending ? "ASC" : "DESC";
        
        // Oracle 12c+语法
        return String.format(
            "SELECT * FROM %s ORDER BY %s %s FETCH FIRST %d ROWS ONLY",
            wrapIdentifier(tableName),
            wrapIdentifier(orderColumn),
            direction,
            n
        );
        
        // Oracle 12c以下的语法
        /*
        return String.format(
            "SELECT * FROM (SELECT * FROM %s ORDER BY %s %s) WHERE ROWNUM <= %d",
            wrapIdentifier(tableName),
            wrapIdentifier(orderColumn),
            direction,
            n
        );
        */
    }

    // 添加处理NULL值的Oracle特有函数
    private String getNvlSql(String expression, String defaultValue) {
        return String.format("NVL(%s, %s)", expression, defaultValue);
    }

    // 添加Oracle条件判断函数
    private String getDecodeSql(String expression, Object... cases) {
        if (cases.length % 2 != 1) {
            throw new IllegalArgumentException("DECODE requires odd number of case arguments");
        }
        
        StringBuilder sb = new StringBuilder("DECODE(");
        sb.append(expression);
        
        for (Object arg : cases) {
            sb.append(", ");
            if (arg instanceof String) {
                sb.append("'").append(arg).append("'");
            } else {
                sb.append(arg);
            }
        }
        
        sb.append(")");
        return sb.toString();
    }

    // 使用示例:
    // getNvlSql("column_name", "'Default'") -> "NVL(column_name, 'Default')"
    // getDecodeSql("status", "A", "'Active'", "I", "'Inactive'", "'Unknown'") 
    // -> "DECODE(status, 'A', 'Active', 'I', 'Inactive', 'Unknown')"

    // 添加一个方法来检测Oracle版本，以便选择合适的语法
    private boolean isOracle12cOrHigher(Connection conn) throws SQLException {
            try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT VERSION FROM PRODUCT_COMPONENT_VERSION WHERE PRODUCT LIKE 'Oracle Database%'")) {
                if (rs.next()) {
                String version = rs.getString(1);
                // 提取主版本号
                if (version != null && version.matches("\\d+\\.\\d+\\.\\d+.*")) {
                    String[] parts = version.split("\\.");
                    int majorVersion = Integer.parseInt(parts[0]);
                    return majorVersion >= 12;
                }
            }
        } catch (Exception e) {
            // 失败就假设是旧版本
            log.warn("Unable to determine Oracle version, assuming pre-12c: {}", e.getMessage());
        }
        return false;
    }

    // 使用版本检测来提供正确的分页SQL
    public String getVersionAwarePageSql(String sql, int offset, int limit) throws SQLException, Exception {
        Connection conn = getConnection();
        try {
            boolean isOracle12c = isOracle12cOrHigher(conn);
            
            if (isOracle12c) {
                // Oracle 12c 及以上版本支持 OFFSET FETCH 语法
                return String.format(
                    "SELECT * FROM (%s) OFFSET %d ROWS FETCH NEXT %d ROWS ONLY",
                    sql, offset, limit
                );
            } else {
                // Oracle 11g 及以下版本使用子查询和 ROWNUM
                return String.format(
                    "SELECT * FROM (SELECT t.*, ROWNUM rn FROM (%s) t WHERE ROWNUM <= %d) WHERE rn > %d",
                    sql, offset + limit, offset
                );
            }
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // 记录异常但不抛出
                    log.error("Error closing Connection", e);
                }
            }
        }
    }

    // 添加用于创建自增主键列的完整方法
    public void createIdColumnWithSequence(String tableName, String columnName) throws Exception {
        // 1. 添加ID列
        String addColumnSql = String.format(
            "ALTER TABLE %s ADD %s NUMBER(19,0) NOT NULL",
            wrapIdentifier(tableName),
            wrapIdentifier(columnName)
        );
        
        // 2. 设置为主键
        String addPkSql = String.format(
            "ALTER TABLE %s ADD CONSTRAINT %s PRIMARY KEY (%s)",
            wrapIdentifier(tableName),
            wrapIdentifier("PK_" + tableName),
            wrapIdentifier(columnName)
        );
        
        // 3. 创建序列
        String createSequenceSql = getCreateSequenceSql(tableName, columnName);
        
        // 4. 创建触发器
        String createTriggerSql = getCreateAutoIncrementTriggerSql(tableName, columnName);
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            // 开始事务
            conn.setAutoCommit(false);
            try {
                // 执行所有步骤
                stmt.execute(addColumnSql);
                stmt.execute(addPkSql);
                stmt.execute(createSequenceSql);
                stmt.execute(createTriggerSql);
                
                // 提交事务
                conn.commit();
            } catch (Exception e) {
                // 回滚事务
                conn.rollback();
                throw e;
            } finally {
                // 恢复自动提交
                conn.setAutoCommit(true);
            }
        }
    }

    // 获取表分区信息
    public List<Map<String, Object>> getTablePartitions(String tableName) throws Exception {
        List<Map<String, Object>> partitions = new ArrayList<>();
        
        String sql = "SELECT partition_name, high_value, partition_position, " +
                     "num_rows, blocks, empty_blocks, avg_space, chain_cnt, " +
                     "avg_row_len, sample_size, last_analyzed " +
                     "FROM all_tab_partitions " +
                     "WHERE table_owner = ? AND table_name = ? " +
                     "ORDER BY partition_position";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, getSchema());
            stmt.setString(2, tableName.toUpperCase());
                
                try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                    while (rs.next()) {
                    Map<String, Object> partition = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        partition.put(columnName, value);
                    }
                    partitions.add(partition);
                }
            }
        }
        
        return partitions;
    }

    // 重建索引
    public void rebuildIndex(String indexName) throws Exception {
        String sql = String.format("ALTER INDEX %s REBUILD", wrapIdentifier(indexName));
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    // 分析索引
    public void analyzeIndex(String indexName) throws Exception {
        String sql = String.format("ANALYZE INDEX %s COMPUTE STATISTICS", wrapIdentifier(indexName));
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    // 添加这个方法到 OracleHandler 类中
    private String getDatabase() {
        return getDatabaseProperty("database", "");
    }

    private String getDatabaseProperty(String key, String defaultValue) {
        // 这里添加获取配置属性的逻辑
        // 可能是从某个配置对象或上下文中获取
        return defaultValue;
    }

    // 添加这些辅助方法
    private String getHost() {
        return getDatabaseProperty("host", "localhost");
    }

    private String getPort() {
        return getDatabaseProperty("port", getDefaultPort());
    }


    public List<Map<String, Object>> executeQuery(String sql) throws Exception {
        if (sql == null || sql.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Map<String, Object>> results = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                results.add(row);
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    // 修改这里，使用 log 而不是 logger
                    log.error("Error closing ResultSet", e);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    // 修改这里，使用 log 而不是 logger
                    log.error("Error closing Statement", e);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // 修改这里，使用 log 而不是 logger
                    log.error("Error closing Connection", e);
                }
            }
        }
        
        return results;
    }

    public int executeUpdate(String sql) throws Exception {
        if (sql == null || sql.trim().isEmpty()) {
            return 0;
        }

        Connection conn = null;
        Statement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.createStatement();
            return stmt.executeUpdate(sql);
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    // 修改这里，使用 log 而不是 logger
                    log.error("Error closing Statement", e);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // 修改这里，使用 log 而不是 logger
                    log.error("Error closing Connection", e);
                }
            }
        }
    }

    private void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                log.error("Error closing connection", e);
            }
        }
    }

    private void closeStatement(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                log.error("Error closing statement", e);
            }
        }
    }

    private void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                log.error("Error closing result set", e);
            }
        }
    }

    @Override
    public List<String> getTablePartitions(String tableName, String partitionField) throws Exception {
        List<String> partitions = new ArrayList<>();
        
        try (Connection conn = getConnection()) {
            // 首先尝试从Oracle分区表信息中获取分区
            String partitionSql = String.format(
                "SELECT PARTITION_NAME, HIGH_VALUE " +
                "FROM ALL_TAB_PARTITIONS " +
                "WHERE TABLE_OWNER = '%s' AND TABLE_NAME = '%s' " +
                "ORDER BY PARTITION_POSITION",
                getSchema().toUpperCase(), tableName.toUpperCase()
            );
            
            boolean hasPartitions = false;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(partitionSql)) {
                while (rs.next()) {
                    hasPartitions = true;
                    String highValue = rs.getString("HIGH_VALUE");
                    
                    // 尝试从分区高值中提取与partitionField相关的值
                    String extractedValue = extractPartitionValue(highValue, partitionField);
                    if (extractedValue != null && !extractedValue.isEmpty()) {
                        partitions.add(extractedValue);
                    }
                }
            } catch (SQLException e) {
                log.warn("获取表分区信息失败: {}", e.getMessage());
            }
            
            // 如果没有找到分区或无法从分区中提取值，则回退到查询唯一值
            if (!hasPartitions || partitions.isEmpty()) {
                String distinctValuesSql = String.format(
                    "SELECT DISTINCT %s FROM %s WHERE %s IS NOT NULL ORDER BY %s",
                    wrapIdentifier(partitionField), 
                    wrapIdentifier(tableName),
                    wrapIdentifier(partitionField),
                    wrapIdentifier(partitionField)
                );
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(distinctValuesSql)) {
                    // 清空之前可能已经填充的分区值
                    if (!partitions.isEmpty()) {
                        partitions.clear();
                    }
                    
                    while (rs.next()) {
                        String value = rs.getString(1);
                        if (value != null) {
                            partitions.add(value);
                        }
                    }
                } catch (Exception e) {
                    log.error("获取表的分区字段值失败: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("获取表分区失败: {}", e.getMessage());
            throw e;
        }
        
        return partitions;
    }
    
    // 辅助方法：从Oracle分区高值中提取与特定字段相关的值
    private String extractPartitionValue(String highValue, String partitionField) {
        if (highValue == null || highValue.isEmpty()) {
            return "";
        }
        
        // Oracle的高值表达式通常格式为：
        // MAXVALUE
        // 或者 TO_DATE(' 2023-01-01 00:00:00', 'SYYYY-MM-DD HH24:MI:SS')
        // 或者 '字符串值'
        
        try {
            // 处理日期类型
            if (highValue.contains("TO_DATE")) {
                Pattern pattern = Pattern.compile("TO_DATE\\('\\s*(.+?)\\s*'");
                Matcher matcher = pattern.matcher(highValue);
                if (matcher.find()) {
                    return matcher.group(1).trim();
                }
            }
            
            // 处理字符串类型
            if (highValue.startsWith("'") && highValue.endsWith("'")) {
                return highValue.substring(1, highValue.length() - 1);
            }
            
            // 处理数值类型或其他类型
            if (highValue.contains(partitionField)) {
                // 尝试提取形如 "partitionField < 100" 中的 "100"
                Pattern pattern = Pattern.compile(partitionField + "\\s*[<>=]\\s*([^\\s,)]+)");
                Matcher matcher = pattern.matcher(highValue);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        } catch (Exception e) {
            log.warn("解析分区高值时出错: {}", e.getMessage());
        }
        
        return highValue; // 如果无法解析，返回原始高值
    }

    /**
     * 获取创建临时表的SQL
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
        // Oracle临时表语法：使用CREATE GLOBAL TEMPORARY TABLE
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
            if (column.containsKey("defaultValue") && column.get("defaultValue") != null) {
                String defaultValue = column.get("defaultValue").toString();
                sql.append(" DEFAULT ").append(defaultValue);
            }
        }
        
        // 添加主键
        if (primaryKeys != null && !primaryKeys.isEmpty()) {
            sql.append(", CONSTRAINT PK_").append(tempTableName).append(" PRIMARY KEY (");
            for (int i = 0; i < primaryKeys.size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append(wrapIdentifier(primaryKeys.get(i)));
            }
            sql.append(")");
        }
        
        sql.append(")");
        
        // 添加Oracle特有的参数
        if (preserveRows) {
            sql.append(" ON COMMIT PRESERVE ROWS");
        } else {
            sql.append(" ON COMMIT DELETE ROWS");
        }
        
        return sql.toString();
    }

} 