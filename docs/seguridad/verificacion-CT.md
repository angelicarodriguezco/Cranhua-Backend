# Verificación de seguridad (CP3-120)

Criterios CT-01 a CT-05 y CT-10. Las pruebas automatizadas corren con `./mvnw verify` (Docker Desktop abierto). Los pasos manuales están en `cranhua-seguridad.postman_collection.json`, en esta misma carpeta.

## CT-01 Permisos por rol

| CT | Qué se verifica | Cómo | Resultado esperado | Resultado obtenido |
|----|-----------------|------|--------------------|--------------------|
| CT-01 | Todos los endpoints protegidos piden sesión | `config/BarridoEndpointsTest.sinSesionDa401` (un caso por endpoint, sale como "METODO /ruta") | 401 en los 53 endpoints | 53/53 en 401, 2026-11-27 |
| CT-01 | Cada endpoint protegido tiene `@PreAuthorize` en el método o en la clase | `config/BarridoEndpointsTest.tienePreAuthorize` | Ninguno sin `@PreAuthorize` | 53/53 con `@PreAuthorize`, sin excepciones, 2026-11-27 |
| CT-01 | Un rol que no está en el `@PreAuthorize` no entra | `config/BarridoEndpointsTest.conUnRolQueNoEstaEnPreAuthorizeDa403` | 403 en los 53 endpoints | 53/53 en 403, 2026-11-27 |
| CT-01 | Reglas de rol, sesión sin roles y cuenta con varios roles | `config/SeguridadPorRolTest` (todos los métodos) | 401 sin sesión, 403 sin el rol, 200 con el rol | En verde, 2026-11-27 |
| CT-01 | Un endpoint por rol sin cookie y con rol incorrecto | Manual: Postman, carpeta "CT-01 Permisos por rol" | 401 sin cookie, 403 con otro rol | Pendiente de ejecutar |

Los endpoints con `@Valid @RequestBody` validan el body antes de que corra `@PreAuthorize`. Con `{}`, 13 de ellos responden 400 en vez de 403. El barrido les manda un body válido mínimo (`CUERPOS_VALIDOS`) para llegar a la autorización.

## CT-02 Cookie de sesión httpOnly

| CT | Qué se verifica | Cómo | Resultado esperado | Resultado obtenido |
|----|-----------------|------|--------------------|--------------------|
| CT-02 | Inicio de sesión devuelve la cookie con los atributos | `auth/AuthControllerTest.loginValidoDevuelveLaCookieConElJwt` | `HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/` | En verde, 2026-11-27 |
| CT-02 | Registro devuelve la cookie con los atributos | `auth/AuthControllerTest.registroDevuelveLaCookieHttpOnlySecureYStrict` | `HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/` | En verde, 2026-11-27 |
| CT-02 | Cerrar sesión borra la cookie | `auth/AuthControllerTest.cerrarSesionBorraLaCookie` | `Max-Age=0` | En verde, 2026-11-27 |
| CT-02 | El token no va en el cuerpo JSON | `auth/AuthControllerTest.elTokenSoloVaEnLaCookieYNoEnElCuerpo` (registro, inicio de sesión y `GET /api/cuenta`) y `auth/AuthControllerTest.elTokenDeGoogleTampocoVaEnElCuerpo` | El cuerpo no tiene el JWT ni un campo `token` | En verde, 2026-11-27 |

## CT-03 Contraseñas

| CT | Qué se verifica | Cómo | Resultado esperado | Resultado obtenido |
|----|-----------------|------|--------------------|--------------------|
| CT-03 | La columna `cuenta.contrasena_hash` guarda BCrypt | `auth/AuthControllerTest.laContrasenaSeGuardaConBcryptEnLaBase` (lee con `JdbcTemplate`) | Distinta del texto plano, cumple `^\$2[aby]\$\d{2}\$.{53}$`, `PasswordEncoder.matches` da true | En verde, 2026-11-27 |
| CT-03 | `GET /api/cuenta` no expone la contraseña ni el hash | `auth/AuthControllerTest.laCuentaNoDevuelveLaContrasenaNiElHash` | Sin hash, sin texto plano, sin campos `contrasena` ni `password` | En verde, 2026-11-27 |
| CT-03 | Las respuestas de auth no exponen la contraseña | `auth/AuthControllerTest.registraConUnRolYDejaLaSesionIniciada` y `loginValidoDevuelveLaCookieConElJwt` | Sin `contrasena` ni `$2a$` | En verde, 2026-11-27 |

## CT-04 CORS

