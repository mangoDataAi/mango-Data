package com.mango.test.database.model.dto.graph;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 节点标签DTO
 */
@Data
public class NodeLabelDTO {
    
    /**
     * 标签名称
     */
    private String name;
    
    /**
     * 标签描述
     */
    private String description;
    
    /**
     * 标签属性定义
     */
    private List<PropertyDefinitionDTO> properties;
    
    /**
     * 节点数量
     */
    private Long count;
    
    /**
     * 额外属性
     */
    private Map<String, Object> extra;
} 