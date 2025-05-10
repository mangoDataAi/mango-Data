package com.mango.test.database.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mango.test.database.entity.Group;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GroupMapper extends BaseMapper<Group> {
} 