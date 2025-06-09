package com.mango.test.database.service.impl.datasource.handlers;

import com.mango.test.database.entity.ColumnDefinition;
import com.mango.test.database.entity.DataSource;
import com.mango.test.database.entity.IndexDefinition;
import com.mango.test.database.entity.TableDefinition;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class KingbaseHandler extends PostgreSQLHandler {

    private static final String DRIVER_CLASS = "com.kingbase8.Driver";
    private static final String DEFAULT_PORT = "54321";
    private static final String URL_TEMPLATE = "jdbc:kingbase8://%s:%s/%s";

    public KingbaseHandler(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public String getDriverClassName() {
        return DRIVER_CLASS;
    }

    @Override
    public String getDefaultPort() {
        return DEFAULT_PORT;
    }

    @Override
    public String getJdbcUrl() {
        return String.format(URL_TEMPLATE,
            dataSource.getHost(),
            dataSource.getPort() != null ? dataSource.getPort() : getDefaultPort(),
            dataSource.getDbName()
        );
    }

    @Override
    public String getDatabaseProductName() {
        return "Kingbase";
    }

    @Override
    public List<String> getSystemDatabases() {
        return Arrays.asList("postgres", "template0", "template1", "kingbase", "sys");
    }

    @Override
    public String convertCreateTableSql(String sourceSql, String sourceDbType) {
        try {
            TableDefinition tableDefinition = parseCreateTableSql(sourceSql);
            
            for (ColumnDefinition column : tableDefinition.getColumns()) {
                String sourceType = column.getType();
                String targetType = convertDataType(sourceType, sourceDbType, getDatabaseProductName());
                column.setType(targetType);
                adjustKingbaseColumnType(column);
            }
            
            return generateCreateTableSql(tableDefinition);
        } catch (Exception e) {
            log.error("Failed to convert CREATE TABLE SQL from {} to Kingbase: {}", sourceDbType, e.getMessage());
            throw new RuntimeException("Failed to convert CREATE TABLE SQL", e);
        }
    }

    @Override
    public TableDefinition parseCreateTableSql(String createTableSql) {
        TableDefinition tableDefinition = new TableDefinition();
        List<ColumnDefinition> columns = new ArrayList<>();
        List<IndexDefinition> indexes = new ArrayList<>();

        try {
            Pattern tablePattern = Pattern.compile("CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([\\w\\.\"]+)\\s*\\(", 
                Pattern.CASE_INSENSITIVE);
            Matcher tableMatcher = tablePattern.matcher(createTableSql);
            if (tableMatcher.find()) {
                String tableName = tableMatcher.group(1).replace("\"", "");
                tableDefinition.setTableName(tableName);
            }

            int startIndex = createTableSql.indexOf('(');
            int endIndex = createTableSql.lastIndexOf(')');
            if (startIndex == -1 || endIndex == -1) {
                throw new RuntimeException("Invalid CREATE TABLE SQL statement");
            }

            String columnsPart = createTableSql.substring(startIndex + 1, endIndex);
            String[] definitions = splitDefinitions(columnsPart);

            for (String definition : definitions) {
                definition = definition.trim();
                if (definition.isEmpty()) continue;

                if (definition.toUpperCase().startsWith("PRIMARY KEY")) {
                    Pattern pkPattern = Pattern.compile("PRIMARY\\s+KEY\\s*\\(([^)]+)\\)", 
                        Pattern.CASE_INSENSITIVE);
                    Matcher pkMatcher = pkPattern.matcher(definition);
                    if (pkMatcher.find()) {
                        String[] pkColumns = pkMatcher.group(1).split(",");
                        for (String pkColumn : pkColumns) {
                            String columnName = pkColumn.trim().replace("\"", "");
                            for (ColumnDefinition column : columns) {
                                if (column.getName().equalsIgnoreCase(columnName)) {
                                    column.setPrimaryKey(true);
                                    break;
                                }
                            }
                        }
                    }
                    continue;
                }

                if (definition.toUpperCase().startsWith("UNIQUE")) {
                    Pattern uniquePattern = Pattern.compile("UNIQUE\\s*\\(([^)]+)\\)", 
                        Pattern.CASE_INSENSITIVE);
                    Matcher uniqueMatcher = uniquePattern.matcher(definition);
                    if (uniqueMatcher.find()) {
                        String[] uniqueColumns = uniqueMatcher.group(1).split(",");
                        IndexDefinition index = new IndexDefinition();
                        index.setUnique(true);
                        index.setColumns(Arrays.asList(uniqueColumns));
                        indexes.add(index);
                    }
                    continue;
                }

                ColumnDefinition column = parseColumnDefinition(definition);
                if (column != null) {
                    columns.add(column);
                }
            }

            tableDefinition.setColumns(columns);
            tableDefinition.setIndexes(indexes);

            // 解析表空间
            Pattern tablespacePattern = Pattern.compile("TABLESPACE\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
            Matcher tablespaceMatcher = tablespacePattern.matcher(createTableSql);
            if (tablespaceMatcher.find()) {
                Map<String, String> extraProps = new HashMap<>();
                extraProps.put("TABLESPACE", tablespaceMatcher.group(1));
                tableDefinition.setExtraProperties(extraProps);
            }

            return tableDefinition;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CREATE TABLE SQL: " + e.getMessage(), e);
        }
    }

    private String[] splitDefinitions(String columnsPart) {
        List<String> definitions = new ArrayList<>();
        StringBuilder currentDefinition = new StringBuilder();
        int parenthesesCount = 0;
        boolean inQuotes = false;
        char[] chars = columnsPart.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            
            if (c == '(' && !inQuotes) {
                parenthesesCount++;
            } else if (c == ')' && !inQuotes) {
                parenthesesCount--;
            } else if (c == '\'' && (i == 0 || chars[i-1] != '\\')) {
                inQuotes = !inQuotes;
            }

            if (c == ',' && parenthesesCount == 0 && !inQuotes) {
                definitions.add(currentDefinition.toString().trim());
                currentDefinition = new StringBuilder();
            } else {
                currentDefinition.append(c);
            }
        }

        if (currentDefinition.length() > 0) {
            definitions.add(currentDefinition.toString().trim());
        }

        return definitions.toArray(new String[0]);
    }

    private ColumnDefinition parseColumnDefinition(String definition) {
        Pattern columnPattern = Pattern.compile(
            "\"?([\\w\\.]+)\"?\\s+" +                   // 列名，支持带点号的名称
            "([\\w\\(\\),\\[\\]\" ]+)" +                // 数据类型，支持数组和模式
            "(?:\\s+DEFAULT\\s+([^,;]+?))?" +          // 默认值（可选），支持复杂表达式
            "(?:\\s+NOT\\s+NULL)?" +                   // NOT NULL（可选）
            "(?:\\s+IDENTITY(?:\\s*\\([^)]+\\))?)?" +  // 自增（可选）
            "(?:\\s+COMMENT\\s+'([^']*)')?" +          // 注释（可选）
            "(?:\\s+PRIMARY\\s+KEY)?",                 // 主键（可选）
            Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = columnPattern.matcher(definition);
        if (!matcher.find()) {
            return null;
        }

        ColumnDefinition column = new ColumnDefinition();
        column.setName(matcher.group(1));

        String fullType = matcher.group(2).trim();
        Pattern typePattern = Pattern.compile("([\\w]+)(?:\\((\\d+)(?:,(\\d+))?\\))?");
        Matcher typeMatcher = typePattern.matcher(fullType);
        if (typeMatcher.find()) {
            column.setType(typeMatcher.group(1));
            if (typeMatcher.group(2) != null) {
                if (typeMatcher.group(3) != null) {
                    column.setPrecision(Integer.parseInt(typeMatcher.group(2)));
                    column.setScale(Integer.parseInt(typeMatcher.group(3)));
                } else {
                    column.setLength(Integer.parseInt(typeMatcher.group(2)));
                }
            }
        }

        if (matcher.group(3) != null) {
            column.setDefaultValue(matcher.group(3).trim());
        }

        if (matcher.group(4) != null) {
            column.setComment(matcher.group(4));
        }

        column.setNullable(!definition.toUpperCase().contains("NOT NULL"));
        column.setAutoIncrement(definition.toUpperCase().contains("IDENTITY"));
        column.setPrimaryKey(definition.toUpperCase().contains("PRIMARY KEY"));

        return column;
    }

    @Override
    public String generateCreateTableSql(TableDefinition tableDefinition) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ");
        sql.append(wrapIdentifier(tableDefinition.getTableName()));
        sql.append(" (\n");

        List<String> columnDefinitions = new ArrayList<>();
        List<String> primaryKeys = new ArrayList<>();
        StringBuilder commentsSql = new StringBuilder();

        for (ColumnDefinition column : tableDefinition.getColumns()) {
            StringBuilder columnSql = new StringBuilder();
            columnSql.append(wrapIdentifier(column.getName())).append(" ");
            
            String type = column.getType().toUpperCase();
            switch (type) {
                case "VARCHAR":
                case "NVARCHAR":
                case "CHAR":
                case "NCHAR":
                case "TEXT":
                case "INTEGER":
                case "INT":
                case "BIGINT":
                case "SMALLINT":
                case "DECIMAL":
                case "NUMERIC":
                case "REAL":
                case "DOUBLE PRECISION":
                case "FLOAT":
                case "BOOLEAN":
                case "DATE":
                case "TIME":
                case "TIMESTAMP":
                case "BYTEA":
                case "CLOB":
                case "BLOB":
                case "XML":
                case "JSON":
                case "JSONB":
                    columnSql.append(type);
                    break;
                default:
                    columnSql.append("VARCHAR");
                    break;
            }

            if (column.getLength() != null) {
                if (type.equals("DECIMAL") || type.equals("NUMERIC")) {
                    columnSql.append("(").append(column.getPrecision())
                            .append(",").append(column.getScale()).append(")");
                } else if (!type.equals("TEXT") && !type.equals("BYTEA") && 
                           !type.equals("CLOB") && !type.equals("BLOB") && 
                           !type.equals("XML") && !type.equals("JSON") && 
                           !type.equals("JSONB")) {
                    columnSql.append("(").append(column.getLength()).append(")");
                }
            }

            if (!column.isNullable()) {
                columnSql.append(" NOT NULL");
            }

            if (column.getDefaultValue() != null) {
                columnSql.append(" DEFAULT ").append(column.getDefaultValue());
            }

            if (column.isAutoIncrement()) {
                columnSql.append(" IDENTITY(1,1)");
            }

            // 不在列定义中添加注释，而是之后用单独的 COMMENT ON COLUMN 语句添加
            if (column.getComment() != null) {
                commentsSql.append("COMMENT ON COLUMN ")
                          .append(wrapIdentifier(tableDefinition.getTableName()))
                          .append(".")
                          .append(wrapIdentifier(column.getName()))
                          .append(" IS ")
                          .append("'")
                          .append(column.getComment().replace("'", "''"))
                          .append("';\n");
            }

            if (column.isPrimaryKey()) {
                primaryKeys.add(wrapIdentifier(column.getName()));
            }

            columnDefinitions.add(columnSql.toString());
        }

        if (!primaryKeys.isEmpty()) {
            columnDefinitions.add("PRIMARY KEY (" + String.join(", ", primaryKeys) + ")");
        }

        sql.append(String.join(",\n", columnDefinitions));
        sql.append("\n)");

        Map<String, String> extraProps = tableDefinition.getExtraProperties();
        if (extraProps != null && extraProps.containsKey("TABLESPACE")) {
            sql.append("\nTABLESPACE ").append(extraProps.get("TABLESPACE"));
        }

        String tableSql = sql.toString();
        if (commentsSql.length() > 0) {
            return tableSql + ";\n" + commentsSql.toString();
        } else {
            return tableSql;
        }
    }

    @Override
    public String getAddIndexSql(String tableName, String indexName, List<String> columns, boolean unique) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE ");
        
        if (unique) {
            sql.append("UNIQUE ");
        }
        
        sql.append("INDEX ").append(wrapIdentifier(indexName))
           .append(" ON ").append(wrapIdentifier(tableName))
           .append(" (");
        
        List<String> wrappedColumns = new ArrayList<>();
        for (String column : columns) {
            wrappedColumns.add(wrapIdentifier(column));
        }
        
        sql.append(String.join(", ", wrappedColumns));
        sql.append(")");
        
        return sql.toString();
    }

    private void adjustKingbaseColumnType(ColumnDefinition column) {
        String type = column.getType().toUpperCase();
        
        if (type.equals("VARCHAR") || type.equals("NVARCHAR")) {
            if (column.getLength() == null) {
                column.setLength(255);
            }
            column.setLength(Math.min(column.getLength(), 10485760));
        }
        else if (type.equals("CHAR") || type.equals("NCHAR")) {
            if (column.getLength() == null) {
                column.setLength(1);
            }
            column.setLength(Math.min(column.getLength(), 10485760));
        }
        else if (type.equals("DECIMAL") || type.equals("NUMERIC")) {
            if (column.getPrecision() == null) {
                column.setPrecision(38);
            }
            if (column.getScale() == null) {
                column.setScale(0);
            }
            column.setPrecision(Math.min(column.getPrecision(), 38));
            column.setScale(Math.min(column.getScale(), column.getPrecision()));
        }
        else if (type.equals("TEXT") || type.equals("BYTEA") || 
                 type.equals("CLOB") || type.equals("BLOB") || 
                 type.equals("XML") || type.equals("JSON") || 
                 type.equals("JSONB")) {
            column.setLength(null);
        }
    }

    @Override
    public String convertDataType(String sourceType, String sourceDbType, String targetDbType) {
        if (sourceType == null || sourceType.isEmpty()) {
            return "VARCHAR(255)";
        }
        
        String upperSourceType = sourceType.toUpperCase();
        
        // 对于特定数据库类型的转换
        if ("MYSQL".equalsIgnoreCase(sourceDbType)) {
            if (upperSourceType.contains("INT") && !upperSourceType.contains("POINT")) {
                if (upperSourceType.contains("TINYINT(1)")) {
                    return "BOOLEAN";
                } else if (upperSourceType.contains("TINYINT")) {
                    return "SMALLINT";
                } else if (upperSourceType.contains("SMALLINT")) {
                    return "SMALLINT";
                } else if (upperSourceType.contains("MEDIUMINT")) {
                    return "INTEGER";
                } else if (upperSourceType.contains("BIGINT")) {
                    return "BIGINT";
                } else {
                    return "INTEGER";
                }
            } else if (upperSourceType.contains("CHAR")) {
                if (upperSourceType.contains("VARCHAR")) {
                    return upperSourceType.replace("VARCHAR", "VARCHAR");
                } else if (upperSourceType.contains("NVARCHAR")) {
                    return upperSourceType.replace("NVARCHAR", "NVARCHAR");
                } else {
                    return upperSourceType;
                }
            } else if (upperSourceType.equals("TEXT") || upperSourceType.equals("LONGTEXT") || 
                       upperSourceType.equals("MEDIUMTEXT") || upperSourceType.equals("TINYTEXT")) {
                return "TEXT";
            } else if (upperSourceType.contains("BLOB")) {
                return "BYTEA";
            } else if (upperSourceType.contains("FLOAT")) {
                return "REAL";
            } else if (upperSourceType.contains("DOUBLE")) {
                return "DOUBLE PRECISION";
            } else if (upperSourceType.equals("DATETIME") || upperSourceType.equals("TIMESTAMP")) {
                return "TIMESTAMP";
            } else if (upperSourceType.equals("BOOLEAN") || upperSourceType.equals("BOOL")) {
                return "BOOLEAN";
            } else if (upperSourceType.contains("ENUM") || upperSourceType.contains("SET")) {
                return "VARCHAR(255)";
            }
        } else if ("ORACLE".equalsIgnoreCase(sourceDbType)) {
            if (upperSourceType.contains("NUMBER")) {
                Pattern pattern = Pattern.compile("NUMBER\\((\\d+)(?:,(\\d+))?\\)");
                Matcher matcher = pattern.matcher(upperSourceType);
                if (matcher.find()) {
                    int precision = Integer.parseInt(matcher.group(1));
                    if (matcher.group(2) != null) {
                        int scale = Integer.parseInt(matcher.group(2));
                        return "NUMERIC(" + precision + "," + scale + ")";
                    } else {
                        if (precision <= 4) {
                            return "SMALLINT";
                        } else if (precision <= 9) {
                            return "INTEGER";
                        } else if (precision <= 18) {
                            return "BIGINT";
                        } else {
                            return "NUMERIC(" + precision + ",0)";
                        }
                    }
                }
                return "NUMERIC";
            } else if (upperSourceType.contains("VARCHAR2") || upperSourceType.contains("NVARCHAR2")) {
                Pattern pattern = Pattern.compile("(N?VARCHAR2)\\((\\d+)(?: (?:BYTE|CHAR))?\\)");
                Matcher matcher = pattern.matcher(upperSourceType);
                if (matcher.find()) {
                    String type = matcher.group(1);
                    int length = Integer.parseInt(matcher.group(2));
                    return (type.startsWith("N") ? "NVARCHAR" : "VARCHAR") + "(" + length + ")";
                }
                return "VARCHAR(255)";
            } else if (upperSourceType.contains("CHAR") || upperSourceType.contains("NCHAR")) {
                Pattern pattern = Pattern.compile("(N?CHAR)\\((\\d+)(?: (?:BYTE|CHAR))?\\)");
                Matcher matcher = pattern.matcher(upperSourceType);
                if (matcher.find()) {
                    String type = matcher.group(1);
                    int length = Integer.parseInt(matcher.group(2));
                    return (type.startsWith("N") ? "NCHAR" : "CHAR") + "(" + length + ")";
                }
                return "CHAR(1)";
            } else if (upperSourceType.contains("CLOB") || upperSourceType.contains("NCLOB")) {
                return "TEXT";
            } else if (upperSourceType.contains("BLOB") || upperSourceType.contains("RAW")) {
                return "BYTEA";
            } else if (upperSourceType.equals("DATE")) {
                return "DATE";
            } else if (upperSourceType.contains("TIMESTAMP")) {
                return "TIMESTAMP";
            }
            
            // 增加对 Oracle 特有类型的支持
            else if (upperSourceType.contains("INTERVAL")) {
                return "INTERVAL";
            }
            else if (upperSourceType.contains("BFILE")) {
                return "TEXT"; // Kingbase 没有直接对应的 BFILE 类型
            }
            else if (upperSourceType.contains("ROWID") || upperSourceType.contains("UROWID")) {
                return "VARCHAR(255)";
            }
        }
        
        // 通用映射表
        switch (upperSourceType) {
            case "VARCHAR":
            case "VARCHAR2":
            case "NVARCHAR":
            case "NVARCHAR2":
            case "CHARACTER VARYING":
                return upperSourceType.replace("VARCHAR2", "VARCHAR").replace("NVARCHAR2", "NVARCHAR").replace("CHARACTER VARYING", "VARCHAR");
            case "CHAR":
            case "NCHAR":
            case "CHARACTER":
                return upperSourceType.replace("CHARACTER", "CHAR");
            case "TEXT":
            case "CLOB":
            case "NCLOB":
            case "LONGTEXT":
            case "MEDIUMTEXT":
            case "TINYTEXT":
                return "TEXT";
            case "INT":
            case "INTEGER":
                return "INTEGER";
            case "SMALLINT":
                return "SMALLINT";
            case "BIGINT":
                return "BIGINT";
            case "FLOAT":
                return "REAL";
            case "DOUBLE":
            case "DOUBLE PRECISION":
                return "DOUBLE PRECISION";
            case "DECIMAL":
            case "NUMERIC":
                return upperSourceType;
            case "BOOLEAN":
            case "BOOL":
                return "BOOLEAN";
            case "DATE":
                return "DATE";
            case "TIME":
                return "TIME";
            case "TIMESTAMP":
            case "DATETIME":
                return "TIMESTAMP";
            case "BLOB":
            case "BINARY":
            case "VARBINARY":
            case "BYTEA":
                return "BYTEA";
            case "JSON":
                return "JSON";
            case "JSONB":
                return "JSONB";
            case "XML":
                return "XML";
            default:
                return "VARCHAR(255)";
        }
    }

    @Override
    public String wrapValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        
        if (value instanceof Date) {
            if (value instanceof java.sql.Date) {
                return "DATE '" + value.toString() + "'";
            } else if (value instanceof java.sql.Time) {
                return "TIME '" + value.toString() + "'";
            } else if (value instanceof java.sql.Timestamp) {
                return "TIMESTAMP '" + value.toString() + "'";
            } else {
                // 普通 java.util.Date，转为 timestamp
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                return "TIMESTAMP '" + sdf.format(value) + "'";
            }
        } else if (value instanceof Boolean) {
            return ((Boolean) value) ? "TRUE" : "FALSE";
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof byte[]) {
            return "E'\\\\x" + bytesToHex((byte[]) value) + "'";
        } else {
            String strValue = value.toString();
            // Kingbase要求字符串使用单引号，需要对内部单引号进行转义
            return "'" + strValue.replace("'", "''") + "'";
        }
    }

    /**
     * 将字节数组转换为十六进制字符串
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(Character.forDigit((b >> 4) & 0xF, 16))
               .append(Character.forDigit((b & 0xF), 16));
        }
        return hex.toString();
    }

    /**
     * 生成范围分区表的创建语句
     * @param tableName 表名
     * @param columns 列定义
     * @param partitionColumn 分区列
     * @param partitions 分区定义，键为分区名称，值为范围条件
     * @return 创建范围分区表的SQL
     */
    public String getCreateRangePartitionTableSql(String tableName, List<ColumnDefinition> columns, 
                                                 String partitionColumn, Map<String, String> partitions) {
        // 首先创建基本表结构
        StringBuilder sql = new StringBuilder();
        TableDefinition tableDefinition = new TableDefinition();
        tableDefinition.setTableName(tableName);
        tableDefinition.setColumns(columns);
        
        String baseTableSql = generateCreateTableSql(tableDefinition);
        
        // 移除结尾的分号
        if (baseTableSql.endsWith(";")) {
            baseTableSql = baseTableSql.substring(0, baseTableSql.length() - 1);
        }
        
        sql.append(baseTableSql);
        
        // 添加分区定义
        sql.append("\nPARTITION BY RANGE(").append(wrapIdentifier(partitionColumn)).append(") (");
        
        List<String> partitionDefs = new ArrayList<>();
        for (Map.Entry<String, String> entry : partitions.entrySet()) {
            String partitionName = entry.getKey();
            String condition = entry.getValue();
            
            if (condition.contains(",")) {
                // 假设条件包含逗号分隔的起始和结束值
                String[] bounds = condition.split(",", 2);
                partitionDefs.add("\n    PARTITION " + wrapIdentifier(partitionName) + 
                                  " FOR VALUES FROM (" + bounds[0].trim() + ") TO (" + bounds[1].trim() + ")");
            } else {
                // 向下兼容旧格式
                partitionDefs.add("\n    PARTITION " + wrapIdentifier(partitionName) + 
                                  " FOR VALUES LESS THAN (" + condition + ")");
            }
        }
        
        sql.append(String.join(",", partitionDefs));
        sql.append("\n)");
        
        return sql.toString();
    }

    /**
     * 生成列表分区表的创建语句
     * @param tableName 表名
     * @param columns 列定义
     * @param partitionColumn 分区列
     * @param partitions 分区定义，键为分区名称，值为列表值
     * @return 创建列表分区表的SQL
     */
    public String getCreateListPartitionTableSql(String tableName, List<ColumnDefinition> columns, 
                                                String partitionColumn, Map<String, List<String>> partitions) {
        // 首先创建基本表结构
        StringBuilder sql = new StringBuilder();
        TableDefinition tableDefinition = new TableDefinition();
        tableDefinition.setTableName(tableName);
        tableDefinition.setColumns(columns);
        
        String baseTableSql = generateCreateTableSql(tableDefinition);
        
        // 移除结尾的分号
        if (baseTableSql.endsWith(";")) {
            baseTableSql = baseTableSql.substring(0, baseTableSql.length() - 1);
        }
        
        sql.append(baseTableSql);
        
        // 添加分区定义
        sql.append("\nPARTITION BY LIST(").append(wrapIdentifier(partitionColumn)).append(") (");
        
        List<String> partitionDefs = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : partitions.entrySet()) {
            String partitionName = entry.getKey();
            List<String> values = entry.getValue();
            
            partitionDefs.add("\n    PARTITION " + wrapIdentifier(partitionName) + 
                              " FOR VALUES IN (" + String.join(", ", values) + ")");
        }
        
        sql.append(String.join(",", partitionDefs));
        sql.append("\n)");
        
        return sql.toString();
    }

    /**
     * 添加外键约束
     * @param tableName 表名
     * @param constraintName 约束名
     * @param columnNames 列名
     * @param refTableName 引用表名
     * @param refColumnNames 引用列名
     * @param onDeleteAction ON DELETE 操作
     * @param onUpdateAction ON UPDATE 操作
     * @return 添加外键约束的SQL
     */
    public String getAddForeignKeySql(String tableName, String constraintName, List<String> columnNames,
                                     String refTableName, List<String> refColumnNames,
                                     String onDeleteAction, String onUpdateAction) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(wrapIdentifier(tableName))
           .append(" ADD CONSTRAINT ").append(wrapIdentifier(constraintName))
           .append(" FOREIGN KEY (");
        
        List<String> wrappedColumns = new ArrayList<>();
        for (String column : columnNames) {
            wrappedColumns.add(wrapIdentifier(column));
        }
        sql.append(String.join(", ", wrappedColumns));
        
        sql.append(") REFERENCES ").append(wrapIdentifier(refTableName)).append(" (");
        
        List<String> wrappedRefColumns = new ArrayList<>();
        for (String column : refColumnNames) {
            wrappedRefColumns.add(wrapIdentifier(column));
        }
        sql.append(String.join(", ", wrappedRefColumns)).append(")");
        
        if (onDeleteAction != null && !onDeleteAction.isEmpty()) {
            sql.append(" ON DELETE ").append(onDeleteAction);
        }
        
        if (onUpdateAction != null && !onUpdateAction.isEmpty()) {
            sql.append(" ON UPDATE ").append(onUpdateAction);
        }
        
        return sql.toString();
    }

    /**
     * 添加检查约束
     * @param tableName 表名
     * @param constraintName 约束名
     * @param checkCondition 检查条件
     * @return 添加检查约束的SQL
     */
    public String getAddCheckConstraintSql(String tableName, String constraintName, String checkCondition) {
        return "ALTER TABLE " + wrapIdentifier(tableName) + 
               " ADD CONSTRAINT " + wrapIdentifier(constraintName) + 
               " CHECK (" + checkCondition + ")";
    }

    /**
     * 创建全文搜索索引
     * @param tableName 表名
     * @param indexName 索引名
     * @param columnName 列名
     * @param config 配置名称
     * @return 创建全文搜索索引的SQL
     */
    public String getCreateFullTextIndexSql(String tableName, String indexName, String columnName, String config) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE INDEX ").append(wrapIdentifier(indexName))
           .append(" ON ").append(wrapIdentifier(tableName))
           .append(" USING gin(to_tsvector('");
        
        if (config != null && !config.isEmpty()) {
            sql.append(config);
        } else {
            sql.append("simple");
        }
        
        sql.append("', ").append(wrapIdentifier(columnName)).append("))");
        
        return sql.toString();
    }

    /**
     * 生成全文搜索查询
     * @param tableName 表名
     * @param columnName 列名
     * @param searchText 搜索文本
     * @param config 配置名称
     * @return 全文搜索查询SQL
     */
    public String getFullTextSearchSql(String tableName, String columnName, String searchText, String config) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ").append(wrapIdentifier(tableName))
           .append(" WHERE to_tsvector('");
        
        if (config != null && !config.isEmpty()) {
            sql.append(config);
        } else {
            sql.append("simple");
        }
        
        sql.append("', ").append(wrapIdentifier(columnName))
           .append(") @@ to_tsquery('");
        
        if (config != null && !config.isEmpty()) {
            sql.append(config);
        } else {
            sql.append("simple");
        }
        
        sql.append("', '").append(searchText.replace("'", "''")).append("')");
        
        return sql.toString();
    }

    /**
     * 创建序列
     * @param sequenceName 序列名
     * @param startValue 起始值
     * @param incrementBy 增量
     * @param minValue 最小值
     * @param maxValue 最大值
     * @param cache 缓存数量
     * @param cycle 是否循环
     * @return 创建序列的SQL
     */
    public String getCreateSequenceSql(String sequenceName, Long startValue, Long incrementBy,
                                      Long minValue, Long maxValue, Integer cache, boolean cycle) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE SEQUENCE ").append(wrapIdentifier(sequenceName));
        
        if (startValue != null) {
            sql.append(" START WITH ").append(startValue);
        }
        
        if (incrementBy != null) {
            sql.append(" INCREMENT BY ").append(incrementBy);
        }
        
        if (minValue != null) {
            sql.append(" MINVALUE ").append(minValue);
        } else {
            sql.append(" NO MINVALUE");
        }
        
        if (maxValue != null) {
            sql.append(" MAXVALUE ").append(maxValue);
        } else {
            sql.append(" NO MAXVALUE");
        }
        
        if (cache != null) {
            sql.append(" CACHE ").append(cache);
        }
        
        if (cycle) {
            sql.append(" CYCLE");
        } else {
            sql.append(" NO CYCLE");
        }
        
        return sql.toString();
    }

    /**
     * 获取序列下一个值
     * @param sequenceName 序列名
     * @return 获取序列下一个值的SQL
     */
    public String getNextSequenceValueSql(String sequenceName) {
        return "SELECT nextval('" + sequenceName + "')";
    }

    /**
     * 获取序列当前值
     * @param sequenceName 序列名
     * @return 获取序列当前值的SQL
     */
    public String getCurrentSequenceValueSql(String sequenceName) {
        return "SELECT currval('" + sequenceName + "')";
    }

    /**
     * 获取包含LIKE查询的SQL，并正确处理转义字符
     * @param tableName 表名
     * @param columnName 列名
     * @param pattern 匹配模式
     * @param caseSensitive 是否区分大小写
     * @return 包含LIKE查询的SQL
     */
    public String getLikeQuerySql(String tableName, String columnName, String pattern, boolean caseSensitive) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ").append(wrapIdentifier(tableName)).append(" WHERE ");
        
        if (!caseSensitive) {
            sql.append("LOWER(").append(wrapIdentifier(columnName)).append(")");
            sql.append(" LIKE LOWER(");
        } else {
            sql.append(wrapIdentifier(columnName)).append(" LIKE ");
        }
        
        // 转义特殊字符
        String escapedPattern = pattern.replace("'", "''").replace("%", "\\%").replace("_", "\\_");
        sql.append("'%").append(escapedPattern).append("%'");
        
        if (!caseSensitive) {
            sql.append(")");
        }
        
        sql.append(" ESCAPE '\\'");
        
        return sql.toString();
    }

    /**
     * 获取查询执行计划
     * @param sql 要分析的SQL
     * @param analyze 是否包含执行统计信息
     * @return 查询执行计划SQL
     */
    public String getExplainSql(String sql, boolean analyze) {
        StringBuilder explainSql = new StringBuilder();
        if (analyze) {
            explainSql.append("EXPLAIN (ANALYZE TRUE, BUFFERS TRUE, FORMAT TEXT) ");
        } else {
            explainSql.append("EXPLAIN FORMAT TEXT ");
        }
        
        explainSql.append(sql);
        
        return explainSql.toString();
    }

    /**
     * 复制表结构
     * @param sourceTable 源表名
     * @param targetTable 目标表名
     * @param includeData 是否包含数据
     * @return 复制表结构的SQL
     */
    public String getCreateTableLikeSql(String sourceTable, String targetTable, boolean includeData) {
        if (includeData) {
            return "CREATE TABLE " + wrapIdentifier(targetTable) + " AS TABLE " + wrapIdentifier(sourceTable);
        } else {
            return "CREATE TABLE " + wrapIdentifier(targetTable) + " (LIKE " + wrapIdentifier(sourceTable) + ")";
        }
    }

    /**
     * 获取数组类型的SQL表示
     * @param baseType 基础类型
     * @param dimensions 维度数
     * @return 数组类型的SQL表示
     */
    public String getArrayTypeSql(String baseType, int dimensions) {
        StringBuilder sql = new StringBuilder(baseType);
        for (int i = 0; i < dimensions; i++) {
            sql.append("[]");
        }
        return sql.toString();
    }

    /**
     * 包装数组值
     * @param arrayValues 数组值列表
     * @return 数组的SQL表示
     */
    public String wrapArrayValue(List<Object> arrayValues) {
        StringBuilder sql = new StringBuilder("ARRAY[");
        List<String> wrappedValues = new ArrayList<>();
        
        for (Object value : arrayValues) {
            wrappedValues.add(wrapValue(value));
        }
        
        sql.append(String.join(", ", wrappedValues));
        sql.append("]");
        
        return sql.toString();
    }

    /**
     * 创建物化视图
     * @param viewName 视图名称
     * @param query 查询语句
     * @param withData 是否包含数据
     * @param refreshMethod 刷新方法 (FAST, COMPLETE, 或 null)
     * @return 创建物化视图的SQL
     */
    public String getCreateMaterializedViewSql(String viewName, String query, boolean withData, String refreshMethod) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE MATERIALIZED VIEW ").append(wrapIdentifier(viewName));
        
        if (refreshMethod != null && !refreshMethod.isEmpty()) {
            sql.append(" WITH (");
            if (refreshMethod.equalsIgnoreCase("FAST")) {
                sql.append("refresh_fast = true");
            } else if (refreshMethod.equalsIgnoreCase("COMPLETE")) {
                sql.append("refresh_complete = true");
            }
            sql.append(")");
        }
        
        sql.append(" AS\n").append(query);
        
        if (!withData) {
            sql.append("\nWITH NO DATA");
        }
        
        return sql.toString();
    }

    /**
     * 刷新物化视图
     * @param viewName 视图名称
     * @param concurrently 是否并发刷新
     * @param withData 是否包含数据
     * @return 刷新物化视图的SQL
     */
    public String getRefreshMaterializedViewSql(String viewName, boolean concurrently, boolean withData) {
        StringBuilder sql = new StringBuilder();
        sql.append("REFRESH MATERIALIZED VIEW ");
        
        if (concurrently) {
            sql.append("CONCURRENTLY ");
        }
        
        sql.append(wrapIdentifier(viewName));
        
        if (!withData) {
            sql.append(" WITH NO DATA");
        }
        
        return sql.toString();
    }

    /**
     * 检查 Kingbase 版本
     * @return Kingbase 版本信息
     */
    public Map<String, Object> getKingbaseVersion() throws Exception {
        Map<String, Object> versionInfo = new HashMap<>();
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT version()")) {
            
            if (rs.next()) {
                String versionString = rs.getString(1);
                versionInfo.put("version_string", versionString);
                
                // 尝试解析版本号
                Pattern pattern = Pattern.compile("\\b(\\d+)\\.(\\d+)(?:\\.(\\d+))?");
                Matcher matcher = pattern.matcher(versionString);
                
                if (matcher.find()) {
                    int majorVersion = Integer.parseInt(matcher.group(1));
                    int minorVersion = Integer.parseInt(matcher.group(2));
                    int patchVersion = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
                    
                    versionInfo.put("major_version", majorVersion);
                    versionInfo.put("minor_version", minorVersion);
                    versionInfo.put("patch_version", patchVersion);
                }
            }
        }
        
        return versionInfo;
    }

    /**
     * 根据 Kingbase 版本生成不同的语法
     * @param majorVersion 主版本号
     * @param minorVersion 次版本号
     * @param feature 特性名称
     * @return 根据版本适配的SQL
     */
    public String getVersionSpecificSql(int majorVersion, int minorVersion, String feature) {
        switch (feature) {
            case "UPSERT":
                // V8.2 及以上版本支持 ON CONFLICT 语法
                if (majorVersion > 8 || (majorVersion == 8 && minorVersion >= 2)) {
                    return "INSERT ... ON CONFLICT DO UPDATE ...";
                } else {
                    // 旧版本使用临时表和多条语句
                    return "使用临时表实现 UPSERT";
                }
            case "JSONB_FUNCTIONS":
                // V8.3 及以上版本支持更多 JSONB 函数
                if (majorVersion > 8 || (majorVersion == 8 && minorVersion >= 3)) {
                    return "使用高级 JSONB 函数";
                } else {
                    return "使用基本 JSON 函数";
                }
            default:
                return null;
        }
    }

    /**
     * 生成批量插入SQL
     * @param tableName 表名
     * @param columns 列名列表
     * @param valuesList 值列表
     * @return 批量插入SQL
     */
    public String getBatchInsertSql(String tableName, List<String> columns, List<List<String>> valuesList) {
        if (valuesList == null || valuesList.isEmpty()) {
            return "";
        }
        
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(wrapIdentifier(tableName)).append(" (");
        
        List<String> wrappedColumns = new ArrayList<>();
        for (String column : columns) {
            wrappedColumns.add(wrapIdentifier(column));
        }
        
        sql.append(String.join(", ", wrappedColumns)).append(") VALUES ");
        
        List<String> valueGroups = new ArrayList<>();
        for (List<String> values : valuesList) {
            valueGroups.add("(" + String.join(", ", values) + ")");
        }
        
        sql.append(String.join(", ", valueGroups));
        
        return sql.toString();
    }

    /**
     * 生成批量UPSERT SQL (ON CONFLICT DO UPDATE)
     * @param tableName 表名
     * @param columns 列名列表
     * @param valuesList 值列表
     * @param conflictColumns 冲突列
     * @param updateColumns 更新列
     * @return 批量UPSERT SQL
     */
    public String getBatchUpsertSql(String tableName, List<String> columns, List<List<String>> valuesList,
                                   List<String> conflictColumns, List<String> updateColumns) {
        if (valuesList == null || valuesList.isEmpty()) {
            return "";
        }
        
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(wrapIdentifier(tableName)).append(" (");
        
        List<String> wrappedColumns = new ArrayList<>();
        for (String column : columns) {
            wrappedColumns.add(wrapIdentifier(column));
        }
        
        sql.append(String.join(", ", wrappedColumns)).append(") VALUES ");
        
        List<String> valueGroups = new ArrayList<>();
        for (List<String> values : valuesList) {
            valueGroups.add("(" + String.join(", ", values) + ")");
        }
        
        sql.append(String.join(", ", valueGroups));
        
        // 添加ON CONFLICT子句
        sql.append(" ON CONFLICT (");
        
        List<String> wrappedConflictColumns = new ArrayList<>();
        for (String column : conflictColumns) {
            wrappedConflictColumns.add(wrapIdentifier(column));
        }
        
        sql.append(String.join(", ", wrappedConflictColumns)).append(") DO UPDATE SET ");
        
        List<String> updateExpressions = new ArrayList<>();
        for (String column : updateColumns) {
            updateExpressions.add(wrapIdentifier(column) + " = EXCLUDED." + wrapIdentifier(column));
        }
        
        sql.append(String.join(", ", updateExpressions));
        
        return sql.toString();
    }

    /**
     * 获取数据库备份命令
     * @param dbName 数据库名
     * @param fileName 备份文件名
     * @param format 备份格式 (c: 自定义, p: 纯文本, t: tar)
     * @return 数据库备份命令
     */
    public String getBackupCommand(String dbName, String fileName, String format) {
        return "pg_dump -F " + format + " -f " + fileName + " " + dbName;
    }

    /**
     * 获取数据库恢复命令
     * @param dbName 数据库名
     * @param fileName 备份文件名
     * @param format 备份格式 (c: 自定义, p: 纯文本, t: tar)
     * @return 数据库恢复命令
     */
    public String getRestoreCommand(String dbName, String fileName, String format) {
        if ("c".equals(format)) {
            return "pg_restore -d " + dbName + " " + fileName;
        } else if ("p".equals(format)) {
            return "psql -d " + dbName + " -f " + fileName;
        } else {
            return "pg_restore -d " + dbName + " " + fileName;
        }
    }

    /**
     * 生成授权语句
     * @param objectType 对象类型 (TABLE, SEQUENCE, FUNCTION 等)
     * @param objectName 对象名称
     * @param privileges 权限列表
     * @param grantee 被授权者
     * @param withGrantOption 是否可以授权给其他用户
     * @return 授权语句SQL
     */
    public String getGrantSql(String objectType, String objectName, List<String> privileges,
                             String grantee, boolean withGrantOption) {
        // 验证 objectType 是 Kingbase 支持的类型
        String validObjectType = objectType.toUpperCase();
        if (!Arrays.asList("TABLE", "SEQUENCE", "FUNCTION", "PROCEDURE", "DATABASE", "SCHEMA").contains(validObjectType)) {
            validObjectType = "TABLE"; // 默认为表
        }
        
        StringBuilder sql = new StringBuilder();
        sql.append("GRANT ");
        
        if (privileges != null && !privileges.isEmpty()) {
            sql.append(String.join(", ", privileges));
        } else {
            sql.append("ALL");
        }
        
        sql.append(" ON ").append(validObjectType).append(" ");
        sql.append(wrapIdentifier(objectName));
        sql.append(" TO ").append(wrapIdentifier(grantee));
        
        if (withGrantOption) {
            sql.append(" WITH GRANT OPTION");
        }
        
        return sql.toString();
    }

    /**
     * 生成撤销权限语句
     * @param objectType 对象类型 (TABLE, SEQUENCE, FUNCTION 等)
     * @param objectName 对象名称
     * @param privileges 权限列表
     * @param grantee 被撤销权限者
     * @param cascade 是否级联撤销
     * @return 撤销权限语句SQL
     */
    public String getRevokeSql(String objectType, String objectName, List<String> privileges,
                              String grantee, boolean cascade) {
        StringBuilder sql = new StringBuilder();
        sql.append("REVOKE ");
        
        if (privileges != null && !privileges.isEmpty()) {
            sql.append(String.join(", ", privileges));
        } else {
            sql.append("ALL");
        }
        
        sql.append(" ON ").append(objectType).append(" ");
        sql.append(wrapIdentifier(objectName));
        sql.append(" FROM ").append(wrapIdentifier(grantee));
        
        if (cascade) {
            sql.append(" CASCADE");
        }
        
        return sql.toString();
    }

    /**
     * 获取连接池配置建议
     * @return 连接池配置建议
     */
    public Map<String, Object> getConnectionPoolRecommendations() {
        Map<String, Object> recommendations = new HashMap<>();
        
        recommendations.put("initialSize", 5);
        recommendations.put("minIdle", 5);
        recommendations.put("maxActive", 20);
        recommendations.put("maxWait", 60000);
        recommendations.put("timeBetweenEvictionRunsMillis", 60000);
        recommendations.put("minEvictableIdleTimeMillis", 300000);
        recommendations.put("validationQuery", "SELECT 1");
        recommendations.put("testWhileIdle", true);
        recommendations.put("testOnBorrow", false);
        recommendations.put("testOnReturn", false);
        recommendations.put("poolPreparedStatements", true);
        recommendations.put("maxPoolPreparedStatementPerConnectionSize", 20);
        
        return recommendations;
    }

    /**
     * 高级健康检查，包括连接池和数据库状态
     * @return 健康检查结果
     */
    public Map<String, Object> performAdvancedHealthCheck() throws Exception {
        Map<String, Object> healthStatus = new HashMap<>();
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // 基本连接检查
            boolean connected = conn != null && !conn.isClosed();
            healthStatus.put("connected", connected);
            
            if (connected) {
                // 数据库版本信息
                try (ResultSet rs = stmt.executeQuery("SELECT version()")) {
                    if (rs.next()) {
                        healthStatus.put("version", rs.getString(1));
                    }
                }
                
                // 数据库负载信息
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT count(*) as connections, " +
                        "(SELECT count(*) FROM pg_stat_activity WHERE state = 'active') as active_connections, " +
                        "(SELECT count(*) FROM pg_stat_activity WHERE waiting) as waiting_connections " +
                        "FROM pg_stat_activity")) {
                    if (rs.next()) {
                        healthStatus.put("total_connections", rs.getInt("connections"));
                        healthStatus.put("active_connections", rs.getInt("active_connections"));
                        healthStatus.put("waiting_connections", rs.getInt("waiting_connections"));
                    }
                }
                
                // 事务日志状态
                try {
                    String walSizeSql;
                    // 尝试使用兼容不同版本的查询
                    try {
                        Connection testConn = getConnection();
                        try (Statement testStmt = testConn.createStatement();
                             ResultSet testRs = testStmt.executeQuery(
                                 "SELECT 1 FROM pg_proc WHERE proname = 'pg_current_wal_lsn' LIMIT 1")) {
                            if (testRs.next()) {
                                // 新版本函数
                                walSizeSql = "SELECT pg_size_pretty(pg_current_wal_lsn() - '0/0'::pg_lsn) as current_wal_size";
                            } else {
                                // 旧版本函数
                                walSizeSql = "SELECT pg_size_pretty(pg_current_xlog_location() - '0/0'::pg_lsn) as current_wal_size";
                            }
                        }
                        testConn.close();
                    } catch (Exception e) {
                        // 默认使用旧版本函数
                        walSizeSql = "SELECT pg_size_pretty(pg_current_xlog_location() - '0/0'::pg_lsn) as current_wal_size";
                    }
                    
                    try (ResultSet rs = stmt.executeQuery(walSizeSql)) {
                        if (rs.next()) {
                            healthStatus.put("current_wal_size", rs.getString("current_wal_size"));
                        }
                    }
                } catch (Exception e) {
                    // 如果WAL查询失败，记录错误
                    healthStatus.put("wal_error", e.getMessage());
                }
                
                // 连接池统计 - 移除依赖 getConnectionPool() 的部分
                // 如果父类或项目提供了其他获取连接池信息的方法，可以使用替代方法
                healthStatus.put("connection_pool_info", "Connection pool information not available");
            }
        } catch (Exception e) {
            healthStatus.put("connected", false);
            healthStatus.put("error", e.getMessage());
        }
        
        return healthStatus;
    }

    /**
     * 执行批量更新操作，针对Kingbase优化
     * @param sql SQL语句模板
     * @param batchParams 批处理参数列表
     * @param batchSize 每批大小
     * @return 受影响的行数数组
     */
    public int[] executeBatchUpdate(String sql, List<Object[]> batchParams, int batchSize) throws Exception {
        if (batchParams == null || batchParams.isEmpty()) {
            return new int[0];
        }
        
        List<int[]> results = new ArrayList<>();
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                int count = 0;
                
                for (Object[] params : batchParams) {
                    for (int i = 0; i < params.length; i++) {
                        pstmt.setObject(i + 1, params[i]);
                    }
                    pstmt.addBatch();
                    
                    if (++count % batchSize == 0) {
                        results.add(pstmt.executeBatch());
                        pstmt.clearBatch();
                        // 部分提交事务以减少锁定时间
                        conn.commit();
                    }
                }
                
                if (count % batchSize != 0) {
                    results.add(pstmt.executeBatch());
                }
                
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        
        // 合并结果
        int totalLength = 0;
        for (int[] result : results) {
            totalLength += result.length;
        }
        
        int[] finalResult = new int[totalLength];
        int destPos = 0;
        
        for (int[] result : results) {
            System.arraycopy(result, 0, finalResult, destPos, result.length);
            destPos += result.length;
        }
        
        return finalResult;
    }

    /**
     * 优化的大批量插入方法，使用COPY命令
     * @param tableName 表名
     * @param columns 列名列表
     * @param data 数据列表
     * @return 插入的行数
     */
    public int bulkInsertWithCopy(String tableName, List<String> columns, List<List<Object>> data) throws Exception {
        StringBuilder copyCommand = new StringBuilder();
        copyCommand.append("COPY ").append(wrapIdentifier(tableName)).append(" (");
        copyCommand.append(columns.stream().map(this::wrapIdentifier).collect(Collectors.joining(", ")));
        copyCommand.append(") FROM STDIN WITH (FORMAT CSV, DELIMITER ',', QUOTE '\"', ENCODING 'UTF8')");
        
        int rowCount = 0;
        try (Connection conn = getConnection()) {
            CopyManager copyManager = new CopyManager((BaseConnection) conn);
            
            // 将数据转换为CSV格式
            StringBuilder csvData = new StringBuilder();
            for (List<Object> row : data) {
                List<String> formattedValues = new ArrayList<>();
                for (Object value : row) {
                    formattedValues.add(formatCsvValue(value));
                }
                csvData.append(String.join(",", formattedValues)).append("\n");
                rowCount++;
            }
            
            ByteArrayInputStream inputStream = new ByteArrayInputStream(csvData.toString().getBytes());
            copyManager.copyIn(copyCommand.toString(), inputStream);
        }
        
        return rowCount;
    }

    /**
     * 为CSV格式化值
     * @param value 原始值
     * @return 格式化后的CSV值
     */
    private String formatCsvValue(Object value) {
        if (value == null) {
            return "";
        }
        
        String stringValue;
        if (value instanceof Date) {
            if (value instanceof java.sql.Date) {
                stringValue = value.toString();
            } else if (value instanceof java.sql.Timestamp) {
                stringValue = value.toString();
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                stringValue = sdf.format(value);
            }
        } else {
            stringValue = value.toString();
        }
        
        // 转义双引号并用双引号包围
        return "\"" + stringValue.replace("\"", "\"\"") + "\"";
    }

    /**
     * 获取自动清理配置建议
     * @return 自动清理配置建议
     */
    public Map<String, String> getVacuumRecommendations() {
        Map<String, String> recommendations = new HashMap<>();
        recommendations.put("autovacuum", "on");
        recommendations.put("autovacuum_vacuum_threshold", "50");
        recommendations.put("autovacuum_analyze_threshold", "50");
        recommendations.put("autovacuum_vacuum_scale_factor", "0.2");
        recommendations.put("autovacuum_analyze_scale_factor", "0.1");
        recommendations.put("autovacuum_max_workers", "3");
        recommendations.put("autovacuum_vacuum_cost_delay", "20ms");
        recommendations.put("maintenance_work_mem", "64MB");
        
        return recommendations;
    }

    /**
     * 获取表的自动清理状态
     * @param tableName 表名
     * @return 表的自动清理状态SQL
     */
    public String getTableVacuumStatusSql(String tableName) {
        return "SELECT relname as table_name, " +
               "last_vacuum, last_autovacuum, " +
               "last_analyze, last_autoanalyze, " +
               "n_dead_tup as dead_tuples, " +
               "round(n_dead_tup / GREATEST(n_live_tup, 1) * 100, 2) as dead_tuple_ratio, " +
               "n_live_tup as live_tuples " +
               "FROM pg_stat_user_tables " +
               "WHERE relname = '" + tableName.replace("'", "''") + "'";
    }

    /**
     * 获取查询执行计划并分析
     * @param sql 查询SQL
     * @return 查询执行计划分析结果
     */
    public Map<String, Object> analyzeQueryPlan(String sql) throws Exception {
        Map<String, Object> analysis = new HashMap<>();
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // 执行EXPLAIN ANALYZE
            String explainSql = "EXPLAIN ANALYZE " + sql;
            try (ResultSet rs = stmt.executeQuery(explainSql)) {
                StringBuilder planBuilder = new StringBuilder();
                while (rs.next()) {
                    planBuilder.append(rs.getString(1)).append("\n");
                }
                String plan = planBuilder.toString();
                analysis.put("full_plan", plan);
                
                // 简单解析关键信息
                if (plan.contains("Seq Scan")) {
                    analysis.put("plan_type", "Sequential Scan");
                } else if (plan.contains("Index Scan")) {
                    analysis.put("plan_type", "Index Scan");
                }
                
                // 提取执行时间 (这里简化处理)
                Pattern planningPattern = Pattern.compile("Planning time: ([\\d\\.]+) ms");
                Matcher planningMatcher = planningPattern.matcher(plan);
                if (planningMatcher.find()) {
                    analysis.put("planning_time", Double.parseDouble(planningMatcher.group(1)));
                }
                
                Pattern executionPattern = Pattern.compile("Execution time: ([\\d\\.]+) ms");
                Matcher executionMatcher = executionPattern.matcher(plan);
                if (executionMatcher.find()) {
                    analysis.put("execution_time", Double.parseDouble(executionMatcher.group(1)));
                }
            }
        }
        
        return analysis;
    }

    /**
     * 创建表分区，支持子分区
     * @param tableName 表名
     * @param partitionType 分区类型 (RANGE, LIST, HASH)
     * @param partitionColumns 分区列
     * @param partitionSpecs 分区规范
     * @param subpartitionType 子分区类型
     * @param subpartitionColumns 子分区列
     * @param subpartitionTemplates 子分区模板
     * @return 创建分区表的SQL
     */
    public String getCreatePartitionedTableWithSubpartitionSql(String tableName,
                                                             String partitionType,
                                                             List<String> partitionColumns,
                                                             Map<String, Object> partitionSpecs,
                                                             String subpartitionType,
                                                             List<String> subpartitionColumns,
                                                             Map<String, Object> subpartitionTemplates) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(wrapIdentifier(tableName)).append(" (");
        
        // 列定义部分（需实际表结构）
        sql.append("\n  id INTEGER,");
        sql.append("\n  data TEXT,");
        sql.append("\n  created_at TIMESTAMP");
        sql.append("\n) PARTITION BY ").append(partitionType).append(" (");
        
        // 分区列
        sql.append(partitionColumns.stream().map(this::wrapIdentifier).collect(Collectors.joining(", ")));
        sql.append(")");
        
        // 主分区定义
        if (partitionSpecs != null && !partitionSpecs.isEmpty()) {
            sql.append("\n(");
            List<String> partitions = new ArrayList<>();
            
            for (Map.Entry<String, Object> entry : partitionSpecs.entrySet()) {
                StringBuilder partitionDef = new StringBuilder();
                String partitionName = entry.getKey();
                Object partitionValue = entry.getValue();
                
                partitionDef.append("\n  PARTITION ").append(wrapIdentifier(partitionName)).append(" VALUES ");
                
                if (partitionType.equalsIgnoreCase("RANGE")) {
                    partitionDef.append("FROM (").append(partitionValue).append(") TO (").append(partitionValue).append(")");
                } else if (partitionType.equalsIgnoreCase("LIST")) {
                    partitionDef.append("IN (").append(partitionValue).append(")");
                }
                
                // 子分区
                if (subpartitionType != null && !subpartitionType.isEmpty() &&
                    subpartitionColumns != null && !subpartitionColumns.isEmpty()) {
                    
                    partitionDef.append("\n    PARTITION BY ").append(subpartitionType).append(" (");
                    partitionDef.append(subpartitionColumns.stream().map(this::wrapIdentifier).collect(Collectors.joining(", ")));
                    partitionDef.append(")");
                    
                    if (subpartitionTemplates != null && !subpartitionTemplates.isEmpty()) {
                        partitionDef.append("\n    (");
                        List<String> subpartitions = new ArrayList<>();
                        
                        for (Map.Entry<String, Object> subEntry : subpartitionTemplates.entrySet()) {
                            StringBuilder subpartitionDef = new StringBuilder();
                            String subpartitionName = subEntry.getKey();
                            Object subpartitionValue = subEntry.getValue();
                            
                            subpartitionDef.append("\n      PARTITION ").append(wrapIdentifier(subpartitionName)).append(" VALUES ");
                            
                            if (subpartitionType.equalsIgnoreCase("RANGE")) {
                                subpartitionDef.append("FROM (").append(subpartitionValue).append(") TO (").append(subpartitionValue).append(")");
                            } else if (subpartitionType.equalsIgnoreCase("LIST")) {
                                subpartitionDef.append("IN (").append(subpartitionValue).append(")");
                            }
                            
                            subpartitions.add(subpartitionDef.toString());
                        }
                        
                        partitionDef.append(String.join(",", subpartitions));
                        partitionDef.append("\n    )");
                    }
                }
                
                partitions.add(partitionDef.toString());
            }
            
            sql.append(String.join(",", partitions));
            sql.append("\n)");
        }
        
        return sql.toString();
    }

    /**
     * 创建部分索引
     * @param tableName 表名
     * @param indexName 索引名
     * @param columns 列名
     * @param whereClause WHERE条件
     * @param indexType 索引类型 (btree, hash, gist, gin)
     * @return 创建部分索引的SQL
     */
    public String getCreatePartialIndexSql(String tableName, String indexName, List<String> columns,
                                         String whereClause, String indexType) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE INDEX ").append(wrapIdentifier(indexName))
           .append(" ON ").append(wrapIdentifier(tableName));
        
        if (indexType != null && !indexType.isEmpty()) {
            sql.append(" USING ").append(indexType);
        }
        
        sql.append(" (");
        sql.append(columns.stream().map(this::wrapIdentifier).collect(Collectors.joining(", ")));
        sql.append(")");
        
        if (whereClause != null && !whereClause.isEmpty()) {
            sql.append(" WHERE ").append(whereClause);
        }
        
        return sql.toString();
    }

    /**
     * 创建函数索引
     * @param tableName 表名
     * @param indexName 索引名
     * @param functionExpression 函数表达式
     * @param indexType 索引类型
     * @return 创建函数索引的SQL
     */
    public String getCreateFunctionIndexSql(String tableName, String indexName, 
                                           String functionExpression, String indexType) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE INDEX ").append(wrapIdentifier(indexName))
           .append(" ON ").append(wrapIdentifier(tableName));
        
        if (indexType != null && !indexType.isEmpty()) {
            sql.append(" USING ").append(indexType);
        }
        
        sql.append(" (").append(functionExpression).append(")");
        
        return sql.toString();
    }

    /**
     * 获取数据库支持的字符集列表
     * @return 获取字符集列表的SQL
     */
    public String getCharacterSetsSql() {
        return "SELECT name, description FROM pg_encoding_to_char(pg_char_to_encoding(pg_database.encoding)) AS name, " +
               "pg_database.encoding AS encoding, " +
               "pg_encoding_max_length(pg_database.encoding) AS max_length, " +
               "pg_database.datcollate AS collate, " +
               "pg_database.datctype AS ctype " +
               "FROM pg_database " +
               "WHERE datname = current_database()";
    }

    /**
     * 获取数据库支持的排序规则列表
     * @return 获取排序规则列表的SQL
     */
    public String getCollationsSql() {
        return "SELECT collname AS name, " +
               "collcollate AS collate, " +
               "collctype AS ctype " +
               "FROM pg_collation " +
               "ORDER BY collname";
    }

    /**
     * 创建自定义排序规则
     * @param collationName 排序规则名称
     * @param locale 区域设置
     * @param provider 提供者
     * @param deterministic 是否确定性的
     * @return 创建自定义排序规则的SQL
     */
    public String getCreateCollationSql(String collationName, String locale, 
                                       String provider, boolean deterministic) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE COLLATION ").append(wrapIdentifier(collationName)).append(" (");
        
        List<String> options = new ArrayList<>();
        options.add("locale = '" + locale.replace("'", "''") + "'");
        
        if (provider != null && !provider.isEmpty()) {
            options.add("provider = '" + provider.replace("'", "''") + "'");
        }
        
        options.add("deterministic = " + (deterministic ? "true" : "false"));
        
        sql.append(String.join(", ", options));
        sql.append(")");
        
        return sql.toString();
    }

    /**
     * 获取数据库性能统计
     * @return 数据库性能统计SQL
     */
    public String getDatabaseStatsSql() {
        return "SELECT * FROM pg_stat_database WHERE datname = current_database()";
    }

    /**
     * 获取索引使用情况统计
     * @return 索引使用情况统计SQL
     */
    public String getIndexUsageStatsSql() {
        return "SELECT " +
               "s.schemaname AS schema_name, " +
               "s.relname AS table_name, " +
               "s.indexrelname AS index_name, " +
               "s.idx_scan AS index_scans, " +
               "s.idx_tup_read AS tuples_read, " +
               "s.idx_tup_fetch AS tuples_fetched, " +
               "pg_size_pretty(pg_relation_size(idx.indexrelid)) AS index_size " +
               "FROM pg_stat_user_indexes s " +
               "JOIN pg_index idx ON s.indexrelid = idx.indexrelid " +
               "ORDER BY s.idx_scan DESC";
    }

    /**
     * 获取缓存命中率统计
     * @return 缓存命中率统计SQL
     */
    public String getCacheHitRatioSql() {
        return "SELECT " +
               "sum(heap_blks_read) AS heap_read, " +
               "sum(heap_blks_hit) AS heap_hit, " +
               "sum(heap_blks_hit) / (sum(heap_blks_hit) + sum(heap_blks_read)) AS ratio " +
               "FROM pg_statio_user_tables";
    }

    /**
     * 获取长时间运行的查询
     * @param seconds 运行时间阈值（秒）
     * @return 获取长时间运行查询的SQL
     */
    public String getLongRunningQueriesSql(int seconds) {
        return "SELECT pid, datname, usename, application_name, client_addr, " +
               "state, age(clock_timestamp(), query_start) AS duration, query " +
               "FROM pg_stat_activity " +
               "WHERE state != 'idle' AND query != current_query() " +
               "AND age(clock_timestamp(), query_start) > interval '" + seconds + " second' " +
               "ORDER BY duration DESC";
    }

    /**
     * 获取表膨胀情况
     * @return 表膨胀情况SQL
     */
    public String getTableBloatSql() {
        return "SELECT " +
               "schemaname, " +
               "relname AS tablename, " +
               "pg_size_pretty(pg_relation_size(schemaname || '.' || relname)) AS table_size, " +
               "pg_size_pretty(pg_total_relation_size(schemaname || '.' || relname) - pg_relation_size(schemaname || '.' || relname)) AS index_size, " +
               "pg_size_pretty(pg_total_relation_size(schemaname || '.' || relname)) AS total_size, " +
               "n_live_tup AS live_tuples, " +
               "n_dead_tup AS dead_tuples, " +
               "CASE WHEN n_live_tup + n_dead_tup > 0 THEN " +
               "  round(100 * n_dead_tup::numeric / (n_live_tup + n_dead_tup)) " +
               "ELSE 0 END AS bloat_ratio " +
               "FROM pg_stat_user_tables " +
               "ORDER BY bloat_ratio DESC";
    }

    /**
     * 获取锁定情况
     * @return 锁定情况SQL
     */
    public String getLocksSql() {
        return "SELECT pl.pid, " +
               "locktype, " +
               "CASE WHEN pl.relation IS NOT NULL THEN " +
               "  (SELECT c.relname FROM pg_class c WHERE c.oid = pl.relation) " +
               "ELSE NULL END AS relation, " +
               "mode, " +
               "page, " +
               "tuple, " +
               "virtualxid, " +
               "transactionid, " +
               "granted, " +
               "age(clock_timestamp(), a.query_start) AS query_age, " +
               "a.query " +
               "FROM pg_locks pl " +
               "LEFT JOIN pg_stat_activity a ON pl.pid = a.pid " +
               "ORDER BY age(clock_timestamp(), a.query_start) DESC";
    }

    /**
     * 获取阻塞的查询
     * @return 阻塞的查询SQL
     */
    public String getBlockingQueriesSql() {
        return "SELECT blocked_locks.pid AS blocked_pid, " +
               "blocked_activity.usename AS blocked_user, " +
               "blocking_locks.pid AS blocking_pid, " +
               "blocking_activity.usename AS blocking_user, " +
               "blocked_activity.query AS blocked_query, " +
               "blocking_activity.query AS blocking_query, " +
               "age(now(), blocked_activity.query_start) AS blocked_duration, " +
               "age(now(), blocking_activity.query_start) AS blocking_duration " +
               "FROM pg_catalog.pg_locks blocked_locks " +
               "JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid " +
               "JOIN pg_catalog.pg_locks blocking_locks " +
               "ON blocking_locks.locktype = blocked_locks.locktype " +
               "AND blocking_locks.database IS NOT DISTINCT FROM blocked_locks.database " +
               "AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation " +
               "AND blocking_locks.page IS NOT DISTINCT FROM blocked_locks.page " +
               "AND blocking_locks.tuple IS NOT DISTINCT FROM blocked_locks.tuple " +
               "AND blocking_locks.virtualxid IS NOT DISTINCT FROM blocked_locks.virtualxid " +
               "AND blocking_locks.transactionid IS NOT DISTINCT FROM blocked_locks.transactionid " +
               "AND blocking_locks.classid IS NOT DISTINCT FROM blocked_locks.classid " +
               "AND blocking_locks.objid IS NOT DISTINCT FROM blocked_locks.objid " +
               "AND blocking_locks.objsubid IS NOT DISTINCT FROM blocked_locks.objsubid " +
               "AND blocking_locks.pid != blocked_locks.pid " +
               "JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid " +
               "WHERE NOT blocked_locks.granted";
    }

    /**
     * 取消指定PID的查询
     * @param pid 进程ID
     * @return 取消查询的SQL
     */
    public String getCancelQuerySql(int pid) {
        return "SELECT pg_cancel_backend(" + pid + ")";
    }

    /**
     * 终止指定PID的连接
     * @param pid 进程ID
     * @return 终止连接的SQL
     */
    public String getTerminateConnectionSql(int pid) {
        return "SELECT pg_terminate_backend(" + pid + ")";
    }


} 