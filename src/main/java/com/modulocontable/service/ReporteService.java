package com.modulocontable.service;

import com.modulocontable.dto.*;
import com.modulocontable.model.Cuenta;
import com.modulocontable.model.DetalleAsiento;
import com.modulocontable.repository.CuentaRepository;
import com.modulocontable.repository.DetalleAsientoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private static final int NIVEL_SUBCUENTA = 4;

    private final CuentaRepository cuentaRepository;
    private final DetalleAsientoRepository detalleAsientoRepository;

    /**
     * Reconstruye el Libro Mayor de una cuenta: todos sus movimientos en orden
     * cronologico junto con el saldo acumulado tras cada uno. El saldo acumulado
     * del ultimo movimiento debe coincidir con cuenta.getSaldoActual().
     */
    @Transactional(readOnly = true)
    public LibroMayorResponseDTO obtenerLibroMayor(Long cuentaId) {
        Cuenta cuenta = cuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new EntityNotFoundException("Cuenta no encontrada: id=" + cuentaId));

        List<DetalleAsiento> detalles = detalleAsientoRepository.findByCuentaIdConAsientoOrdenado(cuentaId);

        List<MovimientoMayorDTO> movimientos = new ArrayList<>();
        BigDecimal saldoAcumulado = BigDecimal.ZERO;

        for (DetalleAsiento detalle : detalles) {
            saldoAcumulado = saldoAcumulado.add(cuenta.calcularMovimientoNeto(detalle.getDebe(), detalle.getHaber()));
            movimientos.add(new MovimientoMayorDTO(
                    detalle.getAsiento().getFecha(),
                    detalle.getAsiento().getNumeroAsiento(),
                    detalle.getConcepto(),
                    detalle.getDebe(),
                    detalle.getHaber(),
                    saldoAcumulado
            ));
        }

        return new LibroMayorResponseDTO(cuenta.getId(), cuenta.getCodigo(), cuenta.getNombre(),
                cuenta.getSaldoActual(), movimientos);
    }

    /**
     * Balance General: Activo (tipo 1) = Pasivo (tipo 2) + Patrimonio (tipo 3).
     * Solo se listan subcuentas (nivel hoja) con saldo distinto de cero.
     */
    @Transactional(readOnly = true)
    public BalanceGeneralDTO obtenerBalanceGeneral() {
        List<Cuenta> subcuentas = cuentaRepository.findByNivelOrderByCodigoAsc(NIVEL_SUBCUENTA);

        List<CuentaSaldoDTO> activo = filtrarYMapear(subcuentas, "1");
        List<CuentaSaldoDTO> pasivo = filtrarYMapear(subcuentas, "2");
        List<CuentaSaldoDTO> patrimonio = filtrarYMapear(subcuentas, "3");

        BigDecimal totalActivo = sumar(activo);
        BigDecimal totalPasivo = sumar(pasivo);
        BigDecimal totalPatrimonio = sumar(patrimonio);

        boolean cuadrado = totalActivo.compareTo(totalPasivo.add(totalPatrimonio)) == 0;

        return new BalanceGeneralDTO(activo, pasivo, patrimonio, totalActivo, totalPasivo, totalPatrimonio, cuadrado);
    }

    /**
     * Estado de Resultados: Utilidad = Ingresos (tipo 5) - Costos y Gastos (tipo 4).
     * Solo se listan subcuentas (nivel hoja) con saldo distinto de cero.
     */
    @Transactional(readOnly = true)
    public EstadoResultadosDTO obtenerEstadoResultados() {
        List<Cuenta> subcuentas = cuentaRepository.findByNivelOrderByCodigoAsc(NIVEL_SUBCUENTA);

        List<CuentaSaldoDTO> ingresos = filtrarYMapear(subcuentas, "5");
        List<CuentaSaldoDTO> costosYGastos = filtrarYMapear(subcuentas, "4");

        BigDecimal totalIngresos = sumar(ingresos);
        BigDecimal totalCostosYGastos = sumar(costosYGastos);
        BigDecimal utilidad = totalIngresos.subtract(totalCostosYGastos);

        return new EstadoResultadosDTO(ingresos, costosYGastos, totalIngresos, totalCostosYGastos, utilidad);
    }

    private List<CuentaSaldoDTO> filtrarYMapear(List<Cuenta> cuentas, String tipoCuenta) {
        return cuentas.stream()
                .filter(c -> tipoCuenta.equals(c.getTipoCuenta()))
                .filter(c -> c.getSaldoActual().compareTo(BigDecimal.ZERO) != 0)
                .map(c -> new CuentaSaldoDTO(c.getCodigo(), c.getNombre(), c.getSaldoActual()))
                .toList();
    }

    private BigDecimal sumar(List<CuentaSaldoDTO> cuentas) {
        return cuentas.stream()
                .map(CuentaSaldoDTO::saldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
