package com.mango.test.database.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mango.test.common.util.UuidUtil;
import com.mango.test.database.entity.ColumnInfo;
import com.mango.test.database.entity.DataSource;
import com.mango.test.database.service.DataSourceService;
import com.mango.test.database.service.ExportService;
import com.mango.test.database.service.TableService;
import com.mango.test.database.service.impl.datasource.AbstractDatabaseHandler;
import com.mango.test.database.service.impl.datasource.DatabaseHandlerFactory;
import com.mango.test.dto.*;
import com.mango.test.util.BeanUtil;
import com.mango.test.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Slf4j
@Api(tags = "数据源管理")
@RestController
@RequestMapping("/api/ds")
public class DataSourceController {

    @Autowired
    private DataSourceService dataSourceService;

    @Autowired
    private ExportService exportService;

    @Autowired
    private TableService tableService;

    /**
     * 获取数据源列表
     */
    @GetMapping("/db/list")
    public R<List<DataSource>> getDataSourceList() {
        return R.ok(dataSourceService.list());
    }

    /**
     * 获取指定类型的数据源列表
     */
    @GetMapping("/db/list/byTypes")
    @ApiOperation("获取指定类型的数据源列表")
    public R<List<DataSource>> getDataSourceListByTypes(@RequestParam("types") String types) {
        try {
            if (StringUtils.isBlank(types)) {
                return R.ok(dataSourceService.list());
            }
            
            List<String> typeList = Arrays.asList(types.split(","));
            LambdaQueryWrapper<DataSource> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(DataSource::getType, typeList)
                  .eq(DataSource::getStatus, true)  // 只返回启用状态的数据源
                  .orderByDesc(DataSource::getCreateTime);
                  
            return R.ok(dataSourceService.list(wrapper));
        } catch (Exception e) {
            log.error("获取指定类型数据源列表失败", e);
            return R.fail("获取指定类型数据源列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取所有文件系统类型的数据源
     * 支持的文件系统类型包括：
     * - FTP服务器
     * - SFTP服务器
     * - HDFS
     * - MinIO对象存储
     * - Amazon S3
     * - 阿里云OSS
     */
    @GetMapping("/fs/list")
    @ApiOperation("获取所有文件系统类型的数据源")
    public R<List<DataSource>> getAllFileSystemDataSources() {
        try {
            // 定义所有支持的文件系统类型
            List<String> fileSystemTypes = Arrays.asList(
                "ftp", "sftp", "hdfs", "minio", "s3", "oss"
            );
            
            LambdaQueryWrapper<DataSource> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(DataSource::getType, fileSystemTypes)
                  .eq(DataSource::getStatus, true)  // 只返回启用状态的数据源
                  .orderByDesc(DataSource::getCreateTime);
                  
            return R.ok(dataSourceService.list(wrapper));
        } catch (Exception e) {
            log.error("获取文件系统数据源列表失败", e);
            return R.fail("获取文件系统数据源列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取所有消息队列类型的数据源
     * 支持的消息队列类型包括：
     * - Kafka
     * - RabbitMQ
     * - RocketMQ
     */
    @GetMapping("/mq/list")
    @ApiOperation("获取所有消息队列类型的数据源")
    public R<List<DataSource>> getAllMessageQueueDataSources() {
        try {
            // 定义所有支持的消息队列类型
            List<String> mqTypes = Arrays.asList(
                "kafka", "rabbitmq", "rocketmq"
            );
            
            LambdaQueryWrapper<DataSource> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(DataSource::getType, mqTypes)
                  .eq(DataSource::getStatus, true)  // 只返回启用状态的数据源
                  .orderByDesc(DataSource::getCreateTime);
                  
            return R.ok(dataSourceService.list(wrapper));
        } catch (Exception e) {
            log.error("获取消息队列数据源列表失败", e);
            return R.fail("获取消息队列数据源列表失败：" + e.getMessage());
        }
    }

    @GetMapping("/db/page")
    @ApiOperation("分页获取数据源列表")
    public R<IPage<DataSource>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String dbTypes,
            @RequestParam(required = false) Boolean status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        try {
            Page<DataSource> page = new Page<>(current, size);
            LambdaQueryWrapper<DataSource> wrapper = new LambdaQueryWrapper<>();

            // 基础名称查询
            if (StringUtils.isNotBlank(name)) {
                wrapper.like(DataSource::getName, name);
            }

            // 数据库类型查询
            if (StringUtils.isNotBlank(dbTypes)) {
                List<String> typeList = Arrays.asList(dbTypes.split(","));
                wrapper.in(DataSource::getType, typeList);
            }

            // 状态查询
            if (status != null) {
                wrapper.eq(DataSource::getStatus, status);
            }

            // 时间范围查询
            if (StringUtils.isNotBlank(startTime)) {
                wrapper.ge(DataSource::getCreateTime, startTime);
            }
            if (StringUtils.isNotBlank(endTime)) {
                wrapper.le(DataSource::getCreateTime, endTime);
            }

            // 默认按创建时间倒序
            wrapper.orderByDesc(DataSource::getCreateTime);

            return R.ok(dataSourceService.page(page, wrapper));
        } catch (Exception e) {
            log.error("获取数据源列表失败", e);
            return R.fail("获取数据源列表失败：" + e.getMessage());
        }
    }


    @DeleteMapping("/db/batch")
    @ApiOperation("批量删除数据源")
    public R<Boolean> batchDelete(@RequestBody List<Long> ids) {
        try {
            boolean success = dataSourceService.removeByIds(ids);
            return R.ok(success);
        } catch (Exception e) {
            log.error("批量删除失败", e);
            return R.fail("批量删除失败：" + e.getMessage());
        }
    }

    /**
     * 获取数据源的表列表
     */
    @GetMapping("/{dataSourceId}/tables")
    @ApiOperation("获取数据库表列表")
    public R<List<Map<String, Object>>> getTableList(
            @PathVariable String dataSourceId,
            @RequestParam(required = false, defaultValue = "table") String types,
            @RequestParam(required = false) String searchText) {
        try {
            List<Map<String, Object>> tables = dataSourceService.getTableList(dataSourceId, types, searchText);
            return R.ok(tables);
        } catch (Exception e) {
            log.error("获取表列表失败", e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取表字段信息
     */
    @GetMapping("/{dataSourceId}/table/{tableName}/columns")
    public R<List<ColumnInfo>> getTableColumns(
            @PathVariable String dataSourceId,
            @PathVariable String tableName) {
        return dataSourceService.getTableColumns(dataSourceId, tableName);
    }

    /**
     * 测试数据源连接
     */
    @PostMapping("/db/test")
    public R<Boolean> testConnection(@RequestBody DataSource dataSource) {
        try {
            boolean result = dataSourceService.testConnection(dataSource);
            return R.ok(result);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 执行SQL
     */
    @PostMapping("/execute")
    @ApiOperation("执行SQL查询")
    public R executeQuery(@RequestBody QueryRequest request) {
        try {
            String sql = request.getSql();
            if (StringUtils.isBlank(sql)) {
                return R.fail("SQL语句不能为空");
            }

            List<Map<String, Object>> result;
            if (request.getDataSourceId() != null) {
                // 通过数据源ID执行查询
                result = dataSourceService.executeQuery(request.getDataSourceId(), sql);
            } else if (StringUtils.isNotBlank(request.getUrl()) &&
                    StringUtils.isNotBlank(request.getUsername()) &&
                    StringUtils.isNotBlank(request.getDbType())) {
                // 通过直接连接信息执行查询
                String dbType = request.getDbType();

                // 如果是查询表结构的SQL，替换为获取完整表结构信息的SQL
                if (sql.toLowerCase().contains("desc") || sql.toLowerCase().contains("describe") ||
                        sql.toLowerCase().matches("show\\s+columns\\s+from.*")) {
                    String tableName = BeanUtil.parseTableName(sql);
                    String schemaName = BeanUtil.parseSchemaName(sql);
                    sql = BeanUtil.getTableStructureSQL(dbType, tableName, schemaName);
                }

                result = dataSourceService.executeQuery(
                        request.getUrl(),
                        request.getUsername(),
                        request.getPassword(),
                        sql
                );
            } else {
                return R.fail("请提供数据源ID或完整的数据库连接信息");
            }

            return R.ok(result);
        } catch (Exception e) {
            log.error("Execute query failed", e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取表结构
     */
    @GetMapping("/{dataSourceId}/table/{tableName}/structure")
    @ApiOperation("获取表结构")
    public R<List<Map<String, Object>>> getTableStructure(
            @PathVariable String dataSourceId,
            @PathVariable String tableName) {
        try {
            List<Map<String, Object>> tables = dataSourceService.getTableList(
                    dataSourceId,
                    "table",  // 只查询表类型
                    tableName // 使用表名作为搜索条件
            );
            List<Map<String, Object>> structure = new ArrayList<>();

            // 查找匹配的表并添加到结果列表
            for (Map<String, Object> table : tables) {
                if (tableName.equals(table.get("name"))) {
                    structure.add(table);
                    break;
                }
            }

            return R.ok(structure);
        } catch (Exception e) {
            log.error("获取表结构失败", e);
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/create")
    @ApiOperation("创建数据源")
    public R<Boolean> create(@RequestBody DataSource dataSource) {
        dataSource.setId(UuidUtil.generateUuid());
        Date date = new Date();
        dataSource.setCreateTime(date);
        dataSource.setUpdateTime(date);
        return R.ok(dataSourceService.save(dataSource));
    }

    @PutMapping("/update")
    @ApiOperation("更新数据源")
    public R<Boolean> update(@RequestBody DataSource dataSource) {
        return R.ok(dataSourceService.updateById(dataSource));
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除数据源")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(dataSourceService.removeById(id));
    }

    @GetMapping("/db/{id}/tables")
    @ApiOperation("获取数据库表列表")
    public R<List<Map<String, Object>>> getTables(@PathVariable String id) {
        try {
            List<Map<String, Object>> tables = dataSourceService.getTables(id);
            return R.ok(tables);
        } catch (Exception e) {
            log.error("获取表列表失败", e);
            return R.fail("获取表列表失败：" + e.getMessage());
        }
    }

    @PostMapping("/test")
    @ApiOperation("测试数据源配置")
    public R<String> testConfig(@RequestBody DataSource dataSource) {
        try {
            boolean success = dataSourceService.testConnection(dataSource);
            return success ? R.ok("连接成功") : R.fail("连接失败");
        } catch (Exception e) {
            log.error("测试连接失败", e);
            return R.fail("连接失败：" + e.getMessage());
        }
    }

    @PostMapping("/table/structure")
    @ApiOperation("获取表结构")
    public R<List<ColumnInfo>> getTableStructure(@RequestBody Map<String, Object> params) {
        try {
            String id = params.get("id").toString();
            String tableName = params.get("tableName").toString();
            return dataSourceService.getTableColumns(id, tableName);
        } catch (Exception e) {
            log.error("获取表结构失败", e);
            return R.fail("获取表结构失败：" + e.getMessage());
        }
    }



    @GetMapping("/{dataSourceId}/export")
    @ApiOperation("导出查询结果")
    public void exportData(
            @PathVariable String dataSourceId,
            @RequestParam("types") List<String> types,
            @RequestParam String tableName,
            @RequestParam(required = false) String sql,
            HttpServletResponse response) {
        try {
            // 设置当前表信息
            exportService.setTableInfo(dataSourceId, tableName);

            // 执行查询获取数据
            List<Map<String, Object>> queryResult = dataSourceService.executeQuery(
                    dataSourceId,
                    StringUtils.isNotBlank(sql) ? sql : "SELECT * FROM " + tableName
            );

            // 导出数据
            exportService.exportData(types, queryResult, response);
        } catch (Exception e) {
            log.error("导出数据失败", e);
            throw new RuntimeException("导出数据失败：" + e.getMessage());
        }
    }


    @GetMapping("/{dataSourceId}/export/tables")
    @ApiOperation("导出选中表数据")
    public void exportTables(
            @PathVariable String dataSourceId,
            @RequestParam List<String> tableNames,
            @RequestParam(required = false) List<String> types,
            HttpServletResponse response) throws IOException {
        File tempFile = null;
        try {
            if (tableNames == null || tableNames.isEmpty()) {
                throw new IllegalArgumentException("请选择要导出的表");
            }

            // 如果没有指定导出格式，使用所有支持的格式
            if (types == null || types.isEmpty()) {
                types = Arrays.asList("csv", "xlsx", "json", "xml", "txt", "sql", "dmp");
            }

            // 规范化类型列表，避免重复的文件扩展名
            Set<String> normalizedTypes = new LinkedHashSet<>();
            for (String type : types) {
                if (type != null) {
                    switch (type.toLowerCase()) {
                        case "excel":
                        case "xlsx":
                            normalizedTypes.add("xlsx");
                            break;
                        case "sql":
                            normalizedTypes.add("sql");
                            break;
                        case "dmp":
                            normalizedTypes.add("dmp");
                            break;
                        case "csv":
                            normalizedTypes.add("csv");
                            break;
                        case "json":
                            normalizedTypes.add("json");
                            break;
                        case "xml":
                            normalizedTypes.add("xml");
                            break;
                        case "txt":
                            normalizedTypes.add("txt");
                            break;
                        default:
                            log.warn("不支持的导出格式: {}", type);
                    }
                }
            }

            if (normalizedTypes.isEmpty()) {
                throw new IllegalArgumentException("没有有效的导出格式");
            }

            // 单表单格式导出
            if (tableNames.size() == 1 && normalizedTypes.size() == 1) {
                String tableName = tableNames.get(0);
                String type = normalizedTypes.iterator().next();
                handleSingleExport(dataSourceId, tableName, type, response);
                return;
            }

            // 设置响应头
            response.reset();
            response.setCharacterEncoding("utf-8");
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", 
                "attachment;filename=" + URLEncoder.encode("tables_export.zip", "UTF-8"));
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");

            // 创建临时文件
            tempFile = File.createTempFile("export_", ".zip");
            
            // 用于跟踪已添加的文件名
            Set<String> usedNames = new HashSet<>();

            // 创建一个ByteArrayOutputStream来存储所有数据
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zipOut = new ZipOutputStream(baos)) {
                // 遍历每个表
                for (String tableName : tableNames) {
                    if (tableName == null || tableName.trim().isEmpty()) {
                        continue;
                    }
                    
                    // 遍历每种导出格式
                    for (String type : normalizedTypes) {
                        String entryFileName = generateUniqueFileName(tableName, type, usedNames);
                        usedNames.add(entryFileName.toLowerCase());
                        
                        log.info("正在导出: {}", entryFileName);
                        
                        // 为每个文件创建一个ByteArrayOutputStream
                        ByteArrayOutputStream entryBaos = new ByteArrayOutputStream();
                        try {
                            // 导出数据到临时ByteArrayOutputStream
                            switch (type.toLowerCase()) {
                                case "csv":
                                    exportTableAsCSV(dataSourceId, tableName, entryBaos);
                                    break;
                                case "xlsx":
                                    exportTableAsExcel(dataSourceId, tableName, entryBaos);
                                    break;
                                case "json":
                                    exportTableAsJSON(dataSourceId, tableName, entryBaos);
                                    break;
                                case "xml":
                                    exportTableAsXML(dataSourceId, tableName, entryBaos);
                                    break;
                                case "sql":
                                    exportTableAsSQL(dataSourceId, tableName, entryBaos);
                                    break;
                                case "dmp":
                                    exportTableAsDMP(dataSourceId, tableName, entryBaos);
                                    break;
                                case "txt":
                                    exportTableAsTXT(dataSourceId, tableName, entryBaos);
                                    break;
                            }
                            
                            // 将数据写入ZIP文件
                            ZipEntry entry = new ZipEntry(entryFileName);
                            zipOut.putNextEntry(entry);
                            entryBaos.writeTo(zipOut);
                            zipOut.closeEntry();
                            
                        } catch (Exception e) {
                            log.error("导出表 {} 为 {} 格式时失败: {}", tableName, type, e.getMessage());
                            // 继续处理下一个文件
                        } finally {
                            entryBaos.close();
                        }
                    }
                }
                zipOut.flush();
            }

            // 将ZIP内容写入响应
            try (OutputStream out = response.getOutputStream()) {
                baos.writeTo(out);
                out.flush();
            }
            
        } catch (Exception e) {
            log.error("导出失败", e);
            response.reset();
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"message\":\"" + e.getMessage() + "\"}");
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * 处理单表单格式导出
     */
    private void handleSingleExport(String dataSourceId, String tableName, String type, HttpServletResponse response) throws IOException {
        switch (type) {
            case "csv":
                response.setContentType("text/csv");
                response.setHeader("Content-Disposition",
                        "attachment;filename=" + URLEncoder.encode(tableName + ".csv", "UTF-8"));
                exportTableAsCSV(dataSourceId, tableName, response.getOutputStream());
                break;
            case "xlsx":
                response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                response.setHeader("Content-Disposition",
                        "attachment;filename=" + URLEncoder.encode(tableName + ".xlsx", "UTF-8"));
                exportTableAsExcel(dataSourceId, tableName, response.getOutputStream());
                break;
            case "json":
                response.setContentType("application/json");
                response.setHeader("Content-Disposition",
                        "attachment;filename=" + URLEncoder.encode(tableName + ".json", "UTF-8"));
                exportTableAsJSON(dataSourceId, tableName, response.getOutputStream());
                break;
            case "xml":
                response.setContentType("application/xml");
                response.setHeader("Content-Disposition",
                        "attachment;filename=" + URLEncoder.encode(tableName + ".xml", "UTF-8"));
                exportTableAsXML(dataSourceId, tableName, response.getOutputStream());
                break;
            case "sql":
                response.setContentType("text/plain");
                response.setHeader("Content-Disposition",
                        "attachment;filename=" + URLEncoder.encode(tableName + ".sql", "UTF-8"));
                exportTableAsSQL(dataSourceId, tableName, response.getOutputStream());
                break;
            case "dmp":
                response.setContentType("text/plain");
                response.setHeader("Content-Disposition",
                        "attachment;filename=" + URLEncoder.encode(tableName + ".dmp", "UTF-8"));
                exportTableAsDMP(dataSourceId, tableName, response.getOutputStream());
                break;
            default:
                response.setContentType("text/plain");
                response.setHeader("Content-Disposition",
                        "attachment;filename=" + URLEncoder.encode(tableName + ".txt", "UTF-8"));
                exportTableAsTXT(dataSourceId, tableName, response.getOutputStream());
        }
    }

    /**
     * 生成唯一文件名
     */
    private String generateUniqueFileName(String tableName, String type, Set<String> usedNames) {
        String baseFileName = tableName + "." + type.toLowerCase();
        String entryFileName = baseFileName;
        int counter = 1;
        
        while (usedNames.contains(entryFileName.toLowerCase())) {
            String nameWithoutExt = tableName;
            String ext = type.toLowerCase();
            entryFileName = String.format("%s_%d.%s", nameWithoutExt, counter++, ext);
        }
        
        return entryFileName;
    }

    @PostMapping("/copy-query")
    @ApiOperation("复制查询结果到新表")
    public R<Boolean> copyQueryResult(@RequestBody @Validated CopyQueryDTO params) {
        try {

            // 参数校验
            if (StringUtils.isBlank(params.getSql())) {
                return R.fail("SQL语句不能为空");
            }

            if (params.getTargetIds() == null || params.getTargetIds().isEmpty()) {
                return R.fail("请选择目标数据源");
            }

            if (StringUtils.isBlank(params.getTableName())) {
                return R.fail("目标表名不能为空");
            }

            if (params.getExistsStrategy() == null) {
                return R.fail("请选择表已存在时的处理策略");
            }

            // 1. 先检查所有目标数据源中是否存在同名表
            if (params.getExistsStrategy() == 1) { // 不覆盖策略
                List<String> existingTables = new ArrayList<>();
                for (String targetId : params.getTargetIds()) {
                    if (tableService.checkTableExists(targetId, params.getTableName())) {
                        DataSource targetDs = dataSourceService.getById(targetId);
                        existingTables.add(String.format("数据源[%s]中表[%s]已存在",
                                targetDs.getName(), params.getTableName()));
                    }
                }
                if (!existingTables.isEmpty()) {
                    return R.fail("以下表已存在，无法创建：\n" + String.join("\n", existingTables));
                }
            }

            // 1. 获取源数据源
            DataSource sourceDs = dataSourceService.getById(params.getSourceId());
            if (sourceDs == null) {
                return R.fail("源数据源不存在");
            }

            // 2. 从源数据源执行查询获取数据
            List<Map<String, Object>> data = dataSourceService.executeQuery(
                    params.getSourceId(),
                    params.getSql()
            );

            if (data.isEmpty()) {
                return R.fail("查询结果为空");
            }

            // 3. 从查询结果中获取字段信息并构建TableDTO
            Map<String, Object> firstRow = data.get(0);
            List<TableFieldDTO> fields = new ArrayList<>();

            for (String key : firstRow.keySet()) {
                TableFieldDTO field = new TableFieldDTO();
                field.setName(key);
                field.setType(getFieldType(firstRow.get(key)));
                fields.add(field);
            }

            // 4. 遍历目标数据源进行复制
            for (String targetId : params.getTargetIds()) {
                DataSource targetDs = dataSourceService.getById(targetId);
                if (targetDs == null) {
                    log.warn("目标数据源不存在: {}", targetId);
                    continue;
                }

                // 处理表名（检查是否存在并根据策略处理）
                String finalTableName = handleTableName(targetId, params.getTableName(), params.getExistsStrategy());
                if (finalTableName == null) {
                    continue; // 根据策略跳过
                }

                // 构建TableDTO
                TableDTO tableDTO = new TableDTO();
                tableDTO.setName(finalTableName);
                tableDTO.setDisplayName(params.getTableComment());

                // 创建表并插入数据
                createTableAndInsertData(targetId, tableDTO, fields, data, targetDs.getType());
            }

            return R.ok(true);
        } catch (Exception e) {
            log.error("复制查询结果失败", e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 删除数据库表
     */
    @DeleteMapping("/{dataSourceId}/table/{tableName}")
    @ApiOperation("删除数据库表")
    public R<Void> dropTable(
            @PathVariable String dataSourceId,
            @PathVariable String tableName) {
        try {
            DataSource dataSource = dataSourceService.getById(dataSourceId);
            if (dataSource == null) {
                return R.fail("数据源不存在");
            }

            AbstractDatabaseHandler handler = DatabaseHandlerFactory.getDatabaseHandler(dataSource);
            String dropTableSql = handler.getDropTableSql(tableName);

            try (Connection conn = handler.getConnection()) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(dropTableSql);
                }
            }

            return R.ok();
        } catch (Exception e) {
            log.error("删除表失败", e);
            return R.fail("删除表失败：" + e.getMessage());
        }
    }

    /**
     * 批量删除数据库表
     */
    @DeleteMapping("/{dataSourceId}/tables/batch")
    @ApiOperation("批量删除数据库表")
    public R<Void> dropTables(
            @PathVariable String dataSourceId,
            @RequestBody Map<String, List<String>> body) {
        try {
            List<String> tableNames = body.get("tableNames");
            if (tableNames == null || tableNames.isEmpty()) {
                return R.fail("请选择要删除的表");
            }

            DataSource dataSource = dataSourceService.getById(dataSourceId);
            if (dataSource == null) {
                return R.fail("数据源不存在");
            }

            AbstractDatabaseHandler handler = DatabaseHandlerFactory.getDatabaseHandler(dataSource);

            try (Connection conn = handler.getConnection()) {
                try (Statement stmt = conn.createStatement()) {
                    for (String tableName : tableNames) {
                        String dropTableSql = handler.getDropTableSql(tableName);
                        stmt.execute(dropTableSql);
                    }
                }
            }

            return R.ok();
        } catch (Exception e) {
            log.error("批量删除表失败", e);
            return R.fail("批量删除表失败：" + e.getMessage());
        }
    }

    /**
     * 处理表名（检查是否存在并根据策略处理）
     */
    private String handleTableName(String dataSourceId, String tableName, Integer strategy) throws Exception {
        boolean exists = tableService.checkTableExists(dataSourceId, tableName);
        if (!exists) {
            return tableName;
        }

        switch (strategy) {
            case 1: // 不覆盖
                log.info("表 {} 已存在，根据策略跳过", tableName);
                throw new RuntimeException(String.format("表 %s 已存在，已跳过复制", tableName));
            case 2: // 覆盖
                log.info("表 {} 已存在，将进行覆盖", tableName);
                // 先删除已存在的表
                try (Connection conn = dataSourceService.getConnection(dataSourceId)) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("DROP TABLE " + tableName);
                    }
                }
                return tableName;
            case 3: // 自动增加后缀
                int suffix = 1;
                String newTableName;
                do {
                    newTableName = tableName + "_" + suffix++;
                } while (tableService.checkTableExists(dataSourceId, newTableName));
                log.info("表 {} 已存在，自动重命名为 {}", tableName, newTableName);
                return newTableName;
            default:
                return null;
        }
    }

    /**
     * 创建表并插入数据
     */
    private void createTableAndInsertData(String targetId, TableDTO tableDTO, List<TableFieldDTO> fields,
                                          List<Map<String, Object>> data, String dbType) throws Exception {
        // 生成建表SQL
        String createTableSql = tableService.generateCreateTableSQL(tableDTO, fields, dbType);
        String tableCommentSql = tableService.generateTableComment(tableDTO, dbType);

        try (Connection conn = dataSourceService.getConnection(targetId)) {
            conn.setAutoCommit(false);

            try {
                // 创建表
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(createTableSql);
                }

                // 执行表注释SQL
                if (StringUtils.isNotBlank(tableCommentSql)) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(tableCommentSql);
                    }
                }

                // 构建INSERT语句
                StringBuilder insertSql = new StringBuilder();
                insertSql.append("INSERT INTO ").append(tableDTO.getName()).append(" (");
                insertSql.append(fields.stream()
                        .map(TableFieldDTO::getName)
                        .collect(Collectors.joining(", ")));
                insertSql.append(") VALUES (");
                insertSql.append(fields.stream()
                        .map(field -> "?")
                        .collect(Collectors.joining(", ")));
                insertSql.append(")");

                // 插入数据
                try (PreparedStatement pstmt = conn.prepareStatement(insertSql.toString())) {
                    for (Map<String, Object> row : data) {
                        int index = 1;
                        for (TableFieldDTO field : fields) {
                            Object value = row.get(field.getName());
                            if (value == null) {
                                pstmt.setNull(index++, java.sql.Types.VARCHAR);
                            } else if ("dm".equalsIgnoreCase(dbType)) {
                                // DM数据库特殊处理
                                if (value instanceof String) {
                                    // 对于DM数据库，处理大文本
                                    String strValue = (String) value;
                                    if (strValue.length() > 4000) {
                                        pstmt.setCharacterStream(index++, new StringReader(strValue), strValue.length());
                                    } else {
                                        pstmt.setString(index++, strValue);
                                    }
                                } else if (value instanceof Number) {
                                    if (value instanceof Integer || value instanceof Long) {
                                        pstmt.setLong(index++, ((Number) value).longValue());
                                    } else {
                                        pstmt.setDouble(index++, ((Number) value).doubleValue());
                                    }
                                } else if (value instanceof Boolean) {
                                    pstmt.setInt(index++, ((Boolean) value) ? 1 : 0);
                                } else if (value instanceof Date) {
                                    pstmt.setTimestamp(index++, new java.sql.Timestamp(((Date) value).getTime()));
                                } else {
                                    pstmt.setString(index++, value.toString());
                                }
                            } else {
                                // 其他数据库正常处理
                                pstmt.setObject(index++, value);
                            }
                        }
                        pstmt.addBatch();
                    }
                    pstmt.executeBatch();
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * 导出表数据为CSV格式
     */
    private void exportTableAsCSV(String dataSourceId, String tableName, OutputStream out) throws IOException {
        try {
            List<Map<String, Object>> data = dataSourceService.executeQuery(
                    dataSourceId,
                    "SELECT * FROM " + tableName
            );
            
            // Don't use try-with-resources here as it might close the underlying ZIP stream
            OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            
            // Write CSV header
            if (!data.isEmpty()) {
                writer.write(String.join(",", data.get(0).keySet()) + "\n");
            }
            
            // Write data rows
            for (Map<String, Object> row : data) {
                writer.write(row.values().stream()
                        .map(this::formatCSVValue)
                        .collect(Collectors.joining(",")) + "\n");
            }
            writer.flush();
            // Don't close the writer as it will close the underlying stream
        } catch (Exception e) {
            throw new IOException("Failed to export CSV: " + e.getMessage(), e);
        }
    }

    /**
     * 导出表数据为Excel格式
     */
    private void exportTableAsExcel(String dataSourceId, String tableName, OutputStream out) throws IOException {
        XSSFWorkbook workbook = null;
        try {
            List<Map<String, Object>> data = dataSourceService.executeQuery(
                    dataSourceId,
                    "SELECT * FROM " + tableName
            );
            
            workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet(tableName);
            
            // Create header row
            if (!data.isEmpty()) {
                Row headerRow = sheet.createRow(0);
                List<String> headers = new ArrayList<>(data.get(0).keySet());
                for (int i = 0; i < headers.size(); i++) {
                    headerRow.createCell(i).setCellValue(headers.get(i));
                }
                
                // Create data rows
                for (int i = 0; i < data.size(); i++) {
                    Row row = sheet.createRow(i + 1);
                    Map<String, Object> rowData = data.get(i);
                    for (int j = 0; j < headers.size(); j++) {
                        Cell cell = row.createCell(j);
                        Object value = rowData.get(headers.get(j));
                        setCellValue(cell, value);
                    }
                }
            }
            
            workbook.write(out);
            out.flush();
        } catch (Exception e) {
            throw new IOException("Failed to export Excel: " + e.getMessage(), e);
        } finally {
            // Only close the workbook if we're not writing to a ZIP stream
            if (workbook != null && !(out instanceof ZipOutputStream)) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * 导出表数据为JSON格式
     */
    private void exportTableAsJSON(String dataSourceId, String tableName, OutputStream out) throws IOException {
        try {
            List<Map<String, Object>> data = dataSourceService.executeQuery(
                    dataSourceId,
                    "SELECT * FROM " + tableName
            );
            
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(out, data);
            out.flush();
        } catch (Exception e) {
            throw new IOException("Failed to export JSON: " + e.getMessage(), e);
        }
    }

    /**
     * 导出表数据为XML格式
     */
    private void exportTableAsXML(String dataSourceId, String tableName, OutputStream out) throws IOException {
        try {
            List<Map<String, Object>> data = dataSourceService.executeQuery(
                    dataSourceId,
                    "SELECT * FROM " + tableName
            );
            
            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            XMLStreamWriter writer = factory.createXMLStreamWriter(out, "UTF-8");
            try {
                writer.writeStartDocument("UTF-8", "1.0");
                writer.writeStartElement("table");
                writer.writeAttribute("name", tableName);
                
                for (Map<String, Object> row : data) {
                    writer.writeStartElement("row");
                    for (Map.Entry<String, Object> entry : row.entrySet()) {
                        writer.writeStartElement(entry.getKey());
                        writer.writeCharacters(entry.getValue() != null ? entry.getValue().toString() : "");
                        writer.writeEndElement();
                    }
                    writer.writeEndElement();
                }
                
                writer.writeEndElement();
                writer.writeEndDocument();
                writer.flush();
            } catch (XMLStreamException e) {
                throw new IOException("Failed to write XML: " + e.getMessage(), e);
            } finally {
                // Only close the writer if we're not writing to a ZIP stream
                if (!(out instanceof ZipOutputStream)) {
                    try {
                        writer.close();
                    } catch (XMLStreamException e) {
                        // ignore
                    }
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to export XML: " + e.getMessage(), e);
        }
    }

    /**
     * 导出表数据为SQL格式
     */
    private void exportTableAsSQL(String dataSourceId, String tableName, OutputStream out) throws IOException {
        BufferedWriter writer = null;
        try {
            // 获取表结构
            List<ColumnInfo> columns = dataSourceService.getTableColumns(dataSourceId.toString(), tableName)
                    .getData();
            
            // 使用LinkedHashSet去重，保持字段顺序
            Set<String> processedFields = new LinkedHashSet<>();
            List<TableFieldDTO> fields = columns.stream()
                    .filter(col -> processedFields.add(col.getName())) // 只保留第一次出现的字段
                    .map(col -> {
                        TableFieldDTO dto = new TableFieldDTO();
                        dto.setName(col.getName());
                        dto.setType(col.getType());
                        dto.setLength(col.getLength());
                        dto.setNotNull(0);
                        dto.setMgcomment(col.getComment());
                        return dto;
                    })
                    .collect(Collectors.toList());
                    
            // 构建TableDTO
            TableDTO tableDTO = new TableDTO();
            tableDTO.setName(tableName);
                    
            // 获取建表语句
            String createTableSQL = tableService.generateCreateTableSQL(
                    tableDTO,
                    fields,
                    dataSourceService.getById(dataSourceId).getType()
            );
            
            // 获取数据
            List<Map<String, Object>> data = dataSourceService.executeQuery(
                    dataSourceId,
                    "SELECT * FROM " + tableName
            );
            
            // 使用 BufferedWriter 提高写入性能
            writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
            writer.write(createTableSQL + ";\n\n");
            
            // 生成带实际数据的INSERT语句
            for (Map<String, Object> row : data) {
                String insertSQL = generateInsertSQLWithData(tableName, fields, row);
                writer.write(insertSQL + ";\n");
            }
            writer.flush();
        } catch (Exception e) {
            log.error("导出SQL失败: {}", e.getMessage(), e);
            throw new IOException("导出SQL失败: " + e.getMessage(), e);
        } finally {
            // 只有在不是ZIP输出流的情况下才关闭writer
            if (writer != null && !(out instanceof ZipOutputStream)) {
                try {
                    writer.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * 导出表数据为DMP格式
     */
    private void exportTableAsDMP(String dataSourceId, String tableName, OutputStream out) throws IOException {
        BufferedWriter writer = null;
        try {
            // 获取数据源信息
            DataSource ds = dataSourceService.getById(dataSourceId);
            
            writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
            
            // 写入头部信息
            writer.write("-- Export dump file\n");
            writer.write("-- Host: " + ds.getUrl() + "\n");
            writer.write("-- Generation Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n");
            writer.write("-- Server version: " + ds.getType() + "\n");
            writer.write("-- Table: " + tableName + "\n\n");
            
            // 获取表结构
            List<ColumnInfo> columns = dataSourceService.getTableColumns(dataSourceId.toString(), tableName)
                    .getData();
            
            // 使用LinkedHashSet去重，保持字段顺序
            Set<String> processedFields = new LinkedHashSet<>();
            List<TableFieldDTO> fields = columns.stream()
                    .filter(col -> processedFields.add(col.getName())) // 只保留第一次出现的字段
                    .map(col -> {
                        TableFieldDTO dto = new TableFieldDTO();
                        dto.setName(col.getName());
                        dto.setType(col.getType());
                        dto.setLength(col.getLength());
                        dto.setNotNull(0);
                        dto.setMgcomment(col.getComment());
                        return dto;
                    })
                    .collect(Collectors.toList());
                    
            TableDTO tableDTO = new TableDTO();
            tableDTO.setName(tableName);
                    
            String createTableSQL = tableService.generateCreateTableSQL(
                    tableDTO,
                    fields,
                    ds.getType()
            );
            
            // 获取数据
            List<Map<String, Object>> data = dataSourceService.executeQuery(
                    dataSourceId,
                    "SELECT * FROM " + tableName
            );
            
            writer.write(createTableSQL + ";\n\n");
            
            // 生成带实际数据的INSERT语句
            for (Map<String, Object> row : data) {
                String insertSQL = generateInsertSQLWithData(tableName, fields, row);
                writer.write(insertSQL + ";\n");
            }
            writer.flush();
        } catch (Exception e) {
            log.error("导出DMP失败: {}", e.getMessage(), e);
            throw new IOException("导出DMP失败: " + e.getMessage(), e);
        } finally {
            // 只有在不是ZIP输出流的情况下才关闭writer
            if (writer != null && !(out instanceof ZipOutputStream)) {
                try {
                    writer.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * 导出表数据为TXT格式
     */
    private void exportTableAsTXT(String dataSourceId, String tableName, OutputStream out) throws IOException {
        try {
            List<Map<String, Object>> data = dataSourceService.executeQuery(
                    dataSourceId,
                    "SELECT * FROM " + tableName
            );
            
            // Don't use try-with-resources here as it might close the underlying ZIP stream
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), true);
            if (!data.isEmpty()) {
                writer.println(String.join("\t", data.get(0).keySet()));
            }
            
            for (Map<String, Object> row : data) {
                writer.println(row.values().stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining("\t")));
            }
            writer.flush();
            // Don't close the writer as it will close the underlying stream
        } catch (Exception e) {
            throw new IOException("Failed to export TXT: " + e.getMessage(), e);
        }
    }

    /**
     * 格式化CSV值
     */
    private String formatCSVValue(Object value) {
        if (value == null) {
            return "";
        }
        String str = String.valueOf(value);
        if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }

    /**
     * 设置Excel单元格值
     */
    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof Date) {
            cell.setCellValue((Date) value);
        } else {
            cell.setCellValue(String.valueOf(value));
        }
    }

    @PostMapping("/{dataSourceId}/import")
    @ApiOperation("导入数据")
    public R<Boolean> importData(
            @PathVariable String dataSourceId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(required = true) Integer strategy) {
        try {
            if (files == null || files.isEmpty()) {
                return R.fail("请选择要导入的文件");
            }

            // 如果是全部取消策略(1)，先检查所有表是否存在
            if (strategy == 1) {
                List<String> existingTables = new ArrayList<>();
                for (MultipartFile file : files) {
                    String fileName = file.getOriginalFilename();
                    if (fileName == null) {
                        continue;
                    }
                    
                    // 从文件名中提取表名
                    String tableName = fileName.substring(0, fileName.lastIndexOf("."));
                    if (tableService.checkTableExists(dataSourceId, tableName)) {
                        existingTables.add(tableName);
                    }
                }
                
                if (!existingTables.isEmpty()) {
                    return R.fail(String.format("以下表已存在：%s", String.join(", ", existingTables)));
                }
            }

            // 处理每个文件
            for (MultipartFile file : files) {
                String fileName = file.getOriginalFilename();
                if (fileName == null) {
                    continue;
                }

                // 检查文件类型
                String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                
                // 如果是ZIP文件，处理多文件导入
                if ("zip".equals(fileExtension)) {
                    R<Boolean> zipResult = handleZipImport(dataSourceId, file, strategy);
                    if (!Boolean.TRUE.equals(zipResult.getData())) {
                        return zipResult;
                    }
                    continue;
                }
                
                // 单文件导入
                String tableName = fileName.substring(0, fileName.lastIndexOf("."));
                R<Boolean> result = handleSingleFileImport(dataSourceId, file, tableName, strategy);
                if (!Boolean.TRUE.equals(result.getData())) {
                    return result;
                }
            }

            return R.ok(true);
        } catch (Exception e) {
            log.error("导入数据失败", e);
            return R.fail("导入数据失败：" + e.getMessage());
        }
    }

    /**
     * 处理ZIP文件导入
     */
    private R<Boolean> handleZipImport(String dataSourceId, MultipartFile file, Integer strategy) throws IOException {
        try (ZipInputStream zipIn = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                String entryName = entry.getName();
                if (entry.isDirectory()) {
                    continue;
                }

                // 从文件名中提取表名和格式
                String fileName = entryName;
                String tableName = fileName.substring(0, fileName.lastIndexOf("."));
                String fileType = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

                // 读取ZIP条目内容
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = zipIn.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                
                // 将内容转换为MultipartFile
                MultipartFile mockFile = new MultipartFile() {
                    @Override
                    public String getName() {
                        return "";
                    }

                    @Override
                    public String getOriginalFilename() {
                        return "";
                    }

                    @Override
                    public String getContentType() {
                        return "";
                    }

                    @Override
                    public boolean isEmpty() {
                        return false;
                    }

                    @Override
                    public long getSize() {
                        return 0;
                    }

                    @Override
                    public byte[] getBytes() throws IOException {
                        return new byte[0];
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        return null;
                    }

                    @Override
                    public void transferTo(File dest) throws IOException, IllegalStateException {

                    }
                };
//                MockMultipartFile mockFile = new MockMultipartFile(
//                    fileName,
//                    fileName,
//                    getMimeType(fileType),
//                    baos.toByteArray()
//                );

                // 导入单个文件
                R<Boolean> result = handleSingleFileImport(dataSourceId, mockFile, tableName, strategy);
                if (!Boolean.TRUE.equals(result.getData())) {
                    return result;
                }

                zipIn.closeEntry();
            }
            return R.ok(true);
        }
    }

    /**
     * 处理单文件导入
     */
    private R<Boolean> handleSingleFileImport(String dataSourceId, MultipartFile file, String tableName, Integer strategy) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            return R.fail("文件名不能为空");
        }

        // 获取文件类型
        String fileType = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        
        // 检查表是否存在
        try {
            boolean exists = tableService.checkTableExists(dataSourceId, tableName);
            if (exists) {
                switch (strategy) {
                    case 1: // 全部取消
                        return R.fail(String.format("表 %s 已存在", tableName));
                    case 2: // 跳过
                        log.info("表 {} 已存在，根据策略跳过", tableName);
                        return R.ok(true);
                    case 3: // 覆盖
                        // 如果表存在且选择覆盖，先删除原表
                        try (Connection conn = dataSourceService.getConnection(dataSourceId)) {
                            try (Statement stmt = conn.createStatement()) {
                                stmt.execute("DROP TABLE IF EXISTS " + tableName);
                            }
                        }
                        break;
                    default:
                        return R.fail("无效的策略选项");
                }
            }

            // 解析数据
            List<Map<String, Object>> data;
            List<TableFieldDTO> fields;
            
            try {
                switch (fileType) {
                    case "csv":
                        data = parseCsvFile(file);
                        break;
                    case "xlsx":
                        data = parseExcelFile(file);
                        break;
                    case "json":
                        data = parseJsonFile(file);
                        break;
                    case "xml":
                        data = parseXmlFile(file);
                        break;
                    case "sql":
                    case "dmp":
                        return importSqlFile(dataSourceId, file, strategy == 3);
                    case "txt":
                        data = parseTxtFile(file);
                        break;
                    default:
                        return R.fail("不支持的文件类型：" + fileType);
                }

                if (data.isEmpty()) {
                    return R.fail("文件内容为空");
                }

                // 从数据中推断字段信息
                fields = inferFieldsFromData(data.get(0));

                // 获取数据源类型
                DataSource ds = dataSourceService.getById(dataSourceId);
                if (ds == null) {
                    return R.fail("数据源不存在");
                }

                // 构建TableDTO
                TableDTO tableDTO = new TableDTO();
                tableDTO.setName(tableName);

                // 创建表并插入数据
                createTableAndInsertData(dataSourceId, tableDTO, fields, data, ds.getType());

                return R.ok(true);
            } catch (Exception e) {
                log.error("导入数据失败", e);
                return R.fail("导入数据失败：" + e.getMessage());
            }
        } catch (Exception e) {
            log.error("导入失败", e);
            return R.fail("导入失败：" + e.getMessage());
        }
    }



    /**
     * 解析CSV文件
     */
    private List<Map<String, Object>> parseCsvFile(MultipartFile file) throws IOException {
        List<Map<String, Object>> data = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return data;
            }

            String[] headers = headerLine.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
            
            // 移除引号
            for (int i = 0; i < headers.length; i++) {
                headers[i] = headers[i].replaceAll("^\"|\"$", "").trim();
            }

            String line;
            while ((line = reader.readLine()) != null) {
                Map<String, Object> row = new HashMap<>();
                String[] values = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                
                for (int i = 0; i < headers.length && i < values.length; i++) {
                    String value = values[i].replaceAll("^\"|\"$", "").trim();
                    row.put(headers[i], value);
                }
                data.add(row);
            }
        }
        return data;
    }

    /**
     * 解析Excel文件
     */
    private List<Map<String, Object>> parseExcelFile(MultipartFile file) throws IOException {
        List<Map<String, Object>> data = new ArrayList<>();
        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            XSSFSheet sheet = workbook.getSheetAt(0);
            
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return data;
            }

            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(cell.getStringCellValue().trim());
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Map<String, Object> rowData = new HashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j);
                    if (cell != null) {
                        rowData.put(headers.get(j), getCellValue(cell));
                    }
                }
                data.add(rowData);
            }
        }
        return data;
    }

    /**
     * 获取Excel单元格值
     */
    private Object getCellValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                }
                return cell.getNumericCellValue();
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }

    /**
     * 解析JSON文件
     */
    private List<Map<String, Object>> parseJsonFile(MultipartFile file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(file.getInputStream(), new TypeReference<List<Map<String, Object>>>() {});
    }

    /**
     * 解析XML文件
     */
    private List<Map<String, Object>> parseXmlFile(MultipartFile file) throws Exception {
        List<Map<String, Object>> data = new ArrayList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(file.getInputStream());

        NodeList rows = document.getElementsByTagName("row");
        for (int i = 0; i < rows.getLength(); i++) {
            Map<String, Object> row = new HashMap<>();
            NodeList fields = rows.item(i).getChildNodes();
            
            for (int j = 0; j < fields.getLength(); j++) {
                Node field = fields.item(j);
                if (field.getNodeType() == Node.ELEMENT_NODE) {
                    row.put(field.getNodeName(), field.getTextContent());
                }
            }
            data.add(row);
        }
        return data;
    }

    /**
     * 解析TXT文件
     */
    private List<Map<String, Object>> parseTxtFile(MultipartFile file) throws IOException {
        List<Map<String, Object>> data = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return data;
            }

            String[] headers = headerLine.split("\t");
            String line;
            while ((line = reader.readLine()) != null) {
                Map<String, Object> row = new HashMap<>();
                String[] values = line.split("\t");
                for (int i = 0; i < headers.length && i < values.length; i++) {
                    row.put(headers[i].trim(), values[i].trim());
                }
                data.add(row);
            }
        }
        return data;
    }

    /**
     * 导入SQL文件
     */
    private R<Boolean> importSqlFile(String dataSourceId, MultipartFile file, boolean override) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             Connection conn = dataSourceService.getConnection(dataSourceId)) {
            
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                StringBuilder sqlBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("--")) {
                        continue;
                    }
                    
                    sqlBuilder.append(line);
                    if (line.endsWith(";")) {
                        String sql = sqlBuilder.toString();
                        stmt.execute(sql.substring(0, sql.length() - 1));
                        sqlBuilder.setLength(0);
                    }
                }
                
                conn.commit();
                return R.ok(true);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            log.error("导入SQL文件失败", e);
            return R.fail("导入SQL文件失败：" + e.getMessage());
        }
    }

    /**
     * 从数据中推断字段信息
     */
    private List<TableFieldDTO> inferFieldsFromData(Map<String, Object> data) {
        List<TableFieldDTO> fields = new ArrayList<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            TableFieldDTO field = new TableFieldDTO();
            field.setName(entry.getKey());
            field.setType(getFieldType(entry.getValue()));
            field.setNotNull(0);
            fields.add(field);
        }
        return fields;
    }

    /**
     * 获取字段类型
     */
    private String getFieldType(Object value) {
        if (value == null) {
            return "VARCHAR(255)";
        }
        
        // 基于值类型判断数据库字段类型
        if (value instanceof String) {
            String strValue = (String) value;
            if (strValue.length() > 4000) {
                return "TEXT";
            }
            return "VARCHAR(255)";
        } else if (value instanceof Integer) {
            return "INTEGER";
        } else if (value instanceof Long) {
            return "BIGINT";
        } else if (value instanceof Double || value instanceof Float) {
            return "DECIMAL(19,6)";
        } else if (value instanceof Boolean) {
            return "BOOLEAN";
        } else if (value instanceof Date) {
            return "TIMESTAMP";
        } else if (value instanceof byte[]) {
            return "BLOB";
        }
        
        // 默认使用VARCHAR类型
        return "VARCHAR(255)";
    }

    /**
     * 检查表是否存在
     */
    @PostMapping("/{dataSourceId}/tables/check-exists")
    @ApiOperation("检查表是否存在")
    public R<List<String>> checkTablesExist(
            @PathVariable String dataSourceId,
            @RequestBody List<String> tableNames) {
        try {
            if (tableNames == null || tableNames.isEmpty()) {
                return R.fail("请提供要检查的表名");
            }

            List<String> existingTables = new ArrayList<>();
            for (String tableName : tableNames) {
                if (tableService.checkTableExists(dataSourceId, tableName)) {
                    existingTables.add(tableName);
                }
            }

            return R.ok(existingTables);
        } catch (Exception e) {
            log.error("检查表是否存在失败", e);
            return R.fail("检查表是否存在失败：" + e.getMessage());
        }
    }

    /**
     * 获取数据库及其所有表信息
     */
    @GetMapping("/dbs/all-tables")
    @ApiOperation("获取所有数据源及其表信息")
    public R<Map<String, Object>> getAllDataSourcesAndTables(
            @RequestParam(required = false, defaultValue = "table") String types,
            @RequestParam(required = false) String searchText) {
        try {
            // 获取所有数据源
            List<DataSource> dataSources = dataSourceService.list();
            Map<String, Object> result = new HashMap<>();
            List<Map<String, Object>> dataSourcesList = new ArrayList<>();
            
            // 遍历每个数据源，获取其表信息
            for (DataSource ds : dataSources) {
                try {
                    Map<String, Object> dsInfo = new HashMap<>();
                    dsInfo.put("id", ds.getId());
                    dsInfo.put("name", ds.getName());
                    dsInfo.put("type", ds.getType());
                    dsInfo.put("status", ds.getStatus());
                    
                    // 获取此数据源的所有表
                    List<Map<String, Object>> tables = dataSourceService.getTableList(ds.getId(), types, searchText);
                    dsInfo.put("tables", tables);
                    
                    dataSourcesList.add(dsInfo);
                } catch (Exception e) {
                    log.warn("获取数据源[{}]的表失败: {}", ds.getName(), e.getMessage());
                    // 将错误信息添加到数据源信息中，但仍然包含这个数据源
                    Map<String, Object> dsInfo = new HashMap<>();
                    dsInfo.put("id", ds.getId());
                    dsInfo.put("name", ds.getName());
                    dsInfo.put("type", ds.getType());
                    dsInfo.put("status", ds.getStatus());
                    dsInfo.put("error", e.getMessage());
                    dsInfo.put("tables", new ArrayList<>());
                    
                    dataSourcesList.add(dsInfo);
                }
            }
            
            result.put("dataSources", dataSourcesList);
            return R.ok(result);
        } catch (Exception e) {
            log.error("获取所有数据源及表信息失败", e);
            return R.fail("获取所有数据源及表信息失败：" + e.getMessage());
        }
    }

    /**
     * 生成带数据的INSERT语句
     */
    private String generateInsertSQLWithData(String tableName, List<TableFieldDTO> fields, Map<String, Object> data) {
        StringBuilder sql = new StringBuilder();
        List<String> fieldNames = new ArrayList<>();
        List<String> values = new ArrayList<>();
        
        // 只包含非空值的字段
        for (TableFieldDTO field : fields) {
            Object value = data.get(field.getName());
            if (value != null) {
                fieldNames.add(field.getName());
                values.add(formatSQLValue(value));
            }
        }
        
        sql.append("INSERT INTO ").append(tableName).append(" (");
        sql.append(String.join(", ", fieldNames));
        sql.append(") VALUES (");
        sql.append(String.join(", ", values));
        sql.append(")");
        
        return sql.toString();
    }
    
    /**
     * 格式化SQL值
     */
    private String formatSQLValue(Object value) {
        if (value == null) {
            return "NULL";
        } else if (value instanceof String) {
            return "'" + ((String) value).replace("'", "''") + "'";
        } else if (value instanceof Date) {
            return "'" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(value) + "'";
        } else if (value instanceof Boolean) {
            return (Boolean) value ? "1" : "0";
        } else {
            return String.valueOf(value);
        }
    }

    /**
     * 通用元数据API，可以处理所有类型的数据源
     */
    @GetMapping("/{dataSourceId}/metadata")
    @ApiOperation("获取数据源元数据")
    public R<List<Map<String, Object>>> getDatasourceMetadata(
            @PathVariable String dataSourceId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String filter) {
        try {
            log.info("获取数据源[{}]的元数据，类型：{}，过滤条件：{}", dataSourceId, type, filter);
            
            // 获取数据源
            DataSource dataSource = dataSourceService.getById(dataSourceId);
            if (dataSource == null) {
                return R.fail("数据源不存在");
            }
            
            // 根据数据源类型和请求的元数据类型，获取相应的元数据
            List<Map<String, Object>> metadata = dataSourceService.getDataSourceMetadata(dataSource, type, filter);
            
            return R.ok(metadata);
        } catch (Exception e) {
            log.error("获取数据源元数据失败", e);
            return R.fail("获取数据源元数据失败：" + e.getMessage());
        }
    }

    /**
     * 获取数据源统计信息
     */
    @GetMapping("/statistics")
    @ApiOperation("获取数据源统计信息")
    public R<Map<String, Object>> getDataSourceStatistics() {
        try {
            Map<String, Object> statistics = new HashMap<>();
            
            // 直接使用数据库查询统计信息，避免加载所有数据源对象
            LambdaQueryWrapper<DataSource> wrapper = new LambdaQueryWrapper<>();
            int totalSources = (int) dataSourceService.count();
            statistics.put("totalSources", totalSources);
            
            // 统计启用的数据源数量
            LambdaQueryWrapper<DataSource> enabledWrapper = new LambdaQueryWrapper<>();
            enabledWrapper.eq(DataSource::getStatus, true);
            int activeSources = (int) dataSourceService.count(enabledWrapper);
            statistics.put("activeSources", activeSources);
            
            // 连接失败的数据源数量
            int failedSources = totalSources - activeSources;
            statistics.put("failedSources", failedSources);
            
            // 计算连接成功率
            double connectionRate = totalSources > 0 ? (double) activeSources / totalSources * 100 : 0;
            statistics.put("connectionRate", Math.round(connectionRate * 10) / 10.0); // 保留一位小数
            
            // 统计数据库类型数量
            LambdaQueryWrapper<DataSource> distinctWrapper = new LambdaQueryWrapper<>();
            List<DataSource> allDs = dataSourceService.list(distinctWrapper);
            Set<String> dbTypes = allDs.stream().map(DataSource::getType).collect(Collectors.toSet());
            statistics.put("dbTypeCount", dbTypes.size());
            statistics.put("supportedTypes", 36); // 支持的数据库类型总数
            
            // 计算增长率（简化计算，避免额外查询）
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH, -1);
            Date oneMonthAgo = calendar.getTime();
            
            int lastMonthCount = 0;
            int thisMonthCount = 0;
            
            for (DataSource ds : allDs) {
                if (ds.getCreateTime() != null) {
                    if (ds.getCreateTime().after(oneMonthAgo)) {
                        thisMonthCount++;
                    } else {
                        lastMonthCount++;
                    }
                }
            }
            
            // 最近添加的数据源
            statistics.put("recentlyAdded", thisMonthCount);
            
            // 计算增长率 - 使用前端期望的键名
            double totalSourcesGrowth = lastMonthCount > 0 ? 
                    (double) thisMonthCount / lastMonthCount * 100 : 0;
            statistics.put("totalSourcesGrowth", (double) Math.round(totalSourcesGrowth * 10) / 10.0);
            
            // 连接成功率增长（模拟值，实际可能需要历史数据）
            statistics.put("connectionRateGrowth", 2.1);
            
            // 连接失败数变化率（模拟值）
            statistics.put("failedSourcesChange", -15.2);
            
            // 添加每周新增数量
            calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, -7);
            Date oneWeekAgo = calendar.getTime();
            
            int weeklyCount = 0;
            for (DataSource ds : allDs) {
                if (ds.getCreateTime() != null && ds.getCreateTime().after(oneWeekAgo)) {
                    weeklyCount++;
                }
            }
            statistics.put("weeklyGrowth", weeklyCount);
            
            return R.ok(statistics);
        } catch (Exception e) {
            log.error("获取数据源统计信息失败", e);
            return R.fail("获取数据源统计信息失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取数据源类别
     */
    private String getDataSourceCategory(DataSource dataSource) {
        String type = dataSource.getType().toLowerCase();
        
        // 关系型数据库
        if (type.contains("mysql") || type.contains("oracle") || type.contains("postgres") || 
            type.contains("sqlserver") || type.contains("db2") || type.contains("dm") || 
            type.contains("kingbase") || type.contains("oscar") || type.contains("gbase")) {
            return "relational";
        }
        
        // 文件系统
        if (type.contains("ftp") || type.contains("sftp") || type.contains("hdfs") || 
            type.contains("minio") || type.contains("s3") || type.contains("oss")) {
            return "filesystem";
        }
        
        // 消息队列
        if (type.contains("kafka") || type.contains("rabbitmq") || type.contains("rocketmq")) {
            return "queue";
        }
        
        // 大数据平台
        if (type.contains("hive") || type.contains("spark") || type.contains("flink") || 
            type.contains("doris") || type.contains("starrocks") || type.contains("clickhouse")) {
            return "bigdata";
        }
        
        // NoSQL数据库
        if (type.contains("mongo") || type.contains("redis") || type.contains("elasticsearch") || 
            type.contains("cassandra") || type.contains("hbase")) {
            return "nosql";
        }
        
        return "other";
    }
}
