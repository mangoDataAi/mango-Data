-- 使用达梦数据库语法创建存储过程安全地添加列
CREATE OR REPLACE PROCEDURE add_column_if_not_exists(
    p_table_name VARCHAR,
    p_column_name VARCHAR,
    p_column_type VARCHAR,
    p_comment VARCHAR
) AS
    v_count NUMBER;
BEGIN
    -- 检查列是否存在
    SELECT COUNT(*) INTO v_count
    FROM USER_TAB_COLUMNS
    WHERE TABLE_NAME = UPPER(p_table_name)
    AND COLUMN_NAME = UPPER(p_column_name);
    
    -- 如果列不存在，则添加
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE ' || p_table_name || ' ADD (' || p_column_name || ' ' || p_column_type || ')';
        -- 添加注释
        EXECUTE IMMEDIATE 'COMMENT ON COLUMN ' || p_table_name || '.' || p_column_name || ' IS ''' || p_comment || '''';
    END IF;
END;
/

-- 调用存储过程添加列
BEGIN
    add_column_if_not_exists('data_asset', 'source_id', 'VARCHAR(64)', '数据源ID');
    add_column_if_not_exists('data_asset', 'domain_id', 'VARCHAR(64)', '主题域ID');
    add_column_if_not_exists('data_asset', 'asset_domain', 'VARCHAR(100)', '主题域名称');
END;
/

-- 删除存储过程
DROP PROCEDURE add_column_if_not_exists;
/ 