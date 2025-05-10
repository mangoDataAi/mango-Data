package com.mango.test.database.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mango.test.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("data_domain_dimension_standard")
public class DimensionStandard extends BaseEntity {
    
    private String dimensionId;
    
    private String standardId;
    
    private String standardType;
    
    private String priority;
    
    private String description;
} 