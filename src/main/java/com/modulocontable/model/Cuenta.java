package com.modulocontable.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;

@Entity
@Table(name = "cuenta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nombre;

    /** 1=Clasificacion General, 2=Rubro, 3=Cuenta de Mayor, 4=Subcuenta */
    @Column(nullable = false)
    private Integer nivel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_padre_id")
    private Cuenta cuentaPadre;

    /** Primer digito del codigo: 1=Activo 2=Pasivo 3=Patrimonio 4=Costos/Gastos 5=Ingresos 6=Cierre 7=Orden */
    @Column(name = "tipo_cuenta", nullable = false, length = 1)
    private String tipoCuenta;

    @Column(name = "saldo_actual", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal saldoActual = BigDecimal.ZERO;

    /**
     * true si el tipo de cuenta tiene naturaleza deudora (Activo=1, Costos/Gastos=4).
     * Util para saber si un DEBE incrementa o disminuye el saldo.
     */
    @Transient
    public boolean esNaturalezaDeudora() {
        return "1".equals(tipoCuenta) || "4".equals(tipoCuenta);
    }
}
