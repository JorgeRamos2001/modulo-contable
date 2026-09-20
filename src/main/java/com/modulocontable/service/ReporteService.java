package com.modulocontable.service;

import com.modulocontable.dto.*;
import com.modulocontable.model.Cuenta;
import com.modulocontable.model.DetalleAsiento;
import com.modulocontable.repository.CuentaRepository;
import com.modulocontable.repository.DetalleAsientoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private static final int NIVEL_SUBCUENTA = 4;

    private final CuentaRepository cuentaRepository;
    private final DetalleAsientoRepository detalleAsientoRepository;

    @Value("${app.empresa.nombre:Mi Empresa, S.A. de C.V.}")
    private String nombreEmpresa;

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
     * Balance General agrupado en Corriente / No Corriente (usando el Rubro de 2 digitos
     * del catalogo), con la Utilidad del Ejercicio calculada en vivo a partir del Estado
     * de Resultados (todavia no manejamos asientos de cierre que la posteen formalmente).
     */
    @Transactional(readOnly = true)
    public BalanceGeneralDTO obtenerBalanceGeneral() {
        List<Cuenta> subcuentas = cuentaRepository.findByNivelOrderByCodigoAsc(NIVEL_SUBCUENTA);

        SeccionDTO activoCorriente = seccionPorPrefijo(subcuentas, "11");
        SeccionDTO activoNoCorriente = seccionPorPrefijo(subcuentas, "12");
        BigDecimal totalActivo = activoCorriente.total().add(activoNoCorriente.total());

        SeccionDTO pasivoCorriente = seccionPorPrefijo(subcuentas, "21");
        SeccionDTO pasivoNoCorriente = seccionPorPrefijo(subcuentas, "22");
        BigDecimal totalPasivo = pasivoCorriente.total().add(pasivoNoCorriente.total());

        SeccionDTO capitalContable = construirCapitalContable(subcuentas);

        BigDecimal totalPasivoYCapital = totalPasivo.add(capitalContable.total());
        boolean cuadrado = totalActivo.compareTo(totalPasivoYCapital) == 0;

        return new BalanceGeneralDTO(nombreEmpresa, LocalDate.now(), activoCorriente, activoNoCorriente,
                totalActivo, pasivoCorriente, pasivoNoCorriente, totalPasivo, capitalContable,
                totalPasivoYCapital, cuadrado);
    }

    /**
     * Capital y Reservas (rubro 31) + Resultados por Aplicar de periodos anteriores (rubro 32,
     * si alguna vez se postean via cierre) + la Utilidad del Ejercicio actual, calculada en vivo.
     */
    private SeccionDTO construirCapitalContable(List<Cuenta> subcuentas) {
        SeccionDTO capitalYReservas = seccionPorPrefijo(subcuentas, "31");

        List<CuentaSaldoDTO> resultadosAnteriores = subcuentas.stream()
                .filter(c -> c.getCodigo().startsWith("32"))
                .filter(c -> !c.getCodigo().equals("320201")) // reemplazado por la utilidad calculada abajo
                .filter(c -> c.getSaldoActual().compareTo(BigDecimal.ZERO) != 0)
                .map(c -> new CuentaSaldoDTO(c.getCodigo(), c.getNombre(), c.getSaldoActual()))
                .toList();

        BigDecimal utilidadDelEjercicio = obtenerEstadoResultados().utilidadAntesImpuestos();

        List<CuentaSaldoDTO> cuentas = new ArrayList<>();
        cuentas.addAll(capitalYReservas.cuentas());
        cuentas.addAll(resultadosAnteriores);
        cuentas.add(new CuentaSaldoDTO("3202", "Utilidad del Ejercicio (calculada)", utilidadDelEjercicio));

        BigDecimal totalResultadosAnteriores = resultadosAnteriores.stream()
                .map(CuentaSaldoDTO::saldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = capitalYReservas.total().add(totalResultadosAnteriores).add(utilidadDelEjercicio);

        return new SeccionDTO(cuentas, total);
    }

    /**
     * Estado de Resultados en formato cascada. Cada bloque se calcula a partir de las
     * subcuentas cuyo codigo de Cuenta de Mayor (4 digitos) corresponde a ese concepto.
     */
    @Transactional(readOnly = true)
    public EstadoResultadosDTO obtenerEstadoResultados() {
        List<Cuenta> subcuentas = cuentaRepository.findByNivelOrderByCodigoAsc(NIVEL_SUBCUENTA);

        BigDecimal ventas = sumarPorPrefijoExcluyendo(subcuentas, "5101", "510104");
        BigDecimal devolucionesRebajasVentas = sumarPorPrefijo(subcuentas, "510104");
        BigDecimal ventasNetas = ventas.subtract(devolucionesRebajasVentas);

        BigDecimal costoVentas = sumarPorPrefijo(subcuentas, "4101");
        BigDecimal utilidadBruta = ventasNetas.subtract(costoVentas);

        BigDecimal gastosVenta = sumarPorPrefijo(subcuentas, "4103");
        BigDecimal gastosAdministracion = sumarPorPrefijo(subcuentas, "4102");
        BigDecimal totalGastosOperacion = gastosVenta.add(gastosAdministracion);
        BigDecimal utilidadOperacional = utilidadBruta.subtract(totalGastosOperacion);

        BigDecimal productosFinancieros = sumarPorPrefijo(subcuentas, "5201");
        BigDecimal gastosFinancieros = sumarPorPrefijo(subcuentas, "4201");
        BigDecimal utilidadFinanciera = productosFinancieros.subtract(gastosFinancieros);

        BigDecimal otrosProductos = sumarPorPrefijos(subcuentas, "5202", "5203", "5204");
        BigDecimal otrosGastos = sumarPorPrefijos(subcuentas, "4202", "4203", "4204", "4205", "4206");
        BigDecimal utilidadOtrosProductosYGastos = otrosProductos.subtract(otrosGastos);

        BigDecimal utilidadAjenaActividad = utilidadFinanciera.add(utilidadOtrosProductosYGastos);
        BigDecimal utilidadAntesImpuestos = utilidadOperacional.add(utilidadAjenaActividad);

        return new EstadoResultadosDTO(ventas, devolucionesRebajasVentas, ventasNetas, costoVentas, utilidadBruta,
                gastosVenta, gastosAdministracion, totalGastosOperacion, utilidadOperacional,
                productosFinancieros, gastosFinancieros, utilidadFinanciera,
                otrosProductos, otrosGastos, utilidadOtrosProductosYGastos,
                utilidadAjenaActividad, utilidadAntesImpuestos);
    }

    // ---------- helpers ----------

    private SeccionDTO seccionPorPrefijo(List<Cuenta> cuentas, String prefijoRubro) {
        List<CuentaSaldoDTO> lista = cuentas.stream()
                .filter(c -> c.getCodigo().startsWith(prefijoRubro))
                .filter(c -> c.getSaldoActual().compareTo(BigDecimal.ZERO) != 0)
                .map(c -> new CuentaSaldoDTO(c.getCodigo(), c.getNombre(), c.getSaldoActual()))
                .toList();
        BigDecimal total = lista.stream().map(CuentaSaldoDTO::saldo).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SeccionDTO(lista, total);
    }

    private BigDecimal sumarPorPrefijo(List<Cuenta> cuentas, String prefijo) {
        return cuentas.stream()
                .filter(c -> c.getCodigo().startsWith(prefijo))
                .map(Cuenta::getSaldoActual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumarPorPrefijos(List<Cuenta> cuentas, String... prefijos) {
        return cuentas.stream()
                .filter(c -> {
                    for (String p : prefijos) {
                        if (c.getCodigo().startsWith(p)) return true;
                    }
                    return false;
                })
                .map(Cuenta::getSaldoActual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumarPorPrefijoExcluyendo(List<Cuenta> cuentas, String prefijo, String codigoExcluido) {
        return cuentas.stream()
                .filter(c -> c.getCodigo().startsWith(prefijo))
                .filter(c -> !c.getCodigo().equals(codigoExcluido))
                .map(Cuenta::getSaldoActual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}