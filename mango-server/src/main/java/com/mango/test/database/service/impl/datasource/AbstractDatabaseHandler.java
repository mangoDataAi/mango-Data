package com.mango.test.database.service.impl.datasource;

import com.mango.test.database.entity.ColumnDefinition;
import com.mango.test.database.entity.DataSource;
import com.mango.test.database.entity.TableDefinition;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mango.test.database.service.impl.datasource.handlers.*;
import com.mango.test.constant.DatabaseTypes;

@Slf4j
public abstract class AbstractDatabaseHandler extends AbstractDataSourceHandler {
    protected static final Logger log = LoggerFactory.getLogger(AbstractDatabaseHandler.class);

    public AbstractDatabaseHandler(DataSource dataSource) {
        super(dataSource);
    }

    /**
     * 获取数据库连接
     */
    @Override
    protected Object createNativeClient() throws Exception {
        Connection conn = DriverManager.getConnection(
                getJdbcUrl(),
                dataSource.getUsername(),
                dataSource.getPassword()
        );

        String schema = getSchema();
        if (schema != null && !schema.isEmpty()) {
            setSchema(conn, schema);
        }

        return conn;
    }
    
    /**
     * 执行SQL查询
     */
    @Override
    public List<Map<String, Object>> executeQuery(String query) throws Exception {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(
                getJdbcUrl(), dataSource.getUsername(), dataSource.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
             
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                results.add(row);
            }
        }
        
