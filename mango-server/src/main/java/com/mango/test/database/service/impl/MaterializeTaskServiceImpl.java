package com.mango.test.database.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mango.test.common.util.UuidUtil;
import com.mango.test.common.utils.UserUtils;
import com.mango.test.database.entity.*;
import com.mango.test.database.event.TaskCreateEvent;
import com.mango.test.database.mapper.MaterializeTaskMapper;
import com.mango.test.database.model.FieldStructure;
import com.mango.test.database.model.PageResult;
import com.mango.test.database.model.TableStructure;
import com.mango.test.database.model.request.TaskCreateRequest;
import com.mango.test.database.model.request.TaskHistoryQueryRequest;
import com.mango.test.database.model.request.TaskQueryRequest;
import com.mango.test.database.model.request.VersionCreateRequest;
import com.mango.test.database.service.*;
import com.mango.test.database.service.impl.datasource.AbstractDatabaseHandler;
import com.mango.test.database.service.impl.datasource.DatabaseHandlerFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 物化任务服务实现类
 */
@Service
@SuppressWarnings("unchecked")
public class MaterializeTaskServiceImpl extends ServiceImpl<MaterializeTaskMapper, MaterializeTask> implements MaterializeTaskService {

    @Autowired
    private TaskTableService taskTableService;

    @Autowired
    private TaskVersionService taskVersionService;

    @Autowired(required = false)
    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    private DataSourceService dataSourceService;

    @Autowired
    private DomainService domainService;

    @Autowired
    private TableFieldService tableFieldService;

    @Autowired
    private TableService tableService;

    @Autowired
    private TaskTableFieldService taskTableFieldService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TaskLogService taskLogService;

