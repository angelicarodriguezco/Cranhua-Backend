-- Sin on delete cascade: la cafetería solo se borra lógicamente y conserva su menú
create table cafeteria (
    id uuid primary key,
    cuenta_id uuid not null,
    nombre varchar(120) not null,
    nombre_contacto varchar(120) not null,
    telefono varchar(30) not null,
    correo varchar(160) not null,
    descripcion varchar(300),
    horario varchar(100),
    ubicacion geometry(Point, 4326) not null,
    canton varchar(60) not null,
    provincia varchar(30) not null,
    volumen_mensual varchar(15),
    portada_url varchar(500),
    eliminada boolean not null default false,
    eliminada_en timestamp with time zone,
    creada_en timestamp with time zone not null,
    constraint fk_cafeteria_cuenta foreign key (cuenta_id) references cuenta (id),
    constraint ck_cafeteria_volumen_mensual check (volumen_mensual in (
        'MENOS_DE_10', 'DE_10_A_25', 'DE_25_A_50', 'DE_50_A_100', 'MAS_DE_100'))
);

-- Una sola cafetería activa por cuenta; las eliminadas no cuentan
create unique index uq_cafeteria_cuenta_activa on cafeteria (cuenta_id) where not eliminada;

create table cafeteria_estilo (
    cafeteria_id uuid not null,
    estilo varchar(10) not null,
    constraint pk_cafeteria_estilo primary key (cafeteria_id, estilo),
    constraint fk_cafeteria_estilo_cafeteria foreign key (cafeteria_id) references cafeteria (id),
    constraint ck_cafeteria_estilo_estilo check (estilo in ('CLARO', 'MEDIO', 'OSCURO'))
);

create table item_menu (
    id uuid primary key,
    cafeteria_id uuid not null,
    nombre varchar(120) not null,
    categoria varchar(15) not null,
    codigo_trazabilidad varchar(30),
    precio numeric(10, 2) not null,
    creado_en timestamp with time zone not null,
    constraint fk_item_menu_cafeteria foreign key (cafeteria_id) references cafeteria (id),
    constraint ck_item_menu_categoria check (categoria in ('CAFE', 'OTRA_COMIDA')),
    constraint ck_item_menu_trazabilidad check (categoria = 'CAFE' or codigo_trazabilidad is null),
    constraint ck_item_menu_precio check (precio > 0)
);

create index idx_item_menu_cafeteria on item_menu (cafeteria_id);

-- Los códigos de trazabilidad salen de varias tablas (hoy la cosecha; luego compras y tuestes)
-- y una FK solo apunta a una. Cada migración que agregue códigos reemplaza esta función con
-- create or replace sumando su tabla. Basta con validar al guardar: los lotes nunca se borran.
create function codigo_trazabilidad_existe(codigo varchar) returns boolean
    language sql stable
    as $$ select exists (select 1 from cosecha where codigo_lote = codigo) $$;

create function validar_codigo_trazabilidad_item_menu() returns trigger
    language plpgsql
    as $$
begin
    if new.codigo_trazabilidad is not null
            and not codigo_trazabilidad_existe(new.codigo_trazabilidad) then
        raise foreign_key_violation using
            message = 'No existe un lote con el código ' || new.codigo_trazabilidad,
            constraint = 'fk_item_menu_codigo_trazabilidad';
    end if;
    return new;
end
$$;

create trigger trg_item_menu_codigo_trazabilidad
    before insert or update of codigo_trazabilidad on item_menu
    for each row execute function validar_codigo_trazabilidad_item_menu();
