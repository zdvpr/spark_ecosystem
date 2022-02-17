DROP DATABASE IF EXISTS otus;
CREATE DATABASE otus;

CREATE TABLE news_all (
	id integer,	
	source varchar(50),
    title varchar(300),
    full_text text,
    publication_date varchar
);

CREATE TABLE words (
	news_id integer,	
	word varchar(50),
	publication_uts bigint,
	dwh_uts bigint
);