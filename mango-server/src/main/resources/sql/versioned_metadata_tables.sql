-- 定版元数据目录表
CREATE TABLE versioned_metadata_catalog (
    id VARCHAR(32) NOT NULL,
    version_id VARCHAR(32) NOT NULL,
    original_id VARCHAR(32) NOT NULL,
    name VARCHAR(100),
    type VARCHAR(50),
    parent_id VARCHAR(32),
    description VARCHAR(500),
    path VARCHAR(500),
    order_num INTEGER,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    PRIMARY KEY (id)
);

COMMENT ON TABLE versioned_metadata_catalog IS '定版元数据目录表';
COMMENT ON COLUMN versioned_metadata_catalog.id IS '主键ID';
COMMENT ON COLUMN versioned_metadata_catalog.version_id IS '版本ID';
COMMENT ON COLUMN versioned_metadata_catalog.original_id IS '原始ID';
COMMENT ON COLUMN versioned_metadata_catalog.name IS '节点名称';
COMMENT ON COLUMN versioned_metadata_catalog.type IS '节点类型';
COMMENT ON COLUMN versioned_metadata_catalog.parent_id IS '父节点ID';
COMMENT ON COLUMN versioned_metadata_catalog.description IS '节点描述';
COMMENT ON COLUMN versioned_metadata_catalog.path IS '节点路径';
COMMENT ON COLUMN versioned_metadata_catalog.order_num IS '排序号';
COMMENT ON COLUMN versioned_metadata_catalog.create_time IS '创建时间';
COMMENT ON COLUMN versioned_metadata_catalog.update_time IS '更新时间';

-- 创建索引
CREATE INDEX idx_vmc_catalog_version_id ON versioned_metadata_catalog (version_id);
CREATE INDEX idx_vmc_catalog_original_id ON versioned_metadata_catalog (original_id);
CREATE INDEX idx_vmc_catalog_parent_id ON versioned_metadata_catalog (parent_id);

-- 定版元数据集合表
CREATE TABLE versioned_metadata_collection (
    id VARCHAR(32) NOT NULL,
    version_id VARCHAR(32) NOT NULL,
    original_id VARCHAR(32) NOT NULL,
    name VARCHAR(100),
    description VARCHAR(500),
    type VARCHAR(50),
    data_source_id VARCHAR(32),
    data_source_name VARCHAR(100),
    metadata_count INTEGER,
    creator_id VARCHAR(32),
    creator_name VARCHAR(100),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    lineage_enabled NUMBER(1),
    last_lineage_analysis_time TIMESTAMP,
    import_config CLOB,
    tags CLOB,
    catalog_id VARCHAR(32),
    matching_strategy CLOB,
    storage_strategy CLOB,
    schedule_config CLOB,
    notification_config CLOB,
    quality_rules CLOB,
    catalog_name VARCHAR(100),
    data_source_type VARCHAR(50),
    matching_strategy_desc VARCHAR(200),
    storage_strategy_desc VARCHAR(200),
    owner VARCHAR(100),
    PRIMARY KEY (id)
);

