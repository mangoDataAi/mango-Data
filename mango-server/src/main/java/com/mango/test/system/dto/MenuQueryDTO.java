package com.mango.test.system.dto;

import lombok.Data;

/**
 * 菜单查询参数
 */
@Data
public class MenuQueryDTO {
    
    /**
     * 当前页码
     */
    private Long current = 1L;
    
    /**
     * 每页大小
     */
    private Long size = 10L;
    
    /**
     * 菜单名称
     */
    private String name;
    
    /**
     * 菜单类型
     */
    private String type;
    
    /**
     * 菜单状态
     */
    private String status;
    
    /**
     * 是否查询树形结构
     */
    private Boolean isTree = true;
} 