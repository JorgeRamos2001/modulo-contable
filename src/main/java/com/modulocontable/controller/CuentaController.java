package com.modulocontable.controller;

import com.modulocontable.dto.CuentaRequestDTO;
import com.modulocontable.dto.CuentaResponseDTO;
import com.modulocontable.model.Cuenta;
import com.modulocontable.repository.CuentaRepository;
import com.modulocontable.service.CuentaService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cuentas")
@RequiredArgsConstructor
public class CuentaController {

    private final CuentaRepository cuentaRepository;
    private final CuentaService cuentaService;

    /** Catalogo completo, ordenado por codigo. */
    @GetMapping
    public List<CuentaResponseDTO> listar() {
        return cuentaRepository.findAllByOrderByCodigoAsc().stream()
                .map(CuentaResponseDTO::from)
                .toList();
    }

    @GetMapping("/{id}")
    public CuentaResponseDTO obtener(@PathVariable Long id) {
        return cuentaRepository.findById(id)
                .map(CuentaResponseDTO::from)
                .orElseThrow(() -> new EntityNotFoundException("Cuenta no encontrada: id=" + id));
    }

    @PostMapping
    public ResponseEntity<CuentaResponseDTO> crear(@Valid @RequestBody CuentaRequestDTO request) {
        Cuenta creada = cuentaService.crearCuenta(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CuentaResponseDTO.from(creada));
    }
}
