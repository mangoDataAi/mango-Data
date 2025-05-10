package com.mango.test.dto;

import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * 模型搜索DTO
 */
@Data
public class ModelSearchDTO {
    /**
     * 模型名称
     */
    private String name;
    
    /**
     * 模型类型
     */
    private String type;
    
    /**
     * 主题域ID
     */
    private String domainId;
    
    /**
     * 所有者
     */
    private String owner;
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 创建时间起始
     */
    private Date createTimeStart;
    
    /**
     * 创建时间结束
     */
    private Date createTimeEnd;
    
    /**
     * 更新时间起始
     */
    private Date updateTimeStart;
    
    /**
     * 更新时间结束
     */
    private Date updateTimeEnd;
    
    /**
     * 标签列表 - 保留字段，但在控制器中不使用
     */
    private List<String> tags;
    
    /**
     * 最小版本号 - 保留字段，但在控制器中不使用
     */
    private Integer minVersion;
    
    /**
     * 最大版本号 - 保留字段，但在控制器中不使用
     */
    private Integer maxVersion;
} 