package com.mango.test.dto;

import lombok.Data;

import java.util.List;

@Data
public class TableDTO {
    private String id;
    private String name;
    private String displayName;
    private List<TableFieldDTO> fields;

    /**
     * 表注释
     */
    private String comment;

    /**
     * 数据库类型
     */
    private String dbType;
}
