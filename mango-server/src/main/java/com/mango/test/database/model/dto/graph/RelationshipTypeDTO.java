package com.mango.test.database.model.dto.graph;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 关系类型DTO
 */
@Data
public class RelationshipTypeDTO {
    
    /**
     * 关系类型名称
     */
    private String name;
    
    /**
     * 关系类型描述
     */
    private String description;
    
    /**
     * 起点节点标签
     */
    private List<String> startNodeLabels;
    
    /**
     * 终点节点标签
     */
    private List<String> endNodeLabels;
    
    /**
     * 关系属性定义
     */
    private List<PropertyDefinitionDTO> properties;
    
    /**
     * 关系数量
     */
    private Long count;
    
    /**
     * 额外属性
     */
    private Map<String, Object> extra;
} 