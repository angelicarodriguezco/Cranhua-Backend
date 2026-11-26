package com.tuanistack.cranhua.pedido;

import com.tuanistack.cranhua.pedido.dto.ActualizarPedidoRequest;
import com.tuanistack.cranhua.pedido.dto.CrearPedidoRequest;
import com.tuanistack.cranhua.pedido.dto.HistorialPedidosResponse;
import com.tuanistack.cranhua.pedido.dto.PedidoRecienteResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

  private final PedidoService pedidoService;

  public PedidoController(PedidoService pedidoService) {
    this.pedidoService = pedidoService;
  }

  @PostMapping
  @PreAuthorize("hasRole('CAFETERIA')")
  public ResponseEntity<PedidoRecienteResponse> crear(
      @Valid @RequestBody CrearPedidoRequest request, Authentication authentication) {
    PedidoRecienteResponse pedido =
        pedidoService.crear(request, UUID.fromString(authentication.getName()));
    return ResponseEntity.created(URI.create("/api/pedidos/" + pedido.id())).body(pedido);
  }

  @GetMapping
  @PreAuthorize("hasRole('CAFETERIA')")
  public HistorialPedidosResponse historial(Authentication authentication) {
    return pedidoService.historial(UUID.fromString(authentication.getName()));
  }

  @PatchMapping("/{id}")
  @PreAuthorize("hasRole('CAFETERIA')")
  public PedidoRecienteResponse actualizar(
      @PathVariable UUID id,
      @Valid @RequestBody ActualizarPedidoRequest request,
      Authentication authentication) {
    return pedidoService.actualizar(id, request, UUID.fromString(authentication.getName()));
  }
}
