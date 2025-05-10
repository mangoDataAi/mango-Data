package com.mango.test.database.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mango.test.database.entity.TaskTable;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务表配置Mapper接口
 */
@Mapper
public interface TaskTableMapper extends BaseMapper<TaskTable> {
}