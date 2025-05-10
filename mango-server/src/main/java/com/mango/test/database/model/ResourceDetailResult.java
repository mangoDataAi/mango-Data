package com.mango.test.database.model;

import lombok.Data;
import java.util.Map;

@Data
public class ResourceDetailResult {
    private String name;            // 资源名称
    private String type;            // 资源类型
    private String path;            // 资源路径
    private Long size;             // 大小
    private String updateTime;      // 更新时间
    private String owner;          // 所有者
    private String permission;     // 权限
    private Map<String, Object> metadata;  // 元数据
    private Map<String, Object> data;      // 资源数据
    private Map<String, Object> schema;    // 资源结构
} 