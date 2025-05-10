package com.mango.test.database.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mango.test.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("data_domain_domain")
public class Domain extends BaseEntity {
    
    /**
     * 主题域名称
     */
    private String name;
    
    /**
     * 数据源类型(delta/starrocks/platform)
     */
    private String sourceType;
    
    /**
     * 数据源ID
     */
    private String dataSourceId;
    
    /**
     * 负责人
     */
    private String owner;
    
    /**
     * 状态(enabled/disabled)
     */
    private String status;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 父级域ID
     */
    private String parentId;

    /**
     * 子域列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<Domain> children;

    /**
     * 注释（非数据库字段）
     */
    @TableField(exist = false)
    private String comment;
    
    /**
     * 额外信息（非数据库字段）
     */
    @TableField(exist = false)
    private Map<String, Object> extraInfo;
    
    /**
     * 物理表创建时间（非数据库字段）
     */
    @TableField(exist = false)
    private Date physicalCreateTime;
    
    /**
     * 物理表更新时间（非数据库字段）
     */
    @TableField(exist = false)
    private Date physicalUpdateTime;

    public List<Domain> getChildren() {
        return children;
    }

    public void setChildren(List<Domain> children) {
        this.children = children;
    }
    
    public String getComment() {
        return comment;
    }
    
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    public Map<String, Object> getExtraInfo() {
        if (extraInfo == null) {
            extraInfo = new HashMap<>();
        }
        return extraInfo;
    }
    
    public void setExtraInfo(Map<String, Object> extraInfo) {
        this.extraInfo = extraInfo;
    }
    
    public Date getPhysicalCreateTime() {
        return physicalCreateTime;
    }
    
    public void setPhysicalCreateTime(Date createTime) {
        this.physicalCreateTime = createTime;
    }
    
    public Date getPhysicalUpdateTime() {
        return physicalUpdateTime;
    }
    
    public void setPhysicalUpdateTime(Date updateTime) {
        this.physicalUpdateTime = updateTime;
    }
} 