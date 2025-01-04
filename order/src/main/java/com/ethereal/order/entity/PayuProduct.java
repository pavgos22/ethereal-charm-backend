package com.ethereal.order.entity;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class PayuProduct {
    private String name;
    private long unitPrice;
    private long quantity;
}
