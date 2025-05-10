-- 创建定版元数据字段信息表
CREATE TABLE versioned_metadata_field_info (
    -- 主键
    id VARCHAR2(64) NOT NULL,
    -- 版本ID
    version_id VARCHAR2(64),
    -- 原始ID
    original_id VARCHAR2(64),
    -- 集合ID
    collection_id VARCHAR2(64),
    -- 执行ID
    execution_id VARCHAR2(64),
    -- 表名
    table_name VARCHAR2(255),
    -- 字段名
    field_name VARCHAR2(255),
    -- 数据类型
    data_type VARCHAR2(50),
    -- 长度
    length NUMBER(10),
    -- 精度
    precision NUMBER(10),
    -- 小数位数
    scale NUMBER(10),
    -- 是否可空
    nullable NUMBER(1),
    -- 字段注释
    field_comment CLOB,
    -- 是否主键
    is_primary_key NUMBER(1),
    -- 是否外键
    is_foreign_key NUMBER(1),
    -- 是否唯一
    is_unique NUMBER(1),
    -- 是否索引
    is_indexed NUMBER(1),
    -- 是否自增
    is_auto_increment NUMBER(1),
    -- 默认值
    default_value VARCHAR2(255),
    -- 字段位置
    ordinal_position NUMBER(10),
    -- 引用表名
    referenced_table_name VARCHAR2(255),
    -- 引用列名
    referenced_column_name VARCHAR2(255),
    -- 版本号
    version VARCHAR2(50),
    -- 是否最新
    is_latest NUMBER(1),
    -- 创建时间
    create_time TIMESTAMP(6),
    -- 更新时间
    update_time TIMESTAMP(6),
    -- 额外信息
    extra VARCHAR2(255),
    -- 字符长度
    character_length NUMBER(10),
    -- 数值精度
    numeric_precision NUMBER(10),
    -- 数值小数位数
    numeric_scale NUMBER(10),
    -- 匹配得分
    match_score NUMBER(10,4),
    -- 匹配标准
    matched_standard VARCHAR2(255),
    -- 字符集
    charset VARCHAR2(50),
    -- 排序规则
    sort_rule VARCHAR2(50),
    -- 索引类型
    index_type VARCHAR2(50),
    -- 索引名称
    index_name VARCHAR2(255),
    -- 去重计数
    distinct_count NUMBER(19),
    -- 空值计数
    null_count NUMBER(19),
    -- 分布信息（JSON）
    distribution CLOB,
    -- 值域范围（JSON）
    value_range CLOB,
    -- 准确度
    accuracy NUMBER(10,4),
    -- 数据格式
    data_format VARCHAR2(255),
    -- 样本值（JSON）
    sample_values CLOB,
    -- 高频值（JSON）
    top_values CLOB,
    -- 是否删除
    is_deleted NUMBER(1) DEFAULT 0,
    
    -- 主键约束
    CONSTRAINT pk_versioned_metadata_field_info PRIMARY KEY (id)
);

-- 创建定版元数据目录表
CREATE TABLE versioned_metadata_catalog (
    -- 主键
    id VARCHAR2(64) NOT NULL,
    -- 版本ID
    version_id VARCHAR2(64),
    -- 原始ID
    original_id VARCHAR2(64),
    -- 节点名称
    name VARCHAR2(255),
    -- 节点类型
    type VARCHAR2(50),
    -- 父节点ID
    parent_id VARCHAR2(64),
    -- 节点描述
    description CLOB,
    -- 节点路径
    path VARCHAR2(1000),
    -- 排序号
    order_num NUMBER(10),
    -- 创建时间
    create_time TIMESTAMP(6),
    -- 更新时间
    update_time TIMESTAMP(6),
    
    -- 主键约束
    CONSTRAINT pk_versioned_metadata_catalog PRIMARY KEY (id)
);

-- 创建字段信息表索引
CREATE INDEX idx_vmfi_version_id ON versioned_metadata_field_info (version_id);
CREATE INDEX idx_vmfi_collection_id ON versioned_metadata_field_info (collection_id);
CREATE INDEX idx_vmfi_table_name ON versioned_metadata_field_info (table_name);
CREATE INDEX idx_vmfi_field_name ON versioned_metadata_field_info (field_name);
CREATE INDEX idx_vmfi_original_id ON versioned_metadata_field_info (original_id);

-- 创建目录表索引
CREATE INDEX idx_vmc_version_id ON versioned_metadata_catalog (version_id);
CREATE INDEX idx_vmc_parent_id ON versioned_metadata_catalog (parent_id);
CREATE INDEX idx_vmc_original_id ON versioned_metadata_catalog (original_id);
CREATE INDEX idx_vmc_path ON versioned_metadata_catalog (path);

