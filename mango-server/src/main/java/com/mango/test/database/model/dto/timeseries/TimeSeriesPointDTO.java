package com.mango.test.database.model.dto.timeseries;

import lombok.Data;
import java.util.Map;

/**
 * 时序数据库数据点DTO
 */
@Data
public class TimeSeriesPointDTO {
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * 数值
     */
    private Object value;
    
    /**
     * 标签
     */
    private Map<String, String> tags;
} 