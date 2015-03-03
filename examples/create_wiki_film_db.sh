#!/bin/bash

set -o nounset
set -o errexit

MYSQL_USER=root

wget http://web.informatik.uni-mannheim.de/DBpediaAsTables/csv/Film.csv.gz

gunzip Film.csv.gz

echo "create database wiki_film" | mysql -u${MYSQL_USER}
mysql -uroot wiki_film < ../schema-minimal.sql
echo "insert into item_attr values (1,'film_abstract','VARCHAR',1);" | mysql -u${MYSQL_USER} wiki_film
echo "add film ids and titles"
cat Film.csv | csvtool -t ',' col 2 - | awk '{i=i+1;gsub("\"","", $0);;printf("insert into items values (%d,\"%s\",1);\n",i,$0)}' | mysql -u${MYSQL_USER} wiki_film
echo "add film abstracts"
cat Film.csv | csvtool -t ',' col 3 - | awk '{i=i+1;gsub("\"","", $0);;printf("insert into item_map_text values (%d,1,\"%s\");\n",i,$0)}' | mysql -u${MYSQL_USER} wiki_film








