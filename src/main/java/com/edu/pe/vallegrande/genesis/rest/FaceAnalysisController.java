package com.edu.pe.vallegrande.genesis.rest;

import com.edu.pe.vallegrande.genesis.model.FaceAnalysis;
import com.edu.pe.vallegrande.genesis.service.FaceAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/face-analysis")
@RequiredArgsConstructor
public class FaceAnalysisController {

    private final FaceAnalysisService service;

    @GetMapping
    public Flux<FaceAnalysis> findAllActive() {
        return service.findAllActive();
    }

    @GetMapping("/inactive")
    public Flux<FaceAnalysis> findAllInactive() {
        return service.findAllInactive();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<FaceAnalysis>> findById(@PathVariable UUID id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/analyze")
    public Mono<FaceAnalysis> analyze(@RequestParam String imageUrl) {
        return service.analyze(imageUrl);
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<FaceAnalysis>> update(@PathVariable UUID id,
                                                     @RequestBody FaceAnalysis body) {
        return service.update(id, body)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // Borrado lógico
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<FaceAnalysis>> disable(@PathVariable UUID id) {
        return service.disable(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // Restaurar registro
    @PatchMapping("/{id}/enable")
    public Mono<ResponseEntity<FaceAnalysis>> enable(@PathVariable UUID id) {
        return service.enable(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
