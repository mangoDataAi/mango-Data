-- 元数据血缘关系表
create table data_metadata_lineage (
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
    
    -- 外键名称
    fk_name varchar(128),
    
    -- 外键列名
    fk_column_name varchar(128),
    
    -- 主键表名
    pk_table_name varchar(128),
    
    -- 主键列名
    pk_column_name varchar(128),
    
    -- 源元数据ID
    source_metadata_id varchar(64),
    
    -- 目标元数据ID
    target_metadata_id varchar(64),
    
    -- 关系类型
    relation_type varchar(32),
    
    -- 关系描述
    description varchar(512),
    
    -- 关系权重
    weight integer default 1,
    
    -- 创建时间
    create_time timestamp default sysdate,
    
    -- 更新时间
    update_time timestamp default sysdate,
    
    -- 是否删除（0-未删除，1-已删除）
    is_deleted integer default 0,
    
    -- 是否最新版本（0-否，1-是）
    is_latest integer default 1,
    
    -- 创建人ID
    creator_id varchar(64),
    
    -- 创建人姓名
    creator_name varchar(64),
    
    -- 更新人ID
    updater_id varchar(64),
    
    -- 更新人姓名
    updater_name varchar(64),
    
    -- 设置主键约束
    constraint pk_data_metadata_lineage primary key (id)
);

-- 创建索引
create index idx_metadata_lineage_collection_id on data_metadata_lineage (collection_id);
create index idx_metadata_lineage_source_id on data_metadata_lineage (source_metadata_id);
create index idx_metadata_lineage_target_id on data_metadata_lineage (target_metadata_id);
create index idx_metadata_lineage_table_name on data_metadata_lineage (table_name);
create index idx_metadata_lineage_version on data_metadata_lineage (version);
create index idx_metadata_lineage_is_latest on data_metadata_lineage (is_latest);
create index idx_metadata_lineage_is_deleted on data_metadata_lineage (is_deleted);

-- 添加表约束
alter table data_metadata_lineage add constraint ck_metadata_lineage_is_deleted check (is_deleted in (0, 1));
alter table data_metadata_lineage add constraint ck_metadata_lineage_is_latest check (is_latest in (0, 1));
alter table data_metadata_lineage add constraint ck_metadata_lineage_weight check (weight > 0);

-- 创建序列（用于生成自增ID）
create sequence seq_metadata_lineage_id
    start with 1
    increment by 1
    nocache;

-- 创建触发器（自动生成ID）
create or replace trigger trg_metadata_lineage_id
before insert on data_metadata_lineage
for each row
begin
    if :new.id is null then
        select 'ML' || to_char(sysdate, 'yyyymmdd') || lpad(seq_metadata_lineage_id.nextval, 6, '0')
        into :new.id
        from dual;
    end if;
end;
/

-- 创建触发器（自动更新更新时间）
create or replace trigger trg_metadata_lineage_update
before update on data_metadata_lineage
for each row
begin
    :new.update_time := sysdate;
end;
/

-- 添加表和列注释
comment on table data_metadata_lineage is '元数据血缘关系表';
comment on column data_metadata_lineage.id is '主键ID';
comment on column data_metadata_lineage.collection_id is '采集任务ID';
comment on column data_metadata_lineage.execution_id is '执行ID';
comment on column data_metadata_lineage.version is '版本号';
comment on column data_metadata_lineage.table_name is '表名';
comment on column data_metadata_lineage.fk_name is '外键名称';
comment on column data_metadata_lineage.fk_column_name is '外键列名';
comment on column data_metadata_lineage.pk_table_name is '主键表名';
comment on column data_metadata_lineage.pk_column_name is '主键列名';
comment on column data_metadata_lineage.source_metadata_id is '源元数据ID';
comment on column data_metadata_lineage.target_metadata_id is '目标元数据ID';
comment on column data_metadata_lineage.relation_type is '关系类型';
comment on column data_metadata_lineage.description is '关系描述';
comment on column data_metadata_lineage.weight is '关系权重';
comment on column data_metadata_lineage.create_time is '创建时间';
comment on column data_metadata_lineage.update_time is '更新时间';
comment on column data_metadata_lineage.is_deleted is '是否删除（0-未删除，1-已删除）';
comment on column data_metadata_lineage.is_latest is '是否最新版本（0-否，1-是）';
comment on column data_metadata_lineage.creator_id is '创建人ID';
comment on column data_metadata_lineage.creator_name is '创建人姓名';
comment on column data_metadata_lineage.updater_id is '更新人ID';
comment on column data_metadata_lineage.updater_name is '更新人姓名';

-- 创建视图（查询最新版本的血缘关系）
create or replace view v_metadata_lineage_latest as
select *
from data_metadata_lineage
where is_latest = 1 and is_deleted = 0;

-- 创建存储过程（更新血缘关系版本）
create or replace procedure proc_update_lineage_version(
    p_collection_id in varchar2,
    p_execution_id in varchar2
) as
begin
    -- 将所有旧版本标记为非最新
    update data_metadata_lineage
    set is_latest = 0
    where collection_id = p_collection_id
    and execution_id <> p_execution_id
    and is_latest = 1;
    
    commit;
end;
/

