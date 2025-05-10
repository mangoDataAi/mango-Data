package com.mango.test.database.service;

import java.util.List;
import java.util.Map;

/**
 * NoSQL数据库服务接口
 */
public interface NoSQLService {

    /**
     * 获取所有集合
     */
    List<Map<String, Object>> getCollections(String dataSourceId, String searchText) throws Exception;

    /**
     * 获取集合信息
     */
    Map<String, Object> getCollectionInfo(String dataSourceId, String collectionName) throws Exception;

    /**
     * 获取集合Schema
     */
    Map<String, Object> getCollectionSchema(String dataSourceId, String collectionName) throws Exception;

    /**
     * 查询集合数据
     */
    Map<String, Object> queryCollection(String dataSourceId, String collectionName, Map<String, Object> request) throws Exception;

    /**
     * 插入文档
     */
    String insertDocument(String dataSourceId, String collectionName, Map<String, Object> document) throws Exception;

    /**
     * 更新文档
     */
    boolean updateDocument(String dataSourceId, String collectionName, String documentId, Map<String, Object> document) throws Exception;

    /**
     * 删除文档
     */
    boolean deleteDocument(String dataSourceId, String collectionName, String documentId) throws Exception;

    /**
     * 创建集合
     */
    boolean createCollection(String dataSourceId, Map<String, Object> request) throws Exception;

    /**
     * 删除集合
     */
    boolean deleteCollection(String dataSourceId, String collectionName) throws Exception;

    /**
     * 执行命令
     */
    Object executeCommand(String dataSourceId, Map<String, Object> request) throws Exception;

    /**
     * 获取集合索引
     */
    List<Map<String, Object>> getIndices(String dataSourceId, String collectionName) throws Exception;

    /**
     * 创建索引
     */
    boolean createIndex(String dataSourceId, Map<String, Object> request) throws Exception;

    /**
     * 删除索引
     */
    boolean deleteIndex(String dataSourceId, String collectionName, String indexName) throws Exception;
} 