package com.mango.test.common.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页查询结果
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页码
     */
    private long pageNum;

    /**
     * 每页数量
     */
    private long pageSize;

    /**
     * 总页数
     */
    private long totalPage;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 列表数据
     */
    private List<T> list;

    /**
     * 构造方法
     *
     * @param list     列表数据
     * @param total    总记录数
     * @param pageSize 每页数量
     * @param pageNum  当前页码
     */
    public PageResult(List<T> list, long total, long pageSize, long pageNum) {
        this.list = list;
        this.total = total;
        this.pageSize = pageSize;
        this.pageNum = pageNum;
        this.totalPage = (total + pageSize - 1) / pageSize;
    }
} 