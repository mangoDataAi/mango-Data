package com.mango.test.database.service.impl.datasource;

import com.mango.test.constant.DatabaseTypes;
import com.mango.test.database.entity.DataSource;
import com.mango.test.database.service.impl.datasource.handlers.*;
import com.mango.test.database.service.impl.datasource.handlers.file.*;
import com.mango.test.database.service.impl.datasource.handlers.graph.Neo4jHandler;
import com.mango.test.database.service.impl.datasource.handlers.mq.KafkaHandler;
import com.mango.test.database.service.impl.datasource.handlers.nosql.*;
import com.mango.test.database.service.impl.datasource.handlers.time.InfluxDBHandler;

import java.util.HashMap;
import java.util.Map;

public class DatabaseHandlerFactory {
    
    private static final Map<String, String> TYPE_MAPPING = new HashMap<>();
    private static final Map<String, Map<String, String>> COMMON_TYPE_MAPPING = new HashMap<>();

    static {
        // 基本类型映射
        TYPE_MAPPING.put("string", "VARCHAR2");
        TYPE_MAPPING.put("text", "CLOB");
        TYPE_MAPPING.put("int", "NUMBER");
        TYPE_MAPPING.put("bigint", "NUMBER(19)");
        TYPE_MAPPING.put("float", "BINARY_FLOAT");
        TYPE_MAPPING.put("double", "BINARY_DOUBLE");
        TYPE_MAPPING.put("decimal", "NUMBER");
        TYPE_MAPPING.put("boolean", "NUMBER(1)");
        TYPE_MAPPING.put("date", "DATE");
        TYPE_MAPPING.put("time", "TIMESTAMP");
        TYPE_MAPPING.put("timestamp", "TIMESTAMP");
        TYPE_MAPPING.put("binary", "BLOB");
        TYPE_MAPPING.put("json", "CLOB");

        // MySQL 类型映射
        Map<String, String> mysqlMapping = new HashMap<>();
        mysqlMapping.put("VARCHAR", "VARCHAR2");
        mysqlMapping.put("CHAR", "CHAR");
        mysqlMapping.put("TEXT", "CLOB");
        mysqlMapping.put("TINYTEXT", "VARCHAR2");
        mysqlMapping.put("MEDIUMTEXT", "CLOB");
        mysqlMapping.put("LONGTEXT", "CLOB");
        mysqlMapping.put("TINYINT", "NUMBER(3)");
        mysqlMapping.put("SMALLINT", "NUMBER(5)");
        mysqlMapping.put("MEDIUMINT", "NUMBER(7)");
        mysqlMapping.put("INT", "NUMBER(10)");
        mysqlMapping.put("INTEGER", "NUMBER(10)");
        mysqlMapping.put("BIGINT", "NUMBER(19)");
        mysqlMapping.put("FLOAT", "BINARY_FLOAT");
        mysqlMapping.put("DOUBLE", "BINARY_DOUBLE");
        mysqlMapping.put("DECIMAL", "NUMBER");
        mysqlMapping.put("NUMERIC", "NUMBER");
        mysqlMapping.put("BOOLEAN", "NUMBER(1)");
        mysqlMapping.put("BOOL", "NUMBER(1)");
        mysqlMapping.put("DATE", "DATE");
        mysqlMapping.put("TIME", "TIMESTAMP");
        mysqlMapping.put("DATETIME", "TIMESTAMP");
        mysqlMapping.put("TIMESTAMP", "TIMESTAMP");
        mysqlMapping.put("BINARY", "RAW");
        mysqlMapping.put("VARBINARY", "RAW");
        mysqlMapping.put("BLOB", "BLOB");
        mysqlMapping.put("TINYBLOB", "BLOB");
        mysqlMapping.put("MEDIUMBLOB", "BLOB");
        mysqlMapping.put("LONGBLOB", "BLOB");
        mysqlMapping.put("JSON", "CLOB");
        mysqlMapping.put("ENUM", "VARCHAR2");
        mysqlMapping.put("SET", "VARCHAR2");
        COMMON_TYPE_MAPPING.put("mysql", mysqlMapping);

        // PostgreSQL 类型映射
        Map<String, String> postgresMapping = new HashMap<>();
        postgresMapping.put("VARCHAR", "VARCHAR2");
        postgresMapping.put("CHAR", "CHAR");
        postgresMapping.put("TEXT", "CLOB");
        postgresMapping.put("SMALLINT", "NUMBER(5)");
        postgresMapping.put("INTEGER", "NUMBER(10)");
        postgresMapping.put("BIGINT", "NUMBER(19)");
        postgresMapping.put("DECIMAL", "NUMBER");
        postgresMapping.put("NUMERIC", "NUMBER");
        postgresMapping.put("REAL", "BINARY_FLOAT");
        postgresMapping.put("DOUBLE PRECISION", "BINARY_DOUBLE");
        postgresMapping.put("MONEY", "NUMBER(19,4)");
        postgresMapping.put("BOOLEAN", "NUMBER(1)");
        postgresMapping.put("DATE", "DATE");
        postgresMapping.put("TIME", "TIMESTAMP");
        postgresMapping.put("TIMESTAMP", "TIMESTAMP");
        postgresMapping.put("BYTEA", "BLOB");
        postgresMapping.put("JSON", "CLOB");
        postgresMapping.put("JSONB", "CLOB");
        postgresMapping.put("XML", "XMLTYPE");
        postgresMapping.put("UUID", "VARCHAR2(36)");
        COMMON_TYPE_MAPPING.put("postgresql", postgresMapping);

        // DM7 类型映射
        Map<String, String> dm7Mapping = new HashMap<>();
        dm7Mapping.put("VARCHAR", "VARCHAR2");
        dm7Mapping.put("VARCHAR2", "VARCHAR2");
        dm7Mapping.put("CHAR", "CHAR");
        dm7Mapping.put("TEXT", "CLOB");
        dm7Mapping.put("CLOB", "CLOB");
        dm7Mapping.put("INT", "NUMBER(10)");
        dm7Mapping.put("INTEGER", "NUMBER(10)");
        dm7Mapping.put("BIGINT", "NUMBER(19)");
        dm7Mapping.put("DECIMAL", "NUMBER");
        dm7Mapping.put("NUMBER", "NUMBER");
        dm7Mapping.put("FLOAT", "BINARY_FLOAT");
        dm7Mapping.put("DOUBLE", "BINARY_DOUBLE");
        dm7Mapping.put("BOOLEAN", "NUMBER(1)");
        dm7Mapping.put("DATE", "DATE");
        dm7Mapping.put("TIME", "TIMESTAMP");
        dm7Mapping.put("TIMESTAMP", "TIMESTAMP");
        dm7Mapping.put("BINARY", "RAW");
        dm7Mapping.put("BLOB", "BLOB");
        dm7Mapping.put("IMAGE", "BLOB");
        COMMON_TYPE_MAPPING.put("dm7", dm7Mapping);

        // ClickHouse 类型映射
        Map<String, String> clickhouseMapping = new HashMap<>();
        clickhouseMapping.put("String", "VARCHAR2");
        clickhouseMapping.put("FixedString", "CHAR");
        clickhouseMapping.put("UInt8", "NUMBER(3)");
        clickhouseMapping.put("UInt16", "NUMBER(5)");
        clickhouseMapping.put("UInt32", "NUMBER(10)");
        clickhouseMapping.put("UInt64", "NUMBER(20)");
        clickhouseMapping.put("Int8", "NUMBER(3)");
        clickhouseMapping.put("Int16", "NUMBER(5)");
        clickhouseMapping.put("Int32", "NUMBER(10)");
        clickhouseMapping.put("Int64", "NUMBER(19)");
        clickhouseMapping.put("Float32", "BINARY_FLOAT");
        clickhouseMapping.put("Float64", "BINARY_DOUBLE");
        clickhouseMapping.put("Decimal", "NUMBER");
        clickhouseMapping.put("Date", "DATE");
        clickhouseMapping.put("DateTime", "TIMESTAMP");
        clickhouseMapping.put("DateTime64", "TIMESTAMP");
        clickhouseMapping.put("UUID", "VARCHAR2(36)");
        COMMON_TYPE_MAPPING.put("clickhouse", clickhouseMapping);
    }

