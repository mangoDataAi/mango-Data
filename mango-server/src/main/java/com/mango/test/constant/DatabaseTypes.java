package com.mango.test.constant;

import java.util.Arrays;

/**
 * 数据库类型常量类
 * 用于统一管理系统中支持的数据库类型
 */
public class DatabaseTypes {
    // 关系型数据库
    public static final String MYSQL = "mysql";
    public static final String ORACLE = "oracle";
    public static final String POSTGRESQL = "postgresql";
    public static final String SQLSERVER = "sqlserver";
    public static final String DB2 = "db2";
    public static final String INFORMIX = "Informix";
    public static final String MARIADB = "mariadb";
    public static final String SYBASE = "sybase";
    public static final String HANA = "hana";
    public static final String H2 = "h2";
    public static final String SNOWFLAKE = "snowflake";
    public static final String TERADATA = "teradata";

    // 国产数据库
    public static final String DM7 = "dm7";
    public static final String DM8 = "dm8";
    public static final String GAUSSDB = "gaussdb";
    public static final String OPENGAUSS = "opengauss";
    public static final String KINGBASE = "kingbase";
    public static final String SHENTONG = "shentong";
    public static final String HIGHGO = "highgo";
    public static final String GBASE = "gbase";
    public static final String UXDB = "uxdb";
    public static final String OSCAR = "oscar";
    public static final String HYDB = "hydb";
    public static final String XUGU = "xugu";
    public static final String NEUDB = "neudb";
    public static final String SEQUOIADB = "sequoiadb";
    public static final String TBASE = "tbase";
    
    // CDC类型
    public static final String CDC_GAUSSDB = "cdc_gaussdb";
    public static final String CDC_OPENGAUSS = "cdc_opengauss";

    // 大数据数据库
    public static final String HIVE = "hive";
    public static final String HBASE = "hbase";
    public static final String CLICKHOUSE = "clickhouse";
    public static final String DORIS = "doris";
    public static final String STARROCKS = "starrocks";
    public static final String SPARKSQL = "sparksql";
    public static final String PRESTO = "presto";
    public static final String TRINO = "trino";
    public static final String IMPALA = "impala";
    public static final String KYLIN = "kylin";

    // 分布式数据库
    public static final String TIDB = "tidb";
    public static final String OCEANBASE = "oceanbase";
    public static final String VITESS = "vitess";

    // 消息队列
    public static final String KAFKA = "kafka";
    public static final String RABBITMQ = "rabbitmq";
    public static final String ROCKETMQ = "rocketmq";
    public static final String ACTIVEMQ = "activemq";

    // NoSQL数据库
    public static final String MONGODB = "mongodb";
    public static final String REDIS = "redis";
    public static final String ELASTICSEARCH = "elasticsearch";
    public static final String CASSANDRA = "cassandra";
    public static final String COUCHBASE = "couchbase";
    public static final String COUCHDB = "couchdb";

    // 图数据库
    public static final String NEO4J = "neo4j";
    public static final String NEBULA = "nebula";
    public static final String HUGEGRAPH = "hugegraph";
    public static final String JANUSGRAPH = "janusgraph";

    // 时序数据库
    public static final String INFLUXDB = "influxdb";
    public static final String TDENGINE = "tdengine";
    public static final String OPENTSDB = "opentsdb";
    public static final String TIMESCALEDB = "timescaledb";

    // 文件系统
    public static final String FTP = "ftp";
    public static final String SFTP = "sftp";
    public static final String HDFS = "hdfs";
    public static final String MINIO = "minio";
    public static final String S3 = "s3";
    public static final String OSS = "oss";
    public static final String GCS = "gcs";
    
    // 通用连接器
    public static final String JDBC = "jdbc";
    
    // Web服务/API
    public static final String REST = "rest";

    // 数据源类型分组 - 用于同步技术判断
    public static final java.util.Set<String> CDC_SOURCES = new java.util.HashSet<>(Arrays.asList(
        MYSQL, POSTGRESQL, ORACLE, SQLSERVER, DB2, GAUSSDB, OPENGAUSS, MARIADB, TIDB, OCEANBASE
    ));

