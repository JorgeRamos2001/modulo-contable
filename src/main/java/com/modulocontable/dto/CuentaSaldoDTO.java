package com.modulocontable.dto;

import java.math.BigDecimal;

public record CuentaSaldoDTO(
        String codigo,
        String nombre,
        BigDecimal saldo
) {
}
