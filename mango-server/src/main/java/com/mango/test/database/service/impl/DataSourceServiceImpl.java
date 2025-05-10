package com.mango.test.database.service.impl;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.alibaba.druid.pool.DruidDataSource;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.mango.test.database.entity.ColumnDefinition;
import com.mango.test.database.entity.ColumnInfo;
import com.mango.test.database.entity.DataSource;
import com.mango.test.database.entity.FileField;
import com.mango.test.database.mapper.DataSourceMapper;
import com.mango.test.database.service.DataSourceService;
import com.mango.test.database.service.impl.datasource.*;
import com.mango.test.dto.TableStructureDTO;
import com.mango.test.vo.R;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.rabbitmq.client.ConnectionFactory;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.text.ParseException;
import java.util.Date;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.mango.test.database.service.impl.datasource.DatabaseHandlerFactory;

@Slf4j
@Service
public class DataSourceServiceImpl extends ServiceImpl<DataSourceMapper, DataSource> implements DataSourceService {


    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;


    @Value("${spring.datasource.file.url}")
    private String fileDbUrl;


    @Value("${spring.datasource.file.username}")
    private String fileDbUsername;


    @Value("${spring.datasource.file.password}")
    private String fileDbPassword;


    @Value("${spring.datasource.file.driver-class-name}")
    private String fileDbDriver;


    // 关系型数据库驱动
    private static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";

    private static final String ORACLE_DRIVER = "oracle.jdbc.OracleDriver";

    private static final String POSTGRESQL_DRIVER = "org.postgresql.Driver";

    private static final String SQLSERVER_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

    private static final String DB2_DRIVER = "com.ibm.db2.jcc.DB2Driver";

    private static final String INFORMIX_DRIVER = "com.informix.jdbc.IfxDriver";

    private static final String SAP_HANA_DRIVER = "com.sap.db.jdbc.Driver";


    // 大数据相关驱动

    private static final String HIVE_DRIVER = "org.apache.hive.jdbc.HiveDriver";

    private static final String HBASE_PHOENIX_DRIVER = "org.apache.phoenix.jdbc.PhoenixDriver";

    private static final String CLICKHOUSE_DRIVER = "ru.yandex.clickhouse.ClickHouseDriver";

    private static final String IMPALA_DRIVER = "com.cloudera.impala.jdbc41.Driver";

    private static final String STARROCKS_DRIVER = "com.mysql.cj.jdbc.Driver"; // StarRocks 使用 MySQL 驱动

    private static final String GREENPLUM_DRIVER = "org.postgresql.Driver"; // GreenPlum 使用 PostgreSQL 驱动


    // 国产数据库驱动

    private static final String DM7_DRIVER = "dm.jdbc.driver.DmDriver";

    private static final String DM8_DRIVER = "dm.jdbc.driver.DmDriver";

    private static final String GAUSSDB_DRIVER = "org.opengauss.Driver";

    private static final String OPENGAUSS_DRIVER = "org.opengauss.Driver";

    private static final String KINGBASE_DRIVER = "com.kingbase8.Driver";

    private static final String GBASE_DRIVER = "com.gbase.jdbc.Driver";

    private static final String UXDB_DRIVER = "com.uxsino.uxdb.Driver";

    private static final String ARTERYBASE_DRIVER = "com.arterybase.jdbc.Driver";

    private static final String PETABASE_DRIVER = "com.petabase.jdbc.Driver";


    // 云数据库驱动

    private static final String ALIYUN_RDS_DRIVER = "com.mysql.cj.jdbc.Driver"; // 阿里云RDS使用对应数据库驱动

    private static final String MAXCOMPUTE_DRIVER = "com.aliyun.odps.jdbc.OdpsDriver";

    private static final String HUAWEI_RDS_DRIVER = "com.mysql.cj.jdbc.Driver"; // 华为云RDS使用对应数据库驱动


    // 添加文件系统相关常量

    private static final String FTP_DRIVER = "org.apache.commons.net.ftp.FTPClient";

    private static final String SFTP_DRIVER = "com.jcraft.jsch.JSch";

    private static final String HDFS_DRIVER = "org.apache.hadoop.fs.FileSystem";

    private static final String S3_DRIVER = "com.amazonaws.services.s3.AmazonS3Client";

    private static final String MINIO_DRIVER = "io.minio.MinioClient";

    private static final String[] DATE_PATTERNS = {
            "yyyy-MM-dd", "yyyy/MM/dd", "yyyyMMdd",
            "yyyy.MM.dd", "dd-MM-yyyy", "dd/MM/yyyy"

    };


    private static final String[] TIME_PATTERNS = {
            "HH:mm:ss", "HH:mm", "HHmmss"
    };

    private static final String[] DATETIME_PATTERNS = {
            "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm",
            "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm",
            "yyyyMMdd HHmmss", "yyyy.MM.dd HH:mm:ss",
            "dd-MM-yyyy HH:mm:ss", "dd/MM/yyyy HH:mm:ss"
    };


    public JdbcTemplate getFileJdbcTemplate() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName(fileDbDriver);

        // 根据驱动类型添加模式名称

        String url = fileDbUrl;

        String schema = "PUBLIC"; // 默认模式名

        if (fileDbDriver.equals(DM7_DRIVER) || fileDbDriver.equals(DM8_DRIVER)) {

            // DM数据库
            schema = fileDbUsername.toUpperCase(); // DM默认使用用户名作为模式名，且要求大写
            url = fileDbUrl + "?schema=" + schema;
        } else if (fileDbDriver.equals(ORACLE_DRIVER)) {
            // Oracle数据库
            schema = fileDbUsername.toUpperCase(); // Oracle默认使用用户名作为模式名，且要求大写
            // Oracle的URL中不需要额外添加schema参数，因为它使用用户名作为默认schema

        } else if (fileDbDriver.equals(MYSQL_DRIVER)) {
            // MySQL数据库
            // 从URL中提取数据库名称作为schema
            int dbNameStart = fileDbUrl.lastIndexOf("/") + 1;
            int dbNameEnd = fileDbUrl.contains("?") ? fileDbUrl.indexOf("?") : fileDbUrl.length();
            schema = fileDbUrl.substring(dbNameStart, dbNameEnd);

        }

        dataSource.setUrl(url);
        dataSource.setUsername(fileDbUsername);
        dataSource.setPassword(fileDbPassword);

        // 设置基本连接池属性

        dataSource.setInitialSize(5);
        dataSource.setMinIdle(5);
        dataSource.setMaxActive(20);
        dataSource.setMaxWait(60000);

        // 设置验证查询，根据数据库类型设置不同的验证SQL

        String validationQuery;

        if (fileDbDriver.equals(DM7_DRIVER) || fileDbDriver.equals(DM8_DRIVER)) {

            validationQuery = "SELECT 1";

        } else if (fileDbDriver.equals(ORACLE_DRIVER)) {

            validationQuery = "SELECT 1 FROM DUAL";
        } else {
            validationQuery = "SELECT 1"; // MySQL和其他数据库
        }

        dataSource.setValidationQuery(validationQuery);

        // 其他连接池设置

        dataSource.setTestWhileIdle(true);
        dataSource.setTestOnBorrow(false);
        dataSource.setTestOnReturn(false);

