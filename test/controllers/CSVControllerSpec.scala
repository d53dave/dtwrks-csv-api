package controllers

import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.libs.json.{ JsArray, Json }
import play.api.mvc.{ Result, _ }
import play.api.test.Helpers._
import play.api.test.{ WithApplication, _ }
import akka.actor.ActorSystem
import akka.util.ByteString
import akka.stream.ActorMaterializer

import scala.concurrent.{ ExecutionContext, Future }
import services.CSVService
import models.CSVUpload
import java.nio.file.Files
import java.io.FileOutputStream
import util.StreamingCSVBodyParser
import storage.StreamableResource

@RunWith(classOf[JUnitRunner])
class CSVControllerSpec extends Specification with Results with Mockito {

  
  val mockCSVService = mock[CSVService]

  // Some test data
  val tempFile = Files.createTempFile("test", ".csv")
  val csvUpload1 = CSVUpload("anId", tempFile.toFile.getName, tempFile.toString)
  val csvUploadJson1 = Json.obj(
    "id" -> "anId",
    "filename" -> tempFile.toFile.getName,
    "path" -> tempFile.toString)

  val csvUpload2 = CSVUpload("otherId", "noName.csv", "/path/where/uploads/are/noname.csv")
  val csvUploadJson2 = Json.obj(
    "id" -> "otherId",
    "filename" -> "noName.csv",
    "path" -> "/path/where/uploads/are/noname.csv")

  val allCSVs = List(csvUploadJson1, csvUploadJson2)
  
  val testWaitingForUploadCache = scala.collection.mutable.Map[String, CSVUpload]()

  // Controller-under-test
  class TestController() extends CSVController(mockCSVService) {
     override def waitingForUpload = testWaitingForUploadCache
  }

  val controller = new TestController()

  "Application" should {


    "CSVController#getCSVs" should {
      "get all available CSVS" in {
        mockCSVService.getAllUploadedCSVs() returns Seq(csvUpload1, csvUpload2)

        val result: Future[Result] = controller.getCSVs().apply(FakeRequest())

        status(result) must be equalTo OK

        contentAsJson(result) must be equalTo JsArray(allCSVs)

        there was one(mockCSVService).getAllUploadedCSVs()
      }
    }

    "CSVController#getCSV" should {
      "return 404 when id not found" in {
        mockCSVService.getCSVMetaData(any[String]) returns None

        val result: Future[Result] = controller.getCSV("wrongId").apply(FakeRequest())

        status(result) must be equalTo NOT_FOUND

        there was one(mockCSVService).getCSVMetaData(any[String])
      }

      "return CSV if found" in {
        // uses akka.stream.FileIO, will stream empty file, but checking for 200
        implicit val actorSystem = FakeApplication().actorSystem
        implicit val materializer = ActorMaterializer()

        mockCSVService.getCSVMetaData(any[String]) returns Some(csvUpload1)
        mockCSVService.getNameWithoutId(any[String]) returns csvUpload1.filename

        val result: Future[Result] = controller.getCSV("anId").apply(FakeRequest())

        status(result) must be equalTo OK

        there were two(mockCSVService).getCSVMetaData(any[String])
        there was one(mockCSVService).getNameWithoutId(any[String])
      }
    }

    "CSVController#createCSV" should {
      "return 400 if form missing" in {
        val request = FakeRequest()
        val result: Future[Result] = controller.createCSV().apply(request)

        status(result) must be equalTo BAD_REQUEST
      }

      "return 400 if form invalid" in {
        val request = FakeRequest().withFormUrlEncodedBody(("garbage", ""))
        val result: Future[Result] = controller.createCSV().apply(request)

        status(result) must be equalTo BAD_REQUEST
      }
      
      "create a new CSVUpload Object if form valid" in {
        mockCSVService.getNewCSVUpload(any[String]) returns csvUpload2
        
        val request = FakeRequest().withFormUrlEncodedBody(("filename", "noName.csv"))
        val result: Future[Result] = controller.createCSV().apply(request)
        
        status(result) must be equalTo OK
        contentAsJson(result) must be equalTo csvUploadJson2
        
        there was one(mockCSVService).getNewCSVUpload(any[String])
      }

    }
    
    "CSVController#uploadCSV" should {
      "return 400 if body is missing" in {
        testWaitingForUploadCache.clear()
        mockCSVService.deleteCsvIfFound(any[String]) returns false
        mockCSVService.buildBodyParser(any[Option[CSVUpload]]) returns BodyParsers.parse.empty
        
        val request = FakeRequest()
        val result = controller.uploadCSV(csvUpload1.id).apply(request)
        
        status(result) must be equalTo BAD_REQUEST
        
        there was one(mockCSVService).deleteCsvIfFound(any[String])
        there was one(mockCSVService).buildBodyParser(any[Option[CSVUpload]])
        there was no(mockCSVService).signalUploadComplete(any[String])
      }
      
      "return OK if upload was prepared" in {
        val csvContent = "1, Dave, 2017-01-01 00:00:00.0"
        
        testWaitingForUploadCache.clear()
        testWaitingForUploadCache.put(csvUpload1.id, csvUpload1)
        
        val stream = Some(new FileOutputStream(tempFile.toFile))
        mockCSVService.buildBodyParser(Some(csvUpload1)) returns StreamingCSVBodyParser.buildStreamingBodyParser(StreamableResource(stream, stream))
        
        val request = FakeRequest().withRawBody(ByteString(csvContent))
        val result: Future[Result] = controller.uploadCSV(csvUpload1.id).apply(request)
        
        status(result) must be equalTo OK
        
        testWaitingForUploadCache must be empty
        
        there was one(mockCSVService).buildBodyParser(Some(csvUpload1))
        there was one(mockCSVService).signalUploadComplete(any[String])
      }
    }

    "send 404 on an unknown route " in pending{
      // This is weird: the test throws java.lang.RuntimeException: The global crypto instance requires a running application!
      // It seems that the CookieSigner uses Crypto, which is not allowed when running under a FakeApplication?!
      // Using 'new WithApplication' results in a BAD_REQUEST for this call
      route(FakeApplication(), FakeRequest(GET, "/unknown")) must beSome.which({ res =>
        status(res) == NOT_FOUND
      })
    }

  }
}