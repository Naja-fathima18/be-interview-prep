package com.example.beinterviewprep.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.PathContainer;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

  static final String LIMIT_HEADER = "X-RateLimit-Limit";
  static final String REMAINING_HEADER = "X-RateLimit-Remaining";

  private final SlidingWindowRateLimiter limiter;
  private final RateLimitProperties properties;
  private final ObjectMapper objectMapper;
  private final List<PathPattern> protectedPaths;

  public RateLimitFilter(
      SlidingWindowRateLimiter limiter, RateLimitProperties properties, ObjectMapper objectMapper) {
    this.limiter = limiter;
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.protectedPaths =
        properties.paths().stream().map(PathPatternParser.defaultInstance::parse).toList();
  }

  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    String path = request.getRequestURI().substring(request.getContextPath().length());
    PathContainer container = PathContainer.parsePath(path);
    return protectedPaths.stream().noneMatch(pattern -> pattern.matches(container));
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain)
      throws ServletException, IOException {
    String apiKey = request.getHeader(properties.header());
    if (apiKey == null || apiKey.isBlank()) {
      writeProblem(request, response, missingApiKeyProblem());
      return;
    }
    RateLimitDecision decision = limiter.tryAcquire(apiKey.strip());
    response.setHeader(LIMIT_HEADER, String.valueOf(decision.limit()));
    response.setHeader(REMAINING_HEADER, String.valueOf(decision.remaining()));
    if (!decision.allowed()) {
      log.debug("Rate limit exceeded on {}", request.getRequestURI());
      response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
      writeProblem(request, response, tooManyRequestsProblem(decision));
      return;
    }
    chain.doFilter(request, response);
  }

  private ProblemDetail missingApiKeyProblem() {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED, "Missing API key in header '" + properties.header() + "'");
    problem.setTitle("Unauthorized");
    return problem;
  }

  private ProblemDetail tooManyRequestsProblem(RateLimitDecision decision) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.TOO_MANY_REQUESTS,
            "Rate limit of "
                + decision.limit()
                + " requests per "
                + properties.window().toSeconds()
                + " seconds exceeded. Retry after "
                + decision.retryAfterSeconds()
                + " seconds.");
    problem.setTitle("Too many requests");
    problem.setProperty("retryAfterSeconds", decision.retryAfterSeconds());
    return problem;
  }

  private void writeProblem(
      HttpServletRequest request, HttpServletResponse response, ProblemDetail problem)
      throws IOException {
    problem.setInstance(URI.create(request.getRequestURI()));
    response.setStatus(problem.getStatus());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), problem);
  }
}
