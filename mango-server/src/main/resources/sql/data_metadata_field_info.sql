-- 元数据字段信息表
create table data_metadata_field_info (
    -- 主键ID
    id varchar(64) not null,
    
    -- 采集任务ID
    collection_id varchar(64),
    
    -- 执行ID
    execution_id varchar(64),
    
    -- 表名
    table_name varchar(128),
    
    -- 字段名
    field_name varchar(128),
    
    -- 字段类型
    data_type varchar(64),
    
    -- 字段长度
    length integer,
    
    -- 字段精度
    precision integer,
    
    -- 字段小数位数
    scale integer,
    
    -- 是否可为空
    nullable integer,
    
    -- 字段注释
    comment varchar(512),
    
    -- 是否为主键
    is_primary_key integer default 0,
    
    -- 是否为外键
    is_foreign_key integer default 0,
    
    -- 是否唯一
    is_unique integer default 0,
    
    -- 是否索引
    is_indexed integer default 0,
    
    -- 是否自增
    is_auto_increment integer default 0,
    
    -- 默认值
    default_value varchar(256),
    
    -- 字段位置
    ordinal_position integer,
    
    -- 引用表名（外键）
    referenced_table_name varchar(128),
    
    -- 引用列名（外键）
    referenced_column_name varchar(128),
    
    -- 版本号
    version integer,
    
    -- 是否最新版本（1是，0否）
    is_latest integer default 1,
    
    -- 是否删除（0-未删除，1-已删除）
    is_deleted integer default 0,
    
    -- 创建时间
    create_time timestamp default sysdate,
    
    -- 更新时间
    update_time timestamp default sysdate,
    
    -- 设置主键约束
    constraint pk_data_metadata_field_info primary key (id)
);

-- 创建索引
create index idx_field_info_collection_id on data_metadata_field_info (collection_id);
create index idx_field_info_execution_id on data_metadata_field_info (execution_id);
create index idx_field_info_table_name on data_metadata_field_info (table_name);
create index idx_field_info_field_name on data_metadata_field_info (field_name);
create index idx_field_info_version on data_metadata_field_info (version);
create index idx_field_info_is_latest on data_metadata_field_info (is_latest);
create index idx_field_info_is_deleted on data_metadata_field_info (is_deleted);
create index idx_field_info_is_primary_key on data_metadata_field_info (is_primary_key);
create index idx_field_info_is_foreign_key on data_metadata_field_info (is_foreign_key);

-- 添加表约束
alter table data_metadata_field_info add constraint ck_field_info_is_deleted check (is_deleted in (0, 1));
alter table data_metadata_field_info add constraint ck_field_info_is_latest check (is_latest in (0, 1));
alter table data_metadata_field_info add constraint ck_field_info_is_primary_key check (is_primary_key in (0, 1));
alter table data_metadata_field_info add constraint ck_field_info_is_foreign_key check (is_foreign_key in (0, 1));
alter table data_metadata_field_info add constraint ck_field_info_is_unique check (is_unique in (0, 1));
alter table data_metadata_field_info add constraint ck_field_info_is_indexed check (is_indexed in (0, 1));
alter table data_metadata_field_info add constraint ck_field_info_is_auto_increment check (is_auto_increment in (0, 1));

-- 创建序列（用于生成自增ID）
create sequence seq_metadata_field_info_id
    start with 1
    increment by 1
    nocache;

-- 创建触发器（自动生成ID）
create or replace trigger trg_metadata_field_info_id
before insert on data_metadata_field_info
for each row
begin
    if :new.id is null then
        select 'FI' || to_char(sysdate, 'yyyymmdd') || lpad(seq_metadata_field_info_id.nextval, 6, '0')
        into :new.id
        from dual;
    end if;
end;
/

-- 创建触发器（自动更新更新时间）
create or replace trigger trg_metadata_field_info_update
before update on data_metadata_field_info
for each row
begin
    :new.update_time := sysdate;
end;
/

-- 添加表和列注释
comment on table data_metadata_field_info is '元数据字段信息表';
comment on column data_metadata_field_info.id is '主键ID';
comment on column data_metadata_field_info.collection_id is '采集任务ID';
comment on column data_metadata_field_info.execution_id is '执行ID';
comment on column data_metadata_field_info.table_name is '表名';
comment on column data_metadata_field_info.field_name is '字段名';
comment on column data_metadata_field_info.data_type is '字段类型';
comment on column data_metadata_field_info.length is '字段长度';
comment on column data_metadata_field_info.precision is '字段精度';
comment on column data_metadata_field_info.scale is '字段小数位数';
comment on column data_metadata_field_info.nullable is '是否可为空';
comment on column data_metadata_field_info.comment is '字段注释';
comment on column data_metadata_field_info.is_primary_key is '是否为主键';
comment on column data_metadata_field_info.is_foreign_key is '是否为外键';
comment on column data_metadata_field_info.is_unique is '是否唯一';
comment on column data_metadata_field_info.is_indexed is '是否索引';
comment on column data_metadata_field_info.is_auto_increment is '是否自增';
comment on column data_metadata_field_info.default_value is '默认值';
comment on column data_metadata_field_info.ordinal_position is '字段位置';
comment on column data_metadata_field_info.referenced_table_name is '引用表名（外键）';
comment on column data_metadata_field_info.referenced_column_name is '引用列名（外键）';
comment on column data_metadata_field_info.version is '版本号';
comment on column data_metadata_field_info.is_latest is '是否最新版本（1是，0否）';
comment on column data_metadata_field_info.is_deleted is '是否删除（0-未删除，1-已删除）';
comment on column data_metadata_field_info.create_time is '创建时间';
comment on column data_metadata_field_info.update_time is '更新时间';

-- 创建视图（查询最新版本的字段信息）
create or replace view v_metadata_field_info_latest as
select *
from data_metadata_field_info
where is_latest = 1 and is_deleted = 0; 