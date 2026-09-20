package com.modulocontable.service;

import com.modulocontable.dto.BalanceGeneralDTO;
import com.modulocontable.dto.CuentaSaldoDTO;
import com.modulocontable.dto.EstadoResultadosDTO;
import com.modulocontable.dto.SeccionDTO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Convierte los DTOs de reportes (ya calculados por ReporteService) a PDF y Excel.
 * No recalcula nada: solo formatea datos que ya vienen listos.
 */
@Service
public class ReporteExportService {

    private static final Font FUENTE_EMPRESA = new Font(Font.HELVETICA, 14, Font.BOLD);
    private static final Font FUENTE_TITULO = new Font(Font.HELVETICA, 12, Font.BOLD);
    private static final Font FUENTE_SECCION = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
    private static final Font FUENTE_CELDA = new Font(Font.HELVETICA, 9, Font.NORMAL);
    private static final Font FUENTE_SUBTOTAL = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Font FUENTE_TOTAL = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Color COLOR_SECCION = new Color(51, 51, 51);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ================= BALANCE GENERAL - PDF =================

    public byte[] balanceGeneralPdf(BalanceGeneralDTO dto) {
        Document document = new Document(PageSize.LETTER.rotate(), 30, 30, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph empresa = new Paragraph(dto.nombreEmpresa(), FUENTE_EMPRESA);
            empresa.setAlignment(Element.ALIGN_CENTER);
            document.add(empresa);

            Paragraph titulo = new Paragraph(
                    "BALANCE GENERAL AL " + dto.fechaCorte().format(FORMATO_FECHA), FUENTE_TITULO);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);
            document.add(new Paragraph(" "));

            PdfPTable columnas = new PdfPTable(new float[]{1f, 1f});
            columnas.setWidthPercentage(100);

            PdfPCell celdaActivo = new PdfPCell(construirTablaActivoPdf(dto));
            celdaActivo.setBorder(Rectangle.NO_BORDER);
            celdaActivo.setPadding(4);
            columnas.addCell(celdaActivo);

            PdfPCell celdaPasivo = new PdfPCell(construirTablaPasivoCapitalPdf(dto));
            celdaPasivo.setBorder(Rectangle.NO_BORDER);
            celdaPasivo.setPadding(4);
            columnas.addCell(celdaPasivo);

            document.add(columnas);
            document.add(new Paragraph(" "));

            Paragraph cuadre = new Paragraph(
                    "Activo = Pasivo + Capital Contable: %s".formatted(dto.cuadrado() ? "CUADRADO" : "NO CUADRA"),
                    FUENTE_TOTAL);
            document.add(cuadre);

            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));
            agregarPieFirmas(document);

        } catch (DocumentException e) {
            throw new RuntimeException("Error generando el PDF del Balance General", e);
        } finally {
            document.close();
        }

        return out.toByteArray();
    }

    private PdfPTable construirTablaActivoPdf(BalanceGeneralDTO dto) throws DocumentException {
        PdfPTable tabla = new PdfPTable(new float[]{6f, 3f});
        tabla.setWidthPercentage(100);

        agregarEncabezadoPrincipal(tabla, "ACTIVO");
        agregarBloquePdf(tabla, "ACTIVO CORRIENTE", dto.activoCorriente(), "Total Circulante");
        agregarBloquePdf(tabla, "ACTIVO NO CORRIENTE", dto.activoNoCorriente(), "Total activo no corriente");
        agregarTotalFinalPdf(tabla, "TOTAL ACTIVO", dto.totalActivo());

        return tabla;
    }

    private PdfPTable construirTablaPasivoCapitalPdf(BalanceGeneralDTO dto) throws DocumentException {
        PdfPTable tabla = new PdfPTable(new float[]{6f, 3f});
        tabla.setWidthPercentage(100);

        agregarEncabezadoPrincipal(tabla, "PASIVO");
        agregarBloquePdf(tabla, "PASIVO CORRIENTE", dto.pasivoCorriente(), "Total Pasivo Corriente");
        if (!dto.pasivoNoCorriente().cuentas().isEmpty()) {
            agregarBloquePdf(tabla, "PASIVO NO CORRIENTE", dto.pasivoNoCorriente(), "Total Pasivo No Corriente");
        }

        agregarEncabezadoPrincipal(tabla, "CAPITAL CONTABLE");
        for (CuentaSaldoDTO cuenta : dto.capitalContable().cuentas()) {
            tabla.addCell(celdaPdf(cuenta.nombre(), Element.ALIGN_LEFT, FUENTE_CELDA));
            tabla.addCell(celdaPdf(cuenta.saldo().toString(), Element.ALIGN_RIGHT, FUENTE_CELDA));
        }
        agregarFilaPdf(tabla, "Total contable", dto.capitalContable().total(), FUENTE_SUBTOTAL);

        agregarTotalFinalPdf(tabla, "TOTAL PASIVO Y CAPITAL CONTABLE", dto.totalPasivoYCapital());

        return tabla;
    }

    private void agregarEncabezadoPrincipal(PdfPTable tabla, String titulo) {
        PdfPCell celda = new PdfPCell(new Phrase(titulo, FUENTE_SECCION));
        celda.setColspan(2);
        celda.setBackgroundColor(COLOR_SECCION);
        celda.setPadding(4);
        tabla.addCell(celda);
    }

    private void agregarBloquePdf(PdfPTable tabla, String tituloBloque, SeccionDTO seccion, String tituloSubtotal)
            throws DocumentException {
        PdfPCell tituloCelda = new PdfPCell(new Phrase(tituloBloque, FUENTE_SUBTOTAL));
        tituloCelda.setColspan(2);
        tituloCelda.setPadding(3);
        tabla.addCell(tituloCelda);

        if (seccion.cuentas().isEmpty()) {
            PdfPCell vacio = new PdfPCell(new Phrase("(sin movimientos)", FUENTE_CELDA));
            vacio.setColspan(2);
            vacio.setPadding(3);
            tabla.addCell(vacio);
        } else {
            for (CuentaSaldoDTO cuenta : seccion.cuentas()) {
                tabla.addCell(celdaPdf(cuenta.nombre(), Element.ALIGN_LEFT, FUENTE_CELDA));
                tabla.addCell(celdaPdf(cuenta.saldo().toString(), Element.ALIGN_RIGHT, FUENTE_CELDA));
            }
        }

        agregarFilaPdf(tabla, tituloSubtotal, seccion.total(), FUENTE_SUBTOTAL);
    }

    private void agregarTotalFinalPdf(PdfPTable tabla, String titulo, BigDecimal valor) {
        PdfPCell label = new PdfPCell(new Phrase(titulo, FUENTE_TOTAL));
        label.setPadding(4);
        label.setBorderWidthTop(1.5f);
        tabla.addCell(label);

        PdfPCell valorCelda = new PdfPCell(new Phrase(valor.toString(), FUENTE_TOTAL));
        valorCelda.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valorCelda.setPadding(4);
        valorCelda.setBorderWidthTop(1.5f);
        tabla.addCell(valorCelda);
    }

    private void agregarFilaPdf(PdfPTable tabla, String etiqueta, BigDecimal valor, Font fuente) {
        PdfPCell label = new PdfPCell(new Phrase(etiqueta, fuente));
        label.setPadding(3);
        tabla.addCell(label);

        PdfPCell valorCelda = new PdfPCell(new Phrase(valor.toString(), fuente));
        valorCelda.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valorCelda.setPadding(3);
        tabla.addCell(valorCelda);
    }

    private PdfPCell celdaPdf(String texto, int alineacion, Font fuente) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
        celda.setHorizontalAlignment(alineacion);
        celda.setPadding(3);
        return celda;
    }

    private void agregarPieFirmas(Document document) throws DocumentException {
        PdfPTable firmas = new PdfPTable(new float[]{1f, 1f});
        firmas.setWidthPercentage(60);
        firmas.addCell(celdaFirma("Representante legal"));
        firmas.addCell(celdaFirma("Contador"));
        document.add(firmas);
    }

    private PdfPCell celdaFirma(String etiqueta) {
        PdfPCell celda = new PdfPCell();
        celda.setBorder(Rectangle.NO_BORDER);
        celda.addElement(new Paragraph("_______________________", FUENTE_CELDA));
        celda.addElement(new Paragraph(etiqueta, FUENTE_CELDA));
        celda.setPadding(4);
        return celda;
    }

    // ================= ESTADO DE RESULTADOS - PDF (cascada) =================

    public byte[] estadoResultadosPdf(EstadoResultadosDTO dto) {
        Document document = new Document(PageSize.LETTER, 40, 40, 50, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("Estado de Resultados", FUENTE_TITULO));
            document.add(new Paragraph(" "));

            PdfPTable tabla = new PdfPTable(new float[]{7f, 3f});
            tabla.setWidthPercentage(100);

            filaCascada(tabla, "Ventas", dto.ventas(), FUENTE_CELDA, 0);
            filaCascada(tabla, "(-) Devoluciones y Rebajas sobre Ventas", dto.devolucionesRebajasVentas(), FUENTE_CELDA, 0);
            filaCascada(tabla, "(=) VENTAS NETAS", dto.ventasNetas(), FUENTE_SUBTOTAL, 1);

            filaCascada(tabla, "(-) Costo de Ventas", dto.costoVentas(), FUENTE_CELDA, 0);
            filaCascada(tabla, "(=) UTILIDAD BRUTA", dto.utilidadBruta(), FUENTE_SUBTOTAL, 1);

            filaCascada(tabla, "GASTOS DE OPERACION", null, FUENTE_SUBTOTAL, 0);
            filaCascada(tabla, "Gastos de Venta", dto.gastosVenta(), FUENTE_CELDA, 1);
            filaCascada(tabla, "(+) Gastos de Administracion", dto.gastosAdministracion(), FUENTE_CELDA, 1);
            filaCascada(tabla, "(=) Total Gastos de Operacion", dto.totalGastosOperacion(), FUENTE_SUBTOTAL, 1);
            filaCascada(tabla, "(=) UTILIDAD OPERACIONAL", dto.utilidadOperacional(), FUENTE_TOTAL, 1);

            filaCascada(tabla, "Productos Financieros", dto.productosFinancieros(), FUENTE_CELDA, 0);
            filaCascada(tabla, "(-) Gastos Financieros", dto.gastosFinancieros(), FUENTE_CELDA, 0);
            filaCascada(tabla, "(=) Utilidad Financiera", dto.utilidadFinanciera(), FUENTE_SUBTOTAL, 1);

            filaCascada(tabla, "Otros Productos", dto.otrosProductos(), FUENTE_CELDA, 0);
            filaCascada(tabla, "(-) Otros Gastos", dto.otrosGastos(), FUENTE_CELDA, 0);
            filaCascada(tabla, "(=) Utilidad Ajena a la Actividad Propia", dto.utilidadOtrosProductosYGastos(), FUENTE_SUBTOTAL, 1);

            filaCascada(tabla, "(=) UTILIDAD AJENA A LA ACTIVIDAD", dto.utilidadAjenaActividad(), FUENTE_SUBTOTAL, 1);
            filaCascada(tabla, "(=) UTILIDAD ANTES DE IMPUESTOS", dto.utilidadAntesImpuestos(), FUENTE_TOTAL, 1);

            document.add(tabla);

        } catch (DocumentException e) {
            throw new RuntimeException("Error generando el PDF del Estado de Resultados", e);
        } finally {
            document.close();
        }

        return out.toByteArray();
    }

    /** nivelIndentacion: 0 = sin indentar, 1 = una sangria (subtotales de bloque). */
    private void filaCascada(PdfPTable tabla, String etiqueta, BigDecimal valor, Font fuente, int nivelIndentacion) {
        String prefijo = "    ".repeat(nivelIndentacion);
        PdfPCell label = new PdfPCell(new Phrase(prefijo + etiqueta, fuente));
        label.setPadding(3);
        label.setBorder(Rectangle.NO_BORDER);
        tabla.addCell(label);

        PdfPCell valorCelda;
        if (valor == null) {
            valorCelda = new PdfPCell(new Phrase(""));
        } else {
            valorCelda = new PdfPCell(new Phrase(valor.toString(), fuente));
        }
        valorCelda.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valorCelda.setPadding(3);
        valorCelda.setBorder(Rectangle.NO_BORDER);
        tabla.addCell(valorCelda);
    }

    // ================= EXCEL =================

    public byte[] balanceGeneralExcel(BalanceGeneralDTO dto) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Balance General");
            CellStyle estiloEncabezado = estiloEncabezado(workbook);
            CellStyle estiloTotal = estiloTotal(workbook);

            Row filaEmpresa = sheet.createRow(0);
            filaEmpresa.createCell(0).setCellValue(dto.nombreEmpresa());
            Row filaTitulo = sheet.createRow(1);
            filaTitulo.createCell(0).setCellValue("BALANCE GENERAL AL " + dto.fechaCorte().format(FORMATO_FECHA));

            // Columna izquierda (A-B): Activo. Columna derecha (D-E): Pasivo + Capital.
            int filaActivo = 3;
            Row tituloActivo = sheet.createRow(filaActivo);
            tituloActivo.createCell(0).setCellValue("ACTIVO");
            tituloActivo.getCell(0).setCellStyle(estiloEncabezado);
            filaActivo++;
            filaActivo = agregarBloqueExcel(sheet, filaActivo, 0, "ACTIVO CORRIENTE", dto.activoCorriente(),
                    "Total Circulante", estiloTotal);
            filaActivo = agregarBloqueExcel(sheet, filaActivo, 0, "ACTIVO NO CORRIENTE", dto.activoNoCorriente(),
                    "Total activo no corriente", estiloTotal);
            filaActivo = agregarTotalFinalExcel(sheet, filaActivo, 0, "TOTAL ACTIVO", dto.totalActivo(), estiloTotal);

            int filaPasivo = 3;
            Row tituloPasivo = sheet.createRow(filaPasivo);
            tituloPasivo.createCell(3).setCellValue("PASIVO");
            tituloPasivo.getCell(3).setCellStyle(estiloEncabezado);
            filaPasivo++;
            filaPasivo = agregarBloqueExcel(sheet, filaPasivo, 3, "PASIVO CORRIENTE", dto.pasivoCorriente(),
                    "Total Pasivo Corriente", estiloTotal);
            if (!dto.pasivoNoCorriente().cuentas().isEmpty()) {
                filaPasivo = agregarBloqueExcel(sheet, filaPasivo, 3, "PASIVO NO CORRIENTE", dto.pasivoNoCorriente(),
                        "Total Pasivo No Corriente", estiloTotal);
            }

            Row tituloCapital = sheet.createRow(filaPasivo++);
            Cell capitalCell = tituloCapital.createCell(3);
            capitalCell.setCellValue("CAPITAL CONTABLE");
            capitalCell.setCellStyle(estiloEncabezado);
            for (CuentaSaldoDTO cuenta : dto.capitalContable().cuentas()) {
                Row fila = sheet.createRow(filaPasivo++);
                fila.createCell(3).setCellValue(cuenta.nombre());
                fila.createCell(5).setCellValue(cuenta.saldo().doubleValue());
            }
            Row filaTotalCapital = sheet.createRow(filaPasivo++);
            Cell etiquetaTotalCapital = filaTotalCapital.createCell(3);
            etiquetaTotalCapital.setCellValue("Total contable");
            etiquetaTotalCapital.setCellStyle(estiloTotal);
            Cell valorTotalCapital = filaTotalCapital.createCell(5);
            valorTotalCapital.setCellValue(dto.capitalContable().total().doubleValue());
            valorTotalCapital.setCellStyle(estiloTotal);

            filaPasivo = agregarTotalFinalExcel(sheet, filaPasivo, 3, "TOTAL PASIVO Y CAPITAL CONTABLE",
                    dto.totalPasivoYCapital(), estiloTotal);

            int filaCuadre = Math.max(filaActivo, filaPasivo) + 1;
            Row cuadreRow = sheet.createRow(filaCuadre);
            cuadreRow.createCell(0).setCellValue(
                    "Activo = Pasivo + Capital Contable: " + (dto.cuadrado() ? "CUADRADO" : "NO CUADRA"));

            for (int col = 0; col <= 5; col++) {
                sheet.autoSizeColumn(col);
            }

            return workbookABytes(workbook);
        } catch (IOException e) {
            throw new RuntimeException("Error generando el Excel del Balance General", e);
        }
    }

    private int agregarBloqueExcel(Sheet sheet, int filaInicio, int colBase, String titulo, SeccionDTO seccion,
                                   String tituloSubtotal, CellStyle estiloTotal) {
        int fila = filaInicio;

        Row filaTitulo = sheet.createRow(fila++);
        filaTitulo.createCell(colBase).setCellValue(titulo);

        for (CuentaSaldoDTO cuenta : seccion.cuentas()) {
            Row filaCuenta = sheet.createRow(fila++);
            filaCuenta.createCell(colBase).setCellValue(cuenta.nombre());
            filaCuenta.createCell(colBase + 2).setCellValue(cuenta.saldo().doubleValue());
        }

        Row filaSubtotal = sheet.createRow(fila++);
        Cell labelSubtotal = filaSubtotal.createCell(colBase);
        labelSubtotal.setCellValue(tituloSubtotal);
        labelSubtotal.setCellStyle(estiloTotal);
        Cell valorSubtotal = filaSubtotal.createCell(colBase + 2);
        valorSubtotal.setCellValue(seccion.total().doubleValue());
        valorSubtotal.setCellStyle(estiloTotal);

        return fila;
    }

    private int agregarTotalFinalExcel(Sheet sheet, int fila, int colBase, String titulo, BigDecimal valor,
                                       CellStyle estiloTotal) {
        Row filaTotal = sheet.createRow(fila++);
        Cell label = filaTotal.createCell(colBase);
        label.setCellValue(titulo);
        label.setCellStyle(estiloTotal);
        Cell valorCell = filaTotal.createCell(colBase + 2);
        valorCell.setCellValue(valor.doubleValue());
        valorCell.setCellStyle(estiloTotal);
        return fila;
    }

    public byte[] estadoResultadosExcel(EstadoResultadosDTO dto) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Estado de Resultados");
            CellStyle estiloSubtotal = estiloTotal(workbook);
            CellStyle estiloTotal = estiloTotal(workbook);

            int fila = 0;
            fila = filaCascadaExcel(sheet, fila, "Ventas", dto.ventas(), null);
            fila = filaCascadaExcel(sheet, fila, "(-) Devoluciones y Rebajas sobre Ventas", dto.devolucionesRebajasVentas(), null);
            fila = filaCascadaExcel(sheet, fila, "(=) VENTAS NETAS", dto.ventasNetas(), estiloSubtotal);

            fila = filaCascadaExcel(sheet, fila, "(-) Costo de Ventas", dto.costoVentas(), null);
            fila = filaCascadaExcel(sheet, fila, "(=) UTILIDAD BRUTA", dto.utilidadBruta(), estiloSubtotal);

            fila = filaCascadaExcel(sheet, fila, "Gastos de Venta", dto.gastosVenta(), null);
            fila = filaCascadaExcel(sheet, fila, "(+) Gastos de Administracion", dto.gastosAdministracion(), null);
            fila = filaCascadaExcel(sheet, fila, "(=) Total Gastos de Operacion", dto.totalGastosOperacion(), estiloSubtotal);
            fila = filaCascadaExcel(sheet, fila, "(=) UTILIDAD OPERACIONAL", dto.utilidadOperacional(), estiloTotal);

            fila = filaCascadaExcel(sheet, fila, "Productos Financieros", dto.productosFinancieros(), null);
            fila = filaCascadaExcel(sheet, fila, "(-) Gastos Financieros", dto.gastosFinancieros(), null);
            fila = filaCascadaExcel(sheet, fila, "(=) Utilidad Financiera", dto.utilidadFinanciera(), estiloSubtotal);

            fila = filaCascadaExcel(sheet, fila, "Otros Productos", dto.otrosProductos(), null);
            fila = filaCascadaExcel(sheet, fila, "(-) Otros Gastos", dto.otrosGastos(), null);
            fila = filaCascadaExcel(sheet, fila, "(=) Utilidad Ajena a la Actividad Propia", dto.utilidadOtrosProductosYGastos(), estiloSubtotal);

            fila = filaCascadaExcel(sheet, fila, "(=) UTILIDAD AJENA A LA ACTIVIDAD", dto.utilidadAjenaActividad(), estiloSubtotal);
            filaCascadaExcel(sheet, fila, "(=) UTILIDAD ANTES DE IMPUESTOS", dto.utilidadAntesImpuestos(), estiloTotal);

            for (int col = 0; col < 2; col++) {
                sheet.autoSizeColumn(col);
            }

            return workbookABytes(workbook);
        } catch (IOException e) {
            throw new RuntimeException("Error generando el Excel del Estado de Resultados", e);
        }
    }

    private int filaCascadaExcel(Sheet sheet, int fila, String etiqueta, BigDecimal valor, CellStyle estilo) {
        Row row = sheet.createRow(fila);
        Cell label = row.createCell(0);
        label.setCellValue(etiqueta);
        Cell valorCell = row.createCell(1);
        valorCell.setCellValue(valor.doubleValue());
        if (estilo != null) {
            label.setCellStyle(estilo);
            valorCell.setCellStyle(estilo);
        }
        return fila + 1;
    }

    private CellStyle estiloEncabezado(Workbook workbook) {
        CellStyle estilo = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font fuente = workbook.createFont();
        fuente.setBold(true);
        fuente.setColor(IndexedColors.WHITE.getIndex());
        estilo.setFont(fuente);
        estilo.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return estilo;
    }

    private CellStyle estiloTotal(Workbook workbook) {
        CellStyle estilo = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font fuente = workbook.createFont();
        fuente.setBold(true);
        estilo.setFont(fuente);
        return estilo;
    }

    private byte[] workbookABytes(Workbook workbook) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }
}