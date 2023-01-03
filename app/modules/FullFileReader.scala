package modules

import play.api.Logging

import scala.io.Source

abstract class FullFileReader(fileName: String) extends Logging {
	/**
	 * Action to perform when a line is read from the file.
	 *
	 * @param line the tweet read
	 */
	def onLine(line: String): Unit
	
	/**
	 * Action to perform when closing the file.
	 */
	def onClose(): Unit
	
	/**
	 * Process the file.
	 */
	def readFile(): Unit = {
		try {
			val source = Source.fromFile(fileName)
			for (line <- source.getLines()) {
				onLine(line)
			}
			source.close()
			onClose()
		} catch {
			case e: Exception => logger.warn(e.getMessage)
		}
	}
}
