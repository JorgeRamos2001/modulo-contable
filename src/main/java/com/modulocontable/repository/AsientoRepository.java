package com.modulocontable.repository;

import com.modulocontable.model.Asiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AsientoRepository extends JpaRepository<Asiento, Long> {

    Optional<Asiento> findByNumeroAsiento(Integer numeroAsiento);

    /** Util para calcular el siguiente numero de asiento correlativo. */
    Optional<Asiento> findTopByOrderByNumeroAsientoDesc();
}
