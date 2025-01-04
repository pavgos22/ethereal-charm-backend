package com.ethereal.cart.controller;

import com.ethereal.cart.entity.CartItemAddDTO;
import com.ethereal.cart.entity.Response;
import com.ethereal.cart.exceptions.CartItemDoesntExistException;
import com.ethereal.cart.exceptions.NoCartInfoException;
import com.ethereal.cart.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;


    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> addProduct(@RequestBody CartItemAddDTO cartItemAddDTO, HttpServletRequest request, HttpServletResponse response) {
        return cartService.add(cartItemAddDTO, request, response);
    }

    @RequestMapping(method = RequestMethod.DELETE)
    public ResponseEntity<Response> delete(@RequestParam String uuid, HttpServletRequest request) {
        return cartService.delete(uuid, request);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> getItems(HttpServletRequest request) {
        return cartService.getItems(request);
    }

    @ExceptionHandler(CartItemDoesntExistException.class)
    private ResponseEntity<Response> handleException(CartItemDoesntExistException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new Response(exception.getMessage()));
    }

    @ExceptionHandler(NoCartInfoException.class)
    private ResponseEntity<Response> handleException(NoCartInfoException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new Response(exception.getMessage()));
    }
}
