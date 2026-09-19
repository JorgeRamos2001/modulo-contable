package com.modulocontable.service;

import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import com.modulocontable.dto.BalanceGeneralDTO;
import com.modulocontable.dto.CuentaSaldoDTO;
import com.modulocontable.dto.EstadoResultadosDTO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Convierte los DTOs de reportes (ya calculados por ReporteService) a PDF y Excel.
 * No recalcula nada: solo formatea datos que ya vienen listos.
 */
@Service
public class ReporteExportService {

    private static final Font FUENTE_TITULO = new Font(Font.HELVETICA, 16, Font.BOLD);
    private static final Font FUENTE_SECCION = new Font(Font.HELVETICA, 12, Font.BOLD);
    private static final Font FUENTE_ENCABEZADO = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
    private static final Font FUENTE_CELDA = new Font(Font.HELVETICA, 10, Font.NORMAL);
    private static final Font FUENTE_TOTAL = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final java.awt.Color COLOR_ENCABEZADO = new Color(51, 51, 51);

    // ---------- PDF ----------

    public byte[] balanceGeneralPdf(BalanceGeneralDTO dto) {
        Document document = new Document(PageSize.LETTER, 40, 40, 50, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("Balance General", FUENTE_TITULO));
            document.add(new Paragraph(" "));

            agregarSeccionPdf(document, "ACTIVO", dto.activo(), dto.totalActivo());
            agregarSeccionPdf(document, "PASIVO", dto.pasivo(), dto.totalPasivo());
            agregarSeccionPdf(document, "PATRIMONIO", dto.patrimonio(), dto.totalPatrimonio());

            document.add(new Paragraph(" "));
            Paragraph verificacion = new Paragraph(
                    "Activo = Pasivo + Patrimonio: %s".formatted(dto.cuadrado() ? "CUADRADO" : "NO CUADRA"),
                    FUENTE_TOTAL);
            document.add(verificacion);

        } catch (DocumentException e) {
            throw new RuntimeException("Error generando el PDF del Balance General", e);
        } finally {
            document.close();
        }

