package com.ethereal.productservice.translator;

import com.ethereal.productservice.entity.ProductEntity;
import com.ethereal.productservice.entity.SimpleProductDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProductEntityToSimpleProduct {

    @Value("${public-image.url}")
    private String PUBLIC_IMAGE_URL;

    public SimpleProductDTO toSimpleProduct(ProductEntity productEntity) {
        String imageUrl = null;

        if (productEntity.getImageUrls() != null && productEntity.getImageUrls().length > 0) {
            imageUrl = PUBLIC_IMAGE_URL + "?uuid=" + productEntity.getImageUrls()[0];
        }

        return new SimpleProductDTO(
                productEntity.getUid(),
                productEntity.getName(),
                productEntity.getMainDesc(),
                productEntity.getPrice(),
                imageUrl,
                productEntity.getCreateAt(),
                productEntity.isDiscount(),
                productEntity.getDiscountedPrice(),
                productEntity.getPriority()
        );
    }
}