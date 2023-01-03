package models.tweet

case class Annotation(start: Option[Int], `end`: Option[Int], probability: Option[Double],
					  annotationType: Option[String], normalizedText: Option[String])
