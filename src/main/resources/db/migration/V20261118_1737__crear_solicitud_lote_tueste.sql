-- Sin on delete cascade: los borrados son lógicos y la trazabilidad se conserva
create table solicitud (
    id uuid primary key,
    cosecha_id uuid not null,
    tostador_id uuid not null,
    cantidad_kg numeric(10, 2) not null,
    precio_kg numeric(12, 2) not null,
    total_estimado numeric(14, 2) not null,
    estado varchar(20) not null,
    solicitada_en timestamp with time zone not null,
    respondida_en timestamp with time zone,
    constraint fk_solicitud_cosecha foreign key (cosecha_id) references cosecha (id),
    constraint fk_solicitud_tostador foreign key (tostador_id) references cuenta (id),
    constraint ck_solicitud_cantidad check (cantidad_kg > 0),
    constraint ck_solicitud_precio check (precio_kg > 0),
    constraint ck_solicitud_total check (total_estimado > 0),
    constraint ck_solicitud_estado check (estado in ('PENDIENTE', 'APROBADA', 'RECHAZADA')),
    constraint ck_solicitud_respondida check ((estado = 'PENDIENTE') = (respondida_en is null))
);

create index idx_solicitud_cosecha on solicitud (cosecha_id);
create index idx_solicitud_tostador on solicitud (tostador_id);

-- El número del código sale de la secuencia para que dos aprobaciones a la vez no lo repitan
create sequence lote_codigo_seq;

create table lote (
    id uuid primary key,
    solicitud_id uuid not null,
    codigo varchar(20) not null,
    estado_trazabilidad varchar(20) not null,
    activado_en timestamp with time zone,
    creado_en timestamp with time zone not null,
    constraint fk_lote_solicitud foreign key (solicitud_id) references solicitud (id),
    constraint uq_lote_solicitud unique (solicitud_id),
    constraint uq_lote_codigo unique (codigo),
    constraint ck_lote_codigo check (codigo ~ '^CRH-[0-9]{4}-[0-9]{4,}$'),
    constraint ck_lote_estado_trazabilidad check (estado_trazabilidad in ('PENDIENTE', 'ACTIVO'))
);

create table tueste (
    id uuid primary key,
    lote_id uuid not null,
    nivel varchar(10) not null,
    peso_verde_kg numeric(10, 2) not null,
    peso_tostado_kg numeric(10, 2) not null,
    fecha_tueste date not null,
    notas varchar(500),
    creado_en timestamp with time zone not null,
    constraint fk_tueste_lote foreign key (lote_id) references lote (id),
    constraint ck_tueste_nivel check (nivel in ('CLARO', 'MEDIO', 'OSCURO')),
    constraint ck_tueste_peso_verde check (peso_verde_kg > 0),
    constraint ck_tueste_peso_tostado check (peso_tostado_kg > 0),
    constraint ck_tueste_pesos check (peso_tostado_kg <= peso_verde_kg)
);

create index idx_tueste_lote on tueste (lote_id);