COMMENT ON TABLE versioned_metadata_collection IS '定版元数据集合表';
COMMENT ON COLUMN versioned_metadata_collection.id IS '主键ID';
COMMENT ON COLUMN versioned_metadata_collection.version_id IS '版本ID';
COMMENT ON COLUMN versioned_metadata_collection.original_id IS '原始ID';
COMMENT ON COLUMN versioned_metadata_collection.name IS '集合名称';
COMMENT ON COLUMN versioned_metadata_collection.description IS '集合描述';
COMMENT ON COLUMN versioned_metadata_collection.type IS '集合类型';
COMMENT ON COLUMN versioned_metadata_collection.data_source_id IS '数据源ID';
COMMENT ON COLUMN versioned_metadata_collection.data_source_name IS '数据源名称';
COMMENT ON COLUMN versioned_metadata_collection.metadata_count IS '元数据数量';
COMMENT ON COLUMN versioned_metadata_collection.creator_id IS '创建人ID';
COMMENT ON COLUMN versioned_metadata_collection.creator_name IS '创建人名称';
COMMENT ON COLUMN versioned_metadata_collection.create_time IS '创建时间';
COMMENT ON COLUMN versioned_metadata_collection.update_time IS '更新时间';
COMMENT ON COLUMN versioned_metadata_collection.lineage_enabled IS '是否启用血缘分析';
COMMENT ON COLUMN versioned_metadata_collection.last_lineage_analysis_time IS '最后一次血缘分析时间';
COMMENT ON COLUMN versioned_metadata_collection.import_config IS '导入配置（JSON格式）';
COMMENT ON COLUMN versioned_metadata_collection.tags IS '标签（JSON数组）';
COMMENT ON COLUMN versioned_metadata_collection.catalog_id IS '目录ID';
COMMENT ON COLUMN versioned_metadata_collection.matching_strategy IS '标准匹配策略';
COMMENT ON COLUMN versioned_metadata_collection.storage_strategy IS '入库策略';
COMMENT ON COLUMN versioned_metadata_collection.schedule_config IS '调度配置';
COMMENT ON COLUMN versioned_metadata_collection.notification_config IS '通知配置';
COMMENT ON COLUMN versioned_metadata_collection.quality_rules IS '质量规则配置';
COMMENT ON COLUMN versioned_metadata_collection.catalog_name IS '目录名称';
COMMENT ON COLUMN versioned_metadata_collection.data_source_type IS '数据源类型';
COMMENT ON COLUMN versioned_metadata_collection.matching_strategy_desc IS '匹配策略描述';
COMMENT ON COLUMN versioned_metadata_collection.storage_strategy_desc IS '入库策略描述';
COMMENT ON COLUMN versioned_metadata_collection.owner IS '责任人';

-- 创建索引
CREATE INDEX idx_vmc_version_id ON versioned_metadata_collection (version_id);
CREATE INDEX idx_vmc_original_id ON versioned_metadata_collection (original_id);
CREATE INDEX idx_vmc_catalog_id ON versioned_metadata_collection (catalog_id);

-- 定版元数据表结构表
CREATE TABLE versioned_metadata_table_structure (
    id VARCHAR(32) NOT NULL,
    version_id VARCHAR(32) NOT NULL,
    original_id VARCHAR(32) NOT NULL,
    collection_id VARCHAR(32),
    execution_id VARCHAR(32),
    table_name VARCHAR(100),
    table_type VARCHAR(50),
    table_comment VARCHAR(500),
    create_sql CLOB,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    version INTEGER,
    engine VARCHAR(50),
    charset VARCHAR(50),
    collation_rule VARCHAR(50),
    table_size BIGINT,
    row_count BIGINT,
    tablespace VARCHAR(100),
    import_status VARCHAR(20),
    db_name VARCHAR(100),
    owner VARCHAR(100),
    table_size_mb DECIMAL(18,2),
    table_create_time TIMESTAMP,
    table_update_time TIMESTAMP,
    table_ddl CLOB,
    column_count INTEGER,
    index_count INTEGER,
    constraint_count INTEGER,
    trigger_count INTEGER,
    partition_count INTEGER,
    storage_info CLOB,
    column_type_summary CLOB,
    index_info CLOB,
    primary_key_info CLOB,
    foreign_key_info CLOB,
    dependency_info CLOB,
    extended_info CLOB,
    PRIMARY KEY (id)
);

