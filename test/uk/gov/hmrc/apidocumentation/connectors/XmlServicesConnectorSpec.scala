/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.apidocumentation.connectors

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.metrics.API

import play.api.Configuration
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.test.Helpers._
import uk.gov.hmrc.apidocumentation.models.XmlApiDocumentation
import uk.gov.hmrc.http.UpstreamErrorResponse

class XmlServicesConnectorSpec extends ConnectorSpec {
  val thirdPartyDeveloperUrl = "https://api-platform-xml-services.example.com"
  val sessionId = "A_SESSION_ID"

  val stubConfig = Configuration(
    "metrics.jvm" -> false,
    "microservice.services.api-platform-xml-services.host" -> stubHost,
    "microservice.services.api-platform-xml-services.port" -> stubPort
  )

  trait Setup {
    implicit val hc = HeaderCarrier()
    val connector = app.injector.instanceOf[XmlServicesConnector]
    val UpstreamException = UpstreamErrorResponse(
      "Internal server error",
      INTERNAL_SERVER_ERROR,
      INTERNAL_SERVER_ERROR
    )
  }

  "api" should {
    "be api-platform-xml-services" in new Setup {
      connector.api shouldBe API("api-platform-xml-services")
    }
  }

  "fetchAllXmlApis" should {
    "return all Xml Apis" in new Setup {
      val xmlApi1 = XmlApiDocumentation(
        name = "xml api 1",
        context = "xml api context",
        description = "xml api description",
        categories = None
      )
      val xmlApi2 = xmlApi1.copy(name = "xml api 2")
      val xmlApis = Seq(xmlApi1, xmlApi2)

      stubFor(
        get(
          urlPathEqualTo(s"/xml/apis")
        )
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withJsonBody(xmlApis)
          )
      )

      val result = await(connector.fetchAllXmlApis)

      result shouldBe xmlApis
    }

    "throw an exception correctly" in new Setup {
      stubFor(
        get(
          urlPathEqualTo(s"/xml/apis")
        )
          .willReturn(
            aResponse()
              .withStatus(400)
          )
      )

      intercept[UpstreamException.type] {
        await(connector.fetchAllXmlApis)
      }
    }
  }
}
