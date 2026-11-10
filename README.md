# Crán̈hua

Proyecto de trazabilidad de café de especialidad para Costa Rica que conecta a cafetaleros, tostadores y cafeterías en un solo sistema, para que cualquier persona pueda ver de dónde salió el café que se está tomando con solo un código en la bolsa.

Este repositorio es el backend: Spring Boot 3.5 con Java 21, PostgreSQL 17 + PostGIS y Spring AI (Gemini). El frontend está en [Cranhua-Frontend](https://github.com/angelicarodriguezco/Cranhua-Frontend).

## Requisitos

- Git
- Docker Desktop abierto (incluye Docker Compose)
- JDK 21, solo si se va a correr el backend fuera de Docker. No hace falta instalar Maven, el repo trae `mvnw`.

## Dependencias externas

Servicios y secretos que el backend usa. Ningún valor real va en el README ni en git: solo en el `.env` local.

### Google Gemini (chat IA)

- **Para qué:** endpoint `POST /api/ai/chat` (Spring AI).
- **Variable:** `GEMINI_API_KEY` (obligatoria para que Docker Compose arranque; sin una llave válida el chat falla).
- **Cómo obtenerla:**
  1. Entrar a [Google AI Studio](https://aistudio.google.com/apikey) con una cuenta de Google.
  2. Crear una API key.
  3. Pegarla en `.env` como `GEMINI_API_KEY`.

### Google Sign-In (registro / login)

- **Para qué:** `POST /api/auth/registro/google` y `POST /api/auth/iniciarSesion/google`.
- **Variable:** `GOOGLE_CLIENT_ID` (opcional; sin ella el backend levanta, pero esas rutas responden 401).
- **Cómo obtenerlo:**
  1. Ir a [Google Cloud Console](https://console.cloud.google.com/) y crear o elegir un proyecto.
  2. En _APIs y servicios_ → _Credenciales_, crear un ID de cliente OAuth tipo **Aplicación web**.
  3. En orígenes autorizados de JavaScript agregar el origen del frontend (en local, `http://localhost:4200`).
  4. Copiar el **Client ID** a `.env` como `GOOGLE_CLIENT_ID`.
  5. El mismo Client ID debe usarse en el frontend (Google Identity Services).

### Cloudinary (fotos y banners)

- **Para qué:** subida de foto/banner de cuenta (`POST /api/cuenta/foto`, `POST /api/cuenta/banner`).
- **Variables:** `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` (opcionales; sin ellas el backend levanta, pero la subida falla).
- **Cómo obtenerlas:**
  1. Crear cuenta en [Cloudinary](https://cloudinary.com).
  2. En el Dashboard → _Account Details_ / _API Keys_, copiar Cloud name, API Key y API Secret.
  3. Pegarlos en `.env`. No compartir el secret en issues ni PRs.

### Correo SMTP (recuperación de contraseña)

- **Para qué:** envío del código en `POST /api/auth/recuperar`.
- **Variables:** `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` (opcionales; sin ellas el backend levanta, pero los códigos no llegan).
- **Cómo obtenerlas (Gmail):**
  1. Activar la verificación en 2 pasos en la cuenta de Google.
  2. Crear una [contraseña de aplicación](https://myaccount.google.com/apppasswords) (no usar la contraseña normal de la cuenta).
  3. En `.env`: host SMTP de Gmail, puerto STARTTLS, el correo en `MAIL_USERNAME` y la contraseña de aplicación en `MAIL_PASSWORD`.

### Secreto JWT (sesión)

- **Para qué:** firmar la cookie de sesión `cranhua_sesion`.
- **Variable:** `JWT_SECRET` (obligatoria; mínimo 32 caracteres).
- **Cómo obtenerla:** generarla en local, no pedirla a un proveedor:

```bash
openssl rand -base64 48
```

Pegar el resultado en `.env` como `JWT_SECRET`.

### Base de datos (PostgreSQL + PostGIS)

- **Para qué:** persistencia; la imagen la levanta Docker Compose (`postgis/postgis`).
- **Variables:** `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` (obligatorias). `DB_PORT` y `BACKEND_PORT` son opcionales si los puertos por defecto están libres.
- **Cómo obtenerlas:** no hay proveedor externo; definir usuario, nombre de base y contraseña propios en `.env`. La contraseña solo vive ahí.

## Variables de entorno

Copiar el archivo de ejemplo en la raíz del repo:

```bash
cp .env.example .env      # Git Bash / macOS / Linux
copy .env.example .env    # PowerShell / CMD
```

Completar el `.env` con lo de la sección anterior. Resumen:

| Variable                | Obligatoria | Dependencia |
| ----------------------- | ----------- | ----------- |
| `GEMINI_API_KEY`        | Sí          | Google Gemini |
| `GOOGLE_CLIENT_ID`      | No          | Google Sign-In |
| `JWT_SECRET`            | Sí          | Secreto JWT (generado en local) |
| `POSTGRES_DB`           | Sí          | PostgreSQL / PostGIS |
| `POSTGRES_USER`         | Sí          | PostgreSQL / PostGIS |
| `POSTGRES_PASSWORD`     | Sí          | PostgreSQL / PostGIS |
| `DB_PORT`               | No          | PostgreSQL / PostGIS (puerto en la máquina) |
| `BACKEND_PORT`          | No          | Puerto del backend en Docker |
| `MAIL_HOST`             | No          | Correo SMTP |
| `MAIL_PORT`             | No          | Correo SMTP |
| `MAIL_USERNAME`         | No          | Correo SMTP |
| `MAIL_PASSWORD`         | No          | Correo SMTP |
| `CLOUDINARY_CLOUD_NAME` | No          | Cloudinary |
| `CLOUDINARY_API_KEY`    | No          | Cloudinary |
| `CLOUDINARY_API_SECRET` | No          | Cloudinary |

El `.env` está en `.gitignore`. No subirlo ni pegar llaves reales en issues o PRs.

## Levantar todo con Docker

```bash
docker compose up -d --build
```

Esto levanta dos contenedores: `cranhua-db` (PostGIS) y `cranhua-backend`. El backend espera a que la base esté lista. La primera vez tarda unos minutos porque descarga las imágenes y las dependencias de Maven.

Verificar que responde abriendo http://localhost:8080/actuator/health en el navegador. Debe devolver `{"status":"UP"}`.

Comandos útiles:

```bash
docker compose logs -f backend   # ver logs del backend
docker compose ps                # estado de los contenedores
docker compose down              # apagar
docker compose down -v           # apagar y borrar los datos de la base
```

Después de cambiar código, volver a correr `docker compose up -d --build` para reconstruir la imagen.

## Desarrollo desde IntelliJ

Para depurar o ver los cambios sin reconstruir la imagen, levantar solo la base en Docker y el backend local:

```bash
docker compose up -d db
./mvnw spring-boot:run        # Git Bash / macOS / Linux
.\mvnw.cmd spring-boot:run    # PowerShell
```

También se puede correr `CranhuaApplication` desde IntelliJ. En ese caso revisar que en la configuración de ejecución el _Working directory_ sea la raíz de este repo, porque `application.yml` lee el `.env` desde ahí.

Si el contenedor `cranhua-backend` está corriendo, el puerto 8080 va a estar ocupado. Apagarlo antes con `docker compose stop backend`.

## Base de datos y migraciones

Las tablas no se crean a mano: al arrancar, el backend corre con Flyway las migraciones de `src/main/resources/db/migration` que falten. Después Hibernate valida que las entidades coincidan con las tablas (`ddl-auto: validate`) y, si no coinciden, el backend no arranca.

Para cambiar el esquema se agrega un archivo nuevo cuya versión es la fecha y hora en que se crea, por ejemplo `V20261108_0915__crear_solicitud.sql`. Así dos ramas abiertas al mismo tiempo nunca usan la misma versión, y como Flyway corre con `out-of-order`, se pueden integrar en cualquier orden.

Nunca editar, renombrar ni borrar una migración que ya está en `develop`: las bases que ya la aplicaron dejan de arrancar. Si hay un choque, se cambia la migración de la rama que todavía no se integra. El CI revisa las dos cosas en cada PR.

Para ver qué migraciones se aplicaron:

```bash
docker compose exec db psql -U cranhua -d cranhua -c "select version, description, success from flyway_schema_history"
```

## Probar PostGIS

El script `docker/db/prueba-postgis.sql` crea una finca de prueba en una tabla temporal y calcula su área. No deja nada guardado.

```bash
# Git Bash / macOS / Linux
docker compose exec -T db psql -U cranhua -d cranhua < docker/db/prueba-postgis.sql

# PowerShell
Get-Content docker\db\prueba-postgis.sql | docker compose exec -T db psql -U cranhua -d cranhua
```

Debe mostrar la versión de PostGIS y una fila con `es_valido = t`, el área en hectáreas y el GeoJSON del polígono.

## Endpoints disponibles

| Método | Ruta                             | Acceso  | Descripción                                                                                                                                                                               |
| ------ | -------------------------------- | ------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `GET`  | `/actuator/health`               | Público | Estado del servicio                                                                                                                                                                       |
| `POST` | `/api/ai/chat`                   | Sesión  | Chat con Gemini. Body: `{"message": "..."}`. Respuesta: `{"reply": "..."}`                                                                                                                |
| `POST` | `/api/auth/registro`             | Público | Registro con correo. Body: `{"nombreCompleto", "correo", "contrasena", "roles": ["CAFETALERO"], "telefono"}`. Responde 201 con la cuenta y deja la sesión iniciada                        |
| `POST` | `/api/auth/registro/google`      | Público | Registro con Google. Body: `{"credencial": "<ID token>", "roles": [...]}`                                                                                                                 |
| `POST` | `/api/auth/iniciarSesion`        | Público | Login con correo. Body: `{"correo", "contrasena"}`. Responde 200 con la cuenta y la cookie de sesión. Credenciales incorrectas o cuenta eliminada: 401 `Correo o contraseña incorrectos.` |
| `POST` | `/api/auth/iniciarSesion/google` | Público | Login con Google. Body: `{"credencial": "<ID token>"}`. 401 si el token no es válido o no hay cuenta (activa) registrada con esa cuenta de Google                                         |
| `POST` | `/api/auth/cerrarSesion`         | Público | Borra la cookie de sesión. Responde 204                                                                                                                                                   |
| `POST` | `/api/auth/recuperar`            | Público | Body: `{"correo"}`. Si hay una cuenta activa con ese correo le manda un código de 6 dígitos que vence a los 10 minutos. Responde 204 siempre, exista o no el correo                     |
| `POST` | `/api/auth/verificarCodigo`      | Público | Body: `{"correo", "codigo"}`. 204 si el código sirve (no lo gasta). Código equivocado o vencido: 400 `El código no es válido o ya venció.` Al quinto fallo el código se invalida         |
| `POST` | `/api/auth/restablecer`          | Público | Body: `{"correo", "codigo", "contrasena"}`. La contraseña sigue las reglas del registro. Gasta el código, cierra todas las sesiones de la cuenta y borra la cookie. Responde 204           |
| `GET`  | `/api/cuenta`                    | Sesión  | Perfil personal de la sesión: `{id, nombreCompleto, correo, roles, telefono, fotoUrl, redes}`                                                                                             |
| `GET`  | `/api/cuenta/actual`             | Sesión  | Alias de `GET /api/cuenta` (compatibilidad)                                                                                                                                               |
| `PATCH`| `/api/cuenta`                    | Sesión  | Actualiza campos editables del perfil (`nombreCompleto`, `telefono`, `fotoUrl`, `redes`). Body parcial; `redes` reemplaza la colección completa. Responde la cuenta actualizada           |
| `DELETE`| `/api/cuenta`                   | Sesión  | Soft-delete de la cuenta y borra la cookie de sesión. Responde 204                                                                                                                        |
| `POST` | `/api/tostadores`                | Tostador | Registra el negocio de la sesión. Body: `{"nombre", "nombreContacto", "telefono", "correo", "descripcion", "canton", "provincia", "estilos": ["CLARO"], "portadaUrl"}`. 201; si la cuenta ya tiene negocio: 409 |
| `GET`  | `/api/tostadores/actual`         | Tostador | Negocio de la sesión. 404 si todavía no registró uno                                                                                                                                    |
| `GET`  | `/api/tostadores/{id}`           | Sesión  | Negocio de tueste: `{id, cuentaId, nombre, nombreContacto, telefono, correo, descripcion, canton, provincia, estilos, portadaUrl}`. 404 si no existe o está eliminado                     |
| `PATCH`| `/api/tostadores/{id}`           | Tostador | Edita el negocio propio con un body parcial; `estilos` reemplaza la lista y `descripcion`/`portadaUrl` en blanco se borran. 403 si el negocio es de otra cuenta                         |

La sesión es un JWT firmado (HS256) en la cookie `cranhua_sesion` (HttpOnly, Secure, SameSite=Strict). El token trae el id de la cuenta en `sub`, todos sus roles en `roles`, la versión de sesión de la cuenta en `version_sesion` (restablecer la contraseña la sube y deja sin efecto los tokens anteriores) y vence a las 8 horas (`app.sesion.duracion`); la cookie vence al mismo tiempo. Registro e inicio de sesión la crean, cerrar sesión la borra. Las rutas que piden sesión responden 401 si no hay cookie o el token no es válido. Desde Angular las peticiones tienen que ir con `withCredentials: true` para que el navegador mande la cookie.

### Acceso por rol

- Sin sesión solo se puede usar `/api/auth/**`, `/api/publico/**` y `GET /actuator/health`. Todo lo demás pide sesión.
- Cada endpoint protegido declara con `@PreAuthorize` qué roles lo pueden usar, por ejemplo `@PreAuthorize("hasRole('TOSTADOR')")`. Si lo puede usar cualquier cuenta: `@PreAuthorize("hasAnyRole('CAFETALERO', 'TOSTADOR', 'CAFETERIA')")`.
- Sin sesión o con un token inválido: 401. Con sesión pero sin el rol que pide el endpoint: 403.
- Los roles salen del claim `roles` del token, así que una cuenta con varios roles puede usar los endpoints de cada uno.
- CORS solo acepta el origen del frontend, `http://localhost:4200` (propiedad `app.cors.allowed-origins` en `application.yml`). Un preflight desde otro origen recibe 403.

## Tests y formato

```bash
./mvnw verify
```

Los tests levantan con Testcontainers un contenedor `postgis/postgis:17-3.5`, así que Docker Desktop tiene que estar abierto. No necesitan `.env`.

El CI revisa el formato con google-java-format. Para no fallar ese check, instalar el plugin _google-java-format_ en IntelliJ y activarlo en _Settings > google-java-format Settings_.

## Flujo de trabajo

1. Crear un branch desde `develop` con el código del ticket de Jira (por ejemplo `feature/CP3-9-readme`).
2. Abrir el PR hacia `develop` usando la plantilla. Los checks `Format` y `Build` tienen que pasar y otro integrante tiene que aprobarlo.
3. Al cerrar el sprint, `develop` pasa a `main` con el workflow _Promote to Main_ en GitHub Actions.

No hacer commits directos a `develop` ni a `main`.

## Problemas comunes

- **`Falta POSTGRES_DB en .env`** (u otra variable): no existe el `.env` o el comando se está corriendo fuera de la raíz del repo.
- **`port is already allocated`** o `address already in use`: hay otro Postgres o backend usando el puerto. Cambiar `DB_PORT` o `BACKEND_PORT` en `.env`, o apagar el otro proceso.
- **`Cannot connect to the Docker daemon`**: Docker Desktop no está abierto.
- **`Validate failed: Migrations have failed validation`**: se editó una migración que ya estaba aplicada en la base local. Volver a dejar el archivo como estaba, o borrar la base con `docker compose down -v` si son solo datos de prueba.
- **El backend local no conecta a la base**: confirmar con `docker compose ps` que `cranhua-db` está `healthy` y que `DB_PORT` coincide.
