package com.mango.test.database.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mango.test.database.entity.TaskLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务日志Mapper接口
 */
@Mapper
public interface TaskLogMapper extends BaseMapper<TaskLog> {
} 