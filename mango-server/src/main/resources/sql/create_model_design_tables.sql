-- 创建模型设计表
CREATE TABLE IF NOT EXISTS `data_model_design` (
  `id` varchar(32) NOT NULL COMMENT '主键ID',
  `model_id` varchar(32) NOT NULL COMMENT '关联的模型ID',
  `canvas` longtext COMMENT '画布数据（JSON格式）',
  `fields` longtext COMMENT '字段定义（JSON格式）',
  `indexes` longtext COMMENT '索引定义（JSON格式）',
  `relations` longtext COMMENT '关系定义（JSON格式）',
  `config` longtext COMMENT '其他配置信息（JSON格式）',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_model_design_model_id` (`model_id`),
  CONSTRAINT `fk_model_design_model_id` FOREIGN KEY (`model_id`) REFERENCES `data_model` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型设计表';

-- 创建模型标准关联表
CREATE TABLE IF NOT EXISTS `data_model_standard` (
  `id` varchar(32) NOT NULL COMMENT '主键ID',
  `model_id` varchar(32) NOT NULL COMMENT '关联的模型ID',
  `standard_id` varchar(32) NOT NULL COMMENT '关联的标准ID',
  `standard_type` varchar(50) DEFAULT NULL COMMENT '标准类型（字段标准、命名标准等）',
  `field_id` varchar(32) DEFAULT NULL COMMENT '关联的字段ID（如果是字段级别的标准）',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_model_standard_model_id` (`model_id`),
  KEY `idx_model_standard_standard_id` (`standard_id`),
  CONSTRAINT `fk_model_standard_model_id` FOREIGN KEY (`model_id`) REFERENCES `data_model` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型标准关联表'; 