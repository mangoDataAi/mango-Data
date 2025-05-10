package com.mango.test.database.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mango.test.database.entity.ModelEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 模型数据库操作接口
 */
public interface ModelMapper extends BaseMapper<ModelEntity> {

    /**
     * 根据模型ID获取关联的物理表
     * 
     * @param modelId 模型ID
     * @return 物理表列表
     */
    @Select("SELECT t.id, t.name, t.display_name as displayName, " +
           "t.field_count as fieldCount, t.create_time as createTime, t.update_time as updateTime " +
           "FROM t_table t " +
           "WHERE t.model_id = #{modelId}")
    List<Map<String, Object>> getPhysicalTablesByModelId(@Param("modelId") String modelId);
}