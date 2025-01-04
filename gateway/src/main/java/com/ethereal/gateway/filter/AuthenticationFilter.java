package com.ethereal.gateway.filter;

import com.ethereal.gateway.config.Carousel;
import com.ethereal.gateway.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.Timestamp;
import java.util.List;

@Component
@Slf4j
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final RouteValidator validator;
    private final RestTemplate template;
    private final JwtUtil jwtUtil;
    @Value("${spring.profiles.active}")
    private String activeProfile;
    private final Carousel carousel;

    public AuthenticationFilter(JwtUtil jwtUtil, RestTemplate restTemplate, RouteValidator validator, Carousel carousel) {
        super(Config.class);
        this.carousel = carousel;
        this.jwtUtil = jwtUtil;
        this.template = restTemplate;
        this.validator = validator;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            log.info("--START GatewayFilter");
            String requestPath = exchange.getRequest().getPath().toString();
            log.info("Incoming request path: {}", requestPath);
            log.info("openApiEndpoints: {}", validator.openApiEndpoints);
            log.info("Request being validated: {} {}", exchange.getRequest().getMethod(), exchange.getRequest().getPath());
            log.info("adminEndpoints: {}", validator.getAdminEndpoints());
            log.info("Checking if admin endpoint: {} {}", exchange.getRequest().getMethod(), exchange.getRequest().getURI().getPath());
            if (validator.isSecure.test(exchange.getRequest())) {
                log.info("Request requires authentication: {}", requestPath);
                if (!exchange.getRequest().getCookies().containsKey(HttpHeaders.AUTHORIZATION) && !exchange.getRequest().getCookies().containsKey("refresh")) {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    String stringBuilder = "{\n" +
                            "\"timestamp\": \"" +
                            new Timestamp(System.currentTimeMillis()) +
                            "\",\n" +
                            "\"message\": \"Wskazany token jest pusty lub nieważny\",\n" +
                            "\"code\": \"A3\"\n" +
                            "}";

                    return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                            .bufferFactory()
                            .wrap((stringBuilder).getBytes())));
                }

                HttpCookie authCookie = exchange.getRequest().getCookies().get(HttpHeaders.AUTHORIZATION).get(0);
                HttpCookie refreshCookie = exchange.getRequest().getCookies().get("refresh").get(0);
                log.info("--START validate Token");
                try {
                    if (activeProfile.equals("test")) {
                        log.debug("Init self auth methods (only for tests)");
                        jwtUtil.validateToken(authCookie.getValue());
                    } else {
                        String cookies = new StringBuilder()
                                .append(authCookie.getName())
                                .append("=")
                                .append(authCookie.getValue())
                                .append(";")
                                .append(refreshCookie.getName())
                                .append("=")
                                .append(refreshCookie.getValue()).toString();
                        HttpHeaders httpHeaders = new HttpHeaders();
                        httpHeaders.add("Cookie", cookies);
                        HttpEntity<Object> entity = new HttpEntity<>(httpHeaders);
                        ResponseEntity<String> response;
                        if (validator.isAdmin.test(exchange.getRequest())) {
                            System.out.println("Admin endpoint accessed: " + exchange.getRequest().getPath());
                            response = template.exchange("http://" + carousel.getUriAuth() + "/api/v1/auth/authorize", HttpMethod.GET, entity, String.class);
                        } else {
                            System.out.println("Not an admin endpoint: " + exchange.getRequest().getPath());
                            response = template.exchange("http://" + carousel.getUriAuth() + "/api/v1/auth/validate", HttpMethod.GET, entity, String.class);
                        }
                        if (response.getStatusCode() == HttpStatus.OK) {
                            List<String> cookiesList = response.getHeaders().get(HttpHeaders.SET_COOKIE);
                            if (cookiesList != null) {
                                List<java.net.HttpCookie> httpCookie = java.net.HttpCookie.parse(cookiesList.get(0));
                                for (java.net.HttpCookie cookie : httpCookie) {
                                    exchange.getResponse().getCookies().add(cookie.getName(),
                                            ResponseCookie.from(cookie.getName(), cookie.getValue())
                                                    .domain(cookie.getDomain())
                                                    .path(cookie.getPath())
                                                    .maxAge(cookie.getMaxAge())
                                                    .secure(cookie.getSecure())
                                                    .httpOnly(cookie.isHttpOnly())
                                                    .build());
                                }
                            }
                            log.info("Successful login");
                        }
                    }
                } catch (HttpClientErrorException e) {
                    log.warn("Can't login bad token");
                    String message = e.getMessage().substring(7);
                    message = message.substring(0, message.length() - 1);
                    ServerHttpResponse response = exchange.getResponse();
                    HttpHeaders headers = response.getHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().writeWith(Flux.just(new DefaultDataBufferFactory().wrap(message.getBytes())));
                }
            }
            log.info("--STOP validate Token");
            log.info("--STOP GatewayFilter");
            return chain.filter(exchange);
        });
    }


    public static class Config {

    }
}