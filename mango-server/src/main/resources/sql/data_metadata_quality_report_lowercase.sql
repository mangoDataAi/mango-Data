-- 元数据质量报告表
create table data_metadata_quality_report (
    -- 主键ID
    id varchar(64) not null,
    
    -- 采集任务ID
    collection_id varchar(64),
    
    -- 执行ID
    execution_id varchar(64),
    
    -- 版本号
    version integer,
    
    -- 表名
    table_name varchar(128),
    
    -- 总行数
    total_rows bigint,
    
    -- 违规数量
    violation_count integer,
    
    -- 违规详情（JSON格式）
    violation_details clob,
    
    -- 创建时间
    create_time timestamp default sysdate,
    
    -- 更新时间
    update_time timestamp default sysdate,
    
    -- 是否删除（0-未删除，1-已删除）
    is_deleted integer default 0,
    
    -- 是否最新版本（0-否，1-是）
    is_latest integer default 1,
    
    -- 设置主键约束
    constraint pk_data_metadata_quality_report primary key (id)
);

-- 创建索引
create index idx_quality_report_collection_id on data_metadata_quality_report (collection_id);
create index idx_quality_report_execution_id on data_metadata_quality_report (execution_id);
create index idx_quality_report_table_name on data_metadata_quality_report (table_name);
create index idx_quality_report_version on data_metadata_quality_report (version);
create index idx_quality_report_is_latest on data_metadata_quality_report (is_latest);
create index idx_quality_report_is_deleted on data_metadata_quality_report (is_deleted);

-- 添加表约束
alter table data_metadata_quality_report add constraint ck_quality_report_is_deleted check (is_deleted in (0, 1));
alter table data_metadata_quality_report add constraint ck_quality_report_is_latest check (is_latest in (0, 1));

-- 创建序列（用于生成自增ID）
create sequence seq_metadata_quality_report_id
    start with 1
    increment by 1
    nocache;

-- 创建触发器（自动生成ID）
create or replace trigger trg_metadata_quality_report_id
before insert on data_metadata_quality_report
for each row
begin
    if :new.id is null then
        select 'QR' || to_char(sysdate, 'yyyymmdd') || lpad(seq_metadata_quality_report_id.nextval, 6, '0')
        into :new.id
        from dual;
    end if;
end;
/

-- 创建触发器（自动更新更新时间）
create or replace trigger trg_metadata_quality_report_update
before update on data_metadata_quality_report
for each row
begin
    :new.update_time := sysdate;
end;
/

-- 添加表和列注释
comment on table data_metadata_quality_report is '元数据质量报告表';
comment on column data_metadata_quality_report.id is '主键ID';
comment on column data_metadata_quality_report.collection_id is '采集任务ID';
comment on column data_metadata_quality_report.execution_id is '执行ID';
comment on column data_metadata_quality_report.version is '版本号';
comment on column data_metadata_quality_report.table_name is '表名';
comment on column data_metadata_quality_report.total_rows is '总行数';
comment on column data_metadata_quality_report.violation_count is '违规数量';
comment on column data_metadata_quality_report.violation_details is '违规详情（JSON格式）';
comment on column data_metadata_quality_report.create_time is '创建时间';
comment on column data_metadata_quality_report.update_time is '更新时间';
comment on column data_metadata_quality_report.is_deleted is '是否删除（0-未删除，1-已删除）';
comment on column data_metadata_quality_report.is_latest is '是否最新版本（0-否，1-是）';

-- 创建视图（查询最新版本的质量报告）
create or replace view v_metadata_quality_report_latest as
select *
from data_metadata_quality_report
where is_latest = 1 and is_deleted = 0;

-- 创建存储过程（更新质量报告版本）
create or replace procedure proc_update_quality_report_version(
    p_collection_id in varchar2,
    p_execution_id in varchar2
) as
begin
    -- 将所有旧版本标记为非最新
    update data_metadata_quality_report
    set is_latest = 0
    where collection_id = p_collection_id
    and execution_id <> p_execution_id
    and is_latest = 1;
    
    commit;
end;
/

-- 创建函数（获取最大版本号）
create or replace function func_get_quality_report_max_version(
    p_collection_id in varchar2
) return number as
    v_max_version number;
begin
    -- 查询最大版本号
    select coalesce(max(version), 0)
    into v_max_version
    from data_metadata_quality_report
    where collection_id = p_collection_id
    and is_deleted = 0;
    
    return v_max_version;
end;
/

-- 插入示例数据
insert into data_metadata_quality_report (
    id, collection_id, execution_id, version, table_name,
    total_rows, violation_count, violation_details,
    create_time, update_time, is_deleted, is_latest
) values (
    'QR20230101000001', 'COL20230101000001', 'EXE20230101000001', 1, 'CUSTOMER',
    1000, 5, '{"null_values": 2, "duplicate_values": 3, "details": [{"column": "email", "error": "null_value", "count": 2}, {"column": "customer_id", "error": "duplicate_value", "count": 3}]}',
    sysdate, sysdate, 0, 1
);

insert into data_metadata_quality_report (
    id, collection_id, execution_id, version, table_name,
    total_rows, violation_count, violation_details,
    create_time, update_time, is_deleted, is_latest
) values (
    'QR20230101000002', 'COL20230101000001', 'EXE20230101000001', 1, 'ORDER',
    500, 2, '{"null_values": 1, "invalid_date": 1, "details": [{"column": "customer_id", "error": "null_value", "count": 1}, {"column": "order_date", "error": "invalid_date", "count": 1}]}',
    sysdate, sysdate, 0, 1
); 