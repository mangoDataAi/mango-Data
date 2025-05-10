package com.mango.test.database.model.dto.graph;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 获取节点邻居请求DTO
 */
@Data
public class NeighborRequestDTO {
    
    /**
     * 节点ID
     */
    private String nodeId;
    
    /**
     * 节点标签
     */
    private List<String> nodeLabels;
    
    /**
     * 关系类型
     */
    private List<String> relationshipTypes;
    
    /**
     * 关系方向 (INCOMING, OUTGOING, BOTH)
     */
    private String direction;
    
    /**
     * 深度
     */
    private Integer depth;
    
    /**
     * 限制数量
     */
    private Integer limit;
    
    /**
     * 属性过滤条件
     */
    private Map<String, Object> propertyFilters;
} 