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
@Table("face_analysis")
public class FaceAnalysis {

    @Id
    private UUID id;

    @Column("image_url")
    private String imageUrl;

    @Column("request_id")
    private String requestId;

    @Column("error_code")
    private Integer errorCode;

    @Column("error_msg")
    private String errorMsg;

    @Column("face_list")
    private String faceList;

    @Column("created_at")
    private LocalDateTime createdAt;

    // A = activo, I = inactivo (borrado lógico)
    private String status;
}
