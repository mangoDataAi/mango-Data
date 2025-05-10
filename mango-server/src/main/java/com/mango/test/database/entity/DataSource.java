package com.mango.test.database.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mango.test.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Properties;
import java.util.Map;

/**
 * 数据源实体类
 * 用于存储连接各种数据库和文件系统的配置信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("sys_datasource")
public class DataSource extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据源名称
     */
    private String name;
    
    /**
     * 数据源类型，如MySQL、Oracle、ElasticSearch等
     * 支持的类型包括：
     * - 关系型数据库: mysql, oracle, postgresql, sqlserver, db2, mariadb, sybase, h2, dm, gaussdb, opengauss, kingbase, shentong, highgo, gbase
     * - 大数据数据库: hive, hbase, clickhouse, doris, starrocks, spark, presto, trino, impala, kylin, tidb, oceanbase
     * - 时序数据库: influxdb, tdengine, opentsdb, timescaledb
     * - 图数据库: neo4j, nebula, hugegraph, janusgraph
     * - NoSQL数据库: mongodb, redis, elasticsearch, cassandra, couchdb, couchbase
     * - 文件服务器: ftp, sftp, hdfs, minio, s3
     * - 消息队列: kafka, rabbitmq, rocketmq, activemq, pulsar
     */
    private String type;
    
    /**
     * 数据库名称
     */
    private String dbName;
    
    /**
     * 数据库连接URL
     */
    private String url;
    
    /**
     * 数据库主机地址
     */
    private String host;
    
    /**
     * 数据库端口号
     */
    private String port;
    
    /**
     * 数据库用户名
     */
    private String username;
    
    /**
     * 数据库密码
     */
    private String password;
    
    /**
     * 连接池初始化大小
     */
    private Integer initialSize;
    
    /**
     * 连接池最大活跃连接数
     */
    private Integer maxActive;
    
    /**
     * 连接池最小空闲连接数
     */
    private Integer minIdle;
    
    /**
     * 连接池获取连接等待超时时间(毫秒)
     */
    private Integer maxWait;
    
    /**
     * 验证查询SQL
     */
    private String validationQuery;
    
    /**
     * 申请连接时执行validationQuery检测连接是否有效
     */
    private Boolean testOnBorrow;
    
    /**
     * 归还连接时执行validationQuery检测连接是否有效
     */
    private Boolean testOnReturn;
    
    /**
     * 空闲时执行validationQuery检测连接是否有效
     */
    private Boolean testWhileIdle;
    
    /**
     * 数据源状态: true-启用, false-禁用
     */
    private Boolean status;

    // 文件系统相关字段
    /**
     * 本地文件系统路径
     */
    private String path;
    
    /**
     * 云存储桶名称 (如S3, OSS等)
     */
    private String bucket;
    
    /**
     * 访问密钥标识
     */
    private String accessKey;
    
    /**
     * 访问密钥密码
     */
    private String secretKey;
    
    /**
     * 云服务区域
     */
    private String region;
    
    /**
     * 服务终端节点
     */
    private String endpoint;
    
    /**
     * HDFS命名空间
     */
    private String namenode;

    /**
     * 额外的连接属性
     */
    private Properties properties;

    /**
     * 连接类型: SID或SERVICE_NAME (主要用于Oracle连接)
     */
    private String connectionType;
    
    // 消息队列特有字段
    /**
     * 消息队列群组ID (Kafka, RocketMQ)
     */
    @TableField(exist = false)
    private String groupId;
    
    /**
     * 消息队列集群列表
     */
    @TableField(exist = false)
    private String bootstrapServers;
    
    /**
     * Flink主机地址
     */
    @TableField(exist = false)
    private String flinkHost;

    /**
     * Flink端口
     */
    @TableField(exist = false)
    private String flinkPort;

    /**
     * Flink安装目录
     */
    @TableField(exist = false)
    private String flinkHome;

    /**
     * 获取主机地址，如果未设置则默认返回localhost
     * @return 主机地址
     */
    public String getHost() {
        return StringUtils.hasText(host) ? host : "localhost";
    }

    /**
     * 判断是否为消息队列类型数据源
     * @return 是否为消息队列类型
     */
    public boolean isMessageQueue() {
        if (type == null) {
            return false;
        }
        return type.toLowerCase().equals("kafka") || 
               type.toLowerCase().equals("rabbitmq") || 
               type.toLowerCase().equals("rocketmq") || 
               type.toLowerCase().equals("activemq") || 
               type.toLowerCase().equals("pulsar") ||
               type.toLowerCase().startsWith("mq_");
    }
    
    /**
     * 判断是否为NoSQL类型数据源
     * @return 是否为NoSQL类型
     */
    public boolean isNoSQL() {
        if (type == null) {
            return false;
        }
        return type.toLowerCase().equals("mongodb") || 
               type.toLowerCase().equals("redis") || 
               type.toLowerCase().equals("elasticsearch") || 
               type.toLowerCase().equals("cassandra") || 
               type.toLowerCase().equals("couchbase") || 
               type.toLowerCase().equals("couchdb") ||
               type.toLowerCase().startsWith("nosql_");
    }
    
    /**
     * 判断是否为图数据库类型数据源
     * @return 是否为图数据库类型
     */
    public boolean isGraphDatabase() {
        if (type == null) {
            return false;
        }
        return type.toLowerCase().equals("neo4j") || 
               type.toLowerCase().equals("nebula") || 
               type.toLowerCase().equals("hugegraph") || 
               type.toLowerCase().equals("janusgraph") ||
               type.toLowerCase().startsWith("graph_");
    }
    
    /**
     * 判断是否为时序数据库类型数据源
     * @return 是否为时序数据库类型
     */
    public boolean isTimeSeriesDatabase() {
        if (type == null) {
            return false;
        }
        return type.toLowerCase().equals("influxdb") || 
               type.toLowerCase().equals("tdengine") || 
               type.toLowerCase().equals("opentsdb") ||
               type.toLowerCase().startsWith("ts_");
    }
    
    /**
     * 判断是否为文件系统类型数据源
     * @return 是否为文件系统类型
     */
    public boolean isFileSystem() {
        if (type == null) {
            return false;
        }
        return type.toLowerCase().equals("ftp") || 
               type.toLowerCase().equals("sftp") || 
               type.toLowerCase().equals("hdfs") || 
               type.toLowerCase().equals("s3") || 
               type.toLowerCase().equals("oss") || 
               type.toLowerCase().equals("minio") ||
               type.toLowerCase().startsWith("fs_");
    }

    /**
     * 获取端口
     * 如果未设置端口，则根据数据源类型返回默认端口
     * @return 端口
     */
    public String getPort() {
        if (port != null && !port.isEmpty()) {
            return port;
        }
        
        // 根据数据源类型返回默认端口
        if (type == null) {
            return "";
        }
        
        switch (type.toLowerCase()) {
            case "mysql":
            case "mariadb":
                return "3306";
            case "postgresql":
                return "5432";
            case "oracle":
                return "1521";
            case "sqlserver":
                return "1433";
            case "db2":
                return "50000";
            case "mongodb":
                return "27017";
            case "redis":
                return "6379";
            case "elasticsearch":
                return "9200";
            case "cassandra":
                return "9042";
            case "neo4j":
                return "7687";
            case "influxdb":
                return "8086";
            case "tdengine":
                return "6041";
            case "clickhouse":
                return "8123";
            case "hive":
                return "10000";
            case "kafka":
                return "9092";
            case "rabbitmq":
                return "5672";
            case "rocketmq":
                return "9876";
            case "ftp":
                return "21";
            case "sftp":
                return "22";
            case "hdfs":
                return "9000";
            default:
                return "";
        }
    }

    /**
     * 判断是否为关系型数据库
     * @return 是否为关系型数据库
     */
    public boolean isRelationalDatabase() {
        if (this.type == null) {
            return false;
        }
        
        String type = this.type.toLowerCase();
        return type.equals("mysql") || type.equals("oracle") || type.equals("postgresql") || 
               type.equals("sqlserver") || type.equals("db2") || type.equals("mariadb") || 
               type.equals("sybase") || type.equals("h2") || type.equals("dm7") || type.equals("dm8") || 
               type.equals("gaussdb") || type.equals("opengauss") || type.equals("kingbase") || 
               type.equals("shentong") || type.equals("highgo") || type.equals("gbase") || 
               type.equals("uxdb") || type.equals("tidb") || type.equals("oceanbase");
    }

    /**
     * 获取数据库驱动类名
     * @return 数据库驱动类名
     */
    public String getDriverClassName() {
        if (this.type == null) {
            return null;
        }
        
        switch (this.type.toLowerCase()) {
            case "mysql":
            case "mariadb":
                return "com.mysql.cj.jdbc.Driver";
            case "postgresql":
            case "postgres":
                return "org.postgresql.Driver";
            case "oracle":
                return "oracle.jdbc.OracleDriver";
            case "sqlserver":
                return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            case "db2":
                return "com.ibm.db2.jcc.DB2Driver";
            case "dm7":
            case "dm8":
                return "dm.jdbc.driver.DmDriver";
            case "gaussdb":
            case "opengauss":
                return "org.opengauss.Driver";
            case "kingbase":
                return "com.kingbase8.Driver";
            case "clickhouse":
                return "com.clickhouse.jdbc.ClickHouseDriver";
            case "doris":
            case "starrocks":
                return "com.mysql.cj.jdbc.Driver";
            case "hive":
                return "org.apache.hive.jdbc.HiveDriver";
            case "snowflake":
                return "net.snowflake.client.jdbc.SnowflakeDriver";
            default:
                return null;
        }
    }
    
    /**
     * 获取数据库名称
     * @return 数据库名称
     */
    public String getDatabase() {
        return this.dbName;
    }

    /**
     * 获取数据库Schema
     * @return 数据库Schema名
     */
    public String getSchema() {
        return this.dbName;
    }
    
    /**
     * 获取Snowflake的仓库
     * @return Snowflake仓库名
     */
    public String getWarehouse() {
        // 优先使用属性中存储的值
        if (properties != null && properties.containsKey("warehouse")) {
            return properties.getProperty("warehouse");
        }
        // 否则返回默认值
        return "COMPUTE_WH";
    }
    
    /**
     * 获取Snowflake的角色
     * @return Snowflake角色名
     */
    public String getRole() {
        // 优先使用属性中存储的值
        if (properties != null && properties.containsKey("role")) {
            return properties.getProperty("role");
        }
        // 否则返回默认值
        return "ACCOUNTADMIN";
    }
    
    /**
     * 获取数据源配置信息
     * @return 配置信息Map
     */
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new java.util.HashMap<>();
        
        // 添加基本连接信息
        config.put("url", this.url);
        config.put("host", this.getHost());
        config.put("port", this.getPort());
        config.put("username", this.username);
        config.put("password", this.password);
        config.put("database", this.getDatabase());
        
        // 添加特定数据库类型的属性
        if (this.type != null) {
            config.put("type", this.type);
            
            // Snowflake特有配置
            if ("SNOWFLAKE".equalsIgnoreCase(this.type)) {
                config.put("account", this.host);
                config.put("user", this.username);
                config.put("schema", this.getSchema());
                config.put("warehouse", this.getWarehouse());
                config.put("role", this.getRole());
            }
            
            // 文件系统特有配置
            if (isFileSystem()) {
                config.put("path", this.path);
                config.put("bucket", this.bucket);
                config.put("accessKey", this.accessKey);
                config.put("secretKey", this.secretKey);
                config.put("region", this.region);
                config.put("endpoint", this.endpoint);
            }
            
            // 消息队列特有配置
            if (isMessageQueue()) {
                config.put("groupId", this.groupId);
                config.put("bootstrapServers", this.bootstrapServers);
            }
        }
        
        // 添加属性配置
        if (this.properties != null) {
            for (Object key : this.properties.keySet()) {
                config.put(key.toString(), this.properties.get(key));
            }
        }
        
        return config;
    }

    /**
     * 获取属性值，如果不存在则返回默认值
     * @param key 属性键
     * @param defaultValue 默认值
     * @return 属性值或默认值
     */
    public String getProperty(String key, String defaultValue) {
        if (properties != null && properties.containsKey(key)) {
            return properties.getProperty(key);
        }
        return defaultValue;
    }
    
    /**
     * 获取属性值
     * @param key 属性键
     * @return 属性值或null
     */
    public String getProperty(String key) {
        if (properties != null && properties.containsKey(key)) {
            return properties.getProperty(key);
        }
        return null;
    }

    /**
     * 获取Flink主机地址
     * @return Flink主机地址
     */
    public String getFlinkHost() {
        return this.flinkHost;
    }

    /**
     * 获取Flink端口
     * @return Flink端口
     */
    public String getFlinkPort() {
        return this.flinkPort;
    }

    /**
     * 获取Flink安装目录
     * @return Flink安装目录
     */
    public String getFlinkHome() {
        return this.flinkHome;
    }
}
