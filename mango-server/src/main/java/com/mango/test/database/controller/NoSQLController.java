package com.mango.test.database.controller;

import com.mango.test.database.model.dto.nosql.*;
import com.mango.test.database.service.NoSQLService;
import com.mango.test.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@Api(tags = "NoSQL数据库")
@RestController
@RequestMapping("/api/nosql")
public class NoSQLController {

    @Autowired
    private NoSQLService noSQLService;

    @GetMapping("/{dataSourceId}/collections")
    @ApiOperation("获取所有集合")
    public R<List<Map<String, Object>>> getCollections(
            @PathVariable String dataSourceId,
            @RequestParam(required = false) String searchText) {
        try {
            List<Map<String, Object>> collections = noSQLService.getCollections(dataSourceId, searchText);
            return R.ok(collections);
        } catch (Exception e) {
            log.error("获取集合列表失败", e);
            return R.fail("获取集合列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{dataSourceId}/collection/{collectionName}/info")
    @ApiOperation("获取集合信息")
    public R<Map<String, Object>> getCollectionInfo(
            @PathVariable String dataSourceId,
            @PathVariable String collectionName) {
        try {
            Map<String, Object> info = noSQLService.getCollectionInfo(dataSourceId, collectionName);
            return R.ok(info);
        } catch (Exception e) {
            log.error("获取集合信息失败", e);
            return R.fail("获取集合信息失败: " + e.getMessage());
        }
    }

    @GetMapping("/{dataSourceId}/collection/{collectionName}/schema")
    @ApiOperation("获取集合Schema")
    public R<Map<String, Object>> getCollectionSchema(
            @PathVariable String dataSourceId,
            @PathVariable String collectionName) {
        try {
            Map<String, Object> schema = noSQLService.getCollectionSchema(dataSourceId, collectionName);
            return R.ok(schema);
        } catch (Exception e) {
            log.error("获取集合Schema失败", e);
            return R.fail("获取集合Schema失败: " + e.getMessage());
        }
    }

    @PostMapping("/{dataSourceId}/collection/{collectionName}/query")
    @ApiOperation("查询集合数据")
    public R<Map<String, Object>> queryCollection(
            @PathVariable String dataSourceId,
            @PathVariable String collectionName,
            @RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> result = noSQLService.queryCollection(dataSourceId, collectionName, request);
            return R.ok(result);
        } catch (Exception e) {
            log.error("查询集合数据失败", e);
            return R.fail("查询集合数据失败: " + e.getMessage());
        }
    }

    @PostMapping("/{dataSourceId}/collection/{collectionName}/document")
    @ApiOperation("插入文档")
    public R<String> insertDocument(
            @PathVariable String dataSourceId,
            @PathVariable String collectionName,
            @RequestBody Map<String, Object> document) {
        try {
            String id = noSQLService.insertDocument(dataSourceId, collectionName, document);
            return R.ok("文档插入成功", id);
        } catch (Exception e) {
            log.error("插入文档失败", e);
            return R.fail("插入文档失败: " + e.getMessage());
        }
    }

    @PutMapping("/{dataSourceId}/collection/{collectionName}/document/{documentId}")
    @ApiOperation("更新文档")
    public R<Boolean> updateDocument(
            @PathVariable String dataSourceId,
            @PathVariable String collectionName,
            @PathVariable String documentId,
            @RequestBody Map<String, Object> document) {
        try {
            boolean success = noSQLService.updateDocument(dataSourceId, collectionName, documentId, document);
            return R.ok("文档更新成功", success);
        } catch (Exception e) {
            log.error("更新文档失败", e);
            return R.fail("更新文档失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{dataSourceId}/collection/{collectionName}/document/{documentId}")
    @ApiOperation("删除文档")
    public R<Boolean> deleteDocument(
            @PathVariable String dataSourceId,
            @PathVariable String collectionName,
            @PathVariable String documentId) {
        try {
            boolean success = noSQLService.deleteDocument(dataSourceId, collectionName, documentId);
            return R.ok("文档删除成功", success);
        } catch (Exception e) {
            log.error("删除文档失败", e);
            return R.fail("删除文档失败: " + e.getMessage());
        }
    }

    @PostMapping("/{dataSourceId}/collection")
    @ApiOperation("创建集合")
    public R<Boolean> createCollection(
            @PathVariable String dataSourceId,
            @RequestBody Map<String, Object> request) {
        try {
            boolean success = noSQLService.createCollection(dataSourceId, request);
            return R.ok("集合创建成功", success);
        } catch (Exception e) {
            log.error("创建集合失败", e);
            return R.fail("创建集合失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{dataSourceId}/collection/{collectionName}")
    @ApiOperation("删除集合")
    public R<Boolean> deleteCollection(
            @PathVariable String dataSourceId,
            @PathVariable String collectionName) {
        try {
            boolean success = noSQLService.deleteCollection(dataSourceId, collectionName);
            return R.ok("集合删除成功", success);
        } catch (Exception e) {
            log.error("删除集合失败", e);
            return R.fail("删除集合失败: " + e.getMessage());
        }
    }

    @PostMapping("/{dataSourceId}/execute")
    @ApiOperation("执行命令")
    public R<Object> executeCommand(
            @PathVariable String dataSourceId,
            @RequestBody Map<String, Object> request) {
        try {
            Object result = noSQLService.executeCommand(dataSourceId, request);
            return R.ok(result);
        } catch (Exception e) {
            log.error("执行命令失败", e);
            return R.fail("执行命令失败: " + e.getMessage());
        }
    }

    @GetMapping("/{dataSourceId}/indices/{collectionName}")
    @ApiOperation("获取集合索引")
    public R<List<Map<String, Object>>> getIndices(
            @PathVariable String dataSourceId,
            @PathVariable String collectionName) {
        try {
            List<Map<String, Object>> indices = noSQLService.getIndices(dataSourceId, collectionName);
            return R.ok(indices);
        } catch (Exception e) {
            log.error("获取索引列表失败", e);
            return R.fail("获取索引列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/{dataSourceId}/index")
    @ApiOperation("创建索引")
    public R<Boolean> createIndex(
            @PathVariable String dataSourceId,
            @RequestBody Map<String, Object> request) {
        try {
            boolean success = noSQLService.createIndex(dataSourceId, request);
            return R.ok("索引创建成功", success);
        } catch (Exception e) {
            log.error("创建索引失败", e);
            return R.fail("创建索引失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{dataSourceId}/collection/{collectionName}/index/{indexName}")
    @ApiOperation("删除索引")
    public R<Boolean> deleteIndex(
            @PathVariable String dataSourceId,
            @PathVariable String collectionName,
            @PathVariable String indexName) {
        try {
            boolean success = noSQLService.deleteIndex(dataSourceId, collectionName, indexName);
            return R.ok("索引删除成功", success);
        } catch (Exception e) {
            log.error("删除索引失败", e);
            return R.fail("删除索引失败: " + e.getMessage());
        }
    }
} 