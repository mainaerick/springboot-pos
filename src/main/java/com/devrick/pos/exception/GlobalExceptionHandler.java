package com.devrick.pos.exception;

import com.devrick.pos.exception.user.DuplicateEmailException;
import com.devrick.pos.exception.user.UserNotFoundException;
import com.devrick.pos.exception.customer.CustomerNotFoundException;
import com.devrick.pos.exception.customer.DuplicateCustomerCodeException;
import com.devrick.pos.exception.branch.BranchNotFoundException;
import com.devrick.pos.exception.branch.DuplicateBranchCodeException;
import com.devrick.pos.exception.branch.DuplicateBranchNameException;
import com.devrick.pos.exception.productcategory.DuplicateProductCategoryCodeException;
import com.devrick.pos.exception.productcategory.DuplicateProductCategoryNameException;
import com.devrick.pos.exception.productcategory.ProductCategoryNotFoundException;
import com.devrick.pos.exception.supplier.DuplicateSupplierCodeException;
import com.devrick.pos.exception.supplier.SupplierNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request, null);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(
            DuplicateEmailException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request, null);
    }

    @ExceptionHandler(BranchNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBranchNotFound(
            BranchNotFoundException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request, null);
    }

    @ExceptionHandler(DuplicateBranchCodeException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateBranchCode(
            DuplicateBranchCodeException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request, null);
    }

    @ExceptionHandler(DuplicateBranchNameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateBranchName(
            DuplicateBranchNameException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request, null);
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCustomerNotFound(
            CustomerNotFoundException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request, null);
    }

    @ExceptionHandler(DuplicateCustomerCodeException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateCustomerCode(
            DuplicateCustomerCodeException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request, null);
    }

    @ExceptionHandler(SupplierNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSupplierNotFound(
            SupplierNotFoundException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request, null);
    }

    @ExceptionHandler(DuplicateSupplierCodeException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateSupplierCode(
            DuplicateSupplierCodeException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request, null);
    }

    @ExceptionHandler(ProductCategoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductCategoryNotFound(
            ProductCategoryNotFoundException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request, null);
    }

    @ExceptionHandler(DuplicateProductCategoryCodeException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateProductCategoryCode(
            DuplicateProductCategoryCodeException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request, null);
    }

    @ExceptionHandler(DuplicateProductCategoryNameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateProductCategoryName(
            DuplicateProductCategoryNameException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException exception, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                Objects.requireNonNullElse(exception.getMessage(), "Authentication failed"),
                request,
                null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException exception, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                Objects.requireNonNullElse(exception.getMessage(), "Access denied"),
                request,
                null);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(
            UsernameNotFoundException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.put(
                    fieldError.getField(), Objects.requireNonNullElse(fieldError.getDefaultMessage(), "Invalid value"));
        }

        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        String message = exception.getMostSpecificCause() == null
                ? "Invalid request body"
                : Objects.requireNonNullElse(exception.getMostSpecificCause().getMessage(), "Invalid request body");
        return buildResponse(HttpStatus.BAD_REQUEST, message, request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), request, null);
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status, String message, HttpServletRequest request, Map<String, String> fieldErrors) {
        ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.name())
                .message(message)
                .path(request.getRequestURI());

        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            builder.fieldErrors(fieldErrors);
        }

        return ResponseEntity.status(status).body(builder.build());
    }
}
