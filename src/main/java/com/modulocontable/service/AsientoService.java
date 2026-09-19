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
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AsientoService {

    private static final String TIPO_ACTIVO = "1";
    private static final String TIPO_PATRIMONIO = "3";

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

        boolean esPrimerAsiento = asientoRepository.count() == 0;

        List<Cuenta> cuentasResueltas = resolverYValidarCuentas(request);

        if (esPrimerAsiento) {
            validarAsientoDeConstitucion(request, cuentasResueltas);
        }

        Asiento asiento = Asiento.builder()
                .numeroAsiento(siguienteNumeroAsiento())
                .fecha(request.fecha())
                .concepto(request.concepto())
                .build();

        for (int i = 0; i < request.detalles().size(); i++) {
            DetalleAsientoRequestDTO detalleDto = request.detalles().get(i);
            Cuenta cuenta = cuentasResueltas.get(i);

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

    /** Busca cada cuenta referenciada y valida que sea subcuenta (nivel hoja). */
    private List<Cuenta> resolverYValidarCuentas(AsientoRequestDTO request) {
        List<Cuenta> cuentas = new ArrayList<>();
        for (DetalleAsientoRequestDTO detalleDto : request.detalles()) {
            Cuenta cuenta = cuentaRepository.findById(detalleDto.cuentaId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Cuenta no encontrada: id=" + detalleDto.cuentaId()));

            if (!Integer.valueOf(4).equals(cuenta.getNivel())) {
                throw new IllegalArgumentException(
                        "No se puede contabilizar contra '%s - %s' porque no es una subcuenta (nivel hoja). Usa una cuenta de 6 digitos."
                                .formatted(cuenta.getCodigo(), cuenta.getNombre()));
            }
            cuentas.add(cuenta);
        }
        return cuentas;
    }

    /**
     * El primer asiento del sistema debe ser la constitucion de la empresa:
     * los aportes de los socios (Debe = cuentas de Activo: caja, bancos, vehiculos,
     * inventario, etc.) contra el Capital Social (Haber = cuentas de Patrimonio).
     */
    private void validarAsientoDeConstitucion(AsientoRequestDTO request, List<Cuenta> cuentasResueltas) {
        for (int i = 0; i < request.detalles().size(); i++) {
            DetalleAsientoRequestDTO detalleDto = request.detalles().get(i);
            Cuenta cuenta = cuentasResueltas.get(i);
            boolean esDebe = detalleDto.debe().compareTo(BigDecimal.ZERO) > 0;

            if (esDebe && !TIPO_ACTIVO.equals(cuenta.getTipoCuenta())) {
                throw new IllegalArgumentException(
                        "El primer asiento debe ser la constitucion de la empresa: en el Debe solo se permiten cuentas de Activo (aportes de los socios). '%s - %s' no es una cuenta de Activo."
                                .formatted(cuenta.getCodigo(), cuenta.getNombre()));
            }
            if (!esDebe && !TIPO_PATRIMONIO.equals(cuenta.getTipoCuenta())) {
                throw new IllegalArgumentException(
                        "El primer asiento debe ser la constitucion de la empresa: en el Haber solo se permiten cuentas de Patrimonio (Capital Social). '%s - %s' no es una cuenta de Patrimonio."
                                .formatted(cuenta.getCodigo(), cuenta.getNombre()));
            }
        }
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