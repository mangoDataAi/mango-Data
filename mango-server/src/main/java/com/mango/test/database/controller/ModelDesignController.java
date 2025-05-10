package com.mango.test.database.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mango.test.common.util.UuidUtil;
import com.mango.test.database.entity.ModelDesignEntity;
import com.mango.test.database.entity.ModelEntity;
import com.mango.test.database.entity.ModelStandard;
import com.mango.test.database.service.IModelDesignService;
import com.mango.test.database.service.IModelService;
import com.mango.test.database.service.IModelStandardService;
import com.mango.test.vo.R;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型设计控制器
 * 处理模型设计相关的API请求
 */
@RestController
@RequestMapping("/api/database/model/design")
public class ModelDesignController {

    // 添加日志声明
    private static final Logger log = LoggerFactory.getLogger(ModelDesignController.class);

    @Autowired
    private IModelService modelService;

    @Autowired
    private IModelDesignService modelDesignService;

    @Autowired
    private IModelStandardService modelStandardService;

    /**
     * 获取模型设计详情
     *
     * @param modelId 模型ID
     * @return 模型设计详情
     */
    @GetMapping("/{modelId}")
    public R<Map<String, Object>> getModelDesign(@PathVariable String modelId) {
        try {
            // 1. 获取模型基本信息
            ModelEntity model = modelService.getById(modelId);
            if (model == null) {
                return R.fail("模型不存在");
            }

            // 2. 获取模型设计信息
            LambdaQueryWrapper<ModelDesignEntity> designWrapper = new LambdaQueryWrapper<>();
            designWrapper.eq(ModelDesignEntity::getModelId, modelId);
            ModelDesignEntity design = modelDesignService.getOne(designWrapper);

            // 3. 获取模型关联的标准
            LambdaQueryWrapper<ModelStandard> standardWrapper = new LambdaQueryWrapper<>();
            standardWrapper.eq(ModelStandard::getModelId, modelId);
            List<ModelStandard> standards = modelStandardService.list(standardWrapper);

            // 4. 组装返回数据
            Map<String, Object> result = new HashMap<>();
            result.put("model", model);
            result.put("design", design);
            result.put("standards", standards);

            return R.ok(result);
        } catch (Exception e) {
            return R.fail("获取模型设计详情失败: " + e.getMessage());
        }
    }

