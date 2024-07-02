package pro.misoft.demostripeintegration.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationPredicate;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;
import org.springframework.http.server.observation.ServerRequestObservationContext;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class TracingConfig {

    @Bean
    public Filter traceIdInResponseFilter(Tracer tracer) {
        return (request, response, chain) -> {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
                // putting trace id value in [traceId] response header
                ((HttpServletResponse) response).setHeader("X-Trace-Id", currentSpan.context().traceId());
            }
            chain.doFilter(request, response);
        };
    }

    @Bean
    public ObservationPredicate noActuatorServerObservations() {
        return (name, context) -> {
            if ("http.server.requests".equals(name) && context instanceof ServerRequestObservationContext) {
                return !((ServerRequestObservationContext) context).getCarrier().getRequestURI().contains("/actuator");
            } else {
                return true;
            }
        };
    }

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(Environment env) {
        return registry -> {
            String profiles = String.join(",", env.getActiveProfiles());
            registry.config().commonTags("profile", profiles.isEmpty() ? "localhost" : profiles);
        };
    }

    /**
     * This and @EnableAspectJAutoProxy are required so that we can use the @Timed annotation
     * on methods that we want to time.
     * See: <a href="https://micrometer.io/docs/concepts#_the_timed_annotation">Micrometer AOP config</a>
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
