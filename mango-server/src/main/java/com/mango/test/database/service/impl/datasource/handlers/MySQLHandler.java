package com.mango.test.database.service.impl.datasource.handlers;

import com.mango.test.database.entity.ColumnDefinition;
import com.mango.test.database.entity.DataSource;
import com.mango.test.database.entity.IndexDefinition;
import com.mango.test.database.entity.TableDefinition;
import com.mango.test.database.service.impl.datasource.AbstractDatabaseHandler;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.json.JSONArray;

public class MySQLHandler extends AbstractDatabaseHandler {

    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";
    private static final String DEFAULT_PORT = "3306";
    private static final String URL_TEMPLATE = "jdbc:mysql://%s:%s/%s?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai";

    private static final int DEFAULT_VARCHAR_LENGTH = 255;
    private static final int MAX_VARCHAR_LENGTH = 16383; // 当使用 utf8mb4 时，最大值应为 16383 而非 65535

    private static final Map<String, String> TYPE_MAPPING = new HashMap<>();
    private static final Map<String, Integer> DEFAULT_LENGTH_MAPPING = new HashMap<>();

    private static final Map<String, Map<String, String>> COMMON_TYPE_MAPPING = new HashMap<>();

    static {
        // 字符串类型
        TYPE_MAPPING.put("string", "VARCHAR");
        TYPE_MAPPING.put("char", "CHAR");
        TYPE_MAPPING.put("text", "TEXT");
        TYPE_MAPPING.put("mediumtext", "MEDIUMTEXT");
        TYPE_MAPPING.put("longtext", "LONGTEXT");
        TYPE_MAPPING.put("tinytext", "TINYTEXT");

        // 数值类型
        TYPE_MAPPING.put("tinyint", "TINYINT");
        TYPE_MAPPING.put("smallint", "SMALLINT");
        TYPE_MAPPING.put("mediumint", "MEDIUMINT");
        TYPE_MAPPING.put("integer", "INT");
        TYPE_MAPPING.put("int", "INT");
        TYPE_MAPPING.put("bigint", "BIGINT");
        TYPE_MAPPING.put("float", "FLOAT");
        TYPE_MAPPING.put("double", "DOUBLE");
        TYPE_MAPPING.put("decimal", "DECIMAL");
        TYPE_MAPPING.put("numeric", "DECIMAL");

        // 日期时间类型
        TYPE_MAPPING.put("date", "DATE");
        TYPE_MAPPING.put("time", "TIME");
        TYPE_MAPPING.put("datetime", "DATETIME");
        TYPE_MAPPING.put("timestamp", "TIMESTAMP");
        TYPE_MAPPING.put("year", "YEAR");

        // 二进制类型
        TYPE_MAPPING.put("binary", "BINARY");
        TYPE_MAPPING.put("varbinary", "VARBINARY");
        TYPE_MAPPING.put("tinyblob", "TINYBLOB");
        TYPE_MAPPING.put("blob", "BLOB");
        TYPE_MAPPING.put("mediumblob", "MEDIUMBLOB");
        TYPE_MAPPING.put("longblob", "LONGBLOB");

        // 其他类型
        TYPE_MAPPING.put("boolean", "TINYINT");
        TYPE_MAPPING.put("bit", "BIT");
        TYPE_MAPPING.put("enum", "ENUM");
        TYPE_MAPPING.put("set", "SET");
        TYPE_MAPPING.put("json", "JSON");

        // 默认长度映射
        DEFAULT_LENGTH_MAPPING.put("VARCHAR", 255);
        DEFAULT_LENGTH_MAPPING.put("CHAR", 1);
        DEFAULT_LENGTH_MAPPING.put("BINARY", 1);
        DEFAULT_LENGTH_MAPPING.put("VARBINARY", 255);
        DEFAULT_LENGTH_MAPPING.put("BIT", 1);
        DEFAULT_LENGTH_MAPPING.put("TINYINT", 4);
        DEFAULT_LENGTH_MAPPING.put("SMALLINT", 6);
        DEFAULT_LENGTH_MAPPING.put("MEDIUMINT", 9);
        DEFAULT_LENGTH_MAPPING.put("INT", 11);
        DEFAULT_LENGTH_MAPPING.put("INTEGER", 11);
        DEFAULT_LENGTH_MAPPING.put("BIGINT", 20);
        DEFAULT_LENGTH_MAPPING.put("FLOAT", 10);
        DEFAULT_LENGTH_MAPPING.put("DOUBLE", 20);
        DEFAULT_LENGTH_MAPPING.put("DECIMAL", 10);
        DEFAULT_LENGTH_MAPPING.put("YEAR", 4);

        // Oracle 类型映射
        Map<String, String> oracleMapping = new HashMap<>();
        oracleMapping.put("VARCHAR2", "VARCHAR");
        oracleMapping.put("NVARCHAR2", "VARCHAR");
        oracleMapping.put("CHAR", "CHAR");
        oracleMapping.put("NCHAR", "CHAR");
        oracleMapping.put("NUMBER", "DECIMAL");
        oracleMapping.put("INTEGER", "INT");
        oracleMapping.put("FLOAT", "FLOAT");
        oracleMapping.put("BINARY_FLOAT", "FLOAT");
        oracleMapping.put("BINARY_DOUBLE", "DOUBLE");
        oracleMapping.put("DATE", "DATETIME");
        oracleMapping.put("TIMESTAMP", "TIMESTAMP");
        oracleMapping.put("CLOB", "LONGTEXT");
        oracleMapping.put("NCLOB", "LONGTEXT");
        oracleMapping.put("BLOB", "LONGBLOB");
        oracleMapping.put("RAW", "VARBINARY");
        oracleMapping.put("LONG RAW", "LONGBLOB");
        COMMON_TYPE_MAPPING.put("oracle", oracleMapping);

        // PostgreSQL 类型映射
        Map<String, String> postgresMapping = new HashMap<>();
        postgresMapping.put("VARCHAR", "VARCHAR");
        postgresMapping.put("CHAR", "CHAR");
        postgresMapping.put("TEXT", "TEXT");
        postgresMapping.put("SMALLINT", "SMALLINT");
        postgresMapping.put("INTEGER", "INT");
        postgresMapping.put("BIGINT", "BIGINT");
        postgresMapping.put("DECIMAL", "DECIMAL");
        postgresMapping.put("NUMERIC", "DECIMAL");
        postgresMapping.put("REAL", "FLOAT");
        postgresMapping.put("DOUBLE PRECISION", "DOUBLE");
        postgresMapping.put("BOOLEAN", "TINYINT");
        postgresMapping.put("DATE", "DATE");
        postgresMapping.put("TIME", "TIME");
        postgresMapping.put("TIMESTAMP", "TIMESTAMP");
        postgresMapping.put("BYTEA", "LONGBLOB");
        postgresMapping.put("JSON", "JSON");
        postgresMapping.put("JSONB", "JSON");
        COMMON_TYPE_MAPPING.put("postgresql", postgresMapping);

        // SQL Server 类型映射
        Map<String, String> sqlserverMapping = new HashMap<>();
        sqlserverMapping.put("VARCHAR", "VARCHAR");
        sqlserverMapping.put("NVARCHAR", "VARCHAR");
        sqlserverMapping.put("CHAR", "CHAR");
        sqlserverMapping.put("NCHAR", "CHAR");
        sqlserverMapping.put("TEXT", "TEXT");
        sqlserverMapping.put("NTEXT", "TEXT");
        sqlserverMapping.put("TINYINT", "TINYINT");
        sqlserverMapping.put("SMALLINT", "SMALLINT");
        sqlserverMapping.put("INT", "INT");
        sqlserverMapping.put("BIGINT", "BIGINT");
        sqlserverMapping.put("DECIMAL", "DECIMAL");
        sqlserverMapping.put("NUMERIC", "DECIMAL");
        sqlserverMapping.put("FLOAT", "FLOAT");
        sqlserverMapping.put("REAL", "FLOAT");
        sqlserverMapping.put("MONEY", "DECIMAL");
        sqlserverMapping.put("SMALLMONEY", "DECIMAL");
        sqlserverMapping.put("BIT", "TINYINT");
        sqlserverMapping.put("DATE", "DATE");
        sqlserverMapping.put("TIME", "TIME");
        sqlserverMapping.put("DATETIME", "DATETIME");
        sqlserverMapping.put("DATETIME2", "DATETIME");
        sqlserverMapping.put("SMALLDATETIME", "DATETIME");
        sqlserverMapping.put("BINARY", "BINARY");
        sqlserverMapping.put("VARBINARY", "VARBINARY");
        sqlserverMapping.put("IMAGE", "LONGBLOB");
        COMMON_TYPE_MAPPING.put("sqlserver", sqlserverMapping);

        // 达梦数据库类型映射
        Map<String, String> dmMapping = new HashMap<>();
        dmMapping.put("VARCHAR", "VARCHAR");
        dmMapping.put("VARCHAR2", "VARCHAR");
        dmMapping.put("CHAR", "CHAR");
        dmMapping.put("TEXT", "TEXT");
        dmMapping.put("INT", "INT");
        dmMapping.put("INTEGER", "INT");
        dmMapping.put("NUMBER", "DECIMAL");
        dmMapping.put("DECIMAL", "DECIMAL");
        dmMapping.put("NUMERIC", "DECIMAL");
        dmMapping.put("FLOAT", "FLOAT");
        dmMapping.put("DOUBLE", "DOUBLE");
        dmMapping.put("BOOLEAN", "TINYINT");
        dmMapping.put("DATE", "DATE");
        dmMapping.put("TIME", "TIME");
        dmMapping.put("TIMESTAMP", "TIMESTAMP");
        dmMapping.put("BLOB", "LONGBLOB");
        dmMapping.put("CLOB", "LONGTEXT");
        dmMapping.put("BINARY", "BINARY");
        COMMON_TYPE_MAPPING.put("dm", dmMapping);

        // DM7 类型映射
        Map<String, String> dm7Mapping = new HashMap<>(dmMapping);
        COMMON_TYPE_MAPPING.put("dm7", dm7Mapping);

        // DM8 类型映射
        Map<String, String> dm8Mapping = new HashMap<>(dmMapping);
        COMMON_TYPE_MAPPING.put("dm8", dm8Mapping);

        // 人大金仓数据库类型映射
        Map<String, String> kingbaseMapping = new HashMap<>();
        kingbaseMapping.put("VARCHAR", "VARCHAR");
        kingbaseMapping.put("VARCHAR2", "VARCHAR");
        kingbaseMapping.put("NVARCHAR2", "VARCHAR");
        kingbaseMapping.put("CHAR", "CHAR");
        kingbaseMapping.put("NCHAR", "CHAR");
        kingbaseMapping.put("TEXT", "TEXT");
        kingbaseMapping.put("CLOB", "LONGTEXT");
        kingbaseMapping.put("NCLOB", "LONGTEXT");
        kingbaseMapping.put("BLOB", "LONGBLOB");
        kingbaseMapping.put("INTEGER", "INT");
        kingbaseMapping.put("INT", "INT");
        kingbaseMapping.put("SMALLINT", "SMALLINT");
        kingbaseMapping.put("BIGINT", "BIGINT");
        kingbaseMapping.put("NUMERIC", "DECIMAL");
        kingbaseMapping.put("NUMBER", "DECIMAL");
        kingbaseMapping.put("DECIMAL", "DECIMAL");
        kingbaseMapping.put("REAL", "FLOAT");
        kingbaseMapping.put("DOUBLE PRECISION", "DOUBLE");
        kingbaseMapping.put("FLOAT", "FLOAT");
        kingbaseMapping.put("BINARY_FLOAT", "FLOAT");
        kingbaseMapping.put("BINARY_DOUBLE", "DOUBLE");
        kingbaseMapping.put("DATE", "DATE");
        kingbaseMapping.put("TIME", "TIME");
        kingbaseMapping.put("TIMESTAMP", "TIMESTAMP");
        kingbaseMapping.put("INTERVAL", "VARCHAR");
        kingbaseMapping.put("BINARY", "BINARY");
        kingbaseMapping.put("VARBINARY", "VARBINARY");
        kingbaseMapping.put("RAW", "VARBINARY");
        kingbaseMapping.put("LONG RAW", "LONGBLOB");
        kingbaseMapping.put("BOOL", "TINYINT");
        kingbaseMapping.put("BOOLEAN", "TINYINT");
        kingbaseMapping.put("JSON", "JSON");
        kingbaseMapping.put("JSONB", "JSON");
        COMMON_TYPE_MAPPING.put("kingbase", kingbaseMapping);

        // MariaDB 类型映射 (基本与MySQL相同)
        Map<String, String> mariadbMapping = new HashMap<>(TYPE_MAPPING);
        COMMON_TYPE_MAPPING.put("mariadb", mariadbMapping);

        // DB2 类型映射
        Map<String, String> db2Mapping = new HashMap<>();
        db2Mapping.put("VARCHAR", "VARCHAR");
        db2Mapping.put("CHAR", "CHAR");
        db2Mapping.put("CLOB", "LONGTEXT");
        db2Mapping.put("DBCLOB", "LONGTEXT");
        db2Mapping.put("GRAPHIC", "VARCHAR");
        db2Mapping.put("VARGRAPHIC", "VARCHAR");
        db2Mapping.put("SMALLINT", "SMALLINT");
        db2Mapping.put("INTEGER", "INT");
        db2Mapping.put("BIGINT", "BIGINT");
        db2Mapping.put("REAL", "FLOAT");
        db2Mapping.put("DOUBLE", "DOUBLE");
        db2Mapping.put("DECIMAL", "DECIMAL");
        db2Mapping.put("NUMERIC", "DECIMAL");
        db2Mapping.put("DECFLOAT", "DECIMAL");
        db2Mapping.put("DATE", "DATE");
        db2Mapping.put("TIME", "TIME");
        db2Mapping.put("TIMESTAMP", "TIMESTAMP");
        db2Mapping.put("BLOB", "LONGBLOB");
        db2Mapping.put("XML", "LONGTEXT");
        COMMON_TYPE_MAPPING.put("db2", db2Mapping);

        // Sybase 类型映射
        Map<String, String> sybaseMapping = new HashMap<>();
        sybaseMapping.put("VARCHAR", "VARCHAR");
        sybaseMapping.put("CHAR", "CHAR");
        sybaseMapping.put("TEXT", "TEXT");
        sybaseMapping.put("UNITEXT", "TEXT");
        sybaseMapping.put("TINYINT", "TINYINT");
        sybaseMapping.put("SMALLINT", "SMALLINT");
        sybaseMapping.put("INT", "INT");
        sybaseMapping.put("BIGINT", "BIGINT");
        sybaseMapping.put("DECIMAL", "DECIMAL");
        sybaseMapping.put("NUMERIC", "DECIMAL");
        sybaseMapping.put("FLOAT", "FLOAT");
        sybaseMapping.put("REAL", "FLOAT");
        sybaseMapping.put("MONEY", "DECIMAL");
        sybaseMapping.put("SMALLMONEY", "DECIMAL");
        sybaseMapping.put("DATETIME", "DATETIME");
        sybaseMapping.put("SMALLDATETIME", "DATETIME");
        sybaseMapping.put("DATE", "DATE");
        sybaseMapping.put("TIME", "TIME");
        sybaseMapping.put("BINARY", "BINARY");
        sybaseMapping.put("VARBINARY", "VARBINARY");
        sybaseMapping.put("IMAGE", "LONGBLOB");
        COMMON_TYPE_MAPPING.put("sybase", sybaseMapping);

        // H2 类型映射
        Map<String, String> h2Mapping = new HashMap<>();
        h2Mapping.put("VARCHAR", "VARCHAR");
        h2Mapping.put("CHAR", "CHAR");
        h2Mapping.put("CLOB", "LONGTEXT");
        h2Mapping.put("TINYINT", "TINYINT");
        h2Mapping.put("SMALLINT", "SMALLINT");
        h2Mapping.put("INT", "INT");
        h2Mapping.put("BIGINT", "BIGINT");
        h2Mapping.put("DECIMAL", "DECIMAL");
        h2Mapping.put("REAL", "FLOAT");
        h2Mapping.put("DOUBLE", "DOUBLE");
        h2Mapping.put("BOOLEAN", "TINYINT");
        h2Mapping.put("DATE", "DATE");
        h2Mapping.put("TIME", "TIME");
        h2Mapping.put("TIMESTAMP", "TIMESTAMP");
        h2Mapping.put("BLOB", "LONGBLOB");
        h2Mapping.put("UUID", "VARCHAR");
        COMMON_TYPE_MAPPING.put("h2", h2Mapping);

        // GaussDB/OpenGauss 类型映射
        Map<String, String> gaussMapping = new HashMap<>();
        gaussMapping.put("VARCHAR", "VARCHAR");
        gaussMapping.put("CHAR", "CHAR");
        gaussMapping.put("TEXT", "TEXT");
        gaussMapping.put("SMALLINT", "SMALLINT");
        gaussMapping.put("INTEGER", "INT");
        gaussMapping.put("BIGINT", "BIGINT");
        gaussMapping.put("DECIMAL", "DECIMAL");
        gaussMapping.put("NUMERIC", "DECIMAL");
        gaussMapping.put("REAL", "FLOAT");
        gaussMapping.put("DOUBLE PRECISION", "DOUBLE");
        gaussMapping.put("BOOLEAN", "TINYINT");
        gaussMapping.put("DATE", "DATE");
        gaussMapping.put("TIME", "TIME");
        gaussMapping.put("TIMESTAMP", "TIMESTAMP");
        gaussMapping.put("BYTEA", "LONGBLOB");
        gaussMapping.put("JSON", "JSON");
        gaussMapping.put("JSONB", "JSON");
        COMMON_TYPE_MAPPING.put("gaussdb", gaussMapping);
        COMMON_TYPE_MAPPING.put("opengauss", gaussMapping);

        // 神通数据库类型映射
        Map<String, String> shentongMapping = new HashMap<>();
        shentongMapping.put("VARCHAR", "VARCHAR");
        shentongMapping.put("CHAR", "CHAR");
        shentongMapping.put("TEXT", "TEXT");
        shentongMapping.put("INT", "INT");
        shentongMapping.put("INTEGER", "INT");
        shentongMapping.put("BIGINT", "BIGINT");
        shentongMapping.put("DECIMAL", "DECIMAL");
        shentongMapping.put("NUMERIC", "DECIMAL");
        shentongMapping.put("FLOAT", "FLOAT");
        shentongMapping.put("DOUBLE", "DOUBLE");
        shentongMapping.put("DATE", "DATE");
        shentongMapping.put("TIME", "TIME");
        shentongMapping.put("TIMESTAMP", "TIMESTAMP");
        shentongMapping.put("BLOB", "LONGBLOB");
        shentongMapping.put("CLOB", "LONGTEXT");
        COMMON_TYPE_MAPPING.put("shentong", shentongMapping);

        // 瀚高数据库类型映射
        Map<String, String> highgoMapping = new HashMap<>();
        // 与 PostgreSQL 类似
        highgoMapping.putAll(postgresMapping);
        COMMON_TYPE_MAPPING.put("highgo", highgoMapping);

        // 大数据数据库类型映射
        // Hive 类型映射
        Map<String, String> hiveMapping = new HashMap<>();
        hiveMapping.put("STRING", "VARCHAR");
        hiveMapping.put("VARCHAR", "VARCHAR");
        hiveMapping.put("CHAR", "CHAR");
        hiveMapping.put("TINYINT", "TINYINT");
        hiveMapping.put("SMALLINT", "SMALLINT");
        hiveMapping.put("INT", "INT");
        hiveMapping.put("BIGINT", "BIGINT");
        hiveMapping.put("FLOAT", "FLOAT");
        hiveMapping.put("DOUBLE", "DOUBLE");
        hiveMapping.put("DECIMAL", "DECIMAL");
        hiveMapping.put("BOOLEAN", "TINYINT");
        hiveMapping.put("DATE", "DATE");
        hiveMapping.put("TIMESTAMP", "TIMESTAMP");
        hiveMapping.put("BINARY", "LONGBLOB");
        hiveMapping.put("ARRAY", "JSON");
        hiveMapping.put("MAP", "JSON");
        hiveMapping.put("STRUCT", "JSON");
        COMMON_TYPE_MAPPING.put("hive", hiveMapping);

        // ClickHouse 类型映射
        Map<String, String> clickhouseMapping = new HashMap<>();
        clickhouseMapping.put("String", "VARCHAR");
        clickhouseMapping.put("FixedString", "CHAR");
        clickhouseMapping.put("UInt8", "TINYINT");
        clickhouseMapping.put("UInt16", "SMALLINT");
        clickhouseMapping.put("UInt32", "INT");
        clickhouseMapping.put("UInt64", "BIGINT");
        clickhouseMapping.put("Int8", "TINYINT");
        clickhouseMapping.put("Int16", "SMALLINT");
        clickhouseMapping.put("Int32", "INT");
        clickhouseMapping.put("Int64", "BIGINT");
        clickhouseMapping.put("Float32", "FLOAT");
        clickhouseMapping.put("Float64", "DOUBLE");
        clickhouseMapping.put("Decimal", "DECIMAL");
        clickhouseMapping.put("Date", "DATE");
        clickhouseMapping.put("DateTime", "DATETIME");
        clickhouseMapping.put("Enum8", "VARCHAR");
        clickhouseMapping.put("Enum16", "VARCHAR");
        clickhouseMapping.put("Array", "JSON");
        clickhouseMapping.put("JSON", "JSON");
        COMMON_TYPE_MAPPING.put("clickhouse", clickhouseMapping);

        // Doris/StarRocks 类型映射
        Map<String, String> dorisMapping = new HashMap<>();
        dorisMapping.put("VARCHAR", "VARCHAR");
        dorisMapping.put("CHAR", "CHAR");
        dorisMapping.put("TEXT", "TEXT");
        dorisMapping.put("TINYINT", "TINYINT");
        dorisMapping.put("SMALLINT", "SMALLINT");
        dorisMapping.put("INT", "INT");
        dorisMapping.put("BIGINT", "BIGINT");
        dorisMapping.put("LARGEINT", "BIGINT");
        dorisMapping.put("FLOAT", "FLOAT");
        dorisMapping.put("DOUBLE", "DOUBLE");
        dorisMapping.put("DECIMAL", "DECIMAL");
        dorisMapping.put("DATE", "DATE");
        dorisMapping.put("DATETIME", "DATETIME");
        dorisMapping.put("JSON", "JSON");
        COMMON_TYPE_MAPPING.put("doris", dorisMapping);
        COMMON_TYPE_MAPPING.put("starrocks", dorisMapping);

        // OceanBase 类型映射
        Map<String, String> oceanbaseMapping = new HashMap<>();
        // 兼容 MySQL 模式
        oceanbaseMapping.putAll(TYPE_MAPPING);
        // 兼容 Oracle 模式
        oceanbaseMapping.putAll(oracleMapping);
        COMMON_TYPE_MAPPING.put("oceanbase", oceanbaseMapping);

        // TiDB 类型映射 (与 MySQL 兼容)
        COMMON_TYPE_MAPPING.put("tidb", new HashMap<>(TYPE_MAPPING));

        // 南大通用 GBase
        Map<String, String> gbaseMapping = new HashMap<>();
        gbaseMapping.putAll(postgresMapping); // GBase 兼容 PostgreSQL
        COMMON_TYPE_MAPPING.put("gbase", gbaseMapping);

        // 优炫数据库
        Map<String, String> uxdbMapping = new HashMap<>();
        uxdbMapping.putAll(postgresMapping); // UXDB 基于 PostgreSQL
        COMMON_TYPE_MAPPING.put("uxdb", uxdbMapping);

        // 华宇数据库
        Map<String, String> hydbMapping = new HashMap<>();
        hydbMapping.putAll(oracleMapping); // 华宇数据库兼容 Oracle
        COMMON_TYPE_MAPPING.put("hydb", hydbMapping);

        // 虚谷数据库
        Map<String, String> xuguMapping = new HashMap<>();
        xuguMapping.putAll(oracleMapping); // 虚谷数据库兼容 Oracle
        COMMON_TYPE_MAPPING.put("xugu", xuguMapping);

        // 东软数据库
        Map<String, String> neudbMapping = new HashMap<>();
        neudbMapping.putAll(oracleMapping); // 东软数据库兼容 Oracle
        COMMON_TYPE_MAPPING.put("neudb", neudbMapping);

        // 巨杉数据库
        Map<String, String> sequoiaMapping = new HashMap<>();
        sequoiaMapping.putAll(postgresMapping); // 巨杉数据库兼容 PostgreSQL
        COMMON_TYPE_MAPPING.put("sequoiadb", sequoiaMapping);

        // 天兵数据库
        Map<String, String> tbaseMapping = new HashMap<>();
        tbaseMapping.putAll(postgresMapping); // 天兵数据库基于 PostgreSQL
        COMMON_TYPE_MAPPING.put("tbase", tbaseMapping);

        // HBase 类型映射
        Map<String, String> hbaseMapping = new HashMap<>();
        hbaseMapping.put("CHAR", "VARCHAR");
        hbaseMapping.put("VARCHAR", "VARCHAR");
        hbaseMapping.put("INTEGER", "INT");
        hbaseMapping.put("BIGINT", "BIGINT");
        hbaseMapping.put("FLOAT", "FLOAT");
        hbaseMapping.put("DOUBLE", "DOUBLE");
        hbaseMapping.put("BOOLEAN", "TINYINT");
        hbaseMapping.put("BINARY", "LONGBLOB");
        COMMON_TYPE_MAPPING.put("hbase", hbaseMapping);

        // Spark SQL 类型映射
        Map<String, String> sparkMapping = new HashMap<>();
        sparkMapping.putAll(hiveMapping); // Spark SQL 兼容 Hive
        sparkMapping.put("STRING", "VARCHAR");
        sparkMapping.put("LONG", "BIGINT");
        sparkMapping.put("BYTE", "TINYINT");
        sparkMapping.put("SHORT", "SMALLINT");
        sparkMapping.put("FLOAT", "FLOAT");
        sparkMapping.put("DOUBLE", "DOUBLE");
        sparkMapping.put("BOOLEAN", "TINYINT");
        sparkMapping.put("TIMESTAMP", "TIMESTAMP");
        sparkMapping.put("BINARY", "LONGBLOB");
        sparkMapping.put("ARRAY", "JSON");
        sparkMapping.put("MAP", "JSON");
        sparkMapping.put("STRUCT", "JSON");
        COMMON_TYPE_MAPPING.put("spark", sparkMapping);

        // Presto/Trino 类型映射
        Map<String, String> prestoMapping = new HashMap<>();
        prestoMapping.put("VARCHAR", "VARCHAR");
        prestoMapping.put("CHAR", "CHAR");
        prestoMapping.put("VARBINARY", "VARBINARY");
        prestoMapping.put("BOOLEAN", "TINYINT");
        prestoMapping.put("TINYINT", "TINYINT");
        prestoMapping.put("SMALLINT", "SMALLINT");
        prestoMapping.put("INTEGER", "INT");
        prestoMapping.put("BIGINT", "BIGINT");
        prestoMapping.put("REAL", "FLOAT");
        prestoMapping.put("DOUBLE", "DOUBLE");
        prestoMapping.put("DECIMAL", "DECIMAL");
        prestoMapping.put("DATE", "DATE");
        prestoMapping.put("TIME", "TIME");
        prestoMapping.put("TIMESTAMP", "TIMESTAMP");
        prestoMapping.put("JSON", "JSON");
        prestoMapping.put("ARRAY", "JSON");
        prestoMapping.put("MAP", "JSON");
        prestoMapping.put("ROW", "JSON");
        COMMON_TYPE_MAPPING.put("presto", prestoMapping);
        COMMON_TYPE_MAPPING.put("trino", prestoMapping);

        // Impala 类型映射
        Map<String, String> impalaMapping = new HashMap<>();
        impalaMapping.putAll(hiveMapping); // Impala 基于 Hive
        impalaMapping.put("STRING", "VARCHAR");
        impalaMapping.put("INT", "INT");
        impalaMapping.put("BIGINT", "BIGINT");
        impalaMapping.put("SMALLINT", "SMALLINT");
        impalaMapping.put("TINYINT", "TINYINT");
        impalaMapping.put("DECIMAL", "DECIMAL");
        impalaMapping.put("FLOAT", "FLOAT");
        impalaMapping.put("DOUBLE", "DOUBLE");
        impalaMapping.put("BOOLEAN", "TINYINT");
        impalaMapping.put("TIMESTAMP", "TIMESTAMP");
        impalaMapping.put("CHAR", "CHAR");
        impalaMapping.put("VARCHAR", "VARCHAR");
        COMMON_TYPE_MAPPING.put("impala", impalaMapping);

        // Kylin 类型映射
        Map<String, String> kylinMapping = new HashMap<>();
        kylinMapping.putAll(hiveMapping); // Kylin 使用 Hive 的类型系统
        kylinMapping.put("VARCHAR", "VARCHAR");
        kylinMapping.put("CHAR", "CHAR");
        kylinMapping.put("STRING", "VARCHAR");
        kylinMapping.put("DECIMAL", "DECIMAL");
        kylinMapping.put("BOOLEAN", "TINYINT");
        kylinMapping.put("INTEGER", "INT");
        kylinMapping.put("INT", "INT");
        kylinMapping.put("TINYINT", "TINYINT");
        kylinMapping.put("SMALLINT", "SMALLINT");
        kylinMapping.put("BIGINT", "BIGINT");
        kylinMapping.put("FLOAT", "FLOAT");
        kylinMapping.put("DOUBLE", "DOUBLE");
        kylinMapping.put("DATE", "DATE");
        kylinMapping.put("TIMESTAMP", "TIMESTAMP");
        COMMON_TYPE_MAPPING.put("kylin", kylinMapping);

        // MySQL 自身的映射
        COMMON_TYPE_MAPPING.put("mysql", TYPE_MAPPING);
    }

