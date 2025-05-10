-- 角色表
CREATE TABLE sys_role (
    id VARCHAR2(36) PRIMARY KEY,
    name VARCHAR2(100) NOT NULL,
    code VARCHAR2(100) NOT NULL,
    color VARCHAR2(20),
    sort NUMBER(10) DEFAULT 0,
    status NUMBER(1) DEFAULT 1, -- 1:启用 0:禁用
    data_scope VARCHAR2(20) DEFAULT 'ALL', -- ALL全部，CUSTOM自定义，DEPT本部门，DEPT_AND_CHILD本部门及以下，SELF仅本人
    creator VARCHAR2(50),
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    updater VARCHAR2(50),
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    remark VARCHAR2(500)
);

-- 添加表注释
COMMENT ON TABLE sys_role IS '角色信息表';
COMMENT ON COLUMN sys_role.id IS '角色ID';
COMMENT ON COLUMN sys_role.name IS '角色名称';
COMMENT ON COLUMN sys_role.code IS '角色编码';
COMMENT ON COLUMN sys_role.color IS '角色颜色';
COMMENT ON COLUMN sys_role.sort IS '显示顺序';
COMMENT ON COLUMN sys_role.status IS '角色状态（1正常 0停用）';
COMMENT ON COLUMN sys_role.data_scope IS '数据权限范围';
COMMENT ON COLUMN sys_role.creator IS '创建者';
COMMENT ON COLUMN sys_role.create_time IS '创建时间';
COMMENT ON COLUMN sys_role.updater IS '更新者';
COMMENT ON COLUMN sys_role.update_time IS '更新时间';
COMMENT ON COLUMN sys_role.remark IS '备注';

-- 创建索引
CREATE INDEX idx_sys_role_code ON sys_role(code);
CREATE INDEX idx_sys_role_status ON sys_role(status);

-- 角色和菜单关联表
CREATE TABLE sys_role_menu (
    id VARCHAR2(36) PRIMARY KEY,
    role_id VARCHAR2(36) NOT NULL,
    menu_id VARCHAR2(36) NOT NULL
);

-- 添加表注释
COMMENT ON TABLE sys_role_menu IS '角色和菜单关联表';
COMMENT ON COLUMN sys_role_menu.id IS '主键ID';
COMMENT ON COLUMN sys_role_menu.role_id IS '角色ID';
COMMENT ON COLUMN sys_role_menu.menu_id IS '菜单ID';

-- 创建索引
CREATE INDEX idx_sys_role_menu_role_id ON sys_role_menu(role_id);
CREATE INDEX idx_sys_role_menu_menu_id ON sys_role_menu(menu_id);

-- 角色和部门关联表
CREATE TABLE sys_role_dept (
    id VARCHAR2(36) PRIMARY KEY,
    role_id VARCHAR2(36) NOT NULL,
    dept_id VARCHAR2(36) NOT NULL
);

-- 添加表注释
COMMENT ON TABLE sys_role_dept IS '角色和部门关联表';
COMMENT ON COLUMN sys_role_dept.id IS '主键ID';
COMMENT ON COLUMN sys_role_dept.role_id IS '角色ID';
COMMENT ON COLUMN sys_role_dept.dept_id IS '部门ID';

-- 创建索引
CREATE INDEX idx_sys_role_dept_role_id ON sys_role_dept(role_id);
CREATE INDEX idx_sys_role_dept_dept_id ON sys_role_dept(dept_id);

-- 用户和角色关联表
CREATE TABLE sys_user_role (
    id VARCHAR2(36) PRIMARY KEY,
    user_id VARCHAR2(36) NOT NULL,
    role_id VARCHAR2(36) NOT NULL
);

-- 添加表注释
COMMENT ON TABLE sys_user_role IS '用户和角色关联表';
COMMENT ON COLUMN sys_user_role.id IS '主键ID';
COMMENT ON COLUMN sys_user_role.user_id IS '用户ID';
COMMENT ON COLUMN sys_user_role.role_id IS '角色ID';

-- 创建索引
CREATE INDEX idx_sys_user_role_user_id ON sys_user_role(user_id);
CREATE INDEX idx_sys_user_role_role_id ON sys_user_role(role_id);

-- 初始角色数据
INSERT INTO sys_role (id, name, code, color, sort, status, data_scope, creator, create_time, updater, update_time, remark)
VALUES ('1', '超级管理员', 'ADMIN', '#1890FF', 1, 1, 'ALL', 'admin', SYSTIMESTAMP, 'admin', SYSTIMESTAMP, '超级管理员');

INSERT INTO sys_role (id, name, code, color, sort, status, data_scope, creator, create_time, updater, update_time, remark)
VALUES ('2', '普通用户', 'USER', '#52C41A', 2, 1, 'SELF', 'admin', SYSTIMESTAMP, 'admin', SYSTIMESTAMP, '普通用户'); 