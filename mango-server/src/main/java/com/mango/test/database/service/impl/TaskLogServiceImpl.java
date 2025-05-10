package com.mango.test.database.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mango.test.database.entity.TaskLog;
import com.mango.test.database.mapper.TaskLogMapper;
import com.mango.test.database.service.TaskLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 任务日志服务实现类
 */
@Service
public class TaskLogServiceImpl extends ServiceImpl<TaskLogMapper, TaskLog> implements TaskLogService {

    @Override
    public boolean logTaskCreation(String taskId, String versionId, String content, String operator) {
        return logTask(taskId, versionId, "INFO", content, "CREATE", operator, null);
    }

    @Override
    public boolean logTask(String taskId, String versionId, String logLevel, String content, String operationType, String operator, String details) {
        TaskLog taskLog = new TaskLog();
        taskLog.setTaskId(taskId);
        taskLog.setVersionId(versionId);
        taskLog.setLogLevel(logLevel);
        taskLog.setContent(content);
        taskLog.setOperationType(operationType);
        taskLog.setOperator(operator);
        taskLog.setDetails(details);
        taskLog.setCreateTime(new Date());
        return save(taskLog);
    }

    @Override
    public boolean logTaskTable(String taskId, String versionId, String logLevel, String content, String operationType, 
                               String operator, String tableId, String tableName, String details) {
        TaskLog taskLog = new TaskLog();
        taskLog.setTaskId(taskId);
        taskLog.setVersionId(versionId);
        taskLog.setLogLevel(logLevel);
        taskLog.setContent(content);
        taskLog.setOperationType(operationType);
        taskLog.setOperator(operator);
        taskLog.setTableId(tableId);
        taskLog.setTableName(tableName);
        taskLog.setDetails(details);
        taskLog.setCreateTime(new Date());
        return save(taskLog);
    }

    @Override
    public List<TaskLog> getTaskLogs(String taskId) {
        LambdaQueryWrapper<TaskLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskLog::getTaskId, taskId);
        wrapper.orderByDesc(TaskLog::getCreateTime);
        return list(wrapper);
    }

    @Override
    public List<TaskLog> getVersionLogs(String versionId) {
        LambdaQueryWrapper<TaskLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskLog::getVersionId, versionId);
        wrapper.orderByDesc(TaskLog::getCreateTime);
        return list(wrapper);
    }
} 