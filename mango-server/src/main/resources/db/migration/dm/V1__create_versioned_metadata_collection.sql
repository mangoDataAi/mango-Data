-- 创建定版元数据集合表
CREATE TABLE versioned_metadata_collection (
    -- 主键
    id VARCHAR2(64) NOT NULL,
    -- 版本ID
    version_id VARCHAR2(64),
    -- 原始ID
    original_id VARCHAR2(64),
    -- 名称
    name VARCHAR2(255),
    -- 描述
    description CLOB,
    -- 类型
    type VARCHAR2(50),
    -- 数据源ID
    data_source_id VARCHAR2(64),
    -- 数据源名称
    data_source_name VARCHAR2(255),
    -- 元数据数量
    metadata_count NUMBER(10),
    -- 创建者ID
    creator_id VARCHAR2(64),
    -- 创建者名称
    creator_name VARCHAR2(255),
    -- 创建时间
    create_time TIMESTAMP(6),
    -- 更新时间
    update_time TIMESTAMP(6),
    -- 是否启用血缘分析
    lineage_enabled NUMBER(1),
    -- 最后血缘分析时间
    last_lineage_analysis_time TIMESTAMP(6),
    -- 导入配置（JSON）
    import_config CLOB,
    -- 标签（JSON）
    tags CLOB,
    -- 目录ID
    catalog_id VARCHAR2(64),
    -- 匹配策略
    matching_strategy VARCHAR2(50),
    -- 存储策略
    storage_strategy VARCHAR2(50),
    -- 调度配置（JSON）
    schedule_config CLOB,
    -- 通知配置（JSON）
    notification_config CLOB,
    -- 质量规则（JSON）
    quality_rules CLOB,
    -- 目录名称
    catalog_name VARCHAR2(255),
    -- 数据源类型
    data_source_type VARCHAR2(50),
    -- 匹配策略描述
    matching_strategy_desc VARCHAR2(255),
    -- 存储策略描述
    storage_strategy_desc VARCHAR2(255),
    -- 所有者
    owner VARCHAR2(255),
    -- 是否删除
    is_deleted NUMBER(1) DEFAULT 0,
    
    -- 主键约束
    CONSTRAINT pk_versioned_metadata_collection PRIMARY KEY (id)
);

-- 创建索引
CREATE INDEX idx_vmc_version_id ON versioned_metadata_collection (version_id);
CREATE INDEX idx_vmc_catalog_id ON versioned_metadata_collection (catalog_id);
CREATE INDEX idx_vmc_original_id ON versioned_metadata_collection (original_id);

-- 添加注释
COMMENT ON TABLE versioned_metadata_collection IS '定版元数据集合表';
COMMENT ON COLUMN versioned_metadata_collection.id IS '主键';
COMMENT ON COLUMN versioned_metadata_collection.version_id IS '版本ID';
COMMENT ON COLUMN versioned_metadata_collection.original_id IS '原始ID';
COMMENT ON COLUMN versioned_metadata_collection.name IS '名称';
COMMENT ON COLUMN versioned_metadata_collection.description IS '描述';
COMMENT ON COLUMN versioned_metadata_collection.type IS '类型';
COMMENT ON COLUMN versioned_metadata_collection.data_source_id IS '数据源ID';
COMMENT ON COLUMN versioned_metadata_collection.data_source_name IS '数据源名称';
COMMENT ON COLUMN versioned_metadata_collection.metadata_count IS '元数据数量';
COMMENT ON COLUMN versioned_metadata_collection.creator_id IS '创建者ID';
COMMENT ON COLUMN versioned_metadata_collection.creator_name IS '创建者名称';
COMMENT ON COLUMN versioned_metadata_collection.create_time IS '创建时间';
COMMENT ON COLUMN versioned_metadata_collection.update_time IS '更新时间';
COMMENT ON COLUMN versioned_metadata_collection.lineage_enabled IS '是否启用血缘分析';
COMMENT ON COLUMN versioned_metadata_collection.last_lineage_analysis_time IS '最后血缘分析时间';
COMMENT ON COLUMN versioned_metadata_collection.import_config IS '导入配置（JSON）';
COMMENT ON COLUMN versioned_metadata_collection.tags IS '标签（JSON）';
COMMENT ON COLUMN versioned_metadata_collection.catalog_id IS '目录ID';
COMMENT ON COLUMN versioned_metadata_collection.matching_strategy IS '匹配策略';
COMMENT ON COLUMN versioned_metadata_collection.storage_strategy IS '存储策略';
COMMENT ON COLUMN versioned_metadata_collection.schedule_config IS '调度配置（JSON）';
COMMENT ON COLUMN versioned_metadata_collection.notification_config IS '通知配置（JSON）';
COMMENT ON COLUMN versioned_metadata_collection.quality_rules IS '质量规则（JSON）';
COMMENT ON COLUMN versioned_metadata_collection.catalog_name IS '目录名称';
COMMENT ON COLUMN versioned_metadata_collection.data_source_type IS '数据源类型';
COMMENT ON COLUMN versioned_metadata_collection.matching_strategy_desc IS '匹配策略描述';
COMMENT ON COLUMN versioned_metadata_collection.storage_strategy_desc IS '存储策略描述';
COMMENT ON COLUMN versioned_metadata_collection.owner IS '所有者';
COMMENT ON COLUMN versioned_metadata_collection.is_deleted IS '是否删除'; 