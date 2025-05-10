package com.mango.test.database.model.dto.timeseries;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 时序数据库写入数据DTO
 */
@Data
public class WriteTimeSeriesDTO {
    
    /**
     * 指标名称
     */
    private String metricName;
    
    /**
     * 数据点列表
     */
    private List<TimeSeriesPointDTO> points;
    
    /**
     * 公共标签
     */
    private Map<String, String> commonTags;
    
    /**
     * 保留策略
     */
    private String retentionPolicy;
    
    /**
     * 是否同步写入
     */
    private boolean sync;
}