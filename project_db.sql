drop database if exists subcontractor;
drop user if exists subcontractor;

create user subcontractor with password 'password';
create database subcontractor with template=template0 owner=subcontractor;
\connect subcontractor;
alter default privileges grant all on tables to subcontractor;
alter default privileges grant all on sequences to subcontractor;

create table poa(
    id integer primary key not null,
    destination_network_id text not null
);

create sequence poa_seq increment 1 start 1;
