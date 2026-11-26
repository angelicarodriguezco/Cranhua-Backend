-- Los códigos de los lotes comprados también sirven en el menú. El parámetro va calificado
-- porque lote tiene una columna con el mismo nombre
create or replace function codigo_trazabilidad_existe(codigo varchar) returns boolean
    language sql stable
    as $$
        select exists (
                select 1 from cosecha where codigo_lote = codigo_trazabilidad_existe.codigo)
            or exists (select 1 from lote where lote.codigo = codigo_trazabilidad_existe.codigo)
    $$;

-- Dos ítems sin código y con el mismo nombre también se consideran repetidos
create unique index uq_item_menu_nombre_codigo
    on item_menu (cafeteria_id, lower(nombre), codigo_trazabilidad) nulls not distinct;
