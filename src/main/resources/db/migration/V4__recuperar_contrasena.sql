alter table cuenta add column version_sesion integer not null default 0;

create table codigo_recuperacion (
    cuenta_id uuid primary key,
    codigo_hash varchar(100) not null,
    intentos_fallidos integer not null default 0,
    vence_en timestamp with time zone not null,
    constraint fk_codigo_recuperacion_cuenta foreign key (cuenta_id) references cuenta (id)
);
