package com.mango.test.database.model.request;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务查询请求
 */
@Data
public class TaskQueryRequest {
    /**
     * 页码
     */
    private Integer pageNum = 1;
    
    /**
     * 每页大小
     */
    private Integer pageSize = 10;
    
    /**
     * 任务名称（模糊搜索）
     */
    private String taskName;
    
    /**
     * 任务状态
     */
    private String status;
    
    /**
     * 发布环境
     */
    private String environment;
    
    /**
     * 创建者
     */
    private String creator;
    
    /**
     * 模型ID
     */
    private String modelId;
    
    /**
     * 开始时间
     */
    private String startTime;
    
    /**
     * 结束时间
     */
    private String endTime;
} 