-- 创建存储过程：发布新版本
CREATE OR REPLACE PROCEDURE publish_version(
    p_metadata_id VARCHAR(32),
    p_metadata_type VARCHAR(50),
    p_metadata_name VARCHAR(100),
    p_metadata_path VARCHAR(500),
    p_version_name VARCHAR(100),
    p_version_description VARCHAR(500),
    p_publisher_id VARCHAR(32),
    p_publisher_name VARCHAR(100),
    p_change_summary VARCHAR(500),
    p_tags VARCHAR(200),
    p_version_id OUT VARCHAR(32)
)
AS
    v_previous_version_id VARCHAR(32);
    v_version_number VARCHAR(20);
    v_current_time TIMESTAMP;
    v_version_id VARCHAR(32);
BEGIN
    -- 获取当前时间
    v_current_time := SYSTIMESTAMP;
    
    -- 生成版本ID
    SELECT REPLACE(SYS_GUID(), '-', '') INTO v_version_id FROM DUAL;
    p_version_id := v_version_id;
    
    -- 获取上一个版本ID
    SELECT id INTO v_previous_version_id FROM metadata_version_record
    WHERE metadata_id = p_metadata_id
      AND metadata_type = p_metadata_type
      AND is_current = 1
      AND is_deleted = 0
    ORDER BY publish_time DESC
    FETCH FIRST 1 ROWS ONLY;
    
    -- 生成版本号
    IF v_previous_version_id IS NULL THEN
        v_version_number := 'v1.0.0';
    ELSE
        DECLARE
            v_current_version VARCHAR(20);
            v_major INTEGER;
            v_minor INTEGER;
            v_patch INTEGER;
        BEGIN
            SELECT version_number INTO v_current_version FROM metadata_version_record
            WHERE id = v_previous_version_id;
            
            IF SUBSTR(v_current_version, 1, 1) = 'v' THEN
                v_current_version := SUBSTR(v_current_version, 2);
            END IF;
            
            v_major := TO_NUMBER(SUBSTR(v_current_version, 1, INSTR(v_current_version, '.') - 1));
            v_minor := TO_NUMBER(SUBSTR(v_current_version, INSTR(v_current_version, '.') + 1, 
                                INSTR(v_current_version, '.', 1, 2) - INSTR(v_current_version, '.') - 1));
            v_patch := TO_NUMBER(SUBSTR(v_current_version, INSTR(v_current_version, '.', 1, 2) + 1));
            
            v_patch := v_patch + 1;
            v_version_number := 'v' || v_major || '.' || v_minor || '.' || v_patch;
        END;
    END IF;
    
    -- 将当前版本设置为非当前版本
    IF v_previous_version_id IS NOT NULL THEN
        UPDATE metadata_version_record
        SET is_current = 0,
            update_time = v_current_time
        WHERE id = v_previous_version_id;
    END IF;
    
    -- 插入新版本记录
    INSERT INTO metadata_version_record (
        id, version_number, version_name, version_description,
        metadata_id, metadata_type, metadata_name, metadata_path,
        publisher_id, publisher_name, publish_time,
        change_summary, previous_version_id, is_current,
        approval_status, tags, create_time, update_time, is_deleted
    ) VALUES (
        v_version_id, v_version_number, p_version_name, p_version_description,
        p_metadata_id, p_metadata_type, p_metadata_name, p_metadata_path,
        p_publisher_id, p_publisher_name, v_current_time,
        p_change_summary, v_previous_version_id, 1,
        'pending', p_tags, v_current_time, v_current_time, 0
    );
    
    -- 保存定版元数据
    CASE p_metadata_type
        WHEN 'catalog' THEN
            CALL save_versioned_catalog(v_version_id, p_metadata_id);
        WHEN 'database' THEN
            CALL save_versioned_database(v_version_id, p_metadata_id);
        WHEN 'table' THEN
            CALL save_versioned_table(v_version_id, p_metadata_id);
        WHEN 'field' THEN
            CALL save_versioned_field(v_version_id, p_metadata_id);
        ELSE
            NULL;
    END CASE;
    
    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END;
/

-- 创建存储过程：保存定版目录
CREATE OR REPLACE PROCEDURE save_versioned_catalog(
    p_version_id VARCHAR(32),
    p_catalog_id VARCHAR(32)
)
AS
    CURSOR c_databases IS
        SELECT id FROM metadata_collection
        WHERE catalog_id = p_catalog_id
          AND is_deleted = 0;
BEGIN
    -- 为每个数据库保存定版信息
    FOR db_rec IN c_databases LOOP
        CALL save_versioned_database(p_version_id, db_rec.id);
    END LOOP;
END;
/

-- 创建存储过程：保存定版数据库
CREATE OR REPLACE PROCEDURE save_versioned_database(
    p_version_id VARCHAR(32),
    p_database_id VARCHAR(32)
)
AS
    v_database metadata_collection%ROWTYPE;
    v_versioned_id VARCHAR(32);
    
    CURSOR c_tables IS
        SELECT id FROM metadata_table_structure
        WHERE collection_id = p_database_id
          AND is_deleted = 0;
