package com.mango.test.database.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mango.test.database.entity.Domain;
import org.apache.ibatis.annotations.Mapper;

/**
 * 主题域Mapper接口
 */
@Mapper
public interface DomainMapper extends BaseMapper<Domain> {
    

}