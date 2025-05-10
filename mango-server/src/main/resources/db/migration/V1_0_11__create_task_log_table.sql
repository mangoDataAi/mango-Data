-- 创建任务日志表
CREATE TABLE IF NOT EXISTS `data_task_log` (
  `id` varchar(36) NOT NULL COMMENT '主键ID',
  `task_id` varchar(36) DEFAULT NULL COMMENT '任务ID',
  `version_id` varchar(36) DEFAULT NULL COMMENT '版本ID',
  `log_level` varchar(10) NOT NULL COMMENT '日志级别: INFO, WARN, ERROR, DEBUG',
  `content` varchar(500) NOT NULL COMMENT '日志内容',
  `operation_type` varchar(50) NOT NULL COMMENT '操作类型: CREATE, UPDATE, EXECUTE, CANCEL等',
  `operator` varchar(100) DEFAULT NULL COMMENT '操作人',
  `table_id` varchar(36) DEFAULT NULL COMMENT '相关表ID',
  `table_name` varchar(100) DEFAULT NULL COMMENT '相关表名称',
  `details` text DEFAULT NULL COMMENT '详细信息',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_version_id` (`version_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务日志表'; 