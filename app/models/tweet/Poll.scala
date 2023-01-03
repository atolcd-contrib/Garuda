package models.tweet

case class Poll(id: Option[String], options: List[OptionPoll], durationMinutes: Option[Int],
				endDatetime: Option[String], votingStatus: Option[String])

case class OptionPoll(position: Option[Int], label: Option[String], votes: Option[Int])