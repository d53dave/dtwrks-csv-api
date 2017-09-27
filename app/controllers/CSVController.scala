package controllers

import javax.inject._
import play.api.mvc._
import models.CSVUpload
import play.api.libs.json.Json
import play.api.Logger
import scala.concurrent.Future
import services.CSVService
import akka.stream.scaladsl.FileIO
import akka.stream.scaladsl.Source
import akka.util.ByteString
import better.files.File
import play.api.http.HttpEntity
import play.api.mvc.AnyContentAsEmpty

/**
 * This controller offers the File Upload related API Endpoints
 *
 * The upload workflow is two-staged: first, a POST to /csv containing
 * file metadata as a form is expected. This returns a CSVUpload object, which
 * contains the id that should be used in the following POST to /csv/{id}/content
 *
 * This also provides a few convenience endpoints to support a minimal JavaScript client
 * i.e. get all uploaded CSVs, remove CSVs, etc.
 */
@Singleton
class CSVController @Inject() (csvService: CSVService) extends Controller {

  def waitingForUpload = scala.collection.mutable.Map[String, CSVUpload]()

  val logger: Logger = Logger.apply("application")

  def getCSVs = Action {
    Ok(Json.toJson(csvService.getAllUploadedCSVs()))
  }

  def getCSV(id: String) = Action {
    csvService.getCSVMetaData(id) match {
      case Some(csv) => {
        val source: Source[ByteString, _] = FileIO.fromPath(File(csv.path).path)
        val name = csvService.getNameWithoutId(csv.filename)
        Result(
          header = ResponseHeader(200, Map("Content-Disposition" -> s"attachment;filename=$name")),
          body = HttpEntity.Streamed(source, None, Some("text/csv")))
      }
      case None => NotFound
    }
  }

  def createCSV = Action { request =>
    val form = request.body.asFormUrlEncoded

    form match {
      case Some(vals) => {
        getValueFromForm("filename", vals) match {
          case Some(name) => {
            val csv = csvService.getNewCSVUpload(name)
            waitingForUpload += (csv.id -> csv)
            logger.debug(s"There are ${waitingForUpload.size} uploads waiting")
            Ok(Json.toJson(csv))
          }
          case None => BadRequest("Filename missing")
        }

      }
      case None => BadRequest("Form missing")
    }
  }

  def uploadCSV(id: String) =
    Action(csvService.buildBodyParser(waitingForUpload.get(id))) {
      request =>
        if (request.body.isInstanceOf[AnyContentAsRaw]) {
          waitingForUpload.remove(id)
          csvService.signalUploadComplete(id)
          Ok
        } else {
          waitingForUpload.remove(id)
          csvService.deleteCsvIfFound(id)
          BadRequest
        }
    }

  def getCSVWithId(id: String) = Action {
    csvService.getCSVMetaData(id) match {
      case Some(value) => Ok(Json.toJson(value))
      case None        => NotFound
    }
  }

  def deleteCSV(id: String) = Action {
    val success = csvService.deleteCsvIfFound(id)

    if (success) Ok else NotFound
  }

  private def getValueFromForm(key: String, values: Map[String, Seq[String]]): Option[String] = {
    values.get(key) match {
      case Some(seq) => {
        if (seq.size > 0) Some(seq(0)) else None
      }
      case None => None
    }
  }
}