-- 添加字段信息表注释
COMMENT ON TABLE versioned_metadata_field_info IS '定版元数据字段信息表';
COMMENT ON COLUMN versioned_metadata_field_info.id IS '主键';
COMMENT ON COLUMN versioned_metadata_field_info.version_id IS '版本ID';
COMMENT ON COLUMN versioned_metadata_field_info.original_id IS '原始ID';
COMMENT ON COLUMN versioned_metadata_field_info.collection_id IS '集合ID';
COMMENT ON COLUMN versioned_metadata_field_info.execution_id IS '执行ID';
COMMENT ON COLUMN versioned_metadata_field_info.table_name IS '表名';
COMMENT ON COLUMN versioned_metadata_field_info.field_name IS '字段名';
COMMENT ON COLUMN versioned_metadata_field_info.data_type IS '数据类型';
COMMENT ON COLUMN versioned_metadata_field_info.length IS '长度';
COMMENT ON COLUMN versioned_metadata_field_info.precision IS '精度';
COMMENT ON COLUMN versioned_metadata_field_info.scale IS '小数位数';
COMMENT ON COLUMN versioned_metadata_field_info.nullable IS '是否可空';
COMMENT ON COLUMN versioned_metadata_field_info.field_comment IS '字段注释';
COMMENT ON COLUMN versioned_metadata_field_info.is_primary_key IS '是否主键';
COMMENT ON COLUMN versioned_metadata_field_info.is_foreign_key IS '是否外键';
COMMENT ON COLUMN versioned_metadata_field_info.is_unique IS '是否唯一';
COMMENT ON COLUMN versioned_metadata_field_info.is_indexed IS '是否索引';
COMMENT ON COLUMN versioned_metadata_field_info.is_auto_increment IS '是否自增';
COMMENT ON COLUMN versioned_metadata_field_info.default_value IS '默认值';
COMMENT ON COLUMN versioned_metadata_field_info.ordinal_position IS '字段位置';
COMMENT ON COLUMN versioned_metadata_field_info.referenced_table_name IS '引用表名';
COMMENT ON COLUMN versioned_metadata_field_info.referenced_column_name IS '引用列名';
COMMENT ON COLUMN versioned_metadata_field_info.version IS '版本号';
COMMENT ON COLUMN versioned_metadata_field_info.is_latest IS '是否最新';
COMMENT ON COLUMN versioned_metadata_field_info.create_time IS '创建时间';
COMMENT ON COLUMN versioned_metadata_field_info.update_time IS '更新时间';
COMMENT ON COLUMN versioned_metadata_field_info.extra IS '额外信息';
COMMENT ON COLUMN versioned_metadata_field_info.character_length IS '字符长度';
COMMENT ON COLUMN versioned_metadata_field_info.numeric_precision IS '数值精度';
COMMENT ON COLUMN versioned_metadata_field_info.numeric_scale IS '数值小数位数';
COMMENT ON COLUMN versioned_metadata_field_info.match_score IS '匹配得分';
COMMENT ON COLUMN versioned_metadata_field_info.matched_standard IS '匹配标准';
COMMENT ON COLUMN versioned_metadata_field_info.charset IS '字符集';
COMMENT ON COLUMN versioned_metadata_field_info.sort_rule IS '排序规则';
COMMENT ON COLUMN versioned_metadata_field_info.index_type IS '索引类型';
COMMENT ON COLUMN versioned_metadata_field_info.index_name IS '索引名称';
COMMENT ON COLUMN versioned_metadata_field_info.distinct_count IS '去重计数';
COMMENT ON COLUMN versioned_metadata_field_info.null_count IS '空值计数';
COMMENT ON COLUMN versioned_metadata_field_info.distribution IS '分布信息（JSON）';
COMMENT ON COLUMN versioned_metadata_field_info.value_range IS '值域范围（JSON）';
COMMENT ON COLUMN versioned_metadata_field_info.accuracy IS '准确度';
COMMENT ON COLUMN versioned_metadata_field_info.data_format IS '数据格式';
COMMENT ON COLUMN versioned_metadata_field_info.sample_values IS '样本值（JSON）';
COMMENT ON COLUMN versioned_metadata_field_info.top_values IS '高频值（JSON）';
COMMENT ON COLUMN versioned_metadata_field_info.is_deleted IS '是否删除';

-- 添加目录表注释
COMMENT ON TABLE versioned_metadata_catalog IS '定版元数据目录表';
COMMENT ON COLUMN versioned_metadata_catalog.id IS '主键';
COMMENT ON COLUMN versioned_metadata_catalog.version_id IS '版本ID';
COMMENT ON COLUMN versioned_metadata_catalog.original_id IS '原始ID';
COMMENT ON COLUMN versioned_metadata_catalog.name IS '节点名称';
COMMENT ON COLUMN versioned_metadata_catalog.type IS '节点类型：datasource(数据源)、database(数据库)、table(表)、view(视图)';
COMMENT ON COLUMN versioned_metadata_catalog.parent_id IS '父节点ID';
COMMENT ON COLUMN versioned_metadata_catalog.description IS '节点描述';
COMMENT ON COLUMN versioned_metadata_catalog.path IS '节点路径';
COMMENT ON COLUMN versioned_metadata_catalog.order_num IS '排序号';
COMMENT ON COLUMN versioned_metadata_catalog.create_time IS '创建时间';
COMMENT ON COLUMN versioned_metadata_catalog.update_time IS '更新时间'; 