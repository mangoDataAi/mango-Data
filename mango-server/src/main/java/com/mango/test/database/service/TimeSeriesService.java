package com.mango.test.database.service;

import java.util.List;
import java.util.Map;

/**
 * 时序数据库服务接口
 */
public interface TimeSeriesService {

    /**
     * 获取所有指标
     */
    List<Map<String, Object>> getMetrics(String dataSourceId) throws Exception;

    /**
     * 获取指标详情
     */
    Map<String, Object> getMetricInfo(String dataSourceId, String metricName) throws Exception;

    /**
     * 获取时序数据
     */
    List<Map<String, Object>> getTimeSeriesData(String dataSourceId, Map<String, Object> query) throws Exception;

    /**
     * 获取指标的标签列表
     */
    List<String> getTagKeys(String dataSourceId, String metricName) throws Exception;

    /**
     * 获取指标标签的可选值
     */
    List<String> getTagValues(String dataSourceId, String metricName, String tagKey) throws Exception;

    /**
     * 获取指标支持的聚合函数
     */
    List<String> getSupportedAggregations(String dataSourceId, String metricName) throws Exception;

    /**
     * 批量获取时序数据
     */
    Map<String, List<Map<String, Object>>> getBatchTimeSeriesData(String dataSourceId, Map<String, Object> query) throws Exception;

    /**
     * 写入时序数据
     */
    boolean writeTimeSeriesData(String dataSourceId, Map<String, Object> data) throws Exception;

    /**
     * 获取时序数据库统计信息
     */
    Map<String, Object> getDatabaseStats(String dataSourceId) throws Exception;
} 