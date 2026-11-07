package com.tuanistack.cranhua.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.tuanistack.cranhua.auth.CorreoEnUsoException;
import com.tuanistack.cranhua.auth.CredencialesInvalidasException;
import com.tuanistack.cranhua.auth.CuentaGoogleNoRegistradaException;
import com.tuanistack.cranhua.auth.GoogleNoVerificadoException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorErrores {

  private static final String DATOS_INVALIDOS = "Revisá los datos ingresados.";

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record RespuestaError(String mensaje, Map<String, String> errores) {}

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public RespuestaError datosInvalidos(MethodArgumentNotValidException e) {
    Map<String, String> errores = new LinkedHashMap<>();
    e.getBindingResult()
        .getFieldErrors()
        .forEach(error -> errores.putIfAbsent(error.getField(), error.getDefaultMessage()));
    return new RespuestaError(DATOS_INVALIDOS, errores);
  }

  // Un tipo de dato equivocado en el JSON (por ejemplo roles como texto) también va por campo
  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public RespuestaError jsonInvalido(HttpMessageNotReadableException e) {
    if (e.getCause() instanceof MismatchedInputException error && !error.getPath().isEmpty()) {
      JsonMappingException.Reference campo = error.getPath().get(0);
      return new RespuestaError(DATOS_INVALIDOS, Map.of(campo.getFieldName(), "Valor inválido."));
    }
    return new RespuestaError(DATOS_INVALIDOS, null);
  }

  @ExceptionHandler(CorreoEnUsoException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public RespuestaError correoEnUso(CorreoEnUsoException e) {
    return new RespuestaError(e.getMessage(), null);
  }

  @ExceptionHandler({
    CredencialesInvalidasException.class,
    GoogleNoVerificadoException.class,
    CuentaGoogleNoRegistradaException.class
  })
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public RespuestaError noAutorizado(RuntimeException e) {
    return new RespuestaError(e.getMessage(), null);
  }
}