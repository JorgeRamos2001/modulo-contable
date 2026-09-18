package com.modulocontable.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record AsientoRequestDTO(

        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        @NotNull(message = "El concepto es obligatorio")
        String concepto,

        @NotEmpty(message = "El asiento debe tener al menos 2 lineas (una de debe y una de haber)")
        @Valid
        List<DetalleAsientoRequestDTO> detalles
) {
}
