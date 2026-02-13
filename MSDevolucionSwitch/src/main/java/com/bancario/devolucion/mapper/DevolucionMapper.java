package com.bancario.devolucion.mapper;

import com.bancario.devolucion.dto.SolicitudDevolucionDTO;
import com.bancario.devolucion.modelo.SolicitudDevolucion;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DevolucionMapper {

    public SolicitudDevolucionDTO toDTO(SolicitudDevolucion entity) {
        if (entity == null)
            return null;
        return SolicitudDevolucionDTO.builder()
                .id(entity.getId())
                .idInstruccionOriginal(entity.getIdInstruccionOriginal())
                .codigoMotivo(entity.getCodigoMotivo())
                .descripcionMotivo(entity.getDescripcionMotivo()) // Ahora es directo
                .estado(entity.getEstado())
                .fechaSolicitud(entity.getFechaSolicitud())
                .build();
    }

    public List<SolicitudDevolucionDTO> toSolicitudList(List<SolicitudDevolucion> entities) {
        if (entities == null)
            return List.of();
        return entities.stream().map(this::toDTO).collect(Collectors.toList());
    }
}
