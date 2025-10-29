package com.ethereal.productservice.entity;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "products")
@NoArgsConstructor
@Getter
@Setter
public class ProductEntity extends Product {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "products_id_seq")
    @SequenceGenerator(name = "products_id_seq", sequenceName = "products_id_seq", allocationSize = 1)
    private long id;
    @ManyToOne
    @JoinColumn(name = "category_parameters")
    private Category category;
    @Column(name = "product_name")
    private String name;
    @Column(name = "create_at")
    private LocalDate createAt;
    @Column(name = "uid")
    private String uid;
    @Column(name = "activate")
    private boolean activate;
    @Column(name = "main_desc")
    private String mainDesc;
    @Column(name = "desc_html")
    private String descHtml;
    @Column(name = "price")
    private float price;
    @Column(name = "image_urls")
    private String[] imageUrls;
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> parameters;
    @Column(name = "discount")
    private boolean discount;
    @Column(name = "discounted_price")
    private Float discountedPrice;
    @Column(name = "priority")
    private Integer priority = 50;

    public ProductEntity(long id, String uid, boolean activate, String name, String mainDesc, String descHtml, float price, String[] imageUrls, Map<String, String> parameters, LocalDate createAt, Category category) {
        super(uid, activate, name, mainDesc, descHtml, price, imageUrls, parameters, createAt);
        this.category = category;
        this.id = id;
    }
}
