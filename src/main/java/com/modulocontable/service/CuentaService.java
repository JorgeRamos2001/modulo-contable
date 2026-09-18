package com.modulocontable.service;

import com.modulocontable.dto.CuentaRequestDTO;
import com.modulocontable.model.Cuenta;
import com.modulocontable.repository.CuentaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CuentaService {

    private final CuentaRepository cuentaRepository;

    @Transactional
    public Cuenta crearCuenta(CuentaRequestDTO request) {
        if (cuentaRepository.findByCodigo(request.codigo()).isPresent()) {
            throw new IllegalArgumentException("Ya existe una cuenta con el codigo " + request.codigo());
        }

        Cuenta padre = null;
        if (request.cuentaPadreId() != null) {
            padre = cuentaRepository.findById(request.cuentaPadreId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Cuenta padre no encontrada: id=" + request.cuentaPadreId()));
        }

        Cuenta cuenta = Cuenta.builder()
                .codigo(request.codigo())
                .nombre(request.nombre())
                .nivel(calcularNivel(request.codigo()))
                .cuentaPadre(padre)
                .tipoCuenta(request.codigo().substring(0, 1))
                .saldoActual(BigDecimal.ZERO)
                .build();

        return cuentaRepository.save(cuenta);
    }

    /** El catalogo usa codigos de 1, 2, 4 o 6 digitos (ver TRD). */
    private Integer calcularNivel(String codigo) {
        return switch (codigo.length()) {
            case 1 -> 1;
            case 2 -> 2;
            case 4 -> 3;
            case 6 -> 4;
            default -> throw new IllegalArgumentException(
                    "Codigo invalido '%s': debe tener 1, 2, 4 o 6 digitos".formatted(codigo));
        };
    }
}
