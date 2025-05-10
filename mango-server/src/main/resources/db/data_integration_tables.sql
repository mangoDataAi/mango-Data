-- 同步任务表
CREATE TABLE t_sync_task (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '任务名称',
    type VARCHAR(50) NOT NULL COMMENT '任务类型：Delta Lake、StarRocks等',
    schedule_type VARCHAR(20) NOT NULL COMMENT '调度类型：manual、scheduled、periodic',
    schedule_time TIMESTAMP NULL COMMENT '定时执行时间',
    expire_time TIMESTAMP NULL COMMENT '定时过期时间',
    periodic_type VARCHAR(20) NULL COMMENT '周期类型：hourly、daily、weekly、monthly、cron',
    hourly_minute INT NULL COMMENT '小时执行的分钟数',
    periodic_time VARCHAR(20) NULL COMMENT '定期执行时间',
    week_days VARCHAR(50) NULL COMMENT '周几执行，多个值用逗号分隔',
    month_days VARCHAR(100) NULL COMMENT '每月哪天执行，多个值用逗号分隔',
    cron_expression VARCHAR(100) NULL COMMENT 'Cron表达式',
    effective_time TIMESTAMP NULL COMMENT '生效时间',
    expiry_time TIMESTAMP NULL COMMENT '失效时间',
    schedule_timeout INT NULL COMMENT '调度超时时间（分钟）',
    priority VARCHAR(10) NULL COMMENT '任务优先级：low、medium、high',
    sync_mode VARCHAR(20) NOT NULL COMMENT '同步模式：full、incremental',
    incremental_strategy VARCHAR(20) NULL COMMENT '增量策略',
    incremental_mode VARCHAR(20) NULL COMMENT '增量模式：timestamp、sequence',
    start_time TIMESTAMP NULL COMMENT '时间戳增量开始时间',
    start_value BIGINT NULL COMMENT '序列号增量起始值',
    incremental_config_mode VARCHAR(20) NULL COMMENT '多表增量配置方式：shared、individual',
    cdc_mode VARCHAR(10) NULL COMMENT 'CDC方式：log、tool',
    cdc_tool VARCHAR(20) NULL COMMENT 'CDC工具：debezium、canal、other',
    cdc_operations VARCHAR(50) NULL COMMENT 'CDC操作类型，多个值用逗号分隔',
    dependencies VARCHAR(500) NULL COMMENT '任务依赖，多个值用逗号分隔',
    dependency_mode VARCHAR(10) NULL COMMENT '依赖模式：all、any',
    retry_enabled TINYINT NULL COMMENT '是否启用重试：0-否，1-是',
    retry_times INT NULL COMMENT '重试次数',
    retry_interval INT NULL COMMENT '重试间隔（分钟）',
    retry_strategy VARCHAR(20) NULL COMMENT '重试策略：fixed、exponential',
    max_wait_time INT NULL COMMENT '最大等待时间（分钟）',
    retry_conditions VARCHAR(200) NULL COMMENT '重试条件，多个值用逗号分隔',
    retry_actions VARCHAR(200) NULL COMMENT '重试前操作，多个值用逗号分隔',
    performance_enabled TINYINT NULL COMMENT '性能参数是否启用：0-否，1-是',
    parallelism INT NULL COMMENT '并行度',
    batch_size INT NULL COMMENT '批量大小',
    buffer_size INT NULL COMMENT '缓冲区大小(MB)',
    reader_concurrency INT NULL COMMENT '读取并发数',
    writer_concurrency INT NULL COMMENT '写入并发数',
    throttle_rate INT NULL COMMENT '限流速率(MB/s)',
    status VARCHAR(20) NOT NULL COMMENT '任务状态：created、running、paused、completed、failed',
    create_time TIMESTAMP NOT NULL COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL COMMENT '更新时间',
    creator VARCHAR(50) NULL COMMENT '创建人',
    last_execute_time TIMESTAMP NULL COMMENT '最后执行时间',
    last_execute_result VARCHAR(100) NULL COMMENT '最后执行结果',
    execute_count INT NULL COMMENT '执行次数'
) COMMENT='数据同步任务表';

