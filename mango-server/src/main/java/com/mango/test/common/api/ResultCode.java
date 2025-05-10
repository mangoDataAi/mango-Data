package com.mango.test.common.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 返回码枚举
 */
@Getter
@AllArgsConstructor
public enum ResultCode implements IResultCode {

    /**
     * 操作成功
     */
    SUCCESS(200, "操作成功"),

    /**
     * 操作失败
     */
    FAILURE(400, "操作失败"),

    /**
     * 未授权
     */
    UNAUTHORIZED(401, "未授权"),

    /**
     * 服务异常
     */
    SERVER_ERROR(500, "服务异常");

    /**
     * 状态码
     */
    final int code;

    /**
     * 消息
     */
    final String message;
} 