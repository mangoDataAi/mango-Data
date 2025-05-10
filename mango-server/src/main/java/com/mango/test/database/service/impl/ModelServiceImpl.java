package com.mango.test.database.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mango.test.database.entity.ModelEntity;
import com.mango.test.database.entity.Table;
import com.mango.test.database.entity.TableField;
import com.mango.test.database.mapper.ModelMapper;
import com.mango.test.database.service.IModelService;
import com.mango.test.database.service.TableService;
import com.mango.test.database.service.TableFieldService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 模型服务实现类
 */
@Service
@Slf4j
public class ModelServiceImpl extends ServiceImpl<ModelMapper, ModelEntity> implements IModelService {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private TableService tableService;

    @Autowired
    private TableFieldService tableFieldService;

    /**
     * 查询模型列表
     */
    @Override
    public List<ModelEntity> selectModelList(ModelEntity model) {
        LambdaQueryWrapper<ModelEntity> queryWrapper = new LambdaQueryWrapper<>();
        
        // 构建查询条件
        if (model != null) {
            if (StringUtils.hasText(model.getName())) {
                queryWrapper.like(ModelEntity::getName, model.getName());
            }
            
            if (StringUtils.hasText(model.getType())) {
                queryWrapper.eq(ModelEntity::getType, model.getType());
            }
            
            if (StringUtils.hasText(model.getOwner())) {
                queryWrapper.like(ModelEntity::getOwner, model.getOwner());
            }
            
            if (StringUtils.hasText(model.getStatus())) {
                queryWrapper.eq(ModelEntity::getStatus, model.getStatus());
            }
        }
        
        // 按创建时间降序排序
        queryWrapper.orderByDesc(ModelEntity::getCreateTime);
        
        return this.list(queryWrapper);
    }

    /**
     * 发布模型
     */
    @Override
    public boolean publishModel(String id) {
        LambdaUpdateWrapper<ModelEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ModelEntity::getId, id)
                    .set(ModelEntity::getStatus, "published");
        return this.update(updateWrapper);
    }

    /**
     * 运行模型
     */
    @Override
    public boolean runModel(String id) {
        LambdaUpdateWrapper<ModelEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ModelEntity::getId, id)
                    .set(ModelEntity::getStatus, "running");
        return this.update(updateWrapper);
    }

    /**
     * 插入前生成UUID
     */
    @Override
    public boolean save(ModelEntity entity) {
        if (entity.getId() == null || entity.getId().isEmpty()) {
            entity.setId(UUID.randomUUID().toString().replaceAll("-", ""));
        }
        
        if (entity.getStatus() == null) {
            entity.setStatus("draft");
        }
        
        return super.save(entity);
    }

    /**
     * 根据模型ID获取物理表列表
     * 
     * @param modelId 模型ID
     * @return 物理表列表
     */
    @Override
    public List<Map<String, Object>> getPhysicalTablesByModelId(String modelId) {
        if (StringUtils.isEmpty(modelId)) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> physicalTables = new ArrayList<>();
        
        try {
            // 1. 查询模型关联的物理表
            List<Table> tables = tableService.getTablesByModelId(modelId);
            
            if (tables != null && !tables.isEmpty()) {
                log.info("模型 [{}] 找到 {} 个物理表", modelId, tables.size());
                
                // 2. 遍历每个物理表，构建树形结构
                for (Table table : tables) {
                    Map<String, Object> tableNode = new HashMap<>();
                    tableNode.put("id", table.getId());
                    tableNode.put("name", table.getName());
                    tableNode.put("displayName", table.getDisplayName());
                    tableNode.put("comment", table.getComment());
                    tableNode.put("fieldCount", table.getFieldCount());
                    tableNode.put("createTime", table.getCreateTime());
                    tableNode.put("updateTime", table.getUpdateTime());
                    tableNode.put("modelId", modelId);
                    
                    // 添加表的字段列表
                    List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
                    if (fields != null && !fields.isEmpty()) {
                        List<Map<String, Object>> fieldsList = new ArrayList<>();
                        for (TableField field : fields) {
                            Map<String, Object> fieldMap = new HashMap<>();
                            fieldMap.put("id", field.getId());
                            fieldMap.put("name", field.getName());
                            fieldMap.put("displayName", field.getDisplayName());
                            fieldMap.put("type", field.getType());
                            fieldMap.put("length", field.getLength());
                            fieldMap.put("isPrimary", field.getIsPrimary());
                            fieldMap.put("notNull", field.getNotNull());
                            fieldMap.put("comment", field.getMgcomment());
                            
                            fieldsList.add(fieldMap);
                        }
                        tableNode.put("fields", fieldsList);
                    }
                    
                    physicalTables.add(tableNode);
                    log.debug("添加物理表: {}", table.getName());
                }
            } else {
                log.info("模型 [{}] 没有关联的物理表", modelId);
            }
        } catch (Exception e) {
            log.error("获取模型物理表失败: modelId=" + modelId, e);
        }
        
        return physicalTables;
    }

    /**
     * 根据表ID获取表字段信息
     * 
     * @param tableId 表ID
     * @return 字段信息列表
     */
    @Override
    public List<Map<String, Object>> getTableFieldsByTableId(String tableId) {
        if (StringUtils.isEmpty(tableId)) {
            return new ArrayList<>();
        }
        
        List<Map<String, Object>> fieldsList = new ArrayList<>();
        
        try {
            // 查询表的所有字段
            List<TableField> fields = tableFieldService.getFieldsByTableId(tableId);
            
            if (fields != null && !fields.isEmpty()) {
                log.info("表 [{}] 找到 {} 个字段", tableId, fields.size());
                
                // 转换字段为前端需要的格式
                for (TableField field : fields) {
                    Map<String, Object> fieldMap = new HashMap<>();
                    fieldMap.put("id", field.getId());
                    fieldMap.put("name", field.getName());
                    fieldMap.put("displayName", field.getDisplayName());
                    fieldMap.put("type", field.getType());
                    fieldMap.put("length", field.getLength());
                    fieldMap.put("isPrimaryKey", field.getIsPrimary());
                    fieldMap.put("nullable", field.getNotNull() == null || field.getNotNull() == 0);
                    fieldMap.put("description", field.getMgcomment());
                    
                    fieldsList.add(fieldMap);
                }
            } else {
                log.info("表 [{}] 没有字段信息", tableId);
            }
        } catch (Exception e) {
            log.error("获取表字段信息失败: tableId=" + tableId, e);
        }
        
        return fieldsList;
    }
} 