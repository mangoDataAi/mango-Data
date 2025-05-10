package com.mango.test.database.model.request;

import lombok.Data;

/**
 * 版本回滚请求
 */
@Data
public class VersionRollbackRequest {
    /**
     * 要回滚的版本ID
     */
    private String versionId;
    
    /**
     * 要回滚到的版本ID
     */
    private String targetVersionId;
    
    /**
     * 创建回滚版本的任务ID
     */
    private String taskId;
    
    /**
     * 回滚原因
     */
    private String reason;
    
    /**
     * 是否执行回滚后的任务
     */
    private Boolean executeAfterRollback = true;
    
    /**
     * 是否保留原始版本
     */
    private Boolean keepOriginalVersion = true;
} 