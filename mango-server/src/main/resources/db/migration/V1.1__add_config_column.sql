-- 为t_api表添加config字段
ALTER TABLE t_api ADD COLUMN IF NOT EXISTS config TEXT COMMENT '完整API配置，JSON格式，用于存储API向导的所有步骤数据'; 