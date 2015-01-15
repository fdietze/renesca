package renesca

import com.github.httpmock.builder.RequestBuilder._
import com.github.httpmock.builder.ResponseBuilder._
import com.github.httpmock.specs.{HttpMock, HttpMockServer}
import org.specs2.mutable


class GraphManagerSpec extends mutable.Specification with HttpMockServer {
  "GraphManager" should {
    // TODO: do everything in integration test instead.
    "some test" in new HttpMock(this) {
      when(request().get("/some/url").build()).thenRespond(response().build())
      true must beTrue
    }
  }

}


