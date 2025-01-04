package com.ethereal.productservice.translator;

import com.ethereal.productservice.entity.Category;
import com.ethereal.productservice.entity.CategoryDTO;
import org.springframework.stereotype.Component;

@Component
public class CategoryToCategoryDTO {

    public CategoryDTO toCategoryDTO(Category category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setName(category.getName());
        dto.setShortId(category.getShortId());
        return dto;
    }
}
