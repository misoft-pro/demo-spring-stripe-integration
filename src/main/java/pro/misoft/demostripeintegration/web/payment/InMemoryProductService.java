package pro.misoft.demostripeintegration.web.payment;

import com.stripe.model.Price;
import com.stripe.model.Product;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
public class InMemoryProductService implements ProductService {

    private static final Map<String, Product> products = new HashMap<>();

    static {

        Product product = new Product();
        Price price = new Price();

        // Book A
        product.setName("Book A");
        product.setId("bookA");
        price.setCurrency("usd");
        price.setUnitAmountDecimal(BigDecimal.valueOf(2999));
        product.setDefaultPriceObject(price);
        products.put("bookA", product);

        // Book B
        product = new Product();
        price = new Price();
        product.setName("Book B");
        product.setId("bookB");
        price.setCurrency("usd");
        price.setUnitAmountDecimal(BigDecimal.valueOf(3999));
        product.setDefaultPriceObject(price);
        products.put("bookB", product);
    }

    @Override
    public Product findProduct(String id) {
        Product product = products.get(id);
        if (product == null) {
            throw new IllegalArgumentException("Product [%s] not found".formatted(id));
        }
        return product;
    }

}
