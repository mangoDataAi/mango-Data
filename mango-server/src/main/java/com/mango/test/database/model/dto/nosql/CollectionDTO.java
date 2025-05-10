package com.mango.test.database.model.dto.nosql;

import lombok.Data;
import java.util.Map;

/**
 * NoSQL数据库集合DTO
 */
@Data
public class CollectionDTO {
    
    /**
     * 集合名称
     */
    private String name;
    
    /**
     * 文档数量
     */
    private Long count;
    
    /**
     * 集合大小 (字节)
     */
    private Long size;
    
    /**
     * 是否分片
     */
    private boolean sharded;
    
    /**
     * 是否有索引
     */
    private boolean indexed;
    
    /**
     * 是否有副本
     */
    private boolean replicated;
    
    /**
     * 集合状态
     */
    private String status;
    
    /**
     * 额外信息
     */
    private Map<String, Object> extra;
} 