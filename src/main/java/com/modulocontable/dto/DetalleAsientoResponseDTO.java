package com.modulocontable.dto;

import com.modulocontable.model.DetalleAsiento;

import java.math.BigDecimal;

public record DetalleAsientoResponseDTO(
        Long id,
        Long cuentaId,
        String codigoCuenta,
        String nombreCuenta,
        String concepto,
        BigDecimal debe,
        BigDecimal haber
) {
    public static DetalleAsientoResponseDTO from(DetalleAsiento detalle) {
        return new DetalleAsientoResponseDTO(
                detalle.getId(),
                detalle.getCuenta().getId(),
                detalle.getCuenta().getCodigo(),
                detalle.getCuenta().getNombre(),
                detalle.getConcepto(),
                detalle.getDebe(),
                detalle.getHaber()
        );
    }
}
