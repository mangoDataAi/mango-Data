package com.mango.test.database.entity;

import lombok.Data;

@Data
public class ColumnInfo {
    private String name;            // 字段名
    private String type;            // 字段类型
    private String comment;         // 字段注释
    private Integer length;         // 字段长度
    private Integer precision;      // 精度
    private Integer scale;
    private Boolean notNull;
    private Boolean primaryKey;     // 是否主键
    private String defaultValue;    // 默认值
    private Boolean autoIncrement;  // 是否自增

    public boolean isNotNull() {
        return notNull != null && notNull;
    }

    public Integer getScale() {
        return scale;
    }
} 