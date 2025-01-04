package com.ethereal.productservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrderProductSummaryDTO {
    private String uuid;
    private String orders;
    private boolean isCompany;
    private String companyName;
    private String nip;
    private String info;
    private List<OrderProductDTO> products;
    private double totalPrice;
}

