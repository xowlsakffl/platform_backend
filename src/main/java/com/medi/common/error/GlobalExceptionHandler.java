package com.medi.common.error;

import com.medi.common.web.ApiError;
import com.medi.common.web.ApiResponse;
import com.medi.common.web.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ApiResponse.Failure> handleApiException(ApiException exception, HttpServletRequest request) {
		return error(
			exception.errorCode(),
			exception.getMessage(),
			exception.details(),
			request
		);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiResponse.Failure> handleMethodArgumentNotValid(
		MethodArgumentNotValidException exception,
		HttpServletRequest request
	) {
		Map<String, List<String>> errors = new LinkedHashMap<>();

		exception.getBindingResult().getFieldErrors().forEach(error ->
			errors.computeIfAbsent(error.getField(), key -> new ArrayList<>()).add(error.getDefaultMessage())
		);

		exception.getBindingResult().getGlobalErrors().forEach(error ->
			errors.computeIfAbsent(error.getObjectName(), key -> new ArrayList<>()).add(error.getDefaultMessage())
		);

		return error(ErrorCode.INVALID_REQUEST, Map.of("errors", errors), request);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ApiResponse.Failure> handleConstraintViolation(
		ConstraintViolationException exception,
		HttpServletRequest request
	) {
		Map<String, List<String>> errors = new LinkedHashMap<>();

		exception.getConstraintViolations().forEach(violation ->
			errors.computeIfAbsent(violation.getPropertyPath().toString(), key -> new ArrayList<>())
				.add(violation.getMessage())
		);

		return error(ErrorCode.INVALID_REQUEST, Map.of("errors", errors), request);
	}

	@ExceptionHandler({
		HttpMessageConversionException.class,
		MissingServletRequestParameterException.class,
		MethodArgumentTypeMismatchException.class,
		MultipartException.class
	})
	ResponseEntity<ApiResponse.Failure> handleInvalidRequest(Exception exception, HttpServletRequest request) {
		return error(ErrorCode.INVALID_REQUEST, request);
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	ResponseEntity<ApiResponse.Failure> handleMaxUploadSizeExceeded(
		MaxUploadSizeExceededException exception,
		HttpServletRequest request
	) {
		return error(ErrorCode.PAYLOAD_TOO_LARGE, request);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ApiResponse.Failure> handleMethodNotAllowed(
		HttpRequestMethodNotSupportedException exception,
		HttpServletRequest request
	) {
		return error(ErrorCode.METHOD_NOT_ALLOWED, request);
	}

	@ExceptionHandler(NoHandlerFoundException.class)
	ResponseEntity<ApiResponse.Failure> handleNoHandlerFound(
		NoHandlerFoundException exception,
		HttpServletRequest request
	) {
		return error(ErrorCode.NOT_FOUND, request);
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ApiResponse.Failure> handleAccessDenied(
		AccessDeniedException exception,
		HttpServletRequest request
	) {
		return error(ErrorCode.FORBIDDEN, request);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiResponse.Failure> handleException(Exception exception, HttpServletRequest request) {
		log.error("처리되지 않은 API 예외가 발생했습니다. traceId={}", RequestTrace.traceId(request), exception);
		return error(ErrorCode.INTERNAL_ERROR, request);
	}

	private ResponseEntity<ApiResponse.Failure> error(ErrorCode errorCode, HttpServletRequest request) {
		return error(errorCode, errorCode.defaultMessage(), null, request);
	}

	private ResponseEntity<ApiResponse.Failure> error(ErrorCode errorCode, Object details, HttpServletRequest request) {
		return error(errorCode, errorCode.defaultMessage(), details, request);
	}

	private ResponseEntity<ApiResponse.Failure> error(
		ErrorCode errorCode,
		String message,
		Object details,
		HttpServletRequest request
	) {
		ApiError error = ApiError.of(errorCode, message, details);
		return ResponseEntity
			.status(errorCode.httpStatus())
			.body(ApiResponse.error(error, RequestTrace.traceId(request)));
	}
}
