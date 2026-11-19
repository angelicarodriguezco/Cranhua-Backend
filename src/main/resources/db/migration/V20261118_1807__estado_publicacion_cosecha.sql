alter table cosecha drop constraint ck_cosecha_estado;

update cosecha
set estado = case
    when estado = 'EN_BENEFICIADO' then 'BORRADOR'
    when estado = 'DISPONIBLE' and kg_disponibles > 0 then 'PUBLICADA'
    when estado = 'DISPONIBLE' then 'AGOTADA'
    else estado
end;

alter table cosecha
    add constraint ck_cosecha_estado check (estado in ('BORRADOR', 'PUBLICADA', 'AGOTADA'));
