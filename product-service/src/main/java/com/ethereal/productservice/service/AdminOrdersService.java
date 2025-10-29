package com.ethereal.productservice.service;

import com.ethereal.productservice.entity.*;
import com.ethereal.productservice.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminOrdersService {

    @PersistenceContext
    private final EntityManager entityManager;
    private final RestTemplate restTemplate;
    private final ProductRepository productRepository;

    @Value("${file-service.url}")
    private String FILE_SERVICE;
    @Value("${order-service.url}")
    private String ORDER_SERVICE;
    @Value("${public-image.url}")
    private String PUBLIC_IMAGE_URL;


    public List<OrderDTO> getAllOrders() {
        List<Object[]> results = entityManager.createNativeQuery("""
                    SELECT o.uuid AS uuid, o.orders AS orders, o.status AS status, 
                           o.firstname AS firstName, o.lastname AS lastName, 
                           o.phone AS phone, o.email AS email, o.city AS city, 
                           o.street AS street, o.number AS number, o.postcode AS postCode, 
                           d.uuid AS deliverUuid, o.iscompany AS isCompany,
                           o.companyname AS companyName, o.nip AS nip, o.info AS info
                    FROM orders o
                    LEFT JOIN deliver d ON o.deliver = d.id
                """).getResultList();


        return results.stream()
                .map(result -> new OrderDTO(
                        (String) result[0],  // uuid
                        (String) result[1],  // orders
                        (String) result[2],  // status
                        (String) result[3],  // firstName
                        (String) result[4],  // lastName
                        (String) result[5],  // phone
                        (String) result[6],  // email
                        (String) result[7],  // city
                        (String) result[8],  // street
                        (String) result[9],  // number
                        (String) result[10], // postCode
                        (String) result[11], // deliverUuid
                        (Boolean) result[12], // isCompany
                        (String) result[13], // companyName
                        (String) result[14], // nip
                        (String) result[15]  // info
                ))
                .collect(Collectors.toList());
    }

    public List<UserDTO> getAllUsers() {
        List<Object[]> results = entityManager.createNativeQuery("""
                    SELECT u.uuid AS uuid, u.login AS login, u.email AS email, 
                           u.role AS role, u.islock AS isLock, u.isenabled AS isEnabled, 
                           u.firstname AS firstName, u.lastname AS lastName, 
                           u.phone AS phone, u.city AS city, u.street AS street, 
                           u.apartment AS apartmentNumber, u.postalcode AS postalCode, 
                           u.iscompany AS isCompany, u.companyname AS companyName, 
                           u.nip AS nip
                    FROM users u
                """).getResultList();

        return results.stream()
                .map(result -> new UserDTO(
                        (String) result[0],  // uuid
                        (String) result[1],  // login
                        (String) result[2],  // email
                        Role.valueOf((String) result[3]), // role
                        (Boolean) result[4], // isLock
                        (Boolean) result[5], // isEnabled
                        (String) result[6],  // firstName
                        (String) result[7],  // lastName
                        (String) result[8],  // phone
                        (String) result[9],  // city
                        (String) result[10], // street
                        (String) result[11], // apartmentNumber
                        (String) result[12], // postalCode
                        (Boolean) result[13], // isCompany
                        (String) result[14], // companyName
                        (String) result[15]  // nip
                ))
                .collect(Collectors.toList());
    }

    public UserDTO getUserByUuid(String uuid) {
        Object[] result = (Object[]) entityManager.createNativeQuery("""
                             SELECT u.uuid AS uuid, u.login AS login, u.email AS email,\s
                                    u.role AS role, u.islock AS isLock, u.isenabled AS isEnabled,\s
                                    u.firstname AS firstName, u.lastname AS lastName,\s
                                    u.phone AS phone, u.city AS city, u.street AS street,\s
                                    u.apartment AS apartmentNumber, u.postalcode AS postalCode,\s
                                    u.iscompany AS isCompany, u.companyname AS companyName,\s
                                    u.nip AS nip
                             FROM users u
                             WHERE u.uuid = :uuid
                        \s""").setParameter("uuid", uuid)
                .getSingleResult();

        return new UserDTO(
                (String) result[0],  // uuid
                (String) result[1],  // login
                (String) result[2],  // email
                Role.valueOf((String) result[3]), // role
                (Boolean) result[4], // isLock
                (Boolean) result[5], // isEnabled
                (String) result[6],  // firstName
                (String) result[7],  // lastName
                (String) result[8],  // phone
                (String) result[9],  // city
                (String) result[10], // street
                (String) result[11], // apartmentNumber
                (String) result[12], // postalCode
                (Boolean) result[13], // isCompany
                (String) result[14], // companyName
                (String) result[15]  // nip
        );
    }


    public List<OrderItemDTO> getOrderItemsByOrderUuid(String orderUuid) {
        String url = ORDER_SERVICE + "/items?orderUuid=" + orderUuid;
        ResponseEntity<OrderItemDTO[]> response = restTemplate.getForEntity(url, OrderItemDTO[].class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return List.of(response.getBody());
        }
        throw new RuntimeException("Could not fetch order items.");
    }

    public List<SimpleProductDTO> getOrderProducts(String orderUuid) {
        String orderItemsUrl = ORDER_SERVICE + "/items?orderUuid=" + orderUuid;
        ResponseEntity<OrderItemDTO[]> response = restTemplate.getForEntity(orderItemsUrl, OrderItemDTO[].class);

        System.out.println("Response Status: " + response.getStatusCode());
        System.out.println("Response Headers: " + response.getHeaders());
        System.out.println("Response Body: " + Arrays.toString(response.getBody()));

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Could not fetch order items.");
        }

        List<OrderItemDTO> orderItems = List.of(response.getBody());

        System.out.println("Order Items: " + orderItems);


        orderItems.forEach(item -> {
            if (item.getProductUuid() == null) {
                throw new RuntimeException("Order item contains a null product UUID.");
            }
        });

        return orderItems.stream()
                .map(orderItem -> {
                    ProductEntity product = productRepository.findByUid(orderItem.getProductUuid())
                            .orElseThrow(() -> new RuntimeException("Product not found: " + orderItem.getProductUuid()));

                    System.out.println("SimpleProductDTO imageUrl: " + product.getImageUrls()[0]);
                    return new SimpleProductDTO(
                            product.getUid(),
                            product.getName(),
                            product.getMainDesc(),
                            product.getPrice(),
                            product.getImageUrls()[0],
                            product.getCreateAt(),
                            product.isDiscount(),
                            product.getDiscountedPrice(),
                            product.getPriority()
                    );
                })
                .toList();
    }

    public OrderProductSummaryDTO getOrderDetails(String orderUuid) {
        Object[] orderDetails = (Object[]) entityManager.createNativeQuery("""
                            SELECT o.orders, o.iscompany, o.companyname, o.nip, o.info
                            FROM orders o
                            WHERE o.uuid = :uuid
                        """)
                .setParameter("uuid", orderUuid)
                .getSingleResult();

        String ordersValue = (String) orderDetails[0];
        boolean isCompany = (Boolean) orderDetails[1];
        String companyName = (String) orderDetails[2];
        String nip = (String) orderDetails[3];
        String info = (String) orderDetails[4];

        List<Object[]> rawOrderItems = entityManager.createNativeQuery("""
                            SELECT oi.uuid, oi.name, oi.product, oi.priceunit, oi.pricesummary, oi.quantity
                            FROM order_items oi
                            JOIN orders o ON oi.orders = o.id
                            WHERE o.uuid = :uuid
                        """)
                .setParameter("uuid", orderUuid)
                .getResultList();

        List<OrderProductDTO> products = rawOrderItems.stream()
                .map(item -> {
                    String productUuid = (String) item[2];
                    ProductEntity product = productRepository.findByUid(productUuid).orElseThrow(() -> new RuntimeException("Product not found: " + productUuid));
                    String imageUrl = PUBLIC_IMAGE_URL + "?uuid=" + product.getImageUrls()[0];
                    return new OrderProductDTO(
                            (String) item[1],
                            ((Number) item[5]).longValue(),
                            ((Number) item[3]).doubleValue(),
                            ((Number) item[4]).doubleValue(),
                            imageUrl
                    );
                })
                .toList();

        double totalPrice = products.stream()
                .mapToDouble(OrderProductDTO::getPriceSummary)
                .sum();

        return new OrderProductSummaryDTO(
                orderUuid, ordersValue, isCompany, companyName, nip, info, products, totalPrice
        );
    }
}

