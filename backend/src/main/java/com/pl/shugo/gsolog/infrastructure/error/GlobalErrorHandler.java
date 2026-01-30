package com.pl.shugo.gsolog.infrastructure.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.server.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global error handler for WebFlux.
 * Provides RFC7807-like ProblemDetails error responses.
 */
@Component
@Order(-2)
public class GlobalErrorHandler extends DefaultErrorAttributes {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorHandler.class);

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Throwable error = getError(request);
        HttpStatus status = determineHttpStatus(error);
        String path = request.path();

        log.error("Error handling request {}: {}", path, error.getMessage(), error);

        ErrorResponse errorResponse;

        if (error instanceof WebExchangeBindException validationError) {
            Map<String, Object> fieldErrors = validationError.getBindingResult()
                    .getFieldErrors()
                    .stream()
                    .collect(Collectors.toMap(
                            FieldError::getField,
                            fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value"
                    ));

            errorResponse = new ErrorResponse(
                    "about:blank",
                    "Validation Failed",
                    status.value(),
                    "Request validation failed",
                    path,
                    fieldErrors
            );
        } else if (error instanceof ResponseStatusException rse) {
            String detail = rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();
            errorResponse = new ErrorResponse(
                    "about:blank",
                    status.getReasonPhrase(),
                    status.value(),
                    detail,
                    path
            );
        } else {
            errorResponse = new ErrorResponse(
                    "about:blank",
                    status.getReasonPhrase(),
                    status.value(),
                    error.getMessage() != null ? error.getMessage() : "An unexpected error occurred",
                    path
            );
        }

        return convertToMap(errorResponse);
    }

    private HttpStatus determineHttpStatus(Throwable error) {
        if (error instanceof ResponseStatusException rse) {
            return HttpStatus.resolve(rse.getStatusCode().value());
        } else if (error instanceof WebExchangeBindException) {
            return HttpStatus.BAD_REQUEST;
        } else if (error instanceof IllegalArgumentException) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private Map<String, Object> convertToMap(ErrorResponse errorResponse) {
        Map<String, Object> map = new HashMap<>();
        map.put("type", errorResponse.type());
        map.put("title", errorResponse.title());
        map.put("status", errorResponse.status());
        map.put("detail", errorResponse.detail());
        map.put("instance", errorResponse.instance());
        map.put("timestamp", errorResponse.timestamp().toString());
        if (errorResponse.errors() != null) {
            map.put("errors", errorResponse.errors());
        }
        return map;
    }
}
