package com.tuanistack.cranhua.lote;

import com.tuanistack.cranhua.lote.dto.FichaCodigoResponse;
import com.tuanistack.cranhua.lote.dto.LotesCompradosResponse;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lotes")
public class LoteController {

  private final LoteService loteService;

  public LoteController(LoteService loteService) {
    this.loteService = loteService;
  }

  @GetMapping
  @PreAuthorize("hasRole('TOSTADOR')")
  public LotesCompradosResponse listar(Authentication authentication) {
    return loteService.listar(UUID.fromString(authentication.getName()));
  }

  @PostMapping("/{id}/codigo")
  @PreAuthorize("hasRole('TOSTADOR')")
  public FichaCodigoResponse activarCodigo(@PathVariable UUID id, Authentication authentication) {
    return loteService.activarCodigo(id, UUID.fromString(authentication.getName()));
  }
}
