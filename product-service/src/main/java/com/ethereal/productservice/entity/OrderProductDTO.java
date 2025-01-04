package com.ethereal.productservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrderProductDTO {
    private String name;
    private long quantity;
    private double priceUnit;
    private double priceSummary;
    private String imageUrl;
    private boolean isDiscount;
    private Float discountedPrice;

    public OrderProductDTO(String name, long quantity, double priceUnit, double priceSummary, String s) {
        this.name = name;
        this.quantity = quantity;
        this.priceUnit = priceUnit;
        this.priceSummary = priceSummary;
        this.imageUrl = s;
    }
}
