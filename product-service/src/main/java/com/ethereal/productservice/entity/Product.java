package com.ethereal.productservice.entity;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Product {
    private String uid;
    private boolean activate;
    @Column(name = "product_name")
    private String name;
    private String mainDesc;
    private String descHtml;
    private float price;
    private String[] imageUrls;
    @Column(columnDefinition = "jsonb")
    private Map<String, String> parameters;
    private LocalDate createAt;

}
