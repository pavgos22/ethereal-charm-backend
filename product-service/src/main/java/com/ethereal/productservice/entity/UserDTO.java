package com.ethereal.productservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserDTO {
    private String uuid;
    private String login;
    private String email;
    private Role role;
    private boolean isLock;
    private boolean isEnabled;
    private String firstName;
    private String lastName;
    private String phone;
    private String city;
    private String street;
    private String apartmentNumber;
    private String postalCode;
    private boolean isCompany;
    private String companyName;
    private String nip;
}
