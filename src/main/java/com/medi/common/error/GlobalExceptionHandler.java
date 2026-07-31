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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	private final boolean debug;

	GlobalExceptionHandler(@Value("${debug:false}") boolean debug) {
		this.debug = debug;
	}

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ApiResponse.Failure> handleApiException(ApiException exception, HttpServletRequest request) {
		return error(exception.errorCode(), exception.getMessage(), exception.details(), request);
	}

	@ExceptionHandler(RateLimitException.class)
	ResponseEntity<ApiResponse.Failure> handleRateLimitException(
		RateLimitException exception,
		HttpServletRequest request
	) {
		ApiError error = ApiError.of(exception.errorCode(), exception.getMessage(), exception.details());
		return ResponseEntity
			.status(exception.errorCode().httpStatus())
			.header(HttpHeaders.RETRY_AFTER, Long.toString(exception.retryAfterSeconds()))
			.body(ApiResponse.error(error, RequestTrace.traceId(request)));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiResponse.Failure> handleMethodArgumentNotValid(
		MethodArgumentNotValidException exception,
		HttpServletRequest request
	) {
		return validationError(exception.getBindingResult(), request);
	}

	@ExceptionHandler(BindException.class)
	ResponseEntity<ApiResponse.Failure> handleBindException(BindException exception, HttpServletRequest request) {
		return validationError(exception.getBindingResult(), request);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	ResponseEntity<ApiResponse.Failure> handleHandlerMethodValidation(
		HandlerMethodValidationException exception,
		HttpServletRequest request
	) {
		return error(ErrorCode.INVALID_REQUEST, request);
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

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	ResponseEntity<ApiResponse.Failure> handleMaxUploadSizeExceeded(
		MaxUploadSizeExceededException exception,
		HttpServletRequest request
	) {
		return error(ErrorCode.PAYLOAD_TOO_LARGE, request);
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

	@ExceptionHandler(AuthenticationException.class)
	ResponseEntity<ApiResponse.Failure> handleAuthentication(
		AuthenticationException exception,
		HttpServletRequest request
	) {
		return error(ErrorCode.UNAUTHORIZED, request);
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ApiResponse.Failure> handleAccessDenied(
		AccessDeniedException exception,
		HttpServletRequest request
	) {
		return error(ErrorCode.FORBIDDEN, request);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ApiResponse.Failure> handleMethodNotAllowed(
		HttpRequestMethodNotSupportedException exception,
		HttpServletRequest request
	) {
		return error(ErrorCode.METHOD_NOT_ALLOWED, request);
	}

	@ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
	ResponseEntity<ApiResponse.Failure> handleNotFound(Exception exception, HttpServletRequest request) {
		return error(ErrorCode.NOT_FOUND, request);
	}

	@ExceptionHandler(ResponseStatusException.class)
	ResponseEntity<ApiResponse.Failure> handleResponseStatus(
		ResponseStatusException exception,
		HttpServletRequest request
	) {
		ErrorCode errorCode = switch (exception.getStatusCode().value()) {
			case 401 -> ErrorCode.UNAUTHORIZED;
			case 403 -> ErrorCode.FORBIDDEN;
			case 404 -> ErrorCode.NOT_FOUND;
			case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
			case 419 -> ErrorCode.TOKEN_ERROR;
			case 422 -> ErrorCode.INVALID_REQUEST;
			case 429 -> ErrorCode.RATE_LIMITED;
			default -> ErrorCode.INTERNAL_ERROR;
		};
		return error(errorCode, request);
	}

	@ExceptionHandler(DataAccessException.class)
	ResponseEntity<ApiResponse.Failure> handleDataAccess(
		DataAccessException exception,
		HttpServletRequest request
	) {
		log.error("데이터베이스 예외가 발생했습니다. traceId={}", RequestTrace.traceId(request), exception);
		return error(ErrorCode.DB_ERROR, debugDetails(exception), request);
	}

	@ExceptionHandler(InternalApplicationException.class)
	ResponseEntity<ApiResponse.Failure> handleInternalApplication(
		InternalApplicationException exception,
		HttpServletRequest request
	) {
		log.error("내부 애플리케이션 예외가 발생했습니다. traceId={}", RequestTrace.traceId(request), exception);
		return error(ErrorCode.INTERNAL_ERROR, debugDetails(exception), request);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiResponse.Failure> handleException(Exception exception, HttpServletRequest request) {
		log.error("처리되지 않은 API 예외가 발생했습니다. traceId={}", RequestTrace.traceId(request), exception);
		return error(ErrorCode.INTERNAL_ERROR, debugDetails(exception), request);
	}

	private ResponseEntity<ApiResponse.Failure> validationError(
		BindingResult bindingResult,
		HttpServletRequest request
	) {
		Map<String, List<String>> errors = new LinkedHashMap<>();
		bindingResult.getFieldErrors().forEach(fieldError ->
			errors.computeIfAbsent(fieldError.getField(), key -> new ArrayList<>()).add(fieldError.getDefaultMessage())
		);
		bindingResult.getGlobalErrors().forEach(globalError ->
			errors.computeIfAbsent(globalError.getObjectName(), key -> new ArrayList<>())
				.add(globalError.getDefaultMessage())
		);
		return error(ErrorCode.INVALID_REQUEST, Map.of("errors", errors), request);
	}

	private Map<String, String> debugDetails(Exception exception) {
		if (!debug) {
			return null;
		}
		Map<String, String> details = new LinkedHashMap<>();
		details.put("exception", exception.getClass().getName());
		details.put("message", exception.getMessage() == null ? "" : exception.getMessage());
		return details;
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
