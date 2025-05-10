package com.mango.test.database.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mango.test.common.util.UuidUtil;
import com.mango.test.database.entity.Dimension;
import com.mango.test.database.entity.DimensionAttribute;
import com.mango.test.database.entity.DimensionStandard;
import com.mango.test.database.entity.Domain;
import com.mango.test.database.service.DimensionService;
import com.mango.test.database.service.DomainService;
import com.mango.test.vo.R;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dimension")
public class DimensionController {
    
    private static final Logger log = LoggerFactory.getLogger(DimensionController.class);
    
    @Autowired
    private DimensionService dimensionService;
    
    @Autowired
    private DomainService domainService;
    
    @GetMapping("/list")
    public R<IPage<Dimension>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String domainId,
            @RequestParam(required = false) Boolean unassigned,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        QueryWrapper<Dimension> queryWrapper = new QueryWrapper<>();
        
        // 添加名称查询条件
        if (StringUtils.isNotBlank(name)) {
            queryWrapper.like("name", name);
        }
        
        // 添加类型查询条件
        if (StringUtils.isNotBlank(type)) {
            queryWrapper.eq("type", type);
        }
        
        // 处理维度域查询条件
        if (unassigned != null && unassigned) {
            queryWrapper.isNull("domain_id").or().eq("domain_id","");  // 查询未分配的维度
        } else if (StringUtils.isNotBlank(domainId)) {
            queryWrapper.eq("domain_id", domainId);  // 查询指定维度域的维度
        }
        
        // 设置排序
        queryWrapper.orderByDesc("update_time");
        
        Page<Dimension> page = new Page<>(current, size);
        IPage<Dimension> result = dimensionService.page(page, queryWrapper);
        
