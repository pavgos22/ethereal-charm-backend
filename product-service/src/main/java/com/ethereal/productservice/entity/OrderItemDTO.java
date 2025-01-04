package com.ethereal.productservice.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrderItemDTO {
    private String uuid;
    private String name;
    private long quantity;
    private double priceUnit;
    private double priceSummary;

    @JsonProperty("product")
    private String productUuid;

    @Override
    public String toString() {
        return "OrderItemDTO{" +
                "productUuid='" + productUuid + '\'' +
                ", uuid='" + uuid + '\'' +
                ", name='" + name + '\'' +
                ", quantity=" + quantity +
                ", priceUnit=" + priceUnit +
                ", priceSummary=" + priceSummary +
                '}';
    }
}

