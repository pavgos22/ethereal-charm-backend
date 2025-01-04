package com.ethereal.fileservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Builder
@Getter
@Setter
@Entity
@Table(name = "image_data")
@AllArgsConstructor
@NoArgsConstructor
public class ImageEntity {
    @Id
    @GeneratedValue(generator = "image_data_id_seq", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "image_data_id_seq", sequenceName = "image_data_id_seq", allocationSize = 1)
    private long id;
    private String uuid;
    @Column(name = "pathfile")
    private String path;
    @Column(name = "isused")
    private boolean isUsed;
    @Column(name = "createat")
    private LocalDate createAt;
}
