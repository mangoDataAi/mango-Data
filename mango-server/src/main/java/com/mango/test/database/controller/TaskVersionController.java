package com.mango.test.database.controller;

import com.mango.test.database.entity.TaskVersion;
import com.mango.test.database.model.PageResult;
import com.mango.test.database.model.request.VersionCreateRequest;
import com.mango.test.database.model.request.VersionRollbackRequest;
import com.mango.test.database.service.TaskVersionService;
import com.mango.test.vo.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 任务版本控制器
 */
@RestController
@RequestMapping("/api/version")
public class TaskVersionController {

    @Autowired
    private TaskVersionService versionService;

    /**
     * 创建版本
     */
    @PostMapping
    public R<String> createVersion(@RequestBody VersionCreateRequest request) {
        try {
            String versionId = versionService.createVersion(request);
            return R.ok(versionId);
        } catch (Exception e) {
            return R.fail("创建版本失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取版本详情
     */
    @GetMapping("/{versionId}")
    public R<TaskVersion> getVersionDetail(@PathVariable String versionId) {
        try {
            TaskVersion version = versionService.getVersionDetail(versionId);
            if (version == null) {
                return R.fail("版本不存在");
            }
            return R.ok(version);
        } catch (Exception e) {
            return R.fail("获取版本详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取任务相关的所有版本
     */
    @GetMapping("/task/{taskId}")
    public R<List<TaskVersion>> getVersionsByTaskId(@PathVariable String taskId) {
        try {
            List<TaskVersion> versions = versionService.getVersionsByTaskId(taskId);
            return R.ok(versions);
        } catch (Exception e) {
            return R.fail("获取任务版本列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 分页获取模型的版本历史
     */
    @GetMapping("/model/{modelId}")
    public R<PageResult<TaskVersion>> pageVersionsByModelId(
            @PathVariable String modelId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            PageResult<TaskVersion> pageResult = versionService.pageVersionsByModelId(modelId, pageNum, pageSize);
            return R.ok(pageResult);
        } catch (Exception e) {
            return R.fail("获取模型版本历史失败: " + e.getMessage());
        }
    }
    
    /**
     * 回滚到指定版本
     */
    @PostMapping("/rollback")
    public R<Boolean> rollbackVersion(@RequestBody VersionRollbackRequest request) {
        try {
            boolean result = versionService.rollbackToVersion(request);
            return R.ok(result);
        } catch (Exception e) {
            return R.fail("回滚版本失败: " + e.getMessage());
        }
    }
    
    /**
     * 比较两个版本
     */
    @GetMapping("/compare")
    public R<Map<String, Object>> compareVersions(
            @RequestParam String versionId1,
            @RequestParam String versionId2) {
        try {
            Map<String, Object> compareResult = versionService.compareVersions(versionId1, versionId2);
            return R.ok(compareResult);
        } catch (Exception e) {
            return R.fail("版本比较失败: " + e.getMessage());
        }
    }
} 