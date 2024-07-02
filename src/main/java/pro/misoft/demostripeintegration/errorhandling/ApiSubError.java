package pro.misoft.demostripeintegration.errorhandling;

public record ApiSubError(
        String objectName,
        String fieldName,
        Object rejectedValue,
        String message) {
}
