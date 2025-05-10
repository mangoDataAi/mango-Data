-- 创建数据资产表
create table data_asset (
    -- 主键ID
    id varchar(64) primary key,
    
    -- 资产名称
    name varchar(100) not null,
    
    -- 资产类型
    type varchar(50) not null,
    
    -- 数据源
    source varchar(100),
    
    -- 数据源ID
    source_id varchar(64),
    
    -- 责任人
    owner varchar(50),
    
    -- 资产描述
    description varchar(500),
    
    -- 创建时间
    create_time timestamp,
    
    -- 更新时间
    update_time timestamp,
    
    -- 创建人
    create_by varchar(64),
    
    -- 更新人
    update_by varchar(64),
    
    -- 是否删除，0-未删除，1-已删除
    is_deleted int default 0,
    
    -- 状态，待审核/已发布/已归档
    status varchar(20) default '待审核',
    
    -- 标签，以逗号分隔
    tags varchar(500),
    
    -- 主题域ID
    domain_id varchar(64),
    
    -- 主题域名称
    asset_domain varchar(100),
    
    -- 数据质量评分(0-100)
    quality_score decimal(5,2),
    
    -- 热度指数
    hot_index int,
    
    -- 资产编码
    code varchar(50),
    
    -- 所有者部门
    owner_department varchar(100),
    
    -- 数据量
    data_volume bigint
);

-- 添加表注释
comment on table data_asset is '数据资产表';

-- 添加列注释
comment on column data_asset.id is '主键ID';
comment on column data_asset.name is '资产名称';
comment on column data_asset.type is '资产类型';
comment on column data_asset.source is '数据源';
comment on column data_asset.source_id is '数据源ID';
comment on column data_asset.owner is '责任人';
comment on column data_asset.description is '资产描述';
comment on column data_asset.create_time is '创建时间';
comment on column data_asset.update_time is '更新时间';
comment on column data_asset.create_by is '创建人';
comment on column data_asset.update_by is '更新人';
comment on column data_asset.is_deleted is '是否删除，0-未删除，1-已删除';
comment on column data_asset.status is '状态，待审核/已发布/已归档';
comment on column data_asset.tags is '标签，以逗号分隔';
comment on column data_asset.domain_id is '主题域ID';
comment on column data_asset.asset_domain is '主题域名称';
comment on column data_asset.quality_score is '数据质量评分(0-100)';
comment on column data_asset.hot_index is '热度指数';
comment on column data_asset.code is '资产编码';
comment on column data_asset.owner_department is '所有者部门';
comment on column data_asset.data_volume is '数据量'; 