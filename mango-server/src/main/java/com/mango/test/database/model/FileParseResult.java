package com.mango.test.database.model;

import com.mango.test.database.entity.FileField;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class FileParseResult {
    private String tableName;        // 表名
    private String tableComment;     // 表注释
    private List<FileField> fields;  // 修改为 FileField 类型
    private List<Map<String, Object>> data;  // 数据
} 