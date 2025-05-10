package com.mango.test.constant;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DatabaseKeywords {
    private static final Set<String> MYSQL_KEYWORDS = new HashSet<>();
    private static final Set<String> ORACLE_KEYWORDS = new HashSet<>();
    private static final Set<String> POSTGRESQL_KEYWORDS = new HashSet<>();
    private static final Set<String> SQLSERVER_KEYWORDS = new HashSet<>();
    private static final Set<String> DB2_KEYWORDS = new HashSet<>();
    private static final Set<String> HIVE_KEYWORDS = new HashSet<>();
    private static final Set<String> CLICKHOUSE_KEYWORDS = new HashSet<>();
    private static final Set<String> DORIS_KEYWORDS = new HashSet<>();
    private static final Set<String> DM_KEYWORDS = new HashSet<>();
    private static final Set<String> SHENTONG_KEYWORDS = new HashSet<>();
    private static final Set<String> GBASE_KEYWORDS = new HashSet<>();

    static {
        // MySQL关键字
        MYSQL_KEYWORDS.addAll(Arrays.asList(
            "ADD", "ALL", "ALTER", "ANALYZE", "AND", "AS", "ASC", "AUTO_INCREMENT",
            "BEFORE", "BETWEEN", "BINARY", "BLOB", "BOTH", "BY", "CALL", "CASCADE",
            "CASE", "CHANGE", "CHARACTER", "CHECK", "COLLATE", "COLUMN", "CONDITION",
            "CONSTRAINT", "CONTINUE", "CONVERT", "CREATE", "CROSS", "CURRENT_DATE",
            "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER", "CURSOR", "DATABASE",
            "DATABASES", "DAY_HOUR", "DAY_MICROSECOND", "DAY_MINUTE", "DAY_SECOND",
            "DEC", "DECIMAL", "DECLARE", "DEFAULT", "DELAYED", "DELETE", "DESC",
            "DESCRIBE", "DETERMINISTIC", "DISTINCT", "DISTINCTROW", "DIV", "DOUBLE",
            "DROP", "DUAL", "EACH", "ELSE", "ELSEIF", "ENCLOSED", "ESCAPED",
            "EXISTS", "EXIT", "EXPLAIN", "FALSE", "FETCH", "FLOAT", "FLOAT4",
            "FLOAT8", "FOR", "FORCE", "FOREIGN", "FROM", "FULLTEXT", "GRANT",
            "GROUP", "HAVING", "HIGH_PRIORITY", "HOUR_MICROSECOND", "HOUR_MINUTE",
            "HOUR_SECOND", "IF", "IGNORE", "IN", "INDEX", "INFILE", "INNER",
            "INOUT", "INSENSITIVE", "INSERT", "INT", "INT1", "INT2", "INT3", "INT4",
            "INT8", "INTEGER", "INTERVAL", "INTO", "IS", "ITERATE", "JOIN", "KEY",
            "KEYS", "KILL", "LEADING", "LEAVE", "LEFT", "LIKE", "LIMIT", "LINEAR",
            "LINES", "LOAD", "LOCALTIME", "LOCALTIMESTAMP", "LOCK", "LONG",
            "LONGBLOB", "LONGTEXT", "LOOP", "LOW_PRIORITY", "MATCH", "MEDIUMBLOB",
            "MEDIUMINT", "MEDIUMTEXT", "MIDDLEINT", "MINUTE_MICROSECOND",
            "MINUTE_SECOND", "MOD", "MODIFIES", "NATURAL", "NOT", "NO_WRITE_TO_BINLOG",
            "NULL", "NUMERIC", "ON", "OPTIMIZE", "OPTION", "OPTIONALLY", "OR",
            "ORDER", "OUT", "OUTER", "OUTFILE", "PRECISION", "PRIMARY", "PROCEDURE",
            "PURGE", "RANGE", "READ", "READS", "READ_WRITE", "REAL", "REFERENCES",
            "REGEXP", "RELEASE", "RENAME", "REPEAT", "REPLACE", "REQUIRE", "RESTRICT",
            "RETURN", "REVOKE", "RIGHT", "RLIKE", "SCHEMA", "SCHEMAS", "SECOND_MICROSECOND",
            "SELECT", "SENSITIVE", "SEPARATOR", "SET", "SHOW", "SMALLINT", "SPATIAL",
            "SPECIFIC", "SQL", "SQLEXCEPTION", "SQLSTATE", "SQLWARNING", "SQL_BIG_RESULT",
            "SQL_CALC_FOUND_ROWS", "SQL_SMALL_RESULT", "SSL", "STARTING", "STRAIGHT_JOIN",
            "TABLE", "TERMINATED", "THEN", "TINYBLOB", "TINYINT", "TINYTEXT", "TO",
            "TRAILING", "TRIGGER", "TRUE", "UNDO", "UNION", "UNIQUE", "UNLOCK",
            "UNSIGNED", "UPDATE", "USAGE", "USE", "USING", "UTC_DATE", "UTC_TIME",
            "UTC_TIMESTAMP", "VALUES", "VARBINARY", "VARCHAR", "VARCHARACTER",
            "VARYING", "WHEN", "WHERE", "WHILE", "WITH", "WRITE", "XOR", "YEAR_MONTH",
            "ZEROFILL"
        ));

        // Oracle关键字
        ORACLE_KEYWORDS.addAll(Arrays.asList(
            "ACCESS", "ADD", "ALL", "ALTER", "AND", "ANY", "AS", "ASC", "AUDIT",
            "BETWEEN", "BY", "CHAR", "CHECK", "CLUSTER", "COLUMN", "COMMENT",
            "COMPRESS", "CONNECT", "CREATE", "CURRENT", "DATE", "DECIMAL", "DEFAULT",
            "DELETE", "DESC", "DISTINCT", "DROP", "ELSE", "EXCLUSIVE", "EXISTS",
            "FILE", "FLOAT", "FOR", "FROM", "GRANT", "GROUP", "HAVING", "IDENTIFIED",
            "IMMEDIATE", "IN", "INCREMENT", "INDEX", "INITIAL", "INSERT", "INTEGER",
            "INTERSECT", "INTO", "IS", "LEVEL", "LIKE", "LOCK", "LONG", "MAXEXTENTS",
            "MINUS", "MLSLABEL", "MODE", "MODIFY", "NOAUDIT", "NOCOMPRESS", "NOT",
            "NOWAIT", "NULL", "NUMBER", "OF", "OFFLINE", "ON", "ONLINE", "OPTION",
            "OR", "ORDER", "PCTFREE", "PRIOR", "PRIVILEGES", "PUBLIC", "RAW",
            "RENAME", "RESOURCE", "REVOKE", "ROW", "ROWID", "ROWNUM", "ROWS",
            "SELECT", "SESSION", "SET", "SHARE", "SIZE", "SMALLINT", "START",
            "SUCCESSFUL", "SYNONYM", "SYSDATE", "TABLE", "THEN", "TO", "TRIGGER",
            "UID", "UNION", "UNIQUE", "UPDATE", "USER", "VALIDATE", "VALUES",
            "VARCHAR", "VARCHAR2", "VIEW", "WHENEVER", "WHERE", "WITH"
        ));

        // PostgreSQL关键字
        POSTGRESQL_KEYWORDS.addAll(Arrays.asList(
            "ALL", "ANALYSE", "ANALYZE", "AND", "ANY", "ARRAY", "AS", "ASC",
            "ASYMMETRIC", "AUTHORIZATION", "BINARY", "BOTH", "CASE", "CAST", "CHECK",
            "COLLATE", "COLUMN", "CONSTRAINT", "CREATE", "CROSS", "CURRENT_DATE",
            "CURRENT_ROLE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER",
            "DEFAULT", "DEFERRABLE", "DESC", "DISTINCT", "DO", "ELSE", "END",
            "EXCEPT", "FALSE", "FOR", "FOREIGN", "FREEZE", "FROM", "FULL", "GRANT",
            "GROUP", "HAVING", "ILIKE", "IN", "INITIALLY", "INNER", "INTERSECT",
            "INTO", "IS", "ISNULL", "JOIN", "LEADING", "LEFT", "LIKE", "LIMIT",
            "LOCALTIME", "LOCALTIMESTAMP", "NATURAL", "NEW", "NOT", "NOTNULL",
            "NULL", "OFF", "OFFSET", "OLD", "ON", "ONLY", "OR", "ORDER", "OUTER",
            "OVERLAPS", "PLACING", "PRIMARY", "REFERENCES", "RIGHT", "SELECT",
            "SESSION_USER", "SIMILAR", "SOME", "SYMMETRIC", "TABLE", "THEN", "TO",
            "TRAILING", "TRUE", "UNION", "UNIQUE", "USER", "USING", "VERBOSE",
            "WHEN", "WHERE"
        ));

        // SQL Server关键字
        SQLSERVER_KEYWORDS.addAll(Arrays.asList(
            "ADD", "ALL", "ALTER", "AND", "ANY", "AS", "ASC", "AUTHORIZATION", "BACKUP", "BEGIN", 
            "BETWEEN", "BREAK", "BROWSE", "BULK", "BY", "CASCADE", "CASE", "CHECK", "CHECKPOINT",
            "CLOSE", "CLUSTERED", "COALESCE", "COLLATE", "COLUMN", "COMMIT", "COMPUTE",
            "CONSTRAINT", "CONTAINS", "CONTAINSTABLE", "CONTINUE", "CONVERT", "CREATE", "CROSS",
            "CURRENT", "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER",
            "CURSOR", "DATABASE", "DBCC", "DEALLOCATE", "DECLARE", "DEFAULT", "DELETE", "DENY",
            "DESC", "DISK", "DISTINCT", "DISTRIBUTED", "DOUBLE", "DROP", "DUMP", "ELSE", "END",
            "ERRLVL", "ESCAPE", "EXCEPT", "EXEC", "EXECUTE", "EXISTS", "EXIT", "EXTERNAL",
            "FETCH", "FILE", "FILLFACTOR", "FOR", "FOREIGN", "FREETEXT", "FREETEXTTABLE",
            "FROM", "FULL", "FUNCTION", "GOTO", "GRANT", "GROUP", "HAVING", "HOLDLOCK",
            "IDENTITY", "IDENTITY_INSERT", "IDENTITYCOL", "IF", "IN", "INDEX", "INNER",
            "INSERT", "INTERSECT", "INTO", "IS", "JOIN", "KEY", "KILL", "LEFT", "LIKE",
            "LINENO", "LOAD", "MERGE", "NATIONAL", "NOCHECK", "NONCLUSTERED", "NOT", "NULL",
            "NULLIF", "OF", "OFF", "OFFSETS", "ON", "OPEN", "OPENDATASOURCE", "OPENQUERY",
            "OPENROWSET", "OPENXML", "OPTION", "OR", "ORDER", "OUTER", "OVER", "PERCENT",
            "PIVOT", "PLAN", "PRECISION", "PRIMARY", "PRINT", "PROC", "PROCEDURE", "PUBLIC",
            "RAISERROR", "READ", "READTEXT", "RECONFIGURE", "REFERENCES", "REPLICATION",
            "RESTORE", "RESTRICT", "RETURN", "REVERT", "REVOKE", "RIGHT", "ROLLBACK",
            "ROWCOUNT", "ROWGUIDCOL", "RULE", "SAVE", "SCHEMA", "SECURITYAUDIT", "SELECT",
            "SEMANTICKEYPHRASETABLE", "SEMANTICSIMILARITYDETAILSTABLE", "SEMANTICSIMILARITYTABLE",
            "SESSION_USER", "SET", "SETUSER", "SHUTDOWN", "SOME", "STATISTICS", "SYSTEM_USER",
            "TABLE", "TABLESAMPLE", "TEXTSIZE", "THEN", "TO", "TOP", "TRAN", "TRANSACTION",
            "TRIGGER", "TRUNCATE", "TRY_CONVERT", "TSEQUAL", "UNION", "UNIQUE", "UNPIVOT",
            "UPDATE", "UPDATETEXT", "USE", "USER", "VALUES", "VARYING", "VIEW", "WAITFOR",
            "WHEN", "WHERE", "WHILE", "WITH", "WITHIN GROUP", "WRITETEXT"
        ));

        // DB2关键字
        DB2_KEYWORDS.addAll(Arrays.asList(
            "ADD", "AFTER", "ALL", "ALLOCATE", "ALLOW", "ALTER", "AND", "ANY", "AS", "ASENSITIVE",
            "ASSOCIATE", "ASUTIME", "AT", "AUDIT", "AUX", "AUXILIARY", "BEFORE", "BEGIN", "BETWEEN",
            "BUFFERPOOL", "BY", "CALL", "CAPTURE", "CASCADED", "CASE", "CAST", "CCSID", "CHAR",
            "CHARACTER", "CHECK", "CLOSE", "CLUSTER", "COLLECTION", "COLUMN", "COMMENT", "COMMIT",
            "CONCAT", "CONDITION", "CONNECT", "CONNECTION", "CONSTRAINT", "CONTAINS", "CONTINUE",
            "COUNT", "COUNT_BIG", "CREATE", "CROSS", "CURRENT", "CURRENT_DATE", "CURRENT_LC_CTYPE",
            "CURRENT_PATH", "CURRENT_SCHEMA", "CURRENT_SERVER", "CURRENT_TIME", "CURRENT_TIMESTAMP",
            "CURRENT_TIMEZONE", "CURRENT_USER", "CURSOR", "DATABASE", "DAY", "DAYS", "DB2GENERAL",
            "DB2GENRL", "DB2SQL", "DBINFO", "DECLARE", "DEFAULT", "DELETE", "DESCRIPTOR", "DETERMINISTIC",
            "DISALLOW", "DISCONNECT", "DISTINCT", "DO", "DOUBLE", "DROP", "DSSIZE", "DYNAMIC",
            "EACH", "EDITPROC", "ELSE", "ELSEIF", "ENCODING", "END", "END-EXEC", "END-EXEC1",
            "ERASE", "ESCAPE", "EXCEPT", "EXCEPTION", "EXCLUDING", "EXECUTE", "EXISTS", "EXIT",
            "EXTERNAL", "FENCED", "FETCH", "FIELDPROC", "FILE", "FINAL", "FOR", "FOREIGN",
            "FREE", "FROM", "FULL", "FUNCTION", "GENERAL", "GENERATED", "GET", "GLOBAL", "GO",
            "GOTO", "GRANT", "GRAPHIC", "GROUP", "HANDLER", "HAVING", "HOLD", "HOUR", "HOURS",
            "IDENTITY", "IF", "IMMEDIATE", "IN", "INCLUDING", "INCREMENT", "INDEX", "INDICATOR",
            "INHERIT", "INNER", "INOUT", "INSENSITIVE", "INSERT", "INTERSECT", "INTO", "IS",
            "ISOBID", "ISOLATION", "ITERATE", "JAR", "JAVA", "JOIN", "KEY", "LABEL", "LANGUAGE",
            "LC_CTYPE", "LEAVE", "LEFT", "LIKE", "LOCAL", "LOCALE", "LOCATOR", "LOCATORS", "LOCK",
            "LOCKMAX", "LOCKSIZE", "LONG", "LOOP", "MAINTAINED", "MATERIALIZED", "MICROSECOND",
            "MICROSECONDS", "MINUTE", "MINUTES", "MODIFIES", "MONTH", "MONTHS", "NEW", "NEW_TABLE",
            "NO", "NONE", "NOT", "NULL", "NULLS", "NUMPARTS", "OBID", "OF", "OLD", "OLD_TABLE",
            "ON", "OPEN", "OPTIMIZATION", "OPTIMIZE", "OPTION", "OR", "ORDER", "OUT", "OUTER",
            "PACKAGE", "PARAMETER", "PART", "PARTITION", "PARTITIONED", "PARTITIONING", "PATH",
            "PIECESIZE", "PLAN", "POSITION", "PRECISION", "PREPARE", "PRIMARY", "PRIQTY", "PRIVILEGES",
            "PROCEDURE", "PROGRAM", "PSID", "PUBLIC", "QUERY", "QUERYNO", "READS", "REFERENCES",
            "REFRESH", "RELEASE", "RENAME", "REPEAT", "RESET", "RESIGNAL", "RESTART", "RESTRICT",
            "RESULT", "RESULT_SET_LOCATOR", "RETURN", "RETURNS", "REVOKE", "RIGHT", "ROLE",
            "ROLLBACK", "ROUTINE", "ROW", "ROWS", "RRN", "RUN", "SAVEPOINT", "SCHEMA", "SCRATCHPAD",
            "SECOND", "SECONDS", "SECQTY", "SECURITY", "SELECT", "SENSITIVE", "SET", "SIGNAL",
            "SIMPLE", "SOME", "SOURCE", "SPECIFIC", "SQL", "SQLID", "STANDARD", "STATIC", "STAY",
            "STOGROUP", "STORES", "STYLE", "SUBSTRING", "SYNONYM", "SYSFUN", "SYSIBM", "SYSPROC",
            "SYSTEM", "TABLE", "TABLESPACE", "THEN", "TO", "TRIGGER", "TRUNCATE", "TYPE", "UNDO",
            "UNION", "UNIQUE", "UNTIL", "UPDATE", "USAGE", "USER", "USING", "VALIDPROC", "VALUE",
            "VALUES", "VARIABLE", "VARIANT", "VCAT", "VIEW", "VOLATILE", "VOLUMES", "WHEN", "WHERE",
            "WHILE", "WITH", "WLM", "XMLELEMENT", "YEAR", "YEARS"
        ));

        // Hive关键字
        HIVE_KEYWORDS.addAll(Arrays.asList(
            "ADD", "ADMIN", "AFTER", "ALL", "ALTER", "ANALYZE", "AND", "ARCHIVE", "ARRAY", "AS",
            "ASC", "BEFORE", "BETWEEN", "BINARY", "BOTH", "BUCKET", "BUCKETS", "BY", "CASCADE",
            "CASE", "CAST", "CHANGE", "CLUSTER", "CLUSTERED", "CLUSTERSTATUS", "COLLECTION",
            "COLUMN", "COLUMNS", "COMMENT", "COMPACT", "COMPACTIONS", "COMPUTE", "CONCATENATE",
            "CONF", "CONTINUE", "CREATE", "CROSS", "CUBE", "CURRENT", "CURRENT_DATE",
            "CURRENT_TIMESTAMP", "CURSOR", "DATA", "DATABASE", "DATABASES", "DATE", "DATETIME",
            "DBPROPERTIES", "DECIMAL", "DEFERRED", "DEFINED", "DELETE", "DELIMITED", "DESC",
            "DESCRIBE", "DIRECTORIES", "DIRECTORY", "DISABLE", "DISTINCT", "DISTRIBUTE",
            "DISTRIBUTED", "DROP", "ELSE", "ENABLE", "END", "ESCAPED", "EXCHANGE", "EXISTS",
            "EXPLAIN", "EXPORT", "EXTENDED", "EXTERNAL", "EXTRACT", "FALSE", "FETCH", "FIELDS",
            "FILE", "FILEFORMAT", "FIRST", "FOLLOWING", "FOR", "FORMAT", "FORMATTED", "FROM",
            "FULL", "FUNCTION", "FUNCTIONS", "GRANT", "GROUP", "GROUPING", "HAVING", "HOLD_DDLTIME",
            "IDXPROPERTIES", "IF", "IGNORE", "IMPORT", "IN", "INDEX", "INDEXES", "INNER", "INPATH",
            "INPUTDRIVER", "INPUTFORMAT", "INSERT", "INTERSECT", "INTERVAL", "INTO", "IS",
            "ITEMS", "JOIN", "KEYS", "LATERAL", "LEFT", "LESS", "LIKE", "LIMIT", "LINES", "LOAD",
            "LOCAL", "LOCATION", "LOCK", "LOCKS", "LOGICAL", "MACRO", "MAP", "MATCHED", "MERGE",
            "METADATA", "MINUS", "MSCK", "NONE", "NOT", "NULL", "NULLS", "OF", "OFFLINE", "ON",
            "ONLY", "OPTION", "OR", "ORDER", "OUT", "OUTER", "OUTPUTDRIVER", "OUTPUTFORMAT",
            "OVER", "OVERWRITE", "PARTITION", "PARTITIONED", "PARTITIONS", "PERCENT", "PRECEDING",
            "PRESERVE", "PRETTY", "PROCEDURE", "PURGE", "RANGE", "READ", "READS", "REBUILD",
            "RECORDREADER", "RECORDWRITER", "REDUCE", "REGEXP", "RELOAD", "RENAME", "REPAIR",
            "REPLACE", "RESTRICT", "REVOKE", "RIGHT", "RLIKE", "ROLE", "ROLES", "ROLLBACK",
            "ROLLUP", "ROW", "ROWS", "SCHEMA", "SCHEMAS", "SELECT", "SEMI", "SEPARATED", "SERDE",
            "SERDEPROPERTIES", "SET", "SETS", "SHOW", "SHOW_DATABASE", "SKEWED", "SORT",
            "SORTED", "SSL", "STATISTICS", "STORED", "STREAMTABLE", "STRING", "STRUCT", "TABLE",
            "TABLES", "TABLESAMPLE", "TBLPROPERTIES", "TEMPORARY", "TERMINATED", "THEN", "TO",
            "TOUCH", "TRANSACTION", "TRANSACTIONS", "TRANSFORM", "TRIGGER", "TRUE", "TRUNCATE",
            "UNARCHIVE", "UNBOUNDED", "UNDO", "UNION", "UNIONTYPE", "UNIQUE", "UNLOCK", "UNSET",
            "UNSIGNED", "UPDATE", "URI", "USE", "USER", "USING", "UTC", "UTC_TMESTAMP", "VALUES",
            "VECTORIZATION", "VIEW", "VIEWS", "WHEN", "WHERE", "WHILE", "WINDOW", "WITH"
        ));

        // ClickHouse关键字
        CLICKHOUSE_KEYWORDS.addAll(Arrays.asList(
            "ATTACH", "DETACH", "DROP", "EXISTS", "EXTRACT", "FROM", "GROUP", "HAVING", "INTO",
            "NOT", "PARTITION", "SAMPLE", "SELECT", "TABLE", "TABLES", "UNION", "USING", "WHERE",
            "WITH", "FORMAT", "LIMIT", "OFFSET", "SETTINGS", "ARRAY", "JOIN", "LEFT", "RIGHT",
            "INNER", "OUTER", "CROSS", "GLOBAL", "ANY", "ALL", "ASOF", "BETWEEN", "IN", "LIKE",
            "ILIKE", "TIMESTAMP", "INTERVAL", "DATE", "DATETIME", "TODAY", "YESTERDAY", "TOMORROW",
            "CURRENT", "NOW", "DEFAULT", "PRIMARY", "KEY", "ENGINE", "IF", "FINAL", "CLUSTER",
            "ASYNC", "MATERIALIZED", "VIEW", "DICTIONARY", "FUNCTION", "DISTINCT", "ORDER",
            "BY", "ASC", "DESC", "COLLATE", "ALIAS", "AS", "ALTER", "ADD", "AFTER", "MODIFY",
            "CLEAR", "COLUMN", "COMMENT", "CONSTRAINT", "CREATE", "DATABASE", "DATABASES",
            "DELETE", "DESCRIBE", "DETACH", "DICTIONARY", "OPTIMIZE", "PREWHERE", "PROFILE",
            "RENAME", "REPLACE", "REPLICA", "REPLICATED", "SET", "SHOW", "SYSTEM", "TEMPORARY",
            "TOTALS", "UPDATE", "USE", "VALUES", "WATCH", "KILL", "QUERY", "SYNC", "ASYNC",
            "TEST", "ON", "OFF", "TOP", "LIMIT", "OFFSET", "FETCH", "NEXT", "TIES", "ONLY",
            "PERCENT", "WITH", "TIES", "START", "TRANSACTION", "COMMIT", "ROLLBACK", "WORK",
            "CHAIN", "NO", "RELEASE", "SAVEPOINT", "LOCK", "UNLOCK", "SHARE", "MODE"
        ));

        // 达梦数据库关键字 (DM7/DM8)
        DM_KEYWORDS.addAll(ORACLE_KEYWORDS);  // 达梦兼容Oracle关键字
        DM_KEYWORDS.addAll(Arrays.asList(
            "SCHEMA", "ROLE", "PACKAGE", "DIRECTORY", "SEQUENCE", "SYNONYM",
            "PUBLIC", "COMMENT", "FORCE", "RESOURCE", "AUDIT", "NOAUDIT",
            "GRANT", "REVOKE", "ANALYZE", "VALIDATE", "COMPUTE", "STATISTICS",
            "TRUNCATE", "EXPLAIN", "PLAN", "OUTLINE", "MERGE", "MINUS",
            "INTERSECT", "START", "CONNECT", "SESSION", "IMMEDIATE", "SAVEPOINT",
            "AUTONOMOUS_TRANSACTION", "COMMIT", "ROLLBACK", "PRAGMA",
            "SERVEROUTPUT", "DBMS_OUTPUT", "PUT_LINE", "RAISE_APPLICATION_ERROR"
        ));

        // 神通数据库关键字
        SHENTONG_KEYWORDS.addAll(ORACLE_KEYWORDS);  // 神通数据库兼容Oracle关键字
        SHENTONG_KEYWORDS.addAll(Arrays.asList(
            "SCHEMA", "ROLE", "PACKAGE", "DIRECTORY", "SEQUENCE", "SYNONYM",
            "PUBLIC", "COMMENT", "FORCE", "RESOURCE", "AUDIT", "NOAUDIT",
            "GRANT", "REVOKE", "ANALYZE", "VALIDATE", "COMPUTE", "STATISTICS"
        ));

        // GBase 8s关键字 (南大通用)
        GBASE_KEYWORDS.addAll(POSTGRESQL_KEYWORDS);  // GBase 8s兼容PostgreSQL关键字
        GBASE_KEYWORDS.addAll(Arrays.asList(
            "SCHEMA", "ROLE", "PACKAGE", "DIRECTORY", "SEQUENCE", "SYNONYM",
            "PUBLIC", "COMMENT", "FORCE", "RESOURCE", "AUDIT", "NOAUDIT",
            "GRANT", "REVOKE", "ANALYZE", "VALIDATE", "COMPUTE", "STATISTICS"
        ));

        // Doris关键字
        DORIS_KEYWORDS.addAll(Arrays.asList(
            "ADD", "ADMIN", "AFTER", "AGGREGATE", "ALL", "ALTER", "ANALYZE",
            "AND", "ANTI", "ARRAY", "AS", "ASC", "ASYNC", "AUTHORS", "AUTO_INCREMENT",
            "BACKEND", "BACKENDS", "BACKUP", "BEGIN", "BETWEEN", "BIGINT", "BITMAP",
            "BITMAP_UNION", "BOOLEAN", "BOTH", "BROKER", "BUCKETS", "BUILD",
            "BY", "CANCEL", "CASE", "CAST", "CATALOG", "CATALOGS", "CHAIN",
            "CHAR", "CHARSET", "CHECK", "CLUSTER", "CLUSTERS", "COLLATE",
            "COLUMN", "COLUMNS", "COMMENT", "COMMIT", "COMMITTED", "COMPUTE",
            "CONFIG", "CONNECTION", "CONSISTENT", "CONVERT", "COPY", "COUNT",
            "CREATE", "CROSS", "CUBE", "CURRENT", "CURRENT_TIMESTAMP",
            "CURRENT_USER", "DATA", "DATABASE", "DATABASES", "DATE", "DATETIME",
            "DAY", "DECIMAL", "DECOMMISSION", "DEFAULT", "DELETE", "DENY",
            "DESC", "DESCRIBE", "DISTINCT", "DISTRIBUTED", "DISTRIBUTION",
            "DIV", "DOUBLE", "DROP", "ELSE", "END", "ENGINE", "ENGINES",
            "ERRORS", "EVENTS", "EXECUTE", "EXISTS", "EXPLAIN", "EXPORT",
            "EXTENDED", "EXTERNAL", "EXTRACT", "FALSE", "FIELDS", "FILE",
            "FILTER", "FIRST", "FLOAT", "FOLLOWING", "FOR", "FORCE", "FORMAT",
            "FREE", "FROM", "FRONTEND", "FRONTENDS", "FULL", "FUNCTION",
            "FUNCTIONS", "GLOBAL", "GRANT", "GRANTS", "GROUP", "GROUPING",
            "GROUPS", "HASH", "HAVING", "HELP", "HLL_UNION", "HOSTNAME",
            "HOUR", "HUB", "IDENTIFIED", "IF", "IGNORE", "IMAGE", "IN",
            "INDEX", "INDEXES", "INFILE", "INNER", "INSERT", "INT", "INTEGER",
            "INTERMEDIATE", "INTERSECT", "INTERVAL", "INTO", "IS", "ISOLATION",
            "JOIN", "JSON", "KILL", "LABEL", "LARGEINT", "LEFT", "LESS",
            "LEVEL", "LIKE", "LIMIT", "LINES", "LINK", "LOAD", "LOCAL",
            "LOCATION", "LOCK", "LOGICAL", "MANUAL", "MAP", "MATERIALIZED",
            "MAX", "MERGE", "MIGRATE", "MIN", "MINUTE", "MODIFY", "MONTH",
            "MTMV", "NAME", "NAMES", "NATURAL", "NEGATIVE", "NO", "NODE",
            "NOT", "NULL", "NULLS", "OBSERVER", "OF", "OFFSET", "ON", "ONLY",
            "OPEN", "OPTIMIZER", "OPTION", "OR", "ORDER", "OUTER", "OUTFILE",
            "OVER", "PARTITION", "PARTITIONS", "PASSWORD", "PATH", "PAUSE",
            "PENDING", "PERCENTILE_UNION", "PLUGIN", "PLUGINS", "PRECEDING",
            "PRIMARY", "PRIVILEGES", "PROCEDURE", "PROPERTIES", "PROPERTY",
            "QUALIFY", "QUARTER", "QUERY", "QUEUE", "QUOTA", "RANDOM", "RANGE",
            "READ", "REAL", "REBUILD", "RECOVER", "REFRESH", "REGEXP", "RELEASE",
            "RENAME", "REPAIR", "REPEATABLE", "REPLACE", "REPLICA", "REPOSITORIES",
            "REPOSITORY", "RESOURCE", "RESOURCES", "RESTORE", "RESUME", "RETURNS",
            "REVOKE", "RIGHT", "RLIKE", "ROLE", "ROLES", "ROLLBACK", "ROLLUP",
            "ROUTINE", "ROW", "ROWS", "SAMPLE", "SCHEDULER", "SCHEMA", "SCHEMAS",
            "SECOND", "SELECT", "SEMI", "SERIALIZABLE", "SESSION", "SET", "SETS",
            "SHOW", "SIGNED", "SMALLINT", "SNAPSHOT", "SQLBLACKLIST", "START",
            "STATS", "STATUS", "STOP", "STORAGE", "STREAM", "STRING", "SUBMIT",
            "SUM", "SYNC", "SYSTEM", "TABLE", "TABLES", "TABLESAMPLE", "TABLET",
            "TABLETS", "TEMPORARY", "TERMINATED", "THAN", "THEN", "TIME", "TIMESTAMP",
            "TINYINT", "TO", "TRANSACTION", "TRANSACTIONS", "TRIGGER", "TRIGGERS",
            "TRUE", "TRUNCATE", "TYPE", "TYPES", "UNBOUNDED", "UNCOMMITTED",
            "UNION", "UNIQUE", "UNSIGNED", "UPDATE", "USE", "USER", "USING",
            "VALUE", "VALUES", "VARCHAR", "VARIABLES", "VIEW", "WARNINGS",
            "WEEK", "WHEN", "WHERE", "WHITE", "WITH", "WORK", "WRITE", "YEAR"
        ));
    }

    public static Set<String> getKeywords(String dbType) {
        String family = DatabaseTypes.getDbFamily(dbType);
        switch (family) {
            case "mysql":
                return MYSQL_KEYWORDS;
            case "oracle":
                return ORACLE_KEYWORDS;
            case "postgresql":
                return POSTGRESQL_KEYWORDS;
            case "sqlserver":
                return SQLSERVER_KEYWORDS;
            case "db2":
                return DB2_KEYWORDS;
            case "hive":
                return HIVE_KEYWORDS;
            case "clickhouse":
                return CLICKHOUSE_KEYWORDS;
            case "doris":
                return DORIS_KEYWORDS;
            case "dm":
                return DM_KEYWORDS;
            case "shentong":
                return SHENTONG_KEYWORDS;
            case "gbase":
                return GBASE_KEYWORDS;
            default:
                return MYSQL_KEYWORDS;
        }
    }
}
