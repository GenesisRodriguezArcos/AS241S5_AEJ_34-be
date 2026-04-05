package com.edu.pe.vallegrande.genesis.rest;

import com.edu.pe.vallegrande.genesis.model.FaceDetection;
import com.edu.pe.vallegrande.genesis.service.FaceDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/face-detection")
@RequiredArgsConstructor
public class FaceDetectionController {

    private final FaceDetectionService service;

    @GetMapping
    public Flux<FaceDetection> findAllActive() {
        return service.findAllActive();
    }

    @GetMapping("/inactive")
    public Flux<FaceDetection> findAllInactive() {
        return service.findAllInactive();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<FaceDetection>> findById(@PathVariable UUID id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/detect")
    public Mono<FaceDetection> detect(@RequestParam String imageUrl) {
        return service.detect(imageUrl);
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<FaceDetection>> update(@PathVariable UUID id,
                                                      @RequestBody FaceDetection body) {
        return service.update(id, body)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // Borrado lógico
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<FaceDetection>> disable(@PathVariable UUID id) {
        return service.disable(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // Restaurar registro
    @PatchMapping("/{id}/enable")
    public Mono<ResponseEntity<FaceDetection>> enable(@PathVariable UUID id) {
        return service.enable(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
