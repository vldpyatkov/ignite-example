# Ignite examples

## LoadDataStream

The example will load a data to the SQL table. The table may determine by script:

```
CREATE TABLE PUBLIC.TIMEDATA (
  OBJECT_ID BIGINT,
  PARAM_ID BIGINT,
  SERIES_TIME TIMESTAMP,
  VALUE DOUBLE,
  PERIOD_ID VARCHAR,
  ANALYTIC_ID VARCHAR,
  UNIT_ID BIGINT,
  CONSTRAINT timedata_pk PRIMARY KEY (OBJECT_ID,PARAM_ID,SERIES_TIME,PERIOD_ID,ANALYTIC_ID)
);
```

To launch the example:

```
java -cp LoadDataStream-1.0-SNAPSHOT.jar;$IGNITE_HOME\libs\*;$IGNITE_HOME\libs\ignite-spring\*;$IGNITE_HOME\libs\ignite-indexing\*;$IGNITE_HOME\libs\optional\ignite-log4j2\* org.example.Main cfg.xml data.csv
```

where

**$IGNITE_HOME** - path where your Ignite saved\
**cfg.xml** - Ignite configuration file (example LoadDataStream/cfg/cfg.xml)\
**data.csv** - file with data in csv format (example LoadDataStream/cfg/data.csv)\

After all data are loaded, the server will ready to work.

Parameters which are dependent on environment:

**cfg.xml**
_workDirectory_ - directory where database will persist\
_gridLogger_ - path to logger configuration

**ignite-log4j.xml**
_LogToRollingFile_ - path to log files
