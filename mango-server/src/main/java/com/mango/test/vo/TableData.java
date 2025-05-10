package com.mango.test.vo;

import lombok.Data;

import java.util.List;

/**
 * 表格数据包装类
 * @param <T> 数据类型
 */
@Data
public class TableData<T> {
    /**
     * 总记录数
     */
    private long total;

    /**
     * 当前页数据
     */
    private List<T> records;

    /**
     * 当前页码
     */
    private long current;

    /**
     * 每页大小
     */
    private long size;

    /**
     * 总页数
     */
    private long pages;

    /**
     * 构造方法
     * @param records 记录
     * @param total 总记录数
     * @param current 当前页码
     * @param size 每页大小
     */
    public TableData(List<T> records, long total, long current, long size) {
        this.records = records;
        this.total = total;
        this.current = current;
        this.size = size;
        this.pages = total % size == 0 ? total / size : total / size + 1;
    }
} 