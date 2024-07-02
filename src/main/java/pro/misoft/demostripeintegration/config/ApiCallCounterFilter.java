package pro.misoft.demostripeintegration.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
@Order(1)  // Ensure this filter runs before other filters
public class ApiCallCounterFilter implements Filter {

    private final Counter apiCallCounter;
    private final Counter authApiCallCounter;

    @Autowired
    public ApiCallCounterFilter(MeterRegistry meterRegistry) {
        this.apiCallCounter = meterRegistry.counter("custom.api.calls.total");
        this.authApiCallCounter = meterRegistry.counter("custom.api.calls.auth");
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization logic if needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        if (!path.contains("/actuator")) {
            apiCallCounter.increment();
        }
        if (path.contains("/auth")) {
            authApiCallCounter.increment();
        }

        chain.doFilter(httpRequest, httpResponse);
    }

    @Override
    public void destroy() {
        // Cleanup logic if needed
    }
}

