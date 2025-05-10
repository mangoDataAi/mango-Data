CREATE TABLE sys_datasource (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '连接名称',
    type VARCHAR(20) NOT NULL COMMENT '数据库类型',
    url VARCHAR(200) NOT NULL COMMENT '连接URL',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    initial_size INT DEFAULT 0 COMMENT '初始连接数',
    max_active INT DEFAULT 8 COMMENT '最大连接数',
    min_idle INT DEFAULT 0 COMMENT '最小空闲数',
    max_wait INT DEFAULT 2000 COMMENT '等待超时',
    validation_query VARCHAR(100) COMMENT '验证SQL',
    test_on_borrow BOOLEAN DEFAULT FALSE COMMENT '获取连接时检测',
    test_on_return BOOLEAN DEFAULT FALSE COMMENT '归还连接时检测',
    test_while_idle BOOLEAN DEFAULT TRUE COMMENT '空闲时检测',
    status BOOLEAN DEFAULT TRUE COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT '数据源配置表'; 