    public static AbstractDataSourceHandler getHandler(DataSource dataSource) {
        if (dataSource == null || dataSource.getType() == null) {
            throw new IllegalArgumentException("数据源或数据库类型不能为空");
        }

        String dbType = dataSource.getType().toLowerCase();
        
        // 关系型数据库
        switch (dbType) {
            case DatabaseTypes.MYSQL:
                return new MySQLHandler(dataSource);
            case DatabaseTypes.ORACLE:
                return new OracleHandler(dataSource);
//            case DatabaseTypes.POSTGRESQL:
//                return new PostgreSQLHandler(dataSource);
//            case DatabaseTypes.SQLSERVER:
//                return new SQLServerHandler(dataSource);
//            case DatabaseTypes.DB2:
//                return new DB2Handler(dataSource);
//            case DatabaseTypes.MARIADB:
//                return new MariaDBHandler(dataSource);
//            case DatabaseTypes.SYBASE:
//                return new SybaseHandler(dataSource);
//            case DatabaseTypes.HANA:
//                return new HanaHandler(dataSource);
//            case DatabaseTypes.H2:
//                return new H2Handler(dataSource);
                
            // 国产数据库
            case DatabaseTypes.DM7:
                return new DM7Handler(dataSource);
            case DatabaseTypes.DM8:
                return new DM8Handler(dataSource);
//            case DatabaseTypes.GAUSSDB:
//                return new GaussDBHandler(dataSource);
//            case DatabaseTypes.OPENGAUSS:
//                return new OpenGaussHandler(dataSource);
//            case DatabaseTypes.KINGBASE:
//                return new KingbaseHandler(dataSource);
//            case DatabaseTypes.SHENTONG:
//                return new ShenTongHandler(dataSource);
//            case DatabaseTypes.HIGHGO:
//                return new HighGoHandler(dataSource);
//            case DatabaseTypes.GBASE:
//                return new GBaseHandler(dataSource);
//            case DatabaseTypes.OSCAR:
//                return new OscarHandler(dataSource);
//            case DatabaseTypes.UXDB:
//                return new UxDBHandler(dataSource);
//            case DatabaseTypes.HYDB:
//                return new HyDBHandler(dataSource);
//            case DatabaseTypes.XUGU:
//                return new XuGuHandler(dataSource);
//            case DatabaseTypes.NEUDB:
//                return new NeuDBHandler(dataSource);
//            case DatabaseTypes.SEQUOIADB:
//                return new SequoiaDBHandler(dataSource);
//            case DatabaseTypes.TBASE:
//                return new TBaseHandler(dataSource);
                
            // 大数据数据库
            case DatabaseTypes.HIVE:
                return new HiveHandler(dataSource);
//            case DatabaseTypes.HBASE:
//                return new HBaseHandler(dataSource);
            case DatabaseTypes.CLICKHOUSE:
                return new ClickHouseHandler(dataSource);
//            case DatabaseTypes.DORIS:
//                return new DorisHandler(dataSource);
//            case DatabaseTypes.STARROCKS:
//                return new StarRocksHandler(dataSource);
//            case DatabaseTypes.SPARKSQL:
//                return new SparkSQLHandler(dataSource);
//            case DatabaseTypes.PRESTO:
//                return new PrestoHandler(dataSource);
//            case DatabaseTypes.TRINO:
//                return new TrinoHandler(dataSource);
//            case DatabaseTypes.IMPALA:
//                return new ImpalaHandler(dataSource);
//            case DatabaseTypes.KYLIN:
//                return new KylinHandler(dataSource);
//            case DatabaseTypes.TIDB:
//                return new TiDBHandler(dataSource);
//            case DatabaseTypes.OCEANBASE:
//                return new OceanBaseHandler(dataSource);
                
            // NoSQL数据库
            case DatabaseTypes.MONGODB:
                return new MongoDBHandler(dataSource);
//            case DatabaseTypes.REDIS:
//                return new RedisHandler(dataSource);
//            case DatabaseTypes.ELASTICSEARCH:
//                return new ElasticSearchHandler(dataSource);
//            case DatabaseTypes.CASSANDRA:
//                return new CassandraHandler(dataSource);
//            case DatabaseTypes.COUCHDB:
//                return new CouchDBHandler(dataSource);
//            case DatabaseTypes.COUCHBASE:
//                return new CouchbaseHandler(dataSource);

                
            // 消息队列
            case DatabaseTypes.KAFKA:
                return new KafkaHandler(dataSource);
//            case DatabaseTypes.RABBITMQ:
//                return new RabbitMQHandler(dataSource);
//            case DatabaseTypes.ROCKETMQ:
//                return new RocketMQHandler(dataSource);
//            case DatabaseTypes.ACTIVEMQ:
//                return new ActiveMQHandler(dataSource);
                
            // 文件系统
//            case DatabaseTypes.FTP:
//                return new FtpHandler(dataSource);
            case DatabaseTypes.SFTP:
                return new SftpHandler(dataSource);
//            case DatabaseTypes.HDFS:
//                return new HdfsHandler(dataSource);
//            case DatabaseTypes.MINIO:
//                return new MinioHandler(dataSource);
//            case DatabaseTypes.S3:
//                return new S3Handler(dataSource);
//            case DatabaseTypes.OSS:
//                return new OSSHandler(dataSource);
                
            // 图数据库
            case DatabaseTypes.NEO4J:
                return new Neo4jHandler(dataSource);
//            case DatabaseTypes.NEBULA:
//                return new NebulaHandler(dataSource);
//            case DatabaseTypes.HUGEGRAPH:
//                return new HugeGraphHandler(dataSource);
//            case DatabaseTypes.JANUSGRAPH:
//                return new JanusGraphHandler(dataSource);
                
            // 时序数据库
            case DatabaseTypes.INFLUXDB:
                return new InfluxDBHandler(dataSource);
//            case DatabaseTypes.TDENGINE:
//                return new TDengineHandler(dataSource);
//            case DatabaseTypes.OPENTSDB:
//                return new OpenTSDBHandler(dataSource);
//            case DatabaseTypes.TIMESCALEDB:
//                return new TimescaleDBHandler(dataSource);
                
            default:
                throw new IllegalArgumentException("不支持的数据库类型: " + dbType);
        }
    }

