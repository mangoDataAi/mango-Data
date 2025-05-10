package com.mango.test.database.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DomainQuery {
    
    /**
     * 当前页码
     */
    private Integer pageNum = 1;
    
    /**
     * 每页数量
     */
    private Integer pageSize = 10;
    
    /**
     * 主题域名称
     */
    private String name;
    
    /**
     * 数据源类型
     */
    private String sourceType;
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 负责人
     */
    private String owner;
    
    /**
     * 创建时间起始
     */
    private LocalDateTime createTimeStart;
    
    /**
     * 创建时间结束
     */
    private LocalDateTime createTimeEnd;
    
    /**
     * 父级域ID
     */
    private String parentId;
} 