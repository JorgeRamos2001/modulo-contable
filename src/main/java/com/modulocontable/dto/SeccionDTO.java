package com.modulocontable.dto;

import java.math.BigDecimal;
import java.util.List;

public record SeccionDTO(
        List<CuentaSaldoDTO> cuentas,
        BigDecimal total
) {
}
