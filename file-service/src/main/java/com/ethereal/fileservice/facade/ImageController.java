package com.ethereal.fileservice.facade;

import com.ethereal.fileservice.entity.ImageResponse;
import com.ethereal.fileservice.mediator.MediatorImage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping(value = "/api/v1/image")
@RequiredArgsConstructor
public class ImageController {

    private final MediatorImage mediatorImage;

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> saveFile(@RequestParam MultipartFile multipartFile) {
        return mediatorImage.saveImage(multipartFile);
    }

    @RequestMapping(method = RequestMethod.DELETE)
    public ResponseEntity<ImageResponse> deleteFile(@RequestParam String uuid) {
        return mediatorImage.delete(uuid);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> getFile(@RequestParam String uuid) throws IOException {
        return mediatorImage.getImage(uuid);
    }

    @RequestMapping(method = RequestMethod.PATCH)
    public ResponseEntity<ImageResponse> activateImage(@RequestParam String uuid) {
        return mediatorImage.activateImage(uuid);
    }
}
