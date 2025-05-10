package com.mango.test.database.model.request;

import lombok.Data;

import java.util.List;

/**
 * 创建物化任务请求
 */
@Data
public class TaskCreateRequest {
    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 关联的模型IDs
     */
    private List<String> modelIds;

    /**
     * 调度类型：immediate, scheduled, periodic
     */
    private String scheduleType;

    /**
     * 定时执行时间
     */
    private String scheduledTime;

    /**
     * Cron表达式
     */
    private String cronExpression;

    /**
     * 发布环境：dev, test, staging, prod
     */
    private String environment;

    /**
     * 数据源类型：theme, custom
     */
    private String dataSourceType;

    /**
     * 主题域ID
     */
    private String domainId;

    /**
     * 主题域名称
     */
    private String domainName;

    /**
     * 自定义数据源ID
     */
    private String customDataSourceId;

    /**
     * 自定义数据源名称
     */
    private String customDataSourceName;

    /**
     * 物化策略：complete, table_incremental
     */
    private String materializeStrategy;

    /**
     * 刷新方式：rebuild, update
     */
    private String refreshMethod;

    /**
     * 执行模式：serial, parallel, dependency
     */
    private String executionMode;

    /**
     * 错误处理：stop, continue, log
     */
    private String errorHandling;

    /**
     * 并行度
     */
    private Integer parallelism;

    /**
     * 超时时间(分钟)
     */
    private Integer timeout;

    /**
     * 表配置列表
     */
    private List<TableConfig> tableConfigs;

    /**
     * 表配置
     */
    @Data
    public static class TableConfig {
        /**
         * 表ID
         */
        private String tableId;

        /**
         * 表名称
         */
        private String tableName;

        /**
         * 模型ID
         */
        private String modelId;

        /**
         * 是否主表
         */
        private Boolean isMain;

        /**
         * 物化类型：full, incremental
         */
        private String materializeType;

        /**
         * 增量字段
         */
        private String incrementalField;

        /**
         * 写入模式：append, overwrite
         */
        private String writeMode;

        /**
         * 依赖表ID列表
         */
        private List<String> dependencies;
    }
} 