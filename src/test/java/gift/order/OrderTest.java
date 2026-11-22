package gift.order;

import gift.option.Option;
import gift.product.Product;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {
    @Test
    void calculatesTotalPriceFromProductPriceAndOrderQuantity() {
        var product = new Product("상품", 12500, null, null);
        var option = new Option(product, "옵션", 10);
        var order = new Order(option, 1L, 2, null);

        assertThat(order.totalPrice()).isEqualTo(25000);
    }
}
