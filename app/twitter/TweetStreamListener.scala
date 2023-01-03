package twitter

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

import scala.collection.mutable
import scala.util.{Failure, Success, Using}
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

import com.twitter.clientlib.ApiException
import models.Collect
import play.api.Logging

class TweetStreamListener(val streamingHandler: StreamingHandler[_], val collect: Collect) extends Logging {
	private val TIMEOUT_MILLIS: Int = 60000
	private val SLEEP_MILLIS: Int = 100
	private val BACKOFF_SLEEP_INTERVAL_MILLIS: Int = 5000
	
	private var tweetsQueuer: TweetsQueuer = new TweetsQueuer()
	private val tweetsListenersExecutor = new TweetsListenersExecutor()
	private val timeoutChecker = new StreamTimeoutChecker()
	
	private val tweetsQueue: mutable.Queue[String] = mutable.Queue[String]()
	private val isRunning: AtomicBoolean = new AtomicBoolean(true)
	private val tweetStreamedTime: AtomicLong = new AtomicLong(0)
	var caughtException: Option[Exception] = None
	private var reconnecting: Long = 0
	
	def stream(): StreamListenersExecutorBuilder = {
		new StreamListenersExecutorBuilder()
	}
	
	private def shutdown(e: Exception): Unit = {
		caughtException = Some(e)
		e.printStackTrace()
		shutdown()
	}
	
	def shutdown(): Unit = {
		isRunning.set(false)
		logger.info("TweetsStreamListenersExecutor is shutting down.")
	}
	
	sys.addShutdownHook({
		if (!isRunning.get()) {
			shutdown()
			tweetsQueuer.join()
			timeoutChecker.join()
			tweetsListenersExecutor.join()
		}
	})
	
	@throws(classOf[ApiException])
	private def connectStream(): InputStream = {
		streamingHandler.connectStream()
	}
	
	private def resetTweetStreamedTime(): Unit = {
		tweetStreamedTime.set(System.currentTimeMillis())
	}
	
	private def isTweetStreamedError: Boolean = {
		System.currentTimeMillis() - tweetStreamedTime.get() > TIMEOUT_MILLIS
	}
	
	private def restartTweetsQueuer(): Unit = {
		tweetsQueuer.shutdownQueuer()
		if (reconnecting < 7) {
			reconnecting += 1
		}
		try {
			logger.info("sleeping " + BACKOFF_SLEEP_INTERVAL_MILLIS * reconnecting)
			Thread.sleep(BACKOFF_SLEEP_INTERVAL_MILLIS * reconnecting) // Wait a bit before starting the TweetsQueuer and calling the API again.
		} catch {
			case e: InterruptedException => e.printStackTrace()
		}
		tweetsQueuer.interrupt()
		tweetsQueuer = new TweetsQueuer()
		tweetsQueuer.start()
	}
	
	private class TweetsListenersExecutor extends Thread {
		
		override def run(): Unit = {
			processTweets()
		}
		
		private def processTweets(): Unit = {
			try {
				while (isRunning.get()) {
					if (tweetsQueue.nonEmpty) {
						try {
							if(!streamingHandler.processAndVerifyStreamingObject(tweetsQueue.dequeue())) {
								restartTweetsQueuer()
							}
						} catch {
							case e: Exception => e.printStackTrace()
						}
					} else {
						Thread.sleep(SLEEP_MILLIS)
					}
				}
				// Process last tweets before stopping the thread
				while (tweetsQueue.nonEmpty) {
					streamingHandler.processAndVerifyStreamingObject(tweetsQueue.dequeue())
				}
			} catch {
				case e: Exception => shutdown(e)
			}
		}
	}
	
	private class TweetsQueuer extends Thread {
		private var isReconnecting: Boolean = false
		
		override def run(): Unit = {
			Using(new BufferedReader(new InputStreamReader(connectStream()))) { reader =>
				while (isRunning.get() && !isReconnecting) {
					val line = reader.readLine()
					resetTweetStreamedTime()
					if (line == null || line.isEmpty) {
						Thread.sleep(SLEEP_MILLIS)
					} else {
						tweetsQueue.enqueue(line)
					}
				}
			} match {
				case Failure(exception) =>
					exception match {
						case e: InterruptedException => e.printStackTrace()
						case e: Exception => shutdown(e)
					}
				case Success(_) =>
			}
		}
		
		def shutdownQueuer(): Unit = {
			isReconnecting = true
		}
	}
	
	private class StreamTimeoutChecker extends Thread {
		override def run(): Unit = {
			resetTweetStreamedTime()
			while (isRunning.get()) {
				if (isTweetStreamedError) {
					shutdown(new ApiException("Tweets are not streaming"))
				}
				try {
					Thread.sleep(SLEEP_MILLIS)
				} catch {
					case e: InterruptedException => e.printStackTrace()
				}
			}
		}
	}
	
	class StreamListenersExecutorBuilder {
		@throws(classOf[ApiException])
		def executeListeners(): Unit = {
			collect.isActive = true
			tweetsListenersExecutor.start()
			tweetsQueuer.start()
			timeoutChecker.start()
		}
	}
}