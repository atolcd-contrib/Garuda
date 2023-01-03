package twitter

import java.io.File
import java.util.Observable

class ObservableFile(private var file: Option[File]) extends Observable {
	def setFile(file: File): Unit = {
		this.synchronized {
			this.file = Some(file)
		}
		setChanged()
		notifyObservers()
	}
	
	def setNone(): Unit = {
		this.synchronized {
			this.file = None
		}
		setChanged()
		notifyObservers()
	}
	
	def isEmpty: Boolean = file.isEmpty
	
	def isDefined: Boolean = file.isDefined
	
	def getFile: File = {
		file.get
	}
}
