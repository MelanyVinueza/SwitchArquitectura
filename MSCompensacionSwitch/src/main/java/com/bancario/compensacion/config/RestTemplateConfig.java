package com.bancario.compensacion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuración de beans para comunicación HTTP entre microservicios.
 * Usado por CompensacionServicio para invocar la liquidación masiva en
 * MS-CONTABILIDAD.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
