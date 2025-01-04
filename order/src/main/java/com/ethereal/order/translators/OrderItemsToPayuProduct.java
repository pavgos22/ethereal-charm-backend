package com.ethereal.order.translators;

import com.ethereal.order.entity.OrderItems;
import com.ethereal.order.entity.PayuProduct;
import org.springframework.stereotype.Component;

@Component
public class OrderItemsToPayuProduct {

    public PayuProduct toPayuProduct(OrderItems orderItems, double unitPrice) {
        return translate(orderItems, unitPrice);
    }

    private PayuProduct translate(OrderItems orderItems, double unitPrice) {
        if (orderItems == null)
            return null;

        PayuProduct payuProduct = new PayuProduct();
        payuProduct.setName(orderItems.getName());
        payuProduct.setQuantity(orderItems.getQuantity());
        payuProduct.setUnitPrice((long) orderItems.getPriceUnit());
        return payuProduct;
    }
}


