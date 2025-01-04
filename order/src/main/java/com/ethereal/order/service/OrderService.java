package com.ethereal.order.service;


import com.ethereal.order.entity.*;
import com.ethereal.order.entity.notify.Notify;
import com.ethereal.order.exception.CartDoesntExistException;
import com.ethereal.order.exception.EmptyCartException;
import com.ethereal.order.exception.OrderDoesntExistException;
import com.ethereal.order.exception.UknowDeliverTypException;
import com.ethereal.order.repository.DeliverRepository;
import com.ethereal.order.repository.OrderRepository;
import com.ethereal.order.translators.CartItemDTOToOrderItems;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final DeliverRepository deliverRepository;
    private final CartService cartService;
    private final ItemService itemService;
    private final PayuService payuService;
    private final CartItemDTOToOrderItems cartItemDTOToItems;
    private final EmailService emailService;
    private final AuthService authService;

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    private Order save(Order order) {
        Deliver deliver = deliverRepository.findByUuid(order.getDeliver().getUuid()).orElseThrow(UknowDeliverTypException::new);
        StringBuilder stringBuilder = new StringBuilder("ORDER/")
                .append(orderRepository.count() + 102137)
                .append("/")
                .append(LocalDate.now().getMonthValue())
                .append("/")
                .append(LocalDate.now().getYear());

        order.setUuid(UUID.randomUUID().toString());
        order.setStatus(Status.PENDING);
        order.setOrders(stringBuilder.toString());
        order.setDeliver(deliver);
        return orderRepository.saveAndFlush(order);
    }

    @Transactional
    public String createOrder(Order order, HttpServletRequest request, HttpServletResponse response) {
        List<Cookie> cookies = Arrays.stream(request.getCookies()).filter(value ->
                value.getName().equals("Authorization") || value.getName().equals("refresh")).toList();

        UserRegisterDTO userRegisterDTO = authService.getUserDetails(cookies);
        if (userRegisterDTO != null)
            order.setClient(userRegisterDTO.getLogin());
        Order finalOrder = save(order);
        AtomicReference<String> result = new AtomicReference<>();
        Arrays.stream(request.getCookies()).filter(cookie -> cookie.getName().equals("cart")).findFirst().ifPresentOrElse(value -> {
            ListCartItemDTO cart = cartService.getCart(value);
            if (cart.getCartProducts().isEmpty()) throw new EmptyCartException();
            List<OrderItems> items = new ArrayList<>();
            cart.getCartProducts().forEach(item -> {
                OrderItems orderItems = cartItemDTOToItems.toOrderItems(item);
                double unitPrice = item.getPrice();
                System.out.println("unitPrice: " + unitPrice);
                orderItems.setPriceUnit(unitPrice);
                orderItems.setPriceSummary(unitPrice * item.getQuantity());
                orderItems.setOrder(finalOrder);
                orderItems.setUuid(UUID.randomUUID().toString());
                items.add(itemService.save(orderItems));
                cartService.removeCart(value, item.getUuid());
                System.out.println("OrderItems: " + orderItems);
            });

            result.set(payuService.createOrder(finalOrder, items));
            value.setMaxAge(0);
            response.addCookie(value);
            emailService.sendActivation(order.getEmail(), order, items);
        }, () -> {
            throw new CartDoesntExistException();
        });
        return result.get();
    }

    public void completeOrder(Notify notify) throws OrderDoesntExistException {
        orderRepository.findOrderByOrders(notify.getOrder().getExtOrderId()).ifPresentOrElse(value -> {
            value.setStatus(notify.getOrder().getStatus());
            orderRepository.save(value);
        }, () -> {
            throw new OrderDoesntExistException();
        });
    }

    public Order getOrderByUuid(String uuid) {
        return orderRepository.findOrderByUuid(uuid).orElseThrow(OrderDoesntExistException::new);
    }

    public Optional<Order> getOrderByUuidP(String uuid) {
        return orderRepository.findOrderByUuid(uuid);
    }

    public List<Order> getOrdersByClient(String login) {
        return orderRepository.findOrderByClient(login);
    }
}
