package com.mango.test.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mango.test.database.entity.ModelDesignEntity;

/**
 * 模型设计服务接口
 * 定义模型设计相关的服务方法
 */
public interface IModelDesignService extends IService<ModelDesignEntity> {

    /**
     * 根据模型ID获取模型设计信息
     *
     * @param modelId 模型ID
     * @return 模型设计信息
     */
    ModelDesignEntity getByModelId(String modelId);

    /**
     * 保存模型设计信息
     *
     * @param design 模型设计信息
     * @return 是否保存成功
     */
    boolean saveModelDesign(ModelDesignEntity design);
} 