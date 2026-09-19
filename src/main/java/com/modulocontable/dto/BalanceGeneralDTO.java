package com.modulocontable.dto;

import java.math.BigDecimal;
import java.util.List;

public record BalanceGeneralDTO(
        List<CuentaSaldoDTO> activo,
        List<CuentaSaldoDTO> pasivo,
        List<CuentaSaldoDTO> patrimonio,
        BigDecimal totalActivo,
        BigDecimal totalPasivo,
        BigDecimal totalPatrimonio,
        /** true si Activo = Pasivo + Patrimonio */
        boolean cuadrado
) {
}
