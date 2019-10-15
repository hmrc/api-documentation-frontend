/*
 * Copyright 2019 HM Revenue & Customs
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
import org.mockito.Mockito.when
import play.api.http.Status._
import play.api.mvc.{Action, Results}
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.connectors.DownloadConnector
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException, NotFoundException}

class DownloadConnectorSpec extends ConnectorSpec {
  val apiDocumentationUrl = "https://api-documentation.example.com"

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
    val mockAppConfig = mock[ApplicationConfig]
    val connector = new DownloadConnector(mockWS, mockAppConfig)

    when(mockAppConfig.localApiDocumentationUrl).thenReturn(apiDocumentationUrl)
  }

  "downloadResource" should {
    "return resource when found" in new Setup {

      val result = await(connector.fetch(serviceName, version, "some/resource"))
      result.header.status shouldBe OK
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
