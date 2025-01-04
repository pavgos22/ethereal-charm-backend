package com.ethereal.order.service;

import com.ethereal.order.entity.*;
import com.ethereal.order.exception.PayUException;
import com.ethereal.order.repository.DeliverRepository;
import com.ethereal.order.translators.OrderItemsToPayuProduct;
import com.google.common.util.concurrent.AtomicDouble;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PayuService {

    private final OrderItemsToPayuProduct orderItemsToPayuProduct;
    private final RestTemplate restTemplate;
    private final DeliverRepository deliverRepository;
    @Value("${payu.client-id}")
    private String client_id;
    @Value("${payu.client-secret}")
    private String client_secret;
    @Value("${payu.url.notf}")
    private String payu_url_notf;
    @Value("${payu.url.auth}")
    private String payu_url_auth;
    @Value("${payu.url.order}")
    private String payu_url_order;
    private String token;


    private void login() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "client_credentials");
        map.add("client_id", client_id);
        map.add("client_secret", client_secret);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

        ResponseEntity<PayUAuth> response =
                restTemplate.exchange(payu_url_auth,
                        HttpMethod.POST,
                        entity,
                        PayUAuth.class);
        if (response.getStatusCode().isError()) throw new PayUException();
        token = "Bearer " + Objects.requireNonNull(response.getBody()).getAccess_token();
    }


    public String createOrder(Order finalOrder, List<OrderItems> items) throws HttpClientErrorException {
        try {
            return (String) sendOrder(finalOrder, items).getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401) {
                login();
                return (String) sendOrder(finalOrder, items).getBody();
            }
        }
        return null;
    }

    private ResponseEntity<?> sendOrder(Order finalOrder, List<OrderItems> items) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", token);

        HttpEntity<PayUOrder> requestEntity = new HttpEntity<>(prepareOrder(finalOrder, items), headers);
        System.out.println(requestEntity.getBody());
        return restTemplate.exchange(payu_url_order, HttpMethod.POST, requestEntity, String.class);
    }

    private PayUOrder prepareOrder(Order order, List<OrderItems> items) {
        AtomicDouble totalPrice = new AtomicDouble(0.0);

        Deliver courier = deliverRepository.findById(order.getDeliver().getId())
                .orElseThrow(() -> new RuntimeException("Courier not found"));

        List<PayuProduct> product = items.stream()
                .map(item -> {
                    double unitPrice = item.getPriceUnit();
                    totalPrice.addAndGet(unitPrice * item.getQuantity());
                    return orderItemsToPayuProduct.toPayuProduct(item, unitPrice);
                })
                .toList();

        totalPrice.addAndGet(courier.getPrice());

        PayUBuyer buyer = new PayUBuyer(
                order.getEmail(),
                order.getPhone(),
                order.getFirstName(),
                order.getLastName()
        );

        return new PayUOrder(
                payu_url_notf,
                "127.0.0.1",
                client_id,
                order.getOrders(),
                "PLN",
                Math.round(totalPrice.get() * 100),
                order.getOrders(),
                buyer,
                product
        );
    }

}
