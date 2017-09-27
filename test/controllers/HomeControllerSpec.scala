package controllers

import play.api.test._
import play.api.test.Helpers._
import org.specs2.mutable.Specification
import play.api.mvc.Results

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 *
 * For more information, see https://www.playframework.com/documentation/latest/ScalaTestingWithScalaTest
 */
class HomeControllerSpec extends Specification with Results {

  "HomeController GET" should {
    
    val heading = "Datawerks Test CSV Uploader"

    "render the index page from a new instance of controller" in {
      val controller = new HomeController
      val home = controller.index().apply(FakeRequest())

      status(home) must be equalTo OK
      contentType(home) must be equalTo Some("text/html")
      contentAsString(home) must contain (heading)
    }

    "render the index page from the application" in new WithApplication {
      val controller = app.injector.instanceOf[HomeController]
      val home = controller.index().apply(FakeRequest())

      status(home) must be equalTo OK
      contentType(home) must be equalTo Some("text/html")
      contentAsString(home) must contain (heading)
    }

    "render the index page from the router" in new WithApplication {
      // Need to specify Host header to get through AllowedHostsFilter
      val request = FakeRequest(GET, "/").withHeaders("Host" -> "localhost")
      val home = route(app, request).get

      status(home) must be equalTo OK
      contentType(home) must be equalTo Some("text/html")
      contentAsString(home) must contain (heading)
    }
  }
}
