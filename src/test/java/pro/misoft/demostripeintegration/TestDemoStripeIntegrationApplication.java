package pro.misoft.demostripeintegration;

import org.springframework.boot.SpringApplication;

public class TestDemoStripeIntegrationApplication {

	public static void main(String[] args) {
		SpringApplication.from(DemoStripeIntegrationApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
