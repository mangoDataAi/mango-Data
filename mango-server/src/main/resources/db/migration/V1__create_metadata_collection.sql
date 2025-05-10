-- 创建元数据集合表
CREATE TABLE metadata_collection (
    -- 基本信息
    id VARCHAR2(36) DEFAULT SYS_GUID() PRIMARY KEY,
    name VARCHAR2(100) NOT NULL,
    description CLOB,
    type VARCHAR2(50),
    data_source_id VARCHAR2(36),
    data_source_name VARCHAR2(100),
    metadata_count NUMBER(10) DEFAULT 0,
    status NUMBER(1) DEFAULT 0,
    creator_id VARCHAR2(36),
    creator_name VARCHAR2(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted NUMBER(1) DEFAULT 0,
    catalog_id VARCHAR2(36),
    
    -- 血缘分析相关
    lineage_enabled NUMBER(1) DEFAULT 0,
    last_lineage_analysis_time TIMESTAMP,
    
    -- JSON配置字段
    import_config CLOB,
    matching_strategy CLOB,
    storage_strategy CLOB,
    schedule_config CLOB,
    notification_config CLOB,
    quality_rules CLOB,
    tags CLOB,
    
    -- 约束
    CONSTRAINT uk_metadata_collection_name UNIQUE (name)
);

-- 创建注释
COMMENT ON TABLE metadata_collection IS '元数据集合表';
COMMENT ON COLUMN metadata_collection.id IS '主键ID';
COMMENT ON COLUMN metadata_collection.name IS '集合名称';
COMMENT ON COLUMN metadata_collection.description IS '集合描述';
COMMENT ON COLUMN metadata_collection.type IS '集合类型';
COMMENT ON COLUMN metadata_collection.data_source_id IS '数据源ID';
COMMENT ON COLUMN metadata_collection.data_source_name IS '数据源名称';
COMMENT ON COLUMN metadata_collection.metadata_count IS '元数据数量';
COMMENT ON COLUMN metadata_collection.status IS '状态（0-未启动，1-运行中，2-已完成，3-失败）';
COMMENT ON COLUMN metadata_collection.creator_id IS '创建人ID';
COMMENT ON COLUMN metadata_collection.creator_name IS '创建人名称';
COMMENT ON COLUMN metadata_collection.create_time IS '创建时间';
COMMENT ON COLUMN metadata_collection.update_time IS '更新时间';
COMMENT ON COLUMN metadata_collection.is_deleted IS '是否删除（0-未删除，1-已删除）';
COMMENT ON COLUMN metadata_collection.catalog_id IS '目录ID';
COMMENT ON COLUMN metadata_collection.lineage_enabled IS '是否启用血缘分析（0-否，1-是）';
COMMENT ON COLUMN metadata_collection.last_lineage_analysis_time IS '最后一次血缘分析时间';
COMMENT ON COLUMN metadata_collection.import_config IS '导入配置（JSON格式）';
COMMENT ON COLUMN metadata_collection.matching_strategy IS '标准匹配策略（JSON格式）';
COMMENT ON COLUMN metadata_collection.storage_strategy IS '入库策略（JSON格式）';
COMMENT ON COLUMN metadata_collection.schedule_config IS '调度配置（JSON格式）';
COMMENT ON COLUMN metadata_collection.notification_config IS '通知配置（JSON格式）';
COMMENT ON COLUMN metadata_collection.quality_rules IS '质量规则配置（JSON格式）';
COMMENT ON COLUMN metadata_collection.tags IS '标签（JSON数组）';

-- 创建索引
CREATE INDEX idx_metadata_collection_status ON metadata_collection(status);
CREATE INDEX idx_metadata_collection_create_time ON metadata_collection(create_time);
CREATE INDEX idx_metadata_collection_data_source_id ON metadata_collection(data_source_id);
CREATE INDEX idx_metadata_collection_catalog_id ON metadata_collection(catalog_id);

-- 创建触发器自动更新update_time
CREATE OR REPLACE TRIGGER trg_metadata_collection_update 
BEFORE UPDATE ON metadata_collection 
FOR EACH ROW
BEGIN
    :NEW.update_time := CURRENT_TIMESTAMP;
END;
/ 