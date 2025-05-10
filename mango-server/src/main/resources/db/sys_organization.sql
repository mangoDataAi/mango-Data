-- 创建机构表
create table sys_organization (
    -- 主键ID
    id varchar(32) not null primary key,
    -- 父级ID
    parent_id varchar(32) not null,
    -- 机构名称
    name varchar(50) not null,
    -- 机构编码
    code varchar(50) not null,
    -- 机构类型（1-公司，2-部门，3-小组，4-其他）
    type varchar(1) not null,
    -- 负责人
    leader varchar(50),
    -- 联系电话
    phone varchar(20),
    -- 排序号
    sort int default 0,
    -- 状态（1-正常，0-禁用）
    status varchar(1) default '1',
    -- 备注
    remark varchar(255),
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
comment on table sys_organization is '机构表';

-- 添加字段注释
comment on column sys_organization.id is '主键ID';
comment on column sys_organization.parent_id is '父级ID';
comment on column sys_organization.name is '机构名称';
comment on column sys_organization.code is '机构编码';
comment on column sys_organization.type is '机构类型（1-公司，2-部门，3-小组，4-其他）';
comment on column sys_organization.leader is '负责人';
comment on column sys_organization.phone is '联系电话';
comment on column sys_organization.sort is '排序号';
comment on column sys_organization.status is '状态（1-正常，0-禁用）';
comment on column sys_organization.remark is '备注';
comment on column sys_organization.create_time is '创建时间';
comment on column sys_organization.update_time is '更新时间';
comment on column sys_organization.creator is '创建人';
comment on column sys_organization.updater is '更新人';

-- 创建索引
create index idx_sys_org_parent_id on sys_organization(parent_id);
create index idx_sys_org_code on sys_organization(code);
create index idx_sys_org_status on sys_organization(status);

-- 创建唯一约束
create unique index uk_sys_org_code on sys_organization(code);

-- 插入初始数据
insert into sys_organization (id, parent_id, name, code, type, leader, phone, sort, status, remark, creator, updater)
values ('1', '0', '总公司', 'HQ', '1', '张三', '13800138000', 1, '1', '总部机构', 'admin', 'admin'); 