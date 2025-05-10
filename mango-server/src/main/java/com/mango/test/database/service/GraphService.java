package com.mango.test.database.service;

import java.util.List;
import java.util.Map;

/**
 * 图数据库服务接口
 */
public interface GraphService {

    /**
     * 获取图数据库Schema
     * 
     * @param dataSourceId 数据源ID
     * @return 图数据库Schema
     * @throws Exception 异常
     */
    Map<String, Object> getGraphSchema(String dataSourceId) throws Exception;

    /**
     * a执行图数据库查询
     * 
     * @param dataSourceId 数据源ID
     * @param query 查询语句
     * @return 查询结果
     * @throws Exception 异常
     */
    Map<String, Object> executeQuery(String dataSourceId, String query) throws Exception;

    /**
     * 创建节点
     * 
     * @param dataSourceId 数据源ID
     * @param node 节点信息
     * @return 节点ID
     * @throws Exception 异常
     */
    String createNode(String dataSourceId, Map<String, Object> node) throws Exception;

    /**
     * 删除节点
     * 
     * @param dataSourceId 数据源ID
     * @param nodeId 节点ID
     * @throws Exception 异常
     */
    void deleteNode(String dataSourceId, String nodeId) throws Exception;

    /**
     * 创建关系
     * 
     * @param dataSourceId 数据源ID
     * @param relationship 关系信息
     * @return 关系ID
     * @throws Exception 异常
     */
    String createRelationship(String dataSourceId, Map<String, Object> relationship) throws Exception;

    /**
     * 删除关系
     * 
     * @param dataSourceId 数据源ID
     * @param relationshipId 关系ID
     * @throws Exception 异常
     */
    void deleteRelationship(String dataSourceId, String relationshipId) throws Exception;

    /**
     * 获取节点邻居
     * 
     * @param dataSourceId 数据源ID
     * @param request 请求参数
     * @return 邻居节点信息
     * @throws Exception 异常
     */
    Map<String, Object> getNeighbors(String dataSourceId, Map<String, Object> request) throws Exception;

    /**
     * 获取所有节点
     * 
     * @param dataSourceId 数据源ID
     * @param limit 限制条数
     * @return 节点列表
     * @throws Exception 异常
     */
    List<Map<String, Object>> getAllNodes(String dataSourceId, int limit) throws Exception;

    /**
     * 获取节点属性
     * 
     * @param dataSourceId 数据源ID
     * @param nodeLabel 节点标签
     * @return 属性列表
     * @throws Exception 异常
     */
    List<Map<String, Object>> getNodeProperties(String dataSourceId, String nodeLabel) throws Exception;

    /**
     * 获取边属性
     * 
     * @param dataSourceId 数据源ID
     * @param edgeType 边类型
     * @return 属性列表
     * @throws Exception 异常
     */
    List<Map<String, Object>> getEdgeProperties(String dataSourceId, String edgeType) throws Exception;
} 