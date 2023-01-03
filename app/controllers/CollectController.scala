package controllers

import dao.{AccountDao, CollectDao, RuleDao, TemporaryRuleDao}
import javax.inject.{Inject, Singleton}
import models.{Collect, Rule, TemporaryRule}
import models.CollectForm.{CollectData, form => collectForm}
import models.TemporaryRuleForm.{TemporaryRuleData, form => ruleForm}
import play.api.Configuration
import play.api.data._
import play.api.mvc._
import play.filters.csrf._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

@Singleton
class CollectController @Inject()(accountDao: AccountDao,
								  collectDao: CollectDao,
								  ruleDao: RuleDao,
								  temporaryRulesDao: TemporaryRuleDao,
								  cc: MessagesControllerComponents,
								  conf: Configuration)
								 (implicit executionContext: ExecutionContext) extends MessagesAbstractController(cc) {
	
	private val postUrlCreateCollect = routes.CollectController.createCollect
	private def postUrlCreateRule(collectName: String) = routes.CollectController.createRule(collectName)
	private def postUrlAffectRules(collectName: String) = routes.CollectController.affectRules(collectName)
	private def postRemoveAccountRulesUrl(collectName: String) = routes.CollectController.removeAccountRules(collectName)
	
	def listCollects: Action[AnyContent] = Action.async { implicit request: MessagesRequest[AnyContent] =>
		// Pass an unpopulated form to the template
		collectDao.all().map {
			case collects => {
				accountDao.all().map {
					case accounts => Ok(views.html.listCollects(collects, accounts, collectForm, postUrlCreateCollect))
				}
			}
		}.flatten
	}
	
	def seeCollect(collectName: String): Action[AnyContent] = Action.async { implicit request: MessagesRequest[AnyContent] =>
		val collect = updateRulesOfCollect(collectName)
		displayCollect(collect)
	}
	
	def startCollect(collectName: String): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
		val collect = updateRulesOfCollect(collectName)
		val account = Await.result(accountDao.findByName(collect.account), Duration.Inf).get
		
		val collectStarted = account.startCollect(collect)
		
		val flash = {
			if (collectStarted.isRight) {
				new Flash(Map("success" -> "Collect started!"))
			} else {
				new Flash(Map("error" -> s"Impossible to start the collect: \n${collectStarted.left}"))
			}
		}
		
		Redirect(routes.CollectController.seeCollect(collect.name)).flashing(flash)
	}
	
	def stopCollect(collectName: String): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
		val collect = updateRulesOfCollect(collectName)
		val account = Await.result(accountDao.findByName(collect.account), Duration.Inf).get
		
		val collectStopped = account.stopCollect(collect)
		
		val flash = {
			if (collectStopped) {
				new Flash(Map("success" -> "Collect stopped!"))
			} else {
				new Flash(Map("error" -> s"Impossible to stop the collect"))
			}
		}
		
		Redirect(routes.CollectController.seeCollect(collect.name)).flashing(flash)
	}
	
	def removeAccountRules(collectName: String): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
		val nbOfRulesToRemove = request.body.asFormUrlEncoded.get("number_of_rules_to_remove").head.toInt
		if (nbOfRulesToRemove > 0) {
			val rulesIds = request.body.asFormUrlEncoded.get("account_rules_ids").flatMap(_.split(",")).map(_.toLong)
			
			val collect = updateRulesOfCollect(collectName)
			val account = Await.result(accountDao.findByName(collect.account), Duration.Inf).get
			
			account.removeRules(collectName, account.rules.getOrElse(List[Rule]()).filter(rule => rulesIds.contains(rule.id)))
			
			Redirect(routes.CollectController.seeCollect(collect.name)).flashing("info" -> "Account rules removes.")
		} else {
			Redirect(routes.CollectController.seeCollect(collectName)).flashing("error" -> "No rule selected.")
		}
	}
	
	def affectRules(collectName: String): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
		val activeIdRules = request.body.asFormUrlEncoded.get("active_ids").flatMap(_.split(","))
		val nonActiveIdRules = request.body.asFormUrlEncoded.get("non_active_ids").flatMap(_.split(","))
		
		// a + id: collect active rules
		// n + id: collect non-active rules
		// t + id: collect temporary rules
		val newActiveIdRules = activeIdRules.filter(_.startsWith("n")).map(_.substring(2).toLong)
		val newActiveIdTemporaryRules = activeIdRules.filter(_.startsWith("t")).map(_.substring(2).toLong)
		val newNonActiveIdRules = nonActiveIdRules.filter(_.startsWith("a")).map(_.substring(2).toLong)
		
		val collect = updateRulesOfCollect(collectName)
		val account = Await.result(accountDao.findByName(collect.account), Duration.Inf).get
		account.initRules(collect.name)
		
		val newActiveRules = collect.nonActiveRules.filter(rule => newActiveIdRules.contains(rule.id))
		val newActiveTemporaryRules = collect.temporaryRules.getOrElse(List[TemporaryRule]()).filter(rule => newActiveIdTemporaryRules.contains(rule.id.get))
		val newNonActiveRules = collect.activeRules.filter(rule => newNonActiveIdRules.contains(rule.id))
		
		var flashData = Map[String, String]()
		
		// Make rules inactive
		if (newNonActiveRules.nonEmpty) {
			val removeRulesResult = account.removeRules(collect.name, newNonActiveRules)
			if (removeRulesResult.isRight) {
				// Update with DAO
				for (rule <- newNonActiveRules) {
					rule.isActive = false
					ruleDao.update(rule.id, rule)
				}
			} else {
				flashData += "error" -> removeRulesResult.left.getOrElse("")
			}
		}
		
		// Make rules active
		if (newActiveRules.nonEmpty || newActiveTemporaryRules.nonEmpty) {
			val filteredNewActiveTemporaryRules = newActiveTemporaryRules.filter(rule => rule.content.length <= account.accountType.sizeOfRule)
			val filteredNewActiveRules = newActiveRules.filter(rule => rule.content.length <= account.accountType.sizeOfRule)
			
			val addRulesResult = account.addRules(collectName, filteredNewActiveTemporaryRules, filteredNewActiveRules)
			if (addRulesResult.isRight) {
				collect.removeTemporaryRules(filteredNewActiveTemporaryRules)
				collect.removeRules(filteredNewActiveRules)
				collect.addRules(addRulesResult.getOrElse(Seq[Rule]()))
				// Update with DAO
				temporaryRulesDao.batchDelete(filteredNewActiveTemporaryRules.map(_.id.get))
				ruleDao.batchDelete(filteredNewActiveRules.map(_.id))
				ruleDao.batchInsert(addRulesResult.getOrElse(Seq[Rule]()))
				// Inform user that some rules have not been added due to size incompatibility
				if (filteredNewActiveRules.size < newActiveRules.size
					|| filteredNewActiveTemporaryRules.size < newActiveTemporaryRules.size) {
					flashData += "info" -> "Some rules have not been added because their length is greater than the authorized length for this account type."
				}
			} else {
				flashData += "error" -> addRulesResult.left.getOrElse("")
			}
		}
		
		//displayCollect(collect, flash = new Flash(flashData))
		Redirect(routes.CollectController.seeCollect(collect.name)).flashing(new Flash(flashData))
	}
	
	// This will be the action that handles our form post
	def createRule(collectName: String): Action[AnyContent] = Action.async { implicit request: MessagesRequest[AnyContent] =>
		
		val errorFunction = { formWithErrors: Form[TemporaryRuleData] =>
			// This is the bad case, where the form had validation errors.
			// Let's show the user the form again, with the errors highlighted.
			// Note how we pass the form with errors to the template.
			val collect = updateRulesOfCollect(collectName)
			displayCollect(collect, formWithErrors)
		}
		
		val successFunction = { data: TemporaryRuleData =>
			// This is the good case, where the form was successfully parsed as a Collect object.
			val newTemporaryRule = Await.result(temporaryRulesDao.insert(TemporaryRule(None, data.tag, data.content, collectName)), Duration.Inf)
			val collect = updateRulesOfCollect(collectName)
			// Add the rule to the collect
			collect.addTemporaryRule(newTemporaryRule)
			// Display the collect
			displayCollect(collect)
		}
		
		val formValidationResult = ruleForm.bindFromRequest()
		formValidationResult.fold(errorFunction, successFunction)
	}
	
	// This will be the action that handles our form post
	def updateCollect(collectName: String): Action[AnyContent] = Action.async { implicit request: MessagesRequest[AnyContent] =>
		
		val errorFunction = { formWithErrors: Form[CollectData] =>
			// This is the bad case, where the form had validation errors.
			// Let's show the user the form again, with the errors highlighted.
			// Note how we pass the form with errors to the template.
			val flash = formWithErrors.errors.foldLeft("")((s, e) => s"$s${e.key}: ${e.message}\n")
			Future(Redirect(routes.CollectController.seeCollect(collectName)).flashing("error" -> flash))
		}
		
		val successFunction = { collectData: CollectData =>
			// This is the good case, where the form was successfully parsed as a Collect object.
			// Only the account can be updated
			val collect = Await.result(collectDao.findByName(collectName), Duration.Inf).get
			collect.account = collectData.account
			collectDao.update(collectName, collect).map(_ =>
				Redirect(routes.CollectController.seeCollect(collectName)).flashing("success" -> "Account updated!")
			)
			
		}
		
		val formValidationResult = collectForm.bindFromRequest()
		formValidationResult.fold(errorFunction, successFunction)
	}
	
	// This will be the action that handles our form post
	def createCollect: Action[AnyContent] = Action.async { implicit request: MessagesRequest[AnyContent] =>
		
		val errorFunction = { formWithErrors: Form[CollectData] =>
			// This is the bad case, where the form had validation errors.
			// Let's show the user the form again, with the errors highlighted.
			// Note how we pass the form with errors to the template.
			collectDao.all().map {
				case collects => {
					accountDao.all().map {
						case accounts => BadRequest(views.html.listCollects(collects, accounts, formWithErrors, postUrlCreateCollect))
					}
				}
			}.flatten
		}
		
		val successFunction = { collectData: CollectData =>
			// This is the good case, where the form was successfully parsed as a Collect object.
			// Check if name is unique
			collectDao.count(collectData.name).map(nb => {
				if (nb == 0) {
					// Can add collect
					val directory = conf.get[String]("garuda.directory") + "/" + collectData.name
					val collect = Collect(collectData.name, directory, collectData.account)
					collectDao.insert(collect).map(_ =>
						Redirect(routes.CollectController.listCollects).flashing("success" -> "Collect created!")
					)
				} else {
					// Name is not unique, return error
					collectDao.all().map {
						case collects => {
							accountDao.all().map {
								case accounts => BadRequest(views.html.listCollects(collects, accounts, collectForm.fill(collectData).withError("Name", "Collect name already exists"), postUrlCreateCollect))
									.flashing("error" -> "Collect name already exists.")
							}
						}
					}.flatten
				}
			}).flatten
		}
		
		val formValidationResult = collectForm.bindFromRequest()
		formValidationResult.fold(errorFunction, successFunction)
	}
	
	def removeCollect(collectName: String): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
		val collect = updateRulesOfCollect(collectName)
		Await.result(collectDao.delete(collectName), Duration.Inf)
		
		val flash = {
			if (collect.isActive) {
				new Flash(Map("error" -> "The collect is started, stop it before removing it."))
			} else {
				new Flash(Map("info" -> s"The collect $collectName has been removed."))
			}
		}
		
		Redirect(routes.CollectController.listCollects).flashing(flash)
	}
	
	/**
	 * Update the rules of the given collect.
	 *
	 * @param collectName the name of the collect to update.
	 * @return the updated collect.
	 */
	private def updateRulesOfCollect(collectName: String): Collect = {
		val collect = Await.result(collectDao.findByName(collectName), Duration.Inf).get
		// Populate rules if not already done
		if (collect.rules.isEmpty) {
			// Retrieve the rules of the collect
			Await.result(ruleDao.findByCollectName(collectName).map {
				case rules => temporaryRulesDao.findByCollectName(collectName).map {
					case temporaryRules => collect.initRules(rules, temporaryRules)
				}
			}, Duration.Inf)
			// Update the rules of the account
			Await.result(accountDao.findByName(collect.account).map {
				case Some(account) => {
					account.initRules(collect.name)
					if (account.rules.isDefined) {
						// Based on the rules of account, set to non-active the rules that are not
						val activeIds = account.rules.get.map(_.id)
						collect.rules.get.foreach(rule => rule.isActive = activeIds.contains(rule.id))
					}
				}
				case None => {}
			}, Duration.Inf)
		}
		collect
	}
	
	/**
	 * Display the given collect.
	 *
	 */
	private def displayCollect(collect: Collect, form: Form[TemporaryRuleData] = ruleForm, flash: Flash = new Flash())(implicit request: MessagesRequest[AnyContent]): Future[Result] = {
		accountDao.all().map {
			case accounts => {
				accountDao.findByName(collect.account).map {
					case Some(account) => {
						val token = CSRF.getToken.get
						Ok(views.html.seeCollect(collect, account, accounts, form, postUrlCreateRule(collect.name),
							postUrlAffectRules(collect.name), postRemoveAccountRulesUrl(collect.name), token.value)).flashing(flash)
					}
					case None => InternalServerError(s"Account ${collect.account} not found.")
				}
			}
		}.flatten
	}
}
