package com.tuanistack.cranhua.publico;

import com.tuanistack.cranhua.publico.dto.TrazabilidadPublicaResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/publico/trazabilidad")
public class TrazabilidadPublicaController {

  private final TrazabilidadPublicaService trazabilidadPublicaService;

  public TrazabilidadPublicaController(TrazabilidadPublicaService trazabilidadPublicaService) {
    this.trazabilidadPublicaService = trazabilidadPublicaService;
  }

  @GetMapping("/{codigo}")
  public TrazabilidadPublicaResponse porCodigo(@PathVariable String codigo) {
    return trazabilidadPublicaService.porCodigo(codigo);
  }
}
