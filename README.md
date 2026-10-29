# Crán̈hua

Proyecto de trazabilidad de café de especialidad para Costa Rica que conecta a cafetaleros, tostadores y cafeterías en un solo sistema, para que cualquier persona pueda ver de dónde salió el café que se está tomando con solo un código en la bolsa.

Este repositorio es el backend: Spring Boot 3.5 con Java 21, PostgreSQL 17 + PostGIS y Spring AI (Gemini). El frontend está en [Cranhua-Frontend](https://github.com/angelicarodriguezco/Cranhua-Frontend).

## Requisitos

- Git
- Docker Desktop abierto (incluye Docker Compose)
- JDK 21, solo si se va a correr el backend fuera de Docker. No hace falta instalar Maven, el repo trae `mvnw`.

## Variables de entorno

Copiar el archivo de ejemplo en la raíz del repo:

```bash
cp .env.example .env      # Git Bash / macOS / Linux
copy .env.example .env    # PowerShell / CMD
```

| Variable            | Para qué sirve                                                                                                                                       | Valor de ejemplo |
| ------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------- |
| `GEMINI_API_KEY`    | Llave de Google AI Studio que usa Spring AI. Docker Compose no arranca si está vacía. Con `pendiente` el backend levanta, pero `/api/ai/chat` falla. | `pendiente`      |
| `POSTGRES_DB`       | Nombre de la base de datos                                                                                                                           | `cranhua`        |
| `POSTGRES_USER`     | Usuario de la base de datos                                                                                                                          | `cranhua`        |
| `POSTGRES_PASSWORD` | Contraseña de la base de datos                                                                                                                       | `Cranhua123`     |
| `DB_PORT`           | Puerto de Postgres en la máquina. Cambiarlo (por ejemplo a `5433`) si ya hay otro Postgres usando el 5432.                                           | `5432`           |
| `BACKEND_PORT`      | Puerto del backend en la máquina cuando corre en Docker                                                                                              | `8080`           |

El `.env` está en `.gitignore`. No subirlo ni pegar llaves reales en issues o PRs. La llave de Gemini se saca en https://aistudio.google.com/apikey.

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

| Método | Ruta               | Acceso  | Descripción                                                                |
| ------ | ------------------ | ------- | -------------------------------------------------------------------------- |
| `GET`  | `/actuator/health` | Público | Estado del servicio                                                        |
| `POST` | `/api/ai/chat`     | Público | Chat con Gemini. Body: `{"message": "..."}`. Respuesta: `{"reply": "..."}` |

Cualquier otra ruta pide autenticación y devuelve 401 por ahora. CORS permite `http://localhost:4200` (propiedad `app.cors.allowed-origins` en `application.yml`).

## Tests y formato

```bash
./mvnw verify
```

Los tests usan H2 en memoria con el perfil `test`, así que no necesitan Docker ni `.env`.

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
- **El backend local no conecta a la base**: confirmar con `docker compose ps` que `cranhua-db` está `healthy` y que `DB_PORT` coincide.
