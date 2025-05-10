-- 创建用户表
create table sys_user (
    -- 主键ID
    id varchar(32) not null primary key,
    -- 用户名
    username varchar(50) not null,
    -- 密码
    password varchar(100) not null,
    -- 姓名
    real_name varchar(50),
    -- 性别（1-男，2-女，0-未知）
    gender varchar(1) default '0',
    -- 手机号
    mobile varchar(20),
    -- 邮箱
    email varchar(100),
    -- 头像
    avatar varchar(255),
    -- 所属机构ID
    organization_id varchar(32),
    -- 状态（1-正常，0-禁用）
    status varchar(1) default '1',
    -- 排序
    sort int default 0,
    -- 备注
    remark varchar(500),
    -- 创建时间
    create_time timestamp(6) default sysdate,
    -- 更新时间
    update_time timestamp(6) default sysdate,
    -- 创建人
    creator varchar(32),
    -- 更新人
    updater varchar(32)
);

-- 添加表注释
comment on table sys_user is '用户表';

-- 添加字段注释
comment on column sys_user.id is '主键ID';
comment on column sys_user.username is '用户名';
comment on column sys_user.password is '密码';
comment on column sys_user.real_name is '姓名';
comment on column sys_user.gender is '性别（1-男，2-女，0-未知）';
comment on column sys_user.mobile is '手机号';
comment on column sys_user.email is '邮箱';
comment on column sys_user.avatar is '头像';
comment on column sys_user.organization_id is '所属机构ID';
comment on column sys_user.status is '状态（1-正常，0-禁用）';
comment on column sys_user.sort is '排序';
comment on column sys_user.remark is '备注';
comment on column sys_user.create_time is '创建时间';
comment on column sys_user.update_time is '更新时间';
comment on column sys_user.creator is '创建人';
comment on column sys_user.updater is '更新人';

-- 创建索引
create index idx_sys_user_username on sys_user(username);
create index idx_sys_user_mobile on sys_user(mobile);
create index idx_sys_user_email on sys_user(email);
create index idx_sys_user_organization on sys_user(organization_id);
create index idx_sys_user_status on sys_user(status);

-- 创建唯一约束
create unique index uk_sys_user_username on sys_user(username);

-- 创建用户角色关联表
create table sys_user_role (
    -- 主键ID
    id varchar(32) not null primary key,
    -- 用户ID
    user_id varchar(32) not null,
    -- 角色ID
    role_id varchar(32) not null,
    -- 创建时间
    create_time timestamp(6) default sysdate
);

-- 添加表注释
comment on table sys_user_role is '用户角色关联表';

-- 添加字段注释
comment on column sys_user_role.id is '主键ID';
comment on column sys_user_role.user_id is '用户ID';
comment on column sys_user_role.role_id is '角色ID';
comment on column sys_user_role.create_time is '创建时间';

-- 创建索引
create index idx_sys_user_role_user on sys_user_role(user_id);
create index idx_sys_user_role_role on sys_user_role(role_id);

-- 创建唯一约束
create unique index uk_sys_user_role on sys_user_role(user_id, role_id);

-- 插入管理员用户
insert into sys_user (id, username, password, real_name, gender, mobile, email, organization_id, status, sort, remark, creator, updater)
values ('1', 'admin', '$2a$10$8KK.26X5X6V5jDkR/Qwdg.QR0zr7sA8qPEKYh7SLdHLtDu3vFhvte', '系统管理员', '1', '13800138000', 'admin@example.com', '1', '1', 1, '系统管理员账号', 'admin', 'admin');

-- 插入普通用户
insert into sys_user (id, username, password, real_name, gender, mobile, email, organization_id, status, sort, remark, creator, updater)
values ('2', 'test', '$2a$10$8KK.26X5X6V5jDkR/Qwdg.QR0zr7sA8qPEKYh7SLdHLtDu3vFhvte', '测试用户', '1', '13900139000', 'test@example.com', '2', '1', 2, '测试账号', 'admin', 'admin');

-- 插入用户角色关联
insert into sys_user_role (id, user_id, role_id) values ('1', '1', '1');
insert into sys_user_role (id, user_id, role_id) values ('2', '2', '2');










create table data_asset_category_relation (
                                              id varchar(36) primary key,
                                              category_id varchar(36),
                                              asset_id varchar(36),
                                              create_time timestamp,
                                              create_by varchar(50),
                                              is_deleted int
);

comment on table data_asset_category_relation is '分类资产关联关系表';
comment on column data_asset_category_relation.id is '主键ID';
comment on column data_asset_category_relation.category_id is '分类ID';
comment on column data_asset_category_relation.asset_id is '资产ID';
comment on column data_asset_category_relation.create_time is '创建时间';
comment on column data_asset_category_relation.create_by is '创建人';
comment on column data_asset_category_relation.is_deleted is '是否删除，0-未删除，1-已删除';


create table data_asset (
                            id varchar(36) primary key,
                            name varchar(100),
                            type varchar(50),
                            source varchar(100),
                            source_id varchar(36),
                            owner varchar(50),
                            description varchar(500),
                            create_time timestamp,
                            update_time timestamp,
                            create_by varchar(50),
                            update_by varchar(50),
                            is_deleted int,
                            status varchar(20),
                            tags varchar(500),
                            tag_names varchar(500),
                            domain_id varchar(36),
                            asset_domain varchar(100),
                            quality_score number(5,2),
                            hot_index int,
                            code varchar(100),
                            owner_department varchar(100),
                            data_volume bigint
);

comment on table data_asset is '数据资产表';
comment on column data_asset.id is '资产ID';
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
comment on column data_asset.tags is '标签，多个用英文逗号分隔';
comment on column data_asset.tag_names is '标签名称，多个用英文逗号分隔';
comment on column data_asset.domain_id is '主题域ID';
comment on column data_asset.asset_domain is '主题域名称';
comment on column data_asset.quality_score is '数据质量评分(0-100)';
comment on column data_asset.hot_index is '热度指数';
comment on column data_asset.code is '资产编码';
comment on column data_asset.owner_department is '所有者部门';
comment on column data_asset.data_volume is '数据量';


create table data_asset_category (
                                     id varchar(36) primary key,
                                     name varchar(100),
                                     code varchar(50),
                                     parent_id varchar(36),
                                     owner varchar(50),
                                     description varchar(500),
                                     create_time timestamp,
                                     update_time timestamp,
                                     create_by varchar(50),
                                     update_by varchar(50),
                                     sort_order int,
                                     is_deleted int
);

comment on table data_asset_category is '数据分类表';
comment on column data_asset_category.id is '主键ID';
comment on column data_asset_category.name is '分类名称';
comment on column data_asset_category.code is '分类编码';
comment on column data_asset_category.parent_id is '父级ID';
comment on column data_asset_category.owner is '责任人';
comment on column data_asset_category.description is '分类描述';
comment on column data_asset_category.create_time is '创建时间';
comment on column data_asset_category.update_time is '更新时间';
comment on column data_asset_category.create_by is '创建人';
comment on column data_asset_category.update_by is '更新人';
comment on column data_asset_category.sort_order is '排序号';
comment on column data_asset_category.is_deleted is '是否删除，0-未删除，1-已删除';


create table data_item (
                           id varchar(36) primary key,
                           name varchar(100),
                           category_id varchar(36),
                           classification_path varchar(500),
                           description varchar(500),
                           status varchar(20),
                           owner varchar(50),
                           create_time timestamp,
                           update_time timestamp,
                           create_by varchar(50),
                           update_by varchar(50),
                           is_deleted int
);

comment on table data_item is '数据编目表';
comment on column data_item.id is '主键ID';
comment on column data_item.name is '编目名称';
comment on column data_item.category_id is '所属分类ID';
comment on column data_item.classification_path is '分类路径';
comment on column data_item.description is '编目描述';
comment on column data_item.status is '状态（published-已发布，unpublished-未发布，archived-已归档）';
comment on column data_item.owner is '责任人';
comment on column data_item.create_time is '创建时间';
comment on column data_item.update_time is '更新时间';
comment on column data_item.create_by is '创建人';
comment on column data_item.update_by is '更新人';
comment on column data_item.is_deleted is '是否删除，0-未删除，1-已删除';


create table data_item_asset (
                                 id varchar(36) primary key,
                                 item_id varchar(36),
                                 category_id varchar(36),
                                 asset_id varchar(36),
                                 create_time timestamp,
                                 create_by varchar(50),
                                 is_deleted int
);

comment on table data_item_asset is '数据项与资产关联表';
comment on column data_item_asset.id is '主键ID';
comment on column data_item_asset.item_id is '数据项ID';
comment on column data_item_asset.category_id is '分类ID (用于catalog-asset关联)';
comment on column data_item_asset.asset_id is '资产ID';
comment on column data_item_asset.create_time is '创建时间';
comment on column data_item_asset.create_by is '创建人';
comment on column data_item_asset.is_deleted is '是否删除，0-未删除，1-已删除';


create table data_standard_object (
                                      id varchar(36) primary key,
                                      package_id varchar(36),
                                      name varchar(100),
                                      code varchar(50),
                                      type varchar(50),
                                      description varchar(500),
                                      create_time timestamp,
                                      update_time timestamp,
                                      create_by varchar(50),
                                      update_by varchar(50)
);

comment on table data_standard_object is '数据标准对象表';
comment on column data_standard_object.id is '主键ID';
comment on column data_standard_object.package_id is '包ID';
comment on column data_standard_object.name is '名称';
comment on column data_standard_object.code is '编码';
comment on column data_standard_object.type is '类型';
comment on column data_standard_object.description is '描述';
comment on column data_standard_object.create_time is '创建时间';
comment on column data_standard_object.update_time is '更新时间';
comment on column data_standard_object.create_by is '创建人';
comment on column data_standard_object.update_by is '更新人';


create table data_standard_package (
                                       id varchar(36) primary key,
                                       name varchar(100),
                                       code varchar(50),
                                       version varchar(50),
                                       description varchar(500),
                                       status varchar(20),
                                       create_time timestamp,
                                       update_time timestamp,
                                       create_by varchar(50),
                                       update_by varchar(50)
);

comment on table data_standard_package is '数据标准包表';
comment on column data_standard_package.id is '主键ID';
comment on column data_standard_package.name is '名称';
comment on column data_standard_package.code is '编码';
comment on column data_standard_package.version is '版本';
comment on column data_standard_package.description is '描述';
comment on column data_standard_package.status is '状态';
comment on column data_standard_package.create_time is '创建时间';
comment on column data_standard_package.update_time is '更新时间';
comment on column data_standard_package.create_by is '创建人';
comment on column data_standard_package.update_by is '更新人';


create table data_metadata_collection (
                                          id varchar(36) primary key,
                                          name varchar(100),
                                          description varchar(500),
                                          type varchar(50),
                                          data_source_id varchar(36),
                                          data_source_name varchar(100),
                                          metadata_count int,
                                          creator_id varchar(36),
                                          creator_name varchar(50),
                                          create_time timestamp,
                                          update_time timestamp,
                                          lineage_enabled number(1),
                                          last_lineage_analysis_time timestamp,
                                          import_config clob,
                                          tags clob,
                                          is_deleted int,
                                          catalog_id varchar(36),
                                          matching_strategy clob,
                                          storage_strategy clob,
                                          schedule_config clob,
                                          notification_config clob,
                                          quality_rules clob,
                                          catalog_name varchar(100),
                                          last_execution_id varchar(36),
                                          last_execute_time timestamp,
                                          schedule_type varchar(20),
                                          schedule_time varchar(50),
                                          schedule_minute int,
                                          schedule_day_of_week varchar(20),
                                          schedule_day_of_month int,
                                          schedule_cron varchar(100),
                                          execution_status int,
                                          execution_message varchar(500),
                                          data_source_type varchar(50),
                                          start_time date,
                                          end_time date,
                                          matching_strategy_desc varchar(200),
                                          storage_strategy_desc varchar(200),
                                          owner varchar(50)
);

comment on table data_metadata_collection is '元数据集合表';
comment on column data_metadata_collection.id is '主键ID';
comment on column data_metadata_collection.name is '集合名称';
comment on column data_metadata_collection.description is '集合描述';
comment on column data_metadata_collection.type is '集合类型';
comment on column data_metadata_collection.data_source_id is '数据源ID';
comment on column data_metadata_collection.data_source_name is '数据源名称';
comment on column data_metadata_collection.metadata_count is '元数据数量';
comment on column data_metadata_collection.creator_id is '创建人ID';
comment on column data_metadata_collection.creator_name is '创建人名称';
comment on column data_metadata_collection.create_time is '创建时间';
comment on column data_metadata_collection.update_time is '更新时间';
comment on column data_metadata_collection.lineage_enabled is '是否启用血缘分析';
comment on column data_metadata_collection.last_lineage_analysis_time is '最后一次血缘分析时间';
comment on column data_metadata_collection.import_config is '导入配置（JSON格式）';
comment on column data_metadata_collection.tags is '标签（JSON数组）';
comment on column data_metadata_collection.is_deleted is '是否删除（0-未删除，1-已删除）';
comment on column data_metadata_collection.catalog_id is '目录ID';
comment on column data_metadata_collection.matching_strategy is '标准匹配策略';
comment on column data_metadata_collection.storage_strategy is '入库策略';
comment on column data_metadata_collection.schedule_config is '调度配置';
comment on column data_metadata_collection.notification_config is '通知配置';
comment on column data_metadata_collection.quality_rules is '质量规则配置';
comment on column data_metadata_collection.catalog_name is '目录名称';
comment on column data_metadata_collection.last_execution_id is '最后一次执行ID';
comment on column data_metadata_collection.last_execute_time is '最后一次执行时间';
comment on column data_metadata_collection.schedule_type is '调度类型：once 一次性, hourly 每小时, daily 每天, weekly 每周, monthly 每月, custom 自定义';
comment on column data_metadata_collection.schedule_time is '调度时间';
comment on column data_metadata_collection.schedule_minute is '调度分钟';
comment on column data_metadata_collection.schedule_day_of_week is '调度星期几';
comment on column data_metadata_collection.schedule_day_of_month is '调度月份几号';
comment on column data_metadata_collection.schedule_cron is '调度cron表达式';
comment on column data_metadata_collection.execution_status is '执行状态(0-未执行 1-执行中 2-执行成功 3-执行失败)';
comment on column data_metadata_collection.execution_message is '执行消息';
comment on column data_metadata_collection.data_source_type is '数据源类型';
comment on column data_metadata_collection.start_time is '开始时间';
comment on column data_metadata_collection.end_time is '结束时间';
comment on column data_metadata_collection.matching_strategy_desc is '匹配策略描述';
comment on column data_metadata_collection.storage_strategy_desc is '入库策略描述';
comment on column data_metadata_collection.owner is '责任人';


