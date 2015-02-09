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

## Example Use Case
The "tag similarity" recommender in [Seldon's Movie Recommender Demo](http://www.seldon.io/movie-demo/) uses this project to create the Semantic Vectors database to allow movie similarity to be calculated.

## License

This project is licensed under the Apache 2 license. See LICENSE.txt. 



