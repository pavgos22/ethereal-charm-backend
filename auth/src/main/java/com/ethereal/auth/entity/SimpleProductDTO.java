package com.ethereal.auth.entity;

import lombok.*;

import java.time.LocalDate;

@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SimpleProductDTO {
    private String uid;
    private String name;
    private String mainDesc;
    private float price;
    private String imageUrl;
    private LocalDate createAt;
    private boolean discount;
    private Float discountedPrice;
}
