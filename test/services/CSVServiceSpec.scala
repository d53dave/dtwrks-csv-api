package services

import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.Specification
import play.api.test._
import play.api.test.Helpers._
import storage.FileStorageProvider
import play.api.mvc.BodyParsers
import models.CSVUpload
import play.api.mvc.BodyParser
import akka.util.ByteString

@RunWith(classOf[JUnitRunner])
class CSVServiceSpec extends Specification with Mockito  {
  
  val storageService = mock[FileStorageProvider]
  val kafkaService = mock[KafkaService]
  
  class TestService() extends CSVService(storageService, kafkaService) {

  }

  val service = new TestService()
 
  "CSVService" should {
    
    "return the correct body parser" in {
      
      val parser = service.buildBodyParser(None)
      
      parser must be equalTo BodyParsers.parse.empty
      
      val parser2 = service.buildBodyParser(Some(CSVUpload("dummy", "data", "here")))
      
      parser2 must beAnInstanceOf[BodyParser[ByteString]]
      
    }
    
    "create new CSVUpload Object and retrieve paths for them" in {
      val testPath = "/some/path"
      storageService.getPath(any[String]) returns testPath
      
      val name = "csvfile.csv"
      val csvUpload = service.getNewCSVUpload(name)
      
      csvUpload.path must be equalTo testPath
      csvUpload.id must have length 36
      csvUpload.filename must startWith(csvUpload.id)
    }
    
    "lists all uploaded files" in {
      val file1Name = "file1.csv"
      val file2Name = "file2.csv"
      val file1NameWithId = s"53a0bf8b-f0ac-49ff-84ba-5ea4d2c061a3$file1Name"
      val file2NameWithId = s"53a0bf8b-f1ac-49ff-84ba-5ea4d2c061a3$file2Name"
      
      storageService.getFileNames() returns Seq(file1NameWithId, file2NameWithId)
      storageService.getPath(any[String]) answers {name => s"/path/to/$name"}
      
      val uploadedCSVs = service.getAllUploadedCSVs()
      
      uploadedCSVs must have size 2
      uploadedCSVs(0).filename must be equalTo file1Name 
      uploadedCSVs(0).path must be equalTo s"/path/to/$file1NameWithId"
      uploadedCSVs(1).filename must be equalTo file2Name 
      uploadedCSVs(0).path must be equalTo s"/path/to/$file2NameWithId"
    }
    
  }
}