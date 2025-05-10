package com.mango.test.database.model.dto.graph;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 图数据库查询结果DTO
 */
@Data
public class QueryResultDTO {
    
    /**
     * 列名列表
     */
    private List<String> columns;
    
    /**
     * 查询结果
     */
    private List<Map<String, Object>> data;
    
    /**
     * 图数据
     */
    private GraphDataDTO graph;
    
    /**
     * 统计信息
     */
    private Map<String, Object> stats;
    
    /**
     * 警告信息
     */
    private List<String> warnings;
    
    /**
     * 查询执行时间(毫秒)
     */
    private Long executionTime;
    
    /**
     * 是否还有更多结果
     */
    private boolean hasMore;
} 