        return results;
    }
    
    /**
     * 执行SQL更新操作
     */
    @Override
    public int executeUpdate(String sql) throws Exception {
        try (Connection conn = DriverManager.getConnection(
                getJdbcUrl(), dataSource.getUsername(), dataSource.getPassword());
             Statement stmt = conn.createStatement()) {
            return stmt.executeUpdate(sql);
        }
    }

    /**
     * 获取元数据列表，默认返回表列表
     * 实现AbstractDataSourceHandler的方法
     */
    @Override
    public List<Map<String, Object>> getMetadataList(String pattern) {
        try {
            List<String> tables = listTables();
            List<Map<String, Object>> result = new ArrayList<>();

            for (String table : tables) {
                if (pattern == null || pattern.isEmpty() || table.contains(pattern)) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", table);
                    map.put("name", table);
                    map.put("type", "table");
                    result.add(map);
                }
            }

            return result;
        } catch (Exception e) {
            log.error("获取表列表失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 测试连接
     * 实现AbstractDataSourceHandler的方法
     */
    @Override
    public boolean testConnection() {
        try {
            Connection conn = getConnection();
            if (conn != null) {
                conn.close();
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("测试连接失败", e);
            return false;
        }
    }

    /**
     * 关闭连接
     * 实现AbstractDataSourceHandler的方法
     */
    public void close() {
        // 默认实现为空，因为getConnection方法在使用后会关闭连接
        log.info("关闭数据库连接");
    }

    /**
     * 获取JDBC URL
     */
    public abstract String getJdbcUrl();

    /**
     * 获取Schema
     */
    public abstract String getSchema();

    /**
     * 设置Schema
     */
    public abstract void setSchema(Connection conn, String schema) throws Exception;

    /**
     * 获取驱动类名
     */
    public abstract String getDriverClassName();

    /**
     * 获取默认端口
     */
    public abstract String getDefaultPort();

    /**
     * 获取默认Schema
     */
    public abstract String getDefaultSchema();

    /**
     * 获取验证查询语句
     */
    public abstract String getValidationQuery();

    /**
     * 获取引号字符串
     */
    public abstract String getQuoteString();

    /**
     * 获取系统数据库列表
     */
    public abstract List<String> getSystemDatabases();

    /**
     * 包装值
     */
    public abstract String wrapValue(Object value);

    /**
     * 包装标识符
     */
    public abstract String wrapIdentifier(String identifier);

    /**
     * 生成分页SQL
     */
    public abstract String generatePageSql(String sql, int offset, int limit);

    /**
     * 生成LIMIT子句
     *
     * @param offset 起始位置
     * @param limit  返回记录的最大数量
     * @return 格式化后的LIMIT子句
     */
    public abstract String getLimitClause(long offset, int limit);

    /**
     * 生成统计SQL
     */
    public abstract String generateCountSql(String sql);

    /**
     * 是否支持批量更新
     */
    public abstract boolean supportsBatchUpdates();

    /**
     * 是否支持获取生成的键
     */
    public abstract boolean supportsGetGeneratedKeys();

    /**
     * 是否支持事务
     */
    public abstract boolean supportsTransactions();

    /**
     * 获取数据库产品名称
     */
    public abstract String getDatabaseProductName();

    /**
     * 获取表是否存在的SQL
     */
    public abstract String getTableExistsSql(String tableName);

    /**
     * 获取删除表的SQL
     */
    public abstract String getDropTableSql(String tableName);

    /**
     * 获取清空表的SQL
     */
    public abstract String getTruncateTableSql(String tableName);

    /**
     * 获取添加列的SQL
     */
    public abstract String getAddColumnSql(String tableName, String columnDefinition);

    /**
     * 获取修改列的SQL
     */
    public abstract String getModifyColumnSql(String tableName, String columnName, String newDefinition);

    /**
     * 获取删除列的SQL
     */
    public abstract String getDropColumnSql(String tableName, String columnName);

    /**
     * 获取重命名表的SQL
     */
    public abstract String getRenameTableSql(String oldTableName, String newTableName);

    /**
     * 获取显示建表语句的SQL
     */
    public abstract String getShowCreateTableSql(String tableName);

    /**
     * 获取显示表列表的SQL
     */
    public abstract String getShowTablesSql();

    /**
     * 获取显示列信息的SQL
     */
    public abstract String getShowColumnsSql(String tableName);

    /**
     * 获取默认字符串字段长度
     */
    public abstract int getDefaultVarcharLength();

    /**
     * 获取最大字符串字段长度
     */
    public abstract int getMaxVarcharLength();

    /**
     * 获取默认文本字段类型
     */
    public abstract String getDefaultTextType();

    /**
     * 获取默认整数类型
     */
    public abstract String getDefaultIntegerType();

    /**
     * 获取默认小数类型
     */
    public abstract String getDefaultDecimalType();

    /**
     * 获取默认日期类型
     */
    public abstract String getDefaultDateType();

    /**
     * 获取默认时间类型
     */
    public abstract String getDefaultTimeType();

    /**
     * 获取默认日期时间类型
     */
    public abstract String getDefaultDateTimeType();

    /**
     * 获取默认布尔类型
     */
    public abstract String getDefaultBooleanType();

    /**
     * 获取默认二进制类型
     */
    public abstract String getDefaultBlobType();

    /**
     * 将Java类型转换为数据库类型
     */
    public abstract String mapJavaTypeToDbType(Class<?> javaType);

    /**
     * 将数据库类型转换为Java类型
     */
    public abstract Class<?> mapDbTypeToJavaType(String dbType);

    /**
     * 获取字段类型映射
     */
    public abstract Map<String, String> getTypeMapping();

    /**
     * 获取默认字段长度映射
     */
    public abstract Map<String, Integer> getDefaultLengthMapping();

    /**
     * 检查字段长度是否有效
     */
    public abstract boolean isValidFieldLength(String type, int length);

    /**
     * 格式化字段定义
     */
    public abstract String formatFieldDefinition(String name, String type, Integer length, Integer precision, Integer scale,
                                                 boolean nullable, String defaultValue, String comment);

    /**
     * 将其他数据库类型转换为当前数据库类型
     */
    public abstract String convertFromOtherDbType(String sourceType, String sourceDbType);

    /**
     * 获取通用类型映射
     */
    public abstract Map<String, Map<String, String>> getCommonTypeMapping();

    /**
     * 创建表
     */
    public abstract String getCreateTableSql(String tableName, List<ColumnDefinition> columns, String tableComment, String engine, String charset, String collate);

    /**
     * 增加表注释
     */
    public abstract String getAddTableCommentSql(String tableName, String comment);

    /**
     * 修改表注释
     */
    public abstract String getModifyTableCommentSql(String tableName, String comment);

    /**
     * 增加字段
     */
    public abstract String getAddColumnSql(String tableName, ColumnDefinition column, String afterColumn);

    /**
     * 增加字段注释
     */
    public abstract String getAddColumnCommentSql(String tableName, String columnName, String comment);

    /**
     * 修改字段注释
     */
    public abstract String getModifyColumnCommentSql(String tableName, String columnName, String comment);

    /**
     * 修改表字符集
     */
    public abstract String getAlterTableCharsetSql(String tableName, String charset, String collate);

    /**
     * 修改表引擎
     */
    public abstract String getAlterTableEngineSql(String tableName, String engine);

    /**
     * 获取表的所有索引
     */
    public abstract String getShowIndexesSql(String tableName);

    /**
     * 添加索引
     */
    public abstract String getAddIndexSql(String tableName, String indexName, List<String> columns, boolean unique);

    /**
     * 获取删除索引的SQL
     */
    public String getDropIndexSql(String tableName, String indexName) {
        return String.format("DROP INDEX %s ON %s",
                escapeIdentifier(indexName),
                escapeIdentifier(tableName)
        );
    }

    /**
     * 获取创建索引的SQL
     */
    public String getCreateIndexSql(String tableName, String indexName, String[] columns) {
        return String.format("CREATE INDEX %s ON %s (%s)",
                escapeIdentifier(indexName),
                escapeIdentifier(tableName),
                String.join(", ", columns)
        );
    }

    /**
     * 转义标识符
     */
    public String escapeIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
        return getQuoteString() + identifier + getQuoteString();
    }


    /**
     * 将其他数据库的建表语句转换为当前数据库的建表语句
     *
     * @param sourceSql    源数据库的建表语句
     * @param sourceDbType 源数据库类型
     * @return 转换后的建表语句
     */
    public abstract String convertCreateTableSql(String sourceSql, String sourceDbType);

    /**
     * 解析建表语句，提取表结构信息
     *
     * @param createTableSql 建表语句
     * @return 表结构信息，包含表名、字段定义等
     */
    public abstract TableDefinition parseCreateTableSql(String createTableSql);

    /**
     * 根据表结构信息生成建表语句
     *
     * @param tableDefinition 表结构信息
     * @return 建表语句
     */
    public abstract String generateCreateTableSql(TableDefinition tableDefinition);

    /**
     * 获取数据库下所有表信息
     *
     * @param database 数据库名称
     * @return 表信息列表，每个表包含名称、类型、注释等信息
     */
    public abstract List<TableDefinition> getAllTables(String database) throws Exception;

    /**
     * 获取指定表的所有字段信息
     *
     * @param database  数据库名称
     * @param tableName 表名
     * @return 字段信息列表，每个字段包含名称、类型、长度、是否可空、默认值、注释等信息
     */
    public abstract List<ColumnDefinition> getTableColumns(String database, String tableName) throws Exception;


    /**
     * 获取数据类型映射关系
     *
     * @return 数据类型映射关系
     */
    protected Map<String, Map<String, String>> getDataTypeMapping() {
        Map<String, Map<String, String>> mapping = new HashMap<>();

        // 标准类型映射（用于中间转换）
        Map<String, String> standardTypes = new HashMap<>();
        standardTypes.put("INTEGER", "INT");
        standardTypes.put("DECIMAL", "DECIMAL");
        standardTypes.put("VARCHAR", "VARCHAR");
        standardTypes.put("CHAR", "CHAR");
        standardTypes.put("TEXT", "TEXT");
        standardTypes.put("DATETIME", "DATETIME");
        standardTypes.put("TIMESTAMP", "TIMESTAMP");
        standardTypes.put("BLOB", "BLOB");
        standardTypes.put("BOOLEAN", "BOOLEAN");
        mapping.put("standard", standardTypes);

        // MySQL类型映射
        Map<String, String> mysqlMapping = new HashMap<>();
        mysqlMapping.put("NUMBER", "DECIMAL");
        mysqlMapping.put("VARCHAR2", "VARCHAR");
        mysqlMapping.put("NVARCHAR2", "VARCHAR");
        mysqlMapping.put("CHAR", "CHAR");
        mysqlMapping.put("NCHAR", "CHAR");
        mysqlMapping.put("DATE", "DATETIME");
        mysqlMapping.put("TIMESTAMP(6)", "TIMESTAMP");
        mysqlMapping.put("CLOB", "LONGTEXT");
        mysqlMapping.put("NCLOB", "LONGTEXT");
        mysqlMapping.put("BLOB", "LONGBLOB");
        mysqlMapping.put("RAW", "VARBINARY");
        mysqlMapping.put("LONG RAW", "LONGBLOB");
        mysqlMapping.put("BINARY_FLOAT", "FLOAT");
        mysqlMapping.put("BINARY_DOUBLE", "DOUBLE");
        mysqlMapping.put("BOOL", "TINYINT(1)");
        mysqlMapping.put("INT8", "BIGINT");
        mysqlMapping.put("SERIAL", "INT AUTO_INCREMENT");
        mysqlMapping.put("BIGSERIAL", "BIGINT AUTO_INCREMENT");
        mapping.put("mysql", mysqlMapping);

        // Oracle类型映射
        Map<String, String> oracleMapping = new HashMap<>();
        oracleMapping.put("TINYINT", "NUMBER(3)");
        oracleMapping.put("SMALLINT", "NUMBER(5)");
        oracleMapping.put("MEDIUMINT", "NUMBER(7)");
        oracleMapping.put("INT", "NUMBER(10)");
        oracleMapping.put("BIGINT", "NUMBER(19)");
        oracleMapping.put("FLOAT", "BINARY_FLOAT");
        oracleMapping.put("DOUBLE", "BINARY_DOUBLE");
        oracleMapping.put("DECIMAL", "NUMBER");
        oracleMapping.put("VARCHAR", "VARCHAR2");
        oracleMapping.put("TEXT", "CLOB");
        oracleMapping.put("LONGTEXT", "CLOB");
        oracleMapping.put("DATETIME", "TIMESTAMP");
        oracleMapping.put("TIMESTAMP", "TIMESTAMP");
        oracleMapping.put("VARBINARY", "RAW");
        oracleMapping.put("LONGBLOB", "LONG RAW");
        oracleMapping.put("BOOLEAN", "NUMBER(1)");
        mapping.put("oracle", oracleMapping);

        // DM类型映射
        Map<String, String> dmMapping = new HashMap<>();
        dmMapping.putAll(oracleMapping);  // DM兼容Oracle类型
        mapping.put("dm", dmMapping);

        // PostgreSQL类型映射
        Map<String, String> postgresqlMapping = new HashMap<>();
        postgresqlMapping.put("NUMBER", "NUMERIC");
        postgresqlMapping.put("VARCHAR2", "VARCHAR");
        postgresqlMapping.put("NVARCHAR2", "VARCHAR");
        postgresqlMapping.put("CHAR", "CHAR");
        postgresqlMapping.put("NCHAR", "CHAR");
        postgresqlMapping.put("DATE", "TIMESTAMP");
        postgresqlMapping.put("CLOB", "TEXT");
        postgresqlMapping.put("NCLOB", "TEXT");
        postgresqlMapping.put("BLOB", "BYTEA");
        postgresqlMapping.put("RAW", "BYTEA");
        postgresqlMapping.put("LONG RAW", "BYTEA");
        postgresqlMapping.put("BINARY_FLOAT", "REAL");
        postgresqlMapping.put("BINARY_DOUBLE", "DOUBLE PRECISION");
        postgresqlMapping.put("TINYINT", "SMALLINT");
        postgresqlMapping.put("MEDIUMINT", "INTEGER");
        postgresqlMapping.put("LONGTEXT", "TEXT");
        postgresqlMapping.put("BOOLEAN", "BOOLEAN");
        postgresqlMapping.put("SERIAL", "SERIAL");
        postgresqlMapping.put("BIGSERIAL", "BIGSERIAL");
        mapping.put("postgresql", postgresqlMapping);

        return mapping;
    }

    /**
     * 转换数据类型
     *
     * @param sourceType   源数据类型
     * @param sourceDbType 源数据库类型
     * @param targetDbType 目标数据库类型
     * @return 转换后的数据类型
     */
    protected String convertDataType(String sourceType, String sourceDbType, String targetDbType) {
        Map<String, Map<String, String>> typeMapping = getDataTypeMapping();

        // 获取源数据库到标准类型的映射
        Map<String, String> sourceMapping = typeMapping.get(sourceDbType.toLowerCase());
        if (sourceMapping == null) {
            return sourceType; // 如果没有映射关系，保持原类型
        }

        // 获取标准类型到目标数据库的映射
        Map<String, String> targetMapping = typeMapping.get(targetDbType.toLowerCase());
        if (targetMapping == null) {
            return sourceType; // 如果没有映射关系，保持原类型
        }

        // 先找到源类型对应的标准类型
        String standardType = null;
        for (Map.Entry<String, String> entry : sourceMapping.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(sourceType)) {
                standardType = entry.getKey();
                break;
            }
        }

        if (standardType == null) {
            return sourceType; // 如果没有找到对应的标准类型，保持原类型
        }

        // 再从标准类型映射到目标类型
        return targetMapping.getOrDefault(standardType, sourceType);
    }

    /**
     * 获取数据库类型
     */
    public abstract String getDatabaseType();

    /**
     * 获取数据库名称
     */
    public abstract String getDatabaseName();

    /**
     * 获取表列表
     */
    public abstract List<String> listTables() throws Exception;

    /**
     * 获取表信息
     */
    public abstract Map<String, Object> getTableInfo(String tableName) throws Exception;

    /**
     * 获取表的列信息
     */
    public abstract List<Map<String, Object>> listColumns(String tableName) throws Exception;

    /**
     * 获取列详细信息
     */
    public abstract Map<String, Object> getColumnInfo(String tableName, String columnName) throws Exception;

    /**
     * 计算质量指标
     */
    public abstract Map<String, Object> calculateQualityMetric(String tableName, String metricType) throws Exception;

    /**
     * 获取质量问题统计
     */
    public abstract Map<String, Object> getQualityIssues(String tableName) throws Exception;

    /**
     * 获取表的存储引擎
     *
     * @param tableName 表名
     * @return 存储引擎名称
     */
    public abstract String getTableEngine(String tableName) throws Exception;

    /**
     * 获取表的字符集
     *
     * @param tableName 表名
     * @return 字符集名称
     */
    public abstract String getTableCharset(String tableName) throws Exception;

    /**
     * 获取表的排序规则
     *
     * @param tableName 表名
     * @return 排序规则名称
     */
    public abstract String getTableCollation(String tableName) throws Exception;

    /**
     * 获取表的大小（字节）
     *
     * @param tableName 表名
     * @return 表大小
     */
    public abstract Long getTableSize(String tableName) throws Exception;

    /**
     * 获取表的行数
     *
     * @param tableName 表名
     * @return 行数
     */
    public abstract Long getTableRowCount(String tableName) throws Exception;

    /**
     * 获取表空间信息
     *
     * @param tableName 表名
     * @return 表空间名称
     */
    public abstract String getTableSpace(String tableName) throws Exception;

    /**
     * 获取字段的字符长度
     *
     * @param tableName  表名
     * @param columnName 字段名
     * @return 字符长度
     */
    public abstract Integer getCharacterLength(String tableName, String columnName) throws Exception;

    /**
     * 获取字段的数值精度
     *
     * @param tableName  表名
     * @param columnName 字段名
     * @return 数值精度
     */
    public abstract Integer getNumericPrecision(String tableName, String columnName) throws Exception;

    /**
     * 获取字段的小数位数
     *
     * @param tableName  表名
     * @param columnName 字段名
     * @return 小数位数
     */
    public abstract Integer getNumericScale(String tableName, String columnName) throws Exception;

    /**
     * 获取字段的默认值
     *
     * @param tableName  表名
     * @param columnName 字段名
     * @return 默认值
     */
    public abstract String getColumnDefault(String tableName, String columnName) throws Exception;

    /**
     * 获取字段的额外属性（如自增等）
     *
     * @param tableName  表名
     * @param columnName 字段名
     * @return 额外属性
     */
    public abstract String getColumnExtra(String tableName, String columnName) throws Exception;

    /**
     * 获取字段的位置
     *
     * @param tableName  表名
     * @param columnName 字段名
     * @return 字段位置
     */
    public abstract Integer getColumnPosition(String tableName, String columnName) throws Exception;

    /**
     * 获取表的创建时间
     *
     * @param tableName 表名
     * @return 创建时间
     */
    public abstract Date getTableCreateTime(String tableName) throws Exception;

    /**
     * 获取表的更新时间
     *
     * @param tableName 表名
     * @return 更新时间
     */
    public abstract Date getTableUpdateTime(String tableName) throws Exception;

    /**
     * 获取表的主键信息
     *
     * @param tableName 表名
     * @return 主键字段列表
     */
    public abstract List<String> getPrimaryKeys(String tableName) throws Exception;

    /**
     * 获取表的外键信息
     *
     * @param tableName 表名
     * @return 外键信息，包含外键名称、引用表、引用字段等
     */
    public abstract List<Map<String, Object>> getForeignKeys(String tableName) throws Exception;

    /**
     * 获取表的索引信息
     *
     * @param tableName 表名
     * @return 索引信息，包含索引名称、类型、字段等
     */
    public abstract List<Map<String, Object>> getIndexes(String tableName) throws Exception;

    /**
     * 获取表的完整性检查
     *
     * @param tableName 表名
     * @return 完整性检查结果，包含空值率、重复率等
     */
    public abstract Map<String, Object> getTableCompleteness(String tableName) throws Exception;

    /**
     * 获取表的准确性检查
     *
     * @param tableName 表名
     * @return 准确性检查结果，包含数据类型匹配率、格式正确率等
     */
    public abstract Map<String, Object> getTableAccuracy(String tableName) throws Exception;

    /**
     * 获取表的一致性检查
     *
     * @param tableName 表名
     * @return 一致性检查结果，包含外键完整性、约束符合率等
     */
    public abstract Map<String, Object> getTableConsistency(String tableName) throws Exception;

    /**
     * 获取表的唯一性检查
     *
     * @param tableName 表名
     * @return 唯一性检查结果，包含重复记录数等
     */
    public abstract Map<String, Object> getTableUniqueness(String tableName) throws Exception;

    /**
     * 获取表的有效性检查
     *
     * @param tableName 表名
     * @return 有效性检查结果，包含数据范围符合率、业务规则符合率等
     */
    public abstract Map<String, Object> getTableValidity(String tableName) throws Exception;

    /**
     * 获取表的质量问题统计
     *
     * @param tableName 表名
     * @return 质量问题统计，包含各类问题的数量
     */
    public abstract Map<String, Integer> getQualityIssueCount(String tableName) throws Exception;

    /**
     * 获取表的质量问题分布
     *
     * @param tableName 表名
     * @return 质量问题分布，包含各类问题的占比
     */
    public abstract Map<String, Double> getQualityIssueDistribution(String tableName) throws Exception;

    /**
     * 获取表的外键关系
     *
     * @param tableName 表名
     * @return 外键关系列表，包含源表、源字段、目标表、目标字段等信息
     */
    public abstract List<Map<String, Object>> getTableForeignKeyRelations(String tableName) throws Exception;

    /**
     * 获取引用此表的外键关系
     *
     * @param tableName 表名
     * @return 被引用的外键关系列表
     */
    public abstract List<Map<String, Object>> getReferencedByRelations(String tableName) throws Exception;

    /**
     * 获取表的字段统计信息
     *
     * @param tableName 表名
     * @return 字段统计信息，包含字段总数、数值类型字段数、字符类型字段数等
     */
    public abstract Map<String, Object> getTableFieldStatistics(String tableName) throws Exception;

    /**
     * 获取表的数据分布信息
     *
     * @param tableName  表名
     * @param columnName 字段名
     * @return 数据分布信息，包含最大值、最小值、平均值、中位数等
     */
    public abstract Map<String, Object> getColumnDistribution(String tableName, String columnName) throws Exception;

    /**
     * 获取表的数据更新频率
     *
     * @param tableName 表名
     * @return 更新频率信息，包含最近更新时间、平均更新间隔等
     */
    public abstract Map<String, Object> getTableUpdateFrequency(String tableName) throws Exception;

    /**
     * 获取表的增长趋势
     *
     * @param tableName 表名
     * @param days      统计天数
     * @return 增长趋势数据，包含每日新增记录数等
     */
    public abstract List<Map<String, Object>> getTableGrowthTrend(String tableName, int days) throws Exception;

    /**
     * 获取表的数据采样
     *
     * @param tableName  表名
     * @param sampleSize 样本大小
     * @return 数据样本
     */
    public abstract List<Map<String, Object>> getTableDataSample(String tableName, int sampleSize) throws Exception;

    /**
     * 获取字段值域范围
     *
     * @param tableName  表名
     * @param columnName 字段名
     * @return 值域范围信息
     */
    public abstract Map<String, Object> getColumnValueRange(String tableName, String columnName) throws Exception;

    /**
     * 获取字段值分布
     *
     * @param tableName  表名
     * @param columnName 字段名
     * @param topN       前N个值
     * @return 值分布信息
     */
    public abstract List<Map<String, Object>> getColumnValueDistribution(String tableName, String columnName, int topN) throws Exception;


    /**
     * 获取表的依赖对象
     *
     * @param tableName 表名
     * @return 依赖对象列表
     */
    public abstract List<Map<String, Object>> getTableDependencies(String tableName) throws Exception;


    /**
     * 获取存储过程定义
     * 如果指定了表名，则只返回与该表相关的存储过程
     * 如果未指定表名，则返回所有存储过程
     *
     * @param tableName 可选的表名
     * @return 存储过程定义映射，键为存储过程名，值为存储过程定义SQL
     */
    public abstract Map<String, String> getStoredProcedureDefinitions(String tableName) throws Exception;

    /**
     * 获取表的分区列表
     *
     * @param tableName 表名
     * @param partitionField 分区字段名
     * @return 分区值列表
     */
    public abstract List<String> getTablePartitions(String tableName, String partitionField) throws Exception;

    /**
     * 获取创建临时表的SQL
     *
     * @param tempTableName 临时表名称
     * @param sourceTableName 源表名称（用于复制结构）
     * @param preserveRows 是否在会话结束后保留数据（取决于数据库实现）
     * @return 创建临时表的SQL
     * @throws Exception 如果出错
     */
    public abstract String getCreateTempTableSql(String tempTableName, String sourceTableName, boolean preserveRows) throws Exception;

}