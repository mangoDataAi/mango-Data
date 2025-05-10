package com.mango.test.database.model;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class ExcelTableInfo {
    @ExcelProperty("表名")
    private String tableName;

    @ExcelProperty("描述")
    private String description;

    // ... 其他字段
}
