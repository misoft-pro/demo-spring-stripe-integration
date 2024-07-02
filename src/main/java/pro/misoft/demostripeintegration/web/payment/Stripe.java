package pro.misoft.demostripeintegration.web.payment;

import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.param.*;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class Stripe {

    Payments payments;
    Customers customers;

    public Stripe(ProductService productService, @Value("${stripe.api.key}") String stripeApiKey) {
        payments = new Payments(productService);
        customers = new Customers();
        com.stripe.Stripe.apiKey = stripeApiKey;
    }

    static class Customers {
        private Customers() {
        }

        @SneakyThrows
        public Customer findCustomerByEmail(String email) {
            CustomerSearchParams params =
                    CustomerSearchParams
                            .builder()
                            .setQuery("email:'" + email + "'")
                            .build();

            CustomerSearchResult result = Customer.search(params);

            return !result.getData().isEmpty() ? result.getData().getFirst() : null;
        }

        @SneakyThrows
        public Customer findOrCreateCustomer(String email, String name) {
            CustomerSearchParams params =
                    CustomerSearchParams
                            .builder()
                            .setQuery("email:'" + email + "'")
                            .build();

            CustomerSearchResult result = Customer.search(params);

            Customer customer;

            if (result.getData().isEmpty()) {

                CustomerCreateParams customerCreateParams = CustomerCreateParams.builder()
                        .setName(name)
                        .setEmail(email)
                        .build();

                customer = Customer.create(customerCreateParams);
            } else {
                customer = result.getData().getFirst();
            }

            return customer;
        }
    }

    static class Payments {
        private final ProductService productService;

        private Payments(ProductService productService) {
            this.productService = productService;
        }

        public PaymentIntent createPaymentIntent(PaymentRequest request, Customer customer) {
            if (!request.invoiceNeeded()) {
                return createDirectPaymentIntent(request, customer);
            } else {
                return createInvoicePaymentIntent(request, customer);
            }
        }

        @SneakyThrows
        private PaymentIntent createDirectPaymentIntent(PaymentRequest request, Customer customer) {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(Long.parseLong(calculateOrderAmount(request.items())))
                    .setCurrency("usd")
                    .setCustomer(customer.getId())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .build();

            return PaymentIntent.create(params);
        }

        @SneakyThrows
        private PaymentIntent createInvoicePaymentIntent(PaymentRequest request, Customer customer) {
            InvoiceCreateParams invoiceCreateParams = InvoiceCreateParams.builder()
                    .setCustomer(customer.getId())
                    .build();

            Invoice invoice = Invoice.create(invoiceCreateParams);

            for (Product product : request.items()) {
                Product stripeProduct = findOrCreateProduct(product);
                createInvoiceItem(invoice, customer, product, stripeProduct);
            }

            invoice = invoice.finalizeInvoice();
            return PaymentIntent.retrieve(invoice.getPaymentIntent());
        }

        @SneakyThrows
        private void createInvoiceItem(Invoice invoice, Customer customer, Product product, Product stripeProduct) {
            InvoiceItemCreateParams invoiceItemCreateParams = InvoiceItemCreateParams.builder()
                    .setInvoice(invoice.getId())
                    .setQuantity(1L)
                    .setCustomer(customer.getId())
                    .setPriceData(
                            InvoiceItemCreateParams.PriceData.builder()
                                    .setProduct(stripeProduct.getId())
                                    .setCurrency(productService.findProduct(product.getId()).getDefaultPriceObject().getCurrency())
                                    .setUnitAmountDecimal(productService.findProduct(product.getId()).getDefaultPriceObject().getUnitAmountDecimal())
                                    .build())
                    .build();

            InvoiceItem.create(invoiceItemCreateParams);
        }

        @SneakyThrows
        private Product findOrCreateProduct(Product product) {
            ProductSearchResult results = Product.search(ProductSearchParams.builder()
                    .setQuery("metadata['app_id']:'" + product.getId() + "'")
                    .build());

            if (results.getData().isEmpty()) {
                ProductCreateParams productCreateParams = ProductCreateParams.builder()
                        .setName(product.getName())
                        .putMetadata("app_id", product.getId())
                        .build();

                return Product.create(productCreateParams);
            } else {
                return results.getData().getFirst();
            }
        }

        @SneakyThrows
        public Session createHostedSession(PaymentRequest paymentRequest, Customer customer, String clientBaseURL) {
            SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setCustomer(customer.getId())
                    .setSuccessUrl(clientBaseURL + "/success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(clientBaseURL + "/failure");

            addLineItems(paymentRequest, paramsBuilder);

            if (paymentRequest.invoiceNeeded()) {
                paramsBuilder.setInvoiceCreation(SessionCreateParams.InvoiceCreation.builder().setEnabled(true).build());
            }

            return Session.create(paramsBuilder.build());
        }

        @SneakyThrows
        public Session createSubscriptionSession(PaymentRequest paymentRequest, Customer customer, String clientBaseURL, boolean trial) {
            SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(customer.getId())
                    .setSuccessUrl(clientBaseURL + "/success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(clientBaseURL + "/failure");

            if (trial) {
                paramsBuilder.setSubscriptionData(SessionCreateParams.SubscriptionData.builder().setTrialPeriodDays(30L).build());
            }

            addLineItems(paymentRequest, paramsBuilder);

            return Session.create(paramsBuilder.build());
        }

        private void addLineItems(PaymentRequest paymentRequest, SessionCreateParams.Builder paramsBuilder) {
            for (Product product : paymentRequest.items()) {
                paramsBuilder.addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .putMetadata("app_id", product.getId())
                                                                .setName(product.getName())
                                                                .build()
                                                )
                                                .setCurrency(productService.findProduct(product.getId()).getDefaultPriceObject().getCurrency())
                                                .setUnitAmountDecimal(productService.findProduct(product.getId()).getDefaultPriceObject().getUnitAmountDecimal())
                                                .setRecurring(SessionCreateParams.LineItem.PriceData.Recurring.builder().setInterval(SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH).build())
                                                .build())
                                .build());
            }
        }

        @SneakyThrows
        public Subscription cancelSubscription(String subscriptionId) {
            Subscription subscription = Subscription.retrieve(subscriptionId);
            return subscription.cancel();
        }

        @SneakyThrows
        public List<Map<String, String>> listSubscriptions(String customerId) {
            SubscriptionCollection subscriptions = Subscription.list(SubscriptionListParams.builder()
                    .setCustomer(customerId)
                    .build());

            return getSubscriptionDetails(subscriptions);
        }

        @SneakyThrows
        private List<Map<String, String>> getSubscriptionDetails(SubscriptionCollection subscriptions) {
            List<Map<String, String>> response = new ArrayList<>();
            for (Subscription subscription : subscriptions.getData()) {
                SubscriptionItemCollection items = SubscriptionItem.list(SubscriptionItemListParams.builder()
                        .setSubscription(subscription.getId())
                        .addExpand("data.price.product")
                        .build());

                for (SubscriptionItem item : items.getData()) {
                    response.add(createSubscriptionMap(subscription, item));
                }
            }
            return response;
        }

        private Map<String, String> createSubscriptionMap(Subscription subscription, SubscriptionItem item) {
            Map<String, String> map = new HashMap<>();
            map.put("appProductId", item.getPrice().getProductObject().getMetadata().get("app_id"));
            map.put("subscriptionId", subscription.getId());
            map.put("subscribedOn", new SimpleDateFormat("dd/MM/yyyy").format(new Date(subscription.getStartDate() * 1000)));
            map.put("nextPaymentDate", new SimpleDateFormat("dd/MM/yyyy").format(new Date(subscription.getCurrentPeriodEnd() * 1000)));
            map.put("price", item.getPrice().getUnitAmountDecimal().toString());

            if (subscription.getTrialEnd() != null && new Date(subscription.getTrialEnd() * 1000).after(new Date())) {
                map.put("trialEndsOn", new SimpleDateFormat("dd/MM/yyyy").format(new Date(subscription.getTrialEnd() * 1000)));
            }
            return map;
        }

        @SneakyThrows
        public List<Map<String, String>> listInvoices(String customerId) {
            Map<String, Object> invoiceSearchParams = new HashMap<>();
            invoiceSearchParams.put("customer", customerId);
            InvoiceCollection invoices = Invoice.list(invoiceSearchParams);

            List<Map<String, String>> response = new ArrayList<>();
            for (Invoice invoice : invoices.getData()) {
                Map<String, String> map = new HashMap<>();
                map.put("number", invoice.getNumber());
                map.put("amount", String.valueOf((invoice.getTotal() / 100f)));
                map.put("url", invoice.getInvoicePdf());
                response.add(map);
            }
            return response;
        }

        private String calculateOrderAmount(List<Product> items) {
            BigDecimal total = BigDecimal.ZERO;
            for (Product item : items) {
                total = total.add(productService.findProduct(item.getId()).getDefaultPriceObject().getUnitAmountDecimal());
            }
            return total.toString();
        }
    }
}
