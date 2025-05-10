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

/**
 * 系统菜单实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_menu")
public class SysMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 菜单ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 父菜单ID
     */
    private String parentId;

    /**
     * 菜单名称
     */
    private String name;

    /**
     * 菜单类型（DIR目录，MENU菜单）
     */
    private String type;

    /**
     * 路由地址
     */
    private String path;

    /**
     * 组件路径
     */
    private String component;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 显示顺序
     */
    private Integer sort;

    /**
     * 菜单状态（1正常 0停用）
     */
    private Integer status;

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
     * 是否为外链（1是 0否）
     */
    private Integer isExternal;

    /**
     * 是否缓存（1是 0否）
     */
    private Integer isCache;

    /**
     * 是否可见（1是 0否）
     */
    private Integer isVisible;

    /**
     * 子菜单，非数据库字段
     */
    @TableField(exist = false)
    private java.util.List<SysMenu> children;
} 