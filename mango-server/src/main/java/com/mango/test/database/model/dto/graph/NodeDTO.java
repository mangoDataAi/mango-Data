package com.mango.test.database.model.dto.graph;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 节点DTO
 */
@Data
public class NodeDTO {
    
    /**
     * 节点ID
     */
    private String id;
    
    /**
     * 节点标签列表
     */
    private List<String> labels;
    
    /**
     * 节点属性
     */
    private Map<String, Object> properties;
} 