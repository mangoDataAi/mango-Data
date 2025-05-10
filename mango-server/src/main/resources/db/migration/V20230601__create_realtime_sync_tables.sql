-- 创建实时同步任务表
CREATE TABLE di_realtime_sync_task (
    id VARCHAR2(36) PRIMARY KEY,
    name VARCHAR2(100) NOT NULL,
    description VARCHAR2(500),
    source_id VARCHAR2(36) NOT NULL,
    source_name VARCHAR2(100),
    source_type VARCHAR2(50),
    target_id VARCHAR2(36) NOT NULL,
    target_name VARCHAR2(100),
    target_type VARCHAR2(50),
    status VARCHAR2(20) DEFAULT 'created',
    sync_mode VARCHAR2(50) NOT NULL,
    flink_job_id VARCHAR2(100),
    table_mappings CLOB,
    filter_condition VARCHAR2(500),
    parallelism NUMBER(10),
    checkpoint_interval NUMBER(10),
    restart_strategy VARCHAR2(50),
    max_restart_attempts NUMBER(10),
    restart_delay NUMBER(10),
    monitor_config CLOB,
    alert_thresholds CLOB,
    advanced_config CLOB,
    last_run_time TIMESTAMP,
    last_run_result VARCHAR2(255),
    total_records_processed NUMBER(20) DEFAULT 0,
    total_records_failed NUMBER(20) DEFAULT 0,
    created_by VARCHAR2(100),
    create_time TIMESTAMP DEFAULT SYSDATE,
    updated_by VARCHAR2(100),
    update_time TIMESTAMP DEFAULT SYSDATE
);

-- 创建实时同步任务表索引
CREATE INDEX idx_realtime_task_name ON di_realtime_sync_task (name);
CREATE INDEX idx_realtime_task_status ON di_realtime_sync_task (status);
CREATE INDEX idx_realtime_task_source_id ON di_realtime_sync_task (source_id);
CREATE INDEX idx_realtime_task_target_id ON di_realtime_sync_task (target_id);
CREATE INDEX idx_realtime_task_create_time ON di_realtime_sync_task (create_time);

-- 创建实时同步任务日志表
CREATE TABLE di_realtime_sync_task_log (
    id VARCHAR2(36) PRIMARY KEY,
    task_id VARCHAR2(36) NOT NULL,
    flink_job_id VARCHAR2(100),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    duration NUMBER(20),
    status VARCHAR2(20),
    records_processed NUMBER(20) DEFAULT 0,
    records_failed NUMBER(20) DEFAULT 0,
    error_message VARCHAR2(4000),
    create_time TIMESTAMP DEFAULT SYSDATE
);

-- 创建实时同步任务日志表索引
CREATE INDEX idx_realtime_task_log_task_id ON di_realtime_sync_task_log (task_id);
CREATE INDEX idx_realtime_task_log_start_time ON di_realtime_sync_task_log (start_time);

-- 创建实时同步任务指标表
CREATE TABLE di_realtime_sync_task_metrics (
    id VARCHAR2(36) PRIMARY KEY,
    task_id VARCHAR2(36) NOT NULL,
    flink_job_id VARCHAR2(100),
    metric_time TIMESTAMP,
    records_per_second NUMBER(20,2),
    total_records NUMBER(20),
    failed_records NUMBER(20),
    checkpoint_count NUMBER(10),
    last_checkpoint_time TIMESTAMP,
    checkpoint_duration_avg NUMBER(20),
    checkpoint_size_avg NUMBER(20),
    cpu_usage NUMBER(10,2),
    memory_usage NUMBER(20),
    metrics_json CLOB,
    create_time TIMESTAMP DEFAULT SYSDATE
);

-- 创建实时同步任务指标表索引
CREATE INDEX idx_realtime_task_metrics_task_id ON di_realtime_sync_task_metrics (task_id);
CREATE INDEX idx_realtime_task_metrics_time ON di_realtime_sync_task_metrics (metric_time);

