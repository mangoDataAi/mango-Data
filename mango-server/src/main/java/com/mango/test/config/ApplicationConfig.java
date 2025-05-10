package com.mango.test.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 应用程序配置类
 */
@Configuration
public class ApplicationConfig {

    /**
     * 创建任务调度器
     * @return ThreadPoolTaskScheduler实例
     */
    @Bean(name = "syncTaskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);                   // 设置线程池大小
        scheduler.setThreadNamePrefix("task-");     // 线程名前缀
        scheduler.setWaitForTasksToCompleteOnShutdown(true); // 优雅关闭
        scheduler.setAwaitTerminationSeconds(60);   // 等待终止的秒数
        scheduler.setErrorHandler(throwable -> {
            // 处理定时任务执行出错的情况
            System.err.println("定时任务执行异常: " + throwable.getMessage());
            throwable.printStackTrace();
        });
        scheduler.initialize();
        return scheduler;
    }
} 