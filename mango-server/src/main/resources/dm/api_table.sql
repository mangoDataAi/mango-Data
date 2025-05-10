-- 创建API表
CREATE TABLE t_api (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT 'API名称',
    path VARCHAR(255) NOT NULL COMMENT 'API路径',
    method VARCHAR(20) NOT NULL COMMENT '请求方法',
    status VARCHAR(20) NOT NULL COMMENT 'API状态：developing-开发中，testing-测试中，published-已发布，offline-已下线，deprecated-已弃用',
    owner VARCHAR(50) COMMENT '负责人',
    description TEXT COMMENT 'API描述',
    create_time TIMESTAMP NOT NULL COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL COMMENT '更新时间',
    request_example TEXT COMMENT '请求示例',
    response_example TEXT COMMENT '响应示例',
    type VARCHAR(20) COMMENT 'API类型：rest, graphql, soap, websocket',
    requires_auth NUMBER(1) DEFAULT 1 COMMENT '是否需要认证',
    parameters TEXT COMMENT '请求参数，JSON格式',
    headers TEXT COMMENT '请求头，JSON格式',
    response_schemas TEXT COMMENT '响应模式，JSON格式',
    is_deleted NUMBER(1) DEFAULT 0 COMMENT '是否已删除'
);

-- 添加索引
CREATE INDEX idx_api_status ON t_api (status);
CREATE INDEX idx_api_create_time ON t_api (create_time);
CREATE INDEX idx_api_owner ON t_api (owner);
CREATE INDEX idx_api_is_deleted ON t_api (is_deleted);

-- 添加注释
COMMENT ON TABLE t_api IS 'API信息表';
COMMENT ON COLUMN t_api.id IS 'API ID';
COMMENT ON COLUMN t_api.name IS 'API名称';
COMMENT ON COLUMN t_api.path IS 'API路径';
COMMENT ON COLUMN t_api.method IS '请求方法';
COMMENT ON COLUMN t_api.status IS 'API状态：developing-开发中，testing-测试中，published-已发布，offline-已下线，deprecated-已弃用';
COMMENT ON COLUMN t_api.owner IS '负责人';
COMMENT ON COLUMN t_api.description IS 'API描述';
COMMENT ON COLUMN t_api.create_time IS '创建时间';
COMMENT ON COLUMN t_api.update_time IS '更新时间';
COMMENT ON COLUMN t_api.request_example IS '请求示例';
COMMENT ON COLUMN t_api.response_example IS '响应示例';
COMMENT ON COLUMN t_api.type IS 'API类型：rest, graphql, soap, websocket';
COMMENT ON COLUMN t_api.requires_auth IS '是否需要认证';
COMMENT ON COLUMN t_api.parameters IS '请求参数，JSON格式';
COMMENT ON COLUMN t_api.headers IS '请求头，JSON格式';
COMMENT ON COLUMN t_api.response_schemas IS '响应模式，JSON格式';
COMMENT ON COLUMN t_api.is_deleted IS '是否已删除'; 