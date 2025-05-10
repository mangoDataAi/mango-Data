package com.mango.test.database.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mango.test.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务日志实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("data_task_log")
public class TaskLog extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    @TableField("task_id")
    private String taskId;

    /**
     * 版本ID
     */
    @TableField("version_id")
    private String versionId;

    /**
     * 日志级别: INFO, WARN, ERROR, DEBUG
     */
    @TableField("log_level")
    private String logLevel;

    /**
     * 日志内容
     */
    @TableField("content")
    private String content;

    /**
     * 操作类型: CREATE, UPDATE, EXECUTE, CANCEL, 等
     */
    @TableField("operation_type")
    private String operationType;

    /**
     * 操作人
     */
    @TableField("operator")
    private String operator;

    /**
     * 相关表ID
     */
    @TableField("table_id")
    private String tableId;

    /**
     * 相关表名称
     */
    @TableField("table_name")
    private String tableName;

    /**
     * 详细信息
     */
    @TableField("details")
    private String details;
} 