package com.ethereal.gateway.fasada;

import com.ethereal.gateway.entity.Endpoint;
import com.ethereal.gateway.filter.RouteValidator;
import lombok.RequiredArgsConstructor;
import org.coffeecode.entity.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/gateway")
@RequiredArgsConstructor
public class RegistrationController {

    private final RouteValidator routeValidator;

    @PostMapping
    public ResponseEntity<Response> register(@RequestBody List<Endpoint> endpoints) {
        routeValidator.addEndpoints(endpoints);
        System.out.println("Registered endpoints: " + endpoints);
        return ResponseEntity.ok(new Response("Successfully register new endpoints"));
    }
}
