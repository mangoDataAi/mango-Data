package com.mango.test.database.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 分页结果封装类
 */
@Data
@Accessors(chain = true)
public class PageResult<T> {
    /**
     * 当前页码
     */
    private long pageNum;
    
    /**
     * 每页数量
     */
    private long pageSize;
    
    /**
     * 总记录数
     */
    private long total;
    
    /**
     * 总页数
     */
    private long pages;
    
    /**
     * 数据列表
     */
    private List<T> records;
    
    /**
     * 是否有下一页
     */
    private boolean hasNext;
    
    /**
     * 是否有上一页
     */
    private boolean hasPrevious;
} 