    public static final java.util.Set<String> CDC_TARGETS = new java.util.HashSet<>(Arrays.asList(
        MYSQL, POSTGRESQL, ORACLE, SQLSERVER, DB2, GAUSSDB, OPENGAUSS, MARIADB, TIDB, OCEANBASE,
        CLICKHOUSE, DORIS, KAFKA, ELASTICSEARCH, DM7, DM8
    ));

    public static final java.util.Set<String> DATABASE_TYPES = new java.util.HashSet<>(Arrays.asList(
        MYSQL, ORACLE, POSTGRESQL, SQLSERVER, DB2, MARIADB, SYBASE, H2, DM7, DM8, 
        GAUSSDB, OPENGAUSS, KINGBASE, SHENTONG, HIGHGO, GBASE, UXDB, HYDB, XUGU, 
        NEUDB, TBASE, TIDB, OCEANBASE, CLICKHOUSE, DORIS, STARROCKS, HIVE
    ));

    public static final java.util.Set<String> FILE_SERVICES = new java.util.HashSet<>(Arrays.asList(
        FTP, SFTP, HDFS, MINIO, S3, OSS, GCS
    ));

    public static final java.util.Set<String> MESSAGING_SERVICES = new java.util.HashSet<>(Arrays.asList(
        KAFKA, RABBITMQ, ROCKETMQ, ACTIVEMQ
    ));

    // 数据库族系分组
    public static final String[] MYSQL_FAMILY = {
        MYSQL, MARIADB, TIDB, DORIS, STARROCKS
    };

    public static final String[] ORACLE_FAMILY = {
        ORACLE, DM7, DM8, HYDB, XUGU
    };

    public static final String[] POSTGRESQL_FAMILY = {
        POSTGRESQL, GAUSSDB, OPENGAUSS, KINGBASE, HIGHGO, GBASE, UXDB, TBASE
    };

    public static final String[] SQLSERVER_FAMILY = {
        SQLSERVER, SYBASE
    };

    public static final String[] DB2_FAMILY = {
        DB2, SHENTONG
    };

    public static final String[] BIGDATA_FAMILY = {
        HIVE, HBASE, CLICKHOUSE, SPARKSQL, PRESTO, TRINO, IMPALA, KYLIN
    };

    public static final String[] OLAP_FAMILY = {
        CLICKHOUSE, DORIS, STARROCKS, KYLIN
    };

    public static final String[] NOSQL_FAMILY = {
        SEQUOIADB
    };

    /**
     * 判断是否为关系型数据库
     * @param type 数据库类型
     * @return 是否为关系型数据库
     */
    public static boolean isRelationalDatabase(String type) {
        if (type == null) {
            return false;
        }
        
        type = type.toLowerCase();
        return type.equals(MYSQL) || type.equals(ORACLE) || type.equals(POSTGRESQL) || 
               type.equals(SQLSERVER) || type.equals(DB2) || type.equals(MARIADB) || 
               type.equals(SYBASE) || type.equals(H2) || type.equals(DM7) || type.equals(DM8) || 
               type.equals(GAUSSDB) || type.equals(OPENGAUSS) || type.equals(KINGBASE) || 
               type.equals(SHENTONG) || type.equals(HIGHGO) || type.equals(GBASE) || 
               type.equals(UXDB) || type.equals(HYDB) || type.equals(XUGU) || type.equals(NEUDB) || 
               type.equals(TBASE) || type.equals(TIDB) || type.equals(OCEANBASE);
    }

    /**
     * 判断是否为国产数据库
     * @param type 数据库类型
     * @return 是否为国产数据库
     */
    public static boolean isDomesticDatabase(String type) {
        if (type == null) {
            return false;
        }
        
        type = type.toLowerCase();
        return type.equals(DM7) || type.equals(DM8) || type.equals(GAUSSDB) || 
               type.equals(OPENGAUSS) || type.equals(KINGBASE) || type.equals(SHENTONG) || 
               type.equals(HIGHGO) || type.equals(GBASE) || type.equals(UXDB) || type.equals(HYDB) || 
               type.equals(XUGU) || type.equals(NEUDB) || type.equals(SEQUOIADB) || type.equals(TBASE);
    }

