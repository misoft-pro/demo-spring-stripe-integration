package pro.misoft.demostripeintegration.errorhandling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import pro.misoft.demostripeintegration.common.BusinessException;

import java.util.HashMap;
import java.util.Map;

public class RestResponseSupport {

    private static final Logger log = LoggerFactory.getLogger(RestResponseSupport.class);

    private static final String COMMON_PREFIX = "common";
    private static final Keys UNSUPPORTED = new Keys(HttpStatus.SERVICE_UNAVAILABLE, COMMON_PREFIX + 5001, "errors.common");
    private static final Keys BAD_REQUEST = new Keys(HttpStatus.BAD_REQUEST, COMMON_PREFIX + 5002, "errors.common.illegalargument");
    private static final Keys UNPROCESSABLE_ENTITY = new Keys(HttpStatus.UNPROCESSABLE_ENTITY, COMMON_PREFIX + 5003, "errors.common.illegalstate");
    private static final Keys MAX_FILE_UPLOAD_SIZE = new Keys(HttpStatus.UNPROCESSABLE_ENTITY, COMMON_PREFIX + 5004, "errors.common.max-file-size");
    private static final Map<Class<? extends Exception>, Keys> exceptionsMap = new HashMap<>();

    static {
        exceptionsMap.put(BusinessException.class, UNPROCESSABLE_ENTITY);
    }

    public record Keys(HttpStatus httpStatus, String internalCode, String i18nKey) {
    }

    public static Keys getKeys(Exception exception) {
        Keys keys = exceptionsMap.get(exception.getClass());
        if (keys == null) {
            log.debug("No translation is explicitly configured for exception={}", exception.getClass().getName());
            keys = switch (exception) {
                case IllegalArgumentException illegalArgumentException -> BAD_REQUEST;
                case IllegalStateException illegalStateException -> UNPROCESSABLE_ENTITY;
                case MaxUploadSizeExceededException maxUploadSizeExceededException -> MAX_FILE_UPLOAD_SIZE;
                default -> UNSUPPORTED;
            };
        }
        return keys;
    }
}

