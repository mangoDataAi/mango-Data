-- 创建规则模板表
CREATE TABLE rule_template (
    id VARCHAR2(32) NOT NULL PRIMARY KEY,
    name VARCHAR2(100) NOT NULL,
    rule_type VARCHAR2(20) NOT NULL,
    source_type VARCHAR2(20),
    scenario VARCHAR2(20),
    description VARCHAR2(500),
    condition_template_type VARCHAR2(50),
    sql_template_type VARCHAR2(50),
    config CLOB,
    source_info CLOB,
    field_mappings CLOB,
    sql_field_mappings CLOB,
    use_count NUMBER(10) DEFAULT 0,
    status VARCHAR2(20) DEFAULT 'enabled',
    is_system NUMBER(1) DEFAULT 0,
    create_by VARCHAR2(32),
    create_time TIMESTAMP,
    update_by VARCHAR2(32),
    update_time TIMESTAMP
);

-- 添加索引
CREATE INDEX idx_rule_template_rule_type ON rule_template (rule_type);
CREATE INDEX idx_rule_template_status ON rule_template (status);
CREATE INDEX idx_rule_template_is_system ON rule_template (is_system);
CREATE INDEX idx_rule_template_create_time ON rule_template (create_time);

-- 添加注释
COMMENT ON TABLE rule_template IS '规则模板表';
COMMENT ON COLUMN rule_template.id IS '主键ID';
COMMENT ON COLUMN rule_template.name IS '模板名称';
COMMENT ON COLUMN rule_template.rule_type IS '规则类型';
COMMENT ON COLUMN rule_template.source_type IS '数据源类型';
COMMENT ON COLUMN rule_template.scenario IS '适用场景';
COMMENT ON COLUMN rule_template.description IS '模板描述';
COMMENT ON COLUMN rule_template.condition_template_type IS '条件模板类型';
COMMENT ON COLUMN rule_template.sql_template_type IS 'SQL模板类型';
COMMENT ON COLUMN rule_template.config IS '配置信息(JSON格式)';
COMMENT ON COLUMN rule_template.source_info IS '数据源信息(JSON格式)';
COMMENT ON COLUMN rule_template.field_mappings IS '字段映射关系(JSON格式)';
COMMENT ON COLUMN rule_template.sql_field_mappings IS 'SQL字段映射关系(JSON格式)';
COMMENT ON COLUMN rule_template.use_count IS '使用次数';
COMMENT ON COLUMN rule_template.status IS '状态';
COMMENT ON COLUMN rule_template.is_system IS '是否系统内置';
COMMENT ON COLUMN rule_template.create_by IS '创建人ID';
COMMENT ON COLUMN rule_template.create_time IS '创建时间';
COMMENT ON COLUMN rule_template.update_by IS '更新人ID';
COMMENT ON COLUMN rule_template.update_time IS '更新时间'; 