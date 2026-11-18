package com.tuanistack.cranhua.mapa;

import com.tuanistack.cranhua.mapa.dto.PuntoMapaResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/publico/mapa")
public class MapaController {

  private final MapaService mapaService;

  public MapaController(MapaService mapaService) {
    this.mapaService = mapaService;
  }

  @GetMapping
  public List<PuntoMapaResponse> listar(@RequestParam(required = false) TipoPuntoMapa tipo) {
    return mapaService.listar(tipo);
  }
}
