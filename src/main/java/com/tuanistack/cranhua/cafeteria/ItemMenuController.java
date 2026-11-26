package com.tuanistack.cranhua.cafeteria;

import com.tuanistack.cranhua.cafeteria.dto.ItemMenuResponse;
import com.tuanistack.cranhua.cafeteria.dto.PublicarItemMenuRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cafeterias/{id}/menu")
public class ItemMenuController {

  private final ItemMenuService itemMenuService;

  public ItemMenuController(ItemMenuService itemMenuService) {
    this.itemMenuService = itemMenuService;
  }

  @PostMapping
  @PreAuthorize("hasRole('CAFETERIA')")
  public ResponseEntity<ItemMenuResponse> publicar(
      @PathVariable UUID id,
      @Valid @RequestBody PublicarItemMenuRequest request,
      Authentication authentication) {
    ItemMenuResponse item = itemMenuService.publicar(id, request, idDe(authentication));
    return ResponseEntity.created(URI.create("/api/cafeterias/" + id + "/menu/" + item.id()))
        .body(item);
  }

  @GetMapping
  @PreAuthorize("hasRole('CAFETERIA')")
  public List<ItemMenuResponse> listar(@PathVariable UUID id, Authentication authentication) {
    return itemMenuService.listar(id, idDe(authentication));
  }

  @DeleteMapping("/{itemId}")
  @PreAuthorize("hasRole('CAFETERIA')")
  public ResponseEntity<Void> quitar(
      @PathVariable UUID id, @PathVariable UUID itemId, Authentication authentication) {
    itemMenuService.quitar(id, itemId, idDe(authentication));
    return ResponseEntity.noContent().build();
  }

  private static UUID idDe(Authentication authentication) {
    return UUID.fromString(authentication.getName());
  }
}
