package com.mango.test.database.controller;

import com.mango.test.database.entity.ApiDataSource;
import com.mango.test.database.service.ApiDataSourceService;
import com.mango.test.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.util.Calendar;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@Api(tags = "接口数据源管理")
@RestController
@RequestMapping("/api/ds/api")
public class ApiDataSourceController {

    @Autowired
    private ApiDataSourceService apiDataSourceService;

    @GetMapping("/stats")
    @ApiOperation("获取API数据源统计信息")
    public R<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 获取接口总数
            long totalApis = apiDataSourceService.count();
            stats.put("totalApis", totalApis);
            
            // 计算成功率
            LambdaQueryWrapper<ApiDataSource> successQuery = new LambdaQueryWrapper<>();
            successQuery.eq(ApiDataSource::getStatus, 1); // 假设状态1表示成功状态
            long successCount = apiDataSourceService.count(successQuery);
            double successRate = totalApis > 0 ? (successCount * 100.0 / totalApis) : 0;
            stats.put("successRate", Math.round(successRate * 10) / 10.0);
            
            // 计算平均响应时间
            double avgResponseTime = apiDataSourceService.getAverageResponseTime();
            stats.put("avgResponseTime", Math.round(avgResponseTime * 10) / 10.0);
            
            // 获取错误数
            LambdaQueryWrapper<ApiDataSource> errorQuery = new LambdaQueryWrapper<>();
            errorQuery.eq(ApiDataSource::getStatus, 0); // 假设状态0表示错误状态
            long errorCount = apiDataSourceService.count(errorQuery);
            stats.put("errorCount", errorCount);
            
            // 计算增长率（与一周前比较）
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -7);
            Date lastWeek = cal.getTime();
            
            LambdaQueryWrapper<ApiDataSource> newApisQuery = new LambdaQueryWrapper<>();
            newApisQuery.ge(ApiDataSource::getCreateTime, lastWeek);
            long newApisCount = apiDataSourceService.count(newApisQuery);
            
            LambdaQueryWrapper<ApiDataSource> oldApisQuery = new LambdaQueryWrapper<>();
            oldApisQuery.lt(ApiDataSource::getCreateTime, lastWeek);
            long oldApisCount = apiDataSourceService.count(oldApisQuery);
            
            double apiGrowthRate = oldApisCount > 0 ? ((newApisCount * 100.0) / oldApisCount) - 100 : 0;
            stats.put("apiGrowthRate", Math.round(apiGrowthRate * 10) / 10.0);
            
            // 计算错误率的变化
            LambdaQueryWrapper<ApiDataSource> newErrorsQuery = new LambdaQueryWrapper<>();
            newErrorsQuery.eq(ApiDataSource::getStatus, 0).ge(ApiDataSource::getCreateTime, lastWeek);
            long newErrorCount = apiDataSourceService.count(newErrorsQuery);
            
            LambdaQueryWrapper<ApiDataSource> oldErrorsQuery = new LambdaQueryWrapper<>();
            oldErrorsQuery.eq(ApiDataSource::getStatus, 0).lt(ApiDataSource::getCreateTime, lastWeek);
            long oldErrorCount = apiDataSourceService.count(oldErrorsQuery);
            
            double errorChangeRate = 0;
            if (oldErrorCount > 0) {
                errorChangeRate = ((newErrorCount * 100.0) / oldErrorCount) - 100;
            } else if (newErrorCount > 0) {
                errorChangeRate = 100;
            }
            stats.put("errorChangeRate", Math.round(errorChangeRate * 10) / 10.0);
            
            // 计算响应时间的变化
            double responseTimeChangeRate = apiDataSourceService.getResponseTimeChangeRate();
            stats.put("responseTimeChangeRate", Math.round(responseTimeChangeRate * 10) / 10.0);
            
            return R.ok(stats);
        } catch (Exception e) {
            log.error("获取API数据源统计信息失败", e);
            return R.fail("获取统计信息失败：" + e.getMessage());
        }
    }

    @GetMapping("/page")
    @ApiOperation("分页获取接口数据源列表")
    public R<IPage<ApiDataSource>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name) {
        try {
            Page<ApiDataSource> page = new Page<>(current, size);
            LambdaQueryWrapper<ApiDataSource> wrapper = new LambdaQueryWrapper<>();
            if (StringUtils.isNotBlank(name)) {
                wrapper.like(ApiDataSource::getName, name);
            }
            wrapper.orderByDesc(ApiDataSource::getCreateTime);
            return R.ok(apiDataSourceService.page(page, wrapper));
        } catch (Exception e) {
            log.error("获取数据源列表失败", e);
            return R.fail("获取数据源列表失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/batch")
    @ApiOperation("批量删除接口数据源")
    public R<Boolean> batchDelete(@RequestBody List<Long> ids) {
        try {
            boolean success = apiDataSourceService.removeByIds(ids);
            return R.ok(success);
        } catch (Exception e) {
            log.error("批量删除失败", e);
            return R.fail("批量删除失败：" + e.getMessage());
        }
    }

    @GetMapping("/list")
    @ApiOperation("获取接口数据源列表")
    public R<List<ApiDataSource>> list() {
        return R.ok(apiDataSourceService.list());
    }

    @PostMapping("/create")
    @ApiOperation("创建接口数据源")
    public R<Boolean> create(@RequestBody ApiDataSource apiDataSource) {
        return R.ok(apiDataSourceService.saveOrUpdate(apiDataSource));
    }

    @PutMapping("/update")
    @ApiOperation("更新接口数据源")
    public R<Boolean> update(@RequestBody ApiDataSource apiDataSource) {
        return R.ok(apiDataSourceService.updateById(apiDataSource));
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除接口数据源")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(apiDataSourceService.removeById(id));
    }

    @PostMapping("/test")
    @ApiOperation("测试接口连接")
    public R<Map<String, Object>> test(@RequestBody ApiDataSource apiDataSource) {
        try {
            Map<String, Object> result = apiDataSourceService.testConnection(apiDataSource);
            return R.ok(result);
        } catch (Exception e) {
            log.error("测试连接失败", e);
            return R.fail("连接失败：" + e.getMessage());
        }
    }

    @GetMapping("/{id}/preview")
    @ApiOperation("预览接口数据")
    public R<Map<String, Object>> preview(@PathVariable Long id) {
        try {
            Map<String, Object> data = apiDataSourceService.previewData(id);
            return R.ok(data);
        } catch (Exception e) {
            log.error("预览数据失败", e);
            return R.fail("预览失败：" + e.getMessage());
        }
    }
}
