package com.mango.test.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mango.test.database.entity.ColumnInfo;
import com.mango.test.database.entity.DataSource;
import com.mango.test.database.entity.FileField;
import com.mango.test.dto.TableStructureDTO;
import com.mango.test.vo.R;
import org.springframework.transaction.TransactionStatus;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

public interface DataSourceService extends IService<DataSource> {

    /**
     * 测试数据源连接
     */
    boolean testConnection(DataSource dataSource) throws Exception;

    /**
     * 获取数据库表列表
     */
    List<Map<String, Object>> getTables(String id) throws Exception;

    /**
     * 获取表列表详情
     */
    List<Map<String, Object>> getTableList(String id, String types, String searchText) throws Exception;

    List<Map<String, Object>> executeQuery(String id, String sql) throws Exception;

    Connection getConnection(String sourceId);

    /**
     * 检查表是否存在
     */
    boolean isTableExists(String tableName);

    /**
     * 开启事务
     */
    TransactionStatus beginTransaction();

    /**
     * 提交事务
     */
    void commit(TransactionStatus status);

    /**
     * 回滚事务
     */
    void rollback(TransactionStatus status);

    /**
     * 创建表
     */
    void createTable(String tableName, String tableComment, List<FileField> fields);

    /**
     * 批量插入数据
     */
    void batchInsertData(String tableName, List<FileField> fields, List<Map<String, Object>> data);


    R<List<ColumnInfo>> getTableColumns(String dataSourceId, String tableName);

    List<Map<String, Object>> executeQuery(String url, String username, String password, String sql);

    /**
     * 获取数据源元数据
     * 通用方法，根据数据源类型和请求的元数据类型，获取相应的元数据
     * @param dataSource 数据源对象
     * @param type 元数据类型(tables, collections, topics等)
     * @param pattern 名称模式（可用于筛选）
     * @return 元数据列表
     */
    List<Map<String, Object>> getDataSourceMetadata(DataSource dataSource, String type, String pattern);
}
