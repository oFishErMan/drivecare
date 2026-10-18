package com.portfolio.drivecare.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.portfolio.drivecare.observability.ExceptionDiagnostics;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
class ApiExceptionHandler {
	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
		return ApiProblems.create(HttpStatus.BAD_REQUEST, "INVALID_INPUT", exception.getMessage());
	}

	@ExceptionHandler(ResponseStatusException.class)
	ProblemDetail handleResponseStatus(ResponseStatusException exception) {
		String detail = exception.getReason() == null ? "요청을 처리할 수 없습니다." : exception.getReason();
		return ApiProblems.create(exception.getStatusCode(), "REQUEST_REJECTED", detail);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		for (FieldError error : exception.getBindingResult().getFieldErrors()) {
			fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
		}
		ProblemDetail problem = ApiProblems.create(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "입력값을 확인해주세요.");
		problem.setProperty("fieldErrors", fieldErrors);
		return problem;
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail handleMalformedBody(HttpMessageNotReadableException exception) {
		return ApiProblems.create(HttpStatus.BAD_REQUEST, "INVALID_JSON", "입력 형식이 올바르지 않습니다. 날짜와 숫자를 확인해 주세요.");
	}

	@ExceptionHandler(AuthenticationException.class)
	ProblemDetail handleAuthentication(AuthenticationException exception) {
		return ApiProblems.create(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", "이메일 또는 비밀번호가 일치하지 않습니다.");
	}

	@ExceptionHandler(AccessDeniedException.class)
	ProblemDetail handleAccessDenied(AccessDeniedException exception) {
		return ApiProblems.create(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "요청을 처리할 권한이 없습니다.");
	}

	@ExceptionHandler({DataIntegrityViolationException.class, ConcurrencyFailureException.class})
	ProblemDetail handleConflict(RuntimeException exception) {
		log.warn("event=data_conflict errorType={}", exception.getClass().getSimpleName());
		return ApiProblems.create(HttpStatus.CONFLICT, "DATA_CONFLICT", "이미 처리되었거나 다른 요청으로 변경된 정보입니다. 새로고침한 뒤 확인해 주세요.");
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception exception) {
		if (exception instanceof ErrorResponse error && error.getStatusCode().is4xxClientError()) {
			return ApiProblems.create(error.getStatusCode(), "INVALID_REQUEST", "요청 형식이나 경로를 확인해 주세요.");
		}
		log.error("event=unexpected_error diagnostic={}", ExceptionDiagnostics.describe(exception));
		return ApiProblems.create(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버가 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.");
	}
}
