package com.ethereal.productservice.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class ProductDTO {
    private String uid;
    private boolean activate;
    private String name;
    private String mainDesc;
    private String descHtml;
    private float price;
    private String[] imageUrls;
    private Map<String, String> parameters;
    private LocalDate createAt;
    private CategoryDTO categoryDTO;
    private boolean discount;
    private Float discountedPrice;
    private int priority;
}
