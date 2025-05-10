-- 添加临时表字段到data_sync_task表
ALTER TABLE data_sync_task 
ADD COLUMN temporary_tables JSON COMMENT '同步过程中创建的临时表列表，用于清理资源'; 