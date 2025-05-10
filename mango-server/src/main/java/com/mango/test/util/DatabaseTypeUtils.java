package com.mango.test.util;

import com.mango.test.constant.DatabaseTypes;
import java.util.*;

public class DatabaseTypeUtils {

    /**
     * 获取默认端口号
     */
    public static String getDefaultPort(String dbType) {
        dbType = dbType.toLowerCase();
        switch (dbType) {
            case DatabaseTypes.MYSQL:
            case DatabaseTypes.MARIADB:
                return "3306";
            case DatabaseTypes.ORACLE:
                return "1521";
            case DatabaseTypes.POSTGRESQL:
                return "5432";
            case DatabaseTypes.SQLSERVER:
                return "1433";
            case DatabaseTypes.DB2:
                return "50000";
            case DatabaseTypes.SYBASE:
                return "5000";
            case DatabaseTypes.H2:
                return "9092";
            case DatabaseTypes.DM7:
            case DatabaseTypes.DM8:
                return "5236";
            case DatabaseTypes.GAUSSDB:
            case DatabaseTypes.OPENGAUSS:
                return "5432";
            case DatabaseTypes.KINGBASE:
                return "54321";
            case DatabaseTypes.SHENTONG:
                return "2003";
            case DatabaseTypes.HIGHGO:
                return "5866";
            case DatabaseTypes.GBASE:
                return "5258";
            case DatabaseTypes.UXDB:
                return "5432";
            case DatabaseTypes.HYDB:
                return "1521";
            case DatabaseTypes.XUGU:
                return "5138";
            case DatabaseTypes.NEUDB:
                return "1621";
            case DatabaseTypes.SEQUOIADB:
                return "11810";
            case DatabaseTypes.TBASE:
                return "5432";
            case DatabaseTypes.HIVE:
                return "10000";
            case DatabaseTypes.HBASE:
                return "2181";
            case DatabaseTypes.CLICKHOUSE:
                return "8123";
            case DatabaseTypes.DORIS:
            case DatabaseTypes.STARROCKS:
                return "9030";
            case DatabaseTypes.SPARKSQL:
                return "10016";
            case DatabaseTypes.PRESTO:
            case DatabaseTypes.TRINO:
                return "8080";
            case DatabaseTypes.IMPALA:
                return "21050";
            case DatabaseTypes.KYLIN:
                return "7070";
            case DatabaseTypes.TIDB:
                return "4000";
            case DatabaseTypes.OCEANBASE:
                return "2881";
            default:
                return "0";
        }
    }

