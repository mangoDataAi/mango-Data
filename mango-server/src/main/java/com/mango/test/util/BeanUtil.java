package com.mango.test.util;

import com.mango.test.constant.DatabaseTypes;
import org.springframework.beans.BeanUtils;

public class BeanUtil {

    public static <T> T copyProperties(Object source, Class<T> targetClass) {
        try {
            T target = targetClass.newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("Bean copy failed", e);
        }
    }

    /**
     * 从SQL中解析表名
     */
    public static String parseTableName(String sql) {
        // 简单的表名解析逻辑，实际项目中需要更复杂的解析
        sql = sql.toLowerCase();
        String tableName = null;
        if (sql.contains("from")) {
            tableName = sql.substring(sql.indexOf("from") + 4).trim();
        } else if (sql.contains("describe")) {
            tableName = sql.substring(sql.indexOf("describe") + 8).trim();
        } else if (sql.contains("desc")) {
            tableName = sql.substring(sql.indexOf("desc") + 4).trim();
        }

        if (tableName != null) {
            // 移除额外的修饰符和空格
            tableName = tableName.split("\\s+")[0];
            // 移除schema前缀
            if (tableName.contains(".")) {
                tableName = tableName.substring(tableName.indexOf(".") + 1);
            }
        }
        return tableName;
    }

    /**
     * 从SQL中解析schema名
     */
    public static String parseSchemaName(String sql) {
        sql = sql.toLowerCase();
        String[] parts = sql.split("\\s+");
        for (String part : parts) {
            if (part.contains(".")) {
                return part.split("\\.")[0];
            }
        }
        return null;
    }

    /**
     * 获取表结构查询SQL
     */
    public static String getTableStructureSQL(String dbType, String tableName, String schemaName) {
        String dbFamily = DatabaseTypes.getDbFamily(dbType.toLowerCase());
        switch (dbFamily) {
            case "mysql":  // MySQL, MariaDB, H2, TiDB
                return "SELECT " +
                        "COLUMN_NAME as name, " +
                        "DATA_TYPE as type, " +
                        "COLUMN_COMMENT as comment, " +
                        "CHARACTER_MAXIMUM_LENGTH as length, " +
                        "NUMERIC_PRECISION as precision, " +
                        "NUMERIC_SCALE as scale, " +
                        "IS_NULLABLE as nullable, " +
                        "COLUMN_DEFAULT as defaultValue, " +
                        "COLUMN_KEY as keyType " +
                        "FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_NAME = '" + tableName + "' " +
                        (schemaName != null ? "AND TABLE_SCHEMA = '" + schemaName + "' " : "") +
                        "ORDER BY ORDINAL_POSITION";

            case "oracle":  // Oracle, DM7/8, 神通
                return "SELECT " +
                        "a.COLUMN_NAME as name, " +
                        "a.DATA_TYPE as type, " +
                        "b.COMMENTS as comment, " +
                        "a.DATA_LENGTH as length, " +
                        "a.DATA_PRECISION as precision, " +
                        "a.DATA_SCALE as scale, " +
                        "a.NULLABLE as nullable, " +
                        "a.DATA_DEFAULT as defaultValue, " +
                        "CASE WHEN c.COLUMN_NAME IS NOT NULL THEN 'PRI' ELSE '' END as keyType " +
                        "FROM ALL_TAB_COLUMNS a " +
                        "LEFT JOIN ALL_COL_COMMENTS b ON a.TABLE_NAME = b.TABLE_NAME AND a.COLUMN_NAME = b.COLUMN_NAME " +
                        "LEFT JOIN ALL_CONSTRAINTS pk ON a.TABLE_NAME = pk.TABLE_NAME AND pk.CONSTRAINT_TYPE = 'P' " +
                        "LEFT JOIN ALL_CONS_COLUMNS c ON pk.CONSTRAINT_NAME = c.CONSTRAINT_NAME AND a.COLUMN_NAME = c.COLUMN_NAME " +
                        "WHERE a.TABLE_NAME = '" + tableName.toUpperCase() + "' " +
                        (schemaName != null ? "AND a.OWNER = '" + schemaName.toUpperCase() + "' " : "") +
                        "ORDER BY a.COLUMN_ID";

            case "postgresql":  // PostgreSQL, GaussDB, OpenGauss, 人大金仓, 瀚高等
                return "SELECT " +
                        "a.attname as name, " +
                        "format_type(a.atttypid, a.atttypmod) as type, " +
                        "col_description(a.attrelid, a.attnum) as comment, " +
                        "CASE WHEN a.atttypmod > 0 THEN a.atttypmod - 4 ELSE null END as length, " +
                        "information_schema._pg_numeric_precision(a.atttypid, a.atttypmod) as precision, " +
                        "information_schema._pg_numeric_scale(a.atttypid, a.atttypmod) as scale, " +
                        "CASE WHEN a.attnotnull THEN 'NO' ELSE 'YES' END as nullable, " +
                        "pg_get_expr(d.adbin, d.adrelid) as defaultValue, " +
                        "CASE WHEN p.contype = 'p' THEN 'PRI' ELSE '' END as keyType " +
                        "FROM pg_attribute a " +
                        "LEFT JOIN pg_attrdef d ON a.attrelid = d.adrelid AND a.attnum = d.adnum " +
                        "LEFT JOIN pg_constraint p ON a.attrelid = p.conrelid AND a.attnum = ANY(p.conkey) AND p.contype = 'p' " +
                        "WHERE a.attrelid = '" + tableName + "'::regclass AND a.attnum > 0 AND NOT a.attisdropped " +
                        "ORDER BY a.attnum";

            case "sqlserver":  // SQLServer, Sybase
                return "SELECT " +
                        "c.name as name, " +
                        "t.name as type, " +
                        "ep.value as comment, " +
                        "c.max_length as length, " +
                        "c.precision as precision, " +
                        "c.scale as scale, " +
                        "c.is_nullable as nullable, " +
                        "OBJECT_DEFINITION(c.default_object_id) as defaultValue, " +
                        "CASE WHEN i.is_primary_key = 1 THEN 'PRI' ELSE '' END as keyType " +
                        "FROM sys.columns c " +
                        "JOIN sys.types t ON c.user_type_id = t.user_type_id " +
                        "LEFT JOIN sys.extended_properties ep ON ep.major_id = c.object_id AND ep.minor_id = c.column_id " +
                        "LEFT JOIN sys.index_columns ic ON ic.object_id = c.object_id AND ic.column_id = c.column_id " +
                        "LEFT JOIN sys.indexes i ON i.object_id = ic.object_id AND i.index_id = ic.index_id " +
                        "WHERE c.object_id = OBJECT_ID('" +
                        (schemaName != null ? schemaName + "." : "") + tableName + "') " +
                        "ORDER BY c.column_id";

            case "db2":  // DB2
                return "SELECT " +
                        "COLNAME as name, " +
                        "TYPENAME as type, " +
                        "REMARKS as comment, " +
                        "LENGTH as length, " +
                        "SCALE as scale, " +
                        "NULLS as nullable, " +
                        "DEFAULT as defaultValue, " +
                        "CASE WHEN KEYSEQ > 0 THEN 'PRI' ELSE '' END as keyType " +
                        "FROM SYSCAT.COLUMNS " +
                        "WHERE TABNAME = '" + tableName.toUpperCase() + "' " +
                        (schemaName != null ? "AND TABSCHEMA = '" + schemaName.toUpperCase() + "' " : "") +
                        "ORDER BY COLNO";

            default:
                throw new UnsupportedOperationException("Unsupported database type: " + dbType);
        }
    }


} 