package storage

import better.files._
import com.google.inject.Inject
import com.typesafe.config.ConfigFactory
import scala.util.Try
import play.api.Logger

/** The FilesystemStorage provides an implementation for [[storage.FileStorageProvider]] backed by the file system.  
 *  
 *  Although this microservice has only CSV files in it's scope, the underlying storage functionality works for
 *  arbitrary files
 */

@Inject
class FilesystemStorage extends FileStorageProvider {
  val logger: Logger = Logger(this.getClass)

  private val basePath = Try {
    val path = ConfigFactory.load().getString("storage.filesystem.basepath")
    File(path).createDirectories()
    path
  }.getOrElse(File.newTemporaryDirectory("dtwrks-upload").path.toString())

  override def getFileNames(): Seq[String] = {
    logger.debug(s"Getting all available files")
    val base = File(basePath)
    val matches: Iterator[File] = base.glob("**/*")
    matches.filter(p => !p.isDirectory).map(f => f.name).toSeq
  }
  
  override def getPath(fileName: String): String = {
    getFile(fileName).path.toString()
  }

  override def writeFile(data: Array[Byte], fileName: String): Unit = {
    logger.debug(s"Writing ${data.size} to $fileName")
    val file = getFile(fileName)
    file.writeByteArray(data) // defaults to overwrite
    () // return unit
  }

  override def getWriteableStream(fileName: String): StreamableResource = {
    logger.debug(s"Creating StreamableResource for $fileName")
    val file = getFile(fileName)
    new StreamableResource(Some(file.newOutputStream), Some(file.newOutputStream))
  }

  override def readFile(fileName: String): Array[Byte] = {
    logger.debug(s"Reading file $fileName")
    getFile(fileName).byteArray
  }

  override def deleteFile(fileName: String): Unit = {
    logger.debug(s"deleteFile file $fileName")
    val swallowExceptions = false
    getFile(fileName).delete(swallowExceptions)
    ()
  }

  private def getFile(fileName: String): File = {
    basePath / fileName
  }
}