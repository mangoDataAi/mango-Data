package com.mango.test.database.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mango.test.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("data_task_table_field")
public class TaskTableField extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 版本ID
     */
    private String versionId;
    
    /**
     * 任务表ID
     */
    private String tableId;
    
    /**
     * 字段名
     */
    private String fieldName;
    
    /**
     * 字段类型
     */
    private String fieldType;
    
    /**
     * 字段长度
     */
    private Integer length;
    
    /**
     * 精度
     */
    private Integer precision;
    
    /**
     * 小数位
     */
    private Integer scale;
    
    /**
     * 是否主键 (1:是 0:否)
     */
    private Integer isPrimary;
    
    /**
     * 是否允许为空 (1:是 0:否)
     */
    private Integer notNull;
    
    /**
     * 是否自增 (1:是 0:否)
     */
    @TableField("is_auto_incr")
    private Integer autoIncrement;
    
    /**
     * 默认值
     */
    private String defaultValue;
    
    /**
     * 字段注释
     */
    private String fieldComment;
    
    /**
     * 字段顺序
     */
    private Integer fieldOrder;
} 