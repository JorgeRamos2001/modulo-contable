package com.modulocontable.controller;

import com.modulocontable.dto.AsientoRequestDTO;
import com.modulocontable.dto.AsientoResponseDTO;
import com.modulocontable.model.Asiento;
import com.modulocontable.repository.AsientoRepository;
import com.modulocontable.service.AsientoService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asientos")
@RequiredArgsConstructor
public class AsientoController {

    private final AsientoService asientoService;
    private final AsientoRepository asientoRepository;

    /** Libro Diario: crea un asiento nuevo (valida Partida Doble y mayoriza en la misma operacion). */
    @PostMapping
    public ResponseEntity<AsientoResponseDTO> crear(@Valid @RequestBody AsientoRequestDTO request) {
        Asiento creado = asientoService.crearAsiento(request);
        // se vuelve a leer con JOIN FETCH para serializar los detalles sin problemas de lazy loading
        Asiento conDetalles = asientoRepository.findByIdConDetalles(creado.getId())
                .orElseThrow(() -> new EntityNotFoundException("Asiento no encontrado tras guardarlo: id=" + creado.getId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(AsientoResponseDTO.from(conDetalles));
    }

    /** Libro Diario: lista todos los asientos con sus detalles. */
    @GetMapping
    public List<AsientoResponseDTO> listar() {
        return asientoRepository.findAllConDetalles().stream()
                .map(AsientoResponseDTO::from)
                .toList();
    }

    /** Detalle de un asiento especifico. */
    @GetMapping("/{id}")
    public AsientoResponseDTO obtener(@PathVariable Long id) {
        return asientoRepository.findByIdConDetalles(id)
                .map(AsientoResponseDTO::from)
                .orElseThrow(() -> new EntityNotFoundException("Asiento no encontrado: id=" + id));
    }
}