BEGIN
    -- 获取数据库信息
    SELECT * INTO v_database FROM metadata_collection
    WHERE id = p_database_id AND is_deleted = 0;
    
    -- 生成定版ID
    SELECT REPLACE(SYS_GUID(), '-', '') INTO v_versioned_id FROM DUAL;
    
    -- 插入定版数据库记录
    INSERT INTO versioned_metadata_collection (
        id, version_id, original_id, name, description, type,
        data_source_id, data_source_name, metadata_count,
        creator_id, creator_name, create_time, update_time,
        lineage_enabled, last_lineage_analysis_time, import_config,
        tags, catalog_id, matching_strategy, storage_strategy,
        schedule_config, notification_config, quality_rules,
        catalog_name, data_source_type, matching_strategy_desc,
        storage_strategy_desc, owner
    ) VALUES (
        v_versioned_id, p_version_id, v_database.id, v_database.name,
        v_database.description, v_database.type, v_database.data_source_id,
        v_database.data_source_name, v_database.metadata_count,
        v_database.creator_id, v_database.creator_name, v_database.create_time,
        v_database.update_time, v_database.lineage_enabled,
        v_database.last_lineage_analysis_time, v_database.import_config,
        v_database.tags, v_database.catalog_id, v_database.matching_strategy,
        v_database.storage_strategy, v_database.schedule_config,
        v_database.notification_config, v_database.quality_rules,
        v_database.catalog_name, v_database.data_source_type,
        v_database.matching_strategy_desc, v_database.storage_strategy_desc,
        v_database.owner
    );
    
    -- 为每个表保存定版信息
    FOR tbl_rec IN c_tables LOOP
        CALL save_versioned_table(p_version_id, tbl_rec.id);
    END LOOP;
END;
/

-- 创建存储过程：保存定版表
CREATE OR REPLACE PROCEDURE save_versioned_table(
    p_version_id VARCHAR(32),
    p_table_id VARCHAR(32)
)
AS
    v_table metadata_table_structure%ROWTYPE;
    v_versioned_id VARCHAR(32);
    
    CURSOR c_fields IS
        SELECT id FROM metadata_field_info
        WHERE table_id = p_table_id
          AND is_deleted = 0;
BEGIN
    -- 获取表信息
    SELECT * INTO v_table FROM metadata_table_structure
    WHERE id = p_table_id AND is_deleted = 0;
    
    -- 生成定版ID
    SELECT REPLACE(SYS_GUID(), '-', '') INTO v_versioned_id FROM DUAL;
    
    -- 插入定版表记录
    INSERT INTO versioned_metadata_table_structure (
        id, version_id, original_id, collection_id, execution_id,
        table_name, table_type, table_comment, create_sql,
        create_time, update_time, version, engine, charset,
        collation_rule, table_size, row_count, tablespace,
        import_status, db_name, owner, table_size_mb,
        table_create_time, table_update_time, table_ddl,
        column_count, index_count, constraint_count,
        trigger_count, partition_count, storage_info,
        column_type_summary, index_info, primary_key_info,
        foreign_key_info, dependency_info, extended_info
    ) VALUES (
        v_versioned_id, p_version_id, v_table.id, v_table.collection_id,
        v_table.execution_id, v_table.table_name, v_table.table_type,
        v_table.table_comment, v_table.create_sql, v_table.create_time,
        v_table.update_time, v_table.version, v_table.engine,
        v_table.charset, v_table.collation_rule, v_table.table_size,
        v_table.row_count, v_table.tablespace, v_table.import_status,
        v_table.db_name, v_table.owner, v_table.table_size_mb,
        v_table.table_create_time, v_table.table_update_time,
        v_table.table_ddl, v_table.column_count, v_table.index_count,
        v_table.constraint_count, v_table.trigger_count,
        v_table.partition_count, v_table.storage_info,
        v_table.column_type_summary, v_table.index_info,
        v_table.primary_key_info, v_table.foreign_key_info,
        v_table.dependency_info, v_table.extended_info
    );
    
    -- 为每个字段保存定版信息
    FOR fld_rec IN c_fields LOOP
        CALL save_versioned_field(p_version_id, fld_rec.id);
    END LOOP;
END;
/

-- 创建存储过程：保存定版字段
CREATE OR REPLACE PROCEDURE save_versioned_field(
    p_version_id VARCHAR(32),
    p_field_id VARCHAR(32)
)
AS
    v_field metadata_field_info%ROWTYPE;
    v_versioned_id VARCHAR(32);
