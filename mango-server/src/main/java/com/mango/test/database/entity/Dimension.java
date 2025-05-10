package com.mango.test.database.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.mango.test.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("data_domain_dimension")
public class Dimension extends BaseEntity {
    
    private String name;
    
    private String type;
    
    @TableField("source_type")
    private String sourceType;
    
    @TableField("domain_id")
    private String domainId;
    
    @TableField("domain_name")
    private String domainName;
    
    private String description;
    
    @TableField("attribute_count")
    private Integer attributeCount;
    
    @TableField("update_strategy")
    private String updateStrategy;
    
    @TableField("last_update_time")
    private java.util.Date lastUpdateTime;

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }
} 