-- 创建规则模板表
create table data_rule_template (
    id varchar2(32) not null primary key,
    name varchar2(100) not null,
    rule_type varchar2(20) not null,
    source_type varchar2(20),
    scenario varchar2(20),
    description varchar2(500),
    condition_template_type varchar2(50),
    sql_template_type varchar2(50),
    config clob,
    source_info clob,
    field_mappings clob,
    sql_field_mappings clob,
    use_count number(10) default 0,
    status varchar2(20) default 'enabled',
    is_system number(1) default 0,
    create_by varchar2(32),
    create_time timestamp,
    update_by varchar2(32),
    update_time timestamp
);

-- 添加索引
create index idx_data_rule_template_type on data_rule_template (rule_type);
create index idx_data_rule_template_status on data_rule_template (status);
create index idx_data_rule_template_system on data_rule_template (is_system);
create index idx_data_rule_template_ctime on data_rule_template (create_time);

-- 添加注释
comment on table data_rule_template is '规则模板表';
comment on column data_rule_template.id is '主键ID';
comment on column data_rule_template.name is '模板名称';
comment on column data_rule_template.rule_type is '规则类型:completeness-完整性,accuracy-准确性,consistency-一致性,timeliness-时效性,uniqueness-唯一性,validity-有效性';
comment on column data_rule_template.source_type is '数据源类型';
comment on column data_rule_template.scenario is '适用场景:ingestion-数据接入,processing-数据处理,storage-数据存储,distribution-数据分发,analysis-数据分析';
comment on column data_rule_template.description is '模板描述';
comment on column data_rule_template.condition_template_type is '条件模板类型';
comment on column data_rule_template.sql_template_type is 'SQL模板类型';
comment on column data_rule_template.config is '配置信息(JSON格式)';
comment on column data_rule_template.source_info is '数据源信息(JSON格式)';
comment on column data_rule_template.field_mappings is '字段映射关系(JSON格式)';
comment on column data_rule_template.sql_field_mappings is 'SQL字段映射关系(JSON格式)';
comment on column data_rule_template.use_count is '使用次数';
comment on column data_rule_template.status is '状态:enabled-启用,disabled-禁用,archived-归档';
comment on column data_rule_template.is_system is '是否系统内置:1-是,0-否';
comment on column data_rule_template.create_by is '创建人ID';
comment on column data_rule_template.create_time is '创建时间';
comment on column data_rule_template.update_by is '更新人ID';
comment on column data_rule_template.update_time is '更新时间'; 