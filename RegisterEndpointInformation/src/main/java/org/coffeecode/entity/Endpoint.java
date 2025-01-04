package org.coffeecode.entity;

public record Endpoint(String url, HttpMethod httpMethod, Role role) {
}
