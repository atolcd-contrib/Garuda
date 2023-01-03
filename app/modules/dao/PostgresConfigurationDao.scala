package modules.dao

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Singleton}
import modules.exporter.postgresql.PostgresConfiguration
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
class PostgresConfigurationDao @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
	extends HasDatabaseConfigProvider[JdbcProfile] {
	
	import profile.api._
	
	private val postgresConfigurations = TableQuery[PostgresConfigurationsTable]
	
	/** Retrieve all the postgres configurations */
	def all(): Future[Seq[PostgresConfiguration]] = db.run(postgresConfigurations.result)
	
	/** Retrieve a postgres configuration from the collect name */
	def findByCollect(collect: String): Future[Option[PostgresConfiguration]] = {
		db.run(postgresConfigurations.filter(_.collect === collect).result.headOption)
	}
	
	/** Insert a new postgres configuration */
	def insert(postgresConfiguration: PostgresConfiguration): Future[Unit] = {
		db.run(postgresConfigurations += postgresConfiguration).map { _ => () }
	}
	
	/** Update a postgres configuration */
	def update(collect: String, postgresConfiguration: PostgresConfiguration): Future[Unit] = {
		val postgresConfigurationToUpdate: PostgresConfiguration = postgresConfiguration.copy(collect)
		db.run(postgresConfigurations.filter(_.collect === collect).update(postgresConfigurationToUpdate)).map(_ => ())
	}
	
	/** Delete a postgres configuration */
	def delete(collect: String): Future[Unit] = {
		db.run(postgresConfigurations.filter(_.collect === collect).delete).map(_ => ())
	}
	
	/** Get the number of Postgres configurations for the given collect name */
	def countByCollectName(name: String): Future[Int] =
		db.run(postgresConfigurations.filter(_.collect === name).length.result)
		
	/** Get the number of Postgres configurations */
	def count(): Future[Int] =
		db.run(postgresConfigurations.length.result)
	
	private class PostgresConfigurationsTable(tag: Tag) extends Table[PostgresConfiguration](tag, "postgres_configuration") {
		/**
		 * Fields
		 */
		def collect = column[String]("collect", O.PrimaryKey)
		
		def host = column[String]("host")
		
		def port = column[Int]("port")
		
		def base = column[String]("base")
		
		def schema = column[String]("schema_")
		
		def user = column[String]("user_")
		
		def password = column[String]("password")
		
		override def * = (collect, host, port, base, schema, user, password) <> ((PostgresConfiguration.apply _).tupled, PostgresConfiguration.unapply)
	}
}
