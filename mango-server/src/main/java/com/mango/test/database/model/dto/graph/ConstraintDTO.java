package com.mango.test.database.model.dto.graph;

import lombok.Data;
import java.util.List;

/**
 * 图数据库约束DTO
 */
@Data
public class ConstraintDTO {
    
    /**
     * 约束名称
     */
    private String name;
    
    /**
     * 约束类型
     */
    private String type;
    
    /**
     * 约束所属标签
     */
    private String label;
    
    /**
     * 约束所属关系类型
     */
    private String relationType;
    
    /**
     * 约束属性列表
     */
    private List<String> properties;
    
    /**
     * 约束状态
     */
    private String state;
} 