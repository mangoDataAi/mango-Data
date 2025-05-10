package com.mango.test.database.service.impl;

import com.mango.test.database.entity.DataSource;
import com.mango.test.database.mapper.DataSourceMapper;
import com.mango.test.database.service.NoSQLService;
import com.mango.test.database.service.impl.datasource.AbstractNoSQLHandler;
import com.mango.test.database.service.impl.datasource.DatabaseHandlerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NoSQL数据库服务实现类
 */
@Slf4j
@Service
public class NoSQLServiceImpl implements NoSQLService {

    @Autowired
    private DataSourceMapper dataSourceMapper;

    /**
     * 获取NoSQL数据库处理器
     */
    private AbstractNoSQLHandler getNoSQLHandler(String dataSourceId) throws Exception {
        DataSource dataSource = dataSourceMapper.selectById(dataSourceId);
        if (dataSource == null) {
            throw new Exception("数据源不存在: " + dataSourceId);
        }
        return DatabaseHandlerFactory.getNoSQLHandler(dataSource);
    }

    @Override
    public List<Map<String, Object>> getCollections(String dataSourceId, String searchText) throws Exception {
        AbstractNoSQLHandler handler = getNoSQLHandler(dataSourceId);
        try {
            return handler.getCollections(searchText);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public Map<String, Object> getCollectionInfo(String dataSourceId, String collectionName) throws Exception {
        AbstractNoSQLHandler handler = getNoSQLHandler(dataSourceId);
        try {
            return handler.getCollectionInfo(collectionName);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public Map<String, Object> getCollectionSchema(String dataSourceId, String collectionName) throws Exception {
        AbstractNoSQLHandler handler = getNoSQLHandler(dataSourceId);
        try {
            Map<String, Object> schema = new HashMap<>();
            
            // 获取数据源类型
            DataSource dataSource = dataSourceMapper.selectById(dataSourceId);
            String dbType = dataSource.getType();
            
            // 获取集合的字段信息
            List<Map<String, Object>> fields = handler.getFields(collectionName);
            schema.put("fields", fields);
            
            // 获取集合的索引信息
            List<Map<String, Object>> indexes = handler.getIndexes(collectionName);
            schema.put("indexes", indexes);
            
            // 获取集合的统计信息
            Map<String, Object> stats = handler.getCollectionStats(collectionName);
            schema.put("stats", stats);
            
            // 获取集合基本信息
            Map<String, Object> info = handler.getCollectionInfo(collectionName);
            schema.put("info", info);
            
            // 特殊处理Redis数据结构
            if ("redis".equalsIgnoreCase(dbType)) {
                log.info("构建Redis数据结构Schema: {}", collectionName);
                
                // 添加数据类型和结构信息
                if (info.containsKey("type")) {
                    String type = info.get("type").toString();
                    schema.put("dataType", type);
                    
                    // 类型显示名称
                    String typeDisplay;
                    switch (type) {
                        case "string": typeDisplay = "字符串"; break;
                        case "list": typeDisplay = "列表"; break;
                        case "set": typeDisplay = "集合"; break;
                        case "zset": typeDisplay = "有序集合"; break;
                        case "hash": typeDisplay = "哈希表"; break;
                        default: typeDisplay = type;
                    }
                    schema.put("typeDisplay", typeDisplay);
                    
                    // 添加特定数据类型的可查询字段
                    List<Map<String, Object>> queryableFields = new ArrayList<>();
                    
                    switch (type) {
                        case "string":
                            Map<String, Object> valueField = new HashMap<>();
                            valueField.put("name", "value");
                            valueField.put("type", "string");
                            valueField.put("description", "字符串值");
                            queryableFields.add(valueField);
                            break;
                        
                        case "hash":
                            // 对于哈希表，添加字段名和值作为可查询字段
                            Map<String, Object> fieldField = new HashMap<>();
                            fieldField.put("name", "field");
                            fieldField.put("type", "string");
                            fieldField.put("description", "哈希字段名");
                            queryableFields.add(fieldField);
                            
                            Map<String, Object> hashValueField = new HashMap<>();
                            hashValueField.put("name", "value");
                            hashValueField.put("type", "string");
                            hashValueField.put("description", "哈希字段值");
                            queryableFields.add(hashValueField);
                            
                            // 添加哈希中的所有字段
                            if (info.containsKey("fields") && info.get("fields") instanceof Map) {
                                Map<?, ?> hashFields = (Map<?, ?>) info.get("fields");
                                for (Object key : hashFields.keySet()) {
                                    Map<String, Object> specificField = new HashMap<>();
                                    specificField.put("name", "field:" + key.toString());
                                    specificField.put("type", "string");
                                    specificField.put("description", "字段 " + key.toString() + " 的值");
                                    queryableFields.add(specificField);
                                }
                            }
                            break;
                        
                        case "list":
                            // 对于列表，添加索引和值作为可查询字段
                            Map<String, Object> listValueField = new HashMap<>();
                            listValueField.put("name", "value");
                            listValueField.put("type", "string");
                            listValueField.put("description", "列表元素");
                            queryableFields.add(listValueField);
                            
                            Map<String, Object> indexField = new HashMap<>();
                            indexField.put("name", "index");
                            indexField.put("type", "number");
                            indexField.put("description", "列表索引");
                            queryableFields.add(indexField);
                            
                            // 添加start和end参数用于范围查询
                            Map<String, Object> startField = new HashMap<>();
                            startField.put("name", "start");
                            startField.put("type", "number");
                            startField.put("description", "列表起始索引");
                            queryableFields.add(startField);
                            
                            Map<String, Object> endField = new HashMap<>();
                            endField.put("name", "end");
                            endField.put("type", "number");
                            endField.put("description", "列表结束索引");
                            queryableFields.add(endField);
                            break;
                        
                        case "set":
                            // 对于集合，添加值作为可查询字段
                            Map<String, Object> setValueField = new HashMap<>();
                            setValueField.put("name", "value");
                            setValueField.put("type", "string");
                            setValueField.put("description", "集合成员");
                            queryableFields.add(setValueField);
                            break;
                        
                        case "zset":
                            // 对于有序集合，添加成员和分数作为可查询字段
                            Map<String, Object> memberField = new HashMap<>();
                            memberField.put("name", "value");
                            memberField.put("type", "string");
                            memberField.put("description", "有序集合成员");
                            queryableFields.add(memberField);
                            
                            Map<String, Object> scoreField = new HashMap<>();
                            scoreField.put("name", "score");
                            scoreField.put("type", "number");
                            scoreField.put("description", "成员分数");
                            queryableFields.add(scoreField);
                            
                            // 添加分数范围字段
                            Map<String, Object> minScoreField = new HashMap<>();
                            minScoreField.put("name", "minScore");
                            minScoreField.put("type", "number");
                            minScoreField.put("description", "最小分数");
                            queryableFields.add(minScoreField);
                            
                            Map<String, Object> maxScoreField = new HashMap<>();
                            maxScoreField.put("name", "maxScore");
                            maxScoreField.put("type", "number");
                            maxScoreField.put("description", "最大分数");
                            queryableFields.add(maxScoreField);
                            break;
                    }
                    
                    schema.put("queryableFields", queryableFields);
                    
                    // 添加查询示例
                    Map<String, Object> queryExamples = new HashMap<>();
                    switch (type) {
                        case "string":
                            queryExamples.put("查询字符串值", "{\"value\": \"示例内容\"}");
                            break;
                        case "hash":
                            queryExamples.put("查询指定字段", "{\"field\": \"name\"}");
                            queryExamples.put("查询字段值", "{\"value\": \"示例值\"}");
                            break;
                        case "list":
                            queryExamples.put("查询指定索引", "{\"index\": 0}");
                            queryExamples.put("查询索引范围", "{\"start\": 0, \"end\": 10}");
                            queryExamples.put("查询包含特定值", "{\"value\": \"示例内容\"}");
                            break;
                        case "set":
                            queryExamples.put("查询包含特定值", "{\"value\": \"示例成员\"}");
                            break;
                        case "zset":
                            queryExamples.put("查询特定成员", "{\"value\": \"示例成员\"}");
                            queryExamples.put("查询特定分数", "{\"score\": 100}");
                            queryExamples.put("查询分数范围", "{\"minScore\": 10, \"maxScore\": 100}");
                            break;
                    }
                    schema.put("queryExamples", queryExamples);
                }
                
                // 添加TTL信息
                if (info.containsKey("ttlInfo")) {
                    schema.put("ttlInfo", info.get("ttlInfo"));
                }
                
                // 添加结构信息
                if (info.containsKey("structure")) {
                    schema.put("structure", info.get("structure"));
                }
            }
            
            return schema;
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public Map<String, Object> queryCollection(String dataSourceId, String collectionName, Map<String, Object> request) throws Exception {
        AbstractNoSQLHandler handler = getNoSQLHandler(dataSourceId);
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 处理分页参数
            int limit = request.containsKey("limit") ? Integer.parseInt(request.get("limit").toString()) : 10;
            int skip = request.containsKey("skip") ? Integer.parseInt(request.get("skip").toString()) : 0;
            
            // 处理查询条件
            Map<String, Object> query = request.containsKey("query") ? (Map<String, Object>) request.get("query") : new HashMap<>();
            
            // 处理排序参数
            Map<String, Object> sort = request.containsKey("sort") ? (Map<String, Object>) request.get("sort") : new HashMap<>();
            
            // 执行查询
            List<Map<String, Object>> documents;
            if (request.containsKey("queryString")) {
                // 使用查询字符串执行查询
                documents = handler.query(collectionName, request.get("queryString").toString(), limit);
            } else {
                // 使用查询对象执行查询
                documents = handler.query(collectionName, query, limit);
            }
            
            // 获取查询计划（如果支持）
            if (request.containsKey("explain") && Boolean.parseBoolean(request.get("explain").toString())) {
                Map<String, Object> queryPlan = handler.getQueryPlan(collectionName, query);
                result.put("queryPlan", queryPlan);
            }
            
            // 获取准确的总记录数
            long total;
            if (request.containsKey("queryString")) {
                total = handler.count(collectionName, request.get("queryString").toString());
            } else {
                total = handler.count(collectionName, query);
            }
            
            // 设置结果
            result.put("data", documents);
            result.put("total", total);
            
            return result;
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public String insertDocument(String dataSourceId, String collectionName, Map<String, Object> document) throws Exception {
        AbstractNoSQLHandler handler = getNoSQLHandler(dataSourceId);
        try {
            boolean success = handler.insert(collectionName, document);
            if (!success) {
                throw new Exception("插入文档失败");
            }
            
            // 返回文档ID（假设文档中有_id字段）
            return document.containsKey("_id") ? document.get("_id").toString() : null;
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public boolean updateDocument(String dataSourceId, String collectionName, String documentId, Map<String, Object> document) throws Exception {
        AbstractNoSQLHandler handler = getNoSQLHandler(dataSourceId);
        try {
            // 构建查询条件
            Map<String, Object> query = new HashMap<>();
            query.put("_id", documentId);
            
            // 执行更新
            int updatedCount = handler.update(collectionName, query, document);
            return updatedCount > 0;
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public boolean deleteDocument(String dataSourceId, String collectionName, String documentId) throws Exception {
        AbstractNoSQLHandler handler = getNoSQLHandler(dataSourceId);
        try {
            // 构建查询条件
            Map<String, Object> query = new HashMap<>();
            query.put("_id", documentId);
            
            // 执行删除
            int deletedCount = handler.delete(collectionName, query);
            return deletedCount > 0;
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public boolean createCollection(String dataSourceId, Map<String, Object> request) throws Exception {
        AbstractNoSQLHandler handler = getNoSQLHandler(dataSourceId);
        try {
            // 从请求中获取集合名称
            if (request == null || !request.containsKey("name")) {
                log.error("创建集合失败：请求中缺少name参数");
                return false;
            }
            
            String collectionName = request.get("name").toString();
            log.info("创建集合请求: 数据源ID={}, 集合名称={}, 请求参数={}", dataSourceId, collectionName, request);
            
            // 特殊处理Redis数据类型
            DataSource dataSource = dataSourceMapper.selectById(dataSourceId);
            Map<String, Object> options = new HashMap<>();
            
            if ("redis".equalsIgnoreCase(dataSource.getType())) {
                log.info("检测到Redis数据源，开始特殊处理Redis参数");
                
                // 直接传入完整的请求参数作为options，包括type, value, values等
                options = new HashMap<>(request);
                
                // 确保Redis数据类型被正确传递
                if (request.containsKey("type")) {
                    String type = request.get("type").toString();
                    log.info("Redis数据类型: {}", type);
                    
                    // 处理不同数据类型的特殊参数
                    switch (type) {
                        case "string":
                            log.info("处理string类型，值: {}", request.get("value"));
                            break;
                            
                        case "list":
                            if (request.containsKey("values")) {
                                log.info("处理list类型，元素数量: {}", 
                                    ((List<?>)request.get("values")).size());
                            } else {
                                log.warn("list类型缺少values参数");
                            }
                            break;
                            
                        case "set":
                            if (request.containsKey("values")) {
                                log.info("处理set类型，元素数量: {}", 
                                    ((List<?>)request.get("values")).size());
                            } else {
                                log.warn("set类型缺少values参数");
                            }
                            break;
                            
                        case "zset":
                            if (request.containsKey("values")) {
                                List<?> zsetValues = (List<?>)request.get("values");
                                log.info("处理zset类型，元素数量: {}", zsetValues.size());
                                
                                // 转换zset值格式为scoreMembers映射
                                Map<String, Double> scoreMembers = new HashMap<>();
                                for (Object value : zsetValues) {
                                    if (value instanceof Map) {
                                        Map<?, ?> item = (Map<?, ?>)value;
                                        if (item.containsKey("member") && item.containsKey("score")) {
                                            String member = item.get("member").toString();
                                            Double score = Double.parseDouble(item.get("score").toString());
                                            scoreMembers.put(member, score);
                                        }
                                    }
                                }
                                
                                // 替换原始values为scoreMembers映射
                                options.put("scoreMembers", scoreMembers);
                                log.info("转换后的zset scoreMembers: {}", scoreMembers);
                            } else {
                                log.warn("zset类型缺少values参数");
                            }
                            break;
                            
                        case "hash":
                            if (request.containsKey("values") && request.get("values") instanceof Map) {
                                Map<?, ?> hashValues = (Map<?, ?>)request.get("values");
                                log.info("处理hash类型，字段数量: {}", hashValues.size());
                                
                                // 复制hashValues到options中
                                for (Map.Entry<?, ?> entry : hashValues.entrySet()) {
                                    options.put(entry.getKey().toString(), entry.getValue());
                                }
                            } else {
                                log.warn("hash类型values参数格式不正确");
                            }
                            break;
                            
                        default:
                            log.warn("未知的Redis数据类型: {}", type);
                    }
                } else {
                    log.warn("Redis请求中缺少type参数");
                }
            } else {
                // 非Redis数据库处理
                options = request.containsKey("options") ? 
                        (Map<String, Object>) request.get("options") : new HashMap<>();
            }
            
            log.info("最终创建集合的参数: collection={}, options={}", collectionName, options);
            
            // 创建集合
            boolean result = handler.createCollection(collectionName, options);
            log.info("创建集合结果: {}", result);
            return result;
        } catch (Exception e) {
            log.error("创建集合异常: {}", e.getMessage(), e);
            throw e;
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public boolean deleteCollection(String dataSourceId, String collectionName) throws Exception {
        AbstractNoSQLHandler handler = getNoSQLHandler(dataSourceId);
        try {
            return handler.dropCollection(collectionName);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public Object executeCommand(String dataSourceId, Map<String, Object> request) throws Exception {
        AbstractNoSQLHandler handler = getNoSQLHandler(dataSourceId);
        try {
            // 从请求中获取命令
            String command = request.get("command").toString();
            
            // 执行命令
            return handler.executeCommand(command);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public List<Map<String, Object>> getIndices(String dataSourceId, String collectionName) throws Exception {
        AbstractNoSQLHandler handler = getNoSQLHandler(dataSourceId);
        try {
            return handler.getIndexes(collectionName);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public boolean createIndex(String dataSourceId, Map<String, Object> request) throws Exception {
        AbstractNoSQLHandler handler = getNoSQLHandler(dataSourceId);
        try {
            // 从请求中获取集合名称和索引定义
            String collectionName = request.get("collection").toString();
            Map<String, Object> indexDefinition = (Map<String, Object>) request.get("index");
            
            // 创建索引
            return handler.createIndex(collectionName, indexDefinition);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public boolean deleteIndex(String dataSourceId, String collectionName, String indexName) throws Exception {
        AbstractNoSQLHandler handler = getNoSQLHandler(dataSourceId);
        try {
            return handler.dropIndex(collectionName, indexName);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }
} 