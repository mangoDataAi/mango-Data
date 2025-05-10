package com.mango.test.database.model.dto.graph;

import lombok.Data;
import java.util.Map;

/**
 * 图数据库查询请求DTO
 */
@Data
public class QueryRequestDTO {
    
    /**
     * 查询语句
     */
    private String query;
    
    /**
     * 查询参数
     */
    private Map<String, Object> parameters;
    
    /**
     * 查询结果限制
     */
    private Integer limit;
    
    /**
     * 结果偏移量
     */
    private Integer offset;
    
    /**
     * 查询超时时间(毫秒)
     */
    private Long timeout;
}