        return R.ok(result);
    }
    
    @GetMapping("/{id}")
    public R<Dimension> getById(@PathVariable String id) {
        log.info("Getting dimension details for ID: {}", id);
        try {
            // 添加ID验证
            if (StringUtils.isBlank(id)) {
                log.warn("Dimension ID is empty");
                return R.fail("维度ID不能为空");
            }
            
            // 获取维度信息
            Dimension dimension = dimensionService.getById(id);
            if (dimension == null) {
                log.warn("Dimension not found for ID: {}", id);
                return R.fail("维度不存在");
            }
            
            log.debug("Found dimension: {}", dimension);
            
            // 获取关联的主题域名称
            if (StringUtils.isNotBlank(dimension.getDomainId())) {
                try {
                    Domain domain = domainService.getById(dimension.getDomainId());
                    if (domain != null) {
                        dimension.setDomainName(domain.getName());
                        log.debug("Associated domain name: {}", domain.getName());
                    } else {
                        log.warn("Domain not found for ID: {}", dimension.getDomainId());
                    }
                } catch (Exception e) {
                    log.warn("Failed to get domain info for dimension {}: {}", id, e.getMessage());
                }
            }
            
            log.info("Successfully retrieved dimension details for ID: {}", id);
            return R.ok(dimension);
        } catch (Exception e) {
            log.error("Failed to get dimension details for ID {}: {}", id, e.getMessage(), e);
            return R.fail("获取维度详情失败: " + e.getMessage());
        }
    }
    
    @PostMapping
    public R<Dimension> create(@RequestBody Dimension dimension) {
        try {
            dimension.setId(UuidUtil.generateUuid());
            dimension.setCreateTime(new Date());
            dimensionService.saveOrUpdate(dimension);
            return R.ok(dimension);
        } catch (Exception e) {
            return R.fail("创建维度失败: " + e.getMessage());
        }
    }
    
    @PutMapping("/{id}")
    public R<Dimension> update(@PathVariable String id, @RequestBody Dimension dimension) {
        try {
            dimension.setId(id);
            dimension.setUpdateTime(new Date());
            if (dimensionService.updateById(dimension)) {
                return R.ok(dimension);
            }
            return R.fail("更新维度失败");
        } catch (Exception e) {
            return R.fail("更新维度失败: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable String id) {
        try {
            if (dimensionService.removeById(id)) {
                return R.ok(true);
            }
            return R.fail("删除维度失败");
        } catch (Exception e) {
            return R.fail("删除维度失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/{id}/attributes")
    public R<List<DimensionAttribute>> getAttributes(@PathVariable String id) {
        try {
            List<DimensionAttribute> attributes = dimensionService.getDimensionAttributes(id);
            return R.ok(attributes);
        } catch (Exception e) {
            return R.fail("获取维度属性失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/{id}/attributes")
    public R<DimensionAttribute> addAttribute(@PathVariable String id, @RequestBody DimensionAttribute attribute) {
        try {
            attribute.setDimensionId(id);
            dimensionService.addDimensionAttribute(attribute);
            return R.ok(attribute);
        } catch (Exception e) {
            return R.fail("添加维度属性失败: " + e.getMessage());
        }
    }
    
    @PutMapping("/attributes/{attributeId}")
    public R<DimensionAttribute> updateAttribute(@PathVariable String attributeId, @RequestBody DimensionAttribute attribute) {
        try {
            attribute.setId(attributeId);
            dimensionService.updateDimensionAttribute(attribute);
            return R.ok(attribute);
        } catch (Exception e) {
            return R.fail("更新维度属性失败: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/attributes/{attributeId}")
    public R<Boolean> deleteAttribute(@PathVariable String attributeId) {
        try {
            dimensionService.deleteDimensionAttribute(attributeId);
            return R.ok(true);
        } catch (Exception e) {
            return R.fail("删除维度属性失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/{id}/standards")
    public R<List<DimensionStandard>> getStandards(@PathVariable String id) {
        try {
            List<DimensionStandard> standards = dimensionService.getDimensionStandards(id);
            return R.ok(standards);
        } catch (Exception e) {
            return R.fail("获取维度标准失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/{id}/standards")
    public R<DimensionStandard> linkStandard(@PathVariable String id, @RequestBody DimensionStandard standard) {
        try {
            standard.setDimensionId(id);
            dimensionService.linkStandard(standard);
            return R.ok(standard);
        } catch (Exception e) {
            return R.fail("关联标准失败: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}/standards/{standardId}")
    public R<Boolean> unlinkStandard(@PathVariable String id, @PathVariable String standardId) {
        try {
            dimensionService.unlinkStandard(id, standardId);
            return R.ok(true);
        } catch (Exception e) {
            return R.fail("解除标准关联失败: " + e.getMessage());
        }
    }

    @GetMapping("/tree")
    public R<List<Map<String, Object>>> tree(@RequestParam(required = false) List<String> domainIds) {
        try {
            // 1. 获取维度列表
            List<Dimension> dimensions;
            if (domainIds != null && !domainIds.isEmpty()) {
                // 如果指定了domainIds，获取这些域下的维度
                LambdaQueryWrapper<Dimension> queryWrapper = new LambdaQueryWrapper<>();
                if (domainIds.contains("unassigned")) {
                    queryWrapper.and(wrapper -> wrapper
                        .isNull(Dimension::getDomainId)
                        .or()
                        .in(Dimension::getDomainId, domainIds.stream()
                            .filter(id -> !"unassigned".equals(id))
                            .collect(Collectors.toList()))
                    );
                } else {
                    queryWrapper.in(Dimension::getDomainId, domainIds);
                }
                dimensions = dimensionService.list(queryWrapper);
            } else {
                // 否则获取所有维度
                dimensions = dimensionService.list();
            }
            
            // 2. 构建树结构
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
            
            // 3. 获取主题域树结构
            List<Domain> domainTree = domainService.getDomainTree();
            
            // 4. 递归构建主题域树，并添加维度节点
            for (Domain domain : domainTree) {
                tree.add(buildDomainTree(domain, dimensions));
            }
            
            return R.ok(tree);
        } catch (Exception e) {
            return R.fail("获取维度树失败: " + e.getMessage());
        }
    }

    /**
     * 递归构建主题域树
     */
    private Map<String, Object> buildDomainTree(Domain domain, List<Dimension> dimensions) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", domain.getId());
        node.put("name", domain.getName());
        node.put("type", "domain");
        
        List<Map<String, Object>> children = new ArrayList<>();
        
        // 1. 先处理子域
        if (domain.getChildren() != null && !domain.getChildren().isEmpty()) {
            for (Domain child : domain.getChildren()) {
                children.add(buildDomainTree(child, dimensions));
            }
        }
        
        // 2. 再添加当前域下的维度节点
        List<Map<String, Object>> dimensionNodes = dimensions.stream()
            .filter(d -> domain.getId().equals(d.getDomainId()))
            .map(this::convertDimensionToNode)
            .collect(Collectors.toList());
        children.addAll(dimensionNodes);
        
        // 3. 设置完整的children列表
        node.put("children", children);
        
        return node;
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
} 