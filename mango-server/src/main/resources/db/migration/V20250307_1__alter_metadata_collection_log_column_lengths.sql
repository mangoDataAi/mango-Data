-- 修改 data_metadata_collection_log 表中字段长度，解决字符串截断问题
-- 适用于达梦数据库(DM)

-- 修改 step 字段长度
ALTER TABLE data_metadata_collection_log MODIFY step VARCHAR(100);

-- 修改 level 字段长度
ALTER TABLE data_metadata_collection_log MODIFY level VARCHAR(50);

-- 修改 message 字段长度
ALTER TABLE data_metadata_collection_log MODIFY message VARCHAR(2000);

-- 修改 details 字段长度
ALTER TABLE data_metadata_collection_log MODIFY details VARCHAR(4000);

-- 修改 stack_trace 字段长度
ALTER TABLE data_metadata_collection_log MODIFY stack_trace VARCHAR(8000);

-- 添加注释
COMMENT ON COLUMN data_metadata_collection_log.step IS '执行步骤，最大长度100';
COMMENT ON COLUMN data_metadata_collection_log.level IS '日志级别，最大长度50';
COMMENT ON COLUMN data_metadata_collection_log.message IS '日志消息，最大长度2000';
COMMENT ON COLUMN data_metadata_collection_log.details IS '详细信息（JSON格式），最大长度4000';
COMMENT ON COLUMN data_metadata_collection_log.stack_trace IS '堆栈跟踪信息，最大长度8000'; 