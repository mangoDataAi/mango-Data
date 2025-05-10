-- 创建元数据表结构信息表
create table data_metadata_table_structure (
    -- 主键ID
    id varchar(64) not null,
    
    -- 采集任务ID
    collection_id varchar(64) not null,
    
    -- 执行ID
    execution_id varchar(64) not null,
    
    -- 表名
    table_name varchar(128) not null,
    
    -- 表类型
    table_type varchar(32),
    
    -- 表注释
    table_comment varchar(1024),
    
    -- 建表SQL
    create_sql clob,
    
    -- 创建时间
    create_time timestamp,
    
    -- 更新时间
    update_time timestamp,
    
    -- 版本号
    version int,
    
    -- 是否最新版本（1是，0否）
    is_latest int,
    
    -- 是否删除（0-未删除，1-已删除）
    is_deleted int default 0,
    
    -- 存储引擎
    engine varchar(32),
    
    -- 字符集
    charset varchar(32),
    
    -- 排序规则
    collation_rule varchar(64),
    
    -- 表大小(字节)
    table_size bigint,
    
    -- 行数
    row_count bigint,
    
    -- 表空间
    tablespace varchar(64),
    
    -- 导入状态（auto-自动导入, review-待审核, process-待处理）
    import_status varchar(16),
    
    -- 数据库名称
    db_name varchar(64),
    
    -- 表所有者
    owner varchar(64),
    
    -- 表大小(MB)
    table_size_mb decimal(18,2),
    
    -- 表创建时间
    table_create_time timestamp,
    
    -- 表更新时间
    table_update_time timestamp,
    
    -- 表DDL
    table_ddl clob,
    
    -- 列数量
    column_count int,
    
    -- 索引数量
    index_count int,
    
    -- 约束数量
    constraint_count int,
    
    -- 触发器数量
    trigger_count int,
    
    -- 分区数量
    partition_count int,
    
    -- 存储信息(JSON)
    storage_info clob,
    
    -- 列类型摘要(JSON)
    column_type_summary clob,
    
    -- 索引信息(JSON)
    index_info clob,
    
    -- 主键信息(JSON)
    primary_key_info clob,
    
    -- 外键信息(JSON)
    foreign_key_info clob,
    
    -- 依赖信息(JSON)
    dependency_info clob,
    
    -- 扩展信息(JSON)
    extended_info clob,
    
    -- 设置主键约束
    constraint pk_metadata_table_structure primary key (id)
);

-- 添加表注释
comment on table data_metadata_table_structure is '元数据表结构信息表';

-- 添加列注释
comment on column data_metadata_table_structure.id is '主键ID';
comment on column data_metadata_table_structure.collection_id is '采集任务ID';
comment on column data_metadata_table_structure.execution_id is '执行ID';
comment on column data_metadata_table_structure.table_name is '表名';
comment on column data_metadata_table_structure.table_type is '表类型';
comment on column data_metadata_table_structure.table_comment is '表注释';
comment on column data_metadata_table_structure.create_sql is '建表SQL';
comment on column data_metadata_table_structure.create_time is '创建时间';
comment on column data_metadata_table_structure.update_time is '更新时间';
comment on column data_metadata_table_structure.version is '版本号';
comment on column data_metadata_table_structure.is_latest is '是否最新版本（1是，0否）';
comment on column data_metadata_table_structure.is_deleted is '是否删除（0-未删除，1-已删除）';
comment on column data_metadata_table_structure.engine is '存储引擎';
comment on column data_metadata_table_structure.charset is '字符集';
comment on column data_metadata_table_structure.collation_rule is '排序规则';
comment on column data_metadata_table_structure.table_size is '表大小(字节)';
comment on column data_metadata_table_structure.row_count is '行数';
comment on column data_metadata_table_structure.tablespace is '表空间';
comment on column data_metadata_table_structure.import_status is '导入状态（auto-自动导入, review-待审核, process-待处理）';
comment on column data_metadata_table_structure.db_name is '数据库名称';
comment on column data_metadata_table_structure.owner is '表所有者';
comment on column data_metadata_table_structure.table_size_mb is '表大小(MB)';
comment on column data_metadata_table_structure.table_create_time is '表创建时间';
comment on column data_metadata_table_structure.table_update_time is '表更新时间';
comment on column data_metadata_table_structure.table_ddl is '表DDL';
comment on column data_metadata_table_structure.column_count is '列数量';
comment on column data_metadata_table_structure.index_count is '索引数量';
comment on column data_metadata_table_structure.constraint_count is '约束数量';
comment on column data_metadata_table_structure.trigger_count is '触发器数量';
comment on column data_metadata_table_structure.partition_count is '分区数量';
comment on column data_metadata_table_structure.storage_info is '存储信息(JSON)';
comment on column data_metadata_table_structure.column_type_summary is '列类型摘要(JSON)';
comment on column data_metadata_table_structure.index_info is '索引信息(JSON)';
comment on column data_metadata_table_structure.primary_key_info is '主键信息(JSON)';
comment on column data_metadata_table_structure.foreign_key_info is '外键信息(JSON)';
comment on column data_metadata_table_structure.dependency_info is '依赖信息(JSON)';
comment on column data_metadata_table_structure.extended_info is '扩展信息(JSON)';

-- 创建索引
create index idx_mts_collection_id on data_metadata_table_structure (collection_id);
create index idx_mts_execution_id on data_metadata_table_structure (execution_id);
create index idx_mts_table_name on data_metadata_table_structure (table_name);
create index idx_mts_version on data_metadata_table_structure (version);
create index idx_mts_is_latest on data_metadata_table_structure (is_latest); 