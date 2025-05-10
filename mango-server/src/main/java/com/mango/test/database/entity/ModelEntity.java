package com.mango.test.database.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mango.test.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 模型实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("data_model")
public class ModelEntity extends BaseEntity implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /** 模型名称 */
    private String name;
    
    /** 模型类型（summary:汇总模型, detail:明细模型） */
    private String type;
    
    /** 负责人 */
    private String owner;
    
    /** 状态（draft:草稿, published:已发布, running:运行中, failed:运行失败） */
    private String status;
    
    /** 模型描述 */
    private String description;
    
    /** 模型配置（JSON格式） */
    private String config;
    
    /** 子节点 */
    @TableField(exist = false)
    private List<ModelEntity> children;
    
    /** 是否为叶子节点 */
    @TableField(exist = false)
    private Boolean isLeaf;

    /**
     * 标签，多个标签用逗号分隔
     */
    private String tags;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 已应用的标准IDs，多个标准ID用逗号分隔
     */
    private String appliedStandardIds;
    
    /**
     * 应用标准的范围（all:所有字段, selected:选中字段）
     */
    private String standardApplyScope;
    
    /**
     * 应用标准的说明
     */
    private String standardApplyDescription;
    
    /**
     * 应用标准的时间
     */
    private Date standardApplyTime;
    
    /**
     * 应用标准的用户
     */
    private String standardApplyUser;

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getAppliedStandardIds() {
        return appliedStandardIds;
    }

    public void setAppliedStandardIds(String appliedStandardIds) {
        this.appliedStandardIds = appliedStandardIds;
    }

    public String getStandardApplyScope() {
        return standardApplyScope;
    }

    public void setStandardApplyScope(String standardApplyScope) {
        this.standardApplyScope = standardApplyScope;
    }

    public String getStandardApplyDescription() {
        return standardApplyDescription;
    }

    public void setStandardApplyDescription(String standardApplyDescription) {
        this.standardApplyDescription = standardApplyDescription;
    }

    public Date getStandardApplyTime() {
        return standardApplyTime;
    }

    public void setStandardApplyTime(Date standardApplyTime) {
        this.standardApplyTime = standardApplyTime;
    }

    public String getStandardApplyUser() {
        return standardApplyUser;
    }

    public void setStandardApplyUser(String standardApplyUser) {
        this.standardApplyUser = standardApplyUser;
    }
} 