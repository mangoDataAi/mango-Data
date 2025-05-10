package com.mango.test.database.controller;

import com.mango.test.database.entity.MaterializeTask;
import com.mango.test.database.entity.TaskTable;
import com.mango.test.database.entity.TaskVersion;
import com.mango.test.database.entity.TaskLog;
import com.mango.test.database.model.PageResult;
import com.mango.test.database.model.request.TaskCreateRequest;
import com.mango.test.database.model.request.TaskQueryRequest;
import com.mango.test.database.model.request.TaskHistoryQueryRequest;
import com.mango.test.database.service.MaterializeTaskService;
import com.mango.test.database.service.TaskTableService;
import com.mango.test.database.service.TaskVersionService;
import com.mango.test.database.service.TaskLogService;
import com.mango.test.vo.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * 物化任务控制器
 */
@RestController
@RequestMapping("/api/materialize")
public class MaterializeTaskController {

    @Autowired
    private MaterializeTaskService taskService;
    
    @Autowired
    private TaskTableService tableService;
    
    @Autowired
    private TaskVersionService versionService;
    
    @Autowired
    private TaskLogService taskLogService;
    

    /**
     * 创建物化任务
     */
    @PostMapping("/tasks")
    public R<String> createTask(@RequestBody TaskCreateRequest request) {
        try {
            String taskId = taskService.createTask(request);
            return R.ok(taskId);
        } catch (Exception e) {
            return R.fail("创建物化任务失败: " + e.getMessage());
        }
    }
    

