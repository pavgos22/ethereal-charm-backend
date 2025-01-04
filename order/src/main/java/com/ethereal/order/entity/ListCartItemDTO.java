package com.ethereal.order.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ListCartItemDTO {
    private List<CartItemDTO> cartProducts;
    private double summaryPrice;
}
