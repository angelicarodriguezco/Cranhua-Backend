alter table cosecha
    add eliminada boolean not null default false,
    add eliminada_en timestamptz;

create index idx_cosecha_finca_activa on cosecha (finca_id) where not eliminada;
