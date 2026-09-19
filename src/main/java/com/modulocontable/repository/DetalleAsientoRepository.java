package com.modulocontable.repository;

import com.modulocontable.model.DetalleAsiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DetalleAsientoRepository extends JpaRepository<DetalleAsiento, Long> {

    List<DetalleAsiento> findByAsientoId(Long asientoId);

    List<DetalleAsiento> findByCuentaId(Long cuentaId);

    /** Movimientos de una cuenta en orden cronologico, con el asiento ya cargado (Libro Mayor). */
    @Query("SELECT d FROM DetalleAsiento d JOIN FETCH d.asiento a WHERE d.cuenta.id = :cuentaId " +
            "ORDER BY a.fecha ASC, a.numeroAsiento ASC")
    List<DetalleAsiento> findByCuentaIdConAsientoOrdenado(@Param("cuentaId") Long cuentaId);
}
