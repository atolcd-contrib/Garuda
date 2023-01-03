package modules.exporter.postgresql

import modules.ModuleFileProcessed

case class PostgresConfiguration(collect: String,
								 host: String,
								 port: Int,
								 base: String,
								 schema: String,
								 user: String,
								 password: String) {
	var filesProcessed: Seq[ModuleFileProcessed] = Seq[ModuleFileProcessed]()
}

object PostgresConfigurationForm {
	/**
	 * Forms related
	 */
	import play.api.data.Forms._
	import play.api.data._
	
	val form: Form[PostgresConfiguration] = Form(
		mapping(
			"Collect" -> nonEmptyText,
			"Host" -> nonEmptyText,
			"Port" -> number,
			"Base" -> nonEmptyText,
			"Schema" -> nonEmptyText,
			"User" -> nonEmptyText,
			"Password" -> nonEmptyText
		)(PostgresConfiguration.apply)(PostgresConfiguration.unapply)
	)
}
