-- 为SyncTask表添加日志相关字段
ALTER TABLE sync_task ADD COLUMN source_total_records BIGINT COMMENT '同步源数据总量';
ALTER TABLE sync_task ADD COLUMN target_total_records BIGINT COMMENT '目标表数据总量';
ALTER TABLE sync_task ADD COLUMN incremental_added_records BIGINT COMMENT '增量同步时新增的记录数';
ALTER TABLE sync_task ADD COLUMN incremental_updated_records BIGINT COMMENT '增量同步时更新的记录数';
ALTER TABLE sync_task ADD COLUMN incremental_deleted_records BIGINT COMMENT '增量同步时删除的记录数';
ALTER TABLE sync_task ADD COLUMN last_incremental_value VARCHAR(255) COMMENT '最近同步的增量值';
ALTER TABLE sync_task ADD COLUMN incremental_sync_details JSON COMMENT '最近同步的增量记录详情（JSON格式）';
ALTER TABLE sync_task ADD COLUMN validation_results JSON COMMENT '数据验证结果（JSON格式）';
ALTER TABLE sync_task ADD COLUMN sync_stage_details JSON COMMENT '同步执行的阶段信息（JSON格式）';
ALTER TABLE sync_task ADD COLUMN execution_time_millis BIGINT COMMENT '同步执行耗时（毫秒）';
ALTER TABLE sync_task ADD COLUMN error_details TEXT COMMENT '错误详情';
ALTER TABLE sync_task ADD COLUMN last_sync_incremental_strategy VARCHAR(1000) COMMENT '最后一次同步的增量策略详情';
ALTER TABLE sync_task ADD COLUMN execution_summary VARCHAR(1000) COMMENT '可读的执行摘要信息'; 