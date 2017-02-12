package storage

import java.io.OutputStream
import java.io.Closeable
import com.google.inject.ImplementedBy

/** A resouce that supports streaming 
 *  
 *  This wraps a [[java.io.OutputStream]] and a [[java.io.Closeable]], which will be closed when 
 *  the resource is going out of scope. The OutputStream and the Closeable can be the same object
 */
case class StreamableResource(stream: Option[OutputStream], resource: Option[Closeable]) extends Closeable {
  override def close = resource.foreach(_.close)
}

/** The FileStorageProvider trait provides the interface for generic file operations
 *
 *  As it stands, FileStorageProvider is implemented only by [[storage.FilesystemStorage]],
 *  but any back-end that can implement this interface (i.e. most importantly can provide a 
 *  [[storage.StreamableResource]]) can be used, e.g. an RDBMS, streaming to a different web service, etc.
 */
@ImplementedBy(classOf[FilesystemStorage])
trait FileStorageProvider {
  def getFileNames(): Seq[String]
  
  def getPath(fileName: String): String
  
  def writeFile(data: Array[Byte], fileName: String): Unit
  
  def getWriteableStream(fileName: String): StreamableResource

  def readFile(fileName: String): Array[Byte]
  
  def deleteFile(fileName: String): Unit
}