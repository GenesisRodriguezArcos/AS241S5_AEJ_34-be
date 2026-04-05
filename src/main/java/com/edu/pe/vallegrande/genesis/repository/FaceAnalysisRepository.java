package com.edu.pe.vallegrande.genesis.repository;

import com.edu.pe.vallegrande.genesis.model.FaceAnalysis;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface FaceAnalysisRepository extends ReactiveCrudRepository<FaceAnalysis, UUID> {
    Flux<FaceAnalysis> findByStatus(String status);
    Mono<FaceAnalysis> findByIdAndStatus(UUID id, String status);
}
