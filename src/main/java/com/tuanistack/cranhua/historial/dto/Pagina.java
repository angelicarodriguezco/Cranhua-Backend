package com.tuanistack.cranhua.historial.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

public record Pagina<T>(
    List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

  public static <E, T> Pagina<T> de(Page<E> page, Function<E, T> mapear) {
    return new Pagina<>(
        page.getContent().stream().map(mapear).toList(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
