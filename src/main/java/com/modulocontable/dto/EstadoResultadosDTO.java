package com.modulocontable.dto;

import java.math.BigDecimal;
import java.util.List;

public record EstadoResultadosDTO(
        List<CuentaSaldoDTO> ingresos,
        List<CuentaSaldoDTO> costosYGastos,
        BigDecimal totalIngresos,
        BigDecimal totalCostosYGastos,
        /** ingresos - costosYGastos. Negativo significa perdida del periodo. */
        BigDecimal utilidad
) {
}
