package com.mango.test.database.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mango.test.common.util.UuidUtil;
import com.mango.test.database.entity.ModelEntity;
import com.mango.test.database.entity.ModelStandard;
import com.mango.test.database.mapper.ModelMapper;
import com.mango.test.database.mapper.ModelStandardMapper;
import com.mango.test.database.service.IModelStandardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 模型-标准关联服务实现类
 */
@Service
public class ModelStandardServiceImpl extends ServiceImpl<ModelStandardMapper, ModelStandard> implements IModelStandardService {

    // 添加日志声明
    private static final Logger log = LoggerFactory.getLogger(ModelStandardServiceImpl.class);

    @Autowired
    private ModelMapper modelMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean applyStandards(String modelId, List<Map<String, Object>> standards, String applyScope, String description) {
        try {
            // 1. 验证模型是否存在
            ModelEntity model = modelMapper.selectById(modelId);
            if (model == null) {
                throw new RuntimeException("模型不存在: " + modelId);
            }
            
            // 2. 获取当前用户信息（根据您的实际情况获取）
            String currentUser = "admin"; // 这里应该从安全上下文或其他地方获取
            Date currentTime = new Date();
            
            // 3. 更新模型实体中的标准应用信息
            // 3.1 构建应用的标准ID字符串
            List<String> standardIds = standards.stream()
                .map(std -> (String) std.get("id"))
                .collect(Collectors.toList());
            String appliedStandardIds = String.join(",", standardIds);
            
            // 3.2 更新模型实体
            model.setAppliedStandardIds(appliedStandardIds);
            model.setStandardApplyScope(applyScope);
            model.setStandardApplyDescription(description);
            model.setStandardApplyTime(currentTime);
            model.setStandardApplyUser(currentUser);
            model.setUpdateTime(currentTime);
            model.setUpdateBy(currentUser);
            
            // 3.3 保存模型实体
            modelMapper.updateById(model);
            
            // 4. 更新模型-标准关联表
            // 4.1 删除旧的关联
            this.deleteModelStandards(modelId);
            
            // 4.2 创建新的关联
            List<ModelStandard> modelStandards = new ArrayList<>();
            for (Map<String, Object> standard : standards) {
                ModelStandard modelStandard = new ModelStandard();
                modelStandard.setId(UuidUtil.generateUuid());
                modelStandard.setModelId(modelId);
                modelStandard.setStandardId((String) standard.get("id"));
                modelStandard.setStandardName((String) standard.get("name"));
                modelStandard.setStandardType((String) standard.get("type")); // required 或 suggested
                modelStandard.setRuleType((String) standard.get("type"));
                modelStandard.setApplyScope(applyScope);
                modelStandard.setDescription(description);
                modelStandard.setApplyTime(currentTime);
                modelStandard.setApplyUser(currentUser);
                modelStandard.setStatus(1); // 有效
                
                modelStandards.add(modelStandard);
            }
            
            // 4.3 批量插入关联
            if (!modelStandards.isEmpty()) {
                this.saveBatch(modelStandards);
            }
            
            return true;
        } catch (Exception e) {
            log.error("应用标准到模型失败", e);
            throw new RuntimeException("应用标准到模型失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ModelStandard> getModelStandards(String modelId) {
        LambdaQueryWrapper<ModelStandard> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ModelStandard::getModelId, modelId);
        queryWrapper.eq(ModelStandard::getStatus, 1); // 只查询有效的
        return this.list(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteModelStandards(String modelId) {
        LambdaQueryWrapper<ModelStandard> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ModelStandard::getModelId, modelId);
        return this.remove(queryWrapper);
    }

} 