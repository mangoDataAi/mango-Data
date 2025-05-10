package com.mango.test.database.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mango.test.common.util.UuidUtil;
import com.mango.test.database.entity.Domain;
import com.mango.test.database.entity.ModelEntity;
import com.mango.test.database.entity.ModelStandard;
import com.mango.test.database.entity.TableField;
import com.mango.test.database.mapper.DomainMapper;
import com.mango.test.database.mapper.ModelMapper;
import com.mango.test.database.model.DomainQuery;
import com.mango.test.database.service.DomainService;
import com.mango.test.database.service.IModelStandardService;
import com.mango.test.database.service.TableFieldService;
import com.mango.test.database.service.TableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DomainServiceImpl extends ServiceImpl<DomainMapper, Domain> implements DomainService {

    private static final Logger log = LoggerFactory.getLogger(DomainServiceImpl.class);


    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private IModelStandardService modelStandardService;

    @Autowired
    private TableService tableService;

    @Autowired
    private TableFieldService tableFieldService;
    
    @Override
    public IPage<Domain> pageDomain(DomainQuery query) {
        LambdaQueryWrapper<Domain> wrapper = new LambdaQueryWrapper<>();
        
        // 构建查询条件
        if (StringUtils.hasText(query.getName())) {
            wrapper.like(Domain::getName, query.getName());
        }
        if (StringUtils.hasText(query.getSourceType())) {
            wrapper.eq(Domain::getSourceType, query.getSourceType());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Domain::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getOwner())) {
            wrapper.eq(Domain::getOwner, query.getOwner());
        }
        if (query.getCreateTimeStart() != null) {
            wrapper.ge(Domain::getCreateTime, query.getCreateTimeStart());
        }
        if (query.getCreateTimeEnd() != null) {
            wrapper.le(Domain::getCreateTime, query.getCreateTimeEnd());
        }
        
        // 如果指定了父级ID，只查询该父级下的子域
        if (StringUtils.hasText(query.getParentId())) {
            wrapper.eq(Domain::getParentId, query.getParentId());
        }
        
        return this.page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addDomain(Domain domain) {
        domain.setCreateTime(new Date());
        domain.setUpdateTime(new Date());
        this.saveOrUpdate(domain);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDomain(Domain domain) {
        domain.setUpdateTime(new Date());
        this.updateById(domain);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDomain(String id) {
        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(String id, String status) {
        Domain domain = new Domain();
        domain.setId(id);
        domain.setStatus(status);
        domain.setUpdateTime(new Date());
        this.updateById(domain);
    }

    @Override
    public boolean hasChildren(String id) {
        LambdaQueryWrapper<Domain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Domain::getParentId, id);
        return this.count(wrapper) > 0;
    }

    @Override
    public List<Domain> getChildren(String parentId) {
        LambdaQueryWrapper<Domain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Domain::getParentId, parentId);
        return this.list(wrapper);
    }

    @Override
    public Domain getParent(String id) {
        Domain domain = this.getById(id);
        if (domain != null && StringUtils.hasText(domain.getParentId())) {
            return this.getById(domain.getParentId());
        }
        return null;
    }

    @Override
    public List<Domain> getDomainTree() {
        // 构建查询条件
        LambdaQueryWrapper<Domain> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Domain::getCreateTime);
        
        // 获取所有主题域
        List<Domain> allDomains = list(wrapper);
        
        // 构建树形结构
        List<Domain> rootDomains = new ArrayList<>();
        Map<String, List<Domain>> childrenMap = new HashMap<>();
        
        // 按父ID分组
        for (Domain domain : allDomains) {
            String parentId = domain.getParentId();
            if (parentId == null || parentId.isEmpty()) {
                rootDomains.add(domain);
            } else {
                childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(domain);
            }
        }
        
        // 递归设置子节点
        for (Domain root : rootDomains) {
            setChildren(root, childrenMap);
        }
        
        return rootDomains;
    }

    /**
     * 递归设置子节点
     */
    private void setChildren(Domain parent, Map<String, List<Domain>> childrenMap) {
        List<Domain> children = childrenMap.get(parent.getId());
        if (children != null) {
            parent.setChildren(children);
            for (Domain child : children) {
                setChildren(child, childrenMap);
            }
        }
    }

    @Override
    public List<Domain> getDomainList(String parentId) {
        LambdaQueryWrapper<Domain> wrapper = new LambdaQueryWrapper<>();
        
        if (parentId == null||parentId.equals("home")) {
            // 如果传入空字符串，同时查询parentId为null和空字符串的记录
            wrapper.and(w -> w.isNull(Domain::getParentId).or().eq(Domain::getParentId, ""));
        } else {
            // 如果传入非空字符串，查询指定父级下的主题域
            wrapper.eq(Domain::getParentId, parentId);
        }
        
        // 按创建时间排序
        wrapper.orderByAsc(Domain::getCreateTime);
        
        return list(wrapper);
    }


    /**
     * 创建统一格式的问题对象
     * @param objectType 对象类型 (table, column, primaryKey, foreignKey, index, constraint, system)
     * @param objectId 对象ID
     * @param objectName 对象名称
     * @param message 问题描述
     * @param details 详细信息列表
     * @param severity 严重程度 (高, 中, 低)
     * @param suggestion 修复建议
     * @return 问题对象
     */
    private Map<String, Object> createIssue(String objectType, String objectId, String objectName, 
                                           String message, List<String> details, 
                                           String severity, String suggestion) {
        Map<String, Object> issue = new HashMap<>();
        issue.put("objectType", objectType);
        issue.put("objectId", objectId);
        issue.put("objectName", objectName);
        issue.put("message", message);
        issue.put("details", details);
        issue.put("severity", severity);
        issue.put("suggestion", suggestion);
        return issue;
    }

    /**
     * 创建表相关的问题对象
     */
    private Map<String, Object> createTableIssue(com.mango.test.database.entity.Table table, 
                                               String message, List<String> details, 
                                               String severity, String suggestion) {
        Map<String, Object> issue = createIssue("table", table.getId(), table.getName(), 
                                              message, details, severity, suggestion);
        return issue;
    }

    /**
     * 创建字段相关的问题对象
     */
    private Map<String, Object> createColumnIssue(TableField field, com.mango.test.database.entity.Table table, 
                                                String message, List<String> details, 
                                                String severity, String suggestion) {
        Map<String, Object> issue = createIssue("column", field.getId(), field.getName(), 
                                              message, details, severity, suggestion);
        issue.put("tableId", table.getId());
        issue.put("tableName", table.getName());
        return issue;
    }

    /**
     * 创建主键相关的问题对象
     */
    private Map<String, Object> createPrimaryKeyIssue(TableField field, com.mango.test.database.entity.Table table, 
                                                    String message, List<String> details, 
                                                    String severity, String suggestion) {
        Map<String, Object> issue = createIssue("primaryKey", field.getId(), field.getName(), 
                                              message, details, severity, suggestion);
        issue.put("tableId", table.getId());
        issue.put("tableName", table.getName());
        return issue;
    }

    /**
     * 创建外键相关的问题对象
     */
    private Map<String, Object> createForeignKeyIssue(String foreignKeyId, String foreignKeyName, 
                                                    com.mango.test.database.entity.Table table, 
                                                    String message, List<String> details, 
                                                    String severity, String suggestion) {
        Map<String, Object> issue = createIssue("foreignKey", foreignKeyId, foreignKeyName, 
                                              message, details, severity, suggestion);
        issue.put("tableId", table.getId());
        issue.put("tableName", table.getName());
        return issue;
    }

    /**
     * 创建索引相关的问题对象
     */
    private Map<String, Object> createIndexIssue(String indexId, String indexName, 
                                               com.mango.test.database.entity.Table table, 
                                               String message, List<String> details, 
                                               String severity, String suggestion) {
        Map<String, Object> issue = createIssue("index", indexId, indexName, 
                                              message, details, severity, suggestion);
        issue.put("tableId", table.getId());
        issue.put("tableName", table.getName());
        return issue;
    }

    /**
     * 创建约束相关的问题对象
     */
    private Map<String, Object> createConstraintIssue(String constraintId, String constraintName, 
                                                    com.mango.test.database.entity.Table table, 
                                                    String message, List<String> details, 
                                                    String severity, String suggestion) {
        Map<String, Object> issue = createIssue("constraint", constraintId, constraintName, 
                                              message, details, severity, suggestion);
        issue.put("tableId", table.getId());
        issue.put("tableName", table.getName());
        return issue;
    }

    /**
     * 创建系统级别的问题对象
     */
    private Map<String, Object> createSystemIssue(String message) {
        return createIssue("system", "", "", message, Collections.singletonList(message), "高", "请联系系统管理员");
    }

    /**
     * 创建系统级别的问题对象（扩展版本）
     */
    private Map<String, Object> createSystemIssue(String message, List<String> details, String severity, String suggestion) {
        return createIssue("system", "", "", message, details, severity, suggestion);
    }

    @SuppressWarnings("unchecked")
    private boolean validateNamingStandard(ModelEntity model, List<com.mango.test.database.entity.Table> tables, Map<String, Object> standard, List<Map<String, Object>> issues) {
        String subType = (String) standard.get("subType");
        Map<String, Object> config = (Map<String, Object>) standard.get("rules");
        
        if (config == null) {
            log.warn("标准配置为空: standardId={}", standard.get("id"));
            return true;
        }
        
        Map<String, Object> rules = (Map<String, Object>) config.get("rules");
        if (rules == null) {
            log.warn("标准规则为空: standardId={}", standard.get("id"));
            return true;
        }
        
        boolean passed = true;
        
        // 根据子类型执行不同的验证
        switch (subType) {
            case "TABLE":
                passed = validateTableNaming(tables, rules, issues);
                break;
            case "COLUMN":
                passed = validateColumnNaming(tables, rules, issues);
                break;
            case "PRIMARY_KEY":
                passed = validatePrimaryKeyNaming(tables, rules, issues);
                break;
            case "FOREIGN_KEY":
                passed = validateForeignKeyNaming(tables, rules, issues);
                break;
            case "INDEX":
                passed = validateIndexNaming(tables, rules, issues);
                break;
            default:
                log.warn("未知的命名规范子类型: {}", subType);
                break;
        }
        
        return passed;
    }

    /**
     * 验证结构规范
     */
    private boolean validateStructureStandard(ModelEntity model, List<com.mango.test.database.entity.Table> tables, Map<String, Object> standard, List<Map<String, Object>> issues) {
        String subType = (String) standard.get("subType");
        Map<String, Object> config = (Map<String, Object>) standard.get("rules");
        
        if (config == null) {
            log.warn("标准配置为空: standardId={}", standard.get("id"));
            return true;
        }
        
        Map<String, Object> rules = (Map<String, Object>) config.get("rules");
        if (rules == null) {
            log.warn("标准规则为空: standardId={}", standard.get("id"));
            return true;
        }
        
        boolean passed = true;
        
        // 根据子类型执行不同的验证
        switch (subType) {
            case "PRIMARY_KEY":
                passed = validatePrimaryKeyStructure(tables, rules, issues);
                break;
            case "FOREIGN_KEY":
                passed = validateForeignKeyStructure(tables, rules, issues);
                break;
            case "COLUMN":
                passed = validateColumnStructure(tables, rules, issues);
                break;
            case "INDEX":
                passed = validateIndexStructure(tables, rules, issues);
                break;
            case "CONSTRAINT":
                passed = validateConstraintStructure(tables, rules, issues);
                break;
            default:
                log.warn("未知的结构规范子类型: {}", subType);
                break;
        }
        
        return passed;
    }

    /**
     * 验证数据规范
     */
    private boolean validateDataStandard(ModelEntity model, List<com.mango.test.database.entity.Table> tables, Map<String, Object> standard, List<Map<String, Object>> issues) {
        String subType = (String) standard.get("subType");
        Map<String, Object> config = (Map<String, Object>) standard.get("rules");
        
        if (config == null) {
            log.warn("标准配置为空: standardId={}", standard.get("id"));
            return true;
        }
        
        Map<String, Object> rules = (Map<String, Object>) config.get("rules");
        if (rules == null) {
            log.warn("标准规则为空: standardId={}", standard.get("id"));
            return true;
        }
        
        boolean passed = true;
        
        // 根据子类型执行不同的验证
        switch (subType) {
            case "DATA_TYPE":
                passed = validateDataType(tables, rules, issues);
                break;
            case "LENGTH":
                passed = validateDataLength(tables, rules, issues);
                break;
            case "PRECISION":
                passed = validateDataPrecision(tables, rules, issues);
                break;
            case "DEFAULT":
                passed = validateDataDefault(tables, rules, issues);
                break;
            case "NULL":
                passed = validateDataNull(tables, rules, issues);
                break;
            default:
                log.warn("未知的数据规范子类型: {}", subType);
                break;
        }
        
        return passed;
    }

    /**
     * 验证数据类型规范
     */
    private boolean validateDataType(List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        log.info("验证数据类型规范: tablesCount={}", tables.size());
        boolean passed = true;
        
        try {
            // 获取规则参数
            List<String> forbiddenTypes = (List<String>) rules.getOrDefault("forbiddenTypes", new ArrayList<>());
            Map<String, String> fieldTypeMapping = (Map<String, String>) rules.getOrDefault("fieldTypeMapping", new HashMap<>());
            Map<String, String> recommendedTypes = (Map<String, String>) rules.getOrDefault("recommendedTypes", new HashMap<>());
            Boolean enforceConsistentTypes = Boolean.TRUE.equals(rules.get("enforceConsistentTypes"));
            
            // 如果启用了类型一致性检查，收集所有字段名和类型
            Map<String, Set<String>> fieldTypesMap = new HashMap<>();
            
            for (com.mango.test.database.entity.Table table : tables) {
                // 获取表的所有字段
                List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
                
                for (TableField field : fields) {
                    String fieldType = field.getType();
                    String fieldName = field.getName();
                    
                    // 验证禁止的数据类型
                    if (forbiddenTypes != null && !forbiddenTypes.isEmpty() && fieldType != null) {
                        String baseType = fieldType.split("\\(")[0].toLowerCase();
                        if (forbiddenTypes.contains(baseType)) {
                            passed = false;
                            List<String> details = new ArrayList<>();
                            details.add("字段使用了禁止的数据类型: " + baseType);
                            String suggestion = "避免使用 " + baseType + " 类型，建议使用更标准的数据类型";
                            issues.add(createColumnIssue(field, table, "使用了禁止的数据类型", details, "高", suggestion));
                        }
                    }
                    
                    // 验证特定字段的推荐类型
                    if (fieldTypeMapping != null && !fieldTypeMapping.isEmpty() && fieldType != null && fieldName != null) {
                        String expectedType = fieldTypeMapping.get(fieldName.toLowerCase());
                        if (expectedType != null) {
                            String baseType = fieldType.split("\\(")[0].toLowerCase();
                            if (!baseType.equalsIgnoreCase(expectedType)) {
                                passed = false;
                                List<String> details = new ArrayList<>();
                                details.add("字段数据类型不符合规范: 当前为 " + baseType + "，应为 " + expectedType);
                                String suggestion = "字段 " + fieldName + " 应使用 " + expectedType + " 类型，当前为 " + baseType;
                                issues.add(createColumnIssue(field, table, "字段数据类型不符合规范", details, "中", suggestion));
                            }
                        }
                    }
                    
                    // 验证通用字段的推荐类型
                    if (recommendedTypes != null && !recommendedTypes.isEmpty() && fieldType != null && fieldName != null) {
                        // 检查字段名是否包含特定关键词，如id、date、time等
                        for (Map.Entry<String, String> entry : recommendedTypes.entrySet()) {
                            String keyword = entry.getKey();
                            String recommendedType = entry.getValue();
                            
                            if (fieldName.toLowerCase().contains(keyword.toLowerCase())) {
                                String baseType = fieldType.split("\\(")[0].toLowerCase();
                                if (!baseType.equalsIgnoreCase(recommendedType)) {
                                    passed = false;
                                    List<String> details = new ArrayList<>();
                                    details.add("包含 '" + keyword + "' 的字段应使用 " + recommendedType + " 类型，当前为 " + baseType);
                                    String suggestion = "包含 '" + keyword + "' 的字段建议使用 " + recommendedType + " 类型";
                                    issues.add(createColumnIssue(field, table, "字段数据类型不符合命名规范", details, "低", suggestion));
                                }
                            }
                        }
                    }
                    
                    // 收集字段名和类型，用于一致性检查
                    if (enforceConsistentTypes && fieldType != null && fieldName != null) {
                        String baseType = fieldType.split("\\(")[0].toLowerCase();
                        fieldTypesMap.computeIfAbsent(fieldName.toLowerCase(), k -> new HashSet<>()).add(baseType);
                    }
                }
            }
            
            // 验证字段类型一致性
            if (enforceConsistentTypes) {
                for (Map.Entry<String, Set<String>> entry : fieldTypesMap.entrySet()) {
                    String fieldName = entry.getKey();
                    Set<String> types = entry.getValue();
                    
                    if (types.size() > 1) {
                        passed = false;
                        List<String> details = new ArrayList<>();
                        details.add("字段 '" + fieldName + "' 在不同表中使用了不同的数据类型: " + String.join(", ", types));
                        String suggestion = "同名字段应使用相同的数据类型，以保持一致性";
                        issues.add(createSystemIssue("字段类型不一致", details, "中", suggestion));
                    }
                }
            }
            
            return passed;
        } catch (Exception e) {
            log.error("验证数据类型规范失败", e);
            issues.add(createSystemIssue("验证数据类型规范时发生错误: " + e.getMessage()));
            return false;
        }
    }

    // 验证数据长度
    private boolean validateDataLength(List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        if (tables.isEmpty()) {
            return true;
        }
        
        Integer maxLength = rules.get("maxLength") instanceof Number ? ((Number) rules.get("maxLength")).intValue() : null;
        Integer minLength = rules.get("minLength") instanceof Number ? ((Number) rules.get("minLength")).intValue() : null;
        
        boolean passed = true;
        
        for (com.mango.test.database.entity.Table table : tables) {
            // 获取表的所有字段
            List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
            
            for (TableField field : fields) {
                String fieldName = field.getName();
                
                // 验证长度
                if (maxLength != null && fieldName.length() > maxLength) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("字段长度超过最大限制: " + fieldName.length() + " > " + maxLength);
                    String suggestion = "字段长度应不超过 " + maxLength + " 个字符";
                    issues.add(createColumnIssue(field, table, "字段长度超过最大限制", details, "中", suggestion));
                }
                
                if (minLength != null && fieldName.length() < minLength) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("字段长度小于最小要求: " + fieldName.length() + " < " + minLength);
                    String suggestion = "字段长度应不少于 " + minLength + " 个字符";
                    issues.add(createColumnIssue(field, table, "字段长度小于最小要求", details, "低", suggestion));
                }
            }
        }
        
        return passed;
    }

    // 验证数据精度
    private boolean validateDataPrecision(List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        if (tables.isEmpty()) {
            return true;
        }
        
        Integer maxPrecision = rules.get("maxPrecision") instanceof Number ? ((Number) rules.get("maxPrecision")).intValue() : null;
        Integer minPrecision = rules.get("minPrecision") instanceof Number ? ((Number) rules.get("minPrecision")).intValue() : null;
        
        boolean passed = true;
        
        for (com.mango.test.database.entity.Table table : tables) {
            // 获取表的所有字段
            List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
            
            for (TableField field : fields) {
                String fieldType = field.getType();
                String fieldName = field.getName();
                
                // 验证精度
                if (maxPrecision != null && fieldType.contains("(") && Integer.parseInt(fieldType.split("\\(")[1].split(",")[0]) > maxPrecision) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("字段精度超过最大限制: " + Integer.parseInt(fieldType.split("\\(")[1].split(",")[0]) + " > " + maxPrecision);
                    String suggestion = "字段精度应不超过 " + maxPrecision + " 位";
                    issues.add(createColumnIssue(field, table, "字段精度超过最大限制", details, "中", suggestion));
                }
                
                if (minPrecision != null && fieldType.contains("(") && Integer.parseInt(fieldType.split("\\(")[1].split(",")[0]) < minPrecision) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("字段精度小于最小要求: " + Integer.parseInt(fieldType.split("\\(")[1].split(",")[0]) + " < " + minPrecision);
                    String suggestion = "字段精度应不少于 " + minPrecision + " 位";
                    issues.add(createColumnIssue(field, table, "字段精度小于最小要求", details, "低", suggestion));
                }
            }
        }
        
        return passed;
    }

    // 验证默认值
    private boolean validateDataDefault(List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        log.info("验证数据默认值规范: tablesCount={}", tables.size());
        boolean passed = true;
        
        try {
            // 获取规则参数
            Map<String, String> defaultValues = (Map<String, String>) rules.getOrDefault("defaultValues", new HashMap<>());
            Boolean requireDefaultForNonNull = Boolean.TRUE.equals(rules.get("requireDefaultForNonNull"));
            List<String> forbiddenDefaults = (List<String>) rules.getOrDefault("forbiddenDefaults", new ArrayList<>());
            Boolean avoidNullDefault = Boolean.TRUE.equals(rules.get("avoidNullDefault"));
            
            for (com.mango.test.database.entity.Table table : tables) {
                // 获取表的所有字段
                List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
                
                for (TableField field : fields) {
                    String fieldName = field.getName();
                    String defaultValue = field.getDefaultValue();
                    
                    // 验证特定字段的默认值
                    if (defaultValues != null && defaultValues.containsKey(fieldName.toLowerCase())) {
                        String expectedDefault = defaultValues.get(fieldName.toLowerCase());
                        
                        // 如果期望的默认值是NULL，则检查字段是否没有默认值
                        if ("NULL".equalsIgnoreCase(expectedDefault)) {
                            if (defaultValue != null && !defaultValue.isEmpty()) {
                                passed = false;
                                List<String> details = new ArrayList<>();
                                details.add("字段默认值不符合规范: 当前值为 " + defaultValue + "，应为 NULL");
                                String suggestion = "字段 " + fieldName + " 应不设置默认值";
                                issues.add(createColumnIssue(field, table, "字段默认值不符合规范", details, "中", suggestion));
                            }
                        } else if (defaultValue == null || !defaultValue.equals(expectedDefault)) {
                            passed = false;
                            List<String> details = new ArrayList<>();
                            details.add("字段默认值不符合规范: 当前值为 " + (defaultValue == null ? "NULL" : defaultValue) + "，应为 " + expectedDefault);
                            String suggestion = "字段 " + fieldName + " 应使用默认值: " + expectedDefault;
                            issues.add(createColumnIssue(field, table, "字段默认值不符合规范", details, "中", suggestion));
                        }
                    }
                    
                    // 验证非空字段是否有默认值
                    if (requireDefaultForNonNull && field.getNotNull() != null && field.getNotNull() == 1) {
                        if (defaultValue == null || defaultValue.isEmpty()) {
                            passed = false;
                            List<String> details = new ArrayList<>();
                            details.add("非空字段应设置默认值，以避免插入数据时出错");
                            String suggestion = "请为非空字段 " + fieldName + " 设置适当的默认值";
                            issues.add(createColumnIssue(field, table, "非空字段缺少默认值", details, "中", suggestion));
                        }
                    }
                    
                    // 验证禁止的默认值
                    if (defaultValue != null && !defaultValue.isEmpty() && !forbiddenDefaults.isEmpty()) {
                        for (String forbiddenDefault : forbiddenDefaults) {
                            if (defaultValue.equalsIgnoreCase(forbiddenDefault)) {
                                passed = false;
                                List<String> details = new ArrayList<>();
                                details.add("字段使用了禁止的默认值: " + forbiddenDefault);
                                String suggestion = "请避免使用禁止的默认值: " + forbiddenDefault;
                                issues.add(createColumnIssue(field, table, "字段使用了禁止的默认值", details, "高", suggestion));
                                break;
                            }
                        }
                    }
                    
                    // 验证是否避免NULL默认值
                    if (avoidNullDefault && "NULL".equalsIgnoreCase(defaultValue)) {
                        passed = false;
                        List<String> details = new ArrayList<>();
                        details.add("字段默认值不应为NULL，应使用更有意义的默认值");
                        String suggestion = "请为字段 " + fieldName + " 设置更有意义的默认值";
                        issues.add(createColumnIssue(field, table, "字段默认值为NULL", details, "低", suggestion));
                    }
                }
            }
            
            return passed;
        } catch (Exception e) {
            log.error("验证数据默认值规范失败", e);
            issues.add(createSystemIssue("验证数据默认值规范时发生错误: " + e.getMessage()));
            return false;
        }
    }

    // 验证空值
    private boolean validateDataNull(List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        if (tables.isEmpty()) {
            return true;
        }
        
        boolean requireNotNull = Boolean.TRUE.equals(rules.get("requireNotNull"));
        
        boolean passed = true;
        
        for (com.mango.test.database.entity.Table table : tables) {
            // 获取表的所有字段
            List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
            
            for (TableField field : fields) {
                // 验证非空约束
                if (requireNotNull && field.getNotNull() != 1) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("字段应设置为非空，以确保数据完整性");
                    String suggestion = "为字段添加非空约束";
                    issues.add(createColumnIssue(field, table, "字段应设置为非空", details, "中", suggestion));
                }
            }
        }
        
        return passed;
    }

    /**
     * 验证质量规范
     */
    private boolean validateQualityStandard(ModelEntity model, List<com.mango.test.database.entity.Table> tables, Map<String, Object> standard, List<Map<String, Object>> issues) {
        log.info("验证质量规范: modelId={}, tableCount={}", model.getId(), tables.size());
        boolean passed = true;
        
        try {
            // 获取质量规范规则
            Map<String, Object> rules = (Map<String, Object>) standard.getOrDefault("rules", new HashMap<>());
            if (rules == null || rules.isEmpty()) {
                log.warn("质量规范规则为空");
                return true;
            }
            
            // 验证完整性
            passed = validateCompleteness(tables, rules, issues) && passed;
            
            // 验证准确性
            passed = validateAccuracy(tables, rules, issues) && passed;
            
            // 验证一致性
            passed = validateConsistency(tables, rules, issues) && passed;
            
            // 验证有效性
            passed = validateValidity(tables, rules, issues) && passed;
            
            // 验证时效性
            passed = validateTimeliness(tables, rules, issues) && passed;
            
            // 验证可靠性 (新增)
            passed = validateReliability(tables, rules, issues) && passed;
            
            // 验证可用性 (新增)
            passed = validateAvailability(tables, rules, issues) && passed;
            
            // 验证可维护性 (新增)
            passed = validateMaintainability(tables, rules, issues) && passed;
            
            return passed;
        } catch (Exception e) {
            log.error("验证质量规范失败", e);
            issues.add(createSystemIssue("验证质量规范时发生错误: " + e.getMessage()));
            return false;
        }
    }

    /**
     * 验证完整性规范
     */
    private boolean validateCompleteness(List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        log.info("验证完整性规范: tablesCount={}", tables.size());
        boolean passed = true;
        
        try {
            // 获取规则参数
            List<String> requiredTables = (List<String>) rules.getOrDefault("requiredTables", new ArrayList<>());
            Map<String, List<String>> requiredFields = (Map<String, List<String>>) rules.getOrDefault("requiredFields", new HashMap<>());
            Boolean requireComment = Boolean.TRUE.equals(rules.get("requireComment"));
            Boolean requirePrimaryKey = Boolean.TRUE.equals(rules.get("requirePrimaryKey"));
            Boolean requireForeignKeyConstraints = Boolean.TRUE.equals(rules.get("requireForeignKeyConstraints"));
            Boolean requireIndexes = Boolean.TRUE.equals(rules.get("requireIndexes"));
            Map<String, List<String>> requiredRelationships = (Map<String, List<String>>) rules.getOrDefault("requiredRelationships", new HashMap<>());
            
            // 检查必需的表
            if (!requiredTables.isEmpty()) {
                Set<String> existingTables = tables.stream()
                        .map(table -> table.getName().toLowerCase())
                        .collect(Collectors.toSet());
                
                for (String requiredTable : requiredTables) {
                    if (!existingTables.contains(requiredTable.toLowerCase())) {
                        passed = false;
                        List<String> details = new ArrayList<>();
                        details.add("模型缺少必需的表: " + requiredTable);
                        String suggestion = "请添加必需的表: " + requiredTable;
                        issues.add(createSystemIssue("缺少必需的表", details, "高", suggestion));
                    }
                }
            }
            
            // 检查每个表的完整性
            for (com.mango.test.database.entity.Table table : tables) {
                String tableName = table.getName();
                
                // 检查表注释
                if (requireComment && (table.getComment() == null || table.getComment().trim().isEmpty())) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("表缺少注释，影响代码可读性和维护性");
                    String suggestion = "为表添加有意义的注释";
                    issues.add(createTableIssue(table, "表缺少注释", details, "中", suggestion));
                }
                
                // 获取表的所有字段
                List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
                Set<String> fieldNames = fields.stream()
                        .map(field -> field.getName().toLowerCase())
                        .collect(Collectors.toSet());
                
                // 检查必需的字段
                if (requiredFields != null && requiredFields.containsKey(tableName.toLowerCase())) {
                    List<String> tableRequiredFields = requiredFields.get(tableName.toLowerCase());
                    if (tableRequiredFields != null) {
                        for (String requiredField : tableRequiredFields) {
                            if (!fieldNames.contains(requiredField.toLowerCase())) {
                                passed = false;
                                List<String> details = new ArrayList<>();
                                details.add("表缺少必需的字段: " + requiredField);
                                String suggestion = "请添加必需的字段: " + requiredField;
                                issues.add(createTableIssue(table, "缺少必需的字段", details, "高", suggestion));
                            }
                        }
                    }
                }
                
                // 检查字段注释
                if (requireComment) {
                    for (TableField field : fields) {
                        if (field.getComment() == null || field.getComment().trim().isEmpty()) {
                            passed = false;
                            List<String> details = new ArrayList<>();
                            details.add("字段缺少注释，影响代码可读性和维护性");
                            String suggestion = "为字段添加有意义的注释";
                            issues.add(createColumnIssue(field, table, "字段缺少注释", details, "中", suggestion));
                        }
                    }
                }
                
                // 检查主键
                if (requirePrimaryKey) {
                    List<TableField> primaryKeys = fields.stream()
                            .filter(field -> field.getIsPrimary() != null && field.getIsPrimary() == 1)
                            .collect(Collectors.toList());
                    
                    if (primaryKeys.isEmpty()) {
                        passed = false;
                        List<String> details = new ArrayList<>();
                        details.add("表缺少主键，可能导致数据完整性问题");
                        String suggestion = "请为表添加主键";
                        issues.add(createTableIssue(table, "表缺少主键", details, "高", suggestion));
                    }
                }
                
                // 检查外键约束
                if (requireForeignKeyConstraints) {
                    List<Map<String, Object>> foreignKeys = getForeignKeysByTableId(table.getId());
                    
                    if (foreignKeys == null || foreignKeys.isEmpty()) {
                        // 检查字段名是否包含可能的外键模式（如以_id结尾）
                        List<String> potentialFkFields = fields.stream()
                                .map(TableField::getName)
                                .filter(name -> name.toLowerCase().endsWith("_id") && !name.equalsIgnoreCase("id"))
                                .collect(Collectors.toList());
                        
                        if (!potentialFkFields.isEmpty()) {
                            passed = false;
                            List<String> details = new ArrayList<>();
                            details.add("表包含可能的外键字段，但缺少外键约束: " + String.join(", ", potentialFkFields));
                            String suggestion = "请为可能的外键字段添加外键约束";
                            issues.add(createTableIssue(table, "缺少外键约束", details, "中", suggestion));
                        }
                    }
                }
                
                // 检查索引
                if (requireIndexes) {
                    List<Map<String, Object>> indexes = getIndexesByTableId(table.getId());
                    
                    if (indexes == null || indexes.isEmpty()) {
                        passed = false;
                        List<String> details = new ArrayList<>();
                        details.add("表缺少索引，可能导致查询性能问题");
                        String suggestion = "请为表添加适当的索引，特别是经常用于查询条件的字段";
                        issues.add(createTableIssue(table, "表缺少索引", details, "中", suggestion));
                    }
                }
            }
            
            // 检查必需的关系
            if (requiredRelationships != null && !requiredRelationships.isEmpty()) {
                Map<String, com.mango.test.database.entity.Table> tableMap = tables.stream()
                        .collect(Collectors.toMap(table -> table.getName().toLowerCase(), table -> table));
                
                for (Map.Entry<String, List<String>> entry : requiredRelationships.entrySet()) {
                    String sourceTableName = entry.getKey();
                    List<String> targetTableNames = entry.getValue();
                    
                    if (tableMap.containsKey(sourceTableName.toLowerCase())) {
                        com.mango.test.database.entity.Table sourceTable = tableMap.get(sourceTableName.toLowerCase());
                        List<Map<String, Object>> foreignKeys = getForeignKeysByTableId(sourceTable.getId());
                        Set<String> relatedTables = new HashSet<>();
                        
                        if (foreignKeys != null) {
                            for (Map<String, Object> fk : foreignKeys) {
                                String refTableName = (String) fk.get("refTableName");
                                if (refTableName != null) {
                                    relatedTables.add(refTableName.toLowerCase());
                                }
                            }
                        }
                        
                        for (String targetTableName : targetTableNames) {
                            if (!relatedTables.contains(targetTableName.toLowerCase())) {
                                passed = false;
                                List<String> details = new ArrayList<>();
                                details.add("缺少必需的表关系: " + sourceTableName + " -> " + targetTableName);
                                String suggestion = "请添加从 " + sourceTableName + " 到 " + targetTableName + " 的外键关系";
                                issues.add(createTableIssue(sourceTable, "缺少必需的表关系", details, "高", suggestion));
                            }
                        }
                    }
                }
            }
            
            return passed;
        } catch (Exception e) {
            log.error("验证完整性规范失败", e);
            issues.add(createSystemIssue("验证完整性规范时发生错误: " + e.getMessage()));
            return false;
        }
    }

    /**
     * 验证准确性规范
     */
    private boolean validateAccuracy(List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        log.info("验证准确性规范: tablesCount={}", tables.size());
        boolean passed = true;
        
        try {
            // 获取规则参数
            Map<String, Object> precisionRules = (Map<String, Object>) rules.get("precision");
            Map<String, Object> rangeRules = (Map<String, Object>) rules.get("range");
            
            if ((precisionRules == null || precisionRules.isEmpty()) && (rangeRules == null || rangeRules.isEmpty())) {
                log.info("没有配置准确性规则，跳过准确性验证");
                return true;
            }
            
            for (com.mango.test.database.entity.Table table : tables) {
                // 获取表的所有字段
                List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
                
                for (TableField field : fields) {
                    String fieldName = field.getName();
                    String dataType = field.getType();
                    Integer precision = field.getPrecision();
                    Integer scale = field.getScale();
                    
                    // 验证精度规则
                    if (precisionRules != null && precisionRules.containsKey(fieldName)) {
                        Map<String, Object> fieldPrecision = (Map<String, Object>) precisionRules.get(fieldName);
                        Integer requiredPrecision = (Integer) fieldPrecision.get("precision");
                        Integer requiredScale = (Integer) fieldPrecision.get("scale");
                        
                        if (requiredPrecision != null && precision != null && precision < requiredPrecision) {
                            passed = false;
                            List<String> details = new ArrayList<>();
                            details.add("字段精度不符合要求");
                            details.add("字段精度应至少为 " + requiredPrecision);
                            String suggestion = "请增加字段精度至少为 " + requiredPrecision;
                            issues.add(createColumnIssue(field, table, "字段精度不符合要求", details, "中", suggestion));
                        }
                        
                        if (requiredScale != null && scale != null && scale < requiredScale) {
                            passed = false;
                            List<String> details = new ArrayList<>();
                            details.add("字段小数位数不符合要求");
                            details.add("字段小数位数应至少为 " + requiredScale);
                            String suggestion = "请增加字段小数位数至少为 " + requiredScale;
                            issues.add(createColumnIssue(field, table, "字段小数位数不符合要求", details, "中", suggestion));
                        }
                    }
                    
                    // 验证范围规则
                    if (rangeRules != null && rangeRules.containsKey(fieldName)) {
                        Map<String, Object> fieldRange = (Map<String, Object>) rangeRules.get(fieldName);
                        Object minValue = fieldRange.get("min");
                        Object maxValue = fieldRange.get("max");
                        
                        // 这里需要根据字段类型进行不同的范围验证
                        // 为简化示例，仅记录问题
                        if (minValue != null || maxValue != null) {
                            String rangeDesc = "";
                            if (minValue != null && maxValue != null) {
                                rangeDesc = minValue + " 到 " + maxValue;
                            } else if (minValue != null) {
                                rangeDesc = "大于等于 " + minValue;
                            } else {
                                rangeDesc = "小于等于 " + maxValue;
                            }
                            
                            List<String> details = new ArrayList<>();
                            details.add("字段值应在范围: " + rangeDesc);
                            String suggestion = "请确保字段值在范围内: " + rangeDesc;
                            issues.add(createColumnIssue(field, table, "字段值范围需要验证", details, "中", suggestion));
                        }
                    }
                }
            }
            
            return passed;
        } catch (Exception e) {
            log.error("验证准确性规范失败", e);
            issues.add(createSystemIssue("验证准确性规范时发生错误: " + e.getMessage()));
            return false;
        }
    }

    /**
     * 验证一致性规范
     */
    private boolean validateConsistency(List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        if (tables.isEmpty()) {
            return true;
        }
        
        Map<String, String> fieldRelations = (Map<String, String>) rules.get("fieldRelations");
        List<Map<String, String>> tableRelations = (List<Map<String, String>>) rules.get("tableRelations");
        
        boolean passed = true;
        
        // 验证字段间的一致性关系
        if (fieldRelations != null) {
            for (com.mango.test.database.entity.Table table : tables) {
                List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
                Map<String, TableField> fieldMap = fields.stream()
                    .collect(Collectors.toMap(TableField::getName, field -> field));  // 使用 getName() 而不是 getFieldName()
                
                for (Map.Entry<String, String> relation : fieldRelations.entrySet()) {
                    String sourceField = relation.getKey();
                    String targetField = relation.getValue();
                    
                    if (fieldMap.containsKey(sourceField) && fieldMap.containsKey(targetField)) {
                        TableField source = fieldMap.get(sourceField);
                        TableField target = fieldMap.get(targetField);
                        
                        // 验证类型一致性
                        if (!source.getType().equals(target.getType())) {
                            passed = false;
                            List<String> details = new ArrayList<>();
                            details.add("关联字段 " + sourceField + " 和 " + targetField + " 的类型不一致");
                            String suggestion = "请确保关联字段的类型保持一致";
                            issues.add(createSystemIssue("关联字段类型不一致: " + sourceField + " 和 " + targetField, details, "高", suggestion));
                        }
                        
                        // 验证长度一致性
                        if (source.getLength() != null && target.getLength() != null && !source.getLength().equals(target.getLength())) {
                            passed = false;
                            List<String> details = new ArrayList<>();
                            details.add("相关字段 " + sourceField + " 和 " + targetField + " 的长度不一致");
                            String suggestion = "相关字段 " + sourceField + " 和 " + targetField + " 的长度应保持一致";
                            issues.add(createRelationIssue(table.getName(), sourceField, table.getName(), targetField, 
                                                          "字段长度不一致", details, "中", suggestion));
                        }
                    }
                }
            }
        }
        
        // 验证表间的一致性关系
        if (tableRelations != null) {
            Map<String, com.mango.test.database.entity.Table> tableMap = tables.stream()
                .collect(Collectors.toMap(com.mango.test.database.entity.Table::getName, table -> table));  // 使用 getName() 而不是 getTableName()
            
            for (Map<String, String> relation : tableRelations) {
                String sourceTable = relation.get("source");
                String sourceField = relation.get("sourceField");
                String targetTable = relation.get("target");
                String targetField = relation.get("targetField");
                
                if (tableMap.containsKey(sourceTable) && tableMap.containsKey(targetTable)) {
                    List<TableField> sourceFields = tableFieldService.getFieldsByTableId(tableMap.get(sourceTable).getId());
                    List<TableField> targetFields = tableFieldService.getFieldsByTableId(tableMap.get(targetTable).getId());
                    
                    Optional<TableField> sourceFieldObj = sourceFields.stream()
                        .filter(f -> f.getName().equals(sourceField))
                        .findFirst();
                    
                    Optional<TableField> targetFieldObj = targetFields.stream()
                        .filter(f -> f.getName().equals(targetField))
                        .findFirst();
                    
                    if (sourceFieldObj.isPresent() && targetFieldObj.isPresent()) {
                        TableField source = sourceFieldObj.get();
                        TableField target = targetFieldObj.get();
                        
                        // 验证类型一致性
                        if (!source.getType().equals(target.getType())) {
                            passed = false;
                            List<String> details = new ArrayList<>();
                            details.add("关联字段 " + sourceTable + "." + sourceField + " 和 " + targetTable + "." + targetField + " 的类型不一致");
                            String suggestion = "请确保关联字段的类型保持一致";
                            issues.add(createSystemIssue("关联字段类型不一致: " + sourceTable + "." + sourceField + " 和 " + targetTable + "." + targetField, details, "高", suggestion));
                        }
                        
                        // 验证长度一致性
                        if (source.getLength() != null && target.getLength() != null && !source.getLength().equals(target.getLength())) {
                            passed = false;
                            List<String> details = new ArrayList<>();
                            details.add("相关字段 " + sourceField + " 和 " + targetField + " 的长度不一致");
                            String suggestion = "相关字段 " + sourceField + " 和 " + targetField + " 的长度应保持一致";
                            issues.add(createRelationIssue(sourceTable, sourceField, targetTable, targetField, 
                                                          "字段长度不一致", details, "中", suggestion));
                        }
                    }
                }
            }
        }
        
        return passed;
    }

    /**
     * 验证有效性规范
     */
    private boolean validateValidity(List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        log.info("验证有效性规范: tablesCount={}", tables.size());
        boolean passed = true;
        
        try {
            // 获取规则参数
            Map<String, String> patterns = (Map<String, String>) rules.get("patterns");
            Map<String, List<String>> enumValues = (Map<String, List<String>>) rules.get("enumValues");
            
            if ((patterns == null || patterns.isEmpty()) && (enumValues == null || enumValues.isEmpty())) {
                log.info("没有配置有效性规则，跳过有效性验证");
                return true;
            }
            
            for (com.mango.test.database.entity.Table table : tables) {
                // 获取表的所有字段
                List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
                
                for (TableField field : fields) {
                    String fieldName = field.getName();
                    String defaultValue = field.getDefaultValue();
                    
                    // 验证正则表达式模式
                    if (patterns != null && patterns.containsKey(fieldName) && defaultValue != null) {
                        String pattern = patterns.get(fieldName);
                        if (!defaultValue.matches(pattern)) {
                            passed = false;
                            List<String> details = new ArrayList<>();
                            details.add("字段默认值不符合有效性规则");
                            details.add("字段值应符合模式: " + pattern);
                            String suggestion = "请修改字段默认值，使其符合模式: " + pattern;
                            issues.add(createColumnIssue(field, table, "字段默认值不符合有效性规则", details, "高", suggestion));
                        }
                    }
                    
                    // 验证枚举值
                    if (enumValues != null && enumValues.containsKey(fieldName) && defaultValue != null) {
                        List<String> allowedValues = enumValues.get(fieldName);
                        if (!allowedValues.contains(defaultValue)) {
                            passed = false;
                            List<String> details = new ArrayList<>();
                            details.add("字段默认值不在允许的枚举值列表中");
                            details.add("字段值应在以下列表中: " + String.join(", ", allowedValues));
                            String suggestion = "请修改字段默认值，使其在允许的枚举值列表中: " + String.join(", ", allowedValues);
                            issues.add(createColumnIssue(field, table, "字段默认值不在允许的枚举值列表中", details, "高", suggestion));
                        }
                    }
                }
            }
            
            return passed;
        } catch (Exception e) {
            log.error("验证有效性规范失败", e);
            issues.add(createSystemIssue("验证有效性规范时发生错误: " + e.getMessage()));
            return false;
        }
    }

    /**
     * 验证时效性规范
     */
    private boolean validateTimeliness(List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        log.info("验证时效性规范: tablesCount={}", tables.size());
        boolean passed = true;
        
        try {
            // 获取规则参数
            Boolean requireTimestampColumns = Boolean.TRUE.equals(rules.get("requireTimestampColumns"));
            List<String> timestampColumns = (List<String>) rules.getOrDefault("timestampColumns", Arrays.asList("created_at", "updated_at"));
            Boolean requireAuditColumns = Boolean.TRUE.equals(rules.get("requireAuditColumns"));
            List<String> auditColumns = (List<String>) rules.getOrDefault("auditColumns", Arrays.asList("created_by", "updated_by"));
            Boolean requireVersionColumn = Boolean.TRUE.equals(rules.get("requireVersionColumn"));
            String versionColumn = (String) rules.getOrDefault("versionColumn", "version");
            Integer maxDataRetentionDays = (Integer) rules.getOrDefault("maxDataRetentionDays", null);
            Boolean requireArchiveStrategy = Boolean.TRUE.equals(rules.get("requireArchiveStrategy"));
            
            for (com.mango.test.database.entity.Table table : tables) {
                // 获取表的所有字段
                List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
                Set<String> fieldNames = fields.stream()
                        .map(field -> field.getName().toLowerCase())
                        .collect(Collectors.toSet());
                
                // 检查时间戳列
                if (requireTimestampColumns && !timestampColumns.isEmpty()) {
                    List<String> missingTimestampColumns = new ArrayList<>();
                    for (String timestampColumn : timestampColumns) {
                        if (!fieldNames.contains(timestampColumn.toLowerCase())) {
                            missingTimestampColumns.add(timestampColumn);
                        }
                    }
                    
                    if (!missingTimestampColumns.isEmpty()) {
                        passed = false;
                        List<String> details = new ArrayList<>();
                        details.add("表缺少时间戳列: " + String.join(", ", missingTimestampColumns));
                        String suggestion = "请添加时间戳列，用于记录数据的创建和更新时间";
                        issues.add(createTableIssue(table, "缺少时间戳列", details, "中", suggestion));
                    }
                }
                
                // 检查审计列
                if (requireAuditColumns && !auditColumns.isEmpty()) {
                    List<String> missingAuditColumns = new ArrayList<>();
                    for (String auditColumn : auditColumns) {
                        if (!fieldNames.contains(auditColumn.toLowerCase())) {
                            missingAuditColumns.add(auditColumn);
                        }
                    }
                    
                    if (!missingAuditColumns.isEmpty()) {
                        passed = false;
                        List<String> details = new ArrayList<>();
                        details.add("表缺少审计列: " + String.join(", ", missingAuditColumns));
                        String suggestion = "请添加审计列，用于记录数据的创建者和更新者";
                        issues.add(createTableIssue(table, "缺少审计列", details, "中", suggestion));
                    }
                }
                
                // 检查版本列
                if (requireVersionColumn && !versionColumn.isEmpty()) {
                    if (!fieldNames.contains(versionColumn.toLowerCase())) {
                        passed = false;
                        List<String> details = new ArrayList<>();
                        details.add("表缺少版本列: " + versionColumn);
                        String suggestion = "请添加版本列，用于实现乐观锁和数据版本控制";
                        issues.add(createTableIssue(table, "缺少版本列", details, "中", suggestion));
                    }
                }
                
                // 检查数据保留策略
                if (maxDataRetentionDays != null || requireArchiveStrategy) {
                    // 检查表注释中是否包含数据保留策略信息
                    String tableComment = table.getComment();
                    boolean hasRetentionInfo = false;
                    
                    if (tableComment != null && !tableComment.isEmpty()) {
                        // 检查注释中是否包含数据保留或归档相关的关键词
                        hasRetentionInfo = tableComment.toLowerCase().contains("retention") || 
                                          tableComment.toLowerCase().contains("archive") || 
                                          tableComment.toLowerCase().contains("保留") || 
                                          tableComment.toLowerCase().contains("归档");
                    }
                    
                    if (!hasRetentionInfo) {
                        passed = false;
                        List<String> details = new ArrayList<>();
                        if (maxDataRetentionDays != null) {
                            details.add("表应定义数据最大保留天数: " + maxDataRetentionDays + " 天");
                        }
                        if (requireArchiveStrategy) {
                            details.add("表应定义数据归档策略");
                        }
                        String suggestion = "请在表注释中添加数据保留和归档策略信息";
                        issues.add(createTableIssue(table, "缺少数据保留策略", details, "低", suggestion));
                    }
                }
            }
            
            return passed;
        } catch (Exception e) {
            log.error("验证时效性规范失败", e);
            issues.add(createSystemIssue("验证时效性规范时发生错误: " + e.getMessage()));
            return false;
        }
    }

    /**
     * 验证域标准
     */
    private boolean validateDomainStandard(ModelEntity model, Map<String, Object> standard, List<Map<String, Object>> issues) {
        log.info("验证领域规范: modelId={}", model.getId());
        boolean passed = true;
        
        try {
            // 获取规则参数
            Map<String, Object> rules = (Map<String, Object>) standard.get("rules");
            if (rules == null) {
                log.warn("领域规范缺少规则定义");
                return true;
            }
            
            // 获取标准名称作为领域名称
            String standardName = (String) standard.getOrDefault("name", "未命名标准");
            
            // 验证领域命名规范
            String domainPrefix = (String) rules.getOrDefault("domainPrefix", "");
            if (!domainPrefix.isEmpty()) {
                // 检查模型中的表名是否都以领域前缀开头
                List<com.mango.test.database.entity.Table> tables = tableService.getTablesByGroupId(model.getId());
                for (com.mango.test.database.entity.Table table : tables) {
                    String tableName = table.getName();
                    if (!tableName.toLowerCase().startsWith(domainPrefix.toLowerCase())) {
                        passed = false;
                        List<String> details = new ArrayList<>();
                        details.add("表名应以前缀 '" + domainPrefix + "' 开头");
                        String suggestion = "请修改表名，添加前缀 '" + domainPrefix + "'";
                        issues.add(createTableIssue(table, "表名不符合命名规范", details, "高", suggestion));
                    }
                }
            }
            
            // 验证领域特定的数据类型规范
            Map<String, String> domainDataTypes = (Map<String, String>) rules.getOrDefault("domainDataTypes", new HashMap<>());
            if (!domainDataTypes.isEmpty()) {
                List<com.mango.test.database.entity.Table> tables = tableService.getTablesByGroupId(model.getId());
                for (com.mango.test.database.entity.Table table : tables) {
                    List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
                    for (TableField field : fields) {
                        String fieldName = field.getName();
                        String fieldType = field.getType();
                        
                        if (fieldType != null && domainDataTypes.containsKey(fieldName.toLowerCase())) {
                            String expectedType = domainDataTypes.get(fieldName.toLowerCase());
                            String baseType = fieldType.split("\\(")[0].toLowerCase();
                            
                            if (!baseType.equalsIgnoreCase(expectedType)) {
                                passed = false;
                                List<String> details = new ArrayList<>();
                                details.add("在标准 '" + standardName + "' 中，字段 '" + fieldName + "' 应使用类型 '" + expectedType + "'，当前为 '" + baseType + "'");
                                String suggestion = "请修改字段类型以符合规范";
                                issues.add(createColumnIssue(field, table, "字段类型不符合规范", details, "中", suggestion));
                            }
                        }
                    }
                }
            }
            
            // 验证领域特定的必需表
            List<String> requiredTables = (List<String>) rules.getOrDefault("requiredTables", new ArrayList<>());
            if (!requiredTables.isEmpty()) {
                List<com.mango.test.database.entity.Table> tables = tableService.getTablesByGroupId(model.getId());
                Set<String> existingTables = tables.stream()
                        .map(table -> table.getName().toLowerCase())
                        .collect(Collectors.toSet());
                
                for (String requiredTable : requiredTables) {
                    if (!existingTables.contains(requiredTable.toLowerCase())) {
                        passed = false;
                        List<String> details = new ArrayList<>();
                        details.add("标准 '" + standardName + "' 要求包含表 '" + requiredTable + "'");
                        String suggestion = "请添加规范要求的表 '" + requiredTable + "'";
                        issues.add(createSystemIssue("缺少规范要求的表", details, "高", suggestion));
                    }
                }
            }
            
            // 验证领域特定的表关系
            List<Map<String, String>> requiredRelations = (List<Map<String, String>>) rules.getOrDefault("requiredRelations", new ArrayList<>());
            if (!requiredRelations.isEmpty()) {
                List<com.mango.test.database.entity.Table> tables = tableService.getTablesByGroupId(model.getId());
                Map<String, com.mango.test.database.entity.Table> tableMap = tables.stream()
                        .collect(Collectors.toMap(table -> table.getName().toLowerCase(), table -> table));
                
                for (Map<String, String> relation : requiredRelations) {
                    String sourceTable = relation.get("sourceTable");
                    String targetTable = relation.get("targetTable");
                    
                    if (sourceTable == null || targetTable == null) {
                        continue;
                    }
                    
                    // 检查表是否存在
                    if (!tableMap.containsKey(sourceTable.toLowerCase()) || !tableMap.containsKey(targetTable.toLowerCase())) {
                        passed = false;
                        List<String> details = new ArrayList<>();
                        details.add("标准 '" + standardName + "' 要求表 '" + sourceTable + "' 和表 '" + targetTable + "' 之间存在关系");
                        String suggestion = "请确保所有规范要求的表都存在";
                        issues.add(createSystemIssue("缺少规范要求的表关系", details, "高", suggestion));
                        continue;
                    }
                    
                    // 检查外键关系
                    com.mango.test.database.entity.Table source = tableMap.get(sourceTable.toLowerCase());
                    List<Map<String, Object>> foreignKeys = getForeignKeysByTableId(source.getId());
                    
                    boolean relationExists = false;
                    if (foreignKeys != null) {
                        for (Map<String, Object> fk : foreignKeys) {
                            String refTable = (String) fk.get("refTable");
                            if (refTable != null && refTable.equalsIgnoreCase(targetTable)) {
                                relationExists = true;
                                break;
                            }
                        }
                    }
                    
                    if (!relationExists) {
                        passed = false;
                        List<String> details = new ArrayList<>();
                        details.add("标准 '" + standardName + "' 要求表 '" + sourceTable + "' 引用表 '" + targetTable + "'");
                        String suggestion = "请添加从表 '" + sourceTable + "' 到表 '" + targetTable + "' 的外键关系";
                        issues.add(createSystemIssue("缺少规范要求的表关系", details, "高", suggestion));
                    }
                }
            }
            
            return passed;
        } catch (Exception e) {
            log.error("验证领域规范失败", e);
            issues.add(createSystemIssue("验证领域规范时发生错误: " + e.getMessage()));
            return false;
        }
    }

    /**
     * 验证资源规范
     */
    private boolean validateResourceStandard(ModelEntity model, List<com.mango.test.database.entity.Table> tables, Map<String, Object> standard, List<Map<String, Object>> issues) {
        log.info("验证资源规范: modelId={}, tableCount={}", model.getId(), tables.size());
        boolean passed = true;
        
        try {
            // 获取资源规范规则
            Map<String, Object> rules = (Map<String, Object>) standard.getOrDefault("rules", new HashMap<>());
            if (rules == null || rules.isEmpty()) {
                log.warn("资源规范规则为空");
                return true;
            }
            
            // 验证存储限制
            passed = validateStorageLimit(model, tables, rules, issues) && passed;
            
            // 验证并发用户
            passed = validateConcurrentUsers(model, tables, rules, issues) && passed;
            
            // 验证优先级
            passed = validatePriority(model, tables, rules, issues) && passed;
            
            // 验证配额执行
            passed = validateQuotaEnforcement(model, tables, rules, issues) && passed;
            
            return passed;
        } catch (Exception e) {
            log.error("验证资源规范失败", e);
            issues.add(createSystemIssue("验证资源规范时发生错误: " + e.getMessage()));
            return false;
        }
    }

    private boolean validateStorageLimit(ModelEntity model, List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        log.info("验证存储限制: modelId={}", model.getId());
        boolean passed = true;
        
        try {
            // 获取规则参数
            Integer storageLimit = (Integer) rules.getOrDefault("storageLimit", null);
            String storageUnit = (String) rules.getOrDefault("storageUnit", "MB");
            
            if (storageLimit == null) {
                return true; // 没有设置存储限制，跳过验证
            }
            
            // 计算模型的估计存储大小
            long estimatedSize = calculateEstimatedStorageSize(tables);
            
            // 转换为相同单位进行比较
            long limitInBytes = convertToBytes(storageLimit, storageUnit);
            
            if (estimatedSize > limitInBytes) {
                passed = false;
                List<String> details = new ArrayList<>();
                details.add("模型估计存储大小超过限制: " + formatSize(estimatedSize) + " > " + storageLimit + " " + storageUnit);
                String suggestion = "请考虑优化表结构或增加存储限制";
                issues.add(createSystemIssue("存储大小超过限制", details, "高", suggestion));
            }
            
            return passed;
        } catch (Exception e) {
            log.error("验证存储限制失败", e);
            issues.add(createSystemIssue("验证存储限制时发生错误: " + e.getMessage()));
            return false;
        }
    }

    private long calculateEstimatedStorageSize(List<com.mango.test.database.entity.Table> tables) {
        // 估算模型的存储大小
        long totalSize = 0;
        
        for (com.mango.test.database.entity.Table table : tables) {
            List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
            long rowSize = 0;
            
            for (TableField field : fields) {
                // 根据字段类型和长度估算每个字段的大小
                rowSize += estimateFieldSize(field);
            }
            
            // 假设每个表有1000行数据作为基准
            long tableSize = rowSize * 1000;
            totalSize += tableSize;
        }
        
        return totalSize;
    }

    private long estimateFieldSize(TableField field) {
        String type = field.getType();
        Integer length = field.getLength();
        
        if (type == null) {
            return 8; // 默认大小
        }
        
        type = type.toUpperCase();
        
        if (type.contains("CHAR") || type.contains("VARCHAR") || type.contains("TEXT")) {
            return length != null ? length : 255;
        } else if (type.contains("INT")) {
            if (type.contains("TINY")) return 1;
            if (type.contains("SMALL")) return 2;
            if (type.contains("MEDIUM")) return 3;
            if (type.contains("BIG")) return 8;
            return 4; // 默认INT
        } else if (type.contains("FLOAT")) {
            return 4;
        } else if (type.contains("DOUBLE") || type.contains("DECIMAL")) {
            return 8;
        } else if (type.contains("DATE")) {
            return 3;
        } else if (type.contains("TIME")) {
            return 3;
        } else if (type.contains("DATETIME") || type.contains("TIMESTAMP")) {
            return 8;
        } else if (type.contains("BLOB") || type.contains("BINARY")) {
            if (type.contains("TINY")) return 255;
            if (type.contains("MEDIUM")) return 16777215;
            if (type.contains("LONG")) return 4294967295L;
            return 65535; // 默认BLOB
        } else {
            return 8; // 默认大小
        }
    }

    private long convertToBytes(Integer value, String unit) {
        if (value == null) {
            return 0;
        }
        
        switch (unit.toUpperCase()) {
            case "KB":
                return value * 1024L;
            case "MB":
                return value * 1024L * 1024L;
            case "GB":
                return value * 1024L * 1024L * 1024L;
            case "TB":
                return value * 1024L * 1024L * 1024L * 1024L;
            default:
                return value;
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else if (bytes < 1024L * 1024L * 1024L * 1024L) {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        } else {
            return String.format("%.2f TB", bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0));
        }
    }

    private boolean validateConcurrentUsers(ModelEntity model, List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        log.info("验证并发用户限制: modelId={}", model.getId());
        boolean passed = true;
        
        try {
            // 获取规则参数
            Integer maxConcurrentUsers = (Integer) rules.getOrDefault("maxConcurrentUsers", null);
            
            if (maxConcurrentUsers == null) {
                return true; // 没有设置并发用户限制，跳过验证
            }
            
            // 这里需要根据实际情况获取当前模型的并发用户数
            // 由于这是一个设计时验证，可能无法获取实际运行时的并发用户数
            // 可以考虑检查模型的设计是否适合高并发场景
            
            // 检查表的索引是否足够支持高并发
            if (maxConcurrentUsers > 100) {
                boolean hasEnoughIndexes = true;
                List<String> tablesWithoutIndexes = new ArrayList<>();
                
                for (com.mango.test.database.entity.Table table : tables) {
                    List<Map<String, Object>> indexes = getIndexesByTableId(table.getId());
                    if (indexes == null || indexes.isEmpty()) {
                        hasEnoughIndexes = false;
                        tablesWithoutIndexes.add(table.getName());
                    }
                }
                
                if (!hasEnoughIndexes) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("模型设计为高并发(" + maxConcurrentUsers + "用户)，但以下表缺少索引:");
                    details.addAll(tablesWithoutIndexes);
                    String suggestion = "请为高并发环境下的表添加适当的索引";
                    issues.add(createSystemIssue("高并发环境下缺少索引", details, "中", suggestion));
                }
            }
            
            return passed;
        } catch (Exception e) {
            log.error("验证并发用户限制失败", e);
            issues.add(createSystemIssue("验证并发用户限制时发生错误: " + e.getMessage()));
            return false;
        }
    }

    private boolean validatePriority(ModelEntity model, List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        log.info("验证优先级设置: modelId={}", model.getId());
        boolean passed = true;
        
        try {
            // 获取规则参数
            String priority = (String) rules.getOrDefault("priority", null);
            
            if (priority == null) {
                return true; // 没有设置优先级，跳过验证
            }
            
            // 根据优先级检查模型设计是否符合要求
            if ("high".equals(priority) || "critical".equals(priority)) {
                // 对于高优先级或紧急优先级的模型，检查是否有足够的性能优化
                boolean hasPerformanceIssues = false;
                List<String> performanceIssues = new ArrayList<>();
                
                // 检查是否有大表没有分区
                for (com.mango.test.database.entity.Table table : tables) {
                    List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
                    if (fields.size() > 20) {
                        // 检查是否有分区键
                        boolean hasPartitionKey = false;
                        for (TableField field : fields) {
                            if (field.getComment() != null && field.getComment().toLowerCase().contains("partition")) {
                                hasPartitionKey = true;
                                break;
                            }
                        }
                        
                        if (!hasPartitionKey) {
                            hasPerformanceIssues = true;
                            performanceIssues.add("表 " + table.getName() + " 字段数量较多(" + fields.size() + ")，但未设置分区键");
                        }
                    }
                }
                
                if (hasPerformanceIssues) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("模型优先级为 " + priority + "，但存在以下性能问题:");
                    details.addAll(performanceIssues);
                    String suggestion = "请考虑为大表添加分区策略，提高查询性能";
                    issues.add(createSystemIssue("高优先级模型存在性能问题", details, "中", suggestion));
                }
            }
            
            return passed;
        } catch (Exception e) {
            log.error("验证优先级设置失败", e);
            issues.add(createSystemIssue("验证优先级设置时发生错误: " + e.getMessage()));
            return false;
        }
    }

    private boolean validateQuotaEnforcement(ModelEntity model, List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        log.info("验证配额强制执行: modelId={}", model.getId());
        boolean passed = true;
        
        try {
            // 获取规则参数
            Boolean enforceQuota = Boolean.TRUE.equals(rules.get("enforceQuota"));
            
            if (!enforceQuota) {
                return true; // 不强制执行配额，跳过验证
            }
            
            // 检查是否有配额相关的字段或表
            boolean hasQuotaTracking = false;
            
            for (com.mango.test.database.entity.Table table : tables) {
                // 检查表名或注释是否包含配额相关信息
                if (table.getName().toLowerCase().contains("quota") || 
                    table.getName().toLowerCase().contains("limit") ||
                    (table.getComment() != null && (
                        table.getComment().toLowerCase().contains("quota") || 
                        table.getComment().toLowerCase().contains("limit")))) {
                    hasQuotaTracking = true;
                    break;
                }
                
                // 检查字段是否包含配额相关信息
                List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
                for (TableField field : fields) {
                    if (field.getName().toLowerCase().contains("quota") || 
                        field.getName().toLowerCase().contains("limit") ||
                        (field.getComment() != null && (
                            field.getComment().toLowerCase().contains("quota") || 
                            field.getComment().toLowerCase().contains("limit")))) {
                        hasQuotaTracking = true;
                        break;
                    }
                }
                
                if (hasQuotaTracking) {
                    break;
                }
            }
            
            if (!hasQuotaTracking) {
                passed = false;
                List<String> details = new ArrayList<>();
                details.add("模型设置了配额强制执行，但未找到配额跟踪相关的表或字段");
                String suggestion = "请添加配额跟踪表或字段，以支持配额强制执行";
                issues.add(createSystemIssue("缺少配额跟踪", details, "中", suggestion));
            }
            
            return passed;
        } catch (Exception e) {
            log.error("验证配额强制执行失败", e);
            issues.add(createSystemIssue("验证配额强制执行时发生错误: " + e.getMessage()));
            return false;
        }
    }

    /**
     * 获取规则名称
     */
    private String getRuleName(String ruleType, String subType) {
        if (StringUtils.hasLength(subType)) {
            // 根据规则类型和子类型获取规则名称
            Map<String, Map<String, String>> ruleNames = new HashMap<>();
            
            // 命名规范
            Map<String, String> namingRules = new HashMap<>();
            namingRules.put("TABLE", "表命名规范");
            namingRules.put("COLUMN", "字段命名规范");
            namingRules.put("PRIMARY_KEY", "主键命名规范");
            namingRules.put("FOREIGN_KEY", "外键命名规范");
            namingRules.put("INDEX", "索引命名规范");
            ruleNames.put("NAMING", namingRules);
            
            // 结构规范
            Map<String, String> structureRules = new HashMap<>();
            structureRules.put("PRIMARY_KEY", "主键设计规范");
            structureRules.put("FOREIGN_KEY", "外键设计规范");
            structureRules.put("COLUMN", "字段设计规范");
            structureRules.put("INDEX", "索引设计规范");
            structureRules.put("CONSTRAINT", "约束设计规范");
            ruleNames.put("STRUCTURE", structureRules);
            
            // 数据规范
            Map<String, String> dataRules = new HashMap<>();
            dataRules.put("DATA_TYPE", "数据类型规范");
            dataRules.put("LENGTH", "数据长度规范");
            dataRules.put("PRECISION", "数据精度规范");
            dataRules.put("DEFAULT", "默认值规范");
            dataRules.put("NULL", "空值规范");
            ruleNames.put("DATA", dataRules);
            
            // 质量规范
            Map<String, String> qualityRules = new HashMap<>();
            qualityRules.put("COMPLETENESS", "完整性规范");
            qualityRules.put("ACCURACY", "准确性规范");
            qualityRules.put("CONSISTENCY", "一致性规范");
            qualityRules.put("VALIDITY", "有效性规范");
            qualityRules.put("TIMELINESS", "及时性规范");
            ruleNames.put("QUALITY", qualityRules);
            
            // 主题域规范
            Map<String, String> domainRules = new HashMap<>();
            domainRules.put("DIVISION", "主题划分规范");
            domainRules.put("DEFINITION", "主题定义规范");
            domainRules.put("RELATION", "主题关系规范");
            ruleNames.put("DOMAIN", domainRules);
            
            // 获取规则名称
            Map<String, String> subRules = ruleNames.get(ruleType);
            if (subRules != null && subRules.containsKey(subType)) {
                return subRules.get(subType);
            }
            
            return ruleType + "-" + subType + "规则验证";
        } else {
            return ruleType + "规则验证";
        }
    }

    /**
     * 验证表命名规范
     */
    private boolean validateTableNaming(List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        log.info("验证表命名规范: tablesCount={}", tables.size());
        boolean allValid = true;
        
        try {
            // 获取规则参数
            String prefix = (String) rules.getOrDefault("prefix", "");
            String suffix = (String) rules.getOrDefault("suffix", "");
            String separator = (String) rules.getOrDefault("separator", "");
            String allowedChars = (String) rules.getOrDefault("allowedChars", "a-z0-9_");
            Integer minLength = (Integer) rules.getOrDefault("minLength", 1);
            Integer maxLength = (Integer) rules.getOrDefault("maxLength", 64);
            String namingStyle = (String) rules.getOrDefault("namingStyle", "");
            List<String> reservedWords = (List<String>) rules.getOrDefault("reservedWords", new ArrayList<>());
            List<String> forbiddenPrefixes = (List<String>) rules.getOrDefault("forbiddenPrefixes", new ArrayList<>());
            List<String> forbiddenSuffixes = (List<String>) rules.getOrDefault("forbiddenSuffixes", new ArrayList<>());
            String pattern = (String) rules.getOrDefault("pattern", "");
            Boolean requirePlural = Boolean.TRUE.equals(rules.get("requirePlural"));
            String wordSeparator = (String) rules.getOrDefault("wordSeparator", "");
            String caseConvention = (String) rules.getOrDefault("caseConvention", "");
            List<String> abbreviations = (List<String>) rules.getOrDefault("abbreviations", new ArrayList<>());
            List<String> forbiddenWords = (List<String>) rules.getOrDefault("forbiddenWords", new ArrayList<>());
            Integer maxWords = (Integer) rules.getOrDefault("maxWords", null);
            
            for (com.mango.test.database.entity.Table table : tables) {
                String tableName = table.getName();
                boolean isValid = true;
                List<String> errorDetails = new ArrayList<>();
                
                // 验证前缀
                if (!prefix.isEmpty() && !tableName.toLowerCase().startsWith(prefix.toLowerCase())) {
                    isValid = false;
                    errorDetails.add("表名必须以前缀 '" + prefix + "' 开头");
                }
                
                // 验证后缀
                if (!suffix.isEmpty() && !tableName.toLowerCase().endsWith(suffix.toLowerCase())) {
                    isValid = false;
                    errorDetails.add("表名必须以后缀 '" + suffix + "' 结尾");
                }
                
                // 验证分隔符
                if (!separator.isEmpty()) {
                    // 去掉前缀和后缀后的名称部分
                    String nameWithoutPrefixSuffix = tableName;
                    if (!prefix.isEmpty() && tableName.toLowerCase().startsWith(prefix.toLowerCase())) {
                        nameWithoutPrefixSuffix = nameWithoutPrefixSuffix.substring(prefix.length());
                    }
                    if (!suffix.isEmpty() && nameWithoutPrefixSuffix.toLowerCase().endsWith(suffix.toLowerCase())) {
                        nameWithoutPrefixSuffix = nameWithoutPrefixSuffix.substring(0, nameWithoutPrefixSuffix.length() - suffix.length());
                    }
                    
                    // 检查分隔符
                    if (!nameWithoutPrefixSuffix.isEmpty()) {
                        String[] parts = nameWithoutPrefixSuffix.split(Pattern.quote(separator));
                        if (parts.length <= 1) {
                            isValid = false;
                            errorDetails.add("表名应使用分隔符 '" + separator + "' 分隔单词");
                        }
                    }
                }
                
                // 验证命名风格
                if (!namingStyle.isEmpty() && !validateNamingStyle(tableName, namingStyle)) {
                    isValid = false;
                    errorDetails.add("表名应使用 " + getNameStyleDescription(namingStyle) + " 风格");
                }
                
                // 验证保留字
                if (!reservedWords.isEmpty()) {
                    for (String reservedWord : reservedWords) {
                        if (tableName.equalsIgnoreCase(reservedWord)) {
                            isValid = false;
                            errorDetails.add("表名不能使用保留字 '" + reservedWord + "'");
                            break;
                        }
                    }
                }
                
                // 验证禁止的前缀
                if (!forbiddenPrefixes.isEmpty()) {
                    for (String forbiddenPrefix : forbiddenPrefixes) {
                        if (tableName.toLowerCase().startsWith(forbiddenPrefix.toLowerCase())) {
                            isValid = false;
                            errorDetails.add("表名不能以 '" + forbiddenPrefix + "' 开头");
                            break;
                        }
                    }
                }
                
                // 验证禁止的后缀
                if (!forbiddenSuffixes.isEmpty()) {
                    for (String forbiddenSuffix : forbiddenSuffixes) {
                        if (tableName.toLowerCase().endsWith(forbiddenSuffix.toLowerCase())) {
                            isValid = false;
                            errorDetails.add("表名不能以 '" + forbiddenSuffix + "' 结尾");
                            break;
                        }
                    }
                }
                
                // 验证命名模式
                if (!pattern.isEmpty()) {
                    try {
                        if (!tableName.matches(pattern)) {
                            isValid = false;
                            errorDetails.add("表名不符合指定的命名模式: " + pattern);
                        }
                    } catch (Exception e) {
                        log.warn("无效的正则表达式模式: {}", pattern, e);
                        errorDetails.add("命名模式配置错误: " + e.getMessage());
                    }
                }
                
                // 验证复数形式
                if (requirePlural) {
                    // 简单检查是否以's'结尾
                    if (!tableName.toLowerCase().endsWith("s")) {
                        isValid = false;
                        errorDetails.add("表名应使用复数形式（通常以's'结尾）");
                    }
                }
                
                // 验证单词分隔符
                if (!wordSeparator.isEmpty()) {
                    if (!tableName.contains(wordSeparator)) {
                        isValid = false;
                        errorDetails.add("表名应使用 '" + wordSeparator + "' 作为单词分隔符");
                    }
                }
                
                // 验证大小写约定
                if (!caseConvention.isEmpty() && !validateNamingStyle(tableName, caseConvention)) {
                    isValid = false;
                    errorDetails.add("表名应遵循 " + getNameStyleDescription(caseConvention) + " 大小写约定");
                }
                
                // 验证缩写
                if (!abbreviations.isEmpty()) {
                    boolean containsValidAbbreviation = false;
                    for (String abbreviation : abbreviations) {
                        if (tableName.toLowerCase().contains(abbreviation.toLowerCase())) {
                            containsValidAbbreviation = true;
                            break;
                        }
                    }
                    
                    if (!containsValidAbbreviation) {
                        isValid = false;
                        errorDetails.add("表名应包含允许的缩写: " + String.join(", ", abbreviations));
                    }
                }
                
                // 验证禁止的词语
                if (!forbiddenWords.isEmpty()) {
                    for (String forbiddenWord : forbiddenWords) {
                        if (tableName.toLowerCase().contains(forbiddenWord.toLowerCase())) {
                            isValid = false;
                            errorDetails.add("表名不能包含禁止的词语 '" + forbiddenWord + "'");
                            break;
                        }
                    }
                }
                
                // 验证最大单词数
                if (maxWords != null) {
                    // 使用常见的分隔符拆分单词
                    String[] words = tableName.split("[_\\-\\s.]");
                    if (words.length > maxWords) {
                        isValid = false;
                        errorDetails.add("表名中的单词数不能超过 " + maxWords + "，当前为 " + words.length);
                    }
                }
                
                // 验证长度
                if (tableName.length() < minLength) {
                    isValid = false;
                    errorDetails.add("表名长度不能小于 " + minLength + " 个字符");
                }
                
                if (tableName.length() > maxLength) {
                    isValid = false;
                    errorDetails.add("表名长度不能超过 " + maxLength + " 个字符");
                }
                
                // 验证允许的字符
                if (!validateAllowedChars(tableName, allowedChars)) {
                    isValid = false;
                    String charSetDescription = "";
                    switch (allowedChars) {
                        case "a-z0-9_":
                            charSetDescription = "字母、数字和下划线";
                            break;
                        case "a-z0-9":
                            charSetDescription = "字母和数字";
                            break;
                        case "a-z":
                            charSetDescription = "仅字母";
                            break;
                        case "0-9":
                            charSetDescription = "仅数字";
                            break;
                        default:
                            charSetDescription = allowedChars;
                    }
                    errorDetails.add("表名只能包含" + charSetDescription);
                }
                
                if (!isValid) {
                    allValid = false;
                    String suggestion = "请按照命名规范修改表名";
                    issues.add(createTableIssue(table, "表名不符合命名规范", errorDetails, "高", suggestion));
                }
            }
            
            return allValid;
        } catch (Exception e) {
            log.error("验证表命名规范失败", e);
            issues.add(createSystemIssue("验证表命名规范时发生错误: " + e.getMessage()));
            return false;
        }
    }

    /**
     * 验证命名风格
     * @param name 名称
     * @param style 风格 (camelCase, PascalCase, snake_case, kebab-case)
     * @return 是否符合风格
     */
    private boolean validateNamingStyle(String name, String style) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        
        switch (style) {
            case "camelCase":
                // 驼峰命名法：首字母小写，后续单词首字母大写
                return name.matches("^[a-z][a-zA-Z0-9]*$") && 
                       !name.contains("_") && 
                       !name.contains("-") && 
                       name.matches(".*[A-Z].*");
            case "PascalCase":
                // 帕斯卡命名法：所有单词首字母大写
                return name.matches("^[A-Z][a-zA-Z0-9]*$") && 
                       !name.contains("_") && 
                       !name.contains("-");
            case "snake_case":
                // 蛇形命名法：所有字母小写，单词用下划线分隔
                return name.matches("^[a-z][a-z0-9_]*$") && 
                       name.contains("_") && 
                       !name.contains("-") && 
                       !name.matches(".*[A-Z].*");
            case "kebab-case":
                // 烤串命名法：所有字母小写，单词用连字符分隔
                return name.matches("^[a-z][a-z0-9-]*$") && 
                       name.contains("-") && 
                       !name.contains("_") && 
                       !name.matches(".*[A-Z].*");
            case "UPPER_SNAKE_CASE":
                // 大写蛇形命名法：所有字母大写，单词用下划线分隔
                return name.matches("^[A-Z][A-Z0-9_]*$") && 
                       name.contains("_") && 
                       !name.contains("-") && 
                       !name.matches(".*[a-z].*");
            case "lowercase":
                // 纯小写：所有字母小写
                return name.matches("^[a-z][a-z0-9]*$") && 
                       !name.contains("_") && 
                       !name.contains("-");
            case "UPPERCASE":
                // 纯大写：所有字母大写
                return name.matches("^[A-Z][A-Z0-9]*$") && 
                       !name.contains("_") && 
                       !name.contains("-");
            default:
                log.warn("未知的命名风格: {}", style);
                return true;
        }
    }

    /**
     * 获取命名风格描述
     * @param style 风格
     * @return 风格描述
     */
    private String getNameStyleDescription(String style) {
        switch (style) {
            case "camelCase":
                return "驼峰命名法 (camelCase)";
            case "PascalCase":
                return "帕斯卡命名法 (PascalCase)";
            case "snake_case":
                return "蛇形命名法 (snake_case)";
            case "kebab-case":
                return "烤串命名法 (kebab-case)";
            case "UPPER_SNAKE_CASE":
                return "大写蛇形命名法 (UPPER_SNAKE_CASE)";
            case "lowercase":
                return "纯小写 (lowercase)";
            case "UPPERCASE":
                return "纯大写 (UPPERCASE)";
            default:
                return style;
        }
    }

    /**
     * 验证字段命名规范
     */
    private boolean validateColumnNaming(List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        log.info("验证列命名规范: tablesCount={}", tables.size());
        boolean allValid = true;
        
        try {
            // 获取规则参数
            String prefix = (String) rules.getOrDefault("prefix", "");
            String suffix = (String) rules.getOrDefault("suffix", "");
            String separator = (String) rules.getOrDefault("separator", "");
            String allowedChars = (String) rules.getOrDefault("allowedChars", "a-z0-9_");
            Integer minLength = (Integer) rules.getOrDefault("minLength", 1);
            Integer maxLength = (Integer) rules.getOrDefault("maxLength", 64);
            String namingStyle = (String) rules.getOrDefault("namingStyle", "");
            List<String> reservedWords = (List<String>) rules.getOrDefault("reservedWords", new ArrayList<>());
            List<String> forbiddenPrefixes = (List<String>) rules.getOrDefault("forbiddenPrefixes", new ArrayList<>());
            List<String> forbiddenSuffixes = (List<String>) rules.getOrDefault("forbiddenSuffixes", new ArrayList<>());
            String pattern = (String) rules.getOrDefault("pattern", "");
            Map<String, String> typeIndicators = (Map<String, String>) rules.getOrDefault("typeIndicators", new HashMap<>()); // 类型指示符
            
            for (com.mango.test.database.entity.Table table : tables) {
                // 获取表的所有字段
                List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
                
                for (TableField field : fields) {
                    String fieldName = field.getName();
                    String fieldType = field.getType();
                    boolean isValid = true;
                    List<String> errorDetails = new ArrayList<>();
                    
                    // 验证前缀
                    if (!prefix.isEmpty() && !fieldName.toLowerCase().startsWith(prefix.toLowerCase())) {
                        isValid = false;
                        errorDetails.add("列名必须以前缀 '" + prefix + "' 开头");
                    }
                    
                    // 验证后缀
                    if (!suffix.isEmpty() && !fieldName.toLowerCase().endsWith(suffix.toLowerCase())) {
                        isValid = false;
                        errorDetails.add("列名必须以后缀 '" + suffix + "' 结尾");
                    }
                    
                    // 验证分隔符
                    if (!separator.isEmpty()) {
                        // 去掉前缀和后缀后的名称部分
                        String nameWithoutPrefixSuffix = fieldName;
                        if (!prefix.isEmpty() && fieldName.toLowerCase().startsWith(prefix.toLowerCase())) {
                            nameWithoutPrefixSuffix = nameWithoutPrefixSuffix.substring(prefix.length());
                        }
                        if (!suffix.isEmpty() && nameWithoutPrefixSuffix.toLowerCase().endsWith(suffix.toLowerCase())) {
                            nameWithoutPrefixSuffix = nameWithoutPrefixSuffix.substring(0, nameWithoutPrefixSuffix.length() - suffix.length());
                        }
                        
                        // 检查分隔符
                        if (!nameWithoutPrefixSuffix.isEmpty()) {
                            String[] parts = nameWithoutPrefixSuffix.split(Pattern.quote(separator));
                            if (parts.length <= 1) {
                                isValid = false;
                                errorDetails.add("列名应使用分隔符 '" + separator + "' 分隔单词");
                            }
                        }
                    }
                    
                    // 验证命名风格
                    if (!namingStyle.isEmpty() && !validateNamingStyle(fieldName, namingStyle)) {
                        isValid = false;
                        errorDetails.add("列名应使用 " + getNameStyleDescription(namingStyle) + " 风格");
                    }
                    
                    // 验证保留字
                    if (!reservedWords.isEmpty()) {
                        for (String reservedWord : reservedWords) {
                            if (fieldName.equalsIgnoreCase(reservedWord)) {
                                isValid = false;
                                errorDetails.add("列名不能使用保留字 '" + reservedWord + "'");
                                break;
                            }
                        }
                    }
                    
                    // 验证禁止的前缀
                    if (!forbiddenPrefixes.isEmpty()) {
                        for (String forbiddenPrefix : forbiddenPrefixes) {
                            if (fieldName.toLowerCase().startsWith(forbiddenPrefix.toLowerCase())) {
                                isValid = false;
                                errorDetails.add("列名不能以 '" + forbiddenPrefix + "' 开头");
                                break;
                            }
                        }
                    }
                    
                    // 验证禁止的后缀
                    if (!forbiddenSuffixes.isEmpty()) {
                        for (String forbiddenSuffix : forbiddenSuffixes) {
                            if (fieldName.toLowerCase().endsWith(forbiddenSuffix.toLowerCase())) {
                                isValid = false;
                                errorDetails.add("列名不能以 '" + forbiddenSuffix + "' 结尾");
                                break;
                            }
                        }
                    }
                    
                    // 验证命名模式
                    if (!pattern.isEmpty()) {
                        try {
                            if (!fieldName.matches(pattern)) {
                                isValid = false;
                                errorDetails.add("列名不符合指定的命名模式: " + pattern);
                            }
                        } catch (Exception e) {
                            log.warn("无效的正则表达式模式: {}", pattern, e);
                            errorDetails.add("命名模式配置错误: " + e.getMessage());
                        }
                    }
                    
                    // 验证类型指示符
                    if (!typeIndicators.isEmpty() && fieldType != null) {
                        String baseType = fieldType.split("\\(")[0].toLowerCase();
                        String expectedIndicator = typeIndicators.get(baseType);
                        
                        if (expectedIndicator != null) {
                            boolean hasIndicator = false;
                            
                            // 检查字段名是否包含类型指示符
                            if (fieldName.toLowerCase().contains(expectedIndicator.toLowerCase())) {
                                hasIndicator = true;
                            }
                            
                            if (!hasIndicator) {
                                isValid = false;
                                errorDetails.add("类型为 '" + baseType + "' 的列名应包含类型指示符 '" + expectedIndicator + "'");
                            }
                        }
                    }
                    
                    // 验证长度
                    if (fieldName.length() < minLength) {
                        isValid = false;
                        errorDetails.add("列名长度不能小于 " + minLength + " 个字符");
                    }
                    
                    if (fieldName.length() > maxLength) {
                        isValid = false;
                        errorDetails.add("列名长度不能超过 " + maxLength + " 个字符");
                    }
                    
                    // 验证允许的字符
                    if (!validateAllowedChars(fieldName, allowedChars)) {
                        isValid = false;
                        String charSetDescription = "";
                        switch (allowedChars) {
                            case "a-z0-9_":
                                charSetDescription = "字母、数字和下划线";
                                break;
                            case "a-z0-9":
                                charSetDescription = "字母和数字";
                                break;
                            case "a-z":
                                charSetDescription = "仅字母";
                                break;
                            case "0-9":
                                charSetDescription = "仅数字";
                                break;
                            default:
                                charSetDescription = allowedChars;
                        }
                        errorDetails.add("列名只能包含" + charSetDescription);
                    }
                    
                    if (!isValid) {
                        allValid = false;
                        String suggestion = "请按照命名规范修改列名";
                        issues.add(createColumnIssue(field, table, "列名不符合命名规范", errorDetails, "高", suggestion));
                    }
                }
            }
            
            return allValid;
        } catch (Exception e) {
            log.error("验证列命名规范失败", e);
            issues.add(createSystemIssue("验证列命名规范时发生错误: " + e.getMessage()));
            return false;
        }
    }

    /**
     * 验证主键命名规范
     */
    private boolean validatePrimaryKeyNaming(List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        log.info("验证主键命名规范: tablesCount={}", tables.size());
        boolean allValid = true;
        
        try {
            // 获取规则参数
            String prefix = (String) rules.getOrDefault("prefix", "");
            String suffix = (String) rules.getOrDefault("suffix", "");
            String allowedChars = (String) rules.getOrDefault("allowedChars", "a-z0-9_");
            Integer minLength = (Integer) rules.getOrDefault("minLength", 1);
            Integer maxLength = (Integer) rules.getOrDefault("maxLength", 64);
            
            for (com.mango.test.database.entity.Table table : tables) {
                // 获取表的主键
                List<TableField> primaryKeys = tableFieldService.getPrimaryKeysByTableId(table.getId());
                
                if (primaryKeys.isEmpty()) {
                    // 如果表没有主键，可以记录一个问题
                    List<String> details = Collections.singletonList("数据库表应该定义主键");
                    String suggestion = "请为表添加主键";
                    issues.add(createTableIssue(table, "表没有定义主键", details, "高", suggestion));
                    allValid = false;
                    continue;
                }
                
                for (TableField pk : primaryKeys) {
                    String pkName = pk.getName();
                    boolean isValid = true;
                    List<String> errorDetails = new ArrayList<>();
                    
                    // 验证前缀
                    if (!prefix.isEmpty() && !pkName.toLowerCase().startsWith(prefix.toLowerCase())) {
                        isValid = false;
                        errorDetails.add("主键名必须以前缀 '" + prefix + "' 开头");
                    }
                    
                    // 验证后缀
                    if (!suffix.isEmpty() && !pkName.toLowerCase().endsWith(suffix.toLowerCase())) {
                        isValid = false;
                        errorDetails.add("主键名必须以后缀 '" + suffix + "' 结尾");
                    }
                    
                    // 验证长度
                    if (pkName.length() < minLength) {
                        isValid = false;
                        errorDetails.add("主键名长度不能小于 " + minLength + " 个字符");
                    }
                    
                    if (pkName.length() > maxLength) {
                        isValid = false;
                        errorDetails.add("主键名长度不能超过 " + maxLength + " 个字符");
                    }
                    
                    // 验证允许的字符
                    if (!validateAllowedChars(pkName, allowedChars)) {
                        isValid = false;
                        String charSetDescription = "";
                        switch (allowedChars) {
                            case "a-z0-9_":
                                charSetDescription = "字母、数字和下划线";
                                break;
                            case "a-z0-9":
                                charSetDescription = "字母和数字";
                                break;
                            case "a-z":
                                charSetDescription = "仅字母";
                                break;
                            case "0-9":
                                charSetDescription = "仅数字";
                                break;
                            default:
                                charSetDescription = allowedChars;
                        }
                        errorDetails.add("主键名只能包含" + charSetDescription);
                    }
                    
                    if (!isValid) {
                        allValid = false;
                        // 使用辅助方法创建统一格式的问题对象
                        String suggestion = "请按照命名规范修改主键名";
                        issues.add(createPrimaryKeyIssue(pk, table, "主键名不符合命名规范", errorDetails, "高", suggestion));
                    }
                }
            }
            
            return allValid;
        } catch (Exception e) {
            log.error("验证主键命名规范失败", e);
            issues.add(createSystemIssue("验证主键命名规范时发生错误: " + e.getMessage()));
            return false;
        }
    }

    /**
     * 验证外键命名规范
     */
    private boolean validateForeignKeyNaming(List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        log.info("验证外键命名规范: tablesCount={}", tables.size());
        boolean allValid = true;
        
        try {
            // 获取规则参数
            String prefix = (String) rules.getOrDefault("prefix", "");
            String suffix = (String) rules.getOrDefault("suffix", "");
            String separator = (String) rules.getOrDefault("separator", "");
            String allowedChars = (String) rules.getOrDefault("allowedChars", "a-z0-9_");
            Integer minLength = (Integer) rules.getOrDefault("minLength", 1);
            Integer maxLength = (Integer) rules.getOrDefault("maxLength", 64);
            String namingStyle = (String) rules.getOrDefault("namingStyle", "");
            String pattern = (String) rules.getOrDefault("pattern", "");
            Boolean includeTableNames = Boolean.TRUE.equals(rules.get("includeTableNames"));
            
            for (com.mango.test.database.entity.Table table : tables) {
                // 获取表的所有外键
                List<Map<String, Object>> foreignKeys = getForeignKeysByTableId(table.getId());
                
                if (foreignKeys == null || foreignKeys.isEmpty()) {
                    continue;
                }
                
                for (Map<String, Object> fk : foreignKeys) {
                    String fkName = (String) fk.get("name");
                    String sourceColumn = (String) fk.get("columnName");
                    String refTable = (String) fk.get("refTable");
                    String refColumn = (String) fk.get("refColumn");
                    
                    if (fkName == null) {
                        continue;
                    }
                    
                    boolean isValid = true;
                    List<String> errorDetails = new ArrayList<>();
                    
                    // 验证前缀
                    if (!prefix.isEmpty() && !fkName.toLowerCase().startsWith(prefix.toLowerCase())) {
                        isValid = false;
                        errorDetails.add("外键名必须以前缀 '" + prefix + "' 开头");
                    }
                    
                    // 验证后缀
                    if (!suffix.isEmpty() && !fkName.toLowerCase().endsWith(suffix.toLowerCase())) {
                        isValid = false;
                        errorDetails.add("外键名必须以后缀 '" + suffix + "' 结尾");
                    }
                    
                    // 验证分隔符
                    if (!separator.isEmpty()) {
                        // 去掉前缀和后缀后的名称部分
                        String nameWithoutPrefixSuffix = fkName;
                        if (!prefix.isEmpty() && fkName.toLowerCase().startsWith(prefix.toLowerCase())) {
                            nameWithoutPrefixSuffix = nameWithoutPrefixSuffix.substring(prefix.length());
                        }
                        if (!suffix.isEmpty() && nameWithoutPrefixSuffix.toLowerCase().endsWith(suffix.toLowerCase())) {
                            nameWithoutPrefixSuffix = nameWithoutPrefixSuffix.substring(0, nameWithoutPrefixSuffix.length() - suffix.length());
                        }
                        
                        // 检查分隔符
                        if (!nameWithoutPrefixSuffix.isEmpty()) {
                            String[] parts = nameWithoutPrefixSuffix.split(Pattern.quote(separator));
                            if (parts.length <= 1) {
                                isValid = false;
                                errorDetails.add("外键名应使用分隔符 '" + separator + "' 分隔单词");
                            }
                        }
                    }
                    
                    // 验证命名风格
                    if (!namingStyle.isEmpty() && !validateNamingStyle(fkName, namingStyle)) {
                        isValid = false;
                        errorDetails.add("外键名应使用 " + getNameStyleDescription(namingStyle) + " 风格");
                    }
                    
                    // 验证是否包含表名
                    if (includeTableNames) {
                        if (!(fkName.toLowerCase().contains(table.getName().toLowerCase()) && 
                              fkName.toLowerCase().contains(refTable.toLowerCase()))) {
                            isValid = false;
                            errorDetails.add("外键名应包含源表和引用表的名称");
                        }
                    }
                    
                    // 验证命名模式
                    if (!pattern.isEmpty()) {
                        try {
                            if (!fkName.matches(pattern)) {
                                isValid = false;
                                errorDetails.add("外键名不符合指定的命名模式: " + pattern);
                            }
                        } catch (Exception e) {
                            log.warn("无效的正则表达式模式: {}", pattern, e);
                            errorDetails.add("命名模式配置错误: " + e.getMessage());
                        }
                    }
                    
                    // 验证长度
                    if (fkName.length() < minLength) {
                        isValid = false;
                        errorDetails.add("外键名长度不能小于 " + minLength + " 个字符");
                    }
                    
                    if (fkName.length() > maxLength) {
                        isValid = false;
                        errorDetails.add("外键名长度不能超过 " + maxLength + " 个字符");
                    }
                    
                    // 验证允许的字符
                    if (!validateAllowedChars(fkName, allowedChars)) {
                        isValid = false;
                        String charSetDescription = "";
                        switch (allowedChars) {
                            case "a-z0-9_":
                                charSetDescription = "字母、数字和下划线";
                                break;
                            case "a-z0-9":
                                charSetDescription = "字母和数字";
                                break;
                            case "a-z":
                                charSetDescription = "仅字母";
                                break;
                            case "0-9":
                                charSetDescription = "仅数字";
                                break;
                            default:
                                charSetDescription = allowedChars;
                        }
                        errorDetails.add("外键名只能包含" + charSetDescription);
                    }
                    
                    if (!isValid) {
                        allValid = false;
                        String suggestion = "请按照命名规范修改外键名";
                        issues.add(createForeignKeyIssue(fkName, fkName, table, "外键名不符合命名规范", errorDetails, "高", suggestion));
                    }
                }
            }
            
            return allValid;
        } catch (Exception e) {
            log.error("验证外键命名规范失败", e);
            issues.add(createSystemIssue("验证外键命名规范时发生错误: " + e.getMessage()));
            return false;
        }
    }

    /**
     * 验证索引命名规范
     */
    private boolean validateIndexNaming(List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        log.info("验证索引命名规范: tablesCount={}", tables.size());
        boolean allValid = true;
        
        try {
            // 获取规则参数
            String prefix = (String) rules.getOrDefault("prefix", "");
            String suffix = (String) rules.getOrDefault("suffix", "");
            String separator = (String) rules.getOrDefault("separator", "");
            String allowedChars = (String) rules.getOrDefault("allowedChars", "a-z0-9_");
            Integer minLength = (Integer) rules.getOrDefault("minLength", 1);
            Integer maxLength = (Integer) rules.getOrDefault("maxLength", 64);
            String namingStyle = (String) rules.getOrDefault("namingStyle", "");
            String pattern = (String) rules.getOrDefault("pattern", "");
            Boolean includeTableName = Boolean.TRUE.equals(rules.get("includeTableName"));
            Boolean includeColumnNames = Boolean.TRUE.equals(rules.get("includeColumnNames"));
            Boolean uniquePrefix = Boolean.TRUE.equals(rules.get("uniquePrefix"));
            
            for (com.mango.test.database.entity.Table table : tables) {
                // 获取表的所有索引
                List<Map<String, Object>> indexes = getIndexesByTableId(table.getId());
                
                if (indexes == null || indexes.isEmpty()) {
                    continue;
                }
                
                for (Map<String, Object> index : indexes) {
                    String indexName = (String) index.get("name");
                    Boolean isUnique = Boolean.TRUE.equals(index.get("isUnique"));
                    List<String> indexColumns = (List<String>) index.get("columns");
                    
                    if (indexName == null) {
                        continue;
                    }
                    
                    boolean isValid = true;
                    List<String> errorDetails = new ArrayList<>();
                    
                    // 验证前缀
                    if (!prefix.isEmpty() && !indexName.toLowerCase().startsWith(prefix.toLowerCase())) {
                        isValid = false;
                        errorDetails.add("索引名必须以前缀 '" + prefix + "' 开头");
                    }
                    
                    // 验证唯一索引前缀
                    if (uniquePrefix && isUnique) {
                        String uniqueIndexPrefix = "uk_";
                        if (!indexName.toLowerCase().startsWith(uniqueIndexPrefix)) {
                            isValid = false;
                            errorDetails.add("唯一索引名应以 '" + uniqueIndexPrefix + "' 开头");
                        }
                    }
                    
                    // 验证后缀
                    if (!suffix.isEmpty() && !indexName.toLowerCase().endsWith(suffix.toLowerCase())) {
                        isValid = false;
                        errorDetails.add("索引名必须以后缀 '" + suffix + "' 结尾");
                    }
                    
                    // 验证分隔符
                    if (!separator.isEmpty()) {
                        // 去掉前缀和后缀后的名称部分
                        String nameWithoutPrefixSuffix = indexName;
                        if (!prefix.isEmpty() && indexName.toLowerCase().startsWith(prefix.toLowerCase())) {
                            nameWithoutPrefixSuffix = nameWithoutPrefixSuffix.substring(prefix.length());
                        }
                        if (!suffix.isEmpty() && nameWithoutPrefixSuffix.toLowerCase().endsWith(suffix.toLowerCase())) {
                            nameWithoutPrefixSuffix = nameWithoutPrefixSuffix.substring(0, nameWithoutPrefixSuffix.length() - suffix.length());
                        }
                        
                        // 检查分隔符
                        if (!nameWithoutPrefixSuffix.isEmpty()) {
                            String[] parts = nameWithoutPrefixSuffix.split(Pattern.quote(separator));
                            if (parts.length <= 1) {
                                isValid = false;
                                errorDetails.add("索引名应使用分隔符 '" + separator + "' 分隔单词");
                            }
                        }
                    }
                    
                    // 验证命名风格
                    if (!namingStyle.isEmpty() && !validateNamingStyle(indexName, namingStyle)) {
                        isValid = false;
                        errorDetails.add("索引名应使用 " + getNameStyleDescription(namingStyle) + " 风格");
                    }
                    
                    // 验证是否包含表名
                    if (includeTableName) {
                        if (!indexName.toLowerCase().contains(table.getName().toLowerCase())) {
                            isValid = false;
                            errorDetails.add("索引名应包含表名");
                        }
                    }
                    
                    // 验证是否包含列名
                    if (includeColumnNames && indexColumns != null && !indexColumns.isEmpty()) {
                        boolean containsAnyColumn = false;
                        for (String column : indexColumns) {
                            if (indexName.toLowerCase().contains(column.toLowerCase())) {
                                containsAnyColumn = true;
                                break;
                            }
                        }
                        
                        if (!containsAnyColumn) {
                            isValid = false;
                            errorDetails.add("索引名应包含至少一个索引列的名称");
                        }
                    }
                    
                    // 验证命名模式
                    if (!pattern.isEmpty()) {
                        try {
                            if (!indexName.matches(pattern)) {
                                isValid = false;
                                errorDetails.add("索引名不符合指定的命名模式: " + pattern);
                            }
                        } catch (Exception e) {
                            log.warn("无效的正则表达式模式: {}", pattern, e);
                            errorDetails.add("命名模式配置错误: " + e.getMessage());
                        }
                    }
                    
                    // 验证长度
                    if (indexName.length() < minLength) {
                        isValid = false;
                        errorDetails.add("索引名长度不能小于 " + minLength + " 个字符");
                    }
                    
                    if (indexName.length() > maxLength) {
                        isValid = false;
                        errorDetails.add("索引名长度不能超过 " + maxLength + " 个字符");
                    }
                    
                    // 验证允许的字符
                    if (!validateAllowedChars(indexName, allowedChars)) {
                        isValid = false;
                        String charSetDescription = "";
                        switch (allowedChars) {
                            case "a-z0-9_":
                                charSetDescription = "字母、数字和下划线";
                                break;
                            case "a-z0-9":
                                charSetDescription = "字母和数字";
                                break;
                            case "a-z":
                                charSetDescription = "仅字母";
                                break;
                            case "0-9":
                                charSetDescription = "仅数字";
                                break;
                            default:
                                charSetDescription = allowedChars;
                        }
                        errorDetails.add("索引名只能包含" + charSetDescription);
                    }
                    
                    if (!isValid) {
                        allValid = false;
                        String suggestion = "请按照命名规范修改索引名";
                        issues.add(createIndexIssue((String)index.get("id"), (String)index.get("name"), table, "索引名不符合命名规范", errorDetails, "高", suggestion));
                    }
                }
            }
            
            return allValid;
        } catch (Exception e) {
            log.error("验证索引命名规范失败", e);
            issues.add(createSystemIssue("验证索引命名规范时发生错误: " + e.getMessage()));
            return false;
        }
    }

    /**
     * 验证主键结构规范
     */
    private boolean validatePrimaryKeyStructure(List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        log.info("验证主键结构规范: tablesCount={}", tables.size());
        boolean passed = true;
        
        try {
            // 获取规则参数
            Boolean requirePK = Boolean.TRUE.equals(rules.get("requirePrimaryKey"));
            Boolean requireAutoIncrement = Boolean.TRUE.equals(rules.get("requireAutoIncrement"));
            Boolean requireSingleColumn = Boolean.TRUE.equals(rules.get("requireSingleColumn"));
            List<String> recommendedTypes = (List<String>) rules.getOrDefault("recommendedTypes", new ArrayList<>());
            Integer maxPKColumns = (Integer) rules.getOrDefault("maxPKColumns", 5);
            
            for (com.mango.test.database.entity.Table table : tables) {
                // 获取表的所有字段
                List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
                
                // 筛选主键字段
                List<TableField> primaryKeys = fields.stream()
                        .filter(field -> field.getIsPrimary() == 1)
                        .collect(Collectors.toList());
                
                // 检查是否有主键
                if (requirePK && primaryKeys.isEmpty()) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("表必须定义主键");
                    String suggestion = "请为表添加主键";
                    issues.add(createTableIssue(table, "表缺少主键", details, "高", suggestion));
                    continue;
                }
                
                // 检查主键列数
                if (requireSingleColumn && primaryKeys.size() > 1) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("主键应该是单列的，当前有 " + primaryKeys.size() + " 列");
                    String suggestion = "请使用单列主键";
                    issues.add(createTableIssue(table, "主键不是单列", details, "中", suggestion));
                }
                
                // 检查主键列数是否超过最大限制
                if (maxPKColumns != null && primaryKeys.size() > maxPKColumns) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("主键列数超过最大限制: " + primaryKeys.size() + " > " + maxPKColumns);
                    String suggestion = "请减少主键列数，不要超过 " + maxPKColumns + " 列";
                    issues.add(createTableIssue(table, "主键列数过多", details, "中", suggestion));
                }
                
                // 检查主键是否自增
                if (requireAutoIncrement && !primaryKeys.isEmpty()) {
                    boolean hasAutoIncrement = primaryKeys.stream()
                            .anyMatch(field -> field.getComment() != null && field.getComment().toLowerCase().contains("auto_increment"));
                    
                    if (!hasAutoIncrement) {
                        passed = false;
                        List<String> details = new ArrayList<>();
                        details.add("主键必须设置为自增");
                        String suggestion = "请将主键设置为自增";
                        issues.add(createTableIssue(table, "主键未设置自增", details, "中", suggestion));
                    }
                }
                
                // 检查主键类型是否符合推荐
                if (!recommendedTypes.isEmpty() && !primaryKeys.isEmpty()) {
                    for (TableField pk : primaryKeys) {
                        String pkType = pk.getType();
                        if (pkType != null) {
                            String baseType = pkType.split("\\(")[0].toLowerCase();
                            if (!recommendedTypes.contains(baseType)) {
                                passed = false;
                                List<String> details = new ArrayList<>();
                                details.add("主键类型不符合推荐: 当前为 " + baseType + "，推荐使用 " + String.join(", ", recommendedTypes));
                                String suggestion = "请使用推荐的主键类型: " + String.join(", ", recommendedTypes);
                                issues.add(createPrimaryKeyIssue(pk, table, "主键类型不符合推荐", details, "低", suggestion));
                            }
                        }
                    }
                }
            }
            
            return passed;
        } catch (Exception e) {
            log.error("验证主键结构规范失败", e);
            issues.add(createSystemIssue("验证主键结构规范时发生错误: " + e.getMessage()));
            return false;
        }
    }

    /**
     * 验证外键结构规范
     */
    private boolean validateForeignKeyStructure(List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        if (tables.isEmpty()) {
            return true;
        }
        
        boolean requireFK = Boolean.TRUE.equals(rules.get("requireForeignKey"));
        boolean requireIndexOnFK = Boolean.TRUE.equals(rules.get("requireIndexOnFK"));
        String onDeleteAction = (String) rules.get("onDeleteAction");
        String onUpdateAction = (String) rules.get("onUpdateAction");
        List<Map<String, String>> expectedForeignKeys = (List<Map<String, String>>) rules.get("expectedForeignKeys");
        
        boolean passed = true;
        
        // 获取所有表的映射，用于后续查找引用表
        Map<String, com.mango.test.database.entity.Table> tableMap = tables.stream()
            .collect(Collectors.toMap(com.mango.test.database.entity.Table::getName, table -> table));
        
        // 遍历所有表，检查外键
        for (com.mango.test.database.entity.Table table : tables) {
            // 获取表的所有字段 - 添加这行
            List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
            
            // 获取表的所有外键
            List<Map<String, Object>> foreignKeys = getForeignKeysByTableId(table.getId());
            
            // 如果规则要求必须有外键，但表没有外键，则记录问题
            if (requireFK && (foreignKeys == null || foreignKeys.isEmpty())) {
                passed = false;
                List<String> details = new ArrayList<>();
                details.add("表缺少外键关联，无法确保数据完整性");
                String suggestion = "为表添加适当的外键关联，以确保数据完整性";
                issues.add(createTableIssue(table, "表缺少外键关联", details, "中", suggestion));
            }
            
            // 如果有期望的外键配置，则验证是否符合要求
            if (expectedForeignKeys != null && !expectedForeignKeys.isEmpty()) {
                for (Map<String, String> expectedFK : expectedForeignKeys) {
                    String sourceTable = expectedFK.get("sourceTable");
                    String sourceColumn = expectedFK.get("sourceColumn");
                    String targetTable = expectedFK.get("targetTable");
                    String targetColumn = expectedFK.get("targetColumn");
                    
                    // 如果当前表是源表，则检查是否有对应的外键
                    if (table.getName().equals(sourceTable)) {
                        boolean fkFound = false;
                        
                        if (foreignKeys != null) {
                            for (Map<String, Object> fk : foreignKeys) {
                                String fkColumn = (String) fk.get("columnName");
                                String refTable = (String) fk.get("referencedTable");
                                String refColumn = (String) fk.get("referencedColumn");
                                
                                if (fkColumn.equals(sourceColumn) && refTable.equals(targetTable) && refColumn.equals(targetColumn)) {
                                    fkFound = true;
                                    break;
                                }
                            }
                        }
                        
                        if (!fkFound) {
                            passed = false;
                            List<String> details = new ArrayList<>();
                            details.add("缺少期望的外键关联: 从 " + sourceTable + "." + sourceColumn + " 到 " + targetTable + "." + targetColumn);
                            String suggestion = "添加从 " + sourceTable + "." + sourceColumn + " 到 " + targetTable + "." + targetColumn + " 的外键关联";
                            issues.add(createRelationIssue(sourceTable, sourceColumn, targetTable, targetColumn, 
                                                          "缺少期望的外键关联", details, "高", suggestion));
                        }
                    }
                }
            }
            
            // 验证现有外键的配置
            if (foreignKeys != null) {
                for (Map<String, Object> fk : foreignKeys) {
                    String fkName = (String) fk.get("constraintName");
                    String fkColumn = (String) fk.get("columnName");
                    String refTable = (String) fk.get("referencedTable");
                    String refColumn = (String) fk.get("referencedColumn");
                    String deleteRule = (String) fk.get("deleteRule");
                    String updateRule = (String) fk.get("updateRule");
                    
                    // 验证外键的删除规则
                    if (onDeleteAction != null && !onDeleteAction.equals(deleteRule)) {
                        passed = false;
                        List<String> details = new ArrayList<>();
                        details.add("外键删除规则不符合要求: 当前为 " + deleteRule + "，应为 " + onDeleteAction);
                        String suggestion = "外键 " + fkName + " 的删除规则应为 " + onDeleteAction + "，当前为 " + deleteRule;
                        issues.add(createForeignKeyIssue(fkName, fkName, table, "外键删除规则不符合要求", details, "中", suggestion));
                    }
                    
                    // 验证外键的更新规则
                    if (onUpdateAction != null && !onUpdateAction.equals(updateRule)) {
                        passed = false;
                        List<String> details = new ArrayList<>();
                        details.add("外键更新规则不符合要求: 当前为 " + updateRule + "，应为 " + onUpdateAction);
                        String suggestion = "外键 " + fkName + " 的更新规则应为 " + onUpdateAction + "，当前为 " + updateRule;
                        issues.add(createForeignKeyIssue(fkName, fkName, table, "外键更新规则不符合要求", details, "中", suggestion));
                    }
                    
                    // 验证外键列是否有索引
                    if (requireIndexOnFK) {
                        List<Map<String, Object>> indexes = getIndexesByTableId(table.getId());
                        boolean hasIndex = false;
                        
                        if (indexes != null) {
                            for (Map<String, Object> index : indexes) {
                                List<String> indexColumns = (List<String>) index.get("columns");
                                if (indexColumns != null && indexColumns.contains(fkColumn)) {
                                    hasIndex = true;
                                    break;
                                }
                            }
                        }
                        
                        if (!hasIndex) {
                            passed = false;
                            // 首先找到对应的字段对象
                            Optional<TableField> fieldOpt = fields.stream()
                                .filter(f -> f.getName().equals(fkColumn))
                                .findFirst();
                            
                            if (fieldOpt.isPresent()) {
                                TableField field = fieldOpt.get();
                                List<String> details = new ArrayList<>();
                                details.add("外键列缺少索引，可能影响查询性能");
                                String suggestion = "为外键列 " + fkColumn + " 创建索引，以提高查询性能";
                                issues.add(createColumnIssue(field, table, "外键列缺少索引", details, "高", suggestion));
                            } else {
                                List<String> details = new ArrayList<>();
                                details.add("外键列缺少索引，可能影响查询性能");
                                String suggestion = "为外键列 " + fkColumn + " 创建索引，以提高查询性能";
                                issues.add(createTableIssue(table, "外键列缺少索引", details, "高", suggestion));
                            }
                        }
                    }
                    
                    // 验证引用的表和列是否存在
                    com.mango.test.database.entity.Table referencedTable = tableMap.get(refTable);
                    if (referencedTable == null) {
                        passed = false;
                        // 首先找到对应的字段对象
                        Optional<TableField> fieldOpt = fields.stream()
                            .filter(f -> f.getName().equals(fkColumn))
                            .findFirst();
                        
                        if (fieldOpt.isPresent()) {
                            TableField field = fieldOpt.get();
                            List<String> details = new ArrayList<>();
                            details.add("外键引用的表不存在: " + refTable);
                            String suggestion = "修正外键引用，确保引用的表 " + refTable + " 存在";
                            issues.add(createColumnIssue(field, table, "外键引用的表不存在", details, "高", suggestion));
                        } else {
                            List<String> details = new ArrayList<>();
                            details.add("外键引用的表不存在: " + refTable);
                            String suggestion = "修正外键引用，确保引用的表 " + refTable + " 存在";
                            issues.add(createTableIssue(table, "外键引用的表不存在", details, "高", suggestion));
                        }
                    } else {
                        List<TableField> refTableFields = tableFieldService.getFieldsByTableId(referencedTable.getId());
                        boolean refColumnExists = refTableFields.stream()
                            .anyMatch(field -> field.getName().equals(refColumn));
                        
                        if (!refColumnExists) {
                            passed = false;
                            // 首先找到对应的字段对象
                            Optional<TableField> fieldOpt = fields.stream()
                                .filter(f -> f.getName().equals(fkColumn))
                                .findFirst();
                            
                            if (fieldOpt.isPresent()) {
                                TableField field = fieldOpt.get();
                                List<String> details = new ArrayList<>();
                                details.add("外键引用的列不存在: " + refTable + "." + refColumn);
                                String suggestion = "修正外键引用，确保引用的列 " + refTable + "." + refColumn + " 存在";
                                issues.add(createColumnIssue(field, table, "外键引用的列不存在", details, "高", suggestion));
                            } else {
                                List<String> details = new ArrayList<>();
                                details.add("外键引用的列不存在: " + refTable + "." + refColumn);
                                String suggestion = "修正外键引用，确保引用的列 " + refTable + "." + refColumn + " 存在";
                                issues.add(createTableIssue(table, "外键引用的列不存在", details, "高", suggestion));
                            }
                        }
                    }
                }
            }
        }
        
        return passed;
    }

    /**
     * 验证字段结构规范
     */
    private boolean validateColumnStructure(List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        log.info("验证列结构规范: tablesCount={}", tables.size());
        boolean passed = true;
        
        try {
            // 获取规则参数
            List<String> requiredFields = (List<String>) rules.getOrDefault("requiredFields", new ArrayList<>());
            Boolean requireComment = Boolean.TRUE.equals(rules.get("requireComment"));
            Boolean avoidDuplicateNames = Boolean.TRUE.equals(rules.get("avoidDuplicateNames"));
            Map<String, String> recommendedTypes = (Map<String, String>) rules.getOrDefault("recommendedTypes", new HashMap<>());
            Map<String, Integer> maxLengths = (Map<String, Integer>) rules.getOrDefault("maxLengths", new HashMap<>());
            Boolean avoidNullableFK = Boolean.TRUE.equals(rules.get("avoidNullableFK"));
            Integer maxColumns = (Integer) rules.getOrDefault("maxColumns", null);
            Boolean requireTimestampColumns = Boolean.TRUE.equals(rules.get("requireTimestampColumns"));
            Boolean requireVersionColumn = Boolean.TRUE.equals(rules.get("requireVersionColumn"));
            Boolean requireAuditColumns = Boolean.TRUE.equals(rules.get("requireAuditColumns"));
            List<String> timestampColumns = (List<String>) rules.getOrDefault("timestampColumns", Arrays.asList("created_at", "updated_at"));
            String versionColumn = (String) rules.getOrDefault("versionColumn", "version");
            List<String> auditColumns = (List<String>) rules.getOrDefault("auditColumns", Arrays.asList("created_by", "updated_by"));
            
            // 收集所有字段名，用于检查重复
            Set<String> allFieldNames = new HashSet<>();
            Map<String, List<String>> duplicateFieldNames = new HashMap<>();
            
            for (com.mango.test.database.entity.Table table : tables) {
                // 获取表的所有字段
                List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
                
                // 验证最大列数
                if (maxColumns != null && fields.size() > maxColumns) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("表中的列数超过最大限制: " + fields.size() + " > " + maxColumns);
                    String suggestion = "请减少表中的列数，不要超过 " + maxColumns + " 列";
                    issues.add(createTableIssue(table, "表列数过多", details, "中", suggestion));
                }
                
                // 获取表的所有外键
                List<Map<String, Object>> foreignKeys = getForeignKeysByTableId(table.getId());
                Set<String> fkColumns = new HashSet<>();
                if (foreignKeys != null) {
                    for (Map<String, Object> fk : foreignKeys) {
                        String fkColumn = (String) fk.get("columnName");
                        if (fkColumn != null) {
                            fkColumns.add(fkColumn.toLowerCase());
                        }
                    }
                }
                
                // 检查必需的字段
                if (!requiredFields.isEmpty()) {
                    Set<String> existingFields = fields.stream()
                            .map(field -> field.getName().toLowerCase())
                            .collect(Collectors.toSet());
                    
                    for (String requiredField : requiredFields) {
                        if (!existingFields.contains(requiredField.toLowerCase())) {
                            passed = false;
                            List<String> details = new ArrayList<>();
                            details.add("表缺少必须的字段: " + requiredField);
                            String suggestion = "表应包含字段: " + requiredField;
                            issues.add(createTableIssue(table, "缺少必须的字段", details, "高", suggestion));
                        }
                    }
                }
                
                // 检查时间戳列
                if (requireTimestampColumns && !timestampColumns.isEmpty()) {
                    Set<String> existingFields = fields.stream()
                            .map(field -> field.getName().toLowerCase())
                            .collect(Collectors.toSet());
                    
                    List<String> missingTimestampColumns = new ArrayList<>();
                    for (String timestampColumn : timestampColumns) {
                        if (!existingFields.contains(timestampColumn.toLowerCase())) {
                            missingTimestampColumns.add(timestampColumn);
                        }
                    }
                    
                    if (!missingTimestampColumns.isEmpty()) {
                        passed = false;
                        List<String> details = new ArrayList<>();
                        details.add("表缺少时间戳列: " + String.join(", ", missingTimestampColumns));
                        String suggestion = "请添加时间戳列，用于记录数据的创建和更新时间";
                        issues.add(createTableIssue(table, "缺少时间戳列", details, "中", suggestion));
                    }
                }
                
                // 检查版本列
                if (requireVersionColumn && !versionColumn.isEmpty()) {
                    Set<String> existingFields = fields.stream()
                            .map(field -> field.getName().toLowerCase())
                            .collect(Collectors.toSet());
                    
                    if (!existingFields.contains(versionColumn.toLowerCase())) {
                        passed = false;
                        List<String> details = new ArrayList<>();
                        details.add("表缺少版本列: " + versionColumn);
                        String suggestion = "请添加版本列，用于实现乐观锁";
                        issues.add(createTableIssue(table, "缺少版本列", details, "中", suggestion));
                    }
                }
                
                // 检查审计列
                if (requireAuditColumns && !auditColumns.isEmpty()) {
                    Set<String> existingFields = fields.stream()
                            .map(field -> field.getName().toLowerCase())
                            .collect(Collectors.toSet());
                    
                    List<String> missingAuditColumns = new ArrayList<>();
                    for (String auditColumn : auditColumns) {
                        if (!existingFields.contains(auditColumn.toLowerCase())) {
                            missingAuditColumns.add(auditColumn);
                        }
                    }
                    
                    if (!missingAuditColumns.isEmpty()) {
                        passed = false;
                        List<String> details = new ArrayList<>();
                        details.add("表缺少审计列: " + String.join(", ", missingAuditColumns));
                        String suggestion = "请添加审计列，用于记录数据的创建者和更新者";
                        issues.add(createTableIssue(table, "缺少审计列", details, "中", suggestion));
                    }
                }
                
                // 检查字段注释
                if (requireComment) {
                    for (TableField field : fields) {
                        if (field.getComment() == null || field.getComment().trim().isEmpty()) {
                            passed = false;
                            List<String> details = new ArrayList<>();
                            details.add("字段缺少注释，影响代码可读性和维护性");
                            String suggestion = "为字段添加有意义的注释";
                            issues.add(createColumnIssue(field, table, "字段缺少注释", details, "中", suggestion));
                        }
                    }
                }
                
                // 检查字段名重复
                if (avoidDuplicateNames) {
                    for (TableField field : fields) {
                        String fieldName = field.getName().toLowerCase();
                        if (allFieldNames.contains(fieldName)) {
                            duplicateFieldNames.computeIfAbsent(fieldName, k -> new ArrayList<>())
                                    .add(table.getName());
                        } else {
                            allFieldNames.add(fieldName);
                        }
                    }
                }
                
                // 检查推荐的字段类型
                if (!recommendedTypes.isEmpty()) {
                    for (TableField field : fields) {
                        String fieldName = field.getName().toLowerCase();
                        String fieldType = field.getType();
                        
                        if (fieldType != null && recommendedTypes.containsKey(fieldName)) {
                            String recommendedType = recommendedTypes.get(fieldName);
                            String baseType = fieldType.split("\\(")[0].toLowerCase();
                            
                            if (!baseType.equals(recommendedType.toLowerCase())) {
                                passed = false;
                                List<String> details = new ArrayList<>();
                                details.add("字段类型不符合推荐: 当前为 " + baseType + "，推荐使用 " + recommendedType);
                                String suggestion = "请使用推荐的字段类型: " + recommendedType;
                                issues.add(createColumnIssue(field, table, "字段类型不符合推荐", details, "低", suggestion));
                            }
                        }
                    }
                }
                
                // 检查字段长度限制
                if (!maxLengths.isEmpty()) {
                    for (TableField field : fields) {
                        String fieldName = field.getName().toLowerCase();
                        Integer length = field.getLength();
                        
                        if (length != null && maxLengths.containsKey(fieldName)) {
                            Integer maxLength = maxLengths.get(fieldName);
                            
                            if (length > maxLength) {
                                passed = false;
                                List<String> details = new ArrayList<>();
                                details.add("字段长度超过限制: 当前为 " + length + "，最大允许 " + maxLength);
                                String suggestion = "请减小字段长度，不要超过 " + maxLength;
                                issues.add(createColumnIssue(field, table, "字段长度超过限制", details, "中", suggestion));
                            }
                        }
                    }
                }
                
                // 检查外键列是否允许为空
                if (avoidNullableFK && !fkColumns.isEmpty()) {
                    for (TableField field : fields) {
                        String fieldName = field.getName().toLowerCase();
                        
                        if (fkColumns.contains(fieldName) && (field.getNotNull() == null || field.getNotNull() != 1)) {
                            passed = false;
                            List<String> details = new ArrayList<>();
                            details.add("外键列应设置为非空，以确保引用完整性");
                            String suggestion = "请将外键列设置为非空";
                            issues.add(createColumnIssue(field, table, "外键列允许为空", details, "中", suggestion));
                        }
                    }
                }
            }
            
            // 报告重复的字段名
            if (avoidDuplicateNames && !duplicateFieldNames.isEmpty()) {
                for (Map.Entry<String, List<String>> entry : duplicateFieldNames.entrySet()) {
                    String fieldName = entry.getKey();
                    List<String> tableNames = entry.getValue();
                    
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("字段名 '" + fieldName + "' 在多个表中重复使用: " + String.join(", ", tableNames));
                    String suggestion = "请为不同表中的字段使用不同的名称，或确保它们表示相同的概念";
                    issues.add(createSystemIssue("字段名重复使用", details, "低", suggestion));
                }
            }
            
            return passed;
        } catch (Exception e) {
            log.error("验证列结构规范失败", e);
            issues.add(createSystemIssue("验证列结构规范时发生错误: " + e.getMessage()));
            return false;
        }
    }

    /**
     * 验证索引结构规范
     */
    private boolean validateIndexStructure(List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        if (tables.isEmpty()) {
            return true;
        }
        
        boolean requirePKIndex = Boolean.TRUE.equals(rules.get("requirePKIndex"));
        boolean requireFKIndex = Boolean.TRUE.equals(rules.get("requireFKIndex"));
        boolean requireUniqueIndex = Boolean.TRUE.equals(rules.get("requireUniqueIndex"));
        Integer maxIndexCount = rules.get("maxIndexCount") instanceof Number ? ((Number) rules.get("maxIndexCount")).intValue() : null;
        Integer maxColumnsPerIndex = rules.get("maxColumnsPerIndex") instanceof Number ? ((Number) rules.get("maxColumnsPerIndex")).intValue() : null;
        List<String> indexableColumns = (List<String>) rules.get("indexableColumns");
        List<Map<String, Object>> expectedIndexes = (List<Map<String, Object>>) rules.get("expectedIndexes");
        
        boolean passed = true;
        
        for (com.mango.test.database.entity.Table table : tables) {
            // 获取表的所有索引
            List<Map<String, Object>> indexes = getIndexesByTableId(table.getId());
            
            // 获取表的所有字段
            List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
            
            // 获取主键字段
            List<TableField> primaryKeys = fields.stream()
                .filter(field -> field.getIsPrimary() != null && field.getIsPrimary() == 1)
                .collect(Collectors.toList());
            
            // 获取外键字段
            List<Map<String, Object>> foreignKeys = getForeignKeysByTableId(table.getId());
            List<String> fkColumns = foreignKeys != null ? 
                foreignKeys.stream()
                    .map(fk -> (String) fk.get("columnName"))
                    .collect(Collectors.toList()) : 
                new ArrayList<>();
            
            // 验证索引数量
            if (maxIndexCount != null && indexes != null && indexes.size() > maxIndexCount) {
                passed = false;
                Map<String, Object> issue = new HashMap<>();
                issue.put("field", table.getName());
                issue.put("issue", "索引数量超过最大限制");
                issue.put("severity", "中");
                issue.put("suggestion", "表的索引数量应不超过 " + maxIndexCount + "，当前为 " + indexes.size() + "。请考虑合并或删除不必要的索引");
                issues.add(issue);
            }
            
            // 验证主键索引
            if (requirePKIndex && !primaryKeys.isEmpty()) {
                boolean hasPKIndex = false;
                
                if (indexes != null) {
                    for (Map<String, Object> index : indexes) {
                        Boolean isPrimary = (Boolean) index.get("isPrimary");
                        if (isPrimary != null && isPrimary) {
                            hasPKIndex = true;
                            break;
                        }
                    }
                }
                
                if (!hasPKIndex) {
                    passed = false;
                    Map<String, Object> issue = new HashMap<>();
                    issue.put("field", table.getName());
                    issue.put("issue", "主键缺少索引");
                    issue.put("severity", "高");
                    issue.put("suggestion", "为主键创建索引，以提高查询性能");
                    issues.add(issue);
                }
            }
            
            // 验证外键索引
            if (requireFKIndex && !fkColumns.isEmpty()) {
                for (String fkColumn : fkColumns) {
                    boolean hasFKIndex = false;
                    
                    if (indexes != null) {
                        for (Map<String, Object> index : indexes) {
                            List<String> indexColumns = (List<String>) index.get("columns");
                            if (indexColumns != null && indexColumns.contains(fkColumn)) {
                                hasFKIndex = true;
                                break;
                            }
                        }
                    }
                    
                    if (!hasFKIndex) {
                        passed = false;
                        // 首先找到对应的字段对象
                        Optional<TableField> fieldOpt = fields.stream()
                            .filter(f -> f.getName().equals(fkColumn))
                            .findFirst();
                        
                        if (fieldOpt.isPresent()) {
                            TableField field = fieldOpt.get();
                            List<String> details = new ArrayList<>();
                            details.add("外键列缺少索引，可能影响查询性能");
                            String suggestion = "为外键列 " + fkColumn + " 创建索引，以提高查询性能";
                            issues.add(createColumnIssue(field, table, "外键列缺少索引", details, "高", suggestion));
                        } else {
                            List<String> details = new ArrayList<>();
                            details.add("外键列缺少索引，可能影响查询性能");
                            String suggestion = "为外键列 " + fkColumn + " 创建索引，以提高查询性能";
                            issues.add(createTableIssue(table, "外键列缺少索引", details, "高", suggestion));
                        }
                    }
                }
            }
            
            // 验证唯一索引
            if (requireUniqueIndex) {
                boolean hasUniqueIndex = false;
                
                if (indexes != null) {
                    for (Map<String, Object> index : indexes) {
                        Boolean isUnique = (Boolean) index.get("isUnique");
                        if (isUnique != null && isUnique) {
                            hasUniqueIndex = true;
                            break;
                        }
                    }
                }
                
                if (!hasUniqueIndex) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("表缺少唯一索引，无法确保数据唯一性");
                    String suggestion = "为表创建至少一个唯一索引，以确保数据唯一性";
                    issues.add(createTableIssue(table, "表缺少唯一索引", details, "中", suggestion));
                }
            }
            
            // 验证每个索引的列数
            if (maxColumnsPerIndex != null && indexes != null) {
                for (Map<String, Object> index : indexes) {
                    List<String> indexColumns = (List<String>) index.get("columns");
                    if (indexColumns != null && indexColumns.size() > maxColumnsPerIndex) {
                        passed = false;
                        List<String> details = new ArrayList<>();
                        details.add("索引包含的列数超过最大限制: " + indexColumns.size() + " > " + maxColumnsPerIndex);
                        String suggestion = "索引 " + index.get("name") + " 包含的列数应不超过 " + maxColumnsPerIndex + "，当前为 " + indexColumns.size();
                        issues.add(createIndexIssue((String)index.get("id"), (String)index.get("name"), table, "索引包含的列数超过最大限制", details, "中", suggestion));
                    }
                }
            }
            
            // 验证应该建立索引的列
            if (indexableColumns != null && !indexableColumns.isEmpty()) {
                for (String indexableColumn : indexableColumns) {
                    boolean columnExists = fields.stream()
                        .anyMatch(field -> field.getName().equals(indexableColumn));
                    
                    if (columnExists) {
                        boolean hasIndex = false;
                        
                        if (indexes != null) {
                            for (Map<String, Object> index : indexes) {
                                List<String> indexColumns = (List<String>) index.get("columns");
                                if (indexColumns != null && indexColumns.contains(indexableColumn)) {
                                    hasIndex = true;
                                    break;
                                }
                            }
                        }
                        
                        if (!hasIndex) {
                            passed = false;
                            // 首先找到对应的字段对象
                            Optional<TableField> fieldOpt = fields.stream()
                                .filter(f -> f.getName().equals(indexableColumn))
                                .findFirst();
                            
                            if (fieldOpt.isPresent()) {
                                TableField field = fieldOpt.get();
                                List<String> details = new ArrayList<>();
                                details.add("应建立索引的列缺少索引，可能影响查询性能");
                                String suggestion = "为列 " + indexableColumn + " 创建索引，以提高查询性能";
                                issues.add(createColumnIssue(field, table, "应建立索引的列缺少索引", details, "中", suggestion));
                            } else {
                                List<String> details = new ArrayList<>();
                                details.add("应建立索引的列缺少索引，可能影响查询性能");
                                String suggestion = "为列 " + indexableColumn + " 创建索引，以提高查询性能";
                                issues.add(createTableIssue(table, "应建立索引的列缺少索引", details, "中", suggestion));
                            }
                        }
                    }
                }
            }
            
            // 验证期望的索引
            if (expectedIndexes != null && !expectedIndexes.isEmpty()) {
                for (Map<String, Object> expectedIndex : expectedIndexes) {
                    String tableName = (String) expectedIndex.get("table");
                    List<String> columns = (List<String>) expectedIndex.get("columns");
                    Boolean unique = (Boolean) expectedIndex.get("unique");
                    
                    if (table.getName().equals(tableName) && columns != null && !columns.isEmpty()) {
                        boolean indexFound = false;
                        
                        if (indexes != null) {
                            for (Map<String, Object> index : indexes) {
                                List<String> indexColumns = (List<String>) index.get("columns");
                                Boolean isUnique = (Boolean) index.get("isUnique");
                                
                                if (indexColumns != null && indexColumns.containsAll(columns) && columns.containsAll(indexColumns)) {
                                    if (unique == null || unique.equals(isUnique)) {
                                        indexFound = true;
                                        break;
                                    }
                                }
                            }
                        }
                        
                        if (!indexFound) {
                            passed = false;
                            List<String> details = new ArrayList<>();
                            details.add("表缺少期望的索引: 包含列 " + String.join(", ", columns));
                            String suggestion = "为表创建包含列 " + String.join(", ", columns) + " 的" + (unique != null && unique ? "唯一" : "") + "索引";
                            issues.add(createTableIssue(table, "缺少期望的索引", details, "高", suggestion));
                        }
                    }
                }
            }
        }
        
        return passed;
    }

    /**
     * 验证约束结构规范
     */
    private boolean validateConstraintStructure(List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        log.info("验证约束结构规范: tablesCount={}", tables.size());
        boolean passed = true;
        
        try {
            // 获取规则参数
            Boolean requirePK = Boolean.TRUE.equals(rules.get("requirePrimaryKey"));
            Boolean requireUnique = Boolean.TRUE.equals(rules.get("requireUnique"));
            Boolean requireCheck = Boolean.TRUE.equals(rules.get("requireCheck"));
            List<String> uniqueColumns = (List<String>) rules.getOrDefault("uniqueColumns", new ArrayList<>());
            List<Map<String, String>> checkConstraints = (List<Map<String, String>>) rules.getOrDefault("checkConstraints", new ArrayList<>());
            
            for (com.mango.test.database.entity.Table table : tables) {
                // 获取表的所有字段
                List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
                
                // 获取表的所有约束
                List<Map<String, Object>> constraints = getConstraintsByTableId(table.getId());
                
                // 筛选主键约束
                List<Map<String, Object>> pkConstraints = constraints.stream()
                        .filter(c -> "PRIMARY_KEY".equals(c.get("type")))
                        .collect(Collectors.toList());
                
                // 筛选唯一约束
                List<Map<String, Object>> uniqueConstraints = constraints.stream()
                        .filter(c -> "UNIQUE".equals(c.get("type")))
                        .collect(Collectors.toList());
                
                // 筛选检查约束
                List<Map<String, Object>> checkConstraintList = constraints.stream()
                        .filter(c -> "CHECK".equals(c.get("type")))
                        .collect(Collectors.toList());
                
                // 检查是否有主键约束
                if (requirePK && pkConstraints.isEmpty()) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("表缺少主键约束，无法确保数据唯一性和完整性");
                    String suggestion = "为表添加主键约束，以确保数据唯一性和完整性";
                    issues.add(createTableIssue(table, "表缺少主键约束", details, "高", suggestion));
                }
                
                // 检查是否有唯一约束
                if (requireUnique && uniqueConstraints.isEmpty()) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("表缺少唯一约束，无法确保数据唯一性");
                    String suggestion = "为表添加至少一个唯一约束，以确保数据唯一性";
                    issues.add(createTableIssue(table, "表缺少唯一约束", details, "中", suggestion));
                }
                
                // 检查特定列是否有唯一约束
                if (!uniqueColumns.isEmpty()) {
                    // 收集所有有唯一约束的列
                    Set<String> columnsWithUniqueConstraint = new HashSet<>();
                    for (Map<String, Object> uc : uniqueConstraints) {
                        List<String> columns = (List<String>) uc.get("columns");
                        if (columns != null) {
                            for (String column : columns) {
                                columnsWithUniqueConstraint.add(column.toLowerCase());
                            }
                        }
                    }
                    
                    // 检查每个需要唯一约束的列
                    for (String uniqueColumn : uniqueColumns) {
                        boolean columnExists = fields.stream()
                                .anyMatch(field -> field.getName().equalsIgnoreCase(uniqueColumn));
                        
                        if (columnExists && !columnsWithUniqueConstraint.contains(uniqueColumn.toLowerCase())) {
                            passed = false;
                            // 首先找到对应的字段对象
                            Optional<TableField> fieldOpt = fields.stream()
                                .filter(f -> f.getName().equalsIgnoreCase(uniqueColumn))
                                .findFirst();
                            
                            if (fieldOpt.isPresent()) {
                                TableField field = fieldOpt.get();
                                List<String> details = new ArrayList<>();
                                details.add("字段缺少唯一约束，无法确保数据唯一性");
                                String suggestion = "为字段 " + uniqueColumn + " 添加唯一约束，以确保数据唯一性";
                                issues.add(createColumnIssue(field, table, "字段缺少唯一约束", details, "中", suggestion));
                            } else {
                                List<String> details = new ArrayList<>();
                                details.add("表中的字段 " + uniqueColumn + " 缺少唯一约束");
                                String suggestion = "为字段 " + uniqueColumn + " 添加唯一约束，以确保数据唯一性";
                                issues.add(createTableIssue(table, "字段缺少唯一约束", details, "中", suggestion));
                            }
                        }
                    }
                }
                
                // 检查是否有检查约束
                if (requireCheck && checkConstraintList.isEmpty()) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("表缺少检查约束，无法确保数据有效性");
                    String suggestion = "为表添加适当的检查约束，以确保数据有效性";
                    issues.add(createTableIssue(table, "表缺少检查约束", details, "中", suggestion));
                }
                
                // 检查特定的检查约束
                if (!checkConstraints.isEmpty()) {
                    // 收集所有检查约束的表达式
                    Set<String> existingCheckExpressions = new HashSet<>();
                    for (Map<String, Object> cc : checkConstraintList) {
                        String expression = (String) cc.get("expression");
                        if (expression != null) {
                            existingCheckExpressions.add(expression.toLowerCase().replaceAll("\\s+", ""));
                        }
                    }
                    
                    // 检查每个需要的检查约束
                    for (Map<String, String> checkConstraint : checkConstraints) {
                        String columnName = checkConstraint.get("column");
                        String checkExpression = checkConstraint.get("expression");
                        
                        if (columnName == null || checkExpression == null) {
                            continue;
                        }
                        
                        boolean columnExists = fields.stream()
                                .anyMatch(field -> field.getName().equalsIgnoreCase(columnName));
                        
                        if (!columnExists) {
                            continue;
                        }
                        
                        // 规范化表达式以进行比较
                        String normalizedExpression = checkExpression.toLowerCase().replaceAll("\\s+", "");
                        
                        if (!existingCheckExpressions.contains(normalizedExpression)) {
                            passed = false;
                            // 首先找到对应的字段对象
                            Optional<TableField> fieldOpt = fields.stream()
                                .filter(f -> f.getName().equalsIgnoreCase(columnName))
                                .findFirst();
                            
                            if (fieldOpt.isPresent()) {
                                TableField field = fieldOpt.get();
                                List<String> details = new ArrayList<>();
                                details.add("字段缺少检查约束: " + checkExpression);
                                String suggestion = "为字段 " + columnName + " 添加检查约束: " + checkExpression;
                                issues.add(createColumnIssue(field, table, "字段缺少检查约束", details, "中", suggestion));
                            } else {
                                List<String> details = new ArrayList<>();
                                details.add("表中的字段 " + columnName + " 缺少检查约束: " + checkExpression);
                                String suggestion = "为字段 " + columnName + " 添加检查约束: " + checkExpression;
                                issues.add(createTableIssue(table, "字段缺少检查约束", details, "中", suggestion));
                            }
                        }
                    }
                }
            }
            
            return passed;
        } catch (Exception e) {
            log.error("验证约束结构规范失败", e);
            issues.add(createSystemIssue("验证约束结构规范时发生错误: " + e.getMessage()));
            return false;
        }
    }

    /**
     * 获取表的外键列表
     * 注意：此方法需要根据实际数据库结构实现
     */
    private List<Map<String, Object>> getForeignKeysByTableId(String tableId) {
        // 实际项目中，应该从数据库或其他存储中获取外键信息
        // 这里返回一个模拟的空列表
        return new ArrayList<>();
    }

    /**
     * 获取表的索引列表
     * 注意：此方法需要根据实际数据库结构实现
     */
    private List<Map<String, Object>> getIndexesByTableId(String tableId) {
        // 实际项目中，应该从数据库或其他存储中获取索引信息
        // 这里返回一个模拟的空列表
        return new ArrayList<>();
    }

    /**
     * 获取表的约束列表
     * 注意：此方法需要根据实际数据库结构实现
     */
    private List<Map<String, Object>> getConstraintsByTableId(String tableId) {
        // 实际项目中，应该从数据库或其他存储中获取约束信息
        // 这里返回一个模拟的空列表
        return new ArrayList<>();
    }

    /**
     * 验证命名是否符合允许的字符集
     * @param name 要验证的名称
     * @param allowedChars 允许的字符集类型 (a-z0-9_, a-z0-9, a-z, 0-9)
     * @return 是否符合要求
     */
    private boolean validateAllowedChars(String name, String allowedChars) {
        if (!StringUtils.hasText(name)) { // 修改这里，使用 !hasText() 替代 isEmpty()
            return false;
        }
        
        // 转换为小写进行验证
        name = name.toLowerCase();
        
        // 根据不同的字符集类型进行验证
        switch (allowedChars) {
            case "a-z0-9_":
                // 字母、数字和下划线
                return name.matches("^[a-z0-9_]+$");
            case "a-z0-9":
                // 字母和数字
                return name.matches("^[a-z0-9]+$");
            case "a-z":
                // 仅字母
                return name.matches("^[a-z]+$");
            case "0-9":
                // 仅数字
                return name.matches("^[0-9]+$");
            default:
                log.warn("未知的字符集类型: {}", allowedChars);
                // 默认使用最宽松的验证规则
                return name.matches("^[a-z0-9_]+$");
        }
    }

    /**
     * 创建关联字段相关的问题对象
     */
    private Map<String, Object> createRelationIssue(String sourceTable, String sourceField, 
                                                  String targetTable, String targetField,
                                                  String message, List<String> details, 
                                                  String severity, String suggestion) {
        Map<String, Object> issue = createIssue("relation", "", 
                                              sourceTable + "." + sourceField + " -> " + targetTable + "." + targetField, 
                                              message, details, severity, suggestion);
        issue.put("sourceTable", sourceTable);
        issue.put("sourceField", sourceField);
        issue.put("targetTable", targetTable);
        issue.put("targetField", targetField);
        return issue;
    }

    private boolean validateMaintainability(List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        log.info("验证可维护性规范: tablesCount={}", tables.size());
        boolean passed = true;
        
        try {
            // 获取规则参数
            Boolean requireDocumentation = Boolean.TRUE.equals(rules.get("requireDocumentation"));
            Boolean requireVersioning = Boolean.TRUE.equals(rules.get("requireVersioning"));
            Boolean requireChangeLog = Boolean.TRUE.equals(rules.get("requireChangeLog"));
            Integer maxTableComplexity = (Integer) rules.getOrDefault("maxTableComplexity", null);
            
            // 检查文档要求
            if (requireDocumentation) {
                // 检查表和字段是否有足够的注释
                int tablesWithoutComment = 0;
                int fieldsWithoutComment = 0;
                List<String> tablesWithoutCommentList = new ArrayList<>();
                
                for (com.mango.test.database.entity.Table table : tables) {
                    if (table.getComment() == null || table.getComment().trim().isEmpty()) {
                        tablesWithoutComment++;
                        tablesWithoutCommentList.add(table.getName());
                    }
                    
                    List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
                    for (TableField field : fields) {
                        if (field.getComment() == null || field.getComment().trim().isEmpty()) {
                            fieldsWithoutComment++;
                        }
                    }
                }
                
                if (tablesWithoutComment > 0) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("有 " + tablesWithoutComment + " 个表缺少注释");
                    if (!tablesWithoutCommentList.isEmpty()) {
                        details.add("缺少注释的表: " + String.join(", ", tablesWithoutCommentList));
                    }
                    String suggestion = "请为所有表添加有意义的注释";
                    issues.add(createSystemIssue("表缺少注释", details, "中", suggestion));
                }
                
                if (fieldsWithoutComment > 0) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("有 " + fieldsWithoutComment + " 个字段缺少注释");
                    String suggestion = "请为所有字段添加有意义的注释";
                    issues.add(createSystemIssue("字段缺少注释", details, "中", suggestion));
                }
            }
            
            // 检查版本控制要求
            if (requireVersioning) {
                // 检查是否有版本控制相关的字段
                boolean hasVersionField = false;
                
                for (com.mango.test.database.entity.Table table : tables) {
                    List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
                    for (TableField field : fields) {
                        String fieldName = field.getName().toLowerCase();
                        if (fieldName.equals("version") || 
                            fieldName.equals("revision") || 
                            fieldName.equals("ver") || 
                            fieldName.contains("_version") || 
                            fieldName.contains("_ver")) {
                            hasVersionField = true;
                            break;
                        }
                    }
                    
                    if (hasVersionField) {
                        break;
                    }
                }
                
                if (!hasVersionField) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("模型缺少版本控制字段");
                    String suggestion = "请添加版本控制字段，如 'version'、'revision' 等";
                    issues.add(createSystemIssue("缺少版本控制", details, "中", suggestion));
                }
            }
            
            // 检查变更日志要求
            if (requireChangeLog) {
                // 检查是否有变更日志表或变更日志相关的字段
                boolean hasChangeLogTable = false;
                boolean hasChangeLogFields = false;
                
                // 检查是否有变更日志表
                for (com.mango.test.database.entity.Table table : tables) {
                    String tableName = table.getName().toLowerCase();
                    if (tableName.contains("changelog") || 
                        tableName.contains("change_log") || 
                        tableName.contains("history") || 
                        tableName.contains("audit") || 
                        tableName.contains("log")) {
                        hasChangeLogTable = true;
                        break;
                    }
                }
                
                // 检查是否有变更日志相关的字段
                if (!hasChangeLogTable) {
                    for (com.mango.test.database.entity.Table table : tables) {
                        List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
                        int changeLogFieldCount = 0;
                        
                        for (TableField field : fields) {
                            String fieldName = field.getName().toLowerCase();
                            if (fieldName.equals("created_at") || 
                                fieldName.equals("updated_at") || 
                                fieldName.equals("created_by") || 
                                fieldName.equals("updated_by") || 
                                fieldName.contains("_time") || 
                                fieldName.contains("_date") || 
                                fieldName.contains("_by")) {
                                changeLogFieldCount++;
                            }
                        }
                        
                        // 如果一个表有多个变更日志相关的字段，认为它支持变更日志
                        if (changeLogFieldCount >= 4) {
                            hasChangeLogFields = true;
                            break;
                        }
                    }
                }
                
                if (!hasChangeLogTable && !hasChangeLogFields) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("模型缺少变更日志机制");
                    String suggestion = "请添加变更日志表或在表中添加变更日志相关的字段";
                    issues.add(createSystemIssue("缺少变更日志", details, "中", suggestion));
                }
            }
            
            // 检查表复杂度
            if (maxTableComplexity != null) {
                List<String> complexTables = new ArrayList<>();
                
                for (com.mango.test.database.entity.Table table : tables) {
                    // 计算表的复杂度（字段数 + 索引数 + 约束数）
                    List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
                    List<Map<String, Object>> indexes = getIndexesByTableId(table.getId());
                    List<Map<String, Object>> constraints = getConstraintsByTableId(table.getId());
                    
                    int complexity = fields.size() + 
                                    (indexes != null ? indexes.size() : 0) + 
                                    (constraints != null ? constraints.size() : 0);
                    
                    if (complexity > maxTableComplexity) {
                        complexTables.add(table.getName() + " (复杂度: " + complexity + ")");
                    }
                }
                
                if (!complexTables.isEmpty()) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("以下表的复杂度超过最大限制 (" + maxTableComplexity + "):");
                    details.addAll(complexTables);
                    String suggestion = "请考虑拆分复杂的表，或减少字段、索引或约束的数量";
                    issues.add(createSystemIssue("表复杂度过高", details, "中", suggestion));
                }
            }
            
            return passed;
        } catch (Exception e) {
            log.error("验证可维护性规范失败", e);
            issues.add(createSystemIssue("验证可维护性规范时发生错误: " + e.getMessage()));
            return false;
        }
    }

    private boolean validateReliability(List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        log.info("验证可靠性规范: tablesCount={}", tables.size());
        boolean passed = true;
        
        try {
            // 获取规则参数
            Boolean requireBackupStrategy = Boolean.TRUE.equals(rules.get("requireBackupStrategy"));
            Boolean requireRecoveryPlan = Boolean.TRUE.equals(rules.get("requireRecoveryPlan"));
            Boolean requireErrorHandling = Boolean.TRUE.equals(rules.get("requireErrorHandling"));
            Integer maxDowntimeMinutes = (Integer) rules.getOrDefault("maxDowntimeMinutes", null);
            Double minAvailabilityPercentage = (Double) rules.getOrDefault("minAvailabilityPercentage", null);
            Boolean requireRedundancy = Boolean.TRUE.equals(rules.get("requireRedundancy"));
            
            // 检查备份策略
            if (requireBackupStrategy) {
                // 检查表注释中是否包含备份策略信息
                boolean hasBackupInfo = false;
                
                for (com.mango.test.database.entity.Table table : tables) {
                    String tableComment = table.getComment();
                    
                    if (tableComment != null && !tableComment.isEmpty()) {
                        // 检查注释中是否包含备份相关的关键词
                        if (tableComment.toLowerCase().contains("backup") || 
                            tableComment.toLowerCase().contains("备份")) {
                            hasBackupInfo = true;
                            break;
                        }
                    }
                }
                
                if (!hasBackupInfo) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("模型应定义备份策略");
                    String suggestion = "请在表注释中添加备份策略信息";
                    issues.add(createSystemIssue("缺少备份策略", details, "中", suggestion));
                }
            }
            
            // 检查恢复计划
            if (requireRecoveryPlan) {
                // 检查表注释中是否包含恢复计划信息
                boolean hasRecoveryInfo = false;
                
                for (com.mango.test.database.entity.Table table : tables) {
                    String tableComment = table.getComment();
                    
                    if (tableComment != null && !tableComment.isEmpty()) {
                        // 检查注释中是否包含恢复相关的关键词
                        if (tableComment.toLowerCase().contains("recovery") || 
                            tableComment.toLowerCase().contains("restore") || 
                            tableComment.toLowerCase().contains("恢复")) {
                            hasRecoveryInfo = true;
                            break;
                        }
                    }
                }
                
                if (!hasRecoveryInfo) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("模型应定义恢复计划");
                    String suggestion = "请在表注释中添加恢复计划信息";
                    issues.add(createSystemIssue("缺少恢复计划", details, "中", suggestion));
                }
            }
            
            // 检查错误处理
            if (requireErrorHandling) {
                // 检查是否有错误日志表或错误处理相关的字段
                boolean hasErrorHandling = false;
                
                for (com.mango.test.database.entity.Table table : tables) {
                    // 检查表名是否包含错误处理相关信息
                    if (table.getName().toLowerCase().contains("error") || 
                        table.getName().toLowerCase().contains("log") || 
                        table.getName().toLowerCase().contains("exception")) {
                        hasErrorHandling = true;
                        break;
                    }
                    
                    // 检查字段是否包含错误处理相关信息
                    List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
                    for (TableField field : fields) {
                        if (field.getName().toLowerCase().contains("error") || 
                            field.getName().toLowerCase().contains("status") || 
                            field.getName().toLowerCase().contains("exception")) {
                            hasErrorHandling = true;
                            break;
                        }
                    }
                    
                    if (hasErrorHandling) {
                        break;
                    }
                }
                
                if (!hasErrorHandling) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("模型应包含错误处理机制");
                    String suggestion = "请添加错误日志表或错误处理相关的字段";
                    issues.add(createSystemIssue("缺少错误处理机制", details, "中", suggestion));
                }
            }
            
            // 检查最大停机时间
            if (maxDowntimeMinutes != null) {
                // 这里只能检查模型设计是否支持高可用性
                // 实际停机时间需要在运行时监控
                
                // 检查是否有冗余设计或高可用性相关的表结构
                boolean hasHighAvailability = false;
                
                for (com.mango.test.database.entity.Table table : tables) {
                    String tableComment = table.getComment();
                    
                    if (tableComment != null && !tableComment.isEmpty()) {
                        // 检查注释中是否包含高可用性相关的关键词
                        if (tableComment.toLowerCase().contains("high availability") || 
                            tableComment.toLowerCase().contains("ha") || 
                            tableComment.toLowerCase().contains("failover") || 
                            tableComment.toLowerCase().contains("高可用")) {
                            hasHighAvailability = true;
                            break;
                        }
                    }
                }
                
                if (!hasHighAvailability && maxDowntimeMinutes < 60) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("模型要求最大停机时间为 " + maxDowntimeMinutes + " 分钟，但未找到高可用性设计");
                    String suggestion = "请考虑添加高可用性设计，以满足最大停机时间要求";
                    issues.add(createSystemIssue("缺少高可用性设计", details, "中", suggestion));
                }
            }
            
            // 检查最小可用性百分比
            if (minAvailabilityPercentage != null) {
                // 这里只能检查模型设计是否支持高可用性
                // 实际可用性百分比需要在运行时监控
                
                // 检查是否有高可用性相关的设计
                boolean hasHighAvailability = false;
                
                for (com.mango.test.database.entity.Table table : tables) {
                    String tableComment = table.getComment();
                    
                    if (tableComment != null && !tableComment.isEmpty()) {
                        // 检查注释中是否包含高可用性相关的关键词
                        if (tableComment.toLowerCase().contains("high availability") || 
                            tableComment.toLowerCase().contains("ha") || 
                            tableComment.toLowerCase().contains("failover") || 
                            tableComment.toLowerCase().contains("高可用")) {
                            hasHighAvailability = true;
                            break;
                        }
                    }
                }
                
                if (!hasHighAvailability && minAvailabilityPercentage > 99.0) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("模型要求最小可用性为 " + minAvailabilityPercentage + "%，但未找到高可用性设计");
                    String suggestion = "请考虑添加高可用性设计，以满足最小可用性要求";
                    issues.add(createSystemIssue("缺少高可用性设计", details, "中", suggestion));
                }
            }
            
            // 检查冗余要求
            if (requireRedundancy) {
                // 检查是否有冗余设计
                boolean hasRedundancy = false;
                
                for (com.mango.test.database.entity.Table table : tables) {
                    String tableComment = table.getComment();
                    
                    if (tableComment != null && !tableComment.isEmpty()) {
                        // 检查注释中是否包含冗余相关的关键词
                        if (tableComment.toLowerCase().contains("redundancy") || 
                            tableComment.toLowerCase().contains("redundant") || 
                            tableComment.toLowerCase().contains("冗余")) {
                            hasRedundancy = true;
                            break;
                        }
                    }
                }
                
                if (!hasRedundancy) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("模型要求支持冗余设计，但未找到冗余设计信息");
                    String suggestion = "请考虑添加冗余设计，以提高系统可靠性";
                    issues.add(createSystemIssue("缺少冗余设计", details, "中", suggestion));
                }
            }
            
            return passed;
        } catch (Exception e) {
            log.error("验证可靠性规范失败", e);
            issues.add(createSystemIssue("验证可靠性规范时发生错误: " + e.getMessage()));
            return false;
        }
    }

    private boolean validateSecurityStandard(ModelEntity model, List<com.mango.test.database.entity.Table> tables, Map<String, Object> standard, List<Map<String, Object>> issues) {
        log.info("验证安全规范: modelId={}, tableCount={}", model.getId(), tables.size());
        boolean passed = true;
        
        try {
            // 获取安全规范规则
            Map<String, Object> rules = (Map<String, Object>) standard.getOrDefault("rules", new HashMap<>());
            if (rules == null || rules.isEmpty()) {
                log.warn("安全规范规则为空");
                return true;
            }
            
            // 验证数据加密
            passed = validateDataEncryption(tables, rules, issues) && passed;
            
            // 验证访问控制
            passed = validateAccessControl(tables, rules, issues) && passed;
            
            // 验证敏感数据处理
            passed = validateSensitiveData(tables, rules, issues) && passed;
            
            // 验证审计日志
            passed = validateAuditLogging(tables, rules, issues) && passed;
            
            return passed;
        } catch (Exception e) {
            log.error("验证安全规范失败", e);
            issues.add(createSystemIssue("验证安全规范时发生错误: " + e.getMessage()));
            return false;
        }
    }

    private boolean validateDataEncryption(List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        log.info("验证数据加密规范: tablesCount={}", tables.size());
        boolean passed = true;
        
        try {
            // 获取规则参数
            Boolean requireEncryption = Boolean.TRUE.equals(rules.get("requireEncryption"));
            List<String> encryptedFields = (List<String>) rules.getOrDefault("encryptedFields", new ArrayList<>());
            String encryptionMethod = (String) rules.getOrDefault("encryptionMethod", null);
            
            if (!requireEncryption) {
                return true; // 不需要加密，跳过验证
            }
            
            // 检查需要加密的字段
            if (!encryptedFields.isEmpty()) {
                for (com.mango.test.database.entity.Table table : tables) {
                    List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
                    
                    for (String encryptedField : encryptedFields) {
                        boolean fieldFound = false;
                        boolean isEncrypted = false;
                        
                        for (TableField field : fields) {
                            if (field.getName().toLowerCase().equals(encryptedField.toLowerCase())) {
                                fieldFound = true;
                                
                                // 检查字段注释或名称是否包含加密相关信息
                                String comment = field.getComment();
                                String fieldName = field.getName().toLowerCase();
                                
                                if ((comment != null && (
                                        comment.toLowerCase().contains("encrypt") || 
                                        comment.toLowerCase().contains("加密"))) || 
                                    fieldName.contains("_encrypted") || 
                                    fieldName.contains("_enc")) {
                                    isEncrypted = true;
                                }
                                
                                break;
                            }
                        }
                        
                        if (fieldFound && !isEncrypted) {
                            passed = false;
                            List<String> details = new ArrayList<>();
                            details.add("表 " + table.getName() + " 中的字段 " + encryptedField + " 需要加密");
                            if (encryptionMethod != null) {
                                details.add("推荐的加密方法: " + encryptionMethod);
                            }
                            String suggestion = "请确保敏感字段使用适当的加密方法，并在字段注释中说明";
                            issues.add(createTableIssue(table, "敏感字段未加密", details, "高", suggestion));
                        }
                    }
                }
            }
            
            return passed;
        } catch (Exception e) {
            log.error("验证数据加密规范失败", e);
            issues.add(createSystemIssue("验证数据加密规范时发生错误: " + e.getMessage()));
            return false;
        }
    }

    private boolean validateAccessControl(List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        log.info("验证访问控制规范: tablesCount={}", tables.size());
        boolean passed = true;
        
        try {
            // 获取规则参数
            Boolean requireAccessControl = Boolean.TRUE.equals(rules.get("requireAccessControl"));
            Boolean requireRoleBasedAccess = Boolean.TRUE.equals(rules.get("requireRoleBasedAccess"));
            Boolean requireRowLevelSecurity = Boolean.TRUE.equals(rules.get("requireRowLevelSecurity"));
            
            if (!requireAccessControl) {
                return true; // 不需要访问控制，跳过验证
            }
            
            // 检查是否有访问控制相关的表
            boolean hasAccessControlTable = false;
            boolean hasRoleTable = false;
            boolean hasPermissionTable = false;
            
            for (com.mango.test.database.entity.Table table : tables) {
                String tableName = table.getName().toLowerCase();
                
                if (tableName.contains("user") || 
                    tableName.contains("account") || 
                    tableName.contains("auth")) {
                    hasAccessControlTable = true;
                }
                
                if (tableName.contains("role") || 
                    tableName.contains("group")) {
                    hasRoleTable = true;
                }
                
                if (tableName.contains("permission") || 
                    tableName.contains("privilege") || 
                    tableName.contains("acl") || 
                    tableName.contains("access")) {
                    hasPermissionTable = true;
                }
            }
            
            if (requireAccessControl && !hasAccessControlTable) {
                passed = false;
                List<String> details = new ArrayList<>();
                details.add("模型缺少访问控制相关的表");
                String suggestion = "请添加用户/账户表和访问控制相关的表";
                issues.add(createSystemIssue("缺少访问控制", details, "高", suggestion));
            }
            
            if (requireRoleBasedAccess && (!hasRoleTable || !hasPermissionTable)) {
                passed = false;
                List<String> details = new ArrayList<>();
                details.add("模型缺少基于角色的访问控制相关的表");
                if (!hasRoleTable) {
                    details.add("缺少角色/组表");
                }
                if (!hasPermissionTable) {
                    details.add("缺少权限/特权表");
                }
                String suggestion = "请添加角色表和权限表，以支持基于角色的访问控制";
                issues.add(createSystemIssue("缺少基于角色的访问控制", details, "中", suggestion));
            }
            
            if (requireRowLevelSecurity) {
                boolean hasRowLevelSecurity = false;
                
                for (com.mango.test.database.entity.Table table : tables) {
                    // 检查表注释是否包含行级安全相关信息
                    String tableComment = table.getComment();
                    if (tableComment != null && (
                            tableComment.toLowerCase().contains("row level security") || 
                            tableComment.toLowerCase().contains("row-level security") || 
                            tableComment.toLowerCase().contains("rls") || 
                            tableComment.toLowerCase().contains("行级安全"))) {
                        hasRowLevelSecurity = true;
                        break;
                    }
                    
                    // 检查是否有行级安全相关的字段
                    List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
                    for (TableField field : fields) {
                        String fieldName = field.getName().toLowerCase();
                        if (fieldName.contains("tenant_id") || 
                            fieldName.contains("org_id") || 
                            fieldName.contains("owner_id") || 
                            fieldName.contains("created_by")) {
                            hasRowLevelSecurity = true;
                            break;
                        }
                    }
                    
                    if (hasRowLevelSecurity) {
                        break;
                    }
                }
                
                if (!hasRowLevelSecurity) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("模型缺少行级安全机制");
                    String suggestion = "请添加行级安全相关的字段，如 tenant_id、org_id、owner_id 等";
                    issues.add(createSystemIssue("缺少行级安全", details, "中", suggestion));
                }
            }
            
            return passed;
        } catch (Exception e) {
            log.error("验证访问控制规范失败", e);
            issues.add(createSystemIssue("验证访问控制规范时发生错误: " + e.getMessage()));
            return false;
        }
    }

    private boolean validateSensitiveData(List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        log.info("验证敏感数据处理规范: tablesCount={}", tables.size());
        boolean passed = true;
        
        try {
            // 获取规则参数
            List<String> sensitiveFields = (List<String>) rules.getOrDefault("sensitiveFields", new ArrayList<>());
            Boolean requireMasking = Boolean.TRUE.equals(rules.get("requireMasking"));
            Boolean requireAnonymization = Boolean.TRUE.equals(rules.get("requireAnonymization"));
            
            // 检查敏感字段
            if (!sensitiveFields.isEmpty()) {
                for (com.mango.test.database.entity.Table table : tables) {
                    List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
                    
                    for (TableField field : fields) {
                        String fieldName = field.getName().toLowerCase();
                        
                        // 检查字段是否是敏感字段
                        boolean isSensitive = sensitiveFields.stream()
                                .anyMatch(sf -> fieldName.equals(sf.toLowerCase()) || 
                                               fieldName.contains(sf.toLowerCase()));
                        
                        if (isSensitive) {
                            // 检查字段注释是否包含敏感数据处理相关信息
                            String comment = field.getComment();
                            boolean hasSensitiveInfo = false;
                            
                            if (comment != null && (
                                    comment.toLowerCase().contains("sensitive") || 
                                    comment.toLowerCase().contains("mask") || 
                                    comment.toLowerCase().contains("anonymize") || 
                                    comment.toLowerCase().contains("敏感") || 
                                    comment.toLowerCase().contains("脱敏") || 
                                    comment.toLowerCase().contains("匿名"))) {
                                hasSensitiveInfo = true;
                            }
                            
                            if (!hasSensitiveInfo) {
                                passed = false;
                                List<String> details = new ArrayList<>();
                                details.add("表 " + table.getName() + " 中的敏感字段 " + field.getName() + " 缺少敏感数据处理说明");
                                String suggestion = "请在字段注释中添加敏感数据处理相关信息";
                                issues.add(createColumnIssue(field, table, "敏感字段缺少处理说明", details, "中", suggestion));
                            }
                        }
                    }
                }
            }
            
            // 检查数据脱敏要求
            if (requireMasking) {
                boolean hasMaskingInfo = false;
                
                for (com.mango.test.database.entity.Table table : tables) {
                    // 检查表注释是否包含数据脱敏相关信息
                    String tableComment = table.getComment();
                    if (tableComment != null && (
                            tableComment.toLowerCase().contains("mask") || 
                            tableComment.toLowerCase().contains("脱敏"))) {
                        hasMaskingInfo = true;
                        break;
                    }
                    
                    // 检查字段注释是否包含数据脱敏相关信息
                    List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
                    for (TableField field : fields) {
                        String comment = field.getComment();
                        if (comment != null && (
                                comment.toLowerCase().contains("mask") || 
                                comment.toLowerCase().contains("脱敏"))) {
                            hasMaskingInfo = true;
                            break;
                        }
                    }
                    
                    if (hasMaskingInfo) {
                        break;
                    }
                }
                
                if (!hasMaskingInfo) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("模型缺少数据脱敏机制");
                    String suggestion = "请在表或字段注释中添加数据脱敏相关信息";
                    issues.add(createSystemIssue("缺少数据脱敏", details, "中", suggestion));
                }
            }
            
            // 检查数据匿名化要求
            if (requireAnonymization) {
                boolean hasAnonymizationInfo = false;
                
                for (com.mango.test.database.entity.Table table : tables) {
                    // 检查表注释是否包含数据匿名化相关信息
                    String tableComment = table.getComment();
                    if (tableComment != null && (
                            tableComment.toLowerCase().contains("anonymize") || 
                            tableComment.toLowerCase().contains("anonymization") || 
                            tableComment.toLowerCase().contains("匿名"))) {
                        hasAnonymizationInfo = true;
                        break;
                    }
                    
                    // 检查字段注释是否包含数据匿名化相关信息
                    List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
                    for (TableField field : fields) {
                        String comment = field.getComment();
                        if (comment != null && (
                                comment.toLowerCase().contains("anonymize") || 
                                comment.toLowerCase().contains("anonymization") || 
                                comment.toLowerCase().contains("匿名"))) {
                            hasAnonymizationInfo = true;
                            break;
                        }
                    }
                    
                    if (hasAnonymizationInfo) {
                        break;
                    }
                }
                
                if (!hasAnonymizationInfo) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("模型缺少数据匿名化机制");
                    String suggestion = "请在表或字段注释中添加数据匿名化相关信息";
                    issues.add(createSystemIssue("缺少数据匿名化", details, "中", suggestion));
                }
            }
            
            return passed;
        } catch (Exception e) {
            log.error("验证敏感数据处理规范失败", e);
            issues.add(createSystemIssue("验证敏感数据处理规范时发生错误: " + e.getMessage()));
            return false;
        }
    }

    private boolean validateAuditLogging(List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        log.info("验证审计日志规范: tablesCount={}", tables.size());
        boolean passed = true;
        
        try {
            // 获取规则参数
            Boolean requireAuditLog = Boolean.TRUE.equals(rules.get("requireAuditLog"));
            List<String> auditActions = (List<String>) rules.getOrDefault("auditActions", Arrays.asList("INSERT", "UPDATE", "DELETE"));
            
            if (!requireAuditLog) {
                return true; // 不需要审计日志，跳过验证
            }
            
            // 检查是否有审计日志表
            boolean hasAuditLogTable = false;
            
            for (com.mango.test.database.entity.Table table : tables) {
                String tableName = table.getName().toLowerCase();
                
                if (tableName.contains("audit") || 
                    tableName.contains("log") || 
                    tableName.contains("history") || 
                    tableName.endsWith("_aud")) {
                    hasAuditLogTable = true;
                    break;
                }
            }
            
            if (!hasAuditLogTable) {
                passed = false;
                List<String> details = new ArrayList<>();
                details.add("模型缺少审计日志表");
                if (!auditActions.isEmpty()) {
                    details.add("需要审计的操作: " + String.join(", ", auditActions));
                }
                String suggestion = "请添加审计日志表，用于记录数据变更";
                issues.add(createSystemIssue("缺少审计日志表", details, "高", suggestion));
            }
            
            return passed;
        } catch (Exception e) {
            log.error("验证审计日志规范失败", e);
            issues.add(createSystemIssue("验证审计日志规范时发生错误: " + e.getMessage()));
            return false;
        }
    }

    private boolean validateAvailability(List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        log.info("验证可用性规范: tablesCount={}", tables.size());
        boolean passed = true;
        
        try {
            // 获取规则参数
            Double minAvailabilityPercentage = (Double) rules.getOrDefault("minAvailabilityPercentage", null);
            Boolean requireLoadBalancing = Boolean.TRUE.equals(rules.get("requireLoadBalancing"));
            Boolean requireFailover = Boolean.TRUE.equals(rules.get("requireFailover"));
            Boolean requireScalability = Boolean.TRUE.equals(rules.get("requireScalability"));
            Integer maxResponseTimeMs = (Integer) rules.getOrDefault("maxResponseTimeMs", null);
            
            // 检查最小可用性百分比
            if (minAvailabilityPercentage != null) {
                // 这里只能检查模型设计是否支持高可用性
                // 实际可用性百分比需要在运行时监控
                
                // 检查是否有高可用性相关的设计
                boolean hasHighAvailability = false;
                
                for (com.mango.test.database.entity.Table table : tables) {
                    String tableComment = table.getComment();
                    
                    if (tableComment != null && !tableComment.isEmpty()) {
                        // 检查注释中是否包含高可用性相关的关键词
                        if (tableComment.toLowerCase().contains("high availability") || 
                            tableComment.toLowerCase().contains("ha") || 
                            tableComment.toLowerCase().contains("failover") || 
                            tableComment.toLowerCase().contains("高可用")) {
                            hasHighAvailability = true;
                            break;
                        }
                    }
                }
                
                if (!hasHighAvailability && minAvailabilityPercentage > 99.0) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("模型要求最小可用性为 " + minAvailabilityPercentage + "%，但未找到高可用性设计");
                    String suggestion = "请考虑添加高可用性设计，以满足最小可用性要求";
                    issues.add(createSystemIssue("缺少高可用性设计", details, "中", suggestion));
                }
            }
            
            // 检查负载均衡
            if (requireLoadBalancing) {
                // 检查是否有负载均衡相关的设计
                boolean hasLoadBalancing = false;
                
                for (com.mango.test.database.entity.Table table : tables) {
                    String tableComment = table.getComment();
                    
                    if (tableComment != null && !tableComment.isEmpty()) {
                        // 检查注释中是否包含负载均衡相关的关键词
                        if (tableComment.toLowerCase().contains("load balancing") || 
                            tableComment.toLowerCase().contains("loadbalancing") || 
                            tableComment.toLowerCase().contains("负载均衡")) {
                            hasLoadBalancing = true;
                            break;
                        }
                    }
                }
                
                if (!hasLoadBalancing) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("模型要求支持负载均衡，但未找到负载均衡设计");
                    String suggestion = "请考虑添加负载均衡设计";
                    issues.add(createSystemIssue("缺少负载均衡设计", details, "低", suggestion));
                }
            }
            
            // 检查故障转移
            if (requireFailover) {
                // 检查是否有故障转移相关的设计
                boolean hasFailover = false;
                
                for (com.mango.test.database.entity.Table table : tables) {
                    String tableComment = table.getComment();
                    
                    if (tableComment != null && !tableComment.isEmpty()) {
                        // 检查注释中是否包含故障转移相关的关键词
                        if (tableComment.toLowerCase().contains("failover") || 
                            tableComment.toLowerCase().contains("fail over") || 
                            tableComment.toLowerCase().contains("故障转移")) {
                            hasFailover = true;
                            break;
                        }
                    }
                }
                
                if (!hasFailover) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("模型要求支持故障转移，但未找到故障转移设计");
                    String suggestion = "请考虑添加故障转移设计";
                    issues.add(createSystemIssue("缺少故障转移设计", details, "中", suggestion));
                }
            }
            
            // 检查可扩展性
            if (requireScalability) {
                // 检查是否有可扩展性相关的设计
                boolean hasScalability = false;
                
                for (com.mango.test.database.entity.Table table : tables) {
                    String tableComment = table.getComment();
                    
                    if (tableComment != null && !tableComment.isEmpty()) {
                        // 检查注释中是否包含可扩展性相关的关键词
                        if (tableComment.toLowerCase().contains("scalability") || 
                            tableComment.toLowerCase().contains("scalable") || 
                            tableComment.toLowerCase().contains("scale out") || 
                            tableComment.toLowerCase().contains("scale up") || 
                            tableComment.toLowerCase().contains("可扩展")) {
                            hasScalability = true;
                            break;
                        }
                    }
                }
                
                // 检查表结构是否支持分区或分片
                if (!hasScalability) {
                    for (com.mango.test.database.entity.Table table : tables) {
                        List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
                        for (TableField field : fields) {
                            String fieldName = field.getName().toLowerCase();
                            String fieldComment = field.getComment();
                            
                            if (fieldName.contains("shard") || 
                                fieldName.contains("partition") || 
                                (fieldComment != null && (
                                    fieldComment.toLowerCase().contains("shard") || 
                                    fieldComment.toLowerCase().contains("partition") || 
                                    fieldComment.toLowerCase().contains("分片") || 
                                    fieldComment.toLowerCase().contains("分区")))) {
                                hasScalability = true;
                                break;
                            }
                        }
                        
                        if (hasScalability) {
                            break;
                        }
                    }
                }
                
                if (!hasScalability) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("模型要求支持可扩展性，但未找到可扩展性设计");
                    String suggestion = "请考虑添加可扩展性设计，如分区、分片等";
                    issues.add(createSystemIssue("缺少可扩展性设计", details, "中", suggestion));
                }
            }
            
            // 检查最大响应时间
            if (maxResponseTimeMs != null) {
                // 这里只能检查模型设计是否支持快速响应
                // 实际响应时间需要在运行时监控
                
                // 检查是否有索引优化
                boolean hasIndexOptimization = true;
                List<String> tablesWithoutIndex = new ArrayList<>();
                
                for (com.mango.test.database.entity.Table table : tables) {
                    List<Map<String, Object>> indexes = getIndexesByTableId(table.getId());
                    if (indexes == null || indexes.isEmpty()) {
                        hasIndexOptimization = false;
                        tablesWithoutIndex.add(table.getName());
                    }
                }
                
                if (!hasIndexOptimization && maxResponseTimeMs < 1000) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("模型要求最大响应时间为 " + maxResponseTimeMs + " 毫秒，但以下表缺少索引优化:");
                    details.addAll(tablesWithoutIndex);
                    String suggestion = "请为表添加适当的索引，以提高查询性能";
                    issues.add(createSystemIssue("缺少索引优化", details, "中", suggestion));
                }
            }
            
            return passed;
        } catch (Exception e) {
            log.error("验证可用性规范失败", e);
            issues.add(createSystemIssue("验证可用性规范时发生错误: " + e.getMessage()));
            return false;
        }
    }

    private boolean validatePerformance(List<com.mango.test.database.entity.Table> tables, Map<String, Object> rules, List<Map<String, Object>> issues) {
        log.info("验证性能规范: tablesCount={}", tables.size());
        boolean passed = true;
        
        try {
            // 获取规则参数
            Integer maxResponseTimeMs = (Integer) rules.getOrDefault("maxResponseTimeMs", null);
            Integer maxThroughput = (Integer) rules.getOrDefault("maxThroughput", null);
            Boolean requireIndexOptimization = Boolean.TRUE.equals(rules.get("requireIndexOptimization"));
            Boolean requireQueryOptimization = Boolean.TRUE.equals(rules.get("requireQueryOptimization"));
            Integer maxTableSize = (Integer) rules.getOrDefault("maxTableSize", null);
            Integer maxRowCount = (Integer) rules.getOrDefault("maxRowCount", null);
            Boolean requireCaching = Boolean.TRUE.equals(rules.get("requireCaching"));
            Boolean requirePagination = Boolean.TRUE.equals(rules.get("requirePagination"));
            
            // 检查索引优化
            if (requireIndexOptimization) {
                boolean hasIndexOptimization = true;
                List<String> tablesWithoutIndex = new ArrayList<>();
                
                for (com.mango.test.database.entity.Table table : tables) {
                    List<Map<String, Object>> indexes = getIndexesByTableId(table.getId());
                    if (indexes == null || indexes.isEmpty()) {
                        hasIndexOptimization = false;
                        tablesWithoutIndex.add(table.getName());
                    }
                }
                
                if (!hasIndexOptimization) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("以下表缺少索引优化:");
                    details.addAll(tablesWithoutIndex);
                    String suggestion = "请为表添加适当的索引，以提高查询性能";
                    issues.add(createSystemIssue("缺少索引优化", details, "中", suggestion));
                }
            }
            
            // 检查查询优化
            if (requireQueryOptimization) {
                // 检查是否有查询优化相关的设计
                boolean hasQueryOptimization = false;
                
                for (com.mango.test.database.entity.Table table : tables) {
                    String tableComment = table.getComment();
                    
                    if (tableComment != null && !tableComment.isEmpty()) {
                        // 检查注释中是否包含查询优化相关的关键词
                        if (tableComment.toLowerCase().contains("query optimization") || 
                            tableComment.toLowerCase().contains("performance") || 
                            tableComment.toLowerCase().contains("查询优化") || 
                            tableComment.toLowerCase().contains("性能优化")) {
                            hasQueryOptimization = true;
                            break;
                        }
                    }
                }
                
                if (!hasQueryOptimization) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("模型缺少查询优化设计");
                    String suggestion = "请考虑添加查询优化设计，如索引、分区、物化视图等";
                    issues.add(createSystemIssue("缺少查询优化", details, "中", suggestion));
                }
            }
            
            // 检查表大小限制
            if (maxTableSize != null) {
                // 估算表大小
                Map<String, Long> tableSizes = new HashMap<>();
                List<String> oversizedTables = new ArrayList<>();
                
                for (com.mango.test.database.entity.Table table : tables) {
                    List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
                    long rowSize = 0;
                    
                    for (TableField field : fields) {
                        rowSize += estimateFieldSize(field);
                    }
                    
                    // 假设每个表有1000行数据作为基准
                    long tableSize = rowSize * 1000;
                    tableSizes.put(table.getName(), tableSize);
                    
                    // 检查是否超过限制
                    if (tableSize > convertToBytes(maxTableSize, "MB")) {
                        oversizedTables.add(table.getName() + " (估计大小: " + formatSize(tableSize) + ")");
                    }
                }
                
                if (!oversizedTables.isEmpty()) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("以下表的估计大小超过限制 (" + maxTableSize + " MB):");
                    details.addAll(oversizedTables);
                    String suggestion = "请考虑拆分大表或优化表结构";
                    issues.add(createSystemIssue("表大小超过限制", details, "中", suggestion));
                }
            }
            
            // 检查行数限制
            if (maxRowCount != null) {
                // 这里只能提供设计建议，实际行数需要在运行时监控
                
                // 检查是否有分区或分表设计
                boolean hasPartitioning = false;
                List<String> tablesWithoutPartitioning = new ArrayList<>();
                
                for (com.mango.test.database.entity.Table table : tables) {
                    String tableComment = table.getComment();
                    boolean tableHasPartitioning = false;
                    
                    if (tableComment != null && !tableComment.isEmpty()) {
                        // 检查注释中是否包含分区相关的关键词
                        if (tableComment.toLowerCase().contains("partition") || 
                            tableComment.toLowerCase().contains("sharding") || 
                            tableComment.toLowerCase().contains("分区") || 
                            tableComment.toLowerCase().contains("分表")) {
                            tableHasPartitioning = true;
                        }
                    }
                    
                    if (!tableHasPartitioning) {
                        tablesWithoutPartitioning.add(table.getName());
                    }
                }
                
                if (!tablesWithoutPartitioning.isEmpty()) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("模型要求最大行数为 " + maxRowCount + "，但以下表缺少分区或分表设计:");
                    details.addAll(tablesWithoutPartitioning);
                    String suggestion = "请考虑添加分区或分表设计，以支持大量数据";
                    issues.add(createSystemIssue("缺少分区或分表设计", details, "中", suggestion));
                }
            }
            
            // 检查缓存要求
            if (requireCaching) {
                // 检查是否有缓存相关的设计
                boolean hasCaching = false;
                
                for (com.mango.test.database.entity.Table table : tables) {
                    String tableComment = table.getComment();
                    
                    if (tableComment != null && !tableComment.isEmpty()) {
                        // 检查注释中是否包含缓存相关的关键词
                        if (tableComment.toLowerCase().contains("cache") || 
                            tableComment.toLowerCase().contains("caching") || 
                            tableComment.toLowerCase().contains("缓存")) {
                            hasCaching = true;
                            break;
                        }
                    }
                }
                
                if (!hasCaching) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("模型要求支持缓存，但未找到缓存设计");
                    String suggestion = "请考虑添加缓存设计，以提高查询性能";
                    issues.add(createSystemIssue("缺少缓存设计", details, "低", suggestion));
                }
            }
            
            // 检查分页要求
            if (requirePagination) {
                // 检查是否有分页相关的设计
                boolean hasPagination = false;
                
                for (com.mango.test.database.entity.Table table : tables) {
                    String tableComment = table.getComment();
                    
                    if (tableComment != null && !tableComment.isEmpty()) {
                        // 检查注释中是否包含分页相关的关键词
                        if (tableComment.toLowerCase().contains("pagination") || 
                            tableComment.toLowerCase().contains("paging") || 
                            tableComment.toLowerCase().contains("分页")) {
                            hasPagination = true;
                            break;
                        }
                    }
                    
                    // 检查是否有分页相关的字段
                    if (!hasPagination) {
                        List<TableField> fields = tableFieldService.getFieldsByTableId(table.getId());
                        for (TableField field : fields) {
                            String fieldName = field.getName().toLowerCase();
                            String fieldComment = field.getComment();
                            
                            if (fieldName.contains("page") || 
                                fieldName.contains("limit") || 
                                fieldName.contains("offset") || 
                                (fieldComment != null && (
                                    fieldComment.toLowerCase().contains("pagination") || 
                                    fieldComment.toLowerCase().contains("paging") || 
                                    fieldComment.toLowerCase().contains("分页")))) {
                                hasPagination = true;
                                break;
                            }
                        }
                    }
                    
                    if (hasPagination) {
                        break;
                    }
                }
                
                if (!hasPagination) {
                    passed = false;
                    List<String> details = new ArrayList<>();
                    details.add("模型要求支持分页，但未找到分页设计");
                    String suggestion = "请考虑添加分页设计，以支持大量数据的高效查询";
                    issues.add(createSystemIssue("缺少分页设计", details, "低", suggestion));
                }
            }
            
            return passed;
        } catch (Exception e) {
            log.error("验证性能规范失败", e);
            issues.add(createSystemIssue("验证性能规范时发生错误: " + e.getMessage()));
            return false;
        }
    }
} 