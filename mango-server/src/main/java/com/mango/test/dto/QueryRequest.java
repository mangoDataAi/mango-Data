package com.mango.test.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 查询请求DTO
 */
@Data
public class QueryRequest {
    /**
     * 数据源ID
     */
    private String dataSourceId;
    
    /**
     * 数据库类型
     */
    private String dbType;
    
    /**
     * 数据库连接URL
     */
    private String url;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * SQL语句
     */
    private String sql;

    /**
     * 表结构信息
     */
    private List<TableField> fields;

    /**
     * 查询结果
     */
    private List<Map<String, Object>> results;

    private Integer pageNum;

    private Integer pageSize;

    @Data
    public static class TableField {
        /**
         * 字段名
         */
        private String name;

        /**
         * 字段类型
         */
        private String type;

        /**
         * 字段注释
         */
        private String comment;

        /**
         * 字段长度
         */
        private Integer length;

        /**
         * 数值精度
         */
        private Integer precision;

        /**
         * 小数位数
         */
        private Integer scale;

        /**
         * 是否可空
         */
        private String nullable;

        /**
         * 默认值
         */
        private String defaultValue;

        /**
         * 是否主键
         */
        private String keyType;
    }
} 