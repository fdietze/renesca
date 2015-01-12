package renesca

import com.github.httpmock.dto.RequestDto
import com.github.httpmock.rules.{MockService, MockVerifyException, Stubbing}
import com.github.httpmock.times.Times
import com.github.httpmock.{HttpMockServerStandalone, PortUtil}
import org.specs2.mutable.Specification
import org.specs2.specification.{Step, Fragments, BeforeAfter, BeforeAfterContextExample}

import scala.collection.JavaConversions._
import com.github.httpmock.builder.RequestBuilder._
import com.github.httpmock.builder.ResponseBuilder._

class GraphManagerSpec extends Specification with HttpMock {
  "GraphManager" should {
    // TODO: do everything in integration test instead.
    "some test" in {
      println("test")
      when(request().get("/some/url").build()).thenRespond(response().build())
      true must beTrue
    }
  }

}

trait BeforeAllAfterAll extends Specification {
  override def map(fragments: =>Fragments) =
    Step(beforeAll) ^ fragments ^ Step(afterAll)

  protected def beforeAll()
  protected def afterAll()
}

trait HttpMockServer extends BeforeAllAfterAll {
  var mockServer : HttpMockServerStandalone = null

  override def beforeAll {
    val randomPorts: List[Integer] = PortUtil.getRandomPorts(2).toList
    mockServer = new HttpMockServerStandalone(randomPorts.get(0), randomPorts.get(1))
    mockServer.start()
  }

  override def afterAll {
    mockServer.stop()
  }
}

trait HttpMock extends HttpMockServer with BeforeAfter {
  val MOCK_SERVER_CONTEXT = "/mockserver"
  var mockService : MockService = null

  override def before {
    mockService = new MockService(baseUri, MOCK_SERVER_CONTEXT);
    mockService.create()
  }

  override def after {
    mockService.delete()
  }

  def when(request : RequestDto) = new Stubbing(mockService, request)

  def verify(request: RequestDto, times: Times) {
    val numberOfCalls = getNumberOfCalls(request)
    if (!times.matches(numberOfCalls)) {
      val expected = times.getFailedDescription
      throw new MockVerifyException(s"Mock verification failed. Request was called $numberOfCalls times but should have been called $expected")
    }
  }

  private def getNumberOfCalls(request: RequestDto) = verifyResponse(request).getTimes
  private def verifyResponse(request: RequestDto) = mockService.verify(request)
  private def baseUri: String = s"http://localhost:$port"
  def port = mockServer.getHttpPort
  def requestUrl = mockService.getRequestUrl
}
