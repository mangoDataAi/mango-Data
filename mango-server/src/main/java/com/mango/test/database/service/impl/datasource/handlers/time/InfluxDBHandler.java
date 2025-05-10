package com.mango.test.database.service.impl.datasource.handlers.time;

import com.mango.test.database.entity.DataSource;
import com.mango.test.database.service.impl.datasource.AbstractTimeSeriesDBHandler;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.*;
import org.influxdb.dto.Point.Builder;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * InfluxDB时序数据库处理器
 */
@Slf4j
public class InfluxDBHandler extends AbstractTimeSeriesDBHandler {
    
    private static final String INFLUXDB_DRIVER = "org.influxdb.InfluxDB";
    private static final String DEFAULT_PORT = "8086";
    private InfluxDB influxDB;
    
    public InfluxDBHandler(DataSource dataSource) {
        super(dataSource);
    }
    
    /**
     * 获取元数据列表
     * @param pattern 名称匹配模式
     * @return 元数据列表
     */
    @Override
    public List<Map<String, Object>> getMetadataList(String pattern) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        try {
            if (!initConnection()) {
                return result;
            }
            
            // 获取所有数据库
            List<String> databases = getDatabases();
            
            for (String database : databases) {
                // 如果提供了pattern，使用它过滤数据库名
                if (StringUtils.hasText(pattern) && !database.contains(pattern)) {
                    continue;
                }
                
                Map<String, Object> dbInfo = new HashMap<>();
                dbInfo.put("name", database);
                dbInfo.put("type", "database");
                
                // 获取该数据库中的measurements数量
                try {
                    List<String> measurements = getMeasurements(database);
                    dbInfo.put("measurementCount", measurements.size());
                    
                    // 添加一些measurements的信息
                    if (!measurements.isEmpty()) {
                        List<Map<String, Object>> measurementList = new ArrayList<>();
                        for (String measurement : measurements) {
                            Map<String, Object> measurementInfo = new HashMap<>();
                            measurementInfo.put("name", measurement);
                            // 可以添加更多信息
                            measurementList.add(measurementInfo);
                        }
                        dbInfo.put("measurements", measurementList);
                    }
                } catch (Exception e) {
                    log.debug("获取数据库 {} 的measurements失败: {}", database, e.getMessage());
                }
                
                result.add(dbInfo);
            }
        } catch (Exception e) {
            log.error("获取InfluxDB元数据列表失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        
        return result;
    }
    
    /**
     * 初始化InfluxDB连接
     */
    private synchronized boolean initConnection() {
        if (influxDB != null) {
            return true;
        }
        
        try {
            // 创建连接配置
            String host = dataSource.getHost();
            String port = StringUtils.hasText(dataSource.getPort()) ? dataSource.getPort() : DEFAULT_PORT;
            String username = dataSource.getUsername();
            String password = dataSource.getPassword();
            
            // 构建连接URL
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append("http");
            // 使用默认的SSL配置（如果支持）
            boolean useSSL = false; // 默认不使用SSL
            if (useSSL) {
                urlBuilder.append("s");
            }
            urlBuilder.append("://").append(host).append(":").append(port);
            
            // 创建InfluxDB客户端
            influxDB = InfluxDBFactory.connect(urlBuilder.toString(), username, password);
            
            // 设置默认保留策略
            influxDB.setRetentionPolicy("autogen");
            
            return true;
        } catch (Exception e) {
            log.error("初始化InfluxDB连接失败: {}", e.getMessage(), e);
            closeConnection();
            return false;
        }
    }
    
    /**
     * 关闭InfluxDB连接
     */
    private synchronized void closeConnection() {
        if (influxDB != null) {
            try {
                influxDB.close();
            } catch (Exception e) {
                log.error("关闭InfluxDB连接失败: {}", e.getMessage(), e);
            } finally {
                influxDB = null;
            }
        }
    }
    
    @Override
    public boolean testConnection() {
        try {
            if (!initConnection()) {
                return false;
            }
            
            // 执行简单查询测试连接
            Query query = new Query("show databases", "");
            QueryResult result = influxDB.query(query);
            return result.getError() == null;
        } catch (Exception e) {
            log.error("测试InfluxDB连接失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection();
        }
    }
    
    @Override
    public List<Map<String, Object>> queryTimeSeries(String database, String measurement, Map<String, Object> filters, String timeRange, String aggregation) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        try {
            if (!initConnection()) {
                return result;
            }
            
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("SELECT ");
            
            // 设置聚合函数或查询所有字段
            if (StringUtils.hasText(aggregation)) {
                queryBuilder.append(aggregation);
            } else {
                queryBuilder.append("*");
            }
            
            queryBuilder.append(" FROM ").append(measurement);
            
            // 添加过滤条件
            if (filters != null && !filters.isEmpty()) {
                queryBuilder.append(" WHERE ");
                
                int i = 0;
                for (Map.Entry<String, Object> entry : filters.entrySet()) {
                    if (i > 0) {
                        queryBuilder.append(" AND ");
                    }
                    
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    
                    if (value instanceof String) {
                        queryBuilder.append(key).append(" = '").append(value).append("'");
                    } else {
                        queryBuilder.append(key).append(" = ").append(value);
                    }
                    
                    i++;
                }
                
                // 添加时间范围
                if (StringUtils.hasText(timeRange)) {
                    queryBuilder.append(" AND ").append(timeRange);
                }
            } else if (StringUtils.hasText(timeRange)) {
                queryBuilder.append(" WHERE ").append(timeRange);
            }
            
            Query query = new Query(queryBuilder.toString(), database);
            QueryResult queryResult = influxDB.query(query);
            
            if (queryResult.getError() != null) {
                log.error("InfluxDB查询错误: {}", queryResult.getError());
                return result;
            }
            
            // 处理查询结果
            if (queryResult.getResults() != null) {
                for (QueryResult.Result res : queryResult.getResults()) {
                    if (res.getSeries() != null) {
                        for (QueryResult.Series series : res.getSeries()) {
                            List<String> columns = series.getColumns();
                            
                            for (List<Object> values : series.getValues()) {
                                Map<String, Object> point = new HashMap<>();
                                
                                // 添加标签信息
                                if (series.getTags() != null) {
                                    point.putAll(series.getTags());
                                }
                                
                                // 添加数据点字段
                                for (int i = 0; i < columns.size(); i++) {
                                    point.put(columns.get(i), values.get(i));
                                }
                                
                                result.add(point);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("查询InfluxDB数据失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        
        return result;
    }
    
    /**
     * 写入时间序列数据点
     * @param database 数据库名
     * @param measurement 测量名称
     * @param tags 标签Map
     * @param fields 字段Map
     * @param timestamp 时间戳
     * @return 是否写入成功
     */
    @Override
    public boolean writeTimeSeriesPoint(String database, String measurement, Map<String, String> tags, Map<String, Object> fields, Long timestamp) {
        try {
            if (!initConnection()) {
                return false;
            }
            
            // 检查数据库是否存在，不存在则创建
            if (!databaseExists(database)) {
                Query query = new Query("CREATE DATABASE " + database, "");
                influxDB.query(query);
            }
            
            // 创建数据点
            Builder pointBuilder = Point.measurement(measurement);
            
            // 添加标签
            if (tags != null && !tags.isEmpty()) {
                for (Map.Entry<String, String> entry : tags.entrySet()) {
                    pointBuilder.tag(entry.getKey(), entry.getValue());
                }
            }
            
            // 添加字段
            if (fields != null && !fields.isEmpty()) {
                for (Map.Entry<String, Object> entry : fields.entrySet()) {
                    addFieldToPointBuilder(pointBuilder, entry.getKey(), entry.getValue());
                }
            }
            
            // 设置时间戳
            if (timestamp != null) {
                pointBuilder.time(timestamp, TimeUnit.MILLISECONDS);
            }
            
            // 写入数据点
            influxDB.write(database, "", pointBuilder.build());
            
            return true;
        } catch (Exception e) {
            log.error("写入InfluxDB数据点失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection();
        }
    }
    
    @Override
    public boolean writeTimeSeriesPoints(String database, List<Map<String, Object>> points) {
        try {
            if (!initConnection()) {
                return false;
            }
            
            // 检查数据库是否存在，不存在则创建
            if (!databaseExists(database)) {
                Query query = new Query("CREATE DATABASE " + database, "");
                influxDB.query(query);
            }
            
            // 批量写入模式
            BatchPoints batchPoints = BatchPoints
                .database(database)
                .consistency(InfluxDB.ConsistencyLevel.ALL)
                .build();
            
            for (Map<String, Object> pointData : points) {
                if (!pointData.containsKey("measurement")) {
                    log.warn("跳过没有measurement字段的数据点");
                    continue;
                }
                
                String measurement = pointData.get("measurement").toString();
                
                Builder pointBuilder = Point.measurement(measurement);
                
                // 添加标签
                if (pointData.containsKey("tags") && pointData.get("tags") instanceof Map) {
                    Map<String, String> tags = (Map<String, String>) pointData.get("tags");
                    for (Map.Entry<String, String> entry : tags.entrySet()) {
                        pointBuilder.tag(entry.getKey(), entry.getValue());
                    }
                }
                
                // 添加字段
                if (pointData.containsKey("fields") && pointData.get("fields") instanceof Map) {
                    Map<String, Object> fields = (Map<String, Object>) pointData.get("fields");
                    for (Map.Entry<String, Object> entry : fields.entrySet()) {
                        addFieldToPointBuilder(pointBuilder, entry.getKey(), entry.getValue());
                    }
                }
                
                // 设置时间戳
                if (pointData.containsKey("timestamp") && pointData.get("timestamp") instanceof Number) {
                    Number timestamp = (Number) pointData.get("timestamp");
                    pointBuilder.time(timestamp.longValue(), TimeUnit.MILLISECONDS);
                }
                
                // 添加到批处理
                batchPoints.point(pointBuilder.build());
            }
            
            // 写入所有点
            influxDB.write(batchPoints);
            
            return true;
        } catch (Exception e) {
            log.error("批量写入InfluxDB数据点失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection();
        }
    }
    
    @Override
    public List<String> getDatabases() {
        List<String> databases = new ArrayList<>();
        
        try {
            if (!initConnection()) {
                return databases;
            }
            
            Query query = new Query("SHOW DATABASES", "");
            QueryResult queryResult = influxDB.query(query);
            
            if (queryResult.getError() != null) {
                log.error("获取InfluxDB数据库列表错误: {}", queryResult.getError());
                return databases;
            }
            
            // 处理查询结果
            if (queryResult.getResults() != null) {
                for (QueryResult.Result result : queryResult.getResults()) {
                    if (result.getSeries() != null) {
                        for (QueryResult.Series series : result.getSeries()) {
                            if (series.getValues() != null) {
                                for (List<Object> values : series.getValues()) {
                                    if (values != null && !values.isEmpty() && values.get(0) != null) {
                                        databases.add(values.get(0).toString());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取InfluxDB数据库列表失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        
        return databases;
    }
    
    /**
     * 检查数据库是否存在
     */
    private boolean databaseExists(String database) {
        List<String> databases = getDatabases();
        return databases.contains(database);
    }
    
    @Override
    public List<String> getMeasurements(String database) {
        List<String> measurements = new ArrayList<>();
        
        try {
            if (!initConnection()) {
                return measurements;
            }
            
            // 检查数据库是否存在
            if (!databaseExists(database)) {
                return measurements;
            }
            
            Query query = new Query("SHOW MEASUREMENTS", database);
            QueryResult queryResult = influxDB.query(query);
            
            if (queryResult.getError() != null) {
                log.error("获取InfluxDB measurements列表错误: {}", queryResult.getError());
                return measurements;
            }
            
            // 处理查询结果
            if (queryResult.getResults() != null) {
                for (QueryResult.Result result : queryResult.getResults()) {
                    if (result.getSeries() != null) {
                        for (QueryResult.Series series : result.getSeries()) {
                            if (series.getValues() != null) {
                                for (List<Object> values : series.getValues()) {
                                    if (values != null && !values.isEmpty() && values.get(0) != null) {
                                        measurements.add(values.get(0).toString());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取InfluxDB measurements列表失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        
        return measurements;
    }
    
    /**
     * 获取标签键及其元数据
     *
     * @param database    数据库名称
     * @param measurement 测量名称
     * @return 标签键及其元数据
     */
    @Override
    public List<Map<String, Object>> getTagKeys(String database, String measurement) {
        List<Map<String, Object>> tagKeys = new ArrayList<>();
        
        try {
            InfluxDB influxDB = getInfluxDBConnection();
            if (influxDB == null) {
                return tagKeys;
            }
            
            try {
                // 检查数据库和测量是否存在
                if (!databaseExists(database) || !getMeasurements(database).contains(measurement)) {
                    return tagKeys;
                }
                
                Query query = new Query("SHOW TAG KEYS FROM \"" + measurement + "\"", database);
                QueryResult queryResult = influxDB.query(query);
                
                if (queryResult.getError() != null) {
                    log.error("获取InfluxDB tag keys错误: {}", queryResult.getError());
                    return tagKeys;
                }
                
                // 处理查询结果
                if (queryResult.getResults() != null) {
                    for (QueryResult.Result result : queryResult.getResults()) {
                        if (result.getSeries() != null) {
                            for (QueryResult.Series series : result.getSeries()) {
                                if (series.getValues() != null) {
                                    for (List<Object> values : series.getValues()) {
                                        if (values != null && !values.isEmpty() && values.get(0) != null) {
                                            String tagKey = values.get(0).toString();
                                            
                                            Map<String, Object> tagKeyInfo = new HashMap<>();
                                            tagKeyInfo.put("key", tagKey);
                                            tagKeyInfo.put("type", "tag");
                                            
                                            // 获取标签值的个数
                                            try {
                                                Query valuesQuery = new Query("SHOW TAG VALUES FROM \"" + measurement + "\" WITH KEY = \"" + tagKey + "\"", database);
                                                QueryResult valuesResult = influxDB.query(valuesQuery);
                                                
                                                if (valuesResult.getResults() != null &&
                                                    !valuesResult.getResults().isEmpty() &&
                                                    valuesResult.getResults().get(0).getSeries() != null) {
                                                    
                                                    List<List<Object>> tagValues = valuesResult.getResults().get(0).getSeries().get(0).getValues();
                                                    if (tagValues != null) {
                                                        tagKeyInfo.put("valueCount", tagValues.size());
                                                    }
                                                }
                                            } catch (Exception e) {
                                                log.warn("获取标签 {} 的值数量失败: {}", tagKey, e.getMessage());
                                            }
                                            
                                            tagKeys.add(tagKeyInfo);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } finally {
                releaseConnection(influxDB);
            }
        } catch (Exception e) {
            log.error("获取InfluxDB tag keys失败: {}", e.getMessage(), e);
        }
        
        return tagKeys;
    }
    
    /**
     * 获取InfluxDB连接
     */
    private InfluxDB getInfluxDBConnection() {
        if (influxDB == null) {
            initConnection();
        }
        return influxDB;
    }
    
    /**
     * 获取JDBC连接
     * 覆盖父类的方法，使用父类的默认Connection实现
     */
    @Override
    public java.sql.Connection getConnection() throws Exception {
        return super.getConnection();
    }
    
    /**
     * 获取标签键及其可能的值
     *
     * @param measurement 测量名称
     * @return 标签键及其可能的值
     */
    @Override
    public Map<String, List<String>> getTagKeys(String measurement) {
        Map<String, List<String>> result = new HashMap<>();
        try {
            InfluxDB influxDB = getInfluxDBConnection();
            if (influxDB == null) {
                return result;
            }
            
            try {
                String database = dataSource.getDbName(); // 使用getDbName替代getDatabaseName
                if (!StringUtils.hasText(database)) { // 使用hasText替代isEmpty
                    log.error("数据库名称为空");
                    return result;
                }
                
                // 查询标签键
                String query = "SHOW TAG KEYS FROM \"" + measurement + "\"";
                QueryResult queryResult = influxDB.query(new Query(query, database));
                
                if (queryResult.hasError()) {
                    log.error("查询标签键失败: {}", queryResult.getError());
                    return result;
                }
                
                List<QueryResult.Result> results = queryResult.getResults();
                if (results == null || results.isEmpty()) {
                    return result;
                }
                
                QueryResult.Result queryResultData = results.get(0);
                List<QueryResult.Series> series = queryResultData.getSeries();
                if (series == null || series.isEmpty()) {
                    return result;
                }
                
                // 获取标签键名称
                List<String> tagKeyNames = new ArrayList<>();
                List<List<Object>> values = series.get(0).getValues();
                if (values != null) {
                    for (List<Object> value : values) {
                        tagKeyNames.add(value.get(0).toString());
                    }
                }
                
                // 获取每个标签键的可能值
                for (String tagKey : tagKeyNames) {
                    String valuesQuery = "SHOW TAG VALUES FROM \"" + measurement + "\" WITH KEY = \"" + tagKey + "\"";
                    QueryResult valuesResult = influxDB.query(new Query(valuesQuery, database));
                    
                    List<String> tagValues = new ArrayList<>();
                    
                    if (!valuesResult.hasError() && valuesResult.getResults() != null && !valuesResult.getResults().isEmpty()) {
                        QueryResult.Result valuesResultData = valuesResult.getResults().get(0);
                        List<QueryResult.Series> valuesSeries = valuesResultData.getSeries();
                        
                        if (valuesSeries != null && !valuesSeries.isEmpty()) {
                            List<List<Object>> tagValuesList = valuesSeries.get(0).getValues();
                            if (tagValuesList != null) {
                                for (List<Object> row : tagValuesList) {
                                    tagValues.add(row.get(1).toString());
                                }
                            }
                        }
                    }
                    
                    result.put(tagKey, tagValues);
                }
            } finally {
                releaseConnection(influxDB);
            }
        } catch (Exception e) {
            log.error("获取标签键及其可能的值失败: {}", e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * 获取指定数据库中所有字段键
     * @param database 数据库名
     * @return 字段键列表
     */
    @Override
    public List<Map<String, Object>> getFieldKeys(String database) {
        List<Map<String, Object>> fieldKeys = new ArrayList<>();
        
        try {
            if (!initConnection()) {
                return fieldKeys;
            }
            
            // 检查数据库是否存在
            if (!databaseExists(database)) {
                return fieldKeys;
            }
            
            // 获取所有measurements
            List<String> measurements = getMeasurements(database);
            
            // 对每个measurement获取字段键
            for (String measurement : measurements) {
                List<Map<String, Object>> measurementFieldKeys = getFieldKeys(database, measurement);
                
                // 为每个字段键添加measurement信息
                for (Map<String, Object> fieldKey : measurementFieldKeys) {
                    fieldKey.put("measurement", measurement);
                    fieldKeys.add(fieldKey);
                }
            }
        } catch (Exception e) {
            log.error("获取InfluxDB所有字段键失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        
        return fieldKeys;
    }
    
    /**
     * 获取指定数据库和测量的字段键
     * @param database 数据库名
     * @param measurement 测量名
     * @return 字段键列表
     */
    @Override
    public List<Map<String, Object>> getFieldKeys(String database, String measurement) {
        List<Map<String, Object>> fieldKeys = new ArrayList<>();
        
        try {
            if (!initConnection()) {
                return fieldKeys;
            }
            
            // 检查数据库和测量是否存在
            if (!databaseExists(database) || !getMeasurements(database).contains(measurement)) {
                return fieldKeys;
            }
            
            Query query = new Query("SHOW FIELD KEYS FROM \"" + measurement + "\"", database);
            QueryResult queryResult = influxDB.query(query);
            
            if (queryResult.getError() != null) {
                log.error("获取InfluxDB field keys错误: {}", queryResult.getError());
                return fieldKeys;
            }
            
            // 处理查询结果
            if (queryResult.getResults() != null) {
                for (QueryResult.Result result : queryResult.getResults()) {
                    if (result.getSeries() != null) {
                        for (QueryResult.Series series : result.getSeries()) {
                            List<String> columns = series.getColumns();
                            int fieldKeyIndex = columns.indexOf("fieldKey");
                            int fieldTypeIndex = columns.indexOf("fieldType");
                            
                            if (fieldKeyIndex != -1 && fieldTypeIndex != -1 && series.getValues() != null) {
                                for (List<Object> values : series.getValues()) {
                                    if (values != null && values.size() > Math.max(fieldKeyIndex, fieldTypeIndex)) {
                                        String fieldKey = values.get(fieldKeyIndex).toString();
                                        String fieldType = values.get(fieldTypeIndex).toString();
                                        
                                        Map<String, Object> fieldKeyInfo = new HashMap<>();
                                        fieldKeyInfo.put("key", fieldKey);
                                        fieldKeyInfo.put("type", "field");
                                        fieldKeyInfo.put("dataType", fieldType);
                                        
                                        fieldKeys.add(fieldKeyInfo);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取InfluxDB field keys失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        
        return fieldKeys;
    }
    
    @Override
    public Map<String, Object> getMetadata(String database, String measurement) {
        Map<String, Object> metadata = new HashMap<>();
        
        try {
            if (!initConnection()) {
                return metadata;
            }
            
            metadata.put("database", database);
            metadata.put("measurement", measurement);
            
            // 添加标签键
            List<Map<String, Object>> tagKeysList = getTagKeys(database, measurement);
            metadata.put("tagKeys", tagKeysList);
            
            // 添加字段键
            List<Map<String, Object>> fieldKeys = getFieldKeys(database, measurement);
            metadata.put("fieldKeys", fieldKeys);
            
            // 获取数据点数量
            try {
                Query countQuery = new Query("SELECT COUNT(*) FROM \"" + measurement + "\"", database);
                QueryResult countResult = influxDB.query(countQuery);
                
                if (countResult.getResults() != null &&
                    !countResult.getResults().isEmpty() &&
                    countResult.getResults().get(0).getSeries() != null &&
                    !countResult.getResults().get(0).getSeries().isEmpty()) {
                    
                    List<String> columns = countResult.getResults().get(0).getSeries().get(0).getColumns();
                    List<List<Object>> values = countResult.getResults().get(0).getSeries().get(0).getValues();
                    
                    if (values != null && !values.isEmpty() && values.get(0) != null) {
                        int countIndex = columns.indexOf("count");
                        if (countIndex != -1 && values.get(0).size() > countIndex) {
                            metadata.put("count", values.get(0).get(countIndex));
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("获取测量 {} 的计数失败: {}", measurement, e.getMessage());
            }
            
            // 获取最早和最晚的时间戳
            try {
                Query timeQuery = new Query("SELECT FIRST(time), LAST(time) FROM \"" + measurement + "\"", database);
                QueryResult timeResult = influxDB.query(timeQuery);
                
                if (timeResult.getResults() != null &&
                    !timeResult.getResults().isEmpty() &&
                    timeResult.getResults().get(0).getSeries() != null &&
                    !timeResult.getResults().get(0).getSeries().isEmpty()) {
                    
                    List<String> columns = timeResult.getResults().get(0).getSeries().get(0).getColumns();
                    List<List<Object>> values = timeResult.getResults().get(0).getSeries().get(0).getValues();
                    
                    if (values != null && !values.isEmpty() && values.get(0) != null) {
                        int firstIndex = columns.indexOf("first");
                        int lastIndex = columns.indexOf("last");
                        
                        if (firstIndex != -1 && values.get(0).size() > firstIndex) {
                            metadata.put("firstTimestamp", values.get(0).get(firstIndex));
                        }
                        
                        if (lastIndex != -1 && values.get(0).size() > lastIndex) {
                            metadata.put("lastTimestamp", values.get(0).get(lastIndex));
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("获取测量 {} 的时间范围失败: {}", measurement, e.getMessage());
            }
        } catch (Exception e) {
            log.error("获取InfluxDB元数据失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        
        return metadata;
    }
    
    @Override
    public String getDefaultPort() {
        return DEFAULT_PORT;
    }
    
    @Override
    public String getDriverClassName() {
        return INFLUXDB_DRIVER;
    }

    /**
     * 获取元数据（默认获取所有数据库的元数据）
     * @return 元数据信息
     */
    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        
        try {
            if (!initConnection()) {
                return metadata;
            }
            
            // 获取所有数据库
            List<String> databases = getDatabases();
            metadata.put("databaseCount", databases.size());
            
            // 获取每个数据库的信息
            List<Map<String, Object>> databaseList = new ArrayList<>();
            for (String database : databases) {
                Map<String, Object> dbInfo = new HashMap<>();
                dbInfo.put("name", database);
                
                // 获取该数据库的measurements
                List<String> measurements = getMeasurements(database);
                dbInfo.put("measurementCount", measurements.size());
                
                databaseList.add(dbInfo);
            }
            
            metadata.put("databases", databaseList);
            
            // 添加版本信息
            try {
                String version = influxDB.version();
                metadata.put("version", version);
            } catch (Exception e) {
                log.debug("获取InfluxDB版本信息失败: {}", e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("获取InfluxDB元数据失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        
        return metadata;
    }

    /**
     * 获取所有measurements，使用默认数据库
     * @return measurements列表
     */
    @Override
    public List<String> getMeasurements() {
        // 获取默认数据库名
        String database = dataSource.getDbName();
        
        // 检查数据库名是否为空
        if (!StringUtils.hasText(database)) {
            log.error("获取measurements失败: 未指定默认数据库名");
            return Collections.emptyList();
        }
        
        // 调用重载方法获取指定数据库的measurements
        return getMeasurements(database);
    }

    /**
     * 添加字段到Point.Builder对象
     * @param builder Point.Builder实例
     * @param key 字段名
     * @param value 字段值
     */
    private void addFieldToPointBuilder(Builder builder, String key, Object value) {
        if (value == null) {
            return;
        }
        
        if (value instanceof String) {
            builder.addField(key, (String) value);
        } else if (value instanceof Integer) {
            builder.addField(key, (Integer) value);
        } else if (value instanceof Long) {
            builder.addField(key, (Long) value);
        } else if (value instanceof Double) {
            builder.addField(key, (Double) value);
        } else if (value instanceof Float) {
            builder.addField(key, (Float) value);
        } else if (value instanceof Boolean) {
            builder.addField(key, (Boolean) value);
        } else {
            // 对于其他类型，转换为字符串
            builder.addField(key, String.valueOf(value));
        }
    }

    /**
     * 写入时间序列数据点（兼容旧版方法签名）
     * @param database 数据库名
     * @param tags 标签Map
     * @param fields 字段Map
     * @param timestamp 时间戳
     * @return 是否写入成功
     */
    @Override
    public boolean writeTimeSeriesPoint(String database, Map<String, String> tags, Map<String, Object> fields, Long timestamp) {
        // 从tags中获取measurement名称，默认为"data"
        String measurement = "data";
        if (tags != null && tags.containsKey("measurement")) {
            measurement = tags.get("measurement");
            // 从tags中移除measurement，因为它不是真正的标签
            Map<String, String> tagsCopy = new HashMap<>(tags);
            tagsCopy.remove("measurement");
            tags = tagsCopy;
        }
        
        // 调用新实现的方法，包含measurement参数
        return writeTimeSeriesPoint(database, measurement, tags, fields, timestamp);
    }

    /**
     * 查询时间序列数据（简化版本，使用默认数据库）
     * @param measurement 测量名称
     * @param queryParams 查询参数Map，可包含filters, timeRange, aggregation等
     * @return 查询结果列表
     */
    @Override
    public List<Map<String, Object>> queryTimeSeries(String measurement, Map<String, Object> queryParams) {
        // 获取默认数据库名
        String database = dataSource.getDbName();
        
        // 检查数据库名是否为空
        if (!StringUtils.hasText(database)) {
            log.error("查询时间序列失败: 未指定默认数据库名");
            return Collections.emptyList();
        }
        
        // 从查询参数中提取需要的参数
        Map<String, Object> filters = null;
        String timeRange = null;
        String aggregation = null;
        
        if (queryParams != null) {
            // 提取filters参数
            if (queryParams.containsKey("filters") && queryParams.get("filters") instanceof Map) {
                filters = (Map<String, Object>) queryParams.get("filters");
            }
            
            // 提取timeRange参数
            if (queryParams.containsKey("timeRange") && queryParams.get("timeRange") instanceof String) {
                timeRange = (String) queryParams.get("timeRange");
            }
            
            // 提取aggregation参数
            if (queryParams.containsKey("aggregation") && queryParams.get("aggregation") instanceof String) {
                aggregation = (String) queryParams.get("aggregation");
            }
        }
        
        // 调用完整版本的查询方法
        return queryTimeSeries(database, measurement, filters, timeRange, aggregation);
    }

    /**
     * 释放InfluxDB连接（这里我们不真正关闭它，而是保持连接池模式）
     * @param db InfluxDB实例
     */
    private void releaseConnection(InfluxDB db) {
        // 由于我们使用的是连接池模式，这里不做任何操作
        // 实际连接将在closeConnection()方法中关闭
    }

    @Override
    public List<String> getSupportedAggregations() {
        List<String> aggregations = new ArrayList<>();
        aggregations.add("MEAN");
        aggregations.add("SUM");
        aggregations.add("COUNT");
        aggregations.add("MIN");
        aggregations.add("MAX");
        aggregations.add("FIRST");
        aggregations.add("LAST");
        aggregations.add("SPREAD");
        aggregations.add("STDDEV");
        aggregations.add("PERCENTILE");
        aggregations.add("INTEGRAL");
        aggregations.add("DERIVATIVE");
        aggregations.add("NON_NEGATIVE_DERIVATIVE");
        aggregations.add("MEDIAN");
        aggregations.add("MODE");
        return aggregations;
    }
    
    @Override
    public List<String> getSupportedAggregations(String database) {
        // InfluxDB的聚合函数不依赖于特定的数据库，因此直接返回所有支持的聚合函数
        return getSupportedAggregations();
    }
} 