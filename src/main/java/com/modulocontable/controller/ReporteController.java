package com.modulocontable.controller;

import com.modulocontable.dto.BalanceGeneralDTO;
import com.modulocontable.dto.EstadoResultadosDTO;
import com.modulocontable.dto.LibroMayorResponseDTO;
import com.modulocontable.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    /** Libro Mayor: movimientos y saldo acumulado de una cuenta especifica. */
    @GetMapping("/api/mayor/{cuentaId}")
    public LibroMayorResponseDTO libroMayor(@PathVariable Long cuentaId) {
        return reporteService.obtenerLibroMayor(cuentaId);
    }

    /** Balance General dinamico: Activo = Pasivo + Patrimonio. */
    @GetMapping("/api/reportes/balance-general")
    public BalanceGeneralDTO balanceGeneral() {
        return reporteService.obtenerBalanceGeneral();
    }

    /** Estado de Resultados dinamico: Utilidad = Ingresos - Costos y Gastos. */
    @GetMapping("/api/reportes/estado-resultados")
    public EstadoResultadosDTO estadoResultados() {
        return reporteService.obtenerEstadoResultados();
    }
}
