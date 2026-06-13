package com.blog.common.exception;

public class ServiceException extends RuntimeException {

    private Integer code = 500;
    private Object payload;

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public ServiceException(Integer code, String message, Object payload) {
        super(message);
        this.code = code;
        this.payload = payload;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }
}
