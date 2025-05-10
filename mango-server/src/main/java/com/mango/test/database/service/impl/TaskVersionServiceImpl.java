package com.mango.test.database.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mango.test.common.utils.UserUtils;
import com.mango.test.database.entity.*;
import com.mango.test.database.mapper.TaskVersionMapper;
import com.mango.test.database.model.PageResult;
import com.mango.test.database.model.request.VersionCreateRequest;
import com.mango.test.database.model.request.VersionRollbackRequest;
import com.mango.test.database.service.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 任务版本服务实现类
 */
@Service
public class TaskVersionServiceImpl extends ServiceImpl<TaskVersionMapper, TaskVersion> implements TaskVersionService {

    private static final Logger log = LoggerFactory.getLogger(TaskVersionServiceImpl.class);

    @Autowired
    private MaterializeTaskService taskService;

    @Autowired
    private IModelService modelService;

    @Autowired
    private TableService tableService;

    @Autowired
    private TableFieldService tableFieldService;

    @Autowired
    private TaskTableService taskTableService;

    @Autowired
    private TaskTableFieldService taskTableFieldService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createVersion(VersionCreateRequest request) {
        // 参数校验
        if (request == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (StringUtils.isBlank(request.getTaskId())) {
            throw new IllegalArgumentException("任务ID不能为空");
        }

        if (StringUtils.isBlank(request.getModelId())) {
            throw new IllegalArgumentException("模型ID不能为空");
        }

        // 验证关联的任务是否存在
        MaterializeTask task = taskService.getById(request.getTaskId());
        if (task == null) {
            throw new IllegalArgumentException("关联的任务不存在");
        }

        // 获取当前模型的最新版本号
        String currentVersionNum = getCurrentVersionNum(request.getModelId());
        String newVersionNum = generateNextVersionNum(currentVersionNum);

        // 创建新版本
        TaskVersion version = new TaskVersion();
        version.setTaskId(request.getTaskId());
        version.setModelId(request.getModelId());
        version.setVersionNum(newVersionNum);

        // 设置环境，优先使用请求中的环境，如果为空则使用任务的环境
        version.setEnvironment(StringUtils.isNotBlank(request.getEnvironment()) ?
                request.getEnvironment() : task.getEnvironment());

        // 设置状态 - 此处重要修改：初始版本状态应当与任务状态保持一致
        // 默认为等待状态，让版本随任务状态变化
        version.setStatus(task.getStatus() != null ? task.getStatus().toLowerCase() : "waiting");

        // 设置发布者信息
        version.setPublisher(UserUtils.getCurrentUsername());
        version.setPublishTime(LocalDateTime.now());

        // 设置描述
        version.setDescription(StringUtils.isNotBlank(request.getDescription()) ?
                request.getDescription() : "版本 " + newVersionNum);

        // 设置发布模式
        version.setMode(StringUtils.isNotBlank(request.getMode()) ?
                request.getMode() : "full");

        // 新版本默认不是当前版本，只有任务成功执行后才会成为当前版本
        version.setIsCurrent(false);

        // 默认不是回滚版本
        version.setIsRollback(false);

        // 设置变更内容
        version.setChanges(request.getChanges());

        // 设置配置信息
        version.setConfig(request.getConfig());

        // 设置创建和更新时间
        Date now = new Date();
        version.setCreateTime(now);
        version.setUpdateTime(now);

        // 保存新版本
        boolean saved = save(version);
        if (!saved) {
            throw new RuntimeException("保存版本失败");
        }

        log.info("创建版本成功: {}, 版本号: {}, 状态: {}",
                version.getId(), version.getVersionNum(), version.getStatus());

        return version.getId();
    }


    /**
     * 获取当前版本号
     */
    private String getCurrentVersionNum(String modelId) {
        LambdaQueryWrapper<TaskVersion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TaskVersion::getModelId, modelId)
                .orderByDesc(TaskVersion::getVersionNum)
                .last("LIMIT 1");

        TaskVersion latestVersion = getOne(queryWrapper);
        return latestVersion != null ? latestVersion.getVersionNum() : "0.0.0";
    }

