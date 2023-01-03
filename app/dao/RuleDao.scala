package dao

import java.sql.Timestamp

import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Singleton}
import models.Rule
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
class RuleDao @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
	extends HasDatabaseConfigProvider[JdbcProfile] {
	
	import profile.api._
	
	private val rules = TableQuery[RulesTable]
	
	/** Retrieve all the rules */
	def all(): Future[Seq[Rule]] = db.run(rules.result)
	
	/** Retrieve a rule from the id */
	def findById(id: Long): Future[Option[Rule]] =
		db.run(rules.filter(_.id === id).result.headOption)
	
	/** Retrieve rules from the collect name */
	def findByCollectName(name: String): Future[Seq[Rule]] =
		db.run(rules.filter(_.collect === name).result)
	
	/** Insert a new rule */
	def insert(rule: Rule): Future[Unit] = db.run(rules += rule).map { _ => () }
	
	/** Insert new rules */
	def batchInsert(newRules: Seq[Rule]): Future[Unit] = db.run(rules ++= newRules).map { _ => () }
	
	/** Update a rule */
	def update(id: Long, rule: Rule): Future[Unit] = {
		val ruleToUpdate: Rule = rule.copy(id)
		db.run(rules.filter(_.id === id).update(ruleToUpdate)).map(_ => ())
	}
	
	/** Delete a rule */
	def delete(id: Long): Future[Unit] =
		db.run(rules.filter(_.id === id).delete).map(_ => ())
	
	/** Delete a set of rules */
	def batchDelete(ids: Seq[Long]): Future[Unit] = {
		db.run(rules.filter(_.id.inSet(ids)).delete).map(_ => ())
	}
	
	private class RulesTable(tag: Tag) extends Table[Rule](tag, "rule") {
		/**
		 * Fields
		 */
		def id = column[Long]("id", O.PrimaryKey)
		
		def ruleTag = column[String]("tag")
		
		def content = column[String]("content")
		
		def collect = column[String]("collect")
		
		implicit def jodaTimeMapping: BaseColumnType[DateTime] = MappedColumnType.base[DateTime, Timestamp](
			dateTime => new Timestamp(dateTime.getMillis),
			timeStamp => new DateTime(timeStamp.getTime)
		)
		def createdAt = column[DateTime]("created_at")
		
		
		override def * = (id, ruleTag, content, collect, createdAt).mapTo[Rule]
	}
}