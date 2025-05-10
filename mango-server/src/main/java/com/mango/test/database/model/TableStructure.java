package com.mango.test.database.model;

import lombok.Data;

import java.util.List;

/**
 * 表结构信息
 */
@Data
public class TableStructure {
    // 表名
    private String tableName;
    // 显示名称
    private String displayName;
    // 表字段列表
    private List<FieldStructure> fields;

    /**
     * 获取表名
     * @return 表名
     */
    public String getName() {
        return tableName;
    }
    
}