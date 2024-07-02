package pro.misoft.demostripeintegration.errorhandling;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import pro.misoft.demostripeintegration.common.Id;

import java.util.Collections;
import java.util.List;

/**
 * Example:
 * `{
 * "httpStatus": 400,
 * "internalCode":"order-4002",
 * "errorMessage":"Input fields contain errors",
 * "traceId":"7f006775-04b5-4f81-8250-a85ffb976722",
 * "subErrors":[
 * {
 * "objectName":"orderDto",
 * "fieldName":"userId",
 * "rejectedValue":"1",
 * "message":"size must be between 36 and 36"
 * }
 * ]
 * }`
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiError(

        @NotNull
        Integer httpStatus,
        /*
          Internal internalCode to classify error

          pattern="${serviceNamePrefix}-${httpErrorCode}${sequenceNumberUniqueForServiceNameAndHttpErrorCode}".

          examples=["orderservice-4001", "orderservice-4002","orderservice-5001", "userservice-4001", "userservice-4002", "userservice-5001"]
         */
        @NotEmpty
        String internalCode,
        /*
          Human readable localized errorMessage to display on client side
         */
        @NotEmpty
        String errorMessage,
        /*
          Unique identifier of user request.
          In case of distributed architecture this identifier is passed to all downstream requests to other services.
         */
        @NotEmpty
        String traceId,
        /*
          Collect information about sub errors,
          for example specific fields of forms providing human-readable error messages for each field to guide user trough out a flow
         */
        @NotNull
        List<ApiSubError> subErrors) {

    public ApiError(HttpStatus status, String errorCode, String errorMessage, String traceId) {
        this(status.value(), errorCode, errorMessage, traceId, Collections.emptyList());
    }

    public ApiError(HttpStatus status, String errorCode, String errorMessage) {
        this(status.value(), errorCode, errorMessage, Id.randomUUID(), Collections.emptyList());
    }
}

