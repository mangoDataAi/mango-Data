package com.mango.test.database.service.impl.datasource.handlers.nosql;

import com.mango.test.database.entity.DataSource;
import com.mango.test.database.service.impl.datasource.AbstractNoSQLHandler;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * MongoDB数据源处理器
 */
@Slf4j
public class MongoDBHandler extends AbstractNoSQLHandler {
    
    private static final String MONGODB_DRIVER = "com.mongodb.client.MongoClient";
    private MongoClient mongoClient;
    private MongoDatabase currentDatabase;
    private String currentDatabaseName;
    
    public MongoDBHandler(DataSource dataSource) {
        super(dataSource);
    }
    
    /**
     * 初始化MongoDB连接
     */
    private synchronized boolean initConnection() {
        if (mongoClient != null) {
            return true;
        }
        
        try {
            String host = dataSource.getHost();
            int port = StringUtils.hasText(dataSource.getPort()) ? 
                Integer.parseInt(dataSource.getPort()) : 
                Integer.parseInt(getDefaultPort());
            
            MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder()
                .applyToSocketSettings(builder -> 
                    builder.connectTimeout(5000, TimeUnit.MILLISECONDS)
                           .readTimeout(10000, TimeUnit.MILLISECONDS))
                .applyToClusterSettings(builder -> 
                    builder.serverSelectionTimeout(5000, TimeUnit.MILLISECONDS)
                           .hosts(Collections.singletonList(new ServerAddress(host, port))));
            
            // 判断是否需要认证
            if (StringUtils.hasText(dataSource.getUsername()) && 
                StringUtils.hasText(dataSource.getPassword())) {
                
                String authDb = StringUtils.hasText(dataSource.getDbName()) ? 
                    dataSource.getDbName() : "admin";
                
                MongoCredential credential = MongoCredential.createCredential(
                    dataSource.getUsername(),
                    authDb,
                    dataSource.getPassword().toCharArray()
                );
                
                settingsBuilder.credential(credential);
            }
            
            mongoClient = MongoClients.create(settingsBuilder.build());
            
            // 设置默认数据库
            currentDatabaseName = StringUtils.hasText(dataSource.getDbName()) ? 
                dataSource.getDbName() : "admin";
            
            currentDatabase = mongoClient.getDatabase(currentDatabaseName);
            
            return true;
        } catch (Exception e) {
            log.error("初始化MongoDB连接失败: {}", e.getMessage(), e);
            closeConnection();
            return false;
        }
    }
    
    /**
     * 关闭MongoDB连接
     */
    private synchronized void closeConnection() {
        if (mongoClient != null) {
            try {
                mongoClient.close();
            } catch (Exception e) {
                log.error("关闭MongoDB连接失败: {}", e.getMessage(), e);
            } finally {
                mongoClient = null;
                currentDatabase = null;
            }
        }
    }
    
