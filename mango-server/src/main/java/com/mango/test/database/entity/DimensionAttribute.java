package com.mango.test.database.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mango.test.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("data_domain_dimension_attribute")
public class DimensionAttribute extends BaseEntity {
    
    private String dimensionId;
    
    private String name;
    
    private String code;
    
    private String dataType;
    
    private Integer length;
    
    private Boolean isPrimary;
    
    private String description;
} 