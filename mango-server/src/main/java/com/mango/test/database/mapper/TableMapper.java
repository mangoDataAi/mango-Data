package com.mango.test.database.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mango.test.database.entity.Table;
import org.apache.ibatis.annotations.Mapper;

/**
 * 表Mapper接口
 */
@Mapper
public interface TableMapper extends BaseMapper<Table> {
}