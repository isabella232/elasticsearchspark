/**
 * A sample streaming application which indexes tweets live into elastic search
 */

package com.holdenkarau.esspark

// Scala imports
import scala.collection.JavaConversions._
// Spark imports
import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
// ES imports
import org.elasticsearch.hadoop.mr.EsOutputFormat
import org.elasticsearch.hadoop.mr.EsInputFormat
import org.elasticsearch.hadoop.cfg.ConfigurationOptions
// sqlcontext
import org.apache.spark.sql._
import org.elasticsearch.spark.sql._
// Hadoop imports
import org.apache.hadoop.mapred.{FileOutputCommitter, FileOutputFormat, JobConf, OutputFormat}
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.{MapWritable, Text, NullWritable}
// Twitter imports
import twitter4j.TwitterFactory

import concurrent._
import ExecutionContext.Implicits._
import duration._


object ReIndexTweets {

  def main(args: Array[String]) {
    
    if (args.length < 5) {
      System.err.println("Usage ReIndexTweets <master> <key> <secret key> <access token> <access token secret>  <es-resource> [es-nodes]")
    }
    val Array(master, consumerKey, consumerSecret, accessToken, accessTokenSecret, esResource) = args.take(6)
    val esNodes = args.length match {
        case x: Int if x > 6 => args(6)
        case _ => "localhost"
    }

    SharedIndex.setupTwitter(consumerKey, consumerSecret, accessToken, accessTokenSecret)

    /**
     * SparkConf 
     * Added: setAppName and set properties
     * Changed: setMaster (testing)
     */
    val conf = new SparkConf
    // Using setMaster explicitly (not from args(0)). 
    conf.setMaster("local[4]")
    conf.setAppName("IndexTweetsLive")
    conf.set("spark.executor.memory","2g")
    
    conf.set("es.nodes", "localhost")
    conf.set("es.port", "9200")
    //conf.set("es.index.auto.create","true")
    
    // Create new fields on index automatically
    conf.set("index.mapper.dynamic","true")
    
    val sc = new SparkContext(conf)
    
    val jobConf = SharedESConfig.setupEsOnSparkContext(sc, esResource, Some(esNodes))
    // RDD of input
    val currentTweets = sc.hadoopRDD(jobConf, classOf[EsInputFormat[Object, MapWritable]], classOf[Object], classOf[MapWritable])
    // Extract only the map
    // Convert the MapWritable[Text, Text] to Map[String, String]
    val tweets = currentTweets.map{ case (key, value) => SharedIndex.mapWritableToInput(value) }
    println(tweets.take(5).mkString(":"))
    var rateLimit = 0
    var deadline = 15.minutes.fromNow
    val tweet4jtweets = tweets.sample(false, 0.01).flatMap{ tweet =>
      //rateLimit += 1
      //if (rateLimit >= 150) {println("panda Waiting for Twitter rate limits to be pulled: go grab coffee!"); Thread.sleep(deadline.timeLeft.toMillis); rateLimit = 0; deadline = 16.minutes.fromNow; }
        try {
          val twitter = TwitterFactory.getSingleton()
          val tweetID = tweet.getOrElse("docid", "")
          Option(twitter.showStatus(tweetID.toLong))
        } catch {
          case e : Exception => {
            println("Failed fetching a tweet, skipping "+e)
            None
          }
    }}
    tweet4jtweets.cache()
    println("Updating "+tweet4jtweets.count())
    // Old way
    //tweet4jtweets.map(SharedIndex.prepareTweets).saveAsHadoopDataset(jobConf)
    // New way
    val sqlCtx = new SQLContext(sc)
    import sqlCtx.createSchemaRDD
    val tweetsAsCS = createSchemaRDD(tweet4jtweets.map(SharedIndex.prepareTweetsCaseClass)) 
    println("Saving")
    tweetsAsCS.saveToEs("twitter/tweet")
    println("Saved")
  }
}
