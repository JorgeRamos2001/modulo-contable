package com.modulocontable.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DetalleAsientoRequestDTO(

        @NotNull(message = "La cuenta es obligatoria")
        Long cuentaId,

        String concepto,

        @NotNull(message = "El debe es obligatorio (puede ser 0)")
        @DecimalMin(value = "0.0", message = "El debe no puede ser negativo")
        BigDecimal debe,

        @NotNull(message = "El haber es obligatorio (puede ser 0)")
        @DecimalMin(value = "0.0", message = "El haber no puede ser negativo")
        BigDecimal haber
) {
}
