package com.mango.test.database.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mango.test.database.entity.TaskTableField;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务表字段 Mapper 接口
 */
@Mapper
public interface TaskTableFieldMapper extends BaseMapper<TaskTableField> {
}