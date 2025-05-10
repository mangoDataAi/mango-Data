-- 创建元数据采集结果相关表
-- 适用于达梦数据库(DM)

-- 表结构信息表
CREATE TABLE data_metadata_table_structure (
    id VARCHAR(64) PRIMARY KEY,
    collection_id VARCHAR(64) NOT NULL,
    execution_id VARCHAR(64) NOT NULL,
    table_name VARCHAR(200) NOT NULL,
    table_type VARCHAR(50),
    table_comment VARCHAR(500),
    create_sql CLOB,
    create_time TIMESTAMP NOT NULL
);

-- 创建索引
CREATE INDEX idx_table_structure_collection_id ON data_metadata_table_structure(collection_id);
CREATE INDEX idx_table_structure_execution_id ON data_metadata_table_structure(execution_id);

-- 字段信息表
CREATE TABLE data_metadata_field_info (
    id VARCHAR(64) PRIMARY KEY,
    collection_id VARCHAR(64) NOT NULL,
    execution_id VARCHAR(64) NOT NULL,
    table_name VARCHAR(200) NOT NULL,
    field_name VARCHAR(200) NOT NULL,
    field_type VARCHAR(100),
    field_size INTEGER,
    nullable BIT,
    default_value VARCHAR(500),
    field_comment VARCHAR(500),
    create_time TIMESTAMP NOT NULL
);

-- 创建索引
CREATE INDEX idx_field_info_collection_id ON data_metadata_field_info(collection_id);
CREATE INDEX idx_field_info_execution_id ON data_metadata_field_info(execution_id);
CREATE INDEX idx_field_info_table_name ON data_metadata_field_info(table_name);

-- 质量报告表
CREATE TABLE data_metadata_quality_report (
    id VARCHAR(64) PRIMARY KEY,
    collection_id VARCHAR(64) NOT NULL,
    execution_id VARCHAR(64) NOT NULL,
    table_name VARCHAR(200) NOT NULL,
    total_rows BIGINT,
    violation_count INTEGER,
    violation_details CLOB,
    create_time TIMESTAMP NOT NULL
);

-- 创建索引
CREATE INDEX idx_quality_report_collection_id ON data_metadata_quality_report(collection_id);
CREATE INDEX idx_quality_report_execution_id ON data_metadata_quality_report(execution_id);

-- 血缘关系表
CREATE TABLE data_metadata_lineage (
    id VARCHAR(64) PRIMARY KEY,
    collection_id VARCHAR(64) NOT NULL,
    execution_id VARCHAR(64) NOT NULL,
    table_name VARCHAR(200) NOT NULL,
    fk_name VARCHAR(200),
    fk_column_name VARCHAR(200),
    pk_table_name VARCHAR(200),
    pk_column_name VARCHAR(200),
    create_time TIMESTAMP NOT NULL
);

-- 创建索引
CREATE INDEX idx_lineage_collection_id ON data_metadata_lineage(collection_id);
CREATE INDEX idx_lineage_execution_id ON data_metadata_lineage(execution_id);
CREATE INDEX idx_lineage_table_name ON data_metadata_lineage(table_name);
CREATE INDEX idx_lineage_pk_table_name ON data_metadata_lineage(pk_table_name);

-- 添加表注释
COMMENT ON TABLE data_metadata_table_structure IS '元数据表结构信息';
COMMENT ON TABLE data_metadata_field_info IS '元数据字段信息';
COMMENT ON TABLE data_metadata_quality_report IS '元数据质量报告';
COMMENT ON TABLE data_metadata_lineage IS '元数据血缘关系'; 