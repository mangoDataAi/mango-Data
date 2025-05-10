-- 创建元数据版本记录表
CREATE TABLE metadata_version_record (
    id VARCHAR(32) NOT NULL,
    version_number VARCHAR(50) NOT NULL,
    version_name VARCHAR(100) NOT NULL,
    version_description VARCHAR(500),
    metadata_id VARCHAR(32) NOT NULL,
    metadata_type VARCHAR(50) NOT NULL,
    metadata_name VARCHAR(100) NOT NULL,
    metadata_path VARCHAR(500),
    publisher_id VARCHAR(32) NOT NULL,
    publisher_name VARCHAR(100) NOT NULL,
    publish_time TIMESTAMP NOT NULL,
    change_summary VARCHAR(500),
    change_details TEXT,
    metadata_snapshot TEXT,
    previous_version_id VARCHAR(32),
    is_current NUMBER(1) DEFAULT 1 NOT NULL,
    approval_status VARCHAR(20) DEFAULT 'pending' NOT NULL,
    approval_comment VARCHAR(500),
    approval_time TIMESTAMP,
    tags VARCHAR(200),
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    is_deleted NUMBER(1) DEFAULT 0 NOT NULL,
    PRIMARY KEY (id)
);

-- 创建索引
CREATE INDEX idx_mvr_metadata_id ON metadata_version_record (metadata_id);
CREATE INDEX idx_mvr_metadata_type ON metadata_version_record (metadata_type);
CREATE INDEX idx_mvr_is_current ON metadata_version_record (is_current);
CREATE INDEX idx_mvr_publish_time ON metadata_version_record (publish_time);
CREATE INDEX idx_mvr_approval_status ON metadata_version_record (approval_status);

-- 添加注释
COMMENT ON TABLE metadata_version_record IS '元数据版本记录表';
COMMENT ON COLUMN metadata_version_record.id IS '主键ID';
COMMENT ON COLUMN metadata_version_record.version_number IS '版本号';
COMMENT ON COLUMN metadata_version_record.version_name IS '版本名称';
COMMENT ON COLUMN metadata_version_record.version_description IS '版本描述';
COMMENT ON COLUMN metadata_version_record.metadata_id IS '元数据ID';
COMMENT ON COLUMN metadata_version_record.metadata_type IS '元数据类型';
COMMENT ON COLUMN metadata_version_record.metadata_name IS '元数据名称';
COMMENT ON COLUMN metadata_version_record.metadata_path IS '元数据路径';
COMMENT ON COLUMN metadata_version_record.publisher_id IS '发布者ID';
COMMENT ON COLUMN metadata_version_record.publisher_name IS '发布者名称';
COMMENT ON COLUMN metadata_version_record.publish_time IS '发布时间';
COMMENT ON COLUMN metadata_version_record.change_summary IS '变更摘要';
COMMENT ON COLUMN metadata_version_record.change_details IS '变更详情';
COMMENT ON COLUMN metadata_version_record.metadata_snapshot IS '元数据快照';
COMMENT ON COLUMN metadata_version_record.previous_version_id IS '上一个版本ID';
COMMENT ON COLUMN metadata_version_record.is_current IS '是否当前版本';
COMMENT ON COLUMN metadata_version_record.approval_status IS '审批状态';
COMMENT ON COLUMN metadata_version_record.approval_comment IS '审批意见';
COMMENT ON COLUMN metadata_version_record.approval_time IS '审批时间';
COMMENT ON COLUMN metadata_version_record.tags IS '标签';
COMMENT ON COLUMN metadata_version_record.create_time IS '创建时间';
COMMENT ON COLUMN metadata_version_record.update_time IS '更新时间';
COMMENT ON COLUMN metadata_version_record.is_deleted IS '是否删除';

-- 创建元数据版本变更日志表
CREATE TABLE metadata_version_change_log (
    id VARCHAR(32) NOT NULL,
    version_id VARCHAR(32) NOT NULL,
    change_type VARCHAR(50) NOT NULL,
    object_type VARCHAR(50) NOT NULL,
    object_id VARCHAR(32) NOT NULL,
    object_name VARCHAR(100) NOT NULL,
    object_path VARCHAR(500),
    before_value TEXT,
    after_value TEXT,
    changed_fields TEXT,
    change_description VARCHAR(500),
    change_time TIMESTAMP NOT NULL,
    changer_id VARCHAR(32) NOT NULL,
    changer_name VARCHAR(100) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    is_deleted NUMBER(1) DEFAULT 0 NOT NULL,
    PRIMARY KEY (id)
);

-- 创建索引
CREATE INDEX idx_mvcl_version_id ON metadata_version_change_log (version_id);
CREATE INDEX idx_mvcl_object_id ON metadata_version_change_log (object_id);
CREATE INDEX idx_mvcl_object_type ON metadata_version_change_log (object_type);
CREATE INDEX idx_mvcl_change_time ON metadata_version_change_log (change_time);

-- 添加注释
COMMENT ON TABLE metadata_version_change_log IS '元数据版本变更日志表';
COMMENT ON COLUMN metadata_version_change_log.id IS '主键ID';
COMMENT ON COLUMN metadata_version_change_log.version_id IS '版本ID';
COMMENT ON COLUMN metadata_version_change_log.change_type IS '变更类型';
COMMENT ON COLUMN metadata_version_change_log.object_type IS '对象类型';
COMMENT ON COLUMN metadata_version_change_log.object_id IS '对象ID';
COMMENT ON COLUMN metadata_version_change_log.object_name IS '对象名称';
COMMENT ON COLUMN metadata_version_change_log.object_path IS '对象路径';
COMMENT ON COLUMN metadata_version_change_log.before_value IS '变更前值';
COMMENT ON COLUMN metadata_version_change_log.after_value IS '变更后值';
COMMENT ON COLUMN metadata_version_change_log.changed_fields IS '变更字段';
COMMENT ON COLUMN metadata_version_change_log.change_description IS '变更描述';
COMMENT ON COLUMN metadata_version_change_log.change_time IS '变更时间';
COMMENT ON COLUMN metadata_version_change_log.changer_id IS '变更者ID';
COMMENT ON COLUMN metadata_version_change_log.changer_name IS '变更者名称';
COMMENT ON COLUMN metadata_version_change_log.create_time IS '创建时间';
COMMENT ON COLUMN metadata_version_change_log.is_deleted IS '是否删除'; 