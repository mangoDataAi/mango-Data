package com.mango.test.common.api;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页数据包装类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 每页记录数
     */
    private long size;

    /**
     * 当前页数
     */
    private long current;

    /**
     * 数据列表
     */
    private List<T> records;
} 