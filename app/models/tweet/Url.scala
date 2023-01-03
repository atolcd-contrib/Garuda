package models.tweet

case class Url(start: Option[Int], `end`: Option[Int], url: Option[String], expandedUrl: Option[String],
			   displayUrl: Option[String], status: Option[Int], title: Option[String], description: Option[String])
