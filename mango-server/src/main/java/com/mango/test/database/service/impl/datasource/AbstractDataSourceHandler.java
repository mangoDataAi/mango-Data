package com.mango.test.database.service.impl.datasource;

import com.mango.test.database.entity.DataSource;
import com.mango.test.database.service.impl.datasource.connection.ConnectionProxy;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 所有数据源处理器的顶层抽象类
 * 提供所有类型数据源处理器共用的方法
 */
@Slf4j
public abstract class AbstractDataSourceHandler {

    protected static final Logger log = LoggerFactory.getLogger(AbstractDataSourceHandler.class);

    public DataSource dataSource;

    public AbstractDataSourceHandler(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 获取数据库连接
     * 所有数据源类型都通过统一的方式返回Connection对象
     * 即使是不支持JDBC的数据源也会返回代理Connection
     */
    public Connection getConnection() throws Exception {
        // 创建适当的原生客户端
        Object nativeClient = createNativeClient();
        
        // 返回连接代理
        return new ConnectionProxy(nativeClient, this);
    }
    
    /**
     * 创建原生客户端对象
     * 子类必须实现此方法以提供适当的客户端对象
     */
    protected abstract Object createNativeClient() throws Exception;
    
    /**
     * 关闭客户端对象
     * 子类应该根据需要重写此方法以正确关闭资源
     */
    public void closeClient(Object client) throws Exception {
        // 默认实现为空
        if (client instanceof AutoCloseable) {
            ((AutoCloseable) client).close();
        }
    }
    
    /**
     * 执行查询操作
     * 子类应该根据需要重写此方法以支持查询
     */
    public List<Map<String, Object>> executeQuery(String query) throws Exception {
        throw new UnsupportedOperationException("此数据源类型不支持查询操作");
    }
    
    /**
     * 执行更新操作
     * 子类应该根据需要重写此方法以支持更新
     */
    public int executeUpdate(String sql) throws Exception {
        throw new UnsupportedOperationException("此数据源类型不支持更新操作");
    }

    /**
     * 获取数据源名称
     */
    public String getDataSourceName() {
        return dataSource != null ? dataSource.getName() : "未知数据源";
    }

    /**
     * 获取数据源类型
     */
    public String getDataSourceType() {
        return dataSource != null ? dataSource.getType() : "unknown";
    }

    /**
     * 测试连接
     *
     * @return 是否连接成功
     */
    public abstract boolean testConnection();

    /**
     * 获取元数据列表（如表、集合、主题等）
     *
     * @param pattern 名称模式
     * @return 元数据列表
     */
    public abstract List<Map<String, Object>> getMetadataList(String pattern);


    /**
     * 获取默认端口
     */
    public abstract String getDefaultPort();


    /**
     * 过滤结果集
     *
     * @param list      原始列表
     * @param pattern   筛选模式
     * @param nameField 名称字段的键
     * @return 过滤后的列表
     */
    protected List<Map<String, Object>> filterListByPattern(List<Map<String, Object>> list, String pattern, String nameField) {
        if (list == null || list.isEmpty() || pattern == null || pattern.trim().isEmpty()) {
            return list;
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : list) {
            String name = (String) item.get(nameField);
            if (name != null && name.contains(pattern)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 包装返回结果
     *
     * @param id   标识
     * @param name 名称
     * @param type 类型
     * @return 包装后的Map
     */
    protected Map<String, Object> wrapResult(String id, String name, String type) {
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("id", id);
        result.put("name", name);
        result.put("type", type);
        return result;
    }

    /**
     * 包装带扩展属性的返回结果
     *
     * @param id         标识
     * @param name       名称
     * @param type       类型
     * @param extraProps 额外属性
     * @return 包装后的Map
     */
    protected Map<String, Object> wrapResult(String id, String name, String type, Map<String, Object> extraProps) {
        Map<String, Object> result = wrapResult(id, name, type);
        if (extraProps != null && !extraProps.isEmpty()) {
            result.putAll(extraProps);
        }
        return result;
    }


    /**
     * 获取驱动类名
     */
    public abstract String getDriverClassName();
    
    /**
     * 关闭处理器资源
     * 所有子类都会继承此方法，用于在使用完处理器后关闭资源
     */
    public void close() {
        try {
            log.debug("关闭数据源处理器: {}", getDataSourceName());
            // 子类可以通过重写这个方法来执行特定的清理操作
        } catch (Exception e) {
            log.error("关闭数据源处理器时发生错误: {}", e.getMessage(), e);
        }
    }
} 