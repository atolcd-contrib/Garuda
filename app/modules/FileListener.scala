package modules

import java.util.{Observable, Observer}

import org.apache.commons.io.input.{Tailer, TailerListenerAdapter}
import twitter.ObservableFile

/**
 * Implement this class to add a new module that listen to a file.
 *
 * @param observableFile the file to observe
 */
abstract class FileListener(var observableFile: ObservableFile) extends Observer {
	/**
	 * Action to perform when a line is added in the observed file.
	 *
	 * @param line the tweet added, String format
	 */
	def onEvent(line: String): Unit
	
	/**
	 * Action to perform when changing the file.
	 */
	def onFileChange(): Unit
	
	/**
	 * Action to perform when stopping the listener.
	 */
	def onStop(): Unit
	
	observableFile.addObserver(this)
	
	private val lock = new AnyRef
	private val listener = new TailerFileListener(onEvent, lock)
	private var tailer: Option[Tailer] = if (observableFile.isDefined) {
		Some(Tailer.create(observableFile.getFile, listener, 0))
	} else {
		None
	}
	
	/**
	 * Stop the listener.
	 */
	def stop(): Unit = {
		close()
		onStop()
	}
	
	override def update(o: Observable, arg: Any): Unit = {
		if (tailer.isDefined) {
			// Wait until the listener has reached the end of file
			Thread.sleep(100)
			while (listener.tailerActive) {
				try {
					Thread.sleep(100)
				} catch {
					case _: InterruptedException => {}
				}
			}
		}
		
		if (o.asInstanceOf[ObservableFile].isDefined) {
			// Change the file
			lock.synchronized {
				listener.tailerActive = true
				close()
				tailer = Some(Tailer.create(o.asInstanceOf[ObservableFile].getFile, listener, 0))
			}
		}
	}
	
	private def close(): Unit = {
		onFileChange()
		if (tailer.isDefined) {
			tailer.get.stop()
		}
	}
}

private class TailerFileListener(onEvent: String => Unit, lock: AnyRef) extends TailerListenerAdapter {
	var tailerActive: Boolean = true
	
	override def handle(line: String): Unit = lock.synchronized {
		tailerActive = true
		onEvent(line)
	}
	
	override def endOfFileReached(): Unit = {
		tailerActive = false
	}
}