    /**
     * 根据数据库类型获取数据库处理器
     * @param dbType 数据库类型
     * @return 数据库处理器
     */
    public static AbstractDataSourceHandler getHandler(String dbType) {
        if (dbType == null) {
            throw new IllegalArgumentException("数据库类型不能为空");
        }

        dbType = dbType.toLowerCase();
        
        // 关系型数据库
        switch (dbType) {
            case DatabaseTypes.MYSQL:
                return new MySQLHandler(null);
            case DatabaseTypes.ORACLE:
                return new OracleHandler(null);
//            case DatabaseTypes.POSTGRESQL:
//                return new PostgreSQLHandler(null);
//            case DatabaseTypes.SQLSERVER:
//                return new SQLServerHandler(null);
//            case DatabaseTypes.MARIADB:
//                return new MariaDBHandler(null);
//            case DatabaseTypes.DB2:
//                return new DB2Handler(null);
//            case DatabaseTypes.HANA:
//                return new HanaHandler(null);
//            case DatabaseTypes.SYBASE:
//                return new SybaseHandler(null);
//            case DatabaseTypes.H2:
//                return new H2Handler(null);
                
            // 国产数据库
            case DatabaseTypes.DM7:
                return new DM7Handler(null);
            case DatabaseTypes.DM8:
                return new DM8Handler(null);
//            case DatabaseTypes.GAUSSDB:
//                return new GaussDBHandler(null);
//            case DatabaseTypes.OPENGAUSS:
//                return new OpenGaussHandler(null);
//            case DatabaseTypes.KINGBASE:
//                return new KingbaseHandler(null);
//            case DatabaseTypes.SHENTONG:
//                return new ShenTongHandler(null);
//            case DatabaseTypes.HIGHGO:
//                return new HighGoHandler(null);
//            case DatabaseTypes.GBASE:
//                return new GBaseHandler(null);
//            case DatabaseTypes.OSCAR:
//                return new OscarHandler(null);
//            case DatabaseTypes.UXDB:
//                return new UxDBHandler(null);
//            case DatabaseTypes.HYDB:
//                return new HyDBHandler(null);
//            case DatabaseTypes.XUGU:
//                return new XuGuHandler(null);
//            case DatabaseTypes.NEUDB:
//                return new NeuDBHandler(null);
//            case DatabaseTypes.SEQUOIADB:
//                return new SequoiaDBHandler(null);
//            case DatabaseTypes.TBASE:
//                return new TBaseHandler(null);
                
            // 大数据数据库
            case DatabaseTypes.HIVE:
                return new HiveHandler(null);
//            case DatabaseTypes.HBASE:
//                return new HBaseHandler(null);
            case DatabaseTypes.CLICKHOUSE:
                return new ClickHouseHandler(null);
//            case DatabaseTypes.DORIS:
//                return new DorisHandler(null);
//            case DatabaseTypes.STARROCKS:
//                return new StarRocksHandler(null);
//            case DatabaseTypes.SPARKSQL:
//                return new SparkSQLHandler(null);
//            case DatabaseTypes.PRESTO:
//                return new PrestoHandler(null);
//            case DatabaseTypes.TRINO:
//                return new TrinoHandler(null);
//            case DatabaseTypes.IMPALA:
//                return new ImpalaHandler(null);
//            case DatabaseTypes.KYLIN:
//                return new KylinHandler(null);
//            case DatabaseTypes.TIDB:
//                return new TiDBHandler(null);
//            case DatabaseTypes.OCEANBASE:
//                return new OceanBaseHandler(null);
                
            // NoSQL数据库
            case DatabaseTypes.MONGODB:
                return new MongoDBHandler(null);
//            case DatabaseTypes.REDIS:
//                return new RedisHandler(null);
//            case DatabaseTypes.ELASTICSEARCH:
//                return new ElasticSearchHandler(null);
//            case DatabaseTypes.CASSANDRA:
//                return new CassandraHandler(null);
//            case DatabaseTypes.COUCHDB:
//                return new CouchDBHandler(null);
//            case DatabaseTypes.COUCHBASE:
//                return new CouchbaseHandler(null);
                
            // 消息队列
            case DatabaseTypes.KAFKA:
                return new KafkaHandler(null);
//            case DatabaseTypes.RABBITMQ:
//                return new RabbitMQHandler(null);
//            case DatabaseTypes.ROCKETMQ:
//                return new RocketMQHandler(null);
//            case DatabaseTypes.ACTIVEMQ:
//                return new ActiveMQHandler(null);
                
            // 文件系统
//            case DatabaseTypes.FTP:
//                return new FtpHandler(null);
            case DatabaseTypes.SFTP:
                return new SftpHandler(null);
//            case DatabaseTypes.HDFS:
//                return new HdfsHandler(null);
//            case DatabaseTypes.MINIO:
//                return new MinioHandler(null);
//            case DatabaseTypes.S3:
//                return new S3Handler(null);
//            case DatabaseTypes.OSS:
//                return new OSSHandler(null);
                
            // 图数据库
            case DatabaseTypes.NEO4J:
                return new Neo4jHandler(null);
//            case DatabaseTypes.NEBULA:
//                return new NebulaHandler(null);
//            case DatabaseTypes.HUGEGRAPH:
//                return new HugeGraphHandler(null);
//            case DatabaseTypes.JANUSGRAPH:
//                return new JanusGraphHandler(null);
                
            // 时序数据库
            case DatabaseTypes.INFLUXDB:
                return new InfluxDBHandler(null);
//            case DatabaseTypes.TDENGINE:
//                return new TDengineHandler(null);
//            case DatabaseTypes.OPENTSDB:
//                return new OpenTSDBHandler(null);
//            case DatabaseTypes.TIMESCALEDB:
//                return new TimescaleDBHandler(null);
                
            default:
                throw new IllegalArgumentException("不支持的数据库类型: " + dbType);
        }
    }

