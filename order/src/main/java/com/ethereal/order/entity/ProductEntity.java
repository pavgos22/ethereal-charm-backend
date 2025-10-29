package com.ethereal.order.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProductEntity {
    private long id;
    private String uid;
    private boolean activate;
    private String name;
    private String mainDesc;
    private String descHtml;
    private float price;
    private String imageUrl;
    private Map<String, String> parameters;
    private LocalDate createAt;

    public String[] getImageUrls() {
        return imageUrl != null ? new String[]{imageUrl} : new String[0];
    }

    @Override
    public String toString() {
        return "ProductEntity{" +
                "id=" + id +
                ", uid='" + uid + '\'' +
                ", activate=" + activate +
                ", name='" + name + '\'' +
                ", mainDesc='" + mainDesc + '\'' +
                ", descHtml='" + descHtml + '\'' +
                ", price=" + price +
                ", imageUrls='" + Arrays.toString(getImageUrls()) + '\'' +
                ", parameters=" + parameters +
                ", createAt=" + createAt +
                '}';
    }
}