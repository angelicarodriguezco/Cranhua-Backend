package com.tuanistack.cranhua.dashboard;

import com.tuanistack.cranhua.dashboard.dto.DashboardCafetaleroResponse;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cafetalero")
public class DashboardCafetaleroController {

  private final DashboardCafetaleroService dashboardService;

  public DashboardCafetaleroController(DashboardCafetaleroService dashboardService) {
    this.dashboardService = dashboardService;
  }

  @GetMapping("/dashboard")
  @PreAuthorize("hasRole('CAFETALERO')")
  public DashboardCafetaleroResponse dashboard(Authentication authentication) {
    return dashboardService.resumen(UUID.fromString(authentication.getName()));
  }
}
