-- 创建数据标准规则表
CREATE TABLE data_standard_rule (
    id VARCHAR2(50) PRIMARY KEY,
    name VARCHAR2(100) NOT NULL,
    type VARCHAR2(50) NOT NULL,
    mode VARCHAR2(50) NOT NULL,
    rule_config CLOB,
    description VARCHAR2(2000),
    status VARCHAR2(50) DEFAULT 'enabled',
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    create_by VARCHAR2(100),
    update_by VARCHAR2(100),
    CONSTRAINT uk_rule_name UNIQUE (name)
);

-- 添加字段注释
COMMENT ON TABLE data_standard_rule IS '数据标准规则表';
COMMENT ON COLUMN data_standard_rule.id IS '主键ID';
COMMENT ON COLUMN data_standard_rule.name IS '规则名称';
COMMENT ON COLUMN data_standard_rule.type IS '规则类型(datatype/domain/format)';
COMMENT ON COLUMN data_standard_rule.mode IS '规则模式(builtin/custom)';
COMMENT ON COLUMN data_standard_rule.rule_config IS '规则配置(JSON)';
COMMENT ON COLUMN data_standard_rule.description IS '规则说明';
COMMENT ON COLUMN data_standard_rule.status IS '规则状态(enabled/disabled)';
COMMENT ON COLUMN data_standard_rule.create_time IS '创建时间';
COMMENT ON COLUMN data_standard_rule.update_time IS '更新时间';
COMMENT ON COLUMN data_standard_rule.create_by IS '创建人';
COMMENT ON COLUMN data_standard_rule.update_by IS '更新人';

-- 创建索引
CREATE INDEX idx_name ON data_standard_rule(name);
CREATE INDEX idx_type ON data_standard_rule(type);
CREATE INDEX idx_status ON data_standard_rule(status);
CREATE INDEX idx_create_time ON data_standard_rule(create_time);

-- 创建触发器用于自动更新update_time
CREATE OR REPLACE TRIGGER trg_data_standard_rule_upd
BEFORE UPDATE ON data_standard_rule
FOR EACH ROW
BEGIN
    :NEW.update_time := SYSTIMESTAMP;
END; 