package com.modulocontable.exception;

import java.math.BigDecimal;

/**
 * Se lanza cuando la suma del Debe no coincide con la suma del Haber
 * de un asiento (viola la regla de Partida Doble).
 */
public class PartidaDobleException extends RuntimeException {

    public PartidaDobleException(BigDecimal totalDebe, BigDecimal totalHaber) {
        super("La partida no cuadra: Debe = %s, Haber = %s (diferencia = %s)".formatted(
                totalDebe, totalHaber, totalDebe.subtract(totalHaber).abs()));
    }
}
