package modules.exporter.postgresql

import modules.FullFileReader

class PostgresFullFileReader(fileName: String, postgresConfiguration: PostgresConfiguration) extends FullFileReader(fileName) {
	private val postgresInsertion = new PostgresInsertion(postgresConfiguration)
	/**
	 * Action to perform when a line is read from the file.
	 *
	 * @param line the tweet read
	 */
	override def onLine(line: String): Unit = {
		try {
			postgresInsertion.insertLine(line)
		} catch {
			case t: Throwable => logger.warn("Error : " + t)
		}
	}
	
	/**
	 * Action to perform when closing the file.
	 */
	override def onClose(): Unit = {
		postgresInsertion.insertBatch()
		postgresInsertion.close()
	}
}
