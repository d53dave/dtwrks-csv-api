package service

import com.google.inject.Inject
import storage.FileStorageProvider
import util.StreamingCSVBodyParser
import model.CSVUpload
import play.api.mvc.BodyParsers

class CSVService @Inject() (storageProvider: FileStorageProvider) {
  
  def buildBodyParser(fileName: Option[CSVUpload]) = {
    fileName match {
      case Some(csv) => StreamingCSVBodyParser.buildStreamingBodyParser(storageProvider.getWriteableStream(csv.filename))
      case None      => BodyParsers.parse.empty
    }
  }

  def getNewCSVUpload(name: String) = {
    val id = java.util.UUID.randomUUID().toString()
    val completeFileName = id + name;
    CSVUpload(id, completeFileName, storageProvider.getPath(completeFileName))
  }

  def deleteCSVifFound(id: String): Boolean = {
    getCSVMetadata(id) match {
      case Some(csv) =>
        storageProvider.deleteFile(id); true
      case None => false
    }
  }
  
  def getAllUploadedCSVs(): Seq[CSVUpload] = {
    storageProvider.getFileNames().map { name => 
      val id = name.substring(0, 36); // UUIDs are 32 hex digits + 4 dashes
      CSVUpload(id, name, storageProvider.getPath(name))
    }
  }

  def getCSVMetaData(id: String): Option[CSVUpload] = {
    getCSVMetadata(id)
  }

  private def getCSVMetadata(id: String): Option[CSVUpload] = {
    storageProvider.getFileNames().find(name => name.startsWith(id)).map(name => CSVUpload(id, name, id + storageProvider.getPath(name)))
  }

}