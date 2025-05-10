package com.mango.test.dto;

import lombok.Data;

@Data
public class TableStructureDTO {
    private String name;        // 字段名
    private String type;        // 字段类型
    private Boolean nullable;   // 是否允许为空
    private Boolean key;        // 是否是主键
    private String defaultValue;// 默认值
    private String comment;     // 字段注释
} 