-- 添加注释
COMMENT ON TABLE di_realtime_sync_task IS '实时同步任务表';
COMMENT ON COLUMN di_realtime_sync_task.id IS '主键ID';
COMMENT ON COLUMN di_realtime_sync_task.name IS '任务名称';
COMMENT ON COLUMN di_realtime_sync_task.description IS '任务描述';
COMMENT ON COLUMN di_realtime_sync_task.source_id IS '源数据源ID';
COMMENT ON COLUMN di_realtime_sync_task.source_name IS '源数据源名称';
COMMENT ON COLUMN di_realtime_sync_task.source_type IS '源数据源类型';
COMMENT ON COLUMN di_realtime_sync_task.target_id IS '目标数据源ID';
COMMENT ON COLUMN di_realtime_sync_task.target_name IS '目标数据源名称';
COMMENT ON COLUMN di_realtime_sync_task.target_type IS '目标数据源类型';
COMMENT ON COLUMN di_realtime_sync_task.status IS '任务状态';
COMMENT ON COLUMN di_realtime_sync_task.sync_mode IS '同步模式';
COMMENT ON COLUMN di_realtime_sync_task.flink_job_id IS 'Flink作业ID';
COMMENT ON COLUMN di_realtime_sync_task.table_mappings IS '表映射关系（JSON格式）';
COMMENT ON COLUMN di_realtime_sync_task.filter_condition IS '数据过滤条件';
COMMENT ON COLUMN di_realtime_sync_task.parallelism IS '并行度';
COMMENT ON COLUMN di_realtime_sync_task.checkpoint_interval IS '检查点间隔（秒）';
COMMENT ON COLUMN di_realtime_sync_task.restart_strategy IS '重启策略';
COMMENT ON COLUMN di_realtime_sync_task.max_restart_attempts IS '最大重启次数';
COMMENT ON COLUMN di_realtime_sync_task.restart_delay IS '重启延迟（秒）';
COMMENT ON COLUMN di_realtime_sync_task.monitor_config IS '监控配置（JSON格式）';
COMMENT ON COLUMN di_realtime_sync_task.alert_thresholds IS '告警阈值（JSON格式）';
COMMENT ON COLUMN di_realtime_sync_task.advanced_config IS '高级配置（JSON格式）';
COMMENT ON COLUMN di_realtime_sync_task.last_run_time IS '上次运行时间';
COMMENT ON COLUMN di_realtime_sync_task.last_run_result IS '上次执行结果';
COMMENT ON COLUMN di_realtime_sync_task.total_records_processed IS '记录处理总数';
COMMENT ON COLUMN di_realtime_sync_task.total_records_failed IS '记录失败总数';
COMMENT ON COLUMN di_realtime_sync_task.created_by IS '创建人';
COMMENT ON COLUMN di_realtime_sync_task.create_time IS '创建时间';
COMMENT ON COLUMN di_realtime_sync_task.updated_by IS '更新人';
COMMENT ON COLUMN di_realtime_sync_task.update_time IS '更新时间';

COMMENT ON TABLE di_realtime_sync_task_log IS '实时同步任务日志表';
COMMENT ON COLUMN di_realtime_sync_task_log.id IS '主键ID';
COMMENT ON COLUMN di_realtime_sync_task_log.task_id IS '任务ID';
COMMENT ON COLUMN di_realtime_sync_task_log.flink_job_id IS 'Flink作业ID';
COMMENT ON COLUMN di_realtime_sync_task_log.start_time IS '开始时间';
COMMENT ON COLUMN di_realtime_sync_task_log.end_time IS '结束时间';
COMMENT ON COLUMN di_realtime_sync_task_log.duration IS '持续时间（毫秒）';
COMMENT ON COLUMN di_realtime_sync_task_log.status IS '状态';
COMMENT ON COLUMN di_realtime_sync_task_log.records_processed IS '处理记录数';
COMMENT ON COLUMN di_realtime_sync_task_log.records_failed IS '失败记录数';
COMMENT ON COLUMN di_realtime_sync_task_log.error_message IS '错误信息';
COMMENT ON COLUMN di_realtime_sync_task_log.create_time IS '创建时间';

COMMENT ON TABLE di_realtime_sync_task_metrics IS '实时同步任务指标表';
COMMENT ON COLUMN di_realtime_sync_task_metrics.id IS '主键ID';
COMMENT ON COLUMN di_realtime_sync_task_metrics.task_id IS '任务ID';
COMMENT ON COLUMN di_realtime_sync_task_metrics.flink_job_id IS 'Flink作业ID';
COMMENT ON COLUMN di_realtime_sync_task_metrics.metric_time IS '指标时间';
COMMENT ON COLUMN di_realtime_sync_task_metrics.records_per_second IS '每秒处理记录数';
COMMENT ON COLUMN di_realtime_sync_task_metrics.total_records IS '总记录数';
COMMENT ON COLUMN di_realtime_sync_task_metrics.failed_records IS '失败记录数';
COMMENT ON COLUMN di_realtime_sync_task_metrics.checkpoint_count IS '检查点数量';
COMMENT ON COLUMN di_realtime_sync_task_metrics.last_checkpoint_time IS '最近检查点时间';
COMMENT ON COLUMN di_realtime_sync_task_metrics.checkpoint_duration_avg IS '检查点平均持续时间';
COMMENT ON COLUMN di_realtime_sync_task_metrics.checkpoint_size_avg IS '检查点平均大小';
COMMENT ON COLUMN di_realtime_sync_task_metrics.cpu_usage IS 'CPU使用率';
COMMENT ON COLUMN di_realtime_sync_task_metrics.memory_usage IS '内存使用量';
COMMENT ON COLUMN di_realtime_sync_task_metrics.metrics_json IS '指标JSON';
COMMENT ON COLUMN di_realtime_sync_task_metrics.create_time IS '创建时间'; 