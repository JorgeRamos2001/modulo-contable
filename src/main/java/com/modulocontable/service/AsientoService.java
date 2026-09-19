package com.modulocontable.service;

import com.modulocontable.dto.AsientoRequestDTO;
import com.modulocontable.dto.DetalleAsientoRequestDTO;
import com.modulocontable.exception.PartidaDobleException;
import com.modulocontable.model.Asiento;
import com.modulocontable.model.Cuenta;
import com.modulocontable.model.DetalleAsiento;
import com.modulocontable.repository.AsientoRepository;
import com.modulocontable.repository.CuentaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AsientoService {

    private final AsientoRepository asientoRepository;
    private final CuentaRepository cuentaRepository;
    private final MayorizacionService mayorizacionService;

    /**
     * Crea un asiento completo (cabecera + detalles), validando la Partida Doble
     * y mayorizando (actualizando saldos) en la misma transaccion. Si algo falla,
     * no se guarda nada (rollback) y no se afecta ningun saldo.
     */
    @Transactional
    public Asiento crearAsiento(AsientoRequestDTO request) {
        validarPartidaDoble(request);

        Asiento asiento = Asiento.builder()
                .numeroAsiento(siguienteNumeroAsiento())
                .fecha(request.fecha())
                .concepto(request.concepto())
                .build();

        for (DetalleAsientoRequestDTO detalleDto : request.detalles()) {
            Cuenta cuenta = cuentaRepository.findById(detalleDto.cuentaId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Cuenta no encontrada: id=" + detalleDto.cuentaId()));

            if (!Integer.valueOf(4).equals(cuenta.getNivel())) {
                throw new IllegalArgumentException(
                        "No se puede contabilizar contra '%s - %s' porque no es una subcuenta (nivel hoja). Usa una cuenta de 6 digitos."
                                .formatted(cuenta.getCodigo(), cuenta.getNombre()));
            }

            DetalleAsiento detalle = DetalleAsiento.builder()
                    .cuenta(cuenta)
                    .concepto(detalleDto.concepto())
                    .debe(detalleDto.debe())
                    .haber(detalleDto.haber())
                    .build();

            asiento.agregarDetalle(detalle);
        }

        Asiento guardado = asientoRepository.save(asiento);
        mayorizacionService.actualizarSaldos(guardado.getDetalles());
        return guardado;
    }

    private void validarPartidaDoble(AsientoRequestDTO request) {
        BigDecimal totalDebe = request.detalles().stream()
                .map(DetalleAsientoRequestDTO::debe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalHaber = request.detalles().stream()
                .map(DetalleAsientoRequestDTO::haber)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebe.compareTo(totalHaber) != 0) {
            throw new PartidaDobleException(totalDebe, totalHaber);
        }
        if (totalDebe.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("El asiento no puede tener todos los montos en cero");
        }
    }

    private Integer siguienteNumeroAsiento() {
        return asientoRepository.findTopByOrderByNumeroAsientoDesc()
                .map(a -> a.getNumeroAsiento() + 1)
                .orElse(1);
    }
}