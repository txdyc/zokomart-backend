package com.zokomart.backend.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException exception) {
        return ResponseEntity.status(exception.getHttpStatus())
                .body(error(exception.getCode(), exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<Map<String, Object>> handleNotLogin(NotLoginException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(error("ADMIN_UNAUTHORIZED", "后台用户未登录或登录已失效", Map.of()));
    }

    @ExceptionHandler(NotPermissionException.class)
    public ResponseEntity<Map<String, Object>> handleNotPermission(NotPermissionException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(error("ADMIN_FORBIDDEN", "当前后台用户无权访问该接口", Map.of()));
    }

    @ExceptionHandler(NotImplementedException.class)
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public Map<String, Object> handleNotImplemented(NotImplementedException exception) {
        return error("NOT_IMPLEMENTED", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error(
                        "RESOURCE_NOT_FOUND",
                        "请求的资源不存在",
                        Map.of(
                                "method", Objects.toString(exception.getHttpMethod(), ""),
                                "resource", safeString(exception.getResourcePath())
                        )
                ));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleMissingRequestHeader(MissingRequestHeaderException exception) {
        return missingHeaderError(exception.getHeaderName());
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleHandlerMethodValidation(HandlerMethodValidationException exception) {
        for (ParameterValidationResult validationResult : exception.getParameterValidationResults()) {
            RequestHeader requestHeader = validationResult.getMethodParameter().getParameterAnnotation(RequestHeader.class);
            if (requestHeader == null) {
                continue;
            }
            String headerName = requestHeader.name().isBlank() ? requestHeader.value() : requestHeader.name();
            if (!headerName.isBlank() && isNullOrBlank(validationResult.getArgument())) {
                return missingHeaderError(headerName);
            }
        }
        return error("INVALID_REQUEST", "请求格式非法或无法解析", Map.of());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleConstraintViolation(ConstraintViolationException exception) {
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            String propertyPath = violation.getPropertyPath().toString();
            if (propertyPath.endsWith(".buyerId") && isNullOrBlank(violation.getInvalidValue())) {
                return missingHeaderError("X-Buyer-Id");
            }
            if (propertyPath.endsWith(".merchantId") && isNullOrBlank(violation.getInvalidValue())) {
                return missingHeaderError("X-Merchant-Id");
            }
            if (propertyPath.endsWith(".adminId") && isNullOrBlank(violation.getInvalidValue())) {
                return missingHeaderError("X-Admin-Id");
            }
        }
        return error("INVALID_REQUEST", "请求格式非法或无法解析", Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        List<Map<String, Object>> fields = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError(
                        fieldError.getField(),
                        fieldError.getCode(),
                        fieldError.getDefaultMessage()
                ))
                .toList();

        return error("INVALID_REQUEST_BODY", "请求体校验失败", Map.of("fields", fields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
        return error("INVALID_REQUEST", "请求格式非法或无法解析", Map.of("path", "$"));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleUnexpectedException(Exception exception) {
        log.error("Unhandled exception caught by GlobalExceptionHandler", exception);
        return error("INTERNAL_ERROR", "服务器内部错误", Map.of());
    }

    private Map<String, Object> missingHeaderError(String headerName) {
        String code = switch (headerName) {
            case "X-Merchant-Id" -> "MISSING_MERCHANT_ID";
            case "X-Buyer-Id" -> "MISSING_BUYER_ID";
            case "X-Admin-Id" -> "MISSING_ADMIN_ID";
            default -> "MISSING_REQUIRED_HEADER";
        };
        return error(code, "缺少必填请求头: " + headerName, Map.of("header", headerName));
    }

    private Map<String, Object> error(String code, String message, Map<String, Object> meta) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("code", safeString(code));
        detail.put("message", safeString(message));
        detail.put("meta", meta == null ? Map.of() : meta);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("detail", detail);
        return body;
    }

    private boolean isNullOrBlank(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String stringValue) {
            return stringValue.isBlank();
        }
        return false;
    }

    private Map<String, Object> fieldError(String field, String reason, String message) {
        Map<String, Object> fieldError = new LinkedHashMap<>();
        fieldError.put("field", safeString(field));
        fieldError.put("reason", safeString(reason));
        fieldError.put("message", safeString(message));
        return fieldError;
    }

    private String safeString(String value) {
        return Objects.toString(value, "");
    }
}
