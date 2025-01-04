package com.ethereal.order.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Items {
    private String uuid;
    private String name;
    private String imageUrl;
    private long quantity;
    private double price;
    private double summaryPrice;
    private boolean discount;
    private double discountedPrice;
}
