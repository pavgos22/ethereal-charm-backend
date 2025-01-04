package com.ethereal.cart.repository;

import com.ethereal.cart.entity.Cart;
import com.ethereal.cart.entity.CartItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItems, Long> {

    Optional<CartItems> findByCartAndProduct(Cart cart, String product);

    @Query(nativeQuery = true, value = "SELECT SUM(quantity) from cart_items where cart = ?1")
    Long sumCartItems(long cart);

    Optional<CartItems> findCartItemsByProductAndCart(String uuid, Cart cart);

    List<CartItems> findCartItemsByCart(Cart cart);
}