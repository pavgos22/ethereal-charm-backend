package com.ethereal.productservice.translator;

import com.ethereal.productservice.entity.Category;
import com.ethereal.productservice.entity.CategoryDTO;
import com.ethereal.productservice.entity.ProductDTO;
import com.ethereal.productservice.entity.ProductEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductEntityToProductDTO {

    public ProductDTO toProductDTO(ProductEntity productEntity) {
        ProductDTO productDTO = new ProductDTO();
        productDTO.setUid(productEntity.getUid());
        productDTO.setActivate(productEntity.isActivate());
        productDTO.setName(productEntity.getName());
        productDTO.setMainDesc(productEntity.getMainDesc());
        productDTO.setDescHtml(productEntity.getDescHtml());
        productDTO.setPrice(productEntity.getPrice());
        productDTO.setImageUrls(productEntity.getImageUrls());
        productDTO.setParameters(productEntity.getParameters());
        productDTO.setCreateAt(productEntity.getCreateAt());
        productDTO.setCategoryDTO(toCategoryDTO(productEntity.getCategory()));
        productDTO.setDiscount(productEntity.isDiscount());
        productDTO.setDiscountedPrice(productEntity.getDiscountedPrice());
        productDTO.setPriority(productDTO.getPriority());

        return productDTO;
    }

    private CategoryDTO toCategoryDTO(Category category) {
        if (category == null) return null;
        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setName(category.getName());
        categoryDTO.setShortId(category.getShortId());
        return categoryDTO;
    }
}
