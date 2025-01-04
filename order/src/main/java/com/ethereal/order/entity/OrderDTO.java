package com.ethereal.order.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrderDTO {
    private String uuid;
    private String orders;
    private Status status;
    private CustomerDetails customerDetails;
    private Address address;
    private DeliverDTO deliver;
    private List<Items> items;
    private double summaryPrice;
    @JsonProperty("isCompany")
    private boolean isCompany;
    private String companyName;
    private String nip;
    private String info;

    @Override
    public String toString() {
        return "OrderDTO{" +
                "uuid='" + uuid + '\'' +
                ", orders='" + orders + '\'' +
                ", status=" + status +
                ", customerDetails=" + customerDetails +
                ", address=" + address +
                ", deliver=" + deliver +
                ", items=" + items +
                ", summaryPrice=" + summaryPrice +
                ", isCompany=" + isCompany +
                ", companyName='" + companyName + '\'' +
                ", nip='" + nip + '\'' +
                ", info='" + info + '\'' +
                '}';
    }

}
