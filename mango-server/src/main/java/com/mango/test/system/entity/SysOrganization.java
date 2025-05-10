package com.mango.test.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * 机构表实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_organization")
public class SysOrganization extends Model<SysOrganization> {

    /**
     * 主键ID
     */
    @TableId
    private String id;

    /**
     * 父级ID
     */
    private String parentId;

    /**
     * 机构名称
     */
    private String name;

    /**
     * 机构编码
     */
    private String code;

    /**
     * 机构类型（1-公司，2-部门，3-小组，4-其他）
     */
    private String type;

    /**
     * 负责人
     */
    private String leader;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 排序号
     */
    private Integer sort;

    /**
     * 状态（1-正常，0-禁用）
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 更新人
     */
    private String updater;
    
    /**
     * 子机构列表，不映射到数据库
     */
    @TableField(exist = false)
    private List<SysOrganization> children;
} 