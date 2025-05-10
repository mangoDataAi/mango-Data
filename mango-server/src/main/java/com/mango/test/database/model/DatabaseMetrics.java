package com.mango.test.database.model;

import lombok.Data;

@Data
public class DatabaseMetrics {
    private Integer activeConnections;    // 活跃连接数
    private Integer idleConnections;      // 空闲连接数
    private Integer maxConnections;       // 最大连接数
    private Double connectionUsage;       // 连接使用率(%)
    private Long waitingThreads;          // 等待线程数
    private Long totalConnections;        // 总连接数
    private Long failedConnections;       // 失败连接数
    private Long uptime;                  // 运行时间(毫秒)
} 