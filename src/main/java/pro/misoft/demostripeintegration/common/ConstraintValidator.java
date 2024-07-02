package pro.misoft.demostripeintegration.common;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.executable.ExecutableValidator;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ConstraintValidator {

    private ConstraintValidator() {
    }

    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private static final ExecutableValidator execValidator = Validation.buildDefaultValidatorFactory().getValidator().forExecutables();

    /**
     * Validates an object using default validator which doesn't include localised error message interpolation
     * from <code>message</code> annotation parameter like `@NotEmpty(message = "{errors.token.password.notempty}")`
     *
     * @param object
     * @param <T>
     */
    public static <T> void validate(T object) {
        validate(object, validator);
    }

    public static <T> void validate(T object, Validator validator) {
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    public static <T> void validateMethod(T object, String methodName, String[] parameterValues) {
        Method method;
        try {
            List<? extends Class<? extends String>> classes = Arrays.stream(parameterValues).map(String::getClass).toList();
            method = object.getClass().getMethod(methodName, classes.toArray(Class[]::new));
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
        Set<ConstraintViolation<T>> violations = execValidator.validateParameters(object, method, parameterValues);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
