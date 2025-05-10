package com.mango.test.database.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mango.test.database.entity.MaterializeTask;
import com.mango.test.database.model.PageResult;
import com.mango.test.database.model.request.TaskCreateRequest;
import com.mango.test.database.model.request.TaskHistoryQueryRequest;
import com.mango.test.database.model.request.TaskQueryRequest;

import java.util.List;
import java.util.Map;

/**
 * 物化任务服务接口
 */
public interface MaterializeTaskService extends IService<MaterializeTask> {
    
    /**
     * 创建物化任务
     * @param request 任务创建请求
     * @return 任务ID
     */
    String createTask(TaskCreateRequest request);

    /**
     * 分页查询任务
     * @param request 查询请求
     * @return 分页结果
     */
    PageResult<MaterializeTask> pageTask(TaskQueryRequest request);
    
    /**
     * 获取任务详情
     * @param taskId 任务ID
     * @return 任务详情
     */
    MaterializeTask getTaskDetail(String taskId);
    
    /**
     * 执行任务
     * @param taskId 任务ID
     * @return 执行结果
     */
    boolean executeTask(String taskId);
    

    /**
     * 分析任务结构变化
     * @param taskId 任务ID
     * @return 变化信息
     */
    Map<String, Object> analyzeTaskStructureChanges(String taskId);
    
    /**
     * 取消任务
     * @param taskId 任务ID
     * @return 执行结果
     */
    boolean cancelTask(String taskId);
    
    /**
     * 更新任务状态
     * @param taskId 任务ID
     * @param status 状态
     * @param progress 进度
     * @return 更新结果
     */
    boolean updateTaskStatus(String taskId, String status, Integer progress);
    
    /**
     * 获取任务历史
     * @param request 历史查询请求
     * @return 分页结果
     */
    PageResult<MaterializeTask> getTaskHistory(TaskHistoryQueryRequest request);
    
    /**
     * 获取任务日志
     * @param taskId 任务ID
     * @return 任务日志
     */
    String getTaskLogs(String taskId);
    
    /**
     * 获取任务统计信息
     * @return 统计信息
     */
    Map<String, Object> getTaskStatistics();
    
    /**
     * 获取任务依赖关系图
     * @param taskId 任务ID
     * @return 依赖关系图
     */
    Map<String, Object> getTaskDependencyGraph(String taskId);
    
    /**
     * 批量执行任务
     * @param taskIds 任务ID列表
     * @return 执行结果
     */
    Map<String, List<String>> executeBatchTasks(List<String> taskIds);
    
    /**
     * 获取任务执行历史
     * @param taskId 任务ID
     * @return 执行历史
     */
    List<Map<String, Object>> getTaskExecutionHistory(String taskId);
    
    /**
     * 批量取消任务
     * @param taskIds 任务ID列表
     * @return 是否全部成功
     */
    boolean cancelBatchTasks(List<String> taskIds);
    
    /**
     * 获取任务详细日志
     * @param taskId 任务ID
     * @return 任务日志列表
     */
    List<Map<String, Object>> getTaskDetailLogs(String taskId);

    /**
     * 回滚到指定版本
     * @param versionId 要回滚到的版本ID
     * @param taskId 任务ID
     * @param description 回滚描述
     * @return 是否成功
     */
    boolean rollbackToVersion(String versionId, String taskId, String description);

    /**
     * 根据数据源类型查询任务列表
     *
     * @param dataSourceType 数据源类型
     * @return 任务列表
     */
    List<MaterializeTask> listByDataSourceType(String dataSourceType);
} 