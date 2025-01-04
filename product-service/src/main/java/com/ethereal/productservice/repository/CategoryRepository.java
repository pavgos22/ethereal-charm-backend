package com.ethereal.productservice.repository;

import com.ethereal.productservice.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByShortId(String shortId);

    Optional<Category> findByName(String name);
}
