-- 数据标准包表
CREATE TABLE DATA_STANDARD_PACKAGE (
    ID VARCHAR(32) NOT NULL,
    NAME VARCHAR(100) NOT NULL,
    CODE VARCHAR(100) NOT NULL,
    DESCRIPTION VARCHAR(500),
    CREATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UPDATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CREATE_USER VARCHAR(32),
    UPDATE_USER VARCHAR(32),
    STATUS INTEGER DEFAULT 1,
    PRIMARY KEY (ID)
);

-- 数据标准对象表（表）
CREATE TABLE DATA_STANDARD_OBJECT (
    ID VARCHAR(32) NOT NULL,
    NAME VARCHAR(100) NOT NULL,
    CODE VARCHAR(100) NOT NULL,
    TYPE VARCHAR(50) NOT NULL,
    DESCRIPTION VARCHAR(500),
    PACKAGE_ID VARCHAR(32) NOT NULL,
    CREATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UPDATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CREATE_USER VARCHAR(32),
    UPDATE_USER VARCHAR(32),
    STATUS INTEGER DEFAULT 1,
    PRIMARY KEY (ID),
    FOREIGN KEY (PACKAGE_ID) REFERENCES DATA_STANDARD_PACKAGE(ID)
);

-- 数据标准规则表
CREATE TABLE DATA_STANDARD_RULE (
    ID VARCHAR(32) NOT NULL,
    NAME VARCHAR(100) NOT NULL,
    CODE VARCHAR(100) NOT NULL,
    TYPE VARCHAR(50) NOT NULL,
    SUB_TYPE VARCHAR(50),
    DESCRIPTION VARCHAR(500),
    CONFIG CLOB,  -- 使用CLOB类型存储大文本数据，适合JSON
    PACKAGE_ID VARCHAR(32) NOT NULL,
    OBJECT_ID VARCHAR(32) NOT NULL,
    CATEGORY_NAME VARCHAR(100),
    SUB_CATEGORY_NAME VARCHAR(100),
    CREATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UPDATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CREATE_USER VARCHAR(32),
    UPDATE_USER VARCHAR(32),
    STATUS INTEGER DEFAULT 1,
    PRIMARY KEY (ID),
    FOREIGN KEY (PACKAGE_ID) REFERENCES DATA_STANDARD_PACKAGE(ID),
    FOREIGN KEY (OBJECT_ID) REFERENCES DATA_STANDARD_OBJECT(ID)
);

-- 创建索引
CREATE INDEX IDX_RULE_PACKAGE_ID ON DATA_STANDARD_RULE(PACKAGE_ID);
CREATE INDEX IDX_RULE_OBJECT_ID ON DATA_STANDARD_RULE(OBJECT_ID);
CREATE INDEX IDX_RULE_TYPE ON DATA_STANDARD_RULE(TYPE);
CREATE INDEX IDX_RULE_SUB_TYPE ON DATA_STANDARD_RULE(SUB_TYPE);
CREATE INDEX IDX_OBJECT_PACKAGE_ID ON DATA_STANDARD_OBJECT(PACKAGE_ID);

-- 注释
COMMENT ON TABLE DATA_STANDARD_PACKAGE IS '数据标准包表';
COMMENT ON COLUMN DATA_STANDARD_PACKAGE.ID IS '主键ID';
COMMENT ON COLUMN DATA_STANDARD_PACKAGE.NAME IS '包名称';
COMMENT ON COLUMN DATA_STANDARD_PACKAGE.CODE IS '包编码';
COMMENT ON COLUMN DATA_STANDARD_PACKAGE.DESCRIPTION IS '描述';
COMMENT ON COLUMN DATA_STANDARD_PACKAGE.STATUS IS '状态：1-启用，0-禁用';

COMMENT ON TABLE DATA_STANDARD_OBJECT IS '数据标准对象表';
COMMENT ON COLUMN DATA_STANDARD_OBJECT.ID IS '主键ID';
COMMENT ON COLUMN DATA_STANDARD_OBJECT.NAME IS '对象名称';
COMMENT ON COLUMN DATA_STANDARD_OBJECT.CODE IS '对象编码';
COMMENT ON COLUMN DATA_STANDARD_OBJECT.TYPE IS '对象类型：MASTER-主题表，DIMENSION-维度表，FACT-事实表，TEMP-临时表，CONFIG-配置表，VIEW-视图';
COMMENT ON COLUMN DATA_STANDARD_OBJECT.DESCRIPTION IS '描述';
COMMENT ON COLUMN DATA_STANDARD_OBJECT.PACKAGE_ID IS '所属包ID';
COMMENT ON COLUMN DATA_STANDARD_OBJECT.STATUS IS '状态：1-启用，0-禁用';

COMMENT ON TABLE DATA_STANDARD_RULE IS '数据标准规则表';
COMMENT ON COLUMN DATA_STANDARD_RULE.ID IS '主键ID';
COMMENT ON COLUMN DATA_STANDARD_RULE.NAME IS '规则名称';
COMMENT ON COLUMN DATA_STANDARD_RULE.CODE IS '规则编码';
COMMENT ON COLUMN DATA_STANDARD_RULE.TYPE IS '规则类型：NAMING-命名规范，STRUCTURE-结构规范，DATA-数据规范，QUALITY-质量规范，DOMAIN-主题域规范等';
COMMENT ON COLUMN DATA_STANDARD_RULE.SUB_TYPE IS '规则子类型';
COMMENT ON COLUMN DATA_STANDARD_RULE.DESCRIPTION IS '描述';
COMMENT ON COLUMN DATA_STANDARD_RULE.CONFIG IS '规则配置（JSON格式）';
COMMENT ON COLUMN DATA_STANDARD_RULE.PACKAGE_ID IS '所属包ID';
COMMENT ON COLUMN DATA_STANDARD_RULE.OBJECT_ID IS '所属对象ID';
COMMENT ON COLUMN DATA_STANDARD_RULE.CATEGORY_NAME IS '规则分类名称';
COMMENT ON COLUMN DATA_STANDARD_RULE.SUB_CATEGORY_NAME IS '规则子分类名称';
COMMENT ON COLUMN DATA_STANDARD_RULE.STATUS IS '状态：1-启用，0-禁用'; 