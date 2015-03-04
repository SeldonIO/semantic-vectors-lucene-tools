Lucene Tool for Semantic Vectors
================================

Allows creation of a Lucene index from meta data about items within a Seldon MySQL Database (e.g. movies, articles).
The resulting index can be used with the Semantic Vectors [https://code.google.com/p/semanticvectors/](https://code.google.com/p/semanticvectors/) tool to create a Semantic Vectors database that can be used for item similarity.

The main use case is at present to call the library as below:

```
java -cp target/semvec-lucene-tools-1.2-jar-with-dependencies.jar io.seldon.semvec.CreateLuceneIndexFromDb -l <lucene_folder> -raw-ids -use-item-attrs -attr-names <attr_names> -recreate -item-limit <item_limit> -jdbc <JDBC>
```

 * ```<lucene_folder>``` : the folder in which to recreate the lucene index
 * ```<attr_names>``` : the list of attr names to use to get meta data
 * ```<item_limit>``` : only get these number of items from the items table
 * ```<jdbc>``` : the JDBC for the database holding the Seldon meta data for items

There is also code to allow:

 * Use of OpenNLP [https://opennlp.apache.org/](https://opennlp.apache.org/) entity name extraction.
 * Remove HTML syntax.
 * Code to output in a format for use in [Yahoo LDA](https://github.com/shravanmn/Yahoo_LDA)

## Caveats

At present the code is specific to a MySQL version of the Seldon database. Eventually, the code could be made more generally useful by allowing interfacing to a general datastore for meta data that is not Seldon specific.

A cut down schema for the 4 tables needed is in schema-minimal.sql, this contains

 * items : a table which provides ids and names to each document
 * item_attr : a list of attributes for each document
 * item_map_varchar : a table to hold varchar attributes (text < 256 characters)
 * item_map_text : a table to hold large text attributes

## Example Use Case

The examples folder has some simple examples. First build the project with Maven:

```
mvn -DskipTests=true clean package
```

The examples have the following dependencies:

 * Internet connection
 * MySQL
 * csvtool

Then go into the examples folder and run:

 * ```./create_wiki_film_db.sh``` : this will download film abstracts from dbpedia and populate a mysql database. You will need to edit the mysql settings for your local setup. 

Then you can run:

 * ```./create_basic_index.sh``` : this will create a lucene index from the film abstracts ; create semantic vectors dbs from this and run an example query.
 * ```./create_ner_index.sh``` : this will download openNLP models for Person name entity extract ; create a lucene index with names extracted and connected by underscore ; build semantic vectors dbs and run an example query.

## License

This project is licensed under the Apache 2 license. See LICENSE.txt. 



