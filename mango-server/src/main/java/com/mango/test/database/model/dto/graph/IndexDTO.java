package com.mango.test.database.model.dto.graph;

import lombok.Data;
import java.util.List;

/**
 * 图数据库索引DTO
 */
@Data
public class IndexDTO {
    
    /**
     * 索引名称
     */
    private String name;
    
    /**
     * 索引类型
     */
    private String type;
    
    /**
     * 索引所属标签
     */
    private String label;
    
    /**
     * 索引所属关系类型
     */
    private String relationType;
    
    /**
     * 索引属性列表
     */
    private List<String> properties;
    
    /**
     * 是否唯一
     */
    private boolean unique;
    
    /**
     * 索引状态
     */
    private String state;
}