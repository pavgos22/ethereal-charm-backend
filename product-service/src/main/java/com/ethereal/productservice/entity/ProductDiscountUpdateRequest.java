package com.ethereal.productservice.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductDiscountUpdateRequest {
    private boolean discount;
    private float price;
    private Float discountedPrice;
}

