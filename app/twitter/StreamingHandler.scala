package twitter

import java.io.InputStream

import com.twitter.clientlib.ApiException

trait StreamingHandler[T] {
	@throws(classOf[ApiException])
	def connectStream(): InputStream
	
	@throws(classOf[ApiException])
	def actionOnStreamingObject(tweetString: String, streamingTweet: T): Unit
	
	@throws(classOf[Exception])
	def getStreamingObject(tweetString: String): T
	
	def hasReconnectErrors(streamingTweet: T): Boolean
	
	@throws(classOf[Exception])
	def processAndVerifyStreamingObject(tweetString: String): Boolean = {
		val tweet: T = getStreamingObject(tweetString)
		actionOnStreamingObject(tweetString, tweet)
		!hasReconnectErrors(tweet)
	}
}
