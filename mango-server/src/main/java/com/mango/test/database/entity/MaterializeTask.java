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
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 物化任务实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "data_materialize_task", autoResultMap = true)
public class MaterializeTask extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务名称
     */
    @TableField("task_name")
    private String taskName;

    /**
     * 任务描述
     */
    @TableField("description")
    private String description;

    /**
     * 任务状态：WAITING, RUNNING, COMPLETED, FAILED, CANCELLED, SCHEDULED, PARTIAL
     */
    @TableField("status")
    private String status;

    /**
     * 调度类型：immediate, scheduled, periodic
     */
    @TableField("schedule_type")
    private String scheduleType;

    /**
     * 定时执行时间
     */
    @TableField("scheduled_time")
    private LocalDateTime scheduledTime;

    /**
     * Cron表达式，用于周期执行
     */
    @TableField("cron_expression")
    private String cronExpression;

    /**
     * 发布环境：dev, test, staging, prod
     */
    @TableField("environment")
    private String environment;

    /**
     * 物化策略：complete, table_incremental
     */
    @TableField("materialize_strategy")
    private String materializeStrategy;

    /**
     * 数据源类型：theme, custom
     */
    @TableField("data_source_type")
    private String dataSourceType;

    /**
     * 主题域ID
     */
    @TableField("domain_id")
    private String domainId;

    /**
     * 主题域名称
     */
    @TableField("domain_name")
    private String domainName;

    /**
     * 自定义数据源ID
     */
    @TableField("custom_data_source_id")
    private String customDataSourceId;

    /**
     * 自定义数据源名称
     */
    @TableField("custom_data_source_name")
    private String customDataSourceName;

    /**
     * 刷新方式：rebuild, update
     */
    @TableField("refresh_method")
    private String refreshMethod;

    /**
     * 执行模式：serial, parallel, dependency
     */
    @TableField("execution_mode")
    private String executionMode;

    /**
     * 错误处理：stop, continue, log
     */
    @TableField("error_handling")
    private String errorHandling;

    /**
     * 物化顺序：auto, defined, parallel
     */
    @TableField("materialize_order")
    private String materializeOrder;

    /**
     * 并行度
     */
    @TableField("parallelism")
    private Integer parallelism;

    /**
     * 超时时间(分钟)
     */
    @TableField("timeout")
    private Integer timeout;

    /**
     * 进度(0-100)
     */
    @TableField("progress")
    private Integer progress;

    /**
     * 开始执行时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 结束执行时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 创建人
     */
    @TableField("creator")
    private String creator;

    /**
     * 关联的模型IDs
     */
    @TableField(value = "model_ids", typeHandler = JacksonTypeHandler.class)
    private List<String> modelIds;

    /**
     * 版本列表
     */
    @TableField(exist = false)
    private List<TaskVersion> versions;

    /**
     * 版本统计信息
     */
    @TableField(exist = false)
    private Map<String, Integer> versionStats;

    /**
     * 错误消息
     */
    @TableField("error_message")
    private String errorMessage;
    
    /**
     * 模型详情列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<Map<String, Object>> models;
    
    /**
     * 任务执行日志（非数据库字段）
     */
    @TableField(exist = false)
    private String logs;

    /**
     * 获取模型 ID 列表
     * 处理不同情况：List<String>、逗号分隔的 String 以及 null
     */
    public List<String> getModelIdList() {
        if (this.modelIds == null) {
            return Collections.emptyList();
        }
        return this.modelIds;
    }

    /**
     * 获取模型 ID 的逗号分隔字符串
     */
    public String getModelIdsString() {
        if (this.modelIds == null || this.modelIds.isEmpty()) {
            return "";
        }
        return String.join(",", this.modelIds);
    }
}