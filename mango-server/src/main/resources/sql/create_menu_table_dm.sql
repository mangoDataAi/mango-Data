-- 创建菜单表 (达梦数据库版本)
create table sys_menu (
    id varchar2(36) primary key,
    parent_id varchar2(36),
    name varchar2(100) not null,
    type varchar2(20) not null, -- DIR:目录 MENU:菜单
    path varchar2(200),
    component varchar2(255),
    icon varchar2(100),
    sort number(10) default 0,
    status number(1) default 1, -- 1:启用 0:禁用
    creator varchar2(50),
    create_time timestamp,
    updater varchar2(50),
    update_time timestamp,
    remark varchar2(500),
    is_external number(1) default 0, -- 1:是 0:否
    is_cache number(1) default 0, -- 1:是 0:否
    is_visible number(1) default 1 -- 1:是 0:否
);

-- 添加表注释
comment on table sys_menu is '系统菜单表';
comment on column sys_menu.id is '菜单ID';
comment on column sys_menu.parent_id is '父菜单ID';
comment on column sys_menu.name is '菜单名称';
comment on column sys_menu.type is '菜单类型（DIR目录，MENU菜单）';
comment on column sys_menu.path is '路由地址';
comment on column sys_menu.component is '组件路径';
comment on column sys_menu.icon is '菜单图标';
comment on column sys_menu.sort is '显示顺序';
comment on column sys_menu.status is '菜单状态（1正常 0停用）';
comment on column sys_menu.creator is '创建者';
comment on column sys_menu.create_time is '创建时间';
comment on column sys_menu.updater is '更新者';
comment on column sys_menu.update_time is '更新时间';
comment on column sys_menu.remark is '备注';
comment on column sys_menu.is_external is '是否为外链（1是 0否）';
comment on column sys_menu.is_cache is '是否缓存（1是 0否）';
comment on column sys_menu.is_visible is '是否可见（1是 0否）';

-- 创建索引
create index idx_sys_menu_parent_id on sys_menu(parent_id);
create index idx_sys_menu_sort on sys_menu(sort);

-- 插入初始菜单数据
insert into sys_menu (id, parent_id, name, type, path, component, icon, sort, status, creator, create_time, updater, update_time, remark)
values ('1', '0', '系统设置', 'DIR', '/system', null, 'Setting', 1, 1, 'admin', SYSDATE, 'admin', SYSDATE, '系统设置目录');

insert into sys_menu (id, parent_id, name, type, path, component, icon, sort, status, creator, create_time, updater, update_time, remark)
values ('2', '1', '基础设置', 'DIR', '/system/base', null, 'Tools', 1, 1, 'admin', SYSDATE, 'admin', SYSDATE, '基础设置目录');

insert into sys_menu (id, parent_id, name, type, path, component, icon, sort, status, creator, create_time, updater, update_time, remark)
values ('3', '2', '用户管理', 'MENU', '/system/base/user', 'system/base/user', 'User', 1, 1, 'admin', SYSDATE, 'admin', SYSDATE, '用户管理菜单');

insert into sys_menu (id, parent_id, name, type, path, component, icon, sort, status, creator, create_time, updater, update_time, remark)
values ('4', '2', '机构管理', 'MENU', '/system/base/organization', 'system/base/organization', 'SetUp', 2, 1, 'admin', SYSDATE, 'admin', SYSDATE, '机构管理菜单');

insert into sys_menu (id, parent_id, name, type, path, component, icon, sort, status, creator, create_time, updater, update_time, remark)
values ('5', '2', '菜单管理', 'MENU', '/system/base/menu', 'system/base/menu', 'Menu', 3, 1, 'admin', SYSDATE, 'admin', SYSDATE, '菜单管理菜单'); 