    /**
     * 根据数据库类型获取对应的抽象处理器类
     * @param dbType 数据库类型
     * @return 抽象处理器类
     */
    public static Class<? extends AbstractDataSourceHandler> getAbstractHandler(String dbType) {
        if (dbType == null) {
            throw new IllegalArgumentException("数据库类型不能为空");
        }

        dbType = dbType.toLowerCase();
        
        switch (dbType) {
            // 关系型数据库
            case DatabaseTypes.MYSQL:
            case DatabaseTypes.ORACLE:
            case DatabaseTypes.POSTGRESQL:
            case DatabaseTypes.SQLSERVER:
            case DatabaseTypes.MARIADB:
            case DatabaseTypes.DB2:
            case DatabaseTypes.HANA:
            case DatabaseTypes.SYBASE:
            case DatabaseTypes.H2:
            case DatabaseTypes.DM7:
            case DatabaseTypes.DM8:
            case DatabaseTypes.GAUSSDB:
            case DatabaseTypes.OPENGAUSS:
            case DatabaseTypes.KINGBASE:
            case DatabaseTypes.SHENTONG:
            case DatabaseTypes.HIGHGO:
            case DatabaseTypes.GBASE:
            case DatabaseTypes.OSCAR:
            case DatabaseTypes.UXDB:
            case DatabaseTypes.HYDB:
            case DatabaseTypes.XUGU:
            case DatabaseTypes.NEUDB:
            case DatabaseTypes.SEQUOIADB:
            case DatabaseTypes.TBASE:
            case DatabaseTypes.HIVE:
            case DatabaseTypes.HBASE:
            case DatabaseTypes.CLICKHOUSE:
            case DatabaseTypes.DORIS:
            case DatabaseTypes.STARROCKS:
            case DatabaseTypes.SPARKSQL:
            case DatabaseTypes.PRESTO:
            case DatabaseTypes.TRINO:
            case DatabaseTypes.IMPALA:
            case DatabaseTypes.KYLIN:
            case DatabaseTypes.TIDB:
            case DatabaseTypes.OCEANBASE:
                return AbstractDatabaseHandler.class;
            
            // NoSQL数据库
            case DatabaseTypes.MONGODB:
            case DatabaseTypes.REDIS:
            case DatabaseTypes.ELASTICSEARCH:
            case DatabaseTypes.CASSANDRA:
            case DatabaseTypes.COUCHDB:
            case DatabaseTypes.COUCHBASE:
                return AbstractNoSQLHandler.class;
            
            // 消息队列
            case DatabaseTypes.KAFKA:
            case DatabaseTypes.RABBITMQ:
            case DatabaseTypes.ROCKETMQ:
            case DatabaseTypes.ACTIVEMQ:
                return AbstractMessageQueueHandler.class;
            
            // 文件系统
            case DatabaseTypes.FTP:
            case DatabaseTypes.SFTP:
            case DatabaseTypes.HDFS:
            case DatabaseTypes.MINIO:
            case DatabaseTypes.S3:
            case DatabaseTypes.OSS:
                return AbstractFileSystemHandler.class;
            
            // 图数据库
            case DatabaseTypes.NEO4J:
            case DatabaseTypes.NEBULA:
            case DatabaseTypes.HUGEGRAPH:
            case DatabaseTypes.JANUSGRAPH:
                return AbstractGraphDBHandler.class;
            
            // 时序数据库
            case DatabaseTypes.INFLUXDB:
            case DatabaseTypes.TDENGINE:
            case DatabaseTypes.OPENTSDB:
            case DatabaseTypes.TIMESCALEDB:
                return AbstractTimeSeriesDBHandler.class;
            
            // 默认返回基类
            default:
                return AbstractDataSourceHandler.class;
        }
    }

