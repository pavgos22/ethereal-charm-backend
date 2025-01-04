package com.ethereal.order.repository;

import com.ethereal.order.entity.Order;
import com.ethereal.order.entity.OrderItems;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemRepository extends JpaRepository<OrderItems, Long> {
    List<OrderItems> findOrderItemsByOrder(Order order);
}
