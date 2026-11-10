-- Sin on delete cascade: el negocio solo se borra lógicamente y conserva sus lotes y códigos
create table negocio_tueste (
    id uuid primary key,
    cuenta_id uuid not null,
    nombre varchar(120) not null,
    nombre_contacto varchar(120) not null,
    telefono varchar(30) not null,
    correo varchar(160) not null,
    descripcion varchar(300),
    canton varchar(60) not null,
    provincia varchar(30) not null,
    portada_url varchar(500),
    eliminado boolean not null default false,
    eliminado_en timestamp with time zone,
    creado_en timestamp with time zone not null,
    constraint uq_negocio_tueste_cuenta unique (cuenta_id),
    constraint fk_negocio_tueste_cuenta foreign key (cuenta_id) references cuenta (id)
);

create table negocio_tueste_estilo (
    negocio_tueste_id uuid not null,
    estilo varchar(10) not null,
    constraint pk_negocio_tueste_estilo primary key (negocio_tueste_id, estilo),
    constraint fk_negocio_tueste_estilo_negocio foreign key (negocio_tueste_id)
        references negocio_tueste (id),
    constraint ck_negocio_tueste_estilo_estilo check (estilo in ('CLARO', 'MEDIO', 'OSCURO'))
);
