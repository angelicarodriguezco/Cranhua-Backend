create table pedido (
                        id uuid primary key,
                        cafeteria_id uuid not null,
                        tueste_id uuid not null,
                        cantidad_kg numeric(10, 2) not null,
                        estado varchar(20) not null,
                        pedido_en timestamp with time zone not null,
                        cerrado_en timestamp with time zone,
                        constraint fk_pedido_cafeteria foreign key (cafeteria_id) references cafeteria (id),
                        constraint fk_pedido_tueste foreign key (tueste_id) references tueste (id),
                        constraint ck_pedido_cantidad check (cantidad_kg > 0),
                        constraint ck_pedido_estado check (estado in ('EN_CAMINO', 'ENTREGADO', 'CANCELADO')),
                        constraint ck_pedido_cerrado check ((estado = 'EN_CAMINO') = (cerrado_en is null))
);

create index idx_pedido_cafeteria on pedido (cafeteria_id);
create index idx_pedido_tueste on pedido (tueste_id);