create table LINKS_ALREADY_PROCESSED
(
    link varchar(2000)
);
create table LINKS_TO_BE_PROCESSED
(
    link varchar(2000)
);
create table NEWS
(
    id          bigint primary key auto_increment,
    title       text,
    content     text,
    url         varchar(2000),
    CREATED_AT  timestamp default now(),
    MODIFIED_AT timestamp default now()
)DEFAULT CHARSET=utf8mb4;