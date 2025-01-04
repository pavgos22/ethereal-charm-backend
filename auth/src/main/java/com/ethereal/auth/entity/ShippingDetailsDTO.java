package com.ethereal.auth.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ShippingDetailsDTO {
    private String firstName;
    private String lastName;
    private String phone;
    private String city;
    private String street;
    @JsonProperty("number")
    private String apartmentNumber;
    @JsonProperty("postCode")
    private String postalCode;
    private boolean isCompany;
    private String companyName;
    private String nip;
}

