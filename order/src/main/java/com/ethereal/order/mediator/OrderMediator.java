package com.ethereal.order.mediator;


import com.ethereal.order.entity.*;
import com.ethereal.order.entity.notify.Notify;
import com.ethereal.order.exception.BadSignatureException;
import com.ethereal.order.exception.OrderDoesntExistException;
import com.ethereal.order.exception.UserNotLoggedInException;
import com.ethereal.order.service.*;
import com.ethereal.order.translators.OrderDTOToOrder;
import com.ethereal.order.translators.OrderItemsToItems;
import com.ethereal.order.translators.OrderToOrderDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderMediator {

    private final OrderDTOToOrder orderDTOToOrder;
    private final OrderService orderService;
    private final ItemService itemService;
    private final SignatureValidator signatureValidator;
    private final OrderItemsToItems orderItemsToItems;
    private final OrderToOrderDTO orderToOrderDTO;
    private final ProductService productService;
    private final AuthService authService;

    public ResponseEntity<?> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();

        List<Map<String, Object>> response = orders.stream().map(order -> {
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("orders", order.getOrders());
            orderData.put("status", order.getStatus());
            orderData.put("firstName", order.getFirstName());
            orderData.put("lastName", order.getLastName());
            orderData.put("phone", order.getPhone());
            orderData.put("email", order.getEmail());
            orderData.put("city", order.getCity());
            orderData.put("street", order.getStreet());
            orderData.put("number", order.getNumber());
            orderData.put("postCode", order.getPostCode());
            orderData.put("client", order.getClient());
            orderData.put("deliver", order.getDeliver() != null ? order.getDeliver().getName() : null);
            orderData.put("isCompany", order.isCompany());
            orderData.put("companyName", order.getCompanyName());
            orderData.put("nip", order.getNip());
            orderData.put("info", order.getInfo());
            return orderData;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }


    public ResponseEntity<?> createOrder(OrderDTO orderDTO, HttpServletRequest request, HttpServletResponse response) {
        System.out.println("OrderDTO received in mediator: " + orderDTO);

        Order order = orderDTOToOrder.toOrder(orderDTO);
        System.out.println("Mapped Order entity: " + order);

        order.setCompany(orderDTO.isCompany());
        order.setCompanyName(orderDTO.getCompanyName());
        order.setNip(orderDTO.getNip());
        order.setInfo(orderDTO.getInfo());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, "application/json");
        return ResponseEntity.status(200).headers(httpHeaders).body(orderService.createOrder(order, request, response));
    }


    public ResponseEntity<Response> handleNotify(Notify notify, HttpServletRequest request) {
        String header = request.getHeader("OpenPayu-Signature");
        System.out.println("Received notification:");
        System.out.println("Header: " + header);
        System.out.println("Payload: " + notify);
        System.out.println("Timestamp: " + new Timestamp(System.currentTimeMillis()));
        try {
            signatureValidator.validate(header, notify);
            orderService.completeOrder(notify);
        } catch (NoSuchAlgorithmException | JsonProcessingException | BadSignatureException e) {
            return ResponseEntity.badRequest().body(new Response("Bad signature"));
        } catch (OrderDoesntExistException e1) {
            return ResponseEntity.badRequest().body(new Response("Order don't exist"));
        }
        ;
        return ResponseEntity.ok(new Response("Notification handle success"));
    }

    public ResponseEntity<?> getOrder(String uuid, HttpServletRequest request) {
        if (uuid == null || uuid.isEmpty()) {
            try {
                List<Cookie> cookies = Arrays.stream(request.getCookies()).filter(value ->
                                value.getName().equals("Authorization") || value.getName().equals("refresh"))
                        .toList();
                UserRegisterDTO user = authService.getUserDetails(cookies);
                if (user != null) {
                    List<OrderDTO> orderDTOList = new ArrayList<>();
                    orderService.getOrdersByClient(user.getLogin()).forEach(value -> {
                        orderDTOList.add(orderToOrderDTO.toOrderDTO(value));
                    });
                    return ResponseEntity.ok(orderDTOList);
                }
                throw new OrderDoesntExistException();
            } catch (NullPointerException e) {
                throw new UserNotLoggedInException();
            }
        }
        Order order = orderService.getOrderByUuid(uuid);
        List<OrderItems> itemsList = itemService.getByOrder(order);
        if (itemsList.isEmpty()) throw new OrderDoesntExistException();
        List<Items> itemsDTO = new ArrayList<>();
        AtomicReference<Double> summary = new AtomicReference<>(0d);
        itemsList.forEach(value -> {
            double unitPrice = value.getPriceUnit();
            Items items = orderItemsToItems.toItems(value);
            items.setPrice(unitPrice);
            items.setSummaryPrice(unitPrice * value.getQuantity());
            items.setImageUrl(productService.getProduct(value.getProduct()).getImageUrls()[0]);
            itemsDTO.add(items);
            summary.set(summary.get() + items.getSummaryPrice());
        });

        OrderDTO orderDTO = orderToOrderDTO.toOrderDTO(order);

        orderDTO.setCompany(order.isCompany());
        orderDTO.setCompanyName(order.getCompanyName());
        orderDTO.setNip(order.getNip());
        orderDTO.setInfo(order.getInfo());

        summary.set(summary.get() + orderDTO.getDeliver().getPrice());
        orderDTO.setSummaryPrice(summary.get());
        orderDTO.setItems(itemsDTO);
        return ResponseEntity.ok(orderDTO);
    }
}
