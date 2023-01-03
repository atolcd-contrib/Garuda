package models.tweet

case class Media(key: Option[String], mediaType: Option[String], url: Option[String], durationMs: Option[Int],
				 height: Option[Int], width: Option[Int], previewImageUrl: Option[String],
				 viewCount: Option[Int], altText: Option[String])