        return out.toByteArray();
    }

    public byte[] estadoResultadosPdf(EstadoResultadosDTO dto) {
        Document document = new Document(PageSize.LETTER, 40, 40, 50, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("Estado de Resultados", FUENTE_TITULO));
            document.add(new Paragraph(" "));

            agregarSeccionPdf(document, "INGRESOS", dto.ingresos(), dto.totalIngresos());
            agregarSeccionPdf(document, "COSTOS Y GASTOS", dto.costosYGastos(), dto.totalCostosYGastos());

            document.add(new Paragraph(" "));
            String etiqueta = dto.utilidad().compareTo(BigDecimal.ZERO) >= 0 ? "UTILIDAD DEL PERIODO" : "PERDIDA DEL PERIODO";
            document.add(new Paragraph("%s: %s".formatted(etiqueta, dto.utilidad()), FUENTE_TOTAL));

        } catch (DocumentException e) {
            throw new RuntimeException("Error generando el PDF del Estado de Resultados", e);
        } finally {
            document.close();
        }

        return out.toByteArray();
    }

    private void agregarSeccionPdf(Document document, String titulo, List<CuentaSaldoDTO> cuentas, BigDecimal total)
            throws DocumentException {
        document.add(new Paragraph(titulo, FUENTE_SECCION));

        PdfPTable tabla = new PdfPTable(new float[]{2f, 5f, 3f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(5);
        tabla.setSpacingAfter(10);

        for (String encabezado : new String[]{"Codigo", "Nombre", "Saldo"}) {
            PdfPCell celda = new PdfPCell(new Phrase(encabezado, FUENTE_ENCABEZADO));
            celda.setBackgroundColor(COLOR_ENCABEZADO);
            celda.setPadding(5);
            tabla.addCell(celda);
        }

        if (cuentas.isEmpty()) {
            PdfPCell vacio = new PdfPCell(new Phrase("(sin movimientos)", FUENTE_CELDA));
            vacio.setColspan(3);
            vacio.setPadding(5);
            tabla.addCell(vacio);
        } else {
            for (CuentaSaldoDTO cuenta : cuentas) {
                tabla.addCell(celda(cuenta.codigo(), Element.ALIGN_LEFT));
                tabla.addCell(celda(cuenta.nombre(), Element.ALIGN_LEFT));
                tabla.addCell(celda(cuenta.saldo().toString(), Element.ALIGN_RIGHT));
            }
        }

        PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL " + titulo, FUENTE_TOTAL));
        totalLabel.setColspan(2);
        totalLabel.setPadding(5);
        tabla.addCell(totalLabel);
        tabla.addCell(celda(total.toString(), Element.ALIGN_RIGHT, FUENTE_TOTAL));

        document.add(tabla);
    }

    private PdfPCell celda(String texto, int alineacion) {
        return celda(texto, alineacion, FUENTE_CELDA);
    }

    private PdfPCell celda(String texto, int alineacion, Font fuente) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
        celda.setHorizontalAlignment(alineacion);
        celda.setPadding(5);
        return celda;
    }

    // ---------- Excel ----------

    public byte[] balanceGeneralExcel(BalanceGeneralDTO dto) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Balance General");
            CellStyle estiloEncabezado = estiloEncabezado(workbook);
            CellStyle estiloTotal = estiloTotal(workbook);

            int fila = 0;
            fila = agregarSeccionExcel(sheet, fila, "ACTIVO", dto.activo(), dto.totalActivo(), estiloEncabezado, estiloTotal);
            fila++;
            fila = agregarSeccionExcel(sheet, fila, "PASIVO", dto.pasivo(), dto.totalPasivo(), estiloEncabezado, estiloTotal);
            fila++;
            agregarSeccionExcel(sheet, fila, "PATRIMONIO", dto.patrimonio(), dto.totalPatrimonio(), estiloEncabezado, estiloTotal);

            for (int col = 0; col < 3; col++) {
                sheet.autoSizeColumn(col);
            }

            return workbookABytes(workbook);
        } catch (IOException e) {
            throw new RuntimeException("Error generando el Excel del Balance General", e);
        }
    }

    public byte[] estadoResultadosExcel(EstadoResultadosDTO dto) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Estado de Resultados");
            CellStyle estiloEncabezado = estiloEncabezado(workbook);
            CellStyle estiloTotal = estiloTotal(workbook);

            int fila = 0;
            fila = agregarSeccionExcel(sheet, fila, "INGRESOS", dto.ingresos(), dto.totalIngresos(), estiloEncabezado, estiloTotal);
            fila++;
            fila = agregarSeccionExcel(sheet, fila, "COSTOS Y GASTOS", dto.costosYGastos(), dto.totalCostosYGastos(), estiloEncabezado, estiloTotal);

            fila++;
            Row filaUtilidad = sheet.createRow(fila);
            String etiqueta = dto.utilidad().compareTo(BigDecimal.ZERO) >= 0 ? "UTILIDAD DEL PERIODO" : "PERDIDA DEL PERIODO";
            Cell etiquetaCell = filaUtilidad.createCell(0);
            etiquetaCell.setCellValue(etiqueta);
            etiquetaCell.setCellStyle(estiloTotal);
            Cell valorCell = filaUtilidad.createCell(2);
            valorCell.setCellValue(dto.utilidad().doubleValue());
            valorCell.setCellStyle(estiloTotal);

            for (int col = 0; col < 3; col++) {
                sheet.autoSizeColumn(col);
            }

            return workbookABytes(workbook);
        } catch (IOException e) {
            throw new RuntimeException("Error generando el Excel del Estado de Resultados", e);
        }
    }

    private int agregarSeccionExcel(Sheet sheet, int filaInicio, String titulo, List<CuentaSaldoDTO> cuentas,
                                    BigDecimal total, CellStyle estiloEncabezado, CellStyle estiloTotal) {
        int fila = filaInicio;

        Row filaTitulo = sheet.createRow(fila++);
        filaTitulo.createCell(0).setCellValue(titulo);

        Row filaEncabezado = sheet.createRow(fila++);
        String[] encabezados = {"Codigo", "Nombre", "Saldo"};
        for (int i = 0; i < encabezados.length; i++) {
            Cell cell = filaEncabezado.createCell(i);
            cell.setCellValue(encabezados[i]);
            cell.setCellStyle(estiloEncabezado);
        }

        for (CuentaSaldoDTO cuenta : cuentas) {
            Row filaCuenta = sheet.createRow(fila++);
            filaCuenta.createCell(0).setCellValue(cuenta.codigo());
            filaCuenta.createCell(1).setCellValue(cuenta.nombre());
            filaCuenta.createCell(2).setCellValue(cuenta.saldo().doubleValue());
        }

        Row filaTotal = sheet.createRow(fila++);
        Cell labelTotal = filaTotal.createCell(0);
        labelTotal.setCellValue("TOTAL " + titulo);
        labelTotal.setCellStyle(estiloTotal);
        Cell valorTotal = filaTotal.createCell(2);
        valorTotal.setCellValue(total.doubleValue());
        valorTotal.setCellStyle(estiloTotal);

        return fila;
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