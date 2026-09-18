package com.modulocontable.repository;

import com.modulocontable.model.Asiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AsientoRepository extends JpaRepository<Asiento, Long> {

    Optional<Asiento> findByNumeroAsiento(Integer numeroAsiento);

    /** Util para calcular el siguiente numero de asiento correlativo. */
    Optional<Asiento> findTopByOrderByNumeroAsientoDesc();

    /**
     * Trae un asiento junto con sus detalles y la cuenta de cada detalle en una sola consulta,
     * evitando LazyInitializationException (open-in-view esta desactivado) y N+1 queries.
     */
    @Query("SELECT a FROM Asiento a LEFT JOIN FETCH a.detalles d LEFT JOIN FETCH d.cuenta WHERE a.id = :id")
    Optional<Asiento> findByIdConDetalles(@Param("id") Long id);

    @Query("SELECT DISTINCT a FROM Asiento a LEFT JOIN FETCH a.detalles d LEFT JOIN FETCH d.cuenta ORDER BY a.numeroAsiento")
    List<Asiento> findAllConDetalles();
}
