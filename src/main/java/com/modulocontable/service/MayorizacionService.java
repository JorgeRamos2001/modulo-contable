package com.modulocontable.service;

import com.modulocontable.model.Cuenta;
import com.modulocontable.model.DetalleAsiento;
import com.modulocontable.repository.CuentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Responsable de la mayorizacion automatica: cada vez que se guarda un asiento,
 * actualiza en tiempo real el saldo (deudor/acreedor) de cada cuenta afectada,
 * sin necesidad de recalcular desde cero todos los movimientos historicos.
 */
@Service
@RequiredArgsConstructor
public class MayorizacionService {

    private final CuentaRepository cuentaRepository;

    public void actualizarSaldos(List<DetalleAsiento> detalles) {
        for (DetalleAsiento detalle : detalles) {
            Cuenta cuenta = detalle.getCuenta();
            BigDecimal movimientoNeto = calcularMovimientoNeto(cuenta, detalle);
            cuenta.setSaldoActual(cuenta.getSaldoActual().add(movimientoNeto));
            cuentaRepository.save(cuenta);
        }
    }

    /**
     * Cuentas de naturaleza deudora (Activo=1, Costos/Gastos=4): el saldo sube con el Debe.
     * Cuentas de naturaleza acreedora (Pasivo=2, Patrimonio=3, Ingresos=5): el saldo sube con el Haber.
     */
    private BigDecimal calcularMovimientoNeto(Cuenta cuenta, DetalleAsiento detalle) {
        if (cuenta.esNaturalezaDeudora()) {
            return detalle.getDebe().subtract(detalle.getHaber());
        }
        return detalle.getHaber().subtract(detalle.getDebe());
    }
}
