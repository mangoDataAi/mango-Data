package com.mango.test.system.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 系统角色数据传输对象
 */
@Data
public class SysRoleDTO {
    
    /**
     * 角色ID
     */
    private String id;
    
    /**
     * 角色名称
     */
    private String name;
    
    /**
     * 角色编码
     */
    private String code;
    
    /**
     * 角色颜色
     */
    private String color;
    
    /**
     * 显示顺序
     */
    private Integer sort;
    
    /**
     * 角色状态（1正常 0停用）
     */
    private Integer status;
    
    /**
     * 数据权限范围
     */
    private String dataScope;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 菜单权限ID集合
     */
    private List<String> menuIds;
    
    /**
     * 部门ID集合
     */
    private List<String> deptIds;
} 