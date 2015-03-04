#!/bin/bash

set -o nounset
set -o errexit

source ./common_vars.sh

#create lucene index
java -cp ../target/semvec-lucene-tools-1.2-jar-with-dependencies.jar io.seldon.semvec.CreateLuceneIndexFromDb -l wiki_film_index -raw-ids -use-item-attrs -attr-names film_abstract -recreate -item-limit 90000 -jdbc "jdbc:mysql://${MYSQL_HOST}:3306/${DB_NAME}?user=${MYSQL_USER}&password=${MYSQL_PASS}&characterEncoding=utf8"

#create SV index
java -cp ../target/semvec-lucene-tools-1.2-jar-with-dependencies.jar pitt.search.semanticvectors.BuildIndex -trainingcycles 1 -maxnonalphabetchars -1 -minfrequency 0 -maxfrequency 1000000 -luceneindexpath wiki_film_index

#example query - films similar to "Star Wars"
java -cp ../target/semvec-lucene-tools-1.2-jar-with-dependencies.jar pitt.search.semanticvectors.Search -queryvectorfile docvectors.bin -searchvectorfile docvectors.bin -matchcase `echo "select item_id from items where name='Star Wars (film)'" | mysql --skip-column-names -u${MYSQL_USER} -p${MYSQL_PASS} -h${MYSQL_HOST} ${DB_NAME}` | cut -d: -f2 | xargs -i echo "select name from items where item_id='{}';" | mysql --skip-column-names -u${MYSQL_USER} -p${MYSQL_PASS} -h${MYSQL_HOST} ${DB_NAME}






