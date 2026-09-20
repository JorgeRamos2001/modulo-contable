package com.modulocontable.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BalanceGeneralDTO(
        String nombreEmpresa,
        LocalDate fechaCorte,
        SeccionDTO activoCorriente,
        SeccionDTO activoNoCorriente,
        BigDecimal totalActivo,
        SeccionDTO pasivoCorriente,
        SeccionDTO pasivoNoCorriente,
        BigDecimal totalPasivo,
        SeccionDTO capitalContable,
        BigDecimal totalPasivoYCapital,
        /** true si Activo = Pasivo + Capital Contable */
        boolean cuadrado
) {
}