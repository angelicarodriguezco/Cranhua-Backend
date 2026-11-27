package com.tuanistack.cranhua.ai.datos;

import static org.assertj.core.api.Assertions.assertThat;

import com.tuanistack.cranhua.BasePostgis;
import com.tuanistack.cranhua.ai.datos.dto.CafeteriaKianaDto;
import com.tuanistack.cranhua.ai.datos.dto.CosechaActivaKianaDto;
import com.tuanistack.cranhua.ai.datos.dto.FincaKianaDto;
import com.tuanistack.cranhua.ai.datos.dto.TostadorKianaDto;
import com.tuanistack.cranhua.ai.datos.dto.TuesteKianaDto;
import com.tuanistack.cranhua.cafeteria.Cafeteria;
import com.tuanistack.cranhua.cafeteria.CafeteriaRepository;
import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.CuentaRepository;
import com.tuanistack.cranhua.cuenta.Rol;
import com.tuanistack.cranhua.finca.Cosecha;
import com.tuanistack.cranhua.finca.CosechaRepository;
import com.tuanistack.cranhua.finca.Finca;
import com.tuanistack.cranhua.finca.FincaRepository;
import com.tuanistack.cranhua.finca.GeometriaUtil;
import com.tuanistack.cranhua.finca.Proceso;
import com.tuanistack.cranhua.finca.ProcesoReparto;
import com.tuanistack.cranhua.finca.dto.Coordenada;
import com.tuanistack.cranhua.lote.Lote;
import com.tuanistack.cranhua.lote.LoteRepository;
import com.tuanistack.cranhua.solicitud.Solicitud;
import com.tuanistack.cranhua.solicitud.SolicitudRepository;
import com.tuanistack.cranhua.tueste.NegocioTueste;
import com.tuanistack.cranhua.tueste.NegocioTuesteRepository;
import com.tuanistack.cranhua.tueste.NivelTueste;
import com.tuanistack.cranhua.tueste.Tueste;
import com.tuanistack.cranhua.tueste.TuesteRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({BasePostgis.class, KianaDatosService.class})
class KianaDatosServiceTest {

  private static final AtomicInteger SEQ = new AtomicInteger();

  @Autowired private KianaDatosService datos;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private CafeteriaRepository cafeteriaRepository;

  @Autowired private FincaRepository fincaRepository;

  @Autowired private CosechaRepository cosechaRepository;

  @Autowired private NegocioTuesteRepository negocioTuesteRepository;

  @Autowired private SolicitudRepository solicitudRepository;

  @Autowired private LoteRepository loteRepository;

  @Autowired private TuesteRepository tuesteRepository;

  @Test
  void buscarCafeteriasExcluyeEliminadasYFiltraPorGuanacaste() {
    cafeteria(
        "Café Guanacaste",
        "Liberia",
        "Guanacaste",
        GeometriaUtil.punto(new Coordenada(10.6346, -85.4375)));
    cafeteria(
        "Café Escazú", "Escazú", "San José", GeometriaUtil.punto(new Coordenada(9.9205, -84.137)));
    Cafeteria eliminada =
        cafeteria(
            "Café Borrada",
            "Nicoya",
            "Guanacaste",
            GeometriaUtil.punto(new Coordenada(10.1450, -85.4520)));
    eliminada.eliminar();
    cafeteriaRepository.saveAndFlush(eliminada);

    List<CafeteriaKianaDto> resultado = datos.buscarCafeterias("Guanacaste", null, null);

    assertThat(resultado).extracting(CafeteriaKianaDto::nombre).containsExactly("Café Guanacaste");
  }

  @Test
  void buscarFincasExcluyeEliminadasYFiltraPorGuanacaste() {
    finca("Finca Guanacaste", "Liberia", "Guanacaste", 10.63, -85.44);
    finca("Finca Tarrazú", "Tarrazú", "San José", 9.66, -84.02);
    Finca eliminada = finca("Finca Borrada", "Nicoya", "Guanacaste", 10.14, -85.45);
    eliminada.eliminar();
    fincaRepository.saveAndFlush(eliminada);

    List<FincaKianaDto> resultado = datos.buscarFincas("Guanacaste", null, null);

    assertThat(resultado).extracting(FincaKianaDto::nombre).containsExactly("Finca Guanacaste");
  }

  @Test
  void listarCosechasActivasExcluyeFincaEliminadaYFiltraPorGuanacaste() {
    Finca guanacaste = finca("Finca Guanacaste", "Liberia", "Guanacaste", 10.63, -85.44);
    Finca sanJose = finca("Finca Tarrazú", "Tarrazú", "San José", 9.66, -84.02);
    Finca eliminada = finca("Finca Borrada", "Nicoya", "Guanacaste", 10.14, -85.45);

    cosechaActiva(guanacaste, "Geisha", "CRH-2026-9101");
    cosechaActiva(sanJose, "Caturra", "CRH-2026-9102");
    cosechaActiva(eliminada, "Typica", "CRH-2026-9103");
    eliminada.eliminar();
    fincaRepository.saveAndFlush(eliminada);

    List<CosechaActivaKianaDto> resultado = datos.listarCosechasActivas("Guanacaste", null, null);

    assertThat(resultado)
        .extracting(CosechaActivaKianaDto::finca)
        .containsExactly("Finca Guanacaste");
    assertThat(resultado)
        .extracting(CosechaActivaKianaDto::codigoLote)
        .containsExactly("CRH-2026-9101");
  }

