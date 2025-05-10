package com.mango.test.database.model.dto.timeseries;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 时序数据库指标DTO
 */
@Data
public class MetricDTO {
    
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
     * 额外信息
     */
    private Map<String, Object> extra;
} 