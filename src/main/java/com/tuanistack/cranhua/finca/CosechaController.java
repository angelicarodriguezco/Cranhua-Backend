package com.tuanistack.cranhua.finca;

import com.tuanistack.cranhua.finca.dto.CosechaResponse;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cosechas")
public class CosechaController {

  private final CosechaService cosechaService;

  public CosechaController(CosechaService cosechaService) {
    this.cosechaService = cosechaService;
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('CAFETALERO')")
  public CosechaResponse obtener(@PathVariable UUID id, Authentication authentication) {
    return cosechaService.obtener(id, UUID.fromString(authentication.getName()));
  }
}
