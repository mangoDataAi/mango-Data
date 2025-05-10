-- 创建定版元数据表结构信息表
CREATE TABLE versioned_metadata_table_structure (
    -- 主键
    id VARCHAR2(64) NOT NULL,
    -- 版本ID
    version_id VARCHAR2(64),
    -- 原始ID
    original_id VARCHAR2(64),
    -- 采集任务ID
    collection_id VARCHAR2(64),
    -- 执行ID
    execution_id VARCHAR2(64),
    -- 表名
    table_name VARCHAR2(255),
    -- 表类型
    table_type VARCHAR2(50),
    -- 表注释
    table_comment CLOB,
    -- 建表SQL
    create_sql CLOB,
    -- 行数
    row_count NUMBER(19),
    -- 表大小(字节)
    table_size NUMBER(19),
    -- 表大小(MB)
    table_size_mb NUMBER(10,2),
    -- 创建时间
    create_time TIMESTAMP(6),
    -- 更新时间
    update_time TIMESTAMP(6),
    -- 存储引擎
    engine VARCHAR2(50),
    -- 版本号
    version VARCHAR2(50),
    -- 字符集
    charset VARCHAR2(50),
    -- 排序规则
    collation_rule VARCHAR2(50),
    -- 表空间
    tablespace VARCHAR2(255),
    -- 数据库名称
    db_name VARCHAR2(255),
    -- 表所有者
    owner VARCHAR2(255),
    -- 表创建时间
    table_create_time TIMESTAMP(6),
    -- 表更新时间
    table_update_time TIMESTAMP(6),
    -- 表DDL
    table_ddl CLOB,
    -- 列数量
    column_count NUMBER(10),
    -- 索引数量
    index_count NUMBER(10),
    -- 约束数量
    constraint_count NUMBER(10),
    -- 触发器数量
    trigger_count NUMBER(10),
    -- 分区数量
    partition_count NUMBER(10),
    -- 存储信息(JSON)
    storage_info CLOB,
    -- 列类型摘要(JSON)
    column_type_summary CLOB,
    -- 索引信息(JSON)
    index_info CLOB,
    -- 主键信息(JSON)
    primary_key_info CLOB,
    -- 外键信息(JSON)
    foreign_key_info CLOB,
    -- 依赖信息(JSON)
    dependency_info CLOB,
    -- 扩展信息(JSON)
    extended_info CLOB,
    -- 是否删除
    is_deleted NUMBER(1) DEFAULT 0,
    
    -- 主键约束
    CONSTRAINT pk_versioned_metadata_table_structure PRIMARY KEY (id)
);

-- 创建索引
CREATE INDEX idx_vmts_version_id ON versioned_metadata_table_structure (version_id);
CREATE INDEX idx_vmts_collection_id ON versioned_metadata_table_structure (collection_id);
CREATE INDEX idx_vmts_table_name ON versioned_metadata_table_structure (table_name);
CREATE INDEX idx_vmts_original_id ON versioned_metadata_table_structure (original_id);
CREATE INDEX idx_vmts_db_name ON versioned_metadata_table_structure (db_name);

-- 添加表注释
COMMENT ON TABLE versioned_metadata_table_structure IS '定版元数据表结构信息表';

-- 添加字段注释
COMMENT ON COLUMN versioned_metadata_table_structure.id IS '主键ID';
COMMENT ON COLUMN versioned_metadata_table_structure.version_id IS '版本ID';
COMMENT ON COLUMN versioned_metadata_table_structure.original_id IS '原始ID';
COMMENT ON COLUMN versioned_metadata_table_structure.collection_id IS '采集任务ID';
COMMENT ON COLUMN versioned_metadata_table_structure.execution_id IS '执行ID';
COMMENT ON COLUMN versioned_metadata_table_structure.table_name IS '表名';
COMMENT ON COLUMN versioned_metadata_table_structure.table_type IS '表类型';
COMMENT ON COLUMN versioned_metadata_table_structure.table_comment IS '表注释';
COMMENT ON COLUMN versioned_metadata_table_structure.create_sql IS '建表SQL';
COMMENT ON COLUMN versioned_metadata_table_structure.row_count IS '行数';
COMMENT ON COLUMN versioned_metadata_table_structure.table_size IS '表大小(字节)';
COMMENT ON COLUMN versioned_metadata_table_structure.table_size_mb IS '表大小(MB)';
COMMENT ON COLUMN versioned_metadata_table_structure.create_time IS '创建时间';
COMMENT ON COLUMN versioned_metadata_table_structure.update_time IS '更新时间';
COMMENT ON COLUMN versioned_metadata_table_structure.engine IS '存储引擎';
COMMENT ON COLUMN versioned_metadata_table_structure.version IS '版本号';
COMMENT ON COLUMN versioned_metadata_table_structure.charset IS '字符集';
COMMENT ON COLUMN versioned_metadata_table_structure.collation_rule IS '排序规则';
COMMENT ON COLUMN versioned_metadata_table_structure.tablespace IS '表空间';
COMMENT ON COLUMN versioned_metadata_table_structure.db_name IS '数据库名称';
COMMENT ON COLUMN versioned_metadata_table_structure.owner IS '表所有者';
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
COMMENT ON COLUMN versioned_metadata_table_structure.is_deleted IS '是否删除'; 