create extension if not exists postgis;

create table finca (
    id uuid primary key,
    nombre varchar(120) not null,
    descripcion varchar(300),
    zona_cafetalera varchar(30),
    perimetro geometry(Polygon, 4326) not null,
    ubicacion geometry(Point, 4326) not null,
    canton varchar(60) not null,
    provincia varchar(30) not null,
    eliminada boolean not null default false,
    eliminada_en timestamp with time zone,
    creada_en timestamp with time zone not null,
    constraint ck_finca_zona_cafetalera check (zona_cafetalera in (
        'Tarrazú', 'Valle Central', 'Valle Occidental', 'Tres Ríos',
        'Orosi', 'Turrialba', 'Brunca', 'Guanacaste'))
);

create index idx_finca_perimetro on finca using gist (perimetro);

create table finca_dueno (
    finca_id uuid not null,
    cuenta_id uuid not null,
    constraint pk_finca_dueno primary key (finca_id, cuenta_id),
    constraint fk_finca_dueno_finca foreign key (finca_id) references finca (id),
    constraint fk_finca_dueno_cuenta foreign key (cuenta_id) references cuenta (id)
);

-- Sin on delete cascade: la finca solo se borra lógicamente y sus cosechas se conservan
create table cosecha (
    id uuid primary key,
    finca_id uuid not null,
    variedad varchar(60) not null,
    altura_msnm integer not null,
    fecha_inicio date not null,
    fecha_fin date not null,
    cantidad_cosechada_kg numeric(10, 2) not null,
    peso_tras_beneficiado_kg numeric(10, 2),
    peso_tras_seleccion_kg numeric(10, 2),
    caracolillo_kg numeric(10, 2),
    notas_sabor varchar(500),
    puntaje_sca numeric(5, 2),
    precio_kg numeric(12, 2) not null,
    kg_disponibles numeric(10, 2),
    estado varchar(20) not null,
    codigo_lote varchar(20),
    creada_en timestamp with time zone not null,
    constraint fk_cosecha_finca foreign key (finca_id) references finca (id),
    constraint uq_cosecha_codigo_lote unique (codigo_lote),
    constraint ck_cosecha_altura check (altura_msnm > 0),
    constraint ck_cosecha_fechas check (fecha_fin >= fecha_inicio),
    constraint ck_cosecha_cantidad check (cantidad_cosechada_kg > 0),
    constraint ck_cosecha_puntaje_sca check (puntaje_sca between 0 and 100),
    constraint ck_cosecha_precio check (precio_kg > 0),
    constraint ck_cosecha_kg_disponibles check (kg_disponibles >= 0),
    constraint ck_cosecha_estado check (estado in ('EN_BENEFICIADO', 'DISPONIBLE'))
);

create table cosecha_proceso (
    cosecha_id uuid not null,
    proceso varchar(20) not null,
    porcentaje integer not null,
    constraint pk_cosecha_proceso primary key (cosecha_id, proceso),
    constraint fk_cosecha_proceso_cosecha foreign key (cosecha_id) references cosecha (id),
    constraint ck_cosecha_proceso_proceso check (proceso in (
        'Honey', 'Lavado', 'Natural', 'Anaeróbico', 'Semi-lavado', 'Red honey', 'Yellow honey')),
    constraint ck_cosecha_proceso_porcentaje check (porcentaje between 1 and 100)
);
