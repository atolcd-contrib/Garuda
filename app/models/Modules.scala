package models

import modules.exporter.postgresql.{PostgresConfiguration, PostgresModule}

class Modules {
	var postgresExporterConfiguration: Option[PostgresConfiguration] = None
	var postgresExporterModule: Option[PostgresModule] = None
}
