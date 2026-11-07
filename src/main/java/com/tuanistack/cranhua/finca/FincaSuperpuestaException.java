package com.tuanistack.cranhua.finca;

import com.tuanistack.cranhua.finca.dto.ConflictoFinca;
import java.util.List;

public class FincaSuperpuestaException extends RuntimeException {

  private final ConflictoFinca conflicto;

  public FincaSuperpuestaException(ConflictoFinca conflicto) {
    super(
        "El terreno se sobrepone con la finca \"%s\" de %s."
            .formatted(conflicto.nombre(), unirNombres(conflicto.duenos())));
    this.conflicto = conflicto;
  }

  private static String unirNombres(List<String> nombres) {
    if (nombres.size() <= 1) {
      return String.join("", nombres);
    }
    return String.join(", ", nombres.subList(0, nombres.size() - 1))
        + " y "
        + nombres.get(nombres.size() - 1);
  }

  public ConflictoFinca getConflicto() {
    return conflicto;
  }
}
