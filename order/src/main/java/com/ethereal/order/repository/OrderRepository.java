package com.ethereal.order.repository;

import com.ethereal.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findOrderByOrders(String orders);

    Optional<Order> findOrderByUuid(String uuid);

    List<Order> findOrderByClient(String client);
}