create table data_standard_rule (
                                    id varchar(36) primary key,
                                    name varchar(100),
                                    code varchar(50),
                                    type varchar(50),
                                    sub_type varchar(50),
                                    description varchar(500),
                                    config clob,
                                    package_id varchar(36),
                                    object_id varchar(36),
                                    category_name varchar(100),
                                    sub_category_name varchar(100),
                                    create_time date,
                                    update_time date,
                                    create_user varchar(50),
                                    update_user varchar(50),
                                    status int
);

comment on table data_standard_rule is '数据标准规则表';
comment on column data_standard_rule.id is '主键ID';
comment on column data_standard_rule.name is '规则名称';
comment on column data_standard_rule.code is '规则编码';
comment on column data_standard_rule.type is '规则类型：NAMING-命名规范，STRUCTURE-结构规范，DATA-数据规范，QUALITY-质量规范，DOMAIN-主题域规范等';
comment on column data_standard_rule.sub_type is '规则子类型';
comment on column data_standard_rule.description is '描述';
comment on column data_standard_rule.config is '规则配置（JSON格式）';
comment on column data_standard_rule.package_id is '所属包ID';
comment on column data_standard_rule.object_id is '所属对象ID';
comment on column data_standard_rule.category_name is '规则分类名称';
comment on column data_standard_rule.sub_category_name is '规则子分类名称';
comment on column data_standard_rule.create_time is '创建时间';
comment on column data_standard_rule.update_time is '更新时间';
comment on column data_standard_rule.create_user is '创建用户';
comment on column data_standard_rule.update_user is '更新用户';
comment on column data_standard_rule.status is '状态：1-启用，0-禁用';



create table data_metadata_catalog (
                                       id varchar(36) primary key,
                                       name varchar(100),
                                       type varchar(50),
                                       parent_id varchar(36),
                                       description varchar(500),
                                       path varchar(500),
                                       order_num int,
                                       create_time timestamp,
                                       update_time timestamp
);

comment on table data_metadata_catalog is '元数据目录表';
comment on column data_metadata_catalog.id is '目录节点ID（UUID格式）';
comment on column data_metadata_catalog.name is '节点名称';
comment on column data_metadata_catalog.type is '节点类型：datasource(数据源)、database(数据库)、table(表)、view(视图)';
comment on column data_metadata_catalog.parent_id is '父节点ID';
comment on column data_metadata_catalog.description is '节点描述';
comment on column data_metadata_catalog.path is '节点路径';
comment on column data_metadata_catalog.order_num is '排序号';
comment on column data_metadata_catalog.create_time is '创建时间';
comment on column data_metadata_catalog.update_time is '更新时间';


create table data_metadata_collection_log (
                                              log_id varchar(36) primary key,
                                              execution_id varchar(36),
                                              collection_id varchar(36),
                                              step varchar(100),
                                              level varchar(20),
                                              message varchar(500),
                                              details clob,
                                              stack_trace clob,
                                              execute_time timestamp,
                                              success number(1),
                                              process_count int
);

comment on table data_metadata_collection_log is '元数据采集日志表';
comment on column data_metadata_collection_log.log_id is '日志ID';
comment on column data_metadata_collection_log.execution_id is '执行ID';
comment on column data_metadata_collection_log.collection_id is '采集任务ID';
comment on column data_metadata_collection_log.step is '执行步骤';
comment on column data_metadata_collection_log.level is '日志级别';
comment on column data_metadata_collection_log.message is '日志消息';
comment on column data_metadata_collection_log.details is '详细信息（JSON格式）';
comment on column data_metadata_collection_log.stack_trace is '堆栈跟踪信息';
comment on column data_metadata_collection_log.execute_time is '执行时间';
comment on column data_metadata_collection_log.success is '是否成功';
comment on column data_metadata_collection_log.process_count is '处理数量';



create table data_metadata_collection_version (
                                                  id varchar(36) primary key,
                                                  collection_id varchar(36),
                                                  execution_id varchar(36),
                                                  data_type varchar(50),
                                                  version_number int,
                                                  version_description varchar(500),
                                                  change_summary varchar(500),
                                                  change_details clob,
                                                  is_latest number(1),
                                                  create_time timestamp,
                                                  create_by varchar(50),
                                                  update_time timestamp,
                                                  update_by varchar(50),
                                                  status int
);

comment on table data_metadata_collection_version is '元数据采集版本记录表';
comment on column data_metadata_collection_version.id is '主键ID';
comment on column data_metadata_collection_version.collection_id is '采集ID';
comment on column data_metadata_collection_version.execution_id is '执行ID';
comment on column data_metadata_collection_version.data_type is '数据类型：table_structure, field_info, quality_report, lineage_info';
comment on column data_metadata_collection_version.version_number is '版本号';
comment on column data_metadata_collection_version.version_description is '版本描述';
comment on column data_metadata_collection_version.change_summary is '变更摘要';
comment on column data_metadata_collection_version.change_details is '变更详情（JSON格式）';
comment on column data_metadata_collection_version.is_latest is '是否为最新版本';
comment on column data_metadata_collection_version.create_time is '创建时间';
comment on column data_metadata_collection_version.create_by is '创建人';
comment on column data_metadata_collection_version.update_time is '更新时间';
comment on column data_metadata_collection_version.update_by is '更新人';
comment on column data_metadata_collection_version.status is '状态：0-无效，1-有效';


create table data_metadata_field_info (
                                          id varchar(36) primary key,
                                          collection_id varchar(36),
                                          execution_id varchar(36),
                                          table_name varchar(100),
                                          field_name varchar(100),
                                          data_type varchar(50),
                                          length int,
                                          precision int,
                                          scale int,
                                          nullable int,
                                          field_comment varchar(500),
                                          is_primary_key int,
                                          is_foreign_key int,
                                          is_unique int,
                                          is_indexed int,
                                          is_auto_increment int,
                                          default_value varchar(200),
                                          ordinal_position int,
                                          referenced_table_name varchar(100),
                                          referenced_column_name varchar(100),
                                          version int,
                                          is_latest int,
                                          is_deleted int,
                                          create_time date,
                                          update_time date,
                                          extra varchar(200),
                                          character_length int,
                                          numeric_precision int,
                                          numeric_scale int,
                                          match_score int,
                                          matched_standard varchar(100),
                                          charset varchar(50),
                                          sort_rule varchar(50),
                                          index_type varchar(50),
                                          index_name varchar(100),
                                          distinct_count bigint,
                                          null_count bigint,
                                          distribution clob,
                                          value_range clob,
                                          accuracy clob,
                                          data_format varchar(100),
                                          sample_values clob,
                                          top_values clob,
                                          need_manual_intervention int,
                                          manual_intervention_reason varchar(500),
                                          manual_intervention_status int,
                                          manual_intervention_time date,
                                          manual_intervention_user varchar(50)
);

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
comment on column data_metadata_field_info.field_comment is '字段注释';
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
comment on column data_metadata_field_info.extra is '额外信息（如自增等）';
comment on column data_metadata_field_info.character_length is '字符长度';
comment on column data_metadata_field_info.numeric_precision is '数值精度';
comment on column data_metadata_field_info.numeric_scale is '数值范围';
comment on column data_metadata_field_info.match_score is '匹配分数';
comment on column data_metadata_field_info.matched_standard is '匹配的标准';
comment on column data_metadata_field_info.charset is '字符集';
comment on column data_metadata_field_info.sort_rule is '排序规则';
comment on column data_metadata_field_info.index_type is '索引类型';
comment on column data_metadata_field_info.index_name is '索引名称';
comment on column data_metadata_field_info.distinct_count is '不同值数量';
comment on column data_metadata_field_info.null_count is '空值数量';
comment on column data_metadata_field_info.distribution is '数据分布信息（JSON格式）';
comment on column data_metadata_field_info.value_range is '值范围信息（JSON格式）';
comment on column data_metadata_field_info.accuracy is '数据准确性信息（JSON格式）';
comment on column data_metadata_field_info.data_format is '数据格式';
comment on column data_metadata_field_info.sample_values is '样本值（JSON格式）';
comment on column data_metadata_field_info.top_values is '频繁值（JSON格式）';
comment on column data_metadata_field_info.need_manual_intervention is '是否需要人工干预（0-不需要，1-需要）';
comment on column data_metadata_field_info.manual_intervention_reason is '人工干预原因';
comment on column data_metadata_field_info.manual_intervention_status is '人工干预状态（0-待处理，1-已处理，2-已拒绝）';
comment on column data_metadata_field_info.manual_intervention_time is '人工干预处理时间';
comment on column data_metadata_field_info.manual_intervention_user is '人工干预处理人';


create table data_metadata_lineage (
                                       id varchar(36) primary key,
                                       collection_id varchar(36),
                                       execution_id varchar(36),
                                       source_type varchar(50),
                                       source_id varchar(36),
                                       source_name varchar(100),
                                       target_type varchar(50),
                                       target_id varchar(36),
                                       target_name varchar(100),
                                       lineage_type varchar(50),
                                       transformation_rule clob,
                                       confidence int,
                                       version int,
                                       is_latest int,
                                       is_deleted int,
                                       create_time date,
                                       update_time date,
                                       lineage_path clob,
                                       additional_info clob
);

comment on table data_metadata_lineage is '元数据血缘关系表';
comment on column data_metadata_lineage.id is '主键ID';
comment on column data_metadata_lineage.collection_id is '采集任务ID';
comment on column data_metadata_lineage.execution_id is '执行ID';
comment on column data_metadata_lineage.source_type is '源元数据类型（database/table/column）';
comment on column data_metadata_lineage.source_id is '源元数据ID';
comment on column data_metadata_lineage.source_name is '源元数据名称';
comment on column data_metadata_lineage.target_type is '目标元数据类型（database/table/column）';
comment on column data_metadata_lineage.target_id is '目标元数据ID';
comment on column data_metadata_lineage.target_name is '目标元数据名称';
comment on column data_metadata_lineage.lineage_type is '血缘关系类型（copy/transform/aggregate/derive等）';
comment on column data_metadata_lineage.transformation_rule is '转换规则或SQL语句';
comment on column data_metadata_lineage.confidence is '血缘关系置信度（0-100）';
comment on column data_metadata_lineage.version is '版本号';
comment on column data_metadata_lineage.is_latest is '是否最新版本（1是，0否）';
comment on column data_metadata_lineage.is_deleted is '是否删除（0-未删除，1-已删除）';
comment on column data_metadata_lineage.create_time is '创建时间';
comment on column data_metadata_lineage.update_time is '更新时间';
comment on column data_metadata_lineage.lineage_path is '血缘路径（JSON格式，记录完整血缘路径）';
comment on column data_metadata_lineage.additional_info is '额外信息（JSON格式）';


create table data_metadata_lineage_detail (
                                              id varchar(36) primary key,
                                              lineage_id varchar(36),
                                              execution_id varchar(36),
                                              expression clob,
                                              description varchar(500),
                                              etl_job_id varchar(36),
                                              etl_job_name varchar(100),
                                              step_order int,
                                              create_time date,
                                              update_time date,
                                              is_deleted int
);

comment on table data_metadata_lineage_detail is '元数据血缘详情表';
comment on column data_metadata_lineage_detail.id is '主键ID';
comment on column data_metadata_lineage_detail.lineage_id is '血缘关系ID';
comment on column data_metadata_lineage_detail.execution_id is '执行ID';
comment on column data_metadata_lineage_detail.expression is '转换表达式或SQL片段';
comment on column data_metadata_lineage_detail.description is '转换描述';
comment on column data_metadata_lineage_detail.etl_job_id is 'ETL作业ID';
comment on column data_metadata_lineage_detail.etl_job_name is 'ETL作业名称';
comment on column data_metadata_lineage_detail.step_order is '转换步骤序号';
comment on column data_metadata_lineage_detail.create_time is '创建时间';
comment on column data_metadata_lineage_detail.update_time is '更新时间';
comment on column data_metadata_lineage_detail.is_deleted is '是否删除（0-未删除，1-已删除）';


create table data_metadata_quality_report (
                                              id varchar(36) primary key,
                                              collection_id varchar(36),
                                              execution_id varchar(36),
                                              version int,
                                              table_name varchar(100),
                                              total_rows bigint,
                                              violation_count int,
                                              violation_details clob,
                                              create_time date,
                                              update_time date,
                                              is_deleted int,
                                              is_latest int,
                                              completeness number(5,2),
                                              accuracy number(5,2),
                                              consistency number(5,2),
                                              uniqueness number(5,2),
                                              validity number(5,2),
                                              missing_count int,
                                              format_error_count int,
                                              out_of_range_count int,
                                              duplicate_count int
);

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
comment on column data_metadata_quality_report.completeness is '完整性指标';
comment on column data_metadata_quality_report.accuracy is '准确性指标';
comment on column data_metadata_quality_report.consistency is '一致性指标';
comment on column data_metadata_quality_report.uniqueness is '唯一性指标';
comment on column data_metadata_quality_report.validity is '有效性指标';
comment on column data_metadata_quality_report.missing_count is '缺失数据数量';
comment on column data_metadata_quality_report.format_error_count is '格式错误数量';
comment on column data_metadata_quality_report.out_of_range_count is '超出范围数量';
comment on column data_metadata_quality_report.duplicate_count is '重复数据数量';


create table data_metadata_table_structure (
                                               id varchar(36) primary key,
                                               collection_id varchar(36),
                                               execution_id varchar(36),
                                               table_name varchar(100),
                                               table_type varchar(50),
                                               table_comment varchar(500),
                                               create_sql clob,
                                               create_time date,
                                               update_time date,
                                               version int,
                                               is_latest int,
                                               is_deleted int,
                                               engine varchar(50),
                                               charset varchar(50),
                                               collation_rule varchar(50),
                                               table_size bigint,
                                               row_count bigint,
                                               tablespace varchar(100),
                                               import_status varchar(20),
                                               db_name varchar(100),
                                               owner varchar(50),
                                               table_size_mb number(10,2),
                                               table_create_time date,
                                               table_update_time date,
                                               table_ddl clob,
                                               column_count int,
                                               index_count int,
                                               constraint_count int,
                                               trigger_count int,
                                               partition_count int,
                                               storage_info clob,
                                               column_type_summary clob,
                                               index_info clob,
                                               primary_key_info clob,
                                               foreign_key_info clob,
                                               dependency_info clob,
                                               extended_info clob
);

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

