package com.ethereal.cart.service;

import com.ethereal.cart.entity.*;
import com.ethereal.cart.exceptions.CartItemDoesntExistException;
import com.ethereal.cart.exceptions.NoCartInfoException;
import com.ethereal.cart.repository.CartItemRepository;
import com.ethereal.cart.repository.CartRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final RestTemplate restTemplate;
    private final CookieService cookieService;
    @Value("${product.service.url}")
    private String PRODUCT_URL;


    public ResponseEntity<?> add(CartItemAddDTO cartItemAddDTO, HttpServletRequest request, HttpServletResponse response) {
        HttpHeaders httpHeaders = new HttpHeaders();
        List<Cookie> cookies = new ArrayList<>();
        if (request.getCookies() != null) {
            cookies.addAll(List.of(request.getCookies()));
        }
        cookies.stream().filter(value -> value.getName().equals("cart"))
                .findFirst().ifPresentOrElse(value -> {
                    cartRepository.findByUuid(value.getValue()).ifPresentOrElse(cart -> {
                        addProductToCart(cart, cartItemAddDTO);
                        Long sum = cartItemRepository.sumCartItems(cart.getId());
                        if (sum == null) sum = 0L;
                        httpHeaders.add("X-Total-Count", String.valueOf(sum));
                    }, () -> {
                        Cart cart = createCart();
                        response.addCookie(cookieService.generateCookie("cart", cart.getUuid()));
                        addProductToCart(cart, cartItemAddDTO);
                        Long sum = cartItemRepository.sumCartItems(cart.getId());
                        if (sum == null) sum = 0L;
                        httpHeaders.add("X-Total-Count", String.valueOf(sum));
                    });
                }, () -> {
                    Cart cart = createCart();
                    response.addCookie(cookieService.generateCookie("cart", cart.getUuid()));
                    addProductToCart(cart, cartItemAddDTO);
                    Long sum = cartItemRepository.sumCartItems(cart.getId());
                    if (sum == null) sum = 0L;
                    httpHeaders.add("X-Total-Count", String.valueOf(sum));
                });
        return ResponseEntity.ok().headers(httpHeaders).body(new Response("Successful add item to cart"));
    }

    private Cart createCart() {
        Cart cart = new Cart();
        cart.setUuid(UUID.randomUUID().toString());
        return cartRepository.saveAndFlush(cart);
    }

    private void addProductToCart(Cart cart, CartItemAddDTO cartItemAddDTO) {
        CartItems cartItems = new CartItems();
        try {
            Product product = getProduct(cartItemAddDTO.getProduct());
            if (product != null) {
                float unitPrice = product.isDiscount() ? product.getDiscountedPrice() : product.getPrice();
                cartItemRepository.findByCartAndProduct(cart, product.getUid()).ifPresentOrElse(cartItems1 -> {
                    cartItems1.setQuantity(cartItems1.getQuantity() + cartItemAddDTO.getQuantity());
                    cartItemRepository.save(cartItems1);
                }, () -> {
                    cartItems.setCart(cart);
                    cartItems.setUuid(UUID.randomUUID().toString());
                    cartItems.setQuantity(cartItemAddDTO.getQuantity());
                    cartItems.setPriceUnit(unitPrice);
                    cartItems.setProduct(product.getUid());
                    cartItemRepository.save(cartItems);
                });
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }


    private Product getProduct(String uuid) throws URISyntaxException {
        URI uri = new URIBuilder(PRODUCT_URL + "/getExternal").addParameter("uuid", uuid).build();
        ResponseEntity<?> response = restTemplate.getForEntity(uri, Product.class);
        if (response.getStatusCode().isError())
            return null;
        return (Product) response.getBody();
    }

    public ResponseEntity<Response> delete(String uuid, HttpServletRequest request) {
        HttpHeaders httpHeaders = new HttpHeaders();
        List<Cookie> cookies = new ArrayList<>();
        if (request.getCookies() != null) {
            cookies.addAll(List.of(request.getCookies()));
        }
        cookies.stream().filter(value -> value.getName().equals("cart"))
                .findFirst().ifPresentOrElse(value -> {
                    cartRepository.findByUuid(value.getValue()).ifPresentOrElse(cart -> {
                        deleteItem(uuid, cart);
                        Long sum = cartItemRepository.sumCartItems(cart.getId());
                        if (sum == null) sum = 0L;
                        httpHeaders.add("X-Total-Count", String.valueOf(sum));
                    }, () -> {
                        throw new NoCartInfoException("Cart doesn't exist");
                    });
                }, () -> {
                    throw new NoCartInfoException("No cart info in request");
                });
        return ResponseEntity.ok().headers(httpHeaders).body(new Response("Successful delete item from cart"));
    }

    private void deleteItem(String uuid, Cart cart) throws CartItemDoesntExistException {
        cartItemRepository.findCartItemsByProductAndCart(uuid, cart).ifPresentOrElse(cartItemRepository::delete, () -> {
            throw new CartItemDoesntExistException("Cart item doesn't exist");
        });
        Long sum = cartItemRepository.sumCartItems(cart.getId());
        if (sum == null || sum == 0) cartRepository.delete(cart);
    }

    public ResponseEntity<?> getItems(HttpServletRequest request) {
        List<Cookie> cookies = new ArrayList<>();
        HttpHeaders httpHeaders = new HttpHeaders();
        if (request.getCookies() != null)
            cookies.addAll(List.of(request.getCookies()));
        CartItemListDTO cartItemListDTO = new CartItemListDTO();
        cartItemListDTO.setCartProducts(new ArrayList<>());

        cookies.stream().filter(value -> value.getName().equals("cart"))
                .findFirst().ifPresentOrElse(value -> {
                    Cart cart = cartRepository.findByUuid(value.getValue()).orElseThrow(NoCartInfoException::new);
                    Long sum = cartItemRepository.sumCartItems(cart.getId());
                    if (sum == null) sum = 0L;
                    httpHeaders.add("X-Total-Count", String.valueOf(sum));

                    cartItemRepository.findCartItemsByCart(cart).forEach(item -> {
                        try {
                            Product product = getProduct(item.getProduct());
                            double unitPrice = item.getPriceUnit();

                            cartItemListDTO.getCartProducts().add(new CartItemDTO(
                                    product.getUid(),
                                    product.getName(),
                                    item.getQuantity(),
                                    product.getImageUrls()[0],
                                    unitPrice,
                                    unitPrice * item.getQuantity()
                            ));
                            cartItemListDTO.setSummaryPrice(cartItemListDTO.getSummaryPrice() + (item.getQuantity() * unitPrice));
                        } catch (URISyntaxException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }, () -> {
                    throw new NoCartInfoException("No cart info in request");
                });

        if (httpHeaders.isEmpty()) {
            httpHeaders.add("X-Total-Count", String.valueOf(0));
        }

        return ResponseEntity.ok().headers(httpHeaders).body(cartItemListDTO);
    }

}
