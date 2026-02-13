package com.bancario.msdirectorio.servicio;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.bancario.msdirectorio.dto.InstitucionDTO;
import com.bancario.msdirectorio.modelo.Institucion;
import com.bancario.msdirectorio.modelo.InterruptorCircuito;
import com.bancario.msdirectorio.modelo.ReglaEnrutamiento;
import com.bancario.msdirectorio.repositorio.InstitucionRepositorio;
import com.bancario.msdirectorio.mapper.InstitucionMapper;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectorioServicio {

    private final InstitucionRepositorio institucionRepositorio;
    private final RedisTemplate<String, Object> redisTemplate;
    private final InstitucionMapper mapper;

    private static final String CACHE_KEY_PREFIX = "lookup:bin:";

    public InstitucionDTO registrarInstitucion(@NonNull InstitucionDTO dto) {

        Institucion institucion = mapper.toEntity(dto);
        log.info("Registrando nueva institución: {}", dto.getCodigoBic());

        if (institucion.getCodigoBic() == null) {
            throw new IllegalArgumentException("El codigoBic no puede ser nulo");
        }

        if (institucionRepositorio.findByCodigoBic(institucion.getCodigoBic()).isPresent()) {
            throw new RuntimeException("El banco con BIC " + institucion.getCodigoBic() + " ya existe.");
        }

        if (institucion.getInterruptorCircuito() == null) {
            institucion.setInterruptorCircuito(new InterruptorCircuito(false, 0, null));
        }

        if (institucion.getReglasEnrutamiento() == null) {
            institucion.setReglasEnrutamiento(new ArrayList<>());
        }

        Institucion saved = institucionRepositorio.save(institucion);
        return mapper.toDTO(saved);
    }

    public List<InstitucionDTO> listarTodas() {
        List<Institucion> all = StreamSupport.stream(institucionRepositorio.findAll().spliterator(), false)
                .collect(Collectors.toList());
        return mapper.toDTOList(all);
    }

    public Optional<InstitucionDTO> buscarPorBic(String bic) {
        log.info("Buscando Institución por BIC: {}", bic);
        if (bic == null)
            return Optional.empty();

        return institucionRepositorio.findByCodigoBic(bic)
                .filter(this::validarDisponibilidad)
                .map(mapper::toDTO);
    }

    public InstitucionDTO aniadirRegla(@NonNull String bic, @NonNull InstitucionDTO.ReglaDTO nuevaReglaDTO) {
        Institucion inst = institucionRepositorio.findByCodigoBic(bic)
                .orElseThrow(() -> new RuntimeException("Banco no encontrado: " + bic));

        if (inst.getReglasEnrutamiento() == null) {
            inst.setReglasEnrutamiento(new ArrayList<>());
        }

        ReglaEnrutamiento nuevaRegla = new ReglaEnrutamiento(nuevaReglaDTO.getPrefijoBin(), nuevaReglaDTO.getAgente());
        inst.getReglasEnrutamiento().add(nuevaRegla);
        try {
            redisTemplate.delete(CACHE_KEY_PREFIX + nuevaRegla.getPrefijoBin());
        } catch (Exception e) {
            log.warn("Redis no disponible para invalidar cache: {}", e.getMessage());
        }

        return mapper.toDTO(institucionRepositorio.save(inst));
    }

    /**
     * Busca un banco por BIN (prefijo de cuenta).
     * DynamoDB no soporta queries sobre nested lists, así que hacemos scan + filtro
     * en memoria.
     */
    public Optional<InstitucionDTO> descubrirBancoPorBin(String bin) {
        log.info("Resolviendo BIN: {}", bin);
        if (bin == null)
            return Optional.empty();
        String cacheKey = CACHE_KEY_PREFIX + bin;

        try {
            Object cacheData = redisTemplate.opsForValue().get(cacheKey);
            if (cacheData instanceof InstitucionDTO) {
                return Optional.of((InstitucionDTO) cacheData);
            }
        } catch (Exception e) {
            log.warn("Redis no disponible para cache lookup: {}", e.getMessage());
        }

        // Scan all institutions and filter by BIN prefix
        return StreamSupport.stream(institucionRepositorio.findAll().spliterator(), false)
                .filter(inst -> inst.getReglasEnrutamiento() != null &&
                        inst.getReglasEnrutamiento().stream()
                                .anyMatch(r -> bin.equals(r.getPrefijoBin())))
                .filter(this::validarDisponibilidad)
                .findFirst()
                .map(inst -> {
                    InstitucionDTO dto = mapper.toDTO(inst);
                    try {
                        redisTemplate.opsForValue().set(cacheKey, dto, Duration.ofHours(1));
                    } catch (Exception e) {
                        log.warn("Redis no disponible para guardar cache: {}", e.getMessage());
                    }
                    return dto;
                });
    }

    public void registrarFallo(String bic) {
        log.warn("Registrando fallo operativo para: {}", bic);
        if (bic == null)
            return;

        institucionRepositorio.findByCodigoBic(bic).ifPresent(inst -> {
            InterruptorCircuito interruptor = inst.getInterruptorCircuito();
            if (interruptor == null) {
                interruptor = new InterruptorCircuito(false, 0, null);
                inst.setInterruptorCircuito(interruptor);
            }

            interruptor.setFallosConsecutivos(interruptor.getFallosConsecutivos() + 1);
            interruptor.setUltimoFallo(LocalDateTime.now(ZoneOffset.UTC));

            if (interruptor.getFallosConsecutivos() >= 5) {
                interruptor.setEstaAbierto(true);
                log.error(">>> CIRCUIT BREAKER ACTIVADO para banco: {}", bic);
                invalidarCacheDelBanco(inst);
            }

            institucionRepositorio.save(inst);
        });
    }

    private void invalidarCacheDelBanco(Institucion inst) {
        if (inst.getReglasEnrutamiento() != null) {
            inst.getReglasEnrutamiento().forEach(r -> {
                try {
                    redisTemplate.delete(CACHE_KEY_PREFIX + r.getPrefijoBin());
                } catch (Exception e) {
                    log.warn("Redis no disponible para invalidar cache: {}", e.getMessage());
                }
            });
        }
    }

    private boolean validarDisponibilidad(@NonNull Institucion inst) {
        InterruptorCircuito interruptor = inst.getInterruptorCircuito();

        if (interruptor == null || !interruptor.isEstaAbierto()) {
            return true;
        }

        if (interruptor.getUltimoFallo() != null) {
            long segundos = ChronoUnit.SECONDS.between(interruptor.getUltimoFallo(), LocalDateTime.now(ZoneOffset.UTC));

            if (segundos > 30) {
                interruptor.setEstaAbierto(false);
                interruptor.setFallosConsecutivos(0);
                institucionRepositorio.save(inst);
                log.info(">>> CIRCUIT BREAKER CERRADO (Auto-recuperación) para banco: {}", inst.getCodigoBic());
                return true;
            }
            return false;
        }

        return false;
    }

    public InstitucionDTO actualizarParametrosRestringidos(String bic, String nuevoEstado, String nuevaUrl) {

        Institucion inst = institucionRepositorio.findByCodigoBic(bic)
                .orElseThrow(() -> new RuntimeException("Institución no encontrada"));

        if (nuevoEstado != null) {
            try {
                Institucion.Estado estadoEnum = Institucion.Estado.valueOf(nuevoEstado.toUpperCase());
                inst.setEstadoOperativo(estadoEnum.name());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Estado inválido. Use: ONLINE, OFFLINE, MANT, SUSPENDIDO o SOLO_RECIBIR");
            }
        }

        if (nuevaUrl != null && !nuevaUrl.isBlank()) {
            inst.setUrlDestino(nuevaUrl);
        }

        invalidarCacheDelBanco(inst);

        return mapper.toDTO(institucionRepositorio.save(inst));
    }
}