    /**
     * 根据数据源获取对应的抽象处理器类
     * @param dataSource 数据源
     * @return 抽象处理器类
     */
    public static Class<? extends AbstractDataSourceHandler> getAbstractHandler(DataSource dataSource) {
        if (dataSource == null || dataSource.getType() == null) {
            throw new IllegalArgumentException("数据源或数据库类型不能为空");
        }
        
        return getAbstractHandler(dataSource.getType());
    }

    /**
     * 获取文件系统处理器
     * @param dataSource 数据源
     * @return 文件系统处理器
     */
    public static AbstractFileSystemHandler getFileSystemHandler(DataSource dataSource) {
        if (dataSource == null || dataSource.getType() == null) {
            throw new IllegalArgumentException("数据源或数据库类型不能为空");
        }
        
        String dbType = dataSource.getType().toLowerCase();
        
        switch (dbType) {
//            case DatabaseTypes.FTP:
//                return new FtpHandler(dataSource);
            case DatabaseTypes.SFTP:
                return new SftpHandler(dataSource);
//            case DatabaseTypes.HDFS:
//                return new HdfsHandler(dataSource);
//            case DatabaseTypes.MINIO:
//                return new MinioHandler(dataSource);
//            case DatabaseTypes.S3:
//                return new S3Handler(dataSource);
//            case DatabaseTypes.OSS:
//                return new OSSHandler(dataSource);
            default:
                throw new IllegalArgumentException("不支持的文件系统类型: " + dbType);
        }
    }