create table data_metadata_version_change_log (
                                                  id varchar(36) primary key,
                                                  version_id varchar(36),
                                                  change_type varchar(50),
                                                  object_type varchar(50),
                                                  object_id varchar(36),
                                                  object_name varchar(100),
                                                  object_path varchar(500),
                                                  before_value clob,
                                                  after_value clob,
                                                  changed_fields clob,
                                                  change_description varchar(500),
                                                  change_time timestamp,
                                                  changer_id varchar(36),
                                                  changer_name varchar(50),
                                                  create_time timestamp,
                                                  is_deleted int
);

comment on table data_metadata_version_change_log is '元数据版本变更日志表';
comment on column data_metadata_version_change_log.id is '主键ID，使用UUID';
comment on column data_metadata_version_change_log.version_id is '版本ID';
comment on column data_metadata_version_change_log.change_type is '变更类型';
comment on column data_metadata_version_change_log.object_type is '对象类型';
comment on column data_metadata_version_change_log.object_id is '对象ID';
comment on column data_metadata_version_change_log.object_name is '对象名称';
comment on column data_metadata_version_change_log.object_path is '对象路径';
comment on column data_metadata_version_change_log.before_value is '变更前值，使用JSON类型';
comment on column data_metadata_version_change_log.after_value is '变更后值，使用JSON类型';
comment on column data_metadata_version_change_log.changed_fields is '变更字段，使用JSON类型';
comment on column data_metadata_version_change_log.change_description is '变更描述';
comment on column data_metadata_version_change_log.change_time is '变更时间';
comment on column data_metadata_version_change_log.changer_id is '变更者ID';
comment on column data_metadata_version_change_log.changer_name is '变更者名称';
comment on column data_metadata_version_change_log.create_time is '创建时间';
comment on column data_metadata_version_change_log.is_deleted is '是否删除';


create table data_metadata_version_record (
                                              id varchar(36) primary key,
                                              version_number varchar(50),
                                              version_name varchar(100),
                                              version_description varchar(500),
                                              metadata_id varchar(36),
                                              metadata_type varchar(50),
                                              metadata_name varchar(100),
                                              metadata_path varchar(500),
                                              publisher_id varchar(36),
                                              publisher_name varchar(50),
                                              publish_time timestamp,
                                              change_summary varchar(500),
                                              change_details clob,
                                              metadata_snapshot clob,
                                              previous_version_id varchar(36),
                                              is_current number(1),
                                              approval_status varchar(20),
                                              approval_comment varchar(500),
                                              approval_time timestamp,
                                              tags varchar(500),
                                              create_time timestamp,
                                              update_time timestamp,
                                              is_deleted int
);

comment on table data_metadata_version_record is '元数据版本记录表';
comment on column data_metadata_version_record.id is '主键ID，使用UUID';
comment on column data_metadata_version_record.version_number is '版本号';
comment on column data_metadata_version_record.version_name is '版本名称';
comment on column data_metadata_version_record.version_description is '版本描述';
comment on column data_metadata_version_record.metadata_id is '关联的元数据ID（可以是目录ID、数据库ID、表ID等）';
comment on column data_metadata_version_record.metadata_type is '元数据类型（catalog, database, table, field）';
comment on column data_metadata_version_record.metadata_name is '元数据名称';
comment on column data_metadata_version_record.metadata_path is '元数据路径';
comment on column data_metadata_version_record.publisher_id is '发布人ID';
comment on column data_metadata_version_record.publisher_name is '发布人名称';
comment on column data_metadata_version_record.publish_time is '发布时间';
comment on column data_metadata_version_record.change_summary is '变更内容摘要';
comment on column data_metadata_version_record.change_details is '变更详情';
comment on column data_metadata_version_record.metadata_snapshot is '元数据快照（JSON格式）';
comment on column data_metadata_version_record.previous_version_id is '关联的上一个版本ID';
comment on column data_metadata_version_record.is_current is '是否为当前版本';
comment on column data_metadata_version_record.approval_status is '审核状态（pending-待审核, approved-已审核, rejected-已拒绝）';
comment on column data_metadata_version_record.approval_comment is '审核意见';
comment on column data_metadata_version_record.approval_time is '审核时间';
comment on column data_metadata_version_record.tags is '标签';
comment on column data_metadata_version_record.create_time is '创建时间';
comment on column data_metadata_version_record.update_time is '更新时间';
comment on column data_metadata_version_record.is_deleted is '是否删除（0-未删除，1-已删除）';


create table data_asset_tag (
                                id varchar(36) primary key,
                                name varchar(100),
                                code varchar(50),
                                type varchar(20),
                                tag_set_id varchar(36),
                                status number(1),
                                description varchar(500),
                                asset_count int,
                                value_constraint varchar(200),
                                default_value varchar(100),
                                asset_relevance int,
                                scope varchar(200),
                                data_source varchar(50),
                                update_cycle varchar(20),
                                department varchar(50),
                                security_level varchar(20),
                                quality_score int,
                                remarks varchar(500),
                                create_time timestamp,
                                create_by varchar(50),
                                update_time timestamp,
                                update_by varchar(50),
                                is_deleted int
);

comment on table data_asset_tag is '标签表';
comment on column data_asset_tag.id is '主键ID';
comment on column data_asset_tag.name is '标签名称';
comment on column data_asset_tag.code is '标签标识';
comment on column data_asset_tag.type is '标签类型：attribute-属性型, metric-指标型, category-分类型, relation-关系型, status-状态型';
comment on column data_asset_tag.tag_set_id is '所属标签集ID';
comment on column data_asset_tag.status is '标签状态：0-禁用，1-启用';
comment on column data_asset_tag.description is '业务含义';
comment on column data_asset_tag.asset_count is '引用次数（资产关联数量）';
comment on column data_asset_tag.value_constraint is '值域约束';
comment on column data_asset_tag.default_value is '默认值';
comment on column data_asset_tag.asset_relevance is '资产关联度，1-低, 2-中, 3-高';
comment on column data_asset_tag.scope is '应用范围，多个用逗号分隔：结构化数据,非结构化数据,半结构化数据,数据服务,API';
comment on column data_asset_tag.data_source is '数据来源: system-系统计算, manual-人工录入, import-外部导入, api-API接入';
comment on column data_asset_tag.update_cycle is '更新周期: realtime-实时, daily-每日, weekly-每周, monthly-每月, quarterly-每季, yearly-每年, manual-手动';
comment on column data_asset_tag.department is '责任部门: data_management-数据管理部, it-IT部门, business-业务部门, risk-风控部门';
comment on column data_asset_tag.security_level is '涉密级别: public-公开, internal-内部, confidential-保密, secret-机密';
comment on column data_asset_tag.quality_score is '质量评分 1-5';
comment on column data_asset_tag.remarks is '备注';
comment on column data_asset_tag.create_time is '创建时间';
comment on column data_asset_tag.create_by is '创建人';
comment on column data_asset_tag.update_time is '更新时间';
comment on column data_asset_tag.update_by is '更新人';
comment on column data_asset_tag.is_deleted is '是否删除，0-未删除，1-已删除';


create table data_asset_tag_category (
                                         id varchar(36) primary key,
                                         name varchar(100),
                                         parent_id varchar(36),
                                         description varchar(500),
                                         sort_no int,
                                         create_time timestamp,
                                         create_by varchar(50),
                                         update_time timestamp,
                                         update_by varchar(50),
                                         is_deleted int
);

comment on table data_asset_tag_category is '标签分类表';
comment on column data_asset_tag_category.id is '主键ID';
comment on column data_asset_tag_category.name is '分类名称';
comment on column data_asset_tag_category.parent_id is '父级分类ID';
comment on column data_asset_tag_category.description is '描述';
comment on column data_asset_tag_category.sort_no is '排序号';
comment on column data_asset_tag_category.create_time is '创建时间';
comment on column data_asset_tag_category.create_by is '创建人';
comment on column data_asset_tag_category.update_time is '更新时间';
comment on column data_asset_tag_category.update_by is '更新人';
comment on column data_asset_tag_category.is_deleted is '是否删除，0-未删除，1-已删除';


create table data_asset_tag_set (
                                    id varchar(36) primary key,
                                    name varchar(100),
                                    category_id varchar(36),
                                    description varchar(500),
                                    tag_count int,
                                    create_time timestamp,
                                    create_by varchar(50),
                                    update_time timestamp,
                                    update_by varchar(50),
                                    is_deleted int
);

comment on table data_asset_tag_set is '标签集表';
comment on column data_asset_tag_set.id is '主键ID';
comment on column data_asset_tag_set.name is '标签集名称';
comment on column data_asset_tag_set.category_id is '所属分类ID';
comment on column data_asset_tag_set.description is '描述';
comment on column data_asset_tag_set.tag_count is '标签数量';
comment on column data_asset_tag_set.create_time is '创建时间';
comment on column data_asset_tag_set.create_by is '创建人';
comment on column data_asset_tag_set.update_time is '更新时间';
comment on column data_asset_tag_set.update_by is '更新人';
comment on column data_asset_tag_set.is_deleted is '是否删除，0-未删除，1-已删除';


create table data_versioned_metadata_catalog (
                                                 id varchar(36) primary key,
                                                 version_id varchar(36),
                                                 original_id varchar(36),
                                                 name varchar(100),
                                                 type varchar(50),
                                                 parent_id varchar(36),
                                                 description varchar(500),
                                                 path varchar(500),
                                                 order_num int,
                                                 create_time date,
                                                 update_time date
);

comment on table data_versioned_metadata_catalog is '定版元数据目录表';
comment on column data_versioned_metadata_catalog.id is '主键ID';
comment on column data_versioned_metadata_catalog.version_id is '版本ID';
comment on column data_versioned_metadata_catalog.original_id is '原始ID';
comment on column data_versioned_metadata_catalog.name is '节点名称';
comment on column data_versioned_metadata_catalog.type is '节点类型：datasource(数据源)、database(数据库)、table(表)、view(视图)';
comment on column data_versioned_metadata_catalog.parent_id is '父节点ID';
comment on column data_versioned_metadata_catalog.description is '节点描述';
comment on column data_versioned_metadata_catalog.path is '节点路径';
comment on column data_versioned_metadata_catalog.order_num is '排序号';
comment on column data_versioned_metadata_catalog.create_time is '创建时间';
comment on column data_versioned_metadata_catalog.update_time is '更新时间';


create table data_versioned_metadata_collection (
                                                    id varchar(36) primary key,
                                                    version_id varchar(36),
                                                    original_id varchar(36),
                                                    name varchar(100),
                                                    description varchar(500),
                                                    type varchar(50),
                                                    data_source_id varchar(36),
                                                    data_source_name varchar(100),
                                                    metadata_count int,
                                                    creator_id varchar(36),
                                                    creator_name varchar(50),
                                                    create_time timestamp,
                                                    update_time timestamp,
                                                    lineage_enabled number(1),
                                                    last_lineage_analysis_time timestamp,
                                                    import_config clob,
                                                    tags clob,
                                                    catalog_id varchar(36),
                                                    matching_strategy varchar(200),
                                                    storage_strategy varchar(200),
                                                    schedule_config clob,
                                                    notification_config clob,
                                                    quality_rules clob,
                                                    catalog_name varchar(100),
                                                    data_source_type varchar(50),
                                                    matching_strategy_desc varchar(200),
                                                    storage_strategy_desc varchar(200),
                                                    owner varchar(50),
                                                    is_deleted int
);

comment on table data_versioned_metadata_collection is '定版元数据集合表';
comment on column data_versioned_metadata_collection.id is '主键ID';
comment on column data_versioned_metadata_collection.version_id is '版本ID';
comment on column data_versioned_metadata_collection.original_id is '原始ID';
comment on column data_versioned_metadata_collection.name is '名称';
comment on column data_versioned_metadata_collection.description is '描述';
comment on column data_versioned_metadata_collection.type is '类型';
comment on column data_versioned_metadata_collection.data_source_id is '数据源ID';
comment on column data_versioned_metadata_collection.data_source_name is '数据源名称';
comment on column data_versioned_metadata_collection.metadata_count is '元数据数量';
comment on column data_versioned_metadata_collection.creator_id is '创建人ID';
comment on column data_versioned_metadata_collection.creator_name is '创建人名称';
comment on column data_versioned_metadata_collection.create_time is '创建时间';
comment on column data_versioned_metadata_collection.update_time is '更新时间';
comment on column data_versioned_metadata_collection.lineage_enabled is '是否启用血缘分析';
comment on column data_versioned_metadata_collection.last_lineage_analysis_time is '最后一次血缘分析时间';
comment on column data_versioned_metadata_collection.import_config is '导入配置（JSON格式）';
comment on column data_versioned_metadata_collection.tags is '标签（JSON格式）';
comment on column data_versioned_metadata_collection.catalog_id is '目录ID';
comment on column data_versioned_metadata_collection.matching_strategy is '匹配策略';
comment on column data_versioned_metadata_collection.storage_strategy is '存储策略';
comment on column data_versioned_metadata_collection.schedule_config is '调度配置';
comment on column data_versioned_metadata_collection.notification_config is '通知配置';
comment on column data_versioned_metadata_collection.quality_rules is '质量规则配置';
comment on column data_versioned_metadata_collection.catalog_name is '目录名称';
comment on column data_versioned_metadata_collection.data_source_type is '数据源类型';
comment on column data_versioned_metadata_collection.matching_strategy_desc is '匹配策略描述';
comment on column data_versioned_metadata_collection.storage_strategy_desc is '存储策略描述';
comment on column data_versioned_metadata_collection.owner is '责任人';
comment on column data_versioned_metadata_collection.is_deleted is '是否删除';

create table data_versioned_metadata_field_info (
                                                    id varchar(36) primary key,
                                                    version_id varchar(36),
                                                    original_id varchar(36),
                                                    collection_id varchar(36),
                                                    execution_id varchar(36),
                                                    table_name varchar(100),
                                                    field_name varchar(100),
                                                    data_type varchar(50),
                                                    length int,
                                                    precision int,
                                                    scale int,
                                                    nullable int,
                                                    field_comment varchar(500),
                                                    is_primary_key int,
                                                    is_foreign_key int,
                                                    is_unique int,
                                                    is_indexed int,
                                                    is_auto_increment int,
                                                    default_value varchar(200),
                                                    ordinal_position int,
                                                    referenced_table_name varchar(100),
                                                    referenced_column_name varchar(100),
                                                    version int,
                                                    is_latest int,
                                                    create_time timestamp,
                                                    update_time timestamp,
                                                    extra varchar(200),
                                                    character_length int,
                                                    numeric_precision int,
                                                    numeric_scale int,
                                                    match_score int,
                                                    matched_standard varchar(100),
                                                    charset varchar(50),
                                                    sort_rule varchar(50),
                                                    index_type varchar(50),
                                                    index_name varchar(100),
                                                    distinct_count bigint,
                                                    null_count bigint,
                                                    distribution clob,
                                                    value_range clob,
                                                    accuracy float,
                                                    data_format varchar(100),
                                                    sample_values clob,
                                                    top_values clob,
                                                    is_deleted int
);

