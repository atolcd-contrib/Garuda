package modules

import org.joda.time.DateTime

case class ModuleFileProcessed(collect: String, module: String, file: String, processedAt: DateTime = DateTime.now())
