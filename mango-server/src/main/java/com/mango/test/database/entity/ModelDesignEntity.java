package com.mango.test.database.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mango.test.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 模型设计实体类
 * 用于存储模型的设计信息，包括画布数据、字段定义、索引和关系等
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("data_model_design")
public class ModelDesignEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关联的模型ID
     */
    @TableField("model_id")
    private String modelId;

    /**
     * 画布数据（JSON格式）
     */
    @TableField("canvas")
    private String canvas;

    /**
     * 字段定义（JSON格式）
     */
    @TableField("fields")
    private String fields;

    /**
     * 索引定义（JSON格式）
     */
    @TableField("indexes")
    private String indexes;

    /**
     * 关系定义（JSON格式）
     */
    @TableField("relations")
    private String relations;

    /**
     * 其他配置信息（JSON格式）
     */
    @TableField("config")
    private String config;
} 