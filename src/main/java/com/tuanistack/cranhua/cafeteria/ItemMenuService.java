package com.tuanistack.cranhua.cafeteria;

import com.tuanistack.cranhua.cafeteria.dto.ItemMenuResponse;
import com.tuanistack.cranhua.cafeteria.dto.PublicarItemMenuRequest;
import com.tuanistack.cranhua.lote.EstadoTrazabilidad;
import com.tuanistack.cranhua.lote.LoteRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ItemMenuService {

  private final ItemMenuRepository itemMenuRepository;
  private final CafeteriaService cafeteriaService;
  private final LoteRepository loteRepository;

  public ItemMenuService(
      ItemMenuRepository itemMenuRepository,
      CafeteriaService cafeteriaService,
      LoteRepository loteRepository) {
    this.itemMenuRepository = itemMenuRepository;
    this.cafeteriaService = cafeteriaService;
    this.loteRepository = loteRepository;
  }

  @Transactional
  public ItemMenuResponse publicar(
      UUID cafeteriaId, PublicarItemMenuRequest request, UUID cuentaId) {
    Cafeteria cafeteria = cafeteriaService.cafeteriaPropia(cafeteriaId, cuentaId);
    ItemMenu item =
        new ItemMenu(cafeteria, request.nombre(), request.categoria(), request.precio());

    if (request.codigoTrazabilidad() != null && !request.codigoTrazabilidad().isBlank()) {
      if (request.categoria() != CategoriaMenu.CAFE) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Solo los cafés llevan código de trazabilidad.");
      }
      String codigo = request.codigoTrazabilidad().strip().toUpperCase(Locale.ROOT);
      if (!loteRepository.existsByCodigoAndEstadoTrazabilidad(codigo, EstadoTrazabilidad.ACTIVO)) {
        throw new ResponseStatusException(
            HttpStatus.NOT_FOUND, "No hay un lote activo con el código %s.".formatted(codigo));
      }
      item.setCodigoTrazabilidad(codigo);
    }

    try {
      item = itemMenuRepository.saveAndFlush(item);
    } catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Ya publicaste un ítem con ese nombre y ese código.");
    }
    return ItemMenuResponse.de(item);
  }

  @Transactional(readOnly = true)
  public List<ItemMenuResponse> listar(UUID cafeteriaId, UUID cuentaId) {
    cafeteriaService.cafeteriaPropia(cafeteriaId, cuentaId);
    return itemMenuRepository
        .findByCafeteriaIdAndCafeteriaEliminadaFalseOrderByNombre(cafeteriaId)
        .stream()
        .map(ItemMenuResponse::de)
        .toList();
  }

  @Transactional
  public void quitar(UUID cafeteriaId, UUID itemId, UUID cuentaId) {
    cafeteriaService.cafeteriaPropia(cafeteriaId, cuentaId);
    ItemMenu item =
        itemMenuRepository
            .findById(itemId)
            .orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontramos ese ítem."));
    if (!item.getCafeteria().getId().equals(cafeteriaId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este ítem no es de tu menú.");
    }
    itemMenuRepository.delete(item);
  }
}
