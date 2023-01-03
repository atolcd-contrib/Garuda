package modules.exporter.postgresql

import java.sql.SQLException

import models.tweet.{Place, Tweet, User}
import play.api.Logging

/**
 * Inserts tweets in the current schema
 *
 * @param postgresConfiguration, the configuration of the PostgreSQL database
 */
class PostgresInsertion(postgresConfiguration: PostgresConfiguration) extends Logging {
	private val postgresDao = new PostgresDao(postgresConfiguration)
	postgresDao.setupCollect()
	private var multiInsertionsPostgres = postgresDao.getMultiInsertions
	
	private var tweetIdInBatch = List[String]()
	private var userIdInBatch = List[String]()
	private var placeIdInBatch = List[String]()
	private var cpt: Int = 0
	private val MAX_SIZE_BATCH: Int = 100
	private var lastTimeInsert: Long = System.currentTimeMillis()
	private val MAX_TIME_BATCH: Long = 60000L
	
	def close(): Unit = {
		postgresDao.close()
	}
	
	/**
	 * Insert the given line representing a tweet in PostgreSQL.
	 *
	 * @param line the tweet added, String format
	 */
	def insertLine(line: String): Unit = {
		var tweet: Option[Tweet] = None
		var tweetId: Option[String] = None
		try {
			tweet = Some(new Tweet(line))
			tweetId = Some(tweet.get.id.get)
		} catch {
			case t: Throwable => logger.warn("Error : " + t)
		}
		if (tweet.isDefined && tweetId.isDefined && !tweetIdInBatch.contains(tweetId.get)) {
			// Tweet is not in the current batch
			cpt += 1
			insertTweet(tweet.get)
		}
		
		// Insert the batch if the size of the time is above the limit
		if (cpt > MAX_SIZE_BATCH || (System.currentTimeMillis() - lastTimeInsert) > MAX_TIME_BATCH) {
			insertBatch()
		}
	}
	
	def insertBatch(): Unit = {
		var retry = true
		while (retry) {
			retry = false
			try {
				postgresDao.executeBatch(multiInsertionsPostgres)
			} catch {
				case e: java.sql.BatchUpdateException => {
					val it = e.iterator()
					while (it.hasNext) {
						val exception = it.next()
						exception match {
							case e: SQLException if isPrimaryKeyException(e) => {}
							case e: SQLException if isDeadlockException(e) => {
								retry = true
								logger.warn(s"${e.getErrorCode} ${e.getSQLState} ${e.getLocalizedMessage}")
								logger.warn(s"Deadlock detected: retry the insert")
							}
							case e: SQLException => {
								logger.warn(s"${e.getErrorCode} ${e.getSQLState} ${e.getLocalizedMessage}")
								throw e
							}
						}
					}
				}
			}
		}
		
		tweetIdInBatch = List[String]()
		userIdInBatch = List[String]()
		placeIdInBatch = List[String]()
		
		multiInsertionsPostgres = postgresDao.getMultiInsertions
		cpt = 0
		lastTimeInsert = System.currentTimeMillis()
	}
	
	private def insertTweet(tweet: Tweet): Unit = {
		// Table Tweet
		postgresDao.addBatchTweet(multiInsertionsPostgres, tweet)
		tweetIdInBatch +:= tweet.id.get
		
		// Table User
		if (tweet.user.isDefined) {
			insertUser(tweet.user.get)
		}
		
		// Table Place
		for (place <- tweet.places) {
			if (place.id.isDefined) {
				insertPlace(place)
				
				// Table Tweet_place
				postgresDao.addBatchTweetPlace(multiInsertionsPostgres, tweet.id.get, place.id.get)
			}
		}
		
		// Table Reply
		if (tweet.inReplyToStatusId.isDefined) {
			postgresDao.addBatchReply(multiInsertionsPostgres, tweet.id.get, tweet.inReplyToStatusId.get, tweet.inReplyToUserId, tweet.inReplyToUserScreenName)
		}
		
		// Table Quote
		if (tweet.isQuote && !tweet.isRetweet) {
			if (tweet.quote.isDefined && tweet.quote.get.id.isDefined) {
				// Verify if tweet is already in batch
				if (!tweetIdInBatch.contains(tweet.quote.get.id.get)) {
					insertTweet(tweet.quote.get)
					
					if (tweet.quote.get.id.isDefined) {
						postgresDao.addBatchQuote(multiInsertionsPostgres, tweet.id.get, tweet.quote.get.id.get)
					}
				}
			}
		}
		
		// Table Retweet
		if (tweet.isRetweet) {
			if (tweet.retweet.isDefined && tweet.retweet.get.id.isDefined) {
				// Verify if tweet is already in batch
				if (!tweetIdInBatch.contains(tweet.retweet.get.id.get)) {
					insertTweet(tweet.retweet.get)
					
					if (tweet.retweet.get.id.isDefined) {
						postgresDao.addBatchRetweet(multiInsertionsPostgres, tweet.id.get, tweet.retweet.get.id.get)
					}
				}
			}
		}
		
		// Table Tweet_hashtag
		if (!tweet.hashtags.isEmpty) {
			postgresDao.addBatchTweetHashtags(multiInsertionsPostgres, tweet.id.get, tweet.hashtags)
		}
		
		// Table Tweet_url
		if (!tweet.urls.isEmpty) {
			postgresDao.addBatchTweetUrls(multiInsertionsPostgres, tweet.id.get, tweet.urls)
		}
		
		// Table Tweet_cashtag
		if (!tweet.cashtags.isEmpty) {
			postgresDao.addBatchTweetCashtags(multiInsertionsPostgres, tweet.id.get, tweet.cashtags)
		}
		
		// Table Tweet_media
		if (!tweet.medias.isEmpty) {
			postgresDao.addBatchTweetMedias(multiInsertionsPostgres, tweet.id.get, tweet.medias)
		}
		
		// Table Tweet_user_mention
		if (!tweet.userMentions.isEmpty) {
			postgresDao.addBatchTweetUserMentions(multiInsertionsPostgres, tweet.id.get, tweet.userMentions)
		}
		
		// Table Tweet_annotation
		if (!tweet.annotations.isEmpty) {
			postgresDao.addBatchAnnotation(multiInsertionsPostgres, tweet.id.get, tweet.annotations)
		}
	}
	
	private def insertUser(user: User): Unit = {
		// Verify if user is already in batch
		if (!userIdInBatch.contains(user.id.get)) {
			// Table User
			postgresDao.addBatchUser(multiInsertionsPostgres, user)
			userIdInBatch +:= user.id.get
			
			// Table Withheld_in_country
			if (!user.withheldInCountries.isEmpty) {
				postgresDao.addBatchUserWithheldInCountry(multiInsertionsPostgres, user.id.get, user.withheldInCountries)
			}
		}
	}
	
	private def insertPlace(place: Place): Unit = {
		// Verify if place is already in batch
		if (!placeIdInBatch.contains(place.id.get)) {
			// Table Place
			postgresDao.addBatchPlace(multiInsertionsPostgres, place)
			placeIdInBatch +:= place.id.get
		}
	}
	
	private def isPrimaryKeyException(e: SQLException): Boolean = {
		e.getSQLState == "23505" || e.getSQLState == "23500"
	}
	
	private def isDeadlockException(e: SQLException): Boolean = {
		e.getSQLState == "40P01"
	}
}


