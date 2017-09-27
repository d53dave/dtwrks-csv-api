package api

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.test.WithBrowser
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import play.api.libs.json.{ JsArray, Json }
import play.api.mvc.{ Result, _ }
import play.api.test.Helpers._
import play.api.test.{ WithApplication, _ }
import play.api.mvc.Results
import play.api.libs.ws.WSClient
import com.google.inject.Inject
import scala.concurrent.duration._
import scala.concurrent.Await
 
@RunWith(classOf[JUnitRunner])
class APISpec @Inject()(wsClient: WSClient) extends Specification with Results {
  
  val heading = "Datawerks Test CSV Uploader"
 
  "Application" should {
    sequential
 
    "work from within a browser" in new WithBrowser(webDriver = new HtmlUnitDriver()) {

      browser.goTo("http://localhost:" + port)
      browser.pageSource must contain(heading)
    }
 
    "retrieve empty data-set through the browser" in new WithBrowser(webDriver = new HtmlUnitDriver()) {
 
      browser.goTo("http://localhost:" + port)
      browser.pageSource must contain("There are no CSV Files available. You can upload one!")
    }
    
    "uploading without POST /csv should return BAD_REQUEST" in new WithServer {
      val response = wsClient.url(s"http://localhost:$port/csv/garbageid/content").post("")
    }
    
    /*
     * Other tests left out for brevity
     * 
     */
    val csvUploadId = ???
    
    "uploading file with prios POST /csv should return 200" in new WithServer {
      
    }
    
    "retrieve non-empty data-set through the browser" in new WithBrowser(webDriver = new HtmlUnitDriver()) {
 
      browser.goTo("http://localhost:" + port)
      browser.pageSource must not contain("There are no CSV Files available. You can upload one!")
    }
    
    "GET against /csv/{id}/content should return file, which should be identical to the uplaoded one" in new WithServer {
      
    }
    
    "DELETE against /csv/{id} should return 200" in new WithServer {
      
    }
    
    "retrieve empty data-set through the browser" in new WithBrowser(webDriver = new HtmlUnitDriver()) {
 
      browser.goTo("http://localhost:" + port)
      browser.pageSource must contain("There are no CSV Files available. You can upload one!")
    }
    
    "DELETE against /csv/{id} should return 404" in new WithServer {
      
    }
  }
}
