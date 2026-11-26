-- Una cuenta con el negocio eliminado puede registrar otro; solo el activo es único
alter table negocio_tueste drop constraint uq_negocio_tueste_cuenta;
create unique index uq_negocio_tueste_cuenta_activo on negocio_tueste (cuenta_id) where not eliminado;

-- Los tuestes son de la cuenta, no del negocio: sin esta marca reaparecerían en el negocio nuevo
alter table tueste add column retirado boolean not null default false;
