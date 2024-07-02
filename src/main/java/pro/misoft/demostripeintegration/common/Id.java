package pro.misoft.demostripeintegration.common;

import java.util.UUID;

public record Id(String value) {

    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    public static Id random() {
        return new Id(UUID.randomUUID().toString());
    }
}
