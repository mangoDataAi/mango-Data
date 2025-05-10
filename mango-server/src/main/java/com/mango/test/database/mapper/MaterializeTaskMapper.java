package com.mango.test.database.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mango.test.database.entity.MaterializeTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 物化任务Mapper接口
 */
@Mapper
public interface MaterializeTaskMapper extends BaseMapper<MaterializeTask> {
} 