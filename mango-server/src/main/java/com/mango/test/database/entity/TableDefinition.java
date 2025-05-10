package com.mango.test.database.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 表结构定义类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TableDefinition {
    private String tableName;                    // 表名
    private String tableType;                    // 表名
    private String tableComment;                 // 表注释
    private List<ColumnDefinition> columns;      // 字段定义列表
    private List<IndexDefinition> indexes;       // 索引定义列表
    private String primaryKey;                   // 主键
    private String engine;                       // 存储引擎
    private String charset;                      // 字符集
    private String collate;                      // 排序规则
    private Map<String, String> extraProperties; // 额外属性

    public boolean isIfNotExists() {
        // 返回表示是否使用 IF NOT EXISTS 的布尔值
        // 可以从类的其他属性中获取或设置默认值
        return false; // 或根据需要返回 true
    }
}