    /**
     * 判断是否为大数据数据库
     * @param type 数据库类型
     * @return 是否为大数据数据库
     */
    public static boolean isBigDataDatabase(String type) {
        if (type == null) {
            return false;
        }
        
        type = type.toLowerCase();
        return type.equals(HIVE) || type.equals(HBASE) || type.equals(CLICKHOUSE) || 
               type.equals(DORIS) || type.equals(STARROCKS) || type.equals(SPARKSQL) || 
               type.equals(PRESTO) || type.equals(TRINO) || type.equals(IMPALA) || type.equals(KYLIN);
    }

    /**
     * 判断是否为MySQL族系数据库
     * @param dbType 数据库类型
     * @return 是否为MySQL族系数据库
     */
    public static boolean isMySQLFamily(String dbType) {
        if (dbType == null) {
            return false;
        }
        
        dbType = dbType.toLowerCase();
        for (String type : MYSQL_FAMILY) {
            if (type.equalsIgnoreCase(dbType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为Oracle族系数据库
     * @param dbType 数据库类型
     * @return 是否为Oracle族系数据库
     */
    public static boolean isOracleFamily(String dbType) {
        if (dbType == null) {
            return false;
        }
        
        dbType = dbType.toLowerCase();
        for (String type : ORACLE_FAMILY) {
            if (type.equalsIgnoreCase(dbType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为PostgreSQL族系数据库
     * @param dbType 数据库类型
     * @return 是否为PostgreSQL族系数据库
     */
    public static boolean isPostgreSQLFamily(String dbType) {
        if (dbType == null) {
            return false;
        }
        
        dbType = dbType.toLowerCase();
        for (String type : POSTGRESQL_FAMILY) {
            if (type.equalsIgnoreCase(dbType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为DB2族系数据库
     * @param dbType 数据库类型
     * @return 是否为DB2族系数据库
     */
    public static boolean isDB2Family(String dbType) {
        if (dbType == null) {
            return false;
        }
        
        dbType = dbType.toLowerCase();
        for (String type : DB2_FAMILY) {
            if (type.equalsIgnoreCase(dbType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为大数据族系数据库
     * @param dbType 数据库类型
     * @return 是否为大数据族系数据库
     */
    public static boolean isBigDataFamily(String dbType) {
        if (dbType == null) {
            return false;
        }
        
        dbType = dbType.toLowerCase();
        for (String type : BIGDATA_FAMILY) {
            if (type.equalsIgnoreCase(dbType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为NoSQL族系数据库
     * @param dbType 数据库类型
     * @return 是否为NoSQL族系数据库
     */
    public static boolean isNoSQLFamily(String dbType) {
        if (dbType == null) {
            return false;
        }
        
        dbType = dbType.toLowerCase();
        for (String type : NOSQL_FAMILY) {
            if (type.equalsIgnoreCase(dbType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取数据库族系名称
     * @param dbType 数据库类型
     * @return 数据库族系名称
     */
    public static String getFamilyName(String dbType) {
        if (dbType == null) {
            return "Unknown";
        }
        
        dbType = dbType.toLowerCase();
        if (isMySQLFamily(dbType)) return "MySQL";
        if (isOracleFamily(dbType)) return "Oracle";
        if (isPostgreSQLFamily(dbType)) return "PostgreSQL";
        if (isDB2Family(dbType)) return "DB2";
        if (isBigDataFamily(dbType)) return "BigData";
        if (isNoSQLFamily(dbType)) return "NoSQL";
        return "Unknown";
    }

    /**
     * 获取数据库类型分类
     * @param dbType 数据库类型
     * @return 数据库类型分类
     */
    public static String getCategory(String dbType) {
        if (dbType == null) {
            return "Unknown";
        }
        
        dbType = dbType.toLowerCase();
        if (isRelationalDatabase(dbType)) return "关系型数据库";
        if (isDomesticDatabase(dbType)) return "国产数据库";
        if (isBigDataDatabase(dbType)) return "大数据数据库";
        return "其他数据库";
    }

    /**
     * 获取数据库所属族系
     * @param dbType 数据库类型
     * @return 数据库所属族系
     */
    public static String getDbFamily(String dbType) {
        if (dbType == null) {
            return "Unknown";
        }
        
        dbType = dbType.toLowerCase();
        if (isMySQLFamily(dbType)) {
            return "MySQL";
        }
        if (isOracleFamily(dbType)) {
            return "Oracle";
        }
        if (isPostgreSQLFamily(dbType)) {
            return "PostgreSQL";
        }
        if (isDB2Family(dbType)) {
            return "DB2";
        }
        if (isBigDataFamily(dbType)) {
            return "BigData";
        }
        if (isNoSQLFamily(dbType)) {
            return "NoSQL";
        }
        return "Unknown";
    }

    /**
     * 转换数据类型
     * @param sourceType 源数据类型
     * @param targetDbType 目标数据库类型
     * @return 转换后的数据类型
     */
    public static String convertType(String sourceType, String targetDbType) {
        if (sourceType == null || targetDbType == null) {
            return sourceType;
        }

        sourceType = sourceType.toUpperCase();
        targetDbType = targetDbType.toLowerCase();

        // 特殊类型转换
        if (Arrays.asList(MYSQL_FAMILY).contains(targetDbType)) {
            if (sourceType.equals("NUMBER")) return "DECIMAL";
            if (sourceType.equals("NVARCHAR")) return "VARCHAR";
            if (sourceType.equals("NCHAR")) return "CHAR";
        } else if (Arrays.asList(ORACLE_FAMILY).contains(targetDbType)) {
            if (sourceType.equals("VARCHAR")) return "VARCHAR2";
            if (sourceType.equals("INT") || sourceType.equals("INTEGER")) return "NUMBER(10)";
            if (sourceType.equals("BIGINT")) return "NUMBER(19)";
        } else if (Arrays.asList(POSTGRESQL_FAMILY).contains(targetDbType)) {
            if (sourceType.equals("VARCHAR2")) return "VARCHAR";
            if (sourceType.equals("NUMBER")) return "NUMERIC";
        }

        return sourceType;
    }

    /**
     * 调整字段长度
     * @param type 字段类型
     * @param length 字段长度
     * @param dbType 数据库类型
     * @return 调整后的字段长度
     */
    public static int adjustLength(String type, Integer length, String dbType) {
        if (length == null) {
            return getDefaultLength(type, dbType);
        }

        type = type.toUpperCase();
        dbType = dbType.toLowerCase();

        // 根据数据库类型调整长度限制
        if (Arrays.asList(MYSQL_FAMILY).contains(dbType)) {
            if (type.contains("VARCHAR")) return Math.min(length, 65535);
            if (type.contains("CHAR")) return Math.min(length, 255);
        } else if (Arrays.asList(ORACLE_FAMILY).contains(dbType)) {
            if (type.contains("VARCHAR2")) return Math.min(length, 4000);
            if (type.contains("CHAR")) return Math.min(length, 2000);
        } else if (Arrays.asList(POSTGRESQL_FAMILY).contains(dbType)) {
            if (type.contains("VARCHAR")) return Math.min(length, 10485760);
            if (type.contains("CHAR")) return Math.min(length, 1000);
        }

        return length;
    }

    /**
     * 获取默认字段长度
     * @param type 字段类型
     * @param dbType 数据库类型
     * @return 默认字段长度
     */
    public static int getDefaultLength(String type, String dbType) {
        type = type.toUpperCase();
        dbType = dbType.toLowerCase();

        // 默认长度设置
        if (type.contains("VARCHAR") || type.contains("NVARCHAR")) {
            if (Arrays.asList(MYSQL_FAMILY).contains(dbType)) return 255;
            if (Arrays.asList(ORACLE_FAMILY).contains(dbType)) return 4000;
            if (Arrays.asList(POSTGRESQL_FAMILY).contains(dbType)) return 255;
            return 255;
        }

        if (type.contains("CHAR") || type.contains("NCHAR")) {
            if (Arrays.asList(MYSQL_FAMILY).contains(dbType)) return 1;
            if (Arrays.asList(ORACLE_FAMILY).contains(dbType)) return 1;
            if (Arrays.asList(POSTGRESQL_FAMILY).contains(dbType)) return 1;
            return 1;
        }

        return 0;
    }
}