    /**
     * 获取关系型数据库处理器
     * @param dataSource 数据源
     * @return 关系型数据库处理器
     */
    public static AbstractDatabaseHandler getDatabaseHandler(DataSource dataSource) {
        if (dataSource == null || dataSource.getType() == null) {
            throw new IllegalArgumentException("数据源或数据库类型不能为空");
        }
        
        String dbType = dataSource.getType().toLowerCase();
        
        switch (dbType) {
            case DatabaseTypes.MYSQL:
                return new MySQLHandler(dataSource);
            case DatabaseTypes.ORACLE:
                return new OracleHandler(dataSource);
//            case DatabaseTypes.POSTGRESQL:
//                return new PostgreSQLHandler(dataSource);
//            case DatabaseTypes.SQLSERVER:
//                return new SQLServerHandler(dataSource);
//            case DatabaseTypes.MARIADB:
//                return new MariaDBHandler(dataSource);
//            case DatabaseTypes.DB2:
//                return new DB2Handler(dataSource);
//            case DatabaseTypes.HANA:
//                return new HanaHandler(dataSource);
//            case DatabaseTypes.SYBASE:
//                return new SybaseHandler(dataSource);
//            case DatabaseTypes.H2:
//                return new H2Handler(dataSource);
            case DatabaseTypes.DM7:
                return new DM7Handler(dataSource);
            case DatabaseTypes.DM8:
                return new DM8Handler(dataSource);
//            case DatabaseTypes.GAUSSDB:
//                return new GaussDBHandler(dataSource);
//            case DatabaseTypes.OPENGAUSS:
//                return new OpenGaussHandler(dataSource);
//            case DatabaseTypes.KINGBASE:
//                return new KingbaseHandler(dataSource);
//            case DatabaseTypes.SHENTONG:
//                return new ShenTongHandler(dataSource);
//            case DatabaseTypes.HIGHGO:
//                return new HighGoHandler(dataSource);
//            case DatabaseTypes.GBASE:
//                return new GBaseHandler(dataSource);
//            case DatabaseTypes.OSCAR:
//                return new OscarHandler(dataSource);
//            case DatabaseTypes.UXDB:
//                return new UxDBHandler(dataSource);
//            case DatabaseTypes.HYDB:
//                return new HyDBHandler(dataSource);
//            case DatabaseTypes.XUGU:
//                return new XuGuHandler(dataSource);
//            case DatabaseTypes.NEUDB:
//                return new NeuDBHandler(dataSource);
//            case DatabaseTypes.SEQUOIADB:
//                return new SequoiaDBHandler(dataSource);
//            case DatabaseTypes.TBASE:
//                return new TBaseHandler(dataSource);
            case DatabaseTypes.HIVE:
                return new HiveHandler(dataSource);
//            case DatabaseTypes.HBASE:
//                return new HBaseHandler(dataSource);
            case DatabaseTypes.CLICKHOUSE:
                return new ClickHouseHandler(dataSource);
//            case DatabaseTypes.DORIS:
//                return new DorisHandler(dataSource);
//            case DatabaseTypes.STARROCKS:
//                return new StarRocksHandler(dataSource);
//            case DatabaseTypes.SPARKSQL:
//                return new SparkSQLHandler(dataSource);
//            case DatabaseTypes.PRESTO:
//                return new PrestoHandler(dataSource);
//            case DatabaseTypes.TRINO:
//                return new TrinoHandler(dataSource);
//            case DatabaseTypes.IMPALA:
//                return new ImpalaHandler(dataSource);
//            case DatabaseTypes.KYLIN:
//                return new KylinHandler(dataSource);
//            case DatabaseTypes.TIDB:
//                return new TiDBHandler(dataSource);
//            case DatabaseTypes.OCEANBASE:
//                return new OceanBaseHandler(dataSource);
            default:
                throw new IllegalArgumentException("不支持的关系型数据库类型: " + dbType);
        }
    }

