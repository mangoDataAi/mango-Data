package com.mango.test.database.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 字段定义类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ColumnDefinition {
    private String name;            // 字段名
    private String type;            // 字段类型
    private Integer length;         // 字段长度
    private Integer precision;      // 精度
    private Integer scale;          // 小数位数
    private boolean nullable;       // 是否可空
    private String defaultValue;    // 默认值
    private String comment;         // 注释
    private boolean autoIncrement;  // 是否自增
    private boolean primaryKey;     // 是否主键
    private String charset;         // 字符集
    private String collate;         // 排序规则
    private Map<String, String> extraProperties; // 额外属性
}
