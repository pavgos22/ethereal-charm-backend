package com.ethereal.order.translators;

import com.ethereal.order.entity.OrderItemDTO;
import com.ethereal.order.entity.OrderItems;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderItemTranslator {

    public OrderItemDTO toDto(OrderItems item) {
        return new OrderItemDTO(
                item.getProduct(),
                item.getUuid(),
                item.getName(),
                item.getQuantity(),
                item.getPriceUnit(),
                item.getPriceSummary()
        );
    }

    public List<OrderItemDTO> toDtoList(List<OrderItems> items) {
        return items.stream()
                .map(this::toDto)
                .toList();
    }
}

