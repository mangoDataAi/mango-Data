package com.mango.test.database.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mango.test.constant.DatabaseKeywords;
import com.mango.test.constant.DatabaseTypes;
import com.mango.test.database.mapper.TableMapper;
import com.mango.test.dto.TableDTO;
import com.mango.test.dto.TableFieldDTO;
import com.mango.test.dto.TableQueryDTO;
import com.mango.test.database.entity.DataSource;
import com.mango.test.database.entity.Table;
import com.mango.test.database.entity.TableField;
import com.mango.test.exception.BusinessException;
import com.mango.test.database.mapper.TableFieldMapper;
import com.mango.test.database.service.DataSourceService;
import com.mango.test.database.service.TableService;
import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TableServiceImpl extends ServiceImpl<TableMapper, Table> implements TableService {

    @Autowired
    private TableFieldMapper tableFieldMapper;

    @Autowired
    private DataSourceService dataSourceService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTable(String groupId, TableDTO tableDTO) {
        // 创建表记录
        Table table = new Table();
        table.setName(tableDTO.getName());
        table.setDisplayName(tableDTO.getDisplayName());
        table.setFieldCount(tableDTO.getFields().size());
        table.setGroupId(groupId);
        table.setCreateTime(new Date());
        table.setUpdateTime(new Date());
        saveOrUpdate(table);

        // 创建字段记录
        saveTableFields(table.getId(), tableDTO.getFields());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTable(String id, TableDTO tableDTO) {
        // 更新表记录
        Table table = getById(id);
        if (table == null) {
            throw new BusinessException("表不存在");
        }

        table.setName(tableDTO.getName());
        table.setDisplayName(tableDTO.getDisplayName());
        table.setFieldCount(tableDTO.getFields().size());
        table.setUpdateTime(new Date());
        updateById(table);

        // 删除旧字段
        tableFieldMapper.delete(new LambdaQueryWrapper<TableField>()
                .eq(TableField::getTableId, id));

        // 创建新字段
        saveTableFields(id, tableDTO.getFields());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTable(String id) {
        // 删除表记录
        removeById(id);

        // 删除字段记录
        tableFieldMapper.delete(new LambdaQueryWrapper<TableField>()
                .eq(TableField::getTableId, id));
    }

    @Override
    public TableDTO getTableDetail(String id) {
        // 获取表信息
        Table table = getById(id);
        if (table == null) {
            throw new BusinessException("表不存在");
        }

        // 获取字段信息
        List<TableField> fields = tableFieldMapper.selectList(
                new LambdaQueryWrapper<TableField>()
                        .eq(TableField::getTableId, id)
        );

        // 转换为DTO
        TableDTO dto = new TableDTO();
        dto.setName(table.getName());
        dto.setDisplayName(table.getDisplayName());
        dto.setFields(fields.stream()
                .map(this::convertToFieldDTO)
                .collect(Collectors.toList()));

        return dto;
    }

    @Override
    public List<Table> getTablesByGroup(String groupId) {
        return list(new LambdaQueryWrapper<Table>()
                .eq(Table::getGroupId, groupId)
                .orderByDesc(Table::getUpdateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTableBySQL(String groupId, String sql) {
        try {
            // 解码 SQL
            String decodedSQL = URLDecoder.decode(sql, StandardCharsets.UTF_8.name());

            // 转义特殊字符
            decodedSQL = StringEscapeUtils.unescapeHtml4(decodedSQL);

            // 解析SQL获取表结构
            TableDTO tableDTO = parseSQLToTableDTO(decodedSQL);

            // 创建表
            createTable(groupId, tableDTO);
        } catch (UnsupportedEncodingException e) {
            throw new BusinessException("SQL解码失败: " + e.getMessage());
        } catch (Exception e) {
            throw new BusinessException("创建表失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void referenceExistingTables(String groupId, String sourceId, List<String> tables) {
        try {
            // 1. 获取数据源
            DataSource dataSource = dataSourceService.getById(sourceId);
            if (dataSource == null) {
                throw new BusinessException("数据源不存在");
            }

            // 2. 获取连接并查询表结构
            try (Connection conn = dataSourceService.getConnection(sourceId)) {
                DatabaseMetaData metaData = conn.getMetaData();

                for (String tableName : tables) {
                    // 3. 获取表的列信息
                    try (ResultSet columns = metaData.getColumns(null, null, tableName, null)) {
                        List<TableField> fields = new ArrayList<>();

                        while (columns.next()) {
                            TableField field = new TableField();
                            field.setName(columns.getString("COLUMN_NAME"));
                            field.setType(columns.getString("TYPE_NAME"));
                            field.setLength(columns.getInt("COLUMN_SIZE"));
                            field.setScale(columns.getInt("DECIMAL_DIGITS"));
                            field.setNotNull("NO".equals(columns.getString("IS_NULLABLE")) ? 1 : 0);
                            field.setMgcomment(columns.getString("REMARKS"));

                            // 检查是否是主键
                            try (ResultSet pks = metaData.getPrimaryKeys(null, null, tableName)) {
                                while (pks.next()) {
                                    if (field.getName().equals(pks.getString("COLUMN_NAME"))) {
                                        field.setIsPrimary(1);
                                        break;
                                    }
                                }
                            }

                            fields.add(field);
                        }

                        // 4. 创建表记录
                        Table table = new Table();
                        table.setName(tableName);
                        table.setDisplayName(tableName);
                        table.setGroupId(groupId);
                        table.setFieldCount(fields.size());
                        saveOrUpdate(table);

                        // 5. 保存字段信息
                        for (TableField field : fields) {
                            field.setTableId(table.getId());
                            tableFieldMapper.insert(field);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("引用表失败", e);
            throw new BusinessException("引用表失败: " + e.getMessage());
        }
    }

    @Override
    public void materializeTables(String targetSourceId, List<String> tableIds) {
        for (String tableId : tableIds) {
            materializeTable(tableId, targetSourceId);
        }
    }

    public void materializeTable(String tableId, String targetSourceId) {
        try {
            // 获取源表信息
            Table tableEntity = getById(tableId);
            if (tableEntity == null) {
                throw new BusinessException("表不存在");
            }

            // 获取表字段
            List<TableField> fieldEntities = tableFieldMapper.selectList(
                    new LambdaQueryWrapper<TableField>()
                            .eq(TableField::getTableId, tableId)
            );

            // 转换为DTO
            TableDTO tableDTO = new TableDTO();
            BeanUtils.copyProperties(tableEntity, tableDTO);

            // 转换字段为DTO
            List<TableFieldDTO> fieldDTOs = fieldEntities.stream()
                    .map(field -> {
                        TableFieldDTO fieldDTO = new TableFieldDTO();
                        BeanUtils.copyProperties(field, fieldDTO);
                        return fieldDTO;
                    })
                    .collect(Collectors.toList());

            // 获取目标数据源
            DataSource targetDs = dataSourceService.getById(targetSourceId);
            if (targetDs == null) {
                throw new BusinessException("目标数据源不存在");
            }

            // 生成建表SQL和注释SQL
            String createTableSql = generateCreateTableSQL(tableDTO, fieldDTOs, targetDs.getType());
            String commentsSql = generateFieldComments(tableDTO, fieldDTOs, targetDs.getType());

            // 使用目标数据源执行SQL
            try (Connection conn = dataSourceService.getConnection(targetSourceId)) {
                // 设置自动提交为false，开启事务
                boolean originalAutoCommit = conn.getAutoCommit();
                conn.setAutoCommit(false);

                try {
                    // 执行建表SQL
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(createTableSql);
                    }

                    // 如果有注释SQL，分别执行每条注释语句
                    if (!commentsSql.isEmpty()) {
                        String[] commentStatements = commentsSql.split(";\\s*\\n");
                        try (Statement stmt = conn.createStatement()) {
                            for (String comment : commentStatements) {
                                if (!comment.trim().isEmpty()) {
                                    stmt.execute(comment);
                                }
                            }
                        }
                    }

                    // 提交事务
                    conn.commit();
                } catch (SQLException e) {
                    // 发生异常时回滚事务
                    try {
                        conn.rollback();
                    } catch (SQLException re) {
                        log.error("回滚事务失败", re);
                    }
                    throw e;
                } finally {
                    // 恢复原来的自动提交设置
                    try {
                        conn.setAutoCommit(originalAutoCommit);
                    } catch (SQLException e) {
                        log.error("恢复自动提交设置失败", e);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("物化表失败", e);
            throw new BusinessException("物化表失败: " + e.getMessage());
        }
    }

    @Override
    public void generateTemplate(String tableId, String format, HttpServletResponse response) {
        // 获取表结构
        TableDTO table = getTableDetail(tableId);

        try {
            // 设置通用响应头
            response.setCharacterEncoding("utf-8");
            String filename = URLEncoder.encode(table.getName() + "." + format, "UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + filename);
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

            switch (format.toLowerCase()) {
                case "excel":
                    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                    generateExcelTemplate(table, response.getOutputStream());
                    break;
                case "csv":
                    response.setContentType("text/csv;charset=utf-8");
                    generateCSVTemplate(table, response.getWriter());
                    break;
                case "txt":
                    response.setContentType("text/plain;charset=utf-8");
                    generateTXTTemplate(table, response.getWriter());
                    break;
                case "dmp":
                    response.setContentType("application/octet-stream");
                    generateDMPTemplate(table, response.getWriter());
                    break;
                case "sql":
                    response.setContentType("text/plain;charset=utf-8");
                    generateSQLTemplate(table, response.getWriter());
                    break;
                default:
                    throw new BusinessException("不支持的模板格式: " + format);
            }
        } catch (Exception e) {
            throw new BusinessException("生成模板失败: " + e.getMessage());
        }
    }

    @Override
    public void generateEmptyTemplate(String format, HttpServletResponse response) {
        try {
            // 设置响应头，防止中文乱码
            response.setCharacterEncoding("utf-8");

            // 设置文件名
            String filename = URLEncoder.encode("template." + format, "UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + filename);

            // 创建一个空的示例表结构
            TableDTO emptyTable = new TableDTO();
            emptyTable.setName("example_table");
            emptyTable.setDisplayName("示例表");

            // 添加示例字段
            List<TableFieldDTO> fields = new ArrayList<>();

            // 添加示例字段
            TableFieldDTO idField = new TableFieldDTO();
            idField.setName("id");
            idField.setType("BIGINT");
            idField.setIsPrimary(1);
            idField.setMgautoIncrement(1);
            idField.setNotNull(1);
            idField.setMgcomment("主键ID");
            fields.add(idField);

            TableFieldDTO nameField = new TableFieldDTO();
            nameField.setName("name");
            nameField.setType("VARCHAR");
            nameField.setLength(50);
            nameField.setNotNull(1);
            nameField.setMgcomment("名称");
            fields.add(nameField);

            TableFieldDTO descField = new TableFieldDTO();
            descField.setName("description");
            descField.setType("TEXT");
            descField.setMgcomment("描述");
            fields.add(descField);

            TableFieldDTO createTimeField = new TableFieldDTO();
            createTimeField.setName("create_time");
            createTimeField.setType("DATETIME");
            createTimeField.setNotNull(1);
            createTimeField.setMgcomment("创建时间");
            fields.add(createTimeField);

            emptyTable.setFields(fields);

            // 根据不同格式生成模板
            switch (format.toLowerCase()) {
                case "excel":
                    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                    generateExcelTemplate(emptyTable, response.getOutputStream());
                    break;
                case "csv":
                    response.setContentType("text/csv;charset=utf-8");
                    generateCSVTemplate(emptyTable, response.getWriter());
                    break;
                case "txt":
                    response.setContentType("text/plain;charset=utf-8");
                    generateTXTTemplate(emptyTable, response.getWriter());
                    break;
                case "dmp":
                    response.setContentType("application/octet-stream");
                    generateDMPTemplate(emptyTable, response.getWriter());
                    break;
                case "sql":
                    response.setContentType("text/plain;charset=utf-8");
                    generateSQLTemplate(emptyTable, response.getWriter());
                    break;
                default:
                    throw new BusinessException("不支持的模板格式: " + format);
            }
        } catch (Exception e) {
            throw new BusinessException("生成空模板失败: " + e.getMessage());
        }
    }

    private void saveTableFields(String tableId, List<TableFieldDTO> fields) {
        for (int i = 0; i < fields.size(); i++) {
            TableFieldDTO fieldDTO = fields.get(i);
            TableField field = new TableField();
            field.setTableId(tableId);
            field.setName(fieldDTO.getName());
            field.setDisplayName(fieldDTO.getDisplayName());
            field.setType(fieldDTO.getType());
            field.setLength(fieldDTO.getLength());
            field.setPrecision(fieldDTO.getPrecision());
            field.setScale(fieldDTO.getScale());
            field.setIsPrimary(fieldDTO.getIsPrimary() != null && fieldDTO.getIsPrimary() == 1 ? 1 : 0);
            field.setNotNull(fieldDTO.getNotNull() != null && fieldDTO.getNotNull() == 1 ? 1 : 0);
            field.setMgautoIncrement(fieldDTO.getMgautoIncrement() != null && fieldDTO.getMgautoIncrement() == 1 ? 1 : 0);
            field.setDefaultValue(fieldDTO.getDefaultValue());
            field.setMgcomment(fieldDTO.getMgcomment());
            // 设置字段顺序
            field.setFieldOrder(i + 1);
            tableFieldMapper.insert(field);
        }
    }

    private TableFieldDTO convertToFieldDTO(TableField field) {
        TableFieldDTO dto = new TableFieldDTO();
        dto.setName(field.getName());
        dto.setDisplayName(field.getDisplayName());
        dto.setType(field.getType());
        dto.setLength(field.getLength());
        dto.setPrecision(field.getPrecision());
        dto.setScale(field.getScale());
        dto.setIsPrimary(field.getIsPrimary());
        dto.setNotNull(field.getNotNull());
        dto.setMgautoIncrement(field.getMgautoIncrement());
        dto.setDefaultValue(field.getDefaultValue());
        dto.setMgcomment(field.getMgcomment());
        return dto;
    }

    /**
     * 解析SQL语句获取表结构
     */
    private TableDTO parseSQLToTableDTO(String sql) {
        try {
            // 使用JSqlParser解析SQL
            net.sf.jsqlparser.statement.Statement statement = CCJSqlParserUtil.parse(sql);

            if (!(statement instanceof CreateTable)) {
                throw new BusinessException("仅支持CREATE TABLE语句");
            }

            CreateTable createTable = (CreateTable) statement;
            TableDTO tableDTO = new TableDTO();

            // 设置表名
            String tableName = createTable.getTable().getName();
            tableDTO.setName(tableName);
            tableDTO.setDisplayName(tableName); // 默认显示名与表名相同

            // 解析字段
            List<TableFieldDTO> fields = new ArrayList<>();
            for (ColumnDefinition column : createTable.getColumnDefinitions()) {
                TableFieldDTO field = new TableFieldDTO();

                // 设置字段名
                field.setName(column.getColumnName());
                field.setMgcomment(column.getColumnName());

                // 解析数据类型
                String dataType = column.getColDataType().getDataType().toUpperCase();
                field.setType(dataType);

                // 解析长度、精度、小数位数
                List<String> argumentsStringList = column.getColDataType().getArgumentsStringList();
                if (argumentsStringList != null && !argumentsStringList.isEmpty()) {
                    if (dataType.contains("CHAR") || dataType.contains("VARCHAR")) {
                        field.setLength(Integer.parseInt(argumentsStringList.get(0)));
                    } else if (dataType.equals("DECIMAL") || dataType.equals("NUMERIC")) {
                        field.setPrecision(Integer.parseInt(argumentsStringList.get(0)));
                        if (argumentsStringList.size() > 1) {
                            field.setScale(Integer.parseInt(argumentsStringList.get(1)));
                        }
                    }
                }

                // 解析约束
                List<String> columnSpecs = column.getColumnSpecs();
                if (columnSpecs != null) {
                    for (String spec : columnSpecs) {
                        switch (spec.toUpperCase()) {
                            case "PRIMARY KEY":
                                field.setIsPrimary(1);
                                field.setNotNull(1);
                                break;
                            case "NOT NULL":
                                field.setNotNull(1);
                                break;
                            case "AUTO_INCREMENT":
                            case "IDENTITY":
                                field.setMgautoIncrement(1);
                                break;
                            default:
                                if (spec.toUpperCase().startsWith("DEFAULT ")) {
                                    field.setDefaultValue(spec.substring(8));
                                } else if (spec.toUpperCase().startsWith("COMMENT ")) {
                                    field.setMgcomment(spec.substring(8).replace("'", ""));
                                }
                        }
                    }
                }

                fields.add(field);
            }

            tableDTO.setFields(fields);
            return tableDTO;

        } catch (JSQLParserException e) {
            log.error("解析SQL失败", e);
            throw new BusinessException("解析SQL失败: " + e.getMessage());
        }
    }

    /**
     * 根据数据库类型生成建表SQL
     */
    public String generateCreateTableSQL(TableDTO table, List<TableFieldDTO> fields, String dbType) {
        StringBuilder sql = new StringBuilder();

        // 处理序列（Oracle系列数据库的自增实现）
        List<String> sequences = new ArrayList<>();
        List<String> triggers = new ArrayList<>();

        // 添加建表语句开始
        if (isOracleFamily(dbType) && table.getName().length() > 30) {
            throw new BusinessException("Oracle系列数据库表名长度不能超过30个字符");
        }

        sql.append("CREATE TABLE ");
        sql.append(quoteIdentifier(table.getName(), dbType)).append(" (\n");

        // 收集主键字段
        List<String> primaryKeys = new ArrayList<>();

        // 添加字段定义
        for (int i = 0; i < fields.size(); i++) {
            TableFieldDTO field = fields.get(i);

            // 检查字段名长度限制
            if (isOracleFamily(dbType) && field.getName().length() > 30) {
                throw new BusinessException("Oracle系列数据库字段名长度不能超过30个字符");
            }

            sql.append("  ");
            sql.append(quoteIdentifier(field.getName(), dbType)).append(" ");

            // 转换字段类型
            String fieldType = getColumnTypeDefinition(field, dbType);
            sql.append(fieldType);

            // 处理约束
            if (field.getIsPrimary() != null && field.getIsPrimary() == 1) {
                primaryKeys.add(field.getName());
                if (!isBigDataPlatform(dbType) && !isPostgreSQLFamily(dbType)) {
                    sql.append(" PRIMARY KEY");
                }
            }

            // 处理非空约束
            sql.append(handleNotNull(field, dbType));

            // 处理自增
            sql.append(handleAutoIncrement(field, dbType, table.getName(), sequences, triggers));

            // 处理默认值
            sql.append(handleDefaultValue(field, dbType));

            if (i < fields.size() - 1) {
                sql.append(",");
            }
            sql.append("\n");
        }

        // 添加表级主键约束
        if (!primaryKeys.isEmpty() && isPostgreSQLFamily(dbType)) {
            sql.append("  ,CONSTRAINT ")
                    .append(quoteIdentifier("PK_" + table.getName(), dbType))
                    .append(" PRIMARY KEY (")
                    .append(primaryKeys.stream()
                            .map(pk -> quoteIdentifier(pk, dbType))
                            .collect(Collectors.joining(", ")))
                    .append(")\n");
        }

        sql.append(")");

        // 添加表选项
        sql.append(getTableOptions(dbType));

        // 添加序列和触发器（Oracle系列）
        if (!sequences.isEmpty()) {
            sql.append(";\n").append(String.join(";\n", sequences));
            sql.append(";\n").append(String.join(";\n", triggers));
        }


        return sql.toString();
    }

    // 处理非空约束
    private String handleNotNull(TableFieldDTO field, String dbType) {
        if (field.getNotNull() == null || field.getNotNull() == 0) {
            return "";
        }

        switch (dbType.toLowerCase()) {
            case "hive":
            case "spark":
            case "presto":
            case "impala":
                // 大数据平台通常不支持非空约束
                return "";
            case "clickhouse":
                return " NOT NULL";
            case "doris":
                return " NOT NULL";
            default:
                return " NOT NULL";
        }
    }

    // 处理自增
    private String handleAutoIncrement(TableFieldDTO field, String dbType, String tableName,
                                       List<String> sequences, List<String> triggers) {
        if (field.getMgautoIncrement() == null || field.getMgautoIncrement() == 0) {
            return "";
        }

        switch (dbType.toLowerCase()) {
            case "mysql":
            case "mariadb":
            case "h2":
            case "gbase":
                return " AUTO_INCREMENT";

            case "postgresql":
            case "kingbase":
            case "gaussdb":
            case "opengauss":
            case "highgo":
                return " GENERATED ALWAYS AS IDENTITY";

            case "oracle":
            case "dm7":
            case "dm8":
            case "shentong":
                // Oracle系列使用序列实现自增
                String seqName = tableName + "_" + field.getName() + "_SEQ";
                sequences.add(String.format(
                        "CREATE SEQUENCE %s START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE",
                        seqName
                ));
                triggers.add(generateAutoIncrementTrigger(tableName, field.getName(), seqName));
                return "";

            case "sqlserver":
            case "sybase":
                return " IDENTITY(1,1)";

            case "db2":
                return " GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)";

            case "clickhouse":
                return " AUTO_INCREMENT";

            case "doris":
                // Doris使用UNIQUE KEY自动生成
                return "";

            default:
                return "";
        }
    }

    // 处理默认值
    private String handleDefaultValue(TableFieldDTO field, String dbType) {
        if (field.getDefaultValue() == null) {
            return "";
        }

        String defaultValue = field.getDefaultValue();
        String fieldType = field.getType().toLowerCase();

        // 处理特殊默认值
        if (defaultValue.equalsIgnoreCase("CURRENT_TIMESTAMP")) {
            switch (dbType.toLowerCase()) {
                case "oracle":
                case "dm7":
                case "dm8":
                case "shentong":
                    return " DEFAULT SYSTIMESTAMP";
                case "postgresql":
                case "kingbase":
                case "gaussdb":
                case "opengauss":
                case "highgo":
                    return " DEFAULT CURRENT_TIMESTAMP";
                case "sqlserver":
                case "sybase":
                    return " DEFAULT GETDATE()";
                case "db2":
                    return " DEFAULT CURRENT TIMESTAMP";
                case "mysql":
                case "mariadb":
                case "h2":
                case "gbase":
                    return " DEFAULT CURRENT_TIMESTAMP";
                case "clickhouse":
                    return " DEFAULT now()";
                case "doris":
                    return " DEFAULT CURRENT_TIMESTAMP";
                default:
                    return " DEFAULT CURRENT_TIMESTAMP";
            }
        }

        // 处理NULL默认值
        if (defaultValue.equalsIgnoreCase("NULL")) {
            return " DEFAULT NULL";
        }

        // 处理字符串类型
        if (isStringType(fieldType)) {
            return " DEFAULT '" + defaultValue.replace("'", "''") + "'";
        }

        // 处理布尔类型
        if (fieldType.contains("bool")) {
            switch (dbType.toLowerCase()) {
                case "postgresql":
                case "kingbase":
                case "gaussdb":
                case "opengauss":
                case "highgo":
                    return " DEFAULT " + (defaultValue.equalsIgnoreCase("true") ? "true" : "false");
                case "mysql":
                case "mariadb":
                case "h2":
                    return " DEFAULT " + (defaultValue.equalsIgnoreCase("true") ? "1" : "0");
                case "oracle":
                case "dm7":
                case "dm8":
                case "shentong":
                    return " DEFAULT " + (defaultValue.equalsIgnoreCase("true") ? "1" : "0");
                default:
                    return " DEFAULT " + defaultValue;
            }
        }

        // 处理数值类型
        if (isNumericType(fieldType)) {
            try {
                Double.parseDouble(defaultValue);
                return " DEFAULT " + defaultValue;
            } catch (NumberFormatException e) {
                throw new BusinessException("非法的数值默认值: " + defaultValue);
            }
        }

        return " DEFAULT " + defaultValue;
    }

    // 判断是否是数值类型
    private boolean isNumericType(String type) {
        type = type.toLowerCase();
        return type.contains("int") || type.contains("float") || type.contains("double") ||
                type.contains("decimal") || type.contains("numeric") || type.contains("number");
    }

    // 生成Oracle自增触发器
    private String generateAutoIncrementTrigger(String tableName, String columnName, String seqName) {
        return String.format(
                "CREATE OR REPLACE TRIGGER %s\n" +
                        "BEFORE INSERT ON %s\n" +
                        "FOR EACH ROW\n" +
                        "BEGIN\n" +
                        "  SELECT %s.NEXTVAL\n" +
                        "  INTO :NEW.%s\n" +
                        "  FROM DUAL;\n" +
                        "END;",
                tableName + "_" + columnName + "_TRG",
                tableName,
                seqName,
                columnName
        );
    }

    // 判断数据库类型
    private boolean isOracleFamily(String dbType) {
        return Arrays.asList(DatabaseTypes.ORACLE_FAMILY).contains(dbType.toLowerCase());
    }

    private boolean isPostgreSQLFamily(String dbType) {
        return Arrays.asList(DatabaseTypes.POSTGRESQL_FAMILY).contains(dbType.toLowerCase());
    }

    private boolean isMySQLFamily(String dbType) {
        return Arrays.asList(DatabaseTypes.MYSQL_FAMILY).contains(dbType.toLowerCase());
    }

    private boolean isSQLServerFamily(String dbType) {
        return Arrays.asList(DatabaseTypes.SQLSERVER_FAMILY).contains(dbType.toLowerCase());
    }

    private boolean isBigDataPlatform(String dbType) {
        return Arrays.asList(DatabaseTypes.BIGDATA_FAMILY).contains(dbType.toLowerCase()) ||
                Arrays.asList(DatabaseTypes.OLAP_FAMILY).contains(dbType.toLowerCase());
    }

    private boolean isStringType(String type) {
        type = type.toLowerCase();
        return type.contains("char") || type.contains("text") || type.contains("string");
    }

    // Excel模板生成
    private void generateExcelTemplate(TableDTO table, OutputStream out) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Sheet1");

            // 创建表头
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < table.getFields().size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(table.getFields().get(i).getName());
            }

            // 创建示例数据行
            Row dataRow = sheet.createRow(1);
            for (int i = 0; i < table.getFields().size(); i++) {
                Cell cell = dataRow.createCell(i);
                cell.setCellValue(getExampleValue(table.getFields().get(i)));
            }

            workbook.write(out);
        }
    }

    // CSV模板生成
    private void generateCSVTemplate(TableDTO table, Writer writer) throws IOException {
        try (CSVWriter csvWriter = new CSVWriter(writer)) {
            // 写入表头
            String[] headers = table.getFields().stream()
                    .map(TableFieldDTO::getName)
                    .toArray(String[]::new);
            csvWriter.writeNext(headers);

            // 写入示例数据
            String[] data = table.getFields().stream()
                    .map(this::getExampleValue)
                    .toArray(String[]::new);
            csvWriter.writeNext(data);
        }
    }

    // 生成TXT模板
    private void generateTXTTemplate(TableDTO table, Writer writer) throws IOException {
        // 写入表头
        writer.write(table.getFields().stream()
                .map(TableFieldDTO::getName)
                .collect(Collectors.joining("\t")));
        writer.write("\n");

        // 写入示例数据
        writer.write(table.getFields().stream()
                .map(this::getExampleValue)
                .collect(Collectors.joining("\t")));
        writer.write("\n");
        writer.flush();
    }

    // 生成DMP模板
    private void generateDMPTemplate(TableDTO table, Writer writer) throws IOException {
        StringBuilder sb = new StringBuilder();

        // 写入表结构
        sb.append("-- 表结构\n");
        sb.append("CREATE TABLE ").append(table.getName()).append(" (\n");

        List<String> columnDefs = new ArrayList<>();
        for (TableFieldDTO field : table.getFields()) {
            StringBuilder def = new StringBuilder();
            def.append("  ").append(field.getName()).append(" ")
                    .append(getColumnTypeDefinition(field, ""));

            // 修复：将布尔值比较改为与整数1比较
            if (field.getNotNull() != null && field.getNotNull() == 1) {
                def.append(" NOT NULL");
            }

            if (field.getDefaultValue() != null) {
                def.append(" DEFAULT ").append(field.getDefaultValue());
            }

            columnDefs.add(def.toString());
        }

        sb.append(String.join(",\n", columnDefs));
        sb.append("\n);\n\n");

        // 添加注释
        for (TableFieldDTO field : table.getFields()) {
            if (field.getMgcomment() != null && !field.getMgcomment().isEmpty()) {
                sb.append(String.format("COMMENT ON COLUMN %s.%s IS '%s';\n",
                        table.getName(), field.getName(), field.getMgcomment()));
            }
        }

        // 写入示例数据
        sb.append("\n-- 示例数据\n");
        sb.append("INSERT INTO ").append(table.getName()).append(" (\n");
        sb.append("  ").append(table.getFields().stream()
                .map(TableFieldDTO::getName)
                .collect(Collectors.joining(", ")));
        sb.append("\n) VALUES (\n");
        sb.append("  ").append(table.getFields().stream()
                .map(this::formatSQLValue)
                .collect(Collectors.joining(", ")));
        sb.append("\n);\n");

        writer.write(sb.toString());
        writer.flush();
    }

    // 生成SQL模板
    private void generateSQLTemplate(TableDTO table, Writer writer) throws IOException {
        StringBuilder sb = new StringBuilder();

        // 写入建表语句
        sb.append(generateCreateTableSQL(table, table.getFields(), ""));
        sb.append("\n\n");

        // 写入示例INSERT语句
        sb.append("-- 示例数据\n");
        sb.append("INSERT INTO ").append(table.getName()).append(" (\n");
        sb.append("  ").append(table.getFields().stream()
                .map(TableFieldDTO::getName)
                .collect(Collectors.joining(", ")));
        sb.append("\n) VALUES (\n");
        sb.append("  ").append(table.getFields().stream()
                .map(this::formatSQLValue)
                .collect(Collectors.joining(", ")));
        sb.append("\n);\n");

        writer.write(sb.toString());
        writer.flush();
    }

    // 根据字段类型生成示例值
    private String getExampleValue(TableFieldDTO field) {
        switch (field.getType().toLowerCase()) {
            case "varchar":
            case "char":
                return "示例文本";
            case "int":
            case "bigint":
                return "1";
            case "decimal":
                return "1.23";
            case "date":
                return "2024-01-01";
            case "datetime":
            case "timestamp":
                return "2024-01-01 12:00:00";
            default:
                return "";
        }
    }

    // 格式化SQL值
    private String formatSQLValue(TableFieldDTO field) {
        String value = getExampleValue(field);
        switch (field.getType().toLowerCase()) {
            case "varchar":
            case "char":
            case "date":
            case "datetime":
            case "timestamp":
                return "'" + value + "'";
            default:
                return value;
        }
    }

    // 转换字段类型
    private String getColumnTypeDefinition(TableFieldDTO field, String dbType) {
        String type = field.getType().toUpperCase();
        StringBuilder definition = new StringBuilder();

        // 转换数据类型
        type = DatabaseTypes.convertType(type, dbType);

        if (type.contains("CHAR") || type.contains("VARCHAR")) {
            definition.append(type);
            if (field.getLength() != null) {
                int length = DatabaseTypes.adjustLength(type, field.getLength(), dbType);
                definition.append("(").append(length).append(")");
            }
        } else if (type.equals("DECIMAL") || type.equals("NUMERIC") || type.equals("NUMBER")) {
            definition.append(type).append("(");
            if (field.getPrecision() != null) {
                definition.append(field.getPrecision());
                if (field.getScale() != null) {
                    definition.append(",").append(field.getScale());
                }
            } else {
                definition.append("18,2"); // 默认精度
            }
            definition.append(")");
        } else {
            definition.append(type);
        }

        return definition.toString();
    }

    @Override
    public Page<Table> getTableList(String groupId, Integer pageNum, Integer pageSize, TableQueryDTO query) {
        // 创建分页对象
        Page<Table> page = new Page<>(pageNum, pageSize);

        // 构建查询条件
        LambdaQueryWrapper<Table> wrapper = new LambdaQueryWrapper<Table>()
                .eq(Table::getGroupId, groupId)
                .like(StringUtils.isNotBlank(query.getName()), Table::getName, query.getName())
                .like(StringUtils.isNotBlank(query.getDisplayName()), Table::getDisplayName, query.getDisplayName())
                .ge(StringUtils.isNotBlank(query.getCreateTimeStart()), Table::getCreateTime, query.getCreateTimeStart())
                .le(StringUtils.isNotBlank(query.getCreateTimeEnd()), Table::getCreateTime, query.getCreateTimeEnd())
                .ge(query.getMinFieldCount() != null, Table::getFieldCount, query.getMinFieldCount())
                .le(query.getMaxFieldCount() != null, Table::getFieldCount, query.getMaxFieldCount())
                .orderByDesc(Table::getCreateTime);

        // 执行分页查询
        return this.page(page, wrapper);
    }

    @Override
    public String generateTableComment(TableDTO tableDTO, String dbType) {
        if (StringUtils.isBlank(tableDTO.getDisplayName())) {
            return null;
        }

        String comment = tableDTO.getDisplayName().replace("'", "''");
        switch (DatabaseTypes.getDbFamily(dbType.toLowerCase())) {
            case "mysql":
                return "ALTER TABLE " + tableDTO.getName() + " COMMENT '" + comment + "'";
            case "oracle":
                return "COMMENT ON TABLE " + tableDTO.getName() + " IS '" + comment + "'";
            case "postgresql":
                return "COMMENT ON TABLE " + tableDTO.getName() + " IS '" + comment + "'";
            default:
                return null;
        }
    }

    // 获取字段是否可为空
    private boolean getNullable(TableFieldDTO field) {
        // Fix comparison with Integer
        return field.getNotNull() == null || field.getNotNull() == 0;
    }

    // 获取自增语法
    private String getAutoIncrementSyntax(String dbType) {
        switch (dbType.toLowerCase()) {
            case "mysql":
            case "mariadb":
            case "h2":
            case "gbase":
                return " AUTO_INCREMENT";

            case "postgresql":
            case "kingbase":
            case "gaussdb":
            case "opengauss":
            case "highgo":
                return " GENERATED ALWAYS AS IDENTITY";

            case "oracle":
            case "dm7":
            case "dm8":
            case "shentong":
                return ""; // 使用序列

            case "sqlserver":
            case "sybase":
                return " IDENTITY(1,1)";

            case "db2":
                return " GENERATED ALWAYS AS IDENTITY";

            case "clickhouse":
                return " AUTO_INCREMENT";

            case "doris":
                return " AUTO_INCREMENT";

            case "hive":
            case "spark":
            case "presto":
            case "impala":
                return ""; // 大数据平台通常不支持自增

            default:
                return " AUTO_INCREMENT";
        }
    }

    // 获取表选项
    private String getTableOptions(String dbType) {
        switch (dbType.toLowerCase()) {
            case "mysql":
            case "mariadb":
            case "gbase":
                return " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

            case "clickhouse":
                return " ENGINE = MergeTree() ORDER BY tuple()";

            case "doris":
                return " ENGINE=OLAP\nDISTRIBUTED BY HASH(`id`) BUCKETS 10\nPROPERTIES (\n" +
                        "    \"replication_num\" = \"1\",\n" +
                        "    \"storage_format\" = \"DEFAULT\"\n)";

            case "hive":
                return " STORED AS ORC";

            default:
                return "";
        }
    }

    // 处理标识符（表名、字段名）
    private String quoteIdentifier(String identifier, String dbType) {
        // 先验证标识符
        validateIdentifier(identifier, dbType);

        switch (dbType.toLowerCase()) {
            case "mysql":
            case "mariadb":
            case "h2":
            case "gbase":
                return "`" + identifier + "`";

            case "postgresql":
            case "kingbase":
            case "gaussdb":
            case "opengauss":
            case "highgo":
                return "\"" + identifier + "\"";

            case "oracle":
            case "dm7":
            case "dm8":
            case "shentong":
                return "\"" + identifier.toUpperCase() + "\"";

            case "sqlserver":
            case "sybase":
                return "[" + identifier + "]";

            case "db2":
                return "\"" + identifier.toUpperCase() + "\"";

            case "hive":
            case "spark":
            case "presto":
            case "impala":
            case "clickhouse":
            case "doris":
                return "`" + identifier + "`";

            default:
                return identifier;
        }
    }

    // 获取保留关键字
    private Set<String> getReservedKeywords(String dbType) {
        return DatabaseKeywords.getKeywords(dbType);
    }

    // 处理标识符引用
    private void validateIdentifier(String identifier, String dbType) {
        // 检查长度限制
        if (isOracleFamily(dbType) && identifier.length() > 30) {
            throw new BusinessException("Oracle系列数据库标识符长度不能超过30个字符: " + identifier);
        }
        if (isSQLServerFamily(dbType) && identifier.length() > 128) {
            throw new BusinessException("SQL Server系列数据库标识符长度不能超过128个字符: " + identifier);
        }
        if (isMySQLFamily(dbType) && identifier.length() > 64) {
            throw new BusinessException("MySQL系列数据库标识符长度不能超过64个字符: " + identifier);
        }

        // 检查标识符是否包含非法字符
        String pattern = isOracleFamily(dbType) ? "^[A-Za-z][A-Za-z0-9_$#]*$" : "^[A-Za-z][A-Za-z0-9_]*$";
        if (!identifier.matches(pattern)) {
            throw new BusinessException("标识符只能包含字母、数字和下划线，且必须以字母开头: " + identifier);
        }

        // 检查是否是数据库关键字
        if (isReservedKeyword(identifier, dbType)) {
            throw new BusinessException("标识符不能使用数据库关键字: " + identifier);
        }
    }

    // 检查是否是保留关键字
    private boolean isReservedKeyword(String identifier, String dbType) {
        Set<String> keywords = getReservedKeywords(dbType);
        return keywords.contains(identifier.toUpperCase());
    }

    // 生成字段注释
    public String generateFieldComments(TableDTO table, List<TableFieldDTO> fields, String dbType) {
        StringBuilder comments = new StringBuilder();

        switch (dbType.toLowerCase()) {
            case "mysql":
            case "mariadb":
            case "h2":
            case "gbase":
                // MySQL风格的注释
                for (TableFieldDTO field : fields) {
                    if (field.getMgcomment() != null && !field.getMgcomment().isEmpty()) {
                        comments.append("ALTER TABLE ")
                                .append(quoteIdentifier(table.getName(), dbType))
                                .append(" MODIFY COLUMN ")
                                .append(quoteIdentifier(field.getName(), dbType))
                                .append(" ")
                                .append(getColumnTypeDefinition(field, dbType))
                                .append(" COMMENT '")
                                .append(field.getMgcomment().replace("'", "''"))
                                .append("';\n");
                    }
                }
                break;

            case "postgresql":
            case "kingbase":
            case "gaussdb":
            case "opengauss":
            case "highgo":
                // PostgreSQL风格的注释
                for (TableFieldDTO field : fields) {
                    if (field.getMgcomment() != null && !field.getMgcomment().isEmpty()) {
                        comments.append("COMMENT ON COLUMN ")
                                .append(quoteIdentifier(table.getName(), dbType))
                                .append(".")
                                .append(quoteIdentifier(field.getName(), dbType))
                                .append(" IS '")
                                .append(field.getMgcomment().replace("'", "''"))
                                .append("';\n");
                    }
                }
                break;

            case "oracle":
            case "dm7":
            case "dm8":
            case "shentong":
                // Oracle风格的注释
                for (TableFieldDTO field : fields) {
                    if (field.getMgcomment() != null && !field.getMgcomment().isEmpty()) {
                        comments.append("COMMENT ON COLUMN ")
                                .append(quoteIdentifier(table.getName(), dbType))
                                .append(".")
                                .append(quoteIdentifier(field.getName(), dbType))
                                .append(" IS '")
                                .append(field.getMgcomment().replace("'", "''"))
                                .append("';\n");
                    }
                }
                break;

            case "db2":
                // DB2风格的注释
                for (TableFieldDTO field : fields) {
                    if (field.getMgcomment() != null && !field.getMgcomment().isEmpty()) {
                        comments.append("COMMENT ON COLUMN ")
                                .append(quoteIdentifier(table.getName(), dbType))
                                .append(".")
                                .append(quoteIdentifier(field.getName(), dbType))
                                .append(" IS '")
                                .append(field.getMgcomment().replace("'", "''"))
                                .append("';\n");
                    }
                }
                break;

            case "sqlserver":
            case "sybase":
                // SQL Server风格的注释
                for (TableFieldDTO field : fields) {
                    if (field.getMgcomment() != null && !field.getMgcomment().isEmpty()) {
                        comments.append("EXEC sp_addextendedproperty 'MS_Description', '")
                                .append(field.getMgcomment().replace("'", "''"))
                                .append("', 'SCHEMA', 'dbo', 'TABLE', '")
                                .append(table.getName())
                                .append("', 'COLUMN', '")
                                .append(field.getName())
                                .append("';\n");
                    }
                }
                break;

            case "clickhouse":
            case "doris":
                // ClickHouse/Doris风格的注释（在建表时直接添加）
                break;

            case "hive":
            case "spark":
            case "presto":
            case "impala":
                // 大数据平台的注释通常在建表时通过TBLPROPERTIES指定
                break;
        }

        return comments.toString();
    }

    @Override
    public boolean checkTableExists(String dataSourceId, String tableName) throws Exception {
        try (Connection conn = dataSourceService.getConnection(dataSourceId)) {
            // 获取数据源信息
            DataSource dataSource = dataSourceService.getById(dataSourceId);
            if (dataSource == null) {
                throw new BusinessException("数据源不存在");
            }

            DatabaseMetaData metaData = conn.getMetaData();
            String schema = null;
            String tableNamePattern = tableName;

            // 根据不同数据库类型处理schema和tableName
            switch (dataSource.getType().toLowerCase()) {
                case "oracle":
                case "dm7":
                case "dm8":
                case "shentong":
                    // Oracle系列数据库使用大写
                    schema = conn.getSchema() != null ? conn.getSchema().toUpperCase() : null;
                    tableNamePattern = tableName.toUpperCase();
                    break;

                case "postgresql":
                case "kingbase":
                case "gaussdb":
                case "opengauss":
                case "highgo":
                    // PostgreSQL系列默认使用public模式
                    schema = conn.getSchema() != null ? conn.getSchema() : "public";
                    break;

                case "mysql":
                case "mariadb":
                case "tidb":
                case "oceanbase":
                    // MySQL系列使用数据库名作为schema
                    schema = conn.getCatalog();
                    break;

                case "sqlserver":
                case "sybase":
                    // SQL Server默认使用dbo模式
                    schema = conn.getSchema() != null ? conn.getSchema() : "dbo";
                    break;

                case "db2":
                    // DB2使用当前schema
                    schema = conn.getSchema();
                    break;

                default:
                    // 其他数据库使用当前schema
                    schema = conn.getSchema();
            }

            try (ResultSet tables = metaData.getTables(conn.getCatalog(), schema, tableNamePattern, new String[]{"TABLE"})) {
                return tables.next();
            }
        }
    }

    @Override
    public List<Table> getTablesByGroupId(String groupId) {
        LambdaQueryWrapper<Table> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Table::getGroupId, groupId);
        return this.list(wrapper);
    }

    @Override
    public List<Table> getTablesByModelId(String modelId) {
        if (StringUtils.isEmpty(modelId)) {
            return new ArrayList<>();
        }
        
        try {
            // 查询与模型关联的表 - 使用字符串字段名替代Lambda表达式
            LambdaQueryWrapper<Table> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Table::getGroupId, modelId)
                      .orderByAsc(Table::getName);
            
            return list(queryWrapper);
        } catch (Exception e) {
            log.error("获取模型 [{}] 的表列表失败: {}", modelId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取表的字段列表
     * @param tableId 表ID
     * @return 字段列表
     */
    @Override
    public List<TableField> getTableFields(String tableId) {
        if (tableId == null || tableId.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            // 使用Wrapper查询字段列表
            LambdaQueryWrapper<TableField> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TableField::getTableId, tableId)
                        .orderByAsc(TableField::getId);
            
            return tableFieldMapper.selectList(queryWrapper);
        } catch (Exception e) {
            log.error("获取表字段失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
