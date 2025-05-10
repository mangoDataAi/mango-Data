package com.mango.test.database.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mango.test.database.entity.Table;
import com.mango.test.database.service.TableService;
import com.mango.test.dto.MaterializeRequest;
import com.mango.test.dto.TableDTO;
import com.mango.test.dto.TableQueryDTO;
import com.mango.test.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Slf4j
@Api(tags = "表管理")
@RestController
@RequestMapping("/api/table")
public class TableController {

    @Autowired
    private TableService tableService;

    @GetMapping("/list/{groupId}")
    @ApiOperation("获取表列表")
    public R<Page<Table>> list(
            @PathVariable String groupId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            TableQueryDTO query) {
        try {
            log.info("获取表列表, groupId: {}, pageNum: {}, pageSize: {}, query: {}", 
                groupId, pageNum, pageSize, query);
            
            Page<Table> page = tableService.getTableList(groupId, pageNum, pageSize, query);
            return R.ok(page);
        } catch (Exception e) {
            log.error("获取表列表失败", e);
            return R.fail("获取表列表失败：" + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @ApiOperation("获取表详情")
    public R<TableDTO> detail(@PathVariable String id) {
        return R.ok(tableService.getTableDetail(id));
    }

    @PostMapping("/{groupId}")
    @ApiOperation("创建表")
    public R<Void> create(
            @PathVariable String groupId,
            @RequestBody TableDTO tableDTO
    ) {
        tableService.createTable(groupId, tableDTO);
        return R.ok();
    }

    @PutMapping("/{id}")
    @ApiOperation("更新表")
    public R<Void> update(
            @PathVariable String id,
            @RequestBody TableDTO tableDTO
    ) {
        tableService.updateTable(id, tableDTO);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除表")
    public R<Void> delete(@PathVariable String id) {
        tableService.deleteTable(id);
        return R.ok();
    }

    @PostMapping("/{groupId}/sql")
    @ApiOperation("SQL创建表")
    public R<Void> createBySQL(
            @PathVariable String groupId,
            @RequestBody String sql
    ) {
        tableService.createTableBySQL(groupId, sql);
        return R.ok();
    }

    @PostMapping("/{groupId}/reference/{sourceId}")
    @ApiOperation("引用已有表")
    public R<Void> referenceExistingTables(
            @PathVariable String groupId,
            @PathVariable String sourceId,
            @RequestBody List<String> tables
    ) {
        try {
            tableService.referenceExistingTables(groupId, sourceId, tables);
            return R.ok();
        } catch (Exception e) {
            log.error("引用表失败", e);
            return R.fail("引用表失败：" + e.getMessage());
        }
    }

    @PostMapping("/materialize")
    @ApiOperation("物化表")
    public R<Void> materializeTable(@RequestBody MaterializeRequest request) {
        try {
            tableService.materializeTables(request.getSourceId(), request.getTables());
            return R.ok();
        } catch (Exception e) {
            log.error("物化表失败", e);
            return R.fail("物化表失败：" + e.getMessage());
        }
    }

    @GetMapping("/{tableId}/template/{format}")
    @ApiOperation("下载表模板")
    public void downloadTemplate(
            @PathVariable String tableId,
            @PathVariable String format,
            HttpServletResponse response
    ) {
        try {
            tableService.generateTemplate(tableId, format.toLowerCase(), response);
        } catch (Exception e) {
            log.error("生成模板失败", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/template/empty/{format}")
    @ApiOperation("下载空模板")
    public void downloadEmptyTemplate(
            @PathVariable String format,
            HttpServletResponse response
    ) {
        try {
            tableService.generateEmptyTemplate(format.toLowerCase(), response);
        } catch (Exception e) {
            log.error("生成空模板失败", e);
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.getWriter().write("{\"code\":500,\"msg\":\"" + e.getMessage() + "\"}");
            } catch (IOException ex) {
                log.error("写入错误响应失败", ex);
            }
        }
    }
}
