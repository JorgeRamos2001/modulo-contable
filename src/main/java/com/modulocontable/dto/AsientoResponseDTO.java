package com.modulocontable.dto;

import com.modulocontable.model.Asiento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AsientoResponseDTO(
        Long id,
        Integer numeroAsiento,
        LocalDate fecha,
        String concepto,
        LocalDateTime fechaCreacion,
        List<DetalleAsientoResponseDTO> detalles
) {
    public static AsientoResponseDTO from(Asiento asiento) {
        return new AsientoResponseDTO(
                asiento.getId(),
                asiento.getNumeroAsiento(),
                asiento.getFecha(),
                asiento.getConcepto(),
                asiento.getFechaCreacion(),
                asiento.getDetalles().stream()
                        .map(DetalleAsientoResponseDTO::from)
                        .toList()
        );
    }
}
