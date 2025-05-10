-- 创建模型设计表
create table if not exists data_model_design (
  id varchar(32) not null,
  model_id varchar(32) not null,
  canvas clob,
  fields clob,
  indexes clob,
  relations clob,
  config clob,
  create_by varchar(64) default null,
  create_time timestamp default null,
  update_by varchar(64) default null,
  update_time timestamp default null,
  primary key (id)
);

-- 创建索引
create index idx_model_design_model_id on data_model_design (model_id);

-- 添加外键约束
alter table data_model_design add constraint fk_model_design_model_id 
  foreign key (model_id) references data_model (id) on delete cascade;

-- 添加表和列注释
comment on table data_model_design is '模型设计表';
comment on column data_model_design.id is '主键id';
comment on column data_model_design.model_id is '关联的模型id';
comment on column data_model_design.canvas is '画布数据（json格式）';
comment on column data_model_design.fields is '字段定义（json格式）';
comment on column data_model_design.indexes is '索引定义（json格式）';
comment on column data_model_design.relations is '关系定义（json格式）';
comment on column data_model_design.config is '其他配置信息（json格式）';
comment on column data_model_design.create_by is '创建人';
comment on column data_model_design.create_time is '创建时间';
comment on column data_model_design.update_by is '更新人';
comment on column data_model_design.update_time is '更新时间';

-- 创建模型标准关联表
create table if not exists data_model_standard (
  id varchar(32) not null,
  model_id varchar(32) not null,
  standard_id varchar(32) not null,
  standard_type varchar(50) default null,
  field_id varchar(32) default null,
  create_by varchar(64) default null,
  create_time timestamp default null,
  update_by varchar(64) default null,
  update_time timestamp default null,
  primary key (id)
);

-- 创建索引
create index idx_model_standard_model_id on data_model_standard (model_id);
create index idx_model_standard_standard_id on data_model_standard (standard_id);

-- 添加外键约束
alter table data_model_standard add constraint fk_model_standard_model_id 
  foreign key (model_id) references data_model (id) on delete cascade;

-- 添加表和列注释
comment on table data_model_standard is '模型标准关联表';
comment on column data_model_standard.id is '主键id';
comment on column data_model_standard.model_id is '关联的模型id';
comment on column data_model_standard.standard_id is '关联的标准id';
comment on column data_model_standard.standard_type is '标准类型（字段标准、命名标准等）';
comment on column data_model_standard.field_id is '关联的字段id（如果是字段级别的标准）';
comment on column data_model_standard.create_by is '创建人';
comment on column data_model_standard.create_time is '创建时间';
comment on column data_model_standard.update_by is '更新人';
comment on column data_model_standard.update_time is '更新时间'; 