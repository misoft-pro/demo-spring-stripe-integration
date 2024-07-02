package pro.misoft.demostripeintegration.errorhandling;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.context.support.AbstractResourceBasedMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import pro.misoft.demostripeintegration.common.BusinessException;
import pro.misoft.demostripeintegration.common.Id;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

@Component
public class ApiErrorFactory {

    private final AbstractResourceBasedMessageSource msgSource;

    ApiErrorFactory(AbstractResourceBasedMessageSource msgSource) {
        this.msgSource = msgSource;
    }

    public ApiError error(Exception ex) {
        RestResponseSupport.Keys responseSupport = RestResponseSupport.getKeys(ex);
        Object[] args = null;
        if (ex instanceof BusinessException businessException) {
            args = businessException.getArgs();
        }
        String message = msgSource.getMessage(responseSupport.i18nKey(), args, ex.getMessage(), Locale.ENGLISH);
        return new ApiError(responseSupport.httpStatus(), responseSupport.internalCode(), message);
    }

    ApiError error(MethodArgumentNotValidException ex) {
        return error(ex.getBindingResult().getFieldErrors());
    }

    private static ApiError error(List<FieldError> fieldErrors) {
        return new ApiError(HttpStatus.BAD_REQUEST.value(), "" + 4002, "Input fields contain errors", Id.randomUUID(), fieldErrors.stream().map(fe ->
                new ApiSubError(fe.getObjectName(), fe.getField(), fe.getRejectedValue(), fe.getDefaultMessage() == null ? "" : fe.getDefaultMessage())).toList());
    }

    ApiError error(ConstraintViolationException ex) {
        var result = new HashMap<String, String>();
        for (ConstraintViolation<?> cv : ex.getConstraintViolations()) {
            if (result.containsKey(cv.getPropertyPath().toString())) {
                if (contains(cv, NotEmpty.class) || contains(cv, NotNull.class) || contains(cv, NotBlank.class)) {
                    result.put(cv.getPropertyPath().toString(), cv.getMessageTemplate());
                }
            } else {
                result.put(cv.getPropertyPath().toString(), cv.getMessageTemplate());
            }
        }
        List<FieldError> errors = ex.getConstraintViolations()
                .stream()
                .filter(cv -> result.containsKey(cv.getPropertyPath().toString()) && result.get(cv.getPropertyPath().toString()).contains(cv.getMessageTemplate()))
                .map(cv -> new FieldError(cv.getRootBeanClass().getSimpleName(), cv.getPropertyPath().toString(), cv.getInvalidValue() != null ? cv.getInvalidValue().toString() : "",
                        true, null, null, getCvMsg(cv.getRootBeanClass().getSimpleName().toLowerCase() + "." + cv.getPropertyPath().toString().toLowerCase(), cv.getMessage())))
                .toList();
        return error(errors);
    }

    private static boolean contains(ConstraintViolation<?> cv, Class<?> claz) {
        return cv.getMessageTemplate().contains(claz.getSimpleName().toLowerCase());
    }

    private String getCvMsg(String key, String defaultMessage) {
        return msgSource.getMessage("errors." + key, null, defaultMessage, Locale.ENGLISH);
    }
}
