package com.mango.test.database.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mango.test.database.entity.ModelStandard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 模型-标准关联Mapper接口
 */
@Mapper
public interface ModelStandardMapper extends BaseMapper<ModelStandard> {
    
    /**
     * 批量插入模型-标准关联
     * @param modelStandards 模型-标准关联列表
     * @return 插入数量
     */
    int batchInsert(@Param("list") List<ModelStandard> modelStandards);
    
    /**
     * 根据模型ID删除关联
     * @param modelId 模型ID
     * @return 删除数量
     */
    int deleteByModelId(@Param("modelId") String modelId);
} 