    /**
     * 获取NoSQL数据库处理器
     * @param dataSource 数据源
     * @return NoSQL数据库处理器
     */
    public static AbstractNoSQLHandler getNoSQLHandler(DataSource dataSource) {
        if (dataSource == null || dataSource.getType() == null) {
            throw new IllegalArgumentException("数据源或数据库类型不能为空");
        }
        
        String dbType = dataSource.getType().toLowerCase();
        
        switch (dbType) {
            case DatabaseTypes.MONGODB:
                return new MongoDBHandler(dataSource);
//            case DatabaseTypes.REDIS:
//                return new RedisHandler(dataSource);
//            case DatabaseTypes.ELASTICSEARCH:
//                return new ElasticSearchHandler(dataSource);
//            case DatabaseTypes.CASSANDRA:
//                return new CassandraHandler(dataSource);
//            case DatabaseTypes.COUCHDB:
//                return new CouchDBHandler(dataSource);
//            case DatabaseTypes.COUCHBASE:
//                return new CouchbaseHandler(dataSource);
            default:
                throw new IllegalArgumentException("不支持的NoSQL数据库类型: " + dbType);
        }
    }

    /**
     * 获取图数据库处理器
     * @param dataSource 数据源
     * @return 图数据库处理器
     */
    public static AbstractGraphDBHandler getGraphDBHandler(DataSource dataSource) {
        if (dataSource == null || dataSource.getType() == null) {
            throw new IllegalArgumentException("数据源或数据库类型不能为空");
        }
        
        String dbType = dataSource.getType().toLowerCase();
        
        switch (dbType) {
            case DatabaseTypes.NEO4J:
                return new Neo4jHandler(dataSource);
//            case DatabaseTypes.NEBULA:
//                return new NebulaHandler(dataSource);
//            case DatabaseTypes.HUGEGRAPH:
//                return new HugeGraphHandler(dataSource);
//            case DatabaseTypes.JANUSGRAPH:
//                return new JanusGraphHandler(dataSource);
            default:
                throw new IllegalArgumentException("不支持的图数据库类型: " + dbType);
        }
    }