    @Override
    public boolean testConnection() {
        try {
            if (initConnection()) {
                // 验证连接是否可用，尝试列出数据库
                mongoClient.listDatabaseNames().first();
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("测试MongoDB连接失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection();
        }
    }
    
    @Override
    public List<Map<String, Object>> getCollections(String pattern) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            if (!initConnection()) {
                return result;
            }
            
            MongoIterable<String> collectionNames = currentDatabase.listCollectionNames();
            try (MongoCursor<String> cursor = collectionNames.iterator()) {
                while (cursor.hasNext()) {
                    String collectionName = cursor.next();
                    if (pattern == null || pattern.isEmpty() || collectionName.contains(pattern)) {
                        Map<String, Object> collectionInfo = new HashMap<>();
                        collectionInfo.put("id", collectionName);
                        collectionInfo.put("name", collectionName);
                        collectionInfo.put("type", "collection");
                        
                        result.add(collectionInfo);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取MongoDB集合列表失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        return result;
    }
    
    /**
     * 获取MongoDB集合列表
     * 实现特定类型的集合获取逻辑
     * @param pattern 集合名称模式（可用于筛选）
     * @return 集合列表
     */
    @Override
    public List<Map<String, Object>> getSpecificCollections(String pattern) {
        return getCollections(pattern);
    }
    
    @Override
    public List<Map<String, Object>> getIndexes(String collection) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            if (!initConnection()) {
                return result;
            }
            
            MongoCollection<Document> coll = currentDatabase.getCollection(collection);
            for (Document indexDoc : coll.listIndexes()) {
                Map<String, Object> index = new HashMap<>();
                index.put("name", indexDoc.getString("name"));
                index.put("keys", indexDoc.get("key"));
                index.put("unique", indexDoc.getBoolean("unique", false));
                
                result.add(index);
            }
        } catch (Exception e) {
            log.error("获取MongoDB索引列表失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        return result;
    }
    
    @Override
    public List<Map<String, Object>> getFields(String collection) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            if (!initConnection()) {
                return result;
            }
            
            MongoCollection<Document> coll = currentDatabase.getCollection(collection);
            // 获取一个文档样本来推断字段结构
            Document sampleDoc = coll.find().limit(1).first();
            
            if (sampleDoc != null) {
                for (String fieldName : sampleDoc.keySet()) {
                    Map<String, Object> field = new HashMap<>();
                    field.put("name", fieldName);
                    
                    Object value = sampleDoc.get(fieldName);
                    String type = value == null ? "Null" : value.getClass().getSimpleName();
                    if (value instanceof ObjectId) {
                        type = "ObjectId";
                    }
                    
                    field.put("type", type);
                    result.add(field);
                }
            }
        } catch (Exception e) {
            log.error("获取MongoDB字段列表失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        return result;
    }
    
    @Override
    public List<Map<String, Object>> query(String collection, Map<String, Object> query, int limit) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            if (!initConnection()) {
                return result;
            }
            
            MongoCollection<Document> coll = currentDatabase.getCollection(collection);
            
            Document queryDoc = new Document();
            if (query != null && !query.isEmpty()) {
                for (Map.Entry<String, Object> entry : query.entrySet()) {
                    queryDoc.append(entry.getKey(), entry.getValue());
                }
            }
            
            try (MongoCursor<Document> cursor = coll.find(queryDoc).limit(limit).iterator()) {
                while (cursor.hasNext()) {
                    Document doc = cursor.next();
                    Map<String, Object> docMap = documentToMap(doc);
                    result.add(docMap);
                }
            }
        } catch (Exception e) {
            log.error("执行MongoDB查询失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        return result;
    }
    
    private Map<String, Object> documentToMap(Document doc) {
        Map<String, Object> map = new HashMap<>();
        for (String key : doc.keySet()) {
            Object value = doc.get(key);
            if (value instanceof Document) {
                map.put(key, documentToMap((Document) value));
            } else if (value instanceof List) {
                List<Object> list = new ArrayList<>();
                for (Object item : (List<?>) value) {
                    if (item instanceof Document) {
                        list.add(documentToMap((Document) item));
                    } else if (item instanceof ObjectId) {
                        // 处理列表中的ObjectId
                        Map<String, String> oidMap = new HashMap<>();
                        oidMap.put("$oid", ((ObjectId) item).toString());
                        list.add(oidMap);
                    } else {
                        list.add(item);
                    }
                }
                map.put(key, list);
            } else if (value instanceof ObjectId) {
                // 将MongoDB的ObjectId转换为标准格式 { $oid: "..." }
                // 这是MongoDB客户端通用的序列化方式
                if (key.equals("_id")) {
                    Map<String, String> oidMap = new HashMap<>();
                    oidMap.put("$oid", ((ObjectId) value).toString());
                    map.put(key, oidMap);
                } else {
                    map.put(key, ((ObjectId) value).toString());
                }
            } else {
                map.put(key, value);
            }
        }
        return map;
    }
    
    @Override
    public List<Map<String, Object>> query(String collection, String queryString, int limit) {
        try {
            // 解析查询字符串为查询对象
            Map<String, Object> queryMap = new HashMap<>();
            // 简单处理，实际项目中应使用更复杂的解析逻辑
            if (queryString != null && !queryString.isEmpty()) {
                String[] parts = queryString.split(",");
                for (String part : parts) {
                    String[] keyValue = part.split(":");
                    if (keyValue.length == 2) {
                        queryMap.put(keyValue[0].trim(), keyValue[1].trim());
                    }
                }
            }
            
            return query(collection, queryMap, limit);
        } catch (Exception e) {
            log.error("执行MongoDB查询失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public boolean insert(String collection, Map<String, Object> data) {
        try {
            if (!initConnection()) {
                return false;
            }
            
            MongoCollection<Document> coll = currentDatabase.getCollection(collection);
            Document doc = mapToDocument(data);
            coll.insertOne(doc);
            
            return true;
        } catch (Exception e) {
            log.error("向MongoDB集合插入数据失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection();
        }
    }
    
    private Document mapToDocument(Map<String, Object> map) {
        Document doc = new Document();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> valueMap = (Map<String, Object>) value;
                doc.append(entry.getKey(), mapToDocument(valueMap));
            } else if (value instanceof List) {
                List<Object> list = new ArrayList<>();
                for (Object item : (List<?>) value) {
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> itemMap = (Map<String, Object>) item;
                        list.add(mapToDocument(itemMap));
                    } else {
                        list.add(item);
                    }
                }
                doc.append(entry.getKey(), list);
            } else {
                doc.append(entry.getKey(), value);
            }
        }
        return doc;
    }
    
    @Override
    public int batchInsert(String collection, List<Map<String, Object>> dataList) {
        try {
            if (!initConnection() || dataList == null || dataList.isEmpty()) {
                return 0;
            }
            
            MongoCollection<Document> coll = currentDatabase.getCollection(collection);
            
            List<Document> documents = new ArrayList<>();
            for (Map<String, Object> data : dataList) {
                documents.add(mapToDocument(data));
            }
            
            coll.insertMany(documents);
            return documents.size();
        } catch (Exception e) {
            log.error("向MongoDB集合批量插入数据失败: {}", e.getMessage(), e);
            return 0;
        } finally {
            closeConnection();
        }
    }
    
    @Override
    public int update(String collection, Map<String, Object> query, Map<String, Object> data) {
        try {
            if (!initConnection()) {
                return 0;
            }
            
            MongoCollection<Document> coll = currentDatabase.getCollection(collection);
            
            Document queryDoc = mapToDocument(query);
            Document updateDoc = new Document("$set", mapToDocument(data));
            
            com.mongodb.client.result.UpdateResult result = coll.updateMany(queryDoc, updateDoc);
            return (int) result.getModifiedCount();
        } catch (Exception e) {
            log.error("更新MongoDB集合数据失败: {}", e.getMessage(), e);
            return 0;
        } finally {
            closeConnection();
        }
    }
    
    @Override
    public int delete(String collection, Map<String, Object> query) {
        try {
            if (!initConnection()) {
                return 0;
            }
            
            MongoCollection<Document> coll = currentDatabase.getCollection(collection);
            
            Document queryDoc = mapToDocument(query);
            com.mongodb.client.result.DeleteResult result = coll.deleteMany(queryDoc);
            
            return (int) result.getDeletedCount();
        } catch (Exception e) {
            log.error("删除MongoDB集合数据失败: {}", e.getMessage(), e);
            return 0;
        } finally {
            closeConnection();
        }
    }
    
    @Override
    public boolean createCollection(String collection, Map<String, Object> options) {
        try {
            if (!initConnection()) {
                return false;
            }
            
            currentDatabase.createCollection(collection);
            return true;
        } catch (Exception e) {
            log.error("创建MongoDB集合失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection();
        }
    }
    
    @Override
    public boolean dropCollection(String collection) {
        try {
            if (!initConnection()) {
                return false;
            }
            
            MongoCollection<Document> coll = currentDatabase.getCollection(collection);
            coll.drop();
            
            return true;
        } catch (Exception e) {
            log.error("删除MongoDB集合失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection();
        }
    }
    
    @Override
    public boolean createIndex(String collection, Map<String, Object> indexDefinition) {
        try {
            if (!initConnection() || indexDefinition == null || !indexDefinition.containsKey("field")) {
                return false;
            }
            
            MongoCollection<Document> coll = currentDatabase.getCollection(collection);
            
            String field = (String) indexDefinition.get("field");
            boolean unique = indexDefinition.containsKey("unique") && 
                             (boolean) indexDefinition.get("unique");
            
            IndexOptions options = new IndexOptions().unique(unique);
            
            if (indexDefinition.containsKey("name")) {
                options.name((String) indexDefinition.get("name"));
            }
            
            coll.createIndex(Indexes.ascending(field), options);
            return true;
        } catch (Exception e) {
            log.error("为MongoDB集合创建索引失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection();
        }
    }
    
    @Override
    public boolean dropIndex(String collection, String indexName) {
        try {
            if (!initConnection()) {
                return false;
            }
            
            MongoCollection<Document> coll = currentDatabase.getCollection(collection);
            coll.dropIndex(indexName);
            
            return true;
        } catch (Exception e) {
            log.error("删除MongoDB集合上的索引失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection();
        }
    }
    
    @Override
    public Map<String, Object> getCollectionInfo(String collection) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (!initConnection()) {
                return result;
            }
            
            MongoCollection<Document> coll = currentDatabase.getCollection(collection);
            
            Document stats = currentDatabase.runCommand(new Document("collStats", collection));
            
            result.put("name", collection);
            result.put("documentCount", stats.getInteger("count", 0));
            result.put("size", stats.getInteger("size", 0));
            result.put("storageSize", stats.getInteger("storageSize", 0));
            result.put("indexCount", stats.getInteger("nindexes", 0));
            result.put("indexSize", stats.getInteger("totalIndexSize", 0));
            result.put("isCapped", stats.getBoolean("capped", false));
            result.put("sharded", stats.containsKey("shards"));
        } catch (Exception e) {
            log.error("获取MongoDB集合信息失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        return result;
    }
    
    @Override
    public List<Map<String, Object>> getDatabases() {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            if (!initConnection()) {
                return result;
            }
            
            for (String dbName : mongoClient.listDatabaseNames()) {
                Document stats = mongoClient.getDatabase(dbName)
                    .runCommand(new Document("dbStats", 1));
                
                Map<String, Object> dbInfo = new HashMap<>();
                dbInfo.put("id", dbName);
                dbInfo.put("name", dbName);
                dbInfo.put("sizeOnDisk", stats.getInteger("storageSize", 0));
                
                result.add(dbInfo);
            }
        } catch (Exception e) {
            log.error("获取MongoDB数据库列表失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        return result;
    }
    
    @Override
    public boolean switchDatabase(String database) {
        try {
            if (!initConnection()) {
                return false;
            }
            
            // 直接切换，不需要重新连接
            currentDatabase = mongoClient.getDatabase(database);
            currentDatabaseName = database;
            
            return true;
        } catch (Exception e) {
            log.error("切换MongoDB数据库失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection();
        }
    }
    
    @Override
    public String getCurrentDatabase() {
        try {
            if (initConnection()) {
                return currentDatabaseName;
            }
        } catch (Exception e) {
            log.error("获取当前MongoDB数据库失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        return null;
    }
    
    @Override
    public Map<String, Object> getCollectionStats(String collection) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (!initConnection()) {
                return result;
            }
            
            Document stats = currentDatabase.runCommand(new Document("collStats", collection));
            
            result.put("name", collection);
            result.put("documentCount", stats.getInteger("count", 0));
            result.put("size", stats.getInteger("size", 0));
            result.put("storageSize", stats.getInteger("storageSize", 0));
            result.put("avgObjSize", stats.get("avgObjSize", 0.0));
            result.put("indexCount", stats.getInteger("nindexes", 0));
            result.put("indexSize", stats.getInteger("totalIndexSize", 0));
            result.put("paddingFactor", stats.get("paddingFactor", 1.0));
            
            if (stats.containsKey("wiredTiger")) {
                Document wt = (Document) stats.get("wiredTiger");
                if (wt.containsKey("creationTime")) {
                    result.put("creationTime", wt.get("creationTime"));
                }
            }
        } catch (Exception e) {
            log.error("获取MongoDB集合统计信息失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        return result;
    }
    
    @Override
    public Map<String, Object> getDatabaseStatus() {
        Map<String, Object> result = new HashMap<>();
        try {
            if (!initConnection()) {
                return result;
            }
            
            Document serverStatus = mongoClient.getDatabase("admin")
                .runCommand(new Document("serverStatus", 1));
            
            result.put("version", serverStatus.get("version", "未知"));
            
            Document uptime = (Document) serverStatus.get("uptime");
            if (uptime != null) {
                result.put("uptime", uptime.getInteger("secs", 0));
            }
            
            Document connections = (Document) serverStatus.get("connections");
            if (connections != null) {
                result.put("connections", connections.getInteger("current", 0));
                result.put("activeConnections", connections.getInteger("active", 0));
            }
            
            Document memory = (Document) serverStatus.get("mem");
            if (memory != null) {
                Map<String, Object> memInfo = new HashMap<>();
                memInfo.put("resident", memory.getInteger("resident", 0));
                result.put("memory", memInfo);
            }
            
            Document repl = (Document) serverStatus.get("repl");
            if (repl != null) {
                Map<String, Object> replInfo = new HashMap<>();
                replInfo.put("ismaster", repl.getBoolean("ismaster", false));
                result.put("replication", replInfo);
            }
            
            Document sharding = (Document) serverStatus.get("sharding");
            if (sharding != null) {
                Map<String, Object> shardingInfo = new HashMap<>();
                shardingInfo.put("enabled", true);
                result.put("sharding", shardingInfo);
            } else {
                Map<String, Object> shardingInfo = new HashMap<>();
                shardingInfo.put("enabled", false);
                result.put("sharding", shardingInfo);
            }
        } catch (Exception e) {
            log.error("获取MongoDB数据库状态失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        return result;
    }
    
    @Override
    public Map<String, Object> getQueryPlan(String collection, Map<String, Object> query) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (!initConnection()) {
                return result;
            }
            
            Document queryDoc = mapToDocument(query);
            
            Document explainCommand = new Document("explain", 
                new Document("find", collection)
                    .append("filter", queryDoc)
            ).append("verbosity", "executionStats");
            
            Document explainResult = currentDatabase.runCommand(explainCommand);
            
            if (explainResult.containsKey("executionStats")) {
                Document executionStats = (Document) explainResult.get("executionStats");
                
                result.put("executionTimeMillis", executionStats.getInteger("executionTimeMillis", 0));
                result.put("totalKeysExamined", executionStats.getInteger("totalKeysExamined", 0));
                result.put("totalDocsExamined", executionStats.getInteger("totalDocsExamined", 0));
                result.put("nReturned", executionStats.getInteger("nReturned", 0));
                
                List<String> indexesUsed = new ArrayList<>();
                Document queryPlanner = (Document) explainResult.get("queryPlanner");
                if (queryPlanner != null && queryPlanner.containsKey("winningPlan")) {
                    Document winningPlan = (Document) queryPlanner.get("winningPlan");
                    extractIndexName(winningPlan, indexesUsed);
                }
                
                result.put("indexesUsed", indexesUsed);
            }
        } catch (Exception e) {
            log.error("获取MongoDB查询计划失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        return result;
    }
    
    private void extractIndexName(Document plan, List<String> indexesUsed) {
        if (plan.containsKey("inputStage")) {
            extractIndexName((Document) plan.get("inputStage"), indexesUsed);
        }
        
        if (plan.containsKey("indexName")) {
            indexesUsed.add(plan.getString("indexName"));
        }
        
        if (plan.containsKey("inputStages")) {
            @SuppressWarnings("unchecked")
            List<Document> stages = (List<Document>) plan.get("inputStages");
            for (Document stage : stages) {
                extractIndexName(stage, indexesUsed);
            }
        }
    }
    
    @Override
    public byte[] exportData(String collection, Map<String, Object> query, String format) {
        try {
            if (!initConnection()) {
                return new byte[0];
            }
            
            List<Map<String, Object>> data = query(collection, query, 1000);
            
            if ("json".equalsIgnoreCase(format)) {
                return convertToJson(data).getBytes();
            } else {
                // 默认使用JSON格式
                return convertToJson(data).getBytes();
            }
        } catch (Exception e) {
            log.error("导出MongoDB数据失败: {}", e.getMessage(), e);
            return new byte[0];
        } finally {
            closeConnection();
        }
    }
    
    private String convertToJson(List<Map<String, Object>> data) {
        // 简单实现，实际项目中可以使用更高效的JSON库
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < data.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(convertMapToJson(data.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }
    
    private String convertMapToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            
            sb.append("\"").append(entry.getKey()).append("\":");
            
            Object value = entry.getValue();
            if (value == null) {
                sb.append("null");
            } else if (value instanceof String) {
                sb.append("\"").append(((String) value).replace("\"", "\\\"")).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else if (value instanceof Date) {
                sb.append("\"").append(value).append("\"");
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> valueMap = (Map<String, Object>) value;
                sb.append(convertMapToJson(valueMap));
            } else if (value instanceof List) {
                sb.append(convertListToJson((List<?>) value));
            } else {
                sb.append("\"").append(value.toString()).append("\"");
            }
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    private String convertListToJson(List<?> list) {
        StringBuilder sb = new StringBuilder("[");
        
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            
            Object value = list.get(i);
            if (value == null) {
                sb.append("null");
            } else if (value instanceof String) {
                sb.append("\"").append(((String) value).replace("\"", "\\\"")).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else if (value instanceof Date) {
                sb.append("\"").append(value).append("\"");
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> valueMap = (Map<String, Object>) value;
                sb.append(convertMapToJson(valueMap));
            } else if (value instanceof List) {
                sb.append(convertListToJson((List<?>) value));
            } else {
                sb.append("\"").append(value.toString()).append("\"");
            }
        }
        
        sb.append("]");
        return sb.toString();
    }
    
    @Override
    public Map<String, Object> importData(String collection, byte[] data, String format) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (!initConnection()) {
                result.put("success", false);
                result.put("error", "无法连接到MongoDB");
                return result;
            }
            
            // 这里需要实现实际的导入逻辑
            // 简单示例，实际项目中需要更完善的实现
            String jsonStr = new String(data);
            
            // 简单处理，假设数据是JSON数组格式
            if (jsonStr.startsWith("[") && jsonStr.endsWith("]")) {
                // 移除首尾的[]
                jsonStr = jsonStr.substring(1, jsonStr.length() - 1);
                
                // 分割成多个JSON对象
                List<String> jsonObjects = splitJsonArray(jsonStr);
                
                List<Document> documents = new ArrayList<>();
                for (String json : jsonObjects) {
                    // 简单解析，实际项目中应使用更可靠的JSON解析器
                    Document doc = Document.parse(json);
                    documents.add(doc);
                }
                
                if (!documents.isEmpty()) {
                    MongoCollection<Document> coll = currentDatabase.getCollection(collection);
                    coll.insertMany(documents);
                    
                    result.put("success", true);
                    result.put("imported", documents.size());
                    result.put("errors", 0);
                } else {
                    result.put("success", false);
                    result.put("error", "没有有效的文档");
                }
            } else {
                result.put("success", false);
                result.put("error", "无效的JSON数据格式");
            }
        } catch (Exception e) {
            log.error("导入MongoDB数据失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        } finally {
            closeConnection();
        }
        return result;
    }
    
    private List<String> splitJsonArray(String jsonArrayContent) {
        List<String> result = new ArrayList<>();
        
        int depth = 0;
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean escaped = false;
        
        for (char c : jsonArrayContent.toCharArray()) {
            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                current.append(c);
                escaped = true;
                continue;
            }
            
            if (c == '"') {
                inQuotes = !inQuotes;
            }
            
            if (!inQuotes) {
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                }
                
                if (c == ',' && depth == 0) {
                    // 一个JSON对象结束
                    result.add(current.toString().trim());
                    current = new StringBuilder();
                    continue;
                }
            }
            
            current.append(c);
        }
        
        // 添加最后一个对象
        String lastObject = current.toString().trim();
        if (!lastObject.isEmpty()) {
            result.add(lastObject);
        }
        
        return result;
    }
    
    @Override
    public Map<String, Object> executeCommand(String command) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (!initConnection()) {
                result.put("ok", 0.0);
                result.put("error", "无法连接到MongoDB");
                return result;
            }
            
            // 解析命令
            Document commandDoc;
            try {
                commandDoc = Document.parse(command);
            } catch (Exception e) {
                result.put("ok", 0.0);
                result.put("error", "命令解析失败: " + e.getMessage());
                return result;
            }
            
            // 执行命令
            Document responseDoc = currentDatabase.runCommand(commandDoc);
            
            // 转换结果
            result = documentToMap(responseDoc);
        } catch (Exception e) {
            log.error("执行MongoDB命令失败: {}", e.getMessage(), e);
            result.put("ok", 0.0);
            result.put("error", e.getMessage());
        } finally {
            closeConnection();
        }
        return result;
    }

    @Override
    public String getDefaultPort() {
        return "27017";
    }

    @Override
    public String getDriverClassName() {
        return MONGODB_DRIVER;
    }

    /**
     * 获取集合中文档的确切数量
     * @param collection 集合名称
     * @param queryString 查询字符串 (可选)
     * @return 文档数量
     */
    @Override
    public long count(String collection, String queryString) throws Exception {
        try {
            if (!initConnection()) {
                return 0;
            }
            
            MongoCollection<Document> coll = currentDatabase.getCollection(collection);
            
            if (queryString != null && !queryString.isEmpty()) {
                // 尝试将查询字符串解析为JSON
                try {
                    Document queryDoc = Document.parse(queryString);
                    return coll.countDocuments(queryDoc);
                } catch (Exception e) {
                    // 如果无法解析为JSON，则使用文本搜索
                    Document textQuery = new Document("$text", new Document("$search", queryString));
                    return coll.countDocuments(textQuery);
                }
            } else {
                return coll.countDocuments();
            }
        } catch (Exception e) {
            log.error("计算MongoDB集合 {} 文档数量失败: {}", collection, e.getMessage(), e);
            throw e;
        } finally {
            closeConnection();
        }
    }
    
    /**
     * 获取集合中文档的确切数量
     * @param collection 集合名称
     * @param query 查询条件 (可选)
     * @return 文档数量
     */
    @Override
    public long count(String collection, Map<String, Object> query) throws Exception {
        try {
            if (!initConnection()) {
                return 0;
            }
            
            MongoCollection<Document> coll = currentDatabase.getCollection(collection);
            
            if (query != null && !query.isEmpty()) {
                Document queryDoc = new Document();
                for (Map.Entry<String, Object> entry : query.entrySet()) {
                    queryDoc.append(entry.getKey(), entry.getValue());
                }
                return coll.countDocuments(queryDoc);
            } else {
                return coll.countDocuments();
            }
        } catch (Exception e) {
            log.error("计算MongoDB集合 {} 文档数量失败: {}", collection, e.getMessage(), e);
            throw e;
        } finally {
            closeConnection();
        }
    }

    /**
     * 获取MongoDB客户端连接
     * @return MongoDB客户端连接
     */
    private MongoClient getMongoClient() {
        if (mongoClient == null) {
            initConnection();
        }
        return mongoClient;
    }

    /**
     * 更新文档
     * @param collection 集合名称
     * @param id 文档ID (字符串形式的ObjectId或数字ID)
     * @param data 更新数据
     * @return 是否成功
     */
    public boolean updateDocument(String collection, String id, Map<String, Object> data) {
        try {
            if (!initConnection()) {
                return false;
            }
            
            MongoCollection<Document> coll = currentDatabase.getCollection(collection);
            
            // 创建一个条件，匹配ID
            Document filter;
            try {
                // 首先尝试将ID解析为数字（处理timestamp格式的ID）
                if (id.matches("\\d+")) {
                    long numericId = Long.parseLong(id);
                    log.info("使用数字ID查询文档: {}", numericId);
                    // 尝试两种可能的查询方式
                    List<Document> docs = new ArrayList<>();
                    // 1. 直接使用数字作为_id
                    docs.addAll(coll.find(new Document("_id", numericId)).into(new ArrayList<>()));
                    // 2. 查找带有timestamp字段的文档
                    docs.addAll(coll.find(new Document("_id.timestamp", numericId)).into(new ArrayList<>()));
                    
                    if (docs.isEmpty()) {
                        log.warn("未找到ID为{}的文档", id);
                        return false;
                    }
                    
                    // 使用找到的第一个文档的_id作为过滤条件
                    filter = new Document("_id", docs.get(0).get("_id"));
                } else {
                    // 尝试将ID解析为ObjectId
                    try {
                        ObjectId objectId = new ObjectId(id);
                        filter = new Document("_id", objectId);
                    } catch (IllegalArgumentException e) {
                        // 如果不是有效的ObjectId，则直接使用字符串ID
                        filter = new Document("_id", id);
                    }
                }
            } catch (NumberFormatException e) {
                // 如果无法解析为数字，尝试ObjectId
                try {
                    ObjectId objectId = new ObjectId(id);
                    filter = new Document("_id", objectId);
                } catch (IllegalArgumentException ex) {
                    // 最后尝试使用字符串ID
                    filter = new Document("_id", id);
                }
            }
            
            // 从更新数据中移除_id字段以避免尝试更新不可变字段
            Map<String, Object> updateData = new HashMap<>(data);
            updateData.remove("_id");
            
            Document updateDoc = new Document("$set", mapToDocument(updateData));
            
            log.info("使用过滤条件更新文档: {}", filter.toJson());
            com.mongodb.client.result.UpdateResult result = coll.updateOne(filter, updateDoc);
            boolean success = result.getModifiedCount() > 0 || result.getMatchedCount() > 0;
            log.info("文档更新结果: 匹配数={}, 修改数={}, 成功={}", 
                    result.getMatchedCount(), result.getModifiedCount(), success);
            return success;
        } catch (Exception e) {
            log.error("更新MongoDB文档失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection();
        }
    }
} 