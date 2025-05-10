-- 创建维度表
CREATE TABLE t_dimension (
    id VARCHAR(32) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    source_type VARCHAR(50),
    domain_id VARCHAR(32),
    description VARCHAR(500),
    attribute_count INTEGER DEFAULT 0,
    update_strategy VARCHAR(20),
    last_update_time TIMESTAMP(0),
    create_time TIMESTAMP(0) NOT NULL,
    create_by VARCHAR(50),
    update_time TIMESTAMP(0),
    update_by VARCHAR(50)
);

-- 创建维度属性表
CREATE TABLE t_dimension_attribute (
    id VARCHAR(32) PRIMARY KEY,
    dimension_id VARCHAR(32) NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(100) NOT NULL,
    data_type VARCHAR(50) NOT NULL,
    length INTEGER,
    is_primary NUMBER(1) DEFAULT 0,
    description VARCHAR(500),
    create_time TIMESTAMP(0) NOT NULL,
    create_by VARCHAR(50),
    update_time TIMESTAMP(0),
    update_by VARCHAR(50),
    CONSTRAINT fk_dim_attr FOREIGN KEY (dimension_id) REFERENCES t_dimension(id)
);

-- 创建维度标准关联表
CREATE TABLE t_dimension_standard (
    id VARCHAR(32) PRIMARY KEY,
    dimension_id VARCHAR(32) NOT NULL,
    standard_id VARCHAR(32) NOT NULL,
    standard_type VARCHAR(50) NOT NULL,
    priority VARCHAR(20),
    description VARCHAR(500),
    create_time TIMESTAMP(0) NOT NULL,
    create_by VARCHAR(50),
    update_time TIMESTAMP(0),
    update_by VARCHAR(50),
    CONSTRAINT fk_dim_std FOREIGN KEY (dimension_id) REFERENCES t_dimension(id)
);

-- 创建索引
CREATE INDEX idx_dimension_domain ON t_dimension(domain_id);
CREATE INDEX idx_dimension_type ON t_dimension(type);
CREATE INDEX idx_dim_attr_dim ON t_dimension_attribute(dimension_id);
CREATE INDEX idx_dim_std_dim ON t_dimension_standard(dimension_id);
CREATE INDEX idx_dim_std_std ON t_dimension_standard(standard_id); 