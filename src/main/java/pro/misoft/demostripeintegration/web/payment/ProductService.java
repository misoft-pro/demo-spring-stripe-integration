package pro.misoft.demostripeintegration.web.payment;

import com.stripe.model.Product;

public interface ProductService {
    /**
     * Finds a product by its ID.
     *
     * @param id the ID of the product to find
     * @return the found product or null if not found
     */
     Product findProduct(String id);
}
