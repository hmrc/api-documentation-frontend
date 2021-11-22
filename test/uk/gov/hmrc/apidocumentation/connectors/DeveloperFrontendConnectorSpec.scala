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

import com.github.tomakehurst.wiremock.client.WireMock._
import play.twirl.api.Html
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.HtmlPartial
import play.api.test.Helpers._
import uk.gov.hmrc.apidocumentation.models.JsonFormatters._
import play.api.Configuration
import uk.gov.hmrc.play.http.metrics.common.API

class DeveloperFrontendConnectorSpec extends ConnectorSpec {
  val developerFrontendUrl = "http://developer-frontend.example.com"

  val stubConfig = Configuration(
    "microservice.services.developer-frontend.host" -> stubHost,
    "microservice.services.developer-frontend.port" -> stubPort
  )

  trait Setup {
    val config = app.injector.instanceOf[ApplicationConfig]
    val connector = app.injector.instanceOf[DeveloperFrontendConnector]
  }

  "api" should {
    "be third-party-developer-frontend" in new Setup {
      connector.api shouldBe API("third-party-developer-frontend")
    }
  }

  "fetchNav links" should {

    "return fetched nav links and pass headers by" in new Setup {
      implicit val hc = HeaderCarrier(extraHeaders = Seq("possibleAuthHeader" -> "possibleAuthHeaderVal"))

      stubFor(
        get(
          urlPathEqualTo("/developer/user-navlinks")
        )
        .willReturn(
          aResponse()
          .withStatus(OK)
          .withJsonBody(Seq(NavLink("Some random link", "/developer/345435345342523534253245")))
        )
      )

      val result = await(connector.fetchNavLinks())
      result shouldBe List(NavLink("Some random link", "/developer/345435345342523534253245", false))
    }
  }

  "fetchTermsOfUsePartial" should {
    "return the terms of use as a partial" in new Setup {
      implicit val hc = HeaderCarrier()
      val response = HtmlPartial.Success(None, Html("<p>some terms of use</p>"))

      stubFor(
        get(
          urlPathEqualTo("/developer/partials/terms-of-use")
        )
        .willReturn(
          aResponse()
          .withStatus(OK)
          .withBody(response.content.toString)
        )
      )

      val result = await(connector.fetchTermsOfUsePartial())
      result shouldBe response
    }
  }
}
