package pro.misoft.demostripeintegration.common;

public class BusinessException extends RuntimeException {
    private final Object[] args;

    public BusinessException(String message, Object[] args) {
        super(message);
        this.args = args;
    }

    public Object[] getArgs() {
        return args;
    }
}
