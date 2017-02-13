package controllers

import javax.inject._
import play.api.mvc._
import model.CSVUpload
import play.api.libs.json.Json
import play.api.Logger
import scala.concurrent.Future
import service.CSVService
import akka.stream.scaladsl.FileIO
import akka.stream.scaladsl.Source
import akka.util.ByteString
import better.files.File
import play.api.http.HttpEntity

@Singleton
class CSVController @Inject() (csvService: CSVService) extends Controller {

  var waitingForUpload = scala.collection.mutable.Map[String, CSVUpload]()

  val logger: Logger = Logger.apply("application")

  case class PreparedUpload[A](id: String, action: Action[A]) extends Action[A] {
    def apply(request: Request[A]): Future[Result] = {
      logger.debug(s"Checking for $id in $waitingForUpload")
      if (waitingForUpload.contains(id)) action(request) else Future.successful(BadRequest)
    }

    val parser = action.parser
  }

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
        if (request.body.isInstanceOf[Unit]) {
          waitingForUpload.remove(id)
          csvService.deleteCSVifFound(id)
          BadRequest
        } else {
          waitingForUpload.remove(id)
          csvService.signalUploadComplete(id)
          Ok
        }
    }

  def getCSVWithId(id: String) = Action {
    csvService.getCSVMetaData(id) match {
      case Some(value) => Ok(Json.toJson(value))
      case None        => NotFound
    }
  }

  def deleteCSV(id: String) = Action {
    val success = csvService.deleteCSVifFound(id)

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
