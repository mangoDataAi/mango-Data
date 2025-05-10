package com.mango.test.database.model;

import lombok.Data;

/**
 * 字段结构信息
 */
@Data
public class FieldStructure {
    // 字段名
    private String name;
    // 字段类型
    private String type;
    // 长度
    private Integer length;
    // 精度
    private Integer precision;
    // 小数位数
    private Integer scale;
    // 是否为主键
    private boolean isPrimary;
    // 是否不为空
    private boolean isNotNull;
    // 是否自增
    private boolean isAutoIncrement;
    // 默认值
    private String defaultValue;
    // 注释
    private String comment;
} 