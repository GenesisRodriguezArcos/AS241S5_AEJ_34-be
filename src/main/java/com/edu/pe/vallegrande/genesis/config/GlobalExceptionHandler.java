package com.edu.pe.vallegrande.genesis.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleRuntime(RuntimeException ex) {
        log.error("RuntimeException: {}", ex.getMessage());

        // Si el error viene de la API externa con 405, dar mensaje claro
        String message = ex.getMessage() != null ? ex.getMessage() : "Error desconocido";
        HttpStatus status = HttpStatus.BAD_GATEWAY;

        if (message.contains("405")) {
            message = "La API externa no soporta este método. Verifica la URL y el endpoint configurado.";
        } else if (message.contains("404")) {
            message = "Registro no encontrado.";
            status = HttpStatus.NOT_FOUND;
        }

        return Mono.just(ResponseEntity.status(status).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", "Error al procesar la solicitud",
                "message", message
        )));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleGeneral(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 500,
                "error", "Error interno del servidor",
                "message", ex.getMessage() != null ? ex.getMessage() : "Error desconocido"
        )));
    }
}
