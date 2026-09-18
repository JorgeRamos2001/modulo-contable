package com.modulocontable.repository;

import com.modulocontable.model.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    Optional<Cuenta> findByCodigo(String codigo);

    List<Cuenta> findByTipoCuenta(String tipoCuenta);

    List<Cuenta> findByCuentaPadreId(Long cuentaPadreId);

    List<Cuenta> findByNivelOrderByCodigoAsc(Integer nivel);

    List<Cuenta> findAllByOrderByCodigoAsc();
}
