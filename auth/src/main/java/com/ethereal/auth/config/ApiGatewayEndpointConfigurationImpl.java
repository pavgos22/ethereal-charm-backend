package com.ethereal.auth.config;

import jakarta.annotation.PostConstruct;
import org.coffeecode.ApiGatewayEndpointConfiguration;
import org.coffeecode.entity.Endpoint;
import org.coffeecode.entity.HttpMethod;
import org.coffeecode.entity.Response;
import org.coffeecode.entity.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ApiGatewayEndpointConfigurationImpl implements ApiGatewayEndpointConfiguration {

    @Value("${api-gateway.url}")
    private String GATEWAY_URL;

    @PostConstruct
    public void startOperation() {
        initMap();
        register();
    }

    @Override
    public void initMap() {
        endpointList.add(new Endpoint("/api/v1/user/shipping-details", HttpMethod.PATCH, Role.USER));
        endpointList.add(new Endpoint("/api/v1/user/favourites", HttpMethod.PATCH, Role.USER));
        endpointList.add(new Endpoint("/api/v1/public/check-email", HttpMethod.POST, Role.GUEST));
    }

    @Override
    public void register() {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Response> response = restTemplate.postForEntity(GATEWAY_URL, endpointList, Response.class);
        if (response.getStatusCode().isError()) {
            throw new RuntimeException("Failed to register endpoints with API Gateway");
        }
    }
}

