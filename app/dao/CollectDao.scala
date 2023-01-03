package dao

import java.sql.Timestamp

import cache.CollectCache
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Singleton}
import models.Collect
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
class CollectDao @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
	extends HasDatabaseConfigProvider[JdbcProfile] {
	
	import profile.api._
	
	private val collects = TableQuery[CollectsTable]
	
	/** Retrieve all the collects */
	def all(): Future[Seq[Collect]] = db.run(collects.result)
	
	/** Retrieve a collect from the name */
	def findByName(name: String): Future[Option[Collect]] = {
		CollectCache.get(name) match {
			case Some(collect) => Future(Some(collect))
			case None => {
				val result = db.run(collects.filter(_.name === name).result.headOption)
				result.map {
					case Some(collect) => CollectCache.add(collect.name, collect)
					case _ => ()
				}
				result
			}
		}
	}
	
	/** Insert a new collect */
	def insert(collect: Collect): Future[Unit] = {
		CollectCache.add(collect.name, collect)
		db.run(collects += collect).map { _ => () }
	}
	
	/** Update a collect */
	def update(name: String, collect: Collect): Future[Unit] = {
		val collectToUpdate: Collect = collect.copy(name)
		CollectCache.add(name, collect)
		db.run(collects.filter(_.name === name).update(collectToUpdate)).map(_ => ())
	}
	
	/** Delete a collect */
	def delete(name: String): Future[Unit] = {
		CollectCache.remove(name)
		db.run(collects.filter(_.name === name).delete).map(_ => ())
	}
	
	/** Get the number of collects with the given name */
	def count(name: String): Future[Int] = {
		db.run(collects.filter { collect => collect.name like name }.length.result)
	}
	
	/** Get the number of collects with the given account name */
	def countByAccountName(name: String): Future[Int] =
		db.run(collects.filter(_.account === name).length.result)
	
	private class CollectsTable(tag: Tag) extends Table[Collect](tag, "collect") {
		/**
		 * Fields
		 */
		def name = column[String]("name", O.PrimaryKey)
		
		def directory = column[String]("directory")
		
		def account = column[String]("account")
		
		implicit def jodaTimeMapping: BaseColumnType[DateTime] = MappedColumnType.base[DateTime, Timestamp](
			dateTime => new Timestamp(dateTime.getMillis),
			timeStamp => new DateTime(timeStamp.getTime)
		)
		def createdAt = column[DateTime]("created_at")
		
		override def * = (name, directory, account, createdAt) <> ((Collect.apply _).tupled, Collect.unapply)
	}
}