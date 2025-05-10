package com.mango.test.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 系统角色实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_role")
public class SysRole implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 角色ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
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
     * 数据权限范围（ALL全部，CUSTOM自定义，DEPT本部门，DEPT_AND_CHILD本部门及以下，SELF仅本人）
     */
    private String dataScope;

    /**
     * 创建者
     */
    private String creator;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新者
     */
    private String updater;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 菜单权限ID集合，非数据库字段
     */
    @TableField(exist = false)
    private List<String> menuIds;

    /**
     * 部门ID集合，非数据库字段
     */
    @TableField(exist = false)
    private List<String> deptIds;
} 