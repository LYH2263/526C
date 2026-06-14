package com.example.book.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

    PARAM_VALIDATION_FAILED(400, "参数校验失败"),
    RESOURCE_NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统繁忙");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
