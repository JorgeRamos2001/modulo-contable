package com.modulocontable.dto;

import jakarta.validation.constraints.NotBlank;

public record CuentaRequestDTO(

        @NotBlank(message = "El codigo es obligatorio")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        /** Opcional: null para cuentas raiz (1 digito, ej. "1" = Activo). */
        Long cuentaPadreId
) {
}
