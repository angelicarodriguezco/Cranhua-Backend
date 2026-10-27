BEGIN;

SELECT postgis_full_version() AS version_postgis;

CREATE TEMP TABLE finca_prueba (
  id_finca SERIAL PRIMARY KEY,
  nombre TEXT NOT NULL,
  poligono geometry(Polygon, 4326) NOT NULL
);

INSERT INTO finca_prueba (nombre, poligono)
VALUES (
           'Finca de prueba',
           ST_GeomFromText(
                   'POLYGON((-84.030 9.660, -84.020 9.660, -84.020 9.670, -84.030 9.670, -84.030 9.660))',
                   4326
           )
       );

SELECT
    nombre,
    ST_IsValid(poligono) AS es_valido,
    round((ST_Area(poligono::geography) / 10000)::numeric, 2) AS area_hectareas,
    ST_Contains(poligono, ST_SetSRID(ST_MakePoint(-84.025, 9.665), 4326)) AS contiene_punto_central,
    ST_AsGeoJSON(poligono) AS geojson
FROM finca_prueba;

ROLLBACK;