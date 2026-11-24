package com.tuanistack.cranhua.publico;

import com.tuanistack.cranhua.publico.dto.MenuCafeteriaPublicoResponse;
import com.tuanistack.cranhua.publico.dto.PerfilCafeteriaPublicoResponse;
import com.tuanistack.cranhua.publico.dto.PerfilFincaPublicoResponse;
import com.tuanistack.cranhua.publico.dto.PerfilTostadorPublicoResponse;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/publico")
public class PerfilPublicoController {

  private final PerfilPublicoService perfilPublicoService;

  public PerfilPublicoController(PerfilPublicoService perfilPublicoService) {
    this.perfilPublicoService = perfilPublicoService;
  }

  @GetMapping("/fincas/{id}")
  public PerfilFincaPublicoResponse finca(@PathVariable UUID id) {
    return perfilPublicoService.finca(id);
  }

  @GetMapping("/cafeterias/{id}")
  public PerfilCafeteriaPublicoResponse cafeteria(@PathVariable UUID id) {
    return perfilPublicoService.cafeteria(id);
  }

  @GetMapping("/cafeterias/{id}/menu")
  public MenuCafeteriaPublicoResponse menu(@PathVariable UUID id) {
    return perfilPublicoService.menu(id);
  }

  @GetMapping("/tostadores/{id}")
  public PerfilTostadorPublicoResponse tostador(@PathVariable UUID id) {
    return perfilPublicoService.tostador(id);
  }
}
