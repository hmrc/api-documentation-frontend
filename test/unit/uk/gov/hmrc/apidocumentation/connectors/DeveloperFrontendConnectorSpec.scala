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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{verify => verifyMockServer, _}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import uk.gov.hmrc.apidocumentation.config.{ApiDocumentationFrontendAuditConnector, WSHttp}
import uk.gov.hmrc.apidocumentation.models.JsonFormatters._
import uk.gov.hmrc.apidocumentation.models._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status._
import play.api.libs.json.Json
import play.twirl.api.Html
import uk.gov.hmrc.apidocumentation.connectors.DeveloperFrontendConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.metrics.{API, NoopMetrics}
import uk.gov.hmrc.play.partials.HtmlPartial
import uk.gov.hmrc.play.test.UnitSpec

class DeveloperFrontendConnectorSpec extends UnitSpec with ScalaFutures with BeforeAndAfterEach with GuiceOneAppPerSuite {
  val developerFrontendPort = sys.env.getOrElse("WIREMOCK", "11115").toInt
  var developerFrontendHost = "localhost"
  val developerFrontendUrl = s"http://$developerFrontendHost:$developerFrontendPort"
  val wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(developerFrontendPort))

  trait Setup {
    val connector = new DeveloperFrontendConnector(new WSHttp(ApiDocumentationFrontendAuditConnector), NoopMetrics)
  }

  override def beforeEach() {
    wireMockServer.start()
    WireMock.configureFor(developerFrontendHost, developerFrontendPort)
  }

  override def afterEach() {
    wireMockServer.stop()
  }

  "api" should {
    "be third-party-developer-frontend" in new Setup {
      connector.api shouldBe API("third-party-developer-frontend")
    }
  }

  "fetchNav links" should {

    "return fetched nav links and pass headers by" in new Setup {

      implicit val hc = HeaderCarrier(extraHeaders = Seq("possibleAuthHeader" -> "possibleAuthHeaderVal"))

      stubFor(get(urlEqualTo("/developer/user-navlinks"))
        .willReturn(aResponse().withStatus(200).withBody(Json.toJson(
          Seq(NavLink("Some random link", "/developer/345435345342523534253245"))
        ).toString())))

      val result = await(connector.fetchNavLinks())
      result shouldBe List(NavLink("Some random link", "/developer/345435345342523534253245", false))
      verifyMockServer(1, getRequestedFor(urlPathEqualTo(s"/developer/user-navlinks")).withHeader("possibleAuthHeader", equalTo("possibleAuthHeaderVal")))
    }
  }

  "fetchTermsOfUsePartial" should {
    "return the terms of use as a partial" in new Setup {
      implicit val hc = HeaderCarrier()

      stubFor(get(urlEqualTo("/developer/partials/terms-of-use"))
        .willReturn(aResponse().withStatus(OK).withBody("<p>some terms of use</p>")))

      val result = await(connector.fetchTermsOfUsePartial())
      result shouldBe HtmlPartial.Success(None, Html("<p>some terms of use</p>"))
    }
  }
}
