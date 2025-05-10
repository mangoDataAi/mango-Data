package com.mango.test.system.dto;

import lombok.Data;

/**
 * 系统角色查询条件
 */
@Data
public class SysRoleQueryDTO {
    
    /**
     * 角色名称
     */
    private String name;
    
    /**
     * 角色编码
     */
    private String code;
    
    /**
     * 角色状态（1正常 0停用）
     */
    private Integer status;
    
    /**
     * 创建时间开始
     */
    private String beginTime;
    
    /**
     * 创建时间结束
     */
    private String endTime;
    
    /**
     * 当前页
     */
    private Integer current = 1;
    
    /**
     * 每页条数
     */
    private Integer size = 10;
} 