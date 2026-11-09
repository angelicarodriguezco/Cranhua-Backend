package com.tuanistack.cranhua.cafeteria;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuanistack.cranhua.BasePostgis;
import com.tuanistack.cranhua.auth.CookieSesion;
import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.CuentaRepository;
import com.tuanistack.cranhua.cuenta.Rol;
import jakarta.servlet.http.Cookie;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(BasePostgis.class)
class CafeteriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CookieSesion cookieSesion;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private CafeteriaRepository cafeteriaRepository;

    private Cuenta sofia;

    private Cuenta diego;

    @BeforeEach
    void crearCuentas() {
        sofia = cuenta("Sofía Jiménez", "sofia@ejemplo.cr", Rol.CAFETERIA);
        diego = cuenta("Diego Mora", "diego@ejemplo.cr", Rol.CAFETERIA);
    }

    @AfterEach
    void limpiar() {
        cafeteriaRepository.deleteAll();
        cuentaRepository.deleteAll();
    }

    private Cuenta cuenta(String nombre, String correo, Rol rol) {
        return cuentaRepository.saveAndFlush(new Cuenta(nombre, correo, "hash", Set.of(rol)));
    }

    private Cookie sesion(Cuenta cuenta) {
        return new Cookie(CookieSesion.NOMBRE, cookieSesion.crear(cuenta).getValue());
    }

    private static Map<String, Object> cafeteria(String nombre) {
        Map<String, Object> cafeteria = new LinkedHashMap<>();
        cafeteria.put("nombre", nombre);
        cafeteria.put("nombreContacto", "Sofía Jiménez");
        cafeteria.put("telefono", "+506 8888-1234");
        cafeteria.put("correo", "Hola@Tuanis.CR");
        cafeteria.put("descripcion", "Café de especialidad en el centro.");
        cafeteria.put("horario", "L-S 7:00 a 18:00");
        cafeteria.put("ubicacion", Map.of("lat", 9.9325, "lng", -84.0795));
        cafeteria.put("canton", "San José");
        cafeteria.put("provincia", "San José");
        cafeteria.put("estilos", List.of("MEDIO", "CLARO"));
        cafeteria.put("volumenMensual", "DE_10_A_25");
        return cafeteria;
    }

    private ResultActions registrar(Cuenta cuenta, Map<String, Object> cafeteria)
            throws Exception {
        return mockMvc.perform(
                post("/api/cafeterias")
                        .cookie(sesion(cuenta))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cafeteria)));
    }

    private String registrarId(Cuenta cuenta) throws Exception {
        String respuesta =
                registrar(cuenta, cafeteria("Café Tuanis"))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return objectMapper.readTree(respuesta).get("id").asText();
    }

    private ResultActions editar(Cuenta cuenta, String id, Map<String, Object> cambios)
            throws Exception {
        return mockMvc.perform(
                patch("/api/cafeterias/" + id)
                        .cookie(sesion(cuenta))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cambios)));
    }

    @Test
    void cafeteriaRegistraSuLocal() throws Exception {
        registrar(sofia, cafeteria(" Café Tuanis "))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.cuentaId").value(sofia.getId().toString()))
                .andExpect(jsonPath("$.nombre").value("Café Tuanis"))
                .andExpect(jsonPath("$.correo").value("hola@tuanis.cr"))
                .andExpect(jsonPath("$.horario").value("L-S 7:00 a 18:00"))
                .andExpect(jsonPath("$.ubicacion.lat").value(9.9325))
                .andExpect(jsonPath("$.ubicacion.lng").value(-84.0795))
                .andExpect(jsonPath("$.estilos", contains("CLARO", "MEDIO")))
                .andExpect(jsonPath("$.volumenMensual").value("DE_10_A_25"));
    }

    @Test
    void segundaCafeteriaActivaDeLaMismaCuentaResponde409() throws Exception {
        registrarId(sofia);

        registrar(sofia, cafeteria("Otra Cafetería")).andExpect(status().isConflict());
    }

    @Test
    void despuesDeEliminarPuedeRegistrarOtra() throws Exception {
        String id = registrarId(sofia);
        Cafeteria anterior = cafeteriaRepository.findById(UUID.fromString(id)).orElseThrow();
        anterior.eliminar();
        cafeteriaRepository.saveAndFlush(anterior);

        registrar(sofia, cafeteria("Café Nuevo")).andExpect(status().isCreated());
    }

    @Test
    void camposObligatoriosFaltantesResponden400PorCampo() throws Exception {
        registrar(sofia, Map.of("descripcion", "Sin datos"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").value("Ingresá el nombre de la cafetería."))
                .andExpect(jsonPath("$.errores.nombreContacto").exists())
                .andExpect(jsonPath("$.errores.telefono").exists())
                .andExpect(jsonPath("$.errores.correo").exists())
                .andExpect(jsonPath("$.errores.ubicacion").exists())
                .andExpect(jsonPath("$.errores.canton").exists())
                .andExpect(jsonPath("$.errores.provincia").exists())
                .andExpect(jsonPath("$.errores.estilos").exists());
    }

    @Test
    void descripcionYHorarioMuyLargosResponden400() throws Exception {
        Map<String, Object> datos = cafeteria("Café Tuanis");
        datos.put("descripcion", "a".repeat(301));
        datos.put("horario", "a".repeat(101));

        registrar(sofia, datos)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.descripcion").exists())
                .andExpect(jsonPath("$.errores.horario").exists());

        String id = registrarId(sofia);
        editar(sofia, id, Map.of("horario", "a".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.horario").exists());
    }

    @Test
    void descripcionYHorarioSonOpcionales() throws Exception {
        Map<String, Object> datos = cafeteria("Café Tuanis");
        datos.remove("descripcion");
        datos.remove("horario");

        registrar(sofia, datos)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descripcion").doesNotExist())
                .andExpect(jsonPath("$.horario").doesNotExist());
    }

    @Test
    void ubicacionFueraDeRangoResponde400() throws Exception {
        Map<String, Object> datos = cafeteria("Café Tuanis");
        datos.put("ubicacion", Map.of("lat", 120, "lng", -84.0795));

        registrar(sofia, datos)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores['ubicacion.lat']").exists());
    }

    @Test
    void editaLosDatosDeLaCafeteria() throws Exception {
        String id = registrarId(sofia);
        Map<String, Object> cambios = new LinkedHashMap<>();
        cambios.put("nombre", "Tuanis Café Bar");
        cambios.put("nombreContacto", "Luis Rojas");
        cambios.put("telefono", "+506 7777-0000");
        cambios.put("correo", "contacto@tuanis.cr");
        cambios.put("ubicacion", Map.of("lat", 9.9187, "lng", -84.1415));
        cambios.put("canton", "Escazú");
        cambios.put("provincia", "San José");
        cambios.put("descripcion", " ");
        cambios.put("horario", "Todos los días 8:00 a 20:00");
        cambios.put("estilos", List.of("OSCURO"));
        cambios.put("volumenMensual", "MAS_DE_100");
        cambios.put("portadaUrl", "https://res.cloudinary.com/cranhua/portada.jpg");

        editar(sofia, id, cambios)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Tuanis Café Bar"))
                .andExpect(jsonPath("$.nombreContacto").value("Luis Rojas"))
                .andExpect(jsonPath("$.telefono").value("+506 7777-0000"))
                .andExpect(jsonPath("$.correo").value("contacto@tuanis.cr"))
                .andExpect(jsonPath("$.ubicacion.lat").value(9.9187))
                .andExpect(jsonPath("$.ubicacion.lng").value(-84.1415))
                .andExpect(jsonPath("$.canton").value("Escazú"))
                .andExpect(jsonPath("$.descripcion").doesNotExist())
                .andExpect(jsonPath("$.horario").value("Todos los días 8:00 a 20:00"))
                .andExpect(jsonPath("$.estilos", contains("OSCURO")))
                .andExpect(jsonPath("$.volumenMensual").value("MAS_DE_100"))
                .andExpect(
                        jsonPath("$.portadaUrl").value("https://res.cloudinary.com/cranhua/portada.jpg"));

        mockMvc
                .perform(get("/api/cafeterias/" + id).cookie(sesion(sofia)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Tuanis Café Bar"))
                .andExpect(jsonPath("$.estilos", contains("OSCURO")));
    }

    @Test
    void editarSoloCambiaLoQueViene() throws Exception {
        String id = registrarId(sofia);

        editar(sofia, id, Map.of("telefono", "+506 7777-0000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Café Tuanis"))
                .andExpect(jsonPath("$.horario").value("L-S 7:00 a 18:00"))
                .andExpect(jsonPath("$.ubicacion.lat").value(9.9325))
                .andExpect(jsonPath("$.estilos", contains("CLARO", "MEDIO")));
    }

    @Test
    void editarConDatosInvalidosResponde400() throws Exception {
        String id = registrarId(sofia);

        editar(sofia, id, Map.of("nombre", " ", "correo", "", "estilos", List.of()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists())
                .andExpect(jsonPath("$.errores.correo").exists())
                .andExpect(jsonPath("$.errores.estilos").exists());
    }

    @Test
    void soloUnaCuentaCafeteriaUsaLosEndpoints() throws Exception {
        Cuenta tostador = cuenta("Mariana Castro", "mariana@ejemplo.cr", Rol.TOSTADOR);
        String id = registrarId(sofia);

        registrar(tostador, cafeteria("Café Tuanis")).andExpect(status().isForbidden());
        mockMvc
                .perform(get("/api/cafeterias/" + id).cookie(sesion(tostador)))
                .andExpect(status().isForbidden());
        editar(tostador, id, Map.of("nombre", "Otro")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/cafeterias/" + id)).andExpect(status().isUnauthorized());
    }

    @Test
    void consultarOEditarCafeteriaAjenaResponde403() throws Exception {
        String id = registrarId(sofia);

        mockMvc
                .perform(get("/api/cafeterias/" + id).cookie(sesion(diego)))
                .andExpect(status().isForbidden());
        editar(diego, id, Map.of("nombre", "Robada")).andExpect(status().isForbidden());
    }

    @Test
    void cafeteriaConsultaSuLocalActual() throws Exception {
        String id = registrarId(sofia);

        mockMvc
                .perform(get("/api/cafeterias/actual").cookie(sesion(sofia)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        mockMvc
                .perform(get("/api/cafeterias/actual").cookie(sesion(diego)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cafeteriaInexistenteResponde404() throws Exception {
        mockMvc
                .perform(get("/api/cafeterias/" + UUID.randomUUID()).cookie(sesion(sofia)))
                .andExpect(status().isNotFound());
    }
}