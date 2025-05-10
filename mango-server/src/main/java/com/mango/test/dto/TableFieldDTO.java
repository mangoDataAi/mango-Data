package com.mango.test.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonSetter;

@Data
public class TableFieldDTO {
    private String id;
    private String name;
    private String type;
    private String displayName;
    private Integer length;
    private Integer precision;
    private Integer scale;
    private Integer notNull;
    private Integer isPrimary;
    private Integer mgautoIncrement;
    private String defaultValue;
    private String mgcomment;

    // 添加布尔值到整数的转换方法
    @JsonSetter("isPrimary")
    public void setIsPrimaryFromAny(Object value) {
        if (value instanceof Boolean) {
            this.isPrimary = (Boolean) value ? 1 : 0;
        } else if (value instanceof Integer) {
            this.isPrimary = (Integer) value;
        } else if (value instanceof String) {
            this.isPrimary = Boolean.parseBoolean((String) value) ? 1 : 0;
        }
    }

    @JsonSetter("notNull")
    public void setNotNullFromAny(Object value) {
        if (value instanceof Boolean) {
            this.notNull = (Boolean) value ? 1 : 0;
        } else if (value instanceof Integer) {
            this.notNull = (Integer) value;
        } else if (value instanceof String) {
            this.notNull = Boolean.parseBoolean((String) value) ? 1 : 0;
        }
    }

    // 处理前端 autoIncrement 到后端 mgautoIncrement 的映射
    @JsonSetter("autoIncrement")
    public void setAutoIncrementFromAny(Object value) {
        if (value instanceof Boolean) {
            this.mgautoIncrement = (Boolean) value ? 1 : 0;
        } else if (value instanceof Integer) {
            this.mgautoIncrement = (Integer) value;
        } else if (value instanceof String) {
            this.mgautoIncrement = Boolean.parseBoolean((String) value) ? 1 : 0;
        }
    }
}
