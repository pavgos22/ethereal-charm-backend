package com.ethereal.gateway.filter;


import com.ethereal.gateway.entity.Endpoint;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.coffeecode.entity.HttpMethod;
import org.coffeecode.entity.Role;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@Slf4j
@Component
public class RouteValidator {
    public Set<Endpoint> openApiEndpoints = new HashSet<>(List.of(
            new Endpoint("/auth/logout", HttpMethod.GET, Role.GUEST),
            new Endpoint("/auth/register", HttpMethod.POST, Role.GUEST),
            new Endpoint("/auth/login", HttpMethod.POST, Role.GUEST),
            new Endpoint("/auth/validate", HttpMethod.GET, Role.GUEST),
            new Endpoint("/auth/activate", HttpMethod.GET, Role.GUEST),
            new Endpoint("/auth/deactivate", HttpMethod.GET, Role.USER),
            new Endpoint("/auth/authorize", HttpMethod.GET, Role.GUEST),
            new Endpoint("/auth/reset-password", HttpMethod.PATCH, Role.GUEST),
            new Endpoint("/auth/reset-password", HttpMethod.POST, Role.GUEST),
            new Endpoint("/api/v1/gateway", HttpMethod.POST, Role.GUEST),
            new Endpoint("/api/v1/auto-login", HttpMethod.GET, Role.GUEST),
            new Endpoint("/api/v1/logged-in", HttpMethod.GET, Role.GUEST)
            // new Endpoint("/api/v1/user/shipping-details",HttpMethod.PATCH,Role.GUEST)
    )
    );
    @Getter
    private Set<Endpoint> adminEndpoints = new HashSet<>();

    public void addEndpoints(List<Endpoint> endpointList) {
        for (Endpoint endpoint : endpointList) {
            if (endpoint.role().name().equals(Role.ADMIN.name())) {
                adminEndpoints.add(endpoint);
                System.out.println("Added to adminEndpoints: " + endpoint);
            }
            if (endpoint.role().name().equals(Role.GUEST.name())) {
                openApiEndpoints.add(endpoint);
                System.out.println("Added to guestEndpoints: " + endpoint);
            }
        }
    }


    public Predicate<ServerHttpRequest> isSecure =
            request -> {
                boolean match = openApiEndpoints.stream().noneMatch(value ->
                        request.getURI().getPath().contains(value.url()) &&
                                request.getMethod().name().equals(value.httpMethod().name()));
                log.info("isSecure check for {}: {}", request.getURI().getPath(), match);
                return match;
            };

    public Predicate<ServerHttpRequest> isAdmin =
            request -> {
                boolean match = adminEndpoints.stream().anyMatch(value ->
                        request.getURI().getPath().contains(value.url()) &&
                                request.getMethod().name().equals(value.httpMethod().name()));
                log.info("isAdmin check for {}: {}", request.getURI().getPath(), match);
                return match;
            };

}