package com.modulocontable.dto;

import com.modulocontable.model.Cuenta;

import java.math.BigDecimal;

public record CuentaResponseDTO(
        Long id,
        String codigo,
        String nombre,
        Integer nivel,
        Long cuentaPadreId,
        String tipoCuenta,
        BigDecimal saldoActual
) {
    public static CuentaResponseDTO from(Cuenta cuenta) {
        return new CuentaResponseDTO(
                cuenta.getId(),
                cuenta.getCodigo(),
                cuenta.getNombre(),
                cuenta.getNivel(),
                // getId() sobre un proxy lazy no dispara una consulta a la BD (Hibernate ya lo conoce),
                // asi que esto es seguro aunque cuentaPadre no este inicializada.
                cuenta.getCuentaPadre() != null ? cuenta.getCuentaPadre().getId() : null,
                cuenta.getTipoCuenta(),
                cuenta.getSaldoActual()
        );
    }
}
