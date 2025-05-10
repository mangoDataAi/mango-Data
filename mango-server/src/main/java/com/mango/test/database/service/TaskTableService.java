package com.mango.test.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mango.test.database.entity.TaskTable;

import java.util.List;

/**
 * 任务表配置服务接口
 */
public interface TaskTableService extends IService<TaskTable> {
    
    /**
     * 根据任务ID获取表配置
     * @param taskId 任务ID
     * @return 表配置列表
     */
    List<TaskTable> getTablesByTaskId(String taskId);
    
    /**
     * 根据模型ID和任务ID获取表配置
     * @param taskId 任务ID
     * @param modelId 模型ID
     * @return 表配置列表
     */
    List<TaskTable> getTablesByTaskIdAndModelId(String taskId, String modelId);
    
    /**
     * 根据模型ID获取表配置
     * @param modelId 模型ID
     * @return 表配置列表
     */
    List<TaskTable> getTablesByModelId(String modelId);
    
    /**
     * 更新表配置
     * @param tables 表配置列表
     * @return 是否成功
     */
    boolean updateTables(List<TaskTable> tables);
    
    /**
     * 根据版本ID获取表配置
     * @param versionId 版本ID
     * @return 表配置列表
     */
    List<TaskTable> getTablesByVersionId(String versionId);
} 