    /**
     * 获取JDBC驱动类名
     */
    public static String getDriverClassName(String dbType) {
        dbType = dbType.toLowerCase();
        switch (dbType) {
            case DatabaseTypes.MYSQL:
            case DatabaseTypes.MARIADB:
            case DatabaseTypes.TIDB:
                return "com.mysql.cj.jdbc.Driver";
            case DatabaseTypes.ORACLE:
                return "oracle.jdbc.OracleDriver";
            case DatabaseTypes.POSTGRESQL:
                return "org.postgresql.Driver";
            case DatabaseTypes.SQLSERVER:
                return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            case DatabaseTypes.DB2:
                return "com.ibm.db2.jcc.DB2Driver";
            case DatabaseTypes.SYBASE:
                return "com.sybase.jdbc4.jdbc.SybDriver";
            case DatabaseTypes.H2:
                return "org.h2.Driver";
            case DatabaseTypes.DM7:
            case DatabaseTypes.DM8:
                return "dm.jdbc.driver.DmDriver";
            case DatabaseTypes.GAUSSDB:
                return "com.huawei.gauss.jdbc.ZenithDriver";
            case DatabaseTypes.OPENGAUSS:
                return "org.opengauss.Driver";
            // ... 其他数据库的驱动类名
            default:
                throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
    }

    /**
     * 获取JDBC URL模板
     */
    public static String getUrlTemplate(String dbType) {
        dbType = dbType.toLowerCase();
        switch (dbType) {
            case DatabaseTypes.MYSQL:
            case DatabaseTypes.MARIADB:
            case DatabaseTypes.TIDB:
                return "jdbc:mysql://%s:%s/%s?useUnicode=true&characterEncoding=utf8";
            case DatabaseTypes.ORACLE:
                return "jdbc:oracle:thin:@%s:%s:%s";
            case DatabaseTypes.POSTGRESQL:
                return "jdbc:postgresql://%s:%s/%s";
            case DatabaseTypes.SQLSERVER:
                return "jdbc:sqlserver://%s:%s;DatabaseName=%s";
            case DatabaseTypes.DB2:
                return "jdbc:db2://%s:%s/%s";
            // ... 其他数据库的URL模板
            default:
                throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
    }

    /**
     * 判断数据库是否支持事务
     */
    public static boolean supportsTransactions(String dbType) {
        dbType = dbType.toLowerCase();
        // 大多数关系型数据库都支持事务
        if (DatabaseTypes.isRelationalDatabase(dbType)) {
            return true;
        }
        // 部分大数据数据库支持事务
        if (dbType.equals(DatabaseTypes.SPARKSQL)) {
            return true;
        }
        return false;
    }

    /**
     * 判断数据库是否支持批量更新
     */
    public static boolean supportsBatchUpdates(String dbType) {
        dbType = dbType.toLowerCase();
        // 大多数关系型数据库都支持批量更新
        if (DatabaseTypes.isRelationalDatabase(dbType)) {
            return true;
        }
        // 部分大数据数据库支持批量更新
        return dbType.equals(DatabaseTypes.SPARKSQL) || 
               dbType.equals(DatabaseTypes.CLICKHOUSE) ||
               dbType.equals(DatabaseTypes.HBASE);
    }

    /**
     * 获取数据库默认字符集
     */
    public static String getDefaultCharset(String dbType) {
        dbType = dbType.toLowerCase();
        switch (dbType) {
            case DatabaseTypes.MYSQL:
            case DatabaseTypes.MARIADB:
            case DatabaseTypes.TIDB:
                return "utf8mb4";
            case DatabaseTypes.ORACLE:
            case DatabaseTypes.DM7:
            case DatabaseTypes.DM8:
                return "AL32UTF8";
            case DatabaseTypes.POSTGRESQL:
            case DatabaseTypes.GAUSSDB:
            case DatabaseTypes.OPENGAUSS:
            case DatabaseTypes.KINGBASE:
                return "UTF8";
            default:
                return "UTF-8";
        }
    }

    /**
     * 获取数据库默认排序规则
     */
    public static String getDefaultCollation(String dbType) {
        dbType = dbType.toLowerCase();
        switch (dbType) {
            case DatabaseTypes.MYSQL:
            case DatabaseTypes.MARIADB:
            case DatabaseTypes.TIDB:
                return "utf8mb4_general_ci";
            case DatabaseTypes.POSTGRESQL:
            case DatabaseTypes.GAUSSDB:
            case DatabaseTypes.OPENGAUSS:
                return "C";
            case DatabaseTypes.ORACLE:
                return "USING_NLS_COMP";
            default:
                return "";
        }
    }

    /**
     * 获取数据库引号字符
     */
    public static String getQuoteString(String dbType) {
        dbType = dbType.toLowerCase();
        switch (dbType) {
            case DatabaseTypes.MYSQL:
            case DatabaseTypes.MARIADB:
            case DatabaseTypes.TIDB:
            case DatabaseTypes.HIVE:
            case DatabaseTypes.CLICKHOUSE:
                return "`";
            case DatabaseTypes.ORACLE:
            case DatabaseTypes.POSTGRESQL:
            case DatabaseTypes.DB2:
            case DatabaseTypes.GAUSSDB:
            case DatabaseTypes.OPENGAUSS:
            case DatabaseTypes.KINGBASE:
                return "\"";
            case DatabaseTypes.SQLSERVER:
                return "[";
            default:
                return "\"";
        }
    }

    /**
     * 获取数据库验证查询SQL
     */
    public static String getValidationQuery(String dbType) {
        dbType = dbType.toLowerCase();
        switch (dbType) {
            case DatabaseTypes.ORACLE:
                return "SELECT 1 FROM DUAL";
            case DatabaseTypes.DB2:
                return "SELECT 1 FROM SYSIBM.SYSDUMMY1";
            default:
                return "SELECT 1";
        }
    }

    /**
     * 获取数据库分页SQL
     */
    public static String generatePageSql(String dbType, String sql, int offset, int limit) {
        dbType = dbType.toLowerCase();
        switch (dbType) {
            case DatabaseTypes.ORACLE:
                return String.format(
                    "SELECT * FROM (SELECT a.*, ROWNUM rnum FROM (%s) a WHERE ROWNUM <= %d) WHERE rnum > %d",
                    sql, offset + limit, offset
                );
            case DatabaseTypes.SQLSERVER:
                return String.format(
                    "SELECT * FROM (SELECT ROW_NUMBER() OVER (ORDER BY (SELECT 0)) AS RowNum, * FROM (%s) AS Results) AS RowConstrainedResult WHERE RowNum > %d AND RowNum <= %d",
                    sql, offset, offset + limit
                );
            default:
                return sql + " LIMIT " + limit + " OFFSET " + offset;
        }
    }

    /**
     * 获取数据库系统数据库列表
     */
    public static List<String> getSystemDatabases(String dbType) {
        dbType = dbType.toLowerCase();
        switch (dbType) {
            case DatabaseTypes.MYSQL:
            case DatabaseTypes.MARIADB:
                return Arrays.asList("mysql", "information_schema", "performance_schema", "sys");
            case DatabaseTypes.POSTGRESQL:
            case DatabaseTypes.GAUSSDB:
            case DatabaseTypes.OPENGAUSS:
                return Arrays.asList("postgres", "template0", "template1");
            case DatabaseTypes.ORACLE:
                return Arrays.asList("SYSTEM", "SYS", "SYSMAN", "DBSNMP");
            case DatabaseTypes.SQLSERVER:
                return Arrays.asList("master", "tempdb", "model", "msdb");
            case DatabaseTypes.HIVE:
                return Arrays.asList("default", "information_schema");
            case DatabaseTypes.CLICKHOUSE:
                return Arrays.asList("system", "information_schema", "default");
            default:
                return Collections.emptyList();
        }
    }

    /**
     * 判断是否为系统数据库
     */
    public static boolean isSystemDatabase(String dbType, String dbName) {
        if (dbName == null) {
            return false;
        }
        return getSystemDatabases(dbType).contains(dbName.toLowerCase());
    }

    /**
     * 获取数据库的最大连接数限制
     */
    public static int getMaxConnections(String dbType) {
        dbType = dbType.toLowerCase();
        switch (dbType) {
            case DatabaseTypes.MYSQL:
            case DatabaseTypes.MARIADB:
            case DatabaseTypes.TIDB:
                return 151; // MySQL默认最大连接数
            case DatabaseTypes.ORACLE:
                return 1521; // Oracle默认进程数限制
            case DatabaseTypes.POSTGRESQL:
            case DatabaseTypes.GAUSSDB:
            case DatabaseTypes.OPENGAUSS:
                return 100; // PostgreSQL默认最大连接数
            case DatabaseTypes.CLICKHOUSE:
                return 4096; // ClickHouse默认最大连接数
            default:
                return 100; // 默认值
        }
    }

    /**
     * 获取数据库的默认字段长度
     */
    public static int getDefaultFieldLength(String dbType, String fieldType) {
        dbType = dbType.toLowerCase();
        fieldType = fieldType.toUpperCase();
        
        switch (fieldType) {
            case "VARCHAR":
            case "NVARCHAR":
                switch (dbType) {
                    case DatabaseTypes.MYSQL:
                    case DatabaseTypes.MARIADB:
                        return 255;
                    case DatabaseTypes.ORACLE:
                        return 4000;
                    case DatabaseTypes.POSTGRESQL:
                        return 255;
                    default:
                        return 255;
                }
            case "CHAR":
            case "NCHAR":
                return 1;
            case "TEXT":
            case "CLOB":
                switch (dbType) {
                    case DatabaseTypes.MYSQL:
                        return 65535;
                    case DatabaseTypes.ORACLE:
                        return Integer.MAX_VALUE;
                    default:
                        return 65535;
                }
            default:
                return 0;
        }
    }

    /**
     * 获取数据库的保留关键字列表
     */
    public static Set<String> getReservedKeywords(String dbType) {
        dbType = dbType.toLowerCase();
        Set<String> keywords = new HashSet<>();
        
        // 通用SQL关键字
        keywords.addAll(Arrays.asList(
            "SELECT", "INSERT", "UPDATE", "DELETE", "FROM", "WHERE",
            "GROUP", "ORDER", "BY", "HAVING", "JOIN", "LEFT", "RIGHT",
            "INNER", "OUTER", "ON", "AS", "AND", "OR", "NOT", "IN",
            "EXISTS", "BETWEEN", "LIKE", "IS", "NULL", "TRUE", "FALSE"
        ));
        
        // 数据库特定关键字
        switch (dbType) {
            case DatabaseTypes.MYSQL:
                keywords.addAll(Arrays.asList(
                    "SHOW", "DESCRIBE", "DESC", "USE", "DATABASE",
                    "LIMIT", "OFFSET", "PROCEDURE", "FUNCTION"
                ));
                break;
            case DatabaseTypes.ORACLE:
                keywords.addAll(Arrays.asList(
                    "ROWNUM", "ROWID", "CONNECT", "BY", "PRIOR",
                    "START", "WITH", "NOCYCLE", "LEVEL", "DUAL"
                ));
                break;
            case DatabaseTypes.POSTGRESQL:
                keywords.addAll(Arrays.asList(
                    "SIMILAR", "ILIKE", "RETURNING", "VARIADIC",
                    "ANALYSE", "ANALYZE", "CONCURRENTLY"
                ));
                break;
        }
        return keywords;
    }

    /**
     * 判断是否为数据库关键字
     */
    public static boolean isReservedKeyword(String dbType, String word) {
        if (word == null) {
            return false;
        }
        return getReservedKeywords(dbType).contains(word.toUpperCase());
    }

    /**
     * 转义数据库关键字（如果需要）
     */
    public static String escapeKeyword(String dbType, String word) {
        if (word == null || !isReservedKeyword(dbType, word)) {
            return word;
        }
        String quote = getQuoteString(dbType);
        return quote + word + quote;
    }

    /**
     * 获取数据库的默认时区
     */
    public static String getDefaultTimeZone(String dbType) {
        dbType = dbType.toLowerCase();
        switch (dbType) {
            case DatabaseTypes.MYSQL:
            case DatabaseTypes.MARIADB:
            case DatabaseTypes.TIDB:
                return "SYSTEM";
            case DatabaseTypes.ORACLE:
                return "GMT";
            case DatabaseTypes.POSTGRESQL:
            case DatabaseTypes.GAUSSDB:
            case DatabaseTypes.OPENGAUSS:
                return "GMT";
            default:
                return "GMT";
        }
    }

    /**
     * 判断是否为大数据类数据库
     */
    public static boolean isBigDataDatabase(String type) {
        return type != null && (
            DatabaseTypes.HIVE.equalsIgnoreCase(type) ||
            DatabaseTypes.HBASE.equalsIgnoreCase(type) ||
            DatabaseTypes.CLICKHOUSE.equalsIgnoreCase(type) ||
            DatabaseTypes.DORIS.equalsIgnoreCase(type) ||
            DatabaseTypes.STARROCKS.equalsIgnoreCase(type) ||
            DatabaseTypes.SPARKSQL.equalsIgnoreCase(type) ||
            DatabaseTypes.PRESTO.equalsIgnoreCase(type) ||
            DatabaseTypes.TRINO.equalsIgnoreCase(type) ||
            DatabaseTypes.IMPALA.equalsIgnoreCase(type) ||
            DatabaseTypes.KYLIN.equalsIgnoreCase(type)
        );
    }

    /**
     * 判断是否为分析型数据库
     */
    public static boolean isAnalyticalDatabase(String type) {
        return type != null && (
            DatabaseTypes.CLICKHOUSE.equalsIgnoreCase(type) ||
            DatabaseTypes.DORIS.equalsIgnoreCase(type) ||
            DatabaseTypes.STARROCKS.equalsIgnoreCase(type) ||
            DatabaseTypes.SPARKSQL.equalsIgnoreCase(type) ||
            DatabaseTypes.PRESTO.equalsIgnoreCase(type) ||
            DatabaseTypes.TRINO.equalsIgnoreCase(type) ||
            DatabaseTypes.IMPALA.equalsIgnoreCase(type) ||
            DatabaseTypes.KYLIN.equalsIgnoreCase(type)
        );
    }

    /**
     * 判断是否为MPP数据库
     */
    public static boolean isMPPDatabase(String type) {
        return type != null && (
            DatabaseTypes.CLICKHOUSE.equalsIgnoreCase(type) ||
            DatabaseTypes.DORIS.equalsIgnoreCase(type) ||
            DatabaseTypes.STARROCKS.equalsIgnoreCase(type) ||
            DatabaseTypes.SPARKSQL.equalsIgnoreCase(type) ||
            DatabaseTypes.PRESTO.equalsIgnoreCase(type) ||
            DatabaseTypes.TRINO.equalsIgnoreCase(type) ||
            DatabaseTypes.IMPALA.equalsIgnoreCase(type)
        );
    }
} 