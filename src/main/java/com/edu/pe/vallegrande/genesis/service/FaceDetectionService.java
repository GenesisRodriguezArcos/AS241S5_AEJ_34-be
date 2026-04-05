package com.edu.pe.vallegrande.genesis.service;

import com.edu.pe.vallegrande.genesis.model.FaceDetection;
import com.edu.pe.vallegrande.genesis.repository.FaceDetectionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaceDetectionService {

    @Qualifier("faceDetectionClient")
    private final WebClient faceDetectionClient;

    private final FaceDetectionRepository repository;

    public Flux<FaceDetection> findAllActive()   { return repository.findByStatus("A"); }
    public Flux<FaceDetection> findAllInactive() { return repository.findByStatus("I"); }
    public Mono<FaceDetection> findById(UUID id) { return repository.findByIdAndStatus(id, "A"); }

    public Mono<FaceDetection> detect(String imageUrl) {
        log.info("Calling face-detection API with url: {}", imageUrl);
        return faceDetectionClient.post()
                .uri("/v1/results?detection=true&embeddings=false")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("url=" + imageUrl)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .doOnNext(body -> log.error("RapidAPI face-detection error [{}]: {}", response.statusCode(), body))
                                .flatMap(body -> Mono.error(new RuntimeException("El servicio de detección facial respondió con error [" + response.statusCode() + "]: " + body)))
                )
                .bodyToMono(JsonNode.class)
                .doOnNext(json -> log.info("RapidAPI face-detection response: {}", json))
                .flatMap(json -> {
                    JsonNode result = json.path("results").path(0);
                    // Si results está vacío, no hay rostros detectados
                    if (result.isMissingNode()) {
                        log.warn("No results returned from face-detection API for: {}", imageUrl);
                        return saveErrorRecord(imageUrl, "El servicio no retornó resultados. La imagen puede no tener rostros detectables.");
                    }
                    JsonNode status = result.path("status");
                    FaceDetection detection = FaceDetection.builder()
                            .imageUrl(imageUrl)
                            .imageName(result.path("name").asText(null))
                            .md5(result.path("md5").asText(null))
                            .width(result.path("width").asInt(0))
                            .height(result.path("height").asInt(0))
                            .statusCode(status.path("code").asText(null))
                            .statusMsg(status.path("message").asText(null))
                            .entities(result.path("entities").toString())
                            .createdAt(LocalDateTime.now())
                            .status("A")
                            .build();
                    return repository.save(detection);
                })
                .onErrorResume(TimeoutException.class, e -> {
                    log.error("Timeout calling face-detection API for: {}", imageUrl);
                    return saveErrorRecord(imageUrl, "Timeout al llamar al servicio de detección facial.");
                })
                .onErrorResume(WebClientRequestException.class, e -> {
                    log.error("Connection error calling face-detection API: {}", e.getMessage());
                    return saveErrorRecord(imageUrl, "No se pudo conectar al servicio de detección facial: " + e.getMessage());
                })
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("HTTP error from face-detection API [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
                    return saveErrorRecord(imageUrl, "Error del servicio externo [" + e.getStatusCode() + "]: " + e.getResponseBodyAsString());
                })
                .onErrorResume(e -> !(e instanceof RuntimeException) || e.getMessage() == null, e -> {
                    log.error("Unexpected error in detect(): {}", e.getMessage(), e);
                    return saveErrorRecord(imageUrl, "Error inesperado: " + e.getMessage());
                });
    }

    /** Guarda un registro de error en BD para que el frontend reciba algo coherente */
    private Mono<FaceDetection> saveErrorRecord(String imageUrl, String errorMsg) {
        FaceDetection errorRecord = FaceDetection.builder()
                .imageUrl(imageUrl)
                .statusCode("ERROR")
                .statusMsg(errorMsg)
                .entities("[]")
                .createdAt(LocalDateTime.now())
                .status("A")
                .build();
        return repository.save(errorRecord);
    }

    public Mono<FaceDetection> update(UUID id, FaceDetection updated) {
        return repository.findByIdAndStatus(id, "A")
                .flatMap(existing -> {
                    existing.setImageUrl(updated.getImageUrl());
                    existing.setImageName(updated.getImageName());
                    return repository.save(existing);
                });
    }

    public Mono<FaceDetection> disable(UUID id) {
        return repository.findByIdAndStatus(id, "A")
                .flatMap(existing -> { existing.setStatus("I"); return repository.save(existing); });
    }

    public Mono<FaceDetection> enable(UUID id) {
        return repository.findByIdAndStatus(id, "I")
                .flatMap(existing -> { existing.setStatus("A"); return repository.save(existing); });
    }
}
