package com.modulocontable.dto;

import java.math.BigDecimal;

/**
 * Estado de Resultados en formato cascada, como se enseña en Contabilidad 1:
 * cada bloque calcula una utilidad parcial que alimenta al siguiente.
 */
public record EstadoResultadosDTO(
        BigDecimal ventas,
        BigDecimal devolucionesRebajasVentas,
        BigDecimal ventasNetas,

        BigDecimal costoVentas,
        BigDecimal utilidadBruta,

        BigDecimal gastosVenta,
        BigDecimal gastosAdministracion,
        BigDecimal totalGastosOperacion,
        BigDecimal utilidadOperacional,

        BigDecimal productosFinancieros,
        BigDecimal gastosFinancieros,
        BigDecimal utilidadFinanciera,

        BigDecimal otrosProductos,
        BigDecimal otrosGastos,
        BigDecimal utilidadOtrosProductosYGastos,

        /** utilidadFinanciera + utilidadOtrosProductosYGastos */
        BigDecimal utilidadAjenaActividad,

        /** utilidadOperacional + utilidadAjenaActividad */
        BigDecimal utilidadAntesImpuestos
) {
}