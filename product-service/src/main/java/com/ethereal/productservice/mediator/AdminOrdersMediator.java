package com.ethereal.productservice.mediator;

import com.ethereal.productservice.entity.SimpleProductDTO;
import com.ethereal.productservice.entity.OrderDTO;
import com.ethereal.productservice.service.AdminOrdersService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AdminOrdersMediator {

    private final AdminOrdersService orderService;

    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        List<OrderDTO> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    public ResponseEntity<List<SimpleProductDTO>> getOrderProducts(String orderUuid) {
        List<SimpleProductDTO> products = orderService.getOrderProducts(orderUuid);
        return ResponseEntity.ok(products);
    }
}

