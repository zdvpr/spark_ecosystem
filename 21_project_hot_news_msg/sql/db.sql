DROP DATABASE IF EXISTS otus;
CREATE DATABASE otus;

CREATE TABLE news_all (
    source varchar(50),
    title varchar(300),
    full_text text,
    publication_date timestamp,
    rubric varchar(50),
    subrubric varchar(50),
    tags varchar(50)
);