COMMENT ON TABLE versioned_metadata_table_structure IS '定版元数据表结构表';
COMMENT ON COLUMN versioned_metadata_table_structure.id IS '主键ID';
COMMENT ON COLUMN versioned_metadata_table_structure.version_id IS '版本ID';
COMMENT ON COLUMN versioned_metadata_table_structure.original_id IS '原始ID';
COMMENT ON COLUMN versioned_metadata_table_structure.collection_id IS '采集任务ID';
COMMENT ON COLUMN versioned_metadata_table_structure.execution_id IS '执行ID';
COMMENT ON COLUMN versioned_metadata_table_structure.table_name IS '表名';
COMMENT ON COLUMN versioned_metadata_table_structure.table_type IS '表类型';
COMMENT ON COLUMN versioned_metadata_table_structure.table_comment IS '表注释';
COMMENT ON COLUMN versioned_metadata_table_structure.create_sql IS '建表SQL';
COMMENT ON COLUMN versioned_metadata_table_structure.create_time IS '创建时间';
COMMENT ON COLUMN versioned_metadata_table_structure.update_time IS '更新时间';
COMMENT ON COLUMN versioned_metadata_table_structure.version IS '版本号';
COMMENT ON COLUMN versioned_metadata_table_structure.engine IS '存储引擎';
COMMENT ON COLUMN versioned_metadata_table_structure.charset IS '字符集';
COMMENT ON COLUMN versioned_metadata_table_structure.collation_rule IS '排序规则';
COMMENT ON COLUMN versioned_metadata_table_structure.table_size IS '表大小(字节)';
COMMENT ON COLUMN versioned_metadata_table_structure.row_count IS '行数';
COMMENT ON COLUMN versioned_metadata_table_structure.tablespace IS '表空间';
COMMENT ON COLUMN versioned_metadata_table_structure.import_status IS '导入状态';
COMMENT ON COLUMN versioned_metadata_table_structure.db_name IS '数据库名称';
COMMENT ON COLUMN versioned_metadata_table_structure.owner IS '表所有者';
COMMENT ON COLUMN versioned_metadata_table_structure.table_size_mb IS '表大小(MB)';
COMMENT ON COLUMN versioned_metadata_table_structure.table_create_time IS '表创建时间';
COMMENT ON COLUMN versioned_metadata_table_structure.table_update_time IS '表更新时间';
COMMENT ON COLUMN versioned_metadata_table_structure.table_ddl IS '表DDL';
COMMENT ON COLUMN versioned_metadata_table_structure.column_count IS '列数量';
COMMENT ON COLUMN versioned_metadata_table_structure.index_count IS '索引数量';
COMMENT ON COLUMN versioned_metadata_table_structure.constraint_count IS '约束数量';
COMMENT ON COLUMN versioned_metadata_table_structure.trigger_count IS '触发器数量';
COMMENT ON COLUMN versioned_metadata_table_structure.partition_count IS '分区数量';
COMMENT ON COLUMN versioned_metadata_table_structure.storage_info IS '存储信息(JSON)';
COMMENT ON COLUMN versioned_metadata_table_structure.column_type_summary IS '列类型摘要(JSON)';
COMMENT ON COLUMN versioned_metadata_table_structure.index_info IS '索引信息(JSON)';
COMMENT ON COLUMN versioned_metadata_table_structure.primary_key_info IS '主键信息(JSON)';
COMMENT ON COLUMN versioned_metadata_table_structure.foreign_key_info IS '外键信息(JSON)';
COMMENT ON COLUMN versioned_metadata_table_structure.dependency_info IS '依赖信息(JSON)';
COMMENT ON COLUMN versioned_metadata_table_structure.extended_info IS '扩展信息(JSON)';

-- 创建索引
CREATE INDEX idx_vmts_version_id ON versioned_metadata_table_structure (version_id);
CREATE INDEX idx_vmts_original_id ON versioned_metadata_table_structure (original_id);
CREATE INDEX idx_vmts_collection_id ON versioned_metadata_table_structure (collection_id);

-- 定版元数据字段信息表
CREATE TABLE versioned_metadata_field_info (
    id VARCHAR(32) NOT NULL,
    version_id VARCHAR(32) NOT NULL,
    original_id VARCHAR(32) NOT NULL,
    table_id VARCHAR(32),
    table_name VARCHAR(100),
    field_name VARCHAR(100),
    data_type VARCHAR(50),
    field_comment VARCHAR(500),
    is_primary_key NUMBER(1),
    nullable NUMBER(1),
    default_value VARCHAR(200),
    length INTEGER,
    precision INTEGER,
    scale INTEGER,
    ordinal_position INTEGER,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    is_auto_increment NUMBER(1),
    is_unique NUMBER(1),
    is_indexed NUMBER(1),
    is_foreign_key NUMBER(1),
    referenced_table_name VARCHAR(100),
    referenced_column_name VARCHAR(100),
    standard_id VARCHAR(32),
    standard_name VARCHAR(100),
    match_rate DECIMAL(5,2),
    sensitive_level VARCHAR(20),
    quality_rules CLOB,
    business_rules CLOB,
    tags VARCHAR(200),
    field_alias VARCHAR(100),
    field_category VARCHAR(50),
    field_source VARCHAR(100),
    is_calculated NUMBER(1),
    calculation_expression VARCHAR(500),
    sample_value VARCHAR(200),
    field_format VARCHAR(100),
    field_unit VARCHAR(50),
    field_range VARCHAR(200),
    field_enum VARCHAR(500),
    extended_info CLOB,
    PRIMARY KEY (id)
);