comment on table data_versioned_metadata_field_info is '定版元数据字段信息表';
comment on column data_versioned_metadata_field_info.id is '主键ID';
comment on column data_versioned_metadata_field_info.version_id is '版本ID';
comment on column data_versioned_metadata_field_info.original_id is '原始ID';
comment on column data_versioned_metadata_field_info.collection_id is '采集ID';
comment on column data_versioned_metadata_field_info.execution_id is '执行ID';
comment on column data_versioned_metadata_field_info.table_name is '表名';
comment on column data_versioned_metadata_field_info.field_name is '字段名';
comment on column data_versioned_metadata_field_info.data_type is '数据类型';
comment on column data_versioned_metadata_field_info.length is '长度';
comment on column data_versioned_metadata_field_info.precision is '精度';
comment on column data_versioned_metadata_field_info.scale is '刻度';
comment on column data_versioned_metadata_field_info.nullable is '是否为空';
comment on column data_versioned_metadata_field_info.field_comment is '字段注释';
comment on column data_versioned_metadata_field_info.is_primary_key is '是否主键';
comment on column data_versioned_metadata_field_info.is_foreign_key is '是否外键';
comment on column data_versioned_metadata_field_info.is_unique is '是否唯一';
comment on column data_versioned_metadata_field_info.is_indexed is '是否索引';
comment on column data_versioned_metadata_field_info.is_auto_increment is '是否自增';
comment on column data_versioned_metadata_field_info.default_value is '默认值';
comment on column data_versioned_metadata_field_info.ordinal_position is '顺序位置';
comment on column data_versioned_metadata_field_info.referenced_table_name is '引用表名';
comment on column data_versioned_metadata_field_info.referenced_column_name is '引用列名';
comment on column data_versioned_metadata_field_info.version is '版本';
comment on column data_versioned_metadata_field_info.is_latest is '是否最新';
comment on column data_versioned_metadata_field_info.create_time is '创建时间';
comment on column data_versioned_metadata_field_info.update_time is '更新时间';
comment on column data_versioned_metadata_field_info.extra is '额外信息';
comment on column data_versioned_metadata_field_info.character_length is '字符长度';
comment on column data_versioned_metadata_field_info.numeric_precision is '数值精度';
comment on column data_versioned_metadata_field_info.numeric_scale is '数值刻度';
comment on column data_versioned_metadata_field_info.match_score is '匹配分数';
comment on column data_versioned_metadata_field_info.matched_standard is '匹配标准';
comment on column data_versioned_metadata_field_info.charset is '字符集';
comment on column data_versioned_metadata_field_info.sort_rule is '排序规则';
comment on column data_versioned_metadata_field_info.index_type is '索引类型';
comment on column data_versioned_metadata_field_info.index_name is '索引名称';
comment on column data_versioned_metadata_field_info.distinct_count is '不同值数量';
comment on column data_versioned_metadata_field_info.null_count is '空值数量';
comment on column data_versioned_metadata_field_info.distribution is '分布情况';
comment on column data_versioned_metadata_field_info.value_range is '值范围';
comment on column data_versioned_metadata_field_info.accuracy is '准确率';
comment on column data_versioned_metadata_field_info.data_format is '数据格式';
comment on column data_versioned_metadata_field_info.sample_values is '样本值';
comment on column data_versioned_metadata_field_info.top_values is '热门值';
comment on column data_versioned_metadata_field_info.is_deleted is '是否删除';


create table data_versioned_metadata_table_structure (
                                                         id varchar(36) primary key,
                                                         version_id varchar(36),
                                                         original_id varchar(36),
                                                         collection_id varchar(36),
                                                         execution_id varchar(36),
                                                         table_name varchar(100),
                                                         table_type varchar(50),
                                                         table_comment varchar(500),
                                                         create_sql clob,
                                                         row_count bigint,
                                                         table_size bigint,
                                                         table_size_mb float,
                                                         create_time timestamp,
                                                         update_time timestamp,
                                                         engine varchar(50),
                                                         version varchar(50),
                                                         charset varchar(50),
                                                         collation_rule varchar(50),
                                                         tablespace varchar(100),
                                                         db_name varchar(100),
                                                         owner varchar(50),
                                                         table_create_time timestamp,
                                                         table_update_time timestamp,
                                                         table_ddl clob,
                                                         column_count int,
                                                         index_count int,
                                                         constraint_count int,
                                                         trigger_count int,
                                                         partition_count int,
                                                         storage_info clob,
                                                         column_type_summary clob,
                                                         index_info clob,
                                                         primary_key_info clob,
                                                         foreign_key_info clob,
                                                         dependency_info clob,
                                                         extended_info clob,
                                                         is_deleted int
);

comment on table data_versioned_metadata_table_structure is '定版元数据表结构信息表';
comment on column data_versioned_metadata_table_structure.id is '主键ID';
comment on column data_versioned_metadata_table_structure.version_id is '版本ID';
comment on column data_versioned_metadata_table_structure.original_id is '原始ID';
comment on column data_versioned_metadata_table_structure.collection_id is '采集任务ID';
comment on column data_versioned_metadata_table_structure.execution_id is '执行ID';
comment on column data_versioned_metadata_table_structure.table_name is '表名';
comment on column data_versioned_metadata_table_structure.table_type is '表类型';
comment on column data_versioned_metadata_table_structure.table_comment is '表注释';
comment on column data_versioned_metadata_table_structure.create_sql is '建表SQL';
comment on column data_versioned_metadata_table_structure.row_count is '行数';
comment on column data_versioned_metadata_table_structure.table_size is '表大小(字节)';
comment on column data_versioned_metadata_table_structure.table_size_mb is '表大小(MB)';
comment on column data_versioned_metadata_table_structure.create_time is '创建时间';
comment on column data_versioned_metadata_table_structure.update_time is '更新时间';
comment on column data_versioned_metadata_table_structure.engine is '存储引擎';
comment on column data_versioned_metadata_table_structure.version is '版本号';
comment on column data_versioned_metadata_table_structure.charset is '字符集';
comment on column data_versioned_metadata_table_structure.collation_rule is '排序规则';
comment on column data_versioned_metadata_table_structure.tablespace is '表空间';
comment on column data_versioned_metadata_table_structure.db_name is '数据库名称';
comment on column data_versioned_metadata_table_structure.owner is '表所有者';
comment on column data_versioned_metadata_table_structure.table_create_time is '表创建时间';
comment on column data_versioned_metadata_table_structure.table_update_time is '表更新时间';
comment on column data_versioned_metadata_table_structure.table_ddl is '表DDL';
comment on column data_versioned_metadata_table_structure.column_count is '列数量';
comment on column data_versioned_metadata_table_structure.index_count is '索引数量';
comment on column data_versioned_metadata_table_structure.constraint_count is '约束数量';
comment on column data_versioned_metadata_table_structure.trigger_count is '触发器数量';
comment on column data_versioned_metadata_table_structure.partition_count is '分区数量';
comment on column data_versioned_metadata_table_structure.storage_info is '存储信息(JSON)';
comment on column data_versioned_metadata_table_structure.column_type_summary is '列类型摘要(JSON)';
comment on column data_versioned_metadata_table_structure.index_info is '索引信息(JSON)';
comment on column data_versioned_metadata_table_structure.primary_key_info is '主键信息(JSON)';
comment on column data_versioned_metadata_table_structure.foreign_key_info is '外键信息(JSON)';
comment on column data_versioned_metadata_table_structure.dependency_info is '依赖信息(JSON)';
comment on column data_versioned_metadata_table_structure.extended_info is '扩展信息(JSON)';
comment on column data_versioned_metadata_table_structure.is_deleted is '是否删除';








create table data_model_design (
                                   id varchar(36) primary key,
                                   model_id varchar(36),
                                   canvas clob,
                                   fields clob,
                                   indexes clob,
                                   relations clob,
                                   config clob,
                                   create_by varchar(50),
                                   create_time date,
                                   update_by varchar(50),
                                   update_time date
);

comment on table data_model_design is '模型设计表';
comment on column data_model_design.id is '主键ID';
comment on column data_model_design.model_id is '关联的模型ID';
comment on column data_model_design.canvas is '画布数据（JSON格式）';
comment on column data_model_design.fields is '字段定义（JSON格式）';
comment on column data_model_design.indexes is '索引定义（JSON格式）';
comment on column data_model_design.relations is '关系定义（JSON格式）';
comment on column data_model_design.config is '其他配置信息（JSON格式）';
comment on column data_model_design.create_by is '创建人';
comment on column data_model_design.create_time is '创建时间';
comment on column data_model_design.update_by is '更新人';
comment on column data_model_design.update_time is '更新时间';


create table data_model (
                            id varchar(36) primary key,
                            name varchar(100),
                            type varchar(20),
                            owner varchar(50),
                            status varchar(20),
                            description varchar(500),
                            config clob,
                            create_by varchar(50),
                            create_time date,
                            update_by varchar(50),
                            update_time date,
                            tags varchar(500),
                            version int,
                            applied_standard_ids varchar(500),
                            standard_apply_scope varchar(20),
                            standard_apply_description varchar(500),
                            standard_apply_time date,
                            standard_apply_user varchar(50)
);

comment on table data_model is '模型表';
comment on column data_model.id is '模型ID';
comment on column data_model.name is '模型名称';
comment on column data_model.type is '模型类型（summary:汇总模型, detail:明细模型）';
comment on column data_model.owner is '负责人';
comment on column data_model.status is '状态（draft:草稿, published:已发布, running:运行中, failed:运行失败）';
comment on column data_model.description is '模型描述';
comment on column data_model.config is '模型配置（JSON格式）';
comment on column data_model.create_by is '创建者';
comment on column data_model.create_time is '创建时间';
comment on column data_model.update_by is '更新者';
comment on column data_model.update_time is '更新时间';
comment on column data_model.tags is '标签，多个标签用逗号分隔';
comment on column data_model.version is '版本号';
comment on column data_model.applied_standard_ids is '已应用的标准IDs，多个标准ID用逗号分隔';
comment on column data_model.standard_apply_scope is '应用标准的范围（all:所有字段, selected:选中字段）';
comment on column data_model.standard_apply_description is '应用标准的说明';
comment on column data_model.standard_apply_time is '应用标准的时间';
comment on column data_model.standard_apply_user is '应用标准的用户';


create table data_model_standard (
                                     id varchar(36) primary key,
                                     model_id varchar(36),
                                     standard_id varchar(36),
                                     standard_name varchar(100),
                                     standard_type varchar(20),
                                     rule_type varchar(50),
                                     apply_scope varchar(20),
                                     description varchar(500),
                                     apply_time date,
                                     apply_user varchar(50),
                                     status int,
                                     field_id varchar(36)
);

comment on table data_model_standard is '模型标准表';
comment on column data_model_standard.id is '关联ID';
comment on column data_model_standard.model_id is '模型ID';
comment on column data_model_standard.standard_id is '标准ID';
comment on column data_model_standard.standard_name is '标准名称';
comment on column data_model_standard.standard_type is '标准类型（required:必须执行, suggested:建议执行）';
comment on column data_model_standard.rule_type is '标准规则类型';
comment on column data_model_standard.apply_scope is '应用范围（all:所有字段, selected:选中字段）';
comment on column data_model_standard.description is '应用说明';
comment on column data_model_standard.apply_time is '应用时间';
comment on column data_model_standard.apply_user is '应用用户';
comment on column data_model_standard.status is '状态（1:有效, 0:无效）';
comment on column data_model_standard.field_id is '字段ID（当应用范围为selected时使用）';


create table t_table (
                         id varchar(36) primary key,
                         name varchar(100),
                         display_name varchar(100),
                         field_count int,
                         group_id varchar(36),
                         data_source_id varchar(36),
                         create_time date,
                         create_by varchar(50),
                         update_time date,
                         update_by varchar(50),
                         delete_flg int,
                         remark varchar(500)
);

comment on table t_table is '表信息表';
comment on column t_table.id is '主键ID';
comment on column t_table.name is '表名';
comment on column t_table.display_name is '显示名称';
comment on column t_table.field_count is '字段数量';
comment on column t_table.group_id is '组ID';
comment on column t_table.data_source_id is '数据源ID';
comment on column t_table.create_time is '创建时间';
comment on column t_table.create_by is '创建人';
comment on column t_table.update_time is '更新时间';
comment on column t_table.update_by is '更新人';
comment on column t_table.delete_flg is '删除标志';
comment on column t_table.remark is '备注';


create table t_table_field (
                               id varchar(36) primary key,
                               table_id varchar(36),
                               name varchar(100),
                               display_name varchar(100),
                               type varchar(50),
                               length int,
                               precision int,
                               scale int,
                               is_primary int,
                               not_null int,
                               mgauto_increment int,
                               default_value varchar(200),
                               mgcomment varchar(500),
                               field_order int,
                               create_time date,
                               create_by varchar(50),
                               update_time date,
                               update_by varchar(50),
                               delete_flg int,
                               remark varchar(500)
);

comment on table t_table_field is '表字段表';
comment on column t_table_field.id is '主键ID';
comment on column t_table_field.table_id is '所属表ID';
comment on column t_table_field.name is '字段名';
comment on column t_table_field.display_name is '字段显示名';
comment on column t_table_field.type is '字段类型';
comment on column t_table_field.length is '字段长度';
comment on column t_table_field.precision is '精度';
comment on column t_table_field.scale is '小数位数';
comment on column t_table_field.is_primary is '是否为主键';
comment on column t_table_field.not_null is '是否可为空';
comment on column t_table_field.mgauto_increment is '是否自动递增';
comment on column t_table_field.default_value is '默认值';
comment on column t_table_field.mgcomment is '字段注释';
comment on column t_table_field.field_order is '字段排序';
comment on column t_table_field.create_time is '创建时间';
comment on column t_table_field.create_by is '创建人';
comment on column t_table_field.update_time is '更新时间';
comment on column t_table_field.update_by is '更新人';
comment on column t_table_field.delete_flg is '删除标志';
comment on column t_table_field.remark is '备注';


create table data_task_log (
                               id varchar(36) primary key,
                               task_id varchar(36),
                               version_id varchar(36),
                               log_level varchar(20),
                               content clob,
                               operation_type varchar(50),
                               operator varchar(50),
                               table_id varchar(36),
                               table_name varchar(100),
                               details clob,
                               create_time timestamp
);

