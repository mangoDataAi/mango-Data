package com.mango.test.vo;

import java.io.Serializable;

/**
 * 统一返回结果类
 * 
 * @param <T> 返回数据类型
 */
public class R<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    private int code;

    /**
     * 返回消息
     */
    private String message;

    /**
     * 返回数据
     */
    private T data;

    /**
     * 私有构造方法，通过静态方法创建实例
     */
    private R(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功返回结果
     * 
     * @param <T> 返回数据类型
     * @return 返回结果
     */
    public static <T> R<T> ok() {
        return new R<>(0, "操作成功", null);
    }

    /**
     * 成功返回结果
     * 
     * @param data 返回数据
     * @param <T>  返回数据类型
     * @return 返回结果
     */
    public static <T> R<T> ok(T data) {
        return new R<>(0, "操作成功", data);
    }

    /**
     * 成功返回结果
     * 
     * @param message 返回消息
     * @param data    返回数据
     * @param <T>     返回数据类型
     * @return 返回结果
     */
    public static <T> R<T> ok(String message, T data) {
        return new R<>(0, message, data);
    }

    /**
     * 失败返回结果
     * 
     * @param <T> 返回数据类型
     * @return 返回结果
     */
    public static <T> R<T> fail() {
        return new R<>(-1, "操作失败", null);
    }

    /**
     * 失败返回结果
     * 
     * @param message 返回消息
     * @param <T>     返回数据类型
     * @return 返回结果
     */
    public static <T> R<T> fail(String message) {
        return new R<>(-1, message, null);
    }

    /**
     * 失败返回结果
     * 
     * @param code    状态码
     * @param message 返回消息
     * @param <T>     返回数据类型
     * @return 返回结果
     */
    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }

    /**
     * 失败返回结果
     * 
     * @param code    状态码
     * @param message 返回消息
     * @param data    返回数据
     * @param <T>     返回数据类型
     * @return 返回结果
     */
    public static <T> R<T> fail(int code, String message, T data) {
        return new R<>(code, message, data);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
    
    /**
     * 设置数据并返回当前对象，支持链式调用
     * 
     * @param data 返回数据
     * @return 当前对象
     */
    public R<T> withData(T data) {
        this.data = data;
        return this;
    }
}
