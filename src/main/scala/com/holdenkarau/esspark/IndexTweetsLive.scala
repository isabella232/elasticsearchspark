/**
 * A sample streaming application which indexes tweets live into elastic search
 */

package com.holdenkarau.esspark

import org.apache.spark.streaming.{Seconds, StreamingContext}
import StreamingContext._
import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.streaming.twitter._
import org.apache.spark.SparkConf
import org.apache.spark.storage.StorageLevel
import org.elasticsearch.hadoop.mr.EsOutputFormat
import org.elasticsearch.hadoop.cfg.ConfigurationOptions;
// sqlcontext
import org.elasticsearch.spark._
import org.apache.spark.sql._
import org.elasticsearch.spark.sql._
// Hadoop imports
import org.apache.hadoop.mapred.{FileOutputCommitter, FileOutputFormat, JobConf, OutputFormat}
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.{MapWritable, Text, NullWritable}



object IndexTweetsLive {
  def main(args: Array[String]) {
   
    if (args.length < 5) {
      System.err.println("Usage IndexTweetsLive <master> <key> <secret key> <access token> <access token secret>  <es-resource> [es-nodes]")
    }
    val Array(master, consumerKey, consumerSecret, accessToken, accessTokenSecret, esResource) = args.take(6)
    val esNodes = args.length match {
        case x: Int if x > 6 => args(6)
        case _ => "localhost"
    }

    SharedIndex.setupTwitter(consumerKey, consumerSecret, accessToken, accessTokenSecret)

    /**
     * SparkConf not created here, so copied from Ref (see link below).
     * Ref: https://spark.apache.org/docs/latest/streaming-programming-guide.html
     * Added: setAppName and set properties
     * Changed: setMaster (testing)  
     */
    
    val conf = new SparkConf
    conf.setMaster("local[4]")
    conf.setAppName("IndexTweetsLive")
    
    conf.set("es.nodes", "localhost")
    conf.set("es.port", "9200")
    //conf.set("es.index.auto.create","false")
    conf.set("index.mapper.dynamic","true")   
    //conf.set("spark.executor.memory","2g")

    val ssc = new StreamingContext(conf, Seconds(1))

    val tweets = TwitterUtils.createStream(ssc, None)    
    tweets.print()
    // Old way
    /**
    tweets.foreachRDD{(tweetRDD, time) =>
      val sc = tweetRDD.context
      val jobConf = SharedESConfig.setupEsOnSparkContext(sc, esResource, Some(esNodes))
      val tweetsAsMap = tweetRDD.map(SharedIndex.prepareTweets)
      tweetsAsMap.saveAsHadoopDataset(jobConf)
    }
    **/      
    
    // New fancy way
    tweets.foreachRDD{(tweetRDD, time) =>           
      val sc = tweetRDD.context
      tweetRDD.persist(StorageLevel.MEMORY_ONLY)
      val sqlCtx = new SQLContext(sc)
      import sqlCtx.createSchemaRDD
      val tweetsAsCS = createSchemaRDD(tweetRDD.map(SharedIndex.prepareTweetsCaseClass))            
      tweetsAsCS.saveToEs(esResource)
    }
    
    println("pandas: sscstart")
    ssc.start()
    println("pandas: awaittermination")
    ssc.awaitTermination()
    println("pandas: done!")
  }
}