COMMENT ON TABLE versioned_metadata_field_info IS '定版元数据字段信息表';
COMMENT ON COLUMN versioned_metadata_field_info.id IS '主键ID';
COMMENT ON COLUMN versioned_metadata_field_info.version_id IS '版本ID';
COMMENT ON COLUMN versioned_metadata_field_info.original_id IS '原始ID';
COMMENT ON COLUMN versioned_metadata_field_info.table_id IS '表ID';
COMMENT ON COLUMN versioned_metadata_field_info.table_name IS '表名';
COMMENT ON COLUMN versioned_metadata_field_info.field_name IS '字段名';
COMMENT ON COLUMN versioned_metadata_field_info.data_type IS '字段类型';
COMMENT ON COLUMN versioned_metadata_field_info.field_comment IS '字段注释';
COMMENT ON COLUMN versioned_metadata_field_info.is_primary_key IS '是否主键';
COMMENT ON COLUMN versioned_metadata_field_info.nullable IS '是否允许为空';
COMMENT ON COLUMN versioned_metadata_field_info.default_value IS '默认值';
COMMENT ON COLUMN versioned_metadata_field_info.length IS '字段长度';
COMMENT ON COLUMN versioned_metadata_field_info.precision IS '字段精度';
COMMENT ON COLUMN versioned_metadata_field_info.scale IS '字段小数位';
COMMENT ON COLUMN versioned_metadata_field_info.ordinal_position IS '字段序号';
COMMENT ON COLUMN versioned_metadata_field_info.create_time IS '创建时间';
COMMENT ON COLUMN versioned_metadata_field_info.update_time IS '更新时间';
COMMENT ON COLUMN versioned_metadata_field_info.is_auto_increment IS '是否自增';
COMMENT ON COLUMN versioned_metadata_field_info.is_unique IS '是否唯一';
COMMENT ON COLUMN versioned_metadata_field_info.is_indexed IS '是否索引';
COMMENT ON COLUMN versioned_metadata_field_info.is_foreign_key IS '是否外键';
COMMENT ON COLUMN versioned_metadata_field_info.referenced_table_name IS '引用表';
COMMENT ON COLUMN versioned_metadata_field_info.referenced_column_name IS '引用字段';
COMMENT ON COLUMN versioned_metadata_field_info.standard_id IS '数据标准ID';
COMMENT ON COLUMN versioned_metadata_field_info.standard_name IS '数据标准名称';
COMMENT ON COLUMN versioned_metadata_field_info.match_rate IS '匹配度';
COMMENT ON COLUMN versioned_metadata_field_info.sensitive_level IS '敏感级别';
COMMENT ON COLUMN versioned_metadata_field_info.quality_rules IS '质量规则';
COMMENT ON COLUMN versioned_metadata_field_info.business_rules IS '业务规则';
COMMENT ON COLUMN versioned_metadata_field_info.tags IS '标签';
COMMENT ON COLUMN versioned_metadata_field_info.field_alias IS '字段别名';
COMMENT ON COLUMN versioned_metadata_field_info.field_category IS '字段分类';
COMMENT ON COLUMN versioned_metadata_field_info.field_source IS '字段来源';
COMMENT ON COLUMN versioned_metadata_field_info.is_calculated IS '是否计算字段';
COMMENT ON COLUMN versioned_metadata_field_info.calculation_expression IS '计算表达式';
COMMENT ON COLUMN versioned_metadata_field_info.sample_value IS '字段示例值';
COMMENT ON COLUMN versioned_metadata_field_info.field_format IS '字段格式';
COMMENT ON COLUMN versioned_metadata_field_info.field_unit IS '字段单位';
COMMENT ON COLUMN versioned_metadata_field_info.field_range IS '字段范围';
COMMENT ON COLUMN versioned_metadata_field_info.field_enum IS '字段枚举值';
COMMENT ON COLUMN versioned_metadata_field_info.extended_info IS '扩展信息';

-- 创建索引
CREATE INDEX idx_vmfi_version_id ON versioned_metadata_field_info (version_id);
CREATE INDEX idx_vmfi_original_id ON versioned_metadata_field_info (original_id);
CREATE INDEX idx_vmfi_table_id ON versioned_metadata_field_info (table_id);
CREATE INDEX idx_vmfi_table_name ON versioned_metadata_field_info (table_name);
CREATE INDEX idx_vmfi_field_name ON versioned_metadata_field_info (field_name); 