comment on table data_task_log is '任务日志表';
comment on column data_task_log.id is '主键ID';
comment on column data_task_log.task_id is '任务ID';
comment on column data_task_log.version_id is '版本ID';
comment on column data_task_log.log_level is '日志级别: INFO, WARN, ERROR, DEBUG';
comment on column data_task_log.content is '日志内容';
comment on column data_task_log.operation_type is '操作类型: CREATE, UPDATE, EXECUTE, CANCEL, 等';
comment on column data_task_log.operator is '操作人';
comment on column data_task_log.table_id is '相关表ID';
comment on column data_task_log.table_name is '相关表名称';
comment on column data_task_log.details is '详细信息';
comment on column data_task_log.create_time is '创建时间';


create table data_task_table (
                                 id varchar(36) primary key,
                                 task_id varchar(36),
                                 version_id varchar(36),
                                 table_id varchar(36),
                                 table_name varchar(100),
                                 table_comment varchar(500),
                                 model_id varchar(36),
                                 model_name varchar(100),
                                 is_main number(1),
                                 materialize_type varchar(20),
                                 incremental_field varchar(100),
                                 write_mode varchar(20),
                                 dependencies clob,
                                 create_time timestamp,
                                 update_time timestamp,
                                 status varchar(20)
);

comment on table data_task_table is '任务表配置表';
comment on column data_task_table.id is '主键ID';
comment on column data_task_table.task_id is '关联的任务ID';
comment on column data_task_table.version_id is '关联的版本ID';
comment on column data_task_table.table_id is '表ID';
comment on column data_task_table.table_name is '表名称';
comment on column data_task_table.table_comment is '表注释';
comment on column data_task_table.model_id is '所属模型ID';
comment on column data_task_table.model_name is '所属模型名称';
comment on column data_task_table.is_main is '是否是主表';
comment on column data_task_table.materialize_type is '物化类型：full, incremental';
comment on column data_task_table.incremental_field is '增量字段';
comment on column data_task_table.write_mode is '写入模式：append, overwrite';
comment on column data_task_table.dependencies is '依赖表IDs';
comment on column data_task_table.create_time is '创建时间';
comment on column data_task_table.update_time is '更新时间';
comment on column data_task_table.status is '状态：waiting, running, completed, failed';


create table data_task_table_field (
                                       id varchar(36) primary key,
                                       task_id varchar(36),
                                       version_id varchar(36),
                                       table_id varchar(36),
                                       field_name varchar(100),
                                       field_type varchar(50),
                                       length int,
                                       precision int,
                                       scale int,
                                       is_primary int,
                                       not_null int,
                                       is_auto_incr int,
                                       default_value varchar(200),
                                       field_comment varchar(500),
                                       field_order int,
                                       create_time date,
                                       create_by varchar(50),
                                       update_time date,
                                       update_by varchar(50),
                                       delete_flg int,
                                       remark varchar(500)
);

comment on table data_task_table_field is '任务表字段表';
comment on column data_task_table_field.id is '主键ID';
comment on column data_task_table_field.task_id is '任务ID';
comment on column data_task_table_field.version_id is '版本ID';
comment on column data_task_table_field.table_id is '任务表ID';
comment on column data_task_table_field.field_name is '字段名';
comment on column data_task_table_field.field_type is '字段类型';
comment on column data_task_table_field.length is '字段长度';
comment on column data_task_table_field.precision is '精度';
comment on column data_task_table_field.scale is '小数位';
comment on column data_task_table_field.is_primary is '是否主键 (1:是 0:否)';
comment on column data_task_table_field.not_null is '是否允许为空 (1:是 0:否)';
comment on column data_task_table_field.is_auto_incr is '是否自增 (1:是 0:否)';
comment on column data_task_table_field.default_value is '默认值';
comment on column data_task_table_field.field_comment is '字段注释';
comment on column data_task_table_field.field_order is '字段顺序';
comment on column data_task_table_field.create_time is '创建时间';
comment on column data_task_table_field.create_by is '创建人';
comment on column data_task_table_field.update_time is '更新时间';
comment on column data_task_table_field.update_by is '更新人';
comment on column data_task_table_field.delete_flg is '删除标志';
comment on column data_task_table_field.remark is '备注';


create table data_task_version (
                                   id varchar(36) primary key,
                                   task_id varchar(36),
                                   model_id varchar(36),
                                   data_source_id varchar(36),
                                   database_id varchar(36),
                                   version_num varchar(20),
                                   environment varchar(20),
                                   status varchar(20),
                                   publisher varchar(50),
                                   publish_time timestamp,
                                   description varchar(500),
                                   mode varchar(20),
                                   is_current number(1),
                                   is_rollback number(1),
                                   rollback_from_version_id varchar(36),
                                   changes clob,
                                   config clob,
                                   logs clob,
                                   create_time timestamp,
                                   update_time timestamp
);

comment on table data_task_version is '任务版本表';
comment on column data_task_version.id is '主键ID';
comment on column data_task_version.task_id is '关联的任务ID';
comment on column data_task_version.model_id is '关联的模型ID';
comment on column data_task_version.data_source_id is '关联的数据源ID';
comment on column data_task_version.database_id is '关联的数据库ID';
comment on column data_task_version.version_num is '版本号';
comment on column data_task_version.environment is '发布环境';
comment on column data_task_version.status is '发布状态：success, failed, running';
comment on column data_task_version.publisher is '发布者';
comment on column data_task_version.publish_time is '发布时间';
comment on column data_task_version.description is '描述';
comment on column data_task_version.mode is '发布方式：full, incremental';
comment on column data_task_version.is_current is '是否为当前版本';
comment on column data_task_version.is_rollback is '是否为回滚版本';
comment on column data_task_version.rollback_from_version_id is '回滚源版本ID';
comment on column data_task_version.changes is '变更内容';
comment on column data_task_version.config is '配置信息';
comment on column data_task_version.logs is '日志信息';
comment on column data_task_version.create_time is '创建时间';
comment on column data_task_version.update_time is '更新时间';


-- 1. 为表字段添加外键约束
alter table t_table_field add constraint fk_table_field_table
    foreign key (table_id) references t_table(id);

-- 2. 为维度属性添加外键约束
alter table t_dimension_attribute add constraint fk_dimension_attr_dimension
    foreign key (dimension_id) references t_dimension(id);

-- 3. 为维度标准添加外键约束
alter table t_dimension_standard add constraint fk_dimension_std_dimension
    foreign key (dimension_id) references t_dimension(id);

-- 4. 为模型设计添加外键约束
alter table data_model_design add constraint fk_model_design_model
    foreign key (model_id) references data_model(id);

-- 5. 为模型标准添加外键约束
alter table data_model_standard add constraint fk_model_std_model
    foreign key (model_id) references data_model(id);

-- 6. 为任务表配置添加外键约束
alter table data_task_table add constraint fk_task_table_task
    foreign key (task_id) references data_materialize_task(id);

-- 7. 为任务表字段添加外键约束
alter table data_task_table_field add constraint fk_task_field_table
    foreign key (table_id) references data_task_table(id);

-- 8. 为任务版本添加外键约束
alter table data_task_version add constraint fk_task_version_task
    foreign key (task_id) references data_materialize_task(id);

-- 9. 为文件字段添加外键约束
alter table file_field add constraint fk_file_field_source
    foreign key (source_id) references file_data_source(id);


-- 1. 为数据源表创建索引
create index idx_datasource_name on sys_datasource(name);
create index idx_datasource_type on sys_datasource(type);

-- 2. 为维度表创建索引
create index idx_dimension_domain on t_dimension(domain_id);
create index idx_dimension_name on t_dimension(name);

-- 3. 为主题域表创建索引
create index idx_domain_parent on t_domain(parent_id);
create index idx_domain_name on t_domain(name);

-- 4. 为表信息表创建索引
create index idx_table_group on t_table(group_id);
create index idx_table_datasource on t_table(data_source_id);
create index idx_table_name on t_table(name);

-- 5. 为模型表创建索引
create index idx_model_name on data_model(name);
create index idx_model_type on data_model(type);
create index idx_model_status on data_model(status);

-- 6. 为物化任务表创建索引
create index idx_task_status on data_materialize_task(status);
create index idx_task_name on data_materialize_task(task_name);
create index idx_task_domain on data_materialize_task(domain_id);

-- 7. 为任务表配置创建索引
create index idx_task_table_task on data_task_table(task_id);
create index idx_task_table_model on data_task_table(model_id);

-- 8. 为任务版本创建索引
create index idx_task_version_task on data_task_version(task_id);
create index idx_task_version_status on data_task_version(status);



create table data_realtime_sync_task (
                                         id varchar(36) primary key,
                                         name varchar(100),
                                         description varchar(500),
                                         source_id varchar(36),
                                         source_name varchar(100),
                                         source_type varchar(50),
                                         target_id varchar(36),
                                         target_name varchar(100),
                                         target_type varchar(50),
                                         status varchar(20),
                                         sync_mode varchar(50),
                                         flink_job_id varchar(100),
                                         table_mappings clob,
                                         filter_condition varchar(500),
                                         parallelism int,
                                         checkpoint_interval int,
                                         restart_strategy varchar(50),
                                         max_restart_attempts int,
                                         restart_delay int,
                                         monitor_config clob,
                                         alert_thresholds clob,
                                         advanced_config clob,
                                         extra_config clob,
                                         last_run_time timestamp,
                                         last_run_result varchar(50),
                                         total_records_processed bigint,
                                         total_records_failed bigint,
                                         created_by varchar(50),
                                         create_time timestamp,
                                         updated_by varchar(50),
                                         update_time timestamp
);

comment on table data_realtime_sync_task is '实时同步任务表';
comment on column data_realtime_sync_task.id is '主键ID';
comment on column data_realtime_sync_task.name is '任务名称';
comment on column data_realtime_sync_task.description is '任务描述';
comment on column data_realtime_sync_task.source_id is '源数据源ID';
comment on column data_realtime_sync_task.source_name is '源数据源名称';
comment on column data_realtime_sync_task.source_type is '源数据源类型';
comment on column data_realtime_sync_task.target_id is '目标数据源ID';
comment on column data_realtime_sync_task.target_name is '目标数据源名称';
comment on column data_realtime_sync_task.target_type is '目标数据源类型';
comment on column data_realtime_sync_task.status is '任务状态（等待中、运行中、已完成、已停止、失败）';
comment on column data_realtime_sync_task.sync_mode is '同步模式（CDC、Kafka、实时API等）';
comment on column data_realtime_sync_task.flink_job_id is 'Flink作业ID';
comment on column data_realtime_sync_task.table_mappings is '表映射关系（JSON格式）';
comment on column data_realtime_sync_task.filter_condition is '数据过滤条件';
comment on column data_realtime_sync_task.parallelism is '并行度';
comment on column data_realtime_sync_task.checkpoint_interval is '检查点间隔（秒）';
comment on column data_realtime_sync_task.restart_strategy is '重启策略';
comment on column data_realtime_sync_task.max_restart_attempts is '最大重启次数';
comment on column data_realtime_sync_task.restart_delay is '重启延迟（秒）';
comment on column data_realtime_sync_task.monitor_config is '监控配置（JSON格式）';
comment on column data_realtime_sync_task.alert_thresholds is '告警阈值（JSON格式）';
comment on column data_realtime_sync_task.advanced_config is '高级配置（JSON格式）';
comment on column data_realtime_sync_task.extra_config is '额外配置（JSON格式）';
comment on column data_realtime_sync_task.last_run_time is '上次运行时间';
comment on column data_realtime_sync_task.last_run_result is '上次执行结果';
comment on column data_realtime_sync_task.total_records_processed is '记录处理总数';
comment on column data_realtime_sync_task.total_records_failed is '记录失败总数';
comment on column data_realtime_sync_task.created_by is '创建人';
comment on column data_realtime_sync_task.create_time is '创建时间';
comment on column data_realtime_sync_task.updated_by is '更新人';
comment on column data_realtime_sync_task.update_time is '更新时间';



create table data_realtime_sync_table_mapping (
                                                  id varchar(36) primary key,
                                                  sync_task_id varchar(36),
                                                  source_table varchar(100),
                                                  source_schema varchar(50),
                                                  target_table varchar(100),
                                                  target_schema varchar(50),
                                                  incremental_field varchar(100),
                                                  incremental_strategy varchar(50),
                                                  filter_condition varchar(500),
                                                  source_primary_key varchar(100),
                                                  field_mappings clob,
                                                  create_time timestamp,
                                                  update_time timestamp
);

comment on table data_realtime_sync_table_mapping is '表映射表';
comment on column data_realtime_sync_table_mapping.id is '主键ID';
comment on column data_realtime_sync_table_mapping.sync_task_id is '所属同步任务ID';
comment on column data_realtime_sync_table_mapping.source_table is '源表名称';
comment on column data_realtime_sync_table_mapping.source_schema is '源数据源模式';
comment on column data_realtime_sync_table_mapping.target_table is '目标表名称';
comment on column data_realtime_sync_table_mapping.target_schema is '目标数据源模式';
comment on column data_realtime_sync_table_mapping.incremental_field is '增量字段';
comment on column data_realtime_sync_table_mapping.incremental_strategy is '增量策略';
comment on column data_realtime_sync_table_mapping.filter_condition is '数据过滤条件';
comment on column data_realtime_sync_table_mapping.source_primary_key is '源表主键字段';
comment on column data_realtime_sync_table_mapping.field_mappings is '字段映射配置（JSON格式）';
comment on column data_realtime_sync_table_mapping.create_time is '创建时间';
comment on column data_realtime_sync_table_mapping.update_time is '更新时间';



create table data_quality_rule (
                                   id varchar(36) primary key,
                                   name varchar(100),
                                   type varchar(50),
                                   level varchar(20),
                                   description varchar(500),
                                   datasource_id varchar(36),
                                   database_name varchar(100),
                                   table_name varchar(100),
                                   template_id varchar(36),
                                   schedule_type varchar(20),
                                   cron_expression varchar(100),
                                   failure_policy varchar(20),
                                   retry_count int,
                                   retry_interval int,
                                   config clob,
                                   alert_config clob,
                                   status int,
                                   last_execution_result int,
                                   last_execution_time timestamp,
                                   creator varchar(50),
                                   create_time timestamp,
                                   updater varchar(50),
                                   update_time timestamp,
                                   tenant_id varchar(36)
);

