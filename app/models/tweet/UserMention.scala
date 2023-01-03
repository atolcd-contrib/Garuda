package models.tweet

case class UserMention(start: Option[Int], `end`: Option[Int], userId: Option[String], userScreenName: Option[String])
