-- 创建模型表
create table data_model (
  id varchar(32) not null,
  name varchar(100) not null,
  type varchar(20) not null,
  domain_id varchar(32),
  owner varchar(50),
  status varchar(20) default 'draft',
  description text,
  config text,
  create_by varchar(50),
  create_time timestamp default current_timestamp,
  update_by varchar(50),
  update_time timestamp default current_timestamp,
  primary key (id)
);

-- 添加注释
comment on table data_model is '数据模型表';
comment on column data_model.id is '模型ID';
comment on column data_model.name is '模型名称';
comment on column data_model.type is '模型类型（summary:汇总模型, detail:明细模型）';
comment on column data_model.domain_id is '所属主题域ID';
comment on column data_model.owner is '负责人';
comment on column data_model.status is '状态（draft:草稿, published:已发布, running:运行中, failed:运行失败）';
comment on column data_model.description is '模型描述';
comment on column data_model.config is '模型配置（JSON格式）';
comment on column data_model.create_by is '创建者';
comment on column data_model.create_time is '创建时间';
comment on column data_model.update_by is '更新者';
comment on column data_model.update_time is '更新时间';

-- 创建索引
create index idx_data_model_domain_id on data_model (domain_id);
create index idx_data_model_type on data_model (type);
create index idx_data_model_status on data_model (status);

-- 创建主题域表（如果不存在）
create table if not exists db_domain (
  id varchar(32) not null,
  name varchar(100) not null,
  description text,
  create_by varchar(50),
  create_time timestamp default current_timestamp,
  update_by varchar(50),
  update_time timestamp default current_timestamp,
  primary key (id)
);

-- 添加主题域表注释
comment on table db_domain is '主题域表';
comment on column db_domain.id is '主题域ID';
comment on column db_domain.name is '主题域名称';
comment on column db_domain.description is '主题域描述';
comment on column db_domain.create_by is '创建者';
comment on column db_domain.create_time is '创建时间';
comment on column db_domain.update_by is '更新者';
comment on column db_domain.update_time is '更新时间';

-- 添加外键约束
alter table data_model add constraint fk_model_domain foreign key (domain_id) references db_domain (id); 