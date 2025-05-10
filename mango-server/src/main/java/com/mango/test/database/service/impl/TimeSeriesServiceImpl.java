package com.mango.test.database.service.impl;

import com.mango.test.database.entity.DataSource;
import com.mango.test.database.mapper.DataSourceMapper;
import com.mango.test.database.service.TimeSeriesService;
import com.mango.test.database.service.impl.datasource.AbstractTimeSeriesDBHandler;
import com.mango.test.database.service.impl.datasource.DatabaseHandlerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 时序数据库服务实现类
 */
@Slf4j
@Service
public class TimeSeriesServiceImpl implements TimeSeriesService {

    @Autowired
    private DataSourceMapper dataSourceMapper;

    /**
     * 获取时序数据库处理器
     */
    private AbstractTimeSeriesDBHandler getTimeSeriesDBHandler(String dataSourceId) throws Exception {
        DataSource dataSource = dataSourceMapper.selectById(dataSourceId);
        if (dataSource == null) {
            throw new Exception("数据源不存在: " + dataSourceId);
        }
        return DatabaseHandlerFactory.getTimeSeriesDBHandler(dataSource);
    }

    @Override
    public List<Map<String, Object>> getMetrics(String dataSourceId) throws Exception {
        AbstractTimeSeriesDBHandler handler = getTimeSeriesDBHandler(dataSourceId);
        try {
            List<String> measurements = handler.getMeasurements();
            List<Map<String, Object>> metrics = new ArrayList<>();
            
            // 将测量名转换为指标信息
            for (String measurement : measurements) {
                Map<String, Object> metric = new HashMap<>();
                metric.put("name", measurement);
                metrics.add(metric);
            }
            
            return metrics;
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public Map<String, Object> getMetricInfo(String dataSourceId, String metricName) throws Exception {
        AbstractTimeSeriesDBHandler handler = getTimeSeriesDBHandler(dataSourceId);
        try {
            Map<String, Object> metricInfo = new HashMap<>();
            metricInfo.put("name", metricName);
            
            // 获取标签键
            Map<String, List<String>> tagKeysWithValues = handler.getTagKeys(metricName);
            metricInfo.put("tags", tagKeysWithValues);
            
            // 获取字段键
            List<Map<String, Object>> fieldKeys = handler.getFieldKeys(null, metricName);
            metricInfo.put("fields", fieldKeys);
            
            return metricInfo;
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public List<Map<String, Object>> getTimeSeriesData(String dataSourceId, Map<String, Object> query) throws Exception {
        AbstractTimeSeriesDBHandler handler = getTimeSeriesDBHandler(dataSourceId);
        try {
            // 从查询参数中提取必要信息
            String metricName = query.get("metric").toString();
            String timeRange = query.containsKey("timeRange") ? query.get("timeRange").toString() : "1h"; // 默认查询1小时
            String aggregation = query.containsKey("aggregation") ? query.get("aggregation").toString() : null;
            Map<String, Object> filters = query.containsKey("filters") ? (Map<String, Object>) query.get("filters") : new HashMap<>();
            
            // 查询时序数据
            return handler.queryTimeSeries(null, metricName, filters, timeRange, aggregation);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public List<String> getTagKeys(String dataSourceId, String metricName) throws Exception {
        AbstractTimeSeriesDBHandler handler = getTimeSeriesDBHandler(dataSourceId);
        try {
            Map<String, List<String>> tagKeysWithValues = handler.getTagKeys(metricName);
            return new ArrayList<>(tagKeysWithValues.keySet());
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public List<String> getTagValues(String dataSourceId, String metricName, String tagKey) throws Exception {
        AbstractTimeSeriesDBHandler handler = getTimeSeriesDBHandler(dataSourceId);
        try {
            Map<String, List<String>> tagKeysWithValues = handler.getTagKeys(metricName);
            return tagKeysWithValues.getOrDefault(tagKey, new ArrayList<>());
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public List<String> getSupportedAggregations(String dataSourceId, String metricName) throws Exception {
        AbstractTimeSeriesDBHandler handler = getTimeSeriesDBHandler(dataSourceId);
        try {
            // 如果handler支持针对特定metric的聚合函数查询，则使用
            if (metricName != null && !metricName.isEmpty()) {
                try {
                    return handler.getSupportedAggregations(metricName);
                } catch (UnsupportedOperationException e) {
                    // 如果不支持针对特定metric的查询，则使用通用查询
                    return handler.getSupportedAggregations();
                }
            } else {
                return handler.getSupportedAggregations();
            }
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public Map<String, List<Map<String, Object>>> getBatchTimeSeriesData(String dataSourceId, Map<String, Object> query) throws Exception {
        AbstractTimeSeriesDBHandler handler = getTimeSeriesDBHandler(dataSourceId);
        try {
            // 从查询参数中提取必要信息
            List<String> metrics = (List<String>) query.get("metrics");
            String timeRange = query.containsKey("timeRange") ? query.get("timeRange").toString() : "1h"; // 默认查询1小时
            String aggregation = query.containsKey("aggregation") ? query.get("aggregation").toString() : null;
            Map<String, Object> filters = query.containsKey("filters") ? (Map<String, Object>) query.get("filters") : new HashMap<>();
            
            // 为每个指标查询时序数据
            Map<String, List<Map<String, Object>>> result = new HashMap<>();
            for (String metricName : metrics) {
                List<Map<String, Object>> data = handler.queryTimeSeries(null, metricName, filters, timeRange, aggregation);
                result.put(metricName, data);
            }
            
            return result;
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public boolean writeTimeSeriesData(String dataSourceId, Map<String, Object> data) throws Exception {
        AbstractTimeSeriesDBHandler handler = getTimeSeriesDBHandler(dataSourceId);
        try {
            // 从数据参数中提取必要信息
            String measurement = data.get("metric").toString();
            Map<String, String> tags = (Map<String, String>) data.get("tags");
            Map<String, Object> fields = (Map<String, Object>) data.get("fields");
            Long timestamp = data.containsKey("timestamp") ? Long.parseLong(data.get("timestamp").toString()) : null;
            
            // 写入数据点
            return handler.writeTimeSeriesPoint(measurement, tags, fields, timestamp);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public Map<String, Object> getDatabaseStats(String dataSourceId) throws Exception {
        AbstractTimeSeriesDBHandler handler = getTimeSeriesDBHandler(dataSourceId);
        try {
            return handler.getMetadata();
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }
} 