package renesca

import java.io.{File, FileOutputStream, InputStream, OutputStream}

import com.github.httpmock.builder.RequestBuilder._
import com.github.httpmock.builder.ResponseBuilder._
import com.github.httpmock.dto.RequestDto
import com.github.httpmock.rules.{MockService, MockVerifyException, Stubbing}
import com.github.httpmock.times.Times
import com.github.httpmock.{HttpMockServerStandalone, PortUtil}
import org.apache.commons.io.IOUtils
import org.specs2.execute.{Result, AsResult}
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

trait HttpMockServer extends BeforeAllAfterAll {
  var mockServer: HttpMockServerStandalone = null

  def createMock() = {
    val mockService = new MockService(baseUri, "/mockserver")
    mockService.create()
    mockService
  }

  def baseUri = s"http://localhost:${port}"

  override def beforeAll {
    val randomPorts = PortUtil.getRandomPorts(2)
    mockServer = new HttpMockServerStandalone(randomPorts.get(0), randomPorts.get(1))
    mockServer.start()

    copyResourceFromDependency(
      classLoader = mockServer.getClass.getClassLoader,
      resourceName = "wars/mockserver.war",
      outputFile = new File("target/mockserver.war"))

    mockServer.deploy("target/mockserver.war")
  }

  def copyResourceFromDependency(classLoader: ClassLoader, resourceName: String, outputFile: File) {
    if (outputFile.exists()) outputFile.delete()

    var inputStream: InputStream = null
    var outputStream: OutputStream = null
    try {
      inputStream = classLoader.getResourceAsStream(resourceName)
      outputStream = new FileOutputStream(outputFile)
      IOUtils.copy(inputStream, outputStream)
      inputStream.close()
      outputStream.close()
    } finally {
      IOUtils.closeQuietly(inputStream)
      IOUtils.closeQuietly(outputStream)
    }
  }

  override def afterAll {
    mockServer.stop()
  }

  def port = mockServer.getHttpPort
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
