package controllers

import dao.{AccountDao, CollectDao}
import javax.inject.{Inject, Singleton}
import models.Account
import models.AccountForm._
import play.api.data._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AccountController @Inject()(accountDao: AccountDao, collectDao: CollectDao, cc: MessagesControllerComponents)
								 (implicit executionContext: ExecutionContext) extends MessagesAbstractController(cc) {
	
	private val postUrl = routes.AccountController.createAccount
	
	def listAccounts: Action[AnyContent] = Action.async { implicit request: MessagesRequest[AnyContent] =>
		// Pass an unpopulated form to the template
		accountDao.all().map(accounts => Ok(views.html.account(accounts, form, postUrl)))
	}
	
	// This will be the action that handles our form post
	def createAccount: Action[AnyContent] = Action.async { implicit request: MessagesRequest[AnyContent] =>
		
		val errorFunction = { formWithErrors: Form[Account] =>
			// This is the bad case, where the form had validation errors.
			// Let's show the user the form again, with the errors highlighted.
			// Note how we pass the form with errors to the template.
			accountDao.all().map(accounts => BadRequest(views.html.account(accounts, formWithErrors, postUrl)))
		}
		
		val successFunction = { account: Account =>
			// This is the good case, where the form was successfully parsed as an Account object.
			// Check if name is unique
			accountDao.count(account.name).map(nb => {
				if (nb == 0) {
					// Can add account
					accountDao.insert(account).map(_ =>
						Redirect(routes.AccountController.listAccounts).flashing("success" -> "Account added!")
					)
				} else {
					// Name is not unique, return error
					accountDao.all().map(accounts =>
						BadRequest(views.html.account(accounts, form.fill(account)
							.withError("Name", "Account name already exists"), postUrl))
							.flashing("error" -> "Account name already exists")
					)
				}
			}).flatten
		}
		
		val formValidationResult = form.bindFromRequest()
		formValidationResult.fold(errorFunction, successFunction)
	}
	
	// This will be the action that handles our form post
	def updateAccount(accountName: String): Action[AnyContent] = Action.async { implicit request: MessagesRequest[AnyContent] =>
		
		val errorFunction = { formWithErrors: Form[Account] =>
			// This is the bad case, where the form had validation errors.
			// Let's show the user the form again, with the errors highlighted.
			// Note how we pass the form with errors to the template.
			val flash = formWithErrors.errors.foldLeft("")((s, e) => s"$s${e.key}: ${e.message}\n")
			Future(Redirect(routes.AccountController.listAccounts).flashing("error" -> flash))
		}
		
		val successFunction = { account: Account =>
			// This is the good case, where the form was successfully parsed as an Account object.
			// Check if name is unique
			accountDao.update(accountName, account).map(_ =>
				Redirect(routes.AccountController.listAccounts).flashing("success" -> "Account updated!")
			)
		}
		
		val formValidationResult = form.bindFromRequest()
		formValidationResult.fold(errorFunction, successFunction)
	}
	
	def removeAccount(accountName: String): Action[AnyContent] = Action.async { implicit request: MessagesRequest[AnyContent] =>
		// Pass an unpopulated form to the template
		collectDao.countByAccountName(accountName).map {
			case 0 => {
				accountDao.delete(accountName)
				Redirect(routes.AccountController.listAccounts).flashing("info" -> s"Account $accountName removed.")
			}
			case 1 => {
				Redirect(routes.AccountController.listAccounts).flashing("error" -> s"Cannot delete account, it is used in 1 collect.")
			}
			case nb: Int => {
				Redirect(routes.AccountController.listAccounts).flashing("error" -> s"Cannot delete account, it is used in $nb collects.")
			}
		}
	}
}
