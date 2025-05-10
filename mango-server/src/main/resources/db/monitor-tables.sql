-- 服务资源监控表
CREATE TABLE IF NOT EXISTS `dataserver_service_resource` (
  `id` varchar(36) NOT NULL COMMENT '主键ID',
  `service_id` varchar(36) DEFAULT NULL COMMENT '服务ID',
  `service_name` varchar(100) DEFAULT NULL COMMENT '服务名称',
  `cpu_usage` double DEFAULT NULL COMMENT 'CPU使用率',
  `memory_usage` double DEFAULT NULL COMMENT '内存使用率',
  `disk_usage` double DEFAULT NULL COMMENT '磁盘使用率',
  `disk_io` double DEFAULT NULL COMMENT '磁盘IO(MB/s)',
  `record_time` datetime DEFAULT NULL COMMENT '记录时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  KEY `idx_service_id` (`service_id`),
  KEY `idx_record_time` (`record_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务资源监控表';

-- 服务性能监控表
CREATE TABLE IF NOT EXISTS `dataserver_service_performance` (
  `id` varchar(36) NOT NULL COMMENT '主键ID',
  `service_id` varchar(36) DEFAULT NULL COMMENT '服务ID',
  `service_name` varchar(100) DEFAULT NULL COMMENT '服务名称',
  `response_time` double DEFAULT NULL COMMENT '平均响应时间(ms)',
  `request_count` int DEFAULT NULL COMMENT '请求数',
  `error_count` int DEFAULT NULL COMMENT '错误数',
  `error_rate` double DEFAULT NULL COMMENT '错误率',
  `record_time` datetime DEFAULT NULL COMMENT '记录时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  KEY `idx_service_id` (`service_id`),
  KEY `idx_record_time` (`record_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务性能监控表';

-- 初始化数据
INSERT INTO `dataserver_service_resource` (`id`, `service_id`, `service_name`, `cpu_usage`, `memory_usage`, `disk_usage`, `disk_io`, `record_time`, `create_time`)
VALUES (UUID(), 'default', '默认服务', 0, 0, 0, 0, NOW(), NOW());

INSERT INTO `dataserver_service_performance` (`id`, `service_id`, `service_name`, `response_time`, `request_count`, `error_count`, `error_rate`, `record_time`, `create_time`)
VALUES (UUID(), 'default', '默认服务', 0, 0, 0, 0, NOW(), NOW()); 