/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package unit.uk.gov.hmrc.apidocumentation.connectors

import mockws.MockWS
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.{Action, Results}
import uk.gov.hmrc.apidocumentation.connectors.DownloadConnector
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException, NotFoundException}
import uk.gov.hmrc.play.http.metrics.NoopMetrics
import uk.gov.hmrc.play.test.UnitSpec


class DownloadConnectorSpec extends UnitSpec with ScalaFutures with BeforeAndAfterEach with GuiceOneAppPerSuite {

  val apiDocumentationPort = sys.env.getOrElse("WIREMOCK", "11114").toInt
  var apiDocumentationHost = "localhost"
  val apiDocumentationUrl = s"http://$apiDocumentationHost:$apiDocumentationPort"

  val serviceName = "hello-world"
  val version = "1.0"
  val resourceFoundUrl = s"$apiDocumentationUrl/apis/$serviceName/$version/documentation/some/resource"
  val resourceNotFoundUrl = s"$apiDocumentationUrl/apis/$serviceName/$version/documentation/some/resourceNotThere"
  val serviceUnavailableUrl = s"$apiDocumentationUrl/apis/$serviceName/$version/documentation/some/resourceInvalid"
  val timeoutUrl = s"$apiDocumentationUrl/apis/$serviceName/$version/documentation/some/timeout"

  val mockWS = MockWS {
    case ("GET", `resourceFoundUrl`) => Action(Results.Ok("hello world"))
    case ("GET", `resourceNotFoundUrl`) => Action(Results.NotFound)
    case ("GET", `serviceUnavailableUrl`) => Action(Results.ServiceUnavailable)
    case ("GET", `timeoutUrl`) => Action(Results.RequestTimeout)
  }

  trait Setup {
    implicit val hc = HeaderCarrier()
    val connector = new DownloadConnector(mockWS, NoopMetrics)
  }

  "downloadResource" should {
    "return resource when found" in new Setup {

      val result = await(connector.fetch(serviceName, version, "some/resource"))
      result.header.status shouldBe (200)
    }

    "throw NotFoundException when not found" in new Setup {
      intercept[NotFoundException] {
        await(connector.fetch(serviceName, version, "some/resourceNotThere"))
      }
    }

    "throw InternalServerException for any other response" in new Setup {
      intercept[InternalServerException] {
        await(connector.fetch(serviceName, version, "some/resourceInvalid"))
      }
      intercept[InternalServerException] {
        await(connector.fetch(serviceName, version, "some/timeout"))
      }
    }
  }

}
