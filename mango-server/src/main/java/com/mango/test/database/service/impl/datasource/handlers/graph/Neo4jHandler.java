package com.mango.test.database.service.impl.datasource.handlers.graph;

import com.mango.test.database.entity.DataSource;
import com.mango.test.database.service.impl.datasource.AbstractGraphDBHandler;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.*;
import org.neo4j.driver.Driver;
import org.neo4j.driver.summary.SummaryCounters;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;
import org.springframework.util.StringUtils;

import java.sql.*;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Neo4j数据库处理器
 */
@Slf4j
public class Neo4jHandler extends AbstractGraphDBHandler {
    
    private static final String NEO4J_DRIVER = "org.neo4j.driver.Driver";
    private static final String DEFAULT_PORT = "7687";
    private Driver driver;
    private Session session;
    
    public Neo4jHandler(DataSource dataSource) {
        super(dataSource);
    }
    
    /**
     * 获取元数据列表
     * @param pattern 名称匹配模式
     * @return 元数据列表
     */
    @Override
    public List<Map<String, Object>> getMetadataList(String pattern) {
        return getLabels(pattern);
    }
    
    /**
     * 获取Neo4j图数据库中的所有节点标签
     * @return 标签列表
     */
    @Override
    public List<String> getLabels() {
        List<String> labels = new ArrayList<String>();
        
        try {
            if (!initConnection()) {
                log.error("获取Neo4j标签失败: 无法初始化连接");
                return labels;
            }
            
            // 创建新会话，确保会话有效
            try (Session session = driver.session()) {
                // 使用Cypher获取所有标签
                Result result = session.run("CALL db.labels()");
                
                while (result.hasNext()) {
                    Record record = result.next();
                    String label = record.get(0).asString();
                    labels.add(label);
                }
                
                log.info("成功获取 {} 个节点标签", labels.size());
            }
        } catch (Exception e) {
            log.error("获取Neo4j标签列表失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        
        return labels;
    }
    
    /**
     * 初始化Neo4j连接
     */
    private synchronized boolean initConnection() {
        if (driver != null) {
            return true;
        }
        
        try {
            // 构建Neo4j连接URI
            String host = dataSource.getHost();
            String port = StringUtils.hasText(dataSource.getPort()) ? dataSource.getPort() : DEFAULT_PORT;
            
            // 使用默认的协议和SSL配置
            boolean useSSL = false; // 默认不使用SSL
            
            // 构建连接URL
            String uri = "bolt" + (useSSL ? "s" : "") + "://" + host + ":" + port;
            
            // 创建Neo4j配置
            Config config;
            if (useSSL) {
                Config.ConfigBuilder builder = Config.builder()
                    .withEncryption()
                    .withConnectionTimeout(5, TimeUnit.SECONDS);
                config = builder.build();
            } else {
                Config.ConfigBuilder builder = Config.builder()
                    .withConnectionTimeout(5, TimeUnit.SECONDS);
                config = builder.build();
            }
            
            // 创建驱动
            if (StringUtils.hasText(dataSource.getUsername()) && 
                StringUtils.hasText(dataSource.getPassword())) {
                driver = GraphDatabase.driver(
                    uri, 
                    AuthTokens.basic(dataSource.getUsername(), dataSource.getPassword()),
                    config
                );
            } else {
                driver = GraphDatabase.driver(uri, config);
            }
            
            // 验证连接
            try (Session session = driver.session()) {
                session.run("RETURN 1").consume();
            }
            
            return true;
        } catch (Exception e) {
            log.error("初始化Neo4j连接失败: {}", e.getMessage(), e);
            closeConnection();
            return false;
        }
    }
    
    /**
     * 关闭Neo4j连接
     */
    private synchronized void closeConnection() {
        if (session != null) {
            try {
                session.close();
            } catch (Exception e) {
                log.error("关闭Neo4j会话失败: {}", e.getMessage(), e);
            } finally {
                session = null;
            }
        }
        
        if (driver != null) {
            try {
                driver.close();
            } catch (Exception e) {
                log.error("关闭Neo4j驱动失败: {}", e.getMessage(), e);
            } finally {
                driver = null;
            }
        }
    }
    
    @Override
    public boolean testConnection() {
        try {
            if (!initConnection()) {
                return false;
            }
            
            // 测试连接是否正常工作
            Result result = session.run("RETURN 1 AS value");
            return result.hasNext() && result.next().get("value").asInt() == 1;
        } catch (Exception e) {
            log.error("测试Neo4j连接失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection();
        }
    }
    
    public List<Map<String, Object>> getLabels(String pattern) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        try {
            if (!initConnection()) {
                return result;
            }
            
            // 获取所有标签
            String cypher = "CALL db.labels()";
            Result queryResult = session.run(cypher);
            
            while (queryResult.hasNext()) {
                Record record = queryResult.next();
                String label = record.get(0).asString();
                
                // 应用模式过滤
                if (StringUtils.hasText(pattern) && !label.contains(pattern)) {
                    continue;
                }
                
                Map<String, Object> labelInfo = new HashMap<>();
                labelInfo.put("name", label);
                labelInfo.put("type", "label");
                
                // 获取标签下的节点数量
                String countCypher = "MATCH (n:" + label + ") RETURN count(n) AS count";
                Result countResult = session.run(countCypher);
                if (countResult.hasNext()) {
                    long count = countResult.next().get("count").asLong();
                    labelInfo.put("count", count);
                }
                
                result.add(labelInfo);
            }
        } catch (Exception e) {
            log.error("获取Neo4j标签列表失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        return result;
    }

    /**
     * 将Neo4j的Value对象转换为Java对象
     */
    private Object convertNeo4jValue(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }
        
        switch (value.type().name()) {
            case "STRING":
                return value.asString();
            case "INTEGER":
                return value.asLong();
            case "FLOAT":
                return value.asDouble();
            case "BOOLEAN":
                return value.asBoolean();
            case "NULL":
                return null;
            case "NODE":
                return convertNode(value.asNode());
            case "RELATIONSHIP":
                return convertRelationship(value.asRelationship());
            case "PATH":
                return convertPath(value.asPath());
            case "LIST":
                return convertList(value.asList());
            case "MAP":
                return value.asMap();
            default:
                return value.toString();
        }
    }
    
    /**
     * 将Neo4j的Node对象转换为Map
     */
    private Map<String, Object> convertNode(Node node) {
        Map<String, Object> nodeData = new HashMap<>();
        nodeData.put("id", node.id());
        
        List<String> labels = new ArrayList<String>();
        for (String label : node.labels()) {
            labels.add(label);
        }
        nodeData.put("labels", labels);
        
        // 添加属性
        nodeData.put("properties", node.asMap());
        
        return nodeData;
    }
    
    /**
     * 将Neo4j的Relationship对象转换为Map
     */
    private Map<String, Object> convertRelationship(Relationship relationship) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", relationship.id());
        result.put("type", relationship.type());
        result.put("startNodeId", relationship.startNodeId());
        result.put("endNodeId", relationship.endNodeId());
        
        Map<String, Object> properties = new HashMap<>();
        relationship.asMap().forEach((key, value) -> properties.put(key, value));
        result.put("properties", properties);
        
        return result;
    }
    
    /**
     * 将Neo4j的Path对象转换为Map
     */
    private Map<String, Object> convertPath(Path path) {
        Map<String, Object> pathData = new HashMap<>();
        
        List<Map<String, Object>> nodes = new ArrayList<Map<String, Object>>();
        for (Node node : path.nodes()) {
            nodes.add(convertNode(node));
        }
        
        List<Map<String, Object>> relationships = new ArrayList<Map<String, Object>>();
        for (Relationship rel : path.relationships()) {
            relationships.add(convertRelationship(rel));
        }
        
        pathData.put("nodes", nodes);
        pathData.put("relationships", relationships);
        pathData.put("length", path.length());
        
        return pathData;
    }
    
    /**
     * 将Neo4j的List对象转换为Java List
     */
    private List<Object> convertList(List<Object> list) {
        List<Object> result = new ArrayList<Object>();
        
        for (Object item : list) {
            if (item instanceof Node) {
                result.add(convertNode((Node) item));
            } else if (item instanceof Relationship) {
                result.add(convertRelationship((Relationship) item));
            } else if (item instanceof Path) {
                result.add(convertPath((Path) item));
            } else if (item instanceof List) {
                result.add(convertList((List<Object>) item));
            } else if (item instanceof Map) {
                result.add(item);
            } else {
                result.add(item);
            }
        }
        return result;
    }
    
    /**
     * 执行Cypher查询并返回结果
     */
    private List<Map<String, Object>> executeCypher(String cypher, Map<String, Object> parameters) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        
        try {
            if (session == null || !session.isOpen()) {
                session = driver.session();
            }
            
            // 执行查询
            Result queryResult;
            if (parameters != null && !parameters.isEmpty()) {
                queryResult = session.run(cypher, parameters);
            } else {
                queryResult = session.run(cypher);
            }
            
            // 处理结果
            while (queryResult.hasNext()) {
                Record record = queryResult.next();
                Map<String, Object> row = new HashMap<>();
                
                for (String key : record.keys()) {
                    row.put(key, convertNeo4jValue(record.get(key)));
                }
                
                result.add(row);
            }
        } catch (Exception e) {
            log.error("执行Cypher查询失败: {}", e.getMessage(), e);
        }
        return result;
    }
    
    @Override
    public List<Map<String, Object>> executeQuery(String query, Map<String, Object> parameters) {
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        
        try {
            if (!initConnection()) {
                return results;
            }
            
            return executeCypher(query, parameters);
        } catch (Exception e) {
            log.error("执行Neo4j查询失败: {}", e.getMessage(), e);
            return results;
        } finally {
            closeConnection();
        }
    }
    
    public List<Map<String, Object>> getRelationshipTypes(String pattern) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        try {
            if (!initConnection()) {
                return result;
            }
            
            // 获取所有关系类型
            String cypher = "CALL db.relationshipTypes()";
            Result queryResult = session.run(cypher);
            
            while (queryResult.hasNext()) {
                Record record = queryResult.next();
                String type = record.get(0).asString();
                
                // 应用模式过滤
                if (StringUtils.hasText(pattern) && !type.contains(pattern)) {
                    continue;
                }
                
                Map<String, Object> typeInfo = new HashMap<>();
                typeInfo.put("name", type);
                typeInfo.put("type", "relationshipType");
                
                // 获取此类型的关系数量
                String countCypher = "MATCH ()-[r:" + type + "]->() RETURN count(r) AS count";
                Result countResult = session.run(countCypher);
                if (countResult.hasNext()) {
                    long count = countResult.next().get("count").asLong();
                    typeInfo.put("count", count);
                }
                
                result.add(typeInfo);
            }
        } catch (Exception e) {
            log.error("获取Neo4j关系类型列表失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        return result;
    }
    
    public List<Map<String, Object>> getPropertyKeys(String pattern) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        try {
            if (!initConnection()) {
                return result;
            }
            
            // 获取所有属性键
            String cypher = "CALL db.propertyKeys()";
            Result queryResult = session.run(cypher);
            
            while (queryResult.hasNext()) {
                Record record = queryResult.next();
                String key = record.get(0).asString();
                
                // 应用模式过滤
                if (StringUtils.hasText(pattern) && !key.contains(pattern)) {
                    continue;
                }
                
                Map<String, Object> keyInfo = new HashMap<>();
                keyInfo.put("name", key);
                keyInfo.put("type", "propertyKey");
                
                result.add(keyInfo);
            }
        } catch (Exception e) {
            log.error("获取Neo4j属性键列表失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        return result;
    }
    
    public Map<String, Object> getDatabaseInfo() {
        Map<String, Object> info = new HashMap<>();
        try {
            if (!initConnection()) {
                return info;
            }
            
            // 获取数据库信息
            String cypher = "CALL dbms.components() YIELD name, versions, edition";
            Result queryResult = session.run(cypher);
            
            if (queryResult.hasNext()) {
                Record record = queryResult.next();
                info.put("name", record.get("name").asString());
                info.put("version", record.get("versions").asList(Value::asString).get(0));
                info.put("edition", record.get("edition").asString());
            }
            
            // 获取统计信息
            Map<String, Object> stats = new HashMap<>();
            
            // 节点数量
            cypher = "MATCH (n) RETURN count(n) AS nodeCount";
            queryResult = session.run(cypher);
            if (queryResult.hasNext()) {
                stats.put("nodeCount", queryResult.next().get("nodeCount").asLong());
            }
            
            // 关系数量
            cypher = "MATCH ()-[r]->() RETURN count(r) AS relationshipCount";
            queryResult = session.run(cypher);
            if (queryResult.hasNext()) {
                stats.put("relationshipCount", queryResult.next().get("relationshipCount").asLong());
            }
            
            // 标签数量
            cypher = "CALL db.labels() YIELD label RETURN count(label) AS labelCount";
            queryResult = session.run(cypher);
            if (queryResult.hasNext()) {
                stats.put("labelCount", queryResult.next().get("labelCount").asLong());
            }
            
            // 关系类型数量
            cypher = "CALL db.relationshipTypes() YIELD relationshipType RETURN count(relationshipType) AS relationshipTypeCount";
            queryResult = session.run(cypher);
            if (queryResult.hasNext()) {
                stats.put("relationshipTypeCount", queryResult.next().get("relationshipTypeCount").asLong());
            }
            
            // 属性键数量
            cypher = "CALL db.propertyKeys() YIELD propertyKey RETURN count(propertyKey) AS propertyKeyCount";
            queryResult = session.run(cypher);
            if (queryResult.hasNext()) {
                stats.put("propertyKeyCount", queryResult.next().get("propertyKeyCount").asLong());
            }
            
            info.put("stats", stats);
        } catch (Exception e) {
            log.error("获取Neo4j数据库信息失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        return info;
    }
    
    private <T> List<T> createTypedArrayList() {
        return new ArrayList<T>();
    }

    /**
     * 获取指定标签的节点属性
     *
     * @param label 节点标签
     * @return 属性信息列表
     */
    @Override
    public List<Map<String, Object>> getNodeProperties(String label) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        
        try {
            if (!initConnection()) {
                return result;
            }
            
            try (Session session = driver.session()) {
                String query = "MATCH (n:" + label + ") RETURN n LIMIT 1";
                Result queryResult = session.run(query);
                
                if (queryResult.hasNext()) {
                    Record record = queryResult.next();
                    Node node = record.get("n").asNode();
                    
                    // 获取属性键列表
                    for (String key : node.keys()) {
                        Map<String, Object> propertyInfo = new HashMap<>();
                        propertyInfo.put("name", key);
                        
                        // 获取属性类型
                        Value value = node.get(key);
                        propertyInfo.put("type", value.type().name().toLowerCase());
                        
                        result.add(propertyInfo);
                    }
                }
                
                // 如果获取不到节点，尝试通过Cypher获取属性键
                if (result.isEmpty()) {
                    String propertiesQuery = "CALL db.schema.nodeTypeProperties() " +
                        "YIELD nodeType, propertyName, propertyTypes " +
                        "WHERE nodeType = $label " +
                        "RETURN propertyName, propertyTypes";
                    
                    Map<String, Object> params = new HashMap<>();
                    params.put("label", label);
                    
                    Result propertiesResult = session.run(propertiesQuery, params);
                    while (propertiesResult.hasNext()) {
                        Record record = propertiesResult.next();
                        
                        Map<String, Object> propertyInfo = new HashMap<>();
                        propertyInfo.put("name", record.get("propertyName").asString());
                        propertyInfo.put("type", record.get("propertyTypes").asList(Value::asString).get(0).toLowerCase());
                        
                        result.add(propertyInfo);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取Neo4j节点属性失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        
        return result;
    }

    /**
     * 创建简单的Map，替代Java 9+ 的Map.of方法
     */
    private Map<String, Object> createSimpleMap(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    /**
     * 创建包含两个键值对的Map，替代Java 9+ 的Map.of方法
     */
    private Map<String, Object> createMap(String key1, Object value1, String key2, Object value2) {
        Map<String, Object> map = new HashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        return map;
    }

    /**
     * 创建包含三个键值对的Map，替代Java 9+ 的Map.of方法
     */
    private Map<String, Object> createMap(String key1, Object value1, String key2, Object value2, 
                                         String key3, Object value3) {
        Map<String, Object> map = new HashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        return map;
    }

    /**
     * 创建包含四个键值对的Map，替代Java 9+ 的Map.of方法
     */
    private Map<String, Object> createMap(String key1, Object value1, String key2, Object value2, 
                                         String key3, Object value3, String key4, Object value4) {
        Map<String, Object> map = new HashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        map.put(key4, value4);
        return map;
    }

    /**
     * 替代Java 9+的Map.of方法
     */
    private Map<String, Object> createMap(String key, boolean value, String key2, String value2) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        map.put(key2, value2);
        return map;
    }

    // 修改所有使用Map.of的地方
    public List<Map<String, Object>> createNode(String label, Map<String, Object> properties) {
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        try {
            if (!initConnection()) {
                Map<String, Object> errorMap = createMap("success", false, "error", "无法连接到Neo4j数据库");
                results.add(errorMap);
                return results;
            }
            
            // 执行创建节点的Cypher
            StringBuilder cypher = new StringBuilder();
            cypher.append("CREATE (n:");
            cypher.append(label);
            cypher.append(" $props) RETURN n");
            
            Map<String, Object> params = new HashMap<>();
            params.put("props", properties);
            
            Result result = session.run(cypher.toString(), params);
            
            while (result.hasNext()) {
                Record record = result.next();
                
                Node node = record.get("n").asNode();
                Map<String, Object> nodeData = convertNode(node);
                
                // 添加操作状态
                Map<String, Object> response = new HashMap<>(nodeData);
                response.put("success", true);
                
                results.add(response);
            }
            
            if (results.isEmpty()) {
                // 如果没有结果，返回一个成功状态但没有节点数据
                results.add(createMap("success", true, "message", "节点已创建，但没有返回数据"));
            }
            
        } catch (Exception e) {
            log.error("创建Neo4j节点失败: {}", e.getMessage(), e);
            
            // 添加错误信息
            Map<String, Object> errorMap = createMap("success", false, "error", e.getMessage());
            results.add(errorMap);
        }
        
        return results;
    }
    
    public List<Map<String, Object>> updateNode(long nodeId, Map<String, Object> properties) {
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        try {
            if (!initConnection()) {
                results.add(createMap("success", false, "error", "无法连接到Neo4j数据库"));
                return results;
            }
            
            // 检查节点是否存在
            String checkCypher = "MATCH (n) WHERE id(n) = $id RETURN n";
            Map<String, Object> params = new HashMap<>();
            params.put("id", nodeId);
            
            Result checkResult = session.run(checkCypher, params);
            if (!checkResult.hasNext()) {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("success", false);
                errorMap.put("error", "节点不存在");
                results.add(errorMap);
                return results;
            }
            
            // 更新节点属性
            StringBuilder cypher = new StringBuilder("MATCH (n) WHERE id(n) = $id ");
            
            if (properties != null && !properties.isEmpty()) {
                cypher.append("SET n = $props ");
                params.put("props", properties);
            }
            
            cypher.append("RETURN n");
            
            Result queryResult = session.run(cypher.toString(), params);
            if (queryResult.hasNext()) {
                Record record = queryResult.next();
                Node node = record.get("n").asNode();
                
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("success", true);
                resultMap.put("id", node.id());
                
                // Create the ArrayList properly from Iterable
                List<String> labelsList = new ArrayList<String>();
                for (String label : node.labels()) {
                    labelsList.add(label);
                }
                resultMap.put("labels", labelsList);
                
                resultMap.put("properties", node.asMap());
                results.add(resultMap);
            } else {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("success", false);
                errorMap.put("error", "更新节点失败");
                results.add(errorMap);
            }
        } catch (Exception e) {
            log.error("更新Neo4j节点失败: {}", e.getMessage(), e);
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("success", false);
            errorMap.put("error", e.getMessage());
            results.add(errorMap);
        } finally {
            closeConnection();
        }
        return results;
    }
    
    public List<Map<String, Object>> deleteNode(long nodeId) {
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        try {
            if (!initConnection()) {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("success", false);
                errorMap.put("error", "无法连接到Neo4j数据库");
                results.add(errorMap);
                return results;
            }
            
            // 检查节点是否存在
            String checkCypher = "MATCH (n) WHERE id(n) = $id RETURN n";
            Map<String, Object> params = new HashMap<>();
            params.put("id", nodeId);
            
            Result checkResult = session.run(checkCypher, params);
            if (!checkResult.hasNext()) {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("success", false);
                errorMap.put("error", "节点不存在");
                results.add(errorMap);
                return results;
            }
            
            // 先保存节点信息，以便返回
            Node node = checkResult.next().get("n").asNode();
            Map<String, Object> nodeInfo = new HashMap<>();
            nodeInfo.put("id", node.id());
            
            // Create the ArrayList properly from Iterable
            List<String> labelsList = new ArrayList<String>();
            for (String label : node.labels()) {
                labelsList.add(label);
            }
            nodeInfo.put("labels", labelsList);
            
            nodeInfo.put("properties", node.asMap());
            
            // 删除节点
            String deleteCypher = "MATCH (n) WHERE id(n) = $id DETACH DELETE n";
            session.run(deleteCypher, params);
            
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("success", true);
            resultMap.put("node", nodeInfo);
            results.add(resultMap);
        } catch (Exception e) {
            log.error("删除Neo4j节点失败: {}", e.getMessage(), e);
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("success", false);
            errorMap.put("error", e.getMessage());
            results.add(errorMap);
        } finally {
            closeConnection();
        }
        return results;
    }
    
    public List<Map<String, Object>> createRelationship(long startNodeId, long endNodeId, String type, Map<String, Object> properties) {
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        try {
            if (!initConnection()) {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("success", false);
                errorMap.put("error", "无法连接到Neo4j数据库");
                results.add(errorMap);
                return results;
            }
            
            if (!StringUtils.hasText(type)) {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("success", false);
                errorMap.put("error", "关系类型不能为空");
                results.add(errorMap);
                return results;
            }
            
            // 检查节点是否存在
            String checkCypher = "MATCH (a), (b) WHERE id(a) = $startId AND id(b) = $endId RETURN a, b";
            Map<String, Object> params = new HashMap<>();
            params.put("startId", startNodeId);
            params.put("endId", endNodeId);
            
            Result checkResult = session.run(checkCypher, params);
            if (!checkResult.hasNext()) {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("success", false);
                errorMap.put("error", "开始节点或结束节点不存在");
                results.add(errorMap);
                return results;
            }
            
            // 创建关系
            StringBuilder cypher = new StringBuilder();
            cypher.append("MATCH (a), (b) WHERE id(a) = $startId AND id(b) = $endId ");
            cypher.append("CREATE (a)-[r:").append(type).append(" $props]->(b) ");
            cypher.append("RETURN r");
            
            params.put("props", properties != null ? properties : new HashMap<>());
            
            Result queryResult = session.run(cypher.toString(), params);
            if (queryResult.hasNext()) {
                Record record = queryResult.next();
                Relationship relationship = record.get("r").asRelationship();
                
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("success", true);
                resultMap.put("id", relationship.id());
                resultMap.put("type", relationship.type());
                resultMap.put("startNodeId", relationship.startNodeId());
                resultMap.put("endNodeId", relationship.endNodeId());
                resultMap.put("properties", relationship.asMap());
                results.add(resultMap);
            } else {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("success", false);
                errorMap.put("error", "创建关系失败");
                results.add(errorMap);
            }
        } catch (Exception e) {
            log.error("创建Neo4j关系失败: {}", e.getMessage(), e);
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("success", false);
            errorMap.put("error", e.getMessage());
            results.add(errorMap);
        } finally {
            closeConnection();
        }
        return results;
    }
    
    public List<Map<String, Object>> updateRelationship(long relationshipId, Map<String, Object> properties) {
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        try {
            if (!initConnection()) {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("success", false);
                errorMap.put("error", "无法连接到Neo4j数据库");
                results.add(errorMap);
                return results;
            }
            
            // 检查关系是否存在
            String checkCypher = "MATCH ()-[r]->() WHERE id(r) = $id RETURN r";
            Map<String, Object> params = new HashMap<>();
            params.put("id", relationshipId);
            
            Result checkResult = session.run(checkCypher, params);
            if (!checkResult.hasNext()) {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("success", false);
                errorMap.put("error", "关系不存在");
                results.add(errorMap);
                return results;
            }
            
            // 更新关系属性
            StringBuilder cypher = new StringBuilder("MATCH ()-[r]->() WHERE id(r) = $id ");
            
            if (properties != null && !properties.isEmpty()) {
                cypher.append("SET r = $props ");
                params.put("props", properties);
            }
            
            cypher.append("RETURN r");
            
            Result queryResult = session.run(cypher.toString(), params);
            if (queryResult.hasNext()) {
                Record record = queryResult.next();
                Relationship relationship = record.get("r").asRelationship();
                
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("success", true);
                resultMap.put("id", relationship.id());
                resultMap.put("type", relationship.type());
                resultMap.put("startNodeId", relationship.startNodeId());
                resultMap.put("endNodeId", relationship.endNodeId());
                resultMap.put("properties", relationship.asMap());
                results.add(resultMap);
            } else {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("success", false);
                errorMap.put("error", "更新关系失败");
                results.add(errorMap);
            }
        } catch (Exception e) {
            log.error("更新Neo4j关系失败: {}", e.getMessage(), e);
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("success", false);
            errorMap.put("error", e.getMessage());
            results.add(errorMap);
        } finally {
            closeConnection();
        }
        return results;
    }
    
    public List<Map<String, Object>> deleteRelationship(long relationshipId) {
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        try {
            if (!initConnection()) {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("success", false);
                errorMap.put("error", "无法连接到Neo4j数据库");
                results.add(errorMap);
                return results;
            }
            
            // 检查关系是否存在
            String checkCypher = "MATCH ()-[r]->() WHERE id(r) = $id RETURN r";
            Map<String, Object> params = new HashMap<>();
            params.put("id", relationshipId);
            
            Result checkResult = session.run(checkCypher, params);
            if (!checkResult.hasNext()) {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("success", false);
                errorMap.put("error", "关系不存在");
                results.add(errorMap);
                return results;
            }
            
            // 先保存关系信息，以便返回
            Relationship relationship = checkResult.next().get("r").asRelationship();
            Map<String, Object> relationshipInfo = new HashMap<>();
            relationshipInfo.put("id", relationship.id());
            relationshipInfo.put("type", relationship.type());
            relationshipInfo.put("startNodeId", relationship.startNodeId());
            relationshipInfo.put("endNodeId", relationship.endNodeId());
            relationshipInfo.put("properties", relationship.asMap());
            
            // 删除关系
            String deleteCypher = "MATCH ()-[r]->() WHERE id(r) = $id DELETE r";
            session.run(deleteCypher, params);
            
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("success", true);
            resultMap.put("relationship", relationshipInfo);
            results.add(resultMap);
        } catch (Exception e) {
            log.error("删除Neo4j关系失败: {}", e.getMessage(), e);
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("success", false);
            errorMap.put("error", e.getMessage());
            results.add(errorMap);
        } finally {
            closeConnection();
        }
        return results;
    }
    
    @Override
    public String getDefaultPort() {
        return DEFAULT_PORT;
    }
    
    @Override
    public String getDriverClassName() {
        return NEO4J_DRIVER;
    }

    /**
     * 获取指定标签的节点
     * @param label 标签
     * @param limit 限制数量
     * @return 节点列表
     */
    @Override
    public List<Map<String, Object>> getNodesByLabel(String label, int limit) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        
        try {
            if (!initConnection()) {
                return result;
            }
            
            try (Session session = driver.session()) {
                String query = "MATCH (n:" + label + ") RETURN n LIMIT " + limit;
                
                Result queryResult = session.run(query);
                
                while (queryResult.hasNext()) {
                    Record record = queryResult.next();
                    Node node = record.get("n").asNode();
                    
                    Map<String, Object> nodeData = new HashMap<>();
                    nodeData.put("id", node.id());
                    
                    // 获取节点的所有属性
                    Map<String, Object> properties = new HashMap<>();
                    for (String key : node.keys()) {
                        Value value = node.get(key);
                        properties.put(key, value.asObject());
                    }
                    nodeData.put("properties", properties);
                    
                    // 获取节点的所有标签
                    List<String> labels = new ArrayList<String>();
                    for (String nodeLabel : node.labels()) {
                        labels.add(nodeLabel);
                    }
                    nodeData.put("labels", labels);
                    
                    result.add(nodeData);
                }
            }
        } catch (Exception e) {
            log.error("获取Neo4j节点失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        
        return result;
    }

    /**
     * 执行图查询，返回图数据结构
     *
     * @param query      查询语句
     * @param parameters 查询参数
     * @return 图数据结构
     */
    @Override
    public Map<String, Object> executeGraphQuery(String query, Map<String, Object> parameters) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!initConnection()) {
                return createMap("success", false, "error", "无法连接到Neo4j数据库");
            }
            
            // 执行Cypher查询
            List<Map<String, Object>> data = executeCypher(query, parameters);
            
            // 构建结果
            result.put("success", true);
            result.put("data", data);
            
        } catch (Exception e) {
            log.error("执行Neo4j图查询失败: {}", e.getMessage(), e);
            result = createMap("success", false, "error", e.getMessage());
        } finally {
            closeConnection();
        }
        
        return result;
    }

    /**
     * 获取Neo4j数据库中的关系类型列表（无参数重载版本）
     * @return 关系类型列表
     */
    @Override
    public List<String> getRelationshipTypes() {
        List<String> types = new ArrayList<String>();
        
        try {
            if (!initConnection()) {
                log.error("获取Neo4j关系类型失败: 无法初始化连接");
                return types;
            }
            
            // 创建新会话，确保会话有效
            try (Session session = driver.session()) {
                // 使用Cypher获取所有关系类型
                Result result = session.run("CALL db.relationshipTypes()");
                
                while (result.hasNext()) {
                    Record record = result.next();
                    String type = record.get(0).asString();
                    types.add(type);
                }
                
                // 如果上述方法返回为空，尝试备用方法
                if (types.isEmpty()) {
                    log.info("尝试使用备用方法获取关系类型");
                    Result altResult = session.run("MATCH ()-[r]->() RETURN DISTINCT type(r) AS relType");
                    while (altResult.hasNext()) {
                        Record record = altResult.next();
                        if (!record.get("relType").isNull()) {
                            String type = record.get("relType").asString();
                            types.add(type);
                        }
                    }
                }
                
                log.info("成功获取 {} 个关系类型", types.size());
            }
        } catch (Exception e) {
            log.error("获取Neo4j关系类型列表失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        
        return types;
    }

    /**
     * 获取Neo4j数据库名称列表
     * @return 数据库名称列表
     */
    @Override
    public List<String> getDatabases() {
        List<String> databases = new ArrayList<String>();
        
        try {
            if (!initConnection()) {
                return databases;
            }
            
            // 使用Cypher查询获取所有数据库
            Result result = session.run("SHOW DATABASES");
            
            while (result.hasNext()) {
                Record record = result.next();
                // 提取数据库名称
                String dbName = record.get("name").asString();
                databases.add(dbName);
            }
        } catch (Exception e) {
            // Neo4j版本可能不支持SHOW DATABASES命令（3.5以下版本）
            // 在这种情况下，添加默认数据库名称
            log.warn("获取Neo4j数据库列表失败: {}", e.getMessage());
            databases.add("neo4j"); // 添加默认数据库
        } finally {
            closeConnection();
        }
        
        return databases;
    }

    // 替代下面方法的另一种实现，以解决类型推断问题
    private <T> List<T> createList() {
        return new ArrayList<T>();
    }

    public List<Map<String, Object>> getLabelCounts() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        
        try {
            if (!initConnection()) {
                return result;
            }
            
            // 使用Cypher查询计算每个标签的节点数量
            String cypher = "MATCH (n) RETURN DISTINCT labels(n) AS labels, count(*) AS count";
            Result queryResult = session.run(cypher);
            
            while (queryResult.hasNext()) {
                Record record = queryResult.next();
                Map<String, Object> labelInfo = new HashMap<>();
                
                List<String> labels = new ArrayList<String>();
                for (String label : record.get("labels").asList(Value::asString)) {
                    labels.add(label);
                }
                
                long count = record.get("count").asLong();
                
                labelInfo.put("labels", labels);
                labelInfo.put("count", count);
                result.add(labelInfo);
            }
        } catch (Exception e) {
            log.error("获取Neo4j标签计数失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        
        return result;
    }

    public List<Map<String, Object>> getNodeCounts() {
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        
        try {
            if (!initConnection()) {
                return results;
            }
            
            // 执行Cypher查询计算每种类型节点的数量
            String cypher = "CALL db.labels() YIELD label " +
                            "MATCH (n:`" + "${label}" + "`) " +
                            "RETURN label AS label, count(n) AS count";
            
            Result result = session.run(cypher);
            
            while (result.hasNext()) {
                Record record = result.next();
                
                String label = record.get("label").asString();
                long count = record.get("count").asLong();
                
                Map<String, Object> nodeCount = new HashMap<>();
                nodeCount.put("label", label);
                nodeCount.put("count", count);
                
                results.add(nodeCount);
            }
        } catch (Exception e) {
            log.error("获取Neo4j节点计数失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        
        return results;
    }

    public List<Map<String, Object>> executeCustomQuery(String query) {
        // 明确指定类型参数
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        
        try {
            if (!initConnection()) {
                return results;
            }
            
            Result queryResult = session.run(query);
            
            while (queryResult.hasNext()) {
                Record record = queryResult.next();
                Map<String, Object> row = new HashMap<>();
                
                for (String key : record.keys()) {
                    Value value = record.get(key);
                    
                    // 处理不同类型的值
                    if (value.type().name().equals("NODE")) {
                        row.put(key, convertNode(value.asNode()));
                    } else if (value.type().name().equals("RELATIONSHIP")) {
                        row.put(key, convertRelationship(value.asRelationship()));
                    } else if (value.type().name().equals("PATH")) {
                        row.put(key, convertPath(value.asPath()));
                    } else if (value.type().name().equals("LIST")) {
                        row.put(key, convertList(value.asList()));
                    } else if (value.type().name().equals("MAP")) {
                        row.put(key, value.asMap());
                    } else {
                        row.put(key, value.asObject());
                    }
                }
                
                results.add(row);
            }
        } catch (Exception e) {
            log.error("执行Neo4j自定义查询失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        
        return results;
    }

    public List<Map<String, Object>> getRelationships(String pattern) {
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        
        try {
            if (!initConnection()) {
                return results;
            }
            
            // 构建Cypher查询
            StringBuilder cypher = new StringBuilder();
            cypher.append("MATCH ()-[r");
            
            if (StringUtils.hasText(pattern)) {
                cypher.append(":`").append(pattern).append("`");
            }
            
            cypher.append("]->() ");
            cypher.append("RETURN DISTINCT type(r) AS type, count(*) AS count");
            
            Result result = session.run(cypher.toString());
            
            while (result.hasNext()) {
                Record record = result.next();
                
                String type = record.get("type").asString();
                long count = record.get("count").asLong();
                
                Map<String, Object> relationship = new HashMap<>();
                relationship.put("name", type);
                relationship.put("count", count);
                
                results.add(relationship);
            }
        } catch (Exception e) {
            log.error("获取Neo4j关系类型失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        
        return results;
    }

    /**
     * 执行更新操作的Cypher查询
     * @param query Cypher查询语句
     * @param parameters 查询参数
     * @return 是否执行成功
     */
    @Override
    public boolean executeUpdate(String query, Map<String, Object> parameters) {
        try {
            if (!initConnection()) {
                return false;
            }
            
            Result result;
            if (parameters != null && !parameters.isEmpty()) {
                result = session.run(query, parameters);
            } else {
                result = session.run(query);
            }
            
            // 获取更新操作的统计信息
            SummaryCounters counters = result.consume().counters();
            
            // 操作执行成功，即使没有任何记录受影响也可能是有效操作
            return true;
        } catch (Exception e) {
            log.error("执行Neo4j更新操作失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection();
        }
    }

    public List<Map<String, Object>> getMyRelationships() {
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        
        try {
            // 示例代码
        } catch (Exception e) {
            log.error("错误信息: {}", e.getMessage(), e);
        }
        
        return results;
    }

    public List<Map<String, Object>> getMyNodes() {
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        
        try {
            // 示例代码
        } catch (Exception e) {
            log.error("错误信息: {}", e.getMessage(), e);
        }
        
        return results;
    }

    /**
     * 执行图数据库查询语句并返回单条结果
     *
     * @param query      查询语句
     * @param parameters 查询参数
     * @return 单条查询结果
     */
    @Override
    public Map<String, Object> executeQueryForSingle(String query, Map<String, Object> parameters) {
        List<Map<String, Object>> results = executeQuery(query, parameters);
        if (results != null && !results.isEmpty()) {
            return results.get(0);
        }
        return new HashMap<>();
    }

    @Override
    public Connection getConnection() throws Exception {
        try {
            if (driver == null) {
                initConnection();
            }
            if (driver == null) {
                throw new Exception("无法初始化Neo4j驱动连接");
            }
            return null;
//            return new Neo4jConnection(driver);
        } catch (Exception e) {
            log.error("获取Neo4j连接失败: {}", e.getMessage(), e);
            throw e;
        }
    }
} 