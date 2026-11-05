create table cuenta (
    id uuid primary key,
    nombre_completo varchar(120) not null,
    correo varchar(160) not null,
    telefono varchar(30),
    foto_url varchar(500),
    contrasena_hash varchar(100) not null,
    eliminada boolean not null default false,
    eliminada_en timestamp with time zone,
    creada_en timestamp with time zone not null,
    constraint uq_cuenta_correo unique (correo)
);

create table cuenta_rol (
    cuenta_id uuid not null,
    rol varchar(20) not null,
    constraint pk_cuenta_rol primary key (cuenta_id, rol),
    constraint fk_cuenta_rol_cuenta foreign key (cuenta_id) references cuenta (id),
    constraint ck_cuenta_rol_rol check (rol in ('CAFETALERO', 'TOSTADOR', 'CAFETERIA'))
);

create table cuenta_red_social (
    cuenta_id uuid not null,
    tipo varchar(20) not null,
    valor varchar(200) not null,
    constraint uq_cuenta_red_social_tipo unique (cuenta_id, tipo),
    constraint fk_cuenta_red_social_cuenta foreign key (cuenta_id) references cuenta (id),
    constraint ck_cuenta_red_social_tipo check (tipo in ('INSTAGRAM', 'WHATSAPP', 'FACEBOOK', 'SITIO_WEB'))
);
