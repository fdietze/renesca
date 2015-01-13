package renesca

import java.io._
import java.net.Socket
import java.util.concurrent.TimeUnit

import com.github.httpmock.builder.RequestBuilder._
import com.github.httpmock.builder.ResponseBuilder._
import com.github.httpmock.dto.RequestDto
import com.github.httpmock.rules.{MockService, MockVerifyException, Stubbing}
import com.github.httpmock.times.Times
import com.github.httpmock.{ExecRunner, PortUtil}
import com.jayway.awaitility.scala.AwaitilitySupport
import com.jayway.awaitility.{Awaitility, Duration}
import com.jayway.restassured.RestAssured
import org.specs2.mutable
import org.specs2.specification._


class GraphManagerSpec extends mutable.Specification with HttpMockServer {
  "GraphManager" should {
    // TODO: do everything in integration test instead.
    "some test" in new HttpMock(this) {
      when(request().get("/some/url").build()).thenRespond(response().build())
      true must beTrue
    }
  }

}

trait BeforeAllAfterAll extends mutable.Specification {
  override def map(fragments: => Fragments) =
    Step(beforeAll) ^ fragments ^ Step(afterAll)

  protected def beforeAll()

  protected def afterAll()
}

trait HttpMockServer extends BeforeAllAfterAll with AwaitilitySupport {
  var runner: ExecRunner = null
  var startPort: String = ""
  var stopPort: String = ""

  def createMock() = {
    val mockService = new MockService(baseUri, "/mockserver")
    mockService.create()
    mockService
  }

  def baseUri = s"http://localhost:${port}"

  override def beforeAll {
    val ports = PortUtil.getRandomPorts(3)
    startPort = ports.get(0).toString
    stopPort = ports.get(1).toString
    val ajpPort = ports.get(2).toString
    runner = new ExecRunner(ExecRunner.readConfiguration(), startPort, stopPort, ajpPort)
    runner.start()

    waitUntilServerIsStarted
  }

  def isServerStarted() = {
    try {
      RestAssured.given().baseUri(baseUri).basePath("/mockserver").get("/").getStatusCode == 200
    } catch {
      case e: Exception =>
        false
    }
  }


  def waitUntilServerIsStarted: Unit = {
    Awaitility.await() atMost(new Duration(60, TimeUnit.SECONDS)) pollDelay (Duration.ONE_SECOND) until { isServerStarted }
  }

  override def afterAll {
    val s = new Socket("localhost", Integer.parseInt(stopPort))
    val out = new PrintStream(s.getOutputStream)
    out.print("SHUTDOWN")
    out.flush()
    s.close()
  }

  def port = startPort
}

class HttpMock(val mockService: MockService) extends Scope with After {

  def this(mockServer: HttpMockServer) {
    this(mockServer.createMock())
  }

  override def after: Unit = {
    deleteMock()
  }

  def deleteMock() {
    mockService.delete()
  }

  def when(request: RequestDto) = new Stubbing(mockService, request)

  def verify(request: RequestDto, times: Times) {
    val numberOfCalls = getNumberOfCalls(request)
    if (!times.matches(numberOfCalls)) {
      val expected = times.getFailedDescription
      throw new MockVerifyException(s"Mock verification failed. Request was called $numberOfCalls times but should have been called $expected")
    }
  }

  private def getNumberOfCalls(request: RequestDto) = verifyResponse(request).getTimes

  private def verifyResponse(request: RequestDto) = mockService.verify(request)

  def requestUrl = mockService.getRequestUrl

}
