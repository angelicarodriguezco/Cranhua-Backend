package com.tuanistack.cranhua.finca;

import com.tuanistack.cranhua.finca.dto.Coordenada;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;

// JTS usa x = lng, y = lat
public final class GeometriaUtil {

  public static final int SRID = 4326;

  private static final GeometryFactory FABRICA = new GeometryFactory(new PrecisionModel(), SRID);

  private GeometriaUtil() {}

  public static Point punto(Coordenada coordenada) {
    return FABRICA.createPoint(new Coordinate(coordenada.lng(), coordenada.lat()));
  }

  // Google Maps no repite el primer punto al final; si viene abierto se cierra el anillo
  public static Polygon poligono(List<Coordenada> puntos) {
    List<Coordinate> anillo = new ArrayList<>();
    puntos.forEach(punto -> anillo.add(new Coordinate(punto.lng(), punto.lat())));
    if (!anillo.get(0).equals2D(anillo.get(anillo.size() - 1))) {
      anillo.add(anillo.get(0).copy());
    }
    return FABRICA.createPolygon(anillo.toArray(Coordinate[]::new));
  }

  public static Coordenada coordenada(Point punto) {
    return new Coordenada(punto.getY(), punto.getX());
  }

  // Se devuelve sin repetir el primer punto, igual que lo manda el frontend
  public static List<Coordenada> puntos(Polygon poligono) {
    Coordinate[] anillo = poligono.getExteriorRing().getCoordinates();
    return Arrays.stream(anillo, 0, anillo.length - 1)
        .map(c -> new Coordenada(c.getY(), c.getX()))
        .toList();
  }
}
