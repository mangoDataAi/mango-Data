package com.mango.test.database.model.request;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 创建版本请求
 */
@Data
public class VersionCreateRequest {
    /**
     * 关联的任务ID
     */
    private String taskId;

    /**
     * 关联的模型ID
     */
    private String modelId;

    /**
     * 发布环境
     */
    private String environment;

    /**
     * 发布描述
     */
    private String description;

    /**
     * 发布方式：full, incremental
     */
    private String mode;

    /**
     * 变更内容
     */
    private List<Map<String, Object>> changes;

    /**
     * 配置信息
     */
    private Map<String, Object> config;
} 