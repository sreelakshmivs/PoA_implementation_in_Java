drop database projectdb;
drop user sree;
create user sree with password 'password';
create database projectdb with template=template0 owner=sree;
\connect projectdb;
alter default privileges grant all on tables to sree;
alter default privileges grant all on sequences to sree;

create table poa(
id integer primary key not null,
poa text not null
);

create sequence poa_seq increment 1 start 1;
