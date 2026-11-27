package com.tuanistack.cranhua.ai;

import com.tuanistack.cranhua.ai.datos.KianaDatosService;
import com.tuanistack.cranhua.ai.datos.dto.CafeteriaKianaDto;
import com.tuanistack.cranhua.ai.datos.dto.CosechaActivaKianaDto;
import com.tuanistack.cranhua.ai.datos.dto.FincaKianaDto;
import com.tuanistack.cranhua.ai.datos.dto.TostadorKianaDto;
import com.tuanistack.cranhua.ai.datos.dto.TuesteKianaDto;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class KianaHerramientas {

  private final KianaDatosService datos;

  public KianaHerramientas(KianaDatosService datos) {
    this.datos = datos;
  }

  @Tool(
      description =
          """
          Busca cafeterías activas en Cránhua. Usa filtros opcionales de provincia, cantón o nombre.
          No incluye teléfono, correo ni nombre de contacto.
          """)
  public List<CafeteriaKianaDto> buscarCafeterias(
      @ToolParam(description = "Provincia de Costa Rica, por ejemplo Guanacaste", required = false)
          String provincia,
      @ToolParam(description = "Cantón", required = false) String canton,
      @ToolParam(description = "Nombre o fragmento del nombre de la cafetería", required = false)
          String nombre) {
    return datos.buscarCafeterias(provincia, canton, nombre);
  }

  @Tool(
      description =
          """
          Busca fincas activas en Cránhua. Usa filtros opcionales de provincia, cantón o nombre.
          No incluye contactos de dueños.
          """)
  public List<FincaKianaDto> buscarFincas(
      @ToolParam(description = "Provincia de Costa Rica, por ejemplo Guanacaste", required = false)
          String provincia,
      @ToolParam(description = "Cantón", required = false) String canton,
      @ToolParam(description = "Nombre o fragmento del nombre de la finca", required = false)
          String nombre) {
    return datos.buscarFincas(provincia, canton, nombre);
  }

  @Tool(
      description =
          """
          Lista cosechas activas publicadas con kilogramos disponibles. Usa filtros opcionales
          de provincia, cantón o nombre de finca. No incluye precio por kilogramo.
          """)
  public List<CosechaActivaKianaDto> listarCosechasActivas(
      @ToolParam(description = "Provincia de Costa Rica, por ejemplo Guanacaste", required = false)
          String provincia,
      @ToolParam(description = "Cantón", required = false) String canton,
      @ToolParam(description = "Nombre o fragmento del nombre de la finca", required = false)
          String nombre) {
    return datos.listarCosechasActivas(provincia, canton, nombre);
  }

  @Tool(
      description =
          """
          Busca tostadores (negocios de tueste) activos en Cránhua. Usa filtros opcionales de
          provincia, cantón o nombre. No incluye teléfono, correo ni nombre de contacto.
          """)
  public List<TostadorKianaDto> buscarTostadores(
      @ToolParam(description = "Provincia de Costa Rica, por ejemplo Guanacaste", required = false)
          String provincia,
      @ToolParam(description = "Cantón", required = false) String canton,
      @ToolParam(description = "Nombre o fragmento del nombre del tostador", required = false)
          String nombre) {
    return datos.buscarTostadores(provincia, canton, nombre);
  }

  @Tool(
      description =
          """
          Lista tuestes disponibles (con kilogramos disponibles) en Cránhua. Usa filtros
          opcionales de provincia, cantón o nombre del tostador. No incluye pedidos ni precios.
          """)
  public List<TuesteKianaDto> listarTuestesDisponibles(
      @ToolParam(description = "Provincia de Costa Rica, por ejemplo Guanacaste", required = false)
          String provincia,
      @ToolParam(description = "Cantón", required = false) String canton,
      @ToolParam(description = "Nombre o fragmento del nombre del tostador", required = false)
          String nombre) {
    return datos.listarTuestesDisponibles(provincia, canton, nombre);
  }
}
