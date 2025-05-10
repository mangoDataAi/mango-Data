package com.mango.test.database.model.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class ExcelTemplateSheet {
    @ExcelProperty("字段名称")
    private String fieldName;
    
    @ExcelProperty("字段类型")
    private String fieldType;
    
    @ExcelProperty("字段长度")
    private Integer length;
    
    @ExcelProperty("是否必填")
    private String required;
    
    @ExcelProperty("字段说明")
    private String description;
    
    @ExcelProperty("示例值")
    private String example;
} 