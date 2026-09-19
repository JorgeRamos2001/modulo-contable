package com.modulocontable.dto;

import java.math.BigDecimal;
import java.util.List;

public record LibroMayorResponseDTO(
        Long cuentaId,
        String codigoCuenta,
        String nombreCuenta,
        BigDecimal saldoActual,
        List<MovimientoMayorDTO> movimientos
) {
}
