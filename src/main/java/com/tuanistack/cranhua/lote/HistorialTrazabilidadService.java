package com.tuanistack.cranhua.lote;

import com.tuanistack.cranhua.cafeteria.CafeteriaRepository;
import com.tuanistack.cranhua.cafeteria.ItemMenuRepository;
import com.tuanistack.cranhua.lote.dto.HistorialTrazabilidad;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class HistorialTrazabilidadService {

  private final LoteRepository loteRepository;
  private final CafeteriaRepository cafeteriaRepository;
  private final ItemMenuRepository itemMenuRepository;

  public HistorialTrazabilidadService(
      LoteRepository loteRepository,
      CafeteriaRepository cafeteriaRepository,
      ItemMenuRepository itemMenuRepository) {
    this.loteRepository = loteRepository;
    this.cafeteriaRepository = cafeteriaRepository;
    this.itemMenuRepository = itemMenuRepository;
  }

  public HistorialTrazabilidad deFinca(UUID fincaId) {
    return HistorialTrazabilidad.de(loteRepository.codigoActivoDeFinca(fincaId));
  }

  public HistorialTrazabilidad deTostador(UUID cuentaId) {
    return HistorialTrazabilidad.de(loteRepository.codigoActivoDelTostador(cuentaId));
  }

  public HistorialTrazabilidad deCafeteria(UUID cafeteriaId) {
    return HistorialTrazabilidad.de(itemMenuRepository.codigoDelMenu(cafeteriaId));
  }

  public HistorialTrazabilidad deCuenta(UUID cuentaId) {
    return HistorialTrazabilidad.de(
        loteRepository
            .codigoActivoDeFincasDelDueno(cuentaId)
            .or(() -> loteRepository.codigoActivoDelTostador(cuentaId))
            .or(() ->
                cafeteriaRepository
                    .findByCuentaIdAndEliminadaFalse(cuentaId)
                    .flatMap(cafeteria -> itemMenuRepository.codigoDelMenu(cafeteria.getId()))));
  }
}
