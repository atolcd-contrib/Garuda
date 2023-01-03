package modules.exporter.postgresql

import java.io.File

import cache.ModuleCache
import models.Collect
import modules.{Module, ModuleFileProcessed}
import modules.dao.ModuleFileProcessedDao

class PostgresModule(override val collect: Collect, val postgresConfiguration: PostgresConfiguration, val moduleFileProcessedDao: ModuleFileProcessedDao) extends Module {
	
	ModuleCache.add(collect.name, this)
	
	private val listener = new PostgresFileListener(collect.observableFile, postgresConfiguration)
	
	private val existingFiles = new File(collect.directory).listFiles().filterNot(_.getAbsolutePath.endsWith("Errors")).flatMap(_.listFiles())
	private val filesToProcess = existingFiles.filterNot(f => postgresConfiguration.filesProcessed.exists(mfp => mfp.file == s"${f.getParent}${File.separator}${f.getName}"))
	
	for (file <- filesToProcess) {
		val fullFileReader = new PostgresFullFileReader(file.getAbsolutePath, postgresConfiguration)
		fullFileReader.readFile()
		val fileProcessed = ModuleFileProcessed(collect.name, "ExporterPostgresql", s"${file.getParent}${File.separator}${file.getName}")
		postgresConfiguration.filesProcessed :+= fileProcessed
		moduleFileProcessedDao.insert(fileProcessed)
	}
	
	override def stop(): Unit = {
		listener.stop()
		ModuleCache.remove(collect.name)
	}
}
