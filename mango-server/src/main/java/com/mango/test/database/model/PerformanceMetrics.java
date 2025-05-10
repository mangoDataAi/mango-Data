package com.mango.test.database.model;

import lombok.Data;
import java.util.List;

@Data
public class PerformanceMetrics {
    private Double cpuUsage;              // CPU使用率
    private Double memoryUsage;           // 内存使用率
    private Long diskIoReads;             // 磁盘读取次数
    private Long diskIoWrites;            // 磁盘写入次数
    private Double queryResponseTime;      // 查询响应时间
    private Integer activeQueries;        // 活跃查询数
    private List<SlowQuery> slowQueries;  // 慢查询列表
    private List<String> errorLogs;       // 错误日志
    private DatabaseCache cacheStats;      // 缓存统计
    
    @Data
    public static class SlowQuery {
        private String sql;               // SQL语句
        private Long executionTime;       // 执行时间(毫秒)
        private String executeTime;       // 执行时间点
        private String user;              // 执行用户
    }
    
    @Data
    public static class DatabaseCache {
        private Long hitCount;            // 缓存命中次数
        private Long missCount;           // 缓存未命中次数
        private Double hitRatio;          // 缓存命中率
    }
} 