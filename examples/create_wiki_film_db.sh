#!/bin/bash

set -o nounset
set -o errexit

STARTUP_DIR="$( cd "$( dirname "$0" )" && pwd )"

source ./common_vars.sh

wget http://web.informatik.uni-mannheim.de/DBpediaAsTables/csv/Film.csv.gz

gunzip Film.csv.gz

echo "create database ${DB_NAME}" | mysql -u${MYSQL_USER} -p${MYSQL_PASS} -h${MYSQL_HOST}
mysql -u${MYSQL_USER} -p${MYSQL_PASS} -h${MYSQL_HOST} ${DB_NAME} < ../schema-minimal.sql
echo "insert into item_attr values (1,'film_abstract','VARCHAR',1);" | mysql -u${MYSQL_USER} -p${MYSQL_PASS} -h${MYSQL_HOST} ${DB_NAME}
echo "add film ids and titles"
cat Film.csv | ${STARTUP_DIR}/extract-csv-column -c 2 | awk '{i=i+1;gsub("\"","", $0);;printf("insert into items values (%d,\"%s\",1);\n",i,$0)}' | mysql -u${MYSQL_USER} -p${MYSQL_PASS} -h${MYSQL_HOST} ${DB_NAME}
echo "add film abstracts"
cat Film.csv | ${STARTUP_DIR}/extract-csv-column -c 3 | awk '{i=i+1;gsub("\"","", $0);;printf("insert into item_map_text values (%d,1,\"%s\");\n",i,$0)}' | mysql -u${MYSQL_USER} -p${MYSQL_PASS} -h${MYSQL_HOST} ${DB_NAME}

