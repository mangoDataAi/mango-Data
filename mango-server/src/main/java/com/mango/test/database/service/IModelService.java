package com.mango.test.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mango.test.database.entity.ModelEntity;
import java.util.List;
import java.util.Map;

/**
 * 模型服务接口
 */
public interface IModelService extends IService<ModelEntity> {
    
    /**
     * 查询模型列表
     * 
     * @param model 模型信息
     * @return 模型集合
     */
    List<ModelEntity> selectModelList(ModelEntity model);
    

    /**
     * 发布模型
     * 
     * @param id 模型ID
     * @return 结果
     */
    boolean publishModel(String id);
    
    /**
     * 运行模型
     * 
     * @param id 模型ID
     * @return 结果
     */
    boolean runModel(String id);
    
    /**
     * 根据模型ID获取物理表列表
     * 
     * @param modelId 模型ID
     * @return 物理表列表
     */
    List<Map<String, Object>> getPhysicalTablesByModelId(String modelId);

    /**
     * 根据表ID获取表字段信息
     * 
     * @param tableId 表ID
     * @return 字段信息列表
     */
    List<Map<String, Object>> getTableFieldsByTableId(String tableId);
} 