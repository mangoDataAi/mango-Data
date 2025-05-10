package com.mango.test.vo;

import lombok.Data;

/**
 * 分页请求对象
 * @param <T> 查询条件对象类型
 */
@Data
public class PageRequest<T> {
    /**
     * 当前页码
     */
    private int current = 1;

    /**
     * 每页大小
     */
    private int size = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序方式：asc或desc
     */
    private String sortOrder;
    
    /**
     * 查询条件
     */
    private T condition;
} 