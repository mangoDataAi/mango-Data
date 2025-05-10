package com.mango.test.database.model.dto.graph;

import lombok.Data;

/**
 * 属性定义DTO
 */
@Data
public class PropertyDefinitionDTO {
    
    /**
     * 属性名称
     */
    private String name;
    
    /**
     * 属性类型
     */
    private String type;
    
    /**
     * 属性描述
     */
    private String description;
    
    /**
     * 是否必需
     */
    private boolean required;
    
    /**
     * 默认值
     */
    private Object defaultValue;
    
    /**
     * 是否索引
     */
    private boolean indexed;
    
    /**
     * 是否唯一
     */
    private boolean unique;
} 