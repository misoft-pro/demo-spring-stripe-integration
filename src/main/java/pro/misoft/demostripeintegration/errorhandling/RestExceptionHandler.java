package pro.misoft.demostripeintegration.errorhandling;


import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;


@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
class RestExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);
    public static final String CONTENT_TYPE = "Content-type";
    public static final String MSG_TEMPLATE = "About to handle an exception";

    private final ApiErrorFactory errorFactory;

    RestExceptionHandler(ApiErrorFactory errorFactory) {
        this.errorFactory = errorFactory;
    }

    @ExceptionHandler(value = {ConstraintViolationException.class})
    public ResponseEntity<ApiError> handleConstraintViolationException(ConstraintViolationException ex) {
        log.debug(MSG_TEMPLATE, ex);
        return ResponseEntity.badRequest().header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).body(errorFactory.error(ex));
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<ApiError> handleExceptions(Exception ex, WebRequest request) {
        log.info(MSG_TEMPLATE, ex);
        ApiError apiError = errorFactory.error(ex);
        return new ResponseEntity<>(apiError, HttpStatus.valueOf(apiError.httpStatus()));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NotNull MethodArgumentNotValidException ex, @NotNull HttpHeaders headers, @NotNull HttpStatusCode status, @NotNull WebRequest request) {
        log.info(MSG_TEMPLATE, ex);
        return ResponseEntity.badRequest().header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).body(errorFactory.error(ex));
    }
}