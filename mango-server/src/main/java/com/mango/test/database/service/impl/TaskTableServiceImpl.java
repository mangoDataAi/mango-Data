package com.mango.test.database.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mango.test.database.entity.TaskTable;
import com.mango.test.database.mapper.TaskTableMapper;
import com.mango.test.database.service.TaskTableService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 任务表配置服务实现类
 */
@Service
public class TaskTableServiceImpl extends ServiceImpl<TaskTableMapper, TaskTable> implements TaskTableService {

    @Override
    public List<TaskTable> getTablesByTaskId(String taskId) {
        LambdaQueryWrapper<TaskTable> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TaskTable::getTaskId, taskId);

        return list(queryWrapper);
    }

    @Override
    public List<TaskTable> getTablesByTaskIdAndModelId(String taskId, String modelId) {
        LambdaQueryWrapper<TaskTable> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TaskTable::getTaskId, taskId)
                .eq(TaskTable::getModelId, modelId);

        return list(queryWrapper);
    }

    @Override
    public List<TaskTable> getTablesByModelId(String modelId) {
        LambdaQueryWrapper<TaskTable> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TaskTable::getModelId, modelId);
        
        return list(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTables(List<TaskTable> tables) {
        if (tables == null || tables.isEmpty()) {
            return false;
        }

        // 更新时间
        tables.forEach(table -> table.setUpdateTime(new Date()));

        return updateBatchById(tables);
    }

    /**
     * 根据版本ID获取表配置
     * @param versionId 版本ID
     * @return 表配置列表
     */
    @Override
    public List<TaskTable> getTablesByVersionId(String versionId) {
        LambdaQueryWrapper<TaskTable> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TaskTable::getVersionId, versionId);
        return list(queryWrapper);
    }
}