package models.tweet

import net.liftweb.json._

object GetJson {
	def string(value: JValue): String = {
		try {
			val JString(result) = value
			result
		} catch {
			case t: Throwable => null
		}
	}
	
	def boolean(value: JValue): Boolean = {
		try {
			val JBool(result) = value
			result
		} catch {
			case t: Throwable => false
		}
	}
	
	def exists(value: JValue): Boolean = {
		try {
			value match {
				case JNothing | JNull => false
				case _ => true
			}
		} catch {
			case t: Throwable => false
		}
	}
	
	def optionString(value: JValue): Option[String] = {
		try {
			val JString(result) = value
			Some(result)
		} catch {
			case t: Throwable => None
		}
	}
	
	def optionBoolean(value: JValue): Option[Boolean] = {
		try {
			val JBool(result) = value
			Some(result)
		} catch {
			case t: Throwable => None
		}
	}
	
	def optionInt(value: JValue): Option[Int] = {
		try {
			val JInt(result) = value
			Some(result.toInt)
		} catch {
			case t: Throwable => None
		}
	}
}
