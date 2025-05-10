package com.mango.test.database.model.dto.timeseries;

import lombok.Data;
import java.util.Map;

/**
 * 时序数据库查询DTO
 */
@Data
public class TimeSeriesQueryDTO {
    
    /**
     * 指标名称
     */
    private String metricName;
    
    /**
     * 开始时间戳
     */
    private Long startTime;
    
    /**
     * 结束时间戳
     */
    private Long endTime;
    
    /**
     * 聚合函数
     */
    private String aggregation;
    
    /**
     * 聚合间隔
     */
    private String interval;
    
    /**
     * 标签过滤条件
     */
    private Map<String, String> tags;
    
    /**
     * 结果限制
     */
    private Integer limit;
    
    /**
     * 结果偏移量
     */
    private Integer offset;
    
    /**
     * 排序方式 (ASC, DESC)
     */
    private String order;
    
    /**
     * 是否包含标签
     */
    private boolean includeTags;
} 