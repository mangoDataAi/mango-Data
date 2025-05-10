package com.mango.test.database.controller;

import com.mango.test.vo.R;
import com.mango.test.database.service.GraphService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@Api(tags = "图数据库")
@RestController
@RequestMapping("/api/graph")
public class GraphController {

    @Autowired
    private GraphService graphService;

    @GetMapping("/{dataSourceId}/schema")
    @ApiOperation("获取图数据库Schema")
    public R<Map<String, Object>> getGraphSchema(@PathVariable String dataSourceId) {
        try {
            Map<String, Object> schema = graphService.getGraphSchema(dataSourceId);
            return R.ok(schema);
        } catch (Exception e) {
            log.error("获取图数据库Schema失败", e);
            return R.fail("获取图数据库Schema失败: " + e.getMessage());
        }
    }

    @PostMapping("/{dataSourceId}/query")
    @ApiOperation("执行图数据库查询")
    public R<Map<String, Object>> executeQuery(
            @PathVariable String dataSourceId,
            @RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> result = graphService.executeQuery(dataSourceId, (String) request.get("query"));
            return R.ok(result);
        } catch (Exception e) {
            log.error("执行图数据库查询失败", e);
            return R.fail("执行图数据库查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/{dataSourceId}/node/{nodeLabel}/properties")
    @ApiOperation("获取节点属性")
    public R<List<Map<String, Object>>> getNodeProperties(
            @PathVariable String dataSourceId,
            @PathVariable String nodeLabel) {
        try {
            List<Map<String, Object>> properties = graphService.getNodeProperties(dataSourceId, nodeLabel);
            return R.ok(properties);
        } catch (Exception e) {
            log.error("获取节点属性失败", e);
            return R.fail("获取节点属性失败: " + e.getMessage());
        }
    }

    @GetMapping("/{dataSourceId}/edge/{edgeType}/properties")
    @ApiOperation("获取边属性")
    public R<List<Map<String, Object>>> getEdgeProperties(
            @PathVariable String dataSourceId,
            @PathVariable String edgeType) {
        try {
            List<Map<String, Object>> properties = graphService.getEdgeProperties(dataSourceId, edgeType);
            return R.ok(properties);
        } catch (Exception e) {
            log.error("获取边属性失败", e);
            return R.fail("获取边属性失败: " + e.getMessage());
        }
    }

    @PostMapping("/{dataSourceId}/node")
    @ApiOperation("创建节点")
    public R<String> createNode(
            @PathVariable String dataSourceId,
            @RequestBody Map<String, Object> node) {
        try {
            String nodeId = graphService.createNode(dataSourceId, node);
            return R.ok("节点创建成功", nodeId);
        } catch (Exception e) {
            log.error("创建节点失败", e);
            return R.fail("创建节点失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{dataSourceId}/node/{nodeId}")
    @ApiOperation("删除节点")
    public R<Boolean> deleteNode(
            @PathVariable String dataSourceId,
            @PathVariable String nodeId) {
        try {
            graphService.deleteNode(dataSourceId, nodeId);
            return R.ok("节点删除成功", true);
        } catch (Exception e) {
            log.error("删除节点失败", e);
            return R.fail("删除节点失败: " + e.getMessage());
        }
    }

    @PostMapping("/{dataSourceId}/relationship")
    @ApiOperation("创建关系")
    public R<String> createRelationship(
            @PathVariable String dataSourceId,
            @RequestBody Map<String, Object> relationship) {
        try {
            String relationshipId = graphService.createRelationship(dataSourceId, relationship);
            return R.ok("关系创建成功", relationshipId);
        } catch (Exception e) {
            log.error("创建关系失败", e);
            return R.fail("创建关系失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{dataSourceId}/relationship/{relationshipId}")
    @ApiOperation("删除关系")
    public R<Boolean> deleteRelationship(
            @PathVariable String dataSourceId,
            @PathVariable String relationshipId) {
        try {
            graphService.deleteRelationship(dataSourceId, relationshipId);
            return R.ok("关系删除成功", true);
        } catch (Exception e) {
            log.error("删除关系失败", e);
            return R.fail("删除关系失败: " + e.getMessage());
        }
    }

    @PostMapping("/{dataSourceId}/neighbors")
    @ApiOperation("获取节点邻居")
    public R<Map<String, Object>> getNeighbors(
            @PathVariable String dataSourceId,
            @RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> result = graphService.getNeighbors(dataSourceId, request);
            return R.ok(result);
        } catch (Exception e) {
            log.error("获取节点邻居失败", e);
            return R.fail("获取节点邻居失败: " + e.getMessage());
        }
    }

    @GetMapping("/{dataSourceId}/nodes")
    @ApiOperation("获取所有节点")
    public R<List<Map<String, Object>>> getAllNodes(
            @PathVariable String dataSourceId, 
            @RequestParam(required = false, defaultValue = "100") int limit) {
        try {
            List<Map<String, Object>> nodes = graphService.getAllNodes(dataSourceId, limit);
            return R.ok(nodes);
        } catch (Exception e) {
            log.error("获取所有节点失败", e);
            return R.fail("获取所有节点失败: " + e.getMessage());
        }
    }
} 