comment on table data_quality_rule is '数据质量规则表';
comment on column data_quality_rule.id is '主键ID';
comment on column data_quality_rule.name is '规则名称';
comment on column data_quality_rule.type is '规则类型：completeness(完整性)、accuracy(准确性)、consistency(一致性)、timeliness(及时性)、uniqueness(唯一性)';
comment on column data_quality_rule.level is '规则级别：critical(严重)、warning(警告)、info(提示)';
comment on column data_quality_rule.description is '规则描述';
comment on column data_quality_rule.datasource_id is '数据源ID';
comment on column data_quality_rule.database_name is '数据库名称';
comment on column data_quality_rule.table_name is '表名称';
comment on column data_quality_rule.template_id is '规则模板ID';
comment on column data_quality_rule.schedule_type is '调度类型：manual(手动触发)、cron(定时调度)';
comment on column data_quality_rule.cron_expression is 'CRON表达式，定时调度时使用';
comment on column data_quality_rule.failure_policy is '失败策略：retry(重试)、ignore(忽略)、suspend(暂停任务)、alert(仅告警)';
comment on column data_quality_rule.retry_count is '重试次数';
comment on column data_quality_rule.retry_interval is '重试间隔（分钟）';
comment on column data_quality_rule.config is '规则配置，JSON格式';
comment on column data_quality_rule.alert_config is '告警配置，JSON格式';
comment on column data_quality_rule.status is '状态：0-禁用，1-启用';
comment on column data_quality_rule.last_execution_result is '最后执行结果：0-失败，1-成功，null-未执行';
comment on column data_quality_rule.last_execution_time is '最后执行时间';
comment on column data_quality_rule.creator is '创建人';
comment on column data_quality_rule.create_time is '创建时间';
comment on column data_quality_rule.updater is '更新人';
comment on column data_quality_rule.update_time is '更新时间';
comment on column data_quality_rule.tenant_id is '租户ID';



create table data_rule_template (
                                    id varchar(36) primary key,
                                    name varchar(100),
                                    rule_type varchar(50),
                                    source_type varchar(50),
                                    scenario varchar(50),
                                    description varchar(500),
                                    condition_template_type varchar(50),
                                    sql_template_type varchar(50),
                                    config clob,
                                    source_info clob,
                                    field_mappings clob,
                                    sql_field_mappings clob,
                                    use_count int,
                                    status varchar(20),
                                    is_system number(1),
                                    create_by varchar(50),
                                    create_time timestamp,
                                    update_by varchar(50),
                                    update_time timestamp
);

comment on table data_rule_template is '规则模板表';
comment on column data_rule_template.id is '主键ID';
comment on column data_rule_template.name is '模板名称';
comment on column data_rule_template.rule_type is '规则类型: completeness-完整性, accuracy-准确性, consistency-一致性, timeliness-时效性, uniqueness-唯一性, validity-有效性';
comment on column data_rule_template.source_type is '数据源类型';
comment on column data_rule_template.scenario is '适用场景: ingestion-数据接入, processing-数据处理, storage-数据存储, distribution-数据分发, analysis-数据分析';
comment on column data_rule_template.description is '模板描述';
comment on column data_rule_template.condition_template_type is '条件模板类型';
comment on column data_rule_template.sql_template_type is 'SQL模板类型';
comment on column data_rule_template.config is '配置信息 (JSON格式)';
comment on column data_rule_template.source_info is '数据源信息 (JSON格式)';
comment on column data_rule_template.field_mappings is '字段映射关系 (JSON格式)';
comment on column data_rule_template.sql_field_mappings is 'SQL字段映射关系 (JSON格式)';
comment on column data_rule_template.use_count is '使用次数';
comment on column data_rule_template.status is '状态: enabled-启用, disabled-禁用, archived-归档';
comment on column data_rule_template.is_system is '是否系统内置';
comment on column data_rule_template.create_by is '创建人ID';
comment on column data_rule_template.create_time is '创建时间';
comment on column data_rule_template.update_by is '更新人ID';
comment on column data_rule_template.update_time is '更新时间';


create table data_encrypt_rule (
                                   id varchar(36) primary key,
                                   rule_name varchar(100),
                                   description varchar(500),
                                   datasource_id varchar(36),
                                   datasource_name varchar(100),
                                   table_name varchar(100),
                                   table_display_name varchar(100),
                                   field_name varchar(100),
                                   field_display_name varchar(100),
                                   encrypt_type varchar(50),
                                   scenario_type varchar(50),
                                   is_custom_algorithm number(1),
                                   custom_algorithm_class varchar(200),
                                   encrypt_key varchar(200),
                                   status varchar(20),
                                   priority int,
                                   creator varchar(50),
                                   create_time timestamp,
                                   updater varchar(50),
                                   update_time timestamp,
                                   deleted int
);

comment on table data_encrypt_rule is '加密规则表';
comment on column data_encrypt_rule.id is '主键ID';
comment on column data_encrypt_rule.rule_name is '规则名称';
comment on column data_encrypt_rule.description is '描述';
comment on column data_encrypt_rule.datasource_id is '数据源ID';
comment on column data_encrypt_rule.datasource_name is '数据源名称';
comment on column data_encrypt_rule.table_name is '表名';
comment on column data_encrypt_rule.table_display_name is '表显示名';
comment on column data_encrypt_rule.field_name is '字段名';
comment on column data_encrypt_rule.field_display_name is '字段显示名';
comment on column data_encrypt_rule.encrypt_type is '加密类型: aes, sm4, md5, sha256, custom';
comment on column data_encrypt_rule.scenario_type is '场景类型: email, phone, idcard, name, address, bank_card, custom';
comment on column data_encrypt_rule.is_custom_algorithm is '是否使用自定义加密算法';
comment on column data_encrypt_rule.custom_algorithm_class is '自定义加密算法类名';
comment on column data_encrypt_rule.encrypt_key is '加密密钥';
comment on column data_encrypt_rule.status is '状态: enabled, disabled';
comment on column data_encrypt_rule.priority is '优先级';
comment on column data_encrypt_rule.creator is '创建者';
comment on column data_encrypt_rule.create_time is '创建时间';
comment on column data_encrypt_rule.updater is '更新者';
comment on column data_encrypt_rule.update_time is '更新时间';
comment on column data_encrypt_rule.deleted is '是否删除 0-未删除 1-已删除';


create table data_mask_rule (
                                id varchar(36) primary key,
                                data_source varchar(100),
                                object_name varchar(100),
                                field_name varchar(100),
                                scenario_type varchar(50),
                                priority int,
                                mask_type varchar(50),
                                keep_prefix int,
                                keep_suffix int,
                                fixed_length number(1),
                                replace_length int,
                                replace_char varchar(10),
                                status varchar(20),
                                description varchar(500),
                                create_time timestamp,
                                update_time timestamp
);

comment on table data_mask_rule is '脱敏规则表';
comment on column data_mask_rule.id is '主键ID';
comment on column data_mask_rule.data_source is '数据源';
comment on column data_mask_rule.object_name is '对象名称（表名）';
comment on column data_mask_rule.field_name is '字段名称';
comment on column data_mask_rule.scenario_type is '场景类型：phone(手机号), idcard(身份证), email(邮箱), name(姓名), address(地址), custom(自定义)';
comment on column data_mask_rule.priority is '优先级：数字越大优先级越高';
comment on column data_mask_rule.mask_type is '脱敏类型：full(全部脱敏), partial(部分脱敏), custom(自定义脱敏)';
comment on column data_mask_rule.keep_prefix is '保留前缀数量（部分脱敏时有效）';
comment on column data_mask_rule.keep_suffix is '保留后缀数量（部分脱敏时有效）';
comment on column data_mask_rule.fixed_length is '是否固定替换长度（部分脱敏时有效）';
comment on column data_mask_rule.replace_length is '替换长度（固定长度时有效）';
comment on column data_mask_rule.replace_char is '替换字符';
comment on column data_mask_rule.status is '状态：enabled(启用)，disabled(禁用)';
comment on column data_mask_rule.description is '描述';
comment on column data_mask_rule.create_time is '创建时间';
comment on column data_mask_rule.update_time is '更新时间';


-- 实时同步任务索引
create index idx_rsync_task_name on data_realtime_sync_task(name);
create index idx_rsync_task_status on data_realtime_sync_task(status);
create index idx_rsync_source_target on data_realtime_sync_task(source_id, target_id);

-- 表映射索引
create index idx_table_mapping_task on data_realtime_sync_table_mapping(sync_task_id);
create index idx_table_mapping_tables on data_realtime_sync_table_mapping(source_table, target_table);

-- 数据质量规则索引
create index idx_rule_name on data_quality_rule(name);
create index idx_rule_datasource on data_quality_rule(datasource_id);
create index idx_rule_template on data_quality_rule(template_id);
create index idx_rule_table on data_quality_rule(table_name);
create index idx_rule_status on data_quality_rule(status);

-- 规则模板索引
create index idx_rule_template_name on data_rule_template(name);
create index idx_rule_template_type on data_rule_template(rule_type);
create index idx_rule_template_status on data_rule_template(status);

-- 加密规则索引
create index idx_encrypt_rule_name on data_encrypt_rule(rule_name);
create index idx_encrypt_datasource on data_encrypt_rule(datasource_id);
create index idx_encrypt_table_field on data_encrypt_rule(table_name, field_name);
create index idx_encrypt_status on data_encrypt_rule(status);

-- 脱敏规则索引
create index idx_mask_rule_source on data_mask_rule(data_source);
create index idx_mask_rule_object on data_mask_rule(object_name);
create index idx_mask_rule_field on data_mask_rule(field_name);
create index idx_mask_rule_status on data_mask_rule(status);
create index idx_mask_rule_scenario on data_mask_rule(scenario_type);


create table t_api (
                       id varchar(36) primary key,
                       name varchar(100),
                       path varchar(200),
                       method varchar(20),
                       status varchar(50),
                       owner varchar(50),
                       description varchar(500),
                       create_time date,
                       update_time date,
                       request_example clob,
                       response_example clob,
                       type varchar(50),
                       requires_auth number(1),
                       parameters clob,
                       headers clob,
                       response_schemas clob,
                       is_deleted number(1),
                       config clob
);

comment on table t_api is 'API实体表';
comment on column t_api.id is 'API ID';
comment on column t_api.name is 'API名称';
comment on column t_api.path is 'API路径';
comment on column t_api.method is '请求方法';
comment on column t_api.status is 'API状态：developing-开发中，testing-测试中，published-已发布，offline-已下线，deprecated-已弃用';
comment on column t_api.owner is '负责人';
comment on column t_api.description is 'API描述';
comment on column t_api.create_time is '创建时间';
comment on column t_api.update_time is '更新时间';
comment on column t_api.request_example is '请求示例';
comment on column t_api.response_example is '响应示例';
comment on column t_api.type is 'API类型：rest, graphql, soap, websocket';
comment on column t_api.requires_auth is '是否需要认证';
comment on column t_api.parameters is '请求参数，JSON格式';
comment on column t_api.headers is '请求头，JSON格式';
comment on column t_api.response_schemas is '响应模式，JSON格式';
comment on column t_api.is_deleted is '是否已删除';
comment on column t_api.config is '完整API配置，JSON格式，用于存储API向导的所有步骤数据';


create table data_optimization_suggestion (
                                              id varchar(36) primary key,
                                              priority varchar(20),
                                              title varchar(200),
                                              description varchar(500),
                                              benefits clob,
                                              steps clob,
                                              status varchar(50),
                                              applied_time timestamp,
                                              ignored_time timestamp,
                                              ignore_reason varchar(500),
                                              issue_id varchar(36),
                                              record_time timestamp,
                                              create_time timestamp,
                                              create_by varchar(50)
);

comment on table data_optimization_suggestion is '优化建议表';
comment on column data_optimization_suggestion.id is '主键ID（UUID）';
comment on column data_optimization_suggestion.priority is '优先级（高, 中, 低）';
comment on column data_optimization_suggestion.title is '建议标题';
comment on column data_optimization_suggestion.description is '建议描述';
comment on column data_optimization_suggestion.benefits is '预期收益（JSON数组）';
comment on column data_optimization_suggestion.steps is '优化步骤（JSON数组）';
comment on column data_optimization_suggestion.status is '状态（待处理, 已应用, 已忽略）';
comment on column data_optimization_suggestion.applied_time is '应用时间';
comment on column data_optimization_suggestion.ignored_time is '忽略时间';
comment on column data_optimization_suggestion.ignore_reason is '忽略原因';
comment on column data_optimization_suggestion.issue_id is '相关问题ID';
comment on column data_optimization_suggestion.record_time is '记录时间';
comment on column data_optimization_suggestion.create_time is '创建时间';
comment on column data_optimization_suggestion.create_by is '创建人';


create table data_performance_analysis (
                                           id varchar(36) primary key,
                                           analysis_type varchar(50),
                                           metric_name varchar(100),
                                           metric_value number(20,6),
                                           metric_unit varchar(20),
                                           percentage int,
                                           tip varchar(500),
                                           record_time timestamp,
                                           create_time timestamp,
                                           create_by varchar(50)
);

comment on table data_performance_analysis is '性能分析表';
comment on column data_performance_analysis.id is '主键ID（UUID）';
comment on column data_performance_analysis.analysis_type is '分析类型（cpu, memory, io, query）';
comment on column data_performance_analysis.metric_name is '指标名称';
comment on column data_performance_analysis.metric_value is '指标值';
comment on column data_performance_analysis.metric_unit is '指标单位';
comment on column data_performance_analysis.percentage is '百分比值（用于进度条展示）';
comment on column data_performance_analysis.tip is '提示信息';
comment on column data_performance_analysis.record_time is '记录时间';
comment on column data_performance_analysis.create_time is '创建时间';
comment on column data_performance_analysis.create_by is '创建人';



create table data_performance_diagnosis (
                                            id varchar(36) primary key,
                                            level varchar(20),
                                            title varchar(200),
                                            description varchar(500),
                                            impact varchar(200),
                                            duration varchar(100),
                                            service_id varchar(36),
                                            diagnosis_time timestamp,
                                            resolved int,
                                            solution clob,
                                            resolve_time timestamp,
                                            create_time timestamp,
                                            create_by varchar(50),
                                            update_time timestamp,
                                            update_by varchar(50),
                                            deleted int
);

comment on table data_performance_diagnosis is '性能诊断表';
comment on column data_performance_diagnosis.id is '主键ID';
comment on column data_performance_diagnosis.level is '问题级别：info, warning, danger';
comment on column data_performance_diagnosis.title is '问题标题';
comment on column data_performance_diagnosis.description is '问题描述';
comment on column data_performance_diagnosis.impact is '影响程度';
comment on column data_performance_diagnosis.duration is '持续时间';
comment on column data_performance_diagnosis.service_id is '服务ID';
comment on column data_performance_diagnosis.diagnosis_time is '诊断时间';
comment on column data_performance_diagnosis.resolved is '是否已解决：0-未解决，1-已解决';
comment on column data_performance_diagnosis.solution is '解决方案';
comment on column data_performance_diagnosis.resolve_time is '解决时间';
comment on column data_performance_diagnosis.create_time is '创建时间';
comment on column data_performance_diagnosis.create_by is '创建者';
comment on column data_performance_diagnosis.update_time is '更新时间';
comment on column data_performance_diagnosis.update_by is '更新者';
comment on column data_performance_diagnosis.deleted is '逻辑删除标识：0-未删除，1-已删除';


