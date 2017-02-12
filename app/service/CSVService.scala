package service

import com.google.inject.Inject
import storage.FileStorageProvider
import util.StreamingCSVBodyParser
import model.CSVUpload
import play.api.mvc.BodyParsers
import play.api.Logger

class CSVService @Inject() (storageProvider: FileStorageProvider) {
  
  def buildBodyParser(fileName: Option[CSVUpload]) = {
    fileName match {
      case Some(csv) => StreamingCSVBodyParser.buildStreamingBodyParser(storageProvider.getWriteableStream(csv.filename))
      case None      => BodyParsers.parse.empty
    }
  }
  
  def getNameWithoutId(name: String): String = {
    if(name.length() > 36) name.substring(36) else name
  }

  def getNewCSVUpload(name: String) = {
    val id = java.util.UUID.randomUUID().toString()
    val completeFileName = id + name;
    CSVUpload(id, completeFileName, storageProvider.getPath(completeFileName))
  }

  def deleteCSVifFound(id: String): Boolean = {
    getCSVMetadata(id) match {
      case Some(csv) =>
        storageProvider.deleteFile(csv.filename); true
      case None => false
    }
  }
  
  def getAllUploadedCSVs(): Seq[CSVUpload] = {
    storageProvider.getFileNames().map { name => 
      Logger("application").debug(s"getAllUploadedCSVs processing $name")
      val id = name.substring(0, 36); // UUIDs are 32 hex digits + 4 dashes
      CSVUpload(id, getNameWithoutId(name), storageProvider.getPath(name))
    }
  }

  def getCSVMetaData(id: String): Option[CSVUpload] = {
    getCSVMetadata(id)
  }

  private def getCSVMetadata(id: String): Option[CSVUpload] = {
    storageProvider.getFileNames().find(name => name.startsWith(id)).map(name => CSVUpload(id, name, storageProvider.getPath(name)))
  }

}