-- 创建函数（获取最大版本号）
create or replace function func_get_lineage_max_version(
    p_collection_id in varchar2
) return number as
    v_max_version number;
begin
    -- 查询最大版本号
    select coalesce(max(version), 0)
    into v_max_version
    from data_metadata_lineage
    where collection_id = p_collection_id
    and is_deleted = 0;
    
    return v_max_version;
end;
/

-- 创建查询血缘关系的存储过程
create or replace procedure proc_get_lineage(
    p_metadata_id in varchar2,
    p_direction in varchar2, -- 'UPSTREAM' or 'DOWNSTREAM'
    p_depth in number default 1
) as
begin
    if p_direction = 'UPSTREAM' then
        -- 查询上游血缘关系
        with recursive lineage_cte(source_id, target_id, level) as (
            -- 基础查询
            select source_metadata_id, target_metadata_id, 1
            from data_metadata_lineage
            where target_metadata_id = p_metadata_id
            and is_deleted = 0
            and is_latest = 1
            
            union all
            
            -- 递归查询
            select l.source_metadata_id, l.target_metadata_id, cte.level + 1
            from data_metadata_lineage l
            join lineage_cte cte on l.target_metadata_id = cte.source_id
            where l.is_deleted = 0
            and l.is_latest = 1
            and cte.level < p_depth
        )
        select * from lineage_cte;
    else
        -- 查询下游血缘关系
        with recursive lineage_cte(source_id, target_id, level) as (
            -- 基础查询
            select source_metadata_id, target_metadata_id, 1
            from data_metadata_lineage
            where source_metadata_id = p_metadata_id
            and is_deleted = 0
            and is_latest = 1
            
            union all
            
            -- 递归查询
            select l.source_metadata_id, l.target_metadata_id, cte.level + 1
            from data_metadata_lineage l
            join lineage_cte cte on l.source_metadata_id = cte.target_id
            where l.is_deleted = 0
            and l.is_latest = 1
            and cte.level < p_depth
        )
        select * from lineage_cte;
    end if;
end;
/

-- 创建完整血缘关系图的存储过程
create or replace procedure proc_get_complete_lineage_graph(
    p_metadata_id in varchar2
) as
begin
    -- 查询上下游血缘关系
    with upstream as (
        -- 上游血缘关系
        with recursive lineage_cte(source_id, target_id, level) as (
            -- 基础查询
            select source_metadata_id, target_metadata_id, 1
            from data_metadata_lineage
            where target_metadata_id = p_metadata_id
            and is_deleted = 0
            and is_latest = 1
            
            union all
            
            -- 递归查询
            select l.source_metadata_id, l.target_metadata_id, cte.level + 1
            from data_metadata_lineage l
            join lineage_cte cte on l.target_metadata_id = cte.source_id
            where l.is_deleted = 0
            and l.is_latest = 1
            and cte.level < 3
        )
        select source_id, target_id, level, 'UPSTREAM' as direction
        from lineage_cte
    ),
    downstream as (
        -- 下游血缘关系
        with recursive lineage_cte(source_id, target_id, level) as (
            -- 基础查询
            select source_metadata_id, target_metadata_id, 1
            from data_metadata_lineage
            where source_metadata_id = p_metadata_id
            and is_deleted = 0
            and is_latest = 1
            
            union all
            
            -- 递归查询
            select l.source_metadata_id, l.target_metadata_id, cte.level + 1
            from data_metadata_lineage l
            join lineage_cte cte on l.source_metadata_id = cte.target_id
            where l.is_deleted = 0
            and l.is_latest = 1
            and cte.level < 3
        )
        select source_id, target_id, level, 'DOWNSTREAM' as direction
        from lineage_cte
    )
    select * from upstream
    union all
    select * from downstream;
end;
/

-- 插入示例数据
insert into data_metadata_lineage (
    id, collection_id, execution_id, version, table_name,
    fk_name, fk_column_name, pk_table_name, pk_column_name,
    source_metadata_id, target_metadata_id, relation_type,
    description, weight, create_time, update_time,
    is_deleted, is_latest, creator_id, creator_name
) values (
    'ML20230101000001', 'COL20230101000001', 'EXE20230101000001', 1, 'CUSTOMER',
    'FK_ORDER_CUSTOMER', 'CUSTOMER_ID', 'CUSTOMER', 'ID',
    'MD20230101000001', 'MD20230101000002', 'FOREIGN_KEY',
    '客户与订单的关联关系', 1, sysdate, sysdate,
    0, 1, 'USER001', '管理员'
);

insert into data_metadata_lineage (
    id, collection_id, execution_id, version, table_name,
    fk_name, fk_column_name, pk_table_name, pk_column_name,
    source_metadata_id, target_metadata_id, relation_type,
    description, weight, create_time, update_time,
    is_deleted, is_latest, creator_id, creator_name
) values (
    'ML20230101000002', 'COL20230101000001', 'EXE20230101000001', 1, 'ORDER',
    'FK_ORDER_ITEM_ORDER', 'ORDER_ID', 'ORDER', 'ID',
    'MD20230101000002', 'MD20230101000003', 'FOREIGN_KEY',
    '订单与订单项的关联关系', 1, sysdate, sysdate,
    0, 1, 'USER001', '管理员'
); 