    /**
     * 获取模型的版本历史
     */
    @GetMapping("/model/{modelId}/versions")
    public R<List<TaskVersion>> getModelVersions(
            @PathVariable String modelId, 
            @RequestParam(required = false) String dataSourceId,
            @RequestParam(required = false) String databaseId) {
        try {
            List<TaskVersion> versions = versionService.getModelVersions(modelId, dataSourceId, databaseId);
            return R.ok(versions);
        } catch (Exception e) {
            return R.fail("获取模型版本历史失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取任务关联的版本
     */
    @GetMapping("/task/{taskId}/versions")
    public R<List<TaskVersion>> getTaskVersions(@PathVariable String taskId) {
        try {
            List<TaskVersion> versions = versionService.getVersionsByTaskId(taskId);
            return R.ok(versions);
        } catch (Exception e) {
            return R.fail("获取任务版本失败: " + e.getMessage());
        }
    }
    
    /**
     * 分页查询任务列表
     */
    @GetMapping("/tasks")
    public R<PageResult<MaterializeTask>> pageTasks(TaskQueryRequest request) {
        try {
            PageResult<MaterializeTask> pageResult = taskService.pageTask(request);
            return R.ok(pageResult);
        } catch (Exception e) {
            return R.fail("查询任务列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取任务详情
     */
    @GetMapping("/task/{taskId}")
    public R<MaterializeTask> getTaskDetail(@PathVariable String taskId) {
        try {
            if (StringUtils.isEmpty(taskId)) {
                return R.fail("任务ID不能为空");
            }
            
            MaterializeTask task = taskService.getTaskDetail(taskId);
            if (task == null) {
                return R.fail("任务不存在");
            }
            
            return R.ok(task);
        } catch (Exception e) {
            return R.fail("获取任务详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取任务相关的表配置
     */
    @GetMapping("/task/{taskId}/tables")
    public R<List<TaskTable>> getTaskTables(@PathVariable String taskId) {
        try {
            List<TaskTable> tables = tableService.getTablesByTaskId(taskId);
            return R.ok(tables);
        } catch (Exception e) {
            return R.fail("获取任务表配置失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行任务
     */
    @PostMapping("/tasks/{taskId}/execute")
    public R<Boolean> executeTask(
            @PathVariable String taskId,
            @RequestParam(required = false, defaultValue = "true") Boolean checkVersionUpgrade) {
        try {
            boolean result = taskService.executeTask(taskId);
            if (!result) {
                return R.fail("执行任务失败，请检查任务状态");
            }
            return R.ok(true);
        } catch (Exception e) {
            return R.fail("执行任务失败: " + e.getMessage());
        }
    }

    /**
     * 取消任务
     */
    @PostMapping("/cancel/{taskId}")
    public R<Boolean> cancelTask(@PathVariable String taskId) {
        try {
            boolean result = taskService.cancelTask(taskId);
            if (!result) {
                return R.fail("取消任务失败，请检查任务状态");
            }
            return R.ok(true);
        } catch (Exception e) {
            return R.fail("取消任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 分析任务结构变化（不执行任务，只检查版本是否需要升级）
     */
    @GetMapping("/tasks/{taskId}/analyze-structure")
    public R<Map<String, Object>> analyzeTaskStructure(@PathVariable String taskId) {
        try {
            Map<String, Object> changes = taskService.analyzeTaskStructureChanges(taskId);
            return R.ok(changes);
        } catch (Exception e) {
            return R.fail("分析任务结构变化失败: " + e.getMessage());
        }
    }
    
    /**
     * 比较两个版本的差异
     */
    @GetMapping("/versions/compare")
    public R<Map<String, Object>> compareVersions(
            @RequestParam String versionId1, 
            @RequestParam String versionId2) {
        try {
            Map<String, Object> diff = versionService.compareVersions(versionId1, versionId2);
            return R.ok(diff);
        } catch (Exception e) {
            return R.fail("比较版本差异失败: " + e.getMessage());
        }
    }

    /**
     * 批量取消任务
     */
    @PostMapping("/cancel-batch")
    public R<Boolean> cancelBatchTasks(@RequestBody List<String> taskIds) {
        try {
            boolean allSuccess = true;
            for (String taskId : taskIds) {
                boolean result = taskService.cancelTask(taskId);
                if (!result) {
                    allSuccess = false;
                }
            }
            if (!allSuccess) {
                return R.fail("部分任务取消失败，请检查任务状态");
            }
            return R.ok(true);
        } catch (Exception e) {
            return R.fail("批量取消任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新任务表配置
     */
    @PutMapping("/task/{taskId}/tables")
    public R<Boolean> updateTaskTables(@PathVariable String taskId, @RequestBody List<TaskTable> tables) {
        try {
            // 验证表配置是否属于该任务
            for (TaskTable table : tables) {
                if (!taskId.equals(table.getTaskId())) {
                    return R.fail("表配置与任务不匹配");
                }
            }
            
            boolean result = tableService.updateTables(tables);
            if (!result) {
                return R.fail("更新表配置失败");
            }
            return R.ok(true);
        } catch (Exception e) {
            return R.fail("更新表配置失败: " + e.getMessage());
        }
    }

    /**
     * 获取任务历史记录-高级搜索
     */
        @GetMapping("/tasks/history/advanced")
    public R<PageResult<MaterializeTask>> getAdvancedTaskHistory(TaskHistoryQueryRequest request) {
        try {
            if (request == null) {
                request = new TaskHistoryQueryRequest();
            }
            
            // 调用service层方法获取任务历史记录
            PageResult<MaterializeTask> pageResult = taskService.getTaskHistory(request);
            return R.ok(pageResult);
        } catch (Exception e) {
            return R.fail("高级查询任务历史记录失败: " + e.getMessage());
        }
    }

    /**
     * 获取任务历史记录
     */
    @GetMapping("/tasks/history")
    public R<PageResult<MaterializeTask>> getTaskHistory(TaskHistoryQueryRequest request) {
        try {
            PageResult<MaterializeTask> pageResult = taskService.getTaskHistory(request);
            return R.ok(pageResult);
        } catch (Exception e) {
            return R.fail("查询任务历史记录失败: " + e.getMessage());
        }
    }

    /**
     * 获取任务日志
     */
    @GetMapping("/task/{taskId}/logs")
    public R<String> getTaskLogs(@PathVariable String taskId) {
        try {
            String logs = taskService.getTaskLogs(taskId);
            return R.ok(logs);
        } catch (Exception e) {
            return R.fail("获取任务日志失败: " + e.getMessage());
        }
    }

    /**
     * 更新任务状态
     */
    @PutMapping("/task/{taskId}/status")
    public R<Boolean> updateTaskStatus(
            @PathVariable String taskId,
            @RequestParam String status,
            @RequestParam(required = false) Integer progress) {
        try {
            boolean result = taskService.updateTaskStatus(taskId, status, progress);
            if (!result) {
                return R.fail("更新任务状态失败");
            }
            return R.ok(true);
        } catch (Exception e) {
            return R.fail("更新任务状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取任务监控信息
     */
    @GetMapping("/tasks/monitor")
    public R<Map<String, Object>> getTasksMonitor() {
        try {
            // 获取当前正在运行的任务数
            TaskQueryRequest runningQuery = new TaskQueryRequest();
            runningQuery.setStatus("RUNNING");
            PageResult<MaterializeTask> runningTasks = taskService.pageTask(runningQuery);
            
            // 获取等待执行的任务数
            TaskQueryRequest waitingQuery = new TaskQueryRequest();
            waitingQuery.setStatus("WAITING");
            PageResult<MaterializeTask> waitingTasks = taskService.pageTask(waitingQuery);
            
            // 获取已计划的任务数
            TaskQueryRequest scheduledQuery = new TaskQueryRequest();
            scheduledQuery.setStatus("SCHEDULED");
            PageResult<MaterializeTask> scheduledTasks = taskService.pageTask(scheduledQuery);
            
            // 汇总监控信息
            Map<String, Object> monitorInfo = new HashMap<>();
            monitorInfo.put("runningTasks", runningTasks.getTotal());
            monitorInfo.put("waitingTasks", waitingTasks.getTotal());
            monitorInfo.put("scheduledTasks", scheduledTasks.getTotal());
            
            return R.ok(monitorInfo);
        } catch (Exception e) {
            return R.fail("获取任务监控信息失败: " + e.getMessage());
        }
    }

    /**
     * 重新调度任务
     */
    @PostMapping("/tasks/{taskId}/reschedule")
    public R<Boolean> rescheduleTask(@PathVariable String taskId) {
        try {
            MaterializeTask task = taskService.getTaskDetail(taskId);
            if (task == null) {
                return R.fail("任务不存在");
            }
            
            // 先取消任务
            taskService.cancelTask(taskId);
            
            // 更新任务状态为等待中
            taskService.updateTaskStatus(taskId, "WAITING", 0);
            
            // 重新执行任务
            boolean result = taskService.executeTask(taskId);
            if (!result) {
                return R.fail("重新调度任务失败");
            }
            
            return R.ok(true);
        } catch (Exception e) {
            return R.fail("重新调度任务失败: " + e.getMessage());
        }
    }

    /**
     * 获取任务统计信息
     */
    @GetMapping("/statistics")
    public R<Map<String, Object>> getTaskStatistics() {
        try {
            Map<String, Object> statistics = taskService.getTaskStatistics();
            return R.ok(statistics);
        } catch (Exception e) {
            return R.fail("获取任务统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取任务依赖关系图
     */
    @GetMapping("/task/{taskId}/dependency-graph")
    public R<Map<String, Object>> getTaskDependencyGraph(@PathVariable String taskId) {
        try {
            Map<String, Object> graph = taskService.getTaskDependencyGraph(taskId);
            return R.ok(graph);
        } catch (Exception e) {
            return R.fail("获取任务依赖关系图失败: " + e.getMessage());
        }
    }

    /**
     * 批量执行任务
     */
    @PostMapping("/tasks/execute-batch")
    public R<Map<String, List<String>>> executeBatchTasks(@RequestBody List<String> taskIds) {
        try {
            Map<String, List<String>> result = taskService.executeBatchTasks(taskIds);
            return R.ok(result);
        } catch (Exception e) {
            return R.fail("批量执行任务失败: " + e.getMessage());
        }
    }

    /**
     * 获取任务执行历史
     */
    @GetMapping("/task/{taskId}/execution-history")
    public R<List<Map<String, Object>>> getTaskExecutionHistory(@PathVariable String taskId) {
        try {
            List<Map<String, Object>> result = taskService.getTaskExecutionHistory(taskId);
            return R.ok(result);
        } catch (Exception e) {
            return R.fail("获取任务执行历史失败: " + e.getMessage());
        }
    }

    /**
     * 获取任务详细日志
     */
    @GetMapping("/task/{taskId}/detail-logs")
    public R<List<Map<String, Object>>> getTaskDetailLogs(@PathVariable String taskId) {
        try {
            if (StringUtils.isEmpty(taskId)) {
                return R.fail("任务ID不能为空");
            }
            
            List<Map<String, Object>> logs = taskService.getTaskDetailLogs(taskId);
            return R.ok(logs);
        } catch (Exception e) {
            return R.fail("获取任务详细日志失败: " + e.getMessage());
        }
    }

    /**
     * 获取历史详情
     */
    @GetMapping("/history/detail")
    public R<List<Map<String, Object>>> getHistoryDetail(@RequestParam String id) {
        try {
            // 使用 !StringUtils.hasText() 代替 StringUtils.isEmpty()
            if (!StringUtils.hasText(id)) {
                return R.fail("历史记录ID不能为空");
            }

            // 获取历史任务详情
            MaterializeTask historyTask = taskService.getTaskDetail(id);
            if (historyTask == null) {
                return R.fail("历史记录不存在");
            }

            // 获取相关版本信息
            List<TaskVersion> versions = versionService.getVersionsByTaskId(id);

            // 获取相关日志信息
            List<Map<String, Object>> logs = taskService.getTaskDetailLogs(id);

            // 将版本数据转换为前端需要的格式
            List<Map<String, Object>> formattedVersions = new ArrayList<>();
            for (TaskVersion version : versions) {
                Map<String, Object> versionMap = new HashMap<>();
                versionMap.put("id", version.getId());
                versionMap.put("taskId", version.getTaskId());
                versionMap.put("version", version.getVersionNum());
                versionMap.put("publishTime", version.getPublishTime());
                versionMap.put("status", version.getStatus());
                versionMap.put("publisher", version.getPublisher());
                versionMap.put("description", version.getDescription());
                // 使用 isRollback 字段代替检查 type
                versionMap.put("isRollback", version.getIsRollback() != null && version.getIsRollback());
                versionMap.put("isCurrent", version.getIsCurrent() != null && version.getIsCurrent());

                // 处理变更内容，规范化类型
                if (version.getChanges() != null) {
                    List<Map<String, Object>> normalizedChanges = new ArrayList<>();
                    for (Map<String, Object> change : version.getChanges()) {
                        Map<String, Object> normalizedChange = new HashMap<>(change);
                        // 确保type字段存在且格式化为大写
                        if (normalizedChange.containsKey("type")) {
                            String type = String.valueOf(normalizedChange.get("type")).toUpperCase();
                            // 标准化类型
                            if ("CREATE".equals(type)) {
                                normalizedChange.put("type", "CREATE");
                            } else if ("FIELD".equals(type)) {
                                normalizedChange.put("type", "FIELD");
                            } else if ("TABLE".equals(type)) {
                                normalizedChange.put("type", "TABLE");
                            } else if ("MODEL".equals(type)) {
                                normalizedChange.put("type", "MODEL");
                            } else if ("CONFIG".equals(type)) {
                                normalizedChange.put("type", "CONFIG");
                            }
                            // 可以添加更多类型的标准化处理
                        }
                        normalizedChanges.add(normalizedChange);
                    }
                    versionMap.put("changes", normalizedChanges);
                } else {
                    versionMap.put("changes", new ArrayList<>());
                }

                formattedVersions.add(versionMap);
            }

            return R.ok(formattedVersions);
        } catch (Exception e) {
            return R.fail("获取历史详情失败: " + e.getMessage());
        }
    }

    /**
     * 回滚到指定版本
     */
    @PostMapping("/rollback")
    public R<Boolean> rollbackVersion(@RequestBody Map<String, Object> request) {
        try {
            String versionId = (String) request.get("versionId");
            String taskId = (String) request.get("taskId");
            String description = (String) request.get("description");
            
            if (!StringUtils.hasText(versionId)) {
                return R.fail("版本ID不能为空");
            }
            
            if (!StringUtils.hasText(taskId)) {
                return R.fail("任务ID不能为空");
            }
            
            boolean success = taskService.rollbackToVersion(versionId, taskId, description);
            if (success) {
                return R.ok(true);
            } else {
                return R.fail("回滚操作失败");
            }
        } catch (Exception e) {
            return R.fail("回滚操作失败: " + e.getMessage());
        }
    }

    /**
     * 获取版本详情
     */
    @GetMapping("/detail")
    public R<TaskVersion> getVersionDetail(
            @RequestParam String versionId) {
        try {
            if (StringUtils.isEmpty(versionId)) {
                return R.fail("版本ID不能为空");
            }

            // 获取版本详情
            TaskVersion version = versionService.getVersionDetail(versionId);
            if (version == null) {
                return R.fail("版本不存在");
            }
            
            // 关联版本日志
            List<TaskLog> versionLogs = taskLogService.getVersionLogs(versionId);
            if (versionLogs != null && !versionLogs.isEmpty()) {
                // 转换为前端需要的日志格式
                List<Map<String, Object>> logList = new ArrayList<>();
                for (TaskLog log : versionLogs) {
                    Map<String, Object> logMap = new HashMap<>();
                    logMap.put("level", log.getLogLevel());
                    logMap.put("message", log.getContent());
                    logMap.put("timestamp", log.getCreateTime());
                    if (log.getTableId() != null) {
                        logMap.put("source", log.getTableName());
                    }
                    logList.add(logMap);
                }
                // 设置日志到版本对象中
                version.setLogs(logList);
            }
            
            return R.ok(version);
        } catch (Exception e) {
            return R.fail("获取版本详情失败: " + e.getMessage());
        }
    }

}