package com.mango.test.database.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mango.test.database.entity.Domain;
import com.mango.test.database.entity.Dimension;
import com.mango.test.database.entity.MaterializeTask;
import com.mango.test.database.entity.ModelEntity;
import com.mango.test.database.entity.TaskVersion;
import com.mango.test.database.model.DomainQuery;
import com.mango.test.database.service.DomainService;
import com.mango.test.database.service.DimensionService;
import com.mango.test.database.service.IModelService;
import com.mango.test.database.service.IModelStandardService;
import com.mango.test.database.service.MaterializeTaskService;
import com.mango.test.database.service.TaskVersionService;
import com.mango.test.vo.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/domain")
public class DomainController {

    private static final Logger log = LoggerFactory.getLogger(DomainController.class);

    @Autowired
    private DomainService domainService;

    @Autowired
    private DimensionService dimensionService;

    @Autowired
    private MaterializeTaskService materializeTaskService;
    
    @Autowired
    private TaskVersionService taskVersionService;
    
    @Autowired
    private IModelService modelService;

    /**
     * 分页查询主题域
     */
    @GetMapping("/page")
    public R<IPage<Domain>> pageDomain(DomainQuery query) {
        return R.ok(domainService.pageDomain(query));
    }

    /**
     * 获取主题域树形结构
     */
    @GetMapping("/tree")
    public R<List<Domain>> getDomainTree() {
        try {
            // 获取基础的主题域树
            List<Domain> domainTree = domainService.getDomainTree();
            log.info("获取到 {} 个主题域", domainTree.size());
            
            // 获取所有与主题域相关的物化任务
            List<MaterializeTask> tasks = materializeTaskService.listByDataSourceType("theme");
            log.info("获取到 {} 个主题类型的物化任务", tasks.size());
            
            // 为每个主题域添加模型和物理表信息
            enrichDomainTreeWithModels(domainTree, tasks);
            
            // 添加维度域信息
            enrichDomainTreeWithDimensions(domainTree);
            
            // 统计树节点总数
            int totalNodes = countNodes(domainTree);
            log.info("树形结构构建完成，共包含 {} 个节点", totalNodes);
            
            return R.ok(domainTree);
        } catch (Exception e) {
            log.error("获取主题域树形结构失败", e);
            return R.fail("获取主题域树形结构失败: " + e.getMessage());
        }
    }

    /**
     * 统计树中的节点总数
     */
    private int countNodes(List<Domain> domains) {
        if (domains == null || domains.isEmpty()) {
            return 0;
        }
        
        int count = domains.size();
        for (Domain domain : domains) {
            if (domain.getChildren() != null && !domain.getChildren().isEmpty()) {
                count += countNodes(domain.getChildren());
            }
        }
        return count;
    }

    /**
     * 获取主题域列表
     */
    @GetMapping("/list")
    public R<List<Domain>> getDomainList(@RequestParam(required = false) String parentId) {
        return R.ok(domainService.getDomainList(parentId));
    }

    /**
     * 获取子主题域列表
     */
    @GetMapping("/children/{parentId}")
    public R<List<Domain>> getChildren(@PathVariable String parentId) {
        return R.ok(domainService.getChildren(parentId));
    }

    /**
     * 获取主题域详情
     */
    @GetMapping("/{id}")
    public R<Domain> getDomain(@PathVariable String id) {
        return R.ok(domainService.getById(id));
    }

    /**
     * 新增主题域
     */
    @PostMapping
    public R<Void> addDomain(@RequestBody Domain domain) {
        // 确保必要的字段都已设置
        if (domain.getDataSourceId() == null) {
            return R.fail("数据源ID不能为空");
        }
        // 如果设置了父级ID，验证父级域是否存在
        if (!StringUtils.isEmpty(domain.getParentId())) {
            Domain parentDomain = domainService.getById(domain.getParentId());
            if (parentDomain == null) {
                return R.fail("父级主题域不存在");
            }
        }
        
        domainService.addDomain(domain);
        return R.ok();
    }

    /**
     * 修改主题域
     */
    @PutMapping
    public R<Void> updateDomain(@RequestBody Domain domain) {
        // 如果修改了父级ID，验证父级域是否存在
        if (domain.getParentId() != null) {
            Domain parentDomain = domainService.getById(domain.getParentId());
            if (parentDomain == null) {
                return R.fail("父级主题域不存在");
            }
            // 检查是否形成循环引用
            if (domain.getId().equals(domain.getParentId())) {
                return R.fail("不能将自己设置为父级主题域");
            }
        }
        
        domainService.updateDomain(domain);
        return R.ok();
    }

