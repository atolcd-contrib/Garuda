package dao

import cache.AccountCache

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Singleton}
import models.{Account, AccountType}
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
class AccountDao @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
	extends HasDatabaseConfigProvider[JdbcProfile] {
	
	import profile.api._
	
	private val accounts = TableQuery[AccountsTable]
	
	/** Retrieve all the accounts */
	def all(): Future[Seq[Account]] = db.run(accounts.result)
	
	/** Retrieve an account from the name */
	def findByName(name: String): Future[Option[Account]] = {
		AccountCache.get(name) match {
			case Some(account) => Future(Some(account))
			case None => {
				val result = db.run(accounts.filter(_.name === name).result.headOption)
				result.map {
					case Some(account) => AccountCache.add(account.name, account)
					case _ => ()
				}
				result
			}
		}
	}
	
	/** Insert an account */
	def insert(account: Account): Future[Unit] = {
		AccountCache.add(account.name, account)
		db.run(accounts += account).map { _ => () }
	}
	
	/** Update an account */
	def update(name: String, account: Account): Future[Unit] = {
		val accountToUpdate: Account = account.copy(name)
		AccountCache.add(name, account)
		db.run(accounts.filter(_.name === name).update(accountToUpdate)).map(_ => ())
	}
	
	/** Delete an account */
	def delete(name: String): Future[Unit] = {
		AccountCache.remove(name)
		db.run(accounts.filter(_.name === name).delete).map(_ => ())
	}
	
	/** Get the number of accounts with the given name */
	def count(name: String): Future[Int] = {
		db.run(accounts.filter { account => account.name like name }.length.result)
	}
	
	private class AccountsTable(tag: Tag) extends Table[Account](tag, "account") {
		/**
		 * Fields
		 */
		def name = column[String]("name", O.PrimaryKey)
		
		implicit val typeMapper = MappedColumnType.base[AccountType, String](
			e => e.name,
			s => AccountType.withName(s)
		)
		def accountType = column[AccountType]("type")
		
		def bearerToken = column[String]("bearer_token")
		
		override def * = (name, accountType, bearerToken).mapTo[Account]
	}
}