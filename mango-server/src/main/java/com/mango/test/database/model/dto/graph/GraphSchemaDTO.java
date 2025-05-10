package com.mango.test.database.model.dto.graph;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 图数据库Schema DTO
 */
@Data
public class GraphSchemaDTO {
    
    /**
     * 节点标签列表
     */
    private List<NodeLabelDTO> nodeLabels;
    
    /**
     * 关系类型列表
     */
    private List<RelationshipTypeDTO> relationshipTypes;
    
    /**
     * 索引列表
     */
    private List<IndexDTO> indices;
    
    /**
     * 约束列表
     */
    private List<ConstraintDTO> constraints;
    
    /**
     * 图数据库额外属性
     */
    private Map<String, Object> properties;
} 