create table data_performance_issue (
                                        id varchar(36) primary key,
                                        level varchar(20),
                                        title varchar(200),
                                        description varchar(500),
                                        impact varchar(200),
                                        duration varchar(100),
                                        issue_time timestamp,
                                        resolved number(1),
                                        resolved_time timestamp,
                                        solution clob,
                                        record_time timestamp,
                                        create_time timestamp,
                                        create_by varchar(50)
);

comment on table data_performance_issue is '性能问题表';
comment on column data_performance_issue.id is '主键ID（UUID）';
comment on column data_performance_issue.level is '问题级别（danger, warning, info）';
comment on column data_performance_issue.title is '问题标题';
comment on column data_performance_issue.description is '问题描述';
comment on column data_performance_issue.impact is '影响程度';
comment on column data_performance_issue.duration is '持续时间';
comment on column data_performance_issue.issue_time is '发现时间';
comment on column data_performance_issue.resolved is '是否已解决';
comment on column data_performance_issue.resolved_time is '解决时间';
comment on column data_performance_issue.solution is '解决方案';
comment on column data_performance_issue.record_time is '记录时间';
comment on column data_performance_issue.create_time is '创建时间';
comment on column data_performance_issue.create_by is '创建人';


create table data_performance_metric (
                                         id varchar(36) primary key,
                                         metric_type varchar(50),
                                         metric_value number(20,6),
                                         metric_unit varchar(20),
                                         status varchar(20),
                                         trend varchar(20),
                                         change_percent number(10,2),
                                         record_time timestamp,
                                         create_time timestamp,
                                         create_by varchar(50),
                                         label varchar(100),
                                         tip varchar(500)
);

comment on table data_performance_metric is '性能指标表';
comment on column data_performance_metric.id is '主键ID（UUID）';
comment on column data_performance_metric.metric_type is '指标类型（cpu, memory, disk_io, response_time）';
comment on column data_performance_metric.metric_value is '指标值';
comment on column data_performance_metric.metric_unit is '指标单位';
comment on column data_performance_metric.status is '状态（normal, warning, danger）';
comment on column data_performance_metric.trend is '变化趋势（up, down）';
comment on column data_performance_metric.change_percent is '变化百分比';
comment on column data_performance_metric.record_time is '记录时间';
comment on column data_performance_metric.create_time is '创建时间';
comment on column data_performance_metric.create_by is '创建人';
comment on column data_performance_metric.label is '标签';
comment on column data_performance_metric.tip is '提示信息';


create table data_service_log (
                                  id varchar(36) primary key,
                                  service_id varchar(36),
                                  service_name varchar(100),
                                  level varchar(20),
                                  message clob,
                                  source varchar(100),
                                  log_time timestamp,
                                  create_time timestamp,
                                  create_by varchar(50),
                                  update_time timestamp,
                                  update_by varchar(50)
);

comment on table data_service_log is '服务日志表';
comment on column data_service_log.id is '主键ID';
comment on column data_service_log.service_id is '服务ID';
comment on column data_service_log.service_name is '服务名称';
comment on column data_service_log.level is '日志级别：info, warning, error, debug';
comment on column data_service_log.message is '日志内容';
comment on column data_service_log.source is '来源';
comment on column data_service_log.log_time is '日志时间';
comment on column data_service_log.create_time is '创建时间';
comment on column data_service_log.create_by is '创建人';
comment on column data_service_log.update_time is '更新时间';
comment on column data_service_log.update_by is '更新人';


create table data_service_performance (
                                          id varchar(36) primary key,
                                          service_id varchar(36),
                                          service_name varchar(100),
                                          response_time number(10,2),
                                          request_count int,
                                          error_count int,
                                          error_rate number(10,4),
                                          record_time timestamp,
                                          create_time timestamp,
                                          create_by varchar(50),
                                          update_time timestamp,
                                          update_by varchar(50)
);

comment on table data_service_performance is '服务性能监控表';
comment on column data_service_performance.id is '主键ID';
comment on column data_service_performance.service_id is '服务ID';
comment on column data_service_performance.service_name is '服务名称';
comment on column data_service_performance.response_time is '平均响应时间(ms)';
comment on column data_service_performance.request_count is '请求数';
comment on column data_service_performance.error_count is '错误数';
comment on column data_service_performance.error_rate is '错误率';
comment on column data_service_performance.record_time is '记录时间';
comment on column data_service_performance.create_time is '创建时间';
comment on column data_service_performance.create_by is '创建人';
comment on column data_service_performance.update_time is '更新时间';
comment on column data_service_performance.update_by is '更新人';


create table data_service_resource (
                                       id varchar(36) primary key,
                                       service_id varchar(36),
                                       service_name varchar(100),
                                       cpu_usage number(5,2),
                                       memory_usage number(5,2),
                                       disk_usage number(5,2),
                                       disk_io number(10,2),
                                       record_time timestamp,
                                       create_time timestamp,
                                       create_by varchar(50),
                                       update_time timestamp,
                                       update_by varchar(50)
);

comment on table data_service_resource is '服务资源监控表';
comment on column data_service_resource.id is '主键ID';
comment on column data_service_resource.service_id is '服务ID';
comment on column data_service_resource.service_name is '服务名称';
comment on column data_service_resource.cpu_usage is 'CPU使用率';
comment on column data_service_resource.memory_usage is '内存使用率';
comment on column data_service_resource.disk_usage is '磁盘使用率';
comment on column data_service_resource.disk_io is '磁盘IO (MB/s)';
comment on column data_service_resource.record_time is '记录时间';
comment on column data_service_resource.create_time is '创建时间';
comment on column data_service_resource.create_by is '创建人';
comment on column data_service_resource.update_time is '更新时间';
comment on column data_service_resource.update_by is '更新人';


-- API表索引
create index idx_api_name on t_api(name);
create index idx_api_path on t_api(path);
create index idx_api_status on t_api(status);

-- 优化建议表索引
create index idx_optimization_priority on data_optimization_suggestion(priority);
create index idx_optimization_status on data_optimization_suggestion(status);
create index idx_optimization_issue on data_optimization_suggestion(issue_id);

-- 性能分析表索引
create index idx_performance_analysis_type on data_performance_analysis(analysis_type);
create index idx_performance_analysis_record on data_performance_analysis(record_time);

-- 性能诊断表索引
create index idx_performance_diagnosis_level on data_performance_diagnosis(level);
create index idx_performance_diagnosis_resolved on data_performance_diagnosis(resolved);
create index idx_performance_diagnosis_service on data_performance_diagnosis(service_id);

-- 性能问题表索引
create index idx_performance_issue_level on data_performance_issue(level);
create index idx_performance_issue_resolved on data_performance_issue(resolved);
create index idx_performance_issue_time on data_performance_issue(issue_time);

-- 性能指标表索引
create index idx_performance_metric_type on data_performance_metric(metric_type);
create index idx_performance_metric_status on data_performance_metric(status);
create index idx_performance_metric_record on data_performance_metric(record_time);

-- 服务日志表索引
create index idx_service_log_service on data_service_log(service_id);
create index idx_service_log_level on data_service_log(level);
create index idx_service_log_time on data_service_log(log_time);

-- 服务性能监控表索引
create index idx_service_performance_service on data_service_performance(service_id);
create index idx_service_performance_time on data_service_performance(record_time);

-- 服务资源监控表索引
create index idx_service_resource_service on data_service_resource(service_id);
create index idx_service_resource_time on data_service_resource(record_time);



create table sys_datasource (
                                id varchar(36) primary key,
                                name varchar(100),
                                type varchar(50),
                                db_name varchar(100),
                                url varchar(500),
                                host varchar(100),
                                port varchar(20),
                                username varchar(100),
                                password varchar(100),
                                initial_size int,
                                max_active int,
                                min_idle int,
                                max_wait int,
                                validation_query varchar(200),
                                test_on_borrow number(1),
                                test_on_return number(1),
                                test_while_idle number(1),
                                status number(1),
                                path varchar(500),
                                bucket varchar(100),
                                access_key varchar(100),
                                secret_key varchar(100),
                                region varchar(50),
                                endpoint varchar(200),
                                namenode varchar(200),
                                properties clob,
                                connection_type varchar(50),
                                create_time date,
                                create_by varchar(50),
                                update_time date,
                                update_by varchar(50),
                                delete_flg int,
                                remark varchar(500)
);

comment on table sys_datasource is '数据源表';
comment on column sys_datasource.id is '主键ID';
comment on column sys_datasource.name is '数据源名称';
comment on column sys_datasource.type is '数据源类型';
comment on column sys_datasource.db_name is '数据库名称';
comment on column sys_datasource.url is '数据库连接URL';
comment on column sys_datasource.host is '数据库主机地址';
comment on column sys_datasource.port is '数据库端口号';
comment on column sys_datasource.username is '数据库用户名';
comment on column sys_datasource.password is '数据库密码';
comment on column sys_datasource.initial_size is '连接池初始化大小';
comment on column sys_datasource.max_active is '连接池最大活跃连接数';
comment on column sys_datasource.min_idle is '连接池最小空闲连接数';
comment on column sys_datasource.max_wait is '连接池获取连接等待超时时间(毫秒)';
comment on column sys_datasource.validation_query is '验证查询SQL';
comment on column sys_datasource.test_on_borrow is '申请连接时执行validationQuery检测连接是否有效';
comment on column sys_datasource.test_on_return is '归还连接时执行validationQuery检测连接是否有效';
comment on column sys_datasource.test_while_idle is '空闲时执行validationQuery检测连接是否有效';
comment on column sys_datasource.status is '数据源状态: 1-启用, 0-禁用';
comment on column sys_datasource.path is '本地文件系统路径';
comment on column sys_datasource.bucket is '云存储桶名称';
comment on column sys_datasource.access_key is '访问密钥标识';
comment on column sys_datasource.secret_key is '访问密钥密码';
comment on column sys_datasource.region is '云服务区域';
comment on column sys_datasource.endpoint is '服务终端节点';
comment on column sys_datasource.namenode is 'HDFS命名空间';
comment on column sys_datasource.connection_type is '连接类型: SID或SERVICE_NAME (主要用于Oracle连接)';
comment on column sys_datasource.create_time is '创建时间';
comment on column sys_datasource.create_by is '创建人';
comment on column sys_datasource.update_time is '更新时间';
comment on column sys_datasource.update_by is '更新人';
comment on column sys_datasource.delete_flg is '删除标志(0:未删除,1:已删除)';
comment on column sys_datasource.remark is '备注';


create index idx_datasource_name on sys_datasource(name);
create index idx_datasource_type on sys_datasource(type);
create index idx_datasource_status on sys_datasource(status);
create index idx_datasource_delete on sys_datasource(delete_flg);

alter table sys_datasource modify delete_flg int default 0;
alter table sys_datasource modify status number(1) default 1;

alter table sys_datasource modify name varchar(100) not null;
alter table sys_datasource modify type varchar(50) not null;




create table data_sync_task (
                                id varchar(36) primary key,
                                name varchar(100),
                                schedule_type varchar(20),
                                schedule_time timestamp,
                                expire_time timestamp,
                                periodic_type varchar(20),
                                hourly_minute int,
                                periodic_time varchar(50),
                                week_days varchar(100),
                                month_days varchar(100),
                                cron_expression varchar(100),
                                effective_time timestamp,
                                expiry_time timestamp,
                                schedule_timeout int,
                                priority varchar(20),
                                sync_mode varchar(20),
                                incremental_strategy varchar(20),
                                incremental_mode varchar(20),
                                incremental_field varchar(100),
                                snapshot_compare_mode varchar(20),
                                snapshot_frequency varchar(20),
                                partition_detection_mode varchar(20),
                                partition_format varchar(100),
                                hash_algorithm varchar(20),
                                chunk_size varchar(50),
                                start_time timestamp,
                                start_value bigint,
                                incremental_config_mode varchar(20),
                                cdc_mode varchar(20),
                                cdc_tool varchar(50),
                                cdc_operations varchar(100),
                                change_table_name varchar(100),
                                operation_field varchar(100),
                                timestamp_field varchar(100),
                                dependencies varchar(500),
                                dependency_mode varchar(20),
                                retry_enabled number(1),
                                retry_times int,
                                retry_interval int,
                                retry_strategy varchar(20),
                                max_wait_time int,
                                retry_conditions varchar(500),
                                retry_actions varchar(500),
                                performance_enabled number(1),
                                parallelism int,
                                batch_size int,
                                buffer_size int,
                                reader_concurrency int,
                                writer_concurrency int,
                                throttle_rate int,
                                table_mappings clob,
                                status varchar(20),
                                create_time timestamp,
                                update_time timestamp,
                                creator varchar(50),
                                last_execute_time timestamp,
                                last_execute_result varchar(50),
                                execute_count int,
                                source_view_type varchar(20),
                                target_view_type varchar(20),
                                validation_enabled number(1),
                                validation_types varchar(100),
                                validation_time varchar(20),
                                validation_fail_action varchar(20),
                                sample_rate int,
                                primary_key_field varchar(100),
                                last_increment_value varchar(100),
                                partition_field varchar(100),
                                last_partition varchar(100),
                                temporary_tables clob
);

