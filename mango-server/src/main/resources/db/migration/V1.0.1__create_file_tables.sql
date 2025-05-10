-- 文件数据源表
CREATE TABLE file_data_source (
    id VARCHAR2(255) PRIMARY KEY,
    file_name VARCHAR2(255) NOT NULL,
    file_type VARCHAR2(50) NOT NULL,
    file_size NUMBER(20),
    file_path VARCHAR2(500),
    table_name VARCHAR2(100),
    field_count INTEGER COMMENT '字段数量',
    data_count NUMBER(10),
    status NUMBER(1) DEFAULT 0,
    error_msg VARCHAR2(500),
    create_time DATE DEFAULT SYSDATE,
    "CREATE_BY" VARCHAR2(50),
    update_time DATE DEFAULT SYSDATE,
    "UPDATE_BY" VARCHAR2(50),
    "DELETE_FLG" NUMBER(1,0) DEFAULT 0,
    "REMARK" VARCHAR2(500)
);

-- 添加注释
COMMENT ON TABLE file_data_source IS '文件数据源表';
COMMENT ON COLUMN file_data_source.id IS '主键ID';
COMMENT ON COLUMN file_data_source.file_name IS '文件名';
COMMENT ON COLUMN file_data_source.file_type IS '文件类型';
COMMENT ON COLUMN file_data_source.file_size IS '文件大小';
COMMENT ON COLUMN file_data_source.file_path IS '文件路径';
COMMENT ON COLUMN file_data_source.table_name IS '数据表名';
COMMENT ON COLUMN file_data_source.field_count IS '字段数量';
COMMENT ON COLUMN file_data_source.data_count IS '数据量';
COMMENT ON COLUMN file_data_source.status IS '状态(0:未解析,1:已解析,2:解析失败)';
COMMENT ON COLUMN file_data_source.error_msg IS '错误信息';
COMMENT ON COLUMN file_data_source.create_time IS '创建时间';
COMMENT ON COLUMN file_data_source.update_time IS '更新时间';

-- 创建序列
CREATE SEQUENCE seq_file_data_source
    INCREMENT BY 1
    START WITH 1
    NOMAXVALUE
    NOCYCLE
    CACHE 20;

-- 创建触发器
CREATE OR REPLACE TRIGGER trg_file_data_source
BEFORE INSERT ON file_data_source
FOR EACH ROW
BEGIN
    SELECT seq_file_data_source.nextval INTO :new.id FROM dual;
    :new.create_time := SYSDATE;
    :new.update_time := SYSDATE;
END;
/

CREATE OR REPLACE TRIGGER trg_file_data_source_upd
BEFORE UPDATE ON file_data_source
FOR EACH ROW
BEGIN
    :new.update_time := SYSDATE;
END;
/

-- 文件字段表
CREATE TABLE file_field (
    id VARCHAR2(255) PRIMARY KEY,
    source_id NUMBER(20) NOT NULL,
    field_name VARCHAR2(100) NOT NULL,
    field_type VARCHAR2(50) NOT NULL,
    field_length NUMBER(10),
    nullable NUMBER(1) DEFAULT 1,
    description VARCHAR2(500),
    order_num NUMBER(10) NOT NULL,
    CONSTRAINT fk_file_field_source FOREIGN KEY (source_id) REFERENCES file_data_source(id)
);

-- 添加注释
COMMENT ON TABLE file_field IS '文件字段信息表';
COMMENT ON COLUMN file_field.id IS '主键ID';
COMMENT ON COLUMN file_field.source_id IS '数据源ID';
COMMENT ON COLUMN file_field.field_name IS '字段名';
COMMENT ON COLUMN file_field.field_type IS '字段类型';
COMMENT ON COLUMN file_field.field_length IS '字段长度';
COMMENT ON COLUMN file_field.nullable IS '是否可为空(1:是,0:否)';
COMMENT ON COLUMN file_field.description IS '字段描述';
COMMENT ON COLUMN file_field.order_num IS '排序号';

-- 创建序列
CREATE SEQUENCE seq_file_field
    INCREMENT BY 1
    START WITH 1
    NOMAXVALUE
    NOCYCLE
    CACHE 20;

-- 创建触发器
CREATE OR REPLACE TRIGGER trg_file_field
BEFORE INSERT ON file_field
FOR EACH ROW
BEGIN
    SELECT seq_file_field.nextval INTO :new.id FROM dual;
END;
/