| CT | Qué se verifica | Cómo | Resultado esperado | Resultado obtenido |
|----|-----------------|------|--------------------|--------------------|
| CT-04 | GET real con origen ajeno y sesión válida | `config/CorsConfigTest.getConOrigenAjenoYSesionValidaDa403SinCabecerasCors` | 403 sin `Access-Control-Allow-Origin` | En verde, 2026-11-27 |
| CT-04 | POST real con origen ajeno y sesión válida | `config/CorsConfigTest.postConOrigenAjenoYSesionValidaDa403SinCabecerasCors` | 403 sin `Access-Control-Allow-Origin` | En verde, 2026-11-27 |
| CT-04 | Preflight con origen ajeno | `config/CorsConfigTest.preflightFromOtherOriginIsRejected` | 403 sin `Access-Control-Allow-Origin` | En verde, 2026-11-27 |
| CT-04 | GET y POST desde `http://localhost:4200` | `config/CorsConfigTest.getDesdeAngularConSesionPermiteOrigenYCredenciales` y `postDesdeAngularConSesionPermiteOrigenYCredenciales` | `Access-Control-Allow-Origin` igual al origen y `Access-Control-Allow-Credentials: true` | En verde, 2026-11-27 |
| CT-04 | Request con header `Origin` ajeno contra el backend corriendo | Manual: Postman, carpeta "CT-04 CORS" | 403 sin `Access-Control-Allow-Origin` | Pendiente de ejecutar |

## CT-05 Evadir el frontend

| CT | Qué se verifica | Cómo | Resultado esperado | Resultado obtenido |
|----|-----------------|------|--------------------|--------------------|
| CT-05 | Traslape de fincas | `finca/FincaControllerTest.sobreponerseConOtraFincaDa409SinGuardar` y `editarConPerimetroSobrepuestoDa409` | 409 y no se guarda | En verde, 2026-11-27 |
| CT-05 | Solicitud con más kg que los disponibles | `solicitud/SolicitudControllerTest.cantidadMayorALoDisponibleDa400` | 400 | En verde, 2026-11-27 |
| CT-05 | Aprobar sin cantidad suficiente | `solicitud/SolicitudControllerTest.aprobarSinCantidadSuficienteDa409YNoCambiaNada` | 409 y no cambia nada | En verde, 2026-11-27 |
| CT-05 | Pedido con más kg que los disponibles | `pedido/PedidoControllerTest.cantidadInvalidaOMayorALoDisponibleDa400` | 400 | En verde, 2026-11-27 |
| CT-05 | Lote con código duplicado | `lote/LoteRepositoryTest.dosLotesConElMismoCodigoFallan` | Falla por `uq_lote_codigo` | En verde, 2026-11-27 |
| CT-05 | Activar un código dos veces | `lote/LoteControllerTest.activarDosVecesNoCambiaElCodigo` | Mismo código | En verde, 2026-11-27 |
| CT-05 | Publicar en el menú un código inexistente o pendiente | `cafeteria/ItemMenuControllerTest.unCodigoInexistenteOPendienteDa404` | 404 y no se guarda | En verde, 2026-11-27 |
| CT-05 | Guardar un código que no es de un lote | `cafeteria/CafeteriaRepositoryTest.rechazaCodigoQueNoEsDeUnLote` | Falla en la base | En verde, 2026-11-27 |
| CT-05 | Publicar en el menú de otra cafetería | `cafeteria/ItemMenuControllerTest.otraCafeteriaRecibe403` | 403 | En verde, 2026-11-27 |
| CT-05 | Editar una finca ajena | `finca/FincaControllerTest.soloUnDuenoPuedeEditarLaFinca` | 403 | En verde, 2026-11-27 |
| CT-05 | Editar o eliminar una cosecha ajena | `finca/CosechaEdicionControllerTest.editarOEliminarCosechaAjenaDa403` | 403 | En verde, 2026-11-27 |
| CT-05 | Solicitar la propia cosecha | `solicitud/SolicitudControllerTest.noSePuedeSolicitarLaPropiaCosechaNiSinRolTostador` | 403 | En verde, 2026-11-27 |
| CT-05 | Los tres casos contra el backend corriendo: traslape, cantidad de más y código repetido | Manual: Postman, carpeta "CT-05 Evadir el frontend" | 409, 400, mismo código y 404 | Pendiente de ejecutar |

## CT-10 Credenciales

| CT | Qué se verifica | Cómo | Resultado esperado | Resultado obtenido |
|----|-----------------|------|--------------------|--------------------|
| CT-10 | Configuración del backend sin valores reales | Revisión de `application.yml`, `application-test.yml`, `.env.example` y `docker-compose.yml` | Solo variables de entorno o valores vacíos | Sin valores reales. `application-test.yml` solo tiene valores de prueba. `.env` está en `.gitignore` y nunca se commiteó. 2026-11-27 |
| CT-10 | Configuración del frontend sin valores reales | Revisión de `.env.example`, `scripts/generar-environment.js` y `.gitignore` | Solo variables o valores vacíos | Sin valores reales. `src/environments/environment.ts` se genera desde `.env` y está en `.gitignore`. 2026-11-27 |
| CT-10 | Historial completo de los dos repos | `gitleaks detect --log-opts=--all` (v8.30.1, imagen `zricethezav/gitleaks`), más `git log -p --all` buscando `AIza`, `sk-`, `secret`, `password=`, `client_secret`, `BEGIN PRIVATE KEY` y archivos `.env` | Sin credenciales | Backend: 108 commits, sin hallazgos. Frontend: 138 commits, sin hallazgos. La búsqueda manual solo encontró el placeholder `api-key: "GEMINI_API_KEY"` de `application-test.yml`. Ningún `.env` en el historial salvo `.env.example`. 2026-11-27 |
