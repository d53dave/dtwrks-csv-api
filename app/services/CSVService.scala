package services

import com.google.inject.Inject
import storage.FileStorageProvider
import util.StreamingCSVBodyParser
import models.CSVUpload
import play.api.mvc.BodyParsers
import play.api.Logger
import akka.actor.ActorRef

/**
 * This service handles CSV related operations by interacting with the injected storage provider and kafka service
 */
class CSVService @Inject() (storageProvider: FileStorageProvider, kafkaService: KafkaService) {
 
  val logger: Logger = Logger(this.getClass())
  
  /**
   * Build the body parser based on the provided file name
   * Returning parse.empty avoids streaming a body if the upload could not be associated
   * with a prior upload request, i.e. POST /csv/ was not called before POST /csv/{id}/content
   */
  def buildBodyParser(csv: Option[CSVUpload]) = {
    csv match {
      case Some(csv) => StreamingCSVBodyParser.buildStreamingBodyParser(storageProvider.getWriteableStream(csv.filename))
      case None      => BodyParsers.parse.empty
    }
  }
  
  def getNameWithoutId(name: String): String = {
    if(name.length() > 36) name.substring(36) else name
  }

  /**
   * To prepare for an upload, an "empty" CSVUpload object is created
   * with a new id. In order to avoid overwriting files with the same 
   * name, the id (UUID) is prepended to the orginal filename. Since
   * UUIDs are 36 characters long, the original file name can be retrieved
   * later.
   */
  def getNewCSVUpload(name: String) = {
    val id = java.util.UUID.randomUUID().toString()
    val completeFileName = id + name;
    CSVUpload(id, completeFileName, storageProvider.getPath(completeFileName))
  }

  def deleteCsvIfFound(id: String): Boolean = {
    getCSVMetadata(id) match {
      case Some(csv) =>
        storageProvider.deleteFile(csv.filename); true
      case None => false
    }
  }
  
  def getAllUploadedCSVs(): Seq[CSVUpload] = {
    storageProvider.getFileNames().map { name => 
      logger.debug(s"getAllUploadedCSVs processing $name")
      val id = name.substring(0, 36); // UUIDs are 32 hex digits + 4 dashes
      CSVUpload(id, getNameWithoutId(name), storageProvider.getPath(name))
    }
  }

  def getCSVMetaData(id: String): Option[CSVUpload] = {
    getCSVMetadata(id)
  }
  
  /**
   * After an upload successfully completes, we push an event into the 
   * message broker using the injected Kafkaservice
   */
  def signalUploadComplete(id: String) = {
    logger.debug(s"Signalling upload complete for id $id");
    getCSVMetadata(id).map(kafkaService.sendCSVUploadEvent(_))
  }

  private def getCSVMetadata(id: String): Option[CSVUpload] = {
    storageProvider.getFileNames().find(name => name.startsWith(id)).map(name => CSVUpload(id, name, storageProvider.getPath(name)))
  }

}