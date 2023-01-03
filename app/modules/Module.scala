package modules

import models.Collect

trait Module {
	val collect: Collect
	
	def stop(): Unit
}
