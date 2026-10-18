package com.portfolio.drivecare.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.HandlerMapping;
import com.portfolio.drivecare.observability.ExceptionDiagnostics;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestIdFilter extends OncePerRequestFilter {
	private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

	static final String HEADER_NAME = "X-Request-ID";
	private static final String MDC_KEY = "requestId";
	private final long slowRequestMs;
	RequestIdFilter(@Value("${app.logging.slow-request-ms:1000}") long slowRequestMs) {
		this.slowRequestMs = slowRequestMs;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String supplied = request.getHeader(HEADER_NAME);
		String requestId = supplied != null && supplied.matches("[a-fA-F0-9]{32}|[a-fA-F0-9]{8}(-[a-fA-F0-9]{4}){3}-[a-fA-F0-9]{12}")
				? supplied : UUID.randomUUID().toString();
		long started = System.nanoTime();
		boolean failed = false;
		String previous = MDC.get(MDC_KEY);
		MDC.put(MDC_KEY, requestId);
		response.setHeader(HEADER_NAME, requestId);
		try {
			filterChain.doFilter(request, response);
		}
		catch (ServletException | IOException | RuntimeException exception) {
			failed = true;
			log.error("event=request_unhandled diagnostic={}", ExceptionDiagnostics.describe(exception));
			throw exception;
		}
		finally {
			long durationMs = (System.nanoTime() - started) / 1_000_000;
			int status = failed ? 500 : response.getStatus();
			Object route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
			// Handler patterns contain no query strings, user input or concrete record IDs.
			String message = "event=request_completed method={} route={} status={} durationMs={}";
			Object[] values = {request.getMethod(), route == null ? "unmapped" : route, status, durationMs};
			if (status >= 500) log.error(message, values);
			else if (durationMs >= slowRequestMs) log.warn(message, values);
			else if (status >= 400 || !"GET".equals(request.getMethod())) log.info(message, values);
			else log.debug(message, values);
			if (previous == null) MDC.remove(MDC_KEY); else MDC.put(MDC_KEY, previous);
		}
	}
}
