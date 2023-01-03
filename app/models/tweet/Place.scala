package models.tweet

import com.twitter.clientlib.model.{Place => TPlace}

class Place(private val twitterPlace: TPlace) {
	import scala.jdk.CollectionConverters._
	
	lazy val id: Option[String] = Option(twitterPlace.getId)
	lazy val placeType: Option[String] = Option(twitterPlace.getPlaceType.getValue)
	lazy val name: Option[String] = Option(twitterPlace.getName)
	lazy val fullName: Option[String] = Option(twitterPlace.getFullName)
	lazy val countryCode: Option[String] = Option(twitterPlace.getCountryCode)
	lazy val country: Option[String] = Option(twitterPlace.getCountry)
	lazy val containedWithin: List[String] = twitterPlace.getContainedWithin.asScala.toList
	
	case class Coordinates(longitude: String, latitude: String)
	lazy val boundingBox: Array[Coordinates] = {
		try {
			(for (c <- twitterPlace.getGeo.getBbox.asScala.grouped(2))
				yield Coordinates(c.head.toString, c(1).toString)).toArray
		} catch {
			case _: Throwable => Array[Coordinates]()
		}
	}
	lazy val boundingBoxType: Option[String] = Option(twitterPlace.getGeo.getType.getValue)
}
