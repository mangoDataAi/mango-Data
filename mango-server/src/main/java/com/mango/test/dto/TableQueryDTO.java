package com.mango.test.dto;

import lombok.Data;

@Data
public class TableQueryDTO {
    private String name;            // 表名
    private String displayName;     // 显示名称
    private String createTimeStart; // 创建时间起始
    private String createTimeEnd;   // 创建时间结束
    private String updateTimeStart; // 更新时间起始
    private String updateTimeEnd;   // 更新时间结束
    private Integer minFieldCount;  // 最小字段数
    private Integer maxFieldCount;  // 最大字段数
} 