    /**
     * 获取消息队列处理器
     * @param dataSource 数据源
     * @return 消息队列处理器
     */
    public static AbstractMessageQueueHandler getMessageQueueHandler(DataSource dataSource) {
        if (dataSource == null || dataSource.getType() == null) {
            throw new IllegalArgumentException("数据源或数据库类型不能为空");
        }
        
        String dbType = dataSource.getType().toLowerCase();
        
        switch (dbType) {
            case DatabaseTypes.KAFKA:
                return new KafkaHandler(dataSource);
//            case DatabaseTypes.RABBITMQ:
//                return new RabbitMQHandler(dataSource);
//            case DatabaseTypes.ROCKETMQ:
//                return new RocketMQHandler(dataSource);
//            case DatabaseTypes.ACTIVEMQ:
//                return new ActiveMQHandler(dataSource);
            default:
                throw new IllegalArgumentException("不支持的消息队列类型: " + dbType);
        }
    }

    /**
     * 获取时序数据库处理器
     * @param dataSource 数据源
     * @return 时序数据库处理器
     */
    public static AbstractTimeSeriesDBHandler getTimeSeriesDBHandler(DataSource dataSource) {
        if (dataSource == null || dataSource.getType() == null) {
            throw new IllegalArgumentException("数据源或数据库类型不能为空");
        }
        
        String dbType = dataSource.getType().toLowerCase();
        
        switch (dbType) {
            case DatabaseTypes.INFLUXDB:
                return new InfluxDBHandler(dataSource);
//            case DatabaseTypes.TDENGINE:
//                return new TDengineHandler(dataSource);
//            case DatabaseTypes.OPENTSDB:
//                return new OpenTSDBHandler(dataSource);
//            case DatabaseTypes.TIMESCALEDB:
//                return new TimescaleDBHandler(dataSource);
            default:
                throw new IllegalArgumentException("不支持的时序数据库类型: " + dbType);
        }
    }

} 