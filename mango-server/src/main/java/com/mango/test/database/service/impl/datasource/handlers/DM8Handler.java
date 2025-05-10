package com.mango.test.database.service.impl.datasource.handlers;

import com.mango.test.database.entity.DataSource;
import com.mango.test.database.entity.TableDefinition;
import com.mango.test.database.entity.ColumnDefinition;
import java.text.SimpleDateFormat;
import java.util.List;

public class DM8Handler extends DM7Handler {

    private static final String DRIVER_CLASS = "dm.jdbc.driver.DmDriver";
    private static final String DEFAULT_PORT = "5236";
    private static final String URL_TEMPLATE = "jdbc:dm://%s:%s/%s";

    public DM8Handler(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public String getDatabaseProductName() {
        return "DM8";
    }

    @Override
    public String convertCreateTableSql(String sourceSql, String sourceDbType) {
        try {
            // 解析源SQL
            TableDefinition tableDefinition = parseCreateTableSql(sourceSql);
            
            // 转换字段类型
            for (ColumnDefinition column : tableDefinition.getColumns()) {
                String sourceType = column.getType();
                String targetType = convertFromOtherDbType(sourceType, sourceDbType);
                column.setType(targetType);
                
                // 使用DM8的列类型调整方法
                adjustDM8ColumnType(column);
            }
            
            // 生成DM8建表语句
            return generateCreateTableSql(tableDefinition);
        } catch (Exception e) {
            log.error("Failed to convert CREATE TABLE SQL from {} to DM8: {}", sourceDbType, e.getMessage());
            throw new RuntimeException("Failed to convert CREATE TABLE SQL", e);
        }
    }

    @Override
    public TableDefinition parseCreateTableSql(String createTableSql) {
        // DM8 完全兼容 DM7 的建表语法，直接调用父类方法
        return super.parseCreateTableSql(createTableSql);
    }

    @Override
    public String generateCreateTableSql(TableDefinition tableDefinition) {
        // DM8 完全兼容 DM7 的建表语法，直接调用父类方法
        return super.generateCreateTableSql(tableDefinition);
    }

    public void adjustDM8ColumnType(ColumnDefinition column) {
        // 先调用父类方法处理通用逻辑
        super.adjustDM7ColumnType(column);
        
        String type = column.getType().toUpperCase();
        
        // DM8特有的类型调整
        switch (type) {
            case "VARCHAR":
            case "VARCHAR2":
                // DM8允许更大的VARCHAR长度
                if (column.getLength() != null && column.getLength() > 32767) {
                    column.setLength(32767); // DM8的VARCHAR最大长度限制
                }
                break;
            case "NVARCHAR":
                // 确保NVARCHAR有合理的长度
                if (column.getLength() == null || column.getLength() <= 0) {
                    column.setLength(getDefaultVarcharLength());
                } else if (column.getLength() > 16383) {
                    column.setLength(16383); // DM8的NVARCHAR最大长度限制
                }
                break;
            case "NUMBER":
                // DM8对NUMBER类型的精度处理
                if (column.getPrecision() != null && column.getPrecision() > 38) {
                    column.setPrecision(38); // DM8的NUMBER最大精度
                }
                break;
        }
    }

    @Override
    public String generatePageSql(String sql, int offset, int limit) {
        // DM8支持更标准的OFFSET FETCH语法
        return sql + " OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY";
    }

    @Override
    public String wrapValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? "1" : "0";
        }
        if (value instanceof java.sql.Date) {
            // DM8日期格式
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return "DATE '" + sdf.format((java.sql.Date)value) + "'";
        }
        if (value instanceof java.sql.Timestamp || value instanceof java.util.Date) {
            // DM8时间戳格式
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            return "TIMESTAMP '" + sdf.format(value) + "'";
        }
        // 字符串值需要单引号转义
        return "'" + value.toString().replace("'", "''") + "'";
    }

    @Override
    public String mapJavaTypeToDbType(Class<?> javaType) {
        if (org.w3c.dom.Document.class.isAssignableFrom(javaType) || 
            javax.xml.bind.JAXBElement.class.isAssignableFrom(javaType) ||
            javaType.getName().contains("XMLGregorianCalendar")) {
            return "XML";
        }
        
        // 调用父类方法处理其他类型
        return super.mapJavaTypeToDbType(javaType);
    }

    @Override
    public Class<?> mapDbTypeToJavaType(String dbType) {
        if (dbType != null && dbType.toUpperCase().equals("JSON")) {
            return String.class; // 或使用专门的JSON处理类
        }
        
        return super.mapDbTypeToJavaType(dbType);
    }

    private String getValidationSqlForDataType(String tableName, String columnName, String dataType) {
        dataType = dataType.toUpperCase();
        
        if (dataType.contains("CHAR") || dataType.contains("TEXT") || dataType.contains("CLOB")) {
            // 字符串类型验证
            return String.format(
                "SELECT COUNT(*) AS invalid_count FROM %s WHERE %s IS NOT NULL AND LENGTH(TRIM(%s)) = 0",
                wrapIdentifier(tableName),
                wrapIdentifier(columnName),
                wrapIdentifier(columnName)
            );
        } else if (dataType.equals("NUMBER") || dataType.contains("INT") || dataType.contains("DECIMAL") || 
                   dataType.contains("FLOAT") || dataType.contains("DOUBLE")) {
            // 数值类型验证 - DM8支持REGEXP_LIKE
            return String.format(
                "SELECT COUNT(*) AS invalid_count FROM %s WHERE %s IS NOT NULL AND " +
                "NOT REGEXP_LIKE(TO_CHAR(%s), '^[+-]?[0-9]*(\\.[0-9]+)?$')",
                wrapIdentifier(tableName),
                wrapIdentifier(columnName),
                wrapIdentifier(columnName)
            );
        } else if (dataType.contains("DATE") || dataType.equals("TIMESTAMP")) {
            // 日期类型验证
            return String.format(
                "SELECT COUNT(*) AS invalid_count FROM %s WHERE %s IS NOT NULL AND " +
                "(%s < DATE '0001-01-01' OR %s > DATE '9999-12-31')",
                wrapIdentifier(tableName),
                wrapIdentifier(columnName),
                wrapIdentifier(columnName),
                wrapIdentifier(columnName)
            );
        }
        
        // 默认验证
        return String.format(
            "SELECT COUNT(*) AS invalid_count FROM %s WHERE %s IS NOT NULL",
            wrapIdentifier(tableName),
            wrapIdentifier(columnName)
        );
    }

    @Override
    public String getCreateTableSql(String tableName, List<ColumnDefinition> columns,
                                  String tableComment, String engine, String charset, String collate) {
        StringBuilder sql = new StringBuilder();
        
        // 基本表结构部分沿用父类方法
        sql.append(super.getCreateTableSql(tableName, columns, tableComment, null, charset, collate));
        
        // 添加DM8特有的存储参数
        if (engine != null && !engine.isEmpty()) {
            sql.append(" TABLESPACE ").append(engine);
        }
        
        return sql.toString();
    }

    @Override
    public String getAddIndexSql(String tableName, String indexName, List<String> columns, boolean unique) {
        StringBuilder columnStr = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                columnStr.append(", ");
            }
            columnStr.append(wrapIdentifier(columns.get(i)));
        }
        
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE ");
        if (unique) {
            sql.append("UNIQUE ");
        }
        sql.append("INDEX ")
           .append(wrapIdentifier(indexName))
           .append(" ON ")
           .append(wrapIdentifier(tableName))
           .append(" (")
           .append(columnStr)
           .append(")");
        
        // DM8支持更多索引类型
        sql.append(" USING BTREE");
        
        return sql.toString();
    }

    // DM8 特有的功能可以在这里重写相应的方法
} 