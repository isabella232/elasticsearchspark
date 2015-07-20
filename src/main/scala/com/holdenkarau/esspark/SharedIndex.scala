/**
 * Shared utilities to convert a tweet to something we want to index
 */

package com.holdenkarau.esspark

// Scala imports
import scala.collection.JavaConversions._
import scala.collection.immutable.HashMap
// Hadoop imports
import org.apache.hadoop.io.{MapWritable, Text, NullWritable}
// twitter imports
import twitter4j.Status
import twitter4j.TwitterFactory
//import java.util.Date
//import java.sql.Date




/**
 * New case class (including source attribution field).
 *                                                    
 */
case class tweetCS(docid: String, createdat: java.sql.Timestamp, message: String, hashTags: String, source: String, location: Option[String])
//case class tweetCS(docid: String, createdat: java.sql.Timestamp, message: String, hashTags: String, location: Option[String])

case class reindexedtweetCS(docid: String, message: String, source: String, hashTags: String, location: Option[String])

object SharedIndex {
  // twitter helper methods
  def prepareTweets(tweet: twitter4j.Status) = {
    println("panda preparing tweet!")
    val fields = tweet.getGeoLocation() match {
        case null => HashMap(
          "docid" -> tweet.getId().toString,
          "message" -> tweet.getText(),
          "hashTags" -> tweet.getHashtagEntities().map(_.getText()).mkString(" ")
        )
        case loc => {
          val lat = loc.getLatitude()
          val lon = loc.getLongitude()
          HashMap(
            "docid" -> tweet.getId().toString,
            "message" -> tweet.getText(),
            "hashTags" -> tweet.getHashtagEntities().map(_.getText()).mkString(" "),
            "location" -> s"$lat,$lon"
          )
        }
      }
    val output = mapToOutput(fields)
    println("panda tweet run: " + fields)
    output    
  }
  
  def prepareTweetsCaseClass(tweet: twitter4j.Status) = {
    
    
    val x = tweet.getCreatedAt()
    val y = new java.sql.Timestamp(x.getTime())
    
    var source = tweet.getSource().toString()
    var sourcepass = ""
    
    if(source matches ".*Android.*") {sourcepass = "mobile";} 
    else if (source matches ".*iPhone.*") sourcepass = "mobile"
    else sourcepass = "web"
    
    tweetCS(tweet.getId().toString, y, tweet.getText(),
      tweet.getHashtagEntities().map(_.getText()).mkString(" "),      
      sourcepass,
      tweet.getGeoLocation() match {
        case null => None
        case loc => {
          val lat = loc.getLatitude()
          val lon = loc.getLongitude()
          Some(s"$lat,$lon")
        }
      }
    )
  }
  

  def setupTwitter(consumerKey: String, consumerSecret: String, accessToken: String, accessTokenSecret: String) ={
    // Set up the system properties for twitter
    System.setProperty("twitter4j.oauth.consumerKey", consumerKey)
    System.setProperty("twitter4j.oauth.consumerSecret", consumerSecret)
    System.setProperty("twitter4j.oauth.accessToken", accessToken)
    System.setProperty("twitter4j.oauth.accessTokenSecret", accessTokenSecret)
    // https:  all kinds of fun
    System.setProperty("twitter4j.restBaseURL", "https://api.twitter.com/1.1/")
    System.setProperty("twitter4j.streamBaseURL", "https://stream.twitter.com/1.1/")
    System.setProperty("twitter4j.siteStreamBaseURL", "https://sitestream.twitter.com/1.1/")
    System.setProperty("twitter4j.userStreamBaseURL", "https://userstream.twitter.com/1.1/")
    System.setProperty("twitter4j.oauth.requestTokenURL", "https://api.twitter.com/oauth/request_token")
    System.setProperty("twitter4j.oauth.accessTokenURL", "https://api.twitter.com/oauth/access_token")
    System.setProperty("twitter4j.oauth.authorizationURL", "https://api.twitter.com/oauth/authorize")
    System.setProperty("twitter4j.oauth.authenticationURL", "https://api.twitter.com/oauth/authenticate")
  }

  def fetchTweets(ids: Seq[String]) = {
    val twitter = new TwitterFactory().getInstance();
  }
  // hadoop helper methods
  def mapToOutput(in: Map[String, String]): (Object, Object) = {
    val m = new MapWritable
    for ((k, v) <- in)
      m.put(new Text(k), new Text(v))
    (NullWritable.get, m)
  }
  def mapWritableToInput(in: MapWritable): Map[String, String] = {
    in.map{case (k, v) => (k.toString, v.toString)}.toMap
  }

}
