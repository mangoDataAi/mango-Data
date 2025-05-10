-- 创建元数据血缘关系表
CREATE TABLE data_metadata_lineage (
    id VARCHAR(32) NOT NULL,
    collection_id VARCHAR(32) NOT NULL,
    execution_id VARCHAR(32) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_id VARCHAR(255) NOT NULL,
    source_name VARCHAR(255) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id VARCHAR(255) NOT NULL,
    target_name VARCHAR(255) NOT NULL,
    lineage_type VARCHAR(50) NOT NULL,
    transformation_rule CLOB,
    confidence INTEGER,
    version INTEGER NOT NULL,
    is_latest INTEGER DEFAULT 1 NOT NULL,
    is_deleted INTEGER DEFAULT 0 NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    lineage_path CLOB,
    additional_info CLOB,
    CONSTRAINT pk_metadata_lineage PRIMARY KEY (id)
);

-- 创建索引
CREATE INDEX idx_lineage_collection ON data_metadata_lineage (collection_id);
CREATE INDEX idx_lineage_execution ON data_metadata_lineage (execution_id);
CREATE INDEX idx_lineage_source ON data_metadata_lineage (source_type, source_id);
CREATE INDEX idx_lineage_target ON data_metadata_lineage (target_type, target_id);
CREATE INDEX idx_lineage_latest ON data_metadata_lineage (is_latest, is_deleted);

-- 添加注释
COMMENT ON TABLE data_metadata_lineage IS '元数据血缘关系表';
COMMENT ON COLUMN data_metadata_lineage.id IS '主键ID';
COMMENT ON COLUMN data_metadata_lineage.collection_id IS '采集任务ID';
COMMENT ON COLUMN data_metadata_lineage.execution_id IS '执行ID';
COMMENT ON COLUMN data_metadata_lineage.source_type IS '源元数据类型（database/table/column）';
COMMENT ON COLUMN data_metadata_lineage.source_id IS '源元数据ID';
COMMENT ON COLUMN data_metadata_lineage.source_name IS '源元数据名称';
COMMENT ON COLUMN data_metadata_lineage.target_type IS '目标元数据类型（database/table/column）';
COMMENT ON COLUMN data_metadata_lineage.target_id IS '目标元数据ID';
COMMENT ON COLUMN data_metadata_lineage.target_name IS '目标元数据名称';
COMMENT ON COLUMN data_metadata_lineage.lineage_type IS '血缘关系类型（copy/transform/aggregate/derive等）';
COMMENT ON COLUMN data_metadata_lineage.transformation_rule IS '转换规则或SQL语句';
COMMENT ON COLUMN data_metadata_lineage.confidence IS '血缘关系置信度（0-100）';
COMMENT ON COLUMN data_metadata_lineage.version IS '版本号';
COMMENT ON COLUMN data_metadata_lineage.is_latest IS '是否最新版本（1是，0否）';
COMMENT ON COLUMN data_metadata_lineage.is_deleted IS '是否删除（0-未删除，1-已删除）';
COMMENT ON COLUMN data_metadata_lineage.create_time IS '创建时间';
COMMENT ON COLUMN data_metadata_lineage.update_time IS '更新时间';
COMMENT ON COLUMN data_metadata_lineage.lineage_path IS '血缘路径（JSON格式，记录完整血缘路径）';
COMMENT ON COLUMN data_metadata_lineage.additional_info IS '额外信息（JSON格式）';

-- 创建元数据血缘详情表
CREATE TABLE data_metadata_lineage_detail (
    id VARCHAR(32) NOT NULL,
    lineage_id VARCHAR(32) NOT NULL,
    execution_id VARCHAR(32) NOT NULL,
    expression CLOB,
    description VARCHAR(500),
    etl_job_id VARCHAR(32),
    etl_job_name VARCHAR(255),
    step_order INTEGER,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted INTEGER DEFAULT 0 NOT NULL,
    CONSTRAINT pk_metadata_lineage_detail PRIMARY KEY (id)
);

-- 创建索引
CREATE INDEX idx_lineage_detail_lid ON data_metadata_lineage_detail (lineage_id);
CREATE INDEX idx_lineage_detail_exec ON data_metadata_lineage_detail (execution_id);
CREATE INDEX idx_lineage_detail_etl ON data_metadata_lineage_detail (etl_job_id);

-- 添加注释
COMMENT ON TABLE data_metadata_lineage_detail IS '元数据血缘详情表';
COMMENT ON COLUMN data_metadata_lineage_detail.id IS '主键ID';
COMMENT ON COLUMN data_metadata_lineage_detail.lineage_id IS '血缘关系ID';
COMMENT ON COLUMN data_metadata_lineage_detail.execution_id IS '执行ID';
COMMENT ON COLUMN data_metadata_lineage_detail.expression IS '转换表达式或SQL片段';
COMMENT ON COLUMN data_metadata_lineage_detail.description IS '转换描述';
COMMENT ON COLUMN data_metadata_lineage_detail.etl_job_id IS 'ETL作业ID';
COMMENT ON COLUMN data_metadata_lineage_detail.etl_job_name IS 'ETL作业名称';
COMMENT ON COLUMN data_metadata_lineage_detail.step_order IS '转换步骤序号';
COMMENT ON COLUMN data_metadata_lineage_detail.create_time IS '创建时间';
COMMENT ON COLUMN data_metadata_lineage_detail.update_time IS '更新时间';
COMMENT ON COLUMN data_metadata_lineage_detail.is_deleted IS '是否删除（0-未删除，1-已删除）';

-- 添加外键约束
ALTER TABLE data_metadata_lineage_detail ADD CONSTRAINT fk_lineage_detail_lid 
    FOREIGN KEY (lineage_id) REFERENCES data_metadata_lineage(id); 