BEGIN
    -- 获取字段信息
    SELECT * INTO v_field FROM metadata_field_info
    WHERE id = p_field_id AND is_deleted = 0;
    
    -- 生成定版ID
    SELECT REPLACE(SYS_GUID(), '-', '') INTO v_versioned_id FROM DUAL;
    
    -- 插入定版字段记录
    INSERT INTO versioned_metadata_field_info (
        id, version_id, original_id, table_id, field_name,
        field_type, field_comment, is_primary_key, is_nullable,
        default_value, field_length, field_precision, field_scale,
        field_order, create_time, update_time, is_auto_increment,
        is_unique, is_indexed, is_foreign_key, referenced_table,
        referenced_field, field_description, standard_id,
        standard_name, match_rate, sensitive_level, quality_rules,
        business_rules, tags, field_alias, field_category,
        field_source, is_calculated, calculation_expression,
        sample_value, field_format, field_unit, field_range,
        field_enum, extended_info
    ) VALUES (
        v_versioned_id, p_version_id, v_field.id, v_field.table_id,
        v_field.field_name, v_field.field_type, v_field.field_comment,
        v_field.is_primary_key, v_field.is_nullable, v_field.default_value,
        v_field.field_length, v_field.field_precision, v_field.field_scale,
        v_field.field_order, v_field.create_time, v_field.update_time,
        v_field.is_auto_increment, v_field.is_unique, v_field.is_indexed,
        v_field.is_foreign_key, v_field.referenced_table, v_field.referenced_field,
        v_field.field_description, v_field.standard_id, v_field.standard_name,
        v_field.match_rate, v_field.sensitive_level, v_field.quality_rules,
        v_field.business_rules, v_field.tags, v_field.field_alias,
        v_field.field_category, v_field.field_source, v_field.is_calculated,
        v_field.calculation_expression, v_field.sample_value,
        v_field.field_format, v_field.field_unit, v_field.field_range,
        v_field.field_enum, v_field.extended_info
    );
END;
/

-- 创建函数：获取当前版本
CREATE OR REPLACE FUNCTION get_current_version(
    p_metadata_id VARCHAR(32),
    p_metadata_type VARCHAR(50)
) RETURN VARCHAR(32)
AS
    v_version_id VARCHAR(32);
BEGIN
    SELECT id INTO v_version_id FROM metadata_version_record
    WHERE metadata_id = p_metadata_id
      AND metadata_type = p_metadata_type
      AND is_current = 1
      AND is_deleted = 0
    ORDER BY publish_time DESC
    FETCH FIRST 1 ROWS ONLY;
    
    RETURN v_version_id;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RETURN NULL;
END;
/

-- 创建存储过程：回滚到指定版本
CREATE OR REPLACE PROCEDURE rollback_to_version(
    p_version_id VARCHAR(32),
    p_comment VARCHAR(500),
    p_user_id VARCHAR(32),
    p_user_name VARCHAR(100),
    p_success OUT NUMBER
)
AS
    v_target_version metadata_version_record%ROWTYPE;
    v_current_version metadata_version_record%ROWTYPE;
    v_current_time TIMESTAMP;
    v_log_id VARCHAR(32);
BEGIN
    p_success := 0;
    v_current_time := SYSTIMESTAMP;
    
    -- 获取目标版本
    SELECT * INTO v_target_version FROM metadata_version_record
    WHERE id = p_version_id AND is_deleted = 0;
    
    -- 获取当前版本
    SELECT * INTO v_current_version FROM metadata_version_record
    WHERE metadata_id = v_target_version.metadata_id
      AND metadata_type = v_target_version.metadata_type
      AND is_current = 1
      AND is_deleted = 0;
    
    -- 如果目标版本已经是当前版本，则无需回滚
    IF p_version_id = v_current_version.id THEN
        p_success := 1;
        RETURN;
    END IF;
    
    -- 更新版本状态
    UPDATE metadata_version_record
    SET is_current = 0,
        update_time = v_current_time
    WHERE id = v_current_version.id;
    
    UPDATE metadata_version_record
    SET is_current = 1,
        update_time = v_current_time
    WHERE id = p_version_id;
    
    -- 记录回滚操作日志
    SELECT REPLACE(SYS_GUID(), '-', '') INTO v_log_id FROM DUAL;
    
    INSERT INTO metadata_version_change_log (
        id, version_id, change_type, object_type, object_id,
        object_name, object_path, change_description, change_time,
        changer_id, changer_name, create_time, is_deleted
    ) VALUES (
        v_log_id, p_version_id, 'rollback', v_target_version.metadata_type,
        v_target_version.metadata_id, v_target_version.metadata_name,
        v_target_version.metadata_path, '回滚到版本 ' || v_target_version.version_number || ': ' || p_comment,
        v_current_time, p_user_id, p_user_name, v_current_time, 0
    );
    
    COMMIT;
    p_success := 1;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        ROLLBACK;
        p_success := 0;
    WHEN OTHERS THEN
        ROLLBACK;
        p_success := 0;
        RAISE;
END;
/ 