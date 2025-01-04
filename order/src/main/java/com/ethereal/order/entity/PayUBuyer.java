package com.ethereal.order.entity;


import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class PayUBuyer {
    private String email;
    private String phone;
    private String firstName;
    private String lastName;
}
