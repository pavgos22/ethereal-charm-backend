package com.ethereal.order.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(generator = "orders_id_seq", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "orders_id_seq", sequenceName = "orders_id_seq", allocationSize = 1)
    private long id;
    private String uuid;
    private String orders;
    @Enumerated(EnumType.STRING)
    private Status status;
    @Column(name = "firstname")
    private String firstName;
    @Column(name = "lastname")
    private String lastName;
    private String phone;
    private String email;
    private String city;
    private String street;
    private String number;
    @Column(name = "postcode")
    private String postCode;
    private String client;
    @ManyToOne
    @JoinColumn(name = "deliver")
    private Deliver deliver;
    @Column(name = "iscompany")
    private boolean isCompany;
    @Column(name = "companyname")
    private String companyName;
    @Column(name = "nip")
    private String nip;
    @Column(name = "info", length = 1000)
    private String info;

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", uuid='" + uuid + '\'' +
                ", orders='" + orders + '\'' +
                ", status=" + status +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", city='" + city + '\'' +
                ", street='" + street + '\'' +
                ", number='" + number + '\'' +
                ", postCode='" + postCode + '\'' +
                ", client='" + client + '\'' +
                ", deliver=" + deliver +
                ", isCompany=" + isCompany +
                ", companyName='" + companyName + '\'' +
                ", nip='" + nip + '\'' +
                ", info='" + info + '\'' +
                '}';
    }
}
