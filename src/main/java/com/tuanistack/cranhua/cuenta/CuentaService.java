package com.tuanistack.cranhua.cuenta;

import com.tuanistack.cranhua.cuenta.dto.ActualizarCuentaRequest;
import com.tuanistack.cranhua.cuenta.dto.CuentaResponse;
import com.tuanistack.cranhua.cuenta.dto.RedSocialRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CuentaService {

  private final CuentaRepository cuentaRepository;

  public CuentaService(CuentaRepository cuentaRepository) {
    this.cuentaRepository = cuentaRepository;
  }

  public CuentaResponse obtener(UUID id) {
    return CuentaResponse.de(cargarActiva(id));
  }

  public CuentaResponse actualizar(UUID id, ActualizarCuentaRequest request) {
    Cuenta cuenta = cargarActiva(id);

    String nombreCompleto =
        request.nombreCompleto() != null
            ? request.nombreCompleto().strip()
            : cuenta.getNombreCompleto();
    String telefono =
        request.telefono() != null ? enBlancoANull(request.telefono()) : cuenta.getTelefono();
    String fotoUrl =
        request.fotoUrl() != null ? enBlancoANull(request.fotoUrl()) : cuenta.getFotoUrl();
    List<RedSocial> redes =
        request.redes() != null ? aRedes(request.redes()) : List.copyOf(cuenta.getRedes());

    cuenta.actualizarPerfil(nombreCompleto, telefono, fotoUrl, redes);
    return CuentaResponse.de(cuentaRepository.save(cuenta));
  }

  public void eliminar(UUID id) {
    Cuenta cuenta = cargarActiva(id);
    cuenta.eliminar();
    cuentaRepository.save(cuenta);
  }

  private Cuenta cargarActiva(UUID id) {
    return cuentaRepository
        .findById(id)
        .filter(cuenta -> !cuenta.isEliminada())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  }

  private static List<RedSocial> aRedes(List<RedSocialRequest> redes) {
    return redes.stream().map(red -> new RedSocial(red.tipo(), red.valor().strip())).toList();
  }

  private static String enBlancoANull(String valor) {
    return valor.isBlank() ? null : valor.strip();
  }
}
