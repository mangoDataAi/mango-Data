-- ----------------------------
-- 在mango模式下导入
-- ----------------------------
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for data_domain_dimension
-- ----------------------------
DROP TABLE IF EXISTS `data_domain_dimension`;
CREATE TABLE `data_domain_dimension`  (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '维度名称',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '维度类型',
  `source_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '来源类型',
  `domain_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所属域ID',
  `domain_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所属域名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '描述',
  `attribute_count` int NULL DEFAULT 0 COMMENT '属性数量',
  `update_strategy` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新策略',
  `last_update_time` datetime NULL DEFAULT NULL COMMENT '最近更新时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `delete_flg` int NULL DEFAULT 0 COMMENT '删除标志(0-未删除 1-已删除)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_dimension_domain_id`(`domain_id` ASC) USING BTREE,
  INDEX `idx_dimension_name`(`name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '维度定义表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of data_domain_dimension
-- ----------------------------

-- ----------------------------
-- Table structure for data_domain_dimension_attribute
-- ----------------------------
DROP TABLE IF EXISTS `data_domain_dimension_attribute`;
CREATE TABLE `data_domain_dimension_attribute`  (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `dimension_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '维度ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '属性名称',
  `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '属性编码',
  `data_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据类型',
  `length` int NULL DEFAULT NULL COMMENT '长度',
  `is_primary` tinyint(1) NULL DEFAULT 0 COMMENT '是否主键(0-否 1-是)',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '描述',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `delete_flg` int NULL DEFAULT 0 COMMENT '删除标志(0-未删除 1-已删除)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_attribute_dimension_id`(`dimension_id` ASC) USING BTREE,
  INDEX `idx_attribute_name`(`name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '维度属性表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of data_domain_dimension_attribute
-- ----------------------------

-- ----------------------------
-- Table structure for data_domain_dimension_standard
-- ----------------------------
DROP TABLE IF EXISTS `data_domain_dimension_standard`;
CREATE TABLE `data_domain_dimension_standard`  (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `dimension_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '维度ID',
  `standard_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标准ID',
  `standard_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标准类型',
  `priority` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '优先级',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '描述',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `delete_flg` int NULL DEFAULT 0 COMMENT '删除标志(0-未删除 1-已删除)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_standard_dimension_id`(`dimension_id` ASC) USING BTREE,
  INDEX `idx_standard_standard_id`(`standard_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '维度标准关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of data_domain_dimension_standard
-- ----------------------------

-- ----------------------------
-- Table structure for data_domain_domain
-- ----------------------------
DROP TABLE IF EXISTS `data_domain_domain`;
CREATE TABLE `data_domain_domain`  (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主题域名称',
  `source_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据源类型(delta/starrocks/platform)',
  `data_source_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据源ID',
  `owner` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '负责人',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'enabled' COMMENT '状态(enabled/disabled)',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '描述',
  `parent_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '父级域ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `delete_flg` int NULL DEFAULT 0 COMMENT '删除标志(0-未删除 1-已删除)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_domain_parent`(`parent_id` ASC) USING BTREE,
  INDEX `idx_domain_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '数据域定义表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of data_domain_domain
-- ----------------------------

-- ----------------------------
-- Table structure for data_materialize_task
-- ----------------------------
DROP TABLE IF EXISTS `data_materialize_task`;
CREATE TABLE `data_materialize_task`  (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `task_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '任务描述',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '任务状态：WAITING, RUNNING, COMPLETED, FAILED, CANCELLED, SCHEDULED, PARTIAL',
  `schedule_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '调度类型：immediate, scheduled, periodic',
  `scheduled_time` datetime NULL DEFAULT NULL COMMENT '定时执行时间',
  `cron_expression` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'Cron表达式，用于周期执行',
  `environment` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '发布环境：dev, test, staging, prod',
  `materialize_strategy` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '物化策略：complete, table_incremental',
  `data_source_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据源类型：theme, custom',
  `domain_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '主题域ID',
  `domain_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '主题域名称',
  `custom_data_source_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '自定义数据源ID',
  `custom_data_source_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '自定义数据源名称',
  `refresh_method` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '刷新方式：rebuild, update',
  `execution_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '执行模式：serial, parallel, dependency',
  `error_handling` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '错误处理：stop, continue, log',
  `materialize_order` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '物化顺序：auto, defined, parallel',
  `parallelism` int NULL DEFAULT NULL COMMENT '并行度',
  `timeout` int NULL DEFAULT NULL COMMENT '超时时间(分钟)',
  `progress` int NULL DEFAULT NULL COMMENT '进度(0-100)',
  `start_time` datetime NULL DEFAULT NULL COMMENT '开始执行时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '结束执行时间',
  `creator` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `model_ids` json NULL COMMENT '关联的模型IDs',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '错误消息',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `delete_flg` int NULL DEFAULT 0 COMMENT '删除标志(0-未删除 1-已删除)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_materialize_task_status`(`status` ASC) USING BTREE,
  INDEX `idx_materialize_task_domain`(`domain_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '物化任务表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of data_materialize_task
-- ----------------------------

-- ----------------------------
-- Table structure for data_model
-- ----------------------------
DROP TABLE IF EXISTS `data_model`;
CREATE TABLE `data_model`  (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模型名称',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '模型类型（summary:汇总模型, detail:明细模型）',
  `owner` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '负责人',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '状态（draft:草稿, published:已发布, running:运行中, failed:运行失败）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '模型描述',
  `config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '模型配置（JSON格式）',
  `tags` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标签，多个标签用逗号分隔',
  `version` int NULL DEFAULT 1 COMMENT '版本号',
  `applied_standard_ids` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '已应用的标准IDs，多个标准ID用逗号分隔',
  `standard_apply_scope` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '应用标准的范围（all:所有字段, selected:选中字段）',
  `standard_apply_description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '应用标准的说明',
  `standard_apply_time` datetime NULL DEFAULT NULL COMMENT '应用标准的时间',
  `standard_apply_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '应用标准的用户',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `delete_flg` int NULL DEFAULT 0 COMMENT '删除标志(0-未删除 1-已删除)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_model_name`(`name` ASC) USING BTREE,
  INDEX `idx_model_type`(`type` ASC) USING BTREE,
  INDEX `idx_model_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '模型表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of data_model
-- ----------------------------

-- ----------------------------
-- Table structure for data_model_design
-- ----------------------------
DROP TABLE IF EXISTS `data_model_design`;
CREATE TABLE `data_model_design`  (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `model_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '关联的模型ID',
  `canvas` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '画布数据（JSON格式）',
  `fields` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '字段定义（JSON格式）',
  `indexes` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '索引定义（JSON格式）',
  `relations` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '关系定义（JSON格式）',
  `config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '其他配置信息（JSON格式）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `delete_flg` int NULL DEFAULT 0 COMMENT '删除标志(0-未删除 1-已删除)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_model_design_model_id`(`model_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '模型设计表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of data_model_design
-- ----------------------------

-- ----------------------------
-- Table structure for data_model_standard
-- ----------------------------
DROP TABLE IF EXISTS `data_model_standard`;
CREATE TABLE `data_model_standard`  (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `model_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模型ID',
  `standard_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标准ID',
  `standard_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标准名称',
  `standard_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标准类型（required:必须执行, suggested:建议执行）',
  `rule_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标准规则类型',
  `apply_scope` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '应用范围（all:所有字段, selected:选中字段）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '应用说明',
  `apply_time` datetime NULL DEFAULT NULL COMMENT '应用时间',
  `apply_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '应用用户',
  `status` int NULL DEFAULT 1 COMMENT '状态（1:有效, 0:无效）',
  `field_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字段ID（当应用范围为selected时使用）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `delete_flg` int NULL DEFAULT 0 COMMENT '删除标志(0-未删除 1-已删除)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_model_standard_model_id`(`model_id` ASC) USING BTREE,
  INDEX `idx_model_standard_standard_id`(`standard_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '模型-标准关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of data_model_standard
-- ----------------------------

-- ----------------------------
-- Table structure for sys_api_datasource
-- ----------------------------
DROP TABLE IF EXISTS `sys_api_datasource`;
CREATE TABLE `sys_api_datasource`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '数据源名称',
  `method` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求方法',
  `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求URL',
  `headers` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '请求头信息',
  `request_body` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '请求体',
  `update_interval` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新间隔',
  `success_condition` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '成功条件',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述信息',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '状态',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `delete_flg` int NULL DEFAULT NULL COMMENT '删除标志',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'API数据源表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_api_datasource
-- ----------------------------

-- ----------------------------
-- Table structure for sys_data_file_field
-- ----------------------------
DROP TABLE IF EXISTS `sys_data_file_field`;
CREATE TABLE `sys_data_file_field`  (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `source_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据源ID',
  `field_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字段名',
  `field_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字段类型',
  `field_length` int NULL DEFAULT NULL COMMENT '字段长度',
  `precision` int NULL DEFAULT NULL COMMENT '精度',
  `scale` int NULL DEFAULT NULL COMMENT '小数位数',
  `nullable` tinyint(1) NULL DEFAULT 1 COMMENT '是否为空',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字段描述',
  `order_num` int NULL DEFAULT 0 COMMENT '排序号',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `delete_flg` int NULL DEFAULT 0 COMMENT '删除标志(0-未删除 1-已删除)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_file_field_source_id`(`source_id` ASC) USING BTREE,
  INDEX `idx_file_field_name`(`field_name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '文件字段信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_data_file_field
-- ----------------------------

-- ----------------------------
-- Table structure for sys_datasource
-- ----------------------------
DROP TABLE IF EXISTS `sys_datasource`;
CREATE TABLE `sys_datasource`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据源名称',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据源类型',
  `db_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '数据库名称',
  `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '数据库连接URL',
  `host` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '数据库主机地址',
  `port` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '数据库端口号',
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '数据库用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '数据库密码',
  `initial_size` int NULL DEFAULT NULL COMMENT '连接池初始化大小',
  `max_active` int NULL DEFAULT NULL COMMENT '连接池最大活跃连接数',
  `min_idle` int NULL DEFAULT NULL COMMENT '连接池最小空闲连接数',
  `max_wait` int NULL DEFAULT NULL COMMENT '连接池获取连接等待超时时间(毫秒)',
  `validation_query` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '验证查询SQL',
  `test_on_borrow` tinyint(1) NULL DEFAULT NULL COMMENT '申请连接时执行validationQuery检测连接是否有效',
  `test_on_return` tinyint(1) NULL DEFAULT NULL COMMENT '归还连接时执行validationQuery检测连接是否有效',
  `test_while_idle` tinyint(1) NULL DEFAULT NULL COMMENT '空闲时执行validationQuery检测连接是否有效',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '数据源状态: 1-启用, 0-禁用',
  `path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '本地文件系统路径',
  `bucket` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '云存储桶名称',
  `access_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '访问密钥标识',
  `secret_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '访问密钥密码',
  `region` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '云服务区域',
  `endpoint` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '服务终端节点',
  `namenode` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'HDFS命名空间',
  `properties` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `connection_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '连接类型: SID或SERVICE_NAME (主要用于Oracle连接)',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `delete_flg` int NULL DEFAULT 0 COMMENT '删除标志(0:未删除,1:已删除)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_datasource_delete`(`delete_flg` ASC) USING BTREE,
  INDEX `idx_datasource_name`(`name` ASC) USING BTREE,
  INDEX `idx_datasource_status`(`status` ASC) USING BTREE,
  INDEX `idx_datasource_type`(`type` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '数据源表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_datasource
-- ----------------------------

-- ----------------------------
-- Table structure for sys_file_data_source
-- ----------------------------
DROP TABLE IF EXISTS `sys_file_data_source`;
CREATE TABLE `sys_file_data_source`  (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件名',
  `file_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件类型',
  `file_size` bigint NULL DEFAULT NULL COMMENT '文件大小(字节)',
  `file_path` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件路径',
  `table_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '表名',
  `field_count` int NULL DEFAULT 0 COMMENT '字段数量',
  `data_count` int NULL DEFAULT 0 COMMENT '数据量',
  `status` int NULL DEFAULT 0 COMMENT '状态(0:未解析,1:已解析,2:解析失败)',
  `error_msg` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '错误信息',
  `table_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '表注释',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `delete_flg` int NULL DEFAULT 0 COMMENT '删除标志(0-未删除 1-已删除)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_file_ds_file_name`(`file_name` ASC) USING BTREE,
  INDEX `idx_file_ds_table_name`(`table_name` ASC) USING BTREE,
  INDEX `idx_file_ds_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '文件数据源表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_file_data_source
-- ----------------------------

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜单ID',
  `parent_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '父菜单ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜单名称',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜单类型（DIR目录，MENU菜单）',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '路由地址',
  `component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '组件路径',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '菜单图标',
  `sort` int NULL DEFAULT 0 COMMENT '显示顺序',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '菜单状态（1正常 0停用）',
  `creator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `updater` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `is_external` tinyint(1) NULL DEFAULT 0 COMMENT '是否为外链（1是 0否）',
  `is_cache` tinyint(1) NULL DEFAULT 0 COMMENT '是否缓存（1是 0否）',
  `is_visible` tinyint(1) NULL DEFAULT 1 COMMENT '是否可见（1是 0否）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sys_menu_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_sys_menu_sort`(`sort` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统菜单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES ('10000', '0', '数据源管理', 'DIR', '/dataSource', 'NULL', 'Connection', 1, 1, 'admin', '2025-05-03 10:59:50', 'NULL', '2025-05-03 10:59:50', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('100000', '0', '系统设置', 'DIR', '/opsManage', 'NULL', 'Setting', 10, 1, 'admin', '2025-05-03 10:59:52', 'NULL', '2025-05-03 10:59:52', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('101000', '100000', '基础设置', 'MENU', '/system/base', 'system/base/index', 'Tools', 1, 1, 'admin', '2025-05-03 10:59:52', 'NULL', '2025-05-03 10:59:52', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('101100', '101000', '用户管理', 'MENU', '/system/base/user', 'system/base/user/index', 'User', 1, 1, 'admin', '2025-05-03 10:59:52', 'NULL', '2025-05-03 10:59:52', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('101101', '101100', '用户查询', 'BUTTON', 'NULL', 'NULL', 'NULL', 1, 1, 'admin', '2025-05-03 11:00:21', 'NULL', '2025-05-03 11:00:21', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('101102', '101100', '用户新增', 'BUTTON', 'NULL', 'NULL', 'NULL', 2, 1, 'admin', '2025-05-03 11:00:21', 'NULL', '2025-05-03 11:00:21', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('101103', '101100', '用户修改', 'BUTTON', 'NULL', 'NULL', 'NULL', 3, 1, 'admin', '2025-05-03 11:00:21', 'NULL', '2025-05-03 11:00:21', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('101104', '101100', '用户删除', 'BUTTON', 'NULL', 'NULL', 'NULL', 4, 1, 'admin', '2025-05-03 11:00:21', 'NULL', '2025-05-03 11:00:21', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('101105', '101100', '重置密码', 'BUTTON', 'NULL', 'NULL', 'NULL', 5, 1, 'admin', '2025-05-03 11:00:21', 'NULL', '2025-05-03 11:00:21', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('101200', '101000', '机构管理', 'MENU', '/system/base/organization', 'system/base/organization/index', 'SetUp', 2, 1, 'admin', '2025-05-03 10:59:52', 'NULL', '2025-05-03 10:59:52', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('101201', '101200', '机构查询', 'BUTTON', 'NULL', 'NULL', 'NULL', 1, 1, 'admin', '2025-05-03 11:00:21', 'NULL', '2025-05-03 11:00:21', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('101202', '101200', '机构新增', 'BUTTON', 'NULL', 'NULL', 'NULL', 2, 1, 'admin', '2025-05-03 11:00:21', 'NULL', '2025-05-03 11:00:21', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('101203', '101200', '机构修改', 'BUTTON', 'NULL', 'NULL', 'NULL', 3, 1, 'admin', '2025-05-03 11:00:21', 'NULL', '2025-05-03 11:00:21', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('101204', '101200', '机构删除', 'BUTTON', 'NULL', 'NULL', 'NULL', 4, 1, 'admin', '2025-05-03 11:00:21', 'NULL', '2025-05-03 11:00:21', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('101300', '101000', '菜单管理', 'MENU', '/system/base/menu', 'system/base/menu/index', 'Menu', 3, 1, 'admin', '2025-05-03 11:00:21', 'NULL', '2025-05-03 11:00:21', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('101301', '101300', '菜单查询', 'BUTTON', 'NULL', 'NULL', 'NULL', 1, 1, 'admin', '2025-05-03 11:00:21', 'NULL', '2025-05-03 11:00:21', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('101302', '101300', '菜单新增', 'BUTTON', 'NULL', 'NULL', 'NULL', 2, 1, 'admin', '2025-05-03 11:00:21', 'NULL', '2025-05-03 11:00:21', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('101303', '101300', '菜单修改', 'BUTTON', 'NULL', 'NULL', 'NULL', 3, 1, 'admin', '2025-05-03 11:00:21', 'NULL', '2025-05-03 11:00:21', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('101304', '101300', '菜单删除', 'BUTTON', 'NULL', 'NULL', 'NULL', 4, 1, 'admin', '2025-05-03 11:00:21', 'NULL', '2025-05-03 11:00:21', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('101400', '101000', '角色权限', 'MENU', '/system/base/role', 'system/base/role/index', 'Lock', 4, 1, 'admin', '2025-05-03 11:00:21', 'NULL', '2025-05-03 11:00:21', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('101401', '101400', '角色查询', 'BUTTON', 'NULL', 'NULL', 'NULL', 1, 1, 'admin', '2025-05-03 11:00:21', 'NULL', '2025-05-03 11:00:21', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('101402', '101400', '角色新增', 'BUTTON', 'NULL', 'NULL', 'NULL', 2, 1, 'admin', '2025-05-03 11:00:21', 'NULL', '2025-05-03 11:00:21', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('101403', '101400', '角色修改', 'BUTTON', 'NULL', 'NULL', 'NULL', 3, 1, 'admin', '2025-05-03 11:00:21', 'NULL', '2025-05-03 11:00:21', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('101404', '101400', '角色删除', 'BUTTON', 'NULL', 'NULL', 'NULL', 4, 1, 'admin', '2025-05-03 11:00:21', 'NULL', '2025-05-03 11:00:21', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('101405', '101400', '分配权限', 'BUTTON', 'NULL', 'NULL', 'NULL', 5, 1, 'admin', '2025-05-03 11:00:22', 'NULL', '2025-05-03 11:00:22', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('101406', '101400', '数据权限', 'BUTTON', 'NULL', 'NULL', 'NULL', 6, 1, 'admin', '2025-05-03 11:00:22', 'NULL', '2025-05-03 11:00:22', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('11000', '10000', '数据源管理', 'MENU', '/dataIntegration/source', 'dataIntegration/source/index', 'Files', 1, 1, 'admin', '2025-05-03 10:59:50', 'NULL', '2025-05-03 10:59:50', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('11100', '11000', '数据库连接池', 'MENU', '/dataIntegration/source/database', 'dataIntegration/source/database/index', 'Connection', 1, 1, 'admin', '2025-05-03 10:59:50', 'NULL', '2025-05-03 10:59:50', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('11200', '11000', '文件数据源', 'MENU', '/dataIntegration/source/file', 'dataIntegration/source/file/index', 'Document', 2, 1, 'admin', '2025-05-03 10:59:50', 'NULL', '2025-05-03 10:59:50', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('11300', '11000', '接口数据源', 'MENU', '/dataIntegration/source/api', 'dataIntegration/source/api/index', 'Link', 3, 1, 'admin', '2025-05-03 10:59:50', 'NULL', '2025-05-03 10:59:50', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('20000', '0', '数据集成', 'DIR', '/dataIntegration', 'NULL', 'Connection', 2, 1, 'admin', '2025-05-03 10:59:50', 'NULL', '2025-05-03 10:59:50', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('21000', '20000', '模型设计', 'MENU', '/dataIntegration/model', 'dataIntegration/model/index', 'Grid', 1, 1, 'admin', '2025-05-03 10:59:50', 'NULL', '2025-05-03 10:59:50', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('21400', '21000', '模型设计器', 'MENU', '/dataIntegration/model/model', 'dataIntegration/model/model/index', 'Edit', 4, 1, 'admin', '2025-05-03 10:59:50', 'NULL', '2025-05-03 10:59:50', 'NULL', 0, 0, 1);
INSERT INTO `sys_menu` VALUES ('21500', '21000', '模型发布', 'MENU', '/dataIntegration/model/publish', 'dataIntegration/model/publish/index', 'Upload', 5, 1, 'admin', '2025-05-03 10:59:50', 'NULL', '2025-05-03 10:59:50', 'NULL', 0, 0, 1);

-- ----------------------------
-- Table structure for sys_organization
-- ----------------------------
DROP TABLE IF EXISTS `sys_organization`;
CREATE TABLE `sys_organization`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键ID',
  `parent_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '父级ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '机构名称',
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '机构编码',
  `type` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '机构类型（1-公司，2-部门，3-小组，4-其他）',
  `leader` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '负责人',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `sort` int NULL DEFAULT 0 COMMENT '排序号',
  `status` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '1' COMMENT '状态（1-正常，0-禁用）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `creator` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `updater` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_org_code`(`code` ASC) USING BTREE,
  INDEX `idx_sys_org_code`(`code` ASC) USING BTREE,
  INDEX `idx_sys_org_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_sys_org_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '机构表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_organization
-- ----------------------------
INSERT INTO `sys_organization` VALUES ('1', '0', '总公司', 'HQ', '1', '张三', '13800138000', 1, '1', '总部机构', '2025-04-26 01:47:19', '2025-04-26 01:47:19', 'admin', 'admin');

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
  `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色编码',
  `color` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '角色颜色',
  `sort` int NULL DEFAULT 0 COMMENT '显示顺序',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '角色状态（1正常 0停用）',
  `data_scope` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'ALL' COMMENT '数据权限范围',
  `creator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `updater` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sys_role_code`(`code` ASC) USING BTREE,
  INDEX `idx_sys_role_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES ('1', '超级管理员', 'ADMIN', '#1890FF', 1, 0, 'ALL', 'admin', '2025-04-26 01:50:02', 'admin', '2025-05-03 09:42:48', '超级管理员');
INSERT INTO `sys_role` VALUES ('2', '普通用户', 'USER', '#52C41A', 2, 1, 'SELF', 'admin', '2025-04-26 01:50:02', 'admin', '2025-04-26 01:50:02', '普通用户');

-- ----------------------------
-- Table structure for sys_role_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键ID',
  `role_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色ID',
  `dept_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sys_role_dept_dept_id`(`dept_id` ASC) USING BTREE,
  INDEX `idx_sys_role_dept_role_id`(`role_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色和部门关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role_dept
-- ----------------------------

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键ID',
  `role_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色ID',
  `menu_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sys_role_menu_menu_id`(`menu_id` ASC) USING BTREE,
  INDEX `idx_sys_role_menu_role_id`(`role_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色和菜单关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES ('02d7aab8-0742-4980-a3ac-031955414c2b', '1', '31200');
INSERT INTO `sys_role_menu` VALUES ('033ce0d6-7abe-47d1-9a36-eca0af6ddc02', '1', '41300');
INSERT INTO `sys_role_menu` VALUES ('048d4de7-4c45-4bb9-bb4b-2875efe822a4', '1', '21100');
INSERT INTO `sys_role_menu` VALUES ('04ae521c-eb42-40d0-b799-d4b50abdbb52', '1', '101103');
INSERT INTO `sys_role_menu` VALUES ('0509ff3a-2bda-4a8d-b831-2ebd77402912', '1', '101101');
INSERT INTO `sys_role_menu` VALUES ('06559183-beed-4325-9527-9f393f44aba9', '1', '100000');
INSERT INTO `sys_role_menu` VALUES ('0c230ad6-cb66-4636-bab5-86b56c9ed4f8', '1', '21300');
INSERT INTO `sys_role_menu` VALUES ('0c65b69f-1427-406a-b12f-b69e0b601376', '1', '51100');
INSERT INTO `sys_role_menu` VALUES ('0dfb74f4-be3a-4092-8cc6-b01bbf6585b1', '1', '61101');
INSERT INTO `sys_role_menu` VALUES ('0e3e39a0-3353-4ded-8ce9-8f4c0549d80b', '1', '31700');
INSERT INTO `sys_role_menu` VALUES ('0ecbdc0c-556c-483a-b7e3-f6700190d61a', '1', '101200');
INSERT INTO `sys_role_menu` VALUES ('0f4ef67c-d619-4691-8f6d-d61559b6afc0', '1', '52000');
INSERT INTO `sys_role_menu` VALUES ('101bb2fa-3677-4ce6-a451-e63fc7d76399', '2', '71000');
INSERT INTO `sys_role_menu` VALUES ('13ccf825-4829-4cd0-80e4-d23a56aa4cd6', '1', '101302');
INSERT INTO `sys_role_menu` VALUES ('1a5e833e-5c07-4f51-9a91-c0d45c5cad42', '1', '31000');
INSERT INTO `sys_role_menu` VALUES ('1ac4c691-c906-4998-8666-3719b10fc908', '2', '22400');
INSERT INTO `sys_role_menu` VALUES ('1e490594-dc41-4df8-b0bc-cc625485bf62', '1', '53600');
INSERT INTO `sys_role_menu` VALUES ('20158345-3ac4-4c21-8c1c-b67e1943eb9f', '1', '42300');
INSERT INTO `sys_role_menu` VALUES ('2a20425b-25df-4406-9212-4f3d79296e16', '1', '90000');
INSERT INTO `sys_role_menu` VALUES ('2da40aad-32b7-4ecc-8bc8-7346afe13f58', '2', '10000');
INSERT INTO `sys_role_menu` VALUES ('2e0b5361-794d-4ee4-9495-d07274bdff3f', '1', '101301');
INSERT INTO `sys_role_menu` VALUES ('31432191-8324-4d54-bc6b-d45da4619cb9', '2', '22000');
INSERT INTO `sys_role_menu` VALUES ('31ef674b-a810-47b6-a62e-f6f2ed345d29', '1', '101000');
INSERT INTO `sys_role_menu` VALUES ('3618130b-d196-4d99-9a93-5895761f92c1', '1', '53400');
INSERT INTO `sys_role_menu` VALUES ('385e25ff-19cd-41f4-afaa-dc352476dc3a', '1', '11200');
INSERT INTO `sys_role_menu` VALUES ('3a3e64c7-ff8e-4645-8390-aea20dba0991', '2', '21000');
INSERT INTO `sys_role_menu` VALUES ('3a8b2cbc-4198-4526-a87d-7ba890d871fc', '2', '71101');
INSERT INTO `sys_role_menu` VALUES ('3b4910aa-ba5f-4f25-91ba-051987a018ab', '1', '101402');
INSERT INTO `sys_role_menu` VALUES ('40e40a96-effe-4ecf-9176-7c2dd4cf33b4', '1', '61100');
INSERT INTO `sys_role_menu` VALUES ('41590e0a-b1fd-4a7f-9518-06806035a471', '2', '20000');
INSERT INTO `sys_role_menu` VALUES ('42140ee7-d130-4895-9fa2-b8ad34be7e17', '1', '82200');
INSERT INTO `sys_role_menu` VALUES ('499b991d-4356-4bf3-a38c-d7c7dffb1c54', '1', '101303');
INSERT INTO `sys_role_menu` VALUES ('4a139443-db0e-4d68-8a2a-a3d1592e801a', '1', '101404');
INSERT INTO `sys_role_menu` VALUES ('4d736838-7f53-4029-89ce-645a44292838', '2', '22300');
INSERT INTO `sys_role_menu` VALUES ('4ef2309b-461e-4b0c-be32-43cc477af148', '2', '70000');
INSERT INTO `sys_role_menu` VALUES ('5118f815-226a-4481-9af9-ab49810adacd', '1', '42200');
INSERT INTO `sys_role_menu` VALUES ('535e99d6-a286-4aad-bef7-2fbeb9d199d3', '2', '21500');
INSERT INTO `sys_role_menu` VALUES ('539c41c0-a4b1-4c6a-b283-782220b77448', '1', '53500');
INSERT INTO `sys_role_menu` VALUES ('54e213f7-9c71-405c-9bf3-26c7d13b6f1b', '1', '41400');
INSERT INTO `sys_role_menu` VALUES ('553644e5-bce8-4532-9e80-b77e249db120', '1', '61300');
INSERT INTO `sys_role_menu` VALUES ('56638f7c-2c1b-4aaa-b6ea-9bf48e2dfa0a', '1', '101203');
INSERT INTO `sys_role_menu` VALUES ('58111e75-a06c-4637-87eb-426a5b917367', '1', '91000');
INSERT INTO `sys_role_menu` VALUES ('5bf74470-3b26-4fc3-9bf6-d53f3da9d04a', '1', '51400');
INSERT INTO `sys_role_menu` VALUES ('5d8de106-caaa-4a72-8ba4-e4dc4e97bb25', '2', '71300');
INSERT INTO `sys_role_menu` VALUES ('5f0d3dbb-4954-4011-9af7-a95250939c30', '1', '31100');
INSERT INTO `sys_role_menu` VALUES ('5fd2ead7-9ff4-491f-ae6a-cc91c0aab900', '1', '30000');
INSERT INTO `sys_role_menu` VALUES ('62dfa8d6-9358-4690-a1ed-0d359db35658', '1', '101100');
INSERT INTO `sys_role_menu` VALUES ('62e8b813-2f21-45eb-8e0c-fdbe4bde0f11', '2', '81200');
INSERT INTO `sys_role_menu` VALUES ('65717291-e36b-4e0e-9eeb-68ead40f2e58', '2', '11300');
INSERT INTO `sys_role_menu` VALUES ('67f54108-5102-4a2a-9415-2367f519bf07', '1', '61400');
INSERT INTO `sys_role_menu` VALUES ('6942eb14-6128-4d19-99e3-3e4dcb10e2e0', '1', '101104');
INSERT INTO `sys_role_menu` VALUES ('6b3a670c-8923-49ca-a087-7de2c69c2748', '1', '61103');
INSERT INTO `sys_role_menu` VALUES ('6d4e3149-b364-4cca-be47-69536864c5f3', '1', '21000');
INSERT INTO `sys_role_menu` VALUES ('6f5d1472-8433-4130-a0a4-c40d9bd40040', '1', '31600');
INSERT INTO `sys_role_menu` VALUES ('75a055c2-cf05-4733-8ed9-4b37bf11a974', '1', '51200');
INSERT INTO `sys_role_menu` VALUES ('75f9ca95-1c82-4799-bd41-afb7e72784b2', '2', '71500');
INSERT INTO `sys_role_menu` VALUES ('77b25797-02ec-442a-9052-4ca84a4e6c5b', '1', '42000');
INSERT INTO `sys_role_menu` VALUES ('7830819b-cf71-446d-aba1-60a0e6ef44cc', '1', '11100');
INSERT INTO `sys_role_menu` VALUES ('7c4e43b0-b39f-4509-a925-c9529eb57d93', '2', '22200');
INSERT INTO `sys_role_menu` VALUES ('7d14378a-c132-466e-ac60-4eec23c6d4d4', '1', '101201');
INSERT INTO `sys_role_menu` VALUES ('828cc36d-30aa-441f-8884-474c3807927b', '1', '101105');
INSERT INTO `sys_role_menu` VALUES ('82e20240-12fc-4c24-af5e-d969f60136ba', '2', '21200');
INSERT INTO `sys_role_menu` VALUES ('852a61d3-e73b-4404-bc8c-1405f25a1754', '1', '22400');
INSERT INTO `sys_role_menu` VALUES ('858d6004-d5af-48d8-9cdc-d5c274d18fcf', '1', '53100');
INSERT INTO `sys_role_menu` VALUES ('86b6992c-ff10-459f-a30f-e81804e0c7ee', '1', '11000');
INSERT INTO `sys_role_menu` VALUES ('86d12e33-24e0-4e0c-8664-505a15952f39', '1', '53800');
INSERT INTO `sys_role_menu` VALUES ('8747a28a-0306-4c89-8249-0f7a5f0da0ce', '2', '11000');
INSERT INTO `sys_role_menu` VALUES ('89607e2c-53b9-421a-84c4-b23a2ea7c39d', '1', '22300');
INSERT INTO `sys_role_menu` VALUES ('89fcd7b0-b963-4e71-8331-e5d619b51298', '2', '81100');
INSERT INTO `sys_role_menu` VALUES ('8b43badf-dbac-4980-b66d-d782ca121dea', '1', '41000');
INSERT INTO `sys_role_menu` VALUES ('8bc5f674-ad26-4d0e-b380-e058a82954a7', '1', '101300');
INSERT INTO `sys_role_menu` VALUES ('8d227201-5cef-4787-8995-16c17bb71243', '1', '20000');
INSERT INTO `sys_role_menu` VALUES ('8ecbc9a2-91d7-4715-88d4-0e1de2473abb', '1', '101102');
INSERT INTO `sys_role_menu` VALUES ('95e74e7c-df08-4f8f-b64f-f1a6b2222dac', '1', '22100');
INSERT INTO `sys_role_menu` VALUES ('965de445-f9e3-4cba-ba13-370f1071baf1', '1', '101401');
INSERT INTO `sys_role_menu` VALUES ('974b2d76-7b69-499a-aec8-6c0f39c2c4cf', '1', '101406');
INSERT INTO `sys_role_menu` VALUES ('97556002-b0d0-45b3-9ba1-2518075445e2', '1', '31300');
INSERT INTO `sys_role_menu` VALUES ('981b3eb9-8b75-4614-8b8d-90d11a3d84be', '2', '11100');
INSERT INTO `sys_role_menu` VALUES ('995c90ec-5e31-424e-b2c6-da75d82d79a1', '1', '40000');
INSERT INTO `sys_role_menu` VALUES ('9c7e425e-3b14-4a21-907a-6ea94fb52c45', '2', '71100');
INSERT INTO `sys_role_menu` VALUES ('9df862ec-bd09-4413-8f76-6d2c693d2b89', '1', '101202');
INSERT INTO `sys_role_menu` VALUES ('a0558623-b118-424f-a56c-8f5a1f6d955b', '1', '42400');
INSERT INTO `sys_role_menu` VALUES ('a0ef0c10-4f17-4621-9329-d5b6a65f9441', '1', '101400');
INSERT INTO `sys_role_menu` VALUES ('a4fd15c1-7ad1-4640-9064-2b0f64ba6813', '1', '10000');
INSERT INTO `sys_role_menu` VALUES ('a54ce77a-8257-42f0-8458-d568cc60e07f', '1', '50000');
INSERT INTO `sys_role_menu` VALUES ('a5703d35-eb22-4519-ac7a-80984dbba496', '1', '62100');
INSERT INTO `sys_role_menu` VALUES ('a59b0fae-bbfd-409d-accf-19437d7112e1', '1', '52100');
INSERT INTO `sys_role_menu` VALUES ('a84ab72e-09bc-4a85-ad8f-f1f0fff24d92', '2', '80000');
INSERT INTO `sys_role_menu` VALUES ('ab054ea2-8d35-4097-9ec1-efeed25964b8', '2', '81300');
INSERT INTO `sys_role_menu` VALUES ('ab19f914-c52e-4975-ac6a-16049594d7ed', '2', '21300');
INSERT INTO `sys_role_menu` VALUES ('ac06c2c0-ffc5-47cb-83b5-7db411351cad', '1', '21500');
INSERT INTO `sys_role_menu` VALUES ('ad5c588a-4267-41c8-b35d-103a62e5617e', '2', '81000');
INSERT INTO `sys_role_menu` VALUES ('ade21d33-2ee2-4a8e-80ae-1cef1d76dfff', '1', '41200');
INSERT INTO `sys_role_menu` VALUES ('af9a60fc-a148-42ee-bf30-f98da81dd6de', '1', '82100');
INSERT INTO `sys_role_menu` VALUES ('b0fd2abf-acdb-49e6-9109-940697c706ab', '1', '22200');
INSERT INTO `sys_role_menu` VALUES ('b17f6db2-7f12-4293-ba94-e577f3f3a948', '1', '61000');
INSERT INTO `sys_role_menu` VALUES ('b3a8d7b7-51b3-4eb9-9f4e-2b36f12045db', '1', '62200');
INSERT INTO `sys_role_menu` VALUES ('b3ee614c-012f-44e1-9129-64ab74211a33', '1', '31800');
INSERT INTO `sys_role_menu` VALUES ('b49eb5c1-c0cf-4af0-b6ff-398ec4cb899d', '1', '101405');
INSERT INTO `sys_role_menu` VALUES ('b4aeadad-897d-403f-8ff1-c9513c765a9b', '1', '101500');
INSERT INTO `sys_role_menu` VALUES ('b55ea357-3db6-47ab-a356-382a78722835', '1', '51300');
INSERT INTO `sys_role_menu` VALUES ('b5c23ce6-aa4e-4b69-b533-2b0e6adbda81', '1', '11300');
INSERT INTO `sys_role_menu` VALUES ('b6368d8e-2928-417e-81b9-46010f53ca4d', '2', '71104');
INSERT INTO `sys_role_menu` VALUES ('b6524c49-3016-49a6-a98b-00067f9f840c', '1', '51000');
INSERT INTO `sys_role_menu` VALUES ('b69a1187-a988-47a5-9aee-3d643f1b66a7', '1', '53000');
INSERT INTO `sys_role_menu` VALUES ('b807e2fe-f94e-4496-b828-e333b074e495', '1', '101403');
INSERT INTO `sys_role_menu` VALUES ('bad11c64-d2bd-4c0e-a5c2-cd26b32ec1d5', '1', '60000');
INSERT INTO `sys_role_menu` VALUES ('bc9cad63-fb72-4f7c-9eb0-120469a62976', '1', '62000');
INSERT INTO `sys_role_menu` VALUES ('c0e1d75f-44ca-4bd2-b45a-126d60babf7c', '1', '53700');
INSERT INTO `sys_role_menu` VALUES ('c44a1076-6bdd-4f22-ba5e-72b0b57d7cc6', '1', '53300');
INSERT INTO `sys_role_menu` VALUES ('c498c18d-941b-483a-94d3-2806b1bbbadf', '1', '62300');
INSERT INTO `sys_role_menu` VALUES ('c5789ab0-58ed-46da-acbf-63d29e6b69ce', '1', '22000');
INSERT INTO `sys_role_menu` VALUES ('c5be888e-0917-4b94-8f47-657a49d7d317', '1', '21400');
INSERT INTO `sys_role_menu` VALUES ('c6c3e045-7e86-4d6b-b4d1-5a4e7b1e681d', '1', '31400');
INSERT INTO `sys_role_menu` VALUES ('c99b29ce-f396-4803-8d16-fb09550c3101', '1', '61104');
INSERT INTO `sys_role_menu` VALUES ('cd77b585-cd35-4d8e-b299-6ece58291274', '1', '61102');
INSERT INTO `sys_role_menu` VALUES ('cff30cf2-b2b2-4018-b246-0efff6182117', '2', '71103');
INSERT INTO `sys_role_menu` VALUES ('d42ae572-4f0a-4bdd-afcf-e18fd4c7fc58', '2', '71400');
INSERT INTO `sys_role_menu` VALUES ('d5de24d8-8ec9-4d8a-b8da-3a0c2d33edb5', '1', '21200');
INSERT INTO `sys_role_menu` VALUES ('d66e8bca-8c32-4479-9e8f-a4960fa71382', '2', '21100');
INSERT INTO `sys_role_menu` VALUES ('da2262de-dc80-4a94-8ff9-0f51d54e5b3f', '2', '21400');
INSERT INTO `sys_role_menu` VALUES ('dc86a2e5-57bb-4060-b703-f61c7b6ca1c1', '1', '62400');
INSERT INTO `sys_role_menu` VALUES ('e1289e78-8be1-4dc6-ac2f-afdde8c699a8', '1', '31500');
INSERT INTO `sys_role_menu` VALUES ('e635f757-1666-48f6-964b-40a0f684762f', '2', '22100');
INSERT INTO `sys_role_menu` VALUES ('e6781ed9-68ac-4652-be51-ab24862414b7', '1', '101204');
INSERT INTO `sys_role_menu` VALUES ('e6e63352-41f5-48d8-b8c6-4a659d01ddfa', '1', '53200');
INSERT INTO `sys_role_menu` VALUES ('ea94a638-48fe-425a-973a-3480c9ffec62', '1', '41100');
INSERT INTO `sys_role_menu` VALUES ('ef354c52-d9a7-4c4d-ba37-5d07b1c182cb', '2', '11200');
INSERT INTO `sys_role_menu` VALUES ('ef8ec0e9-098b-4a71-976c-b44fa9466bb9', '1', '101304');
INSERT INTO `sys_role_menu` VALUES ('f411cf2c-4864-46aa-b884-b11d99f545d9', '2', '71102');
INSERT INTO `sys_role_menu` VALUES ('f7ca1018-dfe1-4599-ab75-e082cb7362b1', '1', '42100');
INSERT INTO `sys_role_menu` VALUES ('f7dffdb5-16fd-487c-9ffa-6f1aaee7eb87', '1', '61200');
INSERT INTO `sys_role_menu` VALUES ('fd93180a-3593-41a5-9450-40a4cee2265d', '2', '71200');
INSERT INTO `sys_role_menu` VALUES ('fdf4e319-4182-4a23-a0ad-8ef1d3cd0e1b', '1', '31900');

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键ID',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '姓名',
  `gender` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '性别（1-男，2-女，0-未知）',
  `mobile` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像',
  `organization_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '所属机构ID',
  `status` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '1' COMMENT '状态（1-正常，0-禁用）',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `creator` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `updater` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_user_username`(`username` ASC) USING BTREE,
  INDEX `idx_sys_user_email`(`email` ASC) USING BTREE,
  INDEX `idx_sys_user_mobile`(`mobile` ASC) USING BTREE,
  INDEX `idx_sys_user_organization`(`organization_id` ASC) USING BTREE,
  INDEX `idx_sys_user_status`(`status` ASC) USING BTREE,
  INDEX `idx_sys_user_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES ('1', 'admin', '$2a$10$8KK.26X5X6V5jDkR/Qwdg.QR0zr7sA8qPEKYh7SLdHLtDu3vFhvte', '系统管理员', '1', '13800138000', 'admin@example.com', 'NULL', '1', '1', 1, '系统管理员账号', '2025-04-29 09:27:10', '2025-04-29 09:27:10', 'admin', 'admin');

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键ID',
  `user_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `role_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色ID',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_user_role`(`user_id` ASC, `role_id` ASC) USING BTREE,
  INDEX `idx_sys_user_role_role`(`role_id` ASC) USING BTREE,
  INDEX `idx_sys_user_role_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户角色关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES ('1', '1', '1', '2025-04-29 09:27:10');
INSERT INTO `sys_user_role` VALUES ('2', '2', '2', '2025-04-29 09:27:10');
INSERT INTO `sys_user_role` VALUES ('4c995d20-ca06-4465-9b3d-aac233493c6d', 'c419d22d-eb21-4510-b410-42e894e1242d', '2', '2025-05-04 18:05:32');

-- ----------------------------
-- Function structure for get_current_version
-- ----------------------------
DROP FUNCTION IF EXISTS `get_current_version`;
delimiter ;;
CREATE FUNCTION `get_current_version`(p_metadata_id VARCHAR(32),
    p_metadata_type VARCHAR(50))
 RETURNS varchar(32) CHARSET utf8mb4
  READS SQL DATA 
BEGIN
    DECLARE v_version_id VARCHAR(32) DEFAULT NULL;

    SELECT `id` INTO v_version_id FROM `data_metadata_version_record`
    WHERE `metadata_id` = p_metadata_id
      AND `metadata_type` = p_metadata_type
      AND `is_current` = 1
      AND `is_deleted` = 0
    ORDER BY `publish_time` DESC
    LIMIT 1;
    
    RETURN v_version_id;
END
;;
delimiter ;

-- ----------------------------
-- Procedure structure for publish_version
-- ----------------------------
DROP PROCEDURE IF EXISTS `publish_version`;
delimiter ;;
CREATE PROCEDURE `publish_version`(IN p_metadata_id VARCHAR(32),
    IN p_metadata_type VARCHAR(50),
    IN p_metadata_name VARCHAR(100),
    IN p_metadata_path VARCHAR(500),
    IN p_version_name VARCHAR(100),
    IN p_version_description VARCHAR(500),
    IN p_publisher_id VARCHAR(32),
    IN p_publisher_name VARCHAR(100),
    IN p_change_summary VARCHAR(500),
    IN p_tags VARCHAR(200),
    OUT p_out_version_id VARCHAR(32))
  MODIFIES SQL DATA 
BEGIN
    DECLARE v_previous_version_id VARCHAR(32) DEFAULT NULL;
    DECLARE v_version_number VARCHAR(20);
    DECLARE v_current_time  timestamp ;
    DECLARE v_new_version_id VARCHAR(32);

    DECLARE v_current_version_str VARCHAR(20);
    DECLARE v_major INT;
    DECLARE v_minor INT;
    DECLARE v_patch INT;
    DECLARE v_temp_minor_patch VARCHAR(20);

    SET v_current_time = NOW(6);
    SET v_new_version_id = REPLACE(UUID(), '-', '');
    SET p_out_version_id = v_new_version_id;
    
    SELECT `id` INTO v_previous_version_id FROM `data_metadata_version_record`
    WHERE `metadata_id` = p_metadata_id
      AND `metadata_type` = p_metadata_type
      AND `is_current` = 1
      AND `is_deleted` = 0
    ORDER BY `publish_time` DESC
    LIMIT 1;
    
    IF v_previous_version_id IS NULL THEN
        SET v_version_number = 'v1.0.0';
    ELSE
        SELECT `version_number` INTO v_current_version_str FROM `data_metadata_version_record`
        WHERE `id` = v_previous_version_id;
        
        IF SUBSTRING(v_current_version_str, 1, 1) = 'v' THEN
            SET v_current_version_str = SUBSTRING(v_current_version_str, 2);
        END IF;
        
        SET v_major = CAST(SUBSTRING_INDEX(v_current_version_str, '.', 1) AS UNSIGNED);
        SET v_temp_minor_patch = SUBSTRING_INDEX(v_current_version_str, '.', -2);
        SET v_minor = CAST(SUBSTRING_INDEX(v_temp_minor_patch, '.', 1) AS UNSIGNED);
        SET v_patch = CAST(SUBSTRING_INDEX(v_temp_minor_patch, '.', -1) AS UNSIGNED);
        
        SET v_patch = v_patch + 1;
        SET v_version_number = CONCAT('v', v_major, '.', v_minor, '.', v_patch);
    END IF;
    
    IF v_previous_version_id IS NOT NULL THEN
        UPDATE `data_metadata_version_record`
        SET `is_current` = 0,
            `update_time` = v_current_time
        WHERE `id` = v_previous_version_id;
    END IF;
    
    INSERT INTO `data_metadata_version_record` (
        `id`, `version_number`, `version_name`, `version_description`,
        `metadata_id`, `metadata_type`, `metadata_name`, `metadata_path`,
        `publisher_id`, `publisher_name`, `publish_time`,
        `change_summary`, `previous_version_id`, `is_current`,
        `approval_status`, `tags`, `create_time`, `update_time`, `is_deleted`
    ) VALUES (
        v_new_version_id, v_version_number, p_version_name, p_version_description,
        p_metadata_id, p_metadata_type, p_metadata_name, p_metadata_path,
        p_publisher_id, p_publisher_name, v_current_time,
        p_change_summary, v_previous_version_id, 1,
        'pending', p_tags, v_current_time, v_current_time, 0
    );
    
    CASE p_metadata_type
        WHEN 'catalog' THEN
            CALL `save_versioned_catalog`(v_new_version_id, p_metadata_id);
        WHEN 'database' THEN
            CALL `save_versioned_database`(v_new_version_id, p_metadata_id);
        WHEN 'table' THEN
            CALL `save_versioned_table`(v_new_version_id, p_metadata_id);
        WHEN 'field' THEN
            CALL `save_versioned_field`(v_new_version_id, p_metadata_id);
        ELSE
            BEGIN END; -- No operation for other types
    END CASE;
    
    -- COMMIT; -- MySQL procedures usually run in auto-commit mode or within a transaction started by the caller.
    -- EXCEPTION handling in MySQL is different. This procedure will implicitly rollback on error if part of a transaction.
END
;;
delimiter ;

SET FOREIGN_KEY_CHECKS = 1;
