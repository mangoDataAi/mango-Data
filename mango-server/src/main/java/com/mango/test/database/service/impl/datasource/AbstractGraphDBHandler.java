package com.mango.test.database.service.impl.datasource;

import com.mango.test.database.entity.DataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * 抽象图数据库处理器基类
 */
@Slf4j
public abstract class AbstractGraphDBHandler extends AbstractDataSourceHandler {

    public AbstractGraphDBHandler(DataSource dataSource) {
        super(dataSource);
    }

    /**
     * 执行图数据库查询语句
     *
     * @param query      查询语句
     * @param parameters 查询参数
     * @return 查询结果列表
     */
    public abstract List<Map<String, Object>> executeQuery(String query, Map<String, Object> parameters);

    /**
     * 执行图数据库查询语句并返回单条结果
     *
     * @param query      查询语句
     * @param parameters 查询参数
     * @return 单条查询结果
     */
    public abstract Map<String, Object> executeQueryForSingle(String query, Map<String, Object> parameters);

    /**
     * 执行图数据库更新语句
     *
     * @param query      更新语句
     * @param parameters 更新参数
     * @return 是否更新成功
     */
    public abstract boolean executeUpdate(String query, Map<String, Object> parameters);

    /**
     * 获取图数据库中的数据库列表
     *
     * @return 数据库列表
     */
    public abstract List<String> getDatabases();

    /**
     * 获取图数据库中的标签列表
     *
     * @return 标签列表
     */
    public abstract List<String> getLabels();

    /**
     * 获取图数据库中的关系类型列表
     *
     * @return 关系类型列表
     */
    public abstract List<String> getRelationshipTypes();

    /**
     * 获取指定标签的节点属性
     *
     * @param label 节点标签
     * @return 属性信息列表
     */
    public abstract List<Map<String, Object>> getNodeProperties(String label);

    /**
     * 获取指定类型的边属性
     *
     * @param edgeType 边类型
     * @return 属性信息列表
     */
    public List<Map<String, Object>> getEdgeProperties(String edgeType) {
        log.info("获取边类型[{}]的属性信息", edgeType);
        try {
            // 默认实现：尝试通过查询获取边属性
            // 子类可以根据具体图数据库类型重写此方法
            String query = "MATCH ()-[r:" + edgeType + "]->() RETURN properties(r) as properties LIMIT 1";
            List<Map<String, Object>> results = executeQuery(query, new HashMap<>());
            
            if (results != null && !results.isEmpty()) {
                Map<String, Object> firstResult = results.get(0);
                Map<String, Object> properties = (Map<String, Object>) firstResult.get("properties");
                
                if (properties != null) {
                    List<Map<String, Object>> propertyList = new ArrayList<>();
                    properties.forEach((key, value) -> {
                        Map<String, Object> propertyInfo = new HashMap<>();
                        propertyInfo.put("name", key);
                        propertyInfo.put("type", value != null ? value.getClass().getSimpleName() : "String");
                        propertyList.add(propertyInfo);
                    });
                    return propertyList;
                }
            }
            
            // 如果没有结果，返回基本属性
            List<Map<String, Object>> defaultProperties = new ArrayList<>();
            Map<String, Object> idProperty = new HashMap<>();
            idProperty.put("name", "id");
            idProperty.put("type", "String");
            defaultProperties.add(idProperty);
            
            Map<String, Object> typeProperty = new HashMap<>();
            typeProperty.put("name", "type");
            typeProperty.put("type", "String");
            defaultProperties.add(typeProperty);
            
            return defaultProperties;
        } catch (Exception e) {
            log.error("获取边属性失败: {}", e.getMessage(), e);
            // 返回默认的边属性
            List<Map<String, Object>> defaultProperties = new ArrayList<>();
            Map<String, Object> idProperty = new HashMap<>();
            idProperty.put("name", "id");
            idProperty.put("type", "String");
            defaultProperties.add(idProperty);
            
            Map<String, Object> typeProperty = new HashMap<>();
            typeProperty.put("name", "type");
            typeProperty.put("type", "String");
            defaultProperties.add(typeProperty);
            
            return defaultProperties;
        }
    }

    /**
     * 执行图查询，返回图数据结构
     *
     * @param query      查询语句
     * @param parameters 查询参数
     * @return 图数据结构
     */
    public abstract Map<String, Object> executeGraphQuery(String query, Map<String, Object> parameters);

    /**
     * 获取指定标签的节点列表
     *
     * @param label 节点标签
     * @param limit 限制条数
     * @return 节点列表
     */
    public abstract List<Map<String, Object>> getNodesByLabel(String label, int limit);

    /**
     * 创建原生客户端对象
     * 图数据库的子类需要实现此方法
     */
    @Override
    protected Object createNativeClient() throws Exception {
        if (!testConnection()) {
            throw new Exception("无法连接到图数据库：" + getDataSourceName());
        }
        // 子类需要重写此方法来创建适当的客户端
        return new Object(); // 默认返回空对象
    }

    /**
     * 执行图数据库查询
     */
    @Override
    public List<Map<String, Object>> executeQuery(String query) throws Exception {
        return executeQuery(query, new HashMap<>());
    }
    
    /**
     * 执行图数据库更新
     */
    @Override
    public int executeUpdate(String sql) throws Exception {
        boolean result = executeUpdate(sql, new HashMap<>());
        return result ? 1 : 0;
    }

} 