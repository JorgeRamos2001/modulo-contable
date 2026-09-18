package com.modulocontable.repository;

import com.modulocontable.model.DetalleAsiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleAsientoRepository extends JpaRepository<DetalleAsiento, Long> {

    List<DetalleAsiento> findByAsientoId(Long asientoId);

    List<DetalleAsiento> findByCuentaId(Long cuentaId);
}
