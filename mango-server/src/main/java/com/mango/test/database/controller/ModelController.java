package com.mango.test.database.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mango.test.database.entity.ModelEntity;
import com.mango.test.database.service.DomainService;
import com.mango.test.database.service.IModelService;
import com.mango.test.dto.ModelSearchDTO;
import com.mango.test.vo.R;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 模型管理控制器
 */
@RestController
@RequestMapping("/api/database/model")
public class ModelController {

    @Autowired
    private IModelService modelService;
    
    @Autowired
    private DomainService domainService;

    /**
     * 获取模型列表（支持高级搜索）
     */
    @GetMapping("/list")
    public R<Map<String, Object>> list(ModelSearchDTO searchDTO,
                                       @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                       @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        Page<ModelEntity> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<ModelEntity> queryWrapper = new LambdaQueryWrapper<>();
        
        // 构建基本查询条件
        if (searchDTO != null) {
            // 基本搜索条件
            if (StringUtils.isNotBlank(searchDTO.getName())) {
                queryWrapper.like(ModelEntity::getName, searchDTO.getName());
            }
            if (StringUtils.isNotBlank(searchDTO.getType())) {
                queryWrapper.eq(ModelEntity::getType, searchDTO.getType());
            }
            if (StringUtils.isNotBlank(searchDTO.getOwner())) {
                queryWrapper.like(ModelEntity::getOwner, searchDTO.getOwner());
            }
            if (StringUtils.isNotBlank(searchDTO.getStatus())) {
                queryWrapper.eq(ModelEntity::getStatus, searchDTO.getStatus());
            }
            
            // 高级搜索条件
            if (StringUtils.isNotBlank(searchDTO.getDescription())) {
                queryWrapper.like(ModelEntity::getDescription, searchDTO.getDescription());
            }
            
            // 创建时间范围
            if (searchDTO.getCreateTimeStart() != null) {
                queryWrapper.ge(ModelEntity::getCreateTime, searchDTO.getCreateTimeStart());
            }
            if (searchDTO.getCreateTimeEnd() != null) {
                queryWrapper.le(ModelEntity::getCreateTime, searchDTO.getCreateTimeEnd());
            }
            
            // 更新时间范围
            if (searchDTO.getUpdateTimeStart() != null) {
                queryWrapper.ge(ModelEntity::getUpdateTime, searchDTO.getUpdateTimeStart());
            }
            if (searchDTO.getUpdateTimeEnd() != null) {
                queryWrapper.le(ModelEntity::getUpdateTime, searchDTO.getUpdateTimeEnd());
            }
            
            // 标签搜索 - 注释掉或修改，因为ModelEntity没有getTags方法
            /*
            if (searchDTO.getTags() != null && !searchDTO.getTags().isEmpty()) {
                queryWrapper.and(wrapper -> {
                    for (String tag : searchDTO.getTags()) {
                        wrapper.or().like(ModelEntity::getTags, tag);
                    }
                });
            }
            */
            
            // 版本号范围 - 注释掉或修改，因为ModelEntity没有getVersion方法
            /*
            if (searchDTO.getMinVersion() != null) {
                queryWrapper.ge(ModelEntity::getVersion, searchDTO.getMinVersion());
            }
            if (searchDTO.getMaxVersion() != null) {
                queryWrapper.le(ModelEntity::getVersion, searchDTO.getMaxVersion());
            }
            */
        }
        
        // 按创建时间降序排序
        queryWrapper.orderByDesc(ModelEntity::getCreateTime);
        
        Page<ModelEntity> result = modelService.page(page, queryWrapper);
        
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getRecords());
        data.put("total", result.getTotal());
        
        return R.ok(data);
    }

    /**
     * 获取模型树
     */
    @GetMapping("/tree")
    public R<List<Map<String, Object>>> getModelTree() {
        try {
            // 获取所有模型
            List<ModelEntity> allModels = modelService.list();
            
            // 将模型直接转换为节点列表，不再构建树结构
            List<Map<String, Object>> modelList = allModels.stream()
                .map(this::convertModelToNode)
                .collect(Collectors.toList());
            
            return R.ok(modelList);
        } catch (Exception e) {
            return R.fail("获取模型列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 将模型转换为节点
     */
    private Map<String, Object> convertModelToNode(ModelEntity model) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", model.getId());
        node.put("name", model.getName());
        node.put("type", model.getType());
        node.put("owner", model.getOwner());
        node.put("status", model.getStatus());
        node.put("description", model.getDescription());
        node.put("updateTime", model.getUpdateTime());
        node.put("isLeaf", true);
        return node;
    }

    /**
     * 获取模型详情
     */
    @GetMapping("/{id}")
    public R<ModelEntity> getInfo(@PathVariable String id) {
        return R.ok(modelService.getById(id));
    }

    /**
     * 新增模型
     */
    @PostMapping
    public R<Boolean> add(@RequestBody ModelEntity model) {
        return modelService.save(model) ? R.ok() : R.fail("新增模型失败");
    }

    /**
     * 修改模型
     */
    @PutMapping
    public R<Boolean> edit(@RequestBody ModelEntity model) {
        return modelService.updateById(model) ? R.ok() : R.fail("修改模型失败");
    }

    /**
     * 删除模型
     */
    @DeleteMapping("/{id}")
    public R<Boolean> remove(@PathVariable String id) {
        return modelService.removeById(id) ? R.ok() : R.fail("删除模型失败");
    }

    /**
     * 发布模型
     */
    @PostMapping("/publish/{id}")
    public R<Boolean> publish(@PathVariable String id) {
        return modelService.publishModel(id) ? R.ok() : R.fail("发布模型失败");
    }

    /**
     * 运行模型
     */
    @PostMapping("/run/{id}")
    public R<Boolean> run(@PathVariable String id) {
        return modelService.runModel(id) ? R.ok() : R.fail("运行模型失败");
    }

    /**
     * 导入模型
     */
    @PostMapping("/import")
    public R<Boolean> importModel(@RequestParam("file") MultipartFile file) {
        // TODO: 实现导入逻辑
        return R.ok();
    }

    /**
     * 导出模型
     */
    @GetMapping("/export/{id}")
    public void export(@PathVariable String id, HttpServletResponse response) {
        // TODO: 实现导出逻辑
    }

    /**
     * 获取物理表的字段信息
     */
    @GetMapping("/{modelId}/tables/{tableId}/fields")
    public R<List<Map<String, Object>>> getTableFields(
            @PathVariable String modelId, 
            @PathVariable String tableId) {
        try {
            // 验证参数
            if (StringUtils.isEmpty(modelId) || StringUtils.isEmpty(tableId)) {
                return R.fail("模型ID或表ID不能为空");
            }
            
            // 获取表字段信息
            List<Map<String, Object>> fields = modelService.getTableFieldsByTableId(tableId);
            
            return R.ok(fields);
        } catch (Exception e) {
            return R.fail("获取表字段失败: " + e.getMessage());
        }
    }

    /**
     * 获取模型关联的物理表列表
     */
    @GetMapping("/{modelId}/tables")
    public R<List<Map<String, Object>>> getModelTables(@PathVariable String modelId) {
        try {
            // 验证参数
            if (StringUtils.isEmpty(modelId)) {
                return R.fail("模型ID不能为空");
            }
            
            // 获取模型关联的物理表
            List<Map<String, Object>> tables = modelService.getPhysicalTablesByModelId(modelId);
            
            return R.ok(tables);
        } catch (Exception e) {
            return R.fail("获取模型关联的物理表失败: " + e.getMessage());
        }
    }
} 