/*
 * Copyright 2023 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock._

import play.api.Configuration
import play.api.test.Helpers._
import play.utils.UriEncoding
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.http.metrics.common.API

import uk.gov.hmrc.apidocumentation.models.XmlApiDocumentation

class XmlServicesConnectorSpec extends ConnectorSpec {

  val stubConfig: Configuration = Configuration(
    "metrics.jvm"                                          -> false,
    "microservice.services.api-platform-xml-services.host" -> stubHost,
    "microservice.services.api-platform-xml-services.port" -> stubPort
  )

  trait Setup {
    implicit val hc: HeaderCarrier      = HeaderCarrier()
    val connector: XmlServicesConnector = app.injector.instanceOf[XmlServicesConnector]

    val UpstreamException: UpstreamErrorResponse = UpstreamErrorResponse(
      "Internal server error",
      INTERNAL_SERVER_ERROR,
      INTERNAL_SERVER_ERROR
    )

    val getAllXmlApisUrl           = "/api-platform-xml-services/xml/apis"
    def getXmlApiUrl(name: String) = s"/api-platform-xml-services/xml/api/${UriEncoding.encodePathSegment(name, "UTF-8")}"

    val getXmlApiUrl = "/api-platform-xml-services/xml/api"

    val xmlApi1: XmlApiDocumentation = XmlApiDocumentation(
      name = "xml api 1",
      context = "xml api context",
      description = "xml api description",
      categories = None
    )

    val xmlApi2: XmlApiDocumentation = xmlApi1.copy(name = "xml api 2")
    val xmlApis                      = Seq(xmlApi1, xmlApi2)

  }

  "api" should {
    "be api-platform-xml-services" in new Setup {
      connector.api shouldBe API("api-platform-xml-services")
    }
  }

  "fetchAllXmlApis" should {
    "return all Xml Apis" in new Setup {

      stubFor(get(urlPathEqualTo(getAllXmlApisUrl))
        .willReturn(aResponse()
          .withStatus(OK)
          .withJsonBody(xmlApis)))

      val result: Seq[XmlApiDocumentation] = await(connector.fetchAllXmlApis())

      result shouldBe xmlApis
    }

    "throw an exception correctly" in new Setup {
      stubFor(get(urlPathEqualTo(getAllXmlApisUrl))
        .willReturn(aResponse()
          .withStatus(BAD_REQUEST)))

      intercept[UpstreamException.type] {
        await(connector.fetchAllXmlApis())
      }
    }
  }

  "fetchXmlApiByServiceName" should {
    "return an Xml Api" in new Setup {

      stubFor(get(urlPathEqualTo(getXmlApiUrl))
        .withQueryParam("serviceName", equalTo(xmlApi1.name))
        .willReturn(aResponse()
          .withStatus(OK)
          .withJsonBody(xmlApi1)))

      val result: Option[XmlApiDocumentation] = await(connector.fetchXmlApiByServiceName(xmlApi1.name))

      result shouldBe Some(xmlApi1)
    }

    "throw an exception correctly" in new Setup {
      stubFor(get(urlPathEqualTo(getXmlApiUrl))
        .withQueryParam("serviceName", equalTo(xmlApi1.name))
        .willReturn(aResponse()
          .withStatus(NOT_FOUND)))

      val result = await(connector.fetchXmlApiByServiceName(xmlApi1.name))
      result shouldBe None
    }
  }
}
