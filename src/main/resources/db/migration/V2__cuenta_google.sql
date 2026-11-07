alter table cuenta alter column contrasena_hash drop not null;

alter table cuenta add column google_id varchar(255);

alter table cuenta add constraint uq_cuenta_google_id unique (google_id);

alter table cuenta add constraint ck_cuenta_acceso
    check (contrasena_hash is not null or google_id is not null);
