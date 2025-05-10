-- 创建数据标准包表
CREATE TABLE data_standard_package (
    id VARCHAR(32) NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    type VARCHAR(50),           -- base, business, technical
    scope VARCHAR(50),          -- global, domain, project
    version VARCHAR(20),
    description VARCHAR(500),
    status VARCHAR(20) DEFAULT '1',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(50),
    update_by VARCHAR(50),
    CONSTRAINT pk_data_standard_package PRIMARY KEY (id)
);

COMMENT ON TABLE data_standard_package IS '数据标准包';
COMMENT ON COLUMN data_standard_package.id IS '主键ID';
COMMENT ON COLUMN data_standard_package.name IS '标准包名称';
COMMENT ON COLUMN data_standard_package.code IS '标准包编码';
COMMENT ON COLUMN data_standard_package.type IS '标准包类型(base:基础,business:业务,technical:技术)';
COMMENT ON COLUMN data_standard_package.scope IS '使用范围(global:全局,domain:领域,project:项目)';
COMMENT ON COLUMN data_standard_package.version IS '版本号';
COMMENT ON COLUMN data_standard_package.description IS '描述';
COMMENT ON COLUMN data_standard_package.status IS '状态';
COMMENT ON COLUMN data_standard_package.create_time IS '创建时间';
COMMENT ON COLUMN data_standard_package.update_time IS '更新时间';
COMMENT ON COLUMN data_standard_package.create_by IS '创建人';
COMMENT ON COLUMN data_standard_package.update_by IS '更新人';

-- 创建数据标准对象表
CREATE TABLE data_standard_object (
    id VARCHAR(32) NOT NULL,
    package_id VARCHAR(32) NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    type VARCHAR(50),
    description VARCHAR(500),
    status VARCHAR(20) DEFAULT '1',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(50),
    update_by VARCHAR(50),
    CONSTRAINT pk_data_standard_object PRIMARY KEY (id),
    CONSTRAINT fk_object_package FOREIGN KEY (package_id) REFERENCES data_standard_package(id)
);

COMMENT ON TABLE data_standard_object IS '数据标准对象';
COMMENT ON COLUMN data_standard_object.id IS '主键ID';
COMMENT ON COLUMN data_standard_object.package_id IS '标准包ID';
COMMENT ON COLUMN data_standard_object.name IS '对象名称';
COMMENT ON COLUMN data_standard_object.code IS '对象编码';
COMMENT ON COLUMN data_standard_object.type IS '对象类型';
COMMENT ON COLUMN data_standard_object.description IS '描述';
COMMENT ON COLUMN data_standard_object.status IS '状态';
COMMENT ON COLUMN data_standard_object.create_time IS '创建时间';
COMMENT ON COLUMN data_standard_object.update_time IS '更新时间';
COMMENT ON COLUMN data_standard_object.create_by IS '创建人';
COMMENT ON COLUMN data_standard_object.update_by IS '更新人';

-- 创建数据标准规则表
CREATE TABLE data_standard_rule (
    id VARCHAR(32) NOT NULL,
    object_id VARCHAR(32) NOT NULL,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50),
    mode VARCHAR(50),
    rule_config TEXT,
    description VARCHAR(500),
    status VARCHAR(20) DEFAULT '1',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(50),
    update_by VARCHAR(50),
    CONSTRAINT pk_data_standard_rule PRIMARY KEY (id),
    CONSTRAINT fk_rule_object FOREIGN KEY (object_id) REFERENCES data_standard_object(id)
);

COMMENT ON TABLE data_standard_rule IS '数据标准规则';
COMMENT ON COLUMN data_standard_rule.id IS '主键ID';
COMMENT ON COLUMN data_standard_rule.object_id IS '对象ID';
COMMENT ON COLUMN data_standard_rule.name IS '规则名称';
COMMENT ON COLUMN data_standard_rule.type IS '规则类型';
COMMENT ON COLUMN data_standard_rule.mode IS '规则模式';
COMMENT ON COLUMN data_standard_rule.rule_config IS '规则配置';
COMMENT ON COLUMN data_standard_rule.description IS '描述';
COMMENT ON COLUMN data_standard_rule.status IS '状态';
COMMENT ON COLUMN data_standard_rule.create_time IS '创建时间';
COMMENT ON COLUMN data_standard_rule.update_time IS '更新时间';
COMMENT ON COLUMN data_standard_rule.create_by IS '创建人';
COMMENT ON COLUMN data_standard_rule.update_by IS '更新人';

-- 创建索引
CREATE INDEX idx_package_code ON data_standard_package(code);
CREATE INDEX idx_object_package_id ON data_standard_object(package_id);
CREATE INDEX idx_object_code ON data_standard_object(code);
CREATE INDEX idx_rule_object_id ON data_standard_rule(object_id); 