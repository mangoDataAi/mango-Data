package com.mango.test.database.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ResourceListResult {
    private String sourceType; // 数据源类型
    private List<ResourceInfo> resources; // 资源列表
    private String version; // 数据库版本(针对数据库类型)
    private String charset; // 字符集
    private Map<String, Object> extraInfo; // 额外信息


}