-- 表映射表
CREATE TABLE t_table_mapping (
    id VARCHAR(36) PRIMARY KEY,
    sync_task_id VARCHAR(36) NOT NULL COMMENT '所属同步任务ID',
    source_table_id VARCHAR(36) NOT NULL COMMENT '源表ID',
    source_table VARCHAR(100) NOT NULL COMMENT '源表名称',
    source_table_path VARCHAR(500) NOT NULL COMMENT '源表完整路径',
    target_table_id VARCHAR(36) NOT NULL COMMENT '目标表ID',
    target_table VARCHAR(100) NOT NULL COMMENT '目标表名称',
    target_table_path VARCHAR(500) NOT NULL COMMENT '目标表完整路径',
    incremental_field VARCHAR(100) NULL COMMENT '增量字段',
    filter_condition VARCHAR(1000) NULL COMMENT '数据过滤条件',
    validation_enabled TINYINT NULL COMMENT '数据验证是否启用：0-否，1-是',
    validation_types VARCHAR(100) NULL COMMENT '验证类型，多个值用逗号分隔',
    row_count_tolerance INT NULL COMMENT '行数验证容错率(%)',
    row_count_mode VARCHAR(20) NULL COMMENT '行数验证模式',
    checksum_fields VARCHAR(500) NULL COMMENT '校验和字段，多个值用逗号分隔',
    checksum_algorithm VARCHAR(20) NULL COMMENT '校验和算法',
    sample_rate INT NULL COMMENT '采样率(%)',
    sample_method VARCHAR(20) NULL COMMENT '采样方式',
    sample_fields VARCHAR(500) NULL COMMENT '样本验证字段，多个值用逗号分隔',
    validation_fail_action VARCHAR(20) NULL COMMENT '验证失败处理',
    selected_columns VARCHAR(2000) NULL COMMENT '选中的列，多个值用逗号分隔',
    field_mappings TEXT NULL COMMENT '字段映射配置（JSON格式）',
    create_time TIMESTAMP NOT NULL COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL COMMENT '更新时间'
) COMMENT='表映射关系表';

-- 任务执行日志表
CREATE TABLE t_sync_task_log (
    id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL COMMENT '同步任务ID',
    start_time TIMESTAMP NOT NULL COMMENT '开始时间',
    end_time TIMESTAMP NULL COMMENT '结束时间',
    status VARCHAR(20) NOT NULL COMMENT '执行状态：running、completed、failed',
    execution_result VARCHAR(20) NULL COMMENT '执行结果：success、failed',
    total_record_count BIGINT NULL COMMENT '总记录数',
    success_record_count BIGINT NULL COMMENT '成功记录数',
    failed_record_count BIGINT NULL COMMENT '失败记录数',
    total_byte_size BIGINT NULL COMMENT '总字节数',
    execution_time BIGINT NULL COMMENT '执行时间（毫秒）',
    error_message TEXT NULL COMMENT '错误信息',
    datax_job_id VARCHAR(36) NULL COMMENT 'DataX作业ID',
    datax_config TEXT NULL COMMENT 'DataX配置',
    datax_output TEXT NULL COMMENT 'DataX输出'
) COMMENT='同步任务执行日志表';

-- 任务模板表
CREATE TABLE t_sync_task_template (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    type VARCHAR(50) NOT NULL COMMENT '任务类型：Delta Lake、StarRocks等',
    description VARCHAR(500) NULL COMMENT '模板描述',
    config TEXT NOT NULL COMMENT '模板配置（JSON格式）',
    creator VARCHAR(50) NULL COMMENT '创建人',
    create_time TIMESTAMP NOT NULL COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL COMMENT '更新时间'
) COMMENT='同步任务模板表';

-- 添加索引
CREATE INDEX idx_task_name ON t_sync_task(name);
CREATE INDEX idx_task_type ON t_sync_task(type);
CREATE INDEX idx_task_status ON t_sync_task(status);
CREATE INDEX idx_task_creator ON t_sync_task(creator);
CREATE INDEX idx_task_create_time ON t_sync_task(create_time);

CREATE INDEX idx_table_mapping_task_id ON t_table_mapping(sync_task_id);

CREATE INDEX idx_task_log_task_id ON t_sync_task_log(task_id);
CREATE INDEX idx_task_log_start_time ON t_sync_task_log(start_time);
CREATE INDEX idx_task_log_status ON t_sync_task_log(status);

CREATE INDEX idx_template_name ON t_sync_task_template(name);
CREATE INDEX idx_template_type ON t_sync_task_template(type); 