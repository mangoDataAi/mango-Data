package com.mango.test.database.model.dto.graph;

import lombok.Data;
import java.util.Map;

/**
 * 关系DTO
 */
@Data
public class RelationshipDTO {
    
    /**
     * 关系ID
     */
    private String id;
    
    /**
     * 关系类型
     */
    private String type;
    
    /**
     * 起点节点ID
     */
    private String startNodeId;
    
    /**
     * 终点节点ID
     */
    private String endNodeId;
    
    /**
     * 关系属性
     */
    private Map<String, Object> properties;
} 