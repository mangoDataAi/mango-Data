package com.mango.test.database.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mango.test.database.entity.DimensionAttribute;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface DimensionAttributeMapper extends BaseMapper<DimensionAttribute> {
    
    List<DimensionAttribute> selectByDimensionId(@Param("dimensionId") String dimensionId);
    
}