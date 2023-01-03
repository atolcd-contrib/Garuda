package models.tweet

import java.time.format.DateTimeFormatter

import com.twitter.clientlib.model.{User => TUser}

class User(private val tweetUser: TUser) {
	import scala.jdk.CollectionConverters._
	
	lazy val id: Option[String] = Option(tweetUser.getId)
	lazy val screenName: Option[String] = Option(tweetUser.getUsername)
	lazy val name: Option[String] = Option(tweetUser.getName)
	
	lazy val location: Option[String] = Option(tweetUser.getLocation)
	lazy val url: Option[String] = Option(tweetUser.getUrl)
	lazy val description: Option[String] = Option(tweetUser.getDescription)
	
	lazy val createdAt: Option[String] = Option(tweetUser.getCreatedAt.toString)
	lazy val pinnedTweetId: Option[String] = Option(tweetUser.getPinnedTweetId)
	lazy val profileImageUrl: Option[String] = Option(tweetUser.getProfileImageUrl.getPath)
	
	lazy val`protected`: Option[Boolean] = Option(tweetUser.getProtected)
	lazy val verified: Option[Boolean] = Option(tweetUser.getVerified)
	lazy val withheldInCountries: Array[String] = {
		if (tweetUser.getWithheld != null && tweetUser.getWithheld.getCountryCodes != null) {
			tweetUser.getWithheld.getCountryCodes.asScala.toArray
		} else {
			Array[String]()
		}
	}
	
	lazy val followersCount: Option[Int] = Option(tweetUser.getPublicMetrics.getFollowersCount)
	lazy val followingCount: Option[Int] = Option(tweetUser.getPublicMetrics.getFollowingCount)
	lazy val listedCount: Option[Int] = Option(tweetUser.getPublicMetrics.getListedCount)
	lazy val tweetsCount: Option[Int] = Option(tweetUser.getPublicMetrics.getTweetCount)
}
