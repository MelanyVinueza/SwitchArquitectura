package com.bancario.compensacion.controlador;

import com.bancario.compensacion.servicio.ArchivosCompensacionServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Controlador compatible con AWS APIM para servicio de Compensación.
 * Expone endpoints en /api/v2/compensation/* configurados en el APIM.
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/compensation")
@RequiredArgsConstructor
@Tag(name = "APIM Compensación", description = "Endpoints de compensación expuestos via AWS API Gateway")
public class ApimCompensacionControlador {

    private final ArchivosCompensacionServicio archivosServicio;

    /**
     * POST /api/v2/compensation/upload
     * Ruta configurada en APIM para subir archivos de compensación.
     * Timeout extendido a 29s en APIM para archivos grandes.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir Archivo de Compensación", description = "Recibe archivos CSV/Excel con datos de compensación para procesamiento batch")
    public ResponseEntity<Map<String, Object>> uploadArchivo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "date", required = false) String compensationDate) {

        log.info("[APIM] Recibido archivo de compensación - Nombre: {}, Tamaño: {} bytes",
                file.getOriginalFilename(), file.getSize());

        try {
            // Delegar al servicio existente
            Object resultado = archivosServicio.procesarArchivo(file, compensationDate);

            return ResponseEntity.ok(Map.of(
                    "status", "COMPLETED",
                    "message", "Archivo procesado exitosamente",
                    "fileName", file.getOriginalFilename(),
                    "result", resultado));

        } catch (Exception e) {
            log.error("[APIM] Error procesando archivo de compensación", e);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
                    "status", "FAILED",
                    "error", "MS03",
                    "message", "Error técnico procesando archivo: " + e.getMessage()));
        }
    }

    /**
     * GET /health
     * Health check para ALB Target Group.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "switch-ms-compensacion",
                "version", "3.0.0"));
    }
}