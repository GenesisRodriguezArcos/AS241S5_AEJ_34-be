package com.edu.pe.vallegrande.genesis.repository;

import com.edu.pe.vallegrande.genesis.model.FaceDetection;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface FaceDetectionRepository extends ReactiveCrudRepository<FaceDetection, UUID> {
    Flux<FaceDetection> findByStatus(String status);
    Mono<FaceDetection> findByIdAndStatus(UUID id, String status);
}
