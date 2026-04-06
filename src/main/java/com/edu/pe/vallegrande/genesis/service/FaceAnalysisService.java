package com.edu.pe.vallegrande.genesis.service;

import com.edu.pe.vallegrande.genesis.model.FaceAnalysis;
import com.edu.pe.vallegrande.genesis.repository.FaceAnalysisRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaceAnalysisService {

    @Qualifier("faceAnalyzerClient")
    private final WebClient faceAnalyzerClient;

    @Qualifier("imageDownloaderClient")
    private final WebClient imageDownloaderClient;

    private final FaceAnalysisRepository repository;
    private final ObjectMapper mapper = new ObjectMapper();

    public Flux<FaceAnalysis> findAllActive()   { return repository.findByStatus("A"); }
    public Flux<FaceAnalysis> findAllInactive() { return repository.findByStatus("I"); }
    public Mono<FaceAnalysis> findById(UUID id) { return repository.findByIdAndStatus(id, "A"); }

    public Mono<FaceAnalysis> analyze(String imageUrl) {
        log.info("Downloading image for analysis: {}", imageUrl);

        return imageDownloaderClient.get()
                .uri(imageUrl)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> {
                            log.error("Failed to download image [{}]: HTTP {}", imageUrl, response.statusCode());
                            return Mono.error(new RuntimeException("No se pudo descargar la imagen (HTTP " + response.statusCode() + "). Verifica que la URL sea pública y accesible."));
                        }
                )
                .bodyToFlux(DataBuffer.class)
                .as(DataBufferUtils::join)
                .flatMap(dataBuffer -> {
                    byte[] imageBytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(imageBytes);
                    DataBufferUtils.release(dataBuffer);

                    if (imageBytes.length == 0) {
                        return Mono.error(new RuntimeException("La imagen descargada está vacía. Verifica la URL."));
                    }

                    log.info("Image downloaded successfully, size: {} bytes", imageBytes.length);

                    MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
                    bodyBuilder.part("image_input", imageBytes)
                            .filename("face.jpg")
                            .contentType(MediaType.IMAGE_JPEG);

                    return faceAnalyzerClient.post()
                            .uri("/face_analysis")
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                            .retrieve()
                            .onStatus(
                                    status -> status.is4xxClientError() || status.is5xxServerError(),
                                    response -> response.bodyToMono(String.class)
                                            .doOnNext(b -> log.error("Face-analysis API error [{}]: {}", response.statusCode(), b))
                                            .flatMap(b -> Mono.error(new RuntimeException("El servicio de análisis facial respondió con error [" + response.statusCode() + "]: " + b)))
                            )
                            .bodyToMono(JsonNode.class)
                            .doOnNext(json -> log.info("Analysis response: {}", json))
                            .flatMap(json -> buildAndSave(imageUrl, json));
                })
                .onErrorResume(TimeoutException.class, e -> {
                    log.error("Timeout downloading image: {}", imageUrl);
                    return saveErrorRecord(imageUrl, "Timeout al descargar la imagen. Intenta con una URL más rápida.");
                })
                .onErrorResume(WebClientRequestException.class, e -> {
                    log.error("Connection error for image URL {}: {}", imageUrl, e.getMessage());
                    return saveErrorRecord(imageUrl, "No se pudo conectar a la URL de la imagen: " + e.getMessage());
                })
                .onErrorResume(e -> !(e instanceof RuntimeException) || e.getMessage() == null, e -> {
                    log.error("Unexpected error in analyze(): {}", e.getMessage(), e);
                    return saveErrorRecord(imageUrl, "Error inesperado: " + e.getMessage());
                });
    }

    private Mono<FaceAnalysis> buildAndSave(String imageUrl, JsonNode json) {
        boolean success = "success".equals(json.path("status").asText());
        ArrayNode facesArray = mapper.createArrayNode();

        if (success) {
            JsonNode results = json.path("analysis_result");
            if (results.isEmpty()) {
                log.warn("No faces detected in image: {}", imageUrl);
            }
            for (JsonNode face : results) {
                ObjectNode f = mapper.createObjectNode();
                f.put("age",                  Math.round(face.path("age").asDouble()));
                f.put("gender",               face.path("gender").asText(null));
                f.put("gender_probability",   Math.round(face.path("gender_probability").asDouble() * 1000.0) / 10.0);
                f.put("emotion",              face.path("emotion").asText(null));
                f.put("emotion_probability",  Math.round(face.path("emotion_probability").asDouble() * 1000.0) / 10.0);
                f.put("wear_facemask",        face.path("wear_facemask").asBoolean(false));
                f.put("is_real_face",         face.path("liveness").path("is_real_face").asBoolean(false));
                f.put("liveness_probability", Math.round(face.path("liveness").path("liveness_probability").asDouble() * 1000.0) / 10.0);
                f.set("bbox",                 face.path("bbox"));
                facesArray.add(f);
            }
        }

        FaceAnalysis analysis = FaceAnalysis.builder()
                .imageUrl(imageUrl)
                .requestId(json.path("image_file_name").asText(null))
                .errorCode(success ? 0 : 1)
                .errorMsg(success ? null : json.path("status").asText())
                .faceList(facesArray.toString())
                .createdAt(LocalDateTime.now())
                .status("A")
                .build();
        return repository.save(analysis);
    }

    /** Guarda un registro de error en BD para que el frontend reciba algo coherente */
    private Mono<FaceAnalysis> saveErrorRecord(String imageUrl, String errorMsg) {
        FaceAnalysis errorRecord = FaceAnalysis.builder()
                .imageUrl(imageUrl)
                .errorCode(1)
                .errorMsg(errorMsg)
                .faceList("[]")
                .createdAt(LocalDateTime.now())
                .status("A")
                .build();
        return repository.save(errorRecord);
    }

    public Mono<FaceAnalysis> update(UUID id, FaceAnalysis updated) {
        return repository.findByIdAndStatus(id, "A")
                .flatMap(existing -> {
                    existing.setImageUrl(updated.getImageUrl());
                    return repository.save(existing);
                });
    }

    public Mono<FaceAnalysis> disable(UUID id) {
        return repository.findById(id)
                .flatMap(existing -> { existing.setStatus("I"); return repository.save(existing); });
    }

    public Mono<FaceAnalysis> enable(UUID id) {
        return repository.findById(id)
                .flatMap(existing -> { existing.setStatus("A"); return repository.save(existing); });
    }
}
