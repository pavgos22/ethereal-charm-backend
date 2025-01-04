package com.ethereal.order.controller;

import com.ethereal.order.entity.Order;
import com.ethereal.order.entity.OrderDTO;
import com.ethereal.order.entity.OrderItemDTO;
import com.ethereal.order.entity.Response;
import com.ethereal.order.entity.notify.Notify;
import com.ethereal.order.exception.*;
import com.ethereal.order.mediator.OrderMediator;
import com.ethereal.order.service.ItemService;
import com.ethereal.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderMediator orderMediator;
    private final ItemService itemService;
    private final OrderService orderService;

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> createOrder(@RequestBody OrderDTO order, HttpServletRequest request, HttpServletResponse response) {
        System.out.println("Received JSON from frontend: " + order);
        return orderMediator.createOrder(order, request, response);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/notification")
    public ResponseEntity<Response> notifyOrder(@RequestBody Notify notify, HttpServletRequest request) {
        return orderMediator.handleNotify(notify, request);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> get(@RequestParam(required = false) String uuid, HttpServletRequest request) {
        return orderMediator.getOrder(uuid, request);
    }

    @GetMapping("/items")
    public ResponseEntity<List<OrderItemDTO>> getOrderItemsByOrderUuid(@RequestParam String orderUuid) {
        Order order = orderService.getOrderByUuidP(orderUuid).orElseThrow(() -> new RuntimeException("Order not found with UUID: " + orderUuid));
        List<OrderItemDTO> orderItemDTOs = itemService.getOrderItemsByOrder(order);
        return ResponseEntity.ok(orderItemDTOs);
    }

    @PostMapping("/items")
    public ResponseEntity<List<OrderItemDTO>> getOrderItemsByOrder(@RequestBody Order order) {
        List<OrderItemDTO> orderItemDTOs = itemService.getOrderItemsByOrder(order);
        return ResponseEntity.ok(orderItemDTOs);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(EmptyCartException.class)
    public Response handleValidationExceptions(EmptyCartException ex) {
        return new Response("Cart is empty");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(CartDoesntExistException.class)
    public Response handleValidationExceptions(CartDoesntExistException ex) {
        return new Response("Cart doesn't exist for this session");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PayUException.class)
    public Response handleValidationExceptions(PayUException ex) {
        return new Response("Server error, contact with administrator");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(UknowDeliverTypException.class)
    public Response handleValidationExceptions(UknowDeliverTypException ex) {
        return new Response("Deliver don't exist with this uuid");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(UserNotLoggedInException.class)
    public Response handleValidationExceptions(UserNotLoggedInException ex) {
        return new Response("User is not logged in");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(OrderDoesntExistException.class)
    public Response handleValidationExceptions(OrderDoesntExistException ex) {
        return new Response("Order doesn't exist");
    }
}