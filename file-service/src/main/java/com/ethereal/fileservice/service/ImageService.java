package com.ethereal.fileservice.service;

import com.ethereal.fileservice.entity.ImageEntity;
import com.ethereal.fileservice.repository.ImageRepository;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@EnableScheduling
@AllArgsConstructor
public class ImageService {

    private final ImageRepository imageRepository;
    private final FtpService ftpService;

    public ImageEntity save(ImageEntity imageEntity) {
        return imageRepository.saveAndFlush(imageEntity);
    }

    public ImageEntity findByUuid(String uuid) {
        return imageRepository.findByUuid(uuid).orElse(null);
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void cleanImages() {
        imageRepository.findDontUseImages().forEach(value -> {
            try {
                ftpService.deleteFile(value.getPath());
                imageRepository.delete(value);
            } catch (IOException e) {
                System.out.println("Cannot delete " + value.getUuid());
            }
        });
    }

    public void delete(ImageEntity imageEntity) {
        imageRepository.delete(imageEntity);
    }
}
