elasticsearchspark
==================

Elastic Search on Spark

Introduction
============

This project is based on Holden Karau's 'elasticsearchspark' project. Holden's talk is available in YouTube [2] and his slides can be found at [3].

* [1] https://github.com/holdenk/elasticsearchspark
* [2] https://youtu.be/jYicnlunDQ0
* [3] https://spark-summit.org/2014/wp-content/uploads/2014/07/Streamlining-Search-Indexing-using-Elastic-Search-and-Spark-Holden-Karau.pdf

Notes:
======

Holden's 'build.sbt' uses '2.1.0.Beta2-holdenmagic' for 'elasticsearch-hadoop'. However, upon setup we found that this version no longer works, so we're using '2.1.0.Beta3' (NB: we tested with '2.1.0.Beta4' and stable but found those don't work either).

We also added some additional resolvers:

   * "clojars" at "https://clojars.org/repo",
   * "conjars" at "http://conjars.org/repo",
   
We're indexing createdAt and source, and we filter source to distinguish web/mobile.
      
Requirements:
=============

Here are instructions to get this working on Mac OS X (Mavericks).

1.  Scala sbt

Available here: http://www.scala-sbt.org/release/tutorial/Setup.html

Note: You'll need Java 1.7. In case you need to manage different versions of Java on your machine you can use jenv (see [4]).

[4] http://www.jenv.be

2.  Elasticsearch

	$ brew install elasticsearch

3.  Postman

Chrome app, install via Chrome App manager.

4.  Kibana

	$ brew install kibana

Setup:
======

1.  Clone repo

	$ git clone ssh://git@git.corp.xoom.com:7999/~ocastaneda/elasticsearchspark.git

2.  cd to project's main dir

	$ cd elasticsearchspark

3.  Run 'sbt'

	$ sbt

4.  Get Twitter credentials

Add Twitter app on dev.twitter.com and get the following:

- Key
- Secret Key
- Access Token
- Access Token Secret

5.  Run elasticsearchspark 

Usage: <master> <key> <secret key> <access token> <access token secret>  <es-resource>
	
Example:
	> run spark://racso.corp.xoom.com:7077 <key> <secret key> <access token> <access token secret> twitter/tweet
	
6.  Choose option to run

You will be presented with the following:

===>
Multiple main classes detected, select one to run:

 [1] com.holdenkarau.esspark.IndexTweetsLive
 [2] com.holdenkarau.esspark.ReIndexTweets
 [3] com.holdenkarau.esspark.TopTweetsInALocation
 [4] com.holdenkarau.esspark.TopTweetsInALocationWithSQL

Enter number: 
<===

Choose whichever option you'd like to run.

7.  Do a GET/POST on your index.

You can use Postman to do a GET on your index:

GET http://localhost:9200/twitter/tweet/_search

8.  Run Kibana

On your terminal get Kibana running

	$ kibana

Then:
- Access Kibana on localhost:5601 with you web browser. 
- Configure an index pattern on 'Settings'
- Go to discover to run a search against your index.
- Run visualizations against your index.



