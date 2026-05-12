package com.blog.personalblogbackend.exception;

import com.blog.personalblogbackend.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<Result<?>> handleServiceException(ServiceException e) {
        if (e.getCode() != null && e.getCode() >= 500) {
            log.error("业务异常: {}", e.getMessage());
        } else {
            log.debug("业务提示: {} {}", e.getCode(), e.getMessage());
        }
        HttpStatus status = mapStatus(e.getCode());
        Result<?> body = Result.fail(e.getCode(), e.getMessage(), e.getPayload());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<?>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        if (msg.isEmpty()) {
            msg = "参数校验失败";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.fail(400, msg, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<?>> handleGeneralException(Exception e) {
        log.error("系统异常: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(500, "系统繁忙，请稍后再试", null));
    }

    private static HttpStatus mapStatus(Integer code) {
        if (code == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return switch (code) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            case 429 -> HttpStatus.TOO_MANY_REQUESTS;
            default -> code >= 400 && code < 500 ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
