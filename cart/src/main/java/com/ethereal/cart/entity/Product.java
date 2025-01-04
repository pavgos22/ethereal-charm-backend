package com.ethereal.cart.entity;

import lombok.*;

import java.time.LocalDate;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class Product {
    private long id;
    private String uid;
    private boolean activate;
    private String name;
    private String mainDesc;
    private String descHtml;
    private float price;
    private String[] imageUrls;
    private Map<String, String> parameters;
    private LocalDate createAt;
    private Category category;
    private boolean discount;
    private Float discountedPrice;
}