comment on table data_sync_task is '离线同步任务表';
comment on column data_sync_task.id is '主键ID';
comment on column data_sync_task.name is '任务名称';
comment on column data_sync_task.schedule_type is '调度类型：manual(手动执行)、scheduled(定时执行)、periodic(周期执行)';
comment on column data_sync_task.schedule_time is '定时执行时间';
comment on column data_sync_task.expire_time is '定时过期时间';
comment on column data_sync_task.periodic_type is '周期类型：hourly、daily、weekly、monthly、cron';
comment on column data_sync_task.hourly_minute is '小时执行的分钟数（针对hourly）';
comment on column data_sync_task.periodic_time is '定期执行时间（非hourly类型）';
comment on column data_sync_task.week_days is '周几执行，多个值用逗号分隔（针对weekly）';
comment on column data_sync_task.month_days is '每月哪天执行，多个值用逗号分隔（针对monthly）';
comment on column data_sync_task.cron_expression is 'Cron表达式（针对cron类型）';
comment on column data_sync_task.effective_time is '生效时间';
comment on column data_sync_task.expiry_time is '失效时间';
comment on column data_sync_task.schedule_timeout is '调度超时时间（分钟）';
comment on column data_sync_task.priority is '任务优先级：low、medium、high';
comment on column data_sync_task.sync_mode is '同步模式：full(全量同步)、incremental(增量同步)';
comment on column data_sync_task.incremental_strategy is '增量策略：field、snapshot、partition、hash、cdc';
comment on column data_sync_task.incremental_mode is '增量模式：timestamp、sequence';
comment on column data_sync_task.incremental_field is '增量字段';
comment on column data_sync_task.snapshot_compare_mode is '比对方式：full、hash';
comment on column data_sync_task.snapshot_frequency is '快照频率';
comment on column data_sync_task.partition_detection_mode is '分区检测方式：auto、manual';
comment on column data_sync_task.partition_format is '分区格式';
comment on column data_sync_task.hash_algorithm is '哈希算法：MD5、SHA-1、CRC32';
comment on column data_sync_task.chunk_size is '分片大小';
comment on column data_sync_task.start_time is '时间戳增量开始时间';
comment on column data_sync_task.start_value is '序列号增量起始值';
comment on column data_sync_task.incremental_config_mode is '多表增量配置方式：shared、individual';
comment on column data_sync_task.cdc_mode is 'CDC方式：log、tool';
comment on column data_sync_task.cdc_tool is 'CDC工具：debezium、canal、other';
comment on column data_sync_task.cdc_operations is 'CDC操作类型，多个值用逗号分隔';
comment on column data_sync_task.change_table_name is '记录变更的表名';
comment on column data_sync_task.operation_field is '记录操作类型的字段名';
comment on column data_sync_task.timestamp_field is '记录变更时间的字段名';
comment on column data_sync_task.dependencies is '任务依赖，多个值用逗号分隔';
comment on column data_sync_task.dependency_mode is '依赖模式：all、any';
comment on column data_sync_task.retry_enabled is '是否启用重试';
comment on column data_sync_task.retry_times is '重试次数';
comment on column data_sync_task.retry_interval is '重试间隔（分钟）';
comment on column data_sync_task.retry_strategy is '重试策略：fixed、exponential';
comment on column data_sync_task.max_wait_time is '最大等待时间（分钟）';
comment on column data_sync_task.retry_conditions is '重试条件，多个值用逗号分隔';
comment on column data_sync_task.retry_actions is '重试前操作，多个值用逗号分隔';
comment on column data_sync_task.performance_enabled is '性能参数是否启用';
comment on column data_sync_task.parallelism is '并行度';
comment on column data_sync_task.batch_size is '批量大小';
comment on column data_sync_task.buffer_size is '缓冲区大小(MB)';
comment on column data_sync_task.reader_concurrency is '读取并发数';
comment on column data_sync_task.writer_concurrency is '写入并发数';
comment on column data_sync_task.throttle_rate is '限流速率(MB/s)';
comment on column data_sync_task.table_mappings is '表映射配置（JSON格式）';
comment on column data_sync_task.status is '任务状态：created(已创建)、running(运行中)、paused(已暂停)、completed(已完成)、failed(失败)';
comment on column data_sync_task.create_time is '创建时间';
comment on column data_sync_task.update_time is '更新时间';
comment on column data_sync_task.creator is '创建人';
comment on column data_sync_task.last_execute_time is '最后执行时间';
comment on column data_sync_task.last_execute_result is '最后执行结果';
comment on column data_sync_task.execute_count is '执行次数';
comment on column data_sync_task.source_view_type is '源数据视图类型：domain(主题域)、datasource(数据源)';
comment on column data_sync_task.target_view_type is '目标源视图类型：domain(主题域)、datasource(数据源)';
comment on column data_sync_task.validation_enabled is '验证是否启用';
comment on column data_sync_task.validation_types is '验证类型，多个值用逗号分隔';
comment on column data_sync_task.validation_time is '验证时间：beforeSync、afterSync、both';
comment on column data_sync_task.validation_fail_action is '验证失败操作：continue、abort、retry';
comment on column data_sync_task.sample_rate is '采样率(%)';
comment on column data_sync_task.primary_key_field is '主键字段名';
comment on column data_sync_task.last_increment_value is '最近同步的增量值';
comment on column data_sync_task.partition_field is '分区字段名';
comment on column data_sync_task.last_partition is '最近同步的分区值';
comment on column data_sync_task.temporary_tables is '临时表名列表';


create table data_sync_task_log (
                                    id varchar(36) primary key,
                                    task_id varchar(36),
                                    task_name varchar(100),
                                    start_time timestamp,
                                    end_time timestamp,
                                    status varchar(20),
                                    execution_result varchar(50),
                                    total_record_count bigint,
                                    success_record_count bigint,
                                    failed_record_count bigint,
                                    total_byte_size bigint,
                                    execution_time bigint,
                                    error_message clob,
                                    datax_job_id varchar(100),
                                    datax_config clob,
                                    datax_output clob,
                                    sync_mode varchar(20),
                                    incremental_strategy varchar(20),
                                    performance_config clob,
                                    table_mapping_info clob,
                                    table_process_detail clob,
                                    retry_count int,
                                    warning_messages clob,
                                    task_version int
);

comment on table data_sync_task_log is '同步任务执行日志表';
comment on column data_sync_task_log.id is '日志ID';
comment on column data_sync_task_log.task_id is '任务ID';
comment on column data_sync_task_log.task_name is '任务名称';
comment on column data_sync_task_log.start_time is '开始时间';
comment on column data_sync_task_log.end_time is '结束时间';
comment on column data_sync_task_log.status is '执行状态';
comment on column data_sync_task_log.execution_result is '执行结果';
comment on column data_sync_task_log.total_record_count is '总记录数';
comment on column data_sync_task_log.success_record_count is '成功记录数';
comment on column data_sync_task_log.failed_record_count is '失败记录数';
comment on column data_sync_task_log.total_byte_size is '总字节数';
comment on column data_sync_task_log.execution_time is '执行时间（毫秒）';
comment on column data_sync_task_log.error_message is '错误信息';
comment on column data_sync_task_log.datax_job_id is 'DataX作业ID';
comment on column data_sync_task_log.datax_config is 'DataX配置';
comment on column data_sync_task_log.datax_output is 'DataX输出';
comment on column data_sync_task_log.sync_mode is '同步模式：full(全量同步)、incremental(增量同步)';
comment on column data_sync_task_log.incremental_strategy is '增量策略：field、snapshot、partition、hash、cdc';
comment on column data_sync_task_log.performance_config is '性能配置：并行度、批量大小等参数的JSON';
comment on column data_sync_task_log.table_mapping_info is '同步的表映射JSON，记录哪些表参与了同步';
comment on column data_sync_task_log.table_process_detail is '同步过程中处理的记录数明细，JSON格式记录每个表的处理情况';
comment on column data_sync_task_log.retry_count is '重试次数';
comment on column data_sync_task_log.warning_messages is '警告信息，记录执行过程中的非致命问题';
comment on column data_sync_task_log.task_version is '同步任务版本号，用于跟踪任务配置变更';


create table data_sync_task_step_log (
                                         id varchar(100) primary key,
                                         task_log_id varchar(36),
                                         task_id varchar(36),
                                         step_number int,
                                         step_name varchar(100),
                                         step_type varchar(50),
                                         step_description varchar(500),
                                         start_time timestamp,
                                         end_time timestamp,
                                         status varchar(20),
                                         result_message varchar(500),
                                         context_info clob,
                                         data_count bigint,
                                         detail_log clob,
                                         execution_time bigint,
                                         duration bigint,
                                         detail clob,
                                         error_message clob,
                                         affected_tables varchar(500),
                                         affected_records bigint,
                                         extra_info clob,
                                         create_time timestamp
);

comment on table data_sync_task_step_log is '同步任务步骤日志表';
comment on column data_sync_task_step_log.id is '步骤日志ID，使用组合ID：任务日志ID_步骤类型';
comment on column data_sync_task_step_log.task_log_id is '关联的任务日志ID';
comment on column data_sync_task_step_log.task_id is '关联的任务ID';
comment on column data_sync_task_step_log.step_number is '步骤序号';
comment on column data_sync_task_step_log.step_name is '步骤名称';
comment on column data_sync_task_step_log.step_type is '步骤类型';
comment on column data_sync_task_step_log.step_description is '步骤描述';
comment on column data_sync_task_step_log.start_time is '步骤开始时间';
comment on column data_sync_task_step_log.end_time is '步骤结束时间';
comment on column data_sync_task_step_log.status is '步骤状态：success, warning, failure';
comment on column data_sync_task_step_log.result_message is '步骤执行结果消息';
comment on column data_sync_task_step_log.context_info is '步骤执行上下文信息';
comment on column data_sync_task_step_log.data_count is '处理数据量';
comment on column data_sync_task_step_log.detail_log is '步骤详细日志';
comment on column data_sync_task_step_log.execution_time is '步骤执行耗时（毫秒）';
comment on column data_sync_task_step_log.duration is '步骤执行时长(毫秒)';
comment on column data_sync_task_step_log.detail is '步骤详情，记录步骤的详细信息';
comment on column data_sync_task_step_log.error_message is '错误信息，如果步骤执行失败';
comment on column data_sync_task_step_log.affected_tables is '影响的表，如果步骤涉及特定表';
comment on column data_sync_task_step_log.affected_records is '影响的记录数，如果步骤处理了记录';
comment on column data_sync_task_step_log.extra_info is '补充信息，JSON格式记录其他相关信息';
comment on column data_sync_task_step_log.create_time is '创建时间';


create table data_sync_table_mapping (
                                         id varchar(36) primary key,
                                         sync_task_id varchar(36),
                                         source_table_id varchar(36),
                                         source_table varchar(100),
                                         source_data_source_id varchar(36),
                                         source_data_source_name varchar(100),
                                         source_schema varchar(50),
                                         target_table_id varchar(36),
                                         target_table varchar(100),
                                         target_data_source_id varchar(36),
                                         target_data_source_name varchar(100),
                                         target_schema varchar(50),
                                         incremental_field varchar(100),
                                         incremental_strategy varchar(50),
                                         incremental_mode varchar(20),
                                         timestamp_field varchar(100),
                                         operation_field varchar(100),
                                         change_table_name varchar(100),
                                         filter_condition varchar(500),
                                         where_condition varchar(500),
                                         validation_enabled number(1),
                                         validation_types varchar(100),
                                         row_count_tolerance int,
                                         row_count_mode varchar(20),
                                         checksum_fields varchar(500),
                                         checksum_algorithm varchar(20),
                                         sample_rate int,
                                         sample_method varchar(20),
                                         sample_fields varchar(500),
                                         validation_fail_action varchar(20),
                                         selected_columns varchar(500),
                                         manual_partitions varchar(500),
                                         field_mappings clob,
                                         create_time timestamp,
                                         update_time timestamp
);

comment on table data_sync_table_mapping is '离线同步表映射表';
comment on column data_sync_table_mapping.id is '主键ID';
comment on column data_sync_table_mapping.sync_task_id is '所属同步任务ID';
comment on column data_sync_table_mapping.source_table_id is '源表ID';
comment on column data_sync_table_mapping.source_table is '源表名称';
comment on column data_sync_table_mapping.source_data_source_id is '源数据源ID';
comment on column data_sync_table_mapping.source_data_source_name is '源数据源名称';
comment on column data_sync_table_mapping.source_schema is '源数据源模式';
comment on column data_sync_table_mapping.target_table_id is '目标表ID';
comment on column data_sync_table_mapping.target_table is '目标表名称';
comment on column data_sync_table_mapping.target_data_source_id is '目标数据源ID';
comment on column data_sync_table_mapping.target_data_source_name is '目标数据源名称';
comment on column data_sync_table_mapping.target_schema is '目标数据源模式';
comment on column data_sync_table_mapping.incremental_field is '增量字段';
comment on column data_sync_table_mapping.incremental_strategy is '增量策略';
comment on column data_sync_table_mapping.incremental_mode is '增量模式：timestamp、sequence';
comment on column data_sync_table_mapping.timestamp_field is '时间戳字段名称';
comment on column data_sync_table_mapping.operation_field is '操作类型字段名称';
comment on column data_sync_table_mapping.change_table_name is '变更表名称';
comment on column data_sync_table_mapping.filter_condition is '数据过滤条件';
comment on column data_sync_table_mapping.where_condition is 'WHERE条件';
comment on column data_sync_table_mapping.validation_enabled is '数据验证是否启用';
comment on column data_sync_table_mapping.validation_types is '验证类型，多个值用逗号分隔：rowCount,checksum,sampleData';
comment on column data_sync_table_mapping.row_count_tolerance is '行数验证容错率(%)';
comment on column data_sync_table_mapping.row_count_mode is '行数验证模式：exact,targetGreaterEqual,targetLessEqual';
comment on column data_sync_table_mapping.checksum_fields is '校验和字段，多个值用逗号分隔';
comment on column data_sync_table_mapping.checksum_algorithm is '校验和算法：md5,sha1,crc32,sum';
comment on column data_sync_table_mapping.sample_rate is '采样率(%)';
comment on column data_sync_table_mapping.sample_method is '采样方式：random,first,last,fixed';
comment on column data_sync_table_mapping.sample_fields is '样本验证字段，多个值用逗号分隔';
comment on column data_sync_table_mapping.validation_fail_action is '验证失败处理：continue,abort,retry';
comment on column data_sync_table_mapping.selected_columns is '选中的列，多个值用逗号分隔';
comment on column data_sync_table_mapping.manual_partitions is '手动指定的分区，多个值用逗号分隔';
comment on column data_sync_table_mapping.field_mappings is '字段映射配置（JSON格式）';
comment on column data_sync_table_mapping.create_time is '创建时间';
comment on column data_sync_table_mapping.update_time is '更新时间';

-- 离线同步任务索引
create index idx_offline_task_name on data_sync_task(name);
create index idx_offline_task_status on data_sync_task(status);
create index idx_offline_task_schedule on data_sync_task(schedule_type);
create index idx_offline_task_sync_mode on data_sync_task(sync_mode);

-- 同步任务日志索引
create index idx_task_log_task_id on data_sync_task_log(task_id);
create index idx_task_log_status on data_sync_task_log(status);
create index idx_task_log_time on data_sync_task_log(start_time, end_time);

-- 同步任务步骤日志索引
create index idx_step_log_task_log_id on data_sync_task_step_log(task_log_id);
create index idx_step_log_status on data_sync_task_step_log(status);
create index idx_step_log_step_type on data_sync_task_step_log(step_type);

-- 离线同步表映射索引
create index idx_offline_table_mapping_task on data_sync_table_mapping(sync_task_id);
create index idx_offline_table_mapping_tables on data_sync_table_mapping(source_table, target_table);
create index idx_offline_table_mapping_sources on data_sync_table_mapping(source_data_source_id, target_data_source_id);