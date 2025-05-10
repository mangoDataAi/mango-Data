package com.mango.test.database.model.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 任务历史查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TaskHistoryQueryRequest extends TaskQueryRequest {
    /**
     * 是否包含版本信息
     */
    private boolean includeVersions = false;
    
    /**
     * 任务名称
     */
    private String taskName;
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 开始时间
     */
    private LocalDateTime startDateTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endDateTime;
    
    /**
     * 页码
     */
    private Integer pageNum = 1;
    
    /**
     * 每页大小
     */
    private Integer pageSize = 10;
    
    // 添加其他需要的字段
} 