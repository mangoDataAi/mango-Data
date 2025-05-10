package com.mango.test.database.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mango.test.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.util.Date;

/**
 * 模型-标准关联实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("data_model_standard")
public class ModelStandard extends BaseEntity implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 模型ID
     */
    private String modelId;
    
    /**
     * 标准ID
     */
    private String standardId;
    
    /**
     * 标准名称
     */
    private String standardName;
    
    /**
     * 标准类型（required:必须执行, suggested:建议执行）
     */
    private String standardType;
    
    /**
     * 标准规则类型
     */
    private String ruleType;
    
    /**
     * 应用范围（all:所有字段, selected:选中字段）
     */
    private String applyScope;
    
    /**
     * 应用说明
     */
    private String description;
    
    /**
     * 应用时间
     */
    private Date applyTime;
    
    /**
     * 应用用户
     */
    private String applyUser;
    
    /**
     * 状态（1:有效, 0:无效）
     */
    private Integer status;
    
    /**
     * 字段ID（当应用范围为selected时使用）
     */
    private String fieldId;
} 