    public MySQLHandler(DataSource dataSource) {
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
        // MySQL不需要设置schema，因为在URL中已经指定了数据库
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
        return "SELECT 1";
    }

    @Override
    public String getQuoteString() {
        return "`";
    }

    @Override
    public List<String> getSystemDatabases() {
        return Arrays.asList("mysql", "information_schema", "performance_schema", "sys");
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
        return "'" + value.toString().replace("'", "''") + "'";
    }

    @Override
    public String wrapIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    @Override
    public String generatePageSql(String sql, int offset, int limit) {
        return sql + " LIMIT " + offset + ", " + limit;
    }

    /**
     * 生成MySQL的LIMIT子句
     * 
     * @param offset 起始位置
     * @param limit  返回记录的最大数量
     * @return 格式化后的LIMIT子句
     */
    @Override
    public String getLimitClause(long offset, int limit) {
        return "LIMIT " + offset + ", " + limit;
    }

    @Override
    public String generateCountSql(String sql) {
        return "SELECT COUNT(*) FROM (" + sql + ") t";
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
        return "MySQL";
    }

    @Override
    public String getTableExistsSql(String tableName) {
        return String.format(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '%s' AND table_name = '%s'",
                getSchema(),
                tableName
        );
    }

    @Override
    public String getCreateTableSql(String tableName, List<ColumnDefinition> columns,
                                    String tableComment, String engine, String charset, String collate) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS ").append(wrapIdentifier(tableName)).append(" (\n");

        List<String> columnDefinitions = new ArrayList<>();
        List<String> primaryKeys = new ArrayList<>();

        for (ColumnDefinition column : columns) {
            // 先调整列类型以符合MySQL要求
            adjustMySQLColumnType(column);

            String columnDef = formatFieldDefinition(
                    column.getName(), column.getType(),
                    column.getLength(), column.getPrecision(), column.getScale(),
                    column.isNullable(), column.getDefaultValue(), column.getComment()
            );

            columnDefinitions.add(columnDef);

            if (column.isPrimaryKey()) {
                primaryKeys.add(column.getName());
            }
        }

        // 添加主键定义
        if (!primaryKeys.isEmpty()) {
            StringBuilder pkDef = new StringBuilder();
            pkDef.append("PRIMARY KEY (");
            for (int i = 0; i < primaryKeys.size(); i++) {
                if (i > 0) {
                    pkDef.append(", ");
                }
                pkDef.append(wrapIdentifier(primaryKeys.get(i)));
            }
            pkDef.append(")");
            columnDefinitions.add(pkDef.toString());
        }

        sql.append(String.join(",\n", columnDefinitions));
        sql.append("\n)");

        // 添加引擎
        if (engine != null && !engine.isEmpty()) {
            sql.append(" ENGINE=").append(engine);
        } else {
            sql.append(" ENGINE=InnoDB"); // MySQL默认推荐InnoDB引擎
        }

        // 添加字符集
        if (charset != null && !charset.isEmpty()) {
            sql.append(" CHARACTER SET=").append(charset);
        } else {
            sql.append(" CHARACTER SET=utf8mb4"); // 推荐使用utf8mb4字符集
        }

        // 添加排序规则
        if (collate != null && !collate.isEmpty()) {
            sql.append(" COLLATE=").append(collate);
        } else if (charset != null && charset.equalsIgnoreCase("utf8mb4")) {
            sql.append(" COLLATE=utf8mb4_unicode_ci"); // utf8mb4字符集对应的常用排序规则
        }

        // 添加表注释
        if (tableComment != null && !tableComment.isEmpty()) {
            sql.append(" COMMENT=").append(wrapValue(tableComment));
        }

        return sql.toString();
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
    public String getAddColumnSql(String tableName, String columnDefinition) {
        return "ALTER TABLE " + wrapIdentifier(tableName) + " ADD COLUMN " + columnDefinition;
    }

    @Override
    public String getModifyColumnSql(String tableName, String columnName, String newDefinition) {
        return String.format("ALTER TABLE %s MODIFY COLUMN %s",
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
        return "RENAME TABLE " + wrapIdentifier(oldTableName) + " TO " + wrapIdentifier(newTableName);
    }

    @Override
    public String getShowCreateTableSql(String tableName) {
        return "SHOW CREATE TABLE " + wrapIdentifier(tableName);
    }

    @Override
    public String getShowTablesSql() {
        return "SHOW TABLES";
    }

    @Override
    public String getShowColumnsSql(String tableName) {
        return "SHOW FULL COLUMNS FROM " + wrapIdentifier(tableName);
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
        return "INT"; // 移除 (11)，符合 MySQL 8.0+ 推荐写法
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
        return "DATETIME";
    }

    @Override
    public String getDefaultBooleanType() {
        return "TINYINT(1)";
    }

    @Override
    public String getDefaultBlobType() {
        return "LONGBLOB";
    }

    @Override
    public String mapJavaTypeToDbType(Class<?> javaType) {
        if (String.class.equals(javaType)) {
            return "VARCHAR";
        } else if (Character.class.equals(javaType) || char.class.equals(javaType)) {
            return "CHAR";
        } else if (Byte.class.equals(javaType) || byte.class.equals(javaType)) {
            return "TINYINT";
        } else if (Short.class.equals(javaType) || short.class.equals(javaType)) {
            return "SMALLINT";
        } else if (Integer.class.equals(javaType) || int.class.equals(javaType)) {
            return "INT";
        } else if (Long.class.equals(javaType) || long.class.equals(javaType)) {
            return "BIGINT";
        } else if (Float.class.equals(javaType) || float.class.equals(javaType)) {
            return "FLOAT";
        } else if (Double.class.equals(javaType) || double.class.equals(javaType)) {
            return "DOUBLE";
        } else if (BigDecimal.class.equals(javaType)) {
            return "DECIMAL";
        } else if (Boolean.class.equals(javaType) || boolean.class.equals(javaType)) {
            return "TINYINT";
        } else if (Date.class.equals(javaType)) {
            return "DATETIME";
        } else if (java.sql.Date.class.equals(javaType)) {
            return "DATE";
        } else if (java.sql.Time.class.equals(javaType)) {
            return "TIME";
        } else if (java.sql.Timestamp.class.equals(javaType)) {
            return "TIMESTAMP";
        } else if (byte[].class.equals(javaType)) {
            return "LONGBLOB";
        } else if (Enum.class.isAssignableFrom(javaType)) {
            return "ENUM";
        }
        return "VARCHAR";
    }

    @Override
    public Class<?> mapDbTypeToJavaType(String dbType) {
        dbType = dbType.toUpperCase();
        if (dbType.contains("CHAR") || dbType.contains("TEXT")) {
            return String.class;
        } else if (dbType.equals("TINYINT")) {
            return Byte.class;
        } else if (dbType.equals("SMALLINT")) {
            return Short.class;
        } else if (dbType.contains("INT")) {
            return Integer.class;
        } else if (dbType.equals("BIGINT")) {
            return Long.class;
        } else if (dbType.equals("FLOAT")) {
            return Float.class;
        } else if (dbType.equals("DOUBLE")) {
            return Double.class;
        } else if (dbType.contains("DECIMAL") || dbType.contains("NUMERIC")) {
            return BigDecimal.class;
        } else if (dbType.equals("BOOL") || dbType.equals("TINYINT(1)")) {
            return Boolean.class;
        } else if (dbType.equals("DATE")) {
            return java.sql.Date.class;
        } else if (dbType.equals("TIME")) {
            return java.sql.Time.class;
        } else if (dbType.equals("TIMESTAMP")) {
            return java.sql.Timestamp.class;
        } else if (dbType.equals("DATETIME")) {
            return Date.class;
        } else if (dbType.contains("BLOB") || dbType.contains("BINARY")) {
            return byte[].class;
        } else if (dbType.equals("JSON")) {
            return String.class;
        } else if (dbType.equals("ENUM") || dbType.equals("SET")) {
            return String.class;
        }
        return String.class;
    }

    @Override
    public Map<String, String> getTypeMapping() {
        return TYPE_MAPPING;
    }

    @Override
    public Map<String, Integer> getDefaultLengthMapping() {
        return DEFAULT_LENGTH_MAPPING;
    }

    @Override
    public boolean isValidFieldLength(String type, int length) {
        if (type.equalsIgnoreCase("VARCHAR") || type.equalsIgnoreCase("NVARCHAR")) {
            // 使用 utf8mb4 字符集时单个字符最多占用4字节
            return length > 0 && length <= MAX_VARCHAR_LENGTH;
        } else if (type.equalsIgnoreCase("CHAR") || type.equalsIgnoreCase("NCHAR")) {
            return length > 0 && length <= 255; // MySQL CHAR 长度限制为 255
        }
        return true;
    }

    @Override
    public String formatFieldDefinition(String name, String type, Integer length, Integer precision, Integer scale,
                                        boolean nullable, String defaultValue, String comment) {
        StringBuilder definition = new StringBuilder();
        definition.append(wrapIdentifier(name)).append(" ");

        // 处理类型和长度
        String upperType = type.toUpperCase();
        definition.append(upperType);

        // 根据类型添加适当的长度/精度/小数位
        if (upperType.equals("DECIMAL") || upperType.equals("NUMERIC")) {
            int p = precision != null ? precision : 10;
            int s = scale != null ? scale : 0;
            definition.append("(").append(p).append(",").append(s).append(")");
        } else if (upperType.equals("VARCHAR") || upperType.equals("NVARCHAR") ||
                upperType.equals("CHAR") || upperType.equals("NCHAR") ||
                upperType.matches("ENUM|SET")) {
            if (length != null) {
                definition.append("(").append(length).append(")");
            } else if (upperType.equals("VARCHAR") || upperType.equals("NVARCHAR")) {
                definition.append("(").append(DEFAULT_VARCHAR_LENGTH).append(")");
            } else if (upperType.equals("CHAR") || upperType.equals("NCHAR")) {
                definition.append("(1)"); // CHAR默认长度为1
            }
            // ENUM和SET的长度是其值列表，应该已包含在类型中
        }
        // MySQL 8.0+不建议为整数类型指定长度，所以不添加

        // 处理可空性
        if (!nullable) {
            definition.append(" NOT NULL");
        } else {
            definition.append(" NULL");
        }

        // 处理默认值
        if (defaultValue != null) {
            if (defaultValue.equalsIgnoreCase("NULL")) {
                definition.append(" DEFAULT NULL");
            } else if (defaultValue.equalsIgnoreCase("CURRENT_TIMESTAMP")) {
                definition.append(" DEFAULT CURRENT_TIMESTAMP");

                // 对于 TIMESTAMP 和 DATETIME 类型，检查是否需要添加 ON UPDATE CURRENT_TIMESTAMP
                if (defaultValue.contains("ON UPDATE") &&
                        (upperType.equals("TIMESTAMP") || upperType.equals("DATETIME"))) {
                    definition.append(" ON UPDATE CURRENT_TIMESTAMP");
                }
            } else if (upperType.matches("CHAR|VARCHAR|TEXT|NCHAR|NVARCHAR|ENUM|SET|JSON")) {
                // 字符串类型需要加引号
                definition.append(" DEFAULT ").append(wrapValue(defaultValue));
            } else {
                // 数值类型不需要加引号
                definition.append(" DEFAULT ").append(defaultValue);
            }
        }

        // 处理自增
        if (upperType.matches("INT|INTEGER|BIGINT|SMALLINT|TINYINT|MEDIUMINT")) {
            if ("AUTO_INCREMENT".equalsIgnoreCase(defaultValue) ||
                    (defaultValue != null && defaultValue.contains("AUTO_INCREMENT"))) {
                definition.append(" AUTO_INCREMENT");
                // 自增列必须是键
                if (!name.equals("id") && !comment.contains("主键")) {
                    definition.append(" PRIMARY KEY");
                }
            }
        }

        // 处理注释
        if (comment != null && !comment.isEmpty()) {
            definition.append(" COMMENT ").append(wrapValue(comment));
        }

        return definition.toString();
    }

    @Override
    public String convertFromOtherDbType(String sourceType, String sourceDbType) {
        // 处理Oracle特有类型
        if ("oracle".equalsIgnoreCase(sourceDbType)) {
            if (sourceType.startsWith("VARCHAR2") || sourceType.startsWith("NVARCHAR2")) {
                int length = extractLength(sourceType, 255);
                if (length > MAX_VARCHAR_LENGTH) {
                    return "TEXT";
                } else {
                    return "VARCHAR(" + length + ")";
                }
            } else if (sourceType.equals("NUMBER")) {
                return "DECIMAL(38,10)";
            } else if (sourceType.matches("NUMBER\\(\\d+\\)")) {
                int precision = extractPrecision(sourceType, 10);
                return "DECIMAL(" + precision + ",0)";
            } else if (sourceType.matches("NUMBER\\(\\d+,\\d+\\)")) {
                int precision = extractPrecision(sourceType, 10);
                int scale = extractScale(sourceType, 0);
                return "DECIMAL(" + precision + "," + scale + ")";
            } else if (sourceType.equals("CLOB")) {
                return "LONGTEXT";
            } else if (sourceType.equals("BLOB")) {
                return "LONGBLOB";
            } else if (sourceType.equals("DATE")) {
                return "DATETIME";
            } else if (sourceType.startsWith("TIMESTAMP")) {
                return "TIMESTAMP";
            } else if (sourceType.equals("RAW") || sourceType.matches("RAW\\(\\d+\\)")) {
                int length = extractLength(sourceType, 255);
                return "VARBINARY(" + Math.min(length, 65535) + ")";
            } else if (sourceType.equals("LONG RAW")) {
                return "LONGBLOB";
            } else if (sourceType.equals("LONG")) {
                return "LONGTEXT";
            } else if (sourceType.equals("ROWID")) {
                return "VARCHAR(18)";
            }
        }

        // 处理SQL Server特有类型
        else if ("sqlserver".equalsIgnoreCase(sourceDbType)) {
            if (sourceType.equals("NVARCHAR")) {
                return "VARCHAR(255)";
            } else if (sourceType.matches("NVARCHAR\\(\\d+\\)")) {
                int length = extractLength(sourceType, 255);
                if (length > MAX_VARCHAR_LENGTH) {
                    return "TEXT";
                } else {
                    return "VARCHAR(" + length + ")";
                }
            } else if (sourceType.equals("NTEXT")) {
                return "LONGTEXT";
            } else if (sourceType.equals("IMAGE")) {
                return "LONGBLOB";
            } else if (sourceType.equals("DATETIME2")) {
                return "DATETIME";
            } else if (sourceType.equals("DATETIMEOFFSET")) {
                return "DATETIME";
            } else if (sourceType.equals("SMALLDATETIME")) {
                return "DATETIME";
            } else if (sourceType.equals("MONEY")) {
                return "DECIMAL(19,4)";
            } else if (sourceType.equals("SMALLMONEY")) {
                return "DECIMAL(10,4)";
            } else if (sourceType.equals("UNIQUEIDENTIFIER")) {
                return "CHAR(36)";
            } else if (sourceType.equals("HIERARCHYID")) {
                return "VARCHAR(255)";
            } else if (sourceType.equals("GEOGRAPHY") || sourceType.equals("GEOMETRY")) {
                return "GEOMETRY";
            } else if (sourceType.equals("XML")) {
                return "LONGTEXT";
            }
        }

        // 处理PostgreSQL特有类型
        else if ("postgresql".equalsIgnoreCase(sourceDbType)) {
            if (sourceType.equals("TEXT")) {
                return "LONGTEXT";
            } else if (sourceType.equals("BYTEA")) {
                return "LONGBLOB";
            } else if (sourceType.equals("UUID")) {
                return "CHAR(36)";
            } else if (sourceType.equals("JSON") || sourceType.equals("JSONB")) {
                return "JSON";
            } else if (sourceType.equals("TIMESTAMP WITH TIME ZONE") ||
                    sourceType.equals("TIMESTAMP WITHOUT TIME ZONE")) {
                return "TIMESTAMP";
            } else if (sourceType.equals("TIME WITH TIME ZONE") ||
                    sourceType.equals("TIME WITHOUT TIME ZONE")) {
                return "TIME";
            } else if (sourceType.equals("INTERVAL")) {
                return "VARCHAR(255)";
            } else if (sourceType.equals("CIDR") || sourceType.equals("INET") ||
                    sourceType.equals("MACADDR")) {
                return "VARCHAR(43)";
            } else if (sourceType.startsWith("CHARACTER VARYING")) {
                int length = extractLength(sourceType, 255);
                if (length > MAX_VARCHAR_LENGTH) {
                    return "TEXT";
                } else {
                    return "VARCHAR(" + length + ")";
                }
            } else if (sourceType.equals("BOOLEAN")) {
                return "TINYINT(1)";
            }
        }

        // 如果找不到匹配的类型转换，则保持原类型
        return sourceType;
    }

    private int extractLength(String type, int defaultValue) {
        try {
            int startPos = type.indexOf('(');
            int endPos = type.indexOf(')', startPos);
            if (startPos > 0 && endPos > startPos) {
                String lengthPart = type.substring(startPos + 1, endPos);
                if (lengthPart.contains(",")) {
                    lengthPart = lengthPart.substring(0, lengthPart.indexOf(','));
                }
                return Integer.parseInt(lengthPart.trim());
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
        return defaultValue;
    }

    private int extractPrecision(String type, int defaultValue) {
        try {
            int startPos = type.indexOf('(');
            int endPos = type.indexOf(')', startPos);
            if (startPos > 0 && endPos > startPos) {
                String precisionPart = type.substring(startPos + 1, endPos);
                if (precisionPart.contains(",")) {
                    precisionPart = precisionPart.substring(0, precisionPart.indexOf(','));
                }
                return Integer.parseInt(precisionPart.trim());
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
        return defaultValue;
    }

    private int extractScale(String type, int defaultValue) {
        try {
            int commaPos = type.indexOf(',');
            int endPos = type.indexOf(')', commaPos);
            if (commaPos > 0 && endPos > commaPos) {
                String scalePart = type.substring(commaPos + 1, endPos);
                return Integer.parseInt(scalePart.trim());
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
        return defaultValue;
    }

    @Override
    public Map<String, Map<String, String>> getCommonTypeMapping() {
        return COMMON_TYPE_MAPPING;
    }

    /**
     * 检查标识符是否合法
     */
    public boolean isValidIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return false;
        }
        // 检查首字符
        char firstChar = identifier.charAt(0);
        if (!Character.isLetter(firstChar) && firstChar != '_' && firstChar != '$') {
            return false;
        }
        // 检查其他字符
        for (int i = 1; i < identifier.length(); i++) {
            char c = identifier.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '$') {
                return false;
            }
        }
        return true;
    }


    /**
     * 修改表注释
     */
    public String getAlterTableCommentSql(String tableName, String comment) {
        return String.format("ALTER TABLE %s COMMENT = %s",
                wrapIdentifier(tableName),
                wrapValue(comment)
        );
    }

    /**
     * 增加字段
     */
    public String getAddColumnSql(String tableName, String columnName, String columnType,
                                  String comment, boolean nullable, String defaultValue, String afterColumn) {

        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(wrapIdentifier(tableName));
        sql.append(" ADD COLUMN ").append(wrapIdentifier(columnName));
        sql.append(" ").append(columnType);

        if (!nullable) {
            sql.append(" NOT NULL");
        }

        if (defaultValue != null) {
            sql.append(" DEFAULT ").append(wrapValue(defaultValue));
        }

        if (comment != null && !comment.isEmpty()) {
            sql.append(" COMMENT ").append(wrapValue(comment));
        }

        if (afterColumn != null && !afterColumn.isEmpty()) {
            sql.append(" AFTER ").append(wrapIdentifier(afterColumn));
        }

        return sql.toString();
    }

    /**
     * 修改字段名
     */
    public String getRenameColumnSql(String tableName, String oldColumnName, String newColumnName, String columnType) {
        return String.format("ALTER TABLE %s CHANGE COLUMN %s %s %s",
                wrapIdentifier(tableName),
                wrapIdentifier(oldColumnName),
                wrapIdentifier(newColumnName),
                columnType
        );
    }


    /**
     * 删除表
     */
    public String getDropTableSql(String tableName, boolean ifExists) {
        return String.format("DROP TABLE %s%s",
                ifExists ? "IF EXISTS " : "",
                wrapIdentifier(tableName)
        );
    }

    /**
     * 批量修改操作
     */
    public String getBatchAlterTableSql(String tableName, List<String> alterOperations) {
        return String.format("ALTER TABLE %s\n%s",
                wrapIdentifier(tableName),
                String.join(",\n", alterOperations)
        );
    }

    /**
     * 检查表是否存在
     */
    public String getCheckTableExistsSql(String tableName) {
        return String.format(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '%s' AND table_name = '%s'",
                getSchema(),
                tableName
        );
    }

    /**
     * 检查字段是否存在
     */
    public String getCheckColumnExistsSql(String tableName, String columnName) {
        return String.format(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = '%s' AND table_name = '%s' AND column_name = '%s'",
                getSchema(),
                tableName,
                columnName
        );
    }

    @Override
    public String getAddTableCommentSql(String tableName, String comment) {
        // MySQL修改表注释使用ALTER TABLE语句
        return "ALTER TABLE " + wrapIdentifier(tableName) +
                " COMMENT = " + wrapValue(comment);
    }

    @Override
    public String getModifyTableCommentSql(String tableName, String comment) {
        // MySQL中添加和修改表注释使用相同语法
        return getAddTableCommentSql(tableName, comment);
    }

    @Override
    public String getAddColumnSql(String tableName, ColumnDefinition column, String afterColumn) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(wrapIdentifier(tableName));
        sql.append(" ADD COLUMN ");

        // 调整列类型以符合MySQL要求
        adjustMySQLColumnType(column);

        String columnDef = formatFieldDefinition(
                column.getName(), column.getType(),
                column.getLength(), column.getPrecision(), column.getScale(),
                column.isNullable(), column.getDefaultValue(), column.getComment()
        );

        sql.append(columnDef);

        // 在特定列之后添加
        if (afterColumn != null && !afterColumn.isEmpty()) {
            sql.append(" AFTER ").append(wrapIdentifier(afterColumn));
        }

        return sql.toString();
    }


    @Override
    public String getAddColumnCommentSql(String tableName, String columnName, String comment) {
        // 需要获取完整的列定义
        try {
            String columnDefinition = getColumnDefinition(tableName, columnName);
            return "ALTER TABLE " + wrapIdentifier(tableName) +
                    " MODIFY COLUMN " + columnDefinition + " COMMENT " + wrapValue(comment);
        } catch (Exception e) {
            // 如果无法获取完整定义，返回一个警告信息
            return "-- 无法自动生成列注释SQL，需要完整的列定义";
        }
    }

    @Override
    public String getModifyColumnCommentSql(String tableName, String columnName, String comment) {
        // MySQL中添加和修改列注释使用相同语法
        return getAddColumnCommentSql(tableName, columnName, comment);
    }

    @Override
    public String getAlterTableCharsetSql(String tableName, String charset, String collate) {
        return String.format("ALTER TABLE %s DEFAULT CHARACTER SET %s COLLATE %s",
                wrapIdentifier(tableName),
                charset,  // 移除 wrapValue
                collate   // 移除 wrapValue
        );
    }

    @Override
    public String getAlterTableEngineSql(String tableName, String engine) {
        return String.format("ALTER TABLE %s ENGINE = %s",
                wrapIdentifier(tableName),
                engine  // 移除 wrapValue，MySQL 存储引擎名称不需要引号
        );
    }

    @Override
    public String getShowIndexesSql(String tableName) {
        return String.format("SHOW INDEXES FROM %s",
                wrapIdentifier(tableName)
        );
    }

    @Override
    public String getAddIndexSql(String tableName, String indexName, List<String> columns, boolean unique) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(wrapIdentifier(tableName));
        sql.append(" ADD ");

        if (unique) {
            sql.append("UNIQUE ");
        }

        sql.append("INDEX ").append(wrapIdentifier(indexName)).append(" (");

        List<String> wrappedColumns = new ArrayList<>();
        for (String column : columns) {
            // 处理带有长度的列定义，如 column(10)
            if (column.contains("(")) {
                int pos = column.indexOf("(");
                String name = column.substring(0, pos);
                String lengthPart = column.substring(pos);
                wrappedColumns.add(wrapIdentifier(name) + lengthPart);
            } else {
                wrappedColumns.add(wrapIdentifier(column));
            }
        }

        sql.append(String.join(", ", wrappedColumns));
        sql.append(")");

        return sql.toString();
    }

    @Override
    public String getDropIndexSql(String tableName, String indexName) {
        return String.format("DROP INDEX %s ON %s",
                wrapIdentifier(indexName),
                wrapIdentifier(tableName)
        );
    }

    /**
     * 获取创建主键的SQL
     */
    public String getAddPrimaryKeySql(String tableName, List<String> columnNames) {
        List<String> safeColumnNames = columnNames.stream()
                .map(this::wrapIdentifier)
                .collect(Collectors.toList());

        return String.format("ALTER TABLE %s ADD PRIMARY KEY (%s)",
                wrapIdentifier(tableName),
                String.join(",", safeColumnNames)
        );
    }

    /**
     * 获取删除主键的SQL
     */
    public String getDropPrimaryKeySql(String tableName) {
        return String.format("ALTER TABLE %s DROP PRIMARY KEY",
                wrapIdentifier(tableName)
        );
    }

    /**
     * 获取创建外键的SQL
     */
    public String getAddForeignKeySql(String tableName, String constraintName,
                                      List<String> columnNames, String refTableName,
                                      List<String> refColumnNames) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(wrapIdentifier(tableName));
        sql.append(" ADD CONSTRAINT ").append(wrapIdentifier(constraintName));
        sql.append(" FOREIGN KEY (");

        List<String> wrappedColumns = columnNames.stream()
                .map(this::wrapIdentifier)
                .collect(Collectors.toList());
        sql.append(String.join(", ", wrappedColumns));

        sql.append(") REFERENCES ").append(wrapIdentifier(refTableName)).append(" (");

        List<String> wrappedRefColumns = refColumnNames.stream()
                .map(this::wrapIdentifier)
                .collect(Collectors.toList());
        sql.append(String.join(", ", wrappedRefColumns));

        sql.append(")");

        // 对于InnoDB引擎，添加常用的ON DELETE和ON UPDATE行为
        // MySQL默认为RESTRICT
        // sql.append(" ON DELETE CASCADE");
        // sql.append(" ON UPDATE CASCADE");

        return sql.toString();
    }


    /**
     * 获取删除外键的SQL
     */
    public String getDropForeignKeySql(String tableName, String constraintName) {

        return String.format("ALTER TABLE %s DROP FOREIGN KEY %s",
                wrapIdentifier(tableName),
                wrapIdentifier(constraintName)
        );
    }

    /**
     * 获取表的所有字段信息
     */
    public String getTableColumnsSql(String tableName) {
        return String.format(
                "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, NUMERIC_PRECISION, " +
                        "NUMERIC_SCALE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT, EXTRA, COLUMN_KEY " +
                        "FROM information_schema.columns " +
                        "WHERE table_schema = '%s' AND table_name = '%s' " +
                        "ORDER BY ORDINAL_POSITION",
                getSchema(),
                tableName
        );
    }

    /**
     * 获取表的所有约束信息
     */
    public String getTableConstraintsSql(String tableName) {
        return String.format(
                "SELECT CONSTRAINT_NAME, CONSTRAINT_TYPE, COLUMN_NAME " +
                        "FROM information_schema.table_constraints tc " +
                        "JOIN information_schema.key_column_usage kcu " +
                        "ON tc.constraint_name = kcu.constraint_name " +
                        "AND tc.table_schema = kcu.table_schema " +
                        "WHERE tc.table_schema = '%s' AND tc.table_name = '%s'",
                getSchema(),
                tableName
        );
    }

    /**
     * 获取表的所有触发器
     */
    public String getTableTriggersSql(String tableName) {
        return String.format("SHOW TRIGGERS WHERE `Table` = '%s'", tableName);
    }

    /**
     * 复制表结构
     */
    public String getCreateTableLikeSql(String newTableName, String sourceTableName) {
        return String.format("CREATE TABLE %s LIKE %s",
                wrapIdentifier(newTableName),
                wrapIdentifier(sourceTableName)
        );
    }

    /**
     * 重命名多个字段
     */
    public String getBatchRenameColumnsSql(String tableName, Map<String, String> columnMapping) {
        List<String> alterClauses = new ArrayList<>();

        for (Map.Entry<String, String> entry : columnMapping.entrySet()) {
            alterClauses.add(String.format("CHANGE COLUMN %s %s",
                    wrapIdentifier(entry.getKey()),
                    wrapIdentifier(entry.getValue())
            ));
        }

        return String.format("ALTER TABLE %s %s",
                wrapIdentifier(tableName),
                String.join(", ", alterClauses)
        );
    }

    /**
     * 获取表的自增值
     */
    public String getTableAutoIncrementSql(String tableName) {
        return String.format(
                "SELECT AUTO_INCREMENT FROM information_schema.tables " +
                        "WHERE table_schema = '%s' AND table_name = '%s'",
                getSchema(),
                tableName
        );
    }

    /**
     * 修改表的自增值
     */
    public String getAlterTableAutoIncrementSql(String tableName, long value) {
        return String.format("ALTER TABLE %s AUTO_INCREMENT = %d",
                wrapIdentifier(tableName),
                value
        );
    }

    /**
     * 获取创建分区的SQL
     */
    public String getAddPartitionSql(String tableName, String partitionName,
                                     String partitionType, String partitionExpression) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(wrapIdentifier(tableName));
        sql.append(" ADD PARTITION (");

        if ("RANGE".equalsIgnoreCase(partitionType)) {
            sql.append("PARTITION ").append(wrapIdentifier(partitionName));
            // 确保表达式格式正确，例如 LESS THAN (1000)
            if (partitionExpression.toUpperCase().contains("LESS THAN")) {
                sql.append(" VALUES ").append(partitionExpression);
            } else {
                sql.append(" VALUES LESS THAN (").append(partitionExpression).append(")");
            }
        } else if ("LIST".equalsIgnoreCase(partitionType)) {
            sql.append("PARTITION ").append(wrapIdentifier(partitionName));
            // 确保表达式格式正确，例如 IN (1,2,3)
            if (partitionExpression.toUpperCase().contains("IN")) {
                sql.append(" VALUES ").append(partitionExpression);
            } else {
                sql.append(" VALUES IN (").append(partitionExpression).append(")");
            }
        } else if ("HASH".equalsIgnoreCase(partitionType) || "KEY".equalsIgnoreCase(partitionType)) {
            // HASH 和 KEY 分区通常不单独添加分区，而是通过修改分区数量
            throw new UnsupportedOperationException(
                    "MySQL 不支持单独添加 HASH 或 KEY 分区，请使用 ALTER TABLE " +
                            wrapIdentifier(tableName) + " PARTITION BY " + partitionType +
                            " PARTITIONS n 来修改分区数量");
        }

        sql.append(")");

        return sql.toString();
    }

    /**
     * 获取删除分区的SQL
     */
    public String getDropPartitionSql(String tableName, String partitionName) {
        return String.format("ALTER TABLE %s DROP PARTITION %s",
                wrapIdentifier(tableName),
                wrapIdentifier(partitionName)
        );
    }

    /**
     * 获取授权SQL
     */
    public String getGrantSql(String tableName, String userName, String host, List<String> privileges) {
        return String.format("GRANT %s ON %s TO '%s'@'%s'",
                String.join(",", privileges),
                wrapIdentifier(tableName),
                userName,
                host
        );
    }

    /**
     * 获取撤销权限SQL
     */
    public String getRevokeSql(String tableName, String userName, String host, List<String> privileges) {
        return String.format("REVOKE %s ON %s FROM '%s'@'%s'",
                String.join(",", privileges),
                wrapIdentifier(tableName),
                userName,
                host
        );
    }

    /**
     * 获取表的权限信息
     */
    public String getTablePrivilegesSql(String tableName) {
        return String.format(
                "SELECT * FROM information_schema.table_privileges " +
                        "WHERE table_schema = '%s' AND table_name = '%s'",
                getSchema(),
                tableName
        );
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
                if (column.getLength() != null) {
                    column.setLength(Math.min(column.getLength(), getMaxVarcharLength()));
                }

                // MySQL特定的类型调整
                adjustMySQLColumnType(column);
            }

            // 设置MySQL特定属性
            if (tableDefinition.getEngine() == null) {
                tableDefinition.setEngine("InnoDB");
            }
            if (tableDefinition.getCharset() == null) {
                tableDefinition.setCharset("utf8mb4");
            }
            if (tableDefinition.getCollate() == null) {
                tableDefinition.setCollate("utf8mb4_general_ci");
            }

            // 生成MySQL建表语句
            return generateCreateTableSql(tableDefinition);
        } catch (Exception e) {
            log.error("Failed to convert CREATE TABLE SQL from {} to MySQL: {}", sourceDbType, e.getMessage());
            throw new RuntimeException("Failed to convert CREATE TABLE SQL", e);
        }
    }

    /**
     * 调整MySQL列类型
     */
    private void adjustMySQLColumnType(ColumnDefinition column) {
        String type = column.getType().toUpperCase();

        // 处理 VARCHAR/NVARCHAR 类型长度
        if (type.equals("VARCHAR") || type.equals("NVARCHAR")) {
            if (column.getLength() == null) {
                column.setLength(DEFAULT_VARCHAR_LENGTH);
            } else if (column.getLength() > MAX_VARCHAR_LENGTH) {
                // 超出长度限制时自动转为TEXT类型
                column.setType("TEXT");
                column.setLength(null); // TEXT 不需要长度
            }
        }

        // 处理 CHAR/NCHAR 类型长度
        else if (type.equals("CHAR") || type.equals("NCHAR")) {
            if (column.getLength() == null) {
                column.setLength(1); // CHAR默认长度为1
            } else if (column.getLength() > 255) {
                column.setLength(255); // MySQL CHAR最大长度为255
            }
        }

        // 处理 DECIMAL/NUMERIC 类型精度和小数位
        else if (type.equals("DECIMAL") || type.equals("NUMERIC")) {
            if (column.getPrecision() == null) {
                column.setPrecision(10); // 默认精度为10
            } else if (column.getPrecision() > 65) {
                column.setPrecision(65); // MySQL最大精度为65
            }

            if (column.getScale() == null) {
                column.setScale(0); // 默认小数位为0
            } else if (column.getScale() > 30 || column.getScale() > column.getPrecision()) {
                column.setScale(Math.min(30, column.getPrecision())); // MySQL最大小数位为30，且不能大于精度
            }
        }

        // INT类型不需要长度
        else if (type.equals("INT") || type.equals("INTEGER") ||
                type.equals("SMALLINT") || type.equals("TINYINT") ||
                type.equals("MEDIUMINT") || type.equals("BIGINT")) {
            column.setLength(null); // MySQL 8.0+不建议为整数类型指定长度
        }

        // TEXT/BLOB类型不需要长度
        else if (type.equals("TEXT") || type.equals("MEDIUMTEXT") ||
                type.equals("LONGTEXT") || type.equals("TINYTEXT") ||
                type.equals("BLOB") || type.equals("MEDIUMBLOB") ||
                type.equals("LONGBLOB") || type.equals("TINYBLOB")) {
            column.setLength(null);
        }

        // 处理 TIMESTAMP 类型
        else if (type.equals("TIMESTAMP")) {
            // MySQL的TIMESTAMP有范围限制(1970-2038)
            column.setLength(null);
        }

        // 处理 JSON 类型
        else if (type.equals("JSON")) {
            column.setLength(null); // JSON 类型不需要长度
            column.setPrecision(null);
            column.setScale(null);
        }

        // 处理 ENUM 和 SET 类型
        // 这两种类型需要保留原始定义，它们的"长度"实际上是值列表
    }

    @Override
    public String generateCreateTableSql(TableDefinition tableDefinition) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ");
        if (tableDefinition.getTableName().contains(".")) {
            sql.append(tableDefinition.getTableName());
        } else {
            sql.append(wrapIdentifier(tableDefinition.getTableName()));
        }
        sql.append(" (\n");

        List<String> columnDefinitions = new ArrayList<>();
        List<String> primaryKeys = new ArrayList<>();

        for (ColumnDefinition column : tableDefinition.getColumns()) {
            StringBuilder columnSql = new StringBuilder();
            columnSql.append(wrapIdentifier(column.getName())).append(" ");

            // 数据类型
            columnSql.append(column.getType().toUpperCase());

            // 长度/精度
            if (column.getLength() != null) {
                if (column.getType().equalsIgnoreCase("DECIMAL") ||
                        column.getType().equalsIgnoreCase("NUMERIC")) {
                    columnSql.append("(")
                            .append(column.getPrecision())
                            .append(",")
                            .append(column.getScale())
                            .append(")");
                } else if (!column.getType().equalsIgnoreCase("TEXT") &&
                        !column.getType().equalsIgnoreCase("MEDIUMTEXT") &&
                        !column.getType().equalsIgnoreCase("LONGTEXT") &&
                        !column.getType().equalsIgnoreCase("BLOB") &&
                        !column.getType().equalsIgnoreCase("MEDIUMBLOB") &&
                        !column.getType().equalsIgnoreCase("LONGBLOB")) {
                    columnSql.append("(").append(column.getLength()).append(")");
                }
            }

            // 字符集
            if (StringUtils.isNotBlank(column.getCharset())) {
                columnSql.append(" CHARACTER SET ").append(column.getCharset());
            }

            // 排序规则
            if (StringUtils.isNotBlank(column.getCollate())) {
                columnSql.append(" COLLATE ").append(column.getCollate());
            }

            // 是否可空
            columnSql.append(column.isNullable() ? " NULL" : " NOT NULL");

            // 默认值
            if (column.getDefaultValue() != null) {
                columnSql.append(" DEFAULT ").append(column.getDefaultValue());
            }

            // 自增
            if (column.isAutoIncrement()) {
                columnSql.append(" AUTO_INCREMENT");
            }

            // 注释
            if (StringUtils.isNotBlank(column.getComment())) {
                columnSql.append(" COMMENT ").append(wrapValue(column.getComment()));
            }

            // 主键
            if (column.isPrimaryKey()) {
                primaryKeys.add(wrapIdentifier(column.getName()));
            }

            columnDefinitions.add(columnSql.toString());
        }

        // 添加主键定义
        if (!primaryKeys.isEmpty()) {
            columnDefinitions.add("PRIMARY KEY (" + String.join(",", primaryKeys) + ")");
        }

        // 添加索引定义
        if (tableDefinition.getIndexes() != null) {
            for (IndexDefinition index : tableDefinition.getIndexes()) {
                if (!index.isPrimaryKey()) {
                    StringBuilder indexSql = new StringBuilder();
                    if (index.isUnique()) {
                        indexSql.append("UNIQUE ");
                    }
                    indexSql.append("KEY ");
                    indexSql.append(wrapIdentifier(index.getName())).append(" (");
                    indexSql.append(index.getColumns().stream()
                            .map(this::wrapIdentifier)
                            .collect(Collectors.joining(",")));
                    indexSql.append(")");
                    columnDefinitions.add(indexSql.toString());
                }
            }
        }

        sql.append(String.join(",\n", columnDefinitions));
        sql.append("\n)");

        // 存储引擎
        if (StringUtils.isNotBlank(tableDefinition.getEngine())) {
            sql.append(" ENGINE=").append(tableDefinition.getEngine());
        }

        // 字符集
        if (StringUtils.isNotBlank(tableDefinition.getCharset())) {
            sql.append(" DEFAULT CHARACTER SET=").append(tableDefinition.getCharset());
        }

        // 排序规则
        if (StringUtils.isNotBlank(tableDefinition.getCollate())) {
            sql.append(" COLLATE=").append(tableDefinition.getCollate());
        }

        // 表注释
        if (StringUtils.isNotBlank(tableDefinition.getTableComment())) {
            sql.append(" COMMENT=").append(wrapValue(tableDefinition.getTableComment()));
        }

        return sql.toString();
    }

    @Override
    public TableDefinition parseCreateTableSql(String createTableSql) {
        TableDefinition tableDefinition = new TableDefinition();
        List<ColumnDefinition> columns = new ArrayList<>();
        List<IndexDefinition> indexes = new ArrayList<>();

        try {
            // 解析表名
            Pattern tablePattern = Pattern.compile("CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([\\w\\.`\"]+)\\s*\\(",
                    Pattern.CASE_INSENSITIVE);
            Matcher tableMatcher = tablePattern.matcher(createTableSql);
            if (tableMatcher.find()) {
                String tableName = tableMatcher.group(1).replace("`", "").replace("\"", "");
                tableDefinition.setTableName(tableName);
            }

            // 提取字段定义部分
            int startIndex = createTableSql.indexOf('(');
            int endIndex = createTableSql.lastIndexOf(')');
            if (startIndex == -1 || endIndex == -1) {
                throw new RuntimeException("Invalid CREATE TABLE SQL statement");
            }

            String columnsPart = createTableSql.substring(startIndex + 1, endIndex);
            String[] definitions = splitDefinitions(columnsPart);

            for (String definition : definitions) {
                definition = definition.trim();
                if (definition.isEmpty()) continue;

                // 处理主键定义
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

                // 处理索引定义
                if (definition.toUpperCase().startsWith("INDEX") ||
                        definition.toUpperCase().startsWith("KEY") ||
                        definition.toUpperCase().startsWith("UNIQUE")) {

                    Pattern indexPattern = Pattern.compile("(?:(UNIQUE)\\s+)?(?:INDEX|KEY)\\s+`?([\\w]+)`?\\s*\\(([^)]+)\\)",
                            Pattern.CASE_INSENSITIVE);
                    Matcher indexMatcher = indexPattern.matcher(definition);

                    if (indexMatcher.find()) {
                        IndexDefinition index = new IndexDefinition();
                        index.setUnique(indexMatcher.group(1) != null);
                        index.setName(indexMatcher.group(2));

                        String[] indexColumns = indexMatcher.group(3).split(",");
                        List<String> columnList = new ArrayList<>();
                        for (String col : indexColumns) {
                            columnList.add(col.trim().replace("`", ""));
                        }
                        index.setColumns(columnList);

                        indexes.add(index);
                    }
                    continue;
                }

                // 处理普通字段定义
                ColumnDefinition column = parseColumnDefinition(definition);
                if (column != null) {
                    columns.add(column);
                }
            }

            tableDefinition.setColumns(columns);
            tableDefinition.setIndexes(indexes);

            // 解析表属性
            Pattern enginePattern = Pattern.compile("ENGINE\\s*=\\s*(\\w+)", Pattern.CASE_INSENSITIVE);
            Matcher engineMatcher = enginePattern.matcher(createTableSql);
            if (engineMatcher.find()) {
                tableDefinition.setEngine(engineMatcher.group(1));
            }

            Pattern charsetPattern = Pattern.compile("CHARACTER\\s+SET\\s*=?\\s*(\\w+)", Pattern.CASE_INSENSITIVE);
            Matcher charsetMatcher = charsetPattern.matcher(createTableSql);
            if (charsetMatcher.find()) {
                tableDefinition.setCharset(charsetMatcher.group(1));
            }

            Pattern collatePattern = Pattern.compile("COLLATE\\s*=?\\s*(\\w+)", Pattern.CASE_INSENSITIVE);
            Matcher collateMatcher = collatePattern.matcher(createTableSql);
            if (collateMatcher.find()) {
                tableDefinition.setCollate(collateMatcher.group(1));
            }

            Pattern commentPattern = Pattern.compile("COMMENT\\s*=?\\s*'([^']*)'", Pattern.CASE_INSENSITIVE);
            Matcher commentMatcher = commentPattern.matcher(createTableSql);
            if (commentMatcher.find()) {
                tableDefinition.setTableComment(commentMatcher.group(1));
            }

            return tableDefinition;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CREATE TABLE SQL: " + e.getMessage(), e);
        }
    }

    /**
     * Split column definitions while handling nested parentheses and quotes
     */
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

    /**
     * Parse a single column definition from CREATE TABLE SQL
     */
    private ColumnDefinition parseColumnDefinition(String definition) {
        // Basic column information regex pattern
        Pattern columnPattern = Pattern.compile(
                "`?([\\w]+)`?\\s+" +                    // Column name
                        "([\\w\\(\\),]+)" +                     // Data type
                        "(?:\\s+CHARACTER\\s+SET\\s+(\\w+))?" + // Character set (optional)
                        "(?:\\s+COLLATE\\s+(\\w+))?" +         // Collation (optional)
                        "(?:\\s+DEFAULT\\s+([^\\s,]+))?" +     // Default value (optional)
                        "(?:\\s+NOT\\s+NULL)?" +               // NOT NULL (optional)
                        "(?:\\s+AUTO_INCREMENT)?" +            // Auto increment (optional)
                        "(?:\\s+COMMENT\\s+'([^']*)')?" +      // Comment (optional)
                        "(?:\\s+PRIMARY\\s+KEY)?",             // Primary key (optional)
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = columnPattern.matcher(definition);
        if (!matcher.find()) {
            return null;
        }

        ColumnDefinition column = new ColumnDefinition();
        column.setName(matcher.group(1));

        // Parse type and length/precision
        String fullType = matcher.group(2);
        Pattern typePattern = Pattern.compile("([\\w]+)(?:\\((\\d+)(?:,(\\d+))?\\))?");
        Matcher typeMatcher = typePattern.matcher(fullType);
        if (typeMatcher.find()) {
            column.setType(typeMatcher.group(1));
            if (typeMatcher.group(2) != null) {
                if (typeMatcher.group(3) != null) {
                    // Has precision and scale
                    column.setPrecision(Integer.parseInt(typeMatcher.group(2)));
                    column.setScale(Integer.parseInt(typeMatcher.group(3)));
                } else {
                    // Only has length
                    column.setLength(Integer.parseInt(typeMatcher.group(2)));
                }
            }
        }

        // Set character set
        if (matcher.group(3) != null) {
            column.setCharset(matcher.group(3));
        }

        // Set collation
        if (matcher.group(4) != null) {
            column.setCollate(matcher.group(4));
        }

        // Set default value
        if (matcher.group(5) != null) {
            column.setDefaultValue(matcher.group(5));
        }

        // Set nullable
        column.setNullable(!definition.toUpperCase().contains("NOT NULL"));

        // Set auto increment
        column.setAutoIncrement(definition.toUpperCase().contains("AUTO_INCREMENT"));

        // Set comment
        if (matcher.group(6) != null) {
            column.setComment(matcher.group(6));
        }

        // Set primary key
        column.setPrimaryKey(definition.toUpperCase().contains("PRIMARY KEY"));

        return column;
    }

    @Override
    public List<TableDefinition> getAllTables(String database) throws Exception {
        List<TableDefinition> tables = new ArrayList<>();
        String sql = "SELECT " +
                "    t.TABLE_NAME AS name, " +
                "    t.TABLE_SCHEMA AS `schema`, " +
                "    t.TABLE_COMMENT AS comment, " +
                "    t.CREATE_TIME AS createTime, " +
                "    t.UPDATE_TIME AS updateTime, " +
                "    t.TABLE_ROWS AS tableRows, " +
                "    t.DATA_LENGTH + t.INDEX_LENGTH AS totalSize, " +
                "    t.DATA_LENGTH AS dataSize, " +
                "    t.ENGINE AS engine, " +
                "    t.TABLE_COLLATION AS collation " +
                "FROM information_schema.TABLES t " +
                "WHERE t.TABLE_SCHEMA = ? " +
                "ORDER BY t.TABLE_NAME";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, database);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TableDefinition table = TableDefinition.builder()
                            .tableName(rs.getString("name"))
                            .tableComment(rs.getString("comment"))
                            .engine(rs.getString("engine"))
                            .collate(rs.getString("collation"))
                            .build();

                    // 获取建表语句
                    try (PreparedStatement showStmt = conn.prepareStatement("SHOW CREATE TABLE " + wrapIdentifier(table.getTableName()))) {
                        try (ResultSet showRs = showStmt.executeQuery()) {
                            if (showRs.next()) {
                                Map<String, String> extraProperties = new HashMap<>();
                                extraProperties.put("createSql", showRs.getString(2));
                                table.setExtraProperties(extraProperties);
                            }
                        }
                    }

                    // 获取索引信息
                    try (PreparedStatement indexStmt = conn.prepareStatement(
                            "SHOW INDEX FROM " + wrapIdentifier(table.getTableName()))) {
                        try (ResultSet indexRs = indexStmt.executeQuery()) {
                            List<IndexDefinition> indexes = new ArrayList<>();
                            while (indexRs.next()) {
                                IndexDefinition indexDefinition = IndexDefinition.builder()
                                        .name(indexRs.getString("Key_name"))
                                        .unique("UNIQUE".equals(indexRs.getString("Non_unique")))
                                        .build();
                                indexes.add(indexDefinition);
                            }
                            table.setIndexes(indexes);
                        }
                    }

                    tables.add(table);
                }
            }
        }

        return tables;
    }

    @Override
    public List<ColumnDefinition> getTableColumns(String database, String tableName) throws Exception {
        String sql = "SELECT " +
                "    c.COLUMN_NAME AS name, " +
                "    c.DATA_TYPE AS dataType, " +
                "    c.CHARACTER_MAXIMUM_LENGTH AS length, " +
                "    c.NUMERIC_PRECISION AS `precision`, " +  // 使用反引号括起precision关键字
                "    c.NUMERIC_SCALE AS scale, " +
                "    c.IS_NULLABLE AS nullable, " +
                "    c.COLUMN_DEFAULT AS defaultValue, " +
                "    c.COLUMN_COMMENT AS comment, " +
                "    c.ORDINAL_POSITION AS position, " +
                "    c.COLUMN_KEY AS columnKey, " +
                "    c.EXTRA AS extra " +
                "FROM " +
                "    INFORMATION_SCHEMA.COLUMNS c " +
                "WHERE " +
                "    c.TABLE_SCHEMA = ? " +
                "AND c.TABLE_NAME = ? " +
                "ORDER BY c.ORDINAL_POSITION";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, database);
            stmt.setString(2, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                List<ColumnDefinition> columns = new ArrayList<>();
                while (rs.next()) {
                    ColumnDefinition column = ColumnDefinition.builder()
                            .name(rs.getString("name"))
                            .type(rs.getString("dataType"))
                            .length(rs.getObject("length") != null ? rs.getInt("length") : null)
                            .precision(rs.getObject("precision") != null ? rs.getInt("precision") : null)
                            .scale(rs.getObject("scale") != null ? rs.getInt("scale") : null)
                            .nullable("YES".equalsIgnoreCase(rs.getString("nullable")))
                            .defaultValue(rs.getString("defaultValue"))
                            .comment(rs.getString("comment"))
                            .primaryKey("PRI".equals(rs.getString("columnKey")))
                            .autoIncrement(rs.getString("extra").contains("auto_increment"))
                            .build();
                    columns.add(column);
                }
                return columns;
            }
        }
    }

    @Override
    public String getTableEngine(String tableName) throws Exception {
        String sql = "SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, getSchema());
            stmt.setString(2, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("ENGINE") : null;
            }
        }
    }

    @Override
    public String getTableCharset(String tableName) throws Exception {
        String sql = "SELECT TABLE_COLLATION FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, getSchema());
            stmt.setString(2, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String collation = rs.getString("TABLE_COLLATION");
                    return collation != null ? collation.split("_")[0] : null;
                }
                return null;
            }
        }
    }

    @Override
    public String getTableCollation(String tableName) throws Exception {
        String sql = "SELECT TABLE_COLLATION FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, getSchema());
            stmt.setString(2, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("TABLE_COLLATION") : null;
            }
        }
    }

    @Override
    public Long getTableSize(String tableName) throws Exception {
        String sql = "SELECT DATA_LENGTH + INDEX_LENGTH as total_size FROM information_schema.TABLES " +
                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, getSchema());
            stmt.setString(2, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong("total_size") : 0L;
            }
        }
    }

    @Override
    public Long getTableRowCount(String tableName) throws Exception {
        String sql = "SELECT TABLE_ROWS FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, getSchema());
            stmt.setString(2, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong("TABLE_ROWS") : 0L;
            }
        }
    }

    @Override
    public String getTableSpace(String tableName) throws Exception {
        String sql = "SELECT TABLESPACE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, getSchema());
            stmt.setString(2, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("TABLESPACE_NAME") : null;
            }
        }
    }

    @Override
    public Integer getCharacterLength(String tableName, String columnName) throws Exception {
        String sql = "SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, getSchema());
            stmt.setString(2, tableName);
            stmt.setString(3, columnName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("CHARACTER_MAXIMUM_LENGTH") : null;
            }
        }
    }

    @Override
    public Integer getNumericPrecision(String tableName, String columnName) throws Exception {
        String sql = "SELECT NUMERIC_PRECISION FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, getSchema());
            stmt.setString(2, tableName);
            stmt.setString(3, columnName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("NUMERIC_PRECISION") : null;
            }
        }
    }

    @Override
    public Integer getNumericScale(String tableName, String columnName) throws Exception {
        String sql = "SELECT NUMERIC_SCALE FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, getSchema());
            stmt.setString(2, tableName);
            stmt.setString(3, columnName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("NUMERIC_SCALE") : null;
            }
        }
    }

    @Override
    public String getColumnDefault(String tableName, String columnName) throws Exception {
        String sql = "SELECT COLUMN_DEFAULT FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, getSchema());
            stmt.setString(2, tableName);
            stmt.setString(3, columnName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("COLUMN_DEFAULT") : null;
            }
        }
    }

    @Override
    public String getColumnExtra(String tableName, String columnName) throws Exception {
        String sql = "SELECT EXTRA FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, getSchema());
            stmt.setString(2, tableName);
            stmt.setString(3, columnName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("EXTRA") : null;
            }
        }
    }

    @Override
    public Integer getColumnPosition(String tableName, String columnName) throws Exception {
        String sql = "SELECT ORDINAL_POSITION FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, getSchema());
            stmt.setString(2, tableName);
            stmt.setString(3, columnName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("ORDINAL_POSITION") : null;
            }
        }
    }

    @Override
    public Map<String, Object> getTableCompleteness(String tableName) throws Exception {
        Map<String, Object> result = new HashMap<>();
        String sql = "SELECT " +
                "COUNT(*) as total_rows, " +
                "SUM(CASE WHEN %s IS NULL THEN 1 ELSE 0 END) as null_count " +
                "FROM " + escapeIdentifier(tableName);

        List<Map<String, Object>> columns = listColumns(tableName);
        Map<String, Double> completenessRates = new HashMap<>();

        try (Connection conn = getConnection()) {
            for (Map<String, Object> column : columns) {
                String columnName = (String) column.get("COLUMN_NAME");
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(String.format(sql, escapeIdentifier(columnName)))) {
                    if (rs.next()) {
                        long totalRows = rs.getLong("total_rows");
                        long nullCount = rs.getLong("null_count");
                        double completeness = totalRows > 0 ? (totalRows - nullCount) * 100.0 / totalRows : 100.0;
                        completenessRates.put(columnName, completeness);
                    }
                }
            }
        }

        result.put("columnCompleteness", completenessRates);
        result.put("overallCompleteness", completenessRates.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0));

        return result;
    }

    @Override
    public Map<String, Object> getTableAccuracy(String tableName) throws Exception {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> columns = listColumns(tableName);
        Map<String, Double> accuracyRates = new HashMap<>();

        try (Connection conn = getConnection()) {
            for (Map<String, Object> column : columns) {
                String columnName = (String) column.get("name");
                String dataType = (String) column.get("DATA_TYPE");

                String validationSql = getValidationSqlForDataType(tableName, columnName, dataType);
                if (validationSql != null) {
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(validationSql)) {
                        if (rs.next()) {
                            long totalRows = rs.getLong("total_rows");
                            long invalidRows = rs.getLong("invalid_rows");
                            double accuracy = totalRows > 0 ? (totalRows - invalidRows) * 100.0 / totalRows : 100.0;
                            accuracyRates.put(columnName, accuracy);
                        }
                    }
                } else {
                    accuracyRates.put(columnName, 100.0);
                }
            }
        }

        result.put("columnAccuracy", accuracyRates);
        result.put("overallAccuracy", accuracyRates.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0));

        return result;
    }

    private String getValidationSqlForDataType(String tableName, String columnName, String dataType) {
        String escapedTableName = escapeIdentifier(tableName);
        String escapedColumnName = escapeIdentifier(columnName);

        switch (dataType.toLowerCase()) {
            case "int":
            case "bigint":
            case "tinyint":
            case "smallint":
            case "mediumint":
                return String.format(
                        "SELECT COUNT(*) as total_rows, " +
                                "SUM(CASE WHEN %s IS NOT NULL AND %s REGEXP '^-?[0-9]+$' = 0 THEN 1 ELSE 0 END) as invalid_rows " +
                                "FROM %s", escapedColumnName, escapedColumnName, escapedTableName);

            case "decimal":
            case "float":
            case "double":
                return String.format(
                        "SELECT COUNT(*) as total_rows, " +
                                "SUM(CASE WHEN %s IS NOT NULL AND %s REGEXP '^-?[0-9]*.?[0-9]+$' = 0 THEN 1 ELSE 0 END) as invalid_rows " +
                                "FROM %s", escapedColumnName, escapedColumnName, escapedTableName);

            case "date":
                return String.format(
                        "SELECT COUNT(*) as total_rows, " +
                                "SUM(CASE WHEN %s IS NOT NULL AND STR_TO_DATE(%s, '%%Y-%%m-%%d') IS NULL THEN 1 ELSE 0 END) as invalid_rows " +
                                "FROM %s", escapedColumnName, escapedColumnName, escapedTableName);

            case "datetime":
            case "timestamp":
                return String.format(
                        "SELECT COUNT(*) as total_rows, " +
                                "SUM(CASE WHEN %s IS NOT NULL AND STR_TO_DATE(%s, '%%Y-%%m-%%d %%H:%%i:%%s') IS NULL THEN 1 ELSE 0 END) as invalid_rows " +
                                "FROM %s", escapedColumnName, escapedColumnName, escapedTableName);

            default:
                return null;
        }
    }

    public List<Map<String, Object>> getTableChangeHistory(String tableName) throws Exception {
        List<Map<String, Object>> history = new ArrayList<>();

        try (Connection conn = getConnection()) {
            // 查询表的变更历史记录
            String sql = "SELECT " +
                    "event_time, " +
                    "user, " +
                    "event_type, " +
                    "command_text " +
                    "FROM mysql.general_log " +
                    "WHERE argument LIKE ? " +
                    "AND command_type IN ('ALTER', 'DROP', 'CREATE', 'RENAME') " +
                    "ORDER BY event_time DESC";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, "%" + tableName + "%");

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> change = new HashMap<>();
                        change.put("changeTime", rs.getTimestamp("event_time"));
                        change.put("user", rs.getString("user"));
                        change.put("operationType", rs.getString("event_type"));
                        change.put("sql", rs.getString("command_text"));
                        history.add(change);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取表变更历史失败: table={}, error={}", tableName, e.getMessage(), e);
        }

        return history;
    }

    @Override
    public List<Map<String, Object>> listColumns(String tableName) throws Exception {
        List<Map<String, Object>> columns = new ArrayList<>();
        try (Connection conn = getConnection()) {
            String sql = "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, CHARACTER_MAXIMUM_LENGTH, " +
                    "NUMERIC_PRECISION, NUMERIC_SCALE, IS_NULLABLE, COLUMN_DEFAULT, " +
                    "COLUMN_COMMENT, EXTRA, ORDINAL_POSITION " +
                    "FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? " +
                    "ORDER BY ORDINAL_POSITION";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> column = new HashMap<>();
                        column.put("name", rs.getString("COLUMN_NAME"));
                        column.put("type", rs.getString("DATA_TYPE"));
                        column.put("fullType", rs.getString("COLUMN_TYPE"));
                        column.put("maxLength", rs.getLong("CHARACTER_MAXIMUM_LENGTH"));
                        column.put("precision", rs.getInt("NUMERIC_PRECISION"));
                        column.put("scale", rs.getInt("NUMERIC_SCALE"));
                        column.put("nullable", "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")));
                        column.put("defaultValue", rs.getString("COLUMN_DEFAULT"));
                        column.put("comment", rs.getString("COLUMN_COMMENT"));
                        column.put("position", rs.getInt("ORDINAL_POSITION"));
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
            String sql = "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, CHARACTER_MAXIMUM_LENGTH, " +
                    "NUMERIC_PRECISION, NUMERIC_SCALE, IS_NULLABLE, COLUMN_DEFAULT, " +
                    "COLUMN_COMMENT, EXTRA, ORDINAL_POSITION " +
                    "FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);
                stmt.setString(3, columnName);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        info.put("name", rs.getString("COLUMN_NAME"));
                        info.put("type", rs.getString("DATA_TYPE"));
                        info.put("fullType", rs.getString("COLUMN_TYPE"));
                        info.put("maxLength", rs.getLong("CHARACTER_MAXIMUM_LENGTH"));
                        info.put("precision", rs.getInt("NUMERIC_PRECISION"));
                        info.put("scale", rs.getInt("NUMERIC_SCALE"));
                        info.put("nullable", "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")));
                        info.put("defaultValue", rs.getString("COLUMN_DEFAULT"));
                        info.put("comment", rs.getString("COLUMN_COMMENT"));
                        info.put("extra", rs.getString("EXTRA"));
                        info.put("position", rs.getInt("ORDINAL_POSITION"));
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取字段信息失败: table={}, column={}, error={}", tableName, columnName, e.getMessage(), e);
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
                            result.put("completeness", 1 - (double)nullCount / rs.getLong("total_count"));
                            break;

                        case "uniqueness":
                            long uniqueCount = rs.getLong("unique_count");
                            result.put("uniqueCount", uniqueCount);
                            result.put("uniqueness", (double)uniqueCount / rs.getLong("total_count"));
                            break;

                        case "validity":
                            long invalidCount = rs.getLong("invalid_count");
                            result.put("invalidCount", invalidCount);
                            result.put("validity", 1 - (double)invalidCount / rs.getLong("total_count"));
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
    public Map<String, Object> getTableConsistency(String tableName) throws Exception {
        Map<String, Object> result = new HashMap<>();
        try (Connection conn = getConnection()) {
            // 检查主键一致性
            String pkSql = "SELECT COUNT(*) as total_rows, COUNT(DISTINCT pk_columns) as unique_keys " +
                    "FROM (SELECT CONCAT_WS(',', pk_columns) as pk_columns " +
                    "FROM information_schema.KEY_COLUMN_USAGE " +
                    "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND CONSTRAINT_NAME = 'PRIMARY') t";

            try (PreparedStatement stmt = conn.prepareStatement(pkSql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        result.put("primaryKeyConsistency",
                                rs.getLong("unique_keys") == rs.getLong("total_rows"));
                    }
                }
            }

            // 检查外键一致性
            String fkSql = "SELECT COUNT(*) as violation_count " +
                    "FROM information_schema.REFERENTIAL_CONSTRAINTS rc " +
                    "JOIN information_schema.KEY_COLUMN_USAGE kcu " +
                    "ON rc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME " +
                    "WHERE rc.TABLE_SCHEMA = ? AND rc.TABLE_NAME = ?";

            try (PreparedStatement stmt = conn.prepareStatement(fkSql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        result.put("foreignKeyConsistency", rs.getLong("violation_count") == 0);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取表一致性失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return result;
    }

    @Override
    public Map<String, Object> getTableUniqueness(String tableName) throws Exception {
        Map<String, Object> result = new HashMap<>();
        try (Connection conn = getConnection()) {
            // 获取所有唯一索引列
            String sql = "SELECT GROUP_CONCAT(COLUMN_NAME) as unique_columns " +
                    "FROM information_schema.STATISTICS " +
                    "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? " +
                    "AND NON_UNIQUE = 0 " +
                    "GROUP BY INDEX_NAME";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);

                List<String> uniqueColumns = new ArrayList<>();
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        uniqueColumns.add(rs.getString("unique_columns"));
                    }
                }

                result.put("uniqueColumns", uniqueColumns);

                // 检查每组唯一列的重复值
                for (String columns : uniqueColumns) {
                    String[] columnArray = columns.split(",");
                    String duplicateCheckSql = String.format(
                            "SELECT COUNT(*) as total_count, " +
                                    "COUNT(DISTINCT %s) as unique_count " +
                                    "FROM %s",
                            String.join(",", columnArray),
                            wrapIdentifier(tableName));

                    try (Statement checkStmt = conn.createStatement();
                         ResultSet rs = checkStmt.executeQuery(duplicateCheckSql)) {
                        if (rs.next()) {
                            result.put(columns + "_uniqueness",
                                    (double)rs.getLong("unique_count") / rs.getLong("total_count"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取表唯一性失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return result;
    }

    @Override
    public Map<String, Object> getTableValidity(String tableName) throws Exception {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> columns = listColumns(tableName);
        Map<String, Double> validityRates = new HashMap<>();
        Map<String, Long> invalidCounts = new HashMap<>();

        try (Connection conn = getConnection()) {
            long totalRows = getTableRowCount(tableName);
            result.put("totalRows", totalRows);

            for (Map<String, Object> column : columns) {
                String columnName = (String) column.get("name");
                String dataType = (String) column.get("type");

                // 根据数据类型构建验证SQL
                String validationSql = getValidationSql(tableName, columnName);
                if (validationSql != null) {
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(validationSql)) {
                        if (rs.next()) {
                            long invalidCount = rs.getLong("invalid_count");
                            invalidCounts.put(columnName, invalidCount);

                            // 计算有效率
                            double validityRate = 1.0;
                            if (totalRows > 0) {
                                long validCount = totalRows - invalidCount;
                                validityRate = (double) validCount / totalRows;
                            }
                            validityRates.put(columnName, validityRate);
                        }
                    }
                } else {
                    // 如果没有适用的验证规则，假设所有数据都有效
                    validityRates.put(columnName, 1.0);
                    invalidCounts.put(columnName, 0L);
                }
            }
        } catch (Exception e) {
            log.error("获取表有效性失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }

        result.put("columnValidity", validityRates);
        result.put("invalidCounts", invalidCounts);

        // 计算整体有效性
        double overallValidity = validityRates.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(1.0);
        result.put("overallValidity", overallValidity);

        // 计算总无效数据量
        long totalInvalidCount = invalidCounts.values().stream()
                .mapToLong(Long::longValue)
                .sum();
        result.put("totalInvalidCount", totalInvalidCount);

        return result;
    }

    private String getValidationSql(String tableName, String columnName) {
        String escapedTableName = escapeIdentifier(tableName);
        String escapedColumnName = escapeIdentifier(columnName);

        // 获取列信息
        try {
            Map<String, Object> columnInfo = getColumnInfo(tableName, columnName);
            String dataType = ((String) columnInfo.get("type")).toLowerCase();

            // 根据数据类型构建验证SQL
            switch (dataType) {
                case "int":
                case "bigint":
                case "tinyint":
                case "smallint":
                case "mediumint":
                    return String.format(
                            "SELECT COUNT(*) as invalid_count FROM %s " +
                                    "WHERE %s IS NOT NULL AND %s REGEXP '^-?[0-9]+$' = 0",
                            escapedTableName, escapedColumnName, escapedColumnName);

                case "decimal":
                case "numeric":
                case "float":
                case "double":
                    return String.format(
                            "SELECT COUNT(*) as invalid_count FROM %s " +
                                    "WHERE %s IS NOT NULL AND %s REGEXP '^-?[0-9]+(\\.[0-9]+)?$' = 0",
                            escapedTableName, escapedColumnName, escapedColumnName);

                case "date":
                    return String.format(
                            "SELECT COUNT(*) as invalid_count FROM %s " +
                                    "WHERE %s IS NOT NULL AND STR_TO_DATE(%s, '%%Y-%%m-%%d') IS NULL",
                            escapedTableName, escapedColumnName, escapedColumnName);

                case "datetime":
                case "timestamp":
                    return String.format(
                            "SELECT COUNT(*) as invalid_count FROM %s " +
                                    "WHERE %s IS NOT NULL AND STR_TO_DATE(%s, '%%Y-%%m-%%d %%H:%%i:%%s') IS NULL",
                            escapedTableName, escapedColumnName, escapedColumnName);

                case "time":
                    return String.format(
                            "SELECT COUNT(*) as invalid_count FROM %s " +
                                    "WHERE %s IS NOT NULL AND STR_TO_DATE(%s, '%%H:%%i:%%s') IS NULL",
                            escapedTableName, escapedColumnName, escapedColumnName);

                case "year":
                    return String.format(
                            "SELECT COUNT(*) as invalid_count FROM %s " +
                                    "WHERE %s IS NOT NULL AND %s REGEXP '^[0-9]{4}$' = 0",
                            escapedTableName, escapedColumnName, escapedColumnName);

                case "char":
                case "varchar":
                case "text":
                    // 对于字符串类型，检查是否超过最大长度
                    Integer maxLength = (Integer) columnInfo.get("length");
                    if (maxLength != null && maxLength > 0) {
                        return String.format(
                                "SELECT COUNT(*) as invalid_count FROM %s " +
                                        "WHERE %s IS NOT NULL AND CHAR_LENGTH(%s) > %d",
                                escapedTableName, escapedColumnName, escapedColumnName, maxLength);
                    }
                    break;

                case "enum":
                case "set":
                    // 对于枚举类型，检查是否在允许的值范围内
                    // 这需要获取枚举的允许值，这里简化处理
                    return String.format(
                            "SELECT COUNT(*) as invalid_count FROM %s " +
                                    "WHERE %s IS NOT NULL AND %s = ''",
                            escapedTableName, escapedColumnName, escapedColumnName);

                case "bit":
                case "boolean":
                    return String.format(
                            "SELECT COUNT(*) as invalid_count FROM %s " +
                                    "WHERE %s IS NOT NULL AND %s NOT IN (0, 1)",
                            escapedTableName, escapedColumnName, escapedColumnName);
            }
        } catch (Exception e) {
            log.warn("构建验证SQL失败: table={}, column={}, error={}",
                    tableName, columnName, e.getMessage());
        }

        return null;
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
                String validationSql = getValidationSql(tableName, columnName);
                if (validationSql != null) {
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(validationSql)) {
                        if (rs.next()) {
                            long invalidRows = rs.getLong("invalid_count");
                            double ratio = (double) invalidRows / totalRows;
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
                    "kcu.REFERENCED_TABLE_NAME, " +
                    "kcu.REFERENCED_COLUMN_NAME, " +
                    "kcu.COLUMN_NAME, " +
                    "rc.CONSTRAINT_NAME, " +
                    "rc.UPDATE_RULE, " +
                    "rc.DELETE_RULE " +
                    "FROM information_schema.KEY_COLUMN_USAGE kcu " +
                    "JOIN information_schema.REFERENTIAL_CONSTRAINTS rc " +
                    "ON kcu.CONSTRAINT_NAME = rc.CONSTRAINT_NAME " +
                    "WHERE kcu.TABLE_SCHEMA = ? " +
                    "AND kcu.TABLE_NAME = ? " +
                    "AND kcu.REFERENCED_TABLE_NAME IS NOT NULL";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> relation = new HashMap<>();
                        relation.put("referencedTable", rs.getString("REFERENCED_TABLE_NAME"));
                        relation.put("referencedColumn", rs.getString("REFERENCED_COLUMN_NAME"));
                        relation.put("sourceColumn", rs.getString("COLUMN_NAME"));
                        relation.put("constraintName", rs.getString("CONSTRAINT_NAME"));
                        relation.put("updateRule", rs.getString("UPDATE_RULE"));
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
            String sql = "SELECT " +
                    "kcu.TABLE_NAME as referencing_table, " +
                    "kcu.COLUMN_NAME as referencing_column, " +
                    "kcu.REFERENCED_COLUMN_NAME, " +
                    "rc.CONSTRAINT_NAME, " +
                    "rc.UPDATE_RULE, " +
                    "rc.DELETE_RULE " +
                    "FROM information_schema.KEY_COLUMN_USAGE kcu " +
                    "JOIN information_schema.REFERENTIAL_CONSTRAINTS rc " +
                    "ON kcu.CONSTRAINT_NAME = rc.CONSTRAINT_NAME " +
                    "WHERE kcu.TABLE_SCHEMA = ? " +
                    "AND kcu.REFERENCED_TABLE_NAME = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> relation = new HashMap<>();
                        relation.put("referencingTable", rs.getString("referencing_table"));
                        relation.put("referencingColumn", rs.getString("referencing_column"));
                        relation.put("referencedColumn", rs.getString("REFERENCED_COLUMN_NAME"));
                        relation.put("constraintName", rs.getString("CONSTRAINT_NAME"));
                        relation.put("updateRule", rs.getString("UPDATE_RULE"));
                        relation.put("deleteRule", rs.getString("DELETE_RULE"));
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
            List<Map<String, Object>> columns = listColumns(tableName);

            for (Map<String, Object> column : columns) {
                String columnName = (String) column.get("name");
                String dataType = ((String) column.get("type")).toLowerCase();

                Map<String, Object> columnStats = new HashMap<>();

                // 基本统计
                String basicStatsSql = String.format(
                        "SELECT COUNT(*) as total_count, " +
                                "COUNT(DISTINCT %s) as unique_count, " +
                                "SUM(CASE WHEN %s IS NULL THEN 1 ELSE 0 END) as null_count ",
                        wrapIdentifier(columnName),
                        wrapIdentifier(columnName));

                // 数值类型的额外统计
                if (dataType.matches("int|bigint|decimal|float|double")) {
                    basicStatsSql += String.format(
                            ", MIN(%s) as min_value" +
                                    ", MAX(%s) as max_value" +
                                    ", AVG(%s) as avg_value" +
                                    ", STD(%s) as std_value",
                            wrapIdentifier(columnName),
                            wrapIdentifier(columnName),
                            wrapIdentifier(columnName),
                            wrapIdentifier(columnName));
                }

                basicStatsSql += " FROM " + wrapIdentifier(tableName);

                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(basicStatsSql)) {
                    if (rs.next()) {
                        columnStats.put("totalCount", rs.getLong("total_count"));
                        columnStats.put("uniqueCount", rs.getLong("unique_count"));
                        columnStats.put("nullCount", rs.getLong("null_count"));

                        if (dataType.matches("int|bigint|decimal|float|double")) {
                            columnStats.put("minValue", rs.getObject("min_value"));
                            columnStats.put("maxValue", rs.getObject("max_value"));
                            columnStats.put("avgValue", rs.getDouble("avg_value"));
                            columnStats.put("stdValue", rs.getDouble("std_value"));
                        }
                    }
                }

                statistics.put(columnName, columnStats);
            }
        } catch (Exception e) {
            log.error("获取字段统计信息失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return statistics;
    }

    @Override
    public Map<String, Object> getColumnDistribution(String tableName, String columnName) throws Exception {
        Map<String, Object> distribution = new HashMap<>();
        try (Connection conn = getConnection()) {
            String dataType = null;
            // 获取字段类型
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT DATA_TYPE FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?")) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);
                stmt.setString(3, columnName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        dataType = rs.getString("DATA_TYPE").toLowerCase();
                    }
                }
            }

            if (dataType != null) {
                String sql = String.format(
                        "SELECT COUNT(*) as total_count, " +
                                "COUNT(DISTINCT %s) as unique_count, " +
                                "COUNT(CASE WHEN %s IS NULL THEN 1 END) as null_count",
                        wrapIdentifier(columnName),
                        wrapIdentifier(columnName));

                if (dataType.matches("int|bigint|decimal|float|double")) {
                    sql += String.format(
                            ", MIN(%s) as min_value" +
                                    ", MAX(%s) as max_value" +
                                    ", AVG(%s) as avg_value" +
                                    ", STD(%s) as std_value",
                            wrapIdentifier(columnName),
                            wrapIdentifier(columnName),
                            wrapIdentifier(columnName),
                            wrapIdentifier(columnName));
                }

                sql += " FROM " + wrapIdentifier(tableName);

                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    if (rs.next()) {
                        distribution.put("totalCount", rs.getLong("total_count"));
                        distribution.put("uniqueCount", rs.getLong("unique_count"));
                        distribution.put("nullCount", rs.getLong("null_count"));

                        if (dataType.matches("int|bigint|decimal|float|double")) {
                            distribution.put("minValue", rs.getObject("min_value"));
                            distribution.put("maxValue", rs.getObject("max_value"));
                            distribution.put("avgValue", rs.getDouble("avg_value"));
                            distribution.put("stdValue", rs.getDouble("std_value"));
                        }
                    }
                }

                // 获取频率分布
                String freqSql = String.format(
                        "SELECT %s as value, COUNT(*) as frequency " +
                                "FROM %s " +
                                "WHERE %s IS NOT NULL " +
                                "GROUP BY %s " +
                                "ORDER BY frequency DESC " +
                                "LIMIT 10",
                        wrapIdentifier(columnName),
                        wrapIdentifier(tableName),
                        wrapIdentifier(columnName),
                        wrapIdentifier(columnName));

                List<Map<String, Object>> frequencies = new ArrayList<>();
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(freqSql)) {
                    while (rs.next()) {
                        Map<String, Object> freq = new HashMap<>();
                        freq.put("value", rs.getObject("value"));
                        freq.put("frequency", rs.getLong("frequency"));
                        frequencies.add(freq);
                    }
                }
                distribution.put("frequencies", frequencies);
            }
        } catch (Exception e) {
            log.error("获取字段分布失败: table={}, column={}, error={}",
                    tableName, columnName, e.getMessage(), e);
            throw e;
        }
        return distribution;
    }

    @Override
    public Map<String, Object> getTableUpdateFrequency(String tableName) throws Exception {
        Map<String, Object> frequency = new HashMap<>();
        try (Connection conn = getConnection()) {
            String sql = "SELECT " +
                    "COUNT(*) as total_updates, " +
                    "MIN(UPDATE_TIME) as first_update, " +
                    "MAX(UPDATE_TIME) as last_update " +
                    "FROM information_schema.TABLES " +
                    "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        frequency.put("totalUpdates", rs.getLong("total_updates"));
                        frequency.put("firstUpdate", rs.getTimestamp("first_update"));
                        frequency.put("lastUpdate", rs.getTimestamp("last_update"));

                        // 计算平均更新间隔
                        java.sql.Timestamp firstUpdate = rs.getTimestamp("first_update");
                        java.sql.Timestamp lastUpdate = rs.getTimestamp("last_update");
                        if (firstUpdate != null && lastUpdate != null) {
                            long updateCount = rs.getLong("total_updates");
                            if (updateCount > 1) {
                                long intervalMs = (lastUpdate.getTime() - firstUpdate.getTime()) / (updateCount - 1);
                                frequency.put("avgUpdateInterval", intervalMs);
                            }
                        }
                    }
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
        List<Map<String, Object>> trend = new ArrayList<>();
        try (Connection conn = getConnection()) {
            // 创建临时表记录每天的数据量
            String createTempTableSql = String.format(
                    "CREATE TEMPORARY TABLE IF NOT EXISTS temp_growth_trend (" +
                            "record_date DATE, " +
                            "row_count BIGINT)");

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createTempTableSql);

                // 插入每天的数据量
                String insertSql = String.format(
                        "INSERT INTO temp_growth_trend " +
                                "SELECT DATE(create_time) as record_date, COUNT(*) as row_count " +
                                "FROM %s " +
                                "WHERE create_time >= DATE_SUB(CURRENT_DATE, INTERVAL ? DAY) " +
                                "GROUP BY DATE(create_time) " +
                                "ORDER BY record_date",
                        wrapIdentifier(tableName));

                try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                    pstmt.setInt(1, days);
                    pstmt.executeUpdate();
                }

                // 查询增长趋势
                String trendSql =
                        "SELECT record_date, row_count, " +
                                "row_count - LAG(row_count) OVER (ORDER BY record_date) as daily_increase " +
                                "FROM temp_growth_trend " +
                                "ORDER BY record_date";

                try (ResultSet rs = stmt.executeQuery(trendSql)) {
                    while (rs.next()) {
                        Map<String, Object> daily = new HashMap<>();
                        daily.put("date", rs.getDate("record_date"));
                        daily.put("rowCount", rs.getLong("row_count"));
                        daily.put("increase", rs.getObject("daily_increase"));
                        trend.add(daily);
                    }
                }

                // 删除临时表
                stmt.execute("DROP TEMPORARY TABLE IF EXISTS temp_growth_trend");
            }
        } catch (Exception e) {
            log.error("获取表增长趋势失败: table={}, days={}, error={}",
                    tableName, days, e.getMessage(), e);
            throw e;
        }
        return trend;
    }

    @Override
    public List<Map<String, Object>> getTableDataSample(String tableName, int sampleSize) throws Exception {
        List<Map<String, Object>> samples = new ArrayList<>();
        try (Connection conn = getConnection()) {
            // 获取表的总行数
            long totalRows = getTableRowCount(tableName);
            if (totalRows == 0) {
                return samples;
            }

            // 如果样本大小大于总行数，直接返回所有数据
            if (sampleSize >= totalRows) {
                String sql = String.format("SELECT * FROM %s", wrapIdentifier(tableName));
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                            row.put(rs.getMetaData().getColumnName(i), rs.getObject(i));
                        }
                        samples.add(row);
                    }
                }
            } else {
                // 使用RAND()函数随机抽样
                String sql = String.format(
                        "SELECT * FROM %s ORDER BY RAND() LIMIT ?",
                        wrapIdentifier(tableName));

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, sampleSize);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            Map<String, Object> row = new HashMap<>();
                            for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                                row.put(rs.getMetaData().getColumnName(i), rs.getObject(i));
                            }
                            samples.add(row);
                        }
                    }
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
        try (Connection conn = getConnection()) {
            // 获取字段类型
            String dataType = null;
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT DATA_TYPE FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?")) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);
                stmt.setString(3, columnName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        dataType = rs.getString("DATA_TYPE").toLowerCase();
                    }
                }
            }

            if (dataType != null) {
                String sql = null;
                switch (dataType) {
                    case "int":
                    case "bigint":
                    case "decimal":
                    case "float":
                    case "double":
                        sql = String.format(
                                "SELECT MIN(%s) as min_value, " +
                                        "MAX(%s) as max_value, " +
                                        "AVG(%s) as avg_value, " +
                                        "STD(%s) as std_value, " +
                                        "VARIANCE(%s) as variance " +
                                        "FROM %s " +
                                        "WHERE %s IS NOT NULL",
                                wrapIdentifier(columnName),
                                wrapIdentifier(columnName),
                                wrapIdentifier(columnName),
                                wrapIdentifier(columnName),
                                wrapIdentifier(columnName),
                                wrapIdentifier(tableName),
                                wrapIdentifier(columnName));
                        break;

                    case "date":
                    case "datetime":
                    case "timestamp":
                        sql = String.format(
                                "SELECT MIN(%s) as min_value, " +
                                        "MAX(%s) as max_value " +
                                        "FROM %s " +
                                        "WHERE %s IS NOT NULL",
                                wrapIdentifier(columnName),
                                wrapIdentifier(columnName),
                                wrapIdentifier(tableName),
                                wrapIdentifier(columnName));
                        break;

                    case "varchar":
                    case "char":
                    case "text":
                        sql = String.format(
                                "SELECT MIN(%s) as min_value, " +
                                        "MAX(%s) as max_value, " +
                                        "AVG(LENGTH(%s)) as avg_length, " +
                                        "MIN(LENGTH(%s)) as min_length, " +
                                        "MAX(LENGTH(%s)) as max_length " +
                                        "FROM %s " +
                                        "WHERE %s IS NOT NULL",
                                wrapIdentifier(columnName),
                                wrapIdentifier(columnName),
                                wrapIdentifier(columnName),
                                wrapIdentifier(columnName),
                                wrapIdentifier(columnName),
                                wrapIdentifier(tableName),
                                wrapIdentifier(columnName));
                        break;
                }

                if (sql != null) {
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(sql)) {
                        if (rs.next()) {
                            range.put("minValue", rs.getObject("min_value"));
                            range.put("maxValue", rs.getObject("max_value"));

                            if (dataType.matches("int|bigint|decimal|float|double")) {
                                range.put("avgValue", rs.getDouble("avg_value"));
                                range.put("stdValue", rs.getDouble("std_value"));
                                range.put("variance", rs.getDouble("variance"));
                            } else if (dataType.matches("varchar|char|text")) {
                                range.put("avgLength", rs.getDouble("avg_length"));
                                range.put("minLength", rs.getInt("min_length"));
                                range.put("maxLength", rs.getInt("max_length"));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取字段值域范围失败: table={}, column={}, error={}",
                    tableName, columnName, e.getMessage(), e);
            throw e;
        }
        return range;
    }

    @Override
    public List<Map<String, Object>> getColumnValueDistribution(String tableName, String columnName, int topN) throws Exception {
        List<Map<String, Object>> distribution = new ArrayList<>();
        try (Connection conn = getConnection()) {
            // 获取字段类型
            String dataType = null;
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT DATA_TYPE FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?")) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);
                stmt.setString(3, columnName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        dataType = rs.getString("DATA_TYPE").toLowerCase();
                    }
                }
            }

            if (dataType != null) {
                String sql = null;
                if (dataType.matches("int|bigint|decimal|float|double")) {
                    // 数值型数据，按区间统计
                    sql = String.format(
                            "WITH stats AS (" +
                                    "  SELECT MIN(%s) as min_val, MAX(%s) as max_val " +
                                    "  FROM %s WHERE %s IS NOT NULL" +
                                    "), ranges AS (" +
                                    "  SELECT min_val + (max_val - min_val) * (n - 1) / ? as range_start, " +
                                    "         min_val + (max_val - min_val) * n / ? as range_end " +
                                    "  FROM stats, (SELECT n FROM (SELECT 1 as n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) t) nums" +
                                    ") " +
                                    "SELECT CONCAT(range_start, ' - ', range_end) as value_range, " +
                                    "COUNT(*) as frequency " +
                                    "FROM %s, ranges " +
                                    "WHERE %s >= range_start AND %s < range_end " +
                                    "GROUP BY value_range " +
                                    "ORDER BY range_start",
                            wrapIdentifier(columnName),
                            wrapIdentifier(columnName),
                            wrapIdentifier(tableName),
                            wrapIdentifier(columnName),
                            topN,
                            topN,
                            wrapIdentifier(tableName),
                            wrapIdentifier(columnName),
                            wrapIdentifier(columnName));
                } else if (dataType.matches("date|datetime|timestamp")) {
                    // 时间类型数据，按时间区间统计
                    sql = String.format(
                            "SELECT DATE_FORMAT(%s, '%%Y-%%m') as time_period, " +
                                    "COUNT(*) as frequency " +
                                    "FROM %s " +
                                    "WHERE %s IS NOT NULL " +
                                    "GROUP BY time_period " +
                                    "ORDER BY time_period DESC " +
                                    "LIMIT ?",
                            wrapIdentifier(columnName),
                            wrapIdentifier(tableName),
                            wrapIdentifier(columnName));
                } else {
                    // 其他类型，直接统计频率
                    sql = String.format(
                            "SELECT %s as value, COUNT(*) as frequency " +
                                    "FROM %s " +
                                    "WHERE %s IS NOT NULL " +
                                    "GROUP BY %s " +
                                    "ORDER BY frequency DESC " +
                                    "LIMIT ?",
                            wrapIdentifier(columnName),
                            wrapIdentifier(tableName),
                            wrapIdentifier(columnName),
                            wrapIdentifier(columnName));
                }

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    if (!dataType.matches("int|bigint|decimal|float|double")) {
                        stmt.setInt(1, topN);
                    }

                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            Map<String, Object> item = new HashMap<>();
                            if (dataType.matches("date|datetime|timestamp")) {
                                item.put("timePeriod", rs.getString("time_period"));
                            } else if (dataType.matches("int|bigint|decimal|float|double")) {
                                item.put("valueRange", rs.getString("value_range"));
                            } else {
                                item.put("value", rs.getObject("value"));
                            }
                            item.put("frequency", rs.getLong("frequency"));
                            distribution.add(item);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取字段值分布失败: table={}, column={}, topN={}, error={}",
                    tableName, columnName, topN, e.getMessage(), e);
            throw e;
        }
        return distribution;
    }

    public List<Map<String, Object>> getTablePartitions(String tableName) throws Exception {
        List<Map<String, Object>> partitions = new ArrayList<>();
        try (Connection conn = getConnection()) {
            String sql = "SELECT PARTITION_NAME, PARTITION_METHOD, PARTITION_EXPRESSION, " +
                    "PARTITION_DESCRIPTION, TABLE_ROWS, DATA_LENGTH " +
                    "FROM information_schema.PARTITIONS " +
                    "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? " +
                    "AND PARTITION_NAME IS NOT NULL";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> partition = new HashMap<>();
                        partition.put("name", rs.getString("PARTITION_NAME"));
                        partition.put("method", rs.getString("PARTITION_METHOD"));
                        partition.put("expression", rs.getString("PARTITION_EXPRESSION"));
                        partition.put("description", rs.getString("PARTITION_DESCRIPTION"));
                        partition.put("rows", rs.getLong("TABLE_ROWS"));
                        partition.put("dataLength", rs.getLong("DATA_LENGTH"));
                        partitions.add(partition);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取表分区信息失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return partitions;
    }

    public List<Map<String, Object>> getTableTriggers(String tableName) throws Exception {
        List<Map<String, Object>> triggers = new ArrayList<>();
        try (Connection conn = getConnection()) {
            String sql = "SELECT TRIGGER_NAME, EVENT_MANIPULATION, EVENT_OBJECT_TABLE, " +
                    "ACTION_STATEMENT, ACTION_TIMING " +
                    "FROM information_schema.TRIGGERS " +
                    "WHERE EVENT_OBJECT_SCHEMA = ? AND EVENT_OBJECT_TABLE = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> trigger = new HashMap<>();
                        trigger.put("name", rs.getString("TRIGGER_NAME"));
                        trigger.put("event", rs.getString("EVENT_MANIPULATION"));
                        trigger.put("table", rs.getString("EVENT_OBJECT_TABLE"));
                        trigger.put("statement", rs.getString("ACTION_STATEMENT"));
                        trigger.put("timing", rs.getString("ACTION_TIMING"));
                        triggers.add(trigger);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取表触发器失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return triggers;
    }

    public List<Map<String, Object>> getTableConstraints(String tableName) throws Exception {
        List<Map<String, Object>> constraints = new ArrayList<>();
        try (Connection conn = getConnection()) {
            String sql = "SELECT CONSTRAINT_NAME, CONSTRAINT_TYPE, " +
                    "TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, " +
                    "REFERENCED_COLUMN_NAME " +
                    "FROM information_schema.KEY_COLUMN_USAGE kcu " +
                    "JOIN information_schema.TABLE_CONSTRAINTS tc " +
                    "ON kcu.CONSTRAINT_NAME = tc.CONSTRAINT_NAME " +
                    "WHERE kcu.TABLE_SCHEMA = ? AND kcu.TABLE_NAME = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> constraint = new HashMap<>();
                        constraint.put("name", rs.getString("CONSTRAINT_NAME"));
                        constraint.put("type", rs.getString("CONSTRAINT_TYPE"));
                        constraint.put("table", rs.getString("TABLE_NAME"));
                        constraint.put("column", rs.getString("COLUMN_NAME"));
                        constraint.put("referencedTable", rs.getString("REFERENCED_TABLE_NAME"));
                        constraint.put("referencedColumn", rs.getString("REFERENCED_COLUMN_NAME"));
                        constraints.add(constraint);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取表约束失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return constraints;
    }

    public List<Map<String, Object>> getTablePrivileges(String tableName) throws Exception {
        List<Map<String, Object>> privileges = new ArrayList<>();
        try (Connection conn = getConnection()) {
            String sql = "SELECT GRANTEE, PRIVILEGE_TYPE, IS_GRANTABLE " +
                    "FROM information_schema.TABLE_PRIVILEGES " +
                    "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> privilege = new HashMap<>();
                        privilege.put("grantee", rs.getString("GRANTEE"));
                        privilege.put("privilegeType", rs.getString("PRIVILEGE_TYPE"));
                        privilege.put("isGrantable", "YES".equals(rs.getString("IS_GRANTABLE")));
                        privileges.add(privilege);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取表权限失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return privileges;
    }

    public Map<String, Object> getTableStorageParameters(String tableName) throws Exception {
        Map<String, Object> params = new HashMap<>();
        try (Connection conn = getConnection()) {
            String sql = "SELECT ENGINE, ROW_FORMAT, TABLE_COLLATION, " +
                    "CREATE_OPTIONS, TABLE_COMMENT " +
                    "FROM information_schema.TABLES " +
                    "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        params.put("engine", rs.getString("ENGINE"));
                        params.put("rowFormat", rs.getString("ROW_FORMAT"));
                        params.put("collation", rs.getString("TABLE_COLLATION"));
                        params.put("createOptions", rs.getString("CREATE_OPTIONS"));
                        params.put("comment", rs.getString("TABLE_COMMENT"));
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取表存储参数失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return params;
    }

    @Override
    public List<Map<String, Object>> getTableDependencies(String tableName) throws Exception {
        List<Map<String, Object>> dependencies = new ArrayList<>();
        try (Connection conn = getConnection()) {
            // 1. 获取视图依赖
            String viewSql = "SELECT DISTINCT " +
                    "TABLE_NAME as dependent_object, " +
                    "'VIEW' as object_type, " +
                    "VIEW_DEFINITION as dependency_definition " +
                    "FROM information_schema.VIEWS " +
                    "WHERE TABLE_SCHEMA = ? " +
                    "AND VIEW_DEFINITION LIKE ?";

            try (PreparedStatement stmt = conn.prepareStatement(viewSql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, "%" + tableName + "%");

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
            }

            // 2. 获取外键依赖（被其他表引用）
            String fkSql = "SELECT DISTINCT " +
                    "kcu.TABLE_NAME as dependent_object, " +
                    "'TABLE' as object_type, " +
                    "kcu.CONSTRAINT_NAME as constraint_name, " +
                    "kcu.COLUMN_NAME as source_column, " +
                    "kcu.REFERENCED_COLUMN_NAME as referenced_column, " +
                    "rc.UPDATE_RULE, " +
                    "rc.DELETE_RULE " +
                    "FROM information_schema.KEY_COLUMN_USAGE kcu " +
                    "JOIN information_schema.REFERENTIAL_CONSTRAINTS rc " +
                    "ON kcu.CONSTRAINT_NAME = rc.CONSTRAINT_NAME " +
                    "WHERE kcu.REFERENCED_TABLE_SCHEMA = ? " +
                    "AND kcu.REFERENCED_TABLE_NAME = ?";

            try (PreparedStatement stmt = conn.prepareStatement(fkSql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> dependency = new HashMap<>();
                        dependency.put("name", rs.getString("dependent_object"));
                        dependency.put("type", rs.getString("object_type"));
                        dependency.put("constraintName", rs.getString("constraint_name"));
                        dependency.put("sourceColumn", rs.getString("source_column"));
                        dependency.put("referencedColumn", rs.getString("referenced_column"));
                        dependency.put("updateRule", rs.getString("UPDATE_RULE"));
                        dependency.put("deleteRule", rs.getString("DELETE_RULE"));
                        dependency.put("dependencyType", "Referenced by Foreign Key");
                        dependencies.add(dependency);
                    }
                }
            }

            // 3. 获取触发器依赖
            String triggerSql = "SELECT DISTINCT " +
                    "TRIGGER_NAME as dependent_object, " +
                    "'TRIGGER' as object_type, " +
                    "ACTION_STATEMENT as trigger_definition, " +
                    "EVENT_MANIPULATION as trigger_event, " +
                    "ACTION_TIMING as trigger_timing " +
                    "FROM information_schema.TRIGGERS " +
                    "WHERE EVENT_OBJECT_SCHEMA = ? " +
                    "AND ACTION_STATEMENT LIKE ?";

            try (PreparedStatement stmt = conn.prepareStatement(triggerSql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, "%" + tableName + "%");

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> dependency = new HashMap<>();
                        dependency.put("name", rs.getString("dependent_object"));
                        dependency.put("type", rs.getString("object_type"));
                        dependency.put("definition", rs.getString("trigger_definition"));
                        dependency.put("event", rs.getString("trigger_event"));
                        dependency.put("timing", rs.getString("trigger_timing"));
                        dependency.put("dependencyType", "Referenced by Trigger");
                        dependencies.add(dependency);
                    }
                }
            }

            // 4. 获取存储过程和函数依赖
            String routineSql = "SELECT DISTINCT " +
                    "ROUTINE_NAME as dependent_object, " +
                    "ROUTINE_TYPE as object_type, " +
                    "ROUTINE_DEFINITION as routine_definition " +
                    "FROM information_schema.ROUTINES " +
                    "WHERE ROUTINE_SCHEMA = ? " +
                    "AND ROUTINE_DEFINITION LIKE ?";

            try (PreparedStatement stmt = conn.prepareStatement(routineSql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, "%" + tableName + "%");

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> dependency = new HashMap<>();
                        dependency.put("name", rs.getString("dependent_object"));
                        dependency.put("type", rs.getString("object_type"));
                        dependency.put("definition", rs.getString("routine_definition"));
                        dependency.put("dependencyType", "Referenced by " + rs.getString("object_type"));
                        dependencies.add(dependency);
                    }
                }
            }

            // 5. 获取事件依赖
            String eventSql = "SELECT DISTINCT " +
                    "EVENT_NAME as dependent_object, " +
                    "'EVENT' as object_type, " +
                    "EVENT_DEFINITION as event_definition " +
                    "FROM information_schema.EVENTS " +
                    "WHERE EVENT_SCHEMA = ? " +
                    "AND EVENT_DEFINITION LIKE ?";

            try (PreparedStatement stmt = conn.prepareStatement(eventSql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, "%" + tableName + "%");

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> dependency = new HashMap<>();
                        dependency.put("name", rs.getString("dependent_object"));
                        dependency.put("type", rs.getString("object_type"));
                        dependency.put("definition", rs.getString("event_definition"));
                        dependency.put("dependencyType", "Referenced by Event");
                        dependencies.add(dependency);
                    }
                }
            }

            // 6. 获取表的外键依赖（引用其他表）
            String referencedSql = "SELECT DISTINCT " +
                    "kcu.REFERENCED_TABLE_NAME as dependent_object, " +
                    "'TABLE' as object_type, " +
                    "kcu.CONSTRAINT_NAME as constraint_name, " +
                    "kcu.COLUMN_NAME as source_column, " +
                    "kcu.REFERENCED_COLUMN_NAME as referenced_column, " +
                    "rc.UPDATE_RULE, " +
                    "rc.DELETE_RULE " +
                    "FROM information_schema.KEY_COLUMN_USAGE kcu " +
                    "JOIN information_schema.REFERENTIAL_CONSTRAINTS rc " +
                    "ON kcu.CONSTRAINT_NAME = rc.CONSTRAINT_NAME " +
                    "WHERE kcu.TABLE_SCHEMA = ? " +
                    "AND kcu.TABLE_NAME = ? " +
                    "AND kcu.REFERENCED_TABLE_NAME IS NOT NULL";

            try (PreparedStatement stmt = conn.prepareStatement(referencedSql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> dependency = new HashMap<>();
                        dependency.put("name", rs.getString("dependent_object"));
                        dependency.put("type", rs.getString("object_type"));
                        dependency.put("constraintName", rs.getString("constraint_name"));
                        dependency.put("sourceColumn", rs.getString("source_column"));
                        dependency.put("referencedColumn", rs.getString("referenced_column"));
                        dependency.put("updateRule", rs.getString("UPDATE_RULE"));
                        dependency.put("deleteRule", rs.getString("DELETE_RULE"));
                        dependency.put("dependencyType", "References Table");
                        dependencies.add(dependency);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取表依赖关系失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return dependencies;
    }

    @Override
    public String getDatabaseType() {
        return "MySQL";
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
                     "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES " +
                             "WHERE TABLE_SCHEMA = ? AND TABLE_TYPE = 'BASE TABLE'")) {
            stmt.setString(1, getDatabaseName());
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
        Map<String, Object> info = new HashMap<>();
        try (Connection conn = getConnection()) {
            // 获取表的基本信息
            String sql = "SELECT TABLE_NAME, ENGINE, TABLE_ROWS, AVG_ROW_LENGTH, DATA_LENGTH, " +
                    "INDEX_LENGTH, AUTO_INCREMENT, CREATE_TIME, UPDATE_TIME, TABLE_COLLATION, " +
                    "TABLE_COMMENT " +
                    "FROM INFORMATION_SCHEMA.TABLES " +
                    "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getDatabaseName());
                stmt.setString(2, tableName);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        info.put("name", rs.getString("TABLE_NAME"));
                        info.put("engine", rs.getString("ENGINE"));
                        info.put("rowCount", rs.getLong("TABLE_ROWS"));
                        info.put("avgRowLength", rs.getLong("AVG_ROW_LENGTH"));
                        info.put("dataLength", rs.getLong("DATA_LENGTH"));
                        info.put("indexLength", rs.getLong("INDEX_LENGTH"));
                        info.put("autoIncrement", rs.getLong("AUTO_INCREMENT"));
                        info.put("createTime", rs.getTimestamp("CREATE_TIME"));
                        info.put("updateTime", rs.getTimestamp("UPDATE_TIME"));
                        info.put("collation", rs.getString("TABLE_COLLATION"));
                        info.put("comment", rs.getString("TABLE_COMMENT"));

                        // 计算总大小
                        long totalSize = rs.getLong("DATA_LENGTH") + rs.getLong("INDEX_LENGTH");
                        info.put("totalSize", totalSize);

                        // 提取字符集
                        String collation = rs.getString("TABLE_COLLATION");
                        if (collation != null && collation.contains("_")) {
                            String charset = collation.substring(0, collation.indexOf('_'));
                            info.put("charset", charset);
                        }
                    }
                }
            }

            // 获取表的列数
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(*) as column_count FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?")) {
                stmt.setString(1, getDatabaseName());
                stmt.setString(2, tableName);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        info.put("columnCount", rs.getInt("column_count"));
                    }
                }
            }

            // 获取表的索引数
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(DISTINCT INDEX_NAME) as index_count " +
                            "FROM INFORMATION_SCHEMA.STATISTICS " +
                            "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?")) {
                stmt.setString(1, getDatabaseName());
                stmt.setString(2, tableName);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        info.put("indexCount", rs.getInt("index_count"));
                    }
                }
            }

            // 获取表的主键信息
            List<String> primaryKeys = getPrimaryKeys(tableName);
            if (!primaryKeys.isEmpty()) {
                info.put("primaryKey", primaryKeys);
                info.put("hasPrimaryKey", true);
            } else {
                info.put("hasPrimaryKey", false);
            }

            // 获取表的外键数
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(*) as fk_count " +
                            "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                            "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? " +
                            "AND REFERENCED_TABLE_NAME IS NOT NULL")) {
                stmt.setString(1, getDatabaseName());
                stmt.setString(2, tableName);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        info.put("foreignKeyCount", rs.getInt("fk_count"));
                        info.put("hasForeignKeys", rs.getInt("fk_count") > 0);
                    }
                }
            }

            // 获取表的碎片信息
            Map<String, Object> fragmentation = getTableFragmentation(tableName);
            if (fragmentation != null && !fragmentation.isEmpty()) {
                info.put("fragmentation", fragmentation);
            }

        } catch (Exception e) {
            log.error("获取表信息失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return info;
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
                String dataType = ((String) column.get("type")).toLowerCase();

                // 根据数据类型检查质量问题
                String sql = null;
                switch (dataType) {
                    case "int":
                    case "bigint":
                        sql = String.format(
                                "SELECT COUNT(*) as issue_count " +
                                        "FROM %s " +
                                        "WHERE %s IS NOT NULL AND %s NOT REGEXP '^-?[0-9]+$'",
                                wrapIdentifier(tableName),
                                wrapIdentifier(columnName),
                                wrapIdentifier(columnName));
                        break;

                    case "decimal":
                    case "float":
                    case "double":
                        sql = String.format(
                                "SELECT COUNT(*) as issue_count " +
                                        "FROM %s " +
                                        "WHERE %s IS NOT NULL AND %s NOT REGEXP '^-?[0-9]*.?[0-9]+$'",
                                wrapIdentifier(tableName),
                                wrapIdentifier(columnName),
                                wrapIdentifier(columnName));
                        break;

                    case "date":
                        sql = String.format(
                                "SELECT COUNT(*) as issue_count " +
                                        "FROM %s " +
                                        "WHERE %s IS NOT NULL AND STR_TO_DATE(%s, '%%Y-%%m-%%d') IS NULL",
                                wrapIdentifier(tableName),
                                wrapIdentifier(columnName),
                                wrapIdentifier(columnName));
                        break;

                    case "datetime":
                    case "timestamp":
                        sql = String.format(
                                "SELECT COUNT(*) as issue_count " +
                                        "FROM %s " +
                                        "WHERE %s IS NOT NULL AND STR_TO_DATE(%s, '%%Y-%%m-%%d %%H:%%i:%%s') IS NULL",
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

    public Map<String, Object> getTableExecutionPlan(String tableName) throws Exception {
        Map<String, Object> plan = new HashMap<>();
        try (Connection conn = getConnection()) {
            // 生成一个简单的查询计划
            String sql = "EXPLAIN FORMAT=JSON SELECT * FROM " + wrapIdentifier(tableName) + " LIMIT 1";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    String jsonPlan = rs.getString(1);
                    plan.put("executionPlan", jsonPlan);
                }
            }

            // 获取表的统计信息
            String statsSql = "SHOW TABLE STATUS WHERE Name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(statsSql)) {
                stmt.setString(1, tableName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        plan.put("rows", rs.getLong("Rows"));
                        plan.put("avgRowLength", rs.getLong("Avg_row_length"));
                        plan.put("dataLength", rs.getLong("Data_length"));
                        plan.put("indexLength", rs.getLong("Index_length"));
                        plan.put("engine", rs.getString("Engine"));
                        plan.put("createTime", rs.getTimestamp("Create_time"));
                        plan.put("updateTime", rs.getTimestamp("Update_time"));
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取表执行计划失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return plan;
    }

    public List<Map<String, Object>> getTableLocks(String tableName) throws Exception {
        List<Map<String, Object>> locks = new ArrayList<>();
        try (Connection conn = getConnection()) {
            // 查询当前表的锁信息
            String sql = "SELECT * FROM information_schema.INNODB_LOCKS " +
                    "WHERE lock_table = CONCAT(?, '.', ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> lock = new HashMap<>();
                        lock.put("lockId", rs.getString("lock_id"));
                        lock.put("lockMode", rs.getString("lock_mode"));
                        lock.put("lockType", rs.getString("lock_type"));
                        lock.put("lockSpace", rs.getString("lock_space"));
                        lock.put("lockPage", rs.getString("lock_page"));
                        lock.put("lockRec", rs.getString("lock_rec"));
                        lock.put("lockData", rs.getString("lock_data"));
                        locks.add(lock);
                    }
                }
            } catch (SQLException e) {
                // 如果表不存在或者MySQL版本不支持INNODB_LOCKS视图，尝试使用其他方式
                if (e.getMessage().contains("doesn't exist") || e.getMessage().contains("Unknown table")) {
                    String alternateSql = "SELECT * FROM performance_schema.data_locks " +
                            "WHERE OBJECT_SCHEMA = ? AND OBJECT_NAME = ?";

                    try (PreparedStatement stmt = conn.prepareStatement(alternateSql)) {
                        stmt.setString(1, getSchema());
                        stmt.setString(2, tableName);

                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                Map<String, Object> lock = new HashMap<>();
                                lock.put("lockId", rs.getString("ENGINE_LOCK_ID"));
                                lock.put("lockMode", rs.getString("LOCK_MODE"));
                                lock.put("lockType", rs.getString("LOCK_TYPE"));
                                lock.put("lockStatus", rs.getString("LOCK_STATUS"));
                                lock.put("lockData", rs.getString("LOCK_DATA"));
                                locks.add(lock);
                            }
                        }
                    } catch (SQLException ex) {
                        // 如果还是失败，尝试使用SHOW PROCESSLIST
                        String processSql = "SHOW PROCESSLIST";
                        try (Statement stmt = conn.createStatement();
                             ResultSet rs = stmt.executeQuery(processSql)) {
                            while (rs.next()) {
                                String info = rs.getString("Info");
                                if (info != null && info.contains(tableName)) {
                                    Map<String, Object> lock = new HashMap<>();
                                    lock.put("processId", rs.getLong("Id"));
                                    lock.put("user", rs.getString("User"));
                                    lock.put("host", rs.getString("Host"));
                                    lock.put("db", rs.getString("db"));
                                    lock.put("command", rs.getString("Command"));
                                    lock.put("time", rs.getLong("Time"));
                                    lock.put("state", rs.getString("State"));
                                    lock.put("info", info);
                                    locks.add(lock);
                                }
                            }
                        }
                    }
                } else {
                    throw e;
                }
            }
        } catch (Exception e) {
            log.error("获取表锁信息失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return locks;
    }

    public Date getDate() throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT NOW() as current_date")) {
            if (rs.next()) {
                return rs.getTimestamp("current_date");
            }
        } catch (Exception e) {
            log.error("获取数据库日期失败: error={}", e.getMessage(), e);
            throw e;
        }
        return new Date();
    }

    public Map<String, Object> checkTableConsistency(String tableName) throws Exception {
        Map<String, Object> result = new HashMap<>();
        try (Connection conn = getConnection()) {
            // 检查表状态
            String checkSql = "CHECK TABLE " + wrapIdentifier(tableName);
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(checkSql)) {
                if (rs.next()) {
                    result.put("msg_type", rs.getString("Msg_type"));
                    result.put("msg_text", rs.getString("Msg_text"));
                    result.put("status", rs.getString("Msg_text").contains("OK") ? "OK" : "Error");
                }
            }

            // 检查主键一致性
            String pkSql = "SELECT COUNT(*) as total_count, " +
                    "COUNT(DISTINCT pk_columns) as unique_count " +
                    "FROM (SELECT CONCAT_WS(',', pk_columns) as pk_columns " +
                    "FROM information_schema.KEY_COLUMN_USAGE " +
                    "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? " +
                    "AND CONSTRAINT_NAME = 'PRIMARY') t";

            try (PreparedStatement stmt = conn.prepareStatement(pkSql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        result.put("primaryKeyConsistency",
                                rs.getLong("unique_count") == rs.getLong("total_count"));
                    }
                }
            }

            // 检查外键一致性
            String fkSql = "SELECT COUNT(*) as violation_count " +
                    "FROM information_schema.REFERENTIAL_CONSTRAINTS rc " +
                    "JOIN information_schema.KEY_COLUMN_USAGE kcu " +
                    "ON rc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME " +
                    "WHERE rc.CONSTRAINT_SCHEMA = ? AND rc.TABLE_NAME = ?";

            try (PreparedStatement stmt = conn.prepareStatement(fkSql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        result.put("foreignKeyConsistency",
                                rs.getLong("violation_count") == 0);
                    }
                }
            }

        } catch (Exception e) {
            log.error("检查表一致性失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return result;
    }

    public Map<String, Object> getTableFragmentation(String tableName) throws Exception {
        Map<String, Object> fragmentation = new HashMap<>();
        try (Connection conn = getConnection()) {
            String sql = "SELECT DATA_FREE, DATA_LENGTH, INDEX_LENGTH " +
                    "FROM information_schema.TABLES " +
                    "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        long dataFree = rs.getLong("DATA_FREE");
                        long dataLength = rs.getLong("DATA_LENGTH");
                        long indexLength = rs.getLong("INDEX_LENGTH");
                        long totalSpace = dataLength + indexLength;

                        fragmentation.put("dataFree", dataFree);
                        fragmentation.put("dataLength", dataLength);
                        fragmentation.put("indexLength", indexLength);
                        fragmentation.put("totalSpace", totalSpace);

                        if (totalSpace > 0) {
                            double fragmentationRatio = (double) dataFree / totalSpace;
                            fragmentation.put("fragmentationRatio", fragmentationRatio);

                            // 添加碎片化评估
                            if (fragmentationRatio < 0.1) {
                                fragmentation.put("fragmentationLevel", "Low");
                            } else if (fragmentationRatio < 0.3) {
                                fragmentation.put("fragmentationLevel", "Medium");
                            } else {
                                fragmentation.put("fragmentationLevel", "High");
                            }

                            // 添加优化建议
                            if (fragmentationRatio > 0.2) {
                                fragmentation.put("optimizationRecommended", true);
                                fragmentation.put("optimizationSql", "OPTIMIZE TABLE " + wrapIdentifier(tableName));
                            } else {
                                fragmentation.put("optimizationRecommended", false);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取表碎片信息失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return fragmentation;
    }

    @Override
    public Map<String, Integer> getQualityIssueCount(String tableName) throws Exception {
        Map<String, Integer> counts = new HashMap<>();
        try (Connection conn = getConnection()) {
            // 获取所有列
            List<Map<String, Object>> columns = listColumns(tableName);

            for (Map<String, Object> column : columns) {
                String columnName = (String) column.get("name");
                String validationSql = getValidationSql(tableName, columnName);
                if (validationSql != null) {
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(validationSql)) {
                        if (rs.next()) {
                            counts.put(columnName, rs.getInt("invalid_rows"));
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
    public List<Map<String, Object>> getIndexes(String tableName) throws Exception {
        List<Map<String, Object>> indexes = new ArrayList<>();
        try (Connection conn = getConnection()) {
            String sql = "SELECT " +
                    "INDEX_NAME, " +
                    "COLUMN_NAME, " +
                    "SEQ_IN_INDEX, " +
                    "NON_UNIQUE, " +
                    "INDEX_TYPE, " +
                    "COMMENT " +
                    "FROM information_schema.STATISTICS " +
                    "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? " +
                    "ORDER BY INDEX_NAME, SEQ_IN_INDEX";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);

                Map<String, Map<String, Object>> indexMap = new HashMap<>();

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String indexName = rs.getString("INDEX_NAME");
                        String columnName = rs.getString("COLUMN_NAME");
                        int seqInIndex = rs.getInt("SEQ_IN_INDEX");
                        boolean nonUnique = rs.getBoolean("NON_UNIQUE");
                        String indexType = rs.getString("INDEX_TYPE");
                        String comment = rs.getString("COMMENT");

                        Map<String, Object> indexInfo = indexMap.get(indexName);
                        if (indexInfo == null) {
                            indexInfo = new HashMap<>();
                            indexInfo.put("name", indexName);
                            indexInfo.put("unique", !nonUnique);
                            indexInfo.put("type", indexType);
                            indexInfo.put("comment", comment);
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
    public List<String> getPrimaryKeys(String tableName) throws Exception {
        List<String> primaryKeys = new ArrayList<>();
        try (Connection conn = getConnection()) {
            String sql = "SELECT COLUMN_NAME " +
                    "FROM information_schema.KEY_COLUMN_USAGE " +
                    "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? " +
                    "AND CONSTRAINT_NAME = 'PRIMARY' " +
                    "ORDER BY ORDINAL_POSITION";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        primaryKeys.add(rs.getString("COLUMN_NAME"));
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
        List<Map<String, Object>> foreignKeys = new ArrayList<>();
        try (Connection conn = getConnection()) {
            String sql = "SELECT " +
                    "kcu.CONSTRAINT_NAME, " +
                    "kcu.COLUMN_NAME, " +
                    "kcu.REFERENCED_TABLE_NAME, " +
                    "kcu.REFERENCED_COLUMN_NAME, " +
                    "rc.UPDATE_RULE, " +
                    "rc.DELETE_RULE " +
                    "FROM information_schema.KEY_COLUMN_USAGE kcu " +
                    "JOIN information_schema.REFERENTIAL_CONSTRAINTS rc " +
                    "ON kcu.CONSTRAINT_NAME = rc.CONSTRAINT_NAME " +
                    "WHERE kcu.TABLE_SCHEMA = ? AND kcu.TABLE_NAME = ? " +
                    "AND kcu.REFERENCED_TABLE_NAME IS NOT NULL " +
                    "ORDER BY kcu.CONSTRAINT_NAME, kcu.ORDINAL_POSITION";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> foreignKey = new HashMap<>();
                        foreignKey.put("constraintName", rs.getString("CONSTRAINT_NAME"));
                        foreignKey.put("columnName", rs.getString("COLUMN_NAME"));
                        foreignKey.put("referencedTable", rs.getString("REFERENCED_TABLE_NAME"));
                        foreignKey.put("referencedColumn", rs.getString("REFERENCED_COLUMN_NAME"));
                        foreignKey.put("updateRule", rs.getString("UPDATE_RULE"));
                        foreignKey.put("deleteRule", rs.getString("DELETE_RULE"));
                        foreignKeys.add(foreignKey);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取外键信息失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return foreignKeys;
    }

    @Override
    public Date getTableCreateTime(String tableName) throws Exception {
        Date createTime = null;
        try (Connection conn = getConnection()) {
            String sql = "SELECT CREATE_TIME FROM INFORMATION_SCHEMA.TABLES " +
                    "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getDatabaseName());
                stmt.setString(2, tableName);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        createTime = rs.getTimestamp("CREATE_TIME");
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取表创建时间失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return createTime;
    }

    @Override
    public Date getTableUpdateTime(String tableName) throws Exception {
        Date updateTime = null;
        try (Connection conn = getConnection()) {
            String sql = "SELECT UPDATE_TIME FROM INFORMATION_SCHEMA.TABLES " +
                    "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getDatabaseName());
                stmt.setString(2, tableName);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        updateTime = rs.getTimestamp("UPDATE_TIME");
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取表更新时间失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return updateTime;
    }


    @Override
    public Map<String, String> getStoredProcedureDefinitions(String tableName) throws Exception {
        Map<String, String> procedures = new HashMap<>();

        try (Connection conn = getConnection()) {
            // 获取当前数据库名
            String dbName = getDatabaseName();

            // 查询与表相关的存储过程
            String sql = "SELECT DISTINCT p.name AS procedure_name, p.body AS procedure_definition " +
                    "FROM information_schema.routines p " +
                    "WHERE p.routine_schema = ? " +
                    "AND p.routine_type = 'PROCEDURE' " +
                    "AND p.routine_definition LIKE ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, dbName);
                stmt.setString(2, "%" + tableName + "%");

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String procName = rs.getString("procedure_name");
                        String procDef = rs.getString("procedure_definition");
                        procedures.put(procName, procDef);
                    }
                }
            }

            // 如果没有找到存储过程，尝试使用SHOW CREATE PROCEDURE
            if (procedures.isEmpty()) {
                // 先获取所有存储过程名称
                String procNamesSql = "SELECT routine_name FROM information_schema.routines " +
                        "WHERE routine_schema = ? AND routine_type = 'PROCEDURE'";

                try (PreparedStatement stmt = conn.prepareStatement(procNamesSql)) {
                    stmt.setString(1, dbName);

                    List<String> procNames = new ArrayList<>();
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            procNames.add(rs.getString("routine_name"));
                        }
                    }

                    // 对每个存储过程，获取其定义并检查是否包含表名
                    for (String procName : procNames) {
                        String showCreateSql = "SHOW CREATE PROCEDURE " + wrapIdentifier(procName);

                        try (Statement showStmt = conn.createStatement();
                             ResultSet showRs = showStmt.executeQuery(showCreateSql)) {
                            if (showRs.next()) {
                                String procDef = showRs.getString("Create Procedure");
                                if (procDef.contains(tableName)) {
                                    procedures.put(procName, procDef);
                                }
                            }
                        } catch (SQLException e) {
                            // 忽略单个存储过程的错误，继续处理其他存储过程
                            log.warn("获取存储过程{}定义失败: {}", procName, e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取存储过程定义失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }

        return procedures;
    }

    @Override
    public List<String> getTablePartitions(String tableName, String partitionField) throws Exception {
        List<String> partitions = new ArrayList<>();

        // 首先尝试从INFORMATION_SCHEMA中获取分区信息
        String partitionInfoSql = "SELECT PARTITION_NAME, PARTITION_EXPRESSION, PARTITION_DESCRIPTION " +
                "FROM INFORMATION_SCHEMA.PARTITIONS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '" + tableName + "' " +
                "ORDER BY PARTITION_ORDINAL_POSITION";

        boolean hasPartitions = false;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(partitionInfoSql)) {

            while (rs.next()) {
                hasPartitions = true;
                String partitionName = rs.getString("PARTITION_NAME");
                String partitionDescription = rs.getString("PARTITION_DESCRIPTION");
                String partitionExpression = rs.getString("PARTITION_EXPRESSION");

                // 检查是否是我们要找的分区字段
                if (partitionExpression != null && partitionExpression.contains(partitionField) &&
                        partitionName != null && !partitionName.equals("NULL")) {
                    // 对于RANGE和LIST分区，描述将是相关值
                    partitions.add(partitionDescription);
                }
            }
        } catch (Exception e) {
            log.warn("获取分区信息失败: {}", e.getMessage());
        }

        // 如果没有找到分区或不是基于指定字段的分区，则回退到查询唯一值
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
                log.error("获取表的分区字段值失败: {}", e.getMessage());
            }
        }

        return partitions;
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
        // MySQL临时表语法
        sql.append("CREATE TEMPORARY TABLE ").append(wrapIdentifier(tempTableName)).append(" (");
        
        // 添加列定义
        for (int i = 0; i < columns.size(); i++) {
            Map<String, Object> column = columns.get(i);
            if (i > 0) {
                sql.append(", ");
            }
            
            String columnName = column.get("name").toString();
            String dataType = column.get("type").toString();
            
            // 修复VARCHAR类型没有指定长度的问题
            if (dataType.equalsIgnoreCase("varchar") || dataType.toLowerCase().startsWith("varchar(")) {
                // 如果只是"varchar"没有长度，添加默认长度
                if (dataType.equalsIgnoreCase("varchar")) {
                    dataType = "varchar(" + DEFAULT_VARCHAR_LENGTH + ")";
                }
            }
            
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
        
        return sql.toString();
    }

    public String createStoredProcedureSql(String procedureName, List<String> parameters,
                                           List<String> variables, String body) {
        StringBuilder sql = new StringBuilder();
        // 在 JDBC 执行时不需要 DELIMITER 语句
        // sql.append("DELIMITER //\n");
        sql.append("CREATE PROCEDURE ").append(wrapIdentifier(procedureName)).append(" (");

        if (parameters != null && !parameters.isEmpty()) {
            sql.append(String.join(", ", parameters));
        }

        sql.append(")\n");
        sql.append("BEGIN\n");

        if (variables != null && !variables.isEmpty()) {
            for (String variable : variables) {
                sql.append("  DECLARE ").append(variable).append(";\n");
            }
            sql.append("\n");
        }

        sql.append(body).append("\n");
        sql.append("END");
        // 在 JDBC 执行时不需要 DELIMITER 语句
        // sql.append(" //\n");
        // sql.append("DELIMITER ;");

        return sql.toString();
    }

    private String getColumnDefinition(String tableName, String columnName) throws Exception {
        Connection conn = getConnection();
        String sql = "SHOW FULL COLUMNS FROM " + wrapIdentifier(tableName) +
                " WHERE Field = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, columnName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String field = rs.getString("Field");
                    String type = rs.getString("Type");
                    String nullable = rs.getString("Null");
                    String defaultValue = rs.getString("Default");
                    String extra = rs.getString("Extra");
                    String comment = rs.getString("Comment");

                    StringBuilder definition = new StringBuilder();
                    definition.append(wrapIdentifier(field)).append(" ").append(type);

                    if ("NO".equals(nullable)) {
                        definition.append(" NOT NULL");
                    }

                    if (defaultValue != null) {
                        if (defaultValue.equalsIgnoreCase("CURRENT_TIMESTAMP")) {
                            definition.append(" DEFAULT CURRENT_TIMESTAMP");
                        } else if (type.contains("char") || type.contains("text") || type.contains("enum") ||
                                type.contains("set") || type.contains("date") || type.contains("time") &&
                                !defaultValue.equalsIgnoreCase("CURRENT_TIMESTAMP")) {
                            definition.append(" DEFAULT ").append(wrapValue(defaultValue));
                        } else {
                            definition.append(" DEFAULT ").append(defaultValue);
                        }
                    }

                    if (extra != null && !extra.isEmpty()) {
                        definition.append(" ").append(extra);
                    }

                    // 不添加注释，因为注释会在外部方法中添加

                    return definition.toString();
                }
            }
        }

        throw new Exception("列 " + columnName + " 在表 " + tableName + " 中不存在");
    }

    public String getInsertOnDuplicateKeySql(String tableName, List<String> columns,
                                             List<Object> values, Map<String, Object> updateValues) {
        if (columns.size() != values.size()) {
            throw new IllegalArgumentException("列数与值数不匹配");
        }

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(wrapIdentifier(tableName)).append(" (");

        // 添加列
        List<String> wrappedColumns = columns.stream()
                .map(this::wrapIdentifier)
                .collect(Collectors.toList());
        sql.append(String.join(", ", wrappedColumns));

        sql.append(") VALUES (");

        // 添加值
        List<String> wrappedValues = new ArrayList<>();
        for (Object value : values) {
            wrappedValues.add(wrapValue(value));
        }
        sql.append(String.join(", ", wrappedValues));

        sql.append(") ON DUPLICATE KEY UPDATE ");

        // 添加更新表达式
        if (updateValues != null && !updateValues.isEmpty()) {
            List<String> updateExpressions = new ArrayList<>();
            for (Map.Entry<String, Object> entry : updateValues.entrySet()) {
                updateExpressions.add(wrapIdentifier(entry.getKey()) + " = " + wrapValue(entry.getValue()));
            }
            sql.append(String.join(", ", updateExpressions));
        } else {
            // 如果没有指定更新值，则默认更新所有列
            List<String> updateExpressions = new ArrayList<>();
            for (String column : columns) {
                updateExpressions.add(wrapIdentifier(column) + " = VALUES(" + wrapIdentifier(column) + ")");
            }
            sql.append(String.join(", ", updateExpressions));
        }

        return sql.toString();
    }

    public String getAddGeneratedColumnSql(String tableName, String columnName,
                                           String columnType, String expression,
                                           boolean stored) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(wrapIdentifier(tableName));
        sql.append(" ADD COLUMN ").append(wrapIdentifier(columnName));
        sql.append(" ").append(columnType);
        sql.append(" GENERATED ALWAYS AS (").append(expression).append(")");

        if (stored) {
            sql.append(" STORED");
        } else {
            sql.append(" VIRTUAL");
        }

        return sql.toString();
    }

    public String getAddFullTextIndexSql(String tableName, String indexName, List<String> columns) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(wrapIdentifier(tableName));
        sql.append(" ADD FULLTEXT INDEX ").append(wrapIdentifier(indexName));
        sql.append(" (");

        List<String> wrappedColumns = columns.stream()
                .map(this::wrapIdentifier)
                .collect(Collectors.toList());
        sql.append(String.join(", ", wrappedColumns));

        sql.append(")");

        return sql.toString();
    }

    /**
     * 创建触发器
     */
    public String getCreateTriggerSql(String triggerName, String tableName,
                                      String timing, String event, String body) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TRIGGER ").append(wrapIdentifier(triggerName)).append("\n");
        sql.append(timing).append(" ").append(event).append(" ON ").append(wrapIdentifier(tableName)).append("\n");
        sql.append("FOR EACH ROW\n");
        sql.append("BEGIN\n");
        sql.append(body).append("\n");
        sql.append("END");

        return sql.toString();
    }

    /**
     * 删除触发器
     */
    public String getDropTriggerSql(String triggerName) {
        return "DROP TRIGGER IF EXISTS " + wrapIdentifier(triggerName);
    }

    /**
     * 创建事件
     */
    public String getCreateEventSql(String eventName, String schedule,
                                    boolean enabled, String body) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE EVENT ").append(wrapIdentifier(eventName)).append("\n");
        sql.append("ON SCHEDULE ").append(schedule).append("\n");

        if (enabled) {
            sql.append("ENABLE\n");
        } else {
            sql.append("DISABLE\n");
        }

        sql.append("DO\n");
        sql.append("BEGIN\n");
        sql.append(body).append("\n");
        sql.append("END");

        return sql.toString();
    }

    /**
     * 删除事件
     */
    public String getDropEventSql(String eventName) {
        return "DROP EVENT IF EXISTS " + wrapIdentifier(eventName);
    }

    /**
     * 重组分区（用于分裂/合并分区）
     */
    public String getReorganizePartitionSql(String tableName, String oldPartitionName,
                                            List<Map<String, String>> newPartitions) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(wrapIdentifier(tableName));
        sql.append(" REORGANIZE PARTITION ").append(wrapIdentifier(oldPartitionName));
        sql.append(" INTO (");

        List<String> partitionDefinitions = new ArrayList<>();
        for (Map<String, String> partition : newPartitions) {
            String name = partition.get("name");
            String values = partition.get("values");
            String engine = partition.get("engine");
            String comment = partition.get("comment");

            StringBuilder partitionDef = new StringBuilder();
            partitionDef.append("PARTITION ").append(wrapIdentifier(name));
            partitionDef.append(" VALUES ").append(values);

            if (engine != null && !engine.isEmpty()) {
                partitionDef.append(" ENGINE = ").append(engine);
            }

            if (comment != null && !comment.isEmpty()) {
                partitionDef.append(" COMMENT = ").append(wrapValue(comment));
            }

            partitionDefinitions.add(partitionDef.toString());
        }

        sql.append(String.join(",\n", partitionDefinitions));
        sql.append(")");

        return sql.toString();
    }

    /**
     * 交换分区
     */
    public String getExchangePartitionSql(String tableName, String partitionName, String withTable) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(wrapIdentifier(tableName));
        sql.append(" EXCHANGE PARTITION ").append(wrapIdentifier(partitionName));
        sql.append(" WITH TABLE ").append(wrapIdentifier(withTable));

        return sql.toString();
    }

    /**
     * 修剪分区
     */
    public String getCoalescePartitionSql(String tableName, int number) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(wrapIdentifier(tableName));
        sql.append(" COALESCE PARTITION ").append(number);

        return sql.toString();
    }

    /**
     * 获取锁定表的SQL
     */
    public String getLockTablesSql(String tableName, String lockType) {
        return "LOCK TABLES " + wrapIdentifier(tableName) + " " + lockType;
    }

    /**
     * 获取解锁表的SQL
     */
    public String getUnlockTablesSql() {
        return "UNLOCK TABLES";
    }

    /**
     * 获取会话锁状态
     */
    public String getShowLocksSql() {
        return "SHOW OPEN TABLES WHERE In_use > 0";
    }

    /**
     * 获取表锁等待SQL
     */
    public String getTableLocksWaitingQuery() {
        return "SELECT * FROM information_schema.INNODB_LOCK_WAITS";
    }

    /**
     * 窗口函数支持
     */
    public String getWindowFunctionSql(String baseQuery, String partitionBy, String orderBy,
                                       String windowFunction, String alias) {
        StringBuilder sql = new StringBuilder();
        sql.append(windowFunction).append(" OVER (");

        if (partitionBy != null && !partitionBy.isEmpty()) {
            sql.append("PARTITION BY ").append(partitionBy).append(" ");
        }

        if (orderBy != null && !orderBy.isEmpty()) {
            sql.append("ORDER BY ").append(orderBy);
        }

        sql.append(") AS ").append(wrapIdentifier(alias));

        return "SELECT *, " + sql.toString() + " FROM (" + baseQuery + ") t";
    }

    /**
     * 公共表表达式（CTE）支持
     */
    public String getCommonTableExpressionSql(String cteName, String cteQuery, String mainQuery) {
        StringBuilder sql = new StringBuilder();
        sql.append("WITH ").append(wrapIdentifier(cteName)).append(" AS (");
        sql.append(cteQuery);
        sql.append(") ");
        sql.append(mainQuery);

        return sql.toString();
    }

    /**
     * 创建函数索引 (MySQL 8.0.13+)
     */
    public String getCreateFunctionalIndexSql(String tableName, String indexName, String expression) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(wrapIdentifier(tableName));
        sql.append(" ADD INDEX ").append(wrapIdentifier(indexName));
        sql.append(" ((").append(expression).append("))");

        return sql.toString();
    }

    /**
     * 创建降序索引 (MySQL 8.0.12+)
     */
    public String getCreateDescendingIndexSql(String tableName, String indexName,
                                              Map<String, Boolean> columnsWithDirection) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(wrapIdentifier(tableName));
        sql.append(" ADD INDEX ").append(wrapIdentifier(indexName));
        sql.append(" (");

        List<String> columnDefinitions = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : columnsWithDirection.entrySet()) {
            String direction = entry.getValue() ? "ASC" : "DESC";
            columnDefinitions.add(wrapIdentifier(entry.getKey()) + " " + direction);
        }

        sql.append(String.join(", ", columnDefinitions));
        sql.append(")");

        return sql.toString();
    }

    /**
     * 不可见索引支持 (MySQL 8.0+)
     */
    public String getAlterIndexVisibilitySql(String tableName, String indexName, boolean visible) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(wrapIdentifier(tableName));
        sql.append(" ALTER INDEX ").append(wrapIdentifier(indexName));

        if (visible) {
            sql.append(" VISIBLE");
        } else {
            sql.append(" INVISIBLE");
        }

        return sql.toString();
    }

    /**
     * 创建角色
     */
    public String getCreateRoleSql(String roleName) {
        return "CREATE ROLE " + wrapIdentifier(roleName);
    }

    /**
     * 删除角色
     */
    public String getDropRoleSql(String roleName) {
        return "DROP ROLE IF EXISTS " + wrapIdentifier(roleName);
    }

    /**
     * 向角色授权
     */
    public String getGrantToRoleSql(String roleName, String privilege, String object) {
        return "GRANT " + privilege + " ON " + object + " TO " + wrapIdentifier(roleName);
    }

    /**
     * 从角色撤销权限
     */
    public String getRevokeFromRoleSql(String roleName, String privilege, String object) {
        return "REVOKE " + privilege + " ON " + object + " FROM " + wrapIdentifier(roleName);
    }

    /**
     * 向用户授予角色
     */
    public String getGrantRoleToUserSql(String roleName, String userName, String host) {
        return "GRANT " + wrapIdentifier(roleName) + " TO '" + userName + "'@'" + host + "'";
    }

    /**
     * 设置默认角色
     */
    public String getSetDefaultRoleSql(String roleName, String userName, String host) {
        return "SET DEFAULT ROLE " + wrapIdentifier(roleName) + " TO '" + userName + "'@'" + host + "'";
    }

    /**
     * 获取mysqldump命令
     */
    public String getMysqlDumpCommand(String database, String outputFile, boolean includeTables,
                                      boolean includeData, boolean includeRoutines, boolean includeEvents) {
        StringBuilder command = new StringBuilder();
        command.append("mysqldump ");

        if (!includeTables) {
            command.append("--no-create-info ");
        }

        if (!includeData) {
            command.append("--no-data ");
        }

        if (includeRoutines) {
            command.append("--routines ");
        }

        if (includeEvents) {
            command.append("--events ");
        }

        command.append("-u [username] -p[password] ");
        command.append(database);
        command.append(" > ").append(outputFile);

        return command.toString();
    }

    /**
     * 获取mysql导入命令
     */
    public String getMysqlImportCommand(String database, String inputFile) {
        return "mysql -u [username] -p[password] " + database + " < " + inputFile;
    }

    /**
     * JSON_EXTRACT操作
     */
    public String getJsonExtractSql(String column, String path) {
        return "JSON_EXTRACT(" + wrapIdentifier(column) + ", '$" + path + "')";
    }

    /**
     * JSON_CONTAINS操作
     */
    public String getJsonContainsSql(String column, String path, Object value) {
        return "JSON_CONTAINS(" + wrapIdentifier(column) + ", '" + value + "', '$" + path + "')";
    }

    /**
     * JSON_SET操作
     */
    public String getJsonSetSql(String column, String path, Object value) {
        return "JSON_SET(" + wrapIdentifier(column) + ", '$" + path + "', " + wrapValue(value) + ")";
    }

    /**
     * JSON_REMOVE操作
     */
    public String getJsonRemoveSql(String column, String path) {
        return "JSON_REMOVE(" + wrapIdentifier(column) + ", '$" + path + "')";
    }

    /**
     * JSON_ARRAY_APPEND操作
     */
    public String getJsonArrayAppendSql(String column, String path, Object value) {
        return "JSON_ARRAY_APPEND(" + wrapIdentifier(column) + ", '$" + path + "', " + wrapValue(value) + ")";
    }

    /**
     * 自然语言模式全文搜索
     */
    public String getNaturalLanguageFullTextSearchSql(String tableName, List<String> columns, String searchText) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ").append(wrapIdentifier(tableName)).append(" WHERE MATCH(");

        List<String> wrappedColumns = columns.stream()
                .map(this::wrapIdentifier)
                .collect(Collectors.toList());
        sql.append(String.join(", ", wrappedColumns));

        sql.append(") AGAINST (").append(wrapValue(searchText)).append(" IN NATURAL LANGUAGE MODE)");

        return sql.toString();
    }

    /**
     * 布尔模式全文搜索
     */
    public String getBooleanModeFullTextSearchSql(String tableName, List<String> columns, String searchText) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ").append(wrapIdentifier(tableName)).append(" WHERE MATCH(");

        List<String> wrappedColumns = columns.stream()
                .map(this::wrapIdentifier)
                .collect(Collectors.toList());
        sql.append(String.join(", ", wrappedColumns));

        sql.append(") AGAINST (").append(wrapValue(searchText)).append(" IN BOOLEAN MODE)");

        return sql.toString();
    }

    /**
     * 添加检查约束 (MySQL 8.0.16+)
     */
    public String getAddCheckConstraintSql(String tableName, String constraintName, String expression) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(wrapIdentifier(tableName));
        sql.append(" ADD CONSTRAINT ").append(wrapIdentifier(constraintName));
        sql.append(" CHECK (").append(expression).append(")");

        return sql.toString();
    }

    /**
     * 删除检查约束
     */
    public String getDropCheckConstraintSql(String tableName, String constraintName) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(wrapIdentifier(tableName));
        sql.append(" DROP CHECK ").append(wrapIdentifier(constraintName));

        return sql.toString();
    }

    /**
     * 添加唯一约束
     */
    public String getAddUniqueConstraintSql(String tableName, String constraintName, List<String> columns) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(wrapIdentifier(tableName));
        sql.append(" ADD CONSTRAINT ").append(wrapIdentifier(constraintName));
        sql.append(" UNIQUE (");

        List<String> wrappedColumns = columns.stream()
                .map(this::wrapIdentifier)
                .collect(Collectors.toList());
        sql.append(String.join(", ", wrappedColumns));

        sql.append(")");

        return sql.toString();
    }

    /**
     * 创建生成列索引的SQL
     */
    public String getCreateGeneratedColumnIndexSql(String tableName, String indexName,
                                                   String columnName, String expression,
                                                   boolean stored, boolean unique) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(wrapIdentifier(tableName));

        // 先添加生成列
        sql.append(" ADD COLUMN ").append(wrapIdentifier(columnName));
        sql.append(" VARCHAR(255) GENERATED ALWAYS AS (").append(expression).append(")");

        if (stored) {
            sql.append(" STORED");
        } else {
            sql.append(" VIRTUAL");
        }

        // 然后在生成列上创建索引
        sql.append(", ADD ");
        if (unique) {
            sql.append("UNIQUE ");
        }
        sql.append("INDEX ").append(wrapIdentifier(indexName));
        sql.append(" (").append(wrapIdentifier(columnName)).append(")");

        return sql.toString();
    }

    /**
     * 获取数据复制SQL (INSERT INTO ... SELECT)
     */
    public String getInsertSelectSql(String targetTable, List<String> targetColumns,
                                     String sourceTable, List<String> sourceColumns,
                                     String whereClause) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(wrapIdentifier(targetTable));

        if (targetColumns != null && !targetColumns.isEmpty()) {
            sql.append(" (");
            List<String> wrappedTargetColumns = targetColumns.stream()
                    .map(this::wrapIdentifier)
                    .collect(Collectors.toList());
            sql.append(String.join(", ", wrappedTargetColumns));
            sql.append(")");
        }

        sql.append(" SELECT ");

        if (sourceColumns != null && !sourceColumns.isEmpty()) {
            List<String> wrappedSourceColumns = sourceColumns.stream()
                    .map(this::wrapIdentifier)
                    .collect(Collectors.toList());
            sql.append(String.join(", ", wrappedSourceColumns));
        } else {
            sql.append("*");
        }

        sql.append(" FROM ").append(wrapIdentifier(sourceTable));

        if (whereClause != null && !whereClause.isEmpty()) {
            sql.append(" WHERE ").append(whereClause);
        }

        return sql.toString();
    }

    /**
     * 创建临时表SQL
     */
    public String getCreateTemporaryTableSql(String tableName, List<ColumnDefinition> columns,
                                             boolean ifNotExists) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TEMPORARY TABLE ");

        if (ifNotExists) {
            sql.append("IF NOT EXISTS ");
        }

        sql.append(wrapIdentifier(tableName)).append(" (");

        List<String> columnDefinitions = new ArrayList<>();
        for (ColumnDefinition column : columns) {
            // 调整列类型以符合MySQL要求
            adjustMySQLColumnType(column);

            String columnDef = formatFieldDefinition(
                    column.getName(), column.getType(),
                    column.getLength(), column.getPrecision(), column.getScale(),
                    column.isNullable(), column.getDefaultValue(), column.getComment()
            );

            columnDefinitions.add(columnDef);
        }

        sql.append(String.join(", ", columnDefinitions));
        sql.append(")");

        return sql.toString();
    }

    /**
     * 获取克隆数据库SQL (MySQL 8.0.17+)
     */
    public String getCloneDatabaseSql(String sourceDb, String targetDb) {
        return "CLONE DATABASE " + wrapIdentifier(sourceDb) + " TO " + wrapIdentifier(targetDb);
    }

    /**
     * 获取克隆实例SQL (MySQL 8.0.17+)
     */
    public String getCloneInstanceSql(String donorHost, int donorPort, String userName, String password) {
        return String.format("CLONE INSTANCE FROM '%s'@'%s':%d IDENTIFIED BY '%s'",
                userName, donorHost, donorPort, password);
    }

    /**
     * JSON_OVERLAPS函数 (MySQL 8.0.28+)
     */
    public String getJsonOverlapsSql(String json1, String json2) {
        return "JSON_OVERLAPS(" + json1 + ", " + json2 + ")";
    }

    /**
     * 获取死锁信息的SQL
     */
    public String getDeadlockInformationSql() {
        return "SHOW ENGINE INNODB STATUS";
    }

    /**
     * 解析InnoDB状态中的死锁信息
     * @param statusOutput InnoDB状态输出
     * @return 解析后的死锁信息
     */
    public Map<String, Object> parseDeadlockInformation(String statusOutput) {
        Map<String, Object> result = new HashMap<>();

        // 找到死锁部分
        int deadlockStart = statusOutput.indexOf("LATEST DETECTED DEADLOCK");
        if (deadlockStart == -1) {
            result.put("hasDeadlock", false);
            return result;
        }

        int deadlockEnd = statusOutput.indexOf("------------", deadlockStart);
        if (deadlockEnd == -1) {
            deadlockEnd = statusOutput.length();
        }

        String deadlockInfo = statusOutput.substring(deadlockStart, deadlockEnd);

        // 基本解析
        result.put("hasDeadlock", true);
        result.put("rawInfo", deadlockInfo);

        // 提取事务信息
        List<Map<String, String>> transactions = new ArrayList<>();
        String[] txParts = deadlockInfo.split("\\*\\*\\* \\(\\d+\\)");

        for (int i = 1; i < txParts.length; i++) {
            Map<String, String> tx = new HashMap<>();

            // 提取事务ID
            String txPart = txParts[i];
            Pattern txIdPattern = Pattern.compile("TRANSACTION (\\d+)");
            Matcher txIdMatcher = txIdPattern.matcher(txPart);
            if (txIdMatcher.find()) {
                tx.put("transactionId", txIdMatcher.group(1));
            }

            // 提取表名
            Pattern tablePattern = Pattern.compile("table `([^`]+)`.`([^`]+)`");
            Matcher tableMatcher = tablePattern.matcher(txPart);
            if (tableMatcher.find()) {
                tx.put("database", tableMatcher.group(1));
                tx.put("table", tableMatcher.group(2));
            }

            // 提取锁类型
            if (txPart.contains("lock_mode X")) {
                tx.put("lockMode", "EXCLUSIVE");
            } else if (txPart.contains("lock_mode S")) {
                tx.put("lockMode", "SHARED");
            }

            // 提取SQL
            Pattern sqlPattern = Pattern.compile("executing query: ([\\s\\S]+?)\\n[^\\n]");
            Matcher sqlMatcher = sqlPattern.matcher(txPart);
            if (sqlMatcher.find()) {
                tx.put("sql", sqlMatcher.group(1).trim());
            }

            transactions.add(tx);
        }

        result.put("transactions", transactions);

        // 提取死锁发生时间
        Pattern timePattern = Pattern.compile("\\d{6} \\d{2}:\\d{2}:\\d{2}");
        Matcher timeMatcher = timePattern.matcher(deadlockInfo);
        if (timeMatcher.find()) {
            result.put("detectedTime", timeMatcher.group(0));
        }

        return result;
    }

    /**
     * 获取查询优化建议
     * @param sql 需要分析的SQL
     * @return 优化建议
     */
    public Map<String, Object> getQueryOptimizationAdvice(String sql) throws Exception {
        Map<String, Object> result = new HashMap<>();
        List<String> suggestions = new ArrayList<>();

        // 1. 获取执行计划
        String explainSql = "EXPLAIN FORMAT=JSON " + sql;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(explainSql)) {

            if (rs.next()) {
                String explainResult = rs.getString(1);
                JSONObject explainJson = new JSONObject(explainResult);

                // 分析执行计划
                List<String> planSuggestions = analyzePlan(explainJson);
                suggestions.addAll(planSuggestions);
            }
        }

        // 2. 检查索引使用情况
        List<String> indexSuggestions = checkIndexUsage(sql);
        suggestions.addAll(indexSuggestions);

        // 3. 检查查询语法
        List<String> syntaxSuggestions = checkQuerySyntax(sql);
        suggestions.addAll(syntaxSuggestions);

        result.put("sql", sql);
        result.put("suggestions", suggestions);

        return result;
    }

    private List<String> analyzePlan(JSONObject explainJson) {
        List<String> suggestions = new ArrayList<>();

        try {
            JSONObject queryBlock = explainJson.getJSONObject("query_block");

            // 检查全表扫描
            if (queryBlock.has("table")) {
                JSONObject table = queryBlock.getJSONObject("table");
                String accessType = table.optString("access_type", "");

                if ("ALL".equals(accessType)) {
                    String tableName = table.optString("table_name", "unknown");
                    suggestions.add("Table '" + tableName + "' is using full table scan. Consider adding an index.");
                }

                // 检查临时表
                if (table.has("using_temporary_table")) {
                    suggestions.add("Query is using a temporary table. This may impact performance.");
                }

                // 检查文件排序
                if (table.has("using_filesort")) {
                    suggestions.add("Query is using filesort. Consider adding an index for ORDER BY clause.");
                }
            }

            // 检查嵌套循环
            if (queryBlock.has("nested_loop")) {
                JSONArray nestedLoop = queryBlock.getJSONArray("nested_loop");
                if (nestedLoop.length() > 3) {
                    suggestions.add("Query has " + nestedLoop.length() + " nested loops. Consider redesigning your query.");
                }
            }
        } catch (Exception e) {
            suggestions.add("Error analyzing execution plan: " + e.getMessage());
        }

        return suggestions;
    }

    private List<String> checkIndexUsage(String sql) {
        List<String> suggestions = new ArrayList<>();

        // 解析SQL提取表名和WHERE条件
        // 这里是一个简化的实现，实际上需要使用SQL解析器
        sql = sql.toLowerCase();
        Pattern tablePattern = Pattern.compile("from\\s+([\\w,\\s.`]+)\\s+where");
        Matcher tableMatcher = tablePattern.matcher(sql);

        if (tableMatcher.find()) {
            String tableSection = tableMatcher.group(1);
            String[] tables = tableSection.split(",");

            for (String table : tables) {
                table = table.trim();
                if (table.contains(" as ")) {
                    table = table.substring(0, table.indexOf(" as ")).trim();
                }

                // 移除反引号
                table = table.replace("`", "");

                try {
                    List<Map<String, Object>> indexes = getIndexes(table);
                    if (indexes.isEmpty()) {
                        suggestions.add("Table '" + table + "' has no indexes. Consider adding appropriate indexes.");
                    }

                    // 检查WHERE子句中的列是否有索引
                    Pattern wherePattern = Pattern.compile("where(.+?)(?:order by|group by|limit|$)");
                    Matcher whereMatcher = wherePattern.matcher(sql);

                    if (whereMatcher.find()) {
                        String whereClause = whereMatcher.group(1);
                        Pattern columnPattern = Pattern.compile("([\\w.`]+)\\s*[=><]");
                        Matcher columnMatcher = columnPattern.matcher(whereClause);

                        while (columnMatcher.find()) {
                            String column = columnMatcher.group(1).replace("`", "");
                            if (column.contains(".")) {
                                column = column.substring(column.indexOf(".") + 1);
                            }

                            boolean hasIndexOnColumn = false;
                            for (Map<String, Object> index : indexes) {
                                String columnName = (String) index.get("Column_name");
                                if (column.equalsIgnoreCase(columnName)) {
                                    hasIndexOnColumn = true;
                                    break;
                                }
                            }

                            if (!hasIndexOnColumn) {
                                suggestions.add("Column '" + column + "' in WHERE clause has no index.");
                            }
                        }
                    }
                } catch (Exception e) {
                    suggestions.add("Error checking indexes for table '" + table + "': " + e.getMessage());
                }
            }
        }

        return suggestions;
    }

    private List<String> checkQuerySyntax(String sql) {
        List<String> suggestions = new ArrayList<>();

        // 检查SELECT *
        if (sql.toLowerCase().contains("select *")) {
            suggestions.add("Using 'SELECT *' is generally not recommended. Specify only needed columns.");
        }

        // 检查LIKE with leading wildcard
        Pattern likePattern = Pattern.compile("like\\s+'%");
        Matcher likeMatcher = likePattern.matcher(sql.toLowerCase());
        if (likeMatcher.find()) {
            suggestions.add("Using LIKE with a leading wildcard ('%...') prevents index usage.");
        }

        // 检查OR条件
        if (sql.toLowerCase().contains(" or ")) {
            suggestions.add("Using OR conditions may prevent optimal index usage. Consider UNION if possible.");
        }

        // 检查JOIN语法
        if (!sql.toLowerCase().contains("inner join") && !sql.toLowerCase().contains("left join") &&
                sql.toLowerCase().contains("join")) {
            suggestions.add("Consider using explicit JOIN syntax (INNER JOIN, LEFT JOIN) for better readability.");
        }

        // 检查GROUP BY和ORDER BY中的数字引用
        Pattern numberPattern = Pattern.compile("(group by|order by)\\s+\\d+");
        Matcher numberMatcher = numberPattern.matcher(sql.toLowerCase());
        if (numberMatcher.find()) {
            suggestions.add("Using column position numbers in GROUP BY or ORDER BY is not recommended.");
        }

        return suggestions;
    }

    /**
     * 获取MySQL服务器健康状态
     * @return 服务器健康状态
     */
    public Map<String, Object> getServerHealthStatus() throws Exception {
        Map<String, Object> result = new HashMap<>();

        // 连接数统计
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW STATUS WHERE Variable_name LIKE 'Threads_%' OR Variable_name = 'Connections'")) {

            Map<String, Object> connections = new HashMap<>();
            while (rs.next()) {
                String name = rs.getString("Variable_name");
                String value = rs.getString("Value");
                connections.put(name, value);
            }
            result.put("connections", connections);
        }

        // 缓存命中率
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW GLOBAL STATUS WHERE Variable_name IN ('Innodb_buffer_pool_reads', 'Innodb_buffer_pool_read_requests')")) {

            long reads = 0;
            long readRequests = 0;

            while (rs.next()) {
                String name = rs.getString("Variable_name");
                if ("Innodb_buffer_pool_reads".equals(name)) {
                    reads = Long.parseLong(rs.getString("Value"));
                } else if ("Innodb_buffer_pool_read_requests".equals(name)) {
                    readRequests = Long.parseLong(rs.getString("Value"));
                }
            }

            double hitRatio = 0;
            if (readRequests > 0) {
                hitRatio = 100.0 - ((double) reads / readRequests * 100.0);
            }

            result.put("bufferPoolHitRatio", String.format("%.2f%%", hitRatio));
        }

        // 慢查询统计
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW GLOBAL STATUS WHERE Variable_name = 'Slow_queries'")) {

            if (rs.next()) {
                result.put("slowQueries", rs.getString("Value"));
            }
        }

        // 检查表锁和行锁
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW GLOBAL STATUS WHERE Variable_name LIKE '%lock%'")) {

            Map<String, String> locks = new HashMap<>();
            while (rs.next()) {
                locks.put(rs.getString("Variable_name"), rs.getString("Value"));
            }
            result.put("locks", locks);
        }

        // 检查磁盘空间
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT table_schema, SUM(data_length + index_length) / 1024 / 1024 AS size_mb FROM information_schema.TABLES GROUP BY table_schema")) {

            Map<String, Double> dbSizes = new HashMap<>();
            while (rs.next()) {
                dbSizes.put(rs.getString("table_schema"), rs.getDouble("size_mb"));
            }
            result.put("databaseSizes", dbSizes);
        }

        // 获取MySQL版本信息
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT VERSION() AS version")) {

            if (rs.next()) {
                result.put("version", rs.getString("version"));
            }
        }

        // 获取InnoDB引擎状态
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW ENGINE INNODB STATUS")) {

            if (rs.next()) {
                String status = rs.getString(3);

                // 提取关键信息
                Map<String, String> innodbStatus = new HashMap<>();

                // 提取缓冲池信息
                Pattern bufferPattern = Pattern.compile("Buffer pool size\\s+(\\d+)");
                Matcher bufferMatcher = bufferPattern.matcher(status);
                if (bufferMatcher.find()) {
                    innodbStatus.put("bufferPoolSize", bufferMatcher.group(1));
                }

                // 提取页读写信息
                Pattern ioPattern = Pattern.compile("(\\d+) OS file reads, (\\d+) OS file writes, (\\d+) OS fsyncs");
                Matcher ioMatcher = ioPattern.matcher(status);
                if (ioMatcher.find()) {
                    innodbStatus.put("osReads", ioMatcher.group(1));
                    innodbStatus.put("osWrites", ioMatcher.group(2));
                    innodbStatus.put("osFsyncs", ioMatcher.group(3));
                }

                // 提取事务信息
                Pattern trxPattern = Pattern.compile("History list length (\\d+)");
                Matcher trxMatcher = trxPattern.matcher(status);
                if (trxMatcher.find()) {
                    innodbStatus.put("historyListLength", trxMatcher.group(1));
                }

                result.put("innodbStatus", innodbStatus);
            }
        }

        return result;
    }

    /**
     * 获取敏感数据掩码SQL
     * @param tableName 表名
     * @param columnName 需要掩码的列名
     * @param maskingType 掩码类型 (例如: 'email', 'phone', 'name', 'creditcard')
     * @return 带掩码的SQL
     */
    public String getMaskedColumnSql(String tableName, String columnName, String maskingType) {
        StringBuilder maskedColumn = new StringBuilder();

        switch (maskingType.toLowerCase()) {
            case "email":
                // 保留邮箱的域名部分，掩码用户名部分
                maskedColumn.append("CONCAT(");
                maskedColumn.append("LEFT(").append(wrapIdentifier(columnName)).append(", 2), ");
                maskedColumn.append("'***', ");
                maskedColumn.append("SUBSTRING(").append(wrapIdentifier(columnName)).append(", LOCATE('@', ").append(wrapIdentifier(columnName)).append(")))");
                break;

            case "phone":
                // 保留前3位和后4位，中间用星号代替
                maskedColumn.append("CONCAT(");
                maskedColumn.append("LEFT(").append(wrapIdentifier(columnName)).append(", 3), ");
                maskedColumn.append("'****', ");
                maskedColumn.append("RIGHT(").append(wrapIdentifier(columnName)).append(", 4))");
                break;

            case "name":
                // 只显示第一个字符，其余用星号代替
                maskedColumn.append("CONCAT(");
                maskedColumn.append("LEFT(").append(wrapIdentifier(columnName)).append(", 1), ");
                maskedColumn.append("REPEAT('*', LENGTH(").append(wrapIdentifier(columnName)).append(") - 1))");
                break;

            case "creditcard":
                // 只显示后4位，其余用星号代替
                maskedColumn.append("CONCAT(");
                maskedColumn.append("REPEAT('*', LENGTH(").append(wrapIdentifier(columnName)).append(") - 4), ");
                maskedColumn.append("RIGHT(").append(wrapIdentifier(columnName)).append(", 4))");
                break;

            case "address":
                // 保留国家/省份信息，其余用星号代替
                maskedColumn.append("CONCAT(");
                maskedColumn.append("SUBSTRING_INDEX(").append(wrapIdentifier(columnName)).append(", ',', 1), ");
                maskedColumn.append("', ***')");
                break;

            default:
                // 默认掩码方式，所有字符用星号代替
                maskedColumn.append("REPEAT('*', LENGTH(").append(wrapIdentifier(columnName)).append("))");
                break;
        }

        return maskedColumn.toString() + " AS " + wrapIdentifier(columnName);
    }

    /**
     * 创建数据加密函数
     * @return 创建加密函数的SQL
     */
    public String createEncryptionFunctions() {
        StringBuilder sql = new StringBuilder();

        // 创建AES加密函数
        sql.append("CREATE FUNCTION IF NOT EXISTS fn_encrypt_aes(input_string VARCHAR(1000), encryption_key VARCHAR(32)) ");
        sql.append("RETURNS VARCHAR(1000) DETERMINISTIC ");
        sql.append("RETURN TO_BASE64(AES_ENCRYPT(input_string, encryption_key));\n");

        // 创建AES解密函数
        sql.append("CREATE FUNCTION IF NOT EXISTS fn_decrypt_aes(encrypted_string VARCHAR(1000), encryption_key VARCHAR(32)) ");
        sql.append("RETURNS VARCHAR(1000) DETERMINISTIC ");
        sql.append("RETURN AES_DECRYPT(FROM_BASE64(encrypted_string), encryption_key);\n");

        return sql.toString();
    }

    /**
     * 获取加密列SQL
     * @param columnName 列名
     * @param encryptionKey 加密密钥
     * @return 加密SQL
     */
    public String getEncryptColumnSql(String columnName, String encryptionKey) {
        return "fn_encrypt_aes(" + wrapIdentifier(columnName) + ", '" + encryptionKey + "')";
    }

    /**
     * 获取解密列SQL
     * @param columnName 列名
     * @param encryptionKey 加密密钥
     * @return 解密SQL
     */
    public String getDecryptColumnSql(String columnName, String encryptionKey) {
        return "fn_decrypt_aes(" + wrapIdentifier(columnName) + ", '" + encryptionKey + "')";
    }

    /**
     * 创建加密表SQL
     * @param sourceTable 源表名
     * @param targetTable 目标表名
     * @param columnsToEncrypt 需要加密的列及其加密密钥
     * @return 创建加密表的SQL
     */
    public String createEncryptedTableSql(String sourceTable, String targetTable,
                                          Map<String, String> columnsToEncrypt) {
        StringBuilder sql = new StringBuilder();

        // 获取源表的列信息
        try {
            List<ColumnDefinition> columns = getTableColumns(getSchema(), sourceTable);

            sql.append("CREATE TABLE ").append(wrapIdentifier(targetTable)).append(" AS\n");
            sql.append("SELECT\n");

            List<String> columnSelects = new ArrayList<>();

            for (ColumnDefinition column : columns) {
                String columnName = column.getName();

                if (columnsToEncrypt.containsKey(columnName)) {
                    String encryptionKey = columnsToEncrypt.get(columnName);
                    columnSelects.add(getEncryptColumnSql(columnName, encryptionKey) + " AS " + wrapIdentifier(columnName));
                } else {
                    columnSelects.add(wrapIdentifier(columnName));
                }
            }

            sql.append(String.join(",\n", columnSelects));
            sql.append("\nFROM ").append(wrapIdentifier(sourceTable)).append(";");

        } catch (Exception e) {
            sql.append("-- Error creating encrypted table: ").append(e.getMessage());
        }

        return sql.toString();
    }

    /**
     * 获取MySQL复制状态信息
     * @return 复制状态信息
     */
    public Map<String, Object> getReplicationStatus() throws Exception {
        Map<String, Object> result = new HashMap<>();

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // 检查主从复制状态
            try (ResultSet rs = stmt.executeQuery("SHOW SLAVE STATUS")) {
                if (rs.next()) {
                    Map<String, Object> slaveStatus = new HashMap<>();

                    slaveStatus.put("slaveIORunning", rs.getString("Slave_IO_Running"));
                    slaveStatus.put("slaveSQLRunning", rs.getString("Slave_SQL_Running"));
                    slaveStatus.put("lastIOError", rs.getString("Last_IO_Error"));
                    slaveStatus.put("lastSQLError", rs.getString("Last_SQL_Error"));
                    slaveStatus.put("secondsBehindMaster", rs.getLong("Seconds_Behind_Master"));
                    slaveStatus.put("masterHost", rs.getString("Master_Host"));
                    slaveStatus.put("masterUser", rs.getString("Master_User"));
                    slaveStatus.put("masterLog", rs.getString("Master_Log_File"));
                    slaveStatus.put("readMasterLogPos", rs.getLong("Read_Master_Log_Pos"));

                    result.put("slaveStatus", slaveStatus);
                    result.put("isReplica", true);
                } else {
                    result.put("isReplica", false);
                }
            } catch (SQLException e) {
                result.put("slaveStatusError", e.getMessage());
            }

            // 检查主库状态
            try (ResultSet rs = stmt.executeQuery("SHOW MASTER STATUS")) {
                if (rs.next()) {
                    Map<String, Object> masterStatus = new HashMap<>();

                    masterStatus.put("binaryLogFile", rs.getString("File"));
                    masterStatus.put("position", rs.getLong("Position"));
                    masterStatus.put("binlogDoDB", rs.getString("Binlog_Do_DB"));
                    masterStatus.put("binlogIgnoreDB", rs.getString("Binlog_Ignore_DB"));

                    result.put("masterStatus", masterStatus);
                    result.put("isMaster", true);
                } else {
                    result.put("isMaster", false);
                }
            } catch (SQLException e) {
                result.put("masterStatusError", e.getMessage());
            }

            // 获取复制延迟
            if ((boolean)result.getOrDefault("isReplica", false)) {
                Map<String, Object> slaveStatus = (Map<String, Object>)result.get("slaveStatus");
                long secondsBehind = (long)slaveStatus.getOrDefault("secondsBehindMaster", 0L);

                String delayStatus = "Normal";
                if (secondsBehind > 3600) {
                    delayStatus = "Critical";
                } else if (secondsBehind > 600) {
                    delayStatus = "Warning";
                } else if (secondsBehind > 60) {
                    delayStatus = "Attention";
                }

                result.put("replicationDelay", secondsBehind);
                result.put("replicationDelayStatus", delayStatus);
            }
        }

        return result;
    }

    /**
     * 配置MySQL复制 (创建主从关系)
     * @param masterHost 主服务器主机名
     * @param masterPort 主服务器端口
     * @param masterUser 复制用户名
     * @param masterPassword 复制用户密码
     * @param logFile 二进制日志文件名
     * @param logPos 日志位置
     * @return 配置复制的SQL语句
     */
    public String getConfigureReplicationSql(String masterHost, int masterPort,
                                             String masterUser, String masterPassword,
                                             String logFile, long logPos) {
        StringBuilder sql = new StringBuilder();

        // 停止现有复制
        sql.append("STOP SLAVE;\n");

        // 设置主服务器信息
        sql.append("CHANGE MASTER TO\n");
        sql.append("  MASTER_HOST = '").append(masterHost).append("',\n");
        sql.append("  MASTER_PORT = ").append(masterPort).append(",\n");
        sql.append("  MASTER_USER = '").append(masterUser).append("',\n");
        sql.append("  MASTER_PASSWORD = '").append(masterPassword).append("'");

        if (logFile != null && !logFile.isEmpty()) {
            sql.append(",\n  MASTER_LOG_FILE = '").append(logFile).append("'");
        }

        if (logPos > 0) {
            sql.append(",\n  MASTER_LOG_POS = ").append(logPos);
        }

        sql.append(";\n");

        // 启动复制
        sql.append("START SLAVE;");

        return sql.toString();
    }

    /**
     * 生成MySQL Shell脚本，用于高级数据库管理
     * @param operationType 操作类型 (backup, restore, cluster-setup, etc.)
     * @param params 操作参数
     * @return MySQL Shell脚本
     */
    public String generateMySQLShellScript(String operationType, Map<String, Object> params) {
        StringBuilder script = new StringBuilder();

        // 添加脚本头部
        script.append("#!/usr/bin/env mysqlsh\n\n");
        script.append("// MySQL Shell Script for ").append(operationType).append("\n");
        script.append("// Generated on ").append(new Date()).append("\n\n");

        switch (operationType.toLowerCase()) {
            case "backup":
                generateBackupScript(script, params);
                break;
            case "restore":
                generateRestoreScript(script, params);
                break;
            case "cluster-setup":
                generateClusterSetupScript(script, params);
                break;
            case "health-check":
                generateHealthCheckScript(script, params);
                break;
            case "data-migration":
                generateDataMigrationScript(script, params);
                break;
            default:
                script.append("print('Unknown operation type: ").append(operationType).append("');\n");
        }

        return script.toString();
    }

    private void generateBackupScript(StringBuilder script, Map<String, Object> params) {
        String uri = params.getOrDefault("uri", "root@localhost:3306").toString();
        String backupDir = params.getOrDefault("backupDir", "/var/backups/mysql").toString();

        script.append("// Connect to MySQL\n");
        script.append("\\connect ").append(uri).append("\n\n");

        script.append("// Create a backup\n");
        script.append("util.dumpInstance(\"").append(backupDir).append("\", {\n");
        script.append("  ocimds: ").append(params.getOrDefault("ocimds", false)).append(",\n");
        script.append("  compatibility: \"").append(params.getOrDefault("compatibility", "8.0.26")).append("\",\n");
        script.append("  threads: ").append(params.getOrDefault("threads", 4)).append(",\n");
        script.append("  includeTriggers: ").append(params.getOrDefault("includeTriggers", true)).append(",\n");
        script.append("  includeUsers: ").append(params.getOrDefault("includeUsers", true)).append(",\n");
        script.append("  compression: \"").append(params.getOrDefault("compression", "zstd")).append("\"\n");
        script.append("});\n\n");

        script.append("print('Backup completed successfully.');\n");
    }

    private void generateRestoreScript(StringBuilder script, Map<String, Object> params) {
        String backupFile = (String) params.getOrDefault("backupFile", "");
        String database = (String) params.getOrDefault("database", "");
        
        script.append("# 数据恢复脚本\n");
        script.append("mysql -u ${MYSQL_USER} -p${MYSQL_PASSWORD} ").append(database)
              .append(" < ").append(backupFile).append("\n\n");
        script.append("# 验证恢复结果\n");
        script.append("mysql -u ${MYSQL_USER} -p${MYSQL_PASSWORD} -e \"USE ")
              .append(database).append("; SHOW TABLES;\"\n");
    }

    private void generateClusterSetupScript(StringBuilder script, Map<String, Object> params) {
        List<String> instances = (List<String>)params.getOrDefault("instances", Arrays.asList("root@mysql1:3306", "root@mysql2:3306", "root@mysql3:3306"));
        String clusterName = params.getOrDefault("clusterName", "myCluster").toString();

        script.append("// Setup InnoDB Cluster\n\n");
        script.append("// Connect to primary instance\n");
        script.append("\\connect ").append(instances.get(0)).append("\n\n");

        script.append("// Create the cluster\n");
        script.append("var cluster = dba.createCluster('").append(clusterName).append("', {\n");
        script.append("  multiPrimary: ").append(params.getOrDefault("multiPrimary", false)).append(",\n");
        script.append("  force: ").append(params.getOrDefault("force", false)).append(",\n");
        script.append("  manualStartOnBoot: ").append(params.getOrDefault("manualStartOnBoot", false)).append("\n");
        script.append("});\n\n");

        script.append("// Add the remaining instances\n");
        for (int i = 1; i < instances.size(); i++) {
            script.append("cluster.addInstance('").append(instances.get(i)).append("', {\n");
            script.append("  recoveryMethod: '").append(params.getOrDefault("recoveryMethod", "clone")).append("'\n");
            script.append("});\n\n");
        }

        script.append("// Setup Router\n");
        script.append("cluster.setupRouterAccount('router@%');\n\n");

        script.append("// Check cluster status\n");
        script.append("print(cluster.status());\n");
    }

    private void generateHealthCheckScript(StringBuilder script, Map<String, Object> params) {
        script.append("# MySQL健康检查脚本\n");
        script.append("mysql -u ${MYSQL_USER} -p${MYSQL_PASSWORD} -e \"SHOW STATUS; SHOW VARIABLES;\"\n");
        script.append("mysql -u ${MYSQL_USER} -p${MYSQL_PASSWORD} -e \"SHOW ENGINE INNODB STATUS\\G\"\n");
        script.append("mysql -u ${MYSQL_USER} -p${MYSQL_PASSWORD} -e \"SHOW PROCESSLIST;\"\n");
    }

    private void generateDataMigrationScript(StringBuilder script, Map<String, Object> params) {
        String sourceDb = (String) params.getOrDefault("sourceDb", "");
        String targetDb = (String) params.getOrDefault("targetDb", "");
        List<String> tables = (List<String>) params.getOrDefault("tables", new ArrayList<>());
        
        script.append("# 数据迁移脚本\n");
        script.append("# 创建目标数据库\n");
        script.append("mysql -u ${MYSQL_USER} -p${MYSQL_PASSWORD} -e \"CREATE DATABASE IF NOT EXISTS ")
              .append(targetDb).append(";\"\n\n");
        
        for (String table : tables) {
            script.append("# 迁移表: ").append(table).append("\n");
            script.append("mysqldump -u ${MYSQL_USER} -p${MYSQL_PASSWORD} ")
                  .append(sourceDb).append(" ").append(table)
                  .append(" | mysql -u ${MYSQL_USER} -p${MYSQL_PASSWORD} ")
                  .append(targetDb).append("\n");
        }
        
        script.append("\n# 验证迁移结果\n");
        script.append("mysql -u ${MYSQL_USER} -p${MYSQL_PASSWORD} -e \"USE ")
              .append(targetDb).append("; SHOW TABLES;\"\n");
    }

    /**
     * 获取备份策略建议
     * @param tableNames 表名列表
     * @return 备份策略建议
     */
    public Map<String, Object> getBackupStrategy(List<String> tableNames) throws Exception {
        Map<String, Object> strategy = new HashMap<>();
        List<Map<String, Object>> tables = new ArrayList<>();
        long totalSize = 0;

        // 分析每个表
        for (String tableName : tableNames) {
            Map<String, Object> tableInfo = new HashMap<>();
            tableInfo.put("name", tableName);

            // 获取表大小
            Long tableSize = getTableSize(tableName);
            tableInfo.put("size", tableSize);
            totalSize += tableSize != null ? tableSize : 0;

            // 获取表行数
            Long rowCount = getTableRowCount(tableName);
            tableInfo.put("rowCount", rowCount);

            // 获取更新频率（如果有更新时间统计）
            try {
                Map<String, Object> updateFreq = getTableUpdateFrequency(tableName);
                tableInfo.put("updateFrequency", updateFreq);
            } catch (Exception e) {
                tableInfo.put("updateFrequency", "Unknown");
            }

            // 检查是否有外键关系
            List<Map<String, Object>> fks = getForeignKeys(tableName);
            tableInfo.put("hasForeignKeys", !fks.isEmpty());

            tables.add(tableInfo);
        }

        strategy.put("tables", tables);
        strategy.put("totalSize", totalSize);

        // 备份建议
        Map<String, Object> recommendations = new HashMap<>();

        // 逻辑备份方式推荐
        if (totalSize < 10 * 1024 * 1024 * 1024L) { // 小于10GB
            recommendations.put("logicalBackup", "使用mysqldump进行逻辑备份适合该数据量");
        } else {
            recommendations.put("logicalBackup", "数据量较大，建议使用MySQL Enterprise Backup或Percona XtraBackup进行物理备份");
        }

        // 备份频率建议
        boolean hasFrequentUpdates = false;
        for (Map<String, Object> table : tables) {
            Map<String, Object> updateFreq = (Map<String, Object>) table.get("updateFrequency");
            if (updateFreq instanceof Map && updateFreq.containsKey("updatesPerDay")) {
                Object updatesPerDay = updateFreq.get("updatesPerDay");
                if (updatesPerDay instanceof Number && ((Number) updatesPerDay).doubleValue() > 1000) {
                    hasFrequentUpdates = true;
                    break;
                }
            }
        }

        if (hasFrequentUpdates) {
            recommendations.put("backupFrequency", "数据更新频繁，建议启用二进制日志并每天进行一次完整备份，辅以每小时的增量备份");
        } else {
            recommendations.put("backupFrequency", "数据更新较少，建议每天进行一次完整备份");
        }

        // 备份工具建议
        if (totalSize > 50 * 1024 * 1024 * 1024L) { // 大于50GB
            recommendations.put("backupTool", "数据量较大，建议使用Percona XtraBackup或MySQL Enterprise Backup进行物理备份");
        } else {
            recommendations.put("backupTool", "mysqldump足以满足需求，配合--single-transaction选项确保一致性");
        }

        // 存储建议
        recommendations.put("storage", "建议将备份存储在与数据库服务器不同的物理位置，并考虑异地备份");

        // 测试恢复建议
        recommendations.put("recoveryTesting", "建议每月至少测试一次备份恢复流程，确保备份可用");

        strategy.put("recommendations", recommendations);

        return strategy;
    }

    /**
     * 生成备份恢复计划
     * @param database 数据库名
     * @param tables 表名列表
     * @param backupDir 备份目录
     * @return 备份恢复计划
     */
    public Map<String, String> generateBackupRecoveryPlan(String database, List<String> tables, String backupDir) {
        Map<String, String> plan = new HashMap<>();

        // 1. 完整备份命令
        StringBuilder fullBackupCmd = new StringBuilder();
        fullBackupCmd.append("mysqldump -u${USER} -p${PASSWORD} --single-transaction --quick --lock-tables=false ");
        fullBackupCmd.append("--set-gtid-purged=ON --triggers --routines --events ");
        fullBackupCmd.append(database).append(" ");

        if (tables != null && !tables.isEmpty()) {
            fullBackupCmd.append(String.join(" ", tables));
        }

        fullBackupCmd.append(" | gzip > ").append(backupDir).append("/");
        fullBackupCmd.append(database).append("_full_$(date +%Y%m%d_%H%M%S).sql.gz");

        plan.put("fullBackupCommand", fullBackupCmd.toString());

        // 2. 增量备份命令 (二进制日志)
        StringBuilder incrementalBackupCmd = new StringBuilder();
        incrementalBackupCmd.append("mysql -u${USER} -p${PASSWORD} -e \"FLUSH BINARY LOGS;\"");

        plan.put("incrementalBackupCommand", incrementalBackupCmd.toString());

        // 3. 二进制日志备份
        plan.put("binlogBackupCommand", "cp /var/lib/mysql/mysql-bin.* " + backupDir + "/binlog/");

        // 4. 恢复完整备份
        StringBuilder restoreFullBackupCmd = new StringBuilder();
        restoreFullBackupCmd.append("gunzip < ").append(backupDir).append("/[BACKUP_FILE].sql.gz | ");
        restoreFullBackupCmd.append("mysql -u${USER} -p${PASSWORD} ").append(database);

        plan.put("restoreFullBackupCommand", restoreFullBackupCmd.toString());

        // 5. 应用二进制日志
        StringBuilder applyBinlogCmd = new StringBuilder();
        applyBinlogCmd.append("mysqlbinlog ");
        applyBinlogCmd.append(backupDir).append("/binlog/mysql-bin.[NUMBER] | ");
        applyBinlogCmd.append("mysql -u${USER} -p${PASSWORD} ").append(database);

        plan.put("applyBinlogCommand", applyBinlogCmd.toString());

        // 6. 备份验证命令
        StringBuilder verifyBackupCmd = new StringBuilder();
        verifyBackupCmd.append("gunzip < ").append(backupDir).append("/[BACKUP_FILE].sql.gz | ");
        verifyBackupCmd.append("grep -v \"^--\" | grep -v \"^/$\" | wc -l");

        plan.put("verifyBackupCommand", verifyBackupCmd.toString());

        // 7. Cron 任务示例
        plan.put("cronJobExample", "0 2 * * * /path/to/backup_script.sh > /var/log/mysql_backup.log 2>&1");

        return plan;
    }

    /**
     * 生成表数据压缩SQL
     * @param tableName 表名
     * @param compressionAlgorithm 压缩算法 (ZLIB, LZ4, NONE)
     * @return 表压缩SQL
     */
    public String getTableCompressionSql(String tableName, String compressionAlgorithm) {
        StringBuilder sql = new StringBuilder();

        // 获取表的ROW_FORMAT和KEY_BLOCK_SIZE
        String rowFormat = "COMPRESSED";
        int keyBlockSize = 8; // 默认值

        if ("LZ4".equalsIgnoreCase(compressionAlgorithm)) {
            // LZ4压缩在MySQL 8.0.20+可用，通常不需要特定的KEY_BLOCK_SIZE
            keyBlockSize = 0;
        } else if ("NONE".equalsIgnoreCase(compressionAlgorithm)) {
            rowFormat = "DYNAMIC";
            keyBlockSize = 0;
        }

        sql.append("ALTER TABLE ").append(wrapIdentifier(tableName)).append(" ");

        if (keyBlockSize > 0) {
            sql.append("KEY_BLOCK_SIZE=").append(keyBlockSize).append(" ");
        }

        sql.append("ROW_FORMAT=").append(rowFormat);

        return sql.toString();
    }

    /**
     * 获取归档表SQL (将旧数据移至归档表)
     * @param sourceTable 源表
     * @param archiveTable 归档表
     * @param whereClause 归档条件 (例如 "created_date < DATE_SUB(NOW(), INTERVAL 1 YEAR)")
     * @param options 归档选项
     * @return 归档SQL
     */
    public Map<String, String> getArchiveTableSql(String sourceTable, String archiveTable,
                                                  String whereClause, Map<String, Object> options) {
        Map<String, String> sqlCommands = new HashMap<>();

        // 1. 创建归档表 (如果不存在)
        StringBuilder createArchiveTable = new StringBuilder();
        createArchiveTable.append("CREATE TABLE IF NOT EXISTS ").append(wrapIdentifier(archiveTable)).append(" LIKE ");
        createArchiveTable.append(wrapIdentifier(sourceTable)).append(";");

        sqlCommands.put("createArchiveTable", createArchiveTable.toString());

        // 2. 移动数据到归档表
        StringBuilder moveData = new StringBuilder();
        moveData.append("INSERT INTO ").append(wrapIdentifier(archiveTable)).append(" ");
        moveData.append("SELECT * FROM ").append(wrapIdentifier(sourceTable)).append(" ");
        moveData.append("WHERE ").append(whereClause).append(";");

        sqlCommands.put("moveData", moveData.toString());

        // 3. 从源表删除数据
        if (Boolean.TRUE.equals(options.getOrDefault("removeFromSource", true))) {
            StringBuilder deleteData = new StringBuilder();
            deleteData.append("DELETE FROM ").append(wrapIdentifier(sourceTable)).append(" ");
            deleteData.append("WHERE ").append(whereClause).append(";");

            sqlCommands.put("deleteData", deleteData.toString());
        }

        // 4. 可选: 压缩归档表
        if (Boolean.TRUE.equals(options.getOrDefault("compressArchive", true))) {
            String compressionSql = getTableCompressionSql(archiveTable, options.getOrDefault("compressionAlgorithm", "ZLIB").toString());
            sqlCommands.put("compressArchiveTable", compressionSql);
        }

        // 5. 可选: 添加存储过程自动归档
        if (Boolean.TRUE.equals(options.getOrDefault("createRoutine", false))) {
            StringBuilder createRoutine = new StringBuilder();
            createRoutine.append("DELIMITER //\n");
            createRoutine.append("CREATE PROCEDURE archive_").append(sourceTable).append("()\n");
            createRoutine.append("BEGIN\n");
            createRoutine.append("    -- 移动数据到归档表\n");
            createRoutine.append("    INSERT INTO ").append(wrapIdentifier(archiveTable)).append(" \n");
            createRoutine.append("    SELECT * FROM ").append(wrapIdentifier(sourceTable)).append(" \n");
            createRoutine.append("    WHERE ").append(whereClause).append(";\n");
            createRoutine.append("    \n");
            createRoutine.append("    -- 从源表删除数据\n");
            if (Boolean.TRUE.equals(options.getOrDefault("removeFromSource", true))) {
                createRoutine.append("    DELETE FROM ").append(wrapIdentifier(sourceTable)).append(" \n");
                createRoutine.append("    WHERE ").append(whereClause).append(";\n");
            }
            createRoutine.append("END //\n");
            createRoutine.append("DELIMITER ;\n");

            sqlCommands.put("createRoutine", createRoutine.toString());
        }

        // 6. 可选: 创建事件定期归档
        if (Boolean.TRUE.equals(options.getOrDefault("createEvent", false))) {
            String schedule = options.getOrDefault("schedule", "EVERY 1 MONTH").toString();

            StringBuilder createEvent = new StringBuilder();
            createEvent.append("DELIMITER //\n");
            createEvent.append("CREATE EVENT archive_").append(sourceTable).append("_event\n");
            createEvent.append("ON SCHEDULE ").append(schedule).append("\n");
            createEvent.append("DO\n");
            createEvent.append("BEGIN\n");
            createEvent.append("    CALL archive_").append(sourceTable).append("();\n");
            createEvent.append("END //\n");
            createEvent.append("DELIMITER ;\n");

            sqlCommands.put("createEvent", createEvent.toString());
        }

        return sqlCommands;
    }

    /**
     * 检查SQL语句的MySQL版本兼容性
     * @param sql SQL语句
     * @param targetVersion 目标MySQL版本（例如"5.7"、"8.0"）
     * @return 兼容性检查结果
     */
    public Map<String, Object> checkSqlCompatibility(String sql, String targetVersion) {
        Map<String, Object> result = new HashMap<>();
        List<String> issues = new ArrayList<>();
        boolean compatible = true;

        // 将目标版本转换为数字，便于比较
        double version = Double.parseDouble(targetVersion);

        // 检查MySQL 8.0+特性
        if (version < 8.0) {
            // 检查窗口函数（MySQL 8.0引入）
            if (sql.toLowerCase().contains("over (")) {
                issues.add("窗口函数（OVER子句）仅在MySQL 8.0及以上版本支持");
                compatible = false;
            }

            // 检查公共表表达式（CTE）（MySQL 8.0引入）
            if (sql.toLowerCase().contains("with ") &&
                    (sql.toLowerCase().contains(" as (select") || sql.toLowerCase().contains(" as(select"))) {
                issues.add("公共表表达式（WITH...AS）仅在MySQL 8.0及以上版本支持");
                compatible = false;
            }

            // 检查降序索引（MySQL 8.0引入）
            if (sql.toLowerCase().contains("desc") &&
                    (sql.toLowerCase().contains("create index") || sql.toLowerCase().contains("add index"))) {
                issues.add("降序索引仅在MySQL 8.0及以上版本支持");
                compatible = false;
            }

            // 检查不可见索引（MySQL 8.0引入）
            if (sql.toLowerCase().contains("invisible") &&
                    (sql.toLowerCase().contains("create index") || sql.toLowerCase().contains("add index"))) {
                issues.add("不可见索引仅在MySQL 8.0及以上版本支持");
                compatible = false;
            }

            // 检查JSON表函数（MySQL 8.0引入）
            if (sql.toLowerCase().contains("json_table")) {
                issues.add("JSON_TABLE函数仅在MySQL 8.0及以上版本支持");
                compatible = false;
            }
        }

        // 检查MySQL 5.7+特性
        if (version < 5.7) {
            // 检查JSON数据类型（MySQL 5.7引入）
            if (sql.toLowerCase().contains("json")) {
                issues.add("JSON数据类型仅在MySQL 5.7及以上版本支持");
                compatible = false;
            }

            // 检查生成列（MySQL 5.7引入）
            if (sql.toLowerCase().contains("generated") && sql.toLowerCase().contains("as")) {
                issues.add("生成列（GENERATED...AS）仅在MySQL 5.7及以上版本支持");
                compatible = false;
            }
        }

        result.put("compatible", compatible);
        result.put("targetVersion", targetVersion);
        result.put("issues", issues);

        return result;
    }

    /**
     * 获取从当前MySQL版本迁移到目标版本的注意事项
     * @param currentVersion 当前MySQL版本
     * @param targetVersion 目标MySQL版本
     * @return 迁移注意事项
     */
    public Map<String, Object> getMigrationConsiderations(String currentVersion, String targetVersion) {
        Map<String, Object> result = new HashMap<>();
        List<String> considerations = new ArrayList<>();
        List<String> deprecatedFeatures = new ArrayList<>();
        List<String> newFeatures = new ArrayList<>();

        double current = Double.parseDouble(currentVersion);
        double target = Double.parseDouble(targetVersion);

        // 从旧版本升级到新版本
        if (current < target) {
            if (current < 5.7 && target >= 5.7) {
                considerations.add("升级到MySQL 5.7+需要注意utf8mb4字符集变为默认推荐");
                considerations.add("MySQL 5.7+中InnoDB成为默认存储引擎，并且不可更改");
                newFeatures.add("JSON数据类型支持");
                newFeatures.add("生成列（Generated Columns）");
                newFeatures.add("InnoDB全文索引改进");
            }

            if (current < 8.0 && target >= 8.0) {
                considerations.add("MySQL 8.0采用新的数据字典，升级前必须解决所有外键约束错误");
                considerations.add("MySQL 8.0默认字符集从latin1变更为utf8mb4");
                considerations.add("MySQL 8.0引入了强制性的SHA256密码加密，需要更新客户端");
                deprecatedFeatures.add("移除了query cache功能");
                newFeatures.add("窗口函数（Window Functions）");
                newFeatures.add("公共表表达式（CTE），包括递归查询");
                newFeatures.add("降序索引和不可见索引");
                newFeatures.add("JSON功能增强（JSON_TABLE等）");
                newFeatures.add("原子DDL");
                newFeatures.add("资源组管理");
                newFeatures.add("角色管理");
            }
        }
        // 从新版本降级到旧版本
        else if (current > target) {
            considerations.add("警告：降级MySQL版本通常不受官方支持，可能导致数据丢失或损坏");
            considerations.add("建议：在降级前进行完整备份并在测试环境验证降级过程");

            if (current >= 8.0 && target < 8.0) {
                considerations.add("MySQL 8.0使用新的数据字典，降级到更早版本需要额外步骤");
                considerations.add("需要移除所有使用8.0特有功能的对象（如角色、窗口函数等）");
                deprecatedFeatures.add("窗口函数将不可用");
                deprecatedFeatures.add("递归CTE将不可用");
                deprecatedFeatures.add("角色管理功能将不可用");
            }

            if (current >= 5.7 && target < 5.7) {
                considerations.add("需要移除所有JSON类型的列和JSON函数");
                considerations.add("需要移除所有生成列");
                deprecatedFeatures.add("JSON数据类型将不可用");
                deprecatedFeatures.add("生成列将不可用");
            }
        }

        result.put("currentVersion", currentVersion);
        result.put("targetVersion", targetVersion);
        result.put("considerations", considerations);
        result.put("deprecatedFeatures", deprecatedFeatures);
        result.put("newFeatures", newFeatures);

        return result;
    }

    /**
     * 获取适合云环境的MySQL配置建议
     * @param environment 云环境类型（例如"aws", "azure", "gcp", "k8s"）
     * @param instanceSize 实例大小（例如"small", "medium", "large"）
     * @return 配置建议
     */
    public Map<String, Object> getCloudMySQLConfiguration(String environment, String instanceSize) {
        Map<String, Object> configuration = new HashMap<>();
        Map<String, Object> variables = new HashMap<>();
        Map<String, String> flags = new HashMap<>();
        List<String> recommendations = new ArrayList<>();

        // 基本设置
        variables.put("innodb_buffer_pool_size", getBufferPoolSize(instanceSize));
        variables.put("max_connections", getMaxConnections(instanceSize));
        variables.put("innodb_flush_log_at_trx_commit", 1); // 默认安全设置

        // 根据环境进行特殊优化
        switch (environment.toLowerCase()) {
            case "aws":
                recommendations.add("使用RDS参数组而非自定义my.cnf");
                recommendations.add("启用Performance Insights监控性能");
                if (instanceSize.equals("large")) {
                    variables.put("innodb_flush_neighbors", 0); // 在高性能存储上禁用相邻页刷新
                }
                flags.put("slow_query_log", "ON");
                break;

            case "azure":
                recommendations.add("使用Azure Database for MySQL的服务器参数");
                recommendations.add("启用查询性能洞察功能");
                recommendations.add("使用Flexible Server以获取更多控制权");
                variables.put("wait_timeout", 120); // 减少空闲连接的超时时间
                break;

            case "gcp":
                recommendations.add("使用Cloud SQL标志配置而非自定义my.cnf");
                recommendations.add("启用高可用性配置");
                recommendations.add("使用私有IP连接以提高安全性");
                variables.put("tmp_table_size", "64M"); // 增加临时表大小
                variables.put("max_heap_table_size", "64M");
                break;

            case "k8s":
                recommendations.add("使用StatefulSet部署MySQL以保证稳定网络标识");
                recommendations.add("使用PersistentVolumeClaims确保数据持久性");
                recommendations.add("考虑使用MySQL Operator简化管理");
                variables.put("innodb_flush_log_at_trx_commit", 2); // K8s环境下适当平衡性能与安全
                variables.put("innodb_buffer_pool_instances", 4);
                flags.put("bind-address", "0.0.0.0"); // 允许从容器外部访问
                break;

            default:
                recommendations.add("未识别的环境类型，提供通用云设置");
        }

        // 所有云环境的共同建议
        recommendations.add("启用自动备份并验证恢复过程");
        recommendations.add("实施连接池以管理数据库连接");
        recommendations.add("使用读写分离提高读取性能");
        recommendations.add("监控慢查询并优化问题SQL");

        configuration.put("variables", variables);
        configuration.put("flags", flags);
        configuration.put("recommendations", recommendations);

        return configuration;
    }

    /**
     * 根据实例大小获取合适的缓冲池大小
     */
    private String getBufferPoolSize(String instanceSize) {
        switch (instanceSize.toLowerCase()) {
            case "micro":
                return "128M";
            case "small":
                return "1G";
            case "medium":
                return "4G";
            case "large":
                return "16G";
            case "xlarge":
                return "64G";
            default:
                return "1G";
        }
    }

    /**
     * 根据实例大小获取最大连接数
     */
    private int getMaxConnections(String instanceSize) {
        switch (instanceSize.toLowerCase()) {
            case "micro":
                return 50;
            case "small":
                return 100;
            case "medium":
                return 500;
            case "large":
                return 1000;
            case "xlarge":
                return 5000;
            default:
                return 151;  // MySQL默认值
        }
    }

    /**
     * 获取基于查询模式的智能索引建议
     * @param tableName 表名
     * @param days 分析最近几天的查询
     * @return 索引建议
     */
    public Map<String, Object> getSmartIndexRecommendations(String tableName, int days) throws Exception {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> recommendations = new ArrayList<>();

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. 获取慢查询日志中的查询模式
            List<String> slowQueries = new ArrayList<>();
            String slowQuerySql = "SELECT query, count(*) as count, avg(query_time) as avg_time " +
                    "FROM mysql.slow_log " +
                    "WHERE start_time > DATE_SUB(NOW(), INTERVAL " + days + " DAY) " +
                    "AND query LIKE '%FROM " + tableName + "%' " +
                    "GROUP BY query " +
                    "ORDER BY count DESC, avg_time DESC " +
                    "LIMIT 20";

            try (ResultSet rs = stmt.executeQuery(slowQuerySql)) {
                while (rs.next()) {
                    slowQueries.add(rs.getString("query"));
                }
            } catch (SQLException e) {
                // 慢查询表可能不可用，使用其他方法
                slowQueries = getFallbackQueryList(tableName, days);
            }

            // 2. 分析每个慢查询
            for (String query : slowQueries) {
                Map<String, Object> recommendation = analyzeQueryForIndexing(query, tableName);
                if (recommendation != null && !recommendation.isEmpty()) {
                    recommendations.add(recommendation);
                }
            }

            // 3. 获取表的现有索引
            List<Map<String, Object>> existingIndexes = getIndexes(tableName);

            // 4. 过滤掉与现有索引重复的建议
            List<Map<String, Object>> filteredRecommendations = new ArrayList<>();
            for (Map<String, Object> recommendation : recommendations) {
                boolean isDuplicate = false;
                List<String> suggestedColumns = (List<String>)recommendation.get("columns");

                for (Map<String, Object> existing : existingIndexes) {
                    String indexColumns = (String)existing.get("Column_name");
                    List<String> existingCols = Arrays.asList(indexColumns.split(","));

                    // 检查前缀匹配
                    if (existingCols.size() >= suggestedColumns.size()) {
                        boolean prefixMatch = true;
                        for (int i = 0; i < suggestedColumns.size(); i++) {
                            if (!suggestedColumns.get(i).equals(existingCols.get(i))) {
                                prefixMatch = false;
                                break;
                            }
                        }
                        if (prefixMatch) {
                            isDuplicate = true;
                            break;
                        }
                    }
                }

                if (!isDuplicate) {
                    filteredRecommendations.add(recommendation);
                }
            }

            // 5. 计算每个建议的相对提升
            for (Map<String, Object> recommendation : filteredRecommendations) {
                String suggestedIndexSql = (String)recommendation.get("createIndexSql");
                double originalCost = estimateQueryCost((String)recommendation.get("query"));

                // 创建临时索引（仅在EXPLAIN中）并估算成本
                String explainWithIndexSql = "EXPLAIN FORMAT=JSON SELECT /*+ " +
                        "INDEX(" + tableName + " tmp_idx) */ * " +
                        "FROM " + wrapIdentifier(tableName) + " " +
                        "WHERE " + recommendation.get("whereClause");

                double newCost = estimateQueryCostWithHypotheticalIndex(explainWithIndexSql);
                double improvement = 0;
                if (originalCost > 0) {
                    improvement = (originalCost - newCost) / originalCost * 100.0;
                }

                recommendation.put("estimatedImprovement", Math.round(improvement) + "%");
            }

            // 6. 按预计改进程度排序
            Collections.sort(filteredRecommendations, (a, b) -> {
                String aImprovement = (String)a.get("estimatedImprovement");
                String bImprovement = (String)b.get("estimatedImprovement");
                return Integer.parseInt(bImprovement.replace("%", "")) -
                        Integer.parseInt(aImprovement.replace("%", ""));
            });

            result.put("tableName", tableName);
            result.put("analyzedDays", days);
            result.put("recommendations", filteredRecommendations);
            result.put("existingIndexes", existingIndexes);
        }

        return result;
    }

    /**
     * 分析查询以提取索引建议
     */
    private Map<String, Object> analyzeQueryForIndexing(String query, String tableName) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 使用正则表达式提取WHERE子句
            Pattern wherePattern = Pattern.compile("WHERE\\s+(.+?)(?:ORDER BY|GROUP BY|LIMIT|$)",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher whereMatcher = wherePattern.matcher(query);

            if (whereMatcher.find()) {
                String whereClause = whereMatcher.group(1).trim();
                List<String> columns = extractColumnsFromWhereClause(whereClause, tableName);

                if (!columns.isEmpty()) {
                    String indexName = "idx_" + tableName + "_" + String.join("_", columns);
                    if (indexName.length() > 64) {
                        // MySQL索引名最大长度为64
                        indexName = "idx_" + tableName + "_auto_" + Math.abs(columns.hashCode());
                    }

                    String createIndexSql = getAddIndexSql(tableName, indexName, columns, false);

                    result.put("indexName", indexName);
                    result.put("columns", columns);
                    result.put("whereClause", whereClause);
                    result.put("query", query);
                    result.put("createIndexSql", createIndexSql);
                }
            }
        } catch (Exception e) {
            // 解析失败，返回空结果
        }

        return result;
    }

    // 添加getFallbackQueryList方法
    private List<String> getFallbackQueryList(String tableName, int days) {
        List<String> queries = new ArrayList<>();
        
        // 获取最近days天内的常见查询
        String sql = "SELECT digest_text FROM performance_schema.events_statements_summary_by_digest " +
                     "WHERE schema_name = ? AND digest_text LIKE ? " +
                     "AND FIRST_SEEN > DATE_SUB(NOW(), INTERVAL ? DAY) " +
                     "ORDER BY count_star DESC LIMIT 10";
                 
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, getDatabaseName());
            stmt.setString(2, "%FROM%`" + tableName + "`%WHERE%");
            stmt.setInt(3, days);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    queries.add(rs.getString("digest_text"));
                }
            }
        } catch (Exception e) {
            // 如果无法访问performance_schema，返回一些通用查询模式
            queries.add("SELECT * FROM " + tableName + " WHERE id = ?");
            queries.add("SELECT * FROM " + tableName + " WHERE created_at BETWEEN ? AND ?");
            queries.add("SELECT * FROM " + tableName + " WHERE status = ?");
        }
        
        return queries;
    }

    // 添加estimateQueryCost方法
    private double estimateQueryCost(String query) {
        double cost = 0.0;
        
        String explainSql = "EXPLAIN FORMAT=JSON " + query;
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(explainSql)) {
            
            if (rs.next()) {
                String explainOutput = rs.getString(1);
                JSONObject explainJson = new JSONObject(explainOutput);
                
                // 从JSON中提取查询成本
                JSONObject queryBlock = explainJson.getJSONObject("query_block");
                if (queryBlock.has("cost_info")) {
                    JSONObject costInfo = queryBlock.getJSONObject("cost_info");
                    cost = costInfo.getDouble("query_cost");
                }
            }
        } catch (Exception e) {
            // 解析失败时返回一个高成本值
            return 1000.0;
        }
        
        return cost;
    }

    // 添加estimateQueryCostWithHypotheticalIndex方法
    private double estimateQueryCostWithHypotheticalIndex(String query) {
        // 注意：MySQL不直接支持假设性索引评估
        // 这里我们使用一个近似方法：假设成本可能会降低30%-50%
        double originalCost = estimateQueryCost(query);
        return originalCost * 0.5; // 假设成本降低50%
    }

    // 添加extractColumnsFromWhereClause方法
    private List<String> extractColumnsFromWhereClause(String query, String tableName) {
        List<String> columns = new ArrayList<>();
        
        // 这是一个简化的实现，实际上解析SQL需要更复杂的逻辑
        // 以下是一个简单的正则表达式匹配示例
        
        // 提取WHERE子句
        Pattern wherePattern = Pattern.compile("WHERE\\s+(.+?)(?:ORDER BY|GROUP BY|LIMIT|$)", 
                                               Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher whereMatcher = wherePattern.matcher(query);
        
        if (whereMatcher.find()) {
            String whereClause = whereMatcher.group(1).trim();
            
            // 查找列名和比较操作
            Pattern columnPattern = Pattern.compile(
                "`?" + tableName + "`?\\.`?(\\w+)`?\\s*(?:=|>|<|>=|<=|LIKE|IN|BETWEEN)",
                Pattern.CASE_INSENSITIVE
            );
            Matcher columnMatcher = columnPattern.matcher(whereClause);
            
            while (columnMatcher.find()) {
                columns.add(columnMatcher.group(1));
            }
            
            // 也匹配没有表名前缀的列
            Pattern simpleColumnPattern = Pattern.compile(
                "(?<!\\w)`?(\\w+)`?\\s*(?:=|>|<|>=|<=|LIKE|IN|BETWEEN)",
                Pattern.CASE_INSENSITIVE
            );
            Matcher simpleColumnMatcher = simpleColumnPattern.matcher(whereClause);
            
            while (simpleColumnMatcher.find()) {
                columns.add(simpleColumnMatcher.group(1));
            }
        }
        
        return columns;
    }
}
