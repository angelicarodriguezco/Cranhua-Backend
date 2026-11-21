package com.tuanistack.cranhua.dashboard;

import com.tuanistack.cranhua.dashboard.dto.DashboardTostadorResponse;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tostador")
public class DashboardTostadorController {

  private final DashboardTostadorService dashboardService;

  public DashboardTostadorController(DashboardTostadorService dashboardService) {
    this.dashboardService = dashboardService;
  }

  @GetMapping("/dashboard")
  @PreAuthorize("hasRole('TOSTADOR')")
  public DashboardTostadorResponse dashboard(Authentication authentication) {
    return dashboardService.resumen(UUID.fromString(authentication.getName()));
  }
}
