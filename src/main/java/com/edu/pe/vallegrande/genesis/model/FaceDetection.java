package com.edu.pe.vallegrande.genesis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("face_detection")
public class FaceDetection {

    @Id
    private UUID id;

    @Column("image_url")
    private String imageUrl;

    @Column("image_name")
    private String imageName;

    private String md5;
    private Integer width;
    private Integer height;

    @Column("status_code")
    private String statusCode;

    @Column("status_msg")
    private String statusMsg;

    private String entities;

    @Column("created_at")
    private LocalDateTime createdAt;

    // A = activo, I = inactivo (borrado lógico)
    private String status;
}