  @Test
  void buscarTostadoresExcluyeEliminadosYFiltraPorGuanacaste() {
    negocio("Tueste Guanacaste", "Liberia", "Guanacaste");
    negocio("Tueste Central", "San José", "San José");
    NegocioTueste eliminado = negocio("Tueste Borrado", "Nicoya", "Guanacaste");
    eliminado.eliminar();
    negocioTuesteRepository.saveAndFlush(eliminado);

    List<TostadorKianaDto> resultado = datos.buscarTostadores("Guanacaste", null, null);

    assertThat(resultado).extracting(TostadorKianaDto::nombre).containsExactly("Tueste Guanacaste");
  }

  @Test
  void listarTuestesDisponiblesExcluyeNegocioEliminadoYFiltraPorGuanacaste() {
    NegocioTueste activo = negocio("Tueste Guanacaste", "Liberia", "Guanacaste");
    NegocioTueste otro = negocio("Tueste Central", "San José", "San José");
    NegocioTueste eliminado = negocio("Tueste Borrado", "Nicoya", "Guanacaste");

    tuesteDisponible(activo, "CRH-2026-9201");
    tuesteDisponible(otro, "CRH-2026-9202");
    tuesteDisponible(eliminado, "CRH-2026-9203");
    eliminado.eliminar();
    negocioTuesteRepository.saveAndFlush(eliminado);

    List<TuesteKianaDto> resultado = datos.listarTuestesDisponibles("Guanacaste", null, null);

    assertThat(resultado).extracting(TuesteKianaDto::tostador).containsExactly("Tueste Guanacaste");
  }

  private Cafeteria cafeteria(String nombre, String canton, String provincia, Point ubicacion) {
    return cafeteriaRepository.saveAndFlush(
        new Cafeteria(
            cuenta(Rol.CAFETERIA),
            nombre,
            "Contacto",
            "+506 8888-0000",
            correo("cafeteria"),
            ubicacion,
            canton,
            provincia,
            Set.of(NivelTueste.MEDIO)));
  }

  private Finca finca(String nombre, String canton, String provincia, double lat, double lng) {
    return fincaRepository.saveAndFlush(
        new Finca(
            nombre,
            perimetro(lat, lng),
            GeometriaUtil.punto(new Coordenada(lat, lng)),
            canton,
            provincia));
  }

  private Cosecha cosechaActiva(Finca finca, String variedad, String codigo) {
    Cosecha cosecha =
        new Cosecha(
            finca,
            variedad,
            1450,
            LocalDate.of(2026, 1, 5),
            LocalDate.of(2026, 2, 28),
            new BigDecimal("500.00"),
            new BigDecimal("9.75"),
            List.of(new ProcesoReparto(Proceso.LAVADO, 100)));
    cosecha.setCodigoLote(codigo);
    cosecha.registrarPesoTrasSeleccion(new BigDecimal("400.00"));
    cosecha.publicar();
    return cosechaRepository.saveAndFlush(cosecha);
  }

  private NegocioTueste negocio(String nombre, String canton, String provincia) {
    return negocioTuesteRepository.saveAndFlush(
        new NegocioTueste(
            cuenta(Rol.TOSTADOR),
            nombre,
            "Contacto",
            "+506 8777-0000",
            correo("tostador"),
            canton,
            provincia,
            Set.of(NivelTueste.CLARO)));
  }

  private Tueste tuesteDisponible(NegocioTueste negocio, String codigoLote) {
    int n = SEQ.incrementAndGet();
    Finca finca =
        finca("Origen " + codigoLote, "Tarrazú", "San José", 9.65 + n * 0.01, -84.02 - n * 0.01);
    Cosecha cosecha = cosechaActiva(finca, "Caturra", codigoLote);
    Solicitud solicitud = new Solicitud(cosecha, negocio.getCuenta(), new BigDecimal("50.00"));
    solicitud.aprobar();
    solicitudRepository.saveAndFlush(solicitud);
    Lote lote = new Lote(solicitud, codigoLote);
    lote.activar();
    loteRepository.saveAndFlush(lote);
    return tuesteRepository.saveAndFlush(
        new Tueste(
            lote,
            NivelTueste.MEDIO,
            new BigDecimal("40.00"),
            new BigDecimal("34.00"),
            LocalDate.of(2026, 3, 1)));
  }

  private Cuenta cuenta(Rol rol) {
    return cuentaRepository.saveAndFlush(
        new Cuenta("Usuario Kiana", correo(rol.name().toLowerCase()), "hash", Set.of(rol)));
  }

  private static String correo(String prefijo) {
    return prefijo + SEQ.incrementAndGet() + "@kiana-test.cr";
  }

  private static Polygon perimetro(double lat, double lng) {
    return GeometriaUtil.poligono(
        List.of(
            new Coordenada(lat, lng),
            new Coordenada(lat, lng + 0.004),
            new Coordenada(lat + 0.004, lng + 0.004),
            new Coordenada(lat + 0.004, lng)));
  }
}
