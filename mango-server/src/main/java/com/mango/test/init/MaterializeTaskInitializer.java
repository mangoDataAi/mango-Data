package com.mango.test.init;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mango.test.database.entity.MaterializeTask;
import com.mango.test.database.service.MaterializeTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 物化任务初始化器
 * 在应用启动时加载所有需要调度的物化任务
 */
@Slf4j
@Component
public class MaterializeTaskInitializer implements CommandLineRunner {

    @Autowired
    private MaterializeTaskService materializeTaskService;
    
    @Autowired(required = false)
    private ThreadPoolTaskScheduler taskScheduler;
    
    // 用于存储任务ID和对应的调度任务，便于后续管理
    private static final ConcurrentHashMap<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @Override
    public void run(String... args) throws Exception {
        log.info("开始初始化物化任务调度...");
        initializeScheduledTasks();
        log.info("物化任务调度初始化完成");
    }

    /**
     * 初始化所有需要调度的任务
     */
    private void initializeScheduledTasks() {
        if (taskScheduler == null) {
            log.error("任务调度器未配置，无法初始化物化任务调度");
            return;
        }
        
        try {
            // 查询所有未完成且需要调度的物化任务
            LambdaQueryWrapper<MaterializeTask> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.ne(MaterializeTask::getStatus, "COMPLETED")
                    .ne(MaterializeTask::getStatus, "FAILED")
                    .ne(MaterializeTask::getStatus, "CANCELLED")
                    .in(MaterializeTask::getScheduleType, "scheduled", "periodic");
            
            List<MaterializeTask> tasks = materializeTaskService.list(queryWrapper);
            log.info("找到{}个需要调度的物化任务", tasks.size());
            
            // 为每个物化任务创建调度
            for (MaterializeTask task : tasks) {
                scheduleTask(task);
            }
        } catch (Exception e) {
            log.error("初始化物化任务调度失败", e);
        }
    }
    
    /**
     * 为单个物化任务创建调度
     */
    private void scheduleTask(MaterializeTask task) {
        try {
            String taskId = task.getId();
            
            // 检查任务调度类型是否有效
            if (!"scheduled".equals(task.getScheduleType()) && !"periodic".equals(task.getScheduleType())) {
                log.warn("任务 {} 调度类型无效: {}", taskId, task.getScheduleType());
                return;
            }
            
            // 对于定时执行的任务，检查执行时间是否已过
            if ("scheduled".equals(task.getScheduleType()) && task.getScheduledTime() != null) {
                if (task.getScheduledTime().isBefore(LocalDateTime.now())) {
                    log.warn("任务 {} 计划执行时间已过: {}", taskId, task.getScheduledTime());
                    return;
                }
                
                // 将LocalDateTime转换为Date对象
                Date scheduledDate = java.util.Date.from(task.getScheduledTime().atZone(java.time.ZoneId.systemDefault()).toInstant());
                
                // 设置单次执行的触发器
                ScheduledFuture<?> future = taskScheduler.schedule(() -> {
                    try {
                        materializeTaskService.executeTask(taskId);
                    } catch (Exception e) {
                        log.error("执行任务失败: {}", taskId, e);
                    }
                }, scheduledDate);
                
                scheduledTasks.put(taskId, future);
                log.info("已调度单次物化任务: {}, 执行时间: {}", taskId, task.getScheduledTime());
            } 
            // 对于周期执行的任务，设置基于Cron表达式的触发器
            else if ("periodic".equals(task.getScheduleType()) && StringUtils.hasText(task.getCronExpression())) {
                // 设置基于Cron表达式的触发器
                CronTrigger trigger = new CronTrigger(task.getCronExpression());
                
                ScheduledFuture<?> future = taskScheduler.schedule(() -> {
                    try {
                        materializeTaskService.executeTask(taskId);
                    } catch (Exception e) {
                        log.error("执行任务失败: {}", taskId, e);
                    }
                }, trigger);
                
                scheduledTasks.put(taskId, future);
                log.info("已调度周期性物化任务: {}, Cron表达式: {}", taskId, task.getCronExpression());
            } else {
                log.warn("任务 {} 缺少必要的调度参数", taskId);
            }
        } catch (Exception e) {
            log.error("为物化任务创建调度失败: {}", task.getId(), e);
        }
    }
    
    /**
     * 获取已调度的任务Map
     * 便于任务管理和监控
     */
    public static ConcurrentHashMap<String, ScheduledFuture<?>> getScheduledTasks() {
        return scheduledTasks;
    }
    
    /**
     * 取消指定任务的调度
     * @param taskId 物化任务ID
     * @return 是否成功取消
     */
    public static boolean cancelTask(String taskId) {
        ScheduledFuture<?> future = scheduledTasks.remove(taskId);
        if (future != null) {
            future.cancel(false);
            return true;
        }
        return false;
    }
} 