    /**
     * 删除主题域
     */
    @DeleteMapping("/{id}")
    public R<Void> deleteDomain(@PathVariable String id) {
        // 检查是否有子域，如果有则不允许删除
        if (domainService.hasChildren(id)) {
            return R.fail("该主题域下存在子域，无法删除");
        }
        domainService.deleteDomain(id);
        return R.ok();
    }

    /**
     * 修改主题域状态
     */
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable String id, @RequestParam String status) {
        domainService.updateStatus(id, status);
        return R.ok();
    }

    /**
     * 获取主题域和维度域的组合树结构
     */
    @GetMapping("/dimension-tree")
    public R<List<Map<String, Object>>> getDomainDimensionTree() {
        try {
            // 1. 获取所有主题域
            List<Domain> domains = domainService.getDomainTree();
            
            // 2. 获取所有维度域
            List<Dimension> dimensions = dimensionService.list();
            
            // 3. 构建组合树
            List<Map<String, Object>> tree = new ArrayList<>();
            
            // 添加未分配节点
            Map<String, Object> unassignedNode = new HashMap<>();
            unassignedNode.put("id", "unassigned");
            unassignedNode.put("name", "未分配维度域");
            unassignedNode.put("type", "domain");
            unassignedNode.put("children", dimensions.stream()
                .filter(d -> d.getDomainId() == null)
                .map(this::convertDimensionToNode)
                .collect(Collectors.toList()));
            tree.add(unassignedNode);
            
            // 构建主题域树
            for (Domain domain : domains) {
                tree.add(buildDomainTree(domain, dimensions));
            }
            
            return R.ok(tree);
        } catch (Exception e) {
            return R.fail("获取主题域维度树失败: " + e.getMessage());
        }
    }

    private Map<String, Object> buildDomainTree(Domain domain, List<Dimension> dimensions) {
        Map<String, Object> domainNode = new HashMap<>();
        domainNode.put("id", domain.getId());
        domainNode.put("name", domain.getName());
        domainNode.put("type", "domain");
        
        List<Map<String, Object>> children = new ArrayList<>();
        
        // 1. 处理子域
        if (domain.getChildren() != null && !domain.getChildren().isEmpty()) {
            for (Domain child : domain.getChildren()) {
                children.add(buildDomainTree(child, dimensions));
            }
        }
        
        // 2. 添加维度节点
        List<Map<String, Object>> dimensionNodes = dimensions.stream()
            .filter(d -> domain.getId().equals(d.getDomainId()))
            .map(this::convertDimensionToNode)
            .collect(Collectors.toList());
        children.addAll(dimensionNodes);
        
        // 3. 设置完整的children列表
        domainNode.put("children", children);
        
        return domainNode;
    }

    private Map<String, Object> convertDimensionToNode(Dimension d) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", d.getId());
        node.put("name", d.getName());
        node.put("type", "dimension");
        node.put("source", d.getSourceType());
        node.put("updateStrategy", d.getUpdateStrategy());
        node.put("attributeCount", d.getAttributeCount());
        node.put("lastUpdate", d.getUpdateTime());
        node.put("description", d.getDescription());
        return node;
    }


    /**
     * 为主题域树添加模型信息
     */
    private void enrichDomainTreeWithModels(List<Domain> domains, List<MaterializeTask> tasks) {
        // 处理每一个主题域
        for (Domain domain : domains) {
            // 先处理子域
            if (domain.getChildren() != null && !domain.getChildren().isEmpty()) {
                enrichDomainTreeWithModels(domain.getChildren(), tasks);
            }
            
            // 获取当前主题域相关的任务
            List<MaterializeTask> domainTasks = tasks.stream()
                .filter(task -> domain.getId().equals(task.getDomainId()))
                .collect(Collectors.toList());

            // 确保domain.children已初始化
            if (domain.getChildren() == null) {
                domain.setChildren(new ArrayList<>());
            }
            
            // 如果有相关任务，创建模型组
            if (!domainTasks.isEmpty()) {
                // 创建一个特殊的Domain对象作为模型组节点
                Domain modelGroupNode = new Domain();
                modelGroupNode.setId(domain.getId());
                modelGroupNode.setName("模型");
                // 设置其他需要的字段，使前端能识别为模型组
                modelGroupNode.setDescription("modelGroup");
                
                // 为模型组节点创建子节点列表
                List<Domain> modelNodes = new ArrayList<>();
                
                // 从每个任务中提取模型
                for (MaterializeTask task : domainTasks) {
                    List<String> modelIds = task.getModelIdList();
                    if (modelIds != null && !modelIds.isEmpty()) {
                        // 获取任务的最新版本
                        TaskVersion latestVersion = taskVersionService.getLatestByTaskId(task.getId());
                        if (latestVersion != null) {
                            // 处理每个模型
                            for (String modelId : modelIds) {
                                ModelEntity modelEntity = modelService.getById(modelId);
                                if (modelEntity != null) {
                                    // 将模型转换为Domain对象添加到模型组
                                    Domain modelNode = createModelDomainNode(modelEntity);
                                    if (modelNode != null) {
                                        modelNodes.add(modelNode);
                                    }
                                }
                            }
                        }
                    }
                }
                
                // 如果有模型，则添加模型组到主题域
                if (!modelNodes.isEmpty()) {
                    modelGroupNode.setChildren(modelNodes);
                    domain.getChildren().add(modelGroupNode);
                }
            }
        }
    }
    
    /**
     * 将模型实体转换为Domain节点
     */
    private Domain createModelDomainNode(ModelEntity model) {
        Domain modelNode = new Domain();
        modelNode.setId(model.getId());
        modelNode.setName(model.getName());
        // 使用description字段存储节点类型
        modelNode.setDescription("model");
        // 使用owner字段存储模型所有者
        modelNode.setOwner(model.getOwner());
        // 使用sourceType字段存储模型类型
        modelNode.setSourceType(model.getType());
        // 使用status字段存储模型状态
        modelNode.setStatus(model.getStatus());
        
        // 初始化children集合
        modelNode.setChildren(new ArrayList<>());
        
        // 获取模型关联的物理表
        List<Map<String, Object>> physicalTables = modelService.getPhysicalTablesByModelId(model.getId());
        if (physicalTables != null && !physicalTables.isEmpty()) {
            log.info("模型 [{}] 关联了 {} 个物理表", model.getId(), physicalTables.size());
            
            // 将物理表转换为Domain对象
            for (Map<String, Object> table : physicalTables) {
                Domain tableNode = new Domain();
                tableNode.setId((String) table.get("id"));
                tableNode.setName((String) table.get("name"));
                // 使用description字段存储节点类型
                tableNode.setDescription("physicalTable");
                
                // 存储物理表的基本信息
                if (table.get("displayName") != null) {
                    tableNode.setSourceType((String) table.get("displayName"));
                }
                
                // 存储物理表的注释
                if (table.get("comment") != null) {
                    tableNode.setComment((String) table.get("comment"));
                }
                
                // 存储创建时间和更新时间
                if (table.get("createTime") != null) {
                    tableNode.setPhysicalCreateTime((Date) table.get("createTime"));
                }
                
                if (table.get("updateTime") != null) {
                    tableNode.setPhysicalUpdateTime((Date) table.get("updateTime"));
                }
                
                // 存储字段数量
                if (table.get("fieldCount") != null) {
                    try {
                        Integer fieldCount = (Integer) table.get("fieldCount");
                        // 可以将fieldCount存储到额外的属性中
                        Map<String, Object> extraInfo = tableNode.getExtraInfo();
                        extraInfo.put("fieldCount", fieldCount);
                    } catch (Exception e) {
                        log.warn("转换字段数量失败", e);
                    }
                }
                
                // 存储字段信息
                if (table.get("fields") != null) {
                    try {
                        List<Map<String, Object>> fields = (List<Map<String, Object>>) table.get("fields");
                        // 将字段信息存储到额外的属性中
                        Map<String, Object> extraInfo = tableNode.getExtraInfo();
                        extraInfo.put("fields", fields);
                        
                        // 处理子节点：每个字段作为物理表的子节点
                        List<Domain> fieldNodes = new ArrayList<>();
                        for (Map<String, Object> field : fields) {
                            Domain fieldNode = new Domain();
                            fieldNode.setId((String) field.get("id"));
                            fieldNode.setName((String) field.get("name"));
                            fieldNode.setDescription("field");
                            
                            // 设置字段的显示名称
                            if (field.get("displayName") != null) {
                                fieldNode.setSourceType((String) field.get("displayName"));
                            }
                            
                            // 设置字段类型
                            if (field.get("type") != null) {
                                fieldNode.setStatus((String) field.get("type"));
                            }
                            
                            // 设置字段额外信息
                            Map<String, Object> fieldExtraInfo = new HashMap<>();
                            fieldExtraInfo.put("isPrimary", field.get("isPrimary"));
                            fieldExtraInfo.put("notNull", field.get("notNull"));
                            fieldExtraInfo.put("length", field.get("length"));
                            fieldExtraInfo.put("comment", field.get("comment"));
                            fieldNode.setExtraInfo(fieldExtraInfo);
                            
                            fieldNodes.add(fieldNode);
                        }
                        
                        // 设置字段节点为表节点的子节点
                        tableNode.setChildren(fieldNodes);
                    } catch (Exception e) {
                        log.warn("转换字段信息失败", e);
                    }
                }
                
                // 添加到模型的子节点中
                modelNode.getChildren().add(tableNode);
            }
        } else {
            log.info("模型 [{}] 没有关联的物理表", model.getId());
        }
        
        return modelNode;
    }
    
    /**
     * 为主题域树添加维度域信息
     */
    private void enrichDomainTreeWithDimensions(List<Domain> domains) {
        // 获取所有维度域
        List<Dimension> dimensions = dimensionService.list();
        
        // 处理每一个主题域
        for (Domain domain : domains) {
            // 先处理子域
            if (domain.getChildren() != null && !domain.getChildren().isEmpty()) {
                enrichDomainTreeWithDimensions(domain.getChildren());
            }
            
            // 获取当前主题域相关的维度域
            List<Dimension> domainDimensions = dimensions.stream()
                .filter(dim -> domain.getId().equals(dim.getDomainId()))
                .collect(Collectors.toList());
            
            // 如果有维度域，创建维度域组
            if (!domainDimensions.isEmpty()) {
                // 确保domain.children已初始化
                if (domain.getChildren() == null) {
                    domain.setChildren(new ArrayList<>());
                }
                
                // 创建维度域组节点
                Domain dimensionGroupNode = new Domain();
                dimensionGroupNode.setId(domain.getId() + "-dimensions");
                dimensionGroupNode.setName("维度域");
                dimensionGroupNode.setDescription("dimensionDomainGroup");
                
                // 创建维度域子节点列表
                List<Domain> dimensionNodes = new ArrayList<>();
                
                // 添加维度域到组
                for (Dimension dim : domainDimensions) {
                    Domain dimensionNode = new Domain();
                    dimensionNode.setId(dim.getId());
                    dimensionNode.setName(dim.getName());
                    dimensionNode.setDescription("dimensionDomain");
                    dimensionNode.setSourceType(dim.getSourceType());
                    
                    dimensionNodes.add(dimensionNode);
                }
                
                // 设置维度域组的子节点
                dimensionGroupNode.setChildren(dimensionNodes);
                
                // 添加维度域组到主题域
                domain.getChildren().add(dimensionGroupNode);
            }
        }
    }
    
    /**
     * 格式化日期
     */
    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return date.toString(); // 实际项目中可使用更好的日期格式化
    }

    /**
     * 获取主题域相关的模型列表
     * @param domainId 主题域ID
     * @return 模型列表
     */
    @GetMapping("/models/{domainId}")
    public R<List<ModelEntity>> getDomainModels(@PathVariable String domainId) {
        try {
            log.info("获取主题域相关的模型列表，参数: domainId={}", domainId);
            
            if (StringUtils.isEmpty(domainId)) {
                return R.fail("主题域ID不能为空");
            }
            
            // 获取该主题域相关的所有物化任务
            List<MaterializeTask> tasks = materializeTaskService.listByDataSourceType("theme")
                .stream()
                .filter(task -> domainId.equals(task.getDomainId()))
                .collect(Collectors.toList());
            
            log.info("找到主题域 [{}] 相关的物化任务 {} 个", domainId, tasks.size());
            
            // 收集所有任务相关的模型ID
            List<String> modelIds = new ArrayList<>();
            for (MaterializeTask task : tasks) {
                List<String> taskModelIds = task.getModelIdList();
                if (taskModelIds != null && !taskModelIds.isEmpty()) {
                    modelIds.addAll(taskModelIds);
                }
            }
            
            log.info("主题域 [{}] 相关的模型ID {} 个", domainId, modelIds.size());
            
            // 如果没有找到相关模型，返回空列表
            if (modelIds.isEmpty()) {
                return R.ok(new ArrayList<>());
            }
            
            // 批量获取模型详情
            List<ModelEntity> models = modelService.listByIds(modelIds);
            
            log.info("成功获取主题域 [{}] 相关的模型 {} 个", domainId, models.size());
            
            return R.ok(models);
        } catch (Exception e) {
            log.error("获取主题域相关的模型列表失败: domainId=" + domainId, e);
            return R.fail("获取主题域相关的模型列表失败: " + e.getMessage());
        }
    }
} 