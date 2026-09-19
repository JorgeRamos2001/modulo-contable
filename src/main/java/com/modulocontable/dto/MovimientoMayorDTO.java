package com.modulocontable.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoMayorDTO(
        LocalDate fecha,
        Integer numeroAsiento,
        String concepto,
        BigDecimal debe,
        BigDecimal haber,
        BigDecimal saldoAcumulado
) {
}
