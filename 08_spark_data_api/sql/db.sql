DROP DATABASE IF EXISTS otus;
CREATE DATABASE otus;

CREATE TABLE stat_by_hours (
distance_bucket       INT,  
count_trip            INT,
avg_trip              REAL,
stddev_trip           REAL,
min_trip              REAL,
max_trip              REAL
);

CREATE TABLE news_all (
    source varchar(50),
    title varchar(300),
    full_text text,
    publication_date timestamp,
    rubric varchar(50),
    subrubric varchar(50),
    tags varchar(50)
);