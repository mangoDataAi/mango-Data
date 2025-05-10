package com.mango.test.database.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mango.test.common.util.UuidUtil;
import com.mango.test.database.entity.ModelDesignEntity;
import com.mango.test.database.mapper.ModelDesignMapper;
import com.mango.test.database.service.IModelDesignService;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 模型设计服务实现类
 */
@Service
public class ModelDesignServiceImpl extends ServiceImpl<ModelDesignMapper, ModelDesignEntity> implements IModelDesignService {

    /**
     * 根据模型ID获取模型设计信息
     *
     * @param modelId 模型ID
     * @return 模型设计信息
     */
    @Override
    public ModelDesignEntity getByModelId(String modelId) {
        LambdaQueryWrapper<ModelDesignEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelDesignEntity::getModelId, modelId);
        return this.getOne(wrapper);
    }

    /**
     * 保存模型设计信息
     *
     * @param design 模型设计信息
     * @return 是否保存成功
     */
    @Override
    public boolean saveModelDesign(ModelDesignEntity design) {
        // 如果是新增，生成ID并设置创建时间
        if (design.getId() == null || design.getId().isEmpty()) {
            design.setId(UuidUtil.generateUuid());
            design.setCreateTime(new Date());
        }
        
        // 设置更新时间
        design.setUpdateTime(new Date());
        
        return this.saveOrUpdate(design);
    }
} 