        try {
            // 初始化数据源
            dataSource.init();
            // 创建并返回JdbcTemplate
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            // 设置当前schema

            if (fileDbDriver.equals(DM7_DRIVER) || fileDbDriver.equals(DM8_DRIVER)) {

                jdbcTemplate.execute("SET SCHEMA " + schema);

            } else if (fileDbDriver.equals(ORACLE_DRIVER)) {

                jdbcTemplate.execute("ALTER SESSION SET CURRENT_SCHEMA = " + schema);

            } else if (fileDbDriver.equals(MYSQL_DRIVER)) {

                jdbcTemplate.execute("USE " + schema);

            }

            return jdbcTemplate;

        } catch (SQLException e) {

            log.error("初始化文件数据源失败", e);

            throw new RuntimeException("初始化文件数据源失败: " + e.getMessage());
        }
    }



    @Override
    public List<Map<String, Object>> getTableList(String id, String types, String searchText) throws Exception {
        DataSource dataSource = this.getById(id);
        if (dataSource == null) {
            throw new RuntimeException("数据源不存在");
        }

        List<String> typeList = Arrays.asList(types.split(","));
        List<Map<String, Object>> result = new ArrayList<>();

        try (Connection conn = getConnection(id)) {
            DatabaseMetaData metaData = conn.getMetaData();

            // 获取当前数据库的模式名
            String schema = null;
            String catalog = null;

            // 根据数据库类型处理 schema
            switch (dataSource.getType().toLowerCase()) {
                case "oracle":
                    schema = dataSource.getUsername().toUpperCase(); // Oracle 使用用户名作为 schema
                    break;
                case "postgresql":
                case "greenplum":
                case "kingbase":
                case "opengauss":
                    schema = "public"; // PostgreSQL 系默认使用 public schema
                    break;
                case "mysql":
                case "mariadb":
                    catalog = dataSource.getDbName(); // MySQL 系使用数据库名作为 catalog
                    break;
                case "dm":
                case "dm7":
                case "dm8":
                    schema = dataSource.getDbName();
                    break;
                case "sqlserver":
                    schema = "dbo"; // SQLServer 默认使用 dbo schema
                    break;
                default:
                    // 其他数据库使用默认 schema
                    break;
            }

            // 获取所有表
            if (typeList.contains("table")) {
                try (ResultSet rs = metaData.getTables(catalog, schema, null, new String[]{"TABLE"})) {
                    while (rs.next()) {
                        String tableName = rs.getString("TABLE_NAME");
                        if (StringUtils.isNotBlank(searchText) && !tableName.toLowerCase().contains(searchText.toLowerCase())) {
                            continue;
                        }
                        Map<String, Object> table = new HashMap<>();
                        table.put("name", tableName);
                        table.put("type", "table");
                        table.put("comment", rs.getString("REMARKS"));
                        table.put("schema", schema);
                        table.put("catalog", catalog);
                        result.add(table);
                    }
                }
            }

            // 获取视图
            if (typeList.contains("view")) {
                try (ResultSet rs = metaData.getTables(catalog, schema, null, new String[]{"VIEW"})) {
                    while (rs.next()) {
                        String viewName = rs.getString("TABLE_NAME");
                        if (StringUtils.isNotBlank(searchText) && !viewName.toLowerCase().contains(searchText.toLowerCase())) {
                            continue;
                        }
                        Map<String, Object> view = new HashMap<>();
                        view.put("name", viewName);
                        view.put("type", "view");
                        view.put("comment", rs.getString("REMARKS"));
                        view.put("schema", schema);
                        view.put("catalog", catalog);
                        result.add(view);
                    }
                }
            }

            // 获取同义词（如果数据库支持）
            if (typeList.contains("synonym")) {
                try (ResultSet rs = metaData.getTables(catalog, schema, null, new String[]{"SYNONYM"})) {
                    while (rs.next()) {
                        String synonymName = rs.getString("TABLE_NAME");
                        if (StringUtils.isNotBlank(searchText) && !synonymName.toLowerCase().contains(searchText.toLowerCase())) {
                            continue;
                        }
                        Map<String, Object> synonym = new HashMap<>();
                        synonym.put("name", synonymName);
                        synonym.put("type", "synonym");
                        synonym.put("comment", rs.getString("REMARKS"));
                        synonym.put("schema", schema);
                        synonym.put("catalog", catalog);
                        result.add(synonym);
                    }
                } catch (SQLException e) {
                    // 某些数据库可能不支持同义词
                    log.warn("数据库不支持同义词查询: {}", e.getMessage());
                }
            }

            return result;
        }
    }

    @Override
    public List<Map<String, Object>> executeQuery(String id, String sql) throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            // 获取数据源信息，用于检测数据库类型
            DataSource dataSource = this.getById(id);
            if (dataSource == null) {
                throw new RuntimeException("数据源不存在");
            }
            
            // 针对ClickHouse的特殊处理
            if ("clickhouse".equalsIgnoreCase(dataSource.getType())) {
                // 检查是否是访问系统表但没有指定system数据库
                String[] knownSystemTables = {
                    "asynchronous_metric_log", "metric_log", "query_log", "query_thread_log",
                    "part_log", "session_log", "trace_log", "crash_log", "text_log", "processors_profile_log",
                    "zookeeper_log", "opentelemetry_span_log", "cluster", "columns", "databases", "dictionaries",
                    "events", "graphite_retentions", "macros", "merge_tree_settings", "numbers", "one", "processes",
                    "replicas", "replication_queue", "settings", "tables", "errors", "zeros", "table_engines",
                    "data_type_families", "formats", "functions", "disks", "clusters", "distribution_queue"
                };
                
                // 提取SQL中的表名，考虑各种引号和格式
                for (String systemTable : knownSystemTables) {
                    // 处理无引号、带反引号和带双引号的表名情况
                    String[] tablePatterns = {
                        "\\b" + systemTable + "\\b", 
                        "`" + systemTable + "`", 
                        "\"" + systemTable + "\""
                    };
                    
                    for (String tablePattern : tablePatterns) {
                        // 忽略已经引用了system数据库的情况
                        if (sql.matches("(?i).*\\bFROM\\s+(?!system\\.)" + tablePattern + ".*")) {
                            // 根据表名是否有引号构造替换
                            String replacement;
                            if (tablePattern.startsWith("`")) {
                                replacement = "$1`system`.`" + systemTable + "`";
                            } else if (tablePattern.startsWith("\"")) {
                                replacement = "$1\"system\".\"" + systemTable + "\"";
                            } else {
                                replacement = "$1system." + systemTable;
                            }
                            
                            sql = sql.replaceAll("(?i)(\\bFROM\\s+)" + tablePattern, replacement);
                            log.info("ClickHouse查询已修正为使用system数据库: {}", sql);
                            break;
                        }
                    }
                }
                
                // 如果查询中有"默认"数据库的明确引用（例如default.part_log），将其替换为system
                for (String systemTable : knownSystemTables) {
                    String defaultPattern = "default\\." + systemTable;
                    if (sql.matches("(?i).*\\bFROM\\s+" + defaultPattern + ".*")) {
                        sql = sql.replaceAll("(?i)(\\bFROM\\s+)default\\." + systemTable, "$1system." + systemTable);
                        log.info("ClickHouse查询已从default数据库修正为system数据库: {}", sql);
                    }
                    
                    // 处理带引号的情况
                    String[] defaultPatterns = {
                        "default\\.`" + systemTable + "`",
                        "`default`\\.`" + systemTable + "`",
                        "default\\.\"" + systemTable + "\"",
                        "\"default\"\\.\"" + systemTable + "\""
                    };
                    
                    for (String pattern : defaultPatterns) {
                        if (sql.matches("(?i).*\\bFROM\\s+" + pattern + ".*")) {
                            // 根据表名引号格式进行替换
                            if (pattern.contains("`")) {
                                sql = sql.replaceAll("(?i)(\\bFROM\\s+)(`?)default(`?)\\.`" + systemTable + "`", 
                                                   "$1`system`.`" + systemTable + "`");
                            } else {
                                sql = sql.replaceAll("(?i)(\\bFROM\\s+)(\"?)default(\"?)\\." + systemTable + "\"", 
                                                   "$1\"system\".\"" + systemTable + "\"");
                            }
                            log.info("ClickHouse查询已从default数据库修正为system数据库(带引号): {}", sql);
                        }
                    }
                }
            }

            try (Connection conn = getConnection(id);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnLabel(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }
                    result.add(row);
                }
            }
        } catch (Exception e) {
            log.error("Execute query failed", e);
            throw new RuntimeException("Execute query failed: " + e.getMessage());
        }

        return result;
    }


    @Override

    public List<Map<String, Object>> getTables(String id) throws Exception {
        DataSource dataSource = this.getById(id);
        if (dataSource == null) {
            throw new Exception("数据源不存在");
        }
        String type = dataSource.getType().toLowerCase();
        // 根据数据源类型分类处理
        switch (type) {
            // 关系型数据库
            case "mysql":
            case "oracle":
            case "postgresql":
            case "sqlserver":
            case "db2":
            case "dm":
            case "dm7":
            case "dm8":
            case "gaussdb":
            case "kingbase":
                return getRelationalTables(dataSource);
            // NoSQL数据库
            case "mongodb":
                return getMongoCollections(dataSource);
            case "redis":
                return getRedisKeys(dataSource);
            case "elasticsearch":
                return getElasticsearchIndices(dataSource);
            case "cassandra":
                return getCassandraTables(dataSource);
            // 大数据相关
            case "hive":
            case "impala":
            case "starrocks":
            case "clickhouse":
                return getBigDataTables(dataSource);

            case "oss":

                return getOssBuckets(dataSource);

            case "minio":

                return getMinioBuckets(dataSource);

            case "ftp":

                return getFtpDirectories(dataSource);

            case "sftp":

                return getSftpDirectories(dataSource);


            default:

                throw new Exception("不支持的数据源类型：" + type);

        }

    }


    // 关系型数据库表获取

    private List<Map<String, Object>> getRelationalTables(DataSource dataSource) throws Exception {

        List<Map<String, Object>> tables = new ArrayList<>();

        try (Connection conn = getConnection(dataSource.getId())) {

            DatabaseMetaData metaData = conn.getMetaData();

            ResultSet rs = metaData.getTables(

                    conn.getCatalog(),

                    conn.getSchema(),

                    "%",

                    new String[]{"TABLE"}

            );


            while (rs.next()) {

                Map<String, Object> table = new HashMap<>();

                table.put("name", rs.getString("TABLE_NAME"));

                table.put("comment", rs.getString("REMARKS"));

                table.put("type", "table");

                table.put("schema", rs.getString("TABLE_SCHEM"));

                table.put("catalog", rs.getString("TABLE_CAT"));

                tables.add(table);

            }

        }

        return tables;

    }


    // MongoDB集合获取

    private List<Map<String, Object>> getMongoCollections(DataSource dataSource) {

        List<Map<String, Object>> collections = new ArrayList<>();

        MongoClient mongoClient = null;

        try {

            mongoClient = createMongoConnection(dataSource);

            MongoDatabase db = mongoClient.getDatabase(dataSource.getDbName());


            for (String collectionName : db.listCollectionNames()) {

                Map<String, Object> collection = new HashMap<>();

                collection.put("name", collectionName);

                collection.put("type", "collection");

                collections.add(collection);

            }


            return collections;

        } catch (Exception e) {

            log.error("获取MongoDB集合失败: {}", e.getMessage());

            throw new RuntimeException("获取集合失败: " + e.getMessage());

        } finally {

            if (mongoClient != null) {

                try {

                    mongoClient.close();

                } catch (Exception e) {

                    log.error("关闭MongoDB连接失败", e);

                }

            }

        }

    }


    // Redis键获取

    private List<Map<String, Object>> getRedisKeys(DataSource dataSource) {

        List<Map<String, Object>> keys = new ArrayList<>();

        Jedis jedis = null;

        try {

            // 端口号可选，默认6379

            int port = org.springframework.util.StringUtils.hasText(dataSource.getPort()) ?

                    Integer.parseInt(dataSource.getPort()) : 6379;


            jedis = new Jedis(dataSource.getHost(), port);


            // 密码可选

            if (org.springframework.util.StringUtils.hasText(dataSource.getPassword())) {

                jedis.auth(dataSource.getPassword());

            }


            // 数据库编号可选，默认0

            if (org.springframework.util.StringUtils.hasText(dataSource.getDbName())) {

                jedis.select(Integer.parseInt(dataSource.getDbName()));

            }


            Set<String> redisKeys = jedis.keys("*");

            for (String key : redisKeys) {

                Map<String, Object> keyInfo = new HashMap<>();

                keyInfo.put("name", key);

                keyInfo.put("type", jedis.type(key));

                keys.add(keyInfo);

            }

            return keys;

        } catch (Exception e) {

            log.error("获取Redis键失败: {}", e.getMessage());

            throw new RuntimeException("获取键失败: " + e.getMessage());

        } finally {

            if (jedis != null) {

                try {

                    jedis.close();

                } catch (Exception e) {

                    log.error("关闭Redis连接失败", e);

                }

            }

        }

    }


    // Elasticsearch索引获取

    private List<Map<String, Object>> getElasticsearchIndices(DataSource dataSource) {

        List<Map<String, Object>> indices = new ArrayList<>();

        RestClient restClient = null;

        ElasticsearchClient client = null;

        try {

            // 端口号可选，默认9200

            int port = org.springframework.util.StringUtils.hasText(dataSource.getPort()) ?

                    Integer.parseInt(dataSource.getPort()) : 9200;


            HttpHost httpHost = new HttpHost(dataSource.getHost(), port, "http");

            RestClientBuilder builder = RestClient.builder(httpHost)

                    .setRequestConfigCallback(requestConfigBuilder ->

                            requestConfigBuilder

                                    .setConnectTimeout(5000)

                                    .setSocketTimeout(60000));


            // 如果有用户名密码，添加认证

            if (org.springframework.util.StringUtils.hasText(dataSource.getUsername()) &&

                    org.springframework.util.StringUtils.hasText(dataSource.getPassword())) {

                final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

                credentialsProvider.setCredentials(AuthScope.ANY,

                        new UsernamePasswordCredentials(dataSource.getUsername(), dataSource.getPassword()));

                builder.setHttpClientConfigCallback(httpClientBuilder ->

                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));

            }


            restClient = builder.build();


            // 创建客户端

            ElasticsearchTransport transport = new RestClientTransport(

                    restClient, new JacksonJsonpMapper());

            client = new ElasticsearchClient(transport);


            // 获取索引，使用显式类型声明替代 var

            co.elastic.clients.elasticsearch.indices.GetIndexResponse indexResponse =

                    client.indices().get(b -> b.index("*"));

            indexResponse.result().forEach((indexName, indexState) -> {

                Map<String, Object> indexInfo = new HashMap<>();

                indexInfo.put("name", indexName);

                indexInfo.put("type", "index");

                indexInfo.put("aliases", indexState.aliases().keySet());

                indices.add(indexInfo);

            });


            return indices;

        } catch (Exception e) {

            log.error("获取Elasticsearch索引失败: {}", e.getMessage());

            throw new RuntimeException("获取索引失败: " + e.getMessage());

        } finally {

            if (restClient != null) {

                try {

                    restClient.close();

                } catch (IOException e) {

                    log.error("关闭Elasticsearch连接失败", e);

                }

            }

        }

    }


    // 文件系统目录获取

    private List<Map<String, Object>> getHdfsDirectories(DataSource dataSource) {

        List<Map<String, Object>> directories = new ArrayList<>();

        try {

            Configuration conf = new Configuration();

            conf.set("fs.defaultFS", dataSource.getUrl());

            FileSystem fs = FileSystem.get(conf);


            FileStatus[] status = fs.listStatus(new Path("/"));

            for (FileStatus fileStatus : status) {

                Map<String, Object> directory = new HashMap<>();

                directory.put("name", fileStatus.getPath().getName());

                directory.put("type", fileStatus.isDirectory() ? "directory" : "file");

                directory.put("size", fileStatus.getLen());

                directory.put("modificationTime", fileStatus.getModificationTime());

                directories.add(directory);

            }


            return directories;

        } catch (Exception e) {

            log.error("获取HDFS目录失败", e);

            throw new RuntimeException("获取目录失败：" + e.getMessage());

        }

    }


    @Override
    public Connection getConnection(String sourceId) {
        try {
            DataSource dataSource = this.getById(sourceId);
            AbstractDataSourceHandler handler = DatabaseHandlerFactory.getHandler(dataSource);
            // 创建连接
            return handler.getConnection();
        } catch (Exception e) {
            log.error("获取数据库连接失败", e);
            throw new RuntimeException("获取数据库连接失败：" + e.getMessage());
        }
    }


    /**
     * 从URL中提取schema
     */

    private String extractSchema(String url, String defaultSchema) {

        try {

            // PostgreSQL格式: jdbc:postgresql://host:port/database?currentSchema=schema

            if (url.contains("currentSchema=")) {

                String[] parts = url.split("currentSchema=");

                if (parts.length > 1) {

                    String schema = parts[1];

                    if (schema.contains("&")) {

                        schema = schema.substring(0, schema.indexOf("&"));

                    }

                    return schema;

                }

            }

            // 达梦格式: jdbc:dm://host:port/schema

            else if (url.startsWith("jdbc:dm:")) {

                String[] parts = url.split("/");

                if (parts.length > 3) {

                    return parts[3];

                }

            }

        } catch (Exception e) {

            log.warn("提取schema失败，使用默认schema: " + defaultSchema + ", error: " + e.getMessage());

        }

        return defaultSchema;

    }


    private String getDriverClassName(String type) {

        // 根据数据库类型返回对应的驱动类名

        switch (type.toLowerCase()) {

            // 关系型数据库

            case "mysql":

                return MYSQL_DRIVER;

            case "oracle":

                return ORACLE_DRIVER;

            case "postgresql":

                return POSTGRESQL_DRIVER;

            case "sqlserver":

                return SQLSERVER_DRIVER;

            case "db2":

                return DB2_DRIVER;

            case "informix":

                return INFORMIX_DRIVER;

            case "mariadb":

                return MYSQL_DRIVER;  // MariaDB 使用 MySQL 驱动

            case "sybase":

                return "com.sybase.jdbc4.jdbc.SybDriver";

            case "h2":

                return "org.h2.Driver";


            // 大数据相关

            case "hive":

                return HIVE_DRIVER;

            case "hbase":

                return HBASE_PHOENIX_DRIVER;

            case "clickhouse":

                return CLICKHOUSE_DRIVER;

            case "impala":

                return IMPALA_DRIVER;

            case "doris":

                return MYSQL_DRIVER;  // Doris 使用 MySQL 驱动

            case "spark":

                return HIVE_DRIVER;   // Spark SQL 使用 Hive 驱动

            case "presto":

                return "com.facebook.presto.jdbc.PrestoDriver";

            case "starrocks":

                return STARROCKS_DRIVER;

            case "greenplum":

                return GREENPLUM_DRIVER;


            // 国产数据库

            case "dm":

            case "dm7":

            case "dm8":

                return DM7_DRIVER;

            case "gaussdb":

                return GAUSSDB_DRIVER;

            case "opengauss":

                return OPENGAUSS_DRIVER;

            case "kingbase":

                return KINGBASE_DRIVER;

            case "shentong":

                return "com.oscar.Driver";

            case "highgo":

                return "com.highgo.jdbc.Driver";

            case "gbase":

                return GBASE_DRIVER;

            case "uxdb":

                return UXDB_DRIVER;

            case "arterybase":

                return ARTERYBASE_DRIVER;

            case "petabase":

                return PETABASE_DRIVER;


            // NoSQL数据库

            case "mongodb":

                return "mongodb.jdbc.MongoDriver";

            case "redis":

                return "com.redis.jdbc.RedisDriver";

            case "elasticsearch":

                return "org.elasticsearch.xpack.sql.jdbc.EsDriver";

            case "cassandra":

                return "com.simba.cassandra.jdbc42.Driver";

            case "neo4j":

                return "org.neo4j.jdbc.Driver";


            // 云数据库

            case "aliyun-rds":

                return ALIYUN_RDS_DRIVER;

            case "maxcompute":

                return MAXCOMPUTE_DRIVER;

            case "huawei-rds":

                return HUAWEI_RDS_DRIVER;


            // 文件系统类型不需要JDBC驱动

            case "ftp":

            case "sftp":

            case "hdfs":

            case "minio":

            case "s3":

            case "oss":

            case "file":

                throw new RuntimeException("文件系统类型不支持获取表结构");


            default:

                throw new RuntimeException("不支持的数据库类型：" + type);

        }

    }


    @Override

    public void batchInsertData(String tableName, List<FileField> fields, List<Map<String, Object>> data) {

        if (data == null || data.isEmpty()) {

            return;

        }


        JdbcTemplate jdbcTemplate = getFileJdbcTemplate();

        StringBuilder sql = new StringBuilder();

        sql.append("INSERT INTO ").append(tableName).append(" (");


        // 构建字段列表

        for (int i = 0; i < fields.size(); i++) {

            sql.append(fields.get(i).getFieldName());

            if (i < fields.size() - 1) {

                sql.append(", ");

            }

        }


        sql.append(") VALUES (");


        // 构建参数占位符

        for (int i = 0; i < fields.size(); i++) {

            sql.append("?");

            if (i < fields.size() - 1) {

                sql.append(", ");

            }

        }

        sql.append(")");


        String insertSql = sql.toString();

        log.info("Batch insert SQL: " + insertSql);


        // 批量插入数据

        jdbcTemplate.batchUpdate(insertSql, new BatchPreparedStatementSetter() {

            @Override

            public void setValues(PreparedStatement ps, int i) throws SQLException {

                Map<String, Object> row = data.get(i);

                for (int j = 0; j < fields.size(); j++) {

                    FileField field = fields.get(j);

                    Object value = row.get(field.getFieldName());

                    if (value == null) {

                        ps.setNull(j + 1, getSqlType(field.getFieldType()));

                    } else {

                        ps.setObject(j + 1, convertValue(value, field.getFieldType()));

                    }

                }

            }


            @Override

            public int getBatchSize() {

                return data.size();

            }

        });

    }


    @Override

    public void createTable(String tableName, String tableComment, List<FileField> fields) {

        // 获取当前数据源类型

        String dbType = getCurrentDatabaseType();


        StringBuilder sql = new StringBuilder();


        // 根据数据库类型生成建表语句

        switch (dbType) {  // dbType 已经在 getCurrentDatabaseType 中标准化了

            case "mysql":

                generateMySqlCreateTable(sql, tableName, tableComment, fields);

                break;

            case "oracle":

                generateOracleCreateTable(sql, tableName, tableComment, fields);

                break;

            case "dm":

                createTableInDM(tableName, tableComment, fields);

                break;

            default:

                throw new RuntimeException("不支持的数据库类型：" + dbType);

        }

    }


    private void generateMySqlCreateTable(StringBuilder sql, String tableName, String tableComment, List<FileField> fields) {

        sql.append("CREATE TABLE ").append(tableName).append(" (");


        for (int i = 0; i < fields.size(); i++) {

            FileField field = fields.get(i);

            sql.append("`").append(field.getFieldName()).append("` ");


            // MySQL数据类型映射

            String fieldType = field.getFieldType().toUpperCase();

            switch (fieldType) {

                case "VARCHAR":

                case "VARCHAR2":

                    sql.append("VARCHAR");

                    break;

                case "NUMBER":

                    sql.append("DECIMAL");

                    break;

                default:

                    sql.append(fieldType);

            }


            // 字段长度

            if (field.getFieldLength() != null) {

                sql.append("(").append(field.getFieldLength()).append(")");

            }


            // 是否可空

            if (!field.getNullable()) {

                sql.append(" NOT NULL");

            }


            // 字段注释

            if (StringUtils.isNotBlank(field.getDescription())) {

                sql.append(" COMMENT '").append(field.getDescription()).append("'");

            }


            if (i < fields.size() - 1) {

                sql.append(", ");

            }

        }


        sql.append(")");


        // 表注释

        if (StringUtils.isNotBlank(tableComment)) {

            sql.append(" COMMENT='").append(tableComment).append("'");

        }

        JdbcTemplate jdbcTemplate = getFileJdbcTemplate();

        jdbcTemplate.execute(sql.toString());

    }


    private void generateOracleCreateTable(StringBuilder sql, String tableName, String tableComment, List<FileField> fields) {

        sql.append("CREATE TABLE \"").append(tableName).append("\" (");


        for (int i = 0; i < fields.size(); i++) {

            FileField field = fields.get(i);

            sql.append("\"").append(field.getFieldName()).append("\" ");


            // Oracle数据类型映射

            String fieldType = field.getFieldType().toUpperCase();

            switch (fieldType) {

                case "VARCHAR":

                    sql.append("VARCHAR2");

                    break;

                case "TEXT":

                    sql.append("CLOB");

                    break;

                case "DATETIME":

                    sql.append("DATE");

                    break;

                case "INT":

                case "INTEGER":

                    sql.append("NUMBER(10)");

                    break;

                case "DECIMAL":

                    sql.append("NUMBER");

                    break;

                default:

                    sql.append(fieldType);

            }


            // 字段长度

            if (field.getFieldLength() != null) {

                if (!"CLOB".equals(fieldType) && !"DATE".equals(fieldType)) {

                    sql.append("(").append(field.getFieldLength()).append(")");

                }

            }


            // 是否可空

            if (!field.getNullable()) {

                sql.append(" NOT NULL");

            }


            if (i < fields.size() - 1) {

                sql.append(", ");

            }

        }

        sql.append(")");


        // Oracle的表和字段注释需要单独添加

        if (StringUtils.isNotBlank(tableComment)) {

            sql.append(";\n");

            sql.append("COMMENT ON TABLE \"").append(tableName)

                    .append("\" IS '").append(tableComment).append("'");

        }


        // 添加字段注释

        for (FileField field : fields) {

            if (StringUtils.isNotBlank(field.getDescription())) {

                sql.append(";\n");

                sql.append("COMMENT ON COLUMN \"").append(tableName).append("\".\"")

                        .append(field.getFieldName()).append("\" IS '")

                        .append(field.getDescription()).append("'");

            }

        }

        JdbcTemplate jdbcTemplate = getFileJdbcTemplate();

        jdbcTemplate.execute(sql.toString());

    }


    private void createTableInDM(String tableName, String tableComment, List<FileField> fields) {
        // 获取JdbcTemplate
        JdbcTemplate jdbcTemplate = getFileJdbcTemplate();
        
        // 1. 先尝试删除已存在的表
        try {
            String dropTableSql = "DROP TABLE IF EXISTS " + tableName;
            jdbcTemplate.execute(dropTableSql);
            log.info("已删除表: {}", tableName);
        } catch (Exception e) {
            log.warn("尝试删除表失败: {}，错误: {}", tableName, e.getMessage());
            // 删除失败继续执行，因为表可能不存在
        }

        // 2. 构建创建表的SQL
        StringBuilder createTableSql = new StringBuilder();
        createTableSql.append("CREATE TABLE ").append(tableName).append(" (\n");

        for (int i = 0; i < fields.size(); i++) {
            FileField field = fields.get(i);
            createTableSql.append("    ").append(field.getFieldName()).append(" ");
            createTableSql.append(convertToDMType(field.getFieldType(), field.getFieldLength(), field.getPrecision(), field.getScale()));

            if (field.getNullable() != null && field.getNullable()) {
                createTableSql.append(" NOT NULL");
            }

            if (i < fields.size() - 1) {
                createTableSql.append(",\n");
            }
        }
        createTableSql.append("\n)");

        // 3. 执行创建表SQL
        try {
            jdbcTemplate.execute(createTableSql.toString());

            // 4. 构建并执行表注释SQL
            if (StringUtils.isNotBlank(tableComment)) {
                String tableCommentSql = String.format(
                        "COMMENT ON TABLE %s IS '%s'",
                        tableName,
                        tableComment.replace("'", "''")
                );
                jdbcTemplate.execute(tableCommentSql);
            }

            // 5. 为每个字段添加注释
            for (FileField field : fields) {
                if (StringUtils.isNotBlank(field.getDescription())) {
                    String fieldCommentSql = String.format(
                            "COMMENT ON COLUMN %s.%s IS '%s'",
                            tableName,
                            field.getFieldName(),
                            field.getDescription().replace("'", "''")
                    );
                    jdbcTemplate.execute(fieldCommentSql);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("在DM数据库中创建表失败: " + e.getMessage(), e);
        }
    }


    private String getCurrentDatabaseType() {

        if (fileDbDriver.equals(DM7_DRIVER) || fileDbDriver.equals(DM8_DRIVER)) {

            return "dm";

        } else if (fileDbDriver.equals(ORACLE_DRIVER)) {

            return "oracle";

        } else if (fileDbDriver.equals(MYSQL_DRIVER)) {

            return "mysql";

        } else {

            throw new RuntimeException("不支持的数据库类型: " + fileDbDriver);

        }

    }


    private String convertToDMType(String sourceType, Integer length, Integer precision, Integer scale) {

        sourceType = sourceType.toUpperCase();


        // 字符类型

        if (sourceType.contains("CHAR") || sourceType.contains("TEXT")) {

            if (sourceType.equals("CHAR")) {

                return "CHAR(" + (length != null ? length : 1) + ")";

            }

            if (sourceType.equals("VARCHAR") || sourceType.equals("VARCHAR2") || sourceType.contains("TEXT")) {

                return "VARCHAR(" + (length != null ? length : 255) + ")";

            }

            if (sourceType.equals("NCHAR")) {

                return "NCHAR(" + (length != null ? length : 1) + ")";

            }

            if (sourceType.equals("NVARCHAR") || sourceType.equals("NVARCHAR2")) {

                return "NVARCHAR(" + (length != null ? length : 255) + ")";

            }

        }


        // 数值类型

        if (sourceType.contains("INT")) {

            if (sourceType.equals("TINYINT")) {

                return "TINYINT";

            }

            if (sourceType.equals("SMALLINT")) {

                return "SMALLINT";

            }

            if (sourceType.equals("INT") || sourceType.equals("INTEGER")) {

                return "INTEGER";

            }

            if (sourceType.equals("BIGINT")) {

                return "BIGINT";

            }

        }


        if (sourceType.contains("DECIMAL") || sourceType.contains("NUMERIC")) {

            precision = precision != null ? precision : 18;

            scale = scale != null ? scale : 2;

            return "DECIMAL(" + precision + "," + scale + ")";

        }


        if (sourceType.contains("FLOAT")) {

            return "FLOAT";

        }


        if (sourceType.contains("DOUBLE")) {

            return "DOUBLE";

        }


        // 日期时间类型

        if (sourceType.contains("DATE")) {

            return "DATE";

        }


        if (sourceType.contains("TIME")) {

            if (sourceType.equals("TIME")) {

                return "TIME";

            }

            if (sourceType.equals("TIMESTAMP")) {

                return "TIMESTAMP";

            }

        }


        // 大对象类型

        if (sourceType.contains("BLOB")) {

            if (sourceType.equals("BLOB")) {

                return "BLOB";

            }

            if (sourceType.equals("CLOB")) {

                return "CLOB";

            }

            if (sourceType.equals("NCLOB")) {

                return "NCLOB";

            }

        }


        // 布尔类型

        if (sourceType.equals("BOOLEAN") || sourceType.equals("BOOL")) {

            return "BIT";

        }


        // 二进制类型

        if (sourceType.contains("BINARY")) {

            if (sourceType.equals("BINARY")) {

                return "BINARY(" + (length != null ? length : 1) + ")";

            }

            if (sourceType.equals("VARBINARY")) {

                return "VARBINARY(" + (length != null ? length : 255) + ")";

            }

        }


        // 其他类型

        if (sourceType.equals("REAL")) {

            return "REAL";

        }


        if (sourceType.equals("MONEY") || sourceType.equals("SMALLMONEY")) {

            return "DECIMAL(19,4)";

        }


        // 默认返回VARCHAR

        return "VARCHAR(255)";

    }


    @Override
    public boolean isTableExists(String tableName) {
        String dbType = getCurrentDatabaseType();
        JdbcTemplate jdbcTemplate = getFileJdbcTemplate();
        
        try {
            String sql;
            if ("dm".equals(dbType)) {
                // 针对DM数据库使用系统表查询
                sql = String.format("SELECT COUNT(*) FROM ALL_TABLES WHERE TABLE_NAME = '%s' AND OWNER = USER", 
                        tableName.toUpperCase());
                Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
                return count != null && count > 0;
            } else {
                // 其他数据库使用原来的方法
                sql = "SELECT 1 FROM " + tableName + " LIMIT 1";
                jdbcTemplate.queryForObject(sql, Integer.class);
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }


    @Override
    public TransactionStatus beginTransaction() {
        return transactionManager.getTransaction(new DefaultTransactionDefinition());
    }


    @Override
    public void commit(TransactionStatus status) {
        transactionManager.commit(status);
    }


    @Override
    public void rollback(TransactionStatus status) {
        transactionManager.rollback(status);
    }


    /**
     * 转换数据类型
     */
    private Object convertValue(Object value, String fieldType) {
        if (value == null) {
            return null;
        }
        String strValue = value.toString().trim();
        if (strValue.isEmpty()) {
            return null;
        }

        try {
            fieldType = fieldType.toUpperCase();
            // 整数类型
            if (fieldType.contains("INT")) {
                if (fieldType.equals("TINYINT")) {
                    return Byte.valueOf(strValue);
                }
                if (fieldType.equals("SMALLINT")) {
                    return Short.valueOf(strValue);
                }

                if (fieldType.equals("INT") || fieldType.equals("INTEGER")) {

                    return Integer.valueOf(strValue);

                }

                if (fieldType.equals("BIGINT")) {

                    return Long.valueOf(strValue);

                }

            }


            // 小数类型

            if (fieldType.equals("DECIMAL") || fieldType.equals("NUMERIC") ||

                    fieldType.equals("NUMBER") || fieldType.contains("MONEY")) {

                return new BigDecimal(strValue);

            }


            if (fieldType.equals("FLOAT") || fieldType.equals("REAL")) {

                return Float.valueOf(strValue);

            }


            if (fieldType.equals("DOUBLE")) {

                return Double.valueOf(strValue);

            }

            // 布尔类型

            if (fieldType.equals("BOOLEAN") || fieldType.equals("BOOL") || fieldType.equals("BIT")) {
                if (strValue.equalsIgnoreCase("true") || strValue.equals("1")) {
                    return true;
                }
                if (strValue.equalsIgnoreCase("false") || strValue.equals("0")) {
                    return false;
                }
                return Boolean.valueOf(strValue);
            }

            // 日期时间类型
            if (fieldType.equals("DATE")) {
                return parseDate(strValue);
            }

            if (fieldType.equals("TIME")) {
                return parseTime(strValue);
            }


            if (fieldType.equals("TIMESTAMP") || fieldType.equals("DATETIME")) {

                return parseDateTime(strValue);

            }


            // 二进制类型

            if (fieldType.contains("BINARY") || fieldType.equals("BLOB")) {

                if (value instanceof byte[]) {

                    return value;

                }

                return strValue.getBytes(StandardCharsets.UTF_8);

            }


            // 字符类型
            if (fieldType.contains("CHAR") || fieldType.contains("TEXT") ||
                    fieldType.equals("CLOB") || fieldType.equals("NCLOB")) {
                return strValue;
            }


            // 默认返回字符串
            return strValue;

        } catch (Exception e) {
            log.error("数据类型转换失败: value=" + value + ", type=" + fieldType + ", error=" + e.getMessage());
            throw new RuntimeException("数据类型转换失败: " + e.getMessage());
        }
    }

    private Date parseDate(String dateStr) throws ParseException {
        return DateUtils.parseDate(dateStr, DATE_PATTERNS);
    }


    private Time parseTime(String timeStr) throws ParseException {
        Date date = DateUtils.parseDate(timeStr, TIME_PATTERNS);
        return new Time(date.getTime());
    }


    private Timestamp parseDateTime(String dateTimeStr) throws ParseException {

        Date date = DateUtils.parseDate(dateTimeStr, DATETIME_PATTERNS);

        return new Timestamp(date.getTime());

    }


    private int getSqlType(String fieldType) {

        fieldType = fieldType.toUpperCase();

        switch (fieldType) {

            case "VARCHAR":

            case "VARCHAR2":

            case "CHAR":

                return Types.VARCHAR;

            case "INT":

            case "INTEGER":

                return Types.INTEGER;

            case "BIGINT":

                return Types.BIGINT;

            case "DECIMAL":

            case "NUMERIC":

                return Types.DECIMAL;

            case "DATE":

                return Types.DATE;

            case "TIMESTAMP":

                return Types.TIMESTAMP;

            case "BLOB":

                return Types.BLOB;

            case "CLOB":

                return Types.CLOB;

            default:

                return Types.VARCHAR;

        }

    }


    // Cassandra表获取

    private List<Map<String, Object>> getCassandraTables(DataSource dataSource) {

        List<Map<String, Object>> tables = new ArrayList<>();

        try {

            // TODO: 实现Cassandra表获取逻辑

            return tables;

        } catch (Exception e) {

            log.error("获取Cassandra表失败", e);

            throw new RuntimeException("获取表失败：" + e.getMessage());

        }

    }


    // 大数据表获取

    private List<Map<String, Object>> getBigDataTables(DataSource dataSource) {

        List<Map<String, Object>> tables = new ArrayList<>();

        try {

            // TODO: 根据不同的大数据类型实现相应的逻辑

            return tables;

        } catch (Exception e) {

            log.error("获取大数据表失败", e);

            throw new RuntimeException("获取表失败：" + e.getMessage());

        }

    }



    // OSS存储桶获取

    private List<Map<String, Object>> getOssBuckets(DataSource dataSource) {

        List<Map<String, Object>> buckets = new ArrayList<>();

        OSS ossClient = null;

        try {

            ossClient = new OSSClientBuilder().build(

                    dataSource.getEndpoint(),

                    dataSource.getAccessKey(),

                    dataSource.getSecretKey()

            );


            ossClient.listBuckets().forEach(bucket -> {

                Map<String, Object> bucketInfo = new HashMap<>();

                bucketInfo.put("name", bucket.getName());

                bucketInfo.put("type", "bucket");

                bucketInfo.put("location", bucket.getLocation());

                buckets.add(bucketInfo);

            });


            return buckets;

        } catch (Exception e) {

            log.error("获取OSS存储桶失败", e);

            throw new RuntimeException("获取存储桶失败：" + e.getMessage());

        } finally {

            if (ossClient != null) {

                ossClient.shutdown();

            }

        }

    }


    // MinIO存储桶获取

    private List<Map<String, Object>> getMinioBuckets(DataSource dataSource) {

        List<Map<String, Object>> buckets = new ArrayList<>();

        try {

            MinioClient minioClient = MinioClient.builder()

                    .endpoint(dataSource.getUrl())

                    .credentials(dataSource.getAccessKey(), dataSource.getSecretKey())

                    .build();


            minioClient.listBuckets().forEach(bucket -> {

                Map<String, Object> bucketInfo = new HashMap<>();

                bucketInfo.put("name", bucket.name());

                bucketInfo.put("type", "bucket");

                bucketInfo.put("creationDate", bucket.creationDate());

                buckets.add(bucketInfo);

            });


            return buckets;

        } catch (Exception e) {

            log.error("获取MinIO存储桶失败", e);

            throw new RuntimeException("获取存储桶失败：" + e.getMessage());

        }

    }


    // FTP目录获取

    private List<Map<String, Object>> getFtpDirectories(DataSource dataSource) {

        List<Map<String, Object>> directories = new ArrayList<>();

        FTPClient ftpClient = new FTPClient();

        try {

            ftpClient.connect(dataSource.getHost(), Integer.parseInt(dataSource.getPort()));

            ftpClient.login(dataSource.getUsername(), dataSource.getPassword());


            for (FTPFile file : ftpClient.listFiles()) {

                Map<String, Object> fileInfo = new HashMap<>();

                fileInfo.put("name", file.getName());

                fileInfo.put("type", file.isDirectory() ? "directory" : "file");

                fileInfo.put("size", file.getSize());

                fileInfo.put("modificationTime", file.getTimestamp().getTimeInMillis());

                directories.add(fileInfo);

            }


            return directories;

        } catch (Exception e) {

            log.error("获取FTP目录失败", e);

            throw new RuntimeException("获取目录失败：" + e.getMessage());

        } finally {

            try {

                ftpClient.disconnect();

            } catch (IOException e) {

                log.error("关闭FTP连接失败", e);

            }

        }

    }


    // SFTP目录获取

    private List<Map<String, Object>> getSftpDirectories(DataSource dataSource) {

        List<Map<String, Object>> directories = new ArrayList<>();

        Session session = null;

        ChannelSftp channelSftp = null;

        try {

            JSch jsch = new JSch();

            session = jsch.getSession(

                    dataSource.getUsername(),

                    dataSource.getHost(),

                    Integer.parseInt(dataSource.getPort())

            );

            session.setPassword(dataSource.getPassword());

            session.setConfig("StrictHostKeyChecking", "no");

            session.connect();


            channelSftp = (ChannelSftp) session.openChannel("sftp");

            channelSftp.connect();


            Vector<ChannelSftp.LsEntry> list = channelSftp.ls(".");

            for (ChannelSftp.LsEntry entry : list) {

                if (!entry.getFilename().equals(".") && !entry.getFilename().equals("..")) {

                    Map<String, Object> fileInfo = new HashMap<>();

                    fileInfo.put("name", entry.getFilename());

                    fileInfo.put("type", entry.getAttrs().isDir() ? "directory" : "file");

                    fileInfo.put("size", entry.getAttrs().getSize());

                    fileInfo.put("modificationTime", entry.getAttrs().getMTime() * 1000L);

                    fileInfo.put("permissions", entry.getAttrs().getPermissionsString());

                    directories.add(fileInfo);

                }

            }


            return directories;

        } catch (Exception e) {

            log.error("获取SFTP目录失败", e);

            throw new RuntimeException("获取目录失败：" + e.getMessage());

        } finally {

            if (channelSftp != null) {

                channelSftp.disconnect();

            }

            if (session != null) {

                session.disconnect();

            }

        }

    }


    // Redis连接测试

    private boolean testRedisConnection(DataSource dataSource) {

        Jedis jedis = null;

        try {

            // 端口号可选，默认6379

            int port = org.springframework.util.StringUtils.hasText(dataSource.getPort()) ?

                    Integer.parseInt(dataSource.getPort()) : 6379;


            jedis = new Jedis(dataSource.getHost(), port);


            // 密码可选

            if (org.springframework.util.StringUtils.hasText(dataSource.getPassword())) {

                jedis.auth(dataSource.getPassword());

            }


            // 数据库编号可选，默认0

            if (org.springframework.util.StringUtils.hasText(dataSource.getDbName())) {

                jedis.select(Integer.parseInt(dataSource.getDbName()));

            }


            String response = jedis.ping();

            return "PONG".equalsIgnoreCase(response);

        } catch (Exception e) {

            log.error("Redis连接测试失败: " + e.getMessage());

            return false;

        } finally {

            if (jedis != null) {

                try {

                    jedis.close();

                } catch (Exception e) {

                    log.error("关闭Redis连接失败", e);

                }

            }

        }
    }


    private MongoClient createMongoConnection(DataSource dataSource) {
        try {
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(String.format(
                            "mongodb://%s:%s@%s:%s/%s",
                            dataSource.getUsername(),
                            dataSource.getPassword(),
                            dataSource.getHost(),
                            dataSource.getPort(),
                            dataSource.getDbName()
                    )))
                    .applyToClusterSettings(builder ->
                            builder.serverSelectionTimeout(5000, TimeUnit.MILLISECONDS)
                    )
                    .build();
            return MongoClients.create(settings);
        } catch (Exception e) {
            log.error("创建MongoDB连接失败: " + e.getMessage());
            throw new RuntimeException("创建MongoDB连接失败: " + e.getMessage());
        }
    }

    @Override
    public R<List<ColumnInfo>> getTableColumns(String dataSourceId, String tableName) {
        try {
            // 修改为使用 getById 方法
            DataSource dataSource = getById(dataSourceId);
            if (dataSource == null) {
                return R.fail("数据源不存在");
            }
            // 获取数据源连接
            AbstractDatabaseHandler handler = DatabaseHandlerFactory.getDatabaseHandler(dataSource);
            List<ColumnDefinition> tableColumns = handler.getTableColumns(dataSource.getDbName(), tableName);
            List<String> primaryKeys = handler.getPrimaryKeys(tableName);

            // 将ColumnDefinition对象转换为ColumnInfo对象
            List<ColumnInfo> columns = new ArrayList<>();
            for (ColumnDefinition columnDef : tableColumns) {
                ColumnInfo column = new ColumnInfo();

                // 设置列属性
                column.setName(columnDef.getName());
                column.setType(columnDef.getType());
                column.setComment(columnDef.getComment());
                column.setLength(columnDef.getLength());
                column.setPrecision(columnDef.getPrecision());
                column.setScale(columnDef.getScale());
                column.setNotNull(!columnDef.isNullable());
                column.setPrimaryKey(columnDef.isPrimaryKey() || primaryKeys.contains(columnDef.getName()));
                column.setDefaultValue(columnDef.getDefaultValue());
                column.setAutoIncrement(columnDef.isAutoIncrement());

                // 处理额外属性
                Map<String, String> extraProps = columnDef.getExtraProperties();
                if (extraProps != null) {
                    // 这里可以处理一些特殊的额外属性，如果需要的话
                    if (extraProps.containsKey("position")) {
                        // Position property is not available in ColumnInfo class
                        // column.setPosition(Integer.parseInt(extraProps.get("position")));
                    }
                }

                columns.add(column);
            }

            return R.ok(columns);
        } catch (Exception e) {
            log.error("获取表字段信息失败", e);
            return R.fail("获取表字段信息失败：" + e.getMessage());
        }
    }


    @Override
    public List<Map<String, Object>> executeQuery(String url, String username, String password, String sql) {
        List<Map<String, Object>> result = new ArrayList<>();

        try (Connection conn = getConnection(url, username, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                result.add(row);
            }

        } catch (Exception e) {
            log.error("Execute query failed", e);
            throw new RuntimeException("Execute query failed: " + e.getMessage());
        }

        return result;
    }

    public Connection getConnection(String url, String username, String password) throws SQLException {
        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            log.error("获取数据库连接失败: {}", e.getMessage(), e);
            throw e;
        }
    }



    private List<Map<String, Object>> filterFileSystemEntries(List<Map<String, Object>> entries, String path, String pattern) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> entry : entries) {
            String entryPath = (String) entry.get("path");
            String name = (String) entry.get("name");
            if (entryPath != null && entryPath.startsWith(path) &&
                    (StringUtils.isEmpty(pattern) || (name != null && name.contains(pattern)))) {
                result.add(entry);
            }
        }
        return result;
    }

    private List<Map<String, Object>> getSimulatedS3Objects(DataSource dataSource, String path, String pattern) {
        List<Map<String, Object>> result = new ArrayList<>();
        // 模拟S3对象列表
        String[] objectKeys = {
                "data/users.csv", "data/orders.json", "logs/app.log",
                "logs/error.log", "backups/db-backup-2023.sql", "images/products/"
        };

        for (String key : objectKeys) {
            if (key.startsWith(path.startsWith("/") ? path.substring(1) : path) &&
                    (StringUtils.isEmpty(pattern) || key.contains(pattern))) {
                Map<String, Object> objectInfo = new HashMap<>();
                String name = key.contains("/") ? key.substring(key.lastIndexOf('/') + 1) : key;
                objectInfo.put("id", key);
                objectInfo.put("name", name);
                objectInfo.put("path", key);
                objectInfo.put("type", key.endsWith("/") ? "directory" : "file");
                objectInfo.put("size", key.endsWith("/") ? 0 : new Random().nextInt(10000000));
                objectInfo.put("lastModified", new Date());
                result.add(objectInfo);
            }
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getDataSourceMetadata(DataSource dataSource, String type, String pattern) {
        if (dataSource == null) {
            log.error("数据源对象为空");
            return new ArrayList<>();
        }
        
        try {
            AbstractDataSourceHandler handler = DatabaseHandlerFactory.getHandler(dataSource);
            if (handler == null) {
                log.error("无法获取数据源处理器，数据源类型：{}", dataSource.getType());
                return new ArrayList<>();
            }
            
            // 处理特殊情况
            if (type != null) {
                // 数据库表/集合
                if ("tables".equals(type) || "collections".equals(type)) {
                    return handler.getMetadataList(pattern);
                }
                
                // 消息队列主题
                if ("topics".equals(type) && handler instanceof AbstractMessageQueueHandler) {
                    return ((AbstractMessageQueueHandler) handler).getTopics(pattern);
                }
                
                // 文件系统路径
                if ("files".equals(type) && handler instanceof AbstractFileSystemHandler) {
                    return ((AbstractFileSystemHandler) handler).listFiles(pattern != null ? pattern : "/", null);
                }
                
                // 数据库
                if ("databases".equals(type) && handler instanceof AbstractNoSQLHandler) {
                    return ((AbstractNoSQLHandler) handler).getDatabases();
                }
                
                // 索引
                if ("indexes".equals(type) && handler instanceof AbstractNoSQLHandler) {
                    return ((AbstractNoSQLHandler) handler).getIndexes(pattern);
                }
            }
            
            // 默认返回数据源的元数据列表
            return handler.getMetadataList(pattern);
        } catch (Exception e) {
            log.error("获取数据源元数据失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取数据源处理器
     * 根据数据源类型返回相应的处理器实例
     *
     * @param dataSource 数据源对象
     * @return 数据源处理器
     */
    private AbstractDataSourceHandler getHandler(DataSource dataSource) {
        try {
            if (dataSource == null ) {
                return null;
            }
            return DatabaseHandlerFactory.getHandler(dataSource);
        } catch (Exception e) {
            log.error("获取数据源处理器失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取特定类型的数据源处理器
     *
     * @param dataSource   数据源对象
     * @param handlerClass 处理器类型
     * @param <T>          处理器类型
     * @return 数据源处理器
     */
    @SuppressWarnings("unchecked")
    private <T extends AbstractDataSourceHandler> T getTypedDataSourceHandler(DataSource dataSource, Class<T> handlerClass) {
        AbstractDataSourceHandler handler = getHandler(dataSource);
        if (handler != null && handlerClass.isAssignableFrom(handler.getClass())) {
            return (T) handler;
        }
        log.error("数据源类型不匹配: 需要 {}, 实际类型 {}", handlerClass.getSimpleName(),
                handler != null ? handler.getClass().getSimpleName() : "null");
        return null;
    }


    /**
     * 测试数据源连接
     * 使用对应类型的处理器进行连接测试
     */
    @Override
    public boolean testConnection(DataSource dataSource) {
        try {
            AbstractDataSourceHandler handler = getHandler(dataSource);
            if (handler == null) {
                log.error("无法获取数据源处理器，数据源类型：{}", dataSource.getType());
                return false;
            }

            return handler.testConnection();
        } catch (Exception e) {
            log.error("测试数据源连接失败: {}", e.getMessage(), e);
            return false;
        }
    }



}


