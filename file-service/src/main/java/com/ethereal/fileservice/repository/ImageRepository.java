package com.ethereal.fileservice.repository;

import com.ethereal.fileservice.entity.ImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<ImageEntity, Long> {

    Optional<ImageEntity> findByUuid(String uuid);

    @Query(nativeQuery = true, value = "SELECT * FROM image_data where createat < current_date - interval '2 days' and isused = false")
    List<ImageEntity> findDontUseImages();
}
