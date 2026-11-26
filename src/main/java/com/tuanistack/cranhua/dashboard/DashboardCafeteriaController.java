package com.tuanistack.cranhua.dashboard;

import com.tuanistack.cranhua.dashboard.dto.DashboardCafeteriaResponse;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cafeterias")
public class DashboardCafeteriaController {

  private final DashboardCafeteriaService dashboardService;

  public DashboardCafeteriaController(DashboardCafeteriaService dashboardService) {
    this.dashboardService = dashboardService;
  }

  @GetMapping("/{id}/dashboard")
  @PreAuthorize("hasRole('CAFETERIA')")
  public DashboardCafeteriaResponse dashboard(
      @PathVariable UUID id, Authentication authentication) {
    return dashboardService.resumen(id, UUID.fromString(authentication.getName()));
  }
}
