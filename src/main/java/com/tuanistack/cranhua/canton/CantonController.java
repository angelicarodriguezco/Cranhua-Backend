package com.tuanistack.cranhua.canton;

import com.tuanistack.cranhua.canton.dto.CantonResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/publico/cantones")
public class CantonController {

  private final CantonService cantonService;

  public CantonController(CantonService cantonService) {
    this.cantonService = cantonService;
  }

  @GetMapping
  public List<CantonResponse> listar(@RequestParam(required = false) String provincia) {
    return cantonService.listar(provincia);
  }
}
