-- 创建同步任务步骤日志表
CREATE TABLE IF NOT EXISTS sync_task_step_log (
    id VARCHAR(64) PRIMARY KEY COMMENT '步骤日志ID，任务日志ID_步骤类型',
    task_log_id VARCHAR(64) NOT NULL COMMENT '关联的任务日志ID',
    task_id VARCHAR(64) NOT NULL COMMENT '关联的任务ID',
    step_number INT NOT NULL COMMENT '步骤序号',
    step_name VARCHAR(100) NOT NULL COMMENT '步骤名称',
    step_type VARCHAR(50) NOT NULL COMMENT '步骤类型',
    step_description VARCHAR(255) COMMENT '步骤描述',
    start_time DATETIME(3) NOT NULL COMMENT '步骤开始时间',
    end_time DATETIME(3) COMMENT '步骤结束时间',
    status VARCHAR(20) COMMENT '步骤状态：success, warning, failure',
    result_message VARCHAR(255) COMMENT '步骤执行结果消息',
    context_info VARCHAR(1000) COMMENT '步骤执行上下文信息',
    data_count BIGINT COMMENT '处理数据量',
    detail_log TEXT COMMENT '步骤详细日志',
    execution_time BIGINT COMMENT '步骤执行耗时（毫秒）',
    INDEX idx_task_log_id (task_log_id),
    INDEX idx_task_id (task_id),
    INDEX idx_step_type (step_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='同步任务步骤日志'; 