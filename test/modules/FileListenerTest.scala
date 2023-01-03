package modules

import java.io.{BufferedWriter, File, FileWriter}

import org.scalatest._
import twitter.ObservableFile

class FileListenerTest extends FlatSpec with Matchers {
	"The file listener" should "reads all the lines" in {
		val observableFile = new ObservableFile(None)
		val dummyFileListener = new DummyFileListener(observableFile)
		val file1: File = new File(getClass.getResource("/").getPath + "/filelistenertest1.txt")
		val file2: File = new File(getClass.getResource("/").getPath + "/filelistenertest2.txt")
		val bf1 = new BufferedWriter(new FileWriter(file1))
		val bf2 = new BufferedWriter(new FileWriter(file2))
		
		bf1.write("line0\n")
		bf1.write("line1\n")
		bf1.flush()
		observableFile.setFile(file1)
		bf1.write("line2\n")
		bf1.write("line3\n")
		bf1.write("line4\n")
		bf1.flush()
		
		observableFile.setFile(file2)
		bf2.write("line5\n")
		bf2.write("line6\n")
		bf2.flush()
		
		observableFile.setNone()
		Thread.sleep(500)
		dummyFileListener.stop()
		
		dummyFileListener.result.length shouldBe 7
		for (i <- 0 until 7) {
			dummyFileListener.result(i) shouldBe s"line$i"
		}
		dummyFileListener.fileChanged shouldBe 3
		
		bf1.close()
		bf2.close()
	}
	
	private class DummyFileListener(obervableFile: ObservableFile) extends FileListener(obervableFile) {
		
		var result: List[String] = List[String]()
		var fileChanged: Int = 0
		
		/**
		 * Action to perform when a line is added in the observed file.
		 *
		 * @param line the tweet added, String format
		 */
		override def onEvent(line: String): Unit = {
			result :+= line
		}
		
		/**
		 * Action to perform when changing the file.
		 */
		override def onFileChange(): Unit = {
			fileChanged += 1
		}
		
		/**
		 * Action to perform when stopping the listener.
		 */
		override def onStop(): Unit = {}
	}
}
