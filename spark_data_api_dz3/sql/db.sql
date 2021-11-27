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

