package com.mango.test.database.model.dto.timeseries;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 时序数据库指标详情DTO
 */
@Data
public class MetricInfoDTO {
    
    /**
     * 指标名称
     */
    private String name;
    
    /**
     * 指标描述
     */
    private String description;
    
    /**
     * 数据类型
     */
    private String dataType;
    
    /**
     * 标签列表
     */
    private List<String> tags;
    
    /**
     * 标签值
     */
    private Map<String, List<String>> tagValues;
    
    /**
     * 支持的聚合函数
     */
    private List<String> supportedAggregations;
    
    /**
     * 默认保留策略
     */
    private String retentionPolicy;
    
    /**
     * 数据点数量
     */
    private Long count;
    
    /**
     * 最早的数据点时间戳
     */
    private Long firstTimestamp;
    
    /**
     * 最新的数据点时间戳
     */
    private Long lastTimestamp;
    
    /**
     * 统计信息
     */
    private Map<String, Object> statistics;
    
    /**
     * 额外信息
     */
    private Map<String, Object> extra;
} 