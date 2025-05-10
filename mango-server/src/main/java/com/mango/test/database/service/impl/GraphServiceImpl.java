package com.mango.test.database.service.impl;

import com.mango.test.database.entity.DataSource;
import com.mango.test.database.mapper.DataSourceMapper;
import com.mango.test.database.service.GraphService;
import com.mango.test.database.service.impl.datasource.AbstractGraphDBHandler;
import com.mango.test.database.service.impl.datasource.DatabaseHandlerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图数据库服务实现类
 */
@Slf4j
@Service
public class GraphServiceImpl implements GraphService {

    @Autowired
    private DataSourceMapper dataSourceMapper;

    /**
     * 获取图数据库处理器
     */
    private AbstractGraphDBHandler getGraphDBHandler(String dataSourceId) throws Exception {
        DataSource dataSource = dataSourceMapper.selectById(dataSourceId);
        if (dataSource == null) {
            throw new Exception("数据源不存在: " + dataSourceId);
        }
        return DatabaseHandlerFactory.getGraphDBHandler(dataSource);
    }

    @Override
    public Map<String, Object> getGraphSchema(String dataSourceId) throws Exception {
        AbstractGraphDBHandler handler = getGraphDBHandler(dataSourceId);
        Map<String, Object> schema = new HashMap<>();
        
        try {
            log.info("开始获取图数据库Schema, 数据源ID: {}", dataSourceId);
            
            // 1. 获取节点标签
            List<String> labels = handler.getLabels();
            log.info("获取到 {} 个节点标签", labels.size());
            schema.put("nodeTypes", convertLabelsToNodeTypes(handler, labels));
            schema.put("labels", labels); // 同时保留原始标签列表
            
            // 2. 获取关系类型
            List<String> relationshipTypes = handler.getRelationshipTypes();
            log.info("获取到 {} 个关系类型", relationshipTypes.size());
            schema.put("edgeTypes", convertRelationshipToEdgeTypes(handler, relationshipTypes));
            schema.put("relationshipTypes", relationshipTypes); // 同时保留原始关系类型列表
            
            // 3. 如果标签和关系都为空，尝试通过查询获取更多信息
            if (labels.isEmpty() && relationshipTypes.isEmpty()) {
                log.info("标签和关系类型为空，尝试通过查询获取更多信息");
                
                // 3.1 尝试直接查询节点和关系统计信息
                try {
                    String countQuery = "MATCH (n) RETURN count(n) as nodeCount";
                    Map<String, Object> nodeCountResult = handler.executeQueryForSingle(countQuery, null);
                    if (nodeCountResult != null && nodeCountResult.containsKey("nodeCount")) {
                        schema.put("nodeCount", nodeCountResult.get("nodeCount"));
                        log.info("节点总数: {}", nodeCountResult.get("nodeCount"));
                    }
                    
                    String relCountQuery = "MATCH ()-[r]->() RETURN count(r) as relCount";
                    Map<String, Object> relCountResult = handler.executeQueryForSingle(relCountQuery, null);
                    if (relCountResult != null && relCountResult.containsKey("relCount")) {
                        schema.put("relationshipCount", relCountResult.get("relCount"));
                        log.info("关系总数: {}", relCountResult.get("relCount"));
                    }
                } catch (Exception e) {
                    log.warn("获取节点和关系统计信息失败: {}", e.getMessage());
                }
                
                // 3.2 尝试从实际数据中提取模式信息
                try {
                    // 查询一些样本节点
                    String sampleNodesQuery = "MATCH (n) RETURN DISTINCT labels(n) as labels, count(n) as count LIMIT 10";
                    List<Map<String, Object>> sampleNodesResult = handler.executeQuery(sampleNodesQuery, null);
                    
                    if (sampleNodesResult != null && !sampleNodesResult.isEmpty()) {
                        List<Map<String, Object>> extractedNodeTypes = new ArrayList<>();
                        
                        for (Map<String, Object> row : sampleNodesResult) {
                            if (row.containsKey("labels") && row.get("labels") instanceof List) {
                                List<String> nodeLabels = (List<String>) row.get("labels");
                                for (String label : nodeLabels) {
                                    Map<String, Object> nodeType = new HashMap<>();
                                    nodeType.put("name", label);
                                    nodeType.put("count", row.get("count"));
                                    extractedNodeTypes.add(nodeType);
                                }
                            }
                        }
                        
                        if (!extractedNodeTypes.isEmpty()) {
                            schema.put("nodeTypes", extractedNodeTypes);
                            log.info("从样本数据中提取到 {} 个节点类型", extractedNodeTypes.size());
                        }
                    }
                    
                    // 查询一些样本关系
                    String sampleRelsQuery = "MATCH ()-[r]->() RETURN DISTINCT type(r) as type, count(r) as count LIMIT 10";
                    List<Map<String, Object>> sampleRelsResult = handler.executeQuery(sampleRelsQuery, null);
                    
                    if (sampleRelsResult != null && !sampleRelsResult.isEmpty()) {
                        List<Map<String, Object>> extractedEdgeTypes = new ArrayList<>();
                        
                        for (Map<String, Object> row : sampleRelsResult) {
                            if (row.containsKey("type")) {
                                Map<String, Object> edgeType = new HashMap<>();
                                edgeType.put("name", row.get("type"));
                                edgeType.put("count", row.get("count"));
                                extractedEdgeTypes.add(edgeType);
                            }
                        }
                        
                        if (!extractedEdgeTypes.isEmpty()) {
                            schema.put("edgeTypes", extractedEdgeTypes);
                            log.info("从样本数据中提取到 {} 个关系类型", extractedEdgeTypes.size());
                        }
                    }
                } catch (Exception e) {
                    log.warn("从样本数据中提取模式信息失败: {}", e.getMessage());
                }
            }
            
            // 4. 获取节点属性信息
            try {
                Map<String, Object> nodeProperties = new HashMap<>();
                for (String label : labels) {
                    List<Map<String, Object>> properties = handler.getNodeProperties(label);
                    if (properties != null && !properties.isEmpty()) {
                        nodeProperties.put(label, properties);
                    }
                }
                schema.put("nodeProperties", nodeProperties);
            } catch (Exception e) {
                log.warn("获取节点属性信息失败: {}", e.getMessage());
            }
            
            log.info("图数据库Schema获取完成");
            return schema;
        } catch (Exception e) {
            log.error("获取图数据库Schema失败: {}", e.getMessage(), e);
            throw e;
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    /**
     * 将标签列表转换为节点类型格式
     */
    private List<Map<String, Object>> convertLabelsToNodeTypes(AbstractGraphDBHandler handler, List<String> labels) {
        List<Map<String, Object>> nodeTypes = new ArrayList<>();
        
        for (String label : labels) {
            Map<String, Object> nodeType = new HashMap<>();
            nodeType.put("name", label);
            
            // 尝试获取节点数量
            try {
                String countQuery = "MATCH (n:" + label + ") RETURN count(n) as count";
                Map<String, Object> result = handler.executeQueryForSingle(countQuery, null);
                if (result != null && result.containsKey("count")) {
                    nodeType.put("count", result.get("count"));
                } else {
                    nodeType.put("count", 0);
                }
            } catch (Exception e) {
                log.warn("获取节点标签 {} 的数量失败: {}", label, e.getMessage());
                nodeType.put("count", 0);
            }
            
            nodeTypes.add(nodeType);
        }
        
        return nodeTypes;
    }

    /**
     * 将关系类型列表转换为边类型格式
     */
    private List<Map<String, Object>> convertRelationshipToEdgeTypes(AbstractGraphDBHandler handler, List<String> relationshipTypes) {
        List<Map<String, Object>> edgeTypes = new ArrayList<>();
        
        for (String type : relationshipTypes) {
            Map<String, Object> edgeType = new HashMap<>();
            edgeType.put("name", type);
            
            // 尝试获取关系数量
            try {
                String countQuery = "MATCH ()-[r:" + type + "]->() RETURN count(r) as count";
                Map<String, Object> result = handler.executeQueryForSingle(countQuery, null);
                if (result != null && result.containsKey("count")) {
                    edgeType.put("count", result.get("count"));
                } else {
                    edgeType.put("count", 0);
                }
            } catch (Exception e) {
                log.warn("获取关系类型 {} 的数量失败: {}", type, e.getMessage());
                edgeType.put("count", 0);
            }
            
            edgeTypes.add(edgeType);
        }
        
        return edgeTypes;
    }

    @Override
    public Map<String, Object> executeQuery(String dataSourceId, String query) throws Exception {
        AbstractGraphDBHandler handler = getGraphDBHandler(dataSourceId);
        try {
            Map<String, Object> parameters = new HashMap<>();
            return handler.executeGraphQuery(query, parameters);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public String createNode(String dataSourceId, Map<String, Object> node) throws Exception {
        AbstractGraphDBHandler handler = getGraphDBHandler(dataSourceId);
        try {
            // 创建节点的查询语句，具体查询语句根据图数据库类型(如Neo4j)而定
            String label = (String) node.getOrDefault("label", "Node");
            Map<String, Object> properties = (Map<String, Object>) node.getOrDefault("properties", new HashMap<>());
            
            // 构建Cypher查询语句（假设使用Neo4j的Cypher）
            StringBuilder query = new StringBuilder();
            query.append("CREATE (n:").append(label).append(" {");
            
            int i = 0;
            Map<String, Object> params = new HashMap<>();
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                if (i > 0) {
                    query.append(", ");
                }
                query.append(entry.getKey()).append(": $").append(entry.getKey());
                params.put(entry.getKey(), entry.getValue());
                i++;
            }
            
            query.append("}) RETURN id(n) as id");
            
            // 执行查询并获取节点ID
            Map<String, Object> result = handler.executeQueryForSingle(query.toString(), params);
            return result.get("id").toString();
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public void deleteNode(String dataSourceId, String nodeId) throws Exception {
        AbstractGraphDBHandler handler = getGraphDBHandler(dataSourceId);
        try {
            // 构建删除节点的查询语句
            String query = "MATCH (n) WHERE id(n) = $nodeId DETACH DELETE n";
            Map<String, Object> params = new HashMap<>();
            params.put("nodeId", Long.parseLong(nodeId));
            
            // 执行删除操作
            handler.executeUpdate(query, params);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public String createRelationship(String dataSourceId, Map<String, Object> relationship) throws Exception {
        AbstractGraphDBHandler handler = getGraphDBHandler(dataSourceId);
        try {
            // 获取关系参数
            String startNodeId = relationship.get("startNodeId").toString();
            String endNodeId = relationship.get("endNodeId").toString();
            String type = (String) relationship.getOrDefault("type", "RELATED_TO");
            Map<String, Object> properties = (Map<String, Object>) relationship.getOrDefault("properties", new HashMap<>());
            
            // 构建创建关系的查询语句
            StringBuilder query = new StringBuilder();
            query.append("MATCH (a), (b) WHERE id(a) = $startNodeId AND id(b) = $endNodeId ");
            query.append("CREATE (a)-[r:").append(type).append(" {");
            
            int i = 0;
            Map<String, Object> params = new HashMap<>();
            params.put("startNodeId", Long.parseLong(startNodeId));
            params.put("endNodeId", Long.parseLong(endNodeId));
            
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                if (i > 0) {
                    query.append(", ");
                }
                query.append(entry.getKey()).append(": $").append(entry.getKey());
                params.put(entry.getKey(), entry.getValue());
                i++;
            }
            
            query.append("}]->(b) RETURN id(r) as id");
            
            // 执行查询并获取关系ID
            Map<String, Object> result = handler.executeQueryForSingle(query.toString(), params);
            return result.get("id").toString();
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public void deleteRelationship(String dataSourceId, String relationshipId) throws Exception {
        AbstractGraphDBHandler handler = getGraphDBHandler(dataSourceId);
        try {
            // 构建删除关系的查询语句
            String query = "MATCH ()-[r]-() WHERE id(r) = $relationshipId DELETE r";
            Map<String, Object> params = new HashMap<>();
            params.put("relationshipId", Long.parseLong(relationshipId));
            
            // 执行删除操作
            handler.executeUpdate(query, params);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    public List<Map<String, Object>> getNeighbors(String dataSourceId, String nodeId, String direction, List<String> relationshipTypes, int depth, int limit) throws Exception {
        if (depth <= 0) depth = 1;
        if (limit <= 0) limit = 100;
        
        AbstractGraphDBHandler handler = getGraphDBHandler(dataSourceId);
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("nodeId", nodeId);
            parameters.put("limit", limit);
            
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("MATCH (n)");
            
            // 根据方向构建不同的查询模式
            if ("outgoing".equalsIgnoreCase(direction)) {
                queryBuilder.append("-[r");
            } else if ("incoming".equalsIgnoreCase(direction)) {
                queryBuilder.append("<-[r");
            } else { // "both" or any other value as default
                queryBuilder.append("-[r");
            }
            
            // 添加关系类型过滤
            if (relationshipTypes != null && !relationshipTypes.isEmpty()) {
                queryBuilder.append(":");
                queryBuilder.append(String.join("|", relationshipTypes));
            }
            
            // 完成关系模式
            if ("outgoing".equalsIgnoreCase(direction)) {
                queryBuilder.append("]->(m)");
            } else if ("incoming".equalsIgnoreCase(direction)) {
                queryBuilder.append("]-");
            } else { // "both" or any other value as default
                queryBuilder.append("]-");
                queryBuilder.append(direction.equalsIgnoreCase("both") ? "(m)" : ">(m)");
            }
            
            // 添加WHERE条件和RETURN语句
            queryBuilder.append(" WHERE id(n) = $nodeId RETURN m, r LIMIT $limit");
            
            return handler.executeQuery(queryBuilder.toString(), parameters);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public Map<String, Object> getNeighbors(String dataSourceId, Map<String, Object> request) throws Exception {
        // 从请求参数中提取必要信息
        String nodeId = request.get("nodeId").toString();
        String direction = (String) request.getOrDefault("direction", "both");
        List<String> relationshipTypes = (List<String>) request.getOrDefault("relationshipTypes", null);
        int depth = request.containsKey("depth") ? Integer.parseInt(request.get("depth").toString()) : 1;
        int limit = request.containsKey("limit") ? Integer.parseInt(request.get("limit").toString()) : 100;
        
        // 调用已实现的带参数版本getNeighbors方法
        List<Map<String, Object>> neighbors = getNeighbors(dataSourceId, nodeId, direction, relationshipTypes, depth, limit);
        
        // 封装结果
        Map<String, Object> result = new HashMap<>();
        result.put("neighbors", neighbors);
        result.put("nodeId", nodeId);
        result.put("count", neighbors.size());
        return result;
    }

    @Override
    public List<Map<String, Object>> getAllNodes(String dataSourceId, int limit) throws Exception {
        AbstractGraphDBHandler handler = getGraphDBHandler(dataSourceId);
        try {
            log.info("开始获取所有节点, 数据源ID: {}, 限制数量: {}", dataSourceId, limit);
            
            // 执行查询获取所有节点
            String query = "MATCH (n) RETURN id(n) as id, n, labels(n) as labels LIMIT " + limit;
            List<Map<String, Object>> queryResults = handler.executeQuery(query, null);
            
            List<Map<String, Object>> formattedNodes = new ArrayList<>();
            for (Map<String, Object> row : queryResults) {
                Map<String, Object> nodeInfo = new HashMap<>();
                
                // 提取节点ID
                Object id = row.get("id");
                nodeInfo.put("id", id);
                
                // 提取节点标签 - 处理不同可能的返回类型
                List<String> labelsList = new ArrayList<>();
                Object labelsObj = row.get("labels");
                
                if (labelsObj instanceof List) {
                    // 如果是列表类型，直接转换
                    labelsList = (List<String>) labelsObj;
                } else if (labelsObj instanceof String) {
                    // 如果是字符串，尝试解析或作为单个标签添加
                    String labelsStr = (String) labelsObj;
                    // 去掉可能的括号，并按逗号分割
                    if (labelsStr.startsWith("[") && labelsStr.endsWith("]")) {
                        labelsStr = labelsStr.substring(1, labelsStr.length() - 1);
                    }
                    String[] parts = labelsStr.split(",");
                    for (String part : parts) {
                        part = part.trim();
                        if (!part.isEmpty()) {
                            labelsList.add(part);
                        }
                    }
                } else if (labelsObj != null) {
                    // 如果是其他类型，转换为字符串并作为单个标签
                    labelsList.add(labelsObj.toString());
                }
                
                nodeInfo.put("labels", labelsList);
                
                // 提取节点属性
                Map<String, Object> nodeData = (Map<String, Object>) row.get("n");
                if (nodeData != null && nodeData.containsKey("properties")) {
                    nodeInfo.put("properties", nodeData.get("properties"));
                }
                
                // 添加显示名称供前端选择使用
                StringBuilder displayName = new StringBuilder();
                displayName.append("ID:").append(id);
                
                if (labelsList != null && !labelsList.isEmpty()) {
                    displayName.append(" [");
                    displayName.append(String.join(", ", labelsList));
                    displayName.append("]");
                }
                
                // 尝试添加名称属性作为显示名
                if (nodeData != null && nodeData.containsKey("properties")) {
                    Map<String, Object> props = (Map<String, Object>) nodeData.get("properties");
                    if (props.containsKey("name")) {
                        displayName.append(" - ").append(props.get("name"));
                    }
                }
                
                nodeInfo.put("displayName", displayName.toString());
                formattedNodes.add(nodeInfo);
            }
            
            log.info("获取到 {} 个节点", formattedNodes.size());
            return formattedNodes;
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public List<Map<String, Object>> getNodeProperties(String dataSourceId, String nodeLabel) throws Exception {
        AbstractGraphDBHandler handler = getGraphDBHandler(dataSourceId);
        
        try {
            // 尝试获取节点属性
            List<Map<String, Object>> properties = handler.getNodeProperties(nodeLabel);
            if (properties != null && !properties.isEmpty()) {
                return properties;
            }
            
            // 如果抽象方法没有返回结果，尝试手动获取一个样例节点的属性
            String cypher = "MATCH (n:" + nodeLabel + ") RETURN properties(n) as props LIMIT 1";
            List<Map<String, Object>> results = handler.executeQuery(cypher, new HashMap<>());
            
            if (results != null && !results.isEmpty()) {
                Map<String, Object> result = results.get(0);
                Map<String, Object> props = (Map<String, Object>) result.get("props");
                
                if (props != null) {
                    List<Map<String, Object>> propList = new ArrayList<>();
                    for (Map.Entry<String, Object> entry : props.entrySet()) {
                        Map<String, Object> propInfo = new HashMap<>();
                        propInfo.put("name", entry.getKey());
                        propInfo.put("type", entry.getValue() != null ? entry.getValue().getClass().getSimpleName() : "String");
                        propList.add(propInfo);
                    }
                    return propList;
                }
            }
            
            // 如果还是没有属性，返回默认属性
            List<Map<String, Object>> defaultProps = new ArrayList<>();
            
            Map<String, Object> idProp = new HashMap<>();
            idProp.put("name", "id");
            idProp.put("type", "String");
            defaultProps.add(idProp);
            
            Map<String, Object> nameProp = new HashMap<>();
            nameProp.put("name", "name");
            nameProp.put("type", "String");
            defaultProps.add(nameProp);
            
            return defaultProps;
        } catch (Exception e) {
            log.error("获取节点属性失败: {}", e.getMessage(), e);
            
            // 出错时返回默认属性
            List<Map<String, Object>> defaultProps = new ArrayList<>();
            
            Map<String, Object> idProp = new HashMap<>();
            idProp.put("name", "id");
            idProp.put("type", "String");
            defaultProps.add(idProp);
            
            Map<String, Object> nameProp = new HashMap<>();
            nameProp.put("name", "name");
            nameProp.put("type", "String");
            defaultProps.add(nameProp);
            
            return defaultProps;
        }
    }

    @Override
    public List<Map<String, Object>> getEdgeProperties(String dataSourceId, String edgeType) throws Exception {
        AbstractGraphDBHandler handler = getGraphDBHandler(dataSourceId);
        
        try {
            // 尝试获取边属性
            List<Map<String, Object>> properties = handler.getEdgeProperties(edgeType);
            if (properties != null && !properties.isEmpty()) {
                return properties;
            }
            
            // 如果抽象方法没有返回结果，尝试手动获取一个样例边的属性
            String cypher = "MATCH ()-[r:" + edgeType + "]->() RETURN properties(r) as props LIMIT 1";
            List<Map<String, Object>> results = handler.executeQuery(cypher, new HashMap<>());
            
            if (results != null && !results.isEmpty()) {
                Map<String, Object> result = results.get(0);
                Map<String, Object> props = (Map<String, Object>) result.get("props");
                
                if (props != null) {
                    List<Map<String, Object>> propList = new ArrayList<>();
                    for (Map.Entry<String, Object> entry : props.entrySet()) {
                        Map<String, Object> propInfo = new HashMap<>();
                        propInfo.put("name", entry.getKey());
                        propInfo.put("type", entry.getValue() != null ? entry.getValue().getClass().getSimpleName() : "String");
                        propList.add(propInfo);
                    }
                    return propList;
                }
            }
            
            // 如果还是没有属性，返回默认属性
            List<Map<String, Object>> defaultProps = new ArrayList<>();
            
            Map<String, Object> idProp = new HashMap<>();
            idProp.put("name", "id");
            idProp.put("type", "String");
            defaultProps.add(idProp);
            
            Map<String, Object> typeProp = new HashMap<>();
            typeProp.put("name", "type");
            typeProp.put("type", "String");
            defaultProps.add(typeProp);
            
            Map<String, Object> weightProp = new HashMap<>();
            weightProp.put("name", "weight");
            weightProp.put("type", "Double");
            defaultProps.add(weightProp);
            
            return defaultProps;
        } catch (Exception e) {
            log.error("获取边属性失败: {}", e.getMessage(), e);
            
            // 出错时返回默认属性
            List<Map<String, Object>> defaultProps = new ArrayList<>();
            
            Map<String, Object> idProp = new HashMap<>();
            idProp.put("name", "id");
            idProp.put("type", "String");
            defaultProps.add(idProp);
            
            Map<String, Object> typeProp = new HashMap<>();
            typeProp.put("name", "type");
            typeProp.put("type", "String");
            defaultProps.add(typeProp);
            
            return defaultProps;
        }
    }
} 