package com.mango.test.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mango.test.database.entity.TaskLog;

/**
 * 任务日志服务接口
 */
public interface TaskLogService extends IService<TaskLog> {

    /**
     * 记录任务创建日志
     * @param taskId 任务ID
     * @param versionId 版本ID
     * @param content 日志内容
     * @param operator 操作人
     * @return 是否成功
     */
    boolean logTaskCreation(String taskId, String versionId, String content, String operator);

    /**
     * 记录任务日志
     * @param taskId 任务ID
     * @param versionId 版本ID
     * @param logLevel 日志级别
     * @param content 日志内容
     * @param operationType 操作类型
     * @param operator 操作人
     * @param details 详细信息
     * @return 是否成功
     */
    boolean logTask(String taskId, String versionId, String logLevel, String content, String operationType, String operator, String details);

    /**
     * 记录任务表相关日志
     * @param taskId 任务ID
     * @param versionId 版本ID
     * @param logLevel 日志级别
     * @param content 日志内容
     * @param operationType 操作类型
     * @param operator 操作人
     * @param tableId 表ID
     * @param tableName 表名称
     * @param details 详细信息
     * @return 是否成功
     */
    boolean logTaskTable(String taskId, String versionId, String logLevel, String content, String operationType, 
                           String operator, String tableId, String tableName, String details);

    /**
     * 获取任务的所有日志
     * @param taskId 任务ID
     * @return 日志列表
     */
    java.util.List<TaskLog> getTaskLogs(String taskId);

    /**
     * 获取特定版本的日志
     * @param versionId 版本ID
     * @return 日志列表
     */
    java.util.List<TaskLog> getVersionLogs(String versionId);
} 