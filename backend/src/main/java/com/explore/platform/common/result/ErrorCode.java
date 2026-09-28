package com.explore.platform.common.result;

import lombok.Getter;

/**
 * 统一错误码枚举
 *
 * @author explore-platform
 */
@Getter//获取属性值的方法
//enum 枚举类，用于定义固定值的常量
public enum ErrorCode {

    SUCCESS(200, "成功"),
    PARAM_ERROR(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证或认证已过期"),
    FORBIDDEN(403, "没有访问权限"),
    NOT_FOUND(404, "请求资源不存在"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),

    LOGIN_FAILED(4001, "手机号或密码错误"),
    USER_FROZEN(4002, "账号已被冻结"),

    BUSINESS_ERROR(5000, "业务处理失败"),

    // 秒杀专用业务码（设计文档约定，便于前端区分提示）
    SECKILL_STOCK_EMPTY(501, "手慢了，优惠券已被抢完"),
    SECKILL_NOT_IN_TIME(502, "不在秒杀活动时间内"),
    SECKILL_REPEAT_ORDER(503, "每人限购1份，请勿重复下单"),

    SYSTEM_ERROR(500, "系统繁忙，请稍后再试");

    private final Integer code;

    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
