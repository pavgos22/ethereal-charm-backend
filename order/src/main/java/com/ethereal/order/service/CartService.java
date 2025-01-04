package com.ethereal.order.service;


import com.ethereal.order.entity.ListCartItemDTO;
import com.ethereal.order.exception.CartDoesntExistException;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class CartService {
    private final RestTemplate restTemplate;
    @Value("${cart.service}")
    private String CART_URL;

    public ListCartItemDTO getCart(Cookie value) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", value.getName() + "=" + value.getValue());
        ResponseEntity<ListCartItemDTO> response;
        try {
            response = restTemplate.exchange(CART_URL,
                    HttpMethod.GET,
                    new HttpEntity<String>(headers),
                    ListCartItemDTO.class);
        } catch (HttpClientErrorException e) {
            throw new CartDoesntExistException("Cart doesn't exist");
        }

        if (response.getStatusCode().isError()) throw new CartDoesntExistException("Cart doesn't exist");
        return response.getBody();
    }

    public void removeCart(Cookie value, String uuid) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", value.getName() + "=" + value.getValue());
        restTemplate.exchange(CART_URL + "?uuid=" + uuid,
                HttpMethod.DELETE,
                new HttpEntity<String>(headers),
                String.class);
    }
}