    /**
     * 生成下一个版本号
     */
    private String generateNextVersionNum(String currentVersion) {
        if (StringUtils.isBlank(currentVersion) || !currentVersion.contains(".")) {
            return "1.0.0";
        }

        String[] parts = currentVersion.split("\\.");
        if (parts.length != 3) {
            return "1.0.0";
        }

        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch = Integer.parseInt(parts[2]);

            // 增加小版本号
            minor++;

            return major + "." + minor + "." + patch;
        } catch (NumberFormatException e) {
            log.error("版本号格式错误: {}", currentVersion, e);
            return "1.0.0";
        }
    }

    @Override
    public String generateNextVersionNum(String currentVersion, String versionType) {
        if (StringUtils.isBlank(currentVersion) || !currentVersion.contains(".")) {
            return "1.0.0";
        }

        String[] parts = currentVersion.split("\\.");
        if (parts.length != 3) {
            return "1.0.0";
        }

        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch = Integer.parseInt(parts[2]);

            // 根据版本类型增加相应部分
            if ("MAJOR".equals(versionType)) {
                major++;
                minor = 0;
                patch = 0;
            } else if ("MINOR".equals(versionType)) {
                minor++;
                patch = 0;
            } else if ("PATCH".equals(versionType)) {
                patch++;
            } else {
                // 默认增加小版本号
                minor++;
            }

            return major + "." + minor + "." + patch;
        } catch (NumberFormatException e) {
            log.error("版本号格式错误: {}", currentVersion, e);
            return "1.0.0";
        }
    }

    /**
     * 更新之前的版本为非当前版本
     */
    private void updatePreviousVersions(String modelId) {
        LambdaQueryWrapper<TaskVersion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TaskVersion::getModelId, modelId)
                .eq(TaskVersion::getIsCurrent, true);

        List<TaskVersion> currentVersions = list(queryWrapper);

        if (!currentVersions.isEmpty()) {
            currentVersions.forEach(v -> {
                v.setIsCurrent(false);
                v.setUpdateTime(new Date());
            });

            boolean updated = updateBatchById(currentVersions);
            if (!updated) {
                log.warn("更新之前的版本失败");
            }
        }
    }

    @Override
    public TaskVersion getVersionDetail(String versionId) {
        if (StringUtils.isBlank(versionId)) {
            throw new IllegalArgumentException("版本ID不能为空");
        }
        return getById(versionId);
    }

    @Override
    public List<TaskVersion> getVersionsByTaskId(String taskId) {
        if (StringUtils.isBlank(taskId)) {
            throw new IllegalArgumentException("任务ID不能为空");
        }

        LambdaQueryWrapper<TaskVersion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TaskVersion::getTaskId, taskId)
                .orderByDesc(TaskVersion::getPublishTime);

        return list(queryWrapper);
    }

    @Override
    public List<TaskVersion> getModelVersions(String modelId, String dataSourceId, String databaseId) {
        if (StringUtils.isBlank(modelId)) {
            throw new IllegalArgumentException("模型ID不能为空");
        }

        LambdaQueryWrapper<TaskVersion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TaskVersion::getModelId, modelId);

        // 根据数据源ID和数据库ID进一步过滤
        if (StringUtils.isNotBlank(dataSourceId)) {
            queryWrapper.eq(TaskVersion::getDataSourceId, dataSourceId);
        }

        if (StringUtils.isNotBlank(databaseId)) {
            queryWrapper.eq(TaskVersion::getDatabaseId, databaseId);
        }

        queryWrapper.orderByDesc(TaskVersion::getPublishTime);

        return list(queryWrapper);
    }

    @Override
    public PageResult<TaskVersion> pageVersionsByModelId(String modelId, int pageNum, int pageSize) {
        if (StringUtils.isBlank(modelId)) {
            throw new IllegalArgumentException("模型ID不能为空");
        }

        // 创建分页对象
        Page<TaskVersion> page = new Page<>(pageNum, pageSize);

        // 构建查询条件
        LambdaQueryWrapper<TaskVersion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TaskVersion::getModelId, modelId)
                .orderByDesc(TaskVersion::getPublishTime);

        // 执行查询
        IPage<TaskVersion> result = page(page, queryWrapper);

        // 转换为PageResult
        PageResult<TaskVersion> pageResult = new PageResult<>();
        pageResult.setPageNum(result.getCurrent())
                .setPageSize(result.getSize())
                .setTotal(result.getTotal())
                .setPages(result.getPages())
                .setRecords(result.getRecords())
                .setHasNext(result.getCurrent() < result.getPages())
                .setHasPrevious(result.getCurrent() > 1);

        return pageResult;
    }

    @Transactional(rollbackFor = Exception.class)
    public String rollbackVersion(String versionId, String description) {
        if (StringUtils.isBlank(versionId)) {
            throw new IllegalArgumentException("版本ID不能为空");
        }

        // 获取要回滚到的版本
        TaskVersion targetVersion = getById(versionId);
        if (targetVersion == null) {
            throw new IllegalArgumentException("要回滚的版本不存在");
        }

        // 验证版本状态
        if (!"success".equals(targetVersion.getStatus())) {
            throw new IllegalArgumentException("只能回滚到成功状态的版本");
        }

        log.info("开始回滚到版本: {}, 版本号: {}", versionId, targetVersion.getVersionNum());

        // 获取最新版本号
        String newVersionNum = generateNextVersionNum(targetVersion.getModelId());

        // 创建回滚版本
        TaskVersion rollbackVersion = new TaskVersion();
        rollbackVersion.setTaskId(targetVersion.getTaskId());
        rollbackVersion.setModelId(targetVersion.getModelId());
        rollbackVersion.setVersionNum(newVersionNum);
        rollbackVersion.setEnvironment(targetVersion.getEnvironment());
        rollbackVersion.setStatus("running"); // 初始状态为运行中
        rollbackVersion.setPublisher(UserUtils.getCurrentUsername());
        rollbackVersion.setPublishTime(LocalDateTime.now());

        // 回滚描述
        if (StringUtils.isNotBlank(description)) {
            rollbackVersion.setDescription(description);
        } else {
            rollbackVersion.setDescription("回滚到版本 " + targetVersion.getVersionNum());
        }

        rollbackVersion.setMode("rollback");
        rollbackVersion.setIsCurrent(false); // 初始不是当前版本
        rollbackVersion.setIsRollback(true);
        rollbackVersion.setRollbackFromVersionId(targetVersion.getId());

        // 复制目标版本配置
        rollbackVersion.setConfig(targetVersion.getConfig());

        // 创建变更记录
        List<Map<String, Object>> changes = new ArrayList<>();
        Map<String, Object> change = new HashMap<>();
        change.put("type", "rollback");
        change.put("path", "全部");
        change.put("description", "回滚到版本 " + targetVersion.getVersionNum());
        changes.add(change);
        rollbackVersion.setChanges(changes);

        // 初始化日志
        List<Map<String, Object>> logs = new ArrayList<>();
        Map<String, Object> versionLogEntry = new HashMap<>();
        versionLogEntry.put("timestamp", LocalDateTime.now().toString());
        versionLogEntry.put("level", "INFO");
        versionLogEntry.put("message", "开始回滚到版本 " + targetVersion.getVersionNum());
        logs.add(versionLogEntry);
        rollbackVersion.setLogs(logs);

        // 设置创建和更新时间
        Date now = new Date();
        rollbackVersion.setCreateTime(now);
        rollbackVersion.setUpdateTime(now);

        // 保存回滚版本
        boolean saved = save(rollbackVersion);
        if (!saved) {
            throw new RuntimeException("保存回滚版本失败");
        }

        // 执行回滚操作
        try {
            executeRollback(rollbackVersion, targetVersion);
        } catch (Exception e) {
            log.error("执行回滚操作失败: {}", e.getMessage(), e);

            // 更新版本状态为失败
            rollbackVersion.setStatus("failed");
            rollbackVersion.setUpdateTime(new Date());
            updateById(rollbackVersion);

            // 添加错误日志
            addVersionLog(rollbackVersion.getId(), "ERROR", "回滚执行失败: " + e.getMessage());

            throw new RuntimeException("执行回滚操作失败: " + e.getMessage(), e);
        }

        return rollbackVersion.getId();
    }

    /**
     * 执行回滚操作
     */
    private void executeRollback(TaskVersion rollbackVersion, TaskVersion targetVersion) {
        // 获取任务信息
        MaterializeTask task = taskService.getById(rollbackVersion.getTaskId());
        if (task == null) {
            throw new RuntimeException("关联的任务不存在");
        }

        log.info("开始执行回滚操作: 从当前版本回滚到 {}", targetVersion.getVersionNum());

        try {
            // 1. 获取目标版本的配置
            Map<String, Object> config = targetVersion.getConfig();
            if (config == null) {
                config = new HashMap<>();
            }

            // 2. 将配置应用到任务并更新
            updateTaskWithConfig(task, config);

            // 3. 执行回滚操作
            // 这里应该包含实际的回滚逻辑，可能包括:
            // - 恢复数据库表结构和数据
            // - 重新执行物化操作
            // - 等其他操作
            // 实际逻辑应根据项目需求实现

            // 模拟回滚操作延迟
            Thread.sleep(2000);

            // 4. 回滚完成后更新状态
            rollbackVersion.setStatus("success");
            rollbackVersion.setIsCurrent(true);
            rollbackVersion.setUpdateTime(new Date());
            updateById(rollbackVersion);

            // 5. 将其他版本标记为非当前版本
            updateOtherVersionsNotCurrent(rollbackVersion.getModelId(), rollbackVersion.getId());

            // 6. 记录回滚完成日志
            Map<String, Object> versionLogEntry = new HashMap<>();
            versionLogEntry.put("timestamp", LocalDateTime.now().toString());
            versionLogEntry.put("level", "INFO");
            versionLogEntry.put("message", "回滚完成，当前为活动版本");

            // 添加日志记录
            addVersionLog(rollbackVersion.getId(), "INFO", "回滚完成，当前为活动版本");

            log.info("版本回滚成功，版本ID: {}", rollbackVersion.getId());
        } catch (Exception e) {
            log.error("执行回滚操作失败: {}", e.getMessage(), e);
            throw new RuntimeException("执行回滚操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新任务配置
     */
    private void updateTaskWithConfig(MaterializeTask task, Map<String, Object> config) {
        // 根据配置更新任务属性
        if (config.containsKey("materializeStrategy")) {
            task.setMaterializeStrategy(config.get("materializeStrategy").toString());
        }

        if (config.containsKey("refreshMethod")) {
            task.setRefreshMethod(config.get("refreshMethod").toString());
        }

        if (config.containsKey("executionMode")) {
            task.setExecutionMode(config.get("executionMode").toString());
        }

        if (config.containsKey("errorHandling")) {
            task.setErrorHandling(config.get("errorHandling").toString());
        }

        // 更新任务
        task.setUpdateTime(new Date());
        taskService.updateById(task);
    }

    /**
     * 将模型的其他版本设置为非当前版本
     */
    private void updateOtherVersionsNotCurrent(String modelId, String currentVersionId) {
        LambdaQueryWrapper<TaskVersion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TaskVersion::getModelId, modelId)
                .eq(TaskVersion::getIsCurrent, true)
                .ne(TaskVersion::getId, currentVersionId);

        List<TaskVersion> versions = list(queryWrapper);

        for (TaskVersion version : versions) {
            version.setIsCurrent(false);
            version.setUpdateTime(new Date());
            updateById(version);
        }
    }

    /**
     * 添加版本日志
     */
    public boolean addVersionLog(String versionId, String level, String message) {
        if (StringUtils.isBlank(versionId)) {
            log.warn("版本ID为空，无法添加日志");
            return false;
        }

        TaskVersion version = getById(versionId);
        if (version == null) {
            log.warn("版本 {} 不存在，无法添加日志", versionId);
            return false;
        }

        List<Map<String, Object>> logs = version.getLogs();
        if (logs == null) {
            logs = new ArrayList<>();
        }

        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("timestamp", LocalDateTime.now().toString());
        logEntry.put("level", level);
        logEntry.put("message", message);

        logs.add(logEntry);
        version.setLogs(logs);
        version.setUpdateTime(new Date());

        // 更新版本
        version.setUpdateTime(new Date());
        return updateById(version);
    }

    @Override
    public Map<String, Object> compareVersions(String versionId1, String versionId2) {
        // 参数校验
        if (StringUtils.isBlank(versionId1) || StringUtils.isBlank(versionId2)) {
            throw new IllegalArgumentException("版本ID不能为空");
        }

        TaskVersion version1 = getById(versionId1);
        TaskVersion version2 = getById(versionId2);

        if (version1 == null || version2 == null) {
            throw new IllegalArgumentException("比较的版本不存在");
        }

        // 如果两个版本属于不同的模型，不允许比较
        if (!version1.getModelId().equals(version2.getModelId())) {
            throw new IllegalArgumentException("只能比较同一模型的版本");
        }

        Map<String, Object> result = new HashMap<>();

        // 比较基本信息
        result.put("version1", version1.getVersionNum());
        result.put("version2", version2.getVersionNum());
        result.put("publishTime1", version1.getPublishTime());
        result.put("publishTime2", version2.getPublishTime());
        result.put("publisher1", version1.getPublisher());
        result.put("publisher2", version2.getPublisher());
        result.put("description1", version1.getDescription());
        result.put("description2", version2.getDescription());
        result.put("environment1", version1.getEnvironment());
        result.put("environment2", version2.getEnvironment());

        // 计算配置差异
        Map<String, Object> configDiff = compareConfigs(version1.getConfig(), version2.getConfig());
        result.put("configDiff", configDiff);

        // 计算变更差异
        List<Map<String, Object>> changesDiff = compareChanges(version1.getChanges(), version2.getChanges());
        result.put("changesDiff", changesDiff);

        return result;
    }

    /**
     * 比较两个版本的配置
     */
    private Map<String, Object> compareConfigs(Map<String, Object> config1, Map<String, Object> config2) {
        Map<String, Object> diff = new HashMap<>();

        // 获取所有键的并集
        Set<String> allKeys = new HashSet<>();
        if (config1 != null) allKeys.addAll(config1.keySet());
        if (config2 != null) allKeys.addAll(config2.keySet());

        // 比较每个键的值
        for (String key : allKeys) {
            Object value1 = config1 != null ? config1.get(key) : null;
            Object value2 = config2 != null ? config2.get(key) : null;

            // 如果值不同，添加到差异中
            if (!Objects.equals(value1, value2)) {
                Map<String, Object> valueDiff = new HashMap<>();
                valueDiff.put("old", value1);
                valueDiff.put("new", value2);
                diff.put(key, valueDiff);
            }
        }

        return diff;
    }

    /**
     * 比较两个版本的变更
     */
    private List<Map<String, Object>> compareChanges(List<Map<String, Object>> changes1, List<Map<String, Object>> changes2) {
        List<Map<String, Object>> diffList = new ArrayList<>();

        // 如果任一列表为空，直接返回结果
        if (changes1 == null) changes1 = Collections.emptyList();
        if (changes2 == null) changes2 = Collections.emptyList();

        // 使用路径作为变更记录的唯一标识
        Map<String, Map<String, Object>> changeMap1 = changes1.stream()
                .filter(c -> c.get("path") != null)
                .collect(Collectors.toMap(
                        c -> c.get("path").toString(),
                        c -> c,
                        (o1, o2) -> o1  // 如果有冲突，使用第一个
                ));

        // 找出版本2中的所有变更，并标记哪些是新增的或修改的
        for (Map<String, Object> change2 : changes2) {
            if (change2.get("path") == null) continue;

            String path = change2.get("path").toString();
            Map<String, Object> change1 = changeMap1.get(path);

            Map<String, Object> diffItem = new HashMap<>();
            diffItem.put("path", path);

            if (change1 == null) {
                // 版本2中新增的变更
                diffItem.put("changeType", "added");
                diffItem.put("details", change2);
            } else {
                // 比较版本1和版本2中同一路径的变更
                if (!Objects.equals(change1.get("type"), change2.get("type")) ||
                        !Objects.equals(change1.get("description"), change2.get("description"))) {
                    // 变更的类型或描述不同
                    diffItem.put("changeType", "modified");
                    diffItem.put("oldDetails", change1);
                    diffItem.put("newDetails", change2);
                } else {
                    // 变更相同，不添加到差异列表
                    continue;
                }
            }

            diffList.add(diffItem);
        }

        // 找出版本2中移除的变更
        for (Map<String, Object> change1 : changes1) {
            if (change1.get("path") == null) continue;

            String path = change1.get("path").toString();
            boolean existsInVersion2 = changes2.stream()
                    .anyMatch(c -> path.equals(c.get("path")));

            if (!existsInVersion2) {
                Map<String, Object> diffItem = new HashMap<>();
                diffItem.put("path", path);
                diffItem.put("changeType", "removed");
                diffItem.put("details", change1);
                diffList.add(diffItem);
            }
        }

        return diffList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setVersionAsCurrent(String versionId) {
        if (StringUtils.isBlank(versionId)) {
            throw new IllegalArgumentException("版本ID不能为空");
        }

        // 获取版本信息
        TaskVersion version = getById(versionId);
        if (version == null) {
            throw new IllegalArgumentException("版本不存在");
        }

        // 检查版本状态
        if (!"success".equals(version.getStatus())) {
            throw new IllegalArgumentException("只有成功状态的版本才能设置为当前版本");
        }

        log.info("设置版本 {} 为当前版本", versionId);

        // 先将该模型的所有版本设置为非当前版本
        updateOtherVersionsNotCurrent(version.getModelId(), versionId);

        // 设置当前版本
        version.setIsCurrent(true);
        version.setUpdateTime(new Date());
        boolean updated = updateById(version);

        if (updated) {
            // 添加日志记录
            addVersionLog(versionId, "INFO", "版本被设置为当前版本");
        }

        return updated;
    }

    @Override
    public boolean rollbackToVersion(VersionRollbackRequest request) {
        if (request == null || StringUtils.isBlank(request.getTargetVersionId())) {
            throw new IllegalArgumentException("目标版本ID不能为空");
        }

        // 查询目标版本
        TaskVersion targetVersion = getById(request.getTargetVersionId());
        if (targetVersion == null) {
            throw new RuntimeException("目标版本不存在");
        }

        // 验证版本状态，只有成功的版本才能回滚
        if (!"success".equals(targetVersion.getStatus())) {
            throw new RuntimeException("只能回滚到成功的版本，当前版本状态: " + targetVersion.getStatus());
        }

        // 获取任务ID
        String taskId = targetVersion.getTaskId();

        // 可以添加更多业务逻辑，例如：
        // 1. 记录回滚操作
        // 2. 创建一个新版本标记为回滚版本
        // 3. 执行实际的回滚操作

        log.info("开始回滚到版本 {}", request.getTargetVersionId());

        try {
            // 这里实现具体的回滚逻辑
            // ...

            // 回滚成功，返回true
            return true;
        } catch (Exception e) {
            log.error("回滚到版本 {} 失败: {}", request.getTargetVersionId(), e.getMessage(), e);
            throw new RuntimeException("回滚失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> checkModelStructureChanges(String modelId, String dataSourceId, String databaseId) {
        Map<String, Object> result = new HashMap<>();
        result.put("hasChanges", false);
        result.put("changes", new ArrayList<>());

        if (StringUtils.isBlank(modelId)) {
            log.warn("模型ID为空，无法检查结构变化");
            return result;
        }

        try {
            // 1. 获取当前最新版本
            TaskVersion latestVersion = null;

            LambdaQueryWrapper<TaskVersion> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TaskVersion::getModelId, modelId);

            if (StringUtils.isNotBlank(dataSourceId)) {
                queryWrapper.eq(TaskVersion::getDataSourceId, dataSourceId);
            }

            if (StringUtils.isNotBlank(databaseId)) {
                queryWrapper.eq(TaskVersion::getDatabaseId, databaseId);
            }

            queryWrapper.orderByDesc(TaskVersion::getPublishTime)
                    .last("LIMIT 1");

            latestVersion = getOne(queryWrapper);

            if (latestVersion == null) {
                // 没有找到最新版本，表示需要创建初始版本
                log.info("未找到模型 {} 的最新版本，需要创建初始版本", modelId);
                result.put("hasChanges", true);
                result.put("changeType", "INITIAL");
                return result;
            }

            // 2. 获取版本相关信息
            String taskId = latestVersion.getTaskId();
            if (StringUtils.isBlank(taskId)) {
                log.warn("版本 {} 未关联任务ID", latestVersion.getId());
                return result;
            }

            // 3. 获取当前模型信息
            ModelEntity currentModel = modelService.getById(modelId);
            if (currentModel == null) {
                log.warn("模型 {} 不存在", modelId);
                return result;
            }

            // 4. 获取版本中保存的配置信息
            Map<String, Object> versionConfig = latestVersion.getConfig();
            if (versionConfig == null) {
                versionConfig = new HashMap<>();
            }

            // 5. 变更记录列表
            List<Map<String, Object>> changes = new ArrayList<>();
            boolean hasChanges = false;

//            // 6. 比较模型基本信息
//            if (compareModelInfo(currentModel, versionConfig, changes)) {
//                hasChanges = true;
//            }
//
            // 7. 获取版本中的任务表信息 (从TaskTable实体)
            List<TaskTable> versionTables = getVersionTables(taskId);
            Map<String, TaskTable> versionTableMap = new HashMap<>();
            for (TaskTable table : versionTables) {
                versionTableMap.put(table.getTableId(), table);
            }

            // 8. 获取最新的表信息 (从Table实体)
            List<Table> currentTables = getModelCurrentTables(modelId);
            Map<String, Table> currentTableMap = new HashMap<>();
            for (Table table : currentTables) {
                currentTableMap.put(table.getId(), table);
            }

            // 9. 比较表结构变化
            if (compareTableStructures(currentTableMap, versionTableMap, changes)) {
                hasChanges = true;
            }

            // 10. 更新结果
            result.put("hasChanges", hasChanges);
            result.put("changes", changes);

            if (hasChanges) {
                // 根据变更类型确定版本类型
                boolean hasMajorChanges = changes.stream()
                        .anyMatch(change -> "TABLE".equals(change.get("type")) && "DELETED".equals(change.get("action")));
                boolean hasMinorChanges = changes.stream()
                        .anyMatch(change -> "FIELD".equals(change.get("type")) && "DELETED".equals(change.get("action")));

                if (hasMajorChanges) {
                    result.put("changeType", "MAJOR");
                } else if (hasMinorChanges) {
                    result.put("changeType", "MINOR");
                } else {
                    result.put("changeType", "PATCH");
                }
            }
        } catch (Exception e) {
            log.error("检查模型结构变化时发生错误", e);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * 获取版本中的任务表
     *
     * @param taskId 任务ID
     * @return 任务表列表
     */
    private List<TaskTable> getVersionTables(String taskId) {
        LambdaQueryWrapper<TaskTable> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TaskTable::getTaskId, taskId);
        return taskTableService.list(queryWrapper);
    }

    /**
     * 获取模型当前的表
     *
     * @param modelId 模型ID
     * @return 表列表
     */
    private List<Table> getModelCurrentTables(String modelId) {
        LambdaQueryWrapper<Table> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Table::getGroupId, modelId);
        return tableService.list(queryWrapper);
    }

    /**
     * 获取版本中任务表的字段
     *
     * @param tableId 任务表ID
     * @return 任务表字段列表
     */
    private List<TaskTableField> getVersionTableFields(String tableId) {
        LambdaQueryWrapper<TaskTableField> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TaskTableField::getTableId, tableId);
        return taskTableFieldService.list(queryWrapper);
    }

    /**
     * 获取当前表的字段
     *
     * @param tableId 表ID
     * @return 表字段列表
     */
    private List<TableField> getCurrentTableFields(String tableId) {
        LambdaQueryWrapper<TableField> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TableField::getTableId, tableId);
        return tableFieldService.list(queryWrapper);
    }

    /**
     * 比较模型基本信息
     */
    private boolean compareModelInfo(ModelEntity currentModel, Map<String, Object> versionConfig, List<Map<String, Object>> changes) {
        boolean hasChanges = false;

        // 比较模型名称
        if (!Objects.equals(currentModel.getName(), versionConfig.get("modelName"))) {
            Map<String, Object> nameChange = new HashMap<>();
            nameChange.put("type", "MODEL");
            nameChange.put("action", "MODIFIED");
            nameChange.put("path", "name");
            nameChange.put("oldValue", versionConfig.get("modelName"));
            nameChange.put("newValue", currentModel.getName());
            nameChange.put("details", "模型名称已变更");
            changes.add(nameChange);
            hasChanges = true;
        }

        // 比较模型类型
        if (!Objects.equals(currentModel.getType(), versionConfig.get("modelType"))) {
            Map<String, Object> typeChange = new HashMap<>();
            typeChange.put("type", "MODEL");
            typeChange.put("action", "MODIFIED");
            typeChange.put("path", "type");
            typeChange.put("oldValue", versionConfig.get("modelType"));
            typeChange.put("newValue", currentModel.getType());
            typeChange.put("details", "模型类型已变更");
            changes.add(typeChange);
            hasChanges = true;
        }

        // 比较模型描述
        if (!Objects.equals(currentModel.getDescription(), versionConfig.get("modelDescription"))) {
            Map<String, Object> descChange = new HashMap<>();
            descChange.put("type", "MODEL");
            descChange.put("action", "MODIFIED");
            descChange.put("path", "description");
            descChange.put("oldValue", versionConfig.get("modelDescription"));
            descChange.put("newValue", currentModel.getDescription());
            descChange.put("details", "模型描述已变更");
            changes.add(descChange);
            hasChanges = true;
        }

        // 比较模型状态
        if (!Objects.equals(currentModel.getStatus(), versionConfig.get("modelStatus"))) {
            Map<String, Object> statusChange = new HashMap<>();
            statusChange.put("type", "MODEL");
            statusChange.put("action", "MODIFIED");
            statusChange.put("path", "status");
            statusChange.put("oldValue", versionConfig.get("modelStatus"));
            statusChange.put("newValue", currentModel.getStatus());
            statusChange.put("details", "模型状态已变更");
            changes.add(statusChange);
            hasChanges = true;
        }

        return hasChanges;
    }

    /**
     * 比较表结构变化
     *
     * @param currentTableMap 当前表映射
     * @param versionTableMap 版本表映射
     * @param changes         变更列表
     * @return 是否有变化
     */
    private boolean compareTableStructures(
            Map<String, Table> currentTableMap,
            Map<String, TaskTable> versionTableMap,
            List<Map<String, Object>> changes) {
        boolean hasChanges = false;

        // 1. 检查版本中存在但当前已不存在的表（删除的表）
        for (String tableId : versionTableMap.keySet()) {
            if (!currentTableMap.containsKey(tableId)) {
                Map<String, Object> tableChange = new HashMap<>();
                tableChange.put("type", "TABLE");
                tableChange.put("action", "DELETED");
                tableChange.put("path", "table/" + tableId);
                tableChange.put("details", "表已被删除");
                TaskTable versionTable = versionTableMap.get(tableId);
                tableChange.put("tableName", versionTable.getTableName());
                changes.add(tableChange);
                hasChanges = true;
            }
        }

        // 2. 检查当前存在但版本中不存在的表（新增的表）
        for (String tableId : currentTableMap.keySet()) {
            if (!versionTableMap.containsKey(tableId)) {
                Map<String, Object> tableChange = new HashMap<>();
                tableChange.put("type", "TABLE");
                tableChange.put("action", "ADDED");
                tableChange.put("path", "table/" + tableId);
                tableChange.put("details", "新增表");
                Table currentTable = currentTableMap.get(tableId);
                tableChange.put("tableName", currentTable.getName());
                changes.add(tableChange);
                hasChanges = true;
            }
        }

        // 3. 比较共有表的变化
        for (String tableId : versionTableMap.keySet()) {
            if (currentTableMap.containsKey(tableId)) {
                TaskTable versionTable = versionTableMap.get(tableId);
                Table currentTable = currentTableMap.get(tableId);

                // 比较表基本信息
                if (compareTableInfo(currentTable, versionTable, tableId, changes)) {
                    hasChanges = true;
                }

                // 比较字段信息
                if (compareTableFieldsInfo(tableId, versionTable.getId(), changes)) {
                    hasChanges = true;
                }
            }
        }

        return hasChanges;
    }

    /**
     * 比较表基本信息
     */
    private boolean compareTableInfo(
            Table currentTable,
            TaskTable versionTable,
            String tableId,
            List<Map<String, Object>> changes) {
        boolean hasChanges = false;

        // 比较表名
        if (!Objects.equals(currentTable.getName(), versionTable.getTableName())) {
            Map<String, Object> tableNameChange = new HashMap<>();
            tableNameChange.put("type", "TABLE");
            tableNameChange.put("action", "MODIFIED");
            tableNameChange.put("path", "table/" + tableId + "/name");
            tableNameChange.put("oldValue", versionTable.getTableName());
            tableNameChange.put("newValue", currentTable.getName());
            tableNameChange.put("details", "表名已变更");
            changes.add(tableNameChange);
            hasChanges = true;
        }

        // 比较表注释
        if (!Objects.equals(currentTable.getDisplayName(), versionTable.getTableComment())) {
            Map<String, Object> tableCommentChange = new HashMap<>();
            tableCommentChange.put("type", "TABLE");
            tableCommentChange.put("action", "MODIFIED");
            tableCommentChange.put("path", "table/" + tableId + "/comment");
            tableCommentChange.put("oldValue", versionTable.getTableComment());
            tableCommentChange.put("newValue", currentTable.getDisplayName());
            tableCommentChange.put("details", "表注释已变更");
            changes.add(tableCommentChange);
            hasChanges = true;
        }

        return hasChanges;
    }

    /**
     * 比较表字段信息
     */
    private boolean compareTableFieldsInfo(String tableId, String taskTableId, List<Map<String, Object>> changes) {
        boolean hasChanges = false;

        // 1. 获取版本中的字段信息 (从TaskTableField实体)
        List<TaskTableField> versionFields = getVersionTableFields(taskTableId);
        Map<String, TaskTableField> versionFieldMap = new HashMap<>();
        for (TaskTableField field : versionFields) {
            // 使用原始的字段ID作为key
            String originalFieldId = field.getFieldName();  // 假设fieldName存储了原始字段的id或唯一标识
            versionFieldMap.put(originalFieldId, field);
        }

        // 2. 获取当前的字段信息 (从TableField实体)
        List<TableField> currentFields = getCurrentTableFields(tableId);
        Map<String, TableField> currentFieldMap = new HashMap<>();
        for (TableField field : currentFields) {
            currentFieldMap.put(field.getName(), field);  // 使用字段名称作为key
        }

        // 3. 检查删除的字段
        for (String fieldName : versionFieldMap.keySet()) {
            if (!currentFieldMap.containsKey(fieldName)) {
                Map<String, Object> fieldChange = new HashMap<>();
                fieldChange.put("type", "FIELD");
                fieldChange.put("action", "DELETED");
                fieldChange.put("path", "table/" + tableId + "/field/" + fieldName);
                fieldChange.put("details", "字段已被删除");
                fieldChange.put("fieldName", fieldName);
                changes.add(fieldChange);
                hasChanges = true;
            }
        }

        // 4. 检查新增的字段
        for (String fieldName : currentFieldMap.keySet()) {
            if (!versionFieldMap.containsKey(fieldName)) {
                Map<String, Object> fieldChange = new HashMap<>();
                fieldChange.put("type", "FIELD");
                fieldChange.put("action", "ADDED");
                fieldChange.put("path", "table/" + tableId + "/field/" + fieldName);
                fieldChange.put("details", "新增字段");
                fieldChange.put("fieldName", fieldName);
                changes.add(fieldChange);
                hasChanges = true;
            }
        }

        // 5. 比较共有字段的变化
        for (String fieldName : versionFieldMap.keySet()) {
            if (currentFieldMap.containsKey(fieldName)) {
                TaskTableField versionField = versionFieldMap.get(fieldName);
                TableField currentField = currentFieldMap.get(fieldName);

                // 比较字段属性
                if (compareFieldAttributes(tableId, fieldName, currentField, versionField, changes)) {
                    hasChanges = true;
                }
            }
        }

        return hasChanges;
    }

    /**
     * 比较字段属性
     */
    private boolean compareFieldAttributes(
            String tableId,
            String fieldName,
            TableField currentField,
            TaskTableField versionField,
            List<Map<String, Object>> changes) {
        boolean hasChanges = false;

        // 比较字段类型
        if (!Objects.equals(currentField.getType(), versionField.getFieldType())) {
            Map<String, Object> fieldTypeChange = new HashMap<>();
            fieldTypeChange.put("type", "FIELD");
            fieldTypeChange.put("action", "MODIFIED");
            fieldTypeChange.put("path", "table/" + tableId + "/field/" + fieldName + "/type");
            fieldTypeChange.put("oldValue", versionField.getFieldType());
            fieldTypeChange.put("newValue", currentField.getType());
            fieldTypeChange.put("details", "字段类型已变更");
            changes.add(fieldTypeChange);
            hasChanges = true;
        }

        // 比较字段长度
        if (!Objects.equals(currentField.getLength(), versionField.getLength())) {
            Map<String, Object> fieldLengthChange = new HashMap<>();
            fieldLengthChange.put("type", "FIELD");
            fieldLengthChange.put("action", "MODIFIED");
            fieldLengthChange.put("path", "table/" + tableId + "/field/" + fieldName + "/length");
            fieldLengthChange.put("oldValue", versionField.getLength());
            fieldLengthChange.put("newValue", currentField.getLength());
            fieldLengthChange.put("details", "字段长度已变更");
            changes.add(fieldLengthChange);
            hasChanges = true;
        }

        // 比较字段精度
        if (!Objects.equals(currentField.getPrecision(), versionField.getPrecision())) {
            Map<String, Object> fieldPrecisionChange = new HashMap<>();
            fieldPrecisionChange.put("type", "FIELD");
            fieldPrecisionChange.put("action", "MODIFIED");
            fieldPrecisionChange.put("path", "table/" + tableId + "/field/" + fieldName + "/precision");
            fieldPrecisionChange.put("oldValue", versionField.getPrecision());
            fieldPrecisionChange.put("newValue", currentField.getPrecision());
            fieldPrecisionChange.put("details", "字段精度已变更");
            changes.add(fieldPrecisionChange);
            hasChanges = true;
        }

        // 比较字段小数位数
        if (!Objects.equals(currentField.getScale(), versionField.getScale())) {
            Map<String, Object> fieldScaleChange = new HashMap<>();
            fieldScaleChange.put("type", "FIELD");
            fieldScaleChange.put("action", "MODIFIED");
            fieldScaleChange.put("path", "table/" + tableId + "/field/" + fieldName + "/scale");
            fieldScaleChange.put("oldValue", versionField.getScale());
            fieldScaleChange.put("newValue", currentField.getScale());
            fieldScaleChange.put("details", "字段小数位数已变更");
            changes.add(fieldScaleChange);
            hasChanges = true;
        }

        // 比较字段主键属性
        if (!Objects.equals(currentField.getIsPrimary(), versionField.getIsPrimary())) {
            Map<String, Object> fieldPrimaryChange = new HashMap<>();
            fieldPrimaryChange.put("type", "FIELD");
            fieldPrimaryChange.put("action", "MODIFIED");
            fieldPrimaryChange.put("path", "table/" + tableId + "/field/" + fieldName + "/isPrimary");
            fieldPrimaryChange.put("oldValue", versionField.getIsPrimary());
            fieldPrimaryChange.put("newValue", currentField.getIsPrimary());
            fieldPrimaryChange.put("details", "字段主键属性已变更");
            changes.add(fieldPrimaryChange);
            hasChanges = true;
        }

        // 比较字段非空属性
        if (!Objects.equals(currentField.getNotNull(), versionField.getNotNull())) {
            Map<String, Object> fieldNullableChange = new HashMap<>();
            fieldNullableChange.put("type", "FIELD");
            fieldNullableChange.put("action", "MODIFIED");
            fieldNullableChange.put("path", "table/" + tableId + "/field/" + fieldName + "/notNull");
            fieldNullableChange.put("oldValue", versionField.getNotNull());
            fieldNullableChange.put("newValue", currentField.getNotNull());
            fieldNullableChange.put("details", "字段非空属性已变更");
            changes.add(fieldNullableChange);
            hasChanges = true;
        }

        // 比较字段默认值
        if (!Objects.equals(currentField.getDefaultValue(), versionField.getDefaultValue())) {
            Map<String, Object> fieldDefaultChange = new HashMap<>();
            fieldDefaultChange.put("type", "FIELD");
            fieldDefaultChange.put("action", "MODIFIED");
            fieldDefaultChange.put("path", "table/" + tableId + "/field/" + fieldName + "/defaultValue");
            fieldDefaultChange.put("oldValue", versionField.getDefaultValue());
            fieldDefaultChange.put("newValue", currentField.getDefaultValue());
            fieldDefaultChange.put("details", "字段默认值已变更");
            changes.add(fieldDefaultChange);
            hasChanges = true;
        }

        // 比较字段注释
        if (!Objects.equals(currentField.getComment(), versionField.getFieldComment())) {
            Map<String, Object> fieldCommentChange = new HashMap<>();
            fieldCommentChange.put("type", "FIELD");
            fieldCommentChange.put("action", "MODIFIED");
            fieldCommentChange.put("path", "table/" + tableId + "/field/" + fieldName + "/comment");
            fieldCommentChange.put("oldValue", versionField.getFieldComment());
            fieldCommentChange.put("newValue", currentField.getComment());
            fieldCommentChange.put("details", "字段注释已变更");
            changes.add(fieldCommentChange);
            hasChanges = true;
        }

        return hasChanges;
    }

    @Override
    public String getCurrentVersionNum(String modelId, String dataSourceId, String databaseId) {
        LambdaQueryWrapper<TaskVersion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TaskVersion::getModelId, modelId);

        if (StringUtils.isNotBlank(dataSourceId)) {
            queryWrapper.eq(TaskVersion::getDataSourceId, dataSourceId);
        }

        if (StringUtils.isNotBlank(databaseId)) {
            queryWrapper.eq(TaskVersion::getDatabaseId, databaseId);
        }

        queryWrapper.orderByDesc(TaskVersion::getVersionNum)
                .last("LIMIT 1");

        TaskVersion latestVersion = getOne(queryWrapper);
        return latestVersion != null ? latestVersion.getVersionNum() : "0.0.0";
    }

    /**
     * 获取模型关联的所有表
     *
     * @param modelId 模型ID
     * @return 表列表
     */
    private List<Map<String, Object>> getModelTables(String modelId) {
        try {
            // 这里应该根据实际情况调用表服务获取模型关联的表
            // 示例: return tableService.getTablesByModelId(modelId);

            // 由于没有具体实现，这里模拟返回空列表
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("获取模型表失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取表的所有字段
     *
     * @param tableId 表ID
     * @return 字段列表
     */
    private List<Map<String, Object>> getTableFields(String tableId) {
        try {
            // 这里应该根据实际情况调用字段服务获取表的字段
            // 示例: return tableFieldService.getFieldsByTableId(tableId);

            // 由于没有具体实现，这里模拟返回空列表
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("获取表字段失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 根据任务ID获取最新版本
     */
    @Override
    public TaskVersion getLatestByTaskId(String taskId) {
        if (StringUtils.isEmpty(taskId)) {
            return null;
        }

        LambdaQueryWrapper<TaskVersion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TaskVersion::getTaskId, taskId)
                .eq(TaskVersion::getIsCurrent, true)
                .orderByDesc(TaskVersion::getPublishTime)
                .last("LIMIT 1");

        return this.getOne(queryWrapper);
    }
} 