package com.mango.test.database.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.mango.test.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 任务版本实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "data_task_version", autoResultMap = true)
public class TaskVersion extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关联的任务ID
     */
    @TableField("task_id")
    private String taskId;

    /**
     * 关联的模型ID
     */
    @TableField("model_id")
    private String modelId;

    /**
     * 关联的数据源ID
     */
    @TableField("data_source_id")
    private String dataSourceId;

    /**
     * 关联的数据库ID
     */
    @TableField("database_id")
    private String databaseId;

    /**
     * 版本号
     */
    @TableField("version_num")
    private String versionNum;

    /**
     * 发布环境
     */
    @TableField("environment")
    private String environment;

    /**
     * 发布状态：success, failed, running
     */
    @TableField("status")
    private String status;

    /**
     * 发布者
     */
    @TableField("publisher")
    private String publisher;

    /**
     * 发布时间
     */
    @TableField("publish_time")
    private LocalDateTime publishTime;

    /**
     * 描述
     */
    @TableField("description")
    private String description;

    /**
     * 发布方式：full, incremental
     */
    @TableField("mode")
    private String mode;

    /**
     * 是否为当前版本
     */
    @TableField("is_current")
    private Boolean isCurrent;

    /**
     * 是否为回滚版本
     */
    @TableField("is_rollback")
    private Boolean isRollback;

    /**
     * 回滚源版本ID
     */
    @TableField("rollback_from_version_id")
    private String rollbackFromVersionId;

    /**
     * 变更内容
     */
    @TableField(value = "changes", typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> changes;

    /**
     * 配置信息
     */
    @TableField(value = "config", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> config;

    /**
     * 日志信息
     */
    @TableField(value = "logs", typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> logs;
} 