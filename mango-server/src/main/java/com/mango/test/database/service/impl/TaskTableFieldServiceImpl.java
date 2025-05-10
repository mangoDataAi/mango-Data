package com.mango.test.database.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mango.test.database.entity.TaskTableField;
import com.mango.test.database.mapper.TaskTableFieldMapper;
import com.mango.test.database.service.TaskTableFieldService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskTableFieldServiceImpl extends ServiceImpl<TaskTableFieldMapper, TaskTableField> implements TaskTableFieldService {
    
    /**
     * 根据表ID获取字段列表
     * @param tableId 表ID
     * @return 字段列表
     */
    @Override
    public List<TaskTableField> listByTableId(String tableId) {
        LambdaQueryWrapper<TaskTableField> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TaskTableField::getTableId, tableId);
        return list(queryWrapper);
    }
}