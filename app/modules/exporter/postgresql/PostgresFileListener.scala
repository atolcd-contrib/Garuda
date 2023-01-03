package modules.exporter.postgresql

import modules.FileListener
import play.api.Logging
import twitter.ObservableFile

class PostgresFileListener(obervableFile: ObservableFile, postgresConfiguration: PostgresConfiguration) extends FileListener(obervableFile) with Logging {
	private val postgresInsertion = new PostgresInsertion(postgresConfiguration)
	
	/**
	 * Action to perform when a line is added in the observed file.
	 *
	 * @param line the tweet added, String format
	 */
	override def onEvent(line: String): Unit = {
		try {
			postgresInsertion.insertLine(line)
		} catch {
			case t: Throwable => logger.warn("Error : " + t)
		}
	}
	
	/**
	 * Action to perform when changing the file.
	 */
	override def onFileChange(): Unit = {
		postgresInsertion.insertBatch()
	}
	
	/**
	 * Action to perform when stopping the listener.
	 */
	override def onStop(): Unit = {
		postgresInsertion.insertBatch()
		postgresInsertion.close()
	}
}