    // 存储已调度的任务
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    private static final Logger log = LoggerFactory.getLogger(MaterializeTaskServiceImpl.class);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createTask(TaskCreateRequest request) {
        // 参数校验
        if (request == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (StringUtils.isBlank(request.getTaskName())) {
            throw new IllegalArgumentException("任务名称不能为空");
        }

        if (request.getModelIds() == null || request.getModelIds().isEmpty()) {
            throw new IllegalArgumentException("模型ID不能为空");
        }

        // 创建任务
        MaterializeTask task = new MaterializeTask();
        String versionId = UuidUtil.generateUuid();

        // 设置基本信息
        task.setId(UuidUtil.generateUuid());
        task.setTaskName(request.getTaskName());
        task.setDescription(StringUtils.isNotBlank(request.getDescription())
                ? request.getDescription()
                : String.format("物化发布于 %s", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        task.setStatus("WAITING"); // 初始状态为等待中
        task.setModelIds(request.getModelIds());
        task.setProgress(0); // 初始进度为0
        task.setCreator(UserUtils.getCurrentUsername()); // 获取当前用户

        // 设置调度信息
        task.setScheduleType(request.getScheduleType());

        // 处理不同的调度类型
        switch (request.getScheduleType()) {
            case "immediate":
                // 立即执行的任务不需要设置其他调度参数
                break;
            case "scheduled":
                // 定时执行需要验证调度时间
                if (StringUtils.isNotBlank(request.getScheduledTime())) {
                    try {
                        LocalDateTime scheduledTime = LocalDateTime.parse(
                                request.getScheduledTime(),
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        );

                        // 验证调度时间是否在未来
                        if (scheduledTime.isBefore(LocalDateTime.now())) {
                            throw new IllegalArgumentException("调度时间必须是未来时间");
                        }

                        task.setScheduledTime(scheduledTime);
                    } catch (DateTimeParseException e) {
                        throw new IllegalArgumentException("调度时间格式错误，正确格式为: yyyy-MM-dd HH:mm:ss", e);
                    }
                } else {
                    throw new IllegalArgumentException("定时执行任务必须设置调度时间");
                }
                break;
            case "periodic":
                // 周期执行需要验证Cron表达式
                if (StringUtils.isBlank(request.getCronExpression())) {
                    throw new IllegalArgumentException("周期执行任务必须设置Cron表达式");
                }

                // 验证Cron表达式是否有效
                try {
                    // 简单验证Cron表达式格式
                    String[] parts = request.getCronExpression().split("\\s+");
                    if (parts.length != 6) {
                        throw new IllegalArgumentException("Cron表达式格式错误，应为6个部分");
                    }

                    task.setCronExpression(request.getCronExpression());
                } catch (Exception e) {
                    throw new IllegalArgumentException("Cron表达式无效: " + e.getMessage(), e);
                }
                break;
            default:
                throw new IllegalArgumentException("无效的调度类型: " + request.getScheduleType());
        }

        // 设置环境信息
        if (StringUtils.isBlank(request.getEnvironment())) {
            // 默认使用开发环境
            task.setEnvironment("dev");
        } else {
            task.setEnvironment(request.getEnvironment());
        }

        // 设置物化策略
        if (StringUtils.isBlank(request.getMaterializeStrategy())) {
            // 默认使用完全物化
            task.setMaterializeStrategy("complete");
        } else {
            task.setMaterializeStrategy(request.getMaterializeStrategy());
        }

        // 设置数据源信息
        task.setDataSourceType(request.getDataSourceType());

        if ("theme".equals(request.getDataSourceType())) {
            // 主题域数据源
            if (StringUtils.isBlank(request.getDomainId())) {
                throw new IllegalArgumentException("主题域数据源必须设置domainId");
            }

            task.setDomainId(request.getDomainId());
            task.setDomainName(StringUtils.isNotBlank(request.getDomainName())
                    ? request.getDomainName()
                    : getModelDomainName(request.getDomainId()));

            // 清空自定义数据源信息
            task.setCustomDataSourceId(null);
            task.setCustomDataSourceName(null);
            
            // 记录数据源选择日志
            taskLogService.logTask(task.getId(), versionId, "INFO",
                    "任务使用主题域数据源: " + task.getDomainName(), 
                    "CONFIGURE", UserUtils.getCurrentUsername(), 
                    "domainId: " + task.getDomainId());
        } else if ("custom".equals(request.getDataSourceType())) {
            // 自定义数据源
            if (StringUtils.isBlank(request.getCustomDataSourceId())) {
                throw new IllegalArgumentException("自定义数据源必须设置customDataSourceId");
            }

            task.setCustomDataSourceId(request.getCustomDataSourceId());
            task.setCustomDataSourceName(StringUtils.isNotBlank(request.getCustomDataSourceName())
                    ? request.getCustomDataSourceName()
                    : getDataSourceName(request.getCustomDataSourceId()));

            // 清空主题域信息
            task.setDomainId(null);
            task.setDomainName(null);
            
            // 记录数据源选择日志
            taskLogService.logTask(task.getId(), versionId, "INFO",
                    "任务使用自定义数据源: " + task.getCustomDataSourceName(), 
                    "CONFIGURE", UserUtils.getCurrentUsername(), 
                    "customDataSourceId: " + task.getCustomDataSourceId());
        } else {
            throw new IllegalArgumentException("无效的数据源类型: " + request.getDataSourceType());
        }

        // 设置刷新方式
        task.setRefreshMethod(StringUtils.isNotBlank(request.getRefreshMethod())
                ? request.getRefreshMethod()
                : "rebuild");

        // 设置执行模式
        task.setExecutionMode(StringUtils.isNotBlank(request.getExecutionMode())
                ? request.getExecutionMode()
                : "serial");

        // 设置错误处理
        task.setErrorHandling(StringUtils.isNotBlank(request.getErrorHandling())
                ? request.getErrorHandling()
                : "stop");

        // 设置并行度，有效值范围1-10，默认5
        if (request.getParallelism() != null && request.getParallelism() >= 1 && request.getParallelism() <= 10) {
            task.setParallelism(request.getParallelism());
        } else {
            task.setParallelism(5);
        }

        // 设置超时时间，单位分钟，默认30分钟
        if (request.getTimeout() != null && request.getTimeout() > 0) {
            task.setTimeout(request.getTimeout());
        } else {
            task.setTimeout(30);
        }

        // 设置创建和更新时间
       Date now = new Date();
        task.setCreateTime(now);
        task.setUpdateTime(now);


        // 保存任务
        save(task);
        String taskId = task.getId();

        if (StringUtils.isBlank(taskId)) {
            log.error("保存任务失败");
            throw new RuntimeException("保存任务失败");
        }
        
        // 记录任务创建日志
        taskLogService.logTask(taskId, versionId, "INFO",
                "创建任务: " + task.getTaskName(), 
                "CREATE", UserUtils.getCurrentUsername(),
                "调度类型: " + task.getScheduleType() + ", 环境: " + task.getEnvironment() + ", 策略: " + task.getMaterializeStrategy());

        // 获取任务需要的表和字段
        try {

            // 记录版本创建日志
            taskLogService.logTask(taskId, versionId, "INFO", 
                    "创建任务初始版本", 
                    "CREATE_VERSION", UserUtils.getCurrentUsername(), 
                    "versionId: " + versionId);
            
            // 创建初始版本（1.0版本）
            createInitialVersions(task, versionId);

            // 为选中的模型创建任务表
            List<TaskTable> taskTables = createTaskTables(taskId, request.getModelIds(), versionId);
            
            // 记录表创建日志
            taskLogService.logTask(taskId, versionId, "INFO", 
                    "为任务创建了 " + taskTables.size() + " 个表", 
                    "CREATE_TABLES", UserUtils.getCurrentUsername(), 
                    "表数量: " + taskTables.size());

            // 保存表字段信息
            taskTables.forEach(table -> {
                try {
                    // 修改为使用单个参数的调用
                    saveTableFields(table, versionId);
                    
                    // 记录字段保存日志
                    taskLogService.logTaskTable(taskId, versionId, "INFO", 
                            "保存表字段信息: " + table.getTableName(), 
                            "SAVE_FIELDS", UserUtils.getCurrentUsername(), 
                            table.getId(), table.getTableName(), 
                            "表注释: " + table.getTableComment());
                } catch (Exception e) {
                    String errorMsg = "保存表字段信息失败: " + e.getMessage();
                    log.error(errorMsg, e);
                    
                    // 记录错误日志
                    taskLogService.logTaskTable(taskId, versionId, "ERROR", 
                            errorMsg, 
                            "SAVE_FIELDS_ERROR", UserUtils.getCurrentUsername(), 
                            table.getId(), table.getTableName(), 
                            e.getMessage());
                    
                    throw new RuntimeException(errorMsg);
                }
            });

            // 如果是定时任务，安排调度
            if ("scheduled".equals(request.getScheduleType()) || "periodic".equals(request.getScheduleType())) {
                scheduleTask(task);
                
                // 记录任务调度日志
                String scheduleInfo = "scheduled".equals(request.getScheduleType()) 
                        ? "定时执行: " + task.getScheduledTime() 
                        : "周期执行: " + task.getCronExpression();
                
                taskLogService.logTask(taskId, versionId, "INFO", 
                        "设置任务调度", 
                        "SCHEDULE", UserUtils.getCurrentUsername(), 
                        scheduleInfo);
            }

            // 发布任务创建事件
            eventPublisher.publishEvent(new TaskCreateEvent(this, task));
            
            // 记录任务创建完成日志
            taskLogService.logTask(taskId, versionId, "INFO", 
                    "任务创建完成", 
                    "CREATE_COMPLETE", UserUtils.getCurrentUsername(), 
                    "任务ID: " + taskId);

            // 返回任务ID
            return taskId;
        } catch (Exception e) {
            String errorMsg = "创建任务失败: " + e.getMessage();
            log.error(errorMsg, e);
            
            // 记录错误日志
            taskLogService.logTask(taskId, versionId, "ERROR",
                    errorMsg, 
                    "CREATE_ERROR", UserUtils.getCurrentUsername(), 
                    e.getMessage());
            
            removeById(taskId); // 清理创建的任务
            throw new RuntimeException(errorMsg);
        }
    }

    /**
     * 创建任务后的处理
     * 在事务提交后执行，确保所有数据都已保存
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTaskCreation(TaskCreateEvent event) {
        MaterializeTask task = event.getTask();
        if ("immediate".equals(task.getScheduleType())) {
            log.info("任务 {} 设置为立即执行，开始执行", task.getId());
            try {
                // 使用新线程异步执行任务，避免阻塞主线程
                new Thread(() -> {
                    try {
                        executeTask(task.getId());
                    } catch (Exception e) {
                        log.error("立即执行任务 {} 失败: {}", task.getId(), e.getMessage(), e);
                    }
                }).start();
            } catch (Exception e) {
                log.error("启动立即执行任务 {} 失败: {}", task.getId(), e.getMessage(), e);
            }
        }
    }
    

    /**
     * 获取模型所属域名称
     */
    private String getModelDomainName(String domainId) {
        // 实际项目中应该查询数据库获取域名称
        // 这里简化处理，返回一个默认值
        return "未知域";
    }

    /**
     * 获取数据源名称
     */
    private String getDataSourceName(String dataSourceId) {
        // 实际项目中应该查询数据库获取数据源名称
        // 这里简化处理，返回一个默认值
        return "未知数据源";
    }

    /**
     * 获取表名称
     */
    private String getTableName(String tableId) {
        // 实际项目中应该查询数据库获取表名称
        // 这里简化处理，返回一个默认值
        return "未知表";
    }

    /**
     * 创建初始版本
     */
    private void createInitialVersions(MaterializeTask task,String versionId) {
        if (task == null || StringUtils.isBlank(task.getId())) {
            return;
        }

        // 检查是否已配置版本服务
        if (taskVersionService == null) {
            log.warn("未配置TaskVersionService，跳过初始版本创建");
            return;
        }

        try {
            log.info("为任务 {} 创建初始版本", task.getId());

            // 获取模型ID列表
            List<String> modelIds = task.getModelIdList();

            // 获取数据源和数据库ID
            String dataSourceId = null;
            String databaseId = null;

            if ("theme".equals(task.getDataSourceType()) && StringUtils.isNotBlank(task.getDomainId())) {
                dataSourceId = task.getDomainId();
                // 如果需要可以从domainService获取对应的数据库ID
                try {
                    // 这里只是示例，实际实现需要根据系统设计来获取
                    // 例如：databaseId = domainService.getDatabaseId(task.getDomainId());
                    databaseId = task.getDomainId(); // 简化处理，使用domainId作为databaseId
                } catch (Exception e) {
                    log.warn("获取主题域对应数据库ID失败: {}", e.getMessage());
                }
            } else if ("custom".equals(task.getDataSourceType()) && StringUtils.isNotBlank(task.getCustomDataSourceId())) {
                dataSourceId = task.getCustomDataSourceId();
                // 可以从dataSourceService获取对应的数据库ID
                try {
                    // 这里只是示例，实际实现需要根据系统设计来获取
                    // 例如：databaseId = dataSourceService.getDatabaseId(task.getCustomDataSourceId());
                    databaseId = task.getCustomDataSourceId(); // 简化处理，使用customDataSourceId作为databaseId
                } catch (Exception e) {
                    log.warn("获取自定义数据源对应数据库ID失败: {}", e.getMessage());
                }
            }

            // 遍历模型ID列表
            for (String modelId : modelIds) {
                if (StringUtils.isBlank(modelId)) {
                    continue;
                }

                // 检查当前任务是否已有此模型的版本
                LambdaQueryWrapper<TaskVersion> taskVersionQuery = new LambdaQueryWrapper<>();
                taskVersionQuery.eq(TaskVersion::getTaskId, task.getId())
                        .eq(TaskVersion::getModelId, modelId);
                
                List<TaskVersion> taskExistingVersions = taskVersionService.list(taskVersionQuery);
                
                // 如果当前任务已有此模型的版本，跳过创建
                if (!taskExistingVersions.isEmpty()) {
                    log.info("任务 {} 已有模型 {} 的版本，跳过创建初始版本", task.getId(), modelId);
                    continue;
                }

                // 直接创建初始版本1.0，不考虑其他任务中的版本
                String versionNum = "1.0";
                // 直接创建TaskVersion对象
                TaskVersion newVersion = new TaskVersion();
                newVersion.setId(versionId);
                newVersion.setTaskId(task.getId());
                newVersion.setModelId(modelId);
                newVersion.setDataSourceId(dataSourceId);
                newVersion.setDatabaseId(databaseId);
                newVersion.setVersionNum(versionNum);
                newVersion.setEnvironment(task.getEnvironment());
                newVersion.setStatus(task.getStatus());
                newVersion.setPublisher(UserUtils.getCurrentUsername());
                newVersion.setPublishTime(LocalDateTime.now());
                newVersion.setDescription("初始版本 - " + task.getDescription());
                newVersion.setMode("full"); // 初始版本默认为全量
                newVersion.setIsCurrent(true);
                newVersion.setIsRollback(false);

                // 创建配置信息
                Map<String, Object> config = new HashMap<>();
                config.put("materializeStrategy", task.getMaterializeStrategy());
                config.put("refreshMethod", task.getRefreshMethod());

                // 添加辅助配置
                if (StringUtils.isNotBlank(task.getExecutionMode())) {
                    config.put("executionMode", task.getExecutionMode());
                }

                if (StringUtils.isNotBlank(task.getErrorHandling())) {
                    config.put("errorHandling", task.getErrorHandling());
                }

                if (task.getParallelism() != null) {
                    config.put("parallelism", task.getParallelism());
                }

                if (task.getTimeout() != null) {
                    config.put("timeout", task.getTimeout());
                }

                // 数据源配置
                if (StringUtils.isNotBlank(task.getDataSourceType())) {
                    config.put("dataSourceType", task.getDataSourceType());

                    if ("theme".equals(task.getDataSourceType()) && StringUtils.isNotBlank(task.getDomainId())) {
                        config.put("domainId", task.getDomainId());
                        config.put("domainName", task.getDomainName());
                    } else if ("custom".equals(task.getDataSourceType()) && StringUtils.isNotBlank(task.getCustomDataSourceId())) {
                        config.put("customDataSourceId", task.getCustomDataSourceId());
                        config.put("customDataSourceName", task.getCustomDataSourceName());
                    }
                }

                newVersion.setConfig(config);

                // 创建初始变更记录
                List<Map<String, Object>> changes = new ArrayList<>();
                Map<String, Object> change = new HashMap<>();
                change.put("type", "create");
                change.put("path", "initial");
                change.put("description", "初始创建");
                changes.add(change);
                newVersion.setChanges(changes);

                // 创建日志记录
                List<Map<String, Object>> logs = new ArrayList<>();
                Map<String, Object> initialLog = new HashMap<>();
                initialLog.put("timestamp", LocalDateTime.now().toString());
                initialLog.put("level", "INFO");
                initialLog.put("message", "初始版本创建，等待任务执行");
                logs.add(initialLog);
                newVersion.setLogs(logs);

                // 设置创建和更新时间
               Date now = new Date();
                newVersion.setCreateTime(now);
                newVersion.setUpdateTime(now);

                // 保存新版本
                try {
                    boolean saved = taskVersionService.save(newVersion);
                    if (saved) {
                        log.info("为模型 {} 创建初始版本成功，版本ID: {}, 版本号: {}", modelId, newVersion.getId(), versionNum);
                    } else {
                        log.error("为模型 {} 创建初始版本失败", modelId);
                    }
                } catch (Exception e) {
                    log.error("为模型 {} 创建初始版本失败: {}", modelId, e.getMessage(), e);
                    // 单个版本创建失败，继续创建其他版本
                }
            }
        } catch (Exception e) {
            log.error("创建初始版本失败: {}", e.getMessage(), e);
            // 不抛出异常，保证任务创建成功
        }
    }

    /**
     * 保存表字段信息
     */
    private void saveTableFields(TaskTable taskTable,String versionId) {
        try {
            log.info("开始保存表 {} 的字段信息", taskTable.getTableName());

            // 查询原始表字段
            LambdaQueryWrapper<TableField> fieldQuery = new LambdaQueryWrapper<>();
            fieldQuery.eq(TableField::getTableId, taskTable.getTableId());
            fieldQuery.orderByAsc(TableField::getFieldOrder);
            List<TableField> sourceFields = tableFieldService.list(fieldQuery);

            if (sourceFields == null || sourceFields.isEmpty()) {
                log.warn("表 {} 没有字段信息", taskTable.getTableName());
                return;
            }

            // 创建任务表字段记录
            List<TaskTableField> taskFields = new ArrayList<>();
            Date currentTime = new Date(); // 使用不同的变量名避免重复

            for (TableField sourceField : sourceFields) {
                TaskTableField taskField = new TaskTableField();
                taskField.setId(UuidUtil.generateUuid());

                // 设置关联关系
                taskField.setTaskId(taskTable.getTaskId());
                taskField.setTableId(taskTable.getId());

                // 复制字段属性
                taskField.setFieldName(sourceField.getName());
                taskField.setFieldType(sourceField.getType());
                taskField.setLength(sourceField.getLength());
                taskField.setPrecision(sourceField.getPrecision());
                taskField.setScale(sourceField.getScale());
                taskField.setIsPrimary(sourceField.getIsPrimary());
                taskField.setNotNull(sourceField.getNotNull());
                taskField.setAutoIncrement(sourceField.getMgautoIncrement());
                taskField.setDefaultValue(sourceField.getDefaultValue());
                taskField.setFieldComment(sourceField.getMgcomment());

                // 设置创建和更新时间
                taskField.setCreateTime(currentTime);
                taskField.setUpdateTime(currentTime);
                taskField.setVersionId(versionId);
                taskFields.add(taskField);
            }

            // 批量保存
            if (!taskFields.isEmpty()) {
                boolean saved = taskTableFieldService.saveBatch(taskFields);
                if (saved) {
                    log.info("成功保存表 {} 的 {} 个字段", taskTable.getTableName(), taskFields.size());
                } else {
                    log.error("保存表 {} 的字段信息失败", taskTable.getTableName());
                }
            }
        } catch (Exception e) {
            log.error("保存表字段信息失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 获取表结构信息 - 修改为优先使用保存的字段信息并进行字段类型转换
     */
    private TableStructure getTableStructure(MaterializeTask task, TaskTable table) {
        TableStructure tableStructure = new TableStructure();
        tableStructure.setTableName(table.getTableName());
        tableStructure.setDisplayName(table.getTableComment());

        // 首先尝试从任务表字段获取
        List<FieldStructure> fields = getFieldsFromTaskTableFields(task.getId(), table.getId());

        // 如果未找到任务表字段，则从原始表字段获取
        if (fields == null || fields.isEmpty()) {
            log.info("未找到任务 {} 表 {} 的保存字段，尝试从原始表获取", task.getId(), table.getTableName());
            fields = getFieldsFromOriginalTable(table.getTableId());

            if (fields == null || fields.isEmpty()) {
                log.warn("未找到表 {} 的任何字段信息，使用默认字段", table.getTableName());
                fields = createDefaultFields();
            }
        }

        // 获取数据库处理器用于类型转换
        AbstractDatabaseHandler dbHandler = null;
        String targetDbType = null;
        try {
            dbHandler = getDbHandler(task);
            targetDbType = dbHandler.getDatabaseType();
            log.info("目标数据库类型: {}", targetDbType);
        } catch (Exception e) {
            log.error("获取数据库处理器失败: {}", e.getMessage(), e);
            // 使用备用方法获取数据库类型
            targetDbType = getDbTypeForTask(task);
            log.info("使用备用方法获取目标数据库类型: {}", targetDbType);
        }

        // 处理字段类型转换和默认值处理
        if (fields != null && !fields.isEmpty()) {
            for (FieldStructure field : fields) {
                // 原始字段类型
                String fieldType = field.getType().toUpperCase();

                // 进行类型转换
                if (targetDbType != null && dbHandler != null) {
                    try {
                        // 推测源数据库类型（简化处理，实际应从源表中获取）
                        String sourceDbType = guessSourceDbType(fieldType);

                        // 使用数据库处理器进行类型转换
                        String convertedType = dbHandler.convertFromOtherDbType(fieldType, sourceDbType);

                        if (convertedType != null && !convertedType.equals(fieldType)) {
                            log.info("字段 {} 类型从 {} 转换为 {}", field.getName(), fieldType, convertedType);
                            field.setType(convertedType);
                        }
                    } catch (Exception e) {
                        log.warn("字段 {} 类型转换失败: {}", field.getName(), e.getMessage());
                    }
                }

                // 处理日期类型的默认值
                String currentFieldType = field.getType().toUpperCase();
                if ((currentFieldType.contains("DATE") || currentFieldType.contains("TIMESTAMP") || currentFieldType.contains("TIME"))
                     && field.getDefaultValue() != null) {
                    String defaultValue = field.getDefaultValue();

                    // 针对达梦数据库修正日期/时间默认值
                    if ("dm".equalsIgnoreCase(targetDbType) ||
                        "dm7".equalsIgnoreCase(targetDbType) ||
                        "dm8".equalsIgnoreCase(targetDbType)) {

                        if (defaultValue.toUpperCase().equals("CURRENT_TIMESTAMP") ||
                            defaultValue.toUpperCase().contains("NOW()")) {
                            field.setDefaultValue("SYSDATE");
                        } else if (defaultValue.startsWith("'") && defaultValue.endsWith("'") &&
                                   !defaultValue.toUpperCase().contains("TO_DATE")) {
                            field.setDefaultValue("TO_DATE(" + defaultValue + ", 'YYYY-MM-DD HH24:MI:SS')");
                        }
                    }
                }
            }
        }

        tableStructure.setFields(fields);
        return tableStructure;
    }

    /**
     * 根据字段类型推测源数据库类型
     */
    private String guessSourceDbType(String fieldType) {
        // 根据字段类型特征推测源数据库类型
        if (fieldType.equals("VARCHAR2") || fieldType.equals("NUMBER") || fieldType.contains("CLOB")) {
            return "oracle";
        } else if (fieldType.equals("INT") || fieldType.equals("BIGINT") || fieldType.equals("LONGTEXT")) {
            return "mysql";
        } else if (fieldType.contains("SERIAL") || fieldType.equals("BYTEA")) {
            return "postgresql";
        } else {
            // 默认使用标准SQL类型
            return "standard";
        }
    }

    /**
     * 从任务表字段获取字段结构
     * 修复 LocalDateTime 与 Date 类型不兼容问题
     */
    private List<FieldStructure> getFieldsFromTaskTableFields(String taskId, String taskTableId) {
        try {
            LambdaQueryWrapper<TaskTableField> query = new LambdaQueryWrapper<>();
            query.eq(TaskTableField::getTaskId, taskId);
            query.eq(TaskTableField::getTableId, taskTableId);
            query.orderByAsc(TaskTableField::getFieldOrder);

            List<TaskTableField> taskFields = taskTableFieldService.list(query);

            if (taskFields != null && !taskFields.isEmpty()) {
                return taskFields.stream().map(tf -> {
                    FieldStructure fs = new FieldStructure();
                    fs.setName(tf.getFieldName());
                    fs.setType(tf.getFieldType());
                    fs.setLength(tf.getLength());
                    fs.setPrecision(tf.getPrecision());
                    fs.setScale(tf.getScale());
                    fs.setPrimary(tf.getIsPrimary() != null && tf.getIsPrimary() == 1);
                    fs.setNotNull(tf.getNotNull() != null && tf.getNotNull() == 1);
                    fs.setAutoIncrement(tf.getAutoIncrement() != null && tf.getAutoIncrement() == 1);
                    fs.setDefaultValue(tf.getDefaultValue());
                    fs.setComment(tf.getFieldComment());

                    // 如果有日期类型的处理，需要解决 LocalDateTime 到 Date 的转换
                    // 这里我们可以根据原代码进行适当处理

                    return fs;
                }).collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("从任务表字段获取字段结构失败: {}", e.getMessage(), e);
        }

        return null;
    }

    /**
     * 获取任务的数据库类型
     */
    private String getDbTypeForTask(MaterializeTask task) {
        try {
            // 这里根据您的业务逻辑获取数据库类型
            if ("theme".equals(task.getDataSourceType())) {
                // 从主题域获取
                Domain domain = domainService.getById(task.getDomainId());
                if (domain != null && StringUtils.isNotBlank(domain.getDataSourceId())) {
                    DataSource dataSource = dataSourceService.getById(domain.getDataSourceId());
                    if (dataSource != null) {
                        return dataSource.getType();
                    }
                }
            } else if ("custom".equals(task.getDataSourceType())) {
                // 从自定义数据源获取
                DataSource dataSource = dataSourceService.getById(task.getCustomDataSourceId());
                if (dataSource != null) {
                    return dataSource.getType();
                }
            }
        } catch (Exception e) {
            log.error("获取任务数据库类型失败: {}", e.getMessage());
        }

        // 默认返回
        return "mysql";
    }

    /**
     * 从原始表获取字段结构
     */
    private List<FieldStructure> getFieldsFromOriginalTable(String tableId) {
        try {
            LambdaQueryWrapper<TableField> query = new LambdaQueryWrapper<>();
            query.eq(TableField::getTableId, tableId);
            query.orderByAsc(TableField::getFieldOrder);

            List<TableField> tableFields = tableFieldService.list(query);

            if (tableFields != null && !tableFields.isEmpty()) {
                return tableFields.stream().map(tf -> {
                    FieldStructure fs = new FieldStructure();
                    fs.setName(tf.getName());
                    fs.setType(tf.getType());
                    fs.setLength(tf.getLength());
                    fs.setPrecision(tf.getPrecision());
                    fs.setScale(tf.getScale());
                    fs.setPrimary(tf.getIsPrimary() != null && tf.getIsPrimary() == 1);
                    fs.setNotNull(tf.getNotNull() != null && tf.getNotNull() == 1);
                    fs.setAutoIncrement(tf.getMgautoIncrement() != null && tf.getMgautoIncrement() == 1);
                    fs.setDefaultValue(tf.getDefaultValue());
                    fs.setComment(tf.getMgcomment());
                    return fs;
                }).collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("从原始表获取字段结构失败: {}", e.getMessage(), e);
        }

        return null;
    }

    /**
     * 创建默认字段结构
     */
    private List<FieldStructure> createDefaultFields() {
        List<FieldStructure> fields = new ArrayList<>();

        // ID字段
        FieldStructure idField = new FieldStructure();
        idField.setName("id");
        idField.setType("BIGINT");
        idField.setPrimary(true);
        idField.setNotNull(true);
        idField.setAutoIncrement(true);
        idField.setComment("主键ID");
        fields.add(idField);

        // 名称字段
        FieldStructure nameField = new FieldStructure();
        nameField.setName("name");
        nameField.setType("VARCHAR");
        nameField.setLength(100);
        nameField.setNotNull(true);
        nameField.setComment("名称");
        fields.add(nameField);

        // 描述字段
        FieldStructure descField = new FieldStructure();
        descField.setName("description");
        descField.setType("TEXT");
        descField.setComment("描述");
        fields.add(descField);

        // 创建时间字段
        FieldStructure createTimeField = new FieldStructure();
        createTimeField.setName("create_time");
        createTimeField.setType("DATETIME");
        createTimeField.setNotNull(true);
        createTimeField.setDefaultValue("CURRENT_TIMESTAMP");
        createTimeField.setComment("创建时间");
        fields.add(createTimeField);

        // 更新时间字段
        FieldStructure updateTimeField = new FieldStructure();
        updateTimeField.setName("update_time");
        updateTimeField.setType("DATETIME");
        updateTimeField.setNotNull(true);
        updateTimeField.setDefaultValue("CURRENT_TIMESTAMP");
        updateTimeField.setComment("更新时间");
        fields.add(updateTimeField);

        return fields;
    }

    @Override
    public PageResult<MaterializeTask> pageTask(TaskQueryRequest request) {
        // 创建分页对象
        Page<MaterializeTask> page = new Page<>(request.getPageNum(), request.getPageSize());

        // 构建查询条件
        LambdaQueryWrapper<MaterializeTask> queryWrapper = new LambdaQueryWrapper<>();

        // 添加查询条件
        if (StringUtils.isNotBlank(request.getTaskName())) {
            queryWrapper.like(MaterializeTask::getTaskName, request.getTaskName());
        }

        if (StringUtils.isNotBlank(request.getStatus())) {
            queryWrapper.eq(MaterializeTask::getStatus, request.getStatus());
        }

        if (StringUtils.isNotBlank(request.getEnvironment())) {
            queryWrapper.eq(MaterializeTask::getEnvironment, request.getEnvironment());
        }

        if (StringUtils.isNotBlank(request.getCreator())) {
            queryWrapper.eq(MaterializeTask::getCreator, request.getCreator());
        }

        // 处理时间范围查询
        if (StringUtils.isNotBlank(request.getStartTime())) {
            LocalDateTime startTime = LocalDateTime.parse(request.getStartTime(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            queryWrapper.ge(MaterializeTask::getCreateTime, startTime);
        }

        if (StringUtils.isNotBlank(request.getEndTime())) {
            LocalDateTime endTime = LocalDateTime.parse(request.getEndTime(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            queryWrapper.le(MaterializeTask::getCreateTime, endTime);
        }

        // 如果有模型ID，查询关联的任务
        if (StringUtils.isNotBlank(request.getModelId())) {
            // 达梦数据库适配的SQL
            queryWrapper.apply("EXISTS (SELECT 1 FROM t_task_table WHERE t_task_table.task_id = t_materialize_task.id AND t_task_table.model_id = {0})", request.getModelId());
        }

        // 默认按创建时间倒序排序
        queryWrapper.orderByDesc(MaterializeTask::getCreateTime);

        // 执行查询
        IPage<MaterializeTask> result = page(page, queryWrapper);

        // 转换为PageResult
        PageResult<MaterializeTask> pageResult = new PageResult<>();
        pageResult.setPageNum(result.getCurrent())
                .setPageSize(result.getSize())
                .setTotal(result.getTotal())
                .setPages(result.getPages())
                .setRecords(result.getRecords())
                .setHasNext(result.getCurrent() < result.getPages())
                .setHasPrevious(result.getCurrent() > 1);

        return pageResult;
    }

    @Override
    public MaterializeTask getTaskDetail(String taskId) {
        // 获取基本任务信息
        MaterializeTask task = getById(taskId);
        if (task == null) {
            return null;
        }
        
        // 获取任务相关版本信息
        List<TaskVersion> versions = taskVersionService.getVersionsByTaskId(taskId);
        task.setVersions(versions);
        
        // 获取版本统计信息
        Map<String, Integer> versionStats = new HashMap<>();
        for (TaskVersion version : versions) {
            String status = version.getStatus();
            versionStats.put(status, versionStats.getOrDefault(status, 0) + 1);
        }
        task.setVersionStats(versionStats);
        
        // 获取任务关联的模型信息
        List<String> modelIds = task.getModelIdList();
        if (!modelIds.isEmpty()) {
            // 这里可以根据模型ID获取模型的详细信息
            // 假设modelService.getModelsByIds返回模型列表
            // task.setModels(modelService.getModelsByIds(modelIds));
            
            // 如果没有modelService，可以设置一个简单的模型列表，前端可以根据ID获取
            List<Map<String, Object>> models = new ArrayList<>();
            for (String modelId : modelIds) {
                Map<String, Object> model = new HashMap<>();
                model.put("id", modelId);
                model.put("name", getModelName(modelId)); // 可以实现getModelName方法获取模型名称
                model.put("type", "数据模型");
                model.put("status", "ACTIVE");
                model.put("version", getModelLatestVersion(modelId, task)); // 获取模型最新版本
                models.add(model);
            }
            task.setModels(models);
        }
        
        // 获取任务日志信息
        try {
            String logs = getTaskLogs(taskId);
            task.setLogs(logs);
        } catch (Exception e) {
            log.error("获取任务日志失败", e);
            task.setLogs("获取日志失败: " + e.getMessage());
        }
        
        return task;
    }
    
    /**
     * 根据模型ID获取模型名称
     */
    private String getModelName(String modelId) {
        try {
            // 这里应该根据实际情况实现获取模型名称的逻辑
            // 例如通过调用模型服务或查询模型表
            // 示例实现:
            /*
            Model model = modelService.getById(modelId);
            if (model != null) {
                return model.getName();
            }
            */
            
            // 临时返回ID作为名称
            return "模型-" + modelId.substring(0, 8);
        } catch (Exception e) {
            log.error("获取模型名称失败: {}", e.getMessage(), e);
            return "未知模型";
        }
    }
    
    /**
     * 获取模型在当前任务中的最新版本
     */
    private String getModelLatestVersion(String modelId, MaterializeTask task) {
        try {
            List<TaskVersion> versions = task.getVersions();
            if (versions != null && !versions.isEmpty()) {
                for (TaskVersion version : versions) {
                    if (modelId.equals(version.getModelId())) {
                        return version.getVersionNum();
                    }
                }
            }
            return "1.0"; // 默认版本
        } catch (Exception e) {
            log.error("获取模型版本失败: {}", e.getMessage(), e);
            return "未知";
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean executeTask(String taskId) {
        MaterializeTask task = getById(taskId);
        if (task == null) {
            log.warn("任务 {} 不存在", taskId);
            return false;
        }

        // 取消已调度的任务
        cancelScheduledTask(taskId);
        
        // 版本控制：检查表结构和字段是否有变化，如果有则升级版本
        checkAndUpdateVersion(task);

        // 更新任务状态为运行中
        task.setStatus("RUNNING");
        task.setStartTime(LocalDateTime.now());
        task.setProgress(0);
        task.setUpdateTime(new Date());
        updateById(task);
        log.info("任务 {} 开始执行", taskId);

        // 异步执行物化任务
        // 实际项目中，这里应该使用线程池或任务调度框架来异步执行
        asyncExecuteTask(task);

        return true;
    }
    
    /**
     * 检查表结构和字段是否有变化，如果有则升级版本
     */
    private void checkAndUpdateVersion(MaterializeTask task) {
        if (task == null || StringUtils.isBlank(task.getId())) {
            return;
        }
        
        if (taskVersionService == null) {
            log.warn("未配置TaskVersionService，跳过版本升级检查");
            return;
        }
        
        try {
            log.info("检查任务 {} 的表结构和字段变化", task.getId());
            
            // 分析任务结构变化
            Map<String, Object> structureChanges = analyzeTaskStructureChanges(task.getId());
            boolean hasChanges = (boolean) structureChanges.getOrDefault("hasChanges", false);
            
            if (hasChanges) {
                List<String> modelsChanged = (List<String>) structureChanges.getOrDefault("modelsChanged", new ArrayList<>());
                Map<String, Object> changeDetails = (Map<String, Object>) structureChanges.getOrDefault("changeDetails", new HashMap<>());
                
                log.info("检测到任务 {} 的模型结构发生变化，需要升级版本", task.getId());
                
                // 获取数据源和数据库ID
                String dataSourceId = null;
                String databaseId = null;
                
                if ("theme".equals(task.getDataSourceType()) && StringUtils.isNotBlank(task.getDomainId())) {
                    dataSourceId = task.getDomainId();
                    databaseId = task.getDomainId();
                } else if ("custom".equals(task.getDataSourceType()) && StringUtils.isNotBlank(task.getCustomDataSourceId())) {
                    dataSourceId = task.getCustomDataSourceId();
                    databaseId = task.getCustomDataSourceId();
                }
                
                // 为每个变化的模型创建新版本
                for (String modelId : modelsChanged) {
                    try {
                        // 查找此模型在相同数据库上已有的版本
                        LambdaQueryWrapper<TaskVersion> queryWrapper = new LambdaQueryWrapper<>();
                        queryWrapper.eq(TaskVersion::getModelId, modelId)
                                .eq(TaskVersion::getTaskId, task.getId())
                                .eq(StringUtils.isNotBlank(dataSourceId), TaskVersion::getDataSourceId, dataSourceId)
                                .eq(StringUtils.isNotBlank(databaseId), TaskVersion::getDatabaseId, databaseId)
                                .orderByDesc(TaskVersion::getVersionNum);
                        
                        // 获取最新版本
                        List<TaskVersion> existingVersions = taskVersionService.list(queryWrapper);
                        
                        // 确定新版本号
                        String versionNum = "1.0"; // 默认版本号
                        if (!existingVersions.isEmpty()) {
                            TaskVersion latestVersion = existingVersions.get(0);
                            try {
                                // 解析最新版本号并加0.1
                                double currentVersion = Double.parseDouble(latestVersion.getVersionNum());
                                versionNum = String.format("%.1f", currentVersion + 0.1);
                            } catch (NumberFormatException e) {
                                log.warn("解析版本号失败，使用默认版本号: {}", e.getMessage());
                            }
                        }
                        
                        // 获取变更的详细信息
                        Map<String, Object> modelChangeDetails = (Map<String, Object>) changeDetails.getOrDefault(modelId, new HashMap<>());
                        
                        // 创建新版本对象
                        TaskVersion newVersion = new TaskVersion();
                        newVersion.setId(UuidUtil.generateUuid());
                        newVersion.setTaskId(task.getId());
                        newVersion.setModelId(modelId);
                        newVersion.setDataSourceId(dataSourceId);
                        newVersion.setDatabaseId(databaseId);
                        newVersion.setVersionNum(versionNum);
                        newVersion.setEnvironment(task.getEnvironment());
                        newVersion.setStatus("WAITING"); // 初始状态为等待中
                        newVersion.setPublisher(UserUtils.getCurrentUsername());
                        newVersion.setPublishTime(LocalDateTime.now());
                        newVersion.setDescription("版本升级 - 检测到表结构变化");
                        newVersion.setMode("full"); // 默认为全量
                        newVersion.setIsCurrent(true);
                        newVersion.setIsRollback(false);
                        
                        // 创建配置信息
                        Map<String, Object> config = new HashMap<>();
                        config.put("materializeStrategy", task.getMaterializeStrategy());
                        config.put("refreshMethod", task.getRefreshMethod());
                        
                        // 添加辅助配置
                        if (StringUtils.isNotBlank(task.getExecutionMode())) {
                            config.put("executionMode", task.getExecutionMode());
                        }
                        
                        if (StringUtils.isNotBlank(task.getErrorHandling())) {
                            config.put("errorHandling", task.getErrorHandling());
                        }
                        
                        if (task.getParallelism() != null) {
                            config.put("parallelism", task.getParallelism());
                        }
                        
                        if (task.getTimeout() != null) {
                            config.put("timeout", task.getTimeout());
                        }
                        
                        // 数据源配置
                        if (StringUtils.isNotBlank(task.getDataSourceType())) {
                            config.put("dataSourceType", task.getDataSourceType());
                            
                            if ("theme".equals(task.getDataSourceType()) && StringUtils.isNotBlank(task.getDomainId())) {
                                config.put("domainId", task.getDomainId());
                                config.put("domainName", task.getDomainName());
                            } else if ("custom".equals(task.getDataSourceType()) && StringUtils.isNotBlank(task.getCustomDataSourceId())) {
                                config.put("customDataSourceId", task.getCustomDataSourceId());
                                config.put("customDataSourceName", task.getCustomDataSourceName());
                            }
                        }
                        
                        newVersion.setConfig(config);
                        
                        // 创建变更记录
                        List<Map<String, Object>> changes = new ArrayList<>();
                        
                        // 从modelChangeDetails提取变更信息
                        List<Map<String, Object>> tableChanges = (List<Map<String, Object>>) modelChangeDetails.getOrDefault("changes", new ArrayList<>());
                        for (Map<String, Object> tableChange : tableChanges) {
                            Map<String, Object> change = new HashMap<>();
                            change.put("type", tableChange.getOrDefault("type", "modify"));
                            change.put("path", tableChange.getOrDefault("tableName", "unknown"));
                            change.put("description", tableChange.getOrDefault("description", "结构变更"));
                            change.put("details", tableChange);
                            changes.add(change);
                        }
                        
                        if (changes.isEmpty()) {
                            // 如果没有具体变更，添加一个一般性变更
                            Map<String, Object> change = new HashMap<>();
                            change.put("type", "modify");
                            change.put("path", "structure");
                            change.put("description", "表结构或字段变更");
                            changes.add(change);
                        }
                        
                        newVersion.setChanges(changes);
                        
                        // 创建日志记录
                        List<Map<String, Object>> logs = new ArrayList<>();
                        Map<String, Object> initialLog = new HashMap<>();
                        initialLog.put("timestamp", LocalDateTime.now().toString());
                        initialLog.put("level", "INFO");
                        initialLog.put("message", "检测到表结构变化，创建新版本 " + versionNum);
                        logs.add(initialLog);
                        newVersion.setLogs(logs);
                        
                        // 设置创建和更新时间
                       Date now = new Date();
                        newVersion.setCreateTime(now);
                        newVersion.setUpdateTime(now);
                        
                        // 将旧版本设置为非当前版本(如果存在)
                        if (!existingVersions.isEmpty()) {
                            for (TaskVersion oldVersion : existingVersions) {
                                if (oldVersion.getIsCurrent()) {
                                    oldVersion.setIsCurrent(false);
                                    taskVersionService.updateById(oldVersion);
                                }
                            }
                        }

                        // 为选中的模型创建任务表
                        List<TaskTable> taskTables = createTaskTables(task.getId(), task.getModelIdList(), newVersion.getId());

                        // 保存表字段信息
                        taskTables.forEach(table -> {
                            try {
                                // 修改为使用单个参数的调用
                                saveTableFields(table,newVersion.getId());
                            } catch (Exception e) {
                                log.error("保存表字段信息失败: {}", e.getMessage(), e);
                                throw new RuntimeException("保存表字段信息失败: " + e.getMessage());
                            }
                        });
                        
                        // 保存新版本
                        boolean saved = taskVersionService.save(newVersion);
                        if (saved) {
                            log.info("为模型 {} 创建新版本成功，版本ID: {}, 版本号: {}", modelId, newVersion.getId(), versionNum);
                        } else {
                            log.error("为模型 {} 创建新版本失败", modelId);
                        }
                    } catch (Exception e) {
                        log.error("为模型 {} 创建新版本失败: {}", modelId, e.getMessage(), e);
                        // 单个版本创建失败，继续创建其他版本
                    }
                }
            } else {
                log.info("未检测到任务 {} 的表结构和字段变化，无需版本升级", task.getId());
            }
        } catch (Exception e) {
            log.error("检查和更新版本失败: {}", e.getMessage(), e);
            // 不抛出异常，保证任务执行继续
        }
    }

    /**
     * 判断任务是否可执行
     */
    private boolean isTaskExecutable(String status) {
        return "WAITING".equals(status) || "SCHEDULED".equals(status) || "FAILED".equals(status);
    }

    /**
     * 异步执行任务
     */
    private void asyncExecuteTask(MaterializeTask task) {
        new Thread(() -> {
            try {
                // 更新当前任务关联的版本状态为运行中
                updateTaskVersionsStatus(task.getId(), "RUNNING");
                
                // 执行任务
                executeTaskWithProgress(task);
            } catch (Exception e) {
                log.error("任务 {} 异步执行失败: {}", task.getId(), e.getMessage(), e);
                
                // 记录错误并更新任务状态
                recordTaskError(task.getId(), e);
                
                // 更新版本状态为失败
                updateTaskVersionsStatus(task.getId(), "FAILED");
            }
        }).start();
    }

    /**
     * 执行带进度的任务
     */
    private void executeTaskWithProgress(MaterializeTask task) {
        String taskId = task.getId();
        String versionId = getCurrentVersionId(taskId);
        try {
            log.info("开始执行任务 {}", taskId);
            // 记录任务开始执行日志
            taskLogService.logTask(taskId, versionId, "INFO", 
                    "开始执行任务: " + task.getTaskName(), 
                    "EXECUTE", UserUtils.getCurrentUsername(), 
                    "任务ID: " + taskId + ", 物化策略: " + task.getMaterializeStrategy());

            // 获取表配置
            List<TaskTable> tables = getTaskTables(taskId, versionId);
            if (tables.isEmpty()) {
                log.warn("任务 {} 没有配置任何表", taskId);
                updateTaskStatus(taskId, "FAILED", null);
                recordTaskError(taskId, new RuntimeException("任务没有配置任何表"));
                
                // 记录无表配置错误日志
                taskLogService.logTask(taskId, versionId, "ERROR", 
                        "任务执行失败: 没有配置任何表", 
                        "EXECUTE_ERROR", UserUtils.getCurrentUsername(), 
                        "任务没有配置任何表");
                return;
            }

            // 记录表配置信息日志
            taskLogService.logTask(taskId, versionId, "INFO", 
                    "获取到 " + tables.size() + " 个表配置", 
                    "EXECUTE", UserUtils.getCurrentUsername(), 
                    "将根据配置执行物化");

            // 确定执行顺序
            List<TaskTable> orderedTables = determineExecutionOrder(tables, task.getExecutionMode());
            
            // 记录表执行顺序日志
            taskLogService.logTask(taskId, versionId, "INFO", 
                    "确定表执行顺序", 
                    "EXECUTE", UserUtils.getCurrentUsername(), 
                    "执行模式: " + task.getExecutionMode());

            // 添加安全检查
            int totalTables = (int) tables.size();
            int completedTables = 0;

            // 记录任务开始
            log.info("任务 {} 将物化 {} 个表", taskId, totalTables);
            taskLogService.logTask(taskId, versionId, "INFO", 
                    "任务将物化 " + totalTables + " 个表", 
                    "EXECUTE", UserUtils.getCurrentUsername(), 
                    "开始执行物化过程");

            // 创建任务版本（如果还没有）
            ensureTaskVersions(task);

            // 根据执行模式处理表
            if ("parallel".equals(task.getExecutionMode())) {
                // 并行执行
                taskLogService.logTask(taskId, versionId, "INFO", 
                        "使用并行模式执行物化", 
                        "EXECUTE", UserUtils.getCurrentUsername(), 
                        "并行度: " + task.getParallelism());
                executeTablesInParallel(task, orderedTables);
            } else {
                // 串行执行
                taskLogService.logTask(taskId, versionId, "INFO", 
                        "使用串行模式执行物化", 
                        "EXECUTE", UserUtils.getCurrentUsername(), 
                        "将按顺序依次执行");
                for (TaskTable table : orderedTables) {
                    try {
                        // 记录开始执行单个表物化日志
                        taskLogService.logTaskTable(taskId, versionId, "INFO", 
                                "开始执行表物化: " + table.getTableName(), 
                                "EXECUTE_TABLE", UserUtils.getCurrentUsername(), 
                                table.getId(), table.getTableName(), 
                                "物化类型: " + table.getMaterializeType());
                                
                        // 执行单个表的物化
                        executeTable(task, table);

                        // 更新进度
                        completedTables++;
                        int progress = (int) (((double) completedTables / totalTables) * 100);
                        updateTaskStatus(taskId, "RUNNING", progress);
                        
                        // 记录表执行完成日志
                        taskLogService.logTaskTable(taskId, versionId, "INFO", 
                                "表物化完成: " + table.getTableName(), 
                                "EXECUTE_TABLE_COMPLETE", UserUtils.getCurrentUsername(), 
                                table.getId(), table.getTableName(), 
                                "进度: " + progress + "%");

                    } catch (Exception e) {
                        log.error("执行表 {} 物化失败: {}", table.getTableName(), e.getMessage(), e);
                        
                        // 记录表执行失败日志
                        taskLogService.logTaskTable(taskId, versionId, "ERROR", 
                                "表物化失败: " + table.getTableName(), 
                                "EXECUTE_TABLE_ERROR", UserUtils.getCurrentUsername(), 
                                table.getId(), table.getTableName(), 
                                "错误: " + e.getMessage());

                        // 根据错误处理策略决定是否继续
                        if ("stop".equals(task.getErrorHandling())) {
                            // 记录停止执行日志
                            taskLogService.logTask(taskId, versionId, "WARN", 
                                    "因表 " + table.getTableName() + " 执行失败而停止任务", 
                                    "EXECUTE_STOP", UserUtils.getCurrentUsername(), 
                                    "错误处理策略: stop");
                            throw e; // 失败即停止
                        }
                        // continue和log策略都继续执行，区别在于错误日志的详细程度
                        taskLogService.logTask(taskId, versionId, "WARN", 
                                "表 " + table.getTableName() + " 执行失败，但继续执行其他表", 
                                "EXECUTE_CONTINUE", UserUtils.getCurrentUsername(), 
                                "错误处理策略: " + task.getErrorHandling());
                    }
                }
            }

            // 检查任务是否被取消
            task = getById(taskId);
            if ("CANCELLED".equals(task.getStatus())) {
                log.info("任务 {} 已被取消", taskId);
                taskLogService.logTask(taskId, versionId, "WARN", 
                        "任务已被取消", 
                        "EXECUTE_CANCELLED", UserUtils.getCurrentUsername(), 
                        "任务ID: " + taskId);
                return;
            }

            // 检查是否有表执行失败
            boolean allSucceeded = true;
            for (TaskTable table : tables) {
                if ("FAILED".equals(table.getStatus())) {
                    allSucceeded = false;
                    break;
                }
            }

            // 所有表执行完成，更新任务状态
            String finalStatus = allSucceeded ? "COMPLETED" : "PARTIAL";
            updateTaskStatus(taskId, finalStatus, 100);
            
            // 物化任务执行完成后，更新任务状态
            task.setStatus(finalStatus);
            task.setProgress(100);
            task.setEndTime(LocalDateTime.now());
            task.setUpdateTime(new Date());
            updateById(task);

            // 更新版本状态
            updateTaskVersionsStatus(taskId, allSucceeded ? "SUCCESS" : "PARTIAL");

            log.info("任务 {} 执行 {}", taskId, allSucceeded ? "成功" : "部分成功");
            
            // 记录任务完成日志
            taskLogService.logTask(taskId, versionId, "INFO", 
                    "任务执行" + (allSucceeded ? "成功" : "部分成功"), 
                    "EXECUTE_COMPLETE", UserUtils.getCurrentUsername(), 
                    "执行状态: " + finalStatus + ", 进度: 100%");
            
        } catch (Exception e) {
            log.error("执行任务失败: {}", e.getMessage(), e);

            // 更新任务状态为失败
            updateTaskStatus(task.getId(), "FAILED", null);
            
            // 记录错误
            recordTaskError(task.getId(), e);

            // 更新版本状态为失败
            updateTaskVersionsStatus(task.getId(), "FAILED");
            
            // 记录任务失败日志
            taskLogService.logTask(taskId, versionId, "ERROR", 
                    "任务执行失败: " + e.getMessage(), 
                    "EXECUTE_FAILED", UserUtils.getCurrentUsername(), 
                    "异常详情: " + e.toString());
        }
    }

    /**
     * 获取任务的表配置
     */
    private List<TaskTable> getTaskTables(String taskId,String versionId) {
        LambdaQueryWrapper<TaskTable> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TaskTable::getTaskId, taskId).eq(TaskTable::getVersionId, versionId);
        return taskTableService.list(queryWrapper);
    }

    /**
     * 确定表的执行顺序
     */
    private List<TaskTable> determineExecutionOrder(List<TaskTable> tables, String executionMode) {
        if ("dependency".equals(executionMode)) {
            // 基于依赖关系排序
            return sortTablesByDependency(tables);
        } else {
            // 默认按主表优先排序
            return sortTablesByMainFirst(tables);
        }
    }

    /**
     * 基于依赖关系排序表
     */
    private List<TaskTable> sortTablesByDependency(List<TaskTable> tables) {
        // 创建表ID到表的映射
        Map<String, TaskTable> tableMap = tables.stream()
                .collect(Collectors.toMap(TaskTable::getTableId, table -> table));

        // 创建依赖图
        Map<String, Set<String>> dependencyGraph = new HashMap<>();
        for (TaskTable table : tables) {
            String tableId = table.getTableId();
            dependencyGraph.put(tableId, new HashSet<>());

            // 使用辅助方法安全地获取依赖关系列表
            String dependenciesStr = table.getDependencies();
            List<String> dependencies = getDependenciesList(dependenciesStr);

            for (String dependencyId : dependencies) {
                if (StringUtils.isNotBlank(dependencyId) && tableMap.containsKey(dependencyId)) {
                    dependencyGraph.get(tableId).add(dependencyId);
                }
            }
        }

        // 拓扑排序
        List<TaskTable> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> temp = new HashSet<>();

        for (TaskTable table : tables) {
            if (!visited.contains(table.getTableId())) {
                topologicalSort(table.getTableId(), dependencyGraph, visited, temp, result, tableMap);
            }
        }

        // 返回排序后的表列表
        return result;
    }

    /**
     * 拓扑排序辅助方法
     */
    private void topologicalSort(String tableId, Map<String, Set<String>> graph, Set<String> visited,
                                 Set<String> temp, List<TaskTable> result, Map<String, TaskTable> tableMap) {
        // 检测循环依赖
        if (temp.contains(tableId)) {
            log.warn("检测到循环依赖: {}", tableId);
            return;
        }

        if (!visited.contains(tableId)) {
            temp.add(tableId);

            // 处理依赖
            Set<String> dependencies = graph.get(tableId);
            if (dependencies != null) {
                for (String dependencyId : dependencies) {
                    topologicalSort(dependencyId, graph, visited, temp, result, tableMap);
                }
            }

            // 标记为已访问，添加到结果
            visited.add(tableId);
            temp.remove(tableId);
            result.add(tableMap.get(tableId));
        }
    }

    /**
     * 按主表优先排序表
     */
    private List<TaskTable> sortTablesByMainFirst(List<TaskTable> tables) {
        // 将主表放在前面
        return tables.stream()
                .sorted((t1, t2) -> {
                    if (Boolean.TRUE.equals(t1.getIsMain()) && !Boolean.TRUE.equals(t2.getIsMain())) {
                        return -1;
                    } else if (!Boolean.TRUE.equals(t1.getIsMain()) && Boolean.TRUE.equals(t2.getIsMain())) {
                        return 1;
                    } else {
                        return 0;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 并行执行表物化
     */
    private void executeTablesInParallel(MaterializeTask task, List<TaskTable> tables) {
        String taskId = task.getId();
        String versionId = getCurrentVersionId(taskId);
        
        // 创建计数器和线程安全的计数器
        int totalTables = (int) tables.size();
        AtomicInteger completedTables = new AtomicInteger(0);

        // 记录并行执行开始日志
        taskLogService.logTask(taskId, versionId, "INFO", 
                "开始并行执行 " + totalTables + " 个表的物化", 
                "PARALLEL_EXECUTE", UserUtils.getCurrentUsername(), 
                "并行度: " + task.getParallelism());

        // 创建任务完成后的回调
        CountDownLatch latch = new CountDownLatch(totalTables);

        // 并行度设置，默认为处理器核心数
        int parallelism = task.getParallelism() != null ? task.getParallelism() : Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(parallelism, totalTables));

        // 记录失败的表
        List<String> failedTables = Collections.synchronizedList(new ArrayList<>());

        // 提交所有表任务
        for (TaskTable table : tables) {
            executor.submit(() -> {
                try {
                    // 记录开始执行单个表物化日志
                    taskLogService.logTaskTable(taskId, versionId, "INFO", 
                            "开始执行表物化: " + table.getTableName(), 
                            "EXECUTE_TABLE", UserUtils.getCurrentUsername(), 
                            table.getId(), table.getTableName(), 
                            "并行执行, 物化类型: " + table.getMaterializeType());
                            
                    // 执行单个表的物化
                    executeTable(task, table);

                    // 更新进度
                    int completed = completedTables.incrementAndGet();
                    int progress = (int) (((double) completed / totalTables) * 100);
                    updateTaskStatus(task.getId(), "RUNNING", progress);
                    
                    // 记录表执行完成日志
                    taskLogService.logTaskTable(taskId, versionId, "INFO", 
                            "表物化完成: " + table.getTableName(), 
                            "EXECUTE_TABLE_COMPLETE", UserUtils.getCurrentUsername(), 
                            table.getId(), table.getTableName(), 
                            "进度: " + progress + "%");

                } catch (Exception e) {
                    log.error("执行表 {} 物化失败: {}", table.getTableName(), e.getMessage(), e);
                    failedTables.add(table.getTableName());
                    
                    // 记录表执行失败日志
                    taskLogService.logTaskTable(taskId, versionId, "ERROR", 
                            "表物化失败: " + table.getTableName(), 
                            "EXECUTE_TABLE_ERROR", UserUtils.getCurrentUsername(), 
                            table.getId(), table.getTableName(), 
                            "错误: " + e.getMessage());

                    // 如果策略是失败即停止，通知其他任务取消
                    if ("stop".equals(task.getErrorHandling())) {
                        // 更新任务状态为失败
                        updateTaskStatus(task.getId(), "FAILED", null);
                        
                        // 记录停止执行日志
                        taskLogService.logTask(taskId, versionId, "WARN", 
                                "因表 " + table.getTableName() + " 执行失败而停止任务", 
                                "EXECUTE_STOP", UserUtils.getCurrentUsername(), 
                                "错误处理策略: stop");
                                
                        // 关闭执行器
                        executor.shutdownNow();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            // 等待所有任务完成或超时
            int timeout = task.getTimeout() != null ? task.getTimeout() : 30;
            boolean completed = latch.await(timeout, TimeUnit.MINUTES);

            // 关闭执行器
            executor.shutdown();

            // 如果未完成（超时）
            if (!completed) {
                log.warn("任务 {} 执行超时", task.getId());
                updateTaskStatus(task.getId(), "FAILED", null);
                
                // 记录执行超时日志
                taskLogService.logTask(taskId, versionId, "ERROR", 
                        "任务执行超时", 
                        "EXECUTE_TIMEOUT", UserUtils.getCurrentUsername(), 
                        "超时设置: " + timeout + " 分钟");
                        
                throw new RuntimeException("任务执行超时");
            }

            // 如果有失败的表且错误处理策略为停止，则标记任务为失败
            if (!failedTables.isEmpty() && "stop".equals(task.getErrorHandling())) {
                log.warn("任务 {} 存在失败的表: {}", task.getId(), String.join(", ", failedTables));
                updateTaskStatus(task.getId(), "FAILED", null);
                
                // 记录执行失败日志
                taskLogService.logTask(taskId, versionId, "ERROR", 
                        "任务执行失败", 
                        "EXECUTE_FAILED", UserUtils.getCurrentUsername(), 
                        "失败的表: " + String.join(", ", failedTables));
                        
                throw new RuntimeException("任务执行失败，失败的表: " + String.join(", ", failedTables));
            } else if (!failedTables.isEmpty()) {
                // 如果有失败的表但策略不是停止，则标记为部分完成
                log.warn("任务 {} 部分完成，存在失败的表: {}", task.getId(), String.join(", ", failedTables));
                updateTaskStatus(task.getId(), "PARTIAL", 100);
                
                // 记录部分完成日志
                taskLogService.logTask(taskId, versionId, "WARN", 
                        "任务部分完成", 
                        "EXECUTE_PARTIAL", UserUtils.getCurrentUsername(), 
                        "失败的表: " + String.join(", ", failedTables));
            } else {
                // 所有表都成功，标记为完成
                updateTaskStatus(task.getId(), "COMPLETED", 100);
                
                // 记录执行成功日志
                taskLogService.logTask(taskId, versionId, "INFO", 
                        "任务执行成功", 
                        "EXECUTE_COMPLETE", UserUtils.getCurrentUsername(), 
                        "所有表物化完成");
            }

        } catch (InterruptedException e) {
            log.error("任务 {} 执行被中断", task.getId());
            Thread.currentThread().interrupt();
            updateTaskStatus(task.getId(), "FAILED", null);
            
            // 记录执行中断日志
            taskLogService.logTask(taskId, versionId, "ERROR", 
                    "任务执行被中断", 
                    "EXECUTE_INTERRUPTED", UserUtils.getCurrentUsername(), 
                    e.getMessage());
                    
            throw new RuntimeException("任务执行被中断", e);
        }
    }

    /**
     * 执行单个表的物化
     */
    private void executeTable(MaterializeTask task, TaskTable table) throws Exception {
        String taskId = task.getId();
        String versionId = getCurrentVersionId(taskId);
        
        log.info("开始执行表 {} 物化", table.getTableName());

        try {
            // 检查任务是否被取消
            MaterializeTask currentTask = getById(task.getId());
            if ("CANCELLED".equals(currentTask.getStatus())) {
                log.info("任务 {} 已被取消，跳过表 {} 物化", task.getId(), table.getTableName());
                
                // 记录任务取消日志
                taskLogService.logTaskTable(taskId, versionId, "WARN", 
                        "跳过表物化: " + table.getTableName(), 
                        "EXECUTE_TABLE_SKIP", UserUtils.getCurrentUsername(), 
                        table.getId(), table.getTableName(), 
                        "原因: 任务已被取消");
                        
                return;
            }

            // 根据物化类型执行不同的物化逻辑
            if ("incremental".equals(table.getMaterializeType()) && StringUtils.isNotBlank(table.getIncrementalField())) {
                // 增量物化
                taskLogService.logTaskTable(taskId, versionId, "INFO", 
                        "执行增量物化: " + table.getTableName(), 
                        "EXECUTE_INCREMENTAL", UserUtils.getCurrentUsername(), 
                        table.getId(), table.getTableName(), 
                        "增量字段: " + table.getIncrementalField());
                        
                executeIncrementalMaterialization(task, table);
            } else {
                // 全量物化
                taskLogService.logTaskTable(taskId, versionId, "INFO", 
                        "执行全量物化: " + table.getTableName(), 
                        "EXECUTE_FULL", UserUtils.getCurrentUsername(), 
                        table.getId(), table.getTableName(), 
                        "刷新方式: " + task.getRefreshMethod());
                        
                executeFullMaterialization(task, table);
            }

            log.info("表 {} 物化完成", table.getTableName());
            
            // 记录表物化完成日志
            taskLogService.logTaskTable(taskId, versionId, "INFO", 
                    "表物化完成: " + table.getTableName(), 
                    "EXECUTE_TABLE_SUCCESS", UserUtils.getCurrentUsername(), 
                    table.getId(), table.getTableName(), 
                    "物化成功");
                    
        } catch (Exception e) {
            log.error("表 {} 物化失败: {}", table.getTableName(), e.getMessage(), e);

            // 记录表物化失败日志
            taskLogService.logTaskTable(taskId, versionId, "ERROR", 
                    "表物化失败: " + table.getTableName(), 
                    "EXECUTE_TABLE_FAIL", UserUtils.getCurrentUsername(), 
                    table.getId(), table.getTableName(), 
                    "错误: " + e.getMessage());

            // 记录表执行错误
            recordTableError(task.getId(), table.getTableId(), e);

            throw e; // 向上传播异常，让调用者处理
        }
    }

    /**
     * 执行全量物化
     */
    private void executeFullMaterialization(MaterializeTask task, TaskTable table) throws Exception {
        String taskId = task.getId();
        String versionId = getCurrentVersionId(taskId);
        
        log.info("执行表 {} 全量物化", table.getTableName());
        
        // 记录开始全量物化日志
        taskLogService.logTaskTable(taskId, versionId, "INFO", 
                "开始全量物化: " + table.getTableName(), 
                "FULL_MATERIALIZE_START", UserUtils.getCurrentUsername(), 
                table.getId(), table.getTableName(), 
                "目标表: " + table.getTableName());

        // 获取数据源对应的AbstractDatabaseHandler
        AbstractDatabaseHandler dbHandler = getDbHandler(task);
        if (dbHandler == null) {
            throw new RuntimeException("无法获取数据库处理器");
        }

        // 获取数据源连接
        Connection connection = dbHandler.getConnection();
        if (connection == null) {
            throw new RuntimeException("无法获取数据源连接");
        }

        try {
            // 根据刷新方式执行不同操作
            if ("rebuild".equals(task.getRefreshMethod())) {
                // 重建表 - 先删除后创建
                String dropTableSql = dbHandler.getDropTableSql(table.getTableName());
                try (Statement stmt = connection.createStatement()) {
                    log.info("执行删除表SQL: {}", dropTableSql);

                    // 首先检查表是否存在
                    String tableExistsSql = dbHandler.getTableExistsSql(table.getTableName());
                    boolean tableExists = false;
                    try (ResultSet rs = stmt.executeQuery(tableExistsSql)) {
                        tableExists = rs.next() && rs.getInt(1) > 0;
                    }

                    if (tableExists) {
                        stmt.execute(dropTableSql);
                        log.info("表 {} 已删除", table.getTableName());
                    } else {
                        log.info("表 {} 不存在，跳过删除操作", table.getTableName());
                    }
                } catch (SQLException e) {
                    log.warn("尝试删除表 {} 时出现异常: {}，继续创建表", table.getTableName(), e.getMessage());
                    // 忽略表不存在的错误，继续执行创建表操作
                }
                createAndPopulateTable(task, table, connection, dbHandler);
            } else if ("update".equals(task.getRefreshMethod())) {
                // 更新表 - 表存在则更新数据，不存在则创建
                String tableExistsSql = dbHandler.getTableExistsSql(table.getTableName());
                boolean tableExists = false;
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery(tableExistsSql)) {
                    tableExists = rs.next() && rs.getInt(1) > 0;
                }

                if (!tableExists) {
                    createAndPopulateTable(task, table, connection, dbHandler);
                } else {
                    updateTableData(task, table, connection, dbHandler);
                }
            } else {
                // 默认重建
                String dropTableSql = dbHandler.getDropTableSql(table.getTableName());
                try (Statement stmt = connection.createStatement()) {
                    // 首先检查表是否存在
                    String tableExistsSql = dbHandler.getTableExistsSql(table.getTableName());
                    boolean tableExists = false;
                    try (ResultSet rs = stmt.executeQuery(tableExistsSql)) {
                        tableExists = rs.next() && rs.getInt(1) > 0;
                    }

                    if (tableExists) {
                        stmt.execute(dropTableSql);
                        log.info("表 {} 已删除", table.getTableName());
                    } else {
                        log.info("表 {} 不存在，跳过删除操作", table.getTableName());
                    }
                } catch (SQLException e) {
                    // 忽略表不存在的错误
                    log.warn("尝试删除表 {} 时出现异常: {}，继续创建表", table.getTableName(), e.getMessage());
                }
                createAndPopulateTable(task, table, connection, dbHandler);
            }
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                log.error("关闭数据库连接失败: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 执行增量物化
     */
    private void executeIncrementalMaterialization(MaterializeTask task, TaskTable table) throws Exception {
        String taskId = task.getId();
        String versionId = getCurrentVersionId(taskId);
        
        log.info("执行表 {} 增量物化，增量字段: {}", table.getTableName(), table.getIncrementalField());
        
        // 记录开始增量物化日志
        taskLogService.logTaskTable(taskId, versionId, "INFO", 
                "开始增量物化: " + table.getTableName(), 
                "INCREMENTAL_MATERIALIZE_START", UserUtils.getCurrentUsername(), 
                table.getId(), table.getTableName(), 
                "增量字段: " + table.getIncrementalField() + ", 目标表: " + table.getTableName());

        // 获取数据源对应的AbstractDatabaseHandler
        AbstractDatabaseHandler dbHandler = getDbHandler(task);
        if (dbHandler == null) {
            String errorMsg = "无法获取数据库处理器";
            taskLogService.logTaskTable(taskId, versionId, "ERROR", 
                    "增量物化失败: " + table.getTableName(), 
                    "INCREMENTAL_MATERIALIZE_ERROR", UserUtils.getCurrentUsername(), 
                    table.getId(), table.getTableName(), 
                    errorMsg);
            throw new RuntimeException(errorMsg);
        }

        // 获取数据源连接
        Connection connection = dbHandler.getConnection();
        if (connection == null) {
            String errorMsg = "无法获取数据源连接";
            taskLogService.logTaskTable(taskId, versionId, "ERROR", 
                    "增量物化失败: " + table.getTableName(), 
                    "INCREMENTAL_MATERIALIZE_ERROR", UserUtils.getCurrentUsername(), 
                    table.getId(), table.getTableName(), 
                    errorMsg);
            throw new RuntimeException(errorMsg);
        }

        try {
            // 首先检查表是否存在
            String tableExistsSql = dbHandler.getTableExistsSql(table.getTableName());
            boolean tableExists = false;
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(tableExistsSql)) {
                tableExists = rs.next() && rs.getInt(1) > 0;
            }

            if (!tableExists) {
                // 如果表不存在，则创建表并执行完整的数据填充
                taskLogService.logTaskTable(taskId, versionId, "INFO", 
                        "目标表不存在，创建新表: " + table.getTableName(), 
                        "INCREMENTAL_CREATE_TABLE", UserUtils.getCurrentUsername(), 
                        table.getId(), table.getTableName(), 
                        "执行完整的表创建和数据填充");
                createAndPopulateTable(task, table, connection, dbHandler);
            } else {
                // 表存在，获取增量字段的最新值
                Object lastUpdateValue = getLastUpdateValue(connection, table.getTableName(), table.getIncrementalField());
                
                taskLogService.logTaskTable(taskId, versionId, "INFO", 
                        "获取增量字段最新值: " + table.getTableName(), 
                        "INCREMENTAL_GET_VALUE", UserUtils.getCurrentUsername(), 
                        table.getId(), table.getTableName(), 
                        "字段: " + table.getIncrementalField() + ", 最新值: " + (lastUpdateValue != null ? lastUpdateValue.toString() : "null"));

                if (lastUpdateValue == null) {
                    // 若无法获取增量字段的值，则执行全量更新
                    taskLogService.logTaskTable(taskId, versionId, "WARN", 
                            "无法获取增量字段值，执行全量更新: " + table.getTableName(), 
                            "INCREMENTAL_FALLBACK", UserUtils.getCurrentUsername(), 
                            table.getId(), table.getTableName(), 
                            "回退至全量物化");
                    updateTableData(task, table, connection, dbHandler);
                } else {
                    // 根据增量更新方式执行不同的操作
                    String updateMode = StringUtils.isNotBlank(table.getWriteMode())
                            ? table.getWriteMode() : "append";
                    
                    taskLogService.logTaskTable(taskId, versionId, "INFO", 
                            "执行增量更新: " + table.getTableName(), 
                            "INCREMENTAL_UPDATE", UserUtils.getCurrentUsername(), 
                            table.getId(), table.getTableName(), 
                            "更新模式: " + updateMode);

                    if ("append".equals(updateMode)) {
                        // 追加模式：只追加新数据，不修改已有数据
                        appendIncrementalData(task, table, connection, table.getIncrementalField(), lastUpdateValue);
                    } else if ("overwrite".equals(updateMode)) {
                        // 覆盖模式：删除重叠部分，再追加新数据
                        overwriteIncrementalData(task, table, connection, table.getIncrementalField(), lastUpdateValue);
                    } else {
                        // 默认追加
                        appendIncrementalData(task, table, connection, table.getIncrementalField(), lastUpdateValue);
                    }
                }
            }
            
            // 记录增量物化完成日志
            taskLogService.logTaskTable(taskId, versionId, "INFO", 
                    "增量物化完成: " + table.getTableName(), 
                    "INCREMENTAL_MATERIALIZE_COMPLETE", UserUtils.getCurrentUsername(), 
                    table.getId(), table.getTableName(), 
                    "物化成功");
                    
        } catch (Exception e) {
            String errorMsg = "增量物化失败: " + e.getMessage();
            log.error(errorMsg, e);
            
            // 记录增量物化失败日志
            taskLogService.logTaskTable(taskId, versionId, "ERROR", 
                    "增量物化失败: " + table.getTableName(), 
                    "INCREMENTAL_MATERIALIZE_ERROR", UserUtils.getCurrentUsername(), 
                    table.getId(), table.getTableName(), 
                    "错误: " + e.getMessage());
                    
            throw e;
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                log.error("关闭数据库连接失败: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 获取数据库处理器
     */
    private AbstractDatabaseHandler getDbHandler(MaterializeTask task) throws Exception {
        String taskId = task.getId();
        String versionId = getCurrentVersionId(taskId);

        // 根据数据源类型获取正确的数据源ID
        String dataSourceId;

        if ("theme".equals(task.getDataSourceType())) {
            // 主题域数据源 - 先查询主题域对象，再获取数据源ID
            String domainId = task.getDomainId();
            if (StringUtils.isBlank(domainId)) {
                throw new RuntimeException("主题域ID不能为空");
            }

            // 通过domainService获取Domain对象
            Domain domain = domainService.getById(domainId);
            if (domain == null) {
                throw new RuntimeException("主题域不存在：" + domainId);
            }

            // 从Domain对象获取数据源ID
            dataSourceId = domain.getDataSourceId();
            if (StringUtils.isBlank(dataSourceId)) {
                throw new RuntimeException("主题域[" + domain.getName() + "]未配置数据源");
            }
        } else if ("custom".equals(task.getDataSourceType())) {
            // 自定义数据源 - 直接获取自定义数据源ID
            dataSourceId = task.getCustomDataSourceId();
            if (StringUtils.isBlank(dataSourceId)) {
                throw new RuntimeException("自定义数据源ID不能为空");
            }
        } else {
            taskLogService.logTask(taskId, versionId, "ERROR",
                    "获取数据库处理器失败",
                    "GET_DB_HANDLER_ERROR", UserUtils.getCurrentUsername(),
                    "不支持的数据源类型: " + task.getDataSourceType());
            throw new RuntimeException("不支持的数据源类型: " + task.getDataSourceType());
        }

        // 获取数据源对象
        DataSource dataSource = dataSourceService.getById(dataSourceId);
        if (dataSource == null) {
            throw new RuntimeException("数据源不存在：" + dataSourceId);
        }

        // 调用工厂方法获取对应的数据库处理器
        AbstractDatabaseHandler handler = DatabaseHandlerFactory.getDatabaseHandler(dataSource);

        if (handler == null) {
            taskLogService.logTask(taskId, versionId, "ERROR",
                    "获取数据库处理器失败",
                    "GET_DB_HANDLER_ERROR", UserUtils.getCurrentUsername(),
                    "无法创建适用于数据源类型 " + dataSource.getType() + " 的数据库处理器");
            throw new RuntimeException("无法创建适用于数据源类型 " + dataSource.getType() + " 的数据库处理器");
        }

        taskLogService.logTask(taskId, versionId, "INFO",
                "获取数据库处理器",
                "GET_DB_HANDLER", UserUtils.getCurrentUsername(),
                "数据库类型: " + dataSource.getType());
        return handler;
    }

    /**
     * 创建并填充表
     */
    private void createAndPopulateTable(MaterializeTask task, TaskTable table, Connection connection, AbstractDatabaseHandler dbHandler) throws Exception {
        String taskId = task.getId();
        String versionId = getCurrentVersionId(taskId);

        // 记录开始创建表日志
        taskLogService.logTaskTable(taskId, versionId, "INFO",
                "开始创建并填充表: " + table.getTableName(),
                "CREATE_POPULATE_TABLE", UserUtils.getCurrentUsername(),
                table.getId(), table.getTableName(),
                "目标表: " + table.getTableName());



        // 1. 获取表结构信息
        TableStructure tableStructure = getTableStructure(task, table);
        if (tableStructure == null) {
            String errorMsg = "无法获取表结构或字段为空";
            taskLogService.logTaskTable(taskId, versionId, "ERROR",
                    "创建表失败: " + table.getTableName(),
                    "CREATE_TABLE_ERROR", UserUtils.getCurrentUsername(),
                    table.getId(), table.getTableName(),
                    errorMsg);
            throw new RuntimeException(errorMsg);
        }


        // 获取数据库类型
        String dbType = dbHandler.getDatabaseType();
        log.info("使用数据库类型 {} 创建表 {}", dbType, table.getTableName());

        // 2. 准备列定义
        List<ColumnDefinition> columns = new ArrayList<>();
        for (FieldStructure field : tableStructure.getFields()) {
            ColumnDefinition column = new ColumnDefinition();
            column.setName(field.getName());
            column.setType(field.getType());
            column.setLength(field.getLength());
            column.setPrecision(field.getPrecision());
            column.setScale(field.getScale());
            column.setNullable(!field.isNotNull());
            column.setPrimaryKey(field.isPrimary());
            column.setAutoIncrement(field.isAutoIncrement());
            column.setDefaultValue(field.getDefaultValue());
            column.setComment(field.getComment());
            columns.add(column);
        }

        // 3. 生成创建表SQL
        String createTableSql = dbHandler.getCreateTableSql(
                table.getTableName(),
                columns,
                tableStructure.getDisplayName(), // 表注释
                "", // 默认引擎
                "", // 默认字符集
                "" // 默认排序规则
        );

        // 4. 执行建表SQL
        try (Statement stmt = connection.createStatement()) {
            taskLogService.logTaskTable(taskId, versionId, "INFO",
                    "生成创建表SQL: " + createTableSql,
                    "GENERATE_CREATE_SQL", UserUtils.getCurrentUsername(),
                    table.getId(), table.getTableName(),
                    "SQL长度: " + createTableSql.length() + " 字符");
            log.info("执行建表SQL: {}", createTableSql);
            stmt.execute(createTableSql);
            log.info("表 {} 创建成功", table.getTableName());
        }

        // 5. 执行注释SQL - 根据数据库类型可能需要单独执行
        if (StringUtils.isNotBlank(tableStructure.getDisplayName())) {
            try {
                String tableCommentSql = dbHandler.getAddTableCommentSql(table.getTableName(), tableStructure.getDisplayName());
                if (StringUtils.isNotBlank(tableCommentSql)) {
                    try (Statement stmt = connection.createStatement()) {
                        log.info("执行表注释SQL: {}", tableCommentSql);
                        taskLogService.logTaskTable(taskId, versionId, "INFO",
                                "执行表注释SQL: " + tableCommentSql,
                                "GENERATE_CREATE_SQL", UserUtils.getCurrentUsername(),
                                table.getId(), table.getTableName(),
                                "SQL长度: " + tableCommentSql.length() + " 字符");
                        stmt.execute(tableCommentSql);
                    }
                }
            } catch (Exception e) {
                log.warn("添加表注释失败: {}", e.getMessage());
                taskLogService.logTaskTable(taskId, versionId, "ERROR",
                        "添加表注释失败: " + table.getTableName(),
                        "GENERATE_CREATE_SQL", UserUtils.getCurrentUsername(),
                        table.getId(), table.getTableName(),
                        "添加表注释失败 "+e.getMessage()+"'");
                // 注释失败不影响主流程
            }
        }

        // 6. 执行字段注释SQL - 如果需要单独处理
        for (FieldStructure field : tableStructure.getFields()) {
            if (StringUtils.isNotBlank(field.getComment())) {
                try {
                    String columnCommentSql = dbHandler.getAddColumnCommentSql(table.getTableName(), field.getName(), field.getComment());
                    if (StringUtils.isNotBlank(columnCommentSql)) {
                        try (Statement stmt = connection.createStatement()) {
                            log.info("执行字段注释SQL: {}", columnCommentSql);
                            stmt.execute(columnCommentSql);
                            taskLogService.logTaskTable(taskId, versionId, "INFO",
                                    "执行字段注释SQL: " + columnCommentSql,
                                    "GENERATE_CREATE_SQL", UserUtils.getCurrentUsername(),
                                    table.getId(), table.getTableName(),
                                    "SQL长度: " + columnCommentSql.length() + " 字符");
                        }
                    }
                } catch (Exception e) {
                    log.warn("添加字段 {} 注释失败: {}", field.getName(), e.getMessage());
                    taskLogService.logTaskTable(taskId, versionId, "ERROR",
                            "执行字段注释SQL: " + field.getName(),
                            "GENERATE_CREATE_SQL", UserUtils.getCurrentUsername(),
                            table.getId(), table.getTableName(),
                            "添加字段 "+field.getName()+" 注释失败: "+e.getMessage()+"'");
                    // 注释失败不影响主流程
                }
            }
        }

        // 7. 加载数据
        loadTableData(task, table, connection, tableStructure);

        log.info("表 {} 创建并加载数据完成", table.getTableName());
    }

    /**
     * 更新表数据
     */
    private void updateTableData(MaterializeTask task, TaskTable table, Connection connection, AbstractDatabaseHandler dbHandler) throws Exception {
        String taskId = task.getId();
        String versionId = getCurrentVersionId(taskId);
        
        // 获取表结构
        TableStructure tableStructure = getTableStructure(task, table);
        if (tableStructure == null) {
            taskLogService.logTaskTable(taskId, versionId, "ERROR",
                    "更新表数据失败: " + table.getTableName(),
                    "UPDATE_TABLE_ERROR", UserUtils.getCurrentUsername(),
                    table.getId(), table.getTableName(),
                    "错误: 无法获取表结构信息");
            throw new RuntimeException("无法获取表结构信息");
        }

        // 清空表数据
        String truncateSql = dbHandler.getTruncateTableSql(table.getTableName());
        try (Statement stmt = connection.createStatement()) {
            log.info("执行清空表SQL: {}", truncateSql);
            stmt.execute(truncateSql);
            log.info("表 {} 数据已清空", table.getTableName());

            taskLogService.logTaskTable(taskId, versionId, "INFO",
                    "执行清空表SQL: " + truncateSql,
                    "UPDATE_TABLE_COMPLETE", UserUtils.getCurrentUsername(),
                    table.getId(), table.getTableName(),
                    "更新成功");
        }

        // 重新加载数据
        loadTableData(task, table, connection, tableStructure);

        log.info("表 {} 数据更新完成", table.getTableName());
    }

    /**
     * 追加增量数据
     */
    private void appendIncrementalData(MaterializeTask task, TaskTable table, Connection connection,
                                       String incrementalField, Object lastUpdateValue) throws Exception {
        String taskId = task.getId();
        String versionId = getCurrentVersionId(taskId);
        
        log.info("追加表 {} 的增量数据，增量字段: {}, 上次更新值: {}",
            table.getTableName(), incrementalField, lastUpdateValue);
            
        // 记录开始追加增量数据日志
        taskLogService.logTaskTable(taskId, versionId, "INFO", 
                "开始追加增量数据: " + table.getTableName(), 
                "APPEND_INCREMENTAL", UserUtils.getCurrentUsername(), 
                table.getId(), table.getTableName(), 
                "增量字段: " + incrementalField + ", 最近更新值: " + (lastUpdateValue != null ? lastUpdateValue.toString() : "null"));

        try {
            // 获取增量数据并追加
            // 这里需要根据实际情况实现增量数据加载逻辑
    
            // 示例实现：从源表查询大于lastUpdateValue的数据，并插入目标表
            // 具体实现根据业务需求编写
            
            // 记录追加数据完成日志
            taskLogService.logTaskTable(taskId, versionId, "INFO", 
                    "追加增量数据完成: " + table.getTableName(), 
                    "APPEND_INCREMENTAL_COMPLETE", UserUtils.getCurrentUsername(), 
                    table.getId(), table.getTableName(), 
                    "增量更新成功");
        } catch (Exception e) {
            String errorMsg = "追加增量数据失败: " + e.getMessage();
            log.error(errorMsg, e);
            
            // 记录追加数据失败日志
            taskLogService.logTaskTable(taskId, versionId, "ERROR", 
                    "追加增量数据失败: " + table.getTableName(), 
                    "APPEND_INCREMENTAL_ERROR", UserUtils.getCurrentUsername(), 
                    table.getId(), table.getTableName(), 
                    "错误: " + e.getMessage());
                    
            throw e;
        }
    }

    /**
     * 覆盖增量数据
     */
    private void overwriteIncrementalData(MaterializeTask task, TaskTable table, Connection connection,
                                          String incrementalField, Object lastUpdateValue) throws Exception {
        String taskId = task.getId();
        String versionId = getCurrentVersionId(taskId);
        
        log.info("覆盖表 {} 的增量数据，增量字段: {}, 上次更新值: {}",
            table.getTableName(), incrementalField, lastUpdateValue);
            
        // 记录开始覆盖增量数据日志
        taskLogService.logTaskTable(taskId, versionId, "INFO", 
                "开始覆盖增量数据: " + table.getTableName(), 
                "OVERWRITE_INCREMENTAL", UserUtils.getCurrentUsername(), 
                table.getId(), table.getTableName(), 
                "增量字段: " + incrementalField + ", 最近更新值: " + (lastUpdateValue != null ? lastUpdateValue.toString() : "null"));

        try {
            // 1. 删除匹配增量条件的数据
            deleteIncrementalData(connection, table.getTableName(), incrementalField, lastUpdateValue);
            
            // 记录删除旧数据日志
            taskLogService.logTaskTable(taskId, versionId, "INFO", 
                    "删除旧数据完成: " + table.getTableName(), 
                    "DELETE_OLD_DATA", UserUtils.getCurrentUsername(), 
                    table.getId(), table.getTableName(), 
                    "删除条件: " + incrementalField + " >= " + lastUpdateValue);
    
            // 2. 加载新的增量数据
            // 这里需要根据实际情况实现增量数据加载逻辑
    
            // 示例实现：从源表查询大于等于lastUpdateValue的数据，并插入目标表
            // 具体实现根据业务需求编写
            
            // 记录覆盖数据完成日志
            taskLogService.logTaskTable(taskId, versionId, "INFO", 
                    "覆盖增量数据完成: " + table.getTableName(), 
                    "OVERWRITE_INCREMENTAL_COMPLETE", UserUtils.getCurrentUsername(), 
                    table.getId(), table.getTableName(), 
                    "覆盖更新成功");
        } catch (Exception e) {
            String errorMsg = "覆盖增量数据失败: " + e.getMessage();
            log.error(errorMsg, e);
            
            // 记录覆盖数据失败日志
            taskLogService.logTaskTable(taskId, versionId, "ERROR", 
                    "覆盖增量数据失败: " + table.getTableName(), 
                    "OVERWRITE_INCREMENTAL_ERROR", UserUtils.getCurrentUsername(), 
                    table.getId(), table.getTableName(), 
                    "错误: " + e.getMessage());
                    
            throw e;
        }
    }

    /**
     * 删除增量数据
     */
    private void deleteIncrementalData(Connection connection, String tableName,
                                       String incrementalField, Object lastUpdateValue) throws SQLException {
        String deleteSql = "DELETE FROM " + tableName + " WHERE " + incrementalField + " >= ?";

        log.info("执行删除增量数据SQL: {}", deleteSql);
        try (PreparedStatement pstmt = connection.prepareStatement(deleteSql)) {
            // 设置参数
            setPreparedStatementParameter(pstmt, 1, lastUpdateValue);

            // 执行删除
            int deletedRows = pstmt.executeUpdate();
            log.info("从表 {} 中删除了 {} 条增量数据", tableName, deletedRows);
        } catch (SQLException e) {
            log.error("删除增量数据失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 设置PreparedStatement参数
     */
    private void setPreparedStatementParameter(PreparedStatement pstmt, int index, Object value) throws SQLException {
        if (value == null) {
            pstmt.setNull(index, Types.NULL);
        } else if (value instanceof String) {
            pstmt.setString(index, (String) value);
        } else if (value instanceof Integer) {
            pstmt.setInt(index, (Integer) value);
        } else if (value instanceof Long) {
            pstmt.setLong(index, (Long) value);
        } else if (value instanceof Double) {
            pstmt.setDouble(index, (Double) value);
        } else if (value instanceof BigDecimal) {
            pstmt.setBigDecimal(index, (BigDecimal) value);
        } else if (value instanceof java.sql.Date) {
            pstmt.setDate(index, (java.sql.Date) value);
        } else if (value instanceof Timestamp) {
            pstmt.setTimestamp(index, (Timestamp) value);
        } else if (value instanceof Boolean) {
            pstmt.setBoolean(index, (Boolean) value);
        } else {
            pstmt.setObject(index, value);
        }
    }

    /**
     * 获取最后更新值
     */
    private Object getLastUpdateValue(Connection connection, String tableName, String incrementalField) {
        try {
            String sql = "SELECT MAX(" + incrementalField + ") FROM " + tableName;

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getObject(1);
                }
            }
        } catch (SQLException e) {
            log.error("获取表 {} 的增量字段 {} 最后值失败: {}", tableName, incrementalField, e.getMessage(), e);
        }

        return null;
    }

    /**
     * 生成创建表的SQL语句
     */
    private String generateCreateTableSQL(TableStructure tableStructure, String dbType) {
        if (tableStructure == null || tableStructure.getFields() == null || tableStructure.getFields().isEmpty()) {
            throw new IllegalArgumentException("表结构不能为空");
        }

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("CREATE TABLE ").append(tableStructure.getTableName()).append(" (\n");

        List<String> columnDefinitions = new ArrayList<>();
        List<String> primaryKeys = new ArrayList<>();

        for (FieldStructure field : tableStructure.getFields()) {
            StringBuilder columnBuilder = new StringBuilder();
            columnBuilder.append("    ").append(field.getName()).append(" ");

            // 处理字段类型
            if ("VARCHAR".equalsIgnoreCase(field.getType()) || "CHAR".equalsIgnoreCase(field.getType())) {
                columnBuilder.append(field.getType()).append("(")
                        .append(field.getLength() != null ? field.getLength() : 255)
                        .append(")");
            } else if ("DECIMAL".equalsIgnoreCase(field.getType()) || "NUMERIC".equalsIgnoreCase(field.getType())) {
                columnBuilder.append(field.getType()).append("(")
                        .append(field.getPrecision() != null ? field.getPrecision() : 10)
                        .append(",")
                        .append(field.getScale() != null ? field.getScale() : 2)
                        .append(")");
            } else {
                columnBuilder.append(field.getType());
            }

            // 处理非空约束
            if (field.isNotNull()) {
                columnBuilder.append(" NOT NULL");
            }

            // 处理默认值
            if (StringUtils.isNotBlank(field.getDefaultValue())) {
                if ("CURRENT_TIMESTAMP".equalsIgnoreCase(field.getDefaultValue())) {
                    // 根据数据库类型处理当前时间戳
                    if ("oracle".equalsIgnoreCase(dbType) || "dm".equalsIgnoreCase(dbType)) {
                        columnBuilder.append(" DEFAULT SYSDATE");
                    } else if ("sqlserver".equalsIgnoreCase(dbType)) {
                        columnBuilder.append(" DEFAULT GETDATE()");
                    } else if ("postgresql".equalsIgnoreCase(dbType)) {
                        columnBuilder.append(" DEFAULT CURRENT_TIMESTAMP");
                    } else {
                        columnBuilder.append(" DEFAULT CURRENT_TIMESTAMP");
                    }
                } else {
                    columnBuilder.append(" DEFAULT '").append(field.getDefaultValue()).append("'");
                }
            }

            // 处理自增
            if (field.isAutoIncrement()) {
                if ("mysql".equalsIgnoreCase(dbType)) {
                    columnBuilder.append(" AUTO_INCREMENT");
                } else if ("sqlserver".equalsIgnoreCase(dbType)) {
                    columnBuilder.append(" IDENTITY(1,1)");
                } else if ("postgresql".equalsIgnoreCase(dbType)) {
                    // PostgreSQL 使用序列，这里简化处理
                    if ("BIGINT".equalsIgnoreCase(field.getType())) {
                        columnBuilder.append(" BIGSERIAL");
                    } else {
                        columnBuilder.append(" SERIAL");
                    }
                } else if ("oracle".equalsIgnoreCase(dbType) || "dm".equalsIgnoreCase(dbType)) {
                    // Oracle 和达梦使用序列，这里先不处理，后续可以通过触发器实现
                }
            }

            columnDefinitions.add(columnBuilder.toString());

            // 收集主键
            if (field.isPrimary()) {
                primaryKeys.add(field.getName());
            }
        }

        // 添加主键约束
        if (!primaryKeys.isEmpty()) {
            columnDefinitions.add("    PRIMARY KEY (" + String.join(", ", primaryKeys) + ")");
        }

        sqlBuilder.append(String.join(",\n", columnDefinitions));
        sqlBuilder.append("\n)");

        // Oracle 和 SQLServer 可能需要特殊处理
        if ("sqlserver".equalsIgnoreCase(dbType)) {
            // SQL Server 可能需要指定文件组
        } else if ("oracle".equalsIgnoreCase(dbType) || "dm".equalsIgnoreCase(dbType)) {
            // Oracle 和达梦可能需要指定表空间
        }

        return sqlBuilder.toString();
    }



    /**
     * 加载表数据
     */
    private void loadTableData(MaterializeTask task, TaskTable table, Connection connection, TableStructure tableStructure) throws Exception {
        String taskId = task.getId();
        String versionId = getCurrentVersionId(taskId);
        
        log.info("加载表 {} 的数据", table.getTableName());
        
        // 记录开始加载数据日志
        taskLogService.logTaskTable(taskId, versionId, "INFO", 
                "开始加载表数据: " + table.getTableName(), 
                "LOAD_TABLE_DATA", UserUtils.getCurrentUsername(), 
                table.getId(), table.getTableName(), 
                "目标表: " + table.getTableName());

        try {
            // 这里根据业务需求实现数据加载逻辑
            // 可能需要从源数据库查询数据，然后插入到目标表
    
            // 记录加载示例数据日志
            taskLogService.logTaskTable(taskId, versionId, "INFO", 
                    "加载示例数据完成: " + table.getTableName(), 
                    "LOAD_SAMPLE_DATA", UserUtils.getCurrentUsername(), 
                    table.getId(), table.getTableName(), 
                    "已加载示例数据");
                    
            // 记录数据加载完成日志
            taskLogService.logTaskTable(taskId, versionId, "INFO", 
                    "表数据加载完成: " + table.getTableName(), 
                    "LOAD_DATA_COMPLETE", UserUtils.getCurrentUsername(), 
                    table.getId(), table.getTableName(), 
                    "数据加载成功");
        } catch (Exception e) {
            String errorMsg = "加载表数据失败: " + e.getMessage();
            log.error(errorMsg, e);
            
            // 记录数据加载失败日志
            taskLogService.logTaskTable(taskId, versionId, "ERROR", 
                    "加载表数据失败: " + table.getTableName(), 
                    "LOAD_DATA_ERROR", UserUtils.getCurrentUsername(), 
                    table.getId(), table.getTableName(), 
                    "错误: " + e.getMessage());
                    
            throw e;
        }
    }

    /**
     * 插入示例数据
     */
    private void insertSampleData(Connection connection, String tableName, TableStructure tableStructure) throws SQLException {
        // 建立字段映射，排除自增主键
        List<String> insertFields = new ArrayList<>();
        for (FieldStructure field : tableStructure.getFields()) {
            if (!(field.isAutoIncrement() && field.isPrimary())) {
                insertFields.add(field.getName());
            }
        }

        if (insertFields.isEmpty()) {
            log.warn("表 {} 没有可插入字段", tableName);
            return;
        }

        // 构建INSERT语句
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("INSERT INTO ").append(tableName).append(" (");
        sqlBuilder.append(String.join(", ", insertFields));
        sqlBuilder.append(") VALUES (");

        // 添加参数占位符
        String[] placeholders = new String[insertFields.size()];
        Arrays.fill(placeholders, "?");
        sqlBuilder.append(String.join(", ", placeholders));
        sqlBuilder.append(")");

        String insertSql = sqlBuilder.toString();
        log.info("执行插入数据SQL: {}", insertSql);

        // 插入5条示例数据
        try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
            for (int i = 0; i < 5; i++) {
                int paramIndex = 1;

                // 为每个字段设置值
                for (String fieldName : insertFields) {
                    FieldStructure field = tableStructure.getFields().stream()
                            .filter(f -> f.getName().equals(fieldName))
                            .findFirst()
                            .orElse(null);

                    if (field == null) {
                        continue;
                    }

                    // 根据字段类型设置不同的示例值
                    switch (field.getType().toUpperCase()) {
                        case "VARCHAR":
                        case "CHAR":
                        case "TEXT":
                            pstmt.setString(paramIndex++, fieldName + "_value_" + i);
                            break;
                        case "INT":
                        case "INTEGER":
                        case "SMALLINT":
                        case "TINYINT":
                            pstmt.setInt(paramIndex++, i + 1);
                            break;
                        case "BIGINT":
                            pstmt.setLong(paramIndex++, i + 1L);
                            break;
                        case "DECIMAL":
                        case "NUMERIC":
                            pstmt.setBigDecimal(paramIndex++, new BigDecimal(i + 1.5));
                            break;
                        case "FLOAT":
                        case "DOUBLE":
                            pstmt.setDouble(paramIndex++, i + 1.5);
                            break;
                        case "DATE":
                            pstmt.setDate(paramIndex++, new java.sql.Date(System.currentTimeMillis()));
                            break;
                        case "DATETIME":
                        case "TIMESTAMP":
                            pstmt.setTimestamp(paramIndex++, new Timestamp(System.currentTimeMillis()));
                            break;
                        case "BOOLEAN":
                            pstmt.setBoolean(paramIndex++, i % 2 == 0);
                            break;
                        default:
                            pstmt.setString(paramIndex++, "默认值_" + i);
                            break;
                    }
                }

                pstmt.executeUpdate();
            }

            log.info("已向表 {} 插入5条示例数据", tableName);
        } catch (SQLException e) {
            log.error("插入示例数据失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 取消调度的任务
     */
    private void cancelScheduledTask(String taskId) {
        // 从当前服务中取消任务
        ScheduledFuture<?> scheduledFuture = scheduledTasks.get(taskId);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledTasks.remove(taskId);
            log.info("已取消服务内任务 {} 的调度", taskId);
        }

        // 同时从初始化器中取消任务
        try {
            boolean cancelled = com.mango.test.init.MaterializeTaskInitializer.cancelTask(taskId);
            if (cancelled) {
                log.info("已取消初始化器中任务 {} 的调度", taskId);
            }
        } catch (Exception e) {
            log.warn("从初始化器取消任务 {} 失败: {}", taskId, e.getMessage());
        }
    }

    /**
     * 更新任务状态
     */
    public boolean updateTaskStatus(String taskId, String status, Integer progress) {
        MaterializeTask task = getById(taskId);
        if (task == null) {
            return false;
        }

        task.setStatus(status);
        if (progress != null) {
            task.setProgress(progress);
        }
        task.setUpdateTime(new Date());

        // 如果任务已完成或失败，设置结束时间
        if ("COMPLETED".equals(status) || "FAILED".equals(status) || "CANCELLED".equals(status)) {
            task.setEndTime(LocalDateTime.now());
        }

        return updateById(task);
    }

    /**
     * 记录任务错误
     */
    private void recordTaskError(String taskId, Exception e) {
        String versionId = getCurrentVersionId(taskId);
        
        try {
            MaterializeTask task = getById(taskId);
            if (task == null) {
                return;
            }

            // 记录错误信息
            String errorMessage = e.getMessage();
            if (StringUtils.isBlank(errorMessage)) {
                errorMessage = "执行失败: " + e.getClass().getSimpleName();
            }

            // 更新任务的错误信息字段
            task.setErrorMessage(errorMessage);
            updateById(task);

            // 记录错误到版本（如果有版本服务）
            recordErrorToVersions(taskId, errorMessage);
            
            // 记录任务错误日志
            taskLogService.logTask(taskId, versionId, "ERROR", 
                    "任务执行出错: " + task.getTaskName(), 
                    "TASK_ERROR", UserUtils.getCurrentUsername(), 
                    "错误信息: " + errorMessage);

            log.info("已记录任务 {} 的错误: {}", taskId, errorMessage);
        } catch (Exception ex) {
            log.error("记录任务错误失败: {}", ex.getMessage(), ex);
        }
    }

    /**
     * 记录错误到版本
     */
    private void recordErrorToVersions(String taskId, String errorMessage) {
        if (taskVersionService == null) {
            return;
        }

        try {
            // 获取任务关联的版本
            LambdaQueryWrapper<TaskVersion> versionQueryWrapper = new LambdaQueryWrapper<>();
            versionQueryWrapper.eq(TaskVersion::getTaskId, taskId);
            List<TaskVersion> versions = taskVersionService.list(versionQueryWrapper);

            for (TaskVersion version : versions) {
                // 创建错误日志
                Map<String, Object> errorLog = new HashMap<>();
                errorLog.put("timestamp", LocalDateTime.now().toString());
                errorLog.put("level", "ERROR");
                errorLog.put("message", errorMessage);

                // 添加到版本日志
                List<Map<String, Object>> logs = version.getLogs();
                if (logs == null) {
                    logs = new ArrayList<>();
                }
                logs.add(errorLog);
                version.setLogs(logs);

                // 更新版本
                version.setStatus("failed");
                version.setUpdateTime(new Date());
                taskVersionService.updateById(version);
            }
        } catch (Exception e) {
            log.error("记录错误到版本失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 记录表错误
     */
    private void recordTableError(String taskId, String tableId, Exception e) {
        String versionId = getCurrentVersionId(taskId);
        
        try {
            if (StringUtils.isBlank(tableId)) {
                return;
            }

            // 查询表信息
            TaskTable table = taskTableService.getById(tableId);
            if (table == null) {
                return;
            }

            // 获取错误信息
            String errorMessage = e.getMessage();
            if (StringUtils.isBlank(errorMessage)) {
                errorMessage = "执行失败: " + e.getClass().getSimpleName();
            }

            // 更新表错误信息
            table.setStatus("FAILED");
            // 使用更新Map而不是调用不存在的setter方法
            Map<String, Object> updateMap = new HashMap<>();
            updateMap.put("id", table.getId());
            updateMap.put("status", "FAILED");
            updateMap.put("error_message", errorMessage);
            updateMap.put("update_time", LocalDateTime.now());
            taskTableService.updateById(table);
            
            // 记录表错误日志
            taskLogService.logTaskTable(taskId, versionId, "ERROR", 
                    "表执行出错: " + table.getTableName(), 
                    "TABLE_ERROR", UserUtils.getCurrentUsername(), 
                    table.getId(), table.getTableName(), 
                    "错误信息: " + errorMessage);

            log.info("已记录表 {} 的错误: {}", table.getTableName(), errorMessage);
        } catch (Exception ex) {
            log.error("记录表错误失败: {}", ex.getMessage(), ex);
        }
    }

    /**
     * 确保任务版本已创建
     */
    private void ensureTaskVersions(MaterializeTask task) {
        if (taskVersionService == null) {
            log.warn("未配置TaskVersionService，跳过版本创建");
            return;
        }

        try {
            // 获取任务关联的版本
            LambdaQueryWrapper<TaskVersion> versionQueryWrapper = new LambdaQueryWrapper<>();
            versionQueryWrapper.eq(TaskVersion::getTaskId, task.getId());
            List<TaskVersion> existingVersions = taskVersionService.list(versionQueryWrapper);

            // 如果没有版本，创建初始版本
            if (existingVersions.isEmpty()) {
                String versionId = UuidUtil.generateUuid();
                log.info("任务 {} 没有关联版本，创建初始版本", task.getId());
                createInitialVersions(task,versionId);
            } else {
                log.info("任务 {} 已有 {} 个关联版本", task.getId(), existingVersions.size());
            }
        } catch (Exception e) {
            log.error("确保任务版本失败: {}", e.getMessage(), e);
            // 版本创建失败不影响任务执行，继续执行
        }
    }

    /**
     * 更新任务版本状态
     */
    private void updateTaskVersionsStatus(String taskId, String status) {
        if (taskVersionService == null) {
            log.warn("未配置TaskVersionService，跳过版本状态更新");
            return;
        }

        try {
            // 获取任务关联的版本
            LambdaQueryWrapper<TaskVersion> versionQueryWrapper = new LambdaQueryWrapper<>();
            versionQueryWrapper.eq(TaskVersion::getTaskId, taskId);
            List<TaskVersion> versions = taskVersionService.list(versionQueryWrapper);

            // 更新版本状态
            for (TaskVersion version : versions) {
                version.setStatus(status);
                version.setUpdateTime(new Date());
                taskVersionService.updateById(version);
            }

            log.info("已更新任务 {} 的 {} 个版本状态为 {}", taskId, versions.size(), status);
        } catch (Exception e) {
            log.error("更新任务版本状态失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 将依赖关系字符串转换为列表
     * @param dependencies 依赖关系字符串，逗号分隔
     * @return 依赖关系列表
     */
    private List<String> getDependenciesList(String dependencies) {
        if (StringUtils.isBlank(dependencies)) {
            return Collections.emptyList();
        }
        return Arrays.asList(dependencies.split(","));
    }

    /**
     * 批量取消任务
     */
    @Override
    public boolean cancelBatchTasks(List<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return false;
        }

        boolean allSuccess = true;
        for (String taskId : taskIds) {
            boolean success = cancelTask(taskId);
            if (!success) {
                allSuccess = false;
                log.warn("取消任务 {} 失败", taskId);
            }
        }

        return allSuccess;
    }

    /**
     * 获取任务历史执行情况
     * @param taskId 任务ID
     * @return 历史执行记录
     */
    @Override
    public List<Map<String, Object>> getTaskExecutionHistory(String taskId) {
        List<Map<String, Object>> history = new ArrayList<>();

        try {
            // 查询任务版本历史
            if (StringUtils.isBlank(taskId) || taskVersionService == null) {
                return history;
            }

            // 获取所有版本
            LambdaQueryWrapper<TaskVersion> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TaskVersion::getTaskId, taskId)
                      .orderByDesc(TaskVersion::getCreateTime);

            List<TaskVersion> versions = taskVersionService.list(queryWrapper);

            // 构建执行历史
            for (TaskVersion version : versions) {
                Map<String, Object> record = new HashMap<>();
                record.put("versionId", version.getId());
                record.put("versionNum", version.getVersionNum());
                record.put("status", version.getStatus());
                record.put("createTime", version.getCreateTime());
                record.put("updateTime", version.getUpdateTime());
                record.put("description", version.getDescription());

                // 添加日志摘要
                if (version.getLogs() != null && !version.getLogs().isEmpty()) {
                    List<Map<String, Object>> logs = version.getLogs();
                    record.put("logs", logs.size() > 3 ? logs.subList(0, 3) : logs); // 只展示前3条日志
                }

                history.add(record);
            }

            return history;
        } catch (Exception e) {
            log.error("获取任务执行历史失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 取消任务
     * @param taskId 任务ID
     * @return 是否成功取消
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelTask(String taskId) {
        // 参数验证
        if (StringUtils.isBlank(taskId)) {
            log.warn("取消任务失败：任务ID为空");
            return false;
        }

        // 获取任务
        MaterializeTask task = getById(taskId);
        if (task == null) {
            log.warn("取消任务失败：任务不存在，ID: {}", taskId);
            return false;
        }

        // 检查任务状态
        if (!canCancelTask(task.getStatus())) {
            log.warn("无法取消任务：当前状态 {} 不允许取消", task.getStatus());
            return false;
        }

        log.info("开始取消任务: {}", taskId);

        // 取消调度任务
        cancelScheduledTask(taskId);

        // 更新任务状态
        task.setStatus("CANCELLED");
        task.setEndTime(LocalDateTime.now());
        task.setUpdateTime(new Date());

        // 更新任务
        boolean updated = updateById(task);

        if (updated) {
            log.info("任务 {} 已成功取消", taskId);

            // 更新关联的版本状态
            try {
                updateTaskVersionsStatus(taskId, "cancelled");
            } catch (Exception e) {
                log.error("更新任务版本状态失败: {}", e.getMessage(), e);
                // 不影响主流程，继续执行
            }
        } else {
            log.error("取消任务 {} 失败：更新数据库失败", taskId);
        }

        return updated;
    }

    /**
     * 判断任务是否可取消
     */
    private boolean canCancelTask(String status) {
        // 只有待执行、计划中和执行中的任务才能取消
        return "WAITING".equals(status) || "SCHEDULED".equals(status) || "RUNNING".equals(status);
    }

    /**
     * 批量执行任务
     * @param taskIds 任务ID列表
     * @return 执行结果，包含成功和失败的任务ID
     */
    @Override
    public Map<String, List<String>> executeBatchTasks(List<String> taskIds) {
        Map<String, List<String>> result = new HashMap<>();
        List<String> successList = new ArrayList<>();
        List<String> failList = new ArrayList<>();

        if (taskIds == null || taskIds.isEmpty()) {
            result.put("success", successList);
            result.put("fail", failList);
            return result;
        }

        for (String taskId : taskIds) {
            try {
                boolean success = executeTask(taskId);
                if (success) {
                    successList.add(taskId);
                } else {
                    failList.add(taskId);
                }
            } catch (Exception e) {
                log.error("执行任务 {} 失败: {}", taskId, e.getMessage(), e);
                failList.add(taskId);
            }
        }

        result.put("success", successList);
        result.put("fail", failList);
        return result;
    }

    /**
     * 获取任务的依赖关系图
     * @param taskId 任务ID
     * @return 依赖关系图数据
     */
    @Override
    public Map<String, Object> getTaskDependencyGraph(String taskId) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (StringUtils.isBlank(taskId)) {
                return Collections.emptyMap();
            }

            // 获取任务详情
            MaterializeTask task = getById(taskId);
            if (task == null) {
                return Collections.emptyMap();
            }

            // 获取任务相关的表
            List<TaskTable> tables = getTaskTables(taskId,getCurrentVersionId(taskId));
            if (tables.isEmpty()) {
                return Collections.emptyMap();
            }

            // 构建节点列表
            List<Map<String, Object>> nodes = new ArrayList<>();
            // 构建边列表
            List<Map<String, Object>> edges = new ArrayList<>();

            // 创建表ID到表的映射
            Map<String, TaskTable> tableMap = tables.stream()
                    .collect(Collectors.toMap(TaskTable::getTableId, table -> table));

            // 添加节点
            for (TaskTable table : tables) {
                Map<String, Object> node = new HashMap<>();
                node.put("id", table.getTableId());
                node.put("name", table.getTableName());
                node.put("isMain", table.getIsMain());
                node.put("type", table.getMaterializeType());
                nodes.add(node);

                // 处理依赖关系
                List<String> dependencies = getDependenciesList(table.getDependencies());
                for (String dependencyId : dependencies) {
                    if (StringUtils.isNotBlank(dependencyId) && tableMap.containsKey(dependencyId)) {
                        Map<String, Object> edge = new HashMap<>();
                        edge.put("source", dependencyId);
                        edge.put("target", table.getTableId());
                        edges.add(edge);
                    }
                }
            }

            result.put("nodes", nodes);
            result.put("edges", edges);
            result.put("taskId", taskId);
            result.put("taskName", task.getTaskName());

            return result;
        } catch (Exception e) {
            log.error("获取任务依赖关系图失败: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * 获取任务历史
     * @param request 历史查询请求
     * @return 分页结果
     */
    @Override
    public PageResult<MaterializeTask> getTaskHistory(TaskHistoryQueryRequest request) {
        if (request == null) {
            request = new TaskHistoryQueryRequest();
        }

        // 设置默认分页参数
        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 10;

        // 创建分页对象
        Page<MaterializeTask> page = new Page<>(pageNum, pageSize);

        // 构建查询条件
        LambdaQueryWrapper<MaterializeTask> queryWrapper = new LambdaQueryWrapper<>();

        // 添加时间范围条件
        if (request.getStartDateTime() != null) {
            queryWrapper.ge(MaterializeTask::getCreateTime, request.getStartDateTime());
        }

        if (request.getEndDateTime() != null) {
            queryWrapper.le(MaterializeTask::getCreateTime, request.getEndDateTime());
        }

        // 添加任务名称条件
        if (StringUtils.isNotBlank(request.getTaskName())) {
            queryWrapper.like(MaterializeTask::getTaskName, request.getTaskName());
        }

        // 添加状态条件
        if (StringUtils.isNotBlank(request.getStatus())) {
            queryWrapper.eq(MaterializeTask::getStatus, request.getStatus());
        }

        // 添加环境条件
        if (StringUtils.isNotBlank(request.getEnvironment())) {
            queryWrapper.eq(MaterializeTask::getEnvironment, request.getEnvironment());
        }

        // 添加创建者条件
        if (StringUtils.isNotBlank(request.getCreator())) {
            queryWrapper.eq(MaterializeTask::getCreator, request.getCreator());
        }

        // 添加模型ID条件
        if (StringUtils.isNotBlank(request.getModelId())) {
            queryWrapper.apply("EXISTS (SELECT 1 FROM t_task_table WHERE t_task_table.task_id = t_materialize_task.id AND t_task_table.model_id = {0})", request.getModelId());
        }

        // 默认按创建时间倒序排序
        queryWrapper.orderByDesc(MaterializeTask::getCreateTime);

        // 执行查询
        IPage<MaterializeTask> result = page(page, queryWrapper);

        // 将查询结果转换为PageResult
        PageResult<MaterializeTask> pageResult = new PageResult<>();
        pageResult.setPageNum(result.getCurrent());
        pageResult.setPageSize(result.getSize());
        pageResult.setTotal(result.getTotal());
        pageResult.setPages(result.getPages());
        pageResult.setRecords(result.getRecords());

        // 计算是否有上一页和下一页
        pageResult.setHasNext(result.getCurrent() < result.getPages());
        pageResult.setHasPrevious(result.getCurrent() > 1);

        // 如果需要，为每个任务加载关联的版本信息
        if (request.isIncludeVersions() && taskVersionService != null) {
            loadTaskVersions(pageResult.getRecords());
        }

        return pageResult;
    }

    /**
     * 为任务加载版本信息
     */
    private void loadTaskVersions(List<MaterializeTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        for (MaterializeTask task : tasks) {
            try {
                // 查询任务关联的版本
                LambdaQueryWrapper<TaskVersion> versionQueryWrapper = new LambdaQueryWrapper<>();
                versionQueryWrapper.eq(TaskVersion::getTaskId, task.getId())
                          .orderByDesc(TaskVersion::getCreateTime);

                List<TaskVersion> versions = taskVersionService.list(versionQueryWrapper);

                // 将版本设置到任务对象
                task.setVersions(versions);

                // 计算版本统计信息
                Map<String, Integer> versionStats = new HashMap<>();
                for (TaskVersion version : versions) {
                    String status = version.getStatus();
                    versionStats.put(status, versionStats.getOrDefault(status, 0) + 1);
                }
                task.setVersionStats(versionStats);

            } catch (Exception e) {
                log.error("加载任务 {} 的版本信息失败: {}", task.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * 获取任务日志
     * @param taskId 任务ID
     * @return 日志内容
     */
    @Override
    public String getTaskLogs(String taskId) {
        if (StringUtils.isBlank(taskId)) {
            return "任务ID不能为空";
        }

        MaterializeTask task = getById(taskId);
        if (task == null) {
            return "任务不存在";
        }

        // 从任务日志表获取实际日志
        List<TaskLog> logs = taskLogService.getTaskLogs(taskId);
        
        if (logs == null || logs.isEmpty()) {
            return "暂无日志记录";
        }
        
        // 将日志格式化为易读的文本
        StringBuilder logContent = new StringBuilder();
        for (TaskLog log : logs) {
            StringBuilder logEntry = new StringBuilder();
            // 日志级别和时间
            logEntry.append(String.format("[%s] %s - ", 
                log.getLogLevel(), 
                new Date()));
            
            // 日志内容
            logEntry.append(log.getContent());
            
            // 如果有表信息，添加表相关内容
            if (StringUtils.isNotBlank(log.getTableName())) {
                logEntry.append(String.format(" | 表: %s", log.getTableName()));
            }
            
            // 如果有详情，添加详情
            if (StringUtils.isNotBlank(log.getDetails())) {
                logEntry.append(String.format(" | %s", log.getDetails()));
            }
            
            logContent.append(logEntry).append("\n");
        }
        
        // 如果任务状态为失败，添加错误信息
        if ("FAILED".equals(task.getStatus()) && StringUtils.isNotBlank(task.getErrorMessage())) {
            logContent.append(String.format("\n[ERROR] 任务执行失败: %s\n", task.getErrorMessage()));
        }

        return logContent.toString();
    }

    /**
     * 格式化日期时间
     */
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 获取任务监控统计信息
     * @return 任务统计信息
     */
    @Override
    public Map<String, Object> getTaskStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        try {
            // 获取各状态任务数量
            Map<String, Long> statusCounts = new HashMap<>();

            // 查询各种状态的任务数量
            String[] statuses = {"WAITING", "RUNNING", "COMPLETED", "FAILED", "CANCELLED", "SCHEDULED", "PARTIAL"};
            for (String status : statuses) {
                LambdaQueryWrapper<MaterializeTask> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(MaterializeTask::getStatus, status);
                long count = count(queryWrapper);
                statusCounts.put(status, count);
            }

            statistics.put("statusCounts", statusCounts);

            // 获取今日创建的任务数
            LambdaQueryWrapper<MaterializeTask> todayQueryWrapper = new LambdaQueryWrapper<>();
            LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            todayQueryWrapper.ge(MaterializeTask::getCreateTime, startOfDay);
            long todayCreated = count(todayQueryWrapper);
            statistics.put("todayCreated", todayCreated);

            // 获取今日完成的任务数
            LambdaQueryWrapper<MaterializeTask> todayCompletedQueryWrapper = new LambdaQueryWrapper<>();
            todayCompletedQueryWrapper.eq(MaterializeTask::getStatus, "COMPLETED")
                                   .ge(MaterializeTask::getEndTime, startOfDay);
            long todayCompleted = count(todayCompletedQueryWrapper);
            statistics.put("todayCompleted", todayCompleted);

            // 获取任务平均执行时长（对于已完成的任务）
            LambdaQueryWrapper<MaterializeTask> completedQueryWrapper = new LambdaQueryWrapper<>();
            completedQueryWrapper.eq(MaterializeTask::getStatus, "COMPLETED")
                             .isNotNull(MaterializeTask::getStartTime)
                             .isNotNull(MaterializeTask::getEndTime);
            List<MaterializeTask> completedTasks = list(completedQueryWrapper);

            if (!completedTasks.isEmpty()) {
                long totalDurationSeconds = 0;
                for (MaterializeTask task : completedTasks) {
                    long durationSeconds = java.time.Duration.between(task.getStartTime(), task.getEndTime()).getSeconds();
                    totalDurationSeconds += durationSeconds;
                }
                double avgDurationMinutes = (double) totalDurationSeconds / completedTasks.size() / 60.0;
                statistics.put("avgExecutionTimeMinutes", avgDurationMinutes);
            }

            // 获取最长执行时间的任务
            if (!completedTasks.isEmpty()) {
                MaterializeTask longestTask = completedTasks.stream()
                    .max(Comparator.comparing(task ->
                        java.time.Duration.between(task.getStartTime(), task.getEndTime()).getSeconds()))
                    .orElse(null);

                if (longestTask != null) {
                    Map<String, Object> longestTaskInfo = new HashMap<>();
                    longestTaskInfo.put("id", longestTask.getId());
                    longestTaskInfo.put("name", longestTask.getTaskName());
                    longestTaskInfo.put("durationMinutes",
                        java.time.Duration.between(longestTask.getStartTime(), longestTask.getEndTime()).getSeconds() / 60.0);
                    statistics.put("longestTask", longestTaskInfo);
                }
            }

            return statistics;
        } catch (Exception e) {
            log.error("获取任务统计信息失败: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * 为单个任务创建调度
     * @param task 物化任务
     */
    private void scheduleTask(MaterializeTask task) {
        if (taskScheduler == null) {
            log.error("任务调度器未配置，无法调度任务");
            return;
        }

        try {
            String taskId = task.getId();

            // 根据调度类型创建触发器
            if ("scheduled".equals(task.getScheduleType()) && task.getScheduledTime() != null) {
                // 单次定时调度 - 将LocalDateTime转换为Date对象
                Date scheduledDate = Date.from(task.getScheduledTime().atZone(java.time.ZoneId.systemDefault()).toInstant());

                ScheduledFuture<?> future = taskScheduler.schedule(() -> {
                    try {
                        // 执行任务
                        executeTask(taskId);
                    } catch (Exception e) {
                        log.error("执行调度任务失败: {}", taskId, e);
                    }
                }, scheduledDate);

                // 存储调度任务引用
                scheduledTasks.put(taskId, future);
                log.info("已调度单次任务: {}, 执行时间: {}",
                        taskId, task.getScheduledTime());

            } else if ("periodic".equals(task.getScheduleType()) && StringUtils.isNotBlank(task.getCronExpression())) {
                // 周期性调度
                ScheduledFuture<?> future = taskScheduler.schedule(() -> {
                    try {
                        // 执行任务
                        executeTask(taskId);
                    } catch (Exception e) {
                        log.error("执行调度任务失败: {}", taskId, e);
                    }
                }, new CronTrigger(task.getCronExpression()));

                // 存储调度任务引用
                scheduledTasks.put(taskId, future);
                log.info("已调度周期性任务: {}, Cron表达式: {}",
                        taskId, task.getCronExpression());
            } else {
                log.warn("任务 {} 调度类型或参数无效，无法创建调度", taskId);
            }
        } catch (Exception e) {
            log.error("为任务 {} 创建调度失败: {}", task.getId(), e.getMessage(), e);
            throw e;
        }
    }



    @Override
    public Map<String, Object> analyzeTaskStructureChanges(String taskId) {
        Map<String, Object> result = new HashMap<>();
        result.put("hasChanges", false);
        result.put("modelsChanged", new ArrayList<String>());
        result.put("changeDetails", new HashMap<String, Object>());

        MaterializeTask task = getById(taskId);
        if (task == null) {
            log.warn("任务 {} 不存在", taskId);
            result.put("error", "任务不存在");
            return result;
        }

        try {
            List<String> modelsChanged = new ArrayList<>();
            Map<String, Object> changeDetails = new HashMap<>();
            boolean hasChanges = false;

            // 检查每个模型的变化
            for (String modelId : task.getModelIdList()) {
                String dataSourceId = null;
                String databaseId = null;

                if ("theme".equals(task.getDataSourceType()) && StringUtils.isNotBlank(task.getDomainId())) {
                    dataSourceId = task.getDomainId();
                    databaseId = task.getDomainId();
                } else if ("custom".equals(task.getDataSourceType()) && StringUtils.isNotBlank(task.getCustomDataSourceId())) {
                    dataSourceId = task.getCustomDataSourceId();
                    databaseId = task.getCustomDataSourceId();
                }

                // 检查模型结构变化
                Map<String, Object> modelChanges = taskVersionService.checkModelStructureChanges(modelId, dataSourceId, databaseId);
                boolean modelHasChanges = (boolean) modelChanges.getOrDefault("hasChanges", false);

                if (modelHasChanges) {
                    hasChanges = true;
                    modelsChanged.add(modelId);
                    changeDetails.put(modelId, modelChanges);
                }
            }

            result.put("hasChanges", hasChanges);
            result.put("modelsChanged", modelsChanged);
            result.put("changeDetails", changeDetails);

        } catch (Exception e) {
            log.error("分析任务结构变化失败", e);
            result.put("error", e.getMessage());
        }

        return result;
    }


    /**
     * 为任务创建表
     *
     * @param taskId 任务ID
     * @param modelIds 模型ID列表
     * @return 任务表列表
     */
    private List<TaskTable> createTaskTables(String taskId, List<String> modelIds,String versionId) {
        List<TaskTable> taskTables = new ArrayList<>();

        if (modelIds == null || modelIds.isEmpty()) {
            return taskTables;
        }

        try {

            for (String modelId : modelIds) {
                // 查询模型相关的表
                List<Table> tables = tableService.getTablesByModelId(modelId);

                if (tables == null || tables.isEmpty()) {
                    log.warn("模型 {} 没有相关的表", modelId);
                    continue;
                }

                // 为每个表创建对应的任务表
                for (Table table : tables) {
                    TaskTable taskTable = new TaskTable();
                    taskTable.setId(UuidUtil.generateUuid());
                    taskTable.setTaskId(taskId);
                    taskTable.setTableId(table.getId());
                    taskTable.setModelId(modelId);

                    // 设置表名
                    String tableName = table.getName();
                    taskTable.setTableName(tableName);

                    // 复制其他属性
                    taskTable.setTableComment(table.getComment());
                    taskTable.setIsMain(false); // 默认不是主表
                    taskTable.setMaterializeType("full"); // 默认全量物化
                    taskTable.setWriteMode("overwrite"); // 默认覆盖模式
                    taskTable.setStatus("waiting"); // 初始状态

                    // 创建时间和更新时间
                    Date now = new Date();
                    taskTable.setCreateTime(now);
                    taskTable.setUpdateTime(now);
                    taskTable.setVersionId(versionId);
                    taskTables.add(taskTable);
                }
            }

            // 批量保存
            if (!taskTables.isEmpty()) {
                boolean saved = taskTableService.saveBatch(taskTables);
                if (!saved) {
                    log.error("保存任务表信息失败");
                    throw new RuntimeException("保存任务表信息失败");
                }

                log.info("成功为任务 {} 创建 {} 个表", taskId, taskTables.size());
            }

            return taskTables;
        } catch (Exception e) {
            log.error("创建任务表失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建任务表失败: " + e.getMessage(), e);
        }
    }


    /**
     * 获取任务当前版本ID
     */
    private String getCurrentVersionId(String taskId) {
        if (StringUtils.isBlank(taskId)) {
            return null;
        }

        try {
            // 获取任务关联的模型
            MaterializeTask task = getById(taskId);
            if (task == null || task.getModelIdList().isEmpty()) {
                return null;
            }

            // 获取任务的第一个模型对应的当前版本
            List<String> modelIdList = task.getModelIdList();
            if (!modelIdList.isEmpty()) {
                String firstModelId = modelIdList.get(0);
                // 搜索第一个模型的当前版本
                LambdaQueryWrapper<TaskVersion> versionQuery = new LambdaQueryWrapper<>();
                versionQuery.eq(TaskVersion::getTaskId, taskId)
                        .eq(TaskVersion::getModelId, firstModelId)
                        .eq(TaskVersion::getIsCurrent, true)
                        .orderByDesc(TaskVersion::getPublishTime)
                        .last("LIMIT 1");

                TaskVersion currentVersion = taskVersionService.getOne(versionQuery);
                if (currentVersion != null) {
                    return currentVersion.getId();
                }

                // 如果没有找到当前版本，则获取最新版本
                versionQuery = new LambdaQueryWrapper<>();
                versionQuery.eq(TaskVersion::getTaskId, taskId)
                        .eq(TaskVersion::getModelId, firstModelId)
                        .orderByDesc(TaskVersion::getPublishTime)
                        .last("LIMIT 1");

                TaskVersion latestVersion = taskVersionService.getOne(versionQuery);
                if (latestVersion != null) {
                    return latestVersion.getId();
                }
            }
        } catch (Exception e) {
            log.error("获取任务当前版本ID失败: {}", e.getMessage(), e);
        }

        return null;
    }

    /**
     * 获取任务的详细日志
     * @param taskId 任务ID
     * @return 任务日志列表
     */
    @Override
    public List<Map<String, Object>> getTaskDetailLogs(String taskId) {
        if (StringUtils.isBlank(taskId)) {
            return Collections.emptyList();
        }

        // 获取任务日志
        List<TaskLog> taskLogs = taskLogService.getTaskLogs(taskId);
        if (taskLogs.isEmpty()) {
            return Collections.emptyList();
        }

        // 转换为前端需要的格式
        return taskLogs.stream().map(log -> {
            Map<String, Object> result = new HashMap<>();
            result.put("id", log.getId());
            result.put("taskId", log.getTaskId());
            result.put("versionId", log.getVersionId());
            result.put("logLevel", log.getLogLevel());
            result.put("content", log.getContent());
            result.put("operationType", log.getOperationType());
            result.put("operator", log.getOperator());
            result.put("tableId", log.getTableId());
            result.put("tableName", log.getTableName());
            result.put("details", log.getDetails());
            result.put("createTime", new Date());
            return result;
        }).collect(Collectors.toList());
    }

    /**
     * 回滚到指定版本
     * @param versionId 要回滚到的版本ID
     * @param taskId 任务ID
     * @param description 回滚描述
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rollbackToVersion(String versionId, String taskId, String description) {
        try {
            // 1. 获取版本详情
            TaskVersion targetVersion = taskVersionService.getById(versionId);
            if (targetVersion == null) {
                log.error("回滚失败：找不到目标版本信息 versionId={}", versionId);
                return false;
            }
            
            // 获取当前版本
            List<TaskVersion> versions = taskVersionService.getVersionsByTaskId(taskId);
            TaskVersion currentVersion = null;
            for (TaskVersion v : versions) {
                if (v.getIsCurrent() != null && v.getIsCurrent()) {
                    currentVersion = v;
                    break;
                }
            }
            
            // 确保不是回滚到当前版本
            if (currentVersion != null && versionId.equals(currentVersion.getId())) {
                log.warn("无需回滚：目标版本已经是当前版本 versionId={}", versionId);
                return true;
            }
            
            // 2. 创建新版本
            TaskVersion rollbackVersion = new TaskVersion();
            // 保留原版本中的大部分信息
            rollbackVersion.setTaskId(taskId);
            rollbackVersion.setModelId(targetVersion.getModelId());
            rollbackVersion.setDataSourceId(targetVersion.getDataSourceId());
            rollbackVersion.setDatabaseId(targetVersion.getDatabaseId());
            
            // 设置新的版本号（当前最高版本号+0.1）
            String newVersion = getNextVersionNumber(versions);
            rollbackVersion.setVersionNum(newVersion);
            
            rollbackVersion.setEnvironment(targetVersion.getEnvironment());
            rollbackVersion.setStatus("WAITING"); // 初始状态为等待中
            rollbackVersion.setPublisher(UserUtils.getCurrentUsername());
            rollbackVersion.setPublishTime(LocalDateTime.now());
            rollbackVersion.setDescription(description != null ? description : "回滚到版本 v" + targetVersion.getVersionNum());
            rollbackVersion.setMode("full"); // 回滚总是全量模式
            rollbackVersion.setIsRollback(true); // 标记为回滚版本
            rollbackVersion.setRollbackFromVersionId(versionId); // 记录回滚源版本
            
            // 复制目标版本的配置
            rollbackVersion.setConfig(targetVersion.getConfig());
            
            // 设置变更记录
            List<Map<String, Object>> changes = new ArrayList<>();
            Map<String, Object> change = new HashMap<>();
            change.put("type", "ROLLBACK");
            change.put("path", "version/v" + targetVersion.getVersionNum());
            change.put("description", "回滚到版本 v" + targetVersion.getVersionNum());
            changes.add(change);
            rollbackVersion.setChanges(changes);
            
            // 保存新版本
            taskVersionService.save(rollbackVersion);
            final String rollbackVersionId = rollbackVersion.getId();
            
            // 3. 获取任务详情
            MaterializeTask task = getById(taskId);
            if (task == null) {
                log.error("回滚失败：找不到任务信息 taskId={}", taskId);
                return false;
            }
            
            // 4. 复制目标版本的表结构
            List<TaskTable> targetTables = taskTableService.getTablesByVersionId(versionId);
            if (targetTables == null || targetTables.isEmpty()) {
                log.error("回滚失败：目标版本没有表结构信息 versionId={}", versionId);
                return false;
            }
            
            // 5. 为新版本创建表记录
            List<TaskTable> rollbackTables = new ArrayList<>();
            for (TaskTable targetTable : targetTables) {
                TaskTable rollbackTable = new TaskTable();
                // 复制属性
                rollbackTable.setTaskId(taskId);
                rollbackTable.setVersionId(rollbackVersionId);
                rollbackTable.setTableId(targetTable.getTableId());
                rollbackTable.setTableName(targetTable.getTableName());
                rollbackTable.setTableComment(targetTable.getTableComment());
                rollbackTable.setModelId(targetTable.getModelId());
                rollbackTable.setModelName(targetTable.getModelName());
                rollbackTable.setIsMain(targetTable.getIsMain());
                rollbackTable.setMaterializeType(targetTable.getMaterializeType());
                rollbackTable.setIncrementalField(targetTable.getIncrementalField());
                rollbackTable.setWriteMode(targetTable.getWriteMode());
                rollbackTable.setDependencies(targetTable.getDependencies());
                rollbackTable.setCreateTime(new Date());
                rollbackTable.setUpdateTime(new Date());
                rollbackTable.setStatus("WAITING");
                
                rollbackTables.add(rollbackTable);
            }
            
            // 批量保存表记录
            taskTableService.saveBatch(rollbackTables);
            
            // 6. 复制字段信息
            for (int i = 0; i < targetTables.size(); i++) {
                TaskTable targetTable = targetTables.get(i);
                TaskTable rollbackTable = rollbackTables.get(i);
                
                // 获取目标表的字段
                List<TaskTableField> targetFields = taskTableFieldService.listByTableId(targetTable.getId());
                if (targetFields != null && !targetFields.isEmpty()) {
                    List<TaskTableField> rollbackFields = new ArrayList<>();
                    for (TaskTableField targetField : targetFields) {
                        TaskTableField rollbackField = new TaskTableField();
                        // 复制属性
                        rollbackField.setTaskId(taskId);
                        rollbackField.setVersionId(rollbackVersionId);
                        rollbackField.setTableId(rollbackTable.getId());
                        rollbackField.setFieldName(targetField.getFieldName());
                        rollbackField.setFieldType(targetField.getFieldType());
                        rollbackField.setLength(targetField.getLength());
                        rollbackField.setPrecision(targetField.getPrecision());
                        rollbackField.setScale(targetField.getScale());
                        rollbackField.setIsPrimary(targetField.getIsPrimary());
                        rollbackField.setNotNull(targetField.getNotNull());
                        rollbackField.setAutoIncrement(targetField.getAutoIncrement());
                        rollbackField.setDefaultValue(targetField.getDefaultValue());
                        rollbackField.setFieldComment(targetField.getFieldComment());
                        rollbackField.setFieldOrder(targetField.getFieldOrder());
                        rollbackField.setCreateTime(new Date());
                        rollbackField.setUpdateTime(new Date());

                        rollbackFields.add(rollbackField);
                    }
                    
                    // 批量保存字段记录
                    taskTableFieldService.saveBatch(rollbackFields);
                }
            }
            
            // 7. 执行物化操作 - 使用单独的线程
            final String finalTaskId = taskId;
            final TaskVersion finalTargetVersion = targetVersion;
            final MaterializeTask finalTask = task;
            final TaskVersion finalRollbackVersion = rollbackVersion;
            final List<TaskTable> finalRollbackTables = rollbackTables;
            final TaskVersion finalCurrentVersion = currentVersion;
            
            CompletableFuture.runAsync(() -> {
                try {
                    // 更新版本状态为running
                    finalRollbackVersion.setStatus("RUNNING");
                    taskVersionService.updateById(finalRollbackVersion);
                    
                    // 记录回滚开始日志
                    TaskLog startLog = new TaskLog();
                    startLog.setTaskId(finalTaskId);
                    startLog.setVersionId(rollbackVersionId);
                    startLog.setLogLevel("INFO");
                    startLog.setContent("开始执行回滚到版本 v" + finalTargetVersion.getVersionNum());
                    startLog.setOperationType("ROLLBACK");
                    startLog.setOperator(UserUtils.getCurrentUsername());
                    startLog.setCreateTime(new Date());
                    taskLogService.save(startLog);
                    
                    // 执行回滚物化操作
                    boolean success = executeRollbackMaterialization(finalTask, finalRollbackTables, finalRollbackVersion);
                    
                    // 更新版本状态
                    finalRollbackVersion.setStatus(success ? "SUCCESS" : "FAILED");
                    if (success) {
                        // 如果成功，标记为当前版本
                        if (finalCurrentVersion != null) {
                            finalCurrentVersion.setIsCurrent(false);
                            taskVersionService.updateById(finalCurrentVersion);
                        }
                        finalRollbackVersion.setIsCurrent(true);
                    }
                    taskVersionService.updateById(finalRollbackVersion);
                    
                    // 记录回滚结束日志
                    TaskLog endLog = new TaskLog();
                    endLog.setTaskId(finalTaskId);
                    endLog.setVersionId(rollbackVersionId);
                    endLog.setLogLevel(success ? "INFO" : "ERROR");
                    endLog.setContent("回滚到版本 v" + finalTargetVersion.getVersionNum() + (success ? "成功" : "失败"));
                    endLog.setOperationType("ROLLBACK");
                    endLog.setOperator(UserUtils.getCurrentUsername());
                    endLog.setCreateTime(new Date());
                    taskLogService.save(endLog);
                    
                } catch (Exception e) {
                    log.error("回滚物化操作失败", e);
                    // 更新版本状态为失败
                    finalRollbackVersion.setStatus("FAILED");
                    taskVersionService.updateById(finalRollbackVersion);
                    
                    // 记录错误日志
                    TaskLog errorLog = new TaskLog();
                    errorLog.setTaskId(finalTaskId);
                    errorLog.setVersionId(rollbackVersionId);
                    errorLog.setLogLevel("ERROR");
                    errorLog.setContent("回滚物化操作失败: " + e.getMessage());
                    errorLog.setOperationType("ROLLBACK");
                    errorLog.setOperator(UserUtils.getCurrentUsername());
                    errorLog.setCreateTime(new Date());
                    taskLogService.save(errorLog);
                }
            });
            
            return true;
        } catch (Exception e) {
            log.error("回滚版本处理失败", e);
            return false;
        }
    }
    
    /**
     * 获取下一个版本号
     */
    private String getNextVersionNumber(List<TaskVersion> versions) {
        // 找出当前最高版本号
        double highestVersion = 0;
        for (TaskVersion version : versions) {
            try {
                String versionNum = version.getVersionNum();
                double versionDouble = Double.parseDouble(versionNum);
                if (versionDouble > highestVersion) {
                    highestVersion = versionDouble;
                }
            } catch (NumberFormatException ignored) {
                // 忽略非数字版本号
            }
        }
        
        // 返回最高版本号 + 0.1
        return String.format("%.1f", highestVersion + 0.1);
    }

    
    /**
     * 执行回滚物化操作
     */
    private boolean executeRollbackMaterialization(MaterializeTask task, List<TaskTable> tables, TaskVersion version) {
        try {
            // 1. 根据依赖关系确定执行顺序
            List<TaskTable> orderedTables = determineExecutionOrder(tables, task.getExecutionMode());
            if (orderedTables == null || orderedTables.isEmpty()) {
                log.error("无法确定回滚表的执行顺序，taskId={}", task.getId());
                return false;
            }
            
            // 2. 记录开始回滚日志
            TaskLog startLog = new TaskLog();
            startLog.setTaskId(task.getId());
            startLog.setVersionId(version.getId());
            startLog.setLogLevel("INFO");
            startLog.setContent("开始执行回滚版本操作，版本号: " + version.getVersionNum());
            startLog.setOperationType("ROLLBACK_START");
            startLog.setOperator(UserUtils.getCurrentUsername());
            startLog.setCreateTime(new Date());
            taskLogService.save(startLog);
            
            // 3. 顺序执行每个表的回滚
            for (TaskTable table : orderedTables) {
                try {
                    // 记录开始处理表日志
                    TaskLog tableStartLog = new TaskLog();
                    tableStartLog.setTaskId(task.getId());
                    tableStartLog.setVersionId(version.getId());
                    tableStartLog.setLogLevel("INFO");
                    tableStartLog.setContent("开始回滚表: " + table.getTableName());
                    tableStartLog.setOperationType("ROLLBACK_TABLE_START");
                    tableStartLog.setTableId(table.getId());
                    tableStartLog.setTableName(table.getTableName());
                    tableStartLog.setOperator(UserUtils.getCurrentUsername());
                    tableStartLog.setCreateTime(new Date());
                    taskLogService.save(tableStartLog);
                    
                    // 执行单表回滚
                    executeRollbackTable(task, table, version);
                    
                    // 记录表回滚完成日志
                    TaskLog tableEndLog = new TaskLog();
                    tableEndLog.setTaskId(task.getId());
                    tableEndLog.setVersionId(version.getId());
                    tableEndLog.setLogLevel("INFO");
                    tableEndLog.setContent("表回滚完成: " + table.getTableName());
                    tableEndLog.setOperationType("ROLLBACK_TABLE_COMPLETE");
                    tableEndLog.setTableId(table.getId());
                    tableEndLog.setTableName(table.getTableName());
                    tableEndLog.setOperator(UserUtils.getCurrentUsername());
                    tableEndLog.setCreateTime(new Date());
                    taskLogService.save(tableEndLog);
                    
                } catch (Exception e) {
                    // 记录表回滚错误日志
                    log.error("回滚表失败, taskId={}, tableId={}, error={}", task.getId(), table.getId(), e.getMessage(), e);
                    
                    TaskLog errorLog = new TaskLog();
                    errorLog.setTaskId(task.getId());
                    errorLog.setVersionId(version.getId());
                    errorLog.setLogLevel("ERROR");
                    errorLog.setContent("回滚表失败: " + table.getTableName() + ", 错误: " + e.getMessage());
                    errorLog.setOperationType("ROLLBACK_TABLE_ERROR");
                    errorLog.setTableId(table.getId());
                    errorLog.setTableName(table.getTableName());
                    errorLog.setOperator(UserUtils.getCurrentUsername());
                    errorLog.setCreateTime(new Date());
                    taskLogService.save(errorLog);
                    
                    return false;
                }
            }
            
            // 4. 记录回滚完成日志
            TaskLog endLog = new TaskLog();
            endLog.setTaskId(task.getId());
            endLog.setVersionId(version.getId());
            endLog.setLogLevel("INFO");
            endLog.setContent("回滚版本操作完成，版本号: " + version.getVersionNum());
            endLog.setOperationType("ROLLBACK_COMPLETE");
            endLog.setOperator(UserUtils.getCurrentUsername());
            endLog.setCreateTime(new Date());
            taskLogService.save(endLog);
            
            return true;
        } catch (Exception e) {
            log.error("执行回滚物化过程失败, taskId={}, error={}", task.getId(), e.getMessage(), e);
            
            // 记录整体回滚失败日志
            TaskLog errorLog = new TaskLog();
            errorLog.setTaskId(task.getId());
            errorLog.setVersionId(version.getId());
            errorLog.setLogLevel("ERROR");
            errorLog.setContent("回滚物化过程失败: " + e.getMessage());
            errorLog.setOperationType("ROLLBACK_ERROR");
            errorLog.setOperator(UserUtils.getCurrentUsername());
            errorLog.setCreateTime(new Date());
            taskLogService.save(errorLog);
            
            return false;
        }
    }
    
    /**
     * 执行单个表的回滚物化操作
     * 注意：此方法独立于executeTable方法，避免修改现有逻辑
     */
    private void executeRollbackTable(MaterializeTask task, TaskTable table, TaskVersion version) throws Exception {
        // 1. 获取数据库连接
        AbstractDatabaseHandler dbHandler = getDbHandler(task);
        if (dbHandler == null) {
            throw new Exception("无法获取数据库处理器");
        }
        
        Connection connection = null;
        try {
            connection = dbHandler.getConnection();
            
            // 2. 获取表结构
            TableStructure tableStructure = getTableStructure(task, table);
            if (tableStructure == null) {
                throw new Exception("无法获取表结构信息");
            }
            
            // 3. 检查表是否存在，如果存在则先删除
            String tableExistsSql = dbHandler.getTableExistsSql(table.getTableName());
            boolean tableExists = false;
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(tableExistsSql)) {
                tableExists = rs.next() && rs.getInt(1) > 0;
            }
            
            if (tableExists) {
                // 使用标准JDBC删除表，而不是直接调用不存在的方法
                String dropTableSql = dbHandler.getDropTableSql(table.getTableName());
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute(dropTableSql);
                    
                    // 记录删除表日志
                    TaskLog dropLog = new TaskLog();
                    dropLog.setTaskId(task.getId());
                    dropLog.setVersionId(version.getId());
                    dropLog.setLogLevel("INFO");
                    dropLog.setContent("回滚操作：删除现有表 [" + table.getTableName() + "]");
                    dropLog.setOperationType("DROP_TABLE");
                    dropLog.setTableId(table.getId());
                    dropLog.setTableName(table.getTableName());
                    dropLog.setOperator(UserUtils.getCurrentUsername());
                    dropLog.setCreateTime(new Date());
                    taskLogService.save(dropLog);
                }
            }
            
            // 4. 创建表 - 准备列定义
            List<ColumnDefinition> columns = new ArrayList<>();
            for (FieldStructure field : tableStructure.getFields()) {
                ColumnDefinition column = new ColumnDefinition();
                column.setName(field.getName());
                column.setType(field.getType());
                column.setLength(field.getLength());
                column.setPrecision(field.getPrecision());
                column.setScale(field.getScale());
                column.setNullable(!field.isNotNull());
                column.setPrimaryKey(field.isPrimary());
                column.setAutoIncrement(field.isAutoIncrement());
                column.setDefaultValue(field.getDefaultValue());
                column.setComment(field.getComment());
                columns.add(column);
            }
            
            // 生成创建表SQL
            String createSql = dbHandler.getCreateTableSql(
                table.getTableName(),
                columns,
                tableStructure.getDisplayName(), // 表注释
                "", // 默认引擎
                "", // 默认字符集
                "" // 默认排序规则
            );
            
            // 执行创建表SQL
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(createSql);
                
                // 记录创建表日志
                TaskLog createLog = new TaskLog();
                createLog.setTaskId(task.getId());
                createLog.setVersionId(version.getId());
                createLog.setLogLevel("INFO");
                createLog.setContent("回滚操作：创建表 [" + table.getTableName() + "]");
                createLog.setOperationType("CREATE_TABLE");
                createLog.setTableId(table.getId());
                createLog.setTableName(table.getTableName());
                createLog.setOperator(UserUtils.getCurrentUsername());
                createLog.setCreateTime(new Date());
                taskLogService.save(createLog);
            }
            
            // 5. 添加表注释(如果数据库支持)
            if (hasText(tableStructure.getDisplayName())) {
                try {
                    String tableCommentSql = dbHandler.getAddTableCommentSql(table.getTableName(), tableStructure.getDisplayName());
                    if (hasText(tableCommentSql)) {
                        try (Statement stmt = connection.createStatement()) {
                            stmt.execute(tableCommentSql);
                        }
                    }
                } catch (Exception e) {
                    log.warn("添加表注释失败（非致命错误）：{}", e.getMessage());
                }
            }
            
            // 6. 添加字段注释(如果数据库支持)
            for (FieldStructure field : tableStructure.getFields()) {
                String fieldCommentSql = dbHandler.getAddColumnCommentSql(table.getTableName(), field.getName(), field.getComment());
                if (hasText(fieldCommentSql)) {
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute(fieldCommentSql);
                    } catch (Exception e) {
                        log.warn("添加字段注释失败（非致命错误）：{}", e.getMessage());
                    }
                }
            }
            
            // 7. 根据配置加载数据 - 改进这部分，参考executeFullMaterialization方法
            if ("full".equals(table.getMaterializeType())) {
                // 根据表的来源类型决定如何加载数据
                String originTableId = table.getTableId();
                if (originTableId != null && !originTableId.isEmpty()) {
                    try {
                        // 记录数据加载开始日志
                        TaskLog startLoadLog = new TaskLog();
                        startLoadLog.setTaskId(task.getId());
                        startLoadLog.setVersionId(version.getId());
                        startLoadLog.setLogLevel("INFO");
                        startLoadLog.setContent("回滚操作：开始加载表数据 [" + table.getTableName() + "]");
                        startLoadLog.setOperationType("LOAD_DATA_START");
                        startLoadLog.setTableId(table.getId());
                        startLoadLog.setTableName(table.getTableName());
                        startLoadLog.setOperator(UserUtils.getCurrentUsername());
                        startLoadLog.setCreateTime(new Date());
                        taskLogService.save(startLoadLog);
                        
                        // 执行数据加载
                        loadTableData(task, table, connection, tableStructure);
                        
                        // 记录数据加载完成日志
                        TaskLog endLoadLog = new TaskLog();
                        endLoadLog.setTaskId(task.getId());
                        endLoadLog.setVersionId(version.getId());
                        endLoadLog.setLogLevel("INFO");
                        endLoadLog.setContent("回滚操作：表数据加载完成 [" + table.getTableName() + "]");
                        endLoadLog.setOperationType("LOAD_DATA_COMPLETE");
                        endLoadLog.setTableId(table.getId());
                        endLoadLog.setTableName(table.getTableName());
                        endLoadLog.setOperator(UserUtils.getCurrentUsername());
                        endLoadLog.setCreateTime(new Date());
                        taskLogService.save(endLoadLog);
                    } catch (Exception e) {
                        // 记录数据加载失败日志
                        TaskLog errorLog = new TaskLog();
                        errorLog.setTaskId(task.getId());
                        errorLog.setVersionId(version.getId());
                        errorLog.setLogLevel("ERROR");
                        errorLog.setContent("回滚操作：加载表数据失败 [" + table.getTableName() + "]: " + e.getMessage());
                        errorLog.setOperationType("LOAD_DATA_ERROR");
                        errorLog.setTableId(table.getId());
                        errorLog.setTableName(table.getTableName());
                        errorLog.setOperator(UserUtils.getCurrentUsername());
                        errorLog.setCreateTime(new Date());
                        taskLogService.save(errorLog);
                        
                        throw e; // 重新抛出异常，让上层处理
                    }
                }
            } else if ("incremental".equals(table.getMaterializeType())) {
                // 回滚时，增量模式也作为全量处理
                loadTableData(task, table, connection, tableStructure);
                
                // 记录数据加载日志
                TaskLog dataLog = new TaskLog();
                dataLog.setTaskId(task.getId());
                dataLog.setVersionId(version.getId());
                dataLog.setLogLevel("INFO");
                dataLog.setContent("回滚操作：加载增量表数据(以全量方式) [" + table.getTableName() + "]");
                dataLog.setOperationType("LOAD_DATA");
                dataLog.setTableId(table.getId());
                dataLog.setTableName(table.getTableName());
                dataLog.setOperator(UserUtils.getCurrentUsername());
                dataLog.setCreateTime(new Date());
                taskLogService.save(dataLog);
            }
            
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    log.error("关闭数据库连接失败", e);
                }
            }
        }
    }

    /**
     * 检查字符串是否有文本内容
     * @param str 要检查的字符串
     * @return 如果字符串不为null且包含至少一个非空白字符，则返回true
     */
    private boolean hasText(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * 根据数据源类型查询任务列表
     */
    @Override
    public List<MaterializeTask> listByDataSourceType(String dataSourceType) {
        if (StringUtils.isEmpty(dataSourceType)) {
            return new ArrayList<>();
        }
        
        LambdaQueryWrapper<MaterializeTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MaterializeTask::getDataSourceType, dataSourceType);
        
        return this.list(queryWrapper);
    }

}