package com.mango.test.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mango.test.database.entity.ModelStandard;
import java.util.List;
import java.util.Map;

/**
 * 模型-标准关联服务接口
 */
public interface IModelStandardService extends IService<ModelStandard> {
    
    /**
     * 应用标准到模型
     * @param modelId 模型ID
     * @param standards 标准列表
     * @param applyScope 应用范围
     * @param description 应用说明
     * @return 是否成功
     */
    boolean applyStandards(String modelId, List<Map<String, Object>> standards, String applyScope, String description);
    
    /**
     * 获取模型已应用的标准
     * @param modelId 模型ID
     * @return 已应用的标准列表
     */
    List<ModelStandard> getModelStandards(String modelId);
    
    /**
     * 删除模型的标准
     * @param modelId 模型ID
     * @return 是否成功
     */
    boolean deleteModelStandards(String modelId);

} 