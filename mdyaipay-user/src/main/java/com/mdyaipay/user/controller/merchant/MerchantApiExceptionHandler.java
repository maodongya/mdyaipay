package com.mdyaipay.user.controller.merchant;

import com.mdyaipay.tools.model.ApiResponse;
import com.mdyaipay.user.service.merchant.MerchantBusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * HTTP 层兜底：参数类异常仍返回 JSON；业务异常通常已由 Facade 封装为 {@link ApiResponse}。
 */
@RestControllerAdvice
public class MerchantApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(10002, ex.getMessage()));
    }

    @ExceptionHandler(MerchantBusinessException.class)
    public ResponseEntity<ApiResponse<Void>> merchantBusiness(MerchantBusinessException ex) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.fail(ex.getErrorCode(), ex.getMessage()));
    }
}
