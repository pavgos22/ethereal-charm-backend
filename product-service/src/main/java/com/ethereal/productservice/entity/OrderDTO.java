package com.ethereal.productservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDTO {
    private String uuid;
    private String orders;
    private String status;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String city;
    private String street;
    private String number;
    private String postCode;
    private String deliverUuid;
    private Boolean isCompany;
    private String companyName;
    private String nip;
    private String info;
}
