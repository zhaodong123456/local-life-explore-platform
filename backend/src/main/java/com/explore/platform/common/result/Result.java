package com.explore.platform.common.result;

import lombok.Data;

import java.io.Serializable;//序列化接口，用于将对象转换为字节数组，方便网络传输
//Serializable 可串行化的

/**
 * 统一响应结果封装
 *
 * @param <T> 业务数据类型
 * @author explore-platform
 */
@Data
public class Result<T> implements Serializable {

    private Integer code;

    private String message;

    private T data;

    public Result() {
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功返回（带数据）
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功返回（无数据）
     */
    public static <T> Result<T> ok() {
        return ok(null);
    }

    /**
     * 失败返回（指定错误码）
     */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 失败返回（指定错误码 + 自定义消息）
     */
    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null);
    }

    /**
     * 失败返回（自定义 code 和消息）
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
