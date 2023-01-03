package models

/**
 * To specify the type of the developer account used to connect to the API,
 * and to known the limits of each type before sending a request.
 */
sealed trait AccountType {
	val name: String
	val sizeOfRule: Int
	val numberOfRules: Int
	
	/**
	 * Filtered stream rates
	 */
	val rateFilteredStreamConnecting: Int = 50
	val rateFilteredStreamAddingOrDeletingFilters: Int
	val rateFilteredStreamListingFilters: Int = 450
}

object AccountType {
	val values: Map[String, AccountType] = Map[String, AccountType](
		Essential.name -> Essential,
		Elevated.name -> Elevated,
		Academic.name -> Academic
	)
	
	def withName(name: String): AccountType = {
		values(name)
	}
}

object Essential extends AccountType {
	override val name: String = "Essential"
	override val sizeOfRule: Int = 512
	override val numberOfRules: Int = 5
	override val rateFilteredStreamAddingOrDeletingFilters: Int = 25
}

object Elevated extends AccountType {
	override val name: String = "Elevated"
	override val sizeOfRule: Int = 512
	override val numberOfRules: Int = 25
	override val rateFilteredStreamAddingOrDeletingFilters: Int = 50
}

object Academic extends AccountType {
	override val name: String = "Academic"
	override val sizeOfRule: Int = 1024
	override val numberOfRules: Int = 1000
	override val rateFilteredStreamAddingOrDeletingFilters: Int = 100
}
