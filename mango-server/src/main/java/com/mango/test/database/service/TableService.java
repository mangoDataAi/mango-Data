package com.mango.test.database.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mango.test.database.entity.Table;
import com.mango.test.database.entity.TableField;
import com.mango.test.dto.TableDTO;
import com.mango.test.dto.TableFieldDTO;
import com.mango.test.dto.TableQueryDTO;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface TableService extends IService<Table> {
    /**
     * 创建表
     */
    void createTable(String groupId, TableDTO tableDTO);

    /**
     * 更新表
     */
    void updateTable(String id, TableDTO tableDTO);

    /**
     * 删除表
     */
    void deleteTable(String id);

    /**
     * 获取表详情
     */
    TableDTO getTableDetail(String id);

    /**
     * 获取分组下的表列表
     */
    List<Table> getTablesByGroup(String groupId);

    /**
     * 执行SQL创建表
     */
    void createTableBySQL(String groupId, String sql);

    /**
     * 引用已有表
     */
    void referenceExistingTables(String groupId, String sourceId, List<String> tableNames) ;

    /**
     * 物化表到数据库
     */
    void materializeTable(String tableId, String sourceId);

    /**
     * 生成表模板
     * @param tableId 表ID
     * @param format 格式(excel/csv/txt/dmp/sql)
     * @param response HTTP响应对象
     */
    void generateTemplate(String tableId, String format, HttpServletResponse response);

    /**
     * 生成空模板
     * @param format 格式(excel/csv/txt/dmp/sql)
     * @param response HTTP响应对象
     */
    void generateEmptyTemplate(String format, HttpServletResponse response);

    /**
     * 批量物化表
     * @param targetSourceId 目标数据源ID
     * @param tableIds 要物化的表ID列表
     */
    void materializeTables(String targetSourceId, List<String> tableIds);

    /**
     * 获取分组下的表列表(分页)
     */
    Page<Table> getTableList(String groupId, Integer pageNum, Integer pageSize, TableQueryDTO query);

    /**
     * 生成建表SQL语句
     * @param tableDTO 表信息
     * @param fields 字段信息列表
     * @param dbType 数据库类型
     * @return 建表SQL语句
     */
    String generateCreateTableSQL(TableDTO tableDTO, List<TableFieldDTO> fields, String dbType);

    /**
     * 生成字段注释SQL语句
     * @param tableDTO 表信息
     * @param fields 字段信息列表
     * @param dbType 数据库类型
     * @return 字段注释SQL语句
     */
    String generateFieldComments(TableDTO tableDTO, List<TableFieldDTO> fields, String dbType);

    /**
     * 生成表注释SQL语句
     * @param tableDTO 表信息
     * @param dbType 数据库类型
     * @return 表注释SQL语句
     */
    String generateTableComment(TableDTO tableDTO, String dbType);

    /**
     * 检查表是否存在
     */
    boolean checkTableExists(String dataSourceId, String tableName) throws Exception;

    /**
     * 根据分组ID获取表列表
     * @param groupId 分组ID
     * @return 表列表
     */
    List<Table> getTablesByGroupId(String groupId);

    /**
     * 根据模型ID获取表列表
     * @param modelId 模型ID
     * @return 表列表
     */
    List<Table> getTablesByModelId(String modelId);

    /**
     * 获取表的字段列表
     * @param tableId 表ID
     * @return 字段列表
     */
    List<TableField> getTableFields(String tableId);
}
