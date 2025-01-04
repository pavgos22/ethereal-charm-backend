package com.ethereal.gateway.entity;

import org.coffeecode.entity.HttpMethod;
import org.coffeecode.entity.Role;

import java.util.Objects;

public record Endpoint(String url, HttpMethod httpMethod, Role role) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Endpoint endpoint = (Endpoint) o;
        return Objects.equals(url, endpoint.url) &&
                httpMethod == endpoint.httpMethod &&
                Objects.equals(role, endpoint.role);
    }

    @Override
    public String toString() {
        return "Endpoint{" +
                "url='" + url + '\'' +
                ", httpMethod=" + httpMethod +
                ", role=" + role +
                '}';
    }
}
