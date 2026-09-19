package com.modulocontable.controller;

import com.modulocontable.dto.BalanceGeneralDTO;
import com.modulocontable.dto.EstadoResultadosDTO;
import com.modulocontable.dto.LibroMayorResponseDTO;
import com.modulocontable.service.ReporteExportService;
import com.modulocontable.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReporteController {

    private static final MediaType EXCEL_MEDIA_TYPE =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReporteService reporteService;
    private final ReporteExportService reporteExportService;

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

    @GetMapping("/api/reportes/balance-general/pdf")
    public ResponseEntity<byte[]> balanceGeneralPdf() {
        byte[] pdf = reporteExportService.balanceGeneralPdf(reporteService.obtenerBalanceGeneral());
        return archivoDescargable(pdf, MediaType.APPLICATION_PDF, "balance-general.pdf");
    }

    @GetMapping("/api/reportes/balance-general/excel")
    public ResponseEntity<byte[]> balanceGeneralExcel() {
        byte[] excel = reporteExportService.balanceGeneralExcel(reporteService.obtenerBalanceGeneral());
        return archivoDescargable(excel, EXCEL_MEDIA_TYPE, "balance-general.xlsx");
    }

    @GetMapping("/api/reportes/estado-resultados/pdf")
    public ResponseEntity<byte[]> estadoResultadosPdf() {
        byte[] pdf = reporteExportService.estadoResultadosPdf(reporteService.obtenerEstadoResultados());
        return archivoDescargable(pdf, MediaType.APPLICATION_PDF, "estado-resultados.pdf");
    }

    @GetMapping("/api/reportes/estado-resultados/excel")
    public ResponseEntity<byte[]> estadoResultadosExcel() {
        byte[] excel = reporteExportService.estadoResultadosExcel(reporteService.obtenerEstadoResultados());
        return archivoDescargable(excel, EXCEL_MEDIA_TYPE, "estado-resultados.xlsx");
    }

    private ResponseEntity<byte[]> archivoDescargable(byte[] contenido, MediaType tipo, String nombreArchivo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(nombreArchivo).build());
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(tipo)
                .body(contenido);
    }
}