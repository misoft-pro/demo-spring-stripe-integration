package pro.misoft.demostripeintegration.web.payment;

import com.stripe.model.Product;

import java.util.List;

public record PaymentRequest(
        List<Product> items,
        String customerName,
        String customerEmail,
        String subscriptionId,
        boolean invoiceNeeded) {
}
