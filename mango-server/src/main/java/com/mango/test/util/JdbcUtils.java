package com.mango.test.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JdbcUtils {

    private static final String DRIVER_CLASS = "dm.jdbc.driver.DmDriver";
    private static final String FILE_DB_URL = "jdbc:dm://localhost:5236/MANGO_FILE";
    private static final String FILE_DB_USERNAME = "MANGO_FILE";
    private static final String FILE_DB_PASSWORD = "123456789";

    private static JdbcTemplate jdbcTemplate;

    public JdbcUtils(DataSource dataSource) {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }
    
    /**
     * 执行建表语句
     */
    public static void createTable(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            log.error("创建表失败", e);
            throw new RuntimeException("创建表失败: " + e.getMessage());
        }
    }

    /**
     * 执行删表语句
     */
    public static void dropTable(String tableName) {
        try {
            String sql = String.format("DROP TABLE IF EXISTS %s", tableName);
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            log.error("删除表失败", e);
            throw new RuntimeException("删除表失败: " + e.getMessage());
        }
    }

    /**
     * 批量插入数据
     */
    public static void batchInsert(String sql, List<Object[]> batchArgs) {
        try {
            jdbcTemplate.batchUpdate(sql, batchArgs);
        } catch (Exception e) {
            log.error("批量插入数据失败", e);
            throw new RuntimeException("批量插入数据失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询数据
     */
    public static List<Map<String, Object>> queryForList(String sql) {
        try {
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("查询数据失败", e);
            throw new RuntimeException("查询数据失败: " + e.getMessage());
        }
    }

    /**
     * 清空表数据
     */
    public static void truncateTable(String tableName) {
        try {
            String sql = String.format("TRUNCATE TABLE %s", tableName);
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            log.error("清空表数据失败", e);
            throw new RuntimeException("清空表数据失败: " + e.getMessage());
        }
    }

    public static void execute(String sql) {
        jdbcTemplate.execute(sql);
    }

    public static void batchUpdate(String sql, List<Object[]> batchArgs) {
        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    public static <T> T queryForObject(String sql, Class<T> requiredType) {
        return jdbcTemplate.queryForObject(sql, requiredType);
    }
}
