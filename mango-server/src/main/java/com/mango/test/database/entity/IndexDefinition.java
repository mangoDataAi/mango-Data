package com.mango.test.database.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 索引定义类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public  class IndexDefinition {
    private String name;            // 索引名称
    private boolean unique;         // 是否唯一索引
    private boolean primaryKey;     // 是否主键
    private List<String> columns;   // 索引列
    private String type;            // 索引类型
    private String comment;         // 索引注释
    private String method;          // 索引方法
    private Map<String, String> properties; // 索引属性
}