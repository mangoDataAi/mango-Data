package com.mango.test.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mango.test.database.entity.TaskVersion;
import com.mango.test.database.model.PageResult;
import com.mango.test.database.model.request.VersionCreateRequest;
import com.mango.test.database.model.request.VersionRollbackRequest;

import java.util.List;
import java.util.Map;

/**
 * 任务版本服务接口
 */
public interface TaskVersionService extends IService<TaskVersion> {
    
    /**
     * 创建版本
     * @param request 创建请求
     * @return 创建的版本ID
     */
    String createVersion(VersionCreateRequest request);
    
    /**
     * 获取版本详情
     * @param versionId 版本ID
     * @return 版本详情
     */
    TaskVersion getVersionDetail(String versionId);
    
    /**
     * 获取任务相关的所有版本
     * @param taskId 任务ID
     * @return 版本列表
     */
    List<TaskVersion> getVersionsByTaskId(String taskId);
    
    /**
     * 获取模型的版本历史
     * @param modelId 模型ID
     * @param dataSourceId 数据源ID (可选)
     * @param databaseId 数据库ID (可选)
     * @return 版本列表
     */
    List<TaskVersion> getModelVersions(String modelId, String dataSourceId, String databaseId);
    
    /**
     * 分页获取模型的版本历史
     * @param modelId 模型ID
     * @param pageNum 页码
     * @param pageSize 页大小
     * @return 分页结果
     */
    PageResult<TaskVersion> pageVersionsByModelId(String modelId, int pageNum, int pageSize);
    
    /**
     * 回滚到指定版本
     * @param request 版本回滚请求
     * @return 回滚结果，true表示成功，false表示失败
     */
    boolean rollbackToVersion(VersionRollbackRequest request);
    
    /**
     * 比较两个版本
     * @param versionId1 版本1
     * @param versionId2 版本2
     * @return 比较结果
     */
    Map<String, Object> compareVersions(String versionId1, String versionId2);
    
    /**
     * 设置版本为当前版本
     * @param versionId 版本ID
     * @return 是否成功
     */
    boolean setVersionAsCurrent(String versionId);
    
    /**
     * 添加版本日志
     * @param versionId 版本ID
     * @param level 日志级别
     * @param message 日志消息
     * @return 是否成功
     */
    boolean addVersionLog(String versionId, String level, String message);
    
    /**
     * 获取当前模型版本号
     * @param modelId 模型ID
     * @param dataSourceId 数据源ID (可选)
     * @param databaseId 数据库ID (可选)
     * @return 当前版本号
     */
    String getCurrentVersionNum(String modelId, String dataSourceId, String databaseId);
    
    /**
     * 生成下一个版本号
     * @param currentVersionNum 当前版本号
     * @param versionType 版本类型 (MAJOR, MINOR, PATCH)
     * @return 新的版本号
     */
    String generateNextVersionNum(String currentVersionNum, String versionType);
    
    /**
     * 检查模型结构是否变化
     * @param modelId 模型ID
     * @param dataSourceId 数据源ID
     * @param databaseId 数据库ID
     * @return 变化详情
     */
    Map<String, Object> checkModelStructureChanges(String modelId, String dataSourceId, String databaseId);
    
    /**
     * 根据任务ID获取最新版本
     *
     * @param taskId 任务ID
     * @return 最新版本信息
     */
    TaskVersion getLatestByTaskId(String taskId);
    
}