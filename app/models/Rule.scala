package models

import org.joda.time.DateTime

case class Rule(id: Long, tag: String, content: String, collect: String, createdAt: DateTime = DateTime.now()) {
	/**
	 * A rule is active when it is currently known by Twitter API.
	 */
	var isActive: Boolean = false
}

