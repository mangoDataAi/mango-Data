package com.mango.test.database.controller;

import com.mango.test.vo.R;
import com.mango.test.database.service.TimeSeriesService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@Api(tags = "时序数据库")
@RestController
@RequestMapping("/api/timeseries")
public class TimeSeriesController {

    @Autowired
    private TimeSeriesService timeSeriesService;

    @GetMapping("/{dataSourceId}/metrics")
    @ApiOperation("获取所有指标")
    public R<List<Map<String, Object>>> getMetrics(@PathVariable String dataSourceId) {
        try {
            List<Map<String, Object>> metrics = timeSeriesService.getMetrics(dataSourceId);
            return R.ok(metrics);
        } catch (Exception e) {
            log.error("获取指标列表失败", e);
            return R.fail("获取指标列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{dataSourceId}/metric/{metricName}")
    @ApiOperation("获取指标详情")
    public R<Map<String, Object>> getMetricInfo(
            @PathVariable String dataSourceId,
            @PathVariable String metricName) {
        try {
            Map<String, Object> metricInfo = timeSeriesService.getMetricInfo(dataSourceId, metricName);
            return R.ok(metricInfo);
        } catch (Exception e) {
            log.error("获取指标详情失败", e);
            return R.fail("获取指标详情失败: " + e.getMessage());
        }
    }

    @PostMapping("/{dataSourceId}/data")
    @ApiOperation("获取时序数据")
    public R<List<Map<String, Object>>> getTimeSeriesData(
            @PathVariable String dataSourceId,
            @RequestBody Map<String, Object> query) {
        try {
            List<Map<String, Object>> data = timeSeriesService.getTimeSeriesData(dataSourceId, query);
            return R.ok(data);
        } catch (Exception e) {
            log.error("获取时序数据失败", e);
            return R.fail("获取时序数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/{dataSourceId}/tags/{metricName}")
    @ApiOperation("获取指标的标签列表")
    public R<List<String>> getTagKeys(
            @PathVariable String dataSourceId,
            @PathVariable String metricName) {
        try {
            List<String> tagKeys = timeSeriesService.getTagKeys(dataSourceId, metricName);
            return R.ok(tagKeys);
        } catch (Exception e) {
            log.error("获取标签列表失败", e);
            return R.fail("获取标签列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{dataSourceId}/tag-values")
    @ApiOperation("获取指标标签的可选值")
    public R<List<String>> getTagValues(
            @PathVariable String dataSourceId,
            @RequestParam String metricName,
            @RequestParam String tagKey) {
        try {
            List<String> tagValues = timeSeriesService.getTagValues(dataSourceId, metricName, tagKey);
            return R.ok(tagValues);
        } catch (Exception e) {
            log.error("获取标签值列表失败", e);
            return R.fail("获取标签值列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{dataSourceId}/aggregations/{metricName}")
    @ApiOperation("获取指标支持的聚合函数")
    public R<List<String>> getSupportedAggregations(
            @PathVariable String dataSourceId,
            @PathVariable String metricName) {
        try {
            List<String> aggregations = timeSeriesService.getSupportedAggregations(dataSourceId, metricName);
            return R.ok(aggregations);
        } catch (Exception e) {
            log.error("获取聚合函数列表失败", e);
            return R.fail("获取聚合函数列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/{dataSourceId}/batch-data")
    @ApiOperation("批量获取时序数据")
    public R<Map<String, List<Map<String, Object>>>> getBatchTimeSeriesData(
            @PathVariable String dataSourceId,
            @RequestBody Map<String, Object> query) {
        try {
            Map<String, List<Map<String, Object>>> data = 
                timeSeriesService.getBatchTimeSeriesData(dataSourceId, query);
            return R.ok(data);
        } catch (Exception e) {
            log.error("批量获取时序数据失败", e);
            return R.fail("批量获取时序数据失败: " + e.getMessage());
        }
    }

    @PostMapping("/{dataSourceId}/write")
    @ApiOperation("写入时序数据")
    public R<Boolean> writeTimeSeriesData(
            @PathVariable String dataSourceId,
            @RequestBody Map<String, Object> data) {
        try {
            boolean success = timeSeriesService.writeTimeSeriesData(dataSourceId, data);
            return R.ok("数据写入成功", success);
        } catch (Exception e) {
            log.error("写入时序数据失败", e);
            return R.fail("写入时序数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/{dataSourceId}/stats")
    @ApiOperation("获取时序数据库统计信息")
    public R<Map<String, Object>> getDatabaseStats(@PathVariable String dataSourceId) {
        try {
            Map<String, Object> stats = timeSeriesService.getDatabaseStats(dataSourceId);
            return R.ok(stats);
        } catch (Exception e) {
            log.error("获取时序数据库统计信息失败", e);
            return R.fail("获取时序数据库统计信息失败: " + e.getMessage());
        }
    }
} 