    /**
     * 保存模型设计
     *
     * @param params 包含模型信息、设计信息和标准关联的参数
     * @return 保存结果
     */
    @PostMapping("/save")
    public R<Map<String, Object>> saveModelDesign(@RequestBody Map<String, Object> params) {
        try {
            // 1. 获取并处理模型基本信息
            ModelEntity model = processModelInfo(params);
            
            // 2. 获取并处理模型设计信息
            ModelDesignEntity design = processDesignInfo(params, model.getId());
            
            // 3. 获取并处理模型标准关联
            List<ModelStandard> standards = processStandardInfo(params, model.getId());
            
            // 4. 保存所有信息
            boolean modelSaved = saveModelInfo(model);
            boolean designSaved = saveDesignInfo(design);
            boolean standardsSaved = saveStandardsInfo(standards, model.getId());
            
            if (!modelSaved || !designSaved) {
                return R.fail("保存模型设计失败");
            }
            
            // 5. 返回保存结果
            Map<String, Object> result = new HashMap<>();
            result.put("id", model.getId());
            result.put("name", model.getName());
            
            return R.ok(result);
        } catch (Exception e) {
            return R.fail("保存模型设计失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理模型基本信息
     */
    private ModelEntity processModelInfo(Map<String, Object> params) {
        Map<String, Object> modelMap = (Map<String, Object>) params.get("model");
        ModelEntity model = new ModelEntity();
        
        if (modelMap != null) {
            // 如果有ID，说明是更新操作
            if (modelMap.containsKey("id") && modelMap.get("id") != null) {
                String modelId = modelMap.get("id").toString();
                ModelEntity existingModel = modelService.getById(modelId);
                if (existingModel != null) {
                    model = existingModel;
                } else {
                    // 如果ID不存在，生成新ID
                    model.setId(UuidUtil.generateUuid());
                }
            } else {
                // 新建模型，生成ID
                model.setId(UuidUtil.generateUuid());
            }
            
            // 设置模型属性
            if (modelMap.containsKey("name")) model.setName(modelMap.get("name").toString());
            if (modelMap.containsKey("type")) model.setType(modelMap.get("type").toString());
            if (modelMap.containsKey("description")) model.setDescription(modelMap.get("description").toString());
            if (modelMap.containsKey("owner")) model.setOwner(modelMap.get("owner").toString());
            if (modelMap.containsKey("status")) model.setStatus(modelMap.get("status").toString());
            else if (model.getStatus() == null) model.setStatus("draft"); // 默认为草稿状态
        } else {
            // 如果没有提供模型信息，创建一个新的模型
            model.setId(UuidUtil.generateUuid());
            model.setName("新建模型");
            model.setType("detail");
            model.setStatus("draft");
        }
        
        return model;
    }
    
    /**
     * 处理模型设计信息
     */
    private ModelDesignEntity processDesignInfo(Map<String, Object> params, String modelId) {
        Map<String, Object> designMap = (Map<String, Object>) params.get("design");
        ModelDesignEntity design = new ModelDesignEntity();
        
        // 查询是否已存在设计信息
        LambdaQueryWrapper<ModelDesignEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelDesignEntity::getModelId, modelId);
        ModelDesignEntity existingDesign = modelDesignService.getOne(wrapper);
        
        if (existingDesign != null) {
            design = existingDesign;
        } else {
            design.setId(UuidUtil.generateUuid());
            design.setModelId(modelId);
        }
        
        if (designMap != null) {
            // 设置设计属性
            if (designMap.containsKey("canvas")) design.setCanvas(designMap.get("canvas").toString());
            if (designMap.containsKey("fields")) design.setFields(designMap.get("fields").toString());
            if (designMap.containsKey("indexes")) design.setIndexes(designMap.get("indexes").toString());
            if (designMap.containsKey("relations")) design.setRelations(designMap.get("relations").toString());
            if (designMap.containsKey("config")) design.setConfig(designMap.get("config").toString());
        }
        
        return design;
    }
    
    /**
     * 处理模型标准关联信息
     */
    private List<ModelStandard> processStandardInfo(Map<String, Object> params, String modelId) {
        List<Map<String, Object>> standardsList = (List<Map<String, Object>>) params.get("standards");
        List<ModelStandard> standards = new ArrayList<>();
        
        if (standardsList != null && !standardsList.isEmpty()) {
            for (Map<String, Object> standardMap : standardsList) {
                ModelStandard standard = new ModelStandard();
                standard.setId(UuidUtil.generateUuid());
                standard.setModelId(modelId);
                
                if (standardMap.containsKey("standardId")) standard.setStandardId(standardMap.get("standardId").toString());
                if (standardMap.containsKey("standardType")) standard.setStandardType(standardMap.get("standardType").toString());
                if (standardMap.containsKey("fieldId")) standard.setFieldId(standardMap.get("fieldId").toString());
                
                standards.add(standard);
            }
        }
        
        return standards;
    }
    
    /**
     * 保存模型基本信息
     */
    private boolean saveModelInfo(ModelEntity model) {
        return modelService.saveOrUpdate(model);
    }
    
    /**
     * 保存模型设计信息
     */
    private boolean saveDesignInfo(ModelDesignEntity design) {
        return modelDesignService.saveOrUpdate(design);
    }
    
    /**
     * 保存模型标准关联信息
     */
    private boolean saveStandardsInfo(List<ModelStandard> standards, String modelId) {
        // 先删除原有关联
        LambdaQueryWrapper<ModelStandard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelStandard::getModelId, modelId);
        modelStandardService.remove(wrapper);
        
        // 保存新的关联
        if (standards != null && !standards.isEmpty()) {
            return modelStandardService.saveBatch(standards);
        }
        
        return true;
    }

    /**
     * 应用标准到模型
     */
    @PostMapping("/apply-standards")
    public R<Void> applyStandards(@RequestBody Map<String, Object> params) {
        try {
            String modelId = (String) params.get("modelId");
            List<Map<String, Object>> standards = (List<Map<String, Object>>) params.get("standards");
            String applyScope = (String) params.get("applyScope");
            String description = (String) params.get("description");
            
            if (StringUtils.isEmpty(modelId)) {
                return R.fail("模型ID不能为空");
            }
            
            if (standards == null || standards.isEmpty()) {
                return R.fail("标准列表不能为空");
            }
            
            // 调用服务应用标准
            boolean result = modelStandardService.applyStandards(modelId, standards, applyScope, description);
            
            if (result) {
                return R.ok();
            } else {
                return R.fail("应用标准失败");
            }
        } catch (Exception e) {
            log.error("应用标准失败", e);
            return R.fail("应用标准失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取模型已应用的标准
     */
    @GetMapping("/model-standards/{modelId}")
    public R<List<ModelStandard>> getModelStandards(@PathVariable String modelId) {
        try {
            if (StringUtils.isEmpty(modelId)) {
                return R.fail("模型ID不能为空");
            }
            
            List<ModelStandard> standards = modelStandardService.getModelStandards(modelId);
            return R.ok(standards);
        } catch (Exception e) {
            log.error("获取模型已应用的标准失败", e);
            return R.fail("获取失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除模型的标准
     */
    @DeleteMapping("/model-standards/{modelId}")
    public R<Boolean> deleteModelStandards(@PathVariable String modelId) {
        try {
            if (StringUtils.isEmpty(modelId)) {
                return R.fail("模型ID不能为空");
            }
            
            boolean result = modelStandardService.deleteModelStandards(modelId);
            return R.ok(result);
        } catch (Exception e) {
            log.error("删除模型标准失败", e);
            return R.fail("删除失败: " + e.getMessage());
        }
    }
} 