package com.ethereal.fileservice.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class ImageDTO {
    private String uuid;
    private LocalDate createAt;
}
