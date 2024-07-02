package pro.misoft.demostripeintegration.web.payment;

import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/v1/payments", consumes = MediaType.APPLICATION_JSON_VALUE)
public class PaymentApi {

    private final Stripe stripe;
    private final String clientBaseURL;

    public PaymentApi(Stripe stripe, @Value("${client.base.url}") String clientBaseURL) {
        this.stripe = stripe;
        this.clientBaseURL = clientBaseURL;
    }

    @PostMapping
    public ResponseEntity<String> intentCheckout(@RequestBody PaymentRequest request) {
        Customer customer = stripe.customers.findOrCreateCustomer(request.customerEmail(), request.customerName());
        PaymentIntent paymentIntent = stripe.payments.createPaymentIntent(request, customer);
        return ResponseEntity.ok(paymentIntent.getClientSecret());
    }

    @PostMapping("/hosted")
    public ResponseEntity<String> hostedCheckout(@RequestBody PaymentRequest paymentRequest) {
        Customer customer = stripe.customers.findOrCreateCustomer(paymentRequest.customerEmail(), paymentRequest.customerName());
        Session session = stripe.payments.createHostedSession(paymentRequest, customer, clientBaseURL);
        return ResponseEntity.ok(session.getUrl());
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<String> newSubscription(@RequestBody PaymentRequest paymentRequest) {
        Customer customer = stripe.customers.findOrCreateCustomer(paymentRequest.customerEmail(), paymentRequest.customerName());
        Session session = stripe.payments.createSubscriptionSession(paymentRequest, customer, clientBaseURL, false);
        return ResponseEntity.ok(session.getUrl());
    }

    @DeleteMapping("/subscriptions/{subscriptionId}")
    public ResponseEntity<String> cancelSubscription(@PathVariable String subscriptionId) {
        Subscription deletedSubscription = stripe.payments.cancelSubscription(subscriptionId);
        return ResponseEntity.ok(deletedSubscription.getStatus());
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<List<Map<String, String>>> viewSubscriptions(@RequestParam String customerEmail) {
        Customer customer = stripe.customers.findCustomerByEmail(customerEmail);
        List<Map<String, String>> subscriptions = stripe.payments.listSubscriptions(customer.getId());
        return ResponseEntity.ok(subscriptions);
    }

    @PostMapping("/subscriptions/trial")
    public ResponseEntity<String> newSubscriptionWithTrial(@RequestBody PaymentRequest paymentRequest) {
        Customer customer = stripe.customers.findOrCreateCustomer(paymentRequest.customerEmail(), paymentRequest.customerName());
        Session session = stripe.payments.createSubscriptionSession(paymentRequest, customer, clientBaseURL, true);
        return ResponseEntity.ok(session.getUrl());
    }

    @GetMapping("/invoices")
    public ResponseEntity<List<Map<String, String>>> listInvoices(@RequestParam String customerEmail) {
        Customer customer = stripe.customers.findCustomerByEmail(customerEmail);
        List<Map<String, String>> invoices = stripe.payments.listInvoices(customer.getId());
        return ResponseEntity.ok(invoices);
    }
}
