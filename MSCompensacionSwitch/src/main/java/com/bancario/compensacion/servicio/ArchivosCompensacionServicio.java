package com.bancario.compensacion.servicio;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio para el procesamiento de archivos de compensación recibidos via
 * APIM.
 */
@Slf4j
@Service
public class ArchivosCompensacionServicio {

    /**
     * Procesa un archivo de compensación (CSV o Texto) y extrae los datos.
     * 
     * @param file             El archivo cargado.
     * @param compensationDate Fecha opcional de la compensación.
     * @return Mapa con el resumen del procesamiento.
     */
    public Map<String, Object> procesarArchivo(MultipartFile file, String compensationDate) {
        log.info("Procesando archivo: {}, para fecha: {}", file.getOriginalFilename(), compensationDate);

        if (file.isEmpty()) {
            throw new RuntimeException("El archivo está vacío");
        }

        List<String> recordsFound = new ArrayList<>();
        int count = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                // Lógica de parsing simple para demostración/pruebas
                if (!line.trim().isEmpty()) {
                    recordsFound.add(line);
                    count++;
                }
            }

            log.info("Archivo procesado. Se encontraron {} registros.", count);

            Map<String, Object> response = new HashMap<>();
            response.put("processedRecords", count);
            response.put("fileName", file.getOriginalFilename());
            response.put("date", compensationDate != null ? compensationDate : "CURRENT_CYCLE");
            response.put("preview", recordsFound.size() > 5 ? recordsFound.subList(0, 5) : recordsFound);

            return response;

        } catch (Exception e) {
            log.error("Error leyendo archivo de compensación", e);
            throw new RuntimeException("Error al leer el contenido del archivo: " + e.getMessage());
        }
    }
}
