package com.ethereal.cart.entity;

import jakarta.persistence.*;
import lombok.*;

@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table
public class CartItems {
    @Id
    @GeneratedValue(generator = "cart_items_id_seq", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "cart_items_id_seq", sequenceName = "cart_items_id_seq", allocationSize = 1)
    private long id;
    private String uuid;
    private String product;
    @ManyToOne
    @JoinColumn(name = "cart")
    private Cart cart;
    private long quantity;
    @Column(name = "price_unit")
    private float priceUnit;
}
