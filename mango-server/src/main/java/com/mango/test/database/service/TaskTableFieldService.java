package com.mango.test.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mango.test.database.entity.TaskTableField;

import java.util.List;

public interface TaskTableFieldService extends IService<TaskTableField> {
    /**
     * 根据表ID获取字段列表
     * @param tableId 表ID
     * @return 字段列表
     */
    List<TaskTableField> listByTableId(String tableId);
} 