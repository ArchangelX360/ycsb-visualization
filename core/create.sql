DROP TABLE series_units;

CREATE TABLE series_units (
  id int(11) NOT NULL auto_increment PRIMARY KEY,
  operationType VARCHAR(56) NOT NULL, /* TODO : Foreign key */
  /*benchmark VARCHAR(255) NOT NULL,  TODO : Foreign key */
  time DOUBLE,
  latency DOUBLE,
  created_at DATETIME
);