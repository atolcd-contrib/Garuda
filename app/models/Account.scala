package models

import twitter.{TweetStreamListener, TwitterConnection}

case class Account(name: String, accountType: AccountType, bearerToken: String) {
	var rules: Option[Seq[Rule]] = None
	val twitterConnection = new TwitterConnection(this)
	var currentActiveCollect: Option[TweetStreamListener] = None
	
	var rateFilteredStreamConnecting: Int = accountType.rateFilteredStreamConnecting
	var rateFilteredStreamAddingOrDeletingFilters: Int = accountType.rateFilteredStreamAddingOrDeletingFilters
	var rateFilteredStreamListingFilters: Int = accountType.rateFilteredStreamListingFilters
	
	def resetRates(): Unit = {
		rateFilteredStreamConnecting = accountType.rateFilteredStreamConnecting
		rateFilteredStreamAddingOrDeletingFilters = accountType.rateFilteredStreamAddingOrDeletingFilters
		rateFilteredStreamListingFilters = accountType.rateFilteredStreamListingFilters
	}
	
	def isCurrentActiveCollect(collect: Collect): Boolean = {
		currentActiveCollect.isDefined && currentActiveCollect.get.collect.name == collect.name
	}
	
	def startCollect(collect: Collect): Either[String, TweetStreamListener] = {
		if (currentActiveCollect.isEmpty) {
			val result = twitterConnection.startCollect(collect)
			if (result.isRight) {
				currentActiveCollect = Some(result.toOption.get)
			}
			result
		} else {
			Left(s"The collect ${currentActiveCollect.get.collect.name} is already active.")
		}
	}
	
	def stopCollect(collect: Collect): Boolean = {
		if (currentActiveCollect.isDefined && currentActiveCollect.get.collect.name == collect.name) {
			currentActiveCollect.get.shutdown()
			currentActiveCollect = None
			collect.close()
			true
		} else {
			false
		}
	}
	
	/**
	 * Initialize this account active rules. Contact the Twitter API only if it was not already done.
	 *
	 * @param collectName the collect linked to these rules
	 * @return the current set of active rules (right), or, if there was a problem with the Twitter API,
	 *         the String containing the detail of the problem
	 */
	def initRules(collectName: String): Either[String, Seq[Rule]] = {
		if (rules.isEmpty) {
			retrieveActiveRules(collectName)
		} else {
			// The rules have already been retrieved, do not contact the Twitter API
			Right(rules.get)
		}
	}
	
	/**
	 * Retrieve this account active rules from the Twitter API.
	 *
	 * @param collectName the collect linked to these rules
	 * @return the current set of active rules (right), or, if there was a problem with the Twitter API,
	 *         the String containing the detail of the problem
	 */
	def retrieveActiveRules(collectName: String): Either[String, Seq[Rule]] = {
		// The rules have not yet be retrieved
		val activeRules = twitterConnection.getAllRules(collectName)
		if (activeRules.isRight) {
			// The rules have been correctly retrieved
			rules = Some(activeRules.getOrElse(Seq[Rule]()))
			Right(rules.get)
		} else {
			// There was a problem with the Twitter API
			Left(activeRules.left.getOrElse("Problem with Twitter API"))
		}
	}
	
	/**
	 * Add the rules to the current Twitter API instance.
	 *
	 * @param collecteName the collect linked to the rules
	 * @param temporaryRules the temporary rules to add
	 * @param rules the rules to add
	 * @return the current set of active rules (right), or, if there was a problem with the Twitter API,
	 *         the String containing the detail of the problem
	 */
	def addRules(collecteName: String, temporaryRules: Seq[TemporaryRule], rules: Seq[Rule]): Either[String, Seq[Rule]] = {
		val addedRules = twitterConnection.addRules(collecteName, temporaryRules, rules)
		if (addedRules.isRight) {
			// If the rules have been correctly updated, update the account rules
			this.rules = Some(addedRules.getOrElse(Seq[Rule]()))
		}
		addedRules
	}
	
	/**
	 * Delete the rules to the current Twitter API instance.
	 *
	 * @param rules the rules to delete
	 * @return the current set of active rules (right), or, if there was a problem with the Twitter API,
	 *         the String containing the detail of the problem
	 */
	def removeRules(collectName: String, rules: Seq[Rule]): Either[String, Seq[Rule]] = {
		twitterConnection.removeRules(rules)
		retrieveActiveRules(collectName)
	}
}

object AccountForm {
	
	/**
	 * Forms related
	 */
	import play.api.data.Forms._
	import play.api.data._
	import play.api.data.format.Formatter
	
	implicit def matchFilterFormat: Formatter[AccountType] = new Formatter[AccountType] {
		override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], AccountType] = {
			data.get(key)
				.map(AccountType.withName)
				.toRight(Seq(FormError(key, "error.required", Nil)))
		}
		
		override def unbind(key: String, value: AccountType): Map[String, String] = {
			Map(key -> value.name)
		}
	}
	
	val form: Form[Account] = Form(
		mapping(
			"Name" -> nonEmptyText,
			"Account type" -> Forms.of[AccountType],
			"Bearer Token" -> nonEmptyText
		)(Account.apply)(Account.unapply)
	)
}
