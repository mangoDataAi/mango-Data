-- 元数据表结构信息表
create table data_metadata_table_structure (
    -- 主键ID
    id varchar(64) not null,
    
    -- 采集任务ID
    collection_id varchar(64),
    
    -- 执行ID
    execution_id varchar(64),
    
    -- 表名
    table_name varchar(128),
    
    -- 表类型
    table_type varchar(32),
    
    -- 表注释
    table_comment varchar(512),
    
    -- 建表SQL
    create_sql clob,
    
    -- 创建时间
    create_time timestamp default sysdate,
    
    -- 更新时间
    update_time timestamp default sysdate,
    
    -- 版本号
    version integer,
    
    -- 是否最新版本（1是，0否）
    is_latest integer default 1,
    
    -- 是否删除（0-未删除，1-已删除）
    is_deleted integer default 0,
    
    -- 设置主键约束
    constraint pk_data_metadata_table_structure primary key (id)
);

-- 创建索引
create index idx_table_structure_collection_id on data_metadata_table_structure (collection_id);
create index idx_table_structure_execution_id on data_metadata_table_structure (execution_id);
create index idx_table_structure_table_name on data_metadata_table_structure (table_name);
create index idx_table_structure_version on data_metadata_table_structure (version);
create index idx_table_structure_is_latest on data_metadata_table_structure (is_latest);
create index idx_table_structure_is_deleted on data_metadata_table_structure (is_deleted);

-- 添加表约束
alter table data_metadata_table_structure add constraint ck_table_structure_is_deleted check (is_deleted in (0, 1));
alter table data_metadata_table_structure add constraint ck_table_structure_is_latest check (is_latest in (0, 1));

-- 创建序列（用于生成自增ID）
create sequence seq_metadata_table_structure_id
    start with 1
    increment by 1
    nocache;

-- 创建触发器（自动生成ID）
create or replace trigger trg_metadata_table_structure_id
before insert on data_metadata_table_structure
for each row
begin
    if :new.id is null then
        select 'TS' || to_char(sysdate, 'yyyymmdd') || lpad(seq_metadata_table_structure_id.nextval, 6, '0')
        into :new.id
        from dual;
    end if;
end;
/

-- 创建触发器（自动更新更新时间）
create or replace trigger trg_metadata_table_structure_update
before update on data_metadata_table_structure
for each row
begin
    :new.update_time := sysdate;
end;
/

-- 添加表和列注释
comment on table data_metadata_table_structure is '元数据表结构信息表';
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

-- 创建视图（查询最新版本的表结构信息）
create or replace view v_metadata_table_structure_latest as
select *
from data_metadata_table_structure
where is_latest = 1 and is_deleted = 0;

-- 创建存储过程（更新表结构版本）
create or replace procedure proc_update_table_structure_version(
    p_collection_id in varchar2,
    p_execution_id in varchar2
) as
begin
    -- 将所有旧版本标记为非最新
    update data_metadata_table_structure
    set is_latest = 0
    where collection_id = p_collection_id
    and execution_id <> p_execution_id
    and is_latest = 1;
    
    commit;
end;
/

-- 创建函数（获取最大版本号）
create or replace function func_get_table_structure_max_version(
    p_collection_id in varchar2
) return number as
    v_max_version number;
begin
    -- 查询最大版本号
    select coalesce(max(version), 0)
    into v_max_version
    from data_metadata_table_structure
    where collection_id = p_collection_id
    and is_deleted = 0;
    
    return v_max_version;
end;
/

-- 插入示例数据
insert into data_metadata_table_structure (
    id, collection_id, execution_id, table_name, table_type,
    table_comment, create_sql, version, is_latest, is_deleted,
    create_time, update_time
) values (
    'TS20230101000001', 'COL20230101000001', 'EXE20230101000001', 'CUSTOMER', 'TABLE',
    '客户信息表', 'create table customer (id varchar(64) primary key, name varchar(100), phone varchar(20))', 
    1, 1, 0, sysdate, sysdate
);

insert into data_metadata_table_structure (
    id, collection_id, execution_id, table_name, table_type,
    table_comment, create_sql, version, is_latest, is_deleted,
    create_time, update_time
) values (
    'TS20230101000002', 'COL20230101000001', 'EXE20230101000001', 'ORDER', 'TABLE',
    '订单信息表', 'create table order (id varchar(64) primary key, customer_id varchar(64), order_date timestamp)', 
    1, 1, 0, sysdate, sysdate
); 