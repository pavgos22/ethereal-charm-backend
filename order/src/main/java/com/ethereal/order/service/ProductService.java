package com.ethereal.order.service;

import com.ethereal.order.entity.ProductEntity;
import lombok.RequiredArgsConstructor;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final RestTemplate restTemplate;
    @Value("${product-service.url}")
    private String PRODUCT_URL;

    public ProductEntity getProduct(String uuid) {
        try {
            URI uri = new URIBuilder(PRODUCT_URL).addParameter("uuid", uuid).build();
            ResponseEntity<ProductEntity[]> response = restTemplate.getForEntity(uri, ProductEntity[].class);

            if (response.getStatusCode().is2xxSuccessful()) {
                ProductEntity[] products = response.getBody();
                return (products != null && products.length > 0) ? products[0] : null;
            }
            return null;
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid URI for product service", e);
        }
    }
}
