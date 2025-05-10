-- 用户表
CREATE TABLE sys_user (
  id NUMBER(20) PRIMARY KEY,
  username VARCHAR2(50) NOT NULL,
  password VARCHAR2(100) NOT NULL,
  status NUMBER(1) DEFAULT 1,
  create_time TIMESTAMP DEFAULT SYSTIMESTAMP,
  update_time TIMESTAMP DEFAULT SYSTIMESTAMP,
  CONSTRAINT uk_username UNIQUE (username)
);

COMMENT ON TABLE sys_user IS '系统用户表';
COMMENT ON COLUMN sys_user.id IS '主键ID';
COMMENT ON COLUMN sys_user.username IS '用户名';
COMMENT ON COLUMN sys_user.password IS '密码';
COMMENT ON COLUMN sys_user.status IS '状态(0:禁用 1:启用)';
COMMENT ON COLUMN sys_user.create_time IS '创建时间';
COMMENT ON COLUMN sys_user.update_time IS '更新时间';

-- 创建序列
CREATE SEQUENCE seq_sys_user_id
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

-- 创建触发器
CREATE OR REPLACE TRIGGER trg_sys_user_id
BEFORE INSERT ON sys_user
FOR EACH ROW
BEGIN
  SELECT seq_sys_user_id.NEXTVAL INTO :NEW.id FROM DUAL;
END;
/

-- 插入初始管理员用户(密码: admin123)
INSERT INTO sys_user(username, password) 
VALUES('admin', '$2a$10$X6TYxjz1H3c8SJX8XVYU9.KKqwNKXg1YXXMkdBvmSJJB8Y8XZgVFi'); 