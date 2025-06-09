package com.mango.test.database.service.impl.datasource.handlers;

import com.mango.test.database.entity.ColumnDefinition;
import com.mango.test.database.entity.DataSource;
import com.mango.test.database.entity.TableDefinition;
import com.mango.test.database.service.impl.datasource.AbstractDatabaseHandler;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PostgreSQLHandler extends AbstractDatabaseHandler {

    private static final String DRIVER_CLASS = "org.postgresql.Driver";
    private static final String DEFAULT_PORT = "5432";
    private static final String URL_TEMPLATE = "jdbc:postgresql://%s:%s/%s";

    private static final int DEFAULT_VARCHAR_LENGTH = 255;
    private static final int MAX_VARCHAR_LENGTH = 10485760;

    private static final Map<String, String> TYPE_MAPPING = new HashMap<>();
    private static final Map<String, Integer> DEFAULT_LENGTH_MAPPING = new HashMap<>();
    private static final Map<String, Map<String, String>> COMMON_TYPE_MAPPING = new HashMap<>();

    static {
        // 基本类型映射
        TYPE_MAPPING.put("string", "VARCHAR");
        TYPE_MAPPING.put("text", "TEXT");
        TYPE_MAPPING.put("int", "INTEGER");
        TYPE_MAPPING.put("bigint", "BIGINT");
        TYPE_MAPPING.put("float", "REAL");
        TYPE_MAPPING.put("double", "DOUBLE PRECISION");
        TYPE_MAPPING.put("decimal", "NUMERIC");
        TYPE_MAPPING.put("boolean", "BOOLEAN");
        TYPE_MAPPING.put("date", "DATE");
        TYPE_MAPPING.put("time", "TIME");
        TYPE_MAPPING.put("timestamp", "TIMESTAMP");
        TYPE_MAPPING.put("binary", "BYTEA");
        TYPE_MAPPING.put("json", "JSON");
        TYPE_MAPPING.put("array", "ARRAY");

        // MySQL 类型映射
        Map<String, String> mysqlMapping = new HashMap<>();
        mysqlMapping.put("VARCHAR", "VARCHAR");
        mysqlMapping.put("CHAR", "CHAR");
        mysqlMapping.put("TEXT", "TEXT");
        mysqlMapping.put("TINYTEXT", "TEXT");
        mysqlMapping.put("MEDIUMTEXT", "TEXT");
        mysqlMapping.put("LONGTEXT", "TEXT");
        mysqlMapping.put("TINYINT", "SMALLINT");
        mysqlMapping.put("SMALLINT", "SMALLINT");
        mysqlMapping.put("MEDIUMINT", "INTEGER");
        mysqlMapping.put("INT", "INTEGER");
        mysqlMapping.put("INTEGER", "INTEGER");
        mysqlMapping.put("BIGINT", "BIGINT");
        mysqlMapping.put("FLOAT", "REAL");
        mysqlMapping.put("DOUBLE", "DOUBLE PRECISION");
        mysqlMapping.put("DECIMAL", "NUMERIC");
        mysqlMapping.put("NUMERIC", "NUMERIC");
        mysqlMapping.put("BOOLEAN", "BOOLEAN");
        mysqlMapping.put("BOOL", "BOOLEAN");
        mysqlMapping.put("DATE", "DATE");
        mysqlMapping.put("TIME", "TIME");
        mysqlMapping.put("DATETIME", "TIMESTAMP");
        mysqlMapping.put("TIMESTAMP", "TIMESTAMP");
        mysqlMapping.put("BINARY", "BYTEA");
        mysqlMapping.put("VARBINARY", "BYTEA");
        mysqlMapping.put("BLOB", "BYTEA");
        mysqlMapping.put("TINYBLOB", "BYTEA");
        mysqlMapping.put("MEDIUMBLOB", "BYTEA");
        mysqlMapping.put("LONGBLOB", "BYTEA");
        mysqlMapping.put("JSON", "JSONB");
        mysqlMapping.put("ENUM", "VARCHAR");
        mysqlMapping.put("SET", "VARCHAR");
        COMMON_TYPE_MAPPING.put("mysql", mysqlMapping);

        // Oracle 类型映射
        Map<String, String> oracleMapping = new HashMap<>();
        oracleMapping.put("VARCHAR2", "VARCHAR");
        oracleMapping.put("NVARCHAR2", "VARCHAR");
        oracleMapping.put("CHAR", "CHAR");
        oracleMapping.put("NCHAR", "CHAR");
        oracleMapping.put("CLOB", "TEXT");
        oracleMapping.put("NCLOB", "TEXT");
        oracleMapping.put("NUMBER", "NUMERIC");
        oracleMapping.put("FLOAT", "DOUBLE PRECISION");
        oracleMapping.put("BINARY_FLOAT", "REAL");
        oracleMapping.put("BINARY_DOUBLE", "DOUBLE PRECISION");
        oracleMapping.put("DATE", "TIMESTAMP");
        oracleMapping.put("TIMESTAMP", "TIMESTAMP");
        oracleMapping.put("TIMESTAMP WITH TIME ZONE", "TIMESTAMP WITH TIME ZONE");
        oracleMapping.put("TIMESTAMP WITH LOCAL TIME ZONE", "TIMESTAMP WITH TIME ZONE");
        oracleMapping.put("INTERVAL YEAR TO MONTH", "INTERVAL YEAR TO MONTH");
        oracleMapping.put("INTERVAL DAY TO SECOND", "INTERVAL DAY TO SECOND");
        oracleMapping.put("RAW", "BYTEA");
        oracleMapping.put("LONG RAW", "BYTEA");
        oracleMapping.put("BLOB", "BYTEA");
        oracleMapping.put("BFILE", "BYTEA");
        oracleMapping.put("XMLTYPE", "XML");
        COMMON_TYPE_MAPPING.put("oracle", oracleMapping);

        // SQLServer 类型映射
        Map<String, String> sqlserverMapping = new HashMap<>();
        sqlserverMapping.put("VARCHAR", "VARCHAR");
        sqlserverMapping.put("NVARCHAR", "VARCHAR");
        sqlserverMapping.put("CHAR", "CHAR");
        sqlserverMapping.put("NCHAR", "CHAR");
        sqlserverMapping.put("TEXT", "TEXT");
        sqlserverMapping.put("NTEXT", "TEXT");
        sqlserverMapping.put("TINYINT", "SMALLINT");
        sqlserverMapping.put("SMALLINT", "SMALLINT");
        sqlserverMapping.put("INT", "INTEGER");
        sqlserverMapping.put("BIGINT", "BIGINT");
        sqlserverMapping.put("DECIMAL", "NUMERIC");
        sqlserverMapping.put("NUMERIC", "NUMERIC");
        sqlserverMapping.put("MONEY", "NUMERIC(19,4)");
        sqlserverMapping.put("SMALLMONEY", "NUMERIC(10,4)");
        sqlserverMapping.put("FLOAT", "DOUBLE PRECISION");
        sqlserverMapping.put("REAL", "REAL");
        sqlserverMapping.put("BIT", "BOOLEAN");
        sqlserverMapping.put("DATE", "DATE");
        sqlserverMapping.put("DATETIME", "TIMESTAMP");
        sqlserverMapping.put("DATETIME2", "TIMESTAMP");
        sqlserverMapping.put("SMALLDATETIME", "TIMESTAMP");
        sqlserverMapping.put("TIME", "TIME");
        sqlserverMapping.put("DATETIMEOFFSET", "TIMESTAMP WITH TIME ZONE");
        sqlserverMapping.put("BINARY", "BYTEA");
        sqlserverMapping.put("VARBINARY", "BYTEA");
        sqlserverMapping.put("IMAGE", "BYTEA");
        sqlserverMapping.put("XML", "XML");
        sqlserverMapping.put("UNIQUEIDENTIFIER", "UUID");
        COMMON_TYPE_MAPPING.put("sqlserver", sqlserverMapping);

        // DB2 类型映射
        Map<String, String> db2Mapping = new HashMap<>();
        db2Mapping.put("VARCHAR", "VARCHAR");
        db2Mapping.put("CHAR", "CHAR");
        db2Mapping.put("CLOB", "TEXT");
        db2Mapping.put("DBCLOB", "TEXT");
        db2Mapping.put("GRAPHIC", "CHAR");
        db2Mapping.put("VARGRAPHIC", "VARCHAR");
        db2Mapping.put("SMALLINT", "SMALLINT");
        db2Mapping.put("INTEGER", "INTEGER");
        db2Mapping.put("BIGINT", "BIGINT");
        db2Mapping.put("REAL", "REAL");
        db2Mapping.put("DOUBLE", "DOUBLE PRECISION");
        db2Mapping.put("DECIMAL", "NUMERIC");
        db2Mapping.put("NUMERIC", "NUMERIC");
        db2Mapping.put("DECFLOAT", "NUMERIC");
        db2Mapping.put("DATE", "DATE");
        db2Mapping.put("TIME", "TIME");
        db2Mapping.put("TIMESTAMP", "TIMESTAMP");
        db2Mapping.put("BLOB", "BYTEA");
        db2Mapping.put("XML", "XML");
        COMMON_TYPE_MAPPING.put("db2", db2Mapping);

        // MariaDB 类型映射 (基本与 MySQL 相同)
        Map<String, String> mariadbMapping = new HashMap<>(mysqlMapping);
        COMMON_TYPE_MAPPING.put("mariadb", mariadbMapping);

        // Sybase 类型映射
        Map<String, String> sybaseMapping = new HashMap<>();
        sybaseMapping.put("VARCHAR", "VARCHAR");
        sybaseMapping.put("CHAR", "CHAR");
        sybaseMapping.put("TEXT", "TEXT");
        sybaseMapping.put("UNITEXT", "TEXT");
        sybaseMapping.put("TINYINT", "SMALLINT");
        sybaseMapping.put("SMALLINT", "SMALLINT");
        sybaseMapping.put("INT", "INTEGER");
        sybaseMapping.put("BIGINT", "BIGINT");
        sybaseMapping.put("DECIMAL", "NUMERIC");
        sybaseMapping.put("NUMERIC", "NUMERIC");
        sybaseMapping.put("MONEY", "NUMERIC(19,4)");
        sybaseMapping.put("SMALLMONEY", "NUMERIC(10,4)");
        sybaseMapping.put("FLOAT", "DOUBLE PRECISION");
        sybaseMapping.put("REAL", "REAL");
        sybaseMapping.put("BIT", "BOOLEAN");
        sybaseMapping.put("DATE", "DATE");
        sybaseMapping.put("DATETIME", "TIMESTAMP");
        sybaseMapping.put("SMALLDATETIME", "TIMESTAMP");
        sybaseMapping.put("TIME", "TIME");
        sybaseMapping.put("BINARY", "BYTEA");
        sybaseMapping.put("VARBINARY", "BYTEA");
        sybaseMapping.put("IMAGE", "BYTEA");
        COMMON_TYPE_MAPPING.put("sybase", sybaseMapping);

        // H2 类型映射
        Map<String, String> h2Mapping = new HashMap<>();
        h2Mapping.put("VARCHAR", "VARCHAR");
        h2Mapping.put("CHAR", "CHAR");
        h2Mapping.put("CLOB", "TEXT");
        h2Mapping.put("TINYINT", "SMALLINT");
        h2Mapping.put("SMALLINT", "SMALLINT");
        h2Mapping.put("INT", "INTEGER");
        h2Mapping.put("INTEGER", "INTEGER");
        h2Mapping.put("BIGINT", "BIGINT");
        h2Mapping.put("DECIMAL", "NUMERIC");
        h2Mapping.put("NUMERIC", "NUMERIC");
        h2Mapping.put("FLOAT", "REAL");
        h2Mapping.put("DOUBLE", "DOUBLE PRECISION");
        h2Mapping.put("BOOLEAN", "BOOLEAN");
        h2Mapping.put("DATE", "DATE");
        h2Mapping.put("TIME", "TIME");
        h2Mapping.put("TIMESTAMP", "TIMESTAMP");
        h2Mapping.put("BINARY", "BYTEA");
        h2Mapping.put("BLOB", "BYTEA");
        h2Mapping.put("UUID", "UUID");
        COMMON_TYPE_MAPPING.put("h2", h2Mapping);

        // 国产数据库映射继续...
                // GaussDB/OpenGauss 类型映射
        Map<String, String> gaussMapping = new HashMap<>();
        gaussMapping.put("VARCHAR", "VARCHAR");
        gaussMapping.put("NVARCHAR", "VARCHAR");
        gaussMapping.put("CHAR", "CHAR");
        gaussMapping.put("NCHAR", "CHAR");
        gaussMapping.put("TEXT", "TEXT");
        gaussMapping.put("CLOB", "TEXT");
        gaussMapping.put("SMALLINT", "SMALLINT");
        gaussMapping.put("INTEGER", "INTEGER");
        gaussMapping.put("BIGINT", "BIGINT");
        gaussMapping.put("NUMERIC", "NUMERIC");
        gaussMapping.put("DECIMAL", "NUMERIC");
        gaussMapping.put("REAL", "REAL");
        gaussMapping.put("DOUBLE PRECISION", "DOUBLE PRECISION");
        gaussMapping.put("FLOAT", "DOUBLE PRECISION");
        gaussMapping.put("BOOLEAN", "BOOLEAN");
        gaussMapping.put("DATE", "DATE");
        gaussMapping.put("TIME", "TIME");
        gaussMapping.put("TIMESTAMP", "TIMESTAMP");
        gaussMapping.put("BINARY", "BYTEA");
        gaussMapping.put("BLOB", "BYTEA");
        gaussMapping.put("RAW", "BYTEA");
        gaussMapping.put("BYTEA", "BYTEA");
        gaussMapping.put("JSON", "JSON");
        gaussMapping.put("JSONB", "JSONB");
        gaussMapping.put("XML", "XML");
        COMMON_TYPE_MAPPING.put("gaussdb", gaussMapping);
        COMMON_TYPE_MAPPING.put("opengauss", gaussMapping);

        // 人大金仓 KingBase 类型映射
        Map<String, String> kingbaseMapping = new HashMap<>();
        kingbaseMapping.put("VARCHAR", "VARCHAR");
        kingbaseMapping.put("VARCHAR2", "VARCHAR");
        kingbaseMapping.put("NVARCHAR2", "VARCHAR");
        kingbaseMapping.put("CHAR", "CHAR");
        kingbaseMapping.put("NCHAR", "CHAR");
        kingbaseMapping.put("TEXT", "TEXT");
        kingbaseMapping.put("CLOB", "TEXT");
        kingbaseMapping.put("NUMBER", "NUMERIC");
        kingbaseMapping.put("NUMERIC", "NUMERIC");
        kingbaseMapping.put("DECIMAL", "NUMERIC");
        kingbaseMapping.put("INTEGER", "INTEGER");
        kingbaseMapping.put("INT", "INTEGER");
        kingbaseMapping.put("BIGINT", "BIGINT");
        kingbaseMapping.put("REAL", "REAL");
        kingbaseMapping.put("DOUBLE PRECISION", "DOUBLE PRECISION");
        kingbaseMapping.put("FLOAT", "DOUBLE PRECISION");
        kingbaseMapping.put("BINARY_FLOAT", "REAL");
        kingbaseMapping.put("BINARY_DOUBLE", "DOUBLE PRECISION");
        kingbaseMapping.put("BOOLEAN", "BOOLEAN");
        kingbaseMapping.put("DATE", "DATE");
        kingbaseMapping.put("TIME", "TIME");
        kingbaseMapping.put("TIMESTAMP", "TIMESTAMP");
        kingbaseMapping.put("BINARY", "BYTEA");
        kingbaseMapping.put("BLOB", "BYTEA");
        kingbaseMapping.put("RAW", "BYTEA");
        kingbaseMapping.put("LONG RAW", "BYTEA");
        kingbaseMapping.put("BYTEA", "BYTEA");
        kingbaseMapping.put("JSON", "JSON");
        kingbaseMapping.put("JSONB", "JSONB");
        kingbaseMapping.put("XML", "XML");
        COMMON_TYPE_MAPPING.put("kingbase", kingbaseMapping);

        // 神通数据库 ShenTong 类型映射
        Map<String, String> shentongMapping = new HashMap<>();
        shentongMapping.put("VARCHAR", "VARCHAR");
        shentongMapping.put("CHAR", "CHAR");
        shentongMapping.put("TEXT", "TEXT");
        shentongMapping.put("CLOB", "TEXT");
        shentongMapping.put("NUMERIC", "NUMERIC");
        shentongMapping.put("DECIMAL", "NUMERIC");
        shentongMapping.put("INTEGER", "INTEGER");
        shentongMapping.put("INT", "INTEGER");
        shentongMapping.put("BIGINT", "BIGINT");
        shentongMapping.put("REAL", "REAL");
        shentongMapping.put("DOUBLE PRECISION", "DOUBLE PRECISION");
        shentongMapping.put("FLOAT", "DOUBLE PRECISION");
        shentongMapping.put("BOOLEAN", "BOOLEAN");
        shentongMapping.put("DATE", "DATE");
        shentongMapping.put("TIME", "TIME");
        shentongMapping.put("TIMESTAMP", "TIMESTAMP");
        shentongMapping.put("BINARY", "BYTEA");
        shentongMapping.put("BLOB", "BYTEA");
        shentongMapping.put("BYTEA", "BYTEA");
        shentongMapping.put("JSON", "JSON");
        COMMON_TYPE_MAPPING.put("shentong", shentongMapping);

        // 瀚高数据库 HighGo 类型映射
        Map<String, String> highgoMapping = new HashMap<>();
        highgoMapping.put("VARCHAR", "VARCHAR");
        highgoMapping.put("CHAR", "CHAR");
        highgoMapping.put("TEXT", "TEXT");
        highgoMapping.put("INT2", "SMALLINT");
        highgoMapping.put("SMALLINT", "SMALLINT");
        highgoMapping.put("INT4", "INTEGER");
        highgoMapping.put("INTEGER", "INTEGER");
        highgoMapping.put("INT8", "BIGINT");
        highgoMapping.put("BIGINT", "BIGINT");
        highgoMapping.put("NUMERIC", "NUMERIC");
        highgoMapping.put("DECIMAL", "NUMERIC");
        highgoMapping.put("REAL", "REAL");
        highgoMapping.put("FLOAT4", "REAL");
        highgoMapping.put("DOUBLE PRECISION", "DOUBLE PRECISION");
        highgoMapping.put("FLOAT8", "DOUBLE PRECISION");
        highgoMapping.put("BOOLEAN", "BOOLEAN");
        highgoMapping.put("DATE", "DATE");
        highgoMapping.put("TIME", "TIME");
        highgoMapping.put("TIMESTAMP", "TIMESTAMP");
        highgoMapping.put("BYTEA", "BYTEA");
        highgoMapping.put("JSON", "JSON");
        highgoMapping.put("JSONB", "JSONB");
        highgoMapping.put("XML", "XML");
        COMMON_TYPE_MAPPING.put("highgo", highgoMapping);

        // 南大通用 GBase 类型映射
        Map<String, String> gbaseMapping = new HashMap<>();
        gbaseMapping.put("VARCHAR", "VARCHAR");
        gbaseMapping.put("CHAR", "CHAR");
        gbaseMapping.put("TEXT", "TEXT");
        gbaseMapping.put("SMALLINT", "SMALLINT");
        gbaseMapping.put("INTEGER", "INTEGER");
        gbaseMapping.put("INT", "INTEGER");
        gbaseMapping.put("SERIAL", "INTEGER");
        gbaseMapping.put("BIGINT", "BIGINT");
        gbaseMapping.put("BIGSERIAL", "BIGINT");
        gbaseMapping.put("DECIMAL", "NUMERIC");
        gbaseMapping.put("NUMERIC", "NUMERIC");
        gbaseMapping.put("REAL", "REAL");
        gbaseMapping.put("DOUBLE PRECISION", "DOUBLE PRECISION");
        gbaseMapping.put("BOOLEAN", "BOOLEAN");
        gbaseMapping.put("DATE", "DATE");
        gbaseMapping.put("TIME", "TIME");
        gbaseMapping.put("TIMESTAMP", "TIMESTAMP");
        gbaseMapping.put("BINARY", "BYTEA");
        gbaseMapping.put("BLOB", "BYTEA");
        gbaseMapping.put("BYTEA", "BYTEA");
        COMMON_TYPE_MAPPING.put("gbase", gbaseMapping);


        // 优炫数据库 UXDB 类型映射
        Map<String, String> uxdbMapping = new HashMap<>();
        uxdbMapping.put("VARCHAR", "VARCHAR");
        uxdbMapping.put("CHAR", "CHAR");
        uxdbMapping.put("TEXT", "TEXT");
        uxdbMapping.put("SMALLINT", "SMALLINT");
        uxdbMapping.put("INTEGER", "INTEGER");
        uxdbMapping.put("INT", "INTEGER");
        uxdbMapping.put("BIGINT", "BIGINT");
        uxdbMapping.put("DECIMAL", "NUMERIC");
        uxdbMapping.put("NUMERIC", "NUMERIC");
        uxdbMapping.put("REAL", "REAL");
        uxdbMapping.put("DOUBLE PRECISION", "DOUBLE PRECISION");
        uxdbMapping.put("FLOAT", "DOUBLE PRECISION");
        uxdbMapping.put("BOOLEAN", "BOOLEAN");
        uxdbMapping.put("DATE", "DATE");
        uxdbMapping.put("TIME", "TIME");
        uxdbMapping.put("TIMESTAMP", "TIMESTAMP");
        uxdbMapping.put("BINARY", "BYTEA");
        uxdbMapping.put("BLOB", "BYTEA");
        uxdbMapping.put("JSON", "JSON");
        uxdbMapping.put("JSONB", "JSONB");
        COMMON_TYPE_MAPPING.put("uxdb", uxdbMapping);

        // 华宇数据库 HYDB 类型映射
        Map<String, String> hydbMapping = new HashMap<>();
        hydbMapping.put("VARCHAR", "VARCHAR");
        hydbMapping.put("CHAR", "CHAR");
        hydbMapping.put("TEXT", "TEXT");
        hydbMapping.put("SMALLINT", "SMALLINT");
        hydbMapping.put("INTEGER", "INTEGER");
        hydbMapping.put("BIGINT", "BIGINT");
        hydbMapping.put("DECIMAL", "NUMERIC");
        hydbMapping.put("NUMERIC", "NUMERIC");
        hydbMapping.put("REAL", "REAL");
        hydbMapping.put("DOUBLE PRECISION", "DOUBLE PRECISION");
        hydbMapping.put("BOOLEAN", "BOOLEAN");
        hydbMapping.put("DATE", "DATE");
        hydbMapping.put("TIME", "TIME");
        hydbMapping.put("TIMESTAMP", "TIMESTAMP");
        hydbMapping.put("BYTEA", "BYTEA");
        hydbMapping.put("JSON", "JSON");
        COMMON_TYPE_MAPPING.put("hydb", hydbMapping);

        // 虚谷数据库 XuGu 类型映射
        Map<String, String> xuguMapping = new HashMap<>();
        xuguMapping.put("VARCHAR", "VARCHAR");
        xuguMapping.put("CHAR", "CHAR");
        xuguMapping.put("TEXT", "TEXT");
        xuguMapping.put("SMALLINT", "SMALLINT");
        xuguMapping.put("INTEGER", "INTEGER");
        xuguMapping.put("BIGINT", "BIGINT");
        xuguMapping.put("DECIMAL", "NUMERIC");
        xuguMapping.put("NUMERIC", "NUMERIC");
        xuguMapping.put("FLOAT", "REAL");
        xuguMapping.put("DOUBLE", "DOUBLE PRECISION");
        xuguMapping.put("BOOLEAN", "BOOLEAN");
        xuguMapping.put("DATE", "DATE");
        xuguMapping.put("TIME", "TIME");
        xuguMapping.put("TIMESTAMP", "TIMESTAMP");
        xuguMapping.put("BLOB", "BYTEA");
        xuguMapping.put("CLOB", "TEXT");
        COMMON_TYPE_MAPPING.put("xugu", xuguMapping);

        // 东软数据库 NeuDB 类型映射
        Map<String, String> neudbMapping = new HashMap<>();
        neudbMapping.put("VARCHAR", "VARCHAR");
        neudbMapping.put("CHAR", "CHAR");
        neudbMapping.put("TEXT", "TEXT");
        neudbMapping.put("SMALLINT", "SMALLINT");
        neudbMapping.put("INTEGER", "INTEGER");
        neudbMapping.put("BIGINT", "BIGINT");
        neudbMapping.put("DECIMAL", "NUMERIC");
        neudbMapping.put("NUMERIC", "NUMERIC");
        neudbMapping.put("REAL", "REAL");
        neudbMapping.put("DOUBLE", "DOUBLE PRECISION");
        neudbMapping.put("BOOLEAN", "BOOLEAN");
        neudbMapping.put("DATE", "DATE");
        neudbMapping.put("TIME", "TIME");
        neudbMapping.put("TIMESTAMP", "TIMESTAMP");
        neudbMapping.put("BLOB", "BYTEA");
        neudbMapping.put("CLOB", "TEXT");
        COMMON_TYPE_MAPPING.put("neudb", neudbMapping);

        // 巨杉数据库 SequoiaDB 类型映射
        Map<String, String> sequoiadbMapping = new HashMap<>();
        sequoiadbMapping.put("STRING", "VARCHAR");
        sequoiadbMapping.put("INT", "INTEGER");
        sequoiadbMapping.put("LONG", "BIGINT");
        sequoiadbMapping.put("DOUBLE", "DOUBLE PRECISION");
        sequoiadbMapping.put("DECIMAL", "NUMERIC");
        sequoiadbMapping.put("BOOLEAN", "BOOLEAN");
        sequoiadbMapping.put("DATE", "DATE");
        sequoiadbMapping.put("TIMESTAMP", "TIMESTAMP");
        sequoiadbMapping.put("BINARY", "BYTEA");
        sequoiadbMapping.put("ARRAY", "ARRAY");
        sequoiadbMapping.put("OBJECT", "JSON");
        COMMON_TYPE_MAPPING.put("sequoiadb", sequoiadbMapping);

        // 天兵数据库 TBase 类型映射
        Map<String, String> tbaseMapping = new HashMap<>();
        tbaseMapping.put("VARCHAR", "VARCHAR");
        tbaseMapping.put("CHAR", "CHAR");
        tbaseMapping.put("TEXT", "TEXT");
        tbaseMapping.put("SMALLINT", "SMALLINT");
        tbaseMapping.put("INTEGER", "INTEGER");
        tbaseMapping.put("BIGINT", "BIGINT");
        tbaseMapping.put("DECIMAL", "NUMERIC");
        tbaseMapping.put("NUMERIC", "NUMERIC");
        tbaseMapping.put("REAL", "REAL");
        tbaseMapping.put("DOUBLE PRECISION", "DOUBLE PRECISION");
        tbaseMapping.put("BOOLEAN", "BOOLEAN");
        tbaseMapping.put("DATE", "DATE");
        tbaseMapping.put("TIME", "TIME");
        tbaseMapping.put("TIMESTAMP", "TIMESTAMP");
        tbaseMapping.put("BYTEA", "BYTEA");
        tbaseMapping.put("JSON", "JSON");
        tbaseMapping.put("JSONB", "JSONB");
        COMMON_TYPE_MAPPING.put("tbase", tbaseMapping);

        // 大数据数据库映射
        // Doris 类型映射
        Map<String, String> dorisMapping = new HashMap<>();
        dorisMapping.put("VARCHAR", "VARCHAR");
        dorisMapping.put("CHAR", "CHAR");
        dorisMapping.put("TEXT", "TEXT");
        dorisMapping.put("BOOLEAN", "BOOLEAN");
        dorisMapping.put("TINYINT", "SMALLINT");
        dorisMapping.put("SMALLINT", "SMALLINT");
        dorisMapping.put("INT", "INTEGER");
        dorisMapping.put("BIGINT", "BIGINT");
        dorisMapping.put("LARGEINT", "NUMERIC(38,0)");
        dorisMapping.put("FLOAT", "REAL");
        dorisMapping.put("DOUBLE", "DOUBLE PRECISION");
        dorisMapping.put("DECIMAL", "NUMERIC");
        dorisMapping.put("DATE", "DATE");
        dorisMapping.put("DATETIME", "TIMESTAMP");
        dorisMapping.put("JSON", "JSON");
        COMMON_TYPE_MAPPING.put("doris", dorisMapping);

        // StarRocks 类型映射
        Map<String, String> starrocksMapping = new HashMap<>();
        starrocksMapping.put("VARCHAR", "VARCHAR");
        starrocksMapping.put("CHAR", "CHAR");
        starrocksMapping.put("TEXT", "TEXT");
        starrocksMapping.put("BOOLEAN", "BOOLEAN");
        starrocksMapping.put("TINYINT", "SMALLINT");
        starrocksMapping.put("SMALLINT", "SMALLINT");
        starrocksMapping.put("INT", "INTEGER");
        starrocksMapping.put("BIGINT", "BIGINT");
        starrocksMapping.put("LARGEINT", "NUMERIC(38,0)");
        starrocksMapping.put("FLOAT", "REAL");
        starrocksMapping.put("DOUBLE", "DOUBLE PRECISION");
        starrocksMapping.put("DECIMAL", "NUMERIC");
        starrocksMapping.put("DATE", "DATE");
        starrocksMapping.put("DATETIME", "TIMESTAMP");
        starrocksMapping.put("JSON", "JSON");
        COMMON_TYPE_MAPPING.put("starrocks", starrocksMapping);

        // ClickHouse 类型映射
        Map<String, String> clickhouseMapping = new HashMap<>();
        clickhouseMapping.put("String", "VARCHAR");
        clickhouseMapping.put("FixedString", "CHAR");
        clickhouseMapping.put("UInt8", "SMALLINT");
        clickhouseMapping.put("UInt16", "INTEGER");
        clickhouseMapping.put("UInt32", "BIGINT");
        clickhouseMapping.put("UInt64", "NUMERIC(20)");
        clickhouseMapping.put("Int8", "SMALLINT");
        clickhouseMapping.put("Int16", "SMALLINT");
        clickhouseMapping.put("Int32", "INTEGER");
        clickhouseMapping.put("Int64", "BIGINT");
        clickhouseMapping.put("Float32", "REAL");
        clickhouseMapping.put("Float64", "DOUBLE PRECISION");
        clickhouseMapping.put("Decimal", "NUMERIC");
        clickhouseMapping.put("Decimal32", "NUMERIC");
        clickhouseMapping.put("Decimal64", "NUMERIC");
        clickhouseMapping.put("Decimal128", "NUMERIC");
        clickhouseMapping.put("Date", "DATE");
        clickhouseMapping.put("DateTime", "TIMESTAMP");
        clickhouseMapping.put("DateTime64", "TIMESTAMP");
        clickhouseMapping.put("UUID", "UUID");
        clickhouseMapping.put("JSON", "JSONB");
        COMMON_TYPE_MAPPING.put("clickhouse", clickhouseMapping);

        // Apache Hive 类型映射
        Map<String, String> hiveMapping = new HashMap<>();
        hiveMapping.put("STRING", "VARCHAR");
        hiveMapping.put("VARCHAR", "VARCHAR");
        hiveMapping.put("CHAR", "CHAR");
        hiveMapping.put("TINYINT", "SMALLINT");
        hiveMapping.put("SMALLINT", "SMALLINT");
        hiveMapping.put("INT", "INTEGER");
        hiveMapping.put("BIGINT", "BIGINT");
        hiveMapping.put("FLOAT", "REAL");
        hiveMapping.put("DOUBLE", "DOUBLE PRECISION");
        hiveMapping.put("DECIMAL", "NUMERIC");
        hiveMapping.put("BOOLEAN", "BOOLEAN");
        hiveMapping.put("DATE", "DATE");
        hiveMapping.put("TIMESTAMP", "TIMESTAMP");
        hiveMapping.put("BINARY", "BYTEA");
        hiveMapping.put("ARRAY", "ARRAY");
        hiveMapping.put("MAP", "JSONB");
        hiveMapping.put("STRUCT", "JSONB");
        hiveMapping.put("UNIONTYPE", "JSONB");
        COMMON_TYPE_MAPPING.put("hive", hiveMapping);

        // Apache Impala 类型映射
        Map<String, String> impalaMapping = new HashMap<>();
        impalaMapping.put("STRING", "VARCHAR");
        impalaMapping.put("VARCHAR", "VARCHAR");
        impalaMapping.put("CHAR", "CHAR");
        impalaMapping.put("TINYINT", "SMALLINT");
        impalaMapping.put("SMALLINT", "SMALLINT");
        impalaMapping.put("INT", "INTEGER");
        impalaMapping.put("BIGINT", "BIGINT");
        impalaMapping.put("FLOAT", "REAL");
        impalaMapping.put("DOUBLE", "DOUBLE PRECISION");
        impalaMapping.put("DECIMAL", "NUMERIC");
        impalaMapping.put("BOOLEAN", "BOOLEAN");
        impalaMapping.put("TIMESTAMP", "TIMESTAMP");
        impalaMapping.put("DATE", "DATE");
        impalaMapping.put("BINARY", "BYTEA");
        impalaMapping.put("ARRAY", "ARRAY");
        impalaMapping.put("MAP", "JSONB");
        impalaMapping.put("STRUCT", "JSONB");
        COMMON_TYPE_MAPPING.put("impala", impalaMapping);

        // Apache Kylin 类型映射
        Map<String, String> kylinMapping = new HashMap<>();
        kylinMapping.put("VARCHAR", "VARCHAR");
        kylinMapping.put("CHAR", "CHAR");
        kylinMapping.put("STRING", "VARCHAR");
        kylinMapping.put("TINYINT", "SMALLINT");
        kylinMapping.put("SMALLINT", "SMALLINT");
        kylinMapping.put("INTEGER", "INTEGER");
        kylinMapping.put("INT", "INTEGER");
        kylinMapping.put("BIGINT", "BIGINT");
        kylinMapping.put("FLOAT", "REAL");
        kylinMapping.put("DOUBLE", "DOUBLE PRECISION");
        kylinMapping.put("DECIMAL", "NUMERIC");
        kylinMapping.put("BOOLEAN", "BOOLEAN");
        kylinMapping.put("DATE", "DATE");
        kylinMapping.put("TIMESTAMP", "TIMESTAMP");
        kylinMapping.put("BINARY", "BYTEA");
        COMMON_TYPE_MAPPING.put("kylin", kylinMapping);

        // Apache Kudu 类型映射
        Map<String, String> kuduMapping = new HashMap<>();
        kuduMapping.put("STRING", "VARCHAR");
        kuduMapping.put("BOOL", "BOOLEAN");
        kuduMapping.put("INT8", "SMALLINT");
        kuduMapping.put("INT16", "SMALLINT");
        kuduMapping.put("INT32", "INTEGER");
        kuduMapping.put("INT64", "BIGINT");
        kuduMapping.put("FLOAT", "REAL");
        kuduMapping.put("DOUBLE", "DOUBLE PRECISION");
        kuduMapping.put("DECIMAL", "NUMERIC");
        kuduMapping.put("UNIXTIME_MICROS", "TIMESTAMP");
        kuduMapping.put("BINARY", "BYTEA");
        COMMON_TYPE_MAPPING.put("kudu", kuduMapping);

        // Presto/Trino 类型映射
        Map<String, String> prestoMapping = new HashMap<>();
        prestoMapping.put("VARCHAR", "VARCHAR");
        prestoMapping.put("CHAR", "CHAR");
        prestoMapping.put("VARBINARY", "BYTEA");
        prestoMapping.put("BOOLEAN", "BOOLEAN");
        prestoMapping.put("TINYINT", "SMALLINT");
        prestoMapping.put("SMALLINT", "SMALLINT");
        prestoMapping.put("INTEGER", "INTEGER");
        prestoMapping.put("BIGINT", "BIGINT");
        prestoMapping.put("REAL", "REAL");
        prestoMapping.put("DOUBLE", "DOUBLE PRECISION");
        prestoMapping.put("DECIMAL", "NUMERIC");
        prestoMapping.put("DATE", "DATE");
        prestoMapping.put("TIME", "TIME");
        prestoMapping.put("TIME WITH TIME ZONE", "TIME WITH TIME ZONE");
        prestoMapping.put("TIMESTAMP", "TIMESTAMP");
        prestoMapping.put("TIMESTAMP WITH TIME ZONE", "TIMESTAMP WITH TIME ZONE");
        prestoMapping.put("INTERVAL YEAR TO MONTH", "INTERVAL");
        prestoMapping.put("INTERVAL DAY TO SECOND", "INTERVAL");
        prestoMapping.put("ARRAY", "ARRAY");
        prestoMapping.put("MAP", "JSONB");
        prestoMapping.put("JSON", "JSONB");
        COMMON_TYPE_MAPPING.put("presto", prestoMapping);
        COMMON_TYPE_MAPPING.put("trino", prestoMapping);

        // Apache Spark SQL 类型映射
        Map<String, String> sparkMapping = new HashMap<>();
        sparkMapping.put("STRING", "VARCHAR");
        sparkMapping.put("CHAR", "CHAR");
        sparkMapping.put("VARCHAR", "VARCHAR");
        sparkMapping.put("BOOLEAN", "BOOLEAN");
        sparkMapping.put("TINYINT", "SMALLINT");
        sparkMapping.put("SMALLINT", "SMALLINT");
        sparkMapping.put("INT", "INTEGER");
        sparkMapping.put("BIGINT", "BIGINT");
        sparkMapping.put("FLOAT", "REAL");
        sparkMapping.put("DOUBLE", "DOUBLE PRECISION");
        sparkMapping.put("DECIMAL", "NUMERIC");
        sparkMapping.put("DATE", "DATE");
        sparkMapping.put("TIMESTAMP", "TIMESTAMP");
        sparkMapping.put("BINARY", "BYTEA");
        sparkMapping.put("ARRAY", "ARRAY");
        sparkMapping.put("MAP", "JSONB");
        sparkMapping.put("STRUCT", "JSONB");
        COMMON_TYPE_MAPPING.put("spark", sparkMapping);

        // Apache Flink SQL 类型映射
        Map<String, String> flinkMapping = new HashMap<>();
        flinkMapping.put("CHAR", "CHAR");
        flinkMapping.put("VARCHAR", "VARCHAR");
        flinkMapping.put("STRING", "VARCHAR");
        flinkMapping.put("BOOLEAN", "BOOLEAN");
        flinkMapping.put("TINYINT", "SMALLINT");
        flinkMapping.put("SMALLINT", "SMALLINT");
        flinkMapping.put("INTEGER", "INTEGER");
        flinkMapping.put("INT", "INTEGER");
        flinkMapping.put("BIGINT", "BIGINT");
        flinkMapping.put("FLOAT", "REAL");
        flinkMapping.put("DOUBLE", "DOUBLE PRECISION");
        flinkMapping.put("DECIMAL", "NUMERIC");
        flinkMapping.put("DATE", "DATE");
        flinkMapping.put("TIME", "TIME");
        flinkMapping.put("TIMESTAMP", "TIMESTAMP");
        flinkMapping.put("TIMESTAMP WITH LOCAL TIME ZONE", "TIMESTAMP WITH TIME ZONE");
        flinkMapping.put("TIMESTAMP WITH TIME ZONE", "TIMESTAMP WITH TIME ZONE");
        flinkMapping.put("INTERVAL YEAR TO MONTH", "INTERVAL");
        flinkMapping.put("INTERVAL DAY TO SECOND", "INTERVAL");
        flinkMapping.put("ARRAY", "ARRAY");
        flinkMapping.put("MAP", "JSONB");
        flinkMapping.put("MULTISET", "JSONB");
        flinkMapping.put("ROW", "JSONB");
        flinkMapping.put("RAW", "BYTEA");
        flinkMapping.put("BINARY", "BYTEA");
        flinkMapping.put("VARBINARY", "BYTEA");
        COMMON_TYPE_MAPPING.put("flink", flinkMapping);

        // TiDB 类型映射
        Map<String, String> tidbMapping = new HashMap<>();
        tidbMapping.put("VARCHAR", "VARCHAR");
        tidbMapping.put("CHAR", "CHAR");
        tidbMapping.put("TEXT", "TEXT");
        tidbMapping.put("TINYTEXT", "TEXT");
        tidbMapping.put("MEDIUMTEXT", "TEXT");
        tidbMapping.put("LONGTEXT", "TEXT");
        tidbMapping.put("TINYINT", "SMALLINT");
        tidbMapping.put("SMALLINT", "SMALLINT");
        tidbMapping.put("MEDIUMINT", "INTEGER");
        tidbMapping.put("INT", "INTEGER");
        tidbMapping.put("INTEGER", "INTEGER");
        tidbMapping.put("BIGINT", "BIGINT");
        tidbMapping.put("FLOAT", "REAL");
        tidbMapping.put("DOUBLE", "DOUBLE PRECISION");
        tidbMapping.put("DECIMAL", "NUMERIC");
        tidbMapping.put("NUMERIC", "NUMERIC");
        tidbMapping.put("BOOLEAN", "BOOLEAN");
        tidbMapping.put("BOOL", "BOOLEAN");
        tidbMapping.put("DATE", "DATE");
        tidbMapping.put("TIME", "TIME");
        tidbMapping.put("DATETIME", "TIMESTAMP");
        tidbMapping.put("TIMESTAMP", "TIMESTAMP");
        tidbMapping.put("BINARY", "BYTEA");
        tidbMapping.put("VARBINARY", "BYTEA");
        tidbMapping.put("BLOB", "BYTEA");
        tidbMapping.put("TINYBLOB", "BYTEA");
        tidbMapping.put("MEDIUMBLOB", "BYTEA");
        tidbMapping.put("LONGBLOB", "BYTEA");
        tidbMapping.put("JSON", "JSONB");
        tidbMapping.put("ENUM", "VARCHAR");
        tidbMapping.put("SET", "VARCHAR");
        COMMON_TYPE_MAPPING.put("tidb", tidbMapping);

        // CockroachDB 类型映射
        Map<String, String> cockroachMapping = new HashMap<>();
        cockroachMapping.put("VARCHAR", "VARCHAR");
        cockroachMapping.put("CHAR", "CHAR");
        cockroachMapping.put("STRING", "VARCHAR");
        cockroachMapping.put("TEXT", "TEXT");
        cockroachMapping.put("INT2", "SMALLINT");
        cockroachMapping.put("SMALLINT", "SMALLINT");
        cockroachMapping.put("INT4", "INTEGER");
        cockroachMapping.put("INTEGER", "INTEGER");
        cockroachMapping.put("INT8", "BIGINT");
        cockroachMapping.put("BIGINT", "BIGINT");
        cockroachMapping.put("FLOAT4", "REAL");
        cockroachMapping.put("REAL", "REAL");
        cockroachMapping.put("FLOAT8", "DOUBLE PRECISION");
        cockroachMapping.put("DOUBLE PRECISION", "DOUBLE PRECISION");
        cockroachMapping.put("DECIMAL", "NUMERIC");
        cockroachMapping.put("NUMERIC", "NUMERIC");
        cockroachMapping.put("BOOL", "BOOLEAN");
        cockroachMapping.put("BOOLEAN", "BOOLEAN");
        cockroachMapping.put("DATE", "DATE");
        cockroachMapping.put("TIME", "TIME");
        cockroachMapping.put("TIMETZ", "TIME WITH TIME ZONE");
        cockroachMapping.put("TIMESTAMP", "TIMESTAMP");
        cockroachMapping.put("TIMESTAMPTZ", "TIMESTAMP WITH TIME ZONE");
        cockroachMapping.put("INTERVAL", "INTERVAL");
        cockroachMapping.put("BYTES", "BYTEA");
        cockroachMapping.put("BYTEA", "BYTEA");
        cockroachMapping.put("JSON", "JSON");
        cockroachMapping.put("JSONB", "JSONB");
        cockroachMapping.put("UUID", "UUID");
        cockroachMapping.put("ARRAY", "ARRAY");
        COMMON_TYPE_MAPPING.put("cockroach", cockroachMapping);

        // YugabyteDB 类型映射
        Map<String, String> yugabyteMapping = new HashMap<>();
        yugabyteMapping.put("VARCHAR", "VARCHAR");
        yugabyteMapping.put("CHAR", "CHAR");
        yugabyteMapping.put("TEXT", "TEXT");
        yugabyteMapping.put("SMALLINT", "SMALLINT");
        yugabyteMapping.put("INTEGER", "INTEGER");
        yugabyteMapping.put("BIGINT", "BIGINT");
        yugabyteMapping.put("DECIMAL", "NUMERIC");
        yugabyteMapping.put("NUMERIC", "NUMERIC");
        yugabyteMapping.put("REAL", "REAL");
        yugabyteMapping.put("DOUBLE PRECISION", "DOUBLE PRECISION");
        yugabyteMapping.put("BOOLEAN", "BOOLEAN");
        yugabyteMapping.put("DATE", "DATE");
        yugabyteMapping.put("TIME", "TIME");
        yugabyteMapping.put("TIMESTAMP", "TIMESTAMP");
        yugabyteMapping.put("INTERVAL", "INTERVAL");
        yugabyteMapping.put("BYTEA", "BYTEA");
        yugabyteMapping.put("JSON", "JSON");
        yugabyteMapping.put("JSONB", "JSONB");
        yugabyteMapping.put("UUID", "UUID");
        yugabyteMapping.put("ARRAY", "ARRAY");
        COMMON_TYPE_MAPPING.put("yugabyte", yugabyteMapping);

        // Apache Cassandra 类型映射
        Map<String, String> cassandraMapping = new HashMap<>();
        cassandraMapping.put("ASCII", "VARCHAR");
        cassandraMapping.put("VARCHAR", "VARCHAR");
        cassandraMapping.put("TEXT", "TEXT");
        cassandraMapping.put("TINYINT", "SMALLINT");
        cassandraMapping.put("SMALLINT", "SMALLINT");
        cassandraMapping.put("INT", "INTEGER");
        cassandraMapping.put("BIGINT", "BIGINT");
        cassandraMapping.put("VARINT", "NUMERIC");
        cassandraMapping.put("FLOAT", "REAL");
        cassandraMapping.put("DOUBLE", "DOUBLE PRECISION");
        cassandraMapping.put("DECIMAL", "NUMERIC");
        cassandraMapping.put("BOOLEAN", "BOOLEAN");
        cassandraMapping.put("DATE", "DATE");
        cassandraMapping.put("TIME", "TIME");
        cassandraMapping.put("TIMESTAMP", "TIMESTAMP");
        cassandraMapping.put("BLOB", "BYTEA");
        cassandraMapping.put("UUID", "UUID");
        cassandraMapping.put("TIMEUUID", "UUID");
        cassandraMapping.put("INET", "INET");
        cassandraMapping.put("LIST", "ARRAY");
        cassandraMapping.put("SET", "ARRAY");
        cassandraMapping.put("MAP", "JSONB");
        cassandraMapping.put("TUPLE", "JSONB");
        COMMON_TYPE_MAPPING.put("cassandra", cassandraMapping);

        // ScyllaDB 类型映射 (与 Cassandra 兼容)
        COMMON_TYPE_MAPPING.put("scylla", cassandraMapping);

        // Apache HBase 类型映射
        Map<String, String> hbaseMapping = new HashMap<>();
        hbaseMapping.put("VARCHAR", "VARCHAR");
        hbaseMapping.put("CHAR", "CHAR");
        hbaseMapping.put("STRING", "VARCHAR");
        hbaseMapping.put("TINYINT", "SMALLINT");
        hbaseMapping.put("SMALLINT", "SMALLINT");
        hbaseMapping.put("INTEGER", "INTEGER");
        hbaseMapping.put("BIGINT", "BIGINT");
        hbaseMapping.put("FLOAT", "REAL");
        hbaseMapping.put("DOUBLE", "DOUBLE PRECISION");
        hbaseMapping.put("DECIMAL", "NUMERIC");
        hbaseMapping.put("BOOLEAN", "BOOLEAN");
        hbaseMapping.put("DATE", "DATE");
        hbaseMapping.put("TIME", "TIME");
        hbaseMapping.put("TIMESTAMP", "TIMESTAMP");
        hbaseMapping.put("BINARY", "BYTEA");
        COMMON_TYPE_MAPPING.put("hbase", hbaseMapping);

        // ... 其他数据库类型映射可以继续添加 ...
    }

    public PostgreSQLHandler(DataSource dataSource) {
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
        return "public";
    }

    @Override
    public void setSchema(Connection conn, String schema) throws SQLException {
        if (schema != null && !schema.isEmpty()) {
            conn.setSchema(schema);
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
        return "public";
    }

    @Override
    public String getValidationQuery() {
        return "SELECT 1";
    }

    @Override
    public String getQuoteString() {
        return "\"";
    }

    @Override
    public List<String> getSystemDatabases() {
        return Arrays.asList("postgres", "template0", "template1");
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
            return ((Boolean) value) ? "true" : "false";
        }
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
        // 确认使用PostgreSQL标准的LIMIT OFFSET语法
        return sql + " LIMIT " + limit + " OFFSET " + offset;
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
        return "PostgreSQL";
    }

    @Override
    public String getTableExistsSql(String tableName) {
        return String.format(
            "SELECT COUNT(*) FROM information_schema.tables " +
            "WHERE table_schema = '%s' AND table_name = '%s'",
            getSchema(),
            tableName.toLowerCase()
        );
    }

    @Override
    public String getCreateTableSql(String tableName, List<ColumnDefinition> columns, String tableComment, String engine, String charset, String collate) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(wrapIdentifier(tableName)).append(" (\n");
        
        List<String> primaryKeys = new ArrayList<>();
        List<String> columnDefinitions = new ArrayList<>();
        
        for (ColumnDefinition column : columns) {
            // 调整PostgreSQL列类型
            adjustPostgreSQLColumnType(column);
            
            String columnDefinition = formatFieldDefinition(
                column.getName(),
                column.getType(),
                column.getLength(),
                column.getPrecision(),
                column.getScale(),
                column.isNullable(),
                column.getDefaultValue(),
                column.getComment()
            );
            
            columnDefinitions.add(columnDefinition);
            
            if (column.isPrimaryKey()) {
                primaryKeys.add(column.getName());
            }
        }
        
        sb.append(String.join(",\n", columnDefinitions));
        
        // 添加主键定义
        if (!primaryKeys.isEmpty()) {
            sb.append(",\n");
            sb.append("PRIMARY KEY (").append(primaryKeys.stream()
                    .map(this::wrapIdentifier)
                    .collect(Collectors.joining(", ")))
               .append(")");
        }
        
        sb.append("\n)");
        
        // PostgreSQL不支持表引擎
        // PostgreSQL不直接支持字符集和排序规则，这些设置通常在数据库或集群级别设置
        
        // 添加表注释
        if (tableComment != null && !tableComment.isEmpty()) {
            sb.append(";\n");
            sb.append("COMMENT ON TABLE ").append(wrapIdentifier(tableName))
              .append(" IS ").append(wrapValue(tableComment));
        }
        
        return sb.toString();
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
    public String getAddColumnSql(String tableName, ColumnDefinition column, String afterColumn) {
        StringBuilder sb = new StringBuilder();
        sb.append("ALTER TABLE ").append(wrapIdentifier(tableName));
        sb.append(" ADD COLUMN ");
        
        // 调整列类型
        adjustPostgreSQLColumnType(column);
        
        String columnDefinition = formatFieldDefinition(
            column.getName(),
            column.getType(),
            column.getLength(),
            column.getPrecision(),
            column.getScale(),
            column.isNullable(),
            column.getDefaultValue(),
            column.getComment()
        );
        
        sb.append(columnDefinition);
        
        // PostgreSQL不支持AFTER语法，列的位置由添加的顺序决定
        // 忽略afterColumn参数
        
        // 如果有列注释，需要单独添加
        if (column.getComment() != null && !column.getComment().isEmpty()) {
            sb.append(";\n");
            sb.append("COMMENT ON COLUMN ").append(wrapIdentifier(tableName))
              .append(".").append(wrapIdentifier(column.getName()))
              .append(" IS ").append(wrapValue(column.getComment()));
        }
        
        return sb.toString();
    }

    @Override
    public String getModifyColumnSql(String tableName, String columnName, String newDefinition) {
        // PostgreSQL使用ALTER TABLE...ALTER COLUMN语法来修改列
        StringBuilder sb = new StringBuilder();
        sb.append("ALTER TABLE ").append(wrapIdentifier(tableName)).append(" ");
        
        // 分析新定义，拆分为类型和约束部分
        String[] parts = newDefinition.split("\\s+", 2);
        String dataType = parts[0];
        String constraints = parts.length > 1 ? parts[1] : "";
        
        // 修改数据类型
        sb.append("ALTER COLUMN ").append(wrapIdentifier(columnName))
          .append(" TYPE ").append(dataType);
        
        // 检查是否包含NOT NULL约束
        if (constraints.contains("NOT NULL")) {
            sb.append(", ALTER COLUMN ").append(wrapIdentifier(columnName))
              .append(" SET NOT NULL");
        } else if (constraints.contains("NULL") && !constraints.contains("NOT NULL")) {
            sb.append(", ALTER COLUMN ").append(wrapIdentifier(columnName))
              .append(" DROP NOT NULL");
        }
        
        // 检查是否包含DEFAULT值
        if (constraints.contains("DEFAULT")) {
            int defaultPos = constraints.indexOf("DEFAULT");
            String defaultValue = constraints.substring(defaultPos + 7).trim();
            // 提取DEFAULT值直到下一个约束或结束
            int nextConstraintPos = defaultValue.indexOf(" ");
            if (nextConstraintPos > 0) {
                defaultValue = defaultValue.substring(0, nextConstraintPos);
            }
            
            sb.append(", ALTER COLUMN ").append(wrapIdentifier(columnName))
              .append(" SET DEFAULT ").append(defaultValue);
        }
        
        return sb.toString();
    }

    @Override
    public String getDropColumnSql(String tableName, String columnName) {
        return "ALTER TABLE " + wrapIdentifier(tableName) + " DROP COLUMN " + wrapIdentifier(columnName);
    }

    @Override
    public String getRenameTableSql(String oldTableName, String newTableName) {
        return "ALTER TABLE " + wrapIdentifier(oldTableName) + " RENAME TO " + wrapIdentifier(newTableName);
    }

    @Override
    public String getShowCreateTableSql(String tableName) {
        return "SELECT pg_get_tabledef('" + tableName + "'::regclass)";
    }

    @Override
    public String getShowTablesSql() {
        // 从特定模式中查询表
        return "SELECT table_name FROM information_schema.tables WHERE table_schema = '" + getSchema() + "' AND table_type = 'BASE TABLE' ORDER BY table_name";
    }

    @Override
    public String getShowColumnsSql(String tableName) {
        // 从信息模式中查询列信息
        return "SELECT column_name, data_type, character_maximum_length, " +
               "numeric_precision, numeric_scale, is_nullable, column_default, " +
               "ordinal_position " +
               "FROM information_schema.columns " +
               "WHERE table_schema = '" + getSchema() + "' AND table_name = '" + tableName + "' " +
               "ORDER BY ordinal_position";
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
        return "NUMERIC(10,2)";
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
        return "BYTEA";
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
            return "DOUBLE PRECISION";
        }
        if (Float.class.equals(javaType) || float.class.equals(javaType)) {
            return "REAL";
        }
        if (BigDecimal.class.equals(javaType)) {
            return "NUMERIC";
        }
        if (Boolean.class.equals(javaType) || boolean.class.equals(javaType)) {
            return "BOOLEAN";
        }
        if (Date.class.equals(javaType)) {
            return "TIMESTAMP";
        }
        if (byte[].class.equals(javaType)) {
            return "BYTEA";
        }
        return "VARCHAR";
    }

    @Override
    public Class<?> mapDbTypeToJavaType(String dbType) {
        dbType = dbType.toUpperCase();
        switch (dbType) {
            case "SMALLINT":
                return Short.class;
            case "INTEGER":
                return Integer.class;
            case "BIGINT":
                return Long.class;
            case "NUMERIC":
            case "DECIMAL":
                return BigDecimal.class;
            case "REAL":
                return Float.class;
            case "DOUBLE PRECISION":
                return Double.class;
            case "BOOLEAN":
                return Boolean.class;
            case "DATE":
            case "TIME":
            case "TIMESTAMP":
                return Date.class;
            case "BYTEA":
                return byte[].class;
            default:
                return String.class;
        }
    }

    @Override
    public Map<String, String> getTypeMapping() {
        // 确保TYPE_MAPPING包含最新的PostgreSQL特有类型
        if (TYPE_MAPPING.isEmpty()) {
            // 基本类型
            TYPE_MAPPING.put("varchar", "character varying");
            TYPE_MAPPING.put("char", "character");
            TYPE_MAPPING.put("int", "integer");
            TYPE_MAPPING.put("integer", "integer");
            TYPE_MAPPING.put("smallint", "smallint");
            TYPE_MAPPING.put("bigint", "bigint");
            
            // 浮点和数值类型
            TYPE_MAPPING.put("float", "real");
            TYPE_MAPPING.put("double", "double precision");
            TYPE_MAPPING.put("real", "real");
            TYPE_MAPPING.put("decimal", "numeric");
            TYPE_MAPPING.put("numeric", "numeric");
            TYPE_MAPPING.put("money", "money");
            
            // 日期时间类型
            TYPE_MAPPING.put("date", "date");
            TYPE_MAPPING.put("time", "time without time zone");
            TYPE_MAPPING.put("timetz", "time with time zone");
            TYPE_MAPPING.put("timestamp", "timestamp without time zone");
            TYPE_MAPPING.put("timestamptz", "timestamp with time zone");
            TYPE_MAPPING.put("interval", "interval");
            
            // 布尔类型
            TYPE_MAPPING.put("boolean", "boolean");
            TYPE_MAPPING.put("bool", "boolean");
            
            // 文本类型
            TYPE_MAPPING.put("text", "text");
            TYPE_MAPPING.put("citext", "citext"); // 不区分大小写的文本
            
            // 二进制数据
            TYPE_MAPPING.put("bytea", "bytea");
            TYPE_MAPPING.put("blob", "bytea");
            
            // 网络地址类型
            TYPE_MAPPING.put("inet", "inet");
            TYPE_MAPPING.put("cidr", "cidr");
            TYPE_MAPPING.put("macaddr", "macaddr");
            
            // JSON类型
            TYPE_MAPPING.put("json", "json");
            TYPE_MAPPING.put("jsonb", "jsonb");
            
            // XML类型
            TYPE_MAPPING.put("xml", "xml");
            
            // UUID类型
            TYPE_MAPPING.put("uuid", "uuid");
            
            // 几何类型
            TYPE_MAPPING.put("point", "point");
            TYPE_MAPPING.put("line", "line");
            TYPE_MAPPING.put("lseg", "lseg");
            TYPE_MAPPING.put("box", "box");
            TYPE_MAPPING.put("path", "path");
            TYPE_MAPPING.put("polygon", "polygon");
            TYPE_MAPPING.put("circle", "circle");
            
            // 自增类型
            TYPE_MAPPING.put("serial", "serial");
            TYPE_MAPPING.put("bigserial", "bigserial");
            TYPE_MAPPING.put("smallserial", "smallserial");
            
            // 数组类型 - 在PostgreSQL中，任何类型都可以有一个数组
            // 不必全部列出，但可以在需要时处理
        }
        return TYPE_MAPPING;
    }

    @Override
    public Map<String, Integer> getDefaultLengthMapping() {
        // 确保DEFAULT_LENGTH_MAPPING包含PostgreSQL合适的默认长度值
        if (DEFAULT_LENGTH_MAPPING.isEmpty()) {
            DEFAULT_LENGTH_MAPPING.put("character varying", DEFAULT_VARCHAR_LENGTH);
            DEFAULT_LENGTH_MAPPING.put("character", 1);
            DEFAULT_LENGTH_MAPPING.put("numeric", 10);  // precision默认为10
            // PostgreSQL中大多数类型不需要指定长度
        }
        return DEFAULT_LENGTH_MAPPING;
    }

    @Override
    public boolean isValidFieldLength(String type, int length) {
        type = type.toLowerCase();
        
        // PostgreSQL中许多类型不需要或不允许指定长度
        if (type.equals("text") || type.equals("integer") || type.equals("bigint") || 
            type.equals("smallint") || type.equals("boolean") || type.equals("json") || 
            type.equals("jsonb") || type.equals("date") || type.equals("time") || 
            type.equals("bytea") || type.equals("uuid") || type.equals("serial") || 
            type.equals("bigserial") || type.equals("smallserial")) {
            return false;
        }
        
        // 对于character varying和character类型，需要检查长度限制
        if (type.equals("character varying") || type.equals("varchar")) {
            return length > 0 && length <= MAX_VARCHAR_LENGTH;
        }
        
        if (type.equals("character") || type.equals("char")) {
            return length > 0;
        }
        
        // 时间戳精度
        if (type.contains("timestamp") || type.contains("time")) {
            return length >= 0 && length <= 6;  // PostgreSQL的时间戳精度最高为6
        }
        
        return true;
    }

    @Override
    public String formatFieldDefinition(String name, String type, Integer length, 
        Integer precision, Integer scale, boolean nullable, String defaultValue, String comment) {
        
        StringBuilder sb = new StringBuilder();
        
        sb.append(wrapIdentifier(name)).append(" ");
        
        // 处理类型和长度/精度/范围
        String lowerCaseType = type.toLowerCase();
        
        // 处理VARCHAR类型
        if (lowerCaseType.equals("varchar") || lowerCaseType.equals("character varying")) {
            sb.append("character varying");
            if (length != null && length > 0) {
                sb.append("(").append(length).append(")");
            }
        } 
        // 处理CHAR类型
        else if (lowerCaseType.equals("char") || lowerCaseType.equals("character")) {
            sb.append("character");
            if (length != null && length > 0) {
                sb.append("(").append(length).append(")");
            }
        }
        // 处理数值类型
        else if (lowerCaseType.equals("numeric") || lowerCaseType.equals("decimal")) {
            sb.append(lowerCaseType);
            if (precision != null && precision > 0) {
                sb.append("(").append(precision);
                if (scale != null && scale >= 0) {
                    sb.append(",").append(scale);
                }
                sb.append(")");
            }
        }
        // 处理时间戳类型
        else if (lowerCaseType.startsWith("timestamp") || lowerCaseType.startsWith("time ")) {
            // 分离时区信息
            boolean withTimeZone = lowerCaseType.contains("with time zone") || 
                                  lowerCaseType.contains("timezone") ||
                                  lowerCaseType.equals("timestamptz") ||
                                  lowerCaseType.equals("timetz");
            
            // 基本类型（不含时区）
            String baseType = lowerCaseType.startsWith("timestamp") ? "timestamp" : "time";
            sb.append(baseType);
            
            // 添加精度（如果有）
            if (precision != null && precision >= 0 && precision <= 6) {
                sb.append("(").append(precision).append(")");
            }
            
            // 添加时区信息
            if (withTimeZone) {
                sb.append(" with time zone");
            } else if (lowerCaseType.contains("without time zone")) {
                sb.append(" without time zone");
            }
        }
        // 处理数组类型
        else if (lowerCaseType.endsWith("[]")) {
            String baseType = lowerCaseType.substring(0, lowerCaseType.length() - 2);
            
            // 递归处理基本类型
            String formattedBaseType = formatFieldDefinition("temp", baseType, length, precision, scale, true, null, null);
            // 提取类型部分（去除列名和其他属性）
            int firstSpacePos = formattedBaseType.indexOf(' ');
            int secondSpacePos = formattedBaseType.indexOf(' ', firstSpacePos + 1);
            if (secondSpacePos > 0) {
                formattedBaseType = formattedBaseType.substring(firstSpacePos + 1, secondSpacePos);
            } else {
                formattedBaseType = formattedBaseType.substring(firstSpacePos + 1);
            }
            
            sb.append(formattedBaseType).append("[]");
        }
        // 处理位串类型
        else if (lowerCaseType.equals("bit") || lowerCaseType.equals("bit varying") || lowerCaseType.equals("varbit")) {
            if (lowerCaseType.equals("varbit")) {
                sb.append("bit varying");
            } else {
                sb.append(lowerCaseType);
            }
            
            if (length != null && length > 0) {
                sb.append("(").append(length).append(")");
            }
        }
        // 其他类型不需要修改
        else {
            sb.append(type);
        }
        
        // 添加NOT NULL约束
        if (!nullable) {
            sb.append(" NOT NULL");
        }
        
        // 添加默认值
        if (defaultValue != null && !defaultValue.isEmpty()) {
            sb.append(" DEFAULT ").append(defaultValue);
        }
        
        return sb.toString();
    }

    @Override
    public String convertFromOtherDbType(String sourceType, String sourceDbType) {
        String lowerCaseType = sourceType.toLowerCase();
        
        // 从MySQL转换
        if ("mysql".equalsIgnoreCase(sourceDbType)) {
            switch (lowerCaseType) {
                case "int":
                case "integer":
                    return "integer";
                case "tinyint(1)":
                    return "boolean";
                case "tinyint":
                    return "smallint";
                case "mediumint":
                    return "integer";
                case "bigint":
                    return "bigint";
                case "float":
                    return "real";
                case "double":
                    return "double precision";
                case "datetime":
                    return "timestamp without time zone";
                case "timestamp":
                    return "timestamp without time zone";
                case "time":
                    return "time without time zone";
                case "blob":
                case "mediumblob":
                case "longblob":
                    return "bytea";
                case "tinytext":
                case "mediumtext":
                case "longtext":
                    return "text";
                case "json":
                    return "jsonb";
                case "enum":
                    return "text"; // PostgreSQL中可以使用CHECK约束或自定义类型替代ENUM
                case "set":
                    return "text[]"; // PostgreSQL中可以使用数组类型替代SET
            }
        }
        
        // 从Oracle转换
        else if ("oracle".equalsIgnoreCase(sourceDbType)) {
            switch (lowerCaseType) {
                case "number":
                    // Oracle NUMBER类型根据精度决定映射
                    if (lowerCaseType.contains("(")) {
                        // 尝试提取精度和范围
                        Pattern pattern = Pattern.compile("number\\((\\d+)(?:,(\\d+))?\\)");
                        Matcher matcher = pattern.matcher(lowerCaseType);
                        if (matcher.find()) {
                            int precision = Integer.parseInt(matcher.group(1));
                            String scaleStr = matcher.group(2);
                            int scale = scaleStr != null ? Integer.parseInt(scaleStr) : 0;
                            
                            if (scale == 0) {
                                if (precision <= 4) return "smallint";
                                if (precision <= 9) return "integer";
                                if (precision <= 18) return "bigint";
                                return "numeric(" + precision + ")";
                            } else {
                                return "numeric(" + precision + "," + scale + ")";
                            }
                        }
                    }
                    return "numeric";
                case "varchar2":
                case "nvarchar2":
                    return "character varying";
                case "char":
                case "nchar":
                    return "character";
                case "date":
                    return "timestamp without time zone"; // Oracle DATE包含时间部分
                case "timestamp":
                    return "timestamp without time zone";
                case "clob":
                case "nclob":
                    return "text";
                case "blob":
                    return "bytea";
                case "raw":
                case "long raw":
                    return "bytea";
                case "bfile":
                    return "text"; // 需要额外处理，PostgreSQL没有直接对应的类型
            }
        }
        
        // 其他数据库类型或未识别的类型
        return getTypeMapping().getOrDefault(lowerCaseType, sourceType);
    }

    @Override
    public Map<String, Map<String, String>> getCommonTypeMapping() {
        return COMMON_TYPE_MAPPING;
    }

    @Override
    public String getAddTableCommentSql(String tableName, String comment) {
        return String.format("COMMENT ON TABLE %s IS %s",
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
        return String.format("COMMENT ON COLUMN %s.%s IS %s",
            wrapIdentifier(tableName),
            wrapIdentifier(columnName),
            wrapValue(comment)
        );
    }

    @Override
    public String getModifyColumnCommentSql(String tableName, String columnName, String comment) {
        return getAddColumnCommentSql(tableName, columnName, comment);
    }

    @Override
    public String getAlterTableCharsetSql(String tableName, String charset, String collate) {
        // PostgreSQL不支持修改表字符集
        return "";
    }

    @Override
    public String getAlterTableEngineSql(String tableName, String engine) {
        // PostgreSQL不支持存储引擎
        return "";
    }

    @Override
    public String getShowIndexesSql(String tableName) {
        return String.format(
            "SELECT i.relname as index_name, " +
            "a.attname as column_name, " +
            "ix.indisunique as is_unique " +
            "FROM pg_class t, pg_class i, pg_index ix, pg_attribute a " +
            "WHERE t.oid = ix.indrelid AND i.oid = ix.indexrelid " +
            "AND a.attrelid = t.oid AND a.attnum = ANY(ix.indkey) " +
            "AND t.relkind = 'r' AND t.relname = '%s'",
            tableName
        );
    }

    @Override
    public String getAddIndexSql(String tableName, String indexName, List<String> columns, boolean unique) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE ");
        
        if (unique) {
            sb.append("UNIQUE ");
        }
        
        sb.append("INDEX ");
        
        // PostgreSQL的索引命名约定
        if (indexName == null || indexName.isEmpty()) {
            // 生成一个基于表名和列名的索引名
            indexName = tableName + "_" + String.join("_", columns) + "_idx";
        }
        
        sb.append(wrapIdentifier(indexName))
          .append(" ON ")
          .append(wrapIdentifier(tableName))
          .append(" (");
        
        List<String> wrappedColumns = columns.stream()
                .map(this::wrapIdentifier)
                .collect(Collectors.toList());
        
        sb.append(String.join(", ", wrappedColumns));
        sb.append(")");
        
        return sb.toString();
    }

    @Override
    public String getDropIndexSql(String tableName, String indexName) {
        return "DROP INDEX " + wrapIdentifier(indexName);
    }

    @Override
    public String convertCreateTableSql(String sourceSql, String sourceDbType) {
        try {
            TableDefinition tableDefinition = parseCreateTableSql(sourceSql);
            
            for (ColumnDefinition column : tableDefinition.getColumns()) {
                String sourceType = column.getType();
                String targetType = convertDataType(sourceType, sourceDbType, getDatabaseProductName());
                column.setType(targetType);
                adjustPostgreSQLColumnType(column);
            }
            
            return generateCreateTableSql(tableDefinition);
        } catch (Exception e) {
            log.error("Failed to convert CREATE TABLE SQL from {} to PostgreSQL: {}", sourceDbType, e.getMessage());
            throw new RuntimeException("Failed to convert CREATE TABLE SQL", e);
        }
    }

    @Override
    public TableDefinition parseCreateTableSql(String createTableSql) {
        TableDefinition tableDefinition = new TableDefinition();
        
        // 从CREATE TABLE语句中提取表名
        Pattern tableNamePattern = Pattern.compile("CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([`\"\\[\\]]?)([^`\"\\[\\]\\s]+)\\1", Pattern.CASE_INSENSITIVE);
        Matcher tableNameMatcher = tableNamePattern.matcher(createTableSql);
        
        if (tableNameMatcher.find()) {
            String tableName = tableNameMatcher.group(2);
            // 使用正确的方法设置表名
            tableDefinition.setTableName(tableName); // 如果有这个方法
            // 或者
            // tableDefinition.name = tableName; // 如果这是一个公共字段
        } else {
            return tableDefinition; // 无法解析表名
        }
        
        // 提取列定义部分
        int columnStart = createTableSql.indexOf('(');
        int columnEnd = createTableSql.lastIndexOf(')');
        
        if (columnStart == -1 || columnEnd == -1 || columnEnd <= columnStart) {
            return tableDefinition; // 无法解析列定义
        }
        
        String columnsPart = createTableSql.substring(columnStart + 1, columnEnd).trim();
        
        // 分割各个列定义
        String[] columnDefinitions = splitDefinitions(columnsPart);
        List<ColumnDefinition> columns = new ArrayList<>();
        
        for (String def : columnDefinitions) {
            def = def.trim();
            
            // 跳过非列定义（如主键、外键约束等）
            if (def.toUpperCase().startsWith("PRIMARY KEY") || 
                def.toUpperCase().startsWith("FOREIGN KEY") || 
                def.toUpperCase().startsWith("CONSTRAINT") ||
                def.toUpperCase().startsWith("UNIQUE")) {
                continue;
            }
            
            ColumnDefinition column = parseColumnDefinition(def);
            if (column.getName() != null && !column.getName().isEmpty()) {
                columns.add(column);
            }
        }
        
        tableDefinition.setColumns(columns);
        
        // 提取主键信息
        Pattern primaryKeyPattern = Pattern.compile("PRIMARY\\s+KEY\\s+\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);
        Matcher primaryKeyMatcher = primaryKeyPattern.matcher(columnsPart);
        
        if (primaryKeyMatcher.find()) {
            String primaryKeysString = primaryKeyMatcher.group(1);
            String[] primaryKeys = primaryKeysString.split(",");
            
            for (String key : primaryKeys) {
                key = key.trim().replaceAll("[`\"\\[\\]]", "");
                
                // 设置对应列为主键
                for (ColumnDefinition column : columns) {
                    if (column.getName().equalsIgnoreCase(key)) {
                        column.setPrimaryKey(true);
                        break;
                    }
                }
            }
        }
        
        // 提取表注释
        Pattern commentPattern = Pattern.compile("COMMENT\\s+ON\\s+TABLE\\s+[^\\s]+\\s+IS\\s+'([^']*)'", Pattern.CASE_INSENSITIVE);
        Matcher commentMatcher = commentPattern.matcher(createTableSql);
        
        if (commentMatcher.find()) {
            String commentText = commentMatcher.group(1);
            // 由于没有setComment方法，我们需要使用TableDefinition类中正确的方法来设置注释
            // 可能的选项包括：
            tableDefinition.setTableComment(commentText); // 如果有这个方法
            // 或者
            // tableDefinition.tableComment = commentText; // 如果这是一个公共字段
            // 或者
            // 直接将注释存储在tableDefinition的某个Map中
            // tableDefinition.getProperties().put("comment", commentText);
        }
        
        return tableDefinition;
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
        Pattern columnPattern = Pattern.compile(
            "\"?([\\w]+)\"?\\s+" +                    // 列名
            "([\\w\\(\\),]+)" +                       // 数据类型
            "(?:\\s+COLLATE\\s+\"?([\\w\\.]+)\"?)?" + // 排序规则（可选）
            "(?:\\s+DEFAULT\\s+([^\\s,]+))?" +       // 默认值（可选）
            "(?:\\s+NOT\\s+NULL)?" +                 // NOT NULL（可选）
            "(?:\\s+GENERATED\\s+(?:ALWAYS|BY\\s+DEFAULT)\\s+AS\\s+IDENTITY)?" + // IDENTITY（可选）
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

        // 处理排序规则
        if (matcher.group(3) != null) {
            Map<String, String> extraProps = new HashMap<>();
            extraProps.put("COLLATE", matcher.group(3));
            column.setExtraProperties(extraProps);
        }

        if (matcher.group(4) != null) {
            column.setDefaultValue(matcher.group(4));
        }

        column.setNullable(!definition.toUpperCase().contains("NOT NULL"));
        column.setPrimaryKey(definition.toUpperCase().contains("PRIMARY KEY"));
        column.setAutoIncrement(definition.toUpperCase().contains("GENERATED"));

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

        for (ColumnDefinition column : tableDefinition.getColumns()) {
            StringBuilder columnSql = new StringBuilder();
            columnSql.append(wrapIdentifier(column.getName())).append(" ");
            
            String type = column.getType().toUpperCase();
            switch (type) {
                case "VARCHAR":
                case "CHAR":
                case "TEXT":
                case "INTEGER":
                case "INT":
                case "BIGINT":
                case "SMALLINT":
                case "DECIMAL":
                case "NUMERIC":
                case "REAL":
                case "DOUBLE PRECISION":
                case "FLOAT":
                case "BOOLEAN":
                case "DATE":
                case "TIME":
                case "TIMESTAMP":
                case "BYTEA":
                case "UUID":
                case "JSON":
                case "JSONB":
                case "XML":
                case "INET":
                case "CIDR":
                case "MACADDR":
                case "POINT":
                case "LINE":
                case "LSEG":
                case "BOX":
                case "PATH":
                case "POLYGON":
                case "CIRCLE":
                case "INTERVAL":
                case "BIT":
                case "VARBIT":
                case "MONEY":
                    columnSql.append(type);
                    break;
                default:
                    columnSql.append("VARCHAR");
                    break;
            }

            if (column.getLength() != null) {
                if (type.equals("DECIMAL") || type.equals("NUMERIC")) {
                    columnSql.append("(").append(column.getPrecision())
                            .append(",").append(column.getScale()).append(")");
                } else if (!type.equals("TEXT") && !type.equals("BYTEA")) {
                    columnSql.append("(").append(column.getLength()).append(")");
                }
            }

            Map<String, String> extraProps = column.getExtraProperties();
            if (extraProps != null && extraProps.containsKey("COLLATE")) {
                columnSql.append(" COLLATE \"").append(extraProps.get("COLLATE")).append("\"");
            }

            if (!column.isNullable()) {
                columnSql.append(" NOT NULL");
            }

            if (column.getDefaultValue() != null) {
                columnSql.append(" DEFAULT ").append(column.getDefaultValue());
            }

            if (column.isAutoIncrement()) {
                columnSql.append(" GENERATED BY DEFAULT AS IDENTITY");
            }

            if (column.isPrimaryKey()) {
                primaryKeys.add(wrapIdentifier(column.getName()));
            }

            columnDefinitions.add(columnSql.toString());
        }

        if (!primaryKeys.isEmpty()) {
            columnDefinitions.add("PRIMARY KEY (" + String.join(", ", primaryKeys) + ")");
        }

        sql.append(String.join(",\n", columnDefinitions));
        sql.append("\n)");

        Map<String, String> extraProps = tableDefinition.getExtraProperties();
        if (extraProps != null) {
            if (extraProps.containsKey("INHERITS")) {
                sql.append("\nINHERITS (").append(extraProps.get("INHERITS")).append(")");
            }
            if (extraProps.containsKey("STORAGE_PARAMS")) {
                sql.append("\nWITH (").append(extraProps.get("STORAGE_PARAMS")).append(")");
            }
            if (extraProps.containsKey("TABLESPACE")) {
                sql.append("\nTABLESPACE ").append(extraProps.get("TABLESPACE"));
            }
        }

        return sql.toString();
    }

    public void adjustPostgreSQLColumnType(ColumnDefinition column) {
        String type = column.getType().toLowerCase();
        
        // 整型处理
        if (type.equals("int") || type.equals("integer")) {
            column.setType("integer");
            column.setLength(null); // PostgreSQL的integer类型不需要长度
        }
        // 小整型处理
        else if (type.equals("smallint")) {
            column.setType("smallint");
            column.setLength(null);
        }
        // 大整型处理
        else if (type.equals("bigint")) {
            column.setType("bigint");
            column.setLength(null);
        }
        // 布尔类型处理
        else if (type.equals("boolean") || type.equals("bool")) {
            column.setType("boolean");
            column.setLength(null);
        }
        // 文本类型处理
        else if (type.equals("text")) {
            column.setType("text");
            column.setLength(null);
        }
        // JSON类型处理
        else if (type.equals("json")) {
            column.setType("json");
            column.setLength(null);
        }
        else if (type.equals("jsonb")) {
            column.setType("jsonb");
            column.setLength(null);
        }
        // 时间戳类型处理
        else if (type.contains("timestamp")) {
            if (type.contains("with time zone") || type.contains("timezone") || type.equals("timestamptz")) {
                column.setType("timestamp with time zone");
            } else {
                column.setType("timestamp without time zone");
            }
        }
        // 时间类型处理
        else if (type.contains("time") && !type.contains("timestamp")) {
            if (type.contains("with time zone") || type.contains("timezone") || type.equals("timetz")) {
                column.setType("time with time zone");
            } else {
                column.setType("time without time zone");
            }
        }
        // UUID类型处理
        else if (type.equals("uuid")) {
            column.setType("uuid");
            column.setLength(null);
        }
        // SERIAL类型处理（自增长）
        else if (type.equals("serial")) {
            column.setType("serial");
            column.setLength(null);
        }
        // 向量类型
        else if (type.equals("vector")) {
            column.setType("vector");
            // vector类型需要维度参数，如vector(3)
        }
        // HStore类型
        else if (type.equals("hstore")) {
            column.setType("hstore");
            column.setLength(null);
        }
        // Range类型
        else if (type.equals("int4range") || type.equals("int8range") || 
                 type.equals("numrange") || type.equals("tsrange") || 
                 type.equals("tstzrange") || type.equals("daterange")) {
            column.setType(type);
            column.setLength(null);
        }
        // 其他PostGIS地理空间类型
        else if (type.equals("geometry") || type.equals("geography")) {
            column.setType(type);
            // 这些类型可能需要SRID和维度等参数
        }
    }

    @Override
    public List<TableDefinition> getAllTables(String database) throws Exception {
        String sql = "SELECT c.relname as TABLE_NAME, " +
                    "CASE c.relkind " +
                    "    WHEN 'r' THEN 'TABLE' " +
                    "    WHEN 'v' THEN 'VIEW' " +
                    "    WHEN 'm' THEN 'MATERIALIZED VIEW' " +
                    "    WHEN 'f' THEN 'FOREIGN TABLE' " +
                    "END as TABLE_TYPE, " +
                    "obj_description(c.oid, 'pg_class') as TABLE_COMMENT, " +
                    "c.reltuples::bigint as APPROXIMATE_ROW_COUNT, " +
                    "pg_size_pretty(pg_total_relation_size(c.oid)) as TABLE_SIZE " +
                    "FROM pg_class c " +
                    "LEFT JOIN pg_namespace n ON n.oid = c.relnamespace " +
                    "WHERE c.relkind IN ('r','v','m','f') " +
                    "AND n.nspname = ? " +
                    "AND c.relname NOT LIKE 'pg_%' " +
                    "AND c.relname NOT LIKE 'sql_%' " +
                    "ORDER BY c.relname";

        try (Connection conn = getConnection()) {
            List<TableDefinition> tableDefinitions = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, database.toLowerCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        TableDefinition tableDefinition = new TableDefinition();
                        tableDefinition.setTableName(rs.getString("TABLE_NAME"));
                        tableDefinition.setTableComment(rs.getString("TABLE_COMMENT"));
                        // Store additional properties if needed
                        Map<String, String> extraProps = new HashMap<>();
                        extraProps.put("APPROXIMATE_ROW_COUNT", String.valueOf(rs.getLong("APPROXIMATE_ROW_COUNT")));
                        extraProps.put("TABLE_SIZE", rs.getString("TABLE_SIZE"));
                        tableDefinition.setExtraProperties(extraProps);
                        tableDefinitions.add(tableDefinition);
                    }
                }
            }
            return tableDefinitions;
        }
    }

    @Override
    public List<ColumnDefinition> getTableColumns(String database, String tableName) throws Exception {
        String sql = "SELECT " +
                    "    a.attname as COLUMN_NAME, " +
                    "    pg_catalog.format_type(a.atttypid, a.atttypmod) as COLUMN_TYPE, " +
                    "    t.typname as DATA_TYPE, " +
                    "    a.attnotnull as NOT_NULL, " +
                    "    (SELECT pg_catalog.pg_get_expr(d.adbin, d.adrelid) " +
                    "     FROM pg_catalog.pg_attrdef d " +
                    "     WHERE d.adrelid = a.attrelid AND d.adnum = a.attnum AND a.atthasdef) as COLUMN_DEFAULT, " +
                    "    col_description(a.attrelid, a.attnum) as COLUMN_COMMENT, " +
                    "    CASE WHEN pk.attname IS NOT NULL THEN true ELSE false END as IS_PRIMARY_KEY, " +
                    "    a.atttypmod as TYPE_MOD, " +
                    "    information_schema._pg_char_max_length(information_schema._pg_truetypid(a.*, t.*), information_schema._pg_truetypmod(a.*, t.*)) as CHARACTER_MAXIMUM_LENGTH, " +
                    "    information_schema._pg_numeric_precision(information_schema._pg_truetypid(a.*, t.*), information_schema._pg_truetypmod(a.*, t.*)) as NUMERIC_PRECISION, " +
                    "    information_schema._pg_numeric_scale(information_schema._pg_truetypid(a.*, t.*), information_schema._pg_truetypmod(a.*, t.*)) as NUMERIC_SCALE, " +
                    "    a.attidentity as IDENTITY, " +
                    "    a.attgenerated as GENERATED " +
                    "FROM pg_catalog.pg_attribute a " +
                    "INNER JOIN pg_catalog.pg_class c ON c.oid = a.attrelid " +
                    "INNER JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace " +
                    "INNER JOIN pg_catalog.pg_type t ON t.oid = a.atttypid " +
                    "LEFT JOIN ( " +
                    "    SELECT con.conrelid, att.attname " +
                    "    FROM pg_catalog.pg_constraint con " +
                    "    INNER JOIN pg_catalog.pg_attribute att ON " +
                    "        att.attrelid = con.conrelid AND att.attnum = ANY(con.conkey) " +
                    "    WHERE con.contype = 'p' " +
                    ") pk ON pk.conrelid = c.oid AND pk.attname = a.attname " +
                    "WHERE a.attnum > 0 " +
                    "AND NOT a.attisdropped " +
                    "AND n.nspname = ? " +
                    "AND c.relname = ? " +
                    "ORDER BY a.attnum";

        try (Connection conn = getConnection()) {
            List<ColumnDefinition> columnDefinitions = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, database.toLowerCase());
                stmt.setString(2, tableName.toLowerCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ColumnDefinition columnDefinition = new ColumnDefinition();
                        columnDefinition.setName(rs.getString("COLUMN_NAME"));
                        columnDefinition.setType(rs.getString("COLUMN_TYPE"));
                        
                        // 处理最大长度
                        Long maxLength = rs.getLong("CHARACTER_MAXIMUM_LENGTH");
                        if (!rs.wasNull()) {
                            columnDefinition.setLength(maxLength.intValue());
                        }
                        
                        // 处理数值精度和小数位
                        Integer precision = rs.getInt("NUMERIC_PRECISION");
                        if (!rs.wasNull()) {
                            columnDefinition.setPrecision(precision);
                        }
                        
                        Integer scale = rs.getInt("NUMERIC_SCALE");
                        if (!rs.wasNull()) {
                            columnDefinition.setScale(scale);
                        }
                        
                        columnDefinition.setNullable(!rs.getBoolean("NOT_NULL"));
                        columnDefinition.setDefaultValue(rs.getString("COLUMN_DEFAULT"));
                        columnDefinition.setComment(rs.getString("COLUMN_COMMENT"));
                        columnDefinition.setPrimaryKey(rs.getBoolean("IS_PRIMARY_KEY"));
                        
                        // 处理身份列和生成列
                        String identity = rs.getString("IDENTITY");
                        String generated = rs.getString("GENERATED");
                        
                        Map<String, String> extraProps = new HashMap<>();
                        if (identity != null && !identity.isEmpty()) {
                            extraProps.put("IS_IDENTITY", "true");
                            extraProps.put("IDENTITY_TYPE", identity);
                            columnDefinition.setAutoIncrement(true);
                        }
                        
                        if (generated != null && !generated.isEmpty()) {
                            extraProps.put("IS_GENERATED", "true");
                            extraProps.put("GENERATED_TYPE", generated);
                        }
                        
                        if (!extraProps.isEmpty()) {
                            columnDefinition.setExtraProperties(extraProps);
                        }
                        
                        columnDefinitions.add(columnDefinition);
                    }
                }
            }
            return columnDefinitions;
        }
    }

    public List<Map<String, Object>> getTableChangeHistory(String tableName) throws Exception {
        List<Map<String, Object>> history = new ArrayList<>();
        
        try (Connection conn = getConnection()) {
            // 查询表的变更历史记录
            String sql = "SELECT " +
                    "event_timestamp, " +
                    "session_user_name, " +
                    "command_tag, " +
                    "current_query " +
                    "FROM pg_stat_activity " +
                    "WHERE query LIKE ? " +
                    "AND command_tag IN ('ALTER TABLE', 'DROP TABLE', 'CREATE TABLE', 'RENAME') " +
                    "ORDER BY event_timestamp DESC";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, "%" + tableName + "%");
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> change = new HashMap<>();
                        change.put("changeTime", rs.getTimestamp("event_timestamp"));
                        change.put("user", rs.getString("session_user_name"));
                        change.put("operationType", rs.getString("command_tag"));
                        change.put("sql", rs.getString("current_query"));
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
    public String getDatabaseType() {
        return "PostgreSQL";
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
                "SELECT table_name FROM information_schema.tables " +
                "WHERE table_schema = ? AND table_type = 'BASE TABLE'")) {
            stmt.setString(1, getSchema());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString("table_name"));
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
            // 获取表基本信息
            String sql = "SELECT t.table_name, t.table_type, " +
                        "obj_description(c.oid, 'pg_class') as table_comment, " +
                        "pg_size_pretty(pg_total_relation_size(c.oid)) as total_size, " +
                        "pg_size_pretty(pg_relation_size(c.oid)) as table_size, " +
                        "pg_size_pretty(pg_indexes_size(c.oid)) as index_size, " +
                        "c.reltuples as row_estimate " +
                        "FROM information_schema.tables t " +
                        "JOIN pg_class c ON c.relname = t.table_name " +
                        "WHERE t.table_schema = ? AND t.table_name = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        info.put("name", rs.getString("table_name"));
                        info.put("type", rs.getString("table_type"));
                        info.put("comment", rs.getString("table_comment"));
                        info.put("totalSize", rs.getString("total_size"));
                        info.put("tableSize", rs.getString("table_size"));
                        info.put("indexSize", rs.getString("index_size"));
                        info.put("rowEstimate", rs.getLong("row_estimate"));
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取表信息失败: table={}, error={}", tableName, e.getMessage(), e);
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
                    case "integer":
                    case "bigint":
                        sql = String.format(
                            "SELECT COUNT(*) as issue_count " +
                            "FROM %s " +
                            "WHERE %s IS NOT NULL AND %s::text !~ '^-?[0-9]+$'",
                            wrapIdentifier(tableName),
                            wrapIdentifier(columnName),
                            wrapIdentifier(columnName));
                        break;
                        
                    case "numeric":
                    case "decimal":
                    case "real":
                    case "double precision":
                        sql = String.format(
                            "SELECT COUNT(*) as issue_count " +
                            "FROM %s " +
                            "WHERE %s IS NOT NULL AND %s::text !~ '^-?[0-9]*.?[0-9]+$'",
                            wrapIdentifier(tableName),
                            wrapIdentifier(columnName),
                            wrapIdentifier(columnName));
                        break;
                        
                    case "date":
                        sql = String.format(
                            "SELECT COUNT(*) as issue_count " +
                            "FROM %s " +
                            "WHERE %s IS NOT NULL AND %s::text !~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}$'",
                            wrapIdentifier(tableName),
                            wrapIdentifier(columnName),
                            wrapIdentifier(columnName));
                        break;
                        
                    case "timestamp":
                    case "timestamptz":
                        sql = String.format(
                            "SELECT COUNT(*) as issue_count " +
                            "FROM %s " +
                            "WHERE %s IS NOT NULL AND %s::text !~ '^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}'",
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
    public List<Map<String, Object>> listColumns(String tableName) throws Exception {
        List<Map<String, Object>> columns = new ArrayList<>();
        try (Connection conn = getConnection()) {
            String sql = "SELECT c.column_name, c.data_type, c.character_maximum_length, " +
                        "c.numeric_precision, c.numeric_scale, c.is_nullable, " +
                        "c.column_default, c.ordinal_position, " +
                        "pg_catalog.col_description(format('%s.%s', c.table_schema, c.table_name)::regclass::oid, c.ordinal_position) as column_comment " +
                        "FROM information_schema.columns c " +
                        "WHERE c.table_schema = ? AND c.table_name = ? " +
                        "ORDER BY c.ordinal_position";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> column = new HashMap<>();
                        column.put("name", rs.getString("column_name"));
                        column.put("type", rs.getString("data_type"));
                        column.put("maxLength", rs.getObject("character_maximum_length"));
                        column.put("precision", rs.getObject("numeric_precision"));
                        column.put("scale", rs.getObject("numeric_scale"));
                        column.put("nullable", "YES".equalsIgnoreCase(rs.getString("is_nullable")));
                        column.put("defaultValue", rs.getString("column_default"));
                        column.put("comment", rs.getString("column_comment"));
                        column.put("position", rs.getInt("ordinal_position"));
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
            String sql = "SELECT c.column_name, c.data_type, c.character_maximum_length, " +
                        "c.numeric_precision, c.numeric_scale, c.is_nullable, " +
                        "c.column_default, c.ordinal_position, " +
                        "pg_catalog.col_description(format('%s.%s', c.table_schema, c.table_name)::regclass::oid, c.ordinal_position) as column_comment, " +
                        "CASE WHEN pk.column_name IS NOT NULL THEN true ELSE false END as is_primary_key " +
                        "FROM information_schema.columns c " +
                        "LEFT JOIN (" +
                        "    SELECT ku.column_name " +
                        "    FROM information_schema.table_constraints tc " +
                        "    JOIN information_schema.key_column_usage ku ON tc.constraint_name = ku.constraint_name " +
                        "    WHERE tc.constraint_type = 'PRIMARY KEY' AND tc.table_schema = ? AND tc.table_name = ?" +
                        ") pk ON c.column_name = pk.column_name " +
                        "WHERE c.table_schema = ? AND c.table_name = ? AND c.column_name = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);
                stmt.setString(3, getSchema());
                stmt.setString(4, tableName);
                stmt.setString(5, columnName);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        info.put("name", rs.getString("column_name"));
                        info.put("type", rs.getString("data_type"));
                        info.put("maxLength", rs.getObject("character_maximum_length"));
                        info.put("precision", rs.getObject("numeric_precision"));
                        info.put("scale", rs.getObject("numeric_scale"));
                        info.put("nullable", "YES".equalsIgnoreCase(rs.getString("is_nullable")));
                        info.put("defaultValue", rs.getString("column_default"));
                        info.put("comment", rs.getString("column_comment"));
                        info.put("position", rs.getInt("ordinal_position"));
                        info.put("isPrimaryKey", rs.getBoolean("is_primary_key"));
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
    public String getTableEngine(String tableName) throws Exception {
        // PostgreSQL没有存储引擎的概念，返回默认值
        return "PostgreSQL";
    }

    @Override
    public String getTableCharset(String tableName) throws Exception {
        String charset = null;
        try (Connection conn = getConnection()) {
            String sql = "SELECT pg_encoding_to_char(encoding) as encoding " +
                        "FROM pg_database " +
                        "WHERE datname = current_database()";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    charset = rs.getString("encoding");
                }
            }
        } catch (Exception e) {
            log.error("获取表字符集失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return charset;
    }

    @Override
    public String getTableCollation(String tableName) throws Exception {
        String collation = null;
        try (Connection conn = getConnection()) {
            String sql = "SELECT c.collname " +
                        "FROM pg_class rel " +
                        "JOIN pg_collation c ON rel.relcollation = c.oid " +
                        "WHERE rel.relname = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        collation = rs.getString("collname");
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取表排序规则失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return collation;
    }

    @Override
    public Long getTableSize(String tableName) throws Exception {
        Long size = null;
        try (Connection conn = getConnection()) {
            String sql = "SELECT pg_total_relation_size(?) as total_size";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema() + "." + tableName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        size = rs.getLong("total_size");
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
        Long count = null;
        try (Connection conn = getConnection()) {
            String sql = "SELECT reltuples::bigint as row_count " +
                        "FROM pg_class " +
                        "WHERE relname = ? AND relkind = 'r'";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        count = rs.getLong("row_count");
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取表行数失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return count;
    }

    @Override
    public String getTableSpace(String tableName) throws Exception {
        String sql = "SELECT t.spcname FROM pg_class c " +
                    "JOIN pg_tablespace t ON c.reltablespace = t.oid " +
                    "WHERE c.relname = '" + tableName + "' " +
                    "AND c.relkind = 'r'";
        
        List<Map<String, Object>> result = executeQuery(sql);
        if (!result.isEmpty() && result.get(0).containsKey("spcname")) {
            return (String) result.get(0).get("spcname");
        }
        
        return "pg_default"; // 默认表空间
    }

    // 获取表所有者
    public String getTableOwner(String tableName) throws Exception {
        String sql = "SELECT u.usename AS owner FROM pg_class c " +
                    "JOIN pg_user u ON c.relowner = u.usesysid " +
                    "WHERE c.relname = '" + tableName + "' " +
                    "AND c.relkind = 'r'";
        
        List<Map<String, Object>> result = executeQuery(sql);
        if (!result.isEmpty() && result.get(0).containsKey("owner")) {
            return (String) result.get(0).get("owner");
        }
        
        return null;
    }

    @Override
    public Integer getCharacterLength(String tableName, String columnName) throws Exception {
        Integer length = null;
        try (Connection conn = getConnection()) {
            String sql = "SELECT character_maximum_length " +
                        "FROM information_schema.columns " +
                        "WHERE table_schema = ? AND table_name = ? AND column_name = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);
                stmt.setString(3, columnName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        length = rs.getObject("character_maximum_length") != null ? 
                                rs.getInt("character_maximum_length") : null;
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
        Integer precision = null;
        try (Connection conn = getConnection()) {
            String sql = "SELECT numeric_precision " +
                        "FROM information_schema.columns " +
                        "WHERE table_schema = ? AND table_name = ? AND column_name = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);
                stmt.setString(3, columnName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        precision = rs.getObject("numeric_precision") != null ? 
                                   rs.getInt("numeric_precision") : null;
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取字段数值精度失败: table={}, column={}, error={}", 
                     tableName, columnName, e.getMessage(), e);
            throw e;
        }
        return precision;
    }

    @Override
    public Integer getNumericScale(String tableName, String columnName) throws Exception {
        Integer scale = null;
        try (Connection conn = getConnection()) {
            String sql = "SELECT numeric_scale " +
                        "FROM information_schema.columns " +
                        "WHERE table_schema = ? AND table_name = ? AND column_name = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);
                stmt.setString(3, columnName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        scale = rs.getObject("numeric_scale") != null ? 
                               rs.getInt("numeric_scale") : null;
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
        String defaultValue = null;
        try (Connection conn = getConnection()) {
            String sql = "SELECT column_default " +
                        "FROM information_schema.columns " +
                        "WHERE table_schema = ? AND table_name = ? AND column_name = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);
                stmt.setString(3, columnName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        defaultValue = rs.getString("column_default");
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取字段默认值失败: table={}, column={}, error={}", 
                     tableName, columnName, e.getMessage(), e);
            throw e;
        }
        return defaultValue;
    }

    @Override
    public String getColumnExtra(String tableName, String columnName) throws Exception {
        String extra = null;
        try (Connection conn = getConnection()) {
            String sql = "SELECT column_default " +
                        "FROM information_schema.columns " +
                        "WHERE table_schema = ? AND table_name = ? AND column_name = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);
                stmt.setString(3, columnName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String defaultValue = rs.getString("column_default");
                        if (defaultValue != null && defaultValue.contains("nextval")) {
                            extra = "auto_increment";
                        }
                    }
                }
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
            String sql = "SELECT ordinal_position " +
                        "FROM information_schema.columns " +
                        "WHERE table_schema = ? AND table_name = ? AND column_name = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);
                stmt.setString(3, columnName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        position = rs.getInt("ordinal_position");
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
        // PostgreSQL不直接存储表的创建时间，可以通过pg_stat_user_tables获取近似值
        Date createTime = null;
        try (Connection conn = getConnection()) {
            String sql = "SELECT min(statime) as create_time " +
                        "FROM pg_stat_user_tables " +
                        "WHERE schemaname = ? AND relname = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        createTime = rs.getTimestamp("create_time");
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
            String sql = "SELECT last_vacuum as update_time " +
                        "FROM pg_stat_user_tables " +
                        "WHERE schemaname = ? AND relname = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        updateTime = rs.getTimestamp("update_time");
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
                            double completeness = 1.0 - (double)nullCount / totalRows;
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
                    String dataType = ((String) column.get("type")).toLowerCase();
                    
                    String validationSql = getValidationSqlForDataType(tableName, columnName, dataType);
                    if (validationSql != null) {
                        try (Statement stmt = conn.createStatement();
                             ResultSet rs = stmt.executeQuery(validationSql)) {
                            if (rs.next()) {
                                long invalidCount = rs.getLong("invalid_count");
                                double accuracy = 1.0 - (double)invalidCount / totalRows;
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
        switch (dataType) {
            case "integer":
            case "bigint":
                return String.format(
                    "SELECT COUNT(*) as invalid_count " +
                    "FROM %s " +
                    "WHERE %s IS NOT NULL AND %s::text !~ '^-?[0-9]+$'",
                    wrapIdentifier(tableName),
                    wrapIdentifier(columnName),
                    wrapIdentifier(columnName));
                
            case "numeric":
            case "decimal":
            case "real":
            case "double precision":
                return String.format(
                    "SELECT COUNT(*) as invalid_count " +
                    "FROM %s " +
                    "WHERE %s IS NOT NULL AND %s::text !~ '^-?[0-9]*.?[0-9]+$'",
                    wrapIdentifier(tableName),
                    wrapIdentifier(columnName),
                    wrapIdentifier(columnName));
                
            case "date":
                return String.format(
                    "SELECT COUNT(*) as invalid_count " +
                    "FROM %s " +
                    "WHERE %s IS NOT NULL AND %s::text !~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}$'",
                    wrapIdentifier(tableName),
                    wrapIdentifier(columnName),
                    wrapIdentifier(columnName));
                
            case "timestamp":
            case "timestamptz":
                return String.format(
                    "SELECT COUNT(*) as invalid_count " +
                    "FROM %s " +
                    "WHERE %s IS NOT NULL AND %s::text !~ '^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}'",
                    wrapIdentifier(tableName),
                    wrapIdentifier(columnName),
                    wrapIdentifier(columnName));
                
            default:
                return null;
        }
    }

    @Override
    public List<String> getPrimaryKeys(String tableName) throws Exception {
        List<String> primaryKeys = new ArrayList<>();
        try (Connection conn = getConnection()) {
            String sql = "SELECT kcu.column_name " +
                        "FROM information_schema.table_constraints tc " +
                        "JOIN information_schema.key_column_usage kcu " +
                        "ON tc.constraint_name = kcu.constraint_name " +
                        "WHERE tc.constraint_type = 'PRIMARY KEY' " +
                        "AND tc.table_schema = ? AND tc.table_name = ? " +
                        "ORDER BY kcu.ordinal_position";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        primaryKeys.add(rs.getString("column_name"));
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
                        "tc.constraint_name, " +
                        "kcu.column_name, " +
                        "ccu.table_name as referenced_table, " +
                        "ccu.column_name as referenced_column, " +
                        "rc.update_rule, " +
                        "rc.delete_rule " +
                        "FROM information_schema.table_constraints tc " +
                        "JOIN information_schema.key_column_usage kcu " +
                        "ON tc.constraint_name = kcu.constraint_name " +
                        "JOIN information_schema.constraint_column_usage ccu " +
                        "ON tc.constraint_name = ccu.constraint_name " +
                        "JOIN information_schema.referential_constraints rc " +
                        "ON tc.constraint_name = rc.constraint_name " +
                        "WHERE tc.constraint_type = 'FOREIGN KEY' " +
                        "AND tc.table_schema = ? AND tc.table_name = ? " +
                        "ORDER BY tc.constraint_name, kcu.ordinal_position";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> foreignKey = new HashMap<>();
                        foreignKey.put("constraintName", rs.getString("constraint_name"));
                        foreignKey.put("columnName", rs.getString("column_name"));
                        foreignKey.put("referencedTable", rs.getString("referenced_table"));
                        foreignKey.put("referencedColumn", rs.getString("referenced_column"));
                        foreignKey.put("updateRule", rs.getString("update_rule"));
                        foreignKey.put("deleteRule", rs.getString("delete_rule"));
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
    public List<Map<String, Object>> getIndexes(String tableName) throws Exception {
        List<Map<String, Object>> indexes = new ArrayList<>();
        try (Connection conn = getConnection()) {
            String sql = "SELECT " +
                        "i.relname as index_name, " +
                        "a.attname as column_name, " +
                        "ix.indisunique as is_unique, " +
                        "ix.indisprimary as is_primary, " +
                        "am.amname as index_type " +
                        "FROM pg_index ix " +
                        "JOIN pg_class i ON i.oid = ix.indexrelid " +
                        "JOIN pg_class t ON t.oid = ix.indrelid " +
                        "JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY(ix.indkey) " +
                        "JOIN pg_am am ON i.relam = am.oid " +
                        "WHERE t.relname = ? " +
                        "ORDER BY i.relname, a.attnum";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName);
                
                Map<String, Map<String, Object>> indexMap = new HashMap<>();
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String indexName = rs.getString("index_name");
                        String columnName = rs.getString("column_name");
                        boolean isUnique = rs.getBoolean("is_unique");
                        boolean isPrimary = rs.getBoolean("is_primary");
                        String indexType = rs.getString("index_type");
                        
                        Map<String, Object> indexInfo = indexMap.get(indexName);
                        if (indexInfo == null) {
                            indexInfo = new HashMap<>();
                            indexInfo.put("name", indexName);
                            indexInfo.put("unique", isUnique);
                            indexInfo.put("primary", isPrimary);
                            indexInfo.put("type", indexType);
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
            String pkSql = "SELECT COUNT(*) as violation_count " +
                          "FROM (SELECT COUNT(*) " +
                          "FROM (SELECT DISTINCT ";
            
            List<String> primaryKeys = getPrimaryKeys(tableName);
            if (primaryKeys.isEmpty()) {
                result.put("primaryKeyConsistency", true);
            } else {
                pkSql += String.join(", ", primaryKeys);
                pkSql += " FROM " + wrapIdentifier(tableName) + ") t " +
                        "GROUP BY " + String.join(", ", primaryKeys) + " " +
                        "HAVING COUNT(*) > 1) v";
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(pkSql)) {
                    if (rs.next()) {
                        result.put("primaryKeyConsistency", rs.getLong("violation_count") == 0);
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
        Map<String, Object> result = new HashMap<>();
        try (Connection conn = getConnection()) {
            // 获取所有唯一索引列
            String sql = "SELECT " +
                        "i.relname as index_name, " +
                        "array_agg(a.attname) as columns " +
                        "FROM pg_index ix " +
                        "JOIN pg_class i ON i.oid = ix.indexrelid " +
                        "JOIN pg_class t ON t.oid = ix.indrelid " +
                        "JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY(ix.indkey) " +
                        "WHERE t.relname = ? AND ix.indisunique = true " +
                        "GROUP BY i.relname";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName);
                
                List<Map<String, Object>> uniqueIndexes = new ArrayList<>();
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> index = new HashMap<>();
                        index.put("indexName", rs.getString("index_name"));
                        index.put("columns", rs.getArray("columns").getArray());
                        uniqueIndexes.add(index);
                    }
                }
                
                result.put("uniqueIndexes", uniqueIndexes);
                
                // 检查每组唯一列的重复值
                for (Map<String, Object> index : uniqueIndexes) {
                    String[] columns = (String[]) index.get("columns");
                    String indexName = (String) index.get("indexName");
                    
                    String duplicateCheckSql = String.format(
                        "SELECT COUNT(*) as total_count, " +
                        "COUNT(DISTINCT %s) as unique_count " +
                        "FROM %s",
                        String.join(",", columns),
                        wrapIdentifier(tableName));
                    
                    try (Statement checkStmt = conn.createStatement();
                         ResultSet rs = checkStmt.executeQuery(duplicateCheckSql)) {
                        if (rs.next()) {
                            double uniqueness = (double)rs.getLong("unique_count") / rs.getLong("total_count");
                            result.put(indexName + "_uniqueness", uniqueness);
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
        try (Connection conn = getConnection()) {
            // 获取所有列的数据类型
            List<Map<String, Object>> columns = listColumns(tableName);
            
            Map<String, Long> invalidCounts = new HashMap<>();
            long totalRows = getTableRowCount(tableName);
            
            for (Map<String, Object> column : columns) {
                String columnName = (String) column.get("name");
                String dataType = ((String) column.get("type")).toLowerCase();
                
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
                    .sum() / (double)(totalRows * invalidCounts.size());
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
                String dataType = ((String) column.get("type")).toLowerCase();
                
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
                String dataType = ((String) column.get("type")).toLowerCase();
                
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
                        "tc.constraint_name, " +
                        "kcu.column_name, " +
                        "ccu.table_name as referenced_table, " +
                        "ccu.column_name as referenced_column, " +
                        "rc.update_rule, " +
                        "rc.delete_rule " +
                        "FROM information_schema.table_constraints tc " +
                        "JOIN information_schema.key_column_usage kcu " +
                        "ON tc.constraint_name = kcu.constraint_name " +
                        "JOIN information_schema.constraint_column_usage ccu " +
                        "ON tc.constraint_name = ccu.constraint_name " +
                        "JOIN information_schema.referential_constraints rc " +
                        "ON tc.constraint_name = rc.constraint_name " +
                        "WHERE tc.constraint_type = 'FOREIGN KEY' " +
                        "AND tc.table_schema = ? AND tc.table_name = ? " +
                        "ORDER BY tc.constraint_name, kcu.ordinal_position";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> relation = new HashMap<>();
                        relation.put("constraintName", rs.getString("constraint_name"));
                        relation.put("sourceColumn", rs.getString("column_name"));
                        relation.put("referencedTable", rs.getString("referenced_table"));
                        relation.put("referencedColumn", rs.getString("referenced_column"));
                        relation.put("updateRule", rs.getString("update_rule"));
                        relation.put("deleteRule", rs.getString("delete_rule"));
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
                        "tc.table_name as referencing_table, " +
                        "kcu.column_name as referencing_column, " +
                        "ccu.column_name as referenced_column, " +
                        "tc.constraint_name, " +
                        "rc.update_rule, " +
                        "rc.delete_rule " +
                        "FROM information_schema.table_constraints tc " +
                        "JOIN information_schema.key_column_usage kcu " +
                        "ON tc.constraint_name = kcu.constraint_name " +
                        "JOIN information_schema.constraint_column_usage ccu " +
                        "ON tc.constraint_name = ccu.constraint_name " +
                        "JOIN information_schema.referential_constraints rc " +
                        "ON tc.constraint_name = rc.constraint_name " +
                        "WHERE tc.constraint_type = 'FOREIGN KEY' " +
                        "AND tc.table_schema = ? AND ccu.table_name = ? " +
                        "ORDER BY tc.constraint_name, kcu.ordinal_position";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> relation = new HashMap<>();
                        relation.put("referencingTable", rs.getString("referencing_table"));
                        relation.put("referencingColumn", rs.getString("referencing_column"));
                        relation.put("referencedColumn", rs.getString("referenced_column"));
                        relation.put("constraintName", rs.getString("constraint_name"));
                        relation.put("updateRule", rs.getString("update_rule"));
                        relation.put("deleteRule", rs.getString("delete_rule"));
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
                if (dataType.matches("integer|bigint|numeric|decimal|real|double precision")) {
                    basicStatsSql += String.format(
                        ", MIN(%s) as min_value" +
                        ", MAX(%s) as max_value" +
                        ", AVG(%s) as avg_value" +
                        ", STDDEV(%s) as std_value",
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
                        
                        if (dataType.matches("integer|bigint|numeric|decimal|real|double precision")) {
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
                    "SELECT data_type FROM information_schema.columns " +
                    "WHERE table_schema = ? AND table_name = ? AND column_name = ?")) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName);
                stmt.setString(3, columnName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        dataType = rs.getString("data_type").toLowerCase();
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

                if (dataType.matches("integer|bigint|numeric|decimal|real|double precision")) {
                    sql += String.format(
                        ", MIN(%s) as min_value" +
                        ", MAX(%s) as max_value" +
                        ", AVG(%s) as avg_value" +
                        ", STDDEV(%s) as std_value",
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

                        if (dataType.matches("integer|bigint|numeric|decimal|real|double precision")) {
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

    public Date getDate() throws Exception {
        Date date = null;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT CURRENT_TIMESTAMP")) {
            if (rs.next()) {
                date = rs.getTimestamp(1);
            }
        } catch (Exception e) {
            log.error("获取数据库日期失败: error={}", e.getMessage(), e);
            throw e;
        }
        return date != null ? date : new Date();
    }

    @Override
    public List<Map<String, Object>> getTableDependencies(String tableName) throws Exception {
        List<Map<String, Object>> dependencies = new ArrayList<>();
        try (Connection conn = getConnection()) {
            // 1. 获取视图依赖
            String viewSql = "SELECT " +
                           "v.table_name as dependent_object, " +
                           "'VIEW' as object_type, " +
                           "v.view_definition as dependency_definition " +
                           "FROM information_schema.views v " +
                           "WHERE v.table_schema = ? " +
                           "AND v.view_definition LIKE ?";
            
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
            
            // 3. 获取函数依赖
            String functionSql = "SELECT " +
                               "p.proname as dependent_object, " +
                               "'FUNCTION' as object_type, " +
                               "pg_get_functiondef(p.oid) as function_definition " +
                               "FROM pg_proc p " +
                               "JOIN pg_namespace n ON p.pronamespace = n.oid " +
                               "WHERE n.nspname = ? " +
                               "AND pg_get_functiondef(p.oid) LIKE ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(functionSql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, "%" + tableName + "%");
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> dependency = new HashMap<>();
                        dependency.put("name", rs.getString("dependent_object"));
                        dependency.put("type", rs.getString("object_type"));
                        dependency.put("definition", rs.getString("function_definition"));
                        dependency.put("dependencyType", "Referenced by Function");
                        dependencies.add(dependency);
                    }
                }
            }
            
            // 4. 获取触发器依赖
            String triggerSql = "SELECT " +
                              "t.tgname as dependent_object, " +
                              "'TRIGGER' as object_type, " +
                              "pg_get_triggerdef(t.oid) as trigger_definition " +
                              "FROM pg_trigger t " +
                              "JOIN pg_class c ON t.tgrelid = c.oid " +
                              "JOIN pg_namespace n ON c.relnamespace = n.oid " +
                              "WHERE n.nspname = ? " +
                              "AND pg_get_triggerdef(t.oid) LIKE ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(triggerSql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, "%" + tableName + "%");
                
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
        } catch (Exception e) {
            log.error("获取表依赖关系失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        return dependencies;
    }

    @Override
    public List<Map<String, Object>> getColumnValueDistribution(String tableName, String columnName, int topN) throws Exception {
        List<Map<String, Object>> distribution = new ArrayList<>();
        try (Connection conn = getConnection()) {
            // 获取字段类型
            String dataType = null;
            try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT data_type FROM information_schema.columns " +
                "WHERE table_schema = ? AND table_name = ? AND column_name = ?")) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName.toLowerCase());
                stmt.setString(3, columnName.toLowerCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        dataType = rs.getString("data_type").toLowerCase();
                    }
                }
            }
            
            if (dataType == null) {
                throw new Exception("Column not found: " + columnName);
            }
            
            // 获取总行数（用于计算百分比）
            long totalRows = 0;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + wrapIdentifier(tableName))) {
                if (rs.next()) {
                    totalRows = rs.getLong(1);
                }
            }
            
            // 构建SQL查询
            String sql = String.format(
                "SELECT %s as value, COUNT(*) as count " +
                "FROM %s " +
                "WHERE %s IS NOT NULL " +
                "GROUP BY %s " +
                "ORDER BY COUNT(*) DESC " +
                "LIMIT %d",
                wrapIdentifier(columnName),
                wrapIdentifier(tableName),
                wrapIdentifier(columnName),
                wrapIdentifier(columnName),
                topN
            );
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    Object value = rs.getObject("value");
                    long count = rs.getLong("count");
                    
                    item.put("value", value);
                    item.put("count", count);
                    
                    // 计算百分比
                    if (totalRows > 0) {
                        double percentage = (double) count / totalRows * 100;
                        item.put("percentage", percentage);
                    }
                    
                    distribution.add(item);
                }
            }
            
            // 对于日期时间类型，可以考虑按时间段分组
            if (dataType.matches("date|timestamp.*") && distribution.isEmpty()) {
                String timeSql = null;
                
                if (dataType.equals("date")) {
                    timeSql = String.format(
                        "SELECT EXTRACT(YEAR FROM %s) as year, EXTRACT(MONTH FROM %s) as month, COUNT(*) as count " +
                        "FROM %s " +
                        "WHERE %s IS NOT NULL " +
                        "GROUP BY EXTRACT(YEAR FROM %s), EXTRACT(MONTH FROM %s) " +
                        "ORDER BY year, month " +
                        "LIMIT %d",
                        wrapIdentifier(columnName),
                        wrapIdentifier(columnName),
                        wrapIdentifier(tableName),
                        wrapIdentifier(columnName),
                        wrapIdentifier(columnName),
                        wrapIdentifier(columnName),
                        topN
                    );
                } else if (dataType.startsWith("timestamp")) {
                    timeSql = String.format(
                        "SELECT EXTRACT(YEAR FROM %s) as year, EXTRACT(MONTH FROM %s) as month, " +
                        "EXTRACT(DAY FROM %s) as day, COUNT(*) as count " +
                        "FROM %s " +
                        "WHERE %s IS NOT NULL " +
                        "GROUP BY EXTRACT(YEAR FROM %s), EXTRACT(MONTH FROM %s), EXTRACT(DAY FROM %s) " +
                        "ORDER BY year, month, day " +
                        "LIMIT %d",
                        wrapIdentifier(columnName),
                        wrapIdentifier(columnName),
                        wrapIdentifier(columnName),
                        wrapIdentifier(tableName),
                        wrapIdentifier(columnName),
                        wrapIdentifier(columnName),
                        wrapIdentifier(columnName),
                        wrapIdentifier(columnName),
                        topN
                    );
                }
                
                if (timeSql != null) {
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(timeSql)) {
                        
                        while (rs.next() && distribution.size() < topN) {
                            Map<String, Object> item = new HashMap<>();
                            
                            if (dataType.equals("date")) {
                                item.put("year", rs.getInt("year"));
                                item.put("month", rs.getInt("month"));
                                item.put("period", rs.getInt("year") + "-" + rs.getInt("month"));
                            } else {
                                item.put("year", rs.getInt("year"));
                                item.put("month", rs.getInt("month"));
                                item.put("day", rs.getInt("day"));
                                item.put("period", rs.getInt("year") + "-" + rs.getInt("month") + "-" + rs.getInt("day"));
                            }
                            
                            long count = rs.getLong("count");
                            item.put("count", count);
                            
                            // 计算百分比
                            if (totalRows > 0) {
                                double percentage = (double) count / totalRows * 100;
                                item.put("percentage", percentage);
                            }
                            
                            distribution.add(item);
                        }
                    } catch (SQLException e) {
                        log.warn("按时间分组获取分布失败: table={}, column={}, error={}", 
                                tableName, columnName, e.getMessage());
                    }
                }
            }
            
            // 对于数值类型，可以考虑按范围分组
            if (dataType.matches("int.*|float.*|double.*|decimal|numeric|real|bigint|smallint") && distribution.isEmpty()) {
                // 获取最大值和最小值
                Map<String, Object> range = getColumnValueRange(tableName, columnName);
                if (range.containsKey("minValue") && range.containsKey("maxValue")) {
                    Object minObj = range.get("minValue");
                    Object maxObj = range.get("maxValue");
                    
                    if (minObj != null && maxObj != null) {
                        double min = ((Number) minObj).doubleValue();
                        double max = ((Number) maxObj).doubleValue();
                        
                        // 计算区间大小
                        double interval = (max - min) / 10;
                        if (interval > 0) {
                            for (int i = 0; i < 10 && distribution.size() < topN; i++) {
                                double lowerBound = min + i * interval;
                                double upperBound = min + (i + 1) * interval;
                                
                                String rangeSql = String.format(
                                    "SELECT COUNT(*) as count " +
                                    "FROM %s " +
                                    "WHERE %s >= %f AND %s < %f",
                                    wrapIdentifier(tableName),
                                    wrapIdentifier(columnName),
                                    lowerBound,
                                    wrapIdentifier(columnName),
                                    upperBound
                                );
                                
                                try (Statement stmt = conn.createStatement();
                                     ResultSet rs = stmt.executeQuery(rangeSql)) {
                                    if (rs.next()) {
                                        Map<String, Object> item = new HashMap<>();
                                        item.put("lowerBound", lowerBound);
                                        item.put("upperBound", upperBound);
                                        item.put("range", String.format("[%.2f, %.2f)", lowerBound, upperBound));
                                        
                                        long count = rs.getLong("count");
                                        item.put("count", count);
                                        
                                        // 计算百分比
                                        if (totalRows > 0) {
                                            double percentage = (double) count / totalRows * 100;
                                            item.put("percentage", percentage);
                                        }
                                        
                                        distribution.add(item);
                                    }
                                } catch (SQLException e) {
                                    log.warn("按范围分组获取分布失败: table={}, column={}, error={}", 
                                            tableName, columnName, e.getMessage());
                                }
                            }
                        }
                    }
                }
            }
            
            // 对于枚举类型，获取所有可能的值
            if (dataType.equals("enum") && distribution.isEmpty()) {
                // PostgreSQL没有内置的枚举类型，但可以使用自定义类型
                // 这里假设使用了自定义的枚举类型
                String enumSql = String.format(
                    "SELECT pg_enum.enumlabel as value, COUNT(*) as count " +
                    "FROM %s " +
                    "JOIN pg_enum ON %s::text = pg_enum.enumlabel " +
                    "WHERE %s IS NOT NULL " +
                    "GROUP BY pg_enum.enumlabel " +
                    "ORDER BY count DESC " +
                    "LIMIT %d",
                    wrapIdentifier(tableName),
                    wrapIdentifier(columnName),
                    wrapIdentifier(columnName),
                    topN
                );
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(enumSql)) {
                    
                    while (rs.next()) {
                        Map<String, Object> item = new HashMap<>();
                        String value = rs.getString("value");
                        long count = rs.getLong("count");
                        
                        item.put("value", value);
                        item.put("count", count);
                        
                        // 计算百分比
                        if (totalRows > 0) {
                            double percentage = (double) count / totalRows * 100;
                            item.put("percentage", percentage);
                        }
                        
                        distribution.add(item);
                    }
                } catch (SQLException e) {
                    log.warn("获取枚举分布失败: table={}, column={}, error={}", 
                            tableName, columnName, e.getMessage());
                }
            }
            
            // 对于布尔类型，只有两个可能的值
            if (dataType.equals("boolean") && distribution.isEmpty()) {
                String boolSql = String.format(
                    "SELECT %s as value, COUNT(*) as count " +
                    "FROM %s " +
                    "WHERE %s IS NOT NULL " +
                    "GROUP BY %s",
                    wrapIdentifier(columnName),
                    wrapIdentifier(tableName),
                    wrapIdentifier(columnName),
                    wrapIdentifier(columnName)
                );
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(boolSql)) {
                    
                    while (rs.next()) {
                        Map<String, Object> item = new HashMap<>();
                        boolean value = rs.getBoolean("value");
                        long count = rs.getLong("count");
                        
                        item.put("value", value);
                        item.put("count", count);
                        
                        // 计算百分比
                        if (totalRows > 0) {
                            double percentage = (double) count / totalRows * 100;
                            item.put("percentage", percentage);
                        }
                        
                        distribution.add(item);
                    }
                } catch (SQLException e) {
                    log.warn("获取布尔分布失败: table={}, column={}, error={}", 
                            tableName, columnName, e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("获取字段值分布失败: table={}, column={}, topN={}, error={}", 
                     tableName, columnName, topN, e.getMessage(), e);
            throw e;
        }
        return distribution;
    }

    @Override
    public Map<String, Object> getColumnValueRange(String tableName, String columnName) throws Exception {
        Map<String, Object> range = new HashMap<>();
        try (Connection conn = getConnection()) {
            // 获取字段类型
            String dataType = null;
            try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT data_type FROM information_schema.columns " +
                "WHERE table_schema = ? AND table_name = ? AND column_name = ?")) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName.toLowerCase());
                stmt.setString(3, columnName.toLowerCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        dataType = rs.getString("data_type").toLowerCase();
                        range.put("dataType", dataType);
                    }
                }
            }
            
            if (dataType == null) {
                throw new Exception("Column not found: " + columnName);
            }
            
            // 对不同类型的字段获取不同的范围信息
            if (dataType.matches("int.*|float.*|double.*|decimal|numeric|real|bigint|smallint")) {
                // 数值类型
                String sql = String.format(
                    "SELECT MIN(%s) as min_value, MAX(%s) as max_value, " +
                    "AVG(%s) as avg_value, STDDEV(%s) as std_value " +
                    "FROM %s WHERE %s IS NOT NULL",
                    wrapIdentifier(columnName),
                    wrapIdentifier(columnName),
                    wrapIdentifier(columnName),
                    wrapIdentifier(columnName),
                    wrapIdentifier(tableName),
                    wrapIdentifier(columnName)
                );
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    if (rs.next()) {
                        range.put("minValue", rs.getObject("min_value"));
                        range.put("maxValue", rs.getObject("max_value"));
                        range.put("avgValue", rs.getDouble("avg_value"));
                        range.put("stdValue", rs.getDouble("std_value"));
                    }
                }
                
                // 计算分位数
                try {
                    String percentileSql = String.format(
                        "SELECT " +
                        "percentile_cont(0.25) WITHIN GROUP (ORDER BY %s) as p25, " +
                        "percentile_cont(0.5) WITHIN GROUP (ORDER BY %s) as p50, " +
                        "percentile_cont(0.75) WITHIN GROUP (ORDER BY %s) as p75 " +
                        "FROM %s WHERE %s IS NOT NULL",
                        wrapIdentifier(columnName),
                        wrapIdentifier(columnName),
                        wrapIdentifier(columnName),
                        wrapIdentifier(tableName),
                        wrapIdentifier(columnName)
                    );
                    
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(percentileSql)) {
                        if (rs.next()) {
                            range.put("p25", rs.getDouble("p25"));
                            range.put("p50", rs.getDouble("p50"));
                            range.put("p75", rs.getDouble("p75"));
                        }
                    }
                } catch (SQLException e) {
                    log.warn("计算分位数失败: table={}, column={}, error={}", 
                            tableName, columnName, e.getMessage());
                }
            } else if (dataType.matches("char.*|varchar.*|text")) {
                // 字符类型
                String sql = String.format(
                    "SELECT MIN(%s) as min_value, MAX(%s) as max_value, " +
                    "MIN(LENGTH(%s)) as min_length, MAX(LENGTH(%s)) as max_length, " +
                    "AVG(LENGTH(%s)) as avg_length " +
                    "FROM %s WHERE %s IS NOT NULL",
                    wrapIdentifier(columnName),
                    wrapIdentifier(columnName),
                    wrapIdentifier(columnName),
                    wrapIdentifier(columnName),
                    wrapIdentifier(columnName),
                    wrapIdentifier(tableName),
                    wrapIdentifier(columnName)
                );
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    if (rs.next()) {
                        range.put("minValue", rs.getString("min_value"));
                        range.put("maxValue", rs.getString("max_value"));
                        range.put("minLength", rs.getInt("min_length"));
                        range.put("maxLength", rs.getInt("max_length"));
                        range.put("avgLength", rs.getDouble("avg_length"));
                    }
                }
            } else if (dataType.matches("date|timestamp.*")) {
                // 日期时间类型
                String sql = String.format(
                    "SELECT MIN(%s) as min_value, MAX(%s) as max_value " +
                    "FROM %s WHERE %s IS NOT NULL",
                    wrapIdentifier(columnName),
                    wrapIdentifier(columnName),
                    wrapIdentifier(tableName),
                    wrapIdentifier(columnName)
                );
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    if (rs.next()) {
                        range.put("minValue", rs.getTimestamp("min_value"));
                        range.put("maxValue", rs.getTimestamp("max_value"));
                        
                        // 计算时间跨度（毫秒）
                        if (rs.getTimestamp("min_value") != null && rs.getTimestamp("max_value") != null) {
                            long span = rs.getTimestamp("max_value").getTime() - rs.getTimestamp("min_value").getTime();
                            range.put("timeSpan", span);
                            range.put("timeSpanDays", span / (1000.0 * 60 * 60 * 24));
                        }
                    }
                }
            }
            
            // 获取空值和非空值数量
            String countSql = String.format(
                "SELECT COUNT(*) as total_count, " +
                "SUM(CASE WHEN %s IS NULL THEN 1 ELSE 0 END) as null_count, " +
                "SUM(CASE WHEN %s IS NOT NULL THEN 1 ELSE 0 END) as not_null_count " +
                "FROM %s",
                wrapIdentifier(columnName),
                wrapIdentifier(columnName),
                wrapIdentifier(tableName)
            );
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(countSql)) {
                if (rs.next()) {
                    range.put("totalCount", rs.getLong("total_count"));
                    range.put("nullCount", rs.getLong("null_count"));
                    range.put("notNullCount", rs.getLong("not_null_count"));
                    
                    // 计算空值率
                    long totalCount = rs.getLong("total_count");
                    if (totalCount > 0) {
                        range.put("nullRate", (double) rs.getLong("null_count") / totalCount);
                    }
                }
            }
            
            // 获取唯一值数量
            String uniqueSql = String.format(
                "SELECT COUNT(DISTINCT %s) as unique_count " +
                "FROM %s WHERE %s IS NOT NULL",
                wrapIdentifier(columnName),
                wrapIdentifier(tableName),
                wrapIdentifier(columnName)
            );
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(uniqueSql)) {
                if (rs.next()) {
                    range.put("uniqueCount", rs.getLong("unique_count"));
                    
                    // 计算唯一率
                    long notNullCount = (long) range.get("notNullCount");
                    if (notNullCount > 0) {
                        range.put("uniqueRate", (double) rs.getLong("unique_count") / notNullCount);
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
    public Map<String, Object> getTableUpdateFrequency(String tableName) throws Exception {
        Map<String, Object> frequency = new HashMap<>();
        try (Connection conn = getConnection()) {
            // 获取表的创建时间和最后更新时间
            Date createTime = getTableCreateTime(tableName);
            Date updateTime = getTableUpdateTime(tableName);
            
            if (createTime != null && updateTime != null) {
                // 计算表存在的天数
                long existDays = (updateTime.getTime() - createTime.getTime()) / (1000 * 60 * 60 * 24);
                if (existDays < 1) existDays = 1; // 避免除零错误
                
                // 获取表的行数
                long rowCount = getTableRowCount(tableName);
                
                // 计算平均每天增长率
                double avgDailyGrowth = (double) rowCount / existDays;
                
                frequency.put("tableName", tableName);
                frequency.put("createTime", createTime);
                frequency.put("lastUpdateTime", updateTime);
                frequency.put("existDays", existDays);
                frequency.put("totalRows", rowCount);
                frequency.put("avgDailyGrowth", avgDailyGrowth);
            }
            
            // 从pg_stat_user_tables获取更多信息
            String sql = "SELECT n_tup_ins, n_tup_upd, n_tup_del, n_live_tup, n_dead_tup, " +
                        "last_vacuum, last_autovacuum, last_analyze, last_autoanalyze " +
                        "FROM pg_stat_user_tables " +
                        "WHERE schemaname = ? AND relname = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, getSchema());
                stmt.setString(2, tableName.toLowerCase());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        // 获取操作统计
                        long insertCount = rs.getLong("n_tup_ins");
                        long updateCount = rs.getLong("n_tup_upd");
                        long deleteCount = rs.getLong("n_tup_del");
                        long liveRows = rs.getLong("n_live_tup");
                        long deadRows = rs.getLong("n_dead_tup");
                        
                        Map<String, Object> operations = new HashMap<>();
                        operations.put("insertCount", insertCount);
                        operations.put("updateCount", updateCount);
                        operations.put("deleteCount", deleteCount);
                        operations.put("totalOperations", insertCount + updateCount + deleteCount);
                        
                        frequency.put("operations", operations);
                        frequency.put("liveRows", liveRows);
                        frequency.put("deadRows", deadRows);
                        
                        // 获取维护操作时间
                        frequency.put("lastVacuum", rs.getTimestamp("last_vacuum"));
                        frequency.put("lastAutoVacuum", rs.getTimestamp("last_autovacuum"));
                        frequency.put("lastAnalyze", rs.getTimestamp("last_analyze"));
                        frequency.put("lastAutoAnalyze", rs.getTimestamp("last_autoanalyze"));
                        
                        // 计算每天平均操作次数
                        if (createTime != null && updateTime != null) {
                            long existDays = (updateTime.getTime() - createTime.getTime()) / (1000 * 60 * 60 * 24);
                            if (existDays < 1) existDays = 1; // 避免除零错误
                            
                            double avgDailyInserts = (double) insertCount / existDays;
                            double avgDailyUpdates = (double) updateCount / existDays;
                            double avgDailyDeletes = (double) deleteCount / existDays;
                            double avgDailyOperations = (double) (insertCount + updateCount + deleteCount) / existDays;
                            
                            Map<String, Object> dailyAvg = new HashMap<>();
                            dailyAvg.put("inserts", avgDailyInserts);
                            dailyAvg.put("updates", avgDailyUpdates);
                            dailyAvg.put("deletes", avgDailyDeletes);
                            dailyAvg.put("total", avgDailyOperations);
                            
                            frequency.put("dailyAverages", dailyAvg);
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
            // 获取表的当前行数
            long currentRowCount = getTableRowCount(tableName);
            
            // 获取表的创建时间和最后更新时间
            Date createTime = getTableCreateTime(tableName);
            Date updateTime = getTableUpdateTime(tableName);
            
            // 尝试从pg_stat_user_tables获取历史数据
            // PostgreSQL没有内置的表增长历史记录，但可以通过扩展如pg_stat_statements或自定义监控表来实现
            // 这里提供一个基于线性估算的实现
            
            if (createTime != null && updateTime != null) {
                // 计算表存在的天数
                long existDays = (updateTime.getTime() - createTime.getTime()) / (1000 * 60 * 60 * 24);
                if (existDays < 1) existDays = 1; // 避免除零错误
                
                // 限制天数不超过表存在的天数
                days = Math.min(days, (int) existDays);
                
                // 假设线性增长，计算每天的增长量
                double dailyGrowth = (double) currentRowCount / existDays;
                
                // 生成过去days天的增长趋势
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(new Date()); // 当前日期
                
                for (int i = 0; i < days; i++) {
                    Map<String, Object> dayTrend = new HashMap<>();
                    dayTrend.put("date", calendar.getTime());
                    
                    // 估算当天的行数（当前行数减去之后天数的增长量）
                    long estimatedRows = Math.max(0, Math.round(currentRowCount - (dailyGrowth * i)));
                    dayTrend.put("rowCount", estimatedRows);
                    
                    trend.add(dayTrend);
                    
                    // 前一天
                    calendar.add(Calendar.DAY_OF_MONTH, -1);
                }
                
                // 反转列表，使其按日期升序排列
                Collections.reverse(trend);
            }
            
            // 如果有自定义的监控表，可以从中获取历史数据
            // 这里假设有一个名为table_growth_history的表记录了表的增长历史
            try {
                String sql = "SELECT record_date, row_count " +
                            "FROM table_growth_history " +
                            "WHERE table_schema = ? AND table_name = ? " +
                            "AND record_date >= CURRENT_DATE - INTERVAL '" + days + " days' " +
                            "ORDER BY record_date";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, getSchema());
                    stmt.setString(2, tableName.toLowerCase());
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        // 如果有历史数据，使用实际记录替换估算数据
                        if (rs.next()) {
                            // 清空之前的估算数据
                            trend.clear();
                            
                            do {
                                Map<String, Object> dayTrend = new HashMap<>();
                                dayTrend.put("date", rs.getDate("record_date"));
                                dayTrend.put("rowCount", rs.getLong("row_count"));
                                trend.add(dayTrend);
                            } while (rs.next());
                        }
                    }
                }
            } catch (SQLException e) {
                // 如果表不存在或查询失败，忽略错误，使用估算数据
                log.debug("获取表增长历史记录失败，使用估算数据: table={}, error={}", tableName, e.getMessage());
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
            // 获取表的所有列
            List<Map<String, Object>> columns = listColumns(tableName);
            List<String> columnNames = columns.stream()
                .map(col -> (String) col.get("name"))
                .collect(Collectors.toList());
            
            // 构建查询SQL
            String columnList = columnNames.stream()
                .map(this::wrapIdentifier)
                .collect(Collectors.joining(", "));
            
            // PostgreSQL支持两种采样方式：LIMIT和TABLESAMPLE
            
            // 方式1：使用LIMIT获取前N行
            String sql = String.format(
                "SELECT %s FROM %s LIMIT %d",
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
            
            // 如果需要随机样本而不是前N行，可以使用TABLESAMPLE
            if (samples.isEmpty()) {
                // 方式2：使用TABLESAMPLE获取随机样本
                // 计算采样百分比，确保不超过100%
                double samplePercent = Math.min(sampleSize * 100.0 / getTableRowCount(tableName), 100.0);
                if (samplePercent < 1.0) samplePercent = 1.0; // 最小采样1%
                
                String randomSql = String.format(
                    "SELECT %s FROM %s TABLESAMPLE SYSTEM(%f) LIMIT %d",
                    columnList,
                    wrapIdentifier(tableName),
                    samplePercent,
                    sampleSize
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
                    // 如果TABLESAMPLE不支持，尝试使用ORDER BY RANDOM()
                    log.debug("TABLESAMPLE不支持，尝试使用ORDER BY RANDOM(): table={}, error={}", 
                             tableName, e.getMessage());
                    
                    String randomOrderSql = String.format(
                        "SELECT %s FROM %s ORDER BY RANDOM() LIMIT %d",
                        columnList,
                        wrapIdentifier(tableName),
                        sampleSize
                    );
                    
                    try (Statement stmt2 = conn.createStatement();
                         ResultSet rs2 = stmt2.executeQuery(randomOrderSql)) {
                        
                        ResultSetMetaData metaData2 = rs2.getMetaData();
                        int columnCount2 = metaData2.getColumnCount();
                        
                        while (rs2.next() && samples.size() < sampleSize) {
                            Map<String, Object> row = new HashMap<>();
                            for (int i = 1; i <= columnCount2; i++) {
                                String colName = metaData2.getColumnName(i);
                                Object value = rs2.getObject(i);
                                row.put(colName, value);
                            }
                            samples.add(row);
                        }
                    } catch (SQLException e2) {
                        log.warn("获取随机样本失败: table={}, error={}", tableName, e2.getMessage());
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
    public Map<String, String> getStoredProcedureDefinitions(String tableName) throws Exception {
        Map<String, String> procedures = new HashMap<>();
        
        try (Connection conn = getConnection()) {
            // 获取当前数据库名
            String dbName = getDatabaseName();
            
            // 查询与表相关的存储过程
            String sql = "SELECT DISTINCT p.proname AS procedure_name, " +
                         "pg_get_functiondef(p.oid) AS procedure_definition " +
                         "FROM pg_proc p " +
                         "JOIN pg_namespace n ON p.pronamespace = n.oid " +
                         "WHERE n.nspname NOT IN ('pg_catalog', 'information_schema') " +
                         "AND pg_get_functiondef(p.oid) LIKE ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, "%" + tableName + "%");
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String procName = rs.getString("procedure_name");
                        String procDef = rs.getString("procedure_definition");
                        procedures.put(procName, procDef);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取存储过程定义失败: table={}, error={}", tableName, e.getMessage(), e);
            throw e;
        }
        
        return procedures;
    }

    public String createSequenceForColumn(String tableName, String columnName) {
        String sequenceName = tableName + "_" + columnName + "_seq";
        StringBuilder sb = new StringBuilder();
        
        // 创建序列
        sb.append("CREATE SEQUENCE IF NOT EXISTS ").append(wrapIdentifier(sequenceName))
          .append(" START WITH 1 INCREMENT BY 1");
        
        return sb.toString();
    }

    public String setColumnDefaultToNextval(String tableName, String columnName) {
        String sequenceName = tableName + "_" + columnName + "_seq";
        StringBuilder sb = new StringBuilder();
        
        // 设置列默认值为序列的下一个值
        sb.append("ALTER TABLE ").append(wrapIdentifier(tableName))
          .append(" ALTER COLUMN ").append(wrapIdentifier(columnName))
          .append(" SET DEFAULT nextval('").append(sequenceName).append("')");
        
        return sb.toString();
    }

    // 日期函数处理
    public String getCurrentTimestampSql() {
        return "CURRENT_TIMESTAMP";
    }

    public String getCurrentDateSql() {
        return "CURRENT_DATE";
    }

    public String getCurrentTimeSql() {
        return "CURRENT_TIME";
    }

    // 日期加减操作
    public String getDateAddSql(String dateColumn, int days) {
        return dateColumn + " + INTERVAL '" + days + " days'";
    }

    public String getDateSubSql(String dateColumn, int days) {
        return dateColumn + " - INTERVAL '" + days + " days'";
    }

    // 日期差值计算
    public String getDateDiffSql(String endDate, String startDate) {
        return "EXTRACT(DAY FROM (" + endDate + " - " + startDate + "))";
    }

    // NULLIF和COALESCE函数
    public String getNullIfSql(String expr1, String expr2) {
        return "NULLIF(" + expr1 + ", " + expr2 + ")";
    }

    public String getCoalesceSql(String... exprs) {
        return "COALESCE(" + String.join(", ", exprs) + ")";
    }

    // 添加主键方法
    public String getAddPrimaryKeySql(String tableName, List<String> columnNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("ALTER TABLE ").append(wrapIdentifier(tableName));
        
        // 生成主键名称
        String pkName = tableName + "_pkey";
        
        sb.append(" ADD CONSTRAINT ").append(wrapIdentifier(pkName))
          .append(" PRIMARY KEY (");
        
        List<String> wrappedColumns = columnNames.stream()
                .map(this::wrapIdentifier)
                .collect(Collectors.toList());
        
        sb.append(String.join(", ", wrappedColumns));
        sb.append(")");
        
        return sb.toString();
    }

    // 删除主键方法
    public String getDropPrimaryKeySql(String tableName) {
        // 获取主键约束名称
        String constraintQuery = "SELECT constraint_name FROM information_schema.table_constraints " +
                                "WHERE table_schema = '" + getSchema() + "' " +
                                "AND table_name = '" + tableName + "' " +
                                "AND constraint_type = 'PRIMARY KEY'";
        
        // 使用具体的主键名称
        return "ALTER TABLE " + wrapIdentifier(tableName) + 
               " DROP CONSTRAINT IF EXISTS " + wrapIdentifier(tableName + "_pkey");
    }

    public String getAddForeignKeySql(String tableName, String keyName, String columnName, 
                                     String refTableName, String refColumnName) {
        if (keyName == null || keyName.isEmpty()) {
            // 生成一个默认的外键名称
            keyName = tableName + "_" + columnName + "_fkey";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("ALTER TABLE ").append(wrapIdentifier(tableName))
          .append(" ADD CONSTRAINT ").append(wrapIdentifier(keyName))
          .append(" FOREIGN KEY (").append(wrapIdentifier(columnName)).append(")")
          .append(" REFERENCES ").append(wrapIdentifier(refTableName))
          .append(" (").append(wrapIdentifier(refColumnName)).append(")");
        
        return sb.toString();
    }

    public String getDropForeignKeySql(String tableName, String keyName) {
        return "ALTER TABLE " + wrapIdentifier(tableName) + 
               " DROP CONSTRAINT IF EXISTS " + wrapIdentifier(keyName);
    }

    // JSON操作符
    public String getJsonExtractPathSql(String jsonColumn, String path) {
        // 使用 -> 操作符提取JSON对象中的键的值
        return jsonColumn + " -> '" + path + "'";
    }

    public String getJsonExtractPathTextSql(String jsonColumn, String path) {
        // 使用 ->> 操作符提取JSON对象中的键的值为文本
        return jsonColumn + " ->> '" + path + "'";
    }

    public String getJsonbContainsSql(String jsonbColumn, String jsonbValue) {
        // 使用 @> 操作符检查JSONB是否包含另一个JSONB
        return jsonbColumn + " @> '" + jsonbValue + "'::jsonb";
    }

    // ARRAY类型操作
    public String getArrayContainsSql(String arrayColumn, String value) {
        // 使用 @> 操作符检查数组是否包含值
        return arrayColumn + " @> ARRAY[" + value + "]";
    }

    public String getArrayOverlapsSql(String arrayColumn1, String arrayColumn2) {
        // 使用 && 操作符检查两个数组是否有共同元素
        return arrayColumn1 + " && " + arrayColumn2;
    }

    // VACUUM和ANALYZE操作
    public String getVacuumTableSql(String tableName) {
        return "VACUUM " + wrapIdentifier(tableName);
    }

    public String getAnalyzeTableSql(String tableName) {
        return "ANALYZE " + wrapIdentifier(tableName);
    }

    public Map<String, Object> analyzeTable(String tableName) throws Exception {
        String sql = getAnalyzeTableSql(tableName);
        executeUpdate(sql);
        
        // 获取表统计信息
        sql = "SELECT reltuples as rows, relpages as pages, pg_size_pretty(pg_relation_size('" + 
              tableName + "')) as size FROM pg_class WHERE relname = '" + tableName + "'";
        
        List<Map<String, Object>> result = executeQuery(sql);
        if (result.isEmpty()) {
            return new HashMap<>();
        }
        
        return result.get(0);
    }

    // 添加PostgreSQL特有的字符串和数值函数处理
    public String getStringConcatSql(String... strings) {
        return String.join(" || ", strings);
    }

    public String getSubstringSql(String string, int start, Integer length) {
        if (length != null) {
            return "substring(" + string + " from " + start + " for " + length + ")";
        } else {
            return "substring(" + string + " from " + start + ")";
        }
    }

    public String getCastSql(String expression, String targetType) {
        return expression + "::" + targetType;
    }

    // 对PostgreSQL的正则表达式支持
    public String getRegexpMatchSql(String string, String pattern) {
        return string + " ~ " + wrapValue(pattern);
    }

    public String getRegexpReplaceSql(String string, String pattern, String replacement) {
        return "regexp_replace(" + string + ", " + wrapValue(pattern) + ", " + wrapValue(replacement) + ")";
    }

    // 添加设置列存储参数的方法
    public String getSetColumnStorageSql(String tableName, String columnName, String storageType) {
        // storageType可以是PLAIN, EXTERNAL, EXTENDED, MAIN
        return "ALTER TABLE " + wrapIdentifier(tableName) + 
               " ALTER COLUMN " + wrapIdentifier(columnName) + 
               " SET STORAGE " + storageType;
    }

    // 添加设置列统计目标的方法
    public String getSetColumnStatisticsSql(String tableName, String columnName, int target) {
        return "ALTER TABLE " + wrapIdentifier(tableName) + 
               " ALTER COLUMN " + wrapIdentifier(columnName) + 
               " SET STATISTICS " + target;
    }

    // 添加创建继承表的方法
    public String getCreateInheritedTableSql(String tableName, String parentTable, 
        List<ColumnDefinition> columns, String tableComment) {
        
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(wrapIdentifier(tableName))
          .append(" (\n");
        
        List<String> columnDefinitions = new ArrayList<>();
        for (ColumnDefinition column : columns) {
            adjustPostgreSQLColumnType(column);
            columnDefinitions.add(formatFieldDefinition(
                column.getName(),
                column.getType(),
                column.getLength(),
                column.getPrecision(),
                column.getScale(),
                column.isNullable(),
                column.getDefaultValue(),
                column.getComment()
            ));
        }
        
        sb.append(String.join(",\n", columnDefinitions))
          .append("\n) INHERITS (").append(wrapIdentifier(parentTable)).append(")");
        
        // 添加表注释
        if (tableComment != null && !tableComment.isEmpty()) {
            sb.append(";\nCOMMENT ON TABLE ").append(wrapIdentifier(tableName))
              .append(" IS ").append(wrapValue(tableComment));
        }
        
        return sb.toString();
    }

    // PostgreSQL 10+分区表支持
    public String getCreatePartitionedTableSql(String tableName, List<ColumnDefinition> columns, 
        String partitionKeyColumn, String partitionType, String tableComment) {
        
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(wrapIdentifier(tableName))
          .append(" (\n");
        
        List<String> columnDefinitions = new ArrayList<>();
        List<String> primaryKeys = new ArrayList<>();
        
        for (ColumnDefinition column : columns) {
            adjustPostgreSQLColumnType(column);
            columnDefinitions.add(formatFieldDefinition(
                column.getName(),
                column.getType(),
                column.getLength(),
                column.getPrecision(),
                column.getScale(),
                column.isNullable(),
                column.getDefaultValue(),
                column.getComment()
            ));
            
            if (column.isPrimaryKey()) {
                primaryKeys.add(column.getName());
            }
        }
        
        sb.append(String.join(",\n", columnDefinitions));
        
        // 添加主键定义
        if (!primaryKeys.isEmpty()) {
            sb.append(",\n");
            sb.append("PRIMARY KEY (").append(primaryKeys.stream()
                    .map(this::wrapIdentifier)
                    .collect(Collectors.joining(", ")))
               .append(")");
        }
        
        sb.append("\n) PARTITION BY ").append(partitionType)
          .append(" (").append(wrapIdentifier(partitionKeyColumn)).append(")");
        
        // 添加表注释
        if (tableComment != null && !tableComment.isEmpty()) {
            sb.append(";\nCOMMENT ON TABLE ").append(wrapIdentifier(tableName))
              .append(" IS ").append(wrapValue(tableComment));
        }
        
        return sb.toString();
    }

    // 添加分区
    public String getAddPartitionSql(String tableName, String partitionName, 
        String fromValue, String toValue) {
        
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(wrapIdentifier(partitionName))
          .append(" PARTITION OF ").append(wrapIdentifier(tableName))
          .append(" FOR VALUES FROM (").append(fromValue)
          .append(") TO (").append(toValue).append(")");
        
        return sb.toString();
    }

    // 添加部分索引支持
    public String getAddPartialIndexSql(String tableName, String indexName, 
        List<String> columns, String whereClause) {
        
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE INDEX ");
        
        if (indexName == null || indexName.isEmpty()) {
            indexName = tableName + "_" + String.join("_", columns) + "_idx";
        }
        
        sb.append(wrapIdentifier(indexName))
          .append(" ON ")
          .append(wrapIdentifier(tableName))
          .append(" (");
        
        List<String> wrappedColumns = columns.stream()
                .map(this::wrapIdentifier)
                .collect(Collectors.toList());
        
        sb.append(String.join(", ", wrappedColumns))
          .append(") WHERE ").append(whereClause);
        
        return sb.toString();
    }

    // 添加表达式索引支持
    public String getAddExpressionIndexSql(String tableName, String indexName, String expression) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE INDEX ");
        
        if (indexName == null || indexName.isEmpty()) {
            indexName = tableName + "_expr_idx";
        }
        
        sb.append(wrapIdentifier(indexName))
          .append(" ON ")
          .append(wrapIdentifier(tableName))
          .append(" (").append(expression).append(")");
        
        return sb.toString();
    }

    // 添加对GIN/GiST等特殊索引类型的支持
    public String getAddSpecialIndexSql(String tableName, String indexName, 
        List<String> columns, String indexType) {
        
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE INDEX ");
        
        if (indexName == null || indexName.isEmpty()) {
            indexName = tableName + "_" + String.join("_", columns) + "_" + indexType.toLowerCase() + "_idx";
        }
        
        sb.append(wrapIdentifier(indexName))
          .append(" ON ")
          .append(wrapIdentifier(tableName))
          .append(" USING ").append(indexType)
          .append(" (");
        
        List<String> wrappedColumns = columns.stream()
                .map(this::wrapIdentifier)
                .collect(Collectors.toList());
        
        sb.append(String.join(", ", wrappedColumns))
          .append(")");
        
        return sb.toString();
    }

    // 添加CHECK约束支持
    public String getAddCheckConstraintSql(String tableName, String constraintName, String expression) {
        if (constraintName == null || constraintName.isEmpty()) {
            constraintName = tableName + "_check";
        }
        
        return "ALTER TABLE " + wrapIdentifier(tableName) + 
               " ADD CONSTRAINT " + wrapIdentifier(constraintName) + 
               " CHECK (" + expression + ")";
    }

    // 添加UNIQUE约束支持
    public String getAddUniqueConstraintSql(String tableName, String constraintName, List<String> columns) {
        if (constraintName == null || constraintName.isEmpty()) {
            constraintName = tableName + "_" + String.join("_", columns) + "_unique";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("ALTER TABLE ").append(wrapIdentifier(tableName))
          .append(" ADD CONSTRAINT ").append(wrapIdentifier(constraintName))
          .append(" UNIQUE (");
        
        List<String> wrappedColumns = columns.stream()
                .map(this::wrapIdentifier)
                .collect(Collectors.toList());
        
        sb.append(String.join(", ", wrappedColumns))
          .append(")");
        
        return sb.toString();
    }

    // 添加删除约束的支持
    public String getDropConstraintSql(String tableName, String constraintName) {
        return "ALTER TABLE " + wrapIdentifier(tableName) + 
               " DROP CONSTRAINT IF EXISTS " + wrapIdentifier(constraintName);
    }

    // 检查扩展是否安装的方法
    public boolean isExtensionInstalled(String extensionName) throws Exception {
        String sql = "SELECT COUNT(*) FROM pg_extension WHERE extname = '" + extensionName + "'";
        List<Map<String, Object>> result = executeQuery(sql);
        if (!result.isEmpty()) {
            Object count = result.get(0).get("count");
            return count != null && !count.equals(0L) && !count.equals(0);
        }
        return false;
    }

    // 创建扩展的SQL
    public String getCreateExtensionSql(String extensionName) {
        return "CREATE EXTENSION IF NOT EXISTS " + extensionName;
    }

    // 卸载扩展的SQL
    public String getDropExtensionSql(String extensionName) {
        return "DROP EXTENSION IF EXISTS " + extensionName;
    }

    // 改进扩展管理，支持更多选项
    public String getCreateExtensionSql(String extensionName, String schema, String version) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE EXTENSION IF NOT EXISTS ").append(extensionName);
        
        if (schema != null && !schema.isEmpty()) {
            sb.append(" SCHEMA ").append(wrapIdentifier(schema));
        }
        
        if (version != null && !version.isEmpty()) {
            sb.append(" VERSION ").append(version);
        }
        
        return sb.toString();
    }

    // 更新扩展版本
    public String getAlterExtensionUpdateSql(String extensionName, String version) {
        return "ALTER EXTENSION " + extensionName + " UPDATE" + 
               (version != null && !version.isEmpty() ? " TO " + version : "");
    }

    // 扩展架构迁移
    public String getAlterExtensionSchemaSql(String extensionName, String newSchema) {
        return "ALTER EXTENSION " + extensionName + " SET SCHEMA " + wrapIdentifier(newSchema);
    }

    // 发送通知
    public String getNotifySql(String channel, String payload) {
        return "NOTIFY " + wrapIdentifier(channel) + 
               (payload != null && !payload.isEmpty() ? ", " + wrapValue(payload) : "");
    }

    // 监听频道
    public String getListenSql(String channel) {
        return "LISTEN " + wrapIdentifier(channel);
    }

    // 停止监听
    public String getUnlistenSql(String channel) {
        return channel != null && !channel.isEmpty() ? 
               "UNLISTEN " + wrapIdentifier(channel) : "UNLISTEN *";
    }

    // 创建物化视图
    public String getCreateMaterializedViewSql(String viewName, String sql, boolean withData) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE MATERIALIZED VIEW ").append(wrapIdentifier(viewName))
          .append(" AS ").append(sql);
        
        if (!withData) {
            sb.append(" WITH NO DATA");
        }
        
        return sb.toString();
    }

    // 刷新物化视图
    public String getRefreshMaterializedViewSql(String viewName, boolean concurrently, boolean withData) {
        StringBuilder sb = new StringBuilder();
        sb.append("REFRESH MATERIALIZED VIEW ");
        
        if (concurrently) {
            sb.append("CONCURRENTLY ");
        }
        
        sb.append(wrapIdentifier(viewName));
        
        if (!withData) {
            sb.append(" WITH NO DATA");
        }
        
        return sb.toString();
    }

    // 删除物化视图
    public String getDropMaterializedViewSql(String viewName) {
        return "DROP MATERIALIZED VIEW IF EXISTS " + wrapIdentifier(viewName);
    }

    // 创建视图
    public String getCreateViewSql(String viewName, String sql) {
        return "CREATE OR REPLACE VIEW " + wrapIdentifier(viewName) + " AS " + sql;
    }

    // 创建函数
    public String getCreateFunctionSql(String functionName, List<String> parameterNames, 
        List<String> parameterTypes, String returnType, String functionBody, String language) {
        
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE OR REPLACE FUNCTION ")
          .append(wrapIdentifier(functionName)).append("(");
        
        List<String> parameters = new ArrayList<>();
        for (int i = 0; i < parameterNames.size(); i++) {
            parameters.add(parameterNames.get(i) + " " + parameterTypes.get(i));
        }
        
        sb.append(String.join(", ", parameters))
          .append(") RETURNS ").append(returnType)
          .append(" AS $$").append(functionBody).append("$$ ")
          .append("LANGUAGE ").append(language);
        
        return sb.toString();
    }

    // 创建触发器
    public String getCreateTriggerSql(String triggerName, String tableName, 
        String timing, String event, String functionName) {
        
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TRIGGER ").append(wrapIdentifier(triggerName))
          .append(" ").append(timing).append(" ").append(event)
          .append(" ON ").append(wrapIdentifier(tableName))
          .append(" FOR EACH ROW EXECUTE FUNCTION ")
          .append(wrapIdentifier(functionName)).append("()");
        
        return sb.toString();
    }

    // 删除触发器
    public String getDropTriggerSql(String triggerName, String tableName) {
        return "DROP TRIGGER IF EXISTS " + wrapIdentifier(triggerName) + 
               " ON " + wrapIdentifier(tableName);
    }

    // 创建规则
    public String getCreateRuleSql(String ruleName, String tableName, 
        String event, String condition, String command) {
        
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE OR REPLACE RULE ").append(wrapIdentifier(ruleName))
          .append(" AS ON ").append(event).append(" TO ")
          .append(wrapIdentifier(tableName));
        
        if (condition != null && !condition.isEmpty()) {
            sb.append(" WHERE ").append(condition);
        }
        
        sb.append(" DO ").append(command);
        
        return sb.toString();
    }

    // 创建行级安全策略
    public String getCreatePolicySql(String policyName, String tableName, 
        String command, String using, String check) {
        
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE POLICY ").append(wrapIdentifier(policyName))
          .append(" ON ").append(wrapIdentifier(tableName))
          .append(" FOR ").append(command);
        
        if (using != null && !using.isEmpty()) {
            sb.append(" USING (").append(using).append(")");
        }
        
        if (check != null && !check.isEmpty()) {
            sb.append(" WITH CHECK (").append(check).append(")");
        }
        
        return sb.toString();
    }

    // 启用行级安全
    public String getEnableRowSecuritySql(String tableName) {
        return "ALTER TABLE " + wrapIdentifier(tableName) + " ENABLE ROW LEVEL SECURITY";
    }

    // 创建枚举类型
    public String getCreateEnumTypeSql(String typeName, List<String> values) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TYPE ").append(wrapIdentifier(typeName))
          .append(" AS ENUM (");
        
        List<String> wrappedValues = values.stream()
                .map(this::wrapValue)
                .collect(Collectors.toList());
        
        sb.append(String.join(", ", wrappedValues))
          .append(")");
        
        return sb.toString();
    }

    // 创建域类型
    public String getCreateDomainTypeSql(String typeName, String baseType, 
        String constraint, String defaultValue) {
        
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE DOMAIN ").append(wrapIdentifier(typeName))
          .append(" AS ").append(baseType);
        
        if (defaultValue != null && !defaultValue.isEmpty()) {
            sb.append(" DEFAULT ").append(defaultValue);
        }
        
        if (constraint != null && !constraint.isEmpty()) {
            sb.append(" CHECK (").append(constraint).append(")");
        }
        
        return sb.toString();
    }

    // 创建复合类型
    public String getCreateCompositeTypeSql(String typeName, 
        Map<String, String> fieldDefinitions) {
        
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TYPE ").append(wrapIdentifier(typeName))
          .append(" AS (");
        
        List<String> fields = new ArrayList<>();
        for (Map.Entry<String, String> entry : fieldDefinitions.entrySet()) {
            fields.add(wrapIdentifier(entry.getKey()) + " " + entry.getValue());
        }
        
        sb.append(String.join(", ", fields))
          .append(")");
        
        return sb.toString();
    }

    // 全文搜索向量生成
    public String getToTsVectorSql(String config, String text) {
        return "to_tsvector(" + 
               (config != null && !config.isEmpty() ? wrapValue(config) + ", " : "") + 
               text + ")";
    }

    // 全文搜索查询
    public String getToTsQuerySql(String config, String query) {
        return "to_tsquery(" + 
               (config != null && !config.isEmpty() ? wrapValue(config) + ", " : "") + 
               wrapValue(query) + ")";
    }

    // 全文搜索匹配
    public String getTsMatchSql(String vector, String query) {
        return vector + " @@ " + query;
    }

    // 全文搜索排序
    public String getTsRankSql(String vector, String query, boolean normalize) {
        return (normalize ? "ts_rank_cd(" : "ts_rank(") + 
               vector + ", " + query + ")";
    }

    // 生成递归查询模板
    public String getRecursiveQueryTemplateSql(String tableName, String idColumn, 
        String parentIdColumn, String startCondition) {
        
        return "WITH RECURSIVE tree AS (\n" +
               "    SELECT * FROM " + wrapIdentifier(tableName) + " WHERE " + startCondition + "\n" +
               "    UNION ALL\n" +
               "    SELECT t.* FROM " + wrapIdentifier(tableName) + " t\n" +
               "    JOIN tree tr ON t." + wrapIdentifier(parentIdColumn) + " = tr." + wrapIdentifier(idColumn) + "\n" +
               ")\n" +
               "SELECT * FROM tree";
    }

    // 创建LIST分区表
    public String getCreateListPartitionedTableSql(String tableName, List<ColumnDefinition> columns, 
        String partitionKeyColumn, String tableComment) {
        
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(wrapIdentifier(tableName))
          .append(" (\n");
        
        List<String> columnDefinitions = new ArrayList<>();
        List<String> primaryKeys = new ArrayList<>();
        
        for (ColumnDefinition column : columns) {
            adjustPostgreSQLColumnType(column);
            columnDefinitions.add(formatFieldDefinition(
                column.getName(),
                column.getType(),
                column.getLength(),
                column.getPrecision(),
                column.getScale(),
                column.isNullable(),
                column.getDefaultValue(),
                column.getComment()
            ));
            
            if (column.isPrimaryKey()) {
                primaryKeys.add(column.getName());
            }
        }
        
        sb.append(String.join(",\n", columnDefinitions));
        
        // 添加主键定义
        if (!primaryKeys.isEmpty()) {
            sb.append(",\n");
            sb.append("PRIMARY KEY (").append(primaryKeys.stream()
                    .map(this::wrapIdentifier)
                    .collect(Collectors.joining(", ")))
               .append(")");
        }
        
        sb.append("\n) PARTITION BY LIST (").append(wrapIdentifier(partitionKeyColumn)).append(")");
        
        // 添加表注释
        if (tableComment != null && !tableComment.isEmpty()) {
            sb.append(";\nCOMMENT ON TABLE ").append(wrapIdentifier(tableName))
              .append(" IS ").append(wrapValue(tableComment));
        }
        
        return sb.toString();
    }

    // 添加LIST分区
    public String getAddListPartitionSql(String tableName, String partitionName, List<String> values) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(wrapIdentifier(partitionName))
          .append(" PARTITION OF ").append(wrapIdentifier(tableName))
          .append(" FOR VALUES IN (");
        
        List<String> formattedValues = values.stream()
                .map(this::wrapValue)
                .collect(Collectors.toList());
        
        sb.append(String.join(", ", formattedValues))
          .append(")");
        
        return sb.toString();
    }

    // 创建默认分区
    public String getAddDefaultPartitionSql(String tableName, String partitionName) {
        return "CREATE TABLE " + wrapIdentifier(partitionName) + 
               " PARTITION OF " + wrapIdentifier(tableName) + 
               " DEFAULT";
    }

    // 分离分区
    public String getDetachPartitionSql(String tableName, String partitionName) {
        return "ALTER TABLE " + wrapIdentifier(tableName) + 
               " DETACH PARTITION " + wrapIdentifier(partitionName);
    }

    // 创建预备语句
    public String getPrepareSql(String name, String sql) {
        return "PREPARE " + name + " AS " + sql;
    }

    // 执行预备语句
    public String getExecuteSql(String name, List<String> parameters) {
        StringBuilder sb = new StringBuilder();
        sb.append("EXECUTE ").append(name);
        
        if (parameters != null && !parameters.isEmpty()) {
            sb.append("(").append(String.join(", ", parameters)).append(")");
        }
        
        return sb.toString();
    }

    // 释放预备语句
    public String getDeallocateSql(String name) {
        return "DEALLOCATE " + name;
    }

    // 声明游标
    public String getDeclareCursorSql(String cursorName, String sql, boolean scrollable, boolean withHold) {
        StringBuilder sb = new StringBuilder();
        sb.append("DECLARE ").append(cursorName).append(" ");
        
        if (scrollable) {
            sb.append("SCROLL ");
        }
        
        sb.append("CURSOR ");
        
        if (withHold) {
            sb.append("WITH HOLD ");
        }
        
        sb.append("FOR ").append(sql);
        
        return sb.toString();
    }

    // 获取游标数据
    public String getFetchCursorSql(String cursorName, int count, String direction) {
        StringBuilder sb = new StringBuilder();
        sb.append("FETCH ");
        
        if (direction != null && !direction.isEmpty()) {
            sb.append(direction).append(" ");
        }
        
        if (count > 0) {
            sb.append(count).append(" ");
        }
        
        sb.append("FROM ").append(cursorName);
        
        return sb.toString();
    }

    // 关闭游标
    public String getCloseCursorSql(String cursorName) {
        return "CLOSE " + cursorName;
    }

    // 添加 executeQuery 方法
    /**
     * 执行查询SQL语句并返回结果集
     * 
     * @param sql 要执行的SQL查询
     * @return 查询结果列表
     * @throws Exception 如果查询执行出错
     */
    @Override
    public List<Map<String, Object>> executeQuery(String sql) throws Exception {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        List<Map<String, Object>> results = new ArrayList<>();
        
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
            return results;
        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) { /* 忽略关闭异常 */ }
            }
            if (stmt != null) {
                try { stmt.close(); } catch (SQLException e) { /* 忽略关闭异常 */ }
            }
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { /* 忽略关闭异常 */ }
            }
        }
    }

    // 添加 executeUpdate 方法
    /**
     * 执行更新SQL语句并返回受影响的行数
     * 
     * @param sql 要执行的SQL更新语句
     * @return 受影响的行数
     * @throws Exception 如果更新执行出错
     */
    @Override
    public int executeUpdate(String sql) throws Exception {
        Connection conn = null;
        Statement stmt = null;
        
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            return stmt.executeUpdate(sql);
        } finally {
            if (stmt != null) {
                try { stmt.close(); } catch (SQLException e) { /* 忽略关闭异常 */ }
            }
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { /* 忽略关闭异常 */ }
            }
        }
    }

    /**
     * 生成PostgreSQL的LIMIT子句
     * 
     * @param offset 起始位置
     * @param limit  返回记录的最大数量
     * @return 格式化后的LIMIT子句
     */
    @Override
    public String getLimitClause(long offset, int limit) {
        return "LIMIT " + limit + " OFFSET " + offset;
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
        
        // PostgreSQL 10+支持声明式分区
        String partitionListSql = 
                "SELECT nmsp_child.nspname AS child_schema, " +
                "child.relname AS child_table, " +
                "pg_get_expr(child.relpartbound, child.oid) AS partition_expr " +
                "FROM pg_inherits " +
                "JOIN pg_class parent ON pg_inherits.inhparent = parent.oid " +
                "JOIN pg_class child ON pg_inherits.inhrelid = child.oid " +
                "JOIN pg_namespace nmsp_parent ON nmsp_parent.oid = parent.relnamespace " +
                "JOIN pg_namespace nmsp_child ON nmsp_child.oid = child.relnamespace " +
                "WHERE parent.relname = ? " +
                "ORDER BY child.relname";
                
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(partitionListSql)) {
            stmt.setString(1, tableName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                boolean hasPartitions = false;
                while (rs.next()) {
                    hasPartitions = true;
                    String partExpr = rs.getString("partition_expr");
                    // PostgreSQL分区表达式类似于 FOR VALUES FROM ('2021-01-01') TO ('2021-02-01')
                    // 提取FROM和TO之间的值
                    if (partExpr != null) {
                        // 简化的解析逻辑，提取单引号之间的值
                        Pattern pattern = Pattern.compile("'(.*?)'");
                        Matcher matcher = pattern.matcher(partExpr);
                        while (matcher.find()) {
                            partitions.add(matcher.group(1));
                        }
                    }
                }
                
                // 如果没有找到分区，使用通用方法查询
                if (!hasPartitions) {
                    // 使用SQL查询分区列表（基于指定的分区字段查询唯一值）
                    String sql = String.format("SELECT DISTINCT %s FROM %s ORDER BY %s", 
                            wrapIdentifier(partitionField), 
                            wrapIdentifier(tableName),
                            wrapIdentifier(partitionField));
                    
                    try (Statement genericStmt = conn.createStatement();
                         ResultSet genericRs = genericStmt.executeQuery(sql)) {
                        
                        while (genericRs.next()) {
                            Object value = genericRs.getObject(1);
                            if (value != null) {
                                partitions.add(value.toString());
                            }
                        }
                    }
                }
            }
        }
        
        return partitions;
    }
    
    /**
     * 获取创建临时表的SQL - PostgreSQL数据库实现
     *
     * @param tempTableName 临时表名称
     * @param sourceTableName 源表名称（用于复制结构）
     * @param preserveRows 是否在会话结束后保留数据（PostgreSQL中可以通过ON COMMIT设置）
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
        // PostgreSQL临时表语法
        sql.append("CREATE TEMPORARY TABLE ").append(wrapIdentifier(tempTableName)).append(" (");
        
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
        
        // 设置会话结束时的行为
        if (preserveRows) {
            sql.append(" ON COMMIT PRESERVE ROWS");
        } else {
            sql.append(" ON COMMIT DELETE ROWS");
        }
        
        return sql.toString();
    }
}